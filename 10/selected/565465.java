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
import domain.core.TeachingUnit;

/** 
 * Data Object Acces to the TEACHING UNIT Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class TeachingUnitDAO extends DAOProduct<TeachingUnit> {

    public static final String TABLE_NAME = "TEACHING_UNIT";

    @Override
    public void delete(TeachingUnit obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("TEACHING_UNIT_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(TeachingUnitDAO.TABLE_NAME, critDel).toString());
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
	 * Return the Set of Teaching Unit linked to the semesterID in parameter
	 * @param semesterID of the semester
	 * @return HashSet<TEACHING_UNIT>
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public HashSet<TeachingUnit> findBySemesterId(Integer semesterID) throws SelectException, DBConnectionException {
        HashSet<TeachingUnit> tuSet = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SEMESTER_ID", semesterID);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                tuSet = new HashSet<TeachingUnit>();
                while (result.next()) {
                    TeachingUnit tu = new TeachingUnit(result.getInt("TEACHING_UNIT_ID"), result.getString("TEACHING_UNIT_NAME"), result.getString("TEACHING_UNIT_DESCRIPTION"), result.getInt("TEACHING_UNIT_NB_ECTS"), result.getInt("TEACHING_UNIT_MIN_SCORE"));
                    tu.setSubjectList(null);
                    tu.setSemester(null);
                    tuSet.add(tu);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return tuSet;
    }

    /**
	 * Return the Teaching Unit linked to the semesterID with the name in parameter
	 * @param name of the teaching unit
	 * @param semesterID of the semester linked with
	 * @return Teaching Unit 
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public TeachingUnit findByName(String name, Integer semesterID) throws SelectException, DBConnectionException {
        TeachingUnit tu = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("TEACHING_UNIT_NAME", name);
        critWhere.addCriterion("SEMESTER_ID", semesterID);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TeachingUnitDAO.TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) tu = new TeachingUnit(result.getInt("TEACHING_UNIT_ID"), result.getString("TEACHING_UNIT_NAME"), result.getString("TEACHING_UNIT_DESCRIPTION"), result.getInt("TEACHING_UNIT_NB_ECTS"), result.getInt("TEACHING_UNIT_MIN_SCORE"));
                tu.setSemester(null);
                tu.setSubjectList(null);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return tu;
    }

    @Override
    public TeachingUnit store(TeachingUnit obj) throws InsertException, DBConnectionException, XmlIOException {
        if (obj.getSemester().getId() == null) throw new InsertException("Missing Semester Foreign Key");
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getSemester().getId());
        values.add(obj.getName());
        values.add(obj.getDescription());
        values.add(obj.getNbECTS());
        values.add(obj.getMinScore());
        try {
            stmt.executeUpdate(new InsertQuery(TeachingUnitDAO.TABLE_NAME, values).toString());
            Criteria critWhere = new Criteria();
            critWhere.addCriterion("SEMESTER_ID", obj.getSemester().getId());
            critWhere.addCriterion("TEACHING_UNIT_NAME", obj.getName());
            List<SQLWord> listSelect = new ArrayList<SQLWord>();
            listSelect.add(new SQLWord("TEACHING_UNIT_ID"));
            ResultSet result = stmt.executeQuery(new SelectQuery(TeachingUnitDAO.TABLE_NAME, listSelect, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    obj.setId(result.getInt(1));
                }
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

    @Override
    public void update(TeachingUnit obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("SEMESTER_ID", obj.getSemester().getId());
        newCrit.addCriterion("TEACHING_UNIT_NAME", obj.getName());
        newCrit.addCriterion("TEACHING_UNIT_DESCRIPTION", obj.getDescription());
        newCrit.addCriterion("TEACHING_UNIT_NB_ECTS", obj.getNbECTS());
        newCrit.addCriterion("TEACHING_UNIT_MIN_SCORE", obj.getMinScore());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("TEACHING_UNIT_ID", obj.getId());
        try {
            stmt.executeUpdate(new UpdateQuery(TeachingUnitDAO.TABLE_NAME, newCrit, critWhere).toString());
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
