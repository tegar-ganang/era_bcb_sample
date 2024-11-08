package gov.sns.services.duplicatepvfinder;

import gov.sns.ca.ChannelChecker;
import gov.sns.ca.ChannelFactory;
import gov.sns.tools.database.ConnectionDictionary;
import gov.sns.tools.database.OracleDatabaseAdaptor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import oracle.jdbc.OracleStatement;

/**
 * Searches for duplicate PVs that are in use.
 * 
 * @author Chris Fowlkes
 */
public class PVFinder {

    /**
   * Holds the <CODE>DatabaseAdaptor</CODE> used to connect to the database.
   */
    private OracleDatabaseAdaptor databaseAdaptor = new OracleDatabaseAdaptor();

    /**
   * Creates a new <CODE>PVFInder</CODE>.
   */
    public PVFinder() {
    }

    /**
   * Looks in the Irmis database for duplicate PVs. The data is returned as 
   * instances of <CODE>DuplicatePV</CODE>.
   * 
   * @param dictionary Database credentials to use.
   * @return The names of the duplicate PVs from the Irmis database.
   * @throws gov.sns.tools.database.DatabaseException Thrown on SQL error.
   */
    public DuplicatePV[] findDuplicatePVs(ConnectionDictionary dictionary) throws gov.sns.tools.database.DatabaseException {
        try {
            Connection connection = databaseAdaptor.getConnection(dictionary);
            try {
                Statement query = connection.createStatement();
                try {
                    ((OracleStatement) query).setRowPrefetch(50);
                    ResultSet result = query.executeQuery("SELECT REC_NM, REC_TYPE, IOC_NM, FILE_NM, COUNT(*) AS OCCUR_IN_FILE_CNT FROM IRMISBASE.DUPLICATE_PV_V GROUP BY (REC_NM, REC_TYPE, IOC_NM, FILE_NM)");
                    try {
                        ArrayList pvs = new ArrayList();
                        while (result.next()) pvs.add(new DuplicatePV(result));
                        return (DuplicatePV[]) pvs.toArray(new DuplicatePV[pvs.size()]);
                    } finally {
                        result.close();
                    }
                } finally {
                    query.close();
                }
            } finally {
                connection.close();
            }
        } catch (java.sql.SQLException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Exception finding duplicate PVs.", exception);
            throw new gov.sns.tools.database.DatabaseException("Exception finding duplicate PVs.", databaseAdaptor, exception);
        }
    }

    /**
   * Finds and returns the bad PVs.
   * 
   * @param pvs The pvs to check as instances of <CODE>DuplicaePV</CODE>.
   */
    public String[] findActivePVs(DuplicatePV[] pvs) {
        ArrayList channels = new ArrayList();
        ChannelFactory factory = ChannelFactory.defaultFactory();
        for (int i = 0; i < pvs.length; i++) channels.add(factory.getChannel(pvs[i].getName()));
        ChannelChecker checker = new ChannelChecker(channels);
        checker.checkThem();
        List<String> goodPVs = checker.getGoodPVs();
        return goodPVs.toArray(new String[goodPVs.size()]);
    }

    /**
   * Saves the given instances of <CODE>DuplicatePV</CODE> as active duplicates in 
   * the irmisBase database.
   * 
   * @param pvs The instances of <CODE>DuplicatePV</CODE> that represent duplicated PVs.
   * @param activePVs The names of the PVs that are active.
   * @param dictionary The database credentials to use.
   * @throws gov.sns.tools.database.DatabaseException Thrown on SQL error.
   */
    public void saveChannelsToDatabase(DuplicatePV[] pvs, String[] activePVs, ConnectionDictionary dictionary) throws gov.sns.tools.database.DatabaseException {
        try {
            Connection connection = databaseAdaptor.getConnection(dictionary);
            try {
                connection.setAutoCommit(false);
                Statement deleteQuery = connection.createStatement();
                try {
                    deleteQuery.execute("DELETE FROM EPICS.DUPL_SGNL");
                } finally {
                    deleteQuery.close();
                }
                PreparedStatement insertQuery = connection.prepareStatement("INSERT INTO EPICS.DUPL_SGNL(SGNL_ID, DVC_ID, EXT_SRC_FILE_NM, ACT_IND) VALUES(?, ?, ?, ?)");
                try {
                    HashMap iocIDs = findIOCIDs(pvs, dictionary);
                    Arrays.sort(activePVs);
                    for (int i = 0; i < pvs.length; i++) {
                        String pvName = pvs[i].getName();
                        insertQuery.setString(1, pvName);
                        insertQuery.setString(2, iocIDs.get(pvs[i].getIOCName()).toString());
                        insertQuery.setString(3, pvs[i].getFileName());
                        if (Arrays.binarySearch(activePVs, pvName) >= 0) insertQuery.setString(4, "Y"); else insertQuery.setString(4, "N");
                        insertQuery.execute();
                    }
                    connection.commit();
                } finally {
                    insertQuery.close();
                }
            } catch (java.sql.SQLException exception) {
                try {
                    connection.rollback();
                } finally {
                    throw exception;
                }
            } finally {
                connection.close();
            }
        } catch (java.sql.SQLException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Exception saving to database.", exception);
            throw new gov.sns.tools.database.DatabaseException("Exception saving to database.", databaseAdaptor, exception);
        }
    }

    protected HashMap findIOCIDs(DuplicatePV[] pvs, ConnectionDictionary dictionary) {
        ArrayList iocNames = new ArrayList();
        for (int i = 0; i < pvs.length; i++) {
            String name = pvs[i].getIOCName();
            if (!iocNames.contains(name)) iocNames.add(name);
        }
        try {
            Connection connection = databaseAdaptor.getConnection(dictionary);
            try {
                StringBuffer sql = new StringBuffer("SELECT DVC_ID, IOC_NET_NM FROM EPICS.IOC_DVC WHERE IOC_NET_NM IN (");
                int iocNameCount = iocNames.size();
                for (int i = 0; i < iocNameCount; i++) {
                    if (i > 0) sql.append(", ");
                    sql.append("?");
                }
                sql.append(")");
                PreparedStatement query = connection.prepareStatement(sql.toString());
                try {
                    ((OracleStatement) query).setRowPrefetch(50);
                    for (int i = 0; i < iocNameCount; i++) query.setString(i + 1, iocNames.get(i).toString());
                    ResultSet result = query.executeQuery();
                    try {
                        HashMap iocIDs = new HashMap();
                        while (result.next()) iocIDs.put(result.getString("IOC_NET_NM"), result.getString("DVC_ID"));
                        return iocIDs;
                    } finally {
                        result.close();
                    }
                } finally {
                    query.close();
                }
            } finally {
                connection.close();
            }
        } catch (java.sql.SQLException exception) {
            Logger.getLogger("global").log(Level.WARNING, "Exception loading IOC IDs from the database.", exception);
            throw new gov.sns.tools.database.DatabaseException("Exception loading IOC IDs from the database.", databaseAdaptor, exception);
        }
    }
}
