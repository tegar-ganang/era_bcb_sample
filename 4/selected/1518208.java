package de.beeld.network.server;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.apache.log4j.Logger;
import de.beeld.network.BeeldPacket;
import de.beeld.network.NetworkMessages;
import de.beeld.util.Helper;

/**
 * The RemoteClient is responsible to read all incoming messages of the client.
 * 
 * @author Martin R&ouml;bert
 * @version $LastChangedRevision: 106 $
 * @since $HeadURL:
 *        https://beeld.svn.sourceforge.net/svnroot/beeld/src/de/beeld/
 *        network/server/RemoteClient.java $
 */
public class RemoteClient extends Thread {

    private static Logger log = Logger.getLogger(RemoteClient.class);

    private Socket clientSocket;

    private Socket pdfClientSocket;

    private ObjectInputStream oisPacketInStream = null;

    private ObjectOutputStream oosPacketOutStream = null;

    private DataOutputStream dosPdfOutStream = null;

    private BeeldPacket outgoingPacket = null;

    private byte[] byteBuffer = new byte[1024];

    private InputStream isFileIn;

    private String strSplitedFileDir;

    private File fileSplitedPDFDir;

    private boolean bolRemoteClientFlag;

    private IncomingPacketDispatcherServer dispatcher = new IncomingPacketDispatcherServer();

    /**
	 * opens all needed sockets and streams
	 * 
	 * @param packetClientSocket
	 * @param imageClientSocket
	 */
    public RemoteClient(Socket packetClientSocket, Socket imageClientSocket) {
        bolRemoteClientFlag = true;
        this.clientSocket = packetClientSocket;
        this.pdfClientSocket = imageClientSocket;
        try {
            oisPacketInStream = new ObjectInputStream(this.clientSocket.getInputStream());
        } catch (IOException e) {
            try {
                this.clientSocket.close();
            } catch (IOException e1) {
                log.error("Could not close connection\n" + e1.getMessage());
            }
            log.error("could not open instream\n" + e.getMessage());
        }
        try {
            oosPacketOutStream = new ObjectOutputStream(this.clientSocket.getOutputStream());
            dosPdfOutStream = new DataOutputStream(pdfClientSocket.getOutputStream());
        } catch (IOException e) {
            try {
                this.clientSocket.close();
                this.pdfClientSocket.close();
            } catch (IOException e1) {
                log.error("Could not close connection\n" + e1.getMessage());
            }
            log.error("could not open outstream\n" + e.getMessage());
            e.printStackTrace();
        }
        this.start();
    }

    /**
	 * Overwritten method of Thread
	 * 
	 * Dispatches all incoming packets
	 */
    @Override
    public void run() {
        while (bolRemoteClientFlag) {
            try {
                dispatcher.dispatchPacket(oisPacketInStream.readObject(), this);
            } catch (IOException e) {
                try {
                    bolRemoteClientFlag = false;
                    clientSocket.close();
                    pdfClientSocket.close();
                } catch (IOException e1) {
                    log.error("Could not close the sockets " + e.getMessage());
                }
                log.error("Could not read input of client; terminating network connection " + e.getMessage());
            } catch (ClassNotFoundException e) {
                log.error("Received class not found." + e.getMessage());
            }
        }
    }

    /**
	 * sends a message to connected client
	 * 
	 * @param message
	 * @param data
	 * @param length
	 */
    public void sendMessage(String message, String data, long length) {
        log.trace("Try to send message: " + message + " with " + data + "/" + length);
        outgoingPacket = new BeeldPacket(message, data, length);
        try {
            oosPacketOutStream.writeObject(outgoingPacket);
        } catch (IOException e) {
        }
    }

    /**
	 * sends a message to connected client
	 * 
	 * @param message
	 * @param data
	 */
    public void sendMessage(String message, String data) {
        log.trace("Try to send message: " + message);
        outgoingPacket = new BeeldPacket(message, data);
        try {
            oosPacketOutStream.writeObject(outgoingPacket);
        } catch (IOException e) {
        }
    }

    /**
	 * sends a message to connected client
	 * 
	 * @param message
	 */
    public void sendMessage(String message) {
        log.trace("Try to send message: " + message);
        outgoingPacket = new BeeldPacket(message);
        try {
            oosPacketOutStream.writeObject(outgoingPacket);
        } catch (IOException e) {
        }
    }

    /**
	 * sends a file to connected client
	 * 
	 * @param currentSlide
	 */
    public void sendSplitedPDFFile(String currentSlide) {
        String nextSlide = Helper.putNextSlideName(currentSlide);
        log.trace("Now processing " + currentSlide + " and guessing next slide " + nextSlide);
        File currentFile = new File(fileSplitedPDFDir.getAbsolutePath() + System.getProperty("file.separator") + currentSlide);
        if (goOn(currentFile)) {
            try {
                log.trace("Current file I try to send: " + currentFile.getAbsolutePath());
                isFileIn = new FileInputStream(currentFile);
                while (isFileIn.available() > 0) {
                    dosPdfOutStream.write(byteBuffer, 0, isFileIn.read(byteBuffer));
                }
                isFileIn.close();
            } catch (FileNotFoundException e) {
                log.error("Could not find the given file. " + e.getMessage());
            } catch (IOException e) {
                log.error("Could not send the file: " + currentFile.getAbsolutePath() + currentFile.getName() + " " + e.getMessage());
            }
        }
    }

    /**
	 * set dir where splited files are stored
	 * 
	 * @param dir
	 */
    public void setSplitedFileDir(String dir) {
        strSplitedFileDir = dir;
        fileSplitedPDFDir = new File(strSplitedFileDir);
        log.trace("setMethod: " + strSplitedFileDir);
    }

    /**
	 * checks, which slide is sended.
	 * 
	 * sends INITSENDFILE (first file), SENDFILE (all other files) or SFFINISH
	 * (no files to serve) to the client
	 * 
	 * @param currentFile
	 * @return
	 */
    public boolean goOn(File currentFile) {
        if (currentFile.getName().equals("slide_1.pdf")) {
            sendMessage(NetworkMessages.INITSENDFILE, "slide_1.pdf", currentFile.length());
            return true;
        } else if (!currentFile.exists()) {
            sendMessage(NetworkMessages.SFFINISH, null);
            return false;
        } else {
            sendMessage(NetworkMessages.SENDFILE, currentFile.getName(), currentFile.length());
            return true;
        }
    }
}
