package org.lindenb.tinytools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.lindenb.util.Compilation;
import org.lindenb.util.StringUtils;
import org.lindenb.xml.EchoHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XAR a tool extracting a XML archive
 * @author lindenb
 *
 */
public class Xar extends DefaultHandler {

    /** depth in the XML document */
    private int depth = -1;

    /** base directory for output */
    private File outputDir = null;

    /** current writer */
    private PrintWriter out;

    /** current file content-type (default is text/plain ) */
    private String contentType = null;

    /** current SAX handler for copying an internal xml document */
    private EchoHandler echoHandler = null;

    /** should files already exists or not ? */
    private boolean exclusiveCreate = false;

    /** can we overwrite existings file */
    private boolean allowOverwrite = true;

    private Xar() {
    }

    @Override
    public void startDocument() throws SAXException {
        this.depth = -1;
        this.out = null;
        this.contentType = null;
        this.echoHandler = null;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        this.depth++;
        if (this.depth == 0) {
            if (!localName.equals("archive")) throw new SAXException("Expected <archive> as root but found " + localName);
        } else if (this.depth == 1) {
            if (!localName.equals("file")) throw new SAXException("Expected <archive> as root but found " + localName);
            String path = attributes.getValue("path");
            if (path != null) path = path.trim();
            while (path != null && path.length() > 0 && path.charAt(0) == File.separatorChar) {
                System.err.println("Removing leading " + File.separatorChar + " from path=" + path);
                path = path.substring(1);
            }
            File currentDir = this.outputDir;
            while (true) {
                int i = path.indexOf(File.separatorChar);
                if (i == -1) break;
                File subdir = new File(currentDir, path.substring(0, i));
                path = path.substring(i + 1);
                if (!subdir.exists()) {
                    if (!subdir.mkdir()) {
                        throw new SAXException("Cannot create directory " + subdir);
                    }
                    System.out.println("Creating directory :" + subdir);
                }
                currentDir = subdir;
            }
            if (path == null || path.length() == 0) throw new SAXException("In element <" + name + "> @path missing");
            try {
                File f = new File(currentDir, path);
                if (exclusiveCreate && f.exists()) throw new SAXException("Exclusive Writing enabled and file exists: " + f);
                System.out.print(f);
                String overwrite = attributes.getValue("overwrite");
                if (f.exists() && overwrite != null && overwrite.equals("false")) {
                    System.out.print(" ... @overwrite=false. ignoring ");
                    this.out = null;
                } else if (!allowOverwrite && f.exists()) {
                    System.out.print(" ... ignoring ");
                    this.out = null;
                } else {
                    this.out = new PrintWriter(new FileWriter(f));
                }
            } catch (IOException e) {
                throw new SAXException(e);
            }
            this.contentType = attributes.getValue("content-type");
            if (this.contentType == null) this.contentType = "text/plain";
            if (!(this.contentType.equals("text/plain") || this.contentType.equals("text/xml"))) {
                throw new SAXException("unknown content-type " + this.contentType);
            }
            if (this.out != null && this.contentType.equals("text/xml")) {
                this.echoHandler = new EchoHandler(this.out);
                this.echoHandler.startDocument();
            }
        } else if (this.out != null) {
            if (this.echoHandler != null) {
                this.echoHandler.startElement(uri, localName, name, attributes);
            } else {
                throw new SAXException("Found illegal tag :" + localName);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
        if (this.depth == 1) {
            System.out.println(" ... Done.");
            if (this.echoHandler != null) {
                this.echoHandler.endDocument();
            }
            if (this.out != null) {
                this.out.flush();
                if (this.out.checkError()) throw new SAXException("Something went wront with the output");
                this.out.close();
            }
            this.echoHandler = null;
            this.out = null;
            this.contentType = null;
        } else if (this.depth == 0) {
        } else if (this.out != null) {
            if (this.echoHandler != null) {
                this.echoHandler.endElement(uri, localName, name);
            } else {
                throw new SAXException("Found illegal tag <" + name + "> under <file> with content-type=" + this.contentType);
            }
        }
        this.depth--;
    }

    @Override
    public void endDocument() throws SAXException {
        this.depth = -1;
        this.out = null;
        this.contentType = null;
        this.echoHandler = null;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.depth < 1 && !StringUtils.isBlank(ch, start, length)) {
            throw new SAXException("Illegal non-white characters before <file>.:\"" + new String(ch, start, length) + "\"");
        }
        if (this.echoHandler != null) {
            this.echoHandler.characters(ch, start, length);
        } else if (this.out != null) {
            out.write(ch, start, length);
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (this.echoHandler != null) this.echoHandler.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        if (this.echoHandler != null) this.echoHandler.endPrefixMapping(prefix);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        if (this.echoHandler != null) this.echoHandler.processingInstruction(target, data);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        this.characters(ch, start, length);
    }

    public static void main(String[] args) {
        try {
            Xar handler = new Xar();
            int optind = 0;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println("Pierre Lindenbaum PhD 2009.");
                    System.err.println(Compilation.getLabel());
                    System.err.println("This tool expand an XML archive and generate the files (see format below)");
                    System.err.println("options:");
                    System.err.println(" -h this screen");
                    System.err.println(" -X eXclusive create: files should not already exist");
                    System.err.println(" -N No overwrite: a file will not be over-written if it already exists.");
                    System.err.println(" -D output directory : where to exand the file (default is the current-directory)");
                    System.err.println("(stdin|urls|files) sources ending with *.xarz or *.xml.gz will be g-unzipped.");
                    System.err.println("\n\nExample:\n\n" + "<?xml version=\"1.0\"?>\n" + "<archive>\n" + " <file path=\"mydir/file.01.txt\">\n" + "  Hello World !\n" + " </file>\n" + " <file path=\"mydir/file.02.text\" content-type=\"text/plain\">\n" + "  Hello World &lt;!\n" + " </file>\n" + " <file path=\"mydir/file.03.text\" overwrite='false'> <!-- won't write if file exists -->\n" + "  Hello World &lt;!\n" + " </file>\n" + " <file path=\"mydir/file.02.xml\" content-type=\"text/xml\">\n" + "  <a>Hello World !<b xmlns=\"urn:any\" att=\"x\">azdpoazd<i/></b></a>\n" + " </file>\n" + "</archive>\n");
                    return;
                } else if (args[optind].equals("-X")) {
                    handler.exclusiveCreate = true;
                } else if (args[optind].equals("-N")) {
                    handler.allowOverwrite = false;
                } else if (args[optind].equals("-D")) {
                    handler.outputDir = new File(args[++optind]);
                    if (!handler.outputDir.exists()) {
                        System.err.println(handler.outputDir + " directory doesn't exist");
                        return;
                    }
                    if (!handler.outputDir.isDirectory()) {
                        System.err.println(handler.outputDir + " is not a directory");
                        return;
                    }
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            SAXParserFactory saxFactory = SAXParserFactory.newInstance();
            saxFactory.setNamespaceAware(true);
            saxFactory.setValidating(false);
            SAXParser parser = saxFactory.newSAXParser();
            if (optind == args.length) {
                parser.parse(System.in, handler);
            } else {
                while (optind < args.length) {
                    String file = args[optind++];
                    if (StringUtils.startsWith(file, "http://", "https://", "ftp://")) {
                        parser.parse(file, handler);
                    } else if (StringUtils.endsWith(file, ".xarz", ".xml.gz")) {
                        InputStream in = new GZIPInputStream(new FileInputStream(file));
                        parser.parse(in, handler);
                        in.close();
                    } else {
                        parser.parse(new File(file), handler);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Error] " + e.getMessage());
        }
    }
}
