package net.fortuna.mstor.connector.mbox;

import java.io.File;
import java.util.Properties;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import net.fortuna.mstor.StoreLifecycle;
import org.apache.commons.io.FileUtils;

/**
 * @author Ben
 * 
 *         <pre>
 * $Id: MboxStoreLifecycle.java,v 1.4 2011/02/19 07:36:01 fortuna Exp $
 * Created on 01/03/2008
 * </pre>
 * 
 */
public class MboxStoreLifecycle implements StoreLifecycle {

    private Properties sessionProps;

    private File testDir;

    private File testFile;

    private Store store;

    /**
     * @param name
     * @param sessionProps
     * @param testFile
     */
    public MboxStoreLifecycle(String name, Properties sessionProps, File testFile) {
        this.sessionProps = sessionProps;
        this.testFile = testFile;
        if (testFile != null) {
            testDir = new File(System.getProperty("java.io.tmpdir"), "mstor_test" + File.separator + name + File.separator + testFile.getName());
        } else {
            testDir = new File(System.getProperty("java.io.tmpdir"), "mstor_test" + File.separator + name);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Store getStore() {
        return store;
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() throws Exception {
        FileUtils.deleteDirectory(testDir);
    }

    /**
     * {@inheritDoc}
     */
    public void startup() throws Exception {
        FileUtils.deleteDirectory(testDir);
        if (testFile != null) {
            if (testFile.isDirectory()) {
                FileUtils.copyDirectory(testFile, testDir);
            } else {
                FileUtils.copyFileToDirectory(testFile, testDir);
            }
        } else {
            testDir.mkdirs();
        }
        Session session = Session.getInstance(sessionProps);
        store = session.getStore(new URLName("mstor:" + testDir.getAbsolutePath()));
    }
}
