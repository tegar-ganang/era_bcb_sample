package net.sf.jtmt.concurrent.blitz;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.rmi.RMISecurityManager;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.space.JavaSpace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Master process. There should be only one of these in the system.
 * This process contains two threads - a feeder thread and a writer
 * thread. The Feeder thread feeds work into the system, optionally 
 * (not implemented yet) scattering the work into multiple JavaSpaces 
 * across multiple machines. The writer thread will write the results
 * of the processing to local disk, optionally (not implemented yet) 
 * gathering all the results from multiple JavaSpaces across multiple 
 * machines. The two threads communicate with each other in order to
 * avoid overloading the JavaSpace with too many jobs.
 * @author Sujit Pal
 * @version $Revision: 51 $
 */
public class Master {

    private final Log log = LogFactory.getLog(getClass());

    private static final long MAX_ENTRIES_TO_FEED = 50;

    private static final long ENTRIES_IN_SPACE_HWM = 5;

    private static final long ENTRIES_IN_SPACE_LWM = 2;

    private static final long RETRY_INTERVAL_MILLIS = 5000L;

    private boolean shouldTerminate = false;

    private long numEntriesSent = 0L;

    private long numEntriesReceived = 0L;

    public void scatterGather() throws Exception {
        Thread feederThread = new Thread(new Runnable() {

            public void run() {
                try {
                    Feeder feeder = new Feeder();
                    for (; ; ) {
                        if (shouldTerminate) {
                            break;
                        }
                        if (numEntriesSent > MAX_ENTRIES_TO_FEED) {
                            break;
                        }
                        if (isSpaceAboveHighWaterMark()) {
                            for (; ; ) {
                                log.info("Space full, pausing");
                                pause(RETRY_INTERVAL_MILLIS);
                                if (isSpaceBelowLowWaterMark()) {
                                    break;
                                }
                            }
                        }
                        try {
                            feeder.process();
                        } catch (Exception e) {
                            log.warn("Feeder process error, retrying...", e);
                            pause(RETRY_INTERVAL_MILLIS);
                            continue;
                        }
                        numEntriesSent++;
                    }
                } catch (Exception e) {
                    log.error("Exception creating feeder process", e);
                }
            }
        });
        Thread writerThread = new Thread(new Runnable() {

            public void run() {
                try {
                    WriteProcessor writer = new WriteProcessor();
                    for (; ; ) {
                        if (shouldTerminate) {
                            break;
                        }
                        try {
                            writer.process();
                        } catch (Exception e) {
                            log.warn("Error processing write, retrying...", e);
                            pause(RETRY_INTERVAL_MILLIS);
                            continue;
                        }
                        numEntriesReceived++;
                    }
                    writer.destroy();
                } catch (Exception e) {
                    log.error("Exception creating collector process", e);
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            public void run() {
                shouldTerminate = true;
            }
        }));
        feederThread.start();
        writerThread.start();
        feederThread.join();
        writerThread.join();
    }

    private boolean isSpaceAboveHighWaterMark() {
        return (numEntriesSent - numEntriesReceived > ENTRIES_IN_SPACE_HWM);
    }

    private boolean isSpaceBelowLowWaterMark() {
        return (numEntriesSent - numEntriesReceived < ENTRIES_IN_SPACE_LWM);
    }

    public void pause(long intervalMillis) {
        try {
            Thread.sleep(intervalMillis);
        } catch (InterruptedException e) {
            log.info("Pause Interrupted");
        }
    }

    /**
   * Models the feeder processing unit.
   */
    private class Feeder extends AbstractProcessor {

        private JavaSpace space;

        private int currentIndex = 0;

        public Feeder() throws Exception {
            super();
            this.space = getSpace();
        }

        @Override
        public void process() throws Exception {
            Document doc = new Document();
            doc.url = currentIndex + ".html";
            doc.status = Document.Status.New;
            log.info("Feeding " + doc);
            space.write(doc, null, Lease.FOREVER);
            currentIndex++;
        }
    }

    /**
   * Models the Writer processing unit.
   */
    private class WriteProcessor extends AbstractProcessor {

        private JavaSpace space;

        private Document template;

        private PrintWriter writer;

        public WriteProcessor() throws Exception {
            super();
            this.space = getSpace();
            this.template = getTemplate(Document.Status.Indexed);
            this.writer = new PrintWriter(new FileWriter("/tmp/docs.txt"), true);
        }

        @Override
        public void process() throws Exception {
            Transaction tx = null;
            try {
                tx = createTransaction();
                Document doc = (Document) space.take(template, tx, Lease.FOREVER);
                log.info("Writing " + doc);
                doc.status = Document.Status.Written;
                writeToFile(writer, doc);
                tx.commit();
            } catch (Exception e) {
                if (tx != null) {
                    tx.abort();
                }
                throw e;
            }
        }

        public void destroy() {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }

        private void writeToFile(PrintWriter writer, Document doc) {
            writer.println(doc.toString());
            writer.flush();
        }
    }

    /**
   * This is how we are called.
   */
    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
        try {
            Master master = new Master();
            master.scatterGather();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
