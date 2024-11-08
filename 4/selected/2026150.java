package jist.minisim;

import java.io.*;
import jist.runtime.JistAPI;

/**
 * Measures infrastructure memory overhead of entities in JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: entity.java,v 1.1 2006/10/21 00:03:45 lmottola Exp $
 * @since JIST1.0
 */
public class entity implements JistAPI.Entity {

    /**
   * Benchmark entry point: create entities and measure memory consumption.
   *
   * @param args command-line parameters
   */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: jist entity <entities>");
            return;
        }
        int num = Integer.parseInt(args[0]);
        entity[] e = new entity[num];
        for (int i = 0; i < num; i++) {
            e[i] = new entity();
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
}
