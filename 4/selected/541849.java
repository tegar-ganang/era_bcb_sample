package com.jot.test.tryouts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import com.jot.system.utils.JotTime;
import com.jot.system.visitors.ByteMessageVisitor;

/**
 * An echo test. Uses plain sockets, no NIO, and a reading thread and a writing thread other tests use the
 * makeMessage() method
 * 
 * @author alanwootton
 * 
 */
public class AsyncClientEchoTest {

    static final boolean dolog = false;

    public static final int messageSize = 256;

    static final int threadCount = 1;

    static final int messageCount = 1;

    static int running = 0;

    static int instanceNumber = 0;

    static OutputStream[] outs = new OutputStream[threadCount];

    static InputStream[] ins = new InputStream[threadCount];

    static Socket[] sockets = new Socket[threadCount];

    static int[] incounts = new int[threadCount];

    static int[] outcounts = new int[threadCount];

    public boolean started = false;

    public static int totalOut = 0;

    public static int totalIn = 0;

    public static byte[] writeMessageBytes = null;

    public static byte[] makeMessage() {
        if (writeMessageBytes != null) return writeMessageBytes;
        ByteBuffer dest = ByteBuffer.allocate(messageSize + 6);
        dest.put((byte) 'j');
        dest.put((byte) 'o');
        dest.putShort((short) (messageSize + 2 + 2 + 2));
        dest.putShort((short) ByteMessageVisitor.MessageEnum.echo.ordinal());
        byte[] sample = "Hello from atw. 1234567890abcdefguhjklmnopquvwxyz.".getBytes();
        int i = 0;
        while (dest.position() < ((messageSize + 2 + 2 + 2))) dest.put(sample[i++ % sample.length]);
        dest.flip();
        writeMessageBytes = new byte[dest.limit()];
        dest.get(writeMessageBytes);
        return writeMessageBytes;
    }

    public static void connect() throws IOException {
        makeMessage();
        for (int i = 0; i < threadCount; i++) {
            String hostname = "localhost";
            int port = 8080;
            InetAddress addr = InetAddress.getByName(hostname);
            sockets[i] = new Socket(addr, port);
            while (!sockets[i].isConnected()) try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outs[i] = sockets[i].getOutputStream();
            ins[i] = sockets[i].getInputStream();
            if (dolog) System.out.println("client connected " + i);
        }
        System.out.println(threadCount + " clients connected");
    }

    public static void close() throws IOException {
        for (int i = 0; i < threadCount; i++) {
            outs[i].close();
            ins[i].close();
        }
    }

    public static int write() throws IOException {
        int amt = 0;
        for (int i = 0; i < threadCount; i++) {
            if (outcounts[i] < messageCount) {
                outs[i].write(writeMessageBytes);
                if (dolog) System.out.println("client wrote " + outcounts[i] + " on " + i);
                outcounts[i]++;
                totalOut++;
                if (outcounts[i] >= messageCount) {
                    outs[i].flush();
                }
            }
            amt += outcounts[i];
        }
        return amt;
    }

    static byte[] dest = new byte[messageSize + 6];

    public static int read() throws IOException {
        int amt = 0;
        for (int i = 0; i < threadCount; i++) {
            if (incounts[i] < messageCount) {
                int avail = ins[i].available();
                while (avail >= writeMessageBytes.length) {
                    ins[i].read(dest, 0, writeMessageBytes.length);
                    if (dolog) System.out.println("client read " + incounts[i] + " on " + i);
                    incounts[i]++;
                    totalIn++;
                    avail = ins[i].available();
                }
                if (incounts[i] >= messageCount) {
                    ins[i].close();
                    outs[i].close();
                }
            }
            amt += incounts[i];
        }
        return amt;
    }

    static class Writer implements Runnable {

        public void run() {
            try {
                int amt = 0;
                long starttime = JotTime.get();
                while (amt < (threadCount * messageCount)) {
                    amt = write();
                    if (starttime + 10000 < JotTime.get()) {
                        System.out.println("wrote " + totalOut);
                        starttime = JotTime.get();
                    }
                }
                System.out.println("writing finished");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("AsyncClientEchoTest client starting");
        connect();
        Thread writeThread = new Thread(new Writer());
        long starttime = JotTime.get();
        writeThread.start();
        int amt = 0;
        try {
            long thetime = JotTime.get();
            while (amt < (threadCount * messageCount) && starttime + 45 * 1000 > JotTime.get()) {
                amt = read();
                if (thetime + 10000 < JotTime.get()) {
                    System.out.println("read " + totalIn);
                    thetime = JotTime.get();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (amt < (threadCount * messageCount)) {
            System.out.println("FAILED ");
            for (int i = 0; i < threadCount; i++) {
                System.out.print("thread" + i);
                System.out.print(" outcounts" + outcounts[i]);
                System.out.print(" incounts" + incounts[i]);
                System.out.println();
            }
        }
        long endtime = JotTime.get();
        System.out.println("reading finished");
        close();
        long totalms = (endtime - starttime);
        double totalseconds = (double) totalms / 1000;
        System.out.println("test is done");
        System.out.println("total time is " + totalms + " ms " + "for " + threadCount * messageCount + " reads of " + messageSize + " each");
        System.out.println("or " + (threadCount * messageCount) / totalseconds + " reads/sec");
        System.out.println("data rate is " + ((long) (threadCount * messageCount) * messageSize) / totalseconds + " bytes/sec");
        System.out.println("data rate is " + ((long) (threadCount * messageCount) * messageSize / (1024 * 1024)) / totalseconds + " M bytes/sec");
    }
}
