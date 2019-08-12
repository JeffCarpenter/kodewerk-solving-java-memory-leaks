//========================================================================
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.example;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.StatisticsHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;

public class ManyServletContexts
{
    public static void main(String[] args)
        throws Exception
    {
        Server server = new Server(8080);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        Context root = new Context(contexts,"/",Context.SESSIONS);
        root.addServlet(new ServletHolder(new HelloServlet("Ciao")), "/*");

        Context other = new Context(contexts,"/other",Context.SESSIONS);
        other.addServlet("org.mortbay.jetty.example.ManyServletContexts$HelloServlet", "/*");

        StatisticsHandler stats = new StatisticsHandler();
        contexts.addHandler(stats);
        Context yetanother =new Context(stats,"/yo",Context.SESSIONS);
        yetanother.addServlet(new ServletHolder(new HelloServlet("YO!")), "/*");

        server.start();
        server.join();
    }

    public static class HelloServlet extends HttpServlet
    {
        String greeting="Hello";
        public HelloServlet()
        {}

        public HelloServlet(String hi)
        {greeting=hi;}

        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("<h1>"+greeting+" SimpleServlet</h1>");
            response.getWriter().println("session="+request.getSession(true).getId());
        }
    }
}
