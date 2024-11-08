package org.riverock.webmill.portal.dao;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.riverock.interfaces.portal.bean.Company;
import org.riverock.interfaces.sso.a3.AuthSession;
import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.db.DatabaseManager;
import org.riverock.generic.schema.db.CustomSequenceType;
import org.riverock.common.tools.RsetTools;
import org.riverock.webmill.portal.bean.CompanyBean;

/**
 * @author SergeMaslyukov
 *         Date: 30.01.2006
 *         Time: 1:24:25
 *         $Id: InternalCompanyDaoImpl.java,v 1.8 2006/06/05 19:18:57 serg_main Exp $
 */
@SuppressWarnings({ "UnusedAssignment" })
public class InternalCompanyDaoImpl implements InternalCompanyDao {

    private static final Logger log = Logger.getLogger(InternalCompanyDaoImpl.class);

    public Company getCompany(String companyName) {
        if (companyName == null) {
            return null;
        }
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select ID_FIRM, full_name, short_name, " + "	    address, chief, buh, url,  " + "	    short_info, is_work, is_search, is_deleted " + "from 	WM_LIST_COMPANY " + "where  full_name=? ";
            ps = db.prepareStatement(sql);
            ps.setString(1, companyName);
            rs = ps.executeQuery();
            Company company = null;
            if (rs.next()) {
                company = loadCompanyFromResultSet(rs);
            }
            return company;
        } catch (Exception e) {
            String es = "Error load company for name: " + companyName;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public Company getCompany(Long companyId, AuthSession authSession) {
        if (companyId == null) {
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
            String sql = "select ID_FIRM, full_name, short_name, " + "	    address, chief, buh, url,  " + "	    short_info, is_work, is_search, is_deleted " + "from 	WM_LIST_COMPANY " + "where  is_deleted=0 and ID_FIRM=? and ID_FIRM in ";
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedCompanyId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_FIRM from v$_read_list_firm z1 where z1.user_login = ?)";
                    break;
            }
            ps = db.prepareStatement(sql);
            int idx = 1;
            ps.setLong(idx++, companyId);
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(idx++, authSession.getUserLogin());
                    break;
            }
            rs = ps.executeQuery();
            Company company = null;
            if (rs.next()) {
                company = loadCompanyFromResultSet(rs);
            }
            return company;
        } catch (Exception e) {
            String es = "Error load company for id: " + companyId;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public List<Company> getCompanyList(AuthSession authSession) {
        if (authSession == null) {
            return null;
        }
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select ID_FIRM, full_name, short_name,\n" + "	address, chief, buh, url, \n" + "	short_info, is_work, is_search, is_deleted " + "from 	WM_LIST_COMPANY " + "where  is_deleted=0 and ID_FIRM in ";
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedCompanyId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_FIRM from v$_read_list_firm z1 where z1.user_login = ?)";
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
            List<Company> list = new ArrayList<Company>();
            while (rs.next()) {
                list.add(loadCompanyFromResultSet(rs));
            }
            return list;
        } catch (Exception e) {
            String es = "Error load company list for userLogin: " + authSession.getUserLogin();
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public Long processAddCompany(Company companyBean, Long holdingId) {
        PreparedStatement ps = null;
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            CustomSequenceType seq = new CustomSequenceType();
            seq.setSequenceName("seq_WM_LIST_COMPANY");
            seq.setTableName("WM_LIST_COMPANY");
            seq.setColumnName("ID_FIRM");
            Long sequenceValue = dbDyn.getSequenceNextValue(seq);
            ps = dbDyn.prepareStatement("insert into WM_LIST_COMPANY (" + "	ID_FIRM, " + "	full_name, " + "	short_name, " + "	address, " + "	chief, " + "	buh, " + "	url, " + "	short_info, " + "   is_deleted" + ")values " + (dbDyn.getIsNeedUpdateBracket() ? "(" : "") + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "   0 " + (dbDyn.getIsNeedUpdateBracket() ? ")" : ""));
            int num = 1;
            RsetTools.setLong(ps, num++, sequenceValue);
            ps.setString(num++, companyBean.getName());
            ps.setString(num++, companyBean.getShortName());
            ps.setString(num++, companyBean.getAddress());
            ps.setString(num++, companyBean.getCeo());
            ps.setString(num++, companyBean.getCfo());
            ps.setString(num++, companyBean.getWebsite());
            ps.setString(num++, companyBean.getInfo());
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of inserted records - " + i1);
            if (holdingId != null) {
                InternalDaoFactory.getInternalHoldingDao().setRelateHoldingCompany(dbDyn, holdingId, sequenceValue);
            }
            dbDyn.commit();
            return sequenceValue;
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error add new company";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public Long processAddCompany(Company companyBean, String userLogin, Long holdingId, AuthSession authSession) {
        if (authSession == null) {
            return null;
        }
        PreparedStatement ps = null;
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            CustomSequenceType seq = new CustomSequenceType();
            seq.setSequenceName("seq_WM_LIST_COMPANY");
            seq.setTableName("WM_LIST_COMPANY");
            seq.setColumnName("ID_FIRM");
            Long sequenceValue = dbDyn.getSequenceNextValue(seq);
            ps = dbDyn.prepareStatement("insert into WM_LIST_COMPANY (" + "	ID_FIRM, " + "	full_name, " + "	short_name, " + "	address, " + "	telefon_buh, " + "	telefon_chief, " + "	chief, " + "	buh, " + "	fax, " + "	email, " + "	icq, " + "	short_client_info, " + "	url, " + "	short_info, " + "is_deleted" + ")" + (dbDyn.getIsNeedUpdateBracket() ? "(" : "") + " select " + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?," + "	?,0 from WM_AUTH_USER " + "where USER_LOGIN=? " + (dbDyn.getIsNeedUpdateBracket() ? ")" : ""));
            int num = 1;
            RsetTools.setLong(ps, num++, sequenceValue);
            ps.setString(num++, companyBean.getName());
            ps.setString(num++, companyBean.getShortName());
            ps.setString(num++, companyBean.getAddress());
            ps.setString(num++, "");
            ps.setString(num++, "");
            ps.setString(num++, companyBean.getCeo());
            ps.setString(num++, companyBean.getCfo());
            ps.setString(num++, "");
            ps.setString(num++, "");
            RsetTools.setLong(ps, num++, null);
            ps.setString(num++, "");
            ps.setString(num++, companyBean.getWebsite());
            ps.setString(num++, companyBean.getInfo());
            ps.setString(num++, userLogin);
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of inserted records - " + i1);
            if (holdingId != null) {
                InternalDaoFactory.getInternalHoldingDao().setRelateHoldingCompany(dbDyn, holdingId, sequenceValue);
            }
            dbDyn.commit();
            return sequenceValue;
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error add new company";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public void processSaveCompany(Company companyBean, AuthSession authSession) {
        if (authSession == null) {
            return;
        }
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            String sql = "UPDATE WM_LIST_COMPANY " + "SET " + "	full_name = ?, " + "	short_name = ?, " + "	address = ?, " + "	telefon_buh = ?, " + "	telefon_chief = ?, " + "	chief = ?, " + "	buh = ?, " + "	fax = ?, " + "	email = ?, " + "	icq = ?, " + "	short_client_info = ?, " + "	url = ?, " + "	short_info = ? " + "WHERE ID_FIRM = ? and ID_FIRM in ";
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedCompanyId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_FIRM from v$_read_list_firm z1 where z1.user_login = ?)";
                    break;
            }
            ps = dbDyn.prepareStatement(sql);
            int num = 1;
            ps.setString(num++, companyBean.getName());
            ps.setString(num++, companyBean.getShortName());
            ps.setString(num++, companyBean.getAddress());
            ps.setString(num++, "");
            ps.setString(num++, "");
            ps.setString(num++, companyBean.getCeo());
            ps.setString(num++, companyBean.getCfo());
            ps.setString(num++, "");
            ps.setString(num++, "");
            RsetTools.setLong(ps, num++, null);
            ps.setString(num++, "");
            ps.setString(num++, companyBean.getWebsite());
            ps.setString(num++, companyBean.getInfo());
            RsetTools.setLong(ps, num++, companyBean.getId());
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(num++, authSession.getUserLogin());
                    break;
            }
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of updated record - " + i1);
            dbDyn.commit();
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error save company";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public void processDeleteCompany(Company companyBean, AuthSession authSession) {
        if (authSession == null) {
            return;
        }
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            if (companyBean.getId() == null) throw new IllegalArgumentException("companyId is null");
            String sql = "update WM_LIST_COMPANY set is_deleted = 1 " + "where  ID_FIRM = ? and ID_FIRM in ";
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    String idList = authSession.getGrantedCompanyId();
                    sql += " (" + idList + ") ";
                    break;
                default:
                    sql += "(select z1.ID_FIRM from v$_read_list_firm z1 where z1.user_login = ?)";
                    break;
            }
            ps = dbDyn.prepareStatement(sql);
            RsetTools.setLong(ps, 1, companyBean.getId());
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
            String es = "Error delete company";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    private CompanyBean loadCompanyFromResultSet(ResultSet rs) throws Exception {
        CompanyBean company = new CompanyBean();
        company.setId(RsetTools.getLong(rs, "ID_FIRM"));
        company.setName(RsetTools.getString(rs, "full_name"));
        company.setShortName(RsetTools.getString(rs, "short_name"));
        company.setAddress(RsetTools.getString(rs, "address"));
        company.setCeo(RsetTools.getString(rs, "chief"));
        company.setCfo(RsetTools.getString(rs, "buh"));
        company.setWebsite(RsetTools.getString(rs, "url"));
        company.setInfo(RsetTools.getString(rs, "short_info"));
        company.setDeleted(RsetTools.getInt(rs, "is_deleted", 0) == 1);
        return company;
    }
}
