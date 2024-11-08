package com.cirnoworks.spk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import util.Random;

/**
 * @author Cloudee
 * 
 */
public abstract class SPKWriter {

    private final long seed;

    public SPKWriter(long seed) {
        this.seed = seed;
    }

    protected interface NameFilter {

        boolean accept(String name);
    }

    protected class FileEntry {

        private final String name;

        private final byte[] nameutf8;

        private FileData data;

        /**
		 * @param name
		 * @param file
		 */
        public FileEntry(String name) throws IOException {
            super();
            this.name = name;
            this.nameutf8 = name.getBytes("utf-8");
        }

        /**
		 * @return the name
		 */
        public String getName() {
            return name;
        }

        /**
		 * @return the nameutf8
		 */
        public byte[] getNameutf8() {
            return nameutf8;
        }

        /**
		 * @return the data
		 */
        public FileData getData() {
            return data;
        }

        /**
		 * @param data
		 *            the data to set
		 */
        public void setData(FileData data) {
            this.data = data;
        }
    }

    public void writeSPK(String base, NameFilter filter, File output) throws IOException {
        checkSource(base);
        ArrayList<FileEntry> files = new ArrayList<FileEntry>();
        listFile(base, filter, files);
        int totalsize = 4;
        for (FileEntry file : files) {
            totalsize += 12 + file.getNameutf8().length;
        }
        FileOutputStream out = new FileOutputStream(output);
        for (int i = 0; i < totalsize; i++) {
            out.write(0);
        }
        byte[] buffer = new byte[16384];
        int pos = totalsize;
        int read;
        for (FileEntry file : files) {
            System.out.print("ENCO " + file.name);
            SPKOutputStream sos = new SPKOutputStream(out, seed);
            GZIPOutputStream gos = new GZIPOutputStream(sos);
            InputStream fis = getInputStream(base, file.name);
            while ((read = fis.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                gos.write(buffer, 0, read);
            }
            gos.close();
            fis.close();
            file.setData(new FileData(file.getName(), pos, sos.length()));
            pos += sos.length();
            System.out.println(" -> " + sos.length());
        }
        out.close();
        RandomAccessFile raf = new RandomAccessFile(output, "rw");
        raf.writeInt(files.size());
        for (FileEntry file : files) {
            System.out.println("META " + file.name);
            raf.writeInt(8 + file.getNameutf8().length);
            raf.writeInt(file.getData().getPos());
            raf.writeInt(file.getData().getLength());
            raf.write(file.getNameutf8());
        }
        raf.close();
        System.out.println("FINISHED");
    }

    class SPKOutputStream extends FilterOutputStream {

        private final Random random;

        private int length = 0;

        /**
		 * @param out
		 */
        public SPKOutputStream(OutputStream out, long seed) {
            super(out);
            random = new Random(seed);
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b ^ (random.nextInt() & 0xff));
            length++;
        }

        public void close() {
        }

        public int length() {
            return length;
        }
    }

    protected abstract InputStream getInputStream(String base, String name) throws IOException;

    protected abstract void checkSource(String toCheck) throws IOException;

    protected abstract void listFile(String base, NameFilter filter, List<FileEntry> files) throws IOException;
}
