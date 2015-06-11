package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.junit.Retry;
import org.apache.sling.commons.testing.junit.RetryRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/** Verify that we can start the Felix HTTP service
 *  with a {@link CrankstartBootstrap}. 
 */
public class CrankstartBootstrapTest {
    
    private static final CrankstartSetup C = new CrankstartSetup();
    public static final int LONG_TIMEOUT = 10000;
    public static final int STD_INTERVAL = 250;
    private DefaultHttpClient client;
    
    @Rule
    public final RetryRule retryRule = new RetryRule();
    
    @Before
    public void setupHttpClient() throws IOException {
        C.setup();
        client = new DefaultHttpClient(); 
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testHttpRoot() throws Exception {
        final HttpUriRequest get = new HttpGet(C.getBaseUrl());
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting page not found at " + get.getURI(), 404, response.getStatusLine().getStatusCode());
        } finally {
            C.closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testSingleConfigServlet() throws Exception {
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + "/single");
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
        } finally {
            C.closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testConfigFactoryServlet() throws Exception {
        final String [] paths = { "/foo", "/bar/test" };
        for(String path : paths) {
            final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
            HttpResponse response = null;
            try {
                response = client.execute(get);
                assertEquals("Expecting success for " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            } finally {
                C.closeConnection(response);
            }
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testJUnitServlet() throws Exception {
        final String path = "/system/sling/junit";
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting JUnit servlet to be installed via sling extension command, at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
        } finally {
            C.closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testAdditionalBundles() throws Exception {
        C.setAdminCredentials(client);
        final String basePath = "/system/console/bundles/";
        final String [] addBundles = {
                "org.apache.sling.commons.mime",
                "org.apache.sling.settings"
        };
        
        for(String name : addBundles) {
            final String path = basePath + name;
            final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
            HttpResponse response = null;
            try {
                response = client.execute(get);
                assertEquals("Expecting additional bundle to be present at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            } finally {
                C.closeConnection(response);
            }
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testSpecificStartLevel() throws Exception {
        // Verify that this bundle is only installed, as it's set to start level 99
        C.setAdminCredentials(client);
        final String path = "/system/console/bundles/org.apache.commons.collections.json";
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting bundle status to be available at " + get.getURI(), 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expecting response entity", response.getEntity());
            String encoding = "UTF-8";
            if(response.getEntity().getContentEncoding() != null) {
                encoding = response.getEntity().getContentEncoding().getValue();
            }
            final String content = IOUtils.toString(response.getEntity().getContent(), encoding);
            
            // Start level is in the props array, with key="Start Level"
            final JSONObject status = new JSONObject(content);
            final JSONArray props = status.getJSONArray("data").getJSONObject(0).getJSONArray("props");
            final String KEY = "key";
            final String SL = "Start Level";
            boolean found = false;
            for(int i=0; i < props.length(); i++) {
                final JSONObject o = props.getJSONObject(i);
                if(o.has(KEY) && SL.equals(o.getString(KEY))) {
                    found = true;
                    assertEquals("Expecting the start level that we set", "99", o.getString("value"));
                }
            }
            assertTrue("Expecting start level to be found in JSON output", found);
        } finally {
            C.closeConnection(response);
        }
    }
    
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testEmptyConfig() throws Exception {
        C.setAdminCredentials(client);
        assertHttpGet(
            "/test/config/empty.config.should.work", 
            "empty.config.should.work#service.pid=(String)empty.config.should.work##EOC#");
    }
        
    @Test
    @Retry(timeoutMsec=CrankstartBootstrapTest.LONG_TIMEOUT, intervalMsec=CrankstartBootstrapTest.STD_INTERVAL)
    public void testFelixFormatConfig() throws Exception {
        C.setAdminCredentials(client);
        assertHttpGet(
                "/test/config/felix.format.test", 
                "felix.format.test#array=(String[])[foo, bar.from.launcher.test]#mongouri=(String)mongodb://localhost:27017#service.pid=(String)felix.format.test#service.ranking.launcher.test=(Integer)54321##EOC#");
    }
    
    private void assertHttpGet(String path, String expectedContent) throws Exception {
        final HttpUriRequest get = new HttpGet(C.getBaseUrl() + path);
        HttpResponse response = null;
        try {
            response = client.execute(get);
            assertEquals("Expecting 200 response at " + path, 200, response.getStatusLine().getStatusCode());
            assertNotNull("Expecting response entity", response.getEntity());
            String encoding = "UTF-8";
            if(response.getEntity().getContentEncoding() != null) {
                encoding = response.getEntity().getContentEncoding().getValue();
            }
            final String content = IOUtils.toString(response.getEntity().getContent(), encoding);
            assertEquals(expectedContent, content);
        } finally {
            C.closeConnection(response);
        }
    }
    
}