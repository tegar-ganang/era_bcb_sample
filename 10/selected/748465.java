package persistence.DAO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.GregorianCalendar;
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
import domain.core.ClassSession;
import domain.core.ElementaryEducationSession;
import domain.core.Period;

/** 
 * Data Object Acces to the CLASS_SESSION Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class ClassSessionDAO extends DAOProduct<ClassSession> {

    public static final String TABLE_NAME = "CLASS_SESSION";

    /**
	 * this method delete the object in parameter from the DB
	 * @param obj the ClassSession object to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws DeleteException 
	 */
    @Override
    public void delete(ClassSession obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("CLASS_SESSION_ID", obj.getId());
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
	 * this method store the object in parameter from the DB
	 * @param obj the ClassSession object to store
	 * @return ClassSession the object stored with its new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws InsertException 
	 */
    @Override
    public ClassSession store(ClassSession obj) throws InsertException, DBConnectionException, XmlIOException {
        if (obj.getPeriod() == null || obj.getElemEdSession() == null) {
            throw new InsertException(TABLE_NAME + " Missing Fields (Period or EES");
        } else {
            Statement stmt = OracleJDBConnector.getInstance().getStatement();
            List<Object> values = new ArrayList<Object>();
            values.add(0);
            if (obj.getClassRoom() != null) values.add(obj.getClassRoom().getId()); else values.add(null);
            values.add(obj.getElemEdSession().getId());
            values.add(obj.getPeriod().getDate());
            values.add(obj.getPeriod().getPosition());
            try {
                stmt.executeUpdate(new InsertQuery(TABLE_NAME, values).toString());
                Criteria critWhere = new Criteria();
                critWhere.addCriterion("EES_ID", obj.getElemEdSession().getId());
                critWhere.addCriterion("PERIOD_DATE", obj.getPeriod().getDate());
                critWhere.addCriterion("PERIOD_POSITION", obj.getPeriod().getPosition());
                if (obj.getClassRoom() != null) critWhere.addCriterion("CLASSROOM_ID", obj.getClassRoom().getId());
                List<SQLWord> listSelect = new ArrayList<SQLWord>();
                listSelect.add(new SQLWord("CLASS_SESSION_ID"));
                ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, listSelect, critWhere).toString());
                if (result != null) {
                    while (result.next()) obj.setId(result.getInt("CLASS_SESSION_ID"));
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
        }
        return obj;
    }

    /**
	 * this method find a list of ClassSession from a EESID
	 * @param eesId
	 * @return HashSet<ClassSession> ClassSession list
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public HashSet<ClassSession> findByEesId(Integer eesId) throws DBConnectionException, SelectException {
        HashSet<ClassSession> classSessionList = null;
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
        tablesFrom.add(new SQLWord(ClassSessionDAO.TABLE_NAME + " cs"));
        tablesFrom.add(new SQLWord(EESDao.TABLE_NAME + " ees"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("cs.CLASS_SESSION_ID", new SQLWord("ees.CLASS_SESSION_ID"));
        critWhere.addCriterion("ees.EES_ID", eesId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            classSessionList = new HashSet<ClassSession>();
            if (result != null) {
                while (result.next()) {
                    ClassSession css = new ClassSession();
                    css.setId(result.getInt("STUDENT_GROUP_ID"));
                    ClassRoom room = new ClassRoom();
                    room.setId(result.getInt("CLASSROOM_ID"));
                    ElementaryEducationSession ees = new ElementaryEducationSession();
                    ees.setId(result.getInt("EES_ID"));
                    GregorianCalendar date = new GregorianCalendar();
                    date.setTime(result.getDate("PERIOD_DATE"));
                    Period period = new Period();
                    period.setDate(date);
                    period.setPosition(result.getInt("PERIOD_POSITION"));
                    css.setClassRoom(room);
                    css.setElemEdSession(ees);
                    css.setPeriod(period);
                    classSessionList.add(css);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return classSessionList;
    }

    /**
	 * this method update the object in parameter in the DB with his new values
 	 * @param obj the academic year object to update
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws UpdateException 
	 */
    @Override
    public void update(ClassSession obj) throws UpdateException, DBConnectionException, XmlIOException {
        if (obj.getElemEdSession().getId() == null || obj.getPeriod() == null) throw new UpdateException(TABLE_NAME + " Missing EES or Period Foreign Key");
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("EES_ID", obj.getElemEdSession().getId());
        newCrit.addCriterion("CLASSROOM_ID", obj.getClassRoom().getId());
        newCrit.addCriterion("PERIOD_DATE", obj.getPeriod().getDate());
        newCrit.addCriterion("PERIOD_POSITION", obj.getPeriod().getPosition());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("CLASS_SESSION_ID", obj.getId());
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
