package org.susan.java.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileHole {

    public static void main(String args[]) throws IOException {
        File temp = File.createTempFile("holy", null);
        RandomAccessFile file = new RandomAccessFile(temp, "rw");
        FileChannel channel = file.getChannel();
        ByteBuffer buffer = ByteBuffer.allocateDirect(100);
        putData(0, buffer, channel);
        putData(5000000, buffer, channel);
        putData(50000, buffer, channel);
        System.out.println("Wrote temp file '" + temp.getPath() + "',size = " + channel.size());
        channel.close();
        file.close();
    }

    private static void putData(int position, ByteBuffer buffer, FileChannel fileChannel) throws IOException {
        String string = "*<-- location " + position;
        buffer.clear();
        buffer.put(string.getBytes("US-ASCII"));
        buffer.flip();
        fileChannel.position(position);
        fileChannel.write(buffer);
    }
}
