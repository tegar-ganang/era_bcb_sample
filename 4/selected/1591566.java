package fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseUtils;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.StringTokenizer;
import java.util.Vector;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.ErrSeverity;
import fr.esrf.TangoDs.TangoConst;
import fr.soleil.commonarchivingapi.ArchivingTools.Tools.GlobalConst;
import fr.soleil.commonarchivingapi.ArchivingTools.Tools.StringFormater;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.ConfigConst;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.AttributesManagement.AdtAptAttributes.AdtAptAttributesFactory;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.AttributesManagement.AdtAptAttributes.IAdtAptAttributes;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbCommands.ConnectionCommands;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbConnection.ConnectionFactory;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbConnection.IDBConnection;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.ArchivingException;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.DateUtil;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.SamplingType;

public abstract class DbUtils implements IDbUtils {

    protected int archType;

    public DbUtils(final int type) {
        archType = type;
    }

    @Override
    public abstract String getFormat(SamplingType samplingType);

    @Override
    public abstract String toDbTimeString(String timeField);

    @Override
    public abstract String toDbTimeFieldString(String timeField);

    @Override
    public abstract String toDbTimeFieldString(String timeField, String format);

    @Override
    public abstract String getTime(String string) throws ArchivingException;

    @Override
    public abstract String getTableName(int index);

    protected abstract String getRequest();

    protected abstract String getFormattedTimeField(String maxOrMin);

    @Override
    public Timestamp getTimeOfLastInsert(final String completeName, final boolean max) throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return null;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        Timestamp ret = null;
        final String maxOrMin = max ? "MAX" : "MIN";
        final String field = getFormattedTimeField(maxOrMin);
        final String tableName = dbConn.getSchema() + "." + getTableName(completeName);
        final String query = "select " + field + " from " + tableName;
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(query);
            rset.next();
            final String rawDate = rset.getString(1);
            if (rawDate == null) {
                return null;
            }
            final long stringToMilli = DateUtil.stringToMilli(rawDate);
            ret = new Timestamp(stringToMilli);
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return ret;
    }

    /**
     * 
     * @param manager
     * @return
     * @throws ArchivingException
     * @throws SQLException
     */
    public void deleteOldRecords(final long keepedPeriod, final String[] attributeList) throws ArchivingException, SQLException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        final IAdtAptAttributes att = AdtAptAttributesFactory.getInstance(archType);
        if (dbConn == null || att == null) {
            return;
        }
        final long currentDate = System.currentTimeMillis();
        final long keepedDate = currentDate - keepedPeriod;
        final long time = keepedDate;
        final Timestamp timeSt = new Timestamp(time);
        Connection conn = null;
        PreparedStatement psDelete = null;
        try {
            conn = dbConn.getConnection();
            for (final String name : attributeList) {
                try {
                    final String tableName = dbConn.getSchema() + "." + getTableName(name);
                    final String tableField = ConfigConst.TAB_SCALAR_RO[0];
                    final String deleteString = "DELETE FROM  " + tableName + " WHERE " + tableField + " <= ?";
                    final String truncateString = "TRUNCATE TABLE  " + tableName;
                    boolean everythingIsOld = false;
                    final Timestamp lastInsert = getTimeOfLastInsert(name, true);
                    if (lastInsert != null) {
                        everythingIsOld = lastInsert.getTime() - timeSt.getTime() < 0;
                    }
                    final String query = everythingIsOld ? truncateString : deleteString;
                    psDelete = conn.prepareStatement(query);
                    if (!everythingIsOld) {
                        psDelete.setTimestamp(1, timeSt);
                    }
                    psDelete.executeUpdate();
                    ConnectionCommands.close(psDelete);
                } catch (final SQLException e) {
                    ConnectionCommands.close(psDelete);
                    e.printStackTrace();
                    System.out.println("SQLException received (go to the next element) : " + e);
                    continue;
                } catch (final ArchivingException e) {
                    ConnectionCommands.close(psDelete);
                    e.printStackTrace();
                    System.out.println("ArchivingException received (go to the next element) : " + e);
                    continue;
                } catch (final Exception e) {
                    ConnectionCommands.close(psDelete);
                    e.printStackTrace();
                    System.out.println("Unknown Exception received (go to the next element) : " + e);
                    continue;
                }
            }
        } finally {
            ConnectionCommands.close(psDelete);
            dbConn.closeConnection(conn);
        }
    }

    public Timestamp now() throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return null;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final String sqlStr = getRequest();
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(sqlStr);
            rset.next();
            final Timestamp date = rset.getTimestamp(1);
            return date;
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
    }

    /**
     * Start watcher report in real time
     */
    @Override
    public void startWatcherReport() throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return;
        }
        Connection conn = null;
        CallableStatement stmt = null;
        final String startAdminReportQuery = "CALL " + dbConn.getSchema() + ".FEEDALIVE()";
        try {
            conn = dbConn.getConnection();
            stmt = conn.prepareCall(startAdminReportQuery);
            stmt.execute();
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
    }

    /**
     * return % of procedure call progression
     */
    @Override
    public int getFeedAliveProgression() throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return 0;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final String table = dbConn.getSchema() + "." + ConfigConst.IS_ALIVED;
        final String getProgressionLevelQuery = "SELECT TRUNC ( COUNT(" + ConfigConst.TAB_ISALIVED[2] + ")/" + "(SELECT COUNT(*) FROM " + ConfigConst.AMT + " WHERE " + ConfigConst.stopDate + " IS NULL)" + ") FROM " + table;
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(getProgressionLevelQuery);
            if (rset.next()) {
                return rset.getInt(1);
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return 0;
    }

    /**
     * return the number of KO and OK attributes
     */
    @Override
    public int getAttributesCountOkOrKo(final boolean isOKStatus) throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return 0;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        int attributesCount = 0;
        final String status = isOKStatus ? "\'OK\'" : "\'KO\'";
        final String selectField = "COUNT(*)";
        final String table = dbConn.getSchema() + "." + ConfigConst.IS_ALIVED;
        final String clause1 = ConfigConst.IS_ALIVED + "." + ConfigConst.TAB_ISALIVED[0] + " = " + status;
        final String getAttributesCountDataQuery = "SELECT DISTINCT(" + selectField + ")" + " FROM " + table + " WHERE " + "(" + clause1 + ")";
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(getAttributesCountDataQuery);
            if (rset.next()) {
                attributesCount = rset.getInt(1);
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return attributesCount;
    }

    @Override
    public String[] getKOAttrCountByDevice() throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return null;
        }
        final String tableName = dbConn.getSchema() + "." + ConfigConst.TABS[7];
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final String getKOAttributesByDeviceQuery = "SELECT * FROM " + tableName;
        final Vector<String> res = new Vector<String>();
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(getKOAttributesByDeviceQuery);
            res.add("\n---------- Ko attributes count by device: ");
            res.add("Archiver  NBR");
            while (rset.next()) {
                final String elt = rset.getString(1) + "  " + String.valueOf(rset.getInt(2));
                res.add(elt);
            }
        } catch (final SQLException e) {
            String message = "";
            if (e.getMessage().equalsIgnoreCase(GlobalConst.COMM_FAILURE_ORACLE) || e.getMessage().indexOf(GlobalConst.COMM_FAILURE_MYSQL) != -1) {
                message = GlobalConst.ARCHIVING_ERROR_PREFIX + " : " + GlobalConst.ADB_CONNECTION_FAILURE;
            } else {
                message = GlobalConst.ARCHIVING_ERROR_PREFIX + " : " + GlobalConst.STATEMENT_FAILURE;
            }
            final String reason = GlobalConst.QUERY_FAILURE;
            final String desc = "Failed while executing DbUtils.getKOAttrCountByDevice() method...";
            throw new ArchivingException(message, reason, ErrSeverity.WARN, desc, this.getClass().getName(), e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return res.size() > 2 ? toStringArray(res) : null;
    }

    /**
     * return the number of KO and OK attributes
     */
    @Override
    public String[] getKoAttributes() throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return null;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final Vector<String> koAttributes = new Vector<String>();
        final String select_field = "*";
        final String table = dbConn.getSchema() + "." + ConfigConst.IS_ALIVED;
        final String clause1 = ConfigConst.IS_ALIVED + "." + ConfigConst.TAB_ISALIVED[0] + " = \'KO\'";
        final String orderbyClause = ConfigConst.IS_ALIVED + "." + ConfigConst.TAB_ISALIVED[4];
        final String getKOAttributesQuery = "SELECT " + select_field + " FROM " + table + " WHERE " + "(" + clause1 + ")" + " ORDER BY " + orderbyClause;
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(getKOAttributesQuery);
            koAttributes.add("\n---------- List of Ko attributes: ");
            koAttributes.add("Status  Full_Name  ID  Archiver  maxTime");
            while (rset.next()) {
                String elt = rset.getString(1) + "  " + rset.getString(2) + "  " + String.valueOf(rset.getInt(3)) + "  " + rset.getString(4) + "  ";
                elt += rset.getDate(5) == null ? "null" : rset.getDate(5);
                koAttributes.add(elt);
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return koAttributes.size() > 2 ? toStringArray(koAttributes) : null;
    }

    /**
     * return the list of job's status
     */
    @Override
    public String[] getListOfJobStatus() throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return null;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final Vector<String> jobStatusVector = new Vector<String>();
        final String selectField = ConfigConst.TAB_LOG_JOB[2] + ", " + ConfigConst.TAB_LOG_JOB[3] + " ," + ConfigConst.TAB_LOG_JOB[7] + ", " + "to_char(" + ConfigConst.TAB_LOG_JOB[1] + ",\'YYYY-MM-DD HH24:MI:SS\')" + " time";
        final String table = ConfigConst.TABS[5];
        final String clause1 = ConfigConst.TAB_LOG_JOB[1] + " IN (SELECT MAX(" + ConfigConst.TAB_LOG_JOB[1] + ") FROM " + table + " GROUP BY " + ConfigConst.TAB_LOG_JOB[3] + ")";
        final String clause_2 = ConfigConst.TAB_LOG_JOB[2] + " IN (\'SYSTEM\', \'" + dbConn.getSchema().toUpperCase() + "\', \'ADMINISTRATOR\') ORDER BY 2,1";
        final String getListOfJobStatusQuery = "SELECT " + selectField + " FROM " + table + " WHERE " + clause1 + " AND " + clause_2;
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(getListOfJobStatusQuery);
            jobStatusVector.add("\n---------- List of job status: ");
            jobStatusVector.add("OWNER  JOB_NAME  STATUS  LOG_DATE");
            while (rset.next()) {
                String elt = rset.getString(1) + "  " + rset.getString(2) + "  " + rset.getString(3) + "  ";
                elt += rset.getTimestamp(4) == null ? "null" : rset.getTimestamp(4).toString();
                jobStatusVector.add(elt);
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return jobStatusVector.size() > 2 ? toStringArray(jobStatusVector) : null;
    }

    /**
     * return the list of job's status
     */
    @Override
    public String[] getListOfJobErrors() throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        if (dbConn == null) {
            return null;
        }
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final Vector<String> jobErrorsVector = new Vector<String>();
        final String selectField = "to_char(" + ConfigConst.TAB_RUN_DETAILS[1] + ",\'YYYY-MM-DD HH24:MI:SS\')" + " time " + ", " + ConfigConst.TAB_RUN_DETAILS[3] + ", " + ConfigConst.TAB_RUN_DETAILS[5] + " ," + ConfigConst.TAB_RUN_DETAILS[14];
        final String table = ConfigConst.TABS[6];
        final String clause1 = ConfigConst.TAB_RUN_DETAILS[14] + " IS NOT NULL ";
        final String clause2 = ConfigConst.TAB_RUN_DETAILS[1] + " > SYSDATE - 5" + "ORDER BY 1";
        String getJobErrorsQuery = "SELECT " + selectField + " FROM " + table + " WHERE " + clause1 + " AND " + clause2;
        try {
            conn = dbConn.getConnection();
            getJobErrorsQuery = conn.nativeSQL(getJobErrorsQuery);
            stmt = conn.createStatement();
            rset = stmt.executeQuery(getJobErrorsQuery);
            jobErrorsVector.add("\n---------- List of job Errors : ");
            jobErrorsVector.add("LOG_DATE  JOB_NAME  STATUS  ADDITIONAL_INFO");
            while (rset.next()) {
                jobErrorsVector.add(rset.getTimestamp(1).toString() + "  " + rset.getString(2) + "  " + rset.getString(3) + "  " + rset.getString(4));
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return jobErrorsVector.size() > 2 ? toStringArray(jobErrorsVector) : null;
    }

    /**
     * <b>Description : </b> Build a array of Double with the given Double
     * Vector
     * 
     * @param vector
     *            The given Double Vector
     * @return a Double type array that contains the differents vector's Double
     *         type elements <br>
     */
    public static double[] toDoubleArray(final Vector<Double> vector) {
        double[] array;
        array = new double[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            array[i] = vector.elementAt(i).doubleValue();
        }
        return array;
    }

    /**
     * <b>Description : </b> Build a array of Double with the two given Double
     * Vectors
     * 
     * @param vector1
     *            The first given Double Vector
     * @param vector2
     *            The second given Double Vector
     * @return a Double type array that contains the first and the second Double
     *         elements <br>
     */
    public static double[] toDoubleArray(final Vector<Double> vector1, final Vector<Double> vector2) {
        for (int i = 0; i < vector2.size(); i++) {
            vector1.addElement(vector2.elementAt(i));
        }
        return toDoubleArray(vector1);
    }

    /**
     * <b>Description : </b> Build a array of String with the given String
     * Vector
     * 
     * @param vector
     *            The given String Vector
     * @return a String type array that contains the differents vector's String
     *         type elements <br>
     */
    public static String[] toStringArray(final Vector<String> vector) {
        String[] array;
        array = new String[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            array[i] = vector.elementAt(i);
        }
        return array;
    }

    /**
     * <b>Description : </b> Build a array of String with the two String Double
     * Vectors
     * 
     * @param vector1
     *            The first given String Vector
     * @param vector2
     *            The second given String Vector
     * @return a String type array that contains the first and the second String
     *         elements <br>
     */
    public static String[] toStringArray(final Vector<String> vector1, final Vector<String> vector2) {
        for (int i = 0; i < vector2.size(); i++) {
            vector1.addElement(vector2.elementAt(i));
        }
        return toStringArray(vector1);
    }

    /**
     * This method returns a String which contains the value columnn name
     * 
     * @param completeName
     *            : attribute full_name
     * @param dataFormat
     *            : AttrDataFormat._SCALAR or AttrDataFormat._SPECTRUM ot
     *            AttrDataFormat._IMAGE
     * @param writable
     *            AttrWriteType._READ_WRITE or AttrWriteType._READ or
     *            AttrWriteType._WRITE
     * @return corresponding column name
     */
    private String getValueColumnName(final int dataFormat, final int writable) {
        if (dataFormat == AttrDataFormat._SCALAR) {
            if (writable == AttrWriteType._READ_WRITE) {
                return ConfigConst.TAB_SCALAR_RW[1];
            } else if (writable == AttrWriteType._READ) {
                return ConfigConst.TAB_SCALAR_RO[1];
            } else {
                return ConfigConst.TAB_SCALAR_WO[1];
            }
        } else if (dataFormat == AttrDataFormat._SPECTRUM) {
            if (writable == AttrWriteType._READ_WRITE) {
                return ConfigConst.TAB_SPECTRUM_RW[2];
            } else {
                return ConfigConst.TAB_SPECTRUM_RO[2];
            }
        } else {
            if (writable == AttrWriteType._READ_WRITE) {
                return ConfigConst.TAB_IMAGE_RW[3];
            } else {
                return ConfigConst.TAB_IMAGE_RO[3];
            }
        }
    }

    /**
     * 
     * @param attDirPath
     * @param prefix
     * @param nbFileMax
     * @return
     */
    public int getRelativePathIndex(final String attDirPath, final String prefix, final int nbFileMax) {
        final File attDirectory = new File(attDirPath);
        if (!attDirectory.exists()) {
            attDirectory.mkdirs();
            return 1;
        } else {
            final int currentIndexDir = attDirectory.listFiles().length;
            final String currentPath = attDirPath + File.separator + prefix + currentIndexDir;
            final File attCurrentDirectory = new File(currentPath);
            if (attCurrentDirectory.listFiles().length < nbFileMax) {
                return currentIndexDir;
            } else {
                return currentIndexDir + 1;
            }
        }
    }

    /**
     * This method returns the name of the table associated (table in wich will
     * host its archived values) to the given attribute
     * 
     * @param attributeName
     *            the attribute's name (cf. ADT in HDB).
     * @return the name of the table associated (table in wich will host its
     *         archived values) to the given attribute.
     */
    @Override
    public String getTableName(final String attributeName) throws ArchivingException {
        final IAdtAptAttributes att = AdtAptAttributesFactory.getInstance(archType);
        final int id = att.getIds().getAttID(attributeName);
        if (id <= 0) {
            throw new ArchivingException("Invalid attribute: " + attributeName, "Invalid attribute: " + attributeName, ErrSeverity.WARN, "No database connection or \"" + attributeName + "\" attribute not found in database", this.getClass().getName());
        }
        return getTableName(id);
    }

    @Override
    public Object[] getSpectrumValue(final String readString, final String writeString, final int dataType) {
        int size = 0;
        StringTokenizer readTokenizer;
        if (readString == null || "".equals(readString) || "null".equals(readString)) {
            readTokenizer = null;
        } else {
            readTokenizer = new StringTokenizer(readString, GlobalConst.CLOB_SEPARATOR);
            size += readTokenizer.countTokens();
        }
        StringTokenizer writeTokenizer;
        if (writeString == null || "".equals(writeString) || "null".equals(writeString)) {
            writeTokenizer = null;
        } else {
            writeTokenizer = new StringTokenizer(writeString, GlobalConst.CLOB_SEPARATOR);
            size += writeTokenizer.countTokens();
        }
        Double[] dvalueArr = null;
        Integer[] lvalueArr = null;
        Long[] lvalueArr2 = null;
        Short[] svalueArr = null;
        Boolean[] bvalueArr = null;
        Float[] fvalueArr = null;
        String[] stvalueArr = null;
        switch(dataType) {
            case TangoConst.Tango_DEV_BOOLEAN:
                bvalueArr = new Boolean[size];
                break;
            case TangoConst.Tango_DEV_STATE:
            case TangoConst.Tango_DEV_LONG:
            case TangoConst.Tango_DEV_ULONG:
                lvalueArr = new Integer[size];
                break;
            case TangoConst.Tango_DEV_LONG64:
            case TangoConst.Tango_DEV_ULONG64:
                lvalueArr2 = new Long[size];
                break;
            case TangoConst.Tango_DEV_SHORT:
            case TangoConst.Tango_DEV_USHORT:
            case TangoConst.Tango_DEV_UCHAR:
                svalueArr = new Short[size];
                break;
            case TangoConst.Tango_DEV_FLOAT:
                fvalueArr = new Float[size];
                break;
            case TangoConst.Tango_DEV_STRING:
                stvalueArr = new String[size];
                break;
            case TangoConst.Tango_DEV_DOUBLE:
            default:
                dvalueArr = new Double[size];
        }
        int i = 0;
        if (readTokenizer != null) {
            while (readTokenizer.hasMoreTokens()) {
                final String currentValRead = readTokenizer.nextToken();
                if (currentValRead == null || currentValRead.trim().equals("")) {
                    break;
                }
                switch(dataType) {
                    case TangoConst.Tango_DEV_BOOLEAN:
                        try {
                            if (currentValRead == null || "".equals(currentValRead) || "null".equals(currentValRead) || "NaN".equalsIgnoreCase(currentValRead)) {
                                bvalueArr[i] = null;
                            } else {
                                bvalueArr[i] = new Boolean(Double.valueOf(currentValRead).intValue() != 0);
                            }
                        } catch (final NumberFormatException n) {
                            bvalueArr[i] = new Boolean("true".equalsIgnoreCase(currentValRead.trim()));
                        }
                        break;
                    case TangoConst.Tango_DEV_STATE:
                    case TangoConst.Tango_DEV_LONG:
                    case TangoConst.Tango_DEV_ULONG:
                        try {
                            if (currentValRead == null || "".equals(currentValRead) || "null".equals(currentValRead) || "NaN".equalsIgnoreCase(currentValRead)) {
                                lvalueArr[i] = null;
                            } else {
                                lvalueArr[i] = Integer.valueOf(currentValRead);
                            }
                        } catch (final NumberFormatException n) {
                            lvalueArr[i] = new Integer(Double.valueOf(currentValRead).intValue());
                        }
                        break;
                    case TangoConst.Tango_DEV_LONG64:
                    case TangoConst.Tango_DEV_ULONG64:
                        try {
                            if (currentValRead == null || "".equals(currentValRead) || "null".equals(currentValRead) || "NaN".equalsIgnoreCase(currentValRead)) {
                                lvalueArr2[i] = null;
                            } else {
                                lvalueArr2[i] = Long.valueOf(currentValRead);
                            }
                        } catch (final NumberFormatException n) {
                            lvalueArr2[i] = new Long(Double.valueOf(currentValRead).longValue());
                        }
                        break;
                    case TangoConst.Tango_DEV_SHORT:
                    case TangoConst.Tango_DEV_USHORT:
                    case TangoConst.Tango_DEV_UCHAR:
                        try {
                            if (currentValRead == null || "".equals(currentValRead) || "null".equals(currentValRead) || "NaN".equalsIgnoreCase(currentValRead)) {
                                svalueArr[i] = null;
                            } else {
                                svalueArr[i] = Short.valueOf(currentValRead);
                            }
                        } catch (final NumberFormatException n) {
                            svalueArr[i] = new Short(Double.valueOf(currentValRead).shortValue());
                        }
                        break;
                    case TangoConst.Tango_DEV_FLOAT:
                        if (currentValRead == null || "".equals(currentValRead) || "null".equals(currentValRead) || "NaN".equalsIgnoreCase(currentValRead)) {
                            fvalueArr[i] = null;
                        } else {
                            fvalueArr[i] = Float.valueOf(currentValRead);
                        }
                        break;
                    case TangoConst.Tango_DEV_STRING:
                        if (currentValRead == null || "".equals(currentValRead) || "null".equals(currentValRead) || "NaN".equalsIgnoreCase(currentValRead)) {
                            stvalueArr[i] = null;
                        } else {
                            stvalueArr[i] = StringFormater.formatStringToRead(new String(currentValRead));
                        }
                        break;
                    case TangoConst.Tango_DEV_DOUBLE:
                    default:
                        if (currentValRead == null || "".equals(currentValRead) || "null".equals(currentValRead) || "NaN".equalsIgnoreCase(currentValRead)) {
                            dvalueArr[i] = null;
                        } else {
                            dvalueArr[i] = Double.valueOf(currentValRead);
                        }
                }
                i++;
            }
        }
        if (writeTokenizer != null) {
            while (writeTokenizer.hasMoreTokens()) {
                final String currentValWrite = writeTokenizer.nextToken();
                if (currentValWrite == null || currentValWrite.trim().equals("")) {
                    break;
                }
                switch(dataType) {
                    case TangoConst.Tango_DEV_BOOLEAN:
                        try {
                            if (currentValWrite == null || "".equals(currentValWrite) || "null".equals(currentValWrite) || "NaN".equalsIgnoreCase(currentValWrite)) {
                                bvalueArr[i] = null;
                            } else {
                                bvalueArr[i] = new Boolean(Double.valueOf(currentValWrite).intValue() != 0);
                            }
                        } catch (final NumberFormatException n) {
                            bvalueArr[i] = new Boolean("true".equalsIgnoreCase(currentValWrite.trim()));
                        }
                        break;
                    case TangoConst.Tango_DEV_STATE:
                    case TangoConst.Tango_DEV_LONG:
                    case TangoConst.Tango_DEV_ULONG:
                        try {
                            if (currentValWrite == null || "".equals(currentValWrite) || "null".equals(currentValWrite) || "NaN".equalsIgnoreCase(currentValWrite)) {
                                lvalueArr[i] = null;
                            } else {
                                lvalueArr[i] = Integer.valueOf(currentValWrite);
                            }
                        } catch (final NumberFormatException n) {
                            lvalueArr[i] = new Integer(Double.valueOf(currentValWrite).intValue());
                        }
                        break;
                    case TangoConst.Tango_DEV_LONG64:
                    case TangoConst.Tango_DEV_ULONG64:
                        try {
                            if (currentValWrite == null || "".equals(currentValWrite) || "null".equals(currentValWrite) || "NaN".equalsIgnoreCase(currentValWrite)) {
                                lvalueArr2[i] = null;
                            } else {
                                lvalueArr2[i] = Long.valueOf(currentValWrite);
                            }
                        } catch (final NumberFormatException n) {
                            lvalueArr2[i] = new Long(Double.valueOf(currentValWrite).intValue());
                        }
                        break;
                    case TangoConst.Tango_DEV_UCHAR:
                    case TangoConst.Tango_DEV_SHORT:
                    case TangoConst.Tango_DEV_USHORT:
                        try {
                            if (currentValWrite == null || "".equals(currentValWrite) || "null".equals(currentValWrite) || "NaN".equalsIgnoreCase(currentValWrite)) {
                                svalueArr[i] = null;
                            } else {
                                svalueArr[i] = Short.valueOf(currentValWrite);
                            }
                        } catch (final NumberFormatException n) {
                            svalueArr[i] = new Short(Double.valueOf(currentValWrite).shortValue());
                        }
                        break;
                    case TangoConst.Tango_DEV_FLOAT:
                        if (currentValWrite == null || "".equals(currentValWrite) || "null".equals(currentValWrite) || "NaN".equalsIgnoreCase(currentValWrite)) {
                            fvalueArr[i] = null;
                        } else {
                            fvalueArr[i] = Float.valueOf(currentValWrite);
                        }
                        break;
                    case TangoConst.Tango_DEV_STRING:
                        if (currentValWrite == null || "".equals(currentValWrite) || "null".equals(currentValWrite) || "NaN".equalsIgnoreCase(currentValWrite)) {
                            stvalueArr[i] = null;
                        } else {
                            stvalueArr[i] = StringFormater.formatStringToRead(new String(currentValWrite));
                        }
                        break;
                    case TangoConst.Tango_DEV_DOUBLE:
                    default:
                        if (currentValWrite == null || "".equals(currentValWrite) || "null".equals(currentValWrite) || "NaN".equalsIgnoreCase(currentValWrite)) {
                            dvalueArr[i] = null;
                        } else {
                            dvalueArr[i] = Double.valueOf(currentValWrite);
                        }
                }
                i++;
            }
        }
        if (readTokenizer == null && writeTokenizer == null) {
            return null;
        }
        switch(dataType) {
            case TangoConst.Tango_DEV_BOOLEAN:
                return bvalueArr;
            case TangoConst.Tango_DEV_STATE:
            case TangoConst.Tango_DEV_LONG:
            case TangoConst.Tango_DEV_ULONG:
                return lvalueArr;
            case TangoConst.Tango_DEV_LONG64:
            case TangoConst.Tango_DEV_ULONG64:
                return lvalueArr2;
            case TangoConst.Tango_DEV_UCHAR:
            case TangoConst.Tango_DEV_SHORT:
            case TangoConst.Tango_DEV_USHORT:
                return svalueArr;
            case TangoConst.Tango_DEV_FLOAT:
                return fvalueArr;
            case TangoConst.Tango_DEV_STRING:
                return stvalueArr;
            case TangoConst.Tango_DEV_DOUBLE:
            default:
                return dvalueArr;
        }
    }

    /**
     * 
     * @param dbValue
     * @param dataType
     * @return
     */
    @Override
    public Object[][] getImageValue(final String dbValue, final int dataType) {
        if (dbValue == null || "".equals(dbValue) || "null".equals(dbValue)) {
            return null;
        }
        Object[] valArray = null;
        String value = new String(dbValue);
        value = value.replaceAll("\\[", "");
        value = value.replaceAll("\\]", "");
        int rowSize = 0, colSize = 0;
        final StringTokenizer readTokenizer = new StringTokenizer(value, GlobalConst.CLOB_SEPARATOR_IMAGE_ROWS);
        rowSize = readTokenizer.countTokens();
        if (readTokenizer != null) {
            valArray = new Object[rowSize];
            int i = 0;
            while (readTokenizer.hasMoreTokens()) {
                valArray[i++] = readTokenizer.nextToken().trim().split(GlobalConst.CLOB_SEPARATOR_IMAGE_COLS);
            }
            if (rowSize > 0) {
                colSize = ((String[]) valArray[0]).length;
            }
        }
        Double[][] dvalueArr = null;
        Byte[][] cvalueArr = null;
        Integer[][] lvalueArr = null;
        Short[][] svalueArr = null;
        Boolean[][] bvalueArr = null;
        Float[][] fvalueArr = null;
        String[][] stvalueArr = null;
        switch(dataType) {
            case TangoConst.Tango_DEV_BOOLEAN:
                bvalueArr = new Boolean[rowSize][colSize];
                for (int i = 0; i < rowSize; i++) {
                    for (int j = 0; j < colSize; j++) {
                        try {
                            if (((String[]) valArray[i])[j] == null || "".equals(((String[]) valArray[i])[j]) || "null".equals(((String[]) valArray[i])[j]) || "NaN".equalsIgnoreCase(((String[]) valArray[i])[j])) {
                                bvalueArr[i][j] = null;
                            } else {
                                bvalueArr[i][j] = new Boolean(Double.valueOf(((String[]) valArray[i])[j].trim()).intValue() != 0);
                            }
                        } catch (final NumberFormatException n) {
                            bvalueArr[i][j] = new Boolean("true".equalsIgnoreCase(((String[]) valArray[i])[j].trim()));
                        }
                    }
                }
                return bvalueArr;
            case TangoConst.Tango_DEV_CHAR:
            case TangoConst.Tango_DEV_UCHAR:
                cvalueArr = new Byte[rowSize][colSize];
                for (int i = 0; i < valArray.length; i++) {
                    for (int j = 0; j < colSize; j++) {
                        try {
                            if (((String[]) valArray[i])[j] == null || "".equals(((String[]) valArray[i])[j]) || "null".equals(((String[]) valArray[i])[j]) || "NaN".equalsIgnoreCase(((String[]) valArray[i])[j])) {
                                cvalueArr[i][j] = null;
                            } else {
                                cvalueArr[i][j] = Byte.valueOf(((String[]) valArray[i])[j].trim());
                            }
                        } catch (final NumberFormatException n) {
                            cvalueArr[i][j] = new Byte(Double.valueOf(((String[]) valArray[i])[j].trim()).byteValue());
                        }
                    }
                }
                return cvalueArr;
            case TangoConst.Tango_DEV_STATE:
            case TangoConst.Tango_DEV_LONG:
            case TangoConst.Tango_DEV_ULONG:
                lvalueArr = new Integer[rowSize][colSize];
                for (int i = 0; i < valArray.length; i++) {
                    for (int j = 0; j < colSize; j++) {
                        try {
                            if (((String[]) valArray[i])[j] == null || "".equals(((String[]) valArray[i])[j]) || "null".equals(((String[]) valArray[i])[j]) || "NaN".equalsIgnoreCase(((String[]) valArray[i])[j])) {
                                lvalueArr[i][j] = null;
                            } else {
                                lvalueArr[i][j] = Integer.valueOf(((String[]) valArray[i])[j].trim());
                            }
                        } catch (final NumberFormatException n) {
                            lvalueArr[i][j] = new Integer(Double.valueOf(((String[]) valArray[i])[j].trim()).intValue());
                        }
                    }
                }
                return lvalueArr;
            case TangoConst.Tango_DEV_SHORT:
            case TangoConst.Tango_DEV_USHORT:
                svalueArr = new Short[rowSize][colSize];
                for (int i = 0; i < valArray.length; i++) {
                    for (int j = 0; j < colSize; j++) {
                        try {
                            if (((String[]) valArray[i])[j] == null || "".equals(((String[]) valArray[i])[j]) || "null".equals(((String[]) valArray[i])[j]) || "NaN".equalsIgnoreCase(((String[]) valArray[i])[j])) {
                                svalueArr[i][j] = null;
                            } else {
                                svalueArr[i][j] = Short.valueOf(((String[]) valArray[i])[j].trim());
                            }
                        } catch (final NumberFormatException n) {
                            svalueArr[i][j] = new Short(Double.valueOf(((String[]) valArray[i])[j].trim()).shortValue());
                        }
                    }
                }
                return svalueArr;
            case TangoConst.Tango_DEV_FLOAT:
                fvalueArr = new Float[rowSize][colSize];
                for (int i = 0; i < valArray.length; i++) {
                    for (int j = 0; j < colSize; j++) {
                        try {
                            if (((String[]) valArray[i])[j] == null || "".equals(((String[]) valArray[i])[j]) || "null".equals(((String[]) valArray[i])[j]) || "NaN".equalsIgnoreCase(((String[]) valArray[i])[j])) {
                                fvalueArr[i][j] = null;
                            } else {
                                fvalueArr[i][j] = Float.valueOf(((String[]) valArray[i])[j].trim());
                            }
                        } catch (final NumberFormatException n) {
                            fvalueArr[i][j] = new Float(Double.valueOf(((String[]) valArray[i])[j].trim()).floatValue());
                        }
                    }
                }
                return fvalueArr;
            case TangoConst.Tango_DEV_STRING:
                stvalueArr = new String[rowSize][colSize];
                for (int i = 0; i < valArray.length; i++) {
                    for (int j = 0; j < colSize; j++) {
                        if (((String[]) valArray[i])[j] == null || "".equals(((String[]) valArray[i])[j]) || "null".equals(((String[]) valArray[i])[j]) || "NaN".equalsIgnoreCase(((String[]) valArray[i])[j])) {
                            stvalueArr[i][j] = null;
                        } else {
                            stvalueArr[i][j] = StringFormater.formatStringToRead(new String(((String[]) valArray[i])[j].trim()));
                        }
                    }
                }
                return stvalueArr;
            case TangoConst.Tango_DEV_DOUBLE:
            default:
                dvalueArr = new Double[rowSize][colSize];
                for (int i = 0; i < valArray.length; i++) {
                    for (int j = 0; j < colSize; j++) {
                        if (((String[]) valArray[i])[j] == null || "".equals(((String[]) valArray[i])[j]) || "null".equals(((String[]) valArray[i])[j]) || "NaN".equalsIgnoreCase(((String[]) valArray[i])[j])) {
                            dvalueArr[i][j] = null;
                        } else {
                            dvalueArr[i][j] = Double.valueOf(((String[]) valArray[i])[j].trim());
                        }
                    }
                }
                return dvalueArr;
        }
    }

    @Override
    public boolean isLastDataNull(final String attributName) throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        final IAdtAptAttributes att = AdtAptAttributesFactory.getInstance(archType);
        if (dbConn == null || att == null) {
            return false;
        }
        final int[] tfw = att.getAtt_TFW_Data(attributName);
        final int format = tfw[1];
        final int writable = tfw[2];
        final boolean roFields = writable == AttrWriteType._READ || writable == AttrWriteType._WRITE;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        final String tableName = dbConn.getSchema() + "." + getTableName(attributName);
        final String fields = roFields ? toDbTimeFieldString(ConfigConst.TAB_SCALAR_RO[0]) + ", " + ConfigConst.TAB_SCALAR_RO[1] : toDbTimeFieldString(ConfigConst.TAB_SCALAR_RW[0]) + ", " + ConfigConst.TAB_SCALAR_RW[1] + ", " + ConfigConst.TAB_SCALAR_RW[2];
        final String query = "SELECT " + fields + " FROM " + tableName;
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(query);
            rset.next();
            boolean ret;
            if (roFields) {
                if (format == AttrDataFormat._SCALAR) {
                    ret = rset.getString(2) == null || rset.getString(2).equals("");
                } else {
                    ret = rset.getClob(2) == null || rset.getClob(2).equals("");
                }
            } else {
                if (format == AttrDataFormat._SCALAR) {
                    ret = rset.getString(2) == null || rset.getString(2).equals("");
                    ret = ret && (rset.getString(3) == null || rset.getString(3).equals(""));
                } else {
                    ret = rset.getClob(2) == null || rset.getClob(2).equals("");
                    ret = ret && (rset.getClob(3) == null || rset.getClob(3).equals(""));
                }
            }
            return ret;
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
    }

    /**
     * This method returns a String array which contains as first element the
     * time value and the second element indicates if the corresponding value is
     * null or not
     * 
     * @param completeName
     *            : attribute full_name
     * @param dataFormat
     *            : AttrDataFormat._SCALAR or AttrDataFormat._SPECTRUM ot
     *            AttrDataFormat._IMAGE
     * @param writable
     *            AttrWriteType._READ_WRITE or AttrWriteType._READ or
     *            AttrWriteType._WRITE
     * @return couple of string with the maximum time value and the value equal
     *         to null or notnull
     * @throws ArchivingException
     */
    @Override
    public String[] getTimeValueNullOrNotOfLastInsert(final String completeName, final int dataFormat, final int writable) throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        final IAdtAptAttributes att = AdtAptAttributesFactory.getInstance(archType);
        if (dbConn == null || att == null) {
            return null;
        }
        String tableName;
        try {
            tableName = dbConn.getSchema() + "." + getTableName(completeName);
        } catch (final ArchivingException e) {
            return null;
        }
        final String query = "select " + getFormattedTimeField("") + "," + getValueColumnName(dataFormat, writable) + " from " + tableName + "  where time = (select MAX(time) from " + tableName + ")";
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        String ret[] = null;
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(query);
            if (rset.next()) {
                final String rawDate = rset.getString(1);
                if (rset.wasNull()) {
                    return null;
                }
                if (rawDate == null || "".equals(rawDate) || "null".equals(rawDate)) {
                    return null;
                }
                ret = new String[2];
                ret[0] = rawDate;
                final String readString = rset.getString(2);
                if (readString == null || "".equals(readString) || "null".equals(readString)) {
                    ret[1] = "null";
                } else {
                    ret[1] = "notnull";
                }
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return ret;
    }

    /**
     * 
     * @param type
     * @return
     */
    public static int getHdbTdbType(final int type) {
        switch(type) {
            case ConfigConst.TDB_MYSQL:
            case ConfigConst.TDB_ORACLE:
                return ConfigConst.TDB;
            case ConfigConst.HDB_MYSQL:
            case ConfigConst.HDB_ORACLE:
                return ConfigConst.HDB;
            default:
                return -1;
        }
    }

    /**
     * 
     * @param type
     * @return
     */
    public static int getDbType(final int type) {
        switch(type) {
            case ConfigConst.TDB_MYSQL:
            case ConfigConst.HDB_MYSQL:
                return ConfigConst.BD_MYSQL;
            case ConfigConst.TDB_ORACLE:
            case ConfigConst.HDB_ORACLE:
                return ConfigConst.BD_ORACLE;
            default:
                return -1;
        }
    }

    public static int getArchivingType(final int type, final int bd) {
        if (type == ConfigConst.HDB) {
            return bd == ConfigConst.BD_MYSQL ? ConfigConst.HDB_MYSQL : ConfigConst.HDB_ORACLE;
        } else {
            return bd == ConfigConst.BD_MYSQL ? ConfigConst.TDB_MYSQL : ConfigConst.TDB_ORACLE;
        }
    }

    public static String getPoolName(final int archType, final String user) {
        if (archType == ConfigConst.HDB) {
            return "HDB_" + user;
        } else {
            return "TDB_" + user;
        }
    }
}
