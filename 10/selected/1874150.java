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
import business.Item;
import dao.DAOException;

/**
 * This class proposes CRUD operations for items.
 * Its attribute is the connection to the database.
 *
 */
public class PostgreItemDAO implements BusinessObjectDAO {

    private Connection connection;

    /**
	 * Constructor
	 * @param con The connection to be used
	 */
    public PostgreItemDAO(Connection con) {
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
	 * Creates an item in database
	 * @param o an item
	 * @return the id of the item created
	 */
    public int create(BusinessObject o) throws DAOException {
        int insert = 0;
        int id = 0;
        Item item = (Item) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("INSERT_ITEM"));
            pst.setString(1, item.getDescription());
            pst.setDouble(2, item.getUnit_price());
            pst.setInt(3, item.getQuantity());
            pst.setDouble(4, item.getVat());
            pst.setInt(5, item.getIdProject());
            pst.setInt(6, item.getIdCurrency());
            insert = pst.executeUpdate();
            if (insert <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (insert > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("select max(id_item) from item");
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
	 * Deletes the item in database 
	 * @param o an item
	 * @return the number of rows updated
	 */
    public int delete(BusinessObject o) throws DAOException {
        int delete = 0;
        Item item = (Item) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("DELETE_ITEM"));
            pst.setInt(1, item.getId());
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
	 * Returns a list of items which matches with the criteria in param
	 * @param criteria
	 * @return a list of items
	 */
    public List retrieve(HashMap criteria) throws DAOException {
        List<Item> result = new ArrayList<Item>();
        try {
            String search = XMLGetQuery.getQuery("RETRIEVE") + " item";
            if (criteria != null && criteria.containsKey("idaccount")) {
                search = "select * from item, project where project.idaccount = " + criteria.get("idaccount") + " and project.id_project = item.idproject and idbill is null order by idproject";
            } else if (criteria != null) {
                search += " where";
                Set s = criteria.entrySet();
                Iterator iter = s.iterator();
                while (iter.hasNext()) {
                    Map.Entry e = (Map.Entry) iter.next();
                    String column = (String) e.getKey();
                    String value = (String) e.getValue();
                    if (column.equals("archived") || column.equals("idproject")) search += " " + column + " = " + value + " and"; else if (column.equals("idbill") && value.equals("null")) search += " " + column + " is " + value + " and"; else if (!(column.equals("order by"))) search += " " + column + " like " + "'%" + value + "%' and";
                }
                search = search.substring(0, search.length() - 3);
                if (criteria.containsKey("order by")) search += " order by " + criteria.get("order by");
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(search);
            while (rs.next()) {
                Item item = BusinessFactory.createItem();
                item.setId(rs.getInt("id_item"));
                item.setDescription(rs.getString("item_description"));
                item.setUnit_price(rs.getDouble("unit_price"));
                item.setQuantity(rs.getInt("quantity"));
                item.setVat(rs.getDouble("vat"));
                item.setIdProject(rs.getInt("idproject"));
                item.setIdBill(rs.getInt("idbill"));
                item.setIdCurrency(rs.getInt("idcurrency"));
                result.add(item);
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
	 * Updates the item in database with the item in param 
	 * @param o an item
	 * @return the number of rows updated
	 */
    public int update(BusinessObject o) throws DAOException {
        int update = 0;
        Item item = (Item) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("UPDATE_ITEM"));
            pst.setString(1, item.getDescription());
            pst.setDouble(2, item.getUnit_price());
            pst.setInt(3, item.getQuantity());
            pst.setDouble(4, item.getVat());
            pst.setInt(5, item.getIdProject());
            if (item.getIdBill() == 0) pst.setNull(6, java.sql.Types.INTEGER); else pst.setInt(6, item.getIdBill());
            pst.setInt(7, item.getIdCurrency());
            pst.setInt(8, item.getId());
            System.out.println("item => " + item.getDescription() + " " + item.getUnit_price() + " " + item.getQuantity() + " " + item.getVat() + " " + item.getIdProject() + " " + item.getIdBill() + " " + item.getIdCurrency() + " " + item.getId());
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
