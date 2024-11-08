package edu.ua.j3dengine.core.test;

import edu.ua.j3dengine.core.*;
import edu.ua.j3dengine.core.geometry.impl.ModelAdapterGeometry;
import edu.ua.j3dengine.core.behavior.InertBehavior;
import edu.ua.j3dengine.core.mgmt.GameObjectManager;
import edu.ua.j3dengine.core.mgmt.WorldInitializationException;
import edu.ua.j3dengine.core.state.DynamicObjectState;
import edu.ua.j3dengine.core.state.StaticObjectState;
import static edu.ua.j3dengine.core.test.Resources.TEST_WORLD_FILE_PATH;
import static edu.ua.j3dengine.utils.Utils.logDebug;
import junit.framework.TestCase;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;

public class BasicStructureMarshallingTest extends TestCase {

    public BasicStructureMarshallingTest() {
        super("BasicStructureMarshallingTest");
    }

    public void testBasicWorldSerialization() throws JAXBException {
        World world = createStructure();
        JAXBContext ctx = GameObjectManager.createJAXBContext();
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter writer = new StringWriter();
        m.marshal(world, writer);
        logDebug(writer.toString());
        StringReader reader = new StringReader(writer.toString());
        Unmarshaller um = ctx.createUnmarshaller();
        Object o = um.unmarshal(reader);
        verifySimilarity(o, world);
    }

    public void testWorldLoading() throws WorldInitializationException {
        GameObjectManager manager = GameObjectManager.getInstance();
        manager.loadWorld(TEST_WORLD_FILE_PATH);
        World world = createStructure();
        World loadedWorld = manager.getWorld();
        verifySimilarity(world, loadedWorld);
        boolean ok = false;
        try {
            manager.loadWorld(TEST_WORLD_FILE_PATH);
        } catch (Exception e) {
            ok = true;
        }
        assertTrue("Second world loading should have failed.", ok);
        DynamicGameObject object = (DynamicGameObject) manager.lookupGameObject("dObject1");
        System.out.println("object.getGeometry().getLocation() = " + object.getGeometry().getLocation());
        assertNotNull("Geometry should have been loaded.", object.getGeometry());
    }

    private static World createStructure() {
        GameObject go1 = new StaticGameObject("sObject1");
        go1.addAttribute(new Attribute<String>("object_Attr1", "object_Attr1_value"));
        StaticObjectState state = new StaticObjectState("sObject1_state1");
        state.addAttribute(new Attribute<String>("sObject1_state1_prop1", "val1"));
        state.addAttribute(new Attribute<Double>("sObject1_state1_prop2", 3.66));
        go1.addState(state);
        state = new StaticObjectState("sObject1_state2");
        state.addAttribute(new Attribute<Integer>("sObject1_state2_prop1", 5));
        go1.addState(state);
        go1.setInitialState(state.getName());
        GameObject go2 = new StaticGameObject("sObject2");
        go2.addAttribute(new Attribute<String>("sObject2_attr1", "sObject2_attr1_value"));
        DynamicGameObject do1 = new DynamicGameObject("dObject1");
        ModelAdapterGeometry geom = new ModelAdapterGeometry("resources\\3ds\\drazinsunhawk\\Drazisunhawk1.1.3ds", "DraziSunHa");
        do1.setGeometry(geom);
        do1.addAttribute(new Attribute<Long>("dObject1_attr1", 2000000L));
        DynamicObjectState state2 = new DynamicObjectState("dObject1_state1", null, null, new InertBehavior("dObject1_behavior1"));
        do1.addState(state2);
        do1.setInitialState(state2.getName());
        World world = World.create("MyWorld");
        world.initialize();
        world.addGameObject(go1);
        world.addGameObject(go2);
        world.addGameObject(do1);
        return world;
    }

    public static void main(String[] args) throws JAXBException, IOException {
        JAXBContext ctx = GameObjectManager.createJAXBContext();
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        File file = new File(TEST_WORLD_FILE_PATH);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        m.marshal(createStructure(), writer);
        writer.flush();
        writer.close();
    }

    private void saveStructure(String structure) throws IOException {
    }

    private void verifySimilarity(Object o, World world) {
        assertEquals(o, world);
        String name = "dObject1";
        DynamicGameObject dgo = (DynamicGameObject) world.getGameObject(name);
        assertNotNull("There should be a DynamicObject named after '" + name + "'", dgo);
        String behaviorName = "dObject1_behavior1";
        assertEquals("DynamicObject should have a behavior named '" + behaviorName + "'", dgo.getBehavior().getName(), behaviorName);
        assertTrue("Behavior should be of type '" + InertBehavior.class + "'", dgo.getBehavior().getClass() == InertBehavior.class);
    }
}
