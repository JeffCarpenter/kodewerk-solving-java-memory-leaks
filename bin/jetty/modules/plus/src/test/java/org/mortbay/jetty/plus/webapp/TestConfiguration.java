package org.mortbay.jetty.plus.webapp;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.plus.naming.EnvEntry;
import org.mortbay.jetty.plus.naming.NamingEntry;
import org.mortbay.jetty.plus.naming.NamingEntryUtil;
import org.mortbay.jetty.webapp.WebAppContext;

import junit.framework.TestCase;

public class TestConfiguration extends TestCase
{
    
    public class MyWebAppContext extends WebAppContext
    {
        public String toString()
        {
            return this.getClass().getName()+"@"+super.hashCode();
        }
    }
    
    public void testIt ()
    throws Exception
    {
        InitialContext ic = new InitialContext();
        
        Server server = new Server();
        
        WebAppContext wac = new MyWebAppContext();
        wac.setServer(server);
        
        //bind some EnvEntrys at the server level 
        EnvEntry ee1 = new EnvEntry(server, "xxx/a", "100", true);
        EnvEntry ee2 = new EnvEntry(server, "yyy/b", "200", false);
        EnvEntry ee3 = new EnvEntry(server, "zzz/c", "300", false);
        EnvEntry ee4 = new EnvEntry(server, "zzz/d", "400", false);
        EnvEntry ee5 = new EnvEntry(server, "zzz/f", "500", true);
        
        //bind some EnvEntrys at the webapp level 
        EnvEntry ee6 = new EnvEntry(wac, "xxx/a", "900", true);
        EnvEntry ee7 = new EnvEntry(wac, "yyy/b", "910", true);
        EnvEntry ee8 = new EnvEntry(wac, "zzz/c", "920", false);
        EnvEntry ee9 = new EnvEntry(wac, "zzz/e", "930", false);
        
        assertNotNull(NamingEntryUtil.lookupNamingEntry(server, "xxx/a"));
        assertNotNull(NamingEntryUtil.lookupNamingEntry(server, "yyy/b"));
        assertNotNull(NamingEntryUtil.lookupNamingEntry(server, "zzz/c"));
        assertNotNull(NamingEntryUtil.lookupNamingEntry(server, "zzz/d"));
        assertNotNull(NamingEntryUtil.lookupNamingEntry(wac, "xxx/a"));
        assertNotNull(NamingEntryUtil.lookupNamingEntry(wac, "yyy/b"));
        assertNotNull(NamingEntryUtil.lookupNamingEntry(wac, "zzz/c")); 
        assertNotNull(NamingEntryUtil.lookupNamingEntry(wac, "zzz/e"));
        
        Configuration config = new Configuration();
        config.setWebAppContext(wac);
        EnvConfiguration envConfig = new EnvConfiguration();
        envConfig.setWebAppContext(wac);
        envConfig.configureDefaults();
        envConfig.bindEnvEntries();
        
        String val = (String)ic.lookup("java:comp/env/xxx/a");
        assertEquals("900", val); //webapp naming overrides server
        val = (String)ic.lookup("java:comp/env/yyy/b");
        assertEquals("910", val);//webapp overrides server
        val = (String)ic.lookup("java:comp/env/zzz/c");
        assertEquals("920",val);//webapp overrides server
        val = (String)ic.lookup("java:comp/env/zzz/d");
        assertEquals("400", val);//from server naming
        val = (String)ic.lookup("java:comp/env/zzz/e");
        assertEquals("930", val);//from webapp naming
        
        NamingEntry ne = (NamingEntry)ic.lookup("java:comp/env/"+NamingEntry.__contextName+"/xxx/a");
        assertNotNull(ne);
        ne = (NamingEntry)ic.lookup("java:comp/env/"+NamingEntry.__contextName+"/yyy/b");
        assertNotNull(ne);
        ne = (NamingEntry)ic.lookup("java:comp/env/"+NamingEntry.__contextName+"/zzz/c");
        assertNotNull(ne);
        ne = (NamingEntry)ic.lookup("java:comp/env/"+NamingEntry.__contextName+"/zzz/d");
        assertNotNull(ne);
        ne = (NamingEntry)ic.lookup("java:comp/env/"+NamingEntry.__contextName+"/zzz/e");
        assertNotNull(ne);
        
        config.bindEnvEntry("foo", "99");
        assertEquals("99",ic.lookup( "java:comp/env/foo"));
        
        config.bindEnvEntry("xxx/a", "7");
        assertEquals("900", ic.lookup("java:comp/env/xxx/a")); //webapp overrides web.xml
        config.bindEnvEntry("yyy/b", "7");
        assertEquals("910", ic.lookup("java:comp/env/yyy/b"));//webapp overrides web.xml
        config.bindEnvEntry("zzz/c", "7");
        assertEquals("7", ic.lookup("java:comp/env/zzz/c"));//webapp does NOT override web.xml
        config.bindEnvEntry("zzz/d", "7");
        assertEquals("7", ic.lookup("java:comp/env/zzz/d"));//server does NOT override web.xml
        config.bindEnvEntry("zzz/e", "7");
        assertEquals("7", ic.lookup("java:comp/env/zzz/e"));//webapp does NOT override web.xml
        config.bindEnvEntry("zzz/f", "7");
        assertEquals("500", ic.lookup("java:comp/env/zzz/f"));//server overrides web.xml
        
        ((Context)ic.lookup("java:comp")).destroySubcontext("env");
        ic.destroySubcontext("xxx");
        ic.destroySubcontext("yyy");
        ic.destroySubcontext("zzz");
    }
}
