package com.kodewerk.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kodewerk.stock.ClosingPriceList;
import com.kodewerk.stock.TickerSymbol;

/**
 * PriceCache
 *
 * @author kirk
 * @version 1.0
 * @since 6:58:19 AM
 */
public class PriceCache {

    private static PriceCache singleton = new PriceCache();

    public static PriceCache priceCache() {
        return singleton;
    }

    private Map<TickerSymbol,ClosingPriceList> map;

    public PriceCache() {
        this.map = new ConcurrentHashMap<>();
    }

    public void put( ClosingPriceList list) {
        map.put( list.getTicker(), list);
    }

    public ClosingPriceList get( TickerSymbol ticker) {		
        return map.get( ticker);
    }
}
