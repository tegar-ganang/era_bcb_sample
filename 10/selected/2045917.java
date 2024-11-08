package beans;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import utility.GlobalVariables;

/**
 * This is component bean which stores individual component
 * @author krishna
 */
public class ComponentBean {

    private String jdbcURL = null;

    private int id = -1;

    private String name = null;

    private int quantity = -1;

    private double rate = 0.0;

    private String description = null;

    public ComponentBean() {
        this(GlobalVariables.getDatabaseURL());
    }

    public ComponentBean(String _url) {
        jdbcURL = _url;
    }

    /**
     * Gets Component Id
     * @return Id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Sets component Id
     * @param id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets component name
     * @return component name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets component name
     * @param name component name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets quantity
     * @return quantity
     */
    public int getQuantity() {
        return this.quantity;
    }

    /**
     * Sets quantity
     * @param quantity total numbber of item
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /**
     * Gets rate
     * @return rate
     */
    public double getRate() {
        return this.rate;
    }

    /**
     * Sets rate
     * @param rate rate of the component
     */
    public void setRate(double rate) {
        this.rate = rate;
    }

    /**
     * Gets description
     * @return description of the component
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets description of the component
     * @param description description of component
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets XML representation of component. It is suitable for displaying using
     * XSLT
     * @return XML representaton of component
     */
    public String getXml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<component>");
        builder.append("<id>");
        builder.append(this.id);
        builder.append("</id>");
        builder.append("<name>");
        builder.append(name);
        builder.append("</name>");
        builder.append("<quantity>");
        builder.append(quantity);
        builder.append("</quantity>");
        builder.append("<rate>");
        builder.append(rate);
        builder.append("</rate>");
        builder.append("<description>");
        builder.append(description);
        builder.append("</description>");
        builder.append("</component>");
        return builder.toString();
    }

    /**
    * Saves component in database
    * @throws SQLException If some problem, throws sql exception
    */
    public void insertComponent() throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = (Connection) DriverManager.getConnection(this.jdbcURL);
            connection.setAutoCommit(false);
            String query = "INSERT INTO components(name,rate,quantity, description) VALUES(?,?,?,?)";
            ps = (PreparedStatement) connection.prepareStatement(query);
            ps.setString(1, this.name);
            ps.setDouble(2, this.rate);
            ps.setInt(3, this.quantity);
            ps.setString(4, this.description);
            ps.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
        } finally {
            try {
                connection.close();
            } catch (Exception ex) {
            }
            try {
                ps.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * It updates the quantity of the component
     * @param id component id
     * @param quantity quantity to add
     */
    public void updateComponent(int id, int quantity) throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = (Connection) DriverManager.getConnection(this.jdbcURL);
            connection.setAutoCommit(false);
            String query = "UPDATE components SET quantity=quantity+? WHERE comp_id=?";
            ps = connection.prepareStatement(query);
            ps.setInt(1, quantity);
            ps.setInt(2, id);
            ps.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
        } finally {
            try {
                connection.close();
            } catch (Exception ex) {
            }
            try {
                ps.close();
            } catch (Exception ex) {
            }
        }
    }
}
