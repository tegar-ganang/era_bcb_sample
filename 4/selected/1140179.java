package net.hanjava.test.lock;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * FileInputStream���� ���� FileChannel�� FileLock�� �������� ����.
 * Exclusive File Lock�� �������� �õ��Ѵ�.
 * 
 * @author accent
 */
public class LockExam2 {

    public static void main(String[] args) throws IOException {
        FileInputStream fis = new FileInputStream(LockExam1.PATH);
        FileChannel ch = fis.getChannel();
        System.err.println("Trying to lock from FileInputStream");
        FileLock lock = ch.lock();
        System.err.println("Success on lock : " + lock);
        fis.close();
    }
}
