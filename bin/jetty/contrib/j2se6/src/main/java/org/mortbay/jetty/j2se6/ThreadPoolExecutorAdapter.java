//========================================================================
//$$Id: ThreadPoolExecutorAdapter.java,v 1.3 2007/11/02 12:39:41 ludovic_orban Exp $$
//
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

package org.mortbay.jetty.j2se6;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.mortbay.component.LifeCycle;
import org.mortbay.log.Log;
import org.mortbay.thread.ThreadPool;

/**
 * Jetty {@link ThreadPool} that bridges requests to a {@link ThreadPoolExecutor}.
 * 
 */
public class ThreadPoolExecutorAdapter implements ThreadPool, LifeCycle
{

    private ThreadPoolExecutor executor;

    public ThreadPoolExecutorAdapter(ThreadPoolExecutor executor)
    {
        this.executor = executor;
    }

    public boolean dispatch(Runnable job)
    {
        try
        {       
            executor.execute(job);
            return true;
        }
        catch(RejectedExecutionException e)
        {
            Log.warn(e);
            return false;
        }
    }

    public int getIdleThreads()
    {
        return executor.getPoolSize()-executor.getActiveCount();
    }

    public int getThreads()
    {
        return executor.getPoolSize();
    }

    public boolean isLowOnThreads()
    {
        return executor.getActiveCount()>=executor.getMaximumPoolSize();
    }

    public void join() throws InterruptedException
    {
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }


    public boolean isFailed()
    {
        return false;
    }

    public boolean isRunning()
    {
        return !executor.isTerminated() && !executor.isTerminating();
    }

    public boolean isStarted()
    {
        return !executor.isTerminated() && !executor.isTerminating();
    }

    public boolean isStarting()
    {
        return false;
    }

    public boolean isStopped()
    {
        return executor.isTerminated();
    }

    public boolean isStopping()
    {
        return executor.isTerminating();
    }

    public void start() throws Exception
    {
        if (executor.isTerminated() || executor.isTerminating() || executor.isShutdown())
            throw new IllegalStateException("Cannot restart");
    }

    public void stop() throws Exception
    {
        executor.shutdown();
        if (!executor.awaitTermination(60,TimeUnit.SECONDS))
            executor.shutdownNow();
    }

    @Override
    public void addLifeCycleListener(Listener listener)
    {
        throw new UnsupportedOperationException ("LifeCycleListeners not implemented");
    }

    @Override
    public void removeLifeCycleListener(Listener listener)
    {
        throw new UnsupportedOperationException ("LifeCycleListeners not implemented");
    }

}
