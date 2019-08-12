/*
 * Copyright 2008-2008 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mortbay.terracotta.servlet;

import java.io.IOException;
import java.util.Random;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.client.ContentExchange;
import org.mortbay.jetty.client.HttpClient;
import org.testng.annotations.Test;

/**
 * @version $Revision: 1645 $ $Date: 2009-09-15 20:31:07 +1000 (Tue, 15 Sep 2009) $
 */
public class LastAccessTimeTest
{
    @Test
    public void testLastAccessTime() throws Exception
    {
        Random random = new Random(System.nanoTime());

        String contextPath = "";
        String servletMapping = "/server";
        int port1 = random.nextInt(50000) + 10000;
        int maxInactivePeriod = 8;
        int scavengePeriod = 2;
        TerracottaJettyServer server1 = new TerracottaJettyServer(port1, maxInactivePeriod, scavengePeriod);
        server1.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
        server1.start();
        try
        {
            int port2 = random.nextInt(50000) + 10000;
            TerracottaJettyServer server2 = new TerracottaJettyServer(port2, maxInactivePeriod, scavengePeriod);
            server2.addContext(contextPath).addServlet(TestServlet.class, servletMapping);
            server2.start();
            try
            {
                HttpClient client = new HttpClient();
                client.setConnectorType(HttpClient.CONNECTOR_SOCKET);
                client.start();
                try
                {
                    // Perform one request to server1 to create a session
                    ContentExchange exchange1 = new ContentExchange(true);
                    exchange1.setMethod(HttpMethods.GET);
                    exchange1.setURL("http://localhost:" + port1 + contextPath + servletMapping + "?action=init");
                    client.send(exchange1);
                    exchange1.waitForDone();
                    assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK;
                    String sessionCookie = exchange1.getResponseFields().getStringField("Set-Cookie");
                    assert sessionCookie != null;
                    // Mangle the cookie, replacing Path with $Path, etc.
                    sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                    // Perform some request to server2 using the session cookie from the previous request
                    // This should migrate the session from server1 to server2, and leave server1's
                    // session in a very stale state, while server2 has a very fresh session.
                    // We want to test that optimizations done to the saving of the shared lastAccessTime
                    // do not break the correct working
                    int requestInterval = 500;
                    for (int i = 0; i < maxInactivePeriod * (1000 / requestInterval); ++i)
                    {
                        ContentExchange exchange2 = new ContentExchange(true);
                        exchange2.setMethod(HttpMethods.GET);
                        exchange2.setURL("http://localhost:" + port2 + contextPath + servletMapping);
                        exchange2.getRequestFields().add("Cookie", sessionCookie);
                        client.send(exchange2);
                        exchange2.waitForDone();
                        assert exchange2.getResponseStatus() == HttpServletResponse.SC_OK;

                        Thread.sleep(requestInterval);
                    }

                    System.out.println("Waiting for scavenging on node1...");

                    // At this point, session1 should be eligible for expiration.
                    // Let's wait for the scavenger to run, waiting 2.5 times the scavenger period
                    Thread.sleep(scavengePeriod * 2500L);

                    // Access again server1, and be sure we can
                    exchange1 = new ContentExchange(true);
                    exchange1.setMethod(HttpMethods.GET);
                    exchange1.setURL("http://localhost:" + port1 + contextPath + servletMapping);
                    exchange1.getRequestFields().add("Cookie", sessionCookie);
                    client.send(exchange1);
                    exchange1.waitForDone();
                    assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK;
                }
                finally
                {
                    client.stop();
                }
            }
            finally
            {
                server2.stop();
            }
        }
        finally
        {
            server1.stop();
        }
    }

    public static class TestServlet extends HttpServlet
    {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException
        {
            String action = request.getParameter("action");
            if ("init".equals(action))
            {
                HttpSession session = request.getSession(true);
                session.setAttribute("test", "test");
            }
            else
            {
                HttpSession session = request.getSession(false);
                session.setAttribute("test", "test");
            }
        }
    }
}
