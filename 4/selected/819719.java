package org.apache.hadoop.hdfs.server.datanode;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.hadoop.fs.ChecksumException;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.net.SocketOutputStream;
import org.apache.hadoop.util.DataChecksum;
import org.apache.hadoop.util.StringUtils;

/**
 * Reads a block from the disk and sends it to a recipient.
 */
class BlockSender implements java.io.Closeable, FSConstants {

    public static final Log LOG = DataNode.LOG;

    static final Log ClientTraceLog = DataNode.ClientTraceLog;

    private Block block;

    private InputStream blockIn;

    private long blockInPosition = -1;

    private DataInputStream checksumIn;

    private DataChecksum checksum;

    private long offset;

    private long endOffset;

    private long blockLength;

    private int bytesPerChecksum;

    private int checksumSize;

    private boolean corruptChecksumOk;

    private boolean chunkOffsetOK;

    private long seqno;

    private boolean transferToAllowed = true;

    private boolean blockReadFully;

    private boolean verifyChecksum;

    private BlockTransferThrottler throttler;

    private final String clientTraceFmt;

    /**
   * Minimum buffer used while sending data to clients. Used only if
   * transferTo() is enabled. 64KB is not that large. It could be larger, but
   * not sure if there will be much more improvement.
   */
    private static final int MIN_BUFFER_WITH_TRANSFERTO = 64 * 1024;

    BlockSender(Block block, long startOffset, long length, boolean corruptChecksumOk, boolean chunkOffsetOK, boolean verifyChecksum, DataNode datanode) throws IOException {
        this(block, startOffset, length, corruptChecksumOk, chunkOffsetOK, verifyChecksum, datanode, null);
    }

    BlockSender(Block block, long startOffset, long length, boolean corruptChecksumOk, boolean chunkOffsetOK, boolean verifyChecksum, DataNode datanode, String clientTraceFmt) throws IOException {
        try {
            this.block = block;
            this.chunkOffsetOK = chunkOffsetOK;
            this.corruptChecksumOk = corruptChecksumOk;
            this.verifyChecksum = verifyChecksum;
            this.blockLength = datanode.data.getLength(block);
            this.transferToAllowed = datanode.transferToAllowed;
            this.clientTraceFmt = clientTraceFmt;
            if (!corruptChecksumOk || datanode.data.metaFileExists(block)) {
                checksumIn = new DataInputStream(new BufferedInputStream(datanode.data.getMetaDataInputStream(block), BUFFER_SIZE));
                BlockMetadataHeader header = BlockMetadataHeader.readHeader(checksumIn);
                short version = header.getVersion();
                if (version != FSDataset.METADATA_VERSION) {
                    LOG.warn("Wrong version (" + version + ") for metadata file for " + block + " ignoring ...");
                }
                checksum = header.getChecksum();
            } else {
                LOG.warn("Could not find metadata file for " + block);
                checksum = DataChecksum.newDataChecksum(DataChecksum.CHECKSUM_NULL, 16 * 1024);
            }
            bytesPerChecksum = checksum.getBytesPerChecksum();
            if (bytesPerChecksum > 10 * 1024 * 1024 && bytesPerChecksum > blockLength) {
                checksum = DataChecksum.newDataChecksum(checksum.getChecksumType(), Math.max((int) blockLength, 10 * 1024 * 1024));
                bytesPerChecksum = checksum.getBytesPerChecksum();
            }
            checksumSize = checksum.getChecksumSize();
            if (length < 0) {
                length = blockLength;
            }
            endOffset = blockLength;
            if (startOffset < 0 || startOffset > endOffset || (length + startOffset) > endOffset) {
                String msg = " Offset " + startOffset + " and length " + length + " don't match block " + block + " ( blockLen " + endOffset + " )";
                LOG.warn(datanode.dnRegistration + ":sendBlock() : " + msg);
                throw new IOException(msg);
            }
            offset = (startOffset - (startOffset % bytesPerChecksum));
            if (length >= 0) {
                long tmpLen = startOffset + length;
                if (tmpLen % bytesPerChecksum != 0) {
                    tmpLen += (bytesPerChecksum - tmpLen % bytesPerChecksum);
                }
                if (tmpLen < endOffset) {
                    endOffset = tmpLen;
                }
            }
            if (offset > 0) {
                long checksumSkip = (offset / bytesPerChecksum) * checksumSize;
                if (checksumSkip > 0) {
                    IOUtils.skipFully(checksumIn, checksumSkip);
                }
            }
            seqno = 0;
            blockIn = datanode.data.getBlockInputStream(block, offset);
        } catch (IOException ioe) {
            IOUtils.closeStream(this);
            IOUtils.closeStream(blockIn);
            throw ioe;
        }
    }

    /**
   * close opened files.
   */
    public void close() throws IOException {
        IOException ioe = null;
        if (checksumIn != null) {
            try {
                checksumIn.close();
            } catch (IOException e) {
                ioe = e;
            }
            checksumIn = null;
        }
        if (blockIn != null) {
            try {
                blockIn.close();
            } catch (IOException e) {
                ioe = e;
            }
            blockIn = null;
        }
        if (ioe != null) {
            throw ioe;
        }
    }

    /**
   * Converts an IOExcpetion (not subclasses) to SocketException.
   * This is typically done to indicate to upper layers that the error 
   * was a socket error rather than often more serious exceptions like 
   * disk errors.
   */
    private static IOException ioeToSocketException(IOException ioe) {
        if (ioe.getClass().equals(IOException.class)) {
            IOException se = new SocketException("Original Exception : " + ioe);
            se.initCause(ioe);
            se.setStackTrace(ioe.getStackTrace());
            return se;
        }
        return ioe;
    }

    /**
   * Sends upto maxChunks chunks of data.
   * 
   * When blockInPosition is >= 0, assumes 'out' is a 
   * {@link SocketOutputStream} and tries 
   * {@link SocketOutputStream#transferToFully(FileChannel, long, int)} to
   * send data (and updates blockInPosition).
   */
    private int sendChunks(ByteBuffer pkt, int maxChunks, OutputStream out) throws IOException {
        int len = Math.min((int) (endOffset - offset), bytesPerChecksum * maxChunks);
        if (len == 0) {
            return 0;
        }
        int numChunks = (len + bytesPerChecksum - 1) / bytesPerChecksum;
        int packetLen = len + numChunks * checksumSize + 4;
        pkt.clear();
        pkt.putInt(packetLen);
        pkt.putLong(offset);
        pkt.putLong(seqno);
        pkt.put((byte) ((offset + len >= endOffset) ? 1 : 0));
        pkt.putInt(len);
        int checksumOff = pkt.position();
        int checksumLen = numChunks * checksumSize;
        byte[] buf = pkt.array();
        if (checksumSize > 0 && checksumIn != null) {
            try {
                checksumIn.readFully(buf, checksumOff, checksumLen);
            } catch (IOException e) {
                LOG.warn(" Could not read or failed to veirfy checksum for data" + " at offset " + offset + " for block " + block + " got : " + StringUtils.stringifyException(e));
                IOUtils.closeStream(checksumIn);
                checksumIn = null;
                if (corruptChecksumOk) {
                    if (checksumOff < checksumLen) {
                        Arrays.fill(buf, checksumOff, checksumLen, (byte) 0);
                    }
                } else {
                    throw e;
                }
            }
        }
        int dataOff = checksumOff + checksumLen;
        if (blockInPosition < 0) {
            IOUtils.readFully(blockIn, buf, dataOff, len);
            if (verifyChecksum) {
                int dOff = dataOff;
                int cOff = checksumOff;
                int dLeft = len;
                for (int i = 0; i < numChunks; i++) {
                    checksum.reset();
                    int dLen = Math.min(dLeft, bytesPerChecksum);
                    checksum.update(buf, dOff, dLen);
                    if (!checksum.compare(buf, cOff)) {
                        throw new ChecksumException("Checksum failed at " + (offset + len - dLeft), len);
                    }
                    dLeft -= dLen;
                    dOff += dLen;
                    cOff += checksumSize;
                }
            }
        }
        try {
            if (blockInPosition >= 0) {
                SocketOutputStream sockOut = (SocketOutputStream) out;
                sockOut.write(buf, 0, dataOff);
                sockOut.transferToFully(((FileInputStream) blockIn).getChannel(), blockInPosition, len);
                blockInPosition += len;
            } else {
                out.write(buf, 0, dataOff + len);
            }
        } catch (IOException e) {
            throw ioeToSocketException(e);
        }
        if (throttler != null) {
            throttler.throttle(packetLen);
        }
        return len;
    }

    /**
   * sendBlock() is used to read block and its metadata and stream the data to
   * either a client or to another datanode. 
   * 
   * @param out  stream to which the block is written to
   * @param baseStream optional. if non-null, <code>out</code> is assumed to 
   *        be a wrapper over this stream. This enables optimizations for
   *        sending the data, e.g. 
   *        {@link SocketOutputStream#transferToFully(FileChannel, 
   *        long, int)}.
   * @param throttler for sending data.
   * @return total bytes reads, including crc.
   */
    long sendBlock(DataOutputStream out, OutputStream baseStream, BlockTransferThrottler throttler) throws IOException {
        if (out == null) {
            throw new IOException("out stream is null");
        }
        this.throttler = throttler;
        long initialOffset = offset;
        long totalRead = 0;
        OutputStream streamForSendChunks = out;
        try {
            try {
                checksum.writeHeader(out);
                if (chunkOffsetOK) {
                    out.writeLong(offset);
                }
                out.flush();
            } catch (IOException e) {
                throw ioeToSocketException(e);
            }
            int maxChunksPerPacket;
            int pktSize = DataNode.PKT_HEADER_LEN + SIZE_OF_INTEGER;
            if (transferToAllowed && !verifyChecksum && baseStream instanceof SocketOutputStream && blockIn instanceof FileInputStream) {
                FileChannel fileChannel = ((FileInputStream) blockIn).getChannel();
                blockInPosition = fileChannel.position();
                streamForSendChunks = baseStream;
                maxChunksPerPacket = (Math.max(BUFFER_SIZE, MIN_BUFFER_WITH_TRANSFERTO) + bytesPerChecksum - 1) / bytesPerChecksum;
                pktSize += checksumSize * maxChunksPerPacket;
            } else {
                maxChunksPerPacket = Math.max(1, (BUFFER_SIZE + bytesPerChecksum - 1) / bytesPerChecksum);
                pktSize += (bytesPerChecksum + checksumSize) * maxChunksPerPacket;
            }
            ByteBuffer pktBuf = ByteBuffer.allocate(pktSize);
            while (endOffset > offset) {
                long len = sendChunks(pktBuf, maxChunksPerPacket, streamForSendChunks);
                offset += len;
                totalRead += len + ((len + bytesPerChecksum - 1) / bytesPerChecksum * checksumSize);
                seqno++;
            }
            try {
                out.writeInt(0);
                out.flush();
            } catch (IOException e) {
                throw ioeToSocketException(e);
            }
        } finally {
            if (clientTraceFmt != null) {
                ClientTraceLog.info(String.format(clientTraceFmt, totalRead));
            }
            close();
        }
        blockReadFully = (initialOffset == 0 && offset >= blockLength);
        return totalRead;
    }

    boolean isBlockReadFully() {
        return blockReadFully;
    }
}
