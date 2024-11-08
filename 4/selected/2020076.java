package com.smartwish.documentburster.job;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import com.smartwish.documentburster.engine.pdf.Merger;
import com.smartwish.documentburster.engine.pdf.PdfBurster;
import com.smartwish.documentburster.other.Helpers;
import com.smartwish.documentburster.scripting.Scripts;
import com.smartwish.documentburster.settings.Settings;
import com.smartwish.documentburster.utils.Utils;
import com.smartwish.documentburster.variables.Variables;

public class CliJobTest extends TestCase {

    private CliJob job;

    public final void testDoBurstNormalFlow() throws Exception {
        executeFacadeBurstFlow(false);
        File jobFile = new File(job.getJobPath());
        assertFalse(jobFile.exists());
    }

    public final void testDoBurstThrowIOExceptionFlow() {
        try {
            executeFacadeBurstFlow(true);
        } catch (Exception e) {
            File jobFile = new File(job.getJobPath());
            assertFalse(jobFile.exists());
        }
    }

    private void executeFacadeBurstFlow(final boolean throwException) throws Exception {
        FileUtils.forceMkdir(new File(Helpers.testOutputTempFolder));
        job = new CliJob(new Settings()) {

            protected PdfBurster getBurster(String filePath) throws Exception {
                return new PdfBurster() {

                    public void burst(String fileToExplodePath) throws IOException {
                        if (throwException) {
                            throw new IOException();
                        }
                    }
                };
            }

            protected String getJobFilePath(String filePath, String jobType) {
                return Helpers.testOutputTempFolder + "/" + Utils.getJobFileName(filePath, "burst");
            }
        };
        job.doBurst(Helpers.testPDFInputBurstDocumentPath);
    }

    public final void testDoMergeNormalFlow() throws Exception {
        executeFacadeMergeFlow(false);
        File jobFile = new File(job.getJobPath());
        assertFalse(jobFile.exists());
    }

    public final void testDoMergeThrowIOExceptionFlow() {
        try {
            executeFacadeMergeFlow(true);
        } catch (Exception e) {
            File jobFile = new File(job.getJobPath());
            assertFalse(jobFile.exists());
        }
    }

    private void executeFacadeMergeFlow(final boolean throwException) throws Exception {
        final Settings settings = new Settings() {

            public String getOutputFolder() {
                return Helpers.testOutputOutputFolder;
            }
        };
        settings.loadSettings(Helpers.testConfigFile);
        job = new CliJob(settings) {

            protected Merger getMerger() throws IOException {
                return new Merger(settings) {

                    public String doMerge(String[] filePaths, String outputFileName) throws IOException {
                        if (throwException) throw new IOException();
                        return "";
                    }
                };
            }

            protected String getJobFilePath(String filePath, String jobType) {
                return Helpers.testOutputTempFolder + "/" + Utils.getJobFileName(filePath, "burst");
            }
        };
        job.doMerge(null, null);
    }

    public final void testDoPoll() throws Exception {
        File inputFile = new File(Helpers.testPDFInputBurstDocumentPath);
        File watchedDir = new File(Helpers.testOutputPollFolder);
        FileUtils.copyFileToDirectory(inputFile, watchedDir);
        Settings settings = new Settings() {

            public String getOutputFolder() {
                return Helpers.testOutputOutputFolder;
            }

            ;

            public String getPollFolder() {
                return Helpers.testOutputPollFolder;
            }

            ;

            public String getBackupFolder() {
                return Helpers.testOutputBackupFolder;
            }

            ;
        };
        settings.loadSettings(Helpers.testConfigFile);
        job = new CliJob(settings) {

            protected String getPollPidFilePath() {
                return Helpers.testOutputTempFolder + "/poll.pid";
            }

            ;

            protected String getJobFilePath(String filePath, String jobType) {
                return Helpers.testOutputTempFolder + "/" + Utils.getJobFileName(filePath, "burst");
            }

            ;

            protected PdfBurster getBurster(String filePath) throws Exception {
                return new PdfBurster() {

                    protected void executeController() throws Exception {
                        scripting.setRoots(new String[] { Helpers.testScriptsFolder });
                        scripting.executeScript(Scripts.CONTROLLER, this.ctx);
                        ctx.variables = new Variables("burst.pdf", ctx.settings.getLanguage(), ctx.settings.getCountry(), ctx.settings.getNumberOfUserVariables());
                    }

                    ;
                };
            }
        };
        Thread pollPidChecker = new Thread() {

            public void run() {
                try {
                    Thread.sleep(10000);
                    File pollPidFile = new File(Helpers.testOutputTempFolder + "/poll.pid");
                    pollPidFile.delete();
                } catch (InterruptedException e) {
                }
            }
        };
        pollPidChecker.start();
        job.doPoll(null);
        assertTrue(true);
    }

    ;
}
