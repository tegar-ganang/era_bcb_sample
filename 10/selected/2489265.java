package com.store;

import java.sql.Date;
import java.util.List;
import com.jedi.BaseObj;
import com.jedi.User;
import com.tss.util.DbConn;
import com.tss.util.DbRs;

public class Techconeng extends BaseObj {

    public void insert() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            DbRs rs = null;
            sql = "select * from techconeng where conid=? and engid=? and taskid=?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.setString(2, getEngid());
            conn.setInt(3, getTaskid());
            rs = conn.executeQuery();
            if (rs.size() > 0) {
                setErr("请不要在同一合同中添加同一个没有创建合同的工程师");
                return;
            }
            sql = "select th.* from techconeng th " + "left join task t on t.taskid=th.taskid " + " where t.status !=3 and t.conid=? and t.engineerid=?";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.setString(2, getEngid());
            rs = conn.executeQuery();
            if (rs.size() > 0) {
                setErr("此工程师的工单还没有结束您暂时不能分配");
                return;
            }
            sql = "insert into techconeng (conid,engid,taskid) values (?,?,? )";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.setString(2, getEngid());
            conn.setInt(3, getTaskid());
            conn.executeUpdate();
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
            String sql = "update techconeng  set" + " conid = ? , engid = ? , taskid = ? where conid=?";
            conn.prepare(sql);
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public boolean hasTaskRole(Techconeng eng) {
        DbConn conn = new DbConn();
        boolean isRole = false;
        DbRs rs = null;
        try {
            String sql = "select * from techconeng where conid=? and engid=? and taskid=0 ";
            conn.prepare(sql);
            conn.setInt(1, eng.getConid());
            conn.setString(2, eng.getEngid());
            rs = conn.executeQuery();
            if (rs.size() > 0) {
                isRole = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
        return isRole;
    }

    public void delete() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            DbRs rs = null;
            sql = "select * from techconeng where conid = ? and engid = ? and taskid = ? ";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.setString(2, getEngid());
            conn.setInt(3, getTaskid());
            rs = conn.executeQuery();
            if (!(rs == null || rs.size() > 0)) {
                setErr("该信息不存在");
                return;
            }
            sql = "delete from techconeng where conid = ? and engid = ? and  taskid = ? ";
            conn.prepare(sql);
            conn.setInt(1, getConid());
            conn.setString(2, getEngid());
            conn.setInt(3, getTaskid());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    private int conid = 0;

    private String engid = "";

    private int taskid = 0;

    private Date assigntime = null;

    private List listEngs = null;

    private String eng_name = "";

    private String taskStatus_name = "";

    private int taskStatus = 0;

    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getEng_name() {
        return eng_name;
    }

    public void setEng_name(String eng_name) {
        this.eng_name = eng_name;
    }

    public List getListEngs() {
        return listEngs;
    }

    public void setListEngs(List listEngs) {
        this.listEngs = listEngs;
    }

    public Date getAssigntime() {
        return assigntime;
    }

    public void setAssigntime(Date assigntime) {
        this.assigntime = assigntime;
    }

    public int getConid() {
        return conid;
    }

    public void setConid(int conid) {
        this.conid = conid;
    }

    public String getEngid() {
        return engid;
    }

    public void setEngid(String engid) {
        this.engid = engid;
    }

    public int getTaskid() {
        return taskid;
    }

    public void setTaskid(int taskid) {
        this.taskid = taskid;
    }

    public int getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(int taskStatus) {
        this.taskStatus = taskStatus;
    }

    public String getTaskStatus_name() {
        return taskStatus_name;
    }

    public void setTaskStatus_name(String taskStatus_name) {
        this.taskStatus_name = taskStatus_name;
    }
}
