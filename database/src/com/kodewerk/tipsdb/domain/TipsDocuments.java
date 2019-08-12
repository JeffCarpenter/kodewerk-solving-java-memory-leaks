package com.kodewerk.tipsdb.domain;

import com.kodewerk.tipsdb.TipsDBProperties;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class TipsDocuments {
    //A TipDocument is the document from which tips were extracted

    private ArrayList tipsDocuments;

    public TipsDocuments() {
    }

    public ArrayList getTipsDocuments() throws IOException {
        if ( tipsDocuments == null) {
            this.loadTipsDocuments();
        }
        return this.tipsDocuments;
    }

    private void loadTipsDocuments() throws IOException {
            this.loadTipsDocument(TipsDBProperties.getTipsDocumentFile());
    }

    private void loadTipsDocument(String filename) throws IOException {
        BufferedReader rdr = new BufferedReader(new FileReader(filename));
        this.tipsDocuments = new ArrayList();
        while ("START".equals(rdr.readLine())) {
            TipDocument doc = new TipDocument();
            this.tipsDocuments.add(doc);
            doc.setUrl(rdr.readLine());
            doc.setDescription(rdr.readLine());
            doc.setDetails(rdr.readLine());
            int numtiplines = Integer.parseInt(rdr.readLine());
            doc.setTips(new Tip[numtiplines]);
            for (int i = 0; i < numtiplines; i++) {
                doc.setTipAt(i, new Tip(rdr.readLine()));
            }
        }
        String s;
        if ((s = rdr.readLine()) != null)
            throw new IOException("Expected end of file but got: " + s);
        Keyword.completeInit();
        //TipsDocuments.AllTipDocuments = new TipsDocuments[alldocs.size()];
        //for (int i = 0; i < alldocs.size(); i++)
        //    TipsDocuments.AllTipDocuments[i] = (TipsDocuments) alldocs.get(i);
        //TipsDocuments.Initialized = true;
    }

}
