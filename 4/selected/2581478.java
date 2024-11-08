package com.qspin.qtaste.ui.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import com.qspin.qtaste.config.StaticConfiguration;
import com.qspin.qtaste.util.FileUtilities;
import com.qspin.qtaste.util.Log4jLoggerFactory;

/**
 *
 * @author vdubois
 */
public class TestScriptCreation {

    private static Logger logger = Log4jLoggerFactory.getLogger(TestScriptCreation.class);

    private String TEMPLATE_DIR = StaticConfiguration.CONFIG_DIRECTORY + "/templates/TestScript";

    private String mTestName, mTestSuiteDir;

    public TestScriptCreation(String testName, String testSuiteDir) {
        mTestName = testName;
        mTestSuiteDir = testSuiteDir;
    }

    public void copyTestSuite(String sourceTestDir) {
        String sourceFileName = sourceTestDir + File.separator + StaticConfiguration.TEST_SCRIPT_FILENAME;
        String destFileName = mTestSuiteDir + File.separator + mTestName + File.separator + StaticConfiguration.TEST_SCRIPT_FILENAME;
        File destFile = new File(destFileName);
        if (destFile.exists()) {
            if (JOptionPane.showConfirmDialog(null, "Test " + mTestName + " already exists. Do you want to overwrite the already existing test?", "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) return;
        }
        FileUtilities.copy(sourceFileName, destFileName);
        copyTestData(sourceTestDir, mTestSuiteDir + File.separator + mTestName);
    }

    public void createTestSuite() {
        BufferedWriter output = null;
        String outputFileName = mTestSuiteDir + File.separator + mTestName + File.separator + StaticConfiguration.TEST_SCRIPT_FILENAME;
        File destFile = new File(outputFileName);
        if (destFile.exists()) {
            if (JOptionPane.showConfirmDialog(null, "Test " + mTestName + " already exists. Do you want to overwrite the already existing test?", "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.NO_OPTION) return;
        }
        try {
            String templateFile = TEMPLATE_DIR + File.separator + StaticConfiguration.TEST_SCRIPT_FILENAME;
            String strContents = getTemplateContent(templateFile);
            strContents = strContents.replace("[$TEST_NAME]", mTestName);
            File outputFile = new File(outputFileName);
            outputFile.getParentFile().mkdirs();
            output = new BufferedWriter(new FileWriter(outputFile));
            output.append(strContents);
            output.close();
            copyTestData(TEMPLATE_DIR, mTestSuiteDir + File.separator + mTestName);
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException ex) {
                    logger.error("Error during creation of TestScript: " + ex.getMessage());
                }
            } else {
                logger.error("Error during creation of TestScript");
            }
        }
    }

    private void copyTestData(String sourceDir, String DestDir) {
        String testDataFileNameWithoutExtension = StaticConfiguration.TEST_DATA_FILENAME.substring(0, StaticConfiguration.TEST_DATA_FILENAME.lastIndexOf('.'));
        String extension = ".csv";
        String testDataSourceFileName = sourceDir + File.separator + testDataFileNameWithoutExtension + extension;
        String testDataDestFileName = DestDir + File.separator + testDataFileNameWithoutExtension + extension;
        FileUtilities.copy(testDataSourceFileName, testDataDestFileName);
        extension = ".xls";
        testDataSourceFileName = sourceDir + File.separator + testDataFileNameWithoutExtension + extension;
        if (new File(testDataSourceFileName).exists()) {
            testDataDestFileName = DestDir + File.separator + testDataFileNameWithoutExtension + extension;
            FileUtilities.copy(testDataSourceFileName, testDataDestFileName);
        }
    }

    private String getTemplateContent(String templateName) throws FileNotFoundException, IOException {
        BufferedReader input = null;
        StringBuilder contents = new StringBuilder();
        input = new BufferedReader(new FileReader(new File(templateName)));
        String line = null;
        while ((line = input.readLine()) != null) {
            contents.append(line);
            contents.append(System.getProperty("line.separator"));
        }
        return contents.toString();
    }
}
