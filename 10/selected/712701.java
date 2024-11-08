package persistence.DAO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import persistence.core.DAOProduct;
import persistence.exception.DBConnectionException;
import persistence.exception.DeleteException;
import persistence.exception.InsertException;
import persistence.exception.SelectException;
import persistence.exception.UpdateException;
import persistence.exception.XmlIOException;
import persistence.tools.Criteria;
import persistence.tools.DeleteQuery;
import persistence.tools.InsertQuery;
import persistence.tools.OracleJDBConnector;
import persistence.tools.SQLWord;
import persistence.tools.SelectQuery;
import persistence.tools.UpdateQuery;
import domain.core.SessionType;

/** 
 * Data Object Acces to the SESSION_TYPE Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class SessionTypeDAO extends DAOProduct<SessionType> {

    public static final String TABLE_NAME = "SESSION_TYPE";

    @Override
    public void delete(SessionType obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("SESSION_TYPE_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(TABLE_NAME, critDel).toString());
            stmt.getConnection().commit();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                stmt.getConnection().rollback();
            } catch (SQLException e1) {
                throw new DBConnectionException(TABLE_NAME + " Rollback Exception :", e1);
            }
            throw new DeleteException(TABLE_NAME + " Deletion exception :", e);
        }
    }

    /**
	 * Return the Session type with the acronym in parameter
	 * @param acronym to find
	 * @return SessionType
	 * @throws DBConnectionException
	 * @throws SelectException 
	 */
    public SessionType findByAcronym(String acronym) throws DBConnectionException, SelectException {
        SessionType sessType = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SESSION_TYPE_ACRONYM", acronym);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    sessType = new SessionType();
                    sessType.setId(result.getInt("SESSION_TYPE_ID"));
                    sessType.setName(result.getString("SESSION_TYPE_NAME"));
                    sessType.setAcronym(result.getString("SESSION_TYPE_ACRONYM"));
                    sessType.setEquivTuto(result.getFloat("SESSION_TYPE_EQV_TD"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return sessType;
    }

    /**
	 * Return the Session type with the name in parameter
	 * @param name of the session type to find
	 * @return SessionType
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public SessionType findByName(String name) throws DBConnectionException, SelectException {
        SessionType sessType = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SESSION_TYPE_NAME", name);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    sessType = new SessionType();
                    sessType.setId(result.getInt("SESSION_TYPE_ID"));
                    sessType.setName(result.getString("SESSION_TYPE_NAME"));
                    sessType.setAcronym(result.getString("SESSION_TYPE_ACRONYM"));
                    sessType.setEquivTuto(result.getFloat("SESSION_TYPE_EQV_TD"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return sessType;
    }

    /**
	 * Return all the session type in data source
	 * @return HashSet<SessionType>
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public HashSet<SessionType> findAllSessionType() throws DBConnectionException, SelectException {
        HashSet<SessionType> sessTypeSet = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME).toString());
            if (result != null) {
                sessTypeSet = new HashSet<SessionType>();
                while (result.next()) {
                    SessionType sessType = new SessionType();
                    sessType.setId(result.getInt("SESSION_TYPE_ID"));
                    sessType.setName(result.getString("SESSION_TYPE_NAME"));
                    sessType.setAcronym(result.getString("SESSION_TYPE_ACRONYM"));
                    sessType.setEquivTuto(result.getFloat("SESSION_TYPE_EQV_TD"));
                    sessTypeSet.add(sessType);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return sessTypeSet;
    }

    /**
	 * Return the Session type concerned by the subject model identified by the id in parameter
	 * @param idSubjectModel of the subject model that is linked to the session type to find
	 * @return SessionType
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public SessionType findBySubjectModel(Integer idSubjectModel) throws SelectException, DBConnectionException {
        SessionType sessType = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        List<SQLWord> selectAttr = new ArrayList<SQLWord>();
        selectAttr.add(new SQLWord("*"));
        List<SQLWord> tablesFrom = new ArrayList<SQLWord>();
        tablesFrom.add(new SQLWord(SessionTypeDAO.TABLE_NAME + " sess"));
        tablesFrom.add(new SQLWord(SubjectModelDAO.TABLE_NAME + " subj"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("subj.SUBJECT_MODEL_ID", idSubjectModel);
        critWhere.addCriterion("sess.SESSION_TYPE_ID", new SQLWord("subj.SESSION_TYPE_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    sessType = new SessionType();
                    sessType.setId(result.getInt("SESSION_TYPE_ID"));
                    sessType.setName(result.getString("SESSION_TYPE_NAME"));
                    sessType.setAcronym(result.getString("SESSION_TYPE_ACRONYM"));
                    sessType.setEquivTuto(result.getFloat("SESSION_TYPE_EQV_TD"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return sessType;
    }

    @Override
    public SessionType store(SessionType obj) throws InsertException, DBConnectionException, XmlIOException {
        SessionType toReturn = null;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getName());
        values.add(obj.getEquivTuto());
        values.add(obj.getAcronym());
        try {
            stmt.executeUpdate(new InsertQuery(TABLE_NAME, values).toString());
            toReturn = findByAcronym(obj.getAcronym());
            stmt.getConnection().commit();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                stmt.getConnection().rollback();
            } catch (SQLException e1) {
                throw new DBConnectionException(TABLE_NAME + " Rollback Exception :", e1);
            }
            throw new InsertException(TABLE_NAME + " Insert Exception :", e);
        }
        return toReturn;
    }

    @Override
    public void update(SessionType obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("SESSION_TYPE_NAME", obj.getName());
        newCrit.addCriterion("SESSION_TYPE_EQV_TD", obj.getEquivTuto());
        newCrit.addCriterion("SESSION_TYPE_ACRONYM", obj.getAcronym());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SESSION_TYPE_ID", obj.getId());
        try {
            stmt.executeUpdate(new UpdateQuery(TABLE_NAME, newCrit, critWhere).toString());
            stmt.getConnection().commit();
            stmt.close();
        } catch (SQLException e) {
            try {
                stmt.getConnection().rollback();
            } catch (SQLException e1) {
                throw new DBConnectionException(TABLE_NAME + " Rollback Exception :", e1);
            }
            throw new UpdateException(TABLE_NAME + " Update exception", e);
        }
    }
}
