package com.store;

import java.util.ArrayList;
import java.util.List;
import com.jedi.BaseObj;
import com.tss.util.DbConn;
import com.tss.util.DbRs;

/**
 * @author wevjoso
 *
 */
public class Sevgrade extends BaseObj {

    public void insert() {
        DbRs rs = null;
        clearErr();
        DbConn conn = new DbConn();
        try {
            conn.setAutoCommit(false);
            String sql = "select * from sevgrade where sgid = ?";
            conn.prepare(sql);
            conn.setInt(1, getSgid());
            rs = conn.executeQuery();
            if (rs != null && rs.size() > 0) {
                setErr("等级编号已经存在请重新输入");
                return;
            }
            sql = "insert into sevgrade(sgid,sgname)values(?,?)";
            conn.prepare(sql);
            conn.setInt(1, getSgid());
            conn.setString(2, getSgname());
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
            sql = "update sevgrade set sgname=? where sgid = ?";
            conn.prepare(sql);
            conn.setString(1, getSgname());
            conn.setInt(2, getSgid());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public void delete() {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            DbRs rs = null;
            sql = "select * from sevgrade where sgid = ?";
            conn.prepare(sql);
            conn.setInt(1, getSgid());
            rs = conn.executeQuery();
            if (!(rs == null || rs.size() > 0)) {
                setErr("该信息不存在");
                return;
            }
            sql = "delete from sevgrade where sgid = ?";
            conn.prepare(sql);
            conn.setInt(1, getSgid());
            conn.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }

    public Sevgrade() {
    }

    private int sgid = 0;

    private String sgname = "";

    public int getSgid() {
        return sgid;
    }

    public void setSgid(int sgid) {
        this.sgid = sgid;
    }

    public String getSgname() {
        return sgname;
    }

    public void setSgname(String sgname) {
        this.sgname = sgname;
    }
}
