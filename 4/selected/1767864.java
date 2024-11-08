package com.xith3d.io;

import com.xith3d.utility.logs.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.zip.*;

/**
 * An archive is a flexible and high performance storage system for Scribable
 * objects.  Objects can be stored and retrieved by name.  The archive can grow
 * as needed.  Objects can be deleted and/or replaced. You can also get an iterator
 * to step through the objects sequentially.  Each object can be stored compressed
 * or uncompressed.
 *
 * The file is organized as follows:
 * The first entry in the file is a directory block.
 *
 * Each directory block is 20,000 bytes.  We cram as many DirEntry as possible into
 * each 20k chunk. Assuming around an average of 30 bytes per entry this is about
 * 600 entries per block.
 *
 * Each free block contains two longs, one is the length of the block and the other
 * is a link to the next free block.
 *
 * Copyright:  Copyright (c) 2000,2001
 * Company:    Teseract Software, LLP
 * @author David Yazel
 *
 */
public class Archive implements Comparator {

    private static boolean debug = false;

    private static boolean usemap = true;

    private static final long clean = 876236278;

    private static final int blockSize = 20000;

    private static final int bufferSize = 100000;

    private static final long longSize = 8;

    private static final long cleanLoc = 0;

    private static final long dirStartLoc = cleanLoc + longSize;

    private static final long freeStartLoc = dirStartLoc + longSize;

    private static final long expandStartLoc = freeStartLoc + longSize;

    private static final long initialExpandLoc = expandStartLoc + longSize;

    RandomAccessFile file;

    TreeMap index = new TreeMap(this);

    ArrayList blocks = new ArrayList();

    LinkedList free = new LinkedList();

    long freelistLoc = 0;

    long dirlistLoc = 0;

    long expandLoc;

    boolean readOnly;

    MappedByteBuffer bb;

    /**
     * Creates an Archive object.
     * @param filename The name of the file
     * @param readonly  True if the file should not be written to
     * @throws IOException
     */
    public Archive(String filename, boolean readonly) throws IOException {
        this.readOnly = readonly;
        File f = new File(filename);
        if (!f.exists()) {
            if (readonly) {
                throw new IOException("No such file " + filename);
            }
            file = new RandomAccessFile(filename, "rw");
            initializeFile();
        } else if (readonly && usemap) {
            FileInputStream fs = new FileInputStream(f);
            FileChannel fc = fs.getChannel();
            bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        } else {
            file = new RandomAccessFile(filename, readonly ? "r" : "rw");
        }
        if (readOnly && usemap) {
            readHeaderViaMap();
        } else {
            readHeader();
        }
        if (!readOnly) {
            readFreeList();
        }
        if (readOnly && usemap) {
            readDirectoryViaMap();
        } else {
            readDirectory();
        }
    }

    public boolean exists(String name) {
        DirEntry d = (DirEntry) index.get(name);
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Archive search for " + name + " : " + (d != null));
        }
        return (d != null);
    }

    /**
     * Writes the directory, free list and closes the file.
     * @throws IOException
     */
    public void close() throws IOException {
        if (!readOnly) {
            writeDirectory();
            writeFreeList();
            file.seek(expandStartLoc);
            file.writeLong(expandLoc);
            file.seek(cleanLoc);
            file.writeLong(clean);
        }
        file.close();
    }

    /**
     * Reads in the header information
     */
    private void readHeader() throws IOException {
        file.seek(cleanLoc);
        long value = file.readLong();
        if (value != clean) {
            throw new Error("Archive is corrupt");
        }
        if (!readOnly) {
            file.seek(cleanLoc);
            file.writeLong(0);
        }
        file.seek(dirStartLoc);
        dirlistLoc = file.readLong();
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Directory root = " + dirlistLoc);
        }
        file.seek(freeStartLoc);
        freelistLoc = file.readLong();
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Free root = " + freelistLoc);
        }
        file.seek(expandStartLoc);
        expandLoc = file.readLong();
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Expand loc = " + expandLoc);
        }
    }

    private void readHeaderViaMap() throws IOException {
        bb.position((int) cleanLoc);
        long value = bb.getLong();
        if (value != clean) {
            throw new Error("Archive is corrupt");
        }
        bb.position((int) dirStartLoc);
        dirlistLoc = bb.getLong();
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Directory root = " + dirlistLoc);
        }
        bb.position((int) freeStartLoc);
        freelistLoc = bb.getLong();
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Free root = " + freelistLoc);
        }
        bb.position((int) expandStartLoc);
        expandLoc = bb.getLong();
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Expand loc = " + expandLoc);
        }
    }

    /**
     * Allocates the specified amount of archive space specified
     * @param size The number of bytes to allocate
     * @throws IOException
     */
    private long allocateSpace(long size) {
        Block minBlock = null;
        Iterator i = free.iterator();
        while (i.hasNext()) {
            Block b = (Block) i.next();
            if (b.length == size) {
                free.remove(b);
                return b.loc;
            } else if (b.length > size) {
                if (minBlock == null) {
                    minBlock = b;
                } else if (minBlock.length > size) {
                    minBlock = b;
                }
            }
        }
        if (minBlock != null) {
            if (minBlock.length < (size * 1.2)) {
                free.remove(minBlock);
                return minBlock.loc;
            }
        }
        long loc = expandLoc;
        expandLoc += size;
        return loc;
    }

    /**
     * Initializes the file.  This creates a single emptry directory block at the
     * beginning of the file.
     */
    private void initializeFile() throws IOException {
        file.setLength(0);
        file.seek(dirStartLoc);
        file.writeLong(0);
        file.seek(freeStartLoc);
        file.writeLong(0);
        file.seek(expandStartLoc);
        file.writeLong(initialExpandLoc);
        file.seek(cleanLoc);
        file.writeLong(clean);
    }

    /**
     * Writes out the free list to the file.  The free list is a linked list
     * of free blocks
     */
    private void writeFreeList() throws IOException {
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Writing free list");
        }
        if (free.size() == 0) {
            file.seek(freeStartLoc);
            file.writeLong(0);
        } else {
            Block first = (Block) free.getFirst();
            file.seek(freeStartLoc);
            file.writeLong(first.loc);
            Object[] b = (Object[]) free.toArray();
            for (int i = 0; i < b.length; i++) {
                Block bcur = (Block) b[i];
                Block next = null;
                if (i < (b.length - 1)) {
                    next = (Block) b[i + 1];
                }
                file.seek(bcur.loc);
                file.writeLong(bcur.length);
                if (next == null) {
                    file.writeLong(0);
                } else {
                    file.writeLong(next.loc);
                }
                if (debug) {
                    Log.log.println(LogType.EXHAUSTIVE, "   bfree block pos=" + bcur.loc + ", len = " + bcur.length);
                }
            }
        }
    }

    /**
     * reads the free list into memory.  These represent blocks in the file
     * which are available for reuse.
     */
    private void readFreeList() throws IOException {
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Reading free list");
        }
        free.clear();
        long loc = freelistLoc;
        while (loc != 0) {
            file.seek(loc);
            Block b = new Block();
            b.length = file.readLong();
            b.loc = loc;
            loc = file.readLong();
            free.add(b);
            if (debug) {
                Log.log.println(LogType.EXHAUSTIVE, "   bfree block pos=" + b.loc + ", len = " + b.length + ", next=" + loc);
            }
        }
    }

    /**
     * Writes out the directory to the file.  The directory is written out
     * in 20k chunks so that many directory entries can be written
     *
     * @throws IOException
     */
    private void writeDirectory() throws IOException {
        long pad = 10;
        if (index.size() == 0) {
            file.seek(dirStartLoc);
            file.writeLong(0);
        } else {
            long loc = allocateSpace(blockSize);
            file.seek(dirStartLoc);
            file.writeLong(loc);
            long stopLoc = loc + blockSize;
            file.seek(loc);
            file.writeLong(blockSize);
            Collection c = index.values();
            Iterator i = c.iterator();
            while (i.hasNext()) {
                DirEntry d = (DirEntry) i.next();
                if ((d.estimateSize() + loc + pad) > stopLoc) {
                    loc = allocateSpace(blockSize);
                    file.writeByte(0);
                    file.writeLong(loc);
                    file.seek(loc);
                    file.writeLong(blockSize);
                    stopLoc = loc + blockSize;
                }
                file.writeByte(1);
                d.write();
                loc = file.getFilePointer();
                if (loc >= stopLoc) {
                    throw new Error("Writing directory exceeded block size");
                }
            }
            file.writeByte(0);
            file.writeLong(0);
        }
    }

    /**
     * Reads in the directory.  This will also build a list of directory blocks
     * for using when we write out the directory.
     */
    private void readDirectory() throws IOException {
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Reading directory");
        }
        int num = 0;
        index.clear();
        long loc = dirlistLoc;
        while (loc != 0) {
            file.seek(loc);
            Block b = new Block();
            b.length = file.readLong();
            b.loc = loc;
            free.add(b);
            num++;
            byte marker = file.readByte();
            while (marker == 1) {
                DirEntry d = new DirEntry();
                d.read();
                marker = file.readByte();
                index.put(d.name, d);
            }
            loc = file.readLong();
        }
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "  Found " + index.size() + " items in " + num + " block");
        }
    }

    private void readDirectoryViaMap() throws IOException {
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Reading directory");
        }
        int num = 0;
        index.clear();
        long loc = dirlistLoc;
        while (loc != 0) {
            bb.position((int) loc);
            Block b = new Block();
            b.length = bb.getLong();
            b.loc = loc;
            free.add(b);
            num++;
            byte marker = bb.get();
            while (marker == 1) {
                DirEntry d = new DirEntry();
                d.readViaMap();
                marker = bb.get();
                index.put(d.name, d);
            }
            loc = bb.getLong();
        }
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "  Found " + index.size() + " items in " + num + " block");
        }
    }

    /**
     * Compares two keys together
     */
    public int compare(Object a, Object b) {
        return ((String) a).compareTo((String) b);
    }

    /**
     * Writes out the object to the repository.
     * @param object
     */
    public void write(String name, Scribable object, boolean compress) throws IOException, UnscribableNodeEncountered {
        DirEntry d = (DirEntry) index.get(name);
        if (d != null) {
            Block b = new Block();
            b.length = d.length;
            b.loc = d.pos;
            free.add(b);
        } else {
            d = new DirEntry();
            d.name = name;
            index.put(name, d);
        }
        d.compressed = compress;
        ZipOutputStream zip = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream(10000);
        OutputStream out = bout;
        if (compress) {
            zip = new ZipOutputStream(out);
            zip.putNextEntry(new ZipEntry("object"));
            out = zip;
        }
        ScribeOutputStream sout = new ScribeOutputStream(out);
        if (object == null) {
            System.out.println("Object is null");
        }
        sout.writeScribable(object);
        if (compress) {
            zip.closeEntry();
        }
        out.close();
        byte[] data = bout.toByteArray();
        d.pos = allocateSpace(data.length);
        d.length = data.length;
        file.seek(d.pos);
        file.write(data, 0, data.length);
        if (debug) {
            Log.log.println(LogType.EXHAUSTIVE, "Wrote out " + d.length + " bytes for " + name);
        }
    }

    /**
     * Writes out the object to the repository.
     * @param object
     */
    public Scribable read(String name) throws IOException, InvalidFormat {
        DirEntry d = (DirEntry) index.get(name);
        if (d == null) {
            return null;
        }
        byte[] data = new byte[(int) d.length];
        if (readOnly && usemap) {
            bb.position((int) d.pos);
            bb.get(data);
        } else {
            file.seek(d.pos);
            file.read(data, 0, data.length);
        }
        ZipInputStream zip = null;
        ByteArrayInputStream bin = new ByteArrayInputStream(data);
        InputStream in = bin;
        if (d.compressed) {
            zip = new ZipInputStream(in);
            zip.getNextEntry();
            in = zip;
        }
        ScribeInputStream sin = new ScribeInputStream(in);
        Scribable object = sin.readScribable();
        in.close();
        return object;
    }

    /**
     * Removes an entry from the repository
     * @param name
     */
    public void remove(String name) {
        DirEntry d = (DirEntry) index.get(name);
        if (d != null) {
            Block b = new Block();
            b.length = d.length;
            b.loc = d.pos;
            free.add(b);
            index.remove(d.name);
        }
    }

    public static void main(String[] args) {
        try {
            Log.log.registerLog(new ConsoleLog(LogType.ALL));
            System.out.println("Test 1");
            Archive a = new Archive("c:/test.carc", false);
            a.close();
            System.out.println("Test 2");
            a = new Archive("c:/test.carc", false);
            a.close();
            System.out.println("Test 3");
            a = new Archive("c:/test.carc", false);
            a.remove("test brush");
            a.remove("test1 brush");
            a.remove("test2 brush");
            a.remove("test3 brush");
            a.remove("test4 brush");
            a.close();
            System.out.println("Test 4");
            a = new Archive("c:/test.carc", false);
            a.close();
            System.out.println("Test 5");
            a = new Archive("c:/test.carc", false);
            a.close();
            System.out.println("Test 6");
            a = new Archive("c:/test.carc", false);
            a.close();
            System.out.println("Test 7");
            a = new Archive("c:/test.carc", false);
            a.write("test scribe 1", new TestScribable(15000), false);
            a.write("test scribe 2", new TestScribable(5000), false);
            a.write("test scribe 3", new TestScribable(5555), false);
            a.close();
            System.out.println("Test 8");
            a = new Archive("c:/test.carc", true);
            a.read("test scribe 1");
            a.read("test scribe 2");
            System.out.println("Checksum = " + ((TestScribable) a.read("test scribe 3")).checksum);
            a.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readUTF() throws IOException {
        int utflen = bb.getShort();
        StringBuffer str = new StringBuffer(utflen);
        byte[] bytearr = new byte[utflen];
        int c;
        int char2;
        int char3;
        int count = 0;
        bb.get(bytearr, 0, utflen);
        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch(c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    count++;
                    str.append((char) c);
                    break;
                case 12:
                case 13:
                    count += 2;
                    if (count > utflen) {
                        throw new UTFDataFormatException();
                    }
                    char2 = (int) bytearr[count - 1];
                    if ((char2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    str.append((char) (((c & 0x1F) << 6) | (char2 & 0x3F)));
                    break;
                case 14:
                    count += 3;
                    if (count > utflen) {
                        throw new UTFDataFormatException();
                    }
                    char2 = (int) bytearr[count - 2];
                    char3 = (int) bytearr[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) {
                        throw new UTFDataFormatException();
                    }
                    str.append((char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0)));
                    break;
                default:
                    throw new UTFDataFormatException();
            }
        }
        return new String(str);
    }

    /**
     * Defines a single directory entry in the file.
     */
    class DirEntry {

        long pos;

        String name;

        long length;

        boolean compressed;

        /**
         * Reads in a directory entry at the current location in the
         * random access file.
         * @throws IOException
         */
        void read() throws IOException {
            pos = file.readLong();
            name = file.readUTF();
            length = file.readLong();
            compressed = file.readBoolean();
        }

        void readViaMap() throws IOException {
            pos = bb.getLong();
            name = readUTF();
            length = bb.getLong();
            compressed = (bb.get() == 0) ? false : true;
        }

        /**
         * This method writes out the directory entry to the current location in the
         * random access file.  It is up to the drectory writer to put the header
         * byte indicating that there is a directory entry.
         *
         * @throws IOException
         */
        void write() throws IOException {
            file.writeLong(pos);
            file.writeUTF(name);
            file.writeLong(length);
            file.writeBoolean(compressed);
        }

        /**
         *
         * @return Returns the number of bytes needed to write this directory
         * entry out to the file.
         */
        long estimateSize() {
            return name.length() + 4 + 25;
        }
    }

    class Block {

        long loc;

        long length;
    }
}
