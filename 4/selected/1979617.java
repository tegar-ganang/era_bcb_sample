package Pump;

import java.io.*;
import java.net.*;
import java.util.logging.*;

public class ClientConnectionHandler implements IfClientConnection {

    private static Logger myLogger = Logger.getLogger("Pump.ClientConnectionHandler");

    ;

    private static FileHandler myLogHandler;

    private URL targetURL;

    private Socket mySocket;

    ClientConnectionHandler(URL u) {
        try {
            myLogHandler = new FileHandler("queuelog.log");
            myLogger.addHandler(myLogHandler);
            myLogger.setLevel(Level.ALL);
        } catch (IOException e) {
            myLogger.log(Level.SEVERE, "Log filehandler error in ClientConnectionHandler");
        }
        try {
            targetURL = new URL(u.toString());
        } catch (MalformedURLException me) {
            myLogger.log(Level.FINE, "Error creating URL");
        }
        myLogger.log(Level.FINEST, "next: socket create");
        try {
            System.out.println("Connect to " + targetURL.getHost());
            mySocket = new Socket(targetURL.getHost(), 80);
        } catch (UnknownHostException ue) {
            myLogger.log(Level.FINE, "Host is unknown:" + targetURL.toString());
        } catch (ConnectException ce) {
            myLogger.log(Level.FINE, "Connect exception, connection refused by remote: " + ce.getMessage());
        } catch (IOException ie) {
            myLogger.log(Level.FINE, "Some IO exception when creating socket : " + ie.getMessage());
        } catch (SecurityException se) {
            myLogger.log(Level.FINE, "Security exception : " + se.getMessage());
        }
        if (mySocket.isBound() & mySocket.isConnected()) {
            myLogger.log(Level.FINEST, "socket create seems OK!");
        } else {
            myLogger.log(Level.FINEST, "socket create seems NOT OK!");
        }
    }

    public void setLogLevel(Level newLevel) {
        myLogger.setLevel(newLevel);
    }

    public boolean sendRequest(URL target, IfHttpRequest R) {
        boolean rValue = false;
        myLogger.log(Level.FINEST, "is about to send reuqest");
        if (mySocket.isConnected()) {
            mySocket.getChannel();
        }
        return rValue;
    }
}
