package net.sf.cantina.datasource;

import junit.framework.TestCase;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import net.sf.cantina.*;
import net.sf.cantina.search.LuceneSearchEngine;
import net.sf.cantina.search.LuceneDocumentSearchHandler;
import net.sf.cantina.util.FileUtils;
import net.sf.cantina.system.CreateDocumentTransaction;
import net.sf.cantina.system.ChangeDocumentTransaction;
import net.sf.cantina.application.Application;

/**
 * @author Stephane JAIS
 */
public class BackupUtilsTest extends TestCase {

    private boolean initDone = false;

    String path = "/tmp/utest-cantina-DataSourceDumpAction";

    private void initDb() throws Exception {
        User u = new User() {

            public boolean isUserInRole(String role) {
                return true;
            }

            public String getName() {
                return "utestUser";
            }
        };
        DataSource ds = DataSource.getInstance();
        ds.createRealm("dumpRealm");
        ds.createRealm("dumpRealm1");
        new CreateDocumentTransaction("dumpDoc", "dumpRealm", "text/plain", u).execute();
        new CreateDocumentTransaction("dumpDoc1", "dumpRealm1", "text/plain", u).execute();
        new ChangeDocumentTransaction("dumpDoc", u, "dumpContent".getBytes("utf-8"), new Locale("en")).execute();
        new ChangeDocumentTransaction("dumpDoc1", u, "dumpContent1".getBytes("utf-8"), new Locale("en")).execute();
        initDone = true;
    }

    public void setUp() throws Exception {
        Application.init();
        if (!initDone) initDb();
    }

    public void tearDown() throws Exception {
        Application.shut();
    }

    public void testCreateHypersonicCopy() throws Exception {
        DataSource dsCopy = new HypersonicDataSource(path, true);
        BackupUtils.importDataSource(DataSource.getInstance(), dsCopy);
        File dataFile = new File(path + ".data");
        assertTrue("dataFile has been created", dataFile.exists());
        Collection originalDocuments = DataSource.getInstance().selectAllDocumentIds();
        Collection copiedDocuments = dsCopy.selectAllDocumentIds();
        for (Iterator i1 = originalDocuments.iterator(); i1.hasNext(); ) if (!copiedDocuments.contains(i1.next().toString())) fail("All documents are not present in the copy");
        dsCopy.release();
    }

    public void testDumpRealm() throws Exception {
        Realm original = DataSource.getInstance().loadRealm("dumpRealm");
        DataSource dsCopy = new HypersonicDataSource(path, true);
        BackupUtils.dumpRealm(original, dsCopy);
        Realm copy = dsCopy.loadRealm(original.getName());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getGroupDefaultLocale(), copy.getGroupDefaultLocale());
        original.setGroupDefaultLocale(new Locale("fr"));
        BackupUtils.dumpRealm(original, dsCopy);
        assertEquals(original.getGroupDefaultLocale(), copy.getGroupDefaultLocale());
        dsCopy.release();
    }

    public void testDumpDocument() throws Exception {
        Document original = DataSource.getInstance().loadDocument("dumpDoc1");
        DataSource dsCopy = new HypersonicDataSource(path, true);
        BackupUtils.dumpRealm(original.getRealm(), dsCopy);
        BackupUtils.dumpDocument(original, dsCopy);
        Document copy = dsCopy.loadDocument(original.getDocumentId());
        assertEquals(original.getDocumentId(), copy.getDocumentId());
        for (Iterator locales = original.getAvailableLocales().iterator(); locales.hasNext(); ) {
            Locale l = (Locale) locales.next();
            assertEquals(original.getContent(l), copy.getContent(l));
        }
        original.setContent(new Locale("fr"), "bonjour".getBytes("utf-8"));
        BackupUtils.dumpDocument(original, dsCopy);
        assertEquals(original.getContent(new Locale("fr")), copy.getContent(new Locale("fr")));
        dsCopy.release();
    }

    /**
   * Verifies that an hypersonic datasource can be restored from just a script file.
   * @throws Exception
   */
    public void testCopyScriptFile() throws Exception {
        DataSource dump = new HypersonicDataSource(path, true);
        BackupUtils.importDataSource(DataSource.getInstance(), dump);
        dump.release();
        FileUtils.copyFile(path + ".script", path + "-copy.script", true);
        DataSource copy = new HypersonicDataSource(path + "-copy", false);
        Document doc = copy.loadDocument("dumpDoc");
        assertEquals("dumpContent", doc.getContentAsString(new Locale("en")));
    }
}
