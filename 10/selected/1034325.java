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
import domain.core.Subject;

/** 
 * Data Object Access to the SUBJECT Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class SubjectDAO extends DAOProduct<Subject> {

    public static final String TABLE_NAME = "SUBJECT";

    @Override
    public void delete(Subject obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("SUBJECT_ID", obj.getId());
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
	 * Return the subjects related to the teaching unit whose id is in parameter
	 * @param teachingUnitID of the teaching unit 
	 * @return HashSet<Subject>
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public HashSet<Subject> findByTeachingUnit(int teachingUnitID) throws SelectException, DBConnectionException {
        HashSet<Subject> subjectSet = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to get statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("TEACHING_UNIT_ID", teachingUnitID);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                subjectSet = new HashSet<Subject>();
                while (result.next()) {
                    Subject subject = new Subject();
                    subject.setName(result.getString("SUBJECT_NAME"));
                    subject.setDescription(result.getString("SUBJECT_DESCRIPTION"));
                    subject.setId(result.getInt("SUBJECT_ID"));
                    subject.setCoeff(result.getFloat("SUBJECT_COEFFICIENT"));
                    subject.setAlias(result.getString("SUBJECT_ALIAS"));
                    subject.setTeachingUnit(null);
                    subjectSet.add(subject);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return subjectSet;
    }

    /**
	 * Return the subject object whose id is in parameter
	 * @param subjectID of the subject to find in Data source
	 * @return Subject
	 * @throws SelectException 
	 * @throws DBConnectionException 
	 */
    public Subject findBySubjectID(Integer subjectID) throws SelectException, DBConnectionException {
        Subject subject = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SUBJECT_ID", subjectID);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    subject = new Subject();
                    subject.setName(result.getString("SUBJECT_NAME"));
                    subject.setDescription(result.getString("SUBJECT_DESCRIPTION"));
                    subject.setId(result.getInt("SUBJECT_ID"));
                    subject.setCoeff(result.getFloat("SUBJECT_COEFFICIENT"));
                    subject.setAlias(result.getString("SUBJECT_ALIAS"));
                    subject.setTeachingUnit(null);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return subject;
    }

    /**
	 * Return the subject object whose Alias is in parameter
	 * @param subjectAlias of the object to find
	 * @return Subject
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public Subject findByAlias(Integer subjectAlias) throws SelectException, DBConnectionException {
        Subject subject = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SUBJECT_ALIAS", subjectAlias);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    subject = new Subject();
                    subject.setName(result.getString("SUBJECT_NAME"));
                    subject.setDescription(result.getString("SUBJECT_DESCRIPTION"));
                    subject.setId(result.getInt("SUBJECT_ID"));
                    subject.setCoeff(result.getFloat("SUBJECT_COEFFICIENT"));
                    subject.setAlias(result.getString("SUBJECT_ALIAS"));
                    subject.setTeachingUnit(null);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return subject;
    }

    /**
	 * Return the subject object whose id is referenced by the subject model
	 * (id in parameter)
	 * @param idSubjectModel of the subject model which reference the subject to find
	 * @return Subject
	 * @throws SelectException 
	 * @throws DBConnectionException 
	 */
    public Subject findBySubjectModelID(Integer idSubjectModel) throws SelectException, DBConnectionException {
        Subject subject = null;
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
        tablesFrom.add(new SQLWord(SubjectDAO.TABLE_NAME + " sub"));
        tablesFrom.add(new SQLWord(SubjectModelDAO.TABLE_NAME + " smod"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("smod.SUBJECT_MODEL_ID", idSubjectModel);
        critWhere.addCriterion("smod.SUBJECT_ID", new SQLWord("sub.SUBJECT_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    subject = new Subject();
                    subject = new Subject();
                    subject.setName(result.getString("SUBJECT_NAME"));
                    subject.setDescription(result.getString("SUBJECT_DESCRIPTION"));
                    subject.setId(result.getInt("SUBJECT_ID"));
                    subject.setCoeff(result.getFloat("SUBJECT_COEFFICIENT"));
                    subject.setAlias(result.getString("SUBJECT_ALIAS"));
                    subject.setTeachingUnit(null);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return subject;
    }

    @Override
    public Subject store(Subject obj) throws InsertException, DBConnectionException {
        if (obj.getTeachingUnit().getId() == null) {
            throw new InsertException("Missing Teaching Unit FK");
        } else {
            Statement stmt;
            try {
                stmt = OracleJDBConnector.getInstance().getStatement();
            } catch (XmlIOException e2) {
                e2.printStackTrace();
                throw new DBConnectionException("Unable to get statement", e2);
            }
            List<Object> values = new ArrayList<Object>();
            values.add(0);
            values.add(obj.getTeachingUnit().getId());
            values.add(obj.getDescription());
            values.add(obj.getName());
            values.add(obj.getDescription());
            values.add(obj.getCoeff());
            values.add(obj.getAlias());
            try {
                stmt.executeUpdate(new InsertQuery(TABLE_NAME, values).toString());
                Criteria critWhere = new Criteria();
                critWhere.addCriterion("TEACHING_UNIT_ID", obj.getTeachingUnit().getId());
                critWhere.addCriterion("SUBJECT_NAME", obj.getName());
                List<SQLWord> listSelect = new ArrayList<SQLWord>();
                listSelect.add(new SQLWord("SUBJECT_ID"));
                ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, listSelect, critWhere).toString());
                if (result != null) {
                    while (result.next()) obj.setId(result.getInt("SUBJECT_ID"));
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

    @Override
    public void update(Subject obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("TEACHING_UNIT_ID", obj.getTeachingUnit().getId());
        newCrit.addCriterion("SUBJECT_NAME", obj.getName());
        newCrit.addCriterion("SUBJECT_DESCRIPTION", obj.getDescription());
        newCrit.addCriterion("SUBJECT_COEFFICIENT", obj.getCoeff());
        newCrit.addCriterion("SUBJECT_ALIAS", obj.getAlias());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SUBJECT_ID", obj.getId());
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
