package it.aton.proj.dem.storage.impl.pmap;

import gnu.trove.THashMap;
import gnu.trove.TObjectObjectProcedure;
import gnu.trove.TObjectProcedure;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class FMap {

    private static final String LOG_FILE_NAME = "db.log";

    private static final String LOCK_FILE_NAME = "db.lock";

    private static final String DAT_FILE_NAME = "db.dat";

    private THashMap<String, String> map = new THashMap<String, String>();

    private File fDir;

    private OutputStream writer;

    private volatile boolean closed;

    private boolean dirty = true;

    public FMap(String dir, long snapshotInterval) throws IOException {
        fDir = new File(dir);
        lockDb();
        load();
        snapshot(true);
        closed = false;
        new Snapshotter(snapshotInterval).start();
    }

    public synchronized void clear(final String prefix) {
        if (closed) throw new IllegalStateException("Storage is closed");
        write('C', prefix);
        doClear(prefix);
    }

    private void doClear(final String prefix) {
        map.forEachKey(new TObjectProcedure<String>() {

            public boolean execute(String key) {
                if (key.startsWith(prefix)) map.remove(key);
                return true;
            }
        });
    }

    public synchronized String get(String key) {
        if (closed) throw new IllegalStateException("Storage is closed");
        return map.get(key);
    }

    public synchronized Map<String, String> getObjects(final String keyPrefix) throws Exception {
        if (closed) throw new IllegalStateException("Storage is closed");
        final Map<String, String> ret = new THashMap<String, String>();
        map.forEachEntry(new TObjectObjectProcedure<String, String>() {

            public boolean execute(String key, String value) {
                if (key.startsWith(keyPrefix)) ret.put(key, value);
                return true;
            }
        });
        return ret;
    }

    public synchronized void put(String key, String value) {
        if (closed) throw new IllegalStateException("Storage is closed");
        write('P', key, value);
        doPut(key, value);
    }

    private void doPut(String key, String value) {
        map.put(key, value);
    }

    public synchronized String remove(String key) {
        write('R', key);
        return doRemove(key);
    }

    private String doRemove(String key) {
        return map.remove(key);
    }

    public synchronized int size() {
        if (closed) throw new IllegalStateException("Storage is closed");
        return map.size();
    }

    public synchronized void close() {
        if (closed) throw new IllegalStateException("Storage is already closed");
        try {
            snapshot(false);
            closed = true;
            writer.close();
            unlockDb();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File lockFile;

    private FileOutputStream lockFileOutputStream;

    private FileLock lockFileLock;

    private void lockDb() throws IOException {
        lockFile = new File(fDir, LOCK_FILE_NAME);
        if (!lockFile.exists()) lockFile.createNewFile();
        lockFile.deleteOnExit();
        lockFileOutputStream = new FileOutputStream(lockFile);
        lockFileLock = lockFileOutputStream.getChannel().tryLock();
        if (lockFileLock == null) {
            lockFileOutputStream.close();
            throw new IOException("Database is already in use.");
        }
    }

    private void unlockDb() throws IOException {
        lockFileLock.release();
        lockFileOutputStream.close();
        lockFile.delete();
    }

    private synchronized void write(char cmd, String... args) {
        dirty = true;
        try {
            writeChar(writer, cmd);
            if (args != null) {
                for (String arg : args) writeString(writer, arg);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void snapshot(boolean reopenWriter) {
        try {
            if (writer != null) writer.close();
            if (dirty) {
                File file = new File(fDir, DAT_FILE_NAME);
                final OutputStream os = new BufferedOutputStream(new DeflaterOutputStream(new FileOutputStream(file, false)));
                writeInt(os, map.size());
                map.forEachEntry(new TObjectObjectProcedure<String, String>() {

                    public boolean execute(String key, String value) {
                        try {
                            writeString(os, key);
                            writeString(os, value);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                });
                os.flush();
                os.close();
                file = new File(fDir, LOG_FILE_NAME);
                file.delete();
                if (reopenWriter) writer = new BufferedOutputStream(new FileOutputStream(file, true));
                dirty = false;
            } else if (reopenWriter) {
                File file = new File(fDir, LOG_FILE_NAME);
                writer = new BufferedOutputStream(new FileOutputStream(file, true));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void load() {
        try {
            File dat = new File(fDir, DAT_FILE_NAME);
            if (dat.exists()) {
                InputStream is = new BufferedInputStream(new InflaterInputStream(new FileInputStream(dat)));
                int size = readInt(is);
                for (int i = 0; i < size; i++) {
                    map.put(readString(is), readString(is));
                }
                is.close();
            }
            File log = new File(fDir, LOG_FILE_NAME);
            dirty = log.exists();
            if (log.exists()) {
                InputStream is = new BufferedInputStream(new FileInputStream(log));
                while (is.available() > 0) {
                    char cmd = readChar(is);
                    switch(cmd) {
                        case 'C':
                            {
                                doClear(readString(is));
                                break;
                            }
                        case 'P':
                            {
                                doPut(readString(is), readString(is));
                                break;
                            }
                        case 'R':
                            {
                                doRemove(readString(is));
                                break;
                            }
                        default:
                            System.err.println("Unknown command: " + cmd);
                    }
                }
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Snapshotter extends Thread {

        private long interval;

        public Snapshotter(long interval) {
            setDaemon(true);
            this.interval = interval;
        }

        public void run() {
            while (!closed) {
                try {
                    sleep(interval);
                } catch (InterruptedException e) {
                    return;
                }
                snapshot(true);
            }
        }
    }

    private static void writeChar(OutputStream os, char c) throws IOException {
        writeInt(os, (int) c);
    }

    private static void writeInt(OutputStream os, int i) throws IOException {
        os.write(i & 0xFF);
        os.write((i >> 8) & 0xFF);
        os.write((i >> 16) & 0xFF);
        os.write((i >> 24) & 0xFF);
    }

    private static void writeString(OutputStream os, String s) throws IOException {
        byte[] bytes = s.getBytes();
        writeInt(os, bytes.length);
        os.write(bytes);
    }

    private static char readChar(InputStream is) throws IOException {
        return (char) readInt(is);
    }

    private static int readInt(InputStream is) throws IOException {
        return is.read() | (is.read() << 8) | (is.read() << 16) | (is.read() << 24);
    }

    private static String readString(InputStream is) throws IOException {
        byte[] bytes = new byte[readInt(is)];
        is.read(bytes);
        return new String(bytes);
    }
}
