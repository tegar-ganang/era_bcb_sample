package ces.platform.infoplat.core.dao;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.regexp.RE;
import org.apache.regexp.RECompiler;
import ces.coral.dbo.DBOperation;
import ces.coral.encrypt.MD5;
import ces.coral.file.CesGlobals;
import ces.coral.file.FileOperation;
import ces.coral.image.ImgHandle;
import ces.coral.lang.StringUtil;
import ces.platform.infoplat.core.DocAffix;
import ces.platform.infoplat.core.DocApprove;
import ces.platform.infoplat.core.DocContentResource;
import ces.platform.infoplat.core.DocPicture;
import ces.platform.infoplat.core.DocResource;
import ces.platform.infoplat.core.DocType;
import ces.platform.infoplat.core.Document;
import ces.platform.infoplat.core.DocumentCBF;
import ces.platform.infoplat.core.DocumentPublish;
import ces.platform.infoplat.core.Documents;
import ces.platform.infoplat.core.Site;
import ces.platform.infoplat.core.SiteChannelDocTypeRelation;
import ces.platform.infoplat.core.Template;
import ces.platform.infoplat.core.base.BaseDAO;
import ces.platform.infoplat.core.base.ConfigInfo;
import ces.platform.infoplat.core.tree.TreeNode;
import ces.platform.infoplat.core.wf.IpWfControl;
import ces.platform.infoplat.ui.common.defaultvalue.OrderNo;
import ces.platform.infoplat.ui.workflow.collect.ContentHtmlParser;
import ces.platform.infoplat.utils.Function;
import ces.platform.infoplat.utils.http.GenHtml;
import ces.platform.system.common.Constant;
import ces.platform.system.common.IdGenerator;
import ces.platform.system.common.XmlInfo;

public class DocumentCBFDAO extends BaseDAO {

    public DocumentCBFDAO() {
    }

    /**
     * �õ�һ���Ѿ����ڵĲɱ෢���ĵ�ʵ��(����ݿ���ȡ)
     * @param docID �ĵ�id
     * @return
     * @throws java.lang.Exception
     */
    public DocumentCBF getInstance(int docID) throws Exception {
        DocumentCBF doc = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            String sql = "select a.*,b.name security_level_name,c.name remark_prop_name,d.input_template_id " + "from t_ip_doc a,t_ip_security_level b,t_ip_code c,t_ip_doc_type d " + "where a.id=? and a.security_level_id=b.id " + "and c.field=1 and a.doctype_path=d.doctype_path";
            if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
                sql += " and a.remark_prop=convert(varchar,c.id) ";
            } else {
                sql += " and a.remark_prop=c.id ";
            }
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                doc = new DocumentCBF();
                doc.setAbstractWords(resultSet.getString("abstract_words"));
                doc.setActyInstId(resultSet.getString("acty_inst_id"));
                doc.setAttachStatus(resultSet.getString("attach_status"));
                doc.setAuthor(resultSet.getString("Author"));
                doc.setChannelPath(resultSet.getString("Channel_Path"));
                doc.setContentFile(resultSet.getString("content_file"));
                doc.setCreateDate(resultSet.getTimestamp("create_date"));
                doc.setCreater(resultSet.getInt("creater"));
                doc.setDoctypePath(resultSet.getString("doctype_path"));
                doc.setInputTemplateId(resultSet.getInt("input_template_id"));
                doc.setEditorRemark(resultSet.getString("editor_remark"));
                doc.setEmitDate(resultSet.getTimestamp("emit_date"));
                doc.setEmitUnit(resultSet.getString("emit_unit"));
                doc.setHyperlink(resultSet.getString("hyperlink"));
                doc.setId(resultSet.getInt("id"));
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
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("�õ��ɱ෢�ĵ�ʵ��ʧ��,id=" + docID, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return doc;
    }

    /**
     * ����һ���ĵ�
     * @param doc �ĵ�����
     */
    public void add(DocumentCBF doc) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "insert into t_ip_doc(id,acty_inst_id,doctype_path,channel_path,content_file,attach_status,year_no,periodical_no,word_no," + "title,title_color,sub_title,author,emit_date,emit_unit,editor_remark,keywords,pertinent_words,abstract_words,source_id,security_level_id,creater,create_date,lastest_modify_date,remark_prop,notes,workflow_id,reservation1,reservation2,reservation3,reservation4,reservation5,reservation6,hyperlink)" + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, doc.getId());
            preparedStatement.setLong(2, Long.parseLong(doc.getActyInstId()));
            preparedStatement.setString(3, doc.getDoctypePath());
            preparedStatement.setString(4, doc.getChannelPath());
            preparedStatement.setString(5, doc.getContentFile());
            preparedStatement.setString(6, doc.getAttachStatus());
            preparedStatement.setInt(7, doc.getYearNo());
            preparedStatement.setInt(8, doc.getPeriodicalNo());
            preparedStatement.setInt(9, doc.getWordNo());
            preparedStatement.setString(10, doc.getTitle());
            preparedStatement.setString(11, doc.getTitleColor());
            preparedStatement.setString(12, doc.getSubTitle());
            preparedStatement.setString(13, doc.getAuthor());
            preparedStatement.setTimestamp(14, (Timestamp) doc.getEmitDate());
            preparedStatement.setString(15, doc.getEmitUnit());
            preparedStatement.setString(16, doc.getEditorRemark());
            preparedStatement.setString(17, doc.getKeywords());
            preparedStatement.setString(18, doc.getPertinentWords());
            preparedStatement.setString(19, doc.getAbstractWords());
            preparedStatement.setInt(20, doc.getSourceId());
            preparedStatement.setInt(21, doc.getSecurityLevelId());
            preparedStatement.setInt(22, doc.getCreater());
            preparedStatement.setTimestamp(23, (Timestamp) doc.getCreateDate());
            preparedStatement.setTimestamp(24, (Timestamp) doc.getLastestModifyDate());
            preparedStatement.setString(25, String.valueOf(doc.getRemarkProp()));
            preparedStatement.setString(26, doc.getNotes());
            preparedStatement.setInt(27, 0);
            preparedStatement.setString(28, doc.getReservation1());
            preparedStatement.setString(29, doc.getReservation2());
            preparedStatement.setString(30, doc.getReservation3());
            preparedStatement.setString(31, doc.getReservation4());
            preparedStatement.setString(32, doc.getReservation5());
            preparedStatement.setString(33, doc.getReservation6());
            preparedStatement.setString(34, doc.getHyperlink());
            preparedStatement.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("�����ĵ�ʧ��,�ĵ�id=" + doc.getId(), ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * �޸�һ���ĵ�
     * @param doc �ĵ�����
     */
    public void update(DocumentCBF doc) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "update t_ip_doc set id=?,acty_inst_id=?,doctype_path=?,channel_path=?,content_file=?,attach_status=?,year_no=?,periodical_no=?,word_no=?,title=?,title_color=?,sub_title=?,author=?,emit_date=?,emit_unit=?,editor_remark=?,keywords=?,pertinent_words=?,abstract_words=?,source_id=?,security_level_id=?,creater=?,create_date=?,lastest_modify_date=?,remark_prop=?,notes=?,workflow_id=?,reservation1=?,reservation2=?,reservation3=?,reservation4=?,reservation5=?,reservation6=?,hyperlink=? where id=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, doc.getId());
            preparedStatement.setLong(2, Long.parseLong(doc.getActyInstId()));
            preparedStatement.setString(3, doc.getDoctypePath());
            preparedStatement.setString(4, doc.getChannelPath());
            preparedStatement.setString(5, doc.getContentFile());
            preparedStatement.setString(6, doc.getAttachStatus());
            preparedStatement.setInt(7, doc.getYearNo());
            preparedStatement.setInt(8, doc.getPeriodicalNo());
            preparedStatement.setInt(9, doc.getWordNo());
            preparedStatement.setString(10, doc.getTitle());
            preparedStatement.setString(11, doc.getTitleColor());
            preparedStatement.setString(12, doc.getSubTitle());
            preparedStatement.setString(13, doc.getAuthor());
            preparedStatement.setTimestamp(14, (Timestamp) doc.getEmitDate());
            preparedStatement.setString(15, doc.getEmitUnit());
            preparedStatement.setString(16, doc.getEditorRemark());
            preparedStatement.setString(17, doc.getKeywords());
            preparedStatement.setString(18, doc.getPertinentWords());
            preparedStatement.setString(19, doc.getAbstractWords());
            preparedStatement.setInt(20, doc.getSourceId());
            preparedStatement.setInt(21, doc.getSecurityLevelId());
            preparedStatement.setInt(22, doc.getCreater());
            preparedStatement.setTimestamp(23, (Timestamp) doc.getCreateDate());
            preparedStatement.setTimestamp(24, (Timestamp) doc.getLastestModifyDate());
            preparedStatement.setString(25, String.valueOf(doc.getRemarkProp()));
            preparedStatement.setString(26, doc.getNotes());
            preparedStatement.setInt(27, 0);
            preparedStatement.setString(28, doc.getReservation1());
            preparedStatement.setString(29, doc.getReservation2());
            preparedStatement.setString(30, doc.getReservation3());
            preparedStatement.setString(31, doc.getReservation4());
            preparedStatement.setString(32, doc.getReservation5());
            preparedStatement.setString(33, doc.getReservation6());
            preparedStatement.setString(34, doc.getHyperlink());
            preparedStatement.setInt(35, doc.getId());
            preparedStatement.executeUpdate();
        } catch (Exception ex) {
            log.error("�����ĵ�ʧ��,�ĵ�id=" + doc.getId(), ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * �õ�ĳ�ĵ��ĸ����б�
     * @param docID �ĵ����
     * @return
     * @throws java.lang.Exception
     */
    public DocAffix[] getAffixList(int docID) throws Exception {
        DocAffix[] result;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.AFFIX_TYPE + "' order by order_no desc";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = new DocAffix[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = (DocAffix) DocResource.getInstance(Integer.parseInt((String) list.get(i)));
            }
        } catch (Exception ex) {
            log.error("�õ������б����", ex);
            ex.printStackTrace();
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * �õ�ĳ�ĵ���������Դ�б�
     * @param docID �ĵ����
     * @return
     * @throws java.lang.Exception
     */
    public DocContentResource[] getDocContentResourceList(int docID) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        DocContentResource[] result;
        String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_CONTENT_TYPE + "' order by order_no desc";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = new DocContentResource[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = (DocContentResource) DocResource.getInstance(Integer.parseInt((String) list.get(i)));
            }
        } catch (Exception ex) {
            log.error("�õ�������Դ�б����", ex);
            ex.printStackTrace();
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * �õ�ĳ�ĵ���ͼƬ�б�
     * @param docID �ĵ����
     * @return
     * @throws java.lang.Exception
     */
    public DocPicture[] getPictureList(int docID) throws Exception {
        DocPicture[] result;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.PICTURE_TYPE + "' and not (uri like 'min%') order by order_no desc";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = new DocPicture[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = (DocPicture) DocResource.getInstance(Integer.parseInt((String) list.get(i)));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("�õ�ͼƬ�б����", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * �õ�ĳ�ĵ���ͼƬ��
     * @param docID �ĵ����
     * @return int
     * @throws java.lang.Exception
     */
    public int getPictureListNum(int docID) throws Exception {
        int iPictureListNum = 0;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select count(id) from t_ip_doc_res where doc_id=? and type='" + DocResource.PICTURE_TYPE + "' and not (uri like 'min%') ";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                iPictureListNum = resultSet.getInt(1);
            }
        } catch (Exception ex) {
            log.error("�õ�ͼƬ�����", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return iPictureListNum;
    }

    /**
     * �õ�ĳ�ĵ�������ͼ�б�
     * @param docID �ĵ����
     * @return
     * @throws java.lang.Exception
     */
    public DocPicture[] getBreviaryImageList(int docID) throws Exception {
        DocPicture[] result;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.PICTURE_TYPE + "' and uri like 'min%' order by order_no desc";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = new DocPicture[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = (DocPicture) DocResource.getInstance(Integer.parseInt((String) list.get(i)));
            }
        } catch (Exception ex) {
            log.error("�õ�ͼƬ�б����", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * �õ�����ĵ��б�
     * @param docID �ĵ����
     * @return
     */
    public Document[] getCorrelationDocList(int docID) throws Exception {
        Document[] result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select uri from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_CORRELATION_TYPE + "'";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            String tmp = null;
            if (resultSet.next()) {
                tmp = resultSet.getString(1);
            }
            String[] correlationDocIDs = Function.stringToArray(tmp);
            List list = new ArrayList();
            for (int i = 0; i < correlationDocIDs.length; i++) {
                DocumentCBF doc = DocumentCBF.getInstance(Integer.parseInt(correlationDocIDs[i]));
                if (doc != null) {
                    list.add(doc);
                }
            }
            if (list.size() > 0) {
                result = new DocumentCBF[list.size()];
            }
            for (int i = 0; i < list.size(); i++) {
                result[i] = (DocumentCBF) list.get(i);
            }
        } catch (Exception ex) {
            log.error("�õ��Ѿ�ѡ�е�����ĵ��б����!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * Ϊ���ĵ���������ĵ�
     * 
     * @param docID
     *            Ҫ��������ĵ����ĵ�id
     * @param correlationDocIDs
     *            Ҫ����Ϊ����ĵ����ĵ�id
     * @throws java.lang.Exception
     */
    public void addCorrelationDoc(int docID, int[] correlationDocIDs) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String cdi = "";
        for (int i = 0; i < correlationDocIDs.length; i++) {
            if (!cdi.equals("")) {
                cdi += ",";
            }
            cdi += correlationDocIDs[i];
        }
        try {
            String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_CORRELATION_TYPE + "'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            int resID = -1;
            DocResource dr = null;
            if (resultSet.next()) {
                resID = resultSet.getInt(1);
            }
            if (resID == -1) {
                dr = new DocResource();
                dr.setDocId(docID);
                dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                dr.setUri(cdi);
                dr.setType(DocResource.DOC_CORRELATION_TYPE);
                dr.add();
            } else {
                dr = DocResource.getInstance(resID);
                dr.setUri(cdi + (dr.getUri() == null ? "" : "," + dr.getUri()));
                dr.update();
            }
        } catch (Exception ex) {
            log.error("", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * Ϊ�ĵ������ڿ��ĵ�(���ĵ�)
     * 
     * @param docId
     *            Ҫ�����ڿ��ĵ����ĵ�id
     * @param magazineIds
     *            �ڿ��ĵ���id����
     * @throws java.lang.Exception
     */
    public void setMagazine(int docId, int[] magazineIds) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String cdi = "";
        for (int i = 0; i < magazineIds.length; i++) {
            if (!cdi.equals("")) {
                cdi += ",";
            }
            cdi += magazineIds[i];
        }
        try {
            String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_MAGAZINE_TYPE + "'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docId);
            resultSet = preparedStatement.executeQuery();
            int resId = -1;
            DocResource dr = null;
            if (resultSet.next()) {
                resId = resultSet.getInt(1);
            }
            if (resId == -1) {
                dr = new DocResource();
                dr.setDocId(docId);
                dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                dr.setUri(cdi);
                dr.setOrderNo(Integer.parseInt(new OrderNo().getDefaultValue()));
                dr.setType(DocResource.DOC_MAGAZINE_TYPE);
                dr.add();
            } else {
                dr = DocResource.getInstance(resId);
                if (dr == null) {
                    throw new Exception("�ĵ�����ɹ�,�����޸��ڿ��ĵ���ϵʧ��!����ԭ��:û���ҵ���Դid=" + resId + "���ڿ��ĵ���ϵ��Դ��¼!");
                }
                dr.setUri(cdi);
                dr.update();
            }
        } catch (Exception ex) {
            log.error("�����ڿ��ĵ�ʧ��!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * Ϊ�ĵ������ڿ��ĵ�(���ĵ�)
     * 
     * @param docId
     *            Ҫ�����ڿ��ĵ����ĵ�id
     * @param magazineIds
     *            �ڿ��ĵ���id����
     * @throws java.lang.Exception
     */
    public void addMagazine(int docId, int[] magazineIds) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String cdi = "";
        for (int i = 0; i < magazineIds.length; i++) {
            if (!cdi.equals("")) {
                cdi += ",";
            }
            cdi += magazineIds[i];
        }
        try {
            String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_MAGAZINE_TYPE + "'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docId);
            resultSet = preparedStatement.executeQuery();
            int resId = -1;
            DocResource dr = null;
            if (resultSet.next()) {
                resId = resultSet.getInt(1);
            }
            if (resId == -1) {
                dr = new DocResource();
                dr.setDocId(docId);
                dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                dr.setUri(cdi);
                dr.setOrderNo(Integer.parseInt(new OrderNo().getDefaultValue()));
                dr.setType(DocResource.DOC_MAGAZINE_TYPE);
                dr.add();
            } else {
                delMagazine(docId, magazineIds);
                dr = DocResource.getInstance(resId);
                if (dr == null) {
                    throw new Exception("�ĵ�����ɹ�,�����޸��ڿ��ĵ���ϵʧ��!����ԭ��:û���ҵ���Դid=" + resId + "���ڿ��ĵ���ϵ��Դ��¼!");
                }
                dr.setUri(cdi + (dr.getUri() == null ? "" : "," + dr.getUri()));
                dr.update();
            }
        } catch (Exception ex) {
            log.error("�����ڿ��ĵ�ʧ��!", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * ɾ����ĵ��Ĳ����ڿ��ĵ�
     * 
     * @param docId
     *            Ҫɾ���ڿ��ĵ����ĵ�id
     * @param delMagazineIds
     *            �ڿ��ĵ���id����
     * @throws java.lang.Exception
     */
    public void delMagazine(int docId, int[] delMagazineIds) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_MAGAZINE_TYPE + "'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docId);
            resultSet = preparedStatement.executeQuery();
            int resID = -1;
            DocResource dr = null;
            if (resultSet.next()) {
                resID = resultSet.getInt(1);
            } else {
                throw new Exception("û���ҵ��ĵ����Ϊ[" + docId + "]���ڿ��ĵ�!����ִ��ɾ�����!");
            }
            dr = DocResource.getInstance(resID);
            String[] oldDocIDs = Function.stringToArray(dr.getUri());
            ArrayList al = new ArrayList();
            for (int i = 0; i < oldDocIDs.length; i++) {
                if (!Function.aInArray(oldDocIDs[i], delMagazineIds)) {
                    al.add(oldDocIDs[i]);
                }
            }
            String[] newDocIDs = new String[al.size()];
            for (int i = 0; i < al.size(); i++) {
                newDocIDs[i] = (String) al.get(i);
            }
            dr.setUri(Function.arrayToStr(newDocIDs));
            dr.update();
        } catch (Exception ex) {
            log.error("", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * ɾ����ĵ��Ĳ�������ĵ�
     * 
     * @param docID
     *            Ҫɾ������ĵ����ĵ�id
     * @param correlationDocIDs
     *            Ҫɾ�������ĵ����ĵ�id
     * @throws java.lang.Exception
     */
    public void delCorrelationDoc(int docID, int[] delDocIDs) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "select id from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_CORRELATION_TYPE + "'";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            resultSet = preparedStatement.executeQuery();
            int resID = -1;
            DocResource dr = null;
            if (resultSet.next()) {
                resID = resultSet.getInt(1);
            } else {
                throw new Exception("û���ҵ��ĵ����Ϊ[" + docID + "]������ĵ�!����ִ��ɾ�����!");
            }
            dr = DocResource.getInstance(resID);
            String[] oldDocIDs = Function.stringToArray(dr.getUri());
            ArrayList al = new ArrayList();
            for (int i = 0; i < oldDocIDs.length; i++) {
                if (!Function.aInArray(oldDocIDs[i], delDocIDs)) {
                    al.add(oldDocIDs[i]);
                }
            }
            String[] newDocIDs = new String[al.size()];
            for (int i = 0; i < al.size(); i++) {
                newDocIDs[i] = (String) al.get(i);
            }
            dr.setUri(Function.arrayToStr(newDocIDs));
            dr.update();
        } catch (Exception ex) {
            log.error("", ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * Ϊ���ĵ��ϴ�����
     * 
     * @param doc
     *            �ĵ�����
     * @param HttpServletRequest
     * @param HttpServletResponse
     */
    public void uploadAffix(Document doc, HttpServletRequest request, HttpServletResponse response) throws Exception {
        upload(doc, request, response, DocResource.AFFIX_TYPE);
    }

    /**
     * �õ�ftpҪ�ϴ���Ŀ¼
     * 
     * @param doc
     *            �ĵ�����
     * @return String �ϴ���Ŀ¼
     */
    public String getUploadPath(Document doc) throws Exception {
        CesGlobals cesGlobals = new CesGlobals();
        String uploadPath;
        cesGlobals.setConfigFile("platform.xml");
        uploadPath = cesGlobals.getCesXmlProperty("platform.datadir");
        uploadPath = new File(uploadPath).getPath();
        if (uploadPath.endsWith("\\")) {
            uploadPath = uploadPath.substring(0, uploadPath.length() - 1);
        }
        String tempUploadPath = new File(uploadPath + "/infoplat/temp/").getPath();
        uploadPath = uploadPath + "/infoplat/workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/res/";
        if (!new File(uploadPath).isDirectory()) {
            new File(uploadPath).mkdirs();
        }
        if (!new File(tempUploadPath).isDirectory()) {
            new File(tempUploadPath).mkdirs();
        }
        return uploadPath;
    }

    /**
     * �ϴ�����
     * 
     * @param docId
     *            �ĵ����
     * @param fileProp
     *            ������
     * @param uri
     *            ����·��
     */
    public void storeAffixInfo(int docId, String fileProp, String uri) throws Exception {
        DocResource dr = new DocAffix();
        dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
        dr.setAutoPlay(false);
        dr.setCreateDate(Function.getSysTime());
        dr.setCreater(1);
        dr.setDocId(docId);
        String[] props = Function.stringToArray(fileProp, "*");
        String original = props[0];
        int fileSize = Integer.parseInt(props[1]);
        dr.setFileExt(original.substring(original.lastIndexOf(".") + 1));
        dr.setFileSize(Function.byteToKB(fileSize));
        dr.setOrderNo(Integer.parseInt(new ces.platform.infoplat.ui.common.defaultvalue.OrderNo().getDefaultValue()));
        dr.setOriginalFile(original);
        dr.setUri(uri + original);
        dr.add();
    }

    /**
     * �ϴ����ļ� ʹ��FTP��ʽ�ϴ����ļ�,�ļ��������ܴ���
     * 
     * @param docId
     *            �ĵ����
     * @param uploaded
     *            ��������
     * @param uri
     *            ����·��
     */
    public void uploadBigFile(int docId, String uploaded[], String uri) throws Exception {
        for (int i = 0; i < uploaded.length; i++) {
            storeAffixInfo(docId, uploaded[i], uri);
        }
    }

    private void upload(Document doc, HttpServletRequest request, HttpServletResponse response, String uploadType) throws Exception {
        CesGlobals cesGlobals = new CesGlobals();
        String uploadPath;
        cesGlobals.setConfigFile("platform.xml");
        uploadPath = cesGlobals.getCesXmlProperty("platform.datadir");
        uploadPath = new File(uploadPath).getPath();
        if (uploadPath.endsWith("\\")) {
            uploadPath = uploadPath.substring(0, uploadPath.length() - 1);
        }
        String tempUploadPath = new File(uploadPath + "/infoplat/temp/").getPath();
        uploadPath = uploadPath + "/infoplat/workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/res/";
        if (!new File(uploadPath).isDirectory()) {
            new File(uploadPath).mkdirs();
        }
        if (!new File(tempUploadPath).isDirectory()) {
            new File(tempUploadPath).mkdirs();
        }
        try {
            DiskFileUpload fu = new DiskFileUpload();
            fu.setSizeMax(10194304);
            fu.setSizeThreshold(4096);
            fu.setRepositoryPath(tempUploadPath);
            List fileItems = null;
            try {
                fileItems = fu.parseRequest(request);
            } catch (FileUploadException ex) {
                String sMessage = ex.toString();
                if (sMessage.indexOf("size exceeds allowed range") >= 0) {
                    throw new Exception("����ϴ�10M�ļ�������10M��������FTP�ϴ���");
                } else {
                    throw new Exception(ex.toString());
                }
            }
            Iterator i = fileItems.iterator();
            String fileName = "";
            String fileExt, sTemp;
            while (i.hasNext()) {
                FileItem fi = (FileItem) i.next();
                fileName = fi.getName();
                long fileSize = fi.getSize();
                if (fileSize != 0 && fileName != null) {
                    MD5 strMD5 = new MD5();
                    fileExt = "";
                    sTemp = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
                    if (sTemp.lastIndexOf(".") > 0) fileExt = sTemp.substring(sTemp.lastIndexOf(".") + 1);
                    fileName = "t" + strMD5.getMD5ofStr(fileName + fi.getSize() + (new Random()).nextInt());
                    if (!"".equals(fileExt)) fileName += "." + fileExt;
                    File f = new File(uploadPath + fileName);
                    fi.write(f);
                    String breviaryImageFileName = "min" + f.getName();
                    if (DocResource.PICTURE_TYPE.equals(uploadType)) {
                        ConfigInfo ci = ConfigInfo.getInstance();
                        ImgHandle imgHandle = new ImgHandle();
                        try {
                            imgHandle.zoom(f.getPath(), f.getParent() + File.separator + breviaryImageFileName, ci.getImageMinHeight(), ci.getImageMinWidth());
                        } catch (Exception ex1) {
                            breviaryImageFileName = "";
                        }
                    }
                    DocResource dr = null;
                    if (DocResource.AFFIX_TYPE.equals(uploadType)) {
                        dr = new DocAffix();
                    } else if (DocResource.PICTURE_TYPE.equals(uploadType)) {
                        dr = new DocPicture();
                    }
                    dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                    dr.setAutoPlay(false);
                    dr.setCreateDate(Function.getSysTime());
                    dr.setCreater(1);
                    dr.setDocId(doc.getId());
                    dr.setFileExt(fileExt);
                    dr.setFileSize(Function.byteToKB(fileSize));
                    dr.setOrderNo(Integer.parseInt(new ces.platform.infoplat.ui.common.defaultvalue.OrderNo().getDefaultValue()));
                    dr.setOriginalFile(new File(fi.getName()).getName());
                    dr.setUri(fileName);
                    dr.add();
                    if (DocResource.PICTURE_TYPE.equals(uploadType) && !breviaryImageFileName.equals("")) {
                        dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                        dr.setCreateDate(Function.getSysTime());
                        dr.setFileSize(Function.byteToKB(fileSize));
                        dr.setOrderNo(Integer.parseInt(new ces.platform.infoplat.ui.common.defaultvalue.OrderNo().getDefaultValue()));
                        dr.setOriginalFile("(����ͼ)" + new File(fi.getName()).getName());
                        dr.setUri(breviaryImageFileName);
                        dr.add();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("aaa:= " + e.getMessage());
            log.error("�ϴ�ʧ��!", e);
            throw e;
        }
    }

    /**
     * Ϊ���ĵ��ϴ�ͼƬ
     * 
     * @param doc
     *            �ĵ�����
     * @param HttpServletRequest
     * @param HttpServletResponse
     */
    public void uploadPicture(Document doc, HttpServletRequest request, HttpServletResponse response) throws Exception {
        upload(doc, request, response, DocResource.PICTURE_TYPE);
    }

    /**
     * ��ȡ���ĵ�����ʷ������Ϣ�б�
     * 
     * @param docID
     * @return ����Ҳ���,����new DocApprove[0]
     * @throws java.lang.Exception
     */
    public DocApprove[] getHistoryApprove(int docID) throws Exception {
        DocApprove[] result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select id from t_ip_doc_approve where doc_id=" + docID + " order by id desc";
        try {
            dbo = createDBOperation();
            resultSet = dbo.select(sql);
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = new DocApprove[list.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = DocApprove.getInstance(Integer.parseInt((String) list.get(i)));
            }
        } catch (Exception ex) {
            log.error("��ȡ��ʷ������Ϣ�б�ʧ��,doc_id=" + docID, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * ɾ���ĵ� <br>
     * ˼· <br>
     * �Ȳ�ѯ���ĵ������е���Դ(����,ͼƬ,����ĵ�,��������)��id, <br>
     * ������DocResource��getInstance�෽����ȡ��Щ��Դ��ʵ��,Ȼ�������Դ��ʵ��deleteɾ����Դ <br>
     * ���ɾ���ĵ��������Ϣ <br>
     * ���ѹ�������Ϣ����Ϊ��ͣ
     * 
     * @param userId
     *            ɾ����
     * @param date
     *            ɾ������
     * @param hasWorkFlow
     *            �Ƿ���Ҫ��ͣ������,Ҳ���Ǵ��ĵ����޹�����
     * @throws java.lang.Exception
     */
    public void delete(int userId, DocumentCBF doc, boolean hasWorkFlow) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "select id from t_ip_doc_res where doc_id=" + doc.getId() + " order by id desc";
            dbo = createDBOperation();
            resultSet = dbo.select(sql);
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            for (int i = 0; i < list.size(); i++) {
                DocResource.getInstance(Integer.parseInt((String) list.get(i))).delete();
            }
            String docContentUrl = ConfigInfo.getInstance().getInfoplatDataDir() + "/workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/d_" + doc.getId();
            FileOperation.deleteFile(docContentUrl + ".data");
            FileOperation.deleteFile(docContentUrl + ".doc");
            FileOperation.deleteFile(docContentUrl + ".htm");
            FileOperation.deleteDirectory(docContentUrl + ".files");
            sql = "delete from t_ip_doc where id=" + doc.getId();
            dbo.delete(sql);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("ɾ���ĵ�ʧ��,doc_id=" + doc.getId(), ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * �ĵ�����,������˼·���� <br>
     * 1.���ĵ���path���뵽�������� <br>
     * 2.���ж�̬ģ��,���html�ļ� 3.����Ƶ������״̬ 4.ִ�й���������һ��(��ѡ)
     * 
     * @param doc
     * @param publishPaths
     * @param isNextStep
     *            �Ƿ�ִ�й���������һ��,��Ϊ,���·�������Ҫִ�й���������һ��,������Ҫ�Ӹ�����
     * @throws java.lang.Exception
     */
    public void publish(DocumentCBF doc, String[] publishPaths, int publisher, Date publishDate, Date validStartDate, Date validEndDate, boolean isNextStep) throws Exception {
        if (doc == null || publishPaths == null) {
            throw new Exception("����ʱ,�ĵ�ʵ��ͷ�����path����Ϊ��!");
        }
        HashSet hsSite = new HashSet();
        for (int i = 0; i < publishPaths.length; i++) {
            if (publishPaths[i] == null || publishPaths[i].length() < 10) {
                log.error("Ҫ����ķ���·����,��" + i + "��path����ȷ,path=" + publishPaths[i] + ",��path�ϵķ���ʧ��!");
                continue;
            }
            insertToBrowse(doc, publishPaths[i], publisher, publishDate, validStartDate, validEndDate);
            if (!hsSite.contains(publishPaths[i].substring(0, 10))) {
                generateHTML(doc, publishPaths[i], true);
            }
            hsSite.add(publishPaths[i].substring(0, 10));
        }
    }

    /**
     * ���ĵ���path���뵽��������
     * 
     * @param doc
     * @param publishPath
     * @param publisher
     * @param publishDate
     * @param validStartDate
     * @param validEndDate
     * @throws Exception
     */
    void insertToBrowse(DocumentCBF doc, String publishPath, int publisher, Date publishDate, Date validStartDate, Date validEndDate) throws Exception {
        DocumentPublish tmpDoc = new DocumentPublish(doc, publishPath, publisher, publishDate, validStartDate, validEndDate, Integer.parseInt(new OrderNo().getDefaultValue()));
        tmpDoc.delete();
        tmpDoc.add();
    }

    /**
     * ���html�ļ�
     * 
     * @param doc
     * @param publishPath
     * @param isCopyRes
     * @throws Exception
     */
    public void generateHTML(DocumentCBF doc, String publishPath, boolean isCopyRes) throws Exception {
        String sitePath = "";
        if (!publishPath.substring(0, 10).equals(sitePath)) {
            sitePath = publishPath.substring(0, 10);
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
            Template template = getTemplate(doc, publishPath);
            String templateJspUrl = getPreviewUrl(doc, publishPath, template);
            try {
                new GenHtml().generate(templateJspUrl, outFile, true, false);
            } catch (Exception ex2) {
                ex2.printStackTrace();
                String error = "���HTML�ļ����?" + "templateJspUrl=" + templateJspUrl;
                log.error(error);
                DocumentPublish.getInstance(publishPath, doc.getId()).delete();
                throw new Exception(error);
            }
            if (isCopyRes) copyResources(doc, template.getAsciiName(), outFileDir);
        }
    }

    /**
     * ������Դ�ļ�
     * 
     * @param doc
     * @param template
     * @param outFileDir
     * @throws Exception
     */
    void copyResources(DocumentCBF doc, String template, String outFileDir) throws Exception {
        File fTmp = null;
        File fTmp1 = null;
        String publishTemplateResDir = outFileDir + "/res_" + template + "/";
        String templateResDir = new File(ConfigInfo.getInstance().getInfoplatDataDir()).getPath() + "/template/dynamic/" + template + "/res_" + template + "/";
        try {
            fTmp = new File(publishTemplateResDir);
            if (!fTmp.exists()) {
                fTmp1 = new File(templateResDir);
                if (fTmp1.exists()) {
                    fTmp.mkdirs();
                    templateResDir = new File(templateResDir).getPath() + File.separator;
                    publishTemplateResDir = new File(publishTemplateResDir).getPath() + File.separator;
                    ces.coral.file.FileOperation.copyDir(templateResDir, publishTemplateResDir);
                }
            }
        } catch (Exception ex1) {
            log.error("templateResDir=" + templateResDir);
            log.error("publishTemplateResDir=" + publishTemplateResDir);
            log.error("copyģ����Դʧ��!", ex1);
        }
        String docResDir = new File(ConfigInfo.getInstance().getInfoplatDataDir()).getPath() + "/workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/res/";
        log.debug("�ĵ���Դ(ͼƬ,������Դ)��·��=" + docResDir);
        String publishDocResDir = outFileDir + "/res/";
        fTmp = new File(publishDocResDir);
        if (!fTmp.exists()) {
            fTmp.mkdirs();
        }
        try {
            String[] resFileNames = doc.getAllResourceUri();
            for (int j = 0; j < resFileNames.length; j++) {
                FileOperation.copy(docResDir + resFileNames[j], publishDocResDir + resFileNames[j], true);
            }
        } catch (Exception ex1) {
            log.error("copy�ĵ���Դʧ��!", ex1);
        }
        docResDir = new File(ConfigInfo.getInstance().getInfoplatDataDir()).getPath() + "/workflow/docs/" + Function.getNYofDate(doc.getCreateDate()) + "/d_" + doc.getId() + ".files/";
        log.debug("�ĵ���Դ(������Դ)��·��=" + docResDir);
        publishDocResDir = outFileDir + "/d_" + doc.getId() + ".files/";
        try {
            fTmp = new File(docResDir);
            if (fTmp.exists()) {
                docResDir = fTmp.getPath() + File.separator;
                fTmp = new File(publishDocResDir);
                if (!fTmp.exists()) {
                    fTmp.mkdirs();
                }
                publishDocResDir = fTmp.getPath() + File.separator;
                FileOperation.copyDir(docResDir, publishDocResDir);
            }
        } catch (Exception ex1) {
            log.error("copy�ĵ�������Դʧ��!", ex1);
        }
    }

    /**
     * �õ���ƪ�ĵ������е���Դ��uri,��������ĵ����� <br>
     * �˷��������ĵ�����ʱ,copy�ĵ���Դ������Ŀ¼��
     * 
     * @param docId
     *            �ĵ����
     * @return ��Դ��uri����
     */
    public String[] getAllResourceUri(int docId) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String[] result;
        try {
            String sql = "select uri from t_ip_doc_res where doc_id=" + docId + " and type<>'" + DocResource.DOC_CORRELATION_TYPE + "' order by id desc";
            dbo = createDBOperation();
            resultSet = dbo.select(sql);
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = Function.list2StringArray(list);
        } catch (Exception ex) {
            log.error("�õ��ĵ���������Դ��uriʧ��,doc_id=" + docId, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * �õ���ƪ�ĵ�����������ЩƵ����path��
     * 
     * @param docId
     *            �ĵ����
     * @return Ƶ����path��uri����
     * @throws java.lang.Exception
     */
    public String[] getPublishedChannelPath(int docId) throws Exception {
        String[] result;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "select channel_path from t_ip_browse where doc_id=" + docId + " order by channel_path";
            dbo = createDBOperation();
            resultSet = dbo.select(sql);
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = Function.list2StringArray(list);
        } catch (Exception ex) {
            log.error("�õ����ĵ����з������Ƶ��ʧ��,doc_id=" + docId, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * �õ���ƪ�ĵ������е���Դ����
     * 
     * @param docId
     *            �ĵ����
     * @return ��Դ���������
     * @return
     */
    public DocResource[] getAllResources(int docId) throws Exception {
        DocResource[] result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "select id from t_ip_doc_res where doc_id=" + docId + "";
            dbo = createDBOperation();
            resultSet = dbo.select(sql);
            List list = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
            result = new DocResource[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = DocResource.getInstance(Integer.parseInt((String) list.get(i)));
            }
        } catch (Exception ex) {
            log.error("�õ����ĵ�������Դʧ��,doc_id=" + docId, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * �����ĵ������ļ�����
     * 
     * @param doc
     * @param strContent
     * @throws java.lang.Exception
     */
    public void saveContent(DocumentCBF doc, String strContent) throws Exception {
        saveContent(doc, strContent, true);
    }

    /**
     * �����ĵ������ļ�����(�ж��Ƿ�Ϊͼ�Ļ��ţ�
     * 
     * @param doc
     * @param strContent
     * @throws java.lang.Exception
     */
    public void saveContent(DocumentCBF doc, String strContent, boolean blRte) throws Exception {
        String strCreateYearMonth = null;
        String strFilePath = null;
        String strFileName = null;
        String[] resFileNames = null;
        try {
            strCreateYearMonth = Function.getNYofDate(doc.getCreateDate());
            strFilePath = cfg.getPlatformDataDir() + "/infoplat/workflow/docs/" + strCreateYearMonth;
            strFileName = "d_" + doc.getId() + ".data";
        } catch (Exception ex) {
            log.error("��Ϣƽ̨�������������ݵ��ļ����?��ȡ����", ex);
            throw ex;
        }
        try {
            File file = new File(strFilePath + "/" + strFileName);
            File fileParent = file.getParentFile();
            if (!fileParent.exists()) {
                fileParent.mkdirs();
            }
            FileOutputStream fileoutputstream = new FileOutputStream(file);
            fileoutputstream.write(strContent.getBytes());
            fileoutputstream.close();
            ContentHtmlParser chp = null;
            if (blRte) {
                chp = new ContentHtmlParser(strFilePath + "/" + strFileName, strFilePath);
                resFileNames = chp.parserHTMLContent();
                strContent = chp.getHtmlContent();
                fileoutputstream = new FileOutputStream(file);
                fileoutputstream.write(strContent.getBytes("ISO-8859-1"));
                fileoutputstream.close();
            } else {
                strContent = StringUtil.replaceAll(strContent, "\r\n", "<br>");
                strContent = StringUtil.replaceAll(strContent, " ", "&nbsp;");
                fileoutputstream = new FileOutputStream(file);
                fileoutputstream.write(strContent.getBytes("GBK"));
                fileoutputstream.close();
            }
            file = null;
        } catch (IOException ex) {
            log.error("��Ϣƽ̨�������������ݵ��ļ����?", ex);
            throw ex;
        }
        if (blRte) {
            DocResource[] drs = doc.getDocContentResourceList();
            for (int i = 0; i < drs.length; i++) {
                drs[i].delete();
            }
            DocResource dr = new DocContentResource();
            dr.setCreateDate(Function.getSysTime());
            dr.setCreater(doc.getCreater());
            dr.setDocId(doc.getId());
            for (int i = 0; i < resFileNames.length; i++) {
                dr.setId((int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC_RES));
                dr.setOrderNo(Integer.parseInt(new OrderNo().getDefaultValue()));
                dr.setUri(resFileNames[i]);
                dr.add();
            }
        }
    }

    /**
     * ����ĵ������ļ�����
     * 
     * @param doc
     *            �ĵ�����
     * @return �����ļ�����
     * @throws java.lang.Exception
     */
    public String getContent(DocumentCBF doc) {
        String strContent = "";
        String strCreateYearMonth = Function.getNYofDate(doc.getCreateDate());
        String strFilePathName = cfg.getPlatformDataDir() + "/infoplat/workflow/docs/";
        strFilePathName += strCreateYearMonth + "/d_" + doc.getId();
        File file = new File(strFilePathName + ".htm");
        if (file == null || !file.exists()) {
            file = new File(strFilePathName + ".data");
            if (file == null || !file.exists()) {
                return "";
            }
        }
        try {
            strContent = Function.readHtmlFile(file.getPath());
        } catch (Exception ex) {
            log.error("����ĵ������ļ�����", ex);
        }
        return strContent;
    }

    /**
     * ����ĵ������ļ�����
     * 
     * @param doc
     * @throws java.lang.Exception
     */
    public String getContentName(DocumentCBF doc) {
        String strCreateYearMonth = Function.getNYofDate(doc.getCreateDate());
        String strFilePathName = cfg.getPlatformDataDir() + "/infoplat/workflow/docs/";
        strFilePathName += strCreateYearMonth + "/d_" + doc.getId() + ".doc";
        return strFilePathName;
    }

    /**
     * �����ĵ�������޸�ʱ��
     * 
     * @param docId
     *            Ҫ�޸�����޸�ʱ����ĵ�id
     */
    public void updateLastModifyDate(int docId) {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "update t_ip_doc set lastest_modify_date=? where id=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setTimestamp(1, Function.getSysTime());
            preparedStatement.setInt(2, docId);
            preparedStatement.executeUpdate();
        } catch (Exception ex) {
            log.error("�����ĵ�������޸�ʱ��ʧ�ܣ�", ex);
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * �ѱ��ĵ�����,���ƺ���ĵ�,title�ֶ�ǰ�����'(����)'������,������� <br>
     * 
     * @param channelPath
     *            Ҫ���Ƶ��ĸ�Ƶ����(ֻ��ָ��һ��,����Ϊnull)
     * @param docTypePath
     *            ���ƺ���ĵ�����path(ֻ��ָ��һ��)
     * @param loginUserId
     *            ��ǰ��¼�û���id
     * @throws java.lang.Exception
     */
    public void copyNew(DocumentCBF doc, String channelPath, String docTypePath, int loginUserId) throws Exception {
        int oldDocId = doc.getId();
        int newDocId = (int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC);
        doc.setId(newDocId);
        doc.setChannelPath(channelPath);
        doc.setDoctypePath(docTypePath);
        IpWfControl IpWf = new IpWfControl();
        int process_Id = Integer.parseInt(IpWf.getProcessId(docTypePath));
        String processInstId = IpWf.getProcess_Inst_Id("" + loginUserId, process_Id, docTypePath);
        doc.setActyInstId(processInstId);
        doc.setTitle("(����)" + doc.getTitle());
        doc.setCreater(loginUserId);
        Timestamp newDocCreateDate = Function.getSysTime();
        Date oldDocCreateDate = doc.getCreateDate();
        doc.setCreateDate(newDocCreateDate);
        doc.setLastestModifyDate(newDocCreateDate);
        String oldDocContentUrl = cfg.getInfoplatDataDir() + "/workflow/docs/" + Function.getNYofDate(oldDocCreateDate) + "/d_" + oldDocId;
        String newDocContentUrl = cfg.getInfoplatDataDir() + "/workflow/docs/" + Function.getNYofDate(newDocCreateDate);
        FileOperation.makeDirectory(newDocContentUrl);
        newDocContentUrl += "/d_" + newDocId;
        FileOperation.copy(oldDocContentUrl + ".data", newDocContentUrl + ".data", true);
        FileOperation.copy(oldDocContentUrl + ".doc", newDocContentUrl + ".doc", true);
        if (new File(oldDocContentUrl + ".htm").exists()) {
            FileOperation.copy(oldDocContentUrl + ".htm", newDocContentUrl + ".htm", true);
            if (new File(oldDocContentUrl + ".files").exists()) {
                FileOperation.copyDir(oldDocContentUrl + ".files", newDocContentUrl + ".files");
                if (new File(newDocContentUrl + ".files/filelist.xml").exists()) {
                    RE re = new RE();
                    RECompiler compiler = new RECompiler();
                    String content = Function.readTextFile(newDocContentUrl + ".files/filelist.xml");
                    re.setProgram(compiler.compile("/d_" + oldDocId + ".htm"));
                    content = re.subst(content, "/d_" + newDocId + ".htm");
                    Function.writeTextFile(content, newDocContentUrl + ".files/filelist.xml", true);
                    content = Function.readTextFile(newDocContentUrl + ".htm");
                    re.setProgram(compiler.compile("/d_" + oldDocId + ".files/"));
                    content = re.subst(content, "/d_" + newDocId + ".files/");
                    Function.writeTextFile(content, newDocContentUrl + ".htm", true);
                }
            }
        }
        doc.add();
        doc.setId(oldDocId);
        DocResource[] docResArr = doc.getAllResources();
        for (int i = 0; docResArr != null && i < docResArr.length; i++) {
            if (docResArr[i] != null) {
                docResArr[i].copyNew(newDocId, newDocCreateDate, oldDocCreateDate, loginUserId);
            }
        }
        doc.setId(newDocId);
    }

    /**
     * ����ָ���ڿ��ı����������ĵ�,�õݹ� <br>
     * 
     * @param doc
     *            Ҫ�������ĵ����ڿ��ĵ�һ���ĵ������ĵ����ĵ���Դ��
     * @param channelPath
     *            Ҫ���Ƶ��ĸ�Ƶ����(ֻ��ָ��һ��,����Ϊnull)
     * @param docTypePath
     *            ���ƺ���ĵ�����path(ֻ��ָ��һ��)
     * @param loginUserId
     *            ��ǰ��¼�û���id
     * @throws java.lang.Exception
     */
    public void copyNewMagazine(DocumentCBF doc, String channelPath, String docTypePath, int loginUserId) throws Exception {
        DocumentCBF[] dCBF = this.getMagazineList(doc.getId(), null);
        int[] magazineIds = null;
        if (dCBF != null && dCBF.length > 0) {
            magazineIds = new int[dCBF.length];
            for (int i = 0; i < dCBF.length; i++) {
                copyNewMagazine(dCBF[i], channelPath, docTypePath, loginUserId);
                magazineIds[i] = dCBF[i].getId();
            }
        }
        int oldDocId = doc.getId();
        int newDocId = (int) IdGenerator.getInstance().getId(IdGenerator.GEN_ID_IP_DOC);
        doc.setId(newDocId);
        doc.setChannelPath(channelPath);
        doc.setDoctypePath(docTypePath);
        doc.setCreater(loginUserId);
        Timestamp newDocCreateDate = Function.getSysTime();
        Date oldDocCreateDate = doc.getCreateDate();
        doc.setCreateDate(newDocCreateDate);
        doc.setLastestModifyDate(newDocCreateDate);
        String oldDocContentUrl = cfg.getInfoplatDataDir() + "/workflow/docs/" + Function.getNYofDate(oldDocCreateDate) + "/d_" + oldDocId;
        String newDocContentUrl = cfg.getInfoplatDataDir() + "/workflow/docs/" + Function.getNYofDate(newDocCreateDate) + "/d_" + newDocId;
        FileOperation.copy(oldDocContentUrl + ".data", newDocContentUrl + ".data", true);
        FileOperation.copy(oldDocContentUrl + ".doc", newDocContentUrl + ".doc", true);
        doc.add();
        doc.setId(oldDocId);
        DocResource[] docResArr = doc.getAllResources();
        for (int i = 0; docResArr != null && i < docResArr.length; i++) {
            if (docResArr[i] != null) {
                docResArr[i].copyNew(newDocId, newDocCreateDate, oldDocCreateDate, loginUserId);
            }
        }
        doc.setId(newDocId);
        if (magazineIds != null && magazineIds.length > 0) this.setMagazine(newDocId, magazineIds);
    }

    /**
     * �õ��ڿ��ĵ�����һ�����ڿ��б�
     * 
     * @param docId
     *            ���ڿ��ĵ�ID
     * @return ���ڿ��ĵ�����
     */
    public DocumentCBF[] getMagazineList(int docId, String condition) throws Exception {
        DocumentCBF[] result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int[] magazineIds;
        String sql = "select uri from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_MAGAZINE_TYPE + "'";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docId);
            resultSet = preparedStatement.executeQuery();
            String tmp = null;
            if (resultSet.next()) {
                tmp = resultSet.getString(1);
            }
            magazineIds = Function.strArray2intArray(Function.stringToArray(tmp));
        } catch (Exception ex) {
            log.error("�õ��ڿ��ĵ��б�ʧ��,�ڿ�ͷid=" + docId, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        result = Documents.getMainList(magazineIds, condition);
        DocumentCBF documentCBF;
        int iLength = magazineIds.length;
        int iLength1 = result.length;
        int i, j, k;
        for (i = 0, k = iLength1 - 1; i < iLength - 1; i++) {
            documentCBF = null;
            for (j = 0; j < iLength1; j++) {
                if (magazineIds[i] == ((DocumentCBF) result[j]).getId()) {
                    documentCBF = (DocumentCBF) result[j];
                    break;
                }
            }
            if (null != documentCBF) {
                result[j] = result[k];
                result[k] = documentCBF;
                k--;
            }
        }
        return result;
    }

    /**
     * �õ��ڿ��ĵ������ڿ���
     * 
     * @param docId
     *            ���ڿ��ĵ�ID
     * @param isRecu
     *            false ֻͳ����һ�����ڿ��� true ͳ�����������ڿ���
     * @return ���ڿ��ĵ���
     */
    public int getMagazineNum(int docId, boolean isRecu) throws Exception {
        int iReturn = 0;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select uri from t_ip_doc_res where doc_id=? and type='" + DocResource.DOC_MAGAZINE_TYPE + "'";
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            if (!isRecu) {
                preparedStatement.setInt(1, docId);
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int[] magazineIds = Function.strArray2intArray(Function.stringToArray(resultSet.getString(1)));
                    iReturn = magazineIds.length;
                }
            } else {
                iReturn = getMagazineNum(docId, preparedStatement);
            }
        } catch (Exception ex) {
            log.error("�õ����ڿ��ĵ���ʧ��,�ڿ�ͷid=" + docId, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return iReturn;
    }

    /**
     * �õ��ڿ��ĵ����������ڿ���,�ݹ����
     * 
     * @param docId
     *            �ڿ��ĵ�ID
     * @return �ڿ��ĵ���
     */
    private int getMagazineNum(int docId, PreparedStatement preparedStatement) throws Exception {
        int iReturn = 0;
        try {
            preparedStatement.setInt(1, docId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int[] magazineIds = Function.strArray2intArray(Function.stringToArray(resultSet.getString(1)));
                iReturn += magazineIds.length;
                for (int i = 0; i < magazineIds.length; i++) {
                    iReturn += getMagazineNum(magazineIds[i], preparedStatement);
                }
            }
        } catch (Exception ex) {
            log.error("�ݹ�õ����ڿ��ĵ���ʧ��,�ڿ�ͷid=" + docId, ex);
            throw ex;
        }
        return iReturn;
    }

    /**
     * �õ�Ԥ���ĵ���url/�����ĵ�����ʱ���·���� ҳ��Ԥ�������Ѳ����ô˷������������·��
     * 
     * @param doc
     * @param publishPath
     * @param template
     * @return
     * @throws Exception
     *             20050105 bill refactor the method getPreviewUrl(DocumentCBF
     *             doc, String publishPath)
     */
    private String getPreviewUrl(DocumentCBF doc, String publishPath, Template template) throws Exception {
        String templateJspUrl = ConfigInfo.getInstance().getInfoplatDataDirContext() + "template/dynamic/" + template.getAsciiName() + "/index_p.jsp?docId=" + doc.getId() + "&publishedPath=" + publishPath;
        log.debug("ģ��jsp������url=" + templateJspUrl);
        return templateJspUrl;
    }

    /**
     * �õ�Ԥ���ĵ���url
     * 
     * @param doc
     * @param publishPath
     * @return
     * @throws Exception
     *             20041213 bill add this method intending to preview the page
     *             of DocumentCBF
     */
    public String getPreviewUrl(DocumentCBF doc, String publishPath) {
        String url = null;
        Template template;
        try {
            template = doc.getTemplate(publishPath);
            url = "../../../platformData/infoplat/template/dynamic/" + template.getAsciiName() + "/index_p.jsp?docId=" + doc.getId() + "&publishedPath=" + publishPath;
        } catch (Exception e) {
            log.error("�õ�Ԥ��·������!docId=" + doc.getId() + " publishPath=" + publishPath);
        }
        return url;
    }

    /**
     * �õ�ĳ�ĵ���ĳƵ���ϵ���ʾģ��
     * @param doc �ĵ�����
     * @param publishPath Ƶ��Path
     * @return ��ʾģ�����
     * @throws Exception
     */
    public Template getTemplate(DocumentCBF doc, String publishPath) throws Exception {
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
            throw new Exception("û���ҵ��ĵ�ģ��!");
        }
        return template;
    }

    public static void main(String[] args) {
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
