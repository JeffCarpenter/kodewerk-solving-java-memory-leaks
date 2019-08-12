// ========================================================================
// Copyright 2006-2007 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferCache.CachedBuffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.HttpSchemes;
import org.mortbay.jetty.HttpURI;
import org.mortbay.jetty.HttpVersions;
import org.mortbay.log.Log;


/**
 * An HTTP client API that encapsulates Exchange with a HTTP server.
 *
 * This object encapsulates:<ul>
 * <li>The HTTP server. (see {@link #setAddress(InetSocketAddress)} or {@link #setURL(String)})
 * <li>The HTTP request method, URI and HTTP version (see {@link #setMethod(String)}, {@link #setURI(String)}, and {@link #setVersion(int)}
 * <li>The Request headers (see {@link #addRequestHeader(String, String)} or {@link #setRequestHeader(String, String)})
 * <li>The Request content (see {@link #setRequestContent(Buffer)} or {@link #setRequestContentSource(InputStream)})
 * <li>The status of the exchange (see {@link #getStatus()})
 * <li>Callbacks to handle state changes (see the onXxx methods such as {@link #onRequestComplete()} or {@link #onResponseComplete()})
 * <li>The ability to intercept callbacks (see {@link #setEventListener(HttpEventListener)}
 * </ul>
 *
 * The HttpExchange class is intended to be used by a developer wishing to have close asynchronous
 * interaction with the the exchange.  Typically a developer will extend the HttpExchange class with a derived
 * class that implements some or all of the onXxx callbacks.  There are also some predefined HttpExchange subtypes
 * that can be used as a basis (see {@link ContentExchange} and {@link CachedExchange}.
 *
 * <p>Typically the HttpExchange is passed to a the {@link HttpClient#send(HttpExchange)} method, which in
 * turn selects a {@link HttpDestination} and calls it's {@link HttpDestination#send(HttpExchange), which
 * then creates or selects a {@link HttpConnection} and calls its {@link HttpConnection#send(HttpExchange).
 * A developer may wish to directly call send on the destination or connection if they wish to bypass
 * some handling provided (eg Cookie handling in the HttpDestination).
 *
 * <p>In some circumstances, the HttpClient or HttpDestination may wish to retry a HttpExchange (eg. failed
 * pipeline request, authentication retry or redirection).  In such cases, the HttpClient and/or HttpDestination
 * may insert their own HttpExchangeListener to intercept and filter the call backs intended for the
 * HttpExchange.
 *
 * @author gregw
 * @author Guillaume Nodet
 */
public class HttpExchange
{
    public static final int STATUS_START = 0;
    public static final int STATUS_WAITING_FOR_CONNECTION = 1;
    public static final int STATUS_WAITING_FOR_COMMIT = 2;
    public static final int STATUS_SENDING_REQUEST = 3;
    public static final int STATUS_WAITING_FOR_RESPONSE = 4;
    public static final int STATUS_PARSING_HEADERS = 5;
    public static final int STATUS_PARSING_CONTENT = 6;
    public static final int STATUS_COMPLETED = 7;
    public static final int STATUS_EXPIRED = 8;
    public static final int STATUS_EXCEPTED = 9;

    String _method = HttpMethods.GET;
    Buffer _scheme = HttpSchemes.HTTP_BUFFER;
    String _uri;
    int _version = HttpVersions.HTTP_1_1_ORDINAL;
    Address _address;
    HttpFields _requestFields = new HttpFields();
    Buffer _requestContent;
    InputStream _requestContentSource;
    long _timeout = -1;

    volatile int _status = STATUS_START;
    Buffer _requestContentChunk;
    boolean _retryStatus = false;
    // controls if the exchange will have listeners autoconfigured by the destination
    boolean _configureListeners = true;
    private HttpEventListener _listener = new Listener();

    boolean _onRequestCompleteDone;
    boolean _onResponseCompleteDone;
    boolean _onDone; // == onConnectionFail || onException || onExpired || onCancelled || onResponseCompleted && onRequestCompleted

    /* ------------------------------------------------------------ */
    public int getStatus()
    {
        return _status;
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public void waitForStatus(int status) throws InterruptedException
    {
        synchronized (this)
        {
            while (_status < status)
            {
                this.wait();
            }
        }
    }


    /**
     * Wait until the exchange is "done".  
     * Done is defined as when a final state has been passed to the 
     * HttpExchange via the associated onXxx call.  Note that an
     * exchange can transit a final state when being used as part
     * of a dialog (eg {@link SecurityListener}.   Done status
     * is thus defined as:<pre>
     *   done == onConnectionFailed 
     *        || onException
     *        || onExpire
     *        || onRequestComplete && onResponseComplete
     * </pre>
     * @return
     * @throws InterruptedException
     */
    public int waitForDone () throws InterruptedException
    {
        synchronized (this)
        {
            while (!isDone(_status))
                this.wait();
            return _status;
        }
    }




    /* ------------------------------------------------------------ */
    public void reset()
    {
        // TODO - this should do a cancel and wakeup everybody that was waiting.
        // might need a version number concept 
        synchronized(this)
        {
            _onRequestCompleteDone=false;
            _onResponseCompleteDone=false;
            _onDone=false;
            setStatus(STATUS_START);
        }
    }

    /* ------------------------------------------------------------ */
    void setStatus(int status)
    {
        // _status is volatile
        _status = status;

        try
        {
            switch (status)
            {
                case STATUS_WAITING_FOR_CONNECTION:
                    break;

                case STATUS_WAITING_FOR_COMMIT:
                    break;

                case STATUS_SENDING_REQUEST:
                    break;

                case HttpExchange.STATUS_WAITING_FOR_RESPONSE:
                    getEventListener().onRequestCommitted();
                    break;

                case STATUS_PARSING_HEADERS:
                    break;

                case STATUS_PARSING_CONTENT:
                    getEventListener().onResponseHeaderComplete();
                    break;

                case STATUS_COMPLETED:
                    getEventListener().onResponseComplete();
                    break;

                case STATUS_EXPIRED:
                    getEventListener().onExpire();
                    break;

            }
        }
        catch (IOException e)
        {
            Log.warn(e);
        }
    }

    /* ------------------------------------------------------------ */
    public boolean isDone (int status)
    {
        synchronized (this)
        {
            return _onDone;
        }
    }

    /* ------------------------------------------------------------ */
    public HttpEventListener getEventListener()
    {
        return _listener;
    }

    /* ------------------------------------------------------------ */
    public void setEventListener(HttpEventListener listener)
    {
        _listener=listener;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param url Including protocol, host and port
     */
    public void setURL(String url)
    {
        HttpURI uri = new HttpURI(url);
        String scheme = uri.getScheme();
        if (scheme != null)
        {
            if (HttpSchemes.HTTP.equalsIgnoreCase(scheme))
                setScheme(HttpSchemes.HTTP_BUFFER);
            else if (HttpSchemes.HTTPS.equalsIgnoreCase(scheme))
                setScheme(HttpSchemes.HTTPS_BUFFER);
            else
                setScheme(new ByteArrayBuffer(scheme));
        }

        int port = uri.getPort();
        if (port <= 0)
            port = "https".equalsIgnoreCase(scheme)?443:80;

        setAddress(new Address(uri.getHost(),port));

        String completePath = uri.getCompletePath();
        if (completePath == null)
            completePath = "/";
        
        setURI(completePath);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param address
     */
    public void setAddress(Address address)
    {
        _address = address;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public Address getAddress()
    {
        return _address;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param scheme
     */
    public void setScheme(Buffer scheme)
    {
        _scheme = scheme;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public Buffer getScheme()
    {
        return _scheme;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param version as integer, 9, 10 or 11 for 0.9, 1.0 or 1.1
     */
    public void setVersion(int version)
    {
        _version = version;
    }

    /* ------------------------------------------------------------ */
    public void setVersion(String version)
    {
        CachedBuffer v = HttpVersions.CACHE.get(version);
        if (v == null)
            _version = 10;
        else
            _version = v.getOrdinal();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public int getVersion()
    {
        return _version;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param method
     */
    public void setMethod(String method)
    {
        _method = method;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public String getMethod()
    {
        return _method;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public String getURI()
    {
        return _uri;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param uri
     */
    public void setURI(String uri)
    {
        _uri = uri;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void addRequestHeader(String name, String value)
    {
        getRequestFields().add(name,value);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void addRequestHeader(Buffer name, Buffer value)
    {
        getRequestFields().add(name,value);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void setRequestHeader(String name, String value)
    {
        getRequestFields().put(name,value);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @param value
     */
    public void setRequestHeader(Buffer name, Buffer value)
    {
        getRequestFields().put(name,value);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param value
     */
    public void setRequestContentType(String value)
    {
        getRequestFields().put(HttpHeaders.CONTENT_TYPE_BUFFER,value);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public HttpFields getRequestFields()
    {
        return _requestFields;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    // methods to commit and/or send the request

    /* ------------------------------------------------------------ */
    /**
     * @param requestContent
     */
    public void setRequestContent(Buffer requestContent)
    {
        _requestContent = requestContent;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param in
     */
    public void setRequestContentSource(InputStream in)
    {
        _requestContentSource = in;
        if (_requestContentSource.markSupported())
            _requestContentSource.mark(Integer.MAX_VALUE);
    }

    /* ------------------------------------------------------------ */
    public InputStream getRequestContentSource()
    {
        return _requestContentSource;
    }

    /* ------------------------------------------------------------ */
    public Buffer getRequestContentChunk() throws IOException
    {
        synchronized (this)
        {
            if (_requestContentChunk == null)
                _requestContentChunk = new ByteArrayBuffer(4096); // TODO configure
            else
            {
                if (_requestContentChunk.hasContent())
                    throw new IllegalStateException();
                _requestContentChunk.clear();
            }

            int read = _requestContentChunk.capacity();
            int length = _requestContentSource.read(_requestContentChunk.array(),0,read);
            if (length >= 0)
            {
                _requestContentChunk.setPutIndex(length);
                return _requestContentChunk;
            }
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    public Buffer getRequestContent()
    {
        return _requestContent;
    }

    /* ------------------------------------------------------------ */
    public boolean getRetryStatus()
    {
        return _retryStatus;
    }

    /* ------------------------------------------------------------ */
    public void setRetryStatus( boolean retryStatus )
    {
        _retryStatus = retryStatus;
    }
    
    /* ------------------------------------------------------------ */
    public long getTimeout()
    {
        return _timeout;
    }

    /* ------------------------------------------------------------ */
    public void setTimeout(long timeout)
    {
        _timeout = timeout;
    }

    /* ------------------------------------------------------------ */
    /** Cancel this exchange
     * Currently this implementation does nothing.
     */
    public void cancel()
    {

    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return getClass().getName() + "@" + hashCode() + "=" + _method + "//" + _address + _uri + "#" + _status;
    }



    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    // methods to handle response
    
    /**
     * Called when the request headers has been sent
     * @throws IOException
     */
    protected void onRequestCommitted() throws IOException
    {
    }

    /**
     * Called when the request and it's body have been sent.
     * @throws IOException
     */
    protected void onRequestComplete() throws IOException
    {
    }

    /**
     * Called when a response status line has been received.
     * @param version HTTP version
     * @param status HTTP status code
     * @param reason HTTP status code reason string
     * @throws IOException
     */
    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
    {
    }

    /**
     * Called for each response header received
     * @param name header name
     * @param value header value
     * @throws IOException
     */
    protected void onResponseHeader(Buffer name, Buffer value) throws IOException
    {
    }

    /**
     * Called when the response header has been completely received.
     * @throws IOException
     */
    protected void onResponseHeaderComplete() throws IOException
    {
    }

    /**
     * Called for each chunk of the response content received.
     * @param content
     * @throws IOException
     */
    protected void onResponseContent(Buffer content) throws IOException
    {
    }

    /**
     * Called when the entire response has been received
     * @throws IOException
     */
    protected void onResponseComplete() throws IOException
    {
    }

    /**
     * Called when an exception was thrown during an attempt to open a connection
     * @param ex
     */
    protected void onConnectionFailed(Throwable ex)
    {
        Log.warn("CONNECTION FAILED on " + this,ex);
    }

    /**
     * Called when any other exception occurs during handling for the exchange
     * @param ex
     */
    protected void onException(Throwable ex)
    {
        Log.warn("EXCEPTION on " + this,ex);
    }

    /**
     * Called when no response has been received within the timeout.
     */
    protected void onExpire()
    {
        Log.warn("EXPIRED " + this);
    }

    /**
     * Called when the request is retried (due to failures or authentication).
     * Implementations may need to reset any consumable content that needs to
     * be sent.
     * @throws IOException
     */
    protected void onRetry() throws IOException
    {
        if (_requestContentSource != null)
        {
            if (_requestContentSource.markSupported())
            {
                _requestContent = null;
                _requestContentSource.reset();
            }
            else
            {
                throw new IOException("Unsupported retry attempt");
            }
        }
    }

    /**
     * true of the exchange should have listeners configured for it by the destination
     *
     * false if this is being managed elsewhere
     *
     * @return
     */
    public boolean configureListeners()
    {
        return _configureListeners;
    }

    public void setConfigureListeners(boolean autoConfigure )
    {
        this._configureListeners = autoConfigure;
    }

    private class Listener implements HttpEventListener
    {
        public void onConnectionFailed(Throwable ex)
        {
            try
            {
                HttpExchange.this.onConnectionFailed(ex);
            }
            finally
            {
                synchronized(HttpExchange.this)
                {
                    _onDone=true;
                    HttpExchange.this.notifyAll();
                }
            }
        }

        public void onException(Throwable ex)
        {
            try
            {
                HttpExchange.this.onException(ex);
            }
            finally
            {
                synchronized(HttpExchange.this)
                {
                    _onDone=true;
                    HttpExchange.this.notifyAll();
                }
            }
        }

        public void onExpire()
        {
            try
            {
                HttpExchange.this.onExpire();
            }
            finally
            {
                synchronized(HttpExchange.this)
                {
                    _onDone=true;
                    HttpExchange.this.notifyAll();
                }
            }
        }

        public void onRequestCommitted() throws IOException
        {
            HttpExchange.this.onRequestCommitted();
        }

        public void onRequestComplete() throws IOException
        {
            try
            {
                HttpExchange.this.onRequestComplete();
            }
            finally
            {
                synchronized(HttpExchange.this)
                {
                    _onRequestCompleteDone=true;
                    _onDone=_onResponseCompleteDone;
                    HttpExchange.this.notifyAll();
                }
            }
        }

        public void onResponseComplete() throws IOException
        {
            try
            {
                HttpExchange.this.onResponseComplete();
            }
            finally
            {
                synchronized(HttpExchange.this)
                {
                    _onResponseCompleteDone=true;
                    _onDone=_onRequestCompleteDone;
                    HttpExchange.this.notifyAll();
                }
            }
        }

        public void onResponseContent(Buffer content) throws IOException
        {
            HttpExchange.this.onResponseContent(content);
        }

        public void onResponseHeader(Buffer name, Buffer value) throws IOException
        {
            HttpExchange.this.onResponseHeader(name,value);
        }

        public void onResponseHeaderComplete() throws IOException
        {
            HttpExchange.this.onResponseHeaderComplete();
        }

        public void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
        {
            HttpExchange.this.onResponseStatus(version,status,reason);
        }

        public void onRetry()
        {
            HttpExchange.this.setRetryStatus( true );
            try
            {
                HttpExchange.this.onRetry();
            }
            catch (IOException e)
            {
                onException(e);
            }
        }
    }

    /**
     * @deprecated use {@link org.mortbay.jetty.client.CachedExchange}
     *
     */
    public static class CachedExchange extends org.mortbay.jetty.client.CachedExchange
    {
        public CachedExchange(boolean cacheFields)
        {
            super(cacheFields);
        }
    }

    /**
     * @deprecated use {@link org.mortbay.jetty.client.ContentExchange}
     *
     */
    public static class ContentExchange extends org.mortbay.jetty.client.ContentExchange
    {

    }



}
