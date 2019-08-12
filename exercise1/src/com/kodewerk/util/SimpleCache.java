package com.kodewerk.util;

import java.util.IdentityHashMap;
import java.util.Map;
import javax.management.*;

/**
 * @author kirk
 * @since Dec 4, 2005
 * Copyright (c) Dec 4, 2005 Kodewerk.  All rights reserved.
 */
public class SimpleCache extends NotificationBroadcasterSupport implements Cache, CacheMXBean {

    private Map cache;

    public SimpleCache() {
        this.cache = new IdentityHashMap();
    }

    public synchronized Object get( Object key) {
        return this.cache.get( key);
    }

    public synchronized void put( Object key, Object value) {
        this.cache.put( key, value);
    }

    public int getSize() {
        return this.cache.size();
    }

    public void clear() {
        this.cache = new IdentityHashMap();
    }
}
