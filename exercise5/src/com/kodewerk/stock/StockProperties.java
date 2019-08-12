package com.kodewerk.stock;

import java.util.Properties;
import java.util.Map;
import java.util.Iterator;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * StockProperties
 * <p/>
 * Description goes here
 *
 * @author kirk
 * @version 1.0
 * Copyright Kodewerk Ltd. All rights reserved.
 * 
 */
public class StockProperties {

    static boolean Initialized = false;

    public static void initialize()
            throws IllegalStateException {
        if (!Initialized) {
            String propertiesFile = System.getProperty("com.kodewerk.stocks.properties");
            if (propertiesFile == null || "".equals(propertiesFile))
                throw new IllegalStateException(
                        "FATAL ERROR: properties file must be specified with 'com.kodewerk.stocks.properties' " +
                        "property, e.g. java -Dcom.kodewerk.stocks.properties=properties.txt ...");
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

    public static int getDBPort() {
        initialize();
        String port = System.getProperty("com.kodewerk.stocks.port");
        if ((port == null) || "".equals(port))
            throw new RuntimeException("Expected com.kodewerk.stocks.port property to define a port");
        int portnum = Integer.parseInt(port);
        return portnum;
    }

    public static String getDataSource() {
        return getString( "com.kodewerk.stocks.datasource");
    }

    public static boolean getCacheEnabled() {
        return Boolean.parseBoolean( getString( "com.kodewerk.stocks.enablecache"));
    }

    public static String getFileSystem() {
        return getString( "com.kodewerk.stocks.filesystem");
    }

    public static String getURL() {
        return getString("com.kodewerk.stocks.url");
    }

    public static String getDriver() {
        return getString("com.kodewerk.stocks.driver");
    }

    public static String getDBName() {
        return getString("com.kodewerk.stocks.name");
    }

    public static String getUser() {
        return getString("com.kodewerk.stocks.user");
    }

    public static String getPassword() {
        return getPossiblyEmptyString("com.kodewerk.stocks.password");
    }

    /**
     * Utility methods
     * @param property
     * @return
     */
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

}
