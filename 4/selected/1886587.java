package net.sf.jmoney.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.xml.transform.TransformerConfigurationException;
import junit.framework.TestCase;
import net.sf.jmoney.model2.BankAccountInfo;
import net.sf.jmoney.model2.TransactionInfo;
import net.sf.jmoney.model2.BankAccount;
import net.sf.jmoney.model2.Entry;
import net.sf.jmoney.model2.Session;
import net.sf.jmoney.model2.Transaction;
import net.sf.jmoney.serializeddatastore.SessionManager;
import net.sf.jmoney.serializeddatastore.formats.JMoneyXmlFormat;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.xml.sax.SAXException;

/**
 * Tests regarding opening JMoney session files.
 * 
 * @author Johann Gyger
 */
public class OpenSessionTest extends TestCase {

    @Override
    public void setUp() {
        Bundle bundle = Platform.getBundle("net.sf.jmoney.test");
        try {
            bundle.start();
        } catch (BundleException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tearDown() {
        Bundle bundle = Platform.getBundle("net.sf.jmoney.test");
        try {
            bundle.stop();
        } catch (BundleException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - Old format (version 0.4.5 or prior)
     * - Empty (minimal file contents)
     * - Uncompressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testOldEmptyUncompressed() throws IOException, CoreException {
        File file = getSessionFile("old_empty_session.xml");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.xmlFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - Old format (version 0.4.5 or prior)
     * - Empty (minimal file contents)
     * - Compressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testOldEmptyCompressed() throws IOException, CoreException {
        File file = getSessionFile("old_empty_session.jmx");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.jmxFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - New format
     * - Empty (minimal file contents)
     * - Uncompressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testNewEmptyUncompressed() throws IOException, CoreException {
        File file = getSessionFile("new_empty_session.xml");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.xmlFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - New format
     * - Empty (minimal file contents)
     * - Compressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testNewEmptyCompressed() throws IOException, CoreException {
        File file = getSessionFile("new_empty_session.jmx");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager("net.sf.jmoney.serializeddatastore.jmxFormat", reader, file);
        reader.readSessionQuietly(file, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
    }

    /**
     * Test if a JMoney file with the properties below can be opened:
     * - New format
     * - Empty (minimal file contents)
     * - Compressed
     * 
     * @throws CoreException
     * @throws IOException
     */
    public void testOpenUpdateSaveCycle() throws IOException, CoreException {
        File inputFile = getSessionFile("new_empty_session.jmx");
        File outputFile = new File("test1.xml");
        JMoneyXmlFormat reader = new JMoneyXmlFormat();
        SessionManager manager = new SessionManager(JMoneyXmlFormat.ID_FILE_FORMAT, reader, inputFile);
        reader.readSessionQuietly(inputFile, manager, null);
        assertNotNull(manager);
        assertNotNull(manager.getSession());
        Session session = manager.getSession();
        BankAccount account1 = session.getAccountCollection().createNewElement(BankAccountInfo.getPropertySet());
        BankAccount account2 = session.getAccountCollection().createNewElement(BankAccountInfo.getPropertySet());
        account1.setName("My Checking Account");
        account2.setName("My Savings Account");
        Transaction trans = session.getTransactionCollection().createNewElement(TransactionInfo.getPropertySet());
        Entry entry1 = trans.getEntryCollection().createEntry();
        Entry entry2 = trans.getEntryCollection().createEntry();
        entry1.setAccount(account1);
        entry2.setAccount(account2);
        entry1.setAmount(1234);
        entry2.setAmount(-1234);
        entry1.setMemo("transfer from savings");
        entry2.setMemo("transfer to checking");
        try {
            reader.writeSessionQuietly(manager, outputFile, null);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        } catch (SAXException e) {
            e.printStackTrace();
            fail(e.getLocalizedMessage());
        }
        SessionManager manager2 = new SessionManager(JMoneyXmlFormat.ID_FILE_FORMAT, reader, inputFile);
        reader.readSessionQuietly(inputFile, manager2, null);
        assertNotNull(manager2);
        assertNotNull(manager2.getSession());
        Session session2 = manager2.getSession();
        assertEquals(1, session2.getCommodityCollection().size());
        assertEquals(2, session2.getAccountCollection().size());
        assertEquals(1, session2.getTransactionCollection().size());
    }

    protected File getSessionFile(String filename) throws IOException {
        Bundle bundle = Platform.getBundle("net.sf.jmoney.test");
        URL url = bundle.getEntry("resources/" + filename);
        return new File(FileLocator.toFileURL(url).getFile());
    }
}
