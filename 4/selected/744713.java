package net.sf.javadc.util.hash;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.List;

/**
 * @author theBaz <CODE>HashTreeRAF</CODE> package local class that wraps {@link RandomAccessFile} used to read and
 *         write {@link HashTree}
 */
class HashTreeRAF extends RandomAccessFile {

    /**
     * Creates a random access file stream to read from, and optionally to write to, the file specified by the
     * {@link File} argument. A new {@link FileDescriptor} object is created to represent this file connection.
     * 
     * @param name the system-dependent filename
     * @param mode the access <a href="#mode">mode</a>
     * @exception IllegalArgumentException if the mode argument is not equal to one of <tt>"r"</tt>, <tt>"rw"</tt>,
     *                <tt>"rws"</tt>, or <tt>"rwd"</tt>
     * @exception FileNotFoundException if the file exists but is a directory rather than a regular file, or cannot be
     *                opened or created for any other reason
     * @exception SecurityException if a security manager exists and its <code>checkRead</code> method denies read
     *                access to the file or the mode is "rw" and the security manager's <code>checkWrite</code> method
     *                denies write access to the file
     * @see java.io.RandomAccessFile
     */
    public HashTreeRAF(File file, String mode) throws FileNotFoundException {
        super(file, mode);
    }

    /**
     * Creates a random access file stream to read from, and optionally to write to, a file with the specified name. A
     * new {@link FileDescriptor} object is created to represent the connection to the file.
     * 
     * @param name the system-dependent filename
     * @param mode the access <a href="#mode">mode</a>
     * @exception IllegalArgumentException if the mode argument is not equal to one of <tt>"r"</tt>, <tt>"rw"</tt>,
     *                <tt>"rws"</tt>, or <tt>"rwd"</tt>
     * @exception FileNotFoundException if the file exists but is a directory rather than a regular file, or cannot be
     *                opened or created for any other reason
     * @exception SecurityException if a security manager exists and its <code>checkRead</code> method denies read
     *                access to the file or the mode is "rw" and the security manager's <code>checkWrite</code> method
     *                denies write access to the file
     * @see java.io.RandomAccessFile
     */
    public HashTreeRAF(String name, String mode) throws FileNotFoundException {
        super(name, mode);
    }

    /**
     * Read from data file <code>HashTree</code> of specified file
     * 
     * @param hashInfo Info of the Hashed File
     * @return HashTree for the specified HashInfo
     * @throws IOException if IO Error occurs
     */
    public HashTree readHashTree(HashInfo hashInfo) throws IOException {
        HashTree hashTree = new HashTree(hashInfo);
        seek(hashInfo.getIndex());
        ByteBuffer buffer = ByteBuffer.allocateDirect(HashTree.HASH_SIZE);
        FileChannel fileChannel = getChannel();
        for (int i = 0; i < hashInfo.getLeafNumber(); i++) {
            FileLock lock = fileChannel.lock();
            fileChannel.read(buffer);
            lock.release();
            hashTree.addLeaf(buffer);
        }
        FileLock lock = fileChannel.lock();
        fileChannel.read(buffer);
        lock.release();
        hashTree.setRoot(buffer);
        return hashTree;
    }

    /**
     * Appends <code>{@link HashTree}</code> to data file
     * 
     * @param hashTree HashTree to be appended
     * @return Info of the Hashed File
     * @throws IOException if IO Error occurs
     */
    public HashInfo writeHashTree(HashTree hashTree) throws IOException {
        HashInfo hashInfo = hashTree.getHashInfo();
        long index = this.length();
        hashInfo.setIndex(index);
        return writeHashTree(hashTree, index);
    }

    /**
     * Writes HashTree to data file to specified position use by <code>{@link writeHashTree(HashTree)}</code> to add
     * <code>{@link HashTree}</code>s at bottom of the store and by <code>{@link HashStore}</code> to delete
     * <code>{@link HashTree}</code>s
     * 
     * @param hashTree HashTree to be written
     * @return Info of the Hashed File
     * @throws IOException if IO Error occurs
     * @see writeHashTree(HashTree)
     * @see HashStore#deleteHashTree(HashTree)
     */
    public HashInfo writeHashTree(HashTree hashTree, long position) throws IOException {
        HashInfo hashInfo = hashTree.getHashInfo();
        long index = position;
        if (position > length()) {
            throw new IOException("Position bigger than file length");
        }
        seek(index);
        FileChannel fileChannel = getChannel();
        List leaves = hashTree.getLeaves();
        Iterator iterator = leaves.iterator();
        ByteBuffer buffer;
        while (iterator.hasNext()) {
            buffer = (ByteBuffer) iterator.next();
            buffer.position(0);
            FileLock lock = fileChannel.lock();
            fileChannel.write(buffer);
            lock.release();
            index += HashTree.HASH_SIZE;
            seek(index);
        }
        FileLock lock = fileChannel.lock();
        fileChannel.write(hashTree.getRoot());
        lock.release();
        return hashInfo;
    }
}
