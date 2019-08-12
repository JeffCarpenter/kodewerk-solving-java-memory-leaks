package com.kodewerk.db;

/**
 * ClosingPriceDataSourceException
 *
 * @author kirk
 * @version 1.0
 * @since 12:34:20 AM
 */
public class ClosingPriceDataSourceException extends Exception {

    public ClosingPriceDataSourceException() {
    }

    public ClosingPriceDataSourceException(String message) {
        super(message);
    }

    public ClosingPriceDataSourceException(Throwable cause) {
        super(cause);
    }

    public ClosingPriceDataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}