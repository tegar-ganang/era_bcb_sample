package uk.ac.ebi.pride.tools.converter.gui.util;

import org.apache.log4j.Logger;
import uk.ac.ebi.pride.tools.converter.conversion.io.MzTabWriter;
import uk.ac.ebi.pride.tools.converter.dao.DAO;
import uk.ac.ebi.pride.tools.converter.dao.DAOFactory;
import uk.ac.ebi.pride.tools.converter.dao.handler.HandlerFactory;
import uk.ac.ebi.pride.tools.converter.gui.NavigationPanel;
import uk.ac.ebi.pride.tools.converter.gui.model.ConverterData;
import uk.ac.ebi.pride.tools.converter.gui.model.FileBean;
import uk.ac.ebi.pride.tools.converter.gui.model.GUIException;
import uk.ac.ebi.pride.tools.converter.report.io.ReportReaderDAO;
import uk.ac.ebi.pride.tools.converter.report.io.ReportWriter;
import uk.ac.ebi.pride.tools.converter.report.io.xml.utilities.ReportXMLUtilities;
import uk.ac.ebi.pride.tools.converter.utils.ConverterException;
import uk.ac.ebi.pride.tools.converter.utils.InvalidFormatException;
import uk.ac.ebi.pride.tools.merger.io.PrideXmlMerger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: rcote
 * Date: 21/10/11
 * Time: 13:34
 */
public class IOUtilities {

    private static final Logger logger = Logger.getLogger(IOUtilities.class);

    public static final String GEL_IDENTIFIER = "Gel Identifier";

    public static final String SPOT_IDENTIFIER = "Spot Identifier";

    public static final String SPOT_REGULAR_EXPRESSION = "Spot Regular Expression";

    public static final String COMPRESS = "Compress output file";

    public static final String GENERATE_QUANT_FIELDS = "Generate Quantitation Fields";

    public static String getShortSourceFilePath(String filePath) {
        int lastSep = filePath.lastIndexOf(File.separator);
        int secondLastSep = -1;
        if (lastSep < 0) {
            return filePath;
        } else {
            String tmpFilePath = filePath.substring(0, lastSep);
            secondLastSep = tmpFilePath.lastIndexOf(File.separator);
        }
        if (secondLastSep > 0) {
            return filePath.substring(secondLastSep + 1);
        } else {
            return filePath;
        }
    }

    public static String getFileNameWithoutExtension(File file) {
        String retval = null;
        if (file != null) {
            if (file.getName().lastIndexOf(".") > 0) {
                retval = file.getName().substring(0, file.getName().lastIndexOf("."));
            }
        }
        return retval;
    }

    public static boolean renameFile(String fromFile, String toFile) {
        try {
            File destinationFile = new File(toFile);
            File sourceFile = new File(fromFile);
            deleteFiles(toFile);
            if (sourceFile.renameTo(destinationFile)) {
                logger.info("Successfully renamed temporary file to final path: " + toFile);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new ConverterException("Error renaming file", e);
        }
    }

    private static void deleteFiles(String file) {
        logger.warn("Deleting file: " + file);
        File fileToDelete = new File(file);
        if (fileToDelete.exists()) {
            try {
                int nbTries = 5;
                while (nbTries > 0 && !fileToDelete.delete()) {
                    System.gc();
                    Thread.sleep(2000);
                    nbTries--;
                }
                if (nbTries == 0) {
                    throw new ConverterException("Could not delete file: " + file);
                }
            } catch (InterruptedException e) {
            }
        }
    }

    public static void deleteFiles(Set<String> filesToDelete) {
        for (String file : filesToDelete) {
            deleteFiles(file);
        }
    }

    public static void generateMzTabFiles(Properties options, Collection<File> inputFiles) throws GUIException {
        NavigationPanel.getInstance().setWorkingMessage("Creating mzTab files.");
        ConverterData.getInstance().setOptions(options);
        Properties localOptions = new Properties();
        for (Map.Entry entry : options.entrySet()) {
            localOptions.put(entry.getKey(), entry.getValue());
        }
        String gelId = null, spotId = null;
        Pattern spotPattern = null;
        int quantFieldsToGenerate = 0;
        if (options.getProperty(GEL_IDENTIFIER) != null && !"".equals(options.getProperty(GEL_IDENTIFIER))) {
            gelId = options.getProperty(GEL_IDENTIFIER);
            localOptions.remove(GEL_IDENTIFIER);
        }
        if (options.getProperty(SPOT_IDENTIFIER) != null && !"".equals(options.getProperty(SPOT_IDENTIFIER))) {
            spotId = options.getProperty(SPOT_IDENTIFIER);
            localOptions.remove(SPOT_IDENTIFIER);
        }
        if (options.getProperty(SPOT_REGULAR_EXPRESSION) != null && !"".equals(options.getProperty(SPOT_REGULAR_EXPRESSION))) {
            String regex = options.getProperty(SPOT_REGULAR_EXPRESSION);
            spotPattern = Pattern.compile(regex);
            localOptions.remove(SPOT_REGULAR_EXPRESSION);
        }
        if (options.getProperty(GENERATE_QUANT_FIELDS) != null && !"".equals(options.getProperty(GENERATE_QUANT_FIELDS))) {
            String quantStr = options.getProperty(GENERATE_QUANT_FIELDS);
            try {
                quantFieldsToGenerate = Integer.parseInt(quantStr);
            } catch (NumberFormatException e) {
                logger.error("invalid number passed to generate quantitation fields, ignoring!");
                quantFieldsToGenerate = 0;
            }
            localOptions.remove(GENERATE_QUANT_FIELDS);
        }
        for (File file : inputFiles) {
            final String absolutePath = file.getAbsolutePath();
            try {
                logger.warn("Reading = " + absolutePath);
                NavigationPanel.getInstance().setWorkingMessage("Creating mzTab file for " + absolutePath);
                FileBean fileBean = new FileBean(absolutePath);
                DAO dao = DAOFactory.getInstance().getDAO(absolutePath, ConverterData.getInstance().getDaoFormat());
                dao.setConfiguration(localOptions);
                MzTabWriter writer;
                if (spotPattern != null) writer = new MzTabWriter(dao, quantFieldsToGenerate, gelId, spotPattern); else writer = new MzTabWriter(dao, quantFieldsToGenerate, gelId, spotId);
                String tabFile = file.getAbsolutePath() + ConverterData.MZTAB;
                writer.writeMzTabFile(new File(tabFile));
                fileBean.setMzTabFile(tabFile);
                ConverterData.getInstance().getDataFiles().add(fileBean);
            } catch (Exception e) {
                logger.fatal("Error in Generating MzTAB Files for input file " + absolutePath + ", error is " + e.getMessage(), e);
                GUIException gex = new GUIException(e);
                gex.setShortMessage("Error in Generating MzTAB Files for input file " + absolutePath);
                gex.setDetailedMessage(null);
                gex.setComponent(IOUtilities.class.getName());
                throw gex;
            }
        }
    }

    public static void generateReportFiles(Properties options, Collection<FileBean> dataFiles, boolean forceRegeneration, boolean automaticallyMapPreferredPTMs) throws GUIException {
        ConverterData.getInstance().setOptions(options);
        for (FileBean fileBean : dataFiles) {
            try {
                String reportFile = fileBean.getInputFile() + ConverterData.REPORT_XML;
                if (forceRegeneration) {
                    generateReportFile(fileBean, options, automaticallyMapPreferredPTMs);
                } else {
                    NavigationPanel.getInstance().setWorkingMessage("Attemping to load existing report file: " + reportFile);
                    File repFile = new File(reportFile);
                    if (!repFile.exists() || !ReportXMLUtilities.isUnmodifiedSourceForReportFile(repFile, fileBean.getInputFile())) {
                        logger.warn("Source file modified since report generation, will recreate report file");
                        generateReportFile(fileBean, options, automaticallyMapPreferredPTMs);
                    }
                }
                fileBean.setReportFile(reportFile);
                ConverterData.getInstance().getDataFiles().add(fileBean);
                ReportReaderDAO reportReaderDAO = new ReportReaderDAO(new File(reportFile));
                ConverterData.getInstance().getPTMs().addAll(reportReaderDAO.getPTMs());
                ConverterData.getInstance().getDatabaseMappings().addAll(reportReaderDAO.getDatabaseMappings());
            } catch (ConverterException e) {
                logger.fatal("Error in generating Report Files for input file " + fileBean.getInputFile() + ", error is " + e.getMessage(), e);
                GUIException gex = new GUIException(e);
                gex.setShortMessage("Error in generating Report Files for input file " + fileBean.getInputFile());
                gex.setDetailedMessage(null);
                gex.setComponent(IOUtilities.class.getName());
                throw gex;
            } catch (InvalidFormatException e) {
                logger.fatal("Invalid file format for input file " + fileBean.getInputFile() + ", error is " + e.getMessage(), e);
                GUIException gex = new GUIException(e);
                gex.setShortMessage("Invalid file format for input file " + fileBean.getInputFile() + "\nPlease select a properly formatted file and try again.");
                gex.setDetailedMessage(null);
                gex.setComponent(IOUtilities.class.getName());
                throw gex;
            }
        }
        try {
            if (ConverterData.getInstance().getDataFiles().size() > 1) {
                File reportFile = createMasterReportFile(ConverterData.getInstance().getDataFiles().iterator().next().getReportFile());
                FileBean masterFileBean = new FileBean("MASTER_FILE");
                masterFileBean.setReportFile(reportFile.getAbsolutePath());
                ConverterData.getInstance().setMasterFile(masterFileBean);
                ReportReaderDAO dao = new ReportReaderDAO(reportFile);
                dao.getSourceFile().setPathToFile("MASTER FILE");
                dao.getSourceFile().setNameOfFile("MASTER FILE");
                ConverterData.getInstance().setMasterDAO(dao);
            } else {
                FileBean masterFile = ConverterData.getInstance().getDataFiles().iterator().next();
                ConverterData.getInstance().setMasterFile(masterFile);
                ReportReaderDAO dao = new ReportReaderDAO(new File(masterFile.getReportFile()));
                ConverterData.getInstance().setMasterDAO(dao);
            }
        } catch (IOException e) {
            logger.fatal("Error creating master file for editing, error is " + e.getMessage(), e);
            GUIException gex = new GUIException(e);
            gex.setShortMessage("Error creating master file for editing!");
            gex.setDetailedMessage(null);
            gex.setComponent(IOUtilities.class.getName());
            throw gex;
        }
    }

    /**
     * This method will make a copy of a report file for temporary usage. This report file will be a temp
     * file that is deleted on exit
     *
     * @param reportFilePath - the path of the report file to copy
     * @return a File handle on the temporary report file
     */
    private static File createMasterReportFile(String reportFilePath) throws IOException {
        File reportFile = new File(reportFilePath);
        File masterFile = File.createTempFile("master_report.", ".xml", reportFile.getParentFile());
        masterFile.deleteOnExit();
        copyFile(reportFile, masterFile);
        return masterFile;
    }

    private static void generateReportFile(FileBean fileBean, Properties options, boolean automaticallyMapPreferredPTMs) throws InvalidFormatException {
        String reportFile = fileBean.getInputFile() + ConverterData.REPORT_XML;
        NavigationPanel.getInstance().setWorkingMessage("Creating report file for " + fileBean.getInputFile());
        logger.warn("Reading = " + fileBean.getInputFile());
        DAO dao = DAOFactory.getInstance().getDAO(fileBean.getInputFile(), ConverterData.getInstance().getDaoFormat());
        dao.setConfiguration(options);
        if (fileBean.getSpectrumFile() != null) {
            dao.setExternalSpectrumFile(fileBean.getSpectrumFile());
        }
        ReportWriter writer = new ReportWriter(reportFile);
        writer.setDAO(dao);
        writer.setAutomaticallyMapPreferredPTMs(automaticallyMapPreferredPTMs);
        if (fileBean.getSequenceFile() != null) {
            writer.setFastaHandler(HandlerFactory.getInstance().getFastaHandler(fileBean.getSequenceFile(), ConverterData.getInstance().getFastaFormat()));
        }
        if (fileBean.getMzTabFile() != null) {
            writer.setExternalHandler(HandlerFactory.getInstance().getDefaultExternalHanlder(fileBean.getMzTabFile()));
        }
        logger.warn("Writing = " + reportFile);
        writer.writeReport();
    }

    /**
     * Merge all PRIDE XML files into a single one. The first file of the list will be the master file.
     *
     * @param options    - DAO options
     * @param inputFiles - a list of PRIDE XML file paths. The first file of the list will be the master file.
     * @throws GUIException - on error
     */
    public static void mergePrideXMLFiles(Properties options, List<String> inputFiles) throws GUIException {
        try {
            boolean compress = false;
            if (options.getProperty(COMPRESS) != null && !"".equals(options.getProperty(COMPRESS))) {
                compress = Boolean.valueOf(options.getProperty(COMPRESS));
            }
            String outputFilePath = inputFiles.get(0) + ConverterData.MERGED_XML;
            PrideXmlMerger merger = new PrideXmlMerger(inputFiles, outputFilePath, compress, true);
            outputFilePath = merger.mergeXml();
            ConverterData.getInstance().setMergedOutputFile(outputFilePath);
        } catch (ConverterException e) {
            logger.fatal("Error in merging XML: " + e.getMessage(), e);
            GUIException gex = new GUIException(e);
            gex.setShortMessage("Error in merging XML files");
            gex.setDetailedMessage(null);
            gex.setComponent(IOUtilities.class.getName());
            throw gex;
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }
}
