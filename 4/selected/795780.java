package org.achup.elgenerador.process;

import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.achup.elgenerador.process.generation.GenerationProcess;
import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.achup.elgenerador.datasource.DataSource;
import org.achup.elgenerador.metadata.Metadata;
import org.achup.elgenerador.project.Project;
import org.achup.elgenerador.xml.ElementParser;
import org.achup.elgenerador.xml.ElementWriter;
import org.achup.elgenerador.xml.ProjectElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 * @author Marco Bassaletti
 */
public class ProcessManager {

    public ProcessManager() {
    }

    public void readMetadata(File inputXmlFile, File outputXmlFile) {
        try {
            Project project = parseProject(inputXmlFile);
            readMetadata(project);
            writeProject(project, outputXmlFile);
        } catch (Exception ex) {
            throw new ProcessException(ex);
        }
    }

    public void readMetadata(Project project) {
        DataSource dataSource = project.getInputDataSource();
        if (dataSource == null) {
            throw new ProcessException("No input data source specified.");
        }
        dataSource.connect();
        Metadata metadata = dataSource.readMetadata();
        dataSource.close();
        project.setMetadata(metadata);
    }

    public void generateData(File xmlFile) {
        try {
            Project project = parseProject(xmlFile);
            generateData(project);
        } catch (Exception ex) {
            throw new ProcessException(ex);
        }
    }

    public void generateData(Project project) {
        GenerationProcess process = new GenerationProcess(project);
        process.generateData();
    }

    public void process(File xmlFile) {
        try {
            Project project = parseProject(xmlFile);
            if (project.getProcessInfo() == null) {
                throw new ProcessException("No process specified.");
            }
            if (project.getProcessInfo() instanceof GenerationProcessInfo) {
                generateData(project);
            } else if (project.getProcessInfo() instanceof ReadMetadataProcessInfo) {
                ReadMetadataProcessInfo readMetadataProcessInfo = (ReadMetadataProcessInfo) project.getProcessInfo();
                readMetadata(project);
                writeProject(project, readMetadataProcessInfo.getOutputXmlFile());
            }
        } catch (Exception ex) {
            throw new ProcessException(ex);
        }
    }

    private Project parseProject(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        if (!xmlFile.exists()) {
            throw new ProcessException("File not found.");
        }
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.parse(xmlFile);
        ElementParser<Project> parser = new ProjectElement();
        Project project = parser.parseXML(document.getDocumentElement());
        return project;
    }

    private void writeProject(Project project, File xmlFile) throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
        ElementWriter<Project> writer = new ProjectElement();
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = docBuilder.newDocument();
        Element element = writer.createXML(document, project);
        document.appendChild(element);
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(xmlFile);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.transform(source, result);
    }
}
