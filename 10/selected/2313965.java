package com.store;

import java.sql.Date;
import com.jedi.BaseObj;
import com.tss.util.DbConn;
import com.tss.util.DbRs;

/**
 * @author wevjoso
 * 
 */
public class Pal extends BaseObj {

    String id = "";

    public Pal() {
    }

    public Pal(String id) {
        this.id = id;
    }

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "";
            DbRs rs = null;
            sql = "select * from pal where taskid = ?";
            conn.prepare(sql);
            conn.setInt(1, getTaskid());
            rs = conn.executeQuery();
            if (rs != null && rs.size() > 0) {
                setErr("此工单已经做过损益分析");
                return;
            }
            sql = "insert into pal(taskid," + "createman,y,m,monthin) values(?, ?,?,?,?)";
            conn.prepare(sql);
            conn.setInt(1, getTaskid());
            conn.setString(2, getCreateman());
            conn.setInt(3, getY());
            conn.setInt(4, getM());
            conn.setInt(5, getMonthin());
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

    public boolean isAnalysis(int taskid) {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            DbRs rs = null;
            sql = "select * from pal where taskid = ?";
            conn.prepare(sql);
            conn.setInt(1, taskid);
            rs = conn.executeQuery();
            if (rs.size() > 0) {
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            conn.close();
        }
        return false;
    }

    public void update() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "update pal  set spartfee=?,outefee=?,tripfee=?,createman=?,y=?,m =?,monthin=? where taskid=?";
            conn.prepare(sql);
            conn.setString(4, getCreateman());
            conn.setInt(5, getY());
            conn.setInt(6, getM());
            conn.setInt(7, getMonthin());
            conn.setInt(8, getTaskid());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    private int taskid = 0;

    private int worktime = 0;

    private double spartfee = 0.00;

    private double outefee = 0.00;

    private double tripfee = 0.00;

    private Date createtime = null;

    private String createman = "";

    private int y = 0;

    private int m = 0;

    private int monthin = 1;

    public String getCreateman() {
        return createman;
    }

    public void setCreateman(String createman) {
        this.createman = createman;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public int getM() {
        return m;
    }

    public void setM(int m) {
        this.m = m;
    }

    public double getOutefee() {
        return outefee;
    }

    public void setOutefee(double outefee) {
        this.outefee = outefee;
    }

    public double getSpartfee() {
        return spartfee;
    }

    public void setSpartfee(double spartfee) {
        this.spartfee = spartfee;
    }

    public int getTaskid() {
        return taskid;
    }

    public void setTaskid(int taskid) {
        this.taskid = taskid;
    }

    public double getTripfee() {
        return tripfee;
    }

    public void setTripfee(double tripfee) {
        this.tripfee = tripfee;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getMonthin() {
        return monthin;
    }

    public void setMonthin(int monthin) {
        this.monthin = monthin;
    }

    public int getWorktime() {
        return worktime;
    }

    public void setWorktime(int worktime) {
        this.worktime = worktime;
    }
}
