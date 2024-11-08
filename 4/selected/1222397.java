package com.jspx.utils;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by IntelliJ IDEA.
 * User: ChenYuan
 * Date: 2004-7-2
 * Time: 18:17:28
 * 
 */
public final class DBUtil {

    private DBUtil() {
    }

    public static void setLargeTextField(PreparedStatement pstmt, int parameterIndex, String value) throws SQLException {
        Reader bodyReader;
        try {
            bodyReader = new StringReader(value);
            pstmt.setCharacterStream(parameterIndex, bodyReader, value.length());
        } catch (Exception e) {
            e.printStackTrace();
            throw new SQLException("Failed to set text field.");
        }
    }

    public static String getLargeTextField(ResultSet rs, int columnIndex) throws SQLException {
        Reader bodyReader;
        String value;
        try {
            bodyReader = rs.getCharacterStream(columnIndex);
            if (bodyReader == null) return null;
            char buf[] = new char[256];
            StringWriter out = new StringWriter(255);
            int i;
            while ((i = bodyReader.read(buf)) >= 0) out.write(buf, 0, i);
            value = out.toString();
            out.close();
            bodyReader.close();
        } catch (Exception e) {
            return rs.getString(columnIndex);
        }
        if (value == null) return "";
        return value;
    }

    public static String getLargeTextField(ResultSet rs, String columnname) throws SQLException {
        Reader bodyReader;
        String value;
        try {
            bodyReader = rs.getCharacterStream(columnname);
            if (bodyReader == null) return "";
            char buf[] = new char[256];
            StringWriter out = new StringWriter(255);
            int i;
            while ((i = bodyReader.read(buf)) >= 0) out.write(buf, 0, i);
            value = out.toString();
            out.close();
            bodyReader.close();
        } catch (Exception e) {
            return rs.getString(columnname);
        }
        return value;
    }
}
