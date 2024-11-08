package net.juantxu.pentaho.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.log4j.Logger;

/**
 * UnZip -- print or unzip a JAR or PKZIP file using java.util.zip. Command-line
 * version: extracts files.
 * 
 * @author Ian Darwin, Ian@DarwinSys.com $Id: UnZip.java,v 1.7 2004/03/07
 *         17:40:35 ian Exp $
 */
public class UnZip {

    private static final long serialVersionUID = 1L;

    static Logger log = Logger.getLogger(DescargaEInstalaPentahoApp.class);

    /** Constants for mode listing or mode extracting. */
    public static final int LIST = 0, EXTRACT = 1;

    /** Whether we are extracting or just printing TOC */
    protected int mode = EXTRACT;

    /** The ZipFile that is used to read an archive */
    protected ZipFile zippy;

    /** The buffer for reading/writing the ZipFile data */
    protected byte[] b;

    /**
   * Simple main program, construct an UnZipper, process each .ZIP file from
   * argv[] through that object.

  /** Construct an UnZip object. Just allocate the buffer */
    UnZip() {
        b = new byte[8092];
    }

    /** Set the Mode (list, extract). */
    protected void setMode(int m) {
        if (m == LIST || m == EXTRACT) mode = m;
    }

    /** Cache of paths we've mkdir()ed. */
    protected SortedSet dirsMade;

    /** For a given Zip file, process each entry. */
    public void unZip(String fileName) {
        dirsMade = new TreeSet();
        try {
            zippy = new ZipFile(fileName);
            Enumeration all = zippy.entries();
            while (all.hasMoreElements()) {
                getFile((ZipEntry) all.nextElement());
            }
        } catch (IOException err) {
            System.err.println("IO Error: " + err);
            return;
        }
    }

    protected boolean warnedMkDir = false;

    /**
   * Process one file from the zip, given its name. Either print the name, or
   * create the file on disk.
   */
    protected void getFile(ZipEntry e) throws IOException {
        String zipName = e.getName();
        switch(mode) {
            case EXTRACT:
                if (zipName.startsWith("/")) {
                    if (!warnedMkDir) System.out.println("Ignoring absolute paths");
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
                            log.debug("Creating Directory: " + dirName);
                            System.out.println();
                            if (!d.mkdirs()) {
                                log.error("Warning: unable to mkdir " + dirName);
                            }
                            dirsMade.add(dirName);
                        }
                    }
                }
                log.debug("Creating " + zipName);
                FileOutputStream os = new FileOutputStream(zipName);
                InputStream is = zippy.getInputStream(e);
                int n = 0;
                while ((n = is.read(b)) > 0) os.write(b, 0, n);
                is.close();
                os.close();
                break;
            case LIST:
                if (e.isDirectory()) {
                    log.debug("Directory " + zipName);
                } else {
                    log.debug("File " + zipName);
                }
                break;
            default:
                throw new IllegalStateException("mode value (" + mode + ") bad");
        }
    }
}
