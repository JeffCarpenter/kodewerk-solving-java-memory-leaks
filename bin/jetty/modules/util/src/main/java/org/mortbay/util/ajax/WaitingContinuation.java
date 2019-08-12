//========================================================================
//$Id: WaitingContinuation.java,v 1.1 2005/11/14 17:45:56 gregwilkins Exp $
//Copyright 2004-2005 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.util.ajax;

import org.mortbay.log.Log;

public class WaitingContinuation implements org.mortbay.util.ajax.Continuation
{
    Object _mutex;
    Object _object;
    boolean _new=true;
    boolean _resumed=false;
    boolean _pending=false;

    public WaitingContinuation()
    {
        _mutex=this;
    }
    
    public WaitingContinuation(Object mutex)
    {
        _mutex=mutex==null?this:mutex;
    }
    
    public void resume()
    {
        synchronized (_mutex)
        {
            _resumed=true;
            _mutex.notify();
        }
    }
    
    public void reset()
    {
        synchronized (_mutex)
        {
            _resumed=false;
            _pending=false;
            _mutex.notify();
        }
    }

    public boolean isNew()
    {
        return _new;
    }

    public boolean suspend(long timeout)
    {
        synchronized (_mutex)
        {
            _new=false;
            _pending=true;
            boolean result;
            try
            {
                if (!_resumed && timeout>=0)
                {
                    if (timeout==0)
                        _mutex.wait();
                    else if (timeout>0)
                        _mutex.wait(timeout);
                        
                }
            }
            catch (InterruptedException e)
            {
                Log.ignore(e);
            }
            finally
            {
                result=_resumed;
                _resumed=false;
                _pending=false;
            }
            
            return result;
        }
    }
    
    public boolean isPending()
    {
        synchronized (_mutex)
        {
            return _pending;
        }
    }
    
    public boolean isResumed()
    {
        synchronized (_mutex)
        {
            return _resumed;
        }
    }

    public Object getObject()
    {
        return _object;
    }

    public void setObject(Object object)
    {
        _object = object;
    }

    public Object getMutex()
    {
        return _mutex;
    }

    public void setMutex(Object mutex)
    {
        if (_pending && mutex!=_mutex)
            throw new IllegalStateException();
        _mutex = mutex==null ? this : mutex; 
    }

    public String toString()
    {
        synchronized (this)
        {
            return "WaitingContinuation@"+hashCode()+
            (_new?",new":"")+
            (_pending?",pending":"")+
            (_resumed?",resumed":"");
        }
    }
}
