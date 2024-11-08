package my.javaforum.jdbc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import my.javaforum.jdbc.exception.JdbcDaoException;

public abstract class JdbcDaoSupport<E, N extends Number> {

    protected N save(String sql, Object[] args) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            this.setParameters(pstmt, args);
            pstmt.executeUpdate();
            conn.commit();
            conn.setAutoCommit(true);
            rs = pstmt.getGeneratedKeys();
            return (N) rs.getObject(1);
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw new JdbcDaoException(e.getMessage(), e);
        } finally {
            JdbcUtils.free(rs, pstmt, conn);
        }
    }

    protected void update(String sql, Object[] args) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JdbcUtils.getConnection();
            conn.setAutoCommit(false);
            pstmt = conn.prepareStatement(sql);
            this.setParameters(pstmt, args);
            pstmt.executeUpdate();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            throw new JdbcDaoException(e.getMessage(), e);
        } finally {
            JdbcUtils.free(pstmt, conn);
        }
    }

    protected E query(String sql, Object[] args) {
        E entity = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            this.setParameters(pstmt, args);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                entity = mapping(rs);
            }
        } catch (SQLException e) {
            throw new JdbcDaoException(e.getMessage(), e);
        } finally {
            JdbcUtils.free(rs, pstmt, conn);
        }
        return entity;
    }

    protected List<E> list(String sql, Object[] args) {
        List<E> list = new ArrayList<E>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = JdbcUtils.getConnection();
            pstmt = conn.prepareStatement(sql);
            this.setParameters(pstmt, args);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                E entity = mapping(rs);
                list.add(entity);
            }
        } catch (SQLException e) {
            throw new JdbcDaoException(e.getMessage(), e);
        } finally {
            JdbcUtils.free(rs, pstmt, conn);
        }
        return list;
    }

    protected abstract E mapping(ResultSet rs) throws SQLException;

    private void setParameters(PreparedStatement pstmt, Object[] args) throws SQLException {
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                pstmt.setObject(i + 1, args[i]);
            }
        }
    }
}
