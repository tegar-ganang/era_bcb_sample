package org.privale.utils;

import java.util.LinkedList;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

public class ChannelWriter {

    private WritableByteChannelWithSize Chan;

    public ChannelWriter(WritableByteChannelWithSize f) {
        Chan = f;
    }

    public ChannelWriter(FileChannel f) {
        this(new FileChannelWithSize(f));
    }

    public ChannelWriter(File f) throws IOException {
        this(new FileOutputStream(f).getChannel());
    }

    public ChannelWriter(File f, boolean append) throws IOException {
        this(new FileOutputStream(f, append).getChannel());
    }

    public void putByte(byte b) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1);
        buf.put(b);
        buf.clear();
        Chan.write(buf);
    }

    public void putBoolean(boolean b) throws IOException {
        if (b) {
            putByte((byte) 1);
        } else {
            putByte((byte) 0);
        }
    }

    public void putInt(int i) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        buf.putInt(i);
        buf.clear();
        Chan.write(buf);
    }

    public void putLong(long l) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
        buf.putLong(l);
        buf.clear();
        Chan.write(buf);
    }

    public void putBytes(byte[] in) throws IOException {
        Chan.write(ByteBuffer.wrap(in));
    }

    public void putByteBuffer(ByteBuffer b) throws IOException {
        Chan.write(b);
    }

    public void putIntLenByteBuffer(ByteBuffer b) throws IOException {
        putInt(b.remaining());
        putByteBuffer(b);
    }

    public void putIntLenBytes(byte[] bytes) throws IOException {
        putIntLenByteBuffer(ByteBuffer.wrap(bytes));
    }

    public void putString(String s) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(s.getBytes());
        putIntLenByteBuffer(buf);
    }

    public void putFileChannel(FileChannel chan, long len) throws IOException {
        chan.transferTo(chan.position(), len, Chan);
    }

    public void putLongFileChannel(FileChannel chan, long len) throws IOException {
        putLong(len);
        putFileChannel(chan, len);
    }

    public void putFile(File f) throws IOException {
        if (f != null) {
            if (f.exists()) {
                FileInputStream fis = new FileInputStream(f);
                FileChannel fic = fis.getChannel();
                putFileChannel(fic, fic.size());
                fic.close();
            } else {
                putLong(0);
            }
        } else {
            putLong(0);
        }
    }

    public void putLongFile(File f) throws IOException {
        if (f != null) {
            FileInputStream fis = new FileInputStream(f);
            FileChannel fic = fis.getChannel();
            putLongFileChannel(fic, fic.size());
            fic.close();
        } else {
            putLong(0);
        }
    }

    public static void OOSwriteByteBuffer(ByteBuffer buf, ObjectOutputStream s) throws IOException {
        if (buf != null) {
            buf.clear();
            byte[] b = ChannelReader.BufToBytes(buf);
            s.writeObject((Boolean) true);
            s.writeObject(b);
        } else {
            s.writeObject((Boolean) false);
        }
    }

    public static void OOSwriteListByteBuffer(LinkedList<ByteBuffer> l, ObjectOutputStream s) throws IOException {
        if (l == null) {
            Integer length = -1;
            s.writeObject(length);
        } else {
            Integer length = l.size();
            s.writeObject(length);
            for (int cnt = 0; cnt < l.size(); cnt++) {
                OOSwriteByteBuffer(l.get(cnt), s);
            }
        }
    }

    public void Write(Writer w) throws IOException {
        w.Write(this);
    }

    public void close() throws IOException {
        Chan.close();
    }
}
