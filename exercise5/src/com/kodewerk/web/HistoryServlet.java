package com.kodewerk.web;

import javax.servlet.http.HttpServlet;

/**
 * Created by IntelliJ IDEA.
 * User: kirk
 * Date: Dec 5, 2008
 * Time: 3:50:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class HistoryServlet extends HttpServlet {

    public String getHeader() {
        return "<html><header><link rel=\"stylesheet\" href=\"/lab/css/plain.css\" type=\"text/css\" media=\"all\"/><TITLE>JPTWML - Stock History</TITLE></header>";
    }
}
