package org.pluginbuilder.internal.core.smartproperties;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.pluginbuilder.core.test.TestUtil;

public class SmartProperties_Test extends TestCase {

    public void testReadFullBlown() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("fullBlown"));
        List<SmartProperty> properties = smartProperties.getAllProperties();
        assertEquals(1, properties.size());
        SmartProperty smartProperty = properties.get(0);
        assertEquals("myProp", smartProperty.getName());
        assertEquals("default", smartProperty.getDefaultValue());
        assertEquals("myValue", smartProperty.getValue());
        assertEquals("myCat", smartProperty.getCategory());
        assertEquals("myType", smartProperty.getType());
        assertEquals("myDocumentation", smartProperty.getDocumentation());
        Map<String, String> typeAttributeMap = smartProperty.getTypeAttributeMap();
        assertEquals(3, typeAttributeMap.size());
        assertEquals("abc", typeAttributeMap.get("myTypeAttribute1"));
        assertEquals("v;,v=", typeAttributeMap.get("myTypeAttribute2"));
        assertEquals("", typeAttributeMap.get("myTypeAttribute3"));
    }

    public void testDefaultIgnore() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("default", "ignore"));
        assertEquals(1, smartProperties.getAllProperties().size());
        assertNull(smartProperties.getAllProperties().get(0).getDefaultValue());
    }

    public void testValue() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("value", "initial"));
        SmartProperty smartProperty = smartProperties.getAllProperties().get(0);
        smartProperty.setValue("myUpdatedValue");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        smartProperties.write(output);
        assertFile("value", "updated", new String(output.toByteArray()));
    }

    public void testValueEmpty() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("value", "empty"));
        List<SmartProperty> allProperties = smartProperties.getAllProperties();
        assertEquals(1, allProperties.size());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        smartProperties.write(output);
        assertFile("value", "empty", new String(output.toByteArray()));
    }

    public void testSetNewValue() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("default", "initial"));
        List<SmartProperty> properties = smartProperties.getAllProperties();
        assertNull(properties.get(0).getValue());
        properties.get(0).setValue("myValue");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        smartProperties.write(output);
        properties = smartProperties.getAllProperties();
        assertFile("default", "myValue", new String(output.toByteArray()));
        output = new ByteArrayOutputStream();
        properties.get(0).setValue("secondValue");
        smartProperties.write(output);
        assertFile("default", "secondValue", new String(output.toByteArray()));
    }

    public void testSetNew() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("setNew", "initial"));
        List<SmartProperty> properties = smartProperties.getAllProperties();
        properties.get(0).setValue("newValue");
        properties.get(1).setValue("newValue");
        assertFile("setNew", "assert", smartProperties);
        properties = smartProperties.getAllProperties();
        properties.get(0).unset();
        properties.get(1).unset();
        assertFile("setNew", "initial", smartProperties);
    }

    public void testGetByName() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("default", "initial"));
        assertNotNull(smartProperties.getPropertyByName("myProp"));
        assertNull(smartProperties.getPropertyByName("myProp1"));
    }

    private void assertFile(String test, String version, SmartProperties smartProperties) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        smartProperties.write(output);
        assertFile(test, version, new String(output.toByteArray()));
    }

    public void testUnset() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("default", "initial"));
        smartProperties.getAllProperties().get(0).setValue("myValue");
        smartProperties.write(new ByteArrayOutputStream());
        smartProperties.getAllProperties().get(0).unset();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        smartProperties.write(output);
        assertFile("default", "initial", new String(output.toByteArray()));
    }

    public void testSetDefaultValue() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("default", "initial"));
        SmartProperty smartProperty = smartProperties.getAllProperties().get(0);
        smartProperty.setValue("myValue");
        smartProperty.setValue(smartProperty.getDefaultValue());
        assertNull(smartProperty.getValue());
    }

    public void testReadAndSaveUnmodified() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("default", "initial"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        smartProperties.write(output);
        assertFile("default", "initial", new String(output.toByteArray()));
    }

    public void testEscapedEquals() {
        String def = "# value = a\\=1";
        assertEquals(1, new SmartProperties(def).getAllProperties().size());
    }

    public void testCategory() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("category"));
        assertEquals(2, smartProperties.getAllProperties().size());
        assertEquals(1, smartProperties.getUncategorizedProperties().size());
        assertEquals(1, smartProperties.getPropertiesByCategory("myCat").size());
        assertEquals(0, smartProperties.getPropertiesByCategory("unknowncategory").size());
    }

    public void testAppendProperty() throws Exception {
        SmartProperties smartProperties = new SmartProperties(readFile("default_ignore"));
        assertEquals(1, smartProperties.getAllProperties().size());
        String name = "test";
        String value = "new";
        smartProperties.appendProperty(name, value);
        assertEquals(2, smartProperties.getAllProperties().size());
        SmartProperty newProperty = smartProperties.getPropertyByName(name);
        assertNotNull(newProperty);
        assertEquals(name, newProperty.getName());
        assertEquals(value, newProperty.getValue());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        smartProperties.write(baos);
        SmartProperties reloaded = new SmartProperties(new String(baos.toByteArray()));
        assertEquals(2, reloaded.getAllProperties().size());
        SmartProperty addedProperty = reloaded.getPropertyByName(name);
        assertNotNull(addedProperty);
    }

    public void testPropertyDefinedTwice() {
    }

    private String readFile(String test) throws Exception {
        return readFile(test, null);
    }

    private String readFile(String test, String version) throws Exception {
        String filename = test;
        if (version != null) {
            filename += "_" + version;
        }
        File file = new File(getTestFileDirectory(), filename + ".properties");
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (fileInputStream.available() > 0) {
            int read = fileInputStream.read(buffer);
            baos.write(buffer, 0, read);
        }
        return new String(baos.toByteArray()).replaceAll("\\x0d\\x0a", System.getProperty("line.separator", "\n"));
    }

    private void assertFile(String test, String version, String content) throws Exception {
        assertEquals(readFile(test, version), content);
    }

    public static File getTestFileDirectory() throws Exception {
        String folder = TestUtil.getFolder("/smartproperties");
        if (folder == null) {
            URL resource = SmartProperties_Test.class.getResource("/");
            folder = resource.getPath() + "../smartproperties";
        }
        return new File(folder);
    }
}
