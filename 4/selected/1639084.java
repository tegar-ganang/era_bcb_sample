package unit.dbadmin;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import uk.co.q3c.deplan.dao.DatabaseConnection;
import uk.co.q3c.deplan.dao.DatabaseFactory;
import uk.co.q3c.deplan.dao.SynchronisedDatabaseConnection_db4o;
import uk.co.q3c.deplan.dbadmin.DbAdmin;
import uk.co.q3c.deplan.dbadmin.UpgradeException;
import uk.co.q3c.deplan.dbadmin.DbAdmin.DefragmentResult;
import uk.co.q3c.deplan.domain.DomainObjectComparator;
import uk.co.q3c.deplan.domain.DomainMatchingLog.Outcome;
import uk.co.q3c.deplan.domain.resource.BaseCalendar;
import uk.co.q3c.deplan.domain.resource.IndividualResource;
import uk.co.q3c.deplan.domain.resource.Resource;
import uk.co.q3c.deplan.domain.resource.ResourcePool;
import uk.co.q3c.deplan.domain.resource.ResourceProfileEntry;
import uk.co.q3c.deplan.domain.task.ResourcedTask;
import uk.co.q3c.deplan.domain.task.Task;
import uk.co.q3c.deplan.domain.task.TaskResourceProfile;
import uk.co.q3c.deplan.util.CaseInsensitiveHashMap;
import util.TestUtils;

/**
 * @see DbAdmin
 * 
 * @author DSowerby 5 Nov 2008
 * 
 */
@Test
public class DbAdmin_UT {

    protected final transient Logger logger = Logger.getLogger(getClass().getName());

    int taskCount1;

    int resourceCount1;

    int taskResourceProfileCount1;

    int taskCount2;

    int resourceCount2;

    int taskResourceProfileCount2;

    File testFile;

    File inputFile;

    File tempDir;

    @BeforeMethod
    public void beforeMethod() throws IOException {
        String currentdir = TestUtils.currentSourceDirName(this);
        inputFile = new File(currentdir + File.separator + "work.db4o");
        tempDir = new File("/home/dave/temp/" + getClass().getSimpleName());
        if (tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }
        FileUtils.forceMkdir(tempDir);
        FileUtils.copyFileToDirectory(inputFile, tempDir);
        testFile = new File(tempDir.getAbsolutePath() + File.separator + "work.db4o");
    }

    /**
	 * This only tests the {@link DbAdmin#upgrade} method, but that in turn uses
	 * the {@link DbAdmin#dbcopy dbcopy} method
	 * 
	 * @throws UpgradeException
	 * @throws IOException
	 * 
	 * @throws IOException
	 */
    @Test(enabled = false)
    public void upgrade() throws UpgradeException, IOException {
        DatabaseConnection dbc = DatabaseFactory.newDatabaseConnection(testFile, false, null, null);
        DbAdmin dbadmin = new DbAdmin();
        taskCount1 = dbc.getGeneralDao().count(Task.class);
        dbadmin.upgrade(dbc, null);
        dbc.open();
        taskCount2 = dbc.getGeneralDao().count(Task.class);
        taskResourceProfileCount1 = dbc.getGeneralDao().count(TaskResourceProfile.class);
        logger.info("Number of task resource profiles = " + taskResourceProfileCount1);
        Assert.assertEquals(dbc.getAdminDao().loadDatabaseVersion().getNumber(), 2);
        Assert.assertEquals(dbc.getBaseCalendarDao().count(BaseCalendar.class), 1);
        DatabaseConnection dbcInput = DatabaseFactory.newDatabaseConnection(inputFile, false, null, null);
        Assert.assertEquals(dbcInput.getGeneralDao().count(Resource.class), dbc.getGeneralDao().count(Resource.class));
        logger.info("orphans " + dbcInput.getTaskDao().orphans().size());
        logger.info(dbcInput.getTaskDao().count(Task.class));
        DomainObjectComparator doc = new DomainObjectComparator();
        doc.getLog().logAllFail();
        Assert.assertEquals(dbcInput.getResourceDao().count(ResourceProfileEntry.class), 0);
        BaseCalendar bcalA = dbcInput.getBaseCalendarDao().find();
        BaseCalendar bcalB = dbc.getBaseCalendarDao().find();
        Outcome finalOutcome = doc.compare(bcalA, bcalB);
        logger.info("Total fails = " + doc.getLog().getTotalFails());
        Assert.assertEquals(finalOutcome, Outcome.PASS);
        ResourcePool poolA = dbcInput.getResourceDao().loadResourcePool();
        ResourcePool poolB = dbc.getResourceDao().loadResourcePool();
        doc.addExcludedField("[root, groups]");
        doc.addExcludedField("[root, individuals]");
        doc.addExcludedClass(CaseInsensitiveHashMap.class);
        finalOutcome = doc.compare(poolA, poolB);
        logger.info("Total fails = " + doc.getLog().getTotalFails());
        Assert.assertEquals(finalOutcome, Outcome.EXCLUDED);
        ResourcedTask rt = (ResourcedTask) dbc.getTaskDao().findFirstByName("Holiday");
        IndividualResource ir = (IndividualResource) rt.getAssignedResource();
        Assert.assertNotNull(ir.getResourceCalendar().getBaseCalendar());
        Task inputMP = dbcInput.getTaskDao().findFirstByName("Master Plan");
        Task outputMP = dbc.getTaskDao().findFirstByName("Master Plan");
        finalOutcome = doc.compare(outputMP, inputMP);
        logger.info("Total fails = " + doc.getLog().getTotalFails());
        Assert.assertEquals(finalOutcome, Outcome.PASS);
        Assert.assertEquals(inputMP.treeCount(), outputMP.treeCount());
        dbcInput.close();
        dbc.close();
        String masterFileName = dbc.filename();
        String localFileName = tempDir + File.separator + "work.yap";
        SynchronisedDatabaseConnection_db4o sdbc = new SynchronisedDatabaseConnection_db4o(masterFileName, localFileName, null, this.getClass().getSimpleName());
        sdbc.open();
        sdbc.synchronise();
        Assert.assertTrue(sdbc.isSynchronised());
        sdbc.close();
    }

    public void defrag() {
        DbAdmin dbadmin = new DbAdmin();
        long before = testFile.length();
        DefragmentResult dr = dbadmin.defragment(testFile.toString());
        long after = testFile.length();
        logger.info(before + "," + after);
        Assert.assertEquals(before, dr.before);
        Assert.assertEquals(after, dr.after);
    }
}
