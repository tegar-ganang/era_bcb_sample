package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import tools.Log;
import business.Bill;
import business.BusinessFactory;
import business.BusinessObject;
import dao.DAOException;

/**
 * This class proposes CRUD operations for bills.
 * Its attribute is the connection to the database.
 *
 */
public class PostgreBillDAO implements BusinessObjectDAO {

    private Connection connection;

    /**
	 * Constructor
	 * @param con The connection to be used
	 */
    public PostgreBillDAO(Connection con) {
        this.connection = con;
    }

    /**
	 * Close the connection
	 */
    public void destroy() {
        try {
            this.connection.close();
        } catch (SQLException sqle) {
            Log.write(sqle.getMessage());
        } catch (NullPointerException npe) {
            Log.write(npe.getMessage());
        }
    }

    /**
	 * Creates a bill in database
	 * @param o a bill
	 * @return the id of the bill created
	 */
    public int create(BusinessObject o) throws DAOException {
        int insert = 0;
        int id = 0;
        Bill bill = (Bill) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("INSERT_BILL"));
            pst.setDate(1, new java.sql.Date(bill.getDate().getTime()));
            pst.setInt(2, bill.getIdAccount());
            insert = pst.executeUpdate();
            if (insert <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (insert > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("select max(id) from bill");
            rs.next();
            id = rs.getInt(1);
            connection.commit();
        } catch (SQLException e) {
            Log.write(e.getMessage());
            throw new DAOException("A SQLException has occured");
        } catch (NullPointerException npe) {
            Log.write(npe.getMessage());
            throw new DAOException("Connection null");
        }
        return id;
    }

    /**
	 * Sets the tag archived of the bill in param on true 
	 * @param o a project
	 * @return the number of rows updated
	 */
    public int delete(BusinessObject o) throws DAOException {
        int delete = 0;
        Bill bill = (Bill) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("DELETE_BILL"));
            pst.setInt(1, bill.getId());
            delete = pst.executeUpdate();
            if (delete <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (delete > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            connection.commit();
        } catch (SQLException e) {
            Log.write(e.getMessage());
            throw new DAOException("A SQLException has occured");
        } catch (NullPointerException npe) {
            Log.write(npe.getMessage());
            throw new DAOException("Connection null");
        }
        return delete;
    }

    /**
	 * Returns a list of bills which matches with the criteria in param
	 * @param criteria
	 * @return a list of bills
	 */
    public List retrieve(HashMap criteria) throws DAOException {
        List<Bill> result = new ArrayList<Bill>();
        try {
            String search = XMLGetQuery.getQuery("RETRIEVE") + " bill";
            if (criteria != null) {
                search += " where";
                Set s = criteria.entrySet();
                Iterator iter = s.iterator();
                while (iter.hasNext()) {
                    Map.Entry e = (Map.Entry) iter.next();
                    String column = (String) e.getKey();
                    String value = (String) e.getValue();
                    if (column.equals("archived")) search += " " + column + "=" + value + " and"; else if (column.equals("validated")) search += " " + column + "=" + value + " and"; else search += " " + column + " like " + "'%" + value + "%' and";
                }
                search = search.substring(0, search.length() - 3);
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(search);
            while (rs.next()) {
                Bill bill = BusinessFactory.createBill();
                bill.setId(rs.getInt("id"));
                bill.setDate(rs.getDate("date"));
                bill.setIdAccount(rs.getInt("idaccount"));
                bill.setArchived(rs.getBoolean("archived"));
                bill.setValidated(rs.getBoolean("validated"));
                result.add(bill);
            }
            connection.commit();
        } catch (java.sql.SQLException e) {
            Log.write(e.getMessage());
            throw new DAOException("A SQLException has occured");
        } catch (NullPointerException npe) {
            Log.write(npe.getMessage());
            throw new DAOException("Connection null");
        }
        return result;
    }

    /**
	 * Updates the bill in database with the bill in parameter (sets the tag validated on true)
	 * @param o a bill
	 * @return the number of rows updated
	 */
    public int update(BusinessObject o) throws DAOException {
        int update = 0;
        Bill bill = (Bill) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("UPDATE_BILL"));
            pst.setInt(1, bill.getId());
            update = pst.executeUpdate();
            if (update <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (update > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            connection.commit();
        } catch (SQLException e) {
            Log.write(e.getMessage());
            throw new DAOException("A SQLException has occured");
        } catch (NullPointerException npe) {
            Log.write(npe.getMessage());
            throw new DAOException("Connection null");
        }
        return update;
    }
}
