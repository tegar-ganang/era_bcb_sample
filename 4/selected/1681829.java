package oxygen.tool.automationagent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.rmi.Remote;
import java.util.Properties;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.security.Permission;
import oxygen.util.*;
import java.lang.reflect.*;

/**
 * AAHandler Implementation
 * @author Ugorji
 */
public class AAHandlerImpl extends UnicastRemoteObject implements AAHandler {

    private static final SimpleDateFormat LOGDATEFMT = new SimpleDateFormat("HH:mm:ss:SSS");

    private boolean debug = true;

    private int counter = 0;

    private String filesep = System.getProperty("file.separator");

    private String pathsep = System.getProperty("path.separator");

    private String javacmd = System.getProperty("java.home") + filesep + "bin" + filesep + "java";

    private String antcmdprefix = "sh ./ant.sh ";

    public AAHandlerImpl() throws RemoteException {
        super();
    }

    /**
   * Implementation of the AAHandler interface.<br>
   * Takes the action, looks for a method by that name, pass the args to it
   * returns the result.<br>
   * All methods in here are defined to have the signature<br>
   * public String[] __action__(String[] args) throws Exception
   */
    public String[] invoke(String action, String[] args) throws RemoteException {
        try {
            Class[] params = new Class[] { String[].class };
            Method meth = getClass().getMethod(action, params);
            String[] rtn = (String[]) meth.invoke(this, new Object[] { args });
            return rtn;
        } catch (InvocationTargetException ite) {
            throw new RemoteException("AAHandlerImpl Exception executing method", ite.getTargetException());
        } catch (Throwable exc) {
            throw new RemoteException("AAHandlerImpl Exception", exc);
        }
    }

    /**
   * Shutdown the AutomationAgent process. basically does a 
   * System.exit on the AAHandlerImpl.<br>
   * Takes no arguments, returns null
   */
    public String[] shutdown(String[] args) throws Exception {
        System.out.println("Shutting down");
        ShutdownHandler.shutdown();
        return null;
    }

    /**
   * This call causes the AutomationAgent to:<br>
   * - create a ServerSocket on any available port<br>
   * - listen for data on that ServerSocket, and reads it into a file passed.<br>
   * Takes single argument of the filename to write to {filename}<br>
   * Returns the host and port of the serversocket (new String[]{host, port})<br>
   */
    public String[] setupToReceiveFileFromSocket(String[] args) throws Exception {
        String filename = args[0];
        String hostname = InetAddress.getLocalHost().getHostName();
        InetAddress ia = InetAddress.getByName(hostname);
        final File f = new File(filename);
        final ServerSocket ss = new ServerSocket(0, -1, ia);
        int port = ss.getLocalPort();
        Runnable r = AAFileWriter.getReceiveFileRunnable(ss, f);
        Thread thr = new Thread(OxygenUtils.topLevelThreadGroup(), r);
        thr.start();
        return new String[] { ia.getHostAddress(), String.valueOf(port) };
    }

    /**  
   * This call causes the AutomationAgent to:<br>
   * - writes file to server socket<br>
   * Takes arguments of {filename, host, port}<br>
   * returns null<br>
   *
   * Typically, here the client has created a serversocket, and is waiting for the
   * AAHandler to write a file into the socket for it.
   */
    public String[] writeFileToSocket(String[] args) throws Exception {
        Socket sock = null;
        FileInputStream fis = null;
        OutputStream os = null;
        try {
            String filename = args[0];
            String host = args[1];
            int port = Integer.parseInt(args[2]);
            File f = new File(filename);
            sock = new Socket(host, port);
            fis = new FileInputStream(f);
            os = sock.getOutputStream();
            byte[] b = new byte[1024];
            int bytesread = -1;
            while ((bytesread = fis.read(b, 0, b.length)) != -1) {
                os.write(b, 0, bytesread);
            }
        } finally {
            CloseUtils.close(os);
            CloseUtils.close(fis);
            CloseUtils.close(sock);
        }
        return null;
    }

    /**  
   * This call causes the AutomationAgent to:<br>
   * - execute a process<br>
   * Takes arguments of: {waitforprocesscompletion, commandline}<br>
   * returns null<br>
   */
    public String[] executeProcess(String[] args) throws Exception {
        boolean waitforprocesscompletion = false;
        if (args[0] != null) {
            waitforprocesscompletion = "true".equals(args[1]);
        }
        String commandline = args[1];
        int ctrindx = getNextCounterIndex();
        Writer os = getProcessOutputStream(commandline, ctrindx);
        Process p = Runtime.getRuntime().exec(commandline);
        OxygenUtils.sleep(100);
        ProcessHandler.handle(p, os, null, waitforprocesscompletion);
        CloseUtils.close(os);
        return null;
    }

    /**  
   * This call causes the AutomationAgent to:<br>
   * - execute an ANT command line<br>
   * - basically does sh ant.sh __antParams__<br>
   * Takes arguments of: {waitforprocesscompletion, waitformarkerfile, markerfile, commandline}<br>
   * if markerfile is -, then it uses a unique string for the marker file<br>
   * returns null
   */
    public String[] doAnt(String[] args) throws Exception {
        boolean waitforprocesscompletion = "true".equals(args[0]);
        boolean waitformarkerfile = "true".equals(args[1]);
        String markerfile = args[2];
        log("handling command: ");
        if (markerfile == null || markerfile.equals("-") || markerfile.trim().length() == 0) {
            markerfile = OxygenUtils.getUniqueID();
        }
        String argsd = args[3];
        int ctrindx = getNextCounterIndex();
        String antcommandline = antcmdprefix + " -logfile " + getProcessOutFileName(ctrindx) + " -Dmarkerfile=" + markerfile + " " + argsd;
        log("about to create process for command: " + antcommandline);
        Writer os = getProcessOutputStream(antcommandline, ctrindx);
        Process p = Runtime.getRuntime().exec(antcommandline);
        OxygenUtils.sleep(100);
        ProcessHandler.handle(p, os, null, waitforprocesscompletion);
        CloseUtils.close(os);
        if (waitformarkerfile) {
            log("waitformarkerfile ...");
            File markerfileobj = new File(markerfile + ".marker.file");
            while (!markerfileobj.exists()) {
                OxygenUtils.sleep(5000l);
            }
            log("Found marker file: " + markerfileobj + " ... will now delete it");
            markerfileobj.delete();
        }
        log("done handling command: ");
        log("");
        return null;
    }

    private synchronized int getNextCounterIndex() {
        counter++;
        return counter;
    }

    private Writer getProcessOutputStream(String antcommandline, int indx) throws Exception {
        String fname = getProcessOutFileName(indx) + ".cmdline";
        FileOutputStream fos = new FileOutputStream(fname);
        PrintWriter pw = new PrintWriter(fos);
        pw.println("=========================================================");
        pw.println("Output for command below: ");
        pw.println(antcommandline);
        pw.println("=========================================================");
        pw.println();
        pw.flush();
        return pw;
    }

    private synchronized String getProcessOutFileName(int indx) {
        String fname = "autoagentlogs/autoagent.process." + indx + ".log";
        return fname;
    }

    private void log(String s) {
        if (debug) {
            Date d = new Date();
            System.out.println("[" + LOGDATEFMT.format(d) + "] " + s);
        }
    }

    private void log() {
        log("");
    }

    /**
   * Takes arguments {registryport}
   * Starts an RMIregistry.
   * Explicitly uses the defaultsocketfactory, since that is firewall aware
   */
    public static void main(String[] argv) throws Exception {
        Registry registry;
        int registryPort = 5999;
        String hostname;
        String bindname = "AAHandler";
        System.out.println("Starting handler");
        if (argv.length > 0) {
            registryPort = Integer.parseInt(argv[0]);
        }
        if (argv.length > 1) {
            FileOutputStream fos = new FileOutputStream(argv[1]);
            PrintStream ps = new PrintStream(fos);
            System.setOut(ps);
            System.setErr(ps);
        }
        System.setSecurityManager(new SecurityManager() {

            public void checkPermission(Permission perm) {
            }
        });
        RMISocketFactory rmisf = RMISocketFactory.getDefaultSocketFactory();
        registry = LocateRegistry.createRegistry(registryPort, rmisf, rmisf);
        AAHandler clh = new AAHandlerImpl();
        registry.bind(bindname, clh);
    }
}
