package com.kodewerk.tipsdb.domain;

public class TipDocument {
    //A TipDocument is the document from which tips were extracted
    String url;
    String description;
    String details;
    Tip[] tips;

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
        tips = t;
    }

    public int getNumberOfTips() {
        return tips.length;
    }

    public Tip getTip(int i) {
        return tips[i];
    }

    public void setTipAt(int i, Tip t) {
        tips[i] = t;
    }
}
