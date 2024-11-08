package org.drftpd.tests;

import java.util.ArrayList;
import java.util.Collections;
import junit.framework.TestCase;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;

/**
 * This a Stress TestCase for the ConnectionManager ThreadPool.<br>
 * It tries to connect to 'localhost:2121' and hammer the daemon wiyh 100 connections.<br>
 * You can change those settings by simple code changes, not going to provide configuration files for this.<br>
 * The code depends on Jakarta Commons, check it out in <link>http://jakarta.apache.org/commons/net/</link>
 * @author fr0w
 * @version $Id: ConnectionStressTest.java 2486 2011-07-10 16:07:08Z djb61 $
 */
public class ConnectionStressTest extends TestCase {

    private static final Logger logger = Logger.getLogger(ConnectionStressTest.class);

    @SuppressWarnings("unused")
    private int failures = 0;

    private int success = 0;

    private ArrayList<Thread> list = new ArrayList<Thread>();

    public ConnectionStressTest(String fName) {
        super(fName);
    }

    public void testStress() {
        int i = 0;
        for (; i < 200; i++) {
            Thread t = new Thread(new DummyClient(this));
            list.add(t);
            t.start();
            t.setName("DummyClient-" + i);
            logger.debug("Launching DummyClient #" + i);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.fatal(e, e);
            }
        }
        assertTrue(success == i);
        Collections.reverse(list);
        int dead = 0;
        for (Thread t : list) {
            while (t.isAlive()) {
            }
            dead += 1;
        }
        assertTrue(dead == success);
    }

    public void addFailure() {
        failures += 1;
    }

    public void addSuccess() {
        success += 1;
    }
}

class DummyClient implements Runnable {

    private static FTPClientConfig ftpConfig = new FTPClientConfig(FTPClientConfig.SYST_UNIX);

    private static final Logger logger = Logger.getLogger(DummyClient.class);

    private ConnectionStressTest _sc;

    public DummyClient(ConnectionStressTest sc) {
        _sc = sc;
    }

    public void run() {
        try {
            FTPClient c = new FTPClient();
            c.configure(ftpConfig);
            logger.debug("Trying to connect");
            c.connect("127.0.0.1", 21211);
            logger.debug("Connected");
            c.setSoTimeout(5000);
            if (!FTPReply.isPositiveCompletion(c.getReplyCode())) {
                logger.debug("Houston, we have a problem. D/C");
                c.disconnect();
                throw new Exception();
            }
            if (c.login("drftpd", "drftpd")) {
                logger.debug("Logged-in, now waiting 5 secs and kill the thread.");
                _sc.addSuccess();
                Thread.sleep(5000);
                c.disconnect();
            } else {
                logger.debug("Login failed, D/C!");
                throw new Exception();
            }
        } catch (Exception e) {
            logger.debug(e, e);
            _sc.addFailure();
        }
        logger.debug("exiting");
    }
}
