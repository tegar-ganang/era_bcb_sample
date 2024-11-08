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
import business.BusinessFactory;
import business.BusinessObject;
import business.Currency;
import dao.DAOException;

/**
 * This class proposes CRUD operations for currencies.
 * Its attribute is the connection to the database.
 *
 */
public class PostgreCurrencyDAO implements BusinessObjectDAO {

    private Connection connection;

    /**
	 * Constructor
	 * @param con The connection to be used
	 */
    public PostgreCurrencyDAO(Connection con) {
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
	 * Creates a currency in database
	 * @param o a currency
	 * @return the id of the currency created
	 */
    public int create(BusinessObject o) throws DAOException {
        int insert = 0;
        int id = 0;
        Currency curr = (Currency) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("INSERT_CURRENCY"));
            pst.setString(1, curr.getName());
            pst.setInt(2, curr.getIdBase());
            pst.setDouble(3, curr.getValue());
            insert = pst.executeUpdate();
            if (insert <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (insert > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("select max(id) from currency");
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
	 * Sets the tag archived of the currency in param on true 
	 * @param o a currency
	 * @return the number of rows updated
	 */
    public int delete(BusinessObject o) throws DAOException {
        int delete = 0;
        Currency curr = (Currency) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("DELETE_CURRENCY"));
            pst.setInt(1, curr.getId());
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
	 * Returns a list of currencies which matches with the criteria in param
	 * @param criteria
	 * @return a list of currencies
	 */
    public List retrieve(HashMap criteria) throws DAOException {
        List<Currency> result = new ArrayList<Currency>();
        try {
            String search = XMLGetQuery.getQuery("RETRIEVE") + " currency";
            if (criteria != null) {
                search += " where";
                Set s = criteria.entrySet();
                Iterator iter = s.iterator();
                while (iter.hasNext()) {
                    Map.Entry e = (Map.Entry) iter.next();
                    String column = (String) e.getKey();
                    String value = (String) e.getValue();
                    if (column.equals("archived")) {
                        search += " " + column + "=" + value + " and";
                    } else search += " " + column + " like " + "'%" + value + "%' and";
                }
                search = search.substring(0, search.length() - 3);
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(search);
            while (rs.next()) {
                Currency curr = BusinessFactory.createCurrency();
                curr.setId(rs.getInt("id"));
                curr.setName(rs.getString("name"));
                curr.setIdBase(rs.getInt("idbase"));
                curr.setValue(rs.getDouble("value"));
                curr.setArchived(rs.getBoolean("archived"));
                result.add(curr);
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
	 * Updates the currency in database with the currency in param 
	 * @param o a currency
	 * @return the number of rows updated
	 */
    public int update(BusinessObject o) throws DAOException {
        int update = 0;
        Currency curr = (Currency) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("UPDATE_CURRENCY"));
            pst.setString(1, curr.getName());
            pst.setInt(2, curr.getIdBase());
            pst.setDouble(3, curr.getValue());
            pst.setInt(4, curr.getId());
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
