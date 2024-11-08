package hambo.community;

import java.util.*;
import java.util.Vector;
import java.sql.SQLException;
import java.sql.ResultSet;
import hambo.util.OID;
import hambo.svc.database.DBServiceManager;
import hambo.svc.database.DBConnection;
import hambo.app.core.DataAccessObject;
import hambo.community.ObjectUserCafe;
import java.util.Enumeration;
import java.sql.PreparedStatement;
import hambo.community.communityConfigObject;
import hambo.community.AdminChatObject;

/**
 * This class basically gets informations from
 * database used by the Admintool
 */
public class AdminCafeDO extends DataAccessObject {

    /**
     * Default Constructor
     */
    public AdminCafeDO() {
    }

    /**
     * Method used to fetch all the Cafe Users with a Picture
     *
     * @return a Vector of {@link ObjectCafeUser} or an empty Vector
     **/
    public Vector getAllUsersWithPictures(String whereDateClause) {
        Vector result = new Vector();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT cafe_user_uid,cafe_user_nick, cafe_user_picture FROM cafe_user WHERE cafe_user_picture=1 " + whereDateClause);
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                ObjectUserCafe aCafeUser = new ObjectUserCafe();
                aCafeUser.setuId(rs.getString("cafe_user_uid"));
                aCafeUser.setNick(rs.getString("cafe_user_nick"));
                aCafeUser.setPicture(rs.getInt("cafe_user_picture"));
                result.add(aCafeUser);
            }
        } catch (SQLException e) {
            logError("ERROR: getAllUsersWithPictures()", e);
        } finally {
            if (con != null) con.release();
        }
        return result;
    }

    /**
     * Method used to fetch all the Cafe Users who are Online
     *
     * @return a Vector of {@link ObjectCafeUserOnline} or an empty Vector
     **/
    public Vector getAllOnlineUsers() {
        Vector result = new Vector();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT cafe_user_nick,cafe_useronline_useruid,cafe_useronline_lastupdatedate,cafe_useronline_notify,cafe_useronline_device FROM cafe_user,cafe_useronline WHERE cafe_useronline_useruid=cafe_user_uid ORDER BY cafe_useronline_lastupdatedate");
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                ObjectUserCafeOnline aCafeUser = new ObjectUserCafeOnline();
                aCafeUser.setuId(rs.getString("cafe_useronline_useruid"));
                aCafeUser.setNick(rs.getString("cafe_user_nick"));
                aCafeUser.setLastUpdateDate(rs.getString("cafe_useronline_lastupdatedate"));
                aCafeUser.setDevice(rs.getInt("cafe_useronline_device"));
                aCafeUser.setNotify(String.valueOf(rs.getInt("cafe_useronline_notify")));
                result.add(aCafeUser);
            }
        } catch (SQLException e) {
            logError("ERROR: getAllOnlineUsers()", e);
        } finally {
            if (con != null) con.release();
        }
        return result;
    }

    /**
     * Method used to fetch all the Cafe Users with picture and a specific gender 
     *
     * @return a Vector of users or an empty Vector
     **/
    public Vector getAllUserWithPictureAndGender(String gender) {
        Vector vAllUser = new Vector();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT cafe_user_uid,cafe_user_nick ");
            query.append("FROM cafe_user ");
            query.append("WHERE cafe_user_gender='" + gender + "' ");
            query.append("AND cafe_user_picture=1 ");
            ResultSet rs = con.executeQuery(query.toString());
            boolean more = rs.next();
            while (more) {
                ObjectUserCafe aUser = new ObjectUserCafe();
                aUser.setuId(rs.getString("cafe_user_uid"));
                aUser.setNick(rs.getString("cafe_user_nick"));
                vAllUser.addElement(aUser);
                more = rs.next();
            }
        } catch (Exception e) {
        } finally {
            if (con != null) con.release();
        }
        return vAllUser;
    }

    /**
     * Method used to update the pictures of Todays Roomies whith selected users 
     *
     **/
    public void updateTableWithPictureOfTheDay(Vector vAllGirls, Vector vAllBoys) {
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("DELETE FROM cafe_Todays_Roomies ");
            con.executeUpdate(query.toString());
            for (Enumeration e = vAllGirls.elements(); e.hasMoreElements(); ) {
                ObjectUserCafe girl = (ObjectUserCafe) e.nextElement();
                String theFuid = girl.getuId();
                String theNick = girl.getNick();
                if (theFuid != null && !theFuid.trim().equals("") && theNick != null && !theNick.trim().equals("")) {
                    StringBuffer query1 = new StringBuffer();
                    query1.append("INSERT INTO cafe_Todays_Roomies ");
                    query1.append("(cafe_Todays_Roomies_Fuid,cafe_Todays_Roomies_Nick) ");
                    query1.append("VALUES (" + theFuid + ",'" + theNick + "')");
                    con.executeUpdate(query1.toString());
                }
            }
            for (Enumeration e = vAllBoys.elements(); e.hasMoreElements(); ) {
                ObjectUserCafe boy = (ObjectUserCafe) e.nextElement();
                String theFuid = boy.getuId();
                String theNick = boy.getNick();
                if (theFuid != null && !theFuid.trim().equals("") && theNick != null && !theNick.trim().equals("")) {
                    StringBuffer query2 = new StringBuffer();
                    query2.append("INSERT INTO cafe_Todays_Roomies ");
                    query2.append("(cafe_Todays_Roomies_Fuid,cafe_Todays_Roomies_Nick) ");
                    query2.append("VALUES (" + theFuid + ",'" + theNick + "')");
                    con.executeUpdate(query2.toString());
                }
            }
        } catch (SQLException e) {
        } finally {
            if (con != null) con.release();
        }
    }

    public void saveConfiguration(communityConfigObject co) {
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            String query = "UPDATE cafe_configuration " + "SET " + "cc_use_useguestbook = ?, " + "cc_use_sms_notification = ?, " + "cc_use_sms_forwarding = ?, " + "cc_use_roomies_of_the_day = ?, " + "cc_use_messages_archive = ?, " + "cc_use_lud_info = ?, " + "cc_use_chat = ?, " + "cc_use_mywap_homepage = ?, " + "cc_use_last_logged_in_users = ?, " + "cc_use_light_Search = ?, " + "cc_lifetime_sec = ?, " + "cc_interval_sec = ?, " + "cc_maxqueue_size = ?, " + "cc_jmsrouter = ?, " + "cc_jmstopic = ?, " + "cc_topdown = ?, " + "cc_send_sms = ?, " + "cc_receive_sms = ?, " + "cc_max_sms = ?, " + "cc_number_of_notifications = ?, " + "cc_forward_messages_to_icq = ?, " + "cc_inbox_restriction = ?, " + "cc_archive_restriction = ? ";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setBoolean(1, co.getUSE_GUESTBOOK());
            ps.setBoolean(2, co.getUSE_SMS_NOTIFICATION());
            ps.setBoolean(3, co.getUSE_SMS_FORWARDING());
            ps.setBoolean(4, co.getUSE_ROOMIES_OF_THE_DAY());
            ps.setBoolean(5, co.getUSE_MESSAGES_ARCHIVE());
            ps.setBoolean(6, co.getUSE_LUD_INFO());
            ps.setBoolean(7, co.getUSE_CHAT());
            ps.setBoolean(8, co.getUSE_MYWAP_HOMEPAGE());
            ps.setBoolean(9, co.getUSE_LAST_LOGGED_IN_USERS());
            ps.setBoolean(10, co.getUSE_LIGHT_SEARCH());
            ps.setInt(11, co.getLIFETIME_SEC());
            ps.setInt(12, co.getINTERVAL_SEC());
            ps.setInt(13, co.getMAXQUEUE_SIZE());
            ps.setString(14, co.getJMSROUTER());
            ps.setString(15, co.getJMSTOPIC());
            ps.setBoolean(16, co.getTOPDOWN());
            ps.setString(17, co.getSEND_SMS());
            ps.setString(18, co.getRECEIVE_SMS());
            ps.setInt(19, co.getMAX_SMS());
            ps.setInt(20, co.getNUMBER_OF_NOTIFICATIONS());
            ps.setString(21, co.getFORWARD_MESSAGES_TO_ICQ());
            ps.setInt(22, co.getINBOX_RESTRICTION());
            ps.setInt(23, co.getARCHIVE_RESTRICTION());
            ps.executeUpdate();
        } catch (SQLException e) {
        } finally {
            if (con != null) con.release();
        }
    }

    public communityConfigObject getConfiguration() {
        DBConnection con = null;
        communityConfigObject co = new communityConfigObject();
        try {
            con = DBServiceManager.allocateConnection();
            String query = "SELECT " + "cc_use_useguestbook, " + "cc_use_sms_notification, " + "cc_use_sms_forwarding, " + "cc_use_roomies_of_the_day, " + "cc_use_messages_archive, " + "cc_use_lud_info, " + "cc_use_chat, " + "cc_use_mywap_homepage, " + "cc_use_last_logged_in_users, " + "cc_use_light_Search, " + "cc_lifetime_sec, " + "cc_interval_sec, " + "cc_maxqueue_size, " + "cc_jmsrouter, " + "cc_jmstopic, " + "cc_topdown, " + "cc_send_sms, " + "cc_receive_sms, " + "cc_max_sms, " + "cc_number_of_notifications, " + "cc_forward_messages_to_icq, " + "cc_inbox_restriction, " + "cc_archive_restriction " + "FROM cafe_configuration ";
            ResultSet rs = con.executeQuery(query);
            if (rs.next()) {
                co.setUSE_GUESTBOOK(rs.getBoolean("cc_use_useguestbook"));
                co.setUSE_SMS_NOTIFICATION(rs.getBoolean("cc_use_sms_notification"));
                co.setUSE_SMS_FORWARDING(rs.getBoolean("cc_use_sms_forwarding"));
                co.setUSE_ROOMIES_OF_THE_DAY(rs.getBoolean("cc_use_roomies_of_the_day"));
                co.setUSE_MESSAGES_ARCHIVE(rs.getBoolean("cc_use_messages_archive"));
                co.setUSE_LUD_INFO(rs.getBoolean("cc_use_lud_info"));
                co.setUSE_CHAT(rs.getBoolean("cc_use_chat"));
                co.setUSE_MYWAP_HOMEPAGE(rs.getBoolean("cc_use_mywap_homepage"));
                co.setUSE_LAST_LOGGED_IN_USERS(rs.getBoolean("cc_use_last_logged_in_users"));
                co.setUSE_LIGHT_SEARCH(rs.getBoolean("cc_use_light_Search"));
                co.setLIFETIME_SEC(rs.getInt("cc_lifetime_sec"));
                co.setINTERVAL_SEC(rs.getInt("cc_interval_sec"));
                co.setMAXQUEUE_SIZE(rs.getInt("cc_interval_sec"));
                co.setJMSROUTER(rs.getString("cc_jmsrouter"));
                co.setJMSTOPIC(rs.getString("cc_jmstopic"));
                co.setTOPDOWN(rs.getBoolean("cc_topdown"));
                co.setSEND_SMS(rs.getString("cc_send_sms"));
                co.setRECEIVE_SMS(rs.getString("cc_receive_sms"));
                co.setMAX_SMS(rs.getInt("cc_max_sms"));
                co.setNUMBER_OF_NOTIFICATIONS(rs.getInt("cc_number_of_notifications"));
                co.setFORWARD_MESSAGES_TO_ICQ(rs.getString("cc_forward_messages_to_icq"));
                co.setINBOX_RESTRICTION(rs.getInt("cc_inbox_restriction"));
                co.setARCHIVE_RESTRICTION(rs.getInt("cc_archive_restriction"));
            }
        } catch (SQLException e) {
        } finally {
            if (con != null) con.release();
        }
        return co;
    }

    /**
     * Method used to get all the chatrooms from database
     
     *@return List containing AdminChatObject or an empty vector
     */
    public List getChatRooms() {
        DBConnection con = null;
        List Rooms = new ArrayList();
        try {
            con = DBServiceManager.allocateConnection();
            String query = "SELECT cafe_Chat_Category_id, cafe_Chat_Category_icon, " + "cafe_chatroom_name, cafe_chatroom_stringid, cafe_chatroom_category, " + "cafe_chatroom_id " + "FROM cafe_Chat_Category, cafe_Chatroom " + "WHERE cafe_Chat_Category_id = cafe_chatroom_category ";
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            boolean more = rs.next();
            while (more) {
                AdminChatObject ao = new AdminChatObject();
                ao.setcafe_chat_category_id(rs.getInt("cafe_Chat_Category_id"));
                ao.setcafe_chatroom_category(rs.getInt("cafe_chatroom_category"));
                ao.setchatRoomName(rs.getString("cafe_chatroom_name"));
                ao.setcafe_Chat_Category_icon(rs.getString("cafe_Chat_Category_icon"));
                ao.setcafe_chatroom_id(rs.getInt("cafe_chatroom_id"));
                ao.setcafe_chatroom_stringid(rs.getString("cafe_chatroom_stringid"));
                Rooms.add(ao);
                more = rs.next();
            }
        } catch (SQLException e) {
        } finally {
            if (con != null) con.release();
        }
        return Rooms;
    }

    /**
     * Method used to save a new chatroom in database
     *@param String name is the name of the chatroom
     *@param String icon is the name of the picture of the chatroom
     *@return int wich is the category id of the saved chatroom
     **/
    public int saveRoom(String name, String icon, String stringid) {
        DBConnection con = null;
        int categoryId = -1;
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            String query = "INSERT INTO cafe_Chat_Category " + "(cafe_Chat_Category_pid,cafe_Chat_Category_name, cafe_Chat_Category_icon) " + "VALUES (null,?,?) ";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, icon);
            ps.executeUpdate();
            query = "SELECT cafe_Chat_Category_id FROM cafe_Chat_Category " + "WHERE cafe_Chat_Category_name=? ";
            ps = con.prepareStatement(query);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                query = "INSERT INTO cafe_Chatroom (cafe_chatroom_category, cafe_chatroom_name, cafe_chatroom_stringid) " + "VALUES (?,?,?) ";
                ps = con.prepareStatement(query);
                ps.setInt(1, rs.getInt("cafe_Chat_Category_id"));
                categoryId = rs.getInt("cafe_Chat_Category_id");
                ps.setString(2, name);
                ps.setString(3, stringid);
                ps.executeUpdate();
            }
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException sqle) {
            }
        } finally {
            if (con != null) con.release();
        }
        return categoryId;
    }

    /**
     * Method used to remove a chatroom from database
     *@param int thisRoom is the category id of the chatroom
     *
     **/
    public void removeRoom(int thisRoom) {
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            String query = "DELETE FROM cafe_Chat_Category WHERE cafe_Chat_Category_id=? ";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, thisRoom);
            ps.executeUpdate();
            query = "DELETE FROM cafe_Chatroom WHERE cafe_chatroom_category=? ";
            ps = con.prepareStatement(query);
            ps.setInt(1, thisRoom);
            ps.executeUpdate();
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException sqle) {
            }
        } finally {
            if (con != null) con.release();
        }
    }

    /**
     * Method used to check if a name of a chatroom is taken since the cafe_chatroom_stringid must be unique
     *@param String name is the name of the chatroom
     *
     *@return int wich is > 0 if the name is taken, else 0
     **/
    public int checkName(String name, String stringid) {
        DBConnection con = null;
        int result = 0;
        try {
            con = DBServiceManager.allocateConnection();
            String query = "SELECT count (*) as nb FROM cafe_Chatroom " + "WHERE cafe_chatroom_stringid=? ";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, stringid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                result = rs.getInt("nb");
            }
            query = "SELECT count (*) as nb FROM cafe_Chatroom " + "WHERE cafe_chatroom_name=? ";
            ps = con.prepareStatement(query);
            ps.setString(1, name);
            ResultSet rs1 = ps.executeQuery();
            if (rs1.next()) {
                result += rs1.getInt("nb");
            }
        } catch (SQLException e) {
        } finally {
            if (con != null) con.release();
        }
        return result;
    }
}
