package com.htmli.compiler;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * This class builds HTMLi base distribution (i.e. HTMLi without any tag
 * library.
 * 
 * @author Matias Bagini
 */
public class BaseBuilder {

    /**
	 * Directory where doc files are stored.
	 */
    private File docDir;

    /**
	 * Directory where javascript files are stored.
	 */
    private File jsDir;

    /**
	 * Directory where base files are stored.
	 */
    private File baseDir;

    /**
	 * Javascript transformer.
	 */
    private Transformer jsTransformer;

    /**
	 * Docs transformer.
	 */
    private Transformer docTransformer;

    /**
	 * @param base
	 *            Root directory of HTMLi distribution
	 */
    public BaseBuilder(File base) {
        this.docDir = new File(base, "doc");
        this.jsDir = new File(base, "js");
        this.baseDir = new File(base, "base");
    }

    public static void sync(File source, File dest) throws IOException {
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File[] files = source.listFiles();
        for (File f : files) {
            if (f.getName().startsWith(".svn")) {
                continue;
            }
            if (f.isDirectory()) {
                sync(f, new File(dest, f.getName()));
            } else {
                FileUtils.copyFileToDirectory(f, dest);
            }
        }
    }

    public synchronized void build() throws IOException, TransformerException {
        ClassLoader loader = BaseBuilder.class.getClassLoader();
        TransformerFactory tFactory = TransformerFactory.newInstance();
        jsTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/js.xsl")));
        docTransformer = tFactory.newTransformer(new StreamSource(loader.getResourceAsStream("com/htmli/compiler/xsl/doc.xsl")));
        buildJavascript();
        buildHTML();
        buildHTMLi();
    }

    private void buildHTMLi() throws IOException, TransformerException {
        File htmliDir = new File(baseDir, "htmli");
        String[] files = htmliDir.list();
        for (String file : files) {
            if (file.endsWith(".xml")) {
                buildHTMLiDocs(new File(htmliDir, file), docDir, file.substring(0, file.lastIndexOf(".")));
            }
        }
    }

    private void buildHTML() throws IOException, TransformerException {
        File htmlDir = new File(baseDir, "html");
        String[] files = htmlDir.list();
        Writer writer = new StringWriter();
        for (String file : files) {
            if (file.endsWith(".xml")) {
                buildHTMLDocs(new File(htmlDir, file), docDir, file.substring(0, file.lastIndexOf(".")));
                buildHTMLJs(new File(htmlDir, file), writer);
            }
        }
        Writer js = null;
        try {
            js = new BufferedWriter(new FileWriter(new File(jsDir, "html.js")));
            js.write(writer.toString().replaceAll("\\s+", " "));
        } finally {
            if (js != null) {
                js.close();
            }
        }
    }

    private void buildHTMLiDocs(File source, File outDir, String element) throws IOException, TransformerException {
        File outputFile = new File(outDir, element + ".html");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
            transform(source, docTransformer, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void buildHTMLDocs(File source, File outDir, String element) throws IOException, TransformerException {
        File outputFile = new File(outDir, element + "Wrapper.html");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
            transform(source, docTransformer, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void buildHTMLJs(File file, Writer writer) throws TransformerException, IOException {
        transform(file, jsTransformer, writer);
    }

    private void buildJavascript() throws IOException, TransformerException {
        File jsDir = new File(baseDir, "javascript");
        String[] files = jsDir.list();
        for (String file : files) {
            if (file.endsWith(".xml")) {
                buildJavascriptDocs(new File(jsDir, file), docDir, file.substring(0, file.lastIndexOf(".")));
            }
        }
    }

    private void buildJavascriptDocs(File source, File outDir, String element) throws IOException, TransformerException {
        File outputFile = new File(outDir, element + ".html");
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
            transform(source, docTransformer, writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

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
}
