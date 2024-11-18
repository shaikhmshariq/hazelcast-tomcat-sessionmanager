package com.hazelcast.session;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.Test;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractMapNameTest extends AbstractHazelcastSessionsTest {

    @Test
    public void testMapName() throws Exception {
        HazelcastInstance hz = Hazelcast.newHazelcastInstance();

        instance1 = getWebContainerConfigurator();
        instance1.port(SERVER_PORT_1).sticky(true).clientOnly(true).mapName(SESSION_REPLICATION_MAP_NAME).sessionTimeout(10).start();
        instance2 = getWebContainerConfigurator();
        instance2.port(SERVER_PORT_2).sticky(true).clientOnly(true).mapName(SESSION_REPLICATION_MAP_NAME).sessionTimeout(10).start();
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        executeRequest("write", SERVER_PORT_1,CookieHandler.getDefault());

        HttpCookie cookie = cookieStore.getCookies().get(0);
        String sessionId = cookie.getValue();

        IMap<String, HazelcastSession> map = hz.getMap(SESSION_REPLICATION_MAP_NAME);
        assertEquals(1, map.size());
        HazelcastSession session = map.get(sessionId);

        assertFalse(session.getAttributes().isEmpty());

        executeRequest("remove", SERVER_PORT_1,CookieHandler.getDefault());
        cookie = cookieStore.getCookies().get(0);
        String newSessionId = cookie.getValue();
        session = map.get(newSessionId);

        assertTrue(session.getAttributes().isEmpty());
    }
}
