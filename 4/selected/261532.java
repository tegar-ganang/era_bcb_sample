package org.systemsbiology.apmlparser.v2;

import java.io.*;
import org.apache.xmlbeans.XmlException;
import org.apache.log4j.Logger;
import javax.xml.stream.XMLStreamException;

/**
 * APMLParserApp is a main class to demonstrate how to use org.systemsbiology.apmlparser.v2 classes.
 * @author Damon May
 * @version version 2.0
 * @since March 14, 2008
 */
public class APMLParserApp {

    static Logger _log = Logger.getLogger(APMLParserApp.class);

    public static void main(String[] args) {
        int numArgs = args.length;
        if (numArgs != 1 && numArgs != 2) {
            printUsage();
            return;
        }
        String apmlFileName = args[0];
        File apmlFile = new File(apmlFileName);
        if (!apmlFile.exists() || !apmlFile.canRead()) throw new IllegalArgumentException("Cannot access file " + apmlFileName);
        File outFile = null;
        if (numArgs == 2) {
            String outFileName = args[1];
            outFile = new File(outFileName);
        }
        try {
            DefaultAPMLReaderListener readerListener = new DefaultAPMLReaderListener();
            APMLReader apmlReader = new APMLReader(readerListener);
            apmlReader.setReadSingleScanPeaks(true);
            apmlReader.read(apmlFile);
            if (outFile != null) {
                APMLWriter apmlWriter = new APMLWriter();
                apmlWriter.writeFromDefaultListener(outFile, readerListener);
            }
        } catch (FileNotFoundException fnfe) {
            System.err.println("File not found");
        } catch (XMLStreamException xse) {
            System.err.println("XMLStreamException");
            xse.printStackTrace(System.err);
        } catch (XmlException xe) {
            System.err.println("XmlException");
            System.err.println(xe.getMessage());
            xe.printStackTrace(System.err);
        } catch (IOException ioe) {
            System.err.println("IOException");
            System.err.println(ioe.getMessage());
            ioe.printStackTrace(System.err);
        }
    }

    protected static void printUsage() {
        System.err.println("Usage: " + APMLParserApp.class.getName());
        System.err.println("\t<apml_file_name> [output_file_name]");
    }
}
