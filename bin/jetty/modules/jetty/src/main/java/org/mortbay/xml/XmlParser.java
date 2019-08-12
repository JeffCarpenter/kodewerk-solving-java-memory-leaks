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

package org.mortbay.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mortbay.log.Log;
import org.mortbay.util.LazyList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/*--------------------------------------------------------------*/
/**
 * XML Parser wrapper. This class wraps any standard JAXP1.1 parser with convieniant error and
 * entity handlers and a mini dom-like document tree.
 * <P>
 * By default, the parser is created as a validating parser only if xerces is present. This can be 
 * configured by setting the "org.mortbay.xml.XmlParser.Validating" system property.
 * 
 * @author Greg Wilkins (gregw)
 */
public class XmlParser
{
    private Map _redirectMap = new HashMap();
    private SAXParser _parser;
    private Map _observerMap;
    private Stack _observers = new Stack();
    private String _xpath;
    private Object _xpaths;
    private String _dtd;

    /* ------------------------------------------------------------ */
    /**
     * Construct
     */
    public XmlParser()
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        boolean validating_dft = factory.getClass().toString().startsWith("org.apache.xerces.");
        String validating_prop = System.getProperty("org.mortbay.xml.XmlParser.Validating", validating_dft ? "true" : "false");
        boolean notValidating = Boolean.getBoolean("org.mortbay.xml.XmlParser.NotValidating"); // deprecated!
        boolean validating = !notValidating && Boolean.valueOf(validating_prop).booleanValue();
        setValidating(validating);
    }

    /* ------------------------------------------------------------ */
    /**
     * Constructor.
     */
    public XmlParser(boolean validating)
    {
        setValidating(validating);
    }
    
    /* ------------------------------------------------------------ */
    public void setValidating(boolean validating)
    {
        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(validating);
            _parser = factory.newSAXParser();
            
            try
            {
                if (validating)
                    _parser.getXMLReader().setFeature("http://apache.org/xml/features/validation/schema", validating);
            }
            catch (Exception e)
            {
                if (validating)
                    Log.warn("Schema validation may not be supported: ", e);
                else
                    Log.ignore(e);
            }

            _parser.getXMLReader().setFeature("http://xml.org/sax/features/validation", validating);
            _parser.getXMLReader().setFeature("http://xml.org/sax/features/namespaces", true);
            _parser.getXMLReader().setFeature("http://xml.org/sax/features/namespace-prefixes", false);  
        }
        catch (Exception e)
        {
            Log.warn(Log.EXCEPTION, e);
            throw new Error(e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param entity
     */
    public synchronized void redirectEntity(String name, URL entity)
    {
        if (entity != null)
            _redirectMap.put(name, entity);
    }

    /* ------------------------------------------------------------ */
    /**
     * 
     * @return Returns the xpath.
     */
    public String getXpath()
    {
        return _xpath;
    }

    /* ------------------------------------------------------------ */
    /**
     * Set an XPath A very simple subset of xpath is supported to select a partial tree. Currently
     * only path like "/node1/nodeA | /node1/nodeB" are supported.
     * 
     * @param xpath The xpath to set.
     */
    public void setXpath(String xpath)
    {
        _xpath = xpath;
        StringTokenizer tok = new StringTokenizer(xpath, "| ");
        while (tok.hasMoreTokens())
            _xpaths = LazyList.add(_xpaths, tok.nextToken());
    }

    /* ------------------------------------------------------------ */
    public String getDTD()
    {
        return _dtd;
    }

    /* ------------------------------------------------------------ */
    /**
     * Add a ContentHandler. Add an additional _content handler that is triggered on a tag name. SAX
     * events are passed to the ContentHandler provided from a matching start element to the
     * corresponding end element. Only a single _content handler can be registered against each tag.
     * 
     * @param trigger Tag local or q name.
     * @param observer SAX ContentHandler
     */
    public synchronized void addContentHandler(String trigger, ContentHandler observer)
    {
        if (_observerMap == null)
            _observerMap = new HashMap();
        _observerMap.put(trigger, observer);
    }

    /* ------------------------------------------------------------ */
    public synchronized Node parse(InputSource source) throws IOException, SAXException
    {
        _dtd=null;
        Handler handler = new Handler();
        XMLReader reader = _parser.getXMLReader();
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);
        reader.setEntityResolver(handler);
        if (Log.isDebugEnabled())
            Log.debug("parsing: sid=" + source.getSystemId() + ",pid=" + source.getPublicId());
        _parser.parse(source, handler);
        if (handler._error != null)
            throw handler._error;
        Node doc = (Node) handler._top.get(0);
        handler.clear();
        return doc;
    }

    /* ------------------------------------------------------------ */
    /**
     * Parse String URL.
     */
    public synchronized Node parse(String url) throws IOException, SAXException
    {
        if (Log.isDebugEnabled())
            Log.debug("parse: " + url);
        return parse(new InputSource(url));
    }

    /* ------------------------------------------------------------ */
    /**
     * Parse File.
     */
    public synchronized Node parse(File file) throws IOException, SAXException
    {
        if (Log.isDebugEnabled())
            Log.debug("parse: " + file);
        return parse(new InputSource(file.toURL().toString()));
    }

    /* ------------------------------------------------------------ */
    /**
     * Parse InputStream.
     */
    public synchronized Node parse(InputStream in) throws IOException, SAXException
    {
        _dtd=null;
        Handler handler = new Handler();
        XMLReader reader = _parser.getXMLReader();
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);
        reader.setEntityResolver(handler);
        _parser.parse(new InputSource(in), handler);
        if (handler._error != null)
            throw handler._error;
        Node doc = (Node) handler._top.get(0);
        handler.clear();
        return doc;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class NoopHandler extends DefaultHandler
    {
        Handler _next;
        int _depth;

        NoopHandler(Handler next)
        {
            this._next = next;
        }

        /* ------------------------------------------------------------ */
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
        {
            _depth++;
        }

        /* ------------------------------------------------------------ */
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            if (_depth == 0)
                _parser.getXMLReader().setContentHandler(_next);
            else
                _depth--;
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Handler extends DefaultHandler
    {
        Node _top = new Node(null, null, null);
        SAXParseException _error;
        private Node _context = _top;
        private NoopHandler _noop;

        Handler()
        {
            _noop = new NoopHandler(this);
        }

        /* ------------------------------------------------------------ */
        void clear()
        {
            _top = null;
            _error = null;
            _context = null;
        }

        /* ------------------------------------------------------------ */
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException
        {
            String name = (uri == null || uri.equals("")) ? qName : localName;
            Node node = new Node(_context, name, attrs);
            

            // check if the node matches any xpaths set?
            if (_xpaths != null)
            {
                String path = node.getPath();
                boolean match = false;
                for (int i = LazyList.size(_xpaths); !match && i-- > 0;)
                {
                    String xpath = (String) LazyList.get(_xpaths, i);

                    match = path.equals(xpath) || xpath.startsWith(path) && xpath.length() > path.length() && xpath.charAt(path.length()) == '/';
                }

                if (match)
                {
                    _context.add(node);
                    _context = node;
                }
                else
                {
                    _parser.getXMLReader().setContentHandler(_noop);
                }
            }
            else
            {
                _context.add(node);
                _context = node;
            }

            ContentHandler observer = null;
            if (_observerMap != null)
                observer = (ContentHandler) _observerMap.get(name);
            _observers.push(observer);

            for (int i = 0; i < _observers.size(); i++)
                if (_observers.get(i) != null)
                    ((ContentHandler) _observers.get(i)).startElement(uri, localName, qName, attrs);
        }

        /* ------------------------------------------------------------ */
        public void endElement(String uri, String localName, String qName) throws SAXException
        {
            _context = _context._parent;
            for (int i = 0; i < _observers.size(); i++)
                if (_observers.get(i) != null)
                    ((ContentHandler) _observers.get(i)).endElement(uri, localName, qName);
            _observers.pop();
        }

        /* ------------------------------------------------------------ */
        public void ignorableWhitespace(char buf[], int offset, int len) throws SAXException
        {
            for (int i = 0; i < _observers.size(); i++)
                if (_observers.get(i) != null)
                    ((ContentHandler) _observers.get(i)).ignorableWhitespace(buf, offset, len);
        }

        /* ------------------------------------------------------------ */
        public void characters(char buf[], int offset, int len) throws SAXException
        {
            _context.add(new String(buf, offset, len));
            for (int i = 0; i < _observers.size(); i++)
                if (_observers.get(i) != null)
                    ((ContentHandler) _observers.get(i)).characters(buf, offset, len);
        }

        /* ------------------------------------------------------------ */
        public void warning(SAXParseException ex)
        {
            Log.debug(Log.EXCEPTION, ex);
            Log.warn("WARNING@" + getLocationString(ex) + " : " + ex.toString());
        }

        /* ------------------------------------------------------------ */
        public void error(SAXParseException ex) throws SAXException
        {
            // Save error and continue to report other errors
            if (_error == null)
                _error = ex;
            Log.debug(Log.EXCEPTION, ex);
            Log.warn("ERROR@" + getLocationString(ex) + " : " + ex.toString());
        }

        /* ------------------------------------------------------------ */
        public void fatalError(SAXParseException ex) throws SAXException
        {
            _error = ex;
            Log.debug(Log.EXCEPTION, ex);
            Log.warn("FATAL@" + getLocationString(ex) + " : " + ex.toString());
            throw ex;
        }

        /* ------------------------------------------------------------ */
        private String getLocationString(SAXParseException ex)
        {
            return ex.getSystemId() + " line:" + ex.getLineNumber() + " col:" + ex.getColumnNumber();
        }

        /* ------------------------------------------------------------ */
        public InputSource resolveEntity(String pid, String sid)
        {
            if (Log.isDebugEnabled())
                Log.debug("resolveEntity(" + pid + ", " + sid + ")");

            if (sid!=null && sid.endsWith(".dtd"))
                _dtd=sid;
            
            URL entity = null;
            if (pid != null)
                entity = (URL) _redirectMap.get(pid);
            if (entity == null)
                entity = (URL) _redirectMap.get(sid);
            if (entity == null)
            {
                String dtd = sid;
                if (dtd.lastIndexOf('/') >= 0)
                    dtd = dtd.substring(dtd.lastIndexOf('/') + 1);

                if (Log.isDebugEnabled())
                    Log.debug("Can't exact match entity in redirect map, trying " + dtd);
                entity = (URL) _redirectMap.get(dtd);
            }

            if (entity != null)
            {
                try
                {
                    InputStream in = entity.openStream();
                    if (Log.isDebugEnabled())
                        Log.debug("Redirected entity " + sid + " --> " + entity);
                    InputSource is = new InputSource(in);
                    is.setSystemId(sid);
                    return is;
                }
                catch (IOException e)
                {
                    Log.ignore(e);
                }
            }
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * XML Attribute.
     */
    public static class Attribute
    {
        private String _name;
        private String _value;

        Attribute(String n, String v)
        {
            _name = n;
            _value = v;
        }

        public String getName()
        {
            return _name;
        }

        public String getValue()
        {
            return _value;
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * XML Node. Represents an XML element with optional attributes and ordered content.
     */
    public static class Node extends AbstractList
    {
        Node _parent;
        private ArrayList _list;
        private String _tag;
        private Attribute[] _attrs;
        private boolean _lastString = false;
        private String _path;

        /* ------------------------------------------------------------ */
        Node(Node parent, String tag, Attributes attrs)
        {
            _parent = parent;
            _tag = tag;

            if (attrs != null)
            {
                _attrs = new Attribute[attrs.getLength()];
                for (int i = 0; i < attrs.getLength(); i++)
                {
                    String name = attrs.getLocalName(i);
                    if (name == null || name.equals(""))
                        name = attrs.getQName(i);
                    _attrs[i] = new Attribute(name, attrs.getValue(i));
                }
            }
        }

        /* ------------------------------------------------------------ */
        public Node getParent()
        {
            return _parent;
        }

        /* ------------------------------------------------------------ */
        public String getTag()
        {
            return _tag;
        }

        /* ------------------------------------------------------------ */
        public String getPath()
        {
            if (_path == null)
            {
                if (getParent() != null && getParent().getTag() != null)
                    _path = getParent().getPath() + "/" + _tag;
                else
                    _path = "/" + _tag;
            }
            return _path;
        }

        /* ------------------------------------------------------------ */
        /**
         * Get an array of element attributes.
         */
        public Attribute[] getAttributes()
        {
            return _attrs;
        }

        /* ------------------------------------------------------------ */
        /**
         * Get an element attribute.
         * 
         * @return attribute or null.
         */
        public String getAttribute(String name)
        {
            return getAttribute(name, null);
        }

        /* ------------------------------------------------------------ */
        /**
         * Get an element attribute.
         * 
         * @return attribute or null.
         */
        public String getAttribute(String name, String dft)
        {
            if (_attrs == null || name == null)
                return dft;
            for (int i = 0; i < _attrs.length; i++)
                if (name.equals(_attrs[i].getName()))
                    return _attrs[i].getValue();
            return dft;
        }

        /* ------------------------------------------------------------ */
        /**
         * Get the number of children nodes.
         */
        public int size()
        {
            if (_list != null)
                return _list.size();
            return 0;
        }

        /* ------------------------------------------------------------ */
        /**
         * Get the ith child node or content.
         * 
         * @return Node or String.
         */
        public Object get(int i)
        {
            if (_list != null)
                return _list.get(i);
            return null;
        }

        /* ------------------------------------------------------------ */
        /**
         * Get the first child node with the tag.
         * 
         * @param tag
         * @return Node or null.
         */
        public Node get(String tag)
        {
            if (_list != null)
            {
                for (int i = 0; i < _list.size(); i++)
                {
                    Object o = _list.get(i);
                    if (o instanceof Node)
                    {
                        Node n = (Node) o;
                        if (tag.equals(n._tag))
                            return n;
                    }
                }
            }
            return null;
        }

        /* ------------------------------------------------------------ */
        public void add(int i, Object o)
        {
            if (_list == null)
                _list = new ArrayList();
            if (o instanceof String)
            {
                if (_lastString)
                {
                    int last = _list.size() - 1;
                    _list.set(last, (String) _list.get(last) + o);
                }
                else
                    _list.add(i, o);
                _lastString = true;
            }
            else
            {
                _lastString = false;
                _list.add(i, o);
            }
        }

        /* ------------------------------------------------------------ */
        public void clear()
        {
            if (_list != null)
                _list.clear();
            _list = null;
        }

        /* ------------------------------------------------------------ */
        /**
         * Get a tag as a string.
         * 
         * @param tag The tag to get
         * @param tags IF true, tags are included in the value.
         * @param trim If true, trim the value.
         * @return results of get(tag).toString(tags).
         */
        public String getString(String tag, boolean tags, boolean trim)
        {
            Node node = get(tag);
            if (node == null)
                return null;
            String s = node.toString(tags);
            if (s != null && trim)
                s = s.trim();
            return s;
        }

        /* ------------------------------------------------------------ */
        public synchronized String toString()
        {
            return toString(true);
        }

        /* ------------------------------------------------------------ */
        /**
         * Convert to a string.
         * 
         * @param tag If false, only _content is shown.
         */
        public synchronized String toString(boolean tag)
        {
            StringBuffer buf = new StringBuffer();
            synchronized (buf)
            {
                toString(buf, tag);
                return buf.toString();
            }
        }

        /* ------------------------------------------------------------ */
        /**
         * Convert to a string.
         * 
         * @param tag If false, only _content is shown.
         */
        public synchronized String toString(boolean tag, boolean trim)
        {
            String s = toString(tag);
            if (s != null && trim)
                s = s.trim();
            return s;
        }

        /* ------------------------------------------------------------ */
        private synchronized void toString(StringBuffer buf, boolean tag)
        {
            if (tag)
            {
                buf.append("<");
                buf.append(_tag);

                if (_attrs != null)
                {
                    for (int i = 0; i < _attrs.length; i++)
                    {
                        buf.append(' ');
                        buf.append(_attrs[i].getName());
                        buf.append("=\"");
                        buf.append(_attrs[i].getValue());
                        buf.append("\"");
                    }
                }
            }

            if (_list != null)
            {
                if (tag)
                    buf.append(">");
                for (int i = 0; i < _list.size(); i++)
                {
                    Object o = _list.get(i);
                    if (o == null)
                        continue;
                    if (o instanceof Node)
                        ((Node) o).toString(buf, tag);
                    else
                        buf.append(o.toString());
                }
                if (tag)
                {
                    buf.append("</");
                    buf.append(_tag);
                    buf.append(">");
                }
            }
            else if (tag)
                buf.append("/>");
        }

        /* ------------------------------------------------------------ */
        /**
         * Iterator over named child nodes.
         * 
         * @param tag The tag of the nodes.
         * @return Iterator over all child nodes with the specified tag.
         */
        public Iterator iterator(final String tag)
        {
            return new Iterator()
            {
                int c = 0;
                Node _node;

                /* -------------------------------------------------- */
                public boolean hasNext()
                {
                    if (_node != null)
                        return true;
                    while (_list != null && c < _list.size())
                    {
                        Object o = _list.get(c);
                        if (o instanceof Node)
                        {
                            Node n = (Node) o;
                            if (tag.equals(n._tag))
                            {
                                _node = n;
                                return true;
                            }
                        }
                        c++;
                    }
                    return false;
                }

                /* -------------------------------------------------- */
                public Object next()
                {
                    try
                    {
                        if (hasNext())
                            return _node;
                        throw new NoSuchElementException();
                    }
                    finally
                    {
                        _node = null;
                        c++;
                    }
                }

                /* -------------------------------------------------- */
                public void remove()
                {
                    throw new UnsupportedOperationException("Not supported");
                }
            };
        }
    }
}
