package org.eigenbase.xom;

import java.io.*;

/**
 * Test the MSParser
 */
public class ParserTester {

    private int parserType;

    private static final int MSXML = 1;

    private static final int XERCES = 2;

    private Parser parser;

    String modelDocType;

    String dtdUrl;

    public ParserTester(String dtdFile, int parserType) throws XOMException, IOException {
        this.parserType = parserType;
        parser = null;
        File dtdPath = new File(dtdFile);
        this.modelDocType = dtdFile.substring(0, dtdFile.indexOf("."));
        this.dtdUrl = "file:" + dtdPath.getAbsolutePath();
        switch(parserType) {
            default:
                throw new XOMException("Unknown parser type: " + parserType);
        }
    }

    public void testFile(String testFile) throws XOMException {
        String xmlString = null;
        try {
            StringWriter sWriter = new StringWriter();
            FileReader reader = new FileReader(testFile);
            if (parserType != MSXML) {
                PrintWriter out = new PrintWriter(sWriter);
                out.println("<?xml version=\"1.0\" ?>");
                if (modelDocType != null) out.println("<!DOCTYPE " + modelDocType + " SYSTEM \"" + dtdUrl + "\">");
                out.flush();
            }
            readerToWriter(reader, sWriter);
            reader.close();
            xmlString = sWriter.toString();
        } catch (IOException ex) {
            throw new XOMException("Unable to read input test " + testFile + ": " + ex.getMessage());
        }
        parser.parse(xmlString);
        System.out.println("Parsing document succeeded.");
    }

    /**
     * Helper function to copy from a reader to a writer
     */
    private static void readerToWriter(Reader reader, Writer writer) throws IOException {
        int numChars;
        final int bufferSize = 16384;
        char[] buffer = new char[bufferSize];
        while ((numChars = reader.read(buffer)) != -1) {
            if (numChars > 0) writer.write(buffer, 0, numChars);
        }
    }

    /**
     * The ParserTester tests MSXML parser and Xerces Parser. Arguments:
     * <ol>
     * <li> The DTD file of the XML file
     * <li> The XML file for this DTD file
     * </ol>
     * </p>
     */
    public static void main(String args[]) throws XOMException, IOException {
        int firstArg = 0;
        if (args.length > 0 && args[0].equals("-debug")) {
            System.err.println("parserTest pausing for debugging. " + "Attach your debugger " + "and press return.");
            try {
                System.in.read();
                firstArg++;
            } catch (IOException ex) {
            }
        }
        int parserType = MSXML;
        if (firstArg < args.length && args[firstArg].equals("-msxml")) {
            parserType = MSXML;
            firstArg++;
        } else if (firstArg < args.length && args[firstArg].equals("-xerces")) {
            parserType = XERCES;
            firstArg++;
        }
        if (args.length < firstArg + 2) {
            System.err.println("Usage: java ParserTester [-debug] [-msxml | -xerces] " + "<DTD file> <XML file>");
            System.exit(-1);
        }
        ParserTester parserTester = new ParserTester(args[firstArg], parserType);
        firstArg++;
        parserTester.testFile(args[firstArg]);
    }
}
