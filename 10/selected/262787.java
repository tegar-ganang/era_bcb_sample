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
import domain.core.HolidaysType;

/** 
 * Data Object Acces to the HOLIDAYS_TYPE Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class HolidaysTypeDAO extends DAOProduct<HolidaysType> {

    public static final String TABLE_NAME = "HOLIDAYS_TYPE";

    @Override
    public void delete(HolidaysType obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("HOLIDAYS_TYPE_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(TABLE_NAME, critDel).toString());
            stmt.getConnection().commit();
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

    @Override
    public HolidaysType store(HolidaysType obj) throws InsertException, DBConnectionException, XmlIOException {
        HolidaysType toReturn = null;
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        List<Object> values = new ArrayList<Object>();
        values.add(0);
        values.add(obj.getName());
        try {
            stmt.executeUpdate(new InsertQuery(TABLE_NAME, values).toString());
            toReturn = findByName(obj.getName());
            if (toReturn == null) {
                throw new SelectException(TABLE_NAME + " Can't retieve record");
            }
            stmt.getConnection().commit();
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
	 * Return the holidays type corresponding to the name given as argument
	 * @param name
	 * @return HolidaysType
	 * @throws SelectException
	 * @throws DBConnectionException
	 */
    public HolidaysType findByName(String name) throws SelectException, DBConnectionException {
        HolidaysType holiType = null;
        Statement stmt;
        try {
            stmt = OracleJDBConnector.getInstance().getStatement();
        } catch (XmlIOException e1) {
            e1.printStackTrace();
            throw new DBConnectionException("Unable to get statement", e1);
        }
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("HOLIDAYS_TYPE_NAME", name);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(TABLE_NAME, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    holiType = new HolidaysType();
                    holiType.setId(result.getInt("HOLIDAYS_TYPE_ID"));
                    holiType.setName(result.getString("HOLIDAYS_TYPE_NAME"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return holiType;
    }

    /**
	 * return the Holidays Type corresponding to id given as argument
	 * @return HolidaysType
	 * @param holidaysId 
	 * @throws SelectException 
	 * @throws DBConnectionException 
	 */
    public HolidaysType findById(Integer holidaysId) throws SelectException, DBConnectionException {
        HolidaysType holidays = null;
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
        tablesFrom.add(new SQLWord(HolidaysDAO.TABLE_NAME + " holy"));
        tablesFrom.add(new SQLWord(HolidaysTypeDAO.TABLE_NAME + " holyt"));
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("holy.HOLIDAYS_TYPE_ID", new SQLWord("holyt.HOLIDAYS_TYPE_ID"));
        critWhere.addCriterion("holy.HOLIDAYS_ID", holidaysId);
        try {
            ResultSet result = stmt.executeQuery(new SelectQuery(tablesFrom, selectAttr, critWhere).toString());
            if (result != null) {
                while (result.next()) {
                    holidays = new HolidaysType();
                    holidays.setId(result.getInt("HOLIDAYS_TYPE_ID"));
                    holidays.setName(result.getString("HOLIDAYS_TYPE_NAME"));
                }
            }
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SelectException(TABLE_NAME + " Request Error", e);
        }
        return holidays;
    }

    @Override
    public void update(HolidaysType obj) throws UpdateException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria newCrit = new Criteria();
        newCrit.addCriterion("HOLIDAYS_TYPE_NAME", obj.getName());
        Criteria critWhere = new Criteria();
        critWhere.addCriterion("HOLIDAYS_TYPE_ID", obj.getId());
        try {
            stmt.executeUpdate(new UpdateQuery(TABLE_NAME, newCrit, critWhere).toString());
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
