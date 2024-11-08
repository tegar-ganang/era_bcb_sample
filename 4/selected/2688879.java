package jshm.internal.patcher;

import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 
 * @author Tim Mullin
 *
 */
public class JarLoader {

    public static void addFileToClasspath(File file) throws Exception {
        Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
        method.setAccessible(true);
        method.invoke(ClassLoader.getSystemClassLoader(), new Object[] { file.toURI().toURL() });
    }

    public static void copy(InputStream source, File dest) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            byte[] buff = new byte[1024];
            int read = -1;
            while ((read = source.read(buff)) > -1) {
                out.write(buff, 0, read);
            }
        } finally {
            if (null != source) source.close();
            if (null != out) out.close();
        }
    }

    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
