package com.kodewerk.db;

/**
 * DataBaseException
 * <p/>
 * Description goes here
 *
 * @author kirk
 * @version 1.0
 *          Copyright Kodewerk Ltd. All rights reserved.
 * @since 8:57:40 PM,Sep 27, 2008
 */
public class DataBaseException extends Exception {

    public DataBaseException() {
    }

    public DataBaseException(String s) {
        super(s);
    }

    public DataBaseException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public DataBaseException(Throwable throwable) {
        super(throwable);
    }
}
