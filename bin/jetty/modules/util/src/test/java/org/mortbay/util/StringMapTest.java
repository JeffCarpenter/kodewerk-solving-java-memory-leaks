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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;


/**
 * @author gregw
 *
 */
public class StringMapTest extends TestCase
{
    StringMap m0;
    StringMap m1;
    StringMap m5;
    StringMap m5i;

    /**
     * Constructor for StringMapTest.
     * @param arg0
     */
    public StringMapTest(String arg0)
    {
        super(arg0);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        m0=new StringMap();
        m1=new StringMap(false);
        m1.put("abc", "0");
        
        m5=new StringMap(false);
        m5.put("a", "0");
        m5.put("ab", "1");
        m5.put("abc", "2");
        m5.put("abb", "3");
        m5.put("bbb", "4");
        
        m5i=new StringMap(true); 
        m5i.put(null, "0");
        m5i.put("ab", "1");
        m5i.put("abc", "2");
        m5i.put("abb", "3");
        m5i.put("bbb", null);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testSize()
    {
        assertEquals(0, m0.size());
        assertEquals(1, m1.size());
        assertEquals(5, m5.size());
        assertEquals(5, m5i.size());
        
        m1.remove("abc");
        m5.remove("abc");
        m5.put("bbb","x");
        m5i.put("ABC", "x");
        assertEquals(0, m0.size());
        assertEquals(0, m1.size());
        assertEquals(4, m5.size());
        assertEquals(5, m5i.size());
    }

    public void testIsEmpty()
    {
        assertTrue(m0.isEmpty());
        assertFalse(m1.isEmpty());
        assertFalse(m5.isEmpty());
        assertFalse(m5i.isEmpty());
    }

    public void testClear()
    {
        m0.clear();
        m1.clear();
        m5.clear();
        m5i.clear();
        assertTrue(m0.isEmpty());
        assertTrue(m1.isEmpty());
        assertTrue(m5.isEmpty());
        assertTrue(m5i.isEmpty());
        assertEquals(null,m1.get("abc"));
        assertEquals(null,m5.get("abc"));
        assertEquals(null,m5i.get("abc"));
    }


    /*
     * Test for Object put(Object, Object)
     */
    public void testPutGet()
    {
        assertEquals("2",m5.get("abc"));
        assertEquals(null,m5.get("aBc"));
        assertEquals("2",m5i.get("abc"));
        assertEquals("2",m5i.get("aBc"));
        
        m5.put(null,"x");
        m5.put("aBc", "x");
        m5i.put("AbC", "x");

        StringBuffer buffer=new StringBuffer();
        buffer.append("aBc");
        assertEquals("2",m5.get("abc"));
        assertEquals("x",m5.get(buffer));
        assertEquals("x",m5i.get((Object)"abc"));
        assertEquals("x",m5i.get("aBc"));
        
        assertEquals("x",m5.get(null));
        assertEquals("0",m5i.get(null));
        
    }



    /*
     * Test for Map.Entry getEntry(String, int, int)
     */
    public void testGetEntryStringintint()
    {
        Map.Entry entry;
        
        entry=m5.getEntry("xabcyz",1,3);
        assertTrue(entry!=null);
        assertEquals("abc",entry.getKey());
        assertEquals("2",entry.getValue());
        
        entry=m5.getBestEntry("xabcyz".getBytes(),1,5);
        assertTrue(entry!=null);
        assertEquals("abc",entry.getKey());
        assertEquals("2",entry.getValue());
        
        entry=m5.getEntry("xaBcyz",1,3);
        assertTrue(entry==null);
        
        entry=m5i.getEntry("xaBcyz",1,3);
        assertTrue(entry!=null);
        assertEquals("abc",entry.getKey());
        assertEquals("2",entry.getValue());
        entry.setValue("x");
        assertEquals("{[c:abc=x]}",entry.toString());
        
        entry=m5i.getEntry((String)null,0,0);
        assertTrue(entry!=null);
        assertEquals(null,entry.getKey());
        assertEquals("0",entry.getValue());
        entry.setValue("x");
        assertEquals("[:null=x]",entry.toString());

    }

    /*
     * Test for Map.Entry getEntry(char[], int, int)
     */
    public void testGetEntrycharArrayintint()
    {
        char[] xabcyz = {'x','a','b','c','y','z'};
        char[] xaBcyz = {'x','a','B','c','y','z'};
        Map.Entry entry;
        
        entry=m5.getEntry(xabcyz,1,3);
        assertTrue(entry!=null);
        assertEquals("abc",entry.getKey());
        assertEquals("2",entry.getValue());
        
        entry=m5.getEntry(xaBcyz,1,3);
        assertTrue(entry==null);
        
        entry=m5i.getEntry(xaBcyz,1,3);
        assertTrue(entry!=null);
        assertEquals("abc",entry.getKey());
        assertEquals("2",entry.getValue());
    }

    /*
     * Test for Object remove(Object)
     */
    public void testRemove()
    {
        m0.remove("abc");
        m1.remove("abc");
        m5.remove("aBc");
        m5.remove("bbb");
        m5i.remove("aBc");
        m5i.remove(null);

        assertEquals(0, m0.size());
        assertEquals(0, m1.size());
        assertEquals(4, m5.size());
        assertEquals(3, m5i.size());

        assertEquals("2",m5.get("abc"));
        assertEquals(null,m5.get("bbb"));
        assertEquals(null,m5i.get("AbC"));
        assertEquals(null,m5i.get(null));
    }


    /*
     * Test for Set entrySet()
     */
    public void testEntrySet()
    {
        Set es0=m0.entrySet();
        Set es1=m1.entrySet();
        Set es5=m5.entrySet();
        assertEquals(0, es0.size());
        assertEquals(1, es1.size());
        assertEquals(5, es5.size());
    }

    /*
     * Test for boolean containsKey(Object)
     */
    public void testContainsKey()
    {
        assertTrue(m5.containsKey("abc"));
        assertTrue(!m5.containsKey("aBc"));
        assertTrue(m5.containsKey("bbb"));
        assertTrue(!m5.containsKey("xyz"));
        
        assertTrue(m5i.containsKey(null));
        assertTrue(m5i.containsKey("abc"));
        assertTrue(m5i.containsKey("aBc"));
        assertTrue(m5i.containsKey("ABC"));
    }

    public void testWriteExternal()
        throws Exception
    {
        ByteArrayOutputStream bout= new ByteArrayOutputStream();
        ObjectOutputStream oo=new ObjectOutputStream(bout);
        ObjectInputStream oi;
        
        oo.writeObject(m0);
        oo.writeObject(m1);
        oo.writeObject(m5);
        oo.writeObject(m5i);
        
        oi=new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
        m0=(StringMap)oi.readObject();
        m1=(StringMap)oi.readObject();
        m5=(StringMap)oi.readObject();
        m5i=(StringMap)oi.readObject();
        testSize();
        
        oi=new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
        m0=(StringMap)oi.readObject();
        m1=(StringMap)oi.readObject();
        m5=(StringMap)oi.readObject();
        m5i=(StringMap)oi.readObject();
        testPutGet();
        
    }
    
    public void testToString()
    {
        assertEquals("{}",m0.toString());
        assertEquals("{abc=0}",m1.toString());
        assertTrue(m5.toString().indexOf("abc=2")>0);
    }

}
