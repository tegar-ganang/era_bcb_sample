package hambo.pim;

import java.sql.*;
import java.math.BigDecimal;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.lang.NumberFormatException;
import hambo.svc.*;
import hambo.svc.database.*;

/**
 * Business object for externalcontacts
 */
public class ExternalContactBO extends BaseBO {

    private ExternalContactDO DO = null;

    public static final int RESET = 0;

    public static final int INSERT = 1;

    public static final int UPDATE = 2;

    public static final int DELETE = 3;

    public static final int EMAIL = 4;

    public static final int SMS = 5;

    public static final int FAX = 6;

    public static final int ICQ = 7;

    public static final int NONE = 8;

    public static final int CONTACT_LIMIT = 1000;

    public ExternalContactBO() {
        super();
        DO = new ExternalContactDO();
    }

    public ExternalContactBO(BigDecimal oid, BigDecimal owner) throws SQLException {
        this(oid, owner, true);
    }

    public ExternalContactBO(BigDecimal oid, BigDecimal owner, boolean html) throws SQLException {
        super();
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select * from  cont_Contact where OId=? and owner=?");
            ps.setBigDecimal(1, oid);
            ps.setBigDecimal(2, owner);
            rs = con.executeQuery(ps, null);
            if (rs.next() == false) {
                throw new DataObjectNotFoundException("ExternalContactDO: [oid=" + oid + "]");
            }
            DO = new ExternalContactDO(rs, html);
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public ExternalContactDO getDO() {
        return DO;
    }

    public void insertExternalContact(ExternalContactDO xDO) throws SQLException {
        DO = xDO;
        insertExternalContact();
    }

    public void insertExternalContact() throws SQLException, LimitExceededException {
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select count(*) from cont_Contact where owner=?");
            ps.setBigDecimal(1, DO.getOwner());
            rs = con.executeQuery(ps, null);
            rs.next();
            if (rs.getInt(1) >= CONTACT_LIMIT) {
                fireUserEvent("ExternalContactBO insertExternalContact: Contact limit exceeded");
                throw new LimitExceededException("fail");
            }
            con.reset();
            ps = con.prepareStatement("insert into cont_Contact (owner,firstname,lastname,nickname,title,organization," + "orgunit,emailaddr,homeph,workph,cellph,im,imno,fax,homeaddr,homelocality," + "homeregion,homepcode,homecountry,workaddr,worklocality,workregion,workpcode," + "workcountry,website,wapsite,comments,birthday,syncstatus,dirtybits," + "quicklist) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setBigDecimal(1, DO.getOwner());
            ps.setString(2, DO.getFirstname());
            ps.setString(3, DO.getLastname());
            ps.setString(4, DO.getNickname());
            ps.setString(5, DO.getTitle());
            ps.setString(6, DO.getOrganization());
            ps.setString(7, DO.getOrgunit());
            ps.setString(8, DO.getEmail());
            ps.setString(9, DO.getHomeph());
            ps.setString(10, DO.getWorkph());
            ps.setString(11, DO.getCellph());
            ps.setString(12, DO.getIm());
            ps.setString(13, DO.getImno());
            ps.setString(14, DO.getFax());
            ps.setString(15, DO.getHomeaddr());
            ps.setString(16, DO.getHomelocality());
            ps.setString(17, DO.getHomeregion());
            ps.setString(18, DO.getHomepcode());
            ps.setString(19, DO.getHomecountry());
            ps.setString(20, DO.getWorkaddr());
            ps.setString(21, DO.getWorklocality());
            ps.setString(22, DO.getWorkregion());
            ps.setString(23, DO.getWorkpcode());
            ps.setString(24, DO.getWorkcountry());
            ps.setString(25, DO.getWebsite());
            ps.setString(26, DO.getWapsite());
            ps.setString(27, DO.getComments());
            if (DO.getBirthday() != null) ps.setDate(28, DO.getBirthday()); else ps.setNull(28, Types.DATE);
            ps.setInt(29, INSERT);
            ps.setInt(30, DO.getDirtybits());
            ps.setInt(31, 0);
            con.executeUpdate(ps, null);
            ps = con.prepareStatement(DBUtil.getQueryCurrentOID(con, "cont_Contact", "newoid"));
            rs = con.executeQuery(ps, null);
            if (rs.next()) DO.setOId(rs.getBigDecimal("newoid"));
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public void updateExternalContact(ExternalContactDO xDO) throws SQLException {
        DO = xDO;
        updateExternalContact();
    }

    public void updateExternalContact() throws SQLException {
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("update cont_Contact set owner=?,firstname=?,lastname=?,nickname=?,title=?," + "organization=?,orgunit=?,emailaddr=?,homeph=?,workph=?,cellph=?,im=?,imno=?," + "fax=?,homeaddr=?,homelocality=?,homeregion=?,homepcode=?,homecountry=?," + "workaddr=?,worklocality=?,workregion=?,workpcode=?,workcountry=?,website=?," + "wapsite=?,comments=?,birthday=?,syncstatus=?,dirtybits=? where OId=?");
            ps.setBigDecimal(1, DO.getOwner());
            ps.setString(2, DO.getFirstname());
            ps.setString(3, DO.getLastname());
            ps.setString(4, DO.getNickname());
            ps.setString(5, DO.getTitle());
            ps.setString(6, DO.getOrganization());
            ps.setString(7, DO.getOrgunit());
            ps.setString(8, DO.getEmail());
            ps.setString(9, DO.getHomeph());
            ps.setString(10, DO.getWorkph());
            ps.setString(11, DO.getCellph());
            ps.setString(12, DO.getIm());
            ps.setString(13, DO.getImno());
            ps.setString(14, DO.getFax());
            ps.setString(15, DO.getHomeaddr());
            ps.setString(16, DO.getHomelocality());
            ps.setString(17, DO.getHomeregion());
            ps.setString(18, DO.getHomepcode());
            ps.setString(19, DO.getHomecountry());
            ps.setString(20, DO.getWorkaddr());
            ps.setString(21, DO.getWorklocality());
            ps.setString(22, DO.getWorkregion());
            ps.setString(23, DO.getWorkpcode());
            ps.setString(24, DO.getWorkcountry());
            ps.setString(25, DO.getWebsite());
            ps.setString(26, DO.getWapsite());
            ps.setString(27, DO.getComments());
            if (DO.getBirthday() != null) ps.setDate(28, DO.getBirthday()); else ps.setNull(28, Types.DATE);
            if (DO.getSyncstatus() == RESET) ps.setInt(29, UPDATE); else ps.setInt(29, DO.getSyncstatus());
            ps.setInt(30, DO.getDirtybits());
            ps.setBigDecimal(31, DO.getOId());
            con.executeUpdate(ps, null);
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public void updateQuicklist(BigDecimal owner, String[] adresses, int type) throws SQLException {
        if (adresses == null) return;
        try {
            StringBuffer buf = new StringBuffer("('");
            con = allocateConnection(tableName);
            if (adresses != null) {
                for (int i = 0; i < adresses.length; i++) buf.append(DBUtil.escapeQuoteSign(con, adresses[i].trim(), '\'')).append("','");
                if (adresses.length > 0) buf.delete(buf.length() - 2, buf.length() - 1);
            }
            buf.append("')");
            doUpdateQuicklist(owner, buf.toString(), type);
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public void updateQuicklist(BigDecimal owner, String adresses, int type) throws SQLException {
        if (adresses == null || adresses.equals("")) return;
        try {
            StringTokenizer tok = new StringTokenizer(adresses, ",", true);
            StringBuffer buf = new StringBuffer("('");
            String str = null;
            con = allocateConnection(tableName);
            while (tok.hasMoreTokens()) {
                if ((str = tok.nextToken()).equals(",")) buf.append("','"); else buf.append(DBUtil.escapeQuoteSign(con, str.trim(), '\''));
            }
            buf.append("')");
            doUpdateQuicklist(owner, buf.toString(), type);
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    private void doUpdateQuicklist(BigDecimal owner, String adresses, int type) throws SQLException {
        String str = null;
        switch(type) {
            case EMAIL:
                str = "emailaddr";
                break;
            case SMS:
                str = "cellph";
                break;
            case FAX:
                str = "fax";
                break;
            case ICQ:
                str = "imno";
                break;
        }
        StringBuffer sql = new StringBuffer("update cont_Contact set quicklist=quicklist+1 where owner=");
        sql.append(owner).append(" and ").append(str).append(" in ").append(adresses);
        con.executeUpdate(sql.toString());
    }

    public static Vector getExternals(String uoid) throws SQLException {
        return getExternals(uoid, 0);
    }

    public static Vector getExternals(String uoid, int externalorder) throws SQLException {
        String orderby = null;
        switch(externalorder) {
            case 1:
                orderby = "nickname desc";
                break;
            case 2:
                orderby = "emailaddr";
                break;
            case 3:
                orderby = "emailaddr desc";
                break;
            default:
                orderby = "nickname";
                break;
        }
        ExternalContactBO xBO = new ExternalContactBO();
        return xBO.getAllExternalContacts(new BigDecimal(uoid), orderby);
    }

    public Vector getAllExternalContacts(BigDecimal owner, String orderby) throws SQLException {
        return getAllExternalContacts(owner, orderby, true);
    }

    public Vector getAllExternalContacts(BigDecimal owner, String orderby, boolean html) throws SQLException {
        Vector exts = new Vector();
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select * from  cont_Contact where owner=? and syncstatus<? order by " + orderby);
            ps.setBigDecimal(1, owner);
            ps.setInt(2, DELETE);
            rs = con.executeQuery(ps, null);
            while (rs.next()) {
                exts.addElement(new ExternalContactDO(rs, html));
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return exts;
    }

    public Hashtable getAllExternalContactsHashed(BigDecimal owner, boolean html) throws SQLException {
        Hashtable exts = new Hashtable();
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select * from  cont_Contact where owner=? and syncstatus<?");
            ps.setBigDecimal(1, owner);
            ps.setInt(2, DELETE);
            rs = con.executeQuery(ps, null);
            ExternalContactDO eDO = null;
            while (rs.next()) {
                eDO = new ExternalContactDO(rs, html);
                exts.put(eDO.getOId().toString(), eDO);
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return exts;
    }

    public Vector getQuickList(BigDecimal owner, boolean html, int howmany) throws SQLException {
        Vector exts = new Vector();
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("select * from  cont_Contact where owner=? and syncstatus<? order by quicklist desc");
            ps.setBigDecimal(1, owner);
            ps.setInt(2, DELETE);
            rs = con.executeQuery(ps, null);
            int i = 0;
            while (rs.next() && i++ < howmany) {
                exts.addElement(new ExternalContactDO(rs, html));
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return exts;
    }

    public String getCommValuesByName(BigDecimal owner, String names, int type) throws SQLException {
        if (names == null || names.equals("")) return "";
        StringBuffer addresses = new StringBuffer();
        try {
            StringTokenizer tok = new StringTokenizer(names, ",", true);
            StringBuffer buf = new StringBuffer("('");
            String str = null;
            Vector rejects = new Vector();
            con = allocateConnection(tableName);
            while (tok.hasMoreTokens()) {
                if ((str = tok.nextToken()).equals(",")) buf.append("','"); else {
                    str = str.trim();
                    buf.append(DBUtil.escapeQuoteSign(con, str, '\''));
                    if (type == EMAIL && isValidEmailAddress(str)) addresses.append(str).append(','); else if ((type == SMS || type == FAX || type == ICQ) && isValidPhoneNr(str)) addresses.append(str).append(','); else rejects.addElement(str);
                }
            }
            buf.append("')");
            switch(type) {
                case EMAIL:
                    str = "emailaddr";
                    break;
                case SMS:
                    str = "cellph";
                    break;
                case FAX:
                    str = "fax";
                    break;
                case ICQ:
                    str = "imno";
                    break;
            }
            StringBuffer sql = new StringBuffer("select cont_Contact.").append(str);
            sql.append(",cont_Contact.nickname");
            sql.append(" from cont_Contact");
            sql.append(" where cont_Contact.owner=").append(owner);
            sql.append(" and cont_Contact.syncstatus<").append(DELETE);
            sql.append(" and cont_Contact.").append(str).append("!=''");
            sql.append(" and cont_Contact.nickname in ").append(buf.toString());
            sql.append(" UNION ");
            sql.append("select distinct cont_Contact.").append(str).append(",cont_Contact.nickname");
            sql.append(" from cont_Contact,cont_Contact_Group_Rel,cont_Group");
            sql.append(" where cont_Contact.owner=").append(owner);
            sql.append(" and cont_Contact.syncstatus<").append(DELETE);
            sql.append(" and cont_Contact.").append(str).append("!=''");
            sql.append(" and cont_Contact_Group_Rel.folder=cont_Group.oid");
            sql.append(" and cont_Contact_Group_Rel.externalcontact=cont_Contact.oid and cont_Group.owner=");
            sql.append(owner).append(" and cont_Group.name in ").append(buf.toString());
            rs = con.executeQuery(sql.toString());
            while (rs.next()) {
                addresses.append(rs.getString(str)).append(',');
                rejects.remove(rs.getString("nickname"));
            }
            if (rejects.size() > 0) {
                sql = new StringBuffer("select name from cont_Group where owner=").append(owner);
                sql.append(" and name in ").append(buf.toString());
                con.reset();
                rs = con.executeQuery(sql.toString());
                while (rs.next()) rejects.remove(rs.getString("name"));
            }
            if (rejects.size() > 0) {
                StringBuffer err = new StringBuffer("(@cocouldnotsendto@) : ");
                for (int i = 0; i < rejects.size(); i++) err.append((String) rejects.elementAt(i)).append(",");
                throw new NoSuchContactException(err.substring(0, err.length() - 1));
            }
            int len = addresses.length();
            if (len > 0) addresses.deleteCharAt(len - 1);
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return addresses.toString();
    }

    public Vector getExternalsByNickname(String owner, String nickname, String orderby) throws SQLException {
        return getExternalsByNickname(owner, nickname, orderby, true);
    }

    public Vector getExternalsByNickname(String owner, String nickname, String orderby, boolean html) throws SQLException {
        return getExternalsByNickname(owner, nickname, orderby, html, NONE);
    }

    public Vector getExternalsByNickname(String owner, String nickname, String orderby, boolean html, int noEmpty) throws SQLException {
        Vector exts = new Vector();
        String str = null;
        if (noEmpty != NONE) {
            if (noEmpty == EMAIL) str = "emailaddr"; else if (noEmpty == SMS) str = "cellph"; else if (noEmpty == FAX) str = "fax"; else if (noEmpty == ICQ) str = "imno";
        }
        try {
            con = allocateConnection(tableName);
            StringBuffer sql = new StringBuffer("select * from cont_Contact where syncstatus<").append(DELETE);
            sql.append(" and owner=").append(owner);
            if (str != null) sql.append(" and ").append(str).append("!=''");
            if (nickname != null && (!nickname.equals(""))) {
                sql.append(" and ").append(DBUtil.getQueryUpper(con, "nickname")).append(" like ");
                sql.append(DBUtil.getQueryUpper(con, "'" + DBUtil.escape(con, nickname, '\'') + "%' ")).append(" escape '\' ");
            }
            sql.append(" order by ").append(orderby);
            rs = con.executeQuery(sql.toString());
            while (rs.next()) {
                exts.addElement(new ExternalContactDO(rs, html));
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return exts;
    }

    public Vector upSync(BigDecimal owner, boolean fullSync) throws SQLException {
        Vector exts = new Vector();
        try {
            boolean foundone = false;
            con = allocateConnection(tableName);
            if (fullSync) {
                ps = con.prepareStatement("select * from  cont_Contact where owner=? order by syncstatus");
                ps.setBigDecimal(1, owner);
            } else {
                ps = con.prepareStatement("select * from  cont_Contact where owner=? and syncstatus>? order by syncstatus");
                ps.setBigDecimal(1, owner);
                ps.setInt(2, RESET);
            }
            rs = con.executeQuery(ps, null);
            while (rs.next()) {
                exts.addElement(new ExternalContactDO(rs));
                foundone = true;
            }
            if (foundone) {
                PreparedStatement delfc = con.prepareStatement("delete from cont_Contact_Group_Rel " + "where externalcontact in " + "(select OId from cont_Contact where owner=? " + "and syncstatus=?)");
                PreparedStatement delx = con.prepareStatement("delete from cont_Contact where owner=? and syncstatus=?");
                PreparedStatement update = con.prepareStatement("update cont_Contact set syncstatus=?,dirtybits=? " + "where owner=?");
                delfc.setBigDecimal(1, owner);
                delfc.setInt(2, DELETE);
                delx.setBigDecimal(1, owner);
                delx.setInt(2, DELETE);
                update.setInt(1, RESET);
                update.setInt(2, RESET);
                update.setBigDecimal(3, owner);
                con.reset();
                con.executeUpdate(delfc, null);
                con.reset();
                con.executeUpdate(delx, null);
                con.reset();
                con.executeUpdate(update, null);
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return exts;
    }

    public Vector downSync(Vector v) throws SQLException {
        Vector retVector = new Vector();
        try {
            con = allocateConnection(tableName);
            PreparedStatement update = con.prepareStatement("update cont_Contact set owner=?,firstname=?," + "lastname=?,nickname=?,title=?,organization=?,orgunit=?," + "emailaddr=?,homeph=?,workph=?,cellph=?,im=?,imno=?," + "fax=?,homeaddr=?,homelocality=?,homeregion=?," + "homepcode=?,homecountry=?,workaddr=?,worklocality=?," + "workregion=?,workpcode=?,workcountry=?,website=?," + "wapsite=?,comments=?,birthday=?,syncstatus=?,dirtybits=? " + "where OId=? and syncstatus=?");
            PreparedStatement insert = con.prepareStatement("insert into cont_Contact (owner,firstname,lastname," + "nickname,title,organization,orgunit,emailaddr,homeph," + "workph,cellph,im,imno,fax,homeaddr,homelocality," + "homeregion,homepcode,homecountry,workaddr,worklocality," + "workregion,workpcode,workcountry,website,wapsite," + "comments,birthday,syncstatus,dirtybits,quicklist) " + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," + "?,?,?,?,?,?,?,?)");
            PreparedStatement insert1 = con.prepareStatement(DBUtil.getQueryCurrentOID(con, "cont_Contact", "newoid"));
            PreparedStatement delete1 = con.prepareStatement("delete from  cont_Contact_Group_Rel where externalcontact=?");
            PreparedStatement delete2 = con.prepareStatement("delete from  cont_Contact where OId=? " + "and (syncstatus=? or syncstatus=?)");
            for (int i = 0; i < v.size(); i++) {
                try {
                    DO = (ExternalContactDO) v.elementAt(i);
                    if (DO.getSyncstatus() == INSERT) {
                        insert.setBigDecimal(1, DO.getOwner());
                        insert.setString(2, DO.getFirstname());
                        insert.setString(3, DO.getLastname());
                        insert.setString(4, DO.getNickname());
                        insert.setString(5, DO.getTitle());
                        insert.setString(6, DO.getOrganization());
                        insert.setString(7, DO.getOrgunit());
                        insert.setString(8, DO.getEmail());
                        insert.setString(9, DO.getHomeph());
                        insert.setString(10, DO.getWorkph());
                        insert.setString(11, DO.getCellph());
                        insert.setString(12, DO.getIm());
                        insert.setString(13, DO.getImno());
                        insert.setString(14, DO.getFax());
                        insert.setString(15, DO.getHomeaddr());
                        insert.setString(16, DO.getHomelocality());
                        insert.setString(17, DO.getHomeregion());
                        insert.setString(18, DO.getHomepcode());
                        insert.setString(19, DO.getHomecountry());
                        insert.setString(20, DO.getWorkaddr());
                        insert.setString(21, DO.getWorklocality());
                        insert.setString(22, DO.getWorkregion());
                        insert.setString(23, DO.getWorkpcode());
                        insert.setString(24, DO.getWorkcountry());
                        insert.setString(25, DO.getWebsite());
                        insert.setString(26, DO.getWapsite());
                        insert.setString(27, DO.getComments());
                        if (DO.getBirthday() != null) insert.setDate(28, DO.getBirthday()); else insert.setNull(28, Types.DATE);
                        insert.setInt(29, RESET);
                        insert.setInt(30, RESET);
                        insert.setInt(31, 0);
                        con.executeUpdate(insert, null);
                        con.reset();
                        rs = con.executeQuery(insert1, null);
                        if (rs.next()) DO.setOId(rs.getBigDecimal("newoid"));
                        con.reset();
                        retVector.add(DO);
                    } else if (DO.getSyncstatus() == UPDATE) {
                        update.setBigDecimal(1, DO.getOwner());
                        update.setString(2, DO.getFirstname());
                        update.setString(3, DO.getLastname());
                        update.setString(4, DO.getNickname());
                        update.setString(5, DO.getTitle());
                        update.setString(6, DO.getOrganization());
                        update.setString(7, DO.getOrgunit());
                        update.setString(8, DO.getEmail());
                        update.setString(9, DO.getHomeph());
                        update.setString(10, DO.getWorkph());
                        update.setString(11, DO.getCellph());
                        update.setString(12, DO.getIm());
                        update.setString(13, DO.getImno());
                        update.setString(14, DO.getFax());
                        update.setString(15, DO.getHomeaddr());
                        update.setString(16, DO.getHomelocality());
                        update.setString(17, DO.getHomeregion());
                        update.setString(18, DO.getHomepcode());
                        update.setString(19, DO.getHomecountry());
                        update.setString(20, DO.getWorkaddr());
                        update.setString(21, DO.getWorklocality());
                        update.setString(22, DO.getWorkregion());
                        update.setString(23, DO.getWorkpcode());
                        update.setString(24, DO.getWorkcountry());
                        update.setString(25, DO.getWebsite());
                        update.setString(26, DO.getWapsite());
                        update.setString(27, DO.getComments());
                        if (DO.getBirthday() != null) update.setDate(28, DO.getBirthday()); else update.setNull(28, Types.DATE);
                        update.setInt(29, RESET);
                        update.setInt(30, RESET);
                        update.setBigDecimal(31, DO.getOId());
                        update.setInt(32, RESET);
                        if (con.executeUpdate(update, null) < 1) retVector.add(DO);
                        con.reset();
                    } else if (DO.getSyncstatus() == DELETE) {
                        try {
                            con.setAutoCommit(false);
                            delete1.setBigDecimal(1, DO.getOId());
                            con.executeUpdate(delete1, null);
                            delete2.setBigDecimal(1, DO.getOId());
                            delete2.setInt(2, RESET);
                            delete2.setInt(3, DELETE);
                            if (con.executeUpdate(delete2, null) < 1) {
                                con.rollback();
                                retVector.add(DO);
                            } else {
                                con.commit();
                            }
                        } catch (Exception e) {
                            con.rollback();
                            retVector.add(DO);
                            throw e;
                        } finally {
                            con.reset();
                        }
                    }
                } catch (Exception e) {
                    if (DO != null) logError("Sync-ExternalContactDO.owner = " + DO.getOwner().toString() + " oid = " + (DO.getOId() != null ? DO.getOId().toString() : "NULL"), e);
                }
            }
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
        return retVector;
    }

    public void deleteExternalContact(BigDecimal oid, BigDecimal owner) throws SQLException {
        try {
            con = allocateConnection(tableName);
            ps = con.prepareStatement("delete from  cont_Contact_Group_Rel where externalcontact=?");
            ps.setBigDecimal(1, oid);
            con.executeUpdate(ps, null);
            con.reset();
            ps = con.prepareStatement("delete from cont_Contact where syncstatus=? and OId=? and owner=?");
            ps.setInt(1, INSERT);
            ps.setBigDecimal(2, oid);
            ps.setBigDecimal(3, owner);
            if (con.executeUpdate(ps, null) < 1) {
                con.reset();
                ps = con.prepareStatement("update cont_Contact set syncstatus=? where OId=? and owner=?");
                ps.setInt(1, DELETE);
                ps.setBigDecimal(2, oid);
                ps.setBigDecimal(3, owner);
                con.executeUpdate(ps, null);
            }
        } catch (SQLException e) {
            if (DEBUG) logError("", e);
            throw e;
        } finally {
            release();
        }
    }

    public class NoSuchContactException extends java.sql.SQLException {

        public NoSuchContactException(String msg) {
            super(msg);
        }
    }
}
