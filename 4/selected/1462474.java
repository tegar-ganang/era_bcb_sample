package com.lts.ipc.smfifo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.lts.io.IOUtilities;
import com.lts.ipc.IPCException;
import com.lts.ipc.semaphore.Semaphore;
import com.lts.ipc.sharedmemory.SharedMemory;

/**
 * An interprocess data stream that uses shared memory as its transport.
 * <H2>Quickstart</H2>
 * <H3>Writer process</H3>
 * <CODE>
 * <PRE>
 * SharedMemoryFIFO fifo = new SharedMemoryFIFO("/foo/bar/channel1");
 * OutputStream ostream = fifo.getOutputStream();
 * ostream.write(<I>some data</I>);
 * ...
 * ostream.close();
 * </PRE>
 * </CODE>
 * 
 * <H3>Reader process</H3>
 * <CODE>
 * <PRE>
 * SharedMemoryFIFO fifo = new SharedMemoryFIFO("/foo/bar/channel1");
 * InputStream istream = fifo.getInputStream();
 * istream.read(<I>buffer</I>);
 * ...
 * istream.close();
 * </PRE>
 * </CODE>
 * 
 * <H2>Description</H2>
 * <P>
 * This class provides a high-speed FIFO.  FIFOs are nice because they take care 
 * of synchronization aspects of IPC, whereas shared memory is nice because it 
 * has very fast bandwidth.  The purpose of this class is to give the client the 
 * best of both worlds: shared memory bandwidth with the convenience of a FIFO.
 * </P>
 * @author cnh
 *
 */
public class SMFIFO {

    public static final int DEFAULT_NUMBER_OF_BUFFERS = 4;

    public static final int DEFAULT_BUFFER_SIZE = 4096;

    public static final int HEADER_SIZE = 256;

    public static final int OFFSET_CONTROL_BLOCK = 0;

    public static final int OFFSET_STATE = ControlBlock.SIZE;

    public static final int OFFSET_BUFFER_STATUS = OFFSET_STATE + 4;

    public static final int OFFSET_DATA = HEADER_SIZE;

    public static final int SIZE = OFFSET_DATA + (DEFAULT_BUFFER_SIZE * DEFAULT_NUMBER_OF_BUFFERS);

    public static enum Directions {

        Reader, Writer
    }

    private SharedMemory mySegment;

    private Directions myDirection;

    private Semaphore mySemaphore;

    private int myAddress;

    private byte[] myHeaderBuffer = new byte[HEADER_SIZE];

    private StateBlock myStateBlock;

    private ControlBlock myControlBlock;

    private BufferStatusBlock[] myBufferStatusBlocks;

    public StateBlock getStateBlock() {
        return myStateBlock;
    }

    public SMFIFO() {
    }

    /**
	 * Create and/or connect to a SMFIFO.
	 * 
	 * <P>
	 * This method specifies a semaphore, shared memory block, and an offset
	 * within that block where the SMFIFO should live.
	 * </P>
	 * 
	 * @param semaphore
	 *            The file representing the semaphore to use to synchronize
	 *            access to certain parts of the SMFIFO.
	 * 
	 * @param segment
	 *            The file representing the shared memory to use with the
	 *            SMFIFO.
	 * 
	 * @param address
	 *            The offset within the shared memory where the SMFIFO should
	 *            live.
	 * 
	 * @throws IPCException
	 *             This is thrown if there is a problem creating, initializing
	 *             or connecting to the SMFIFO.
	 */
    public SMFIFO(File semaphore, File segment, int segmentSize, int address) throws IPCException {
        initialize(semaphore, segment, segmentSize, address);
    }

    public SMFIFO(File semaphore, File segment) throws IPCException {
        initialize(semaphore, segment, SIZE, 0);
    }

    protected void initialize(File semaphore, File segment, int segmentSize, int address) throws IPCException {
        mySemaphore = new Semaphore(semaphore);
        myControlBlock = new ControlBlock(address);
        myStateBlock = new StateBlock(address + OFFSET_STATE);
        myBufferStatusBlocks = new BufferStatusBlock[DEFAULT_NUMBER_OF_BUFFERS];
        for (int i = 0; i < DEFAULT_NUMBER_OF_BUFFERS; i++) {
            int bstatusAddr = address + OFFSET_BUFFER_STATUS;
            bstatusAddr = bstatusAddr + (i * BufferStatusBlock.SIZE);
            myBufferStatusBlocks[i] = new BufferStatusBlock(bstatusAddr, i);
        }
        createOrConnectToSegment(segment, segmentSize);
    }

    private void createOrConnectToSegment(File segment, int segmentSize) throws IPCException {
        if (!segment.exists()) {
            createSegmentFile(segment);
        }
        mySegment = new SharedMemory(segment, segmentSize);
    }

    private void createSegmentFile(File segment) throws IPCException {
        try {
            File temp = createTempFile(segment.getParentFile());
            if (!temp.renameTo(segment)) {
                String msg = "Could not rename temp file, " + temp + " to SMFIFO segment file " + segment;
                throw new IPCException(msg);
            }
        } catch (IOException e) {
            String msg = "Error creating temporary SMFIFO segment file, " + segment;
            throw new IPCException(msg, e);
        }
    }

    private BufferStatusBlock[] createBufferStatusBlocks() {
        int count = myControlBlock.getNumberOfBuffers();
        BufferStatusBlock[] blocks = new BufferStatusBlock[count];
        int addr = OFFSET_BUFFER_STATUS;
        for (int i = 0; i < count; i++) {
            int temp = addr + (i * BufferStatusBlock.SIZE);
            blocks[i] = new BufferStatusBlock(temp, i);
        }
        return blocks;
    }

    private void loadHeader() throws IPCException {
        try {
            mySegment.read(myHeaderBuffer, myAddress);
            ByteArrayInputStream bais = new ByteArrayInputStream(myHeaderBuffer);
            DataInputStream dis = new DataInputStream(bais);
            myControlBlock.readFrom(dis);
            myStateBlock.readFrom(dis);
            int numberOfBuffers = myControlBlock.getNumberOfBuffers();
            for (int i = 0; i < numberOfBuffers; i++) {
                myBufferStatusBlocks[i].readFrom(dis);
            }
        } catch (IOException e) {
            throw new IPCException("Error parsing in SMFIFO header", e);
        }
    }

    private void storeHeader() throws IPCException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(myHeaderBuffer.length);
            DataOutputStream dos = new DataOutputStream(baos);
            myControlBlock.writeTo(dos);
            myStateBlock.writeTo(dos);
            for (BufferStatusBlock bsb : myBufferStatusBlocks) {
                bsb.writeTo(dos);
            }
            dos.close();
            baos.close();
            mySegment.write(baos.toByteArray(), myAddress);
        } catch (IOException e) {
            throw new IPCException("Error writing SMFIFO header", e);
        }
    }

    private File createTempFile(File directory) throws IOException, IPCException {
        File tempFile = File.createTempFile("SMFIFO", "tmp", directory);
        FileOutputStream fos = null;
        DataOutputStream dos = null;
        try {
            myControlBlock.initializeBlock();
            myStateBlock.setState(SMFIFOStates.Start);
            for (BufferStatusBlock bsb : myBufferStatusBlocks) {
                bsb.initializeBlock();
            }
            fos = new FileOutputStream(tempFile);
            dos = new DataOutputStream(fos);
            writeInitialHeader(dos);
            writeInitialData(dos);
        } catch (IOException e) {
            String msg = "Error trying to create temporary SMFIFO file " + tempFile;
            throw new IOException(msg, e);
        } finally {
            IOUtilities.close(dos);
            IOUtilities.close(fos);
        }
        return tempFile;
    }

    private void writeInitialData(DataOutputStream dos) throws IOException {
        for (int i = 0; i < DEFAULT_NUMBER_OF_BUFFERS; i++) {
            for (int j = 0; j < DEFAULT_BUFFER_SIZE; j++) {
                dos.writeByte(0);
            }
        }
    }

    private void writeInitialHeader(DataOutputStream dos) throws IPCException, IOException {
        ControlBlock controlBlock = new ControlBlock();
        controlBlock.initializeBlock();
        controlBlock.writeTo(dos);
        StateBlock stateBlock = new StateBlock(myAddress + OFFSET_STATE);
        stateBlock.setState(SMFIFOStates.Start);
        stateBlock.writeTo(dos);
        BufferStatusBlock bsb = new BufferStatusBlock();
        bsb.initializeBlock();
        for (int i = 0; i < controlBlock.getNumberOfBuffers(); i++) {
            bsb.writeTo(dos);
        }
        int numBuffs = controlBlock.getNumberOfBuffers();
        int bufSize = controlBlock.getBufferSize();
        int padBytes = OFFSET_DATA - calculateHeaderSize(numBuffs, bufSize);
        for (int i = 0; i < padBytes; i++) {
            dos.writeByte(0);
        }
    }

    private int calculateHeaderSize(int numBuffs, int bufSize) {
        int size = ControlBlock.SIZE;
        size += StateBlock.SIZE;
        size += (numBuffs * BufferStatusBlock.SIZE);
        return size;
    }

    private int calculateBufferAddress(int index, int bufferSize) {
        return OFFSET_DATA + (index * bufferSize);
    }

    public InputStream getInputStream() throws IPCException {
        if (null != myDirection && myDirection != Directions.Reader) {
            String msg = "Attempt to change direction from writer to reader";
            throw new IPCException(msg);
        }
        if (null == mySegment) {
            throw new IPCException("Not connected to a segment");
        }
        reserveReader();
        SMFIFOInputStream istream = new SMFIFOInputStream(this);
        return istream;
    }

    public OutputStream getOutputStream() throws IPCException {
        if (null != myDirection && myDirection != Directions.Writer) {
            String msg = "Attempt to change direction from reader to writer";
            throw new IPCException(msg);
        }
        if (null == mySegment) {
            throw new IPCException("Not connected to a segment");
        }
        reserveWriter();
        return new SMFIFOOutputStream(this, createSBSB());
    }

    private StateBufferStatusBlock createSBSB() {
        StateBlock stateBlock = new StateBlock(myAddress + OFFSET_STATE);
        int address = myAddress + OFFSET_BUFFER_STATUS;
        int bnum = myControlBlock.getNumberOfBuffers();
        BufferStatusBlock[] statusBlocks = new BufferStatusBlock[bnum];
        for (int i = 0; i < bnum; i++) {
            int bsbAddr = address + (i * BufferStatusBlock.SIZE);
            statusBlocks[i] = new BufferStatusBlock(bsbAddr, i);
        }
        StateBufferStatusBlock sbsb = new StateBufferStatusBlock(myAddress + OFFSET_STATE, stateBlock, statusBlocks);
        return sbsb;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (null == mySegment) {
            return super.toString();
        }
        sb.append(mySegment.getSegmentFile());
        return sb.toString();
    }

    public int getBufferSize() {
        return myControlBlock.getBufferSize();
    }

    public int getNumberOfBuffers() {
        return myControlBlock.getNumberOfBuffers();
    }

    /**
	 * Tries to reserve the reader role in an SMFIFO, simply returning if it 
	 * is successful and throwing an exception if not.
	 * 
	 * @throws IPCException If the reader role is not available.
	 */
    public void reserveReader() throws IPCException {
        try {
            mySemaphore.reserve();
            loadHeader();
            if (myControlBlock.getReader() != -1) {
                throw new IPCException("Another process is reading from the SMFIFO");
            }
            myControlBlock.setReader(1);
            switch(myStateBlock.getState()) {
                case Connecting:
                    myStateBlock.setState(SMFIFOStates.Established);
                    break;
                case Start:
                    myStateBlock.setState(SMFIFOStates.Connecting);
                    break;
                default:
                    {
                        String msg = "SMFIFO in invalid state to allow for connection: " + myStateBlock.getState();
                        throw new IPCException(msg);
                    }
            }
            storeHeader();
        } finally {
            mySemaphore.releaseAll();
        }
    }

    /**
	 * Tries to reserve the role of writer in an SMFIFO.
	 * 
	 * <P>
	 * The method simply returns if successful, otherwise it throws an {@link IPCException}.
	 * </P>
	 */
    public void reserveWriter() throws IPCException {
        try {
            mySemaphore.reserve();
            loadHeader();
            if (myControlBlock.getWriter() != -1) {
                throw new IPCException("Another process is writing tothe SMFIFO");
            }
            myControlBlock.setWriter(1);
            switch(myStateBlock.getState()) {
                case Connecting:
                    myStateBlock.setState(SMFIFOStates.Established);
                    break;
                case Start:
                    myStateBlock.setState(SMFIFOStates.Connecting);
                    break;
                default:
                    {
                        String msg = "SMFIFO in invalid state to allow for connection: " + myStateBlock.getState();
                        throw new IPCException(msg);
                    }
            }
            storeHeader();
        } finally {
            mySemaphore.releaseAll();
        }
    }

    public StateBufferStatusBlock getStateBufferStatusBlock() {
        StateBlock stateBlock = new StateBlock(myAddress + OFFSET_STATE);
        BufferStatusBlock[] statusBlocks = createBufferStatusBlocks();
        StateBufferStatusBlock sbsb = new StateBufferStatusBlock(myAddress + OFFSET_STATE, stateBlock, statusBlocks);
        return sbsb;
    }

    public void loadBuffer(int bufferIndex, byte[] buffer) throws IPCException {
        int address = calculateBufferAddress(bufferIndex, myControlBlock.getBufferSize());
        int bytesRead = mySegment.read(buffer, address);
        if (bytesRead != buffer.length) {
            throw new IPCException("bytes read != buffer length");
        }
    }

    public int writeToDataBuffer(int index, byte[] buffer, int offset, int length) throws IPCException {
        int address = calculateBufferAddress(index, getBufferSize());
        int count = (length > getBufferSize()) ? length - getBufferSize() : length;
        mySegment.write(buffer, offset, count, address);
        return count;
    }

    public int advanceBufferIndex(int index) {
        index++;
        index = index % myControlBlock.getNumberOfBuffers();
        return index;
    }

    public SharedMemory getSegment() {
        return mySegment;
    }
}
