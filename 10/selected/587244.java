package hambo.community;

import hambo.internationalization.TranslationServiceManager;
import java.util.Vector;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import hambo.util.OID;
import hambo.util.StringUtil;
import hambo.svc.database.DBServiceManager;
import hambo.svc.database.DBConnection;
import hambo.app.core.DataAccessObject;

/**
 * This class basically gets Guestbook informations from
 * database and feeds it to GuestbookItem Object
 */
public class GuestbookDO extends DataAccessObject {

    /** Cafe User if of the Owner of the Guestbook */
    private String owner_fuid;

    /** Object Guestbook related to this GuestbookDO  */
    private ObjectGuestbook theItem = null;

    /**
     * Default Constructor: initialize the attribut owner of the Guestbook and setup the ObjectGuestbook theItem
     */
    public GuestbookDO(String owner_fuid) {
        this.owner_fuid = owner_fuid;
        this.theItem = initGuestBook();
    }

    /**
     * Private Method to initialize the Guestbook: setup the prefences for this Guestbook
     * Welcome Text,Visiblity, Notification SMS, Notification Email,Email for Notification,Owner Language,Owner Cellphone,Owner USerid,Owner OID
     *
     * @return the GuestbookObject initialized
     */
    private ObjectGuestbook initGuestBook() {
        ObjectGuestbook guestbook = new ObjectGuestbook(owner_fuid);
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT cafe_guestbook_conf_fuid FROM cafe_guestbook_conf WHERE cafe_guestbook_conf_fuid=" + owner_fuid);
            ResultSet rsCheck = con.executeQuery(query.toString());
            if (!rsCheck.next()) {
                query = new StringBuffer();
                query.append("INSERT INTO cafe_guestbook_conf ");
                query.append("(cafe_guestbook_conf_fuid,cafe_guestbook_conf_welcome,cafe_guestbook_conf_visible,cafe_guestbook_conf_email,cafe_guestbook_conf_nbsms,cafe_guestbook_conf_nbemail) ");
                String lang = TranslationServiceManager.DEFAULT_LANGUAGE;
                String welcomeMessage = TranslationServiceManager.translateTag("cfdefaultwelcomemessage", lang);
                query.append("VALUES (" + owner_fuid + ",'" + welcomeMessage + "',1,null,0,0)");
                con.executeUpdate(query.toString());
            }
            query = new StringBuffer();
            query.append("SELECT cafe_guestbook_conf_welcome,cafe_guestbook_conf_visible,");
            query.append("cafe_guestbook_conf_email,");
            query.append("cafe_guestbook_conf_nbemail,cafe_guestbook_conf_nbsms, ");
            query.append("language,cellph,userid,oid ");
            query.append("FROM cafe_guestbook_conf,cafe_user,user_useraccount ");
            query.append("WHERE cafe_user_uid=" + owner_fuid + " AND cafe_user_id=userid ");
            query.append("AND cafe_guestbook_conf_fuid=" + owner_fuid);
            ResultSet rs = con.executeQuery(query.toString());
            if (rs.next()) {
                guestbook.setWelcomeText(rs.getString("cafe_guestbook_conf_welcome"));
                guestbook.setIsVisble(rs.getInt("cafe_guestbook_conf_visible"));
                guestbook.setEmail(rs.getString("cafe_guestbook_conf_email"));
                guestbook.setEmailNb(rs.getInt("cafe_guestbook_conf_nbemail"));
                guestbook.setSMSNb(rs.getInt("cafe_guestbook_conf_nbsms"));
                guestbook.setOwner_language(rs.getString("language"));
                guestbook.setOwner_cell(rs.getString("cellph"));
                guestbook.setOwner_userid(rs.getString("userid"));
                guestbook.setOwner_OID(new OID(rs.getString("oid")));
            }
        } catch (SQLException e) {
            logError("ERROR: initGuestBook()", e);
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return guestbook;
    }

    /**
     * Method used to get the Welcome Message of the Current Guestbook
     * @return the Welcome Message or an empty String
     **/
    public String getWelcomeMessage() {
        String welcomeMessage = this.theItem.getWelcomeText();
        return welcomeMessage;
    }

    /**
     * Method used to check if the Guestbook is visible
     * 
     * @return 1 if visible or 0 if NOT visible
     **/
    public int getIsVisible() {
        return this.theItem.getIsVisble();
    }

    /**
     * Method that return if the owner want an email each time someone writes in his guestbook
     * @return the email where to send or an empty String
     **/
    public String getEmail() {
        return this.theItem.getEmail();
    }

    /**
     * Method that return if the owner want a SMS each time someone writes in his guestbook
     * @return number Maximal of SMS that the User wants to receive per day
     **/
    public int getSMSNb() {
        return this.theItem.getSMSNb();
    }

    /**
     * Method that return if the owner want a Email each time someone writes in his guestbook
     * @return number Maximal of Email that the User wants to receive per day
     **/
    public int getEmailNb() {
        return this.theItem.getEmailNb();
    }

    /**
     * Method used to fetch the OID of the Owner of the Guestbook
     * @return the OID of the Owner of the Guestbook
     **/
    public OID getOwnerOID() {
        return this.theItem.getOwner_OID();
    }

    /**
     * Method used to fetch the Cellphone of the Owner of the Guestbook
     * @return the Cellphone of the Owner of the Guestbook or null
     **/
    public String getOwnerCell() {
        return this.theItem.getOwner_cell();
    }

    /**
     * Method used to fetch the language of the Owner of the Guestbook
     * @return the language of the Owner of the Guestbook or null
     **/
    public String getOwnerLanguage() {
        return this.theItem.getOwner_language();
    }

    /**
     * Method used to fetch the Hambo User id of the Owner of the Guestbook
     * @return the the Hambo User id of the Owner of the Guestbook or null
     **/
    public String getOwnerUserId() {
        return this.theItem.getOwner_userid();
    }

    /**
     * Method used to save the preferences of a user about his guestbook
     *
     * @param user_uid the Cafe Userid of the Owner of the Guestbook
     * @param theWelcomeText the Welcome Text (Should already been encoded)
     * @param email the email adress where to send the notifications
     * @param visible is the flag: 1 guestbook visible to everybody, 0 not visible
     * @param nbemail the number maximal per day of email receive as notification when someone write in the Guestbook
     * @param nbsms the number maximal per day of SMS receive as notification when someone write in the Guestbook
     * @return  0 if the preferences have been saved else return 1
     **/
    public int savePreferences(String user_uid, String theWelcomeText, String email, String visible, String nbemail, String nbsms) {
        int flag = 0;
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            StringBuffer query = new StringBuffer();
            query.append("DELETE FROM cafe_guestbook_conf ");
            query.append("WHERE cafe_guestbook_conf_fuid=" + user_uid);
            con.executeUpdate(query.toString());
            query = new StringBuffer();
            query.append("INSERT INTO cafe_guestbook_conf ");
            query.append("(cafe_guestbook_conf_fuid,cafe_guestbook_conf_welcome,cafe_guestbook_conf_visible,cafe_guestbook_conf_email,cafe_guestbook_conf_nbsms,cafe_guestbook_conf_nbemail) ");
            query.append("VALUES (" + user_uid + ",'" + theWelcomeText + "'," + visible + ",'" + email + "'," + nbsms + "," + nbemail + ")");
            con.executeUpdate(query.toString());
            con.commit();
            con.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException sqle) {
            }
            logError("ERROR: savePreferences(" + user_uid + "," + theWelcomeText + "," + email + "," + visible + "," + nbemail + "," + nbsms + ")", e);
            flag = 1;
        } finally {
            if (con != null) {
                try {
                    con.reset();
                } catch (SQLException e) {
                }
                con.release();
            }
        }
        return flag;
    }

    /**
     * Method used to write in the guestbook, user_uid WRITES in the guest book
     *
     * @param user_uid is the Cafe User id of the User WRITTING in the Guestbook
     * @param theText i sthe Text to write in the Guestbook (Should already been encoded)
     * @param smile is the smiley id choosen by the user who write the Message (deprecated)
     * @param nick is the Nickname of the writer
     * @return  0 if the Message has been written in the Guestbook else return 1
     **/
    public int writeInGuestBook(String user_uid, String theText, String nick) {
        int flag = 0;
        if (theText != null) {
            char c = 39;
            theText = StringUtil.replace(theText, (new Character(c)).toString(), "&#39;");
        }
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            if (theText != null) {
                char c = 39;
                theText = StringUtil.replace(theText, (new Character(c)).toString(), "&#39;");
            }
            StringBuffer query = new StringBuffer();
            query.append("INSERT INTO cafe_guestbook ");
            query.append("(cafe_guestbook_to_fuid,cafe_guestbook_from_fuid,cafe_guestbook_from_nick,cafe_guestbook_date,cafe_guestbook_smile_id,cafe_guestbook_msg) ");
            query.append("VALUES (" + owner_fuid + "," + user_uid + ",'" + nick + "',getdate(),0,'" + theText + "')");
            con.executeUpdate(query.toString());
        } catch (SQLException e) {
            logError("ERROR: writeInGuestBook(" + user_uid + "," + theText + "," + nick + ")", e);
            flag = 1;
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return flag;
    }

    /**
     * Method used to fetch all the messages of the Guestbook
     *
     * @return a Vector of {@link ObjectGuestbookItem} or an empty Vector
     **/
    public Vector getAllMessages() {
        Vector result = new Vector();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT cafe_guestbook_id,cafe_guestbook_to_fuid,cafe_guestbook_from_fuid,cafe_guestbook_from_nick,cafe_guestbook_date,cafe_guestbook_smile_id,cafe_guestbook_msg ");
            query.append("FROM cafe_guestbook ");
            query.append("WHERE cafe_guestbook_to_fuid=" + owner_fuid + " ORDER BY cafe_guestbook_date DESC ");
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                ObjectGuestbookItem anItem = new ObjectGuestbookItem();
                anItem.setId(rs.getString("cafe_guestbook_id"));
                anItem.setText(rs.getString("cafe_guestbook_msg"));
                anItem.setFromFuid(rs.getString("cafe_guestbook_from_fuid"));
                anItem.setFromNick(rs.getString("cafe_guestbook_from_nick"));
                anItem.setDate(rs.getString("cafe_guestbook_date"));
                anItem.setSmile(rs.getString("cafe_guestbook_smile_id"));
                result.add(anItem);
            }
        } catch (SQLException e) {
            logError("ERROR: getAllMessages()", e);
        } finally {
            if (con != null) con.release();
        }
        return result;
    }

    /**
     * Method used to remove a message in the guestbook
     *
     * @param msgid is the Message id to remove
     * @return  0 if the Message has been deleted from the Guestbook else return 1
     **/
    public int removeFromGuestBook(String msgid) {
        int flag = 0;
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("DELETE FROM cafe_guestbook WHERE cafe_guestbook_id=" + msgid);
            con.executeUpdate(query.toString());
        } catch (SQLException e) {
            logError("ERROR: removeFromGuestBook(" + msgid + ")", e);
            flag = 1;
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return flag;
    }

    /**
     * Method used to find how many SMS have been sent to the Owner of the Guestbook
     *
     * @return the number of SMS sent during the day
     */
    public int findHowManySMSHaveBeenSendTo() {
        int nb = 2000;
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT cafe_gu_notification_nbsms FROM cafe_gu_notification WHERE cafe_gu_notification_fuid=" + owner_fuid);
            ResultSet rs = con.executeQuery(query.toString());
            if (rs.next()) {
                nb = rs.getInt("cafe_gu_notification_nbsms");
            } else {
                nb = 0;
            }
        } catch (SQLException e) {
            logError("ERROR: findHowManySMSHaveBeenSendTo()", e);
        } finally {
            if (con != null) con.release();
        }
        return nb;
    }

    /**
     * Method used to find how many Email have been sent to the Owner of the Guestbook
     *
     * @return the number of Email sent during the day
     */
    public int findHowManyEmailHaveBeenSendTo() {
        int nb = 2000;
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT cafe_gu_notification_nbemail FROM cafe_gu_notification WHERE cafe_gu_notification_fuid=" + owner_fuid);
            ResultSet rs = con.executeQuery(query.toString());
            if (rs.next()) {
                nb = rs.getInt("cafe_gu_notification_nbemail");
            } else {
                nb = 0;
            }
        } catch (SQLException e) {
            logError("ERROR: findHowManyEmailHaveBeenSendTo()", e);
        } finally {
            if (con != null) con.release();
        }
        return nb;
    }

    /**
     * Method used to increase the reminder about: how many Email and SMS have been sent during the day to the owner of the Guestbook
     *
     * @param oldNumberSMS number of SMS already sent
     * @param oldNumberEmail number of Email already sent
     * @param increaseSMS must I increase the number of SMS sent today?
     * @param increaseEmail must I increase the number of Email sent today?
     */
    public void increaseNumberOfSMSSent(int oldNumberSMS, int oldNumberEmail, boolean increaseSMS, boolean increaseEmail) {
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            int newNumberSMS = 1;
            if (increaseSMS) {
                newNumberSMS = newNumberSMS + oldNumberSMS;
            } else {
                newNumberSMS = oldNumberSMS;
            }
            int newNumberEmail = 1;
            if (increaseEmail) {
                newNumberEmail = newNumberEmail + oldNumberEmail;
            } else {
                newNumberEmail = oldNumberEmail;
            }
            StringBuffer query = new StringBuffer();
            if (oldNumberSMS == 0 && oldNumberEmail == 0) {
                query.append("DELETE FROM cafe_gu_notification WHERE cafe_gu_notification_fuid=" + owner_fuid);
                con.executeUpdate(query.toString());
                query = new StringBuffer();
                query.append("INSERT INTO cafe_gu_notification (cafe_gu_notification_fuid,cafe_gu_notification_nbsms,cafe_gu_notification_nbemail) VALUES (" + owner_fuid + "," + newNumberSMS + "," + newNumberEmail + ")");
                con.executeUpdate(query.toString());
            } else {
                query.append("UPDATE cafe_gu_notification SET cafe_gu_notification_nbsms=" + newNumberSMS + ",cafe_gu_notification_nbemail=" + newNumberEmail + " WHERE cafe_gu_notification_fuid=" + owner_fuid);
                con.executeUpdate(query.toString());
            }
        } catch (SQLException e) {
            logError("ERROR: increaseNumberOfSMSSent(" + oldNumberSMS + "," + oldNumberEmail + "," + increaseSMS + "," + increaseEmail + ")", e);
        } finally {
            if (con != null) {
                con.release();
            }
        }
    }

    /**
     * Method used to count the number of new guestbook items in guestbook 
     *
     * @param userUid is the id of the guestbook owner 
     */
    public int countNewGuestbookEntries(String userUid) {
        DBConnection con = null;
        int result = 0;
        try {
            con = DBServiceManager.allocateConnection();
            String query = "SELECT count(cafe_guestbook_to_fuid) as nb " + "FROM cafe_guestbook " + "WHERE cafe_guestbook_to_fuid = ? " + "AND cafe_guestbook_msgread = ? ";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, Integer.parseInt(userUid));
            ps.setInt(2, 0);
            ResultSet rs = con.executeQuery(ps, null);
            if (rs.next()) {
                result = Integer.parseInt(rs.getString("nb"));
            }
        } catch (SQLException e) {
            logError("countNewGuestbookEntries(" + userUid + ")");
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return result;
    }

    /**
     * Method used to set the msg read indicator to 1 
     *
     * @param userUid is the id of the guestbook owner 
     */
    public void setMsgIndicatorToOne(String userUid) {
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            String query = "UPDATE cafe_guestbook " + "SET cafe_guestbook_msgread = ? " + "WHERE cafe_guestbook_to_fuid = ? " + "AND cafe_guestbook_msgread = ? ";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, 1);
            ps.setInt(2, Integer.parseInt(userUid));
            ps.setInt(3, 0);
            con.executeUpdate(ps, null);
        } catch (SQLException e) {
            logError("setMsgIndicatorToOne(" + userUid + ")");
        } finally {
            if (con != null) {
                con.release();
            }
        }
    }
}
