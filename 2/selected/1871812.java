package org.jcrom;

import java.io.File;
import java.net.URL;
import java.util.logging.LogManager;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.core.TransientRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Thanks to Vincent Gigure for providing this test case.
 *
 * @author Vincent Gigure
 */
public class TestInstantiation {

    private Repository repo;

    private Session session;

    @Before
    public void setUpRepository() throws Exception {
        repo = (Repository) new TransientRepository();
        session = repo.login(new SimpleCredentials("a", "b".toCharArray()));
        ClassLoader loader = TestMapping.class.getClassLoader();
        URL url = loader.getResource("logger.properties");
        if (url == null) {
            url = loader.getResource("/logger.properties");
        }
        LogManager.getLogManager().readConfiguration(url.openStream());
    }

    @After
    public void tearDownRepository() throws Exception {
        session.logout();
        deleteDir(new File("repository"));
        new File("repository.xml").delete();
        new File("derby.log").delete();
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    @Test
    public void test_dynamic_map_instantiation() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(DynamicObject.class).map(ReferencedEntity.class);
        Node rootNode = session.getRootNode().addNode("test");
        DynamicObject dyna = new DynamicObject();
        dyna.setName("Dynamic");
        ReferencedEntity child = new ReferencedEntity();
        child.setName("Child");
        dyna.putSingleReference("childOne", jcrom.fromNode(ReferencedEntity.class, jcrom.addNode(rootNode, child, new String[] { "mix:referenceable" })));
        dyna.putSingleReference("childTwo", jcrom.fromNode(ReferencedEntity.class, jcrom.addNode(rootNode, child, new String[] { "mix:referenceable" })));
        assertEquals(2, dyna.getSingleReferences().size());
        assertNotNull(dyna.getSingleReferences().get("childOne"));
        assertNotNull(dyna.getSingleReferences().get("childTwo"));
        DynamicObject loaded = jcrom.fromNode(DynamicObject.class, jcrom.addNode(rootNode, dyna));
        assertEquals(2, loaded.getSingleReferences().size());
        assertNotNull(loaded.getSingleReferences().get("childOne"));
        assertNotNull(loaded.getSingleReferences().get("childTwo"));
    }

    @Test
    public void test_dynamic_maps_stored_as_child_nodes_can_be_retrieved_by_key() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(DynamicObject.class).map(Child.class);
        Node rootNode = session.getRootNode().addNode("test");
        DynamicObject dyna = new DynamicObject();
        dyna.setName("Dynamic");
        dyna.putSingleValueChild("childOne", createChildWithName("childName1"));
        dyna.putSingleValueChild("childTwo", createChildWithName("childName2"));
        dyna.putSingleValueChild("childThree", createChildWithName("childName3"));
        assertEquals(3, dyna.getSingleValueChildren().size());
        DynamicObject loaded = jcrom.fromNode(DynamicObject.class, jcrom.addNode(rootNode, dyna));
        assertEquals(3, loaded.getSingleValueChildren().size());
        assertNotNull(loaded.getSingleValueChildren().get("childOne"));
        assertNotNull(loaded.getSingleValueChildren().get("childTwo"));
        assertNotNull(loaded.getSingleValueChildren().get("childThree"));
    }

    private Child createChildWithName(String name) {
        Child child = new Child();
        child.setName(name);
        return child;
    }
}
