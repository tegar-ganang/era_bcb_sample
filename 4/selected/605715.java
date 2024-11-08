package org.neodatis.odb.test.ee.layers.layer3;

import org.neodatis.odb.core.layers.layer3.Bytes;
import org.neodatis.odb.core.layers.layer3.Bytes2;
import org.neodatis.odb.core.layers.layer3.BytesFactory;
import org.neodatis.odb.core.layers.layer3.Layer3Converter;
import org.neodatis.odb.test.ODBTest;

/**
 * @author olivier
 * 
 */
public class TestBytes extends ODBTest {

    int SIZE = 1000;

    public void test1() {
        Bytes bytes = BytesFactory.getBytes();
        for (int i = 0; i < 500; i++) {
            bytes.set(i, (byte) i);
        }
        assertEquals(500, bytes.getRealSize());
        assertEquals(1, bytes.getNbBlocks());
    }

    public void test2() {
        int size = SIZE;
        Bytes bytes = BytesFactory.getBytes();
        long start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            bytes.set(i, (byte) i);
        }
        long end1 = System.currentTimeMillis();
        assertEquals(size, bytes.getRealSize());
        assertEquals(size / Layer3Converter.DEFAULT_BYTES_SIZE + 1, bytes.getNbBlocks());
        for (int i = 0; i < size; i++) {
            assertEquals((byte) i, bytes.get(i));
        }
        long end2 = System.currentTimeMillis();
        System.out.println(String.format("Bytes  write=%d  and read=%d", (end1 - start), (end2 - end1)));
    }

    public void testExtract() {
        int size = 10000;
        Bytes bytes = BytesFactory.getBytes();
        for (int i = 0; i < size; i++) {
            bytes.set(i, (byte) i);
        }
        for (int i = 0; i < 10; i++) {
            bytes.set(1020 + i, (byte) i);
        }
        assertEquals(size, bytes.getRealSize());
        byte[] byteArray = bytes.extract(1020, 10);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, byteArray[i]);
        }
    }

    public void testExtract2() {
        int size = 10000;
        Bytes bytes = BytesFactory.getBytes();
        for (int i = 0; i < size; i++) {
            bytes.set(i, (byte) i);
        }
        for (int i = 0; i < 10; i++) {
            bytes.set(1020 + i, (byte) i);
        }
        assertEquals(size, bytes.getRealSize());
        byte[] byteArray = bytes.extract(1020, 4);
        for (int i = 0; i < 4; i++) {
            assertEquals(i, byteArray[i]);
        }
    }

    public void testExtract3() {
        int size = 10000;
        Bytes bytes = BytesFactory.getBytes();
        for (int i = 0; i < size; i++) {
            bytes.set(i, (byte) i);
        }
        for (int i = 0; i < 10; i++) {
            bytes.set(1020 + i, (byte) i);
        }
        assertEquals(size, bytes.getRealSize());
        byte[] byteArray = bytes.getByteArray();
        assertEquals(size, byteArray.length);
        Bytes bytes2 = BytesFactory.getBytes(byteArray);
        assertEquals(size, bytes2.getRealSize());
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bytes2.get(1020 + i));
        }
    }

    public void testAppend() {
        int blockSize = Layer3Converter.DEFAULT_BYTES_SIZE;
        byte[] bb = new byte[blockSize];
        bb[blockSize - 1] = -1;
        bb[blockSize - 2] = -2;
        bb[blockSize - 3] = -3;
        Bytes bytes = BytesFactory.getBytes(bb);
        assertEquals(blockSize, bytes.getRealSize());
        byte[] bb2 = new byte[blockSize];
        bb2[0] = 1;
        bb2[1] = 2;
        bb2[2] = 3;
        bytes.append(bb2);
        assertEquals(2 * blockSize, bytes.getRealSize());
        assertEquals(-3, bytes.get(blockSize - 3));
        assertEquals(-2, bytes.get(blockSize - 2));
        assertEquals(-1, bytes.get(blockSize - 1));
        assertEquals(1, bytes.get(blockSize));
        assertEquals(2, bytes.get(blockSize + 1));
        assertEquals(3, bytes.get(blockSize + 2));
    }

    public void test22() {
        int size = SIZE;
        Bytes2 bytes = new Bytes2(100);
        long start = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            bytes.set(i, (byte) i);
        }
        long end1 = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            assertEquals((byte) i, bytes.get(i));
        }
        long end2 = System.currentTimeMillis();
        System.out.println(String.format("Bytes2 write=%d  and read=%d", (end1 - start), (end2 - end1)));
    }

    public void perfTest3() {
        int size = SIZE;
        long start = System.currentTimeMillis();
        byte[] bytes = new byte[size];
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) i;
        }
        long end1 = System.currentTimeMillis();
        assertEquals(size, bytes.length);
        for (int i = 0; i < size; i++) {
            assertEquals((byte) i, bytes[i]);
        }
        long end2 = System.currentTimeMillis();
        System.out.println(String.format("byte[] write=%d  and read=%d", (end1 - start), (end2 - end1)));
    }

    public void test4() {
        test2();
        test22();
        perfTest3();
    }

    public static void main(String[] args) {
        TestBytes t = new TestBytes();
        t.test2();
        t.test22();
        t.perfTest3();
    }
}
