package net.admin4j.dao.xml;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import net.admin4j.config.Admin4JConfiguration;
import net.admin4j.config.TestEnvironmentConfiguration;
import net.admin4j.dao.ExceptionInfoDAO;
import net.admin4j.entity.ExceptionInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestExceptionInfoDAOXml extends BaseTestDAOXml {

    private static final String LEGACY_RC1_EXCEPTION_XML_FILE_NAME = System.getProperty("user.dir") + "/test/net/admin4j/dao/xml/release-1.0-rc1.exceptionInfo.xml";

    private static final String LEGACY_RC2_EXCEPTION_XML_FILE_NAME = System.getProperty("user.dir") + "/test/net/admin4j/dao/xml/release-1.0-rc2.exceptionInfo.xml";

    private static final String TEST_ADMIN4J_CONFIGURATION_XML = "testAdmin4JConfiguration.xml";

    private ExceptionInfoDAO dao;

    private Set<ExceptionInfo> exceptionList;

    @Before
    public void setup() {
        dao = new ExceptionInfoDAOXml();
        TestEnvironmentConfiguration.setExceptionInformationStorageFormat(Admin4JConfiguration.StorageFormat.XML);
        TestEnvironmentConfiguration.setExceptionInformationXmlFileName(TEST_ADMIN4J_CONFIGURATION_XML);
        exceptionList = new HashSet<ExceptionInfo>();
        Throwable t = new NullPointerException();
        exceptionList.add(new ExceptionInfo(t.getClass().getName(), t.getStackTrace()));
    }

    @After
    public void teardown() throws Exception {
        purgefile(new File(TEST_ADMIN4J_CONFIGURATION_XML));
        purgefile(new File(TEST_ADMIN4J_CONFIGURATION_XML + ".previous"));
        purgefile(new File(TEST_ADMIN4J_CONFIGURATION_XML + ".temp"));
        purgefile(new File(LEGACY_RC1_EXCEPTION_XML_FILE_NAME + ".previous"));
        purgefile(new File(LEGACY_RC1_EXCEPTION_XML_FILE_NAME + ".temp"));
        purgefile(new File(LEGACY_RC2_EXCEPTION_XML_FILE_NAME + ".previous"));
        purgefile(new File(LEGACY_RC2_EXCEPTION_XML_FILE_NAME + ".temp"));
        TestEnvironmentConfiguration.setExceptionInformationXmlFileName(null);
    }

    @Test
    public void testFindAll() {
        dao.saveAll(exceptionList);
        Set<ExceptionInfo> list = dao.findAll();
        assertTrue("basic read/write test", list.size() == 1);
    }

    @Test
    public void testFindAllRelease_1_0_RC1() {
        TestEnvironmentConfiguration.setExceptionInformationXmlFileName(LEGACY_RC1_EXCEPTION_XML_FILE_NAME);
        Set<ExceptionInfo> list = dao.findAll();
        assertTrue("basic read/write test", list.size() == 13);
    }

    @Test
    public void testFindAllRelease_1_0_RC2() {
        TestEnvironmentConfiguration.setExceptionInformationXmlFileName(LEGACY_RC2_EXCEPTION_XML_FILE_NAME);
        Set<ExceptionInfo> list = dao.findAll();
        assertTrue("basic read/write test", list.size() == 13);
    }
}
