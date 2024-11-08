package net.sf.webwarp.util.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.WriteAbortedException;
import java.util.HashMap;
import net.sf.webwarp.util.thread.LazyThread;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * This class is used to store a data object persistently. The content is serialized to a file. This is done on shutdown (using a shutdown hook). To make sure
 * the state is stored, even on improper termination of the program (kill -9 etc.), the state is also stored after commit() or set(...) is called. This is done
 * delayed by a low priority thread. Set values using the set method. The advantage against direct serialization is that the stored object may be extended
 * without loosing old values. It is recommended to extend this class by a Java-Bean like class. If the content of an Object is changed commit() should be
 * called.
 * 
 * @author bse
 * @created 13. Dezember 2002
 */
public class FiledState {

    public static final long DEFAULT_DELAY_MILLIS = 100;

    /**
     * The content - serializable.
     */
    private HashMap<String, Serializable> content = null;

    /**
     * The log
     */
    protected Logger log = Logger.getLogger(FiledState.class);

    /**
     * The file where the State is stored
     */
    private String persistentFileName;

    private WriteThread writeThread;

    private Thread shutdownHook = new Thread() {

        public void run() {
            shutdown();
        }
    };

    /**
     * Constructor to create a new FiledState, if no stored filed state is available.
     * 
     * @param persistentFileName
     *            The location of the persistent file
     * @exception IOException
     */
    @SuppressWarnings("unchecked")
    public FiledState(String persistentFileName) throws IOException {
        this.persistentFileName = persistentFileName;
        File persistentFile = new File(persistentFileName);
        if (!persistentFile.exists()) {
            log.info("Persistent file '" + persistentFile.getAbsolutePath() + "' does not yet exist - creating new.");
        } else {
            ObjectInputStream stream = null;
            stream = new ObjectInputStream(new FileInputStream(persistentFile));
            try {
                content = (HashMap<String, Serializable>) stream.readObject();
                log.debug("Persistent file '" + persistentFile.getAbsolutePath() + "' was properly read.");
            } catch (InvalidClassException ex) {
                log.warn("Resetting stored FiledState, due to " + ex);
            } catch (ClassNotFoundException ex) {
                log.error("Resetting stored FiledState, due to ClassNotFoundException", ex);
            } catch (WriteAbortedException ex) {
                log.error("Resetting stored FiledState, due to WriteAbortedException", ex);
            } finally {
                stream.close();
            }
        }
        if (content == null) {
            content = new HashMap<String, Serializable>();
        }
        writeThread = new WriteThread(DEFAULT_DELAY_MILLIS);
        writeThread.start();
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    /**
     * Set the delay for writing the state. If set to 0 writing is disabled until shutdown.
     */
    public void setWriteStateDelay(long millis) {
        writeThread.setDelay(millis);
    }

    /**
     * The commit method should be called if variable in the derived class or the content of an object has changed. If the set method is used to change an
     * object the commit is called automatically.
     */
    public void commit() {
        writeThread.trigger();
    }

    /**
     * Set method. Usually this should be called by derived class.
     * 
     * @param key
     *            The name of the parameter
     * @param value
     *            Description of the Parameter
     * @return previous value associated with specified key, or null if there was no mapping for key. A null return can also indicate that the map previously
     *         associated null with the specified key.
     * @value The parameter (a Serializable object).
     */
    public Serializable set(String key, Serializable value) {
        Serializable old = (Serializable) content.put(key, value);
        commit();
        return old;
    }

    /**
     * Get method. Usually this should be called by derived class.
     * 
     * @param key
     *            The name of the parameter
     * @return Value associated with specified key, or null if there was no mapping for key. A null return can also indicate that the map previously associated
     *         null with the specified key.
     */
    public Serializable get(String key) {
        return (Serializable) content.get(key);
    }

    public void setChar(String key, char value) {
        set(key, Character.valueOf(value));
    }

    public char getChar(String key) {
        Character value = (Character) get(key);
        return value == null ? 0 : value.charValue();
    }

    public void setByte(String key, byte value) {
        set(key, Byte.valueOf(value));
    }

    public byte getByte(String key) {
        Byte value = (Byte) get(key);
        return value == null ? 0 : value.byteValue();
    }

    public void setInt(String key, int value) {
        set(key, Integer.valueOf(value));
    }

    public int getInt(String key) {
        Integer value = (Integer) get(key);
        return value == null ? 0 : value.intValue();
    }

    public void setLong(String key, long value) {
        set(key, Long.valueOf(value));
    }

    public long getLong(String key) {
        Long value = (Long) get(key);
        return value == null ? 0 : value.longValue();
    }

    public void setFloat(String key, float value) {
        set(key, Float.valueOf(value));
    }

    public float getFloat(String key) {
        Float value = (Float) get(key);
        return value == null ? 0 : value.floatValue();
    }

    public void setDouble(String key, int value) {
        set(key, new Double(value));
    }

    public double getDouble(String key) {
        Double value = (Double) get(key);
        return value == null ? 0 : value.doubleValue();
    }

    public void setString(String key, String value) {
        set(key, value);
    }

    public String getString(String key) {
        return (String) get(key);
    }

    /**
     * Write the persistent file
     */
    private void saveState() {
        ObjectOutputStream stream = null;
        File persistentFile = new File(persistentFileName);
        File tempFile = new File(persistentFile.getParent(), '.' + persistentFile.getName() + ".tmp");
        try {
            stream = new ObjectOutputStream(new FileOutputStream(tempFile));
            stream.writeObject(content);
            log.debug("Saved state to '" + tempFile.getAbsolutePath() + "'");
        } catch (Exception ex) {
            log.error("Exception while writing FiledState to file: " + tempFile.getAbsolutePath() + "; " + ex);
        } finally {
            try {
                stream.close();
            } catch (Exception ex) {
            }
        }
        if (persistentFile.delete()) {
            log.debug("Deleted old file '" + persistentFile.getAbsolutePath() + "'");
        } else {
            log.debug("Did not delete old file '" + persistentFile.getAbsolutePath() + "'");
        }
        if (tempFile.renameTo(persistentFile)) {
            log.debug("Renamed temp file to '" + persistentFile.getAbsolutePath() + "'");
        } else {
            log.error("Failed to rename temp file to: " + persistentFile.getAbsolutePath());
        }
    }

    /**
     * Stop method. Write state and wait for shutdown. It is automatically called during shutdown using a shutdown hook. It may also be called explicitely.
     */
    public void shutdown() {
        if (writeThread.isShutdown()) {
            return;
        }
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ex) {
        }
        writeThread.trigger();
        try {
            writeThread.shutdown();
        } catch (InterruptedException iex) {
        }
    }

    private class WriteThread extends LazyThread {

        WriteThread(long delay) {
            super(delay);
            setPriority(Thread.NORM_PRIORITY - 2);
            setDaemon(true);
        }

        public void execute() {
            saveState();
        }
    }

    public static void main(String args[]) {
        Logger.getRootLogger().setLevel(Level.DEBUG);
        ConsoleAppender a = new ConsoleAppender(new PatternLayout("%d %t %-5p [%c] - %m%n"));
        a.setThreshold(Level.DEBUG);
        a.setWriter(new PrintWriter(System.out));
        BasicConfigurator.configure(a);
        try {
            String fileName = "C://temp/TestFiledState.dat";
            TestFiledState state = new TestFiledState(fileName);
            state.setWriteStateDelay(10);
            System.out.println("a = " + state.getA());
            System.out.println("x = " + state.getX());
            state.setA(String.valueOf(Math.random()));
            state.setX((int) (100 * Math.random()));
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TestFiledState extends FiledState {

        TestFiledState(String fileName) throws IOException {
            super(fileName);
        }

        public void setA(String a) {
            set("A", a);
        }

        public String getA() {
            return (String) get("A");
        }

        public void setX(int x) {
            setInt("X", x);
        }

        public int getX() {
            return getInt("X");
        }
    }
}
