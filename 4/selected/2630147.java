package sf2.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import sf2.core.ProcessExecutor;
import sf2.core.ProcessExecutorException;
import sf2.io.impl.StreamObjectInputStream;
import sf2.io.impl.StreamObjectOutputStream;
import sf2.log.Logging;

public class StreamFuture implements Serializable {

    private static final long serialVersionUID = 4663009428792309070L;

    private static final boolean FALLBACK_2GB = true;

    private static final long SIZE_2GB = 2 * 1024L * 1024L * 1024L;

    public static final int TYPE_NULL = 0;

    public static final int TYPE_FILE = 1;

    public static final int TYPE_DIR = 2;

    protected static final String LOG_NAME = "Future";

    protected static Logging logging = Logging.getInstance();

    protected long size;

    protected boolean isDir;

    protected boolean sender;

    protected String path;

    protected String name;

    protected boolean opend;

    protected StreamObjectInputStream streamIn;

    protected StreamFuture() {
        path = null;
        isDir = false;
        sender = false;
    }

    public StreamFuture(String pathToFileOrDir) throws IOException {
        open(pathToFileOrDir);
    }

    public void open(String pathToFileOrDir) throws IOException {
        File file = new File(pathToFileOrDir);
        if (!file.exists()) throw new IOException("File not found: " + file.getAbsolutePath());
        path = pathToFileOrDir;
        name = file.getName();
        opend = true;
        sender = true;
        if (file.isDirectory()) {
            isDir = true;
        } else {
            isDir = false;
            size = file.length();
        }
    }

    public boolean isTransferred() {
        return !sender;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (!(out instanceof StreamObjectOutputStream)) throw new IOException("OutputStream is not STREAMOBJECTOUTPUTSTREAM!");
        if (!opend) throw new IOException("StreamFuture open() was not called.");
        StreamObjectOutputStream streamOut = (StreamObjectOutputStream) out;
        streamOut.addFuture(this);
        if (isDir) {
            streamOut.writeUnshared(TYPE_DIR);
        } else {
            streamOut.writeUnshared(TYPE_FILE);
            streamOut.writeUnshared(name);
            streamOut.writeUnshared(size);
        }
    }

    public void transferTo(WritableByteChannel channel, ByteBuffer buffer, CharsetEncoder encoder) throws IOException {
        if (isDir) {
            recursiveTransfer(new File(path), channel, buffer, encoder);
        } else {
            FileInputStream fileIn = new FileInputStream(path);
            FileChannel fileChannel = fileIn.getChannel();
            long pos = 0, rem = fileChannel.size();
            if (!FALLBACK_2GB || rem <= SIZE_2GB) {
                while (rem > 0) {
                    long transferred = fileChannel.transferTo(pos, rem, channel);
                    if (transferred <= 0) {
                        logging.warning(LOG_NAME, "Unexpected termination of transfer");
                        break;
                    }
                    pos += transferred;
                    rem -= transferred;
                }
            } else {
                ByteBuffer buf = ByteBuffer.allocate(8192);
                while (fileChannel.read(buf) > 0) {
                    buf.flip();
                    while (buf.hasRemaining()) channel.write(buf);
                    buf.clear();
                }
            }
            fileChannel.close();
        }
    }

    protected void recursiveTransfer(File target, WritableByteChannel channel, ByteBuffer buffer, CharsetEncoder encoder) throws IOException {
        String name = target.getName();
        long pos, rem;
        if (target.isDirectory()) {
            buffer.clear();
            buffer.putInt(TYPE_DIR);
            buffer.putInt(0);
            encoder.reset();
            encoder.encode(CharBuffer.wrap(name), buffer, true);
            encoder.flush(buffer);
            int mark = buffer.position();
            int len = buffer.position() - 8;
            buffer.position(4);
            buffer.putInt(len);
            buffer.position(mark);
            buffer.flip();
            while (buffer.hasRemaining()) channel.write(buffer);
            for (File f : target.listFiles()) {
                recursiveTransfer(f, channel, buffer, encoder);
            }
            buffer.clear();
            buffer.putInt(TYPE_NULL);
            buffer.putInt(0);
            buffer.flip();
            while (buffer.hasRemaining()) channel.write(buffer);
        } else {
            FileInputStream fileIn = new FileInputStream(target);
            FileChannel fileChannel = fileIn.getChannel();
            long size = fileChannel.size();
            buffer.clear();
            buffer.putInt(TYPE_FILE);
            buffer.putInt(0);
            encoder.reset();
            encoder.encode(CharBuffer.wrap(name), buffer, true);
            encoder.flush(buffer);
            int mark = buffer.position();
            int len = buffer.position() - 8;
            buffer.position(4);
            buffer.putInt(len);
            buffer.position(mark);
            buffer.putLong(size);
            buffer.flip();
            while (buffer.hasRemaining()) channel.write(buffer);
            pos = 0;
            rem = size;
            if (!FALLBACK_2GB || rem < SIZE_2GB) {
                while (rem > 0) {
                    long transferred = fileChannel.transferTo(pos, rem, channel);
                    if (transferred <= 0) {
                        logging.warning(LOG_NAME, "Unexpected termination of transfer");
                        break;
                    }
                    pos += transferred;
                    rem -= transferred;
                }
            } else {
                ByteBuffer buf = ByteBuffer.allocate(8192);
                while (fileChannel.read(buf) > 0) {
                    buf.flip();
                    while (buf.hasRemaining()) channel.write(buf);
                    buf.clear();
                }
            }
            fileChannel.close();
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (!(in instanceof StreamObjectInputStream)) throw new IOException("InputStream is not STREAMOBJECTINPUTSTREAM!");
        streamIn = (StreamObjectInputStream) in;
        int type = (Integer) in.readUnshared();
        if (type == TYPE_DIR) {
            streamIn.addFuture(this);
            isDir = true;
        } else if (type == TYPE_FILE) {
            name = (String) in.readUnshared();
            size = (Long) in.readUnshared();
            streamIn.addFuture(this);
            isDir = false;
        }
        opend = true;
    }

    protected void transferTo(FileChannel channel) throws IOException {
        ReadableByteChannel sockChannel = streamIn.getChannel();
        long pos = 0, rem = size;
        showProgress(name, pos, size);
        while (rem > 0) {
            long transferred = channel.transferFrom(sockChannel, pos, rem);
            if (transferred <= 0) {
                logging.warning(LOG_NAME, "Unexpected termination of transfer");
                break;
            }
            pos += transferred;
            rem -= transferred;
            showProgress(name, pos, size);
        }
    }

    protected void showProgress(String name, long pos, long size) {
        if (size == 0) return;
        int i;
        String str = name + " |";
        for (i = 0; i < 10 * pos / size; i++) str += "==";
        for (; i < 10; i++) str += "  ";
        str += "| " + (size / 1024 / 1024) + " MB, " + (100 * pos / size) + " %";
        logging.debug(LOG_NAME, str);
    }

    protected boolean recursiveTransfer(String path, boolean topDir) throws IOException {
        ReadableByteChannel channel = streamIn.getChannel();
        ByteBuffer buffer = streamIn.getBuffer();
        CharsetDecoder decoder = streamIn.getDecoder();
        boolean reached = false;
        buffer.clear();
        buffer.limit(8);
        while (buffer.hasRemaining()) channel.read(buffer);
        buffer.flip();
        int type = buffer.getInt();
        int len = buffer.getInt();
        if (type == TYPE_NULL) {
            reached = true;
        } else {
            buffer.clear();
            buffer.limit(len);
            while (buffer.hasRemaining()) channel.read(buffer);
            buffer.flip();
            String name = decoder.decode(buffer).toString();
            if (type == TYPE_DIR) {
                String dirPath = path + File.separator + name;
                if (topDir) this.path = dirPath;
                File target = new File(dirPath);
                if (!target.exists()) target.mkdirs();
                while (!recursiveTransfer(dirPath, false)) ;
            } else if (type == TYPE_FILE) {
                buffer.clear();
                buffer.limit(8);
                while (buffer.hasRemaining()) channel.read(buffer);
                buffer.flip();
                long size = buffer.getLong();
                FileOutputStream fileOut = new FileOutputStream(path + File.separator + name);
                FileChannel fileChannel = fileOut.getChannel();
                long pos = 0, rem = size;
                showProgress(name, pos, size);
                while (rem > 0) {
                    long transferred = fileChannel.transferFrom(channel, pos, rem);
                    if (transferred <= 0) {
                        logging.warning(LOG_NAME, "Unexpected termination of transfer");
                        break;
                    }
                    pos += transferred;
                    rem -= transferred;
                    showProgress(name, pos, size);
                }
                fileChannel.close();
            }
        }
        return reached;
    }

    public void transferTo(String pathLocal) throws IOException {
        if (isDir) {
            recursiveTransfer(pathLocal, true);
        } else {
            path = pathLocal + File.separator + name;
            FileOutputStream fileOut = new FileOutputStream(path);
            transferTo(fileOut.getChannel());
            fileOut.close();
        }
    }

    public String getPath() {
        return path;
    }

    public boolean moveTo(String dest) {
        File src = new File(path);
        return src.renameTo(new File(dest));
    }

    public String toString() {
        return path;
    }
}
