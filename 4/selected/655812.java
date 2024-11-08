package org.ximtec.igesture.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.sigtec.jdom.JdomDocument;
import org.sigtec.jdom.id.Factory;
import org.sigtec.util.Constant;
import org.sigtec.util.FileHandler;
import org.ximtec.igesture.batch.BatchProcessContainer;
import org.ximtec.igesture.batch.BatchResultSet;
import org.ximtec.igesture.batch.core.jdom.JdomBatchAlgorithm;
import org.ximtec.igesture.batch.jdom.JdomBatchResultSet;
import org.ximtec.igesture.configuration.Configuration;
import org.ximtec.igesture.configuration.jdom.JdomConfiguration;
import org.ximtec.igesture.core.GestureClass;
import org.ximtec.igesture.core.GestureSet;
import org.ximtec.igesture.core.TestSet;
import org.ximtec.igesture.core.jdom.JdomGestureClass;
import org.ximtec.igesture.core.jdom.JdomGestureSet;
import org.ximtec.igesture.core.jdom.JdomTestSet;
import org.xml.sax.InputSource;

/**
 * Provides static methods with XML import/export functionality.
 * 
 * @version 1.0, Dec 2006
 * @author Ueli Kurmann, igesture@uelikurmann.ch
 * @author Beat Signer, signer@inf.ethz.ch
 */
public class XMLTool {

    private static final Logger LOGGER = Logger.getLogger(XMLTool.class.getName());

    public static final String ROOT_TAG = "sets";

    public static final String TEST_SETS_TAG = "testSets";

    public static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    /**
    * Imports a gesture set.
    * 
    * @param file the XML file.
    * @return a list of gesture sets.
    */
    public static GestureSet importGestureSet(InputStream inputStream) {
        Document document = importDocument(inputStream);
        Element setElement = document.getRootElement().getChild(JdomGestureSet.ROOT_TAG);
        return (setElement != null) ? JdomGestureSet.unmarshal(setElement) : null;
    }

    public static GestureSet importGestureSet(File file) {
        try {
            return importGestureSet(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, Constant.EMPTY_STRING, e);
            return null;
        }
    }

    /**
    * Exports a gesture set.
    * 
    * @param set the gesture set to be exported.
    * @param file the XML file.
    */
    public static void exportGestureSet(GestureSet set, File file) {
        final List<GestureSet> list = new ArrayList<GestureSet>();
        list.add(set);
        exportGestureSet(list, file);
    }

    /**
    * Exports a Gesture Set as Input Stream
    * @param set
    * @return
    */
    public static InputStream exportGestureSetAsStream(GestureSet set) {
        List<GestureSet> list = new ArrayList<GestureSet>();
        list.add(set);
        return exportGestureSetsAsStream(list);
    }

    /**
    * Exports a list of Gesture Sets as InputStream
    * @param sets
    * @return
    */
    public static InputStream exportGestureSetsAsStream(List<GestureSet> sets) {
        JdomDocument igestureDocument = new JdomDocument(ROOT_TAG);
        Set<GestureClass> hashSet = new HashSet<GestureClass>();
        for (final GestureSet set : sets) {
            for (final GestureClass gestureClass : set.getGestureClasses()) {
                hashSet.add(gestureClass);
            }
        }
        for (final GestureClass gestureClass : hashSet) {
            igestureDocument.attach(new JdomGestureClass(gestureClass));
        }
        for (final GestureSet set : sets) {
            igestureDocument.attach(new JdomGestureSet(set));
        }
        return IOUtils.toInputStream(igestureDocument.toXml());
    }

    /**
    * Exports a list of gesture sets.
    * 
    * @param sets the gesture sets to be exported.
    * @param file the XML file.
    */
    public static void exportGestureSet(List<GestureSet> sets, File file) {
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(exportGestureSetsAsStream(sets), outputStream);
            outputStream.close();
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Could not export Gesture Sets. Export File not found.", e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not export Gesture Sets.", e);
        }
    }

    /**
    * Exports a test set.
    * 
    * @param testSet the test set to be exported.
    * @param file the XML file.
    */
    public static void exportTestSet(TestSet testSet, File file) {
        final List<TestSet> list = new ArrayList<TestSet>();
        list.add(testSet);
        exportTestSet(list, file);
    }

    /**
    * Exports a list of test sets.
    * 
    * @param testSetList the list of test sets to be exported.
    * @param file the XML file.
    */
    public static void exportTestSet(List<TestSet> testSetList, File file) {
        final JdomDocument igestureDocument = new JdomDocument(ROOT_TAG);
        for (final TestSet set : testSetList) {
            igestureDocument.attach(new JdomTestSet(set));
        }
        FileHandler.writeFile(file.getPath(), igestureDocument.toXml());
    }

    /**
    * Imports a list of test sets.
    * 
    * @param file the XML File
    * @return a list of GestureSets
    */
    public static TestSet importTestSet(File file) {
        try {
            return importTestSet(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, file.getName() + " not found. Could not import Testsets.", e);
        }
        return null;
    }

    /**
    * Imports a list of test sets from an XML document.
    * @param inputStream the input stream of the XML document
    * @return a list of test sets
    */
    public static TestSet importTestSet(InputStream inputStream) {
        Document document = importDocument(inputStream);
        Element testSetElement = document.getRootElement().getChild(JdomTestSet.ROOT_TAG);
        return (testSetElement != null) ? JdomTestSet.unmarshal(testSetElement) : null;
    }

    /**
    * Imports a configuration.
    * 
    * @param file the XML file
    * @return the configuration.
    */
    public static Configuration importConfiguration(InputStream inputStream) {
        Configuration configuration = new Configuration();
        final Document document = importDocument(inputStream);
        configuration = (Configuration) JdomConfiguration.unmarshal(document.getRootElement());
        return configuration;
    }

    /**
    * Imports a configuration
    * @param file
    * @return
    */
    public static Configuration importConfiguration(File file) {
        try {
            return importConfiguration(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOGGER.severe("Configuration File not found.");
        }
        return null;
    }

    /**
    * Exports a configuration.
    * 
    * @param configuration the configuration to be exported.
    * @param file the XML file.
    */
    public static void exportConfiguration(Configuration configuration, File file) {
        JdomDocument document = new JdomDocument(new JdomConfiguration(configuration));
        FileHandler.writeFile(file.getPath(), document.toXml());
    }

    /**
    * Exports a Configuration as XML Stream
    * @param configuration
    * @return a Configuration as XML InputStream
    */
    public static InputStream exportConfigurationAsStream(Configuration configuration) {
        JdomDocument document = new JdomDocument(new JdomConfiguration(configuration));
        return IOUtils.toInputStream(document.toXml());
    }

    /**
    * Exports a Test Set as XML Stream
    * @param testSet
    * @return a TestSet as XML InputStreamd
    */
    public static InputStream exportTestSetAsStream(TestSet testSet) {
        JdomDocument document = new JdomDocument(TEST_SETS_TAG);
        document.attach(new JdomTestSet(testSet));
        return IOUtils.toInputStream(document.toXml());
    }

    /**
    * Exports a batch result set.
    * 
    * @param resultSet the batch result set to be exported
    * @param file the XML file
    */
    public static void exportBatchResultSet(BatchResultSet resultSet, File file) {
        FileHandler.writeFile(file.getPath(), XMLTool.exportBatchResultSet(resultSet));
    }

    /**
    * Exports a batch result set to its XML representation.
    * 
    * @param resultSet the batch result set to be exported.
    * @return the string containing the XML representation.
    */
    public static String exportBatchResultSet(BatchResultSet resultSet) {
        final JdomDocument document = new JdomDocument(new JdomBatchResultSet(resultSet));
        return document.toXml();
    }

    /**
    * Imports an XML document.
    * 
    * @param file the XML file.
    * @return the JDOM document.
    */
    public static Document importDocument(InputStream inputStream) {
        Document document = null;
        try {
            final SAXBuilder builder = new SAXBuilder(false);
            builder.setFactory(new Factory());
            builder.setIgnoringElementContentWhitespace(true);
            final InputSource is = new InputSource(inputStream);
            document = builder.build(is);
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, Constant.EMPTY_STRING, e);
        } catch (final JDOMException e) {
            LOGGER.log(Level.SEVERE, Constant.EMPTY_STRING, e);
        }
        return document;
    }

    /**
    * Imports an XML Document from a file.
    * @param file
    * @return
    */
    public static Document importDocument(File file) {
        try {
            return importDocument(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, Constant.EMPTY_STRING, e);
            return null;
        }
    }

    /**
    * Imports an XML File and builds up the object tree. The result is a list of
    * batch algorithm instances.
    * 
    * @param file the XML file to be imported.
    * @return list of batch algorithm instances.
    */
    @SuppressWarnings("unchecked")
    public static BatchProcessContainer importBatchProcessContainer(File file) {
        final BatchProcessContainer container = new BatchProcessContainer();
        final Document document = importDocument(file);
        for (final Element elem : (List<Element>) document.getRootElement().getChildren(JdomBatchAlgorithm.ROOT_TAG)) {
            container.addAlgorithm(JdomBatchAlgorithm.unmarshal(elem));
        }
        for (final Element set : (List<Element>) document.getRootElement().getChildren(JdomGestureSet.ROOT_TAG)) {
            container.addGestureSet(JdomGestureSet.unmarshal(set));
        }
        return container;
    }

    /**
    * Transforms an XML Document using XSLT
    * @param xmlDocument the XML Document
    * @param xslFile the XSL Document
    * @return a String containing the transformed document.
    * @throws TransformerException
    * @throws TransformerConfigurationException
    * @throws TransformerFactoryConfigurationError
    */
    public static String transform(String xmlDocument, String xslFile) throws TransformerException, TransformerConfigurationException, TransformerFactoryConfigurationError {
        final StringReader xmlSR = new StringReader(xmlDocument);
        final StreamSource xmlIn = new StreamSource(xmlSR);
        final StringWriter xmlSW = new StringWriter();
        final StreamResult xmlOut = new StreamResult(xmlSW);
        FileReader xslSR = null;
        try {
            xslSR = new FileReader(new File(xslFile));
        } catch (final FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, Constant.EMPTY_STRING, e);
        }
        final TransformerFactory tFactory = TransformerFactory.newInstance();
        final Transformer transformer = tFactory.newTransformer(new StreamSource(xslSR));
        transformer.transform(xmlIn, xmlOut);
        return xmlSW.toString();
    }

    /**
    * XSL transformation.
    * @param xmlDocument XML document (String)
    * @param xsl XSL document (InputStream)
    * @return the transformed XML document
    * @throws TransformerException
    * @throws TransformerConfigurationException
    * @throws TransformerFactoryConfigurationError
    */
    public static String transform(String xmlDocument, InputStream xsl) throws TransformerException, TransformerConfigurationException, TransformerFactoryConfigurationError {
        final StringReader xmlSR = new StringReader(xmlDocument);
        final StreamSource xmlIn = new StreamSource(xmlSR);
        final StringWriter xmlSW = new StringWriter();
        final StreamResult xmlOut = new StreamResult(xmlSW);
        StreamSource xslSR = new StreamSource(xsl);
        final TransformerFactory tFactory = TransformerFactory.newInstance();
        final Transformer transformer = tFactory.newTransformer(xslSR);
        transformer.transform(xmlIn, xmlOut);
        return xmlSW.toString();
    }
}
