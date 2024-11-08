package org.riverock.webmill.portal.dao;

import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.riverock.interfaces.portal.bean.User;
import org.riverock.interfaces.portal.bean.Company;
import org.riverock.interfaces.sso.a3.AuthSession;
import org.riverock.generic.db.DatabaseAdapter;
import org.riverock.generic.db.DatabaseManager;
import org.riverock.generic.schema.db.CustomSequenceType;
import org.riverock.webmill.portal.bean.UserBean;
import org.riverock.common.tools.RsetTools;

/**
 * @author Sergei Maslyukov
 *         Date: 29.05.2006
 *         Time: 15:26:40
 */
@SuppressWarnings({ "UnusedAssignment" })
public class InternalUserDaoImpl implements InternalUserDao {

    private static final Logger log = Logger.getLogger(InternalUserDaoImpl.class);

    public User getUser(Long portalUserId, AuthSession authSession) {
        if (portalUserId == null) {
            return null;
        }
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select a.ID_USER,a.ID_FIRM,a.FIRST_NAME,a.MIDDLE_NAME,a.LAST_NAME, " + "       a.DATE_START_WORK,a.DATE_FIRE,a.ADDRESS,a.TELEPHONE,a.EMAIL, a.IS_DELETED " + "from   WM_LIST_USER a " + "where  a.ID_USER=? and a.IS_DELETED=0 and a.ID_FIRM in ";
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
            int num = 1;
            ps.setLong(num++, portalUserId);
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(num++, authSession.getUserLogin());
                    break;
            }
            rs = ps.executeQuery();
            UserBean beanPortal = null;
            if (rs.next()) {
                beanPortal = loadPortalUserFromResultSet(rs);
                final Company company = InternalDaoFactory.getInternalCompanyDao().getCompany(beanPortal.getCompanyId(), authSession);
                if (company != null) beanPortal.setCompanyName(company.getName()); else beanPortal.setCompanyName("Warning. Company not found");
            }
            return beanPortal;
        } catch (Exception e) {
            String es = "Error load portal user for id: " + portalUserId;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public User getUserByEMail(String eMail) {
        if (eMail == null) {
            return null;
        }
        DatabaseAdapter db = null;
        try {
            db = DatabaseAdapter.getInstance();
            return getUserByEMail(db, eMail);
        } catch (Exception e) {
            String es = "Error search user for e-mail: " + eMail;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db);
            db = null;
        }
    }

    public User getUserByEMail(DatabaseAdapter db, String eMail) {
        if (eMail == null) {
            return null;
        }
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            String sql = "select a.ID_USER,a.ID_FIRM,a.FIRST_NAME,a.MIDDLE_NAME,a.LAST_NAME, " + "       a.DATE_START_WORK,a.DATE_FIRE,a.ADDRESS,a.TELEPHONE,a.EMAIL, a.IS_DELETED " + "from   WM_LIST_USER a " + "where  a.EMAIL=?";
            ps = db.prepareStatement(sql);
            ps.setString(1, eMail);
            rs = ps.executeQuery();
            UserBean beanPortal = null;
            if (rs.next()) {
                beanPortal = loadPortalUserFromResultSet(rs);
            }
            return beanPortal;
        } catch (Exception e) {
            String es = "Error search user for e-mail: " + eMail;
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(rs, ps);
            rs = null;
            ps = null;
        }
    }

    public List<User> getUserList(AuthSession authSession) {
        DatabaseAdapter db = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            db = DatabaseAdapter.getInstance();
            String sql = "select a.ID_USER,a.ID_FIRM,a.FIRST_NAME,a.MIDDLE_NAME,a.LAST_NAME," + "       a.DATE_START_WORK,a.DATE_FIRE,a.ADDRESS,a.TELEPHONE,a.EMAIL,a.IS_DELETED " + "from   WM_LIST_USER a " + "where  a.IS_DELETED=0  and a.ID_FIRM in ";
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
            int num = 1;
            switch(db.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(num++, authSession.getUserLogin());
                    break;
            }
            rs = ps.executeQuery();
            List<User> list = new ArrayList<User>();
            while (rs.next()) {
                UserBean beanPortal = loadPortalUserFromResultSet(rs);
                final Company company = InternalDaoFactory.getInternalCompanyDao().getCompany(beanPortal.getCompanyId(), authSession);
                if (company != null) beanPortal.setCompanyName(company.getName()); else beanPortal.setCompanyName("Warning. Ccompany not found");
                list.add(beanPortal);
            }
            return list;
        } catch (Exception e) {
            String es = "Error load list of portal users";
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(db, rs, ps);
            db = null;
            rs = null;
            ps = null;
        }
    }

    public Long addUser(User portalUserBean) {
        DatabaseAdapter dbDyn = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            Long userId = addUser(dbDyn, portalUserBean);
            dbDyn.commit();
            return userId;
        } catch (Exception e) {
            try {
                if (dbDyn != null) dbDyn.rollback();
            } catch (Exception e001) {
            }
            String es = "Error add new portal user";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn);
            dbDyn = null;
        }
    }

    public Long addUser(DatabaseAdapter dbDyn, User portalUserBean) {
        log.debug("Start addUserInfo");
        if (portalUserBean == null) {
            throw new IllegalStateException("portalUserBean is null");
        }
        PreparedStatement ps = null;
        try {
            Long sequenceValue;
            if (portalUserBean.getUserId() == null) {
                CustomSequenceType seq = new CustomSequenceType();
                seq.setSequenceName("seq_WM_LIST_USER");
                seq.setTableName("WM_LIST_USER");
                seq.setColumnName("ID_USER");
                sequenceValue = dbDyn.getSequenceNextValue(seq);
            } else {
                sequenceValue = portalUserBean.getUserId();
            }
            String sql = "insert into WM_LIST_USER " + "(ID_USER,ID_FIRM,FIRST_NAME,MIDDLE_NAME,LAST_NAME,DATE_START_WORK, " + "ADDRESS,TELEPHONE,EMAIL) " + "values " + (dbDyn.getIsNeedUpdateBracket() ? "(" : "") + "?,?,?,?,?,?,?,?,? " + (dbDyn.getIsNeedUpdateBracket() ? ")" : "");
            ps = dbDyn.prepareStatement(sql);
            int num = 1;
            ps.setLong(num++, sequenceValue);
            ps.setLong(num++, portalUserBean.getCompanyId());
            ps.setString(num++, portalUserBean.getFirstName());
            ps.setString(num++, portalUserBean.getMiddleName());
            ps.setString(num++, portalUserBean.getLastName());
            RsetTools.setTimestamp(ps, num++, new Timestamp(System.currentTimeMillis()));
            ps.setString(num++, portalUserBean.getAddress());
            ps.setString(num++, portalUserBean.getPhone());
            ps.setString(num++, portalUserBean.getEmail());
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    break;
            }
            ps.executeUpdate();
            return sequenceValue;
        } catch (Exception e) {
            String es = "Error add new portal user";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(ps);
            ps = null;
        }
    }

    public void updateUser(User portalUserBean, AuthSession authSession) {
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            String sql = "update WM_LIST_USER " + "set    FIRST_NAME=?,MIDDLE_NAME=?,LAST_NAME=?, " + "       ADDRESS=?,TELEPHONE=?,EMAIL=? " + "where  ID_USER=? and is_deleted=0 and  ID_FIRM in ";
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
            ps.setString(num++, portalUserBean.getFirstName());
            ps.setString(num++, portalUserBean.getMiddleName());
            ps.setString(num++, portalUserBean.getLastName());
            ps.setString(num++, portalUserBean.getAddress());
            ps.setString(num++, portalUserBean.getPhone());
            ps.setString(num++, portalUserBean.getEmail());
            ps.setLong(num++, portalUserBean.getUserId());
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
                if (dbDyn != null) {
                    dbDyn.rollback();
                }
            } catch (Exception e001) {
            }
            String es = "Error update of portal user";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    public void deleteUser(User portalUserBean, AuthSession authSession) {
        DatabaseAdapter dbDyn = null;
        PreparedStatement ps = null;
        try {
            dbDyn = DatabaseAdapter.getInstance();
            if (portalUserBean.getUserId() == null) throw new IllegalArgumentException("id of portal user is null");
            String sql = "update WM_LIST_USER " + "set    is_deleted=1 " + "where  ID_USER=? and is_deleted = 0 and ID_FIRM in ";
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
            ps.setLong(num++, portalUserBean.getUserId());
            switch(dbDyn.getFamaly()) {
                case DatabaseManager.MYSQL_FAMALY:
                    break;
                default:
                    ps.setString(num++, authSession.getUserLogin());
                    break;
            }
            int i1 = ps.executeUpdate();
            if (log.isDebugEnabled()) log.debug("Count of deleted records - " + i1);
            dbDyn.commit();
        } catch (Exception e) {
            try {
                if (dbDyn != null) {
                    dbDyn.rollback();
                }
            } catch (Exception e001) {
            }
            String es = "Error delete of portal user";
            log.error(es, e);
            throw new IllegalStateException(es, e);
        } finally {
            DatabaseManager.close(dbDyn, ps);
            dbDyn = null;
            ps = null;
        }
    }

    private UserBean loadPortalUserFromResultSet(ResultSet rs) throws Exception {
        UserBean bean = new UserBean();
        bean.setUserId(RsetTools.getLong(rs, "ID_USER"));
        bean.setCompanyId(RsetTools.getLong(rs, "ID_FIRM"));
        bean.setFirstName(RsetTools.getString(rs, "FIRST_NAME"));
        bean.setMiddleName(RsetTools.getString(rs, "MIDDLE_NAME"));
        bean.setLastName(RsetTools.getString(rs, "LAST_NAME"));
        bean.setCreatedDate(RsetTools.getTimestamp(rs, "DATE_START_WORK"));
        bean.setDeletedDate(RsetTools.getTimestamp(rs, "DATE_FIRE"));
        bean.setAddress(RsetTools.getString(rs, "ADDRESS"));
        bean.setPhone(RsetTools.getString(rs, "TELEPHONE"));
        bean.setEmail(RsetTools.getString(rs, "EMAIL"));
        bean.setDeleted(RsetTools.getInt(rs, "IS_DELETED", 0) == 1);
        return bean;
    }
}
