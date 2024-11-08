package uk.midearth.dvb.server;

import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.io.*;
import java.lang.*;
import uk.midearth.dvb.confParser.*;
import uk.midearth.dvb.*;

public class RemoteDVB extends UnicastRemoteObject implements RemoteDVBInterface {

    private ConfParser confParser;

    private Configurator conf;

    private TzapThread tzapThread = new TzapThread();

    private String localhost;

    private String client;

    public RemoteDVB(String newConfigFilename, String newConfFilename) throws RemoteException {
        System.out.println("RemoteDVB>> Creating remote RemoteDVB object");
        confParser = new ConfParser(newConfigFilename);
        conf = new Configurator(newConfFilename);
        tzapThread.start();
    }

    private void stopChannel() {
    }

    public void channel(int channelNo) throws RemoteException {
        tzapThread.changeChannels(channelNo);
    }

    public String[] channelList() throws RemoteException {
        return confParser.channelList();
    }

    public String currentChannel() throws RemoteException {
        int chan = tzapThread.currentChannel();
        if (chan == -1) return new String("Nothing");
        return new String(confParser.getChannel(chan).channelName());
    }

    public int currentChannelNo() throws RemoteException {
        return tzapThread.currentChannel();
    }

    public void stopNow() throws RemoteException {
        tzapThread.cleanUp();
    }

    public void stdErr() throws RemoteException {
        tzapThread.stdErr();
    }

    public void stdOut() throws RemoteException {
        tzapThread.stdOut();
    }

    class TzapThread extends Thread {

        private int timeout = 1000;

        private Process tzap = null;

        private Process dvbStream = null;

        private boolean processChannel = false;

        private int currentChar = -1;

        private int channelNo = -1;

        private Runtime rt;

        public synchronized void run() {
            while (true) {
                try {
                    while (!processChannel) wait();
                } catch (InterruptedException e) {
                }
                try {
                    rt = Runtime.getRuntime();
                    tzap = rt.exec(conf.get("tzap") + " -c " + confParser.configFilename() + " " + confParser.getChannel(channelNo).channelName());
                    sleep(timeout);
                    dvbStream = rt.exec(conf.get("dvbstream") + " " + confParser.getChannel(channelNo).videoPID() + " " + confParser.getChannel(channelNo).audioPID());
                    wait();
                } catch (Exception e) {
                    System.out.println("RemoteDVB.TzapListner>> Tzap died: " + e);
                    e.printStackTrace();
                }
            }
        }

        public synchronized void changeChannels(int newChannel) {
            cleanUp();
            channelNo = newChannel;
            processChannel = true;
            notifyAll();
        }

        public int currentChannel() {
            return channelNo;
        }

        public void cleanUp() {
            if (dvbStream != null) dvbStream.destroy();
            if (tzap != null) tzap.destroy();
        }

        public void stdErr() {
            try {
                int i = tzap.getErrorStream().read();
                while (i != -1) {
                    System.out.print((char) i);
                    i = tzap.getErrorStream().read();
                }
            } catch (Exception e) {
                System.out.println("RemoteDVB.TzapThread>> Failed to show stderr: " + e);
                e.printStackTrace();
            }
            try {
                int i = dvbStream.getErrorStream().read();
                while (i != -1) {
                    System.out.print((char) i);
                    i = dvbStream.getErrorStream().read();
                }
            } catch (Exception e) {
                System.out.println("RemoteDVB.TzapThread>> Failed to show stderr: " + e);
                e.printStackTrace();
            }
        }

        public void stdOut() {
            try {
                int i = tzap.getInputStream().read();
                while (i != -1) {
                    System.out.print((char) i);
                    i = tzap.getInputStream().read();
                }
            } catch (Exception e) {
                System.out.println("RemoteDVB.TzapThread>> Failed to show stderr: " + e);
                e.printStackTrace();
            }
            try {
                int i = dvbStream.getInputStream().read();
                while (i != -1) {
                    System.out.print((char) i);
                    i = dvbStream.getInputStream().read();
                }
            } catch (Exception e) {
                System.out.println("RemoteDVB.TzapThread>> Failed to show stderr: " + e);
                e.printStackTrace();
            }
        }
    }
}
