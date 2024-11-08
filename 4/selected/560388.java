package com.openbravo.data.loader;

import java.sql.*;
import java.util.*;
import com.openbravo.basic.BasicException;
import java.math.BigDecimal;

/**
 *
 * @author  adrianromero
 */
public class PreparedSentence extends JDBCSentence {

    private String m_sentence;

    protected SerializerWrite m_SerWrite = null;

    protected SerializerRead m_SerRead = null;

    private PreparedStatement m_Stmt;

    public PreparedSentence(Session s, String sentence, SerializerWrite serwrite, SerializerRead serread) {
        super(s);
        m_sentence = sentence;
        m_SerWrite = serwrite;
        m_SerRead = serread;
        m_Stmt = null;
    }

    public PreparedSentence(Session s, String sentence, SerializerWrite serwrite) {
        this(s, sentence, serwrite, null);
    }

    public PreparedSentence(Session s, String sentence) {
        this(s, sentence, null, null);
    }

    private static final class PreparedSentencePars implements DataWrite {

        private PreparedStatement m_ps;

        /** Creates a new instance of SQLParameter */
        PreparedSentencePars(PreparedStatement ps) {
            m_ps = ps;
        }

        public void setInt(int paramIndex, Integer iValue) throws BasicException {
            try {
                m_ps.setObject(paramIndex, iValue, Types.INTEGER);
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }

        public void setString(int paramIndex, String sValue) throws BasicException {
            try {
                m_ps.setString(paramIndex, sValue);
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }

        public void setDouble(int paramIndex, Double dValue) throws BasicException {
            try {
                m_ps.setObject(paramIndex, dValue, Types.DOUBLE);
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }

        public void setDecimal(int paramIndex, BigDecimal dValue) throws BasicException {
            try {
                m_ps.setObject(paramIndex, dValue, Types.DECIMAL);
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }

        public void setBoolean(int paramIndex, Boolean bValue) throws BasicException {
            try {
                if (bValue == null) {
                    m_ps.setObject(paramIndex, null);
                } else {
                    m_ps.setBoolean(paramIndex, bValue.booleanValue());
                }
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }

        public void setTimestamp(int paramIndex, java.util.Date dValue) throws BasicException {
            try {
                m_ps.setObject(paramIndex, dValue == null ? null : new Timestamp(dValue.getTime()), Types.TIMESTAMP);
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }

        public void setBytes(int paramIndex, byte[] value) throws BasicException {
            try {
                m_ps.setBytes(paramIndex, value);
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }

        public void setObject(int paramIndex, Object value) throws BasicException {
            try {
                m_ps.setObject(paramIndex, value);
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            }
        }
    }

    public DataResultSet openExec(Object params) throws BasicException {
        closeExec();
        try {
            m_Stmt = m_s.getConnection().prepareStatement(m_sentence);
            if (params != null) {
                m_SerWrite.writeValues(new PreparedSentencePars(m_Stmt), params);
            }
            if (m_Stmt.execute()) {
                return new JDBCDataResultSet(m_Stmt.getResultSet(), m_SerRead);
            } else {
                int iUC = m_Stmt.getUpdateCount();
                if (iUC < 0) {
                    return null;
                } else {
                    return new SentenceUpdateResultSet(iUC);
                }
            }
        } catch (SQLException eSQL) {
            throw new BasicException(eSQL);
        }
    }

    public final DataResultSet moreResults() throws BasicException {
        try {
            if (m_Stmt.getMoreResults()) {
                return new JDBCDataResultSet(m_Stmt.getResultSet(), m_SerRead);
            } else {
                int iUC = m_Stmt.getUpdateCount();
                if (iUC < 0) {
                    return null;
                } else {
                    return new SentenceUpdateResultSet(iUC);
                }
            }
        } catch (SQLException eSQL) {
            throw new BasicException(eSQL);
        }
    }

    public final void closeExec() throws BasicException {
        if (m_Stmt != null) {
            try {
                m_Stmt.close();
            } catch (SQLException eSQL) {
                throw new BasicException(eSQL);
            } finally {
                m_Stmt = null;
            }
        }
    }
}
