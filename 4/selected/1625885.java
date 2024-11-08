package net.mitnet.tools.pdf.book.openoffice.converter;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import net.mitnet.tools.pdf.book.io.FileHelper;
import net.mitnet.tools.pdf.book.openoffice.net.OpenOfficeConnectionFactory;
import net.mitnet.tools.pdf.book.openoffice.net.OpenOfficeServerContext;
import net.mitnet.tools.pdf.book.util.MathHelper;
import net.mitnet.tools.pdf.book.util.ProgressMonitor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeManager;

/**
 * Open Office Document Converter.
 * 
 * This class is a facade to the underlying "OpenOfficeDocumentConverter".
 * 
 * TODO - review tracing and debugging.
 * TODO - review convert document method signatures
 * 
 * @author Tim Telcik <telcik@gmail.com>
 * 
 * @see com.artofsolving.jodconverter.cli.ConvertDocument
 * @see com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter
 */
public class OpenOfficeDocConverter {

    public static final String OUTPUT_FORMAT_PDF = "pdf";

    public static final String OUTPUT_FORMAT_OPEN_DOC_TEXT = "odt";

    public static final String DEFAULT_OUTPUT_FORMAT = OUTPUT_FORMAT_PDF;

    private OpenOfficeServerContext serverContext = null;

    private boolean verboseEnabled = false;

    private boolean debugEnabled = false;

    private ProgressMonitor progressMonitor = null;

    public OpenOfficeDocConverter(OpenOfficeServerContext serverContext) throws Exception {
        this.serverContext = serverContext;
    }

    public OpenOfficeDocConverter(String openOfficeHost, int openOfficePort) throws Exception {
        this.serverContext = new OpenOfficeServerContext(openOfficeHost, openOfficePort);
    }

    public void setVerboseEnabled(boolean value) {
        this.verboseEnabled = value;
    }

    public boolean isVerboseEnabled() {
        return this.verboseEnabled;
    }

    public void setDebugEnabled(boolean value) {
        this.debugEnabled = value;
    }

    public boolean isDebugEnabled() {
        return this.debugEnabled;
    }

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    public void setProgressMonitor(ProgressMonitor progressMonitor) {
        this.progressMonitor = progressMonitor;
    }

    public void convertDocuments(File sourceDir, File outputDir, String outputFormat) throws Exception {
        if (isVerboseEnabled()) {
            verbose("sourceDir: " + sourceDir);
            verbose("outputDir: " + outputDir);
            verbose("outputFormat: " + outputFormat);
            verbose("progressMonitor: " + getProgressMonitor());
        }
        List<File> sourceFileList = FileHelper.findOpenOfficeFiles(sourceDir, true);
        if (isDebugEnabled()) {
            verbose("sourceFileList.size: " + sourceFileList.size());
            verbose("sourceFileList: " + sourceFileList);
        }
        if (!sourceFileList.isEmpty()) {
            convertDocuments(sourceDir, sourceFileList, outputDir, outputFormat);
        }
    }

    public void convertDocuments(File sourceDir, List<File> sourceFileList, File outputDir, String outputFormat) throws Exception {
        if (isVerboseEnabled()) {
            if (sourceFileList != null) {
                System.out.println("Converting " + sourceFileList.size() + " doucments to " + outputFormat + " ...");
            }
        }
        if (isDebugEnabled()) {
            verbose("sourceDir: " + sourceDir);
            verbose("sourceFileList: " + sourceFileList);
            verbose("outputDir: " + outputDir);
            verbose("outputFormat: " + outputFormat);
            verbose("converting " + sourceFileList.size() + " document(s)");
            verbose("output dir is " + outputDir);
            verbose("output format is " + outputFormat);
            verbose("connecting to OpenOffice server " + this.serverContext);
        }
        OfficeManager officeManager = new DefaultOfficeManagerConfiguration().setPortNumber(8100).buildOfficeManager();
        officeManager.start();
        try {
            OfficeDocumentConverter converter = new OfficeDocumentConverter(officeManager);
            if (isDebugEnabled()) {
                verbose("converter is " + converter);
            }
            int currentItemIndex = 0;
            int maxItemIndex = sourceFileList.size();
            for (File inputFile : sourceFileList) {
                currentItemIndex++;
                if (inputFile.isFile()) {
                    String baseOutputFilePath = null;
                    if (outputDir == null) {
                        baseOutputFilePath = inputFile.getParent();
                    } else {
                        String pathToParent = FileHelper.getPathToParent(sourceDir, inputFile);
                        if (StringUtils.isEmpty(pathToParent)) {
                            baseOutputFilePath = outputDir.getAbsolutePath();
                        } else {
                            baseOutputFilePath = new File(outputDir, pathToParent).getPath();
                        }
                    }
                    String baseInputFileName = FilenameUtils.getBaseName(inputFile.getName());
                    String outputFileName = baseInputFileName + "." + outputFormat;
                    File outputFile = new File(baseOutputFilePath, outputFileName);
                    convertDocument(converter, inputFile, outputFile);
                    if (getProgressMonitor() != null) {
                        int progressPercentage = MathHelper.calculatePercentage(currentItemIndex, maxItemIndex);
                        getProgressMonitor().setProgressPercentage(progressPercentage);
                    }
                }
            }
        } finally {
            if (isDebugEnabled()) {
                verbose("disconnecting");
            }
            officeManager.stop();
        }
    }

    public void convertDocument(File sourceFile, File outputDir, String outputFormat) throws Exception {
        List<File> sourceFiles = new ArrayList<File>();
        sourceFiles.add(sourceFile);
        File sourceDir = sourceFile.getParentFile();
        convertDocuments(sourceDir, sourceFiles, outputDir, outputFormat);
    }

    private void convertDocument(OfficeDocumentConverter converter, File inputFile, File outputFile) throws IOException {
        String inputFileExtension = FilenameUtils.getExtension(inputFile.getName());
        String outputFileExtension = FilenameUtils.getExtension(outputFile.getName());
        boolean sameExtension = false;
        if ((inputFileExtension != null) && (outputFileExtension != null)) {
            if (inputFileExtension.equals(outputFileExtension)) {
                sameExtension = true;
            }
        }
        if (sameExtension) {
            if (isVerboseEnabled()) {
                verbose("input and output file have same extension - copying " + inputFile + " to " + outputFile);
            }
            FileUtils.copyFile(inputFile, outputFile);
        } else {
            if (isVerboseEnabled()) {
                verbose("converting " + inputFile + " to " + outputFile);
            }
            converter.convert(inputFile, outputFile);
        }
    }

    private void verbose(String msg) {
        if (isVerboseEnabled()) {
            System.out.println("" + msg);
        }
    }

    private void debug(String msg) {
        if (isDebugEnabled()) {
            System.out.println("--" + msg);
        }
    }
}
