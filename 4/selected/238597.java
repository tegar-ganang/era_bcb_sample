package ssg.common.utils;

import ssg.common.utils.UID;
import ssg.common.utils.CloneHelper;
import ssg.common.utils._data_.CloneHelperData;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author ssg
 */
public class CloneHelperTest extends TestCase {

    public CloneHelperTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(CloneHelperTest.class);
        return suite;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of reflectiveClone method, of class CloneHelper.
     */
    public void testReflectiveClone() throws Exception {
        System.out.println("reflectiveClone");
        String pu = CloneHelperData.PU;
        String pro = CloneHelperData.PRO;
        String pri = CloneHelperData.PRI;
        {
            CloneHelperData.CHCloneable instance = new CloneHelperData.CHCloneable(pu, pro, pri);
            CloneHelperData.CHCloneable result = (CloneHelperData.CHCloneable) CloneHelper.reflectiveClone(instance);
            if (!result.testPu(pu) || !result.testPro(pro) || !result.testPri(pri)) {
                fail("Cloning of 'Cloneable' failed.");
            }
        }
        {
            CloneHelperData.CHNotCloneable instance = new CloneHelperData.CHNotCloneable(pu, pro, pri);
            CloneHelperData.CHNotCloneable result = (CloneHelperData.CHNotCloneable) CloneHelper.reflectiveClone(instance);
            if (!result.testPu(pu) || !result.testPro(pro) || !result.testPri(pri)) {
            }
        }
        {
            CloneHelperData.CHNotCloneableBean instance = new CloneHelperData.CHNotCloneableBean(pu, pro, pri);
            CloneHelperData.CHNotCloneableBean result = (CloneHelperData.CHNotCloneableBean) CloneHelper.reflectiveClone(instance);
            if (!result.testPu(pu) || !result.testPro(pro) || !result.testPri(pri)) {
                fail("Cloning of 'not Cloneable bean' failed.");
            }
        }
        try {
            String pro2 = "d";
            CloneHelperData.WithNestedClass.NPU = pu;
            CloneHelperData.WithNestedClass.NPRO = pro;
            CloneHelperData.WithNestedClass.NPRI = pri;
            CloneHelperData.WithNestedClass.NPRO2 = pro2;
            CloneHelperData.WithNestedClass instance = new CloneHelperData.WithNestedClass();
            CloneHelperData.WithNestedClass.NPU = "aa";
            CloneHelperData.WithNestedClass.NPRO = "bb";
            CloneHelperData.WithNestedClass.NPRI = "cc";
            CloneHelperData.WithNestedClass.NPRO2 = "dd";
            CloneHelperData.WithNestedClass result = (CloneHelperData.WithNestedClass) CloneHelper.reflectiveClone(instance);
            StringBuffer sb = new StringBuffer();
            for (java.lang.reflect.Field f : instance.getClass().getFields()) {
                Object o = f.get(result);
                for (java.lang.reflect.Method m : o.getClass().getMethods()) {
                    if (m.getName().startsWith("testP")) {
                        String ts = null;
                        if (m.getName().equals("testPu")) {
                            ts = pu;
                        } else if (m.getName().equals("testPro")) {
                            ts = (f.getName().indexOf("Inline") == -1) ? pro : pro2;
                        } else if (m.getName().equals("testPri")) {
                            ts = pri;
                        }
                        Boolean b = (Boolean) m.invoke(o, new Object[] { ts });
                        if (!b && ts != null) {
                            if (!(o instanceof CloneHelperData.CHNotCloneable)) {
                                sb.append("Failed for: " + f.getName() + "." + m.getName() + "('" + ts + "'); ");
                            }
                        }
                    }
                }
            }
            if (sb.length() > 0) {
                fail("Cloning of inline instances failed: " + sb.toString());
            }
        } finally {
            CloneHelperData.WithNestedClass.NPU = pu;
            CloneHelperData.WithNestedClass.NPRO = pro;
            CloneHelperData.WithNestedClass.NPRI = pri;
            CloneHelperData.WithNestedClass.NPRO2 = "d";
        }
    }

    /**
     * Test of tryToSetName method, of class CloneHelper.
     */
    public void testTryToSetName() {
        System.out.println("tryToSetName");
        String name = "name";
        {
            CloneHelperData.CHNamedPublic o = new CloneHelperData.CHNamedPublic();
            o.name = name + "__";
            CloneHelper.tryToSetName(o, name);
            if (!o.name.equals(name)) {
                fail("Failed to set name for object with public string field 'name'.");
            }
        }
        {
            CloneHelperData.CHNamedBean o = new CloneHelperData.CHNamedBean();
            o.setName(name + "__");
            CloneHelper.tryToSetName(o, name);
            if (!o.getName().equals(name)) {
                fail("Failed to set name for object with full bean (get/set) for 'name'.");
            }
        }
        {
            CloneHelperData.CHNamedBeanRO o = new CloneHelperData.CHNamedBeanRO(name + "__");
            CloneHelper.tryToSetName(o, name);
            if (o.getName().equals(name)) {
                fail("Managed to set name for object with R/O bean (get) for 'name'.");
            }
        }
    }

    /**
     * Test of tryToGetName method, of class CloneHelper.
     */
    public void testTryToGetName() {
        System.out.println("tryToGetName");
        String name = "name";
        {
            CloneHelperData.CHNamedPublic o = new CloneHelperData.CHNamedPublic();
            o.name = name;
            String result = CloneHelper.tryToGetName(o);
            assertEquals(name, result);
        }
        {
            CloneHelperData.CHNamedBean o = new CloneHelperData.CHNamedBean();
            o.setName(name);
            String result = CloneHelper.tryToGetName(o);
            assertEquals(name, result);
        }
        {
            CloneHelperData.CHNamedBeanRO o = new CloneHelperData.CHNamedBeanRO(name);
            String result = CloneHelper.tryToGetName(o);
            assertEquals(name, result);
        }
    }

    /**
     * Test of tryToSetUID method, of class CloneHelper.
     */
    public void testTryToSetUID() {
        System.out.println("tryToSetUID");
        UID uid = new UID();
        {
            CloneHelperData.CHNamedPublic o = new CloneHelperData.CHNamedPublic();
            o.uid = new UID();
            CloneHelper.tryToSetUID(o, uid);
            if (!o.uid.equals(uid)) {
                fail("Failed to set name for object with public string field 'name'.");
            }
        }
        {
            CloneHelperData.CHNamedBean o = new CloneHelperData.CHNamedBean();
            o.setUID(new UID());
            CloneHelper.tryToSetUID(o, uid);
            if (!o.getUID().equals(uid)) {
                fail("Failed to set name for object with full bean (get/set) for 'name'.");
            }
        }
        {
            CloneHelperData.CHNamedBeanRO o = new CloneHelperData.CHNamedBeanRO(new UID());
            CloneHelper.tryToSetUID(o, uid);
            if (o.getUID().equals(uid)) {
                fail("Managed to set name for object with R/O bean (get) for 'name'.");
            }
        }
    }

    /**
     * Test of tryToGetUID method, of class CloneHelper.
     */
    public void testTryToGetUID() {
        System.out.println("tryToGetUID");
        UID uid = new UID();
        {
            CloneHelperData.CHNamedPublic o = new CloneHelperData.CHNamedPublic();
            o.uid = uid;
            UID result = CloneHelper.tryToGetUID(o);
            assertEquals(uid, result);
        }
        {
            CloneHelperData.CHNamedBean o = new CloneHelperData.CHNamedBean();
            o.setUID(uid);
            UID result = CloneHelper.tryToGetUID(o);
            assertEquals(uid, result);
        }
        {
            CloneHelperData.CHNamedBeanRO o = new CloneHelperData.CHNamedBeanRO(uid);
            UID result = CloneHelper.tryToGetUID(o);
            assertEquals(uid, result);
        }
    }

    /**
     * Test of tryToInvoke method, of class CloneHelper.
     */
    public void testTryToInvoke() {
        System.out.println("tryToInvoke");
        CloneHelperData.CHNamedBean o = new CloneHelperData.CHNamedBean();
        o.setName("__");
        String methodName = "setName";
        Object value = "name";
        CloneHelper.tryToInvoke(o, methodName, value);
        assertEquals(value, o.getName());
        methodName = "sztGHILName";
        value = "name";
        CloneHelper.tryToInvoke(o, methodName, value);
    }

    /**
     * Test of tryToGetField method, of class CloneHelper.
     */
    public void testTryToGetField() {
        System.out.println("tryToGetField");
        CloneHelperData.CHNamedPublic o = new CloneHelperData.CHNamedPublic();
        String testName = "test";
        o.name = testName;
        String fieldName = "name";
        Object defaultValue = "zzz";
        Object expResult = testName;
        Object result = CloneHelper.tryToGetField(o, fieldName, defaultValue);
        assertEquals(expResult, result);
        fieldName = "asdf_name";
        expResult = defaultValue;
        result = CloneHelper.tryToGetField(o, fieldName, defaultValue);
        assertEquals(expResult, result);
    }

    /**
     * Test of cloneCollection method, of class CloneHelper.
     */
    public void testCloneCollection() throws Exception {
        System.out.println("cloneCollection");
        Collection c = new LinkedList();
        for (int i = 0; i < 10; i++) {
            c.add(new CloneHelperData.CHCloneable("a" + i, "b" + i * 2, "c" + i * 3));
        }
        boolean cloneItems = false;
        List result = CloneHelper.cloneCollection(c, cloneItems);
        if (result == c || result.size() != c.size() || result.iterator().next() != c.iterator().next()) {
            fail("Collection cloning failed: size differ or same object returned.");
        }
        cloneItems = true;
        result = CloneHelper.cloneCollection(c, cloneItems);
        if (result.size() != c.size() || result.iterator().next() == c.iterator().next()) {
            fail("Collection cloning failed: requested items cloning failed.");
        }
    }

    /**
     * Test of cloneMap method, of class CloneHelper.
     */
    public void testCloneMap() throws Exception {
        System.out.println("cloneMap");
        Map c = new HashMap();
        for (int i = 0; i < 10; i++) {
            c.put("abc" + i, new CloneHelperData.CHCloneable("a" + i, "b" + i * 2, "c" + i * 3));
        }
        boolean cloneItems = false;
        Map result = CloneHelper.cloneMap(c, cloneItems);
        if (result == c || result.size() != c.size() || result.get(result.keySet().iterator().next()) != c.get(c.keySet().iterator().next())) {
            fail("Map cloning failed: size differ or same object returned.");
        }
        cloneItems = true;
        result = CloneHelper.cloneMap(c, cloneItems);
        if (result.size() != c.size() || result.get(result.keySet().iterator().next()) == c.get(c.keySet().iterator().next())) {
            fail("Collection cloning failed: requested items cloning failed.");
        }
    }

    /**
     * Test of cloneProperties method, of class CloneHelper.
     */
    public void testCloneProperties() {
        System.out.println("cloneProperties");
        Properties c = new Properties();
        c.setProperty("a", "aa");
        c.setProperty("b", "bb");
        c.setProperty("c", "cc");
        Properties result = CloneHelper.cloneProperties(c);
        if (c == result || !c.equals(result)) {
            fail("Properties cloning failed.");
        }
    }

    /**
     * Test of tryCopyFields method, of class CloneHelper.
     */
    public void testTryCopyFields_3args() {
        System.out.println("tryCopyFields");
        Object source = null;
        Object target = null;
        String[] fieldNames = null;
        int expResult = 0;
        int result = CloneHelper.tryCopyFields(source, target, fieldNames);
    }

    /**
     * Test of tryCopyFields method, of class CloneHelper.
     */
    public void testTryCopyFields_4args() {
        System.out.println("tryCopyFields");
        Object source = null;
        Object target = null;
        Map<String, Object> readers = null;
        Map<String, Object> writers = null;
        int expResult = 0;
        int result = CloneHelper.tryCopyFields(source, target, readers, writers);
    }

    /**
     * Test of getReaders method, of class CloneHelper.
     */
    public void testGetReaders_Object_StringArr() {
        System.out.println("getReaders");
        Object obj = null;
        String[] namesOnly = null;
        Map expResult = null;
        Map result = CloneHelper.getReaders(obj, namesOnly);
    }

    /**
     * Test of getReaders method, of class CloneHelper.
     */
    public void testGetReaders_Class_StringArr() {
        System.out.println("getReaders");
        Class c = null;
        String[] namesOnly = null;
        Map expResult = null;
        Map result = CloneHelper.getReaders(c, namesOnly);
    }

    /**
     * Test of getWriters method, of class CloneHelper.
     */
    public void testGetWriters_Object_StringArr() {
        System.out.println("getWriters");
        Object obj = null;
        String[] namesOnly = null;
        Map expResult = null;
        Map result = CloneHelper.getWriters(obj, namesOnly);
    }

    /**
     * Test of getWriters method, of class CloneHelper.
     */
    public void testGetWriters_Class_StringArr() {
        System.out.println("getWriters");
        Class c = null;
        String[] namesOnly = null;
        Map expResult = null;
        Map result = CloneHelper.getWriters(c, namesOnly);
    }

    /**
     * Test of reflectiveEquals method, of class CloneHelper.
     */
    public void testReflectiveEquals() {
        System.out.println("reflectiveEquals");
        Object obj1 = new CloneHelperData.CHNotCloneableBean("a", "b", "c");
        Object obj2 = new CloneHelperData.CHNotCloneableBean("a", "b", "c");
        boolean expResult = true;
        boolean result = CloneHelper.reflectiveEquals(obj1, obj2);
        assertEquals(expResult, result);
        obj2 = new CloneHelperData.CHNotCloneableBean("a", "b", "d");
        expResult = false;
        result = CloneHelper.reflectiveEquals(obj1, obj2);
        assertEquals(expResult, result);
        obj1 = new CloneHelperData.CHNotCloneableBean(null, "b", "d");
        obj2 = new CloneHelperData.CHNotCloneableBean(null, "b", "d");
        expResult = true;
        result = CloneHelper.reflectiveEquals(obj1, obj2);
        assertEquals(expResult, result);
    }

    /**
     * Test of getValues method, of class CloneHelper.
     */
    public void testGetValues_Object_Map() throws Exception {
        System.out.println("getValues");
        Object obj = null;
        Map<String, Object> readers = null;
        Map expResult = null;
        Map result = CloneHelper.getValues(obj, readers);
    }

    /**
     * Test of getValues method, of class CloneHelper.
     */
    public void testGetValues_Object() throws Exception {
        System.out.println("getValues");
        Object obj = null;
        Map expResult = null;
        Map result = CloneHelper.getValues(obj);
    }

    /**
     * Test of setValues method, of class CloneHelper.
     */
    public void testSetValues_3args() throws Exception {
        System.out.println("setValues");
        Object obj = null;
        Map<String, Object> writers = null;
        Map<String, Object> values = null;
        CloneHelper.setValues(obj, writers, values);
    }

    /**
     * Test of setValues method, of class CloneHelper.
     */
    public void testSetValues_Object_Map() throws Exception {
        System.out.println("setValues");
        Object obj = null;
        Map<String, Object> values = null;
        CloneHelper.setValues(obj, values);
    }

    /**
     * Test of toNameClassMap method, of class CloneHelper.
     */
    public void testToNameClassMap() {
        System.out.println("toNameClassMap");
        Map<String, Object> items = null;
        Map expResult = null;
        Map result = CloneHelper.toNameClassMap(items);
    }

    /**
     * Test of createObject method, of class CloneHelper.
     */
    public void testCreateObject() throws Exception {
        System.out.println("createObject");
        Class c = null;
        Object[] params = null;
        Object expResult = null;
        Object result = CloneHelper.createObject(c, params);
    }
}
