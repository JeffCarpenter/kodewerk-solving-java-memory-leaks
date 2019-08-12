//========================================================================
//$Id: Scanner.java 3022 2008-06-18 04:06:37Z janb $
//Copyright 2006 Mort Bay Consulting Pty. Ltd.
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


package org.mortbay.util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.mortbay.log.Log;


/**
 * Scanner
 * 
 * Utility for scanning a directory for added, removed and changed
 * files and reporting these events via registered Listeners.
 *
 * TODO AbstractLifeCycle
 */
public class Scanner
{
    private int _scanInterval;
    private List _listeners = Collections.synchronizedList(new ArrayList());
    private Map _prevScan = new HashMap();
    private Map _currentScan = new HashMap();
    private FilenameFilter _filter;
    private List _scanDirs;
    private volatile boolean _running = false;
    private boolean _reportExisting = true;
    private Timer _timer;
    private TimerTask _task;
    private boolean _recursive=true;


    /**
     * Listener
     * 
     * Marker for notifications re file changes.
     */
    public interface Listener
    {
    }

    
    public interface DiscreteListener extends Listener
    {
        public void fileChanged (String filename) throws Exception;
        public void fileAdded (String filename) throws Exception;
        public void fileRemoved (String filename) throws Exception;
    }
    
    
    public interface BulkListener extends Listener
    {
        public void filesChanged (List filenames) throws Exception;
    }


    /**
     * 
     */
    public Scanner ()
    {       
    }

    /**
     * Get the scan interval
     * @return interval between scans in seconds
     */
    public int getScanInterval()
    {
        return _scanInterval;
    }

    /**
     * Set the scan interval
     * @param scanInterval pause between scans in seconds
     */
    public synchronized void setScanInterval(int scanInterval)
    {
        this._scanInterval = scanInterval;
        schedule();
    }

    /**
     * Set the location of the directory to scan.
     * @param dir
     * @deprecated use setScanDirs(List dirs) instead
     */
    public void setScanDir (File dir)
    {
        _scanDirs = new ArrayList();
        _scanDirs.add(dir);
    }

    /**
     * Get the location of the directory to scan
     * @return
     * @deprecated use getScanDirs() instead
     */
    public File getScanDir ()
    {
        return (_scanDirs==null?null:(File)_scanDirs.get(0));
    }

    public void setScanDirs (List dirs)
    {
        _scanDirs = dirs;
    }
    
    public List getScanDirs ()
    {
        return _scanDirs;
    }
    
    public void setRecursive (boolean recursive)
    {
        _recursive=recursive;
    }
    
    public boolean getRecursive ()
    {
        return _recursive;
    }
    /**
     * Apply a filter to files found in the scan directory.
     * Only files matching the filter will be reported as added/changed/removed.
     * @param filter
     */
    public void setFilenameFilter (FilenameFilter filter)
    {
        this._filter = filter;
    }

    /**
     * Get any filter applied to files in the scan dir.
     * @return
     */
    public FilenameFilter getFilenameFilter ()
    {
        return _filter;
    }

    /**
     * Whether or not an initial scan will report all files as being
     * added.
     * @param reportExisting if true, all files found on initial scan will be 
     * reported as being added, otherwise not
     */
    public void setReportExistingFilesOnStartup (boolean reportExisting)
    {
        this._reportExisting = reportExisting;
    }

    /**
     * Add an added/removed/changed listener
     * @param listener
     */
    public synchronized void addListener (Listener listener)
    {
        if (listener == null)
            return;
        _listeners.add(listener);   
    }



    /**
     * Remove a registered listener
     * @param listener the Listener to be removed
     */
    public synchronized void removeListener (Listener listener)
    {
        if (listener == null)
            return;
        _listeners.remove(listener);    
    }


    /**
     * Start the scanning action.
     */
    public synchronized void start ()
    {
        if (_running)
            return;

        _running = true;

        if (_reportExisting)
        {
            // if files exist at startup, report them
            scan();
        }
        else
        {
            //just register the list of existing files and only report changes
            scanFiles();
            _prevScan.putAll(_currentScan);
        }
        schedule();
    }

    public TimerTask newTimerTask ()
    {
        return new TimerTask()
        {
            public void run() { scan(); }
        };
    }

    public Timer newTimer ()
    {
        return new Timer(true);
    }
    
    public void schedule ()
    {  
        if (_running)
        {
            if (_timer!=null)
                _timer.cancel();
            if (_task!=null)
                _task.cancel();
            if (getScanInterval() > 0)
            {
                _timer = newTimer();
                _task = newTimerTask();
                _timer.schedule(_task, 1000L*getScanInterval(),1000L*getScanInterval());
            }
        }
    }
    /**
     * Stop the scanning.
     */
    public synchronized void stop ()
    {
        if (_running)
        {
            _running = false; 
            if (_timer!=null)
                _timer.cancel();
            if (_task!=null)
                _task.cancel();
            _task=null;
            _timer=null;
        }
    }

    /**
     * Perform a pass of the scanner and report changes
     */
    public void scan ()
    {
        scanFiles();
        reportDifferences(_currentScan, _prevScan);
        _prevScan.clear();
        _prevScan.putAll(_currentScan);
    }

    /**
     * Recursively scan all files in the designated directories.
     * @return Map of name of file to last modified time
     */
    public void scanFiles ()
    {
        if (_scanDirs==null)
            return;
        
        _currentScan.clear();
        Iterator itor = _scanDirs.iterator();
        while (itor.hasNext())
        {
            File dir = (File)itor.next();
            
            if ((dir != null) && (dir.exists()))
                scanFile(dir, _currentScan);
        }
    }


    /**
     * Report the adds/changes/removes to the registered listeners
     * 
     * @param currentScan the info from the most recent pass
     * @param oldScan info from the previous pass
     */
    public void reportDifferences (Map currentScan, Map oldScan) 
    {
        List bulkChanges = new ArrayList();
        
        Set oldScanKeys = new HashSet(oldScan.keySet());
        Iterator itor = currentScan.entrySet().iterator();
        while (itor.hasNext())
        {
            Map.Entry entry = (Map.Entry)itor.next();
            if (!oldScanKeys.contains(entry.getKey()))
            {
                Log.debug("File added: "+entry.getKey());
                reportAddition ((String)entry.getKey());
                bulkChanges.add(entry.getKey());
            }
            else if (!oldScan.get(entry.getKey()).equals(entry.getValue()))
            {
                Log.debug("File changed: "+entry.getKey());
                reportChange((String)entry.getKey());
                oldScanKeys.remove(entry.getKey());
                bulkChanges.add(entry.getKey());
            }
            else
                oldScanKeys.remove(entry.getKey());
        }

        if (!oldScanKeys.isEmpty())
        {

            Iterator keyItor = oldScanKeys.iterator();
            while (keyItor.hasNext())
            {
                String filename = (String)keyItor.next();
                Log.debug("File removed: "+filename);
                reportRemoval(filename);
                bulkChanges.add(filename);
            }
        }
        
        if (!bulkChanges.isEmpty())
            reportBulkChanges(bulkChanges);
    }


    /**
     * Get last modified time on a single file or recurse if
     * the file is a directory. 
     * @param f file or directory
     * @param scanInfoMap map of filenames to last modified times
     */
    private void scanFile (File f, Map scanInfoMap)
    {
        try
        {
            if (!f.exists())
                return;

            if (f.isFile())
            {
                if ((_filter == null) || ((_filter != null) && _filter.accept(f.getParentFile(), f.getName())))
                {
                    String name = f.getCanonicalPath();
                    long lastModified = f.lastModified();
                    scanInfoMap.put(name, new Long(lastModified));
                }
            }
            else if (f.isDirectory() && (_recursive || _scanDirs.contains(f)))
            {
                File[] files = f.listFiles();
                for (int i=0;i<files.length;i++)
                    scanFile(files[i], scanInfoMap);
            }
        }
        catch (IOException e)
        {
            Log.warn("Error scanning watched files", e);
        }
    }

    private void warn(Object listener,String filename,Throwable th)
    {
        Log.warn(th);
        Log.warn(listener+" failed on '"+filename);
    }

    /**
     * Report a file addition to the registered FileAddedListeners
     * @param filename
     */
    private void reportAddition (String filename)
    {
        Iterator itor = _listeners.iterator();
        while (itor.hasNext())
        {
            Object l = itor.next();
            try
            {
                if (l instanceof DiscreteListener)
                    ((DiscreteListener)l).fileAdded(filename);
            }
            catch (Exception e)
            {
                warn(l,filename,e);
            }
            catch (Error e)
            {
                warn(l,filename,e);
            }
        }
    }


    /**
     * Report a file removal to the FileRemovedListeners
     * @param filename
     */
    private void reportRemoval (String filename)
    {
        Iterator itor = _listeners.iterator();
        while (itor.hasNext())
        {
            Object l = itor.next();
            try
            {
                if (l instanceof DiscreteListener)
                    ((DiscreteListener)l).fileRemoved(filename);
            }
            catch (Exception e)
            {
                warn(l,filename,e);
            }
            catch (Error e)
            {
                warn(l,filename,e);
            }
        }
    }


    /**
     * Report a file change to the FileChangedListeners
     * @param filename
     */
    private void reportChange (String filename)
    {
        Iterator itor = _listeners.iterator();
        while (itor.hasNext())
        {
            Object l = itor.next();
            try
            {
                if (l instanceof DiscreteListener)
                    ((DiscreteListener)l).fileChanged(filename);
            }
            catch (Exception e)
            {
                warn(l,filename,e);
            }
            catch (Error e)
            {
                warn(l,filename,e);
            }
        }
    }
    
    private void reportBulkChanges (List filenames)
    {
        Iterator itor = _listeners.iterator();
        while (itor.hasNext())
        {
            Object l = itor.next();
            try
            {
                if (l instanceof BulkListener)
                    ((BulkListener)l).filesChanged(filenames);
            }
            catch (Exception e)
            {
                warn(l,filenames.toString(),e);
            }
            catch (Error e)
            {
                warn(l,filenames.toString(),e);
            }
        }
    }

}
