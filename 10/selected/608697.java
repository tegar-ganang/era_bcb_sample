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
import domain.core.GroupType;
import domain.core.StudentsGroup;
import domain.core.YearOfStudy;

/** 
 * Data Access Object to the STUDENT_GROUP table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class StudentsGroupDAO extends DAOProduct<StudentsGroup> {

    public static final String TABLE_NAME = "STUDENT_GROUP";

    /**
	 * this method delete the object passed in parameter from the DB
	 * @param obj the object to delete
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 */
    @Override
    public void delete(StudentsGroup obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("STUDENT_GROUP_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(StudentsGroupDAO.TABLE_NAME, critDel).toString());
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
	 * this method return the students group corresponding to the EESId passed in parameter
	 * @param eesId
	 * @return StudentGroup
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public StudentsGroup findByEesId(Integer eesId) throws DBConnectionException, SelectException {
        StudentsGroup stdGrp = null;
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
        tablesFrom.add(new SQLWord(StudentsGroupDAO.TABLE_NAME + " stg"));
        tablesFrom.add(new SQLWord(EESDao.TABLE_NAME + " ees"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("stg.STUDENT_GROUP_ID", new SQLWord("ees.STUDENT_GROUP_ID"));
        critWhere.addCriterion("ees.EES_ID", eesId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    stdGrp = new StudentsGroup();
                    stdGrp.setId(result.getInt("STUDENT_GROUP_ID"));
                    stdGrp.setGroupType(null);
                    stdGrp.setYearOfStudy(null);
                    stdGrp.setName(result.getString("STUDENT_GROUP_NAME"));
                    stdGrp.setNbStudents(result.getInt("STUDENT_GROUP_NB_STUDENT"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return stdGrp;
    }

    /**
	 * @param yosId the id of the year of study concerning by the request
	 * @return HashSet<StudentsGroup> List of student group
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public HashSet<StudentsGroup> findStudentGroups(Integer yosId) throws DBConnectionException, SelectException {
        HashSet<StudentsGroup> stdGrpList = new HashSet<StudentsGroup>();
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("YEAR_STUDY_ID", yosId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(StudentsGroupDAO.TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    StudentsGroup stdGrp = new StudentsGroup();
                    stdGrp.setId(result.getInt("STUDENT_GROUP_ID"));
                    GroupType groupType = new GroupType();
                    groupType.setId((result.getInt("GROUP_TYPE_ID")));
                    stdGrp.setGroupType(groupType);
                    YearOfStudy yos = new YearOfStudy();
                    yos.setId((result.getInt("YEAR_STUDY_ID")));
                    stdGrp.setYearOfStudy(yos);
                    stdGrp.setName(result.getString("STUDENT_GROUP_NAME"));
                    stdGrp.setNbStudents(result.getInt("STUDENT_GROUP_NB_STUDENT"));
                    stdGrpList.add(stdGrp);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return stdGrpList;
    }

    /**
	 * This method store the object passed in parameter in the DB
	 * @param obj the object to store
	 * @return StudentsGroup the object stored with his new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 */
    @Override
    public StudentsGroup store(StudentsGroup obj) throws InsertException, DBConnectionException, XmlIOException {
        if (obj.getGroupType().getId() == null || obj.getYearOfStudy().getId() == null) {
            throw new InsertException("Missing GroupType FK and YearOfStudy FK");
        } else {
            Statement stmt = OracleJDBConnector.getInstance().getStatement();
            List<Object> values = new ArrayList<Object>();
            values.add(0);
            values.add(obj.getGroupType().getId());
            values.add(obj.getYearOfStudy().getId());
            values.add(obj.getName());
            values.add(obj.getNbStudents());
            try {
                stmt.executeUpdate(new InsertQuery(StudentsGroupDAO.TABLE_NAME, values).toString());
                Criteria critWhere = new Criteria();
                critWhere.addCriterion("YEAR_STUDY_ID", obj.getName());
                critWhere.addCriterion("STUDENT_GROUP_NAME", obj.getName());
                List<SQLWord> listSelect = new ArrayList<SQLWord>();
                listSelect.add(new SQLWord("STUDENT_GROUP_ID"));
                ResultSet result = stmt.executeQuery(new SelectQuery(StudentsGroupDAO.TABLE_NAME, listSelect, critWhere).toString());
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
        }
        return obj;
    }

    @Override
    public void update(StudentsGroup obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("GROUP_TYPE_ID", obj.getGroupType().getId());
        newCrit.addCriterion("YEAR_STUDY_ID", obj.getYearOfStudy().getId());
        newCrit.addCriterion("STUDENT_GROUP_NAME", obj.getName());
        newCrit.addCriterion("STUDENT_GROUP_NB_STUDENT", obj.getNbStudents());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("STUDENT_GROUP_ID", obj.getId());
        try {
            stmt.executeUpdate(new UpdateQuery(StudentsGroupDAO.TABLE_NAME, newCrit, critWhere).toString());
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
