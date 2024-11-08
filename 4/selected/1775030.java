package org.carp.assemble;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;

public class ReaderAssemble implements Assemble {

    public void setValue(ResultSet rs, List<Object> data, int index) throws Exception {
        Reader reader = rs.getCharacterStream(index);
        if (reader == null) {
            data.add(null);
            return;
        }
        StringWriter writer = new StringWriter();
        char[] buf = new char[2048];
        for (int len = -1; (len = reader.read(buf, 0, 2048)) != -1; ) {
            writer.write(buf, 0, len);
        }
        StringReader value = new StringReader(writer.toString());
        data.add(value);
    }

    public Object setFieldValue(ResultSet rs, Object entity, Method m, int index) throws Exception {
        Reader reader = rs.getCharacterStream(index);
        if (reader != null) {
            StringWriter writer = new StringWriter();
            char[] buf = new char[2048];
            for (int len = -1; (len = reader.read(buf, 0, 2048)) != -1; ) {
                writer.write(buf, 0, len);
            }
            reader = new StringReader(writer.toString());
        }
        m.invoke(entity, new Object[] { reader });
        return reader;
    }

    public Object setFieldValue(ResultSet rs, Object entity, Field f, int index) throws Exception {
        Reader reader = rs.getCharacterStream(index);
        if (reader != null) {
            StringWriter writer = new StringWriter();
            char[] buf = new char[2048];
            for (int len = -1; (len = reader.read(buf, 0, 2048)) != -1; ) {
                writer.write(buf, 0, len);
            }
            reader = new StringReader(writer.toString());
        }
        boolean isAccess = f.isAccessible();
        f.setAccessible(true);
        f.set(entity, reader);
        f.setAccessible(isAccess);
        return reader;
    }

    public Object setFieldValue(Object entity, Field f, Object value) throws Exception {
        Reader reader = (Reader) value;
        if (reader != null) {
            StringWriter writer = new StringWriter();
            char[] buf = new char[2048];
            for (int len = -1; (len = reader.read(buf, 0, 2048)) != -1; ) {
                writer.write(buf, 0, len);
            }
            reader = new StringReader(writer.toString());
        }
        boolean isAccess = f.isAccessible();
        f.setAccessible(true);
        f.set(entity, reader);
        f.setAccessible(isAccess);
        return reader;
    }
}
