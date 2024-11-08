package uk.azdev.openfire.common;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.azdev.openfire.testutil.TestUtils;

public class IniDataTest {

    private IniData iniContents;

    @Before
    public void setUp() {
        iniContents = new IniData();
    }

    @Test
    public void testDefaultState() {
        assertEquals(0, iniContents.getAllSectionNames().size());
    }

    @Test
    public void testReadSinglePropertyIni() throws IOException {
        InputStream testIniStream = TestUtils.getTestResource(this.getClass(), "singlesection.ini");
        iniContents.read(testIniStream);
        checkSingleSectionProperties();
    }

    @Test
    public void testReadSinglePropertyIniFromSteam() throws IOException {
        InputStream testIniStream = TestUtils.getTestResource(this.getClass(), "singlesection.ini");
        iniContents = new IniData(testIniStream);
        checkSingleSectionProperties();
    }

    @Test
    public void testReadFromFile() throws IOException {
        String testFileName = copyTestResourceToFile("singlesection.ini");
        iniContents = new IniData(testFileName);
        checkSingleSectionProperties();
    }

    private String copyTestResourceToFile(String testResourceName) throws IOException, FileNotFoundException {
        InputStream testIniStream = TestUtils.getTestResource(this.getClass(), testResourceName);
        ReadableByteChannel testIniChannel = Channels.newChannel(testIniStream);
        File tempFile = File.createTempFile("openfire", "iniDataTest");
        FileOutputStream testFileStream = new FileOutputStream(tempFile);
        FileChannel fileChannel = testFileStream.getChannel();
        fileChannel.transferFrom(testIniChannel, 0, testIniStream.available());
        testIniChannel.close();
        fileChannel.close();
        return tempFile.getAbsolutePath();
    }

    private void checkSingleSectionProperties() {
        assertEquals(1, iniContents.getAllSectionNames().size());
        assertTrue(iniContents.hasSection("sectionA"));
        assertFalse(iniContents.hasSection("anotherSection"));
        assertEquals("xyz", iniContents.getStringProperty("sectionA", "stringProp"));
        assertEquals(100, iniContents.getIntegerProperty("sectionA", "intProp"));
        assertEquals(-200, iniContents.getIntegerProperty("sectionA", "negativeIntProp"));
        assertEquals("C:\\Program Files\\", iniContents.getStringProperty("sectionA", "pathProp"));
        assertTrue(iniContents.getBooleanProperty("sectionA", "booleanTrueProp"));
        assertTrue(iniContents.getBooleanProperty("sectionA", "booleanTrueProp2"));
        assertTrue(iniContents.getBooleanProperty("sectionA", "booleanTrueProp3"));
        assertFalse(iniContents.getBooleanProperty("sectionA", "booleanFalseProp"));
        assertFalse(iniContents.getBooleanProperty("sectionA", "booleanFalseProp2"));
        assertFalse(iniContents.getBooleanProperty("sectionA", "booleanFalseProp3"));
        assertEquals(10000000000L, iniContents.getLongProperty("sectionA", "longProp"));
        assertEquals(-10000000000L, iniContents.getLongProperty("sectionA", "negativeLongProp"));
        assertEquals(1.256, iniContents.getDoubleProperty("sectionA", "doubleProp"));
        assertEquals(-1.256, iniContents.getDoubleProperty("sectionA", "negativeDoubleProp"));
        assertEquals(new Date(100), iniContents.getDateProperty("sectionA", "dateProp"));
    }

    @Test
    public void testReadEmptyIni() throws IOException {
        InputStream testIniStream = TestUtils.getTestResource(this.getClass(), "empty.ini");
        iniContents.read(testIniStream);
        assertEquals(0, iniContents.getAllSectionNames().size());
        assertEquals(0, iniContents.getAllPropertyNames("missingSection").size());
    }

    @Test(expected = FileNotFoundException.class)
    public void testReadNonExistentFile() throws IOException {
        File f = File.createTempFile("missingFile", "iniDataTest");
        f.delete();
        iniContents.read(f);
    }

    @Test
    public void testIteratorOrder() throws IOException {
        InputStream testIniStream = TestUtils.getTestResource(this.getClass(), "multisection.ini");
        iniContents.read(testIniStream);
        List<String> sectionNames = iniContents.getAllSectionNames();
        Iterator<String> sectionNamesIter = sectionNames.iterator();
        assertEquals("sectionA", sectionNamesIter.next());
        assertEquals("sectionB", sectionNamesIter.next());
        assertEquals("sectionC", sectionNamesIter.next());
        assertEquals("sectionD", sectionNamesIter.next());
        List<String> sectionAProps = iniContents.getAllPropertyNames("sectionA");
        assertEquals(3, sectionAProps.size());
        Iterator<String> sectionAPropsIter = sectionAProps.iterator();
        assertEquals("property1", sectionAPropsIter.next());
        assertEquals("property2", sectionAPropsIter.next());
        assertEquals("property3", sectionAPropsIter.next());
        assertFalse(sectionAPropsIter.hasNext());
        List<String> sectionBProps = iniContents.getAllPropertyNames("sectionB");
        assertEquals(0, sectionBProps.size());
        Iterator<String> sectionBPropsIter = sectionBProps.iterator();
        assertFalse(sectionBPropsIter.hasNext());
        List<String> sectionCProps = iniContents.getAllPropertyNames("sectionC");
        assertEquals(2, sectionCProps.size());
        Iterator<String> sectionCPropsIter = sectionCProps.iterator();
        assertEquals("property1", sectionCPropsIter.next());
        assertEquals("property2", sectionCPropsIter.next());
        assertFalse(sectionCPropsIter.hasNext());
        List<String> sectionDProps = iniContents.getAllPropertyNames("sectionD");
        assertEquals(1, sectionDProps.size());
        Iterator<String> sectionDPropsIter = sectionDProps.iterator();
        assertEquals("property1", sectionDPropsIter.next());
        assertFalse(sectionDPropsIter.hasNext());
    }

    @Test
    public void testDefaultValues() throws IOException {
        InputStream testIniStream = TestUtils.getTestResource(this.getClass(), "singlesection.ini");
        iniContents.read(testIniStream);
        assertFalse(iniContents.hasPropertyInSection("sectionA", "missingProperty"));
        assertFalse(iniContents.hasPropertyInSection("missingSection", "missingProperty"));
        assertEquals("defaultValue", iniContents.getStringProperty("sectionA", "missingProperty", "defaultValue"));
        assertEquals("defaultValue", iniContents.getStringProperty("missingSection", "missingProperty", "defaultValue"));
        assertEquals(100, iniContents.getIntegerProperty("sectionA", "missingProperty", 100));
        assertEquals(100, iniContents.getIntegerProperty("sectionA", "stringProp", 100));
        assertEquals(true, iniContents.getBooleanProperty("sectionA", "missingProperty", true));
        assertEquals(true, iniContents.getBooleanProperty("sectionA", "stringProp", true));
        assertEquals(10000000000L, iniContents.getLongProperty("sectionA", "missingProperty", 10000000000L));
        assertEquals(10000000000L, iniContents.getLongProperty("sectionA", "stringProp", 10000000000L));
        assertEquals(new Date(100), iniContents.getDateProperty("sectionA", "missingProperty", new Date(100)));
        assertEquals(new Date(100), iniContents.getDateProperty("sectionA", "stringProp", new Date(100)));
        assertEquals(1.234, iniContents.getDoubleProperty("sectionA", "missingProperty", 1.234));
        assertEquals(1.234, iniContents.getDoubleProperty("sectionA", "stringProp", 1.234));
        assertEquals(0, iniContents.getIntegerProperty("sectionA", "missingProperty"));
        assertEquals(false, iniContents.getBooleanProperty("sectionA", "missingProperty"));
        assertEquals(0L, iniContents.getLongProperty("sectionA", "missingProperty"));
        assertEquals(new Date(0), iniContents.getDateProperty("sectionA", "missingProperty"));
    }

    @Test
    public void testReadIniWithErrors() throws IOException {
        InputStream testIniStream = TestUtils.getTestResource(this.getClass(), "errors.ini");
        iniContents.read(testIniStream);
        assertEquals(1, iniContents.getAllSectionNames().size());
        assertTrue(iniContents.hasSection("sectionA"));
        assertEquals(1, iniContents.getAllPropertyNames("sectionA").size());
        assertEquals("1", iniContents.getStringProperty("sectionA", "valueWithDuplicate"));
    }

    @Test
    public void testRecreateSingleSectionIni() throws IOException {
        iniContents.setStringProperty("sectionA", "stringProp", "xyz");
        iniContents.setIntegerProperty("sectionA", "intProp", 100);
        iniContents.setIntegerProperty("sectionA", "negativeIntProp", -200);
        iniContents.setStringProperty("sectionA", "pathProp", "C:\\Program Files\\");
        iniContents.setBooleanProperty("sectionA", "booleanTrueProp", true);
        iniContents.setStringProperty("sectionA", "booleanTrueProp2", "TRuE");
        iniContents.setIntegerProperty("sectionA", "booleanTrueProp3", 1);
        iniContents.setBooleanProperty("sectionA", "booleanFalseProp", false);
        iniContents.setStringProperty("sectionA", "booleanFalseProp2", "FaLSe");
        iniContents.setIntegerProperty("sectionA", "booleanFalseProp3", 0);
        iniContents.setLongProperty("sectionA", "longProp", 10000000000L);
        iniContents.setLongProperty("sectionA", "negativeLongProp", -10000000000L);
        iniContents.setDoubleProperty("sectionA", "doubleProp", 1.256);
        iniContents.setDoubleProperty("sectionA", "negativeDoubleProp", -1.256);
        iniContents.setDateProperty("sectionA", "dateProp", new Date(100));
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        iniContents.setLineFeedString("\r\n");
        iniContents.write(byteStream);
        byte[] bytes = byteStream.toByteArray();
        assertArrayEquals("arrays do not match", TestUtils.getByteArrayForResource(this.getClass(), "singlesection.ini"), bytes);
    }

    @Test
    public void testRecreateMultiSectionIni() throws IOException {
        iniContents.setStringProperty("sectionA", "property1", "abc");
        iniContents.setStringProperty("sectionA", "property2", "def");
        iniContents.setStringProperty("sectionA", "property3", "ghi");
        iniContents.setStringProperty("sectionB", "property1", "abc");
        iniContents.removePropertyFromSection("sectionB", "property1");
        iniContents.setStringProperty("sectionC", "property1", "abc");
        iniContents.setStringProperty("sectionC", "property2", "def");
        iniContents.setStringProperty("sectionD", "property1", "abc");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        iniContents.setLineFeedString("\r\n");
        iniContents.write(byteStream);
        byte[] bytes = byteStream.toByteArray();
        assertArrayEquals("arrays do not match", TestUtils.getByteArrayForResource(this.getClass(), "multisection.ini"), bytes);
    }

    @Test
    public void testRecreateMultiSectionIniToFile() throws IOException {
        iniContents.setStringProperty("sectionA", "property1", "abc");
        iniContents.setStringProperty("sectionA", "property2", "def");
        iniContents.setStringProperty("sectionA", "property3", "ghi");
        iniContents.setStringProperty("sectionB", "property1", "abc");
        iniContents.removePropertyFromSection("sectionB", "property1");
        iniContents.setStringProperty("sectionC", "property1", "abc");
        iniContents.setStringProperty("sectionC", "property2", "def");
        iniContents.setStringProperty("sectionD", "property1", "abc");
        File testFile = File.createTempFile("multiini", "iniDataTest");
        iniContents.setLineFeedString("\r\n");
        iniContents.write(testFile);
        byte[] bytes = TestUtils.getBytesFromFile(testFile);
        assertArrayEquals("arrays do not match", TestUtils.getByteArrayForResource(this.getClass(), "multisection.ini"), bytes);
    }

    @Test
    public void testRemovePropertyFromSection() {
        iniContents.setStringProperty("section", "testProperty", "hello");
        assertTrue(iniContents.hasPropertyInSection("section", "testProperty"));
        iniContents.removePropertyFromSection("section", "testProperty");
        assertFalse(iniContents.hasPropertyInSection("section", "testProperty"));
    }

    @Test
    public void testRemoveSection() {
        iniContents.setStringProperty("section", "testProperty", "hello");
        assertTrue(iniContents.hasPropertyInSection("section", "testProperty"));
        assertTrue(iniContents.hasSection("section"));
        iniContents.removeSection("section");
        assertFalse(iniContents.hasPropertyInSection("section", "testProperty"));
        assertFalse(iniContents.hasSection("section"));
    }

    @Test
    public void testRemovePropertyFromNonExistentSection() {
        iniContents.setStringProperty("section", "testProperty", "hello");
        iniContents.removeSection("section");
        iniContents.removePropertyFromSection("section", "testProperty");
        assertFalse(iniContents.hasPropertyInSection("section", "testProperty"));
    }

    @Test
    public void testSetLineFeedString() throws IOException {
        iniContents.setLineFeedString("teststring");
        assertEquals("teststring", iniContents.getLineFeedString());
        iniContents.setStringProperty("mysection", "myproperty", "hello");
        StringWriter writer = new StringWriter();
        iniContents.write(writer);
        assertEquals("[mysection]teststringmyproperty=helloteststringteststring", writer.toString());
    }

    @Test
    public void testSetStringProperty_withNullSectionName() {
        try {
            iniContents.setStringProperty(null, "aProperty", "hello");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty section name provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testSetStringProperty_withNullPropertyName() {
        try {
            iniContents.setStringProperty("section", null, "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty property name provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testSetStringProperty_withNullValue() {
        try {
            iniContents.setStringProperty("section", "property", null);
        } catch (IllegalArgumentException e) {
            assertEquals("null value provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testSetDateProperty_withNullValue() {
        try {
            iniContents.setDateProperty("section", "property", null);
        } catch (IllegalArgumentException e) {
            assertEquals("null value provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testSetStringProperty_withEmptySectionName() {
        try {
            iniContents.setStringProperty("", "aProperty", "hello");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty section name provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testSetStringProperty_withEmptyPropertyName() {
        try {
            iniContents.setStringProperty("section", "", "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty property name provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testGetStringProperty_withNullSectionName() {
        try {
            iniContents.getStringProperty(null, "aProperty", "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty section name provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testGetStringProperty_withNullPropertyName() {
        try {
            iniContents.getStringProperty("section", null, "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty property name provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testGetStringProperty_withEmptySectionName() {
        try {
            iniContents.getStringProperty("", "aProperty", "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty section name provided as an argument", e.getMessage());
        }
    }

    @Test
    public void testGetStringProperty_withEmptyPropertyName() {
        try {
            iniContents.getStringProperty("section", "", "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("null or empty property name provided as an argument", e.getMessage());
        }
    }
}
