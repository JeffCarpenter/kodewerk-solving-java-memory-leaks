package com.kodewerk.stock;

/**
 * TickerSymbol
 *
 * @author kirk
 * @version 1.0
 * @since 6:58:19 AM
 */
public class TickerSymbol {

    final private String tickerSymbol;

    public TickerSymbol(String symbol) {
        this.tickerSymbol = symbol;
    }

    @Override
    public boolean equals(Object other) {
        if ( other instanceof TickerSymbol)
            return ((TickerSymbol)other).tickerSymbol.equals(this.tickerSymbol);
        return false;
    }

    public String toString() {
        return tickerSymbol;
    }
}
