package org.jomc.tools.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBException;
import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jomc.model.Implementation;
import org.jomc.model.Module;
import org.jomc.model.Specification;
import org.jomc.tools.JavaSources;
import org.xml.sax.SAXException;

/**
 * Tests {@code JavaSources} implementations.
 *
 * @author <a href="mailto:cs@jomc.org">Christian Schulte</a>
 * @version $Id: JavaSourcesTest.java 744 2009-10-06 04:43:21Z schulte2005 $
 */
public class JavaSourcesTest extends JomcToolTest {

    /** Serial number of the test sources directory. */
    private int testSourcesId;

    /** The {@code JavaSources} instance tests are performed with. */
    private JavaSources testTool;

    @Override
    public JavaSources getTestTool() throws IOException, SAXException, JAXBException {
        if (this.testTool == null) {
            this.testTool = new JavaSources();
            this.testTool.setModules(this.getTestModules());
        }
        return this.testTool;
    }

    /**
     * Gets the directory to write sources to.
     *
     * @return The directory to write sources to.
     *
     * @throws IOException if getting the directory fails.
     */
    public File getTestSourcesDirectory() throws IOException {
        final File testSourcesDirectory = new File(this.getTestProperty("testSourcesDirectory"), Integer.toString(this.testSourcesId++));
        if (testSourcesDirectory.exists()) {
            FileUtils.cleanDirectory(testSourcesDirectory);
        }
        return testSourcesDirectory;
    }

    private String getTestProperty(final String key) throws IOException {
        final java.util.Properties p = new java.util.Properties();
        final InputStream in = this.getClass().getResourceAsStream("JavaSourcesTest.properties");
        p.load(in);
        in.close();
        return p.getProperty(key);
    }

    @Override
    public void testNullPointerException() throws Exception {
        super.testNullPointerException();
        try {
            this.getTestTool().getImplementationEditor(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().getSpecificationEditor(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().manageSources(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().manageSources((Implementation) null, new File("/"));
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().manageSources(new Implementation(), null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().manageSources((Module) null, new File("/"));
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().manageSources(new Module(), null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().manageSources((Specification) null, new File("/"));
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getTestTool().manageSources(new Specification(), null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        final JavaSources.JavaSpecificationEditor specEditor = this.getTestTool().getSpecificationEditor(new Specification());
        Assert.assertNotNull(specEditor);
        try {
            specEditor.editAnnotationsSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            specEditor.editDocumentationSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            specEditor.editLicenseSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        final JavaSources.JavaImplementationEditor implEditor = this.getTestTool().getImplementationEditor(new Implementation());
        Assert.assertNotNull(implEditor);
        try {
            implEditor.editAnnotationsSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editConstructorsSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editDefaultConstructorSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editDependenciesSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editDocumentationSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editLicenseSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editMessagesSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editPropertiesSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.editSection(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            implEditor.getOutput(null);
            Assert.fail("Expected NullPointerException not thrown.");
        } catch (NullPointerException e) {
            assertNullPointerException(e);
        }
    }

    @Override
    public void testNotNull() throws Exception {
        final Implementation i = new Implementation();
        i.setIdentifier("TEST");
        i.setClazz("TEST");
        final Specification s = new Specification();
        s.setIdentifier("TEST");
        s.setClazz("TEST");
        Assert.assertNotNull(this.getTestTool().getImplementationEditor(i));
        Assert.assertNotNull(this.getTestTool().getSpecificationEditor(s));
    }

    public void testManageSources() throws Exception {
        this.getTestTool().manageSources(this.getTestSourcesDirectory());
        this.getTestTool().manageSources(this.getTestTool().getModules().getModule("Module"), this.getTestSourcesDirectory());
        final File implementationDirectory = this.getTestSourcesDirectory();
        this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
        this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
        final File specificationDirectory = this.getTestSourcesDirectory();
        this.getTestTool().manageSources(this.getTestTool().getModules().getSpecification("Specification"), specificationDirectory);
        this.getTestTool().manageSources(this.getTestTool().getModules().getSpecification("Specification"), specificationDirectory);
        IOUtils.copy(this.getClass().getResourceAsStream("IllegalImplementationSource.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        IOUtils.copy(this.getClass().getResourceAsStream("IllegalSpecificationSource.java.txt"), new FileOutputStream(new File(specificationDirectory, "Specification.java")));
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e.toString());
        }
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getSpecification("Specification"), specificationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e.toString());
        }
        this.getTestTool().setProfile("DOES_NOT_EXIST");
        this.getTestTool().manageSources(this.getTestSourcesDirectory());
        this.getTestTool().manageSources(this.getTestTool().getModules().getModule("Module"), this.getTestSourcesDirectory());
        this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), this.getTestSourcesDirectory());
        this.getTestTool().manageSources(this.getTestTool().getModules().getSpecification("Specification"), this.getTestSourcesDirectory());
    }

    public void testMandatorySections() throws Exception {
        final File specificationDirectory = this.getTestSourcesDirectory();
        final File implementationDirectory = this.getTestSourcesDirectory();
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutAnnotationsSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e);
        }
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutDependenciesSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e);
        }
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutMessagesSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e);
        }
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutPropertiesSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e);
        }
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationOfSpecificationWithoutConstructorsSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "ImplementationOfSpecification.java")));
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("ImplementationOfSpecification"), implementationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e);
        }
        IOUtils.copy(this.getClass().getResourceAsStream("SpecificationWithoutAnnotationsSection.java.txt"), new FileOutputStream(new File(specificationDirectory, "Specification.java")));
        try {
            this.getTestTool().manageSources(this.getTestTool().getModules().getSpecification("Specification"), specificationDirectory);
            Assert.fail("Expected IOException not thrown.");
        } catch (IOException e) {
            Assert.assertNotNull(e.getMessage());
            System.out.println(e);
        }
    }

    public void testOptionalSections() throws Exception {
        final File implementationDirectory = this.getTestSourcesDirectory();
        final File specificationDirectory = this.getTestSourcesDirectory();
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutConstructorsSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutDefaultConstructorSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutDocumentationSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
        IOUtils.copy(this.getClass().getResourceAsStream("ImplementationWithoutLicenseSection.java.txt"), new FileOutputStream(new File(implementationDirectory, "Implementation.java")));
        this.getTestTool().manageSources(this.getTestTool().getModules().getImplementation("Implementation"), implementationDirectory);
        IOUtils.copy(this.getClass().getResourceAsStream("SpecificationWithoutDocumentationSection.java.txt"), new FileOutputStream(new File(specificationDirectory, "Specification.java")));
        this.getTestTool().manageSources(this.getTestTool().getModules().getSpecification("Specification"), specificationDirectory);
        IOUtils.copy(this.getClass().getResourceAsStream("SpecificationWithoutLicenseSection.java.txt"), new FileOutputStream(new File(specificationDirectory, "Specification.java")));
        this.getTestTool().manageSources(this.getTestTool().getModules().getSpecification("Specification"), specificationDirectory);
    }
}
