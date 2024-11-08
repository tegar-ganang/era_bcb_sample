package com.htmli.compiler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Matias Bagini
 *
 */
public class LibraryBuilder {

    /**
	 *
	 */
    private String name;

    /**
	 *
	 */
    private File base;

    /**
	 *
	 */
    private Writer jsWriter;

    /**
	 *
	 */
    private Writer cssWriter;

    /**
	 *
	 */
    private Transformer jsTransformer;

    /**
	 *
	 */
    private Transformer cssTransformer;

    /**
	 *
	 */
    private Transformer xslTransformer;

    /**
	 *
	 */
    private Transformer docTransformer;

    /**
	 * Document where elements are added.
	 */
    private Document elements;

    /**
	 * @param base
	 * @param name
	 */
    public LibraryBuilder(File base, String name) {
        this.base = base;
        this.name = name;
    }

    /**
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws SAXException
	 */
    public synchronized void build() throws IOException, ParserConfigurationException, TransformerException, SAXException {
        ClassLoader loader = LibraryBuilder.class.getClassLoader();
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        elements = db.newDocument();
        elements.appendChild(elements.createElement("elements"));
        TransformerFactory tFactory = TransformerFactory.newInstance();
        xslTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/xsl.xsl")));
        cssTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/css.xsl")));
        jsTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/js.xsl")));
        docTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/doc.xsl")));
        File xslDir = new File(base, "xsl");
        File jspTagDir = new File(base, "jsp");
        File jsDir = new File(base, "js");
        File cssDir = new File(base, "css");
        File docDir = new File(base, "doc");
        File libraryDir = new File(new File(base, "src"), name);
        jsWriter = new StringWriter();
        cssWriter = new StringWriter();
        Writer js = null;
        Writer css = null;
        buildDirectory(libraryDir, docDir, xslDir, jspTagDir, cssDir, "");
        try {
            js = new BufferedWriter(new FileWriter(new File(jsDir, "lib-" + name + ".js")));
            js.write(jsWriter.toString());
            js.close();
            css = new BufferedWriter(new FileWriter(new File(cssDir, "lib-" + name + ".css")));
            css.write(cssWriter.toString());
            css.close();
        } finally {
            try {
                if (js != null) {
                    js.close();
                }
            } finally {
                if (css != null) {
                    css.close();
                }
            }
        }
        buildPackageXsl();
        addPackageXslToCore();
        buildPackageDocIndex();
        addPackageDocIndexToMain();
    }

    private void addPackageDocIndexToMain() throws ParserConfigurationException, TransformerException, IOException, SAXException {
        DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
        df.setNamespaceAware(true);
        DocumentBuilder db = df.newDocumentBuilder();
        File docDir = new File(base, "doc");
        File indexFile = new File(docDir, "packages.html");
        Document indexDoc = db.parse(indexFile);
        Node containerDiv = indexDoc.getElementsByTagName("div").item(0);
        NodeList libraries = containerDiv.getChildNodes();
        String libName = "lib-" + name + ".html";
        for (int i = 0; i < libraries.getLength(); i++) {
            if (libraries.item(i) instanceof Element) {
                if (libName.equals(((Element) libraries.item(i)).getAttribute("a"))) {
                    return;
                }
            }
        }
        Element newLib = indexDoc.createElement("a");
        newLib.setAttribute("href", libName);
        newLib.setAttribute("style", "display: block;");
        newLib.setAttribute("target", "classes");
        String libraryName = name.equals("std") ? "Standard Library" : name;
        newLib.appendChild(indexDoc.createTextNode(libraryName));
        containerDiv.appendChild(newLib);
        save(indexDoc, indexFile);
    }

    /**
	 * @throws IOException 
	 * @throws TransformerException 
	 *
	 */
    private void buildPackageDocIndex() throws TransformerException, IOException {
        ClassLoader loader = LibraryBuilder.class.getClassLoader();
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer docIndexTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/docindex.xsl")));
        Writer writer = null;
        try {
            File docDir = new File(base, "doc");
            writer = new BufferedWriter(new FileWriter(new File(docDir, "lib-" + name + ".html")));
            transform(elements, docIndexTransformer, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws TransformerException
	 */
    private void addPackageXslToCore() throws ParserConfigurationException, SAXException, IOException, TransformerException {
        DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
        df.setNamespaceAware(true);
        DocumentBuilder db = df.newDocumentBuilder();
        File xslDir = new File(base, "xsl");
        File libsFile = new File(xslDir, "libs.xsl");
        Document libs = db.parse(libsFile);
        NodeList current = libs.getDocumentElement().getChildNodes();
        String libName = "lib-" + name + ".xsl";
        for (int i = 0; i < current.getLength(); i++) {
            if (current.item(i) instanceof Element) {
                if (libName.equals(((Element) current.item(i)).getAttribute("href"))) {
                    return;
                }
            }
        }
        Element newLib = libs.createElementNS("http://www.w3.org/1999/XSL/Transform", "include");
        newLib.setAttribute("href", libName);
        libs.getDocumentElement().appendChild(newLib);
        save(libs, libsFile);
    }

    /**
	 * @throws IOException
	 * @throws TransformerException
	 */
    private void buildPackageXsl() throws IOException, TransformerException {
        ClassLoader loader = LibraryBuilder.class.getClassLoader();
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer packageTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/elements.xsl")));
        Writer writer = null;
        try {
            File xslDir = new File(base, "xsl");
            writer = new BufferedWriter(new FileWriter(new File(xslDir, "lib-" + name + ".xsl")));
            transform(elements, packageTransformer, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
	 * @param libraryDir
	 * @param docDir
	 * @param xslDir
	 * @param imageDir
	 * @param pack
	 */
    private void buildDirectory(File libraryDir, File docDir, File xslDir, File jspTagDir, File imageDir, String pack) {
        String[] files = libraryDir.list();
        for (String file : files) {
            if (file.endsWith(".xml")) {
                File f = new File(libraryDir, file);
                String element = file.substring(0, file.lastIndexOf("."));
                try {
                    buildDocs(f, docDir, element, pack);
                    buildXsl(f, xslDir, element);
                    buildCss(f);
                    buildJs(f);
                    register(pack, element);
                    File imgDir = new File(libraryDir, element);
                    if (imgDir.exists()) {
                        buildImages(new File(imageDir, element), imgDir);
                    }
                } catch (IOException e) {
                    throw new BadDescriptorException(pack + "." + element, e);
                } catch (TransformerException e) {
                    throw new BadDescriptorException(pack + "." + element, e);
                }
            } else {
                File child = new File(libraryDir, file);
                if (child.isDirectory() && Character.isLowerCase(file.charAt(0))) {
                    buildDirectory(child, docDir, new File(xslDir, file), new File(jspTagDir, file), new File(imageDir, file), ("".equals(pack) ? file : pack + "." + file));
                }
            }
        }
    }

    /**
	 * @param pack
	 * @param element
	 */
    private void register(String pack, String element) {
        Element e = elements.createElement("htmli-element");
        e.setAttribute("name", element);
        e.setAttribute("package", pack);
        elements.getDocumentElement().appendChild(e);
    }

    /**
	 * @param imgDir
	 * @param outDir
	 * @throws IOException
	 */
    private void buildImages(File outDir, File imgDir) throws IOException {
        String[] files = imgDir.list();
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        for (String file : files) {
            if (!file.startsWith(".svn")) {
                FileUtils.copyFileToDirectory(new File(imgDir, file), outDir);
            }
        }
    }

    /**
	 * @param f
	 * @throws TransformerException
	 * @throws IOException
	 */
    private void buildJs(File f) throws TransformerException, IOException {
        transform(f, jsTransformer, jsWriter);
    }

    /**
	 * @param f
	 * @throws TransformerException
	 * @throws IOException
	 */
    private void buildCss(File f) throws TransformerException, IOException {
        transform(f, cssTransformer, cssWriter);
    }

    /**
	 * @param f
	 * @param xslDir
	 * @param element
	 * @throws IOException
	 * @throws TransformerException
	 */
    private void buildXsl(File f, File xslDir, String element) throws IOException, TransformerException {
        if (!xslDir.exists()) {
            xslDir.mkdirs();
        }
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(xslDir, element + ".xsl")), "utf-8"));
            transform(f, xslTransformer, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
	 * @param f
	 * @param docDir
	 * @param element
	 * @param pack
	 * @throws IOException
	 * @throws TransformerException
	 */
    private void buildDocs(File f, File docDir, String element, String pack) throws IOException, TransformerException {
        File outputFile = new File(docDir, pack.replaceAll("\\.", "_") + "_" + element + ".html");
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputFile);
            transform(f, docTransformer, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
	 * Generic XSL transformer.
	 *
	 * @param xml
	 *            Source xml file
	 * @param xsl
	 *            Xsl Transformer
	 * @param out
	 *            Output from transformation
	 * @throws TransformerException
	 * @throws IOException
	 */
    private void transform(File xml, Transformer xsl, Writer out) throws TransformerException, IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(xml));
            xsl.transform(new StreamSource(is), new StreamResult(out));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
	 * Generic XSL transformer.
	 *
	 * @param xml
	 * @param xsl
	 * @param out
	 * @throws TransformerException
	 * @throws IOException
	 */
    private void transform(Document xml, Transformer xsl, Writer out) throws TransformerException, IOException {
        xsl.transform(new DOMSource(xml), new StreamResult(out));
    }

    /**
	 * Generic XML saver.
	 *
	 * @param doc
	 * @param out
	 * @throws TransformerException
	 * @throws IOException
	 */
    private void save(Document doc, File out) throws TransformerException, IOException {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(out));
            transformer.transform(new DOMSource(doc), new StreamResult(os));
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }
}
