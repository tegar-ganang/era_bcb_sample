package org.gamio.buffer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import org.gamio.conf.BufferPoolProps;
import org.gamio.exception.GamioException;
import org.gamio.util.Helper;
import org.gamio.util.SimplePool;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 20 $ $Date: 2008-10-01 19:37:36 -0400 (Wed, 01 Oct 2008) $
 */
public class BufferFactoryImpl implements BufferFactory {

    private static final ReentrantLock lock = new ReentrantLock();

    private static volatile BufferFactoryImpl bufferFactory = null;

    private static BufferPool bufferPool = null;

    private static final class BufferImpl implements Buffer {

        private static final int DUMP_BYTESINROW = 16;

        private static int unitSize = 0;

        private static BytesPool pool = null;

        private int position = 0;

        private int mark = 0;

        private int size = 0;

        private final ArrayList<byte[]> data = new ArrayList<byte[]>();

        private static final class BytesPool extends SimplePool<byte[]> {

            public BytesPool(int capacity) {
                super(capacity);
            }

            protected byte[] newInstance() {
                return new byte[unitSize];
            }
        }

        public static void initialize(int capacity, int unitSize) {
            BufferImpl.unitSize = unitSize;
            pool = new BytesPool(capacity);
        }

        private static void put(byte[] b) {
            pool.release(b);
        }

        private static byte[] take() {
            return pool.acquire();
        }

        public byte getByte(int index) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(errMsg(index, 1, size));
            return byteAt(index);
        }

        public int getIntB(int start) {
            if (start < 0 || start + 4 > size) throw new IndexOutOfBoundsException(errMsg(start, 4, size));
            int a = byteAt(start++);
            int b = byteAt(start++) & 0xFF;
            int c = byteAt(start++) & 0xFF;
            int d = byteAt(start) & 0xFF;
            return ((a << 24) | (b << 16) | (c << 8) | d);
        }

        public int getIntL(int start) {
            if (start < 0 || start + 4 > size) throw new IndexOutOfBoundsException(errMsg(start, 4, size));
            int a = byteAt(start++) & 0xFF;
            int b = byteAt(start++) & 0xFF;
            int c = byteAt(start++) & 0xFF;
            int d = byteAt(start);
            return ((d << 24) | (c << 16) | (b << 8) | a);
        }

        public long getLongB(int start) {
            if (start < 0 || start + 8 > size) throw new IndexOutOfBoundsException(errMsg(start, 8, size));
            int length = 8;
            long v = 0;
            while (length-- > 0) v = (v << 8) | (byteAt(start++) & 0xFF);
            return v;
        }

        public long getLongL(int start) {
            if (start < 0 || start + 8 > size) throw new IndexOutOfBoundsException(errMsg(start, 8, size));
            int length = 8;
            long v = 0;
            start += 8;
            while (length-- > 0) v = (v << 8) | (byteAt(--start) & 0xFF);
            return v;
        }

        public short getShortB(int start) {
            if (start < 0 || start + 2 > size) throw new IndexOutOfBoundsException(errMsg(start, 2, size));
            int a = byteAt(start++);
            int b = byteAt(start) & 0xFF;
            return (short) ((a << 8) | b);
        }

        public short getShortL(int start) {
            if (start < 0 || start + 2 > size) throw new IndexOutOfBoundsException(errMsg(start, 2, size));
            int a = byteAt(start++) & 0xFF;
            int b = byteAt(start);
            return (short) ((b << 8) | a);
        }

        public byte[] getBytes(int start) {
            return getBytes(start, size - start);
        }

        public byte[] getBytes(int start, int length) {
            if (start < 0 || length < 0 || start + length > size) throw new IndexOutOfBoundsException(errMsg(start, length, size));
            byte[] dst = new byte[length];
            if (length > 0) {
                int x = start / unitSize;
                int y = start % unitSize;
                start = 0;
                do {
                    byte[] src = data.get(x);
                    int len = src.length - y;
                    if (len > length) len = length;
                    System.arraycopy(src, y, dst, start, len);
                    length -= len;
                    start += len;
                    x++;
                    y = 0;
                } while (length > 0);
            }
            return dst;
        }

        public String getString(int start) {
            return new String(getBytes(start));
        }

        public String getString(int start, String charsetName) throws UnsupportedEncodingException {
            return new String(getBytes(start), charsetName);
        }

        public String getString(int start, int length) {
            return new String(getBytes(start, length));
        }

        public String getString(int start, int length, String charsetName) throws UnsupportedEncodingException {
            return new String(getBytes(start, length), charsetName);
        }

        public int getUByte(int index) {
            return (getByte(index) & 0xFF);
        }

        public int getUShortB(int start) {
            return (getShortB(start) & 0xFFFF);
        }

        public int getUShortL(int start) {
            return (getShortL(start) & 0xFFFF);
        }

        public int length() {
            return size;
        }

        public void clear() {
            for (byte[] b : data) put(b);
            data.clear();
            mark = 0;
            position = 0;
            size = 0;
        }

        public void close() {
            clear();
            bufferPool.release(this);
        }

        public void trimToSize(int newSize) {
            if (newSize < 0 || newSize >= size) return;
            if (newSize == 0) {
                close();
                return;
            }
            size = newSize--;
            while (data.size() > (newSize / unitSize + 1)) put(data.remove(data.size() - 1));
        }

        public void mark() {
            mark = position;
        }

        public int position() {
            return position;
        }

        public byte readByte() {
            if (position >= size) throw new GamioException(errMsg(position, 1, size));
            return byteAt(position++);
        }

        public byte[] readBytes() {
            return readBytes(remaining());
        }

        public byte[] readBytes(int length) {
            if (length < 0) throw new IllegalArgumentException(errMsg(position, length, size));
            if (position + length > size) throw new GamioException(errMsg(position, length, size));
            byte[] dst = new byte[length];
            if (length > 0) {
                int x = position / unitSize;
                int y = position % unitSize;
                int i = 0;
                do {
                    byte[] src = data.get(x);
                    int len = src.length - y;
                    if (len > length) len = length;
                    System.arraycopy(src, y, dst, i, len);
                    length -= len;
                    i += len;
                    x++;
                    y = 0;
                } while (length > 0);
                position += dst.length;
            }
            return dst;
        }

        public int readIntB() {
            if (position + 4 > size) throw new GamioException(errMsg(position, 4, size));
            int a = byteAt(position++);
            int b = byteAt(position++) & 0xFF;
            int c = byteAt(position++) & 0xFF;
            int d = byteAt(position++) & 0xFF;
            return ((a << 24) | (b << 16) | (c << 8) | d);
        }

        public int readIntL() {
            if (position + 4 > size) throw new GamioException(errMsg(position, 4, size));
            int a = byteAt(position++) & 0xFF;
            int b = byteAt(position++) & 0xFF;
            int c = byteAt(position++) & 0xFF;
            int d = byteAt(position++);
            return ((d << 24) | (c << 16) | (b << 8) | a);
        }

        public long readLongB() {
            if (position + 8 > size) throw new GamioException(errMsg(position, 8, size));
            int length = 8;
            long v = 0;
            while (length-- > 0) v = (v << 8) | (byteAt(position++) & 0xFF);
            return v;
        }

        public long readLongL() {
            if (position + 8 > size) throw new GamioException(errMsg(position, 8, size));
            int length = 8;
            position += 8;
            int i = position;
            long v = 0;
            while (length-- > 0) v = (v << 8) | (byteAt(--i) & 0xFF);
            return v;
        }

        public short readShortB() {
            if (position + 2 > size) throw new GamioException(errMsg(position, 2, size));
            int a = byteAt(position++);
            int b = byteAt(position++) & 0xFF;
            return (short) ((a << 8) | b);
        }

        public short readShortL() {
            if (position + 2 > size) throw new GamioException(errMsg(position, 2, size));
            int a = byteAt(position++) & 0xFF;
            int b = byteAt(position++);
            return (short) ((b << 8) | a);
        }

        public String readString() {
            return new String(readBytes());
        }

        public String readString(String charsetName) throws UnsupportedEncodingException {
            return new String(readBytes(), charsetName);
        }

        public String readString(int length) {
            return new String(readBytes(length));
        }

        public String readString(int length, String charsetName) throws UnsupportedEncodingException {
            return new String(readBytes(length), charsetName);
        }

        public int readUByte() {
            return (readByte() & 0xFF);
        }

        public int readUShortB() {
            return (readShortB() & 0xFFFF);
        }

        public int readUShortL() {
            return (readShortL() & 0xFFFF);
        }

        public int remaining() {
            return size - position;
        }

        public void reset() {
            position = mark;
        }

        public void rewind() {
            position = mark = 0;
        }

        public int size() {
            return size;
        }

        public void skip(int n) {
            if (n < 0) throw new IllegalArgumentException(errMsg(position, n, size));
            if (position + n > size) throw new GamioException(errMsg(position, n, size));
            position += n;
        }

        public BufferWriter setByte(int index, byte value) {
            if (index < 0 || index >= size) throw new IndexOutOfBoundsException(errMsg(index, 1, size));
            setAt(index, value);
            return this;
        }

        public BufferWriter setBytes(int start, byte[] src) {
            return setBytes(start, src, 0, src.length);
        }

        public BufferWriter setBytes(int start, byte[] src, int offset, int length) {
            if (offset < 0 || length < 0 || offset + length > src.length) throw new IndexOutOfBoundsException();
            if (start < 0 || start + length > size) throw new IndexOutOfBoundsException(errMsg(start, length, size));
            if (length > 0) {
                int x = start / unitSize;
                start %= unitSize;
                do {
                    byte[] dst = data.get(x);
                    int len = dst.length - start;
                    if (len > length) len = length;
                    System.arraycopy(src, offset, dst, start, len);
                    length -= len;
                    offset += len;
                    x++;
                    start = 0;
                } while (length > 0);
            }
            return this;
        }

        public BufferWriter setIntB(int start, int value) {
            if (start < 0 || start + 4 > size) throw new IndexOutOfBoundsException(errMsg(start, 4, size));
            setAt(start++, (byte) (value >>> 24));
            setAt(start++, (byte) (value >>> 16));
            setAt(start++, (byte) (value >>> 8));
            setAt(start, (byte) value);
            return this;
        }

        public BufferWriter setIntL(int start, int value) {
            if (start < 0 || start + 4 > size) throw new IndexOutOfBoundsException(errMsg(start, 4, size));
            setAt(start++, (byte) value);
            setAt(start++, (byte) (value >>> 8));
            setAt(start++, (byte) (value >>> 16));
            setAt(start, (byte) (value >>> 24));
            return this;
        }

        public BufferWriter setLongB(int start, long value) {
            if (start + 8 > size) throw new IndexOutOfBoundsException(errMsg(start, 8, size));
            start += 8;
            for (int i = 0; i < 8; i++) {
                setAt(--start, (byte) value);
                value >>>= 8;
            }
            return this;
        }

        public BufferWriter setLongL(int start, long value) {
            if (start + 8 > size) throw new IndexOutOfBoundsException(errMsg(start, 8, size));
            for (int i = 0; i < 8; i++) {
                setAt(start++, (byte) value);
                value >>>= 8;
            }
            return this;
        }

        public BufferWriter setShortB(int start, short value) {
            if (start < 0 || start + 2 > size) throw new IndexOutOfBoundsException(errMsg(start, 2, size));
            setAt(start++, (byte) (value >>> 8));
            setAt(start, (byte) value);
            return this;
        }

        public BufferWriter setShortL(int start, short value) {
            if (start < 0 || start + 2 > size) throw new IndexOutOfBoundsException(errMsg(start, 2, size));
            setAt(start++, (byte) value);
            setAt(start, (byte) (value >>> 8));
            return this;
        }

        private BufferWriter write(BufferImpl inBuffer, int length) {
            if (length < 0) throw new IllegalArgumentException("length must be non-negative");
            if (inBuffer.position + length > inBuffer.size) throw new GamioException(errMsg(inBuffer.position, length, inBuffer.size));
            int len = 0;
            while (length > 0) {
                int sX = inBuffer.position / unitSize;
                int sY = inBuffer.position % unitSize;
                byte[] src = inBuffer.data.get(sX);
                int srcLen = src.length - sY;
                int dX = size / unitSize;
                int dY = size % unitSize;
                byte[] dst = null;
                if (dY == 0) {
                    dst = take();
                    data.add(dst);
                } else dst = data.get(dX);
                int dstLen = dst.length - dY;
                len = srcLen > dstLen ? dstLen : srcLen;
                if (len > length) len = length;
                System.arraycopy(src, sY, dst, dY, len);
                inBuffer.position += len;
                size += len;
                length -= len;
            }
            return this;
        }

        public BufferWriter write(BufferReader in) {
            return write(in, in.remaining());
        }

        public BufferWriter write(BufferReader in, int length) {
            try {
                return write((BufferImpl) in, length);
            } catch (ClassCastException e) {
                return writeBytes(in.readBytes(length));
            }
        }

        public BufferWriter writeByte(byte value) {
            int x = size / unitSize;
            int y = size % unitSize;
            byte[] dst = null;
            if (y == 0) {
                dst = take();
                data.add(dst);
            } else dst = data.get(x);
            dst[y++] = value;
            size++;
            return this;
        }

        public BufferWriter writeBytes(byte[] src) {
            return writeBytes(src, 0, src.length);
        }

        public BufferWriter writeBytes(byte[] src, int offset, int length) {
            if (offset < 0 || length < 0 || offset + length > src.length) throw new IndexOutOfBoundsException();
            int x = size / unitSize;
            int y = size % unitSize;
            byte[] dst = null;
            if (y == 0) {
                dst = take();
                data.add(dst);
            } else dst = data.get(x);
            size += length;
            while (length > 0) {
                int len = unitSize - y;
                if (len > length) len = length;
                System.arraycopy(src, offset, dst, y, len);
                offset += len;
                length -= len;
                if (length > 0) {
                    dst = take();
                    data.add(dst);
                    y = 0;
                }
            }
            return this;
        }

        public BufferWriter writeIntB(int value) {
            int len = (size + 3) / unitSize;
            while (data.size() <= len) data.add(take());
            setAt(size++, (byte) (value >>> 24));
            setAt(size++, (byte) (value >>> 16));
            setAt(size++, (byte) (value >>> 8));
            setAt(size++, (byte) value);
            return this;
        }

        public BufferWriter writeIntL(int value) {
            int len = (size + 3) / unitSize;
            while (data.size() <= len) data.add(take());
            setAt(size++, (byte) value);
            setAt(size++, (byte) (value >>> 8));
            setAt(size++, (byte) (value >>> 16));
            setAt(size++, (byte) (value >>> 24));
            return this;
        }

        public BufferWriter writeLongB(long value) {
            int len = (size + 7) / unitSize;
            while (data.size() <= len) data.add(take());
            size += 8;
            len = size;
            for (int i = 0; i < 8; i++) {
                setAt(--len, (byte) value);
                value >>>= 8;
            }
            return this;
        }

        public BufferWriter writeLongL(long value) {
            int len = (size + 7) / unitSize;
            while (data.size() <= len) data.add(take());
            for (int i = 0; i < 8; i++) {
                setAt(size++, (byte) value);
                value >>>= 8;
            }
            return this;
        }

        public BufferWriter writeShortB(short value) {
            int len = (size + 1) / unitSize;
            while (data.size() <= len) data.add(take());
            setAt(size++, (byte) (value >>> 8));
            setAt(size++, (byte) value);
            return this;
        }

        public BufferWriter writeShortL(short value) {
            int len = (size + 1) / unitSize;
            while (data.size() <= len) data.add(take());
            setAt(size++, (byte) value);
            setAt(size++, (byte) (value >>> 8));
            return this;
        }

        public BufferWriter writeString(String s) {
            return writeBytes(s.getBytes());
        }

        public BufferWriter writeString(String s, String charsetName) throws UnsupportedEncodingException {
            return writeBytes(s.getBytes(charsetName));
        }

        public void read(ByteBuffer byteBuffer) {
            int maxLen = byteBuffer.remaining();
            if (maxLen < 1) return;
            int len = remaining();
            if (len < 1) throw new GamioException("No Remaining Bytes");
            if (maxLen > len) maxLen = len;
            int x = position / unitSize;
            int y = position % unitSize;
            position += maxLen;
            len = unitSize - y;
            while (maxLen > 0) {
                if (len > maxLen) len = maxLen;
                byte[] src = data.get(x++);
                byteBuffer.put(src, y, len);
                maxLen -= len;
                y = 0;
                len = unitSize;
            }
        }

        public void write(ByteBuffer byteBuffer) {
            int x = size / unitSize;
            int y = size % unitSize;
            byte[] dst = null;
            if (y == 0) {
                dst = take();
                data.add(dst);
            } else dst = data.get(x);
            size += byteBuffer.remaining();
            while (byteBuffer.remaining() > 0) {
                int len = unitSize - y;
                if (len > byteBuffer.remaining()) len = byteBuffer.remaining();
                byteBuffer.get(dst, y, len);
                if (byteBuffer.remaining() > 0) {
                    dst = take();
                    data.add(dst);
                    y = 0;
                }
            }
        }

        public void readTo(Object bytesMessage, Method write) throws Exception {
            if (position >= size) return;
            int x = position / unitSize;
            int y = position % unitSize;
            for (; x < data.size(); x++) {
                int n = remaining();
                if (n > unitSize) n = unitSize;
                byte[] src = data.get(x);
                write.invoke(bytesMessage, src, y, n);
                y = 0;
            }
            position = size;
        }

        public void writeFrom(Object bytesMessage, Method read, int len) throws Exception {
            if (len <= 0) return;
            int x = size / unitSize;
            int y = size % unitSize;
            int n = len;
            byte[] dst = take();
            try {
                if (y > 0) {
                    n = unitSize - y;
                    read.invoke(bytesMessage, dst, n);
                    System.arraycopy(dst, 0, data.get(x), y, n);
                    if (n >= len) {
                        put(dst);
                        size += len;
                        return;
                    }
                    n = len - n;
                }
                data.add(dst);
                while ((n -= (Integer) read.invoke(bytesMessage, dst, unitSize)) > 0) {
                    dst = take();
                    data.add(dst);
                }
                size += len;
            } catch (Exception e) {
                trimToSize(size);
                throw e;
            }
        }

        public void dump(StringBuilder strBldr) {
            int addr = 0;
            int x = 0;
            int x1 = 0;
            int y = unitSize;
            int y1 = unitSize;
            int length = size;
            byte[] bytes = null;
            while (length > 0) {
                Helper.dumpInt32(strBldr, addr);
                strBldr.append("h:");
                int i = 0;
                for (; i < DUMP_BYTESINROW && length > 0; i++, length--) {
                    strBldr.append(' ');
                    if (y1 == unitSize) {
                        y1 = 0;
                        bytes = data.get(x1++);
                    }
                    Helper.dumpByte(strBldr, bytes[y1++]);
                }
                int len = i;
                while (i++ < DUMP_BYTESINROW) strBldr.append("   ");
                strBldr.append("      ");
                for (i = 0; i < len; i++) {
                    if (y == unitSize) {
                        y = 0;
                        bytes = data.get(x++);
                    }
                    int c = bytes[y++] & 0xFF;
                    strBldr.append(Character.isISOControl(c) ? '.' : (char) c);
                }
                strBldr.append(Helper.getLineSeparator());
                addr += DUMP_BYTESINROW;
            }
        }

        private byte byteAt(int index) {
            int x = index / unitSize;
            int y = index % unitSize;
            return data.get(x)[y];
        }

        private void setAt(int index, byte value) {
            int x = index / unitSize;
            int y = index % unitSize;
            data.get(x)[y] = value;
        }

        private String errMsg(int start, int length, int size) {
            return Helper.buildString("start[", start, "], length[", length, "], size[", size, "]");
        }
    }

    private static final class BufferPool extends SimplePool<Buffer> {

        public BufferPool(int capacity) {
            super(capacity);
        }

        protected Buffer newInstance() {
            return new BufferImpl();
        }
    }

    private BufferFactoryImpl(BufferPoolProps bufferPoolProps) {
        BufferImpl.initialize(bufferPoolProps.getPoolSize(), bufferPoolProps.getUnitSize());
        bufferPool = new BufferPool(bufferPoolProps.getPoolSize() / 2 + 1);
    }

    public static BufferFactory getInstance(BufferPoolProps bufferPoolProps) {
        if (bufferFactory == null) {
            lock.lock();
            try {
                if (bufferFactory == null) bufferFactory = new BufferFactoryImpl(bufferPoolProps);
            } finally {
                lock.unlock();
            }
        }
        return bufferFactory;
    }

    public Buffer create() {
        return bufferPool.acquire();
    }
}
