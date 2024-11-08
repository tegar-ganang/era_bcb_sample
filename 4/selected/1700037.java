package org.jscsi.target.storage;

import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RandomAccessStorageModuleTest {

    private static final String TEST_FILE_NAME = "storage_test_file.dat";

    private static RandomAccessStorageModule module = null;

    private static final int TEST_FILE_SIZE = 1024;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File file = new File(TEST_FILE_NAME);
        if (!file.exists()) file.createNewFile();
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(TEST_FILE_SIZE);
        raf.close();
        module = RandomAccessStorageModule.open(TEST_FILE_NAME);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        module.close();
        File file = new File(TEST_FILE_NAME);
        file.delete();
    }

    @Test
    public void testReadAndWrite() {
        final byte[] writeArray = new byte[TEST_FILE_SIZE];
        for (int i = 0; i < TEST_FILE_SIZE; ++i) writeArray[i] = (byte) (Math.random() * 256);
        final byte[] readArray = new byte[TEST_FILE_SIZE];
        try {
            module.write(writeArray, 0, TEST_FILE_SIZE, 0);
            module.read(readArray, 0, TEST_FILE_SIZE, 0);
        } catch (IOException e) {
            fail("IOException was thrown");
        }
        for (int i = 0; i < TEST_FILE_SIZE; ++i) if (writeArray[i] != readArray[i]) fail("values do not match");
    }

    @Test
    public void testCheckBounds0() {
        int result = module.checkBounds(0, 2);
        assert (result == 0);
        result = module.checkBounds(1, 1);
        assert (result == 0);
        result = module.checkBounds(1, 1);
        assert (result == 0);
        result = module.checkBounds(0, 0);
        assert (result == 0);
    }

    @Test
    public void testCheckBounds1() {
        int result = module.checkBounds(-1, 1);
        assert (result == 1);
        result = module.checkBounds(2, 1);
        assert (result == 1);
    }

    @Test
    public void testCheckBounds2() {
        int result = module.checkBounds(0, 3);
        assert (result == 2);
        result = module.checkBounds(0, -1);
        assert (result == 2);
    }

    @Test
    public void testOpen() {
        assert (module != null);
    }
}
