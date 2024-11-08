package net.hanjava.test.lock;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * �׳� Lock �ɱ�. FileOutputStream�� ���� FileChannel�� ����.
 * ������ Ȧ�� �������.
 * 
 * @author accent
 */
public class LockExam1 {

    public static final String PATH = "C:/Temp/lock.xls";

    public static void pause() {
        System.err.println("Press ENTER to continue");
        byte[] temp = new byte[100];
        try {
            System.in.read(temp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        FileOutputStream fos = new FileOutputStream(PATH);
        FileChannel ch = fos.getChannel();
        System.err.println("Trying to lock from FileOutputStream");
        FileLock lock = ch.lock();
        System.err.println("Got a lock");
        pause();
        fos.close();
    }
}
