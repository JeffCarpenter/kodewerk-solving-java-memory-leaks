package com.kodewerk.web;

import com.kodewerk.db.ClosingPriceDataSourceException;
import com.kodewerk.db.ClosingPriceDataSource;
import com.kodewerk.stock.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Iterator;

/**
 * StockHistory
 *
 * @author kirk
 * @version 1.0
 * @since 11:25:30 AM
 */
public class DBHistory extends HistoryServlet {

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
        ClosingPriceList priceList = null;
        response.setContentType("text/html");
        TickerSymbol ticker = new TickerSymbol(request.getParameter("ticker"));
        if ( ticker != null)
            try {
                priceList = ds.load( ticker);
            } catch (ClosingPriceDataSourceException e) {
                priceList = null;

            }
        response.getOutputStream().print( this.asHTMLDocument( priceList));
    }


    /**
     * We are going to perform the same operations for POST requests
     * as for GET methods, so this method just sends the request to
     * the doGet method.
     */

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    public String asHTMLDocument( ClosingPriceList priceList) {
        StringBuffer document = new StringBuffer("<html>");
        document = document.append( "<header><TITLE>JavaONE Lab/Stock History</TITLE></header>");
        document = document.append( "<body><H1>Stock History</H1>");
        document = document.append( constructForm());
        document = document.append( constructResultsTable(priceList));
        return document.append( "</body></html>").toString();
    }

    private String constructForm() {
        StringBuffer form = new StringBuffer("<form action=\"http://localhost:8080/lab/stock\" method=\"post\">");
        form = form.append( this.constructDropdownList());
        form = form.append( "<input type=\"submit\" value=\"fetch\">");
        return form.append("</form>").toString();
    }

    private String constructDropdownList() {

        Iterator tickerSymbols = new TickerList().iterator();
        StringBuffer options = new StringBuffer("<select name=\"ticker\">");
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
        String table = "<H2>" + priceList.getTicker() + "</H2>";
        table += "<br><table border=\"1\">";
        table += "<TR><TH>Date</TH><TH>Open</TH><TH>High</TH><TH>Low</TH><TH>Close</TH><TH>Volume</TH><TH>Adjusted Close</TH></TR>";
        Iterator prices = priceList.iterator();
        while ( prices.hasNext()) {
            ClosingPrice price = (ClosingPrice)prices.next();
            table += "<tr><td>";
            table += price.getDate();
            table += "</td><td>";
            table += price.getOpen();
            table += "</td><td>";
            table += price.getHigh();
            table += "</td><td>";
            table += price.getLow();
            table += "</td><td>";
            table += price.getClose();
            table += "</td><td>";
            table += price.getVolume();
            table += "</td><td>";
            table += price.getAdjustedClose();
            table += "</td></tr>";
        }
        table += "</table>";
        return table;
    }
}
