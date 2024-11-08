package org.riverock.portlet.manager.portletname;

import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import org.apache.log4j.Logger;
import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.db.DatabaseManager;
import org.riverock.generic.schema.db.CustomSequenceType;
import org.riverock.common.tools.RsetTools;

/**
 * @author SergeMaslyukov
 *         Date: 26.02.2006
 *         Time: 15:50:56
 *         $Id: PortletNameDaoImpl.java,v 1.3 2006/06/05 19:19:22 serg_main Exp $
 */
@SuppressWarnings({ "UnusedAssignment" })
public class PortletNameDaoImpl {

    private static final Logger log = Logger.getLogger(PortletNameDaoImpl.class);

    public PortletNameBean getPortletName(Long portletNameId) {
        if (portletNameId == null) {
            return null;
        }
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select ID_SITE_CTX_TYPE, TYPE " + "from 	WM_PORTAL_PORTLET_NAME " + "where  ID_SITE_CTX_TYPE=? ";
            ps = db.prepareStatement(sql);
            ps.setLong(1, portletNameId);
            rs = ps.executeQuery();
            PortletNameBean bean = null;
            if (rs.next()) {
                bean = loadPortletNameFromResultSet(rs);
            }
            return bean;
        } catch (Exception e) {
            String es = "Error load portlet name for id: " + portletNameId;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public List<PortletNameBean> getPortletNameList() {
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select ID_SITE_CTX_TYPE, TYPE " + "from 	WM_PORTAL_PORTLET_NAME ";
            ps = db.prepareStatement(sql);
            rs = ps.executeQuery();
            List<PortletNameBean> list = new ArrayList<PortletNameBean>();
            while (rs.next()) {
                list.add(loadPortletNameFromResultSet(rs));
            }
            return list;
        } catch (Exception e) {
            String es = "Error load list of portlet names";
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public Long addPortletName(PortletNameBean portletNameBean) {
        PreparedStatement ps = null;
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            CustomSequenceType seq = new CustomSequenceType();
            seq.setSequenceName("seq_WM_PORTAL_PORTLET_NAME");
            seq.setTableName("WM_PORTAL_PORTLET_NAME");
            seq.setColumnName("ID_SITE_CTX_TYPE");
            Long sequenceValue = dbDyn.getSequenceNextValue(seq);
            ps = dbDyn.prepareStatement("insert into WM_PORTAL_PORTLET_NAME " + "( ID_SITE_CTX_TYPE, TYPE ) " + "values " + (dbDyn.getIsNeedUpdateBracket() ? "(" : "") + " ?, ?" + (dbDyn.getIsNeedUpdateBracket() ? ")" : ""));
            RsetTools.setLong(ps, 1, sequenceValue);
            ps.setString(2, portletNameBean.getPortletName());
            ps.executeUpdate();
            dbDyn.commit();
            return sequenceValue;
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error add new portlet name ";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public void updatePortletName(PortletNameBean portletNameBean) {
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            String sql = "update WM_PORTAL_PORTLET_NAME " + "set    TYPE=? " + "where  ID_SITE_CTX_TYPE=?";
            ps = dbDyn.prepareStatement(sql);
            ps.setString(1, portletNameBean.getPortletName());
            RsetTools.setLong(ps, 2, portletNameBean.getPortletId());
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of updated record - " + i1);
            dbDyn.commit();
        } catch (Exception e) {
            try {
                dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error save portlet name";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public void deletePortletName(PortletNameBean portletNameBean) {
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            if (portletNameBean.getPortletId() == null) throw new IllegalArgumentException("portletNameId is null");
            String sql = "delete from  WM_PORTAL_PORTLET_NAME " + "where  ID_SITE_CTX_TYPE=?";
            ps = dbDyn.prepareStatement(sql);
            RsetTools.setLong(ps, 1, portletNameBean.getPortletId());
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of deleted records - " + i1);
            dbDyn.commit();
        } catch (Exception e) {
            try {
                dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error delete portlet name";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    private PortletNameBean loadPortletNameFromResultSet(ResultSet rs) throws Exception {
        PortletNameBean bean = new PortletNameBean();
        bean.setPortletId(RsetTools.getLong(rs, "ID_SITE_CTX_TYPE"));
        bean.setPortletName(RsetTools.getString(rs, "TYPE"));
        return bean;
    }
}
