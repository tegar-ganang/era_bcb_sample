package org.archive.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.Serializable;
import org.archive.crawler.checkpoint.ObjectPlusFilesInputStream;
import org.archive.crawler.checkpoint.ObjectPlusFilesOutputStream;

/**
 * FIFO byte queue, using disk space as needed.
 *
 * TODO: add maximum size?
 *
 * Flips between two backing files: as soon as reading head
 * reaches end of one, and is about to start at the front
 * of the other, the writing tail flips to a new file.
 *
 * The current write-target file (tail) has a file extension
 * ".qout", the current read file (head) has a file extension
 * ".qin".
 *
 * @author Gordon Mohr
 */
public class DiskByteQueue implements Serializable {

    private static final String IN_FILE_EXTENSION = ".qin";

    private static final String OUT_FILE_EXTENSION = ".qout";

    File tempDir;

    String backingFilenamePrefix;

    File inFile;

    File outFile;

    transient FlipFileInputStream headStream;

    long rememberedPosition = 0;

    transient FlipFileOutputStream tailStream;

    /**
     * Create a new BiskBackedByteQueue in the given directory with given
     * filename prefix
     *
     * @param tempDir
     * @param backingFilenamePrefix
     * @param reuse whether to reuse any prexisting backing files
     */
    public DiskByteQueue(File tempDir, String backingFilenamePrefix, boolean reuse) {
        super();
        this.tempDir = tempDir;
        this.backingFilenamePrefix = backingFilenamePrefix;
        tempDir.mkdirs();
        String pathPrefix = tempDir.getPath() + File.separatorChar + backingFilenamePrefix;
        inFile = new File(pathPrefix + IN_FILE_EXTENSION);
        outFile = new File(pathPrefix + OUT_FILE_EXTENSION);
        if (reuse == false) {
            if (inFile.exists()) {
                inFile.delete();
            }
            if (outFile.exists()) {
                outFile.delete();
            }
        }
    }

    /**
     * The stream to read from this byte queue
     *
     * @return
     * @throws IOException
     */
    public InputStream getHeadStream() throws IOException {
        if (headStream == null) {
            headStream = new FlipFileInputStream(rememberedPosition);
        }
        return headStream;
    }

    /**
     * The stream to write to this byte queue
     *
     * @return
     * @throws FileNotFoundException
     */
    public OutputStream getTailStream() throws FileNotFoundException {
        if (tailStream == null) {
            tailStream = new FlipFileOutputStream();
        }
        return tailStream;
    }

    /**
     * flip the current outFile to the inFile role,
     * make new outFile
     * @throws IOException
     */
    void flip() throws IOException {
        inFile.delete();
        tailStream.flush();
        tailStream.close();
        outFile.renameTo(inFile);
        tailStream.setupStreams();
    }

    /**
     * Returns an input stream that covers the entire queue. It only allows read
     * access. Reading from it will not affect the queue in any way. It is not
     * safe to add or remove items to the queue while using this stream.
     *
     * @return an input stream that covers the entire queue
     * @throws IOException
     */
    public InputStream getReadAllInputStream() throws IOException {
        tailStream.flush();
        BufferedInputStream inStream1;
        if (inFile == null) {
            inStream1 = null;
        } else {
            FileInputStream tmpFileStream1 = new FileInputStream(inFile);
            tmpFileStream1.getChannel().position(headStream.getReadPosition());
            inStream1 = new BufferedInputStream(tmpFileStream1, 4096);
        }
        tailStream.flush();
        FileInputStream tmpFileStream2 = new FileInputStream(outFile);
        BufferedInputStream inStream2 = new BufferedInputStream(tmpFileStream2, 4096);
        ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
        new ObjectOutputStream(baOutStream);
        ByteArrayInputStream baInStream = new ByteArrayInputStream(baOutStream.toByteArray());
        return new SequenceInputStream((inStream1 == null ? (InputStream) baInStream : (InputStream) new SequenceInputStream(baInStream, inStream1)), inStream2);
    }

    /**
     * @throws IOException
     */
    public void close() throws IOException {
        if (headStream != null) {
            rememberedPosition = headStream.position;
            headStream.close();
            headStream = null;
        }
        if (tailStream != null) {
            tailStream.close();
            tailStream = null;
        }
    }

    /**
     * frees all streams/files associated with this object
     * @throws IOException
     */
    public void discard() throws IOException {
        close();
        if (!inFile.delete()) {
            throw new IOException("unable to delete " + inFile);
        }
        if (!outFile.delete()) {
            throw new IOException("unable to delete " + outFile);
        }
    }

    /**
     * @throws IOException
     */
    public void disconnect() throws IOException {
        close();
    }

    /**
     * @throws IOException
     */
    public void connect() throws IOException {
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        tailStream.flush();
        rememberedPosition = headStream.getReadPosition();
        stream.defaultWriteObject();
        ObjectPlusFilesOutputStream coostream = (ObjectPlusFilesOutputStream) stream;
        coostream.snapshotAppendOnlyFile(outFile);
        coostream.snapshotAppendOnlyFile(inFile);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        ObjectPlusFilesInputStream coistream = (ObjectPlusFilesInputStream) stream;
        coistream.restoreFile(outFile);
        coistream.restoreFile(inFile);
    }

    /**
     * An output stream that supports the DiskBackedByteQueue, by
     * always appending to the current outFile.
     *
     * @author Gordon Mohr.
     */
    class FlipFileOutputStream extends OutputStream {

        BufferedOutputStream outStream;

        FileOutputStream fileStream;

        /**
         * Constructor
         * @throws FileNotFoundException if unable to create FileOutStream.
         */
        public FlipFileOutputStream() throws FileNotFoundException {
            setupStreams();
        }

        protected void setupStreams() throws FileNotFoundException {
            fileStream = new FileOutputStream(outFile, true);
            outStream = new BufferedOutputStream(fileStream, 4096);
        }

        /** (non-Javadoc)
         * @see java.io.OutputStream#write(int)
         */
        public void write(int b) throws IOException {
            outStream.write(b);
        }

        /** (non-Javadoc)
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        public void write(byte[] b, int off, int len) throws IOException {
            outStream.write(b, off, len);
        }

        /** (non-Javadoc)
         * @see java.io.OutputStream#write(byte[])
         */
        public void write(byte[] b) throws IOException {
            outStream.write(b);
        }

        /** (non-Javadoc)
         * @see java.io.OutputStream#close()
         */
        public void close() throws IOException {
            super.close();
            outStream.close();
        }

        /** (non-Javadoc)
         * @see java.io.OutputStream#flush()
         */
        public void flush() throws IOException {
            outStream.flush();
        }
    }

    /**
     * An input stream that supports the DiskBackedByteQueue,
     * by always reading from the current inFile, and triggering
     * a "flip" when one inFile is exhausted.
     *
     * @author Gordon Mohr.
     */
    public class FlipFileInputStream extends InputStream {

        FileInputStream fileStream;

        InputStream inStream;

        long position;

        /**
         * Constructor.
         * @param readPosition
         * @throws IOException
         */
        public FlipFileInputStream(long readPosition) throws IOException {
            setupStreams(readPosition);
        }

        /** (non-Javadoc)
         * @see java.io.InputStream#read()
         */
        public int read() throws IOException {
            int c;
            if (inStream == null || (c = inStream.read()) == -1) {
                getNewInStream();
                if ((c = inStream.read()) == -1) {
                    return -1;
                }
            }
            if (c != -1) {
                position++;
            }
            return c;
        }

        /** (non-Javadoc)
         * @see java.io.InputStream#read(byte[])
         */
        public int read(byte[] b) throws IOException {
            return read(b, 0, b.length);
        }

        /** (non-Javadoc)
         * @see java.io.InputStream#read(byte[], int, int)
         */
        public int read(byte[] b, int off, int len) throws IOException {
            int count;
            if (inStream == null || (count = inStream.read(b, off, len)) == -1) {
                getNewInStream();
                if ((count = inStream.read(b, off, len)) == -1) {
                    return -1;
                }
            }
            if (count != -1) {
                position += count;
            }
            return count;
        }

        /**
         * Once the current file is exhausted, this method is called to flip files
         * for both input and output streams.
         * @throws FileNotFoundException
         * @throws IOException
         */
        private void getNewInStream() throws FileNotFoundException, IOException {
            if (inStream != null) {
                inStream.close();
            }
            DiskByteQueue.this.flip();
            setupStreams(0);
        }

        private void setupStreams(long readPosition) throws IOException {
            inFile.createNewFile();
            fileStream = new FileInputStream(inFile);
            inStream = new BufferedInputStream(fileStream, 4096);
            inStream.skip(readPosition);
            position = readPosition;
        }

        /** (non-Javadoc)
         * @see java.io.InputStream#close()
         */
        public void close() throws IOException {
            super.close();
            if (inStream != null) {
                inStream.close();
            }
        }

        /**
         * Returns the current position of the input stream in the current file
         * (since last flip).
         * @return number of bytes that have been read from the current file.
         */
        public long getReadPosition() {
            return position;
        }
    }
}
