package home.projects.misc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTPClient;

public class FtpTest {

    public static void main(String[] args) {
        new FtpTest().add("a", "b");
    }

    public int add(Character a, Character b) {
        return 42;
    }

    public int add(Comparable a, Comparable b) {
        return 42;
    }

    private int add(Object a, Object b) {
        return 42;
    }

    private int add(int a, int b) {
        return 42;
    }
}

class FtpTester {

    public void test() {
        for (int i = 0; i < 10; ++i) {
            FtpWorker worker = new FtpWorker();
            worker.start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class FtpWorker extends Thread {

    private static final String DIR = "xxxDirxxx";

    private static final String FILE_PREFIX = "file-";

    @Override
    public void run() {
        try {
            FTPClient ftp = new FTPClient();
            try {
                ftp.connect("localhost", 21);
                ftp.login("ftpuser", "ftpuser123");
                System.out.println("Current: " + ftp.printWorkingDirectory());
                System.out.println("Dir status: " + ftp.makeDirectory(DIR));
                ftp.changeWorkingDirectory(DIR);
                System.out.println("File status: " + ftp.storeFile(FILE_PREFIX + this.getName(), getByteInputStream()));
            } finally {
                ftp.disconnect();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getByteInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream("HELLO".getBytes());
        return bais;
    }
}
