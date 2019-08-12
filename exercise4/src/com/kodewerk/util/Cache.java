package com.kodewerk.util;

/**
 * Created by IntelliJ IDEA.
 * User: kirk
 */
public interface Cache {

    public Object get( Object key);
    public void put( Object key, Object value);
}
