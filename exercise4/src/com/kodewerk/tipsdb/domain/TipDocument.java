package com.kodewerk.tipsdb.domain;

import java.util.stream.Stream;
import java.util.ArrayList;

public class TipDocument {
    //A TipDocument is the document from which tips were extracted
    String url;
    String description;
    String details;
    ArrayList<Tip> tips = new ArrayList<>();

    public void setUrl(String s) {
        url = s;
    }

    public void setDescription(String s) {
        description = s;
    }

    public void setDetails(String s) {
        details = s;
    }

    public void setTips(Tip[] t) {
        for ( Tip tip : t) {
            tips.add(tip);
        }
    }

    public int getNumberOfTips() {
        return tips.size();
    }

    public Tip getTip(int i) {
        return tips.get(i);
    }

    public void setTipAt(int i, Tip t) {
        tips.add(i,t);
    }

    public Stream<Tip> stream() {
        return tips.stream();
    }

    public Stream<Tip> parallelStream() {
        return tips.parallelStream();
    }
}
