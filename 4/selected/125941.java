package org.carp.assemble;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;

public class InputStreamAssemble implements Assemble {

    public void setValue(ResultSet rs, List<Object> data, int index) throws Exception {
        InputStream ins = rs.getBinaryStream(index);
        if (ins == null) {
            data.add(null);
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b = new byte[2048];
        for (int len = -1; (len = ins.read(b, 0, 2048)) != -1; ) {
            baos.write(b, 0, len);
        }
        java.io.ByteArrayInputStream value = new java.io.ByteArrayInputStream(baos.toByteArray());
        data.add(value);
    }

    public Object setFieldValue(ResultSet rs, Object entity, Method m, int index) throws Exception {
        InputStream ins = rs.getBinaryStream(index);
        if (ins != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[2048];
            for (int len = -1; (len = ins.read(b, 0, 2048)) != -1; ) baos.write(b, 0, len);
            ins = new java.io.ByteArrayInputStream(baos.toByteArray());
        }
        m.invoke(entity, new Object[] { ins });
        return ins;
    }

    public Object setFieldValue(ResultSet rs, Object entity, Field f, int index) throws Exception {
        boolean isAccess = f.isAccessible();
        InputStream ins = rs.getBinaryStream(index);
        if (ins != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[2048];
            for (int len = -1; (len = ins.read(b, 0, 2048)) != -1; ) baos.write(b, 0, len);
            ins = new java.io.ByteArrayInputStream(baos.toByteArray());
        }
        f.setAccessible(true);
        f.set(entity, ins);
        f.setAccessible(isAccess);
        return ins;
    }

    public Object setFieldValue(Object entity, Field f, Object value) throws Exception {
        boolean isAccess = f.isAccessible();
        InputStream ins = (InputStream) value;
        if (ins != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[2048];
            for (int len = -1; (len = ins.read(b, 0, 2048)) != -1; ) baos.write(b, 0, len);
            ins = new java.io.ByteArrayInputStream(baos.toByteArray());
        }
        f.setAccessible(true);
        f.set(entity, ins);
        f.setAccessible(isAccess);
        return ins;
    }
}
