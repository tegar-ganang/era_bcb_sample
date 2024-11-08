package net.hanjava.test.lock;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * LockExam3�� ������ channel�� invalid ���·� �����.
 * @author accent
 */
public class LockExam4 {

    public static void main(String[] args) throws IOException {
        FileInputStream fis = new FileInputStream(LockExam1.PATH);
        FileChannel ch = fis.getChannel();
        System.err.println("Trying to lock from FileInputStream");
        FileLock lock = ch.lock(0, Long.MAX_VALUE, true);
        System.err.println("Success on lock : " + lock);
        LockExam1.pause();
        fis.close();
        System.err.println("Invalid lock : " + lock);
    }
}
