package com.jvantage.ce.facilities.upgrade.ejb;

import com.jvantage.ce.common.Constants;
import com.jvantage.ce.common.DatabaseConstants;
import com.jvantage.ce.common.ObjectAttribute;
import com.jvantage.ce.common.ObjectAttributeSet;
import com.jvantage.ce.common.UpgradeEvent;
import com.jvantage.ce.common.UpgradeHistory;
import com.jvantage.ce.facilities.FacilitiesException;
import com.jvantage.ce.facilities.application.ejb.ApplicationFacilitiesLocal;
import com.jvantage.ce.facilities.persistence.PersistenceFacilitiesException;
import com.jvantage.ce.facilities.persistence.ejb.PersistenceFacilitiesLocal;
import com.jvantage.ce.facilities.system.ejb.SystemFacilitiesLocal;
import com.jvantage.ce.logging.LogConstants;
import com.jvantage.ce.persistence.DataSourceHelper;
import com.jvantage.ce.persistence.TableAgent;
import com.jvantage.ce.persistence.ejb.TableAgentManagerLocal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ejb.CreateException;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author Brent Clay
 */
@Stateless
public class UpgradeFacilitiesBean implements UpgradeFacilitiesRemote, UpgradeFacilitiesLocal {

    @EJB
    private TableAgentManagerLocal tableAgentManagerBean;

    @EJB
    private ApplicationFacilitiesLocal applicationFacilitiesBean;

    @EJB
    private PersistenceFacilitiesLocal persistenceFacilitiesBean;

    private static Logger logger = Logger.getLogger(LogConstants.sfLoggerName_SystemFacilities);

    private DataSourceHelper dataSourceHelper = null;

    public void performUpgrade() throws FacilitiesException {
        try {
            logger.info("Checking upgrades.");
            boolean upgradesWerePerformed = false;
            if (persistenceFacilitiesBean.tableExistsInDatabase(Constants.sfJVantageDeveloperApplicationName, DatabaseConstants.TableName_JV_UPGRADEHISTORY) == false) {
                StringBuffer msg = new StringBuffer();
                msg.append("The ").append(DatabaseConstants.TableName_JV_UPGRADEHISTORY).append(" table does not exist.");
                logger.info(msg.toString());
                createUpgradeHistoryTable();
            } else {
                StringBuffer msg = new StringBuffer();
                msg.append("Found ").append(DatabaseConstants.TableName_JV_UPGRADEHISTORY).append(" table.");
                logger.info(msg.toString());
            }
            if (upgradesWerePerformed) {
                logger.info("Upgrades were applied successfully.");
            } else {
                logger.info("No upgrade performed.");
            }
        } catch (PersistenceFacilitiesException ex) {
            throw new FacilitiesException("Unable to perform upgrade", ex);
        }
    }

    private void createUpgradeHistoryTable() throws FacilitiesException {
        ApplicationFacilitiesLocal af = null;
        PersistenceFacilitiesLocal pf = null;
        long newTableID = 0L;
        try {
            logger.info("Preparing to create " + DatabaseConstants.TableName_JV_UPGRADEHISTORY + " table.");
            writeLog("Looking up ApplicationFacilities.");
            af = applicationFacilitiesBean;
            writeLog("Looking up PersistenceFacilities.");
            pf = persistenceFacilitiesBean;
            writeLog("Fetching " + DatabaseConstants.TableName_JV_TABLEATTRS + " TableAgent instance.");
            TableAgent tableAttrsTA = getTableAgent(DatabaseConstants.TableName_JV_TABLEATTRS);
            ObjectAttributeSet tableAttrsOAS = tableAttrsTA.getTableAgentObjectAttributes();
            writeLog("Determining whether defunct table attributes already exist for table " + DatabaseConstants.TableName_JV_UPGRADEHISTORY + ".");
            ObjectAttributeSet existingUpgradeHistoryTableAttrsRecordOAS = tableAttrsTA.lookupRecord(DatabaseConstants.TableFieldName_JV_TABLEATTRS_NAME, DatabaseConstants.TableName_JV_UPGRADEHISTORY, true);
            if (existingUpgradeHistoryTableAttrsRecordOAS != null) {
                writeLog("Defunct attributes for table " + DatabaseConstants.TableName_JV_UPGRADEHISTORY + " were found.  Purging.");
                tableAttrsTA.deleteRecordWithoutRegardForDependencies(existingUpgradeHistoryTableAttrsRecordOAS.getPrimaryKeyValue());
                writeLog("Defunct attributes for table " + DatabaseConstants.TableName_JV_UPGRADEHISTORY + " were removed successfully.");
            }
            writeLog("Determing " + Constants.sfJVantageDeveloperApplicationName + " application ID.");
            long jVantageApplicationID = af.getApplicationID(Constants.sfJVantageDeveloperApplicationName);
            writeLog(Constants.sfJVantageDeveloperApplicationName + " application ID: " + jVantageApplicationID);
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_NAME, DatabaseConstants.TableName_JV_UPGRADEHISTORY);
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_ENTITYNAME, "UpgradeHistory");
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_SHORTDESC, "Keeps a record of all upgrades to this jVantage instance.");
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_DESCRIPTION, "Keeps a record of all upgrades to this jVantage instance.");
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_DESCRIPTIVENAMEFIELD, DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_NAME);
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_ELEMENTNAME, "Upgrade Event");
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_PLURALOFELEMENTNAME, "Upgrade History");
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_SECUREWITHSSL, Constants.sfNo);
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_SINGLETON, Constants.sfNo);
            setUpgradeHistoryAttributeValue(tableAttrsOAS, DatabaseConstants.TableFieldName_JV_TABLEATTRS_APPLICATION, new Long(jVantageApplicationID));
            writeLog("Inserting table attributes.");
            newTableID = tableAttrsTA.insertDatabaseRecord(tableAttrsOAS);
            writeLog(DatabaseConstants.TableName_JV_UPGRADEHISTORY + " tableID is [" + newTableID + "].");
        } catch (Exception e) {
            logger.error("Exception", e);
            throw new FacilitiesException("Exception", e);
        }
        try {
            writeLog("Preparing to create " + DatabaseConstants.TableName_JV_UPGRADEHISTORY + " table in underyling database.");
            pf.createNewlyDefinedTable(newTableID);
            writeLog(DatabaseConstants.TableName_JV_UPGRADEHISTORY + " table created successfully.");
            writeLog("Creating name field in " + DatabaseConstants.TableName_JV_UPGRADEHISTORY + " table.");
            pf.insertNameFieldIntoTableDefinition(DatabaseConstants.TableName_JV_UPGRADEHISTORY);
            writeLog("Creating description field in " + DatabaseConstants.TableName_JV_UPGRADEHISTORY + " table.");
            pf.insertDescriptionFieldIntoTableDefinition(DatabaseConstants.TableName_JV_UPGRADEHISTORY);
            writeLog(DatabaseConstants.TableName_JV_UPGRADEHISTORY + " table created successfully.");
        } catch (Exception e) {
            logger.error("Exception", e);
            throw new FacilitiesException("Exception", e);
        }
    }

    private void setUpgradeHistoryAttributeValue(ObjectAttributeSet oas, String attributeName, Object value) {
        writeLog("Setting attribute [" + attributeName + "] to value [" + value + "].");
        ObjectAttribute oa = oas.getObjectWithName(attributeName);
        oa.setValue(value);
    }

    private void writeLog(String value) {
        logger.info("    " + value);
    }

    private void upgradeFrom8To10() {
        SAXParserFactory spf = SAXParserFactory.newInstance();
    }

    public void addUpgradeEvent(UpgradeEvent event) throws FacilitiesException {
        if (event == null) {
            throw new FacilitiesException("Null UpgradeEvent argument.");
        }
        if (event.isValid() == false) {
            throw new FacilitiesException("Invalid UpgradeEvent argument: " + event);
        }
        TableAgent ta;
        try {
            ta = tableAgentManagerBean.getTableAgent(DatabaseConstants.TableName_JV_UPGRADEHISTORY);
            ObjectAttributeSet oas = (ObjectAttributeSet) ta.getTableAgentObjectAttributes().clone();
            oas.getObjectWithName(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_NAME).setValue(event.getProductName());
            oas.getObjectWithName(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_DESCRIPTION).setValue(event.getDescription());
            oas.getObjectWithName(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_LASTEVENT).setValue(event.getLastEventDate());
            oas.getObjectWithName(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_VERSIONNUMBER).setValue(event.getVersion());
            oas.getObjectWithName(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_RELEASENUMBER).setValue(event.getRelease());
            oas.getObjectWithName(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_MILESTONENUMBER).setValue(event.getMilestone());
            oas.getObjectWithName(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_BUILDNUMBER).setValue(event.getBuild());
            ta.insertDatabaseRecord(oas);
        } catch (PersistenceException ex) {
            throw new FacilitiesException("Unable to add UpgradeEvent: " + event, ex);
        }
    }

    public UpgradeHistory getUpgradeHistory(String productName) throws FacilitiesException {
        UpgradeHistory upgradeHistory = new UpgradeHistory();
        StringBuffer query = new StringBuffer();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = getDataSource().getConnection();
            query.append("SELECT * FROM ").append(DatabaseConstants.TableName_JV_UPGRADEHISTORY);
            boolean useProductName = false;
            if (StringUtils.isNotBlank(productName)) {
                useProductName = true;
                query.append(" WHERE LOWER(").append(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_NAME).append(") = LOWER(?)");
            }
            query.append(" ORDER BY ").append(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_LASTEVENT).append(" DESC");
            stmt = conn.prepareStatement(query.toString());
            if (useProductName) {
                stmt.setString(1, productName);
            }
            rs = stmt.executeQuery(query.toString());
            while (rs.next()) {
                UpgradeEvent event = new UpgradeEvent();
                event.setProductName(rs.getString(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_NAME));
                event.setDescription(rs.getString(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_DESCRIPTION));
                event.setLastEventDate(rs.getDate(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_LASTEVENT));
                event.setVersion(rs.getString(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_VERSIONNUMBER));
                event.setRelease(rs.getString(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_RELEASENUMBER));
                event.setMilestone(rs.getString(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_MILESTONENUMBER));
                event.setBuild(rs.getString(DatabaseConstants.TableFieldName_JV_UPGRADEHISTORY_BUILDNUMBER));
                upgradeHistory.addEvent(event);
            }
        } catch (SQLException e) {
            DataSourceHelper.logSQLException(e, logger, query);
            throw new FacilitiesException("java.sql.SQLException", e);
        } finally {
            DataSourceHelper.releaseResources(conn, stmt);
        }
        return upgradeHistory;
    }

    public int getUpgradeHistoryEventCount(String productName) throws FacilitiesException {
        UpgradeHistory upgradeHistory = getUpgradeHistory(productName);
        if (upgradeHistory == null) {
            return 0;
        }
        return upgradeHistory.getNumberOfHistoryEvents();
    }

    private DataSource getDataSource() {
        return getDataSourceHelper().getDataSource();
    }

    private DataSourceHelper getDataSourceHelper() {
        if (dataSourceHelper == null) {
            dataSourceHelper = new DataSourceHelper();
        }
        return dataSourceHelper;
    }

    private TableAgent getTableAgent(String tableName) throws FacilitiesException {
        return tableAgentManagerBean.getTableAgent(tableName);
    }
}
