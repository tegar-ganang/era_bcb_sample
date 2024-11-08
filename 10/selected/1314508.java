package com.entelience.metrics.events;

import com.entelience.sql.Db;
import com.entelience.sql.DbHelper;
import com.entelience.sql.SqlArray;
import com.entelience.util.FileHelper;
import com.entelience.objects.metricsquery.ResultBean;
import com.entelience.objects.metricsquery.QueryBean;
import com.entelience.objects.metricsquery.QueryBeanItem;
import com.entelience.objects.metricsquery.QueryType;
import com.entelience.objects.metricsquery.Metric;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Database routines for loading and unloading various
 * SDW (Security Data Warehouse) metrics are all loaded
 * through this class.
 * <br/>
 * This class contains a mixture of static methods (for
 * managing general features of configuration bundles and
 * 
 */
public final class SDW {

    public static final int STATE_READY = 0;

    public static final int STATE_STARTED = 1;

    public static final int STATE_COMPLETE = 2;

    public static final int STATE_NEEDS_UNDO = 3;

    public static final int STATE_UNDO_STARTED = 4;

    public static final int STATE_UNDONE = 5;

    private final Bundle cfg;

    /**
     * Constructor.  This class contains both static methods
     * and stateful methods which need a shared configuration bundle.
     * <br/>
     * For "stateful" methods which keep a configuration
     * around, 
     */
    private SDW(Bundle cfg) {
        this.cfg = cfg;
    }

    /**
     * Open an SDW configured by a specific XML file.  Note that
     * there is no parameter for location of the XML file - it 
     * is expected that the database is queried for this.
     * <br/>
     * UNIT TEST METHOD ONLY.  Otherwise use forMetrics().
     */
    public static SDW forFile(Db db, String xmlFileName) throws Exception {
        Bundle bundle = new Bundle();
        try {
            db.enter();
            PreparedStatement ps = db.prepareStatement("SELECT DISTINCT e_bundle_id, xml_decl_path, xml_text FROM sdw.e_bundle WHERE xml_decl_path = ?;");
            ps.setString(1, xmlFileName);
            ResultSet rs = db.executeQuery(ps);
            if (rs.next()) {
                loadMetricsCfg(bundle, rs.getString(2), rs.getString(3));
            } else {
                throw new IllegalStateException("Could not find configuration for metrics defined in file " + xmlFileName);
            }
            return new SDW(bundle);
        } finally {
            db.exit();
        }
    }

    /**
     * Open an SDW configured for the provided query.
     */
    public static SDW forQuery(Db db, QueryBean query) throws Exception {
        Set<String> s = new HashSet<String>();
        Iterator<QueryBeanItem> qbiIter = query.queryItemIterator();
        while (qbiIter.hasNext()) {
            QueryBeanItem qbi = qbiIter.next();
            QueryType qt = qbi.getQuery();
            Iterator<Metric> metricsIter = qt.metricsIterator();
            while (metricsIter.hasNext()) {
                Metric metric = metricsIter.next();
                s.add(metric.getMetric());
            }
        }
        int size = s.size();
        if (size == 0) return null;
        String metricOrGroupNames[] = new String[size];
        Iterator<String> i = s.iterator();
        int j = 0;
        while (i.hasNext()) {
            metricOrGroupNames[j++] = i.next();
        }
        return forMetrics(db, metricOrGroupNames);
    }

    /**
     * Open an SDW configured for the provided metrics.
     */
    public static SDW forMetrics(Db db, String metricOrGroupNames[]) throws Exception {
        Bundle bundle = new Bundle();
        try {
            db.enter();
            PreparedStatement ps = db.prepareStatement("SELECT DISTINCT b.e_bundle_id, b.xml_decl_path, b.xml_text FROM sdw.e_bundle b WHERE (b.e_bundle_id IN (SELECT n.bundle_id FROM sdw.e_metric_name n WHERE n.metric_name = ANY(?))) OR (b.e_bundle_id IN (SELECT g.bundle_id FROM sdw.e_metric_group g WHERE g.metric_group_name = ANY(?)));");
            SqlArray ary = new SqlArray(metricOrGroupNames);
            ps.setObject(1, ary);
            ps.setObject(2, ary);
            ResultSet rs = db.executeQuery(ps);
            if (rs.next()) {
                do {
                    loadMetricsCfg(bundle, rs.getString(2), rs.getString(3));
                } while (rs.next());
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append("Could not find configuration for ");
                if (metricOrGroupNames == null || metricOrGroupNames.length == 0) {
                    sb.append("< undefined metrics >");
                } else {
                    for (int i = 0; i < metricOrGroupNames.length; ++i) {
                        if (i > 0) sb.append(", ");
                        sb.append(metricOrGroupNames[i]);
                    }
                }
                throw new IllegalStateException(sb.toString());
            }
            return new SDW(bundle);
        } finally {
            db.exit();
        }
    }

    /**
     * Run this once to prepare the database for new metrics.
     * <br/>
     * The xml file will be stored in the sdw schema and parsed
     * for metrics and groups which will then be written into the
     * relevant tables.
     * <br/>
     * @param db database connection (no txn started) for work.
     * @param xmlFileName path to the xml file which defines some metrics to be updated in the database.
     * @return true if successfully updated, false if metrics already defined (or overlap)
     * @throws Exception with information if logical error
     */
    public static boolean installMetricsCfg(Db db, String xmlFileName) throws Exception {
        String xmlText = FileHelper.asString(xmlFileName);
        Bundle bundle = new Bundle();
        loadMetricsCfg(bundle, xmlFileName, xmlText);
        try {
            db.begin();
            PreparedStatement psExists = db.prepareStatement("SELECT e_bundle_id, xml_decl_path, xml_text FROM sdw.e_bundle WHERE xml_decl_path = ?;");
            psExists.setString(1, xmlFileName);
            ResultSet rsExists = db.executeQuery(psExists);
            if (rsExists.next()) {
                db.rollback();
                return false;
            }
            PreparedStatement psId = db.prepareStatement("SELECT currval('sdw.e_bundle_serial');");
            PreparedStatement psAdd = db.prepareStatement("INSERT INTO sdw.e_bundle (xml_decl_path, xml_text, sdw_major_version, sdw_minor_version, file_major_version, file_minor_version) VALUES (?, ?, ?, ?, ?, ?);");
            psAdd.setString(1, xmlFileName);
            psAdd.setString(2, xmlText);
            FileInformation fi = bundle.getSingleFileInformation();
            if (!xmlFileName.equals(fi.filename)) throw new IllegalStateException("FileInformation bad for " + xmlFileName);
            psAdd.setInt(3, Globals.SDW_MAJOR_VER);
            psAdd.setInt(4, Globals.SDW_MINOR_VER);
            psAdd.setInt(5, fi.majorVer);
            psAdd.setInt(6, fi.minorVer);
            if (1 != db.executeUpdate(psAdd)) {
                throw new IllegalStateException("Could not add " + xmlFileName);
            }
            int bundleId = DbHelper.getIntKey(psId);
            PreparedStatement psGroupId = db.prepareStatement("SELECT currval('sdw.e_metric_group_serial');");
            PreparedStatement psAddGroup = db.prepareStatement("INSERT INTO sdw.e_metric_group (bundle_id, metric_group_name) VALUES (?, ?);");
            psAddGroup.setInt(1, bundleId);
            PreparedStatement psMetricId = db.prepareStatement("SELECT currval('sdw.e_metric_name_serial');");
            PreparedStatement psAddMetric = db.prepareStatement("INSERT INTO sdw.e_metric_name (bundle_id, metric_name) VALUES (?, ?);");
            psAddMetric.setInt(1, bundleId);
            PreparedStatement psAddGroup2Metric = db.prepareStatement("INSERT INTO sdw.e_metric_groups (metric_name_id, metric_group_id) VALUES (?, ?);");
            Iterator<MetricGroup> i = bundle.getAllMetricGroups();
            while (i.hasNext()) {
                MetricGroup grp = i.next();
                psAddGroup.setString(2, grp.groupName);
                if (1 != db.executeUpdate(psAddGroup)) throw new IllegalStateException("Could not add group " + grp.groupName + " from " + xmlFileName);
                int groupId = DbHelper.getIntKey(psGroupId);
                psAddGroup2Metric.setInt(2, groupId);
                Iterator<String> j = grp.getAllMetricNames();
                while (j.hasNext()) {
                    String metricName = j.next();
                    psAddMetric.setString(2, metricName);
                    if (1 != db.executeUpdate(psAddMetric)) throw new IllegalStateException("Could not add " + metricName + " from " + xmlFileName);
                    int metricId = DbHelper.getIntKey(psMetricId);
                    psAddGroup2Metric.setInt(1, metricId);
                    if (1 != db.executeUpdate(psAddGroup2Metric)) throw new IllegalStateException("Could not add group " + grp.groupName + " -> " + metricName + " from " + xmlFileName);
                }
            }
            return true;
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.commitUnless();
        }
    }

    /**
     * Load configuration from one or more XML files into a configuration bundle.
     * <br/>
     * @param cfg configuration bundle
     * @param xmlFileName file name to the xml to define all the metrics in a bundle.
     * @param xmlText if not null then this contains the actual XML text defining the metrics, already loaded from the file.
     */
    private static void loadMetricsCfg(Bundle bundle, String xmlFileName, String xmlText) throws Exception {
        if (bundle == null) throw new IllegalArgumentException("Bundle is NULL");
        MetricsXMLParser parser = MetricsXMLParser.newParser();
        parser.parse(bundle, xmlFileName, xmlText);
    }

    /**
     * Create an SDW operation db object.
     */
    public SDWOperationDb newSDWOperationDb(Db db) throws Exception {
        SDWOperationDb sdwOpDb = new SDWOperationDb();
        sdwOpDb.setSDW(this);
        sdwOpDb.setDb(db);
        return sdwOpDb;
    }

    /**
     * Load an event import database object for the given metric.  This object, along with
     * the "Position" object, can be used to import data to the database.
     */
    public EventImportDb getEventImportDb(Db db, String metricName) throws Exception {
        return cfg.getEventImportDb(db, metricName);
    }

    /**
     * Get an object that can run the metrics (re-) computation for a given time period.
     */
    public EventCalculationDb getEventCalculationDb(Db db, String metricName) throws Exception {
        return cfg.getEventCalculationDb(db, metricName, newSDWOperationDb(db));
    }

    /**
     * Perform ANY well-specified query on the database.
     */
    public static ResultBean query(Db db, QueryBean query) throws Exception {
        try {
            db.enter();
            SDW sdw = SDW.forQuery(db, query);
            EventDb evdb = sdw.cfg.getEventDb(db, query);
            return evdb.query(sdw.cfg, query);
        } finally {
            db.exit();
        }
    }

    /**
     * Retrieve the minimally-specified position vector for any given metric.
     * <br/>
     * Other objects, not in the position vector, will not be written to the event database.
     */
    public Position getPosition(String metricName) {
        return cfg.getPosition(metricName);
    }

    /**
     * Get this object's configuration bundle.  Is this really needed?
     */
    public Bundle getBundle() {
        return cfg;
    }
}
