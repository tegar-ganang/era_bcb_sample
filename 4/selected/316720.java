package barde.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;
import barde.log.LogReader;
import barde.log.Message;
import barde.t4c.T4CClientWriter;

/**
 * This class provides an implementation for of the <tt>write</tt> methods of the {@link LogWriter} interface,
 * to minimize the effort required to implement this interface.<br>
 * To implement a basic sequential <tt>LogWriter</tt>, the programmer needs only to extend this class and provide implementations for the following methods :
 * <ul>
 * <li>{@link #write(Message)} method.<br>
 * <li>also, the {@link #close()} method may be overriden, since it does nothing in this class
 * </ul>
 * To implement a <tt>LogWriter</tt> that can write comments, the programmer needs also to override the following methods :
 * <ul>
 * <li>{@link #commentsAreSupported()}	(returns false otherwise)
 * <li>{@link #writeComment(String)}	(throws UnsupportedOperationException otherwise)
 * </ul>
 * To implement a <tt>LogWriter</tt> that can split its output, the programmer needs also to override the following methods :
 * <ul>
 * <li>{@link #canSplit()}			(returns false otherwise)
 * <li>{@link #beforeSplit()}		(throws UnsupportedOperationException otherwise)
 * <li>{@link #afterSplit(File)}	(throws UnsupportedOperationException otherwise)
 * </ul>
 * @author cbonar
 * @see LogWriter
 */
public abstract class AbstractLogWriter implements LogWriter {

    /**
	 * This implementation handles bad messages by printing an error to stderr,
	 * and continuing with the next one.
	 */
    public void write(LogReader reader) throws IOException {
        while (true) {
            try {
                Message next = reader.read();
                if (next == null) return;
                write(next);
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
        }
    }

    public void write(Collection messages) throws IOException, ClassCastException {
        for (Iterator it = messages.iterator(); it.hasNext(); ) write((Message) it.next());
    }

    public void write(Message[] messages) throws IOException {
        for (int m = 0; m < messages.length; m++) write(messages[m]);
    }

    public void close() throws IOException {
    }

    public boolean commentsAreSupported() {
        return false;
    }

    public void writeComment(String comment) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public boolean canSplit() {
        return false;
    }

    public void beforeSplit() throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void afterSplit(File newOutput) throws IOException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public static final LogWriter getInstance(OutputStream os, ResourceBundle i18n) throws FileNotFoundException {
        if (i18n.getLocale().getLanguage().equals("t4c")) return new T4CClientWriter(os, i18n); else throw new IllegalArgumentException("Don't know how to instanciate a LogWriter for the type '" + i18n.getLocale().getLanguage() + "'");
    }
}
