package net.hanjava.test.lock;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class LockExam9 {

    public static void main(String[] args) throws IOException {
        FileOutputStream fos = new FileOutputStream(LockExam1.PATH, true);
        FileChannel ch = fos.getChannel();
        ch.lock();
        LockExam1.pause();
        fos.close();
    }
}
