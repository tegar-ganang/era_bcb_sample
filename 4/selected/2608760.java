package tests;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import edu.virginia.cs.storagedesk.common.NativeFile;

public class Test {

    public static void test() {
        NativeFile hello = new NativeFile();
        byte[] bytes = new byte[65536];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        long pos = 1;
        hello.write("\\temp\\1.txt", bytes, pos);
        System.out.println("to verify");
        byte[] results = hello.read("\\temp\\1.txt", pos, bytes.length);
        boolean diff = false;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != results[i]) {
                System.err.println("wrong result at " + i + " : " + bytes[i] + " - " + results[i]);
                diff = true;
            }
        }
        if (diff) {
            System.out.println("diff ");
        } else {
            System.out.println("same ");
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        try {
            RandomAccessFile afile = new RandomAccessFile("\\temp\\1.txt", "rws");
            byte[] bytes = new byte[65536];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) i;
            }
            FileChannel fc = afile.getChannel();
            for (int i = 0; i < 2500; i++) {
                System.out.println(i * 65536 / (1024 * 1024));
                fc.write(ByteBuffer.wrap(bytes));
            }
            fc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
