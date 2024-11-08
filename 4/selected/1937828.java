package gg.arkehion.store.abstimpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import gg.arkehion.configuration.Configuration;
import gg.arkehion.exceptions.ArEndTransferException;
import gg.arkehion.exceptions.ArFileException;
import gg.arkehion.exceptions.ArFileWormException;
import gg.arkehion.exceptions.ArUnvalidIndexException;
import gg.arkehion.store.ArkDualCasFileInterface;
import gg.arkehion.store.ArkLegacyInterface;
import gg.arkehion.store.ArkStoreInterface;
import gg.arkehion.utils.ArConstants;
import goldengate.common.digest.FilesystemBasedDigest;
import goldengate.common.file.DataBlock;

/**
 * @author Frederic Bregier
 * 
 */
public abstract class ArkAbstractDualDoc extends ArkPath implements ArkDualCasFileInterface {

    /**
     * Is this Document ready to be accessed
     */
    protected boolean isReady = false;

    /**
     * Associated if any outputStream
     */
    protected OutputStream outputStream = null;

    /**
     * Associated if any inputStream
     */
    protected InputStream inputStream = null;

    /**
     * Valid Position of this file
     */
    protected long position = 0;

    /**
     * Blocksize to use
     */
    protected int blocksize = ArConstants.BUFFERSIZEDEFAULT;

    /**
     * Associated DKey if already computed
     */
    protected String DKey = null;

    /**
     * If needed: Digest
     */
    protected FilesystemBasedDigest digest = null;

    /**
     * 
     * @param did
     * @throws ArUnvalidIndexException
     */
    public ArkAbstractDualDoc(long did) throws ArUnvalidIndexException {
        super.setArPath(did);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof ArkAbstractDualDoc) {
            ArkAbstractDualDoc doc = (ArkAbstractDualDoc) obj;
            return ((this.idUnique == doc.idUnique) && (this.getStore().getIndex() == doc.getStore().getIndex()) && (this.getStore().getLegacy().getLID() == doc.getStore().getLegacy().getLID()));
        }
        return false;
    }

    /**
     * 
     * @return the internal Target
     */
    protected abstract Object getTarget();

    /**
     * 
     * @return the internal Metadata Target
     */
    protected abstract Object getMetaTarget();

    /**
     * Prepare the directory (mkdirs)
     * 
     * @return True if ok
     * @throws ArFileException
     */
    protected abstract boolean prepareDirectory() throws ArFileException;

    @Override
    public final boolean isReady() {
        return isReady;
    }

    @Override
    public final boolean canWrite() throws ArUnvalidIndexException {
        if (!this.realCanWrite()) {
            for (int i = 0; i < ArConstants.RETRYDELNB; i++) {
                if (ArkAbstractDeleteDualDocQueue.isInDeletion(this)) {
                    if (!this.realDelete()) {
                        try {
                            Thread.sleep(ArConstants.RETRYDELINMS);
                        } catch (InterruptedException e) {
                        }
                        if (!this.realCanWrite()) {
                            continue;
                        }
                    }
                } else {
                    try {
                        Thread.sleep(ArConstants.RETRYDELINMS);
                    } catch (InterruptedException e) {
                    }
                    if (!this.realCanWrite()) {
                        return false;
                    }
                    return true;
                }
                return true;
            }
        }
        return true;
    }

    /**
     * 
     * @return True if the current Doc is ready for writing (in Writing or not
     *         existing)
     * @throws ArUnvalidIndexException
     */
    protected abstract boolean realCanWrite() throws ArUnvalidIndexException;

    /**
     * Close the current Doc
     * 
     */
    protected final void closeFile() {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            inputStream = null;
        }
        if (outputStream != null) {
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                try {
                    outputStream.close();
                } catch (IOException e1) {
                }
            }
            outputStream = null;
        }
    }

    @Override
    public void clear() {
        super.clear();
        closeFile();
        isReady = false;
        position = 0;
        DKey = null;
        digest = null;
    }

    protected abstract boolean metaExists() throws ArUnvalidIndexException;

    @Override
    public final void abort() throws ArUnvalidIndexException, ArFileException {
        boolean isInWriting = isInWriting();
        closeFile();
        if (isInWriting) {
            deleteInternal(false);
        }
    }

    @Override
    public final void store(int blocksize, String metadata) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc unready");
        }
        if (!canWrite()) {
            throw new ArFileWormException("Target doc already exists");
        }
        this.blocksize = blocksize;
        try {
            this.digest = new FilesystemBasedDigest(Configuration.algoMark);
        } catch (NoSuchAlgorithmException e) {
            throw new ArFileException("Algo not available", e);
        }
        writeMetadata(metadata);
        this.outputStream = ArkAbstractDirFunction.getDirFunction(getLegacy()).getOutputStream(getLegacy(), getTarget(), blocksize);
        position = 0;
    }

    @Override
    public final void retrieve(int blocksize) throws ArUnvalidIndexException, ArFileException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc unready");
        }
        if (!canRead()) {
            throw new ArFileException("Doc not readable");
        }
        this.blocksize = blocksize;
        this.inputStream = ArkAbstractDirFunction.getDirFunction(getLegacy()).getInputStream(getLegacy(), getTarget(), blocksize);
        position = 0;
    }

    /**
     * Set the LastTime on the Directories up to the root of the current Legacy
     * 
     * @return True if OK, else False
     */
    protected abstract boolean updateLastTime(long time);

    /**
     * Delete file and metadata but do not make any check on DKey (done before)
     * 
     * @return True if both file and metadata are deleted
     */
    protected final boolean deleteInternal(boolean updateTime) {
        ArkAbstractDeleteDualDocQueue.add(this);
        if (updateTime) ArkAbstractUpdateTimeQueue.add(this);
        digest = null;
        return true;
    }

    /**
     * Real Delete file and metadata but do not make any check on DKey (done before)
     * 
     * @return True if both file and metadata are deleted
     */
    protected abstract boolean realDelete();

    @Override
    public final boolean delete(String dkey) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        if (!this.isReady) {
            return false;
        }
        if (this.exists()) {
            if (!this.isDKeyEqual(dkey)) {
                return false;
            }
        } else {
            return true;
        }
        boolean delete = deleteInternal(true);
        return delete;
    }

    @Override
    public final boolean isDKeyEqual(String shash) throws ArUnvalidIndexException, ArFileException {
        if (shash == null) {
            throw new ArFileException("DKey null cannot be compared");
        }
        String shash2 = this.getDKey();
        if (shash2 == null) {
            throw new ArFileException("DKey null cannot be compared");
        }
        return shash.equals(shash2);
    }

    @Override
    public final DataBlock readDataBlock() throws ArUnvalidIndexException, ArEndTransferException, ArFileException {
        if (isReady) {
            if (inputStream == null) {
                throw new ArEndTransferException("End of File");
            }
            DataBlock dataBlock = new DataBlock();
            ChannelBuffer buffer = readBlockChannelBuffer();
            int len = buffer.readableBytes();
            if (len == 0) {
                dataBlock.setBlock(ChannelBuffers.EMPTY_BUFFER);
                dataBlock.setEOF(true);
                return dataBlock;
            } else if (len < blocksize) {
                dataBlock.setBlock(buffer);
                dataBlock.setEOF(true);
                return dataBlock;
            } else {
                dataBlock.setBlock(buffer);
                return dataBlock;
            }
        }
        throw new ArUnvalidIndexException("Doc is not ready");
    }

    /**
     * Get the current block ChannelBuffer of the current FileInterface. There
     * is therefore no limitation of the file size to 2^32 bytes.
     * 
     * The returned block is limited to sizeblock. If the returned block is less
     * than sizeblock length, it is the last block to read.
     * 
     * @return the resulting block ChannelBuffer (even empty)
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     * @throws ArEndTransferException
     */
    protected final ChannelBuffer readBlockChannelBuffer() throws ArUnvalidIndexException, ArFileException, ArEndTransferException {
        if (isReady && inputStream != null) {
            byte[] readBytes = readBlock();
            ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(readBytes);
            buffer.writerIndex(readBytes.length);
            return buffer;
        }
        throw new ArUnvalidIndexException("Doc is not ready");
    }

    /**
     * Get the current block in a byte array of the current FileInterface. There
     * is therefore no limitation of the file size to 2^32 bytes.
     * 
     * The returned block is limited to sizeblock. If the returned block is less
     * than sizeblock length, it is the last block to read.
     * 
     * @return the resulting block byte array (even empty)
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    protected final byte[] readBlock() throws ArUnvalidIndexException, ArFileException {
        if (isReady && inputStream != null) {
            byte[] bytes = new byte[blocksize];
            int nbRead = 0;
            int totalRead = 0;
            while (totalRead < this.blocksize) {
                try {
                    nbRead = inputStream.read(bytes, totalRead, blocksize - totalRead);
                } catch (IOException e) {
                    throw new ArFileException("Last block cannot be read", e);
                }
                if (nbRead == -1) {
                    byte[] newbytes = new byte[totalRead];
                    position += totalRead;
                    if (totalRead > 0) System.arraycopy(bytes, 0, newbytes, 0, totalRead);
                    closeFile();
                    return newbytes;
                }
                totalRead += nbRead;
            }
            position += totalRead;
            return bytes;
        }
        throw new ArUnvalidIndexException("Doc is not ready");
    }

    @Override
    public final String writeDataBlock(DataBlock dataBlock) throws ArUnvalidIndexException, ArFileException {
        if (isReady && outputStream != null) {
            if (dataBlock.isEOF()) {
                return writeBlockChannelBufferEnd(dataBlock.getBlock());
            }
            writeBlockChannelBuffer(dataBlock.getBlock());
            return null;
        }
        throw new ArUnvalidIndexException("Doc is not ready");
    }

    /**
     * Write the current FileInterface with the given ChannelBuffer. The file is
     * not limited to 2^32 bytes since this write operation is in add mode.
     * 
     * In case of error, the current already written blocks are maintained and
     * the position is not changed.
     * 
     * @param buffer
     *            added to the file
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    protected final void writeBlockChannelBuffer(ChannelBuffer buffer) throws ArUnvalidIndexException, ArFileException {
        if (buffer == null) {
            return;
        }
        long bufferSize = buffer.readableBytes();
        byte[] newbuf = new byte[(int) bufferSize];
        buffer.readBytes(newbuf);
        writeBlock(newbuf);
    }

    /**
     * Write the current FileInterface with the given byte arrays. The file is
     * not limited to 2^32 bytes since this write operation is in add mode.
     * 
     * In case of error, the current already written blocks are maintained and
     * the position is not changed.
     * 
     * @param buffer
     *            added to the file
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    protected final void writeBlock(byte[] buffer) throws ArUnvalidIndexException, ArFileException {
        if (buffer == null) {
            return;
        }
        try {
            outputStream.write(buffer);
            digest.Update(buffer, 0, buffer.length);
        } catch (IOException e) {
            abort();
            deleteInternal(false);
            throw new ArFileException("Doc cannot be written", e);
        }
        position += buffer.length;
    }

    /**
     * End the Write of the current FileInterface with the given ChannelBuffer.
     * The file is not limited to 2^32 bytes since this write operation is in
     * add mode.
     * 
     * @param buffer
     *            added to the file
     * @return DKey
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    protected final String writeBlockChannelBufferEnd(ChannelBuffer buffer) throws ArUnvalidIndexException, ArFileException {
        writeBlockChannelBuffer(buffer);
        DKey = FilesystemBasedDigest.getHex(digest.Final());
        digest = null;
        closeFile();
        ArkAbstractUpdateTimeQueue.add(this);
        return DKey;
    }

    /**
     * End the Write of the current FileInterface with the given byte array. The
     * file is not limited to 2^32 bytes since this write operation is in add
     * mode.
     * 
     * @param buffer
     *            added to the file
     * @return DKey
     * @throws ArFileException
     * @throws ArUnvalidIndexException
     */
    protected final String writeBlockEnd(byte[] buffer) throws ArUnvalidIndexException, ArFileException {
        writeBlock(buffer);
        DKey = FilesystemBasedDigest.getHex(digest.Final());
        digest = null;
        closeFile();
        ArkAbstractUpdateTimeQueue.add(this);
        return DKey;
    }

    @Override
    public final ArkAbstractDualDoc copy(long sidSto, long didFile) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        if (!this.isReady) {
            throw new ArUnvalidIndexException("No Doc is ready");
        }
        if (ArConstants.isIdUniqueKO(sidSto) || ArConstants.isIdUniqueKO(didFile)) {
            throw new ArUnvalidIndexException("Target indexes are unvalid");
        }
        if (!this.canRead()) {
            throw new ArFileException("Doc is unreadable");
        }
        if ((this.idUnique == didFile) && (this.getStore().getIndex() == sidSto)) {
            throw new ArUnvalidIndexException("Src and Target are identical");
        }
        ArkLegacyInterface sameLegacy = this.getStore().getLegacy();
        ArkStoreInterface storagenew = sameLegacy.getStore(sidSto);
        ArkAbstractDualDoc docnew = (ArkAbstractDualDoc) storagenew.getDoc(didFile);
        if (!docnew.canWrite()) {
            throw new ArFileException("Target doc already exists");
        }
        try {
            ArkAbstractDirFunction.getDirFunction(sameLegacy).copyPathToPath(sameLegacy, getMetaTarget(), sameLegacy, docnew.getMetaTarget(), false);
            ArkAbstractDirFunction.getDirFunction(sameLegacy).copyPathToPath(sameLegacy, getTarget(), sameLegacy, docnew.getTarget(), false);
            docnew.DKey = this.DKey;
            ArkAbstractUpdateTimeQueue.add(docnew);
            return docnew;
        } catch (ArFileException e) {
            docnew.deleteInternal(false);
            storagenew = null;
            docnew = null;
            throw e;
        }
    }

    @Override
    public final ArkAbstractDualDoc move(long sidSto, long didFile, String dkey) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        if (!this.isReady) {
            throw new ArUnvalidIndexException("No Doc is ready");
        }
        if (!this.isDKeyEqual(dkey)) {
            throw new ArFileWormException("DKey incorrect while move requested");
        }
        ArkAbstractDualDoc docnew = copy(sidSto, didFile);
        this.deleteInternal(true);
        return docnew;
    }

    @Override
    public final void move(ArkDualCasFileInterface docnew, String dkey) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        if ((!this.isReady) || (!docnew.isReady())) {
            throw new ArUnvalidIndexException("No Doc are ready");
        }
        if (!this.isDKeyEqual(dkey)) {
            throw new ArFileWormException("DKey incorrect while move requested");
        }
        copy(docnew);
        this.deleteInternal(true);
    }

    @Override
    public void setLoopIdUnique(ArkStoreInterface storage, long did) throws ArUnvalidIndexException {
        this.clear();
        if (storage == null) {
            throw new ArUnvalidIndexException("Storage is not defined");
        }
        try {
            setArPath(did);
        } catch (ArUnvalidIndexException e) {
            this.clear();
            throw e;
        }
        this.isReady = true;
    }

    protected abstract void writeMetadata(String metadata) throws ArFileException;

    @Override
    public final boolean isInWriting() throws ArUnvalidIndexException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc unready");
        }
        return (outputStream != null);
    }

    @Override
    public final boolean isInReading() throws ArUnvalidIndexException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc unready");
        }
        return (inputStream != null);
    }

    @Override
    public final long get(FileChannel fileChannelOut) throws ArUnvalidIndexException, ArFileException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        if (fileChannelOut == null) {
            throw new ArUnvalidIndexException("FileChannel Out is not ready");
        }
        if (inputStream == null) {
            throw new ArFileException("Doc not ready to be read");
        }
        byte[] bytes = new byte[blocksize];
        int read = 0;
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        try {
            fileChannelOut.force(true);
            read = inputStream.read(bytes);
            while (read > 0) {
                byteBuffer.limit(read);
                fileChannelOut.write(byteBuffer);
                position += read;
                byteBuffer.clear();
                read = inputStream.read(bytes);
            }
            inputStream.close();
            inputStream = null;
            fileChannelOut.close();
        } catch (IOException e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                }
                inputStream = null;
            }
            byteBuffer = null;
            bytes = null;
            throw new ArFileException("Doc is not readable");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                }
                inputStream = null;
            }
        }
        return position;
    }

    @Override
    public final long get(OutputStream outputStream) throws ArUnvalidIndexException, ArFileException {
        if (!isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        if (outputStream == null) {
            throw new ArUnvalidIndexException("OutputStream is not ready");
        }
        if (inputStream == null) {
            throw new ArFileException("Doc not ready to be read");
        }
        byte[] bytes = new byte[blocksize];
        int read = 0;
        try {
            read = inputStream.read(bytes);
            while (read > 0) {
                outputStream.write(bytes, 0, read);
                position += read;
                read = inputStream.read(bytes);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                }
                inputStream = null;
            }
            bytes = null;
            throw new ArFileException("Doc is not readable");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                }
                inputStream = null;
            }
        }
        return position;
    }

    @Override
    public final String write(FileChannel fileChannelIn) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        if (!this.isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        if (fileChannelIn == null) {
            abort();
            deleteInternal(false);
            throw new ArFileException("Arg FileChannelIn is not ready");
        }
        if (outputStream == null) {
            abort();
            deleteInternal(false);
            throw new ArFileException("Doc not ready to be written");
        }
        long size = 0;
        try {
            size = fileChannelIn.size();
        } catch (IOException e) {
            abort();
            deleteInternal(false);
            throw new ArFileException("Arg FileChannelIn is not ready");
        }
        long transfert = 0L;
        int last_bloc;
        int taille_bloc;
        if (size > blocksize) {
            taille_bloc = blocksize;
        } else {
            taille_bloc = (int) size;
        }
        byte[] bytes = new byte[taille_bloc];
        FilesystemBasedDigest digest = null;
        try {
            digest = new FilesystemBasedDigest(Configuration.algoMark);
        } catch (NoSuchAlgorithmException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            }
            abort();
            deleteInternal(false);
            outputStream = null;
            bytes = null;
            throw new ArFileException("Error while creating Digest", e);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        try {
            last_bloc = fileChannelIn.read(byteBuffer);
            while (last_bloc > 0) {
                outputStream.write(bytes, 0, last_bloc);
                transfert += last_bloc;
                digest.Update(bytes, 0, last_bloc);
                byteBuffer.clear();
                last_bloc = fileChannelIn.read(byteBuffer);
            }
        } catch (IOException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            }
            abort();
            deleteInternal(false);
            outputStream = null;
            byteBuffer = null;
            bytes = null;
            throw new ArFileException("Cannot write doc");
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                }
                try {
                    outputStream.close();
                } catch (IOException e1) {
                }
                outputStream = null;
            }
        }
        try {
            fileChannelIn.close();
        } catch (IOException e) {
        }
        byteBuffer = null;
        bytes = null;
        if (size == transfert) {
            ArkAbstractUpdateTimeQueue.add(this);
            position = size;
            DKey = FilesystemBasedDigest.getHex(digest.Final());
            return DKey;
        } else {
            abort();
            deleteInternal(false);
            throw new ArFileException("Cannot write doc");
        }
    }

    @Override
    public final String write(InputStream inputStream) throws ArUnvalidIndexException, ArFileException, ArFileWormException {
        if (!this.isReady) {
            throw new ArUnvalidIndexException("Doc is not ready");
        }
        if (inputStream == null) {
            abort();
            deleteInternal(false);
            throw new ArFileException("Arg InputStream is not ready");
        }
        if (outputStream == null) {
            abort();
            deleteInternal(false);
            throw new ArFileException("Doc not ready to be written");
        }
        FilesystemBasedDigest digest = null;
        try {
            digest = new FilesystemBasedDigest(Configuration.algoMark);
        } catch (NoSuchAlgorithmException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            }
            abort();
            deleteInternal(false);
            outputStream = null;
            throw new ArFileException("Error while creating Digest", e);
        }
        long transfert = 0L;
        int last_bloc;
        int taille_bloc = blocksize;
        byte[] bytes = new byte[taille_bloc];
        try {
            last_bloc = inputStream.read(bytes);
            while (last_bloc > 0) {
                outputStream.write(bytes, 0, last_bloc);
                transfert += last_bloc;
                digest.Update(bytes, 0, last_bloc);
                last_bloc = inputStream.read(bytes);
            }
        } catch (IOException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            }
            abort();
            deleteInternal(false);
            outputStream = null;
            bytes = null;
            throw new ArFileException("Cannot write doc");
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                }
                try {
                    outputStream.close();
                } catch (IOException e1) {
                }
                outputStream = null;
            }
        }
        try {
            inputStream.close();
        } catch (IOException e) {
        }
        bytes = null;
        ArkAbstractUpdateTimeQueue.add(this);
        position = transfert;
        DKey = FilesystemBasedDigest.getHex(digest.Final());
        return DKey;
    }

    @Override
    public final String getGlobalPath() {
        return this.getStore().getObjectGlobalPath() + this.getObjectGlobalPath();
    }

    @Override
    public final String getAbstractName() {
        return this.getStore().getObjectAbstractName() + this.getObjectAbstractName();
    }

    @Override
    public final String getGlobalPathWoBasename() {
        return this.getStore().getObjectGlobalPath() + this.getObjectGlobalPathWoBasename();
    }

    @Override
    public final long getPosition() {
        return position;
    }
}
