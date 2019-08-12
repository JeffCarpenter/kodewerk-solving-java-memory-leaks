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
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.mortbay.component.LifeCycle;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.nio.DirectNIOBuffer;
import org.mortbay.io.nio.IndirectNIOBuffer;
import org.mortbay.jetty.AbstractBuffers;
import org.mortbay.jetty.HttpSchemes;
import org.mortbay.jetty.client.security.Authorization;
import org.mortbay.jetty.client.security.RealmResolver;
import org.mortbay.log.Log;
import org.mortbay.resource.Resource;
import org.mortbay.thread.QueuedThreadPool;
import org.mortbay.thread.ThreadPool;
import org.mortbay.thread.Timeout;
import org.mortbay.util.Attributes;
import org.mortbay.util.AttributesMap;

/**
 * Http Client.
 * <p/>
 * HttpClient is the main active component of the client API implementation.
 * It is the opposite of the Connectors in standard Jetty, in that it listens
 * for responses rather than requests.   Just like the connectors, there is a
 * blocking socket version and a non-blocking NIO version (implemented as nested classes
 * selected by {@link #setConnectorType(int)}).
 * <p/>
 * The an instance of {@link HttpExchange} is passed to the {@link #send(HttpExchange)} method
 * to send a request.  The exchange contains both the headers and content (source) of the request
 * plus the callbacks to handle responses.   A HttpClient can have many exchanges outstanding
 * and they may be queued on the {@link HttpDestination} waiting for a {@link HttpConnection},
 * queued in the {@link HttpConnection} waiting to be transmitted or pipelined on the actual
 * TCP/IP connection waiting for a response.
 * <p/>
 * The {@link HttpDestination} class is an aggregation of {@link HttpConnection}s for the
 * same host, port and protocol.   A destination may limit the number of connections
 * open and they provide a pool of open connections that may be reused.   Connections may also
 * be allocated from a destination, so that multiple request sources are not multiplexed
 * over the same connection.
 *
 * @see {@link HttpExchange}
 * @see {@link HttpDestination}
 * @author Greg Wilkins
 * @author Matthew Purland
 * @author Guillaume Nodet
 */
public class HttpClient extends AbstractBuffers implements Attributes
{
    public static final int CONNECTOR_SOCKET=0;
    public static final int CONNECTOR_SELECT_CHANNEL=2;

    private int _connectorType=CONNECTOR_SELECT_CHANNEL;
    private boolean _useDirectBuffers=true;
    private int _maxConnectionsPerAddress=32;
    private Map<Address, HttpDestination> _destinations = new HashMap<Address, HttpDestination>();
    ThreadPool _threadPool;
    Connector _connector;
    private long _idleTimeout=20000;
    private long _timeout=320000;
    private int _soTimeout = 10000;
    private Timeout _timeoutQ = new Timeout();
    private Timeout _idleTimeoutQ = new Timeout();
    private Address _proxy;
    private Authorization _proxyAuthentication;
    private Set<String> _noProxy;
    private int _maxRetries = 3;
    private LinkedList<String> _registeredListeners;

    // TODO clean up and add getters/setters to some of this maybe
    private String _keyStoreLocation;
    private String _keyStoreType="JKS";
    private String _keyStorePassword;
    private String _keyManagerAlgorithm = "SunX509";
    private String _keyManagerPassword;
    private String _trustStoreLocation;
    private String _trustStoreType="JKS";
    private String _trustStorePassword;
    private String _trustManagerAlgorithm = "SunX509";

    private SSLContext _sslContext;

    private String _protocol="TLS";
    private String _provider;
    private String _secureRandomAlgorithm;

    private RealmResolver _realmResolver;
    
    private AttributesMap _attributes=new AttributesMap();

    /* ------------------------------------------------------------------------------- */
    public void dump() throws IOException
    {
        for (Map.Entry<Address, HttpDestination> entry : _destinations.entrySet())
        {
            System.err.println("\n"+entry.getKey()+":");
            entry.getValue().dump();
        }
    }

    /* ------------------------------------------------------------------------------- */
    public void send(HttpExchange exchange) throws IOException
    {
        if (!isStarted())
            throw new IllegalStateException("!started");
        boolean ssl=HttpSchemes.HTTPS_BUFFER.equalsIgnoreCase(exchange.getScheme());
        exchange.setStatus(HttpExchange.STATUS_WAITING_FOR_CONNECTION);
        HttpDestination destination=getDestination(exchange.getAddress(),ssl);
        destination.send(exchange);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the threadPool
     */
    public ThreadPool getThreadPool()
    {
        return _threadPool;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param threadPool the threadPool to set
     */
    public void setThreadPool(ThreadPool threadPool)
    {
        _threadPool=threadPool;
    }


    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @return Attribute associated with client
     */
    public Object getAttribute(String name)
    {
        return _attributes.getAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /**
     * @return names of attributes associated with client
     */
    public Enumeration getAttributeNames()
    {
        return _attributes.getAttributeNames();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param name
     */
    public void removeAttribute(String name)
    {
        _attributes.removeAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /**
     * Set an attribute on the HttpClient.
     * Attributes are not used by the client, but are provided for
     * so that users of a shared HttpClient may share other structures.
     * @param name
     * @param attribute
     */
    public void setAttribute(String name, Object attribute)
    {
        _attributes.setAttribute(name,attribute);
    }

    /* ------------------------------------------------------------ */
    /**
     * @param name
     * @return
     */
    public void clearAttributes()
    {
        _attributes.clearAttributes();
    }

    /* ------------------------------------------------------------------------------- */
    public HttpDestination getDestination(Address remote, boolean ssl) throws UnknownHostException, IOException
    {
        if (remote==null)
            throw new UnknownHostException("Remote socket address cannot be null.");

        synchronized (_destinations)
        {
            HttpDestination destination=_destinations.get(remote);
            if (destination==null)
            {
                destination=new HttpDestination(this,remote,ssl,_maxConnectionsPerAddress);
                if (_proxy != null && (_noProxy == null || !_noProxy.contains(remote.getHost())))
                {
                    destination.setProxy(_proxy);
                    if (_proxyAuthentication!=null)
                        destination.setProxyAuthentication(_proxyAuthentication);
                }
                _destinations.put(remote,destination);
            }
            return destination;
        }
    }

    /* ------------------------------------------------------------ */
    public void schedule(Timeout.Task task)
    {
        _timeoutQ.schedule(task);
    }
    
    /* ------------------------------------------------------------ */
    public void schedule(Timeout.Task task, long timeout)
    {
        _timeoutQ.schedule(task, timeout);
    }

    /* ------------------------------------------------------------ */
    public void scheduleIdle(Timeout.Task task)
    {
        _idleTimeoutQ.schedule(task);
    }

    /* ------------------------------------------------------------ */
    public void cancel(Timeout.Task task)
    {
        task.cancel();
    }

    /* ------------------------------------------------------------ */
    /**
     * Get whether the connector can use direct NIO buffers.
     */
    public boolean getUseDirectBuffers()
    {
        return _useDirectBuffers;
    }

    /* ------------------------------------------------------------ */
    public void setRealmResolver( RealmResolver resolver )
    {
        _realmResolver = resolver;
    }

    /* ------------------------------------------------------------ */
    /**
     * returns the SecurityRealmResolver registered with the HttpClient or null
     *
     * @return
     */
    public RealmResolver getRealmResolver()
    {
        return _realmResolver;
    }

    /* ------------------------------------------------------------ */
    public boolean hasRealms()
    {
        return _realmResolver==null?false:true;
    }


    /**
     * Registers a listener that can listen to the stream of execution between the client and the
     * server and influence events.  Sequential calls to the method wrapper sequentially wrap the preceeding
     * listener in a delegation model.
     * <p/>
     * NOTE: the SecurityListener is a special listener which doesn't need to be added via this
     * mechanic, if you register security realms then it will automatically be added as the top listener of the
     * delegation stack.
     *
     * @param listenerClass
     */
    public void registerListener( String listenerClass )
    {
        if ( _registeredListeners == null )
        {
            _registeredListeners = new LinkedList<String>();
        }
        _registeredListeners.add( listenerClass );
    }

    public LinkedList<String> getRegisteredListeners()
    {
        return _registeredListeners;
    }


    /* ------------------------------------------------------------ */
    /**
     * Set to use NIO direct buffers.
     *
     * @param direct
     *            If True (the default), the connector can use NIO direct
     *            buffers. Some JVMs have memory management issues (bugs) with
     *            direct buffers.
     */
    public void setUseDirectBuffers(boolean direct)
    {
        _useDirectBuffers=direct;
    }

    /* ------------------------------------------------------------ */
    /**
     * Get the type of connector (socket, blocking or select) in use.
     */
    public int getConnectorType()
    {
        return _connectorType;
    }

    /* ------------------------------------------------------------ */
    public void setConnectorType(int connectorType)
    {
        this._connectorType=connectorType;
    }

    /* ------------------------------------------------------------ */
    /**
     * Create a new NIO buffer. If using direct buffers, it will create a direct
     * NIO buffer, other than an indirect buffer.
     */
    @Override
    protected Buffer newBuffer(int size)
    {
        if (_connectorType!=CONNECTOR_SOCKET)
        {
            Buffer buf=null;
            if (size==getHeaderBufferSize())
                buf=new IndirectNIOBuffer(size);
            else if (_useDirectBuffers)
                buf=new DirectNIOBuffer(size);
            else
                buf=new IndirectNIOBuffer(size);
            return buf;
        }
        else
        {
            return new ByteArrayBuffer(size);
        }
    }

    /* ------------------------------------------------------------ */
    public int getMaxConnectionsPerAddress()
    {
        return _maxConnectionsPerAddress;
    }

    /* ------------------------------------------------------------ */
    public void setMaxConnectionsPerAddress(int maxConnectionsPerAddress)
    {
        _maxConnectionsPerAddress=maxConnectionsPerAddress;
    }

    /* ------------------------------------------------------------ */
    protected void doStart() throws Exception
    {
        super.doStart();

        _timeoutQ.setDuration(_timeout);
        _timeoutQ.setNow();
        _idleTimeoutQ.setDuration(_idleTimeout);
        _idleTimeoutQ.setNow();

        if(_threadPool==null)
        {
            QueuedThreadPool pool = new QueuedThreadPool();
            pool.setMaxThreads(16);
            pool.setDaemon(true);
            pool.setName("HttpClient");
            _threadPool=pool;
        }

        if (_threadPool instanceof LifeCycle)
        {
            ((LifeCycle)_threadPool).start();
        }


        if (_connectorType==CONNECTOR_SELECT_CHANNEL)
        {

            _connector=new SelectConnector(this);
        }
        else
        {
            _connector=new SocketConnector(this);
        }
        _connector.start();

        _threadPool.dispatch(new Runnable()
        {
            public void run()
            {
                while (isRunning())
                {
                    _timeoutQ.tick(System.currentTimeMillis());
                    _idleTimeoutQ.tick(_timeoutQ.getNow());
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e)
                    {
                        Log.ignore(e);
                    }
                }
            }
        });

    }

    /* ------------------------------------------------------------ */
    long getNow()
    {
        return _timeoutQ.getNow();
    }

    /* ------------------------------------------------------------ */
    protected void doStop() throws Exception
    {
        _connector.stop();
        _connector=null;
        if (_threadPool instanceof LifeCycle)
        {
            ((LifeCycle)_threadPool).stop();
        }
        for (HttpDestination destination : _destinations.values())
        {
            destination.close();
        }

        _timeoutQ.cancelAll();
        _idleTimeoutQ.cancelAll();
        super.doStop();
    }

    /* ------------------------------------------------------------ */
    interface Connector extends LifeCycle
    {
        public void startConnection(HttpDestination destination) throws IOException;

    }

    /**
     * if a keystore location has been provided then client will attempt to use it as the keystore,
     * otherwise we simply ignore certificates and run with a loose ssl context.
     *
     * @return
     * @throws IOException
     */
    protected SSLContext getSSLContext() throws IOException
    {
   	if (_sslContext == null)
    	{
            if (_keyStoreLocation == null)
            {
                _sslContext = getLooseSSLContext();
            }
            else
            {
                _sslContext = getStrictSSLContext();
            }
        }
    	return _sslContext;
    }

    protected SSLContext getStrictSSLContext() throws IOException
    {

        try
        {
            if (_trustStoreLocation==null)
            {
                _trustStoreLocation=_keyStoreLocation;
                _trustStoreType=_keyStoreType;
            }

            KeyManager[] keyManagers=null;
            
            InputStream keystoreInputStream = null;
            keystoreInputStream= Resource.newResource(_keyStoreLocation).getInputStream();
            
            KeyStore keyStore=KeyStore.getInstance(_keyStoreType);
            keyStore.load(keystoreInputStream,_keyStorePassword==null?null:_keyStorePassword.toString().toCharArray());            
            keystoreInputStream.close();
            
            KeyManagerFactory keyManagerFactory=KeyManagerFactory.getInstance(_keyManagerAlgorithm);
            keyManagerFactory.init(keyStore,_keyManagerPassword==null?null:_keyManagerPassword.toString().toCharArray());
            keyManagers=keyManagerFactory.getKeyManagers();

            TrustManager[] trustManagers=null;
            
            InputStream truststoreInputStream = null;
            truststoreInputStream = Resource.newResource(_trustStoreLocation).getInputStream();
            
            KeyStore trustStore=KeyStore.getInstance(_trustStoreType);
            trustStore.load(truststoreInputStream,_trustStorePassword==null?null:_trustStorePassword.toString().toCharArray());           
            truststoreInputStream.close();
            
            TrustManagerFactory trustManagerFactory=TrustManagerFactory.getInstance(_trustManagerAlgorithm);
            trustManagerFactory.init(trustStore);
            trustManagers=trustManagerFactory.getTrustManagers();

            SecureRandom secureRandom=_secureRandomAlgorithm==null?null:SecureRandom.getInstance(_secureRandomAlgorithm);
            SSLContext context=_provider==null?SSLContext.getInstance(_protocol):SSLContext.getInstance(_protocol,_provider);
            context.init(keyManagers,trustManagers,secureRandom);
            return context;
        }
        catch ( Exception e )
        {
            Log.debug(e);
            throw new IOException( "error generating ssl context for " + _keyStoreLocation  + " " + e.getMessage() );
        }
    }

    protected SSLContext getLooseSSLContext() throws IOException
    {

        // Create a trust manager that does not validate certificate
        // chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
        {
            public java.security.cert.X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }

            public void checkClientTrusted( java.security.cert.X509Certificate[] certs, String authType )
            {
            }

            public void checkServerTrusted( java.security.cert.X509Certificate[] certs, String authType )
            {
            }
        } };

        HostnameVerifier hostnameVerifier = new HostnameVerifier()
        {
            public boolean verify( String urlHostName, SSLSession session )
            {
                Log.warn( "Warning: URL Host: " + urlHostName + " vs." + session.getPeerHost() );
                return true;
            }
        };

        // Install the all-trusting trust manager
        try
        {
            // TODO real trust manager
            SSLContext sslContext = SSLContext.getInstance( "SSL" );
            sslContext.init( null, trustAllCerts, new java.security.SecureRandom() );
            return sslContext;
        }
        catch ( Exception e )
        {
            Log.debug(e);
            throw new IOException( "issue ignoring certs" );
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the period in milliseconds a {@link HttpConnection} can be idle for before it is closed.
     */
    public long getIdleTimeout()
    {
        return _idleTimeout;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param ms the period in milliseconds a {@link HttpConnection} can be idle for before it is closed.
     */
    public void setIdleTimeout(long ms)
    {
        _idleTimeout=ms;
    }

    /* ------------------------------------------------------------ */
    public int getSoTimeout() 
    {
        return _soTimeout;
    }

    /* ------------------------------------------------------------ */
    public void setSoTimeout(int so)
    {
        _soTimeout = so;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return the period in ms that an exchange will wait for a response from the server.
     */
    public long getTimeout()
    {
        return _timeout;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param ms the period in ms that an exchange will wait for a response from the server.
     */
    public void setTimeout(long ms)
    {
        _timeout=ms;
    }

    /* ------------------------------------------------------------ */
    public Address getProxy()
    {
        return _proxy;
    }

    /* ------------------------------------------------------------ */
    public void setProxy(Address proxy)
    {
        this._proxy = proxy;
    }

    /* ------------------------------------------------------------ */
    public Authorization getProxyAuthentication()
    {
        return _proxyAuthentication;
    }

    /* ------------------------------------------------------------ */
    public void setProxyAuthentication(Authorization authentication)
    {
        _proxyAuthentication = authentication;
    }

    /* ------------------------------------------------------------ */
    public boolean isProxied()
    {
        return this._proxy!=null;
    }

    /* ------------------------------------------------------------ */
    public Set<String> getNoProxy()
    {
        return _noProxy;
    }

    /* ------------------------------------------------------------ */
    public void setNoProxy(Set<String> noProxyAddresses)
    {
        _noProxy = noProxyAddresses;
    }

    /* ------------------------------------------------------------ */
    public int maxRetries()
    {
        return _maxRetries;
    }

     /* ------------------------------------------------------------ */
    public void setMaxRetries( int retries )
    {
        _maxRetries = retries;
    }

    /* ------------------------------------------------------------ */
    public String getTrustStoreLocation()
    {
        return _trustStoreLocation;
    }

    /* ------------------------------------------------------------ */
    public void setTrustStoreLocation(String trustStoreLocation)
    {
        this._trustStoreLocation = trustStoreLocation;
    }

    /* ------------------------------------------------------------ */
    public String getKeyStoreLocation()
    {
        return _keyStoreLocation;
    }

    /* ------------------------------------------------------------ */
    public void setKeyStoreLocation(String keyStoreLocation)
    {
        this._keyStoreLocation = keyStoreLocation;
    }

    /* ------------------------------------------------------------ */
    public void setKeyStorePassword(String _keyStorePassword)
    {
        this._keyStorePassword = _keyStorePassword;
    }

    /* ------------------------------------------------------------ */
    public void setKeyManagerPassword(String _keyManagerPassword)
    {
        this._keyManagerPassword = _keyManagerPassword;
    }

    /* ------------------------------------------------------------ */
    public void setTrustStorePassword(String _trustStorePassword)
    {
        this._trustStorePassword = _trustStorePassword;
    }
}
