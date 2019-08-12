package com.kodewerk.tipsdb.query;

import java.util.ArrayList;
import java.util.HashMap;

public class Result {

    String[] results;
    String keyword;
    String extraInfo;
    String queryType = "KEYWORD";

    public Result(String keyword, String[] arr) {
        results = arr;
        this.keyword = keyword;
    }

    public Result(String keyword, String[] arr, String extra) {
        results = arr;
        this.keyword = keyword;
        extraInfo = extra;
    }

    public void setExtraInfo(String e) {
        extraInfo = e;
    }


    public void setQueryType(String e) {
        queryType = e;
    }

    /** return the intersection of results */
    public static Result intersect(Result result1, Result result2) {
        String keyword = result1.keyword + " AND " + result2.keyword;
        HashMap resultsMap = new HashMap();
        for (int i = 0; i < result1.results.length; i++)
            resultsMap.put(result1.results[i], Boolean.TRUE);
        ArrayList intersection = new ArrayList();
        for (int i = 0; i < result2.results.length; i++)
            if (resultsMap.get(result2.results[i]) == Boolean.TRUE)
                intersection.add(result2.results[i]);

        String[] results = new String[intersection.size()];
        for (int i = 0; i < intersection.size(); i++)
            results[i] = (String) intersection.get(i);

        String extraInfo;
        if (result2.extraInfo == null)
            extraInfo = result1.extraInfo;
        else if (result1.extraInfo == null)
            extraInfo = result2.extraInfo;
        else
            extraInfo = result1.extraInfo + result2.extraInfo;
        return new Result(keyword, results, extraInfo);
    }

    public String asHTMLDocument() {
        String document = "<HTML>\r\n";
        document += "<HEAD><TITLE>Query the tips base</TITLE></HEAD>\r\n";
        document += "<BODY>\r\n";
        document += "<H1>Query the tips base</H1>\r\n";
        document += "\r\n";
        document += "<FORM ACTION=\"";
        document += queryType.toLowerCase();
        document += "\" METHOD=GET>\r\n";
        document += queryType;
        document += ": <INPUT TYPE=\"text\" NAME=\"";
        document += queryType.toLowerCase();
        document += "\" SIZE=40 MAXLENGTH=32 VALUE=\"\r\n";
        document += keyword;
        document += "\" ><BR>\r\n";
        if (extraInfo != null) {
            document += extraInfo;
        }
        document += "<BR>\r\n";
        document += "<INPUT TYPE=\"submit\" VALUE=\"Run Query\">\r\n";
        document += "<INPUT TYPE=\"reset\" VALUE=\"Clear Fields\">\r\n";
        document += "</FORM>\r\n";
        document += "<HR>\r\n";
        document += "<P>\r\n";
        document += "RESULTS: (";
        document += results.length;
        if (results.length != 1)
            document += " tips) \r\n";
        else
            document += " tip) \r\n";
        document += "</P>\r\n";

        document += "<UL>\r\n";
        for (int i = 0; i < results.length; i++) {
            document += "<LI>\r\n";
            document += results[i];
            document += "\r\n";
        }
        document += "</UL>\r\n";
        document += "\r\n";
        document += "</BODY>\r\n";
        document += "</HTML>\r\n";

        return document;
    }

}
