package com.squadlimber.awesome;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AwesomeAcceptanceTestCase extends TestCase {

    private Document testDoc;

    private String htmlFileName;

    private File outputDirectory = new File(System.getProperty("java.io.tmpdir"), "awesome");

    private Exception exceptionInRunBare = null;

    @Override
    public final void runBare() throws Throwable {
        htmlFileName = getHtmlFilename(this.getClass().getSimpleName());
        try {
            testDoc = XmlUtils.loadFileFromClasspath(htmlFileName, this.getClass());
            super.runBare();
        } catch (FileNotFoundException e) {
            exceptionInRunBare = new FileNotFoundException("Could not find default test specification: " + htmlFileName);
        } catch (JDOMException e) {
            exceptionInRunBare = new JDOMException("Could not parse the document as xml: " + htmlFileName, e);
        }
        if (exceptionInRunBare != null) {
            throw exceptionInRunBare;
        }
    }

    public void testProcessDocument() throws Exception {
        File report = new File(findOrCreateOutputDirectory(), htmlFileName);
        AwesomeTestReportingListener listener = new HtmlReportingListener(report, testDoc);
        AwesomeHtmlParser parser = new AwesomeHtmlParser(testDoc, this, listener);
        parser.parseDocument();
        copyCSSFileToOutputDirectory();
        copyJavaScriptFilesToOutputDirectory();
        if (listener.testsFailed()) {
            fail("Test failed. See the test report in: " + report);
        } else {
            System.out.println("Test passed. See the test report in: " + report);
        }
    }

    private void copyJavaScriptFilesToOutputDirectory() throws IOException {
        File dhtmlDir = new File("lib/dhtml");
        if (dhtmlDir.exists()) {
            FileUtils.copyDirectoryToDirectory(dhtmlDir, outputDirectory);
        } else {
            System.err.println("Warning: DHTML directory " + dhtmlDir.getCanonicalPath() + " could not be found");
        }
    }

    private void copyCSSFileToOutputDirectory() throws IOException {
        Element linkElement;
        try {
            linkElement = XmlUtils.findElementByName(testDoc, "link");
        } catch (JDOMException noLinkElementFound) {
            return;
        }
        File cssFile = new File(linkElement.getAttributeValue("href"));
        if (cssFile.exists()) {
            FileUtils.copyFileToDirectory(cssFile, outputDirectory);
        } else {
            System.err.println("Warning: CSS file " + cssFile.getCanonicalPath() + " could not be found");
        }
    }

    private File findOrCreateOutputDirectory() {
        File outputDir = outputDirectory;
        if (!outputDir.isDirectory()) {
            outputDir.mkdirs();
        }
        return outputDir;
    }

    private static String getHtmlFilename(String className) {
        return StringUtils.chomp(className, "Test") + ".html";
    }

    protected void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }
}
