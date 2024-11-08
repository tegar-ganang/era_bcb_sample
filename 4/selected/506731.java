package net.hanjava.test.lock;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Compound File�� lock�� �ɸ� Ž������ '�Ӽ�'�� �ߴµ� ���� �ɸ���.
 * �̸� ���ϱ� ���� lock�� �Ŵ� ������ ũ�⸦ 0���� �Ѵ�.
 * @author accent
 */
public class LockExam7 {

    public static void main(String[] args) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(LockExam1.PATH, "rw");
        FileChannel ch = raf.getChannel();
        System.err.println("Trying to lock from FileOutputStream");
        FileLock lock = ch.lock(0, 0, false);
        System.err.println("Got a lock");
        LockExam1.pause();
        raf.close();
    }
}
