package gg.arkehion.newstore.fileimpl;

import gg.arkehion.exceptions.ArEndTransferException;
import gg.arkehion.exceptions.ArFileException;
import gg.arkehion.exceptions.ArUnvalidIndexException;
import gg.arkehion.newstore.ArkStoreInterface;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * Version using Uncrypted file and FileChannel for In, FileOutputStream for Put
 * 
 * @author frederic
 * 
 */
public class ArkFsDocUnencryptedFO extends ArkFsDocAbstract {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(ArkFsDocUnencryptedFO.class);

    /**
     * FileOutputStream Out
     */
    private FileOutputStream fileOutputStream = null;

    /**
     * FileChannel In
     */
    private FileChannel bfileChannelIn = null;

    /**
     * Associated ByteBuffer
     */
    private ByteBuffer bbyteBuffer = null;

    /**
     * Create a new Document according to the storage and its did (unique id for
     * the document).
     * 
     * The Document is marked as Ready.
     * 
     * @param storage
     *            associated Storage
     * @param did
     *            Document Id
     * @throws ArUnvalidIndexException
     */
    public ArkFsDocUnencryptedFO(ArkStoreInterface storage, long did) throws ArUnvalidIndexException {
        super(storage, did);
    }

    @Override
    public void closeFile() {
        if (bfileChannelIn != null) {
            try {
                bfileChannelIn.close();
            } catch (IOException e) {
            }
            bfileChannelIn = null;
            bbyteBuffer = null;
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (ClosedChannelException e) {
            } catch (IOException e) {
            }
            fileOutputStream = null;
        }
        position = 0;
    }

    @Override
    public boolean isInWriting() throws ArUnvalidIndexException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc unready");
        }
        return bfileChannelIn == null;
    }

    @Override
    public boolean isInReading() throws ArUnvalidIndexException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc unready");
        }
        return (bfileChannelIn != null) && (fileOutputStream == null);
    }

    protected byte[] get() throws ArUnvalidIndexException, ArFileException {
        if (!this.isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        FileChannel fileChannelIn = ((ArkFsLegacy) this.storage.getLegacy()).getFileChannelIn(realFile, 0);
        if (fileChannelIn == null) {
            throw new ArFileException("Doc is not readable");
        }
        ByteBuffer byteBuffer = null;
        long size = 0;
        try {
            size = fileChannelIn.size();
            byteBuffer = ByteBuffer.allocate((int) size);
            fileChannelIn.read(byteBuffer);
            fileChannelIn.close();
        } catch (IOException e) {
            logger.info("Error during get");
            byteBuffer = null;
            try {
                fileChannelIn.close();
            } catch (IOException e1) {
            }
            throw new ArFileException("Doc is not readable");
        }
        position = size;
        fileChannelIn = null;
        byte[] result = byteBuffer.array();
        byteBuffer = null;
        return result;
    }

    @Override
    public void get(FileChannel fileChannelOut) throws ArUnvalidIndexException, ArFileException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        if (fileChannelOut == null) {
            throw new ArUnvalidIndexException("FileChannel Out is not ready");
        }
        FileChannel fileChannelIn = ((ArkFsLegacy) this.storage.getLegacy()).getFileChannelIn(realFile, 0);
        if (fileChannelIn == null) {
            throw new ArFileException("Doc is not readable");
        }
        long size = 0;
        long transfert = 0;
        try {
            size = fileChannelIn.size();
            transfert = fileChannelOut.transferFrom(fileChannelIn, 0, size);
            fileChannelOut.force(true);
            fileChannelIn.close();
            fileChannelIn = null;
            fileChannelOut.close();
        } catch (IOException e) {
            logger.error("Error during get:", e);
            if (fileChannelIn != null) {
                try {
                    fileChannelIn.close();
                } catch (IOException e1) {
                }
            }
            throw new ArFileException("Doc is not readable");
        }
        if (transfert == size) {
            position += size;
        }
        if (transfert != size) {
            throw new ArFileException("Doc is not fully readable");
        }
    }

    protected byte[] getBlockBytes() throws ArUnvalidIndexException, ArFileException, ArEndTransferException {
        int sizeout = getBlockByteBuffer();
        byte[] result = new byte[sizeout];
        this.bbyteBuffer.get(result);
        this.bbyteBuffer.clear();
        if (sizeout < blocksize) {
            closeFile();
        }
        return result;
    }

    @Override
    public ChannelBuffer getBlockChannelBuffer() throws ArUnvalidIndexException, ArFileException, ArEndTransferException {
        int sizeout = getBlockByteBuffer();
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(bbyteBuffer);
        bbyteBuffer.clear();
        if (sizeout < blocksize) {
            closeFile();
        }
        return buffer;
    }

    /**
     * Read into the bbyteBuffer at most sizeBlock bytes from Doc. The buffer is
     * already flipped.
     * 
     * @return the number of bytes read
     * @throws ArUnvalidIndexException
     * @throws ArFileException
     * @throws ArEndTransferException
     */
    private int getBlockByteBuffer() throws ArUnvalidIndexException, ArFileException, ArEndTransferException {
        if (!isReady) {
            throw new ArUnvalidIndexException("No Doc is ready");
        }
        if (bfileChannelIn == null) {
            bfileChannelIn = ((ArkFsLegacy) this.storage.getLegacy()).getFileChannelIn(realFile, position);
            if (bfileChannelIn != null) {
                if (bbyteBuffer != null) {
                    if (bbyteBuffer.capacity() != blocksize) {
                        bbyteBuffer = null;
                        bbyteBuffer = ByteBuffer.allocateDirect(blocksize);
                    }
                } else {
                    bbyteBuffer = ByteBuffer.allocateDirect(blocksize);
                }
            }
        }
        if (bfileChannelIn == null) {
            throw new ArFileException("Internal error, Doc is not ready");
        }
        int sizeout = 0;
        while (sizeout < blocksize) {
            try {
                int sizeread = bfileChannelIn.read(bbyteBuffer);
                if (sizeread <= 0) {
                    break;
                }
                sizeout += sizeread;
            } catch (IOException e) {
                logger.error("Error during get:", e);
                closeFile();
                throw new ArFileException("Internal error, Doc is not ready");
            }
        }
        if (sizeout <= 0) {
            closeFile();
            throw new ArEndTransferException("End of Doc");
        }
        this.bbyteBuffer.flip();
        position += sizeout;
        return sizeout;
    }

    @Override
    public long write(FileChannel fileChannelIn) throws ArUnvalidIndexException, ArFileException {
        if (!this.isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        if (fileChannelIn == null) {
            throw new ArFileException("Arg FileChannelIn is not ready");
        }
        checkDirectory();
        if (!realFile.canWrite()) {
            throw new ArFileException("Doc is not writable");
        }
        FileChannel fileChannelOut = ((ArkFsLegacy) this.storage.getLegacy()).getFileChannelOut(realFile, 0);
        long size = 0;
        long transfert = 0;
        try {
            size = fileChannelIn.size();
            transfert = fileChannelOut.transferFrom(fileChannelIn, 0, size);
        } catch (IOException e) {
            logger.info("Error during write");
            try {
                fileChannelOut.close();
            } catch (IOException e1) {
            }
            fileChannelOut = null;
            abort();
            throw new ArFileException("Doc cannot be writen");
        }
        try {
            fileChannelOut.close();
            fileChannelOut = null;
            fileChannelIn.close();
        } catch (IOException e) {
        }
        boolean retour = (size == transfert);
        if (retour) {
            this.updateLastTime();
        } else {
            size = -1;
            abort();
            throw new ArFileException("Cannot write doc");
        }
        return size;
    }

    @Override
    public long write(InputStream inputStream) throws ArUnvalidIndexException, ArFileException {
        if (!this.isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        if (inputStream == null) {
            throw new ArFileException("Arg InputStream is not ready");
        }
        checkDirectory();
        if (!realFile.canWrite()) {
            throw new ArFileException("Doc is not writable");
        }
        FileChannel fileChannelOut = ((ArkFsLegacy) this.storage.getLegacy()).getFileChannelOut(realFile, 0);
        long size = 0;
        int transfert = 0;
        byte[] bytes = new byte[blocksize];
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        try {
            transfert = inputStream.read(bytes);
            while (transfert >= 0) {
                size += transfert;
                byteBuffer.limit(transfert);
                fileChannelOut.write(byteBuffer);
                byteBuffer.clear();
                transfert = inputStream.read(bytes);
            }
        } catch (IOException e) {
            logger.info("Error during write");
            try {
                fileChannelOut.close();
            } catch (IOException e1) {
            }
            fileChannelOut = null;
            byteBuffer = null;
            bytes = null;
            this.abort();
            throw new ArFileException("Doc is not writable");
        }
        try {
            fileChannelOut.close();
            inputStream.close();
        } catch (IOException e) {
        }
        fileChannelOut = null;
        byteBuffer = null;
        bytes = null;
        position = size;
        this.updateLastTime();
        return size;
    }

    @Override
    public void writeBlock(byte[] bytes) throws ArUnvalidIndexException, ArFileException {
        if (!isReady) {
            throw new ArUnvalidIndexException("No Doc is ready");
        }
        if (fileOutputStream == null) {
            fileOutputStream = ((ArkFsLegacy) this.storage.getLegacy()).getFileOutputStream(realFile, position);
        }
        if (fileOutputStream == null) {
            this.deleteInternal();
            throw new ArFileException("Internal error, Doc is not ready");
        }
        long bufferSize = bytes.length;
        try {
            fileOutputStream.write(bytes);
        } catch (IOException e2) {
            logger.error("Error during write:", e2);
            closeFile();
            throw new ArFileException("Internal error, file is not ready");
        }
        position += bufferSize;
    }

    @Override
    public void get(OutputStream outputStream) throws ArUnvalidIndexException, ArFileException {
    }
}
