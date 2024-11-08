package org.wat.wcy.isi.mmazur.bp.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

public class TestSerialize {

    public static void main(String args[]) {
        FileOutputStream fos = null;
        DataOutputStream dos = null;
        int bytes = 0;
        try {
            fos = new FileOutputStream("d:\\test.txt");
            dos = new DataOutputStream(fos);
            dos.writeUTF("Jeden=1");
            bytes = dos.size();
            dos.writeUTF("Dwa=2");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(dos);
        }
        FileInputStream fis = null;
        DataInputStream dis = null;
        try {
            fis = new FileInputStream("d:\\test.txt");
            dis = new DataInputStream(fis);
            dis.skipBytes(bytes);
            System.out.println(dis.readUTF());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(dis);
        }
    }

    private static void test4() throws Throwable {
        FileChannel rwChannel = new RandomAccessFile(new File("d:\\data.txt"), "rw").getChannel();
        int position = 4;
        ByteBuffer wrBuf = ByteBuffer.allocate((int) (rwChannel.size() - position));
        rwChannel.read(wrBuf, position);
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.put("Heja".getBytes());
        buf.flip();
        wrBuf.flip();
        rwChannel.position(4);
        rwChannel.write(buf);
        rwChannel.position(position + 4);
        rwChannel.write(wrBuf);
        rwChannel.close();
    }

    private static void test3() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("A", "AA");
        map.put("B", "BB");
        map.put("C", "CC");
        FileOutputStream fos = null;
        BufferedOutputStream oos = null;
        ObjectOutputStream dos = null;
        try {
            fos = new FileOutputStream("D:\\file.txt");
            oos = new BufferedOutputStream(fos);
            dos = new ObjectOutputStream(oos);
            dos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void test() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("A", "AA");
        map.put("B", "BB");
        map.put("C", "CC");
        FileOutputStream fos = null;
        BufferedOutputStream oos = null;
        DataOutputStream dos = null;
        try {
            fos = new FileOutputStream("D:\\file.txt");
            oos = new BufferedOutputStream(fos);
            dos = new DataOutputStream(oos);
            long l = 1000;
            dos.writeLong(l);
            dos.writeUTF("Jeden");
            dos.writeUTF("Dwa");
            dos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void test2() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("A", "AA");
        map.put("B", "BB");
        map.put("C", "CC");
        FileInputStream fos = null;
        BufferedInputStream oos = null;
        DataInputStream dos = null;
        try {
            fos = new FileInputStream("D:\\file.txt");
            oos = new BufferedInputStream(fos);
            dos = new DataInputStream(oos);
            long l = dos.readLong();
            System.out.println(l);
            String s = dos.readUTF();
            System.out.println(s);
            s = dos.readUTF();
            System.out.println(s);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void write() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream("d:\\test.txt");
            FileChannel fch = fos.getChannel();
            fch.lock();
            ByteBuffer srcs = ByteBuffer.allocate(2);
            srcs.put((byte) 5);
            srcs.put((byte) 6);
            fch.write(new ByteBuffer[] { srcs }, 2, 2);
            fos.write(Integer.MAX_VALUE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
