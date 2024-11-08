package com.nimium.spizd;

import java.io.*;
import javax.mail.*;
import java.util.*;
import java.net.*;
import org.vrspace.util.*;

/**
Connector thread implementation: connection contains all the data required to
connect, and calls appropriate connectors.
@see Runner
@see Connector
*/
public class Connection implements Runnable {

    public Runner runner;

    public String host;

    public String login;

    public String password;

    /** line read from dictionary/urllist file */
    public String line;

    /** connector class */
    public Class cls;

    public static boolean debug = false;

    public Connection(Runner runner, String host, String line, String login, String password, Class connectorClass) {
        this.runner = runner;
        this.host = host;
        this.line = line;
        this.login = login;
        this.password = password;
        this.cls = connectorClass;
    }

    /**
  Starts a Connector and processes it's results.
  */
    public void run() {
        if (runner.active) {
            runner.curThreads++;
            if (runner.curThreads > runner.totThreads) runner.totThreads = runner.curThreads;
            if (debug) System.out.println("Attempting connection to " + this.host + " as " + this.login + "/" + this.password);
            Connector conn = null;
            try {
                conn = (Connector) cls.newInstance();
                conn.init(this);
                runner.curConn++;
                if (runner.curConn > runner.totConn) runner.totConn = runner.curConn;
                conn.connect();
                if (runner.verboseConnect) System.out.println("CONNECTED: " + conn.getLogin() + "/" + conn.getPassword());
                boolean loggedIn = conn.login();
                if (runner.verboseLogin) System.out.println("LOGGED IN: " + loggedIn);
                conn.close();
                runner.curConn--;
            } catch (InstantiationException ie) {
                runner.active = false;
                System.out.println("FATAL: Startup failed: " + ie);
            } catch (IllegalAccessException ie) {
                runner.active = false;
                System.out.println("FATAL: Startup failed: " + ie);
            } catch (AuthenticationFailedException afe) {
            } catch (MessagingException me) {
                if (runner.verboseFail) System.out.println("Startup of thread " + runner.curThreads + " failed: " + me);
                if (runner.decreaseThreads) runner.maxThreads = runner.curThreads - 1;
                if (me.getCause() instanceof javax.net.ssl.SSLHandshakeException || me.getCause() instanceof com.sun.mail.iap.ProtocolException) {
                    System.out.println("\nThis may mean server sent self-signed certificate, run bin/installcert.sh\n");
                }
            } catch (UnknownHostException uhe) {
                runner.active = false;
                System.out.println("FATAL: Startup failed: " + uhe);
            } catch (NoRouteToHostException nrthe) {
                System.out.println("FATAL: Startup of thread " + runner.curThreads + " failed: " + nrthe);
                runner.active = false;
                runner.curConn--;
            } catch (SocketException se) {
                if (runner.verboseFail) System.out.println("Startup of thread " + runner.curThreads + " failed: " + se);
                if (runner.decreaseThreads) runner.maxThreads = runner.curThreads - 1;
                runner.curConn--;
            } catch (RuntimeException re) {
                System.out.println("FATAL: Startup of thread " + runner.curThreads + " failed: " + re);
                runner.active = false;
                throw re;
            } catch (SocketTimeoutException ste) {
                if (runner.verboseFail) System.out.println("Execution of thread " + runner.curThreads + " failed: " + ste);
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                }
            } catch (Exception e) {
                if (runner.verboseFail) System.out.println("Startup of thread " + runner.curThreads + " failed: " + e);
                if (debug) e.printStackTrace(System.out);
                runner.curConn--;
            }
            runner.curThreads--;
        }
    }
}
