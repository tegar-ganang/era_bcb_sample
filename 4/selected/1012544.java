package com.jot.test.tryouts.performance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import com.jot.system.utils.JotTime;

public class AsyncBlockClients {

    static boolean dolog = false;

    static int running = 0;

    static int instanceNumber = 0;

    static int threadCount = 200;

    static int fetchCount = 900;

    static OutputStream[] outs = new OutputStream[threadCount];

    static InputStream[] ins = new InputStream[threadCount];

    static Socket[] sockets = new Socket[threadCount];

    static int[] incounts = new int[threadCount];

    static int[] outcounts = new int[threadCount];

    public boolean started = false;

    public static void connect() throws IOException {
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

    public static int write() {
        int amt = 0;
        for (int i = 0; i < threadCount; i++) {
            if (outcounts[i] < fetchCount) {
                try {
                    outs[i].write(BlockDataServer.block);
                    outcounts[i]++;
                    if (dolog) System.out.println("client wrote " + outcounts[i] + " on " + i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            amt += outcounts[i];
        }
        return amt;
    }

    static byte[] dest = new byte[BlockDataServer.size];

    public static int read() {
        int amt = 0;
        for (int i = 0; i < threadCount; i++) {
            try {
                if (incounts[i] < fetchCount) {
                    int avail = ins[i].available();
                    while (avail >= BlockDataServer.size) {
                        ins[i].read(dest, 0, BlockDataServer.size);
                        incounts[i]++;
                        if (dolog) System.out.println("client read " + incounts[i] + " on " + i);
                        avail = ins[i].available();
                    }
                } else if (ins[i].available() > 0) {
                    System.out.println("have data on empty channel !!!1");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            amt += incounts[i];
        }
        return amt;
    }

    static class Writer implements Runnable {

        public void run() {
            int amt = 0;
            while (amt < (threadCount * fetchCount)) {
                amt = write();
            }
            System.out.println("writing finished");
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("AsyncBlockData client starting");
        connect();
        Thread writeThread = new Thread(new Writer());
        long starttime = JotTime.get();
        writeThread.start();
        int amt = 0;
        while (amt < (threadCount * fetchCount)) {
            amt = read();
        }
        long endtime = JotTime.get();
        System.out.println("reading finished");
        close();
        long totalms = (endtime - starttime);
        double totalseconds = (double) totalms / 1000;
        System.out.println("test is done");
        System.out.println("total time is " + totalms + " ms " + "for " + threadCount * fetchCount + " reads of " + BlockDataServer.size + " each");
        System.out.println("or " + (threadCount * fetchCount) / totalseconds + " reads/sec");
        System.out.println("data rate is " + ((long) (threadCount * fetchCount) * BlockDataServer.size) / totalseconds + " bytes/sec");
        System.out.println("data rate is " + ((long) (threadCount * fetchCount) * BlockDataServer.size / (1024 * 1024)) / totalseconds + " M bytes/sec");
    }
}
