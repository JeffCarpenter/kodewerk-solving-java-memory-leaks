package com.kodewerk.util;

/**
 * @author kirk
 * @since Dec 4, 2005
 * Copyright (c) Dec 4, 2005 Kodewerk.  All rights reserved.
 */
public class NullCache implements Cache {

    public NullCache() {
    }

    public synchronized Object get( Object key) {
        return null;
    }

    public synchronized void put( Object key, Object value) {
    }
}
