package it.aton.proj.dem.transport.impl.persistent;

import it.aton.proj.dem.transport.impl.Queue;
import it.aton.proj.dem.transport.service.Message;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class PersistentQueue implements Queue {

    private static class Pointer {

        public final int[] fileSeq = new int[3];

        public int offset;

        public void reset() {
            offset = 0;
            fileSeq[0] = 0;
            fileSeq[1] = 0;
            fileSeq[2] = 0;
        }
    }

    private static class Utils {

        public static void incrementSeq(int[] seq) {
            int i = 2;
            while (i >= 0 && seq[i] == 35) {
                seq[i] = 0;
                i--;
            }
            if (i >= 0) seq[i]++;
        }

        public static void getExt(int[] seq, StringBuilder sb) {
            sb.append(SEQ[seq[0]]);
            sb.append(SEQ[seq[1]]);
            sb.append(SEQ[seq[2]]);
        }

        public static void codeShort(int val, byte[] bytes, int offset) {
            if (val > MAX_LEN) val = 0;
            bytes[offset] = (byte) (val & 0xFF);
            val >>= 8;
            bytes[offset + 1] = (byte) (val & 0xFF);
        }

        public static void codeLong(long val, byte[] bytes, int offset) {
            for (int i = 0; i < 8; i++) {
                bytes[offset++] = (byte) (val & 0xFF);
                val >>= 8;
            }
        }

        public static void codeInt(int val, byte[] bytes, int offset) {
            for (int i = 0; i < 4; i++) {
                bytes[offset++] = (byte) (val & 0xFF);
                val >>= 8;
            }
        }

        public static int decodeShort(byte[] bytes, int offset) {
            int ret = bytes[offset] & 0xFF;
            ret |= (bytes[offset + 1] & 0xFF) << 8;
            return ret;
        }

        public static long decodeLong(byte[] bytes, int offset) {
            long ret = 0;
            for (int i = 7; i >= 0; i--) {
                ret <<= 8;
                ret |= bytes[offset + i] & 0xFF;
            }
            return ret;
        }

        public static int decodeInt(byte[] bytes, int offset) {
            int ret = 0;
            for (int i = 3; i >= 0; i--) {
                ret <<= 8;
                ret |= bytes[offset + i] & 0xFF;
            }
            return ret;
        }
    }

    private static final byte MAGIC = (byte) 0xDC;

    private static final char[] SEQ = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

    private static final int HEADER_LEN = 20;

    private static final int MAX_LEN = (1 << 16) - 1;

    private static final String INDEX_FILE = "index";

    private final Semaphore EMPTY_SEMAPHORE = new Semaphore(0);

    private final Semaphore FULL_SEMAPHORE = new Semaphore(0);

    private final File dir;

    private final String name;

    private final Pointer head = new Pointer();

    private final Pointer tail = new Pointer();

    private int size;

    private int capacity;

    public PersistentQueue(String name, File dir, int capacity) throws IOException {
        this.name = name;
        this.capacity = capacity;
        if (!dir.isDirectory() && !dir.canWrite()) throw new IllegalArgumentException("Invalid directory " + dir.getAbsolutePath());
        this.dir = new File(dir, name);
        this.dir.mkdirs();
        File indexFile = new File(this.dir, INDEX_FILE);
        if (indexFile.exists()) {
            byte[] idx = new byte[14];
            FileInputStream fis = new FileInputStream(indexFile);
            fis.read(idx);
            fis.close();
            size = Utils.decodeInt(idx, 0);
            decodePointer(head, idx, 4);
            decodePointer(tail, idx, 9);
        }
        EMPTY_SEMAPHORE.release(size);
        FULL_SEMAPHORE.release(capacity - size);
    }

    public String getName() {
        return name;
    }

    private File fi;

    private FileInputStream fis;

    public Message pop(long wait) {
        try {
            if (wait < 0) EMPTY_SEMAPHORE.tryAcquire(); else EMPTY_SEMAPHORE.tryAcquire(wait, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
        if (size == 0) return null;
        byte[] domain, message;
        long ttl, tstamp;
        synchronized (this) {
            try {
                if (fis == null) {
                    fi = determinePointedFile(tail);
                    fis = new FileInputStream(fi);
                }
                byte[] header = new byte[HEADER_LEN];
                fis.read(header);
                if (header[0] != MAGIC) throw new RuntimeException("Corrupted file: no magic number");
                int len = Utils.decodeShort(header, 1);
                tstamp = Utils.decodeLong(header, 3);
                ttl = Utils.decodeLong(header, 11);
                int dLen = header[19] & 0xFF;
                domain = new byte[dLen];
                message = new byte[len - 20 - dLen];
                fis.read(domain);
                fis.read(message);
                if (fis.available() == 0) {
                    fis.close();
                    fi.delete();
                    fis = null;
                    Utils.incrementSeq(tail.fileSeq);
                    tail.offset = 0;
                } else {
                    tail.offset += len;
                }
                size--;
                if (size == 0) resetPointers(); else saveIndex();
                FULL_SEMAPHORE.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Message ret = new Message(new String(domain), new String(message), ttl);
        ret.setTimestamp(tstamp);
        return ret;
    }

    private FileOutputStream fos;

    public void push(Message m) {
        byte[] domain = m.getDomain().getBytes();
        byte[] message = m.getXMLContent().getBytes();
        if (domain.length > 255) throw new IllegalArgumentException("Domain too long (> 255).");
        int len = 20 + domain.length + message.length;
        byte[] corpus = new byte[len];
        corpus[0] = MAGIC;
        Utils.codeShort(len, corpus, 1);
        Utils.codeLong(m.getTimestamp(), corpus, 3);
        Utils.codeLong(m.getTtl(), corpus, 11);
        corpus[19] = (byte) (domain.length & 0xFF);
        System.arraycopy(domain, 0, corpus, 20, domain.length);
        System.arraycopy(message, 0, corpus, 20 + domain.length, message.length);
        synchronized (this) {
            try {
                FULL_SEMAPHORE.acquire();
            } catch (InterruptedException e) {
                return;
            }
            try {
                if (head.offset + len >= MAX_LEN) {
                    Utils.incrementSeq(head.fileSeq);
                    head.offset = 0;
                    if (fos != null) fos.close();
                    fos = null;
                }
                if (fos == null) {
                    File file = determinePointedFile(head);
                    fos = new FileOutputStream(file, true);
                }
                fos.write(corpus);
                fos.flush();
                head.offset += len;
                size++;
                saveIndex();
                EMPTY_SEMAPHORE.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private File determinePointedFile(Pointer ptr) {
        StringBuilder filename = new StringBuilder();
        filename.append(name);
        filename.append(".");
        Utils.getExt(ptr.fileSeq, filename);
        File file = new File(dir, filename.toString());
        return file;
    }

    private FileOutputStream idxFos;

    private FileChannel idxFc;

    private void saveIndex() throws IOException {
        if (idxFc == null) {
            idxFos = new FileOutputStream(new File(dir, INDEX_FILE), false);
            idxFc = idxFos.getChannel();
        }
        byte[] idx = new byte[14];
        Utils.codeInt(size, idx, 0);
        codePointer(head, idx, 4);
        codePointer(tail, idx, 9);
        idxFc.truncate(0);
        idxFos.write(idx);
        idxFos.flush();
    }

    private static void codePointer(Pointer ptr, byte[] idx, int i) {
        idx[i] = (byte) (ptr.fileSeq[0] & 0xFF);
        idx[i + 1] = (byte) (ptr.fileSeq[1] & 0xFF);
        idx[i + 2] = (byte) (ptr.fileSeq[2] & 0xFF);
        Utils.codeShort(ptr.offset, idx, i + 3);
    }

    private void decodePointer(Pointer ptr, byte[] idx, int i) {
        ptr.fileSeq[0] = idx[i];
        ptr.fileSeq[1] = idx[i + 1];
        ptr.fileSeq[2] = idx[i + 2];
        ptr.offset = Utils.decodeShort(idx, i + 3);
    }

    public synchronized int size() {
        return size;
    }

    private synchronized void resetPointers() throws IOException {
        head.reset();
        if (fos != null) {
            fos.close();
            fos = null;
        }
        tail.reset();
        if (fis != null) {
            fis.close();
            fis = null;
        }
        EMPTY_SEMAPHORE.release(size);
        FULL_SEMAPHORE.release(capacity - size);
        saveIndex();
    }

    public synchronized void clear() {
        for (File f : dir.listFiles()) f.delete();
        size = 0;
        try {
            resetPointers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (fos != null) {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fos = null;
        }
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fis = null;
        }
    }

    public Message pop() {
        return pop(-1);
    }
}
