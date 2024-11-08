package com.hotye.school.jbean;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.hotye.school.dbmgr.DBConnect;
import com.hotye.school.server.ServerUtil;
import com.hotye.school.util.GetDate;
import com.hotye.school.util.StrFun;

public class AssetsDao {

    /**
	 * �����û�
	 * @param request
	 * @throws Exception
	 */
    public void addAssets(HttpServletRequest request) throws Exception {
        Assets assets = new Assets();
        assets.assetsName = StrFun.getString(request, "assetsName");
        assets.regAmount = StrFun.getInt(request, "regAmount");
        assets.regPrice = StrFun.getInt(request, "regPrice");
        assets.changeRate = StrFun.getInt(request, "changeRate");
        if (assets.changeRate <= 0) {
            assets.changeRate = 100;
        }
        String sql = "INSERT INTO SS_Assets(assetsName, regAmount, regPrice, changePrice, changeRate, updateAmount)" + " VALUES('" + assets.assetsName + "', '" + assets.regAmount + "', " + " '" + assets.regPrice + "', '" + assets.regPrice + "', '" + assets.changeRate + "', '" + assets.regAmount + "')";
        System.out.println("���ӹ̶��ʲ�SQL:" + sql);
        DBConnect dbConnect = new DBConnect();
        try {
            dbConnect.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dbConnect.close();
        }
    }

    public void deleteCategory(HttpServletRequest request) {
        Category category = new Category();
        category.id = StrFun.getInt(request, "id");
        category.updateDate = new Date();
        String sql = "update SS_Category set " + "delStatus=1, " + "updateDate='" + GetDate.dateToStr(category.updateDate) + "' " + "where id=" + category.id;
        DBConnect dbc = null;
        try {
            dbc = DBConnect.createDBConnect();
            dbc.executeUpdate(sql);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dbc != null) try {
                dbc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void editAssets(HttpServletRequest request) {
        Assets ass = new Assets();
        ass.id = StrFun.getInt(request, "id");
        ass.assetsName = StrFun.getString(request, "assetsName");
        ass.changeRate = StrFun.getInt(request, "changeRate");
        ass.updateAmount = StrFun.getInt(request, "updateAmount");
        ass.changePrice = StrFun.getInt(request, "changePrice");
        String changeRemark = StrFun.getString(request, "changeRemark");
        ass.updateDate = new Date();
        DBConnect dbc = null;
        Connection conn = null;
        try {
            dbc = DBConnect.createDBConnect();
            conn = dbc.getConnection();
            conn.setAutoCommit(false);
            String sql = "update SS_Assets set " + "assetsName='" + ass.assetsName + "', " + "changeRate='" + ass.changeRate + "', " + "updateAmount='" + ass.updateAmount + "', " + "changePrice='" + ass.changePrice + "', " + "updateDate='" + GetDate.dateToStr(ass.updateDate) + "' " + "where id=" + ass.id;
            conn.createStatement().executeUpdate(sql);
            sql = "insert into SS_AssetsLine (aid, changeAmount, changeNum, changePrice, changeRemark, userNo) values(" + "'" + ass.id + "', " + "'" + ass.updateAmount + "', " + "'" + ass.updateAmount + "', " + "'" + ass.changePrice + "', " + "'" + changeRemark + "', " + "'" + ServerUtil.getUserFromSession(request).userNo + "')";
            conn.createStatement().executeUpdate(sql);
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
        } finally {
            if (dbc != null) try {
                dbc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public List<AssetsLine> findLineListByAid(int aid) {
        String sql = "select * from SS_AssetsLine where delStatus=0 and aid=" + aid;
        List<AssetsLine> list = new ArrayList<AssetsLine>();
        DBConnect dbc = null;
        try {
            dbc = DBConnect.createDBConnect();
            ResultSet rs = dbc.executeQuery(sql);
            while (rs != null && rs.next()) {
                AssetsLine assl = new AssetsLine();
                assl.id = rs.getInt("id");
                assl.aid = rs.getInt("aid");
                assl.changeAmount = rs.getInt("changeAmount");
                assl.changeDate = rs.getDate("changeDate");
                assl.changeNum = rs.getInt("changeNum");
                assl.changePrice = rs.getInt("changePrice");
                assl.changeRemark = rs.getString("changeRemark");
                assl.userNo = rs.getString("userNo");
                list.add(assl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dbc != null) try {
                dbc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public Assets findAssetsById(String id) {
        String sql = "select * from SS_Assets where delStatus=0 and id=" + id;
        List<Assets> list = findList(sql);
        if (list.size() > 0) return list.get(0);
        return null;
    }

    public List<Assets> findAssetsList() {
        String sql = "select * from SS_Assets where delStatus=0";
        List<Assets> list = findList(sql);
        return list;
    }

    private List<Assets> findList(String sql) {
        List<Assets> list = new ArrayList<Assets>();
        DBConnect dbc = null;
        try {
            dbc = DBConnect.createDBConnect();
            ResultSet rs = dbc.executeQuery(sql);
            while (rs != null && rs.next()) {
                Assets ass = new Assets();
                ass.id = rs.getInt("id");
                ass.assetsName = rs.getString("assetsName");
                ass.changeRate = rs.getInt("changeRate");
                ass.regAmount = rs.getInt("regAmount");
                ass.updateAmount = rs.getInt("updateAmount");
                ass.updateDate = rs.getDate("updateDate");
                ass.updatePrice = rs.getInt("updatePrice");
                ass.regDate = rs.getDate("regDate");
                ass.regPrice = rs.getInt("regPrice");
                ass.changePrice = rs.getInt("changePrice");
                list.add(ass);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dbc != null) try {
                dbc.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
