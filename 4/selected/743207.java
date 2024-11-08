package org.lindenb.tinytools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.lindenb.io.IOUtils;
import org.lindenb.util.AbstractApplication;

/**
 * 
 * WebAppIsDown
 *
 */
public class WebAppIsDown extends AbstractApplication {

    private static final String J2EE = "http://java.sun.com/xml/ns/javaee";

    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private File fileout;

    private File messageFile = null;

    private String messageString = null;

    private WebAppIsDown() {
    }

    @Override
    protected void usage(PrintStream out) {
        out.println("Creates an empty J2EE web app with a custom message.");
        out.println("Options:");
        out.println(" -f <file> load custom message as file");
        out.println(" -s <s> load custom message as string");
        super.usage(out);
    }

    @Override
    protected int processArg(String[] args, int optind) {
        if (args[optind].equals("-f")) {
            this.messageFile = new File(args[++optind]);
            return optind;
        } else if (args[optind].equals("-m")) {
            this.messageString = args[++optind];
            return optind;
        }
        return super.processArg(args, optind);
    }

    private void createWar() throws IOException, XMLStreamException {
        String appName = this.fileout.getName();
        int i = appName.indexOf(".");
        if (i != -1) appName = appName.substring(0, i);
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(this.fileout));
        {
            ZipEntry entry = new ZipEntry("WEB-INF/web.xml");
            zout.putNextEntry(entry);
            XMLOutputFactory factory = XMLOutputFactory.newInstance();
            XMLStreamWriter w = factory.createXMLStreamWriter(zout, "ASCII");
            w.writeStartDocument("ASCII", "1.0");
            w.writeStartElement("web-app");
            w.writeAttribute("xsi", XSI, "schemaLocation", "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml /ns/javaee/web-app_2_5.xsd");
            w.writeAttribute("version", "2.5");
            w.writeAttribute("xmlns", J2EE);
            w.writeAttribute("xmlns:xsi", XSI);
            w.writeStartElement("description");
            w.writeCharacters("Site maintenance for " + appName);
            w.writeEndElement();
            w.writeStartElement("display-name");
            w.writeCharacters(appName);
            w.writeEndElement();
            w.writeStartElement("servlet");
            w.writeStartElement("servlet-name");
            w.writeCharacters("down");
            w.writeEndElement();
            w.writeStartElement("jsp-file");
            w.writeCharacters("/WEB-INF/jsp/down.jsp");
            w.writeEndElement();
            w.writeEndElement();
            w.writeStartElement("servlet-mapping");
            w.writeStartElement("servlet-name");
            w.writeCharacters("down");
            w.writeEndElement();
            w.writeStartElement("url-pattern");
            w.writeCharacters("/*");
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndElement();
            w.writeEndDocument();
            w.flush();
            zout.closeEntry();
        }
        {
            ZipEntry entry = new ZipEntry("WEB-INF/jsp/down.jsp");
            zout.putNextEntry(entry);
            PrintWriter w = new PrintWriter(zout);
            if (this.messageFile != null) {
                IOUtils.copyTo(new FileReader(this.messageFile), w);
            } else if (this.messageString != null) {
                w.print("<html><body>" + this.messageString + "</body></html>");
            } else {
                w.print("<html><body><div style='text-align:center;font-size:500%;'>Oh No !<br/><b>" + appName + "</b><br/>is down for maintenance!</div></body></html>");
            }
            w.flush();
            zout.closeEntry();
        }
        zout.finish();
        zout.flush();
        zout.close();
    }

    public static void main(String[] args) {
        LOG.setLevel(Level.OFF);
        try {
            WebAppIsDown app = new WebAppIsDown();
            int i = app.processArgs(args);
            if (i + 1 != args.length) {
                System.err.println("Illegal number of arguments.");
                return;
            }
            app.fileout = new File(args[i]);
            if (!app.fileout.getName().endsWith(".war")) {
                System.err.println("Name should end with '.war':" + app.fileout);
                return;
            }
            app.createWar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
