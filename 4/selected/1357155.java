package com.objectwave.utility;

import java.util.zip.*;
import java.io.*;
import java.util.*;

/**
 * A helper class that assists in the manipulation of Jar files.
 * A typical usage
 * <CODE>
    JarReader rdr = new JarReader(fileName);
    byte [] result = rdr.getResource("com/objectwave/classFile/AnImage.gif");
 * </CODE>
 * @author Dave Hoag
 */
public class JarReader {

    Hashtable contents;

    File file;

    class ZipEnumeration implements Enumeration {

        Object next = initValue();

        FileInputStream stream;

        BufferedInputStream buff;

        ZipInputStream zipper;

        byte[] data;

        Object initValue() {
            try {
                stream = new FileInputStream(file);
                buff = new BufferedInputStream(stream);
                zipper = new ZipInputStream(buff);
                return zipper.getNextEntry();
            } catch (Exception e) {
                System.out.println("Exception initializing enumeration " + e);
            }
            return null;
        }

        public boolean hasMoreElements() {
            return next != null;
        }

        public byte[] getData() {
            return data;
        }

        public ZipEntry skipZipEntry(int value) {
            try {
                for (int i = 0; i < value; i++) next = zipper.getNextEntry();
                return (ZipEntry) nextElement();
            } catch (Exception e) {
                next = null;
            }
            return null;
        }

        /**
         *
         */
        public Object nextElement() {
            Object result = next;
            data = new byte[1024];
            int read = -1;
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                do {
                    read = zipper.read(data, 0, 1024);
                    if (read > -1) bos.write(data, 0, read);
                } while (read > -1);
                data = bos.toByteArray();
                next = zipper.getNextEntry();
            } catch (Exception e) {
                System.out.println("Exception reading next entry " + e);
                e.printStackTrace();
                next = null;
            }
            return result;
        }

        public void finalize() {
            try {
                zipper.close();
                buff.close();
                stream.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	* Some basic read like operations for a jar.
	*/
    public JarReader(String fileName) throws IOException {
        file = new File(fileName);
        if (!file.exists()) throw new IOException("File doesn't exist. " + fileName);
    }

    public synchronized boolean contains(String fileName) {
        String sanatizedName = sanatizeName(fileName);
        if (contents != null) return contents.containsKey(sanatizedName);
        contents = new Hashtable();
        ZipEnumeration e = (ZipEnumeration) this.elements();
        int count = 0;
        boolean result = false;
        while (e.hasMoreElements()) {
            ZipEntry ent = (ZipEntry) e.nextElement();
            String entryName = sanatizeName(ent.getName());
            contents.put(entryName, new Integer(count++));
            if (entryName.equals(sanatizedName)) result = true;
        }
        return result;
    }

    public Enumeration elements() {
        try {
            return new ZipEnumeration();
        } catch (Exception e) {
            return new Vector().elements();
        }
    }

    /**
     * The parameter should be sanatized before calling this method.
     * @see #sanatizeName
     */
    byte[] fastGetResource(String name) {
        Integer count = (Integer) contents.get(name);
        if (count == null) return null;
        int cnt = count.intValue();
        ZipEnumeration e = (ZipEnumeration) elements();
        e.skipZipEntry(cnt);
        return e.getData();
    }

    /**
	*/
    public byte[] getData(Enumeration e) {
        if (e instanceof ZipEnumeration) return ((ZipEnumeration) e).getData();
        return null;
    }

    public String getName() {
        return file.getName();
    }

    /**
     * Get the byte [] from the jar file associated with the file name.
     */
    public byte[] getResource(String name) {
        String sanatizedName = sanatizeName(name);
        if (contents != null) return fastGetResource(sanatizedName);
        Enumeration e = elements();
        while (e.hasMoreElements()) {
            ZipEntry ent = (ZipEntry) e.nextElement();
            String entryName = sanatizeName(ent.getName());
            if (entryName.equals(sanatizedName)) {
                return ((ZipEnumeration) e).getData();
            }
        }
        return null;
    }

    /**
     * Jars will store file paths as '/' or '\' depending upon the system the jar file was
     * built. This can screw up String.equals() results.
     * Convert the '\' to '/'.
     */
    String sanatizeName(String fileName) {
        return fileName.replace('\\', '/');
    }

    public static void main(String[] args) {
        try {
            System.out.println("Reading " + args[0]);
            JarReader rdr = new JarReader(args[0]);
            Enumeration e = rdr.elements();
            while (e.hasMoreElements()) {
                ZipEntry ent = (ZipEntry) e.nextElement();
                System.out.println(" " + ent.getName() + " " + ent.getSize() + " " + ent.getComment());
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
