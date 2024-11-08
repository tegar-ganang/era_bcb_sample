package ces.platform.infoplat.utils.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import ces.coral.dbo.ERDBOperation;
import ces.platform.infoplat.core.base.BaseDAO;

/**
 * @author Administrator
 *
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class UpdateOrder extends BaseDAO {

    ERDBOperation dbo = null;

    ERDBOperation dbo1 = null;

    /**
	 * ����Ƶ���������
	 * @param channelPath
	 * @param dataField
	 */
    public void update(String channelPath, String dataField) {
        String sql = "select t.doc_id,t.title,t.channel_path,t.emit_date,t.order_no from t_ip_browse t " + "where channel_path  = '" + channelPath + "' order by " + dataField;
        String sql1 = "update t_ip_browse set order_no = ? where doc_id = ? and channel_path = '" + channelPath + "'";
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            dbo = (ERDBOperation) createDBOperation();
            dbo1 = (ERDBOperation) createDBOperation();
            rs = dbo.select(sql);
            int docId = 0;
            conn = dbo1.getConnection();
            conn.setAutoCommit(true);
            ps = conn.prepareStatement(sql1);
            boolean allSuccess = true;
            int start = 1;
            while (rs.next()) {
                docId = rs.getInt(1);
                ps.setInt(1, start++);
                ps.setInt(2, docId);
                int success = ps.executeUpdate();
                if (success != 1) {
                    allSuccess = false;
                }
            }
            if (allSuccess) {
                conn.commit();
                log.info("allSuccess!");
            } else {
                log.info("not all Success!");
            }
            conn.setAutoCommit(true);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("����orderNo���?", e);
        } finally {
            close(null, null, ps, conn, dbo1);
            close(rs, null, null, null, dbo);
        }
    }

    /**
	 * �������ڿ��������
	 * @param channelPath
	 * @param dataField
	 */
    public void update(String channelPath, String dataField, String fatherDocId) {
        String sqlInitial = "select uri from t_ip_doc_res where doc_id = '" + fatherDocId + "' and type=" + " '" + ces.platform.infoplat.core.DocResource.DOC_MAGAZINE_TYPE + "' ";
        String sqlsortURL = "update t_ip_doc_res set uri = ? where doc_id = '" + fatherDocId + "' " + " and type = '" + ces.platform.infoplat.core.DocResource.DOC_MAGAZINE_TYPE + "' ";
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement ps = null;
        try {
            dbo = (ERDBOperation) createDBOperation();
            String url = "";
            boolean flag = true;
            StringTokenizer st = null;
            conn = dbo.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(sqlInitial);
            rs = ps.executeQuery();
            if (rs.next()) url = rs.getString(1);
            if (!url.equals("")) {
                st = new StringTokenizer(url, ",");
                String sortDocId = "";
                while (st.hasMoreTokens()) {
                    if (flag) {
                        sortDocId = "'" + st.nextToken() + "'";
                        flag = false;
                    } else {
                        sortDocId = sortDocId + "," + "'" + st.nextToken() + "'";
                    }
                }
                String sqlsort = "select id from t_ip_doc where id in (" + sortDocId + ") order by " + dataField;
                ps = conn.prepareStatement(sqlsort);
                rs = ps.executeQuery();
                String sortURL = "";
                boolean sortflag = true;
                while (rs.next()) {
                    if (sortflag) {
                        sortURL = rs.getString(1);
                        sortflag = false;
                    } else {
                        sortURL = sortURL + "," + rs.getString(1);
                    }
                }
                ps = conn.prepareStatement(sqlsortURL);
                ps.setString(1, sortURL);
                ps.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            close(rs, null, ps, conn, dbo);
        }
    }

    /**
	 * �������ֶ����� 
	 * @param docID
	 * @param dataField
	 */
    public void affixorder(String docID, String dataField) {
        String sql = "select t.id from T_IP_DOC_RES t where DOC_ID  = " + docID + " order by " + dataField;
        log.debug("�������ֶ����� sql: " + sql);
        String sql1 = "update T_IP_DOC_RES set order_no = ? where id = ?";
        try {
            dbo = (ERDBOperation) createDBOperation();
            dbo1 = (ERDBOperation) createDBOperation();
            ResultSet rs = dbo.select(sql);
            int Id = 0;
            Connection conn = dbo1.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql1);
            boolean allSuccess = true;
            int start = 1;
            while (rs.next()) {
                Id = rs.getInt(1);
                ps.setInt(1, start++);
                ps.setInt(2, Id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("�������ֶ�������?", e);
        } finally {
            close(null, null, null, null, dbo1);
            close(null, null, null, null, dbo);
        }
    }
}
