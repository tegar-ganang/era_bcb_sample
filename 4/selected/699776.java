package ces.platform.infoplat.core.dao;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.htmlparser.util.ParserException;
import ces.coral.dbo.DBOperation;
import ces.coral.dbo.DBOperationFactory;
import ces.coral.dbo.ERDBOperationFactory;
import ces.coral.file.CesGlobals;
import ces.platform.infoplat.core.Channel;
import ces.platform.infoplat.core.DocumentPublish;
import ces.platform.infoplat.core.Site;
import ces.platform.infoplat.core.Template;
import ces.platform.infoplat.core.base.BaseDAO;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.base.Const;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.taglib.ds.document.FlipPageTag;
import ces.platform.infoplat.ui.template.TemplateHtmlParser;
import ces.platform.infoplat.ui.website.ChannelHtmlParser;
import ces.platform.infoplat.utils.Function;
import ces.platform.infoplat.utils.http.GenHtml;
import ces.platform.system.common.Constant;
import ces.platform.system.common.XmlInfo;
import ces.platform.system.facade.AuthorityManager;
import ces.platform.system.facade.StructAuth;
import ces.platform.system.facade.StructResource;

/**
 * Description of the Class
 * 
 * @Title ��Ϣƽ̨
 * @Company �Ϻ�������Ϣ��չ���޹�˾
 * 
 * @version 2.5
 * @author ���� ������ ֣��ǿ
 * @created 2004��2��12��
 */
public class ChannelDAO extends BaseDAO {

    public ChannelDAO() {
    }

    /**
	 * ͨ��Ƶ��ID�õ�Ƶ��
	 * 
	 * @param channelId
	 * @return Ƶ������
	 * @exception Exception
	 *                Description of the Exception
	 */
    public Channel getInstance(int channelId) throws Exception {
        String sql = new StringBuffer().append(" select channel.*").append(",site.ascii_name site_ascii_name").append(" from t_ip_channel channel").append(",t_ip_site site").append(" where channel.id=" + channelId).append(" and channel.site_id = site.id").toString();
        Channel channel = getInstance0(sql);
        return channel;
    }

    /**
	 * ͨ��·���õ�Ƶ��
	 * 
	 * @param path
	 * @return Ƶ������
	 * @exception Exception
	 *                Description of the Exception
	 */
    public Channel getInstanceByPath(String path) throws Exception {
        String sql = new StringBuffer().append(" select channel.*").append(",site.ascii_name site_ascii_name").append(" from t_ip_channel channel").append(",t_ip_site site").append(" where channel.channel_path='" + path + "'").append(" and channel.site_id = site.id").toString();
        Channel channel = getInstance0(sql);
        return channel;
    }

    /**
	 * @param channelId
	 * @return
	 */
    private Channel getInstance0(String sql) throws Exception {
        Channel channel = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            if (rs != null) if (rs.next()) {
                channel = new Channel(rs.getInt("id"));
                channel.setAsciiName(rs.getString("ascii_name"));
                channel.setChannelType(rs.getString("type"));
                channel.setCreateDate(rs.getDate("create_date"));
                channel.setCreator(rs.getInt("creator"));
                channel.setDataUrl(rs.getString("data_url"));
                channel.setDesc(rs.getString("description"));
                channel.setName(rs.getString("name"));
                channel.setOrderNo(rs.getInt("order_no"));
                channel.setSiteId(rs.getInt("site_id"));
                channel.setStyle(rs.getString("style"));
                channel.setTemplateId(rs.getString("template_id"));
                channel.setUseStatus(rs.getString("use_status"));
                channel.setPath(rs.getString("channel_path"));
                channel.setRefresh(rs.getString("refresh_flag"));
                channel.setSiteAsciiName(rs.getString("site_ascii_name"));
                channel.setPageNum(rs.getInt("page_num"));
            }
        } catch (Exception e) {
            log.error("�õ�Ƶ��ʵ��ʱ����", e);
            throw e;
        } finally {
            close(rs, null, preparedStatement, connection, dbo);
        }
        return channel;
    }

    /**
	 * ���Ƶ��Path�õ���Ƶ����һ��Ƶ���б�
	 * 
	 * @param parentPath
	 * @return Ƶ�������б�
	 */
    public List getChannelsList(String parentPath) throws Exception {
        List channels = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer(" select channel.id ");
            sql.append(" from t_ip_channel channel,t_ip_tree_frame tree ");
            sql.append(" where channel.channel_path = tree.path ");
            sql.append(" and channel.channel_path like '");
            sql.append(parentPath).append("%' ");
            sql.append(" and tree.tree_level = ");
            sql.append(parentPath.length() / 5);
            ResultSet rs1 = dbo.select(sql.toString());
            while (rs1.next()) {
                String sql0 = new StringBuffer().append(" select channel.*").append(",site.ascii_name site_ascii_name").append(" from t_ip_channel channel").append(",t_ip_site site").append(" where channel.id=" + rs1.getInt("id")).append(" and channel.site_id = site.id").toString();
                channels.add(getInstance0(sql0));
            }
        } catch (Exception e) {
            log.error("�õ�Ƶ���б�ʱ����", e);
            throw e;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return channels;
    }

    /**
	 * ���Ƶ��Path�õ���Ƶ����һ��Ƶ������
	 * 
	 * @param parentPath
	 * @return Ƶ����������
	 */
    public Channel[] getChannels(String parentPath) throws Exception {
        List channelsList = getChannelsList(parentPath);
        Channel[] channels = new Channel[channelsList.size()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = (Channel) channelsList.get(i);
        }
        return channels;
    }

    /**
	 * ���Ƶ��ID�õ���Ƶ�����ѷ����ĵ��б�
	 * 
	 * @param chanelId
	 * @return �ѷ����ĵ��б�
	 */
    public List getChannelDocuments(String chanelId) throws Exception {
        List documents = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer(" select browse.* ");
            sql.append(" from t_ip_channel channel,t_ip_browse browse ");
            sql.append(" where channel.id = ");
            sql.append(chanelId);
            sql.append(" and channel.channel_path=browse.channel_path ");
            ResultSet rs = dbo.select(sql.toString());
            while (rs.next()) {
                DocumentPublish doc = new DocumentPublish();
                doc.setAbstractWords(rs.getString("doc_abstract"));
                doc.setAttachStatus(rs.getString("attach_state"));
                doc.setAuthor(rs.getString("author"));
                doc.setChannelPath(rs.getString("channel_path"));
                doc.setContentFile(rs.getString("content_file"));
                doc.setCreateDate(rs.getTimestamp("create_date"));
                doc.setCreater(rs.getInt("creater"));
                doc.setDoctypePath(rs.getString("doctype_path"));
            }
        } catch (Exception e) {
            log.error("�õ�Ƶ���б�ʱ����", e);
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return documents;
    }

    /**
	 * �Ѵ�Ƶ�����ݲ�����ݿ� ���Ƿ񴴽��ļ���
	 * 
	 * @param channel
	 *            Ƶ��ʵ��
	 */
    public void add(Channel channel, boolean createFolder) throws Exception {
        if (createFolder) {
            if (cfg == null) {
                cfg = ConfigInfo.getInstance();
            }
            String pFilePath = cfg.getInfoplatDataDir() + "pub/" + channel.getSiteAsciiName() + "/" + channel.getAsciiName();
            createFolder(pFilePath);
        }
        this.add(channel);
    }

    /**
	 * �Ѵ�Ƶ�����ݲ�����ݿ�
	 * 
	 * @param channel
	 *            Ƶ��ʵ��
	 */
    public void add(Channel channel) throws Exception {
        String sqlStr = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            sqlStr = "insert into t_ip_channel (id,name,description,ascii_name,channel_path,site_id,type,data_url,template_id,use_status,order_no,style,creator,create_date,refresh_flag,page_num) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            String[] path = new String[1];
            path[0] = channel.getPath();
            selfDefineAdd(path, channel, connection, preparedStatement);
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setInt(1, channel.getChannelID());
            preparedStatement.setString(2, channel.getName());
            preparedStatement.setString(3, channel.getDescription());
            preparedStatement.setString(4, channel.getAsciiName());
            preparedStatement.setString(5, channel.getPath());
            preparedStatement.setInt(6, channel.getSiteId());
            preparedStatement.setString(7, channel.getChannelType());
            preparedStatement.setString(8, channel.getDataUrl());
            if (channel.getTemplateId() == null || channel.getTemplateId().trim().equals("")) preparedStatement.setNull(9, Types.INTEGER); else preparedStatement.setInt(9, Integer.parseInt(channel.getTemplateId()));
            preparedStatement.setString(10, channel.getUseStatus());
            preparedStatement.setInt(11, channel.getOrderNo());
            preparedStatement.setString(12, channel.getStyle());
            preparedStatement.setInt(13, channel.getCreator());
            preparedStatement.setTimestamp(14, (Timestamp) channel.getCreateDate());
            preparedStatement.setString(15, channel.getRefPath());
            preparedStatement.setInt(16, channel.getPageNum());
            preparedStatement.executeUpdate();
            connection.commit();
            int operateTypeID = Const.OPERATE_TYPE_ID;
            int resID = channel.getChannelID() + Const.CHANNEL_TYPE_RES;
            String resName = channel.getName();
            int resTypeID = Const.RES_TYPE_ID;
            String remark = "";
            AuthorityManager am = new AuthorityManager();
            am.createExtResource(Integer.toString(resID), resName, resTypeID, operateTypeID, remark);
        } catch (SQLException ex) {
            connection.rollback();
            log.error("���Ƶ��ʱSql�쳣��ִ����䣺" + sqlStr);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
	 * @param channel
	 * @param connection
	 * @param preparedStatement
	 * @return
	 * @throws SQLException
	 */
    private void selfDefineAdd(String[] path, Channel channel, Connection connection, PreparedStatement preparedStatement) throws SQLException {
        if (path == null) {
            path = new String[0];
        }
        List selfDefine = channel.getSelfDefineList();
        try {
            for (int j = 0; j < path.length; j++) {
                for (int i = 0; i < selfDefine.size(); i++) {
                    Map map = (Map) selfDefine.get(i);
                    preparedStatement = connection.prepareStatement("insert into t_ip_fieldset (path," + " field,field_name,order_no) values (?,?,?,?)  ");
                    preparedStatement.setString(1, path[j]);
                    preparedStatement.setString(2, (String) map.get("field"));
                    preparedStatement.setString(3, (String) map.get("field_name"));
                    preparedStatement.setInt(4, Integer.parseInt((String) map.get("order_no")));
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("��������վ���Զ����ֶ�ʧ�ܣ�");
            throw e;
        }
    }

    public Map initAddChannelSelfDefine(String path) {
        String splitTag = "#";
        String sqlStr = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map map = new HashMap();
        try {
            sqlStr = "select field,field_name,order_no from t_ip_fieldset where path = ?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setString(1, path);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                map.put(resultSet.getString(1).toUpperCase(), resultSet.getString(3) + splitTag + resultSet.getString(2));
            }
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return map;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    public List getSelfDefine(String ChannelPath) {
        ArrayList alReturn = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        ResultSet resultSet = null;
        Map map = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            resultSet = connection.createStatement().executeQuery("select field,field_name,order_no from t_ip_fieldset where path = '" + ChannelPath + "'");
            map = new HashMap();
            while (resultSet.next()) {
                map.put("path", ChannelPath);
                map.put("field", resultSet.getString(1));
                map.put("field_name", resultSet.getString(2));
                map.put("order_no", resultSet.getString(3));
                alReturn.add(map);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(resultSet, null, null, connection, dbo);
        }
        return alReturn;
    }

    /**
	 * ɾ����ݿ��еĴ�ʵ�� �Ƿ�ɾ���ļ���
	 * 
	 * @param channel
	 *            Ҫɾ���Ƶ����
	 * @param deleteFolder
	 *            �Ƿ�ɾ���ļ��� Ƶ��ʵ��
	 */
    public void delete(Channel channel, boolean deleteFolder) throws Exception {
        delete(channel);
        if (deleteFolder) {
            if (cfg == null) {
                cfg = ConfigInfo.getInstance();
            }
            String pFilePath = cfg.getInfoplatDataDir() + "pub/" + channel.getSiteAsciiName() + "/" + channel.getAsciiName();
            deleteFolder(pFilePath);
        }
    }

    /**
	 * ɾ����ݿ��еĴ�ʵ��
	 * 
	 * @param channel
	 *            Ƶ��ʵ��
	 */
    public void delete(Channel channel) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            String[] selfDefinePath = getSelfDefinePath(channel.getPath(), "1", connection, preparedStatement, resultSet);
            selfDefineDelete(selfDefinePath, connection, preparedStatement);
            String sqlStr = "delete from t_ip_channel where channel_path=?";
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setString(1, channel.getPath());
            preparedStatement.executeUpdate();
            sqlStr = "delete from t_ip_channel_order where channel_order_site = ?";
            preparedStatement.setString(1, channel.getPath());
            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            log.error("ɾ��Ƶ��ʧ�ܣ�channelPath=" + channel.getPath(), ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
	 * ������ݿ��еĴ�ʵ��
	 * 
	 * @param createFolder
	 *            ���Ƿ񴴽��ļ���
	 * @param channel
	 *            Ƶ��ʵ��
	 * 
	 */
    public void update(Channel channel, boolean createFolder) throws Exception {
        if (createFolder) {
            if (cfg == null) {
                cfg = ConfigInfo.getInstance();
            }
            String pFilePath = cfg.getInfoplatDataDir() + "pub/" + channel.getSiteAsciiName() + "/" + channel.getAsciiName();
            createFolder(pFilePath);
        }
        this.update(channel);
    }

    /**
	 * ������ݿ��еĴ�ʵ��
	 * 
	 * @param channel
	 *            Ƶ��ʵ��
	 */
    public void update(Channel channel) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String exp = channel.getExtendParent();
        String path = channel.getPath();
        try {
            String sqlStr = "UPDATE t_ip_channel SET id=?,name=?,description=?,ascii_name=?,site_id=?,type=?,data_url=?,template_id=?,use_status=?,order_no=?,style=?,creator=?,create_date=?,refresh_flag=?,page_num=? where channel_path=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            String[] selfDefinePath = getSelfDefinePath(path, exp, connection, preparedStatement, resultSet);
            selfDefineDelete(selfDefinePath, connection, preparedStatement);
            selfDefineAdd(selfDefinePath, channel, connection, preparedStatement);
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setInt(1, channel.getChannelID());
            preparedStatement.setString(2, channel.getName());
            preparedStatement.setString(3, channel.getDescription());
            preparedStatement.setString(4, channel.getAsciiName());
            preparedStatement.setInt(5, channel.getSiteId());
            preparedStatement.setString(6, channel.getChannelType());
            preparedStatement.setString(7, channel.getDataUrl());
            if (channel.getTemplateId() == null || channel.getTemplateId().trim().equals("")) preparedStatement.setNull(8, Types.INTEGER); else preparedStatement.setInt(8, Integer.parseInt(channel.getTemplateId()));
            preparedStatement.setString(9, channel.getUseStatus());
            preparedStatement.setInt(10, channel.getOrderNo());
            preparedStatement.setString(11, channel.getStyle());
            preparedStatement.setInt(12, channel.getCreator());
            preparedStatement.setTimestamp(13, (Timestamp) channel.getCreateDate());
            preparedStatement.setString(14, channel.getRefresh());
            preparedStatement.setInt(15, channel.getPageNum());
            preparedStatement.setString(16, channel.getPath());
            preparedStatement.executeUpdate();
            connection.commit();
            int resID = channel.getChannelID() + Const.CHANNEL_TYPE_RES;
            StructResource sr = new StructResource();
            sr.setResourceID(Integer.toString(resID));
            sr.setOperateID(Integer.toString(1));
            sr.setOperateTypeID(Const.OPERATE_TYPE_ID);
            sr.setTypeID(Const.RES_TYPE_ID);
            StructAuth sa = new AuthorityManager().getExternalAuthority(sr);
            int authID = sa.getAuthID();
            if (authID == 0) {
                String resName = channel.getName();
                int resTypeID = Const.RES_TYPE_ID;
                int operateTypeID = Const.OPERATE_TYPE_ID;
                String remark = "";
                AuthorityManager am = new AuthorityManager();
                am.createExtResource(Integer.toString(resID), resName, resTypeID, operateTypeID, remark);
            }
        } catch (SQLException ex) {
            connection.rollback();
            log.error("����Ƶ��ʧ�ܣ�channelPath=" + channel.getPath());
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    private String[] getSelfDefinePath(String path, String exp, Connection con, PreparedStatement pstm, ResultSet rs) {
        if (exp == null || exp.trim().equals("0")) {
            String[] rv = { path };
            return rv;
        }
        List list = new ArrayList();
        String[] selfDefinePath;
        try {
            pstm = con.prepareStatement(" select channel_path from t_ip_channel where channel_path like '" + path + "%'  ");
            rs = pstm.executeQuery();
            while (rs.next()) {
                list.add(rs.getString(1));
            }
            selfDefinePath = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                selfDefinePath[i] = (String) list.get(i);
            }
        } catch (SQLException e) {
            selfDefinePath = new String[0];
            e.printStackTrace();
        }
        return selfDefinePath;
    }

    private void selfDefineDelete(String[] path, Connection con, PreparedStatement pstm) {
        try {
            pstm = con.prepareStatement("delete from t_ip_fieldset where path = ?");
            for (int i = 0; i < path.length; i++) {
                pstm.setString(1, path[i]);
                pstm.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
	 * ��ö����ļ���������Ϣ��������
	 * 
	 * @param template
	 * @return
	 */
    public boolean readDefineFile(Channel channel) {
        try {
            String strFilePath = channel.getDefineFilePath();
            String strFileName = channel.getDefineFileName();
            TemplateHtmlParser chp = null;
            chp = new TemplateHtmlParser(strFilePath + strFileName, strFilePath, channel.getAsciiName());
            chp.convertBackgroundBase(channel.getResBaseURL());
            channel.setDefineFileContent(new String(chp.getHtmlContent().getBytes("ISO-8859-1"), "GBK"));
        } catch (Exception ex1) {
            return false;
        }
        return true;
    }

    /**
	 * ���¶����ļ���������Ϣ��������
	 * 
	 * @param template
	 * @return
	 */
    public boolean updateDefineFile(Channel channel, String defineFileContent) {
        try {
            String strContent = defineFileContent;
            String strFilePath = channel.getDefineFilePath();
            String strFileName = channel.getDefineFileName();
            Function.writeTextFile(strContent, strFilePath + strFileName, true);
            TemplateHtmlParser chp = null;
            try {
                chp = new TemplateHtmlParser(strFilePath + strFileName, strFilePath, channel.getAsciiName());
                chp.downloadContentRes();
                strContent = chp.getHtmlContent();
            } catch (Exception ex1) {
            }
            Function.writeTextFile(strContent, strFilePath + strFileName, "ISO-8859-1", true);
        } catch (Exception ex) {
            log.error(Const.LOG_ERROR_DESC + "д�ļ�ʧ��", ex);
            return false;
        }
        return true;
    }

    /**
	 * ���CHDC�ļ���index_dc.jsp��
	 * 
	 * @param channel
	 * @return boolean �Ƿ�Ϊ��̬Ƶ��������Ƶ����û�ж�̬���Դģ��������
	 */
    public boolean genDCFile(Channel channel) {
        boolean blStatic = true;
        StringBuffer sbContent = new StringBuffer();
        sbContent.append("<%@ page contentType=\"text/html; charset=GBK\"%>\n");
        sbContent.append("<%@ taglib uri=\"http://www.cesgroup.com.cn/taglibs/infoplat\" prefix=\"ip\" %>");
        sbContent.append("<ip:dstrManage>");
        try {
            String strFilePath = channel.getDefineFilePath();
            String strFileName = channel.getDefineFileName();
            ChannelHtmlParser chp = null;
            chp = new ChannelHtmlParser(strFilePath + strFileName, strFilePath);
            blStatic = chp.replaceTagAndFilterProp();
            sbContent.append(new String(chp.getHtmlContent().getBytes("ISO-8859-1"), "GBK"));
        } catch (Exception ex1) {
        }
        sbContent.append("</ip:dstrManage>");
        try {
            String fileName = blStatic ? "index_dcs.jsp" : "index_dcd.jsp";
            Function.writeTextFile(sbContent.toString(), channel.getDefineFilePath() + fileName, true);
        } catch (Exception ex) {
        }
        return blStatic;
    }

    /**
	 * ����CHDC�ļ�����CHDC�ļ���TAG�����õķ��� �滻CHD�ļ��е����Դ���ñ�ǩ��PARAMS����ֵ����Ҫ������pathֵ��
	 * 
	 * @return
	 * @throws java.lang.Exception
	 */
    public boolean updateIndexDFile(Channel channel, HashMap hmParams) throws Exception {
        try {
            String strFilePath = channel.getDefineFilePath();
            String strFileName = channel.getDefineFileName();
            ChannelHtmlParser chp = null;
            chp = new ChannelHtmlParser(strFilePath + strFileName, strFilePath);
            String strContent = new String(chp.UpdateIndexDfileParams(hmParams).getBytes("ISO-8859-1"), "GBK");
            Function.writeTextFile(strContent, strFilePath + "index_d.html", true);
        } catch (ParserException ex) {
            log.error(ex);
        } catch (IOException ex) {
            log.error(ex);
        }
        return true;
    }

    /**
	 * Ƶ������,������˼·�����裩���� <br>
	 * 1.�ж��Ƿ������Ƶ����Ҫע����˵����Դ���������ڷ����� 2.�жϱ�Ƶ���Ƿ���Ҫ���������壺 <br>
	 * ��������ʱ���ܼ򵥾�����Ƶ�����С��Ƿ���ˢ�±�־��Ϊ��1����Ƶ�� <br>
	 * ��ȫ����ʱ������������ 3.����Ƶ��ģ������ĵ���html��jsp�� 4.����Ƶ�����Ƿ���ˢ�±�־��Ϊ��0��
	 * 
	 * @param channel
	 *            Ҫ����Ƶ��
	 * @param blFull
	 *            �Ƿ���ȫ����������ȫ�������ж��Ƿ���Ҫ������
	 * @throws java.lang.Exception
	 */
    public void publish(Channel channel, boolean blFull) throws Exception {
        Channel channels[] = (Channel[]) channel.getList();
        for (int i = 0; channels != null && i < channels.length; i++) {
            if (channels[i].getType().equals("chan")) {
                channels[i].publish(blFull);
            }
        }
        boolean noRefresh = channel.getRefresh().equals(Channel.REFRESH_NEVER) || channel.getRefresh().equals(Channel.REFRESH_NO);
        if (noRefresh && !blFull) {
            return;
        }
        int flag = channel.getPageNum();
        if (flag == 0) {
            generateChannelPage(channel, true);
        } else {
            generateChannelTPage(channel, true, null, flag);
        }
        channel.updateRefreshFlag(false);
    }

    /**
	 * Ƶ������,������˼·�����裩���� <br>
	 * 1.�ж��Ƿ������Ƶ����Ҫע����˵����Դ���������ڷ����� 2.�жϱ�Ƶ���Ƿ���Ҫ���������壺 <br>
	 * ��������ʱ���ܼ򵥾�����Ƶ�����С��Ƿ���ˢ�±�־��Ϊ��1����Ƶ�� <br>
	 * ��ȫ����ʱ������������ 3.����Ƶ��ģ������ĵ���html��jsp�� 4.����Ƶ�����Ƿ���ˢ�±�־��Ϊ��0�� 5 ͬ����Ƶ��
	 * 
	 * @param channel
	 *            Ҫ����Ƶ��
	 * @param blFull
	 *            �Ƿ���ȫ����������ȫ�������ж��Ƿ���Ҫ������
	 * @param isSyn
	 *            �Ƿ�ͬ��
	 * @throws java.lang.Exception
	 */
    public void publish(Channel channel, boolean blFull, boolean isSyn) throws Exception {
        ArrayList flags = new ArrayList();
        Channel channels[] = (Channel[]) channel.getList();
        for (int i = 0; channels != null && i < channels.length; i++) {
            if (channels[i].getType().equals("chan")) {
                channels[i].publish(blFull);
                flags.add(String.valueOf(channels[i].getPageNum()));
            }
        }
        boolean noRefresh = channel.getRefresh().equals(Channel.REFRESH_NEVER) || channel.getRefresh().equals(Channel.REFRESH_NO);
        if (noRefresh && !blFull) {
            return;
        }
        int flag = channel.getPageNum();
        if (flag == 0) {
            generateChannelPage(channel, true);
        } else {
            generateChannelTPage(channel, true, null, flag);
        }
        channel.updateRefreshFlag(false);
        try {
            if (isSyn) {
                channel.synOutSite();
            }
        } catch (Exception ex1) {
            log.error("ͬ��ʧ��!", ex1);
        }
    }

    /**
	 * bill modified this method for Channel Page generating which have more
	 * flap pages need generate
	 * 
	 * @param channel
	 *            channel need to generate page
	 * @param flap
	 *            if generate flap pages
	 * @see FlipPageTag
	 */
    public void generateChannelPage(Channel channel, boolean flap) throws Exception {
        boolean blStatic = true;
        String sTemp = "";
        String pFileName = "index_ps.jsp";
        String channelProtoTyleUrl = "";
        String pFilePath = cfg.getInfoplatDataDir() + "pub/" + channel.getSiteAsciiName() + "/" + channel.getAsciiName();
        File file = new File(pFilePath + "/" + pFileName);
        if (!file.exists()) {
            blStatic = false;
            pFileName = "index_pd.jsp";
            file = new File(pFilePath + "/" + pFileName);
        }
        if (!file.exists()) {
            channelProtoTyleUrl = getPreviewUrl(channel.getPath(), channel.getTemplateId());
            if (channelProtoTyleUrl.equals("")) {
                log.debug("Ƶ����" + channel.getName() + "��ģ���ļ������ڣ���" + pFilePath + "��");
                return;
            }
            int iInd = channelProtoTyleUrl.indexOf("blStatic=");
            if (iInd > 0) {
                sTemp = channelProtoTyleUrl.substring(iInd + 9);
                if (sTemp.equals("false")) blStatic = false; else blStatic = true;
            }
        } else {
            log.debug("Ƶ����" + channel.getName() + "ˢ�±�־ΪTrue,����ģ���ļ����ڣ�");
            channelProtoTyleUrl = cfg.getInfoplatDataDirContext() + "pub/" + channel.getSiteAsciiName() + "/" + channel.getAsciiName() + "/" + pFileName + "?channelPath=" + channel.getPath() + "&blStatic=" + blStatic;
        }
        if (!flap) {
            channelProtoTyleUrl = channelProtoTyleUrl + "&refreshFlag=" + channel.getRefresh();
        }
        String strResultFileName = pFilePath;
        log.debug("Ƶ��URL�� " + channelProtoTyleUrl);
        sTemp = channelProtoTyleUrl.substring(0, channelProtoTyleUrl.indexOf("?"));
        int pageNum = channel.getPageNum();
        channelProtoTyleUrl = channelProtoTyleUrl + "&pageNum=" + pageNum;
        channelProtoTyleUrl = channelProtoTyleUrl + "&genURL=" + sTemp;
        channelProtoTyleUrl = channelProtoTyleUrl + "&genFilePath=" + strResultFileName;
        log.debug("����Ƶ��URL�� " + channelProtoTyleUrl);
        if (!blStatic) {
            genStaticRedirectFile(strResultFileName + "/index.html");
            strResultFileName += "/index.jsp";
            new GenHtml().generateHtmlPage(channelProtoTyleUrl, strResultFileName, true, true);
        } else {
            strResultFileName += "/index.html";
            try {
                new GenHtml().generateHtmlPage(channelProtoTyleUrl, strResultFileName, true);
            } catch (Exception ex) {
                log.error("���HTML�ļ�����");
                log.error(ex.toString());
            }
        }
    }

    /**
	 * bill modified this method for Channel Page generating which have more
	 * flap pages need generate
	 * 
	 * @param channel
	 *            channel need to generate page
	 * @param flap
	 *            if generate flap pages
	 * @see FlipPageTag
	 */
    public void generateChannelTPage(Channel channel, boolean flap, Date createDate, int flag) throws Exception {
        boolean blStatic = true;
        String sTemp = "";
        String pFileName = "index_ps.jsp";
        String channelProtoTyleUrl = "";
        String pFilePath = cfg.getInfoplatDataDir() + "pub/" + channel.getSiteAsciiName() + "/" + channel.getAsciiName();
        File file = new File(pFilePath + "/" + pFileName);
        if (!file.exists()) {
            blStatic = false;
            pFileName = "index_pd.jsp";
            file = new File(pFilePath + "/" + pFileName);
        }
        if (!file.exists()) {
            channelProtoTyleUrl = getPreviewUrl(channel.getPath(), channel.getTemplateId());
            if (channelProtoTyleUrl.equals("")) {
                log.debug("Ƶ����" + channel.getName() + "��ģ���ļ������ڣ���" + pFilePath + "��");
                return;
            }
            int iInd = channelProtoTyleUrl.indexOf("blStatic=");
            if (iInd > 0) {
                sTemp = channelProtoTyleUrl.substring(iInd + 9);
                if (sTemp.equals("false")) blStatic = false; else blStatic = true;
            }
        } else {
            log.debug("Ƶ����" + channel.getName() + "ˢ�±�־ΪTrue,����ģ���ļ����ڣ�");
            channelProtoTyleUrl = cfg.getInfoplatDataDirContext() + "pub/" + channel.getSiteAsciiName() + "/" + channel.getAsciiName() + "/" + pFileName + "?channelPath=" + channel.getPath();
        }
        String strResultFileName = pFilePath;
        if (!blStatic) {
            return;
        } else {
            String CreateDateFrom = "";
            String CreateDateTo = "";
            if (createDate != null) {
                Calendar cal = new GregorianCalendar();
                cal.setTime(createDate);
                int creYear = cal.get(Calendar.YEAR);
                int creMonth = cal.get(Calendar.MONTH) + 1;
                int creDay = cal.get(Calendar.DATE);
                if (flag == 1) {
                    strResultFileName += "/index_" + creYear + ".html";
                    CreateDateFrom = creYear + "-01-01";
                    CreateDateTo = (creYear + 1) + "-01-01";
                } else if (flag == 2) {
                    strResultFileName += "/index_" + creYear + "_" + creMonth + ".html";
                    CreateDateFrom = creYear + "-" + creMonth + "-01";
                    cal.add(Calendar.MONTH, 1);
                    creYear = cal.get(Calendar.YEAR);
                    creMonth = cal.get(Calendar.MONTH) + 1;
                    CreateDateTo = "" + creYear + "-" + creMonth + "-01";
                } else if (flag == 3) {
                    int weeks = cal.get(Calendar.WEEK_OF_YEAR);
                    strResultFileName += "/index_" + creYear + "_" + weeks + ".html";
                    cal.add(Calendar.DATE, -cal.get(Calendar.DAY_OF_WEEK) + cal.getFirstDayOfWeek());
                    creYear = cal.get(Calendar.YEAR);
                    creMonth = cal.get(Calendar.MONTH) + 1;
                    creDay = cal.get(Calendar.DATE);
                    CreateDateFrom = creYear + "-" + creMonth + "-" + creDay;
                    cal.add(Calendar.DATE, 7);
                    creYear = cal.get(Calendar.YEAR);
                    creMonth = cal.get(Calendar.MONTH) + 1;
                    creDay = cal.get(Calendar.DATE);
                    CreateDateTo = creYear + "-" + creMonth + "-" + creDay;
                } else if (flag == 4) {
                    strResultFileName += "/index_" + creYear + "_" + creMonth + "_" + creDay + ".html";
                    CreateDateFrom = creYear + "-" + creMonth + "-" + creDay;
                    cal.add(Calendar.DATE, 1);
                    creYear = cal.get(Calendar.YEAR);
                    creMonth = cal.get(Calendar.MONTH) + 1;
                    creDay = cal.get(Calendar.DATE);
                    CreateDateTo = creYear + "-" + creMonth + "-" + creDay;
                }
                channelProtoTyleUrl = channelProtoTyleUrl + "&CreateDateFrom=" + CreateDateFrom + "&CreateDateTo=" + CreateDateTo + "&flag=" + flag;
            } else {
                channelProtoTyleUrl = channelProtoTyleUrl + "&flag=" + flag + "&channelUrl=" + channelProtoTyleUrl + "&strFileName=" + strResultFileName;
                strResultFileName += "/index.html";
            }
            try {
                log.debug("channelProtoTyleUrl:= " + channelProtoTyleUrl);
                log.debug("strResultFileName:= " + strResultFileName);
                new GenHtml().generateHtmlPage(channelProtoTyleUrl, strResultFileName, true);
            } catch (Exception ex) {
                log.error("���HTML�ļ�����");
                log.error(ex.toString());
            }
        }
    }

    /**
	 * ��ȡƵ��ģ��Ԥ��URL
	 * 
	 * @param channelPath
	 * @return
	 */
    public String getPreviewUrl(String channelPath, String templateId) {
        if (null == templateId || templateId.equals("")) return "";
        String templateJspUrl = "";
        try {
            Template template = Template.getInstance(Integer.parseInt(templateId));
            String type = template.getType();
            if (!type.equals("2204") && !type.equals("2205")) return "";
            boolean blStatic = true;
            if (type.equals("2204")) blStatic = true; else if (type.equals("2205")) blStatic = false;
            templateJspUrl = ConfigInfo.getInstance().getInfoplatDataDirContext() + "template/dynamic" + (template.getAsciiName().startsWith("/") ? "" : "/") + template.getAsciiName() + "/index_ps.jsp?channelPath=" + channelPath + "&blStatic=" + blStatic;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return templateJspUrl;
    }

    /**
	 * ��ɶ�̬�����ļ��ľ�̬�Զ�ת��ҳ��
	 */
    private void genStaticRedirectFile(String redirectFile) {
        StringBuffer sbContent = new StringBuffer();
        sbContent.append("<SCRIPT LANGUAGE=\"JavaScript\">\n").append("<!--\n").append("window.location=\"index.jsp\"\n").append("//-->\n").append("</SCRIPT>\n");
        try {
            Function.writeTextFile(sbContent.toString(), redirectFile, true);
        } catch (IOException ex) {
        }
    }

    /**
	 * ����Ƶ��ҳ��ˢ�·�����־ �����ǣ�����Ϊ�˱�־������ʧ�ܣ�û���㹻���������׳��쳣 ����׳��쳣����Ӱ�췢������
	 * 
	 * @param blRefreshFlag
	 */
    public void updateRefreshFlag(Channel channel, boolean blRefreshFlag) {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            resultSet = dbo.select("select refresh_flag from t_ip_channel where channel_path='" + channel.getPath() + "'");
            if (resultSet.next()) {
                String tmp = resultSet.getString(1);
                if (tmp == null) {
                    return;
                }
                boolean flag = tmp.equals(Channel.REFRESH_FOREVER) || tmp.equals(Channel.REFRESH_NEVER) || tmp.equals(Channel.REFRESH_RIGHT_NOW);
                if (tmp != null && flag) {
                    return;
                }
            }
            String sqlStr = "update t_ip_channel set refresh_flag=? where channel_path=?";
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setString(1, (blRefreshFlag ? "1" : "0"));
            preparedStatement.setString(2, channel.getPath());
            preparedStatement.executeUpdate();
        } catch (Exception ex) {
            log.error("����Ƶ��ҳ��ˢ�±�־���?", ex);
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
	 * �õ���Ƶ����վ��ascii_name
	 * 
	 * @return
	 * @throws java.lang.Exception
	 */
    public String getSiteAsciiName(Channel channel) throws Exception {
        if (channel == null) {
            return null;
        }
        if (channel.getPath() == null || channel.getPath().length() < 10) {
            throw new Exception("��Channel�����channelPath�쳣:" + channel.getPath() + "!Ӧ������15λ!");
        }
        TreeNode tn = TreeNode.getInstance(channel.getPath().substring(0, 10));
        if (tn == null) {
            throw new Exception("û���ҵ�path=" + channel.getPath() + "������վ�����!");
        }
        Site site = null;
        try {
            site = (Site) tn;
        } catch (Exception ex) {
            throw new Exception("path=" + channel.getPath().substring(1, 10) + "�Ķ�����һ��Site����!");
        }
        return site.getAsciiName();
    }

    /**
	 * ȡ����Ƶ��
	 * 
	 * @param channel
	 *            String
	 * @param tpye
	 *            String
	 * @return String[]
	 */
    public String[] getRelatingChannel(String channel, String type) {
        return getRelatingChannel(channel, type, false);
    }

    /**
	 * ȡ����Ƶ��
	 * 
	 * @param channel
	 * @param type
	 * @param forTree
	 *            ���������Ϊtrue��ʱ��,ȡ������Ƶ��ˢ�µ���������Ƶ��,��ѯ�������෴
	 * @return
	 */
    public String[] getRelatingChannel(String channel, String type, boolean forTree) {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String[] list = null;
        try {
            new CesGlobals().setConfigFile(Constant.DB_CONFIGE_FILE);
            DBOperationFactory factory = new ERDBOperationFactory();
            dbo = factory.createDBOperation(ConfigInfo.getInstance().getPoolName());
            connection = dbo.getConnection();
            String sql = "select " + type + "  from t_ip_channel_relating where channelpath=?";
            if (forTree) {
                sql = "select channelpath  from t_ip_channel_relating where " + type + " =?";
            }
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, channel);
            resultSet = preparedStatement.executeQuery();
            Vector v = new Vector();
            while (resultSet.next()) {
                if (resultSet.getString(1) != null) {
                    v.add(resultSet.getString(1));
                }
            }
            list = new String[v.size()];
            for (int j = 0; j < v.size(); j++) {
                list[j] = (String) v.get(j);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return list;
    }

    /**
	 * �����ļ���
	 * 
	 * @param folderName
	 * @throws Exception
	 */
    private void createFolder(String folderName) throws Exception {
        File file = new File(folderName);
        if (!file.exists()) {
            file.mkdirs();
            log.debug("create folder:folderName sucess");
        } else {
            log.debug("create folder:" + folderName + " is exists");
        }
    }

    /**
	 * ɾ���ļ���
	 * 
	 * @param folderName
	 *            ɾ���ļ�����
	 * @throws Exception
	 */
    private void deleteFolder(String folderName) throws Exception {
        File file = new File(folderName);
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] fe = file.listFiles();
                for (int i = 0; i < fe.length; i++) {
                    deleteFolder(fe[i].toString());
                    fe[i].delete();
                }
            }
            file.delete();
            log.debug("delete folder:" + folderName + " sucess");
        } else {
            log.debug("folder:" + folderName + " is not exists");
        }
    }

    /**
	 * �õ�ĳƵ��Path�¿��Է�����Ƶ�����б�
	 * 
	 * @return Ƶ���б�
	 */
    public List getPublishableList(String parentPath) throws Exception {
        Channel channel = null;
        List channels = new ArrayList();
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer().append(" select channel.*").append(",site.ascii_name site_ascii_name").append(" from t_ip_channel channel").append(",t_ip_site site").append(" where channel.channel_path like '").append(parentPath).append("%' ").append(" and channel.site_id=site.id").append(" and (channel.refresh_flag='").append(Channel.REFRESH_FOREVER).append("' or  channel.refresh_flag='").append(Channel.REFRESH_RIGHT_NOW).append("' or  channel.refresh_flag='").append(Channel.REFRESH_YES).append("')");
            ResultSet rs = dbo.select(sql.toString());
            while (rs.next()) {
                channel = new Channel(rs.getInt("id"));
                channel.setAsciiName(rs.getString("ascii_name"));
                channel.setChannelType(rs.getString("type"));
                channel.setCreateDate(rs.getDate("create_date"));
                channel.setCreator(rs.getInt("creator"));
                channel.setDataUrl(rs.getString("data_url"));
                channel.setDesc(rs.getString("description"));
                channel.setName(rs.getString("name"));
                channel.setOrderNo(rs.getInt("order_no"));
                channel.setSiteId(rs.getInt("site_id"));
                channel.setStyle(rs.getString("style"));
                channel.setTemplateId(rs.getString("template_id"));
                channel.setUseStatus(rs.getString("use_status"));
                channel.setPath(rs.getString("channel_path"));
                channel.setRefresh(rs.getString("refresh_flag"));
                channel.setSiteAsciiName(rs.getString("site_ascii_name"));
                channel.setPageNum(rs.getInt("page_num"));
                channels.add(channel);
            }
        } catch (Exception e) {
            log.error("�õ�Ƶ���б�ʱ����", e);
            throw e;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return channels;
    }

    /**
	 * �õ�ĳƵ��Path��������
	 * 
	 * @return ����
	 */
    public int getChannelMaxLevel(String path) throws Exception {
        DBOperation dbo = null;
        ResultSet resultSet = null;
        int maxLevel = -1;
        try {
            dbo = createDBOperation();
            String sql = "select max(channel_path) from t_ip_channel where channel_path like '" + path + "%'";
            resultSet = dbo.select(sql);
            if (resultSet.next()) {
                String mPath = resultSet.getString(1);
                maxLevel = mPath.length() / 5 - 1;
            }
        } catch (Exception e) {
            log.error("�õ�ĳƵ��Path�¿�������ʱ����", e);
            throw e;
        } finally {
            close(resultSet, null, null, null, dbo);
        }
        return maxLevel;
    }

    /**
	 * �õ�Ƶ���ļ����ϼ�Ƶ������ư�����ķָ��
	 * 
	 * @return
	 */
    public String getSelfAndAncestors(String channelPath, String sep) throws Exception {
        DBOperation dbo = null;
        ResultSet resultSet = null;
        String allPath = "";
        try {
            dbo = createDBOperation();
            String sql = "select title from t_ip_tree_frame where '" + channelPath + "' like concat(path,'%') and path != '00000' order by path";
            resultSet = dbo.select(sql);
            if (null == sep || "".equals(sep)) sep = ">>";
            while (resultSet.next()) {
                allPath += sep + resultSet.getString("title");
            }
        } catch (Exception e) {
            log.error("�õ� �õ�Ƶ���ļ����ϼ�Ƶ�������ʱ����", e);
            throw e;
        } finally {
            close(resultSet, null, null, null, dbo);
        }
        if (allPath.length() > 0) allPath = allPath.substring(sep.length());
        return allPath;
    }

    /**
	 * ɾ��Ƶ���µ��ĵ�
	 * 
	 * @param blRefreshFlag
	 */
    public void delChannelDoc(Channel channel) {
        DBOperation dbo = null;
        Connection connection = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            stmt = connection.createStatement();
            stmt.addBatch("delete from T_IP_DOC_RES where DOC_ID in (select id from T_IP_DOC where CHANNEL_PATH like '" + channel.getPath() + "%')");
            stmt.addBatch("delete from T_IP_BROWSE where DOC_ID in (select id from T_IP_DOC where CHANNEL_PATH like '" + channel.getPath() + "%')");
            stmt.addBatch("delete from T_WF_TASK where PROS_INST_ID in (select ACTY_INST_ID from T_IP_DOC where CHANNEL_PATH like '" + channel.getPath() + "%')");
            stmt.addBatch("delete from T_WF_PROCESS_INSTANCE where id in (select ACTY_INST_ID from T_IP_DOC where CHANNEL_PATH like '" + channel.getPath() + "%')");
            stmt.addBatch("delete from T_IP_DOC where CHANNEL_PATH like '" + channel.getPath() + "%'");
            stmt.executeBatch();
        } catch (Exception ex) {
            log.error("ɾ��Ƶ���µ��ĵ���", ex);
        } finally {
            close(resultSet, stmt, null, connection, dbo);
        }
    }
}
