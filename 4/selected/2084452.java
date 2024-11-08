package equilClient;

import equilSharedFramework.IncomingCommunicator;
import java.io.*;
import java.nio.channels.*;

/**
 * The client for EQUIL.
 * 
 * @author CITS3200 2006 Group E
 * @version 1.0.0
 */
class EQUILClient {

    static ClientState running;

    public static void main(String[] args) {
        try {
            new EQUILClient();
        } catch (Exception e) {
            System.out.println("Unhandled Exception in EquilClient");
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public EQUILClient() {
        running = new ClientState();
        if (!running.lockClient()) {
            System.out.println("Cannot start client " + "as there is already a client " + "running on this machine");
            System.exit(1);
        }
        IncomingCommunicator ic = null;
        try {
            ic = new IncomingCommunicator();
        } catch (Exception e) {
            System.out.println("Failed to create listener");
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        new GUIManager(ic);
    }

    private class ClientState {

        protected boolean unLockClient() {
            try {
                getLock().release();
            } catch (Exception e) {
                return false;
            }
            try {
                getMonitor().close();
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        protected boolean lockClient() {
            Runtime.getRuntime().addShutdownHook(new shutdownThread());
            try {
                setMutex(new RandomAccessFile("Equil.tmp", "rw"));
                setMonitor(getMutex().getChannel());
                try {
                    setLock(getMonitor().tryLock());
                } catch (Exception e) {
                    unLockClient();
                    return false;
                }
                if (getLock() == null) {
                    unLockClient();
                    return false;
                }
                return true;
            } catch (Exception e) {
                unLockClient();
                return false;
            }
        }

        protected FileLock getLock() {
            return lock;
        }

        protected void setLock(FileLock lock) {
            this.lock = lock;
        }

        protected RandomAccessFile getMutex() {
            return mutex;
        }

        protected void setMutex(RandomAccessFile mutex) {
            this.mutex = mutex;
        }

        protected FileChannel getMonitor() {
            return monitor;
        }

        protected void setMonitor(FileChannel monitor) {
            this.monitor = monitor;
        }

        private FileLock lock;

        private RandomAccessFile mutex;

        private FileChannel monitor;

        private class shutdownThread extends Thread {

            public void run() {
                unLockClient();
                try {
                    getMutex().close();
                } catch (Exception e) {
                    System.exit(1);
                }
            }
        }
    }
}
