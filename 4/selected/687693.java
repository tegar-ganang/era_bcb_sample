package org.jchains.intercept;

import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ObjectOutputStream;
import java.security.Permission;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.jchains.CORBA.*;
import org.jchains.CORBA.PermissionTransferPackage.*;
import org.jchains.internal.Util;
import org.jchains.receiver.Perm;

abstract class Strategy {

    EnvironmentEntry[] eearr = Util.propsToEnvironmentEntry(System.getProperties());

    public abstract void init() throws Exception;

    abstract void send(CPermission e);

    abstract void end();
}

class FileStrategy extends Strategy {

    FileChannel destination;

    java.io.File destFile = new java.io.File(System.getProperty("org.jchains.file", "permissions.csv"));

    ObjectOutputStream oos;

    BufferedWriter bw;

    public void init() throws Exception {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        destination = new FileOutputStream(destFile).getChannel();
        bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile)));
    }

    public void send(CPermission cp) {
        try {
            Perm perm = new Perm(System.currentTimeMillis(), cp, Util.emptyret);
            try {
                synchronized (bw) {
                    bw.write(perm.toString() + "\r\n");
                    bw.flush();
                }
            } catch (java.nio.channels.ClosedChannelException e) {
                System.out.println("NIO channel is closed, this is expected during shutdown");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    void end() {
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class CORBAStrategy extends Strategy {

    PermissionTransfer pt;

    int corbasession;

    public void init() throws Exception {
        String s = System.getProperty("org.jchains.CNameServiceIOR", "corbaloc::127.0.0.1:1050/NameService");
        String fn = System.getProperty("org.jchains.outputfile", System.currentTimeMillis() + "out.txt");
        System.out.println(s);
        ORB orb = ORB.init(new String[] {}, null);
        org.omg.CORBA.Object obj = orb.string_to_object(s);
        NamingContextExt namingcontextext = NamingContextExtHelper.narrow(obj);
        System.out.println("nc0:" + namingcontextext);
        org.omg.CORBA.Object obj1 = namingcontextext.resolve_str("PermissionTransfer");
        pt = PermissionTransferHelper.narrow(obj1);
        corbasession = pt.init(fn, eearr);
    }

    @Override
    void end() {
    }

    @Override
    void send(CPermission e) {
        pt.send(corbasession, e);
    }
}

class SocketStrategy extends Strategy {

    int socketserverport;

    Socket sock;

    ObjectOutputStream oos;

    @Override
    void end() {
    }

    @Override
    public void init() throws Exception {
        socketserverport = Integer.parseInt(System.getProperty("org.jchains.socketport", "0"));
        while (sock == null) {
            try {
                Thread.sleep(1000);
                sock = new Socket(InetAddress.getLocalHost(), socketserverport);
                oos = new ObjectOutputStream(sock.getOutputStream());
            } catch (Exception e) {
                System.out.println("e:" + e);
            }
        }
    }

    @Override
    void send(CPermission e) {
    }
}

class PermissionCache extends Hashtable<String, String> {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
}

enum mode {

    ORB, FILE, SOCKET
}

;

public class StandardEmitter implements Emitter {

    static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("JChains");

    PermissionTransfer pt;

    PermissionCache pc = new PermissionCache();

    CodeSourceForClass csc = new CodeSourceForClass();

    EnvironmentEntry[] eearr = Util.propsToEnvironmentEntry(System.getProperties());

    public StandardEmitter() {
        this('F');
    }

    public void exit() {
        strat.end();
    }

    public StandardEmitter(char e) {
        switch(e) {
            case 'F':
                strat = new FileStrategy();
                break;
            case 'O':
                strat = new CORBAStrategy();
                break;
            case 'S':
                strat = new SocketStrategy();
                break;
            default:
                strat = new FileStrategy();
                break;
        }
    }

    String bomitFilePermissions = System.getProperty("org.jchains.suppressFilePermissions", "classfiles");

    Strategy strat;

    public void init() throws Exception {
        strat.init();
    }

    static boolean bLogIt = false;

    public void emit(Class c, Permission permission, StackTraceElement[] ste) {
        String cname = c.getName();
        String target = permission.getName();
        if (cname.startsWith("sun.")) return;
        if (cname.startsWith("java.")) return;
        if (cname.startsWith("javax.")) return;
        if (permission instanceof FilePermission) {
            if (bomitFilePermissions.equals("all")) return;
            if (bomitFilePermissions.equals("classfiles") && target.endsWith(".class")) {
                return;
            }
        }
        String cs_str = csc.put(c);
        String theperm = permission.getClass().getName();
        String compstr = theperm + cs_str + target + permission.getActions();
        if (false) {
            compstr += cname;
        }
        if (!pc.containsKey(compstr)) {
            pc.put(compstr, "1");
            String[] actions = permission.getActions().split(",");
            if (bLogIt) log.info("AL:" + actions.length + ":" + permission.getActions());
            StacktraceSeqHolder stack = Util.Ste2String(ste);
            CPermission p = new CPermission(cname, theperm, cs_str, target, actions, (short) 0, stack.value);
            strat.send(p);
            p = null;
        }
    }
}
