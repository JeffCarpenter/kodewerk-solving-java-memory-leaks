package com.kodewerk.tipsdb.query;

import com.kodewerk.tipsdb.domain.Keyword;
import com.kodewerk.tipsdb.domain.Tip;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class DataAccess {

    Connection dbconnection;
    SQLException lastException;
    String lastFailureString;

    public String lastFailureString() {
        return lastFailureString;
    }

    public SQLException lastException() {
        return lastException;
    }

    public static DataAccess newDataAccess(String driver, String url, String user, String password)
            throws SQLException {
        DataAccess access = new DataAccess();
        if (!access.connect(driver, url, user, password))
            throw access.lastException;
        return access;
    }

    public void reset() {
        lastException = null;
        lastFailureString = null;
    }

    public boolean connect(String driver, String url, String user, String password) {
        reset();
        try {
            // Load the HSQL Database Engine JDBC driver
            Class.forName(driver);

            // connect to the database.
            dbconnection = DriverManager.getConnection(url, user, password);
            return true;
        } catch (SQLException ex1) {
            lastFailureString = "db error: couldn't connect to database (is it running?). See next trace:";
            lastException = ex1;
            return false;
        } catch (ClassNotFoundException ex2) {
            lastFailureString = "db error: couldn't load driver " + driver;
            lastException = new SQLException(lastFailureString);
            return false;
        }
    }

    public boolean createTipsTable() {
        reset();
        Connection conn = dbconnection;
        try {
            // Make an empty table called TIPS with three columns, ID, KEYWORD, TIP.
            // By declaring the id column IDENTITY, the db will automatically
            // generate unique values for new rows.
            Statement st = conn.createStatement();
            String sqlCreateTable =
                    "CREATE TABLE tips ( id INTEGER IDENTITY, keyword VARCHAR(32), tip VARCHAR(1024) )";
            st.executeQuery(sqlCreateTable); //ignore result set
            st.close();
            return true;
        } catch (SQLException ex1) {
            // If run this more than once, should throw execption since table
            // already there, and don't want to insert data twice
            ex1.printStackTrace();
            lastFailureString = "db error: possibly table TIPS already exists? See next trace:";
            lastException = ex1;
            return false;
        }
    }

    public boolean fillTipsTableWithData() {
        reset();
        Connection conn = dbconnection;
        try {
            // Add rows. This would create duplicates if run more then once
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO tips(keyword,tip) VALUES(?,?)");
            for (int i = 0; i < Keyword.AllKeywordsList.size(); i++) {
                String keyword = (String) Keyword.AllKeywordsList.get(i);
                ArrayList tipList = (ArrayList) Keyword.AllKeywords.get(keyword);
                for (int j = 0; j < tipList.size(); j++) {
                    String tip = ((Tip) tipList.get(j)).getTip();
                    ps.setString(1, keyword);
                    ps.setString(2, tip);
                    if (ps.executeUpdate() != 1) {
                        lastFailureString = "db error : (terminating updates) failed to insert row for keyword " + keyword;
                        lastException = new SQLException("Prepared statement 'INSERT INTO tips(keyword,tip) VALUES(?,?)' failed");
                        ps.close();
                        return false;
                    }
                }
            }
            return true;
        } catch (SQLException ex1) {
            lastFailureString = "db error: unable to complete update of TIPS table. See next trace:";
            lastException = ex1;
            return false;
        }
    }

    public boolean simpleQuery(String sqlQuery, ResultHandler handler) {
        reset();
        Connection conn = dbconnection;
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sqlQuery);
            handler.result(rs);
            st.close(); // if you close a statement the associated ResultSet is closed too
            return true;
        } catch (SQLException ex1) {
            // If run this more than once, should throw execption since table
            // already there, and don't want to insert data twice
            lastFailureString = "db error: query test against TIPS table failed. See next trace:";
            lastException = ex1;
            return false;
        }
    }

    public boolean createKeywordIndex() {
        reset();
        Connection conn = dbconnection;
        try {
            // Make an empty table called TIPS with three columns, ID, KEYWORD, TIP.
            // By declaring the id column IDENTITY, the db will automatically
            // generate unique values for new rows.
            Statement st = conn.createStatement();
            String sqlIndexTable =
                    "CREATE INDEX KeywordIndex ON TIPS(keyword)";
            st.executeQuery(sqlIndexTable); //ignore result set
            st.close();
            return true;
        } catch (SQLException ex1) {
            // If run this more than once, should throw execption since table
            // already there, and don't want to insert data twice
            lastFailureString = "db error: possibly index KEYWORD in TIPS already exists? See next trace:";
            lastException = ex1;
            return false;
        }
    }

    public boolean shutdown() {
        reset();
        Connection conn = dbconnection;
        try {
            conn.close();
            return true;
        } catch (SQLException ex1) {
            lastFailureString = "db error: couldn't shut connection to database. See next trace:";
            lastException = ex1;
            return false;
        }
    }

}
