package cn.ekuma.epos.datalogic.define.dao;

import com.openbravo.data.basic.BasicException;
import com.openbravo.data.loader.DataRead;
import com.openbravo.data.loader.DataWrite;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.I_Session;
import com.openbravo.data.loader.Session;
import com.openbravo.data.loader.TableDefinition;
import com.openbravo.data.model.Field;
import com.openbravo.format.Formats;
import cn.ekuma.data.dao.BaseDAO;
import cn.ekuma.epos.symmetricds.AppConfig;
import cn.ekuma.epos.symmetricds.bean.Trigger;

public class TriggerDAO extends BaseDAO<Trigger> {

    public TriggerDAO(I_Session s) {
        super(s);
    }

    @Override
    public Trigger readValues(DataRead dr, Trigger obj) throws BasicException {
        if (obj == null) obj = new Trigger();
        obj.setTriggerId(dr.getString(1));
        obj.setSourceCatalogName(dr.getString(2));
        obj.setSourceSchemaName(dr.getString(3));
        obj.setSourceTableName(dr.getString(4));
        obj.setChannelId(dr.getString(5));
        obj.setSyncOnUpdate(dr.getBoolean(6));
        obj.setSyncOnInsert(dr.getBoolean(7));
        obj.setSyncOnDelete(dr.getBoolean(8));
        obj.setSyncOnIncomingBatch(dr.getBoolean(9));
        obj.setNameForUpdateTrigger(dr.getString(10));
        obj.setNameForInsertTrigger(dr.getString(11));
        obj.setNameForDeleteTrigger(dr.getString(12));
        obj.setSyncOnUpdateCondition(dr.getString(13));
        obj.setSyncOnInsertCondition(dr.getString(14));
        obj.setSyncOnDeleteCondition(dr.getString(15));
        obj.setExternalSelect(dr.getString(16));
        obj.setTxIdExpression(dr.getString(17));
        obj.setExcludedColumnNames(dr.getString(18));
        obj.setCreateTime(dr.getTimestamp(19));
        obj.setLastUpdateBy(dr.getString(20));
        obj.setLastUpdateTime(dr.getTimestamp(21));
        return obj;
    }

    @Override
    public TableDefinition getTable() {
        return new TableDefinition(s, AppConfig.getAppProperty(AppConfig.TABLE_PREFIX) + "_" + "trigger", new Field[] { new Field("trigger_id", Datas.STRING, Formats.STRING), new Field("source_catalog_name", Datas.STRING, Formats.STRING), new Field("source_schema_name", Datas.STRING, Formats.STRING), new Field("source_table_name", Datas.STRING, Formats.STRING), new Field("channel_id", Datas.STRING, Formats.STRING), new Field("sync_on_update", Datas.BOOLEAN, Formats.BOOLEAN), new Field("sync_on_insert", Datas.BOOLEAN, Formats.BOOLEAN), new Field("sync_on_delete", Datas.BOOLEAN, Formats.BOOLEAN), new Field("sync_on_incoming_batch", Datas.BOOLEAN, Formats.BOOLEAN), new Field("name_for_update_trigger", Datas.STRING, Formats.STRING), new Field("name_for_insert_trigger", Datas.STRING, Formats.STRING), new Field("name_for_delete_trigger", Datas.STRING, Formats.STRING), new Field("sync_on_update_condition", Datas.STRING, Formats.STRING), new Field("sync_on_insert_condition", Datas.STRING, Formats.STRING), new Field("sync_on_delete_condition", Datas.STRING, Formats.STRING), new Field("external_select", Datas.STRING, Formats.STRING), new Field("tx_id_expression", Datas.STRING, Formats.STRING), new Field("excluded_column_names", Datas.STRING, Formats.STRING), new Field("create_time", Datas.TIMESTAMP, Formats.TIMESTAMP), new Field("last_update_by", Datas.STRING, Formats.STRING), new Field("last_update_time", Datas.TIMESTAMP, Formats.TIMESTAMP) }, new int[] { 0 });
    }

    /**
	 *  <table name="trigger" description="Configures database triggers that capture changes in the database. Configuration of which triggers are generated for which tables is stored here.  Triggers are created in a node's database if the source_node_group_id of a router is mapped to a row in this table.">
        <column name="trigger_id" type="VARCHAR" size="50" required="true" primaryKey="true"  description="Unique identifier for a trigger." />
        <column name="source_catalog_name" type="VARCHAR" size="50"  description="Optional name for the catalog the configured table is in." />
        <column name="source_schema_name" type="VARCHAR" size="50"  description="Optional name for the schema a configured table is in." />
        <column name="source_table_name" type="VARCHAR" size="50" required="true"  description="The name of the source table that will have a trigger installed to watch for data changes." />
        <column name="channel_id" type="VARCHAR" size="20" required="true"  description="The channel_id of the channel that data changes will flow through." />
        
        <column name="sync_on_update" type="BOOLEANINT" size="1" required="true" default="1"  description="Whether or not to install an update trigger." />
        <column name="sync_on_insert" type="BOOLEANINT" size="1" required="true" default="1"  description="Whether or not to install an insert trigger." />
        <column name="sync_on_delete" type="BOOLEANINT" size="1" required="true" default="1"  description="Whether or not to install an delete trigger." />
        <column name="sync_on_incoming_batch" type="BOOLEANINT" size="1" required="true" default="0"  description="Whether or not an incoming batch that loads data into this table should cause the triggers to capture data_events. Be careful turning this on, because an update loop is possible." />
        <column name="name_for_update_trigger" type="VARCHAR" size="50"  description="Override the default generated name for the update trigger." />
        
        <column name="name_for_insert_trigger" type="VARCHAR" size="50"  description="Override the default generated name for the insert trigger." />
        <column name="name_for_delete_trigger" type="VARCHAR" size="50"  description="Override the default generated name for the delete trigger." />
        <column name="sync_on_update_condition" type="LONGVARCHAR"  description="Specify a condition for the update trigger firing using an expression specific to the database." />
        <column name="sync_on_insert_condition" type="LONGVARCHAR"  description="Specify a condition for the insert trigger firing using an expression specific to the database." />
        <column name="sync_on_delete_condition" type="LONGVARCHAR"  description="Specify a condition for the delete trigger firing using an expression specific to the database." />
        
        <column name="external_select" type="LONGVARCHAR"  description="Specify a SQL select statement that returns a single result.  It will be used in the generated database trigger to populate the EXTERNAL_DATA field on the data table." />
        <column name="tx_id_expression" type="LONGVARCHAR"  description="Override the default expression for the transaction identifier that groups the data changes that were committed together." />
        <column name="excluded_column_names" type="LONGVARCHAR"  description="Specify a comma-delimited list of columns that should not be synchronized from this table.  Note that if a primary key is found in this list, it will be ignored." />
        <column name="create_time" type="TIMESTAMP" required="true"  description="Timestamp when this entry was created." />
        <column name="last_update_by" type="VARCHAR" size="50"  description="The user who last updated this entry." />
        
        <column name="last_update_time" type="TIMESTAMP" required="true"  description="Timestamp when a user last updated this entry." />
    </table>
	 */
    @Override
    public void writeInsertValues(DataWrite dp, Trigger obj) throws BasicException {
        dp.setString(1, obj.getTriggerId());
        dp.setString(2, obj.getSourceCatalogName());
        dp.setString(3, obj.getSourceSchemaName());
        dp.setString(4, obj.getSourceTableName());
        dp.setString(5, obj.getChannelId());
        dp.setBoolean(6, obj.isSyncOnUpdate());
        dp.setBoolean(7, obj.isSyncOnInsert());
        dp.setBoolean(8, obj.isSyncOnDelete());
        dp.setBoolean(9, obj.isSyncOnIncomingBatch());
        dp.setString(10, obj.getNameForUpdateTrigger());
        dp.setString(11, obj.getNameForInsertTrigger());
        dp.setString(12, obj.getNameForDeleteTrigger());
        dp.setString(13, obj.getSyncOnUpdateCondition());
        dp.setString(14, obj.getSyncOnInsertCondition());
        dp.setString(15, obj.getSyncOnDeleteCondition());
        dp.setString(16, obj.getExternalSelect());
        dp.setString(17, obj.getTxIdExpression());
        dp.setString(18, obj.getExcludedColumnNames());
        dp.setTimestamp(19, obj.getCreateTime());
        dp.setString(20, obj.getLastUpdateBy());
        dp.setTimestamp(21, obj.getLastUpdateTime());
    }

    @Override
    public Class getSuportClass() {
        return Trigger.class;
    }
}
