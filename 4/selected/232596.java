package com.sts.webmeet.tests.client;

import com.sts.webmeet.common.*;
import com.sts.webmeet.content.common.chat.*;
import java.net.*;
import java.io.*;

public class SocketClient {

    public static void main(String[] args) throws Exception {
        SocketClient tester = new SocketClient(args[0], args[1], args[2], args[3], args[4], args[5]);
        tester.join();
    }

    public SocketClient(String strHost, String strPort, String strConfID, String strPassword, String strThreadCount, String strName) throws Exception {
        this.strHost = strHost;
        this.iPort = Integer.parseInt(strPort);
        this.strConfID = strConfID;
        this.strPassword = strPassword;
        this.iThreadCount = Integer.parseInt(strThreadCount);
        threads = new Thread[iThreadCount];
        for (int i = 0; i < iThreadCount; i++) {
            threads[i] = new Thread(new RunnableConnection(strName + i));
            threads[i].start();
        }
        for (int i = 0; i < iThreadCount; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public void join() throws InterruptedException {
        threads[0].join();
    }

    class RunnableConnection implements Runnable {

        public RunnableConnection(String strName) {
            this.strName = strName;
        }

        public void run() {
            WebmeetMessage mess = null;
            try {
                Socket sock = new Socket(strHost, iPort);
                ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                ServerIDMessage serverID = (ServerIDMessage) ois.readObject();
                pi.setServerID(serverID.getServerID());
                pi.setLogonName(strName);
                pi.setConfID(strConfID);
                sendMessage(new RosterJoinMessage(strPassword));
                Object obj = null;
                do {
                    obj = ois.readObject();
                } while (!(obj instanceof RosterJoinAcceptMessage));
                Runnable writerThread = new Runnable() {

                    public void run() {
                        try {
                            while (true) {
                                sendMessage(new ChatMessage("test"));
                                Thread.currentThread().sleep(5000);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                Thread writer = new Thread(writerThread);
                writer.start();
                while (true) {
                    mess = (WebmeetMessage) ois.readObject();
                    mess = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendMessage(WebmeetMessage mess) throws Exception {
            mess.setSender(pi);
            Socket sock = new Socket(strHost, iPort + 2);
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            oos.writeObject(mess);
            oos.flush();
            oos.close();
            System.out.println("wrote message");
        }

        private String strName;

        private ParticipantInfo pi = new ParticipantInfo();

        private boolean bJoined;
    }

    private Thread[] threads;

    String strHost;

    int iPort;

    String strConfID;

    String strPassword;

    int iThreadCount;
}
