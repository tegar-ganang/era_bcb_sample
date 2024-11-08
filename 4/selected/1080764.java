package com.nokia.ats4.appmodel.util.localisation;

import com.nokia.ats4.appmodel.MainApplication;
import com.nokia.ats4.appmodel.dao.DAOFactory;
import com.nokia.ats4.appmodel.dao.ProjectDAO;
import com.nokia.ats4.appmodel.export.rtf.Composer;
import com.nokia.ats4.appmodel.model.KendoModel;
import com.nokia.ats4.appmodel.model.KendoProject;
import com.nokia.ats4.appmodel.model.impl.MainApplicationModel;
import com.nokia.ats4.appmodel.util.Settings;
import java.io.File;
import java.io.IOException;
import junit.framework.*;

/**
 *
 * @author hannuph
 */
public class LanguageVariantTest extends TestCase {

    /** Generate all documents or not */
    private boolean generate = false;

    public LanguageVariantTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        Settings.load(MainApplication.FILE_PROPERTIES);
    }

    protected void tearDown() throws Exception {
    }

    /**
     * Test of getName method, of class com.nokia.kendo.util.localisation.LanguageVariant.
     */
    public void testGetName() {
        System.out.println("getName");
        LanguageVariant instance = null;
        try {
            instance = LanguageVariantFactory.getVariant("English");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String expResult = "English";
        String result = instance.getName();
        assertEquals(expResult, result);
    }

    /**
     * Test of getText method, of class com.nokia.kendo.util.localisation.LanguageVariant.
     */
    public void testGetText() {
        System.out.println("getText");
        LanguageVariant instance = null;
        try {
            instance = LanguageVariantFactory.getVariant("English");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String separator = Settings.getProperty(Settings.LOGICAL_NAME_SEPARATOR_PROPERTY);
        String logicalName = String.format("%scmd_exit%s", separator, separator);
        String expected = "Exit";
        String result = instance.getText(logicalName);
        assertEquals(expected, result);
    }

    /**
     * Test of getLocigalNames method, of class com.nokia.kendo.util.localisation.LanguageVariant.
     */
    public void testGetLocigalNames() {
        System.out.println("getLocigalNames");
        LanguageVariant instance = null;
        try {
            instance = LanguageVariantFactory.getVariant("English");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        String separator = Settings.getProperty(Settings.LOGICAL_NAME_SEPARATOR_PROPERTY);
        String logicalName = String.format("%scmd_exit%s", separator, separator);
        assertTrue(instance.getLocigalNames().contains(logicalName));
    }

    public void testGenerateAllLanguages() {
        String path = "Gallery/Gallery.xml";
        File file = new File(path);
        if (file.exists() && generate) {
            String[] languages = null;
            try {
                languages = LanguageVariantFactory.getVariantNames();
                Settings.load(MainApplication.FILE_PROPERTIES);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            for (int i = 0; i < languages.length; i++) {
                Settings.setProperty("language.variant", languages[i]);
                MainApplicationModel model = MainApplicationModel.getInstance();
                model.setCurrentDirectory(file);
                KendoProject project = model.getActiveProject();
                project.setFile(file);
                DAOFactory factory = DAOFactory.getDAOFactory(DAOFactory.LOCAL_PERSISTENCE);
                ProjectDAO dao = factory.getProjectDAO(project);
                dao.open();
                Settings.setProperty("language.variant", languages[i]);
                KendoModel m = project.getModelByName("Gallery");
                String tempFile = file.getParent() + File.separator + m.getName() + "-" + languages[i] + ".rtf";
                File rtfFile = new File(tempFile);
                Composer composer = new Composer(null, project, m, rtfFile);
                Thread writeThread = new Thread(composer);
                writeThread.start();
            }
        }
    }
}
