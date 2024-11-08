package com.infovide.qac.wunit;

import java.util.ArrayList;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.channels.Channels;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * @author Jan Topi�ski
 */
public class PrepereTest {

    static char PATHSEP = System.getProperty("file.separator").charAt(0);

    private WUnitConf wconf = new WUnitConf();

    private String actionOnInputXmlExistence;

    public static void main(String[] args) throws FileNotFoundException, DocumentException {
        String packageName;
        String serviceName;
        ArrayList xmlsFilesList = new ArrayList();
        if (args.length >= 2) {
            packageName = args[0];
            serviceName = args[1];
            if (args.length >= 3) {
                for (int i = 2; i < args.length; i++) {
                    int indexSep = args[i].lastIndexOf(PATHSEP);
                    if (indexSep == -1) indexSep = 0;
                    int indexDot = args[i].indexOf(".");
                    if (indexDot == -1) indexDot = 0;
                    String fileName = args[i].substring(indexSep + 1, indexDot);
                    String fileNameUpper = fileName.substring(0, 1).toUpperCase().concat(fileName.substring(1, fileName.length()));
                    TestPair tp = new TestPair(args[i], "test" + fileNameUpper);
                    xmlsFilesList.add(tp);
                }
            }
            try {
                new PrepereTest().prepareTests(packageName, serviceName, xmlsFilesList, "fail");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        } else {
            System.out.println("Prepare test run with arguments: packageName serviceName [pathsToXmlFiles]");
        }
    }

    /**
	 * 
	 */
    public void prepareTests(String packageName, String serviceName, ArrayList testPairs, String actionOnInputXmlExistence) throws FileNotFoundException, IncorrectConfigurationValueException, InputXMLAlreadyExistsException, DuplicatedTestMethodException, InGeneretingException {
        validateConfigurationParameters();
        this.actionOnInputXmlExistence = actionOnInputXmlExistence;
        ArrayList testPairForUnpreparedMethods = new ArrayList();
        String serviceNameUpper = serviceName.substring(0, 1).toUpperCase().concat(serviceName.substring(1, serviceName.length()));
        String filePath = wconf.outputDir.trim();
        if (!(PATHSEP == filePath.charAt(filePath.length() - 1))) filePath += PATHSEP;
        filePath += packageName.replace('.', PATHSEP) + PATHSEP + serviceNameUpper + ".java";
        File javaFile = new File(filePath);
        if (!javaFile.exists()) {
            prepereClass(filePath, packageName, serviceNameUpper);
        }
        testPairForUnpreparedMethods = (ArrayList) testPairs.clone();
        if (testPairs != null) {
            for (int i = 0; i < testPairs.size(); i++) {
                TestPair tp = (TestPair) testPairs.get(i);
                if (i != 0) {
                    this.actionOnInputXmlExistence = "fail";
                }
                try {
                    prepereMethod(javaFile, packageName, serviceName, tp);
                    testPairForUnpreparedMethods.remove(tp);
                } catch (DocumentException de) {
                    InGeneretingException ing = new InGeneretingException(de.getMessage());
                    ing.testPairForUnpreparedMethods = testPairForUnpreparedMethods;
                    throw ing;
                } catch (DuplicatedTestMethodException dtme) {
                    dtme.testPairForUnpreparedMethods = testPairForUnpreparedMethods;
                    throw dtme;
                } catch (InputXMLAlreadyExistsException ixe) {
                    ixe.testPairForUnpreparedMethods = testPairForUnpreparedMethods;
                    throw ixe;
                } catch (InGeneretingException ixe) {
                    ixe.testPairForUnpreparedMethods = testPairForUnpreparedMethods;
                    throw ixe;
                }
            }
        }
    }

    public void prepereClass(String filePath, String packageName, String serviceName) {
        File dir = new File(filePath.replaceAll(serviceName + ".java", ""));
        dir.mkdirs();
        File javaFile = new File(filePath);
        try {
            javaFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        OutputStreamWriter os;
        try {
            os = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(javaFile)));
            os.write("package " + packageName + ";\n");
            os.write("\n");
            os.write("import org.dom4j.io.DocumentSource;\n");
            os.write("import org.dom4j.Document;\n");
            os.write("import org.dom4j.io.SAXReader;\n");
            os.write("import java.text.ParseException;\n");
            os.write("import java.io.File;\n");
            os.write("import java.io.InputStream;\n");
            os.write("import com.wm.app.b2b.client.ServiceException;\n");
            os.write("import com.infovide.qac.wunit.*;\n");
            os.write("\n");
            os.write("public class " + serviceName + " extends WTestCase\n{\n\n");
            os.write("}");
            os.close();
        } catch (FileNotFoundException e2) {
            RuntimeException rte = new RuntimeException("XSL transformation failed!");
            rte.initCause(e2);
            throw rte;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void prepereMethod(File javaFile, String packageName, String serviceName, TestPair tp) throws DocumentException, InputXMLAlreadyExistsException, DuplicatedTestMethodException, InGeneretingException {
        File xmlFile = new File(tp.getXmlFilePath());
        xmlFile.deleteOnExit();
        DocumentSource xmlDoc = null;
        TransformerFactory factory = TransformerFactory.newInstance();
        DocumentSource src = null;
        Transformer transformer = null;
        RandomAccessFile raf = null;
        RuntimeException rte = null;
        try {
            xmlDoc = new DocumentSource(new SAXReader().read(xmlFile));
            InputStream xslFile = getClass().getClassLoader().getResourceAsStream("xslt/tomethod.xsl");
            src = new DocumentSource(new SAXReader().read(xslFile));
            transformer = factory.newTransformer(src);
            String name = tp.getMethodName();
            if (name == null || "".equals(name)) {
                throw new InGeneretingException("Name of method can not be null or empty string.");
            }
            if (isMethodNameUsed(javaFile, name)) {
                throw new DuplicatedTestMethodException(packageName, serviceName, name, "Method " + packageName + ":" + serviceName + "." + name + " is already used");
            }
            raf = new RandomAccessFile(javaFile, "rw");
            raf.seek(raf.length() - 1);
            while (raf.read() != '}') {
                raf.seek(raf.getFilePointer() - 2);
            }
            raf.seek(raf.getFilePointer() - 2);
            Writer wr = Channels.newWriter(raf.getChannel(), "UTF-8");
            wr.write("\n\n\t/**\n\t * Test .");
            wr.write("\n\t * ");
            wr.write("\n\t * @throws Throwable");
            wr.write("\n\t */");
            wr.write("\n\tpublic void " + name + "()\n\t\t\tthrows Throwable \n");
            wr.write("\t{\n");
            File inputDocFile = new File("tempInDoc.xml");
            inputDocFile.deleteOnExit();
            StreamResult result = new StreamResult(inputDocFile);
            transformer.transform(xmlDoc, result);
            writeInputXml(wr, inputDocFile, javaFile.getAbsolutePath(), xmlFile.getName(), packageName);
            writeTryCatchPart(wr, packageName, serviceName);
            wr.write("\n\t}\n");
            wr.write("}\n");
            wr.flush();
        } catch (TransformerException e1) {
            rte = new RuntimeException("XSL transformation failed!");
            rte.initCause(e1);
        } catch (FileNotFoundException e) {
            rte = new RuntimeException("Java file not found");
            rte.initCause(e);
        } catch (MalformedURLException e) {
            rte = new RuntimeException("Wrong xsl document location");
            rte.initCause(e);
        } catch (IOException e) {
            rte = new RuntimeException("IO problem!");
            rte.initCause(e);
        } finally {
            try {
                if (raf != null) {
                    raf.close();
                }
                if (rte != null) {
                    throw rte;
                }
            } catch (IOException e2) {
                rte = new RuntimeException("NO xsl document!");
                rte.initCause(e2);
                throw rte;
            }
        }
    }

    public void makeInputXml(Writer writer, Document xmlDocument) throws IOException {
        Element root = xmlDocument.getRootElement();
        String rootElementName = "IDataXMLCoder";
        String rootElementAttribute = "version";
        String rootElementAttributeValue = "\"1.0\"";
        writer.write("\n");
        writer.write("\t\tDocumentFactory factory = new DocumentFactory();\n");
        writer.write("\t\tfactory.createDocument(");
        writer.write("");
        writer.write(");\n");
    }

    public void writeInputXml(Writer writer, File inputDocFile, String pathForXml, String xmlFileName, String packageName) throws IOException, InputXMLAlreadyExistsException {
        String xmlInSeparatedFile = wconf.xmlInSeparatedFileElementValue;
        try {
            DocumentSource xmlInputDoc = new DocumentSource(new SAXReader().read(inputDocFile));
            Document xmlDocument = xmlInputDoc.getDocument();
            if ("yes".equals(xmlInSeparatedFile)) {
                writeSeparatedInputXml(pathForXml, xmlFileName, xmlDocument, writer, packageName);
            } else if ("no".equals(xmlInSeparatedFile)) {
                writeInsideInputXml(xmlDocument, writer);
            }
        } catch (DocumentException e) {
            RuntimeException rte = new RuntimeException("NO xml document!");
            rte.initCause(e);
        }
    }

    private void writeSeparatedInputXml(String pathForXml, String xmlFileName, Document xmlDocument, Writer writer, String packageName) throws InputXMLAlreadyExistsException, IOException {
        pathForXml = pathForXml.substring(0, pathForXml.lastIndexOf(PATHSEP));
        if (xmlFileName.indexOf(".xml") == -1) xmlFileName += ".xml";
        File newXmlFile = new File(pathForXml + PATHSEP + xmlFileName);
        if (newXmlFile.exists() == true) {
            if (actionOnInputXmlExistence.equals("fail")) {
                throw new InputXMLAlreadyExistsException("File " + newXmlFile.getAbsolutePath() + " does not exist.", newXmlFile.getAbsolutePath());
            } else if (actionOnInputXmlExistence.equals("override")) {
                createXMLFile(newXmlFile, xmlDocument);
            } else if (actionOnInputXmlExistence.equals("noOverride")) {
            }
        } else {
            createXMLFile(newXmlFile, xmlDocument);
        }
        writer.write("\t\tInputStream is =  ClassLoader.getSystemResourceAsStream(\"" + packageName.replace('.', '/') + "/" + xmlFileName + "\");");
        writer.write("\n\t\tDocument inDoc = makeDocument(is);\n");
    }

    private void writeInsideInputXml(Document xmlDocument, Writer writer) throws IOException {
        OutputFormat of = OutputFormat.createPrettyPrint();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLWriter xw = null;
        xw = new XMLWriter(baos, of);
        xw.write(xmlDocument);
        String xmlStr = baos.toString("UTF-8");
        xmlStr = xmlStr.replaceAll("\"", "\\\\\"");
        xmlStr = xmlStr.replaceAll("\n", "\" + \n\t\t\t\t\"");
        xmlStr += "\";";
        writer.write("\t\tString xml = \"");
        writer.write(xmlStr);
        writer.write("\n\n\t\t");
        writer.write("Document  inDoc = makeData(xml);");
        writer.write("\n\t\t");
    }

    private void createXMLFile(File newXmlFile, Document xmlDocument) throws IOException {
        newXmlFile.createNewFile();
        OutputFormat of = OutputFormat.createPrettyPrint();
        XMLWriter xw = null;
        try {
            BufferedOutputStream oss;
            oss = new BufferedOutputStream(new FileOutputStream(newXmlFile));
            xw = new XMLWriter(oss, of);
            xw.write(xmlDocument);
        } catch (IOException e) {
            RuntimeException rte = new RuntimeException("Writing xml file failed!");
            rte.initCause(e);
            throw rte;
        }
    }

    public void writeTryCatchPart(Writer writer, String packageName, String serviceName) throws IOException {
        OutputFormat of = OutputFormat.createPrettyPrint();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write("\n\t\t");
        writer.write("Document outDoc = null;");
        writer.write("\n\t\t");
        writer.write("try{");
        writer.write("\n\t\t\t");
        writer.write("outDoc = invokeService(\"" + packageName + "\",\"" + serviceName + "\",inDoc);");
        writer.write("\n\t\t\t");
        writer.write(wconf.defaultAssertions);
        writer.write("\n\n\t\t");
        writer.write("}");
        writer.write("catch(Throwable t){");
        writer.write("\n\t\t\t");
        writer.write("if(inDoc!=null){");
        writer.write("\n\t\t\t\t");
        writer.write("print(inDoc);");
        writer.write("\n\t\t\t\t");
        writer.write("makeDevDbgFile(inDoc);");
        writer.write("\n\t\t\t}");
        writer.write("if(outDoc!=null){");
        writer.write("\n\t\t\t\t");
        writer.write("print(outDoc);");
        writer.write("\n\t\t\t}");
        writer.write("\n\t\t\t");
        writer.write("throw t;");
        writer.write("\n\t\t");
        writer.write("}");
        writer.write("\n\t\t\t");
    }

    protected boolean isMethodNameUsed(File javaFile, String methodName) throws FileNotFoundException, IOException {
        BufferedReader br;
        String szLine;
        try {
            br = new BufferedReader(new FileReader(javaFile));
        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        }
        br.mark(0);
        br.reset();
        while ((szLine = br.readLine()) != null) {
            br.mark(0);
            br.reset();
            szLine = szLine.trim();
            szLine = szLine.replaceAll("\\s{2,}", " ");
            if (szLine.startsWith("public void " + methodName + "(")) {
                return true;
            }
        }
        br.close();
        return false;
    }

    /**
	 * 
	 *
	 */
    private void validateConfigurationParameters() throws IncorrectConfigurationValueException {
        if (!wconf.xmlInSeparatedFilePossibleValues.contains(wconf.xmlInSeparatedFileElementValue)) throw new IncorrectConfigurationValueException("Configuration param " + this.wconf.xmlInSeparatedFileElementName + " is set to incorrect value: " + this.wconf.xmlInSeparatedFileElementValue + ". Possible values are: " + this.wconf.xmlInSeparatedFilePossibleValues.toString(), this.wconf.xmlInSeparatedFileElementName, this.wconf.xmlInSeparatedFilePossibleValues, this.wconf.xmlInSeparatedFileElementValue);
        if (!wconf.devDbgPossibleValues.contains(wconf.devDbgElementValue)) throw new IncorrectConfigurationValueException("Configuration param " + this.wconf.xmlInSeparatedFileElementName + " is set to incorrect value: " + this.wconf.xmlInSeparatedFileElementValue + ". Possible values are: " + this.wconf.xmlInSeparatedFilePossibleValues.toString(), this.wconf.devDbgElementName, this.wconf.devDbgPossibleValues, this.wconf.devDbgElementValue);
    }
}
