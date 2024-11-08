package org.jomc.tools.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jomc.model.Dependency;
import org.jomc.model.Implementation;
import org.jomc.model.Message;
import org.jomc.model.ModelObject;
import org.jomc.model.Module;
import org.jomc.model.Modules;
import org.jomc.model.Multiplicity;
import org.jomc.model.Property;
import org.jomc.model.Specification;
import org.jomc.model.modlet.DefaultModelProvider;
import org.jomc.model.modlet.ModelHelper;
import org.jomc.modlet.Model;
import org.jomc.modlet.ModelContext;
import org.jomc.modlet.ModelContextFactory;
import org.jomc.modlet.ModelException;
import org.jomc.tools.ClassFileProcessor;
import org.jomc.tools.ResourceFileProcessor;
import org.jomc.tools.SourceFileProcessor;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test cases for class {@code org.jomc.tools.ClassFileProcessor}.
 *
 * @author <a href="mailto:schulte2005@users.sourceforge.net">Christian Schulte</a>
 * @version $JOMC$
 */
public class ClassFileProcessorTest extends JomcToolTest {

    /** Creates a new {@code ClassFileProcessorTest} instance. */
    public ClassFileProcessorTest() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public ClassFileProcessor getJomcTool() {
        return (ClassFileProcessor) super.getJomcTool();
    }

    /** {@inheritDoc} */
    @Override
    protected ClassFileProcessor newJomcTool() {
        return new ClassFileProcessor();
    }

    /** {@inheritDoc} */
    @Override
    protected Model newModel() {
        try {
            DefaultModelProvider.setDefaultModuleLocation(this.getClass().getPackage().getName().replace('.', '/') + "/jomc-tools.xml");
            Model m = this.getModelContext().findModel(ModelObject.MODEL_PUBLIC_ID);
            if (m != null) {
                final Modules modules = ModelHelper.getModules(m);
                if (modules != null) {
                    final Module cp = modules.getClasspathModule(Modules.getDefaultClasspathModuleName(), this.getClass().getClassLoader());
                    if (cp != null) {
                        modules.getModule().add(cp);
                    }
                }
                m = this.getModelContext().processModel(m);
            }
            return m;
        } catch (final ModelException e) {
            throw new AssertionError(e);
        } finally {
            DefaultModelProvider.setDefaultModuleLocation(null);
        }
    }

    /**
     * Gets a directory holding class files corresponding to the model of the instance.
     *
     * @return A directory holding class files corresponding to the model of the instance.
     *
     * @see #getNextOutputDirectory()
     */
    public final File getNextClassesDirectory() {
        try {
            final File classesDirectory = this.getNextOutputDirectory();
            this.unzipResource("classfiles.zip", classesDirectory);
            return classesDirectory;
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public final void testClassFileProcessorNullPointerException() throws Exception {
        final Marshaller marshaller = this.getModelContext().createMarshaller(ModelObject.MODEL_PUBLIC_ID);
        final Unmarshaller unmarshaller = this.getModelContext().createUnmarshaller(ModelObject.MODEL_PUBLIC_ID);
        final URL object = this.getClass().getResource("/java/lang/Object.class");
        InputStream in = null;
        JavaClass objectClass = null;
        boolean suppressExceptionOnClose = true;
        try {
            in = object.openStream();
            objectClass = new ClassParser(in, object.toExternalForm()).parse();
            suppressExceptionOnClose = false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                if (!suppressExceptionOnClose) {
                    throw e;
                }
            }
        }
        try {
            this.getJomcTool().commitModelObjects(null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(this.getModelContext(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects((Implementation) null, (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Implementation(), (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Implementation(), this.getModelContext(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects((Implementation) null, (Marshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Implementation(), (Marshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Implementation(), marshaller, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects((Module) null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Module(), null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Module(), this.getModelContext(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects((Specification) null, (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Specification(), (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Specification(), this.getModelContext(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects((Specification) null, (Marshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Specification(), (Marshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().commitModelObjects(new Specification(), marshaller, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().decodeModelObject(null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().decodeModelObject(unmarshaller, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().decodeModelObject(unmarshaller, new byte[0], null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().encodeModelObject(null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().encodeModelObject(marshaller, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().getClassfileAttribute(null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().getClassfileAttribute(objectClass, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().setClassfileAttribute(null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().setClassfileAttribute(objectClass, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), new File("/"), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects((Module) null, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Module(), null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Module(), this.getModelContext(), null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Module(), this.getModelContext(), new File("/"), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects((Specification) null, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Specification(), null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Specification(), this.getModelContext(), null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Specification(), this.getModelContext(), new File("/"), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects((Implementation) null, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Implementation(), null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Implementation(), this.getModelContext(), null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Implementation(), this.getModelContext(), new File("/"), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects((Specification) null, null, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Specification(), null, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Specification(), marshaller, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Specification(), marshaller, unmarshaller, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Specification(), marshaller, unmarshaller, objectClass, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects((Implementation) null, null, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Implementation(), null, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Implementation(), marshaller, null, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Implementation(), marshaller, unmarshaller, null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().transformModelObjects(new Implementation(), marshaller, unmarshaller, objectClass, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(null, (File) null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(this.getModelContext(), (File) null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Module) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Module(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Module) null, null, (File) null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Module(), null, (File) null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Module(), this.getModelContext(), (File) null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Specification) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Specification(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Specification) null, (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Specification(), (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Specification(), this.getModelContext(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Implementation) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Implementation(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Implementation) null, (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Implementation(), (ModelContext) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Implementation(), this.getModelContext(), null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Specification) null, (Unmarshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Specification(), (Unmarshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Specification(), unmarshaller, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects((Implementation) null, (Unmarshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Implementation(), (Unmarshaller) null, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
        try {
            this.getJomcTool().validateModelObjects(new Implementation(), unmarshaller, null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNullPointerException(e);
        }
    }

    @Test
    public final void testCommitTransformValidateClasses() throws Exception {
        final File nonExistentDirectory = this.getNextOutputDirectory();
        final File emptyDirectory = this.getNextOutputDirectory();
        assertTrue(emptyDirectory.mkdirs());
        final File allClasses = this.getNextClassesDirectory();
        final ClassLoader allClassesLoader = new URLClassLoader(new URL[] { allClasses.toURI().toURL() });
        final File moduleClasses = this.getNextClassesDirectory();
        final ClassLoader moduleClassesLoader = new URLClassLoader(new URL[] { moduleClasses.toURI().toURL() });
        final File implementationClasses = this.getNextClassesDirectory();
        final ClassLoader implementationClassesLoader = new URLClassLoader(new URL[] { implementationClasses.toURI().toURL() });
        final File specificationClasses = this.getNextClassesDirectory();
        final ClassLoader specificationClassesLoader = new URLClassLoader(new URL[] { specificationClasses.toURI().toURL() });
        final File uncommittedClasses = this.getNextClassesDirectory();
        final ClassLoader uncommittedClassesLoader = new URLClassLoader(new URL[] { uncommittedClasses.toURI().toURL() });
        final Module m = this.getJomcTool().getModules().getModule("JOMC Tools");
        final Specification s = this.getJomcTool().getModules().getSpecification("org.jomc.tools.ClassFileProcessor");
        final Implementation i = this.getJomcTool().getModules().getImplementation("org.jomc.tools.ClassFileProcessor");
        assertNotNull(m);
        assertNotNull(s);
        assertNotNull(i);
        final List<Transformer> transformers = Arrays.asList(new Transformer[] { this.getTransformer("no-op.xsl") });
        final List<Transformer> illegalSpecificationTransformers = Arrays.asList(new Transformer[] { this.getTransformer("illegal-specification-transformation.xsl") });
        final List<Transformer> illegalSpecificationsTransformers = Arrays.asList(new Transformer[] { this.getTransformer("illegal-specifications-transformation.xsl") });
        final List<Transformer> illegalDependenciesTransformers = Arrays.asList(new Transformer[] { this.getTransformer("illegal-dependencies-transformation.xsl") });
        final List<Transformer> illegalMessagesTransformers = Arrays.asList(new Transformer[] { this.getTransformer("illegal-messages-transformation.xsl") });
        final List<Transformer> illegalPropertiesTransformers = Arrays.asList(new Transformer[] { this.getTransformer("illegal-properties-transformation.xsl") });
        try {
            this.getJomcTool().commitModelObjects(this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().commitModelObjects(this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().commitModelObjects(m, this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().commitModelObjects(m, this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().commitModelObjects(s, this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().commitModelObjects(s, this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().commitModelObjects(i, this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().commitModelObjects(i, this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), nonExistentDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), emptyDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(m, this.getModelContext(), nonExistentDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(m, this.getModelContext(), emptyDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(s, this.getModelContext(), nonExistentDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(s, this.getModelContext(), emptyDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(i, this.getModelContext(), nonExistentDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(i, this.getModelContext(), emptyDirectory, transformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(m, this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(m, this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(s, this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(s, this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(i, this.getModelContext(), nonExistentDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(i, this.getModelContext(), emptyDirectory);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        this.getJomcTool().commitModelObjects(this.getModelContext(), allClasses);
        this.getJomcTool().commitModelObjects(m, this.getModelContext(), moduleClasses);
        this.getJomcTool().commitModelObjects(s, this.getModelContext(), specificationClasses);
        this.getJomcTool().commitModelObjects(i, this.getModelContext(), implementationClasses);
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), allClasses, illegalSpecificationTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), allClasses, illegalSpecificationsTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), allClasses, illegalDependenciesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), allClasses, illegalMessagesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(this.getModelContext(), allClasses, illegalPropertiesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(m, this.getModelContext(), moduleClasses, illegalSpecificationTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(m, this.getModelContext(), moduleClasses, illegalSpecificationsTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(m, this.getModelContext(), moduleClasses, illegalDependenciesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(m, this.getModelContext(), moduleClasses, illegalMessagesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(m, this.getModelContext(), moduleClasses, illegalPropertiesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(s, this.getModelContext(), specificationClasses, illegalSpecificationTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(i, this.getModelContext(), implementationClasses, illegalSpecificationsTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(i, this.getModelContext(), implementationClasses, illegalDependenciesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(i, this.getModelContext(), implementationClasses, illegalMessagesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().transformModelObjects(i, this.getModelContext(), implementationClasses, illegalPropertiesTransformers);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        this.getJomcTool().transformModelObjects(this.getModelContext(), allClasses, transformers);
        this.getJomcTool().transformModelObjects(m, this.getModelContext(), moduleClasses, transformers);
        this.getJomcTool().transformModelObjects(s, this.getModelContext(), specificationClasses, transformers);
        this.getJomcTool().transformModelObjects(i, this.getModelContext(), implementationClasses, transformers);
        this.getJomcTool().validateModelObjects(ModelContextFactory.newInstance().newModelContext(allClassesLoader));
        this.getJomcTool().validateModelObjects(m, ModelContextFactory.newInstance().newModelContext(moduleClassesLoader));
        this.getJomcTool().validateModelObjects(s, ModelContextFactory.newInstance().newModelContext(specificationClassesLoader));
        this.getJomcTool().validateModelObjects(i, ModelContextFactory.newInstance().newModelContext(implementationClassesLoader));
        this.getJomcTool().validateModelObjects(this.getModelContext(), allClasses);
        this.getJomcTool().validateModelObjects(m, this.getModelContext(), moduleClasses);
        this.getJomcTool().validateModelObjects(s, this.getModelContext(), specificationClasses);
        this.getJomcTool().validateModelObjects(i, this.getModelContext(), implementationClasses);
        this.getJomcTool().validateModelObjects(ModelContextFactory.newInstance().newModelContext(uncommittedClassesLoader));
        this.getJomcTool().validateModelObjects(this.getModelContext(), uncommittedClasses);
        final Model model = this.getJomcTool().getModel();
        final Model copy = model.clone();
        final Modules modules = ModelHelper.getModules(copy);
        final Module testModule = modules.getModule("JOMC Tools");
        assertNotNull(testModule);
        final Specification classFileProcessor = testModule.getSpecifications().getSpecification(ClassFileProcessor.class.getName());
        final Specification resourceFileProcessor = testModule.getSpecifications().getSpecification(ResourceFileProcessor.class.getName());
        final Specification sourceFileProcessor = testModule.getSpecifications().getSpecification(SourceFileProcessor.class.getName());
        final Implementation classFileProcessorImpl = testModule.getImplementations().getImplementation(ClassFileProcessor.class.getName());
        final Implementation resourceFileProcessorImpl = testModule.getImplementations().getImplementation(ResourceFileProcessor.class.getName());
        final Implementation sourceFileProcessorImpl = testModule.getImplementations().getImplementation(SourceFileProcessor.class.getName());
        assertNotNull(classFileProcessor);
        assertNotNull(resourceFileProcessor);
        assertNotNull(sourceFileProcessor);
        assertNotNull(classFileProcessorImpl);
        assertNotNull(resourceFileProcessorImpl);
        assertNotNull(sourceFileProcessorImpl);
        classFileProcessor.setMultiplicity(Multiplicity.ONE);
        classFileProcessor.setScope("TEST");
        resourceFileProcessor.setMultiplicity(Multiplicity.ONE);
        resourceFileProcessor.setScope("TEST");
        sourceFileProcessor.setMultiplicity(Multiplicity.ONE);
        sourceFileProcessor.setScope("TEST");
        Property p = classFileProcessorImpl.getProperties().getProperty("TestStringProperty");
        assertNotNull(p);
        assertNotNull(classFileProcessorImpl.getProperties().getProperty().remove(p));
        p = classFileProcessorImpl.getProperties().getProperty("TestPrimitiveProperty");
        assertNotNull(p);
        p.setType(null);
        p = resourceFileProcessorImpl.getProperties().getProperty("TestStringProperty");
        assertNotNull(p);
        assertNotNull(resourceFileProcessorImpl.getProperties().getProperty().remove(p));
        p = resourceFileProcessorImpl.getProperties().getProperty("TestPrimitiveProperty");
        assertNotNull(p);
        p.setType(null);
        p = sourceFileProcessorImpl.getProperties().getProperty("TestStringProperty");
        assertNotNull(p);
        assertNotNull(sourceFileProcessorImpl.getProperties().getProperty().remove(p));
        p = sourceFileProcessorImpl.getProperties().getProperty("TestPrimitiveProperty");
        assertNotNull(p);
        p.setType(null);
        Message message = classFileProcessorImpl.getMessages().getMessage("TestMessage");
        assertNotNull(message);
        assertNotNull(classFileProcessorImpl.getMessages().getMessage().remove(message));
        message = resourceFileProcessorImpl.getMessages().getMessage("TestMessage");
        assertNotNull(message);
        assertNotNull(resourceFileProcessorImpl.getMessages().getMessage().remove(message));
        message = sourceFileProcessorImpl.getMessages().getMessage("TestMessage");
        assertNotNull(message);
        assertNotNull(sourceFileProcessorImpl.getMessages().getMessage().remove(message));
        Dependency dependency = classFileProcessorImpl.getDependencies().getDependency("Locale");
        assertNotNull(dependency);
        dependency.setImplementationName(null);
        dependency.setVersion(Integer.toString(Integer.MAX_VALUE));
        dependency = classFileProcessorImpl.getDependencies().getDependency("JavaClasses");
        assertNotNull(dependency);
        assertNotNull(classFileProcessorImpl.getDependencies().getDependency().remove(dependency));
        dependency = resourceFileProcessorImpl.getDependencies().getDependency("Locale");
        assertNotNull(dependency);
        dependency.setImplementationName(null);
        dependency.setVersion(Integer.toString(Integer.MAX_VALUE));
        dependency = resourceFileProcessorImpl.getDependencies().getDependency("JavaBundles");
        assertNotNull(dependency);
        assertNotNull(resourceFileProcessorImpl.getDependencies().getDependency().remove(dependency));
        dependency = sourceFileProcessorImpl.getDependencies().getDependency("Locale");
        assertNotNull(dependency);
        dependency.setImplementationName(null);
        dependency.setVersion(Integer.toString(Integer.MAX_VALUE));
        dependency = sourceFileProcessorImpl.getDependencies().getDependency("JavaSources");
        assertNotNull(dependency);
        assertNotNull(sourceFileProcessorImpl.getDependencies().getDependency().remove(dependency));
        this.getJomcTool().setModel(copy);
        this.getJomcTool().validateModelObjects(ModelContextFactory.newInstance().newModelContext(allClassesLoader));
        this.getJomcTool().validateModelObjects(testModule, ModelContextFactory.newInstance().newModelContext(moduleClassesLoader));
        this.getJomcTool().validateModelObjects(classFileProcessor, ModelContextFactory.newInstance().newModelContext(specificationClassesLoader));
        this.getJomcTool().validateModelObjects(classFileProcessorImpl, ModelContextFactory.newInstance().newModelContext(implementationClassesLoader));
        this.getJomcTool().validateModelObjects(this.getModelContext(), allClasses);
        this.getJomcTool().validateModelObjects(testModule, this.getModelContext(), moduleClasses);
        this.getJomcTool().validateModelObjects(classFileProcessor, this.getModelContext(), specificationClasses);
        this.getJomcTool().validateModelObjects(classFileProcessorImpl, this.getModelContext(), implementationClasses);
        this.getJomcTool().validateModelObjects(ModelContextFactory.newInstance().newModelContext(uncommittedClassesLoader));
        this.getJomcTool().validateModelObjects(this.getModelContext(), uncommittedClasses);
        classFileProcessor.setClazz(this.getClass().getPackage().getName() + ".DoesNotExist");
        classFileProcessorImpl.setClazz(this.getClass().getPackage().getName() + ".DoesNotExist");
        resourceFileProcessor.setClazz(this.getClass().getPackage().getName() + ".DoesNotExist");
        resourceFileProcessorImpl.setClazz(this.getClass().getPackage().getName() + ".DoesNotExist");
        sourceFileProcessor.setClazz(this.getClass().getPackage().getName() + ".DoesNotExist");
        sourceFileProcessorImpl.setClazz(this.getClass().getPackage().getName() + ".DoesNotExist");
        try {
            this.getJomcTool().validateModelObjects(ModelContextFactory.newInstance().newModelContext(allClassesLoader));
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(testModule, ModelContextFactory.newInstance().newModelContext(moduleClassesLoader));
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(classFileProcessor, ModelContextFactory.newInstance().newModelContext(specificationClassesLoader));
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(classFileProcessorImpl, ModelContextFactory.newInstance().newModelContext(implementationClassesLoader));
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(this.getModelContext(), allClasses);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(testModule, this.getModelContext(), moduleClasses);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(classFileProcessor, this.getModelContext(), specificationClasses);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(classFileProcessorImpl, this.getModelContext(), implementationClasses);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(ModelContextFactory.newInstance().newModelContext(uncommittedClassesLoader));
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        try {
            this.getJomcTool().validateModelObjects(this.getModelContext(), uncommittedClasses);
            fail("Expected IOException not thrown.");
        } catch (final IOException e) {
            assertNotNull(e.getMessage());
            System.out.println(e);
        }
        this.getJomcTool().setModel(model);
    }

    @Test
    public final void testCopyConstructor() throws Exception {
        try {
            new ClassFileProcessor(null);
            fail("Expected NullPointerException not thrown.");
        } catch (final NullPointerException e) {
            assertNotNull(e.getMessage());
            System.out.println(e.toString());
        }
        new ClassFileProcessor(this.getJomcTool());
    }

    @Test
    public final void testClassFileProcessorModelObjectsNotFound() throws Exception {
        final JavaClass object = new ClassParser(ClassLoader.getSystemResourceAsStream("java/lang/Object.class"), "Object.class").parse();
        final Module m = new Module();
        m.setName("DOES_NOT_EXIST");
        final Specification s = new Specification();
        s.setIdentifier("DOES_NOT_EXIST)");
        final Implementation i = new Implementation();
        i.setIdentifier("DOES_NOT_EXIST");
        final Model oldModel = this.getJomcTool().getModel();
        this.getJomcTool().setModel(null);
        this.getJomcTool().commitModelObjects(this.getModelContext(), new File("/tmp"));
        this.getJomcTool().commitModelObjects(m, this.getModelContext(), new File("/tmp"));
        this.getJomcTool().commitModelObjects(s, this.getModelContext(), new File("/tmp"));
        this.getJomcTool().commitModelObjects(i, this.getModelContext(), new File("/tmp"));
        this.getJomcTool().commitModelObjects(s, this.getModelContext().createMarshaller(ModelObject.MODEL_PUBLIC_ID), object);
        this.getJomcTool().commitModelObjects(i, this.getModelContext().createMarshaller(ModelObject.MODEL_PUBLIC_ID), object);
        this.getJomcTool().validateModelObjects(this.getModelContext());
        this.getJomcTool().validateModelObjects(m, this.getModelContext());
        this.getJomcTool().validateModelObjects(s, this.getModelContext());
        this.getJomcTool().validateModelObjects(i, this.getModelContext());
        this.getJomcTool().validateModelObjects(this.getModelContext(), new File("/tmp"));
        this.getJomcTool().validateModelObjects(m, this.getModelContext(), new File("/tmp"));
        this.getJomcTool().validateModelObjects(s, this.getModelContext(), new File("/tmp"));
        this.getJomcTool().validateModelObjects(i, this.getModelContext(), new File("/tmp"));
        this.getJomcTool().validateModelObjects(s, this.getModelContext().createUnmarshaller(ModelObject.MODEL_PUBLIC_ID), object);
        this.getJomcTool().validateModelObjects(i, this.getModelContext().createUnmarshaller(ModelObject.MODEL_PUBLIC_ID), object);
        this.getJomcTool().transformModelObjects(this.getModelContext(), new File("/tmp"), Collections.<Transformer>emptyList());
        this.getJomcTool().transformModelObjects(m, this.getModelContext(), new File("/tmp"), Collections.<Transformer>emptyList());
        this.getJomcTool().transformModelObjects(s, this.getModelContext(), new File("/tmp"), Collections.<Transformer>emptyList());
        this.getJomcTool().transformModelObjects(i, this.getModelContext(), new File("/tmp"), Collections.<Transformer>emptyList());
        this.getJomcTool().transformModelObjects(s, this.getModelContext().createMarshaller(ModelObject.MODEL_PUBLIC_ID), this.getModelContext().createUnmarshaller(ModelObject.MODEL_PUBLIC_ID), object, Collections.<Transformer>emptyList());
        this.getJomcTool().transformModelObjects(i, this.getModelContext().createMarshaller(ModelObject.MODEL_PUBLIC_ID), this.getModelContext().createUnmarshaller(ModelObject.MODEL_PUBLIC_ID), object, Collections.<Transformer>emptyList());
        this.getJomcTool().setModel(oldModel);
    }

    private Transformer getTransformer(final String resource) throws URISyntaxException, TransformerConfigurationException {
        final TransformerFactory transformerFactory = TransformerFactory.newInstance();
        final URL url = this.getClass().getResource(resource);
        assertNotNull(url);
        final Transformer transformer = transformerFactory.newTransformer(new StreamSource(url.toURI().toASCIIString()));
        return transformer;
    }

    private void unzipResource(final String resourceName, final File targetDirectory) throws IOException {
        final URL resource = this.getClass().getResource(resourceName);
        assertNotNull("Expected '" + resourceName + "' not found.", resource);
        assertTrue(targetDirectory.isAbsolute());
        FileUtils.deleteDirectory(targetDirectory);
        assertTrue(targetDirectory.mkdirs());
        ZipInputStream in = null;
        boolean suppressExceptionOnClose = true;
        try {
            in = new ZipInputStream(resource.openStream());
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    continue;
                }
                final File dest = new File(targetDirectory, e.getName());
                assertTrue(dest.isAbsolute());
                OutputStream out = null;
                try {
                    out = FileUtils.openOutputStream(dest);
                    IOUtils.copy(in, out);
                    suppressExceptionOnClose = false;
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                        suppressExceptionOnClose = true;
                    } catch (final IOException ex) {
                        if (!suppressExceptionOnClose) {
                            throw ex;
                        }
                    }
                }
                in.closeEntry();
            }
            suppressExceptionOnClose = false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (final IOException e) {
                if (!suppressExceptionOnClose) {
                    throw e;
                }
            }
        }
    }
}
