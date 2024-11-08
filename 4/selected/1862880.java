package org.susan.java.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class NativeFileWrite {

    public static void main(String args[]) {
        FileOutputStream fileOutputStream;
        FileChannel fileChannel;
        ByteBuffer byteBuffer;
        try {
            fileOutputStream = new FileOutputStream("D:/work/test.txt");
            fileChannel = fileOutputStream.getChannel();
            byteBuffer = ByteBuffer.allocateDirect(26);
            for (int i = 0; i < 26; i++) byteBuffer.put((byte) ('A' + i));
            byteBuffer.rewind();
            fileChannel.write(byteBuffer);
            fileChannel.close();
            fileOutputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
