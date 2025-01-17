package org.jumpmind.symmetric.service.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.sql.UniqueKeyException;
import org.jumpmind.symmetric.common.Constants;
import org.jumpmind.symmetric.common.ParameterConstants;
import org.jumpmind.symmetric.db.ISymmetricDialect;
import org.jumpmind.symmetric.io.data.Batch;
import org.jumpmind.symmetric.io.data.DataContext;
import org.jumpmind.symmetric.io.data.DataEventType;
import org.jumpmind.symmetric.io.data.DataProcessor;
import org.jumpmind.symmetric.io.data.IDataProcessorListener;
import org.jumpmind.symmetric.io.data.IDataReader;
import org.jumpmind.symmetric.io.data.IDataWriter;
import org.jumpmind.symmetric.io.data.reader.ProtocolDataReader;
import org.jumpmind.symmetric.io.data.transform.TransformPoint;
import org.jumpmind.symmetric.io.data.transform.TransformTable;
import org.jumpmind.symmetric.io.data.writer.Conflict;
import org.jumpmind.symmetric.io.data.writer.Conflict.DetectConflict;
import org.jumpmind.symmetric.io.data.writer.Conflict.ResolveConflict;
import org.jumpmind.symmetric.io.data.writer.DatabaseWriter;
import org.jumpmind.symmetric.io.data.writer.IDatabaseWriterFilter;
import org.jumpmind.symmetric.io.data.writer.IProtocolDataWriterListener;
import org.jumpmind.symmetric.io.data.writer.ResolvedData;
import org.jumpmind.symmetric.io.data.writer.StagingDataWriter;
import org.jumpmind.symmetric.io.data.writer.TransformWriter;
import org.jumpmind.symmetric.io.stage.IStagedResource;
import org.jumpmind.symmetric.io.stage.IStagedResource.State;
import org.jumpmind.symmetric.io.stage.IStagingManager;
import org.jumpmind.symmetric.load.ConfigurationChangedFilter;
import org.jumpmind.symmetric.load.DefaultDataLoaderFactory;
import org.jumpmind.symmetric.load.IDataLoaderFactory;
import org.jumpmind.symmetric.model.BatchInfo;
import org.jumpmind.symmetric.model.Channel;
import org.jumpmind.symmetric.model.ChannelMap;
import org.jumpmind.symmetric.model.IncomingBatch;
import org.jumpmind.symmetric.model.IncomingBatch.Status;
import org.jumpmind.symmetric.model.IncomingError;
import org.jumpmind.symmetric.model.Node;
import org.jumpmind.symmetric.model.NodeGroupLink;
import org.jumpmind.symmetric.model.NodeSecurity;
import org.jumpmind.symmetric.model.RemoteNodeStatus;
import org.jumpmind.symmetric.service.IConfigurationService;
import org.jumpmind.symmetric.service.IDataLoaderService;
import org.jumpmind.symmetric.service.IIncomingBatchService;
import org.jumpmind.symmetric.service.INodeService;
import org.jumpmind.symmetric.service.IParameterService;
import org.jumpmind.symmetric.service.ITransformService;
import org.jumpmind.symmetric.service.ITriggerRouterService;
import org.jumpmind.symmetric.service.RegistrationNotOpenException;
import org.jumpmind.symmetric.service.RegistrationRequiredException;
import org.jumpmind.symmetric.service.impl.TransformService.TransformTableNodeGroupLink;
import org.jumpmind.symmetric.statistic.IStatisticManager;
import org.jumpmind.symmetric.transport.AuthenticationException;
import org.jumpmind.symmetric.transport.ConnectionRejectedException;
import org.jumpmind.symmetric.transport.IIncomingTransport;
import org.jumpmind.symmetric.transport.ITransportManager;
import org.jumpmind.symmetric.transport.SyncDisabledException;
import org.jumpmind.symmetric.transport.TransportException;
import org.jumpmind.symmetric.transport.http.HttpTransportManager;
import org.jumpmind.symmetric.transport.internal.InternalIncomingTransport;
import org.jumpmind.symmetric.util.AppUtils;
import org.jumpmind.symmetric.web.WebConstants;

/**
 * Responsible for writing batch data to the database
 * 
 * @see IDataLoaderService
 */
public class DataLoaderService extends AbstractService implements IDataLoaderService {

    private IIncomingBatchService incomingBatchService;

    private IConfigurationService configurationService;

    private ITransportManager transportManager;

    private List<IDatabaseWriterFilter> filters;

    private IStatisticManager statisticManager;

    private INodeService nodeService;

    private ITransformService transformService;

    private IStagingManager stagingManager;

    private Map<String, IDataLoaderFactory> dataLoaderFactories = new HashMap<String, IDataLoaderFactory>();

    private Map<NodeGroupLink, List<ConflictSettingNodeGroupLink>> conflictSettingsCache = new HashMap<NodeGroupLink, List<ConflictSettingNodeGroupLink>>();

    private long lastConflictCacheResetTimeInMs = 0;

    public DataLoaderService(IParameterService parameterService, ISymmetricDialect symmetricDialect, IIncomingBatchService incomingBatchService, IConfigurationService configurationService, ITransportManager transportManager, IStatisticManager statisticManager, INodeService nodeService, ITransformService transformService, ITriggerRouterService triggerRouterService, IStagingManager stagingManager) {
        super(parameterService, symmetricDialect);
        this.incomingBatchService = incomingBatchService;
        this.configurationService = configurationService;
        this.transportManager = transportManager;
        this.statisticManager = statisticManager;
        this.nodeService = nodeService;
        this.transformService = transformService;
        this.stagingManager = stagingManager;
        this.filters = new ArrayList<IDatabaseWriterFilter>();
        this.filters.add(new ConfigurationChangedFilter(parameterService, configurationService, triggerRouterService, transformService));
        this.addDataLoaderFactory(new DefaultDataLoaderFactory(parameterService));
        this.setSqlMap(new DataLoaderServiceSqlMap(platform, createSqlReplacementTokens()));
    }

    public void addDataLoaderFactory(IDataLoaderFactory factory) {
        this.dataLoaderFactories.put(factory.getTypeName(), factory);
    }

    public List<String> getAvailableDataLoaderFactories() {
        return new ArrayList<String>(dataLoaderFactories.keySet());
    }

    public List<IncomingBatch> loadDataBatch(String batchData) throws IOException {
        InternalIncomingTransport transport = new InternalIncomingTransport(new BufferedReader(new StringReader(batchData)));
        return loadDataFromTransport(nodeService.findIdentityNodeId(), transport);
    }

    /**
     * Connect to the remote node and pull data. The acknowledgment of
     * commit/error status is sent separately after the data is processed.
     */
    public RemoteNodeStatus loadDataFromPull(Node remote) throws IOException {
        RemoteNodeStatus status = new RemoteNodeStatus(remote != null ? remote.getNodeId() : null);
        loadDataFromPull(remote, status);
        return status;
    }

    public void loadDataFromPull(Node remote, RemoteNodeStatus status) throws IOException {
        try {
            Node local = nodeService.findIdentity();
            if (local == null) {
                local = new Node(this.parameterService, symmetricDialect);
            }
            NodeSecurity localSecurity = nodeService.findNodeSecurity(local.getNodeId());
            IIncomingTransport transport = null;
            if (remote != null && localSecurity != null) {
                Map<String, String> requestProperties = new HashMap<String, String>();
                ChannelMap suspendIgnoreChannels = configurationService.getSuspendIgnoreChannelLists();
                requestProperties.put(WebConstants.SUSPENDED_CHANNELS, suspendIgnoreChannels.getSuspendChannelsAsString());
                requestProperties.put(WebConstants.IGNORED_CHANNELS, suspendIgnoreChannels.getIgnoreChannelsAsString());
                transport = transportManager.getPullTransport(remote, local, localSecurity.getNodePassword(), requestProperties, parameterService.getRegistrationUrl());
            } else {
                transport = transportManager.getRegisterTransport(local, parameterService.getRegistrationUrl());
                log.info("Using registration URL of {}", transport.getUrl());
                remote = new Node();
                remote.setSyncUrl(parameterService.getRegistrationUrl());
            }
            List<IncomingBatch> list = loadDataFromTransport(remote.getNodeId(), transport);
            if (list.size() > 0) {
                status.updateIncomingStatus(list);
                local = nodeService.findIdentity();
                if (local != null) {
                    localSecurity = nodeService.findNodeSecurity(local.getNodeId());
                    if (StringUtils.isNotBlank(transport.getRedirectionUrl())) {
                        String url = transport.getRedirectionUrl();
                        url = url.replace(HttpTransportManager.buildRegistrationUrl("", local), "");
                        remote.setSyncUrl(url);
                    }
                    sendAck(remote, local, localSecurity, list);
                }
            }
        } catch (RegistrationRequiredException e) {
            log.warn("Registration was lost. Attempting to re-register.");
            loadDataFromPull(null, status);
            nodeService.findIdentity(false);
        } catch (MalformedURLException e) {
            if (remote != null) {
                log.error("Could not connect to the {} node's transport because of a bad URL: {}", remote.getNodeId(), remote.getSyncUrl());
            } else {
                log.error(e.getMessage(), e);
            }
            throw e;
        }
    }

    /**
     * Load database from input stream and write acknowledgment to output
     * stream. This is used for a "push" request with a response of an
     * acknowledgment.
     */
    public void loadDataFromPush(String sourceNodeId, InputStream in, OutputStream out) throws IOException {
        List<IncomingBatch> list = loadDataFromTransport(sourceNodeId, new InternalIncomingTransport(in));
        Node local = nodeService.findIdentity();
        NodeSecurity security = nodeService.findNodeSecurity(local.getNodeId());
        transportManager.writeAcknowledgement(out, list, local, security != null ? security.getNodePassword() : null);
    }

    public void addDatabaseWriterFilter(IDatabaseWriterFilter filter) {
        if (filters == null) {
            filters = new ArrayList<IDatabaseWriterFilter>();
        }
        filters.add(filter);
    }

    public void removeDatabaseWriterFilter(IDatabaseWriterFilter filter) {
        filters.remove(filter);
    }

    /**
     * Try a configured number of times to get the ACK through.
     */
    protected void sendAck(Node remote, Node local, NodeSecurity localSecurity, List<IncomingBatch> list) throws IOException {
        Exception error = null;
        int sendAck = -1;
        int numberOfStatusSendRetries = parameterService.getInt(ParameterConstants.DATA_LOADER_NUM_OF_ACK_RETRIES);
        for (int i = 0; i < numberOfStatusSendRetries && sendAck != HttpURLConnection.HTTP_OK; i++) {
            try {
                sendAck = transportManager.sendAcknowledgement(remote, list, local, localSecurity.getNodePassword(), parameterService.getRegistrationUrl());
            } catch (IOException ex) {
                error = ex;
            } catch (RuntimeException ex) {
                error = ex;
            }
            if (sendAck != HttpURLConnection.HTTP_OK) {
                log.warn("Ack was not sent successfully on try number {}.  {}", i + 1, error != null ? error.getMessage() : "");
                if (i < numberOfStatusSendRetries - 1) {
                    AppUtils.sleep(parameterService.getLong(ParameterConstants.DATA_LOADER_TIME_BETWEEN_ACK_RETRIES));
                } else if (error instanceof RuntimeException) {
                    throw (RuntimeException) error;
                } else if (error instanceof IOException) {
                    throw (IOException) error;
                } else {
                    throw new IOException(Integer.toString(sendAck));
                }
            }
        }
    }

    /**
     * Load database from input stream and return a list of batch statuses. This
     * is used for a pull request that responds with data, and the
     * acknowledgment is sent later.
     */
    protected List<IncomingBatch> loadDataFromTransport(final String sourceNodeId, IIncomingTransport transport) throws IOException {
        final ManageIncomingBatchListener listener = new ManageIncomingBatchListener();
        try {
            long totalNetworkMillis = System.currentTimeMillis();
            if (parameterService.is(ParameterConstants.STREAM_TO_FILE_ENABLED)) {
                IDataReader dataReader = new ProtocolDataReader(transport.open());
                IDataWriter dataWriter = new StagingDataWriter(sourceNodeId, Constants.STAGING_CATEGORY_INCOMING, stagingManager, new LoadIntoDatabaseOnArrivalListener(sourceNodeId, listener));
                new DataProcessor(dataReader, dataWriter).process();
                totalNetworkMillis = System.currentTimeMillis() - totalNetworkMillis;
            } else {
                DataProcessor processor = new DataProcessor(new ProtocolDataReader(transport.open()), null, listener) {

                    @Override
                    protected IDataWriter chooseDataWriter(Batch batch) {
                        return buildDataWriter(sourceNodeId, batch.getChannelId(), batch.getBatchId());
                    }
                };
                processor.process();
            }
            List<IncomingBatch> batchesProcessed = listener.getBatchesProcessed();
            for (IncomingBatch incomingBatch : batchesProcessed) {
                if (incomingBatch.getBatchId() != BatchInfo.VIRTUAL_BATCH_FOR_REGISTRATION && incomingBatchService.updateIncomingBatch(incomingBatch) == 0) {
                    log.error("Failed to update batch {}.  Zero rows returned.", incomingBatch.getBatchId());
                }
            }
        } catch (Exception ex) {
            logAndRethrow(ex);
        } finally {
            transport.close();
        }
        return listener.getBatchesProcessed();
    }

    protected void logAndRethrow(Exception ex) throws IOException {
        if (ex instanceof RegistrationRequiredException) {
            throw (RegistrationRequiredException) ex;
        } else if (ex instanceof ConnectException) {
            throw (ConnectException) ex;
        } else if (ex instanceof UnknownHostException) {
            log.warn("Could not connect to the transport because the host was unknown: {}", ex.getMessage());
            throw (UnknownHostException) ex;
        } else if (ex instanceof RegistrationNotOpenException) {
            log.warn("Registration attempt failed.  Registration was not open for the node");
        } else if (ex instanceof ConnectionRejectedException) {
            log.warn("The server was too busy to accept the connection");
            throw (ConnectionRejectedException) ex;
        } else if (ex instanceof AuthenticationException) {
            log.warn("Could not authenticate with node");
        } else if (ex instanceof SyncDisabledException) {
            log.warn("Synchronization is disabled on the server node");
            throw (SyncDisabledException) ex;
        } else if (ex instanceof IOException) {
            if (ex.getMessage() != null && !ex.getMessage().startsWith("http")) {
                log.error("Failed while reading batch because: {}", ex.getMessage());
            } else {
                log.error("Failed while reading batch because: {}", ex.getMessage(), ex);
            }
            throw (IOException) ex;
        } else {
            log.error("Failed while parsing batch", ex);
        }
    }

    protected IDataWriter buildDataWriter(String sourceNodeId, String channelId, long batchId) {
        TransformTable[] transforms = null;
        NodeGroupLink link = null;
        List<ResolvedData> resolvedDatas = new ArrayList<ResolvedData>();
        if (sourceNodeId != null) {
            List<TransformTableNodeGroupLink> transformsList = transformService.findTransformsFor(new NodeGroupLink(sourceNodeId, nodeService.findIdentityNodeId()), TransformPoint.LOAD, true);
            transforms = transformsList != null ? transformsList.toArray(new TransformTable[transformsList.size()]) : null;
            List<IncomingError> incomingErrors = getIncomingErrors(batchId, sourceNodeId);
            for (IncomingError incomingError : incomingErrors) {
                if (incomingError.isResolveIgnore() || StringUtils.isNotBlank(incomingError.getResolveData())) {
                    resolvedDatas.add(new ResolvedData(incomingError.getFailedRowNumber(), incomingError.getResolveData(), incomingError.isResolveIgnore()));
                }
            }
            Node sourceNode = nodeService.findNode(sourceNodeId);
            if (sourceNode != null) {
                link = new NodeGroupLink(sourceNode.getNodeGroupId(), parameterService.getNodeGroupId());
            }
        }
        TransformWriter transformWriter = new TransformWriter(platform, TransformPoint.LOAD, null, transforms);
        IDataWriter targetWriter = getFactory(channelId).getDataWriter(sourceNodeId, platform, transformWriter, filters, getConflictSettingsNodeGroupLinks(link, false), resolvedDatas);
        transformWriter.setTargetWriter(targetWriter);
        return transformWriter;
    }

    protected IDataLoaderFactory getFactory(String channelId) {
        Channel channel = configurationService.getChannel(channelId);
        String dataLoaderType = "default";
        IDataLoaderFactory factory = null;
        if (channel != null) {
            dataLoaderType = channel.getDataLoaderType();
        } else {
            log.warn("Could not locate the channel with the id of '{}'.  Using the 'default' data loader.", channelId);
        }
        factory = dataLoaderFactories.get(dataLoaderType);
        if (factory == null) {
            log.warn("Could not find a data loader factory of type '{}'.  Using the 'default' data loader.", dataLoaderType);
            factory = dataLoaderFactories.get("default");
        }
        if (!factory.isPlatformSupported(platform)) {
            log.warn("The current platform does not support a data loader type of '{}'.  Using the 'default' data loader.", dataLoaderType);
            factory = dataLoaderFactories.get("default");
        }
        return factory;
    }

    public List<ConflictSettingNodeGroupLink> getConflictSettingsNodeGroupLinks() {
        List<ConflictSettingNodeGroupLink> list = new ArrayList<DataLoaderService.ConflictSettingNodeGroupLink>();
        list = sqlTemplate.query(getSql("selectConflictSettingsSql"), new ConflictSettingsNodeGroupLinkMapper());
        return list;
    }

    public List<ConflictSettingNodeGroupLink> getConflictSettingsNodeGroupLinks(NodeGroupLink link, boolean refreshCache) {
        if (link != null) {
            long cacheTime = parameterService.getLong(ParameterConstants.CACHE_TIMEOUT_CONFLICT_IN_MS);
            if (System.currentTimeMillis() - lastConflictCacheResetTimeInMs > cacheTime || refreshCache) {
                synchronized (this) {
                    conflictSettingsCache.clear();
                    lastConflictCacheResetTimeInMs = System.currentTimeMillis();
                }
            }
            List<ConflictSettingNodeGroupLink> list = conflictSettingsCache.get(link);
            if (list == null) {
                list = sqlTemplate.query(getSql("selectConflictSettingsSql", " where source_node_group_id=? and target_node_group_id=?"), new ConflictSettingsNodeGroupLinkMapper(), link.getSourceNodeGroupId(), link.getTargetNodeGroupId());
                synchronized (this) {
                    conflictSettingsCache.put(link, list);
                }
            }
            return list;
        } else {
            return new ArrayList<DataLoaderService.ConflictSettingNodeGroupLink>(0);
        }
    }

    public void delete(ConflictSettingNodeGroupLink settings) {
        sqlTemplate.update(getSql("deleteConflictSettingsSql"), settings.getConflictId());
    }

    public void save(ConflictSettingNodeGroupLink setting) {
        this.lastConflictCacheResetTimeInMs = 0;
        if (sqlTemplate.update(getSql("updateConflictSettingsSql"), setting.getNodeGroupLink().getSourceNodeGroupId(), setting.getNodeGroupLink().getTargetNodeGroupId(), setting.getTargetChannelId(), setting.getTargetCatalogName(), setting.getTargetSchemaName(), setting.getTargetTableName(), setting.getDetectType().name(), setting.getResolveType().name(), setting.isResolveChangesOnly() ? 1 : 0, setting.isResolveRowOnly() ? 1 : 0, setting.getDetectExpression(), setting.getLastUpdateBy(), setting.getConflictId()) == 0) {
            sqlTemplate.update(getSql("insertConflictSettingsSql"), setting.getNodeGroupLink().getSourceNodeGroupId(), setting.getNodeGroupLink().getTargetNodeGroupId(), setting.getTargetChannelId(), setting.getTargetCatalogName(), setting.getTargetSchemaName(), setting.getTargetTableName(), setting.getDetectType().name(), setting.getResolveType().name(), setting.isResolveChangesOnly() ? 1 : 0, setting.isResolveRowOnly() ? 1 : 0, setting.getDetectExpression(), setting.getLastUpdateBy(), setting.getConflictId());
        }
    }

    public List<IncomingError> getIncomingErrors(long batchId, String nodeId) {
        return sqlTemplate.query(getSql("selectIncomingErrorSql"), new IncomingErrorMapper(), batchId, nodeId);
    }

    public IncomingError getCurrentIncomingError(long batchId, String nodeId) {
        return sqlTemplate.queryForObject(getSql("selectCurrentIncomingErrorSql"), new IncomingErrorMapper(), batchId, nodeId);
    }

    public void insertIncomingError(IncomingError incomingError) {
        sqlTemplate.update(getSql("insertIncomingErrorSql"), incomingError.getBatchId(), incomingError.getNodeId(), incomingError.getFailedRowNumber(), incomingError.getFailedLineNumber(), incomingError.getTargetCatalogName(), incomingError.getTargetSchemaName(), incomingError.getTargetTableName(), incomingError.getEventType().getCode(), incomingError.getColumnNames(), incomingError.getPrimaryKeyColumnNames(), incomingError.getRowData(), incomingError.getOldData(), incomingError.getResolveData(), incomingError.getResolveData(), incomingError.getCreateTime(), incomingError.getLastUpdateBy(), incomingError.getLastUpdateTime());
    }

    public void updateIncomingError(IncomingError incomingError) {
        sqlTemplate.update(getSql("updateIncomingErrorSql"), incomingError.getResolveData(), incomingError.isResolveIgnore(), incomingError.getBatchId(), incomingError.getNodeId(), incomingError.getFailedRowNumber());
    }

    /**
     * Used for unit tests
     */
    protected void setTransportManager(ITransportManager transportManager) {
        this.transportManager = transportManager;
    }

    class ConflictSettingsNodeGroupLinkMapper implements ISqlRowMapper<ConflictSettingNodeGroupLink> {

        public ConflictSettingNodeGroupLink mapRow(Row rs) {
            ConflictSettingNodeGroupLink setting = new ConflictSettingNodeGroupLink();
            setting.setNodeGroupLink(new NodeGroupLink(rs.getString("source_node_group_id"), rs.getString("target_node_group_id")));
            setting.setTargetChannelId(rs.getString("target_channel_id"));
            setting.setTargetCatalogName(rs.getString("target_catalog_name"));
            setting.setTargetSchemaName(rs.getString("target_schema_name"));
            setting.setTargetTableName(rs.getString("target_table_name"));
            setting.setDetectType(DetectConflict.valueOf(rs.getString("detect_type").toUpperCase()));
            setting.setResolveType(ResolveConflict.valueOf(rs.getString("resolve_type").toUpperCase()));
            setting.setResolveChangesOnly(rs.getBoolean("resolve_changes_only"));
            setting.setResolveRowOnly(rs.getBoolean("resolve_row_only"));
            setting.setDetectExpression(rs.getString("detect_expression"));
            setting.setLastUpdateBy(rs.getString("last_update_by"));
            setting.setConflictId(rs.getString("conflict_id"));
            setting.setCreateTime(rs.getDateTime("create_time"));
            setting.setLastUpdateTime(rs.getDateTime("last_update_time"));
            return setting;
        }
    }

    class IncomingErrorMapper implements ISqlRowMapper<IncomingError> {

        public IncomingError mapRow(Row rs) {
            IncomingError incomingError = new IncomingError();
            incomingError.setBatchId(rs.getLong("batch_id"));
            incomingError.setNodeId(rs.getString("node_id"));
            incomingError.setFailedRowNumber(rs.getLong("failed_row_number"));
            incomingError.setFailedLineNumber(rs.getLong("failed_line_number"));
            incomingError.setTargetCatalogName(rs.getString("target_catalog_name"));
            incomingError.setTargetSchemaName(rs.getString("target_schema_name"));
            incomingError.setTargetTableName(rs.getString("target_table_name"));
            incomingError.setEventType(DataEventType.getEventType(rs.getString("event_type")));
            incomingError.setColumnNames(rs.getString("column_names"));
            incomingError.setPrimaryKeyColumnNames(rs.getString("pk_column_names"));
            incomingError.setRowData(rs.getString("row_data"));
            incomingError.setOldData(rs.getString("old_data"));
            incomingError.setResolveData(rs.getString("resolve_data"));
            incomingError.setResolveIgnore(rs.getBoolean("resolve_ignore"));
            incomingError.setCreateTime(rs.getDateTime("create_time"));
            incomingError.setLastUpdateBy(rs.getString("last_update_by"));
            incomingError.setLastUpdateTime(rs.getDateTime("last_update_time"));
            return incomingError;
        }
    }

    class LoadIntoDatabaseOnArrivalListener implements IProtocolDataWriterListener {

        private ManageIncomingBatchListener listener;

        private long batchStartsToArriveTimeInMs;

        private String sourceNodeId;

        public LoadIntoDatabaseOnArrivalListener(String sourceNodeId, ManageIncomingBatchListener listener) {
            this.sourceNodeId = sourceNodeId;
            this.listener = listener;
        }

        public void start(Batch batch) {
            batchStartsToArriveTimeInMs = System.currentTimeMillis();
        }

        public void end(Batch batch, IStagedResource resource) {
            if (listener.currentBatch != null) {
                listener.currentBatch.setNetworkMillis(System.currentTimeMillis() - batchStartsToArriveTimeInMs);
            }
            try {
                DataProcessor processor = new DataProcessor(new ProtocolDataReader(resource), null, listener) {

                    @Override
                    protected IDataWriter chooseDataWriter(Batch batch) {
                        return buildDataWriter(sourceNodeId, batch.getChannelId(), batch.getBatchId());
                    }
                };
                processor.process();
            } finally {
                resource.setState(State.DONE);
            }
        }
    }

    class ManageIncomingBatchListener implements IDataProcessorListener {

        protected List<IncomingBatch> batchesProcessed = new ArrayList<IncomingBatch>();

        protected IncomingBatch currentBatch;

        public void beforeBatchEnd(DataContext context) {
            enableSyncTriggers(context);
        }

        public boolean beforeBatchStarted(DataContext context) {
            this.currentBatch = null;
            Batch batch = context.getBatch();
            if (parameterService.is(ParameterConstants.DATA_LOADER_ENABLED) || (batch.getChannelId() != null && batch.getChannelId().equals(Constants.CHANNEL_CONFIG))) {
                IncomingBatch incomingBatch = new IncomingBatch(batch);
                this.batchesProcessed.add(incomingBatch);
                if (incomingBatchService.acquireIncomingBatch(incomingBatch)) {
                    this.currentBatch = incomingBatch;
                    return true;
                }
            }
            return false;
        }

        public void afterBatchStarted(DataContext context) {
            Batch batch = context.getBatch();
            symmetricDialect.disableSyncTriggers(findTransaction(context), batch.getNodeId());
        }

        protected ISqlTransaction findTransaction(DataContext context) {
            if (context.getWriter() instanceof TransformWriter) {
                IDataWriter targetWriter = ((TransformWriter) context.getWriter()).getTargetWriter();
                if (targetWriter instanceof DatabaseWriter) {
                    return ((DatabaseWriter) targetWriter).getTransaction();
                }
            }
            return null;
        }

        public void batchSuccessful(DataContext context) {
            Batch batch = context.getBatch();
            this.currentBatch.setValues(context.getReader().getStatistics().get(batch), context.getWriter().getStatistics().get(batch), true);
            Status oldStatus = this.currentBatch.getStatus();
            try {
                this.currentBatch.setStatus(Status.OK);
                incomingBatchService.updateIncomingBatch(this.currentBatch);
            } catch (RuntimeException ex) {
                this.currentBatch.setStatus(oldStatus);
                throw ex;
            }
        }

        protected void enableSyncTriggers(DataContext context) {
            try {
                ISqlTransaction transaction = findTransaction(context);
                if (transaction != null) {
                    symmetricDialect.enableSyncTriggers(transaction);
                }
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
            }
        }

        public void batchInError(DataContext context, Exception ex) {
            try {
                Batch batch = context.getBatch();
                if (context.getWriter() != null) {
                    this.currentBatch.setValues(context.getReader().getStatistics().get(batch), context.getWriter().getStatistics().get(batch), false);
                } else {
                    log.error("An error caused a batch to fail without attempting to load data", ex);
                    ex.printStackTrace();
                }
                enableSyncTriggers(context);
                statisticManager.incrementDataLoadedErrors(this.currentBatch.getChannelId(), 1);
                if (ex instanceof IOException || ex instanceof TransportException) {
                    log.warn("Failed to load batch {} because: {}", this.currentBatch.getNodeBatchId(), ex.getMessage());
                    this.currentBatch.setSqlMessage(ex.getMessage());
                } else {
                    log.error("Failed to load batch {} because: {}", new Object[] { this.currentBatch.getNodeBatchId(), ex.getMessage() });
                    log.error(ex.getMessage(), ex);
                    SQLException se = unwrapSqlException(ex);
                    if (se != null) {
                        this.currentBatch.setSqlState(se.getSQLState());
                        this.currentBatch.setSqlCode(se.getErrorCode());
                        this.currentBatch.setSqlMessage(se.getMessage());
                    } else {
                        this.currentBatch.setSqlMessage(ex.getMessage());
                    }
                }
                if (this.currentBatch.getStatus() != Status.OK) {
                    this.currentBatch.setStatus(IncomingBatch.Status.ER);
                    if (context.getTable() != null) {
                        try {
                            IncomingError error = new IncomingError();
                            error.setBatchId(this.currentBatch.getBatchId());
                            error.setNodeId(this.currentBatch.getNodeId());
                            error.setColumnNames(Table.getCommaDeliminatedColumns(context.getTable().getColumns()));
                            error.setPrimaryKeyColumnNames(Table.getCommaDeliminatedColumns(context.getTable().getPrimaryKeyColumns()));
                            error.setCsvData(context.getData());
                            error.setEventType(context.getData().getDataEventType());
                            error.setFailedLineNumber(this.currentBatch.getFailedLineNumber());
                            error.setFailedRowNumber(this.currentBatch.getFailedRowNumber());
                            error.setTargetCatalogName(context.getTable().getCatalog());
                            error.setTargetSchemaName(context.getTable().getSchema());
                            error.setTargetTableName(context.getTable().getName());
                            insertIncomingError(error);
                        } catch (UniqueKeyException e) {
                        }
                    }
                }
                incomingBatchService.updateIncomingBatch(this.currentBatch);
            } catch (Exception e) {
                log.error("Failed to record status of batch {} because {}", this.currentBatch != null ? this.currentBatch.getNodeBatchId() : context.getBatch().getNodeBatchId(), e.getMessage());
                e.printStackTrace();
            }
        }

        public List<IncomingBatch> getBatchesProcessed() {
            return batchesProcessed;
        }

        public IncomingBatch getCurrentBatch() {
            return currentBatch;
        }
    }

    public static class ConflictSettingNodeGroupLink extends Conflict {

        private static final long serialVersionUID = 1L;

        protected NodeGroupLink nodeGroupLink;

        public void setNodeGroupLink(NodeGroupLink nodeGroupLink) {
            this.nodeGroupLink = nodeGroupLink;
        }

        public NodeGroupLink getNodeGroupLink() {
            return nodeGroupLink;
        }
    }
}
