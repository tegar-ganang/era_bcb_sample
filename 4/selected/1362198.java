package org.testtoolinterfaces.testresultinterface;

import java.util.Enumeration;
import java.util.Hashtable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOError;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.testtoolinterfaces.testresult.TestRunResult;
import org.testtoolinterfaces.utils.Trace;
import org.testtoolinterfaces.utils.Warning;

/**
 * @author Arjan Kranenburg
 *
 */
public class TestRunResultXmlWriter implements TestRunResultWriter {

    File myXslSourceDir = null;

    String myTestEnvironment = "Unknown";

    String myTestPhase = "Unknown";

    /**
	 * 
	 */
    public TestRunResultXmlWriter() {
    }

    /**
	 * @param anXslSourceDir the directory containing the XSL file, stylesheets, etc.
	 * @param anEnvironment the environment where the test is executed.
	 * All these files may be needed for the presentation of the test results.
	 */
    public TestRunResultXmlWriter(File anXslSourceDir, String anEnvironment, String aTestPhase) {
        if (anXslSourceDir == null) {
            throw new Error("No directory specified.");
        }
        if (!anXslSourceDir.isDirectory()) {
            throw new Error("Not a directory: " + anXslSourceDir.getAbsolutePath());
        }
        myXslSourceDir = anXslSourceDir;
        myTestEnvironment = anEnvironment;
        myTestPhase = aTestPhase;
    }

    public void print(TestRunResult aRunResult) {
        OutputStreamWriter stdOutWriter = new OutputStreamWriter(System.out);
        try {
            printXmlHeader(aRunResult, stdOutWriter, "STDOUT");
            printXmlTestRuns(aRunResult, stdOutWriter);
            stdOutWriter.flush();
        } catch (IOException e) {
            Warning.println("Printing XML failed: " + e.getMessage());
            Trace.print(Trace.LEVEL.SUITE, e);
        }
    }

    public void writeToFile(TestRunResult aRunResult, File aFileName) {
        File logDir = aFileName.getParentFile();
        if (!logDir.exists()) {
            logDir.mkdir();
        }
        copyXsl(logDir);
        FileWriter xmlFile;
        try {
            xmlFile = new FileWriter(aFileName);
            printXmlHeader(aRunResult, xmlFile, aFileName.getName());
            printXmlTestRuns(aRunResult, xmlFile);
            xmlFile.flush();
        } catch (IOException e) {
            Warning.println("Saving XML failed: " + e.getMessage());
            Trace.print(Trace.LEVEL.SUITE, e);
        }
    }

    /**
	 * @param aFile
	 * @throws IOException
	 */
    public void printXmlHeader(TestRunResult aRunResult, OutputStreamWriter aStream, String aDocName) throws IOException {
        aStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        aStream.write("<?xml-stylesheet type=\"text/xsl\" href=\"testLog.xsl\"?>\n\n");
        aStream.write("<!--\n");
        aStream.write("    Document   : " + aDocName + "\n");
        aStream.write("    Created on : " + aRunResult.getStartDateString() + "\n");
        aStream.write("    Author     : " + aRunResult.getAuthor() + "\n");
        aStream.write("    Name       : " + aRunResult.getDisplayName() + "\n");
        aStream.write("-->\n");
    }

    /**
	 * @param aFile
	 * @throws IOException
	 */
    public void printXmlTestRuns(TestRunResult aRunResult, OutputStreamWriter aStream) throws IOException {
        aStream.write("<testrun");
        aStream.write(" name='" + aRunResult.getDisplayName() + "'");
        aStream.write(" suite='" + aRunResult.getTestSuite() + "'");
        aStream.write(" environment='" + myTestEnvironment + "'");
        aStream.write(" phase='" + myTestPhase + "'");
        aStream.write(" author='" + aRunResult.getAuthor() + "'");
        aStream.write(" machine='" + aRunResult.getMachine() + "'");
        aStream.write(" created='" + aRunResult.getStartDateString() + "'");
        aStream.write(" startdate='" + aRunResult.getStartDateString() + "'");
        aStream.write(" starttime='" + aRunResult.getStartTimeString() + "'");
        aStream.write(" enddate='" + aRunResult.getEndDateString() + "'");
        aStream.write(" endtime='" + aRunResult.getEndTimeString() + "'");
        aStream.write(">\n");
        printXmlSut(aRunResult, aStream);
        TestGroupResultXmlWriter tgResultWriter = new TestGroupResultXmlWriter(aRunResult.getTestGroup(), 0);
        tgResultWriter.printXml(aStream);
        aStream.write("</testrun>\n");
    }

    /**
	 * @param aFile
	 * @throws IOException
	 */
    public void printXmlSut(TestRunResult aRunResult, OutputStreamWriter aStream) throws IOException {
        if (!aRunResult.getSutProduct().isEmpty()) {
            aStream.write("    <systemundertest");
            aStream.write(" product='" + aRunResult.getSutProduct() + "'");
            aStream.write(">\n");
            aStream.write("      <version");
            aStream.write(" mainLevel='" + aRunResult.getSutVersionMainLevel() + "'");
            aStream.write(" subLevel='" + aRunResult.getSutVersionSubLevel() + "'");
            aStream.write(" patchLevel='" + aRunResult.getSutVersionPatchLevel() + "'");
            aStream.write(">\n");
            printXmlLogFiles(aRunResult.getSutLogs(), aStream);
            aStream.write("      </version>\n");
            aStream.write("    </systemundertest>\n");
        }
    }

    /**
	 * Checks if there are log-files and then prints them in XML format 
	 * 
	 * @param aLogs     Hashtable of the logfiles
	 * @param aStream   OutputStreamWriter of the stream to print the xml to
	 * 
	 * @throws IOException
	 */
    public void printXmlLogFiles(Hashtable<String, String> aLogs, OutputStreamWriter aStream) throws IOException {
        Trace.println(Trace.LEVEL.UTIL);
        if (!aLogs.isEmpty()) {
            aStream.write("        <logFiles>\n");
            for (Enumeration<String> keys = aLogs.keys(); keys.hasMoreElements(); ) {
                String key = keys.nextElement();
                aStream.write("          <logFile");
                aStream.write(" type='" + key + "'");
                aStream.write(">" + aLogs.get(key));
                aStream.write("</logFile>\n");
            }
            aStream.write("        </logFiles>\n");
        }
    }

    private void copyXsl(File aTargetLogDir) {
        Trace.println(Trace.LEVEL.UTIL, "copyXsl( " + aTargetLogDir.getName() + " )", true);
        if (myXslSourceDir == null) {
            return;
        }
        File[] files = myXslSourceDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File srcFile = files[i];
            if (!srcFile.isDirectory()) {
                File tgtFile = new File(aTargetLogDir + File.separator + srcFile.getName());
                FileChannel inChannel = null;
                FileChannel outChannel = null;
                try {
                    inChannel = new FileInputStream(srcFile).getChannel();
                    outChannel = new FileOutputStream(tgtFile).getChannel();
                    inChannel.transferTo(0, inChannel.size(), outChannel);
                } catch (IOException e) {
                    throw new IOError(e);
                } finally {
                    if (inChannel != null) try {
                        inChannel.close();
                    } catch (IOException exc) {
                        throw new IOError(exc);
                    }
                    if (outChannel != null) try {
                        outChannel.close();
                    } catch (IOException exc) {
                        throw new IOError(exc);
                    }
                }
            }
        }
    }
}
