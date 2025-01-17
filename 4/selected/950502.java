package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import net.sourceforge.cruisecontrol.dashboard.Projects;
import net.sourceforge.cruisecontrol.dashboard.sourcecontrols.Svn;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.FilesystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class ConfigXmlFileServiceTest extends MockObjectTestCase {

    private File tempDirForSetup;

    private ConfigXmlFileService service;

    private File configFile;

    private Mock envService;

    public void setUp() throws Exception {
        tempDirForSetup = DataUtils.createTempDirectory("temp");
        TemplateRenderService renderService = new TemplateRenderService();
        renderService.loadTemplates();
        envService = mock(EnvironmentService.class, new Class[] { SystemService.class, DashboardConfigService[].class }, new Object[] { new SystemService(), new DashboardConfigService[] {} });
        service = new ConfigXmlFileService((EnvironmentService) envService.proxy(), renderService);
        File configDirectory = FilesystemUtils.createDirectory("tempDir");
        configFile = FilesystemUtils.createFile("config.xml", configDirectory);
    }

    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(tempDirForSetup);
    }

    public void testShouldBeAbleToReadConfigXml() throws Exception {
        File firstFile = DataUtils.createTempFile("config", "xml");
        DataUtils.writeContentToFile(firstFile, "first");
        assertEquals("first", service.readContentFromConfigFile(firstFile.getAbsolutePath()));
    }

    public void testShouldBeAbleToWriteContentToConfigXml() throws Exception {
        File firstFile = DataUtils.createTempFile("config", "xml");
        String expected = "some thing to write";
        service.writeContentToConfigXml(firstFile.getAbsolutePath(), expected);
        assertEquals(expected, service.readContentFromConfigFile(firstFile.getAbsolutePath()));
    }

    public void testShouldReturnNullWhenInvalidConfigurationDirectoryIsSpecified() {
        envService.expects(once()).method("getConfigXml").will(returnValue(new File("not/exist")));
        assertNull(service.getConfigXmlFile(new File("")));
    }

    public void testShouldBeNotNullWhenValidConfigurationDirectoryIsSpecifiedThoughProp() {
        envService.expects(atLeastOnce()).method("getConfigXml").will(returnValue(new File(configFile.getAbsolutePath())));
        assertNotNull(service.getConfigXmlFile(new File("")));
    }

    public void testShouldSayThatTheTemporaryDirectoryWithConfigFileIsAValidLocation() throws Exception {
        assertNotNull(service.getConfigXmlFile(configFile));
    }

    public void testShouldBeAbleGetAllProjectsInfoFromConfigXmlFile() throws Exception {
        File testData = new File("test/data");
        envService.expects(atLeastOnce()).method("getProjectsDir").will(returnValue(new File(testData, "projects")));
        envService.expects(atLeastOnce()).method("getLogDir").will(returnValue(new File(testData, "logs")));
        envService.expects(atLeastOnce()).method("getArtifactsDir").will(returnValue(new File(testData, "arbitrary_artifacts")));
        Projects projects = service.getProjects(DataUtils.getConfigXmlAsFile());
        File baseRoot = ((EnvironmentService) envService.proxy()).getLogDir();
        assertEquals(new File(baseRoot, "project1"), projects.getLogRoot("project1"));
        assertEquals(new File(baseRoot, "project2"), projects.getLogRoot("project2"));
        assertEquals(new File(baseRoot, "projectWithoutPublishers"), projects.getLogRoot("projectWithoutPublishers"));
        assertEquals(new File(baseRoot, "project name"), projects.getLogRoot("project name"));
        File artifacts = ((EnvironmentService) envService.proxy()).getArtifactsDir();
        assertEquals(new File(artifacts, "project1"), projects.getArtifactRoot("project1"));
        assertEquals(new File(artifacts, "project2"), projects.getArtifactRoot("project2"));
        assertEquals(8, projects.getProjectNames().length);
    }

    public void testShouldPutPluginProjectIntoProjectsHashMap() throws Exception {
        File testData = new File("test/data");
        envService.expects(atLeastOnce()).method("getProjectsDir").will(returnValue(new File(testData, "projects")));
        envService.expects(atLeastOnce()).method("getLogDir").will(returnValue(new File(testData, "logs")));
        envService.expects(atLeastOnce()).method("getArtifactsDir").will(returnValue(new File(testData, "arbitrary_artifacts")));
        Projects projects = service.getProjects(DataUtils.getConfigXmlAsFile());
        assertTrue(projects.hasProject("cclive"));
        assertTrue(projects.hasProject("cc-live-2"));
    }

    public void testShouldAssembleProjectsWithSpecifiedLogroot() throws Exception {
        File logfolder = FilesystemUtils.createDirectory("temp").getAbsoluteFile();
        envService.expects(atLeastOnce()).method("getProjectsDir").will(returnValue(new File("projects")));
        envService.expects(atLeastOnce()).method("getLogDir").will(returnValue(logfolder));
        envService.expects(atLeastOnce()).method("getArtifactsDir").will(returnValue(new File("arbitrary_artifacts")));
        Projects projects = service.getProjects(DataUtils.getConfigXmlAsFile());
        assertEquals(new File(logfolder, "projecta"), projects.getLogRoot("projecta"));
    }

    public void testShouldSupportMultiPlugin() throws Exception {
        File testData = new File("test/data");
        envService.expects(atLeastOnce()).method("getProjectsDir").will(returnValue(new File(testData, "projects")));
        envService.expects(atLeastOnce()).method("getLogDir").will(returnValue(new File(testData, "logs")));
        envService.expects(atLeastOnce()).method("getArtifactsDir").will(returnValue(new File(testData, "arbitrary_artifacts")));
        Projects projects = service.getProjects(DataUtils.getConfigXmlAsFile());
        assertTrue(projects.hasProject("dashboardlive"));
    }

    public void testShouldBeAbleToAddNewProjectToCurrentConfigXml() throws Exception {
        File configDirectory = FilesystemUtils.createDirectory("cruisecontrol");
        FileUtils.copyFileToDirectory(DataUtils.getConfigXmlAsFile(), configDirectory);
        File config = new File(configDirectory, "config.xml");
        long initLength = config.length();
        service.addProject(config, "CruiseControlOSS", new Svn(null, null));
        String currentFile = FileUtils.readFileToString(config, null);
        assertTrue(StringUtils.contains(currentFile, "CruiseControlOSS"));
        assertTrue(config.length() > initLength);
    }

    public void testShouldAddBootstrapperAndRepositoryBaseOnVCS() throws Exception {
        File configDirectory = FilesystemUtils.createDirectory("cruisecontrol");
        File configxml = FilesystemUtils.createFile("config3.xml", configDirectory);
        FileUtils.writeStringToFile(configxml, "<cruisecontrol></cruisecontrol>");
        File config = new File(configDirectory, "config.xml");
        service.addProject(config, "CruiseControlOSS", new Svn(null, null));
        String currentFile = FileUtils.readFileToString(config, null);
        assertTrue(StringUtils.contains(currentFile, "project name=\"CruiseControlOSS\""));
        assertTrue(StringUtils.contains(currentFile, "<svnbootstrapper localWorkingCopy"));
        assertTrue(StringUtils.contains(currentFile, "<svn localWorkingCopy="));
    }
}
