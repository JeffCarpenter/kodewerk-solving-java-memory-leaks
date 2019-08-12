package com.kodewerk.db;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.io.*;

import com.kodewerk.stock.ClosingPriceList;
import com.kodewerk.db.ClosingPriceDataSourceException;
import com.kodewerk.stock.ClosingPrice;
import com.kodewerk.stock.StockProperties;

/**
 * ClosingPriceDataSource
 *
 * @author kirk
 * @version 1.0
 * @since 11:35:27 PM
 */
public class ClosingPriceFileSystemDataSource implements ClosingPriceDataSource {

    private static Map<String, ClosingPriceList> cache;

    private String sourceDirectory;
    private boolean cacheEnabled;

    public ClosingPriceFileSystemDataSource() throws ClosingPriceDataSourceException {
        this.cacheEnabled = StockProperties.getCacheEnabled();
        cache = Collections.synchronizedMap(new HashMap<String,ClosingPriceList>());
        this.sourceDirectory = StockProperties.getFileSystem();

        File dataSource = new File( sourceDirectory);
        if ( ! dataSource.isDirectory())
            throw new ClosingPriceDataSourceException( sourceDirectory + " is not a directory");
    }

    private void cache( ClosingPriceList list) {
        if (cacheEnabled)
            cache.put( list.getTicker(), list);
    }

    private ClosingPriceList get( String ticker) {
            return cache.get( ticker);
    }

    synchronized public ClosingPrice getLatestClosingPrice( String ticker) throws ClosingPriceDataSourceException {
        return load( ticker).getLastClosingPrice();
    }

    synchronized public ClosingPriceList load( String ticker) throws ClosingPriceDataSourceException {
        ClosingPriceList closingPriceList = this.get( ticker);
        if ( closingPriceList != null)
            return closingPriceList;

        try {
            InputStream rdr = new DataInputStream(new FileInputStream(sourceDirectory + "/" + ticker + ".xml"));
            closingPriceList = new ClosingPriceList( ticker);
            this.parseXML(rdr, closingPriceList);
            this.cache( closingPriceList);
            return closingPriceList;
        } catch (ParserConfigurationException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new ClosingPriceDataSourceException(e);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new ClosingPriceDataSourceException(e);
        } catch (SAXException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            throw new ClosingPriceDataSourceException(e);
        }
    }

    private void parseXML(InputStream in, ClosingPriceList closingPriceList) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);
        NodeList nl = doc.getElementsByTagName("closingPrice");
        for (int n = 0; n < nl.getLength(); n++) {
            Element price = (Element) nl.item(n);
            String date = price.getAttribute("date");
            String open = price.getAttribute("open");
            String high = price.getAttribute("high");
            String low = price.getAttribute("low");
            String close = price.getAttribute("close");
            String volume = price.getAttribute("volume");
            String adjClose = price.getAttribute("adjClose");
            ClosingPrice closingPrice = new ClosingPrice(date, open, high, low, close, volume, adjClose);
            closingPriceList.addClosingPrice( closingPrice);
        }
    }
}
