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
import domain.core.Status;

/** 
 * Data Object Acces to the STATUS Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class StatusDAO extends DAOProduct<Status> {

    public static final String TABLE_NAME = "STATUS";

    /**
	 * Method that delete the object in parameter from the DB
	 * @param obj the object to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 */
    @Override
    public void delete(Status obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("STATUS_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(TABLE_NAME, critDel).toString());
            stmt.getConnection().commit();
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
	 * This method find all of status
	 * @return HashSet<Status> list of all of status
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public HashSet<Status> findAllStatus() throws DBConnectionException, SelectException {
        HashSet<Status> statusSet = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME).toString());
            if (result != null) {
                statusSet = new HashSet<Status>();
                while (result.next()) {
                    Status status = new Status();
                    status.setId(result.getInt("STATUS_ID"));
                    status.setName(result.getString("STATUS_NAME"));
                    status.setNbHours(result.getInt("STATUS_NB_HOURS"));
                    statusSet.add(status);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return statusSet;
    }

    /**
	 * this method store the object in parameter in the DB
	 * @param obj the object to store
	 * @return Status the object stored with his new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 */
    @Override
    public Status store(Status obj) throws InsertException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getName());
        values.add(obj.getNbHours());
        try {
            stmt.executeUpdate(new InsertQuery(TABLE_NAME, values).toString());
            Criteria critWhere = new Criteria();
            critWhere.addCriterion("STATUS_NAME", obj.getName());
            List<SQLWord> listSelect = new ArrayList<SQLWord>();
            listSelect.add(new SQLWord("STATUS_ID"));
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, listSelect, critWhere).toString());
            if (result != null) {
                while (result.next()) obj.setId(result.getInt("STATUS_ID"));
            } else {
                throw new SelectException(TABLE_NAME + " Can't retieve record");
            }
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
        return obj;
    }

    /**
	 * 
	 * @param teacherId
	 * @return Status the status of the given teacher 
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public Status findTeacherStatus(Integer teacherId) throws DBConnectionException, SelectException {
        Status status = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        List<SQLWord> selectAttr = new ArrayList<SQLWord>();
        selectAttr.add(new SQLWord("*"));
        List<SQLWord> tablesFrom = new ArrayList<SQLWord>();
        tablesFrom.add(new SQLWord(StatusDAO.TABLE_NAME + " st"));
        tablesFrom.add(new SQLWord(TeacherDAO.TABLE_NAME + " te"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("st.STATUS_ID", new SQLWord("te.STATUS_ID"));
        critWhere.addCriterion("te.TEACHER_ID", teacherId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    status = new Status();
                    status.setId(result.getInt("STATUS_ID"));
                    status.setName(result.getString("STATUS_NAME"));
                    status.setNbHours(result.getInt("STATUS_NB_HOURS"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return status;
    }

    /**
	 * Return the status of the teacher identified with the id in parameter
	 * @param teacherID of the teacher
	 * @return Status
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public Status findByTeacherID(Integer teacherID) throws SelectException, DBConnectionException {
        Status status = null;
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
        tablesFrom.add(new SQLWord(TeacherDAO.TABLE_NAME + " tch"));
        tablesFrom.add(new SQLWord(StatusDAO.TABLE_NAME + " sts"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("tch.TEACHER_ID", teacherID);
        critWhere.addCriterion("tch.STATUS_ID", new SQLWord("sts.STATUS_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    status = new Status();
                    status.setId(result.getInt("STATUS_ID"));
                    status.setName(result.getString("STATUS_NAME"));
                    status.setNbHours(result.getInt("STATUS_NB_HOURS"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return status;
    }

    /**
	 * this method update the object in parameter with his new values in the DB
	 * @param obj the object to update in the DB
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 */
    @Override
    public void update(Status obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("STATUS_NAME", obj.getName());
        newCrit.addCriterion("STATUS_NB_HOURS", obj.getNbHours());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("STATUS_ID", obj.getId());
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
