package com.hazelcast.session;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.test.HazelcastTestSupport;
import org.apache.catalina.Manager;
import org.apache.catalina.session.StandardSession;
import org.junit.After;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public abstract class AbstractHazelcastSessionsTest extends HazelcastTestSupport {

    protected static int SERVER_PORT_1 = findFreeTCPPort();
    protected static int SERVER_PORT_2 = findFreeTCPPort();
    protected static String SESSION_REPLICATION_MAP_NAME = "session-replication-map";

    protected WebContainerConfigurator<?> instance1;
    protected WebContainerConfigurator<?> instance2;

    protected abstract WebContainerConfigurator<?> getWebContainerConfigurator();

    @After
    public void cleanup() throws Exception {
        instance1.stop();
        instance2.stop();
        HazelcastClient.shutdownAll();
        Hazelcast.shutdownAll();
    }

    protected String executeRequest(String context, int serverPort, CookieHandler handler) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(handler)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/" + context))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    /**
     * Helper method to retrieve the JSESSIONID value from the {@link CookieStore}.
     * @param cookieStore the cookie store containing sessions.
     * @return the value of the JSESSIONID cookie if present, otherwise null.
     */
    protected static String getJSessionId(CookieStore cookieStore) {
        String jSessionId = null;
        for (HttpCookie cookie : cookieStore.getCookies()) {
            if ("JSESSIONID".equalsIgnoreCase(cookie.getName())) {
                jSessionId = cookie.getValue();
                break;
            }
        }
        return jSessionId;
    }

    /**
     * Retrieves sessions using {@link Manager#findSession(String)} in accordance with the {@link StandardSession#isValid()}
     * method.
     *
     * @param jSessionId the session id.
     * @param instance the tomcat instance.
     * @return the instance of {@link HazelcastSession} if present, otherwise null.
     */
    protected static HazelcastSession getHazelcastSession(String jSessionId, WebContainerConfigurator<?> instance)
            throws IOException {
        return (HazelcastSession) ((Manager) instance.getManager()).findSession(jSessionId);
    }

    /**
     * Returns any free local TCP port number available.
     */
    private static int findFreeTCPPort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int localPort = serverSocket.getLocalPort();
            serverSocket.close();
            return localPort;
        } catch (Exception e) {
            throw new IllegalStateException("Could not find any available port", e);
        }
    }
}
