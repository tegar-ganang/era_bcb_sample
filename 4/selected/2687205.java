package takatuka;

import java.io.*;
import net.tinyos.packet.*;
import net.tinyos.util.*;

/**
 * 
 * Description:
 * <p>
 * To listen serial packets arrived on a serial port.
 * This program is based on net.tinyos.tools.Listen. The main difference
 * is that it always consider that packet receive is a Java String and hence it
 * convert data portion of packet into String and prints it. Secondly, it has two
 * threads, one thread wait for the input and terminates the program if x is pressed.
 * The other thread get the packets on serial port and print the payload as string.
 * 
 * We (the TakaTuka team) are reporducing net.tinyos.tools.Listen copyrights notice below.
 * 
 * </p> 
 * <p> "Copyright (c) 2000-2003 The Regents of the University  of California.  
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Copyright (c) 2002-2003 Intel Corporation
 * All rights reserved.
 *
 * This file is distributed under the terms in the attached INTEL-LICENSE     
 * file. If you do not find these files, copies can be found by writing to
 * Intel Research Berkeley, 2150 Shattuck Avenue, Suite 1300, Berkeley, CA, 
 * 94704.  Attention:  Intel License Inquiry.
 * </p>
 * @version 1.0
 */
public class SerialDebugger extends Thread {

    private static int count = 0;

    public SerialDebugger() {
    }

    public void run() {
        PacketSource reader = BuildSource.makePacketSource();
        PrintSerialData printData = PrintSerialData.getInstanceOf();
        if (reader == null) {
            System.err.println("Invalid pTestSerialMsgacket source (check your MOTECOM environment variable)");
            System.exit(2);
        }
        try {
            reader.open(PrintStreamMessenger.err);
            while (true) {
                byte[] packet = reader.readPacket();
                printData.print(packet);
                System.out.flush();
                System.out.print("\n");
                if (reader.writePacket(packet)) {
                    System.out.println("packet sent!");
                } else {
                    System.out.println("send packet error!!!");
                }
            }
        } catch (IOException e) {
            System.err.println("Error on " + reader.getName() + ": " + e);
        }
    }

    /**
     * first start the input thread for ending the program
     * then start the thread that received packet from mote.
     */
    public void execute() {
        InputThreadClass InputThread = new InputThreadClass();
        InputThread.start();
        this.start();
    }

    public static void main(String args[]) throws Exception {
        if (args.length > 0) {
            PrintExceptions printExp = PrintExceptions.getInstanceOf();
            printExp.setFileNames(args[0], args[1], args[2]);
        }
        System.out.println("Press X to exit ");
        new SerialDebugger().execute();
    }

    /**
     * This class is used for input to terminate the program
     */
    private class InputThreadClass extends Thread {

        public boolean StopVM = false;

        public void run() {
            try {
                while (true) {
                    char input = (char) System.in.read();
                    if (input == 'X' || input == 'x') {
                        System.exit(0);
                    } else if (input == 'S' || input == 's') {
                        StopVM = !StopVM;
                    }
                }
            } catch (Exception d) {
                d.printStackTrace();
                System.exit(1);
            }
        }
    }
}
