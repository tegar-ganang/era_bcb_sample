package hambo.wtc.category;

import java.util.Vector;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Connection;
import hambo.wtc.util.MediaErrorMessages;
import hambo.wtc.util.MediaVariables;
import hambo.wtc.util.MediaToolBox;
import hambo.wtc.media.ObjectIcon;
import hambo.wtc.media.ObjectRingTone;
import hambo.app.core.DataAccessObject;
import hambo.svc.database.DBUtil;
import hambo.svc.database.*;

/**
 * The class CategoryHandler takes care of the request done by the presentation layer in the RT & I application. <BR>
 * It manages the fetching of both categories and the content held within the categories. <BR>
 *
 */
public class CategoryHandler extends DataAccessObject {

    /** Media type used in the Category */
    private String mediatype = null;

    /** Max of item in the "Most Popular media list" */
    private int MAX_TOP = 5;

    /** Max of item in the "New media list" */
    private int MAX_NEW = 5;

    /** Max of item in the "Feature media list" */
    private int MAX_FEATURE = 5;

    /** Order of the Request */
    public static final int ORDER_BY_ALFABETHICAL = 1;

    public static final int ORDER_BY_TOP_TEN = 2;

    public static final int ORDER_BY_NEW = 3;

    public static final int ORDER_BY_FEATURE = 4;

    /** Database to use */
    private String database = "wapdb";

    private DBConnection theConnection = null;

    /**
     * Basic constructor<BR>
     * Setup the media type of this category
     * @param mediatype Media type used in the Category
     */
    public CategoryHandler(String mediatype) {
        this.mediatype = mediatype;
    }

    /**
     * Method used to get all the categories from one gallery.<BR>
     *  - If the user_id is NULL then it will use the gallery provided<BR>
     *  - If not then it the user_gallery is used since the request comes from a user. 
     * 
     * @param userid the userid of the Owner of the Categories that we will fetch
     * @return a Vector of {@link CategoryObject} or an empty Vector
     */
    public Vector fetchUsersCategories(String userid) {
        Vector vCategories = new Vector();
        if (userid != null && !userid.trim().equals("")) {
            try {
                theConnection = DBServiceManager.allocateConnection(database);
                String query = "SELECT ";
                query += "DISTINCT user_gallery.category, ";
                query += "count(user_gallery.reference) AS nb ";
                query += "FROM user_gallery,categories ";
                query += "WHERE ";
                query += "user_gallery.data_type='" + mediatype + "' ";
                query += "AND user_gallery.user_id='" + userid + "' ";
                query += "AND " + DBUtil.getQueryJoin(theConnection, "user_gallery.category", "categories.category") + " ";
                query += "GROUP BY user_gallery.category ";
                query += "HAVING user_gallery.user_id='" + userid + "' ";
                query += "UNION ";
                query += "SELECT categories.category,0 AS nb ";
                query += "FROM categories ";
                query += "WHERE categories.user_id='" + userid + "' ";
                query += "AND categories.category ";
                query += "NOT IN (";
                query += " SELECT DISTINCT user_gallery.category ";
                query += " FROM user_gallery ";
                query += " WHERE user_gallery.data_type='" + mediatype + "' ";
                query += " AND user_gallery.user_id='" + userid + "'";
                query += ") ";
                query += "ORDER BY nb DESC ";
                PreparedStatement stm = theConnection.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    String categoryname = rs.getString("category");
                    int count = rs.getInt("nb");
                    vCategories.add(new CategoryObject(categoryname, count));
                }
            } catch (SQLException e) {
                vCategories = new Vector();
            } finally {
                if (theConnection != null) theConnection.release();
            }
        }
        return vCategories;
    }

    /**
     * Method use to fetch all categories from a specific gallery and not the user_gallery
     *
     * @param gallery the gallery name
     * @return a Vector of {@link CategoryObject} or an empty Vector
     */
    public Vector fetchCategories(String gallery) {
        Vector vCategories = new Vector();
        if (gallery != null && !gallery.trim().equals("")) {
            try {
                theConnection = DBServiceManager.allocateConnection(database);
                String query = "SELECT category , count(reference) as number ";
                query += "FROM  " + gallery + " ";
                query += "WHERE data_type='" + mediatype + "' ";
                query += "AND category!='No category' ";
                query += "AND category!='All categories' ";
                query += "GROUP by category";
                System.out.println(query);
                PreparedStatement stm = theConnection.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
                while (rs.next()) {
                    String categoryname = rs.getString("category");
                    vCategories.add(new CategoryObject(categoryname, rs.getInt("number")));
                }
            } catch (SQLException e) {
                vCategories = new Vector();
            } finally {
                if (theConnection != null) {
                    theConnection.release();
                }
            }
        }
        return vCategories;
    }

    /**
     * Method used when a user wants to create a new category in there my gallery. 
     * The category will be created if the name of the new category is unique
     *
     * @param categoryname the name of the New Category
     * @param userid the userid owner of the Gallery where we will create the new Category
     * @return a {@link MediaErrors} Message
     */
    public String addCategory(String categoryname, String userid) {
        String res = MediaErrorMessages.ERROR_DATABASE;
        if (categoryname != null && !categoryname.trim().equals("") && userid != null && !userid.trim().equals("")) {
            try {
                theConnection = DBServiceManager.allocateConnection("wapdb");
                String query = "SELECT category ";
                query += "FROM categories ";
                query += "WHERE gallery='user_gallery' ";
                query += "AND user_id='" + userid + "' ";
                query += "AND category='" + categoryname + "'";
                PreparedStatement stm = theConnection.prepareStatement(query);
                ResultSet rs = stm.executeQuery();
                if (rs.next()) {
                    res = MediaErrorMessages.NAME_ALREADY_TAKEN;
                } else {
                    char c = 39;
                    categoryname = MediaToolBox.replace(categoryname, (new Character(c)).toString(), "&#39;");
                    query = "INSERT INTO categories ";
                    query += "(gallery,category,user_id) ";
                    query += "VALUES ('user_gallery','" + categoryname + "','" + userid + "')";
                    PreparedStatement stm2 = theConnection.prepareStatement(query);
                    stm2.executeUpdate();
                    res = MediaErrorMessages.ACTION_DONE_CATEGORY_ADDED;
                }
            } catch (SQLException e) {
                res = MediaErrorMessages.ERROR_DATABASE;
            } finally {
                if (theConnection != null) theConnection.release();
            }
        } else {
            res = MediaErrorMessages.EMPTY_PARAM;
        }
        return res;
    }

    /**
     * Method used to delete a Category in the user_gallery table
     *
     * @param categoryname Category Name to delete
     * @param categoryname user_id of the owner of the Category to delete
     * @return a {@link MediaErrorMessages} Message
     */
    public String deleteCategory(String categoryname, String userid) {
        String res = MediaErrorMessages.ERROR_DATABASE;
        if (categoryname != null && !categoryname.trim().equals("") && userid != null && !userid.trim().equals("")) {
            try {
                char c = 39;
                categoryname = MediaToolBox.replace(categoryname, (new Character(c)).toString(), "&#39;");
                theConnection = DBServiceManager.allocateConnection(database);
                theConnection.setAutoCommit(false);
                String query = "DELETE FROM categories ";
                query += "WHERE category='" + categoryname + "' ";
                query += "AND gallery='user_gallery' ";
                query += "AND user_id='" + userid + "'";
                PreparedStatement state = theConnection.prepareStatement(query);
                state.executeUpdate();
                query = "DELETE FROM user_gallery ";
                query += "WHERE category='" + categoryname + "' ";
                query += "AND user_id='" + userid + "'";
                PreparedStatement state2 = theConnection.prepareStatement(query);
                state2.executeUpdate();
                theConnection.commit();
                res = MediaErrorMessages.ACTION_DONE;
            } catch (SQLException e) {
                if (theConnection != null) {
                    try {
                        theConnection.rollback();
                    } catch (SQLException ex) {
                    }
                }
                res = MediaErrorMessages.ERROR_DATABASE;
            } finally {
                if (theConnection != null) {
                    try {
                        theConnection.setAutoCommit(true);
                    } catch (SQLException ex) {
                    }
                    theConnection.release();
                }
            }
        } else {
            return MediaErrorMessages.EMPTY_PARAM;
        }
        return res;
    }

    /**
     * This is the method used to get all the media from a category <BR>
     * If the user_id is NULL then it will use the gallery provided<BR>
     * If not then it the user_gallery is used since the request comes from a user. <BR>
     * The mediatype is suppied in order to determin what table to fetch the media from. <BR>
     * 
     * @param gallery ?? Must ask Linda!
     * @param categoryname the Category name where we must fetch the data from
     * @param orderby the Order By Specification {@link MediaVariables}
     * @param userid the userid of the Owner of the Category where we must fetch the data from
     *
     * return a Vector of {@link ObjectIcon} or {@link ObjectRingTone} according to the mediatypeof the CategoryHandler or an empty Vector
     */
    public Vector getAllFromCategory(String gallery, String categoryname, int orderby, String userid) {
        Vector mediavector = new Vector();
        if (categoryname != null && gallery != null) {
            String table = "";
            if (mediatype.equals(MediaVariables.MEDIA_TYPE_RINGTONE)) {
                table = MediaVariables.TABLE_RINGTONE;
            } else if (mediatype.equals(MediaVariables.MEDIA_TYPE_ICON)) {
                table = MediaVariables.TABLE_ICON;
            } else {
                return mediavector;
            }
            String order = null;
            switch(orderby) {
                case ORDER_BY_ALFABETHICAL:
                    order = "ORDER BY name";
                    break;
                case ORDER_BY_TOP_TEN:
                    order = "ORDER BY " + table + ".counter DESC";
                    break;
                case ORDER_BY_NEW:
                    order = "ORDER BY " + table + ".in_date DESC, lower(" + table + ".name)";
                    break;
                default:
                    order = "ORDER BY name";
                    break;
            }
            try {
                theConnection = DBServiceManager.allocateConnection(database);
                String query = "SELECT name, " + table + ".reference ";
                query += "FROM " + table + ",user_gallery ";
                query += "WHERE category='" + categoryname + "' ";
                query += "AND data_type='" + mediatype + "' ";
                query += "AND " + table + ".reference = user_gallery.reference ";
                query += "AND user_id='" + userid + "' " + order;
                PreparedStatement state = theConnection.prepareStatement(query);
                ResultSet rs = state.executeQuery();
                while (rs.next()) {
                    String name = rs.getString("name");
                    int ref = rs.getInt("reference");
                    if (mediatype.equals(MediaVariables.MEDIA_TYPE_RINGTONE)) {
                        ObjectRingTone ort = new ObjectRingTone(name, ref);
                        mediavector.add(ort);
                    } else if (mediatype.equals(MediaVariables.MEDIA_TYPE_ICON)) {
                        ObjectIcon oi = new ObjectIcon(name, ref);
                        mediavector.add(oi);
                    }
                }
            } catch (SQLException e) {
                mediavector = new Vector();
            } finally {
                if (theConnection != null) theConnection.release();
            }
        }
        return mediavector;
    }

    /**
     * This is the method used to get all the media from a category <BR>
     * The mediatype is suppied in order to determin what table to fetch the media from. <BR>
     * 
     * @param gallery ?? Must ask Linda!
     * @param categoryname the Category name where we must fetch the data from
     * @param orderby the Order By Specification {@link MediaVariables}
     *
     * return a Vector of {@link ObjectIcon} or {@link ObjectRingTone} according to the mediatypeof the CategoryHandler or an empty Vector
     */
    public Vector getAllFromCategory(String gallery, String categoryname, int orderby) {
        Vector mediavector = new Vector();
        if (categoryname != null && gallery != null) {
            String table = "";
            if (mediatype.equals(MediaVariables.MEDIA_TYPE_RINGTONE)) {
                table = MediaVariables.TABLE_RINGTONE;
            } else if (mediatype.equals(MediaVariables.MEDIA_TYPE_ICON)) {
                table = MediaVariables.TABLE_ICON;
            } else {
                return mediavector;
            }
            String order = null;
            switch(orderby) {
                case ORDER_BY_ALFABETHICAL:
                    order = "and category='" + categoryname + "' order by " + table + ".name";
                    break;
                case ORDER_BY_TOP_TEN:
                    order = "order by " + table + ".counter DESC";
                    break;
                case ORDER_BY_NEW:
                    order = "order by " + table + ".in_date DESC, lower(" + table + ".name)";
                    break;
                default:
                    order = "order by " + table + ".name";
                    break;
            }
            try {
                theConnection = DBServiceManager.allocateConnection(database);
                String query = "SELECT name, " + table + ".reference ";
                query += "FROM " + table + "," + gallery + " ";
                query += "WHERE data_type='" + mediatype + "' ";
                query += "AND " + table + ".reference = " + gallery + ".reference ";
                query += order;
                PreparedStatement state = theConnection.prepareStatement(query);
                if (orderby == ORDER_BY_NEW) {
                    state.setMaxRows(MAX_NEW);
                }
                if (orderby == ORDER_BY_TOP_TEN) {
                    state.setMaxRows(MAX_TOP);
                }
                if (orderby == ORDER_BY_FEATURE) {
                    state.setMaxRows(MAX_FEATURE);
                }
                ResultSet rs = state.executeQuery();
                while (rs.next()) {
                    String name = rs.getString("name");
                    int ref = rs.getInt("reference");
                    if (mediatype.equals(MediaVariables.MEDIA_TYPE_RINGTONE)) {
                        ObjectRingTone ort = new ObjectRingTone(name, ref);
                        mediavector.add(ort);
                    } else if (mediatype.equals(MediaVariables.MEDIA_TYPE_ICON)) {
                        ObjectIcon oi = new ObjectIcon(name, ref);
                        mediavector.add(oi);
                    }
                }
            } catch (SQLException e) {
                mediavector = new Vector();
            } finally {
                if (theConnection != null) theConnection.release();
            }
        }
        return mediavector;
    }

    /**
     * Method used to fetch all the Galleries except the import gallery and the user_gallery
     * 
     * @return Vector of String (name of the Galleries)
     */
    public Vector getAllGalleries() {
        Vector vGalleries = new Vector();
        try {
            theConnection = DBServiceManager.allocateConnection(database);
            String query = "SELECT gallery ";
            query += "FROM galleries ";
            query += "WHERE gallery <> 'import' ";
            query += "AND gallery <> 'user_gallery'";
            PreparedStatement state = theConnection.prepareStatement(query);
            ResultSet rs = state.executeQuery();
            while (rs.next()) {
                vGalleries.add(rs.getString("gallery"));
            }
        } catch (SQLException e) {
            vGalleries = new Vector();
        } finally {
            if (theConnection != null) theConnection.release();
        }
        return vGalleries;
    }

    /**
     * Method used to setup the number maximal of results that we want to have for a Specific request
     *
     * @param whichcase is the {@link MediaVariables} that specify wich request we will do
     * @param nb_max is the number of results maximal that we expect for the case specified
     */
    public void setMax(int whichcase, int nb_max) {
        if (whichcase == ORDER_BY_ALFABETHICAL) {
        } else if (whichcase == ORDER_BY_TOP_TEN) {
            this.MAX_TOP = nb_max;
        } else if (whichcase == ORDER_BY_NEW) {
            this.MAX_NEW = nb_max;
        }
    }
}
