package net.hanjava.test.lock;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class LockExample8 {

    public static void main(String[] args) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(LockExam1.PATH, "rw");
        FileChannel ch = raf.getChannel();
        System.err.println("Trying to lock from RandomAccessFile");
        MappedByteBuffer mbuf = ch.map(MapMode.READ_WRITE, 0, ch.size());
        mbuf.put(0, mbuf.get(0));
        mbuf.force();
        System.err.println("Mapped : " + mbuf);
        LockExam1.pause();
        raf.close();
    }
}
