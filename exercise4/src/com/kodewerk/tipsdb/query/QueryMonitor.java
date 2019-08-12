package com.kodewerk.tipsdb.query;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.*;

public class QueryMonitor implements QueryMonitorMXBean {
    
    private AtomicLong totalConnectionTime = new AtomicLong(0L);
    private AtomicInteger connectionCount = new AtomicInteger(0);
    private AtomicLong totalQueryTime = new AtomicLong(0L);
    private AtomicInteger queryCount = new AtomicInteger(0);
    private AtomicLong totalCloseTime = new AtomicLong(0L);
    private AtomicInteger closeCount = new AtomicInteger(0);
    
    public QueryMonitor() {
        ObjectName objectName;
        try {
            objectName = new ObjectName("com.kodewerk:type=query,name=keyword");
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
        } catch (Exception ex) {
            Logger.getLogger(QueryMonitor.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
    
    public void addConnectionTime( long connectionTime) {
        totalConnectionTime.addAndGet( connectionTime);
        connectionCount.incrementAndGet();
    }
    
    public void addQueryTime( long queryTime) {
        totalQueryTime.addAndGet( queryTime);
        queryCount.incrementAndGet();
    }
    
    public void addCloseTime( long closeTime) {
        totalCloseTime.addAndGet( closeTime);
        closeCount.incrementAndGet();
    }
    
    //JMX Attributes
    
    public double getAverageConnectionTime() {
        int localConnectionCount = connectionCount.get();
        long localConnectionTime = totalConnectionTime.get();
        if ( localConnectionCount == 0) return 0;
        return (double)localConnectionTime / (double)localConnectionCount;
    }
    
    public double getAverageQueryTime() {
        int localQueryCount = queryCount.get();
        long localQueryTime = totalQueryTime.get();
        if ( localQueryCount == 0) return 0;
        return (double)localQueryTime / (double)localQueryCount;
    } 
    
    public double getAverageCloseTime() {
        int localCloseCount = queryCount.get();
        long localCloseTime = totalCloseTime.get();
        if ( localCloseCount == 0) return 0;
        return (double)localCloseTime / (double)localCloseCount;
    }
    
    public int getConnectionCount() {
        return connectionCount.get();
    }
    
    public int getQueryCount() {
        return queryCount.get();
    }
    
    public int getCloseCount() {
        return closeCount.get();
    }
    
    // JMX operations
    
    public void clear() {
        totalConnectionTime = new AtomicLong(0L);
        connectionCount = new AtomicInteger(0);
        totalQueryTime = new AtomicLong(0L);
        queryCount = new AtomicInteger(0);
        totalCloseTime = new AtomicLong(0L);
        closeCount = new AtomicInteger(0);    
    }
}
