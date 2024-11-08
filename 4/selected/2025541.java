package net.sourceforge.olduvai.lrac.jdbcdaytonadataservice;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.att.research.jdbc.daytona.ext.Day;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.ActiveSourceQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.queries.SwatchQueryInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.records.DetailRecord;
import net.sourceforge.olduvai.lrac.genericdataservice.records.SwatchRecordInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.InputChannelItemInterface;
import net.sourceforge.olduvai.lrac.genericdataservice.structure.SourceInterface;
import net.sourceforge.olduvai.lrac.jdbcdaytonadataservice.structure.InputChannel;
import net.sourceforge.olduvai.lrac.jdbcdaytonadataservice.structure.Source;
import net.sourceforge.olduvai.lrac.util.Util;

/**
 * 
 * @author Peter McLachlan <spark343@cs.ubc.ca>
 * 
 */
public class DaytonaJDBCReader {

    static final long SPINSLEEP = 10;

    static final String sourceQueryFilePath = DataServiceDispatcher.DAYTONASERVICEPATH + "list_routers.Q";

    /**
	 * Map the input channels to the relevant cymbol query file
	 */
    static final String[][] swatchQueryFilenames = { { "Memory_used_percent", "swatch_percentmem.Q" }, { "Memory_free", "swatch_memory.Q" }, { "Memory_used", "swatch_memory.Q" }, { "CPU_util", "swatch_cpu.Q" } };

    /**
	 * Detail query file names
	 */
    static final String[][] detailQueryFilenames = { { "Memory_used_percent", "detail_percentmem.Q" }, { "Memory_free", "detail_memory.Q" }, { "Memory_used", "detail_memory.Q" }, { "CPU_util", "detail_cpu.Q" } };

    private static DaytonaJDBCReader INSTANCE;

    static final synchronized DaytonaJDBCReader getInstance() {
        if (INSTANCE == null) INSTANCE = new DaytonaJDBCReader();
        return INSTANCE;
    }

    /**
	 * Tracks all swatch queries, used to look up the correct cymbal query + correct table to access for a given channel.  
	 */
    private Map<InputChannelInterface, DaytonaSwatchQuery> daytonaSwatchQueries = new HashMap<InputChannelInterface, DaytonaSwatchQuery>();

    /**
	 * Tracks all stat queries, used to look up correct cymbal query to access a given channel
	 */
    private Map<InputChannelInterface, DaytonaDetailQuery> daytonaDetailQueries = new HashMap<InputChannelInterface, DaytonaDetailQuery>();

    private DaytonaJDBCReader() {
    }

    /**
	 * 
	 * @return
	 * @throws IOException
	 */
    @SuppressWarnings("unchecked")
    protected List<InputChannelInterface> getInputChannels() throws IOException {
        final List channelList = new ArrayList();
        InputChannelInterface channel = new InputChannel("CPU_util", "%", "CPU");
        channelList.add(channel);
        channel = new InputChannel("Memory_free", "B", "Memory");
        channelList.add(channel);
        channel = new InputChannel("Memory_used", "B", "Memory");
        channelList.add(channel);
        channel = new InputChannel("Memory_used_percent", "%", "Memory");
        channelList.add(channel);
        return channelList;
    }

    /**
	 * Initiates a process of pre-compiling the queries for later use.
	 */
    protected void compileQueries() throws IOException {
        final DataServiceDispatcher d = DataServiceDispatcher.getInstance();
        final ThreadGroup queryCompile = new ThreadGroup("Query compilation") {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                final QueryCompileWorker w = (QueryCompileWorker) t;
                w.setErrorStatus();
            }
        };
        System.out.println("-- Compiling swatch queries");
        final List<QueryCompileWorker> threads = new ArrayList<QueryCompileWorker>(swatchQueryFilenames.length);
        for (String[] pair : swatchQueryFilenames) {
            final String channelname = pair[0];
            final String filename = pair[1];
            final String fullpath = DataServiceDispatcher.DAYTONASERVICEPATH + filename;
            final InputChannelInterface c = getChannelFromListByName(d.getInputChannels(), channelname);
            DaytonaSwatchQuery dsq = null;
            for (final DaytonaSwatchQuery q : daytonaSwatchQueries.values()) {
                if (q.getQueryFilePath().equals(fullpath)) dsq = q;
            }
            if (dsq == null) {
                dsq = new DaytonaSwatchQuery(filename, fullpath, c);
                final QueryCompileWorker w = new QueryCompileWorker(queryCompile, filename, dsq);
                threads.add(w);
                w.start();
            } else {
                dsq.addChannel(c);
            }
            daytonaSwatchQueries.put(c, dsq);
        }
        boolean swatchError = false;
        while (true) {
            boolean working = false;
            for (final QueryCompileWorker t : threads) {
                if (t.isErrorStatus()) {
                    swatchError = true;
                }
                if (t.isAlive()) working = true;
            }
            if (!working) break;
            try {
                Thread.sleep(SPINSLEEP);
            } catch (InterruptedException e) {
            }
        }
        if (swatchError) throw new IOException("query compilation failed");
        System.out.println("-- Finished compiling swatch queries");
        System.out.println("-- Compiling detail queries");
        threads.clear();
        for (String[] pair : detailQueryFilenames) {
            final String channelname = pair[0];
            final String filename = pair[1];
            final String fullpath = DataServiceDispatcher.DAYTONASERVICEPATH + filename;
            final InputChannelInterface c = getChannelFromListByName(d.getInputChannels(), channelname);
            DaytonaDetailQuery dsq = null;
            for (final DaytonaDetailQuery q : daytonaDetailQueries.values()) {
                if (q.getQueryFilePath().equals(fullpath)) dsq = q;
            }
            if (dsq == null) {
                dsq = new DaytonaDetailQuery(filename, fullpath, c);
                final QueryCompileWorker w = new QueryCompileWorker(queryCompile, filename, dsq);
                threads.add(w);
                w.start();
            } else {
                dsq.addChannel(c);
            }
            daytonaDetailQueries.put(c, dsq);
        }
        boolean detailError = false;
        while (true) {
            boolean working = false;
            for (final QueryCompileWorker t : threads) {
                if (t.isErrorStatus()) detailError = true;
                if (t.isAlive()) working = true;
            }
            if (!working) break;
            try {
                Thread.sleep(SPINSLEEP);
            } catch (InterruptedException e) {
            }
        }
        if (detailError) throw new IOException("query compilation failed");
        System.out.println("-- Finished compiling detail queries");
    }

    /**
	 * Retrieve the complete list of darkstar routers (data "sources").  
	 * 
	 * This method is temporarily unused - a bypass is in place to load the list 
	 * of sources from file until a new source query can be created.  
	 * 
	 * @return
	 * @throws IOException
	 */
    protected Map<String, SourceInterface> getSources() throws IOException {
        final Map<String, SourceInterface> result = new HashMap<String, SourceInterface>();
        try {
            final Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, -1);
            final String[] param = new String[] { c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH) };
            System.out.println("-- Router list parameter: " + Util.arrayToString(param));
            final Connection conn = ConnectionPool.getInstance().getConnection();
            final PreparedStatement stmt = getSourceStatement(conn);
            final ResultSet jdbcResultSet = QueryWorkerThread.readData(conn, stmt, param);
            ConnectionPool.getInstance().returnConnection(conn);
            System.out.println("-- Router list retrieved");
            while (jdbcResultSet.next()) {
                final String sourceInternalName = jdbcResultSet.getString("Name");
                final Source newSource = Source.createSource(sourceInternalName);
                result.put(sourceInternalName, newSource);
            }
            jdbcResultSet.close();
        } catch (Exception e) {
            System.err.println("SQL exception reading source list");
            throw new IOException(e.getMessage());
        }
        return result;
    }

    private PreparedStatement getSourceStatement(Connection conn) throws SQLException {
        final InputStream queryStream = Util.getJARresource(this.getClass().getClassLoader(), sourceQueryFilePath);
        final String query = Util.getInputStreamAsString(queryStream);
        return Day.getClass(conn).dayPrepareStatement("LR_" + System.currentTimeMillis(), query);
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
        final String intervalDesc = q.getInterval().getDescription();
        final long queryId = q.getQueryId();
        final List<SwatchRecordInterface> result = new ArrayList<SwatchRecordInterface>();
        final Set<DaytonaSwatchQuery> dqueries = new HashSet<DaytonaSwatchQuery>();
        for (final InputChannelItemInterface channelitem : q.getInputChannelItemList()) {
            if (channelitem instanceof InputChannelInterface) {
                dqueries.add(daytonaSwatchQueries.get(channelitem));
            }
        }
        final String[] querystring = new String[] { Integer.toString((int) (q.getBeginDate().getTime() / 1000)), Integer.toString((int) (q.getEndDate().getTime() / 1000)), makeCommaList(q.getSourceList()) };
        final ThreadGroup tg = new ThreadGroup("Swatch worker threads") {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                final SwatchWorkerThread swt = (SwatchWorkerThread) t;
                swt.setErrorStatus();
            }
        };
        final List<SwatchWorkerThread> threads = new ArrayList<SwatchWorkerThread>(dqueries.size());
        for (final DaytonaSwatchQuery dsq : dqueries) {
            final String workerName = "SwatchQuery worker " + dsq.getName();
            final SwatchWorkerThread t = new SwatchWorkerThread(tg, workerName, dsq, querystring, queryId);
            threads.add(t);
            System.out.println("-- starting swatch worker thread");
            t.start();
        }
        while (true) {
            boolean alive = false;
            for (final SwatchWorkerThread t : threads) {
                if (t.isErrorStatus()) throw new IOException("Swatch worker thread failed");
                if (t.isAlive()) alive = true; else {
                    final List<SwatchRecordInterface> r = t.getResult();
                    if (r != null) {
                        result.addAll(r);
                    }
                }
            }
            if (!alive) break;
            try {
                Thread.sleep(SPINSLEEP);
            } catch (InterruptedException e) {
                System.err.println("!! " + Thread.currentThread().getName() + " interrupted.");
            }
        }
        return result;
    }

    private static InputChannelInterface getChannelFromListByName(List<InputChannelInterface> channels, String channelInternalName) {
        for (final InputChannelInterface channel : channels) {
            if (channel.getInternalName().equals(channelInternalName)) return channel;
        }
        throw new RuntimeException("Channel not found");
    }

    /**
	 * Turns a list of objects into a comma separated string of the output of their toString methods
	 * @param list
	 * @return
	 */
    @SuppressWarnings("unchecked")
    static final String makeCommaList(List list) {
        String s = "";
        for (Object item : list) {
            s += item.toString() + ",";
        }
        s = s.substring(0, s.length() - 1);
        return s;
    }

    /**
	 * TODO: will probably want to do some kind of aggregate chart query in the future for 
	 * performance reasons where single query retrieves data for multiple charts 
	 * 
	 * @param chartDetailBulkQuery
	 * @return
	 */
    protected List<DetailRecord> getChartData(ChartDetailBulkQuery chartDetailBulkQuery) throws IOException {
        final String intervalDesc = chartDetailBulkQuery.getInterval().getDescription();
        final long queryId = chartDetailBulkQuery.getQueryId();
        final List<DetailRecord> result = new ArrayList<DetailRecord>();
        final Set<DaytonaDetailQuery> dqueries = new HashSet<DaytonaDetailQuery>();
        for (final InputChannelItemInterface channelitem : chartDetailBulkQuery.getChannelList()) {
            if (channelitem instanceof InputChannelInterface) {
                dqueries.add(daytonaDetailQueries.get(channelitem));
            }
        }
        try {
            final String[] querystring = new String[] { Integer.toString((int) (chartDetailBulkQuery.getBeginDate().getTime() / 1000)), Integer.toString((int) (chartDetailBulkQuery.getEndDate().getTime() / 1000)), makeCommaList(chartDetailBulkQuery.getSourceList()) };
            final ThreadGroup tg = new ThreadGroup("Detail worker threads") {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    final DetailWorkerThread dwt = (DetailWorkerThread) t;
                    dwt.setErrorStatus();
                }
            };
            final List<DetailWorkerThread> threads = new ArrayList<DetailWorkerThread>(dqueries.size());
            for (final DaytonaDetailQuery dsq : dqueries) {
                final String workerName = "DetailWorkerThread " + dsq.getName();
                final DetailWorkerThread t = new DetailWorkerThread(tg, workerName, dsq, querystring, queryId);
                threads.add(t);
                t.start();
            }
            while (true) {
                boolean alive = false;
                for (final DetailWorkerThread t : threads) {
                    if (t.isErrorStatus()) throw new IOException("Detail worker thread failed");
                    if (t.isAlive()) alive = true; else {
                        final List<DetailRecord> r = t.getResult();
                        if (r != null) {
                            result.addAll(r);
                        }
                    }
                }
                if (!alive) break;
                try {
                    Thread.sleep(SPINSLEEP);
                } catch (InterruptedException e) {
                    System.err.println("!! " + Thread.currentThread().getName() + " interrupted.");
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return result;
    }

    protected void init() throws IOException {
        try {
            ConnectionPool.initialize(DataServiceDispatcher.getJDBCurl(), DataServiceDispatcher.getParticipantId(), DataServiceDispatcher.getPassword());
        } catch (ClassNotFoundException c) {
            throw new IOException("Error loading daytona database driver.");
        } catch (IllegalAccessException i) {
            throw new IOException("Error loading daytona database driver.");
        } catch (InstantiationException c) {
            throw new IOException("Error loading daytona database driver.");
        } catch (SQLException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
	 * Meant to retrieve a complete Set of sources to be rendered.  In the case of 
	 * the daytona version, we don't (yet) have such a thing as a SourceGroup, so 
	 * the Set is actually just the input list of individual sources.  
	 * 
	 *  When SourceGroups are defined, this will instead return the UNION of 
	 *  each source belonging to each sourcegroup, and the list of individual
	 *  sources.
	 * 
	 * @param query
	 * @return
	 */
    protected Set<SourceInterface> getActiveSources(ActiveSourceQueryInterface query) {
        HashSet<SourceInterface> results = new HashSet<SourceInterface>();
        results.addAll(query.getSourceList());
        return results;
    }
}
