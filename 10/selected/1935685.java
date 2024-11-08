package ces.platform.bbs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Logger;
import org.cnjsp.util.SecurityUtil;
import ces.platform.bbs.exception.UserNotFoundException;
import ces.platform.bbs.mysql.DbForumFactory;
import ces.platform.bbs.mysql.DbSequenceManager;
import ces.platform.infoplat.utils.Function;

/**
 * @author sam
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BBSInterface {

    private static final String INSERT_USER = "INSERT INTO " + DbForumFactory.DB_PREFIX_MODULE + "USER(V_USERNAME, V_PASSWORD, C_ADDTIME, V_QUESTION, V_ANSWER, V_EMAIL, V_REALNAME, I_USERID) VALUES(?,?,?,?,?,?,?,?)";

    private static final String INSTER_USERGROUP = "INSERT INTO " + DbForumFactory.DB_PREFIX_MODULE + "GROUPUSER( I_GROUPID, I_USERID, I_ADMIN ) VALUES(?,?,?)";

    private static final String INSERT_USERPROPS = "INSERT INTO " + DbForumFactory.DB_PREFIX_MODULE + "USERPROPS" + " (V_SIGN, V_SEX, V_HOMEPAGE, I_LOGINS, V_FACE" + ", I_WIDTH, I_HEIGHT, V_OICQ, V_LASTACCESS, V_LASTLOGIN" + ", I_USERLEVEL, I_USERWEALTH, I_USEREP, I_USERCP, V_TITLE" + ", V_ICQ, V_MSN, V_PHONE, V_COMPANY, V_COMPANYSITE" + ", V_BRIEF, V_BIRTHDAY, V_SKINPATH, I_STATUS, I_ONLINETIME" + ", I_USERID)" + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static Logger log = Logger.getLogger(BBSInterface.class.getName());

    public BBSInterface() {
    }

    public void addUser(String strUserName, String strPass) {
        String datetime = Function.getSysTime().toString();
        String time = datetime.substring(0, 4) + datetime.substring(5, 7) + datetime.substring(8, 10) + datetime.substring(11, 13) + datetime.substring(14, 16) + datetime.substring(17, 19) + datetime.substring(20, 22) + "0";
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbForumFactory.getConnection();
            con.setAutoCommit(false);
            int userID = DbSequenceManager.nextID(DbSequenceManager.USER);
            pstmt = con.prepareStatement(INSERT_USER);
            pstmt.setString(1, strUserName);
            pstmt.setString(2, SecurityUtil.md5ByHex(strPass));
            pstmt.setString(3, time);
            pstmt.setString(4, "");
            pstmt.setString(5, "");
            pstmt.setString(6, "");
            pstmt.setString(7, "");
            pstmt.setInt(8, userID);
            pstmt.executeUpdate();
            pstmt.clearParameters();
            pstmt = con.prepareStatement(INSERT_USERPROPS);
            pstmt.setString(1, "");
            pstmt.setString(2, "");
            pstmt.setString(3, "");
            pstmt.setInt(4, 0);
            pstmt.setString(5, "");
            pstmt.setInt(6, 0);
            pstmt.setInt(7, 0);
            pstmt.setString(8, "");
            pstmt.setString(9, "");
            pstmt.setString(10, "");
            pstmt.setInt(11, 0);
            pstmt.setInt(12, 0);
            pstmt.setInt(13, 0);
            pstmt.setInt(14, 0);
            pstmt.setString(15, "");
            pstmt.setString(16, "");
            pstmt.setString(17, "");
            pstmt.setString(18, "");
            pstmt.setString(19, "");
            pstmt.setString(20, "");
            pstmt.setString(21, "");
            pstmt.setString(22, "");
            pstmt.setString(23, "");
            pstmt.setInt(24, 0);
            pstmt.setInt(25, 0);
            pstmt.setInt(26, userID);
            pstmt.executeUpdate();
            pstmt.clearParameters();
            pstmt = con.prepareStatement(INSTER_USERGROUP);
            pstmt.setInt(1, 4);
            pstmt.setInt(2, userID);
            pstmt.setInt(3, 0);
            pstmt.executeUpdate();
            con.commit();
        } catch (Exception e) {
            try {
                con.rollback();
            } catch (SQLException e1) {
            }
            log.error("insert user Error: " + e.toString());
        } finally {
            DbForumFactory.closeDB(null, pstmt, null, con);
        }
    }

    public void setBbsSession(HttpServletRequest request, String loginName, String userPass) {
        AuthorizationFactory authorizationFactory = AuthorizationFactory.getInstance();
        Authorization authorization = null;
        try {
            authorization = authorizationFactory.getAuthorization(loginName, userPass);
            HttpSession session = request.getSession();
            if (session != null) {
                authorization.setIP(request.getRemoteAddr() == null ? "127.0.0.1" : request.getRemoteAddr());
                authorization.setSessionID(session.getId());
                session.setAttribute("authorization", authorization);
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void modify(String strName, String strNewPass) {
        String str = "update jb_user set V_PASSWORD =? where V_USERNAME =?";
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbForumFactory.getConnection();
            con.setAutoCommit(false);
            pstmt = con.prepareStatement(str);
            pstmt.setString(1, SecurityUtil.md5ByHex(strNewPass));
            pstmt.setString(2, strName);
            pstmt.executeUpdate();
            con.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
            }
        } finally {
            try {
                DbForumFactory.closeDB(null, pstmt, null, con);
            } catch (Exception e) {
            }
        }
    }

    public void delUser(String userName) {
        if (userName == null) return;
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            String srtSql = "select i_userid from jb_user where v_username = ?";
            con = DbForumFactory.getConnection();
            con.setAutoCommit(false);
            pstmt = con.prepareStatement(srtSql);
            pstmt.setString(1, userName);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new UserNotFoundException(ForumException.ERROR_EXAMPLE);
            }
            int userId = rs.getInt("I_USERID");
            String strDelUserProps = "delete from JB_USERPROPS where i_userid = ?";
            pstmt = con.prepareStatement(strDelUserProps);
            pstmt.setInt(1, userId);
            pstmt.execute();
            String strDelUser = "delete from JB_USER where i_userid = ?";
            pstmt = con.prepareStatement(strDelUser);
            pstmt.setInt(1, userId);
            pstmt.execute();
            con.commit();
        } catch (Exception e) {
            try {
                con.rollback();
            } catch (SQLException e1) {
            }
            log.error("delete user Error: " + e.toString());
        } finally {
            DbForumFactory.closeDB(rs, pstmt, null, con);
        }
    }

    public static void main(String[] args) {
        BBSInterface bbs = new BBSInterface();
        try {
            bbs.modify("��Сͦ", "670B14728AD9902AECBA32E22FA4F6BD");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addUser(String strUserName, String strPass, boolean isEncrypt) throws Exception {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = DbForumFactory.getConnection();
            con.setAutoCommit(false);
            int userID = DbSequenceManager.nextID(DbSequenceManager.USER);
            pstmt = con.prepareStatement(INSERT_USER);
            pstmt.setString(1, strUserName);
            if (isEncrypt) {
                pstmt.setString(2, SecurityUtil.md5ByHex(strPass));
            } else {
                pstmt.setString(2, strPass);
            }
            pstmt.setString(3, "");
            pstmt.setString(4, "");
            pstmt.setString(5, "");
            pstmt.setString(6, "");
            pstmt.setString(7, "");
            pstmt.setInt(8, userID);
            pstmt.executeUpdate();
            pstmt.clearParameters();
            pstmt = con.prepareStatement(INSERT_USERPROPS);
            pstmt.setString(1, "");
            pstmt.setString(2, "");
            pstmt.setString(3, "");
            pstmt.setInt(4, 0);
            pstmt.setString(5, "");
            pstmt.setInt(6, 0);
            pstmt.setInt(7, 0);
            pstmt.setString(8, "");
            pstmt.setString(9, "");
            pstmt.setString(10, "");
            pstmt.setInt(11, 0);
            pstmt.setInt(12, 0);
            pstmt.setInt(13, 0);
            pstmt.setInt(14, 0);
            pstmt.setString(15, "");
            pstmt.setString(16, "");
            pstmt.setString(17, "");
            pstmt.setString(18, "");
            pstmt.setString(19, "");
            pstmt.setString(20, "");
            pstmt.setString(21, "");
            pstmt.setString(22, "");
            pstmt.setString(23, "");
            pstmt.setInt(24, 0);
            pstmt.setInt(25, 0);
            pstmt.setInt(26, userID);
            pstmt.executeUpdate();
            pstmt.clearParameters();
            pstmt = con.prepareStatement(INSTER_USERGROUP);
            pstmt.setInt(1, 4);
            pstmt.setInt(2, userID);
            pstmt.setInt(3, 0);
            pstmt.executeUpdate();
            con.commit();
        } catch (Exception e) {
            try {
                con.rollback();
            } catch (SQLException e1) {
            }
            log.error("insert user Error: " + e.toString());
        } finally {
            DbForumFactory.closeDB(null, pstmt, null, con);
        }
    }
}
