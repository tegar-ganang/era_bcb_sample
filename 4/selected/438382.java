package org.neblipedia.archivos;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Usese para leer tanto archivos grandes como pequeños.<br />
 * Facilita el uso de la clase FileChannel
 * 
 * @author juan
 * 
 */
public class LeedorFileChannel {

    public static String leer(File file) throws IOException {
        LeedorFileChannel tmp = new LeedorFileChannel(file);
        String tmp2 = tmp.leerString();
        tmp.close();
        return tmp2;
    }

    public static byte[] leerByte(File f) throws IOException {
        LeedorFileChannel tmp = new LeedorFileChannel(f);
        long tam = tmp.size();
        byte[] tmp2 = new byte[(int) tam];
        tmp.read(tmp2);
        tmp.close();
        return tmp2;
    }

    private FileChannel reader;

    public LeedorFileChannel(File file) throws FileNotFoundException {
        FileInputStream fi = new FileInputStream(file);
        reader = fi.getChannel();
    }

    public LeedorFileChannel(String file) throws FileNotFoundException {
        this(new File(file));
    }

    public final void close() throws IOException {
        reader.close();
    }

    public String leerString() throws IOException {
        long tam = size();
        StringBuilder tmp = new StringBuilder();
        leerString(tmp, 0, (int) tam);
        return tmp.toString();
    }

    public StringBuilder leerString(int tam) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(tam);
        reader.read(bb);
        bb.flip();
        StringBuilder tt = new StringBuilder();
        tt.append(new String(bb.array()));
        return tt;
    }

    public long leerString(StringBuilder str, long ini, int tam) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(tam);
        long r = reader.read(bb, ini);
        bb.flip();
        str.append(new String(bb.array()));
        bb.clear();
        return r;
    }

    public int read(byte[] array) throws IOException {
        return reader.read(ByteBuffer.wrap(array));
    }

    public int read(byte[] array, long inicio) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(array);
        return reader.read(bb, inicio);
    }

    public long size() throws IOException {
        return reader.size();
    }
}
