package com.hotye.school.jbean;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.hotye.school.dao.DaoBase;
import com.hotye.school.dbmgr.DBConnect;
import com.hotye.school.server.ServerUtil;
import com.hotye.school.util.ApplicationException;
import com.hotye.school.util.StrFun;

public class RegisterInfoDao extends DaoBase {

    public void addRegisterInfo(HttpServletRequest request) throws ApplicationException {
        String[] pids = request.getParameterValues("pid");
        if (pids == null || pids.length <= 0) throw new ApplicationException("��ѡ��Ҫ���Ĳ�Ʒ");
        RegisterDao registerDao = new RegisterDao();
        Register register = registerDao.findPojoById(StrFun.getString(request, "rid"), Register.class);
        if (register.audit) throw new ApplicationException("��������Ѿ���ˣ��������µ���Ʒ");
        DBConnect dbc = null;
        Connection conn = null;
        try {
            dbc = DBConnect.createDBConnect();
            conn = dbc.getConnection();
            conn.setAutoCommit(false);
            for (String pid : pids) {
                RegisterInfo pd = new RegisterInfo();
                pd.rid = StrFun.getInt(request, "rid");
                pd.pid = Integer.parseInt(pid);
                pd.productName = StrFun.getString(request, "productName_" + pid);
                pd.regAmount = StrFun.getInt(request, "regAmount_" + pid);
                pd.regPrice = StrFun.getInt(request, "regPrice_" + pid);
                pd.regSalePrice = StrFun.getInt(request, "regSalePrice_" + pid);
                pd.userNo = ServerUtil.getUserFromSession(request).userNo;
                if (pd.regAmount <= 0) throw new ApplicationException("�����������Ϊ��");
                String sql = "insert into SS_RegisterInfo " + "(pid, rid, productName, regAmount, regPrice, regSalePrice, userNo) " + "values(" + "'" + pd.pid + "', " + "'" + pd.rid + "', " + "'" + pd.productName + "', " + "'" + pd.regAmount + "', " + "'" + pd.regPrice + "', " + "'" + pd.regSalePrice + "', " + "'" + pd.userNo + "' " + ")";
                conn.createStatement().executeUpdate(sql);
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
            }
            throw new ApplicationException(e.getMessage(), e);
        } finally {
            if (dbc != null) try {
                dbc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deletePojo(HttpServletRequest request) throws ApplicationException {
        int id = StrFun.getInt(request, "id");
        String sql = "select sr.* from SS_Register as sr, SS_RegisterInfo as sri where sr.id=sri.rid and sr.audit=1 and sri.id=" + id;
        List<Register> list = findList(sql, Register.class);
        if (list.isEmpty()) deletePojo(request, RegisterInfo.class); else throw new ApplicationException("����������,������ɾ��!");
    }

    /**
	 * ���������Ų�������嵥����ϸ��Ʒ��Ϣ
	 * @param rid
	 * @return
	 */
    public List<RegisterInfo> findListByRid(String rid) {
        String sql = "select * from SS_RegisterInfo where delStatus=0 and rid=" + rid;
        return findList(sql, RegisterInfo.class);
    }
}
