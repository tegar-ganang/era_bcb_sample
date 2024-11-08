package ar.com.jkohen.util;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.*;

public class Resource {

    /**
     * BUGS:
     *  * Netscape Navigator (tested with 4.7x) won't load files from outside a JAR.
     *  * Life would be easier if Netscape Navigator supported Class.getResource(). Their not supporting it is actually documented on Netscape's Developers website.
     */
    public static byte[] getResource(String name, Object target) throws IOException {
        Class cl = (target instanceof Class) ? (Class) target : target.getClass();
        InputStream is = cl.getResourceAsStream(name);
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int pos = 0;
        int readed;
        final int chunk_size = 4096;
        byte[] buf = new byte[chunk_size];
        while (-1 != (readed = bis.read(buf, 0, chunk_size))) {
            baos.write(buf, pos, readed);
            pos += readed;
        }
        return baos.toByteArray();
    }

    public static Image createImage(String resource_name, Object target) throws IOException {
        byte[] image = getResource(resource_name, target);
        return Toolkit.getDefaultToolkit().createImage(image);
    }
}
