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
import domain.core.SubjectModel;

/** 
 * Data Object Access to the SUBJECT_MODEL Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class SubjectModelDAO extends DAOProduct<SubjectModel> {

    public static final String TABLE_NAME = "SUBJECT_MODEL";

    @Override
    public void delete(SubjectModel obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("SUBJECT_MODEL_ID", obj.getId());
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
	 * Retrieve from the data source the SubjectModel linked to the EES
	 * identified by the idEED in parameter
	 * @param idEES 
	 * @return SubjectModel linked to the EES
	 * @throws DBConnectionException 
	 * @throws SelectException 
	 */
    public SubjectModel findByEES(Integer idEES) throws DBConnectionException, SelectException {
        SubjectModel subMod = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e2) {
            e2.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e2);
        }
        Criteria subCrit = new Criteria();
        subCrit.addCriterion("EES_ID", idEES);
        List<SQLWord> specificModelID = new ArrayList<SQLWord>();
        specificModelID.add(new SQLWord("SUBJECT_MODEL_ID"));
        Criteria byIdEES = new Criteria();
        byIdEES.addCriterion("SUBJECT_MODEL_ID", new SelectQuery(EESDao.TABLE_NAME, specificModelID, subCrit));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(SubjectModelDAO.TABLE_NAME, byIdEES).toString());
            if (result != null) {
                subMod = new SubjectModel(result.getInt("SUBJECT_MODEL_NB_HOURS"));
                subMod.setId(result.getInt("SUBJECT_MODEL_ID"));
                subMod.setSessionType(null);
                subMod.setSubject(null);
                subMod.setGroupType(null);
                subMod.setElemEdSession(null);
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                stmt.getConnection().rollback();
            } catch (SQLException e1) {
                throw new DBConnectionException("Rollback Exception :", e1);
            }
            throw new SelectException("Request Error", e);
        }
        return subMod;
    }

    /**
	 * Retrieve from the data source the SubjectModel linked to the Session Type and Subject
	 * whose IDs are in parameter
	 * @param sessionTypeID of the session type 
	 * @param subjectID of the linked subject
	 * @return SubjectModel
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public SubjectModel findBySubjectAndSessionType(Integer sessionTypeID, Integer subjectID) throws SelectException, DBConnectionException {
        SubjectModel subjectModel = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SESSION_TYPE_ID", sessionTypeID);
        critWhere.addCriterion("SUBJECT_ID", subjectID);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    subjectModel = new SubjectModel(result.getInt("SUBJECT_MODEL_NB_HOURS"));
                    subjectModel.setId(result.getInt("SUBJECT_MODEL_ID"));
                    subjectModel.setSessionType(null);
                    subjectModel.setSubject(null);
                    subjectModel.setGroupType(null);
                    subjectModel.setElemEdSession(null);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return subjectModel;
    }

    @Override
    public SubjectModel store(SubjectModel obj) throws InsertException, DBConnectionException, XmlIOException {
        if (obj.getSessionType().getId() == null || obj.getSubject().getId() == null || obj.getGroupType().getId() == null) {
            throw new InsertException(TABLE_NAME + " Missing Field");
        } else {
            Statement stmt = OracleJDBConnector.getInstance().getStatement();
            List<Object> values = new ArrayList<Object>();
            values.add(0);
            values.add(obj.getSessionType().getId());
            values.add(obj.getSubject().getId());
            values.add(obj.getGroupType().getId());
            values.add(obj.getNbHours());
            try {
                stmt.executeUpdate(new InsertQuery(SubjectModelDAO.TABLE_NAME, values).toString());
                Criteria critWhere = new Criteria();
                critWhere.addCriterion("SESSION_TYPE_ID", obj.getSessionType().getId());
                critWhere.addCriterion("SUBJECT_ID", obj.getSubject().getId());
                critWhere.addCriterion("GROUP_TYPE_ID", obj.getGroupType().getId());
                List<SQLWord> listSelect = new ArrayList<SQLWord>();
                listSelect.add(new SQLWord("SUBJECT_MODEL_ID"));
                ResultSet idSM = stmt.executeQuery(new SelectQuery(SubjectModelDAO.TABLE_NAME, listSelect, critWhere).toString());
                if (idSM != null) {
                    obj.setId(idSM.getInt("SUBJECT_MODEL_ID"));
                } else {
                    throw new SelectException("Can't retieve record");
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
    }

    @Override
    public void update(SubjectModel obj) throws UpdateException, DBConnectionException {
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e2) {
            e2.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e2);
        }
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("SUBJECT_MODEL_NB_HOURS", obj.getNbHours());
        newCrit.addCriterion("SUBJECT_ID", obj.getSubject().getId());
        newCrit.addCriterion("SESSION_TYPE_ID", obj.getSessionType().getId());
        newCrit.addCriterion("GROUP_TYPE_ID", obj.getGroupType().getId());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("SUBJECT_MODEL_ID", obj.getId());
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
