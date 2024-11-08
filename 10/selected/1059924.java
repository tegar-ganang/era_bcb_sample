package hambo.mobiledirectory;

import java.util.*;
import java.sql.*;
import hambo.util.XMLUtil;
import hambo.util.StringUtil;
import hambo.svc.database.*;
import hambo.util.OID;

/**
 * General class to handle all DB queries about Site
 */
public class DirectorySiteRequest {

    private Vector vLanguages = null;

    private boolean only = false;

    private String location = "1";

    private Vector vOtherUrl = null;

    /**
    * Constructor 
    */
    public DirectorySiteRequest() {
    }

    public void setOtherURL(Vector vOtherUrl) {
        this.vOtherUrl = vOtherUrl;
    }

    /**
    * Set the languages that the sites must support for the Search
    */
    public void setvLanguages(Vector vLanguages) {
        this.vLanguages = vLanguages;
    }

    /**
    * Set the location that the sites must support for the Search
    */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
    * Set the only flag for the Search...
    * If only is true the result of the request must include ONLY sites 
    */
    public void setOnlySpeakingLanguges(boolean only) {
        this.only = only;
    }

    /**
    * Creates and returns an ObjectSite object to hold data about a site, identified by its id: rid
    * @param rid the site_id
    */
    public ObjectSite getSite(int rid) {
        ObjectSite site = null;
        DBConnection con = null;
        DirectoryUtil dUtil = new DirectoryUtil();
        int rootid = rid;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT di_site_name,di_site_id,di_site_cid,di_site_url,");
            query.append("di_site_description,di_site_language_id, di_site_rating,");
            query.append(" di_site_updated_date, di_site_updated_by, di_site_approved_date,");
            query.append("di_site_approved_by, di_site_submitted_date, di_site_submitted_by,");
            query.append(" di_site_creation_date, di_site_weburl FROM mdir_Site WHERE di_site_id=" + rootid);
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                String name = rs.getString(1);
                int id = rs.getInt(2);
                int cid = rs.getInt(3);
                String url = rs.getString(4);
                String description = rs.getString(5);
                String language = rs.getString(6);
                float rating = rs.getFloat(7);
                String updD = dUtil.correctDate(rs.getString(8));
                String updB = rs.getString(9);
                String appD = dUtil.correctDate(rs.getString(10));
                String appB = rs.getString(11);
                String subD = dUtil.correctDate(rs.getString(12));
                String subB = rs.getString(13);
                String creD = dUtil.correctDate(rs.getString(14));
                String webUrl = rs.getString(15);
                site = new ObjectSite();
                site.setName(name);
                site.setId(id);
                site.setCid(cid);
                site.setURL(url);
                site.setDescription(description);
                site.setRating(rating);
                site.setLastUpdateDate(updD);
                site.setLastUpdateBy(updB);
                site.setApprovedDate(appD);
                site.setApprovedBy(appB);
                site.setSubmittedDate(subD);
                site.setSubmittedBy(subB);
                site.setCreationDate(creD);
                site.setWebUrl(webUrl);
            }
            Vector vLanguages = new Vector();
            query = new StringBuffer();
            query.append("SELECT DISTINCT di_site_language_languageid ");
            query.append("FROM mdir_Site_Language WHERE di_site_language_siteid=" + site.getId());
            rs = con.executeQuery(query.toString());
            while (rs.next()) {
                vLanguages.addElement(new Integer(rs.getInt(1)));
            }
            site.setLanguages(vLanguages);
            Vector vLocations = new Vector();
            query = new StringBuffer();
            query.append("SELECT DISTINCT di_site_location_locationid ");
            query.append("FROM mdir_Site_Location WHERE di_site_location_siteid=" + site.getId());
            rs = con.executeQuery(query.toString());
            while (rs.next()) {
                vLocations.addElement(new Integer(rs.getInt(1)));
            }
            site.setLocations(vLocations);
            Vector vOtherURL = new Vector();
            query = new StringBuffer();
            query.append("SELECT DISTINCT di_otherurl_typeid,di_otherurl_value,di_urltype_name ");
            query.append("FROM mdir_Url_Support,mdir_Url_Type WHERE di_urltype_id=di_otherurl_typeid AND di_otherurl_siteid=" + site.getId());
            rs = con.executeQuery(query.toString());
            while (rs.next()) {
                int type = rs.getInt(1);
                String value = rs.getString(2);
                String typename = rs.getString(3);
                ObjectOtherURL oOtherURL = new ObjectOtherURL();
                oOtherURL.setUrlValue(value);
                oOtherURL.setUrlType(type);
                oOtherURL.setUrlTypeName(typename);
                vOtherURL.addElement(oOtherURL);
            }
            site.setOtherURL(vOtherURL);
        } catch (SQLException sqle) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return site;
    }

    /**
    * Return all the sites for the category with the specific id: rid
    * @param username is the user_id of teh current user
    * @param rid is the category_id of the category specified
    * @param orderBy is the String which will appear after the ORDER BY in the request
    */
    public Vector getAllSite(String username, int rid, String orderBy) {
        Vector vSite = new Vector();
        DBConnection con = null;
        DirectoryUtil dUtil = new DirectoryUtil();
        int rootid = rid;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT DISTINCT di_site_name,di_site_id,di_site_cid,");
            query.append("di_site_description,di_site_url,di_site_language_id,");
            query.append("di_site_rating,di_site_updated_date FROM mdir_Site ");
            if (!location.equals("1")) query.append(",mdir_Site_Location ");
            if (only && vLanguages != null) query.append(",mdir_Site_Language ");
            query.append(" WHERE di_site_cid=" + rootid + " AND di_site_deleted_date is NULL");
            if (!location.equals("1")) {
                query.append(" AND di_site_location_siteid=di_site_id AND di_site_location_locationid IN (");
                query.append("1," + location + ")");
            }
            if (only && vLanguages != null) {
                query.append(" AND di_site_language_siteid=di_site_id AND di_site_language_languageid IN (");
                String lang = "";
                for (Enumeration eLang = vLanguages.elements(); eLang.hasMoreElements(); ) {
                    lang = lang + (String) eLang.nextElement() + ",";
                }
                lang = lang.substring(0, lang.length() - 1);
                query.append(lang + ")");
            }
            if (orderBy.trim().equals("")) orderBy = " ORDER BY di_site_name";
            query.append(orderBy);
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                String name = rs.getString(1);
                int id = rs.getInt(2);
                int cid = rs.getInt(3);
                String description = rs.getString(4);
                String url = rs.getString(5);
                String language = rs.getString(6);
                float rating = rs.getFloat(7);
                String creD = dUtil.correctDate(rs.getString(8));
                ObjectSite site = new ObjectSite();
                site.setId(id);
                site.setName(name);
                site.setCid(cid);
                site.setURL(url);
                site.setDescription(description);
                site.setRating(rating);
                site.setLastUpdateDate(creD);
                vSite.addElement(site);
            }
            for (Enumeration e = vSite.elements(); e.hasMoreElements(); ) {
                ObjectSite site = (ObjectSite) e.nextElement();
                int id = site.getId();
                Vector vLanguages = new Vector();
                query = new StringBuffer();
                query.append("SELECT DISTINCT di_site_language_languageid ");
                query.append("FROM mdir_Site_Language WHERE di_site_language_siteid=" + id);
                rs = con.executeQuery(query.toString());
                while (rs.next()) {
                    vLanguages.addElement(new Integer(rs.getInt(1)));
                }
                site.setLanguages(vLanguages);
                Vector vLocations = new Vector();
                query = new StringBuffer();
                query.append("SELECT DISTINCT di_site_location_locationid ");
                query.append("FROM mdir_Site_Location WHERE di_site_location_siteid=" + id);
                rs = con.executeQuery(query.toString());
                while (rs.next()) {
                    vLocations.addElement(new Integer(rs.getInt(1)));
                }
                site.setLocations(vLocations);
            }
        } catch (SQLException e) {
        } finally {
            if (con != null) con.release();
        }
        return vSite;
    }

    /**
    * Add a new site in the table di_site, from a SUBMISSION
    * @param username is teh user_id of teh editor who had the site
    * @param cid is the category_id of the category where the site is stored
    * @param siteURL is the WAP url of the site
    * @param siteName is the name of teh site
    * @param siteDesc is the description of the site 
    * @param siteLanguages is the table with the language_id supported by teh site
    * @param submissionId is the id of the submission which propose this site
    * @param siteURLweb is the WEB url of the site
    */
    public boolean addNewSite(String username, int cid, String siteURL, String siteName, String siteDesc, int[] siteLanguages, int submissionId, String siteURLweb, int[] siteLocations) {
        DirectoryUtil dUtil = new DirectoryUtil();
        DBConnection con = null;
        int rs = 0;
        boolean result = false;
        DirectorySubmissionRequest dSubr = new DirectorySubmissionRequest();
        ObjectSubmittedSite oss = dSubr.getSubmittedSite(submissionId);
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            con.executeUpdate("INSERT INTO mdir_Site (di_site_cid, di_site_url, di_site_name, di_site_description, di_site_deleted_date, di_site_deleted_by, di_site_updated_date, di_site_updated_by, di_site_approved_date, di_site_approved_by, di_site_language_id, di_site_grade, di_site_submitted_date, di_site_submitted_by, di_site_creation_date, di_site_rating, di_site_shadowname,di_site_shadowdescription, di_site_weburl) VALUES (" + cid + ",\'" + XMLUtil.encodeNumerical(siteURL) + "\',\'" + XMLUtil.encodeNumerical(siteName) + "\',\'" + XMLUtil.encodeNumerical(siteDesc) + "\', NULL,NULL,getdate(),\'" + username + "\',getdate(),\'" + username + "\',0,0,\'" + dUtil.correctDate(oss.getDate()) + "\',\'" + oss.getEmail() + "\',getdate(),0,\'" + XMLUtil.shadow(StringUtil.replace(siteName, "\'", "\'\'")) + "\',\'" + XMLUtil.shadow(StringUtil.replace(siteDesc, "\'", "\'\'")) + "\','" + XMLUtil.encodeNumerical(StringUtil.replace(siteURLweb, "\'", "\'\'")) + "')");
            OID newoid = DBUtil.getCurrentOID(con, "mdir_Site");
            for (int i = 0; i < siteLanguages.length; i++) con.executeUpdate("INSERT INTO mdir_Site_Language (di_site_language_siteid, di_site_language_languageid) VALUES ( " + newoid.toString() + ", " + siteLanguages[i] + ")");
            for (int i = 0; i < siteLocations.length; i++) con.executeUpdate("INSERT INTO mdir_Site_Location (di_site_location_siteid, di_site_location_locationid) VALUES ( " + newoid.toString() + ", " + siteLocations[i] + ")");
            if (vOtherUrl != null) {
                for (Enumeration e = vOtherUrl.elements(); e.hasMoreElements(); ) {
                    ObjectOtherURL oUrl = (ObjectOtherURL) e.nextElement();
                    String url_value = oUrl.getUrlValue();
                    int url_type = oUrl.getUrlType();
                    con.executeUpdate("INSERT INTO mdir_Url_Support (di_otherurl_siteid, di_otherurl_typeid, di_otherurl_value) VALUES ( " + newoid.toString() + "," + url_type + ",'" + XMLUtil.encodeNumerical(url_value) + "') ");
                }
            }
            con.commit();
            result = true;
        } catch (SQLException sqle) {
            System.err.println("SQL EXCEPTION : addNewSite FROM a submisson:" + sqle);
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException e) {
                }
            }
        } finally {
            if (con != null) {
                try {
                    con.reset();
                } catch (SQLException e) {
                }
                con.release();
            }
        }
        return result;
    }

    /**
    * Add a new Site in the table di_site
    * @param username is teh user_id of teh editor who had the site
    * @param cid is the category_id of the category where the site is stored
    * @param siteURL is the WAP url of the site
    * @param siteName is the name of teh site
    * @param siteDesc is the description of the site 
    * @param siteLanguages is the table with the language_id supported by teh site
    * @param siteURLweb is the WEB url of the site
    */
    public boolean addNewSite(String username, int cid, String siteURL, String siteName, String siteDesc, int[] siteLanguages, String siteURLweb, int[] siteLocations) {
        DBConnection con = null;
        int rs = 0;
        boolean result = false;
        int siteId = 0;
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            con.executeUpdate("INSERT INTO mdir_Site (di_site_cid, di_site_url, di_site_name, di_site_description, di_site_deleted_date, di_site_deleted_by, di_site_updated_date, di_site_updated_by, di_site_approved_date, di_site_approved_by, di_site_language_id, di_site_grade, di_site_submitted_date, di_site_submitted_by, di_site_creation_date, di_site_rating, di_site_shadowname,di_site_shadowdescription, di_site_weburl) VALUES (" + cid + ",\'" + siteURL + "\',\'" + XMLUtil.encodeNumerical(siteName) + "\',\'" + XMLUtil.encodeNumerical(siteDesc) + "\',NULL,NULL,getdate(),\'" + username + "\',getdate(),\'" + username + "\',0,0,getdate(),\'" + username + "\',getdate(),0,\'" + XMLUtil.shadow(StringUtil.replace(siteName, "\'", "\'\'")) + "\',\'" + XMLUtil.shadow(StringUtil.replace(siteDesc, "\'", "\'\'")) + "\','" + StringUtil.replace(siteURLweb, "\'", "\'\'") + "')");
            OID newoid = DBUtil.getCurrentOID(con, "mdir_Site");
            for (int i = 0; i < siteLanguages.length; i++) con.executeUpdate("INSERT INTO mdir_Site_Language (di_site_language_siteid, di_site_language_languageid) VALUES ( " + newoid.toString() + ", " + siteLanguages[i] + ")");
            for (int i = 0; i < siteLocations.length; i++) con.executeUpdate("INSERT INTO mdir_Site_Location (di_site_location_siteid, di_site_location_locationid) VALUES ( " + newoid.toString() + ", " + siteLocations[i] + ")");
            if (vOtherUrl != null) {
                for (Enumeration e = vOtherUrl.elements(); e.hasMoreElements(); ) {
                    ObjectOtherURL oUrl = (ObjectOtherURL) e.nextElement();
                    String url_value = oUrl.getUrlValue();
                    int url_type = oUrl.getUrlType();
                    con.executeUpdate("INSERT INTO mdir_Url_Support  (di_otherurl_siteid, di_otherurl_typeid, di_otherurl_value) VALUES ( " + newoid.toString() + "," + url_type + ",'" + url_value + "') ");
                }
            }
            con.commit();
            result = true;
        } catch (SQLException sqle) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException e) {
                }
            }
        } finally {
            if (con != null) {
                try {
                    con.reset();
                } catch (SQLException e) {
                }
                con.release();
            }
        }
        return result;
    }

    /**
    * Mark the Site with the site_id sid deleted
    * @param username is the user_id of the editor who delete the site
    * @param sid isthe site_id of the site to delete
    */
    public boolean deleteSite(String username, int sid) {
        DBConnection con = null;
        int rs = 0;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("UPDATE mdir_Site SET di_site_deleted_date=getdate(),di_site_deleted_by");
            query.append("=\'" + username + "\' WHERE di_site_id=" + sid);
            rs = con.executeUpdate(query.toString());
        } catch (SQLException e) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return (rs != 0);
    }

    /**
    * Update a Site
    * @param username is teh user_id of teh editor who had the site
    * @param cid is the category_id of the category where the site is stored
    * @param siteURL is the WAP url of the site
    * @param siteName is the name of teh site
    * @param siteDesc is the description of the site 
    * @param siteLanguages is the table with the language_id supported by teh site
    * @param siteURLweb is the WEB url of the site
    */
    public boolean updateSite(String username, int sid, String siteURL, String siteName, String siteDesc, int[] siteLanguages, String siteURLweb, int[] siteLocations) {
        DBConnection con = null;
        int rs = 0;
        boolean result = false;
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            StringBuffer query = new StringBuffer();
            query.append("UPDATE mdir_Site SET di_site_url=\'" + siteURL + "\',di_site_name");
            query.append("=\'" + XMLUtil.encodeNumerical(siteName) + "\',di_site_description=\'" + XMLUtil.encodeNumerical(siteDesc) + "\',di_site_updated_date");
            query.append("=getdate(),di_site_updated_by=\'" + username + "\',");
            query.append("di_site_shadowname=\'" + XMLUtil.shadow(StringUtil.replace(siteName, "\'", "\'\'")) + "\',di_site_shadowdescription");
            query.append("=\'" + XMLUtil.shadow(StringUtil.replace(siteDesc, "\'", "\'\'")) + "\',di_site_weburl='" + StringUtil.replace(siteURLweb, "\'", "\'\'") + "' ");
            query.append("WHERE di_site_id=" + sid);
            rs = con.executeUpdate(query.toString());
            result = (rs != 0);
            rs = con.executeUpdate("DELETE FROM mdir_Site_Language WHERE di_site_language_siteid=" + sid);
            for (int i = 0; i < siteLanguages.length; i++) {
                rs = con.executeUpdate("INSERT INTO mdir_Site_Language (di_site_language_siteid, di_site_language_languageid)  VALUES (" + sid + "," + siteLanguages[i] + ")");
                result = result && (rs != 0);
            }
            rs = con.executeUpdate("DELETE FROM mdir_Site_Location WHERE di_site_location_siteid=" + sid);
            for (int i = 0; i < siteLocations.length; i++) {
                rs = con.executeUpdate("INSERT INTO mdir_Site_Location (di_site_location_siteid, di_site_location_locationid) VALUES (" + sid + "," + siteLocations[i] + ")");
                result = result && (rs != 0);
            }
            rs = con.executeUpdate("DELETE FROM mdir_Url_Support WHERE di_otherurl_siteid=" + sid);
            if (vOtherUrl != null && vOtherUrl.size() > 0) {
                query = new StringBuffer();
                for (Enumeration e = vOtherUrl.elements(); e.hasMoreElements(); ) {
                    ObjectOtherURL oUrl = (ObjectOtherURL) e.nextElement();
                    String url_value = oUrl.getUrlValue();
                    int url_type = oUrl.getUrlType();
                    query.append("INSERT INTO mdir_Url_Support (di_otherurl_siteid, di_otherurl_typeid, di_otherurl_value) VALUES (" + sid + "," + url_type + ",'" + url_value + "') ");
                }
                rs = con.executeUpdate(query.toString());
            }
            con.commit();
        } catch (SQLException e) {
            System.out.println("EXCEPTION:");
            System.out.println(e.toString());
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException re) {
                    System.out.println("EXCEPTION:");
                    System.out.println(re.toString());
                }
            }
        } finally {
            if (con != null) {
                try {
                    con.reset();
                } catch (SQLException e) {
                }
                con.release();
            }
        }
        return result;
    }

    /**
    * Return the number total of Site not deleted in the database
    */
    public int getNumberTotalOfSite() {
        int nbTotalOfSite = 0;
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            ResultSet rs = con.executeQuery("SELECT count(*) FROM mdir_Site WHERE di_site_deleted_date is NULL");
            if (rs.next()) {
                nbTotalOfSite = rs.getInt(1);
            }
        } catch (SQLException sqle) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return nbTotalOfSite;
    }

    /**
     * Get the 'exclude reviews' flag from the database.
     */
    public boolean getExcludeReviews() {
        DBConnection con = null;
        boolean result = false;
        try {
            con = DBServiceManager.allocateConnection();
            ResultSet rs = con.executeQuery("SELECT exclude FROM mdir_Site_Review_Exclude");
            while (rs.next()) {
                result = rs.getBoolean("exclude");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return result;
    }

    /**
     * Get all reviews for the site whith site_id "sid"
     * @param sid is the site_id
     */
    public Vector getAllReviews(int sid) {
        DBConnection con = null;
        Vector result = new Vector();
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT di_review_id, di_review_rating, di_review_comment, di_review_name, ");
            query.append("di_review_email,di_review_date FROM mdir_Site_Review WHERE di_review_sid=" + sid);
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                int id = rs.getInt(1);
                float rating = rs.getFloat(2);
                String comment = rs.getString(3);
                String name = rs.getString(4);
                String email = rs.getString(5);
                String thedate = rs.getString(6);
                ObjectReview or = new ObjectReview(id, rating, comment, name, email, thedate);
                result.addElement(or);
            }
        } catch (SQLException e) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return result;
    }

    /**
    * Add a new review for the site with the site_id "sid"
    * @param sid is the site_id
    * @param name is the name of the person who rate the site
    * @param email is the email of the person who rate the site
    * @param rating is the rate for the site, given by the person
    * @param comment is the comment about the site, given by the person 
    */
    public boolean addNewReview(int sid, String name, String email, float rating, String comment) {
        DBConnection con = null;
        int rs = 0;
        boolean result = false;
        try {
            con = DBServiceManager.allocateConnection();
            DirectoryUtil dut = new DirectoryUtil();
            rs = con.executeUpdate("INSERT INTO mdir_Site_Review (di_review_sid, di_review_rating, di_review_comment, di_review_name, di_review_email, di_review_date) VALUES(" + sid + "," + rating + ",'" + dut.clean(comment) + "','" + dut.clean(name) + "','" + email + "',getdate())");
            result = (rs != 0);
            Vector allReviews = getAllReviews(sid);
            float avg = 0;
            for (Enumeration e = allReviews.elements(); e.hasMoreElements(); ) avg = avg + ((ObjectReview) e.nextElement()).getRating();
            avg = avg / allReviews.size();
            rs = con.executeUpdate("UPDATE mdir_Site SET di_site_rating=" + avg + " WHERE di_site_id=" + sid);
        } catch (SQLException e) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        result = result & (rs != 0);
        return result;
    }

    /**
    * Update a review for the site with the site_id "sid"
    * @param sid is the site_id
    * @param name is the name of the person who rate the site
    * @param email is the email of the person who rate the site
    * @param rating is the rate for the site, given by the person
    * @param comment is the comment about the site, given by the person 
    */
    public boolean updateReview(int sid, String name, String email, float rating, String comment) {
        DBConnection con = null;
        int rs = 0;
        boolean result = false;
        try {
            con = DBServiceManager.allocateConnection();
            DirectoryUtil dut = new DirectoryUtil();
            rs = con.executeUpdate("UPDATE mdir_Site_Review SET di_review_comment='" + dut.clean(comment) + "' , di_review_rating = " + rating + ",di_review_date = getdate() WHERE di_review_sid=" + sid + " AND di_review_name='" + dut.clean(name) + "'");
            result = (rs != 0);
            Vector allReviews = getAllReviews(sid);
            float avg = 0;
            for (Enumeration e = allReviews.elements(); e.hasMoreElements(); ) avg = avg + ((ObjectReview) e.nextElement()).getRating();
            avg = avg / allReviews.size();
            rs = con.executeUpdate("UPDATE mdir_Site SET di_site_rating=" + avg + " WHERE di_site_id=" + sid);
        } catch (SQLException e) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        result = result & (rs != 0);
        return result;
    }

    /**
    * Delete a review for the site with the site_id "sid"
    * @param sid is the site_id
    * @param revid is the review_id of teh review to delete
    */
    public boolean deleteReview(int sid, String revid) {
        DBConnection con = null;
        int rs = 0;
        boolean result = false;
        try {
            con = DBServiceManager.allocateConnection();
            rs = con.executeUpdate("DELETE FROM mdir_Site_Review WHERE di_review_id=" + revid);
            result = (rs != 0);
            Vector allReviews = getAllReviews(sid);
            float avg = 0;
            for (Enumeration e = allReviews.elements(); e.hasMoreElements(); ) avg = avg + ((ObjectReview) e.nextElement()).getRating();
            avg = avg / allReviews.size();
            if (allReviews.size() == 0) avg = 0;
            rs = con.executeUpdate("UPDATE mdir_Site SET di_site_rating=" + avg + " WHERE di_site_id=" + sid);
        } catch (SQLException e) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        result = result & (rs != 0);
        return result;
    }

    /**
    * Move a site
    * Returns an error code:
    * 0 - Operation performed
    * 1 - N/A
    * 2 - generic error
    * @param sid is the site_id of teh site that you want to move
    * @ to is the category_id of the destination category where you want to move the site
    */
    public int moveSite(int sid, int to) {
        DBConnection con = null;
        int rs = 0;
        try {
            con = DBServiceManager.allocateConnection();
            rs = con.executeUpdate("UPDATE mdir_Site SET di_site_cid=" + to + " WHERE di_site_id=" + sid);
        } catch (SQLException e) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        if (rs != 0) {
            return 0;
        }
        return 2;
    }

    public boolean alreadyExists(int cid, String siteName) {
        boolean exists = true;
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            String query = "SELECT * FROM mdir_Site WHERE di_site_name=\'" + XMLUtil.encodeNumerical(siteName) + "\' AND di_site_cid=" + cid;
            ResultSet rs = con.executeQuery(query.toString());
            if (!rs.next()) {
                exists = false;
            }
        } catch (SQLException e) {
            exists = true;
        } finally {
            if (con != null) con.release();
        }
        return exists;
    }
}
