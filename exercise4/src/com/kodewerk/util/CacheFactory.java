package com.kodewerk.util;

import java.util.IdentityHashMap;
import java.util.Map;
import javax.management.*;
import java.lang.management.*;

/**
 * @author kirk
 * @since Dec 4, 2005
 * Copyright (c) Dec 4, 2005 Kodewerk.  All rights reserved.
 */
public class CacheFactory implements Cache {

    private static Cache singleton;

    public static Cache getCache() {
        if ( singleton == null) {
            singleton = new NullCache();
        }
        return singleton;
    }

    public synchronized static void setCache( String policy) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InstanceAlreadyExistsException, MalformedObjectNameException, JMException  {
        if ((policy != null) && ( singleton == null)) {
            Class clazz = Class.forName( policy, true, Thread.currentThread().getContextClassLoader());
            singleton = (Cache)clazz.newInstance();

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("com.kodewerk:type=cache,name=jdbc");
            mbs.registerMBean( singleton, name);
        }
    }

    private Map cache;

    private CacheFactory() {
        this.cache = new IdentityHashMap();
    }

    public synchronized Object get( Object key) {
        return this.cache.get( key);
    }

    public synchronized void put( Object key, Object value) {
        this.cache.put( key, value);
    }

    static class NullCache implements Cache {
        public NullCache() {}
        public void put( Object key, Object value) {}
        public Object get( Object key) { return null; }
    }
}
