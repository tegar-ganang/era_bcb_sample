package adv.tools;

import java.io.*;
import java.nio.channels.*;

/**
 * Alberto Vilches Ratón
 * User: avilches
 * Date: 15-oct-2006
 * Time: 22:59:34
 * To change this template use File | Settings | File Templates.
 */
public class IOTools {

    public static void copyFile(File in, File out) throws Exception {
        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;
        try {
            sourceChannel = new FileInputStream(in).getChannel();
            destinationChannel = new FileOutputStream(out).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        } finally {
            if (sourceChannel != null) sourceChannel.close();
            if (destinationChannel != null) destinationChannel.close();
        }
    }

    public static void serialize(Object o, File f) throws IOException {
        FileOutputStream fout = new FileOutputStream(f);
        serialize(o, fout);
    }

    public static void serialize(Object o, OutputStream os) throws IOException {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(os);
            oos.writeObject(o);
        } finally {
            if (oos != null) oos.close();
        }
    }

    public static Object deserialize(File f) throws IOException, ClassNotFoundException {
        FileInputStream fin = new FileInputStream(f);
        return deserialize(fin);
    }

    public static Object deserialize(InputStream os) throws IOException, ClassNotFoundException {
        ObjectInputStream oos = null;
        try {
            oos = new ObjectInputStream(os);
            Object o = oos.readObject();
            return o;
        } finally {
            if (oos != null) oos.close();
        }
    }

    public static int getObjectSize(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialize(object, baos);
        byte[] bytes = baos.toByteArray();
        return bytes.length;
    }

    public static Object clone(Object object) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialize(object, baos);
        byte[] bytes = baos.toByteArray();
        return deserialize(new ByteArrayInputStream(bytes));
    }

    public static int copy(InputStream input, OutputStream output) throws IOException {
        int total = 0;
        try {
            final byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) > 0) {
                total += n;
                output.write(buffer, 0, n);
            }
        } finally {
            if (input != null) input.close();
        }
        return total;
    }

    public static int copy(Reader input, Writer output) throws IOException {
        int total = 0;
        try {
            final char[] buffer = new char[4096];
            int n;
            while (-1 != (n = input.read(buffer))) {
                total += n;
                output.write(buffer, 0, n);
            }
        } finally {
            if (input != null) input.close();
        }
        return total;
    }
}
