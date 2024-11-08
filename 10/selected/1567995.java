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
import domain.core.AcademicYear;

/** 
 * Data Object Acces to the ACADEMIC_YEAR Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class AcademicYearDAO extends DAOProduct<AcademicYear> {

    public static final String TABLE_NAME = "ACADEMIC_YEAR";

    /**
	 * this method delete the object in parameter from the DB
	 * @param obj the academic year object to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws DeleteException 
	 */
    @Override
    public void delete(AcademicYear obj) throws DBConnectionException, XmlIOException, DeleteException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("ACADEMIC_YEAR_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(AcademicYearDAO.TABLE_NAME, critDel).toString());
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
	 * this method store the object in parameter into the DB
	 * @param obj the academic year object to store
	 * @return AcademicYear the object stored with its new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws InsertException 
	 */
    @Override
    public AcademicYear store(AcademicYear obj) throws InsertException, DBConnectionException, XmlIOException {
        AcademicYear toReturn = null;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getName());
        try {
            stmt.executeUpdate(new InsertQuery(AcademicYearDAO.TABLE_NAME, values).toString());
            toReturn = findByName(obj.getName());
            if (toReturn == null) {
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
        return toReturn;
    }

    /**
	 * return the AcademicYear corresponding to id given as argument
	 * @return AcademicYear
	 * @param holidaysId 
	 * @throws SelectException 
	 * @throws DBConnectionException 
	 */
    public AcademicYear findById(Integer holidaysId) throws SelectException, DBConnectionException {
        AcademicYear acaYear = null;
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
        tablesFrom.add(new SQLWord(AcademicYearDAO.TABLE_NAME + " ay"));
        tablesFrom.add(new SQLWord(HolidaysDAO.TABLE_NAME + " holy"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("holy.ACADEMIC_YEAR_ID", new SQLWord("ay.ACADEMIC_YEAR_ID"));
        critWhere.addCriterion("holy.HOLIDAYS_ID", holidaysId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    acaYear = new AcademicYear();
                    acaYear.setId(result.getInt("ACEDEMIC_YEAR_ID"));
                    acaYear.setName(result.getString("ACEDEMIC_YEAR_NAME"));
                    acaYear.setYos(null);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return acaYear;
    }

    /**
	 * Return the Academic Year with the name in parameter 
	 * (or null if don't exist in data source)
	 * @param name of the Academic Year to find
	 * @return AcademicYear
	 * @throws SelectException
	 * @throws DBConnectionException
	 * @throws XmlIOException 
	 */
    public AcademicYear findByName(String name) throws SelectException, DBConnectionException {
        AcademicYear acaY = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("ACADEMIC_YEAR_NAME", name);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    acaY = new AcademicYear();
                    acaY.setId(result.getInt("ACEDEMIC_YEAR_ID"));
                    acaY.setName(result.getString("ACEDEMIC_YEAR_NAME"));
                    acaY.setYos(null);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return acaY;
    }

    /**
	 * Return the current academic year (latest)
	 * @return AcademicYear
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public String findCurrent() throws DBConnectionException, SelectException {
        String acaY = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        String query = "SELECT MAX(ACADEMIC_YEAR_NAME) AS " + '"' + "CURRENT_ACADEMIC_YEAR_NAME" + '"' + " FROM ACADEMIC_YEAR";
        try {
            ResultSet result = stmt.executeQuery(query);
            if (result != null) {
                if (result.next()) {
                    acaY = result.getString("CURRENT_ACADEMIC_YEAR_NAME");
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return acaY;
    }

    /**
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 * @param semesterId the id of semester which we will return the academic year correspond 
	 * @return AcademicYear the academic year corresponding to the semester id passing in argument
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public AcademicYear findBySemester(Integer semesterId) throws DBConnectionException, SelectException {
        AcademicYear acaYear = null;
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
        tablesFrom.add(new SQLWord(AcademicYearDAO.TABLE_NAME + " acaye"));
        tablesFrom.add(new SQLWord(SemesterDAO.TABLE_NAME + " sem"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("acaye.ACADEMIC_YEAR_ID", new SQLWord("sem.ACADEMIC_YEAR_ID"));
        critWhere.addCriterion("sem.SEMESTER_ID", semesterId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    acaYear = new AcademicYear();
                    acaYear.setId(result.getInt("ACADEMIC_YEAR_ID"));
                    acaYear.setName(result.getString("ACADEMIC_YEAR_NAME"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return acaYear;
    }

    /**
	 * this method look for all Academic Year and put them into a HashSet
	 * @return HashSet<AcademicYear> list of all of academic year 
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public HashSet<AcademicYear> findAll() throws DBConnectionException, SelectException {
        HashSet<AcademicYear> acaYearList = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(AcademicYearDAO.TABLE_NAME).toString());
            if (result != null) {
                acaYearList = new HashSet<AcademicYear>();
                while (result.next()) {
                    AcademicYear acaYear = new AcademicYear();
                    acaYear.setId(result.getInt("ACADEMIC_YEAR_ID"));
                    acaYear.setName(result.getString("ACADEMIC_YEAR_NAME"));
                    acaYearList.add(acaYear);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return acaYearList;
    }

    /**
	 * this method update the object passed as parameter
	 * @param obj the object to update in data base
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws UpdateException 
	 */
    @Override
    public void update(AcademicYear obj) throws DBConnectionException, XmlIOException, UpdateException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("ACADEMIC_YEAR_NAME", obj.getName());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("ACADEMIC_YEAR_ID", obj.getId());
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
