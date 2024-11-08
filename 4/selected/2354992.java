package it.ilz.hostingjava.logger;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.*;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * Simple file logging <tt>Handler</tt>.
 * <p>
 * The <tt>FileHandler</tt> can either write to a specified file,
 * or it can write to a rotating set of files.
 * <p>
 * For a rotating set of files, as each file reaches a given size
 * limit, it is closed, rotated out, and a new file opened.
 * Successively older files are named by adding "0", "1", "2",
 * etc into the base filename.
 * <p>
 * By default buffering is enabled in the IO libraries but each log
 * record is flushed out when it is complete.
 * <p>
 * By default the <tt>XMLFormatter</tt> class is used for formatting.
 * <p>
 * <b>Configuration:</b>
 * By default each <tt>FileHandler</tt> is initialized using the following
 * <tt>LogManager</tt> configuration properties.  If properties are not defined
 * (or have invalid values) then the specified default values are used.
 * <ul>
 * <li>   java.util.logging.FileHandler.level
 *	  specifies the default level for the <tt>Handler</tt>
 *	  (defaults to <tt>Level.ALL</tt>).
 * <li>   java.util.logging.FileHandler.filter
 * 	  specifies the name of a <tt>Filter</tt> class to use
 *	  (defaults to no <tt>Filter</tt>).
 * <li>   java.util.logging.FileHandler.formatter
 *	  specifies the name of a </tt>Formatter</tt> class to use
 *        (defaults to <tt>java.util.logging.XMLFormatter</tt>)
 * <li>   java.util.logging.FileHandler.encoding
 *	  the name of the character set encoding to use (defaults to
 *	  the default platform encoding).
 * <li>   java.util.logging.FileHandler.limit
 *	  specifies an approximate maximum amount to write (in bytes)
 * 	  to any one file.  If this is zero, then there is no limit.
 *	  (Defaults to no limit).
 * <li>   java.util.logging.FileHandler.count
 *	  specifies how many output files to cycle through (defaults to 1).
 * <li>   java.util.logging.FileHandler.pattern
 *	  specifies a pattern for generating the output file name.  See
 *        below for details. (Defaults to "%h/java%u.log").
 * <li>   java.util.logging.FileHandler.append
 *	  specifies whether the FileHandler should append onto
 *        any existing files (defaults to false).
 * </ul>
 * <p>
 * <p>
 * A pattern consists of a string that includes the following special
 * components that will be replaced at runtime:
 * <ul>
 * <li>    "/"    the local pathname separator
 * <li>     "%t"   the system temporary directory
 * <li>     "%h"   the value of the "user.home" system property
 * <li>     "%g"   the generation number to distinguish rotated logs
 * <li>     "%u"   a unique number to resolve conflicts
 * <li>     "%%"   translates to a single percent sign "%"
 * </ul>
 * If no "%g" field has been specified and the file count is greater
 * than one, then the generation number will be added to the end of
 * the generated filename, after a dot.
 * <p>
 * Thus for example a pattern of "%t/java%g.log" with a count of 2
 * would typically cause log files to be written on Solaris to
 * /var/tmp/java0.log and /var/tmp/java1.log whereas on Windows 95 they
 * would be typically written to C:\TEMP\java0.log and C:\TEMP\java1.log
 * <p>
 * Generation numbers follow the sequence 0, 1, 2, etc.
 * <p>
 * Normally the "%u" unique field is set to 0.  However, if the <tt>FileHandler</tt>
 * tries to open the filename and finds the file is currently in use by
 * another process it will increment the unique number field and try
 * again.  This will be repeated until <tt>FileHandler</tt> finds a file name that
 * is  not currently in use. If there is a conflict and no "%u" field has
 * been specified, it will be added at the end of the filename after a dot.
 * (This will be after any automatically added generation number.)
 * <p>
 * Thus if three processes were all trying to log to fred%u.%g.txt then
 * they  might end up using fred0.0.txt, fred1.0.txt, fred2.0.txt as
 * the first file in their rotating sequences.
 * <p>
 * Note that the use of unique ids to avoid conflicts is only guaranteed
 * to work reliably when using a local disk file system.
 *
 * @version 1.34, 04/05/04
 * @since 1.4
 */
public class FileHandler extends StreamHandler implements Closeable {

    private MeteredStream meter;

    private boolean append;

    private int limit;

    private int count;

    private String pattern;

    private String lockFileName;

    private FileOutputStream lockStream;

    private File files[];

    private static final int MAX_LOCKS = 100;

    private static java.util.HashMap locks = new java.util.HashMap();

    private class MeteredStream extends OutputStream {

        OutputStream out;

        int written;

        MeteredStream(OutputStream out, int written) {
            this.out = out;
            this.written = written;
        }

        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        public void write(byte buff[]) throws IOException {
            out.write(buff);
            written += buff.length;
        }

        public void write(byte buff[], int off, int len) throws IOException {
            out.write(buff, off, len);
            written += len;
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            out.close();
        }
    }

    private void open(File fname, boolean append) throws IOException {
        int len = 0;
        if (append) {
            len = (int) fname.length();
        }
        FileOutputStream fout = new FileOutputStream(fname.toString(), append);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        meter = new MeteredStream(bout, len);
        setOutputStream(meter);
    }

    /**
     * Initialize a <tt>FileHandler</tt> to write to a set of files
     * with optional append.  When (approximately) the given limit has
     * been written to one file, another file will be opened.  The
     * output will cycle through a set of count files.
     * <p>
     * The <tt>FileHandler</tt> is configured based on <tt>LogManager</tt>
     * properties (or their default values) except that the given pattern
     * argument is used as the filename pattern, the file limit is
     * set to the limit argument, and the file count is set to the
     * given count argument, and the append mode is set to the given
     * <tt>append</tt> argument.
     * <p>
     * The count must be at least 1.
     *
     * @param pattern  the pattern for naming the output file
     * @param limit  the maximum number of bytes to write to any one file
     * @param count  the number of files to use
     * @param append  specifies append mode
     * @exception  IOException if there are IO problems opening the files.
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have <tt>LoggingPermission("control")</tt>.
     * @exception IllegalArgumentException if limit < 0, or count < 1.
     * @exception  IllegalArgumentException if pattern is an empty string
     *
     */
    public FileHandler(String pattern, int limit, int count, boolean append) throws IOException, SecurityException {
        if (limit < 0 || count < 1 || pattern.length() < 1) {
            throw new IllegalArgumentException();
        }
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        this.append = append;
    }

    private void openFiles() throws IOException {
        if (count < 1) {
            throw new IllegalArgumentException("file count = " + count);
        }
        if (limit < 0) {
            limit = 0;
        }
        InitializationErrorManager em = new InitializationErrorManager();
        setErrorManager(em);
        int unique = -1;
        for (; ; ) {
            unique++;
            if (unique > MAX_LOCKS) {
                throw new IOException("Couldn't get lock for " + pattern);
            }
            lockFileName = generate(pattern, 0, unique).toString() + ".lck";
            synchronized (locks) {
                if (locks.get(lockFileName) != null) {
                    continue;
                }
                FileChannel fc;
                try {
                    lockStream = new FileOutputStream(lockFileName);
                    fc = lockStream.getChannel();
                } catch (IOException ix) {
                    continue;
                }
                try {
                    FileLock fl = fc.tryLock();
                    if (fl == null) {
                        continue;
                    }
                } catch (IOException ix) {
                }
                locks.put(lockFileName, lockFileName);
                break;
            }
        }
        files = new File[count];
        for (int i = 0; i < count; i++) {
            files[i] = generate(pattern, i, unique);
        }
        if (append) {
            open(files[0], true);
        } else {
            rotate();
        }
        Exception ex = em.lastException;
        if (ex != null) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else if (ex instanceof SecurityException) {
                throw (SecurityException) ex;
            } else {
                throw new IOException("Exception: " + ex);
            }
        }
        setErrorManager(new ErrorManager());
        if (controllo == null) {
            controllo = new it.ilz.hostingjava.valves.controlli.File(this);
            controllo.start();
        }
    }

    private File generate(String pattern, int generation, int unique) throws IOException {
        File file = null;
        String word = "";
        int ix = 0;
        boolean sawg = false;
        boolean sawu = false;
        while (ix < pattern.length()) {
            char ch = pattern.charAt(ix);
            ix++;
            char ch2 = 0;
            if (ix < pattern.length()) {
                ch2 = Character.toLowerCase(pattern.charAt(ix));
            }
            if (ch == '/') {
                if (file == null) {
                    file = new File(word);
                } else {
                    file = new File(file, word);
                }
                word = "";
                continue;
            } else if (ch == '%') {
                if (ch2 == 't') {
                    String tmpDir = System.getProperty("java.io.tmpdir");
                    if (tmpDir == null) {
                        tmpDir = System.getProperty("user.home");
                    }
                    file = new File(tmpDir);
                    ix++;
                    word = "";
                    continue;
                } else if (ch2 == 'h') {
                    file = new File(System.getProperty("user.home"));
                    if (isSetUID()) {
                        throw new IOException("can't use %h in set UID program");
                    }
                    ix++;
                    word = "";
                    continue;
                } else if (ch2 == 'g') {
                    word = word + generation;
                    sawg = true;
                    ix++;
                    continue;
                } else if (ch2 == 'u') {
                    word = word + unique;
                    sawu = true;
                    ix++;
                    continue;
                } else if (ch2 == '%') {
                    word = word + "%";
                    ix++;
                    continue;
                }
            }
            word = word + ch;
        }
        if (count > 1 && !sawg) {
            word = word + "." + generation;
        }
        if (unique > 0 && !sawu) {
            word = word + "." + unique;
        }
        if (word.length() > 0) {
            if (file == null) {
                file = new File(word);
            } else {
                file = new File(file, word);
            }
        }
        return file;
    }

    private synchronized void rotate() {
        Level oldLevel = getLevel();
        setLevel(Level.OFF);
        super.close();
        for (int i = count - 2; i >= 0; i--) {
            File f1 = files[i];
            File f2 = files[i + 1];
            if (f1.exists()) {
                if (f2.exists()) {
                    f2.delete();
                }
                f1.renameTo(f2);
            }
        }
        try {
            open(files[0], false);
        } catch (IOException ix) {
            reportError(null, ix, ErrorManager.OPEN_FAILURE);
        }
        setLevel(oldLevel);
    }

    /**
     * Format and publish a <tt>LogRecord</tt>.
     *
     * @param  record  description of the log event. A null record is
     *                 silently ignored and is not published
     */
    public synchronized void publish(LogRecord record) {
        if (record.getLevel().intValue() < Level.INFO.intValue()) return;
        try {
            if (lockStream == null) openFiles();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (!isLoggable(record)) {
            return;
        }
        super.publish(record);
        flush();
        if (limit > 0 && meter.written >= limit) {
            AccessController.doPrivileged(new PrivilegedAction() {

                public Object run() {
                    rotate();
                    return null;
                }
            });
        }
        if (controllo != null) controllo.aggiornaTempo(System.currentTimeMillis());
    }

    /**
     * Close all the files.
     *
     * @exception  SecurityException  if a security manager exists and if
     *             the caller does not have <tt>LoggingPermission("control")</tt>.
     */
    public synchronized void close() throws SecurityException {
        super.close();
        if (lockFileName == null) {
            return;
        }
        try {
            lockStream.close();
        } catch (Exception ex) {
        }
        synchronized (locks) {
            locks.remove(lockFileName);
        }
        new File(lockFileName).delete();
        lockFileName = null;
        lockStream = null;
        controllo = null;
    }

    private static class InitializationErrorManager extends ErrorManager {

        Exception lastException;

        public void error(String msg, Exception ex, int code) {
            lastException = ex;
        }
    }

    private static native boolean isSetUID();

    private it.ilz.hostingjava.valves.controlli.File controllo = null;
}
