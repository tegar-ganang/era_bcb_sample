package com.mindtree.techworks.insight.preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.mindtree.techworks.insight.InsightConstants;
import com.mindtree.techworks.insight.preferences.model.ListPreferenceAttribute;
import com.mindtree.techworks.insight.preferences.model.Preference;
import com.mindtree.techworks.insight.preferences.model.PreferenceAttribute;
import com.mindtree.techworks.insight.preferences.model.PreferenceAttributeType;
import com.mindtree.techworks.insight.preferences.model.PreferenceInfo;
import junit.framework.TestCase;

/**
 * Generic test cases to test preferences
 * 
 * @see PreferenceManager
 * 
 * @author <a href="mailto:bindul_bhowmik@mindtree.com">Bindul Bhowmik</a>
 * @version $Revision: 30 $ $Date: 2007-12-16 07:20:57 -0500 (Sun, 16 Dec 2007) $
 */
public class PreferencesTest extends TestCase {

    private Log log = LogFactory.getLog(PreferencesTest.class);

    private String insightTestDir = null;

    private boolean haveSetInsightHome = false;

    protected void setUp() throws Exception {
        super.setUp();
        String tempDirName = System.getProperty("java.io.tmpdir");
        File tempDir = new File(tempDirName);
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            fail("Cannot get temporary directory to run tests");
        }
        insightTestDir = tempDirName + "/insight-test-" + System.currentTimeMillis();
        File insightTestDirFile = new File(insightTestDir);
        if (!insightTestDirFile.mkdir()) {
            fail("Cannot create temporary directory to run tests");
        }
        insightTestDirFile = new File(insightTestDirFile.getAbsolutePath() + "/config");
        if (!insightTestDirFile.mkdir()) {
            fail("Cannot create temporary directory to run tests");
        }
        InputStream rawStream = getClass().getResourceAsStream("/insight-preferences.xml");
        FileOutputStream foStream = new FileOutputStream(insightTestDirFile.getAbsoluteFile() + "/insight-preferences.xml");
        byte[] readBuf = new byte[1024];
        int readCount = 0;
        while ((readCount = rawStream.read(readBuf)) != -1) {
            foStream.write(readBuf, 0, readCount);
        }
        if (null != foStream) {
            try {
                foStream.close();
            } catch (IOException e) {
            }
        }
        String insightHome = System.getProperty(InsightConstants.INSIGHT_HOME);
        if (null == insightHome || insightHome.trim().length() == 0) {
            System.setProperty(InsightConstants.INSIGHT_HOME, insightTestDir);
            haveSetInsightHome = true;
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (haveSetInsightHome) {
            System.getProperties().remove(InsightConstants.INSIGHT_HOME);
        }
        if (null != insightTestDir) {
            File testDir = new File(insightTestDir);
            if (testDir.exists()) {
                recursiveDelete(testDir);
            }
        }
    }

    /**
	 * Recursively empty the directory or file passed.
	 * @param fileToDel The file or directory to delete
	 */
    private void recursiveDelete(File fileToDel) {
        if (fileToDel.exists()) {
            if (fileToDel.isFile()) {
                fileToDel.delete();
            } else if (fileToDel.isDirectory()) {
                File[] children = fileToDel.listFiles();
                if (children != null && children.length > 0) {
                    for (int i = 0; i < children.length; i++) {
                        recursiveDelete(children[i]);
                    }
                }
                fileToDel.delete();
            }
        }
    }

    public void testGetPreferenceManager() {
        assertNotNull(PreferenceManager.getInstance());
    }

    public void testReadAllPreferences() {
        log.info("STARTING DUMP ALL PREFERENCES TEST");
        PreferenceManager preferenceManager = PreferenceManager.getInstance();
        log.debug("GETTING ALL PREFERENCE NAMES");
        Collection preferenceNames = preferenceManager.getAllPreferenceNames();
        log.debug("PRINTING ALL PREFERENCE NAMES");
        Iterator preferenceNameIterator = preferenceNames.iterator();
        while (preferenceNameIterator.hasNext()) {
            PreferenceInfo preferenceInfo = (PreferenceInfo) preferenceNameIterator.next();
            log.info("Preference: " + preferenceInfo.getName() + " [" + preferenceInfo.getId() + "] ");
            log.info("\tisUserModifiable: " + preferenceInfo.isUserModifiable());
            runPreferenceTest(preferenceInfo.getId());
        }
        log.info("END OF TEST");
    }

    private void runPreferenceTest(String id) {
        PreferenceManager manager = PreferenceManager.getInstance();
        Preference preference = manager.getPreference(id);
        if (null != preference) {
            log.info("PREFERENCE INFO: ");
            log.info("id: " + preference.getId());
            log.info("name: " + preference.getName());
            log.info("isUserModifiable: " + preference.isUserModifiable());
            log.info("Attribute Ids: ");
            Iterator attributeIdIterator = preference.iteratePreferenceAttributeIds();
            while (attributeIdIterator.hasNext()) {
                log.info(attributeIdIterator.next() + "\t");
            }
            attributeIdIterator = preference.iteratePreferenceAttributeIds();
            while (attributeIdIterator.hasNext()) {
                runPreferenceAttributeTest(preference.getPreferenceAttributeById((String) attributeIdIterator.next()));
            }
        }
    }

    /**
	 * Runs tests on Preference Attributes
	 * @param preferenceAttribute
	 */
    private void runPreferenceAttributeTest(PreferenceAttribute preferenceAttribute) {
        if (null != preferenceAttribute) {
            log.info("\tPREFERENCE ATTRIBUTE INFO: ");
            log.info("\tid: " + preferenceAttribute.getId());
            log.info("\tname: " + preferenceAttribute.getName());
            log.info("\tisUserModifiable: " + preferenceAttribute.isUserModifiable());
            log.info("\tisPersistant: " + preferenceAttribute.isPersistant());
            log.info("\tdefaultValue: " + preferenceAttribute.getDefaultValue());
            log.info("\tvalue: " + preferenceAttribute.getValue());
            log.info("\ttype: " + preferenceAttribute.getType());
            if (preferenceAttribute.getType().getType() == PreferenceAttributeType.LIST_TYPE) {
                runListPreferenceAttributeTest((ListPreferenceAttribute) preferenceAttribute);
            }
        }
    }

    private void runListPreferenceAttributeTest(ListPreferenceAttribute listPreferenceAttribute) {
        if (null != listPreferenceAttribute) {
            log.info("\t\tLIST PREFERENCE ATTRIBUTE INFO: ");
            Iterator optionsIterator = listPreferenceAttribute.iterateOptions();
            log.info("\t\tOptions: ");
            while (optionsIterator.hasNext()) {
                log.info("\t\t\t" + optionsIterator.next());
            }
        }
    }

    public void testPreferenceSave() {
        PreferenceManager preferenceManager = PreferenceManager.getInstance();
        Preference appPreference = preferenceManager.getPreference("app");
        Preference ftpPreference = preferenceManager.getPreference("ftp");
        if (null != appPreference) {
            PreferenceAttribute preferenceAttribute = appPreference.getPreferenceAttributeById("look");
            preferenceAttribute.setValue("Motif");
        }
        if (null != ftpPreference) {
            PreferenceAttribute preferenceAttribute = ftpPreference.getPreferenceAttributeById("url");
            preferenceAttribute.setValue("ftp://testservices.mindtree.com/build/jars/HEAD");
        }
        List preferenceList = new ArrayList();
        preferenceList.add(appPreference);
        preferenceList.add(ftpPreference);
        PreferenceDataHandler dataHandler = null;
        try {
            dataHandler = PreferenceDataHandlerFactory.getDefaultHandler();
            dataHandler.savePreferences(preferenceList);
        } catch (PreferenceHandlerInstantiationException e) {
            fail("PreferenceHandlerInstantiationException while writing preference");
        } catch (PreferenceHandlerStoreException e) {
            fail("PreferenceHandlerStoreException while writing preference");
        }
        preferenceManager = PreferenceManager.getInstance();
        appPreference = preferenceManager.getPreference("app");
        ftpPreference = preferenceManager.getPreference("ftp");
        if (null != appPreference) {
            PreferenceAttribute preferenceAttribute = appPreference.getPreferenceAttributeById("look");
            assertEquals("Motif", preferenceAttribute.getValue());
        } else {
            fail("App Preference Not Found");
        }
        if (null != ftpPreference) {
            PreferenceAttribute preferenceAttribute = ftpPreference.getPreferenceAttributeById("url");
            assertEquals("ftp://testservices.mindtree.com/build/jars/HEAD", preferenceAttribute.getValue());
        } else {
            fail("App Preference Not Found");
        }
    }

    public void testPreferenceChangeNotification() {
        PreferenceManager preferenceManager = PreferenceManager.getInstance();
        PrefAttributeChangeListenerTest listener = new PrefAttributeChangeListenerTest();
        preferenceManager.registerListener("log4jPattern" + Preference.ID_SEPERATOR + "childPref1", "dbld1", listener);
        Preference preference = preferenceManager.getPreference("log4jPattern");
        if (null != preference) {
            Preference childPreference = preference.getPreferenceById("childPref1");
            PreferenceAttribute preferenceAttribute = childPreference.getPreferenceAttributeById("dbld1");
            System.out.println(preferenceAttribute.getValue());
            preferenceAttribute.setValue("NewTestValue");
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        assertEquals("NewTestValue", listener.newValue);
    }

    private static class PrefAttributeChangeListenerTest implements PreferenceAttributeChangeListener {

        private String newValue;

        public void attributeValueChanged(String preferenceId, String attributeId, String newValue) {
            this.newValue = newValue;
        }
    }
}
