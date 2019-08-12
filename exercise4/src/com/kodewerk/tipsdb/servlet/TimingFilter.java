package com.kodewerk.tipsdb.servlet;

import javax.management.ObjectName;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TimingFilter implements Filter, TimingFilterMXBean {

    private FilterConfig config;
	private String name;

    private AtomicLong totalResponseTime = new AtomicLong(0L);
    private AtomicLong numberOfCalls = new AtomicLong(0L);
    private volatile long currentResponseTime = 0L;
    private volatile long longestResponseTime = 0L;
    private volatile long timeOfFirstEvent = 0L;

    /*
        Wire up the MXBean in this method
        Properties are defined in the web.xml file
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
		name = this.config.getInitParameter("name");
         try {
             ObjectName objectName = new ObjectName("com.kodewerk:type=servlet,name="+name);
             ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
         } catch (Exception ex) {
             Logger.getLogger(TimingFilter.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
         }
    }


    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        long startTime = System.nanoTime();
        if ( timeOfFirstEvent == 0L)
            timeOfFirstEvent = startTime;

        if ( this.config != null) {
            try {
                chain.doFilter( request, response);
            } finally {
                record(System.nanoTime() - startTime);
            }
        }
    }

    public void destroy() {
        this.config = null;
    }

    public void record( long responseTime) {
        numberOfCalls.incrementAndGet();
        currentResponseTime = responseTime / 1_000_000;
        totalResponseTime.addAndGet( currentResponseTime);
        if ( currentResponseTime > longestResponseTime)
            longestResponseTime = currentResponseTime;
    }

    public double getAverageResponseTime() {
        if ( numberOfCalls.longValue() == 0L) return 0.0d;
        return totalResponseTime.doubleValue() / numberOfCalls.doubleValue();
    }

    public double getThroughput() {
        if ( numberOfCalls.longValue() == 0L) return 0.0d;
        return numberOfCalls.doubleValue() / ( (double)(System.nanoTime() - timeOfFirstEvent) / 1_000_000_000);
    }

    public long getNumberOfCalls() {
        return numberOfCalls.longValue();
    }

    public long getCurrentResponseTime() { return currentResponseTime; }

    public long getLongestResponseTime() { return longestResponseTime; }

    public void reset() {
        timeOfFirstEvent = 0L;
        totalResponseTime = new AtomicLong(0L);
        numberOfCalls = new AtomicLong(0L);
        currentResponseTime = 0L;
        longestResponseTime = 0L;
    }

}
