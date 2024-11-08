package org.neodatis.odb.core.transaction;

import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.OdbConfiguration;
import org.neodatis.odb.core.NeoDatisError;
import org.neodatis.odb.core.layers.layer2.meta.ODBType;
import org.neodatis.odb.core.layers.layer3.ByteArrayConverter;
import org.neodatis.odb.core.layers.layer4.engine.IFileSystemInterface;
import org.neodatis.tool.DLogger;
import org.neodatis.tool.DisplayUtility;
import org.neodatis.tool.wrappers.list.IOdbList;
import org.neodatis.tool.wrappers.list.OdbArrayList;

/**The WriteAction class is the description of a Write operation that will be applied to the main database file when committing.
 * 
 * All operations(writes) that can not be written to the database file before committing , pointers (for example) are stored in WriteAction
 * objects. The transaction keeps track of all these WriteActions. When committing, the transaction apply each WriteAction to the engine database file. 
 * 
 * @author osmadja
 *
 */
public class DefaultWriteAction implements IWriteAction {

    public static int count = 0;

    public static final int UNKNOWN_WRITE_ACTION = 0;

    public static final int DATA_WRITE_ACTION = 1;

    public static final int POINTER_WRITE_ACTION = 2;

    public static final int DIRECT_WRITE_ACTION = 3;

    public static final String LOG_ID = "WriteAction";

    private static String UNKNOWN_LABEL = "?";

    private long position;

    private ByteArrayConverter byteArrayConverter;

    private IOdbList<byte[]> listOfBytes;

    private int size;

    public DefaultWriteAction(long position) {
        this(position, null);
    }

    public DefaultWriteAction(long position, byte[] bytes) {
        this(position, bytes, null);
    }

    public DefaultWriteAction(long position, byte[] bytes, String label) {
        this.byteArrayConverter = OdbConfiguration.getCoreProvider().getByteArrayConverter();
        this.position = position;
        listOfBytes = new OdbArrayList<byte[]>(20);
        if (bytes != null) {
            listOfBytes.add(bytes);
            this.size = bytes.length;
        }
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public byte[] getBytes(int index) {
        return listOfBytes.get(index);
    }

    public void addBytes(byte[] bytes) {
        listOfBytes.add(bytes);
        size += bytes.length;
    }

    public void persist(IFileSystemInterface fsi, int index) {
        long currentPosition = fsi.getPosition();
        if (OdbConfiguration.isDebugEnabled(LOG_ID)) {
        }
        int sizeOfLong = ODBType.LONG.getSize();
        int sizeOfInt = ODBType.INTEGER.getSize();
        byte[] bytes = new byte[sizeOfLong + sizeOfInt + size];
        byteArrayConverter.longToByteArray(position, bytes, 0);
        byteArrayConverter.intToByteArray(size, bytes, 8);
        int offset = 8 + 4;
        for (int i = 0; i < listOfBytes.size(); i++) {
            byte[] tmp = listOfBytes.get(i);
            System.arraycopy(tmp, 0, bytes, offset, tmp.length);
            offset += tmp.length;
        }
        fsi.writeBytes(bytes, false, "Transaction");
        int fixedSize = sizeOfLong + sizeOfInt;
        long positionAfterWrite = fsi.getPosition();
        long writeSize = positionAfterWrite - currentPosition;
        if (writeSize != size + fixedSize) {
            throw new ODBRuntimeException(NeoDatisError.DIFFERENT_SIZE_IN_WRITE_ACTION.addParameter(size).addParameter(writeSize));
        }
    }

    public static DefaultWriteAction read(IFileSystemInterface fsi, int index) {
        try {
            long position = fsi.readLong();
            int size = fsi.readInt();
            byte[] bytes = fsi.readBytes(size);
            DefaultWriteAction writeAction = new DefaultWriteAction(position, bytes);
            if (OdbConfiguration.isDebugEnabled(LOG_ID)) {
                DLogger.debug("Loading Write Action # " + index + " at " + fsi.getPosition() + " => " + writeAction.toString());
            }
            return writeAction;
        } catch (ODBRuntimeException e) {
            DLogger.error("error reading write action " + index + " at position " + fsi.getPosition());
            throw e;
        }
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("position=").append(position);
        StringBuffer bytes = new StringBuffer();
        if (listOfBytes != null) {
            for (int i = 0; i < listOfBytes.size(); i++) {
                bytes.append(DisplayUtility.byteArrayToString(getBytes(i)));
            }
            buffer.append(" | bytes=[").append(bytes).append("] & size=" + size);
        } else {
            buffer.append(" | bytes=null & size=").append(size);
        }
        return buffer.toString();
    }

    public void applyTo(IFileSystemInterface fsi, int index) {
        if (OdbConfiguration.isDebugEnabled(LOG_ID)) {
            DLogger.debug("Applying WriteAction #" + index + " : " + toString());
        }
        fsi.setWritePosition(position, false);
        for (int i = 0; i < listOfBytes.size(); i++) {
            fsi.writeBytes(getBytes(i), false, "WriteAction");
        }
    }

    public boolean isEmpty() {
        return listOfBytes == null || listOfBytes.isEmpty();
    }

    public void clear() {
        listOfBytes.clear();
        listOfBytes = null;
        DefaultWriteAction.count--;
    }
}
