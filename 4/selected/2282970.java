package net.admin4j.dao.xml;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import net.admin4j.config.Admin4JConfiguration;
import net.admin4j.config.TestEnvironmentConfiguration;
import net.admin4j.timer.TaskTimer;
import net.admin4j.timer.TaskTimerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestTaskTimerDAOXml extends BaseTestDAOXml {

    private static final String TEST_ADMIN4J_TIMER_XML = "testTimer.xml";

    private static final String LEGACY_RC2_PERFORMANCE_XML_FILE_NAME = System.getProperty("user.dir") + "/test/net/admin4j/dao/xml/release-1.0-rc2.performanceInfo.xml";

    private TaskTimerDAOXml dao;

    private Set<TaskTimer> timerSet;

    @Before
    public void setUp() throws Exception {
        dao = new TaskTimerDAOXml();
        TestEnvironmentConfiguration.setPerformanceInformationStorageFormat(Admin4JConfiguration.StorageFormat.XML);
        TestEnvironmentConfiguration.setPerformanceInformationXmlFileName(TEST_ADMIN4J_TIMER_XML);
        timerSet = new HashSet<TaskTimer>();
        TaskTimer timer = TaskTimerFactory.start("fu");
        Thread.sleep(5);
        timer.stop();
        timerSet.add(timer);
        timer = TaskTimerFactory.start("bar");
        Thread.sleep(5);
        timer.stop();
        timerSet.add(timer);
    }

    @After
    public void tearDown() throws Exception {
        purgefile(new File(TEST_ADMIN4J_TIMER_XML));
        purgefile(new File(TEST_ADMIN4J_TIMER_XML + ".previous"));
        purgefile(new File(TEST_ADMIN4J_TIMER_XML + ".temp"));
        purgefile(new File(LEGACY_RC2_PERFORMANCE_XML_FILE_NAME + ".previous"));
        purgefile(new File(LEGACY_RC2_PERFORMANCE_XML_FILE_NAME + ".temp"));
        TestEnvironmentConfiguration.setPerformanceInformationXmlFileName(null);
    }

    @Test
    public void testFindAll() {
        dao.saveAll(timerSet);
        Set<TaskTimer> list = dao.findAll();
        assertTrue("basic read/write test", list.size() == 2);
    }

    @Test
    public void testFindAllRelease_1_0_RC2() {
        TestEnvironmentConfiguration.setPerformanceInformationXmlFileName(LEGACY_RC2_PERFORMANCE_XML_FILE_NAME);
        Set<TaskTimer> list = dao.findAll();
        assertTrue("basic read/write test.  size=" + list.size(), list.size() == 55);
    }
}
