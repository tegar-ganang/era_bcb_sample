package ces.platform.infoplat.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import ces.coral.dbo.DBOperation;
import ces.coral.lang.StringUtil;
import ces.platform.infoplat.core.Channel;
import ces.platform.infoplat.core.DocType;
import ces.platform.infoplat.core.Site;
import ces.platform.infoplat.core.base.BaseDAO;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.utils.Function;
import ces.platform.system.common.Constant;
import ces.platform.system.common.XmlInfo;
import ces.platform.system.facade.AuthorityManager;

/**
 * <p>
 * Title: ������Ϣƽ̨:���ڵ���ݿ������
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2004
 * </p>
 * <p>
 * Company: �Ϻ�������Ϣ��չ���޹�˾
 * </p>
 * 
 * @author ����
 * @version 2.5
 */
public class TreeNodeDAO extends BaseDAO {

    public TreeNodeDAO() {
    }

    /**
	 * �õ����ڵ��ʵ��(��ݽڵ������,���첻ͬ������ʵ��),���Կ���һ���򵥹�������
	 * 
	 * @param path
	 *            ��path
	 * @return @throws
	 *         java.lang.Exception
	 */
    public TreeNode getTreeNode(String path) throws Exception {
        TreeNode result = null;
        int treeNodeType = this.getTreeNodeType(path);
        if (treeNodeType == TreeNode.TREENODE_TYPE_SITE) {
            result = this.getSiteInstance(path);
        } else if (treeNodeType == TreeNode.TREENODE_TYPE_CHANNEL) {
            result = this.getChannelInstance(path);
        } else if (treeNodeType == TreeNode.TREENODE_TYPE_DOCTYPE) {
            result = this.getDocTypeInstance(path);
        } else if (treeNodeType == TreeNode.TREENODE_TYPE_ROOT) {
            result = this.getRootInstance(path);
        }
        return result;
    }

    /**
	 * ɾ�����е�ĳ���ڵ�
	 * 
	 * @param treeNode
	 *            Ҫɾ��Ľڵ�ʵ��
	 * @throws java.lang.Exception
	 */
    public void delete(TreeNode treeNode) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sqlStr = "delete from t_ip_tree_frame where path=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setString(1, treeNode.getPath());
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
	 * �õ�վ��Ƶ�������ܲ���
	 * 
	 * @throws java.lang.Exception
	 */
    public int getSiteChannelTreeTotalLevel() throws Exception {
        int result = 0;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sqlStr = "select max(tree_level) from t_ip_tree_frame where type='site' or type='chan'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sqlStr);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                result = resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * �õ��ĵ����������ܲ���
	 * 
	 * @throws java.lang.Exception
	 */
    public int getDocTypeTreeTotalLevel() throws Exception {
        int result = 0;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sqlStr = "select max(tree_level) from t_ip_tree_frame where type='type'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sqlStr);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                result = resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * �޸�һ���ڵ������(������path�ǹؼ���,���ܸı�; ����,�޸Ļ᲻�ɹ�,���ǲ�����Exception
	 * 
	 * @param treeNode
	 *            Ҫ�޸ĵĽڵ����
	 * @throws java.lang.Exception
	 */
    public void update(TreeNode treeNode) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sqlStr = "update t_ip_tree_frame set id=?,parent_id=?,path=?,ref_path=?,tree_level=?,type=?,order_no=?,title=?,description=?,style=?,icon=? where path=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setString(1, treeNode.getId());
            preparedStatement.setString(2, treeNode.getParentId());
            preparedStatement.setString(3, treeNode.getPath());
            preparedStatement.setString(4, treeNode.getRefPath());
            preparedStatement.setInt(5, treeNode.getLevel());
            preparedStatement.setString(6, treeNode.getType());
            preparedStatement.setInt(7, treeNode.getOrderNo());
            preparedStatement.setString(8, treeNode.getTitle());
            preparedStatement.setString(9, treeNode.getDescription());
            preparedStatement.setString(10, treeNode.getStyle());
            preparedStatement.setString(11, treeNode.getIcon());
            preparedStatement.setString(12, treeNode.getPath());
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
	 * ȡ�øýڵ��µ�������һ���ӽڵ�
	 * 
	 * @param path
	 *            �ýڵ����path
	 * @return @throws
	 *         java.lang.Exception
	 */
    public TreeNode[] getList(String path) throws Exception {
        final int SITE_LIST = 0;
        final int CHANNEL_LIST = 1;
        final int DOC_TYPE_LIST = 2;
        final int NON_LIST = -1;
        if (path == null) {
            return null;
        }
        List list = new ArrayList();
        int treeType = NON_LIST;
        if (path.equals(TreeNode.SITECHANNEL_TREE_ROOT_PATH)) {
            treeType = SITE_LIST;
        } else if (path.length() >= 5 && path.substring(0, 5).equals(TreeNode.SITECHANNEL_TREE_ROOT_PATH)) {
            treeType = CHANNEL_LIST;
        } else if (path.equals(TreeNode.DOCTYPE_TREE_ROOT_PATH) || (path.length() >= 5 && path.substring(0, 5).equals(TreeNode.DOCTYPE_TREE_ROOT_PATH))) {
            treeType = DOC_TYPE_LIST;
        } else {
            throw new Exception("�޷�ʶ���path(" + path + ")!");
        }
        if (treeType == SITE_LIST) {
            return getSiteList();
        } else if (treeType == CHANNEL_LIST) {
            return getChannelList(path);
        } else if (treeType == DOC_TYPE_LIST) {
            return getDocTypeList(path);
        } else {
            return null;
        }
    }

    /**
	 * ȡ�øýڵ�������ӽڵ㣨�����ӽڵ���ӽڵ㣩
	 * 
	 * @param path
	 *            �ýڵ����path
	 * @return @throws
	 *         java.lang.Exception
	 */
    public TreeNode[] getTree(String path, int userId) throws Exception {
        TreeNode[] tree = null;
        return tree;
    }

    /**
	 * ȡ�øýڵ�������ӽڵ㣨�����ӽڵ���ӽڵ㣩
	 * 
	 * @param path
	 *            �ýڵ����path
	 * @return 
	 * @throws
	 *         java.lang.Exception
	 */
    public TreeNode[] getTree(String path) throws Exception {
        final int SITE_CHANNEL_TREE = 0;
        final int DOC_TYPE_TREE = 1;
        final int NON_TREE = -1;
        if (path == null) {
            return null;
        }
        List list = new ArrayList();
        int treeType = NON_TREE;
        if (path.equals(TreeNode.SITECHANNEL_TREE_ROOT_PATH)) {
            treeType = SITE_CHANNEL_TREE;
        } else if (path.equals(TreeNode.DOCTYPE_TREE_ROOT_PATH)) {
            treeType = DOC_TYPE_TREE;
        } else {
            TreeNode tn = TreeNode.getInstance(path);
            if (tn == null) {
                throw new Exception("��ݿ���û���ҵ�path='" + path + "'���κμ�¼!");
            }
            if (tn.getType() != null && (tn.getType().equalsIgnoreCase("site") || tn.getType().equalsIgnoreCase("chan") || (tn.getType().equalsIgnoreCase("ds")))) {
                treeType = SITE_CHANNEL_TREE;
            } else if (tn.getType() != null && tn.getType().equalsIgnoreCase("type")) {
                treeType = DOC_TYPE_TREE;
            } else if (tn.getType() == null) {
                throw new Exception("path='" + path + "'��ʵ�������޷�ȷ��,��������ݱ��ƻ�!��ݿ���type=" + tn.getType() + "!");
            }
        }
        if (treeType == SITE_CHANNEL_TREE) {
            return getSiteChannelTree(path);
        } else if (treeType == DOC_TYPE_TREE) {
            return getDocTypeTree(path);
        } else {
            return null;
        }
    }

    /**
	 * ����һ�����ڵ�
	 * 
	 * @param treeNode
	 *            Ҫ���ӵ����ڵ����
	 * @throws java.lang.Exception
	 */
    public void add(TreeNode treeNode) throws Exception {
        String sqlStr = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            sqlStr = "insert into t_ip_tree_frame (id,parent_id,path,ref_path,tree_level,type,order_no,title,description,style,icon) values(?,?,?,?,?,?,?,?,?,?,?)";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setString(1, treeNode.getId());
            preparedStatement.setString(2, treeNode.getParentId());
            preparedStatement.setString(3, treeNode.getPath());
            preparedStatement.setString(4, treeNode.getRefPath());
            preparedStatement.setInt(5, treeNode.getLevel());
            preparedStatement.setString(6, treeNode.getType());
            preparedStatement.setInt(7, treeNode.getOrderNo());
            preparedStatement.setString(8, treeNode.getTitle());
            preparedStatement.setString(9, treeNode.getDescription());
            preparedStatement.setString(10, treeNode.getStyle());
            preparedStatement.setString(11, treeNode.getIcon());
            preparedStatement.executeUpdate();
        } catch (SQLException ex) {
            log.error("���ӵ����ڵ�������:" + sqlStr, ex);
            ex.printStackTrace();
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
	 * ȡ�ÿսڵ�
	 * @param path
	 *            �ýڵ����path
	 * @return 
	 * @throws
	 *         java.lang.Exception
	 */
    public TreeNode[] getDocTypeTree(int userId, String operaterId) throws Exception {
        TreeNode[] tree = null;
        return tree;
    }

    /**
	 * ���userId��operaterId��Ȩ�޵õ�վ��Ƶ����
	 * @param userId
	 * @param operaterId
	 * @return
	 * @throws Exception
	 */
    public TreeNode[] getSiteChannelTree(int userId, String operaterId) throws Exception {
        boolean has = false;
        String sitePaths = getAuthoritiedNodePath(userId, operaterId, 1);
        if (sitePaths != null && sitePaths.length() != 0) {
            has = true;
        } else {
            sitePaths = "";
        }
        String channelPaths = getAuthoritiedNodePath(userId, operaterId, 2);
        if (channelPaths != null && channelPaths.length() != 0) {
            has = true;
        } else {
            channelPaths = "";
        }
        if (!has) return null;
        String paths = sitePaths + channelPaths;
        String[] pathArray = Function.stringToArray(paths.trim());
        StringBuffer parents = new StringBuffer();
        for (int i = 0; i < pathArray.length; i++) {
            int count = pathArray[i].length();
            if (count < 5) continue;
            count = count / 5;
            for (int j = 0; j < count; j++) {
                String element = pathArray[i].substring(0, (j + 1) * 5);
                boolean bAdd = false;
                int index1 = parents.toString().indexOf(element + ",");
                int index2 = paths.indexOf(element + ",");
                if (index1 == -1 && index2 == -1) bAdd = true;
                if (element.length() <= 5) bAdd = false;
                if (bAdd) {
                    parents.append(element).append(",");
                }
            }
        }
        String allPaths = parents.append(paths).toString();
        allPaths = allPaths.trim();
        if (null != allPaths && !"".equals(allPaths)) {
            allPaths = allPaths.substring(0, allPaths.length() - 1);
            allPaths = "'" + StringUtil.replaceAll(allPaths, ",", "','") + "'";
        }
        ArrayList list = new ArrayList();
        TreeNode[] sites = getList(TreeNode.SITECHANNEL_TREE_ROOT_PATH);
        if (sites == null || sites.length == 0) {
            return null;
        }
        for (int i = 0; i < sites.length; i++) {
            if (sites[i] == null) {
                continue;
            } else if (sites[i] != null && allPaths.indexOf(sites[i].getPath()) == -1) {
                continue;
            } else {
                list.add(sites[i]);
            }
        }
        DBOperation dbo = null;
        ResultSet resultSet = null;
        try {
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("channel.id chan_id,").append("channel.name chan_name,").append("channel.description chan_description,").append("channel.ascii_name chan_ascii_name,").append("channel.site_id chan_site_id,").append("channel.type chan_type,").append("channel.data_url chan_data_url,").append("channel.template_id chan_template_id,").append("channel.use_status chan_use_status,").append("channel.order_no chan_order_no,").append("channel.style chan_style,").append("channel.creator chan_creator,").append("channel.create_date chan_create_date,").append("channel.refresh_flag chan_refresh_flag ").append(" from t_ip_tree_frame tree,t_ip_channel channel ").append(" where path in ").append("(").append(allPaths).append(")").append(" and tree.path=channel.channel_path order by path");
            dbo = createDBOperation();
            resultSet = dbo.select(sql.toString());
            while (resultSet.next()) {
                Channel channel = new Channel();
                channel.setAsciiName(resultSet.getString("chan_ascii_name"));
                channel.setChannelID(resultSet.getInt("chan_id"));
                channel.setChannelType(resultSet.getString("chan_type"));
                channel.setCreateDate(resultSet.getTimestamp("chan_create_date"));
                channel.setCreator(resultSet.getInt("chan_creator"));
                channel.setDataUrl(resultSet.getString("chan_data_url"));
                channel.setDesc(resultSet.getString("chan_description"));
                channel.setName(resultSet.getString("chan_name"));
                channel.setOrderNo(resultSet.getInt("chan_order_no"));
                channel.setRefresh(resultSet.getString("chan_refresh_flag"));
                channel.setSiteId(resultSet.getInt("chan_site_id"));
                channel.setStyle(resultSet.getString("chan_style"));
                channel.setTemplateId(resultSet.getString("chan_template_id"));
                channel.setUseStatus(resultSet.getString("chan_use_status"));
                channel.setDescription(resultSet.getString("tree_description"));
                channel.setIcon(resultSet.getString("tree_icon"));
                channel.setId(resultSet.getString("tree_id"));
                channel.setLevel(resultSet.getInt("tree_tree_level"));
                channel.setParentId(resultSet.getString("tree_parent_id"));
                channel.setPath(resultSet.getString("tree_path"));
                channel.setRefPath(resultSet.getString("tree_ref_path"));
                channel.setTitle(resultSet.getString("tree_title"));
                channel.setType(resultSet.getString("tree_type"));
                String cPath = channel.getPath();
                if (cPath != null && paths.indexOf(cPath + ",") != -1) {
                    channel.setEnableNode(true);
                }
                list.add(channel);
            }
        } catch (Exception e) {
            log.error("���userId=" + userId + "��operaterId=" + operaterId + "��Ȩ�޵õ�վ��Ƶ��������!");
        } finally {
            close(resultSet, null, null, null, dbo);
        }
        TreeNode[] tree = null;
        if (list.size() > 0) {
            tree = new TreeNode[list.size()];
        }
        for (int i = 0; i < list.size(); i++) {
            tree[i] = (TreeNode) list.get(i);
        }
        return tree;
    }

    /**
	 * pick out all the pathes of the aviable channels
	 * @param userId
	 * @param operaterId
	 * @param type
	 */
    public String getAuthoritiedNodePath(int userId, String operaterId, int type) {
        AuthorityManager am = new AuthorityManager();
        Vector res = new Vector();
        try {
            res = am.getResource(userId, "h_" + type, operaterId);
        } catch (Exception e1) {
            log.error("getResource���?" + e1);
        }
        int size = res.size();
        if (size == 0) return null;
        StringBuffer sql = new StringBuffer();
        if (type == 1) sql.append("select t.site_path from t_ip_site t where ");
        if (type == 2) sql.append(" select t.channel_path from t_ip_channel t where ");
        int count = 0;
        for (Iterator iter = res.iterator(); iter.hasNext(); ) {
            count++;
            String element = (String) iter.next();
            int id = Integer.parseInt(element) - Integer.parseInt(type + "00000000");
            sql.append(" t.id=").append(id);
            if (count < size) sql.append(" or ");
        }
        String sqlStr = null;
        DBOperation dbo = null;
        Connection connection = null;
        ResultSet rs = null;
        StringBuffer pathBuf = new StringBuffer();
        try {
            dbo = createDBOperation();
            rs = dbo.select(sql.toString());
            while (rs.next()) {
                pathBuf.append(rs.getString(1)).append(",");
            }
        } catch (Exception e) {
            log.error("getAuthoritiedNodePath(int userId, String operaterId,int type)���?");
        }
        return pathBuf.toString();
    }

    /**
	 * ���һ��path,�õ���path�����վ��Ƶ���� @param startPath ��ʼpath,������Ϊ�� @return
	 * ���нڵ������,���û���κνڵ�,����null����new TreeNode[0]
	 */
    private TreeNode[] getSiteChannelTree(String startPath) throws Exception {
        List list = new ArrayList();
        if (startPath.length() == 5) {
            TreeNode[] sites = getList(startPath);
            if (sites == null || sites.length == 0) {
                return null;
            }
            for (int i = 0; i < sites.length; i++) {
                if (sites[i] == null) {
                    continue;
                }
                TreeNode[] channels = getChannelTree(sites[i].getPath());
                list.add(sites[i]);
                for (int j = 0; channels != null && j < channels.length; j++) {
                    if (channels[j] != null) {
                        list.add(channels[j]);
                    }
                }
            }
        }
        if (startPath.length() == 10) {
            TreeNode site = getTreeNode(startPath);
            if (site == null) {
                throw new Exception("û���ҵ�path=" + startPath + "�Ķ���!");
            }
            TreeNode[] channels = getChannelTree(site.getPath());
            list.add(site);
            for (int j = 0; channels != null && j < channels.length; j++) {
                if (channels[j] != null) {
                    list.add(channels[j]);
                }
            }
        }
        if (startPath.length() > 10) {
            TreeNode[] channels = getChannelTree(startPath);
            for (int j = 0; channels != null && j < channels.length; j++) {
                if (channels[j] != null) {
                    list.add(channels[j]);
                }
            }
        }
        if (list.size() == 0) {
            return null;
        }
        TreeNode[] result = new TreeNode[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = (TreeNode) list.get(i);
        }
        return result;
    }

    /**
	 * �õ�һ��path��������Ƶ�����б�,��pathֻ����վ�����Ƶ�� @param siteChannelPath
	 * ֻ����վ�����Ƶ����path,�����Ǹ� @return
	 */
    private TreeNode[] getChannelTree(String siteChannelPath) throws Exception {
        List list = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("channel.id chan_id,").append("channel.name chan_name,").append("channel.description chan_description,").append("channel.ascii_name chan_ascii_name,").append("channel.site_id chan_site_id,").append("channel.type chan_type,").append("channel.data_url chan_data_url,").append("channel.template_id chan_template_id,").append("channel.use_status chan_use_status,").append("channel.order_no chan_order_no,").append("channel.style chan_style,").append("channel.creator chan_creator,").append("channel.create_date chan_create_date,").append("channel.refresh_flag chan_refresh_flag ").append("from t_ip_tree_frame tree,t_ip_channel channel ").append("where path like ? and path<>? and tree.path=channel.channel_path order by tree_path, chan_order_no");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, siteChannelPath + "%");
            preparedStatement.setString(2, siteChannelPath);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Channel channel = new Channel();
                channel.setAsciiName(resultSet.getString("chan_ascii_name"));
                channel.setChannelID(resultSet.getInt("chan_id"));
                channel.setChannelType(resultSet.getString("chan_type"));
                channel.setCreateDate(resultSet.getTimestamp("chan_create_date"));
                channel.setCreator(resultSet.getInt("chan_creator"));
                channel.setDataUrl(resultSet.getString("chan_data_url"));
                channel.setDesc(resultSet.getString("chan_description"));
                channel.setName(resultSet.getString("chan_name"));
                channel.setOrderNo(resultSet.getInt("chan_order_no"));
                channel.setRefresh(resultSet.getString("chan_refresh_flag"));
                channel.setSiteId(resultSet.getInt("chan_site_id"));
                channel.setStyle(resultSet.getString("chan_style"));
                channel.setTemplateId(resultSet.getString("chan_template_id"));
                channel.setUseStatus(resultSet.getString("chan_use_status"));
                channel.setDescription(resultSet.getString("tree_description"));
                channel.setIcon(resultSet.getString("tree_icon"));
                channel.setId(resultSet.getString("tree_id"));
                channel.setLevel(resultSet.getInt("tree_tree_level"));
                channel.setParentId(resultSet.getString("tree_parent_id"));
                channel.setPath(resultSet.getString("tree_path"));
                channel.setRefPath(resultSet.getString("tree_ref_path"));
                channel.setTitle(resultSet.getString("tree_title"));
                channel.setType(resultSet.getString("tree_type"));
                list.add(channel);
            }
        } catch (SQLException ex) {
            log.error("�õ�������!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        TreeNode[] result = null;
        if (list.size() > 0) {
            result = new TreeNode[list.size()];
        }
        for (int i = 0; i < list.size(); i++) {
            result[i] = (TreeNode) list.get(i);
        }
        return result;
    }

    /**
	 * �õ�һ��path���������ĵ����͵��б� @param startPath ֻ�����ĵ����͵�path,�����Ǹ� @return
	 */
    private TreeNode[] getDocTypeTree(String startPath) throws Exception {
        List list = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("doctype.id doctype_id,").append("doctype.name doctype_name,").append("doctype.description doctype_description,").append("doctype.ascii_name doctype_ascii_name,").append("doctype.category doctype_category,").append("doctype.input_template_id doctype_input_template_id,").append("doctype.show_template_id doctype_show_template_id,").append("doctype.content_template_id doctype_content_template_id,").append("doctype.use_status doctype_use_status,").append("doctype.order_no doctype_order_no,").append("doctype.creator doctype_creator,").append("doctype.create_date doctype_create_date,").append("doctype.process_id doctype_process_id").append(" from t_ip_tree_frame tree,t_ip_doc_type doctype ").append(" where path like ? and path<>? and tree.path=doctype.doctype_path order by path");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, startPath + "%");
            preparedStatement.setString(2, startPath);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                DocType docType = new DocType();
                docType.setAsciiName(resultSet.getString("docType_Ascii_Name"));
                docType.setCategory(resultSet.getString("docType_Category"));
                docType.setContentTemplateID(resultSet.getInt("docType_Content_Template_Id"));
                docType.setCreateDate(resultSet.getTimestamp("docType_Create_Date"));
                docType.setCreator(resultSet.getInt("docType_Creator"));
                docType.setDocTypeID(resultSet.getInt("doctype_id"));
                docType.setInputTemplateID(resultSet.getInt("docType_Input_Template_Id"));
                docType.setName(resultSet.getString("docType_Name"));
                docType.setProcessId(resultSet.getInt("docType_Process_Id"));
                docType.setShowTemplateID(resultSet.getInt("docType_Show_Template_Id"));
                docType.setUseStatus(resultSet.getInt("docType_Use_Status") == 1 ? true : false);
                docType.setDescription(resultSet.getString("tree_description"));
                docType.setIcon(resultSet.getString("tree_icon"));
                docType.setId(resultSet.getString("tree_id"));
                docType.setLevel(resultSet.getInt("tree_tree_level"));
                docType.setParentId(resultSet.getString("tree_parent_id"));
                docType.setPath(resultSet.getString("tree_path"));
                docType.setRefPath(resultSet.getString("tree_ref_path"));
                docType.setTitle(resultSet.getString("tree_title"));
                docType.setType(resultSet.getString("tree_type"));
                list.add(docType);
            }
        } catch (SQLException ex) {
            log.error("�õ�������!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        TreeNode[] result = null;
        if (list.size() > 0) {
            result = new DocType[list.size()];
        }
        for (int i = 0; i < list.size(); i++) {
            result[i] = (TreeNode) list.get(i);
        }
        return result;
    }

    /**
	 * �õ�վ����б�
	 * 
	 * @return
	 */
    private TreeNode[] getSiteList() throws Exception {
        List list = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("site.id site_id,").append("site.name site_name,").append("site.description site_description,").append("site.ascii_name site_ascii_name,").append("site.remark_number site_remark_number,").append("site.increment_index site_increment_index,").append("site.use_status site_use_status,").append("site.appserver_id site_appserver_id,appserver.name appserver_name").append(" from t_ip_tree_frame tree,t_ip_site site,t_ip_appserver appserver ").append(" where tree.path=site.site_path ");
            if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
                sql.append("and site.appserver_id=convert(varchar,appserver.id) ");
            } else {
                sql.append("and site.appserver_id=appserver.id ");
            }
            sql.append("order by path");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Site site = new Site();
                site.setAppserverID(resultSet.getInt("site_appserver_id"));
                site.setAppserverName(resultSet.getString("appserver_name"));
                site.setAsciiName(resultSet.getString("site_ascii_name"));
                site.setIncrementIndex(resultSet.getString("site_increment_index"));
                site.setName(resultSet.getString("site_name"));
                site.setSiteID(resultSet.getInt("site_id"));
                site.setUseStatus(resultSet.getInt("site_use_status"));
                site.setDescription(resultSet.getString("tree_description"));
                site.setIcon(resultSet.getString("tree_icon"));
                site.setId(resultSet.getString("tree_id"));
                site.setLevel(resultSet.getInt("tree_tree_level"));
                site.setParentId(resultSet.getString("tree_parent_id"));
                site.setPath(resultSet.getString("tree_path"));
                site.setRefPath(resultSet.getString("tree_ref_path"));
                site.setTitle(resultSet.getString("tree_title"));
                site.setType(resultSet.getString("tree_type"));
                list.add(site);
            }
        } catch (SQLException ex) {
            log.error("�õ�վ���б����!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        TreeNode[] result = null;
        if (list.size() > 0) {
            result = new Site[list.size()];
        }
        for (int i = 0; i < list.size(); i++) {
            result[i] = (Site) list.get(i);
        }
        return result;
    }

    /**
	 * �õ�һ��path���浥��Ƶ�����б�,��pathֻ����վ�����Ƶ�� @param siteChannelPath
	 * ֻ����վ�����Ƶ����path,�����Ǹ� @return
	 */
    private TreeNode[] getChannelList(String siteChannelPath) throws Exception {
        List list = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ");
            sql.append("tree.id tree_id,");
            sql.append("tree.parent_id tree_parent_id,");
            sql.append("tree.path tree_path,");
            sql.append("tree.ref_path tree_ref_path,");
            sql.append("tree.tree_level tree_tree_level,");
            sql.append("tree.type tree_type,");
            sql.append("tree.title tree_title,");
            sql.append("tree.order_no tree_order_no,");
            sql.append("tree.description tree_description,");
            sql.append("tree.style tree_style,");
            sql.append("tree.icon tree_icon,");
            sql.append("channel.id chan_id,");
            sql.append("channel.name chan_name,");
            sql.append("channel.description chan_description,");
            sql.append("channel.ascii_name chan_ascii_name,");
            sql.append("channel.site_id chan_site_id,");
            sql.append("channel.type chan_type,");
            sql.append("channel.data_url chan_data_url,");
            sql.append("channel.template_id chan_template_id,");
            sql.append("channel.use_status chan_use_status,");
            sql.append("channel.order_no chan_order_no,");
            sql.append("channel.style chan_style,");
            sql.append("channel.creator chan_creator,");
            sql.append("channel.create_date chan_create_date,");
            sql.append("channel.refresh_flag chan_refresh_flag,");
            sql.append("channel.page_num chan_page_num ");
            sql.append("from t_ip_tree_frame tree,t_ip_channel channel ");
            sql.append("where path like ? and tree.tree_level=? and tree.path=channel.channel_path");
            sql.append(" order by channel.order_no");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, siteChannelPath + "%");
            preparedStatement.setInt(2, siteChannelPath.length() / 5);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Channel channel = new Channel();
                channel.setAsciiName(resultSet.getString("chan_ascii_name"));
                channel.setChannelID(resultSet.getInt("chan_id"));
                channel.setChannelType(resultSet.getString("chan_type"));
                channel.setCompletePath("");
                channel.setCreateDate(resultSet.getTimestamp("chan_create_date"));
                channel.setCreator(resultSet.getInt("chan_creator"));
                channel.setDataUrl(resultSet.getString("chan_data_url"));
                channel.setDesc(resultSet.getString("chan_description"));
                channel.setName(resultSet.getString("chan_name"));
                channel.setOrderNo(resultSet.getInt("chan_order_no"));
                channel.setRefresh(resultSet.getString("chan_refresh_flag"));
                channel.setSiteId(resultSet.getInt("chan_site_id"));
                channel.setStyle(resultSet.getString("chan_style"));
                channel.setTemplateId(resultSet.getString("chan_template_id"));
                channel.setUseStatus(resultSet.getString("chan_use_status"));
                channel.setDescription(resultSet.getString("tree_description"));
                channel.setIcon(resultSet.getString("tree_icon"));
                channel.setId(resultSet.getString("tree_id"));
                channel.setLevel(resultSet.getInt("tree_tree_level"));
                channel.setParentId(resultSet.getString("tree_parent_id"));
                channel.setPath(resultSet.getString("tree_path"));
                channel.setRefPath(resultSet.getString("tree_ref_path"));
                channel.setTitle(resultSet.getString("tree_title"));
                channel.setType(resultSet.getString("tree_type"));
                channel.setPageNum(resultSet.getInt("chan_page_num"));
                list.add(channel);
            }
        } catch (SQLException ex) {
            log.error("�õ�Ƶ���б����!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        TreeNode[] result = null;
        if (list.size() > 0) {
            result = new Channel[list.size()];
        }
        for (int i = 0; i < list.size(); i++) {
            result[i] = (TreeNode) list.get(i);
        }
        return result;
    }

    /**
	 * �õ�һ��path���浥���ĵ����͵��б� @param startPath ֻ�����ĵ����͵�path,�����Ǹ� @return
	 */
    private TreeNode[] getDocTypeList(String startPath) throws Exception {
        List list = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("doctype.id doctype_id,").append("doctype.name doctype_name,").append("doctype.description doctype_description,").append("doctype.ascii_name doctype_ascii_name,").append("doctype.category doctype_category,").append("doctype.input_template_id doctype_input_template_id,").append("doctype.show_template_id doctype_show_template_id,").append("doctype.content_template_id doctype_content_template_id,").append("doctype.use_status doctype_use_status,").append("doctype.order_no doctype_order_no,").append("doctype.creator doctype_creator,").append("doctype.create_date doctype_create_date,").append("doctype.process_id doctype_process_id").append(" from t_ip_tree_frame tree,t_ip_doc_type doctype ").append(" where path like ? and tree.tree_level=? and tree.path=doctype.doctype_path order by path");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, startPath + "%");
            preparedStatement.setInt(2, startPath.length() / 5);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                DocType docType = new DocType();
                docType.setAsciiName(resultSet.getString("docType_Ascii_Name"));
                docType.setCategory(resultSet.getString("docType_Category"));
                docType.setContentTemplateID(resultSet.getInt("docType_Content_Template_Id"));
                docType.setCreateDate(resultSet.getTimestamp("docType_Create_Date"));
                docType.setCreator(resultSet.getInt("docType_Creator"));
                docType.setDocTypeID(resultSet.getInt("doctype_id"));
                docType.setInputTemplateID(resultSet.getInt("docType_Input_Template_Id"));
                docType.setName(resultSet.getString("docType_Name"));
                docType.setProcessId(resultSet.getInt("docType_Process_Id"));
                docType.setShowTemplateID(resultSet.getInt("docType_Show_Template_Id"));
                docType.setUseStatus(resultSet.getInt("docType_Use_Status") == 1 ? true : false);
                docType.setDescription(resultSet.getString("tree_description"));
                docType.setIcon(resultSet.getString("tree_icon"));
                docType.setId(resultSet.getString("tree_id"));
                docType.setLevel(resultSet.getInt("tree_tree_level"));
                docType.setParentId(resultSet.getString("tree_parent_id"));
                docType.setPath(resultSet.getString("tree_path"));
                docType.setRefPath(resultSet.getString("tree_ref_path"));
                docType.setTitle(resultSet.getString("tree_title"));
                docType.setType(resultSet.getString("tree_type"));
                list.add(docType);
            }
        } catch (SQLException ex) {
            log.error("�õ��ĵ��б����!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        TreeNode[] result = null;
        if (list.size() > 0) {
            result = new DocType[list.size()];
        }
        for (int i = 0; i < list.size(); i++) {
            result[i] = (TreeNode) list.get(i);
        }
        return result;
    }

    /**
	 * �����path�õ���path���Ӧ�Ľڵ�����:
	 * ��3�ַ���ֵ:վ������,Ƶ������,�ĵ����͵�����
	 * ��3�����Ͷ�Ӧ��TreeNode��3�������
	 * @param path
	 * @return
	 * @throws Exception
	 */
    private int getTreeNodeType(String path) throws Exception {
        if (path == null) {
            return TreeNode.TREENODE_TYPE_NON;
        }
        if (path.length() == 5) {
            return TreeNode.TREENODE_TYPE_ROOT;
        }
        int result = TreeNode.TREENODE_TYPE_NON;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            String sql = "select type from t_ip_tree_frame where path=?";
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, path);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String treeNodeType = resultSet.getString(1);
                if (treeNodeType != null && treeNodeType.trim().equalsIgnoreCase("site")) {
                    result = TreeNode.TREENODE_TYPE_SITE;
                } else if (treeNodeType != null && (treeNodeType.trim().equalsIgnoreCase("chan")) || (treeNodeType.trim().equalsIgnoreCase("ds"))) {
                    result = TreeNode.TREENODE_TYPE_CHANNEL;
                } else if (treeNodeType != null && treeNodeType.trim().equalsIgnoreCase("type")) {
                    result = TreeNode.TREENODE_TYPE_DOCTYPE;
                } else {
                    throw new Exception("�޷�ȷ��path=" + path + "������!path�����ͷǷ�:" + treeNodeType + "!");
                }
            } else {
                result = TreeNode.TREENODE_TYPE_NON;
            }
        } catch (Exception e) {
            log.error("���path�õ��ڵ�����ʧ��,path=(" + path + ")!", e);
            throw new Exception("path�õ��ڵ�����ʧ��,path=(" + path + ")!����ԭ��:" + e.toString());
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * ���һ��path�õ�һ��վ��ʵ��
	 * 
	 * @param path
	 *            ��path��Ӧ��һ��Ҫ��Site����
	 * @return ���û���ҵ�,return null
	 */
    private Site getSiteInstance(String path) throws Exception {
        Site result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("site.id site_id,").append("site.name site_name,").append("site.description site_description,").append("site.ascii_name site_ascii_name,").append("site.remark_number site_remark_number,").append("site.increment_index site_increment_index,").append("site.use_status site_use_status,").append("site.appserver_id site_appserver_id").append(" from t_ip_tree_frame tree,t_ip_site site").append(" where tree.path=site.site_path and path=? order by path");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, path);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                result = new Site();
                result.setAppserverID(resultSet.getInt("site_appserver_id"));
                result.setAsciiName(resultSet.getString("site_ascii_name"));
                result.setIncrementIndex(resultSet.getString("site_increment_index"));
                result.setName(resultSet.getString("site_name"));
                result.setSiteID(resultSet.getInt("site_id"));
                result.setUseStatus(resultSet.getInt("site_use_status"));
                result.setDescription(resultSet.getString("tree_description"));
                result.setIcon(resultSet.getString("tree_icon"));
                result.setId(resultSet.getString("tree_id"));
                result.setLevel(resultSet.getInt("tree_tree_level"));
                result.setParentId(resultSet.getString("tree_parent_id"));
                result.setPath(resultSet.getString("tree_path"));
                result.setRefPath(resultSet.getString("tree_ref_path"));
                result.setTitle(resultSet.getString("tree_title"));
                result.setType(resultSet.getString("tree_type"));
            }
        } catch (SQLException ex) {
            log.error("�õ�վ��ʵ�����!path=" + path, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * ���һ��path�õ�һ��Ƶ��ʵ��
	 * 
	 * @param path
	 *            ��path��Ӧ��һ��Ҫ��Channel����
	 * @return ���û���ҵ�,return null
	 */
    private Channel getChannelInstance(String path) throws Exception {
        Channel result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("channel.id chan_id,").append("channel.name chan_name,").append("channel.description chan_description,").append("channel.ascii_name chan_ascii_name,").append("channel.site_id chan_site_id,").append("channel.type chan_type,").append("channel.data_url chan_data_url,").append("channel.template_id chan_template_id,").append("channel.use_status chan_use_status,").append("channel.order_no chan_order_no,").append("channel.style chan_style,").append("channel.creator chan_creator,").append("channel.create_date chan_create_date,").append("channel.refresh_flag chan_refresh_flag, ").append("channel.page_num page_num ").append("from t_ip_tree_frame tree,t_ip_channel channel ").append("where path = ? and tree.path=channel.channel_path order by path");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, path);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                result = new Channel();
                result.setAsciiName(resultSet.getString("chan_ascii_name"));
                result.setChannelID(resultSet.getInt("chan_id"));
                result.setChannelType(resultSet.getString("chan_type"));
                result.setCompletePath("");
                result.setCreateDate(resultSet.getTimestamp("chan_create_date"));
                result.setCreator(resultSet.getInt("chan_creator"));
                result.setDataUrl(resultSet.getString("chan_data_url"));
                result.setDesc(resultSet.getString("chan_description"));
                result.setName(resultSet.getString("chan_name"));
                result.setOrderNo(resultSet.getInt("chan_order_no"));
                result.setRefresh(resultSet.getString("chan_refresh_flag"));
                result.setSiteId(resultSet.getInt("chan_site_id"));
                result.setStyle(resultSet.getString("chan_style"));
                result.setTemplateId(resultSet.getString("chan_template_id"));
                result.setUseStatus(resultSet.getString("chan_use_status"));
                result.setDescription(resultSet.getString("tree_description"));
                result.setIcon(resultSet.getString("tree_icon"));
                result.setId(resultSet.getString("tree_id"));
                result.setLevel(resultSet.getInt("tree_tree_level"));
                result.setParentId(resultSet.getString("tree_parent_id"));
                result.setPath(resultSet.getString("tree_path"));
                result.setRefPath(resultSet.getString("tree_ref_path"));
                result.setTitle(resultSet.getString("tree_title"));
                result.setType(resultSet.getString("tree_type"));
                result.setPageNum(resultSet.getInt("page_num"));
            }
        } catch (SQLException ex) {
            log.error("�õ�Ƶ��ʵ�����!path=" + path, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * ���һ��path�õ�һ��������ʵ��
	 * 
	 * @param path
	 *            ��path��Ӧ��һ��Ҫ�Ǹ�ڵ�
	 * @return ���û���ҵ�,return null
	 */
    private TreeNode getRootInstance(String path) throws Exception {
        TreeNode result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon").append(" from t_ip_tree_frame tree ").append(" where path = ? ");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, path);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                result = new TreeNode();
                result.setDescription(resultSet.getString("tree_description"));
                result.setIcon(resultSet.getString("tree_icon"));
                result.setId(resultSet.getString("tree_id"));
                result.setLevel(resultSet.getInt("tree_tree_level"));
                result.setParentId(resultSet.getString("tree_parent_id"));
                result.setPath(resultSet.getString("tree_path"));
                result.setRefPath(resultSet.getString("tree_ref_path"));
                result.setTitle(resultSet.getString("tree_title"));
                result.setType(resultSet.getString("tree_type"));
            }
        } catch (SQLException ex) {
            log.error("�õ���ڵ�ʵ�����!path=" + path, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * ���һ��path�õ�һ���ĵ�����ʵ��
	 * 
	 * @param path
	 *            ��path��Ӧ��һ��Ҫ���ĵ�����
	 * @return ���û���ҵ�,return null
	 */
    private DocType getDocTypeInstance(String path) throws Exception {
        DocType result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append("select ").append("tree.id tree_id,").append("tree.parent_id tree_parent_id,").append("tree.path tree_path,").append("tree.ref_path tree_ref_path,").append("tree.tree_level tree_tree_level,").append("tree.type tree_type,").append("tree.title tree_title,").append("tree.order_no tree_order_no,").append("tree.description tree_description,").append("tree.style tree_style,").append("tree.icon tree_icon,").append("doctype.id doctype_id,").append("doctype.name doctype_name,").append("doctype.description doctype_description,").append("doctype.ascii_name doctype_ascii_name,").append("doctype.category doctype_category,").append("doctype.input_template_id doctype_input_template_id,").append("doctype.show_template_id doctype_show_template_id,").append("doctype.content_template_id doctype_content_template_id,").append("doctype.use_status doctype_use_status,").append("doctype.order_no doctype_order_no,").append("doctype.creator doctype_creator,").append("doctype.create_date doctype_create_date,").append("doctype.process_id doctype_process_id").append(" from t_ip_tree_frame tree,t_ip_doc_type doctype ").append(" where path = ? and tree.path=doctype.doctype_path order by path");
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql.toString());
            preparedStatement.setString(1, path);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                result = new DocType();
                result.setAsciiName(resultSet.getString("docType_Ascii_Name"));
                result.setCategory(resultSet.getString("docType_Category"));
                result.setContentTemplateID(resultSet.getInt("docType_Content_Template_Id"));
                result.setCreateDate(resultSet.getTimestamp("docType_Create_Date"));
                result.setCreator(resultSet.getInt("docType_Creator"));
                result.setDocTypeID(resultSet.getInt("doctype_id"));
                result.setInputTemplateID(resultSet.getInt("docType_Input_Template_Id"));
                result.setName(resultSet.getString("docType_Name"));
                result.setProcessId(resultSet.getInt("docType_Process_Id"));
                result.setShowTemplateID(resultSet.getInt("docType_Show_Template_Id"));
                result.setUseStatus(resultSet.getInt("docType_Use_Status") == 1 ? true : false);
                result.setDescription(resultSet.getString("tree_description"));
                result.setIcon(resultSet.getString("tree_icon"));
                result.setId(resultSet.getString("tree_id"));
                result.setLevel(resultSet.getInt("tree_tree_level"));
                result.setParentId(resultSet.getString("tree_parent_id"));
                result.setPath(resultSet.getString("tree_path"));
                result.setRefPath(resultSet.getString("tree_ref_path"));
                result.setTitle(resultSet.getString("tree_title"));
                result.setType(resultSet.getString("tree_type"));
            }
        } catch (SQLException ex) {
            log.error("�õ��ĵ�����ʵ�����!path=" + path, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * @param a
	 * @throws Exception
	 */
    public static void main(String[] a) throws Exception {
        TreeNode[] tn = new TreeNodeDAO().getSiteChannelTree(102, "1");
        System.out.println(tn.length);
        for (int i = 0; i < tn.length; i++) {
        }
    }
}
