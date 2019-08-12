package com.kodewerk.tipsdb.query;

public interface QueryMonitorMXBean {
    
    //JMX Attributes    
    public double getAverageConnectionTime();    
    public double getAverageQueryTime();     
    public double getAverageCloseTime();
    
    public int getConnectionCount();    
    public int getQueryCount();    
    public int getCloseCount();
    
    // JMX operations
    public void clear();
}
