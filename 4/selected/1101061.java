package Pretzealz.Server.ServerClient;

import Pretzealz.Server.Player.*;
import java.net.Socket;
import java.io.*;
import java.util.StringTokenizer;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.sql.*;

/**
 *
 * @author Sonis
 */
public class ServerClient extends Player implements Runnable {

    public static final int bufferSize = 1000000;

    public java.net.Socket mySock;

    public java.io.InputStream in;

    public java.io.OutputStream out;

    public byte buffer[] = null;

    public int readPtr, writePtr;

    public ServerClient(java.net.Socket s, int _playerId) {
        super(_playerId);
        mySock = s;
        try {
            in = s.getInputStream();
            out = s.getOutputStream();
        } catch (java.io.IOException ioe) {
            Derefrence.Derefrence.Derefrence.print("ScarNastics Server (1): Exception!");
            ioe.printStackTrace();
        }
        readPtr = writePtr = 0;
        buffer = buffer = new byte[bufferSize];
    }

    /**
     * We just accepted a new connection -- now let's handle this bull crap lol...
     */
    public void run() {
    }
}
