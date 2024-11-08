package com.luzan.common.httprpc.io;

import org.apache.commons.io.IOUtils;
import com.luzan.common.httprpc.HttpRpcWriter;
import com.luzan.common.httprpc.HttpRpcServer;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletOutputStream;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.io.*;

/**
 * PNGWriter
 *
 * @author Alexander Bondar
 */
public class BinaryWriter implements HttpRpcWriter {

    protected String contentType = "image/png";

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void write(HttpServletRequest req, HttpServletResponse res, Object bean) throws IntrospectionException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, IOException {
        res.setContentType(contentType);
        final Object r;
        if (HttpRpcServer.HttpRpcOutput.class.isAssignableFrom(bean.getClass())) {
            HttpRpcServer.HttpRpcOutput output = (HttpRpcServer.HttpRpcOutput) bean;
            r = output.getResult();
        } else r = bean;
        if (r != null) {
            final ServletOutputStream outputStream = res.getOutputStream();
            if (File.class.isAssignableFrom(r.getClass())) {
                File file = (File) r;
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    IOUtils.copy(in, outputStream);
                } finally {
                    if (in != null) in.close();
                }
            } else if (InputStream.class.isAssignableFrom(r.getClass())) {
                InputStream in = null;
                try {
                    in = (InputStream) r;
                    if (ByteArrayInputStream.class.isAssignableFrom(r.getClass())) res.addHeader("Content-Length", Integer.toString(in.available()));
                    IOUtils.copy(in, outputStream);
                } finally {
                    if (in != null) in.close();
                }
            }
            outputStream.flush();
        }
    }
}
