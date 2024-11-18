package com.hazelcast.session.springboot;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractSpringBootConfigurationTest {
    ApplicationContext applicationContext;

    abstract void setup();

    abstract void clean();

    @Test()
    public void testManagerCustomizerBean() {
        assertNotNull(applicationContext.getBean("hazelcastTomcatSessionManagerCustomizer"));
    }

    @Test
    public void testSessionMapCreated() {
        //given
        //the Spring Boot application is started
        //then
        HazelcastInstance hazelcastInstance = (HazelcastInstance) applicationContext.getBean("hazelcastInstance");
        LinkedList<DistributedObject> distributedObjects = (LinkedList<DistributedObject>) hazelcastInstance.getDistributedObjects();
        assertEquals("Session map should be created.", 1, distributedObjects.size());
    }

    @Test()
    public void testSessionStoredInHazelcast()
            throws Exception {
        //given
        //the Spring Boot application is started
        HazelcastInstance hazelcastInstance = (HazelcastInstance) applicationContext.getBean("hazelcastInstance");
        LinkedList<DistributedObject> distributedObjects = (LinkedList<DistributedObject>) hazelcastInstance.getDistributedObjects();

        //when
        CookieManager manager = new CookieManager();
        CookieHandler.setDefault(manager);

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(CookieHandler.getDefault())
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:9999/set"))
                .GET()
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());

        //then
        IMap<Object, Object> sessionMap = hazelcastInstance.getMap(distributedObjects.get(0).getName());
        assertEquals("Session should be created.",1, sessionMap.size());
    }
}
