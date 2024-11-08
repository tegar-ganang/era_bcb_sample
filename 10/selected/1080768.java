package com.sheng.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.sheng.datasource.Pool;
import com.sheng.po.Outwuliao;

public class DelwuliaoDaoImpl implements DelwuliaoDAO {

    public boolean checkwuliaonameexist(String pid) {
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        boolean flag = false;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select * from addwuliao where pid=?");
            pm.setString(1, pid);
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    flag = true;
                }
            } else {
                flag = false;
            }
            Pool.close(rs);
            Pool.close(pm);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
            Pool.close(rs);
            Pool.close(pm);
            Pool.close(conn);
        } finally {
            Pool.close(rs);
            Pool.close(pm);
            Pool.close(conn);
        }
        return flag;
    }

    public int getnum(String pid) {
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        int a = 0;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select innum from addwuliao where pid=?");
            pm.setString(1, pid);
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    a = rs.getInt("innum");
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
        return a;
    }

    public boolean savedel(Outwuliao ow) {
        Connection conn = null;
        PreparedStatement pm = null;
        boolean flag = false;
        try {
            conn = Pool.getConnection();
            conn.setAutoCommit(false);
            pm = conn.prepareStatement("insert into delwuliao(pid,outname,outnum,outprice,outuserid,outdate,maori,sumsales,summaori,purchaser)values(?,?,?,?,?,?,?,?,?,?)");
            pm.setString(1, ow.getPid());
            pm.setString(2, ow.getOutname());
            pm.setInt(3, ow.getOutnum());
            pm.setDouble(4, ow.getOutprice());
            pm.setString(5, ow.getOutuserid());
            pm.setString(6, ow.getOutdate());
            pm.setDouble(7, ow.getMaori());
            pm.setDouble(8, ow.getSumsales());
            pm.setDouble(9, ow.getSummaori());
            pm.setString(10, ow.getPurchaser());
            flag = pm.execute();
            conn.commit();
            Pool.close(pm);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            Pool.close(pm);
            Pool.close(conn);
        } finally {
            Pool.close(pm);
            Pool.close(conn);
        }
        return flag;
    }

    public boolean updatenum(int num, String pid) {
        boolean flag = false;
        Connection conn = null;
        PreparedStatement pm = null;
        try {
            conn = Pool.getConnection();
            conn.setAutoCommit(false);
            pm = conn.prepareStatement("update addwuliao set innum=? where pid=?");
            pm.setInt(1, num);
            pm.setString(2, pid);
            int a = pm.executeUpdate();
            if (a == 0) {
                flag = false;
            } else {
                flag = true;
            }
            conn.commit();
            Pool.close(pm);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            Pool.close(pm);
            Pool.close(conn);
        } finally {
            Pool.close(pm);
            Pool.close(conn);
        }
        return flag;
    }

    public boolean delwuliao(String pid) {
        boolean flag = false;
        Connection conn = null;
        PreparedStatement pm = null;
        try {
            conn = Pool.getConnection();
            conn.setAutoCommit(false);
            pm = conn.prepareStatement("delete from addwuliao where pid=?");
            pm.setString(1, pid);
            int x = pm.executeUpdate();
            if (x == 0) {
                flag = false;
            } else {
                flag = true;
            }
            conn.commit();
            Pool.close(pm);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            Pool.close(pm);
            Pool.close(conn);
        } finally {
            Pool.close(pm);
            Pool.close(conn);
        }
        return flag;
    }

    public List<Outwuliao> findallout() {
        List<Outwuliao> ls = new ArrayList<Outwuliao>();
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select * from delwuliao");
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    Outwuliao ow = setproperty(rs);
                    ls.add(ow);
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
        return ls;
    }

    public String getpname(String pid) {
        String message = "";
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select inname from addwuliao where pid=?");
            pm.setString(1, pid);
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    message = rs.getString("inname");
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
        return message;
    }

    public double getprice(String pid) {
        double price = 0.0;
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select inprice from addwuliao where pid=?");
            pm.setString(1, pid);
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    price = rs.getDouble("inprice");
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
        return price;
    }

    public List<Outwuliao> findalloutbypage(int pageSize, int pageNo) {
        List<Outwuliao> ls = new ArrayList<Outwuliao>();
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select * from delwuliao limit " + (pageSize * pageNo - pageSize) + "," + pageSize + "");
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    Outwuliao ow = setproperty(rs);
                    ls.add(ow);
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
        return ls;
    }

    private Outwuliao setproperty(ResultSet rs) throws SQLException {
        Outwuliao ow = new Outwuliao();
        ow.setPid(rs.getString("pid"));
        ow.setPurchaser(rs.getString("purchaser"));
        ow.setMaori(rs.getDouble("maori"));
        ow.setOutname(rs.getString("outname"));
        ow.setOutnum(rs.getInt("outnum"));
        ow.setOutprice(rs.getDouble("outprice"));
        ow.setOutuserid(rs.getString("outuserid"));
        ow.setOutdate(rs.getString("outdate").substring(0, 19));
        return ow;
    }

    public List<Integer> gettotalpage(int pageSize) {
        List<Integer> num = new ArrayList<Integer>();
        Connection conn = null;
        PreparedStatement pm = null;
        ResultSet rs = null;
        try {
            conn = Pool.getConnection();
            pm = conn.prepareStatement("select count(*) from delwuliao");
            rs = pm.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    num.add(rs.getInt(1) / pageSize);
                    num.add(rs.getInt(1) % pageSize);
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
        return num;
    }

    public List<Double> getsumsm() {
        List<Double> ls = new ArrayList<Double>();
        Connection conn = null;
        PreparedStatement pm1 = null;
        PreparedStatement pm2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;
        try {
            conn = Pool.getConnection();
            pm1 = conn.prepareStatement("select sum(outprice) from delwuliao");
            pm2 = conn.prepareStatement("select sum(maori) from delwuliao");
            rs1 = pm1.executeQuery();
            rs2 = pm2.executeQuery();
            if (rs1 != null && rs2 != null) {
                while (rs1.next() && rs2.next()) {
                    ls.add(rs1.getDouble(1));
                    ls.add(rs2.getDouble(1));
                }
            }
            Pool.close(rs1);
            Pool.close(rs2);
            Pool.close(pm1);
            Pool.close(pm2);
            Pool.close(conn);
        } catch (Exception e) {
            e.printStackTrace();
            Pool.close(rs1);
            Pool.close(rs2);
            Pool.close(pm1);
            Pool.close(pm2);
            Pool.close(conn);
        } finally {
            Pool.close(rs1);
            Pool.close(rs2);
            Pool.close(pm1);
            Pool.close(pm2);
            Pool.close(conn);
        }
        return ls;
    }
}
