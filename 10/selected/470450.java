package com.sheng.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.sheng.datasource.Pool;
import com.sheng.po.Addwuliao;

public class InsertwuliaoDaoImpl implements InsertwuliaoDAO {

    public boolean addwuliao(Addwuliao aw) {
        Connection conn = null;
        PreparedStatement pm = null;
        PreparedStatement pm2 = null;
        boolean flag = false;
        try {
            conn = Pool.getConnection();
            conn.setAutoCommit(false);
            pm = conn.prepareStatement("insert addwuliao(pid,inname,innum,inprice,inuserid,indate,productsdetail)values(?,?,?,?,?,?,?)");
            pm2 = conn.prepareStatement("insert addwuliaobeifen(pid,inname,innum,inprice,inuserid,indate,productsdetail)values(?,?,?,?,?,?,?)");
            pm.setString(1, aw.getPid());
            pm.setString(2, aw.getInname());
            pm.setInt(3, aw.getInnum());
            pm.setDouble(4, aw.getInprice());
            pm.setString(5, aw.getInuserid());
            pm.setString(6, aw.getIndate());
            pm.setString(7, aw.getProductsdetail());
            pm2.setString(1, aw.getPid());
            pm2.setString(2, aw.getInname());
            pm2.setInt(3, aw.getInnum());
            pm2.setDouble(4, aw.getInprice());
            pm2.setString(5, aw.getInuserid());
            pm2.setString(6, aw.getIndate());
            pm2.setString(7, aw.getProductsdetail());
            pm.execute();
            pm2.execute();
            conn.commit();
            flag = true;
            Pool.close(pm);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
            try {
                conn.rollback();
            } catch (Exception ep) {
                ep.printStackTrace();
            }
            Pool.close(pm);
            Pool.close(conn);
        } finally {
            Pool.close(pm);
            Pool.close(conn);
        }
        return flag;
    }

    public int updatewuliao(Addwuliao aw) {
        int flag = 0;
        Connection conn = null;
        PreparedStatement pm = null;
        try {
            conn = Pool.getConnection();
            conn.setAutoCommit(false);
            pm = conn.prepareStatement("update addwuliao set inname=?,innum=?,inprice=?,productsdetail=?where pid=?");
            pm.setString(1, aw.getInname());
            pm.setInt(2, aw.getInnum());
            pm.setDouble(3, aw.getInprice());
            pm.setString(4, aw.getProductsdetail());
            pm.setString(5, aw.getPid());
            flag = pm.executeUpdate();
            conn.commit();
            Pool.close(pm);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (Exception ep) {
                ep.printStackTrace();
            }
            Pool.close(pm);
            Pool.close(conn);
        } finally {
            Pool.close(pm);
            Pool.close(conn);
        }
        return flag;
    }

    public int findpid(String id) {
        int i = 1;
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select * from addwuliao where pid=?");
            pm.setString(1, id);
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    i = 0;
                }
            }
            Pool.close(rs);
            Pool.close(pm);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            Pool.close(rs);
            Pool.close(pm);
            Pool.close(conn);
        } finally {
            Pool.close(rs);
            Pool.close(pm);
            Pool.close(conn);
        }
        return i;
    }
}
