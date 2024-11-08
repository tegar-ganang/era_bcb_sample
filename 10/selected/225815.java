package beans;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import utility.GlobalVariables;

/**
 * This is similar to shopping basket which contains a collection of carts.
 * The cart contains the product and quantity
 * @author krishna
 */
public class ShoppingBean {

    private Collection cart = null;

    private String jdbcURL = null;

    /**
     * Constructor
     */
    public ShoppingBean() {
        this(GlobalVariables.getDatabaseURL());
    }

    /**
     * Constructor
     * @param jdbcURL JDBC URL
     */
    public ShoppingBean(String jdbcURL) {
        this.jdbcURL = jdbcURL;
        this.cart = new ArrayList();
    }

    /**
     * Adds a product in the shopping basket. Product is equivalent to a cart.
     * @param product Product
     * @param quantity Quantity
     */
    public void addProduct(ProductBean product, int quantity) {
        System.out.println(product.getId());
        Object[] shoppingInfo = null;
        Iterator iterator = this.cart.iterator();
        boolean found = false;
        while (iterator.hasNext()) {
            shoppingInfo = new Object[2];
            shoppingInfo = (Object[]) iterator.next();
            if (product.getId() == ((ProductBean) shoppingInfo[0]).getId()) {
                found = true;
                shoppingInfo[1] = ((Integer) shoppingInfo[1]).intValue() + quantity;
                break;
            }
        }
        if (!found) {
            shoppingInfo = new Object[2];
            shoppingInfo[0] = product;
            shoppingInfo[1] = quantity;
            this.cart.add(shoppingInfo);
        }
    }

    /**
     * Removes a product from the shopping basket
     * @param product Product
     */
    public void removeProduct(ProductBean product) {
        Object[] shippingInfo = null;
        Iterator iterator = this.cart.iterator();
        while (iterator.hasNext()) {
            shippingInfo = (Object[]) iterator.next();
            if (product.getId() == ((ProductBean) shippingInfo[0]).getId()) {
                iterator.remove();
            }
        }
    }

    /**
     * Removes a product
     * NOT IMPLEMENTED YET
     * @param product Product to remove
     * @param quantity Quantity
     * @TODO
     */
    public void removeProduct(ProductBean product, int quantity) {
    }

    /**
     * Clears the basket
     */
    public void clear() {
        this.cart.clear();
    }

    /**
     * Gets the collection of carts
     * @return carts
     */
    public Collection getCart() {
        return this.cart;
    }

    /**
     * Stores the shopping information for the user
     * Stores the current shopping information
     * @param userId User ID
     */
    public void updateShoppingBean(String userId) {
        Connection connection = null;
        PreparedStatement preparedStatement1 = null;
        PreparedStatement preparedStatement2 = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(this.jdbcURL);
            connection.setAutoCommit(false);
            String preparedQuery = "INSERT INTO dbComputerShopping.order(name,product_id,quantity,date,status)VALUES(?,?,?,?,?)";
            preparedStatement1 = connection.prepareStatement(preparedQuery);
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String orderDate = format.format(date);
            Iterator iterator = this.cart.iterator();
            Object[] shoppingInfo = null;
            while (iterator.hasNext()) {
                shoppingInfo = (Object[]) iterator.next();
                ProductBean product = (ProductBean) shoppingInfo[0];
                int quantity = (Integer) shoppingInfo[1];
                preparedStatement1.setString(1, userId);
                preparedStatement1.setInt(2, product.getId());
                preparedStatement1.setInt(3, quantity);
                preparedStatement1.setString(4, orderDate);
                preparedStatement1.setString(5, "confirmed");
                preparedStatement1.executeUpdate();
            }
            Object[] cartInfo = null;
            preparedQuery = "UPDATE components SET quantity=quantity-? WHERE comp_id=?";
            preparedStatement2 = connection.prepareStatement(preparedQuery);
            for (Iterator i = this.cart.iterator(); i.hasNext(); ) {
                cartInfo = (Object[]) i.next();
                ProductBean product = (ProductBean) cartInfo[0];
                int quantity = (Integer) cartInfo[1];
                ProductListBean productList = new ProductListBean(jdbcURL);
                ArrayList components = productList.getComponents(product.getId());
                for (Iterator j = components.iterator(); j.hasNext(); ) {
                    ComponentBean component = (ComponentBean) j.next();
                    preparedStatement2.setInt(1, quantity);
                    preparedStatement2.setInt(2, component.getId());
                    preparedStatement2.executeUpdate();
                }
            }
            connection.commit();
        } catch (Exception ex) {
            try {
                connection.rollback();
            } catch (SQLException e) {
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
            }
            try {
                preparedStatement1.close();
            } catch (SQLException ex) {
            }
            try {
                preparedStatement2.close();
            } catch (SQLException ex) {
            }
        }
    }

    /**
     * Gets the equivalent XML representation of the shopping basket
     * It is suitble for displaying
     * @return XML String
     */
    public String getXml() {
        StringBuilder xmlOut = new StringBuilder();
        Object[] shoppingInfo = null;
        xmlOut.append("<shoppinglist>");
        Iterator iterator = this.cart.iterator();
        while (iterator.hasNext()) {
            shoppingInfo = (Object[]) iterator.next();
            xmlOut.append("<order>");
            xmlOut.append(((ProductBean) shoppingInfo[0]).getXml());
            xmlOut.append("<quantity>");
            xmlOut.append(((Integer) shoppingInfo[1]).intValue());
            xmlOut.append("</quantity>");
            xmlOut.append("</order>");
        }
        xmlOut.append("</shoppinglist>");
        return xmlOut.toString();
    }
}
