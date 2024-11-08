package net.hanjava.test.lock;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class LockExam6 {

    /**
	 * RandomAccessFile�� ���� Exclusive Lock�� ���ϰ� OutputStream�� �� ����
	 * �� ������ �����Ѵ�.
	 * @author accent
	 * @throws IOException
	 */
    public static void main(String[] args) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(LockExam1.PATH, "rw");
        FileChannel ch = raf.getChannel();
        System.err.println("Trying to lock from FileOutputStream");
        FileLock lock = ch.lock();
        System.err.println("Got a lock - ready to write");
        LockExam1.pause();
        writeOnLockedFile();
        raf.close();
    }

    private static void writeOnLockedFile() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(LockExam1.PATH);
            fos.write("TestLockingTestLocking".getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
