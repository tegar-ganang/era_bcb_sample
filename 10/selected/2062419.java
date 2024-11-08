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
import persistence.tools.SelectQuery;
import persistence.tools.UpdateQuery;
import domain.core.Department;

/** 
 * Data Object Acces to the DEPARTMENT Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class DepartmentDAO extends DAOProduct<Department> {

    public static final String TABLE_NAME = "DEPARTMENT";

    /**
	 * this method delete the object in parameter from the DB
	 * @param obj the Department object to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws DeleteException 
	 */
    @Override
    public void delete(Department obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("DEPARTMENT_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(DepartmentDAO.TABLE_NAME, critDel).toString());
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
	 * @param obj the Department object to store
	 * @return Department the department stored with its new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws InsertException 
	 */
    @Override
    public Department store(Department obj) throws InsertException, DBConnectionException, XmlIOException {
        Department toReturn = null;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getName());
        values.add(obj.getDescription());
        values.add(obj.getAcronym());
        try {
            stmt.executeUpdate(new InsertQuery(DepartmentDAO.TABLE_NAME, values).toString());
            toReturn = findByAcronym(obj.getAcronym());
            if (toReturn != null) toReturn.setYear_of_study(obj.getYearOfStudyList()); else {
                throw new SelectException(TABLE_NAME + " Can't retieve record");
            }
            stmt.getConnection().commit();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                stmt.getConnection().rollback();
            } catch (SQLException e1) {
                throw new DBConnectionException("Rollback Exception :", e1);
            }
            throw new InsertException(TABLE_NAME + " Insert Exception :", e);
        }
        return toReturn;
    }

    /**
	 * this method update the object in parameter in the DB
	 * @param obj the department to update
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws UpdateException 
	 */
    @Override
    public void update(Department obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("DEPARTMENT_NAME", obj.getName());
        newCrit.addCriterion("DEPARTMENT_DESCRIPTION", obj.getDescription());
        newCrit.addCriterion("DEPARTMENT_ACRONYM", obj.getAcronym());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("DEPARTMENT_ID", obj.getId());
        try {
            stmt.executeUpdate(new UpdateQuery(DepartmentDAO.TABLE_NAME, newCrit, critWhere).toString());
            stmt.getConnection().commit();
            stmt.close();
        } catch (SQLException e) {
            try {
                stmt.getConnection().rollback();
            } catch (SQLException e1) {
                throw new DBConnectionException("Rollback Exception :", e1);
            }
            throw new UpdateException(TABLE_NAME + " Update exception", e);
        }
    }

    /**
	 * return the department with the acronym in parameter
	 * (or null if don't exist in data source)
	 * @param acronym of the department
	 * @return Department
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 * @throws XmlIOException 
	 */
    public Department findByAcronym(String acronym) throws DBConnectionException, SelectException {
        Department dpt = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("DEPARTMENT_ACRONYM", acronym);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(DepartmentDAO.TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    dpt = new Department(result.getString("DEPARTMENT_NAME"), result.getString("DEPARTMENT_DESCRIPTION"), result.getString("DEPARTMENT_ACRONYM"), null);
                    dpt.setId(result.getInt("DEPARTMENT_ID"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return dpt;
    }

    /**
	 * Return all the departments in data source
	 * (or null if don't exist in data source)
	 * @return HashSet<Department>
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public HashSet<Department> findAllDepartment() throws DBConnectionException, SelectException {
        HashSet<Department> dptList = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(DepartmentDAO.TABLE_NAME).toString());
            if (result != null) {
                dptList = new HashSet<Department>();
                while (result.next()) {
                    Department dpt = new Department(result.getString("DEPARTMENT_NAME"), result.getString("DEPARTMENT_DESCRIPTION"), result.getString("DEPARTMENT_ACRONYM"), null);
                    dpt.setId(result.getInt("DEPARTMENT_ID"));
                    dptList.add(dpt);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return dptList;
    }

    /**
	 * Return the department with the name in parameter 
	 * (or null if don't exist in data source)
	 * @param name of the department to find
	 * @return Department
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public Department findByName(String name) throws DBConnectionException, SelectException {
        Department dpt = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("DEPARTMENT_NAME", name);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(DepartmentDAO.TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    dpt = new Department(result.getString("DEPARTMENT_NAME"), result.getString("DEPARTMENT_DESCRIPTION"), result.getString("DEPARTMENT_ACRONYM"), null);
                    dpt.setId(result.getInt("DEPARTMENT_ID"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return dpt;
    }
}
