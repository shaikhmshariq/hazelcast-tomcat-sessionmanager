package com.hazelcast.session;

import com.hazelcast.core.Hazelcast;
import org.junit.Before;
import org.junit.Test;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;

import static org.junit.Assert.assertTrue;

public abstract class AbstractTomcatSecurityTest extends AbstractHazelcastSessionsTest {
    @Override
    protected WebContainerConfigurator<?> getWebContainerConfigurator() {
        return getTomcatConfigurator("appWithSecurity");
    }

    @Before
    public void init() throws Exception {
        Hazelcast.newHazelcastInstance();
        instance1 = getWebContainerConfigurator();
        instance1.port(SERVER_PORT_1).sticky(false).clientOnly(true).sessionTimeout(10).start();
        instance2 = getWebContainerConfigurator();
        instance2.port(SERVER_PORT_2).sticky(false).clientOnly(true).sessionTimeout(10).start();
    }

    @Test
    public void testGetProtectedResourceFormLogin() throws Exception {
        CookieHandler.setDefault(new CookieManager());
        CookieStore cookieStore = ((CookieManager) CookieHandler.getDefault()).getCookieStore();
        assertTrue(executeRequest("secureEndpoint", SERVER_PORT_1,CookieHandler.getDefault()).contains("redirected to LoginServlet"));
    }

    protected abstract WebContainerConfigurator<?> getTomcatConfigurator(String appName);
}
