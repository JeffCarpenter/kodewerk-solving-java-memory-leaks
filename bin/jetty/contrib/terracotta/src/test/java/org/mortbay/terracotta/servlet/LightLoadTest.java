// ========================================================================
// Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.terracotta.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
public class LightLoadTest
{
    private boolean _stress = Boolean.getBoolean( "STRESS" );

    @Test
    public void testLightLoad()
        throws Exception
    {
        if ( _stress )
        {
            Random random = new Random( System.nanoTime() );

            String contextPath = "";
            String servletMapping = "/server";
            int port1 = random.nextInt( 50000 ) + 10000;
            TerracottaJettyServer server1 = new TerracottaJettyServer( port1 );
            server1.addContext( contextPath ).addServlet( TestServlet.class, servletMapping );
            server1.start();
            try
            {
                int port2 = random.nextInt( 50000 ) + 10000;
                TerracottaJettyServer server2 = new TerracottaJettyServer( port2 );
                server2.addContext( contextPath ).addServlet( TestServlet.class, servletMapping );
                server2.start();
                try
                {
                    HttpClient client = new HttpClient();
                    client.setConnectorType( HttpClient.CONNECTOR_SOCKET );
                    client.start();
                    try
                    {
                        String[] urls = new String[2];
                        urls[0] = "http://localhost:" + port1 + contextPath + servletMapping;
                        urls[1] = "http://localhost:" + port2 + contextPath + servletMapping;

                        ContentExchange exchange1 = new ContentExchange( true );
                        exchange1.setMethod( HttpMethods.GET );
                        exchange1.setURL( urls[0] + "?action=init" );
                        client.send( exchange1 );
                        exchange1.waitForDone();
                        assert exchange1.getResponseStatus() == HttpServletResponse.SC_OK;
                        String sessionCookie = exchange1.getResponseFields().getStringField( "Set-Cookie" );
                        assert sessionCookie != null;
                        // Mangle the cookie, replacing Path with $Path, etc.
                        sessionCookie = sessionCookie.replaceFirst("(\\W)(P|p)ath=", "$1\\$Path=");

                        ExecutorService executor = Executors.newCachedThreadPool();
                        int clientsCount = 50;
                        CyclicBarrier barrier = new CyclicBarrier( clientsCount + 1 );
                        int requestsCount = 100;
                        Worker[] workers = new Worker[clientsCount];
                        for ( int i = 0; i < clientsCount; ++i )
                        {
                            workers[i] = new Worker( barrier, requestsCount, sessionCookie, urls );
                            workers[i].start();
                            executor.execute( workers[i] );
                        }
                        // Wait for all workers to be ready
                        barrier.await();
                        long start = System.nanoTime();

                        // Wait for all workers to be done
                        barrier.await();
                        long end = System.nanoTime();
                        long elapsed = TimeUnit.NANOSECONDS.toMillis( end - start );
                        System.out.println( "elapsed ms: " + elapsed );

                        for ( Worker worker : workers )
                            worker.stop();
                        executor.shutdownNow();

                        // Perform one request to get the result
                        ContentExchange exchange2 = new ContentExchange( true );
                        exchange2.setMethod( HttpMethods.GET );
                        exchange2.setURL( urls[0] + "?action=result" );
                        exchange2.getRequestFields().add( "Cookie", sessionCookie );
                        client.send( exchange2 );
                        exchange2.waitForDone();
                        assert exchange2.getResponseStatus() == HttpServletResponse.SC_OK;
                        String response = exchange2.getResponseContent();
                        System.out.println( "get = " + response );
                        assert response.trim().equals( String.valueOf( clientsCount * requestsCount ) );
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
    }

    public static class Worker
        implements Runnable
    {
        private final HttpClient client;

        private final CyclicBarrier barrier;

        private final int requestsCount;

        private final String sessionCookie;

        private final String[] urls;

        public Worker( CyclicBarrier barrier, int requestsCount, String sessionCookie, String[] urls )
        {
            this.client = new HttpClient();
            this.client.setConnectorType( HttpClient.CONNECTOR_SOCKET );
            this.barrier = barrier;
            this.requestsCount = requestsCount;
            this.sessionCookie = sessionCookie;
            this.urls = urls;
        }

        public void start()
            throws Exception
        {
            client.start();
        }

        public void stop()
            throws Exception
        {
            client.stop();
        }

        public void run()
        {
            try
            {
                // Wait for all workers to be ready
                barrier.await();

                Random random = new Random( System.nanoTime() );

                for ( int i = 0; i < requestsCount; ++i )
                {
                    int urlIndex = random.nextInt( urls.length );

                    ContentExchange exchange = new ContentExchange( true );
                    exchange.setMethod( HttpMethods.GET );
                    exchange.setURL( urls[urlIndex] + "?action=increment" );
                    exchange.getRequestFields().add( "Cookie", sessionCookie );
                    client.send( exchange );
                    exchange.waitForDone();
                    assert exchange.getResponseStatus() == HttpServletResponse.SC_OK;
                }

                // Wait for all workers to be done
                barrier.await();
            }
            catch ( Exception x )
            {
                throw new RuntimeException( x );
            }
        }
    }

    public static class TestServlet
        extends HttpServlet
    {
        @Override
        protected void doGet( HttpServletRequest request, HttpServletResponse response )
            throws ServletException, IOException
        {
            String action = request.getParameter( "action" );
            if ( "init".equals( action ) )
            {
                HttpSession session = request.getSession( true );
                session.setAttribute( "value", 0 );
            }
            else if ( "increment".equals( action ) )
            {
                // Without synchronization, because it is taken care by Jetty/Terracotta
                HttpSession session = request.getSession( false );
                int value = (Integer) session.getAttribute( "value" );
                session.setAttribute( "value", value + 1 );
            }
            else if ( "result".equals( action ) )
            {
                HttpSession session = request.getSession( false );
                int value = (Integer) session.getAttribute( "value" );
                PrintWriter writer = response.getWriter();
                writer.println( value );
                writer.flush();
            }
        }
    }
}
