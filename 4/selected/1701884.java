package eu.planets_project.ifr.core.services.characterisation.extractor.impl;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistry;
import eu.planets_project.ifr.core.techreg.formats.FormatRegistryFactory;
import eu.planets_project.services.datatypes.DigitalObject;
import eu.planets_project.services.datatypes.Parameter;
import eu.planets_project.services.datatypes.ServiceReport;
import eu.planets_project.services.datatypes.ServiceReport.Status;
import eu.planets_project.services.datatypes.ServiceReport.Type;
import eu.planets_project.services.utils.DigitalObjectUtils;
import eu.planets_project.services.utils.FileUtils;
import eu.planets_project.services.utils.ProcessRunner;

/**
 *
 */
public class CoreExtractor {

    private static String XCLTOOLS_HOME = (System.getenv("XCLTOOLS_HOME") + File.separator);

    private static String EXTRACTOR_HOME = (XCLTOOLS_HOME + File.separator + "extractor" + File.separator).replace(File.separator + File.separator, File.separator);

    private static final String EXTRACTOR_TOOL = "extractor";

    private String extractorWork = null;

    private static String EXTRACTOR_IN = "INPUT";

    private static String EXTRACTOR_OUT = "OUTPUT";

    private String defaultInputFileName = "xcdlMigrateInput.bin";

    private String outputFileName;

    private String thisExtractorName;

    private Log plogger;

    private static String NO_NORM_DATA_FLAG = "disableNormDataInXCDL";

    private static String RAW_DATA_FLAG = "enableRawDataInXCDL";

    private static String OPTIONAL_XCEL_PARAM = "optionalXCELString";

    public static final FormatRegistry format = FormatRegistryFactory.getFormatRegistry();

    /**
     * @param extractorName
     * @param logger
     */
    public CoreExtractor(String extractorName, Log logger) {
        this.plogger = logger;
        thisExtractorName = extractorName;
        extractorWork = extractorName.toUpperCase();
    }

    public static List<URI> getSupportedInputFormats() {
        List<URI> inputFormats = new ArrayList<URI>();
        inputFormats.add(format.createExtensionUri("JPEG"));
        inputFormats.add(format.createExtensionUri("JPG"));
        inputFormats.add(format.createExtensionUri("TIFF"));
        inputFormats.add(format.createExtensionUri("TIF"));
        inputFormats.add(format.createExtensionUri("GIF"));
        inputFormats.add(format.createExtensionUri("PNG"));
        inputFormats.add(format.createExtensionUri("BMP"));
        inputFormats.add(format.createExtensionUri("PDF"));
        return inputFormats;
    }

    public static List<URI> getSupportedOutputFormats() {
        List<URI> outputFormats = new ArrayList<URI>();
        outputFormats.add(format.createExtensionUri("XCDL"));
        return outputFormats;
    }

    /**
     * @param input
     * @param inputFormat TODO
     * @param xcelFile
     * @param parameters
     * @return The resulting XCDL file created by the Extractor, or null if no file was written
     */
    public File extractXCDL(DigitalObject input, URI inputFormat, File xcelFile, List<Parameter> parameters) {
        if (EXTRACTOR_HOME == null) {
            System.err.println("EXTRACTOR_HOME is not set! Please create an system variable\n" + "and point it to the Extractor installation folder!");
            plogger.error("EXTRACTOR_HOME is not set! Please create an system variable\n" + "and point it to the Extractor installation folder!");
        }
        plogger.info("Starting " + thisExtractorName + " Service...");
        List<String> extractor_arguments = null;
        File extractor_work_folder = null;
        File extractor_in_folder = null;
        File extractor_out_folder = null;
        extractor_work_folder = FileUtils.createFolderInWorkFolder(FileUtils.getPlanetsTmpStoreFolder(), extractorWork);
        extractor_in_folder = FileUtils.createFolderInWorkFolder(extractor_work_folder, EXTRACTOR_IN);
        extractor_out_folder = FileUtils.createFolderInWorkFolder(extractor_work_folder, EXTRACTOR_OUT);
        String inputFileName = DigitalObjectUtils.getFileNameFromDigObject(input, inputFormat);
        if (inputFileName == null || inputFileName.equalsIgnoreCase("")) {
            inputFileName = FileUtils.randomizeFileName(defaultInputFileName);
        } else {
            inputFileName = FileUtils.randomizeFileName(inputFileName);
        }
        outputFileName = getOutputFileName(inputFileName, format.createExtensionUri("xcdl"));
        File srcFile = new File(extractor_in_folder, inputFileName);
        FileUtils.writeInputStreamToFile(input.getContent().read(), srcFile);
        ProcessRunner shell = new ProcessRunner();
        plogger.info("EXTRACTOR_HOME = " + EXTRACTOR_HOME);
        plogger.info("Configuring Commandline");
        extractor_arguments = new ArrayList<String>();
        extractor_arguments.add(EXTRACTOR_HOME + EXTRACTOR_TOOL);
        String srcFilePath = srcFile.getAbsolutePath().replace('\\', '/');
        plogger.info("Input-Image file path: " + srcFilePath);
        extractor_arguments.add(srcFilePath);
        String outputFilePath = extractor_out_folder.getAbsolutePath() + File.separator + outputFileName;
        outputFilePath = outputFilePath.replace('\\', '/');
        if (xcelFile != null) {
            String xcelFilePath = xcelFile.getAbsolutePath().replace('\\', '/');
            plogger.info("Input-XCEL file path: " + xcelFilePath);
            extractor_arguments.add(xcelFilePath);
            extractor_arguments.add(outputFilePath);
        } else {
            extractor_arguments.add("-o");
            extractor_arguments.add(outputFilePath);
        }
        if (parameters != null) {
            if (parameters.size() != 0) {
                plogger.info("Got additional parameters: ");
                for (Iterator<Parameter> iterator = parameters.iterator(); iterator.hasNext(); ) {
                    Parameter parameter = (Parameter) iterator.next();
                    String name = parameter.getName();
                    if (name.equalsIgnoreCase(OPTIONAL_XCEL_PARAM)) {
                        plogger.info("Optional XCEL passed! Using specified XCEL.");
                        continue;
                    }
                    if (name.equalsIgnoreCase(RAW_DATA_FLAG)) {
                        plogger.info("Got Parameter: " + name + " = " + parameter.getValue());
                        plogger.info("Configuring Extractor to write RAW data!");
                        extractor_arguments.add(parameter.getValue());
                        continue;
                    } else if (name.equalsIgnoreCase(NO_NORM_DATA_FLAG)) {
                        plogger.info("Got Parameter: " + name + " = " + parameter.getValue());
                        plogger.info("Configuring Extractor to skip NormData!");
                        extractor_arguments.add(parameter.getValue());
                        continue;
                    } else {
                        plogger.warn("Invalid parameter: " + name + " = '" + parameter.getValue() + "'. Ignoring parameter...!");
                        continue;
                    }
                }
            }
        }
        String line = "";
        for (String argument : extractor_arguments) {
            line = line + argument + " ";
        }
        plogger.info("Setting command to: " + line);
        shell.setCommand(extractor_arguments);
        shell.setStartingDir(new File(EXTRACTOR_HOME));
        plogger.info("Setting starting Dir to: " + EXTRACTOR_HOME);
        plogger.info("Starting Extractor tool...");
        shell.run();
        String processOutput = shell.getProcessOutputAsString();
        String processError = shell.getProcessErrorAsString();
        plogger.info("Process Output: " + processOutput);
        System.out.println("Process Output: " + processOutput);
        if (!"".equals(processError)) {
            plogger.error("Process Error: " + processError);
            System.err.println("Process Error: " + processError);
        }
        plogger.info("Creating File to return...");
        File resultXCDL = new File(outputFilePath);
        if (!resultXCDL.exists()) {
            plogger.error("File doesn't exist: " + resultXCDL.getAbsolutePath());
            return null;
        }
        return resultXCDL;
    }

    private String getOutputFileName(String inputFileName, URI outputFormat) {
        String fileName = null;
        String outputExt = format.getFirstExtension(outputFormat);
        if (inputFileName.contains(".")) {
            fileName = inputFileName.substring(0, inputFileName.lastIndexOf(".")) + "." + outputExt;
        } else {
            fileName = inputFileName + "." + outputExt;
        }
        return fileName;
    }

    /**
     * @param inputFormat The format
     * @return A service report indicating the format is not supported
     */
    public static ServiceReport unsupportedInputFormatReport(URI inputFormat) {
        return new ServiceReport(Type.ERROR, Status.TOOL_ERROR, "Unsupported input format: " + inputFormat);
    }

    /**
     * @param format The format
     * @param parameters The parameters
     * @return True, if either the extractor provides an XCEL for the format, or
     *         the XCEL has been given in the parameters
     */
    public static boolean supported(URI format, List<Parameter> parameters) {
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                if (parameter.getName().toLowerCase().contains("xcel")) {
                    return true;
                }
            }
        }
        if (format == null) {
            return false;
        }
        List<URI> aliases = FormatRegistryFactory.getFormatRegistry().getFormatUriAliases(format);
        List<URI> supported = getSupportedInputFormats();
        for (URI a : aliases) {
            for (URI s : supported) {
                if (a.equals(s)) {
                    return true;
                }
            }
        }
        return false;
    }
}
