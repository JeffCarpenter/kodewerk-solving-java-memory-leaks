package com.kodewerk.stock;

/**
 * ClosingPrice
 *
 * @author Kirk Pepperdine
 * @version 1.0
 * @since 8:56:33 PM
 * 
 */
public class ClosingPrice {

    private String date;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
    private String adjClose;

    public ClosingPrice( String date, String open, String high, String low, String close, String volume, String adjClose) {
        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.adjClose = adjClose;
    }

    public String getDate() { return this.date; }
    public String getOpen() { return this.open; }
    public String getHigh() { return high; }
    public String getLow() { return low; }
    public String getClose() { return close; }
    public String getVolume() { return volume; }
    public String getAdjustedClose() { return adjClose; }

    public boolean newerThan( ClosingPrice price) {
        return this.getDate().compareToIgnoreCase( price.getDate()) > 0;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append( this.getDate());
        builder.append( ":").append( getOpen());
        builder.append( ":").append( getHigh());
        builder.append( ":").append( getLow());
        builder.append( ":").append( getClose());
        builder.append( ":").append( getVolume());
        builder.append( ":").append( getAdjustedClose());
        return builder.toString();
    }

    public boolean lessThan(ClosingPrice price) {        
        return Double.parseDouble( this.getClose()) < Double.parseDouble( price.getClose());
    }
}
