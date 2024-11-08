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
import business.Project;
import dao.DAOException;

/**
 * This class proposes CRUD operations for projects.
 * Its attribute is the connection to the database.
 *
 */
public class PostgreProjectDAO implements BusinessObjectDAO {

    private Connection connection;

    /**
	 * Constructor
	 * @param con The connection to be used
	 */
    public PostgreProjectDAO(Connection con) {
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
	 * Creates a project in database
	 * @param o a project
	 * @return the id of the project created
	 */
    public int create(BusinessObject o) throws DAOException {
        int insert = 0;
        int id = 0;
        Project project = (Project) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("INSERT_PROJECT"));
            pst.setString(1, project.getName());
            pst.setString(2, project.getDescription());
            pst.setInt(3, project.getIdAccount());
            pst.setInt(4, project.getIdContact());
            insert = pst.executeUpdate();
            if (insert <= 0) {
                connection.rollback();
                throw new DAOException("Number of rows <= 0");
            } else if (insert > 1) {
                connection.rollback();
                throw new DAOException("Number of rows > 1");
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery("select max(id_project) from project");
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
	 * Sets the tag archived of the project in param on true 
	 * @param o a project
	 * @return the number of rows updated
	 */
    public int delete(BusinessObject o) throws DAOException {
        int delete = 0;
        Project project = (Project) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("DELETE_PROJECT"));
            pst.setInt(1, project.getId());
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
	 * Returns a list of projects which matches with the criteria in param
	 * @param criteria
	 * @return a list of projects
	 */
    public List retrieve(HashMap criteria) throws DAOException {
        List<Project> result = new ArrayList<Project>();
        try {
            String search = XMLGetQuery.getQuery("RETRIEVE") + " project p, account a, contact c";
            if (criteria != null) {
                search += " where";
                Set s = criteria.entrySet();
                Iterator iter = s.iterator();
                while (iter.hasNext()) {
                    Map.Entry e = (Map.Entry) iter.next();
                    String column = (String) e.getKey();
                    String value = (String) e.getValue();
                    if (column.contains("archived") || column.contains("id")) search += " " + column + "=" + value + " and"; else search += " " + column + " like " + "'%" + value + "%' and";
                }
                search += " p.idaccount = a.id and p.idcontact = c.id";
            }
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(search);
            while (rs.next()) {
                Project project = BusinessFactory.createProject();
                project.setId(rs.getInt("id_project"));
                project.setName(rs.getString("project_name"));
                project.setDescription(rs.getString("project_description"));
                project.setIdAccount(rs.getInt("idaccount"));
                project.setIdContact(rs.getInt("idcontact"));
                project.setArchived(rs.getBoolean("project_archived"));
                result.add(project);
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
	 * Updates the project in database with the project in param 
	 * @param o a project
	 * @return the number of rows updated
	 */
    public int update(BusinessObject o) throws DAOException {
        int update = 0;
        Project project = (Project) o;
        try {
            PreparedStatement pst = connection.prepareStatement(XMLGetQuery.getQuery("UPDATE_PROJECT"));
            pst.setString(1, project.getName());
            pst.setString(2, project.getDescription());
            pst.setInt(3, project.getIdAccount());
            pst.setInt(4, project.getIdContact());
            pst.setBoolean(5, project.isArchived());
            pst.setInt(6, project.getId());
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
