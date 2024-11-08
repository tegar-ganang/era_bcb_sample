package com.javaeedev.util;

import static org.junit.Assert.*;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class FileUtilTest {

    private String tempPath;

    @Before
    public void setUp() throws Exception {
        tempPath = System.getenv("TEMP");
        if (!tempPath.endsWith("/") && !tempPath.endsWith("//")) tempPath = tempPath + "/";
    }

    @Test
    public void testRemoveFile() throws IOException {
        assertFalse(FileUtil.removeFile(new File(tempPath + "non-exist-file.txt")));
        File f = new File(tempPath + "new-temp-file.txt");
        assertTrue(f.createNewFile());
        assertTrue(FileUtil.removeFile(f));
    }

    @Test
    public void testReadFile() throws IOException {
        byte[] data = new byte[1024 * 10240];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 127);
        }
        File f = new File(tempPath + "read-and-write-file.txt");
        long start = System.currentTimeMillis();
        FileUtil.writeFile(f, data);
        long end = System.currentTimeMillis();
        System.out.println("Write 10MB data: " + (end - start) + " ms.");
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        start = System.currentTimeMillis();
        FileUtil.readFile(f, new BufferedOutputStream(byteArray));
        end = System.currentTimeMillis();
        System.out.println("Read 10MB data: " + (end - start) + " ms.");
        byte[] content = byteArray.toByteArray();
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], content[i]);
        }
        assertTrue(FileUtil.removeFile(f));
    }
}
