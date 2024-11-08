package net.sf.refactorit.common.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Patch {

    private byte[] sought;

    private byte[] replacement;

    private String fileName;

    private File file;

    private boolean matches(MappedByteBuffer bb, int pos) {
        for (int j = 0; j < sought.length; ++j) {
            if (sought[j] != bb.get(pos + j)) {
                return false;
            }
        }
        return true;
    }

    private void replace(MappedByteBuffer bb, int pos) {
        for (int j = 0; j < sought.length; ++j) {
            byte b = (j < replacement.length) ? replacement[j] : (byte) ' ';
            bb.put(pos + j, b);
        }
    }

    private int searchAndReplace(MappedByteBuffer bb, int sz) {
        int replacementsCount = 0;
        for (int pos = 0; pos <= sz - sought.length; ++pos) {
            if (matches(bb, pos)) {
                replace(bb, pos);
                pos += sought.length - 1;
                ++replacementsCount;
            }
        }
        return replacementsCount;
    }

    private void run(File f) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        FileChannel fc = raf.getChannel();
        int sz = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_WRITE, 0, sz);
        searchAndReplace(bb, sz);
        bb.force();
        raf.close();
    }

    public void replace(String sought, String replacement) throws IOException {
        this.sought = sought.getBytes();
        this.replacement = replacement.getBytes();
        if (this.sought.length != this.replacement.length) {
            throw new IllegalArgumentException("Sought string size shall be equals to replacement string size!");
        }
        run(getFile());
    }

    public Patch(String fileName) {
        this.fileName = fileName;
    }

    public Patch(File file) {
        this.file = file;
    }

    private File getFile() {
        if (file == null) {
            file = new File(fileName);
        }
        return file;
    }
}
