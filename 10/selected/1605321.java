package com.store;

import com.jedi.BaseObj;
import com.tss.util.DbConn;
import com.tss.util.DbRs;

public class Company extends BaseObj {

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "";
            DbRs rs = null;
            sql = "select * from companyinfo where comname = ?";
            conn.prepare(sql);
            conn.setString(1, getComnam());
            rs = conn.executeQuery();
            if (rs != null && rs.size() > 0) {
                setErr("此厂商名称已经存在");
                return;
            }
            sql = "insert into companyinfo(comname,comtel," + "comaddress,comremark) values(?,?,?,?)";
            conn.prepare(sql);
            conn.setString(1, getComnam());
            conn.setString(2, getComtel());
            conn.setString(3, getComaddress());
            conn.setString(4, getComremark());
            conn.executeUpdate();
            sql = "select  lastval() ";
            conn.prepare(sql);
            rs = conn.executeQuery();
            int lastid = rs.getInt(0, 0);
            Companysale comsale = getComsale();
            if (!comsale.getSalename().trim().equals("")) {
                sql = "insert into companysales(salename,comid,saletel,salemail," + "remark) values(?,?,?,?,?)";
                conn.prepare(sql);
                conn.setString(1, comsale.getSalename());
                conn.setInt(2, lastid);
                conn.setString(3, comsale.getSaletel());
                conn.setString(4, comsale.getSalemail());
                conn.setString(5, comsale.getRemark());
                conn.executeUpdate();
            }
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public void update() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "";
            sql = "update companyinfo set comname =? ,comman=?,comaddress=?, comtel=? ,commantel=?,commanmail=?,comremark=?  where comid = ?";
            conn.prepare(sql);
            conn.setString(1, getComnam());
            conn.setString(2, getComman());
            conn.setString(3, getComaddress());
            conn.setString(4, getComtel());
            conn.setString(5, getCommantel());
            conn.setString(6, getCommanmail());
            conn.setString(7, getComremark());
            conn.setInt(8, getComid());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    private int comid;

    private String comnam = "";

    private String comtel = "";

    private String comman = "";

    private String commantel = "";

    private String commanmail = "";

    private String createtime = "";

    private String comaddress = "";

    private String comremark = "";

    private Companysale comsale;

    private int id;

    private String id_s = "";

    public Company() {
    }

    public Company(int id) {
        this.id = id;
    }

    public Company(String id) {
        this.id_s = id;
    }

    public int getId() {
        return id;
    }

    public int getComid() {
        return comid;
    }

    public void setComid(int comid) {
        this.comid = comid;
    }

    public String getComman() {
        return comman;
    }

    public void setComman(String comman) {
        this.comman = comman;
    }

    public String getCommanmail() {
        return commanmail;
    }

    public void setCommanmail(String commanmail) {
        this.commanmail = commanmail;
    }

    public String getCommantel() {
        return commantel;
    }

    public void setCommantel(String commantel) {
        this.commantel = commantel;
    }

    public String getComnam() {
        return comnam;
    }

    public void setComnam(String comnam) {
        this.comnam = comnam;
    }

    public String getComtel() {
        return comtel;
    }

    public void setComtel(String comtel) {
        this.comtel = comtel;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public String getComaddress() {
        return comaddress;
    }

    public void setComaddress(String comaddress) {
        this.comaddress = comaddress;
    }

    public String getComremark() {
        return comremark;
    }

    public void setComremark(String comremark) {
        this.comremark = comremark;
    }

    public Companysale getComsale() {
        return comsale;
    }

    public void setComsale(Companysale comsale) {
        this.comsale = comsale;
    }
}
