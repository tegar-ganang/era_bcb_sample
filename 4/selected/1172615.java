package com.siberhus.commons.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipUtil {

    private Logger logger = LoggerFactory.getLogger(ZipUtil.class);

    /** Constants for mode listing or mode extracting. */
    public static enum Mode {

        LIST, EXTRACT
    }

    ;

    /** Whether we are extracting or just printing TOC */
    protected Mode mode = Mode.LIST;

    /** The ZipFile that is used to read an archive */
    protected ZipFile zippy;

    /** The buffer for reading/writing the ZipFile data */
    protected byte[] b = new byte[8092];

    ;

    /** Set the Mode (list, extract). */
    protected void setMode(Mode m) {
        mode = m;
    }

    /** Cache of paths we've mkdir()ed. */
    protected SortedSet<String> dirsMade;

    /** For a given Zip file, process each entry. 
	 * @throws IOException 
	 * @throws ZipException */
    public void unzip(File zipFile) throws IOException {
        dirsMade = new TreeSet<String>();
        zippy = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> all = zippy.entries();
        while (all.hasMoreElements()) {
            getFile(all.nextElement());
        }
    }

    protected boolean warnedMkDir = false;

    /** Process one file from the zip, given its name.
	 * Either print the name, or create the file on disk.
	 */
    protected void getFile(ZipEntry e) throws IOException {
        String zipName = e.getName();
        switch(mode) {
            case EXTRACT:
                if (zipName.startsWith("/")) {
                    if (!warnedMkDir) {
                        logger.warn("Ignoring absolute paths");
                    }
                    warnedMkDir = true;
                    zipName = zipName.substring(1);
                }
                if (zipName.endsWith("/")) {
                    return;
                }
                int ix = zipName.lastIndexOf('/');
                if (ix > 0) {
                    String dirName = zipName.substring(0, ix);
                    if (!dirsMade.contains(dirName)) {
                        File d = new File(dirName);
                        if (!(d.exists() && d.isDirectory())) {
                            logger.debug("Creating Directory: " + dirName);
                            if (!d.mkdirs()) {
                                logger.warn("Warning: unable to mkdir " + dirName);
                            }
                            dirsMade.add(dirName);
                        }
                    }
                }
                logger.debug("Creating " + zipName);
                FileOutputStream os = new FileOutputStream(zipName);
                InputStream is = zippy.getInputStream(e);
                int n = 0;
                while ((n = is.read(b)) > 0) os.write(b, 0, n);
                is.close();
                os.close();
                break;
            case LIST:
                if (e.isDirectory()) {
                    logger.debug("Directory " + zipName);
                } else {
                    logger.debug("File " + zipName);
                }
                break;
            default:
                throw new IllegalStateException("mode value (" + mode + ") bad");
        }
    }

    public void zip(File inFile, File outFile) throws IOException {
        if (inFile.isDirectory()) {
            zipDirectory(inFile, outFile);
        } else {
            zipFile(inFile, outFile);
        }
    }

    /** Gzip the contents of the from file and save in the to file. */
    protected void zipFile(File from, File to) throws IOException {
        FileInputStream in = new FileInputStream(from);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(to));
        byte[] buffer = new byte[4096];
        int bytes_read;
        while ((bytes_read = in.read(buffer)) != -1) out.write(buffer, 0, bytes_read);
        in.close();
        out.close();
    }

    /** Zip the contents of the directory, and save it in the zipfile */
    protected void zipDirectory(File dir, File zipfile) throws IOException, IllegalArgumentException {
        if (!dir.isDirectory()) throw new IllegalArgumentException("Compress: not a directory:  " + dir);
        String[] entries = dir.list();
        byte[] buffer = new byte[4096];
        int bytes_read;
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
        for (int i = 0; i < entries.length; i++) {
            File f = new File(dir, entries[i]);
            if (f.isDirectory()) continue;
            FileInputStream in = new FileInputStream(f);
            ZipEntry entry = new ZipEntry(f.getPath());
            out.putNextEntry(entry);
            while ((bytes_read = in.read(buffer)) != -1) out.write(buffer, 0, bytes_read);
            in.close();
        }
        out.close();
    }
}
