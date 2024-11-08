package de.enough.polish.io;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>A data input stream that logs all read data and provides it later as a byte array.</p>
 * <p>This stream is useful for getting the accessed data for later storage.</p>
 * <p>Example usage:
 * 	private void loadAndStoreData( InputStream in) {
 * 		RecordingDataInputStream dataIn = new RecordingDataInputStream( in );
 * 		load( dataIn );
 * 		byte[] readData = dataIn.getRecordedData();
 * 		store( readData );
 *  }
 * </p>
 *
 * <p>Copyright Enough Software 2007 - 2009</p>
 * <pre>
 * history
 *        Nov 27, 2007 - rob creation
 * </pre>
 * @author Robert Virkus, j2mepolish@enough.de
 */
public class RecordingDataInputStream extends InputStream implements DataInput {

    private ByteArrayOutputStream byteOut;

    private DataOutputStream dataOut;

    private final DataInputStream dataIn;

    /**
	 * 
	 * @param in the input stream
	 */
    public RecordingDataInputStream(InputStream in) {
        this.dataIn = new DataInputStream(in);
        this.byteOut = new ByteArrayOutputStream();
        this.dataOut = new DataOutputStream(this.byteOut);
    }

    /**
	 * Retrieves the data that has been read since the beginning or since the last clearRecordedData() call.
	 * @return the read data as an byte array
	 * @see #clearRecordedData()
	 */
    public byte[] getRecordedData() {
        try {
            this.dataOut.flush();
        } catch (IOException e) {
        }
        return this.byteOut.toByteArray();
    }

    /**
	 * Resets the data recording and deletes all previously recorded data
	 */
    public void clearRecordedData() {
        this.byteOut = new ByteArrayOutputStream();
        this.dataOut = new DataOutputStream(this.byteOut);
    }

    public int read() throws IOException {
        int result = this.dataIn.read();
        this.dataOut.writeByte(result);
        return result;
    }

    public boolean readBoolean() throws IOException {
        boolean result = this.dataIn.readBoolean();
        this.dataOut.writeBoolean(result);
        return result;
    }

    public byte readByte() throws IOException {
        byte result = this.dataIn.readByte();
        this.dataOut.writeByte(result);
        return result;
    }

    public char readChar() throws IOException {
        char result = this.dataIn.readChar();
        this.dataOut.writeChar(result);
        return result;
    }

    public double readDouble() throws IOException {
        double result = this.dataIn.readDouble();
        this.dataOut.writeDouble(result);
        return result;
    }

    public float readFloat() throws IOException {
        float result = this.dataIn.readFloat();
        this.dataOut.writeFloat(result);
        return result;
    }

    public void readFully(byte[] buffer) throws IOException {
        readFully(buffer, 0, buffer.length);
    }

    public void readFully(byte[] buffer, int offset, int len) throws IOException {
        this.dataIn.readFully(buffer, offset, len);
        this.dataOut.write(buffer, offset, len);
    }

    public int readInt() throws IOException {
        int result = this.dataIn.readInt();
        this.dataOut.writeInt(result);
        return result;
    }

    public String readLine() throws IOException {
        return readUTF();
    }

    public long readLong() throws IOException {
        long result = this.dataIn.readLong();
        this.dataOut.writeLong(result);
        return result;
    }

    public short readShort() throws IOException {
        short result = this.dataIn.readShort();
        this.dataOut.writeShort(result);
        return result;
    }

    public String readUTF() throws IOException {
        String result = this.dataIn.readUTF();
        this.dataOut.writeUTF(result);
        return result;
    }

    public int readUnsignedByte() throws IOException {
        int result = this.dataIn.readUnsignedByte();
        this.dataOut.writeByte(result);
        return result;
    }

    public int readUnsignedShort() throws IOException {
        int result = this.dataIn.readUnsignedShort();
        this.dataOut.writeShort(result);
        return result;
    }

    public int skipBytes(int len) throws IOException {
        int result = this.dataIn.skipBytes(len);
        return result;
    }

    public int available() throws IOException {
        return this.dataIn.available();
    }

    public void close() throws IOException {
        this.dataIn.close();
    }

    public synchronized void mark(int readlimit) {
        this.dataIn.mark(readlimit);
    }

    public boolean markSupported() {
        return this.dataIn.markSupported();
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int read = this.dataIn.read(b, off, len);
        this.dataOut.write(b, off, read);
        return read;
    }

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public synchronized void reset() throws IOException {
        this.dataIn.reset();
    }

    public long skip(long n) throws IOException {
        return this.dataIn.skip(n);
    }
}
