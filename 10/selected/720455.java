package beans;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utility.DBConnection;
import utility.GlobalVariables;

/**
 * Customer profile bean to store the user profile. This stores all the information
 * of a customer like user name, password, adresses, email address etc.
 * @author krishna
 */
public class CustomerProfileBean {

    private String url = null;

    private String name = null;

    private String password = null;

    private String firstName = null;

    private String middleName = null;

    private String lastName = null;

    private String address1 = null;

    private String address2 = null;

    private String city = null;

    private String postBox = null;

    private String email = null;

    private String country = null;

    private DBConnection connection = null;

    /**
     * Constructor
     */
    public CustomerProfileBean() {
        this(GlobalVariables.getDatabaseURL());
    }

    /**
     * Constructor
     * @param url JDBC URL
     */
    public CustomerProfileBean(String url) {
        this.url = url;
        this.connection = new DBConnection(this.url);
    }

    /**
     * Gets customer id which is unique for each customer
     * @return customer id
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets customer id which is unique for each customer
     * @param name id of customer
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets customer password
     * @return password password of the customer
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the password of customer
     * @param password password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Gets first name of the customer
     * @return first name
     */
    public String getFirstName() {
        return this.firstName;
    }

    /**
     * Sets first name of the customer
     * @param firstName first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the middle name of customer
     * @return middle name
     */
    public String getMiddleName() {
        return this.middleName;
    }

    /**
     * Sets the middle name of customer
     * @param middleName middle name
     */
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /**
     * Gets the last name of customer
     * @return last name
     */
    public String getLastName() {
        return this.lastName;
    }

    /**
     * Set the last name
     * @param lastName last name of customer
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets address1 of customer
     * @return address1
     */
    public String getAddress1() {
        return this.address1;
    }

    /**
     * Sets address1 of customer
     * @param address1
     */
    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    /**
     * Gets address2 of customer
     * @return address2 of customer
     */
    public String getAddress2() {
        return this.address2;
    }

    /**
     * Sets address2 of customer
     * @param address2
     */
    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    /**
     * Gets city
     * @return city of customer
     */
    public String getCity() {
        return this.city;
    }

    /**
    * Set city of customer
    * @param city
    */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Gets post box of customer
     * @return post box
     */
    public String getPostBox() {
        return this.postBox;
    }

    /**
     * sets post box of customer
     * @param postBox post box
     */
    public void setPostBox(String postBox) {
        this.postBox = postBox;
    }

    /**
     * Gets email address of customer
     * @return email address
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Sets the email address
     * @param email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets country
     * @return country
     */
    public String getCountry() {
        return this.country;
    }

    /**
     * Sets the country of customer
     * @param country
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Loads the customer profile. We need to call this method to get profile informaiton.
     * So,before we use this bean, we need to call this method.
     * @param name customer id
     * @throws Exception 
     */
    public void populateCustomerProfile(String name) throws Exception {
        ResultSet resultSet;
        String query = "SELECT a.name,a.password,b.first_name,b.middle_name," + "b.last_name,b.address1,b.address2,b.city,b.post_box,b.email," + "b.country FROM customers a LEFT JOIN customers_profile b ON a.name=b.name" + " WHERE a.name='" + name + "'";
        System.out.println(query);
        resultSet = this.connection.ExecuteQuery(query);
        resultSet.next();
        this.name = name;
        this.password = resultSet.getString("password");
        this.firstName = resultSet.getString("first_name");
        this.middleName = resultSet.getString("middle_name");
        this.lastName = resultSet.getString("last_name");
        this.address1 = resultSet.getString("address1");
        this.address2 = resultSet.getString("address2");
        this.city = resultSet.getString("city");
        this.postBox = resultSet.getString("post_box");
        this.email = resultSet.getString("email");
        this.country = resultSet.getString("country");
        this.connection.close();
    }

    /**
     * Inserts the profile information
     * Implementation of transaction management and prepared statement
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public void insertProfile() throws ClassNotFoundException, SQLException {
        Connection connection = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(this.url);
            connection.setAutoCommit(false);
            String query1 = "INSERT INTO customers(name,password) VALUES(?,?)";
            ps1 = connection.prepareStatement(query1);
            ps1.setString(1, this.name);
            ps1.setString(2, this.password);
            String query2 = "INSERT INTO customer_roles(name,role_name) VALUES(?,?)";
            ps2 = connection.prepareStatement(query2);
            ps2.setString(1, this.name);
            ps2.setString(2, "user");
            String query3 = "INSERT INTO customers_profile(name,first_name,middle_name,last_name,address1,address2,city,post_box,email,country)" + "VALUES(?,?,?,?,?,?,?,?,?,?)";
            ps3 = connection.prepareStatement(query3);
            ps3.setString(1, this.name);
            ps3.setString(2, this.firstName);
            ps3.setString(3, this.middleName);
            ps3.setString(4, this.lastName);
            ps3.setString(5, this.address1);
            ps3.setString(6, this.address2);
            ps3.setString(7, this.city);
            ps3.setString(8, this.postBox);
            ps3.setString(9, this.email);
            ps3.setString(10, this.country);
            ps1.executeUpdate();
            ps2.executeUpdate();
            ps3.executeUpdate();
            connection.commit();
        } catch (Exception ex) {
            connection.rollback();
        } finally {
            try {
                this.connection.close();
            } catch (Exception ex) {
            }
            try {
                ps1.close();
            } catch (Exception ex) {
            }
            try {
                ps2.close();
            } catch (Exception ex) {
            }
            try {
                ps3.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Updates the profile information. It is used to change or edit the user profile.
     * It can also used to change password.
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public void updateProfile() throws ClassNotFoundException, SQLException {
        Connection connection = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(this.url);
            connection.setAutoCommit(false);
            String query2 = "UPDATE customers SET password=? WHERE name=?";
            String query3 = "UPDATE customers_profile " + "SET first_name=?,middle_name=?,last_name=?,address1=?" + ",address2=?,city=?,post_box=?,email=?,country=? WHERE name=?";
            ps1 = connection.prepareStatement(query3);
            ps2 = connection.prepareStatement(query2);
            ps1.setString(1, this.firstName);
            ps1.setString(2, this.middleName);
            ps1.setString(3, this.lastName);
            ps1.setString(4, this.address1);
            ps1.setString(5, this.address2);
            ps1.setString(6, this.city);
            ps1.setString(7, this.postBox);
            ps1.setString(8, this.email);
            ps1.setString(9, this.country);
            ps1.setString(10, this.name);
            ps2.setString(1, this.password);
            ps2.setString(2, this.name);
            ps1.executeUpdate();
            ps2.executeUpdate();
        } catch (Exception ex) {
            connection.rollback();
        } finally {
            try {
                this.connection.close();
            } catch (Exception ex) {
            }
            try {
                ps1.close();
            } catch (Exception ex) {
            }
            try {
                ps2.close();
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Checks whether user already exist in the database or not
     * It can be used to <i>Check</i> before inserting new user profile
     * @param userId customer id
     * @return true: if user exists, false: if does not exist
     */
    public boolean userExists(String userId) {
        String query = "SELECT name from customers WHERE name='" + userId + "'";
        try {
            ResultSet resultSet = this.connection.ExecuteQuery(query);
            return resultSet.next();
        } catch (Exception ex) {
            return false;
        } finally {
            this.connection.close();
        }
    }

    /**
     * For testing purpose
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        CustomerProfileBean customerProfile = new CustomerProfileBean();
        customerProfile.populateCustomerProfile("krishna");
        System.out.println(customerProfile.getAddress1());
    }
}
