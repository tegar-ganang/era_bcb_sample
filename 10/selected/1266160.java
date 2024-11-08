package org.riverock.webmill.portal.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.db.DatabaseManager;
import org.riverock.generic.schema.db.CustomSequenceType;
import org.riverock.interfaces.portal.bean.PortletName;
import org.riverock.webmill.core.GetWmPortalPortletNameItem;
import org.riverock.webmill.core.InsertWmPortalPortletNameItem;
import org.riverock.webmill.portal.bean.PortletNameBean;
import org.riverock.webmill.schema.core.WmPortalPortletNameItemType;
import org.riverock.common.tools.RsetTools;

/**
 * @author Sergei Maslyukov
 *         Date: 05.05.2006
 *         Time: 14:52:29
 */
@SuppressWarnings({ "UnusedAssignment" })
public class InternalPortletNameDaoImpl implements InternalPortletNameDao {

    private static final Logger log = Logger.getLogger(InternalPortletNameDaoImpl.class);

    public PortletName getPortletName(Long portletId) {
        DatabaseAdapter adapter = null;
        try {
            adapter = DatabaseAdapter.getInstance();
            WmPortalPortletNameItemType ctxType = GetWmPortalPortletNameItem.getInstance(adapter, portletId).item;
            PortletNameBean bean = new PortletNameBean();
            if (ctxType != null) {
                bean.setPortletName(ctxType.getType());
                bean.setPortletId(portletId);
            }
            return bean;
        } catch (Exception e) {
            String es = "Error get getSiteBean()";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(adapter);
            adapter = null;
        }
    }

    public PortletName getPortletName(String portletName) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        DatabaseAdapter adapter = null;
        try {
            adapter = DatabaseAdapter.getInstance();
            ps = adapter.prepareStatement("select * from WM_PORTAL_PORTLET_NAME where TYPE=?");
            ps.setString(1, portletName);
            rs = ps.executeQuery();
            PortletNameBean bean = null;
            if (rs.next()) {
                WmPortalPortletNameItemType item = GetWmPortalPortletNameItem.fillBean(rs);
                bean = new PortletNameBean();
                bean.setPortletId(item.getIdSiteCtxType());
                bean.setPortletName(item.getType());
                bean.setActive(false);
            }
            return bean;
        } catch (Exception e) {
            String es = "Error get getPortletName()";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(adapter, rs, ps);
            adapter = null;
            rs = null;
            ps = null;
        }
    }

    public List<PortletName> getPortletNameList() {
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select ID_SITE_CTX_TYPE, TYPE " + "from 	WM_PORTAL_PORTLET_NAME ";
            ps = db.prepareStatement(sql);
            rs = ps.executeQuery();
            List<PortletName> list = new ArrayList<PortletName>();
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

    public Long createPortletName(PortletName portletName) {
        DatabaseAdapter adapter = null;
        try {
            adapter = DatabaseAdapter.getInstance();
            CustomSequenceType seq = new CustomSequenceType();
            seq.setSequenceName("seq_WM_PORTAL_PORTLET_NAME");
            seq.setTableName("WM_PORTAL_PORTLET_NAME");
            seq.setColumnName("ID_SITE_CTX_TYPE");
            Long id = adapter.getSequenceNextValue(seq);
            WmPortalPortletNameItemType item = new WmPortalPortletNameItemType();
            item.setIdSiteCtxType(id);
            item.setType(portletName.getPortletName());
            InsertWmPortalPortletNameItem.process(adapter, item);
            adapter.commit();
            return id;
        } catch (Throwable e) {
            try {
                if (adapter != null) adapter.rollback();
            } catch (Throwable th) {
            }
            String es = "Error create portlet name";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(adapter);
            adapter = null;
        }
    }

    public void updatePortletName(PortletName portletNameBean) {
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
                if (dbDyn != null) dbDyn.rollback();
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

    public void deletePortletName(PortletName portletNameBean) {
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
                if (dbDyn != null) dbDyn.rollback();
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

    private PortletName loadPortletNameFromResultSet(ResultSet rs) throws Exception {
        PortletNameBean bean = new PortletNameBean();
        bean.setPortletId(RsetTools.getLong(rs, "ID_SITE_CTX_TYPE"));
        bean.setPortletName(RsetTools.getString(rs, "TYPE"));
        return bean;
    }
}
