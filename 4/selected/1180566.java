package org.localstorm.mcc.ejb.gtd.dao;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.localstorm.mcc.ejb.dao.JdbcDaoHelper;
import org.localstorm.mcc.ejb.dao.Guard;

/**
 * @author Alexey Kuznetsov
 */
public class FileDao {

    private static final Logger log = Logger.getLogger(FileDao.class);

    private DataSource ds;

    public FileDao(DataSource ds) {
        Guard.checkDataSourceNotNull(ds);
        this.ds = ds;
    }

    public void deleteBody(Integer fileId) throws SQLException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            Guard.checkConnectionNotNull(conn);
            PreparedStatement ps = conn.prepareStatement("delete from FILE_BODIES where id=?");
            ps.setInt(1, fileId);
            ps.execute();
        } finally {
            JdbcDaoHelper.safeClose(conn, log);
        }
    }

    public int uploadFile(InputStream is, int fileId) throws IOException, SQLException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            Guard.checkConnectionNotNull(conn);
            PreparedStatement ps = conn.prepareStatement("insert into FILE_BODIES (id, file_id, data) " + "values (null, ?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
            ps.setInt(1, fileId);
            ps.setBlob(2, is);
            ps.execute();
            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) {
                throw new SQLException("Hmm.. No keys were generated!");
            }
            return rs.getInt(1);
        } finally {
            JdbcDaoHelper.safeClose(conn, log);
        }
    }

    public void downloadFile(OutputStream os, int fileId) throws IOException, SQLException {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            Guard.checkConnectionNotNull(conn);
            PreparedStatement ps = conn.prepareStatement("select * from FILE_BODIES where file_id=?");
            ps.setInt(1, fileId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new FileNotFoundException("File with id=" + fileId + " not found!");
            }
            Blob blob = rs.getBlob("data");
            InputStream is = blob.getBinaryStream();
            IOUtils.copyLarge(is, os);
        } finally {
            JdbcDaoHelper.safeClose(conn, log);
        }
    }
}
