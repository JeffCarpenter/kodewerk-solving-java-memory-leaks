package com.kodewerk.tipsdb.domain;

public class Tip {
    //A tip is one line of text, 10 keywords, a ref to the TipDocument
    String line;

    public Tip(String s) {
        line = s;
        Keyword.extractKeywordsFrom(this, s);
    }

    public String getTip() {
        return getTipFromLI();
    }

    public String getWholeTip() {
        return line;
    }

    public String getTipFromLI() {
        int idx = line.indexOf("LI>");
        return line.substring(idx + 3);
    }
}
