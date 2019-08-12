// ========================================================================
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.servlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.HttpOnlyCookie;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.SessionIdManager;
import org.mortbay.jetty.SessionManager;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.util.LazyList;

/* ------------------------------------------------------------ */
/**
 * An Abstract implementation of SessionManager. The partial implementation of
 * SessionManager interface provides the majority of the handling required to
 * implement a SessionManager. Concrete implementations of SessionManager based
 * on AbstractSessionManager need only implement the newSession method to return
 * a specialized version of the Session inner class that provides an attribute
 * Map.
 * <p>
 * If the property
 * org.mortbay.jetty.servlet.AbstractSessionManager.23Notifications is set to
 * true, the 2.3 servlet spec notification style will be used.
 * <p>
 * 
 * @author Greg Wilkins (gregw)
 */
public abstract class AbstractSessionManager extends AbstractLifeCycle implements SessionManager
{
    /* ------------------------------------------------------------ */
    public final static int __distantFuture=60*60*24*7*52*20;

    private static final HttpSessionContext __nullSessionContext=new NullSessionContext();

    private boolean _usingCookies=true;
    
    /* ------------------------------------------------------------ */
    // Setting of max inactive interval for new sessions
    // -1 means no timeout
    protected int _dftMaxIdleSecs=-1;
    protected SessionHandler _sessionHandler;
    protected boolean _httpOnly=false;
    protected int _maxSessions=0;

    protected int _minSessions=0;
    protected SessionIdManager _sessionIdManager;
    protected boolean _secureCookies=false;
    protected Object _sessionAttributeListeners;
    protected Object _sessionListeners;
    
    protected ClassLoader _loader;
    protected ContextHandler.SContext _context;
    protected String _sessionCookie=__DefaultSessionCookie;
    protected String _sessionURL=__DefaultSessionURL;
    protected String _sessionURLPrefix=";"+_sessionURL+"=";
    protected String _sessionDomain;
    protected String _sessionPath;
    protected int _maxCookieAge=-1;
    protected int _refreshCookieAge;
    protected boolean _nodeIdInSessionId;

    /* ------------------------------------------------------------ */
    public AbstractSessionManager()
    {
    }

    /* ------------------------------------------------------------ */
    public Cookie access(HttpSession session,boolean secure)
    {
        long now=System.currentTimeMillis();

        Session s = ((SessionIf)session).getSession();
        s.access(now);
        
        // Do we need to refresh the cookie?
        if (isUsingCookies() &&
            (s.isIdChanged() ||
             (getMaxCookieAge()>0 && getRefreshCookieAge()>0 && ((now-s.getCookieSetTime())/1000>getRefreshCookieAge()))
            )
           )
        {
            Cookie cookie=getSessionCookie(session,_context.getContextPath(),secure);
            s.cookieSet();
            s.setIdChanged(false);
            return cookie;
        }
        
        return null;
    }

    /* ------------------------------------------------------------ */
    public void addEventListener(EventListener listener)
    {
        if (listener instanceof HttpSessionAttributeListener)
            _sessionAttributeListeners=LazyList.add(_sessionAttributeListeners,listener);
        if (listener instanceof HttpSessionListener)
            _sessionListeners=LazyList.add(_sessionListeners,listener);
    }

    /* ------------------------------------------------------------ */
    public void clearEventListeners()
    {
        _sessionAttributeListeners=null;
        _sessionListeners=null;
    }

    /* ------------------------------------------------------------ */
    public void complete(HttpSession session)
    {
        Session s = ((SessionIf)session).getSession();
        s.complete();
    }

    /* ------------------------------------------------------------ */
    public void doStart() throws Exception
    {
        _context=ContextHandler.getCurrentContext();
        _loader=Thread.currentThread().getContextClassLoader();

        if (_sessionIdManager==null)
        {
            Server server=getSessionHandler().getServer();
            synchronized (server)
            {
                _sessionIdManager=server.getSessionIdManager();
                if (_sessionIdManager==null)
                {
                    _sessionIdManager=new HashSessionIdManager();
                    server.setSessionIdManager(_sessionIdManager);
                }
            }
        }
        if (!_sessionIdManager.isStarted())
            _sessionIdManager.start();

        if (_context != null)
        {
            // Look for a session cookie name
            String tmp=_context.getInitParameter(SessionManager.__SessionCookieProperty);
            if (tmp!=null)
                _sessionCookie=tmp;

            tmp=_context.getInitParameter(SessionManager.__SessionURLProperty);
            if (tmp!=null)
            {
                _sessionURL=(tmp==null||"none".equals(tmp))?null:tmp;
                _sessionURLPrefix=(tmp==null||"none".equals(tmp))?null:(";"+_sessionURL+"=");
            }

            // set up the max session cookie age if it isn't already
            if (_maxCookieAge==-1)
            {
                tmp=_context.getInitParameter(SessionManager.__MaxAgeProperty);
                if (tmp!=null)
                    _maxCookieAge=Integer.parseInt(tmp.trim());
            }
            // set up the session domain if it isn't already
            if (_sessionDomain==null)
            {
                // only try the context initParams
                _sessionDomain=_context.getInitParameter(SessionManager.__SessionDomainProperty);
            }

            // set up the sessionPath if it isn't already
            if (_sessionPath==null)
            {
                // only the context initParams
                _sessionPath=_context.getInitParameter(SessionManager.__SessionPathProperty);
            }
        }

        super.doStart();
    }

    /* ------------------------------------------------------------ */
    public void doStop() throws Exception
    {
        super.doStop();

        invalidateSessions();

        _loader=null;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the httpOnly.
     */
    public boolean getHttpOnly()
    {
        return _httpOnly;
    }

    /* ------------------------------------------------------------ */
    public HttpSession getHttpSession(String nodeId)
    {
        String cluster_id = getIdManager().getClusterId(nodeId);
        
        synchronized (this)
        {
            Session session = getSession(cluster_id);
            
            if (session!=null && !session.getNodeId().equals(nodeId))
                session.setIdChanged(true);
            return session;
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the metaManager used for cross context session management
     */
    public SessionIdManager getIdManager()
    {
        return _sessionIdManager;
    }

    /* ------------------------------------------------------------ */
    public int getMaxCookieAge()
    {
        return _maxCookieAge;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return seconds
     */
    public int getMaxInactiveInterval()
    {
        return _dftMaxIdleSecs;
    }

    /* ------------------------------------------------------------ */
    public int getMaxSessions()
    {
        return _maxSessions;
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated use {@link #getIdManager()}
     */
    public SessionIdManager getMetaManager()
    {
        return getIdManager();
    }

    /* ------------------------------------------------------------ */
    public int getMinSessions()
    {
        return _minSessions;
    }

    /* ------------------------------------------------------------ */
    public int getRefreshCookieAge()
    {
        return _refreshCookieAge;
    }


    /* ------------------------------------------------------------ */
    /**
     * @return Returns the secureCookies.
     */
    public boolean getSecureCookies()
    {
        return _secureCookies;
    }

    /* ------------------------------------------------------------ */
    public String getSessionCookie()
    {
        return _sessionCookie;
    }

    /* ------------------------------------------------------------ */
    public Cookie getSessionCookie(HttpSession session, String contextPath, boolean requestIsSecure)
    {
        if (isUsingCookies())
        {
            String id = getNodeId(session);
            Cookie cookie=getHttpOnly()?new HttpOnlyCookie(_sessionCookie,id):new Cookie(_sessionCookie,id);

            cookie.setPath((contextPath==null||contextPath.length()==0)?"/":contextPath);
            cookie.setMaxAge(getMaxCookieAge());
            cookie.setSecure(requestIsSecure&&getSecureCookies());

            // set up the overrides
            if (_sessionDomain!=null)
                cookie.setDomain(_sessionDomain);
            if (_sessionPath!=null)
                cookie.setPath(_sessionPath);

            return cookie;
        }
        return null;
    }

    public String getSessionDomain()
    {
        return _sessionDomain;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the sessionHandler.
     */
    public SessionHandler getSessionHandler()
    {
        return _sessionHandler;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @deprecated.  Need to review if it is needed.
     */
    public abstract Map getSessionMap();
    
    /* ------------------------------------------------------------ */
    public String getSessionPath()
    {
        return _sessionPath;
    }

    /* ------------------------------------------------------------ */
    public abstract int getSessions();

    /* ------------------------------------------------------------ */
    public String getSessionURL()
    {
        return _sessionURL;
    }

    /* ------------------------------------------------------------ */
    public String getSessionURLPrefix()
    {
        return _sessionURLPrefix;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the usingCookies.
     */
    public boolean isUsingCookies()
    {
        return _usingCookies;
    }

    /* ------------------------------------------------------------ */
    public boolean isValid(HttpSession session)
    {
        Session s = ((SessionIf)session).getSession();
        return s.isValid();
    }
    
    /* ------------------------------------------------------------ */
    public String getClusterId(HttpSession session)
    {
        Session s = ((SessionIf)session).getSession();
        return s.getClusterId();
    }
    
    /* ------------------------------------------------------------ */
    public String getNodeId(HttpSession session)
    {
        Session s = ((SessionIf)session).getSession();
        return s.getNodeId();
    }

    /* ------------------------------------------------------------ */
    /**
     * Create a new HttpSession for a request
     */
    public HttpSession newHttpSession(HttpServletRequest request)
    {
        Session session=newSession(request);
        session.setMaxInactiveInterval(_dftMaxIdleSecs);
        addSession(session,true);
        return session;
    }

    /* ------------------------------------------------------------ */
    public void removeEventListener(EventListener listener)
    {
        if (listener instanceof HttpSessionAttributeListener)
            _sessionAttributeListeners=LazyList.remove(_sessionAttributeListeners,listener);
        if (listener instanceof HttpSessionListener)
            _sessionListeners=LazyList.remove(_sessionListeners,listener);
    }

    /* ------------------------------------------------------------ */
    public void resetStats()
    {
        _minSessions=getSessions();
        _maxSessions=getSessions();
    }

    /* ------------------------------------------------------------ */
    /**
     * @param httpOnly
     *            The httpOnly to set.
     */
    public void setHttpOnly(boolean httpOnly)
    {
        _httpOnly=httpOnly;
    }
    

    /* ------------------------------------------------------------ */
    /**
     * @param metaManager The metaManager used for cross context session management.
     */
    public void setIdManager(SessionIdManager metaManager)
    {
        _sessionIdManager=metaManager;
    }

    /* ------------------------------------------------------------ */
    public void setMaxCookieAge(int maxCookieAgeInSeconds)
    {
        _maxCookieAge=maxCookieAgeInSeconds;
        
        if (_maxCookieAge>0 && _refreshCookieAge==0)
            _refreshCookieAge=_maxCookieAge/3;
            
    }

    /* ------------------------------------------------------------ */
    /**
     * @param seconds
     */
    public void setMaxInactiveInterval(int seconds)
    {
        _dftMaxIdleSecs=seconds;
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated use {@link #setIdManager(SessionIdManager)}
     */
    public void setMetaManager(SessionIdManager metaManager)
    {
        setIdManager(metaManager);
    }

    /* ------------------------------------------------------------ */
    public void setRefreshCookieAge(int ageInSeconds)
    {
        _refreshCookieAge=ageInSeconds;
    }


    /* ------------------------------------------------------------ */
    /**
     * @param secureCookies
     *            The secureCookies to set.
     */
    public void setSecureCookies(boolean secureCookies)
    {
        _secureCookies=secureCookies;
    }

    /* ------------------------------------------------------------ */
    public void setSessionCookie(String cookieName)
    {
        _sessionCookie=cookieName;
    }

    /* ------------------------------------------------------------ */
    public void setSessionDomain(String domain)
    {
        _sessionDomain=domain;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param sessionHandler
     *            The sessionHandler to set.
     */
    public void setSessionHandler(SessionHandler sessionHandler)
    {
        _sessionHandler=sessionHandler;
    }

    /* ------------------------------------------------------------ */
    public void setSessionPath(String path)
    {
        _sessionPath=path;
    }

    /* ------------------------------------------------------------ */
    /** Set the session ID URL parameter name
     * @param param The parameter name for session id URL rewriting (null or "none" for no rewriting).
     */
    public void setSessionURL(String param)
    {
        _sessionURL=(param==null||"none".equals(param))?null:param;
        _sessionURLPrefix=(param==null||"none".equals(param))?null:(";"+_sessionURL+"=");
    }
    /* ------------------------------------------------------------ */
    /**
     * @param usingCookies
     *            The usingCookies to set.
     */
    public void setUsingCookies(boolean usingCookies)
    {
        _usingCookies=usingCookies;
    }


    /* ------------------------------------------------------------ */
    protected abstract void addSession(Session session);

    /* ------------------------------------------------------------ */
    /**
     * Add the session Registers the session with this manager and registers the
     * session ID with the sessionIDManager;
     */
    protected void addSession(Session session, boolean created)
    {
        synchronized (_sessionIdManager)
        {
            _sessionIdManager.addSession(session);
            synchronized (this)
            {
                addSession(session);
                if (getSessions()>this._maxSessions)
                    this._maxSessions=getSessions();
            }
        }

        if (!created)
        {
            session.didActivate();
        }
        else if (_sessionListeners!=null)
        {
            HttpSessionEvent event=new HttpSessionEvent(session);
            for (int i=0; i<LazyList.size(_sessionListeners); i++)
                ((HttpSessionListener)LazyList.get(_sessionListeners,i)).sessionCreated(event);
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Get a known existingsession
     * @param idInCluster The session ID in the cluster, stripped of any worker name.
     * @return A Session or null if none exists.
     */
    public abstract Session getSession(String idInCluster);

    /* ------------------------------------------------------------ */
    protected abstract void invalidateSessions();

    
    /* ------------------------------------------------------------ */
    /**
     * Create a new session instance
     * @param request
     * @return
     */
    protected abstract Session newSession(HttpServletRequest request);
    


    /* ------------------------------------------------------------ */
    /**
     * @return true if the cluster node id (worker id) is returned as part of the session id by {@link HttpSession#getId()}. Default is false. 
     */
    public boolean isNodeIdInSessionId()
    {
        return _nodeIdInSessionId;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param nodeIdInSessionId true if the cluster node id (worker id) will be returned as part of the session id by {@link HttpSession#getId()}. Default is false.
     */
    public void setNodeIdInSessionId(boolean nodeIdInSessionId)
    {
        _nodeIdInSessionId=nodeIdInSessionId;
    }

    /* ------------------------------------------------------------ */
    /** Remove session from manager 
     * @param session The session to remove
     * @param invalidate True if {@link HttpSessionListener#sessionDestroyed(HttpSessionEvent)} and
     * {@link SessionIdManager#invalidateAll(String)} should be called.
     */
    public void removeSession(HttpSession session, boolean invalidate)
    {
        Session s = ((SessionIf)session).getSession();
        removeSession(s,invalidate);
    }

    /* ------------------------------------------------------------ */
    /** Remove session from manager 
     * @param session The session to remove
     * @param invalidate True if {@link HttpSessionListener#sessionDestroyed(HttpSessionEvent)} and
     * {@link SessionIdManager#invalidateAll(String)} should be called.
     */
    public void removeSession(Session session, boolean invalidate)
    {
        // Remove session from context and global maps
        boolean removed = false;
        synchronized (this)
        {
            //take this session out of the map of sessions for this context
            if (getSession(session.getClusterId()) != null)
            {
                removed = true;
                removeSession(session.getClusterId());
            }
        }  

        if (removed && invalidate)
        {
            // Remove session from all context and global id maps
            _sessionIdManager.removeSession(session);
            _sessionIdManager.invalidateAll(session.getClusterId());
        }
        
        if (invalidate && _sessionListeners!=null)
        {
            HttpSessionEvent event=new HttpSessionEvent(session);
            for (int i=LazyList.size(_sessionListeners); i-->0;)
                ((HttpSessionListener)LazyList.get(_sessionListeners,i)).sessionDestroyed(event);
        }
        if (!invalidate)
        {
            session.willPassivate();
        }
    }

    /* ------------------------------------------------------------ */
    protected abstract void removeSession(String idInCluster);
    
    /* ------------------------------------------------------------ */
    /**
     * Null returning implementation of HttpSessionContext
     * 
     * @author Greg Wilkins (gregw)
     */
    public static class NullSessionContext implements HttpSessionContext
    {
        /* ------------------------------------------------------------ */
        private NullSessionContext()
        {
        }

        /* ------------------------------------------------------------ */
        /**
         * @deprecated From HttpSessionContext
         */
        public Enumeration getIds()
        {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        /* ------------------------------------------------------------ */
        /**
         * @deprecated From HttpSessionContext
         */
        public HttpSession getSession(String id)
        {
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * Interface that any session wrapper should implement so that
     * SessionManager may access the Jetty session implementation.
     *
     */
    public interface SessionIf extends HttpSession
    {
        public Session getSession();
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * 
     * <p>
     * Implements {@link javax.servlet.HttpSession} from the {@link javax.servlet} package.   
     * </p>
     * @author gregw
     *
     */
    public abstract class Session implements SessionIf, Serializable
    {
        protected final String _clusterId;
        protected final String _nodeId;
        protected boolean _idChanged;
        protected final long _created;
        protected long _cookieSet;
        protected long _accessed;
        protected long _lastAccessed;
        protected boolean _invalid;
        protected boolean _doInvalidate;
        protected long _maxIdleMs=_dftMaxIdleSecs*1000;
        protected boolean _newSession;
        protected Map _values;
        protected int _requests;

        /* ------------------------------------------------------------- */
        protected Session(HttpServletRequest request)
        {
            _newSession=true;
            _created=System.currentTimeMillis();
            _clusterId=_sessionIdManager.newSessionId(request,_created);
            _nodeId=_sessionIdManager.getNodeId(_clusterId,request);
            _accessed=_created;
            _requests=1;
        }

        /* ------------------------------------------------------------- */
        protected Session(long created, String clusterId)
        {
            _created=created;
            _clusterId=clusterId;
            _nodeId=_sessionIdManager.getNodeId(_clusterId,null);
            _accessed=_created;
        }

        /* ------------------------------------------------------------- */
        public Session getSession()
        {
            return this;
        }
        
        /* ------------------------------------------------------------- */
        protected void initValues() 
        {
            _values=newAttributeMap();
        }

        /* ------------------------------------------------------------ */
        public synchronized Object getAttribute(String name)
        {
            if (_invalid)
                throw new IllegalStateException();

            if (null == _values)
                return null;
            
            return _values.get(name);
        }

        /* ------------------------------------------------------------ */
        public synchronized Enumeration getAttributeNames()
        {
            if (_invalid)
                throw new IllegalStateException();
            List names=_values==null?Collections.EMPTY_LIST:new ArrayList(_values.keySet());
            return Collections.enumeration(names);
        }
        
        /* ------------------------------------------------------------- */
        public long getCookieSetTime() 
        {
            return _cookieSet;
        }

        /* ------------------------------------------------------------- */
        public long getCreationTime() throws IllegalStateException
        {
            if (_invalid)
                throw new IllegalStateException();
            return _created;
        }

        /* ------------------------------------------------------------ */
        public String getId() throws IllegalStateException
        {
            return _nodeIdInSessionId?_nodeId:_clusterId;
        }

        /* ------------------------------------------------------------- */
        protected String getNodeId()
        {
            return _nodeId;
        }
        
        /* ------------------------------------------------------------- */
        protected String getClusterId()
        {
            return _clusterId;
        }

        /* ------------------------------------------------------------- */
        public long getLastAccessedTime() throws IllegalStateException
        {
            if (_invalid)
                throw new IllegalStateException();
            return _lastAccessed;
        }

        /* ------------------------------------------------------------- */
        public int getMaxInactiveInterval()
        {
            if (_invalid)
                throw new IllegalStateException();
            return (int)(_maxIdleMs/1000);
        }

        /* ------------------------------------------------------------ */
        /*
         * @see javax.servlet.http.HttpSession#getServletContext()
         */
        public ServletContext getServletContext()
        {
            return _context;
        }

        /* ------------------------------------------------------------- */
        /**
         * @deprecated
         */
        public HttpSessionContext getSessionContext() throws IllegalStateException
        {
            if (_invalid)
                throw new IllegalStateException();
            return __nullSessionContext;
        }

        /* ------------------------------------------------------------- */
        /**
         * @deprecated As of Version 2.2, this method is replaced by
         *             {@link #getAttribute}
         */
        public Object getValue(String name) throws IllegalStateException
        {
            return getAttribute(name);
        }

        /* ------------------------------------------------------------- */
        /**
         * @deprecated As of Version 2.2, this method is replaced by
         *             {@link #getAttributeNames}
         */
        public synchronized String[] getValueNames() throws IllegalStateException
        {
            if (_invalid)
                throw new IllegalStateException();
            if (_values==null)
                return new String[0];
            String[] a=new String[_values.size()];
            return (String[])_values.keySet().toArray(a);
        }

        /* ------------------------------------------------------------ */
        protected void access(long time)
        {
            synchronized(this)
            {
                _newSession=false;
                _lastAccessed=_accessed;
                _accessed=time;
                _requests++;
            }
        }

        /* ------------------------------------------------------------ */
        protected void complete()
        {
            synchronized(this)
            {
                _requests--;
                if (_doInvalidate && _requests<=0  )
                    doInvalidate();
            }
        }
        
        
        /* ------------------------------------------------------------- */
        protected void timeout() throws IllegalStateException
        {
            // remove session from context and invalidate other sessions with same ID.
            removeSession(this,true);

            // Notify listeners and unbind values
            synchronized (this)
            {
                if (!_invalid)
                {
                    if (_requests<=0)
                        doInvalidate();
                    else
                        _doInvalidate=true;
                }
            }
        }
        
        /* ------------------------------------------------------------- */
        public void invalidate() throws IllegalStateException
        {
            // remove session from context and invalidate other sessions with same ID.
            removeSession(this,true);
            doInvalidate();
        }
        
        /* ------------------------------------------------------------- */
        protected void doInvalidate() throws IllegalStateException
        {
            try
            {
                // Notify listeners and unbind values
                if (_invalid)
                    throw new IllegalStateException();

                while (_values!=null && _values.size()>0)
                {
                    ArrayList keys;
                    synchronized (this)
                    {
                        keys=new ArrayList(_values.keySet());
                    }

                    Iterator iter=keys.iterator();
                    while (iter.hasNext())
                    {
                        String key=(String)iter.next();

                        Object value;
                        synchronized (this)
                        {
                            value=_values.remove(key);
                        }
                        unbindValue(key,value);

                        if (_sessionAttributeListeners!=null)
                        {
                            HttpSessionBindingEvent event=new HttpSessionBindingEvent(this,key,value);

                            for (int i=0; i<LazyList.size(_sessionAttributeListeners); i++)
                                ((HttpSessionAttributeListener)LazyList.get(_sessionAttributeListeners,i)).attributeRemoved(event);
                        }
                    }
                }
            }
            finally
            {
                // mark as invalid
                _invalid=true;
            }
        }

        /* ------------------------------------------------------------- */
        public boolean isIdChanged()
        {
            return _idChanged;
        }

        /* ------------------------------------------------------------- */
        public boolean isNew() throws IllegalStateException
        {
            if (_invalid)
                throw new IllegalStateException();
            return _newSession;
        }

        /* ------------------------------------------------------------- */
        /**
         * @deprecated As of Version 2.2, this method is replaced by
         *             {@link #setAttribute}
         */
        public void putValue(java.lang.String name, java.lang.Object value) throws IllegalStateException
        {
            setAttribute(name,value);
        }

        /* ------------------------------------------------------------ */
        public synchronized void removeAttribute(String name)
        {
            if (_invalid)
                throw new IllegalStateException();
            if (_values==null)
                return;

            Object old=_values.remove(name);
            if (old!=null)
            {
                unbindValue(name,old);
                if (_sessionAttributeListeners!=null)
                {
                    HttpSessionBindingEvent event=new HttpSessionBindingEvent(this,name,old);

                    for (int i=0; i<LazyList.size(_sessionAttributeListeners); i++)
                        ((HttpSessionAttributeListener)LazyList.get(_sessionAttributeListeners,i)).attributeRemoved(event);
                }
            }
        }

        /* ------------------------------------------------------------- */
        /**
         * @deprecated As of Version 2.2, this method is replaced by
         *             {@link #removeAttribute}
         */
        public void removeValue(java.lang.String name) throws IllegalStateException
        {
            removeAttribute(name);
        }

        /* ------------------------------------------------------------ */
        public synchronized void setAttribute(String name, Object value)
        {
            if (value==null)
            {
                removeAttribute(name);
                return;
            }

            if (_invalid)
                throw new IllegalStateException();
            if (_values==null)
                _values=newAttributeMap();
            Object oldValue=_values.put(name,value);

            if (oldValue==null || !value.equals(oldValue)) 
            {
                unbindValue(name,oldValue);
                bindValue(name,value);

                if (_sessionAttributeListeners!=null)
                {
                    HttpSessionBindingEvent event=new HttpSessionBindingEvent(this,name,oldValue==null?value:oldValue);

                    for (int i=0; i<LazyList.size(_sessionAttributeListeners); i++)
                    {
                        HttpSessionAttributeListener l=(HttpSessionAttributeListener)LazyList.get(_sessionAttributeListeners,i);

                        if (oldValue==null)
                            l.attributeAdded(event);
                        else if (value==null)
                            l.attributeRemoved(event);
                        else
                            l.attributeReplaced(event);
                    }
                }
            }
        }

        /* ------------------------------------------------------------- */
        public void setIdChanged(boolean changed)
        {
            _idChanged=changed;
        }

        /* ------------------------------------------------------------- */
        public void setMaxInactiveInterval(int secs)
        {
            _maxIdleMs=(long)secs*1000;
        }

        /* ------------------------------------------------------------- */
        public String toString()
        {
            return this.getClass().getName()+":"+getId()+"@"+hashCode();
        }

        /* ------------------------------------------------------------- */
        /** If value implements HttpSessionBindingListener, call valueBound() */
        protected void bindValue(java.lang.String name, Object value)
        {
            if (value!=null&&value instanceof HttpSessionBindingListener)
                ((HttpSessionBindingListener)value).valueBound(new HttpSessionBindingEvent(this,name));
        }

        /* ------------------------------------------------------------ */
        protected boolean isValid()
        {
            return !_invalid;
        }

        /* ------------------------------------------------------------ */
        protected abstract Map newAttributeMap();

        /* ------------------------------------------------------------- */
        protected void cookieSet()
        {
            _cookieSet=_accessed;
        }

        /* ------------------------------------------------------------- */
        /** If value implements HttpSessionBindingListener, call valueUnbound() */
        protected void unbindValue(java.lang.String name, Object value)
        {
            if (value!=null&&value instanceof HttpSessionBindingListener)
                ((HttpSessionBindingListener)value).valueUnbound(new HttpSessionBindingEvent(this,name));
        }

        /* ------------------------------------------------------------- */
        protected synchronized void willPassivate() 
        {
            HttpSessionEvent event = new HttpSessionEvent(this);
            for (Iterator iter = _values.values().iterator(); iter.hasNext();) 
            {
                Object value = iter.next();
                if (value instanceof HttpSessionActivationListener) 
                {
                    HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                    listener.sessionWillPassivate(event);
                }
            }
        }

        /* ------------------------------------------------------------- */
        protected synchronized void didActivate() 
        {
            HttpSessionEvent event = new HttpSessionEvent(this);
            for (Iterator iter = _values.values().iterator(); iter.hasNext();) 
            {
                Object value = iter.next();
                if (value instanceof HttpSessionActivationListener) 
                {
                    HttpSessionActivationListener listener = (HttpSessionActivationListener) value;
                    listener.sessionDidActivate(event);
                }
            }
        }
    }
}
