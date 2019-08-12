package com.kodewerk.tipsdb.servlet;

import com.kodewerk.tipsdb.domain.TipDocument;
import com.kodewerk.tipsdb.domain.Tip;
import com.kodewerk.tipsdb.domain.TipsDocuments;
import com.kodewerk.tipsdb.query.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class WildcardQuery extends SimpleQuery {


    TipsDocuments documents = new TipsDocuments();
    public void init() {
        try {
            documents.getTipsDocuments();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String queryType() {
        return "wildcard";
    }

    public void doValidQuery(String parameterkeywords, PrintWriter out, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String keywords = parameterkeywords.replaceAll("\\%..", " "); //get rid of all HTML mappings
        keywords = keywords.replaceAll("\\W+", " "); //get rid of all non-word chars
        String[] keywordArray = keywords.split(" ");
        //Functionality to add: NOT/AND/OR booleans, HashMap (TreeMap) so that original words retained
        //...
        //Now do query
        ArrayList resultList = new ArrayList();
        Iterator tipsDocumentsIterator = documents.getTipsDocuments().iterator();
        while ( tipsDocumentsIterator.hasNext()) {
            TipDocument doc = (TipDocument)tipsDocumentsIterator.next();
            for (int j = 0; j < doc.getNumberOfTips(); j++) {
                Tip tip = doc.getTip(j);
                boolean includeInResults = true;
                for (int k = 0; k < keywordArray.length; k++) {
                    if (tip.getTipFromLI().toUpperCase().indexOf(keywordArray[k].toUpperCase()) == -1)
                        includeInResults = false;
                }
                if (includeInResults)
                    resultList.add(tip.getTipFromLI());
            }
        }
        String[] resultList2 = new String[resultList.size()];
        for (int i = 0; i < resultList.size(); i++)
            resultList2[i] = (String) resultList.get(i);
        Result queryResult = new Result(keywords, resultList2, defaultInfo());
        queryResult.setQueryType(queryType().toUpperCase());
        String doc = queryResult.asHTMLDocument();
        out.println(doc);
    }

}
