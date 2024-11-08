package jist.minisim;

import java.io.*;
import jist.runtime.JistAPI;

/**
 * Measures memory overhead of events in JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: mem_events.java,v 1.1 2006/10/21 00:03:48 lmottola Exp $
 */
public class mem_events implements JistAPI.Entity {

    /**
   * Benchmark entry point: schedule events and measure memory consumption.
   *
   * @param args command-line parameters
   */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: jist mem_events <events>");
            return;
        }
        int num = Integer.parseInt(args[0]);
        mem_events e = new mem_events();
        for (int i = 0; i < num; i++) {
            e.process();
        }
        System.gc();
        System.out.println("freemem:  " + Runtime.getRuntime().freeMemory());
        System.out.println("maxmem:   " + Runtime.getRuntime().maxMemory());
        System.out.println("totalmem: " + Runtime.getRuntime().totalMemory());
        System.out.println("used:     " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        try {
            byte[] b = new byte[5000];
            FileInputStream fin = new FileInputStream("/proc/self/status");
            int readbytes = fin.read(b);
            System.out.write(b, 0, readbytes);
        } catch (IOException ex) {
            JistAPI.end();
        }
    }

    /** dummy event to schedule. */
    public void process() {
    }
}
