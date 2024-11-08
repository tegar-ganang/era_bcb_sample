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
import business.Account;
import business.BusinessFactory;
import business.BusinessObject;
import dao.DAOException;

/**
 * This class proposes CRUD operations for accounts.
 * Its attribute is the connection to the database.
 *
 */
public class PostgreAccountDAO implements BusinessObjectDAO {

    private Connection connection;

    /**
	 * Constructor
	 * @param con The connection to be used
	 */
    public PostgreAccountDAO(Connection con) {
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
	 * Inserts in table account a new account which is in parameter
	 * @param o an account
	 * @return int the number of inserted lines
	 */
    public int create(BusinessObject o) throws DAOException {
        int insert = 0;
        int id = 0;
        Account acc = (Account) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("INSERT_ACCOUNT"));
            pst.setString(1, acc.getName());
            pst.setString(2, acc.getAddress());
            pst.setInt(3, acc.getCurrency());
            pst.setInt(4, acc.getMainContact());
            insert = pst.executeUpdate();
            if (insert <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (insert > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("select max(id) from account");
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
	 * "Deletes" in table account the account in parameter (sets the column archived in database to true)
	 * @param o an account
	 * @return int the number of archived lines
	 */
    public int delete(BusinessObject o) throws DAOException {
        int delete = 0;
        Account acc = (Account) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("DELETE_ACCOUNT"));
            pst.setInt(1, acc.getId());
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
	 * Searchs all accounts that could corresponds to criteria
	 * @param criteria the criteria for research
	 * @return List of results 
	 */
    public List retrieve(HashMap criteria) throws DAOException {
        List<Account> result = new ArrayList<Account>();
        try {
            String search = XMLGetQuery.getQuery("RETRIEVE") + " account";
            if (criteria != null) {
                search += " where";
                Set s = criteria.entrySet();
                Iterator iter = s.iterator();
                while (iter.hasNext()) {
                    Map.Entry e = (Map.Entry) iter.next();
                    String column = (String) e.getKey();
                    String value = (String) e.getValue();
                    if (column.equals("archived")) {
                        search += " " + column + " = " + value + " and";
                    } else {
                        search += " " + column + " like " + "'%" + value + "%' and";
                    }
                }
                search = search.substring(0, search.length() - 3);
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(search);
            while (rs.next()) {
                Account acc = BusinessFactory.createAccount();
                acc.setId(rs.getInt("id"));
                acc.setName(rs.getString("name"));
                acc.setAddress(rs.getString("address"));
                acc.setCurrency(rs.getInt("idcurrency"));
                acc.setMainContact(rs.getInt("idmaincontact"));
                acc.setArchived(rs.getBoolean("archived"));
                result.add(acc);
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
	 * Updates the account in database
	 * @param o an account
	 * @return int number of updates
	 */
    public int update(BusinessObject o) throws DAOException {
        int update = 0;
        Account acc = (Account) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("UPDATE_ACCOUNT"));
            pst.setString(1, acc.getName());
            pst.setString(2, acc.getAddress());
            pst.setInt(3, acc.getCurrency());
            pst.setInt(4, acc.getMainContact());
            pst.setBoolean(5, acc.isArchived());
            pst.setInt(6, acc.getId());
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
