package org.monet.reportservice.engine.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.monet.reportservice.configuration.Configuration;
import org.monet.reportservice.engine.AbstractReportGenerator;
import org.monet.reportservice.library.Zipper;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class PentahoReport extends AbstractReportGenerator {

    private String definitionURL;

    private HashMap<String, Object> parameters;

    private Configuration configuration;

    private Random rand;

    private boolean cubeNeedChange = true;

    private String tmpCubePath;

    private Logger log;

    public PentahoReport(String definitionURL, HashMap<String, Object> parametersWithValues, Configuration configuration) throws Exception {
        log = Logger.getRootLogger();
        log.info("PentahoReport() " + definitionURL);
        try {
            this.configuration = configuration;
        } catch (Exception e) {
            e.printStackTrace();
        }
        rand = new Random();
        String tmpDirDecompress = this.configuration.getTmpDir() + File.separator + rand.nextInt();
        File tmp = new File(tmpDirDecompress);
        if (tmp.exists()) {
            this.deleteDir(new File(tmpDirDecompress));
        }
        tmp.mkdir();
        Boolean isDecompress = Zipper.decompress(definitionURL, tmpDirDecompress);
        ArrayList<String> list = new ArrayList<String>();
        if (isDecompress) {
            this.GetFiledata(tmpDirDecompress);
            this.listFiles(tmpDirDecompress, tmpDirDecompress.length(), list);
            this.definitionURL = this.configuration.getTmpDir() + File.separator + "template_" + rand.nextInt() + ".prpt";
            Boolean compress = Zipper.compress(tmpDirDecompress, list, this.definitionURL);
            if (!compress) {
                this.deleteDir(new File(tmpDirDecompress));
                log.error("Compress template error: " + tmpDirDecompress);
                throw new Exception("Compress template error");
            }
        } else {
            tmp.delete();
            log.error("Decompress template error: " + "Difinition URL=" + definitionURL + " tmp dir=" + tmpDirDecompress);
            throw new Exception("Decompress template error");
        }
        if (this.definitionURL.charAt(0) == '/') this.definitionURL = "file://" + this.definitionURL; else this.definitionURL = "file:/" + this.definitionURL;
        this.parameters = parametersWithValues;
        this.deleteDir(new File(tmpDirDecompress));
    }

    @Override
    public MasterReport getReportDefinition() {
        log.info("getReportDefinition()");
        try {
            URL reportDefinitionURL = new URL(this.definitionURL);
            ResourceManager resourceManager = new ResourceManager();
            resourceManager.registerDefaults();
            Resource directly = resourceManager.createDirectly(reportDefinitionURL, MasterReport.class);
            return (MasterReport) directly.getResource();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DataFactory getDataFactory() {
        return null;
    }

    @Override
    public Map<String, Object> getReportParameters() {
        return this.parameters;
    }

    public void dispose() {
        log.info("dispose()");
        String tmpPrPt = this.definitionURL.substring(5);
        File tmpPRPT = new File(tmpPrPt);
        if (tmpPRPT.exists()) tmpPRPT.delete();
        File tmpCubeDel = new File(this.tmpCubePath);
        if (tmpCubeDel.exists()) tmpCubeDel.delete();
    }

    private void GetFiledata(String dirIn) {
        log.info("GetFiledata() Dir:" + dirIn);
        File dir = new File(dirIn);
        File[] files = dir.listFiles();
        FileFilter fileFilter = new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        files = dir.listFiles(fileFilter);
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals("datasources")) {
                remplaceDBConfig(files[i].getAbsolutePath());
            } else this.GetFiledata(files[i].getAbsolutePath());
        }
    }

    private void remplaceDBConfig(String absolutePath) {
        log.info("remplaceDBConfig() AbsolutePath:" + absolutePath);
        absolutePath += File.separator + "mondrian-ds.xml";
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(absolutePath));
            NodeList nodes = doc.getElementsByTagName("data:connection");
            if (nodes != null) {
                ((Element) nodes.item(0)).setAttribute("jdbc-password", this.configuration.getString(Configuration.JDBC_PASSWORD));
                ((Element) nodes.item(0)).setAttribute("jdbc-user", this.configuration.getString(Configuration.JDBC_USERNAME));
            }
            nodes = doc.getElementsByTagName("data:driver");
            if (nodes != null) {
                System.out.println();
                for (int i = 0; i < nodes.getLength(); i++) {
                    if (i == 1) {
                        if (this.configuration.getString(Configuration.DATABASE_TYPE).equals("MYSQL")) {
                            nodes.item(i).setTextContent("com.mysql.jdbc.Driver");
                        } else {
                            nodes.item(i).setTextContent("oracle.jdbc.driver.OracleDriver");
                        }
                    }
                }
            }
            nodes = doc.getElementsByTagName("data:url");
            if (nodes != null) {
                nodes.item(0).setTextContent(this.configuration.getString(Configuration.JDBC_URL));
            }
            nodes = doc.getElementsByTagName("data:property");
            if (nodes != null) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    String value = ((Element) nodes.item(i)).getAttribute("name");
                    if (value.equals("::pentaho-reporting::database-name")) nodes.item(i).setTextContent(this.configuration.getString(Configuration.DATABASE_NAME)); else if (value.equals("::pentaho-reporting::database-type")) nodes.item(i).setTextContent(this.configuration.getString(Configuration.DATABASE_TYPE)); else if (value.equals("user")) nodes.item(i).setTextContent(this.configuration.getString(Configuration.JDBC_USERNAME)); else if (value.equals("password")) nodes.item(i).setTextContent(this.configuration.getString(Configuration.JDBC_PASSWORD));
                }
            }
            nodes = doc.getElementsByTagName("data:cube-filename");
            if (nodes != null) {
                String pathCube = nodes.item(0).getTextContent();
                String fileCube = pathCube.substring(0, pathCube.indexOf(".")) + ".dist.xml";
                if (cubeNeedChange) {
                    this.changeCubeDist(fileCube);
                    cubeNeedChange = false;
                }
                nodes.item(0).setTextContent(tmpCubePath);
            }
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(doc), new StreamResult(new File(absolutePath)));
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void changeCubeDist(String fileCube) {
        log.info("changeCubeDist() cubePath:" + fileCube);
        try {
            tmpCubePath = this.configuration.getTmpDir() + File.separator + rand.nextInt() + fileCube;
            this.copy(this.configuration.getCubeDir() + File.separator + fileCube, tmpCubePath);
            if (this.configuration.getString(Configuration.DATABASE_TYPE).equals("GENERIC")) {
                this.replaceTextInFile(tmpCubePath, "#databasename#", this.configuration.getString(Configuration.DATABASE_NAME).toUpperCase());
                this.upperCaseTablesNames();
            } else this.replaceTextInFile(tmpCubePath, "#databasename#", this.configuration.getString(Configuration.DATABASE_NAME));
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void upperCaseTablesNames() {
        log.info("upperCaseTablesNames()");
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(tmpCubePath));
            NodeList nodes = doc.getElementsByTagName("Table");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = ((Element) nodes.item(i));
                e.setAttribute("name", e.getAttribute("name").toUpperCase());
            }
            nodes = doc.getElementsByTagName("Level");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = ((Element) nodes.item(i));
                e.setAttribute("column", e.getAttribute("column").toUpperCase());
                if (!e.getAttribute("captionColumn").equals("")) e.setAttribute("captionColumn", e.getAttribute("captionColumn").toUpperCase());
            }
            nodes = doc.getElementsByTagName("Measure");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = ((Element) nodes.item(i));
                e.setAttribute("column", e.getAttribute("column").toUpperCase());
            }
            nodes = doc.getElementsByTagName("Hierarchy");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = ((Element) nodes.item(i));
                e.setAttribute("primaryKey", e.getAttribute("primaryKey").toUpperCase());
            }
            nodes = doc.getElementsByTagName("Dimension");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = ((Element) nodes.item(i));
                if (!e.getAttribute("foreignKey").equals("")) e.setAttribute("foreignKey", e.getAttribute("foreignKey").toUpperCase());
            }
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(new DOMSource(doc), new StreamResult(new File(tmpCubePath)));
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void listFiles(String dirIn, int ids, List<String> outFiles) {
        log.info("listFiles() Dir:" + dirIn + " length:" + ids);
        File dir = new File(dirIn);
        File[] files = dir.listFiles();
        files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                String tmpFilename = dirIn.substring(ids) + File.separator + files[i].getName();
                outFiles.add(tmpFilename.substring(1));
            } else this.listFiles(files[i].getAbsolutePath(), ids, outFiles);
        }
    }

    private boolean deleteDir(File dir) {
        log.info("deleteDir() Dir:" + dir);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public void replaceTextInFile(String fileName, String fromText, String toText) {
        log.info("replaceTextInFile() file:" + fileName + " formText:" + fromText + " toText:" + toText);
        try {
            File file = new File(fileName);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "", oldtext = "";
            while ((line = reader.readLine()) != null) {
                oldtext += line + "\r\n";
            }
            reader.close();
            String newtext = oldtext.replaceAll(fromText, toText);
            FileWriter writer = new FileWriter(fileName);
            writer.write(newtext);
            writer.close();
        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void copy(String fromFileName, String toFileName) throws IOException {
        log.info("copy() file:" + fromFileName + " toFile:" + toFileName);
        File fromFile = new File(fromFileName);
        File toFile = new File(toFileName);
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}
