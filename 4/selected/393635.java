package org.xtreemfs.babudb.index.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;
import org.xtreemfs.babudb.api.database.ResultSet;
import org.xtreemfs.babudb.index.ByteRange;
import org.xtreemfs.babudb.index.reader.InternalBufferUtil;
import org.xtreemfs.foundation.buffer.BufferPool;
import org.xtreemfs.foundation.buffer.ReusableBuffer;

/**
 * Writes an index to a set of files on disk. A file will not be larger than the
 * given max file size. Multiple files are necessary to handle databases larger
 * than 2 GB on 32-bit systems due to MMap limitations.
 * 
 * The index has two parts, a sorted list of blocks containing key/value-pairs
 * and a block index. The block index is a sparse index pointing to the sorted
 * blocks.
 * 
 * @author stender
 * @author hoegqvist
 */
public class DiskIndexWriter {

    private String path;

    private int maxBlockEntries;

    private boolean compressed;

    private int maxFileSize;

    private short blockFileId;

    /**
     * Creates a new DiskIndexWriter
     * 
     * @param path
     *            The path to the directory where the index will be written. The
     *            directory is created if it does not yet exist.
     * @param maxBlockEntries
     *            The maximum number of entries in a single block.
     * @param compressed
     *            Indicates if the blocks should be compressed.
     * @param maxFileSize
     *            The max size of a file storing blocks in bytes. On a 32-bit
     *            system this should not be larger than 2GB.
     * @throws IOException
     */
    public DiskIndexWriter(String path, int maxBlockEntries, boolean compressed, int maxFileSize) throws IOException {
        if (!path.endsWith(System.getProperty("file.separator"))) path += System.getProperty("file.separator");
        assert (maxFileSize <= Integer.MAX_VALUE);
        File diDir = new File(path);
        if (diDir.exists()) throw new IOException("index already exists");
        if (!diDir.mkdirs()) throw new IOException("could not create directory '" + path + "'");
        this.compressed = compressed;
        this.path = path;
        this.maxBlockEntries = maxBlockEntries;
        this.maxFileSize = maxFileSize;
    }

    /**
     * Write blocks to the file at the given path until the maxFileSize is
     * reached.
     * 
     * @param path
     * @param iterator
     * @throws IOException
     */
    private void writeIndex(String path, BlockWriter blockIndex, Iterator<Entry<Object, Object>> iterator) throws IOException {
        FileOutputStream out = new FileOutputStream(path);
        BlockWriter block;
        if (compressed) block = new CompressedBlockWriter(true, true); else block = new DefaultBlockWriter(true, true);
        int entryCount = 0;
        int blockOffset = 0;
        boolean newBlockFile = false;
        while (iterator.hasNext() && !newBlockFile) {
            Entry<Object, Object> next = iterator.next();
            block.add(next.getKey(), next.getValue());
            entryCount++;
            if (entryCount % maxBlockEntries == 0 || !iterator.hasNext()) {
                ReusableBuffer buf = ReusableBuffer.wrap(new byte[(Integer.SIZE / 8) + (Short.SIZE / 8)]);
                buf.putInt(blockOffset);
                buf.putShort(blockFileId);
                blockIndex.add(InternalBufferUtil.toBuffer(block.getBlockKey()), buf.array());
                SerializedBlock serializedBlock = block.serialize();
                blockOffset += serializedBlock.size();
                int writtenBytes = 0;
                Iterator<Object> it = serializedBlock.iterator();
                while (it.hasNext()) {
                    if (!out.getFD().valid()) {
                        out.close();
                        out = new FileOutputStream(path, true);
                    }
                    Object nextBuffer = it.next();
                    if (nextBuffer instanceof ByteRange) {
                        ByteRange rng = (ByteRange) nextBuffer;
                        if (rng.getReusableBuf() != null) BufferPool.free(rng.getReusableBuf());
                    }
                    writtenBytes += writeBuffer(out, nextBuffer);
                }
                assert (writtenBytes == serializedBlock.size());
                if (blockOffset >= maxFileSize) {
                    newBlockFile = true;
                } else {
                    if (iterator.hasNext()) if (compressed) block = new CompressedBlockWriter(true, true); else block = new DefaultBlockWriter(true, true);
                }
            }
        }
        out.close();
    }

    /**
     * Creates an on-disk representation of an index from an iterator of
     * key-value pairs. The iterator has to return keys in ascending order!
     * 
     * @param iterator
     *            an iterator w/ key-value pairs, keys must be in ascending
     *            order
     * @throws IOException
     *             if an I/O error occurs
     */
    public void writeIndex(ResultSet<Object, Object> iterator) throws IOException {
        BlockWriter blockIndex = new DefaultBlockWriter(true, false);
        while (iterator.hasNext()) {
            String indexPath = path + "blockfile_" + new Short(blockFileId).toString() + ".idx";
            writeIndex(indexPath, blockIndex, iterator);
            blockFileId++;
        }
        iterator.free();
        new File(path + "blockindex.idx").createNewFile();
        FileOutputStream out = new FileOutputStream(path + "blockindex.idx", false);
        SerializedBlock serializedBuf = blockIndex.serialize();
        int bytesWritten = 0;
        Iterator<Object> it = serializedBuf.iterator();
        while (it.hasNext()) bytesWritten += writeBuffer(out, it.next());
        assert (bytesWritten == serializedBuf.size());
        out.close();
    }

    private int writeBuffer(FileOutputStream out, Object buf) throws IOException {
        if (buf instanceof byte[]) {
            byte[] bytes = (byte[]) buf;
            out.write(bytes);
            return bytes.length;
        } else {
            ByteRange range = (ByteRange) buf;
            range.getBuf().position(range.getStartOffset());
            ByteBuffer slice = range.getBuf().slice();
            slice.limit(range.getSize());
            return out.getChannel().write(slice);
        }
    }
}
