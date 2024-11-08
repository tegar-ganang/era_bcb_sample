package persistence.DAO;

import java.sql.SQLException;
import java.sql.Statement;
import persistence.core.DAOProduct;
import persistence.exception.DBConnectionException;
import persistence.exception.DeleteException;
import persistence.exception.InsertException;
import persistence.exception.UpdateException;
import persistence.exception.XmlIOException;
import persistence.tools.Criteria;
import persistence.tools.DeleteQuery;
import persistence.tools.OracleJDBConnector;
import domain.core.Period;

/** 
 * Data Access Object to the PERIOD table
 * @author Florent Revy for FARS Design
 * @author Zakaria Taghy for FARS Design
 */
public class PeriodDAO extends DAOProduct<Period> {

    public static final String TABLE_NAME = "PERIOD";

    /**
	 * @throws XmlIOException 
	 * @throws DBConnectionException 
	 * 
	 */
    @Override
    public void delete(Period obj) throws DeleteException, DBConnectionException, XmlIOException {
        Statement stmt = OracleJDBConnector.getInstance().getStatement();
        Criteria critDel = new Criteria();
        critDel.addCriterion("PERIOD_DATE", obj.getDate());
        critDel.addCriterion("PERIOD_POSITION", obj.getPosition());
        try {
            stmt.executeUpdate(new DeleteQuery(PeriodDAO.TABLE_NAME, critDel).toString());
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

    public Period find(Period obj) {
        return null;
    }

    /**
	 * 
	 */
    @Override
    public Period store(Period obj) throws InsertException {
        Period period = new Period();
        return period;
    }

    /**
	 * 
	 */
    @Override
    public void update(Period obj) throws UpdateException {
    }
}
