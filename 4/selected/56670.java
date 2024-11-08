package com.serena.xmlbridge.adapter.p4.client;

import java.io.*;
import java.util.*;

/**
 * This class handles the Java Native Interface to the P4 libraries.
 *
 * @author <a href="mailto:david@markley.cc">David Markley</a>
 * @version $Date: 2001/11/05 $ $Revision: #1 $
 */
public class P4JNI extends Thread {

    private String cmd[];

    private Object listener;

    private Env environ;

    private static boolean valid = false;

    private boolean has_pipes = false;

    private BufferedReader in_reader;

    private PipedWriter in_writer;

    private PipedReader out_reader;

    private BufferedWriter out_writer;

    static {
        try {
            System.loadLibrary("perforce");
            valid = true;
        } catch (UnsatisfiedLinkError err) {
            valid = false;
        }
    }

    private native synchronized void exec(String[] cmd, Object listener, String port, String user, String password, String client);

    public P4JNI() {
        setupPipes();
    }

    private void setupPipes() {
        if (has_pipes) return;
        try {
            in_writer = new PipedWriter();
            in_reader = new BufferedReader(new PipedReader(in_writer));
            out_reader = new PipedReader();
            out_writer = new BufferedWriter(new PipedWriter(out_reader));
            has_pipes = true;
        } catch (IOException ex) {
        }
    }

    public static boolean isValid() {
        return valid;
    }

    public boolean isPiped() {
        return has_pipes;
    }

    public BufferedReader getReader() {
        return in_reader;
    }

    public BufferedWriter getWriter() {
        return out_writer;
    }

    public void runCommand(Object listener, String[] cmd, Env environ) {
        this.listener = listener;
        this.cmd = cmd;
        this.environ = environ;
        start();
    }

    public synchronized void in_write(char[] cbuf, int off, int len) {
        try {
            in_writer.write(cbuf, off, len);
        } catch (IOException ex) {
        }
    }

    public synchronized void in_close() {
        try {
            in_writer.close();
        } catch (IOException ex) {
        }
    }

    public synchronized void in_flush() {
        try {
            in_writer.flush();
        } catch (IOException ex) {
        }
    }

    public synchronized int out_read(char[] cbuf, int off, int len) {
        try {
            return out_reader.read(cbuf, off, len);
        } catch (IOException ex) {
            return -1;
        }
    }

    public void run() {
        exec(cmd, listener, environ.getPort(), environ.getUser(), environ.getPassword(), environ.getClient());
    }

    public static void main(String[] args) {
        String cmd[] = { "p4", "dirs", "//depot/pd/ttg/%1" };
        String cmd2[] = { "p4", "fstat", "//depot/pd/ttg/..." };
        String cmd3[] = { "p4", "client", "-i" };
        if (!P4JNI.isValid()) {
            System.err.println("No Perforce shared library available.");
            System.exit(-1);
        }
        String txt = "Client: dmarkley-testit\nOwner:  vss\nDescription:\n\tCreated by JNI interface.\n\nRoot:   d:\\foobarbaz\n\nOptions:        noallwrite noclobber compress crlf locked nomodtime\n\nView:\n\t//depot/pd/ttg/... //dmarkley-testit/ttg/...\n\n";
        char foo[] = txt.toCharArray();
        P4JNI tmpA = new P4JNI();
        P4JNI tmpB = new P4JNI();
        Env myEnv = new Env();
        BufferedReader br;
        BufferedWriter bw;
        String l;
        myEnv.setPort("localhost:1666");
        myEnv.setUser("vss");
        myEnv.setPassword("Blows");
        myEnv.setClient("vss-dmarkley");
        tmpA.runCommand(tmpB, cmd3, myEnv);
        br = tmpB.getReader();
        bw = tmpB.getWriter();
        try {
            bw.write(foo, 0, foo.length);
            bw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            while (null != (l = br.readLine())) {
                System.out.println("LINE: " + l);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
