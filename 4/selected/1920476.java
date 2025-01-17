package net.hanjava.test.io;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import net.hanjava.test.Util;

public class ArrayInputTest {

    public static final String LARGE_FILE_PATH = "C:/Temp/large.ppt";

    public static void main(String[] args) throws Exception {
        System.out.println("Before load : " + Util.getUsedMemory());
        File file = new File(LARGE_FILE_PATH);
        int size = (int) file.length();
        Util.pause();
        long beforeTime1 = System.currentTimeMillis();
        byte[] buf = new byte[size];
        FileInputStream fis = new FileInputStream(file);
        fis.read(buf);
        System.out.println("Before after array load : " + Util.getUsedMemory());
        Util.pause();
        FileChannel ch = fis.getChannel();
        MappedByteBuffer mbuf = ch.map(MapMode.READ_ONLY, 0, size);
        System.out.println("Before memory mapped I/O : " + Util.getUsedMemory());
        Util.pause();
        fis.close();
    }
}
