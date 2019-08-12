//========================================================================
//Copyright 2004-2008 Mort Bay Consulting Pty. Ltd.
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



import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;
import org.mortbay.util.IO;

public class ServletTest extends TestCase
{
    ServletTester tester;
    
    
    /* ------------------------------------------------------------ */
    protected void setUp() throws Exception
    {
        super.setUp();
        tester=new ServletTester();
        tester.setContextPath("/context");
        tester.addServlet(TestServlet.class, "/servlet/*");
        tester.addServlet(HelloServlet.class, "/hello/*");
        tester.addServlet("org.mortbay.jetty.servlet.DefaultServlet", "/");
        tester.start();
    }

    /* ------------------------------------------------------------ */
    protected void tearDown() throws Exception
    {
        tester.stop();
        tester=null;
        super.tearDown();
    }

    /* ------------------------------------------------------------ */
    public void testServletTesterRaw() throws Exception
    {
        // Raw HTTP test requests
        String requests=
            "GET /context/servlet/info?query=foo HTTP/1.1\r\n"+
            "Host: tester\r\n"+
            "\r\n"+

            "GET /context/hello HTTP/1.1\r\n"+
            "Host: tester\r\n"+
            "\r\n";

        String responses = tester.getResponses(requests);

        String expected=
            "HTTP/1.1 200 OK\r\n"+
            "Content-Type: text/html; charset=iso-8859-1\r\n"+
            "Content-Length: 21\r\n"+
            "\r\n"+
            "<h1>Test Servlet</h1>" +

            "HTTP/1.1 200 OK\r\n"+
            "Content-Type: text/html; charset=iso-8859-1\r\n"+
            "Content-Length: 22\r\n"+
            "\r\n"+
            "<h1>Hello Servlet</h1>";

        assertEquals(expected,responses);
    }

    /* ------------------------------------------------------------ */
    public void testServletTesterClient() throws Exception
    {
        String base_url=tester.createSocketConnector(true);
        
        URL url = new URL(base_url+"/context/hello/info");
        String result = IO.toString(url.openStream());
        assertEquals("<h1>Hello Servlet</h1>",result);
    }

    /* ------------------------------------------------------------ */
    public void testHttpTester() throws Exception
    {
        // generated and parsed test
        HttpTester request = new HttpTester();
        HttpTester response = new HttpTester();
        
        // test GET
        request.setMethod("GET");
        request.setVersion("HTTP/1.0");
        request.setHeader("Host","tester");
        request.setURI("/context/hello/info");
        response.parse(tester.getResponses(request.generate()));
        assertTrue(response.getMethod()==null);
        assertEquals(200,response.getStatus());
        assertEquals("<h1>Hello Servlet</h1>",response.getContent());

        // test GET with content
        request.setMethod("POST");
        request.setContent("<pre>Some Test Content</pre>");
        request.setHeader("Content-Type","text/html");
        response.parse(tester.getResponses(request.generate()));
        assertTrue(response.getMethod()==null);
        assertEquals(200,response.getStatus());
        assertEquals("<h1>Hello Servlet</h1><pre>Some Test Content</pre>",response.getContent());
        
        // test redirection
        request.setMethod("GET");
        request.setURI("/context");
        request.setContent(null);
        response.parse(tester.getResponses(request.generate()));
        assertEquals(302,response.getStatus());
        assertEquals("http://tester/context/",response.getHeader("location"));

        // test not found
        request.setURI("/context/xxxx");
        response.parse(tester.getResponses(request.generate()));
        assertEquals(404,response.getStatus());
        
    }


    /* ------------------------------------------------------------ */
    public void testBigPost() throws Exception
    {
        // generated and parsed test
        HttpTester request = new HttpTester();
        HttpTester response = new HttpTester();
        
        String content = "0123456789abcdef";
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+=content;
        content+="!";
        
        request.setMethod("POST");
        request.setVersion("HTTP/1.1");
        request.setURI("/context/hello/info");
        request.setHeader("Host","tester");
        request.setHeader("Content-Type","text/plain");
        request.setContent(content);
        String r=request.generate();
        r = tester.getResponses(r);
        response.parse(r);
        assertTrue(response.getMethod()==null);
        assertEquals(200,response.getStatus());
        assertEquals("<h1>Hello Servlet</h1>"+content,response.getContent());
        
        
    }
    

    /* ------------------------------------------------------------ */
    public void testCharset()
        throws Exception
    {
        byte[] content_iso_8859_1="abcd=1234&AAA=xxx".getBytes("iso8859-1");
        byte[] content_utf_8="abcd=1234&AAA=xxx".getBytes("utf-8");
        byte[] content_utf_16="abcd=1234&AAA=xxx".getBytes("utf-16");

        String request_iso_8859_1=
            "POST /context/servlet/post HTTP/1.1\r\n"+
            "Host: whatever\r\n"+
            "Content-Type: application/x-www-form-urlencoded\r\n"+
            "Content-Length: "+content_iso_8859_1.length+"\r\n"+
            "\r\n";

        String request_utf_8=
            "POST /context/servlet/post HTTP/1.1\r\n"+
            "Host: whatever\r\n"+
            "Content-Type: application/x-www-form-urlencoded; charset=utf-8\r\n"+
            "Content-Length: "+content_utf_8.length+"\r\n"+
            "\r\n";

        String request_utf_16=
            "POST /context/servlet/post HTTP/1.1\r\n"+
            "Host: whatever\r\n"+
            "Content-Type: application/x-www-form-urlencoded; charset=utf-16\r\n"+
            "Content-Length: "+content_utf_16.length+"\r\n"+
            "Connection: close\r\n"+
            "\r\n";
        
        ByteArrayBuffer out = new ByteArrayBuffer(4096);
        out.put(request_iso_8859_1.getBytes("iso8859-1"));
        out.put(content_iso_8859_1);
        out.put(request_utf_8.getBytes("iso8859-1"));
        out.put(content_utf_8);
        out.put(request_utf_16.getBytes("iso8859-1"));
        out.put(content_utf_16);

        ByteArrayBuffer responses = tester.getResponses(out);
        
        String expected=
            "HTTP/1.1 200 OK\r\n"+
            "Content-Type: text/html; charset=iso-8859-1\r\n"+
            "Content-Length: 21\r\n"+
            "\r\n"+
            "<h1>Test Servlet</h1>"+
            "HTTP/1.1 200 OK\r\n"+
            "Content-Type: text/html; charset=iso-8859-1\r\n"+
            "Content-Length: 21\r\n"+
            "\r\n"+
            "<h1>Test Servlet</h1>"+
            "HTTP/1.1 200 OK\r\n"+
            "Content-Type: text/html; charset=iso-8859-1\r\n"+
            "Connection: close\r\n"+
            "\r\n"+
            "<h1>Test Servlet</h1>";
        
        assertEquals(expected,responses.toString());
    }

    
    /* ------------------------------------------------------------ */
    public static class HelloServlet extends HttpServlet
    {
        private static final long serialVersionUID=2779906630657190712L;

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            doGet(request,response);
        }
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            response.setContentType("text/html");
            response.getWriter().print("<h1>Hello Servlet</h1>");
            if (request.getContentLength()>0)
                response.getWriter().write(IO.toString(request.getInputStream()));
        }
    }
    
    public static class TestServlet extends HttpServlet
    {
        private static final long serialVersionUID=2779906630657190712L;

        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            assertEquals("/context",request.getContextPath());
            assertEquals("/servlet",request.getServletPath());
            assertEquals("/post",request.getPathInfo());
            assertEquals(2,request.getParameterMap().size());
            assertEquals("1234",request.getParameter("abcd"));
            assertEquals("xxx",request.getParameter("AAA"));
            
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("<h1>Test Servlet</h1>");
        }
        
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
        {
            assertEquals("/context",request.getContextPath());
            assertEquals("/servlet",request.getServletPath());
            assertEquals("/info",request.getPathInfo());
            assertEquals("query=foo",request.getQueryString());
            assertEquals(1,request.getParameterMap().size());
            assertEquals(1,request.getParameterValues("query").length);
            assertEquals("foo",request.getParameter("query"));
            
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print("<h1>Test Servlet</h1>");
        }
    }
}
