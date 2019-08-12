package com.kodewerk.tipsdb.servlet;

import com.kodewerk.tipsdb.domain.Tip;
import com.kodewerk.tipsdb.domain.TipsDocuments;
import com.kodewerk.tipsdb.query.Result;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuickQuery extends SimpleQuery {

    TipsDocuments documents = new TipsDocuments();

    public void init() {
    }

    public String queryType() {
        return "Quick";
    }

    public void doValidQuery(String parameterkeywords, PrintWriter out,
                             HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String keywords = parameterkeywords.replaceAll("\\%..", " "); //get rid of all HTML mappings
        keywords = keywords.replaceAll("\\W+", " "); //get rid of all non-word chars
        List<String> keywordArray = Stream.of(keywords.split(" ")).parallel().collect(Collectors.toList());

        Predicate<Tip> tipFilter = tip -> keywordArray.parallelStream()
                .map(String::toUpperCase)
                .filter(s -> tip.getTipFromLI().toUpperCase().contains(s))
                .count() > 0L;

        //Now do query
        List<String> resultList = documents.parallelStream()
                .flatMap(doc -> doc.stream())
                .filter(tipFilter)
                .map(Tip::getTipFromLI)
                .collect(Collectors.toList());

        String[] resultList2 = resultList.toArray(new String[0]);
        Result queryResult = new Result(keywords, resultList2, defaultInfo());
        queryResult.setQueryType(queryType().toUpperCase());
        String doc = queryResult.asHTMLDocument();
        out.println(doc);
    }

    public String defaultInfo() {
        String defaultInfo = "Only alphanumeric characters are relevant for the query.<BR>";
        return defaultInfo;
    }
}
