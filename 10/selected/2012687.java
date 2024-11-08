package ces.platform.system.dbaccess;

import ces.platform.system.common.IdGenerator;
import ces.platform.system.common.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import ces.coral.dbo.*;
import ces.coral.log.*;

/**
 *  <p>
 *  ����: <font class=titlefont>
 * ����֯����ݱ��ݡ���
 *  </font>
 *  <p>
 *  ����: <font class=descriptionfont>
 *  <br>ʵ����֯����ݵı�����ָ�
 *  </font>
 *  �汾��: <font class=versionfont>
 *  Copyright (c) 2.50.2003.0925
 *  </font>
 *  <p>
 *  ��˾: <font class=companyfont>
 *  �Ϻ�������Ϣ��չ���޹�˾
 *  </font>
 *
 *@author     ����
 *@created    2003��9��25��
 *@version    2.50.2003.0925
 */
public class OrganizeBackup extends OperationBase implements Serializable {

    static Logger logger = new Logger(OrganizeBackup.class);

    /**
     *  ��������Ϊ�����ݡ�
     */
    public static final String BACKUP = "0";

    /**
     *  ��������Ϊ���ָ���
     */
    public static final String RESTORE = "1";

    /**
     *  ��Ա��������¼�ţ���Ӧ��t_sys_organize_bak_record.record_no
     */
    protected int recordNO;

    /**
     *  ��Ա�������汾�ţ���Ӧ��t_sys_organize_bak_record.version_no
     */
    protected int versionNO;

    /**
     *  ��Ա�����������ˣ�operator.user_id��Ӧ��t_sys_organize_bak_record.operator
     */
    protected User operator;

    /**
     *  ��Ա�������������ڣ���Ӧ��t_sys_organize_bak_record.op_date
     */
    protected java.sql.Timestamp opDate;

    /**
     *  ��Ա�������������ͣ���Ӧ��t_sys_organize_bak_record.flag_op
     */
    protected String flagOp;

    /**
     *  ���캯��1
     *
     *@param  recordNO  ��¼��
     */
    public OrganizeBackup(int recordNO) {
        this.recordNO = recordNO;
    }

    /**
     *  ���캯��2
     *
     *@param  recordNO   ��¼��
     *@param  versionNO  �汾��
     *@param  operator   ������
     *@param  opDate     ��������
     *@param  flagOp     ��������
     */
    public OrganizeBackup(int recordNO, int versionNO, User operator, java.sql.Timestamp opDate, String flagOp) {
        this.recordNO = recordNO;
        this.versionNO = versionNO;
        this.operator = operator;
        this.opDate = opDate;
        this.flagOp = flagOp;
    }

    /**
     *  ���ñ��ݲ�������ı��ݲ�����¼��
     *
     *@param  recordNO  ���ݲ�����¼��
     */
    public void setRecordNO(int recordNO) {
        this.recordNO = recordNO;
    }

    /**
     *  ���ñ��ݲ�������ı��ݰ汾��
     *
     *@param  versionNO  ���ݰ汾��
     */
    public void setVersionNO(int versionNO) {
        this.versionNO = versionNO;
    }

    /**
     *  ���ñ��ݲ�������ı��ݻ�ָ�������
     *
     *@param  operator  ���ݻ�ָ�������
     */
    public void setOperator(User operator) {
        this.operator = operator;
    }

    /**
     *  ���ñ��ݲ�������ı��ݻ�ָ���������
     *
     *@param  opDate  ���ݻ�ָ���������
     */
    public void setOpDate(java.sql.Timestamp opDate) {
        this.opDate = opDate;
    }

    /**
     *  ���ñ��ݲ�������Ĳ������ͣ�"����"��"�ָ�"��
     *
     *@param  flagOp  ���ݻ�ָ��������ͣ�"����"��"�ָ�"��
     */
    public void setFlagOp(String flagOp) {
        this.flagOp = flagOp;
    }

    /**
     *  ��ȡ���ݲ�������ı��ݲ�����¼��
     *
     *@return    ���ݲ�����¼��
     */
    public int getRecordNO() {
        return this.recordNO;
    }

    /**
     *  ��ȡ���ݲ�������ı��ݰ汾��
     *
     *@return    ���ݰ汾��
     */
    public int getVersionNO() {
        return this.versionNO;
    }

    /**
     *  ��ȡ���ݲ�������ı��ݻ�ָ�������
     *
     *@return    ���ݻ�ָ�������
     */
    public User getOperator() {
        return this.operator;
    }

    /**
     *  ��ȡ���ݲ�������ı��ݻ�ָ���������
     *
     *@return    ���ݻ�ָ���������
     */
    public java.sql.Timestamp getOpDate() {
        return this.opDate;
    }

    /**
     *  ��ȡ���ݲ�������Ĳ������ͣ�"����"��"�ָ�"��
     *
     *@return    ���ݻ�ָ��������ͣ�"����"��"�ָ�"��
     */
    public String getFlagOp() {
        return this.flagOp;
    }

    /**
     *  ��֤����֯����ݱ��ݶ�������ݿ����Ƿ����
     *
     *@return                                           true: �ö�������ݿ��д��� false:
     *      �ö�������ݿ��в�����
     *@throws Exception                    Description of the
     *      Exception
     */
    protected boolean isExist() throws Exception {
        boolean returnValue = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT record_no FROM " + Common.ORGANIZE_BAK_RECORD_TABLE + " WHERE record_no = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.recordNO);
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                String strTemp = result.getString(1);
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.isExist(): SQLException: \n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return returnValue;
    }

    /**
     *  ��֤����֯����ݱ��ݶ�������ݿ����Ƿ����,���ύ
     *
     *@return                                           true: �ö�������ݿ��д��� false:
     *      �ö�������ݿ��в�����
     *@throws Exception                    Description of the
     *      Exception
     */
    protected boolean isExist(Connection con) throws Exception {
        boolean returnValue = false;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT record_no FROM " + Common.ORGANIZE_BAK_RECORD_TABLE + " WHERE record_no = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.recordNO);
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                String strTemp = result.getString(1);
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.isExist(): SQLException: \n\t" + se);
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
     *  �½�����֯����ݱ��ݶ���
     *@param con ���Ӷ���
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doNew(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("Organize_backup.doNew(): Illegal data values for insert");
        }
        PreparedStatement ps = null;
        String strQuery = "INSERT INTO " + Common.ORGANIZE_BAK_RECORD_TABLE + "(record_no,version_no,operator,op_date,flag_op) " + "VALUES (?,?,?,?,?)";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.recordNO);
            ps.setInt(2, this.versionNO);
            ps.setInt(3, this.operator.getUserID());
            ps.setTimestamp(4, this.opDate);
            ps.setString(5, this.flagOp);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("Organize_backup.doNew(): ERROR Inserting data " + "in T_SYS_ORGANIZE_BAK_RECORD INSERT !! resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doNew(): SQLException while inserting new Organize_backup; " + "record_no = " + this.recordNO + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     *  ���¸���֯����ݱ��ݶ���
     *@param con ���Ӷ���
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doUpdate(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("Organize_backup.doUpdate(): Illegal data values for update");
        }
        PreparedStatement ps = null;
        String strQuery = "UPDATE " + Common.ORGANIZE_BAK_RECORD_TABLE + " SET " + "version_no = ?, " + "operator = ?, " + "op_date = ?, " + "flag_op = ? " + "WHERE record_no = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.versionNO);
            ps.setInt(2, this.operator.getUserID());
            ps.setTimestamp(3, this.opDate);
            ps.setString(4, this.flagOp);
            ps.setInt(5, this.recordNO);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("Organize_backup.doUpdate(): ERROR updating data in T_SYS_ORGANIZE_BAK_RECORD!! " + "resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doUpdate(): SQLException while updating Organize_backup; " + "record_no = " + this.recordNO + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     *  ����ݿ���ɾ�����֯����ݱ��ݶ���:ͬʱɾ��,���ύ��
     *@param con ���Ӷ���
     *@throws Exception                    Description of the
     *      Exception
     */
    public void doDelete(Connection con) throws Exception {
        PreparedStatement ps = null;
        String strBrtQuery = "DELETE FROM " + Common.ORGANIZE_BAK_RECORD_TABLE + " WHERE record_no = ?";
        String strTbtQuery = "DELETE FROM " + Common.ORGANIZE_TYPE_B_TABLE + " WHERE version_no = ?";
        String strTrbtQuery = "DELETE FROM " + Common.ORGANIZE_TYPE_RELATION_B_TABLE + " WHERE version_no = ?";
        String strBtQuery = "DELETE FROM " + Common.ORGANIZE_B_TABLE + " WHERE version_no = ?";
        String strRbtQuery = "DELETE FROM " + Common.ORGANIZE_RELATION_B_TABLE + " WHERE version_no = ?";
        try {
            ps = con.prepareStatement(strBtQuery);
            ps.setInt(1, this.versionNO);
            ps.executeUpdate();
            ps = con.prepareStatement(strRbtQuery);
            ps.setInt(1, this.versionNO);
            ps.executeUpdate();
            ps = con.prepareStatement(strTbtQuery);
            ps.setInt(1, this.versionNO);
            ps.executeUpdate();
            ps = con.prepareStatement(strTrbtQuery);
            ps.setInt(1, this.versionNO);
            ps.executeUpdate();
            ps = con.prepareStatement(strBrtQuery);
            ps.setInt(1, this.recordNO);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("Organize_backup.doDelete(): ERROR deleting data in T_SYS_ORGANIZE_BAK_RECORD!! " + "resultCount = " + resultCount);
            }
        } catch (Exception se) {
            throw new CesSystemException("Organize_backup.doDelete(): SQLException while deleting Organize_backup; " + "record_no = " + this.recordNO + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     *  ��ȡ��ݿ������е���֯����ݱ������
     *
     *@return                                           ��֯����ݱ��ݶ���ļ���
     *@throws Exception                    Description of the
     *      Exception
     */
    public Vector getAllOrganizeBackups() throws Exception {
        Vector allOrganizeBackups = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT ut.user_id,ut.user_name,ut.login_name,ut.flag_emp," + "ut.user_cryptogram,ut.flag_lock,ut.flag_define,ut.ic_no,ut.conn_num," + "ut.flag_check,ut.flag_active,ut.flag_sa,ut.show_order,ut.position_x,ut.position_y,ut.type," + "obrt.record_no,obrt.version_no,obrt.operator,obrt.op_date,obrt.flag_op " + "FROM " + Common.USER_TABLE + " ut, " + Common.ORGANIZE_BAK_RECORD_TABLE + " obrt " + "WHERE obrt.operator = ut.user_id";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            int i = 1;
            while (result.next()) {
                i = 1;
                int strUserId = result.getInt(i++);
                String strUserName = result.getString(i++);
                String strLoginName = result.getString(i++);
                String strFlagEmp = result.getString(i++);
                String strUserCryptogram = result.getString(i++);
                String strFlagLock = result.getString(i++);
                String strFlagDefine = result.getString(i++);
                String strIcNo = result.getString(i++);
                int nConnNum = result.getInt(i++);
                String strFlagCheck = result.getString(i++);
                String strFlagActive = result.getString(i++);
                String strFlagSA = result.getString(i++);
                int nShowOrder = result.getInt(i++);
                int nPositionX = result.getInt(i++);
                int nPositionY = result.getInt(i++);
                String type = result.getString(i++);
                User uTemp = new User(strUserId, strUserName, strLoginName, strFlagEmp, strUserCryptogram, strFlagLock, strFlagDefine, strIcNo, nConnNum, strFlagCheck, strFlagActive, strFlagSA, nShowOrder, nPositionX, nPositionY, type);
                int nRecordNO = result.getInt(i++);
                int nVersionNO = result.getInt(i++);
                String strOperaor = result.getString(i++);
                java.sql.Timestamp dOpDate = result.getTimestamp(i++);
                String strFlagOp = result.getString(i++);
                OrganizeBackup ob = new OrganizeBackup(nRecordNO, nVersionNO, uTemp, dOpDate, strFlagOp);
                allOrganizeBackups.addElement(ob);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.getAllOrganizeBackups(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allOrganizeBackups;
    }

    /**
     *  ��ȡһ���µİ汾��
     *
     *@return                                           �µİ汾�ţ��硰1,2,3...��
     *@throws Exception                    Description of the
     *      Exception
     */
    public static int getNewVersionNo() throws Exception {
        return ((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_SYS_VERSION));
    }

    /**
     *  ��ȡһ���µļ�¼��
     *
     *@return                                           �µļ�¼�ţ��硰1,2,3...��
     *@throws Exception                    Description of the
     *      Exception
     */
    public static int getNewRecordNo() throws Exception {
        return ((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_SYS_RECORD_NO));
    }

    /**
     *  ���б��ݲ���
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    public void doBackup() throws Exception {
        doBackupOrganize();
        doBackupOrganizeRelation();
        doBackupOrganizeType();
        doBackupOrganizeTypeRelation();
    }

    /**
     *  ���ݡ���֯���ͱ?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doBackupOrganizeType() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strSelQuery = "SELECT organize_type_id,organize_type_name,width " + "FROM " + Common.ORGANIZE_TYPE_TABLE;
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_TYPE_B_TABLE + " " + "(version_no,organize_type_id,organize_type_name,width) " + "VALUES (?,?,?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strSelQuery);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setInt(1, this.versionNO);
                    ps.setString(2, result.getString("organize_type_id"));
                    ps.setString(3, result.getString("organize_type_name"));
                    ps.setInt(4, result.getInt("width"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doBackupOrganizeType(): ERROR Inserting data " + "in T_SYS_ORGANIZE_B_TYPE INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doBackupOrganizeType(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doBackupOrganizeType(): SQLException while committing or rollback");
        }
    }

    /**
     *  ���ݡ���֯���͹�ϵ�?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doBackupOrganizeTypeRelation() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strSelQuery = "SELECT parent_organize_type,child_organize_type " + "FROM " + Common.ORGANIZE_TYPE_RELATION_TABLE;
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_TYPE_RELATION_B_TABLE + " " + "(version_no,parent_organize_type,child_organize_type) " + "VALUES (?,?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strSelQuery);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setInt(1, this.versionNO);
                    ps.setString(2, result.getString("parent_organize_type"));
                    ps.setString(3, result.getString("child_organize_type"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doBackupOrganizeTypeRelation(): ERROR Inserting data " + "in T_SYS_ORGANIZE_TYPE_RELATION_B INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doBackupOrganizeTypeRelation(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doBackupOrganizeTypeRelation(): SQLException while committing or rollback");
        }
    }

    /**
     *  ���ݡ���֯�?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doBackupOrganize() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strSelQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE;
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_B_TABLE + " " + "(version_no,organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y) " + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strSelQuery);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setInt(1, this.versionNO);
                    ps.setString(2, result.getString("organize_id"));
                    ps.setString(3, result.getString("organize_type_id"));
                    ps.setString(4, result.getString("organize_name"));
                    ps.setString(5, result.getString("organize_manager"));
                    ps.setString(6, result.getString("organize_describe"));
                    ps.setString(7, result.getString("work_type"));
                    ps.setInt(8, result.getInt("show_order"));
                    ps.setInt(9, result.getInt("position_x"));
                    ps.setInt(10, result.getInt("position_y"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doBackupOrganize(): ERROR Inserting data " + "in T_SYS_ORGANIZE_B INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doBackupOrganize(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doBackupOrganize(): SQLException while committing or rollback");
        }
    }

    /**
     *  ���ݡ���֯��ϵ�?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doBackupOrganizeRelation() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strSelQuery = "SELECT organize_id,organize_type_id,child_id,child_type_id,remark " + "FROM " + Common.ORGANIZE_RELATION_TABLE;
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_RELATION_B_TABLE + " " + "(version_no,organize_id,organize_type,child_id,child_type,remark) " + "VALUES (?,?,?,?,?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strSelQuery);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setInt(1, this.versionNO);
                    ps.setString(2, result.getString("organize_id"));
                    ps.setString(3, result.getString("organize_type_id"));
                    ps.setString(4, result.getString("child_id"));
                    ps.setString(5, result.getString("child_type_id"));
                    ps.setString(6, result.getString("remark"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doBackupOrganizeRelation(): ERROR Inserting data " + "in T_SYS_ORGANIZE_RELATION_B INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doBackupOrganizeRelation(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doBackupOrganizeRelation(): SQLException while committing or rollback");
        }
    }

    /**
     *  ���лָ�����
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    public void doRestore() throws Exception {
        doRestoreOrganizeType();
        doRestoreOrganizeTypeRelation();
        doRestoreOrganize();
        doRestoreOrganizeRelation();
    }

    /**
     *  �ָ�����֯���ͱ?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doRestoreOrganizeType() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strDelQuery = "DELETE FROM " + Common.ORGANIZE_TYPE_TABLE;
        String strSelQuery = "SELECT organize_type_id,organize_type_name,width " + "FROM " + Common.ORGANIZE_TYPE_B_TABLE + " " + "WHERE version_no = ?";
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_TYPE_TABLE + " " + "(organize_type_id,organize_type_name,width) " + "VALUES (?,?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strDelQuery);
                ps.executeUpdate();
                ps = con.prepareStatement(strSelQuery);
                ps.setInt(1, this.versionNO);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setString(1, result.getString("organize_type_id"));
                    ps.setString(2, result.getString("organize_type_name"));
                    ps.setInt(3, result.getInt("width"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doRestoreOrganizeType(): ERROR Inserting data " + "in T_SYS_ORGANIZE_TYPE INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doRestoreOrganizeType(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doRestoreOrganizeType(): SQLException while committing or rollback");
        }
    }

    /**
     *  �ָ�����֯���͹�ϵ�?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doRestoreOrganizeTypeRelation() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strDelQuery = "DELETE FROM " + Common.ORGANIZE_TYPE_RELATION_TABLE;
        String strSelQuery = "SELECT parent_organize_type,child_organize_type " + "FROM " + Common.ORGANIZE_TYPE_RELATION_B_TABLE + " " + "WHERE version_no = ?";
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_TYPE_RELATION_TABLE + " " + "(parent_organize_type,child_organize_type) " + "VALUES (?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strDelQuery);
                ps.executeUpdate();
                ps = con.prepareStatement(strSelQuery);
                ps.setInt(1, this.versionNO);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setString(1, result.getString("parent_organize_type"));
                    ps.setString(2, result.getString("child_organize_type"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doRestoreOrganizeTypeRelation(): ERROR Inserting data " + "in T_SYS_ORGANIZE_TYPE_RELATION INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doRestoreOrganizeTypeRelation(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doRestoreOrganizeTypeRelation(): SQLException while committing or rollback");
        }
    }

    /**
     *  �ָ�����֯�?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doRestoreOrganize() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strDelQuery = "DELETE FROM " + Common.ORGANIZE_TABLE;
        String strSelQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_B_TABLE + " " + "WHERE version_no = ?";
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_TABLE + " " + "(organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y) " + "VALUES (?,?,?,?,?,?,?,?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strDelQuery);
                ps.executeUpdate();
                ps = con.prepareStatement(strSelQuery);
                ps.setInt(1, this.versionNO);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setString(1, result.getString("organize_id"));
                    ps.setString(2, result.getString("organize_type_id"));
                    ps.setString(3, result.getString("organize_name"));
                    ps.setString(4, result.getString("organize_manager"));
                    ps.setString(5, result.getString("organize_describe"));
                    ps.setString(6, result.getString("work_type"));
                    ps.setInt(7, result.getInt("show_order"));
                    ps.setInt(8, result.getInt("position_x"));
                    ps.setInt(9, result.getInt("position_y"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doRestoreOrganize(): ERROR Inserting data " + "in T_SYS_ORGANIZE INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doRestoreOrganize(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doRestoreOrganize(): SQLException while committing or rollback");
        }
    }

    /**
     *  �ָ�����֯��ϵ�?
     *
     *@throws Exception                    Description of the
     *      Exception
     */
    protected void doRestoreOrganizeRelation() throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strDelQuery = "DELETE FROM " + Common.ORGANIZE_RELATION_TABLE;
        String strSelQuery = "SELECT organize_id,organize_type_id,child_id,child_type_id,remark " + "FROM " + Common.ORGANIZE_RELATION_B_TABLE + " " + "WHERE version_no = ?";
        String strInsQuery = "INSERT INTO " + Common.ORGANIZE_RELATION_TABLE + " " + "(organize_id,organize_type,child_id,child_type,remark) " + "VALUES (?,?,?,?,?)";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            try {
                con = dbo.getConnection();
                con.setAutoCommit(false);
                ps = con.prepareStatement(strDelQuery);
                ps.executeUpdate();
                ps = con.prepareStatement(strSelQuery);
                ps.setInt(1, this.versionNO);
                result = ps.executeQuery();
                ps = con.prepareStatement(strInsQuery);
                while (result.next()) {
                    ps.setString(1, result.getString("organize_id"));
                    ps.setString(2, result.getString("organize_type_id"));
                    ps.setString(3, result.getString("child_id"));
                    ps.setString(4, result.getString("child_type_id"));
                    ps.setString(5, result.getString("remark"));
                    int resultCount = ps.executeUpdate();
                    if (resultCount != 1) {
                        con.rollback();
                        throw new CesSystemException("Organize_backup.doRestoreOrganizeRelation(): ERROR Inserting data " + "in T_SYS_ORGANIZE_RELATION INSERT !! resultCount = " + resultCount);
                    }
                }
                con.commit();
            } catch (SQLException se) {
                con.rollback();
                throw new CesSystemException("Organize_backup.doRestoreOrganizeRelation(): SQLException:  " + se);
            } finally {
                con.setAutoCommit(true);
                close(dbo, ps, result);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize_backup.doRestoreOrganizeRelation(): SQLException while committing or rollback");
        }
    }

    /**
     * ��֤������֯����ݱ��ݱ��е����
     *
     * @return                   true:   ��֤�ɹ�
     *                           false:  ��֤ʧ��
     */
    protected boolean isValidate() {
        if ((this.operator == null) || (this.opDate == null) || (this.recordNO == 0) || (this.versionNO == 0) || (this.operator.getUserID() == 0)) {
            return (false);
        } else {
            return (true);
        }
    }
}
