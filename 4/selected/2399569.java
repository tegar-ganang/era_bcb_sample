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
import com.explosion.expfmodules.javahelp.JavaHelpModuleManager;
import com.explosion.utilities.FileSystemUtils;
import com.explosion.utilities.exception.ExceptionManagerFactory;
import com.explosion.utilities.process.threads.ProcessThread;

public class MapFileWriter {

    private static final String VERSION = "version";

    private static final String MAP = "map";

    private static final String MAPID = "mapID";

    private static final String TARGET = "target";

    private static final String URL = "url";

    private Document document = null;

    private File outputFile = null;

    private File baseDir = null;

    private String mapFileName = null;

    public MapFileWriter(File outputDir, File baseDir, String mapFileName) {
        this.outputFile = FileSystemUtils.checkGivenPathValid(outputDir.getAbsolutePath() + System.getProperty("file.separator") + mapFileName);
        this.baseDir = baseDir;
    }

    public void buildMap(ProcessThread processThread) {
        try {
            Vector files = FileSystemUtils.getFileList(processThread, true, baseDir.getAbsolutePath(), "", false);
            System.out.println(files.size());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.newDocument();
            Element map = document.createElement(MAP);
            map.setAttribute(VERSION, "1.0");
            document.appendChild(map);
            for (int i = 0; i < files.size(); i++) {
                String id = JavaHelpModuleManager.getFileID(baseDir, (File) files.elementAt(i));
                String url = JavaHelpModuleManager.getFileUrl(baseDir, (File) files.elementAt(i));
                Element mapID = document.createElement(MAPID);
                mapID.setAttribute(TARGET, id);
                mapID.setAttribute(URL, "./" + url);
                map.appendChild(mapID);
            }
        } catch (Exception e) {
            ExceptionManagerFactory.getExceptionManager().manageException(e, null);
        }
    }

    public void write(ProcessThread processThread) {
        buildMap(processThread);
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
