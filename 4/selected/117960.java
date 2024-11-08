package ces.platform.infoplat.core.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ces.coral.dbo.DBOperation;
import ces.platform.infoplat.core.DocResource;
import ces.platform.infoplat.core.DocResourceRecycle;
import ces.platform.infoplat.core.DocumentRecycle;
import ces.platform.infoplat.core.base.BaseDAO;
import ces.platform.system.common.Constant;
import ces.platform.system.common.XmlInfo;

/**
 * <p>Title: ������Ϣƽ̨</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Company: �Ϻ�������Ϣ��չ���޹�˾</p>
 * @author ����
 * @version 2.5
 */
public class DocumentRecycleDAO extends BaseDAO {

    public DocumentRecycleDAO() {
    }

    /**
     * �õ�һ����ɾ����ĵ���ʵ��
     * @param docID
     * @param ts
     * @return
     * @throws java.lang.Exception
     */
    public DocumentRecycle getInstance(int docId, Timestamp deleteDate) throws Exception {
        DocumentRecycle doc = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            dbo = createDBOperation();
            connection = dbo.getConnection();
            String sql = "select a.*,b.name security_level_name,c.name remark_prop_name " + "from t_ip_doc_bak a,t_ip_security_level b,t_ip_code c " + "where a.id=? and a.bak_date=? and a.security_level_id=b.id and c.field=1 ";
            if (XmlInfo.getInstance().getSysDataBaseType().equalsIgnoreCase(Constant.SYBASE)) {
                sql += " and a.remark_prop=convert(varchar,c.id) ";
            } else {
                sql += " and a.remark_prop=c.id ";
            }
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docId);
            preparedStatement.setTimestamp(2, (Timestamp) deleteDate);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                doc = new DocumentRecycle();
                doc.setAbstractWords(resultSet.getString("abstract_words"));
                doc.setActyInstId(resultSet.getString("acty_inst_id"));
                doc.setAttachStatus(resultSet.getString("attach_status"));
                doc.setAuthor(resultSet.getString("Author"));
                doc.setChannelPath(resultSet.getString("Channel_Path"));
                doc.setContentFile(resultSet.getString("content_file"));
                doc.setCreateDate(resultSet.getTimestamp("create_date"));
                doc.setCreater(resultSet.getInt("creater"));
                doc.setDoctypePath(resultSet.getString("doctype_path"));
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
                int deleteUserId = resultSet.getInt("bakor");
                doc.setBakDate(resultSet.getTimestamp("bak_date"));
            }
        } catch (Exception ex) {
            log.error("�õ�����վ�ĵ�ʵ��ʧ��!docId=" + docId + ",ɾ������=" + deleteDate, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return doc;
    }

    /**
     * ����һ���ĵ�������վ<br>
     * ����������:<br>
     * 1.���ĵ��������ݷŵ�����վ
     * 2.����Դ�ŵ�����վ
     */
    public void add(DocumentRecycle doc, DocResource[] allResources) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        DocumentRecycle tmpDoc = DocumentRecycle.getInstance(doc.getId(), (Timestamp) doc.getBakDate());
        if (tmpDoc != null) {
            tmpDoc.delete(doc.getBakor());
        }
        try {
            String sql = "insert into t_ip_doc_bak(id,acty_inst_id,doctype_path,channel_path,content_file,attach_status,year_no,periodical_no,word_no,title,title_color,sub_title,author,emit_date,emit_unit,editor_remark,keywords,pertinent_words,abstract_words,source_id,security_level_id,creater,create_date,lastest_modify_date,remark_prop,notes,workflow_id,reservation1,reservation2,reservation3,reservation4,reservation5,reservation6,bakor,bak_date,bak_flag,hyperlink)" + " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
            preparedStatement.setInt(34, doc.getBakor());
            preparedStatement.setTimestamp(35, (Timestamp) doc.getBakDate());
            preparedStatement.setString(36, doc.isBakFlag() ? "1" : "0");
            preparedStatement.setString(37, doc.getHyperlink());
            preparedStatement.executeUpdate();
            for (int i = 0; i < allResources.length; i++) {
                new DocResourceRecycle(allResources[i], doc.getBakDate()).add();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("���ĵ��ŵ�����վʧ��,�ĵ�id=" + doc.getId(), ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
	 * �õ���ƪ�ĵ������е�ĳ���������Դ����
	 * @param docID
	 * @param bakDate ��������
	 * @return ������Դ��������
	 */
    public DocResourceRecycle[] getAllResources(int docID, Date bakDate) throws Exception {
        DocResourceRecycle[] result = null;
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            String sql = "select id,bak_date from t_ip_doc_res_bak where doc_id=? and bak_date=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, docID);
            preparedStatement.setTimestamp(2, (Timestamp) bakDate);
            resultSet = preparedStatement.executeQuery();
            List list = new ArrayList();
            List list1 = new ArrayList();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
                list1.add(resultSet.getTimestamp(2));
            }
            result = new DocResourceRecycle[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = DocResourceRecycle.getInstance(Integer.parseInt((String) list.get(i)), (Timestamp) list1.get(i));
            }
        } catch (Exception ex) {
            log.error("�õ����ĵ�������Դʧ��,doc_id=" + docID, ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
        return result;
    }

    /**
     * ɾ����ĵ�<br>
     * ˼·<br>
       �Ȳ�ѯ���ĵ������е���Դ(����,ͼƬ,����ĵ�,��������)��id,<br>
       ������DocResourceBak��getInstance�෽����ȡ��Щ��Դ��ʵ��,Ȼ�������Դ��ʵ��deleteɾ����Դ<br>
       ���ɾ���ĵ��������Ϣ<br>
       �����ֹ������
     * @param docID
     * @throws java.lang.Exception
     */
    public void delete(int userId, DocumentRecycle doc) throws Exception {
        DBOperation dbo = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            DocResourceRecycle[] docResBaks = doc.getAllResources();
            for (int i = 0; i < docResBaks.length; i++) {
                docResBaks[i].delete();
            }
            String sql = "delete from t_ip_doc_bak where id=? and bak_date=?";
            dbo = createDBOperation();
            connection = dbo.getConnection();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, doc.getId());
            preparedStatement.setTimestamp(2, (Timestamp) doc.getBakDate());
            preparedStatement.executeUpdate();
        } catch (Exception ex) {
            log.error("ɾ����ĵ�ʧ��,doc_id=" + doc.getId() + ",bak_date=" + doc.getBakDate(), ex);
            throw ex;
        } finally {
            close(resultSet, null, preparedStatement, connection, dbo);
        }
    }

    /**
     * �汾�ع���ֻ�ع���ݣ����ع�������Ϣ<br>
     * ����������<br>
     * 1.�ָ��ĵ���������(��ݿ��е����)<br>
     * 2.�ָ��ĵ���������<br>
     * 3.�ָ���Դ<br>
     * ��������(���?��):<br>
     * 1.��ɾ���ĵ��ɱ෢�е����(ɾ�����վ)<br>
     * 2.�����Ӳɱ෢�ĵ�
     */
    public void unRecycle(DocumentRecycle doc) throws Exception {
    }
}
