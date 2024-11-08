package org.grailrtls.gui.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.zip.GZIPInputStream;
import org.grailrtls.gui.event.CommandResponseEvent;
import org.grailrtls.gui.event.ConnectionEvent;
import org.grailrtls.server.EventManager;

/**
 * @author Robert S. Moore II
 *
 */
public class ReplayServerInterface extends ServerInterface {

    private volatile boolean keepReplaying = false;

    private volatile boolean paused = false;

    private final File replayFile;

    private FileInputStream fileInput;

    private DataInputStream dataInput;

    private volatile long replayStart = 0l;

    private volatile long replayIndex = 0l;

    private volatile long bytesRead = 0;

    private volatile long fileSize = 1;

    public ReplayServerInterface(File replayFile) {
        super();
        if (replayFile == null) throw new NullPointerException("Cannot replay a null file.");
        this.replayFile = replayFile;
        if (!this.replayFile.exists()) {
            System.err.println("Replay file missing.");
            System.exit(1);
        }
        if (!this.replayFile.canRead()) {
            System.err.println("Could not read from replay file.");
            System.exit(1);
        }
        this.fileSize = this.replayFile.length();
    }

    @Override
    public synchronized boolean setServerInfo(InetAddress serverAddress, int port) {
        return true;
    }

    @Override
    public synchronized boolean setUserInfo(String userName, String password) {
        return true;
    }

    @Override
    public synchronized void closeConnection() {
        this.keepReplaying = false;
        if (this.dataInput == null) return;
        try {
            this.dataInput.close();
            this.fileInput.close();
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            ioe.printStackTrace(System.err);
            System.exit(1);
        }
        this.fileInput = null;
        this.dataInput = null;
        this.fireServerEvent(new ConnectionEvent(this, false));
    }

    @Override
    public synchronized boolean loginToServer() {
        this.keepReplaying = true;
        try {
            this.fileInput = new FileInputStream(this.replayFile);
            this.dataInput = new DataInputStream(this.fileInput);
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
            ioe.printStackTrace(System.err);
            System.exit(1);
        }
        this.notifyAll();
        this.fireServerEvent(new ConnectionEvent(this, true));
        this.bytesRead = 0;
        this.replayStart = 0l;
        this.replayIndex = 0l;
        return true;
    }

    @Override
    public synchronized boolean checkServerConnection() {
        return true;
    }

    @Override
    public synchronized void stopRunning() {
        this.keepReplaying = false;
    }

    @Override
    public synchronized void restart() {
    }

    public synchronized boolean togglePaused() {
        if (this.paused) {
            this.paused = false;
            this.notifyAll();
        } else {
            this.paused = true;
        }
        return this.paused;
    }

    public int getFileProgress() {
        if (this.fileInput == null) return 0;
        return (int) this.bytesRead;
    }

    public long getTimeIndex() {
        return this.replayIndex;
    }

    public int getFileSize() {
        return (int) this.replayFile.length();
    }

    @Override
    public synchronized void sendConsoleCommand(String command) {
    }

    @Override
    public synchronized String getServerString() {
        return "replay file: " + this.replayFile.getPath() + " (" + ServerInterface.binaryUnits(this.replayFile.length()) + ")";
    }

    @Override
    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }

    @Override
    public void run() {
        while (true) {
            if (this.keepReplaying && !this.paused) {
                try {
                    long nextMessageTime = this.dataInput.readLong();
                    long nextDelay = nextMessageTime - this.replayIndex;
                    if (nextDelay > 0) try {
                        Thread.sleep(nextDelay);
                    } catch (InterruptedException ie) {
                    }
                    this.replayIndex = nextMessageTime;
                } catch (EOFException eofe) {
                    this.closeConnection();
                    continue;
                } catch (IOException ioe) {
                    System.err.println(ioe.getLocalizedMessage());
                    ioe.printStackTrace(System.err);
                    System.exit(1);
                }
                int message_length = -1;
                try {
                    message_length = this.dataInput.readInt();
                } catch (IOException ioe) {
                    System.err.println(ioe.getLocalizedMessage());
                    this.stopRunning();
                    continue;
                }
                if (message_length < 1) {
                    this.stopRunning();
                    continue;
                }
                byte type = (byte) EventManager.MESSAGE_TYPE_UNKNOWN;
                try {
                    type = this.dataInput.readByte();
                } catch (IOException ioe) {
                    this.stopRunning();
                    continue;
                }
                final byte[] message = new byte[message_length - 1];
                int k = 0, read = 0;
                while (read < message.length) {
                    try {
                        k = this.dataInput.read(message, read, message.length - read);
                        if (k == -1) {
                            this.stopRunning();
                            continue;
                        }
                        if (k == 0) {
                            System.err.println("No data read...");
                            continue;
                        }
                        read += k;
                    } catch (SocketTimeoutException ste) {
                        continue;
                    } catch (IOException ioe) {
                        this.stopRunning();
                        break;
                    }
                }
                if (read < message.length) {
                    this.stopRunning();
                    continue;
                }
                this.receiveBPS.updateUnits(message.length + 5);
                this.bytesRead += (message.length + 13);
                switch(type) {
                    case EventManager.MESSAGE_TYPE_CONSOLE:
                        try {
                            this.fireServerEvent(new CommandResponseEvent(this, new String(message, "ASCII")));
                        } catch (UnsupportedEncodingException uee) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_LOCATION:
                        if (!this.handleLocation(message)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_FINGERPRINT_MEAN_GZIP:
                        if (!this.handleFingerprint(message, EventManager.MESSAGE_TYPE_FINGERPRINT_MEAN_GZIP)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_FINGERPRINT_STDEV_GZIP:
                        if (!this.handleFingerprint(message, EventManager.MESSAGE_TYPE_FINGERPRINT_STDEV_GZIP)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_XML_GZIP:
                        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
                        try {
                            GZIPInputStream unzipStream = new GZIPInputStream(new ByteArrayInputStream(message));
                            byte[] buffer = new byte[1024];
                            int readGZ = 0;
                            while ((readGZ = unzipStream.read(buffer, 0, buffer.length)) > 0) {
                                uncompressed.write(buffer, 0, readGZ);
                            }
                        } catch (IOException ioe) {
                            System.err.println("Couldn't decompress XML.");
                            break;
                        }
                        if (uncompressed.size() == 0) {
                            System.err.println("No data decompressed.");
                            break;
                        }
                        try {
                            this.handleXML(new String(uncompressed.toByteArray(), "ASCII"));
                        } catch (UnsupportedEncodingException uee) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_XML:
                        try {
                            this.handleXML(new String(message, "ASCII"));
                        } catch (UnsupportedEncodingException uee) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_STATISTICS_GZIP:
                        ByteArrayOutputStream uncompressedStat = new ByteArrayOutputStream();
                        try {
                            GZIPInputStream unzipStream = new GZIPInputStream(new ByteArrayInputStream(message));
                            byte[] buffer = new byte[1024];
                            int readGZ = 0;
                            while ((readGZ = unzipStream.read(buffer, 0, buffer.length)) > 0) {
                                uncompressedStat.write(buffer, 0, readGZ);
                            }
                        } catch (IOException ioe) {
                            System.err.println("Couldn't decompress XML.");
                            break;
                        }
                        if (uncompressedStat.size() == 0) {
                            System.err.println("No data decompressed.");
                            break;
                        }
                        if (!this.handleStatistics(uncompressedStat.toByteArray())) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    case EventManager.MESSAGE_TYPE_HUB_CONNECT:
                        if (!this.handleHubConnection(message)) {
                            this.stopRunning();
                            continue;
                        }
                        break;
                    default:
                        this.stopRunning();
                        continue;
                }
            } else {
                if (!this.paused) this.closeConnection();
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException ie) {
                }
            }
        }
    }
}
