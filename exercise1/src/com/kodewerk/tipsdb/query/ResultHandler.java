package com.kodewerk.tipsdb.query;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultHandler {
    public void result(ResultSet rs) throws SQLException;
}
