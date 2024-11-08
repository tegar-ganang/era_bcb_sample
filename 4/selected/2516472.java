package com.abb.util;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

/** Wraps a {@link Reader} so that every call to {@link #read} will
    write the read character into another specified {@link
    Writer}. When the reader is closed, the writer will also be
    closed.

    @author Axel Uhl
    @version $Id: TeeReader.java,v 1.2 2001/01/06 18:57:12 aul Exp $ */
public class TeeReader extends Reader {

    /** wraps the given reader so that all bytes read from it
	will also be "teed" to the given writer

	@param reader the stream to read from when {@link #read}
	is called
	@param writer the stream to "tee" the read input to
    */
    public TeeReader(Reader reader, Writer writer) {
        this.reader = reader;
        this.writer = writer;
    }

    /** reads a byte from the underlying reader and "tees" it
	into the writer

	@param buf the buffer into which to read
	@param offset where in the buffer to start storing characters
	@param len how many characters to read
	@return the number of characters read
    */
    public int read(char[] buf, int offset, int len) throws IOException {
        int r = reader.read(buf, offset, len);
        if (r > 0) writer.write(buf, offset, r);
        return r;
    }

    /** closes the underlying input writer <em>and</em> the tee writer
    */
    public void close() throws IOException {
        reader.close();
        writer.close();
    }

    /** the input stream from which bytes are read */
    private Reader reader;

    /** the output stream into which read bytes are "teed" (written)
     */
    private Writer writer;
}
