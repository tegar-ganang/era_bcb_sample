package com.luzan.app.map.io;

import org.apache.commons.io.IOUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import com.luzan.common.httprpc.HttpRpcServer;
import com.sun.xfile.XFile;
import com.sun.xfile.XFileInputStream;

/**
 * BinaryWriter
 *
 * @author Alexander Bondar
 */
public class BinaryWriter extends com.luzan.common.httprpc.io.BinaryWriter {

    public void write(HttpServletRequest req, HttpServletResponse res, Object bean) throws IntrospectionException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, IOException {
        res.setContentType(contentType);
        final Object r;
        if (HttpRpcServer.HttpRpcOutput.class.isAssignableFrom(bean.getClass())) {
            HttpRpcServer.HttpRpcOutput output = (HttpRpcServer.HttpRpcOutput) bean;
            r = output.getResult();
        } else r = bean;
        if (r != null) {
            if (File.class.isAssignableFrom(r.getClass())) {
                File file = (File) r;
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    IOUtils.copy(in, res.getOutputStream());
                } finally {
                    if (in != null) in.close();
                }
            } else if (InputStream.class.isAssignableFrom(r.getClass())) {
                InputStream in = null;
                try {
                    in = (InputStream) r;
                    IOUtils.copy(in, res.getOutputStream());
                } finally {
                    if (in != null) in.close();
                }
            } else if (XFile.class.isAssignableFrom(r.getClass())) {
                XFile file = (XFile) r;
                InputStream in = null;
                try {
                    in = new XFileInputStream(file);
                    IOUtils.copy(in, res.getOutputStream());
                } finally {
                    if (in != null) in.close();
                }
            }
            res.getOutputStream().flush();
        }
    }
}
