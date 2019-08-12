package com.kodewerk.tipsdb.query;

import java.util.stream.Stream;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SingleArrayResultHandler implements ResultHandler {

    ArrayList<String> values;

    public SingleArrayResultHandler(int size) {
        values = new ArrayList<>();
    }

    public void result(ResultSet rs) throws SQLException {
        boolean done = false;
        while ( true) {
            values.add(rs.getString(1));
            if (done) break;
            rs.next();
            done = rs.isLast();
        }
    }

    public String[] getValuesAsStringArray() {
        return values.toArray(new String[0]);
    }

    public Stream<String> stream() {
        return values.stream();
    }
}
