package com.kodewerk.db;

import com.kodewerk.stock.ClosingPriceList;
import com.kodewerk.stock.ClosingPrice;
import com.kodewerk.stock.TickerSymbol;

/**
 * Created by IntelliJ IDEA.
 * User: kirk
 * Date: Dec 2, 2008
 * Time: 1:08:01 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ClosingPriceDataSource {

    public ClosingPriceList load( TickerSymbol ticker) throws ClosingPriceDataSourceException;
    public ClosingPrice getLatestClosingPrice( TickerSymbol ticker) throws ClosingPriceDataSourceException;
    
}
