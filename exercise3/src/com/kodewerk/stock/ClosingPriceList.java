package com.kodewerk.stock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Comparator;

/**
 * ClosingPriceList
 *
 * @author kirk
 * @version 1.0
 * @since 8:52:38 PM
 */
public class ClosingPriceList {

    private String ticker;
    private ArrayList<ClosingPrice> data;

    public ClosingPriceList( String ticker) {
        this.ticker = ticker;
        this.data = new ArrayList<ClosingPrice>();
    }

    synchronized public String getTicker() { return this.ticker; }

    synchronized public void addClosingPrice( ClosingPrice price) {
        this.data.add( price);
    }

    synchronized public ClosingPrice getLastClosingPrice() {
        return data.get( data.size() - 1);
    }

    synchronized public Iterator<ClosingPrice> iterator() {
        return data.iterator();
    }

    synchronized public ClosingPrice[] sharpestRise( int gap) {
        ClosingPrice first = null;
        ClosingPrice second = null;
        ClosingPrice[] pair = new ClosingPrice[ 2];
        double increase = 0.0;
        double firstPrice = 0.0;
        double secondPrice = 0.0;
        Iterator iter = this.iterator();
        if ( iter.hasNext()) {
            first = (ClosingPrice)iter.next();
            firstPrice = Double.parseDouble( first.getAdjustedClose());
        }
        if ( iter.hasNext()) {
            second = (ClosingPrice)iter.next();
            secondPrice = Double.parseDouble( second.getAdjustedClose());
        }
        increase = secondPrice - firstPrice;
        pair[ 0] = first;
        pair[ 1] = second;
        while ( iter.hasNext()) {
            first = second;
            firstPrice = secondPrice;
            second = (ClosingPrice)iter.next();
            secondPrice = Double.parseDouble( second.getAdjustedClose());
            if ( ( secondPrice - firstPrice) > increase) {
                increase = secondPrice - firstPrice;
                pair[ 0] = first;
                pair[ 1] = second;
            }
        }

        return pair;
    }

    // todo: calculation
    public ClosingPrice find90thPercentile() {
   //     ArrayList<ClosingPrice> dataset = (ArrayList<ClosingPrice>)this.data.clone();
   //     Arrays.sort( dataset, new Comparator<ClosingPriceList>() {
   //         public int compare( Object one, Object two) {
   //             return (int)(Double.parseDouble(((ClosingPrice)one).getAdjustedClose()) - Double.parseDouble(((ClosingPrice)two).getAdjustedClose()));
   //         }
   //
   //     });
        return data.get( 0);
    }

    public ClosingPrice findFridayOfHigestValueWithFallOnMonday() {
        return data.get( 0);
    }

    public ClosingPrice findHigh() {
        ClosingPrice high = null;
        Iterator iter = this.iterator();
        if ( iter.hasNext())
            high = (ClosingPrice)iter.next();
        while ( iter.hasNext()) {
            ClosingPrice price = (ClosingPrice)iter.next();
            if ( high.lessThan( price ))
                high = price;
        }

        return high;
    }

    //todo: calculation
    public ClosingPrice findLow() {
        return data.get( 0);
    }

    //todo: calculation
    public ClosingPrice findHighestVolume() {
        return data.get( 0);
    }

    public ClosingPrice findLowestVolume() {
        return data.get( 0);  
    }
}
