package com.onionnetworks.fec.io;

import java.io.*;
import java.util.*;
import java.security.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.onionnetworks.fec.FECCode;
import com.onionnetworks.fec.FECCodeFactory;
import com.onionnetworks.io.RAF;
import com.onionnetworks.util.*;
import EDU.oswego.cs.dl.util.concurrent.*;

/**
 * This class provides the necessary file IO routines to go along with the raw
 * FEC codes.  It is completely thread safe for multiple readers and writers,
 * and will automatically encode and decode the packets ass necessary.  If
 * the FECFile is originally opened in "rw" mode it will revert to "r" mode
 * once it has recieved enough data and decoded the entire file.
 *
 * File Encoding Example:
 * <code>
 *   File f = new File(args[0]);
 * 
 *   int k = 32;  // source packet per block
 *   int n = 256; // number of packets to expand each block to.
 *   int packetSize = 1024; // number of bytes in each packet.
 *
 *   FECParameters params = new FECParameters(k,n,packetSize,f.length());
 *
 *   FECFile fecF = new FECFile(f,"r",params);
 *
 *   // Read the packet with blockNum=0,stripeNum=32 into the Buffer
 *   // The read() interface is much faster if you encode multiple packets
 *   // per block and encourages you to do so.  We are not going to for
 *   // simplicity.
 *   Buffer b = new Buffer(packetSize);
 *   fecF.read(new Buffer[] {b}, 0, new int[] {32});
 * </code>
 *  
 * Please see tests/FECFileTest for a further example.
 *
 * <lu>
 * Thread/synchronization behavior:
 *
 * The FECFile tries to maintain as little contention as possible, but seeing
 * as it involves both IO intensive and processor intensive functions, this is
 * rather difficult.
 *
 * <li> Disk IO is fully synchronized.  This doesn't really make any difference
 * because you can't do two parallel IO operations on the same file anyway.
 *
 * <li> The highest level of synchronization is per-block reader/writer locks. 
 * This means that any number of threads may read/encode at parallel and that
 * a write or decode operation blocks all other threads trying to access the
 * same block.  Since encoding is a CPU intensive task, there will be minimal 
 * gains from parallel encoding, but the opportunity for encoding and disk IO 
 * to occur in parallel may afford some savings.
 * </lu>
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public class FECFile {

    private static Log log = LogFactory.getLog(FECFile.class);

    private FECParameters params;

    private RAF raf;

    private PacketPlacement pp;

    private FileIntegrity integrity;

    private MessageDigest md;

    private FECCode code;

    private ReflectiveEventDispatch dispatch;

    private int k, n, packetSize, blockSize, blockCount;

    private ReadWriteLock[] locks;

    private Decoder decoder;

    protected ExceptionHandler exceptionHandler;

    /**
     * Open the file according to <code>mode</code (either "r" or "rw") and 
     * encoding/decoding according to the FECParameters.  This constructor
     * will simply set the destination file to be the same as f if it is opened
     * in "rw" mode.
     *
     * @param f The File to which the data will be read/written
     * @param mode Either "r" or "rw"
     * @param params The FECParameters that specify how the data will be
     * encoded and decoded.
     */
    public FECFile(File f, String mode, FECParameters params) throws IOException {
        this(f, mode, params, null);
    }

    /**
     * Open the file according to <code>mode</code (either "r" or "rw") and 
     * encoding/decoding according to the FECParameters.  This constructor
     * will simply set the destination file to be the same as f if it is opened
     * in "rw" mode.
     *
     * @param f The File to which the data will be read/written
     * @param mode Either "r" or "rw"
     * @param params The FECParameters that specify how the data will be
     * encoded and decoded.
     * @param integrity Used for checking the file integrity.
     */
    public FECFile(File f, String mode, FECParameters params, FileIntegrity integrity) throws IOException {
        this.params = params;
        this.integrity = integrity;
        this.k = params.getK();
        this.n = params.getN();
        this.packetSize = params.getPacketSize();
        this.blockSize = params.getUnexpandedBlockSize();
        this.blockCount = params.getBlockCount();
        this.code = FECCodeFactory.getDefault().createFECCode(k, n);
        this.raf = new RAF(f, mode);
        locks = new ReadWriteLock[blockCount];
        for (int i = 0; i < locks.length; i++) {
            locks[i] = new ReentrantWriterPreferenceReadWriteLock();
        }
        setDecodeExceptionHandler(new ExceptionHandler() {

            public void handleException(ExceptionEvent ev) {
                log.warn("Set an ExceptionHandler via " + "FECFile.setDecodeExceptionHandler()", ev.getException());
            }
        });
        if (mode.equals("rw")) {
            dispatch = new ReflectiveEventDispatch();
            this.pp = new PacketPlacement(params);
            decoder = new Decoder();
            addFECIOListener(decoder);
            new Thread(decoder, "Decoder Thread").start();
        } else {
            if (integrity != null) {
                throw new IllegalArgumentException("Integrity not used in read-only mode");
            }
        }
    }

    /**
     * Sets the ExceptionHandler for dealing with problems that occur during
     * decoding.
     * 
     * @param ExceptionHandler This object that will handle the exception.
     */
    public synchronized void setDecodeExceptionHandler(ExceptionHandler eh) {
        this.exceptionHandler = eh;
    }

    protected synchronized ExceptionHandler getDecodeExceptionHandler() {
        return exceptionHandler;
    }

    protected static final byte[] createBuffer(int len) {
        return new byte[len];
    }

    protected final byte[] createBuffer() {
        return createBuffer(blockSize);
    }

    protected final Buffer[] wrapBuffer(byte[] b) {
        if (b.length != blockSize) {
            throw new IllegalArgumentException("b.length != " + blockSize);
        }
        Buffer[] bufs = new Buffer[k];
        for (int i = 0; i < bufs.length; i++) {
            bufs[i] = new Buffer(b, i * packetSize, packetSize);
        }
        return bufs;
    }

    /**
     * When the File is opened in r/w mode you must specify a destination.
     * This method allows you to specify the destination lazily to allow
     * downloads to take place before the user has even chosen the final
     * location with their file picker.  Be ware that the final write to
     * that would cause the file to be moved and re-opened will block until
     * the destination file is set, so make sure to set it asap.
     */
    public void renameTo(File destFile) throws IOException {
        raf.renameTo(destFile);
    }

    /**
     * This method reads a number of packets (encoding them if necessary) into
     * the provided buffers.  The method accepts an array of stripeNums and 
     * an array of Buffers because it is vastly more efficient to encode
     * multiple blocks all at once rather than across multiple method calls.
     *
     * Calls to read should be safe even during the rw->r switch because
     * a packet that existed will never become unavailable.
     *
     * @param pkts The array of Buffers into which the data will be stored
     * each Buffer must be <code>params.getPacketSize()</code> in length.
     *
     * @param blockNum The block num of the packets to encode.
     * @param stripeNums The stripe nums of the packets to encode.
     *
     * @throws IOException If there is an IOException in the underlying file 
     * IO.
     * @throws PacketNotFoundException If the desired packet can not be found
     * in the PacketPlacement.
     */
    public void read(Buffer[] pkts, int blockNum, int[] stripeNums) throws IOException, PacketNotFoundException {
        if (blockNum < 0 || blockNum >= blockCount) {
            throw new IllegalArgumentException("Illegal block# : blockNum=" + blockNum + ",stripeNum=" + stripeNums[0]);
        }
        int ubs = -1;
        byte[] b = null;
        try {
            locks[blockNum].readLock().acquire();
            try {
                if (raf.getMode().equals("r") || pp.isBlockDecoded(blockNum)) {
                    ubs = params.getUnexpandedBlockSize(blockNum);
                    b = createBuffer();
                    raf.seekAndReadFully(blockNum * blockSize, b, 0, ubs);
                } else {
                    for (int i = 0; i < stripeNums.length; i++) {
                        if (params.isPaddingPacket(blockNum, stripeNums[i])) {
                            Util.bzero(pkts[i].b, pkts[i].off, pkts[i].len);
                            continue;
                        }
                        int packetIndex = pp.getPacketIndex(blockNum, stripeNums[i]);
                        if (packetIndex == -1) {
                            throw new PacketNotFoundException("Packet not on disk: blockNum=" + blockNum + ",stripeNum=" + stripeNums[i], blockNum, stripeNums[i]);
                        }
                        raf.seekAndReadFully(packetIndex * packetSize, pkts[i].b, pkts[i].off, pkts[i].len);
                    }
                    return;
                }
            } finally {
                locks[blockNum].readLock().release();
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.toString());
        }
        Util.bzero(b, ubs, b.length - ubs);
        code.encode(wrapBuffer(b), pkts, stripeNums);
    }

    /**
     * Writes a packet to disk.  If the packet is the k'th packet that
     * has been written for this block, then the block will be decoded in
     * this call.  Since decoding is a relatively costly process it may
     * cause your program to experience some bursty behavior in how long
     * the write calls take.  None-the-less if you are making any timing
     * judgements based off of IO then you are an idiot.
     *
     * If this packet is the last packet that needs to be written to disk 
     * in order to decode the whole file, the block will be decoded and
     * then the file will be properly truncated, closed, moved to the 
     * destination file and re-opened in read-only mode.  If the destFile
     * has not been set yet then this will block until that is set.
     *
     * @param pkt The Buffer from which the data will be written.
     * @param blockNum The blockNum of the packet to be written.
     * @param stripeNum The stripeNum of the packet to be written.
     *
     * @throws IOException When there is a disk IOException.  
     * @throws DuplicatePacketException When there is an attempt to write a
     *   packet a second time.  
     * @throws BlockAlreadyDecodedException When the block is already decoded. 
     * @throws FileAlreadyDecodedException When you are trying to write a
     *   packet when the file has been decoded and switched to read-only 
     *
     * @return The block packet count after writing this packet.  This
     * gives you the order the packets were written in for this block.
     */
    public int write(Buffer pkt, int blockNum, int stripeNum) throws IOException, FileAlreadyDecodedException {
        int result = -1;
        try {
            locks[blockNum].writeLock().acquire();
            try {
                result = write0(pkt, blockNum, stripeNum);
            } finally {
                locks[blockNum].writeLock().release();
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.toString());
        }
        if (result >= k) {
            Thread.yield();
        }
        return result;
    }

    /**
     * locks[blockNum].writeLock() acquired.
     */
    private int write0(Buffer pkt, int blockNum, int stripeNum) throws IOException, DuplicatePacketException, BlockAlreadyDecodedException, FileAlreadyDecodedException {
        if (raf.getMode().equals("r")) {
            throw new FileAlreadyDecodedException("Attempted to write packet in read-only mode");
        }
        if (params.isPaddingPacket(blockNum, stripeNum)) {
            log.warn("You have attempted to write a padding packet which is " + "is already generated by this FECFile and shouldn't be " + "sent across the network.  Talk to Justin for more info. " + "blockNum=" + blockNum + ",stripeNum=" + stripeNum);
            throw new DuplicatePacketException("Attempted padding packet write. blockNum=" + blockNum + ",stripeNum=" + stripeNum, blockNum, stripeNum, -1);
        }
        if (pp.isBlockDecoded(blockNum)) {
            throw new BlockAlreadyDecodedException("Block already decoded : blockNum=" + blockNum + ",stripeNum=" + stripeNum, blockNum, stripeNum);
        }
        if (pp.getPacketIndex(blockNum, stripeNum) != -1) {
            int i = pp.getPacketIndex(blockNum, stripeNum);
            throw new DuplicatePacketException("Duplicate packet: blockNum=" + blockNum + "stripeNum=" + stripeNum + "index=" + i, blockNum, stripeNum, i);
        }
        int packetIndex = -1;
        int packetCount = -1;
        synchronized (pp) {
            packetIndex = pp.addPacketEntry(blockNum, stripeNum);
            packetCount = pp.getPacketCount(blockNum);
        }
        raf.seekAndWrite(packetIndex * packetSize, pkt.b, pkt.off, pkt.len);
        fire(new PacketWrittenEvent(this, blockNum, stripeNum, packetCount));
        return packetCount;
    }

    /**
     * Try to decode the specified block.  This method will read in the block,
     * decode it, and write the decoded block back to disk.
     * @return true if the block was successfully decoded and verified.
     */
    protected boolean tryDecode(int blockNum, int[] stripeNums) throws IOException {
        log.debug("trying to decode block : blockNum=" + blockNum);
        byte[] b = createBuffer();
        Buffer[] bufs = wrapBuffer(b);
        read(bufs, blockNum, stripeNums);
        code.decode(bufs, stripeNums);
        try {
            locks[blockNum].writeLock().acquire();
            try {
                raf.seekAndWrite(blockNum * blockSize, b, 0, b.length);
                pp.setBlockDecoded(blockNum);
            } finally {
                locks[blockNum].writeLock().release();
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.toString());
        }
        fire(new BlockDecodedEvent(this, blockNum));
        return true;
    }

    /**
     * Acquires all write block locks.  This method ABSOLUTELY CANNOT be called
     * if a blockLock has already been acquired by this thread because
     * of the possibility for deadly embrace.
     */
    public void acquireAllWriteLocks() throws InterruptedException {
        for (int i = 0; i < locks.length; i++) {
            locks[i].writeLock().acquire();
        }
    }

    /**
     * Releases all write block locks.  This method ABSOLUTELY CANNOT be called
     * if a blockLock has already been acquired by this thread because
     * of the possibility for deadly embrace.
     */
    public void releaseAllWriteLocks() throws InterruptedException {
        for (int i = 0; i < locks.length; i++) {
            locks[i].writeLock().release();
        }
    }

    /**
     * Close the underlying file descriptor and free up the resources 
     * associated with this FECFile.
     */
    public void close() throws IOException {
        try {
            acquireAllWriteLocks();
            try {
                raf.close();
            } finally {
                releaseAllWriteLocks();
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException(e.toString());
        }
        if (decoder != null) {
            decoder.close();
        }
        if (dispatch != null) {
            dispatch.close();
        }
    }

    /**
     * Adds a new FECIOListener.
     *
     */
    public void addFECIOListener(FECIOListener fil) {
        if (dispatch == null) {
            throw new IllegalStateException("No events in read-only mode");
        }
        dispatch.addListener(this, fil, FECIOListener.EVENTS);
    }

    public void removeFECIOListener(FECIOListener fil) {
        if (dispatch == null) {
            throw new IllegalStateException("No events in read-only mode");
        }
        dispatch.removeListener(this, fil, FECIOListener.EVENTS);
    }

    /**
     * Fire an event.
     */
    protected void fire(FECIOEvent ev) {
        dispatch.fire(ev, "notify");
    }

    /**
     * @return The decoded block count
     */
    public int getDecodedBlockCount() {
        return pp == null ? blockCount : pp.getDecodedBlockCount();
    }

    /**
     * @return true if the block is decoded.
     */
    public boolean isBlockDecoded(int blockNum) {
        return pp == null ? true : pp.isBlockDecoded(blockNum);
    }

    /**
     * @return true if the FECFile contains the specified packet.
     */
    public boolean containsPacket(int blockNum, int stripeNum) {
        if (pp == null) {
            return true;
        }
        synchronized (pp) {
            if (pp.isBlockDecoded(blockNum)) {
                return true;
            } else {
                return pp.getPacketIndex(blockNum, stripeNum) != -1;
            }
        }
    }

    /**
     * @return The number of packets written to disk.
     */
    public int getWrittenCount() {
        return pp == null ? params.getUnexpandedPacketCount() : pp.getWrittenCount();
    }

    /** 
     * @return The FECParameters used for encoding/decoding. 
     */
    public FECParameters getFECParameters() {
        return params;
    }

    /**
     * @return true if this file is decoded.
     */
    public boolean isDecoded() {
        return raf.getMode().equals("r");
    }

    /**
     * This method blocks and returns once the file has been decoded.  This
     * is primarily so that you know when it is safe to close the FECFile
     * without having to setup your own FECFileListeners to wait for the 
     * FileDecodedEvent.
     */
    public void waitForFileDecoded() throws InterruptedException {
        FECIOListener fil = new FECIOListener() {

            public void notify(FECIOEvent ev) {
                if (ev instanceof FileDecodedEvent) {
                    synchronized (this) {
                        this.notify();
                    }
                }
            }
        };
        synchronized (fil) {
            addFECIOListener(fil);
            if (!isDecoded()) {
                fil.wait();
            }
            removeFECIOListener(fil);
        }
    }

    public class Decoder implements Runnable, FECIOListener {

        LinkedList queue = new LinkedList();

        boolean done = false;

        public void notify(FECIOEvent ev) {
            if (ev instanceof PacketWrittenEvent) {
                synchronized (queue) {
                    queue.add(ev);
                    queue.notify();
                }
            }
        }

        public void close() {
            synchronized (queue) {
                if (done) {
                    return;
                }
                removeFECIOListener(this);
                done = true;
                queue.clear();
                queue.notify();
            }
        }

        /**
         * Generates the next combo for decoding.
         * @param combo (in/out) Modified to provide the next combo.
         * @param reserved The number of indexes guarenteed to be in every
         * combo.  These are the last indexes in the combo array.
         * @param n The number of elements in the set to choose the combo from.
         *
         * @return false if there are no more combos to generate.
         */
        private final boolean nextCombo(int[] combo, int n) {
            for (int i = combo.length - 1; i >= 0; i--) {
                if (combo[i] != (n - combo.length) + i) {
                    combo[i]++;
                    for (int j = i + 1; j < combo.length; j++) {
                        combo[j] = combo[j - 1] + 1;
                    }
                    return true;
                }
            }
            return false;
        }

        public void run() {
            PacketWrittenEvent ev = null;
            while (true) {
                synchronized (queue) {
                    if (done) {
                        return;
                    } else if (queue.isEmpty()) {
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                        continue;
                    } else {
                        ev = (PacketWrittenEvent) queue.removeFirst();
                    }
                }
                int blockNum = ev.getBlockNum();
                int blockPacketCount = ev.getBlockPacketCount();
                int paddingCount = k - params.getUnexpandedPacketCount(blockNum);
                if (blockPacketCount < k - paddingCount) {
                    continue;
                } else if (pp.isBlockDecoded(blockNum)) {
                    continue;
                }
                int[] possibleStripes = pp.getStripeNums(blockNum, blockPacketCount);
                int[] stripeNums = new int[k];
                stripeNums[k - paddingCount - 1] = possibleStripes[possibleStripes.length - 1];
                for (int i = k - paddingCount; i < k; i++) {
                    stripeNums[i] = i;
                }
                int[] combo = new int[k - paddingCount - 1];
                for (int i = 0; i < combo.length; i++) {
                    combo[i] = i;
                }
                do {
                    for (int i = 0; i < combo.length; i++) {
                        stripeNums[i] = possibleStripes[combo[i]];
                    }
                    StringBuffer sb = new StringBuffer("stripeNums=" + stripeNums[0]);
                    for (int i = 1; i < stripeNums.length; i++) {
                        sb.append("," + stripeNums[i]);
                    }
                    log.debug(sb);
                    try {
                        if (tryDecode(blockNum, stripeNums)) {
                            break;
                        }
                    } catch (IOException e) {
                        getDecodeExceptionHandler().handleException(new ExceptionEvent(FECFile.this, e));
                    }
                } while (nextCombo(combo, blockPacketCount - 1));
                try {
                    if (pp.getDecodedBlockCount() == params.getBlockCount()) {
                        log.debug("File Decoded, switching to read-only");
                        acquireAllWriteLocks();
                        try {
                            raf.setLength(params.getFileSize());
                            raf.setReadOnly();
                        } finally {
                            releaseAllWriteLocks();
                        }
                        fire(new FileDecodedEvent(FECFile.this));
                        close();
                        return;
                    }
                } catch (Exception e) {
                    getDecodeExceptionHandler().handleException(new ExceptionEvent(FECFile.this, e));
                }
            }
        }
    }
}
