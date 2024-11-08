package org.fpse.forum.test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.fpse.store.impl.SQLBasedStore;

/**
 * Created on Nov 5, 2009 4:23:35 PM by ajays
 */
public class TestingSQLStore extends SQLBasedStore {

    private final DatabaseHook m_hook;

    public TestingSQLStore() throws ClassNotFoundException {
        super();
        m_hook = new DatabaseHook();
    }

    @Override
    protected void ensureConnection() throws SQLException {
        super.ensureConnection();
        if (!m_hook.isCreated()) {
            try {
                m_hook.setUp(m_connection);
            } catch (final IOException e) {
                throw new SQLException(e);
            }
        }
    }

    @Override
    public void close() {
        if (m_hook.isCreated() && m_connection != null) {
            try {
                m_hook.tearDown(m_connection);
            } catch (final SQLException _) {
            } catch (final IOException _) {
            }
        }
        super.close();
    }

    @Override
    protected <T> T getGeneratedKeys(final PreparedStatement statement, final Connection connection, final Class<T> type) throws SQLException {
        final String query = "CALL IDENTITY();";
        PreparedStatement cs = null;
        ResultSet result = null;
        try {
            cs = connection.prepareStatement(query);
            result = cs.executeQuery();
            if (result.next()) {
                final Object o = result.getObject(1);
                return type.cast(o);
            } else {
                throw new SQLException("No keys generated.");
            }
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (final SQLException _) {
                }
            }
            if (cs != null) {
                try {
                    cs.close();
                } catch (final SQLException _) {
                }
            }
        }
    }

    @Override
    public void makeRead(final String user, final long databaseID, final long time) throws SQLException {
        final String query = "insert into fs.read_post (post, user, read_date) values (?, ?, ?)";
        ensureConnection();
        final PreparedStatement statement = m_connection.prepareStatement(query);
        try {
            statement.setLong(1, databaseID);
            statement.setString(2, user);
            statement.setTimestamp(3, new Timestamp(time));
            final int count = statement.executeUpdate();
            if (0 == count) {
                throw new SQLException("Nothing updated.");
            }
            m_connection.commit();
        } catch (final SQLException e) {
            m_connection.rollback();
            throw e;
        } finally {
            statement.close();
        }
    }
}
