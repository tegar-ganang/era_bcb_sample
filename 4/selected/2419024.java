package org.tripcom.api.execution;

import java.util.Random;
import net.jini.core.transaction.Transaction;
import net.jini.space.JavaSpace;
import org.apache.log4j.Logger;
import org.tripcom.api.controller.BusConnectionManager;
import org.tripcom.api.controller.SystemBusArea;
import org.tripcom.integration.entry.External;

/**
 * This class is used to write a entry into the systembus
 * 
 * @author Jan-Ole Christian
 *
 */
public class WriteToBusThread extends Thread {

    private static Logger log = Logger.getLogger(WriteToBusThread.class);

    private static JavaSpace systemBus = null;

    public External entry;

    public long timeout = 2000;

    public Transaction javaSpaceTransaction;

    public WriteToBusThread(External entry) {
        this(entry, 2000, null);
    }

    public WriteToBusThread(External entry, long timeOut) {
        this(entry, timeOut, null);
    }

    public WriteToBusThread(External entry, long timeOut, Transaction javaspacetransaction) {
        this.javaSpaceTransaction = javaspacetransaction;
        this.timeout = timeOut;
        if (systemBus == null) {
            systemBus = (JavaSpace) BusConnectionManager.getSharedInstance().getBus(SystemBusArea.INCOMING);
        }
        this.entry = entry;
        this.setName("WriteToBusThread OpId:" + entry.operationID);
        this.start();
    }

    public void run() {
        boolean done = false;
        long startTime = System.currentTimeMillis();
        while ((startTime + timeout) > (System.currentTimeMillis())) {
            Random rand = new Random();
            try {
                if (systemBus == null) {
                    log.error("WriteToBusThread: SystemBus null");
                    throw new java.lang.RuntimeException("WriteToBusThread: SystemBus is still null.");
                }
                systemBus.write(entry, javaSpaceTransaction, Long.MAX_VALUE);
                log.info("WriteToBusThread: wrote: " + entry.toString());
                done = true;
                break;
            } catch (Exception e) {
                try {
                    wait(rand.nextInt((int) timeout / 3));
                } catch (Exception e1) {
                }
            }
        }
        if (!done) {
            log.error("ERROR: could not write entry into systembus. Given up");
            throw new java.lang.RuntimeException("writeToBusThread: Could not write into systembus for some reason");
        }
    }
}
