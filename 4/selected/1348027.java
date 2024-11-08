package org.tanso.ts.fts;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Socket;
import org.tanso.fountain.core.eventmodel.GenericContainer;
import org.tanso.fountain.core.eventmodel.GenericEvent;
import org.tanso.fountain.core.eventmodel.GenericEventFilter;
import org.tanso.fountain.util.PathUtil;
import org.tanso.ts.base.GenericSocketServer;
import org.tanso.ts.base.SocketProtocol;

/**
 * The implementation for FTS.
 * 
 * @author Haiping Huang
 */
public class FTSImpl extends GenericSocketServer implements FileTransportService {

    /**
	 * The call back method's name
	 */
    private static final String METHOD_ONNEWFILE = "onNewFile";

    private FileTransportServiceProvider parent;

    /**
	 * Buffer size for sending file. Default is 8192
	 */
    private int bufferSize = 8192;

    private String basePath = "./";

    private FTSContainer container = new FTSContainer();

    private void info(String messgage) {
        if (null != parent) {
            parent.logger.info(messgage);
        } else {
            System.out.println(messgage);
        }
    }

    private void warn(String messgage) {
        if (null != parent) {
            parent.logger.warn(messgage);
        } else {
            System.out.println(messgage);
        }
    }

    private void error(String messgage) {
        if (null != parent) {
            parent.logger.error(messgage);
        } else {
            System.err.println(messgage);
        }
    }

    /**
	 * Protected constructor for FTSImpl
	 * 
	 */
    protected FTSImpl(FileTransportServiceProvider parent, String basePath) {
        this.parent = parent;
        if (basePath != null) {
            changeBasePath(basePath);
        }
    }

    public void changeBasePath(String basePath) {
        File dir = new File(basePath);
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                return;
            }
        }
        this.basePath = PathUtil.expandPath(basePath, true);
        info("[FTS Service] Save path: " + this.basePath);
    }

    public String getBasePath() {
        return this.basePath;
    }

    public void setBufferSize(int bufferSize) {
        if (bufferSize > 0) {
            this.bufferSize = bufferSize;
        }
    }

    public boolean sendFile(String filePath, String targetIP, int targetPort) {
        File file = new File(filePath);
        try {
            info("[FTS] Prepare to transport file: " + file.getCanonicalPath());
            if (!file.isFile()) {
                error("[FTS] Illegal file: " + file.getCanonicalPath());
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Socket client = super.connectToTCP(targetIP, targetPort);
        if (null == client) {
            warn("[FTS] Connect to: " + targetIP + ":" + targetPort + " failed!");
            return false;
        }
        try {
            DataInputStream fis = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            byte[] buffer = new byte[this.bufferSize];
            String fileName = file.getName();
            dos.writeShort(fileName.length());
            dos.writeChars(fileName);
            dos.writeLong(file.length());
            while (true) {
                int readByte = 0;
                readByte = fis.read(buffer);
                if (readByte == -1) {
                    break;
                }
                dos.write(buffer, 0, readByte);
            }
            dos.flush();
            dos.close();
            fis.close();
            client.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onRecvTCP(Socket clientSocket) {
        if (null != parent) {
            parent.getThreadPoolService().runTask(new FileReceiver(clientSocket));
        } else {
            new Thread(new FileReceiver(clientSocket)).start();
        }
    }

    @Override
    protected void onRecvUDP(DatagramPacket ip) {
        throw new RuntimeException("FTS Doesn't support UDP!");
    }

    public boolean listen(String basePath, int port) {
        if (null != basePath) {
            this.changeBasePath(basePath);
        }
        if (super.listen(SocketProtocol.TCP, port)) {
            info("[FTS] FTS listening on " + port);
            return true;
        } else {
            error("[FTS] FTS open port " + port + " failed!");
            return false;
        }
    }

    private void notifyListeners(FTSEvent e) {
        this.container.notifyListeners(e);
    }

    public void addFTSListener(FTSListener fl, GenericEventFilter filter) {
        this.container.addEventListerner(fl, filter);
    }

    public void removeFTSListener(FTSListener fl) {
        this.container.removeEventListener(fl);
    }

    /**
	 * Inner class for receiving file.
	 * 
	 * @author Haiping Huang
	 */
    private class FileReceiver implements Runnable {

        private Socket clientSocket;

        private FileReceiver(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            String storePath = null;
            String fileName = null;
            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                int nameLen = dis.readShort();
                if (nameLen == -1) {
                    return;
                }
                char c;
                StringBuffer sb = new StringBuffer(200);
                for (long i = 0; i < nameLen; i++) {
                    c = dis.readChar();
                    sb.append(c);
                }
                fileName = sb.toString();
                sb.insert(0, FTSImpl.this.basePath);
                storePath = sb.toString();
                DataOutputStream fileOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(storePath)));
                long len = dis.readLong();
                info("[FTS] Incoming file- Storage full path:" + storePath + "| Total Len: " + len + " byte(s)\n");
                byte[] buf = new byte[FTSImpl.this.bufferSize];
                while (true) {
                    int read = 0;
                    if (dis != null) {
                        read = dis.read(buf);
                    }
                    if (read == -1) {
                        break;
                    }
                    fileOut.write(buf, 0, read);
                }
                buf = null;
                dis.close();
                fileOut.close();
                this.clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FTSEvent ftsEvent = FTSEvent.createFTSEvent(this, FTSImpl.this.basePath, fileName);
            FTSImpl.this.notifyListeners(ftsEvent);
        }
    }

    private class FTSContainer extends GenericContainer {

        @Override
        public String getHandlerMethodName(GenericEvent e) {
            return FTSImpl.METHOD_ONNEWFILE;
        }
    }

    public FileTransportService getNewServiceInstance(Object requestor) {
        if (null != parent) {
            return (FileTransportService) parent.getService(requestor, "org.tanso.ts.fts.FileTransportService");
        } else {
            return FTSFactory.getInstance().createFTSInstance();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        this.container.dispose();
    }

    /**
	 * Send noop message to set the thread quit immediately
	 */
    public void stop() {
        super.stop();
        if (getMode() == SocketProtocol.TCP) {
            Socket connection = connectToTCP("localhost", getPort());
            if (connection == null) {
                System.err.println("Warning: stop connection failed!");
            } else {
                try {
                    DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                    dos.writeShort(-1);
                    dos.flush();
                    dos.close();
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
