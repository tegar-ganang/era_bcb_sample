package beans;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import com.mysql.jdbc.Statement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import utility.GlobalVariables;

/**
 * Bean to represent a product item
 * @author krishna
 */
public class ProductBean {

    private int id = -1;

    private String name = null;

    private String description = null;

    private String jdbcURL = null;

    public ProductBean() {
        this(GlobalVariables.getDatabaseURL());
    }

    /**
     * Constructor
     * @param _url JDBC URL
     */
    public ProductBean(String _url) {
        jdbcURL = _url;
    }

    /**
     * Gets product ID
     * @return product Id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Sets product ID
     * @param id product id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets product name
     * @return product name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets product name
     * @param name product name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets product description
     * @return product description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets product description
     * @param description product descritpion
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets XML representation of product
     * @return XML string
     */
    public String getXml() {
        StringBuilder xmlOut = new StringBuilder();
        xmlOut.append("<product>");
        xmlOut.append("<id>");
        xmlOut.append(this.id);
        xmlOut.append("</id>");
        xmlOut.append("<name>");
        xmlOut.append(name);
        xmlOut.append("</name>");
        xmlOut.append("<description>");
        xmlOut.append(description);
        xmlOut.append("</description>");
        xmlOut.append("</product>");
        return xmlOut.toString();
    }

    /**
     * Saves the product
     * @return max product id
     * @throws SQLException
     * */
    public int saveProduct() throws SQLException {
        Connection connection = null;
        PreparedStatement ps1 = null;
        ResultSet rs = null;
        int value = 0;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = (Connection) DriverManager.getConnection(jdbcURL);
            connection.setAutoCommit(false);
            String query1 = "INSERT INTO products(name,description) VALUES(?,?)";
            ps1 = (PreparedStatement) connection.prepareStatement(query1);
            ps1.setString(1, this.name);
            ps1.setString(2, this.description);
            ps1.executeUpdate();
            String query2 = "SELECT MAX(product_id) AS ProductId FROM products";
            Statement s = (Statement) connection.createStatement();
            s.executeQuery(query2);
            rs = s.getResultSet();
            while (rs.next()) {
                value = rs.getInt("productId");
                break;
            }
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
        } finally {
            try {
                connection.close();
            } catch (Exception ex) {
            }
            try {
                ps1.close();
            } catch (Exception ex) {
            }
        }
        return value;
    }
}
