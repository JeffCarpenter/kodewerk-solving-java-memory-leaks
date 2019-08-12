package com.kodewerk.tipsdb;

public class DBMain {
    public static void main(String[] args) throws Exception {
        TipsDBProperties.initialize();
        if (args.length != 1)
            System.out.println("USAGE: java ... com.kodewerk.tipsdb.DBMain STARTUP|SHUTDOWN|TEST");
        else if ("STARTUP".equals(args[0])) {
            int port = TipsDBProperties.getDBPort();
            String dbname = TipsDBProperties.getDBName();
            String[] argz = {"-port", Integer.toString(port), "-database", dbname};
            org.hsqldb.Server.main(argz);
        } else if ("SHUTDOWN".equals(args[0])) {
            System.out.println("Shutting down the database server");
            CreateDB inst = new CreateDB();
            System.out.println("Note we expect an exception now as the server abruptly closes the client connection when it shuts down");
            inst.failed(inst.shutdownDatabaseServer());
            inst.failed(inst.shutdown());
        } else if ("TEST".equals(args[0])) {
            System.out.println("Testing if DB is up and loaded with TIPS table");
            CreateDB inst = new CreateDB();
            inst.failed(inst.testQueryDB());
            inst.failed(inst.shutdown());
            System.out.println("And test JPT drivers too");
            inst = new CreateDB(false);
            inst.failed(inst.testQueryDB());
            inst.failed(inst.shutdown());
        } else if ("CREATEDB".equals(args[0])) {
            CreateDB.main(args);
        } else
            System.out.println("USAGE: java ... com.kodewerk.tipsdb.DBMain STARTUP|SHUTDOWN");
    }
}
