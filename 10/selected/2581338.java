package ces.platform.infoplat.core.dao;

import ces.platform.infoplat.core.base.BaseDAO;
import ces.platform.infoplat.core.tree.*;
import java.util.*;
import ces.platform.infoplat.core.*;
import ces.coral.dbo.*;
import java.sql.*;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class SiteChannelDocTypeRelationDAO extends BaseDAO {

    /**
     * ���ӹ�ϵ(��װվ��Ƶ��path������)
     * վ��Ƶ��path���ĵ�����path������1�Զ�Ĺ�ϵ
     * @param siteChannelPath վ��Ƶ��path
     * @param docTypePaths �ĵ�����path����
     */
    public void addBySiteChannelPath(String siteChannelPath, String[] docTypePaths) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            dbo.delete("delete from t_ip_doctype_channel where chan_path='" + siteChannelPath + "'");
            for (int i = 0; i < docTypePaths.length; i++) {
                dbo = createDBOperation();
                dbo.insert("insert into t_ip_doctype_channel(doctype_path,chan_path) values('" + docTypePaths[i] + "','" + siteChannelPath + "')");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * ���ӹ�ϵ(��װվ��Ƶ��path������)
     * վ��Ƶ��path���ĵ�����path������1�Զ�Ĺ�ϵ
     * @param siteChannelPath վ��Ƶ��path
     * @param docTypePaths �ĵ�����path����
     */
    public void addBySiteChannelPath(String siteChannelPath, String[] docTypePaths, String[] showTemplateIds) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            String sql = "delete from t_ip_doctype_channel where chan_path='" + siteChannelPath + "'";
            connection.createStatement().executeUpdate(sql);
            sql = "insert into t_ip_doctype_channel(doctype_path,chan_path,show_template_id) values(?,'" + siteChannelPath + "',?)";
            preparedStatement = connection.prepareStatement(sql);
            for (int i = 0; i < docTypePaths.length; i++) {
                preparedStatement.setString(1, docTypePaths[i]);
                String temp = showTemplateIds != null && i < showTemplateIds.length ? showTemplateIds[i] : "null";
                if (temp == null || temp.trim().equals("") || temp.trim().equalsIgnoreCase("null")) {
                    preparedStatement.setInt(2, Types.NULL);
                } else {
                    preparedStatement.setInt(2, Integer.parseInt(temp));
                }
                preparedStatement.executeUpdate();
            }
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
            ex.printStackTrace();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * ���վ��Ƶ��path���ĵ�����path,��ѯ�����Ӧ����ʾģ���id
     * @param siteChannelPath վ��Ƶ��path
     * @param docTypePath �ĵ�����path
     * @return
     */
    public int getShowTemplateId(String channelPath, String docTypePath) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int result = -1;
        try {
            dbo = createDBOperation();
            resultSet = dbo.select("select show_template_id from t_ip_doctype_channel where chan_path='" + channelPath + "' and doctype_path='" + docTypePath + "'");
            if (resultSet.next()) {
                String templateId = resultSet.getString(1);
                if (templateId != null && !templateId.trim().equals("")) {
                    result = Integer.parseInt(templateId.trim());
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * ���ӹ�ϵ(��װ�ĵ�����path������)
     * �ĵ�����path��վ��Ƶ��path������1�Զ�Ĺ�ϵ
     * @param docTypePath �ĵ�����path
     * @param siteChannelPaths վ��Ƶ��path����
     */
    public void addByDocTypePath(String docTypePath, String[] siteChannelPaths) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement ps1 = null, ps2;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            String sql = "select chan_path from t_ip_doctype_channel where doctype_path='" + docTypePath + "'";
            ps1 = connection.prepareStatement(sql);
            resultSet = ps1.executeQuery();
            HashSet hs1 = new HashSet();
            int i;
            while (resultSet.next()) {
                hs1.add(resultSet.getString("chan_path"));
            }
            for (i = 0; i < siteChannelPaths.length; i++) {
                if (hs1.contains(siteChannelPaths[i])) {
                    hs1.remove(siteChannelPaths[i]);
                    siteChannelPaths[i] = "";
                }
            }
            String[] dels = (String[]) hs1.toArray(new String[] {});
            sql = "";
            if (dels != null && dels.length > 0) {
                for (i = 0; i < dels.length; i++) {
                    sql += ",'" + dels[i] + "'";
                }
                sql = "delete from t_ip_doctype_channel where doctype_path='" + docTypePath + "' and chan_path in (" + sql.substring(1) + ") ";
                ps1 = connection.prepareStatement(sql);
                ps1.executeUpdate();
            }
            sql = "insert into t_ip_doctype_channel(doctype_path,chan_path) values('" + docTypePath + "',?)";
            ps2 = connection.prepareStatement(sql);
            for (i = 0; i < siteChannelPaths.length; i++) {
                if (!"".equals(siteChannelPaths[i])) {
                    ps2.setString(1, siteChannelPaths[i]);
                    ps2.executeUpdate();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("���ӹ�ϵ(��װ�ĵ�����path������),ʧ��!");
            throw ex;
        } finally {
            close(resultSet, null, ps1, connection, dbo);
        }
    }

    /**
     * ���վ��Ƶ��path,��ѯ�����Ӧ���ĵ����Ͷ���ļ���
     * @param siteChannelPath վ��Ƶ��path
     * @param isFindParentNode �Ƿ���Ҹ��ڵ�  true:����  false:������
     * @return
     */
    public TreeNode[] getDocTypes(String siteChannelPath, boolean isFindParentNode) throws Exception {
        String[] paths = getDocTypePaths(siteChannelPath, isFindParentNode);
        if (paths == null) {
            return new TreeNode[0];
        }
        TreeNode[] result = new DocType[paths.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = TreeNode.getInstance(paths[i]);
        }
        return result;
    }

    /**
     * ���վ��Ƶ��path,��ѯ�����Ӧ���ĵ����Ͷ���ļ���
     * @param siteChannelPath վ��Ƶ��path
     * @param isFindParentNode �Ƿ���Ҹ��ڵ�  true:����  false:������
     * @return
     */
    public String[] getDocTypePaths(String siteChannelPath, boolean isFindParentNode) throws Exception {
        if (siteChannelPath == null) {
            return null;
        }
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List list = new ArrayList();
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            if (isFindParentNode) {
                while (siteChannelPath.length() >= 5) {
                    preparedStatement = connection.prepareStatement("select doctype_path from t_ip_doctype_channel where chan_path='" + siteChannelPath + "'");
                    resultSet = preparedStatement.executeQuery();
                    boolean repeat = true;
                    while (resultSet.next()) {
                        repeat = false;
                        list.add(resultSet.getString(1));
                    }
                    if (repeat) {
                        siteChannelPath = siteChannelPath.substring(0, siteChannelPath.length() - 5);
                    } else {
                        break;
                    }
                }
            } else {
                resultSet = dbo.select("select doctype_path from t_ip_doctype_channel where chan_path='" + siteChannelPath + "'");
                while (resultSet.next()) {
                    list.add(resultSet.getString(1));
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        String[] result = new String[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (String) list.get(i);
        }
        return result;
    }

    /**
     * ���վ��Ƶ��path,��ѯ�����Ӧ���ĵ����Ͷ���ļ���
     * @param siteChannelPath վ��Ƶ��path
     * @param isFindParentNode �Ƿ���Ҹ��ڵ�  true:����  false:������
     * @return
     */
    public HashMap getDocTypePathsAndShowTemplateIds(String siteChannelPath, boolean isFindParentNode) throws Exception {
        if (siteChannelPath == null) {
            return null;
        }
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        HashMap result = new HashMap();
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            if (isFindParentNode) {
                while (siteChannelPath.length() >= 5) {
                    preparedStatement = connection.prepareStatement("select doctype_path,show_template_id from t_ip_doctype_channel where chan_path='" + siteChannelPath + "'");
                    resultSet = preparedStatement.executeQuery();
                    boolean repeat = true;
                    while (resultSet.next()) {
                        repeat = false;
                        result.put(resultSet.getString(1), resultSet.getString(2));
                    }
                    if (repeat) {
                        siteChannelPath = siteChannelPath.substring(0, siteChannelPath.length() - 5);
                    } else {
                        break;
                    }
                }
            } else {
                resultSet = dbo.select("select doctype_path,show_template_id from t_ip_doctype_channel where chan_path='" + siteChannelPath + "'");
                while (resultSet.next()) {
                    result.put(resultSet.getString(1), resultSet.getString(2));
                }
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * ����ĵ�����path,��ѯ�����Ӧ��վ��Ƶ������ļ���
     * @param docTypPath �ĵ�����path
     * @param isFindParentNode �Ƿ���Ҹ��ڵ�  true:����  false:������
     * @return
     */
    public TreeNode[] getSiteChannels(String docTypPath, boolean isFindParentNode) throws Exception {
        String[] paths = getSiteChannelPaths(docTypPath, isFindParentNode);
        TreeNode[] result = new TreeNode[paths.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = TreeNode.getInstance(paths[i]);
        }
        return result;
    }

    /**
     * ����ĵ�����path,��ѯ�����Ӧ��վ��Ƶ������ļ���
     * @param docTypPath �ĵ�����path
     * @param isFindParentNode �Ƿ���Ҹ��ڵ�  true:����  false:������
     * @return
     */
    public String[] getSiteChannelPaths(String docTypPath, boolean isFindParentNode) throws Exception {
        if (docTypPath == null) {
            return new String[0];
        }
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        List list = new ArrayList();
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            resultSet = dbo.select("select chan_path from t_ip_doctype_channel where doctype_path='" + docTypPath + "'");
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        String[] result = new String[list.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (String) list.get(i);
        }
        return result;
    }

    /**
     * ɾ���ϵ(���վ��Ƶ��·��ɾ���Ӧ���ĵ�����)
     * վ��Ƶ�����ĵ�������һ�Զ�Ĺ�ϵ
     * @param siteChannelPath
     */
    public void deleteDocTypes(String siteChannelPath) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            dbo.delete("delete from t_ip_doctype_channel where chan_path='" + siteChannelPath + "'");
        } catch (Exception ex) {
            log.error("ɾ���ϵ(���վ��Ƶ��ɾ���Ӧ���ĵ�����),ʧ��!");
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * ɾ���ϵ(����ĵ�����·��ɾ���Ӧ��վ��Ƶ��)
     * �ĵ����ͺ�վ��Ƶ����һ�Զ�Ĺ�ϵ
     * @param docTypePath
     */
    public void deleteSiteChannels(String docTypePath) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            dbo.delete("delete from t_ip_doctype_channel where doctype_path='" + docTypePath + "'");
        } catch (Exception ex) {
            log.error("ɾ���ϵ(����ĵ�����ɾ���Ӧ��վ��Ƶ��),ʧ��!");
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }
}
