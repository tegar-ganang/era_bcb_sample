package org.exist.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.exist.external.org.apache.commons.io.output.ByteArrayOutputStream;

;

/**
 * 
 * This class is a cross-over of many others, but mainly File and OutputStream
 * 
 * @author jmfernandez
 *
 */
public class VirtualTempFile extends OutputStream {

    private static final Logger LOG = Logger.getLogger(VirtualTempFile.class);

    private static final int DEFAULT_MAX_CHUNK_SIZE = 0x40000;

    private static final String DEFAULT_TEMP_PREFIX = "eXistRPCV";

    private static final String DEFAULT_TEMP_POSTFIX = ".res";

    protected File tempFile;

    protected boolean deleteTempFile;

    protected ByteArrayOutputStream baBuffer;

    protected FileOutputStream strBuffer;

    protected OutputStream os;

    protected byte[] tempBuffer;

    protected int maxMemorySize;

    protected int maxChunkSize;

    protected long vLength;

    protected String temp_prefix;

    protected String temp_postfix;

    /**
	 * Constructor for a fresh VirtualTempFile
	 */
    public VirtualTempFile() {
        this(DEFAULT_MAX_CHUNK_SIZE, DEFAULT_MAX_CHUNK_SIZE);
    }

    /**
	 * Constructor for a fresh VirtualTempFile, with some params
	 * @param maxMemorySize
	 * @param maxChunkSize
	 */
    public VirtualTempFile(int maxMemorySize, int maxChunkSize) {
        this.maxMemorySize = maxMemorySize;
        this.maxChunkSize = maxChunkSize;
        vLength = -1L;
        baBuffer = new ByteArrayOutputStream(maxMemorySize);
        strBuffer = null;
        tempFile = null;
        tempBuffer = null;
        deleteTempFile = true;
        os = baBuffer;
        temp_prefix = DEFAULT_TEMP_PREFIX;
        temp_postfix = DEFAULT_TEMP_POSTFIX;
    }

    /**
	 * Constructor for an already known file
	 * @param theFile
	 */
    public VirtualTempFile(File theFile) {
        this(theFile, DEFAULT_MAX_CHUNK_SIZE);
    }

    /**
	 * Constructor for an already known file, with params
	 * @param theFile
	 * @param maxChunkSize
	 */
    public VirtualTempFile(File theFile, int maxChunkSize) {
        this.maxMemorySize = maxChunkSize;
        this.maxChunkSize = maxChunkSize;
        baBuffer = null;
        strBuffer = null;
        os = null;
        tempFile = theFile;
        deleteTempFile = false;
        vLength = theFile.length();
        tempBuffer = null;
        temp_prefix = DEFAULT_TEMP_PREFIX;
        temp_postfix = DEFAULT_TEMP_POSTFIX;
    }

    /**
	 * Constructor for an already known memory block
	 * @param theBlock
	 */
    public VirtualTempFile(byte[] theBlock) {
        this(theBlock, theBlock.length, DEFAULT_MAX_CHUNK_SIZE);
    }

    /**
	 * Constructor for an already known memory block, with params
	 * @param theBlock
	 * @param maxMemorySize
	 * @param maxChunkSize
	 */
    public VirtualTempFile(byte[] theBlock, int maxMemorySize, int maxChunkSize) {
        this.maxMemorySize = maxMemorySize;
        this.maxChunkSize = maxChunkSize;
        baBuffer = null;
        strBuffer = null;
        os = null;
        temp_prefix = DEFAULT_TEMP_PREFIX;
        temp_postfix = DEFAULT_TEMP_POSTFIX;
        tempFile = null;
        deleteTempFile = true;
        vLength = theBlock.length;
        if (vLength <= maxMemorySize) {
            tempBuffer = theBlock;
        } else {
            try {
                tempFile = File.createTempFile(temp_prefix, temp_postfix);
                tempFile.deleteOnExit();
                LOG.debug("Writing to temporary file: " + tempFile.getName());
                OutputStream tmpBuffer = new FileOutputStream(tempFile);
                try {
                    tmpBuffer.write(theBlock);
                } finally {
                    tmpBuffer.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    /**
	 * The prefix string used when the temp file is going to be created
	 * @return prefix string
	 */
    public String getTempPrefix() {
        return temp_prefix;
    }

    /**
	 * The postfix string used when the temp file is going to be created
	 * @return  postfix string
	 */
    public String getTempPostfix() {
        return temp_postfix;
    }

    /**
	 * It sets the used prefix string on temp filename creation
	 * @param newPrefix
	 */
    public void setTempPrefix(String newPrefix) {
        if (newPrefix == null) newPrefix = DEFAULT_TEMP_PREFIX;
        temp_prefix = newPrefix;
    }

    /**
	 * It sets the used prefix string on temp filename creation
	 * @param newPostfix
	 */
    public void setTempPostfix(String newPostfix) {
        if (newPostfix == null) newPostfix = DEFAULT_TEMP_POSTFIX;
        temp_postfix = newPostfix;
    }

    /**
	 * Method from OutputStream
	 */
    public void close() throws IOException {
        if (baBuffer != null) {
            tempBuffer = baBuffer.toByteArray();
            baBuffer = null;
            vLength = tempBuffer.length;
        }
        if (strBuffer != null) {
            strBuffer.close();
            strBuffer = null;
            vLength = tempFile.length();
        }
        if (os != null) os = null;
    }

    /**
	 * Method from OutputStream
	 */
    public void flush() throws IOException {
        if (os == null) throw new IOException("No stream to flush");
        os.flush();
    }

    /**
     * The method <code>getChunk</code>
     *
     * @param offset a <code>long</code> value
     * @return a <code>byte[]</code> value
     * @exception IOException if an error occurs
     */
    public byte[] getChunk(long offset) throws IOException {
        byte[] data = null;
        if (os != null) close();
        if (tempFile != null) {
            RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
            raf.seek(offset);
            long remaining = raf.length() - offset;
            if (remaining > maxChunkSize) remaining = maxChunkSize; else if (remaining < 0) remaining = 0;
            data = new byte[(int) remaining];
            raf.readFully(data);
            raf.close();
        } else if (tempBuffer != null) {
            long remaining = tempBuffer.length - offset;
            if (remaining > maxChunkSize) remaining = maxChunkSize; else if (remaining < 0) remaining = 0;
            data = new byte[(int) remaining];
            if (remaining > 0) System.arraycopy(tempBuffer, (int) offset, data, 0, (int) remaining);
        }
        return data;
    }

    public boolean exists() {
        return tempFile != null || tempBuffer != null || baBuffer != null;
    }

    public long length() {
        if (os != null) {
            try {
                close();
            } catch (IOException ioe) {
            }
        }
        return vLength;
    }

    /**
	 * Method from File
	 * @return Always returns true
	 */
    public boolean delete() {
        if (os != null) {
            try {
                close();
            } catch (IOException ioe) {
            }
        }
        if (tempFile != null) {
            if (strBuffer != null) {
                try {
                    strBuffer.close();
                } catch (IOException ioe) {
                }
                strBuffer = null;
            }
            if (deleteTempFile) tempFile.delete();
            tempFile = null;
        }
        if (baBuffer != null) {
            try {
                baBuffer.close();
            } catch (IOException ioe) {
            }
            baBuffer = null;
        }
        if (tempBuffer != null) {
            tempBuffer = null;
        }
        return true;
    }

    private void writeSwitch() throws IOException {
        if (tempFile == null) {
            tempFile = File.createTempFile(temp_prefix, temp_postfix);
            tempFile.deleteOnExit();
            LOG.debug("Writing to temporary file: " + tempFile.getName());
            strBuffer = new FileOutputStream(tempFile);
            strBuffer.write(baBuffer.toByteArray());
            os = strBuffer;
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (os == null) {
            throw new IOException("No stream to write to");
        }
        os.write(b);
        if (baBuffer != null && baBuffer.size() > maxMemorySize) {
            writeSwitch();
        }
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (os == null) {
            throw new IOException("No stream to write to");
        }
        os.write(b, off, len);
        if (baBuffer != null && baBuffer.size() > maxMemorySize) {
            writeSwitch();
        }
    }

    /**
	 * A commodity method to write the whole content of an InputStream
	 */
    public void write(InputStream is) throws IOException {
        write(is, -1L);
    }

    /**
	 * A commodity method to write the whole content of an InputStream,
	 * giving an optional max length (honored when it is bigger than 0)
	 */
    public void write(InputStream is, long lengthHint) throws IOException {
        if (os == null) {
            throw new IOException("No stream to write to");
        }
        byte[] buffer = new byte[maxChunkSize];
        long off = 0;
        int count = 0;
        do {
            count = is.read(buffer);
            if (count > 0) {
                os.write(buffer, 0, count);
                off += count;
            }
            if (baBuffer != null && baBuffer.size() > maxMemorySize) {
                writeSwitch();
            }
        } while (count != -1 && (lengthHint <= 0 || off < lengthHint));
    }

    /**
	 * An easy way to obtain an InputStream
	 * @return byte stream
	 * @throws IOException
	 */
    public InputStream getByteStream() throws IOException {
        if (os != null) close();
        InputStream result = null;
        if (tempFile != null) {
            result = new BufferedInputStream(new FileInputStream(tempFile), 655360);
        } else if (tempBuffer != null) {
            result = new ByteArrayInputStream(tempBuffer);
        }
        return result;
    }

    /**
	 * It returns either a byte array or a File
	 * with the content. The initial threshold rules
	 * which kind of object you are getting
	 * @return Either a File or a byte[] object
	 */
    public Object getContent() {
        try {
            if (os != null) close();
        } catch (IOException ioe) {
        }
        return (tempFile != null) ? tempFile : tempBuffer;
    }

    /**
	 * Method to force materialization as a (temp)file the VirtualTempFile instance
	 * @return A (temporal) file with the content
	 * @throws IOException
	 */
    public File toFile() throws IOException {
        writeSwitch();
        if (os != null) close();
        File retFile = tempFile;
        tempFile = null;
        return retFile;
    }

    /**
	 * Method to materialize the accumulated content in an OutputStream
	 * @param out The output stream where the content is going to be written
	 */
    public void writeToStream(OutputStream out) throws IOException {
        InputStream result = null;
        if (tempFile != null) {
            InputStream input = new BufferedInputStream(new FileInputStream(tempFile));
            IOUtils.copy(input, out);
            IOUtils.closeQuietly(input);
        } else if (tempBuffer != null) {
            out.write(tempBuffer);
        }
    }
}
