package com.lts.ipc.namedpipe;

import com.lts.ipc.IPCException;
import com.lts.ipc.IPCPackage;

/**
 * An internal class that provides the underlying system calls needed to implement 
 * named pipes.
 * <P>
 * This class is implementation dependent and not intended for general use.
 * </P>
 * 
 * @author cnh
 */
public class NamedPipeImpl {

    public static final int DIRECTION_READER = 0;

    public static final int DIRECTION_WRITER = 1;

    /**
	 * The role a client is taking (reader/writer) when using a named pipe. <H2>NOTE</H2>
	 * This class really belongs in {@link NamedPipe} and will probably be moved there in
	 * the near future.
	 * 
	 * @author cnh
	 */
    public enum PipeDirection {

        Reader(DIRECTION_READER), Writer(DIRECTION_WRITER);

        public int jniValue;

        private PipeDirection(int direction) {
            jniValue = direction;
        }
    }

    private native void createImpl(NamedPipeResult result);

    public NamedPipeResult create() {
        IPCPackage.ensureInitialized();
        setCreator(true);
        NamedPipeResult result = new NamedPipeResult();
        createImpl(result);
        if (result.resultCode == NamedPipeResult.SUCCESS) {
            setHandle(result.handle);
        }
        return result;
    }

    private native void writeImpl(NamedPipeResult result, byte[] buffer, int offset, int length);

    public int write(byte[] buffer, int offset, int length) throws IPCException {
        if (getDirection() != PipeDirection.Writer) {
            throw new IPCException("Attempt to write to a read-only pipe.");
        }
        NamedPipeResult result = new NamedPipeResult();
        writeImpl(result, buffer, offset, length);
        if (result.resultCode != NamedPipeResult.SUCCESS) {
            String msg = "Error writing named pipe, error code = " + result.errorCode;
            throw new IPCException(msg);
        }
        return result.byteCount;
    }

    private native void readImpl(NamedPipeResult result, byte[] buffer, int offset, int length);

    public int read(byte[] buffer, int offset, int length) throws IPCException {
        if (PipeDirection.Reader != getDirection()) {
            throw new IPCException("Attempt to read a write-only pipe.");
        }
        NamedPipeResult result = new NamedPipeResult();
        readImpl(result, buffer, offset, length);
        if (result.resultCode != NamedPipeResult.SUCCESS) {
            String msg = "Error reading named pipe, code = " + result.errorCode;
            throw new IPCException(msg);
        }
        return result.byteCount;
    }

    private static native boolean virtualNameContainsActualNameImpl();

    public static boolean virtualNameContainsActualName() {
        IPCPackage.ensureInitialized();
        return virtualNameContainsActualNameImpl();
    }

    public static native String createPipeNameImpl(String s);

    public static String createPipeName(String s) {
        return createPipeNameImpl(s);
    }

    private native void openImpl(NamedPipeResult result, int direction);

    public NamedPipeResult open(PipeDirection direction) {
        setDirection(direction);
        NamedPipeResult result = new NamedPipeResult();
        openImpl(result, direction.jniValue);
        setHandle(result.handle);
        return result;
    }

    private String myActualName;

    private boolean myCreator;

    private PipeDirection myDirection;

    private long myHandle;

    private int myBufferSize;

    protected int getBufferSize() {
        return myBufferSize;
    }

    protected void setBufferSize(int bufferSize) {
        myBufferSize = bufferSize;
    }

    protected String getActualName() {
        return myActualName;
    }

    protected PipeDirection getDirection() {
        return myDirection;
    }

    protected long getHandle() {
        return myHandle;
    }

    protected void setActualName(String actualName) {
        myActualName = actualName;
    }

    public void setCreator(boolean creator) {
        myCreator = creator;
    }

    protected void setDirection(PipeDirection direction) {
        myDirection = direction;
    }

    protected void setHandle(long handle) {
        myHandle = handle;
    }

    protected boolean isCreator() {
        return myCreator;
    }
}
