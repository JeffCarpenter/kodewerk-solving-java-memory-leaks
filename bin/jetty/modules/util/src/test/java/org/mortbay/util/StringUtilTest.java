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

package org.mortbay.util;


import junit.framework.TestCase;

/**
 * @author gregw
 *
 */
public class StringUtilTest extends TestCase
{

    /**
     * Constructor for StringUtilTest.
     * @param arg0
     */
    public StringUtilTest(String arg0)
    {
        super(arg0);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testAsciiToLowerCase()
    {
        String lc="\u0690bc def 1\u06903";
        assertEquals(StringUtil.asciiToLowerCase("\u0690Bc DeF 1\u06903"), lc);
        assertTrue(StringUtil.asciiToLowerCase(lc)==lc);
    }

    public void testStartsWithIgnoreCase()
    {
        
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690b\u0690defg", "\u0690b\u0690"));
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690bcdefg", "\u0690bc"));
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690bcdefg", "\u0690Bc"));
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690Bcdefg", "\u0690bc"));
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690Bcdefg", "\u0690Bc"));
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690bcdefg", ""));
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690bcdefg", null));
        assertTrue(StringUtil.startsWithIgnoreCase("\u0690bcdefg", "\u0690bcdefg"));

        assertFalse(StringUtil.startsWithIgnoreCase(null, "xyz")); 
        assertFalse(StringUtil.startsWithIgnoreCase("\u0690bcdefg", "xyz"));
        assertFalse(StringUtil.startsWithIgnoreCase("\u0690", "xyz")); 
    }

    public void testEndsWithIgnoreCase()
    {
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcd\u0690f\u0690", "\u0690f\u0690"));
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcdefg", "efg"));
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcdefg", "eFg"));
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcdeFg", "efg"));
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcdeFg", "eFg"));
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcdefg", ""));
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcdefg", null));
        assertTrue(StringUtil.endsWithIgnoreCase("\u0690bcdefg", "\u0690bcdefg"));

        assertFalse(StringUtil.endsWithIgnoreCase(null, "xyz")); 
        assertFalse(StringUtil.endsWithIgnoreCase("\u0690bcdefg", "xyz"));
        assertFalse(StringUtil.endsWithIgnoreCase("\u0690", "xyz"));  
    }

    public void testIndexFrom()
    {
        assertEquals(StringUtil.indexFrom("\u0690bcd", "xyz"),-1);
        assertEquals(StringUtil.indexFrom("\u0690bcd", "\u0690bcz"),0);
        assertEquals(StringUtil.indexFrom("\u0690bcd", "bcz"),1);
        assertEquals(StringUtil.indexFrom("\u0690bcd", "dxy"),3);
    }

    public void testReplace()
    {
        String s="\u0690bc \u0690bc \u0690bc";
        assertEquals(StringUtil.replace(s, "\u0690bc", "xyz"),"xyz xyz xyz");
        assertTrue(StringUtil.replace(s,"xyz","pqy")==s);
        
        s=" \u0690bc ";
        assertEquals(StringUtil.replace(s, "\u0690bc", "xyz")," xyz ");
        
    }

    public void testUnquote()
    {
        String uq =" not quoted ";
        assertTrue(StringUtil.unquote(uq)==uq);
        assertEquals(StringUtil.unquote("' quoted string '")," quoted string ");
        assertEquals(StringUtil.unquote("\" quoted string \"")," quoted string ");
        assertEquals(StringUtil.unquote("' quoted\"string '")," quoted\"string ");
        assertEquals(StringUtil.unquote("\" quoted'string \"")," quoted'string ");
    }


    public void testNonNull()
    {
        String nn="";
        assertTrue(nn==StringUtil.nonNull(nn));
        assertEquals("",StringUtil.nonNull(null));
    }

    /*
     * Test for boolean equals(String, char[], int, int)
     */
    public void testEqualsStringcharArrayintint()
    {
        assertTrue(StringUtil.equals("\u0690bc", new char[] {'x','\u0690','b','c','z'},1,3));
        assertFalse(StringUtil.equals("axc", new char[] {'x','a','b','c','z'},1,3));
    }

    public void testAppend()
    {
        StringBuffer buf = new StringBuffer();
        buf.append('a');
        StringUtil.append(buf, "abc", 1, 1);
        StringUtil.append(buf, (byte)12, 16);
        StringUtil.append(buf, (byte)16, 16);
        StringUtil.append(buf, (byte)-1, 16);
        StringUtil.append(buf, (byte)-16, 16);
        assertEquals("ab0c10fff0",buf.toString());
        
    }
    
    public static void main(String[] arg) throws Exception
    {
        String string = "Now \u0690xxxxxxxx";
        System.err.println(string);
        byte[] bytes=string.getBytes("UTF-8");
        System.err.println(new String(bytes));
        System.err.println(bytes.length);
        long calc=0;
        Utf8StringBuffer strbuf = new Utf8StringBuffer(bytes.length);
        for (int i=0;i<10;i++)
        {
            long s1=System.currentTimeMillis();
            for (int j=1000000; j-->0;)
            {
                calc+=new String(bytes,0,bytes.length,"UTF-8").hashCode();
            }
            long s2=System.currentTimeMillis();
            for (int j=1000000; j-->0;)
            {
                calc+=StringUtil.toUTF8String(bytes,0,bytes.length).hashCode();
            }
            long s3=System.currentTimeMillis();
            for (int j=1000000; j-->0;)
            {
                Utf8StringBuffer buffer = new Utf8StringBuffer(bytes.length);
                buffer.append(bytes,0,bytes.length);
                calc+=buffer.toString().hashCode();
            }
            long s4=System.currentTimeMillis();
            for (int j=1000000; j-->0;)
            {
                strbuf.reset();
                strbuf.append(bytes,0,bytes.length);
                calc+=strbuf.toString().hashCode();
            }
            long s5=System.currentTimeMillis();
            
            System.err.println((s2-s1)+", "+(s3-s2)+", "+(s4-s3)+", "+(s5-s4));
        }
        System.err.println(calc);
    }
}
