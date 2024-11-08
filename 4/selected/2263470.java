package org.privale.utils;

import java.util.LinkedList;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

public class ChannelReader {

    public static int MAXINTLEN = 10000;

    public static long MAXLONGLEN = 1024L * 1024L * 1024L;

    private ReadableByteChannelWithSize Chan;

    public ChannelReader(ReadableByteChannelWithSize f) {
        Chan = f;
    }

    public ChannelReader(FileChannel f) {
        this(new FileChannelWithSize(f));
    }

    public ChannelReader(File f) throws IOException {
        this(new FileInputStream(f).getChannel());
    }

    public byte getByte() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1);
        Chan.read(buf);
        buf.clear();
        return buf.get();
    }

    public boolean getBoolean() throws IOException {
        if (getByte() != 0) return true;
        return false;
    }

    public int getInt() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        Chan.read(buf);
        buf.clear();
        return buf.getInt();
    }

    public long getLong() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        Chan.read(buf);
        buf.clear();
        return buf.getLong();
    }

    public void getByteBuffer(ByteBuffer buf) throws IOException {
        Chan.read(buf);
    }

    public ByteBuffer getByteBuffer(int len) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(len);
        Chan.read(buf);
        buf.clear();
        return buf;
    }

    public byte[] getBytes(int len) throws IOException {
        ByteBuffer b = getByteBuffer(len);
        return BufToBytes(b);
    }

    public ByteBuffer getIntLenByteBuffer() throws IOException {
        int len = getInt();
        if (len > MAXINTLEN) {
            Thread.dumpStack();
            throw new IOException("Length exceeds max! " + len);
        }
        return getByteBuffer(len);
    }

    public byte[] getIntLenBytes() throws IOException {
        return BufToBytes(getIntLenByteBuffer());
    }

    public void getBytes(byte[] buf, int len) throws IOException {
        if (buf.length < len) {
            throw new IOException("byte array is not long enough for the length specified!");
        }
        ByteBuffer in = ByteBuffer.wrap(buf);
        in.limit(len);
        Chan.read(in);
    }

    public String getString() throws IOException {
        ByteBuffer buf = getIntLenByteBuffer();
        String s = new String(BufToBytes(buf));
        return s;
    }

    public String getLine() throws IOException {
        String s = "";
        String t = null;
        byte[] b = new byte[1];
        do {
            getBytes(b, 1);
            t = new String(b);
            if (!t.equals("\n") && !t.equals("\r")) {
                s = s + t;
            }
        } while (!t.equals("\n") && remaining() > 0);
        return s;
    }

    public void getFileChannel(FileChannel chan, long len) throws IOException {
        chan.transferFrom(Chan, chan.position(), len);
    }

    public void getLongFileChannel(FileChannel chan) throws IOException {
        long l = getLong();
        if (l > MAXLONGLEN) {
            throw new IOException("Long length exceeds max! " + l);
        }
        chan.transferFrom(Chan, chan.position(), l);
    }

    public File getFile(long len, FileManager m) throws IOException {
        File n = m.createNewFile("cr", "get");
        if (len > MAXLONGLEN) {
            throw new IOException("Long length exceeds max! " + len);
        }
        if (len > 0) {
            FileOutputStream fos = new FileOutputStream(n);
            FileChannel foc = fos.getChannel();
            getFileChannel(foc, len);
            foc.close();
        }
        return n;
    }

    public File getLongFile(FileManager m) throws IOException {
        long l = getLong();
        return getFile(l, m);
    }

    public File getToEndFile(FileManager m) throws IOException {
        long l = Chan.size() - Chan.position();
        return getFile(l, m);
    }

    public void Read(Reader r) throws IOException {
        r.Read(this);
    }

    public long getSize() throws IOException {
        return Chan.size();
    }

    public long getPos() throws IOException {
        return Chan.position();
    }

    public void setPos(long pos) throws IOException {
        Chan.position(pos);
    }

    public long remaining() throws IOException {
        return getSize() - getPos();
    }

    public void close() throws IOException {
        if (Chan.isOpen()) {
            Chan.close();
        }
    }

    public static ByteBuffer OISreadByteBuffer(ObjectInputStream s) throws IOException, ClassNotFoundException {
        Boolean b = (Boolean) s.readObject();
        if (b.equals(Boolean.valueOf(true))) {
            byte[] bytes = (byte[]) s.readObject();
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            return buf;
        }
        return null;
    }

    public static LinkedList<ByteBuffer> OISreadListByteBuffer(ObjectInputStream s) throws IOException, ClassNotFoundException {
        Integer number = (Integer) s.readObject();
        if (number.equals(Integer.valueOf(-1))) {
            return null;
        } else {
            LinkedList<ByteBuffer> buf = new LinkedList<ByteBuffer>();
            for (int cnt = 0; cnt < number; cnt++) {
                buf.add(OISreadByteBuffer(s));
            }
            return buf;
        }
    }

    public static byte[] BufToBytes(ByteBuffer buf) {
        if (buf.hasArray()) {
            return buf.array();
        } else {
            buf.clear();
            byte[] b = new byte[buf.remaining()];
            for (int cnt = 0; buf.hasRemaining(); cnt++) {
                b[cnt] = buf.get();
            }
            return b;
        }
    }
}
