package org.qctools4j.clients.misc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.jqc.QcBug;
import org.junit.BeforeClass;
import org.junit.Test;
import org.qctools4j.IArgsConstants;
import org.qctools4j.IQcConnection;
import org.qctools4j.QcConnectionFactory;
import org.qctools4j.clients.AbstractClientTest;
import org.qctools4j.exception.QcException;
import org.qctools4j.model.metadata.Domain;
import org.qctools4j.model.metadata.Project;
import org.qctools4j.utils.LoggerFactory;

/**
 * Sample test to check login, connection, disconnection...
 * 
 * @author tszadel
 */
public class ConnectionTest {

    /** The logger. **/
    private static final Log log = LoggerFactory.getLog(ConnectionTest.class);

    /** The input PROPERTIES. */
    protected static final Properties PROPERTIES = new Properties();

    /**
	 * Init the PROPERTIES.
	 * 
	 * @throws IOException Error loading the PROPERTIES.
	 * @throws QcException QC Error.
	 */
    @BeforeClass
    public static final void init() throws IOException, QcException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing connection PROPERTIES...");
        }
        final File lFile = new File(System.getProperty("user.home") + "/.qctools4j", "connection.properties").getCanonicalFile();
        lFile.getParentFile().mkdirs();
        if (!lFile.exists()) {
            lFile.createNewFile();
            final String lMsg = "The connection PROPERTIES has not been set. Please fill it here:\n" + lFile;
            log.fatal(lMsg);
            final BufferedInputStream lIn = new BufferedInputStream(AbstractClientTest.class.getClassLoader().getResourceAsStream("default_connection.properties"));
            final BufferedOutputStream lOut = new BufferedOutputStream(new FileOutputStream(lFile));
            int i;
            while ((i = lIn.read()) != -1) {
                lOut.write(i);
            }
            lOut.flush();
            lOut.close();
            lIn.close();
            throw new QcException(lMsg);
        }
        final InputStream lIn = new BufferedInputStream(new FileInputStream(lFile));
        try {
            PROPERTIES.load(lIn);
        } finally {
            lIn.close();
        }
    }

    /**
	 * Check multiple connections.
	 * 
	 * @throws QcException QcError.
	 */
    @Test
    public void checkMultiConnections() throws QcException {
        log.info("Testing multiple connections");
        final IQcConnection lCon = QcConnectionFactory.createConnection(PROPERTIES.getProperty(IArgsConstants.SERVER));
        List<Domain> lDomains;
        try {
            lCon.login(PROPERTIES.getProperty(IArgsConstants.USER), PROPERTIES.getProperty(IArgsConstants.PASSWORD));
            lDomains = lCon.getDomainList();
        } finally {
            lCon.disconnect();
        }
        if (log.isDebugEnabled()) {
            log.debug("Now login...");
        }
        final Set<Thread> lList = new HashSet<Thread>();
        for (final Domain lDomain : lDomains) {
            for (final Project lProject : lDomain.getProjects()) {
                final Thread lThread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            if (log.isDebugEnabled()) {
                                log.debug("Now login into project " + lProject.getName());
                            }
                            final IQcConnection lCon = QcConnectionFactory.createConnection(PROPERTIES.getProperty(IArgsConstants.SERVER));
                            try {
                                lCon.connect(PROPERTIES.getProperty(IArgsConstants.USER), PROPERTIES.getProperty(IArgsConstants.PASSWORD), lDomain.getName(), lProject.getName());
                                ConnectionTest.this.sleep(2);
                                final Collection<QcBug> lBugs = lCon.getBugClient().getBugs();
                                log.info(lBugs.size() + " bug(s) found in project " + lProject.getName());
                            } finally {
                                ConnectionTest.this.sleep(2);
                                lCon.disconnect();
                            }
                        } catch (final QcException e) {
                            log.warn(e.getMessage(), e);
                        }
                    }
                };
                lList.add(lThread);
                lThread.start();
            }
        }
        while (!lList.isEmpty()) {
            final Iterator<Thread> lIter = lList.iterator();
            while (lIter.hasNext()) {
                if (!lIter.next().isAlive()) {
                    lIter.remove();
                }
            }
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
            }
        }
        log.info("OK");
    }

    /**
	 * Test connect/disconnect.
	 * 
	 * @throws QcException QcError.
	 */
    @Test
    public void connectDisconnect() throws QcException {
        log.info("Testing connection/Disconnection");
        final IQcConnection lConnection = QcConnectionFactory.createConnection(PROPERTIES.getProperty(IArgsConstants.SERVER));
        sleep(2);
        lConnection.disconnect();
        log.info("OK");
    }

    /**
	 * Test connection/disconnection to all available projects.
	 * 
	 * @throws QcException QcError.
	 */
    @Test
    public void connectDisconnectAvailableProjects() throws QcException {
        log.info("Testing connection/disconnection to all available projects.");
        final IQcConnection lConnection = QcConnectionFactory.createConnection(PROPERTIES.getProperty(IArgsConstants.SERVER));
        lConnection.login(PROPERTIES.getProperty(IArgsConstants.USER), PROPERTIES.getProperty(IArgsConstants.PASSWORD));
        final List<Domain> lDomainList = lConnection.getDomainList();
        lConnection.disconnect();
        for (final Domain lDomain : lDomainList) {
            log.info("Connection to domain " + lDomain.getName());
            for (final Project lPrj : lDomain.getProjects()) {
                log.info("Connection to project " + lPrj.getName());
                lConnection.connect(PROPERTIES.getProperty(IArgsConstants.USER), PROPERTIES.getProperty(IArgsConstants.PASSWORD), lDomain.getName(), lPrj.getName());
                sleep(1);
                lConnection.disconnect();
                log.info("==> OK");
            }
        }
        log.info("OK");
    }

    /**
	 * Test connection/disconnection to a project.
	 * 
	 * @throws QcException QcError.
	 */
    @Test
    public void connectDisconnectProject() throws QcException {
        log.info("Testing connection/disconnection to a project.");
        final IQcConnection lConnection = QcConnectionFactory.createConnection(PROPERTIES.getProperty(IArgsConstants.SERVER));
        lConnection.connect(PROPERTIES.getProperty(IArgsConstants.USER), PROPERTIES.getProperty(IArgsConstants.PASSWORD), PROPERTIES.getProperty(IArgsConstants.DOMAIN), PROPERTIES.getProperty(IArgsConstants.PROJECT));
        sleep(2);
        lConnection.disconnect();
        log.info("OK");
    }

    /**
	 * Test the domains.
	 * 
	 * @throws QcException QC error.
	 */
    @Test
    public void getDomainList() throws QcException {
        final IQcConnection lCon = QcConnectionFactory.createConnection(PROPERTIES.getProperty(IArgsConstants.SERVER));
        try {
            lCon.login(PROPERTIES.getProperty(IArgsConstants.USER), PROPERTIES.getProperty(IArgsConstants.PASSWORD));
            final List<Domain> lDomList = lCon.getDomainList();
            for (final Domain lDomain : lDomList) {
                System.out.println(lDomain);
            }
        } finally {
            lCon.disconnect();
        }
    }

    /**
	 * Waiting before some operations to avoid QC deadlock...
	 * 
	 * @param pSeconds The number of seconds to wait.
	 */
    private void sleep(final int pSeconds) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Waiting " + pSeconds + " s");
            }
            Thread.sleep(pSeconds * 1000);
        } catch (final InterruptedException e) {
        }
    }
}
