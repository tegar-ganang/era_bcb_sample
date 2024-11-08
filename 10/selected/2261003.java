package beans;

import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import java.sql.DriverManager;
import java.sql.SQLException;
import utility.GlobalVariables;

/**
 *
 * @author Suman
 */
public class MappingBean {

    private int productId;

    private String productName;

    private int componentId;

    private String componentName;

    private int count;

    private String jdbcURL = null;

    /**
     * constructor that initializes jdbc connection url
     */
    public MappingBean() {
        this(GlobalVariables.getDatabaseURL());
    }

    /**
     * parameterized constructor that initializes the url;
     * @param _url
     */
    public MappingBean(String _url) {
        jdbcURL = _url;
    }

    /**
     * gets producrt id
     * @return
     */
    public int getProductId() {
        return productId;
    }

    /**
     * sets product id
     * @param _productId
     */
    public void setProductId(int _productId) {
        productId = _productId;
    }

    /**
     * gets product name
     * @return
     */
    public String getProductName() {
        return productName;
    }

    /**
     * gets component id
     * @return
     */
    public int getComponentId() {
        return componentId;
    }

    /**
     * sets component id
     * @param _componentId
     */
    public void setComponentId(int _componentId) {
        componentId = _componentId;
    }

    /**
     * gets component name
     * @return
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * gets count
     * @return
     */
    public int getCount() {
        return count;
    }

    /**
     * sets count
     * @param _count
     */
    public void setCount(int _count) {
        count = _count;
    }

    /**
     * inserts into mapping
     * @throws SQLException
     */
    public void saveMapping() throws SQLException {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = (Connection) DriverManager.getConnection(this.jdbcURL);
            connection.setAutoCommit(false);
            String query = "INSERT INTO mapping(product_id, comp_id, count) VALUES(?,?,?)";
            ps = (PreparedStatement) connection.prepareStatement(query);
            ps.setInt(1, this.productId);
            ps.setInt(2, this.componentId);
            ps.setInt(3, 1);
            ps.executeUpdate();
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
