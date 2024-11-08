package org.simpleframework.http.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The <code>Accumulator</code> object is an output stream that can
 * buffer bytes written up to a given size. This is used if a buffer
 * is requested for the response output. Such a mechanism allows the
 * response to be written without committing the response. Also it
 * enables content that has been written to be reset, by simply
 * clearing the accumulator buffer. Once the accumulator buffer has
 * overflown then the response is committed.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.core.Transfer
 */
class Accumulator extends OutputStream {

    /**
    * This is the transfer object used to transfer the response.
    */
    private Transfer transfer;

    /**
    * This is the buffer used to accumulate the response bytes.
    */
    private byte[] buffer;

    /**
    * This is used to determine if the accumulate was flushed.
    */
    private boolean flushed;

    /**
    * This is used to determine if the accumulator was closed.
    */
    private boolean closed;

    /**
    * This counts the number of bytes that have been accumulated.
    */
    private int count;

    /**
    * Constructor for the <code>Accumulator</code> object. This will
    * create a buffering output stream which will flush data to the
    * underlying transport provided with the entity. All I/O events
    * are reported to the monitor so the server can process other
    * requests within the pipeline when the current one is finished.
    *
    * @param support this is used to determine the response semantics
    * @param entity this is used to acquire the underlying transport
    * @param monitor this is used to report I/O events to the kernel    
    */
    public Accumulator(Conversation support, Entity entity, Monitor monitor) {
        this(support, entity.getChannel(), monitor);
    }

    /**
    * Constructor for the <code>Accumulator</code> object. This will
    * create a buffering output stream which will flush data to the
    * underlying transport provided with the channel. All I/O events
    * are reported to the monitor so the server can process other
    * requests within the pipeline when the current one is finished.
    *
    * @param support this is used to determine the response semantics
    * @param entity this is used to acquire the underlying transport
    * @param monitor this is used to report I/O events to the kernel    
    */
    public Accumulator(Conversation support, Channel channel, Monitor monitor) {
        this(support, channel.getSender(), monitor);
    }

    /**
    * Constructor for the <code>Accumulator</code> object. This will
    * create a buffering output stream which will flush data to the
    * underlying transport using the sender provided. All I/O events
    * are reported to the monitor so the server can process other
    * requests within the pipeline when the current one is finished.
    *
    * @param support this is used to determine the response semantics
    * @param sender this is used to write to the underlying transport
    * @param monitor this is used to report I/O events to the kernel    
    */
    public Accumulator(Conversation support, Sender sender, Monitor monitor) {
        this.transfer = new Transfer(support, sender, monitor);
        this.buffer = new byte[] {};
    }

    /**
    * This is used to reset the buffer so that it can be written to
    * again. If the accumulator has already been flushed then the
    * stream can not be reset. Resetting the stream is typically 
    * done if there is an error in writing the response and an error
    * message is generated to replaced the partial response.
    */
    public void reset() throws IOException {
        if (flushed) {
            throw new IOException("Respose has been flushed");
        }
        count = 0;
    }

    /**
    * This is used to write the provided octet to the buffer. If the
    * buffer is full it will be flushed and the octet is appended to
    * the start of the buffer. If however the buffer is zero length
    * then this will write directly to the underlying transport.
    *
    * @param octet this is the octet that is to be written
    */
    public void write(int octet) throws IOException {
        byte value = (byte) octet;
        if (closed) {
            throw new IOException("Response has been transferred");
        }
        write(new byte[] { value });
    }

    /**
    * This is used to write the provided octet to the buffer. If the
    * buffer is full it will be flushed and the octet is appended to
    * the start of the buffer. If however the buffer is zero length
    * then this will write directly to the underlying transport.
    *
    * @param array this is the array of bytes to send to the client
    * @param off this is the offset within the array to send from
    * @param len this is the number of bytes that are to be sent
    */
    public void write(byte[] array, int off, int size) throws IOException {
        if (closed) {
            throw new IOException("Response has been transferred");
        }
        if (count + size > buffer.length) {
            flush();
        }
        if (size > buffer.length) {
            transfer.write(array, off, size);
        } else {
            System.arraycopy(array, off, buffer, count, size);
            count += size;
        }
    }

    /**
    * This is used to expand the capacity of the internal buffer. If
    * there is already content that has been appended to the buffer
    * this will copy that data to the newly created buffer. This 
    * will not decrease the size of the buffer if it is larger than
    * the requested capacity.
    *
    * @param capacity this is the capacity to expand the buffer to
    */
    public void expand(int capacity) throws IOException {
        if (buffer.length < capacity) {
            int size = buffer.length * 2;
            int resize = Math.max(capacity, size);
            byte[] temp = new byte[resize];
            System.arraycopy(buffer, 0, temp, 0, count);
            buffer = temp;
        }
    }

    /**
    * This is used to flush the contents of the buffer to the 
    * underlying transport. Once the accumulator is flushed the HTTP
    * headers are written such that the semantics of the connection
    * match the protocol version and the existing response headers.
    */
    public void flush() throws IOException {
        if (!flushed) {
            transfer.start();
        }
        if (count > 0) {
            transfer.write(buffer, 0, count);
        }
        count = 0;
        flushed = true;
    }

    /**
    * This will flush the buffer to the underlying transport and 
    * close the stream.  Once the accumulator is flushed the HTTP
    * headers are written such that the semantics of the connection
    * match the protocol version and the existing response headers.
    * Closing this stream does not mean the connection is closed.
    */
    public void close() throws IOException {
        if (!closed) {
            commit();
        }
        flushed = true;
        closed = true;
    }

    /**
    * This will close the underlying transfer object which will 
    * notify the server kernel that the next request is read to be
    * processed. If the accumulator is unflushed then this willl set
    * a Content-Length header such that it matches the number of
    * bytes that are buffered within the internal buffer.
    */
    private void commit() throws IOException {
        if (!flushed) {
            transfer.start(count);
        }
        if (count > 0) {
            transfer.write(buffer, 0, count);
        }
        transfer.close();
    }
}
