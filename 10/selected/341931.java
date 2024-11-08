package hambo.mobiledirectory;

import java.util.*;
import java.sql.*;
import hambo.svc.database.*;
import hambo.util.OID;

/**
 * General class to handle all DB queries about Submision
 */
public class DirectorySubmissionRequest {

    private Vector vOtherUrl1 = null;

    private Vector vOtherUrl2 = null;

    /**
     * Constructor 
     */
    public DirectorySubmissionRequest() {
    }

    public void setOtherURL(Vector vOtherUrl1) {
        this.vOtherUrl1 = vOtherUrl1;
    }

    public void setOtherURL2(Vector vOtherUrl2) {
        this.vOtherUrl2 = vOtherUrl2;
    }

    /**
     * Returns a vector of all submissions in THIS category (not sent
     * to this cat) Note: the vector holds Integers which are the
     * submission IDs
     * @param cid is a category_id
     */
    public Vector getAllSubmissionsHere(int cid) {
        Vector vSubs = new Vector();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT DISTINCT di_submission_name,di_submission_email,");
            query.append("di_submission_url, di_submission_date, di_submission_description, ");
            query.append("di_submission_cid,di_submission_language_id,di_submission_id FROM mdir_Submission ");
            query.append("WHERE di_submission_cid=" + cid + " ORDER BY  di_submission_date ASC");
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                int sid = rs.getInt(8);
                Integer in = new Integer(sid);
                vSubs.add(in);
            }
        } catch (SQLException e) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return vSubs;
    }

    /**
    * Returns true if there is already an application to become editor for this category and this userid
    * @param userid is the user_id of teh user who want to do an application
    * @param cid is teh category_id where the user want to be an editor
    */
    public boolean isThereAnApplication(String userid, int cid) {
        DBConnection con = null;
        boolean result = false;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT (di_submission_id) FROM mdir_Submission WHERE di_submission_email=\'" + userid + "\' AND ");
            query.append("di_submission_cid=" + cid + " AND di_submission_type=1");
            ResultSet rs = con.executeQuery(query.toString());
            result = rs.next();
        } catch (SQLException sqle) {
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return result;
    }

    /**
    * Returns a Vector of all wannabe editors for the category or subcategories
    * The Vector contains a list of ObjectAppliedEditor object
    * @param cid is a category_id
    */
    public Vector getAllAppliedEditors(int cid) {
        Vector vApps = new Vector();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT DISTINCT di_submission_id, di_submission_email, ");
            query.append("di_submission_date, di_submission_url, di_submission_name,");
            query.append(" di_submission_description, di_submission_language_id, ");
            query.append("di_submission_url2, di_submission_name2, di_submission_description2, ");
            query.append("di_submission_language_id2, di_submission_cid, di_submission_affiliation,");
            query.append("di_submission_url_web,di_submission_url2_web FROM mdir_Submission ");
            query.append("WHERE di_submission_type=1 AND di_submission_id ");
            query.append("IN (SELECT di_submitcat_subid FROM mdir_Submission_Category WHERE ");
            query.append("di_submitcat_cid=" + cid + ") ORDER BY  di_submission_date ASC");
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                int subid = rs.getInt(1);
                String userid = rs.getString(2);
                String date = rs.getString(3);
                String url1 = rs.getString(4);
                String name1 = rs.getString(5);
                String description1 = rs.getString(6);
                int language1 = rs.getInt(7);
                String url2 = rs.getString(8);
                String name2 = rs.getString(9);
                String description2 = rs.getString(10);
                int language2 = rs.getInt(11);
                int subcid = rs.getInt(12);
                String affiliation = rs.getString(13);
                String url1web = rs.getString(14);
                String url2web = rs.getString(15);
                ObjectAppliedEditor editor = new ObjectAppliedEditor();
                editor.setUserid(userid);
                editor.setCid(subcid);
                editor.setAffiliation(affiliation);
                editor.setId(subid);
                editor.setDate(date);
                editor.setFirstLink(name1, url1, description1, language1, url1web);
                editor.setSecondLink(name2, url2, description2, language2, url2web);
                vApps.add(editor);
            }
        } catch (SQLException e) {
            System.err.println("ERROR:getAllAppliedEditors" + e);
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return vApps;
    }

    /**
    * Return an ObjectSubmittedSite object to hold data about a submitted site with the submission_id "rid"
    * @param rid is a submission_id
    */
    public ObjectSubmittedSite getSubmittedSite(int rid) {
        ObjectSubmittedSite site = null;
        DBConnection con = null;
        int rootid = rid;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT di_submission_id,di_submission_name,di_submission_cid,");
            query.append("di_submission_url,di_submission_description,di_submission_language_id,");
            query.append("'5',di_submission_email,di_submission_date,di_submission_url_web ");
            query.append("FROM mdir_Submission WHERE di_submission_id=" + rootid + " ORDER BY ");
            query.append("di_submission_date");
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                String name = rs.getString(2);
                int id = rs.getInt(1);
                int cid = rs.getInt(3);
                String url = rs.getString(4);
                String description = rs.getString(5);
                String language = rs.getString(6);
                String note = rs.getString(7);
                String email = rs.getString(8);
                String date = rs.getString(9);
                String urlweb = rs.getString(10);
                site = new ObjectSubmittedSite(rootid, name, cid, url, description, email, date, (new Integer(language)).intValue(), urlweb);
            }
            query = new StringBuffer();
            query.append("SELECT di_submit_language1_languageid FROM mdir_Submission_Language1 WHERE");
            query.append(" di_submit_language1_subid=" + site.getId());
            rs = con.executeQuery(query.toString());
            Vector vLanguages = new Vector();
            while (rs.next()) {
                vLanguages.addElement(new Integer(rs.getInt(1)));
            }
            site.setLanguages(vLanguages);
            query = new StringBuffer();
            query.append("SELECT di_submit_URL1_typeid,di_submit_URL1_value FROM mdir_Submission_Url1 WHERE");
            query.append(" di_submit_URL1_subid=" + site.getId());
            rs = con.executeQuery(query.toString());
            Vector vOtherURL = new Vector();
            while (rs.next()) {
                ObjectOtherURL oOtherURL = new ObjectOtherURL();
                int di_submit_URL1_typeid = rs.getInt(1);
                String di_submit_URL1_value = rs.getString(2);
                oOtherURL.setUrlValue(di_submit_URL1_value);
                oOtherURL.setUrlType(di_submit_URL1_typeid);
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
    *  Returns a Vector of all submitted sites for the category (or subcategories if there is no relevant editor)
    * The Vector contains a list of ObjectSubmittedSite object
    * @param userid is the user_id of the editor
    * @param cid is the category_id where the user is editor
    */
    public Vector getAllSubmittedSites(String username, int cid) {
        Vector vSubs = new Vector();
        DBConnection con = null;
        try {
            con = DBServiceManager.allocateConnection();
            StringBuffer query = new StringBuffer();
            query.append("SELECT DISTINCT di_submission_name,di_submission_email,di_submission_url, di_submission_date, di_submission_description, di_submission_cid,di_submission_language_id,di_submission_id,di_submission_url_web FROM mdir_Submission WHERE di_submission_id IN (SELECT di_submitcat_subid FROM mdir_Submission_Category WHERE di_submitcat_cid=" + cid + ") AND di_submission_type=0 ORDER BY  di_submission_date ASC");
            ResultSet rs = con.executeQuery(query.toString());
            while (rs.next()) {
                String name = rs.getString(1);
                String email = rs.getString(2);
                String url = rs.getString(3);
                String date = rs.getString(4);
                String description = rs.getString(5);
                int scid = rs.getInt(6);
                int language = rs.getInt(7);
                int sid = rs.getInt(8);
                String urlweb = rs.getString(9);
                ObjectSubmittedSite subsite = new ObjectSubmittedSite(sid, name, scid, url, description, email, date, language, urlweb);
                vSubs.add(subsite);
            }
        } catch (SQLException e) {
            System.err.println("ERROR:getAllSubmittedSites" + e);
        } finally {
            if (con != null) {
                con.release();
            }
        }
        return vSubs;
    }

    /**
    * Create a new submission to propose a site
    * @param userid is the user_id of the user who wnat to be editor
    * @param cid is teh category_id where the user want to be editor
    * @param siteURL is the wap url of the first site submitted by the user
    * @param siteName is the title of the first site submitted by the user
    * @param siteDesc is the description of  the first site submitted by the user
    * @param siteLanguages is a table with the language_id supported by the first site submitted by the user
    * @param siteURLweb is the web url of the first site submitted by the user
    * @param siteLoc is location id of the first site submitted by the user
    */
    public boolean submitNewSite(String username, int cid, String siteURL, String siteName, String siteDesc, int[] siteLanguages, String siteURLweb, int[] siteLocations) {
        DBConnection con = null;
        int rs = 0;
        int subid = 0;
        DirectoryUtil dUtil = new DirectoryUtil();
        boolean result = false;
        siteURL = dUtil.clean(siteURL);
        siteURLweb = dUtil.clean(siteURLweb);
        siteName = dUtil.clean(siteName);
        if (siteName.length() >= 40) siteName = siteName.substring(0, 40);
        if (siteDesc == null) {
            siteDesc = "";
        }
        siteDesc = dUtil.clean(siteDesc);
        if (siteDesc.length() >= 255) siteDesc = siteDesc.substring(0, 255);
        if (siteURL.trim().equals("http://")) siteURL = "";
        if (siteURLweb.trim().equals("http://")) siteURLweb = "";
        if (siteURL.trim().equals(siteURLweb.trim()) && siteURL.trim().equals("")) return result;
        if ((siteName.trim().equals("")) || siteLanguages.length <= 0) return result;
        DirectoryCategoryRequest dcr = new DirectoryCategoryRequest();
        int catToSubmit = dcr.findCategoryWithEditor(cid);
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            StringBuffer query = new StringBuffer();
            con.executeUpdate("INSERT INTO mdir_Submission (di_submission_date, di_submission_email, di_submission_url, di_submission_name, di_submission_description, di_submission_language_id, di_submission_grade, di_submission_url2, di_submission_name2, di_submission_description2, di_submission_language_id2, di_submission_grade2, di_submission_cid, di_submission_affiliation, di_submission_type, di_submission_category_sent, di_submission_date_sent, di_submission_url_web, di_submission_url2_web) VALUES (getdate(), \'" + username + "\', \'" + siteURL + "\', \'" + siteName + "\',\'" + siteDesc + "\',0,0,'','','',0,0," + cid + ",'',0," + catToSubmit + ",getdate(),'" + siteURLweb + "','')");
            OID newoid = DBUtil.getCurrentOID(con, "mdir_Submission");
            con.executeUpdate("INSERT INTO mdir_Submission_Category (di_submitcat_cid, di_submitcat_subid) VALUES (" + catToSubmit + "," + newoid.toString() + ")");
            for (int i = 0; i < siteLanguages.length; i++) {
                con.executeUpdate("INSERT INTO mdir_Submission_Language1 (di_submit_language1_subid, di_submit_language1_languageid) VALUES ( " + newoid.toString() + ", " + siteLanguages[i] + ") ");
            }
            for (int i = 0; i < siteLocations.length; i++) {
                con.executeUpdate("INSERT INTO mdir_Submission_Location1 (di_submit_location1_subid, di_submit_location1_locationid) VALUES ( " + newoid.toString() + ", " + siteLocations[i] + ") ");
            }
            if (vOtherUrl1 != null) {
                for (Enumeration e = vOtherUrl1.elements(); e.hasMoreElements(); ) {
                    ObjectOtherURL oUrl = (ObjectOtherURL) e.nextElement();
                    String url_value = oUrl.getUrlValue();
                    int url_type = oUrl.getUrlType();
                    con.executeUpdate("INSERT INTO mdir_Submission_Url1 (di_submit_URL1_subid, di_submit_URL1_typeid, di_submit_URL1_value) VALUES ( " + newoid.toString() + "," + url_type + ",'" + url_value + "') ");
                }
            }
            con.commit();
            result = true;
        } catch (SQLException sqle) {
            System.err.println("EXCEPTION:submitNewSite:" + sqle);
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException e) {
                }
            }
            result = false;
        } finally {
            try {
                con.reset();
            } catch (SQLException e) {
            }
            if (con != null) {
                con.release();
            }
        }
        return result;
    }

    /**
    * Create a new submission to become an editor
    * @param userid is the user_id of the user who wnat to be editor
    * @param cid is teh category_id where the user want to be editor
    * @param affiliation is teh text written by teh user which explain why he want to be editor
    * @param url1 is the wap url of the first site submitted by the user
    * @param title1 is the title of the first site submitted by the user
    * @param description1 is the description of  the first site submitted by the user
    * @param languages1 is a table with the language_id supported by the first site submitted by the user
    * @param url1web is teh web url of the first site submitted by the user
    * @param url2 is the wap url of the second site submitted by the user
    * @param title2 is the title of the second site submitted by the user
    * @param description2 is the description of  the second site submitted by the user
    * @param languages2 is a table with the language_id supported by the second site submitted by the user
    * @param url2web is teh web url of the second site submitted by the user
    
    */
    public boolean applyNewEditor(String userid, int cid, String affiliation, String url1, String title1, String description1, int[] languages1, String url2, String title2, String description2, int[] languages2, String url1web, String url2web, int[] locations1, int[] locations2) {
        DBConnection con = null;
        boolean result = false;
        int subid = 0;
        DirectoryUtil dUtil = new DirectoryUtil();
        affiliation = dUtil.clean(affiliation);
        if (affiliation.length() >= 255) affiliation = affiliation.substring(0, 255);
        url1 = dUtil.clean(url1);
        url1web = dUtil.clean(url1web);
        title1 = dUtil.clean(title1);
        if (title1.length() >= 40) title1 = title1.substring(0, 40);
        description1 = dUtil.clean(description1);
        if (description1.length() >= 255) description1 = description1.substring(0, 255);
        url2 = dUtil.clean(url2);
        url2web = dUtil.clean(url2web);
        title2 = dUtil.clean(title2);
        if (title2.length() >= 40) title2 = title2.substring(0, 40);
        description2 = dUtil.clean(description2);
        if (description2.length() >= 255) description2 = description2.substring(0, 255);
        DirectoryCategoryRequest dcr = new DirectoryCategoryRequest();
        int catToSubmit = dcr.findCategoryWithEditor(cid);
        try {
            con = DBServiceManager.allocateConnection();
            con.setAutoCommit(false);
            StringBuffer query = new StringBuffer();
            con.executeUpdate("INSERT INTO mdir_Submission (di_submission_date, di_submission_email, di_submission_url, di_submission_name, di_submission_description, di_submission_language_id, di_submission_grade, di_submission_url2, di_submission_name2, di_submission_description2, di_submission_language_id2, di_submission_grade2, di_submission_cid, di_submission_affiliation, di_submission_type, di_submission_category_sent, di_submission_date_sent, di_submission_url_web, di_submission_url2_web) VALUES (getdate(), \'" + userid + "\', \'" + url1 + "\', \'" + title1 + "\',\'" + description1 + "\',0,5,\'" + url2 + "\', \'" + title2 + "\',\'" + description2 + "\',0,5," + cid + ",\'" + affiliation + "\',1," + catToSubmit + ",getDate(),'" + url1web + "','" + url2web + "')");
            OID newoid = DBUtil.getCurrentOID(con, "mdir_Submission");
            con.executeUpdate("INSERT INTO mdir_Submission_Category (di_submitcat_cid, di_submitcat_subid) VALUES (" + catToSubmit + "," + newoid.toString() + ")");
            for (int i = 0; i < languages1.length; i++) {
                con.executeUpdate("INSERT INTO mdir_Submission_Language1 (di_submit_language1_subid, di_submit_language1_languageid) VALUES ( " + newoid.toString() + ", " + languages1[i] + ")");
            }
            for (int i = 0; i < languages2.length; i++) {
                con.executeUpdate("INSERT INTO mdir_Submission_Language2 (di_submit_language2_subid, di_submit_language2_languageid) VALUES ( " + newoid.toString() + ", " + languages2[i] + ")");
            }
            for (int i = 0; i < locations1.length; i++) {
                con.executeUpdate("INSERT INTO mdir_Submission_Location1 (di_submit_location1_subid, di_submit_location1_locationid) VALUES ( " + newoid.toString() + ", " + locations1[i] + ")");
            }
            for (int i = 0; i < locations2.length; i++) {
                con.executeUpdate("INSERT INTO mdir_Submission_Location2 (di_submit_location2_subid, di_submit_location2_locationid) VALUES ( " + newoid.toString() + ", " + locations2[i] + ")");
            }
            if (vOtherUrl1 != null) {
                for (Enumeration e = vOtherUrl1.elements(); e.hasMoreElements(); ) {
                    ObjectOtherURL oUrl = (ObjectOtherURL) e.nextElement();
                    String url_value = oUrl.getUrlValue();
                    int url_type = oUrl.getUrlType();
                    con.executeUpdate("INSERT INTO mdir_Submission_Url1 (di_submit_URL1_subid, di_submit_URL1_typeid, di_submit_URL1_value) VALUES ( " + newoid.toString() + "," + url_type + ",'" + url_value + "') ");
                }
            }
            if (vOtherUrl2 != null) {
                for (Enumeration e = vOtherUrl2.elements(); e.hasMoreElements(); ) {
                    ObjectOtherURL oUrl = (ObjectOtherURL) e.nextElement();
                    String url_value = oUrl.getUrlValue();
                    int url_type = oUrl.getUrlType();
                    con.executeUpdate("INSERT INTO mdir_Submission_Url2 (di_submit_URL2_subid, di_submit_URL2_typeid, di_submit_URL2_value) VALUES ( " + newoid.toString() + "," + url_type + ",'" + url_value + "') ");
                }
            }
            con.commit();
            result = true;
        } catch (SQLException sqle) {
            System.err.println("EXCEPTION:applyNewEditor:" + sqle);
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
    * Delete the submission with submission_id "subid"
    * @param username is the user_id of the editor who delete the submission
    * @param subid is teh submission_id of the submission to delete
    */
    public boolean deleteSubmission(String username, int subid) {
        DBConnection con = null;
        int rs = 0;
        boolean result = true;
        con = DBServiceManager.allocateConnection();
        try {
            rs = con.executeUpdate("DELETE FROM mdir_Submission_Category WHERE di_submitcat_subid=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        result = result && (rs != 0);
        try {
            con.executeUpdate("DELETE FROM mdir_Submission_Language1 WHERE di_submit_language1_subid=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        try {
            con.executeUpdate("DELETE FROM mdir_Submission_Language2 WHERE di_submit_language2_subid=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        try {
            con.executeUpdate("DELETE FROM mdir_Submission_Location1 WHERE di_submit_location1_subid=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        try {
            con.executeUpdate("DELETE FROM mdir_Submission_Location2 WHERE di_submit_location2_subid=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        try {
            con.executeUpdate("DELETE FROM mdir_Submission_Url1 WHERE di_submit_URL1_subid=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        try {
            con.executeUpdate("DELETE FROM mdir_Submission_Url2 WHERE di_submit_URL2_subid=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        try {
            con.executeUpdate("DELETE FROM mdir_Submission WHERE di_submission_id=" + subid);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        if (con != null) {
            con.release();
        }
        result = result & (rs != 0);
        return result;
    }
}
