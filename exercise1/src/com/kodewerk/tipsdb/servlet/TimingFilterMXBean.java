package com.kodewerk.tipsdb.servlet;

public interface TimingFilterMXBean {

    public double getAverageResponseTime();
    public long getNumberOfCalls();
    public long getCurrentResponseTime();
    public long getLongestResponseTime();
    public double getThroughput();

    public void reset();

}
