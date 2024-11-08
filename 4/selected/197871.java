package org.japano;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.jsp.JspWriter;

/** Collects String object references and finally flushes them to a Writer.
 
 @author Sven Helmberger ( sven dot helmberger at gmx dot de )
 @version $Id: Buffer.java,v 1.13 2005/12/30 15:54:02 fforw Exp $
 #SFLOGO#
 */
public class Buffer extends JspWriter {

    /** default capacity counted in String objects */
    public static final int DEFAULT_CAPACITY = 128;

    /** Contains all String objects collected by this buffer. the size of the array grows
   * with the capacity' of the buffer
   */
    private String[] buf;

    /** Current Amount of collected String objects */
    private int count;

    /** Total length sum of the collected String objects. */
    private int len;

    /** headMarker */
    private int headMark;

    /** set to true when the buffer is closed.*/
    private boolean closed;

    /** parent writer */
    private Writer writer;

    /** Creates a new instance of Buffer with default capacity, */
    public Buffer() {
        this(DEFAULT_CAPACITY);
    }

    /** Creates a new instance of Buffer with given capacity.    
   @param capacity capacity counted in string fragments ( not characters!). if this is 0 the allocation
   of the string array will be defered until the first access to buffer (with print or ensureCapacity(int))
   */
    public Buffer(int capacity) {
        super(0, false);
        if (capacity > 0) {
            buf = new String[capacity];
        }
    }

    /**
   * Sets the writer this buffer flushes to.
   * @param writer Writer
   */
    public void setWriter(Writer writer) {
        this.writer = writer;
    }

    /** Ensures that the Buffer has enough capacity for the given amount of
   * String objects.
   * @param capacity Capacity in String objects (<B>NOT</B> character)
   */
    public void ensureCapacity(int capacity) {
        if (buf == null) {
            buf = new String[capacity];
        } else {
            if (capacity > buf.length) {
                String[] tmp = new String[(buf.length << 1) + 1];
                System.arraycopy(buf, 0, tmp, 0, count);
                buf = tmp;
            }
        }
    }

    /** Gets the current capacity in String objects (<B>NOT</B> character)
   * @return capacity in String objects (<B>NOT</B> character)
   */
    public int getCapacity() {
        return buf == null ? 0 : buf.length;
    }

    /** Clears the Buffer. */
    public void clear() {
        count = 0;
        len = 0;
    }

    /**
   * Flushes all collected Strings
   * @throws IOException 
   */
    public void flush() throws IOException {
        if (writer == null) throw new IllegalStateException("No parent writer");
        if (buf != null) {
            for (int i = 0; i < count; i++) {
                String s = buf[i];
                if (s != null) writer.write(s);
            }
        }
    }

    public int getRemaining() {
        return Integer.MAX_VALUE;
    }

    private static final String NEWLINE = System.getProperty("line.separator");

    public void print(String str) {
        if (closed) throw new IllegalStateException("writer already closed");
        if (str != null) {
            int fragmentLen = str.length();
            if (fragmentLen > 0) {
                ensureCapacity(count + 1);
                buf[count++] = str;
                len += fragmentLen;
            }
        }
    }

    /**
   Returns a substring of the buffer starting with the given fragment position
   and ending at the given fragment position.
   
   @param start position of the first fragment
   @param end position of the last fragment plus one.
   */
    public String substring(int start, int end) {
        StringBuffer buf = new StringBuffer(len);
        for (int i = start; i < end; i++) buf.append(this.buf[i]);
        return buf.toString();
    }

    /**
   Returns the length of the buffer content (not the fragment count).
   @return length in characters
   */
    public int length() {
        return len;
    }

    public void newLine() {
        print(NEWLINE);
    }

    public void print(char[] values) {
        print(String.valueOf(values));
    }

    public void print(int param) {
        print(String.valueOf(param));
    }

    public void print(boolean param) {
        print(String.valueOf(param));
    }

    public void print(char param) {
        print(String.valueOf(param));
    }

    public void print(float param) {
        print(String.valueOf(param));
    }

    public void print(Object obj) {
        print(obj != null ? obj.toString() : "null");
    }

    public void print(long param) {
        print(String.valueOf(param));
    }

    public void print(double param) {
        print(String.valueOf(param));
    }

    public void println() {
        newLine();
    }

    public void println(float param) {
        print(param);
        newLine();
    }

    public void println(Object obj) {
        print(obj);
        newLine();
    }

    public void println(int param) {
        print(param);
        newLine();
    }

    public void println(char param) {
        print(param);
        newLine();
    }

    public void println(boolean param) {
        print(param);
        newLine();
    }

    public void println(long param) {
        print(param);
        newLine();
    }

    public void println(double param) {
        print(param);
        newLine();
    }

    public void println(String str) {
        print(str);
        newLine();
    }

    public void println(char[] values) {
        print(values);
        newLine();
    }

    public void write(char[] cbuf) {
        print(String.valueOf(cbuf));
    }

    public void write(char[] cbuf, int off, int len) {
        print(String.valueOf(cbuf, off, len));
    }

    public void write(int c) {
        print(String.valueOf((char) c));
    }

    public void write(String str) {
        print(str);
    }

    public void write(String str, int off, int len) {
        print(str.substring(off, len));
    }

    public void clearBuffer() {
        clear();
    }

    public void close() throws IOException {
        if (writer != null) writer.close();
        closed = true;
    }

    public int size() {
        return count;
    }

    public String toString() {
        if (this.buf == null) return ""; else {
            return substring(0, count);
        }
    }

    public void setHeadMarker() {
        this.headMark = count;
    }

    /**
   Inserts the given buffer content into the page's head section
   */
    public void insertInHead(Buffer b) {
        if (headMark == 0) throw new IllegalStateException("No <head></head> in page");
        insert(b, headMark);
    }

    public void insert(Buffer b, int offset) {
        int insertLen = b.count;
        ensureCapacity(count + insertLen);
        System.arraycopy(buf, offset, buf, offset + insertLen, count - offset);
        System.arraycopy(b.buf, 0, buf, offset, insertLen);
        len += b.length();
        count += insertLen;
    }

    /**
   Cuts the length of the buffer to the given fragment count.   
   */
    public void setSize(int size) {
        if (size >= buf.length) throw new IllegalArgumentException("Illegal size");
        count = size;
    }

    public void printNode(Object o) {
        if (o instanceof PageNode) {
            ((PageNode) o).generate(this);
        } else {
            if (o != null) this.print(o.toString());
        }
    }
}
