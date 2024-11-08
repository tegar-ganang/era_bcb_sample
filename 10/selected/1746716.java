package com.hotye.school.jbean;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.hotye.school.dao.DaoBase;
import com.hotye.school.dbmgr.DBConnect;
import com.hotye.school.server.ServerUtil;
import com.hotye.school.util.ApplicationException;
import com.hotye.school.util.GetDate;
import com.hotye.school.util.StrFun;

public class RegisterDao extends DaoBase {

    public void addPojo(HttpServletRequest request) throws ApplicationException {
        Register pd = new Register();
        pd.regNo = StrFun.getStringMust(request, "regNo");
        pd.regName = StrFun.getStringMust(request, "regName");
        pd.provision = StrFun.getString(request, "provision");
        pd.userNo = ServerUtil.getUserNoFS(request);
        String sql = "insert into SS_Register " + "(regNo, regName, provision, userNo) " + "values('" + pd.regNo + "', '" + pd.regName + "', '" + pd.provision + "', '" + pd.userNo + "')";
        executeUpdate(sql);
    }

    public void deletePojo(HttpServletRequest request) {
        Register pd = new Register();
        pd.id = StrFun.getInt(request, "id");
        pd.userNo = ServerUtil.getUserNoFS(request);
        Register register = findPojoById(pd.id + "", Register.class);
        if (register.audit) {
            throw new ApplicationException(register.regNo + "����˲���ɾ��");
        }
        String sql = "Select * from SS_RegisterInfo where rid=" + pd.id;
        List<RegisterInfo> list = findList(sql, RegisterInfo.class);
        for (RegisterInfo ri : list) {
            sql = "update SS_RegisterInfo set userNo='" + pd.userNo + "', delStatus=1 where id=" + ri.id;
            executeUpdate(sql);
        }
        sql = "update SS_Register set " + "userNo='" + pd.userNo + "', " + "delStatus=1 " + "where id=" + pd.id;
        executeUpdate(sql);
    }

    public void editPojo(HttpServletRequest request) {
        Register pd = new Register();
        pd.id = StrFun.getInt(request, "id");
        pd.regNo = StrFun.getString(request, "regNo");
        pd.regName = StrFun.getString(request, "regName");
        pd.provision = StrFun.getString(request, "provision");
        pd.userNo = ServerUtil.getUserNoFS(request);
        String sql = "update SS_Register set " + "regNo='" + pd.regNo + "', " + "regName='" + pd.regName + "', " + "provision='" + pd.provision + "', " + "userNo='" + pd.userNo + "' " + "where id=" + pd.id;
        executeUpdate(sql);
    }

    /**
	 * ��˽����
	 * @param request
	 */
    public void auditRegister(HttpServletRequest request) {
        Register reg = new Register();
        reg.id = StrFun.getInt(request, "rid");
        Register register = findPojoById(reg.id + "", Register.class);
        if (register.audit) throw new ApplicationException("�Ѿ���˹����ٴ����!");
        RegisterInfoDao infoDao = new RegisterInfoDao();
        List<RegisterInfo> list = infoDao.findListByRid("" + reg.id);
        if (list == null || list.size() <= 0) throw new ApplicationException("��������������Ϣ���ݲ�����ˣ�");
        DBConnect ddc = null;
        Connection conn = null;
        try {
            ddc = DBConnect.createDBConnect();
            conn = ddc.getConnection();
            conn.setAutoCommit(false);
            for (RegisterInfo rInfo : list) {
                String sql = "update SS_Product set " + "productAmount=productAmount+" + rInfo.regAmount + ", " + "productStorage=productStorage+" + rInfo.regAmount + ", " + "productPrice=" + rInfo.regPrice + ", " + "productSalePrice=" + rInfo.regSalePrice + " " + "where id=" + rInfo.pid;
                conn.createStatement().executeUpdate(sql);
            }
            String sql = "update SS_Register set audit=1, auditDate='" + GetDate.dateToStr(new Date()) + "' where id=" + reg.id;
            conn.createStatement().executeUpdate(sql);
            conn.commit();
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            throw new ApplicationException("���ʧ�ܣ�", e);
        } finally {
            if (ddc != null) {
                try {
                    ddc.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
