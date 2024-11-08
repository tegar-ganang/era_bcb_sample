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
import domain.core.YearOfStudy;

/** 
 * Data Access Object to the YEAR_OF_STUDY table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class YearOfStudyDAO extends DAOProduct<YearOfStudy> {

    public static final String TABLE_NAME = "YEAR_OF_STUDY";

    /**
	 * Method that delete the year of study in parameter
	 * @param obj the year of study to delete
	 * @throws DBConnectionException
	 * @throws XmlIOException
	 */
    @Override
    public void delete(YearOfStudy obj) throws DeleteException, DBConnectionException, XmlIOException {
        String query;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("YEAR_STUDY_ID", obj.getId());
        query = new DeleteQuery(YearOfStudyDAO.TABLE_NAME, critDel).toString();
        try {
            stmt.executeUpdate(query);
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
	 * Search and return Year Of Study attached to the Department id 
	 * @param departmentId of the department
	 * @return HashSet of Year Of Study
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public HashSet<YearOfStudy> findByDepartment(Integer departmentId) throws DBConnectionException, SelectException {
        HashSet<YearOfStudy> yosSet = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("DEPARTMENT_ID", departmentId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(YearOfStudyDAO.TABLE_NAME, critWhere).toString());
            if (result != null) {
                yosSet = new HashSet<YearOfStudy>();
                while (result.next()) {
                    YearOfStudy yos = new YearOfStudy(result.getString("YEAR_STUDY_NAME"));
                    yos.setAcaYear(null);
                    yos.setDescription(result.getString("YEAR_STUDY_DESCRIPTION"));
                    yos.setDurationSession(result.getFloat("YEAR_STUDY_DURATION_SESSION"));
                    yos.setGroupList(null);
                    yos.setHolidays(null);
                    yos.setId(result.getInt("YEAR_STUDY_ID"));
                    yos.setNbSessionPM(result.getInt("YEAR_STUDY_NB_SESSIONPM"));
                    yos.setNbSessionsAM(result.getInt("YEAR_STUDY_NB_SESSIONAM"));
                    yosSet.add(yos);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return yosSet;
    }

    /**
	 * Return the Year Of Study with the Id in parameter
	 * @param id of the year of study
	 * @return Year of Study
	 * @throws SelectException
	 * @throws DBConnectionException
	 * @throws XmlIOException 
	 */
    public YearOfStudy findByID(Integer id) throws SelectException, DBConnectionException {
        YearOfStudy yos = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("YEAR_STUDY_ID", id);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(YearOfStudyDAO.TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    yos = new YearOfStudy(result.getString("YEAR_STUDY_NAME"));
                    yos.setAcaYear(null);
                    yos.setDescription(result.getString("YEAR_STUDY_DESCRIPTION"));
                    yos.setDurationSession(result.getFloat("YEAR_STUDY_DURATION_SESSION"));
                    yos.setGroupList(null);
                    yos.setHolidays(null);
                    yos.setId(result.getInt("YEAR_STUDY_ID"));
                    yos.setNbSessionPM(result.getInt("YEAR_STUDY_NB_SESSIONPM"));
                    yos.setNbSessionsAM(result.getInt("YEAR_STUDY_NB_SESSIONAM"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return yos;
    }

    /**
	 * Search and Return if present, the Year Of Study referenced by the StudentGroup
	 * with the studentGroup_id in parameter
	 * @param idStudentsGroup of the Student group which reference the year of study to find
	 * @return YearOfStudy
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public YearOfStudy findByStudentsGroup(Integer idStudentsGroup) throws DBConnectionException, SelectException {
        YearOfStudy yos = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        List<SQLWord> selectAttrSubReq = new ArrayList<SQLWord>();
        selectAttrSubReq.add(new SQLWord("YEAR_STUDY_ID"));
        Criteria critWhereSubReq = new Criteria();
        critWhereSubReq.addCriterion("STUDENT_GROUP_ID", idStudentsGroup);
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("YEAR_STUDY_ID", new SelectQuery(StudentsGroupDAO.TABLE_NAME, selectAttrSubReq, critWhereSubReq).toString());
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(YearOfStudyDAO.TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    yos = new YearOfStudy(result.getString("YEAR_STUDY_NAME"));
                    yos.setAcaYear(null);
                    yos.setDescription(result.getString("YEAR_STUDY_DESCRIPTION"));
                    yos.setDurationSession(result.getFloat("YEAR_STUDY_DURATION_SESSION"));
                    yos.setGroupList(null);
                    yos.setHolidays(null);
                    yos.setId(result.getInt("YEAR_STUDY_ID"));
                    yos.setNbSessionPM(result.getInt("YEAR_STUDY_NB_SESSIONPM"));
                    yos.setNbSessionsAM(result.getInt("YEAR_STUDY_NB_SESSIONAM"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return yos;
    }

    /**
	 * Search and Return if present, the Year Of Study with the name in parameter and
	 * attached to the Academic year with the name in parameter.
	 * @param yosName of the year of study to find
	 * @param academicYearName of the academic year related to this year of study
	 * @return YearOfStudy
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public YearOfStudy findByName(String yosName, String academicYearName) throws DBConnectionException, SelectException {
        YearOfStudy yos = null;
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
        tablesFrom.add(new SQLWord(YearOfStudyDAO.TABLE_NAME + " yos"));
        tablesFrom.add(new SQLWord(HolidaysDAO.TABLE_NAME + " h"));
        tablesFrom.add(new SQLWord(AcademicYearDAO.TABLE_NAME + " ay"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("yos.YEAR_STUDY_NAME", yosName);
        critWhere.addCriterion("ay.ACADEMIC_YEAR_NAME", academicYearName);
        critWhere.addCriterion("h.ACADEMIC_YEAR_ID", new SQLWord("ay.ACADEMIC_YEAR_ID"));
        critWhere.addCriterion("h.YEAR_STUDY_ID", new SQLWord("yos.YEAR_STUDY_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    yos = new YearOfStudy(result.getString("YEAR_STUDY_NAME"));
                    yos.setAcaYear(null);
                    yos.setDescription(result.getString("YEAR_STUDY_DESCRIPTION"));
                    yos.setDurationSession(result.getFloat("YEAR_STUDY_DURATION_SESSION"));
                    yos.setGroupList(null);
                    yos.setHolidays(null);
                    yos.setId(result.getInt("YEAR_STUDY_ID"));
                    yos.setNbSessionPM(result.getInt("YEAR_STUDY_NB_SESSIONPM"));
                    yos.setNbSessionsAM(result.getInt("YEAR_STUDY_NB_SESSIONAM"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return yos;
    }

    /**
	 * Search and Return if present, a HashSet of Year Of Study
	 * attached to the Academic year with the id in parameter.
	 * @param academicYearID of the year of the year of study to find
	 * @return HashSet<YearOfStudy>
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public HashSet<YearOfStudy> findByAcademicYearId(Integer academicYearID) throws SelectException, DBConnectionException {
        HashSet<YearOfStudy> yosSet = null;
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
        tablesFrom.add(new SQLWord(YearOfStudyDAO.TABLE_NAME + " yos"));
        tablesFrom.add(new SQLWord(HolidaysDAO.TABLE_NAME + " h"));
        tablesFrom.add(new SQLWord(AcademicYearDAO.TABLE_NAME + " ay"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("ay.ACADEMIC_YEAR_ID", academicYearID);
        critWhere.addCriterion("h.ACADEMIC_YEAR_ID", new SQLWord("ay.ACADEMIC_YEAR_ID"));
        critWhere.addCriterion("h.YEAR_STUDY_ID", new SQLWord("yos.YEAR_STUDY_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                yosSet = new HashSet<YearOfStudy>();
                while (result.next()) {
                    YearOfStudy yos = new YearOfStudy();
                    yos = new YearOfStudy(result.getString("YEAR_STUDY_NAME"));
                    yos.setAcaYear(null);
                    yos.setDescription(result.getString("YEAR_STUDY_DESCRIPTION"));
                    yos.setDurationSession(result.getFloat("YEAR_STUDY_DURATION_SESSION"));
                    yos.setGroupList(null);
                    yos.setHolidays(null);
                    yos.setId(result.getInt("YEAR_STUDY_ID"));
                    yos.setNbSessionPM(result.getInt("YEAR_STUDY_NB_SESSIONPM"));
                    yos.setNbSessionsAM(result.getInt("YEAR_STUDY_NB_SESSIONAM"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return yosSet;
    }

    /**
	 * Search and Return if present, the Year Of Study with the name in parameter and
	 * attached to the Department the name in parameter.
	 * @param yosName of the year of study to find
	 * @param departmentName of the Department related to this year of study
	 * @return YearOfStudy to find or null if not existing
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public YearOfStudy findByNameDept(String yosName, String departmentName) throws DBConnectionException, SelectException {
        YearOfStudy yos = null;
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
        tablesFrom.add(new SQLWord(YearOfStudyDAO.TABLE_NAME + " yos"));
        tablesFrom.add(new SQLWord(DepartmentDAO.TABLE_NAME + " dep"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("yos.YEAR_STUDY_NAME", yosName);
        critWhere.addCriterion("dep.DEPARTMENT_NAME", yosName);
        critWhere.addCriterion("dep.DEPARTMENT_ID", new SQLWord("yos.DEPARTMENT_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    yos = new YearOfStudy(result.getString("YEAR_STUDY_NAME"));
                    yos.setAcaYear(null);
                    yos.setDescription(result.getString("YEAR_STUDY_DESCRIPTION"));
                    yos.setDurationSession(result.getFloat("YEAR_STUDY_DURATION_SESSION"));
                    yos.setGroupList(null);
                    yos.setHolidays(null);
                    yos.setId(result.getInt("YEAR_STUDY_ID"));
                    yos.setNbSessionPM(result.getInt("YEAR_STUDY_NB_SESSIONPM"));
                    yos.setNbSessionsAM(result.getInt("YEAR_STUDY_NB_SESSIONAM"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return yos;
    }

    /**
	 * Method that store the object passing in parameter in the DB
	 * @param obj the year of study to store
	 * @return YearOfStudy the year of study stored with the new ID
	 * @throws DBConnectionException
	 * @throws XmlIOException
	 */
    @Override
    public YearOfStudy store(YearOfStudy obj) throws InsertException, DBConnectionException, XmlIOException {
        String query;
        if (obj.getDepartment().getId() == null) {
            throw new InsertException("Missing Department FK");
        } else {
            Statement stmt = OracleJDBConnector.getInstance().getStatement();
            List<Object> values = new ArrayList<Object>();
            values.add(0);
            values.add(obj.getName());
            values.add(obj.getDescription());
            values.add(obj.getDurationSession());
            values.add(obj.getNbSessionsAM());
            values.add(obj.getNbSessionPM());
            values.add(obj.getDepartment().getId());
            query = new InsertQuery(TABLE_NAME, values).toString();
            try {
                stmt.executeUpdate(query);
                Criteria critWhere = new Criteria();
                critWhere.addCriterion("DEPARTMENT_ID", obj.getDepartment().getId());
                critWhere.addCriterion("YEAR_STUDY_NAME", obj.getName());
                List<SQLWord> listSelect = new ArrayList<SQLWord>();
                listSelect.add(new SQLWord("YEAR_STUDY_ID"));
                ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, listSelect, critWhere).toString());
                if (result != null) {
                    while (result.next()) obj.setId(result.getInt(1));
                } else {
                    throw new SelectException(TABLE_NAME + " Can't retrieve record");
                }
                stmt.getConnection().commit();
                stmt.close();
            } catch (SQLException e) {
                System.out.println(TABLE_NAME + " Store problem");
                e.printStackTrace();
                try {
                    stmt.getConnection().rollback();
                } catch (SQLException e1) {
                    throw new DBConnectionException("Rollback Exception :", e1);
                }
                throw new InsertException(TABLE_NAME + " Insert Exception :", e);
            }
        }
        return obj;
    }

    /**
	 * Method that update the object in parameter with his new values in the DB
	 * @param obj the year of study to update
	 * @throws DBConnectionException
	 * @throws XmlIOException
	 */
    @Override
    public void update(YearOfStudy obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("YEAR_STUDY_NAME", obj.getName());
        newCrit.addCriterion("YEAR_STUDY_DESCRIPTION", obj.getDescription());
        newCrit.addCriterion("YEAR_STUDY_DURATION_SESSION", obj.getDurationSession());
        newCrit.addCriterion("YEAR_STUDY_NB_SESSIONAM", obj.getNbSessionsAM());
        newCrit.addCriterion("YEAR_STUDY_NB_SESSIONPM", obj.getNbSessionPM());
        newCrit.addCriterion("YEAR_STUDY_DEPARTMENT_ID", obj.getDepartment().getId());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("YEAR_STUDY_ID", obj.getId());
        try {
            stmt.executeUpdate(new UpdateQuery(YearOfStudyDAO.TABLE_NAME, newCrit, critWhere).toString());
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
