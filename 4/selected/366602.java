package org.apache.hadoop.hdfs.server.datanode;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import org.apache.commons.logging.Log;
import org.apache.hadoop.fs.FSInputChecker;
import org.apache.hadoop.fs.FSOutputSummer;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.DataTransferProtocol;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.util.Daemon;
import org.apache.hadoop.util.DataChecksum;
import org.apache.hadoop.util.StringUtils;
import static org.apache.hadoop.hdfs.server.datanode.DataNode.DN_CLIENTTRACE_FORMAT;

/** A class that receives a block and writes to its own disk, meanwhile
 * may copies it to another site. If a throttler is provided,
 * streaming throttling is also supported.
 **/
class BlockReceiver implements java.io.Closeable, FSConstants {

    public static final Log LOG = DataNode.LOG;

    static final Log ClientTraceLog = DataNode.ClientTraceLog;

    private Block block;

    protected boolean finalized;

    private DataInputStream in = null;

    private DataChecksum checksum;

    private OutputStream out = null;

    private DataOutputStream checksumOut = null;

    private int bytesPerChecksum;

    private int checksumSize;

    private ByteBuffer buf;

    private int bufRead;

    private int maxPacketReadLen;

    protected long offsetInBlock;

    protected final String inAddr;

    protected final String myAddr;

    private String mirrorAddr;

    private DataOutputStream mirrorOut;

    private Daemon responder = null;

    private BlockTransferThrottler throttler;

    private FSDataset.BlockWriteStreams streams;

    private boolean isRecovery = false;

    private String clientName;

    DatanodeInfo srcDataNode = null;

    private Checksum partialCrc = null;

    private DataNode datanode = null;

    BlockReceiver(Block block, DataInputStream in, String inAddr, String myAddr, boolean isRecovery, String clientName, DatanodeInfo srcDataNode, DataNode datanode) throws IOException {
        try {
            this.block = block;
            this.in = in;
            this.inAddr = inAddr;
            this.myAddr = myAddr;
            this.isRecovery = isRecovery;
            this.clientName = clientName;
            this.offsetInBlock = 0;
            this.srcDataNode = srcDataNode;
            this.datanode = datanode;
            this.checksum = DataChecksum.newDataChecksum(in);
            this.bytesPerChecksum = checksum.getBytesPerChecksum();
            this.checksumSize = checksum.getChecksumSize();
            streams = datanode.data.writeToBlock(block, isRecovery);
            this.finalized = datanode.data.isValidBlock(block);
            if (streams != null) {
                this.out = streams.dataOut;
                this.checksumOut = new DataOutputStream(new BufferedOutputStream(streams.checksumOut, SMALL_BUFFER_SIZE));
                if (datanode.blockScanner != null && isRecovery) {
                    datanode.blockScanner.deleteBlock(block);
                }
            }
        } catch (BlockAlreadyExistsException bae) {
            throw bae;
        } catch (IOException ioe) {
            IOUtils.closeStream(this);
            cleanupBlock();
            IOException cause = FSDataset.getCauseIfDiskError(ioe);
            if (cause != null) {
                ioe = cause;
                datanode.checkDiskError(ioe);
            }
            throw ioe;
        }
    }

    /**
   * close files.
   */
    public void close() throws IOException {
        IOException ioe = null;
        try {
            if (checksumOut != null) {
                checksumOut.flush();
                checksumOut.close();
                checksumOut = null;
            }
        } catch (IOException e) {
            ioe = e;
        }
        try {
            if (out != null) {
                out.flush();
                out.close();
                out = null;
            }
        } catch (IOException e) {
            ioe = e;
        }
        if (ioe != null) {
            datanode.checkDiskError(ioe);
            throw ioe;
        }
    }

    /**
   * Flush block data and metadata files to disk.
   * @throws IOException
   */
    void flush() throws IOException {
        if (checksumOut != null) {
            checksumOut.flush();
        }
        if (out != null) {
            out.flush();
        }
    }

    /**
   * While writing to mirrorOut, failure to write to mirror should not
   * affect this datanode unless a client is writing the block.
   */
    private void handleMirrorOutError(IOException ioe) throws IOException {
        LOG.info(datanode.dnRegistration + ":Exception writing block " + block + " to mirror " + mirrorAddr + "\n" + StringUtils.stringifyException(ioe));
        mirrorOut = null;
        if (clientName.length() > 0) {
            throw ioe;
        }
    }

    /**
   * Verify multiple CRC chunks. 
   */
    private void verifyChunks(byte[] dataBuf, int dataOff, int len, byte[] checksumBuf, int checksumOff) throws IOException {
        while (len > 0) {
            int chunkLen = Math.min(len, bytesPerChecksum);
            checksum.update(dataBuf, dataOff, chunkLen);
            if (!checksum.compare(checksumBuf, checksumOff)) {
                if (srcDataNode != null) {
                    try {
                        LOG.info("report corrupt block " + block + " from datanode " + srcDataNode + " to namenode");
                        LocatedBlock lb = new LocatedBlock(block, new DatanodeInfo[] { srcDataNode });
                        datanode.namenode.reportBadBlocks(new LocatedBlock[] { lb });
                    } catch (IOException e) {
                        LOG.warn("Failed to report bad block " + block + " from datanode " + srcDataNode + " to namenode");
                    }
                }
                throw new IOException("Unexpected checksum mismatch " + "while writing " + block + " from " + inAddr);
            }
            checksum.reset();
            dataOff += chunkLen;
            checksumOff += checksumSize;
            len -= chunkLen;
        }
    }

    /**
   * Makes sure buf.position() is zero without modifying buf.remaining().
   * It moves the data if position needs to be changed.
   */
    private void shiftBufData() {
        if (bufRead != buf.limit()) {
            throw new IllegalStateException("bufRead should be same as " + "buf.limit()");
        }
        if (buf.position() > 0) {
            int dataLeft = buf.remaining();
            if (dataLeft > 0) {
                byte[] b = buf.array();
                System.arraycopy(b, buf.position(), b, 0, dataLeft);
            }
            buf.position(0);
            bufRead = dataLeft;
            buf.limit(bufRead);
        }
    }

    /**
   * reads upto toRead byte to buf at buf.limit() and increments the limit.
   * throws an IOException if read does not succeed.
   */
    private int readToBuf(int toRead) throws IOException {
        if (toRead < 0) {
            toRead = (maxPacketReadLen > 0 ? maxPacketReadLen : buf.capacity()) - buf.limit();
        }
        int nRead = in.read(buf.array(), buf.limit(), toRead);
        if (nRead < 0) {
            throw new EOFException("while trying to read " + toRead + " bytes");
        }
        bufRead = buf.limit() + nRead;
        buf.limit(bufRead);
        return nRead;
    }

    /**
   * Reads (at least) one packet and returns the packet length.
   * buf.position() points to the start of the packet and 
   * buf.limit() point to the end of the packet. There could 
   * be more data from next packet in buf.<br><br>
   * 
   * It tries to read a full packet with single read call.
   * Consecutive packets are usually of the same length.
   */
    private int readNextPacket() throws IOException {
        if (buf == null) {
            int chunkSize = bytesPerChecksum + checksumSize;
            int chunksPerPacket = (datanode.writePacketSize - DataNode.PKT_HEADER_LEN - SIZE_OF_INTEGER + chunkSize - 1) / chunkSize;
            buf = ByteBuffer.allocate(DataNode.PKT_HEADER_LEN + SIZE_OF_INTEGER + Math.max(chunksPerPacket, 1) * chunkSize);
            buf.limit(0);
        }
        if (bufRead > buf.limit()) {
            buf.limit(bufRead);
        }
        while (buf.remaining() < SIZE_OF_INTEGER) {
            if (buf.position() > 0) {
                shiftBufData();
            }
            readToBuf(-1);
        }
        buf.mark();
        int payloadLen = buf.getInt();
        buf.reset();
        if (payloadLen == 0) {
            buf.limit(buf.position() + SIZE_OF_INTEGER);
            return 0;
        }
        if (payloadLen < 0 || payloadLen > (100 * 1024 * 1024)) {
            throw new IOException("Incorrect value for packet payload : " + payloadLen);
        }
        int pktSize = payloadLen + DataNode.PKT_HEADER_LEN;
        if (buf.remaining() < pktSize) {
            int toRead = pktSize - buf.remaining();
            int spaceLeft = buf.capacity() - buf.limit();
            if (toRead > spaceLeft && buf.position() > 0) {
                shiftBufData();
                spaceLeft = buf.capacity() - buf.limit();
            }
            if (toRead > spaceLeft) {
                byte oldBuf[] = buf.array();
                int toCopy = buf.limit();
                buf = ByteBuffer.allocate(toCopy + toRead);
                System.arraycopy(oldBuf, 0, buf.array(), 0, toCopy);
                buf.limit(toCopy);
            }
            while (toRead > 0) {
                toRead -= readToBuf(toRead);
            }
        }
        if (buf.remaining() > pktSize) {
            buf.limit(buf.position() + pktSize);
        }
        if (pktSize > maxPacketReadLen) {
            maxPacketReadLen = pktSize;
        }
        return payloadLen;
    }

    /** 
   * Receives and processes a packet. It can contain many chunks.
   * returns size of the packet.
   */
    private int receivePacket() throws IOException {
        int payloadLen = readNextPacket();
        if (payloadLen <= 0) {
            return payloadLen;
        }
        buf.mark();
        buf.getInt();
        offsetInBlock = buf.getLong();
        long seqno = buf.getLong();
        boolean lastPacketInBlock = (buf.get() != 0);
        int endOfHeader = buf.position();
        buf.reset();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Receiving one packet for block " + block + " of length " + payloadLen + " seqno " + seqno + " offsetInBlock " + offsetInBlock + " lastPacketInBlock " + lastPacketInBlock);
        }
        setBlockPosition(offsetInBlock);
        if (mirrorOut != null) {
            try {
                mirrorOut.write(buf.array(), buf.position(), buf.remaining());
                mirrorOut.flush();
            } catch (IOException e) {
                handleMirrorOutError(e);
            }
        }
        buf.position(endOfHeader);
        int len = buf.getInt();
        if (len < 0) {
            throw new IOException("Got wrong length during writeBlock(" + block + ") from " + inAddr + " at offset " + offsetInBlock + ": " + len);
        }
        if (len == 0) {
            LOG.debug("Receiving empty packet for block " + block);
        } else {
            offsetInBlock += len;
            int checksumLen = ((len + bytesPerChecksum - 1) / bytesPerChecksum) * checksumSize;
            if (buf.remaining() != (checksumLen + len)) {
                throw new IOException("Data remaining in packet does not match " + "sum of checksumLen and dataLen");
            }
            int checksumOff = buf.position();
            int dataOff = checksumOff + checksumLen;
            byte pktBuf[] = buf.array();
            buf.position(buf.limit());
            if (mirrorOut == null || clientName.length() == 0) {
                verifyChunks(pktBuf, dataOff, len, pktBuf, checksumOff);
            }
            try {
                if (!finalized) {
                    out.write(pktBuf, dataOff, len);
                    if (partialCrc != null) {
                        if (len > bytesPerChecksum) {
                            throw new IOException("Got wrong length during writeBlock(" + block + ") from " + inAddr + " " + "A packet can have only one partial chunk." + " len = " + len + " bytesPerChecksum " + bytesPerChecksum);
                        }
                        partialCrc.update(pktBuf, dataOff, len);
                        byte[] buf = FSOutputSummer.convertToByteStream(partialCrc, checksumSize);
                        checksumOut.write(buf);
                        LOG.debug("Writing out partial crc for data len " + len);
                        partialCrc = null;
                    } else {
                        checksumOut.write(pktBuf, checksumOff, checksumLen);
                    }
                    datanode.myMetrics.bytesWritten.inc(len);
                }
            } catch (IOException iex) {
                datanode.checkDiskError(iex);
                throw iex;
            }
        }
        flush();
        if (responder != null) {
            ((PacketResponder) responder.getRunnable()).enqueue(seqno, lastPacketInBlock);
        }
        if (throttler != null) {
            throttler.throttle(payloadLen);
        }
        return payloadLen;
    }

    void writeChecksumHeader(DataOutputStream mirrorOut) throws IOException {
        checksum.writeHeader(mirrorOut);
    }

    void receiveBlock(DataOutputStream mirrOut, DataInputStream mirrIn, DataOutputStream replyOut, String mirrAddr, BlockTransferThrottler throttlerArg, int numTargets) throws IOException {
        mirrorOut = mirrOut;
        mirrorAddr = mirrAddr;
        throttler = throttlerArg;
        try {
            if (!finalized) {
                BlockMetadataHeader.writeHeader(checksumOut, checksum);
            }
            if (clientName.length() > 0) {
                responder = new Daemon(datanode.threadGroup, new PacketResponder(this, block, mirrIn, replyOut, numTargets));
                responder.start();
            }
            while (receivePacket() > 0) {
            }
            if (mirrorOut != null) {
                try {
                    mirrorOut.writeInt(0);
                    mirrorOut.flush();
                } catch (IOException e) {
                    handleMirrorOutError(e);
                }
            }
            if (responder != null) {
                ((PacketResponder) responder.getRunnable()).close();
            }
            if (clientName.length() == 0) {
                close();
                block.setNumBytes(offsetInBlock);
                datanode.data.finalizeBlock(block);
                datanode.myMetrics.blocksWritten.inc();
            }
        } catch (IOException ioe) {
            LOG.info("Exception in receiveBlock for block " + block + " " + ioe);
            IOUtils.closeStream(this);
            if (responder != null) {
                responder.interrupt();
            }
            cleanupBlock();
            throw ioe;
        } finally {
            if (responder != null) {
                try {
                    responder.join();
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted receiveBlock");
                }
                responder = null;
            }
        }
    }

    /** Cleanup a partial block 
   * if this write is for a replication request (and not from a client)
   */
    private void cleanupBlock() throws IOException {
        if (clientName.length() == 0) {
            datanode.data.unfinalizeBlock(block);
        }
    }

    /**
   * Sets the file pointer in the local block file to the specified value.
   */
    private void setBlockPosition(long offsetInBlock) throws IOException {
        if (finalized) {
            if (!isRecovery) {
                throw new IOException("Write to offset " + offsetInBlock + " of block " + block + " that is already finalized.");
            }
            if (offsetInBlock > datanode.data.getLength(block)) {
                throw new IOException("Write to offset " + offsetInBlock + " of block " + block + " that is already finalized and is of size " + datanode.data.getLength(block));
            }
            return;
        }
        if (datanode.data.getChannelPosition(block, streams) == offsetInBlock) {
            return;
        }
        long offsetInChecksum = BlockMetadataHeader.getHeaderSize() + offsetInBlock / bytesPerChecksum * checksumSize;
        if (out != null) {
            out.flush();
        }
        if (checksumOut != null) {
            checksumOut.flush();
        }
        if (offsetInBlock % bytesPerChecksum != 0) {
            LOG.info("setBlockPosition trying to set position to " + offsetInBlock + " for block " + block + " which is not a multiple of bytesPerChecksum " + bytesPerChecksum);
            computePartialChunkCrc(offsetInBlock, offsetInChecksum, bytesPerChecksum);
        }
        LOG.info("Changing block file offset of block " + block + " from " + datanode.data.getChannelPosition(block, streams) + " to " + offsetInBlock + " meta file offset to " + offsetInChecksum);
        datanode.data.setChannelPosition(block, streams, offsetInBlock, offsetInChecksum);
    }

    /**
   * reads in the partial crc chunk and computes checksum
   * of pre-existing data in partial chunk.
   */
    private void computePartialChunkCrc(long blkoff, long ckoff, int bytesPerChecksum) throws IOException {
        int sizePartialChunk = (int) (blkoff % bytesPerChecksum);
        int checksumSize = checksum.getChecksumSize();
        blkoff = blkoff - sizePartialChunk;
        LOG.info("computePartialChunkCrc sizePartialChunk " + sizePartialChunk + " block " + block + " offset in block " + blkoff + " offset in metafile " + ckoff);
        byte[] buf = new byte[sizePartialChunk];
        byte[] crcbuf = new byte[checksumSize];
        FSDataset.BlockInputStreams instr = null;
        try {
            instr = datanode.data.getTmpInputStreams(block, blkoff, ckoff);
            IOUtils.readFully(instr.dataIn, buf, 0, sizePartialChunk);
            IOUtils.readFully(instr.checksumIn, crcbuf, 0, crcbuf.length);
        } finally {
            IOUtils.closeStream(instr);
        }
        partialCrc = new CRC32();
        partialCrc.update(buf, 0, sizePartialChunk);
        LOG.info("Read in partial CRC chunk from disk for block " + block);
        if (partialCrc.getValue() != FSInputChecker.checksum2long(crcbuf)) {
            String msg = "Partial CRC " + partialCrc.getValue() + " does not match value computed the " + " last time file was closed " + FSInputChecker.checksum2long(crcbuf);
            throw new IOException(msg);
        }
    }

    /**
   * Processed responses from downstream datanodes in the pipeline
   * and sends back replies to the originator.
   */
    class PacketResponder implements Runnable, FSConstants {

        private LinkedList<Packet> ackQueue = new LinkedList<Packet>();

        private volatile boolean running = true;

        private Block block;

        DataInputStream mirrorIn;

        DataOutputStream replyOut;

        private int numTargets;

        private BlockReceiver receiver;

        public String toString() {
            return "PacketResponder " + numTargets + " for Block " + this.block;
        }

        PacketResponder(BlockReceiver receiver, Block b, DataInputStream in, DataOutputStream out, int numTargets) {
            this.receiver = receiver;
            this.block = b;
            mirrorIn = in;
            replyOut = out;
            this.numTargets = numTargets;
        }

        /**
     * enqueue the seqno that is still be to acked by the downstream datanode.
     * @param seqno
     * @param lastPacketInBlock
     */
        synchronized void enqueue(long seqno, boolean lastPacketInBlock) {
            if (running) {
                LOG.debug("PacketResponder " + numTargets + " adding seqno " + seqno + " to ack queue.");
                ackQueue.addLast(new Packet(seqno, lastPacketInBlock));
                notifyAll();
            }
        }

        /**
     * wait for all pending packets to be acked. Then shutdown thread.
     */
        synchronized void close() {
            while (running && ackQueue.size() != 0 && datanode.shouldRun) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    running = false;
                }
            }
            LOG.debug("PacketResponder " + numTargets + " for block " + block + " Closing down.");
            running = false;
            notifyAll();
        }

        private synchronized void lastDataNodeRun() {
            long lastHeartbeat = System.currentTimeMillis();
            boolean lastPacket = false;
            while (running && datanode.shouldRun && !lastPacket) {
                long now = System.currentTimeMillis();
                try {
                    while (running && datanode.shouldRun && ackQueue.size() == 0) {
                        long idle = now - lastHeartbeat;
                        long timeout = (datanode.socketTimeout / 2) - idle;
                        if (timeout <= 0) {
                            timeout = 1000;
                        }
                        try {
                            wait(timeout);
                        } catch (InterruptedException e) {
                            if (running) {
                                LOG.info("PacketResponder " + numTargets + " for block " + block + " Interrupted.");
                                running = false;
                            }
                            break;
                        }
                        now = System.currentTimeMillis();
                        if (now - lastHeartbeat > datanode.socketTimeout / 2) {
                            replyOut.writeLong(-1);
                            replyOut.flush();
                            lastHeartbeat = now;
                        }
                    }
                    if (!running || !datanode.shouldRun) {
                        break;
                    }
                    Packet pkt = ackQueue.removeFirst();
                    long expected = pkt.seqno;
                    notifyAll();
                    LOG.debug("PacketResponder " + numTargets + " for block " + block + " acking for packet " + expected);
                    if (pkt.lastPacketInBlock) {
                        if (!receiver.finalized) {
                            receiver.close();
                            block.setNumBytes(receiver.offsetInBlock);
                            datanode.data.finalizeBlock(block);
                            datanode.myMetrics.blocksWritten.inc();
                            datanode.notifyNamenodeReceivedBlock(block, DataNode.EMPTY_DEL_HINT);
                            if (ClientTraceLog.isInfoEnabled() && receiver.clientName.length() > 0) {
                                ClientTraceLog.info(String.format(DN_CLIENTTRACE_FORMAT, receiver.inAddr, receiver.myAddr, block.getNumBytes(), "HDFS_WRITE", receiver.clientName, datanode.dnRegistration.getStorageID(), block));
                            } else {
                                LOG.info("Received block " + block + " of size " + block.getNumBytes() + " from " + receiver.inAddr);
                            }
                        }
                        lastPacket = true;
                    }
                    replyOut.writeLong(expected);
                    replyOut.writeShort(DataTransferProtocol.OP_STATUS_SUCCESS);
                    replyOut.flush();
                } catch (Exception e) {
                    if (running) {
                        LOG.info("PacketResponder " + block + " " + numTargets + " Exception " + StringUtils.stringifyException(e));
                        running = false;
                    }
                }
            }
            LOG.info("PacketResponder " + numTargets + " for block " + block + " terminating");
        }

        /**
     * Thread to process incoming acks.
     * @see java.lang.Runnable#run()
     */
        public void run() {
            if (numTargets == 0) {
                lastDataNodeRun();
                return;
            }
            boolean lastPacketInBlock = false;
            while (running && datanode.shouldRun && !lastPacketInBlock) {
                try {
                    short op = DataTransferProtocol.OP_STATUS_SUCCESS;
                    boolean didRead = false;
                    long expected = -2;
                    try {
                        long seqno = mirrorIn.readLong();
                        didRead = true;
                        if (seqno == -1) {
                            replyOut.writeLong(-1);
                            replyOut.flush();
                            LOG.debug("PacketResponder " + numTargets + " got -1");
                            continue;
                        } else if (seqno == -2) {
                            LOG.debug("PacketResponder " + numTargets + " got -2");
                        } else {
                            LOG.debug("PacketResponder " + numTargets + " got seqno = " + seqno);
                            Packet pkt = null;
                            synchronized (this) {
                                while (running && datanode.shouldRun && ackQueue.size() == 0) {
                                    if (LOG.isDebugEnabled()) {
                                        LOG.debug("PacketResponder " + numTargets + " seqno = " + seqno + " for block " + block + " waiting for local datanode to finish write.");
                                    }
                                    wait();
                                }
                                pkt = ackQueue.removeFirst();
                                expected = pkt.seqno;
                                notifyAll();
                                LOG.debug("PacketResponder " + numTargets + " seqno = " + seqno);
                                if (seqno != expected) {
                                    throw new IOException("PacketResponder " + numTargets + " for block " + block + " expected seqno:" + expected + " received:" + seqno);
                                }
                                lastPacketInBlock = pkt.lastPacketInBlock;
                            }
                        }
                    } catch (Throwable e) {
                        if (running) {
                            LOG.info("PacketResponder " + block + " " + numTargets + " Exception " + StringUtils.stringifyException(e));
                            running = false;
                        }
                    }
                    if (Thread.interrupted()) {
                        LOG.info("PacketResponder " + block + " " + numTargets + " : Thread is interrupted.");
                        running = false;
                        continue;
                    }
                    if (!didRead) {
                        op = DataTransferProtocol.OP_STATUS_ERROR;
                    }
                    if (lastPacketInBlock && !receiver.finalized) {
                        receiver.close();
                        block.setNumBytes(receiver.offsetInBlock);
                        datanode.data.finalizeBlock(block);
                        datanode.myMetrics.blocksWritten.inc();
                        datanode.notifyNamenodeReceivedBlock(block, DataNode.EMPTY_DEL_HINT);
                        if (ClientTraceLog.isInfoEnabled() && receiver.clientName.length() > 0) {
                            ClientTraceLog.info(String.format(DN_CLIENTTRACE_FORMAT, receiver.inAddr, receiver.myAddr, block.getNumBytes(), "HDFS_WRITE", receiver.clientName, datanode.dnRegistration.getStorageID(), block));
                        } else {
                            LOG.info("Received block " + block + " of size " + block.getNumBytes() + " from " + receiver.inAddr);
                        }
                    }
                    replyOut.writeLong(expected);
                    replyOut.writeShort(DataTransferProtocol.OP_STATUS_SUCCESS);
                    LOG.debug("PacketResponder " + numTargets + " for block " + block + " responded my status " + " for seqno " + expected);
                    for (int i = 0; i < numTargets && datanode.shouldRun; i++) {
                        try {
                            if (op == DataTransferProtocol.OP_STATUS_SUCCESS) {
                                op = mirrorIn.readShort();
                                if (op != DataTransferProtocol.OP_STATUS_SUCCESS) {
                                    LOG.debug("PacketResponder for block " + block + ": error code received from downstream " + " datanode[" + i + "] " + op);
                                }
                            }
                        } catch (Throwable e) {
                            op = DataTransferProtocol.OP_STATUS_ERROR;
                        }
                        replyOut.writeShort(op);
                    }
                    replyOut.flush();
                    LOG.debug("PacketResponder " + block + " " + numTargets + " responded other status " + " for seqno " + expected);
                    if (expected == -2) {
                        running = false;
                    }
                    if (op == DataTransferProtocol.OP_STATUS_ERROR && receiver.clientName.length() > 0) {
                        running = false;
                    }
                } catch (IOException e) {
                    if (running) {
                        LOG.info("PacketResponder " + block + " " + numTargets + " Exception " + StringUtils.stringifyException(e));
                        running = false;
                    }
                } catch (RuntimeException e) {
                    if (running) {
                        LOG.info("PacketResponder " + block + " " + numTargets + " Exception " + StringUtils.stringifyException(e));
                        running = false;
                    }
                }
            }
            LOG.info("PacketResponder " + numTargets + " for block " + block + " terminating");
        }
    }

    /**
   * This information is cached by the Datanode in the ackQueue.
   */
    private static class Packet {

        long seqno;

        boolean lastPacketInBlock;

        Packet(long seqno, boolean lastPacketInBlock) {
            this.seqno = seqno;
            this.lastPacketInBlock = lastPacketInBlock;
        }
    }
}
