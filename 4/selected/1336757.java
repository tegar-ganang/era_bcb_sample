package net.sf.osadm.table2table.tsv2docbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import net.sf.osadm.linedata.table.ValuePolisher;
import net.sf.osadm.linedata.table.template.TableConfiguration;
import net.sf.osadm.linedata.table.template.TableConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConverterMarcel implements Converter {

    private static final String OUTPUT_ENCODING = "UTF-8";

    private static final String INPUT_ENCODING = "UTF-8";

    /** The logging instance. */
    protected Logger logger = LoggerFactory.getLogger(ConverterMarcel.class);

    private final TableConfigurationManager tableConfigManager;

    private final IdentificationScanner idScanner;

    private final ValuePolisher inputPolisher;

    private final File sourceDir;

    public ConverterMarcel(TableConfigurationManager tableConfigManager, IdentificationScanner idScanner, ValuePolisher inputPolisher, File sourceDir) {
        super();
        this.tableConfigManager = tableConfigManager;
        this.idScanner = idScanner;
        this.inputPolisher = inputPolisher;
        this.sourceDir = sourceDir;
    }

    public void execute(File tsvFile, File xmlFile) {
        BufferedReader reader = null;
        Writer writer = null;
        Boolean isFileSuccessfullyConverted = Boolean.TRUE;
        TableConfiguration tableConfig = null;
        try {
            xmlFile.getParentFile().mkdirs();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(tsvFile), INPUT_ENCODING));
            writer = new OutputStreamWriter(new FileOutputStream(xmlFile), OUTPUT_ENCODING);
            tableConfig = Tsv2DocbookConverter.convert2(tableConfigManager, idScanner.extractIdentification(tsvFile), reader, writer, inputPolisher);
            isFileSuccessfullyConverted = (tableConfig != null);
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to create reader with UTF-8 encoding: " + e.getMessage(), e);
        } catch (FileNotFoundException fnfe) {
            logger.error("Failed to open tsv input file '" + tsvFile + "'. " + fnfe.getMessage());
        } catch (Throwable cause) {
            logger.error("Failed to convert input tsv file '" + tsvFile + "'.", cause);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    logger.warn("Unable to close input file.", ioe);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ioe) {
                    logger.warn("Unable to close output file.", ioe);
                }
            }
        }
        if (isFileSuccessfullyConverted) {
            String newOutputFileName = tableConfig.getFileName(idScanner.extractIdentification(tsvFile));
            if (newOutputFileName != null) {
                File newOutputFile = new File(xmlFile.getParentFile(), newOutputFileName);
                if (!xmlFile.renameTo(newOutputFile)) {
                    logger.warn("Unable to rename '" + xmlFile + "' to '" + newOutputFile + "'.");
                    logger.info("Created successfully '" + xmlFile + "'.");
                } else {
                    logger.info("Created successfully '" + newOutputFileName + "'.");
                }
            } else {
                logger.info("Created successfully '" + xmlFile + "'.");
            }
        } else {
            logger.warn("Unable to convert input tsv file '" + Tsv2DocBookApplication.trimPath(sourceDir, tsvFile) + "' to docbook.");
            if (xmlFile.exists() && !xmlFile.delete()) {
                logger.warn("Unable to remove (empty) output file '" + xmlFile + "', which was created as target for the docbook table.");
            }
        }
    }
}
