package com.gever.sysman.privilege.dao.impl;

import com.gever.config.Constants;
import com.gever.exception.DefaultException;
import com.gever.exception.db.DAOException;
import com.gever.jdbc.connection.ConnectionProvider;
import com.gever.jdbc.connection.ConnectionProviderFactory;
import com.gever.jdbc.sqlhelper.DefaultSQLHelper;
import com.gever.jdbc.sqlhelper.SQLHelper;
import com.gever.sysman.privilege.dao.*;
import com.gever.sysman.privilege.model.Operation;
import com.gever.sysman.privilege.model.Resource;
import com.gever.sysman.privilege.model.RolePermission;
import com.gever.sysman.privilege.model.UserPermission;
import com.gever.web.homepage.dao.UserMenuDAOFactory;
import com.gever.web.homepage.dao.UserMenuDao;
import org.apache.commons.lang.StringUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Hu.Walker
 *
 */
public class DefaultPermissionDAO implements PermissionDAO {

    private static String DEL_ROLE_PERM = "DELETE FROM T_ROLE_PERMISSION WHERE role_id=?";

    private static String ADD_ROLE_PERM = "INSERT INTO T_ROLE_PERMISSION(role_id,resource_id,resoperation_id) VALUES(?,?,?)";

    private static String DEL_USER_PERM = "DELETE FROM T_USER_PERMISSION WHERE user_id=?";

    private static String ADD_USER_PERM = "INSERT INTO T_USER_PERMISSION(user_id,resource_id,resoperation_id) VALUES(?,?,?)";

    private StringBuffer res_role_tree_str = new StringBuffer();

    private StringBuffer res_user_tree_str = new StringBuffer();

    private int level = 0;

    public static Collection parents_collect = new ArrayList();

    /**
     * ��ݲ������Ϻ͵�ǰ��ɫ�Ĳ���Ȩ�ޣ����ò������ϵ�һ�����ԣ��Ƿ��в���Ȩ�ޣ�
     * @param opt_collect ���в����ļ���
     * @param role_res_opt_collect  ��ɫȨ�޼���
     * @return
     */
    public Collection returnOpts(Collection opt_collect, Collection role_res_opt_collect) {
        Collection opt_collect_isOpt = new ArrayList();
        Iterator opt_iterator = opt_collect.iterator();
        if (role_res_opt_collect == null) {
            return opt_collect;
        }
        Iterator perm_iterator = role_res_opt_collect.iterator();
        long opt_id = -1;
        long perm_opt_id = 0;
        while (opt_iterator.hasNext()) {
            Operation opt = (Operation) opt_iterator.next();
            opt_id = opt.getId();
            while (perm_iterator.hasNext()) {
                RolePermission role_perm = (RolePermission) perm_iterator.next();
                perm_opt_id = role_perm.getResopid();
                if (perm_opt_id == opt_id) {
                    opt.setOpt(true);
                    break;
                }
            }
            perm_iterator = role_res_opt_collect.iterator();
            opt_collect_isOpt.add(opt);
        }
        return opt_collect_isOpt;
    }

    /**
     * ����û��ĵ�Ȩ�ޣ����ɫ�ģ���������Դ������һ�����ԣ��Ƿ��в���Ȩ�ޣ�
     * @param opt_collect ���в����ļ���
     * @param user_res_opt_collect �û�Ȩ�޼���
     * @return
     */
    public Collection returnUserOpts(Collection opt_collect, Collection user_res_opt_collect) {
        Collection opt_collect_isOpt = new ArrayList();
        Iterator opt_iterator = opt_collect.iterator();
        if (user_res_opt_collect == null) {
            return opt_collect;
        }
        Iterator perm_iterator = user_res_opt_collect.iterator();
        long opt_id = -1;
        long perm_opt_id = 0;
        while (opt_iterator.hasNext()) {
            Operation opt = (Operation) opt_iterator.next();
            opt_id = opt.getId();
            while (perm_iterator.hasNext()) {
                UserPermission user_perm = (UserPermission) perm_iterator.next();
                perm_opt_id = user_perm.getResop_id();
                if (perm_opt_id == opt_id) {
                    opt.setRolePerm(user_perm.isRolePerm());
                    opt.setOpt(true);
                    break;
                }
            }
            perm_iterator = user_res_opt_collect.iterator();
            opt_collect_isOpt.add(opt);
        }
        return opt_collect_isOpt;
    }

    /**
     * ����������Ӧ����Ӧ����Դ����
     * @param res_collect
     * @param opt_isOpt_collect
     * @return
     */
    public Collection returnRess(Collection res_collect, Collection opt_isOpt_collect) {
        Collection res_collect_isOpt = new ArrayList();
        Iterator res_iterator = res_collect.iterator();
        if (opt_isOpt_collect == null) {
            return res_collect;
        }
        Iterator opt_iterator = opt_isOpt_collect.iterator();
        long res_id = -1;
        long opt_id = -2;
        while (res_iterator.hasNext()) {
            Resource res = (Resource) res_iterator.next();
            long folder = Long.parseLong(res.getIsFolder());
            if (folder > 0) {
                res_collect_isOpt.add(res);
                continue;
            } else {
                res_id = res.getId();
                Collection opt_temp = new ArrayList();
                while (opt_iterator.hasNext()) {
                    Operation opt = (Operation) opt_iterator.next();
                    long opt_res_id = opt.getResourceID();
                    if (res_id == opt_res_id) {
                        opt_temp.add(opt);
                    }
                }
                res_id = -1;
                opt_id = -2;
                opt_iterator = opt_isOpt_collect.iterator();
                res.setOperations(opt_temp);
                res_collect_isOpt.add(res);
            }
        }
        return res_collect_isOpt;
    }

    public Collection getChildNodes(Collection node_collect, long node_id) {
        Collection childs = new ArrayList();
        Iterator root_ir = node_collect.iterator();
        while (root_ir.hasNext()) {
            Resource res = (Resource) root_ir.next();
            if (res.getParentID() == node_id) {
                childs.add(res);
                continue;
            }
        }
        return childs;
    }

    public Resource getParentNodes(Collection root, long node_pid) {
        Resource parent = null;
        Iterator root_ir = root.iterator();
        while (root_ir.hasNext()) {
            Resource res = (Resource) root_ir.next();
            if (res.getId() == node_pid) {
                parent = res;
                break;
            }
        }
        return parent;
    }

    public Resource getRoot(Collection root) {
        Resource r = null;
        Iterator r_iterator = root.iterator();
        while (r_iterator.hasNext()) {
            r = (Resource) r_iterator.next();
            if (r.getParentID() == -1) {
                break;
            }
        }
        return r;
    }

    /**
     * �����Դ�б?������û�Ȩ���б�˵�
     * @param res_collect
     * @return
     */
    public String user_process(Collection res_collect) {
        Collection ress = res_collect;
        if (ress == null) {
            return "��";
        }
        Resource root = this.getRoot(res_collect);
        Collection root_child = this.getChildNodes(res_collect, root.getId());
        res_user_tree_str.append("<TR id=\"tr" + root.getId() + "\"><TD><span style=\"cursor:hand\" class='ss' id='e" + root.getId() + "p" + root.getParentID() + "' onclick='Display(this,e" + root.getId() + ")'>-</span><input type='checkbox' name='cb' value='r" + root.getId() + "' onclick=\"chk('r" + root.getId() + "')\">\n");
        res_user_tree_str.append(root.getName() + "</TD></TR>\n");
        level++;
        resUserTree(ress, root_child, root, res_user_tree_str);
        return res_user_tree_str.toString();
    }

    /**
     * �ݹ�����û���Դ�����б?�û�Ȩ�ޣ�
     */
    public void resUserTree(Collection res_collect, Collection node_child_collect, Resource res_node, StringBuffer result) {
        Iterator res_ir = node_child_collect.iterator();
        level++;
        int opt_num = 0;
        while (res_ir.hasNext()) {
            Resource res = (Resource) res_ir.next();
            Collection child_collect = this.getChildNodes(res_collect, res.getId());
            if (child_collect.size() == 0) {
                Collection opt_collect = res.getOperations();
                if ((opt_collect != null) && (opt_collect.size() > 0)) {
                    result.append("<TR id=\"ts" + res.getId() + "\"><TD>" + space(level) + "<span style=\"cursor:hand\" class='ss' id='e" + res.getId() + "p" + res.getParentID() + "' onclick='Display(this,e" + res.getId() + ")'>" + "-</span><input type='checkbox' name='cb' value='r" + res.getId() + "' onclick=\"chk('r" + res.getId() + "')\">\n");
                    result.append(res.getName() + "</TD></TR>\n");
                    Iterator opt_ir = opt_collect.iterator();
                    level++;
                    opt_num = 0;
                    result.append("<TR id=\"to" + res.getId() + "\"><TD>");
                    while (opt_ir.hasNext()) {
                        opt_num++;
                        Operation opt = (Operation) opt_ir.next();
                        String color = "";
                        if (opt_num == 1) {
                            if (!opt.isRolePerm()) {
                                result.append("&nbsp;&nbsp;" + space(level) + "<input type='checkbox' name='opt' value='" + opt.getResourceID() + "o" + opt.getId() + "' " + (opt.isOpt() ? "checked" : " ") + " onclick=\"chk('o" + opt.getId() + "')\">\n");
                            } else {
                                color = "#C0C0C0";
                                result.append("&nbsp;&nbsp;" + space(level) + "<input type='checkbox' name='r_opt' value='" + opt.getResourceID() + "o" + opt.getId() + "' " + (opt.isOpt() ? "checked" : " ") + " onclick=\"chk('o" + opt.getId() + "')\">\n");
                            }
                        } else {
                            if (!opt.isRolePerm()) {
                                result.append("&nbsp;&nbsp;" + "<input type='checkbox' name='opt' value='" + opt.getResourceID() + "o" + opt.getId() + "' " + (opt.isOpt() ? "checked" : " ") + " onclick=\"chk('o" + opt.getId() + "')\">\n");
                            } else {
                                color = "#C0C0C0";
                                result.append("&nbsp;&nbsp;" + "<input type='checkbox' name='r_opt' value='" + opt.getResourceID() + "o" + opt.getId() + "' " + (opt.isOpt() ? "checked" : " ") + " onclick=\"chk('o" + opt.getId() + "')\">\n");
                            }
                        }
                        result.append("<span style=\"background-color:" + color + "\">" + opt.getName() + "</span>");
                    }
                    result.append(" </TD></TR>\n");
                    level--;
                    continue;
                } else {
                    result.append("<TR id=\"ts" + res.getId() + "\"><TD>" + space(level) + "<span style=\"cursor:hand\" class='ss' id='e" + res.getId() + "' onclick='Display(this,e" + res.getId() + ")'>" + "-</span>\n");
                    result.append(res.getName() + "</TD></TR>\n");
                }
            } else {
                result.append("<TR id=\"ts" + res.getId() + "\"><TD>" + space(level) + "<span style=\"cursor:hand\" class='ss' id='e" + res.getId() + "' onclick='Display(this,e" + res.getId() + ")'>" + "-</span>\n");
                result.append(res.getName() + "</TD></TR>\n");
                resUserTree(res_collect, child_collect, res, result);
                level--;
            }
        }
        res_user_tree_str = result;
    }

    /**
     * �����Դ�б?�������ɫȨ���б�˵�
     * @param res_collect
     * @return
     */
    public String process(Collection res_collect) {
        Collection ress = res_collect;
        if (ress == null) {
            return "��";
        }
        Resource root = this.getRoot(res_collect);
        Collection root_child = this.getChildNodes(res_collect, root.getId());
        res_role_tree_str.append("<TR id=\"tr" + root.getId() + "\"><TD>\n");
        res_role_tree_str.append(root.getName() + "</TD></TR>\n");
        level++;
        resTree(ress, root_child, root, res_role_tree_str);
        return res_role_tree_str.toString();
    }

    /**
     * �ݹ������Դ�����б�
     */
    public void resTree(Collection res_collect, Collection node_child_collect, Resource res_node, StringBuffer result) {
        Iterator res_ir = node_child_collect.iterator();
        level++;
        int opt_num = 0;
        while (res_ir.hasNext()) {
            Resource res = (Resource) res_ir.next();
            Collection child_collect = this.getChildNodes(res_collect, res.getId());
            if (child_collect.size() == 0) {
                Collection opt_collect = res.getOperations();
                if ((opt_collect != null) && (opt_collect.size() > 0)) {
                    result.append("<TR id=\"ts" + res.getId() + "\"><TD>" + space(level) + "\n");
                    result.append(res.getName() + "</TD></TR>\n");
                    Iterator opt_ir = opt_collect.iterator();
                    level++;
                    opt_num = 0;
                    result.append("<TR id=\"to" + res.getId() + "\"><TD>");
                    while (opt_ir.hasNext()) {
                        opt_num++;
                        Operation opt = (Operation) opt_ir.next();
                        if (opt_num == 1) {
                            result.append("&nbsp;&nbsp;" + space(level) + "<input type='checkbox' name='opt' value='" + opt.getResourceID() + "o" + opt.getId() + "' " + (opt.isOpt() ? "checked" : " ") + " onclick=\"chk('o" + opt.getId() + "')\">\n");
                        } else {
                            result.append("&nbsp;&nbsp;" + "<input type='checkbox' name='opt' value='" + opt.getResourceID() + "o" + opt.getId() + "' " + (opt.isOpt() ? "checked" : " ") + " onclick=\"chk('o" + opt.getId() + "')\">\n");
                        }
                        result.append(opt.getName());
                    }
                    result.append(" </TD></TR>\n");
                    level--;
                    continue;
                } else {
                    result.append("<TR id=\"ts" + res.getId() + "\"><TD>" + space(level) + "\n");
                    result.append(res.getName() + "</TD></TR>\n");
                }
            } else {
                result.append("<TR id=\"ts" + res.getId() + "\"><TD>" + space(level) + "\n");
                result.append(res.getName() + "</TD></TR>\n");
                resTree(res_collect, child_collect, res, result);
                level--;
            }
        }
        res_role_tree_str = result;
    }

    public String space(int i) {
        String s = "&nbsp;&nbsp;";
        for (int j = 0; j < i; j++) {
            s += "&nbsp;&nbsp;";
        }
        return s;
    }

    /**
     * ɾ���û�ʱɾ���û�Ȩ�޹������
     * @author Hu.Walker
     */
    public void delUserPerm(String[] userId) throws DAOException, SQLException {
        StringBuffer sql = new StringBuffer();
        sql.append("DELETE FROM T_USER_PERMISSION WHERE USER_ID=");
        for (int i = 0; i < userId.length; i++) {
            sql.append(userId[i] + " OR USER_ID=");
        }
        sql.append("-100");
        try {
            SQLHelper helper = new DefaultSQLHelper("gdp");
            helper.execUpdate(sql.toString());
        } catch (DefaultException e) {
            throw new DAOException("ORG_MenuSetup_deleteByUserID", e);
        }
    }

    /**
     * �洢�û�Ȩ��
     */
    public void savaUserPerm(String userid, Collection user_perm_collect) throws DAOException, SQLException {
        ConnectionProvider cp = null;
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        PrivilegeFactory factory = PrivilegeFactory.getInstance();
        Operation op = factory.createOperation();
        try {
            cp = ConnectionProviderFactory.getConnectionProvider(Constants.DATA_SOURCE);
            conn = cp.getConnection();
            pstmt = conn.prepareStatement(DEL_USER_PERM);
            pstmt.setString(1, userid);
            pstmt.executeUpdate();
            if ((user_perm_collect == null) || (user_perm_collect.size() <= 0)) {
                return;
            } else {
                conn.setAutoCommit(false);
                pstmt = conn.prepareStatement(ADD_USER_PERM);
                Iterator user_perm_ir = user_perm_collect.iterator();
                while (user_perm_ir.hasNext()) {
                    UserPermission userPerm = (UserPermission) user_perm_ir.next();
                    pstmt.setString(1, String.valueOf(userPerm.getUser_id()));
                    pstmt.setString(2, String.valueOf(userPerm.getResource_id()));
                    pstmt.setString(3, String.valueOf(userPerm.getResop_id()));
                    pstmt.executeUpdate();
                }
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            conn.rollback();
            throw new DAOException();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * �洢��ɫȨ��
     */
    public void savaRolePerm(String roleid, Collection role_perm_collect) throws DAOException, SQLException {
        ConnectionProvider cp = null;
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        PrivilegeFactory factory = PrivilegeFactory.getInstance();
        Operation op = factory.createOperation();
        try {
            cp = ConnectionProviderFactory.getConnectionProvider(Constants.DATA_SOURCE);
            conn = cp.getConnection();
            try {
                pstmt = conn.prepareStatement(DEL_ROLE_PERM);
                pstmt.setString(1, roleid);
                pstmt.executeUpdate();
            } catch (Exception e) {
            }
            if ((role_perm_collect == null) || (role_perm_collect.size() == 0)) {
                return;
            } else {
                conn.setAutoCommit(false);
                pstmt = conn.prepareStatement(ADD_ROLE_PERM);
                Iterator role_perm_ir = role_perm_collect.iterator();
                while (role_perm_ir.hasNext()) {
                    RolePermission rolePerm = (RolePermission) role_perm_ir.next();
                    pstmt.setString(1, String.valueOf(rolePerm.getRoleid()));
                    pstmt.setString(2, String.valueOf(rolePerm.getResourceid()));
                    pstmt.setString(3, String.valueOf(rolePerm.getResopid()));
                    pstmt.executeUpdate();
                }
                conn.commit();
                conn.setAutoCommit(true);
            }
        } catch (Exception e) {
            conn.rollback();
            throw new DAOException();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * ��ȡ�û�Ȩ��
     * @param roleid
     * @param res_opt
     * @return
     */
    public Collection getUserPerm(String userid, String[] res_opt) {
        Collection userPermCollect = new ArrayList();
        if ((res_opt == null) || (res_opt.length == 0)) {
            return null;
        } else {
            for (int i = 0; i < res_opt.length; i++) {
                UserPermission userPerm = PrivilegeFactory.getInstance().createUserPermission();
                String[] r_o = StringUtils.split(res_opt[i], "o");
                userPerm.setUser_id(Long.parseLong(userid));
                userPerm.setResource_id(Long.parseLong(r_o[0]));
                userPerm.setResop_id(Long.parseLong(r_o[1]));
                userPermCollect.add(userPerm);
            }
        }
        return userPermCollect;
    }

    /**
     * ��ȡ��ɫȨ��
     * @param roleid ��ɫid
     * @param res_opt ��Դ��������
     */
    public Collection getRolePerm(String roleid, String[] res_opt) {
        Collection rolePermCollect = new ArrayList();
        if ((res_opt == null) || (res_opt.length == 0)) {
            return null;
        } else {
            for (int i = 0; i < res_opt.length; i++) {
                RolePermission rolePerm = PrivilegeFactory.getInstance().createRolePermission();
                String[] r_o = StringUtils.split(res_opt[i], "o");
                rolePerm.setRoleid(Long.parseLong(roleid));
                rolePerm.setResourceid(Long.parseLong(r_o[0]));
                rolePerm.setResopid(Long.parseLong(r_o[1]));
                rolePermCollect.add(rolePerm);
            }
        }
        return rolePermCollect;
    }

    /**
     * �ϲ��û�Ȩ�޺ͽ�ɫȨ�޵��û�Ȩ�޼���
     * @param userOpts �û�Ȩ�޼���
     * @param roleOpts �û���ɫȨ�޼���
     */
    public Collection getUserAndRolePerm(long userID, Collection userOpts, Collection roleOpts) {
        Collection userAndRolePerm = userOpts;
        if ((roleOpts == null) || (roleOpts.size() == 0)) {
            return userAndRolePerm;
        }
        Iterator roleOpts_ir = roleOpts.iterator();
        Iterator userOpts_ir = null;
        if ((userOpts != null)) {
            userOpts_ir = userOpts.iterator();
        }
        if (userOpts != null && userOpts.size() > 0) {
            while (roleOpts_ir.hasNext()) {
                RolePermission roleperm = (RolePermission) roleOpts_ir.next();
                long role_opt_id = roleperm.getResopid();
                long role_res_id = roleperm.getResourceid();
                boolean addIndex = true;
                while (userOpts_ir.hasNext()) {
                    UserPermission userperm = (UserPermission) userOpts_ir.next();
                    long user_opt_id = userperm.getResop_id();
                    long user_res_id = userperm.getResource_id();
                    if ((user_opt_id == role_opt_id) && (user_res_id == role_res_id)) {
                        addIndex = false;
                        break;
                    }
                }
                if (addIndex) {
                    UserPermission userperm_t = PrivilegeFactory.getInstance().createUserPermission();
                    userperm_t.setUser_id(userID);
                    userperm_t.setResop_id(role_opt_id);
                    userperm_t.setResource_id(role_res_id);
                    userperm_t.setRes_code(roleperm.getRes_code());
                    userperm_t.setOpt_code(roleperm.getOpt_code());
                    userperm_t.setRolePerm(true);
                    userAndRolePerm.add(userperm_t);
                }
                userOpts_ir = userOpts.iterator();
            }
        } else {
            if (userAndRolePerm == null) {
                userAndRolePerm = new ArrayList();
            }
            String role_res_code;
            String role_opt_code;
            while (roleOpts_ir.hasNext()) {
                RolePermission roleperm = (RolePermission) roleOpts_ir.next();
                long role_opt_id = roleperm.getResopid();
                long role_res_id = roleperm.getResourceid();
                role_opt_code = roleperm.getOpt_code();
                role_res_code = roleperm.getRes_code();
                UserPermission userperm_t = PrivilegeFactory.getInstance().createUserPermission();
                userperm_t.setUser_id(userID);
                userperm_t.setResop_id(role_opt_id);
                userperm_t.setResource_id(role_res_id);
                userperm_t.setRes_code(role_res_code);
                userperm_t.setOpt_code(role_opt_code);
                userperm_t.setRolePerm(true);
                userAndRolePerm.add(userperm_t);
            }
        }
        return userAndRolePerm;
    }

    /**
     * ���õݹ飬�����Դid��ȡ���еĸ�����Դ(�������Դ����)
     * @param resid
     * @return
     */
    public void getAllParentResByid(String resid) {
        ResourceDAO resDAO = PrivilegeFactory.getInstance().getResourceDAO();
        try {
            Collection res_collect = resDAO.getResources();
            Resource res = resDAO.findResourceByID(Long.parseLong(resid));
            parents_collect.add(res);
            Iterator res_ir = res_collect.iterator();
            long res_pid = res.getParentID();
            while (res_ir.hasNext()) {
                Resource pres = (Resource) res_ir.next();
                if (res_pid == pres.getId()) {
                    parents_collect.add(pres);
                    getAllParentResByid(String.valueOf(pres.getId()));
                    break;
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * ���Ȩ�޼��Ϸ�����Դ���
     * @param userPerm
     * @return
     */
    public Collection getResCollect(Collection userPerm) throws DAOException {
        PrivilegeFactory factory = PrivilegeFactory.getInstance();
        long[] ids = new long[userPerm.size()];
        Iterator itr = userPerm.iterator();
        int i = 0;
        while (itr.hasNext()) {
            ids[i] = ((UserPermission) itr.next()).getResource_id();
            i++;
        }
        Collection ress = PrivilegeFactory.getInstance().getResourceDAO().findResourceByIDs(ids);
        return ress;
    }

    /**
     * ���Ȩ�޼��Ϸ�����Դ���
     * @param userPerm
     * @return
     */
    public Collection getResCollectByRole(Collection rolePerm) throws DAOException {
        PrivilegeFactory factory = PrivilegeFactory.getInstance();
        Collection ress = new ArrayList();
        Iterator rolePerm_ir = rolePerm.iterator();
        while (rolePerm_ir.hasNext()) {
            RolePermission roleperm = factory.createRolePermission();
            roleperm = (RolePermission) rolePerm_ir.next();
            Resource res = PrivilegeFactory.getInstance().getResourceDAO().findResourceByID(roleperm.getResourceid());
            ress.add(res);
        }
        return ress;
    }

    /**
     * �����Դ����ȡ���еĸ�����Դ
     * @param res_collect
     * @return
     */
    public Collection geResCollectIncludeParents(Collection res_collect) {
        Collection ress = new ArrayList();
        Iterator res_ir = res_collect.iterator();
        Set res_set = new HashSet();
        while (res_ir.hasNext()) {
            Resource res = (Resource) res_ir.next();
            this.getAllParentResByid(String.valueOf(res.getId()));
            res_set.addAll(parents_collect);
        }
        return res_set;
    }

    public Set getAllResources(Collection resources) throws DAOException {
        ResourceDAO resourceDAO = PrivilegeFactory.getInstance().getResourceDAO();
        Collection allResources = resourceDAO.getResources();
        Set result = new HashSet();
        Iterator res_ir = resources.iterator();
        while (res_ir.hasNext()) {
            Resource resource = (Resource) res_ir.next();
            result.add(resource);
            setParentResources(allResources, result, resource.getParentID());
        }
        return result;
    }

    public void setParentResources(Collection allResources, Set result, long parentID) {
        Iterator iAllResources = allResources.iterator();
        while (iAllResources.hasNext()) {
            Resource resource = (Resource) iAllResources.next();
            if (resource.getId() == parentID) {
                result.add(resource);
                setParentResources(allResources, result, resource.getParentID());
            }
        }
    }

    /**
     * �����û��˵�
     * @param userid
     * @param userPerm
     */
    public void resetMenuRes(String userid, Collection userPerm) throws DAOException {
        Collection ress = null;
        Collection opt_collect = null;
        Collection user_res_opt_collect = null;
        Collection user_role_perm_collect = null;
        PrivilegeFactory factory = PrivilegeFactory.getInstance();
        UserDAO userDAO = factory.getUserDAO();
        OperationDAO optDAO = factory.getOperationDAO();
        PermissionDAO pmDAO = factory.getPermissionDAO();
        UserMenuDao dao = UserMenuDAOFactory.getInstance().getUserMenuDao();
        if ((userid != null) && (!"".equals(userid))) {
            user_role_perm_collect = optDAO.getUserRolePerm(userid);
            user_res_opt_collect = pmDAO.getUserAndRolePerm(Long.parseLong(userid), userPerm, user_role_perm_collect);
            if (user_res_opt_collect != null) {
                user_res_opt_collect = getResCollect(user_res_opt_collect);
                user_res_opt_collect = getAllResources(user_res_opt_collect);
            }
            if (user_res_opt_collect != null) {
                Iterator i = user_res_opt_collect.iterator();
                while (i.hasNext()) {
                    Resource r = (Resource) i.next();
                }
            }
            dao.resetUserMenus(userid, user_res_opt_collect);
        }
    }

    /**
     * ��ȡ�����û�Ȩ��
     */
    public String[] getUserPerm(long userID) throws DAOException, SQLException {
        String[] operationsAndResources = null;
        Collection c = new ArrayList();
        ConnectionProvider cp = null;
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        PrivilegeFactory factory = PrivilegeFactory.getInstance();
        Operation op = factory.createOperation();
        try {
            cp = ConnectionProviderFactory.getConnectionProvider(Constants.DATA_SOURCE);
            conn = cp.getConnection();
            String sql = "select * from T_USER_PERMISSION where USER_ID = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userID);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                c.add(rs.getLong("RESOURCE_ID") + "o" + rs.getLong("RESOPERATION_ID"));
            }
            rs.close();
            pstmt.close();
            conn.close();
            operationsAndResources = (String[]) c.toArray(new String[0]);
            return operationsAndResources;
        } catch (Exception e) {
            throw new DAOException();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (Exception e) {
            }
        }
    }
}
