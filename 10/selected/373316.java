package com.pbonhomme.xf.dao.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.pbonhomme.xf.dao.SQLService;
import com.pbonhomme.xf.san.SanBean;
import com.pbonhomme.xf.san.file.FileBean;
import com.pbonhomme.xf.san.file.FileDAO;
import com.pbonhomme.xf.san.mimetype.MimeTypeBean;

public class FileDAOImpl extends SQLService implements FileDAO {

    private static final Log log = LogFactory.getLog(FileDAOImpl.class);

    private static final String INSERT_FILE = "INSERT INTO FILE_IMAGE (FILE_ID, MIMETYPE_ID, SAN_ID, STATUS,CR_DATE) VALUES (?, ? ,? ,?,sysdate)";

    private static final String NEXT_FILE_ID = "SELECT SEC_ID_FILE_IMAGE.NEXTVAL FROM DUAL";

    private static final String SELECT_FILE_BY_ID = "SELECT FILE_ID, MIMETYPE_ID, SAN_ID, ABSOLUTE_PATH, NAME, CR_DATE, UP_DATE, STATUS FROM FILE_IMAGE WHERE FILE_ID = ?";

    private static final String DELETE_FILES_LOGIC = "UPDATE FILE_IMAGE SET STATUS = 2 WHERE FILE_ID = ?";

    private static final String UPDATE_FILE = "UPDATE FILE_IMAGE SET MIMETYPE_ID=?,SAN_ID=?,ABSOLUTE_PATH=?,NAME=?,STATUS=?,UP_DATE=sysdate WHERE FILE_ID=?";

    private static final int WORKFLOW_ATTENTE_VALIDATION = 3;

    private static final String NEXTVAL = "NEXTVAL";

    private static final String MIMETYPE_ID = "MIMETYPE_ID";

    private static final String FILE_ID = "FILE_ID";

    private static final String SAN_ID = "SAN_ID";

    private static final String ABSOLUTE_PATH = "ABSOLUTE_PATH";

    private static final String NAME = "NAME";

    private static final String CR_DATE = "CR_DATE";

    private static final String UP_DATE = "UP_DATE";

    private static final String STATUS = "STATUS";

    /**
	 * return FileId
	 */
    public long create(long mimeTypeId, long sanId) throws SQLException {
        long fileId = 0;
        DataSource ds = getDataSource(DEFAULT_DATASOURCE);
        Connection conn = ds.getConnection();
        try {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.execute(NEXT_FILE_ID);
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                fileId = rs.getLong(NEXTVAL);
            }
            PreparedStatement pstmt = conn.prepareStatement(INSERT_FILE);
            pstmt.setLong(1, fileId);
            pstmt.setLong(2, mimeTypeId);
            pstmt.setLong(3, sanId);
            pstmt.setLong(4, WORKFLOW_ATTENTE_VALIDATION);
            int nbrow = pstmt.executeUpdate();
            if (nbrow == 0) {
                throw new SQLException();
            }
            conn.commit();
            closeRessources(conn, pstmt);
        } catch (SQLException e) {
            log.error("Can't FileDAOImpl.create " + e.getMessage());
            conn.rollback();
            throw e;
        }
        return fileId;
    }

    /**
	 * return FileBean
	 */
    public FileBean create(MimeTypeBean mimeType, SanBean san) throws SQLException {
        long fileId = 0;
        DataSource ds = getDataSource(DEFAULT_DATASOURCE);
        Connection conn = ds.getConnection();
        try {
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            stmt.execute(NEXT_FILE_ID);
            ResultSet rs = stmt.getResultSet();
            while (rs.next()) {
                fileId = rs.getLong(NEXTVAL);
            }
            PreparedStatement pstmt = conn.prepareStatement(INSERT_FILE);
            pstmt.setLong(1, fileId);
            pstmt.setLong(2, mimeType.getId());
            pstmt.setLong(3, san.getId());
            pstmt.setLong(4, WORKFLOW_ATTENTE_VALIDATION);
            int nbrow = pstmt.executeUpdate();
            if (nbrow == 0) {
                throw new SQLException();
            }
            conn.commit();
            closeRessources(conn, pstmt);
        } catch (SQLException e) {
            log.error("Can't FileDAOImpl.create " + e.getMessage());
            conn.rollback();
            throw e;
        }
        FileBean fileBean = new FileBean();
        return fileBean;
    }

    /**
	 * 
	 */
    public int deleteFile(Integer[] fileID) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Connection conn = null;
        int nbrow = 0;
        try {
            DataSource ds = getDataSource(DEFAULT_DATASOURCE);
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            if (log.isDebugEnabled()) {
                log.debug("FileDAOImpl.deleteFile() " + DELETE_FILES_LOGIC);
            }
            for (int i = 0; i < fileID.length; i++) {
                pstmt = conn.prepareStatement(DELETE_FILES_LOGIC);
                pstmt.setInt(1, fileID[i].intValue());
                nbrow = pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            conn.rollback();
            log.error("FileDAOImpl.deleteFile() : erreur technique", e);
            throw e;
        } finally {
            conn.commit();
            closeRessources(conn, pstmt, rs);
        }
        return nbrow;
    }

    public List getListStatus(long[] fileID) throws SQLException {
        return null;
    }

    public FileBean load(long id) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Connection conn = null;
        FileBean fileBean = null;
        try {
            DataSource ds = getDataSource(DEFAULT_DATASOURCE);
            conn = ds.getConnection();
            if (log.isDebugEnabled()) {
                log.debug("FileDAOImpl.load() " + SELECT_FILE_BY_ID);
            }
            pstmt = conn.prepareStatement(SELECT_FILE_BY_ID);
            pstmt.setLong(1, id);
            pstmt.execute();
            rs = pstmt.getResultSet();
            while (rs.next()) {
                fileBean = new FileBean();
                fileBean.setAbsolutePath(rs.getString(ABSOLUTE_PATH));
                fileBean.setCrdate(rs.getTimestamp(CR_DATE));
                fileBean.setId(rs.getLong(FILE_ID));
                fileBean.setMimeTypeId(rs.getLong(MIMETYPE_ID));
                fileBean.setName(rs.getString(NAME));
                fileBean.setStatus(rs.getShort(STATUS));
                fileBean.setStorageId(rs.getLong(SAN_ID));
                fileBean.setUpdate(rs.getTimestamp(UP_DATE));
            }
        } catch (SQLException e) {
            log.error("FileDAOImpl.load() : erreur technique", e);
            throw e;
        } finally {
            closeRessources(conn, pstmt, rs);
        }
        return fileBean;
    }

    public void load(FileBean file) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Connection conn = null;
        FileBean fileBean;
        try {
            DataSource ds = getDataSource(DEFAULT_DATASOURCE);
            conn = ds.getConnection();
            if (log.isDebugEnabled()) {
                log.debug("FileDAOImpl.load() " + SELECT_FILE_BY_ID);
            }
            pstmt = conn.prepareStatement(SELECT_FILE_BY_ID);
            pstmt.setLong(1, file.getId());
            pstmt.execute();
            rs = pstmt.getResultSet();
            while (rs.next()) {
                fileBean = new FileBean();
                fileBean.setAbsolutePath(rs.getString(ABSOLUTE_PATH));
                fileBean.setCrdate(rs.getTimestamp(CR_DATE));
                fileBean.setId(rs.getLong(FILE_ID));
                fileBean.setMimeTypeId(rs.getLong(MIMETYPE_ID));
                fileBean.setName(rs.getString(NAME));
                fileBean.setStatus(rs.getShort(STATUS));
                fileBean.setStorageId(rs.getLong(SAN_ID));
                fileBean.setUpdate(rs.getTimestamp(UP_DATE));
            }
        } catch (SQLException e) {
            log.error("FileDAOImpl.load() : erreur technique", e);
            throw e;
        } finally {
            closeRessources(conn, pstmt, rs);
        }
    }

    public int update(FileBean bean) throws SQLException {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        Connection conn = null;
        int nbrow = 0;
        try {
            DataSource ds = getDataSource(DEFAULT_DATASOURCE);
            conn = ds.getConnection();
            if (log.isDebugEnabled()) {
                log.debug("FileDAOImpl.update() " + UPDATE_FILE);
            }
            pstmt = conn.prepareStatement(UPDATE_FILE);
            pstmt.setLong(1, bean.getMimeTypeId());
            pstmt.setLong(2, bean.getStorageId());
            pstmt.setString(3, bean.getAbsolutePath());
            pstmt.setString(4, bean.getName());
            pstmt.setLong(5, bean.getStatus());
            pstmt.setLong(6, bean.getId());
            nbrow = pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("FileDAOImpl.update() : erreur technique", e);
            throw e;
        } finally {
            closeRessources(conn, pstmt, rs);
        }
        return nbrow;
    }
}
