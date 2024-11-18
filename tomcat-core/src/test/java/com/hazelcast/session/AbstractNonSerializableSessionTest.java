package com.hazelcast.session;

import com.hazelcast.core.Hazelcast;
import org.junit.Test;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;

import static org.junit.Assert.assertEquals;

public abstract class AbstractNonSerializableSessionTest extends AbstractHazelcastSessionsTest {
    @Test
    public void testSerialization() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        Hazelcast.newHazelcastInstance();

        instance1 = getWebContainerConfigurator();
        instance1.port(SERVER_PORT_1).sticky(true).clientOnly(true).mapName(SESSION_REPLICATION_MAP_NAME).sessionTimeout(10).start();
        instance2 = getWebContainerConfigurator();
        instance2.port(SERVER_PORT_2).sticky(true).clientOnly(true).mapName(SESSION_REPLICATION_MAP_NAME).sessionTimeout(10).start();
        CookieHandler.setDefault(new CookieManager());
        assertEquals("true", executeRequest("nonserializable", SERVER_PORT_1,CookieHandler.getDefault()));
    }
}
