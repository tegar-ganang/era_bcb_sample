package de.enough.polish.log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * <p>Contains one log entry.</p>
 *
 * <p>Copyright (c) Enough Software 2005 - 2009</p>
 * <pre>
 * history
 *        23-Apr-2005 - rob creation
 * </pre>
 * @author Robert Virkus, j2mepolish@enough.de
 */
public class LogEntry {

    static final int VERSION = 100;

    public final String className;

    public final long time;

    public final String level;

    public final String message;

    public final int lineNumber;

    public final String exception;

    public final String thread;

    private byte[] data;

    /**
	 * Creates a new log entry,
	 * 
	 * @param className the name of the class
	 * @param lineNumber the line number within the class
	 * @param time the time of the log event
	 * @param level the level, e.g. "debug" or "info"
	 * @param message the message
	 * @param exception the exception message, if any
	 * 
	 */
    public LogEntry(String className, int lineNumber, long time, String level, String message, String exception) {
        this(className, lineNumber, time, level, message, exception, Thread.currentThread().getName());
    }

    /**
	 * Creates a new log entry,
	 * 
	 * @param className the name of the class
	 * @param lineNumber the line number within the class
	 * @param time the time of the log event
	 * @param level the level, e.g. "debug" or "info"
	 * @param message the message
	 * @param exception the exception message, if any
	 * @param threadName the name of the current thread, always null on CLDC 1.0 devices
	 */
    protected LogEntry(String className, int lineNumber, long time, String level, String message, String exception, String threadName) {
        if (className == null) {
            className = "";
        }
        if (level == null) {
            level = "";
        }
        if (message == null) {
            message = "";
        }
        if (exception == null) {
            exception = "";
        }
        if (threadName == null) {
            threadName = Thread.currentThread().toString();
        }
        this.className = className;
        this.lineNumber = lineNumber;
        this.time = time;
        this.level = level;
        this.message = message;
        this.exception = exception;
        this.thread = threadName;
    }

    /**
	 * Writes the data into a byte buffer.
	 * 
	 * @return a byte array containing the data
	 * @throws IOException when the data could not be written
	 */
    public byte[] toByteArray() throws IOException {
        if (this.data == null) {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);
            write(out);
            out.close();
            byteOut.close();
            this.data = byteOut.toByteArray();
        }
        return this.data;
    }

    /**
	 * Converts the entry into a log message.
	 * @return the String representation of this log entry
	 */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[").append(this.level).append("] ");
        buffer.append("{").append(this.thread).append("} ");
        buffer.append("<").append(this.time).append(" ms> ");
        buffer.append(this.className).append(" (").append(this.lineNumber).append("): ");
        buffer.append(this.message);
        if (this.exception.length() > 0) {
            buffer.append("/").append(this.exception);
        }
        return buffer.toString();
    }

    /**
	 * Writes this log entry to the given output stream.
	 * 
	 * @param out the output to which this entry should be writen.
	 * @throws IOException when the data could not be written 
	 */
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(VERSION);
        out.writeUTF(this.level);
        out.writeLong(this.time);
        out.writeUTF(this.className);
        out.writeInt(this.lineNumber);
        out.writeUTF(this.message);
        out.writeUTF(this.exception);
        out.writeUTF(this.thread);
    }

    /**
	 * Reads a new log entry from the given input stream
	 * 
	 * @param data the data
	 * @return a new log entry instance
	 * @throws IOException when the data could not be read 
	 */
    public static LogEntry newLogEntry(byte[] data) throws IOException {
        ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(byteIn);
        LogEntry entry = newLogEntry(in);
        in.close();
        return entry;
    }

    /**
	 * Reads a new log entry from the given input stream
	 * 
	 * @param in the input from which the data is read
	 * @return a new log entry instance
	 * @throws IOException when the data could not be read 
	 */
    public static LogEntry newLogEntry(DataInputStream in) throws IOException {
        int version = in.readInt();
        if (version > VERSION) {
            throw new IOException("Unable to read new log entry format, supported version is [" + VERSION + "], required version is [" + version + "].");
        }
        String level = in.readUTF();
        long time = in.readLong();
        String className = in.readUTF();
        int lineNumber = in.readInt();
        String message = in.readUTF();
        String exception = in.readUTF();
        String thread = in.readUTF();
        return new LogEntry(className, lineNumber, time, level, message, exception, thread);
    }

    /**
	 * Retrieves the message of this entry
	 * @return the message along with the exception, if there is any.
	 */
    public String getMessage() {
        if (this.exception != null) {
            return this.message + ": " + this.exception;
        } else {
            return this.message;
        }
    }
}
