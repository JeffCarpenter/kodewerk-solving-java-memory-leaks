package com.kodewerk.tipsdb.query;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OneValueResultHandler implements ResultHandler {

    String value;

    public void result(ResultSet rs) throws SQLException {
        if (rs.next()) {
            value = rs.getString(1);
        }
    }

    public String getValueAsString() {
        return value;
    }
}
