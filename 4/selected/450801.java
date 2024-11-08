package org.txt2xml.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import org.txt2xml.config.ProcessorFactory;
import org.txt2xml.core.Processor;
import org.txt2xml.driver.StreamDriver;

/**
 * A simple command line utility to apply a txt2xml
 * conversion with a specified configuration to a set
 * of files.
 * 
 * Usage:
 * <pre>
 * java org.txt2xml.cli.Batch <config_xml> <source_file>*
 * </pre>
 * Applies txt2xml to all source files as configured by the config_xml,
 * saving result xml by appending .xml to the source filename.
 * 
 * @author <A HREF="mailto:smeyfroi@users.sourceforge.net">Steve Meyfroidt</A>
 */
public class Batch {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java org.txt2xml.cli.Batch <config_xml> <source_file>*");
            System.out.println("Applies txt2xml to all source files as configured by the config_xml,\n" + "saving result xml by appending .xml to the source filename.");
            System.exit(1);
        }
        String config = args[0];
        String[] sources = new String[args.length - 1];
        System.arraycopy(args, 1, sources, 0, sources.length);
        URL configUrl = new File(config).toURL();
        Processor processor = ProcessorFactory.getInstance().createProcessor(configUrl);
        StreamDriver driver = new StreamDriver(processor);
        driver.useDebugOutputProperties();
        for (int i = 0; i < sources.length; i++) {
            String sourceName = sources[i];
            processFile(driver, sourceName);
        }
    }

    private static void processFile(StreamDriver driver, String sourceName) throws Exception {
        String destName = sourceName + ".xml";
        File dest = new File(destName);
        if (dest.exists()) {
            throw new IllegalArgumentException("File '" + destName + "' already exists!");
        }
        FileChannel sourceChannel = new FileInputStream(sourceName).getChannel();
        try {
            MappedByteBuffer sourceByteBuffer = sourceChannel.map(FileChannel.MapMode.READ_ONLY, 0, sourceChannel.size());
            CharsetDecoder decoder = Charset.forName("ISO-8859-15").newDecoder();
            CharBuffer sourceBuffer = decoder.decode(sourceByteBuffer);
            driver.generateXmlDocument(sourceBuffer, new FileOutputStream(dest));
        } finally {
            sourceChannel.close();
        }
    }
}
