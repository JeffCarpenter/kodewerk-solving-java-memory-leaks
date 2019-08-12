package com.kodewerk.stock;

import java.util.Map;
//import java.util.IdentityHashMap;
import java.util.HashMap;

/**
 * CrumbTrail
 *
 * @author kirk
 * @version 1.0
 * @since 6:58:19 AM
 */
public class CrumbTrail {

    private Map<String,ClosingPriceList> map;

    public CrumbTrail() {
        this.map = new HashMap<>();
    }

    public void addCrumb( ClosingPriceList list) {
        map.put( list.getTicker(), list);
    }

    public ClosingPriceList getCrumb( String ticker) {		
        return map.get( ticker);
    }
}
