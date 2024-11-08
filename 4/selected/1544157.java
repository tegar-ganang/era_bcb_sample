package org.globaltester.testmanager.reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.globaltester.logger.TestLogger;
import org.globaltester.testmanager.Activator;
import org.globaltester.testmanager.ITestExtender;
import org.globaltester.testmanager.preferences.PreferenceConstants;
import org.globaltester.testmanager.testframework.Failure;
import org.globaltester.testmanager.testframework.FileChecksum;
import org.globaltester.testmanager.testframework.TestCase;
import org.globaltester.testmanager.testframework.TestSuite;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ReportXML {

    private TestSuite testSuite;

    private long sessionTime = 0;

    private String internalPath = "/stylesheets/reports";

    private String testReportLogo = "/GT_Logo.gif";

    private String testReportDTD = "/testreport.dtd";

    private String testReportXSL = "/testreport.xsl";

    private String pathSep = "/";

    private String reportFileName;

    private String reportDirName;

    private boolean valid = false;

    public ReportXML(TestSuite testSuite) {
        this.testSuite = testSuite;
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        String newReportFileName = "Report_" + testSuite.getTestSuiteID() + ".xml";
        String newReportDirName = Activator.getWorkingDir() + pathSep + "Reports" + pathSep + Activator.getIsoDate("yyyyMMddHHmmss");
        if (!store.getBoolean(PreferenceConstants.P_FIXEDDIRSETTINGS)) {
            File newReportDir = new File(newReportDirName);
            newReportDir.mkdirs();
            FileDialog fileDialog = new FileDialog(Activator.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell(), SWT.SAVE);
            fileDialog.setFileName(newReportFileName);
            fileDialog.setFilterPath(newReportDirName);
            fileDialog.setText("Save test report");
            fileDialog.setFilterExtensions(new String[] { "*.xml" });
            if (fileDialog.open() == null) {
                this.valid = false;
                return;
            }
            newReportDirName = fileDialog.getFilterPath();
            newReportFileName = fileDialog.getFileName();
        } else {
            newReportDirName = store.getString(PreferenceConstants.P_REPORTDIR);
        }
        createReportEnvironment(newReportDirName, newReportFileName);
        Element root = createXMLStructure();
        saveDoc(root);
        this.valid = true;
    }

    /**
	 * Generates xml structure of test case
	 * 
	 * @return xml structure
	 */
    private Element createXMLStructure() {
        int passedTests = 0;
        int failedTests = 0;
        Element root = new Element("TESTREPORT");
        Element reportSpecName = new Element("SPECNAME");
        reportSpecName.setText(testSuite.testSuiteSpecName);
        root.addContent(reportSpecName);
        Element reportSpecVersion = new Element("SPECVERSION");
        reportSpecVersion.setText(testSuite.testSuiteSpecVersion);
        root.addContent(reportSpecVersion);
        Element reportID = new Element("TESTSUITEID");
        reportID.setText(testSuite.testSuiteID);
        root.addContent(reportID);
        Element reportDescr = new Element("SHORTDESCRIPTION");
        reportDescr.setText(testSuite.testSuiteShortDescr);
        root.addContent(reportDescr);
        Element reportRelease = new Element("RELEASE");
        reportRelease.setText(testSuite.testSuiteVersion);
        root.addContent(reportRelease);
        Element reportReleaseDate = new Element("RELEASEDATE");
        reportReleaseDate.setText(testSuite.testSuiteDate);
        root.addContent(reportReleaseDate);
        Element reportDate = new Element("DATE");
        Date now = new Date();
        reportDate.setText(now.toString());
        root.addContent(reportDate);
        Element reportUser = new Element("USER");
        reportUser.setText(System.getProperty("user.name"));
        root.addContent(reportUser);
        Element readerName = new Element("READER");
        String cardReaderName = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_CARDREADERNAME);
        readerName.setText(cardReaderName);
        root.addContent(readerName);
        Element integrityOfTestSuite = new Element("INTEGRITY");
        String integrity = FileChecksum.RESULTS[Activator.getDefault().getPreferenceStore().getInt(PreferenceConstants.P_TESTSUITEINTEGRITY)];
        integrityOfTestSuite.setText(integrity);
        root.addContent(integrityOfTestSuite);
        Element reportAddInfo = new Element("ADDITIONALINFO");
        Iterator<ITestExtender> iter = Activator.testExtenders.iterator();
        while (iter.hasNext()) {
            iter.next().extendReport(reportAddInfo);
        }
        if (reportAddInfo.getChildren().size() != 0) {
            root.addContent(reportAddInfo);
        }
        Element reportStatus = new Element("STATUS");
        reportStatus.setText(testSuite.getTestSuiteStatus());
        root.addContent(reportStatus);
        Element reportExcutedTests = new Element("EXECUTEDTESTS");
        reportExcutedTests.setText(new Integer(testSuite.getTestCases().size()).toString());
        root.addContent(reportExcutedTests);
        Element reportFailures = new Element("FAILURES");
        reportFailures.setText((new Integer(testSuite.getFailures())).toString());
        root.addContent(reportFailures);
        Element reportWarnings = new Element("WARNINGS");
        reportWarnings.setText((new Integer(testSuite.getWarnings())).toString());
        root.addContent(reportWarnings);
        for (int i = 0; i < testSuite.getTestCases().size(); i++) {
            TestCase tc = (TestCase) testSuite.getTestCases().get(i);
            Element reportTestCase = new Element("TESTCASE");
            Element reportTestCaseID = new Element("TESTCASEID");
            reportTestCaseID.setText(tc.getTestCaseID());
            reportTestCase.addContent(reportTestCaseID);
            Element reportTestCaseTime = new Element("TESTCASETIME");
            reportTestCaseTime.setText(String.valueOf(Math.rint(tc.getTime()) / 1000.));
            reportTestCase.addContent(reportTestCaseTime);
            sessionTime = sessionTime + tc.getTime();
            Element reportTestCaseStatus = new Element("TESTCASESTATUS");
            reportTestCaseStatus.setText(TestCase.STATUS_STRINGS[tc.getStatus()]);
            reportTestCase.addContent(reportTestCaseStatus);
            if (tc.getStatus() == TestCase.STATUS_PASSED) {
                passedTests++;
            }
            if (tc.getStatus() == TestCase.STATUS_FAILURE) {
                failedTests++;
            }
            Element reportTestCaseComment = new Element("TESTCASECOMMENT");
            reportTestCaseComment.setText(tc.getComment());
            reportTestCase.addContent(reportTestCaseComment);
            Element reportTestCaseDescr = new Element("TESTCASEDESCR");
            reportTestCaseDescr.setText(tc.getTestCaseDescr());
            reportTestCase.addContent(reportTestCaseDescr);
            LinkedList<Failure> failureList = tc.getFailureList();
            if (failureList != null) {
                for (int j = 0; j < failureList.size(); j++) {
                    Element failure = new Element("TESTCASEFAILURE");
                    Failure currentFailure = failureList.get(j);
                    Element failureID = new Element("FAILUREID");
                    failureID.setText((new Integer(currentFailure.getId())).toString());
                    failure.addContent(failureID);
                    Element failureRating = new Element("RATING");
                    failureRating.setText(Failure.RATING_STRINGS[currentFailure.getRating()]);
                    failure.addContent(failureRating);
                    Element failureText = new Element("DESCRIPTION");
                    failureText.setText(currentFailure.getFailureText());
                    failure.addContent(failureText);
                    Element failureLineScript = new Element("LINESCRIPT");
                    failureLineScript.setText((new Integer(currentFailure.getLineScript())).toString());
                    failure.addContent(failureLineScript);
                    Element failureLineLogFile = new Element("LINELOGFILE");
                    failureLineLogFile.setText((new Integer(currentFailure.getLineLogFile())).toString());
                    failure.addContent(failureLineLogFile);
                    Element failureExpectedVal = new Element("EXPECTEDVALUE");
                    failureExpectedVal.setText(currentFailure.getExpectedValue());
                    failure.addContent(failureExpectedVal);
                    Element failureReceivedVal = new Element("RECEIVEDVALUE");
                    failureReceivedVal.setText(currentFailure.getReceivedValue());
                    failure.addContent(failureReceivedVal);
                    reportTestCase.addContent(failure);
                }
            }
            root.addContent(reportTestCase);
        }
        Element reportTestsFailed = new Element("FAILEDTESTS");
        reportTestsFailed.setText((new Integer(failedTests).toString()));
        root.addContent(reportTestsFailed);
        Element reportTestsPassed = new Element("PASSEDTESTS");
        reportTestsPassed.setText((new Integer(passedTests).toString()));
        root.addContent(reportTestsPassed);
        Element reportTestSessionTime = new Element("TESTSESSIONTIME");
        reportTestSessionTime.setText(String.valueOf(Math.rint(sessionTime) / 1000.));
        root.addContent(reportTestSessionTime);
        File newReportDir = new File(reportDirName);
        Element reportDirectory = new Element("REPORTDIR");
        reportDirectory.setText(newReportDir.toURI().toString());
        root.addContent(reportDirectory);
        return root;
    }

    /**
	 * Writes the xml structure to file
	 * 
	 * @param root
	 *            xml structure of test report
	 */
    @SuppressWarnings("unchecked")
    private void saveDoc(Element root) {
        DocType type = new DocType("TESTREPORT", "testreport.dtd");
        ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"testreport.xsl\"");
        Document doc = new Document(root, type);
        doc.getContent().add(0, pi);
        Format format = Format.getRawFormat();
        format.setEncoding("ISO-8859-1");
        XMLOutputter serializer = new XMLOutputter(format);
        serializer.setFormat(Format.getPrettyFormat());
        try {
            FileOutputStream fos = new FileOutputStream(new File(reportDirName + pathSep + reportFileName));
            serializer.output(doc, fos);
            fos.close();
        } catch (IOException e) {
            TestLogger.error(e);
            return;
        }
        TestLogger.debug("Test report " + reportDirName + pathSep + reportFileName + " generated.");
        TestLogger.debug("Test log file (plain): " + TestLogger.getLogFileName());
    }

    /**
	 * Creates the neccessary environment for this test case
	 * 
	 * @param newReportDirName
	 *            directory name of test report
	 */
    private void createReportEnvironment(String newReportDirName, String newReportFileName) {
        reportDirName = newReportDirName;
        reportFileName = newReportFileName;
        File reportDir = new File(newReportDirName);
        if (!reportDir.exists()) {
            reportDir.mkdir();
        }
        IPath pluginDir = Activator.getPluginDir();
        String path = pluginDir + internalPath;
        File internalTRLogo = new File(path + testReportLogo);
        File externalTRLogo = new File(newReportDirName + testReportLogo);
        copy(internalTRLogo, externalTRLogo);
        File internalTRDTD = new File(path + testReportDTD);
        File externalTRDTD = new File(newReportDirName + testReportDTD);
        copy(internalTRDTD, externalTRDTD);
        File internalTRXSL = new File(path + testReportXSL);
        File externalTRXSL = new File(newReportDirName + testReportXSL);
        copy(internalTRXSL, externalTRXSL);
    }

    /**
	 * Overwrites the existing destination file with the source file
	 * 
	 * @param sourceFile
	 * @param destinationFile
	 */
    private void copy(File sourceFile, File destinationFile) {
        try {
            FileChannel in = new FileInputStream(sourceFile).getChannel();
            FileChannel out = new FileOutputStream(destinationFile).getChannel();
            try {
                in.transferTo(0, in.size(), out);
                in.close();
                out.close();
            } catch (IOException e) {
                TestLogger.error(e);
            }
        } catch (FileNotFoundException e) {
            TestLogger.error(e);
        }
    }

    public String getReportFileName() {
        return reportFileName;
    }

    public String getReportDirName() {
        return reportDirName;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    /**
	 * @return valid
	 */
    public boolean isValid() {
        return valid;
    }
}
