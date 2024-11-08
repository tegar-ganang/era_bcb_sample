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
import cn.ekuma.epos.symmetricds.bean.Channel;

public class ChannelDAO extends BaseDAO<Channel> {

    public ChannelDAO(I_Session s) {
        super(s);
    }

    @Override
    public TableDefinition getTable() {
        return new TableDefinition(s, AppConfig.getAppProperty(AppConfig.TABLE_PREFIX) + "_" + "node", new Field[] { new Field("channel_id", Datas.STRING, Formats.STRING), new Field("processing_order", Datas.INT, Formats.INT), new Field("max_batch_size", Datas.INT, Formats.INT), new Field("max_batch_to_send", Datas.INT, Formats.INT), new Field("max_data_to_route", Datas.INT, Formats.INT), new Field("extract_period_millis", Datas.INT, Formats.INT), new Field("enabled", Datas.BOOLEAN, Formats.BOOLEAN), new Field("use_old_data_to_route", Datas.BOOLEAN, Formats.BOOLEAN), new Field("use_row_data_to_route", Datas.BOOLEAN, Formats.BOOLEAN), new Field("use_pk_data_to_route", Datas.BOOLEAN, Formats.BOOLEAN), new Field("contains_big_lob", Datas.BOOLEAN, Formats.BOOLEAN), new Field("batch_algorithm", Datas.STRING, Formats.STRING) }, new int[] { 0 });
    }

    @Override
    public Channel readValues(DataRead dr, Channel obj) throws BasicException {
        if (obj == null) obj = new Channel();
        obj.setChannelId(dr.getString(1));
        obj.setProcessingOrder(dr.getInt(2));
        obj.setMaxBatchSize(dr.getInt(3));
        obj.setMaxBatchToSend(dr.getInt(4));
        obj.setMaxDataToRoute(dr.getInt(5));
        obj.setExtractPeriodMillis(dr.getInt(6));
        obj.setEnabled(dr.getBoolean(7));
        obj.setUseOldDataToRoute(dr.getBoolean(8));
        obj.setUseRowDataToRoute(dr.getBoolean(9));
        obj.setUsePkDataToRoute(dr.getBoolean(10));
        obj.setContainsBigLob(dr.getBoolean(11));
        obj.setBatchAlgorithm(dr.getString(12));
        return obj;
    }

    @Override
    public void writeInsertValues(DataWrite dp, Channel obj) throws BasicException {
        dp.setString(1, obj.getChannelId());
        dp.setInt(2, obj.getProcessingOrder());
        dp.setInt(3, obj.getMaxBatchSize());
        dp.setInt(4, obj.getMaxBatchToSend());
        dp.setInt(5, obj.getMaxDataToRoute());
        dp.setInt(6, (int) obj.getExtractPeriodMillis());
        dp.setBoolean(7, obj.isEnabled());
        dp.setBoolean(8, obj.isUseOldDataToRoute());
        dp.setBoolean(9, obj.isUseRowDataToRoute());
        dp.setBoolean(10, obj.isUsePkDataToRoute());
        dp.setBoolean(11, obj.isContainsBigLob());
        dp.setString(12, obj.getBatchAlgorithm());
    }

    /**
	 * <table name="channel" description="This table represents a category of data that can be synchronized independently of other channels. Channels allow control over the type of data flowing and prevents one type of synchronization from contending with another.">
        <column name="channel_id" type="VARCHAR" size="20" required="true" primaryKey="true" description="A unique identifer, usually named something meaningful, like 'sales' or 'inventory'."/>
        <column name="processing_order" type="INTEGER" required="true" default="1" description="Order of sequence to process channel data."/>
        <column name="max_batch_size" type="INTEGER" required="true" default="1000" description="The maximum number of Data Events to process within a batch for this channel."/>
        <column name="max_batch_to_send" type="INTEGER" required="true" default="60" description="The maximum number of batches to send during a 'synchronization' between two nodes. A 'synchronization' is equivalent to a push or a pull. If there are 12 batches ready to be sent for a channel and max_batch_to_send is equal to 10, then only the first 10 batches will be sent." />
        <column name="max_data_to_route" type="INTEGER" required="true" default="100000" description="The maximum number of data rows to route for a channel at a time." />        
        <column name="extract_period_millis" type="INTEGER" required="true" default="0" description="The minimum number of milliseconds allowed between attempts to extract data for targeted at a node_id."/>
        <column name="enabled" type="BOOLEANINT" size="1" required="true" default="1" description="Indicates whether channel is enabled or not."/>
        <column name="use_old_data_to_route" type="BOOLEANINT" size="1" required="true" default="1" description="Indicates whether to read the old data during routing."/>
        <column name="use_row_data_to_route" type="BOOLEANINT" size="1" required="true" default="1" description="Indicates whether to read the row data during routing."/>
        <column name="use_pk_data_to_route" type="BOOLEANINT" size="1" required="true" default="1" description="Indicates whether to read the pk data during routing."/>
        <column name="contains_big_lob" type="BOOLEANINT" size="1" required="true" default="0" description="Provides SymmetricDS a hint as to whether this channel will contain big lobs data.  Some databases have shortcuts that SymmetricDS can take advantage of if it knows that the lob columns in sym_data aren't going to contain large lobs.  The definition of how big a 'large' lob is will differ from database to database."/>        
        <column name="batch_algorithm" type="VARCHAR" size="50" required="true" default="default" description="The algorithm to use when batching data on this channel.  Possible values are: 'default', 'transactional', and 'nontransactional'"/>
        <column name="description" type="VARCHAR" size="255" description="Description on the type of data carried in this channel."/>
    </table>
	 */
    @Override
    public Class getSuportClass() {
        return Channel.class;
    }
}
