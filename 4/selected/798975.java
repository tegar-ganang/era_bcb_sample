package org.ala.spatial.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.ala.layers.intersect.Grid;

/**
 * Zipper helper class to zip up files.
 * Quick zipper helper class
 *
 * @author ajay
 */
public class Zipper {

    /**
     * zipFile method for zipping a single file. Provide an zip filename.
     *
     * @param infile Input file to be zipped
     * @param outfile Output zipped filename
     */
    public static void zipFile(String infile, String outfile) {
        String[] infiles = { infile };
        zipFiles(infiles, outfile);
    }

    /**
     * zipFile method for zipping a single file. Output filename generated
     * based on the input file
     *
     * @param infile Input file to be zipped
     * @return Output zipped filename
     */
    public static String zipFile(String infile) {
        String outfile = infile + ".zip";
        String[] infiles = { infile };
        zipFiles(infiles, outfile);
        return outfile;
    }

    /**
     * zipFiles method to zip a bunch of files. Output filename to be provided.
     *
     * @param infiles Input files to be zipped
     * @param filenames Human-readable filenames
     * @param outfile Output zipped filename
     */
    public static void zipFiles(String[] infiles, String[] filenames, String outfile) {
        byte[] buf = new byte[1024];
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outfile));
            for (int i = 0; i < infiles.length; i++) {
                File f = new File(infiles[i]);
                FileInputStream in = new FileInputStream(f);
                String fname = f.getName();
                if (filenames != null) {
                    if (filenames.length == infiles.length) {
                        if (!filenames[i].trim().equalsIgnoreCase("")) {
                            fname = filenames[i];
                        }
                    }
                }
                out.putNextEntry(new ZipEntry(fname));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (IOException e) {
        }
    }

    public static void zipDirectory(String dirpath, String outpath) {
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outpath));
            zipDir(dirpath, zos, dirpath);
            zos.close();
        } catch (Exception e) {
        }
    }

    private static void zipDir(String dir2zip, ZipOutputStream zos, String parentDir) {
        try {
            File zipDir = new File(dir2zip);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    zipDir(filePath, zos, parentDir);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String fileToAdd = f.getAbsolutePath().substring(parentDir.length() + 1);
                ZipEntry anEntry = new ZipEntry(fileToAdd);
                System.out.println("adding: " + anEntry.getName());
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
        }
    }

    /**
     * zipFiles method to zip a bunch of files. Output filename to be provided.
     *
     * @param infiles Input files to be zipped
     * @param outfile Output zipped filename
     */
    public static void zipFiles(String[] infiles, String outfile) {
        zipFiles(infiles, null, outfile);
    }

    /**
     * Unzip a file
     *
     * @param name Name of the file without the path
     * @param data InputStream data
     * @param basepath String path where the file will be unzipped to
     * @param createDirectory If the unzipped file should be unzipped into it's own folder or the base folder
     * @return
     */
    public static boolean unzipFile(String name, InputStream data, String basepath, boolean createDirectory) {
        try {
            String outputpath = basepath;
            if (createDirectory) {
                String zipfilename = name.substring(0, name.lastIndexOf("."));
                outputpath += zipfilename + "/";
                File outputDir = new File(outputpath);
                outputDir.mkdirs();
            }
            ZipInputStream zis = new ZipInputStream(data);
            ZipEntry ze = null;
            String shpfile = "";
            String type = "";
            while ((ze = zis.getNextEntry()) != null) {
                System.out.println("ze.file: " + ze.getName());
                String fname = outputpath + ze.getName();
                copyInputStream(zis, new BufferedOutputStream(new FileOutputStream(fname)));
                zis.closeEntry();
            }
            zis.close();
        } catch (Exception e) {
            System.out.println("unable to load user kml: ");
            e.printStackTrace(System.out);
            return false;
        }
        return true;
    }

    public static void cleanUpZip(String zipfile, String[] deleteFiles, String[] renameFile) {
    }

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException, Exception {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) > -1) {
            out.write(buffer, 0, len);
        }
        out.close();
    }

    /**
     * Run as a separate class
     *
     * @param args command line or method inputs
     */
    public static void main(String[] args) {
        Grid g = new Grid("/Users/ajay/projects/data/modelling/erosivity");
        System.out.println("rows: " + g.nrows);
        System.out.println("cols: " + g.ncols);
        System.out.println("v: " + g.minval);
        System.out.println("v: " + g.maxval);
        System.out.println("size: " + g.xres + " x " + g.yres);
        float[] dv = g.getGrid();
        System.out.println("values:\n" + dv.length);
        for (int i = 0; i < dv.length; i++) {
            System.out.println(dv[i]);
        }
        System.out.println("Static methods available. ");
    }
}
