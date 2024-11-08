package com.jedi;

import java.util.ArrayList;
import java.util.List;
import com.tss.util.DbConn;
import com.tss.util.DbRs;

/**
 * @author Administrator
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Role extends BaseObj {

    public Role() {
    }

    public Role(String id, boolean isGetPower) {
        this.roleId = id;
        if (isGetPower) setPower();
    }

    public Role(String id) {
        this.roleId = id;
    }

    public boolean hasPower(String powerId) {
        clearErr();
        List power = getPower();
        if (power == null || power.size() == 0) return false;
        for (int i = 0; i < power.size(); i++) {
            Power item = (Power) power.get(i);
            if (item.getId().equals(powerId)) return true;
        }
        return false;
    }

    public boolean hasPower(Power power) {
        return hasPower(power.getId());
    }

    public void updatePower(String[] power) {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            conn.setAutoCommit(false);
            sql = "delete from rolepower where role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.executeUpdate();
            if (power != null && power.length > 0) {
                for (int i = 0; i < power.length; i++) {
                    sql = "insert into rolepower (role_id,power_id) values (" + "?,?)";
                    conn.prepare(sql);
                    conn.setString(1, getId());
                    conn.setString(2, power[i]);
                    conn.executeUpdate();
                }
            }
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
                conn.setAutoCommit(true);
            } catch (Exception subEx) {
                subEx.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    /**
	 * 删除角色同时：1，此角色有用户部能删除2，删除此角色的权限表
	 * 修改日期 2006-09-20 
	 */
    public void delete() {
        DbConn conn = new DbConn();
        try {
            String sql = "select * from userinfo where role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            DbRs rs = conn.executeQuery();
            if (rs != null && rs.size() > 0) {
                setErr("此角色已经创建了用户不能删除！");
                return;
            }
            conn.setAutoCommit(false);
            sql = "delete from roleinfo where role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.executeUpdate();
            sql = "delete from rolepower where role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception sex) {
                sex.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public void update(String[] power) {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            conn.setAutoCommit(false);
            sql = "update roleinfo set role_name = ?,show_order=?,role_desc=? where role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getRoleName());
            conn.setInt(2, getShowOrder());
            conn.setString(3, getRoleDesc());
            conn.setString(4, getId());
            conn.executeUpdate();
            sql = "delete from rolepower where role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.executeUpdate();
            if (power != null && power.length > 0) {
                for (int i = 0; i < power.length; i++) {
                    sql = "insert into rolepower (role_id,power_id) values (" + "?,?)";
                    conn.prepare(sql);
                    conn.setString(1, getId());
                    conn.setString(2, power[i]);
                    conn.executeUpdate();
                }
            }
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception subEx) {
                subEx.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public void insert(String[] power) {
        clearErr();
        DbConn conn = new DbConn();
        try {
            String sql = "";
            sql = "select * from userinfo where role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            DbRs rs = conn.executeQuery();
            if (rs != null && rs.size() > 0) {
                setErr("此角色已经存在！");
                return;
            }
            conn.setAutoCommit(false);
            sql = "insert into roleinfo (role_id,role_name,show_order,role_desc)" + " values (?,?,?,?)";
            conn.prepare(sql);
            conn.setString(1, getId());
            conn.setString(2, getRoleName());
            conn.setInt(3, getShowOrder());
            conn.setString(4, getRoleDesc());
            conn.executeUpdate();
            if (power != null && power.length > 0) {
                for (int i = 0; i < power.length; i++) {
                    sql = "insert into rolepower (role_id,power_id) values (" + "?,?)";
                    conn.prepare(sql);
                    conn.setString(1, getId());
                    conn.setString(2, power[i]);
                    conn.executeUpdate();
                }
            }
            conn.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
            try {
                conn.rollback();
            } catch (Exception subEx) {
                subEx.printStackTrace();
            }
        } finally {
            conn.close();
        }
    }

    public String getId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public int getShowOrder() {
        return showOrder;
    }

    public String getRoleDesc() {
        return roleDesc;
    }

    public List getPower() {
        return power;
    }

    private void setId(String id) {
        this.roleId = id;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setShowOrder(int showOrder) {
        this.showOrder = showOrder;
    }

    public void setRoleDesc(String roleDesc) {
        this.roleDesc = roleDesc;
    }

    private String roleId = "";

    private String roleName = "";

    private int showOrder = 0;

    private String roleDesc = "";

    private List power = null;

    /**
	 * 在用户登录时设置用户角色的权限
	 * 从角色权限表（rolepower）和权限表（poweinfo）关联
	 */
    private void setPower() {
        DbConn conn = new DbConn();
        try {
            String sql = "select a.*,b.power_name,b.group_name,b.power_desc " + "from rolepower a inner join poweinfo b on a.power_id = b.power_id where a.role_id = ?";
            conn.prepare(sql);
            conn.setString(1, getId());
            DbRs rs = conn.executeQuery();
            power = new ArrayList();
            if (rs == null || rs.size() == 0) return;
            for (int i = 0; i < rs.size(); i++) {
                Power item = new Power(get(rs, i, "power_id"));
                item.setPowerName(get(rs, i, "power_name"));
                item.setGroupName(get(rs, i, "group_name"));
                item.setPowerDesc(get(rs, i, "power_desc"));
                power.add(item);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        } finally {
            conn.close();
        }
    }
}
