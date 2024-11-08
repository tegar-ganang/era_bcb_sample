package org.jcrom;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.logging.LogManager;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import org.apache.jackrabbit.core.TransientRepository;
import org.jcrom.entities.First;
import org.jcrom.entities.Second;
import org.jcrom.util.NodeFilter;
import org.junit.After;
import org.junit.Before;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
public class TestMapping {

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

    private Parent createParent(String name) {
        Parent parent = new Parent();
        parent.setTitle(name);
        parent.setBirthDay(new Date());
        parent.setDrivingLicense(true);
        parent.setFingers(10);
        parent.setHairs(0L);
        parent.setHeight(1.80);
        parent.setWeight(83.54);
        parent.setNickName("Daddy");
        parent.addTag("father");
        parent.addTag("parent");
        parent.addTag("male");
        return parent;
    }

    private Child createChild(String name) {
        Child child = new Child();
        child.setTitle(name);
        child.setBirthDay(new Date());
        child.setDrivingLicense(false);
        child.setFingers(11);
        child.setHairs(130000L);
        child.setHeight(1.93);
        child.setWeight(90.45);
        child.setNickName("Baby");
        return child;
    }

    private GrandChild createGrandChild(String name) {
        GrandChild grandChild = new GrandChild();
        grandChild.setTitle(name);
        grandChild.setBirthDay(new Date());
        grandChild.setDrivingLicense(false);
        grandChild.setFingers(12);
        grandChild.setHairs(130000L);
        grandChild.setHeight(1.37);
        grandChild.setWeight(42.17);
        grandChild.setNickName("Zima");
        return grandChild;
    }

    static JcrFile createFile(String name) throws Exception {
        return createFile(name, false);
    }

    static JcrFile createFile(String name, boolean stream) throws Exception {
        JcrFile jcrFile = new JcrFile();
        jcrFile.setName(name);
        jcrFile.setMimeType("image/jpeg");
        jcrFile.setEncoding("UTF-8");
        File imageFile = new File("src/test/resources/ogg.jpg");
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(imageFile.lastModified());
        jcrFile.setLastModified(lastModified);
        if (stream) {
            jcrFile.setDataProvider(new JcrDataProviderImpl(new FileInputStream(imageFile)));
        } else {
            jcrFile.setDataProvider(new JcrDataProviderImpl(imageFile));
        }
        return jcrFile;
    }

    private Photo createPhoto(String name) throws Exception {
        Photo jcrFile = new Photo();
        jcrFile.setName(name);
        jcrFile.setMimeType("image/jpeg");
        jcrFile.setOriginalFilename(name);
        File imageFile = new File("src/test/resources/ogg.jpg");
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(imageFile.lastModified());
        jcrFile.setLastModified(lastModified);
        jcrFile.setFileSize(imageFile.length());
        jcrFile.setPhotographer("Testino");
        jcrFile.setChild(createParent("Kate"));
        jcrFile.setDataProvider(new JcrDataProviderImpl(imageFile));
        jcrFile.setFileBytes(readBytes(new FileInputStream(imageFile)));
        return jcrFile;
    }

    private byte[] readBytes(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            in.close();
            out.close();
        }
        return out.toByteArray();
    }

    static void printNode(Node node, String indentation) throws Exception {
        System.out.println();
        System.out.println(indentation + "------- NODE -------");
        System.out.println(indentation + "Path: " + node.getPath());
        System.out.println(indentation + "------- Properties: ");
        PropertyIterator propertyIterator = node.getProperties();
        while (propertyIterator.hasNext()) {
            Property p = propertyIterator.nextProperty();
            if (!p.getName().equals("jcr:data") && !p.getName().equals("jcr:mixinTypes") && !p.getName().equals("fileBytes")) {
                System.out.print(indentation + p.getName() + ": ");
                if (p.getDefinition().getRequiredType() == PropertyType.BINARY) {
                    System.out.print("binary, (length:" + p.getLength() + ") ");
                } else if (!p.getDefinition().isMultiple()) {
                    System.out.print(p.getString());
                } else {
                    for (Value v : p.getValues()) {
                        System.out.print(v.getString() + ", ");
                    }
                }
                System.out.println();
            }
            if (p.getName().equals("jcr:childVersionHistory")) {
                System.out.println(indentation + "------- CHILD VERSION HISTORY -------");
                printNode(node.getSession().getNodeByUUID(p.getString()), indentation + "\t");
                System.out.println(indentation + "------- CHILD VERSION ENDS -------");
            }
        }
        NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            printNode(nodeIterator.nextNode(), indentation + "\t");
        }
    }

    @Test
    public void testEnums() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(EnumEntity.class);
        EnumEntity.Suit[] suitArray = new EnumEntity.Suit[2];
        suitArray[0] = EnumEntity.Suit.HEARTS;
        suitArray[1] = EnumEntity.Suit.CLUBS;
        EnumEntity enumEntity = new EnumEntity();
        enumEntity.setName("MySuit");
        enumEntity.setSuit(EnumEntity.Suit.DIAMONDS);
        enumEntity.setSuitAsArray(suitArray);
        enumEntity.addSuitToList(EnumEntity.Suit.SPADES);
        Node rootNode = session.getRootNode().addNode("enumTest");
        Node newNode = jcrom.addNode(rootNode, enumEntity);
        session.save();
        EnumEntity fromNode = jcrom.fromNode(EnumEntity.class, newNode);
        assertEquals(fromNode.getSuit(), enumEntity.getSuit());
        assertTrue(fromNode.getSuitAsArray().length == enumEntity.getSuitAsArray().length);
        assertTrue(fromNode.getSuitAsArray()[0].equals(enumEntity.getSuitAsArray()[0]));
        assertTrue(fromNode.getSuitAsList().size() == enumEntity.getSuitAsList().size());
        assertTrue(fromNode.getSuitAsList().get(0).equals(enumEntity.getSuitAsList().get(0)));
    }

    @Test(expected = JcrMappingException.class)
    public void mapInvalidObject() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(InvalidEntity.class);
    }

    @Test
    public void serializedProperties() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(EntityWithSerializedProperties.class);
        EntityWithSerializedProperties entity = new EntityWithSerializedProperties();
        entity.setName("withSerializedProperties");
        Parent parent = createParent("John");
        entity.setParent(parent);
        Node rootNode = session.getRootNode().addNode("mapChildTest");
        Node newNode = jcrom.addNode(rootNode, entity);
        session.save();
        EntityWithSerializedProperties entityFromJcr = jcrom.fromNode(EntityWithSerializedProperties.class, newNode);
        assertTrue(entityFromJcr.getParent().getName().equals(entity.getParent().getName()));
        assertTrue(entityFromJcr.getParent().getBirthDay().equals(entity.getParent().getBirthDay()));
        assertTrue(entityFromJcr.getParent().getHeight() == entity.getParent().getHeight());
    }

    @Test
    public void mapsAsChildren() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(EntityWithMapChildren.class);
        Integer[] myIntArr1 = { 1, 2, 3 };
        Integer[] myIntArr2 = { 4, 5, 6 };
        int[] myIntArr3 = { 7, 8, 9 };
        int myInt1 = 1;
        int myInt2 = 2;
        String[] myStringArr1 = { "a", "b", "c" };
        String[] myStringArr2 = { "d", "e", "f" };
        String[] myStringArr3 = { "h", "i", "j" };
        String myString1 = "string1";
        String myString2 = "string2";
        Locale locale = Locale.ITALIAN;
        Locale[] locales = { Locale.FRENCH, Locale.GERMAN };
        EntityWithMapChildren entity = new EntityWithMapChildren();
        entity.setName("mapEntity");
        entity.addIntegerArray("myIntArr1", myIntArr1);
        entity.addIntegerArray("myIntArr2", myIntArr2);
        entity.setMultiInt(myIntArr3);
        entity.addStringArray("myStringArr1", myStringArr1);
        entity.addStringArray("myStringArr2", myStringArr2);
        entity.setMultiString(myStringArr3);
        entity.addString("myString1", myString1);
        entity.addString("myString2", myString2);
        entity.addInteger("myInt1", myInt1);
        entity.addInteger("myInt2", myInt2);
        entity.setLocale(locale);
        entity.setMultiLocale(locales);
        Node rootNode = session.getRootNode().addNode("mapChildTest");
        Node newNode = jcrom.addNode(rootNode, entity);
        session.save();
        EntityWithMapChildren entityFromJcr = jcrom.fromNode(EntityWithMapChildren.class, newNode);
        assertTrue(entityFromJcr.getIntegers().equals(entity.getIntegers()));
        assertTrue(entityFromJcr.getStrings().equals(entity.getStrings()));
        assertTrue(entityFromJcr.getMultiInt().length == entity.getMultiInt().length);
        assertTrue(entityFromJcr.getMultiInt()[1] == myIntArr3[1]);
        assertTrue(entityFromJcr.getMultiString().length == entity.getMultiString().length);
        assertTrue(entityFromJcr.getIntegerArrays().size() == entity.getIntegerArrays().size());
        assertTrue(entityFromJcr.getIntegerArrays().get("myIntArr1").length == myIntArr1.length);
        assertTrue(entityFromJcr.getIntegerArrays().get("myIntArr2").length == myIntArr2.length);
        assertTrue(entityFromJcr.getIntegerArrays().get("myIntArr1")[1] == myIntArr1[1]);
        assertTrue(entityFromJcr.getStringArrays().size() == entity.getStringArrays().size());
        assertTrue(entityFromJcr.getStringArrays().get("myStringArr1").length == myStringArr1.length);
        assertTrue(entityFromJcr.getStringArrays().get("myStringArr2").length == myStringArr2.length);
        assertTrue(entityFromJcr.getStringArrays().get("myStringArr1")[1].equals(myStringArr1[1]));
        assertTrue(entityFromJcr.getLocale().equals(locale));
        assertTrue(entityFromJcr.getMultiLocale().length == entity.getMultiLocale().length);
        assertTrue(entityFromJcr.getMultiLocale()[1].equals(locales[1]));
    }

    @Test
    public void dynamicInstantiation() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(Circle.class).map(Rectangle.class).map(ShapeParent.class).map(Square.class);
        Shape circle = new Circle(5);
        circle.setName("circle");
        Shape rectangle = new Rectangle(5, 5);
        rectangle.setName("rectangle");
        Node rootNode = session.getRootNode().addNode("dynamicInstTest");
        Node circleNode = jcrom.addNode(rootNode, circle);
        Node rectangleNode = jcrom.addNode(rootNode, rectangle);
        session.save();
        Shape circleFromNode = jcrom.fromNode(Shape.class, circleNode);
        Shape rectangleFromNode = jcrom.fromNode(Shape.class, rectangleNode);
        assertTrue(circleFromNode.getArea() == circle.getArea());
        assertTrue(rectangleFromNode.getArea() == rectangle.getArea());
        Shape circle1 = new Circle(1);
        circle1.setName("circle1");
        Shape circle2 = new Circle(2);
        circle2.setName("circle2");
        Shape rectangle1 = new Rectangle(2, 2);
        rectangle1.setName("rectangle");
        Shape square = new Square(3, 3);
        square.setName("square");
        ShapeParent shapeParent = new ShapeParent();
        shapeParent.setName("ShapeParent");
        shapeParent.addShape(circle1);
        shapeParent.addShape(rectangle1);
        shapeParent.addShape(square);
        shapeParent.setMainShape(circle2);
        Node shapeParentNode = jcrom.addNode(rootNode, shapeParent);
        session.save();
        ShapeParent fromNode = jcrom.fromNode(ShapeParent.class, shapeParentNode);
        assertTrue(fromNode.getMainShape().getArea() == shapeParent.getMainShape().getArea());
        assertTrue(fromNode.getShapes().size() == shapeParent.getShapes().size());
        assertTrue(fromNode.getShapes().get(0).getArea() == shapeParent.getShapes().get(0).getArea());
        assertTrue(fromNode.getShapes().get(1).getArea() == shapeParent.getShapes().get(1).getArea());
        assertTrue(fromNode.getShapes().get(2).getArea() == shapeParent.getShapes().get(2).getArea());
    }

    @Test
    public void references() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(ReferenceContainer.class);
        jcrom.map(Rectangle.class);
        ReferencedEntity reference = new ReferencedEntity();
        reference.setName("myReference");
        reference.setBody("myBody");
        ReferencedEntity reference2 = new ReferencedEntity();
        reference2.setName("Reference2");
        reference2.setBody("Body of ref 2.");
        Rectangle rectangle = new Rectangle(2, 3);
        rectangle.setName("rectangle");
        Node rootNode = session.getRootNode().addNode("referenceTest");
        jcrom.addNode(rootNode, reference);
        jcrom.addNode(rootNode, reference2);
        jcrom.addNode(rootNode, rectangle);
        session.save();
        ReferenceContainer refContainer = new ReferenceContainer();
        refContainer.setName("refContainer");
        refContainer.setReference(reference);
        refContainer.addReference(reference);
        refContainer.addReference(reference2);
        refContainer.setReferenceByPath(reference);
        refContainer.addReferenceByPath(reference);
        refContainer.addReferenceByPath(reference2);
        refContainer.setShape(rectangle);
        Node refNode = jcrom.addNode(rootNode, refContainer);
        ReferenceContainer fromNode = jcrom.fromNode(ReferenceContainer.class, refNode);
        assertTrue(fromNode.getReference() != null);
        assertTrue(fromNode.getReference().getName().equals(reference.getName()));
        assertTrue(fromNode.getReference().getBody().equals(reference.getBody()));
        assertTrue(fromNode.getReferences().size() == 2);
        assertTrue(fromNode.getReferences().get(1).getName().equals(reference2.getName()));
        assertTrue(fromNode.getReferences().get(1).getBody().equals(reference2.getBody()));
        assertTrue(fromNode.getReferencesByPath().size() == 2);
        assertTrue(fromNode.getReferencesByPath().get(1).getName().equals(reference2.getName()));
        assertTrue(fromNode.getReferencesByPath().get(1).getBody().equals(reference2.getBody()));
        assertTrue(fromNode.getReferenceByPath() != null);
        assertTrue(fromNode.getReferenceByPath().getName().equals(reference.getName()));
        assertTrue(fromNode.getReferenceByPath().getBody().equals(reference.getBody()));
        assertTrue(fromNode.getShape().getArea() == rectangle.getArea());
    }

    @Test
    public void versioningDAO() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(VersionedEntity.class);
        Node rootNode = session.getRootNode().addNode("content").addNode("versionedEntities");
        VersionedDAO versionedDao = new VersionedDAO(session, jcrom);
        VersionedEntity entity = new VersionedEntity();
        entity.setTitle("MyEntity");
        entity.setBody("First");
        entity.setPath(rootNode.getPath());
        VersionedEntity child1 = new VersionedEntity();
        child1.setName("child1");
        child1.setBody("child1Body");
        VersionedEntity child2 = new VersionedEntity();
        child2.setName("child2");
        child2.setBody("child2Body");
        entity.addVersionedChild(child1);
        entity.addVersionedChild(child2);
        Child child3 = createChild("John");
        entity.addUnversionedChild(child3);
        VersionedEntity test = new VersionedEntity();
        test.setName("testChild");
        test.setBody("testBody");
        entity.versionedChild1 = test;
        VersionedEntity test2 = new VersionedEntity();
        test2.setName("testChild2");
        test2.setBody("testBody2");
        entity.versionedChild2 = test2;
        versionedDao.create(entity);
        entity.setBody("Second");
        entity.getUnversionedChildren().get(0).setNickName("Kalli");
        entity.getVersionedChildren().get(0).setBody("zimbob");
        entity.versionedChild1.setBody("mybody");
        entity.versionedChild2.setBody("mybody2");
        versionedDao.update(entity);
        entity.setBody("SecondSecond");
        versionedDao.update(entity);
        assertEquals(3, versionedDao.getVersionSize(entity.getPath()));
        VersionedEntity loadedEntity = versionedDao.get(entity.getPath());
        assertEquals("1.2", loadedEntity.getBaseVersionName());
        assertEquals("1.2", loadedEntity.getVersionName());
        assertEquals("Kalli", loadedEntity.getUnversionedChildren().get(0).getNickName());
        assertEquals("zimbob", loadedEntity.getVersionedChildren().get(0).getBody());
        assertEquals("mybody", loadedEntity.versionedChild1.getBody());
        assertEquals("mybody2", loadedEntity.versionedChild2.getBody());
        assertTrue(loadedEntity.getUnversionedChildren().size() == entity.getUnversionedChildren().size());
        assertTrue(versionedDao.getVersionList(entity.getPath()).size() == versionedDao.getVersionSize(entity.getPath()));
        VersionedEntity middleVersion = versionedDao.getVersion(entity.getPath(), "1.2");
        assertNotNull(middleVersion);
        versionedDao.restoreVersion(entity.getPath(), "1.0");
        loadedEntity = versionedDao.get(entity.getPath());
        assertTrue(loadedEntity.getBaseVersionName().equals("1.0"));
        Child loadedChild3 = loadedEntity.getUnversionedChildren().get(0);
        assertEquals("Baby", loadedChild3.getNickName());
        loadedEntity.setBody("Third");
        versionedDao.update(loadedEntity);
        VersionedEntity oldEntity = versionedDao.getVersion(entity.getPath(), "1.0");
        assertTrue(oldEntity != null);
        assertTrue(oldEntity.getBody().equals("First"));
        List<VersionedEntity> versions = versionedDao.getVersionList(entity.getPath());
        for (VersionedEntity version : versions) {
            System.out.println("Version [" + version.getVersionName() + "] [" + version.getBody() + "], base [" + version.getBaseVersionName() + "] [" + version.getBaseVersionCreated() + "]");
        }
        VersionedEntity anotherEntity = new VersionedEntity();
        anotherEntity.setName("anotherEntity");
        anotherEntity.setBody("anotherBody");
        versionedDao.create(rootNode.getPath(), anotherEntity);
        VersionedEntity childEntity = loadedEntity.getVersionedChildren().get(0);
        versionedDao.move(childEntity, anotherEntity.getPath() + "/versionedChildren");
        assertTrue(versionedDao.exists(anotherEntity.getPath() + "/versionedChildren/" + childEntity.getName()));
        versionedDao.remove(loadedEntity.getVersionedChildren().get(1).getPath());
        assertFalse(versionedDao.exists(loadedEntity.getVersionedChildren().get(1).getPath()));
        VersionedEntity addVersionedEntity = new VersionedEntity();
        addVersionedEntity.setName("newAddedEntity");
        addVersionedEntity.setBody("newAddedEntity");
        loadedEntity.getVersionedChildren().add(addVersionedEntity);
        versionedDao.update(loadedEntity);
        versionedDao.remove(loadedEntity.getPath());
    }

    /**
     * Thanks to Andrius Kurtinaitis for identifying this problem and
     * contributing this test case.
     * @throws Exception
     */
    @Test
    public void versioningDAOChild1() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(VersionedEntity.class);
        Node rootNode = session.getRootNode().addNode("content").addNode("versionedEntities");
        VersionedDAO versionedDao = new VersionedDAO(session, jcrom);
        VersionedEntity entity = new VersionedEntity();
        entity.setName("MyEntity");
        entity.setBody("First");
        entity.setPath(rootNode.getPath());
        VersionedEntity child = new VersionedEntity();
        child.setName("child1");
        child.setBody("child1Body");
        entity.versionedChild1 = child;
        versionedDao.create(entity);
        VersionedEntity fromNode = versionedDao.getVersion(entity.getPath(), "1.0");
        assertEquals(child.getBody(), fromNode.versionedChild1.getBody());
    }

    /**
     * Thanks to Andrius Kurtinaitis for identifying this problem and
     * contributing this test case.
     * @throws Exception
     */
    @Test
    public void versioningDAOChild2() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(VersionedEntity.class);
        Node rootNode = session.getRootNode().addNode("content").addNode("versionedEntities");
        VersionedDAO versionedDao = new VersionedDAO(session, jcrom);
        VersionedEntity entity = new VersionedEntity();
        entity.setTitle("MyEntity");
        entity.setBody("First");
        entity.setPath(rootNode.getPath());
        VersionedEntity child = new VersionedEntity();
        child.setName("child1");
        child.setBody("child1Body");
        entity.versionedChild2 = child;
        versionedDao.create(entity);
        assertEquals(child.getBody(), versionedDao.getVersion(entity.getPath(), "1.0").versionedChild2.getBody());
    }

    /**
     * Thanks to Andrius Kurtinaitis for identifying this problem and
     * contributing this test case.
     * @throws Exception
     */
    @Test
    public void versioningDAOChild3() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(VersionedEntity.class);
        Node rootNode = session.getRootNode().addNode("content").addNode("versionedEntities");
        VersionedDAO versionedDao = new VersionedDAO(session, jcrom);
        VersionedEntity entity = new VersionedEntity();
        entity.setTitle("MyEntity");
        entity.setBody("First");
        entity.setPath(rootNode.getPath());
        VersionedEntity child = new VersionedEntity();
        child.setName("child1");
        child.setBody("child1Body");
        entity.versionedChild3 = child;
        versionedDao.create(entity);
        assertEquals(child.getBody(), versionedDao.getVersion(entity.getPath(), "1.0").versionedChild3.getBody());
    }

    /**
     * Thanks to Andrius Kurtinaitis for identifying this problem and
     * contributing this test case.
     * @throws Exception
     */
    @Test
    public void versioningDAOChild4() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(VersionedEntity.class);
        Node rootNode = session.getRootNode().addNode("content").addNode("versionedEntities");
        VersionedDAO versionedDao = new VersionedDAO(session, jcrom);
        VersionedEntity entity = new VersionedEntity();
        entity.setTitle("MyEntity");
        entity.setBody("First");
        entity.setPath(rootNode.getPath());
        VersionedEntity child = new VersionedEntity();
        child.setName("child1");
        child.setBody("child1Body");
        entity.versionedChild4 = child;
        versionedDao.create(entity);
        assertEquals(child.getBody(), versionedDao.getVersion(entity.getPath(), "1.0").versionedChild4.getBody());
    }

    @Test
    public void testDAOs() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(Parent.class);
        Node rootNode = session.getRootNode().addNode("content").addNode("parents");
        ParentDAO parentDao = new ParentDAO(session, jcrom);
        Parent dad = createParent("John Bobs");
        dad.setDrivingLicense(false);
        dad.setPath(rootNode.getPath());
        dad.setAdoptedChild(createChild("AdoptedChild1"));
        dad.addChild(createChild("Child1"));
        dad.addChild(createChild("Child2"));
        Parent mom = createParent("Jane");
        mom.setPath(rootNode.getPath());
        assertFalse(parentDao.exists(dad.getPath() + "/" + dad.getName()));
        parentDao.create(dad);
        parentDao.create(mom);
        session.save();
        assertTrue(parentDao.exists(dad.getPath()));
        Parent loadedParent = parentDao.get(dad.getPath());
        assertTrue(loadedParent.getNickName().equals(dad.getNickName()));
        Parent uuidParent = parentDao.loadByUUID(loadedParent.getUuid());
        assertTrue(uuidParent.getNickName().equals(dad.getNickName()));
        Parent loadedMom = parentDao.get(mom.getPath());
        assertTrue(loadedMom.getNickName().equals(mom.getNickName()));
        loadedParent.getAdoptedChild().setNickName("testing");
        loadedParent.getChildren().get(0).setNickName("hello");
        parentDao.update(loadedParent, "*", 2);
        Parent updatedParent = parentDao.get(dad.getPath());
        assertEquals(loadedParent.getAdoptedChild().getNickName(), updatedParent.getAdoptedChild().getNickName());
        assertEquals(loadedParent.getChildren().get(0).getNickName(), updatedParent.getChildren().get(0).getNickName());
        List<Parent> parents = parentDao.findAll(rootNode.getPath());
        assertTrue(parents.size() == 2);
        List<Parent> parentsWithLicense = parentDao.findByLicense();
        assertTrue(parentsWithLicense.size() == 1);
        parentDao.remove(dad.getPath());
        assertFalse(parentDao.exists(dad.getPath()));
        parentDao.remove(loadedMom.getPath());
        assertFalse(parentDao.exists(mom.getPath()));
        Parent rootDad = createParent("John Smith");
        parentDao.create("/", rootDad);
        assertTrue(rootDad.getPath().equals("/" + rootDad.getName()));
        rootDad.setName("John Smythe");
        parentDao.update(rootDad);
    }

    /**
     * Thanks to Nguyen Ngoc Trung for reporting the issues tested here. 
     * @throws java.lang.Exception
     */
    @Test
    public void testMultiValuedProperties() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(Parent.class);
        Node rootNode = session.getRootNode().addNode("root");
        Parent parent = createParent("John Mugabe");
        parent.getTags().clear();
        parent.getTags().add("first");
        Node newNode = jcrom.addNode(rootNode, parent);
        assertTrue(newNode.getProperty("tags").getDefinition().isMultiple());
        assertEquals(1, newNode.getProperty("tags").getValues().length);
        Parent fromNode = jcrom.fromNode(Parent.class, newNode);
        assertEquals(1, fromNode.getTags().size());
        assertEquals("first", fromNode.getTags().get(0));
        Parent parent2 = createParent("MultiParent");
        Node newNode2 = jcrom.addNode(rootNode, parent2);
        assertTrue(newNode2.getProperty("tags").getDefinition().isMultiple());
        assertEquals(3, newNode2.getProperty("tags").getValues().length);
        Parent fromNode2 = jcrom.fromNode(Parent.class, newNode2);
        fromNode2.getTags().clear();
        fromNode2.getTags().add("second");
        jcrom.updateNode(newNode2, fromNode2);
        Parent fromNodeAgain = jcrom.fromNode(Parent.class, newNode2);
        assertTrue(newNode2.getProperty("tags").getDefinition().isMultiple());
        assertEquals(1, newNode2.getProperty("tags").getValues().length);
    }

    @Test
    public void mapObjectsToNodesAndBack() throws Exception {
        Parent parent = createParent("John Mugabe");
        Child adoptedChild = createChild("Mubi");
        parent.setAdoptedChild(adoptedChild);
        parent.addChild(createChild("Jane"));
        parent.addChild(createChild("Julie"));
        Child child = createChild("Robert");
        parent.addChild(child);
        child.setAdoptedGrandChild(createGrandChild("Jim"));
        child.addGrandChild(createGrandChild("Adam"));
        Photo photo = createPhoto("jcr_passport.jpg");
        parent.setPassportPhoto(photo);
        for (int i = 0; i < 3; i++) {
            parent.addFile(createFile("jcr_image" + i + ".jpg"));
        }
        Jcrom jcrom = new Jcrom();
        jcrom.map(Parent.class);
        Node rootNode = session.getRootNode().addNode("root");
        String[] mixinTypes = { "mix:referenceable" };
        Node newNode = jcrom.addNode(rootNode, parent, mixinTypes);
        String uuid = newNode.getUUID();
        assertTrue(newNode.getUUID() != null && newNode.getUUID().length() > 0);
        assertTrue(newNode.getProperty("birthDay").getDate().getTime().equals(parent.getBirthDay()));
        assertTrue(newNode.getProperty("weight").getDouble() == parent.getWeight());
        assertTrue((int) newNode.getProperty("fingers").getDouble() == parent.getFingers());
        assertTrue(new Boolean(newNode.getProperty("drivingLicense").getBoolean()).equals(new Boolean(parent.isDrivingLicense())));
        assertTrue(newNode.getProperty("hairs").getLong() == parent.getHairs());
        assertTrue(newNode.getNode("children").getNodes().nextNode().getName().equals("Jane"));
        assertTrue(newNode.getProperty("tags").getValues().length == 3);
        assertTrue(newNode.getProperty("tags").getValues()[0].getString().equals("father"));
        Parent parentFromNode = jcrom.fromNode(Parent.class, newNode);
        File imageFileFromNode = new File("target/ogg_copy.jpg");
        parentFromNode.getPassportPhoto().getDataProvider().writeToFile(imageFileFromNode);
        assertTrue(parentFromNode.getPassportPhoto().getFileBytes().length == photo.getFileBytes().length);
        assertTrue(parentFromNode.getFiles().size() == 3);
        assertTrue(parentFromNode.getUuid().equals(uuid));
        assertTrue(parentFromNode.getNickName().equals(parent.getNickName()));
        assertTrue(parentFromNode.getBirthDay().equals(parent.getBirthDay()));
        assertTrue(parentFromNode.getWeight() == parent.getWeight());
        assertTrue(parentFromNode.getFingers() == parent.getFingers());
        assertTrue(new Boolean(parentFromNode.isDrivingLicense()).equals(new Boolean(parent.isDrivingLicense())));
        assertTrue(parentFromNode.getHairs() == parent.getHairs());
        assertTrue(parentFromNode.getTags().size() == 3);
        assertTrue(parentFromNode.getAdoptedChild() != null && parentFromNode.getAdoptedChild().getName().equals(adoptedChild.getName()));
        assertTrue(parentFromNode.getChildren().size() == 3);
        assertTrue(parentFromNode.getChildren().get(2).getTitle().equals(child.getTitle()));
        assertTrue(parentFromNode.getChildren().get(2).getAdoptedGrandChild().getTitle().equals("Jim"));
        assertTrue(parentFromNode.getChildren().get(2).getGrandChildren().size() == 1);
        assertTrue(parentFromNode.getChildren().get(2).getGrandChildren().get(0).getTitle().equals("Adam"));
        assertTrue(((Parent) parentFromNode.getAdoptedChild().getParent()).getTitle().equals(parent.getTitle()));
        assertTrue(((Parent) parentFromNode.getChildren().get(0).getParent()).getTitle().equals(parent.getTitle()));
        assertTrue(parentFromNode.getChildren().get(2).getAdoptedGrandChild().getParent().getTitle().equals(child.getTitle()));
        assertTrue(parentFromNode.getChildren().get(2).getGrandChildren().get(0).getParent().getTitle().equals(child.getTitle()));
        parent.setNickName("Father");
        parent.setBirthDay(new Date());
        parent.setWeight(87.5);
        parent.setFingers(9);
        parent.setDrivingLicense(false);
        parent.setHairs(2);
        parent.getTags().remove(0);
        parent.addTag("test1");
        parent.addTag("test2");
        parent.getAdoptedChild().setFingers(10);
        parent.getPassportPhoto().setPhotographer("Johnny Bob");
        parent.getPassportPhoto().setDataProvider(null);
        jcrom.updateNode(newNode, parent);
        parentFromNode = jcrom.fromNode(Parent.class, newNode);
        System.out.println("Updated photographer: " + parentFromNode.getPassportPhoto().getPhotographer());
        System.out.println("InputStream is null: " + (parentFromNode.getPassportPhoto().getDataProvider().getInputStream() == null));
        assertTrue(newNode.getProperty("birthDay").getDate().getTime().equals(parent.getBirthDay()));
        assertTrue(newNode.getProperty("weight").getDouble() == parent.getWeight());
        assertTrue((int) newNode.getProperty("fingers").getDouble() == parent.getFingers());
        assertTrue(new Boolean(newNode.getProperty("drivingLicense").getBoolean()).equals(new Boolean(parent.isDrivingLicense())));
        assertTrue(newNode.getProperty("hairs").getLong() == parent.getHairs());
        assertTrue((int) newNode.getNode("adoptedChild").getNodes().nextNode().getProperty("fingers").getDouble() == parent.getAdoptedChild().getFingers());
        assertTrue(parentFromNode.getTags().equals(parent.getTags()));
        parentFromNode.getFiles().remove(1);
        parentFromNode.getFiles().add(1, createFile("jcr_image5.jpg"));
        parentFromNode.addFile(createFile("jcr_image6.jpg"));
        jcrom.updateNode(newNode, parentFromNode);
        Parent updatedParent = jcrom.fromNode(Parent.class, newNode);
        assertTrue(updatedParent.getFiles().size() == parentFromNode.getFiles().size());
        NodeFilter nodeFilter = new NodeFilter("children", NodeFilter.DEPTH_INFINITE, 1);
        Parent filteredParent = jcrom.fromNode(Parent.class, newNode, nodeFilter);
        assertNull(filteredParent.getAdoptedChild());
        assertEquals(updatedParent.getChildren().size(), filteredParent.getChildren().size());
        assertEquals(updatedParent.getChildren().get(0).getNickName(), filteredParent.getChildren().get(0).getNickName());
        parent.setTitle("Mohammed");
        parent.setNickName("Momo");
        jcrom.updateNode(newNode, parent);
        assertFalse(rootNode.hasNode("John_Mugabe"));
        assertTrue(rootNode.hasNode("Mohammed"));
        assertTrue(rootNode.getNode("Mohammed").getProperty("nickName").getString().equals("Momo"));
        Node adoptedChildNode = rootNode.getNode(parent.getName()).getNode("adoptedChild").getNodes().nextNode();
        adoptedChild.setNickName("Mubal");
        jcrom.updateNode(adoptedChildNode, adoptedChild);
        assertTrue(rootNode.getNode(parent.getName()).getNode("adoptedChild").getNodes().nextNode().getProperty("nickName").getString().equals(adoptedChild.getNickName()));
    }

    /**
     * Thanks to Decebal Suiu for contributing this test case.
     * @throws Exception 
     */
    @Test
    public void testEmptyList() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(Person.class);
        Person person = new Person();
        person.setName("peter");
        person.setAge(20);
        person.setPhones(Arrays.asList(new String[] { "053453553" }));
        Node node = jcrom.addNode(session.getRootNode(), person);
        Person personFromJcr = jcrom.fromNode(Person.class, node);
        assertEquals(1, personFromJcr.getPhones().size());
        person.setPhones(new ArrayList<String>());
        jcrom.updateNode(node, person);
        personFromJcr = jcrom.fromNode(Person.class, node);
        assertFalse(personFromJcr.getPhones().size() == 1);
    }

    @Test
    public void testJcrFileMapping() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(JcrFile.class);
        Node root = session.getRootNode().addNode("files");
        JcrFile file = createFile("myfile", true);
        Calendar lastModified = file.getLastModified();
        Node node = jcrom.addNode(root, file);
        assertTrue(node.hasNode("jcr:content"));
        assertEquals("image/jpeg", node.getNode("jcr:content").getProperty("jcr:mimeType").getString());
        assertEquals("UTF-8", node.getNode("jcr:content").getProperty("jcr:encoding").getString());
        JcrFile fromNode = jcrom.fromNode(JcrFile.class, node);
        assertEquals("image/jpeg", fromNode.getMimeType());
        assertEquals("UTF-8", fromNode.getEncoding());
        assertEquals(lastModified, fromNode.getLastModified());
        assertTrue(fromNode.getDataProvider().getContentLength() > 0);
    }

    /**
     * Thanks to Danilo Barboza for contributing this test case.
     * @throws Exception 
     */
    @Test
    public void testCustomJcrFile() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(CustomJCRFile.class);
        session.getRootNode().addNode("customs");
        CustomJCRFileDAO dao = new CustomJCRFileDAO(session, jcrom);
        File imageFile = new File("src/test/resources/ogg.jpg");
        CustomJCRFile custom = new CustomJCRFile();
        custom.setPath("customs");
        custom.setMetadata("My Metadata!");
        custom.setEncoding("UTF-8");
        custom.setMimeType("image/jpg");
        custom.setLastModified(Calendar.getInstance());
        custom.setDataProvider(new JcrDataProviderImpl(imageFile));
        custom.setName(imageFile.getName());
        dao.create(custom);
        CustomJCRFile customFromJcr = dao.get(custom.getPath());
        assertEquals(custom.getName(), customFromJcr.getName());
        assertEquals(custom.getEncoding(), customFromJcr.getEncoding());
        assertEquals(custom.getMimeType(), customFromJcr.getMimeType());
        assertEquals(custom.getMetadata(), customFromJcr.getMetadata());
        customFromJcr.setEncoding("ISO-8859-1");
        customFromJcr.setMimeType("image/gif");
        customFromJcr.setMetadata("Updated metadata");
        customFromJcr.setDataProvider(null);
        dao.update(customFromJcr);
        CustomJCRFile updatedFromJcr = dao.get(customFromJcr.getPath());
        assertEquals(customFromJcr.getEncoding(), updatedFromJcr.getEncoding());
        assertEquals(customFromJcr.getMimeType(), updatedFromJcr.getMimeType());
        assertEquals(customFromJcr.getMetadata(), updatedFromJcr.getMetadata());
    }

    @Test
    public void testNoChildContainerNode() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(UserProfile.class);
        Node rootNode = session.getRootNode().addNode("noChildTest");
        UserProfile userProfile = new UserProfile();
        userProfile.setName("john");
        Address address = new Address();
        address.setStreet("Some street");
        address.setPostCode("101");
        userProfile.setAddress(address);
        Node userProfileNode = jcrom.addNode(rootNode, userProfile);
        UserProfile fromJcr = jcrom.fromNode(UserProfile.class, userProfileNode);
        assertEquals(address.getName(), "address");
        assertEquals(address.getStreet(), fromJcr.getAddress().getStreet());
        assertEquals(address.getPostCode(), fromJcr.getAddress().getPostCode());
        fromJcr.getAddress().setStreet("Another street");
        jcrom.updateNode(userProfileNode, fromJcr);
        UserProfile updatedFromJcr = jcrom.fromNode(UserProfile.class, userProfileNode);
        assertEquals(fromJcr.getAddress().getStreet(), updatedFromJcr.getAddress().getStreet());
        assertEquals(fromJcr.getAddress().getPostCode(), updatedFromJcr.getAddress().getPostCode());
    }

    @Test
    public void testSecondLevelFileUpdate() throws Exception {
        Jcrom jcrom = new Jcrom();
        jcrom.map(GrandParent.class).map(Photo.class);
        GrandParent grandParent = new GrandParent();
        grandParent.setName("Charles");
        Parent parent = createParent("William");
        Photo photo = createPhoto("jcr_passport.jpg");
        parent.setPassportPhoto(photo);
        parent.setJcrFile(createFile("jcr_image.jpg"));
        grandParent.setChild(parent);
        Node rootNode = session.getRootNode().addNode("root");
        Node newNode = jcrom.addNode(rootNode, grandParent);
        GrandParent fromNode = jcrom.fromNode(GrandParent.class, newNode);
        fromNode.getChild().getPassportPhoto().setName("bobby.xml");
        fromNode.getChild().getPassportPhoto().setPhotographer("Bobbs");
        fromNode.getChild().setName("test");
        fromNode.getChild().setTitle("Something");
        fromNode.getChild().getJcrFile().setName("bob.xml");
        jcrom.updateNode(newNode, photo);
    }

    /**
     * Thanks to Bouiaw and Andrius Kurtinaitis for identifying this problem and
     * contributing this test case.
     * @throws Exception
     */
    @Test
    public void testReferenceCycles() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(BadNode.class);
        BadNode node1 = new BadNode();
        node1.body = "body1";
        BadNode node2 = new BadNode();
        node2.body = "body2";
        Node rootNode = session.getRootNode();
        Node n1 = jcrom.addNode(rootNode, node1);
        Node n2 = jcrom.addNode(rootNode, node2);
        node1.reference = node2;
        node2.reference = node1;
        jcrom.updateNode(n1, node1);
        jcrom.updateNode(n2, node2);
        BadNode fromNode1 = jcrom.fromNode(BadNode.class, n1);
        BadNode fromNode2 = jcrom.fromNode(BadNode.class, n2);
        assertEquals(node1.body, fromNode2.reference.body);
        assertEquals(node2.body, fromNode1.reference.body);
    }

    @Test
    public void testNestedInterfaces() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(AImpl.class).map(BImpl.class).map(CImpl.class);
        A a = new AImpl("a");
        B b = new BImpl("b");
        C c = new CImpl("c");
        c.setA(a);
        b.setC(c);
        a.setB(b);
        Node rootNode = session.getRootNode();
        Node nodeA = jcrom.addNode(rootNode, a);
        A fromNodeA = jcrom.fromNode(A.class, nodeA);
    }

    /**
     * Thanks to Leander for identifying this problem and
     * contributing this test case.
     * @throws Exception
     */
    @Test
    public final void testUpdateNodeNodeObject() throws Exception {
        final Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(Shape.class).map(Triangle.class).map(ShapeParent.class);
        Triangle mainShape = new Triangle(1, 1);
        mainShape.setName("mainShape");
        Triangle childShape = new Triangle(2, 2);
        childShape.setName("childShape");
        ShapeParent shapeParent = new ShapeParent();
        shapeParent.setPath("/");
        shapeParent.setName("shapeParent");
        shapeParent.setMainShape(mainShape);
        shapeParent.addShape(childShape);
        final Node rootNode = this.session.getRootNode();
        jcrom.addNode(rootNode, shapeParent);
        this.session.save();
        Node node = rootNode.getNode("shapeParent");
        shapeParent = jcrom.fromNode(ShapeParent.class, node);
        mainShape = (Triangle) shapeParent.getMainShape();
        assertEquals("Base is wrong", 1, mainShape.getBase(), 0);
        assertEquals("Height is wrong", 1, mainShape.getHeight(), 0);
        mainShape.setBase(2);
        mainShape.setHeight(2);
        childShape = (Triangle) shapeParent.getShapes().get(0);
        assertEquals("Base is wrong", 2, mainShape.getBase(), 0);
        assertEquals("Height is wrong", 2, mainShape.getHeight(), 0);
        childShape.setBase(3);
        childShape.setHeight(3);
        jcrom.updateNode(node, shapeParent);
        this.session.save();
        node = rootNode.getNode("shapeParent");
        shapeParent = jcrom.fromNode(ShapeParent.class, node);
        mainShape = (Triangle) shapeParent.getMainShape();
        assertEquals("Base is wrong", 2, mainShape.getBase(), 0);
        assertEquals("Height is wrong", 2, mainShape.getHeight(), 0);
        childShape = (Triangle) shapeParent.getShapes().get(0);
        assertEquals("Base is wrong", 3, childShape.getBase(), 0);
        assertEquals("Height is wrong", 3, childShape.getHeight(), 0);
    }

    /**
     * Thanks to Leander for contributing this test case.
     * @throws Exception
     */
    @Test
    public final void testAddCustomJCRFileParentNode() throws Exception {
        final Jcrom jcrom = new Jcrom();
        jcrom.map(CustomJCRFile.class);
        jcrom.map(CustomJCRFileParentNode.class);
        final Node customs = this.session.getRootNode().addNode("customs");
        final CustomJCRFile custom = new CustomJCRFile();
        custom.setPath("customs");
        custom.setMetadata("My Metadata!");
        custom.setEncoding("UTF-8");
        custom.setMimeType("image/jpg");
        custom.setLastModified(Calendar.getInstance());
        final File imageFile = new File("src/test/resources/ogg.jpg");
        custom.setDataProvider(new JcrDataProviderImpl(imageFile));
        custom.setName(imageFile.getName());
        final CustomJCRFileParentNode parent = new CustomJCRFileParentNode();
        parent.setName("parent");
        parent.setFile(custom);
        final Node parentNode = jcrom.addNode(customs, parent);
        final Node customNode = parentNode.getNode("file/ogg.jpg");
        final NodeType[] mixins = customNode.getMixinNodeTypes();
        assertEquals("Mixin size is wrong.", 1, mixins.length);
        assertEquals("mix:referenceable", mixins[0].getName());
        final CustomJCRFileParentNode parentFromJcr = jcrom.fromNode(CustomJCRFileParentNode.class, parentNode);
        assertNotNull("UUID is null.", parentFromJcr.getFile().getUuid());
    }

    /**
     * Thanks to Leander for contributing this test case.
     * @throws Exception
     */
    @Test
    public final void testAddCustomJCRFile() throws Exception {
        final Jcrom jcrom = new Jcrom();
        jcrom.map(CustomJCRFile.class);
        jcrom.map(CustomJCRFileParentNode.class);
        final Node customs = this.session.getRootNode().addNode("customs");
        final CustomJCRFile custom = new CustomJCRFile();
        custom.setPath("customs");
        custom.setMetadata("My Metadata!");
        custom.setEncoding("UTF-8");
        custom.setMimeType("image/jpg");
        custom.setLastModified(Calendar.getInstance());
        final File imageFile = new File("src/test/resources/ogg.jpg");
        custom.setDataProvider(new JcrDataProviderImpl(imageFile));
        custom.setName(imageFile.getName());
        final Node customNode = jcrom.addNode(customs, custom);
        final NodeType[] mixins = customNode.getMixinNodeTypes();
        assertEquals("Mixin size is wrong.", 1, mixins.length);
        assertEquals("mix:referenceable", mixins[0].getName());
        final CustomJCRFile customFromJcr = jcrom.fromNode(CustomJCRFile.class, customNode);
        assertNotNull("UUID is null.", customFromJcr.getUuid());
    }

    @Test
    public void finalFields() throws Exception {
        final Jcrom jcrom = new Jcrom();
        jcrom.map(FinalEntity.class);
        FinalEntity entity = new FinalEntity("This cannot be changed");
        entity.setName("myentity");
        final Node parentNode = this.session.getRootNode().addNode("mynode");
        jcrom.addNode(parentNode, entity);
        assertTrue(parentNode.getNode("myentity").hasProperty("immutableString"));
        assertEquals(entity.getImmutableString(), parentNode.getNode("myentity").getProperty("immutableString").getString());
    }

    @Test
    public void mapPackage() throws Exception {
        final Jcrom jcrom = new Jcrom();
        jcrom.mapPackage("org.jcrom.entities");
        final Node parentNode = this.session.getRootNode().addNode("mynode");
        First first = new First();
        first.setName("first");
        first.setFirstString("nr1");
        Second second = new Second();
        second.setName("second");
        second.setSecondString("nr2");
        jcrom.addNode(parentNode, first);
        jcrom.addNode(parentNode, second);
        assertTrue(parentNode.getNode("first").hasProperty("firstString"));
        assertTrue(parentNode.getNode("second").hasProperty("secondString"));
    }

    @Test
    public void parentInterface() throws Exception {
        final Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(Square.class).map(WithParentInterface.class);
        final Node parentNode = this.session.getRootNode().addNode("mynode");
        WithParentInterface child = new WithParentInterface();
        child.setName("child");
        Square square = new Square(3, 3);
        square.setName("square");
        square.setChild(child);
        Node newNode = jcrom.addNode(parentNode, square);
        Square fromNode = jcrom.fromNode(Square.class, newNode);
        assertTrue(square.getArea() == fromNode.getChild().getParent().getArea());
    }

    @Test
    public void customChildContainers() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(Parent.class);
        Node rootNode = session.getRootNode().addNode("root");
        Parent parent = createParent("Daddy");
        Child listChild1 = createChild("Sonny1");
        Child listChild2 = createChild("Sonny2");
        Child mapChild1 = createChild("Jane1");
        Child mapChild2 = createChild("Jane2");
        parent.getCustomList().add(listChild1);
        parent.getCustomList().add(listChild2);
        parent.getCustomMap().put(mapChild1.getName(), mapChild1);
        parent.getCustomMap().put(mapChild2.getName(), mapChild2);
        Node newNode = jcrom.addNode(rootNode, parent);
        Parent fromNode = jcrom.fromNode(Parent.class, newNode);
        assertTrue(fromNode.getCustomList() instanceof LinkedList);
        assertTrue(fromNode.getCustomMap() instanceof TreeMap);
    }

    @Test(expected = PathNotFoundException.class)
    public void testNodeClassChange() throws Exception {
        Jcrom jcrom = new Jcrom(true, true);
        jcrom.map(Rectangle.class).map(Triangle.class);
        Node rootNode = session.getRootNode().addNode("root");
        Triangle triangle = new Triangle(1, 1);
        triangle.setName("test");
        Node newNode = jcrom.addNode(rootNode, triangle);
        Rectangle rectangle = new Rectangle(2.5, 3.3);
        rectangle.setName("test");
        jcrom.updateNode(newNode, rectangle);
        newNode.getProperty("base");
    }
}
