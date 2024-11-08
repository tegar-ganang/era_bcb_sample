package com.gorillalogic.gosh.commands;

import com.gorillalogic.dal.*;
import com.gorillalogic.gosh.*;
import java.io.*;
import java.net.*;

/**
 * <code>RemoteShell</code> is the remote client for
 * <code>Gateway</code>.
 *
 * @author <a href="mailto:Brendan@Gosh"></a>
 * @version 1.0
 */
public class RemoteShell extends RGosh {

    public void run(String[] args) {
        try {
            doRun(args);
        } catch (IOException e) {
            System.out.println("");
            System.out.println("  GXE Server not running or not accessible.");
            System.out.println("");
        }
    }

    public void setPort(int port) {
        Gateway.setPort(port);
    }

    private void doRun(String[] args) throws IOException {
        Socket s = new Socket("localhost", Gateway.getPort());
        final PrintWriter toServer = new PrintWriter(s.getOutputStream());
        final BufferedReader fromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
        final Thread trackingThread = Thread.currentThread();
        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    PrintWriter out = new PrintWriter(System.out);
                    trackOutput(fromServer, out);
                } catch (IOException e) {
                    System.out.println("Connection terminated from server");
                } finally {
                    System.exit(0);
                }
            }
        });
        t.start();
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        for (int i = 0; i < args.length; i++) {
            toServer.println(Gateway.ARGMARKER + args[i]);
        }
        toServer.println(Gateway.READYMARKER);
        toServer.flush();
        try {
            processInput(rdr, toServer);
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null) msg = e.getClass().getName();
            System.out.println("Error from server: " + msg);
            System.out.println("Exiting from unrecoverable error");
            System.exit(-1);
        }
    }

    private void trackOutput(Reader from, PrintWriter to) throws IOException {
        do {
            int nx = from.read();
            if (nx < 0) {
                return;
            }
            char c = (char) nx;
            to.print(c);
            to.flush();
        } while (true);
    }

    private void processInput(BufferedReader in, PrintWriter out) throws IOException {
        do {
            String nextLine = in.readLine();
            out.println(nextLine);
            out.flush();
        } while (true);
    }
}
