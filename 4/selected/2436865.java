package com.sun.jini.mahalo.log;

import com.sun.jini.logging.Levels;
import com.sun.jini.mahalo.log.MultiLogManager.LogRemovalManager;
import com.sun.jini.mahalo.TxnManager;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

/**
 * An implementation of a re-usable <code>Log</code>.
 * 
 * @author Sun Microsystems, Inc.
 * 
 * @see com.sun.jini.mahalo.log.Log
 */
public class SimpleLogFile implements Log {

    /** Unique ID associated with this log */
    private long cookie;

    /** Output stream for writing log objects */
    private ObjectOutputStream out;

    /**
	 * File output stream associated with <code>out</code>. Used to get a handle
	 * to underlying file descriptor object.
	 */
    private FileOutputStream outfile;

    /** (Relative) File name of the log file */
    private String name;

    /**
	 * Reference to <code>LogRemovalManager</code>, which is called to remove
	 * this log from the managed set of logs.
	 */
    private LogRemovalManager logMgr;

    /**
	 * Flag that indicates validity of this log. Set to false by call to
	 * <code>invalidate()</code>.
	 */
    private boolean valid = true;

    /**
	 * Flag to indicate that the log file has been created via the read-only
	 * constructor. This flag is set to false via the non-read-only constructor
	 * or a call to <code>recover()</code>
	 */
    private boolean readonly = false;

    /** Logger for persistence related messages */
    private static final Logger persistenceLogger = Logger.getLogger(TxnManager.MAHALO + ".persistence");

    /** Logger for operations related messages */
    private static final Logger operationsLogger = Logger.getLogger(TxnManager.MAHALO + ".operations");

    /** Logger for initialization related messages */
    private static final Logger initLogger = Logger.getLogger(TxnManager.MAHALO + ".init");

    /**
	 * This class extends <tt>ObjectInputStream</tt> and overrides the
	 * <code>readStreamHeader</code> method to a no-op operation. This class is
	 * intended to work in conjunction with
	 * <code>HeaderlessObjectOutputStream</code>.
	 */
    private static class HeaderlessObjectInputStream extends ObjectInputStream {

        /**
		 * Simple constructor that passes its argument to the superclass
		 * 
		 * @exception IOException
		 *                if an I/O error occurs
		 */
        public HeaderlessObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        /**
		 * Overrides <tt>ObjectInputStream</tt>'s method with no-op
		 * functionality.
		 * 
		 * @see HeaderlessObjectOutputStream#writeStreamHeader
		 * @exception IOException
		 *                if an I/O error occurs
		 */
        protected void readStreamHeader() throws IOException {
        }
    }

    /**
	 * This class extends <tt>ObjectOutputStream</tt> and overrides the
	 * <code>writeStreamHeader</code> method to a no-op operation. This class is
	 * intended to be used in conjunction with
	 * <code>HeaderlessObjectInputStream</code>.
	 */
    private static class HeaderlessObjectOutputStream extends ObjectOutputStream {

        /**
		 * Simple constructor that passes its argument to the superclass
		 * 
		 * @exception IOException
		 *                if an I/O error occurs
		 */
        public HeaderlessObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        /**
		 * Overrides <tt>ObjectOutputStream</tt>'s method with no-op
		 * functionality. This prevents header information from being sent to
		 * the stream, which makes appending to existing log files easier.
		 * Otherwise, appending header info to an existing log file would cause
		 * a corresponding <code>ObjectInputStream</code> to throw a
		 * <code>StreamCorruptedException</code> when it encountered the header
		 * information instead of the class/object type code information it was
		 * expecting.
		 * 
		 * @exception IOException
		 *                if an I/O error occurs
		 */
        protected void writeStreamHeader() throws IOException {
        }
    }

    /**
	 * Creates a read-only <code>SimpleLogFile</code>
	 * 
	 * To be used for read-only access to a named <code>Log</code>. This is
	 * desired when recovering information from a <code>Log</code>.
	 * 
	 * @param name
	 *            names the file in which information is stored.
	 * 
	 * @param logMgr
	 *            <code>LogRemovalManager</code> managing this log. This object
	 *            is called back to remove this log from the manager's managed
	 *            set of log files.
	 * 
	 * @see com.sun.jini.mahalo.log.Log
	 * @see com.sun.jini.mahalo.log.LogManager
	 * @see com.sun.jini.mahalo.log.MultiLogManager
	 * @see com.sun.jini.mahalo.log.MultiLogManager.LogRemovalManager
	 */
    public SimpleLogFile(String name, LogRemovalManager logMgr) {
        init(name, 0, logMgr);
        readonly = true;
    }

    /**
	 * Creates a <code>SimpleLogFile</code>.
	 * 
	 * @param name
	 *            names the file in which information is stored.
	 * 
	 * @param cookie
	 *            identifier representing information being stored.
	 * 
	 * @param logMgr
	 *            <code>LogRemovalManager</code> managing this log. This object
	 *            is called back to remove this log from the manager's
	 *            responsibility.
	 * 
	 * @see com.sun.jini.mahalo.log.Log
	 * @see com.sun.jini.mahalo.log.LogManager
	 * @see com.sun.jini.mahalo.log.MultiLogManager
	 * @see com.sun.jini.mahalo.log.MultiLogManager.LogRemovalManager
	 */
    public SimpleLogFile(String name, long cookie, LogRemovalManager logMgr) {
        init(name, cookie, logMgr);
    }

    private void init(String name, long cookie, LogRemovalManager logMgr) {
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.entering(SimpleLogFile.class.getName(), "init", new Object[] { name, new Long(cookie), logMgr });
        }
        if (name == null) throw new IllegalArgumentException("SimpleLogFile: null name");
        if (logMgr == null) throw new IllegalArgumentException("SimpleLogFile: null log manager");
        this.name = name;
        this.cookie = cookie;
        this.logMgr = logMgr;
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.exiting(SimpleLogFile.class.getName(), "init");
        }
    }

    /**
	 * Returns the identifier associated with information in this
	 * <code>Log</code>.
	 * 
	 * @see com.sun.jini.mahalo.log.Log
	 */
    public long cookie() {
        return cookie;
    }

    /**
	 * Add a <code>LogRecord</code> to the <code>Log</code>.
	 * 
	 * @param rec
	 *            the record to be logged.
	 * 
	 * @see com.sun.jini.mahalo.log.LogRecord
	 */
    public synchronized void write(LogRecord rec) throws LogException {
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.entering(SimpleLogFile.class.getName(), "write", rec);
        }
        try {
            if (!valid) throw new InvalidatedLogException("Cannot write to to " + "invalidated log");
            if (readonly) throw new LogException("Unable to write to read only log");
            if (out == null) {
                boolean append = true;
                File log = new File(name);
                outfile = new FileOutputStream(name, append);
                out = new HeaderlessObjectOutputStream(new BufferedOutputStream(outfile));
                if (log.length() == 0) {
                    out.writeLong(cookie);
                }
                out.reset();
            }
            out.writeObject(rec);
            out.flush();
            outfile.getFD().sync();
            if (persistenceLogger.isLoggable(Level.FINEST)) {
                persistenceLogger.log(Level.FINEST, "Wrote: {0}", rec);
            }
        } catch (InvalidClassException ice) {
            if (persistenceLogger.isLoggable(Level.WARNING)) {
                persistenceLogger.log(Level.WARNING, "Problem persisting LogRecord", ice);
            }
        } catch (NotSerializableException nse) {
            if (persistenceLogger.isLoggable(Level.WARNING)) {
                persistenceLogger.log(Level.WARNING, "Problem persisting LogRecord", nse);
            }
        } catch (IOException ioe) {
            if (persistenceLogger.isLoggable(Level.WARNING)) {
                persistenceLogger.log(Level.WARNING, "Problem persisting LogRecord", ioe);
            }
        } catch (SecurityException se) {
            if (persistenceLogger.isLoggable(Level.WARNING)) {
                persistenceLogger.log(Level.WARNING, "Problem persisting LogRecord", se);
            }
        }
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.exiting(SimpleLogFile.class.getName(), "write", rec);
        }
    }

    /**
	 * Invalidate the log.
	 */
    public synchronized void invalidate() throws LogException {
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.entering(MultiLogManager.class.getName(), "invalidate");
        }
        if (persistenceLogger.isLoggable(Level.FINEST)) {
            persistenceLogger.log(Level.FINEST, "Invalidating log for cookie: {0}", new Long(cookie));
        }
        if (valid) {
            valid = false;
            logMgr.release(cookie);
        }
        try {
            if (out != null) {
                if (persistenceLogger.isLoggable(Level.FINEST)) {
                    persistenceLogger.log(Level.FINEST, "Closing log file for: {0}", new Long(cookie));
                }
                out.close();
            }
        } catch (IOException ioe) {
            if (persistenceLogger.isLoggable(Levels.HANDLED)) {
                persistenceLogger.log(Levels.HANDLED, "Problem closing log file", ioe);
            }
        }
        try {
            File fl = new File(name);
            if (persistenceLogger.isLoggable(Level.FINEST)) {
                persistenceLogger.log(Level.FINEST, "Deleting log file for: {0}", new Long(cookie));
            }
            if (!fl.delete()) {
                if (persistenceLogger.isLoggable(Levels.HANDLED)) {
                    persistenceLogger.log(Levels.HANDLED, "Could not delete log file");
                }
            }
        } catch (SecurityException se) {
            if (persistenceLogger.isLoggable(Level.FINEST)) {
                persistenceLogger.log(Level.FINEST, "SecurityException on log deletion", se);
            }
            throw new LogException("SimpleLogFile: invalidate: " + "cannot delete log file.");
        }
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.exiting(MultiLogManager.class.getName(), "invalidate");
        }
    }

    /**
	 * Recover information from the log.
	 * 
	 * @param client
	 *            who to inform with information from the log.
	 * 
	 * @see com.sun.jini.mahalo.log.LogRecovery
	 */
    public synchronized void recover(LogRecovery client) throws LogException {
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.entering(MultiLogManager.class.getName(), "recover", client);
        }
        if (!valid) throw new InvalidatedLogException("Cannot recover from " + "invalidated log");
        if (client == null) throw new IllegalArgumentException("Cannot have a <null> " + "client argument.");
        ObjectInputStream in = null;
        ArrayList recList = new ArrayList();
        try {
            if (persistenceLogger.isLoggable(Level.FINEST)) {
                persistenceLogger.log(Level.FINEST, "Recovering from: {0}", name);
            }
            in = new HeaderlessObjectInputStream(new BufferedInputStream(new FileInputStream(name)));
            this.cookie = in.readLong();
            LogRecord rec = null;
            boolean done = false;
            boolean update = true;
            try {
                while (!done) {
                    rec = (LogRecord) in.readObject();
                    if (rec != null) {
                        recList.add(rec);
                    } else {
                        update = false;
                        done = true;
                        if (persistenceLogger.isLoggable(Levels.HANDLED)) {
                            persistenceLogger.log(Levels.HANDLED, "Log for cookie {0} contained a null " + "record object", new Long(cookie));
                        }
                    }
                }
            } catch (ClassNotFoundException cnfe) {
                update = false;
                if (persistenceLogger.isLoggable(Level.WARNING)) {
                    persistenceLogger.log(Level.WARNING, "Problem recovering log file", cnfe);
                }
            } catch (ClassCastException cce) {
                update = false;
                if (persistenceLogger.isLoggable(Level.WARNING)) {
                    persistenceLogger.log(Level.WARNING, "Problem recovering log file", cce);
                }
            } catch (EOFException eofe) {
            } catch (IOException ioe) {
                update = false;
                if (persistenceLogger.isLoggable(Level.WARNING)) {
                    persistenceLogger.log(Level.WARNING, "Problem recovering log file", ioe);
                }
            }
            if (update) {
                for (int i = 0; i < recList.size(); i++) {
                    client.recover(cookie, (LogRecord) recList.get(i));
                }
            } else {
                if (persistenceLogger.isLoggable(Level.WARNING)) {
                    persistenceLogger.log(Level.WARNING, "Skipping log recovery for", name);
                }
            }
        } catch (IOException ioe) {
            if (persistenceLogger.isLoggable(Level.WARNING)) {
                persistenceLogger.log(Level.WARNING, "Problem recovering log file", ioe);
            }
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ioe) {
                if (persistenceLogger.isLoggable(Levels.HANDLED)) {
                    persistenceLogger.log(Levels.HANDLED, "Problem closing recovered log file", ioe);
                }
            }
            readonly = false;
        }
        if (operationsLogger.isLoggable(Level.FINER)) {
            operationsLogger.exiting(MultiLogManager.class.getName(), "recover");
        }
    }
}
