package hambo.user;

import java.sql.*;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import hambo.svc.database.*;
import hambo.svc.*;
import hambo.util.OID;
import hambo.util.HamboFatalException;

/**
 * Manages the Hambo portal users.
 * 
 */
public class HamboUserManager extends UserManager {

    private static final String MSG_SERVICE_LOOKUP_FAILED = "HamboUserManager failed to lookup database service.";

    private static final String MSG_SQL_FAILED = "HamboUserManager failed to execute SQL statement.";

    private static final String MSG_CONNECTION_FAILED = "HamboUserManager failed to allocate database connection.";

    private static final String MSG_RESET_FAILED = "HamboUserManager failed to release database connection.";

    private static final String MSG_INSERT_FAILED = "HamboUserManager failed to insert user into database.";

    private static final String TABLE_COLUMNS = "userid,OId,password,language,timezn," + "datecreated,lastlogin,disabled,wapsigned,ldapInSync,offerings,firstname," + "lastname,street,zipcode,city,province,country,birthday,gender,email,cellph";

    /**
     * Default constructor.
     */
    public HamboUserManager() {
    }

    /**
     * Initialize the instance with properties. This method is called when
     * the instance is created.
     * @param prop a Properties object.
     */
    public void init(Properties prop) {
    }

    /**
     * Will attempt to find a stored user entry with the given user id.
     * @param userId the wanted user's user id.
     * @return a {@link hambo.user.User} object if found, otherwise null.
     */
    public User findUserFromMobileNumber(String sender) throws HamboFatalException {
        DBConnection con = null;
        User user = null;
        try {
            con = DBServiceManager.allocateConnection();
            String sql = "select " + TABLE_COLUMNS + " from user_UserAccount where cellph=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, sender);
            ResultSet rs = con.executeQuery(ps, null);
            if (rs.next()) user = buildUser(rs);
        } catch (SQLException ex) {
            throw new HamboFatalException(MSG_SQL_FAILED, ex);
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return user;
    }

    /**
     * Will attempt to find a stored user entry with the given user id.
     * @param userId the wanted user's user id.
     * @return a {@link hambo.user.User} object if found, otherwise null.
     */
    public User findUser(String userId) throws HamboFatalException {
        DBConnection con = null;
        User user = null;
        try {
            con = DBServiceManager.allocateConnection();
            String sql = "select " + TABLE_COLUMNS + "  from user_UserAccount where userid=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ResultSet rs = con.executeQuery(ps, null);
            if (rs.next()) user = buildUser(rs);
        } catch (SQLException ex) {
            throw new HamboFatalException(MSG_SQL_FAILED, ex);
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return user;
    }

    /**
     * Will attempt to find a stored user entry with the given OID value.
     * @param oid the wanted user's OID value.
     * @return a {@link hambo.user.User} object if found, otherwise null.
     */
    public User findUser(OID oid) throws HamboFatalException {
        DBConnection con = null;
        User user = null;
        try {
            con = DBServiceManager.allocateConnection();
            String sql = "select " + TABLE_COLUMNS + " from user_UserAccount where OId=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setBigDecimal(1, oid);
            ResultSet rs = con.executeQuery(ps, null);
            if (rs.next()) user = buildUser(rs);
        } catch (SQLException ex) {
            throw new HamboFatalException(MSG_SQL_FAILED, ex);
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return user;
    }

    /**
     * Finds a user by querying the database for a matching uid/pwd pair.
     * @param userid the user's username.
     * @param encryptedPassword the user's password in encrypted/encoded format.
     * @return a {@link hambo.user.User} object if found, otherwise null.
     */
    public User findUser(String userId, String encryptedPassword) throws HamboFatalException {
        DBConnection con = null;
        User user = null;
        try {
            con = DBServiceManager.allocateConnection();
            String sql = "select " + TABLE_COLUMNS + " from user_UserAccount where userid=? and password=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setString(2, encryptedPassword);
            ResultSet rs = con.executeQuery(ps, null);
            if (rs.next()) user = buildUser(rs);
        } catch (SQLException ex) {
            throw new HamboFatalException(MSG_SQL_FAILED, ex);
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return user;
    }

    /**
     * Inserts a user into the database.
     * TODO: Don't insert the messenger folders here!
     * @return a {@link hambo.user.User} object based on the user data.
     */
    public User createUser(Map userData) throws HamboFatalException {
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            String userId = (String) userData.get(HamboUser.USER_ID);
            String sql = "insert into user_UserAccount " + "(userid,firstname,lastname,street,zipcode,city," + "province,country,email,cellph,gender,password," + "language,timezn,birthday,datecreated,lastlogin," + "disabled,wapsigned,ldapInSync,offerings,firstcb) " + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setString(2, (String) userData.get(HamboUser.FIRST_NAME));
            ps.setString(3, (String) userData.get(HamboUser.LAST_NAME));
            ps.setString(4, (String) userData.get(HamboUser.STREET_ADDRESS));
            ps.setString(5, (String) userData.get(HamboUser.ZIP_CODE));
            ps.setString(6, (String) userData.get(HamboUser.CITY));
            ps.setString(7, (String) userData.get(HamboUser.STATE));
            ps.setString(8, (String) userData.get(HamboUser.COUNTRY));
            ps.setString(9, (String) userData.get(HamboUser.EXTERNAL_EMAIL_ADDRESS));
            ps.setString(10, (String) userData.get(HamboUser.MOBILE_NUMBER));
            ps.setString(11, (String) userData.get(HamboUser.GENDER));
            ps.setString(12, (String) userData.get(HamboUser.PASSWORD));
            ps.setString(13, (String) userData.get(HamboUser.LANGUAGE));
            ps.setString(14, (String) userData.get(HamboUser.TIME_ZONE));
            java.sql.Date date = (java.sql.Date) userData.get(HamboUser.BIRTHDAY);
            if (date != null) ps.setDate(15, date); else ps.setNull(15, Types.DATE);
            date = (java.sql.Date) userData.get(HamboUser.CREATED);
            if (date != null) ps.setDate(16, date); else ps.setNull(16, Types.DATE);
            date = (java.sql.Date) userData.get(HamboUser.LAST_LOGIN);
            if (date != null) ps.setDate(17, date); else ps.setNull(17, Types.DATE);
            Boolean bool = (Boolean) userData.get(HamboUser.DISABLED);
            if (bool != null) ps.setBoolean(18, bool.booleanValue()); else ps.setBoolean(18, UserAccountInfo.DEFAULT_DISABLED);
            bool = (Boolean) userData.get(HamboUser.WAP_ACCOUNT);
            if (bool != null) ps.setBoolean(19, bool.booleanValue()); else ps.setBoolean(19, UserAccountInfo.DEFAULT_WAP_ACCOUNT);
            bool = (Boolean) userData.get(HamboUser.LDAP_IN_SYNC);
            if (bool != null) ps.setBoolean(20, bool.booleanValue()); else ps.setBoolean(20, UserAccountInfo.DEFAULT_LDAP_IN_SYNC);
            bool = (Boolean) userData.get(HamboUser.OFFERINGS);
            if (bool != null) ps.setBoolean(21, bool.booleanValue()); else ps.setBoolean(21, UserAccountInfo.DEFAULT_OFFERINGS);
            ps.setString(22, (String) userData.get(HamboUser.COBRANDING_ID));
            con.executeUpdate(ps, null);
            ps = con.prepareStatement(DBUtil.getQueryCurrentOID(con, "user_UserAccount", "newoid"));
            ResultSet rs = con.executeQuery(ps, null);
            if (rs.next()) {
                OID newOID = new OID(rs.getBigDecimal("newoid").doubleValue());
                userData.put(HamboUser.OID, newOID);
            }
            con.commit();
        } catch (Exception ex) {
            if (con != null) try {
                con.rollback();
            } catch (SQLException sqlex) {
            }
            throw new HamboFatalException(MSG_INSERT_FAILED, ex);
        } finally {
            if (con != null) try {
                con.reset();
            } catch (SQLException ex) {
            }
            if (con != null) con.release();
        }
        return buildUser(userData);
    }

    /**
     * Updates the stored user entry.
     * @param user the User object that contains the information that the
     *        user entry will be updated with.
     */
    public void updateUser(User user) throws HamboFatalException {
        UserAccountInfo accountInfo = user.getAccountInfo();
        UserContactInfo contactInfo = user.getContactInfo();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            String sql = "update user_UserAccount set password=?,language=?,timezn=?," + "lastlogin=?,disabled=?,wapsigned=?,ldapInSync=?,offerings=?,firstcb=?," + "firstname=?,lastname=?,street=?,zipcode=?,city=?,province=?," + "country=?,birthday=?,gender=?,email=?,cellph=? " + "where OId=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, accountInfo.getPassword());
            ps.setString(2, accountInfo.getLanguage());
            ps.setString(3, accountInfo.getTimeZone());
            ps.setDate(4, accountInfo.getLastLogin());
            ps.setBoolean(5, accountInfo.getDisabled());
            ps.setBoolean(6, accountInfo.getWapAccount());
            ps.setBoolean(7, false);
            ps.setBoolean(8, accountInfo.getOfferings());
            ps.setString(9, accountInfo.getCobrandingId());
            ps.setString(10, contactInfo.getFirstName());
            ps.setString(11, contactInfo.getLastName());
            ps.setString(12, contactInfo.getStreetAddress());
            ps.setString(13, contactInfo.getZipCode());
            ps.setString(14, contactInfo.getCity());
            ps.setString(15, contactInfo.getState());
            ps.setString(16, contactInfo.getCountry());
            if (contactInfo.getBirthday() != null) ps.setDate(17, contactInfo.getBirthday()); else ps.setNull(17, Types.DATE);
            ps.setString(18, contactInfo.getGender());
            ps.setString(19, contactInfo.getExternalEmailAddress());
            ps.setString(20, contactInfo.getMobileNumber());
            ps.setBigDecimal(21, user.getOID());
            con.executeUpdate(ps, null);
        } catch (SQLException ex) {
            throw new HamboFatalException(MSG_SQL_FAILED, ex);
        } finally {
            if (con != null) {
                con.release();
            }
        }
    }

    /**
     * Updates the user account when the user has logged in.
     * @param oid the user's OID value.
     */
    public void updateLogin(OID oid) throws HamboFatalException {
        DBConnection con = null;
        try {
            java.util.Date date = new java.util.Date();
            java.sql.Date sqlDate = new java.sql.Date(date.getTime());
            con = DBServiceManager.allocateConnection();
            String sql = "update user_UserAccount set lastlogin=? where OId=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDate(1, sqlDate);
            ps.setBigDecimal(2, oid);
            con.executeUpdate(ps, null);
        } catch (SQLException ex) {
            throw new HamboFatalException(MSG_SQL_FAILED, ex);
        } finally {
            if (con != null) {
                con.release();
            }
        }
    }

    /**
     * Updates the user account when the user has logged out.
     * @param oid the user's OID value.
     */
    public void updateLogout(OID oid) {
    }

    User buildUser(Map userData) {
        String userId = (String) userData.get(HamboUser.USER_ID);
        OID oid = (OID) userData.get(HamboUser.OID);
        UserAccountInfo accountInfo = new UserAccountInfo();
        accountInfo.setPassword((String) userData.get(HamboUser.PASSWORD));
        accountInfo.setLanguage((String) userData.get(HamboUser.LANGUAGE));
        accountInfo.setTimeZone((String) userData.get(HamboUser.TIME_ZONE));
        accountInfo.setCreated((java.sql.Date) userData.get(HamboUser.CREATED));
        accountInfo.setLastLogin((java.sql.Date) userData.get(HamboUser.LAST_LOGIN));
        accountInfo.setDisabled((Boolean) userData.get(HamboUser.DISABLED));
        accountInfo.setWapAccount((Boolean) userData.get(HamboUser.WAP_ACCOUNT));
        accountInfo.setLdapInSync((Boolean) userData.get(HamboUser.LDAP_IN_SYNC));
        accountInfo.setOfferings((Boolean) userData.get(HamboUser.OFFERINGS));
        UserContactInfo contactInfo = new UserContactInfo();
        contactInfo.setFirstName((String) userData.get(HamboUser.FIRST_NAME));
        contactInfo.setLastName((String) userData.get(HamboUser.LAST_NAME));
        contactInfo.setStreetAddress((String) userData.get(HamboUser.STREET_ADDRESS));
        contactInfo.setZipCode((String) userData.get(HamboUser.ZIP_CODE));
        contactInfo.setCity((String) userData.get(HamboUser.CITY));
        contactInfo.setState((String) userData.get(HamboUser.STATE));
        contactInfo.setCountry((String) userData.get(HamboUser.COUNTRY));
        contactInfo.setBirthday((java.sql.Date) userData.get(HamboUser.BIRTHDAY));
        contactInfo.setGender((String) userData.get(HamboUser.GENDER));
        contactInfo.setExternalEmailAddress((String) userData.get(HamboUser.EXTERNAL_EMAIL_ADDRESS));
        contactInfo.setMobileNumber((String) userData.get(HamboUser.MOBILE_NUMBER));
        return new HamboUser(userId, oid, accountInfo, contactInfo);
    }

    User buildUser(ResultSet resultSet) throws SQLException {
        String userId = resultSet.getString("userid");
        OID oid = new OID(resultSet.getBigDecimal("OId").doubleValue());
        UserAccountInfo accountInfo = new UserAccountInfo();
        accountInfo.setPassword(resultSet.getString("password"));
        accountInfo.setLanguage(resultSet.getString("language"));
        accountInfo.setTimeZone(resultSet.getString("timezn"));
        accountInfo.setCreated(resultSet.getDate("datecreated"));
        accountInfo.setLastLogin(resultSet.getDate("lastlogin"));
        accountInfo.setDisabled(resultSet.getBoolean("disabled"));
        accountInfo.setWapAccount(resultSet.getBoolean("wapsigned"));
        accountInfo.setLdapInSync(resultSet.getBoolean("ldapInSync"));
        accountInfo.setOfferings(resultSet.getBoolean("offerings"));
        UserContactInfo contactInfo = new UserContactInfo();
        contactInfo.setFirstName(resultSet.getString("firstname"));
        contactInfo.setLastName(resultSet.getString("lastname"));
        contactInfo.setStreetAddress(resultSet.getString("street"));
        contactInfo.setZipCode(resultSet.getString("zipcode"));
        contactInfo.setCity(resultSet.getString("city"));
        contactInfo.setState(resultSet.getString("province"));
        contactInfo.setCountry(resultSet.getString("country"));
        contactInfo.setBirthday(resultSet.getDate("birthday"));
        contactInfo.setGender(resultSet.getString("gender"));
        contactInfo.setExternalEmailAddress(resultSet.getString("email"));
        contactInfo.setMobileNumber(resultSet.getString("cellph"));
        return new HamboUser(userId, oid, accountInfo, contactInfo);
    }
}
