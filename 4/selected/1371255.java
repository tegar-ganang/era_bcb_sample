package de.jmda.util.fileset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import de.jmda.util.fileset.FileSet.RuleSetUpdateMode;

public class JUTFileSetJavaFilesOnly {

    private static final File TEST_DIRECTORY_ROOT = new File("./tmp");

    private static final File TEST_FILE_JAVA_SOURCE_ROOT_INCLUDE = new File(TEST_DIRECTORY_ROOT.getAbsolutePath() + "/include.java");

    private static final File TEST_FILE_JAVA_SOURCE_ROOT_EXCLUDE = new File(TEST_DIRECTORY_ROOT.getAbsolutePath() + "/exclude.java");

    private static final File TEST_FILE_NON_JAVA_ROOT = new File(TEST_DIRECTORY_ROOT.getAbsolutePath() + "/non.java.file");

    private static final File TEST_DIRECTORY_SUB = new File(TEST_DIRECTORY_ROOT.getAbsolutePath() + "/sub");

    private static final File TEST_FILE_JAVA_SOURCE_SUB_INCLUDE = new File(TEST_DIRECTORY_SUB.getAbsolutePath() + "/include.java");

    private static final File TEST_FILE_JAVA_SOURCE_SUB_EXCLUDE = new File(TEST_DIRECTORY_SUB.getAbsolutePath() + "/exclude.java");

    private static final File TEST_FILE_NON_JAVA_SUB = new File(TEST_DIRECTORY_SUB.getAbsolutePath() + "/non.java.file");

    private static JAXBContext jaxbContext;

    private Marshaller marshaller;

    private Unmarshaller unmarshaller;

    private FileSetJavaFilesOnly fileSetJavaFilesOnly;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        jaxbContext = JAXBContext.newInstance(new Class[] { FileSetJavaFilesOnly.class });
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        jaxbContext = null;
    }

    @Before
    public void setUp() throws Exception {
        marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        unmarshaller = jaxbContext.createUnmarshaller();
        fileSetJavaFilesOnly = new FileSetJavaFilesOnly();
        FileUtils.touch(TEST_FILE_JAVA_SOURCE_ROOT_INCLUDE);
        FileUtils.touch(TEST_FILE_JAVA_SOURCE_ROOT_EXCLUDE);
        FileUtils.touch(TEST_FILE_NON_JAVA_ROOT);
        FileUtils.touch(TEST_FILE_JAVA_SOURCE_SUB_INCLUDE);
        FileUtils.touch(TEST_FILE_JAVA_SOURCE_SUB_EXCLUDE);
        FileUtils.touch(TEST_FILE_NON_JAVA_SUB);
    }

    @After
    public void tearDown() throws Exception {
        marshaller = null;
        unmarshaller = null;
        FileUtils.forceDelete(TEST_DIRECTORY_ROOT);
    }

    @Test
    public void testMarshalling() throws JAXBException, IOException {
        fileSetJavaFilesOnly.includeDirectoryRecursive(TEST_DIRECTORY_ROOT, RuleSetUpdateMode.ADD_RULE);
        Writer writer = marshal(fileSetJavaFilesOnly);
        assertNotNull("unexpected null writer", writer);
        assertNotNull("unexpected null writer content", writer.toString());
        assertFalse("unexpected empty writer content", writer.toString().equals(""));
    }

    @Test
    public void testUnmarshalling() throws JAXBException, IOException {
        fileSetJavaFilesOnly.includeDirectoryRecursive(TEST_DIRECTORY_ROOT, RuleSetUpdateMode.ADD_RULE);
        Writer writer = marshal(fileSetJavaFilesOnly);
        fileSetJavaFilesOnly = unmarshal(writer);
        assertEquals("unexpected size of include directory filters recursive", 1, fileSetJavaFilesOnly.getIncludeDirectoryFiltersRecursive().size());
    }

    @Test
    public void testCalculateFileSetIncludeFilesValid() {
        fileSetJavaFilesOnly.includeFile(TEST_FILE_JAVA_SOURCE_ROOT_INCLUDE, RuleSetUpdateMode.ADD_RULE);
        fileSetJavaFilesOnly.includeFile(TEST_FILE_JAVA_SOURCE_SUB_INCLUDE, RuleSetUpdateMode.ADD_RULE);
        Set<File> files = fileSetJavaFilesOnly.calculateFileSet();
        assertEquals("unexpected size of files after including valid files", 2, files.size());
    }

    @Test
    public void testCalculateFileSetIncludeFilesInvalid() {
        fileSetJavaFilesOnly.includeFile(TEST_FILE_NON_JAVA_ROOT, RuleSetUpdateMode.ADD_RULE);
        fileSetJavaFilesOnly.includeFile(TEST_FILE_NON_JAVA_SUB, RuleSetUpdateMode.ADD_RULE);
        Set<File> files = fileSetJavaFilesOnly.calculateFileSet();
        assertEquals("unexpected size of files after including valid files", 0, files.size());
    }

    @Test
    public void testCalculateFileSetIncludeDirectories() {
        fileSetJavaFilesOnly.includeDirectory(TEST_DIRECTORY_ROOT, RuleSetUpdateMode.ADD_RULE);
        fileSetJavaFilesOnly.includeDirectory(TEST_DIRECTORY_SUB, RuleSetUpdateMode.ADD_RULE);
        Set<File> files = fileSetJavaFilesOnly.calculateFileSet();
        assertEquals("unexpected size of files after including valid files", 4, files.size());
    }

    @Test
    public void testCalculateFileSetIncludeDirectoryRecursive() {
        fileSetJavaFilesOnly.includeDirectoryRecursive(TEST_DIRECTORY_ROOT, RuleSetUpdateMode.ADD_RULE);
        Set<File> files = fileSetJavaFilesOnly.calculateFileSet();
        assertEquals("unexpected size of files after including valid files", 4, files.size());
    }

    @Test
    public void testCalculateFileSetExcludeFiles() {
        fileSetJavaFilesOnly.includeDirectoryRecursive(TEST_DIRECTORY_ROOT, RuleSetUpdateMode.ADD_RULE);
        fileSetJavaFilesOnly.excludeFile(TEST_FILE_JAVA_SOURCE_ROOT_EXCLUDE, RuleSetUpdateMode.ADD_RULE);
        fileSetJavaFilesOnly.excludeFile(TEST_FILE_JAVA_SOURCE_SUB_EXCLUDE, RuleSetUpdateMode.ADD_RULE);
        Set<File> files = fileSetJavaFilesOnly.calculateFileSet();
        assertEquals("unexpected size of files after excluding files", 2, files.size());
    }

    @Test
    public void testCalculateFileSetExcludeDirectory() {
        fileSetJavaFilesOnly.includeDirectoryRecursive(TEST_DIRECTORY_ROOT, RuleSetUpdateMode.ADD_RULE);
        fileSetJavaFilesOnly.excludeDirectory(TEST_DIRECTORY_SUB, RuleSetUpdateMode.ADD_RULE);
        Set<File> files = fileSetJavaFilesOnly.calculateFileSet();
        assertEquals("unexpected size of files after excluding directory", 2, files.size());
    }

    private Writer marshal(FileSetJavaFilesOnly fileSetJavaFilesOnly) {
        Writer result = new StringWriter();
        try {
            marshaller.marshal(fileSetJavaFilesOnly, result);
        } catch (JAXBException e) {
            fail("failure marshalling " + FileSetJavaFilesOnly.class.getName() + " object");
        }
        return result;
    }

    private FileSetJavaFilesOnly unmarshal(Writer writer) {
        FileSetJavaFilesOnly result = null;
        Reader reader = new StringReader(writer.toString());
        try {
            result = (FileSetJavaFilesOnly) unmarshaller.unmarshal(reader);
        } catch (JAXBException e) {
            fail("failure unmarshalling " + FileSetJavaFilesOnly.class.getName() + "object");
        }
        return result;
    }
}
