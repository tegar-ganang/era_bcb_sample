package fr.soleil.hdbtdbArchivingApi.ArchivingApi.AttributesManagement.AttributeExtractor.DataGetters;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import fr.esrf.Tango.AttrWriteType;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.ConfigConst;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.AttributesManagement.AdtAptAttributes.AdtAptAttributesFactory;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.AttributesManagement.AdtAptAttributes.IAdtAptAttributes;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbCommands.ConnectionCommands;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbConnection.ConnectionFactory;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseManagement.DbConnection.IDBConnection;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseUtils.DbUtilsFactory;
import fr.soleil.hdbtdbArchivingApi.ArchivingApi.DataBaseUtils.IDbUtils;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.ArchivingException;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.DateUtil;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.ImageEvent_RO;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.SpectrumEvent_RO;
import fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools.SpectrumEvent_RW;

/**
 * @author AYADI
 * 
 */
public class OracleDataGetters extends DataGetters {

    /**
     * @param con
     * @param ut
     * @param at
     */
    public OracleDataGetters(final int type) {
        super(type);
    }

    @Override
    protected Vector treatStatementResultForGetSpectData(final ResultSet rset, final boolean isBothReadAndWrite, final int dataType, final Vector spectrumS) throws ArchivingException {
        final IDbUtils dbUtils = DbUtilsFactory.getInstance(archType);
        if (dbUtils == null) {
            return null;
        }
        try {
            while (rset.next()) {
                final SpectrumEvent_RO spectrumEventRO = new SpectrumEvent_RO();
                final SpectrumEvent_RW spectrumEventRW = new SpectrumEvent_RW();
                final String rawDate = rset.getString(1);
                final long milliDate = DateUtil.stringToMilli(rawDate);
                spectrumEventRO.setTimeStamp(milliDate);
                spectrumEventRW.setTimeStamp(milliDate);
                final int dimX = rset.getInt(2);
                spectrumEventRO.setDim_x(dimX);
                spectrumEventRW.setDim_x(dimX);
                Clob readClob = null;
                String readString = null;
                readClob = rset.getClob(3);
                if (rset.wasNull()) {
                    readString = "null";
                } else {
                    readString = readClob.getSubString(1, (int) readClob.length());
                }
                Clob writeClob = null;
                String writeString = null;
                if (isBothReadAndWrite) {
                    writeClob = rset.getClob(4);
                    if (rset.wasNull()) {
                        writeString = "null";
                    } else {
                        writeString = writeClob.getSubString(1, (int) writeClob.length());
                    }
                }
                final Object value = dbUtils.getSpectrumValue(readString, writeString, dataType);
                if (isBothReadAndWrite) {
                    spectrumEventRW.setValue(value);
                    spectrumS.add(spectrumEventRW);
                } else {
                    spectrumEventRO.setValue(value);
                    spectrumS.add(spectrumEventRO);
                }
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        }
        return spectrumS;
    }

    @Override
    protected Vector treatStatementResultForGetImageData(final ResultSet rset, final boolean isBothReadAndWrite, final int dataType, final Vector imageS) throws ArchivingException {
        final IDbUtils dbUtils = DbUtilsFactory.getInstance(archType);
        if (dbUtils == null) {
            return null;
        }
        try {
            while (rset.next()) {
                final ImageEvent_RO imageEventRO = new ImageEvent_RO();
                imageEventRO.setTimeStamp(DateUtil.stringToMilli(rset.getString(1)));
                final int dimX = rset.getInt(2);
                final int dimY = rset.getInt(3);
                imageEventRO.setDim_x(dimX);
                imageEventRO.setDim_y(dimY);
                String readString;
                final Clob readClob = rset.getClob(4);
                if (rset.wasNull()) {
                    readString = "null";
                } else {
                    readString = readClob.getSubString(1, (int) readClob.length());
                }
                if (isBothReadAndWrite) {
                } else {
                    imageEventRO.setValue(dbUtils.getImageValue(readString, dataType));
                    imageS.add(imageEventRO);
                }
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        }
        return imageS;
    }

    @Override
    protected String getDbScalarLast_nRequest(final String tableName, final String attributeName, final int number, final boolean roFields, final String fields) throws ArchivingException {
        final String orderField = roFields ? ConfigConst.TAB_SCALAR_RO[0] : ConfigConst.TAB_SCALAR_RW[0];
        return "SELECT " + fields + " FROM " + "(" + "SELECT * FROM " + tableName + " ORDER BY " + orderField + " DESC" + ")" + " WHERE rownum <= " + number + " ORDER BY  " + orderField + " ASC";
    }

    @Override
    public Vector getAttSpectrumDataLast_n(final String attributeName, final int number, final int writable) throws ArchivingException {
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        final IAdtAptAttributes att = AdtAptAttributesFactory.getInstance(archType);
        final IDbUtils dbUtils = DbUtilsFactory.getInstance(archType);
        if (dbConn == null || att == null || dbUtils == null) {
            return null;
        }
        final Vector tmpSpectrumVect = new Vector();
        final int data_type = att.getAtt_TFW_Data(attributeName)[0];
        final boolean roFields = writable == AttrWriteType._READ || writable == AttrWriteType._WRITE;
        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;
        String tableName = dbConn.getSchema() + "." + dbUtils.getTableName(attributeName);
        String selectField0;
        String selectField1;
        String selectField2;
        String selectField3 = null;
        String selectFields;
        String orderField;
        if (roFields) {
            selectField0 = ConfigConst.TAB_SPECTRUM_RO[0];
            selectField1 = ConfigConst.TAB_SPECTRUM_RO[1];
            selectField2 = ConfigConst.TAB_SPECTRUM_RO[2];
        } else {
            selectField0 = ConfigConst.TAB_SPECTRUM_RW[0];
            selectField1 = ConfigConst.TAB_SPECTRUM_RW[1];
            selectField2 = ConfigConst.TAB_SPECTRUM_RW[2];
            selectField3 = ConfigConst.TAB_SPECTRUM_RW[3];
        }
        selectFields = selectField0 + ", " + selectField1 + ", " + selectField2;
        orderField = roFields ? ConfigConst.TAB_SPECTRUM_RO[0] : ConfigConst.TAB_SPECTRUM_RW[0];
        final String whereClause = "rownum" + " <= " + number + " ORDER BY  " + orderField + " ASC";
        final String query1 = "SELECT * FROM " + tableName + " ORDER BY " + selectField0 + " DESC";
        tableName = dbConn.getSchema() + "." + dbUtils.getTableName(attributeName) + " T";
        if (roFields) {
            selectField0 = "T" + "." + ConfigConst.TAB_SPECTRUM_RO[0];
            selectField1 = "T" + "." + ConfigConst.TAB_SPECTRUM_RO[1];
            selectField2 = "T" + "." + ConfigConst.TAB_SPECTRUM_RO[2];
        } else {
            selectField0 = "T" + "." + ConfigConst.TAB_SPECTRUM_RW[0];
            selectField1 = "T" + "." + ConfigConst.TAB_SPECTRUM_RW[1];
            selectField2 = "T" + "." + ConfigConst.TAB_SPECTRUM_RW[2];
            selectField3 = "T" + "." + ConfigConst.TAB_SPECTRUM_RW[3];
        }
        selectFields = dbUtils.toDbTimeFieldString(selectField0) + ", " + selectField1 + ", " + selectField2;
        if (!roFields) {
            selectFields += ", " + selectField3;
        }
        final String query = "SELECT " + selectFields + " FROM (" + query1 + ") T WHERE " + whereClause;
        try {
            conn = dbConn.getConnection();
            stmt = conn.createStatement();
            rset = stmt.executeQuery(query);
            while (rset.next()) {
                final SpectrumEvent_RO spectrumEventRO = new SpectrumEvent_RO();
                final SpectrumEvent_RW spectrumEventRW = new SpectrumEvent_RW();
                try {
                    spectrumEventRO.setTimeStamp(DateUtil.stringToMilli(rset.getString(1)));
                    spectrumEventRW.setTimeStamp(DateUtil.stringToMilli(rset.getString(1)));
                } catch (final Exception e) {
                }
                final int dimX = rset.getInt(2);
                spectrumEventRO.setDim_x(dimX);
                spectrumEventRW.setDim_x(dimX);
                Clob readClob = null;
                String readString = null;
                readClob = rset.getClob(3);
                if (rset.wasNull()) {
                    readString = "null";
                } else {
                    readString = readClob.getSubString(1, (int) readClob.length());
                }
                Clob writeClob = null;
                String writeString = null;
                if (!roFields) {
                    writeClob = rset.getClob(4);
                    if (rset.wasNull()) {
                        writeString = "null";
                    } else {
                        writeString = writeClob.getSubString(1, (int) writeClob.length());
                    }
                }
                final Object value = dbUtils.getSpectrumValue(readString, writeString, data_type);
                if (!roFields) {
                    spectrumEventRW.setValue(value);
                    tmpSpectrumVect.add(spectrumEventRW);
                } else {
                    spectrumEventRO.setValue(value);
                    tmpSpectrumVect.add(spectrumEventRO);
                }
            }
        } catch (final SQLException e) {
            throw new ArchivingException(e);
        } finally {
            ConnectionCommands.close(rset);
            ConnectionCommands.close(stmt);
            dbConn.closeConnection(conn);
        }
        return tmpSpectrumVect;
    }

    @Override
    public void buildAttributeTab(final String tableName, final int data_type, final int data_format, final int writable) throws ArchivingException {
    }

    @Override
    public String getNearestValueQuery(final String attributeName, final String timestamp) throws ArchivingException {
        final IDbUtils dbUtils = DbUtilsFactory.getInstance(archType);
        final IDBConnection dbConn = ConnectionFactory.getInstance(archType);
        final String tableName = dbConn.getSchema() + "." + dbUtils.getTableName(attributeName);
        final int[] tfw = AdtAptAttributesFactory.getInstance(archType).getAtt_TFW_Data(attributeName);
        final int writeType = tfw[2];
        final boolean roFields = writeType == AttrWriteType._READ || writeType == AttrWriteType._WRITE;
        String selectFields;
        final String time = ConfigConst.TAB_SCALAR_RO[0];
        if (roFields) {
            selectFields = time + ", " + ConfigConst.TAB_SCALAR_RO[1];
        } else {
            selectFields = time + ", " + ConfigConst.TAB_SCALAR_RW[1];
        }
        final String dateFormat = "'DD-MM-YYYY HH24:MI:SS'";
        final String query = "SELECT " + selectFields + " FROM " + tableName + " WHERE ( ABS ( TO_DATE('" + timestamp + "'," + dateFormat + ") - TO_DATE(to_char(" + time + ", " + dateFormat + "), " + dateFormat + "))) = (SELECT MIN(ABS ( TO_DATE ('" + timestamp + "'," + dateFormat + ") - TO_DATE(to_char(" + time + ", " + dateFormat + "), " + dateFormat + "))) FROM " + tableName + ")";
        return query;
    }
}
