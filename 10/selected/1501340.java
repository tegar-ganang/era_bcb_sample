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

/** 
 * Data Object Acces to the GROUP_TYPE Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class GroupTypeDAO extends DAOProduct<GroupType> {

    public static final String TABLE_NAME = "GROUP_TYPE";

    /**
	 * this method delete the object in parameter from the DB
	 * @param obj the Group Type to delete 
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws DeleteException 
	 */
    @Override
    public void delete(GroupType obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("GROUP_TYPE_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(GroupTypeDAO.TABLE_NAME, critDel).toString());
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
	 * Return all the group type in the data source
	 * @return HashSet<GroupType> Group Type list
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public HashSet<GroupType> findAllGroupType() throws DBConnectionException, SelectException {
        HashSet<GroupType> grpTypeList = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(GroupTypeDAO.TABLE_NAME).toString());
            if (result != null) {
                grpTypeList = new HashSet<GroupType>();
                while (result.next()) {
                    GroupType grp = new GroupType();
                    grp.setId(result.getInt("GROUP_TYPE_ID"));
                    grp.setLabel(result.getString("GROUP_TYPE_LABEL"));
                    grpTypeList.add(grp);
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return grpTypeList;
    }

    /**
	 * Return the Group Type  with the Label in parameter
	 * @param groupTypeLabel of the group type to find
	 * @return GroupType (or null if not exists in data source)
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public GroupType findByLabel(String groupTypeLabel) throws DBConnectionException, SelectException {
        GroupType grpType = null;
        Statement stmt = null;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to Get Statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("GROUP_TYPE_LABEL", groupTypeLabel);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                grpType = new GroupType();
                while (result.next()) {
                    grpType.setId(result.getInt("GROUP_TYPE_ID"));
                    grpType.setLabel(result.getString("GROUP_TYPE_LABEL"));
                }
            } else {
                throw new SelectException(TABLE_NAME + " Can't retieve record");
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return grpType;
    }

    /**
	 * Return the Group Type of the students group designed by the ID in parameter 
	 * @param idStudentsGroup of the students group corresponding to the group type to find
	 * @return GroupType
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public GroupType findByStudentsGroup(Integer idStudentsGroup) throws DBConnectionException, SelectException {
        GroupType grpType = null;
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
        tablesFrom.add(new SQLWord(GroupTypeDAO.TABLE_NAME + " grpt"));
        tablesFrom.add(new SQLWord(StudentsGroupDAO.TABLE_NAME + " stugrp"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("stugrp.STUDENT_GROUP_ID", idStudentsGroup);
        critWhere.addCriterion("grpt.GROUP_TYPE_ID", new SQLWord("stugrp.GROUP_TYPE_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    grpType = new GroupType();
                    grpType.setId(result.getInt("GROUP_TYPE_ID"));
                    grpType.setLabel(result.getString("GROUP_TYPE_LABEL"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return grpType;
    }

    /**
	 * Return the Group Type of the subject model designed by the ID in parameter 
	 * @param idSubjectModel of the subject model pointing the group type to find
	 * @return GroupType
	 * @throws DBConnectionException
	 * @throws SelectException
	 */
    public GroupType findBySubjectModel(Integer idSubjectModel) throws DBConnectionException, SelectException {
        GroupType grpType = null;
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
        tablesFrom.add(new SQLWord(GroupTypeDAO.TABLE_NAME + " grpt"));
        tablesFrom.add(new SQLWord(SubjectModelDAO.TABLE_NAME + " subj"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("subj.SUBJECT_MODEL_ID", idSubjectModel);
        critWhere.addCriterion("grpt.GROUP_TYPE_ID", new SQLWord("subj.GROUP_TYPE_ID"));
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    grpType = new GroupType();
                    grpType.setId(result.getInt("GROUP_TYPE_ID"));
                    grpType.setLabel(result.getString("GROUP_TYPE_LABEL"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return grpType;
    }

    /**
	 * this method store the object in parameter from the DB
	 * @param obj the ExtraordinaryClassSession to delete 
	 * @return GroupType the group type stored with its new ID
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws InsertException 
	 */
    @Override
    public GroupType store(GroupType obj) throws InsertException, DBConnectionException, XmlIOException {
        GroupType toReturn = null;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getLabel());
        try {
            stmt.executeUpdate(new InsertQuery(TABLE_NAME, values).toString());
            toReturn = findByLabel(obj.getLabel());
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
	 * this method update the object in parameter in the DB
	 * @param obj the GroupType object to update 
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws UpdateException 
	 */
    @Override
    public void update(GroupType obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("GROUP_TYPE_LABEL", obj.getLabel());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("GROUP_TYPE_ID", obj.getId());
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
