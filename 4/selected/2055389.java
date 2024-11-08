package com.controltier.ctl.common;

import junit.framework.Test;
import junit.framework.TestSuite;
import com.controltier.ctl.cli.CtlGenMain;
import com.controltier.ctl.tools.CtlTest;
import com.controltier.ctl.utils.FileUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

/**
 * ControlTier Software Inc. User: alexh Date: Jul 21, 2005 Time: 6:26:12 PM
 */
public class TestFrameworkProject extends CtlTest {

    private final String PROJECT_NAME = "TestFrameworkProject";

    File projectBasedir;

    File nodesfile;

    public TestFrameworkProject(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestFrameworkProject.class);
    }

    protected void setUp() {
        super.setUp();
        projectBasedir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        nodesfile = new File(projectBasedir, "/etc/resources.xml");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FileUtils.deleteDir(projectdir);
    }

    public void testCreateDepotStructure() throws IOException {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        if (projectDir.exists()) {
            FileUtils.deleteDir(projectDir);
        }
        FrameworkProject.createFileStructure(projectDir, false);
        assertTrue(new File(projectDir, FrameworkProject.ETC_DIR_NAME).exists());
        final File projectModsDir = new File(projectDir, "modules");
        assertFalse(projectModsDir.exists());
        FrameworkProject.createFileStructure(projectDir, true);
        assertTrue(projectModsDir.exists());
    }

    public void testConstruction() {
        if (projectBasedir.exists()) {
            projectBasedir.delete();
        }
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        assertTrue("incorrect project.dir", project.getBaseDir().equals(new File(getFrameworkProjectsBase(), PROJECT_NAME)));
        assertTrue("Incorrect deployments.dir ", project.getDeploymentsBaseDir().equals(new File(project.getBaseDir(), "resources")));
        assertTrue("number of types: " + project.listChildren().size() + " should be 0", project.listChildren().size() == 0);
    }

    public void testAddRemoveType() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        final FrameworkType t = project.createType("HahaType");
        assertNotNull(t);
        final FrameworkType th = (FrameworkType) project.getChild("HahaType");
        assertEquals("HahaType", th.getName());
        assertTrue(project.existsChild("HahaType"));
        assertEquals(1, project.listChildren().size());
        final FrameworkType ti = (FrameworkType) project.listChildren().iterator().next();
        assertEquals("HahaType", ti.getName());
        project.remove("HahaType");
        assertFalse(project.existsChild("HahaType"));
    }

    public void testChildCouldBeLoaded() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        assertFalse(project.childCouldBeLoaded("HahaType"));
        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        hahadir.mkdirs();
        assertTrue(project.childCouldBeLoaded("HahaType"));
        hahadir.delete();
        assertFalse(project.childCouldBeLoaded("HahaType"));
    }

    public void testListChildNames() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        assertEquals(0, project.listChildNames().size());
        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        hahadir.mkdirs();
        assertEquals(1, project.listChildNames().size());
        assertTrue(project.listChildNames().contains("HahaType"));
        hahadir.delete();
        assertEquals(0, project.listChildNames().size());
    }

    public void testLoadChild() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        assertEquals(project.listChildNames().size(), 0);
        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        assertFalse(project.childCouldBeLoaded("HahaType"));
        try {
            IFrameworkResource res1 = project.loadChild("HahaType");
            fail("should not load type");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }
        hahadir.mkdirs();
        assertTrue(project.childCouldBeLoaded("HahaType"));
        final IFrameworkResource res1 = project.loadChild("HahaType");
        assertNotNull(res1);
        assertEquals(res1.getName(), "HahaType");
        assertTrue(res1 instanceof FrameworkType);
        hahadir.delete();
    }

    public void testGetFrameworkProject() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        assertEquals(project.listChildNames().size(), 0);
        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        assertFalse(project.childCouldBeLoaded("HahaType"));
        try {
            FrameworkType res1 = project.getFrameworkType("HahaType");
            fail("should not load type");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }
        hahadir.mkdirs();
        assertTrue(project.childCouldBeLoaded("HahaType"));
        final FrameworkType res1 = project.getFrameworkType("HahaType");
        assertNotNull(res1);
        assertEquals(res1.getName(), "HahaType");
        assertTrue(res1 instanceof FrameworkType);
        hahadir.delete();
    }

    public void testGetChild() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        assertEquals(project.listChildNames().size(), 0);
        final File deployments = new File(project.getBaseDir(), "resources");
        final File hahadir = new File(deployments, "HahaType");
        assertFalse(project.childCouldBeLoaded("HahaType"));
        try {
            IFrameworkResource res1 = project.getChild("HahaType");
            fail("should not load type");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }
        hahadir.mkdirs();
        assertTrue(project.childCouldBeLoaded("HahaType"));
        final IFrameworkResource res1 = project.getChild("HahaType");
        assertNotNull(res1);
        assertEquals(res1.getName(), "HahaType");
        assertTrue(res1 instanceof FrameworkType);
        hahadir.delete();
    }

    public void testProperties() throws IOException {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FrameworkProject.createFileStructure(projectDir, false);
        final File etcDir = new File(projectDir, "etc");
        final File projectPropertyFile = new File(etcDir, "project.properties");
        final Properties p = new Properties();
        p.put("project.dir", "${framework.projects.dir}/${project.name}");
        p.put("project.resources.dir", "${project.dir}/resources");
        p.put("project.etc.dir", "${project.dir}/etc");
        p.put("project.resources.file", "${project.etc.dir}/resources.properties");
        p.store(new FileOutputStream(projectPropertyFile), "test properties");
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        assertEquals(project.getProperty("project.dir"), projectDir.getAbsolutePath());
    }

    public void testModuleLib() throws Throwable {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        FrameworkProject.createFileStructure(projectDir, true);
        final File projectModsDir = new File(projectDir, "modules");
        final File etcDir = new File(projectDir, FrameworkProject.ETC_DIR_NAME);
        final File templateBasedir = new File("src/templates/ant");
        final CtlGenMain creator = CtlGenMain.create(templateBasedir, projectModsDir);
        creator.executeAction("module", "add", new String[] { "-m", "NewModule1" });
        creator.executeAction("module", "add", new String[] { "-m", "NewModule2" });
        final Properties p = new Properties();
        p.setProperty("modules.dir", projectModsDir.getAbsolutePath());
        final File modPropsFile = new File(etcDir, PROJECT_NAME + " modules.properties");
        p.store(new FileOutputStream(modPropsFile), "modules.properties");
        final IFrameworkProjectMgr projectMgr = getFrameworkInstance().getFrameworkProjectMgr();
        final IModuleLookup moduleLookup = projectMgr.createModuleLookup(PROJECT_NAME, projectModsDir, true, false);
        assertTrue(moduleLookup.existsCmdModule("NewModule1"));
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), projectMgr, moduleLookup);
        assertEquals(project.getModulesDir().getAbsolutePath(), projectModsDir.getAbsolutePath());
        assertTrue(project.getModuleLookup().existsCmdModule("NewModule1"));
    }

    public void testHasOwnModuleLib() throws IOException {
        final File projectDir = new File(getFrameworkProjectsBase(), PROJECT_NAME);
        if (projectDir.exists()) {
            FileUtils.deleteDir(projectDir);
        }
        FrameworkProject.createFileStructure(projectDir, false);
        assertFalse("expected project to not have a configured module lib", FrameworkProject.hasConfiguredModuleLib(projectDir));
        FileUtils.deleteDir(projectDir);
        FrameworkProject.createFileStructure(projectDir, true);
        assertTrue("expected project to have a configured module lib", FrameworkProject.hasConfiguredModuleLib(projectDir));
    }

    public void testGetFrameworkResourceInstances() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        final Collection types = new ArrayList();
        types.add(project.createType("Type1"));
        types.add(project.createType("Type2"));
        types.add(project.createType("Type3"));
        for (Iterator typesIter = types.iterator(); typesIter.hasNext(); ) {
            final FrameworkType type = (FrameworkType) typesIter.next();
            type.createObject("object1", false);
            type.createObject("object2", false);
            type.createObject("object3", false);
        }
        final Collection projectObjects = project.getFrameworkResourceInstances();
        assertTrue("wrong number of expected objects in project", projectObjects.size() == 9);
        assertTrue("wrong number of expected types in project", project.listChildren().size() == 3);
        assertTrue("Type1 type not found in project", project.existsChild("Type1"));
        assertTrue("Type2 type not found in project", project.existsChild("Type1"));
        assertTrue("Type3 type not found in project", project.existsChild("Type1"));
        for (Iterator typesIter = types.iterator(); typesIter.hasNext(); ) {
            final FrameworkType type = (FrameworkType) typesIter.next();
            assertTrue("object1 object not found", type.existsChild("object1"));
            assertTrue("object2 object not found", type.existsChild("object2"));
            assertTrue("object3 object not found", type.existsChild("object3"));
        }
    }

    public void testGetNodes() throws Exception {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        FileUtils.copyFileStreams(new File("src/test/com/controltier/ctl/common/test-nodes1.xml"), nodesfile);
        assertTrue(nodesfile.exists());
        Nodes nodes = project.getNodes();
        assertNotNull(nodes);
        assertEquals("nodes was incorrect size", 2, nodes.listNodes().size());
        assertTrue("nodes did not have correct test node1", nodes.hasNode("testnode1"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode2"));
    }

    public void testGetResourcesUrl() {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        final String davurl = getFrameworkInstance().getProperty("framework.webdav.uri");
        assertEquals("incorrect resource url: " + project.getProperty("project.resources.url"), project.getProperty("project.resources.url"), davurl + "/projects/" + PROJECT_NAME + "/etc/resources.xml");
        assertEquals("incorrect resource url: " + project.getProperty("project.nodes.url"), project.getProperty("project.nodes.url"), davurl + "/projects/" + PROJECT_NAME + "/etc/resources.xml");
    }

    public void testGenerateProjectPropertiesFile() throws IOException {
        final FrameworkProject project = FrameworkProject.create(PROJECT_NAME, new File(getFrameworkProjectsBase()), getFrameworkInstance().getFrameworkProjectMgr());
        boolean overwrite = true;
        project.generateProjectPropertiesFile(overwrite);
        final File propFile = new File(project.getEtcDir(), "project.properties");
        assertTrue("project.properties file was not generated", propFile.exists());
        final Properties p = new Properties();
        p.load(new FileInputStream(propFile));
        assertTrue(p.containsKey("project.resources.file"));
        assertTrue(p.containsKey("project.resources.url"));
        System.out.println("TEST: propertyFile=" + project.getPropertyFile());
    }
}
