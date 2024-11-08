package com.iver.utiles.bigfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

/**
 * @author fjp
 *
 */
public class TestBigByteBuffer {

    File f = new File("D:/Fjp/chiara/plano/vias.shp");

    FileInputStream fin;

    FileInputStream fin2;

    FileChannel channel2;

    BigByteBuffer2 bb;

    static int numPruebas = 50000;

    public class MyThread extends Thread {

        String name;

        public MyThread(String string) {
            name = string;
        }

        public void run() {
            try {
                prueba2(name, f, numPruebas);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        TestBigByteBuffer test = new TestBigByteBuffer();
        test.test();
    }

    public void test() throws IOException {
        fin2 = new FileInputStream(f);
        channel2 = fin2.getChannel();
        bb = new BigByteBuffer2(channel2, FileChannel.MapMode.READ_ONLY, 1024 * 8);
        MyThread th = new MyThread("T1:");
        th.start();
        MyThread th2 = new MyThread("T2: ");
        th2.start();
        System.out.println("Fin de la prueba. " + numPruebas + " iteraciones.");
    }

    /**
     * @param name 
     * @param f
     * @param numPruebas
     * @throws Exception 
     */
    private void prueba2(String name, File f, int numPruebas) throws Exception {
        FileInputStream fin;
        fin = new FileInputStream(f);
        FileChannel channel = fin.getChannel();
        int size = (int) channel.size();
        ByteBuffer bbCorrect = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        Random rnd = new Random();
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < numPruebas; i++) {
            int pos = rnd.nextInt(size - 10);
            bbCorrect.position(pos);
            int bCorrect = bbCorrect.getInt();
            int bPrueba = bb.getInt(pos);
            if (bCorrect != bPrueba) {
                System.err.println(name + "Error de lectura. " + bCorrect + " " + bPrueba);
                throw new Exception("Error con pos=" + pos);
            } else {
                System.out.println(name + "Correcto: pos=" + pos + " byte= " + bPrueba);
            }
        }
        close(channel2, fin2, bb);
        long t2 = System.currentTimeMillis();
        System.out.println("T=" + (t2 - t1) + "mseconds");
    }

    /**
     * @param f
     * @param numPruebas
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void prueba1(File f, int numPruebas) throws FileNotFoundException, IOException {
        FileInputStream fin;
        fin = new FileInputStream(f);
        FileChannel channel = fin.getChannel();
        int size = (int) channel.size();
        ByteBuffer bbCorrect = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);
        Random rnd = new Random();
        long t1 = System.currentTimeMillis();
        FileInputStream fin2 = new FileInputStream(f);
        FileChannel channel2 = fin2.getChannel();
        BigByteBuffer bb = new BigByteBuffer(channel2, FileChannel.MapMode.READ_ONLY, 1024 * 1024);
        for (int i = 0; i < numPruebas; i++) {
            int pos = rnd.nextInt(size - 10);
            byte bCorrect = bbCorrect.get(pos);
            byte bPrueba = bb.get(pos);
            if (bCorrect != bPrueba) {
                System.err.println("Error de lectura. " + bCorrect + " " + bPrueba);
            } else {
            }
        }
        close(channel2, fin2, bb);
        System.gc();
        long t2 = System.currentTimeMillis();
        System.out.println("T=" + (t2 - t1) + "mseconds");
    }

    public static synchronized void close(FileChannel channel, FileInputStream fin, BigByteBuffer2 bb) throws IOException {
        IOException ret = null;
        try {
            channel.close();
        } catch (IOException e) {
            ret = e;
        } finally {
            try {
                fin.close();
            } catch (IOException e1) {
                ret = e1;
            }
        }
        if (ret != null) {
            throw ret;
        }
    }

    public static synchronized void close(FileChannel channel, FileInputStream fin, BigByteBuffer bb) throws IOException {
        IOException ret = null;
        try {
            channel.close();
        } catch (IOException e) {
            ret = e;
        } finally {
            try {
                fin.close();
            } catch (IOException e1) {
                ret = e1;
            }
        }
        if (ret != null) {
            throw ret;
        } else bb = null;
    }
}
