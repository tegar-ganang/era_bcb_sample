package com.citizenhawk.jziplib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {

    public static void main(String[] args) throws IOException {
        testFile();
        testDir();
    }

    private static void testFile() throws IOException {
        File file = new File("license/license.txt");
        File output = new File("license.zip");
        ZipUtil.createZip(file, output);
    }

    private static void testDir() throws IOException {
        File dir = new File("license");
        File output = new File("license.zip");
        createZip(dir, output);
    }

    private ZipUtil() {
    }

    /**
     * Create the zipfile and add the directory to it.
     *
     * @param input   Either a directory or a file.  If a directory is used, the directory and everything
     *                under it is included.  If a file is used, only that file is included in the zip.
     * @param zipfile the name of the zip file to create.
     * @throws java.io.IOException      if creation of file fails.
     * @throws IllegalArgumentException if the input file does not exist
     * @throws NullPointerException     if the input or zipfile are null
     */
    public static void createZip(File input, File zipfile) throws IOException {
        validate(input, zipfile);
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipfile));
            zos.setMethod(ZipOutputStream.DEFLATED);
            zos.setLevel(9);
            addDirOrFile(input, zos);
        } finally {
            if (zos != null) zos.close();
        }
    }

    private static void addDirOrFile(File input, ZipOutputStream zos) throws IOException {
        if (input.isDirectory()) {
            addDir(input.getName() + File.separatorChar, input, zos);
        } else if (input.isFile()) {
            addFile("", input, zos);
        }
    }

    static void addDirOrFile(String path, File input, ZipOutputStream zos) throws IOException {
        if (!path.endsWith(File.separator) && !"".equals(path)) {
            path += File.separator;
        }
        if (input.isDirectory()) {
            addDir(path, input, zos);
        } else if (input.isFile()) {
            addFile(path, input, zos);
        }
    }

    private static void validate(File input, File zipfile) {
        Validator.notNull("input", input);
        Validator.notNull("zipfile", zipfile);
        Validator.exists("input", input);
    }

    /**
     * Add a directory to a zipfile.
     *
     * @param path the path to prepend to the files in this directory.
     * @param dir  the directory to add.
     * @param zos  the zipstream we are writing to.
     * @throws IOException if writing to the zipfile fails.
     */
    static void addDir(String path, File dir, ZipOutputStream zos) throws IOException {
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            File f = new File(dir, files[i]);
            if (f.isFile()) addFile(path, f, zos); else if (f.isDirectory()) addDir(path + f.getName() + File.separatorChar, f, zos);
        }
    }

    /**
     * Add a file to a zipfile.
     *
     * @param path the path to prepend to the filename.
     * @param f    the File to add to the zip.
     * @param zos  the zipstream we are writing to.
     * @throws IOException if writing to the zipfile fails.
     */
    static void addFile(String path, File f, ZipOutputStream zos) throws IOException {
        ZipEntry ze = new ZipEntry(path + f.getName());
        ze.setSize(f.length());
        ze.setTime(f.lastModified());
        zos.putNextEntry(ze);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            byte buf[] = new byte[1024];
            int read;
            while ((read = fis.read(buf)) >= 0) {
                zos.write(buf, 0, read);
            }
        } finally {
            if (fis != null) fis.close();
        }
        zos.flush();
        zos.closeEntry();
    }
}
