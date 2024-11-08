package org.susan.java.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class ExplicitChannelRead {

    public static void main(String args[]) {
        FileInputStream fileInputStream;
        FileChannel fileChannel;
        long fileSize;
        ByteBuffer byteBuffer;
        try {
            fileInputStream = new FileInputStream("D:\\read.txt");
            fileChannel = fileInputStream.getChannel();
            fileSize = fileChannel.size();
            byteBuffer = ByteBuffer.allocate((int) fileSize);
            fileChannel.read(byteBuffer);
            byteBuffer.rewind();
            for (int i = 0; i < fileSize; i++) System.out.print((char) byteBuffer.get());
            fileChannel.close();
            fileInputStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
