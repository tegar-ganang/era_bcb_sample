package de.dgrid.bisgrid.services.proxy.redirect.util;

import de.dgrid.bisgrid.services.proxy.redirect.*;
import de.dgrid.bisgrid.services.proxy.redirect.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;

/**
 * Class to hold a Byte Buffer including the
 * length of the content currently
 * stored in the buffer.
 */
public final class ByteArrayBuffer {

    private static Logger log = Logger.getLogger(ByteArrayBuffer.class);

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    private int readPointer;

    private int initBufferSIze;

    private ByteArrayOutputStream dest;

    public static final int CONNECTION_CLOSED = -2;

    public static final int NO_MORE_DATA = -1;

    public ByteArrayBuffer(int initBufferSize) {
        super();
        this.initBufferSIze = initBufferSize;
        this.data = new byte[0];
        this.readPointer = 0;
        this.dest = new ByteArrayOutputStream(initBufferSize);
    }

    /**
     * Fill the buffer.
     * @return -1 == no more data from server
     *         -2 == connection closed.
     */
    public int read(InputStream source) {
        return read(source, -1);
    }

    /**
     * Fill the buffer. Any existing data is overwritten
     * @return -1 == no more data from server
     *         -2 == connection closed.
     */
    public int read(InputStream source, int linit) {
        log.debug("Start reading");
        int BytesFromServer = 0;
        byte[] TmpBuffer = null;
        try {
            int BytesAvailable;
            BytesAvailable = source.available();
            log.debug("available:" + String.valueOf(BytesAvailable));
            if (BytesAvailable == -1) {
                return NO_MORE_DATA;
            }
            int MaxRead;
            if (BytesAvailable > 0) {
                MaxRead = BytesAvailable;
            } else {
                MaxRead = this.initBufferSIze;
            }
            if ((linit > 0) && (MaxRead > linit)) {
                MaxRead = linit;
            }
            TmpBuffer = new byte[MaxRead];
            log.info("Start to read " + String.valueOf(MaxRead) + " byte.");
            BytesFromServer = source.read(TmpBuffer, 0, MaxRead);
            if (BytesFromServer == -1) {
                log.info("Server returned -1 byte; EOF");
                return NO_MORE_DATA;
            }
            this.dest.write(TmpBuffer, 0, BytesFromServer);
            this.data = this.dest.toByteArray();
            log.info("Server returned " + String.valueOf(BytesFromServer) + " byte. Now " + String.valueOf(this.data.length) + " are in the buffer.");
            if (log.isDebugEnabled()) {
                log.debug("Buffer>>>");
                log.debug(Util.toHex(this.data, 0, this.data.length - 1));
                log.debug("<<<");
            }
        } catch (IOException e) {
            log.debug(e.getMessage());
            return CONNECTION_CLOSED;
        } finally {
            log.debug("end reading");
        }
        log.debug("ByteArrayBuffer::Number of bytes in buffer:" + String.valueOf((this.data == null) ? 0 : this.data.length));
        return BytesFromServer;
    }

    /**
     * This method reads a fixed number of bytes from the
     * stream and stores them in the buffer.
     * If the client sends the data in multiple chunks,
     * this may result in multiple read operations.
     *
     * @param Source The stream from which the data is read.
     * @param Size The number of bytes that we are reading.
     *
     * If the stream is closed before the required number
     * of bytes can be read, this method returns the
     * value "-1" and the bytes that have been received from
     * the stream are stored in this buffer.
     *
     * @return -1 == no more data from server
     *         -2 == connection closed.
     */
    public int readExact(InputStream source, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("readExact with negative size argument");
        }
        int bytesMissing = size;
        while (bytesMissing > 0) {
            int readResult = read(source, bytesMissing);
            if (readResult < 0) {
                log.warn("Inputstream closed while reading " + String.valueOf(size) + " bytes.");
                return readResult;
            }
            bytesMissing -= readResult;
            if (bytesMissing < 0) {
                throw new IllegalArgumentException("Too many bytes received from input stream.");
            }
        }
        return size;
    }

    /**
     * Read a single byte from the stream.
     *
     * @returns         >=0 The value
     *                  CONNECTION_CLOSED=-2;
     *                  NO_MORE_DATA=-1;
     */
    private int readSingle(InputStream source) {
        boolean block = false;
        int result;
        try {
            if (log.isDebugEnabled()) {
                int BytesAvailable;
                BytesAvailable = source.available();
                if (BytesAvailable < 0) {
                    log.debug("Read of single byte will block...");
                    block = true;
                }
            }
            result = source.read();
            if (log.isDebugEnabled()) {
                if (block) {
                    log.debug("Blocked read returned: " + String.valueOf(result));
                }
            }
        } catch (IOException e) {
            log.debug(e.getMessage());
            result = CONNECTION_CLOSED;
        }
        return result;
    }

    /**
     * Read a HTTP Header from the input stream. The HTTP Header ends with an
     * empty line.
     *
     * The data of the header can be received using "getProcessedPart"
     *
     * @param Source    Source stream of the HTTP header
     * @return         >=0 --> length for the HTTP header
     *                  CONNECTION_CLOSED=-2;
     *                  NO_MORE_DATA=-1;
     */
    public int readHttpHeader(InputStream source) {
        int Result = 0;
        byte CurrentValue = 0;
        byte PreviousValue = 0;
        int HeaderSize = 0;
        while ((Result = readSingle(source)) >= 0) {
            HeaderSize++;
            CurrentValue = (byte) Result;
            this.dest.write(Result);
            if ((CurrentValue == 10) && (PreviousValue == 10)) {
                log.info("End-of-Header");
                Result = HeaderSize;
                break;
            }
            if (CurrentValue != 13) {
                PreviousValue = CurrentValue;
            }
        }
        this.data = this.dest.toByteArray();
        return Result;
    }

    /**
     * Read one chunk of data from the source.
     * A "Chunk" is defined in "RF2616" as
     * <size in hex><cr><lf>
     * <data><cr><lf>
     *
     * The size includes the line which defined the
     * size and the trailing <cr><lf>
     *
     * The data is stored in "this" buffer. The
     * chunk-size definition is included in the buffer
     *
     * @return         >=0 --> length for the chunk
     *                  CONNECTION_CLOSED=-2;
     *                  NO_MORE_DATA=-1;
     */
    public int readChunk(InputStream Source) throws HttpException {
        StringBuffer ChunkSizeAsString = new StringBuffer();
        int Result;
        byte CurrentValue;
        char CC;
        int ChunkSize = 0;
        int ProcessedBytes = 0;
        while ((Result = readSingle(Source)) >= 0) {
            CurrentValue = (byte) Result;
            ProcessedBytes++;
            this.dest.write(Result);
            if (CurrentValue == 10) {
                log.debug("End-of-Chunk-Header");
                try {
                    ChunkSize = Integer.parseInt(ChunkSizeAsString.toString(), 16);
                    if (ChunkSize == 0) {
                        boolean NewLineAdded = false;
                        try {
                            if (Source.available() >= 2) {
                                Result = readSingle(Source);
                                this.dest.write(Result);
                                Result = readSingle(Source);
                                this.dest.write(Result);
                                NewLineAdded = true;
                            }
                        } catch (IOException Ex) {
                        }
                        if (NewLineAdded == false) {
                            try {
                                this.dest.write("\r\n".getBytes());
                            } catch (IOException e) {
                                log.error("Error", e);
                            }
                        }
                        this.data = this.dest.toByteArray();
                        return 0;
                    }
                } catch (NumberFormatException E) {
                    HttpException E1 = new HttpException("Error in chunk size defintion. Size string=\'" + ChunkSizeAsString + "\'" + " Buffer Content=" + this.toString());
                    log.error(E1.toString());
                    throw E1;
                }
                break;
            }
            CC = (char) CurrentValue;
            if (Character.digit(CC, 16) >= 0) {
                ChunkSizeAsString.append(CC);
            }
        }
        if (Result <= 0) {
            log.warn("Result = " + String.valueOf(Result) + " after reading chunk size line ");
            return Result;
        }
        log.debug("Chunk size = " + String.valueOf(ChunkSize));
        Result = readExact(Source, ChunkSize + 2);
        return ChunkSize;
    }

    /**
     * Read the next byte from the buffer
     */
    public byte next() {
        byte Result = data[readPointer];
        readPointer++;
        return Result;
    }

    public boolean hasNext() {
        return readPointer < this.data.length;
    }

    /**
     * Get the part of the buffer
     * that has already been processed.
     *
     * Note: this will not change the read position
     */
    public ByteArrayBuffer getProcessed() {
        ByteArrayBuffer Result = new ByteArrayBuffer(this.readPointer);
        Result.data = new byte[this.readPointer];
        System.arraycopy(this.data, 0, Result.data, 0, this.readPointer);
        return Result;
    }

    /**
     * Get the part of the buffer
     * that has not yet been processed.
     *
     * Note: this will not change the read position
     */
    public ByteArrayBuffer getRemaining() {
        int RemainingSize = getBytesRemaining();
        ByteArrayBuffer Result = new ByteArrayBuffer(RemainingSize);
        Result.data = new byte[RemainingSize];
        System.arraycopy(this.data, readPointer, Result.data, 0, this.data.length - readPointer);
        return Result;
    }

    /**
     * Get the part of the buffer
     * that has not yet been processed.
     *
     * Note: this will not change the read position
     */
    public byte[] getRemainingAsArray() {
        int RemainingSize = getBytesRemaining();
        byte[] Result = new byte[RemainingSize];
        if (RemainingSize > 0) {
            System.arraycopy(this.data, readPointer, Result, 0, this.data.length - readPointer);
        }
        return Result;
    }

    /**
     * Write the unprocessed part of the buffer to a stream.
     * After writing it to a stream, the data is "processed",
     * no more date remains in the buffer.
     */
    public void writeTo(OutputStream Destination) throws IOException {
        int BytesRemaining = getBytesRemaining();
        if (BytesRemaining > 0) {
            Destination.write(this.data, this.readPointer, BytesRemaining);
            this.readPointer = this.data.length;
        }
    }

    /**
     * Remove all the data from
     * the buffer
     */
    public void clear() {
        this.data = null;
        this.readPointer = 0;
        this.dest.reset();
    }

    /**
     * For debugging
     * @return The content of the buffer as string
     */
    @Override
    public String toString() {
        StringBuffer Result = new StringBuffer(this.data.length);
        for (int Index = 0; Index < this.data.length; Index++) {
            Result.append((char) data[Index]);
        }
        return Result.toString();
    }

    /**
     * Get the number of bytes that a client
     * can read from this buffer.
     */
    public int getBytesRemaining() {
        if (this.data == null) {
            return 0;
        }
        return this.data.length - this.readPointer;
    }
}
