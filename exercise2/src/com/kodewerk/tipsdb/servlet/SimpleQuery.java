package com.kodewerk.tipsdb.servlet;

import com.kodewerk.tipsdb.query.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class SimpleQuery extends HttpServlet {

    abstract String queryType();

    abstract void doValidQuery(String parameterkeywords, PrintWriter out,
                               HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

    public String defaultInfo() {
        String defaultInfo = "Only alphanumeric characters are relevant for the query.<BR>";
        return defaultInfo;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        if (!request.getRequestURI().startsWith("/tips/" + queryType().toLowerCase())) {
            //Some invalid request
            Result res = new Result("", new String[0], defaultInfo());
            res.setQueryType(queryType().toUpperCase());
            String doc = res.asHTMLDocument();
            out.println(doc);
        } else {
            String param = request.getQueryString();
            if ((param == null) || ! param.toLowerCase().startsWith(queryType().toLowerCase())) {
                //Another invalid request
                Result res = new Result("", new String[0], defaultInfo());
                res.setQueryType(queryType().toUpperCase());
                String doc = res.asHTMLDocument();
                out.println(doc);
            } else {
                //A valid query
                doValidQuery(param.substring(queryType().length() + 1), out, request, response);
            }
        }
        this.release();
    }


    /**
     * We are going to perform the same operations for POST requests
     * as for GET methods, so this method just sends the request to
     * the doGet method.
     */

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doGet(request, response);
    }
	
    public void release() {}
}
