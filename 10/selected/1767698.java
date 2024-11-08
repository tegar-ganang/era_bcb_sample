package persistence.DAO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
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
import domain.core.Status;
import domain.core.Teacher;

/** 
 * Data Object Acces to the SESSION_TYPE Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class TeacherDAO extends DAOProduct<Teacher> {

    public static final String TABLE_NAME = "TEACHER";

    /**
	 * Method that delete the teacher object in parameter from the DB
	 * @param obj the teacher to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 */
    @Override
    public void delete(Teacher obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("TEACHER_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(TeacherDAO.TABLE_NAME, critDel).toString());
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
	 * Method that find a teacher from his first name and last name
	 * @param firstName of the teacher to find
	 * @param lastName of the teacher ton find
	 * @return Teacher
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public Teacher findByName(String firstName, String lastName) throws DBConnectionException, SelectException {
        Teacher teacher = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("TEACHER_FIRST_NAME", firstName);
        critWhere.addCriterion("TEACHER_LAST_NAME", lastName);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    teacher = new Teacher();
                    GregorianCalendar dob = new GregorianCalendar();
                    dob.setTime(result.getDate("TEACHER_DOB"));
                    teacher.setId(result.getInt("TEACHER_ID"));
                    teacher.setStatus(null);
                    teacher.setFirstName(result.getString("TEACHER_FIRST_NAME"));
                    teacher.setLastName(result.getString("TEACHER_LAST_NAME"));
                    teacher.setDob(dob);
                    teacher.setCity(result.getString("TEACHER_CITY"));
                    teacher.setPostalCode(result.getInt("TEACHER_PC"));
                    teacher.setAddress1(result.getString("TEACHER_ADDRESS1"));
                    teacher.setAddress2(result.getString("TEACHER_ADDRESS2"));
                    teacher.setAlias(result.getString("TEACHER_ALIAS"));
                    teacher.setTitle(result.getString("TEACHER_TITLE"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return teacher;
    }

    /**
	 * Return all the teacher in data source
	 * @return HashSet<Teacher>
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public HashSet<Teacher> findAllTeacher() throws DBConnectionException, SelectException {
        HashSet<Teacher> teacherSet = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TeacherDAO.TABLE_NAME).toString());
            if (result != null) {
                teacherSet = new HashSet<Teacher>();
                while (result.next()) {
                    Teacher tea = new Teacher();
                    GregorianCalendar dob = new GregorianCalendar();
                    if (result.getDate("TEACHER_DOB") != null) dob.setTime(result.getDate("TEACHER_DOB"));
                    tea.setStatus(null);
                    tea.setId(result.getInt("TEACHER_ID"));
                    tea.setFirstName(result.getString("TEACHER_FIRST_NAME"));
                    tea.setLastName(result.getString("TEACHER_LAST_NAME"));
                    tea.setDob(dob);
                    tea.setCity(result.getString("TEACHER_CITY"));
                    tea.setPostalCode(result.getInt("TEACHER_PC"));
                    tea.setAddress1(result.getString("TEACHER_ADDRESS1"));
                    tea.setAddress2(result.getString("TEACHER_ADDRESS2"));
                    tea.setAlias(result.getString("TEACHER_ALIAS"));
                    tea.setTitle(result.getString("TEACHER_TITLE"));
                    teacherSet.add(tea);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return teacherSet;
    }

    /**
	 * Method that store the object in parameter
	 * @param obj the teacher to store
	 * @return the teacher stored with his new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 */
    @Override
    public Teacher store(Teacher obj) throws InsertException, DBConnectionException, XmlIOException {
        String day;
        String month;
        String year;
        GregorianCalendar date;
        String query;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        if (obj.getStatus() != null) {
            values.add(obj.getStatus().getId());
        } else {
            System.out.println("null");
            values.add("");
        }
        values.add(obj.getFirstName());
        values.add(obj.getLastName());
        if (obj.getDob() != null) {
            date = obj.getDob();
            day = date.get(Calendar.DAY_OF_MONTH) + "";
            month = (date.get(Calendar.MONTH) + 1) + "";
            year = date.get(Calendar.YEAR) + "";
            if (day.length() == 1) {
                day = "0" + day;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }
            if (year.length() < 4) {
                year = "0" + year;
                if (year.length() < 4) {
                    year = "0" + year;
                    if (year.length() < 4) {
                        year = "0" + year;
                    }
                }
            }
            values.add(day + "-" + month + "-" + year);
        }
        if (obj.getCity() != null) {
            values.add(obj.getCity());
        } else {
            values.add("");
        }
        if (obj.getPostalCode() != null) {
            values.add(obj.getPostalCode());
        } else {
            values.add(0);
        }
        if (obj.getAddress1() != null) {
            values.add(obj.getAddress1());
        } else {
            values.add("");
        }
        if (obj.getAddress2() != null) {
            values.add(obj.getAddress2());
        } else {
            values.add("");
        }
        if (obj.getTitle() != null) {
            values.add(obj.getTitle());
        } else {
            values.add("");
        }
        if (obj.getAlias() != null) {
            values.add(obj.getAlias());
        } else {
            values.add("");
        }
        System.out.println("before query");
        try {
            query = new InsertQuery(TeacherDAO.TABLE_NAME, values).toString();
            stmt.executeUpdate(query);
            Criteria critWhere = new Criteria();
            critWhere.addCriterion("TEACHER_FIRST_NAME", obj.getFirstName());
            critWhere.addCriterion("TEACHER_LAST_NAME", obj.getLastName());
            List<SQLWord> listSelect = new ArrayList<SQLWord>();
            listSelect.add(new SQLWord("TEACHER_ID"));
            ResultSet result = stmt.executeQuery(new SelectQuery(TeacherDAO.TABLE_NAME, listSelect, critWhere).toString());
            if (result != null) {
                while (result.next()) obj.setId(result.getInt(1));
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
                throw new DBConnectionException("Rollback Exception :", e1);
            }
            throw new InsertException(TABLE_NAME + " Insert Exception :", e);
        }
        return obj;
    }

    /**
	 * Return the Teacher involved in the EES identified with the id in parameter
	 * @return Teacher
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public Teacher findByEesId(Integer eesId) throws DBConnectionException, SelectException {
        Teacher teacher = null;
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
        tablesFrom.add(new SQLWord(TeacherDAO.TABLE_NAME + " tea"));
        tablesFrom.add(new SQLWord(EESDao.TABLE_NAME + " ees"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("tea.TEACHER_ID", new SQLWord("ees.TEACHER_ID"));
        critWhere.addCriterion("ees.EES_ID", eesId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    teacher = new Teacher();
                    Status status = new Status();
                    status.setId(result.getInt("STATUS_ID"));
                    GregorianCalendar dob = new GregorianCalendar();
                    dob.setTime(result.getDate("TEACHER_DOB"));
                    teacher.setId(result.getInt("TEACHER_ID"));
                    teacher.setStatus(status);
                    teacher.setFirstName(result.getString("TEACHER_FIRST_NAME"));
                    teacher.setLastName(result.getString("TEACHER_FIRST_NAME"));
                    teacher.setDob(dob);
                    teacher.setCity(result.getString("TEACHER_CITY"));
                    teacher.setPostalCode(result.getInt("TEACHER_PC"));
                    teacher.setAddress1(result.getString("TEACHER_ADDRESS1"));
                    teacher.setAddress2(result.getString("TEACHER_ADDRESS2"));
                    teacher.setAlias(result.getString("TEACHER_ALIAS"));
                    teacher.setTitle(result.getString("TEACHER_TITLE"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return teacher;
    }

    @Override
    public void update(Teacher obj) throws UpdateException, DBConnectionException, XmlIOException {
        String day;
        String month;
        String year;
        GregorianCalendar date;
        String query;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("TEACHER_ID", obj.getId());
        if (obj.getStatus() != null) {
            newCrit.addCriterion("STATUS_ID", obj.getStatus().getId());
        }
        newCrit.addCriterion("TEACHER_FIRST_NAME", obj.getFirstName());
        newCrit.addCriterion("TEACHER_LAST_NAME", obj.getLastName());
        if (obj.getDob() != null) {
            date = obj.getDob();
            day = date.get(Calendar.DAY_OF_MONTH) + "";
            month = (date.get(Calendar.MONTH) + 1) + "";
            year = date.get(Calendar.YEAR) + "";
            if (day.length() == 1) {
                day = "0" + day;
            }
            if (month.length() == 1) {
                month = "0" + month;
            }
            if (year.length() < 4) {
                year = "0" + year;
                if (year.length() < 4) {
                    year = "0" + year;
                    if (year.length() < 4) {
                        year = "0" + year;
                    }
                }
            }
            newCrit.addCriterion("TEACHER_DOB", day + "-" + month + "-" + year);
        }
        if (obj.getCity() != null) {
            newCrit.addCriterion("TEACHER_CITY", obj.getCity());
        }
        if (obj.getPostalCode() != null) {
            newCrit.addCriterion("TEACHER_PC", obj.getPostalCode());
        }
        if (obj.getAddress1() != null) {
            newCrit.addCriterion("TEACHER_ADDRESS1", obj.getAddress1());
        }
        if (obj.getAddress2() != null) {
            newCrit.addCriterion("TEACHER_ADDRESS2", obj.getAddress2());
        }
        if (obj.getTitle() != null) {
            newCrit.addCriterion("TEACHER_TITLE", obj.getTitle());
        }
        if (obj.getAlias() != null) {
            newCrit.addCriterion("TEACHER_ALIAS", obj.getAlias());
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("TEACHER_ID", obj.getId());
        query = new UpdateQuery(TeacherDAO.TABLE_NAME, newCrit, critWhere).toString();
        try {
            stmt.executeUpdate(query);
            stmt.getConnection().commit();
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
