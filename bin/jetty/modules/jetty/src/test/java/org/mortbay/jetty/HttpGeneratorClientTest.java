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

package org.mortbay.jetty;

import java.io.IOException;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.ByteArrayEndPoint;
import org.mortbay.io.SimpleBuffers;
import org.mortbay.io.View;
import org.mortbay.util.StringUtil;

import junit.framework.TestCase;

public class HttpGeneratorClientTest extends TestCase
{
    public final static String CONTENT="The quick brown fox jumped over the lazy dog.\nNow is the time for all good men to come to the aid of the party\nThe moon is blue to a fish in love.\n";
    public final static String[] connect={null,"keep-alive","close"};

    public HttpGeneratorClientTest(String arg0)
    {
        super(arg0);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(HttpGeneratorTest.class);
    }

    public void testContentLength()
        throws Exception
    {
        Buffer bb=new ByteArrayBuffer(8096);
        Buffer sb=new ByteArrayBuffer(1500);
        ByteArrayEndPoint endp = new ByteArrayEndPoint(new byte[0],4096);
        HttpGenerator generator = new HttpGenerator(new SimpleBuffers(new Buffer[]{sb,bb}),endp, sb.capacity(), bb.capacity());
        
        generator.setRequest("GET","/usr");
        
        HttpFields fields = new HttpFields();
        fields.add("Header","Value");
        fields.add("Content-Type","text/plain");
        
        String content = "The quick brown fox jumped over the lazy dog";
        fields.addLongField("Content-Length",content.length());
        
        generator.completeHeader(fields,false);
        
        generator.addContent(new ByteArrayBuffer(content),true);
        generator.flush();
        generator.complete();
        generator.flush();
        
        String result=endp.getOut().toString();
        result=StringUtil.replace(result,"\r\n","|");
        result=StringUtil.replace(result,"\r","|");
        result=StringUtil.replace(result,"\n","|");
        
        assertEquals("GET /usr HTTP/1.1|Header: Value|Content-Type: text/plain|Content-Length: 44||"+content,result);
    }

    public void testAutoContentLength()
        throws Exception
    {
        Buffer bb=new ByteArrayBuffer(8096);
        Buffer sb=new ByteArrayBuffer(1500);
        ByteArrayEndPoint endp = new ByteArrayEndPoint(new byte[0],4096);
        HttpGenerator generator = new HttpGenerator(new SimpleBuffers(new Buffer[]{sb,bb}),endp, sb.capacity(), bb.capacity());
        
        generator.setRequest("GET","/usr");
        
        HttpFields fields = new HttpFields();
        fields.add("Header","Value");
        fields.add("Content-Type","text/plain");
        
        String content = "The quick brown fox jumped over the lazy dog";

        generator.addContent(new ByteArrayBuffer(content),true);
        generator.completeHeader(fields,true);
        
        generator.flush();
        generator.complete();
        generator.flush();
        
        String result=endp.getOut().toString();
        result=StringUtil.replace(result,"\r\n","|");
        result=StringUtil.replace(result,"\r","|");
        result=StringUtil.replace(result,"\n","|");
        assertEquals("GET /usr HTTP/1.1|Header: Value|Content-Type: text/plain|Content-Length: 44||"+content,result);
    }

    public void testChunked()
        throws Exception
    {
        Buffer bb=new ByteArrayBuffer(8096);
        Buffer sb=new ByteArrayBuffer(1500);
        ByteArrayEndPoint endp = new ByteArrayEndPoint(new byte[0],4096);
        HttpGenerator generator = new HttpGenerator(new SimpleBuffers(new Buffer[]{sb,bb}),endp, sb.capacity(), bb.capacity());
        
        generator.setRequest("GET","/usr");
        
        HttpFields fields = new HttpFields();
        fields.add("Header","Value");
        fields.add("Content-Type","text/plain");
        
        String content = "The quick brown fox jumped over the lazy dog";

        generator.completeHeader(fields,false);
        
        generator.addContent(new ByteArrayBuffer(content),false);
        generator.flush();
        generator.complete();
        generator.flush();

        String result=endp.getOut().toString();
        result=StringUtil.replace(result,"\r\n","|");
        result=StringUtil.replace(result,"\r","|");
        result=StringUtil.replace(result,"\n","|");
        assertEquals("GET /usr HTTP/1.1|Header: Value|Content-Type: text/plain|Transfer-Encoding: chunked||2C|"+content+"|0||",result);
    }
    
    public void testHTTP()
        throws Exception
    {
        Buffer bb=new ByteArrayBuffer(8096);
        Buffer sb=new ByteArrayBuffer(1500);
        HttpFields fields = new HttpFields();
        ByteArrayEndPoint endp = new ByteArrayEndPoint(new byte[0],4096);
        HttpGenerator hb = new HttpGenerator(new SimpleBuffers(new Buffer[]{sb,bb}),endp, sb.capacity(), bb.capacity());
        Handler handler = new Handler();
        HttpParser parser=null;
        
        // For HTTP version
        for (int v=9;v<=11;v++)
        {
            // For each test result
            for (int r=0;r<tr.length;r++)
            {
                // chunks = 1 to 3
                for (int chunks=1;chunks<=6;chunks++)
                {
                    // For none, keep-alive, close
                    for (int c=0;c<connect.length;c++)
                    {
                        String t="v="+v+",r="+r+",chunks="+chunks+",c="+c+",tr="+tr[r];
                        // System.err.println(t);
                        
                        hb.reset(true);
                        endp.reset();
                        fields.clear();

                        // System.out.println("TEST: "+t);
                        
                        try
                        {
                            tr[r].build(v,hb,connect[c],null,chunks, fields);
                        }
                        catch(IllegalStateException e)
                        {
                            if (v<10 || v==10 && chunks>2)
                                continue;
                            System.err.println(t);
                            throw e;
                        }
                        String request=endp.getOut().toString();
                        // System.out.println(request+(hb.isPersistent()?"...\n":"---\n"));
                        
                        assertTrue(t,hb.isPersistent());
                        
                        if (v==9)
                        {
                            assertEquals(t,"GET /context/path/info\r\n", request);
                            continue;
                        }
                        
                        parser=new HttpParser(new ByteArrayBuffer(request.getBytes()), handler);
                        try
                        {
                            parser.parse();
                        }
                        catch(IOException e)
                        {
                            if (tr[r].body!=null)
                                throw e;
                            continue;
                        }
                        
                        if (tr[r].body!=null)
                            assertEquals(t,tr[r].body, this.content);
                        if (v==10)
                            assertTrue(t,hb.isPersistent() || tr[r].values[1]==null || c==2 || c==0);
                        else
                            assertTrue(t,hb.isPersistent() ||  c==2);
                        
                        assertTrue(t,tr[r].values[1]==null || content.length()==Integer.parseInt(tr[r].values[1]));
                    }
                }
            }
        }
    }

    

    static final String[] headers= { "Content-Type","Content-Length","Connection","Transfer-Encoding","Other"};
    class TR
    {
        String[] values=new String[headers.length];
        String body;
        
        TR(String ct, String cl ,String content)
        {
            values[0]=ct;
            values[1]=cl;
            values[4]="value";
            this.body=content;
        }
        
        void build(int version,HttpGenerator hb, String connection, String te, int chunks, HttpFields fields)
                throws Exception
        {
            values[2]=connection;
            values[3]=te;

            hb.setRequest(HttpMethods.GET,"/context/path/info");
            hb.setVersion(version);
            
            for (int i=0;i<headers.length;i++)
            {
                if (values[i]==null)    
                    continue;
                fields.put(new ByteArrayBuffer(headers[i]),new ByteArrayBuffer(values[i]));
            }
                        
            if (body!=null)
            {
                int inc=1+body.length()/chunks;
                Buffer buf=new ByteArrayBuffer(body);
                View view = new View(buf);
                for (int i=1;i<chunks;i++)
                {
                    view.setPutIndex(i*inc);
                    view.setGetIndex((i-1)*inc);
                    hb.addContent(view,HttpGenerator.MORE);
                    if (hb.isBufferFull() && hb.isState(HttpGenerator.STATE_HEADER))
                        hb.completeHeader(fields, HttpGenerator.MORE);
                    if (i%2==0)
                    {
                        if (hb.isState(HttpGenerator.STATE_HEADER))
                        {
                            if (version<11)
                                fields.addLongField("Content-Length",body.length());
                            hb.completeHeader(fields, HttpGenerator.MORE);
                        }
                        hb.flush();
                    }
                }
                view.setPutIndex(buf.putIndex());
                view.setGetIndex((chunks-1)*inc);
                hb.addContent(view,HttpGenerator.LAST);
                if(hb.isState(HttpGenerator.STATE_HEADER))
                    hb.completeHeader(fields, HttpGenerator.LAST);
            }
            else
            {
                hb.completeHeader(fields, HttpGenerator.LAST);
            }
            hb.complete();
        }
        
        public String toString()
        {
            return "["+values[0]+","+values[1]+","+(body==null?"none":"_content")+"]";
        }
    }
    
    private TR[] tr =
    {
      /* 0 */  new TR(null,null,null),
      /* 1 */  new TR(null,null,CONTENT),
      /* 3 */  new TR(null,""+CONTENT.length(),CONTENT),
      /* 4 */  new TR("text/html",null,null),
      /* 5 */  new TR("text/html",null,CONTENT),
      /* 7 */  new TR("text/html",""+CONTENT.length(),CONTENT),
    };
    

    String content;
    String f0;
    String f1;
    String f2;
    String[] hdr;
    String[] val;
    int h;
    
    class Handler extends HttpParser.EventHandler
    {   
        int index=0;
        
        public void content(Buffer ref)
        {
            if (index == 0)
                content= "";
            content= content.substring(0, index) + ref;
            index+=ref.length();
        }


        public void startRequest(Buffer tok0, Buffer tok1, Buffer tok2)
        {
            h= -1;
            hdr= new String[9];
            val= new String[9];
            f0= tok0.toString();
            f1= tok1.toString();
            if (tok2!=null)
                f2= tok2.toString();
            else
                f2=null;
            index=0;
            // System.out.println(f0+" "+f1+" "+f2);
        }


        /* (non-Javadoc)
         * @see org.mortbay.jetty.EventHandler#startResponse(org.mortbay.io.Buffer, int, org.mortbay.io.Buffer)
         */
        public void startResponse(Buffer version, int status, Buffer reason)
        {
            h= -1;
            hdr= new String[9];
            val= new String[9];
            f0= version.toString();
            f1= ""+status;
            if (reason!=null)
                f2= reason.toString();
            else
                f2=null;
            index=0;
        }

        public void parsedHeader(Buffer name,Buffer value)
        {
            hdr[++h]= name.toString();
            val[h]= value.toString();
        }

        public void headerComplete()
        {
            content= null;
        }

        public void messageComplete(long contentLength)
        {
        }


    }

}
