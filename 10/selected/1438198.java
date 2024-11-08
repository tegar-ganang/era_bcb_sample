package org.riverock.webmill.portal.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.riverock.common.tools.RsetTools;
import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.db.DatabaseManager;
import org.riverock.generic.schema.db.CustomSequenceType;
import org.riverock.interfaces.portal.bean.Holding;
import org.riverock.interfaces.sso.a3.AuthSession;
import org.riverock.webmill.portal.bean.HoldingBean;

/**
 * @author SergeMaslyukov
 *         Date: 30.01.2006
 *         Time: 1:24:25
 *         $Id: InternalHoldingDaoImpl.java,v 1.3 2006/06/05 19:18:57 serg_main Exp $
 */
@SuppressWarnings({ "UnusedAssignment" })
public class InternalHoldingDaoImpl implements InternalHoldingDao {

    private static final Logger log = Logger.getLogger(InternalHoldingDaoImpl.class);

    public Holding loadHolding(Long holdingId, AuthSession authSession) {
        if (holdingId == null) {
            return null;
        }
        if (authSession == null) {
            return null;
        }
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select ID_HOLDING, full_name_HOLDING, NAME_HOLDING " + "from 	WM_LIST_HOLDING " + "where  ID_HOLDING=? and ID_HOLDING in ";
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedHoldingId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_ROAD from v$_read_list_road z1 where z1.user_login = ?)";
                    break;
            }
            ps = db.prepareStatement(sql);
            int idx = 1;
            ps.setLong(idx++, holdingId);
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(idx++, authSession.getUserLogin());
                    break;
            }
            rs = ps.executeQuery();
            HoldingBean holding = null;
            if (rs.next()) {
                holding = loadHoldingFromResultSet(rs);
                holding.setCompanyIdList(getCompanyIdList(db, holding.getId(), authSession));
            }
            return holding;
        } catch (Exception e) {
            String es = "Error load holding for id: " + holdingId;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public List<Holding> getHoldingList(AuthSession authSession) {
        if (authSession == null) {
            return null;
        }
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select ID_HOLDING, full_name_HOLDING, NAME_HOLDING " + "from 	WM_LIST_HOLDING " + "where  ID_HOLDING in ";
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedHoldingId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_ROAD from v$_read_list_road z1 where z1.user_login = ?)";
                    break;
            }
            ps = db.prepareStatement(sql);
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(1, authSession.getUserLogin());
                    break;
            }
            rs = ps.executeQuery();
            List<Holding> list = new ArrayList<Holding>();
            while (rs.next()) {
                HoldingBean holding = loadHoldingFromResultSet(rs);
                holding.setCompanyIdList(getCompanyIdList(db, holding.getId(), authSession));
                list.add(holding);
            }
            return list;
        } catch (Exception e) {
            String es = "Error load holding list for userLogin: " + authSession.getUserLogin();
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public Long processAddHolding(Holding holdingBean, AuthSession authSession) {
        if (authSession == null) {
            return null;
        }
        PreparedStatement ps = null;
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            CustomSequenceType seq = new CustomSequenceType();
            seq.setSequenceName("seq_WM_LIST_HOLDING");
            seq.setTableName("WM_LIST_HOLDING");
            seq.setColumnName("ID_HOLDING");
            Long sequenceValue = dbDyn.getSequenceNextValue(seq);
            ps = dbDyn.prepareStatement("insert into WM_LIST_HOLDING " + "( ID_HOLDING, full_name_HOLDING, NAME_HOLDING )" + "values " + (dbDyn.getIsNeedUpdateBracket() ? "(" : "") + " ?, ?, ? " + (dbDyn.getIsNeedUpdateBracket() ? ")" : ""));
            int num = 1;
            RsetTools.setLong(ps, num++, sequenceValue);
            ps.setString(num++, holdingBean.getName());
            ps.setString(num++, holdingBean.getShortName());
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of inserted records - " + i1);
            HoldingBean bean = new HoldingBean(holdingBean);
            bean.setId(sequenceValue);
            processInsertRelatedCompany(dbDyn, bean, authSession);
            dbDyn.commit();
            return sequenceValue;
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error add new holding";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public void processSaveHolding(Holding holdingBean, AuthSession authSession) {
        if (authSession == null) {
            return;
        }
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            String sql = "UPDATE WM_LIST_HOLDING " + "SET " + "   full_name_HOLDING=?, " + "   NAME_HOLDING=? " + "WHERE ID_HOLDING = ? and ID_HOLDING in ";
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedHoldingId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_ROAD from v$_read_list_road z1 where z1.user_login = ?)";
                    break;
            }
            ps = dbDyn.prepareStatement(sql);
            int num = 1;
            ps.setString(num++, holdingBean.getName());
            ps.setString(num++, holdingBean.getShortName());
            RsetTools.setLong(ps, num++, holdingBean.getId());
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(num++, authSession.getUserLogin());
                    break;
            }
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of updated record - " + i1);
            processDeleteRelatedCompany(dbDyn, holdingBean, authSession);
            processInsertRelatedCompany(dbDyn, holdingBean, authSession);
            dbDyn.commit();
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error save holding";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public void processDeleteHolding(Holding holdingBean, AuthSession authSession) {
        if (authSession == null) {
            return;
        }
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            if (holdingBean.getId() == null) throw new IllegalArgumentException("holdingId is null");
            processDeleteRelatedCompany(dbDyn, holdingBean, authSession);
            String sql = "delete from WM_LIST_HOLDING " + "where  ID_HOLDING=? and ID_HOLDING in ";
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedHoldingId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_ROAD from v$_read_list_road z1 where z1.user_login = ?)";
                    break;
            }
            ps = dbDyn.prepareStatement(sql);
            RsetTools.setLong(ps, 1, holdingBean.getId());
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(2, authSession.getUserLogin());
                    break;
            }
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of deleted records - " + i1);
            dbDyn.commit();
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error delete holding";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public List<Long> getCompanyIdList(DatabaseAdapter db, Long holdingId, AuthSession authSession) {
        if (holdingId == null) {
            return null;
        }
        if (authSession == null) {
            return null;
        }
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            String sql = "select ID_COMPANY " + "from 	WM_LIST_R_HOLDING_COMPANY " + "where  ID_HOLDING=? and ID_HOLDING in ";
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedHoldingId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_ROAD from v$_read_list_road z1 where z1.user_login = ?)";
                    break;
            }
            ps = db.prepareStatement(sql);
            int idx = 1;
            ps.setLong(idx++, holdingId);
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(idx++, authSession.getUserLogin());
                    break;
            }
            rs = ps.executeQuery();
            List<Long> ids = new ArrayList<Long>();
            while (rs.next()) {
                ids.add(RsetTools.getLong(rs, "ID_COMPANY"));
            }
            return ids;
        } catch (Exception e) {
            String es = "Error load company id list for holding id: " + holdingId;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(rs, ps);
            rs = null;
            ps = null;
        }
    }

    public void setRelateHoldingCompany(DatabaseAdapter ora_, Long holdingId, Long companyId) {
        PreparedStatement ps = null;
        try {
            if (!getRelateHoldingCompany(ora_, companyId, holdingId)) {
                CustomSequenceType seq = new CustomSequenceType();
                seq.setSequenceName("seq_WM_LIST_R_HOLDING_COMPANY");
                seq.setTableName("WM_LIST_R_HOLDING_COMPANY");
                seq.setColumnName("ID_REL_HOLDING");
                long id = ora_.getSequenceNextValue(seq);
                ps = ora_.prepareStatement("insert into WM_LIST_R_HOLDING_COMPANY " + "(ID_REL_HOLDING, ID_HOLDING, ID_COMPANY) " + "values " + "(?, ?, ?)");
                ps.setLong(1, id);
                ps.setObject(2, holdingId);
                ps.setObject(3, companyId);
                int i = ps.executeUpdate();
                if (log.isDebugEnabled()) log.debug("Count of added record in WM_LIST_R_HOLDING_COMPANY: " + i);
            }
        } catch (Exception e) {
            final String es = "Error setRelateHoldingCompany()";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(ps);
            ps = null;
        }
    }

    private void processDeleteRelatedCompany(DatabaseAdapter dbDyn, Holding holdingBean, AuthSession authSession) {
        if (authSession == null) {
            return;
        }
        PreparedStatement ps = null;
        try {
            if (holdingBean.getId() == null) throw new IllegalArgumentException("holdingId is null");
            String sql = "delete from wm_list_r_holding_company " + "where  ID_HOLDING=? and ID_HOLDING in ";
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedHoldingId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_ROAD from v$_read_list_road z1 where z1.user_login = ?)";
                    break;
            }
            ps = dbDyn.prepareStatement(sql);
            RsetTools.setLong(ps, 1, holdingBean.getId());
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(2, authSession.getUserLogin());
                    break;
            }
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of deleted records - " + i1);
        } catch (Exception e) {
            String es = "Error delete holding";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(ps);
            ps = null;
        }
    }

    private void processInsertRelatedCompany(DatabaseAdapter dbDyn, Holding holdingBean, AuthSession authSession) {
        if (authSession == null || holdingBean.getCompanyIdList() == null) {
            return;
        }
        if (holdingBean.getId() == null) throw new IllegalArgumentException("holdingId is null");
        for (Long companyId : holdingBean.getCompanyIdList()) {
            setRelateHoldingCompany(dbDyn, holdingBean.getId(), companyId);
        }
    }

    private boolean getRelateHoldingCompany(DatabaseAdapter ora_, Long companyId, Long holdingId) throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = ora_.prepareStatement("select null COUNT_REC from WM_LIST_R_HOLDING_COMPANY " + "where ID_COMPANY=? and ID_HOLDING=?");
            ps.setObject(1, companyId);
            ps.setObject(2, holdingId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            log.error("Error getRelateServiceFirm", e);
            throw e;
        } finally {
            DatabaseManager.close(rs, ps);
            rs = null;
            ps = null;
        }
    }

    private HoldingBean loadHoldingFromResultSet(ResultSet rs) throws Exception {
        HoldingBean holding = new HoldingBean();
        holding.setId(RsetTools.getLong(rs, "ID_HOLDING"));
        holding.setName(RsetTools.getString(rs, "full_name_HOLDING"));
        holding.setShortName(RsetTools.getString(rs, "name_HOLDING"));
        return holding;
    }
}
