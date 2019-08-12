package com.kodewerk.db;

import com.kodewerk.stock.*;

import java.sql.*;
import java.util.Iterator;

/**
 * DataBase
 *
 * @author kirk
 * @version 1.0
 *
 * Copyright Kodewerk Ltd. All rights reserved.
 */
public class DataBase {

    private final static String[] TICKERS = { "hpq","ibm","java","msft","novl","orcl" };

    volatile private Connection connection =  null;

    public DataBase() {}

    public void connect(String driver, String url, String user, String password) throws DataBaseException {
        if ( this.connection == null) {
            synchronized( this) {
                try {
                    Class.forName(driver);
                    connection = DriverManager.getConnection(url, user, password);
                } catch (SQLException ex1) {
                    throw new DataBaseException("db error: couldn't connect to database (is it running?). See next trace:", ex1);
                } catch (ClassNotFoundException ex2) {
                      throw new DataBaseException( "db error: couldn't load driver " + driver, ex2);
                }
            }
        }
    }

    public void close() throws DataBaseException {
        try {
            this.connection.close();
        } catch (SQLException e) {
            throw new DataBaseException( e);
        } finally {
            this.connection = null;
        }
    }

    public ResultSet getClosingPrices( TickerSymbol ticker) throws DataBaseException {
        String query = "SELECT * FROM stocks where ticker="+ticker;
        return simpleQuery( query);
    }

    public synchronized ResultSet simpleQuery(String sqlQuery ) throws DataBaseException {
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sqlQuery);
            st.close();
            return rs;
        } catch (SQLException e) {
            throw new DataBaseException("db error: query test against STOCKS table failed. See next trace:",e);
        }
    }

    public synchronized void create() throws DataBaseException {
        String createStatement = "CREATE TABLE stocks (\n" +
                "    id INTEGER IDENTITY,\n" +
                "    ticker VARCHAR(4) NOT NULL,\n" +
                "    date VARCHAR(10) NOT NULL,\n" +
                "    open FLOAT NOT NULL,\n" +
                "    high FLOAT NOT NULL,\n" +
                "    low FLOAT NOT NULL,\n" +
                "    close FLOAT NOT NULL,\n" +
                "    volume INTEGER NOT NULL,\n" +
                "    adjClose FLOAT NOT NULL\n" +
                ");";
       this.simpleQuery( createStatement);

    }

    public synchronized void load() throws DataBaseException {
        try {
            String statement = "INSERT INTO stocks(ticker,date,open,high,low,close,volume,adjClose) VALUES(?,?,?,?,?,?,?,?)";
            PreparedStatement ps = connection.prepareStatement( statement);
            for ( String source : TICKERS) {
                System.out.println("\nLoading: " + source);
                ClosingPriceList list = new ClosingPriceRDBDataSource().load( new TickerSymbol(source));
                Iterator iter = list.iterator();
                while ( iter.hasNext()) {
                    System.out.print(".");
                    ClosingPrice price = (ClosingPrice)iter.next();
                    ps.setString( 1, source);
                    ps.setString( 2, price.getDate());
                    ps.setString( 3, price.getOpen());
                    ps.setString( 4, price.getHigh());
                    ps.setString( 5, price.getLow());
                    ps.setString( 6, price.getClose());
                    ps.setString( 7, price.getVolume());
                    ps.setString( 8, price.getAdjustedClose());
                    if ( ps.executeUpdate() != 1) {
                        throw new DataBaseException( "Unable to update DB");
                    }
                }
            }
        } catch (SQLException e) {
            throw new DataBaseException( e);
        } catch (ClosingPriceDataSourceException cpdse) {
            throw new DataBaseException( cpdse);
        }

    }

    public void test() throws DataBaseException {
        try {
            String statement = "SELECT count(*) FROM stocks";
            ResultSet rs = this.simpleQuery( statement);
            rs.next();
            System.out.println( rs.getInt( 1));          
        } catch (SQLException e) {
            throw new DataBaseException( e);
        }
    }

    // Domain specific queries
    public ClosingPrice getLatestClosingPrice( String ticker) throws DataBaseException {
        try {
            String st = "SELECT * FROM stocks where ticker='" + ticker + "'";
            ResultSet rs = this.simpleQuery( st);
            ClosingPrice newestPrice = null;
            while ( rs.next()) {
                newestPrice = this.compareCurrent( rs, newestPrice);
            }
            return newestPrice;
        } catch (SQLException e) {
            throw new DataBaseException( e);
        }
    }

    private ClosingPrice compareCurrent( ResultSet rs, ClosingPrice current) throws SQLException {
        String ticker = rs.getString( 2);
        String date = rs.getString( 3);
        String open = Double.toString(rs.getDouble(4));
        String high = Double.toString(rs.getDouble(5));
        String low = Double.toString(rs.getDouble(6));
        String close = Double.toString(rs.getDouble(7));
        String volume = Double.toString(rs.getInt(8));
        String adjustedClose = Double.toString(rs.getDouble(9));
        ClosingPrice newest = new ClosingPrice(date, open, high, low, close, volume, adjustedClose);
        if (( current == null) || (newest.newerThan( current)))
            return newest;
        else return current;
        
    }

    public static void main(String[] args) throws Exception {
        StockProperties.initialize();
        DataBase db = new DataBase();
        db.connect( StockProperties.getDriver(), StockProperties.getURL(), StockProperties.getUser(), StockProperties.getPassword());
        db.create();
        db.load();
        db.test();
        ClosingPrice price = db.getLatestClosingPrice("java");
        System.out.println(price);
        db.close();
    }
}
