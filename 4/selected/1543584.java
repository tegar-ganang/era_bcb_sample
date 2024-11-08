package net.sf.joafip.file.entity;

import org.apache.log4j.Logger;
import net.sf.joafip.NotStorableClass;
import net.sf.joafip.file.service.FileForStorable;
import net.sf.joafip.heapfile.entity.EnumFileState;
import net.sf.joafip.heapfile.service.HeapException;

/**
 * all that is common to storable in file using {@link FileForStorable}:<br>
 * <ul>
 * <li>the {@link FileForStorable} manager to use</li>
 * <li>position of data in file</li>
 * <li>flags for first creation and state change</li>
 * <li>primitive type read and write in file</li>
 * <li>crc32 compute and check for data integrity check</li>
 * </ul>
 * 
 * by extention make able to create mutable or inmmutable object, immutable are
 * for file record unique representation, mutable make able to change the file
 * record representated<br>
 * 
 * @author luc peuvrier
 * 
 */
@NotStorableClass
public abstract class AbstractFileStorable implements IFileStorable {

    protected final Logger _log = Logger.getLogger(getClass());

    private static final String ALREADY_READED = "already readed";

    private static final String RECORD_INTEGRITY_ERROR = "record integrity error";

    private static final String NO_VALUE_CHANGE_TO_WRITE = "no value change to write";

    private static final String READING_LOOSE_VALUE_CHANGE = "reading loose value change";

    /** position in file of this in marshallized forms */
    protected long positionInFile;

    /** length of this file storable */
    private Integer length;

    /** to read/write this in file */
    protected FileForStorable fileForStorable;

    /** to compute crc32 record header */
    protected int crc32;

    /** current number of byte readed or writed */
    private int numberOfByteReadedWrited = 0;

    /**
	 * true if this value changed, creation implies no value to save since not
	 * read and not write
	 */
    protected boolean valueChanged = false;

    /** true if just created in memory, never read from file */
    protected boolean justCreated = true;

    /**
	 * mutable construction<br>
	 * 
	 */
    protected AbstractFileStorable() {
        super();
    }

    public void clear() {
        valueChanged = false;
        justCreated = true;
    }

    /**
	 * immutable construction<br>
	 * 
	 * @param positionInFile
	 */
    public AbstractFileStorable(final long positionInFile, final Integer length) {
        super();
        this.positionInFile = positionInFile;
        this.length = length;
    }

    /**
	 * set position in file, only for specialization where the same object can
	 * represent any data area in heap file<br>
	 * 
	 * @param positionInFile
	 *            position of record in file
	 */
    protected void setPositionInFile(final long positionInFile) {
        this.positionInFile = positionInFile;
    }

    public void setLength(final int length) throws HeapException {
        this.length = length;
        if (numberOfByteReadedWrited > this.length) {
            throw new HeapException("too much byte readed/writed=" + numberOfByteReadedWrited + " length=" + length);
        }
    }

    public void resetNumberOfByteReadedWrited() {
        this.numberOfByteReadedWrited = 0;
    }

    public long getPositionInFile() {
        return positionInFile;
    }

    public void readFromFile(final FileForStorable fileForStorable) throws HeapException {
        if (!justCreated) {
            throw new HeapException(ALREADY_READED);
        }
        if (valueChanged) {
            throw new HeapException(READING_LOOSE_VALUE_CHANGE);
        }
        this.fileForStorable = fileForStorable;
        fileForStorable.seek(positionInFile);
        crc32 = 0;
        numberOfByteReadedWrited = 0;
        unmarshallImpl();
        justCreated = false;
    }

    public void writeToFile(final FileForStorable fileForStorable) throws HeapException {
        if (!valueChanged) {
            throw new HeapException(NO_VALUE_CHANGE_TO_WRITE);
        }
        this.fileForStorable = fileForStorable;
        fileForStorable.seek(positionInFile);
        crc32 = 0;
        numberOfByteReadedWrited = 0;
        marshallImpl();
        valueChanged = false;
    }

    /**
	 * unmarshaling implementation for read<br>
	 * {@link #crc32} is set to zero<br>
	 * 
	 * @throws HeapException
	 */
    protected abstract void unmarshallImpl() throws HeapException;

    /**
	 * marshaling implementation for write<br>
	 * {@link #crc32} is set to zero<br>
	 * 
	 * @throws HeapException
	 */
    protected abstract void marshallImpl() throws HeapException;

    /**
	 * 
	 * @return true if just created ( not read from file and not setted )
	 */
    public boolean isJustCreated() {
        return justCreated;
    }

    /**
	 * to know if the value state change
	 * 
	 * @return true if value changed
	 */
    public boolean isValueChanged() {
        return valueChanged;
    }

    public void setValueIsChanged() {
        valueChanged = true;
        justCreated = false;
    }

    /**
	 * update crc32 with byte value in buffer
	 * 
	 * @param buffer
	 *            bytes for crc32 update
	 */
    private void updateCrc32(final byte[] buffer) {
        for (int i = 0; i < buffer.length; i++) {
            final int byteValue = buffer[i];
            for (int j = 0; j < 8; j++) {
                final int testbit = ((crc32 >> 31) & 1) ^ ((byteValue >> j) & 1);
                crc32 <<= 2;
                if (testbit != 0) {
                    crc32 ^= 0x8005;
                }
            }
        }
    }

    /**
	 * read and check CRC32
	 * 
	 * @throws HeapException
	 *             read error crc32 error in record
	 */
    protected void readAndCheckCrc32() throws HeapException {
        final int computedCrc32 = crc32;
        final int readedCrc32 = readInteger();
        if (computedCrc32 != readedCrc32) {
            throw new HeapException(RECORD_INTEGRITY_ERROR + " record position=" + positionInFile + " readed=" + readedCrc32 + " computed=" + computedCrc32 + " length=" + length + " nb rd/wr=" + numberOfByteReadedWrited, EnumFileState.STATE_CORRUPTED);
        }
    }

    /**
	 * read bytes
	 * 
	 * @param length
	 *            number of bytes to read
	 * @return the bytes readed
	 * @throws HeapException
	 */
    protected byte[] readBytes(final int length) throws HeapException {
        numberOfByteReadedWrited += length;
        if (this.length != null && numberOfByteReadedWrited > this.length) {
            throw new HeapException("too much byte readed=" + numberOfByteReadedWrited + " length=" + this.length);
        }
        final byte[] dataByteArray = new byte[length];
        fileForStorable.read(dataByteArray);
        updateCrc32(dataByteArray);
        return dataByteArray;
    }

    /**
	 * unmarshall from byte array current {@link #dataByteArrayIndex} index to
	 * long<br>
	 * {@link #dataByteArrayIndex} is updated to next data to read position<br>
	 * 
	 * @param dataByteArray
	 * @return the unmarshalled value
	 * @throws HeapException
	 */
    protected long readLong() throws HeapException {
        final byte[] dataByteArray = readBytes(8);
        long value = 0;
        for (int byteIndex = 7; byteIndex >= 0; byteIndex--) {
            value = (value << 8) | (((long) dataByteArray[byteIndex]) & 0xff);
        }
        return value;
    }

    protected int readInteger() throws HeapException {
        final byte[] dataByteArray = readBytes(4);
        int value = 0;
        for (int byteIndex = 3; byteIndex >= 0; byteIndex--) {
            value = (value << 8) | (((int) dataByteArray[byteIndex]) & 0xff);
        }
        return value;
    }

    protected boolean readBoolean() throws HeapException {
        final byte[] dataByteArray = readBytes(1);
        final int byteValue = ((int) dataByteArray[0]) & 0xff;
        return byteValue == 0 ? false : true;
    }

    protected byte readByte() throws HeapException {
        final byte[] dataByteArray = readBytes(1);
        return dataByteArray[0];
    }

    /**
	 * write bytes
	 * 
	 * @param dataByteArray
	 *            bytes to write
	 * @throws HeapException
	 */
    protected void writeBytes(final byte[] dataByteArray) throws HeapException {
        numberOfByteReadedWrited += dataByteArray.length;
        if (this.length != null && numberOfByteReadedWrited > this.length) {
            throw new HeapException("too much byte writed");
        }
        fileForStorable.write(dataByteArray);
        updateCrc32(dataByteArray);
    }

    /**
	 * marshall long value to byte array current {@link #dataByteArrayIndex}
	 * index<br>
	 * {@link #dataByteArrayIndex} is updated to next data to write position<br>
	 * 
	 * @param value
	 *            the value to marshall
	 * @throws HeapException
	 */
    protected void writeLong(final long value) throws HeapException {
        final byte[] dataByteArray = new byte[8];
        long localValue = value;
        for (int byteIndex = 0; byteIndex < 8; byteIndex++) {
            dataByteArray[byteIndex] = (byte) localValue;
            localValue >>= 8;
        }
        writeBytes(dataByteArray);
    }

    protected void writeInteger(final int value) throws HeapException {
        final byte[] dataByteArray = new byte[4];
        int localValue = value;
        for (int byteIndex = 0; byteIndex < 4; byteIndex++) {
            dataByteArray[byteIndex] = (byte) localValue;
            localValue >>= 8;
        }
        writeBytes(dataByteArray);
    }

    protected void writeBoolean(final boolean value) throws HeapException {
        final byte[] dataByteArray = new byte[1];
        dataByteArray[0] = (byte) (value ? 1 : 0);
        writeBytes(dataByteArray);
    }

    protected void writeByte(final byte value) throws HeapException {
        final byte[] dataByteArray = new byte[1];
        dataByteArray[0] = value;
        writeBytes(dataByteArray);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (int) (positionInFile ^ (positionInFile >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        final boolean equals;
        if (obj == null) {
            equals = false;
        } else if (obj == this) {
            equals = true;
        } else if (obj instanceof AbstractFileStorable) {
            final AbstractFileStorable storable = (AbstractFileStorable) obj;
            equals = positionInFile == storable.positionInFile;
        } else {
            equals = false;
        }
        return equals;
    }
}
