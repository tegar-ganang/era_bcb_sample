package com.explosion.expfmodules.javahelp.writers;

import java.io.File;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.explosion.expfmodules.javahelp.JavaHelpConstants;
import com.explosion.expfmodules.javahelp.JavaHelpModuleManager;
import com.explosion.expfmodules.javahelp.project.ProjectStore;
import com.explosion.utilities.FileSystemUtils;
import com.explosion.utilities.exception.ExceptionManagerFactory;
import com.explosion.utilities.preferences.Preference;
import com.explosion.utilities.process.threads.ProcessThread;

/**
 * @author Stephen Cowx ,Explosion Software
 * Date created:@31-Jan-2003
 */
public class TOCFileWriter {

    private static final String VERSION = "version";

    private static final String TOC = "toc";

    private static final String TOCITEM = "tocitem";

    private static final String TEXT = "text";

    private static final String TARGET = "target";

    private static final String IMAGE = "image";

    private Document document = null;

    private File outputFile = null;

    private File baseDir = null;

    private ProjectStore project = null;

    private static String getValidSeparator() {
        if (System.getProperty("file.separator").equals("\\")) return "\\\\"; else return System.getProperty("file.separator");
    }

    public TOCFileWriter(File outputDir, File baseDir, String tocFileName, ProjectStore project) {
        this.outputFile = FileSystemUtils.checkGivenPathValid(outputDir.getAbsolutePath() + System.getProperty("file.separator") + tocFileName);
        this.baseDir = baseDir;
        this.project = project;
    }

    public void build(ProcessThread processThread) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
            Element toc = document.createElement(TOC);
            toc.setAttribute(VERSION, "1.0");
            document.appendChild(toc);
            populate(toc);
        } catch (Exception e) {
            ExceptionManagerFactory.getExceptionManager().manageException(e, null);
        }
    }

    /**
 * Creates a JTree from the project file
 */
    public Node populate(Node tocNode) throws Exception {
        Node node = project.getChildNamed(project.getRoot(), JavaHelpConstants.NODE_NAME_TOC_DEFINITION);
        if (node == null) {
            return null;
        } else {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childnode = children.item(i);
                if (childnode.getNodeName().equals(JavaHelpConstants.NODE_NAME_TOC_ITEM)) analyse(childnode, tocNode);
            }
        }
        return node;
    }

    private void analyse(Node projectSourceNode, Node tocFileDestinationNode) throws Exception {
        Node item = document.createElement(JavaHelpConstants.NODE_NAME_TOC_ITEM);
        tocFileDestinationNode.appendChild(item);
        Vector v = project.getTOCItemPreferences(projectSourceNode);
        Preference text = (Preference) v.elementAt(0);
        ((Element) item).setAttribute(TEXT, (String) text.getValue());
        Preference target = (Preference) v.elementAt(1);
        if (((File) target.getValue()).exists()) {
            String id = JavaHelpModuleManager.getFileID(baseDir, (File) target.getValue());
            ((Element) item).setAttribute(TARGET, id);
        }
        Preference icon = (Preference) v.elementAt(2);
        if (((File) icon.getValue()).exists()) {
            String id = JavaHelpModuleManager.getFileID(baseDir, (File) icon.getValue());
            ((Element) item).setAttribute(IMAGE, id);
        }
        NodeList children = projectSourceNode.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node childnode = children.item(i);
            if (childnode.getNodeName().equals(JavaHelpConstants.NODE_NAME_TOC_ITEM)) analyse(childnode, item);
        }
    }

    public void write(ProcessThread processThread) {
        build(processThread);
        try {
            System.out.println(document.toString());
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(outputFile);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer domTransformer = factory.newTransformer();
            domTransformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            ExceptionManagerFactory.getExceptionManager().manageException(e, null);
        } catch (TransformerFactoryConfigurationError e) {
            ExceptionManagerFactory.getExceptionManager().manageException(e, null);
        } catch (TransformerException e) {
            ExceptionManagerFactory.getExceptionManager().manageException(e, null);
        }
    }
}
