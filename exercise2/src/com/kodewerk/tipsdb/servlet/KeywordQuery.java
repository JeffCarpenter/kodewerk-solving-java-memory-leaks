package com.kodewerk.tipsdb.servlet;

import com.kodewerk.tipsdb.domain.Keyword;
import com.kodewerk.tipsdb.query.Result;
import com.kodewerk.tipsdb.query.Query;
import com.kodewerk.util.CacheFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;

public class KeywordQuery extends SimpleQuery {

    public String queryType() {
        return "keyword";
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        String policy = servletConfig.getInitParameter("cache");
        if ( policy != null)
            try {
                CacheFactory.setCache( policy);
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }

    public void doValidQuery(String parameterkeywords, PrintWriter out,
                             HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String keywords = parameterkeywords.replaceAll("\\%..", " "); //get rid of all HTML mappings
        keywords = keywords.replaceAll("\\W+", " "); //get rid of all non-word chars
        String[] keywordArray = keywords.split(" ");
        ArrayList relevantKeywords = new ArrayList();
        ArrayList nonPriorityKeywords = new ArrayList();
        String defaultInfo = defaultInfo();
        for (int i = 0; i < keywordArray.length; i++) {
            if (Keyword.IgnoreWords.get(keywordArray[i].toUpperCase()) != null) {
                defaultInfo += "<em>";
                defaultInfo += keywordArray[i];
                defaultInfo += " ignored;</em>";
            } else if ( Keyword.PrioritizeWords.containsKey( keywordArray[ i].toUpperCase()))
                relevantKeywords.add( keywordArray[ i].toUpperCase());
            else
                nonPriorityKeywords.add( keywordArray[ i].toUpperCase());
        }
        //Functionality to add: NOT/AND/OR booleans, HashMap (TreeMap) so that original words retained
        //...
        //Now do query
        relevantKeywords.addAll( nonPriorityKeywords);
        Result queryResult;
        try {
            if (relevantKeywords.size() > 0) {
                queryResult = new Query().getAllTipsForKeyword((String) relevantKeywords.get(0));
                for (int i = 1; i < relevantKeywords.size(); i++) {
                    queryResult = Result.intersect(queryResult, new Query().getAllTipsForKeyword((String) relevantKeywords.get(i)));
                }
            } else {
                queryResult = new Result("", new String[0]);
            }
            queryResult.setExtraInfo(defaultInfo + request.getRequestURI() + "//" + request.getQueryString());
        } catch (Exception ex1) {
            queryResult = new Result(keywords, new String[0], "Sorry, an exception occured during query execution: " + ex1);
        }
        String doc = queryResult.asHTMLDocument();
        out.println(doc);
    }

}
