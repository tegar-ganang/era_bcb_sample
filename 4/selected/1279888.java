package org.jude.server;

import java.util.*;
import java.net.*;
import java.io.*;
import org.jude.client.*;
import org.jude.client.logger.*;
import org.jude.client.db.*;
import org.jude.simplelogic.*;

/**
 * <p> The LogginServer waits new connections from clients.
 *     It associate to each user a ServerDatabase. It runs on the server side.
 * <p> LogginServer is the interface of the User authenticator server.  
 * <p> The comunication is performed through Socket. A previous implementation used RMI but was too slow
 *     so I prefer a more lightweight implementation. 
 * 
 * @author Massimo Zaniboni
 * @version $Revision: 1.2 $ 
 */
public class LogginServer extends Thread {

    protected XSBInteractionManager xsbManager;

    protected long serverKey;

    protected long mainKey;

    protected ServerSocket serverSocket = null;

    protected int portNumber;

    protected String outputDirectory;

    protected DiagnosticLogger logger = null;

    /**
     * Constructor. 
     */
    public LogginServer(int port, String outDirectory, int mainKey, XSBInteractionManager xsb, DiagnosticLogger l) {
        super();
        this.logger = l;
        this.mainKey = mainKey;
        this.portNumber = port;
        this.outputDirectory = outDirectory;
        this.xsbManager = xsb;
    }

    public void run() {
        try {
            serverSocket = new ServerSocket(portNumber);
            serverSocket.setSoTimeout(0);
            getLogger().addMessage("Starting LogginServer, it accepts clients at address " + serverSocket.getInetAddress().getHostAddress() + ":" + String.valueOf(portNumber));
            getLogger().addMessage("Jude Main Database read and write files in the directory " + getKnowledgeBaseDirectory());
        } catch (Exception ex) {
            getLogger().addErrorMessage(ex);
        }
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                getLogger().addDebugMessage("Received connection request from: " + clientSocket.getInetAddress());
                ServerDatabase server = new ServerDatabase(this, clientSocket, getXSBManager(), getKnowledgeBaseDirectory(), getLogger());
                server.start();
            } catch (Exception ex) {
                getLogger().addErrorMessage(ex);
            }
        }
    }

    public DiagnosticLogger getLogger() {
        return logger;
    }

    public CompilationEnviroment getEnviroment() {
        return getXSBManager().getEnviroment();
    }

    public XSBInteractionManager getXSBManager() {
        return xsbManager;
    }

    protected String getKnowledgeBaseDirectory() {
        return outputDirectory;
    }
}
