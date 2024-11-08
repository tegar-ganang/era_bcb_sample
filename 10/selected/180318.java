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
import business.Contact;
import dao.DAOException;

/**
 * This class proposes CRUD operations for contacts.
 * Its attribute is the connection to the database.
 *
 */
public class PostgreContactDAO implements BusinessObjectDAO {

    private Connection connection;

    /**
	 * Constructor
	 * @param con The connection to be used
	 */
    public PostgreContactDAO(Connection con) {
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
	 * Inserts in database a new contact
	 * @param o a contact
	 * @return int the id of the new Contact
	 */
    public int create(BusinessObject o) throws DAOException {
        int insert = 0;
        int id = 0;
        Contact contact = (Contact) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("INSERT_CONTACT"));
            pst.setString(1, contact.getName());
            pst.setString(2, contact.getFirstname());
            pst.setString(3, contact.getPhone());
            pst.setString(4, contact.getEmail());
            if (contact.getAccount() == 0) {
                pst.setNull(5, java.sql.Types.INTEGER);
            } else {
                pst.setInt(5, contact.getAccount());
            }
            insert = pst.executeUpdate();
            if (insert <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (insert > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("select max(id) from contact");
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
	 * Deletes the contact in parameter (sets its column archived to true)
	 * @param o a contact
	 * @return int the number of modifications
	 */
    public int delete(BusinessObject o) throws DAOException {
        int delete = 0;
        Contact contact = (Contact) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("DELETE_CONTACT"));
            pst.setInt(1, contact.getId());
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
	 * Searchs all contacts which corresponds with criteria in parameter
	 * @param criteria
	 * @return List of results
	 */
    public List retrieve(HashMap criteria) throws DAOException {
        List<Contact> result = new ArrayList<Contact>();
        try {
            String search = XMLGetQuery.getQuery("RETRIEVE") + " contact";
            if (criteria != null) {
                search += " where";
                Set s = criteria.entrySet();
                Iterator iter = s.iterator();
                while (iter.hasNext()) {
                    Map.Entry e = (Map.Entry) iter.next();
                    String column = (String) e.getKey();
                    String value = (String) e.getValue();
                    if (column.equals("idaccount") && value.equals("is null")) search += " " + column + " " + value + " and"; else search += " " + column + " like " + "'%" + value + "%' and";
                }
                search = search.substring(0, search.length() - 3);
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(search);
            while (rs.next()) {
                Contact contact = BusinessFactory.createContact();
                contact.setId(rs.getInt("id"));
                contact.setName(rs.getString("name"));
                contact.setFirstname(rs.getString("firstname"));
                contact.setPhone(rs.getString("phone"));
                contact.setEmail(rs.getString("email"));
                contact.setAccount(rs.getInt("idaccount"));
                contact.setArchived(rs.getBoolean("archived"));
                result.add(contact);
            }
            connection.commit();
        } catch (SQLException e) {
            Log.write(e.getMessage());
            throw new DAOException("A SQLException has occured");
        } catch (NullPointerException npe) {
            Log.write(npe.getMessage());
            throw new DAOException("Connection null");
        }
        return result;
    }

    /**
	 * Updates in database the value of the contact in parameter
	 * @param o a contact
	 * @return int number of updates
	 */
    public int update(BusinessObject o) throws DAOException {
        int update = 0;
        Contact contact = (Contact) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("UPDATE_CONTACT"));
            pst.setString(1, contact.getName());
            pst.setString(2, contact.getFirstname());
            pst.setString(3, contact.getPhone());
            pst.setString(4, contact.getEmail());
            if (contact.getAccount() == 0) {
                pst.setNull(5, java.sql.Types.INTEGER);
            } else {
                pst.setInt(5, contact.getAccount());
            }
            pst.setBoolean(6, contact.isArchived());
            pst.setInt(7, contact.getId());
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
