package de.flingelli.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.flingelli.latex.reports.Requirements;
import de.flingelli.latex.reports.Sprints;
import de.flingelli.scrum.observer.ProductPropertyChangeSupport;

/**
 * 
 * @author Markus Flingelli
 * 
 */
public final class PdfLaTeX {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfLaTeX.class);

    private PdfLaTeX() {
    }

    public static void generateSprintReport(final Sprints sprints, final String pdfFileName, final String pdfLaTeXFileName) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                File tempFile;
                try {
                    tempFile = File.createTempFile("sprintreport", ".tex");
                    sprints.writeToFile(tempFile.getAbsolutePath());
                    execute(pdfLaTeXFileName, tempFile.getAbsolutePath());
                    String generatedFile = tempFile.getAbsolutePath();
                    generatedFile = generatedFile.substring(0, generatedFile.length() - 4);
                    generatedFile += ".pdf";
                    File file = new File(generatedFile);
                    if (file.exists()) {
                        FileUtils.copyFile(file, new File(pdfFileName));
                        ProductPropertyChangeSupport.getInstance().sprintPdfFileGenerated(pdfFileName);
                    } else {
                        LOGGER.error("File '" + generatedFile + "' does not exist.");
                    }
                    tempFile.delete();
                } catch (IOException e) {
                    LOGGER.error("Sprint report cannot be created.", e);
                }
            }
        });
        thread.start();
    }

    public static void generateRequirementReport(final Requirements requirements, final String pdfFileName, final String pdfLaTeXFileName) {
        Thread thread = new Thread(new Runnable() {

            public void run() {
                File tempFile;
                try {
                    tempFile = File.createTempFile("requirements", ".tex");
                    requirements.writeToFile(tempFile.getAbsolutePath());
                    execute(pdfLaTeXFileName, tempFile.getAbsolutePath());
                    String generatedFile = tempFile.getAbsolutePath();
                    generatedFile = generatedFile.substring(0, generatedFile.length() - 4);
                    generatedFile += ".pdf";
                    File file = new File(generatedFile);
                    if (file.exists()) {
                        FileUtils.copyFile(file, new File(pdfFileName));
                        ProductPropertyChangeSupport.getInstance().requirementsPdfFileGenerated(pdfFileName);
                    } else {
                        LOGGER.error("File '" + generatedFile + "' does not exist.");
                    }
                    tempFile.delete();
                } catch (IOException e) {
                    LOGGER.error("Sprint report cannot be created.", e);
                }
            }
        });
        thread.start();
    }

    private static void execute(String pdflatex, String texFileName) {
        File pdf = new File(pdflatex);
        File tex = new File(texFileName);
        for (int i = 0; i < 2; i++) {
            Process p;
            try {
                ProcessBuilder builder = new ProcessBuilder("\"" + pdf.getAbsolutePath() + "\"", "\"" + tex.getAbsolutePath() + "\"");
                builder.directory(tex.getParentFile());
                p = builder.start();
                PrintWriter out = new PrintWriter(new OutputStreamWriter(p.getOutputStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                out.flush();
                String resultLine = in.readLine();
                while (resultLine != null) {
                    LOGGER.trace(resultLine);
                    resultLine = in.readLine();
                }
                p.destroy();
                p.waitFor();
            } catch (IOException e) {
                LOGGER.error(e.toString());
            } catch (InterruptedException e) {
                LOGGER.error(e.toString());
            }
        }
    }
}
