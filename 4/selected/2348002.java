package com.continuent.tungsten.replicator.channel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.continuent.tungsten.commons.config.TungstenProperties;
import com.continuent.tungsten.replicator.ReplicatorException;
import com.continuent.tungsten.replicator.database.Database;
import com.continuent.tungsten.replicator.database.DatabaseFactory;
import com.continuent.tungsten.replicator.plugin.PluginContext;
import com.continuent.tungsten.replicator.service.PipelineService;

/**
 * Provides a service interface to the shard-to-channel assignment table.
 * 
 * @author <a href="mailto:robert.hodges@continuent.com">Robert Hodges</a>
 */
public class ChannelAssignmentService implements PipelineService {

    private static Logger logger = Logger.getLogger(ChannelAssignmentService.class);

    private String name;

    private String user;

    private String url;

    private String password;

    private int channels;

    private Database conn;

    private ShardChannelTable channelTable;

    private Map<String, Integer> assignments = new HashMap<String, Integer>();

    private int maxChannel;

    private int nextChannel = 0;

    private int accessFailures;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#configure(com.continuent.tungsten.replicator.plugin.PluginContext)
     */
    public void configure(PluginContext context) throws ReplicatorException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#prepare(com.continuent.tungsten.replicator.plugin.PluginContext)
     */
    public void prepare(PluginContext context) throws ReplicatorException, InterruptedException {
        if (url == null) {
            url = context.getJdbcUrl(context.getReplicatorSchemaName());
        }
        if (user == null) user = context.getJdbcUser();
        if (password == null) password = context.getJdbcPassword();
        try {
            conn = DatabaseFactory.createDatabase(url, user, password);
            conn.connect();
        } catch (SQLException e) {
            throw new ReplicatorException("Unable to connect to database: " + e.getMessage(), e);
        }
        String metadataSchema = context.getReplicatorSchemaName();
        channelTable = new ShardChannelTable(metadataSchema);
        try {
            if (conn.supportsUseDefaultSchema() && metadataSchema != null) {
                if (conn.supportsCreateDropSchema()) conn.createSchema(metadataSchema);
                conn.useDefaultSchema(metadataSchema);
            }
            channelTable.initializeShardTable(conn);
        } catch (SQLException e) {
            throw new ReplicatorException("Unable to initialize shard-channel table", e);
        }
        loadChannelAssignments();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.plugin.ReplicatorPlugin#release(com.continuent.tungsten.replicator.plugin.PluginContext)
     */
    @Override
    public void release(PluginContext context) throws ReplicatorException, InterruptedException {
        if (conn != null) {
            conn.close();
            conn = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.service.Service#status()
     */
    public synchronized List<Map<String, String>> listChannelAssignments() {
        List<Map<String, String>> channels = null;
        try {
            channels = channelTable.list(conn);
        } catch (SQLException e) {
            accessFailures++;
            if (logger.isDebugEnabled()) logger.debug("Channel table access failed", e);
            channels = new ArrayList<Map<String, String>>();
        }
        return channels;
    }

    /**
     * Inserts a shard/channel assignment.
     * 
     * @param shardId Shard name
     * @param channel Channel number
     * @throws ReplicatorException Thrown if there is an error accessing
     *             database
     */
    public synchronized void insertChannelAssignment(String shardId, int channel) throws ReplicatorException {
        try {
            channelTable.insert(conn, shardId, channel);
            assignments.put(shardId, channel);
        } catch (SQLException e) {
            throw new ReplicatorException("Unable to access channel assignment table; ensure it is defined", e);
        }
    }

    /**
     * Looks up a channel assignment for a shard. This creates a new assignment
     * if required.
     * 
     * @param shardId Shard name
     * @return Integer channel number for shard
     * @throws ReplicatorException Thrown if there is an error accessing
     *             database
     */
    public synchronized Integer getChannelAssignment(String shardId) throws ReplicatorException {
        Integer channel = assignments.get(shardId);
        if (channel == null) {
            if (nextChannel >= channels) {
                nextChannel = 0;
            }
            channel = nextChannel++;
            insertChannelAssignment(shardId, channel);
        }
        return channel;
    }

    private synchronized void loadChannelAssignments() throws ReplicatorException {
        try {
            List<Map<String, String>> rows = channelTable.list(conn);
            for (Map<String, String> assignment : rows) {
                String shardId = assignment.get(ShardChannelTable.SHARD_ID_COL);
                Integer channel = Integer.parseInt(assignment.get(ShardChannelTable.CHANNEL_COL));
                assignments.put(shardId, channel);
            }
        } catch (SQLException e) {
            throw new ReplicatorException("Unable to access shard assignment table; ensure it is defined", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.continuent.tungsten.replicator.service.Service#status()
     */
    public TungstenProperties status() {
        TungstenProperties props = new TungstenProperties();
        props.setString("name", name);
        props.setLong("totalAssignments", assignments.size());
        props.setLong("maxChannel", maxChannel);
        props.setLong("accessFailures", accessFailures);
        return props;
    }
}
