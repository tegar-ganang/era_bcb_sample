package com.googlecode.compress_j2me.gzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.framework.Assert;

public class UnitTest {

    public static ByteArrayInputStream s2in(String content) {
        return new ByteArrayInputStream(s2b(content));
    }

    public static ByteArrayInputStream h2in(String content) {
        return new ByteArrayInputStream(h2b(content));
    }

    public static ByteArrayInputStream b2in(byte[] content) {
        return new ByteArrayInputStream(content);
    }

    public static ByteArrayInputStream file2in(String fileName) {
        try {
            return new ByteArrayInputStream(readFile(fileName));
        } catch (IOException e) {
            Assert.fail(e.toString());
            return null;
        }
    }

    public static AssertiveOutputStream s2out(String content) {
        return new AssertiveOutputStream(s2b(content));
    }

    public static AssertiveOutputStream h2out(String content) {
        return new AssertiveOutputStream(h2b(content));
    }

    public static AssertiveOutputStream file2out(String fileName) {
        try {
            return new AssertiveOutputStream(readFile(fileName));
        } catch (IOException e) {
            Assert.fail(e.toString());
            return null;
        }
    }

    static void pump(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        out.flush();
    }

    static void pump(final ZStream in, OutputStream out) throws IOException {
        pump(new InputStream() {

            @Override
            public int read() throws IOException {
                return in.read();
            }
        }, out);
    }

    static void pump(InputStream in, final ZStream out) throws IOException {
        pump(in, new OutputStream() {

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }
        });
    }

    public static final byte[] s2b(String content) {
        byte[] data = new byte[content.length()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) content.charAt(i);
        }
        return data;
    }

    public static final byte h2b(char ch) {
        ch = Character.toLowerCase(ch);
        if (ch >= '0' && ch <= '9') {
            return (byte) (ch - '0');
        }
        if (ch >= 'a' && ch <= 'f') {
            return (byte) (ch - 'a' + 10);
        }
        throw new RuntimeException("invalid hex=" + ch);
    }

    public static final byte[] h2b(String hex) {
        hex = hex.replaceAll("\\s", "");
        byte[] data = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); ++i) {
            data[i / 2] <<= 4;
            data[i / 2] |= h2b(hex.charAt(i));
        }
        return data;
    }

    public static final byte[] readFile(String fileName) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024);
        FileInputStream origFile = new FileInputStream(fileName);
        byte[] data = new byte[128];
        int read;
        while ((read = origFile.read(data)) > 0) {
            buffer.write(data, 0, read);
        }
        return buffer.toByteArray();
    }

    public static final void writeFile(String fileName, byte[] data) throws IOException {
        FileOutputStream out = new FileOutputStream(fileName);
        out.write(data);
        out.close();
    }

    public static String toHex(int ch) {
        char[] H = "0123456789ABCDEF".toCharArray();
        return "" + H[(0xF0 & ch) >> 4] + H[ch & 0x0F];
    }
}
