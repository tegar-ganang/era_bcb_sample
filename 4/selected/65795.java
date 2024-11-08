package org.ji18n.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @version $Id: Streams.java 159 2008-07-03 01:28:51Z david_ward2 $
 * @author david at ji18n.org
 */
public class Streams {

    private Streams() {
    }

    public static void copy(InputStream is, OutputStream os) throws IOException {
        BufferedOutputStream bos = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(is);
            bos = new BufferedOutputStream(os);
            byte[] buf = new byte[1024];
            int read = 0;
            while ((read = bis.read(buf)) != -1) bos.write(buf, 0, read);
        } finally {
            if (bos != null) bos.flush();
        }
    }
}
