package com.apc.websiteschema.res.fms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import websiteschema.conf.Configure;
import websiteschema.utils.DbUtil;

/**
 *
 * @author ray
 */
public class FmsData {

    private static final Configure conf = new Configure("configure-site.ini");

    private static final Logger l = Logger.getLogger(FmsData.class);

    private static FmsData ins = new FmsData();

    public static FmsData getInstance() {
        return ins;
    }

    private volatile Info info;

    public FmsData() {
        load();
    }

    class Info {

        ChannelInfo channelInfo;

        SourceInfo sourceInfo;

        ApcColumnInfo apcColumnInfo;

        RegionInfo regionInfo;
    }

    class ChannelInfo {

        Map<String, FmsChannel> repo = new HashMap<String, FmsChannel>();

        ChannelInfo(List<FmsChannel> list) {
            if (null != list) {
                for (FmsChannel e : list) {
                    repo.put(e.getJobname(), e);
                }
            }
        }

        FmsChannel getChannel(String jobname) {
            return repo.get(jobname);
        }
    }

    class ApcColumnInfo {

        Map<String, FmsApcColumn> repo = new HashMap<String, FmsApcColumn>();

        ApcColumnInfo(List<FmsApcColumn> list) {
            if (null != list) {
                for (FmsApcColumn e : list) {
                    repo.put(e.getId(), e);
                }
            }
        }

        FmsApcColumn getColumn(String id) {
            return repo.get(id);
        }

        List<FmsApcColumn> getColumnCascade(String id) {
            List<FmsApcColumn> ret = null;
            if (repo.containsKey(id)) {
                FmsApcColumn fac = repo.get(id);
                ret = new ArrayList<FmsApcColumn>();
                ret.add(fac);
                String parentId = fac.getParentId();
                List<FmsApcColumn> cascade = getColumnCascade(parentId);
                if (null != cascade) {
                    ret.addAll(cascade);
                }
            }
            return ret;
        }
    }

    class RegionInfo {

        Map<String, FmsRegion> repo = new HashMap<String, FmsRegion>();

        RegionInfo(List<FmsRegion> list) {
            if (null != list) {
                for (FmsRegion e : list) {
                    repo.put(e.getId(), e);
                }
            }
        }

        FmsRegion getColumn(String id) {
            return repo.get(id);
        }

        List<FmsRegion> getColumnCascade(String id) {
            List<FmsRegion> ret = null;
            if (repo.containsKey(id)) {
                FmsRegion fac = repo.get(id);
                ret = new ArrayList<FmsRegion>();
                ret.add(fac);
                String parentId = fac.getParentId();
                List<FmsRegion> cascade = getColumnCascade(parentId);
                if (null != cascade) {
                    ret.addAll(cascade);
                }
            }
            return ret;
        }
    }

    class SourceInfo {

        Map<String, FmsSource> repo = new HashMap<String, FmsSource>();

        SourceInfo(List<FmsSource> list) {
            if (null != list) {
                for (FmsSource e : list) {
                    repo.put(e.getId(), e);
                }
            }
        }

        FmsSource getSource(String id) {
            return repo.get(id);
        }
    }

    public FmsChannel getChannel(String jobname) {
        if (null != info) {
            return info.channelInfo.getChannel(jobname);
        }
        return null;
    }

    public FmsSource getSource(String id) {
        if (null != info) {
            return info.sourceInfo.getSource(id);
        }
        return null;
    }

    public FmsApcColumn getApcColumn(String id) {
        if (null != info) {
            return info.apcColumnInfo.getColumn(id);
        }
        return null;
    }

    public List<FmsApcColumn> getApcColumnCascade(String id) {
        if (null != info) {
            return info.apcColumnInfo.getColumnCascade(id);
        }
        return null;
    }

    public FmsRegion getRegion(String id) {
        if (null != info) {
            return info.regionInfo.getColumn(id);
        }
        return null;
    }

    public List<FmsRegion> getRegionCascade(String id) {
        if (null != info) {
            return info.regionInfo.getColumnCascade(id);
        }
        return null;
    }

    public final void load() {
        Connection conn = createConnection();
        if (null != conn) {
            try {
                Statement stmt = conn.createStatement();
                List<FmsChannel> chnls = loadChannel(stmt);
                List<FmsSource> sources = loadSource(stmt);
                List<FmsApcColumn> columns = loadApcColumn(stmt);
                List<FmsRegion> regions = loadRegion(stmt);
                if (null != chnls && null != sources && null != columns && null != regions) {
                    l.debug("All data loaded.");
                    ChannelInfo ci = new ChannelInfo(chnls);
                    SourceInfo si = new SourceInfo(sources);
                    ApcColumnInfo aci = new ApcColumnInfo(columns);
                    RegionInfo ri = new RegionInfo(regions);
                    Info i = new Info();
                    i.apcColumnInfo = aci;
                    i.channelInfo = ci;
                    i.sourceInfo = si;
                    i.regionInfo = ri;
                    info = i;
                }
            } catch (Exception ex) {
                l.error(ex.getMessage(), ex);
            } finally {
                try {
                    conn.close();
                } catch (Exception e) {
                    l.error(e.getMessage(), e);
                }
            }
        }
    }

    private Connection createConnection() {
        Connection conn = null;
        String url = conf.getProperty("FMS", "fms.jdbc.url", "jdbc:oracle:thin:@10.8.0.160:1521:fms");
        String driver = conf.getProperty("FMS", "fms.jdbc.driver", "oracle.jdbc.driver.OracleDriver");
        String userName = conf.getProperty("FMS", "fms.jdbc.username", "fcm");
        String password = conf.getProperty("FMS", "fms.jdbc.password", "fcm");
        try {
            Class.forName(driver).newInstance();
            conn = DriverManager.getConnection(url, userName, password);
            return conn;
        } catch (Exception ex) {
            l.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * select id, biz_job jobname, channel_name name, source_id sourceId, apc_column_id apcColumnId, region_id regionId from Job_Info where valid = 1;
     * @param conn
     * @return
     * @throws Exception
     */
    private List<FmsChannel> loadChannel(Statement stmt) throws Exception {
        String sql = "select id, biz_job jobname, channel_name name, source_id sourceId, apc_column_id apcColumnId from Job_Info where valid = 1";
        l.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        List<Map<String, String>> res = DbUtil.listFromRS(rs);
        if (null != res) {
            List<FmsChannel> ret = new ArrayList<FmsChannel>();
            for (Map<String, String> map : res) {
                FmsChannel obj = FmsChannel.apply(map);
                ret.add(obj);
            }
            return ret;
        }
        return null;
    }

    /**
     * select tmp.*, t2.source_type_name smallKind from
    (
    select
    s.source_id id, s.source_name name, s.source_type_id1 type_id1, t.source_type_name bigKind, s.source_type_id2 type_id2, s.if_core core, s.source_url url, region_id, source_expertise, source_originality, source_influence
    from source_info s
    left join source_type_info t on t.source_type_id = s.source_type_id1
    ) tmp, source_type_info t2 where tmp.type_id2 = t2.source_type_id;
     * @param conn
     * @return
     * @throws Exception
     */
    private List<FmsSource> loadSource(Statement stmt) throws Exception {
        String sql = "select tmp.*, t2.source_type_name smallKind from ( select id,name,type_id1,bigKind,type_id2,core,url,regionId,sourceExpertise,sourceOriginality,sourceInfluence,ltrim(max(sys_connect_by_path(sourceKindName,',')),',') as sourceKindName from ( select s.source_id id, s.source_name name, s.source_type_id1 type_id1, t.source_type_name bigKind, s.source_type_id2 type_id2, s.if_core core, s.source_url url, region_id regionId, source_expertise sourceExpertise, source_originality sourceOriginality, source_influence sourceInfluence, sci.source_kind_name sourceKindName, s.source_id+(row_number() over(order by s.source_id)) node_id, row_number() over(partition by s.source_id order by s.source_id) rn from source_info s left join source_type_info t on t.source_type_id = s.source_type_id1 left join source_kind_link sc on sc.source_id = s.source_id left join source_kind_info sci on sci.source_kind_id = sc.source_kind_id ) start with rn = 1 connect by node_id-1 = prior node_id    group by id,name,type_id1,bigKind,type_id2,core,url,regionId,sourceExpertise,sourceOriginality,sourceInfluence ) tmp, source_type_info t2 where tmp.type_id2 = t2.source_type_id";
        l.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        List<Map<String, String>> res = DbUtil.listFromRS(rs);
        if (null != res) {
            List<FmsSource> ret = new ArrayList<FmsSource>();
            for (Map<String, String> map : res) {
                FmsSource obj = FmsSource.apply(map);
                ret.add(obj);
            }
            return ret;
        }
        return null;
    }

    /**
     * select apc_column_id id, apc_column_name name, parent_id parentId, depth from apc_column_info;
     * @param conn
     * @return
     * @throws Exception
     */
    private List<FmsApcColumn> loadApcColumn(Statement stmt) throws Exception {
        String sql = "select apc_column_id id, apc_column_name name, parent_id parentId, depth from apc_column_info";
        l.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        List<Map<String, String>> res = DbUtil.listFromRS(rs);
        if (null != res) {
            List<FmsApcColumn> ret = new ArrayList<FmsApcColumn>();
            for (Map<String, String> map : res) {
                FmsApcColumn obj = FmsApcColumn.apply(map);
                ret.add(obj);
            }
            return ret;
        }
        return null;
    }

    /**
     * select id, name, parent_id parentId, depth from region_info where status=1
     * @param conn
     * @return
     * @throws Exception
     */
    private List<FmsRegion> loadRegion(Statement stmt) throws Exception {
        String sql = "select id, name, parent_id parentId, depth from region_info where status=1";
        l.debug(sql);
        ResultSet rs = stmt.executeQuery(sql);
        List<Map<String, String>> res = DbUtil.listFromRS(rs);
        if (null != res) {
            List<FmsRegion> ret = new ArrayList<FmsRegion>();
            for (Map<String, String> map : res) {
                FmsRegion obj = FmsRegion.apply(map);
                ret.add(obj);
            }
            return ret;
        }
        return null;
    }
}
