package ces.platform.system.dbaccess;

import ces.platform.system.common.*;
import ces.coral.dbo.*;
import java.sql.*;
import java.io.*;
import java.util.*;
import ces.coral.log.*;

/**
 * <p>����:
 * <font class=titlefont>
 * ���û�Ȩ���ڽ�ɫ����
 * </font>
 * <p>����:
 * <font class=descriptionfont>
 * <br>����һ���û�Ȩ���ڽ�ɫ��¼
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
public class RoleAuthority extends OperationBase implements Serializable {

    static Logger logger = new Logger(RoleAuthority.class);

    /**
	 * ��Ա��������ɫ��role.role_id��Ӧ��t_sys_role_authority.role_id
	 **/
    protected Role role;

    /**
	 * ��Ա������Ȩ�ޣ�authority.authority_id��Ӧ��t_sys_role_authority.authority_id
	 **/
    protected Authority authority;

    /**
	 * ��Ա������Ȩ���ڽ�ɫ�û���provider.user_id��Ӧ��t_sys_role_authority.provider
	 **/
    protected User provider;

    /**
	 * ��Ա������Ȩ���ڽ�ɫ�û���ݣ�providerFigure.figure_id��Ӧ��t_sys_role_authority.provider_type
	 **/
    protected Figure providerFigure;

    /**
	 * ȱʡ���캯��
	 **/
    public RoleAuthority() {
    }

    /**
	 * ���캯��1
         *
	 * @param	role	                ��ɫ
	 * @param	authority	        Ȩ��
	 * @param	provider	        Ȩ���ڽ�ɫ�û�
	 * @param	providerFigure	Ȩ���ڽ�ɫ�û����
	 **/
    public RoleAuthority(Role role, Authority authority, User provider, Figure providerFigure) {
        this.role = role;
        this.authority = authority;
        this.provider = provider;
        this.providerFigure = providerFigure;
    }

    /**
	 * ���캯��2
         *
	 * @param	role	                ��ɫ
	 * @param	authority	        Ȩ��
	 **/
    public RoleAuthority(Role role, Authority authority) {
        this.role = role;
        this.authority = authority;
    }

    /**
	 * ���ý�ɫ
         *
	 * @param	role	��ɫ
	 **/
    public void setRole(Role role) {
        this.role = role;
    }

    /**
	 * ����Ȩ��
         *
	 * @param	authority	Ȩ��
	 **/
    public void setAuthority(Authority authority) {
        this.authority = authority;
    }

    /**
	 * ����Ȩ���ڽ�ɫ�û�
         *
	 * @param	provider	Ȩ���ڽ�ɫ�û�
	 **/
    public void setProvider(User provider) {
        this.provider = provider;
    }

    /**
	 * ����Ȩ���ڽ�ɫ�û����
         *
	 * @param	providerFigure	Ȩ���ڽ�ɫ�û����
	 **/
    public void setProviderFigure(Figure providerFigure) {
        this.providerFigure = providerFigure;
    }

    /**
	 * ��ȡ��ɫ
         *
	 * @return	��ɫ
	 **/
    public Role getRole() {
        return this.role;
    }

    /**
	 * ��ȡȨ��
         *
	 * @return	Ȩ��
	 **/
    public Authority getAuthority() {
        return this.authority;
    }

    /**
	 * ��ȡȨ���ڽ�ɫ�û�
         *
	 * @return	Ȩ���ڽ�ɫ�û�
	 **/
    public User getProvider() {
        return this.provider;
    }

    /**
	 * ��ȡȨ���ڽ�ɫ�û����
         *
	 * @return	Ȩ���ڽ�ɫ�û����
	 **/
    public Figure getProviderFigure() {
        return this.providerFigure;
    }

    /**
	 * ��֤�ý�ɫȨ�޹�ϵ��������ݿ����Ƿ����
     *
     * @return                  true:  �ö�������ݿ��д���
     *                          false: �ö�������ݿ��в�����
     * @throws Exception
     *                          �����֤�����⣬���׳��쳣
	 */
    protected boolean isExist() throws Exception {
        boolean returnValue = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT role_id FROM " + Common.ROLE_AUTHORITY_TABLE + " WHERE role_id = ? " + "AND authority_id = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.role.getRoleID());
            ps.setInt(2, this.authority.getAuthorityID());
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                int nTemp = result.getInt(1);
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("RoleAuthority.isExist(): SQLException: \n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return returnValue;
    }

    /**
     * ��֤�ý�ɫȨ�޹�ϵ��������ݿ����Ƿ����,���ύ
     *
     * @return                  true:  �ö�������ݿ��д���
     *                          false: �ö�������ݿ��в�����
     * @throws Exception
     *                          �����֤�����⣬���׳��쳣
     */
    protected boolean isExist(Connection con) throws Exception {
        boolean returnValue = false;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT role_id FROM " + Common.ROLE_AUTHORITY_TABLE + " WHERE role_id = ? " + "AND authority_id = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.role.getRoleID());
            ps.setInt(2, this.authority.getAuthorityID());
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                int nTemp = result.getInt(1);
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("RoleAuthority.isExist(): SQLException: \n\t" + se);
        } finally {
            closeResultSet(result);
            closePreparedStatement(ps);
        }
        return returnValue;
    }

    protected void doAddNew(Connection con) throws Exception {
    }

    protected void doSelfDelete(Connection con) throws Exception {
    }

    /**
	 * �½���ɫȨ�޹�ϵ����
         * @param con ���Ӷ���
         * @throws Exception
         *                          ����½������⣬���׳��쳣
	 */
    protected void doNew(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("RoleAuthority.doNew(): Illegal data values for insert");
        }
        PreparedStatement ps = null;
        String strQuery = "INSERT INTO " + Common.ROLE_AUTHORITY_TABLE + "(role_id,authority_id,provider,provider_type) " + "VALUES (?,?,?,?)";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.role.getRoleID());
            ps.setInt(2, this.authority.getAuthorityID());
            ps.setInt(3, this.provider.getUserID());
            ps.setInt(4, this.providerFigure.getUserGroupID());
            int resultCount = ps.executeUpdate();
            AssignLog aLog = new AssignLog();
            aLog.setLogNO(aLog.getNewLogNo());
            aLog.setID(this.authority.getAuthorityID());
            aLog.setName(this.authority.getAuthorityName());
            aLog.setAssignType(AssignLog.ASSIGN_AUTHORITY);
            aLog.setAssignDate(Translate.getSysTime());
            aLog.setAssignFrom(this.provider.getUserID());
            aLog.setAssignFromName(this.provider.getUserName());
            aLog.setAssignTo(this.role.getRoleID());
            aLog.setAssignToName(this.role.getRoleName());
            aLog.setReceiverType(AssignLog.RECEIVER_ROLE);
            aLog.setInfo("���ɫ�����Ȩ��");
            aLog.setPath("");
            aLog.setPathName("");
            aLog.doUpdateOrNew(con);
            if (resultCount != 1) {
                throw new CesSystemException("RoleAuthority.doNew(): ERROR Inserting data " + "in T_SYS_ROLE_AUTHORITY INSERT !! resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Role_authority.doNew(): SQLException while inserting new Role_authority; " + " role_id = " + this.role.getRoleID() + " authority_id = " + this.authority.getAuthorityID() + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
	 * ���¸ý�ɫȨ�޹�ϵ����
     * @param con ���Ӷ���
     * @throws Exception
     *                          �����������⣬���׳��쳣
	 */
    protected void doUpdate(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("RoleAuthority.doUpdate(): Illegal data values for update");
        }
        PreparedStatement ps = null;
        String strQuery = "UPDATE " + Common.ROLE_AUTHORITY_TABLE + " SET " + "provider = ?, " + "provider_type = ? " + "WHERE role_id = ? " + "AND authority_id = ?";
        try {
            con.setAutoCommit(false);
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.provider.getUserID());
            ps.setInt(2, this.providerFigure.getUserGroupID());
            ps.setInt(3, this.role.getRoleID());
            ps.setInt(4, this.authority.getAuthorityID());
            int resultCount = ps.executeUpdate();
            AssignLog aLog = new AssignLog();
            aLog.setLogNO(aLog.getNewLogNo());
            aLog.setID(this.authority.getAuthorityID());
            aLog.setName(this.authority.getAuthorityName());
            aLog.setAssignType(AssignLog.ASSIGN_AUTHORITY);
            aLog.setAssignDate(Translate.getSysTime());
            aLog.setAssignFrom(this.provider.getUserID());
            aLog.setAssignFromName(this.provider.getUserName());
            aLog.setAssignTo(this.role.getRoleID());
            aLog.setAssignToName(this.role.getRoleName());
            aLog.setReceiverType(AssignLog.RECEIVER_ROLE);
            aLog.setInfo("��Ľ�ɫ�е�Ȩ��");
            aLog.setPath("");
            aLog.setPathName("");
            aLog.doUpdateOrNew(con);
            if (resultCount != 1) {
                throw new CesSystemException("RoleAuthority.doUpdate(): ERROR updating data in T_SYS_ROLE_AUTHORITY!! " + "resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("RoleAuthority.doUpdate(): SQLException while updating Role_authority; " + " role_id = " + this.role.getRoleID() + " authority_id = " + this.authority.getAuthorityID() + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
	 * ����ݿ���ɾ��ý�ɫȨ�޹�ϵ����:ͬʱɾ��,���ύ��
         * @param con ���Ӷ���
         * @throws Exception
         *                          ���ɾ�������⣬���׳��쳣
	 */
    public void doDelete(Connection con) throws Exception {
        PreparedStatement ps = null;
        String strQuery = "DELETE FROM " + Common.ROLE_AUTHORITY_TABLE + " WHERE role_id = ? " + "AND authority_id = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.role.getRoleID());
            ps.setInt(2, this.authority.getAuthorityID());
            int resultCount = ps.executeUpdate();
            AssignLog aLog = new AssignLog();
            aLog.setLogNO(aLog.getNewLogNo());
            aLog.setID(this.authority.getAuthorityID());
            aLog.setName(this.authority.getAuthorityName());
            aLog.setAssignType(AssignLog.ASSIGN_AUTHORITY);
            aLog.setAssignDate(Translate.getSysTime());
            aLog.setAssignFrom(this.provider.getUserID());
            aLog.setAssignFromName(this.provider.getUserName());
            aLog.setAssignTo(this.role.getRoleID());
            aLog.setAssignToName(this.role.getRoleName());
            aLog.setReceiverType(AssignLog.RECEIVER_ROLE);
            aLog.setInfo("ɾ���ɫ�е�Ȩ��");
            aLog.setPath("");
            aLog.setPathName("");
            aLog.doUpdateOrNew(con);
            if (resultCount != 1) {
                throw new CesSystemException("RoleAuthority.doDelete(con): ERROR deleting data in T_SYS_ROLE_AUTHORITY!! " + "resultCount = " + resultCount);
            }
        } catch (Exception se) {
            throw new CesSystemException("Role_authority.doDelete(con): Exception while deleting Role_authority; " + " role_id = " + this.role.getRoleID() + " authority_id = " + this.authority.getAuthorityID() + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
	 * ����ݿ���ɾ��ĳ��ɫӵ�еĽ�ɫȨ�޹�ϵ����:���ύ��
         *
         * @param role              ��ɫ����
         * @throws Exception
         *                          ���ɾ�������⣬���׳��쳣
	 */
    public void doDelete(Role role) throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strEdQuery = "SELECT authority_id from " + Common.ROLE_AUTHORITY_TABLE + " WHERE role_id = ?";
        String strQuery = "DELETE FROM " + Common.ROLE_AUTHORITY_TABLE + " WHERE role_id = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strEdQuery);
                ps.setInt(1, role.getRoleID());
                result = ps.executeQuery();
                while (result.next()) {
                    int authID = result.getInt("authority_id");
                    Authority auth = new Authority(authID);
                    auth.load();
                    AssignLog aLog = new AssignLog();
                    aLog.setLogNO(aLog.getNewLogNo());
                    aLog.setID(authID);
                    aLog.setName(auth.getAuthorityName());
                    aLog.setAssignType(AssignLog.ASSIGN_AUTHORITY);
                    aLog.setAssignDate(Translate.getSysTime());
                    aLog.setAssignFrom(this.provider.getUserID());
                    aLog.setAssignFromName(this.provider.getUserName());
                    aLog.setAssignTo(role.getRoleID());
                    aLog.setAssignToName(role.getRoleName());
                    aLog.setReceiverType(AssignLog.RECEIVER_ROLE);
                    aLog.setInfo("ɾ���ɫ�е�Ȩ��");
                    aLog.setPath("");
                    aLog.setPathName("");
                    aLog.doUpdateOrNew(con);
                }
                ps = con.prepareStatement(strQuery);
                ps.setInt(1, role.getRoleID());
                int resultCount = ps.executeUpdate();
                if (resultCount < 0) {
                    con.rollback();
                    throw new CesSystemException("RoleAuthority.doDelete(role): ERROR deleting data in T_SYS_ROLE_AUTHORITY!! " + "resultCount = " + resultCount);
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("RoleAuthority.doDelete(role): SQLException while deleting Role_authority; " + " role_id = " + role.getRoleID() + " :\n\t" + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException sqle) {
            throw new CesSystemException("RoleAuthority.doDelete(role): SQLException while committing or rollback");
        }
    }

    /**
	 * ��ȡ��ݿ������еĽ�ɫȨ�޹�ϵ����
         *
         * @return                  ��ɫȨ�޹�ϵ���󼯺�
         * @throws Exception
         *                          �����������⣬���׳��쳣
	 */
    public Vector getAllRoleAuthoritys() throws Exception {
        Vector allRoleAuthoritys = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT rt.role_id,rt.role_name,rt.remark,rt.owner,rt.owner_type," + Authority.AUTHORITY_SELECT + "," + "ut.user_id,ut.user_name,ut.login_name,ut.flag_emp," + "ut.user_cryptogram,ut.flag_lock,ut.flag_define,ut.ic_no,ut.conn_num," + "ut.flag_check,ut.flag_active,ut.flag_sa,ut.show_order,ut.position_x,ut.position_y,ut.type " + "FROM " + Common.ROLE_AUTHORITY_TABLE + " rat, " + Common.ROLE_TABLE + " rt, " + Common.AUTHORITY_TABLE + " at1, " + Common.USER_TABLE + " ut " + "WHERE rat.role_id = rt.role_id " + "AND rat.authority_id = at1.authority_id " + "AND rat.provider = ut.user_id ";
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
                Role rlTemp = Role.generateRole(result, va);
                Authority auTemp = Authority.generateAuthority(result, va);
                User uTemp = User.generateUser(result, va);
                RoleAuthority raTemp = new RoleAuthority(rlTemp, auTemp, uTemp, Figure.DEFAULTFIGURE);
                allRoleAuthoritys.addElement(raTemp);
            }
        } catch (SQLException se) {
            throw new CesSystemException("RoleAuthority.getAllRoleAuthoritys(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allRoleAuthoritys;
    }

    /**
     * ��֤������е����
     *
     * @return                   true:   ��֤�ɹ�
     *                           false:  ��֤ʧ��
     */
    protected boolean isValidate() {
        if ((this.role == null) || (this.authority == null) || (this.provider == null) || (this.providerFigure == null)) {
            return (false);
        } else {
            return (true);
        }
    }
}
