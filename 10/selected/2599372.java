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
import domain.core.ClassRoom;

/** 
 * Data Object Acces to the CLASSROOM Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class ClassRoomDAO extends DAOProduct<ClassRoom> {

    public static final String TABLE_NAME = "CLASSROOM";

    /**
	 * this method delete the object in parameter from the DB
	 * @param obj the classroom object to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws DeleteException 
	 */
    @Override
    public void delete(ClassRoom obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("CLASSROOM_ID", obj.getId());
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
	 * this method store the object passed as argument in the DB
	 * @param obj the object to store
	 * @return ClassRoom the classroom stored with its new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws InsertException 
	 */
    @Override
    public ClassRoom store(ClassRoom obj) throws DBConnectionException, XmlIOException, InsertException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getName());
        values.add(obj.getCapacity());
        try {
            stmt.executeUpdate(new InsertQuery(TABLE_NAME, values).toString());
            Criteria critWhere = new Criteria();
            critWhere.addCriterion("CLASSROOM_NAME", obj.getName());
            List<SQLWord> listSelect = new ArrayList<SQLWord>();
            listSelect.add(new SQLWord("CLASSROOM_ID"));
            ResultSet resultID = stmt.executeQuery(new SelectQuery(TABLE_NAME, listSelect, critWhere).toString());
            if (resultID != null) {
                resultID.next();
                obj.setId(resultID.getInt("CLASSROOM_ID"));
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
	 * this method return all of classrooms present in the DB
	 * @return HashSet<ClassRoom> the list of all of classroom
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public HashSet<ClassRoom> findAllClassRoom() throws DBConnectionException, SelectException {
        HashSet<ClassRoom> classroomSet = null;
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
                classroomSet = new HashSet<ClassRoom>();
                while (result.next()) {
                    ClassRoom classroom = new ClassRoom();
                    classroom.setId(result.getInt("CLASSROOM_ID"));
                    classroom.setName(result.getString("CLASSROOM_NAME"));
                    classroom.setCapacity(result.getInt("CLASSROOM_CAPACITY"));
                    classroomSet.add(classroom);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return classroomSet;
    }

    /**
	 * Return the classroom with the name in parameter
	 * @param className of the classroom to find
	 * @return ClassRoom
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public ClassRoom findByName(String className) throws SelectException, DBConnectionException {
        ClassRoom classroom = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("CLASSROOM_NAME", className);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    classroom = new ClassRoom();
                    classroom.setId(result.getInt("CLASSROOM_ID"));
                    classroom.setName(result.getString("CLASSROOM_NAME"));
                    classroom.setCapacity(result.getInt("CLASSROOM_CAPACITY"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return classroom;
    }

    /**
	 * this method update the object passed as argument
	 * @param obj the classroom that will be updated
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws UpdateException 
	 */
    @Override
    public void update(ClassRoom obj) throws DBConnectionException, XmlIOException, UpdateException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("CLASSROOM_NAME", obj.getName());
        newCrit.addCriterion("CLASSROOM_CAPACITY", obj.getCapacity());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("CLASSROOM_ID", obj.getId());
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
