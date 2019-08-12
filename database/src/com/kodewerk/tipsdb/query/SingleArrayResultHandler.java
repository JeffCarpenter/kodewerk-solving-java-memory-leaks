package com.kodewerk.tipsdb.query;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SingleArrayResultHandler implements ResultHandler {

    String[] values;

    public SingleArrayResultHandler(int size) {
        values = new String[size];
    }

    public void result(ResultSet rs)
            throws SQLException {
        for (int i = 0; i < values.length; i++) {
            rs.next();
            values[i] = rs.getString(1);
        }
    }

    public String[] getValuesAsStringArray() {
        return values;
    }
}
