package ces.platform.infoplat.core.dao;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import ces.coral.dbo.DBOperation;
import ces.coral.file.CesGlobals;
import ces.platform.infoplat.core.Site;
import ces.platform.infoplat.core.base.BaseDAO;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.base.Const;
import ces.platform.system.facade.AuthorityManager;
import ces.platform.system.facade.StructAuth;
import ces.platform.system.facade.StructResource;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class SiteDAO extends BaseDAO {

    /**
     *  �Ѵ����ݲ�����ݿ�
     *
     *@param  site  վ��ʵ��
     */
    public void add(Site site) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sqlStr = "insert into t_ip_site (id,name,description,ascii_name,site_path,remark_number,increment_index,use_status,appserver_id) VALUES(?,?,?,?,?,?,?,?,?)";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setInt(1, site.getSiteID());
            preparedStatement.setString(2, site.getName());
            preparedStatement.setString(3, site.getDescription());
            preparedStatement.setString(4, site.getAsciiName());
            preparedStatement.setString(5, site.getPath());
            preparedStatement.setInt(6, site.getRemarkNumber());
            preparedStatement.setString(7, site.getIncrementIndex().trim());
            preparedStatement.setString(8, String.valueOf(site.getUseStatus()));
            preparedStatement.setString(9, String.valueOf(site.getAppserverID()));
            preparedStatement.executeUpdate();
            String[] path = new String[1];
            path[0] = site.getPath();
            selfDefineAdd(path, site, connection, preparedStatement);
            connection.commit();
            int resID = site.getSiteID() + Const.SITE_TYPE_RES;
            String resName = site.getName();
            int resTypeID = Const.RES_TYPE_ID;
            int operateTypeID = Const.OPERATE_TYPE_ID;
            String remark = "";
            AuthorityManager am = new AuthorityManager();
            am.createExtResource(Integer.toString(resID), resName, resTypeID, operateTypeID, remark);
            site.wirteFile();
        } catch (SQLException ex) {
            connection.rollback();
            log.error("����վ��ʧ��!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     *  ɾ����ݿ��еĴ�ʾ��
     *
     *@param  site  վ��ʵ��
     */
    public void delete(Site site) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String chkSql = "select id from t_ip_doc where channel_path=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            String[] selfDefinePath = getSelfDefinePath(site.getPath(), "1", connection, preparedStatement, resultSet);
            selfDefineDelete(selfDefinePath, connection, preparedStatement);
            preparedStatement = connection.prepareStatement(chkSql);
            preparedStatement.setString(1, site.getPath());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                throw new Exception("ɾ��ʧ�ܣ�" + site.getName() + "���Ѿ����ĵ����ڣ�");
            } else {
                String sqlStr = "delete from t_ip_site where site_path=?";
                dbo = createDBOperation();
                connection = dbo.getConnection();
                preparedStatement = connection.prepareStatement(sqlStr);
                preparedStatement.setString(1, site.getPath());
                preparedStatement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     *  ������ݿ��еĴ�ʾ��
     *
     *@param  site  վ��ʵ��
     */
    public void update(Site site) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String exp = site.getExtendParent();
        String path = site.getPath();
        try {
            String sqlStr = "update t_ip_site set id=?,name=?,description=?,ascii_name=?,remark_number=?,increment_index=?,use_status=?,appserver_id=? where id=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            connection.setAutoCommit(false);
            String[] selfDefinePath = getSelfDefinePath(path, exp, connection, preparedStatement, resultSet);
            selfDefineDelete(selfDefinePath, connection, preparedStatement);
            selfDefineAdd(selfDefinePath, site, connection, preparedStatement);
            preparedStatement = connection.prepareStatement(sqlStr);
            preparedStatement.setInt(1, site.getSiteID());
            preparedStatement.setString(2, site.getName());
            preparedStatement.setString(3, site.getDescription());
            preparedStatement.setString(4, site.getAsciiName());
            preparedStatement.setInt(5, site.getRemarkNumber());
            preparedStatement.setString(6, site.getIncrementIndex().trim());
            preparedStatement.setString(7, String.valueOf(site.getUseStatus()));
            preparedStatement.setString(8, String.valueOf(site.getAppserverID()));
            preparedStatement.setInt(9, site.getSiteID());
            preparedStatement.executeUpdate();
            connection.commit();
            int resID = site.getSiteID() + Const.SITE_TYPE_RES;
            StructResource sr = new StructResource();
            sr.setResourceID(Integer.toString(resID));
            sr.setOperateID(Integer.toString(1));
            sr.setOperateTypeID(Const.OPERATE_TYPE_ID);
            sr.setTypeID(Const.RES_TYPE_ID);
            StructAuth sa = new AuthorityManager().getExternalAuthority(sr);
            int authID = sa.getAuthID();
            if (authID == 0) {
                String resName = site.getName();
                int resTypeID = Const.RES_TYPE_ID;
                int operateTypeID = Const.OPERATE_TYPE_ID;
                String remark = "";
                AuthorityManager am = new AuthorityManager();
                am.createExtResource(Integer.toString(resID), resName, resTypeID, operateTypeID, remark);
            }
            site.wirteFile();
        } catch (SQLException ex) {
            connection.rollback();
            log.error("����վ������ʧ��!", ex);
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
    private void selfDefineAdd(String[] path, Site site, Connection connection, PreparedStatement preparedStatement) throws SQLException {
        List selfDefine = site.getSelfDefineList();
        try {
            for (int j = 0; j < path.length; j++) {
                for (int i = 0; i < selfDefine.size(); i++) {
                    Map map = (Map) selfDefine.get(i);
                    preparedStatement = connection.prepareStatement("insert into t_ip_fieldset (path," + " field,field_name,order_no) values (?,?,?,?)  ");
                    preparedStatement.setString(1, path[j]);
                    preparedStatement.setString(2, (String) map.get("field"));
                    preparedStatement.setString(3, (String) map.get("field_name"));
                    preparedStatement.setInt(4, Integer.parseInt((String) map.get("order_no")));
                    int flag = preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("��������վ���Զ����ֶ�ʧ�ܣ�");
            throw e;
        }
    }

    private String[] getSelfDefinePath(String path, String exp, Connection con, PreparedStatement pstm, ResultSet rs) {
        if (exp == null || exp.trim().equals("0")) {
            String[] rv = new String[1];
            rv[0] = path;
            return rv;
        }
        List list = new ArrayList();
        list.add(path);
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

    /**
     * �ڵõ�һ��siteʵ���ʱ����Ҫ�������ļ�
     *
     * @param site
     * @throws java.lang.Exception
     */
    public void readFile(Site site) throws Exception {
        try {
            CesGlobals cesGlobals = new CesGlobals();
            cesGlobals.setConfigFile("platform.xml");
            org.jdom.Document doc = null;
            Element element = null;
            SAXBuilder builder = new SAXBuilder();
            doc = builder.build(CesGlobals.getCesHome() + "/platform.xml");
            element = doc.getRootElement();
            element = element.getChild("platform");
            element = element.getChild("common");
            element = element.getChild("jobserver");
            ListIterator lIte = element.getChildren().listIterator();
            boolean isStartSyn = true;
            String isStartUp = "";
            for (int i = 0; lIte.hasNext(); i++) {
                element = (Element) lIte.next();
                isStartUp = element.getAttributeValue("startUp");
                Element el = (Element) element.getChild("runParam");
                String sitepath = "" + site.getPath();
                if (el.getText().trim().equals(sitepath.trim())) {
                    try {
                        isStartSyn = Boolean.valueOf(isStartUp).booleanValue();
                    } catch (Exception e) {
                        isStartSyn = true;
                    }
                    site.setIsStartSyn(isStartSyn);
                    element = element.getChild("shedule");
                    el = element.getChild("policy-delay");
                    site.setDelayStartTime(el.getAttributeValue("startTime"));
                    site.setDelayPeriod(el.getAttributeValue("period"));
                    el = element.getChild("policy-rate");
                    if (el != null && el.hasChildren()) {
                        site.setRateStartTime(el.getAttributeValue("startTime"));
                        site.setRatePeriod(el.getAttributeValue("period"));
                        site.setMaxRunTime(el.getAttributeValue("maxExecuteTime"));
                    }
                    break;
                }
            }
            String server = ConfigInfo.getInstance().getSynFtpServer(site.getSiteID());
            site.setServer(server);
            String port = String.valueOf(ConfigInfo.getInstance().getSynFtpPort(site.getSiteID()));
            site.setPort(port);
            String username = ConfigInfo.getInstance().getSynFtpLoginUser(site.getSiteID());
            site.setUsername(username);
            String password = ConfigInfo.getInstance().getSynFtpPassword(site.getSiteID());
            site.setPassword(password);
            String ftpPath = ConfigInfo.getInstance().getSynFtpRootPath(site.getSiteID());
            site.setFtpPath(ftpPath);
        } catch (Exception ex) {
            log.error("��ȡ�����ļ����ݳ���!", ex);
        }
    }

    /**
     * ��add��update��ʱ����Ҫд�����ļ�
     *
     * @param  site  վ��ʵ��
     */
    public void writeFile(Site site) throws Exception {
        CesGlobals cesGlobals = new CesGlobals();
        cesGlobals.setConfigFile("platform.xml");
        org.jdom.Document doc = null;
        Element element = null;
        Element eleFtp = null;
        SAXBuilder builder = new SAXBuilder();
        doc = builder.build(CesGlobals.getCesHome() + "/platform.xml");
        element = doc.getRootElement();
        element = element.getChild("platform");
        eleFtp = element.getChild("infoplat");
        element = element.getChild("common");
        element = element.getChild("jobserver");
        ListIterator lIte = element.getChildren().listIterator();
        boolean Found = false;
        for (int i = 0; lIte.hasNext(); i++) {
            element = (Element) lIte.next();
            Element el = (Element) element.getChild("runParam");
            if (el.getText().trim().equals(site.getPath())) {
                element.setAttribute("startUp", String.valueOf(site.getIsStartSyn()));
                element = element.getChild("shedule");
                el = element.getChild("policy-delay");
                el.setAttribute("startTime", site.getDelayStartTime());
                el.setAttribute("period", site.getDelayPeriod());
                Found = true;
                break;
            }
        }
        if (!Found) {
            Element eltJobtask = new Element("jobtask");
            eltJobtask.setAttribute("name", "��Ϣƽ̨3.0վ��ˢ�·�������");
            eltJobtask.setAttribute("startUp", String.valueOf(site.getIsStartSyn()));
            element.getParent().addContent(eltJobtask);
            Element eltRemark = new Element("remark");
            eltRemark.setText("����Ϊ��" + site.getName() + "��");
            eltJobtask.addContent(eltRemark);
            Element eltRunClassName = new Element("runClassName");
            eltRunClassName.setText("ces.platform.infoplat.service.jobs.InfoplatDataExportJob");
            eltJobtask.addContent(eltRunClassName);
            Element eltRunParam = new Element("runParam");
            eltRunParam.setText(site.getPath());
            eltJobtask.addContent(eltRunParam);
            Element eltShedule = new Element("shedule");
            eltJobtask.addContent(eltShedule);
            Element eltPolicyDelay = new Element("policy-delay");
            eltPolicyDelay.setAttribute("startTime", site.getDelayStartTime());
            eltPolicyDelay.setAttribute("period", site.getDelayPeriod());
            eltShedule.addContent(eltPolicyDelay);
        }
        Found = false;
        eleFtp = eleFtp.getChild("site-syn");
        Element eleId = eleFtp.getChild("site-" + site.getSiteID());
        if (eleId == null) {
            Element el1 = new Element("site-" + site.getSiteID());
            eleFtp.addContent(el1);
            Element el2 = new Element("ftp");
            el1.addContent(el2);
            Element ele = new Element("server");
            ele.setText(site.getServer());
            el2.addContent(ele);
            ele = new Element("port");
            ele.setText(site.getPort());
            el2.addContent(ele);
            ele = new Element("username");
            ele.setText(site.getUsername());
            el2.addContent(ele);
            ele = new Element("password");
            ele.setText(site.getPassword());
            el2.addContent(ele);
            ele = new Element("path");
            ele.setText(site.getFtpPath());
            el2.addContent(ele);
        } else {
            eleFtp = eleId.getChild("ftp");
            Element ele = eleFtp.getChild("server");
            ele.setText(site.getServer());
            ele = eleFtp.getChild("port");
            ele.setText(site.getPort());
            ele = eleFtp.getChild("username");
            ele.setText(site.getUsername());
            ele = eleFtp.getChild("password");
            ele.setText(site.getPassword());
            ele = eleFtp.getChild("path");
            ele.setText(site.getFtpPath());
        }
        File outfile = new File(CesGlobals.getCesHome() + "/platform.xml");
        FileOutputStream outStream = new FileOutputStream(outfile);
        XMLOutputter fmt = new XMLOutputter();
        fmt.setEncoding("gb2312");
        fmt.output(doc, outStream);
    }
}
