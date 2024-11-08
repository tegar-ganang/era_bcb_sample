package uk.ac.ebi.pride.tools.converter.report.io.xml.utilities;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import uk.ac.ebi.pride.tools.converter.dao.DAO;
import uk.ac.ebi.pride.tools.converter.dao.DAOFactory;
import uk.ac.ebi.pride.tools.converter.gui.model.ConverterData;
import uk.ac.ebi.pride.tools.converter.report.io.ReportReader;
import uk.ac.ebi.pride.tools.converter.report.model.SearchResultIdentifier;
import uk.ac.ebi.pride.tools.converter.report.validator.ReportSchemaValidator;
import uk.ac.ebi.pride.tools.converter.report.validator.ReportValidationErrorHandler;
import uk.ac.ebi.pride.tools.converter.utils.InvalidFormatException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: rcote
 * Date: 12/09/11
 * Time: 14:18
 */
public class ReportXMLUtilities {

    private static Logger logger = Logger.getLogger(ReportXMLUtilities.class);

    public static boolean isValidReportFile(File repFile) {
        try {
            if (repFile != null && repFile.exists()) {
                ReportSchemaValidator validator = new ReportSchemaValidator();
                ReportValidationErrorHandler errors = validator.validate(new FileReader(repFile));
                if (errors.noErrors()) {
                    return true;
                } else {
                    logger.warn("Submitted report files has schema errors, will overwrite: " + errors.toString());
                    return false;
                }
            }
            return false;
        } catch (IOException e) {
            logger.warn("Error reading report file, will overwrite: " + e.getMessage(), e);
            return false;
        } catch (SAXException e) {
            logger.warn("Error reading report file, will overwrite: " + e.getMessage(), e);
            return false;
        }
    }

    public static boolean isUnmodifiedSourceForReportFile(File repFile, String reportSource) {
        try {
            if (repFile != null && repFile.exists()) {
                DAO dao = DAOFactory.getInstance().getDAO(reportSource, ConverterData.getInstance().getDaoFormat());
                SearchResultIdentifier originalSRI = dao.getSearchResultIdentifier();
                ReportReader reader = new ReportReader(repFile);
                SearchResultIdentifier reportSRI = reader.getSearchResultIdentifier();
                return reportSRI.getHash().equals(originalSRI.getHash());
            }
            return false;
        } catch (InvalidFormatException e) {
            logger.warn("Error reading report file, will overwrite: " + e.getMessage(), e);
            return false;
        }
    }
}
