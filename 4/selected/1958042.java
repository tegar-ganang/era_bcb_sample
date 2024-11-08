package ces.platform.infoplat.core.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;
import ces.common.workflow.WAPI;
import ces.common.workflow.WFFactory;
import ces.common.workflow.entity.Task;
import ces.coral.dbo.DBOperation;
import ces.coral.file.FileOperation;
import ces.eim.proxy.BusinessInterface;
import ces.platform.infoplat.core.DocResource;
import ces.platform.infoplat.core.DocType;
import ces.platform.infoplat.core.Document;
import ces.platform.infoplat.core.DocumentCBF;
import ces.platform.infoplat.core.DocumentPublish;
import ces.platform.infoplat.core.RelatedDocument;
import ces.platform.infoplat.core.Site;
import ces.platform.infoplat.core.SiteChannelDocTypeRelation;
import ces.platform.infoplat.core.Template;
import ces.platform.infoplat.core.base.BaseDAO;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.utils.Function;
import ces.platform.infoplat.utils.http.GenHtml;
import ces.platform.system.common.Constant;
import ces.platform.system.common.XmlInfo;
import ces.platform.system.dbaccess.User;
import ces.research.oa.document.util.Config_new;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class DocumentPublishDAO extends BaseDAO {

    public DocumentPublishDAO() {
    }

    /**
	 * �õ��ѷ����ĵ�����
	 * @param docId �ĵ����
	 * @param channelPath Ƶ��Path
	 * @return �ѷ����ĵ�����
	 * @throws Exception
	 * */
    public DocumentPublish getInstance(int docId, String channelPath) throws Exception {
        DocumentPublish doc = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            String sql = new StringBuffer().append(" select browse1.*").append(",securityLevel.name security_level_name").append(",code.name remark_prop_name").append(",channel.ascii_name channel_ascii_name").append(",site.ascii_name site_ascii_name,docType.input_template_id,docType.name docTypeName").append(" from t_ip_browse browse1").append(",t_ip_security_level securityLevel").append(",t_ip_code code ").append(",t_ip_channel channel").append(",t_ip_site site").append(",t_ip_doc_type docType ").append(" where browse1.doc_id=? and browse1.channel_path=?").append(" and browse1.security_level_id=securityLevel.id").append(" and code.field=1 and browse1.channel_path = channel.channel_path").append(" and channel.site_id = site.id and browse1.doctype_path=docType.doctype_path").toString();
            if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
                sql += " and browse1.remark_prop= convert(varchar,code.id) ";
            } else {
                sql += " and browse1.remark_prop= code.id ";
            }
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docId);
            preparedStatement.setString(2, channelPath);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                doc = new DocumentPublish();
                doc.setAbstractWords(resultSet.getString("abstract_words"));
                doc.setAttachStatus(resultSet.getString("attach_status"));
                doc.setAuthor(resultSet.getString("Author"));
                doc.setChannelPath(resultSet.getString("Channel_Path"));
                doc.setContentFile(resultSet.getString("content_file"));
                doc.setCreateDate(resultSet.getTimestamp("create_date"));
                doc.setCreater(resultSet.getInt("creater"));
                doc.setDoctypePath(resultSet.getString("doctype_path"));
                doc.setInputTemplateId(resultSet.getInt("input_template_id"));
                doc.setDocTypeName(resultSet.getString("docTypeName"));
                doc.setEditorRemark(resultSet.getString("editor_remark"));
                doc.setEmitDate(resultSet.getTimestamp("emit_date"));
                doc.setEmitUnit(resultSet.getString("emit_unit"));
                doc.setHyperlink(resultSet.getString("hyperlink"));
                doc.setId(resultSet.getInt("doc_id"));
                doc.setChannelAsciiName(resultSet.getString("channel_ascii_name"));
                doc.setSiteAsciiName(resultSet.getString("site_ascii_name"));
                doc.setKeywords(resultSet.getString("keywords"));
                doc.setLastestModifyDate(resultSet.getTimestamp("lastest_modify_date"));
                doc.setNotes(resultSet.getString("notes"));
                doc.setPeriodicalNo(resultSet.getInt("periodical_no"));
                doc.setPertinentWords(resultSet.getString("pertinent_words"));
                doc.setRemarkProp(resultSet.getInt("remark_prop"));
                doc.setRemarkPropName(resultSet.getString("remark_prop_name"));
                doc.setReservation1(resultSet.getString("reservation1"));
                doc.setReservation2(resultSet.getString("reservation2"));
                doc.setReservation3(resultSet.getString("reservation3"));
                doc.setReservation4(resultSet.getString("reservation4"));
                doc.setReservation5(resultSet.getString("reservation5"));
                doc.setReservation6(resultSet.getString("reservation6"));
                doc.setSecurityLevelId(resultSet.getInt("security_level_id"));
                doc.setSecurityLevelName(resultSet.getString("security_level_name"));
                doc.setSourceId(resultSet.getInt("source_id"));
                doc.setSubTitle(resultSet.getString("sub_title"));
                doc.setTitle(resultSet.getString("title"));
                doc.setTitleColor(resultSet.getString("title_color"));
                doc.setWordNo(resultSet.getInt("word_no"));
                doc.setYearNo(resultSet.getInt("year_no"));
                int publisher = resultSet.getInt("publisher");
                doc.setPublisher(publisher);
                doc.setOrderNo(resultSet.getInt("order_no"));
                doc.setPublishDate(resultSet.getTimestamp("publish_date"));
                doc.setValidStartDate(resultSet.getTimestamp("valid_startdate"));
                doc.setValidEndDate(resultSet.getTimestamp("valid_enddate"));
            }
        } catch (Exception ex) {
            log.error("�õ��ѷ����ĵ���ʵ��ʧ��!docId=" + docId + ",channelPath=" + channelPath, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return doc;
    }

    /**
	 * �����ĵ�������ǰ�Ķ����е���ݱ��浽��ݿ���(�½�����,����һ�������ĵ�)
	 * �˷����Ա��޸Ĺ��������ڷ���֪ͨͨ���ʱ�򣬷�wotalkϵͳ��Ϣ
	 * �޸��ˣ�ʩ��ͬ
	 * �޸�ʱ�䣺2009-7-7
	 * @param doc �ѷ����ĵ�����
	 */
    public void add(DocumentPublish doc) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        boolean result = false;
        try {
            String sql = "insert into t_ip_browse(doc_id,channel_path," + "doctype_path,publisher,publish_date,order_no," + "valid_startdate,valid_enddate,syn_status," + "content_file,attach_status,year_no," + "periodical_no,word_no,title,title_color," + "sub_title,author,emit_date,emit_unit," + "editor_remark,keywords,pertinent_words," + "abstract_words,source_id,security_level_id," + "creater,create_date,lastest_modify_date,remark_prop," + "notes,reservation1,reservation2,reservation3," + "reservation4,reservation5,reservation6,hyperlink)" + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, doc.getId());
            preparedStatement.setString(2, doc.getChannelPath());
            preparedStatement.setString(3, doc.getDoctypePath());
            preparedStatement.setInt(4, doc.getPublisher());
            preparedStatement.setTimestamp(5, (Timestamp) doc.getPublishDate());
            preparedStatement.setInt(6, doc.getOrderNo());
            preparedStatement.setTimestamp(7, (Timestamp) doc.getValidStartDate());
            preparedStatement.setTimestamp(8, (Timestamp) doc.getValidEndDate());
            preparedStatement.setString(9, doc.isSynStatus() ? "1" : "0");
            preparedStatement.setString(10, doc.getContentFile());
            preparedStatement.setString(11, doc.getAttachStatus());
            preparedStatement.setInt(12, doc.getYearNo());
            preparedStatement.setInt(13, doc.getPeriodicalNo());
            preparedStatement.setInt(14, doc.getWordNo());
            preparedStatement.setString(15, doc.getTitle());
            preparedStatement.setString(16, doc.getTitleColor());
            preparedStatement.setString(17, doc.getSubTitle());
            preparedStatement.setString(18, doc.getAuthor());
            preparedStatement.setTimestamp(19, (Timestamp) doc.getEmitDate());
            preparedStatement.setString(20, doc.getEmitUnit());
            preparedStatement.setString(21, doc.getEditorRemark());
            preparedStatement.setString(22, doc.getKeywords());
            preparedStatement.setString(23, doc.getPertinentWords());
            preparedStatement.setString(24, doc.getAbstractWords());
            preparedStatement.setString(25, String.valueOf(doc.getSourceId()));
            preparedStatement.setInt(26, doc.getSecurityLevelId());
            preparedStatement.setInt(27, doc.getCreater());
            preparedStatement.setTimestamp(28, (Timestamp) doc.getCreateDate());
            if (doc.getLastestModifyDate() == null) {
                preparedStatement.setTimestamp(29, Function.getSysTime());
            } else {
                preparedStatement.setTimestamp(29, (Timestamp) doc.getLastestModifyDate());
            }
            preparedStatement.setString(30, String.valueOf(doc.getRemarkProp()));
            preparedStatement.setString(31, doc.getNotes());
            preparedStatement.setString(32, doc.getReservation1());
            preparedStatement.setString(33, doc.getReservation2());
            preparedStatement.setString(34, doc.getReservation3());
            preparedStatement.setString(35, doc.getReservation4());
            preparedStatement.setString(36, doc.getReservation5());
            preparedStatement.setString(37, doc.getReservation6());
            preparedStatement.setString(38, doc.getHyperlink());
            preparedStatement.executeUpdate();
            result = true;
        } catch (Exception ex) {
            result = false;
            log.error("����һƪ�����ĵ�ʧ�ܣ�", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        try {
            String noticeChannelPath = Config_new.getInstance().getpath("NOTICE_CHANNEL_PATH");
            if (result && noticeChannelPath.equals(doc.getChannelPath())) {
                String yqsshouye = Config_new.getInstance().getpath("YQS_SHOU_YE_JSP");
                String strUserId = doc.getReservation4() == null ? "" : doc.getReservation4();
                String[] userIds = strUserId.split(",");
                if ("".equals(strUserId)) {
                    int publisher = doc.getPublisher();
                    User user = new User(publisher);
                    user.load();
                    Vector vv = user.getAllUsers();
                    Vector tmp = new Vector();
                    for (int i = 0; i < vv.size(); i++) {
                        User u = (User) vv.elementAt(i);
                        if (publisher == u.getUserID()) {
                            continue;
                        }
                        tmp.add(String.valueOf(u.getUserID()));
                    }
                    String[] tmpUserIds = new String[tmp.size()];
                    tmp.toArray(tmpUserIds);
                    userIds = tmpUserIds;
                }
                for (int i = 0; i < userIds.length; i++) {
                    if (!"".equals(userIds[i])) {
                        BusinessInterface.sendSystemMessage(Integer.parseInt(userIds[i]), "֪ͨͨ�棺" + doc.getTitle(), yqsshouye);
                    }
                }
            }
        } catch (Exception ex) {
            log.error("wotalk����ϵͳ��Ϣ����", ex);
        }
    }

    /**
	 * ɾ��һ���Ѿ��������ĵ�
	 * @param doc �ѷ����ĵ�����
	 */
    public void delete(DocumentPublish doc) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        Statement stmt = null;
        try {
            String sql = "delete from t_ip_browse where doc_id=" + doc.getId() + " and channel_path='" + doc.getChannelPath() + "'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            stmt = connection.createStatement();
            stmt.executeUpdate(sql);
        } catch (Exception ex) {
            log.error("ɾ���ĵ�ʧ��!", ex);
            throw ex;
        } finally {
            close(null, stmt, null, connection, dbo);
        }
    }

    /**
	 * ���Ѿ��������ĵ�������<br>
	 * �߼���<br>
	 * 1.���ж���ƪ�ĵ��Ƿ񷢲�������Ƶ���ϣ�
	 * ���û�У���ôɾ���ĵ�����Դ���ļ���
	 * 2.ɾ����м�¼
	 * 3.�Ƿ��˻ع�����
	 * @param doc
	 * @param isBackProcess �Ƿ��˻ع�����
	 * @param userId Ҫ������ƪ�ĵ����˵�userId
	 * @throws java.lang.Exception
	 */
    public void unPublish(DocumentPublish doc, String userId, boolean isBackProcess) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        Statement stmt = null;
        ResultSet resultSet = null;
        try {
            boolean isDelRes = true;
            String sql = "select count(doc_id) from t_ip_browse where doc_id=" + doc.getId() + " and channel_path like '" + doc.getChannelPath().substring(0, 10) + "%'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            stmt = connection.createStatement();
            resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                if (resultSet.getInt(1) > 1) {
                    isDelRes = false;
                }
            }
            if (isDelRes) {
                DocType docType = (DocType) DocType.getInstance(doc.getDoctypePath());
                Template template = Template.getInstance(docType.getShowTemplateID());
                String outFile = ConfigInfo.getInstance().getInfoplatDataDir() + "pub" + File.separator + ((Site) TreeNode.getInstance(doc.getChannelPath().substring(0, 10))).getAsciiName() + File.separator + "docs" + File.separator + Function.getNYofDate(doc.getCreateDate()) + File.separator + "d_" + doc.getId();
                File f = new File(new File(outFile + ".html").getPath());
                int i = 2;
                while (f.exists()) {
                    f.delete();
                    f = new File(new File(outFile + "_" + i + ".html").getPath());
                    i++;
                }
                log.debug("ɾ����ɵ�html�ļ���" + f.getPath());
                String publishDocResDir = new File(outFile).getParent() + File.separator + "res" + File.separator;
                String[] resFileNames = DocumentCBF.getInstance(doc.getId()).getAllResourceUri();
                for (int j = 0; j < resFileNames.length; j++) {
                    FileOperation.deleteFile(publishDocResDir + resFileNames[j]);
                    log.debug("ɾ���ĵ���Դ��" + publishDocResDir + resFileNames[j]);
                }
                publishDocResDir = new File(outFile).getParent() + File.separator + "d_" + doc.getId() + ".files" + File.separator;
                FileOperation.deleteDirectory(publishDocResDir);
            }
            delete(doc);
            boolean isBack = true;
            sql = "select count(doc_id) from t_ip_browse where doc_id=" + doc.getId();
            resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                if (resultSet.getInt(1) >= 1) {
                    isBack = false;
                }
            }
            if (isBack && isBackProcess) {
                int docId = doc.getId();
                reCreateProInst(String.valueOf(docId), userId);
            }
        } catch (Exception ex) {
            log.error("ɾ���ĵ�ʧ��!", ex);
            throw ex;
        } finally {
            close(resultSet, stmt, null, connection, dbo);
        }
    }

    /**
	 * modified by yangjb
	 * �����ĵ��Ǹ���ĵ���ż��������½��ĵ�����ʵ��
	 * @param docId
	 * @param userId
	 * @throws Exception
	 */
    public void reCreateProInst(String docId, String userId) throws Exception {
        DBOperation dbo = null;
        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            dbo = createDBOperation();
            con = dbo.getConnection();
            String queryProSql = "select dt.process_id from t_ip_doc d,t_ip_doc_type dt where " + " d.doctype_path = dt.doctype_path and d.id  = " + docId;
            String queryActySql = "select id from t_ipwf_activity where process_id = ? order by order_no ";
            pstm = con.prepareStatement(queryProSql);
            rs = pstm.executeQuery();
            String processId = "";
            if (rs.next()) processId = rs.getString(1);
            pstm = con.prepareStatement(queryActySql);
            pstm.setInt(1, Integer.parseInt(processId));
            rs = pstm.executeQuery();
            String activityId = "";
            if (rs.next()) activityId = rs.getString(1);
            WAPI wapi = WFFactory.createWAPI(userId);
            Task task = wapi.startProcessInstance(Integer.parseInt(processId), String.valueOf(activityId), "��������");
            String processInstId = String.valueOf(task.getProsInstId());
            String updateDocSQL = "update t_ip_doc set acty_inst_id = " + processInstId + " where id = " + docId;
            pstm = con.prepareStatement(updateDocSQL);
            pstm.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            close(rs, null, pstm, con, dbo);
        }
    }

    /**
     * ����ĵ������ļ�����
     * @param doc
     * @param strContent
     * @throws java.lang.Exception
     */
    public String getContentName(DocumentPublish doc) {
        String strCreateYearMonth = Function.getNYofDate(doc.getCreateDate());
        String strFilePathName = cfg.getPlatformDataDir() + "/infoplat/workflow/docs/";
        strFilePathName += strCreateYearMonth + "/d_" + doc.getId() + ".data";
        return strFilePathName;
    }

    /**
	 * ���·����ĵ�
	 * @param tmpDoc
	 * @throws Exception
	 */
    public void rePublish(DocumentPublish tmpDoc) throws Exception {
        DocumentCBF doc = DocumentCBF.getInstance(tmpDoc.getId());
        String publishedPath[] = { tmpDoc.getChannelPath() };
        tmpDoc = new DocumentPublish(doc, publishedPath[0], tmpDoc.getPublisher(), tmpDoc.getPublishDate(), tmpDoc.getValidStartDate(), tmpDoc.getValidEndDate(), tmpDoc.getOrderNo());
        tmpDoc.delete();
        tmpDoc.add();
        String publishPath = tmpDoc.getChannelPath();
        String sitePath = null;
        if (publishPath == null || publishPath.length() < 10) {
            log.error("Ҫ����ķ���·����,path=" + publishPath + ",��path�ϵķ���ʧ��!");
        }
        if (!publishPath.substring(0, 10).equals(sitePath)) {
            sitePath = publishPath.substring(0, 10);
            int showTemplateId = -1;
            showTemplateId = SiteChannelDocTypeRelation.getShowTemplateId(publishPath, doc.getDoctypePath());
            if (showTemplateId == -1 || showTemplateId == 0) {
                DocType docType = (DocType) DocType.getInstance(doc.getDoctypePath());
                if (docType == null) {
                    throw new Exception("û���ҵ�pathΪ" + doc.getDoctypePath() + "���ĵ�����!");
                }
                showTemplateId = docType.getShowTemplateID();
            }
            Template template = Template.getInstance(showTemplateId);
            if (template == null) {
                throw new Exception("û���ҵ�idΪ" + showTemplateId + "��ģ��!");
            }
            String templateJspUrl = ConfigInfo.getInstance().getInfoplatDataDirContext() + "template/dynamic/" + template.getAsciiName() + "/index_p.jsp?docId=" + doc.getId() + "&publishedPath=" + publishPath;
            log.debug("ģ��jsp������url=" + templateJspUrl);
            TreeNode treeNode = TreeNode.getInstance(publishPath.substring(0, 10));
            if (treeNode == null) {
                throw new Exception("�Ҳ�������·��Ϊ" + publishPath + "��վ��ʵ��(path=" + publishPath.substring(0, 10) + "!");
            }
            Site site = null;
            try {
                site = (Site) treeNode;
            } catch (Exception ex) {
                throw new Exception("pathΪ" + treeNode.getPath() + "����һ��վ��ʵ��,��������ݶ������!");
            }
            String outFile = ConfigInfo.getInstance().getInfoplatDataDir() + "pub/" + site.getAsciiName() + "/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/d_" + doc.getId() + ".html";
            String outFileDir = new File(outFile).getParent();
            log.debug("����ģ��jsp��ɵ�html��ȫ·��=" + outFile);
            File f = new File(outFileDir);
            if (!f.exists()) {
                f.mkdirs();
            }
            try {
                new GenHtml().generateHtmlPage(templateJspUrl, outFile, true);
            } catch (Exception ex2) {
                log.error("���HTML�ļ�����");
                log.error(ex2.toString());
            }
            String templateResDir = new File(ConfigInfo.getInstance().getInfoplatDataDir()).getPath() + "/template/dynamic/" + template.getAsciiName() + "/res_" + template.getAsciiName() + "/";
            log.debug("ģ����Դ��·��=" + templateResDir);
            String docResDir = new File(ConfigInfo.getInstance().getInfoplatDataDir()).getPath() + "/workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/res/";
            log.debug("�ĵ���Դ(ͼƬ,����,������Դ)��·��=" + docResDir);
            String publishTemplateResDir = outFileDir + "/res_" + template.getAsciiName() + "/";
            String publishDocResDir = outFileDir + "/res/";
            File fTmp = new File(publishTemplateResDir);
            if (!fTmp.exists()) {
                fTmp.mkdirs();
            }
            fTmp = new File(publishDocResDir);
            if (!fTmp.exists()) {
                fTmp.mkdirs();
            }
            fTmp = null;
            log.debug("����ʱģ����Դ·��=" + publishTemplateResDir);
            log.debug("����ʱ�ĵ���Դ·��=" + publishDocResDir);
            String[] resFileNames = doc.getAllResourceUri();
            for (int j = 0; j < resFileNames.length; j++) {
                ces.coral.file.FileOperation.copy(docResDir + resFileNames[j], publishDocResDir + resFileNames[j], true);
            }
            try {
                templateResDir = new File(templateResDir).getPath() + File.separator;
                publishTemplateResDir = new File(publishTemplateResDir).getPath() + File.separator;
                ces.coral.file.FileOperation.copyDir(templateResDir, publishTemplateResDir);
            } catch (Exception ex1) {
                log.error("templateResDir=" + templateResDir);
                log.error("publishTemplateResDir=" + publishTemplateResDir);
                log.error("copyģ����Դʧ��!", ex1);
            }
        }
    }

    /**
	 * ����ĵ������ļ�����
	 * @param doc
	 * @param strContent
	 * @throws java.lang.Exception
	 */
    public String getContent(DocumentPublish doc) {
        String strContent = null;
        String strCreateYearMonth = Function.getNYofDate(doc.getCreateDate());
        String strFilePathName = cfg.getPlatformDataDir() + "/infoplat/workflow/docs/";
        strFilePathName += strCreateYearMonth + "/d_" + doc.getId();
        File file = new File(strFilePathName + ".htm");
        if (file == null || !file.exists()) {
            file = new File(strFilePathName + ".data");
        }
        try {
            if (file != null && file.exists()) {
                strContent = Function.readHtmlFile(file.getPath());
            }
        } catch (Exception ex) {
            log.error("����ĵ������ļ����ݳ���!", ex);
        }
        return strContent;
    }

    /**
	 * �õ�����ĵ��б�
	 * @param docID �ĵ����
	 * @param siteAsciiName վ��AsciiName
	 * @return ����ĵ�����
	 */
    public Document[] getCorrelationDocList(int docID, String siteAsciiName) throws Exception {
        Document[] result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = new StringBuffer("select docRes.uri from").append(" t_ip_doc_res docRes").append(" ,t_ip_browse browse1").append(" where docRes.doc_id=?").append(" and docRes.doc_id=browse1.doc_id").append(" and type='").append(DocResource.DOC_CORRELATION_TYPE).append("'").toString();
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            String tmp = null;
            if (resultSet.next()) {
                tmp = resultSet.getString(1);
            } else {
                return null;
            }
            String[] correlationDocIDs = Function.stringToArray(tmp);
            result = new RelatedDocument[correlationDocIDs.length];
            for (int i = 0; i < correlationDocIDs.length; i++) {
                result[i] = RelatedDocument.getInstance(Integer.parseInt(correlationDocIDs[i]), siteAsciiName);
            }
        } catch (Exception ex) {
            log.error("�õ�����ĵ��б����", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
	 * վ��Ƶ��������ͳ��
	 * @ siteChannelPath
	 * @ timeOffset ʱ���,���Ϊnull����Ϊȫ��
	 */
    public int getPublishedAmount(String siteChannelPath, String timeOffset) throws Exception {
        int amount = 0;
        DBOperation dbo = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            StringBuffer sql = new StringBuffer();
            sql.append(" select count(*) from t_ip_browse t");
            if (siteChannelPath.length() == 10) {
                sql.append(" where t.channel_path like '");
                sql.append(siteChannelPath);
                sql.append("%'");
            } else {
                sql.append(" where t.channel_path = '");
                sql.append(siteChannelPath);
                sql.append("'");
            }
            if (timeOffset != null) {
                sql.append(timeOffset);
            }
            resultSet = dbo.select(sql.toString());
            if (resultSet.next()) {
                amount = resultSet.getInt(1);
            }
            dbo.close();
        } catch (Exception ex) {
            log.error("�õ�Ƶ���б����!", ex);
            throw ex;
        } finally {
            close(resultSet, null, null, null, dbo);
        }
        return amount;
    }

    /**
	 * �ж���һ��վ���ϵĶ��Ƶ�����Ƿ����ͬһƪ�ĵ�<br>
	 * @param doc
	 * @return true ���� false ������
	 * @throws java.lang.Exception
	 */
    public boolean isMoreChannel(DocumentPublish doc) throws Exception {
        boolean bReturn = false;
        DBOperation dbo = null;
        ResultSet resultSet = null;
        String sql = "select count(doc_id) from t_ip_browse where doc_id=" + doc.getId() + " and channel_path like '" + doc.getChannelPath().substring(0, 10) + "%'";
        try {
            dbo = createDBOperation();
            resultSet = dbo.select(sql);
            if (resultSet.next() && resultSet.getInt(1) > 1) {
                bReturn = true;
            }
        } catch (Exception e) {
        } finally {
            close(resultSet, null, null, null, dbo);
        }
        return bReturn;
    }

    public void setPublish_channel(DocumentPublish doc) {
        DBOperation dbo = null;
        ResultSet rs = null;
        String sql = "select browse_.channel_path,channel.name " + "from t_ip_browse browse_,t_ip_channel channel where  doc_id =" + doc.getId() + " and browse_.channel_path = channel.channel_path";
        try {
            dbo = createDBOperation();
            rs = dbo.select(sql);
            int count = 0;
            String channel_name = "";
            while (rs.next()) {
                count++;
                channel_name = channel_name + rs.getString(2) + ",";
            }
            if (!channel_name.equals("")) {
                channel_name = channel_name.substring(0, channel_name.length() - 1);
            }
            doc.setPublished_channel_count(String.valueOf(count));
            doc.setPublished_channel(channel_name);
        } catch (Exception e) {
        } finally {
            close(rs, null, null, null, dbo);
        }
    }
}
