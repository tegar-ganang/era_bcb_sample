package fr.sonictools.jgrisbicatcleaner.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import fr.sonictools.jgrisbicatcleaner.business.auto_generated.grisbi06.Grisbi;
import fr.sonictools.jgrisbicatcleaner.business.auto_generated.grisbi06.Transaction;
import fr.sonictools.jgrisbicatcleaner.tool.ApplicationLogger;
import fr.sonictools.jgrisbicatcleaner.tool.FileUtils;
import fr.sonictools.xml.JaxbXmlParser;

/**
 * Class dealing with the grisbi file. This class allows to convert grisbi file
 * into grisbi java object, and grisbi java object into grisbi file
 * 
 * @author jbenech
 * 
 */
public class GrisbiFileManager {

    public static final String BUSINESS_PACKAGE = "fr.sonictools.jgrisbicatcleaner.business.auto_generated.grisbi06";

    private String m_inputGrisbiFilePath = "";

    private String m_backupGrisbiFilePath = "";

    private Grisbi m_grisbiFileObj = null;

    public GrisbiFileManager(String pathGrisbiFile) {
        m_inputGrisbiFilePath = pathGrisbiFile;
        m_backupGrisbiFilePath = pathGrisbiFile + ".bkp";
    }

    /**
	 * Loads the requested grisbi file into the a Grisbi object
	 * 
	 * @throws JAXBException
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
    public void loadGrisbiFileObject() throws JAXBException, XMLStreamException, FileNotFoundException {
        JaxbXmlParser parser = new JaxbXmlParser(BUSINESS_PACKAGE);
        m_grisbiFileObj = (Grisbi) parser.loadObjectUsingStax(m_inputGrisbiFilePath);
    }

    /**
	 * Saves the current grisbi object into the output file specified as
	 * parameter
	 * 
	 * @param outputGrisbiFilePath
	 * @throws JAXBException
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 */
    public void saveGrisbiFileObject(String outputGrisbiFilePath) throws JAXBException, XMLStreamException, FileNotFoundException {
        try {
            createBackupIfRequested();
            JaxbXmlParser parser = new JaxbXmlParser(BUSINESS_PACKAGE);
            parser.storeObjectUsingStax(m_grisbiFileObj, outputGrisbiFilePath);
            ApplicationLogger.getLogger().log(Level.INFO, "Grisbi file updated  \"" + outputGrisbiFilePath + "\"");
        } catch (IOException e) {
            ApplicationLogger.getLogger().log(Level.SEVERE, "The updated grisbi file will not be saved because an error occured while creating a backup", e);
        }
    }

    /**
	 * Allows to create a backup according to the CREATE.BACKUP property
	 * 
	 * @throws IOException
	 */
    private void createBackupIfRequested() throws IOException {
        ApplicationProperties applicationProperties = ApplicationProperties.getInstance();
        if (applicationProperties.createBackup()) {
            File inputFile = new File(m_inputGrisbiFilePath);
            File backupFile = new File(m_backupGrisbiFilePath);
            try {
                FileUtils.copyFile(inputFile, backupFile);
                ApplicationLogger.getLogger().log(Level.INFO, "Backup created \"" + m_backupGrisbiFilePath + "\"");
            } catch (IOException e) {
                ApplicationLogger.getLogger().log(Level.SEVERE, "Unable to create a backup of the file \"" + m_inputGrisbiFilePath + "\"");
                throw e;
            }
        }
    }

    /**
	 * retrieves a list of all of the operations included in the grisbi file
	 * 
	 * @return
	 */
    public ArrayList<Transaction> getFullOperationList() {
        ArrayList<Transaction> operationList = new ArrayList<Transaction>();
        Grisbi grisbiFileObj = getGrisbiFileObj();
        if (grisbiFileObj != null) {
            operationList = new ArrayList<Transaction>(grisbiFileObj.getTransaction());
        }
        return operationList;
    }

    /**
	 * retrieves from the grisbi file, a list of the operations not already
	 * associated to a category/subCategory
	 * 
	 * @return
	 */
    public ArrayList<Transaction> getNotAssociatedToCategOperationList() {
        ArrayList<Transaction> fullOperationList = getFullOperationList();
        ArrayList<Transaction> notAssociatedOperationList = new ArrayList<Transaction>();
        for (Transaction curOperation : fullOperationList) {
            if (curOperation.getCa().equals("0") && curOperation.getSca().equals("0")) {
                notAssociatedOperationList.add(curOperation);
            }
        }
        return notAssociatedOperationList;
    }

    /**
	 * retrieves from the grisbi file, a list of the operations not already
	 * associated to a third part
	 * 
	 * @return
	 */
    public ArrayList<Transaction> getNotAssociatedToThirdPartOperationList() {
        ArrayList<Transaction> fullOperationList = getFullOperationList();
        ArrayList<Transaction> notAssociatedOperationList = new ArrayList<Transaction>();
        for (Transaction curOperation : fullOperationList) {
            if (curOperation.getPa().equals("0")) {
                notAssociatedOperationList.add(curOperation);
            }
        }
        return notAssociatedOperationList;
    }

    public Grisbi getGrisbiFileObj() {
        if (m_grisbiFileObj == null) {
            try {
                loadGrisbiFileObject();
            } catch (JAXBException ex) {
                ApplicationLogger.getLogger().log(Level.SEVERE, null, ex);
            } catch (XMLStreamException ex) {
                ApplicationLogger.getLogger().log(Level.SEVERE, null, ex);
            } catch (FileNotFoundException ex) {
                ApplicationLogger.getLogger().log(Level.SEVERE, null, ex);
            }
        }
        return m_grisbiFileObj;
    }

    public String getInputGrisbiFilePath() {
        return m_inputGrisbiFilePath;
    }

    public String getBackupGrisbiFilePath() {
        return m_backupGrisbiFilePath;
    }
}
