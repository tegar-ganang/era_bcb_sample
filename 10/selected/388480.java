package org.riverock.generic.db.definition;

import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.schema.db.CustomSequenceType;
import org.riverock.generic.schema.db.DataDefinitionActionDataListType;
import org.apache.log4j.Logger;
import java.sql.PreparedStatement;

/**
 * User: Admin
 * Date: May 20, 2003
 * Time: 9:49:58 PM
 *
 * $Id: AddRecordToList.java,v 1.4 2006/06/05 19:18:25 serg_main Exp $
 */
public class AddRecordToList implements DefinitionProcessingInterface {

    private static Logger log = Logger.getLogger("org.riverock.generic.db.definition.AddRecordToList");

    public AddRecordToList() {
    }

    public void processAction(DatabaseAdapter db_, DataDefinitionActionDataListType parameters) throws Exception {
        PreparedStatement ps = null;
        try {
            if (log.isDebugEnabled()) log.debug("db connect - " + db_.getClass().getName());
            String seqName = DefinitionService.getString(parameters, "sequence_name", null);
            if (seqName == null) {
                String errorString = "Name of sequnce not found";
                log.error(errorString);
                throw new Exception(errorString);
            }
            String tableName = DefinitionService.getString(parameters, "name_table", null);
            if (tableName == null) {
                String errorString = "Name of table not found";
                log.error(errorString);
                throw new Exception(errorString);
            }
            String columnName = DefinitionService.getString(parameters, "name_pk_field", null);
            if (columnName == null) {
                String errorString = "Name of column not found";
                log.error(errorString);
                throw new Exception(errorString);
            }
            CustomSequenceType seqSite = new CustomSequenceType();
            seqSite.setSequenceName(seqName);
            seqSite.setTableName(tableName);
            seqSite.setColumnName(columnName);
            long seqValue = db_.getSequenceNextValue(seqSite);
            String valueColumnName = DefinitionService.getString(parameters, "name_value_field", null);
            if (columnName == null) {
                String errorString = "Name of valueColumnName not found";
                log.error(errorString);
                throw new Exception(errorString);
            }
            String insertValue = DefinitionService.getString(parameters, "insert_value", null);
            if (columnName == null) {
                String errorString = "Name of insertValue not found";
                log.error(errorString);
                throw new Exception(errorString);
            }
            String sql = "insert into " + tableName + " " + "(" + columnName + "," + valueColumnName + ")" + "values" + "(?,?)";
            if (log.isDebugEnabled()) {
                log.debug(sql);
                log.debug("pk " + seqValue);
                log.debug("value " + insertValue);
            }
            ps = db_.prepareStatement(sql);
            ps.setLong(1, seqValue);
            ps.setString(2, insertValue);
            ps.executeUpdate();
            db_.commit();
        } catch (Exception e) {
            try {
                db_.rollback();
            } catch (Exception e1) {
            }
            log.error("Error insert value", e);
            throw e;
        } finally {
            org.riverock.generic.db.DatabaseManager.close(ps);
            ps = null;
        }
    }
}
