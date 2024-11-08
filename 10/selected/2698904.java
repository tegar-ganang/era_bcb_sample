package com.store;

import com.jedi.BaseObj;
import com.tss.util.DbConn;

public class Companysale extends BaseObj {

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "insert into companysales(salename,comid,saletel,salemail," + "remark) values(?,?,?,?,?)";
            conn.prepare(sql);
            conn.setString(1, getSalename());
            conn.setInt(2, getComid());
            conn.setString(3, getSaletel());
            conn.setString(4, getSalemail());
            conn.setString(5, getRemark());
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

    public void update() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            sql = "update companysales set salename = ? ,comid=?,saletel=?,salemail=? ,remark =? where saleid = ?";
            conn.prepare(sql);
            conn.setString(1, getSalename());
            conn.setInt(2, getComid());
            conn.setString(3, getSaletel());
            conn.setString(4, getSalemail());
            conn.setString(5, getRemark());
            conn.setInt(6, getSaleid());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    private int saleid = 0;

    private String salename = "";

    private int comid = 0;

    private String saletel = "";

    private String salemail = "";

    private String remark = "";

    private String createtime = "";

    private int id = 0;

    private String str_id = "";

    private String comname_str = "";

    public Companysale() {
    }

    public Companysale(int id) {
        this.id = id;
    }

    public Companysale(String id) {
        this.str_id = id;
    }

    public int getComid() {
        return comid;
    }

    public void setComid(int comid) {
        this.comid = comid;
    }

    public String getCreatetime() {
        return createtime;
    }

    public void setCreatetime(String createtime) {
        this.createtime = createtime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getSaleid() {
        return saleid;
    }

    public void setSaleid(int saleid) {
        this.saleid = saleid;
    }

    public String getSalemail() {
        return salemail;
    }

    public void setSalemail(String salemail) {
        this.salemail = salemail;
    }

    public String getSalename() {
        return salename;
    }

    public void setSalename(String salename) {
        this.salename = salename;
    }

    public String getSaletel() {
        return saletel;
    }

    public void setSaletel(String saletel) {
        this.saletel = saletel;
    }

    public String getComname_str() {
        return comname_str;
    }

    public void setComname_str(String comname_str) {
        this.comname_str = comname_str;
    }

    public String getStr_id() {
        return str_id;
    }

    public void setStr_id(String str_id) {
        this.str_id = str_id;
    }
}
