package oxygen.tool;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Recurses a directory, zipping and then deleting the files which 
 * are older than
 * a certain amount of time, or all the empty directories, as it
 * traverses the tree.
 * 
 * @author  Ugorji Dick-Nwoke ugorji.dick-nwoke@bea.com
 * @version 1.0, Mar 3, 2001
 */
public class DirZip {

    private static String HELP_MESSAGE = null;

    public File dir = new File(".");

    public long diff = Long.MAX_VALUE;

    public boolean debug = false;

    public boolean doZip = false;

    public boolean doDelete = false;

    private final long currtime = System.currentTimeMillis();

    private final Date logdate = new Date();

    private final SimpleDateFormat logdateFmt = new SimpleDateFormat("HH:mm:ss:SSS");

    public static final String ALL_FILES_NAME = "allfiles.zip";

    public static final FileFilter FILE_ONLY_FILE_FILTER;

    public static final FileFilter DIR_ONLY_FILE_FILTER;

    static {
        FILE_ONLY_FILE_FILTER = new FileFilter() {

            public boolean accept(File f) {
                if (f.isFile()) {
                    if (f.getName().equals(ALL_FILES_NAME)) return false;
                    if (f.getName().endsWith(".zip")) return false;
                    return true;
                }
                return false;
            }
        };
    }

    static {
        DIR_ONLY_FILE_FILTER = new FileFilter() {

            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                return false;
            }
        };
    }

    static {
        String lsep = System.getProperty("line.separator");
        HELP_MESSAGE = "Usage: " + lsep + "java [-Ddebug=true/false] [-Dzip=true/false] " + "[-Ddelete=true/false] DirZip <dir> <timediff> " + lsep + "  timediff format: 1ms, 1s, 1m, 1h, 1d";
    }

    public void run() throws Exception {
        if (!(dir.exists() && dir.isDirectory())) {
            throw new Exception("The directory specified either does not exist " + "or is not a directory: " + dir.getAbsolutePath());
        }
        log("        dir: " + dir.getAbsolutePath());
        log("  time-diff: " + diff);
        recurse(dir);
    }

    private File[] zipDir(File file) throws Exception {
        if (!file.isDirectory()) return null;
        File[] fa = null;
        long lastModifiedTime = file.lastModified();
        boolean isOld = ((currtime - lastModifiedTime) > diff);
        if (isOld) {
            fa = file.listFiles(FILE_ONLY_FILE_FILTER);
            if (fa == null || fa.length == 0) fa = null;
        }
        return fa;
    }

    private void zipFiles(File file, File[] fa) throws Exception {
        File f = new File(file, ALL_FILES_NAME);
        if (f.exists()) {
            f.delete();
            f = new File(file, ALL_FILES_NAME);
        }
        ZipOutputStream zoutstrm = new ZipOutputStream(new FileOutputStream(f));
        for (int i = 0; i < fa.length; i++) {
            ZipEntry zipEntry = new ZipEntry(fa[i].getName());
            zoutstrm.putNextEntry(zipEntry);
            FileInputStream fr = new FileInputStream(fa[i]);
            byte[] buffer = new byte[1024];
            int readCount = 0;
            while ((readCount = fr.read(buffer)) > 0) {
                zoutstrm.write(buffer, 0, readCount);
            }
            fr.close();
            zoutstrm.closeEntry();
        }
        zoutstrm.close();
        log("created zip file: " + file.getName() + "/" + ALL_FILES_NAME);
    }

    private void deleteFiles(File[] fa) throws Exception {
        for (int i = 0; i < fa.length; i++) {
            log("deleting file: " + fa[i]);
            fa[i].delete();
        }
    }

    public void recurse(File file) throws Exception {
        File[] fa = zipDir(file);
        if (fa != null) {
            if (doZip) zipFiles(file, fa);
            if (doDelete) deleteFiles(fa);
            log(fa.length + " files zipped/deleted in " + file.getAbsolutePath());
        }
        File[] fad = file.listFiles(DIR_ONLY_FILE_FILTER);
        if (fad == null || fad.length == 0) return;
        for (int i = 0; i < fad.length; i++) {
            recurse(fad[i]);
        }
    }

    /**
   * Logs a message to the PrintWriter specified
   * If msg is a throwable, it prints the stack trace to the printwriter
   */
    public void log(Object msg, PrintWriter pw) {
        if (!debug) return;
        logdate.setTime(System.currentTimeMillis());
        pw.println("[" + logdateFmt.format(logdate) + "] " + msg);
        if (msg instanceof Throwable) {
            Throwable exc = (Throwable) msg;
            pw.print(" --> ");
            exc.printStackTrace(pw);
        }
        pw.flush();
    }

    /**
   * Logs a message to the PrintStream specified
   * If msg is a throwable, it also prints the stack trace to the PrintStream
   */
    public void log(Object msg, PrintStream ps) {
        if (!debug) return;
        PrintWriter pw = new PrintWriter(ps);
        log(msg, pw);
    }

    /**
   * Same as calling log (msg, System.out);
   */
    public void log(Object msg) {
        if (!debug) return;
        log(msg, System.out);
    }

    /** 
   * Parse command line parameters and create an instance of TimeBasedFileDelete
   * which tries to delete the old files
   */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println(HELP_MESSAGE);
            System.exit(0);
        }
        DirZip tbfd = new DirZip();
        tbfd.debug = "true".equals(System.getProperty("debug"));
        String tmpstr = System.getProperty("zip");
        if (tmpstr != null) tbfd.doZip = "true".equals(tmpstr);
        tmpstr = System.getProperty("delete");
        if (tmpstr != null) tbfd.doDelete = "true".equals(tmpstr);
        tbfd.dir = new File(args[0]).getCanonicalFile();
        tbfd.diff = 1l;
        String diffstr = args[1];
        int diffstrlen = diffstr.length();
        if (diffstr.endsWith("ms")) {
            tbfd.diff = Long.parseLong(diffstr.substring(0, diffstrlen - 2));
        } else if (diffstr.endsWith("s")) {
            tbfd.diff = Long.parseLong(diffstr.substring(0, diffstrlen - 1));
            tbfd.diff = tbfd.diff * 1000;
        } else if (diffstr.endsWith("m")) {
            tbfd.diff = Long.parseLong(diffstr.substring(0, diffstrlen - 1));
            tbfd.diff = tbfd.diff * 1000 * 60;
        } else if (diffstr.endsWith("h")) {
            tbfd.diff = Long.parseLong(diffstr.substring(0, diffstrlen - 1));
            tbfd.diff = tbfd.diff * 1000 * 60 * 60;
        } else if (diffstr.endsWith("d")) {
            tbfd.diff = Long.parseLong(diffstr.substring(0, diffstrlen - 1));
            tbfd.diff = tbfd.diff * 1000 * 60 * 60 * 24;
        } else {
            tbfd.diff = Long.parseLong(diffstr);
        }
        tbfd.run();
    }
}
