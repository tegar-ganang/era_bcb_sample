package org.rendezvous.avconf;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

/**
 * <p>Abstracts a read interface that pushes data in the form of Buffer objects.
 * This interface allows a source stream to transfer data in the form of an
 * entire media chunk to the user of this source stream. The media object or
 * chunk transferred is the Buffer object as defined in javax.media.Buffer.
 * The user of the stream will allocate an empty Buffer object and pass this
 * over to the source stream in the read() method. The source stream will
 * allocate the Buffer object's data and header, set them on the Buffer and
 * send them over to the user.</p>
 */
public class AVStream implements PushBufferStream {

    protected Format mFormat;

    protected Control[] mControls = new Control[0];

    protected BufferTransferHandler transferHandler = null;

    private Buffer mBuffer = new Buffer();

    private int writeCount = 0;

    private int readCount = 0;

    /**
   * Create a data stream for the specified format.
   * @param fmt Format of Video Buffers in this data stream.
   */
    public AVStream(Format fmt) {
        mFormat = fmt;
    }

    /**
   * Get the current content type for this stream.
   * @return ContentDescriptor.RAW.
   */
    public ContentDescriptor getContentDescriptor() {
        return new ContentDescriptor(ContentDescriptor.RAW);
    }

    /**
   * Get the length of the Video stream.
   * @return LENGTH_UNKNOWN
   */
    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }

    /**
   * Check whether or not the end of the stream is reached = false.
   * @return false
   */
    public boolean endOfStream() {
        return false;
    }

    /**
   * Get the Video format for the data in this stream.
   * @return the format associated with this data stream
   */
    public Format getFormat() {
        return mFormat;
    }

    /**
   * Place a Buffer of Video data in the stream and signal the transfer of data.
   * @param data byte array of Video data.
   */
    public void write(Buffer data) {
        if (!data.getFormat().getEncoding().equals(mFormat.getEncoding())) {
            System.out.println("AVStream: data encoding switch to " + data.getFormat().getEncoding());
            mFormat = data.getFormat();
        }
        synchronized (mBuffer) {
            mBuffer.setData(data.getData());
            mBuffer.setOffset(data.getOffset());
            mBuffer.setFormat(data.getFormat());
            mBuffer.setSequenceNumber(data.getSequenceNumber());
            mBuffer.setLength(data.getLength());
            mBuffer.setFlags(data.getFlags());
            mBuffer.setHeader(data.getHeader());
            mBuffer.setEOM(data.isEOM());
            writeCount++;
            if (transferHandler != null) transferHandler.transferData(this);
        }
    }

    /**
   * Read a Buffer of data from the stream.
   * @param buffer Buffer passed in where data is to be placed.
   */
    public void read(Buffer buffer) throws java.io.IOException {
        synchronized (mBuffer) {
            if (mBuffer == null) {
                System.out.println("AVStream: error, mBuffer = null");
            } else if (readCount == writeCount) {
                System.out.println("AVStream: buffer already read");
            } else {
                buffer.setData(mBuffer.getData());
                buffer.setOffset(mBuffer.getOffset());
                buffer.setFormat(mBuffer.getFormat());
                buffer.setSequenceNumber(mBuffer.getSequenceNumber());
                buffer.setLength(mBuffer.getLength());
                buffer.setFlags(mBuffer.getFlags());
                buffer.setHeader(mBuffer.getHeader());
                buffer.setEOM(mBuffer.isEOM());
                readCount++;
            }
        }
    }

    /**
   * Register an object to service data transfers to this stream.
   * @param transferHandler The handler to transfer data to.
   */
    public void setTransferHandler(BufferTransferHandler transferHandler) {
        synchronized (this) {
            this.transferHandler = transferHandler;
            notifyAll();
        }
    }

    /**
   * Obtain the collection of objects that control the object that implements
   * this interface. If no controls are supported, a zero length array is returned.
   * @return the collection of object controls
   */
    public Object[] getControls() {
        return mControls;
    }

    /**
   * Obtain the object that implements the specified Class or Interface.
   * The full class or interface name must be used.
   * If the control is not supported then null is returned.
   * @return the object that implements the control, or null.
   */
    public Object getControl(String controlType) {
        try {
            Class cls = Class.forName(controlType);
            Object cs[] = getControls();
            for (int i = 0; i < cs.length; i++) {
                if (cls.isInstance(cs[i])) return cs[i];
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
