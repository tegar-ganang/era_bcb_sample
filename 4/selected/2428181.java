package com.jacum.cms.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 */
public class ResourceUtil {

    private static final Log log = LogFactory.getLog(ResourceUtil.class);

    public static Properties getProperties(String path, ClassLoader cl) {
        Properties p = new Properties();
        try {
            InputStream is = getInputStream(path, cl);
            p.load(is);
        } catch (Exception e) {
            log.error("Can't load properties " + path, e);
            p = null;
        }
        return p;
    }

    public static InputStream getInputStream(String path, ClassLoader cl) {
        InputStream is = null;
        try {
            while (true) {
                is = cl.getResourceAsStream(path);
                if (is != null) {
                    break;
                }
                cl = cl.getParent();
                if (cl == null) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Can't get input stream from " + path);
        }
        return is;
    }

    public static InputStream getInputStream(String path) {
        return getInputStream(path, ResourceUtil.class.getClassLoader());
    }

    public static Properties getProperties(String path) {
        return getProperties(path, ResourceUtil.class.getClassLoader());
    }

    public static byte[] loadBinary(String path) throws IOException {
        return loadBinary(path, ResourceUtil.class.getClassLoader());
    }

    public static byte[] loadBinary(String path, ClassLoader cl) throws IOException {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        InputStream is = getInputStream(path, cl);
        if (is == null) {
            return null;
        }
        int read;
        byte[] buffer = new byte[1024];
        while ((read = is.read(buffer)) > -1) {
            byteOutput.write(buffer, 0, read);
        }
        is.close();
        return byteOutput.toByteArray();
    }
}
