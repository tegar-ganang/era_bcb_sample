package com.xsky.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import org.apache.commons.codec.binary.Base64;
import org.hibernate.Hibernate;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.xsky.common.util.Compresser;

public class XStreamBlobConverter implements Converter {

    /**
	 * 将Blob进行压缩编码生成Base64编码
	 */
    public void marshal(Object obj, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        Blob blob = (Blob) obj;
        InputStream in = null;
        ByteArrayOutputStream bout = null;
        try {
            in = blob.getBinaryStream();
            bout = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int readBytes = in.read(buffer);
            while (readBytes > 0) {
                bout.write(buffer, 0, readBytes);
                readBytes = in.read(buffer);
            }
            writer.setValue(Compresser.compressToBase64Str(bout.toByteArray()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (bout != null) {
                try {
                    bout.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
 * 从经过压缩编码的字符串中获取Blob
 */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        Blob obj = null;
        try {
            String base64Str = reader.getValue();
            byte[] base64 = Compresser.decompressFromBase64Str(base64Str).getBytes();
            obj = Hibernate.createBlob(base64);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return obj;
    }

    public boolean canConvert(Class clazz) {
        return clazz == Blob.class || Blob.class.isAssignableFrom(clazz);
    }
}
