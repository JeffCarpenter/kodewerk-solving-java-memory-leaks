package com.kodewerk.db;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.util.Map;
import java.util.HashMap;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.kodewerk.stock.ClosingPriceList;
import com.kodewerk.db.ClosingPriceDataSourceException;
import com.kodewerk.stock.ClosingPrice;
import com.kodewerk.stock.StockProperties;
import com.kodewerk.stock.TickerSymbol;
import com.kodewerk.cache.PriceCache;

/**
 * ClosingPriceDataSource
 *
 * @author kirk
 * @version 1.0
 * @since 11:35:27 PM
 */
public class ClosingPriceRDBDataSource implements ClosingPriceDataSource {

    private PriceCache cache = PriceCache.priceCache();
    private boolean cacheEnabled;
    private DataBase db;

    public ClosingPriceRDBDataSource() throws ClosingPriceDataSourceException {
        this.cacheEnabled = StockProperties.getCacheEnabled();
        db = new DataBase();
        String driver = StockProperties.getDriver();
        String url = StockProperties.getURL();
        String user = StockProperties.getUser();
        String password = StockProperties.getPassword();

        try {
            db.connect( driver, url, user, password);
        } catch (DataBaseException e) {
            throw new ClosingPriceDataSourceException( e);
        }
    }

    synchronized private void cache( ClosingPriceList list) {
        if (cacheEnabled)
            cache.put( list);
    }

    synchronized private ClosingPriceList get( TickerSymbol ticker) {
            return cache.get( ticker);
    }

    public ClosingPrice getLatestClosingPrice( TickerSymbol ticker) throws ClosingPriceDataSourceException {
        return load( ticker).getLastClosingPrice();
    }

    public ClosingPriceList load( TickerSymbol ticker) throws ClosingPriceDataSourceException {
        ClosingPriceList closingPriceList = this.get( ticker);
        if ( closingPriceList != null)
            return closingPriceList;

        try {
            ResultSet closingPrices = db.getClosingPrices( ticker);
            closingPriceList = new ClosingPriceList( ticker);
            this.parseResultSet( closingPrices, closingPriceList);
            closingPrices.close();
            this.cache( closingPriceList);
            return closingPriceList;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new ClosingPriceDataSourceException(e);
        }
    }


    private void parseResultSet( ResultSet closingPrices, ClosingPriceList closingPriceList) throws SQLException {
        while ( closingPrices.next()) {
            String date = closingPrices.getString( 2);
            String open = closingPrices.getString( 3);
            String high = closingPrices.getString( 4);
            String low = closingPrices.getString( 5);
            String close = closingPrices.getString( 6);
            String volume = closingPrices.getString( 7);
            String adjClose = closingPrices.getString( 8);
            ClosingPrice closingPrice = new ClosingPrice(date, open, high, low, close, volume, adjClose);
            closingPriceList.addClosingPrice( closingPrice);
        }
    }
}
