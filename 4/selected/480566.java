package org.zhouer.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Standalone {

    public static void main(final String args[]) {
        Protocol p;
        Thread a, b;
        int port;
        if (args.length < 2) {
            System.out.println("Not enough parameter!");
            return;
        }
        if (args[0].equalsIgnoreCase("telnet")) {
            if (args.length == 2) {
                port = 23;
            } else {
                port = Integer.parseInt(args[2]);
            }
            p = new Telnet(args[1], port);
        } else if (args[0].equalsIgnoreCase("ssh2")) {
            if (args.length == 2) {
                port = 22;
            } else {
                port = Integer.parseInt(args[2]);
            }
            p = new SSH2(args[1], port);
            p.setTerminalType("vt100");
        } else {
            System.out.println("Unknown protocol!");
            return;
        }
        if (p.connect() == false) {
            System.out.println("Connection error!");
            return;
        }
        a = new ForwardThread(p.getInputStream(), System.out);
        b = new ForwardThread(System.in, p.getOutputStream());
        a.start();
        b.start();
        try {
            a.join();
            b.join();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class ForwardThread extends Thread {

    InputStream is;

    OutputStream os;

    public ForwardThread(final InputStream is, final OutputStream os) {
        this.is = is;
        this.os = os;
    }

    public void run() {
        while (true) {
            try {
                this.os.write(this.is.read());
            } catch (final IOException e) {
                break;
            }
        }
    }
}
