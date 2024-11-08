package org.nexopenframework.management.realizations.spring.channels;

import static org.springframework.util.Assert.notNull;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import org.nexopenframework.management.module.Module;
import org.nexopenframework.management.module.model.NodeInfo;
import org.nexopenframework.management.monitor.MonitorException;
import org.nexopenframework.management.monitor.channels.ChannelNotification;
import org.nexopenframework.management.monitor.channels.ChannelNotificationInfo;
import org.nexopenframework.management.monitor.channels.ChannelNotificationInfo.ChannelMedia;
import org.nexopenframework.management.monitor.core.Lifecycle;
import org.nexopenframework.management.realizations.spring.support.SQLDataLoader;
import org.nexopenframework.management.realizations.support.SequenceGenerator;
import org.nexopenframework.management.support.NamingThreadFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Implementation of {@link ChannelNotification} for sending notifications thru
 * a JDBC channel using Spring Framework support.</p>
 * 
 * @see org.nexopenframework.management.monitor.core.Lifecycle
 * @see org.nexopenframework.management.spring.monitor.channels.ChannelNotification
 * @see org.springframework.jdbc.core.support.JdbcDaoSupport
 * @author Francesc Xavier Magdaleno
 * @version $Revision ,$Date 12/11/2009 11:53:18
 * @since 1.0.0.m2
 */
public class JdbcChannelNotification extends JdbcDaoSupport implements ChannelNotification, Lifecycle {

    /**SQL query to insert a notification*/
    private static final String SQL_INSERT = "insert into " + SQLDataLoader.QueryParser.SCHEMA_PATTERN + "open_frwk_notifications(id,thread_name,severity,message,creation_date,module_name,host_name,host_address) values(?,?,?,?,?,?,?,?)";

    /**prefix of DML file to be loaded*/
    private static final String TABLE_PREFIX = "create_table_";

    /**SQL file suffix extension (<code>.sql</code>)*/
    private static final String TABLE_SUFFIX = ".sql";

    /**Where to locate in classpath DML files*/
    private static final String TABLE_LOCATION = "META-INF/sql/";

    /**Default period of time for {@link WatchDog} class to sleep before asks again for information. Value is 20 minutes*/
    private static final long DEFAULT_PERIOD = 20 * 60 * 1000;

    /***/
    private PlatformTransactionManager transactionManager;

    /**A template for a */
    private final TransactionTemplate template = new TransactionTemplate();

    /**Queue of rejected queries*/
    protected final Queue<Runnable> queue = new ConcurrentLinkedQueue<Runnable>();

    /**Spring contract for sending asynchronous tasks using a suitable implementation (j2se 5.0, commonj,...)*/
    private SchedulingTaskExecutor service;

    /**simple generator of identifiers*/
    private final SequenceGenerator generator = new SequenceGenerator();

    /**period of execution list of queue of rejected emails*/
    private long period = DEFAULT_PERIOD;

    /**RDBMS schema if needed*/
    private String schema;

    /**if current WatchDog must be cancelled [cancellation requested flag]*/
    private volatile boolean cancelled;

    /**
	 * <p></p>
	 * 
	 * @param transactionManager
	 */
    public void setTransactionManager(final PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
	 * <p></p>
	 * 
	 * @param service
	 */
    public void setSchedulingTaskExecutor(final SchedulingTaskExecutor service) {
        this.service = service;
    }

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.management.realizations.support.SequenceGenerator#setInitialValue(long)
	 * @param initialValue
	 */
    public void setInitialValue(final long initialValue) {
        this.generator.setInitialValue(initialValue);
    }

    /**
	 * <p></p>
	 * 
	 * @param period
	 */
    public void setPeriod(final long period) {
        this.period = period;
    }

    /**
	 * <p></p>
	 * 
	 * @param schema
	 */
    public void setSchema(final String schema) {
        this.schema = schema;
    }

    /**
	 * <p>Given a notification from listeners, we persist such information adding to a given RDBMS</p>
	 * 
	 * @see org.nexopenframework.management.monitor.channels.ChannelNotification#processNotification(org.nexopenframework.management.monitor.channels.ChannelNotificationInfo)
	 */
    public void processNotification(final ChannelNotificationInfo info) {
        final ChannelMedia media = info.getChannelMedia();
        if (media.equals(ChannelMedia.ASYNC)) {
            persistInfo(info);
        } else {
            service.execute(new Runnable() {

                public void run() {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Invoking asynchronously channel notification information [" + info + "]");
                    }
                    persistInfo(info);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Invoked asynchronously [" + info + "]");
                    }
                }
            });
        }
    }

    /**
	 * <p>Free resources</p>
	 * 
	 * @see org.nexopenframework.management.monitor.core.Lifecycle#destroy()
	 */
    public void destroy() {
        if (service instanceof DisposableBean) {
            try {
                ((DisposableBean) service).destroy();
            } catch (final Exception e) {
                if (logger.isInfoEnabled()) {
                    logger.info("Problem in destroy method invoking destroy of SchedulingTaskExecutor", e);
                }
            }
        }
        SQLDataLoader.QueryParser.clear();
    }

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.management.monitor.core.Lifecycle#init()
	 */
    public void init() throws MonitorException {
        notNull(getDataSource(), "javax.sql.DataSource MUST be different from null");
        if (this.generator.getCurrentValue() == 0) {
            this.generator.load();
        }
        if (service == null) {
            final ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();
            final NamingThreadFactory threadFactory = new NamingThreadFactory("nexopen-jdbc-channel");
            threadFactory.setUncaughtExceptionHandler(new JdbcUncaughtExceptionHandler());
            threadPool.setThreadFactory(threadFactory);
            threadPool.setCorePoolSize(10);
            threadPool.setRejectedExecutionHandler(new JdbcChannelPolicy());
            threadPool.afterPropertiesSet();
            service = threadPool;
        }
        if (this.transactionManager == null) {
            this.transactionManager = new DataSourceTransactionManager(this.getDataSource());
        }
        template.setTransactionManager(transactionManager);
        final WatchDog wd = new WatchDog();
        this.service.execute(wd);
        Resource sqlTablesFile = null;
        String tableName = null;
        try {
            final String url = (String) JdbcUtils.extractDatabaseMetaData(this.getDataSource(), "getURL");
            if (url != null && url.indexOf("oracle") > -1) {
                tableName = new StringBuffer(TABLE_PREFIX).append("ora10g").append(TABLE_SUFFIX).toString();
            } else if (url != null && url.indexOf("hsql") > -1) {
                tableName = new StringBuffer(TABLE_PREFIX).append("hsqldb").append(TABLE_SUFFIX).toString();
            } else if (url != null && url.indexOf("derby") > -1) {
                tableName = new StringBuffer(TABLE_PREFIX).append("derby").append(TABLE_SUFFIX).toString();
            } else if (url != null && url.indexOf("mysql") > -1) {
                tableName = new StringBuffer(TABLE_PREFIX).append("mysql").append(TABLE_SUFFIX).toString();
            } else {
                tableName = new StringBuffer(TABLE_PREFIX).append("generic").append(TABLE_SUFFIX).toString();
            }
            final ClassLoader cls = Thread.currentThread().getContextClassLoader();
            final InputStream is = cls.getResourceAsStream(TABLE_LOCATION + tableName);
            if (is != null) {
                sqlTablesFile = new InputStreamResource(is);
            }
        } catch (final MetaDataAccessException e) {
            throw new MonitorException("Problem determining type of RDBMS or connecting problems [verify logs for more info]", e);
        }
        if (sqlTablesFile != null) {
            final SQLDataLoader loader = new SQLDataLoader();
            loader.setDataSource(this.getDataSource());
            loader.setFile(sqlTablesFile);
            loader.setSchema(schema);
            try {
                loader.load();
                if (logger.isInfoEnabled()) {
                    logger.info("created Notifications table from script :: " + tableName);
                }
            } catch (final IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Problems loading database info", e);
                }
            }
        }
    }

    /**
	 * <p>Persist given information to RDBMS</p>
	 * 
	 * @see #getJdbcTemplate()
	 * @param info
	 */
    protected void persistInfo(final ChannelNotificationInfo info) {
        final String insert = SQLDataLoader.QueryParser.parseSQL(SQL_INSERT, schema);
        template.execute(new TransactionCallbackWithoutResult() {

            /**
			 * <p>Executes in a transactional context an insertion of a notification</p>
			 * 
			 * @see org.springframework.transaction.support.TransactionCallbackWithoutResult#doInTransactionWithoutResult(org.springframework.transaction.TransactionStatus)
			 */
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                getJdbcTemplate().execute(insert, new PreparedStatementCallback<Integer>() {

                    public Integer doInPreparedStatement(final PreparedStatement ps) throws SQLException, DataAccessException {
                        ps.setLong(1, generator.getNext());
                        ps.setString(2, info.getThreadName());
                        ps.setString(3, info.getSeverity().name());
                        ps.setString(4, info.getMessage());
                        ps.setDate(5, new Date(info.getTimestamp()));
                        final Module m = info.getModule();
                        ps.setString(6, m.getName());
                        final NodeInfo nodeInfo = info.getNodeInfo();
                        ps.setString(7, nodeInfo.getHostName());
                        ps.setString(8, nodeInfo.getHostAddress());
                        return ps.executeUpdate();
                    }
                });
            }
        });
    }

    public class JdbcChannelPolicy implements RejectedExecutionHandler {

        /**
		 * <p>If pool is full all tasks are rejected and inserted in a queue for WatchDog process.</p>
		 * 
		 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
		 */
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                if (logger.isInfoEnabled()) {
                    logger.info("Rejected Runnable " + r);
                }
                queue.add(r);
            }
        }
    }

    public class JdbcUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        public void uncaughtException(final Thread t, final Throwable e) {
            logger.error("Problem in JDBC Channel :: " + e.getMessage(), e);
        }
    }

    /**
	 * <p>WatchDog which deals with emails that it has not been send previously because
	 * a rejection condition has been fulfilled.</p>
	 */
    class WatchDog implements Runnable {

        public void run() {
            while (!cancelled) {
                if (!queue.isEmpty()) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Sending SQL inserts rejected by thread pool");
                    }
                    while (!queue.isEmpty()) {
                        final Runnable r = queue.poll();
                        if (r != null) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("processing runnable " + r);
                            }
                            r.run();
                        }
                    }
                }
                try {
                    Thread.sleep(period);
                } catch (final InterruptedException e) {
                    logger.debug("Interrupted exception", e);
                }
            }
        }
    }
}
