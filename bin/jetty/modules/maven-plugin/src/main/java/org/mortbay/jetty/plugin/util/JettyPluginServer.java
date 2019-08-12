//========================================================================
//$Id: JettyPluginServer.java 2094 2007-09-10 06:11:26Z janb $
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
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

package org.mortbay.jetty.plugin.util;

import org.mortbay.jetty.webapp.WebAppContext;

/**
 * JettyPluginServer
 * 
 * 
 * Type to hide differences in API for different versions
 * of Jetty for Server class.
 *
 */
public interface JettyPluginServer extends Proxy
{
    public void setRequestLog(Object requestLog);
    
    public Object getRequestLog();
  
    public void setConnectors (Object[] connectors) throws Exception;
    public Object[] getConnectors();
   
    public  void setUserRealms (Object[] realms) throws Exception;
    public Object[] getUserRealms();
    
    public void configureHandlers () throws Exception;
    
    public  void addWebApplication (WebAppContext webapp) throws Exception;
    
    public  void start() throws Exception;
    
    public Object createDefaultConnector (String port) throws Exception;
    
    public void join () throws Exception;

}
