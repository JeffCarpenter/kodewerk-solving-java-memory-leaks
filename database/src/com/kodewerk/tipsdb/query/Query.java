package com.kodewerk.tipsdb.query;

import com.kodewerk.tipsdb.TipsDBProperties;
import com.kodewerk.util.CacheFactory;

import java.sql.SQLException;

public class Query {


    //Can ask for keywords; or tips; or tip based on keywords
    protected DataAccess connect() throws SQLException {
        String driver = TipsDBProperties.getDriverFacade();
        String url = TipsDBProperties.getUrl();
        String user = TipsDBProperties.getUser();
        String password = TipsDBProperties.getPassword();

        return DataAccess.newDataAccess(driver, url, user, password);
    }

    public Result getAllKeywords() throws SQLException {
        //First get the number of keywords, then read them all.
        //into a list and return that!
        DataAccess access = connect();
        String countQuery = "SELECT count(KEYWORD) FROM TIPS";
        OneValueResultHandler one = new OneValueResultHandler();
        if (!access.simpleQuery(countQuery, one)) {
            System.out.println(access.lastFailureString());
            access.lastException().printStackTrace();
        }
        int size = Integer.parseInt(one.getValueAsString());

        String query = "SELECT KEYWORD FROM TIPS";
        SingleArrayResultHandler arr = new SingleArrayResultHandler(size);
        if (!access.simpleQuery(query, arr)) {
            System.out.println(access.lastFailureString());
            access.lastException().printStackTrace();
        }
        return new Result("", arr.getValuesAsStringArray());
    }

    public Result getAllTips() throws SQLException {
        return null;
    }

    public Result getAllTipsForKeyword(String keyword) throws SQLException {
        //First get the number of tips, then read them all
        //into a list and return that!
        DataAccess access = connect();
        String countQuery = "SELECT count(TIP) FROM TIPS WHERE KEYWORD='" + keyword + "'";
        OneValueResultHandler one = new OneValueResultHandler();
        if (!access.simpleQuery(countQuery, one)) {
            System.out.println(access.lastFailureString());
            access.lastException().printStackTrace();
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
        access.shutdown();
        return new Result(keyword, results.getValuesAsStringArray());
    }

}
