package com.controltier.ctl.common;

import junit.framework.Test;
import junit.framework.TestSuite;
import com.controltier.ctl.cli.CtlGenMain;
import com.controltier.ctl.tools.CtlTest;
import com.controltier.ctl.utils.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

/**
 * ControlTier Software Inc. User: alexh Date: Jul 21, 2005 Time: 6:26:12 PM
 */
public class TestDepot extends CtlTest {

    private final String DEPOT_NAME = "TestDepot";

    File depotBasedir;

    File nodesfile;

    public TestDepot(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return new TestSuite(TestDepot.class);
    }

    protected void setUp() {
        super.setUp();
        depotBasedir = new File(getDepotsBase(), DEPOT_NAME);
        nodesfile = new File(depotBasedir, "/etc/nodes.properties");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        File depotdir = new File(getDepotsBase(), DEPOT_NAME);
        FileUtils.deleteDir(depotdir);
    }

    public void testCreateDepotStructure() throws IOException {
        final File depotDir = new File(getDepotsBase(), DEPOT_NAME);
        if (depotDir.exists()) {
            FileUtils.deleteDir(depotDir);
        }
        Depot.createDepotStructure(depotDir, false);
        assertTrue(new File(depotDir, Depot.ETC_DIR_NAME).exists());
        final File depotModsDir = new File(depotDir, "modules");
        assertFalse(depotModsDir.exists());
        Depot.createDepotStructure(depotDir, true);
        assertTrue(depotModsDir.exists());
    }

    public void testConstruction() {
        if (depotBasedir.exists()) {
            depotBasedir.delete();
        }
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        assertTrue("incorrect depot.dir", depot.getBaseDir().equals(new File(getDepotsBase(), DEPOT_NAME)));
        assertTrue("Incorrect deployments.dir ", depot.getDeploymentsBaseDir().equals(new File(depot.getBaseDir(), "deployments")));
        assertTrue("number of types: " + depot.listChildren().size() + " should be 0", depot.listChildren().size() == 0);
    }

    public void testAddRemoveType() {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        final DepotType t = depot.createType("HahaType");
        assertNotNull(t);
        final DepotType th = (DepotType) depot.getChild("HahaType");
        assertEquals("HahaType", th.getName());
        assertTrue(depot.existsChild("HahaType"));
        assertEquals(1, depot.listChildren().size());
        final DepotType ti = (DepotType) depot.listChildren().iterator().next();
        assertEquals("HahaType", ti.getName());
        depot.remove("HahaType");
        assertFalse(depot.existsChild("HahaType"));
    }

    public void testChildCouldBeLoaded() {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        assertFalse(depot.childCouldBeLoaded("HahaType"));
        final File deployments = new File(depot.getBaseDir(), "deployments");
        final File hahadir = new File(deployments, "HahaType");
        hahadir.mkdirs();
        assertTrue(depot.childCouldBeLoaded("HahaType"));
        hahadir.delete();
        assertFalse(depot.childCouldBeLoaded("HahaType"));
    }

    public void testListChildNames() {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        assertEquals(0, depot.listChildNames().size());
        final File deployments = new File(depot.getBaseDir(), "deployments");
        final File hahadir = new File(deployments, "HahaType");
        hahadir.mkdirs();
        assertEquals(1, depot.listChildNames().size());
        assertTrue(depot.listChildNames().contains("HahaType"));
        hahadir.delete();
        assertEquals(0, depot.listChildNames().size());
    }

    public void testLoadChild() {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        assertEquals(depot.listChildNames().size(), 0);
        final File deployments = new File(depot.getBaseDir(), "deployments");
        final File hahadir = new File(deployments, "HahaType");
        assertFalse(depot.childCouldBeLoaded("HahaType"));
        try {
            IFrameworkResource res1 = depot.loadChild("HahaType");
            fail("should not load type");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }
        hahadir.mkdirs();
        assertTrue(depot.childCouldBeLoaded("HahaType"));
        final IFrameworkResource res1 = depot.loadChild("HahaType");
        assertNotNull(res1);
        assertEquals(res1.getName(), "HahaType");
        assertTrue(res1 instanceof DepotType);
        hahadir.delete();
    }

    public void testGetDepotType() {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        assertEquals(depot.listChildNames().size(), 0);
        final File deployments = new File(depot.getBaseDir(), "deployments");
        final File hahadir = new File(deployments, "HahaType");
        assertFalse(depot.childCouldBeLoaded("HahaType"));
        try {
            DepotType res1 = depot.getDepotType("HahaType");
            fail("should not load type");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }
        hahadir.mkdirs();
        assertTrue(depot.childCouldBeLoaded("HahaType"));
        final DepotType res1 = depot.getDepotType("HahaType");
        assertNotNull(res1);
        assertEquals(res1.getName(), "HahaType");
        assertTrue(res1 instanceof DepotType);
        hahadir.delete();
    }

    public void testGetChild() {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        assertEquals(depot.listChildNames().size(), 0);
        final File deployments = new File(depot.getBaseDir(), "deployments");
        final File hahadir = new File(deployments, "HahaType");
        assertFalse(depot.childCouldBeLoaded("HahaType"));
        try {
            IFrameworkResource res1 = depot.getChild("HahaType");
            fail("should not load type");
        } catch (FrameworkResourceParent.NoSuchResourceException e) {
            assertNotNull(e);
        }
        hahadir.mkdirs();
        assertTrue(depot.childCouldBeLoaded("HahaType"));
        final IFrameworkResource res1 = depot.getChild("HahaType");
        assertNotNull(res1);
        assertEquals(res1.getName(), "HahaType");
        assertTrue(res1 instanceof DepotType);
        hahadir.delete();
    }

    public void testProperties() throws IOException {
        final File depotDir = new File(getDepotsBase(), DEPOT_NAME);
        Depot.createDepotStructure(depotDir, false);
        final File etcDir = new File(depotDir, "etc");
        final File depotPropertyFile = new File(etcDir, "depot.properties");
        final Properties p = new Properties();
        p.put("depot.dir", "${framework.depots.dir}/${depot.name}");
        p.put("depot.deployments.dir", "${depot.dir}/deployments");
        p.put("depot.etc.dir", "${depot.dir}/etc");
        p.put("depot.deployments.file", "${depot.etc.dir}/deployments.properties");
        p.store(new FileOutputStream(depotPropertyFile), "test properties");
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        assertEquals(depot.getProperty("depot.dir"), depotDir.getAbsolutePath());
    }

    public void testModuleLib() throws Throwable {
        final File depotDir = new File(getDepotsBase(), DEPOT_NAME);
        Depot.createDepotStructure(depotDir, true);
        final File depotModsDir = new File(depotDir, "modules");
        final File etcDir = new File(depotDir, Depot.ETC_DIR_NAME);
        final File templateBasedir = new File("src/templates/ant");
        final CtlGenMain creator = CtlGenMain.create(templateBasedir, depotModsDir);
        creator.executeAction("module", "add", new String[] { "-m", "NewModule1" });
        creator.executeAction("module", "add", new String[] { "-m", "NewModule2" });
        final Properties p = new Properties();
        p.setProperty("modules.dir", depotModsDir.getAbsolutePath());
        final File modPropsFile = new File(etcDir, DEPOT_NAME + " modules.properties");
        p.store(new FileOutputStream(modPropsFile), "modules.properties");
        final IDepotMgr depotMgr = getFrameworkInstance().getDepotResourceMgr();
        final IModuleLookup moduleLookup = depotMgr.createModuleLookup(DEPOT_NAME, depotModsDir, true, false);
        assertTrue(moduleLookup.existsCmdModule("NewModule1"));
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), depotMgr, moduleLookup);
        assertEquals(depot.getModulesDir().getAbsolutePath(), depotModsDir.getAbsolutePath());
        assertTrue(depot.getModuleLookup().existsCmdModule("NewModule1"));
    }

    public void testHasOwnModuleLib() throws IOException {
        final File depotDir = new File(getDepotsBase(), DEPOT_NAME);
        if (depotDir.exists()) {
            FileUtils.deleteDir(depotDir);
        }
        Depot.createDepotStructure(depotDir, false);
        assertFalse("expected depot to not have a configured module lib", Depot.hasConfiguredModuleLib(depotDir));
        FileUtils.deleteDir(depotDir);
        Depot.createDepotStructure(depotDir, true);
        assertTrue("expected depot to have a configured module lib", Depot.hasConfiguredModuleLib(depotDir));
    }

    public void testGetDepotObjects() {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        final Collection types = new ArrayList();
        types.add(depot.createType("Type1"));
        types.add(depot.createType("Type2"));
        types.add(depot.createType("Type3"));
        for (Iterator typesIter = types.iterator(); typesIter.hasNext(); ) {
            final DepotType type = (DepotType) typesIter.next();
            type.createObject("object1", false);
            type.createObject("object2", false);
            type.createObject("object3", false);
        }
        final Collection depotObjects = depot.getDepotObjects();
        assertTrue("wrong number of expected objects in depot", depotObjects.size() == 9);
        assertTrue("wrong number of expected types in depot", depot.listChildren().size() == 3);
        assertTrue("Type1 type not found in depot", depot.existsChild("Type1"));
        assertTrue("Type2 type not found in depot", depot.existsChild("Type1"));
        assertTrue("Type3 type not found in depot", depot.existsChild("Type1"));
        for (Iterator typesIter = types.iterator(); typesIter.hasNext(); ) {
            final DepotType type = (DepotType) typesIter.next();
            assertTrue("object1 object not found", type.existsChild("object1"));
            assertTrue("object2 object not found", type.existsChild("object2"));
            assertTrue("object3 object not found", type.existsChild("object3"));
        }
    }

    public void testGetNodes() throws Exception {
        final Depot depot = Depot.create(DEPOT_NAME, new File(getDepotsBase()), getFrameworkInstance().getDepotResourceMgr());
        FileUtils.copyFileStreams(new File("src/test/com/controltier/ctl/common/test-nodes1.properties"), nodesfile);
        assertTrue(nodesfile.exists());
        Nodes nodes = depot.getNodes();
        assertNotNull(nodes);
        assertEquals("nodes was incorrect size", 2, nodes.listNodes().size());
        assertTrue("nodes did not have correct test node1", nodes.hasNode("testnode1"));
        assertTrue("nodes did not have correct test node2", nodes.hasNode("testnode2"));
    }
}
