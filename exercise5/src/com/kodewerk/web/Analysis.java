package com.kodewerk.web;

import com.kodewerk.db.ClosingPriceDataSource;
import com.kodewerk.stock.StockProperties;
import com.kodewerk.stock.ClosingPriceList;
import com.kodewerk.stock.TickerList;
import com.kodewerk.stock.ClosingPrice;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: kirk
 * Date: Dec 4, 2008
 * Time: 11:51:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class Analysis extends HttpServlet {

        private ClosingPriceDataSource ds;

    public void init() {
        try {
            String dataSource = StockProperties.getDataSource();
            Class clazz = Class.forName( dataSource, true, Thread.currentThread().getContextClassLoader());
            this.ds = (ClosingPriceDataSource)clazz.newInstance();
        } catch (Exception e) {
            System.out.println( e.getMessage());
            e.printStackTrace();
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.doPost( request, response);
    }

    /**
     * We are going to perform the same operations for POST requests
     * as for GET methods, so this method just sends the request to
     * the doGet method.
     */

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    public ClosingPrice find90thPercentile( ClosingPriceList list) {
        return list.find90thPercentile();
    }

    public ClosingPrice findFridayOfHigestValueWithFallOnMonday( ClosingPriceList list) {
        return list.findFridayOfHigestValueWithFallOnMonday();
    }

    public ClosingPrice findHigh( ClosingPriceList list) {
        return list.findHigh();
    }

    public ClosingPrice findLow( ClosingPriceList list) {
        return list.findLow();
    }

    public ClosingPrice findHighestVolumne( ClosingPriceList list) {
        return list.findHighestVolume();
    }

    public ClosingPrice findLowestVolume( ClosingPriceList list) {
        return list.findLowestVolume();
    }

    public String asHTMLDocument( ClosingPriceList priceList) {
        StringBuilder document = new StringBuilder("<html>");
        document = document.append( "<header><TITLE>JavaONE Lab/Stock History</TITLE></header>");
        document = document.append( "<body><H1>Stock History</H1>");
        document = document.append( constructForm());
        document = document.append( constructResultsTable(priceList));
        return document.append( "</body></html>").toString();
    }

    private String constructForm() {
        StringBuilder form = new StringBuilder("<form action=\"http://localhost:8080/lab/stock\" method=\"post\">");
        form = form.append( this.constructDropdownList());
        form = form.append( "<input type=\"submit\" name=\"history\" value=\"history\">");
        form = form.append( "<input type=\"submit\" name=\"lastClose\" value=\"last close\">");
        return form.append("</form>").toString();
    }

    private String constructDropdownList() {

        Iterator tickerSymbols = new TickerList().iterator();
        StringBuilder options = new StringBuilder("<select name=\"ticker\">");
        while ( tickerSymbols.hasNext()) {
            String tickerSymbol = (String)tickerSymbols.next();
            options = options.append( "<option value=\"");
            options = options.append(tickerSymbol);
            options = options.append( "\">");
            options = options.append( tickerSymbol);
            options = options.append( "</option>");
        }
        return options.append("</select>").toString();
    }

    public String constructResultsTable( ClosingPriceList priceList) {
        if ( priceList == null) return "";
        StringBuilder table = new StringBuilder( "<H2>" + priceList.getTicker() + "</H2>");
        table.append("<br><table border=\"1\">");
        table.append("<TR><TH>Date</TH><TH>Open</TH><TH>High</TH><TH>Low</TH><TH>Close</TH><TH>Volume</TH><TH>Adjusted Close</TH></TR>");
        Iterator prices = priceList.iterator();
        while ( prices.hasNext()) {
            ClosingPrice price = (ClosingPrice)prices.next();
            table.append("<tr><td>");
            table.append(price.getDate());
            table.append("</td><td>");
            table.append(price.getOpen());
            table.append("</td><td>");
            table.append(price.getHigh());
            table.append("</td><td>");
            table.append(price.getLow());
            table.append("</td><td>");
            table.append(price.getClose());
            table.append("</td><td>");
            table.append(price.getVolume());
            table.append("</td><td>");
            table.append(price.getAdjustedClose());
            table.append("</td></tr>");
        }
        table.append("</table>");
        return table.toString();
    }


}
