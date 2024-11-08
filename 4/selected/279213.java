package visitpc.mtightvnc.sessionplayer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import visitpc.mtightvnc.RfbProto;
import visitpc.UserOutput;

public class PlaybackServer implements Runnable {

    public static final int MAX_SESSION_COUNT = 20;

    private static int SessionCount = 0;

    private boolean playbackServerRunning;

    private ServerSocket serverSocket;

    private SessionReader sessionReader;

    private byte[] readBuffer;

    private File sessionFile;

    private UserOutput uo;

    private int port;

    /**
   * Constructor
   * 
   */
    public PlaybackServer(UserOutput uo, int port) {
        this.uo = uo;
        this.port = port;
        sessionReader = new SessionReader();
        readBuffer = new byte[65536];
    }

    /**
   * The Thread method
   */
    public void run() {
        Socket socket;
        try {
            if (port < 1) {
                serverSocket = new ServerSocket();
            } else {
                serverSocket = new ServerSocket(port);
            }
            uo.info("VNC session playback server started, using port " + getTCPPort());
            uo.info("Please connect to the above port on this machine with your VNC client.");
            playbackServerRunning = true;
            while (playbackServerRunning) {
                socket = serverSocket.accept();
                uo.info("Client connected to playback server");
                uo.info("Playing back " + sessionFile);
                class PlaybackSessionThread implements Runnable {

                    private Socket socket;

                    public PlaybackSessionThread(Socket socket) throws IOException {
                        if (PlaybackServer.SessionCount > PlaybackServer.MAX_SESSION_COUNT) {
                            throw new IOException("To many outstanding PlaybackSessionThread objects (>" + PlaybackServer.MAX_SESSION_COUNT + ")");
                        }
                        this.socket = socket;
                    }

                    public void run() {
                        try {
                            PlaybackServer.SessionCount++;
                            handle(socket);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            PlaybackServer.SessionCount--;
                        }
                    }
                }
                Thread sessionPlaybackThread = new Thread(new PlaybackSessionThread(socket));
                sessionPlaybackThread.setDaemon(true);
                sessionPlaybackThread.start();
            }
        } catch (IOException e) {
        }
    }

    /**
   * Not thread safe in that the sessionFile may be changed
   * 
   * @param sessionFile
   */
    public void setSessionFile(File sessionFile) throws IOException {
        this.sessionFile = sessionFile;
        uo.info("This server will playback " + sessionFile);
    }

    /**
   * Handle a connected socket.
   * 
   * @param socket
   */
    void handle(Socket socket) throws IOException {
        sessionReader.open(sessionFile);
        sessionReader.readHeader();
        String infoLines[] = sessionReader.infoLines();
        for (String line : infoLines) {
            uo.info(line);
        }
        DataInputStream dis = new DataInputStream(socket.getInputStream());
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.write(sessionReader.getRFBVersionMessage().getBytes());
        dis.readFully(readBuffer, 0, 12);
        dos.writeInt(1);
        int securityType = dis.readByte();
        if (securityType != RfbProto.SecTypeNone) {
            throw new IOException("VNC client requested security type " + securityType + " but we only support " + RfbProto.SecTypeNone + ".");
        }
        dos.writeShort(sessionReader.getFrameBufferWidth());
        dos.writeShort(sessionReader.getFrameBufferHeight());
        dos.write(sessionReader.getFBSServerInitMessage());
        dos.writeInt(sessionReader.getDesktopName().length());
        dos.write(sessionReader.getDesktopName().getBytes());
        byte[] postHeaderBuffer = sessionReader.getPostHeaderBuffer();
        if (postHeaderBuffer != null && postHeaderBuffer.length > 0) {
            dos.write(postHeaderBuffer);
        }
        long lastTime = -1;
        long time;
        int len;
        byte[] readBuffer;
        int availableByteCount;
        try {
            while (true) {
                sessionReader.readElement();
                time = sessionReader.getTime();
                len = sessionReader.getLen();
                readBuffer = sessionReader.getReadBuffer();
                dos.write(readBuffer, 0, len);
                if (lastTime != -1) {
                    Thread.sleep(time - lastTime);
                }
                availableByteCount = dis.available();
                if (availableByteCount > 0) {
                    dis.readFully(readBuffer, 0, availableByteCount);
                }
                lastTime = time;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (EOFException e) {
        } finally {
            try {
                dis.close();
            } catch (Exception e) {
            }
        }
        uo.info("Playback complete");
    }

    /**
   * Get the TCP IP port.
   * 
   * @return The TCP IP port.
   */
    public int getTCPPort() {
        if (serverSocket == null) {
            return -1;
        }
        return serverSocket.getLocalPort();
    }

    /**
   * Close the server
   */
    public void close() throws IOException {
        serverSocket.close();
        serverSocket = null;
    }
}
