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

package org.mortbay.jetty.servlet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServletRequest;

import org.mortbay.log.Log;
import org.mortbay.util.LazyList;


/* ------------------------------------------------------------ */
/** An in-memory implementation of SessionManager.
 *
 * @author Greg Wilkins (gregw)
 */
public class HashSessionManager extends AbstractSessionManager
{
    private Timer _timer;
    private TimerTask _task;
    private int _scavengePeriodMs=30000;
    private int _savePeriodMs=0; //don't do period saves by default
    private TimerTask _saveTask;
    protected Map _sessions;
    private File _storeDir;
    private boolean _lazyLoad=false;
    private boolean _sessionsLoaded=false;
    
    /* ------------------------------------------------------------ */
    public HashSessionManager()
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#doStart()
     */
    public void doStart() throws Exception
    {
        _sessions=new HashMap();
        super.doStart();

        _timer=new Timer(true);
        
        setScavengePeriod(getScavengePeriod());

        if (_storeDir!=null)
        {
            if (!_storeDir.exists())
                _storeDir.mkdir();

            if (!_lazyLoad)
                restoreSessions();
        }
 
        setSavePeriod(getSavePeriod());
    }

    /* ------------------------------------------------------------ */
    /* (non-Javadoc)
     * @see org.mortbay.jetty.servlet.AbstractSessionManager#doStop()
     */
    public void doStop() throws Exception
    {
        
        if (_storeDir != null)
            saveSessions();
        
        super.doStop();
 
        _sessions.clear();
        _sessions=null;

        // stop the scavenger
        synchronized(this)
        {
            if (_saveTask!=null)
                _saveTask.cancel();
            if (_task!=null)
                _task.cancel();
            if (_timer!=null)
                _timer.cancel();
            _timer=null;
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @return seconds
     */
    public int getScavengePeriod()
    {
        return _scavengePeriodMs/1000;
    }

    
    /* ------------------------------------------------------------ */
    public Map getSessionMap()
    {
        return Collections.unmodifiableMap(_sessions);
    }


    /* ------------------------------------------------------------ */
    public int getSessions()
    {
        return _sessions.size();
    }


    /* ------------------------------------------------------------ */
    public void setMaxInactiveInterval(int seconds)
    {
        super.setMaxInactiveInterval(seconds);
        if (_dftMaxIdleSecs>0&&_scavengePeriodMs>_dftMaxIdleSecs*1000)
            setScavengePeriod((_dftMaxIdleSecs+9)/10);
    }

    /* ------------------------------------------------------------ */
    public void setSavePeriod (int seconds)
    {
        int oldSavePeriod = _savePeriodMs;
        int period = (seconds * 1000);
        if (period < 0)
            period=0;
        _savePeriodMs=period;
        
        if (_timer!=null)
        {
            synchronized (this)
            {
                if (_saveTask!=null)
                    _saveTask.cancel();
                if (_savePeriodMs > 0 && _storeDir!=null) //only save if we have a directory configured
                {
                    _saveTask = new TimerTask()
                    {
                        public void run()
                        {
                            try
                            {
                                saveSessions();
                            }
                            catch (Exception e)
                            {
                                Log.warn(e);
                            }
                        }   
                    };
                    _timer.schedule(_saveTask,_savePeriodMs,_savePeriodMs);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    public int getSavePeriod ()
    {
        if (_savePeriodMs<=0)
            return 0;
        
        return _savePeriodMs/1000;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param seconds
     */
    public void setScavengePeriod(int seconds)
    {
        if (seconds==0)
            seconds=60;

        int old_period=_scavengePeriodMs;
        int period=seconds*1000;
        if (period>60000)
            period=60000;
        if (period<1000)
            period=1000;

        _scavengePeriodMs=period;
        if (_timer!=null && (period!=old_period || _task==null))
        {
            synchronized (this)
            {
                if (_task!=null)
                    _task.cancel();
                _task = new TimerTask()
                {
                    public void run()
                    {
                        scavenge();
                    }   
                };
                _timer.schedule(_task,_scavengePeriodMs,_scavengePeriodMs);
            }
        }
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Find sessions that have timed out and invalidate them. This runs in the
     * SessionScavenger thread.
     */
    private void scavenge()
    {
        //don't attempt to scavenge if we are shutting down
        if (isStopping() || isStopped())
            return;
        
        Thread thread=Thread.currentThread();
        ClassLoader old_loader=thread.getContextClassLoader();
        try
        {
            if (_loader!=null)
                thread.setContextClassLoader(_loader);

            long now=System.currentTimeMillis();

            try
            {
                if (!_sessionsLoaded && _lazyLoad)
                    restoreSessions();
            }
            catch(Exception e)
            {
                Log.debug(e);
            }
            
            // Since Hashtable enumeration is not safe over deletes,
            // we build a list of stale sessions, then go back and invalidate
            // them
            Object stale=null;

            synchronized (HashSessionManager.this)
            {
                // For each session
                for (Iterator i=_sessions.values().iterator(); i.hasNext();)
                {
                    Session session=(Session)i.next();
                    long idleTime=session._maxIdleMs;
                    if (idleTime>0&&session._accessed+idleTime<now)
                    {
                        // Found a stale session, add it to the list
                        stale=LazyList.add(stale,session);
                    }
                }
            }

            // Remove the stale sessions
            for (int i=LazyList.size(stale); i-->0;)
            {
                // check it has not been accessed in the meantime
                Session session=(Session)LazyList.get(stale,i);
                long idleTime=session._maxIdleMs;
                if (idleTime>0&&session._accessed+idleTime<System.currentTimeMillis())
                {
                    ((Session)session).timeout();
                    int nbsess=this._sessions.size();
                    if (nbsess<this._minSessions)
                        this._minSessions=nbsess;
                }
            }
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw ((ThreadDeath)t);
            else
                Log.warn("Problem scavenging sessions", t);
        }
        finally
        {
            thread.setContextClassLoader(old_loader);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected void addSession(AbstractSessionManager.Session session)
    {
        _sessions.put(session.getClusterId(),session);
    }
    
    /* ------------------------------------------------------------ */
    public AbstractSessionManager.Session getSession(String idInCluster)
    {
        try
        {
            if (!_sessionsLoaded && _lazyLoad)
                restoreSessions();
        }
        catch(Exception e)
        {
            Log.warn(e);
        }
        
        if (_sessions==null)
            return null;

        return (Session)_sessions.get(idInCluster);
    }

    /* ------------------------------------------------------------ */
    protected void invalidateSessions()
    {
        // Invalidate all sessions to cause unbind events
        ArrayList sessions=new ArrayList(_sessions.values());
        for (Iterator i=sessions.iterator(); i.hasNext();)
        {
            Session session=(Session)i.next();
            session.invalidate();
        }
        _sessions.clear();
        
    }

    /* ------------------------------------------------------------ */
    protected AbstractSessionManager.Session newSession(HttpServletRequest request)
    {
        return new Session(request);
    }
    
    /* ------------------------------------------------------------ */
    protected void removeSession(String clusterId)
    {
        _sessions.remove(clusterId);
    }
    
    /* ------------------------------------------------------------ */
    public void setStoreDirectory (File dir)
    {
        _storeDir=dir;
    }
    
    /* ------------------------------------------------------------ */
    public File getStoreDirectory ()
    {
        return _storeDir;
    }
    
    /* ------------------------------------------------------------ */
    public void setLazyLoad(boolean lazyLoad)
    {
        _lazyLoad = lazyLoad;
    }

    /* ------------------------------------------------------------ */
    public boolean isLazyLoad()
    {
        return _lazyLoad;
    }

    /* ------------------------------------------------------------ */
    public void restoreSessions () throws Exception
    {
        if (_storeDir==null || !_storeDir.exists())
        {
            return;
        }

        if (!_storeDir.canRead())
        {
            Log.warn ("Unable to restore Sessions: Cannot read from Session storage directory "+_storeDir.getAbsolutePath());
            return;
        }

        File[] files = _storeDir.listFiles();
        for (int i=0;files!=null&&i<files.length;i++)
        {
            try
            {
                FileInputStream in = new FileInputStream(files[i]);           
                Session session = restoreSession(in);
                in.close();          
                addSession(session, false);
                files[i].delete();
            }
            catch (Exception e)
            {
                Log.warn("Problem restoring session "+files[i].getName(), e);
            }
        }
        
        _sessionsLoaded = true;
    }

    /* ------------------------------------------------------------ */
    public void saveSessions () throws Exception
    {
        if (_storeDir==null || !_storeDir.exists())
        {
            return;
        }
        
        if (!_storeDir.canWrite())
        {
            Log.warn ("Unable to save Sessions: Session persistence storage directory "+_storeDir.getAbsolutePath()+ " is not writeable");
            return;
        }
 
        synchronized (this)
        {
            Iterator itor = _sessions.entrySet().iterator();
            while (itor.hasNext())
            {
                Map.Entry entry = (Map.Entry)itor.next();
                String id = (String)entry.getKey();
                Session session = (Session)entry.getValue();
                try
                {
                    File file = new File (_storeDir, id);
                    if (file.exists())
                        file.delete();
                    file.createNewFile();
                    FileOutputStream fos = new FileOutputStream (file);
                    session.save(fos);
                    fos.close();
                }
                catch (Exception e)
                {
                    Log.warn("Problem persisting session "+id, e);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    public Session restoreSession (FileInputStream fis) 
    throws Exception
    {

        /*
         * Take care of this class's fields first by calling 
         * defaultReadObject
         */
        
        DataInputStream in = new DataInputStream(fis);
        String clusterId = in.readUTF();
        String nodeId = in.readUTF();
        boolean idChanged = in.readBoolean();
        long created = in.readLong();
        long cookieSet = in.readLong();
        long accessed = in.readLong();
        long lastAccessed = in.readLong();
        //boolean invalid = in.readBoolean();
        //boolean invalidate = in.readBoolean();
        //long maxIdle = in.readLong();
        //boolean isNew = in.readBoolean();
        int requests = in.readInt();
        
        Session session = new Session (created, clusterId);
        session._cookieSet = cookieSet;
        session._lastAccessed = lastAccessed;
        
        int size = in.readInt();
        if (size > 0)
        {
            ArrayList keys = new ArrayList();
            for (int i=0; i<size; i++)
            {
                String key = in.readUTF();
                keys.add(key);
            }
            ClassLoadingObjectInputStream ois = new ClassLoadingObjectInputStream(in);
            for (int i=0;i<size;i++)
            {
                Object value = ois.readObject();
                session.setAttribute((String)keys.get(i),value);
            }
            ois.close();
        }
        else
            session.initValues();
        in.close();
        return session;
    }

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class Session extends AbstractSessionManager.Session
    {
        /* ------------------------------------------------------------ */
        private static final long serialVersionUID=-2134521374206116367L;
        
   

        /* ------------------------------------------------------------- */
        protected Session(HttpServletRequest request)
        {
            super(request);
        }
        
        protected Session(long created, String clusterId)
        {
            super(created, clusterId);
        }
        /* ------------------------------------------------------------- */
        public void setMaxInactiveInterval(int secs)
        {
            super.setMaxInactiveInterval(secs);
            if (_maxIdleMs>0&&(_maxIdleMs/10)<_scavengePeriodMs)
                HashSessionManager.this.setScavengePeriod((secs+9)/10);
        }
        
        /* ------------------------------------------------------------ */
        protected Map newAttributeMap()
        {
            return new HashMap(3);
        }
        
 
        public void invalidate ()
        throws IllegalStateException
        {
            super.invalidate();
            remove(getId());
        }
        
        public void remove (String id)
        {
            if (id==null)
                return;
            
            //all sessions are invalidated when jetty is stopped, make sure we don't
            //remove all the sessions in this case
            if (isStopping() || isStopped())
                return;
            
            if (_storeDir==null || !_storeDir.exists())
            {
                return;
            }
            
            File f = new File(_storeDir, id);
            f.delete();
        }
        
        public void save(FileOutputStream fos)  throws IOException 
        {
            DataOutputStream out = new DataOutputStream(fos);
            out.writeUTF(_clusterId);
            out.writeUTF(_nodeId);
            out.writeBoolean(_idChanged);
            out.writeLong( _created);
            out.writeLong(_cookieSet);
            out.writeLong(_accessed);
            out.writeLong(_lastAccessed);
            /* Don't write these out, as they don't make sense to store because they
             * either they cannot be true or their value will be restored in the 
             * Session constructor.
             */
            //out.writeBoolean(_invalid);
            //out.writeBoolean(_doInvalidate);
            //out.writeLong(_maxIdleMs);
            //out.writeBoolean( _newSession);
            out.writeInt(_requests);
            if (_values != null)
            {
                out.writeInt(_values.size());
                Iterator itor = _values.keySet().iterator();
                while (itor.hasNext())
                {
                    String key = (String)itor.next();
                    out.writeUTF(key);
                }
                itor = _values.values().iterator();
                ObjectOutputStream oos = new ObjectOutputStream(out);
                while (itor.hasNext())
                {
                    oos.writeObject(itor.next());
                }
                oos.close();
            }
            else
                out.writeInt(0);
            out.close();
        }
        
    }
    
    protected class ClassLoadingObjectInputStream extends ObjectInputStream
    {
        public ClassLoadingObjectInputStream(java.io.InputStream in)
        throws IOException
        {
            super(in);
        }
        
        public ClassLoadingObjectInputStream ()
        throws IOException
        {
            super();
        }
        
        public Class resolveClass (java.io.ObjectStreamClass cl)
        throws IOException, ClassNotFoundException
        {
            Class clazz;
            try
            {
                return Class.forName(cl.getName(), false, Thread.currentThread().getContextClassLoader());
            }
            catch (ClassNotFoundException e)
            {
                return super.resolveClass(cl);
            }
        }
    }

    
}
