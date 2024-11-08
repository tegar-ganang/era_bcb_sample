package org.apache.hadoop.net;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

/**
 * This implements an output stream that can have a timeout while writing.
 * This sets non-blocking flag on the socket channel.
 * So after creating this object , read() on 
 * {@link Socket#getInputStream()} and write() on 
 * {@link Socket#getOutputStream()} on the associated socket will throw 
 * llegalBlockingModeException.
 * Please use {@link SocketInputStream} for reading.
 */
public class SocketOutputStream extends OutputStream implements WritableByteChannel {

    private Writer writer;

    private static class Writer extends SocketIOWithTimeout {

        WritableByteChannel channel;

        Writer(WritableByteChannel channel, long timeout) throws IOException {
            super((SelectableChannel) channel, timeout);
            this.channel = channel;
        }

        int performIO(ByteBuffer buf) throws IOException {
            return channel.write(buf);
        }
    }

    /**
   * Create a new ouput stream with the given timeout. If the timeout
   * is zero, it will be treated as infinite timeout. The socket's
   * channel will be configured to be non-blocking.
   * 
   * @param channel 
   *        Channel for writing, should also be a {@link SelectableChannel}.  
   *        The channel will be configured to be non-blocking.
   * @param timeout timeout in milliseconds. must not be negative.
   * @throws IOException
   */
    public SocketOutputStream(WritableByteChannel channel, long timeout) throws IOException {
        SocketIOWithTimeout.checkChannelValidity(channel);
        writer = new Writer(channel, timeout);
    }

    /**
   * Same as SocketOutputStream(socket.getChannel(), timeout):<br><br>
   * 
   * Create a new ouput stream with the given timeout. If the timeout
   * is zero, it will be treated as infinite timeout. The socket's
   * channel will be configured to be non-blocking.
   * 
   * @see SocketOutputStream#SocketOutputStream(WritableByteChannel, long)
   *  
   * @param socket should have a channel associated with it.
   * @param timeout timeout timeout in milliseconds. must not be negative.
   * @throws IOException
   */
    public SocketOutputStream(Socket socket, long timeout) throws IOException {
        this(socket.getChannel(), timeout);
    }

    public void write(int b) throws IOException {
        byte[] buf = new byte[1];
        buf[0] = (byte) b;
        write(buf, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(b, off, len);
        while (buf.hasRemaining()) {
            try {
                if (write(buf) < 0) {
                    throw new IOException("The stream is closed");
                }
            } catch (IOException e) {
                if (buf.capacity() > buf.remaining()) {
                    writer.close();
                }
                throw e;
            }
        }
    }

    public synchronized void close() throws IOException {
        writer.channel.close();
        writer.close();
    }

    /**
   * Returns underlying channel used by this stream.
   * This is useful in certain cases like channel for 
   * {@link FileChannel#transferTo(long, long, WritableByteChannel)}
   */
    public WritableByteChannel getChannel() {
        return writer.channel;
    }

    public boolean isOpen() {
        return writer.isOpen();
    }

    public int write(ByteBuffer src) throws IOException {
        return writer.doIO(src, SelectionKey.OP_WRITE);
    }

    /**
   * waits for the underlying channel to be ready for writing.
   * The timeout specified for this stream applies to this wait.
   *
   * @throws SocketTimeoutException 
   *         if select on the channel times out.
   * @throws IOException
   *         if any other I/O error occurs. 
   */
    public void waitForWritable() throws IOException {
        writer.waitForIO(SelectionKey.OP_WRITE);
    }

    /**
   * Transfers data from FileChannel using 
   * {@link FileChannel#transferTo(long, long, WritableByteChannel)}. 
   * 
   * Similar to readFully(), this waits till requested amount of 
   * data is transfered.
   * 
   * @param fileCh FileChannel to transfer data from.
   * @param position position within the channel where the transfer begins
   * @param count number of bytes to transfer.
   * 
   * @throws EOFException 
   *         If end of input file is reached before requested number of 
   *         bytes are transfered.
   *
   * @throws SocketTimeoutException 
   *         If this channel blocks transfer longer than timeout for 
   *         this stream.
   *          
   * @throws IOException Includes any exception thrown by 
   *         {@link FileChannel#transferTo(long, long, WritableByteChannel)}. 
   */
    public void transferToFully(FileChannel fileCh, long position, int count) throws IOException {
        while (count > 0) {
            waitForWritable();
            int nTransfered = (int) fileCh.transferTo(position, count, getChannel());
            if (nTransfered == 0) {
                if (position >= fileCh.size()) {
                    throw new EOFException("EOF Reached. file size is " + fileCh.size() + " and " + count + " more bytes left to be " + "transfered.");
                }
            } else if (nTransfered < 0) {
                throw new IOException("Unexpected return of " + nTransfered + " from transferTo()");
            } else {
                position += nTransfered;
                count -= nTransfered;
            }
        }
    }
}
