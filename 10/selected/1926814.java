package ces.platform.system.dbaccess;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Vector;
import ces.coral.dbo.DBOperation;
import ces.coral.log.Logger;
import ces.platform.system.common.CesSystemException;
import ces.platform.system.common.Constant;
import ces.platform.system.common.IdGenerator;
import ces.platform.system.common.OperationBase;
import ces.platform.system.common.ValueAsc;
import ces.platform.system.common.XmlInfo;
import ces.platform.system.facade.OrgUser;

/**
 * <p>
 * ����: <font class=titlefont> ����֯���� </font>
 * <p>
 * ����: <font class=descriptionfont> <br>
 * ����������͵���֯�� ����֯������ĳ������ </font>
 * <p>
 * �汾��: <font class=versionfont> Copyright (c) 2.50.2003.0925 </font>
 * <p>
 * ��˾: <font class=companyfont> �Ϻ�������Ϣ��չ���޹�˾ </font>
 * 
 * @author ����
 * @version 2.50.2003.0925
 */
public class Organize extends OperationBase implements Serializable {

    static Logger logger = new Logger(ces.platform.system.dbaccess.Organize.class);

    public static final int TOP = -1;

    public static final int selfTOP = -2;

    /**
	 * ��Ա��������֯���ͱ�ţ���Ӧ��t_sys_organize.organize_type_id��
	 */
    protected String organizeTypeID;

    /**
	 * ��Ա��������֯��ţ���Ӧ��t_sys_organize.organize_id
	 */
    protected int organizeID;

    /**
	 * ��Ա��������֯��ƣ���Ӧ��t_sys_organize.organize_name
	 */
    protected String organizeName;

    /**
	 * ��Ա��������֯�����ߣ���Ӧ��t_sys_organize.organize_manager
	 */
    protected String organizeManager;

    /**
	 * ��Ա��������֯��������Ӧ��t_sys_organize.organize_describe
	 */
    protected String organizeDescribe;

    /**
	 * ��Ա��������֯�������ͣ���Ӧ��t_sys_organize.work_type
	 */
    protected String workType;

    /**
	 * ��Ա��������ʾ��ţ���Ӧ��t_sys_organize.show_order
	 */
    protected int showOrder;

    /**
	 * ��Ա������xλ�ã���Ӧ��t_sys_organize.position_x
	 */
    protected int positionX;

    /**
	 * ��Ա������yλ�ã���Ӧ��t_sys_organize.position_y
	 */
    protected int positionY;

    /**
	 * ��Ա�������Ƿ�Ϊ��ְ��֯
	 */
    protected String partTime = "no";

    /**
	 * ���캯��1
	 * 
	 * @param organizeTypeID
	 *            ��֯���ͱ��
	 * @param organizeID
	 *            ��֯���
	 */
    public Organize(int organizeID, String organizeTypeID) {
        this.organizeTypeID = organizeTypeID;
        this.organizeID = organizeID;
    }

    /**
	 * ���캯��2
	 * 
	 * @param organizeID
	 *            ��֯���
	 * @param organizeTypeID
	 *            ��֯���ͱ��
	 * @param organizeName
	 *            ��֯���
	 * @param organizeManager
	 *            ��֯������
	 * @param organizeDescribe
	 *            ��֯����
	 * @param showOrder
	 *            ��֯���
	 */
    public Organize(int organizeID, String organizeTypeID, String organizeName, String organizeManager, String organizeDescribe, int showOrder) {
        this.organizeID = organizeID;
        this.organizeTypeID = organizeTypeID;
        this.organizeName = organizeName;
        this.organizeManager = organizeManager;
        this.organizeDescribe = organizeDescribe;
        this.showOrder = showOrder;
    }

    /**
	 * ���캯��3
	 * 
	 * @param organizeID
	 *            ��֯���
	 * @param organizeTypeID
	 *            ��֯���ͱ��
	 * @param organizeName
	 *            ��֯���
	 * @param organizeManager
	 *            ��֯������
	 * @param organizeDescribe
	 *            ��֯����
	 * @param workType
	 *            ҵ�����
	 * @param showOrder
	 *            ��֯���
	 * @param positionX
	 *            xλ��
	 * @param positionY
	 *            yλ��
	 */
    public Organize(int organizeID, String organizeTypeID, String organizeName, String organizeManager, String organizeDescribe, String workType, int showOrder, int positionX, int positionY) {
        this.organizeID = organizeID;
        this.organizeTypeID = organizeTypeID;
        this.organizeName = organizeName;
        this.organizeManager = organizeManager;
        this.organizeDescribe = organizeDescribe;
        this.workType = workType;
        this.showOrder = showOrder;
        this.positionX = positionX;
        this.positionY = positionY;
    }

    /**
	 * ȱʡ���캯��4
	 */
    public Organize() {
        this(0, null, null, null, null, null, 0, 0, 0);
    }

    /**
	 * ������֯�������֯����
	 * 
	 * @param organizeTypeID
	 *            ��֯���ͱ��
	 */
    public void setOrganizeTypeID(String organizeTypeID) {
        this.organizeTypeID = organizeTypeID;
    }

    /**
	 * ������֯����ı��
	 * 
	 * @param organizeID
	 *            ��֯���
	 */
    public void setOrganizeID(int organizeID) {
        this.organizeID = organizeID;
    }

    /**
	 * ������֯��������
	 * 
	 * @param organizeName
	 *            ��֯���
	 */
    public void setOrganizeName(String organizeName) {
        this.organizeName = organizeName;
    }

    /**
	 * ������֯�������֯������
	 * 
	 * @param organizeManager
	 *            ��֯������
	 */
    public void setOrganizeManager(String organizeManager) {
        this.organizeManager = organizeManager;
    }

    /**
	 * ������֯�������֯����
	 * 
	 * @param organizeDescribe
	 *            ��֯����
	 */
    public void setOrganizeDescribe(String organizeDescribe) {
        this.organizeDescribe = organizeDescribe;
    }

    /**
	 * ������֯����Ĺ�������
	 * 
	 * @param workType
	 *            ��֯��������
	 */
    public void setWorkType(String workType) {
        this.workType = workType;
    }

    /**
	 * ������֯�����xλ��
	 * 
	 * @param positionX
	 *            xλ��
	 */
    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    /**
	 * ������֯�����yλ��
	 * 
	 * @param positionY
	 *            yλ��
	 */
    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    /**
	 * ������ʾ���
	 * 
	 * @param showOrder
	 *            ��ʾ���
	 */
    public void setShowOrder(int showOrder) {
        this.showOrder = showOrder;
    }

    /**
	 * ��ȡ��֯���Ͷ���ı��
	 * 
	 * @return ��֯���ͱ��
	 */
    public String getOrganizeTypeID() {
        return this.organizeTypeID;
    }

    /**
	 * ��ȡ��֯����ı��
	 * 
	 * @return ��֯���
	 */
    public int getOrganizeID() {
        return this.organizeID;
    }

    /**
	 * ��ȡ��֯��������
	 * 
	 * @return ��֯���
	 */
    public String getOrganizeName() {
        return this.organizeName;
    }

    /**
	 * ��ȡ��֯�������֯������
	 * 
	 * @return ��֯������
	 */
    public String getOrganizeManager() {
        return this.organizeManager;
    }

    /**
	 * ��ȡ��֯�������֯����
	 * 
	 * @return ��֯����
	 */
    public String getOrganizeDescribe() {
        return this.organizeDescribe;
    }

    /**
	 * ��ȡ��֯����Ĺ�������
	 * 
	 * @return ��������
	 */
    public String getWorkType() {
        return this.workType;
    }

    /**
	 * ��ȡ��֯�����xλ��
	 * 
	 * @return xλ��
	 */
    public int getPositionX() {
        return this.positionX;
    }

    /**
	 * ��ȡ��֯�����yλ��
	 * 
	 * @return yλ��
	 */
    public int getPositionY() {
        return this.positionY;
    }

    /**
	 * ��ȡ��ʾ���
	 * 
	 * @return ��ʾ���
	 */
    public int getShowOrder() {
        return this.showOrder;
    }

    /**
	 * ��ȡ�Ƿ��ְ
	 * 
	 * @return �Ƿ��ְ
	 */
    public String getPartTime() {
        return partTime;
    }

    /**
	 * �����Ƿ��ְ
	 * 
	 * @param partTime
	 *            �Ƿ��ְ
	 */
    public void setPartTime(String partTime) {
        this.partTime = partTime;
    }

    /**
	 * ��֤����֯��������ݿ����Ƿ����
	 * 
	 * @return true: �ö�������ݿ��д��� false: �ö�������ݿ��в�����
	 * @throws Exception
	 *             �����֤�����⣬���׳��쳣
	 */
    protected boolean isExist() throws Exception {
        boolean returnValue = false;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id = ? AND organize_type_id = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            ps.setString(1, String.valueOf(this.organizeID));
            ps.setString(2, this.organizeTypeID);
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                int nTemp = result.getInt(1);
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize.isExist(): SQLException: \n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return returnValue;
    }

    /**
	 * ��֤����֯��������ݿ����Ƿ����
	 * 
	 * @return true: �ö�������ݿ��д��� false: �ö�������ݿ��в�����
	 * @throws Exception
	 *             �����֤�����⣬���׳��쳣
	 */
    protected boolean isExist(Connection con) throws Exception {
        boolean returnValue = false;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id = ? AND organize_type_id = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setInt(1, this.organizeID);
            ps.setString(2, this.organizeTypeID);
            result = ps.executeQuery();
            if (!result.next()) {
                returnValue = false;
            } else {
                int nTemp = result.getInt(1);
                returnValue = true;
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize.isExist(): SQLException: \n\t" + se);
        } finally {
            closeResultSet(result);
            closePreparedStatement(ps);
        }
        return returnValue;
    }

    protected void doAddNew(Connection con) throws Exception {
    }

    public int getMaxChildOrder(String orgTypeID) throws Exception {
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        String maxSql = "select max(a.show_order) from " + Common.ORGANIZE_TABLE + " a ," + Common.ORGANIZE_RELATION_TABLE + " b where b.organize_id ='" + organizeID + "' and a.organize_id = b.child_id " + "and a.organize_type_id = '" + orgTypeID + "'";
        int orderNo = -1;
        ResultSet rest = dbo.select(maxSql);
        if (rest.next()) {
            orderNo = rest.getInt(1);
        }
        this.closeConnection(dbo);
        return orderNo;
    }

    public int getMaxUserOrder() throws SQLException {
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        String maxSql = "select max(b.user_order) from " + Common.USER_TABLE + " a ," + Common.ORGANIZE_RELATION_TABLE + " b where b.organize_id ='" + organizeID + "' and b.child_type_id =  '" + OrganizeType.USER + "'";
        if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
            maxSql += " and b.child_id = convert(varchar,a.user_id) ";
        } else {
            maxSql += " and a.user_id = b.child_id ";
        }
        logger.debug("wbq: " + maxSql);
        int orderNo = -1;
        ResultSet rest = dbo.select(maxSql);
        if (rest.next()) {
            orderNo = rest.getInt(1);
        }
        dbo.commit();
        return orderNo;
    }

    /**
	 * �½���֯����
	 * 
	 * @param con
	 *            ���Ӷ���
	 * @throws Exception
	 *             ����½������⣬���׳��쳣
	 */
    protected void doNew(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("Organize.doNew(): Illegal data values for insert");
        }
        PreparedStatement ps = null;
        String strQuery = "INSERT INTO " + Common.ORGANIZE_TABLE + "(organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y) " + "VALUES (?,?,?,?,?,?,?,?,?)";
        logger.debug("wbq:6" + strQuery);
        try {
            ps = con.prepareStatement(strQuery);
            ps.setString(1, String.valueOf(this.organizeID));
            ps.setString(2, this.organizeTypeID);
            ps.setString(3, this.organizeName);
            ps.setString(4, this.organizeManager);
            ps.setString(5, this.organizeDescribe);
            ps.setString(6, this.workType);
            ps.setInt(7, showOrder);
            ps.setInt(8, this.positionX);
            ps.setInt(9, this.positionY);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("Organize.doNew(): ERROR Inserting data " + "in T_SYS_ORGANIZE INSERT !! resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize.doNew(): SQLException while inserting new organize; " + "organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
	 * ���¸���֯����
	 * 
	 * @param con
	 *            ���Ӷ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    protected void doUpdate(Connection con) throws Exception {
        if (!isValidate()) {
            throw new CesSystemException("Organize.doNew(): Illegal data values for insert");
        }
        PreparedStatement ps = null;
        String strQuery = "UPDATE " + Common.ORGANIZE_TABLE + " SET " + "organize_name = ?, " + "organize_manager = ?, " + "organize_describe = ?, " + "work_type = ?, " + "show_order= ?," + "position_x = ?, " + "position_y = ? " + "WHERE organize_type_id = ? " + "AND organize_id = ?";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setString(1, this.organizeName);
            ps.setString(2, this.organizeManager);
            ps.setString(3, this.organizeDescribe);
            ps.setString(4, this.workType);
            ps.setInt(5, this.showOrder);
            ps.setInt(6, this.positionX);
            ps.setInt(7, this.positionY);
            ps.setString(8, this.organizeTypeID);
            ps.setString(9, String.valueOf(this.organizeID));
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("Organize.doUpdate(): ERROR updating data in T_SYS_ORGANIZE!! " + "resultCount = " + resultCount);
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize.doUpdate(): SQLException while updating organize; " + "organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
	 * ����ݿ�������װ�ر���֯���
	 * 
	 * @throws Exception
	 *             ����ȡ�����⣬���׳��쳣
	 */
    public void load() throws Exception {
        if (this.organizeID == 0 || this.organizeTypeID == null) {
            throw new CesSystemException("Organize.load(): Illegal key words for load");
        }
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " " + "WHERE organize_id = ? " + "AND organize_type_id = ?";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            ps.setString(1, String.valueOf(this.organizeID));
            ps.setString(2, this.organizeTypeID);
            result = ps.executeQuery();
            while (result.next()) {
                this.organizeID = result.getInt("organize_id");
                this.organizeTypeID = result.getString("organize_type_id");
                this.organizeName = result.getString("organize_name");
                this.organizeManager = result.getString("organize_manager");
                this.organizeDescribe = result.getString("organize_describe");
                this.workType = result.getString("work_type");
                this.showOrder = result.getInt("show_order");
                this.positionX = result.getInt("position_x");
                this.positionY = result.getInt("position_y");
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize.load(): SQLException while loading organize; " + "organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
    }

    public void load(int orgID) throws Exception {
        if (orgID == 0) {
            throw new CesSystemException("Organize.load(): Illegal key words for load");
        }
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " " + "WHERE organize_id = ? ";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            ps.setString(1, String.valueOf(orgID));
            result = ps.executeQuery();
            while (result.next()) {
                this.organizeID = result.getInt("organize_id");
                this.organizeTypeID = result.getString("organize_type_id");
                this.organizeName = result.getString("organize_name");
                this.organizeManager = result.getString("organize_manager");
                this.organizeDescribe = result.getString("organize_describe");
                this.workType = result.getString("work_type");
                this.showOrder = result.getInt("show_order");
                this.positionX = result.getInt("position_x");
                this.positionY = result.getInt("position_y");
            }
        } catch (SQLException se) {
            throw new CesSystemException("Organize.load(): SQLException while loading organize; " + "organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
    }

    /**
	 * ����ݿ���ɾ�����֯����:ͬʱɾ�����֯�����¼���֯����֯ ����Ĺ�ϵ���������������й�ϵ��,���ύ��
	 * 
	 * @param con
	 *            ���Ӷ���
	 * @throws Exception
	 *             ���ɾ�������⣬���׳��쳣
	 */
    public void doDelete(Connection con) throws Exception {
        Organize child = null;
        try {
            Organize org = new Organize(this.organizeID, this.organizeTypeID);
            Enumeration enumChilds = getAllChilds(org, con).elements();
            Vector vUser = new OrgUser().getAllUsers(this.organizeID, this.organizeTypeID);
            OrganizeRelation orgR = new OrganizeRelation();
            for (int i = 0; i < vUser.size(); i++) {
                User us = (User) vUser.elementAt(i);
                orgR.update(us.getUserID());
            }
            while (enumChilds.hasMoreElements()) {
                child = (Organize) enumChilds.nextElement();
                child.delete(con);
            }
            org.delete(con);
        } catch (Exception e) {
            throw new CesSystemException("Organize.doDelete(con): Exception while deleting organize; " + "organize_id = " + this.organizeID + " :\n\t" + e);
        }
    }

    /**
	 * ����ݿ���ɾ�����֯����:ͬʱɾ�����֯�����¼���֯����֯ ����Ĺ�ϵ���������������й�ϵ��,���ύ��
	 * 
	 * @param con
	 *            ���Ӷ���
	 * @throws Exception
	 *             ���ɾ�������⣬���׳��쳣
	 */
    public void doSelfDelete(Connection con) throws Exception {
        Organize child = null;
        try {
            Organize org = new Organize(this.organizeID, this.organizeTypeID);
            org.delete(con);
        } catch (Exception e) {
            throw new CesSystemException("Organize.doDelete(con): Exception while deleting organize; " + "organize_id = " + this.organizeID + " :\n\t" + e);
        }
    }

    /**
	 * ����ݿ���ɾ�����֯����:ͬʱɾ�����֯ ����Ĺ�ϵ���������������й�ϵ��,���ύ��
	 * 
	 * @param con
	 *            ���Ӷ���
	 * @throws Exception
	 *             ���ɾ�������⣬���׳��쳣
	 */
    public void delete(Connection con) throws Exception {
        PreparedStatement ps = null;
        OrganizeRelation or = null;
        Organize child = null;
        String strQuery = "DELETE FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id = ? " + " AND organize_type_id = ?";
        try {
            Enumeration enumOrg = getOrgRelation(con).elements();
            while (enumOrg.hasMoreElements()) {
                or = (OrganizeRelation) enumOrg.nextElement();
                or.doDelete(con);
            }
            Enumeration enumUser = getOrgUserRelation(con).elements();
            while (enumUser.hasMoreElements()) {
                or = (OrganizeRelation) enumUser.nextElement();
                or.doDelete(con);
            }
            ps = con.prepareStatement(strQuery);
            ps.setString(1, String.valueOf(this.organizeID));
            ps.setString(2, this.organizeTypeID);
            int resultCount = ps.executeUpdate();
            if (resultCount != 1) {
                throw new CesSystemException("Organize.delete(con): ERROR deleting data in T_SYS_ORGANIZE!! " + "resultCount = " + resultCount);
            }
        } catch (Exception e) {
            throw new CesSystemException("Organize.delete(con): Exception while deleting organize; " + "organize_id = " + this.organizeID + " :\n\t" + e);
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
	 * ����һ���µ���֯��ţ���֯�������ݿ��������л�ȡ������ͱ�֤��Ψһ��
	 * 
	 * @return ��֯���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public static int getNewOrgID() throws Exception {
        return (int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_SYS_ORGANIZE);
    }

    /**
	 * ���������֯�йص�������֯��ϵ
	 * 
	 * @return ��֯��ϵ����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getOrgRelation(Connection con) throws Exception {
        Vector allOrgRelation = new Vector();
        Organize parentTemp = null;
        Organize childTemp = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT op.organize_id,op.organize_type_id,op.organize_name,op.organize_manager," + "op.organize_describe,op.work_type,op.show_order,op.position_x,op.position_y," + "oc.organize_id,oc.organize_type_id,oc.organize_name,oc.organize_manager," + "oc.organize_describe,oc.work_type,oc.show_order,oc.position_x,oc.position_y " + "FROM " + Common.ORGANIZE_TABLE + " op, " + Common.ORGANIZE_TABLE + " oc, " + Common.ORGANIZE_RELATION_TABLE + " ort " + "WHERE ((ort.organize_id = op.organize_id AND ort.organize_type_id = op.organize_type_id) " + "AND (ort.child_id = oc.organize_id AND ort.child_type_id = oc.organize_type_id)) " + "AND ((ort.organize_id = ? AND ort.organize_type_id = ?) " + "OR (ort.child_id = ? AND ort.child_type_id = ?))";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setString(1, String.valueOf(this.organizeID));
            ps.setString(2, this.organizeTypeID);
            ps.setString(3, String.valueOf(this.organizeID));
            ps.setString(4, this.organizeTypeID);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            while (result.next()) {
                i = 1;
                va.setStart(i);
                parentTemp = Organize.generateOrganize(result, va);
                childTemp = Organize.generateOrganize(result, va);
                allOrgRelation.addElement(new OrganizeRelation(parentTemp, childTemp));
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getOrgRelation(): SQLException while getting all organize_relations where " + "organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            closeResultSet(result);
            closePreparedStatement(ps);
        }
        return allOrgRelation;
    }

    /**
	 * ��ȡ��֯�����е��û�����֯�Ĺ�ϵ����,�����Ӷ������û�
	 * 
	 * @return Vector(Organize_relation) �û�����֯�Ĺ�ϵ����ļ��ϣ��������û�����֯�Ĺ�ϵ��
	 * 
	 * @throws Exception
	 *             Description of the Exception
	 * 
	 */
    public Vector getOrgUserRelation(Connection con) throws Exception {
        Vector userOrgRelation = new Vector();
        Organize parent = null;
        User childUser = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT op.organize_id,op.organize_type_id,op.organize_name,op.organize_manager," + "op.organize_describe,op.work_type,op.show_order,op.position_x,op.position_y," + "uc.user_id,uc.user_name,uc.login_name,uc.flag_emp,uc.user_cryptogram,uc.flag_lock,uc.flag_define," + "uc.ic_no,uc.conn_num,uc.flag_check,uc.flag_active,uc.flag_sa,uc.show_order,uc.position_x,uc.position_y,uc.type, " + "ort.parttime " + " FROM " + Common.ORGANIZE_TABLE + " op, " + Common.USER_TABLE + " uc, " + Common.ORGANIZE_RELATION_TABLE + " ort " + " WHERE ((ort.organize_id = op.organize_id AND ort.organize_type_id = op.organize_type_id) ";
        if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
            strQuery += " AND (ort.child_id = convert(varchar,uc.user_id) ";
        } else {
            strQuery += " AND (ort.child_id = uc.user_id ";
        }
        strQuery += "AND ort.child_type_id = '" + OrganizeType.USER + "'))" + " AND (ort.organize_id = ? AND ort.organize_type_id = ?) ORDER BY op.show_order";
        try {
            ps = con.prepareStatement(strQuery);
            ps.setString(1, String.valueOf(this.organizeID));
            ps.setString(2, this.organizeTypeID);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            while (result.next()) {
                i = 1;
                va.setStart(i);
                parent = Organize.generateOrganize(result, va);
                childUser = User.generateUser(result, va);
                String partTime = result.getString(va.next());
                childUser.setPartTime(partTime);
                parent.setPartTime(partTime);
                userOrgRelation.addElement(new OrganizeRelation(parent, childUser));
            }
            va = null;
        } catch (Exception se) {
            throw new CesSystemException("Organize_relation.getAllUserOrgRelation(): SQLException:  " + se);
        } finally {
            closeResultSet(result);
            closePreparedStatement(ps);
        }
        return userOrgRelation;
    }

    /**
	 * ���ظ�ϵͳ�е����й�������֯
	 * 
	 * @return ������֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getIsolatedOrganize() throws Exception {
        Vector allIsolatedOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " " + "WHERE (organize_id,organize_type_id) NOT IN " + "(SELECT organize_id,organize_type_id FROM " + Common.ORGANIZE_RELATION_TABLE + ") " + "AND (organize_id,organize_type_id) NOT IN " + "(SELECT child_id,child_type_id FROM " + Common.ORGANIZE_RELATION_TABLE + ")" + " ORDER BY show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allIsolatedOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getIsolatedOrganize(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allIsolatedOrg;
    }

    /**
	 * ���ظ�ϵͳ�е�����������֯�����������֯��
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getTopOrganize() throws Exception {
        return new Vector();
    }

    /**
	 * ��ȡ��ݿ������е���֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getAllOrg() throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " ORDER BY show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getAllOrg(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��ݿ������е�����֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getChildOrg(int parentId) throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id IN(SELECT child_id FROM " + Common.ORGANIZE_RELATION_TABLE + "" + " WHERE organize_id = '" + parentId + "' and child_type_id <> '0000')  ORDER BY show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��ݿ������е�����֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getChild(int parentId) throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id IN(SELECT a.child_id FROM (SELECT child_id FROM " + Common.ORGANIZE_RELATION_TABLE + " WHERE organize_id = '" + parentId + "')a,t_sys_organize_relation b where a.child_id = b.organize_id AND b.child_type_id!='0000')  ORDER BY show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��֯���������е�����֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getChildType(String childId) throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = " SELECT child_organize_type FROM " + Common.ORGANIZE_TYPE_RELATION_TABLE + " WHERE '" + childId + "' in (select parent_organize_type from t_sys_organize_type_relation where child_organize_type != '" + OrganizeType.USER + "') ";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            while (result.next()) {
                String[] Obj = new String[1];
                Obj[0] = (String) result.getObject(1);
                allOrg.addElement(Obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��ݿ������е�����֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getOrg(int orgId) throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id = " + orgId + " ";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��ݿ������е�����֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getOtherOrg(int orgId) throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id != " + orgId + " ";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��ݿ������е�����֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getParentOrg() throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id='" + Organize.TOP + "' ORDER BY show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getAllOrg(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��ݿ������е�����֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getDuliOrg() throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " WHERE organize_id='" + Organize.selfTOP + "' ";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getAllOrg(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��ݿ�ָ�������µ���֯
	 * 
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getSpecialOrg(String orgTypeID) throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " " + "WHERE organize_type_id='" + orgTypeID + "' " + "ORDER BY show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getSpecialOrg(String orgTypeID): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ��ȡ��֯��ϵ���еľ����ض���֯���͵�����֯
	 * 
	 * @param strOrganizeType
	 *            ��֯����
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getSpecialChildOrg(String strOrganizeType) throws Exception {
        Vector allOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT ot.organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " ot, " + Common.ORGANIZE_RELATION_TABLE + " ort " + "WHERE ort.child_id = ot.organize_id " + "AND ort.child_type_id = '" + strOrganizeType + "' " + " ORDER BY ot.show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getSpecialChildOrg(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return allOrg;
    }

    /**
	 * ���ص�ǰӦ�õ�λ�������֯���ͣ���˵���������֯������Ψһ��
	 * 
	 * @return ��֯����
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Organize getUnit() throws Exception {
        Organize org = null;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT organize_id,organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " " + "WHERE organize_type_id = '" + OrganizeType.UNIT + "' " + "AND organize_id = '" + Organize.TOP + "'";
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
                org = Organize.generateOrganize(result, va);
            }
            va = null;
            return org;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getUnit(): SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
    }

    /**
	 * ��̬����:ϵͳ��ʼ����ǰӦ�õ�λ
	 * 
	 * @param unit
	 *            �����֯����
	 * @throws Exception
	 *             �����һ���֤��������⣬���׳��쳣
	 */
    public static void doUpdateOrNewUnit(Organize unit) throws Exception {
        if (unit.isUnit()) {
            if (unit.isExist() && unit.isValidate()) {
                unit.doUpdate();
            } else {
                unit.doNew();
            }
        } else {
            throw new CesSystemException("Organize.doUpdateOrNewUnit(): This Organize is not a top unit!");
        }
    }

    /**
	 * ��ȡ����֯���������ָ�����͵��¼���֯����
	 * 
	 * @param strOrgType
	 *            ָ���¼���֯����
	 * @return ��֯����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getJuniorOrg(String strOrgType) throws Exception {
        Vector allChildOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT ot.organize_id,ot.organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " ot, " + Common.ORGANIZE_RELATION_TABLE + " ort " + "WHERE ot.organize_id = ort.child_id " + "AND ort.organize_id = '" + this.organizeID + "' " + "AND ort.organize_type_id = '" + this.organizeTypeID + "' " + "AND ort.child_type_id = '" + strOrgType + "'" + " ORDER BY show_order";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allChildOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getJuniorOrg(): SQLException while getting inferior organize " + "where superior organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return allChildOrg;
    }

    /**
	 * ��ȡ����֯����������û�����,������ְ�û���������������֯�е��û���
	 * 
	 * @return �û�����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getJuniorUser() throws Exception {
        Vector allChildUser = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT uc.user_id,uc.user_name,uc.login_name,uc.flag_emp,uc.user_cryptogram,uc.flag_lock,uc.flag_define," + "uc.ic_no,uc.conn_num,uc.flag_check,uc.flag_active,uc.flag_sa,uc.show_order,uc.position_x,uc.position_y,uc.type," + "ort.parttime,ort.user_order " + "FROM " + Common.USER_TABLE + " uc, " + Common.ORGANIZE_RELATION_TABLE + " ort ";
        if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
            strQuery += "WHERE ort.child_id = convert(varchar,uc.user_id) ";
        } else {
            strQuery += "WHERE ort.child_id = uc.user_id ";
        }
        strQuery += "AND ort.organize_id = '" + this.organizeID + "' " + "AND ort.organize_type_id = '" + this.organizeTypeID + "' " + "AND ort.child_type_id = '" + OrganizeType.USER + "'" + " ORDER BY ort.user_order";
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
                User childUser = User.generateOrganizeUser(result, va);
                allChildUser.addElement(childUser);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getJuniorUser(): SQLException while getting inferior user " + "where superior organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return allChildUser;
    }

    /**
	 * ��ȡ����֯����������û�����,������ְ�û���������������֯�е��û���
	 * 
	 * @return �û�����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getJuniorUser(String condition) throws Exception {
        Vector allChildUser = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT uc.user_id,uc.user_name,uc.login_name,uc.flag_emp,uc.user_cryptogram,uc.flag_lock,uc.flag_define," + "uc.ic_no,uc.conn_num,uc.flag_check,uc.flag_active,uc.flag_sa,uc.show_order,uc.position_x,uc.position_y,uc.type," + "ort.parttime,ort.user_order " + "FROM " + Common.USER_TABLE + " uc, " + Common.ORGANIZE_RELATION_TABLE + " ort " + "WHERE ort.child_id = uc.user_id " + "AND ort.organize_id = '" + this.organizeID + "' " + "AND ort.organize_type_id = '" + this.organizeTypeID + "' " + "AND ort.child_type_id = '" + OrganizeType.USER + "'" + " " + condition + "" + " ORDER BY ort.user_order";
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
                User childUser = User.generateOrganizeUser(result, va);
                allChildUser.addElement(childUser);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getJuniorUser(): SQLException while getting inferior user " + "where superior organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return allChildUser;
    }

    /**
	 * ��ȡ����֯����������û�����,������ְ�û���������������֯�е��û���
	 * 
	 * @param con
	 *            Connection ϵͳ����ƽ̨��ݿ�����
	 * @return �û�����ļ���
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getJuniorUser(Connection con) throws Exception {
        Vector allChildUser = new Vector();
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT uc.user_id,uc.user_name,uc.login_name,uc.flag_emp,uc.user_cryptogram,uc.flag_lock,uc.flag_define," + "uc.ic_no,uc.conn_num,uc.flag_check,uc.flag_active,uc.flag_sa,uc.show_order,uc.position_x,uc.position_y,uc.type," + "ort.parttime " + "FROM " + Common.USER_TABLE + " uc, " + Common.ORGANIZE_RELATION_TABLE + " ort ";
        if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
            strQuery += "WHERE ort.child_id = convert(varchar,uc.user_id) ";
        } else {
            strQuery += "WHERE ort.child_id = uc.user_id ";
        }
        strQuery += "AND ort.organize_id = '" + this.organizeID + "' " + "AND ort.organize_type_id = '" + this.organizeTypeID + "' " + "AND ort.child_type_id = '" + OrganizeType.USER + "'" + " ORDER BY uc.show_order";
        try {
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            while (result.next()) {
                i = 1;
                va.setStart(i);
                User childUser = User.generateUser(result, va);
                String partTime = result.getString(va.next());
                childUser.setPartTime(partTime);
                allChildUser.addElement(childUser);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getJuniorUser(Connection con): SQLException while getting inferior user " + "where superior organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            close(null, ps, result);
        }
        return allChildUser;
    }

    /**
	 * ��ȡ��֯�µ��û�
	 * 
	 * @return ������֯�µ��û�
	 * @throws java.lang.Exception
	 */
    public Vector getUserOfOrg() throws Exception {
        Vector vUser = null;
        try {
            Vector allUser = getJuniorUser();
            for (int i = 0; i < allUser.size(); i++) {
                User uTemp = (User) allUser.get(i);
                if (uTemp.getPartTime().equals("no")) {
                    vUser.addElement(uTemp);
                }
            }
        } catch (Exception e) {
            throw new CesSystemException("Organize.getUserOfOrg(): Exception:  " + e);
        }
        return vUser;
    }

    /**
	 * ��ȡ��֯�µļ�ְ�û�
	 * 
	 * @return ������֯�µļ�ְ�û�
	 * @throws java.lang.Exception
	 */
    public Vector getPartUserOfOrg() throws Exception {
        Vector vUser = null;
        try {
            Vector allUser = getJuniorUser();
            for (int i = 0; i < allUser.size(); i++) {
                User uTemp = (User) allUser.get(i);
                if (uTemp.getPartTime().equals("yes")) {
                    vUser.addElement(uTemp);
                }
            }
        } catch (Exception e) {
            throw new CesSystemException("Organize.getPartUserOfOrg(): Exception:  " + e);
        }
        return vUser;
    }

    /**
	 * Ϊ����֯��������һ�¼���֯����
	 * 
	 * @param oJuniorOrg
	 *            �¼���֯����
	 * @throws Exception
	 *             ���ʱ�д��󣬽��׳��쳣
	 */
    public void doAddJuniorOrg(Organize oJuniorOrg) throws Exception {
        OrganizeRelation orgRlt = new OrganizeRelation(this, oJuniorOrg);
        orgRlt.doUpdateOrNew();
    }

    /**
	 * ɾ�����֯�������¼���֯����Ĺ�ϵ ���ã�OrganizeRelation.doDelete()
	 * 
	 * @param oJuniorOrg
	 *            �¼���֯����
	 * @throws Exception
	 *             ���ɾ��ʱ�д��󣬽��׳��쳣
	 */
    public void doDeleteJuniorOrg(Organize oJuniorOrg) throws Exception {
        OrganizeRelation orCreate = new OrganizeRelation(this, oJuniorOrg);
        orCreate.doDelete();
    }

    /**
	 * ��֤������֯���е����
	 * 
	 * @return true: ��֤�ɹ� false: ��֤ʧ��
	 */
    protected boolean isValidate() {
        if ((this.organizeID == 0) || (this.organizeTypeID == null) || (this.organizeName == null)) {
            return (false);
        } else {
            return (true);
        }
    }

    /**
	 * ��֤��֯�Ƿ�Ϊ�����֯
	 * 
	 * @return true: �������֯ false: ���������֯
	 */
    private boolean isUnit() {
        if ((this.organizeID == Organize.TOP) && (this.organizeTypeID.equals(OrganizeType.UNIT))) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * ��ɲ�ѯ��֯����
	 * 
	 * @param result
	 *            ��ѯ���
	 * @param v
	 *            ������
	 * @return ������ɵĶ���
	 */
    public static Organize generateOrganize(ResultSet result, ValueAsc v) {
        Organize oTemp = new Organize();
        try {
            oTemp.setOrganizeID(result.getInt(v.next()));
            oTemp.setOrganizeTypeID(result.getString(v.next()));
            oTemp.setOrganizeName(result.getString(v.next()));
            oTemp.setOrganizeManager(result.getString(v.next()));
            oTemp.setOrganizeDescribe(result.getString(v.next()));
            oTemp.setWorkType(result.getString(v.next()));
            oTemp.setShowOrder(result.getInt(v.next()));
            oTemp.setPositionX(result.getInt(v.next()));
            oTemp.setPositionY(result.getInt(v.next()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return oTemp;
    }

    /**
	 * �õ���ǰ��֯��ֱ���ϼ�
	 * 
	 * @return ��֯����
	 */
    public Vector getParent() throws Exception {
        Vector allParentOrg = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT ot.organize_id,ot.organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " ot, " + Common.ORGANIZE_RELATION_TABLE + " ort " + "WHERE ot.organize_id = ort.organize_id " + "AND ot.organize_type_id=ort.organize_type_id " + "AND ort.child_id = '" + this.organizeID + "' " + "AND ort.child_type_id = '" + this.organizeTypeID + "' ";
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
                Organize oTemp = Organize.generateOrganize(result, va);
                allParentOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getParent(): SQLException while getting inferior organize " + "where child organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            close(dbo, ps, result);
        }
        return allParentOrg;
    }

    /**
	 * �ݹ�õ������ϼ���֯
	 * 
	 * @return
	 * @throws Exception
	 */
    public static Vector getAllParents(Organize orgChild) throws Exception {
        Vector vAll = new Vector();
        Vector vParents = orgChild.getParent();
        vAll.addAll(vParents);
        for (int i = 0; i < vParents.size(); i++) {
            Organize org = (Organize) vParents.get(i);
            vAll.addAll(Organize.getAllParents(org));
        }
        return vAll;
    }

    /**
	 * �õ���ǰ��֯��ֱ���¼�
	 * 
	 * @return ��֯����
	 */
    public Vector getChild(Connection con) throws Exception {
        Vector allChildOrg = new Vector();
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT ot.organize_id,ot.organize_type_id,organize_name,organize_manager," + "organize_describe,work_type,show_order,position_x,position_y " + "FROM " + Common.ORGANIZE_TABLE + " ot, " + Common.ORGANIZE_RELATION_TABLE + " ort " + "WHERE ot.organize_id = ort.child_id " + "AND ot.organize_type_id=ort.child_type_id " + "AND ort.organize_id = '" + this.organizeID + "' " + "AND ort.organize_type_id = '" + this.organizeTypeID + "' ";
        try {
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            int i = 1;
            ValueAsc va = new ValueAsc(i);
            while (result.next()) {
                i = 1;
                va.setStart(i);
                Organize oTemp = Organize.generateOrganize(result, va);
                allChildOrg.addElement(oTemp);
            }
            va = null;
        } catch (SQLException se) {
            throw new CesSystemException("Organize.getParent(): SQLException while getting inferior organize " + "where child organize_id = " + this.organizeID + " :\n\t" + se);
        } finally {
            closeResultSet(result);
            closePreparedStatement(ps);
        }
        return allChildOrg;
    }

    /**
	 * �ݹ�õ������¼���֯
	 * 
	 * @return
	 * @throws Exception
	 */
    public static Vector getAllChilds(Organize orgParent, Connection con) throws Exception {
        Vector vAll = new Vector();
        Vector vChilds = orgParent.getChild(con);
        vAll.addAll(vChilds);
        for (int i = 0; i < vChilds.size(); i++) {
            Organize org = (Organize) vChilds.get(i);
            vAll.addAll(Organize.getAllChilds(org, con));
        }
        return vAll;
    }

    /**
	 * ��ȡĳ����֯�µ����е��û�������������֯�µ��û�����Щ�û�����Ϊ������֯�ļ�ְ�û���Ҳ���ܲ��Ǽ�ְ�û�
	 * 
	 * @param orgParent
	 *            Organize �����֯����ȡ����֯�°�������֯�µ������û�
	 * @param con
	 *            Connection ϵͳ����ƽ̨��ݿ�����
	 * @throws Exception
	 *             �����?���׳��쳣
	 * @return Vector �����û����������б�
	 */
    public Vector getAllUser(Connection con) throws Exception {
        Vector vcUser = new Vector();
        Vector vcOrg = new Vector();
        try {
            vcOrg.add(this);
            vcOrg.addAll(getAllChilds(this, con));
            int nNum = vcOrg.size();
            for (int i = 0; i < nNum; i++) {
                Organize org = (Organize) vcOrg.get(i);
                logger.debug("orgID == " + org.getOrganizeID());
                vcUser.addAll(org.getJuniorUser(con));
            }
        } catch (Exception e) {
            throw new CesSystemException("Organize.getAllUser(): SQLException while get all user of an organize " + " :\n\t" + e);
        }
        logger.debug("vcUser.size() == " + vcUser.size());
        return vcUser;
    }

    /**
	 * ��ȡĳ����֯����������֯�������û�������������ְ�û���
	 * 
	 * @param orgParent
	 *            Organize �����֯����ȡ����֯�°�������֯�µ������û�����������ְ�û�
	 * @param con
	 *            Connection ϵͳ����ƽ̨��ݿ�����
	 * @throws Exception
	 *             �����?���׳��쳣
	 * @return Vector �����û����������б�
	 */
    public Vector getAllUserOfOrg(Connection con) throws Exception {
        Vector vcUser = new Vector();
        try {
            Vector allUser = getAllUser(con);
            int nNum = allUser.size();
            for (int i = 0; i < nNum; i++) {
                User uTemp = (User) allUser.get(i);
                if (uTemp.getPartTime().equals("no")) {
                    vcUser.addElement(uTemp);
                }
            }
        } catch (Exception e) {
            throw new CesSystemException("Organize.getAllUserOfOrg(): SQLException while get all user of an organize " + " :\n\t" + e);
        }
        return vcUser;
    }

    /**
	 * �жϵ�ǰ��֯���¼���֯�����Ƿ����û�����
	 * 
	 * @return true:��ǰ��֯���¼��û����� false:��ǰ��֯û���¼��û�����
	 * @throws Exception
	 *             ���������׳��쳣
	 */
    public boolean hasChildUser() {
        boolean flag = false;
        OrganizeType orgType = new OrganizeType(this.organizeTypeID);
        try {
            Vector vChildType = orgType.getChildOrganizeType();
            int nNum = vChildType.size();
            if (nNum > 0) {
                for (int i = 0; i < nNum; i++) {
                    OrganizeType childType = (OrganizeType) vChildType.get(i);
                    if (childType.getOrganizeTypeID().equals(OrganizeType.USER)) {
                        flag = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
        return flag;
    }

    /**
	 * �жϵ�ǰ��֯���¼���֯�����Ƿ��г��û���������֯����
	 * 
	 * @return true:��ǰ��֯�г��û���������֯���� false:��ǰ��֯û�г��û���������֯����
	 * @throws Exception
	 *             ���������׳��쳣
	 */
    public boolean hasChildOrg() {
        boolean flag = false;
        OrganizeType orgType = new OrganizeType(this.organizeTypeID);
        try {
            Vector vChildType = orgType.getChildOrganizeType();
            int nNum = vChildType.size();
            if (nNum > 0) {
                for (int i = 0; i < nNum; i++) {
                    OrganizeType childType = (OrganizeType) vChildType.get(i);
                    if (childType.getOrganizeTypeID().equals(OrganizeType.USER)) {
                        continue;
                    } else {
                        flag = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
        }
        return flag;
    }

    /**
	 * ��֯����ʱǰ��һλ
	 * 
	 * @param showOrder
	 *            ��Ҫ�ƶ�����֯�����
	 * @param orgID
	 *            ��Ҫ�ƶ�����֯ID
	 * @param targetShowOrder
	 *            �ƶ���Ŀ�������
	 * @param targetOrgID
	 *            �ƶ���Ŀ����֯ID
	 * @return
	 */
    public void movePrior(String[] showOrder, String[] orgID, String targetShowOrder, String targetOrgID) throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        int moveCount = showOrder.length;
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        String strQuery = "select show_order from " + Common.ORGANIZE_TABLE + " where show_order=" + showOrder[moveCount - 1] + " and organize_id= '" + orgID[moveCount - 1] + "'";
        try {
            con = dbo.getConnection();
            con.setAutoCommit(false);
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            int maxOrderNo = 0;
            if (result.next()) {
                maxOrderNo = result.getInt(1);
            }
            String[] sqls = new String[moveCount + 1];
            sqls[0] = "update " + Common.ORGANIZE_TABLE + " set show_order=" + maxOrderNo + " where show_order=" + targetShowOrder + " and organize_id= '" + targetOrgID + "'";
            for (int i = 0; i < showOrder.length; i++) {
                sqls[i + 1] = "update " + Common.ORGANIZE_TABLE + " set show_order=show_order-1" + " where show_order=" + showOrder[i] + " and organize_id= '" + orgID[i] + "'";
            }
            for (int j = 0; j < sqls.length; j++) {
                ps = con.prepareStatement(sqls[j]);
                int resultCount = ps.executeUpdate();
                if (resultCount != 1) {
                    throw new CesSystemException("Organize.movePrior(): ERROR Inserting data " + "in T_SYS_ORGANIZE update !! resultCount = " + resultCount);
                }
            }
            con.commit();
        } catch (SQLException se) {
            if (con != null) {
                con.rollback();
            }
            throw new CesSystemException("Organize.movePrior(): SQLException while mov organize order " + " :\n\t" + se);
        } finally {
            con.setAutoCommit(true);
            close(dbo, ps, result);
        }
    }

    /**
	 * ��֯����ʱ�����һλ
	 * 
	 * @param showOrder
	 *            ��Ҫ�ƶ�����֯�����
	 * @param orgID
	 *            ��Ҫ�ƶ�����֯ID
	 * @param targetShowOrder
	 *            �ƶ���Ŀ�������
	 * @param targetOrgID
	 *            �ƶ���Ŀ����֯ID
	 * @return
	 */
    public void moveNext(String[] showOrder, String[] orgID, String targetShowOrder, String targetOrgID) throws Exception {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        int moveCount = showOrder.length;
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        String strQuery = "select show_order from " + Common.ORGANIZE_TABLE + " where show_order=" + showOrder[0] + " and organize_id= '" + orgID[0] + "'";
        try {
            con = dbo.getConnection();
            con.setAutoCommit(false);
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            int minOrderNo = 0;
            if (result.next()) {
                minOrderNo = result.getInt(1);
            }
            String[] sqls = new String[moveCount + 1];
            sqls[0] = "update " + Common.ORGANIZE_TABLE + " set show_order=" + minOrderNo + " where show_order=" + targetShowOrder + " and organize_id= '" + targetOrgID + "'";
            for (int i = 0; i < showOrder.length; i++) {
                sqls[i + 1] = "update " + Common.ORGANIZE_TABLE + " set show_order=show_order+1" + " where show_order=" + showOrder[i] + " and organize_id= '" + orgID[i] + "'";
            }
            for (int j = 0; j < sqls.length; j++) {
                ps = con.prepareStatement(sqls[j]);
                int resultCount = ps.executeUpdate();
                if (resultCount != 1) {
                    throw new CesSystemException("Organize.moveNext(): ERROR Inserting data " + "in T_SYS_ORGANIZE update !! resultCount = " + resultCount);
                }
            }
            con.commit();
        } catch (SQLException se) {
            if (con != null) {
                con.rollback();
            }
            throw new CesSystemException("Organize.moveNext(): SQLException while mov organize order " + " :\n\t" + se);
        } finally {
            con.setAutoCommit(true);
            close(dbo, ps, result);
        }
    }

    /**
	 * ��������ѯ�û����ɫ������Ϣ
	 * 
	 * @param strCondition
	 *            ��ѯ����
	 * @return ��ɫ������󼯺�
	 * @throws Exception
	 *             �����������⣬���׳��쳣
	 */
    public Vector getPerson(String authId) throws Exception {
        Vector vec = new Vector();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        String strQuery = "SELECT c.user_name,c.login_name,d.organize_name " + "FROM (SELECT a.user_id,a.user_name,a.login_name,b.organize_id " + "FROM " + Common.USER_TABLE + " A," + Common.ORGANIZE_RELATION_TABLE + " B WHERE a.user_id = b.child_id " + "AND a.user_id in ((select user_id from (select operator_id FROM " + Common.AUTHORITY_ASSIGN_TABLE + " " + "WHERE authority_id = '2002')))) C," + Common.ORGANIZE_TABLE + " D WHETE c.organize_id = d.organize_id";
        DBOperation dbo = factory.createDBOperation(POOL_NAME);
        try {
            con = dbo.getConnection();
            ps = con.prepareStatement(strQuery);
            result = ps.executeQuery();
            while (result.next()) {
                String[] obj = new String[3];
                obj[0] = (String) result.getObject(1);
                obj[1] = (String) result.getObject(2);
                obj[2] = (String) result.getObject(3);
                vec.add(obj);
            }
        } catch (SQLException se) {
            throw new CesSystemException(" SQLException:  " + se);
        } finally {
            close(dbo, ps, result);
        }
        return vec;
    }
}
