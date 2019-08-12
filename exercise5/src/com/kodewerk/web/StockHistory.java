package com.kodewerk.web;

import com.kodewerk.db.*;
import com.kodewerk.stock.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * StockHistory
 *
 * @author kirk
 * @version 1.0
 * @since 11:25:30 AM
 */
public class StockHistory extends HistoryServlet {

    private ClosingPriceDataSource ds;

    public void init() {
        try {
            String dataSource = StockProperties.getDataSource();
            Class clazz = Class.forName( dataSource, true, Thread.currentThread().getContextClassLoader());
            this.ds = (ClosingPriceDataSource)clazz.newInstance();
        } catch (Exception e) {
            getServletContext().log("An exception occurred", e);
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ClosingPriceList priceList = null;
        response.setContentType("text/html");

        TickerSymbol ticker = new TickerSymbol(request.getParameter("ticker"));
        if ( ticker != null)
            try {
                String action = request.getParameter( "history");
                if ( "history".equals(action)) {
                    priceList = ds.load( ticker);
                } else if ( "analysis".equals( action)) {
                    priceList = ds.load( ticker);
                } else {
                    action = request.getParameter("lastClose");
                    if ( "last close".equals(action)) {
                        ClosingPrice price = ds.getLatestClosingPrice( ticker);
                        priceList = new ClosingPriceList( ticker);
                        priceList.addClosingPrice( price);
                    }
                }
            } catch (ClosingPriceDataSourceException e) {
                getServletContext().log("An exception occurred", e);
                priceList = null;
            }
        this.writeHTMLDocument(response.getOutputStream(), priceList);
    }


    /**
     * We are going to perform the same operations for POST requests
     * as for GET methods, so this method just sends the request to
     * the doGet method.
     */

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    public void writeHTMLDocument( OutputStream stream, ClosingPriceList priceList) throws IOException {
        this.write( stream, super.getHeader());
        this.write( stream, "<body><H1>Stock History</H1>");
        constructForm( stream);
        constructResultsTable(stream,priceList);
        this.write( stream, "</body></html>");
    }

    private void constructForm( OutputStream stream) throws IOException {
        this.write( stream, "<form action=\"http://localhost:8080/lab/stock\" method=\"post\">");
        this.constructDropdownList( stream);
        this.write( stream, "<input type=\"submit\" name=\"history\" value=\"history\">");
        this.write( stream, "<input type=\"submit\" name=\"lastClose\" value=\"last close\">");
        this.write( stream, "<input type=\"submit\" name=\"analysis\" value=\"analysis\">");
        this.write( stream, "</form>");
    }

    private void constructDropdownList( OutputStream stream) throws IOException {

        Iterator tickerSymbols = new TickerList().iterator();
        this.write( stream, "<select name=\"ticker\">");
        while ( tickerSymbols.hasNext()) {
            String tickerSymbol = (String)tickerSymbols.next().toString();
            this.write( stream, "<option value=\"");
            this.write( stream, tickerSymbol);
            this.write( stream, "\">");
            this.write( stream, tickerSymbol);
            this.write( stream, "</option>");
        }
        this.write( stream,"</select>");
    }


    public void constructResultsTable( OutputStream stream, ClosingPriceList priceList) throws IOException {
        if ( priceList == null) return;
        this.write( stream, "<H2>" + priceList.getTicker().toString() + "</H2>");
        this.write( stream, "<br><table border=\"1\">");
        this.write( stream, "<TR><TH>Date</TH><TH>Open</TH><TH>High</TH><TH>Low</TH><TH>Close</TH><TH>Volume</TH><TH>Adjusted Close</TH></TR>");
        Iterator prices = priceList.iterator();
        boolean even = true;
        while ( prices.hasNext()) {
            ClosingPrice price = (ClosingPrice)prices.next();
            this.write( stream, "<tr class=\"");
            if ( even) 
                this.write( stream, "true");
            else
                this.write( stream, "false");
            this.write( stream, "\"><td>");
            this.write( stream, price.getDate());
            this.write( stream, "</td><td>");
            this.write( stream, price.getOpen());
            this.write( stream, "</td><td>");
            this.write( stream, price.getHigh());
            this.write( stream,"</td><td>");
            this.write( stream, price.getLow());
            this.write( stream, "</td><td>");
            this.write( stream, price.getClose());
            this.write( stream, "</td><td>");
            this.write( stream, price.getVolume());
            this.write( stream, "</td><td>");
            this.write( stream, price.getAdjustedClose());
            this.write( stream, "</td></tr>");
            even = ! even;
        }
        this.write( stream, "</table>");
    }

    private void write( OutputStream stream, String text) throws IOException {
        stream.write(text.getBytes());
    }
}
