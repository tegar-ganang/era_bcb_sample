package persistence.DAO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
import domain.core.ElementaryEducationSession;

/** 
 * Data Object Acces to the EES Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class EESDao extends DAOProduct<ElementaryEducationSession> {

    public static final String TABLE_NAME = "EES";

    /**
	 * this method delete the object in parameter from the DB
	 * @param obj the Elementary Education Service to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws DeleteException 
	 */
    @Override
    public void delete(ElementaryEducationSession obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("EES_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(EESDao.TABLE_NAME, critDel).toString());
            stmt.getConnection().commit();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                stmt.getConnection().rollback();
            } catch (SQLException e1) {
                throw new DBConnectionException("Rollback Exception :", e1);
            }
            throw new DeleteException(TABLE_NAME + " Deletion exception :", e);
        }
    }

    /**
	 * Return the subjects related to the teaching unit whose id is in parameter
	 * @param teacherID of the teaching unit 
	 * @return HashSet<Subject>
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public ArrayList<String[]> findByTeacher(int teacherID) throws SelectException, DBConnectionException {
        ArrayList<String[]> eesList = null;
        String ees[];
        Statement stmt = null;
        String query;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to get statement", e1);
        }
        query = "select department_acronym, year_study_name, semester_level, " + "subject_alias, session_type_acronym, student_group_name, " + "subject_model_nb_hours, subject_model_nb_hours * session_type_eqv_td " + "as " + '"' + "subject_model_td_nb_hours" + '"' + ", " + "(select count(*) from class_session c " + "where e.ees_id = c.ees_id) " + "* (year_study_duration_session / 60) as " + '"' + "ees_nb_performed_hours" + '"' + ", ees_statutory " + "from department d, year_of_study y, semester s, " + "subject su, session_type se, student_group st, " + "subject_model sm, ees e, teacher t, teaching_unit tu " + "where d.department_id = y.department_id " + "and y.year_study_id = s.year_study_id " + "and s.semester_id = tu.semester_id " + "and tu.teaching_unit_id = su.teaching_unit_id " + "and su.subject_id = sm.subject_id " + "and se.session_type_id = sm.session_type_id " + "and sm.subject_model_id = e.subject_model_id " + "and st.student_group_id = e.student_group_id " + "and t.teacher_id = e.teacher_id " + "and t.teacher_id = " + teacherID;
        try {
            ResultSet result = stmt.executeQuery(query);
            if (result != null) {
                eesList = new ArrayList<String[]>();
                while (result.next()) {
                    ees = new String[10];
                    ees[0] = result.getString("DEPARTMENT_ACRONYM");
                    ees[1] = result.getString("YEAR_STUDY_NAME");
                    ees[2] = result.getString("SEMESTER_LEVEL");
                    ees[3] = result.getString("SUBJECT_ALIAS");
                    ees[4] = result.getString("SESSION_TYPE_ACRONYM");
                    ees[5] = result.getString("STUDENT_GROUP_NAME");
                    ees[6] = result.getString("SUBJECT_MODEL_NB_HOURS");
                    ees[7] = result.getString("SUBJECT_MODEL_TD_NB_HOURS");
                    ees[8] = result.getString("EES_NB_PERFORMED_HOURS");
                    ees[9] = result.getString("EES_STATUTORY");
                    eesList.add(ees);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return eesList;
    }

    /**
	 * this method store the object in parameter into the DB
	 * @param obj the Elementary Education Service to store
	 * @return Elementary Education Session stored with its new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws InsertException 
	 */
    @Override
    public ElementaryEducationSession store(ElementaryEducationSession obj) throws InsertException, DBConnectionException, XmlIOException {
        if (obj.getTeacher() == null || obj.getSubjectModel() == null || obj.getStudentsGroup() == null) {
            throw new InsertException("Missing Field");
        } else {
            Statement stmt = OracleJDBConnector.getInstance().getStatement();
            List<Object> values = new ArrayList<Object>();
            values.add(0);
            values.add(obj.getTeacher().getId());
            values.add(obj.getSubjectModel().getId());
            values.add(obj.getStudentsGroup().getId());
            try {
                stmt.executeUpdate(new InsertQuery(EESDao.TABLE_NAME, values).toString());
                Criteria critWhere = new Criteria();
                critWhere.addCriterion("TEACHER_ID", obj.getTeacher().getId());
                critWhere.addCriterion("SUBJECT_MODEL_ID", obj.getSubjectModel().getId());
                critWhere.addCriterion("STUDENT_GROUP_ID", obj.getStudentsGroup().getId());
                List<SQLWord> listSelect = new ArrayList<SQLWord>();
                listSelect.add(new SQLWord("CLASS_SESSION_ID"));
                ResultSet result = stmt.executeQuery(new SelectQuery(EESDao.TABLE_NAME, listSelect, critWhere).toString());
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
                    throw new DBConnectionException(TABLE_NAME + " Rollback Exception :", e1);
                }
                throw new InsertException(TABLE_NAME + " Insert Exception :", e);
            }
        }
        return obj;
    }

    /**
	 * this method update the object in parameter from the DB
	 * @param obj the Elementary Education Service to update
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws UpdateException 
	 */
    @Override
    public void update(ElementaryEducationSession obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("TEACHER_ID", obj.getTeacher().getId());
        newCrit.addCriterion("SUBJECT_MODEL_ID", obj.getSubjectModel().getId());
        newCrit.addCriterion("STUDENT_GROUP_ID", obj.getStudentsGroup().getId());
        newCrit.addCriterion("EES_STATUTORY", obj.getStatutory());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("CLASS_SESSION_ID", obj.getId());
        try {
            stmt.executeUpdate(new UpdateQuery(ClassSessionDAO.TABLE_NAME, newCrit, critWhere).toString());
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
}
