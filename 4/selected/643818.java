package de.jmda.util.jaxb;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;
import de.jmda.util.JUTRuntimeUtil;
import de.jmda.util.jaxb.JAXBConfigUtil;
import de.jmda.util.jaxb.JAXBConfigUtil.NamespaceInfo;
import de.jmda.util.jaxb.SchemaGenerator;

/**
 * @author roger@jmda.de
 */
public class JUTXMLSchema {

    private static final Logger LOGGER = Logger.getLogger(JUTRuntimeUtil.class);

    private static JAXBContext JAXB_CONTEXT;

    private static JAXBConfigUtil JAXB_CONFIG_UTIL;

    /**
	 * class name pattern:
	 * xmlroot annotation == 1
	 * namespace in annotation == 1
	 * context member == 1
	 */
    @XmlRootElement(namespace = "root")
    private static class XMLRootNamespaceContextMember111 {

        @XmlAttribute(required = true)
        private String required;
    }

    /**
	 * class name pattern:
	 * xmlroot annotation == 1
	 * namespace in annotation == 0
	 * context member == 1
	 */
    @XmlRootElement
    private static class XMLRootNamespaceContextMember101 {

        @XmlAttribute(required = true)
        private String required;
    }

    /**
	 * class name pattern:
	 * xmlroot annotation == 1
	 * namespace in annotation == 0
	 * context member == 1
	 */
    @XmlRootElement
    private static class XMLRootNamespaceContext100 {

        @XmlAttribute(required = true)
        private String required;
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        JAXB_CONTEXT = JAXBContext.newInstance(XMLRootNamespaceContextMember111.class, XMLRootNamespaceContextMember101.class);
        JAXB_CONFIG_UTIL = new JAXBConfigUtil();
        JAXB_CONFIG_UTIL.add(new NamespaceInfo("", "core.xsd", "c"));
        JAXB_CONFIG_UTIL.add(new NamespaceInfo("core", "core.xsd", "c"));
        JAXB_CONFIG_UTIL.add(new NamespaceInfo("root", "root.xsd", "r"));
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
	 * Triggers schema file generation and checks if the generated schema files
	 * exist.
	 *
	 * @throws IOException
	 */
    @Test
    public void testSchemaGeneration() throws IOException {
        SchemaGenerator schemaGenerator = new SchemaGenerator(JAXB_CONTEXT, getNamespaceURISchemaFilenameMap());
        schemaGenerator.generateSchema();
        for (File schemafile : schemaGenerator.getGeneratedSchemafiles()) {
            assertTrue("failure finding schemafile " + schemafile.getAbsolutePath(), schemafile.exists());
        }
    }

    /**
	 * Triggers marshalling without schema validation.
	 *
	 * @throws JAXBException
	 */
    @Test
    public void testJAXBMarshallingWithoutSchema() throws JAXBException {
        XMLRootNamespaceContextMember111 sample = new XMLRootNamespaceContextMember111();
        Writer writer = new StringWriter();
        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        JAXB_CONFIG_UTIL.activateFormattedOutput(marshaller);
        marshaller.marshal(sample, writer);
        LOGGER.debug(writer.toString());
    }

    /**
	 * Triggers marshalling with schema validation for a valid object.
	 *
	 * @throws JAXBException
	 * @throws SAXException
	 * @throws IOException
	 */
    @Test
    public void testJAXBMarshallingWithSchemaValid() throws JAXBException, SAXException, IOException {
        XMLRootNamespaceContextMember111 sample = new XMLRootNamespaceContextMember111();
        sample.required = "";
        Writer writer = new StringWriter();
        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        JAXB_CONFIG_UTIL.activateFormattedOutput(marshaller);
        marshaller.setSchema(getSchema());
        marshaller.marshal(sample, writer);
        LOGGER.debug(writer.toString());
    }

    /**
	 * Triggers marshalling with schema validation for an invalid object.
	 *
	 * @throws JAXBException
	 * @throws SAXException
	 * @throws IOException
	 */
    @Test(expected = MarshalException.class)
    public void testJAXBMarshallingWithSchemaInvalid() throws JAXBException, SAXException, IOException {
        XMLRootNamespaceContextMember111 sample = new XMLRootNamespaceContextMember111();
        Writer writer = new StringWriter();
        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        JAXB_CONFIG_UTIL.activateFormattedOutput(marshaller);
        marshaller.setSchema(getSchema());
        marshaller.marshal(sample, writer);
    }

    /**
	 * Triggers marshalling for an object that is not contained in the context.
	 *
	 * @throws JAXBException
	 * @throws SAXException
	 * @throws IOException
	 */
    @Test(expected = JAXBException.class)
    public void testJAXBMarshallingWithObjectNotContainedInContext() throws JAXBException, SAXException, IOException {
        XMLRootNamespaceContext100 sample = new XMLRootNamespaceContext100();
        Writer writer = new StringWriter();
        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        JAXB_CONFIG_UTIL.activateFormattedOutput(marshaller);
        marshaller.marshal(sample, writer);
    }

    @Test
    public void testJAXBMarshallingWithNamespacePrefixMapping() throws JAXBException, SAXException, IOException {
        XMLRootNamespaceContextMember111 sample = new XMLRootNamespaceContextMember111();
        Writer writer = new StringWriter();
        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        JAXB_CONFIG_UTIL.activateFormattedOutput(marshaller);
        JAXB_CONFIG_UTIL.activateNamespacePrefixMapping(marshaller);
        marshaller.marshal(sample, writer);
        LOGGER.debug("\n" + writer.toString());
    }

    private Schema getSchema() throws SAXException, IOException {
        SchemaFactory schemaFactory = JAXBConfigUtil.getSchemaFactory();
        return schemaFactory.newSchema(getSources());
    }

    private Source[] getSources() throws IOException {
        List<Source> result = new LinkedList<Source>();
        SchemaGenerator schemaGenerator = new SchemaGenerator(JAXB_CONTEXT, getNamespaceURISchemaFilenameMap());
        schemaGenerator.generateSchema();
        ClassLoader classloader = getClass().getClassLoader();
        for (File schemafile : schemaGenerator.getGeneratedSchemafiles()) {
            FileUtils.copyFile(schemafile, new File("./bin/" + schemafile.getName()));
            result.add(new StreamSource(classloader.getResourceAsStream(schemafile.getName())));
        }
        return result.toArray(new Source[] {});
    }

    /**
	 * The returned map can be used by {@link SchemaGenerator} to determine schema
	 * filenames for given namespaceURIs.
	 *
	 * @return
	 */
    private Map<String, String> getNamespaceURISchemaFilenameMap() {
        Map<String, String> result = new HashMap<String, String>();
        for (String namespaceURI : JAXB_CONFIG_UTIL.getNamespaceURIs()) {
            result.put(namespaceURI, JAXB_CONFIG_UTIL.getSchemaFilenameForNamespaceURI(namespaceURI));
        }
        return result;
    }
}
