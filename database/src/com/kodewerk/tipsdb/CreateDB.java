package com.kodewerk.tipsdb;



import com.kodewerk.tipsdb.query.ResultHandler;
import com.kodewerk.tipsdb.query.DataAccess;
import com.kodewerk.tipsdb.domain.TipDocument;
import com.kodewerk.tipsdb.domain.TipsDocuments;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CreateDB implements ResultHandler {

    public static void main(String[] args) throws Exception {
        TipsDocuments documents = new TipsDocuments();
        documents.getTipsDocuments();
        System.out.println( "Creating DB");
        CreateDB inst = new CreateDB();
        inst.createDB();
        inst.failed(inst.shutdown());

        System.out.println("Testing with facade");
        //And also test a query with our driver facade
        inst = new CreateDB(false);
        inst.failed(inst.testQueryDB());
        inst.failed(inst.shutdown());
    }

    public boolean shutdown() {
        return access.shutdown();
    }

    DataAccess access;

    public CreateDB() throws Exception {
        this(true);
    }

    public CreateDB(boolean realDB) throws Exception {
        String driver = TipsDBProperties.getRealDriver();
        String url = TipsDBProperties.getRealUrl();
        if (!realDB) {
            driver = TipsDBProperties.getDriverFacade();
            url = TipsDBProperties.getUrl();
        }
        String user = TipsDBProperties.getUser();
        String password = TipsDBProperties.getPassword();

        System.out.println("Connecting to database");
        access = DataAccess.newDataAccess(driver, url, user, password);
    }

    public void createDB() throws Exception {
        System.out.println("Creating TIPS table");
        if (failed(access.createTipsTable()))
            return;

        System.out.println("Updating TIPS table");
        if (failed(access.fillTipsTableWithData()))
            return;

        System.out.println("Querying TIPS table");
        if (failed(testQueryDB()))
            return;

        System.out.println("Querying TIPS table");
        if (failed(testQueryDB()))
            return;

        System.out.println("Creating index on KEYWORD for TIPS table");
        if (failed(access.createKeywordIndex()))
            return;

        System.out.println("Querying TIPS table");
        if (failed(testQueryDB()))
            return;

    }

    public boolean failed(boolean b) {
        if (b)
            return false;

        System.out.println(access.lastFailureString());
        access.lastException().printStackTrace();
        return true;
    }

    public boolean testQueryDB() {
        String sqlQueryTable = "SELECT tip FROM tips WHERE KEYWORD='STACKTRACEELEMENT'";
        long time = System.currentTimeMillis();
        if (failed(access.simpleQuery(sqlQueryTable, this)))
            return false;
        System.out.println("Time to query: " + (System.currentTimeMillis() - time));

        if (count == 0) {
            System.out.println("db error: query test against TIPS table failed, 0 tips returned for query " + sqlQueryTable);
            return false;
        }
        count = -1;
        return true;
    }

    public boolean shutdownDatabaseServer() {
        String sqlQueryTable = "SHUTDOWN";
        if (failed(access.simpleQuery(sqlQueryTable, this)))
            return false;

        count = -1;
        return true;
    }

    int count = -1;

    public void result(ResultSet rs)
            throws SQLException {
        count = 0;
        while (rs.next()) {
            count++;
        }
    }

}
