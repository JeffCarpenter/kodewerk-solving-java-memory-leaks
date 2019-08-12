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

import java.io.UnsupportedEncodingException;

// ====================================================================
/** Fast String Utilities.
 *
 * These string utilities provide both conveniance methods and
 * performance improvements over most standard library versions. The
 * main aim of the optimizations is to avoid object creation unless
 * absolutely required.
 *
 * @author Greg Wilkins (gregw)
 */
public class StringUtil
{
    public static final String CRLF="\015\012";
    public static final String __LINE_SEPARATOR=
        System.getProperty("line.separator","\n");
    
    public static final String __ISO_8859_1;
    static
    {
        String iso=System.getProperty("ISO_8859_1");
        if (iso==null)
        {
            try{
                new String(new byte[]{(byte)20},"ISO-8859-1");
                iso="ISO-8859-1";
            }
            catch(java.io.UnsupportedEncodingException e)
            {
                iso="ISO8859_1";
            }        
        }
        __ISO_8859_1=iso;
    }
    
    public final static String __UTF8="UTF-8";
    public final static String __UTF8Alt="UTF8";
    public final static String __UTF16="UTF-16";
    
    
    private static char[] lowercases = {
          '\000','\001','\002','\003','\004','\005','\006','\007',
          '\010','\011','\012','\013','\014','\015','\016','\017',
          '\020','\021','\022','\023','\024','\025','\026','\027',
          '\030','\031','\032','\033','\034','\035','\036','\037',
          '\040','\041','\042','\043','\044','\045','\046','\047',
          '\050','\051','\052','\053','\054','\055','\056','\057',
          '\060','\061','\062','\063','\064','\065','\066','\067',
          '\070','\071','\072','\073','\074','\075','\076','\077',
          '\100','\141','\142','\143','\144','\145','\146','\147',
          '\150','\151','\152','\153','\154','\155','\156','\157',
          '\160','\161','\162','\163','\164','\165','\166','\167',
          '\170','\171','\172','\133','\134','\135','\136','\137',
          '\140','\141','\142','\143','\144','\145','\146','\147',
          '\150','\151','\152','\153','\154','\155','\156','\157',
          '\160','\161','\162','\163','\164','\165','\166','\167',
          '\170','\171','\172','\173','\174','\175','\176','\177' };

    /* ------------------------------------------------------------ */
    /**
     * fast lower case conversion. Only works on ascii (not unicode)
     * @param s the string to convert
     * @return a lower case version of s
     */
    public static String asciiToLowerCase(String s)
    {
        char[] c = null;
        int i=s.length();

        // look for first conversion
        while (i-->0)
        {
            char c1=s.charAt(i);
            if (c1<=127)
            {
                char c2=lowercases[c1];
                if (c1!=c2)
                {
                    c=s.toCharArray();
                    c[i]=c2;
                    break;
                }
            }
        }

        while (i-->0)
        {
            if(c[i]<=127)
                c[i] = lowercases[c[i]];
        }
        
        return c==null?s:new String(c);
    }


    /* ------------------------------------------------------------ */
    public static boolean startsWithIgnoreCase(String s,String w)
    {
        if (w==null)
            return true;
        
        if (s==null || s.length()<w.length())
            return false;
        
        for (int i=0;i<w.length();i++)
        {
            char c1=s.charAt(i);
            char c2=w.charAt(i);
            if (c1!=c2)
            {
                if (c1<=127)
                    c1=lowercases[c1];
                if (c2<=127)
                    c2=lowercases[c2];
                if (c1!=c2)
                    return false;
            }
        }
        return true;
    }
    
    /* ------------------------------------------------------------ */
    public static boolean endsWithIgnoreCase(String s,String w)
    {
        if (w==null)
            return true;

        if (s==null)
            return false;
            
        int sl=s.length();
        int wl=w.length();
        
        if (sl<wl)
            return false;
        
        for (int i=wl;i-->0;)
        {
            char c1=s.charAt(--sl);
            char c2=w.charAt(i);
            if (c1!=c2)
            {
                if (c1<=127)
                    c1=lowercases[c1];
                if (c2<=127)
                    c2=lowercases[c2];
                if (c1!=c2)
                    return false;
            }
        }
        return true;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * returns the next index of a character from the chars string
     */
    public static int indexFrom(String s,String chars)
    {
        for (int i=0;i<s.length();i++)
           if (chars.indexOf(s.charAt(i))>=0)
              return i;
        return -1;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * replace substrings within string.
     */
    public static String replace(String s, String sub, String with)
    {
        int c=0;
        int i=s.indexOf(sub,c);
        if (i == -1)
            return s;
    
        StringBuffer buf = new StringBuffer(s.length()+with.length());

        synchronized(buf)
        {
            do
            {
                buf.append(s.substring(c,i));
                buf.append(with);
                c=i+sub.length();
            } while ((i=s.indexOf(sub,c))!=-1);
            
            if (c<s.length())
                buf.append(s.substring(c,s.length()));
            
            return buf.toString();
        }
    }


    /* ------------------------------------------------------------ */
    /** Remove single or double quotes.
     */
    public static String unquote(String s)
    {
        return QuotedStringTokenizer.unquote(s);
    }


    /* ------------------------------------------------------------ */
    /** Append substring to StringBuffer 
     * @param buf StringBuffer to append to
     * @param s String to append from
     * @param offset The offset of the substring
     * @param length The length of the substring
     */
    public static void append(StringBuffer buf,
                              String s,
                              int offset,
                              int length)
    {
        synchronized(buf)
        {
            int end=offset+length;
            for (int i=offset; i<end;i++)
            {
                if (i>=s.length())
                    break;
                buf.append(s.charAt(i));
            }
        }
    }

    
    /* ------------------------------------------------------------ */
    /**
     * append hex digit
     * 
     */
    public static void append(StringBuffer buf,byte b,int base)
    {
        int bi=0xff&b;
        int c='0'+(bi/base)%base;
        if (c>'9')
            c= 'a'+(c-'0'-10);
        buf.append((char)c);
        c='0'+bi%base;
        if (c>'9')
            c= 'a'+(c-'0'-10);
        buf.append((char)c);
    }

    
    /* ------------------------------------------------------------ */
    public static void append2digits(StringBuffer buf,int i)
    {
        if (i<100)
        {
            buf.append((char)(i/10+'0'));
            buf.append((char)(i%10+'0'));
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Return a non null string.
     * @param s String
     * @return The string passed in or empty string if it is null. 
     */
    public static String nonNull(String s)
    {
        if (s==null)
            return "";
        return s;
    }
    
    /* ------------------------------------------------------------ */
    public static boolean equals(String s,char[] buf, int offset, int length)
    {
        if (s.length()!=length)
            return false;
        for (int i=0;i<length;i++)
            if (buf[offset+i]!=s.charAt(i))
                return false;
        return true;
    }

    /* ------------------------------------------------------------ */
    public static String toUTF8String(byte[] b,int offset,int length)
    {
        try
        {
            if (length<32)
            {
                Utf8StringBuffer buffer = new Utf8StringBuffer(length);
                buffer.append(b,offset,length);
                return buffer.toString();
            }
            
            return new String(b,offset,length,__UTF8);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    public static String toString(byte[] b,int offset,int length,String charset)
    {
        if (charset==null || StringUtil.isUTF8(charset))
            return toUTF8String(b,offset,length);
        
        try
        {
            return new String(b,offset,length,charset);
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            return null;
        }
    }


    /* ------------------------------------------------------------ */
    public static boolean isUTF8(String charset)
    {
        return charset==__UTF8||__UTF8.equalsIgnoreCase(charset)||__UTF8Alt.equalsIgnoreCase(charset);
    }


    /* ------------------------------------------------------------ */
    public static String printable(String name)
    {
        if (name==null)
            return null;
        StringBuffer buf = new StringBuffer(name.length());
        for (int i=0;i<name.length();i++)
        {
            char c=name.charAt(i);
            if (!Character.isISOControl(c))
                buf.append(c);
        }
        return buf.toString();
    }
    
    
}
