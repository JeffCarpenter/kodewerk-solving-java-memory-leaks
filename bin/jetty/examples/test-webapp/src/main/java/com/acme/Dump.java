// ========================================================================
// Copyright 1996-2005 Mort Bay Consulting Pty. Ltd.
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

package com.acme;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.UnavailableException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.StringUtil;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;



/* ------------------------------------------------------------ */
/** Dump Servlet Request.
 * 
 */
public class Dump extends HttpServlet
{
    static boolean fixed;
    /* ------------------------------------------------------------ */
    public void init(ServletConfig config) throws ServletException
    {
    	super.init(config);
    	
    	if (config.getInitParameter("unavailable")!=null && !fixed)
    	{
    	    
    	    fixed=true;
    	    throw new UnavailableException("Unavailable test",Integer.parseInt(config.getInitParameter("unavailable")));
    	}
    }

    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }

    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if(request.getPathInfo()!=null && request.getPathInfo().toLowerCase().indexOf("script")!=-1)
        {
            response.sendRedirect(getServletContext().getContextPath() + "/dump/info");
            return;
        }
            
        request.setCharacterEncoding("UTF-8");
        
        if (request.getParameter("empty")!=null)
        {
            response.setStatus(200);
            response.flushBuffer();
            return;
        }
        
        if (request.getParameter("sleep")!=null)
        {
            try
            {
                long s = Long.parseLong(request.getParameter("sleep"));
                Thread.sleep(s/2);
                response.sendError(102);
                Thread.sleep(s/2);
            }
            catch (InterruptedException e)
            {
                return;
            }
            catch (Exception e)
            {
                throw new ServletException(e);
            }
        }
        
        if (request.getParameter("continue")!=null)
        {
            try
            {
                Continuation continuation = ContinuationSupport.getContinuation(request, null);
                continuation.suspend(Long.parseLong(request.getParameter("continue")));
            }
            catch(Exception e)
            {
                throw new ServletException(e);
            }
        }
            
        request.setAttribute("Dump", this);
        getServletContext().setAttribute("Dump",this);
        // getServletContext().log("dump "+request.getRequestURI());

        // Force a content length response
        String length= request.getParameter("length");
        if (length != null && length.length() > 0)
        {
            response.setContentLength(Integer.parseInt(length));
        }

        // Handle a dump of data
        String data= request.getParameter("data");
        String block= request.getParameter("block");
        String dribble= request.getParameter("dribble");
        if (data != null && data.length() > 0)
        {
            long d=Long.parseLong(data);
            int b=(block!=null&&block.length()>0)?Integer.parseInt(block):50;
            byte[] buf=new byte[b];
            for (int i=0;i<b;i++)
            {
                
                buf[i]=(byte)('0'+(i%10));
                if (i%10==9)
                    buf[i]=(byte)'\n';
            }
            buf[0]='o';
            OutputStream out=response.getOutputStream();
            response.setContentType("text/plain");
            while (d > 0)
            {
                if (b==1)
                {
                    out.write(d%80==0?'\n':'.');
                    d--;
                }
                else if (d>=b)
                {
                    out.write(buf);
                    d=d-b;
                }
                else
                {
                    out.write(buf,0,(int)d);
                    d=0;
                }
                
                if (dribble!=null)
                {
                    out.flush();
                    try
                    {
                        Thread.sleep(Long.parseLong(dribble));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        break;
                    }
                }
                
            }
            
            return;
        }

        // Handle a dump of data
        String chars= request.getParameter("chars");
        if (chars != null && chars.length() > 0)
        {
            long d=Long.parseLong(chars);
            int b=(block!=null&&block.length()>0)?Integer.parseInt(block):50;
            char[] buf=new char[b];
            for (int i=0;i<b;i++)
            {
                buf[i]=(char)('0'+(i%10));
                if (i%10==9)
                    buf[i]='\n';
            }
            buf[0]='o';
            response.setContentType("text/plain");
            PrintWriter out=response.getWriter();
            while (d > 0 && !out.checkError())
            {
                if (b==1)
                {
                    out.write(d%80==0?'\n':'.');
                    d--;
                }
                else if (d>=b)
                {
                    out.write(buf);
                    d=d-b;
                }
                else
                {
                    out.write(buf,0,(int)d);
                    d=0;
                }
            }
            return;
        }

        
        
        // handle an exception
        String info= request.getPathInfo();
        if (info != null && info.endsWith("Exception"))
        {
            try
            {
                throw (Throwable) Thread.currentThread().getContextClassLoader().loadClass(info.substring(1)).newInstance();
            }
            catch (Throwable th)
            {
                throw new ServletException(th);
            }
        }

        // test a reset
        String reset= request.getParameter("reset");
        if (reset != null && reset.length() > 0)
        {
            response.getOutputStream().println("THIS SHOULD NOT BE SEEN!");
            response.setHeader("SHOULD_NOT","BE SEEN");
            response.reset();
        }
        
        
        // handle an redirect
        String redirect= request.getParameter("redirect");
        if (redirect != null && redirect.length() > 0)
        {
            response.getOutputStream().println("THIS SHOULD NOT BE SEEN!");
            response.sendRedirect(redirect);
            try
            {
                response.getOutputStream().println("THIS SHOULD NOT BE SEEN!");
            }
            catch(IOException e)
            {
                // ignored as stream is closed.
            }
            return;
        }

        // handle an error
        String error= request.getParameter("error");
        if (error != null && error.length() > 0 && request.getAttribute("javax.servlet.error.status_code")==null)
        {
            response.getOutputStream().println("THIS SHOULD NOT BE SEEN!");
            response.sendError(Integer.parseInt(error));
            try
            {
                response.getOutputStream().println("THIS SHOULD NOT BE SEEN!");
            }
            catch(IllegalStateException e)
            {
                try
                {
                    response.getWriter().println("NOR THIS!!"); 
                }
                catch(IOException e2){}
            }
            catch(IOException e){}
            return;
        }

        // Handle a extra headers 
        String headers= request.getParameter("headers");
        if (headers != null && headers.length() > 0)
        {
            long h=Long.parseLong(headers);
            for (int i=0;i<h;i++)
                response.addHeader("Header"+i,"Value"+i);
        }

        String buffer= request.getParameter("buffer");
        if (buffer != null && buffer.length() > 0)
            response.setBufferSize(Integer.parseInt(buffer));

        String charset= request.getParameter("charset");
        if (charset==null)
            charset="UTF-8";
        response.setCharacterEncoding(charset);
        response.setContentType("text/html");

        if (info != null && info.indexOf("Locale/") >= 0)
        {
            try
            {
                String locale_name= info.substring(info.indexOf("Locale/") + 7);
                Field f= java.util.Locale.class.getField(locale_name);
                response.setLocale((Locale)f.get(null));
            }
            catch (Exception e)
            {
                e.printStackTrace();
                response.setLocale(Locale.getDefault());
            }
        }

        String cn= request.getParameter("cookie");
        String cv=request.getParameter("cookiev");
        if (cn!=null && cv!=null)
        {
            Cookie cookie= new Cookie(cn, cv);
            if (request.getParameter("version")!=null)
                cookie.setVersion(Integer.parseInt(request.getParameter("version")));
            cookie.setComment("Cookie from dump servlet");
            response.addCookie(cookie);
        }

        String pi= request.getPathInfo();
        if (pi != null && pi.startsWith("/ex"))
        {
            OutputStream out= response.getOutputStream();
            out.write("</H1>This text should be reset</H1>".getBytes());
            if ("/ex0".equals(pi))
                throw new ServletException("test ex0", new Throwable());
            else if ("/ex1".equals(pi))
                throw new IOException("test ex1");
            else if ("/ex2".equals(pi))
                throw new UnavailableException("test ex2");
            else if (pi.startsWith("/ex3/"))
                throw new UnavailableException("test ex3",Integer.parseInt(pi.substring(5)));
            throw new RuntimeException("test<script>alert('no script?');</script>");
        }

        if ("true".equals(request.getParameter("close")))
            response.setHeader("Connection","close");

        String buffered= request.getParameter("buffered");
        
        PrintWriter pout=null;
        
        try
        {
            pout =response.getWriter();
        }
        catch(IllegalStateException e)
        {
            pout=new PrintWriter(new OutputStreamWriter(response.getOutputStream(),charset));
        }
        if (buffered!=null)
            pout = new PrintWriter(new BufferedWriter(pout,Integer.parseInt(buffered)));
        
        try
        {
            pout.write("<html>\n<body>\n");
            pout.write("<h1>Dump Servlet</h1>\n");
            pout.write("<table width=\"95%\">");
            pout.write("<tr>\n");
            pout.write("<th align=\"right\">getMethod:&nbsp;</th>");
            pout.write("<td>" + notag(request.getMethod())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getContentLength:&nbsp;</th>");
            pout.write("<td>"+Integer.toString(request.getContentLength())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getContentType:&nbsp;</th>");
            pout.write("<td>"+notag(request.getContentType())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getRequestURI:&nbsp;</th>");
            pout.write("<td>"+notag(request.getRequestURI())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getRequestURL:&nbsp;</th>");
            pout.write("<td>"+notag(request.getRequestURL().toString())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getContextPath:&nbsp;</th>");
            pout.write("<td>"+request.getContextPath()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getServletPath:&nbsp;</th>");
            pout.write("<td>"+notag(request.getServletPath())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getPathInfo:&nbsp;</th>");
            pout.write("<td>"+notag(request.getPathInfo())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getPathTranslated:&nbsp;</th>");
            pout.write("<td>"+notag(request.getPathTranslated())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getQueryString:&nbsp;</th>");
            pout.write("<td>"+notag(request.getQueryString())+"</td>");

            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getProtocol:&nbsp;</th>");
            pout.write("<td>"+request.getProtocol()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getScheme:&nbsp;</th>");
            pout.write("<td>"+request.getScheme()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getServerName:&nbsp;</th>");
            pout.write("<td>"+notag(request.getServerName())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getServerPort:&nbsp;</th>");
            pout.write("<td>"+Integer.toString(request.getServerPort())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getLocalName:&nbsp;</th>");
            pout.write("<td>"+request.getLocalName()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getLocalAddr:&nbsp;</th>");
            pout.write("<td>"+request.getLocalAddr()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getLocalPort:&nbsp;</th>");
            pout.write("<td>"+Integer.toString(request.getLocalPort())+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getRemoteUser:&nbsp;</th>");
            pout.write("<td>"+request.getRemoteUser()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getRemoteAddr:&nbsp;</th>");
            pout.write("<td>"+request.getRemoteAddr()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getRemoteHost:&nbsp;</th>");
            pout.write("<td>"+request.getRemoteHost()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getRemotePort:&nbsp;</th>");
            pout.write("<td>"+request.getRemotePort()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getRequestedSessionId:&nbsp;</th>");
            pout.write("<td>"+request.getRequestedSessionId()+"</td>");
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">isSecure():&nbsp;</th>");
            pout.write("<td>"+request.isSecure()+"</td>");

            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">isUserInRole(admin):&nbsp;</th>");
            pout.write("<td>"+request.isUserInRole("admin")+"</td>");

            pout.write("</tr><tr>\n");
            pout.write("<th align=\"right\">getLocale:&nbsp;</th>");
            pout.write("<td>"+request.getLocale()+"</td>");
            
            Enumeration locales= request.getLocales();
            while (locales.hasMoreElements())
            {
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">getLocales:&nbsp;</th>");
                pout.write("<td>"+locales.nextElement()+"</td>");
            }
            pout.write("</tr><tr>\n");
            
            pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Other HTTP Headers:</big></th>");
            Enumeration h= request.getHeaderNames();
            String name;
            while (h.hasMoreElements())
            {
                name= (String)h.nextElement();

                Enumeration h2= request.getHeaders(name);
                while (h2.hasMoreElements())
                {
                    String hv= (String)h2.nextElement();
                    pout.write("</tr><tr>\n");
                    pout.write("<th align=\"right\">"+notag(name)+":&nbsp;</th>");
                    pout.write("<td>"+notag(hv)+"</td>");
                }
            }

            pout.write("</tr><tr>\n");
            pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Request Parameters:</big></th>");
            h= request.getParameterNames();
            while (h.hasMoreElements())
            {
                name= (String)h.nextElement();
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">"+notag(name)+":&nbsp;</th>");
                pout.write("<td>"+notag(request.getParameter(name))+"</td>");
                String[] values= request.getParameterValues(name);
                if (values == null)
                {
                    pout.write("</tr><tr>\n");
                    pout.write("<th align=\"right\">"+notag(name)+" Values:&nbsp;</th>");
                    pout.write("<td>"+"NULL!"+"</td>");
                }
                else if (values.length > 1)
                {
                    for (int i= 0; i < values.length; i++)
                    {
                        pout.write("</tr><tr>\n");
                        pout.write("<th align=\"right\">"+notag(name)+"["+i+"]:&nbsp;</th>");
                        pout.write("<td>"+notag(values[i])+"</td>");
                    }
                }
            }

            pout.write("</tr><tr>\n");
            pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Cookies:</big></th>");
            Cookie[] cookies = request.getCookies();
            for (int i=0; cookies!=null && i<cookies.length;i++)
            {
                Cookie cookie = cookies[i];

                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">"+notag(cookie.getName())+":&nbsp;</th>");
                pout.write("<td>"+notag(cookie.getValue())+"</td>");
            }
            
            String content_type=request.getContentType();
            if (content_type!=null &&
                !content_type.startsWith("application/x-www-form-urlencoded") &&
                !content_type.startsWith("multipart/form-data"))
            {
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"left\" valign=\"top\" colspan=\"2\"><big><br/>Content:</big></th>");
                pout.write("</tr><tr>\n");
                pout.write("<td><pre>");
                char[] content= new char[4096];
                int len;
                try{
                    Reader in=request.getReader();
                    
                    while((len=in.read(content))>=0)
                        pout.write(notag(new String(content,0,len)));
                }
                catch(IOException e)
                {
                    pout.write(e.toString());
                }
                
                pout.write("</pre></td>");
            }
            
            
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Request Attributes:</big></th>");
            Enumeration a= request.getAttributeNames();
            while (a.hasMoreElements())
            {
                name= (String)a.nextElement();
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\" valign=\"top\">"+name+":&nbsp;</th>");
                pout.write("<td>"+"<pre>" + toString(request.getAttribute(name)) + "</pre>"+"</td>");
            }            

            
            pout.write("</tr><tr>\n");
            pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Servlet InitParameters:</big></th>");
            a= getInitParameterNames();
            while (a.hasMoreElements())
            {
                name= (String)a.nextElement();
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">"+name+":&nbsp;</th>");
                pout.write("<td>"+ toString(getInitParameter(name)) +"</td>");
            }

            pout.write("</tr><tr>\n");
            pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Context InitParameters:</big></th>");
            a= getServletContext().getInitParameterNames();
            while (a.hasMoreElements())
            {
                name= (String)a.nextElement();
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">"+name+":&nbsp;</th>");
                pout.write("<td>"+ toString(getServletContext().getInitParameter(name)) + "</td>");
            }

            pout.write("</tr><tr>\n");
            pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Context Attributes:</big></th>");
            a= getServletContext().getAttributeNames();
            while (a.hasMoreElements())
            {
                name= (String)a.nextElement();
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\" valign=\"top\">"+name+":&nbsp;</th>");
                pout.write("<td>"+"<pre>" + toString(getServletContext().getAttribute(name)) + "</pre>"+"</td>");
            }


            String res= request.getParameter("resource");
            if (res != null && res.length() > 0)
            {
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"left\" colspan=\"2\"><big><br/>Get Resource: \""+res+"\"</big></th>");
                
                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">this.getClass().getResource(...):&nbsp;</th>");
                pout.write("<td>"+this.getClass().getResource(res)+"</td>");

                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">this.getClass().getClassLoader().getResource(...):&nbsp;</th>");
                pout.write("<td>"+this.getClass().getClassLoader().getResource(res)+"</td>");

                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">Thread.currentThread().getContextClassLoader().getResource(...):&nbsp;</th>");
                pout.write("<td>"+Thread.currentThread().getContextClassLoader().getResource(res)+"</td>");

                pout.write("</tr><tr>\n");
                pout.write("<th align=\"right\">getServletContext().getResource(...):&nbsp;</th>");
                try{pout.write("<td>"+getServletContext().getResource(res)+"</td>");}
                catch(Exception e) {pout.write("<td>"+"" +e+"</td>");}
            }
            
            pout.write("</tr></table>\n");

            /* ------------------------------------------------------------ */
            pout.write("<h2>Request Wrappers</h2>\n");
            ServletRequest rw=request;
            int w=0;
            while (rw !=null)
            {
                pout.write((w++)+": "+rw.getClass().getName()+"<br/>");
                if (rw instanceof HttpServletRequestWrapper)
                    rw=((HttpServletRequestWrapper)rw).getRequest();
                else if (rw  instanceof ServletRequestWrapper)
                    rw=((ServletRequestWrapper)rw).getRequest();
                else
                    rw=null;
            }
            
            pout.write("<br/>");
            pout.write("<h2>International Characters (UTF-8)</h2>");
            pout.write("LATIN LETTER SMALL CAPITAL AE<br/>\n");
            pout.write("Directly uni encoded(\\u1d01): \u1d01<br/>");
            pout.write("HTML reference (&amp;AElig;): &AElig;<br/>");
            pout.write("Decimal (&amp;#7425;): &#7425;<br/>");
            pout.write("Javascript unicode (\\u1d01) : <script language='javascript'>document.write(\"\u1d01\");</script><br/>");
            pout.write("<br/>");
            pout.write("<h2>Form to generate GET content</h2>");
            pout.write("<form method=\"GET\" action=\""+response.encodeURL(getURI(request))+"\">");
            pout.write("TextField: <input type=\"text\" name=\"TextField\" value=\"value\"/><br/>\n");
            pout.write("<input type=\"submit\" name=\"Action\" value=\"Submit\">");
            pout.write("</form>");

            pout.write("<br/>");
            
            pout.write("<h2>Form to generate POST content</h2>");
            pout.write("<form method=\"POST\" accept-charset=\"utf-8\" action=\""+response.encodeURL(getURI(request))+"\">");
            pout.write("TextField: <input type=\"text\" name=\"TextField\" value=\"value\"/><br/>\n");
            pout.write("Select: <select multiple name=\"Select\">\n");
            pout.write("<option>ValueA</option>");
            pout.write("<option>ValueB1,ValueB2</option>");
            pout.write("<option>ValueC</option>");
            pout.write("</select><br/>");
            pout.write("<input type=\"submit\" name=\"Action\" value=\"Submit\"><br/>");
            pout.write("</form>");
            pout.write("<br/>");
            
            pout.write("<h2>Form to generate UPLOAD content</h2>");
            pout.write("<form method=\"POST\" enctype=\"multipart/form-data\" accept-charset=\"utf-8\" action=\""+response.encodeURL(getURI(request))+"\">");
            pout.write("TextField: <input type=\"text\" name=\"TextField\" value=\"comment\"/><br/>\n");
            pout.write("File 1: <input type=\"file\" name=\"file1\" /><br/>\n");
            pout.write("File 2: <input type=\"file\" name=\"file2\" /><br/>\n");
            pout.write("<input type=\"submit\" name=\"Action\" value=\"Submit\"><br/>");
            pout.write("</form>");

            pout.write("<h2>Form to set Cookie</h2>");
            pout.write("<form method=\"POST\" action=\""+response.encodeURL(getURI(request))+"\">");
            pout.write("cookie: <input type=\"text\" name=\"cookie\" /><br/>\n");
            pout.write("value: <input type=\"text\" name=\"cookiev\" /><br/>\n");
            pout.write("<input type=\"submit\" name=\"Action\" value=\"setCookie\">");
            pout.write("</form>\n");
            
            pout.write("<h2>Form to get Resource</h2>");
            pout.write("<form method=\"POST\" action=\""+response.encodeURL(getURI(request))+"\">");
            pout.write("resource: <input type=\"text\" name=\"resource\" /><br/>\n");
            pout.write("<input type=\"submit\" name=\"Action\" value=\"getResource\">");
            pout.write("</form>\n");
            

        }
        catch (Exception e)
        {
            getServletContext().log("dump", e);
        }

        
        if (request.getParameter("stream")!=null)
        {
            pout.flush();
            Continuation continuation = ContinuationSupport.getContinuation(request, null);
            continuation.suspend(Long.parseLong(request.getParameter("stream")));
        }

        String lines= request.getParameter("lines");
        if (lines!=null)
        {
            char[] line = "<span>A line of characters. Blah blah blah blah.  blooble blooble</span></br>\n".toCharArray();
            for (int l=Integer.parseInt(lines);l-->0;)
            {
                pout.write("<span>"+l+" </span>");
                pout.write(line);
            }
        }
        
        pout.write("</body>\n</html>\n");
        
        pout.close();

        if (pi != null)
        {
            if ("/ex4".equals(pi))
                throw new ServletException("test ex4", new Throwable());
            if ("/ex5".equals(pi))
                throw new IOException("test ex5");
            if ("/ex6".equals(pi))
                throw new UnavailableException("test ex6");
        }


    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Dump Servlet";
    }

    /* ------------------------------------------------------------ */
    public synchronized void destroy()
    {
    }

    /* ------------------------------------------------------------ */
    private String getURI(HttpServletRequest request)
    {
        String uri= (String)request.getAttribute("javax.servlet.forward.request_uri");
        if (uri == null)
            uri= request.getRequestURI();
        return uri;
    }

    /* ------------------------------------------------------------ */
    private static String toString(Object o)
    {
        if (o == null)
            return null;

        try
        {
            if (o.getClass().isArray())
            {
                StringBuffer sb = new StringBuffer();
                if (!o.getClass().getComponentType().isPrimitive())
                {
                    Object[] array= (Object[])o;
                    for (int i= 0; i < array.length; i++)
                    {
                        if (i > 0)
                            sb.append("\n");
                        sb.append(array.getClass().getComponentType().getName());
                        sb.append("[");
                        sb.append(i);
                        sb.append("]=");
                        sb.append(toString(array[i]));
                    }
                    return sb.toString();
                }
                else
                { 
                    int length = Array.getLength(o);
                    for (int i=0;i<length;i++)
                    {
                        if (i > 0)
                            sb.append("\n");
                        sb.append(o.getClass().getComponentType().getName()); 
                        sb.append("[");
                        sb.append(i);
                        sb.append("]=");
                        sb.append(toString(Array.get(o, i)));
                    }
                    return sb.toString();
                }
            }
            else
                return o.toString();
        }
        catch (Exception e)
        {
            return e.toString();
        }
    }

    private String notag(String s)
    {
        if (s==null)
            return "null";
        s=StringUtil.replace(s,"&","&amp;");
        s=StringUtil.replace(s,"<","&lt;");
        s=StringUtil.replace(s,">","&gt;");
        return s;
    }
}
