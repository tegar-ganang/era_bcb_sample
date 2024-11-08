package tests.service;

import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.Set;
import junit.framework.TestCase;
import com.ivis.xprocess.core.Organization;
import com.ivis.xprocess.core.Portfolio;
import com.ivis.xprocess.framework.DataSource;
import com.ivis.xprocess.framework.vcs.exceptions.VCSCleanNeededException;
import com.ivis.xprocess.framework.vcs.exceptions.VCSUpdateNeededException;
import com.ivis.xprocess.util.FileUtils;
import com.ivis.xprocess.util.License;
import com.ivis.xprocess.web.elements.TransientOrganization;
import com.ivis.xprocess.web.elements.TransientPortfolio;
import com.ivis.xprocess.web.framework.MasterDatasource;
import com.ivis.xprocess.web.framework.MasterDatastoreManager;
import com.ivis.xprocess.web.framework.SessionDatasource;

/**
 * The idea of the 'test portfolio' is that you write test that use this portfolio to create elements under, then when setup runs it gets deleted and you can starte with a clean area to test
 * 
 */
public class WebTestCase extends TestCase {

    protected String sessionToken;

    protected TransientPortfolio testPortfolio;

    protected TransientOrganization testOrganization;

    protected MasterDatasource masterDatasource;

    protected SessionDatasource sessionDatasource;

    @Override
    protected void setUp() throws Exception {
        if (sessionDatasource == null) {
            String baseDir = "c:\temp";
            if (System.getProperty("os.name").toLowerCase().startsWith("linux")) {
                baseDir = "/home/tim/temp/";
            }
            File file = new File(baseDir);
            if (!file.exists()) {
                file.mkdirs();
                System.out.println("make temp dir - " + baseDir);
            }
            File realConfigFile = new File(baseDir + File.separator + "config.xml");
            if (!realConfigFile.exists()) {
                File junitConfigFile = new File("." + File.separator + "config" + File.separator + "config.xml");
                if (!junitConfigFile.exists()) {
                    throw new RuntimeException("Unable to find the JUnit config file");
                }
                FileUtils.copyFile(junitConfigFile, realConfigFile);
            }
            License.initialize(new FileReader("." + File.separator + "config" + File.separator + "license.lic"));
            if (MasterDatastoreManager.getMasterDatasource() == null) {
                masterDatasource = new MasterDatasource();
                MasterDatastoreManager.setMasterDatasource(masterDatasource);
                masterDatasource.initialise(baseDir);
            } else {
                masterDatasource = (MasterDatasource) MasterDatastoreManager.getMasterDatasource();
            }
            sessionToken = MasterDatastoreManager.createSession("test", "password");
            sessionDatasource = MasterDatastoreManager.getSessionDatasource(sessionToken);
        }
        DataSource datasource = sessionDatasource.getDataSource();
        Portfolio rootPortfolio = (Portfolio) datasource.getRoot();
        Set<Portfolio> portfolios = rootPortfolio.getPortfolios();
        for (Portfolio portfolio : portfolios) {
            portfolio.delete();
        }
        try {
            sessionDatasource.getDataSource().getVcsProvider().commit();
        } catch (VCSUpdateNeededException vcsUpdateNeededException) {
            sessionDatasource.getDataSource().getVcsProvider().update();
            sessionDatasource.getDataSource().getVcsProvider().commit();
        } catch (VCSCleanNeededException vcsCleanNeededException) {
            sessionDatasource.getDataSource().getVcsProvider().cleanup();
            sessionDatasource.getDataSource().getVcsProvider().commit();
        }
        Portfolio newPortfolio = rootPortfolio.createPortfolio("Test Portfolio");
        masterDatasource.saveAndAdd(newPortfolio);
        Organization newOrganization = newPortfolio.createOrganization("Test Organization");
        masterDatasource.saveAndAdd(newOrganization);
        testPortfolio = (TransientPortfolio) sessionDatasource.getTransientElement(newPortfolio.getUuid());
        testOrganization = (TransientOrganization) sessionDatasource.getTransientElement(newOrganization.getUuid());
        sessionDatasource.getDataSource().getVcsProvider().commit();
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected void pause(long time) {
        Date today = new Date();
        long start = today.getTime();
        while (true) {
            today = new Date();
            long millisecondsPassed = today.getTime() - start;
            if (millisecondsPassed > time) {
                return;
            }
        }
    }
}
