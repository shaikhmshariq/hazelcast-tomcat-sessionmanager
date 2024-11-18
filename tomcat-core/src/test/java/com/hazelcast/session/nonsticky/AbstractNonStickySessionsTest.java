package com.hazelcast.session.nonsticky;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.session.AbstractHazelcastSessionsTest;
import com.hazelcast.session.CustomAttribute;
import com.hazelcast.session.HazelcastSession;
import org.junit.Test;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public abstract class AbstractNonStickySessionsTest extends AbstractHazelcastSessionsTest {

    @Test
    public void testContextReloadNonSticky() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());
        instance1.reload();

        String value = executeRequest("read", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("value", value);
    }

    @Test
    public void testReadWriteRead() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        String value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("null", value);

        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        value = executeRequest("read", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("value", value);
    }

    @Test
    public void testReadWriteReadWithCustomSerialization() throws Exception {
        CustomAttribute expected = new CustomAttribute("value");
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        String value = executeRequest("read-custom-attribute", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("null", value);

        executeRequest("write-custom-attribute", SERVER_PORT_1,CookieHandler.getDefault());

        value = executeRequest("read-custom-attribute", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals(expected.toString(), value);
    }

    @Test(timeout = 60000)
    public void testAttributeDistribution() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("value", value);
    }

    @Test(timeout = 60000)
    public void testAttributeRemoval() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("value", value);

        value = executeRequest("remove", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("true", value);

        value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("null", value);
    }

    @Test(timeout = 60000)
    public void testAttributeUpdate() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("value", value);

        value = executeRequest("update", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("true", value);

        value = executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("value-updated", value);
    }

    @Test(timeout = 60000)
    public void testAttributeInvalidate() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        String value = executeRequest("read", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("value", value);

        value = executeRequest("invalidate", SERVER_PORT_2,CookieHandler.getDefault());
        assertEquals("true", value);

        HazelcastInstance instance = HazelcastClient.newHazelcastClient();
        IMap<Object, Object> map = instance.getMap("default");
        assertEquals(0, map.size());
    }

    @Test(timeout = 60000)
    public void testAttributeNames() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        executeRequest("read", SERVER_PORT_1,CookieHandler.getDefault());

        String commaSeparatedAttributeNames = executeRequest("names", SERVER_PORT_2,CookieHandler.getDefault());

        // no name should be created
        assertEquals("", commaSeparatedAttributeNames);

        executeRequest("write", SERVER_PORT_2,CookieHandler.getDefault());

        commaSeparatedAttributeNames = executeRequest("names", SERVER_PORT_1,CookieHandler.getDefault());
        assertEquals("key", commaSeparatedAttributeNames);

    }

    @Test(timeout = 60000)
    public void test_isNew() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        assertEquals("true", executeRequest("isNew", SERVER_PORT_1,CookieHandler.getDefault()));
        assertEquals("false", executeRequest("isNew", SERVER_PORT_2,CookieHandler.getDefault()));
    }

    @Test(timeout = 60000)
    public void test_LastAccessTime() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        String lastAccessTime1 = executeRequest("lastAccessTime", SERVER_PORT_1,CookieHandler.getDefault());
        executeRequest("lastAccessTime", SERVER_PORT_2,CookieHandler.getDefault());
        String lastAccessTime2 = executeRequest("lastAccessTime", SERVER_PORT_2,CookieHandler.getDefault());

        assertNotEquals(lastAccessTime1, lastAccessTime2);
    }

    @Test
    public void givenValidSession_whenNonStickySessions_thenAccessTimesAreEqualOnAllNodes() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        assertEquals("true", executeRequest("isNew", SERVER_PORT_1,CookieHandler.getDefault()));
        String jSessionId = getJSessionId(cookieStore);

        HazelcastSession session1 = getHazelcastSession(jSessionId, instance1);
        HazelcastSession session2 = getHazelcastSession(jSessionId, instance2);

        validateSessionAccessTime(session1, session2);
    }

    public abstract void validateSessionAccessTime(HazelcastSession session1, HazelcastSession session2);
}
