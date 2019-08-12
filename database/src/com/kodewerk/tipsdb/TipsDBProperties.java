package com.kodewerk.tipsdb;

import java.util.Map;
import java.util.Properties;
import java.util.Iterator;
import java.io.FileInputStream;
import java.io.IOException;

public class TipsDBProperties {

    static boolean Initialized = false;

    public static void initialize()
            throws IllegalStateException {
        if (!Initialized) {
            String propertiesFile = System.getProperty("com.kodewerk.tipsdb.properties");
            if (propertiesFile == null || "".equals(propertiesFile))
                throw new IllegalStateException(
                        "FATAL ERROR: properties file must be specified with 'com.kodewerk.tipsdb.properties' " +
                        "property, e.g. java -Dcom.kodewerk.tipsdb.properties=properties.txt ...");
            try {
                initializeWith(propertiesFile);
            } catch (IOException e) {
                throw new IllegalStateException(
                        "FATAL ERROR: Unable to initialize properties from " + propertiesFile);
            }
        }
    }

    public static void initializeWith(String propertiesfile)
            throws IOException {
        Properties p = new Properties();
        FileInputStream in = new FileInputStream(propertiesfile);
        p.load(new FileInputStream(propertiesfile));
        in.close();
        Iterator iterator = p.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry e = (Map.Entry) iterator.next();
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (System.getProperty(key) == null || "".equals(System.getProperty(key)))
                System.setProperty(key, value);
        }
        Initialized = true;
    }

    public static void pause(long delay) {
        long starttime = System.currentTimeMillis();
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
        }
        long endtime = System.currentTimeMillis();
        while (endtime - starttime < delay) {
            try {
                Thread.sleep(delay - (endtime - starttime));
            } catch (InterruptedException e) {
            }
            endtime = System.currentTimeMillis();
        }
    }

    public static String getTipsDocumentFile() {
        return getString("com.kodewerk.tipsdb.tipsfile");
    }

    public static String getRealDriver() {
        return getString("com.kodewerk.tipsdb.realdriver");
    }

    public static String getDriverFacade() {
        return getString("com.kodewerk.tipsdb.driver");
    }

    public static String getPassword() {
        return getPossiblyEmptyString("com.kodewerk.tipsdb.password");
    }

    public static String getUser() {
        return getString("com.kodewerk.tipsdb.user");
    }

    public static String getRealUrl() {
        return getString("com.kodewerk.tipsdb.realurl");
    }

    public static String getUrl() {
        return getString("com.kodewerk.tipsdb.url");
    }

    public static String getDBName() {
        return getString("com.kodewerk.tipsdb.dbname");
    }

    public static String getAppserverConfigFile() {
        return getString("com.kodewerk.tipsdb.appserver.config");
    }


    public static String get() {
        return getString("");
    }


    public static String getKeywordMaxLength() {
        return getString("JPTTipsKeywordMaxLength");
    }

    protected static String getString(String property) {
        initialize();
        String s = System.getProperty(property);
        if ((property == null) || "".equals(property))
            throw new RuntimeException("Expected " + property + " property to define a string");
        return s;
    }

    protected static String getPossiblyEmptyString(String property) {
        initialize();
        String s = System.getProperty(property);
        if (property == null)
            throw new RuntimeException("Expected " + property + " property to define a string");
        return s;
    }

    public static int getDBPort() {
        initialize();
        String port = System.getProperty("com.kodewerk.tipsdb.port");
        if ((port == null) || "".equals(port))
            throw new RuntimeException("Expected com.kodewerk.tipsdb.port property to define a port");
        int portnum = Integer.parseInt(port);
        return portnum;
    }

    public static long getReadDelay() {
        return getDelay("ReadDelay");
    }

    public static long getWriteDelay() {
        return getDelay("WriteDelay");
    }

    public static long getConnectionDelay() {
        return getDelay("ConnectionDelay");
    }

    protected static long getDelay(String property) {
        initialize();
        String delay = System.getProperty(property);
        if ((delay == null) || "".equals(delay))
            throw new RuntimeException("Expected " + property + " property to define a millisecond time");
        long ldelay = Long.parseLong(delay);
        return ldelay;
    }


}
