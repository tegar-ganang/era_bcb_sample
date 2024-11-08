package fi.hip.gb.disk.client;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/** 
 * Class Filemaker is used to produce random files for testing
 * purposes. The file has random contents and size in MB defined by
 * the given argument. Only a simple main method here.
 */
public class FileMaker {

    public static void makeMegaFile(int megas, File file) {
        makeKiloFile(megas * 1024, file);
    }

    public static void makeKiloFile(long kiloBytes, File file) {
        Random ranGen = new Random();
        byte[] byteArray = new byte[1024];
        try {
            FileOutputStream fileStream = new FileOutputStream(file);
            for (int i = 0; i < kiloBytes; i++) {
                ranGen.nextBytes(byteArray);
                fileStream.write(byteArray);
            }
            fileStream.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void makeFile(long bytes, File file) {
        Random ranGen = new Random();
        try {
            FileOutputStream fileStream = new FileOutputStream(file);
            for (int i = 0; i < bytes; i++) {
                fileStream.write(ranGen.nextInt());
            }
            fileStream.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void makeNioKiloFile(long bytes, File file) {
        Random ranGen = new Random();
        ByteBuffer BB = ByteBuffer.allocate(1024);
        try {
            FileChannel chann = new FileOutputStream(file).getChannel();
            byte[] byteArr = new byte[1024];
            for (int i = 0; i < bytes; i++) {
                ranGen.nextBytes(byteArr);
                BB.put(byteArr);
                BB.flip();
                chann.write(BB);
                BB.clear();
            }
            chann.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String name = args[0];
        int size = Integer.parseInt(args[1]);
        File output = new File(name);
        makeMegaFile(size, output);
    }
}
