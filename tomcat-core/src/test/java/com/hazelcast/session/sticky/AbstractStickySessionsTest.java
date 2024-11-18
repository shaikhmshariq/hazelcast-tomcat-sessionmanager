package com.hazelcast.session.sticky;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.session.AbstractHazelcastSessionsTest;
import org.junit.Test;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public abstract class AbstractStickySessionsTest extends AbstractHazelcastSessionsTest {

    @Test
    public void testContextReloadSticky() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());
        System.out.println("reloading");
        instance1.reload();
        System.out.println("reloaded");
        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value", value);
    }

    @Test
    public void testReadWriteRead() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("null", value);

        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value", value);
    }

    @Test(timeout = 80000)
    public void testAttributeDistribution() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value", value);
    }

    @Test(timeout = 80000)
    public void testAttributeRemoval() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value", value);

        value = executeRequest("remove", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("true", value);

        value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("null", value);
    }

    @Test(timeout = 80000)
    public void testAttributeUpdate() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value", value);

        value = executeRequest("update", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("true", value);

        value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value-updated", value);
    }

    @Test(timeout = 80000)
    public void testAttributeInvalidate() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value", value);

        value = executeRequest("invalidate", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("true", value);

        HazelcastInstance instance = HazelcastClient.newHazelcastClient();
        IMap<Object, Object> map = instance.getMap("default");
        assertEquals(0, map.size());
    }

//    @Test
//    public void testSessionExpire() throws Exception {
//
//        int DEFAULT_SESSION_TIMEOUT = 10;
//        CookieStore cookieStore = new BasicCookieStore();
//        executeRequest("write", SERVER_PORT_1, cookieStore);
//        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
//        assertEquals("value", value);
//
//        sleepSeconds(DEFAULT_SESSION_TIMEOUT + instance1.getManager().getProcessExpiresFrequency());
//
//        value = executeRequest("read", SERVER_PORT_1, cookieStore);
//        assertEquals("null", value);
//    }

    @Test(timeout = 80000)
    public void testAttributeNames() throws Exception {
        CookieHandler.setDefault(new CookieManager());

        executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());

        String commaSeparatedAttributeNames = executeRequest("names", SERVER_PORT_1,CookieHandler.getDefault());

        //no name should be created
        assertEquals("", commaSeparatedAttributeNames);

        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        commaSeparatedAttributeNames = executeRequest("names", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("key", commaSeparatedAttributeNames);
    }

    @Test(timeout = 80000)
    public void test_isNew() throws Exception {
        CookieHandler.setDefault(new CookieManager());

        assertEquals("true", executeRequest("isNew", SERVER_PORT_1,CookieHandler.getDefault()));
        assertEquals("false", executeRequest("isNew", SERVER_PORT_1,CookieHandler.getDefault()));
    }

    @Test(timeout = 80000)
    public void test_LastAccessTime() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        String lastAccessTime1 = executeRequest("lastAccessTime", SERVER_PORT_1,CookieHandler.getDefault());
        executeRequest("lastAccessTime", SERVER_PORT_1,CookieHandler.getDefault());
        String lastAccessTime2 = executeRequest("lastAccessTime", SERVER_PORT_1,CookieHandler.getDefault());

        assertNotEquals(lastAccessTime1, lastAccessTime2);
    }

    @Test(timeout = 80000)
    public void testFailoverWithNoStaleSession() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("null", value);

        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());
        String oldSessionId = executeRequest("get-session-id", SERVER_PORT_1,CookieHandler.getDefault());

        instance1.stop();

        HazelcastInstance hzInstance1 = Hazelcast.getHazelcastInstanceByName("hzInstance1");
        if (hzInstance1 != null) {
            hzInstance1.shutdown();
        }

        String newSessionId = executeRequest("get-session-id", SERVER_PORT_2,CookieHandler.getDefault());
        //The session id should be different after failover because of the changed jvmRoute
        assertNotEquals(oldSessionId, newSessionId);
        value = executeRequest("read", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("value", value);

    }

    @Test(timeout = 80000)
    public void testFailoverWithNoNewSession() throws Exception {
        //given
        CookieHandler manager1 = new CookieManager();
        CookieHandler manager2 = new CookieManager();

        executeRequest("write", SERVER_PORT_1, manager1);
        executeRequest("write", SERVER_PORT_1, manager2);

        //when
        instance1.stop();
        HazelcastInstance hzInstance1 = Hazelcast.getHazelcastInstanceByName("hzInstance1");
        if (hzInstance1 != null) {
            hzInstance1.shutdown();
        }

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    executeRequest("read", SERVER_PORT_2, manager1);
                    executeRequest("read", SERVER_PORT_2, manager2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        for (int i = 0; i < 2; i++) {
            executorService.execute(runnable);
        }

        //then
        String value = executeRequest("read", SERVER_PORT_2, manager1);
        assertEquals("value", value);
        String value2 = executeRequest("read", SERVER_PORT_2, manager2);
        assertEquals("value", value2);
    }

//    @Test
//    public void testCleanupAfterSessionExpire() throws Exception {
//        int DEFAULT_SESSION_TIMEOUT = 10;
//        CookieStore cookieStore = new BasicCookieStore();
//        executeRequest("write", SERVER_PORT_1, cookieStore);
//        String value = executeRequest("read", SERVER_PORT_1, cookieStore);
//        assertEquals("value", value);
//
//        sleepSeconds(DEFAULT_SESSION_TIMEOUT+instance1.getManager().getProcessExpiresFrequency());
//
//        HazelcastInstance instance = Hazelcast.newHazelcastInstance();
//        IMap<Object, Object> map = instance.getMap("default");
//        assertEquals(0, map.size());
//    }
}
