package com.genia.toolbox.projects.csv.business.manager.impl;

import java.io.File;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.genia.toolbox.basics.exception.BundledException;
import com.genia.toolbox.projects.csv.bean.CsvDataRow;
import com.genia.toolbox.projects.csv.bean.CsvResultList;
import com.genia.toolbox.projects.csv.bean.CsvVersion;
import com.genia.toolbox.projects.csv.bean.FolderDescriptor;
import com.genia.toolbox.projects.csv.bean.MappingDescriptor;
import com.genia.toolbox.projects.csv.business.manager.CsvBackgroundProcess;
import com.genia.toolbox.projects.csv.business.manager.CsvImportManager;
import com.genia.toolbox.projects.csv.business.manager.CsvVersionManager;
import com.genia.toolbox.projects.csv.business.util.CsvUtils;
import com.genia.toolbox.spring.provider.message.manager.CustomResourceBundleMessageSource;

/**
 * the implementation class of {@link CsvImportManager}.
 */
public class CsvImportManagerImpl implements CsvImportManager {

    /**
   * the logger.
   */
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvImportManagerImpl.class);

    /**
   * the name of the readme file.
   */
    private static final String README_FILENAME = "readme.txt";

    /**
   * reference to the {@link CsvBackgroundProcess}.
   */
    private CsvBackgroundProcess csvBackgroundProcess;

    /**
   * reference to the {@link CsvUtils}.
   */
    private CsvUtils csvUtils;

    /**
   * reference to the {@link CsvVersionManager}.
   */
    private CsvVersionManager csvVersionManager;

    /**
   * reference to the {@link CustomResourceBundleMessageSource}.
   */
    private CustomResourceBundleMessageSource messageSource;

    /**
   * generate empty csv files with column headers.
   * 
   * @param mappingDescriptor
   *          the {@link MappingDescriptor} that describes the mapping
   * @param basePath
   *          the {@link File} representation of the path to the base folder
   * @return the {@link CsvVersion} of generated files
   * @throws BundledException
   *           if an error occur
   */
    public CsvVersion generateEmptyCsvFiles(MappingDescriptor mappingDescriptor, File basePath) throws BundledException {
        try {
            LOGGER.info("Starting the CSV files generation .....");
            CsvVersion newVersion = getCsvVersionManager().getNewVersion(mappingDescriptor, basePath);
            String workingFileName = getCsvVersionManager().format(newVersion);
            LOGGER.info("Version of file to process : " + workingFileName);
            for (FolderDescriptor folder : mappingDescriptor.getFolders()) {
                LOGGER.info("Processing the folder named " + folder.getFolderName() + " ......");
                List<String> csvColumns = getCsvBackgroundProcess().getColumnNames(folder);
                List<String> csvComments = getCsvBackgroundProcess().getNeededComments(folder);
                final File folderPath = new File(basePath, folder.getFolderName());
                folderPath.mkdirs();
                File pathToFile = new File(folderPath, workingFileName);
                if (csvComments.isEmpty()) {
                    getCsvUtils().writeCsvHeaders(pathToFile, csvColumns);
                } else {
                    getCsvUtils().writeCsvHeaders(pathToFile, csvColumns, csvComments);
                }
                LOGGER.info("Folder named " + folder.getFolderName() + " processed successfully !");
            }
            LOGGER.info("Writing the readme file ....");
            List<String> readmeTexts = getCsvBackgroundProcess().getReadmeDependencies(mappingDescriptor);
            getCsvUtils().writeReadmeTexts(new File(basePath, README_FILENAME), readmeTexts);
            LOGGER.info("CSV files generation finished successfully !");
            return newVersion;
        } catch (BundledException error) {
            LOGGER.error(getMessageSource().getMessage(error.getI18nMessage(), Locale.getDefault()), error);
            throw error;
        }
    }

    /**
   * getter for the csvBackgroundProcess property.
   * 
   * @return the csvBackgroundProcess
   */
    public CsvBackgroundProcess getCsvBackgroundProcess() {
        return csvBackgroundProcess;
    }

    /**
   * getter for the csvUtils property.
   * 
   * @return the csvUtils
   */
    public CsvUtils getCsvUtils() {
        return csvUtils;
    }

    /**
   * getter for the csvVersionManager property.
   * 
   * @return the csvVersionManager
   */
    public CsvVersionManager getCsvVersionManager() {
        return csvVersionManager;
    }

    /**
   * getter for the messageSource property.
   * 
   * @return the messageSource
   */
    public CustomResourceBundleMessageSource getMessageSource() {
        return messageSource;
    }

    /**
   * import csv data for the given version.
   * 
   * @param mappingDescriptor
   *          the {@link MappingDescriptor} that describes the mapping
   * @param basePath
   *          the {@link File} representation of the path to the base folder
   * @param version
   *          the {@link CsvVersion} to consider
   * @throws BundledException
   *           if an error occur
   */
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED, rollbackFor = BundledException.class)
    public void importCsvData(MappingDescriptor mappingDescriptor, File basePath, CsvVersion version) throws BundledException {
        try {
            LOGGER.info("Starting the CSV data importation ......");
            String workingFileName = getCsvVersionManager().format(version);
            LOGGER.info("Version of file to process : " + workingFileName);
            getCsvBackgroundProcess().saveSupportedLanguages(mappingDescriptor);
            for (FolderDescriptor folder : mappingDescriptor.getFolders()) {
                LOGGER.info("Processing the folder named " + folder.getFolderName() + " ......");
                File folderPath = new File(basePath, folder.getFolderName());
                CsvResultList csvResults = getCsvUtils().readCsvData(folder, new File(folderPath, workingFileName));
                if (!csvResults.getIgnoredDataRows().isEmpty()) {
                    LOGGER.warn("The following CSV data will be ignored because they are not conform to the csv column headers :");
                    this.logIgnoredCsvData(csvResults.getIgnoredDataRows());
                }
                getCsvBackgroundProcess().importCsvDataRows(folderPath, folder, csvResults.getAcceptedDataRows());
                LOGGER.info("Folder named " + folder.getFolderName() + " processed successfully !");
            }
            LOGGER.info("CSV data importation finished successfully !");
        } catch (BundledException error) {
            LOGGER.error(getMessageSource().getMessage(error.getI18nMessage(), Locale.getDefault()));
            throw error;
        }
    }

    /**
   * automatically choose the latest csv version and import data.
   * 
   * @param mappingDescriptor
   *          the {@link MappingDescriptor} that describes the mapping
   * @param basePath
   *          the {@link File} representation of the path to the base folder
   * @throws BundledException
   *           if an error occur
   */
    public void importLastCsvDataVersion(MappingDescriptor mappingDescriptor, File basePath) throws BundledException {
        CsvVersion lastVersion = getCsvVersionManager().getLastVersion(mappingDescriptor, basePath);
        importCsvData(mappingDescriptor, basePath, lastVersion);
    }

    /**
   * list exsting versions for the given base path.
   * 
   * @param mappingDescriptor
   *          the {@link MappingDescriptor} that describes the mapping
   * @param basePath
   *          the {@link File} representation of the path to the base folder
   * @return a {@link List} of {@link CsvVersion}s
   * @throws BundledException
   *           if an error occur
   */
    public List<CsvVersion> listVersions(MappingDescriptor mappingDescriptor, File basePath) throws BundledException {
        return getCsvVersionManager().getVersions(mappingDescriptor, basePath);
    }

    /**
   * setter for the csvBackgroundProcess property.
   * 
   * @param csvBackgroundProcess
   *          the csvBackgroundProcess to set
   */
    public void setCsvBackgroundProcess(CsvBackgroundProcess csvBackgroundProcess) {
        this.csvBackgroundProcess = csvBackgroundProcess;
    }

    /**
   * setter for the csvUtils property.
   * 
   * @param csvUtils
   *          the csvUtils to set
   */
    public void setCsvUtils(CsvUtils csvUtils) {
        this.csvUtils = csvUtils;
    }

    /**
   * setter for the csvVersionManager property.
   * 
   * @param csvVersionManager
   *          the csvVersionManager to set
   */
    public void setCsvVersionManager(CsvVersionManager csvVersionManager) {
        this.csvVersionManager = csvVersionManager;
    }

    /**
   * setter for the messageSource property.
   * 
   * @param messageSource
   *          the messageSource to set
   */
    public void setMessageSource(CustomResourceBundleMessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
   * write to the log the list of ignored data row.
   * 
   * @param ignoredDataRows
   *          the {@link List} of {@link CsvDataRow} to describe
   */
    private void logIgnoredCsvData(List<CsvDataRow> ignoredDataRows) {
        for (CsvDataRow dataRow : ignoredDataRows) {
            LOGGER.warn("at line " + dataRow.getLineNumber());
        }
    }
}
