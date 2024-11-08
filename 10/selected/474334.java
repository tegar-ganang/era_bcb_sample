package persistence.DAO;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import persistence.core.DAOProduct;
import persistence.exception.DBConnectionException;
import persistence.exception.DeleteException;
import persistence.exception.InsertException;
import persistence.exception.UpdateException;
import persistence.exception.XmlIOException;
import persistence.tools.Criteria;
import persistence.tools.DeleteQuery;
import persistence.tools.InsertQuery;
import persistence.tools.OracleJDBConnector;
import domain.core.ExtraordinaryClassSession;

/** 
 * Data Object Acces to the EXTRAORDINARY_CLASSSESSION Table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class ExtraClassSessionDAO extends DAOProduct<ExtraordinaryClassSession> {

    public static final String TABLE_NAME = "EXTRAORDINARY_CLASSSESSION";

    /**
	 * this method delete the object in parameter from the DB
	 * @param obj the ExtraordinaryClassSession to delete 
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * @throws DeleteException 
	 */
    @Override
    public void delete(ExtraordinaryClassSession obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("EXTRAORDINARY_CLASSSESSION_ID", obj.getId());
        try {
            stmt.executeUpdate(new DeleteQuery(ExtraClassSessionDAO.TABLE_NAME, critDel).toString());
            stmt.getConnection().commit();
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

    public ExtraordinaryClassSession find(ExtraordinaryClassSession obj) {
        return null;
    }

    @Override
    public ExtraordinaryClassSession store(ExtraordinaryClassSession obj) throws DBConnectionException, XmlIOException, InsertException {
        if (obj.getRoom() == null || obj.getTeacher() == null || obj.getPeriod().getDate() == null || obj.getPeriod().getPosition() == null) {
            throw new InsertException("Missing Field");
        } else {
            Statement stmt = OracleJDBConnector.getInstance().getStatement();
            List<Object> values = new ArrayList<Object>();
            values.add(0);
            values.add(obj.getRoom().getId());
            values.add(obj.getTeacher().getId());
            values.add(obj.getPeriod().getDate());
            values.add(obj.getPeriod().getPosition());
            try {
                stmt.executeUpdate(new InsertQuery(ExtraClassSessionDAO.TABLE_NAME, values).toString());
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
        }
        return obj;
    }

    @Override
    public void update(ExtraordinaryClassSession obj) throws UpdateException {
    }
}
