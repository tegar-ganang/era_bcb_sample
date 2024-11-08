package jwadlib;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * The {@link Lump Lump} class is the parent class of all lump classes that are used 
 * to virtually store lumps from WAD files that have been extracted into a {@link 
 * Wad Wad} object.
 * @author Samuel "insertwackynamehere" Horwitz
 * @version 1.0 Alpha 2
 * @since 1.0
 */
public class Lump {

    /**
     * The name of the lump as a {@link java.lang.String String}.
     * @since 1.0
     */
    protected String name;

    /**
     * The {@link WadByteBuffer WadByteBuffer} that contains the content of the 
     * lump.
     * @since 1.0
     */
    protected WadByteBuffer content;

    /**
     * Creates a {@link Lump Lump} with the specified name, of size 0.
     * @param name the name of the {@link Lump Lump}.
     * @throws jwadlib.UnableToInitializeLumpException if the {@link Lump Lump's} 
     * {@link #initialize() initialize()} method returns false.
     * @since 1.0
     */
    public Lump(String name) throws UnableToInitializeLumpException {
        this(name, 0);
    }

    /**
     * Creates a {@link Lump Lump} with the specified name and size.
     * @param name the name of the {@link Lump Lump}.
     * @param size the size of the {@link Lump Lump}.
     * @throws jwadlib.UnableToInitializeLumpException if the {@link Lump Lump's} 
     * {@link #initialize() initialize()} method returns false.
     * @since 1.0
     */
    public Lump(String name, int size) throws UnableToInitializeLumpException {
        this.name = WadByteBuffer.convertToEightByteString(name);
        content = new WadByteBuffer(size);
        if (!initialize()) {
            throw new UnableToInitializeLumpException("Lump could not be initialized.");
        }
    }

    /**
     * Creates a {@link Lump Lump} with the specified name and data.
     * @param name the name of the {@link Lump Lump}.
     * @param data the data contained in the {@link Lump Lump} as an array of bytes.
     * @throws jwadlib.UnableToInitializeLumpException if the {@link Lump Lump's} 
     * {@link #initialize() initialize()} method returns false.
     * @since 1.0
     */
    public Lump(String name, byte[] data) throws UnableToInitializeLumpException {
        this(name, new WadByteBuffer(data));
    }

    /**
     * Creates a {@link Lump Lump} with the specified name and data.
     * @param name the name of the {@link Lump Lump}.
     * @param data the data contained in the {@link Lump Lump} as a {@link WadByteBuffer 
     * WadByteBuffer}.
     * @throws jwadlib.UnableToInitializeLumpException if the {@link Lump Lump's} 
     * {@link #initialize() initialize()} method returns false.
     * @since 1.0
     */
    public Lump(String name, WadByteBuffer data) throws UnableToInitializeLumpException {
        this.name = WadByteBuffer.convertToEightByteString(name);
        content = data;
        if (!initialize()) {
            throw new UnableToInitializeLumpException("Lump could not be initialized.");
        }
    }

    /**
     * Creates a {@link Lump Lump} object from the information in the WAD file's 
     * directory.  The actual lump data is not extracted, however the information 
     * needed to extract the data is passed into the object, along with the name 
     * of the lump and the WAD that is found in.  The lump itself is actually 
     * extracted from the WAD file when {@link Lump Lump's} {@link #getRawLumpData() 
     * getRawLumpData()} method is called. This is done to save memory and time by 
     * storing only pointers to the data as opposed to the data itself, unless 
     * necessary.
     * @param name the name of lump as a {@link java.lang.String String}.
     * @param size the size, in bytes, of the lump data.
     * @param filechannel the parent {@link Wad Wad's} {@link java.nio.channels.FileChannel 
     * FileChannel}.
     * @param pointer the starting location in the WAD file of the lump data.
     * @throws jwadlib.UnableToInitializeLumpException if the {@link Lump Lump's} 
     * {@link #initialize() initialize()} method returns false.
     * @throws jwadlib.UnableToReadWADFileException if the WAD file cannot be read.
     * @since 1.0
     */
    public Lump(String name, int size, FileChannel filechannel, int pointer) throws UnableToInitializeLumpException, UnableToReadWADFileException {
        this.name = WadByteBuffer.convertToEightByteString(name);
        if (size == 0) {
            content = new WadByteBuffer(0);
        } else {
            content = new WadByteBuffer(filechannel, size, pointer);
        }
        if (!initialize()) {
            throw new UnableToInitializeLumpException("Lump could not be initialized.");
        }
    }

    /**
     * This method is called at the end of every {@link Lump Lump} constructor. 
     * It can be overridden by classes that extend the {@link Lump Lump} class 
     * snd the overridden methods will be called instead.
     * @return true if {@link Lump Lump} is initialized successfully.
     * @since 1.0
     */
    private boolean initialize() {
        return true;
    }

    /**
     * Returns the name of the lump as a {@link java.lang.String String}.
     * @return the name of the lump as a {@link java.lang.String String}.
     * @since 1.0
     */
    public String getName() {
        return name;
    }

    /**
     * Returns true if and only if the lump is virtual (has a declared 
     * size of zero bytes).
     * @return true if and only if the lump is virtual, otherwise false.
     * @since 1.0
     */
    public boolean isVirtual() {
        if (content.getCapacity() == 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the capacity of the {@link Lump Lump} data to 0.
     * @return true if completed successfully.
     * @since 1.0
     */
    public boolean makeVirtual() {
        content = new WadByteBuffer(0);
        return true;
    }

    /**
     * Sets and returns the actual data of the lump from the WAD file.  After this 
     * method has been called, the {@link Lump Lump} object will store the data 
     * in memory as opposed to retrieving it from the WAD file.
     * @return the lump data as an {@link java.util.ArrayList ArrayList}.
     * @since 1.0
     */
    public WadByteBuffer getRawLumpData() {
        return content;
    }

    /**
     * Returns the number of bytes in the lump.
     * @return the number of bytes in the lump.
     * @since 1.0
     */
    public int getNumberOfBytes() {
        return content.getLength();
    }

    /**
     * Returns the size of the lump, in bytes. It should be noted that this is not 
     * the number of bytes currently held in the {@link Lump Lump's} {@link 
     * #content content}, but instead how many bytes the {@link WadByteBuffer 
     * WadByteBuffer} backing the {@link Lump Lump} could hold.
     * @return the size of the lump, in bytes.
     * @since 1.0
     */
    public int getSize() {
        return content.getCapacity();
    }

    /**
     * Changes the name of the lump to an eight byte {@link java.lang.String String} 
     * that is specified and passed through {@link WadByteBuffer WadByteBuffer's} 
     * {@link WadByteBuffer#convertToEightByteString(java.lang.String) convertToEightByteString} 
     * method.
     * @param name the new name as a {@link java.lang.String String}.
     * @since 1.0
     */
    public void changeName(String name) {
        this.name = WadByteBuffer.convertToEightByteString(name);
    }

    /**
     * Changes the {@link Lump Lump's} data with the specified array of bytes.
     * @param data the new lump data as a byte array.
     * @return true when the lump data is updated.
     * @since 1.0
     */
    public boolean alterRawLumpData(byte[] data) {
        return alterRawLumpData(new WadByteBuffer(data));
    }

    /**
     * Changes the {@link Lump Lump's} data with the specified {@link WadByteBuffer 
     * WadByteBuffer}.
     * @param data the new lump data as a {@link WadByteBuffer WadByteBuffer}.
     * @return true when the lump data is updated.
     * @since 1.0
     */
    public boolean alterRawLumpData(WadByteBuffer data) {
        content = data;
        content.setPosition(0);
        return true;
    }

    /**
     * Append raw byte data to the content of a {@link Lump Lump}.
     * @param data the data to be appended as an array of bytes.
     * @return true if completed successfully.
     * @since 1.0
     */
    public boolean appendRawLumpData(byte[] data) {
        return appendRawLumpData(new WadByteBuffer(data));
    }

    /**
     * Append raw byte data to the content of a {@link Lump Lump}.
     * @param data the data to be appended as a {@link WadByteBuffer WadByteBuffer}.
     * @return true if completed successfully.
     * @since 1.0
     */
    public boolean appendRawLumpData(WadByteBuffer data) {
        content.alterBufferSize(content.getLength() + data.getLength());
        content.put(data);
        return true;
    }

    /**
     * Writes the {@link Lump Lump} to a binary file with the {@link Lump Lump's} name and 
     * the extension ".lmp".
     * @param directory the directory to write the lump file to.
     * @return true if completed successfully.
     * @throws jwadlib.UnableToWriteLumpFileException if the lump file cannot be written to.
     * @since 1.0
     */
    public boolean writeToFile(File directory) throws UnableToWriteLumpFileException {
        return writeToFile(directory, name);
    }

    /**
     * Writes the {@link Lump Lump} to a binary file with a custom name and the extension 
     * ".lmp".
     * @param directory the directory to write the lump file to.
     * @param filename the filename of the lump. Do not include an extension, the 
     * extension will be ".lmp".
     * @return true if completed successfully.
     * @throws jwadlib.UnableToWriteLumpFileException if the lump file cannot be written to.
     * @since 1.0
     */
    public boolean writeToFile(File directory, String filename) throws UnableToWriteLumpFileException {
        if (!directory.isDirectory()) {
            throw new UnableToWriteLumpFileException("Invalid directory.");
        }
        filename = filename.replaceAll("[\\x00]", "");
        filename = filename.replaceAll("[\\[\\]\\\\]", "_");
        File temp = new File(directory.getPath(), filename + ".lmp");
        int numericextension = 2;
        while (temp.exists()) {
            temp = new File(directory, filename + ".lmp" + "." + numericextension);
            numericextension++;
        }
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(temp, "rw");
        } catch (FileNotFoundException e) {
            throw new UnableToWriteLumpFileException("Random Access File could not be created.", e);
        }
        return writeToFile(raf);
    }

    /**
     * Writes the {@link Lump Lump} to a binary file.
     * @param file the {@link java.io.RandomAccessFile RandomAccessFile} to write to.
     * @return true if the file has been successfully written to.
     * @throws UnableToWriteLumpFileException if the specified file cannot be written to.
     * @since 1.0
     */
    public boolean writeToFile(RandomAccessFile file) throws UnableToWriteLumpFileException {
        return writeToFile(file.getChannel());
    }

    /**
     * Writes the {@link Lump Lump} to a binary file.
     * @param filechannel the {@link java.nio.channels.FileChannel FileChannel} to write to.
     * @return true if the file has been successfully written to.
     * @throws UnableToWriteLumpFileException if the specified file cannot be written to.
     * @since 1.0
     */
    public boolean writeToFile(FileChannel filechannel) throws UnableToWriteLumpFileException {
        return writeToFile(filechannel, 0);
    }

    /**
     * Writes the {@link Lump Lump} to a binary file at the specified position.
     * @param filechannel the {@link java.nio.channels.FileChannel FileChannel} to write to.
     * @param position the position in the {@link java.nio.channels.FileChannel FileChannel} 
     * to begin writing at.
     * @return true if the file has been successfully written to.
     * @throws UnableToWriteLumpFileException if the specified file cannot be written to.
     * @since 1.0
     */
    public boolean writeToFile(FileChannel filechannel, long position) throws UnableToWriteLumpFileException {
        int oldpos = content.getPosition();
        content.setPosition(0);
        try {
            filechannel.position(position);
        } catch (IOException e) {
            throw new UnableToWriteLumpFileException("The specified position is out of bounds.", e);
        }
        try {
            filechannel.write(content.getByteBuffer());
        } catch (IOException e) {
            throw new UnableToWriteLumpFileException("The lump file could not be written.", e);
        }
        content.setPosition(oldpos);
        return true;
    }

    /**
     * Returns the name of the {@link Lump Lump} by calling the {@link Lump#getName() getName()} 
     * method. Overrides {@link java.lang.Object java.lang.Object's} 
     * {@link java.lang.Object#toString() toString()} method.
     * @return the name of the {@link Lump Lump}.
     * @since 1.0
     */
    @Override
    public String toString() {
        return getName();
    }
}
