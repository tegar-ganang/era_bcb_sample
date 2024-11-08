package org.gdbms.engine.data.indexes.hashMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * DOCUMENT ME!
 *
 * @author Fernando Gonz�lez Cort�s
 */
public class DiskIndex implements Index {

    static int RECORD_SIZE = 8;

    private int socketCount = 2;

    private int recordCount;

    private int positionCount;

    private File file;

    private RandomAccessFile raf;

    private FileChannel channel;

    private ByteBuffer buffer;

    /**
	 * Crea un nuevo DiskIndex.
	 *
	 * @param f DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public DiskIndex(File f) throws IOException {
        initIndex(f);
    }

    /**
	 * Crea un nuevo DiskIndex.
	 *
	 * @param socketCount DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    public DiskIndex(int socketCount) throws IOException {
        File f = File.createTempFile("gdbms", ".gix");
        f.deleteOnExit();
        this.socketCount = socketCount;
        initIndex(f);
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param f DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 */
    private void initIndex(File f) throws IOException {
        file = f;
        if (file.length() == 0) {
            FileOutputStream fos = new FileOutputStream(file);
            FileChannel channel = fos.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
            buffer.putInt(socketCount);
            buffer.putInt(socketCount);
            buffer.flip();
            channel.write(buffer, 0);
            for (int i = 0; i < socketCount; i++) {
                buffer.clear();
                buffer.putInt(-1);
                buffer.putInt(-1);
                buffer.flip();
                channel.write(buffer, (long) (byteNumber(i)));
            }
            channel.force(true);
            buffer = null;
            channel.close();
            fos.close();
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @throws IndexException
	 *
	 * @see org.gdbms.engine.data.indexes.hashMap.Index#start()
	 */
    public void start() throws IndexException {
        try {
            raf = new RandomAccessFile(file, "rws");
            channel = raf.getChannel();
            buffer = ByteBuffer.allocate(RECORD_SIZE);
            channel.position(0);
            buffer.clear();
            channel.read(buffer);
            buffer.flip();
            positionCount = buffer.getInt();
            recordCount = buffer.getInt();
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @throws IndexException
	 *
	 * @see org.gdbms.engine.data.indexes.hashMap.Index#stop()
	 */
    public void stop() throws IndexException {
        buffer = null;
        try {
            channel.close();
            raf.close();
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param recordIndex DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    static long byteNumber(int recordIndex) {
        return (recordIndex + 1) * RECORD_SIZE;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param v DOCUMENT ME!
	 * @param position DOCUMENT ME!
	 *
	 * @throws IndexException
	 *
	 * @see org.gdbms.engine.data.indexes.hashMap.Index#add(com.hardcode.gdbms.engine.values.Value,
	 * 		int)
	 */
    public void add(Object v, int position) throws IndexException {
        try {
            int pos = Math.abs(v.hashCode());
            pos = (pos % positionCount);
            buffer.clear();
            channel.position(byteNumber(pos));
            channel.read(buffer);
            buffer.flip();
            int value = buffer.getInt();
            int next = buffer.getInt();
            if (value == -1) {
                buffer.clear();
                buffer.putInt(position);
                buffer.putInt(-1);
                buffer.flip();
                channel.position(byteNumber(pos));
                channel.write(buffer);
                channel.force(true);
                return;
            } else {
                while (next != -1) {
                    pos = next;
                    buffer.clear();
                    channel.position(byteNumber(pos));
                    channel.read(buffer);
                    buffer.flip();
                    value = buffer.getInt();
                    next = buffer.getInt();
                }
                channel.position(byteNumber(pos));
                buffer.clear();
                buffer.putInt(value);
                buffer.putInt(recordCount);
                buffer.flip();
                channel.write(buffer);
                channel.position(byteNumber(recordCount));
                buffer.clear();
                buffer.putInt(position);
                buffer.putInt(-1);
                buffer.flip();
                channel.write(buffer);
                channel.force(true);
                recordCount++;
            }
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param v DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 *
	 * @throws IndexException
	 *
	 * @see org.gdbms.engine.data.indexes.hashMap.Index#getPositions(com.hardcode.gdbms.engine.values.Value)
	 */
    public PositionIterator getPositions(Object v) throws IndexException {
        try {
            return new DiskPositionIterator(channel, positionCount, v);
        } catch (IOException e) {
            throw new IndexException(e);
        }
    }
}
