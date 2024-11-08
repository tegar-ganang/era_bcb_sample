package ces.platform.system.dbaccess;

import ces.platform.system.common.*;
import ces.coral.dbo.*;
import java.sql.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import ces.coral.log.*;

/**
 * <p>����:
 * <font class=titlefont>
 * ���û��Ự����
 * </font>
 * <p>����:
 * <font class=descriptionfont>
 * <br>����һ���û��Ự��¼
 * </font>
 * <p>�汾��:
 * <font class=versionfont>
 * Copyright (c) 2.50.2003.0925
 * </font>
 * <p>��˾:
 * <font class=companyfont>
 * �Ϻ�������Ϣ��չ���޹�˾
 * </font>
 * @author ����
 * @version 2.50.2003.0925
 */
public class UserSession extends OperationBase implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    static Logger logger = new Logger(UserSession.class);

    /**
     * ��̬����:��¼��־----�ɹ�
     **/
    public static final String LOGIN_SUCCESS = "1";

    /**
     * ��̬����:��¼��־----���ɹ��������û����
     **/
    public static final String LOGIN_FAIL_CONN = "2";

    /**
     * ��̬����:��¼��־----���ɹ���������δ���
     **/
    public static final String LOGIN_FAIL_PASSWORD = "3";

    /**
     * ��̬����:�˳���־----δ�˳�
     **/
    public static final String LOGOUT_NO = "0";

    /**
     * ��̬����:�˳���־----���˳�
     **/
    public static final String LOGOUT_NORMAL = "1";

    /**
     * ��̬����:�˳���־----ע���˳�
     **/
    public static final String LOGOUT_CANCEL = "2";

    /**
     * ��̬����:���Ự��������session��
     **/
    public static final String STORE_SESSION = "1";

    /**
     * ��̬����:���Ự��������property��
     **/
    public static final String STORE_PROPERTY = "2";

    /**
     * ��Ա�������Ự���
     **/
    protected String sessionID;

    /**
     * ��Ա�������Ự��ʼʱ�䣬��Ӧ��t_sys_user_session.begin_date
     **/
    protected java.sql.Timestamp beginDate;

    /**
     * ��Ա������IP��ַ����Ӧ��t_sys_user_session.ip_address
     **/
    protected String ipAddress;

    /**
     * ��Ա�������û�����
     **/
    protected User user;

    /**
     * ��Ա���������ַ
     **/
    protected String macNO;

    /**
     * ��Ա�������û���¼ID
     */
    protected String loginID;

    /**
     * ȱʡ���캯��
     *
     **/
    public UserSession() {
    }

    /**
     * ���캯��1
     *
     * @param sessionID	        �Ự���
     **/
    public UserSession(String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * ���캯��2
     *
     * @param sessionID	        �Ự���
     * @param beginDate	        �Ự��ʼʱ��
     * @param ipAddress	        IP��ַ
     * @param macNO	            ���ַ
     * @param loginID           �û���¼ID
     * @param user	            �û�����
     **/
    public UserSession(String sessionID, java.sql.Timestamp beginDate, String ipAddress, String macNO, String loginID, User user) {
        this.sessionID = sessionID;
        this.beginDate = beginDate;
        this.ipAddress = ipAddress;
        this.macNO = macNO;
        this.loginID = loginID;
        this.user = user;
    }

    /**
     * ���ûỰ���
     *
     * @param sessionID	        �Ự���
     **/
    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * ���ûỰ��ʼʱ��
     *
     * @param beginDate	�Ự��ʼʱ��
     **/
    public void setBeginDate(java.sql.Timestamp beginDate) {
        this.beginDate = beginDate;
    }

    /**
     * ����IP��ַ
     *
     * @param ipAddress	IP��ַ
     **/
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * �����û�
     *
     * @param user	    �û�����
     **/
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * �������ַ
     *
     * @param macNO	    ���ַ
     **/
    public void setMacNO(String macNO) {
        this.macNO = macNO;
    }

    /**
     * ��ȡ�Ự���
     *
     * @return	�Ự���
     **/
    public String getSessionID() {
        return this.sessionID;
    }

    /**
     * ��ȡ�Ự��ʼʱ��
     *
     * @return	�Ự��ʼʱ��
     **/
    public java.sql.Timestamp getBeginDate() {
        return this.beginDate;
    }

    /**
     * ��ȡIP��ַ
     *
     * @return	IP��ַ
     **/
    public String getIpAddress() {
        return this.ipAddress;
    }

    /**
     * ��ȡ�û�
     *
     * @return	�û�����
     **/
    public User getUser() {
        return this.user;
    }

    /**
     * ��ȡ���ַ
     *
     * @return	���ַ
     **/
    public String getMacNO() {
        return this.macNO;
    }

    /**
     * ��ȡ�û���¼ID
     * @return  ��¼ID
     */
    public String getLoginID() {
        return loginID;
    }

    /**
     * �����û���¼ID
     * @param loginID
     */
    public void setLoginID(String loginID) {
        this.loginID = loginID;
    }

    /**
     * ��֤���û��Ự��������ݿ����Ƿ����
     * @return                  true:  �ö�������ݿ��д���
     *                          false: �ö�������ݿ��в�����
     * @throws Exception
     *                          �����֤�����⣬���׳��쳣
     */
    public boolean isExist() throws Exception {
        boolean returnValue = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT session_id FROM " + Common.USER_SESSION_TABLE + " WHERE session_id = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.sessionID);
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("User_session.isExist(): SQLException: \n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return returnValue;
    }

    /**
     * ��֤���û��Ự��������ݿ����Ƿ����
     * @return                  �ỰID:  �ö�������ݿ��д���
     *                          null: �ö�������ݿ��в�����
     * @throws Exception
     *                          �����֤�����⣬���׳��쳣
     */
    public String isOverLapExist() throws Exception {
        String returnValue = null;
        ResultSet result = null;
        String strQuery = "SELECT session_id FROM " + Common.USER_SESSION_TABLE + " WHERE user_id = " + this.user.getUserID() + " and ip_address = '" + this.ipAddress + "' and login_id is null";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            result = dbo.select(strQuery);
            if (result.next()) {
                returnValue = result.getString(1);
            }
        } catch (SQLException se) {
            throw new CesSystemException("User_session.isExist(): SQLException: \n\t" + se);
        } finally {
            close(dbo, null, result);
        }
        return returnValue;
    }

    /**
     * ��֤���û��Ự��������ݿ����Ƿ����
     *
     * @return                  true:  �ö�������ݿ��д���
     *                          false: �ö�������ݿ��в�����
     * @throws Exception
     *                          �����֤�����⣬���׳��쳣
     */
    public boolean isExist(Connection con) throws Exception {
        boolean returnValue = false;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT session_id FROM " + Common.USER_SESSION_TABLE + " WHERE session_id = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.sessionID);
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("User_session.isExist(): SQLException: \n\t" + se);
        } finally {
            closeResultSet(result);
            closePreparedStatement(ps);
        }
        return returnValue;
    }

    /**
     * ����ݿ�������װ����û��Ự������Ϣ
     *
     * @return                  true:  װ��ɹ�
     *                          false: װ�벻�ɹ�
     * @throws Exception
     *                          ���װ�������⣬���׳��쳣
     */
    public boolean load() throws Exception {
        boolean returnValue = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT ut.user_id,ut.user_name,ut.login_name,ut.flag_emp," + "ut.user_cryptogram,ut.flag_lock,ut.flag_define,ut.ic_no,ut.conn_num," + "ut.flag_check,ut.flag_active,ut.flag_sa,ut.show_order,ut.position_x,ut.position_y,ut.type," + "ust.session_id,ust.begin_date,ust.ip_address,ust.mac_no,ust.login_id " + "FROM " + Common.USER_TABLE + " ut, " + Common.USER_SESSION_TABLE + " ust " + "WHERE ut.user_id = ust.user_id " + "AND ust.session_id = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.sessionID);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            if (!result.next()) {
                returnValue = false;
            } else {
                i = 1;
                va.setStart(i);
                User uTemp = User.generateUser(result, va);
                this.user = uTemp;
                this.sessionID = result.getString(va.next());
                this.beginDate = result.getTimestamp(va.next());
                this.ipAddress = result.getString(va.next());
                this.macNO = result.getString(va.next());
                this.loginID = result.getString(va.next());
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("UserSession.load(): SQLException: \n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return returnValue;
    }

    protected void doAddNew(Connection con) throws Exception {
    }

    protected void doSelfDelete(Connection con) throws Exception {
    }

    /**
     * �½����û��Ự����
     * @param con ���Ӷ���
     * @throws Exception
     *                          ����½������⣬���׳��쳣
     */
    protected void doNew(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("User_session.doNew(): Illegal data values for insert");
        }
        PreparedStatement ps = null;
        String strQuery = "INSERT INTO " + Common.USER_SESSION_TABLE + "(session_id,user_id,begin_date,ip_address,mac_no,login_id)" + "VALUES (?,?,?,?,?,?)";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.sessionID);
            ps.setInt(2, this.user.getUserID());
            ps.setTimestamp(3, this.beginDate);
            ps.setString(4, this.ipAddress);
            ps.setString(5, this.macNO);
            ps.setString(6, this.loginID);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("User_session.doNew(): ERROR Inserting data " + "in T_SYS_USER_SESSION INSERT !! resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("User_session.doNew(): SQLException while inserting new user_session; " + "session_id = " + this.sessionID + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     * ���¸��û��Ự����
     * @param con ���Ӷ���
     * @throws Exception
     *                          �����������⣬���׳��쳣
     */
    protected void doUpdate(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("User_session.doUpdate(): Illegal data values for update");
        }
        PreparedStatement ps = null;
        String strQuery = "UPDATE " + Common.USER_SESSION_TABLE + " SET " + "user_id = ?, begin_date = ? , " + "ip_address = ?, mac_no = ?, login_id= ? " + "WHERE session_id = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.user.getUserID());
            ps.setTimestamp(2, this.beginDate);
            ps.setString(3, this.ipAddress);
            ps.setString(4, this.macNO);
            ps.setString(5, this.loginID);
            ps.setString(6, this.sessionID);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("User_session.doUpdate(): ERROR updating data in T_SYS_USER_SESSION!! " + "resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("User_session.doUpdate(): SQLException while updating user_session; " + "session_id = " + this.sessionID + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     * ���IP���¸��û��Ự����
     *
     * @throws Exception
     *                          �����������⣬���׳��쳣
     */
    public void doUpdateByIP() throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("User_session.doUpdateByIP(): Illegal data values for update");
        }
        Connection con = null;
        PreparedStatement ps = null;
        String strQuery = "UPDATE " + Common.USER_SESSION_TABLE + " SET " + "session_id = ?, user_id = ?, begin_date = ? , " + " mac_no = ?, login_id= ? " + "WHERE ip_address = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            con.setAutoCommit(false);
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.sessionID);
            ps.setInt(2, this.user.getUserID());
            ps.setTimestamp(3, this.beginDate);
            ps.setString(4, this.macNO);
            ps.setString(5, this.loginID);
            ps.setString(6, this.ipAddress);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                con.rollback();
                throw new CesSystemException("User_session.doUpdateByIP(): ERROR updating data in T_SYS_USER_SESSION!! " + "resultCount = " + resultCount);
            }
            con.commit();
        } catch (SQLException se) {
            if (con != null) {
                con.rollback();
            }
            throw new CesSystemException("User_session.doUpdateByIP(): SQLException while updating user_session; " + "session_id = " + this.sessionID + " :\n\t" + se);
        } finally {
            con.setAutoCommit(true);
            closePreparedStatement(ps);
            closeConnection(dbo);
        }
    }

    /**
     * ��ݵ�¼ID���¸��û��Ự����
     *
     * @throws Exception
     *                          �����������⣬���׳��쳣
     */
    public void doUpdateByLoginID() throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("User_session.doUpdateByLoginID(): Illegal data values for update");
        }
        Connection con = null;
        PreparedStatement ps = null;
        String strQuery = "UPDATE " + Common.USER_SESSION_TABLE + " SET " + "session_id = ?, user_id = ?, begin_date = ? , " + "ip_address = ?, mac_no = ? " + "WHERE  login_id= ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            con.setAutoCommit(false);
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.sessionID);
            ps.setInt(2, this.user.getUserID());
            ps.setTimestamp(3, this.beginDate);
            ps.setString(4, this.ipAddress);
            ps.setString(5, this.macNO);
            ps.setString(6, this.loginID);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                con.rollback();
                throw new CesSystemException("User_session.doUpdateByLoginID(): ERROR updating data in T_SYS_USER_SESSION!! " + "resultCount = " + resultCount);
            }
            con.commit();
        } catch (SQLException se) {
            if (con != null) {
                con.rollback();
            }
            throw new CesSystemException("User_session.doUpdateByLoginID(): SQLException while updating user_session; " + "session_id = " + this.sessionID + " :\n\t" + se);
        } finally {
            con.setAutoCommit(true);
            closePreparedStatement(ps);
            closeConnection(dbo);
        }
    }

    /**
     * ����ݿ���ɾ����û��Ự���󣬲��ύ��
     * @param con ���Ӷ���
     * @throws Exception
     *                          ���ɾ�������⣬���׳��쳣
     */
    public void doDelete(Connection con) throws Exception {
        PreparedStatement ps = null;
        String strQuery = "DELETE FROM " + Common.USER_SESSION_TABLE + " WHERE session_id = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.sessionID);
            ps.executeUpdate();
        } catch (Exception se) {
            throw new CesSystemException("User_session.doDelete(): Exception while deleting user_session; " + "session_id = " + this.sessionID + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     * ������ݿ��е������û��Ự����
     *
     * @return                  �û��Ự���󼯺�
     * @throws Exception
     *                          �����������⣬���׳��쳣
     */
    public Vector getAllUserSessions() throws Exception {
        Vector vAllUserSessions = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT ut.user_id,ut.user_name,ut.login_name,ut.flag_emp," + "ut.user_cryptogram,ut.flag_lock,ut.flag_define,ut.ic_no,ut.conn_num," + "ut.flag_check,ut.flag_active,ut.flag_sa,ut.show_order,ut.position_x,ut.position_y,ut.type," + "ust.session_id,ust.begin_date,ust.ip_address,ust.mac_no,ust.login_id " + "FROM " + Common.USER_TABLE + " ut, " + Common.USER_SESSION_TABLE + " ust " + "WHERE ut.user_id = ust.user_id";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            while (result.next()) {
                i = 1;
                va.setStart(i);
                User uTemp = User.generateUser(result, va);
                UserSession usTemp = UserSession.generateUserSession(result, va, uTemp);
                vAllUserSessions.addElement(usTemp);
            }
        } catch (SQLException se) {
            throw new CesSystemException("User_session.getAllUserSessions(): SQLException: \n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return vAllUserSessions;
    }

    /**
     * ��ɲ�ѯ�Ự����
     * @param result   ��ѯ���
     * @param v        ������
     * @return         ������ɵĶ���
     */
    public static UserSession generateUserSession(ResultSet result, ValueAsc v, User user) {
        UserSession usTemp = new UserSession();
        try {
            usTemp.setSessionID(result.getString(v.next()));
            usTemp.setBeginDate(result.getTimestamp(v.next()));
            usTemp.setIpAddress(result.getString(v.next()));
            usTemp.setMacNO(result.getString(v.next()));
            usTemp.setLoginID(result.getString(v.next()));
            usTemp.setUser(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return usTemp;
    }

    /**
     * ��֤�����û��Ự���е����
     *
     * @return                   true:   ��֤�ɹ�
     *                           false:  ��֤ʧ��
     */
    protected boolean isValidate() {
        if ((this.sessionID == null) || (this.user == null) || (this.user.getUserID() == 0)) {
            return (false);
        } else {
            return (true);
        }
    }

    /**
     * �洢�Ự����������session�����Զ�������Լ���
     * @param type 1:����session�У�2�������Զ�������Լ�����
     */
    public static SessionProperty setAttributeBatch(String type, HttpSession session, User user) throws CesSystemException {
        SessionProperty sp = null;
        if (type.equals(UserSession.STORE_SESSION)) {
            sp = new SessionProperty(session);
        } else {
            sp = new SessionProperty();
        }
        Vector authorities = null;
        try {
            authorities = AuthContext.getInstance().getUserAuthAssign(user.getUserID());
            Hashtable extrAuths = AuthContext.getInstance().getUserExtrAuthAssign(user.getUserID());
            if (authorities == null) {
                authorities = user.getAuthoritiesFromContext();
                extrAuths = new Hashtable();
                if (authorities != null && authorities.size() > 0) {
                    Authority au = null;
                    for (int i = 0; i < authorities.size(); ) {
                        au = (Authority) authorities.get(i);
                        if (au.getAuthorityType().trim().equals(Authority.TYPE_EXTERNAL)) {
                            extrAuths.put(au.getAuthorityID() + "", au);
                            authorities.remove(i);
                        } else {
                            i++;
                        }
                    }
                }
                AuthContext.getInstance().setUserAuthAssign(user.getUserID(), authorities);
                AuthContext.getInstance().setUserExtrAuthAssign(user.getUserID(), extrAuths);
            }
            sp.setAttribute("extrauthority", extrAuths);
            sp.setAttribute("authority", authorities);
            sp.setAttribute("user", user);
            sp.setAttribute("sa", user.getFlagSA());
            Organize org = user.getOrgOfUser();
            Vector rs = new Vector();
            if (org != null) {
                rs = Organize.getAllParents(org);
                rs.add(0, org);
            }
            sp.setAttribute("organizes", rs);
        } catch (CesSystemException e) {
            throw e;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sp;
    }

    /**
     * ����������ʱ��ջỰ��
     */
    public static void clearSession() throws Exception {
        try {
            Vector vAllSession = new UserSession().getAllUserSessions();
            int nNum = vAllSession.size();
            UserSession[] delTemp = new UserSession[nNum];
            UserSessionHistory[] addTemp = new UserSessionHistory[nNum];
            for (int i = 0; i < nNum; i++) {
                UserSession us = (UserSession) vAllSession.get(i);
                String strSessionID = us.getSessionID();
                Timestamp tBegin = us.getBeginDate();
                String strIP = us.getIpAddress();
                String strMacNO = us.getMacNO();
                User user = us.getUser();
                User admin = new User(1);
                admin.load();
                delTemp[i] = us;
                UserSessionHistory ush = new UserSessionHistory(strSessionID);
                ush.setBeginDate(tBegin);
                ush.setEndDate(Common.getSysDate());
                ush.setIpAddress(strIP);
                ush.setMacNO(strMacNO);
                ush.setUser(user);
                ush.setLoginFlag(UserSession.LOGIN_SUCCESS);
                ush.setLogoutFlag(UserSession.LOGOUT_NO);
                ush.setCancelPerson(admin);
                addTemp[i] = ush;
            }
            new UserSession().doDeleteBatch(delTemp);
            new UserSessionHistory().doAddBatch(addTemp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CesSystemException("UserSession.clearSession(): SQLException: \n\t" + e);
        }
    }
}
