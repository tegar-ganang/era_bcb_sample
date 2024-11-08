package net.sourceforge.olduvai.lrac.jdbcswiftdataservice;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.jglchartutil.datamodels.SimpleDataSeries;
import net.sourceforge.olduvai.lrac.LiveRAC;
import net.sourceforge.olduvai.lrac.drawer.queries.DetailQuery;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.ActiveSourceQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.SwatchQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.records.DetailRecord;
import net.sourceforge.olduvai.lrac.genericdataservice.records.SwatchRecordInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceGroupInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;
import net.sourceforge.olduvai.lrac.jdbcswiftdataservice.records.CellSwatchRecord;
import net.sourceforge.olduvai.lrac.jdbcswiftdataservice.structure.InputChannel;
import net.sourceforge.olduvai.lrac.jdbcswiftdataservice.structure.InputChannelGroup;
import net.sourceforge.olduvai.lrac.jdbcswiftdataservice.structure.Source;
import net.sourceforge.olduvai.lrac.jdbcswiftdataservice.structure.SourceGroup;
import net.sourceforge.olduvai.lrac.logging.LogEntry;

/**
 * 
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 *
 * TODO: should handle InputChannelItemInterface objects by parsing for {@link InputChannelGroupInterface}
 * objects, and converting these into their component {@link InputChannelInterface} objects.   
 *
 */
public class SwiftJDBCReader {

    static final boolean SQLDEBUG = false;

    /**
	 * Needs to get build dynamically by the Connection dialog
	 */
    static String dbUrl;

    /**
	 * JDBC connection object, initialized in the constructor
	 */
    static Connection conn = null;

    /**
	 * Cached sourceGroupQuery statement
	 */
    static PreparedStatement sourceGroupQuery;

    /**
	 * Cached sourceQuery jdbc statement
	 */
    static PreparedStatement sourceQuery;

    /**
	 * Cached swatchQuery jdbc statements
	 */
    static PreparedStatement swatch5MinQuery;

    private static PreparedStatement swatchHourlyQuery;

    private static PreparedStatement swatchDailyQuery;

    private static PreparedStatement inputChannelQuery;

    private static PreparedStatement inputChannelGroupQuery;

    /**
	 * 
	 */
    protected SwiftJDBCReader() {
    }

    /**
	 * Retrieves the query statement for a list of object groups that 
	 * can be used for picking which objects to render in the visualization.  
	 * 
	 * @return
	 */
    private static final PreparedStatement getSourceGroupStatement() {
        if (conn == null) return null;
        if (sourceGroupQuery == null) try {
            sourceGroupQuery = conn.prepareStatement("SELECT * FROM  `vg_inv_node` WHERE  `key` =  \"name\" AND  `level` <>  \"t\" AND  `level` <>  \"o\"");
        } catch (SQLException e) {
            e.printStackTrace();
            sourceGroupQuery = null;
        }
        return sourceGroupQuery;
    }

    private static PreparedStatement getInputChannelsStatement() {
        if (conn == null) return null;
        if (inputChannelQuery == null) try {
            final String sql = "SELECT c.key, c.unit, c.label FROM vg_objchannel AS c";
            inputChannelQuery = conn.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            inputChannelQuery = null;
        }
        return inputChannelQuery;
    }

    protected List<InputChannelInterface> getInputChannels() throws IOException {
        ResultSet resultSet = readData(getInputChannelsStatement());
        List<InputChannelInterface> results = new ArrayList<InputChannelInterface>();
        if (resultSet == null) {
            System.err.println("Null or empty group list?");
            return results;
        }
        try {
            while (resultSet.next()) {
                final String channelName = resultSet.getString("key");
                final String unit = resultSet.getString("unit");
                final String label = resultSet.getString("label");
                final InputChannel channel = new InputChannel(channelName, unit, label);
                results.add(channel);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return results;
    }

    /**
	 * Parameter internalNameToInputChannel is provided to support resolving the string 
	 * names of input channels into Java {@link InputChannelInterface} objects.  In this
	 * case the contract is that the contents of this parameter are NOT modified by this method.   
	 * 
	 * @param internalNameToInputChannel
	 * @return A list containing all of the {@link InputChannelGroupInterface} objects fully associated with 
	 * their relevant {@link InputChannelInterface} objects. 
	 * 
	 * @throws IOException
	 */
    protected List<InputChannelGroupInterface> getInputChannelGroups(final Map<String, InputChannelInterface> internalNameToInputChannel) throws IOException {
        ResultSet resultSet = readData(getInputChannelGroupsStatement());
        List<InputChannelGroupInterface> output = new ArrayList<InputChannelGroupInterface>();
        HashMap<String, InputChannelGroup> groupMap = new HashMap<String, InputChannelGroup>();
        if (resultSet == null) {
            System.err.println("Null or empty group list?");
            return output;
        }
        try {
            while (resultSet.next()) {
                final String groupName = resultSet.getString("groupname");
                final String groupLabel = resultSet.getString("grouplabel");
                final String targetChannelName = resultSet.getString("channelname");
                InputChannelGroup channelGroup = groupMap.get(groupName);
                if (channelGroup == null) {
                    channelGroup = new InputChannelGroup(groupName, groupLabel, "");
                    groupMap.put(groupName, channelGroup);
                }
                final InputChannelInterface channel = internalNameToInputChannel.get(targetChannelName);
                channelGroup.addChannel(channel);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        output.addAll(groupMap.values());
        return output;
    }

    private static PreparedStatement getInputChannelGroupsStatement() {
        if (conn == null) return null;
        if (inputChannelGroupQuery == null) try {
            final String sql = "SELECT cg.groupname, cg.grouplabel, cg.channelname FROM vg_channelgroup AS cg";
            inputChannelGroupQuery = conn.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            inputChannelGroupQuery = null;
        }
        return inputChannelGroupQuery;
    }

    /**
	 * Requests a statement that retrieves a list of sources given a source group.
	 * PreparedStatement needs two parameters: sourcegroup level & id.   
	 * @param daytonaQuery
	 * @return SQL query string to retrieve the list of sources
	 */
    private static PreparedStatement getSourceStatement() {
        if (sourceQuery == null) {
            try {
                final String sql = " SELECT DISTINCT M1.id1 AS 'sourcename'" + ",GROUP_CONCAT(DISTINCT `M2`.`level2` SEPARATOR '|') AS 'grouptypes' " + ",GROUP_CONCAT(DISTINCT `M2`.`id2` SEPARATOR '|') AS 'groups' " + "FROM `vg_inv_map` AS M1 " + "RIGHT OUTER JOIN vg_inv_map AS M2 " + "ON M1.id1 = M2.id1 " + "WHERE M1.level1 = \"o\" " + "AND M1.level2 = ? " + "AND M1.id2 = ? " + "GROUP BY M1.id1";
                sourceQuery = conn.prepareStatement(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return sourceQuery;
    }

    /**
	 * Retrieve list of source groups from the server, these can be of type: 
	 * business, customer, group, location, site
	 * 
	 * @param q 
	 * @return
	 */
    protected List<SourceGroupInterface> getSourceGroups() throws IOException {
        ResultSet resultSet = readData(getSourceGroupStatement());
        List<SourceGroupInterface> result = new ArrayList<SourceGroupInterface>();
        if (resultSet == null) {
            System.err.println("Null or empty group list?");
            return result;
        }
        try {
            while (resultSet.next()) {
                final SourceGroup newGroup = SourceGroup.sgFromRS(resultSet);
                if (newGroup != null) result.add(newGroup);
            }
            resultSet.close();
        } catch (Exception e) {
            System.err.println("SQL exception reading source groups");
            throw new IOException(e.getMessage());
        }
        return result;
    }

    /**
	 * Given an ActiveSourceQueryInterface, retrieve a list of sources that match both the specified
	 * groups inside of the query, and match on individual source names (if specified).  
	 * 
	 * @param q
	 * @return
	 * @throws IOException
	 */
    protected Set<SourceInterface> getSourceSet(ActiveSourceQueryInterface q) throws IOException {
        final Set<SourceInterface> result = new HashSet<SourceInterface>();
        final List<SourceGroupInterface> groups = q.getGroupList();
        for (final Iterator<SourceGroupInterface> it = groups.iterator(); it.hasNext(); ) {
            final SourceGroupInterface group = it.next();
            final PreparedStatement stmt = getSourceStatement();
            try {
                stmt.setString(1, group.getInternalType());
                stmt.setString(2, group.getInternalName());
                final ResultSet jdbcResultSet = readData(stmt);
                if (jdbcResultSet == null) {
                    System.err.println("Null result set?");
                    continue;
                }
                while (jdbcResultSet.next()) {
                    final String sourceInternalName = jdbcResultSet.getString("sourcename");
                    final String tokenizedGroupTypes = jdbcResultSet.getString("grouptypes");
                    final String tokenizedGroups = jdbcResultSet.getString("groups");
                    final Source newSource = Source.createSource(sourceInternalName, tokenizedGroupTypes, tokenizedGroups);
                    result.add(newSource);
                }
                jdbcResultSet.close();
            } catch (Exception e) {
                System.err.println("SQL exception reading source groups");
                throw new IOException(e.getMessage());
            }
        }
        return result;
    }

    /**
	 * Retrieves a set of "swatch values" from the server by means of a JDBC query loop.  
	 * 
	 * @param q
	 * @param dataServiceDispatcher
	 * @return
	 * @throws IOException
	 */
    protected List<SwatchRecordInterface> getCellSwatchValues(SwatchQueryInterface q, DataServiceDispatcher dataServiceDispatcher) throws IOException {
        final long queryId = q.getQueryId();
        final List<SwatchRecordInterface> result = new ArrayList<SwatchRecordInterface>();
        final PreparedStatement stmt = getSwatchStatement(q);
        final List<SourceGroupInterface> groupList = q.getSourceGroupList();
        for (final Iterator<SourceGroupInterface> groupIt = groupList.iterator(); groupIt.hasNext(); ) {
            final SourceGroupInterface group = groupIt.next();
            final List<InputChannelItemInterface> channelList = q.getInputChannelItemList();
            for (final Iterator<InputChannelItemInterface> channelIt = channelList.iterator(); channelIt.hasNext(); ) {
                final InputChannelInterface channel = (InputChannelInterface) channelIt.next();
                try {
                    stmt.setString(1, group.getInternalType());
                    stmt.setString(2, group.getInternalName());
                    stmt.setString(3, channel.getInternalName());
                    stmt.setInt(4, (int) (q.getBeginDate().getTime() / 1000));
                    stmt.setInt(5, (int) (q.getEndDate().getTime() / 1000));
                    final ResultSet jdbcResultSet = readData(stmt);
                    while (jdbcResultSet.next()) {
                        final String sourceId = jdbcResultSet.getString("id");
                        final SourceInterface source = dataServiceDispatcher.getResultInterface().getActiveSource(sourceId);
                        final float min = jdbcResultSet.getFloat("min");
                        final float max = jdbcResultSet.getFloat("max");
                        final float total = jdbcResultSet.getFloat("total");
                        final int count = jdbcResultSet.getInt("count");
                        final SwatchRecordInterface record = CellSwatchRecord.createSwatchRecord(source, channel, min, max, total, count, queryId);
                        result.add(record);
                    }
                    jdbcResultSet.close();
                } catch (Exception e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        return result;
    }

    /**
	 * 
	 * @return PreparedStatement with two variables, the sourcegroup and the channelname 
	 * to be retrieved respectively. 
	 * 
	 */
    private static final PreparedStatement getSwatchStatement(SwatchQueryInterface query) {
        final String intervalDesc = query.getInterval().getDescription();
        String tableName;
        if (intervalDesc.equals(DataServiceDispatcher.INTERVAL_5MINUTE_DESC)) {
            if (swatch5MinQuery != null) return swatch5MinQuery;
            tableName = "vg_objstat";
        } else if (intervalDesc.equals(DataServiceDispatcher.INTERVAL_HOUR_DESC)) {
            if (swatchHourlyQuery != null) return swatchHourlyQuery;
            tableName = "vg_objstat_hourly";
        } else {
            if (swatchDailyQuery != null) return swatchDailyQuery;
            tableName = "vg_objstat_daily";
        }
        final String sql = "SELECT ot.id AS id, " + "MIN(val) AS min,  " + "MAX(val) as max, " + "SUM(val) AS total, " + "COUNT(*) as count " + "FROM `" + tableName + "` " + "JOIN vg_obj AS ot ON (" + tableName + ".obj = ot.internal_id) " + "WHERE " + "obj IN " + "( " + "SELECT obj FROM vg_objlookup WHERE level = ? AND id = ? " + ") " + "AND " + "channel = ( " + " SELECT internal_id FROM vg_objchannel WHERE `key` = ? " + ") " + " AND " + "( timeissued BETWEEN ? AND ? )" + "GROUP BY `obj`";
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            stmt = null;
        }
        if (intervalDesc.equals(DataServiceDispatcher.INTERVAL_5MINUTE_DESC)) {
            swatch5MinQuery = stmt;
        } else if (intervalDesc.equals(DataServiceDispatcher.INTERVAL_HOUR_DESC)) {
            swatchHourlyQuery = stmt;
        } else {
            swatchDailyQuery = stmt;
        }
        return stmt;
    }

    private static InputChannelInterface getChannelFromListByName(List<InputChannelItemInterface> channels, String channelInternalName) {
        for (Iterator<InputChannelItemInterface> it = channels.iterator(); it.hasNext(); ) {
            final InputChannelInterface channel = (InputChannelInterface) it.next();
            if (channel.getInternalName().equals(channelInternalName)) return channel;
        }
        return null;
    }

    /**
	 * 
	 * @param query
	 * @return
	 */
    protected List<DetailRecord> getChartData(DetailQuery query) throws IOException {
        final int numChannels = query.getChannelList().size();
        final PreparedStatement stmt = getChartDataStatement(query, query.getChannelList().size());
        final SourceInterface targetSource = query.getSource();
        final HashMap<String, TempSeriesHolder> channelnameToTempholder = new HashMap<String, TempSeriesHolder>();
        try {
            stmt.setString(1, targetSource.getName());
            stmt.setInt(2, (int) (query.getBeginDate().getTime() / 1000));
            stmt.setInt(3, (int) (query.getEndDate().getTime() / 1000));
            final List<InputChannelItemInterface> channelList = query.getChannelList();
            final int startOffset = 4;
            for (int i = 0; i < numChannels; i++) {
                stmt.setString(startOffset + i, channelList.get(i).getInternalName());
            }
            final ResultSet jdbcResult = readData(stmt);
            while (jdbcResult.next()) {
                final String key = jdbcResult.getString("key");
                TempSeriesHolder holder = channelnameToTempholder.get(key);
                if (holder == null) {
                    final String label = jdbcResult.getString("label");
                    final String unit = jdbcResult.getString("unit");
                    final InputChannelInterface channel = getChannelFromListByName(channelList, jdbcResult.getString("key"));
                    channelnameToTempholder.put(key, new TempSeriesHolder(channel, label, unit));
                } else {
                    holder.numVals++;
                }
            }
            for (Iterator<TempSeriesHolder> it = channelnameToTempholder.values().iterator(); it.hasNext(); ) {
                it.next().createTimeVals();
            }
            jdbcResult.beforeFirst();
            while (jdbcResult.next()) {
                final String key = jdbcResult.getString("key");
                TempSeriesHolder holder = channelnameToTempholder.get(key);
                if (holder == null) {
                    System.err.println("SwiftJDBCReader.getChartData: holder should never be null");
                } else {
                    holder.addTimeVal((double) jdbcResult.getInt("timeissued"), (double) jdbcResult.getFloat("val"));
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        final List<DetailRecord> results = new ArrayList<DetailRecord>();
        final long queryId = query.getQueryId();
        for (Iterator<TempSeriesHolder> it = channelnameToTempholder.values().iterator(); it.hasNext(); ) {
            final TempSeriesHolder tempHolder = it.next();
            final SimpleDataSeries series = new SimpleDataSeries(tempHolder.times, tempHolder.vals);
            final DetailRecord record = new DetailRecord(targetSource, tempHolder.channel, series, queryId);
            results.add(record);
        }
        return results;
    }

    /**
	 * Unfortunately this prepared statement can only be used once because of the 
	 * variable size of the input list.  See comments at bottom of 
	 * getChartData for a possible strategy to work around this using a temporary server-side 
	 * table.  
	 * 
	 * @param numberOfChannels
	 * @return
	 */
    private static final PreparedStatement getChartDataStatement(DetailQuery query, int numberOfChannels) {
        final String intervalDesc = query.getInterval().getDescription();
        String tableName;
        if (intervalDesc.equals(DataServiceDispatcher.INTERVAL_5MINUTE_DESC)) {
            tableName = "vg_stat";
        } else if (intervalDesc.equals(DataServiceDispatcher.INTERVAL_HOUR_DESC)) {
            tableName = "vg_stat_hourly";
        } else {
            tableName = "vg_stat_daily";
        }
        String sql = "SELECT timeissued, stat.key, val, unit, label " + " FROM `" + tableName + "` AS stat " + " WHERE `id` = ? " + " AND (`stat`.`timeissued` BETWEEN ? AND ? ) " + " AND `key` IN (";
        for (int i = 0; i < numberOfChannels; i++) {
            sql += "?";
            if (i < numberOfChannels - 1) sql += ", "; else sql += " ";
        }
        sql += ")";
        PreparedStatement statQuery = null;
        try {
            statQuery = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return statQuery;
    }

    static final ResultSet readData(PreparedStatement sqlStatement) throws IOException {
        if (SQLDEBUG) System.out.println(sqlStatement);
        LiveRAC.makeLogEntry(LogEntry.QUERY, "Query to " + dbUrl, sqlStatement);
        ResultSet result = null;
        try {
            boolean isResult;
            isResult = sqlStatement.execute();
            if (isResult) {
                result = sqlStatement.getResultSet();
            } else {
                System.err.println("Statement --" + sqlStatement + "-- returned no ResultSet");
            }
        } catch (Exception e) {
            System.err.println("Error reading data from JDBC database server.");
            throw new IOException(e.getMessage());
        }
        return result;
    }

    protected void init() throws IOException {
        try {
            dbUrl = DataServiceDispatcher.getJDBCurl();
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            System.out.println("Connecting JDBC driver . . . ");
            conn = DriverManager.getConnection(dbUrl);
            System.out.println("JDBC driver connected.");
        } catch (Exception e) {
            System.err.println("Error connecting to JDBC data source");
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * This is a temporary holding structure used internal to the SwiftJDBCReader
	 * to cache some properties until we can build the SimpleDataSeries and 
	 * DetailRecord objects during getChartData queries.  
	 * @author peter
	 *
	 */
    class TempSeriesHolder {

        InputChannelInterface channel;

        String unit;

        String label;

        /**
		 * Number of values we expect for this data series 
		 */
        int numVals;

        /**
		 * Index number for where we are at filling out data values
		 */
        int currentValIndex = 0;

        /**
		 * Store the data values for this object
		 */
        double[] times;

        double[] vals;

        public TempSeriesHolder(InputChannelInterface channel, String label, String unit) {
            this.channel = channel;
            this.label = label;
            this.unit = unit;
            this.numVals = 1;
        }

        /**
		 * called after numVals has been set
		 */
        void createTimeVals() {
            times = new double[numVals];
            vals = new double[numVals];
        }

        /**
		 * Called to add each value
		 * @param val
		 */
        void addTimeVal(double time, double val) {
            times[currentValIndex] = time;
            vals[currentValIndex++] = val;
        }
    }
}
