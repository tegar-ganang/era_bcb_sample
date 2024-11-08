package org.project.trunks.connection;

import java.util.*;
import java.sql.*;
import org.project.trunks.utilities.*;
import org.project.trunks.data.Field;
import org.project.trunks.data.FieldType;
import org.project.trunks.data.FieldDB;
import java.math.*;
import org.project.trunks.data.*;
import org.apache.commons.logging.*;
import org.apache.commons.logging.Log;
import java.util.regex.*;
import java.io.*;
import java.text.*;

public class ApplicDBManager extends DBManager {

    /**
	 * The logger, set to this class
	 */
    private static Log log = LogFactory.getLog(ApplicDBManager.class);

    public ApplicDBManager() {
    }

    public ApplicDBManager(String connect_id) {
        super(connect_id);
    }

    /**
	 * Load values for given query
	 * @return List of values
	 * @throws Exception
	 */
    public void executeRequestSelect(XmlSelect xs) throws Exception {
        executeRequestSelect(xs, true);
    }

    public void executeRequestSelect(XmlSelect xs, boolean gestMaxRowToRetrieve) throws Exception {
        Connection conn = null;
        try {
            try {
                conn = getConnection();
            } catch (Exception e) {
                throw new Exception("ApplicDBManager @ executeRequestSelect (getConnection()) - Exception : " + e.getMessage());
            }
            executeRequestSelect(conn, xs, gestMaxRowToRetrieve);
        } finally {
            closeConnection(conn);
        }
    }

    /**
   * Load values for given query
   * @return List of values
   * @throws Exception
   */
    public void executeRequestSelect(Connection conn, XmlSelect xs) throws Exception {
        executeRequestSelect(conn, xs, true);
    }

    public void executeRequestSelect(Connection conn, XmlSelect xs, boolean gestMaxRowToRetrieve) throws Exception {
        log.info("ApplicDBManager.executeRequestSelect - Begin ");
        PreparedStatement pStmt = null;
        ResultSet rs = null;
        Vector v = new Vector();
        try {
            String query = xs.buildSelect();
            log.info("'" + query + "'");
            Vector vParamField = new Vector();
            vParamField.addAll(xs.getVFieldParam());
            vParamField.addAll(xs.getVSupplFieldParam());
            pStmt = conn.prepareStatement(query);
            for (int i = 0; i < vParamField.size(); i++) {
                Field field = (Field) vParamField.elementAt(i);
                setPreparedStatementValue(pStmt, i + 1, field);
            }
            rs = pStmt.executeQuery();
            int cpt = 0;
            int i = 0;
            xs.setMaxRowToRetrieveReached(false);
            int maxRowToRetrieve = -1;
            if (gestMaxRowToRetrieve) {
                try {
                    maxRowToRetrieve = Integer.parseInt(ApplicationProperties.getProperty("LIST.MAX_ROW_TO_RETRIEVE"));
                } catch (Exception e) {
                }
            }
            while (rs.next()) {
                Field[] recordFields = new Field[xs.getFieldCount()];
                for (i = 0; i < xs.getFieldCount(); i++) {
                    String val = rs.getString(i + 1);
                    recordFields[i] = new Field();
                    recordFields[i].copyProperties(xs.getField(i));
                    recordFields[i].setValue(val);
                }
                v.addElement(recordFields);
                cpt++;
                if (maxRowToRetrieve != -1 && cpt >= maxRowToRetrieve) {
                    log.info("ApplicDBManager.executeRequestSelect - MaxRowToRetrieveReached !!! ");
                    xs.setMaxRowToRetrieveReached(true);
                    try {
                        pStmt = conn.prepareStatement("Select count('x') from (" + query + ")");
                        for (i = 0; i < vParamField.size(); i++) {
                            Field field = (Field) vParamField.elementAt(i);
                            setPreparedStatementValue(pStmt, i + 1, field);
                        }
                        rs = pStmt.executeQuery();
                        if (rs.next()) {
                            xs.setMaxRowToRetrieve(rs.getInt(1));
                            log.info("ApplicDBManager.executeRequestSelect - MaxRowToRetrieve = " + xs.getMaxRowToRetrieve());
                        }
                    } catch (Exception e) {
                        log.error("ApplicDBManager.executeRequestSelect - MaxRowToRetrieve - EXCEPTION : '" + e.getMessage() + "'");
                    }
                    break;
                }
            }
            xs.setVRecord(v);
            xs.setVSelected(new boolean[v.size()]);
            log.info("ApplicDBManager.executeRequestSelect - nbValues : " + cpt + " v.size() = " + v.size());
            log.info("ApplicDBManager.executeRequestSelect - nbFields : " + i);
            log.info("ApplicDBManager.executeRequestSelect - End ");
        } catch (Exception e) {
            throw new Exception("ApplicDBManager.executeRequestSelect Exception : " + e.getMessage());
        } finally {
            closeQueryObjects(rs, pStmt);
        }
    }

    /**
   * Execute a statement from values which passed in a vector
   * @param pStmt Reference to the PreparedStatement
   * @param i Parameter number of this PreparedStatement
   * @param value Parameter value
   * @param typeCol Column i type
  */
    protected void setPreparedStatementValue(PreparedStatement pStmt, int i, Field field) throws Exception {
        setPreparedStatementValue(pStmt, i, field, false);
    }

    protected void setPreparedStatementValue(PreparedStatement pStmt, int i, Field field, boolean insertMode) throws Exception {
        try {
            String value = StringUtilities.getEString(field.getForcedValue(), field.getValue());
            if (insertMode) value = StringUtilities.getEString(value, field.getDefaultValue());
            value = encodeValue(value);
            if (StringUtilities.stringToBoolean((String) field.getCustomObjectParams("SAVE.UPPER"))) value = value.toUpperCase();
            if (StringUtilities.stringToBoolean((String) field.getCustomObjectParams("SAVE.LOWER"))) value = value.toLowerCase();
            if (field.getKind().equals(FieldKind.PASSWORD)) log.info("ApplicDBManager.setPreparedStatementValue(" + i + ", '{" + StringUtilities.repeat("*", value.length()) + "}')"); else log.info("ApplicDBManager.setPreparedStatementValue(" + i + ", '" + value + "')");
            if (field.getType().equals(FieldType.DATE)) {
                if (value == null || value.equals("")) pStmt.setNull(i, Types.DATE); else if (value.equalsIgnoreCase("#SYSDATE#")) {
                    String sSysdate = new DateUtil().currentTimeToString(DateUtil.completeFormatter_ss);
                    java.sql.Timestamp tsValue = DateUtil.stringToSqlTimestamp(sSysdate, DateUtil.completeFormatter_ss.toPattern());
                    pStmt.setObject(i, tsValue);
                    value = new SimpleDateFormat(field.getFormatDate()).format(new java.util.Date(tsValue.getTime()));
                } else {
                    pStmt.setObject(i, field.getTimestampValue(value));
                }
            } else if (field.getType().equals(FieldType.INTEGER)) {
                if (value == null || value.equals("")) pStmt.setNull(i, java.sql.Types.INTEGER); else pStmt.setInt(i, Integer.parseInt(value));
            } else if (field.getType().equals(FieldType.NUMBER)) {
                if (value == null || value.equals("")) pStmt.setNull(i, java.sql.Types.NUMERIC); else pStmt.setBigDecimal(i, new BigDecimal(value));
            } else if (field.getType().equals(FieldType.BLOB)) {
            } else if (field.getType().equals(FieldType.BOOLEAN)) {
                if (value == null || value.equals("")) pStmt.setNull(i, java.sql.Types.VARCHAR); else pStmt.setString(i, value);
            } else {
                if (value == null || value.equals("")) pStmt.setNull(i, java.sql.Types.VARCHAR); else pStmt.setString(i, value);
            }
            field.setValue(value);
        } catch (Exception ex) {
            log.info("Exception : ApplicDBManager.setPreparedStatementValue : '" + ex.getMessage() + "'");
            throw ex;
        }
    }

    /**
   * Execute an update statement from values which passed in a vector
   * @param pStmt Reference to the PreparedStatement
   * @param i Parameter number of this PreparedStatement
   * @param value Parameter value
   * @param typeCol Column i type
  */
    protected void setFieldValueFromResultSet(ResultSet rs, int i, Field field) throws Throwable {
        try {
            if (field.getType().equals(FieldType.INTEGER)) {
                int res = rs.getInt(i);
                if (rs.wasNull()) field.setValue(""); else field.setValue(Integer.toString(res));
            } else if (field.getType().equals(FieldType.NUMBER)) {
                BigDecimal bd = rs.getBigDecimal(i);
                if (rs.wasNull()) field.setValue(""); else field.setValue(StringUtilities.numberToString(bd.toString(), Math.max(rs.getMetaData().getScale(i), 2)));
            } else if (field.getType().equals(FieldType.BOOLEAN)) {
                field.setValue(StringUtilities.getNString(rs.getString(i)));
            } else if (field.getType().equals(FieldType.DATE)) {
                Timestamp ts = rs.getTimestamp(i);
                if (rs.wasNull()) {
                    field.setValue("");
                } else {
                    field.setTimestampValue(ts);
                }
            } else if (field.getType().equals(FieldType.CLOB)) {
                Clob clob = rs.getClob(i);
                if (rs.wasNull() || clob == null) field.setValue(""); else {
                    StringBuffer strOut = new StringBuffer();
                    String aux;
                    BufferedReader br = new BufferedReader(clob.getCharacterStream());
                    while ((aux = br.readLine()) != null) strOut.append(aux);
                    field.setValue(strOut.toString());
                }
            } else if (field.getType().equals(FieldType.BLOB)) {
                Blob blob = (Blob) rs.getObject(i);
                field.setValue("[BLOB_LENGTH=" + blob.length() + "]");
            } else {
                String value = StringUtilities.getNString(rs.getString(i));
                value = decodeValue(value);
                field.setValue(value);
            }
            field.setInitialValue(field.getValue());
        } catch (Throwable ex) {
            String s = "";
            try {
                s = rs.getString(i);
            } catch (Throwable e) {
            }
            log.error("Exception : ApplicDBManager.setFieldValueFromResultSet('" + field.getID() + "', '" + s + "') : '" + ex.getMessage() + "'");
            throw ex;
        }
    }

    public Vector loadValuesForLOV(FieldDB field) throws Exception {
        log.info("ApplicDBManager.loadValuesForLOV[" + field.getID() + "] - Begin ");
        Connection conn = null;
        PreparedStatement pStmt = null;
        ResultSet rs = null;
        Vector v = new Vector();
        try {
            conn = getConnection(field.getConnect_id());
            String f_join = field.getF_join();
            FieldAttribute[] attributes = field.getOptionAttributes();
            String itemChoice = field.getItemChoice();
            Vector vFieldParam = field.getVFieldParam();
            Vector vSupplFieldParam = field.getVSupplFieldParam();
            String sJoinField = "";
            Vector vJoinField = new Vector();
            if (f_join != null) vJoinField.addElement(f_join);
            if (attributes == null) attributes = new FieldAttribute[0];
            if (vFieldParam == null) vFieldParam = new Vector();
            if (vSupplFieldParam == null) vSupplFieldParam = new Vector();
            for (int i = 0; i < vJoinField.size(); i++) sJoinField += ", " + (String) vJoinField.elementAt(i);
            String query = field.buildQueryLOV();
            pStmt = conn.prepareStatement(query);
            int nbParam = vFieldParam.size();
            for (int i = 1; i < nbParam + 1; i++) {
                setPreparedStatementValue(pStmt, i, (Field) vFieldParam.elementAt(i - 1));
            }
            for (int i = nbParam + 1; i < vSupplFieldParam.size() + nbParam + 1; i++) {
                setPreparedStatementValue(pStmt, i, (Field) vSupplFieldParam.elementAt(i - 1));
            }
            rs = pStmt.executeQuery();
            if (itemChoice != null) {
                v.add(field.getDummyLabelValueBean());
            }
            while (rs.next()) {
                String optionValue = rs.getString(2);
                int startIndex = 3;
                for (int i = 0; i < vJoinField.size(); i++) optionValue += "@" + decodeValue(rs.getString(startIndex + i));
                startIndex += vJoinField.size();
                LabelValueBean[] attr = new LabelValueBean[attributes.length];
                for (int i = 0; i < attributes.length; i++) {
                    Object o = rs.getObject(attributes[i].getID());
                    String valAttr = "";
                    if (!rs.wasNull()) valAttr += decodeValue(o.toString());
                    attr[i] = new LabelValueBean(attributes[i].getID(), valAttr);
                }
                v.add(new LabelValueBean(decodeValue(rs.getString(1)), optionValue, attr));
            }
            log.info("ApplicDBManager.loadValuesForLOV[" + field.getID() + "] - nbValues : " + v.size());
            conn.commit();
            log.info("ApplicDBManager.loadValuesForLOV[" + field.getID() + "] - End ");
        } catch (Exception e) {
            try {
                log.error("<<<< ApplicDBManager * loadValuesForLOV[" + field.getID() + "] Error :" + e.getMessage());
                if (conn != null) conn.rollback();
            } catch (Exception e2) {
                throw new Exception("ApplicDBManager * loadValuesForLOV[" + field.getID() + "] Exception : " + e2.getMessage());
            }
            throw new Exception("ApplicDBManager.loadValuesForLOV[" + field.getID() + "] Exception : " + e.getMessage());
        } finally {
            closeQueryObjects(rs, pStmt);
            closeConnection(conn);
        }
        return v;
    }

    /**
		 * executeSelect
		 * @param tableName String
		 * @param expression String
		 * @param whereClause String
		 * @return String
		 * @throws java.lang.Exception
		 */
    public String executeSelect(String tableName, String expression, String whereClause) throws Exception {
        return executeSelect(tableName, expression, whereClause, null);
    }

    public String executeSelect(String tableName, String expression, String whereClause, Connection conn) throws Exception {
        log.info("<<<<<< ApplicDBManager.executeSelect >>>>>> Begin ");
        Statement stmt = null;
        ResultSet rs = null;
        boolean givenConnection = true;
        try {
            if (conn == null) {
                conn = getConnection();
                givenConnection = false;
            }
            String query = " SELECT " + expression + "   FROM " + tableName;
            if (whereClause != null && !whereClause.equals("")) query += "  WHERE " + whereClause;
            log.info("<<<<<< ApplicDBManager.executeSelect query = '" + query + "'");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            String res = null;
            if (rs.next()) {
                res = decodeValue(rs.getString(1));
            }
            log.info("<<<<<< ApplicDBManager.executeSelect res = '" + res + "'");
            if (!givenConnection) conn.commit();
            return res;
        } catch (Exception e) {
            log.error("<<<<<< ApplicDBManager.executeSelect - Error :" + e.getMessage());
            if (!givenConnection) rollback(conn);
            manageException(e, "executeSelect");
            return null;
        } finally {
            log.info("<<<<<< ApplicDBManager.executeSelect >>>>>>  End ");
            closeQueryObjects(rs, stmt);
            if (!givenConnection) closeConnection(conn);
        }
    }

    /**
   * Load data for a list Box populated by a list of Bean
   * Return n times a couple of Label / Value
   * @param tableName Name of the table in which the values will be inserted
   * @param vColName Vector which contains the name of the columns for insert operation
   * @param vTypeCol Vector which contains the type of each column to insert
   * @param vValue Vector which contains the values to insert
  */
    public void executeSelect(String tableName, Vector vField, Field fieldID) throws Throwable {
        log.info("<<<<<< ApplicDBManager.executeSelect >>>>>> Begin ");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            String listColDB = "";
            for (int i = 0; i < vField.size(); i++) {
                if (i != 0) listColDB += ", ";
                listColDB += ((Field) vField.elementAt(i)).getID();
            }
            String query = " SELECT " + listColDB + "   FROM " + tableName + "  WHERE " + fieldID.getID() + " = " + fieldID.getValue();
            log.info("<<<<<< ApplicDBManager.executeSelect query = '" + query + "'");
            stmt = conn.createStatement();
            rs = stmt.executeQuery(query);
            if (rs.next()) {
                for (int i = 0; i < vField.size(); i++) setFieldValueFromResultSet(rs, i + 1, (Field) vField.elementAt(i));
            }
            log.info("<<<<<< ApplicDBManager.executeSelect >>>>>>  End ");
        } catch (Exception e) {
            manageException(e, "executeSelect");
        } finally {
            closeQueryObjects(rs, stmt);
            closeConnection(conn);
        }
    }

    /**
   * Execute an insert statement from values which passed in a vector
   * @param tableName Name of the table in which the values will be inserted
   * @param vField Vector which contains the field for insert operation
  */
    public synchronized void executeInsert(String tableName, Vector vField, Field pkField) throws Exception {
        log.info("<<<<<< ApplicDBManager.executeInsert >>>>>> Begin ");
        Connection conn = null;
        PreparedStatement pStmt = null;
        try {
            conn = getConnection();
            String listColDB = "(";
            String listDynParam = "(";
            for (int i = 0; i < vField.size(); i++) {
                if (i != 0) {
                    listColDB += ", ";
                    listDynParam += ", ";
                }
                listColDB += ((Field) vField.elementAt(i)).getName();
                listDynParam += "?";
            }
            listColDB += ")";
            listDynParam += ")";
            String query = " INSERT INTO " + tableName + listColDB + "  VALUES " + listDynParam;
            log.info("<<<<<< ApplicDBManager.executeInsert - query = '" + query + "'");
            pStmt = conn.prepareStatement(query);
            for (int i = 1; i < vField.size() + 1; i++) {
                setPreparedStatementValue(pStmt, i, (Field) vField.elementAt(i - 1), true);
            }
            pStmt.execute();
            conn.commit();
            if (pkField != null) {
                String res = executeSelect(tableName, "max(" + pkField.getName() + ")", null);
                pkField.setValue(res);
                pkField.setInitialValue(res);
            }
            log.info("<<<<<< ApplicDBManager.executeInsert >>>>>>  End ");
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception e2) {
                throw new Exception("ApplicDBManager * executeInsert Exception : " + e2.getMessage());
            }
            manageException(e, "executeInsert");
        } finally {
            closeQueryObjects(null, pStmt);
            closeConnection(conn);
        }
    }

    /**
   * Execute an update statement from values which passed in a vector
   * @param tableName Name of the table in which the values will be inserted
   * @param vField Vector which contains the fields for update operation
   * @param WHERE_CLAUSE Boolean indicates if there is a where clause for the update Statement
   * @param pkField Field which is the primary key
   */
    public void executeUpdate(String tableName, Vector vField, Vector vPkField) throws Exception {
        log.info("<<<<<< ApplicDBManager.executeUpdate >>>>>> Begin ");
        Connection conn = null;
        PreparedStatement pStmt = null;
        try {
            conn = getConnection();
            String listColDB = "";
            for (int i = 0; i < vField.size(); i++) {
                if (i != 0) listColDB += ", ";
                listColDB += ((Field) vField.elementAt(i)).getName() + " = ? ";
            }
            String query = " UPDATE " + tableName + "  SET " + listColDB;
            query += "  WHERE 1=1 ";
            for (int i = 0; i < vPkField.size(); i++) query += "    AND " + ((Field) vPkField.elementAt(i)).getName() + " = ? ";
            log.info("<<<<<< ApplicDBManager.executeUpdate - query = '" + query + "'");
            pStmt = conn.prepareStatement(query);
            int i = 1;
            for (int j = 0; j < vField.size(); j++) setPreparedStatementValue(pStmt, i++, (Field) vField.elementAt(j));
            for (int j = 0; j < vPkField.size(); j++) setPreparedStatementValue(pStmt, i++, (Field) vPkField.elementAt(j));
            pStmt.execute();
            conn.commit();
            log.info("<<<<<< ApplicDBManager.executeUpdate >>>>>>  End ");
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception e2) {
                throw new Exception("ApplicDBManager * executeUpdate Exception : " + e2.getMessage());
            }
            manageException(e, "executeUpdate");
        } finally {
            closeQueryObjects(null, pStmt);
            closeConnection(conn);
        }
    }

    /**
   * executeDelete
   * @param tableName
   * @param vPkField
   * @throws java.lang.Exception
   */
    public void executeDelete(String tableName, Vector vPkField) throws Exception {
        executeDelete(tableName, vPkField, null, null, null);
    }

    /**
   * executeDelete
   * @param tableName
   * @param vPkField
   * @param supplWhereClause
   * @param vSupplFieldWhereClause
   * @throws java.lang.Exception
   */
    public void executeDelete(String tableName, Vector vPkField, String supplWhereClause, Vector vSupplFieldWhereClause) throws Exception {
        executeDelete(tableName, vPkField, supplWhereClause, vSupplFieldWhereClause, null);
    }

    /**
   * executeDelete
   * @param tableName
   * @param vPkField
   * @param conn
   * @throws java.lang.Exception
   */
    public int executeDelete(String tableName, Vector vPkField, Connection conn) throws Exception {
        return executeDelete(tableName, vPkField, null, null, conn);
    }

    /**
   * executeDelete
   * @param tableName
   * @param vPkField
   * @param supplWhereClause
   * @param vSupplFieldWhereClause
   * @param conn
   * @throws java.lang.Exception
   */
    public int executeDelete(String tableName, Vector vPkField, String supplWhereClause, Vector vSupplFieldWhereClause, Connection conn) throws Exception {
        log.info("<<<<<< ApplicDBManager.executeDelete >>>>>> Begin ");
        boolean givenConnection = true;
        PreparedStatement pStmt = null;
        try {
            if (conn == null) {
                conn = getConnection();
                givenConnection = false;
            }
            String query = " DELETE FROM " + tableName;
            query += "  WHERE 1=1 ";
            for (int i = 0; i < vPkField.size(); i++) query += "    AND " + StringUtilities.getStringExtracted(((Field) vPkField.elementAt(i)).getName()) + " = ? ";
            if (!StringUtilities.getNString(supplWhereClause).equals("")) {
                query += supplWhereClause;
            }
            log.info("<<<<<< ApplicDBManager.executeDelete - query = '" + query + "'");
            pStmt = conn.prepareStatement(query);
            int cptParam = 1;
            for (int i = 0; i < vPkField.size(); i++) setPreparedStatementValue(pStmt, cptParam++, (Field) vPkField.elementAt(i));
            if (vSupplFieldWhereClause != null) {
                for (int i = 0; i < vSupplFieldWhereClause.size(); i++) setPreparedStatementValue(pStmt, cptParam++, (Field) vSupplFieldWhereClause.elementAt(i));
            }
            int nb = pStmt.executeUpdate();
            if (!givenConnection) conn.commit();
            log.info("<<<<<< ApplicDBManager.executeDelete >>>>>>  End ");
            return nb;
        } catch (Exception e) {
            if (!givenConnection) rollback(conn);
            manageException(e, "executeDelete");
            return 0;
        } finally {
            log.info("<<<<<< ApplicDBManager.executeDelete >>>>>> End");
            closeQueryObjects(null, pStmt);
            if (!givenConnection) closeConnection(conn);
        }
    }

    /**
   * Exception manager
   * @param e Exception caught in the method calling manageException
   * @param methodName Name of the method which calls manageException
   * @exception DefaultException (Exception) With formated exception message
   */
    public void manageException(Throwable e, String methodName) throws Exception {
        manageException(e, methodName, "");
    }

    public void manageException(Throwable e, String methodName, String supplInfo) throws Exception {
        log.error(this.getClass().getName() + "." + methodName + " - EXCEPTION : '" + e.getMessage() + "'");
        if (e instanceof SQLException) {
            throw new Exception(formatMessage(e.getMessage()));
        } else {
            String msgException = StringUtilities.getNString(e.getMessage());
            String msgFormatted = formatMessage(StringUtilities.getNString(e.getMessage()));
            if (msgFormatted.equals(msgException)) msgFormatted = this.getClass().getName() + "." + methodName + " : '" + msgFormatted + "'" + " " + supplInfo;
            throw new Exception(msgFormatted);
        }
    }

    /**
	 * Extrait l'erreur du message et laisse tomber le superflu
	 * @param msg Message d'erreur
	 * @return Message d'erreur format�
	 */
    public String formatMessage(String msg) {
        String[] sep = { "[Microsoft]", "[Gestionnaire de pilotes ODBC]", "[Pilote ODBC Microsoft Access]" };
        for (int k = 0; k < sep.length; k++) {
            msg = msg.replaceAll("\\" + sep[k], "");
        }
        String context_webForm = StringUtilities.addPrefix(id_webForm, ".");
        String context_lang = StringUtilities.addPrefix(code_lang, ".");
        if (msg.startsWith("ORA-00001")) {
            msg = StringUtilities.getEString(getContextualMessage("MSG_UNIQUE_CONSTRAINT"), StringUtilities.getEString(ApplicationProperties.getProperty("MSG_UNIQUE_CONSTRAINT" + context_lang), "The data you want to add or update in the table already exists."));
        } else if (msg.startsWith("ORA-02292")) {
            msg = StringUtilities.getEString(getContextualMessage("MSG_INTEGRITY_CONSTRAINT"), StringUtilities.getEString(ApplicationProperties.getProperty("MSG_INTEGRITY_CONSTRAINT" + context_lang), "This data cannot be deleted. Its deletion would violate the database integrity."));
        } else if (msg.startsWith("ORA-01438")) {
            msg = StringUtilities.getEString(getContextualMessage("MSG_VALUE_LARGER"), StringUtilities.getEString(ApplicationProperties.getProperty("MSG_VALUE_LARGER" + context_lang), "Value larger than specified precision allows for this column."));
        } else {
            Pattern pattern = Pattern.compile(".*ORA-20\\d\\d\\d: (.*)#.*ORA-\\d\\d\\d\\d\\d.*");
            Matcher matcher = pattern.matcher(msg.replaceAll("\\n", ""));
            if (matcher.find()) {
                return matcher.replaceAll("$1");
            }
            if (StringUtilities.stringToBoolean(ApplicationProperties.getProperty("ORACLE_ERROR_IS_GENERAL_ERROR"), false) && msg.indexOf("ORA-") != -1) {
                return msg + " #ORACLE_ERROR#";
            }
        }
        return msg;
    }

    /**
   * getContextualMessage
   * @return Message from application.properties
   */
    public String getContextualMessage(String CODE_MESSAGE) {
        String context_webForm = StringUtilities.addPrefix(id_webForm, ".");
        String context_lang = StringUtilities.addPrefix(code_lang, ".");
        return ApplicationProperties.getProperty(CODE_MESSAGE + context_webForm + context_lang);
    }

    /**
   * rsToHs
   * @param cntxt String prefix may be null or empty, in this case, no prefix will be applied
   * @param hs Hashtable H
   * @param rs ResultSet Data set - Query result
   */
    protected void rsToHs(String cntxt, Hashtable hs, ResultSet rs) throws Exception {
        if (rs.next()) {
            cntxt = StringUtilities.getNString(cntxt);
            if (StringUtilities.getNString(cntxt).equals("")) cntxt += ".";
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String colName = rs.getMetaData().getColumnName(i);
                hs.put(cntxt + colName, decodeValue(StringUtilities.getNString(rs.getString(colName))));
            }
        }
    }

    /**
   * rsToVector
   * Send ResultSet data to Vector - String[] for each row of the dataset
   * @param v Vector which will contain String[]
   * @param rs ResultSet Data
   */
    protected void rsToVector(Vector v, ResultSet rs) throws Exception {
        while (rs.next()) {
            String[] values = new String[rs.getMetaData().getColumnCount()];
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String colName = rs.getMetaData().getColumnName(i);
                values[i - 1] = decodeValue(StringUtilities.getNString(rs.getString(colName)));
            }
            v.addElement(values);
        }
    }
}
