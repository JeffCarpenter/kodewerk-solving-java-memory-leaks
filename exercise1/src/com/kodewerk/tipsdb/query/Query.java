package com.kodewerk.tipsdb.query;

import com.kodewerk.tipsdb.TipsDBProperties;
import com.kodewerk.util.CacheFactory;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.sql.SQLException;

public class Query {
	
    private final static String LOG_NAME = Query.class.getName();
    private final static QueryMonitor monitor = new QueryMonitor();
    private static Logger log;
	
    static {
        log = Logger.getLogger(Logger.class.getName());
    }
		

    //Can ask for keywords; or tips; or tip based on keywords
    protected DataAccess connect() throws Exception {
        long start = System.currentTimeMillis();
        try {
            String driver = TipsDBProperties.getDriver();
            String url = TipsDBProperties.getUrl();
            String user = TipsDBProperties.getUser();
            String password = TipsDBProperties.getPassword();
            return DataAccess.newDataAccess(driver, url, user, password);
        } finally {
            monitor.addConnectionTime( System.currentTimeMillis() - start);
        }
    }

    public Result getAllKeywords() throws Exception {
        //First get the number of keywords, then read them all.
        //into a list and return that!
        DataAccess access = connect();
        long start = System.currentTimeMillis();
        try { 
            String countQuery = "SELECT count(KEYWORD) FROM TIPS";
            OneValueResultHandler one = new OneValueResultHandler();
            if (!access.simpleQuery(countQuery, one)) {
                log.log( Level.WARNING, access.lastFailureString(), access.lastException());
            }
            int size = Integer.parseInt(one.getValueAsString());

            String query = "SELECT KEYWORD FROM TIPS";
            SingleArrayResultHandler arr = new SingleArrayResultHandler(size);
            if (!access.simpleQuery(query, arr)) {
                log.log( Level.WARNING, access.lastFailureString(), access.lastException());
            }
            return new Result("", arr.getValuesAsStringArray());
        } finally {
            monitor.addQueryTime( System.currentTimeMillis() - start);
        }
    }

    public Result getAllTips() throws SQLException {
        return null;
    }

    public Result getAllTipsForKeyword(String keyword) throws Exception {
        //First get the number of tips, then read them all
        //into a list and return that!
        DataAccess access = connect();
        long start = System.currentTimeMillis();        
        try {
            String countQuery = "SELECT count(TIP) FROM TIPS WHERE KEYWORD='" + keyword + "'";
            OneValueResultHandler one = new OneValueResultHandler();
            if (!access.simpleQuery(countQuery, one)) {
                log.log( Level.WARNING, access.lastFailureString(), access.lastException());
            }
            int size = Integer.parseInt(one.getValueAsString());

            String query = "SELECT TIP FROM TIPS WHERE KEYWORD='" + keyword + "'";
            SingleArrayResultHandler results = (SingleArrayResultHandler) CacheFactory.getCache().get( keyword);
            if ( results == null) {
                results = new SingleArrayResultHandler(size);
                if (!access.simpleQuery(query, results)) {
                    System.out.println(access.lastFailureString());
                    access.lastException().printStackTrace();
                }
                CacheFactory.getCache().put( keyword, results);
            }
            return new Result(keyword, results.getValuesAsStringArray());
        } finally {
            monitor.addQueryTime( System.currentTimeMillis() - start);
            start = System.currentTimeMillis();
            try {
                access.shutdown();
            } finally {
                monitor.addCloseTime( System.currentTimeMillis() - start);
            }
        }
    }
}
