package org.vmasterdiff;

import java.awt.Component;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import javax.swing.JFileChooser;

public class RunTestNIO {

    public static void main(String[] args) {
        RunTestNIO obj = new RunTestNIO();
        String fileName = obj.getFileName(null);
        RandomAccessFile in = null;
        try {
            in = new RandomAccessFile(fileName, "r");
            obj.testOldIO(in);
            obj.testNewIONonDirectBuffer(in);
            obj.testNewIODirectBuffer(in);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    /**
	 *  Only opens files, any files(not directories)
	 */
    protected String getFileName(Component parent) {
        String path = System.getProperty("user.dir");
        JFileChooser chooser = new JFileChooser(path);
        int returnVal = chooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) return chooser.getSelectedFile().getAbsolutePath();
        return null;
    }

    private void testOldIO(RandomAccessFile in) throws Throwable {
        FileChannel channel = in.getChannel();
        byte[] buffer = new byte[5000];
        Date before = new Date();
        long len = in.length();
        for (int i = 0; i < 50; i++) {
            while (in.getFilePointer() < len) {
                in.read(buffer, 0, 1500);
            }
            in.seek(0);
        }
        Date after = new Date();
        double seconds = ((double) (after.getTime() - before.getTime())) / 1000;
        System.out.println("time(seconds)=" + seconds);
    }

    private void testNewIONonDirectBuffer(RandomAccessFile in) throws Throwable {
        FileChannel channel = in.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(5000);
        Date before = new Date();
        long len = channel.size();
        for (int i = 0; i < 50; i++) {
            System.out.println("i=" + i);
            while (channel.position() < len) {
                System.out.print("position=" + channel.position());
                channel.read(buffer, 1500);
                System.out.print("  pos2=" + channel.position());
                byte[] temp = buffer.array();
                System.out.println("  size=" + temp.length);
            }
            channel.position(0);
        }
        Date after = new Date();
        double seconds = ((double) (after.getTime() - before.getTime())) / 1000;
        System.out.println("time(seconds)=" + seconds);
    }

    private void testNewIODirectBuffer(RandomAccessFile in) throws Throwable {
        FileChannel channel = in.getChannel();
        ByteBuffer buffer = ByteBuffer.allocateDirect(5000);
        Date before = new Date();
        long len = channel.size();
        for (int i = 0; i < 50; i++) {
            System.out.println("i=" + i);
            while (channel.position() < len) {
                System.out.print("position=" + channel.position());
                channel.read(buffer, 1500);
                System.out.println("   pos2=" + channel.position());
            }
            channel.position(0);
        }
        Date after = new Date();
        double seconds = ((double) (after.getTime() - before.getTime())) / 1000;
        System.out.println("time(seconds)=" + seconds);
    }
}
