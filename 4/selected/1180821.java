package de.rayweb.test.test;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.util.Arrays;

public class NIOTest {

    public static void testUDP() throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[100], 100);
        DatagramSocket socket = new DatagramSocket(22222);
        socket.receive(dp);
        System.out.println("a");
    }

    public static void testUDPsend() throws IOException {
        DatagramPacket dp = new DatagramPacket(new byte[100], 100, InetAddress.getLocalHost(), 22222);
        DatagramSocket socket = new DatagramSocket();
        socket.send(dp);
        System.out.println("vb");
    }

    public static void testFile() throws IOException {
        FileInputStream fis = new FileInputStream("C:\\test2.rtf");
        FileChannel channel = fis.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(100);
        int i = channel.read(buffer);
        channel.close();
        System.out.println(new String(buffer.array()));
    }

    public static void testPipe() throws IOException {
        Pipe pipe = Pipe.open();
        SinkChannel sink = pipe.sink();
        SourceChannel source = pipe.source();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("asd".getBytes());
        ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
        sink.write(buffer);
    }

    public static void testUDP_NIO() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(22223));
        System.out.println(channel.socket().isConnected());
        System.out.println(channel.isOpen());
        System.out.println(channel.isBlocking());
        ByteBuffer buffer = ByteBuffer.allocate(1400);
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            buffer.clear();
            channel.receive(buffer);
            if (buffer.position() == 0) System.out.println("-");
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Runnable() {

            public void run() {
                try {
                    byte[] b = new byte[1400];
                    Arrays.fill(b, "X".getBytes("UTF-8")[0]);
                    DatagramPacket dp = new DatagramPacket(b, b.length, InetAddress.getLocalHost(), 22223);
                    DatagramSocket ds = new DatagramSocket();
                    for (int i = 0; i < 1000; i++) {
                        byte[] bytes = ("" + (i + 100)).getBytes("UTF-8");
                        b[0] = bytes[0];
                        b[1] = bytes[1];
                        b[2] = bytes[2];
                        dp.setData(b);
                        ds.send(dp);
                        System.out.println("send" + i);
                        Thread.sleep(20);
                    }
                    Thread.sleep(2000);
                    for (int i = 0; i < 1000; i++) {
                        byte[] bytes = ("" + (i + 100)).getBytes("UTF-8");
                        b[0] = bytes[0];
                        b[1] = bytes[1];
                        b[2] = bytes[2];
                        dp.setData(b);
                        ds.send(dp);
                        System.out.println("send" + i);
                        Thread.sleep(20);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        testUDP_NIO();
    }
}
