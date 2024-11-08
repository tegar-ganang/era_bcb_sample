package octopus.requests;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Types;
import hambo.util.StringUtil;
import hambo.svc.database.*;
import octopus.tools.Messages.OctopusErrorMessages;
import octopus.OctopusApplication;

/**
* Requests UPDATE and INSERT in Octopus
*/
public class OctopusRequestFactory {

    public static String updateMainLanguageTranslation(String tag_id, String current_language, String edit_translation, String edit_desc, String user_id, boolean increaseVersion) {
        if (tag_id == null || tag_id.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        if (current_language == null || current_language.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        if (user_id == null || user_id.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        if (edit_translation != null && !edit_translation.trim().equals("")) {
            edit_translation = StringUtil.replace(edit_translation, "\n", " ");
            edit_translation = StringUtil.replace(edit_translation, "\r", " ");
            edit_translation = StringUtil.replace(edit_translation, "\t", " ");
            edit_translation = StringUtil.replace(edit_translation, "<", "&#60;");
            edit_translation = StringUtil.replace(edit_translation, ">", "&#62;");
            edit_translation = StringUtil.replace(edit_translation, "'", "&#39;");
        } else {
            return OctopusErrorMessages.TRANSLATION_TEXT_EMPTY;
        }
        if (edit_desc != null && !edit_desc.trim().equals("")) {
            edit_desc = StringUtil.replace(edit_desc, "\n", " ");
            edit_desc = StringUtil.replace(edit_desc, "\r", " ");
            edit_desc = StringUtil.replace(edit_desc, "\t", " ");
            edit_desc = StringUtil.replace(edit_desc, "<", "&#60;");
            edit_desc = StringUtil.replace(edit_desc, ">", "&#62;");
            edit_desc = StringUtil.replace(edit_desc, "'", "&#39;");
        } else {
            return OctopusErrorMessages.DESCRIPTION_TEXT_EMPTY;
        }
        String resultUpdateDescription = updateDescription(tag_id, edit_desc);
        String versionMainLanguage = "0";
        if (resultUpdateDescription.equals(OctopusErrorMessages.ACTION_DONE)) {
            versionMainLanguage = OctopusRequest.getTranslationVersion(tag_id, OctopusApplication.MASTER_LANGUAGE);
            if (versionMainLanguage == null) {
                versionMainLanguage = "0";
            }
            if (increaseVersion) {
                int i = Integer.parseInt(versionMainLanguage);
                i++;
                versionMainLanguage = String.valueOf(i);
            }
        } else {
            return resultUpdateDescription;
        }
        return doUpdateTranslation(tag_id, current_language, edit_translation, user_id, versionMainLanguage);
    }

    public static String updateTranslation(String tag_id, String current_language, String edit_translation, String user_id) {
        if (tag_id == null || tag_id.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        if (current_language == null || current_language.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        if (user_id == null || user_id.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        if (edit_translation != null && !edit_translation.trim().equals("")) {
            edit_translation = StringUtil.replace(edit_translation, "\n", " ");
            edit_translation = StringUtil.replace(edit_translation, "\r", " ");
            edit_translation = StringUtil.replace(edit_translation, "\t", " ");
            edit_translation = StringUtil.replace(edit_translation, "<", "&#60;");
            edit_translation = StringUtil.replace(edit_translation, ">", "&#62;");
            edit_translation = StringUtil.replace(edit_translation, "'", "&#39;");
        } else {
            return OctopusErrorMessages.TRANSLATION_TEXT_EMPTY;
        }
        String versionMainLanguage = OctopusRequest.getTranslationVersion(tag_id, OctopusApplication.MASTER_LANGUAGE);
        if (versionMainLanguage == null) {
            versionMainLanguage = "0";
        }
        String versionTranslation = OctopusRequest.getTranslationVersion(tag_id, current_language);
        if (versionTranslation == null) {
            return doInsertTranslation(tag_id, current_language, edit_translation, user_id, versionMainLanguage);
        } else {
            return doUpdateTranslation(tag_id, current_language, edit_translation, user_id, versionMainLanguage);
        }
    }

    private static String doInsertTranslation(String tag_id, String current_language, String edit_translation, String user_id, String version) {
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            String query = "INSERT INTO tr_translation ( ";
            query += "tr_translation_trtagid, ";
            query += "tr_translation_language, ";
            query += "tr_translation_text, ";
            query += "tr_translation_version, ";
            query += "tr_translation_lud, ";
            query += "tr_translation_lun ";
            query += ") ";
            query += "VALUES('";
            query += tag_id + "','";
            query += current_language + "','";
            query += edit_translation + "',";
            query += version + ",";
            query += "getdate(),'";
            query += user_id + "')";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.executeUpdate();
            so = OctopusErrorMessages.ACTION_DONE;
        } catch (SQLException e) {
            so = OctopusErrorMessages.ERROR_DATABASE;
        } finally {
            if (theConnection != null) theConnection.release();
        }
        return so;
    }

    private static String doUpdateTranslation(String tag_id, String current_language, String edit_translation, String user_id, String version) {
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            String query = "UPDATE tr_translation SET ";
            query += "tr_translation_text='" + edit_translation + "', ";
            query += "tr_translation_lud=getdate(), ";
            query += "tr_translation_lun='" + user_id + "', ";
            query += "tr_translation_version=" + version + " ";
            query += "WHERE tr_translation_trtagid='" + tag_id + "' ";
            query += "AND tr_translation_language='" + current_language + "'";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.executeUpdate();
            so = OctopusErrorMessages.ACTION_DONE;
        } catch (SQLException e) {
            so = OctopusErrorMessages.ERROR_DATABASE;
            e.printStackTrace();
        } finally {
            if (theConnection != null) theConnection.release();
        }
        return so;
    }

    private static String updateDescription(String tag_id, String edit_desc) {
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            String query = "UPDATE tr_tag SET tr_tag_info='" + edit_desc + "' WHERE tr_tag_id=?";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.setString(1, tag_id);
            state.executeUpdate();
            so = OctopusErrorMessages.ACTION_DONE;
        } catch (SQLException e) {
            so = OctopusErrorMessages.ERROR_DATABASE;
        } finally {
            if (theConnection != null) theConnection.release();
        }
        return so;
    }

    public static String addTag(String tag_id, String tag_description, String tag_text, String tag_author, String application_code) {
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        if (tag_id == null || tag_id.trim().equals("")) {
            return OctopusErrorMessages.TAG_ID_CANT_BE_EMPTY;
        }
        if (tag_id.trim().equals(application_code)) {
            return OctopusErrorMessages.TAG_ID_TOO_SHORT;
        }
        if (!StringUtil.isAlphaNumerical(StringUtil.replace(StringUtil.replace(tag_id, "-", ""), "_", ""))) {
            return OctopusErrorMessages.TAG_ID_MUST_BE_ALPHANUMERIC;
        }
        if (!tag_id.startsWith(application_code)) {
            return OctopusErrorMessages.TAG_ID_MUST_START + " " + application_code;
        }
        String tag_exist = exist(tag_id);
        if (!tag_exist.equals(OctopusErrorMessages.DOESNT_ALREADY_EXIST)) {
            return tag_exist;
        }
        if (tag_description != null && !tag_description.trim().equals("")) {
            tag_description = StringUtil.replace(tag_description, "\n", " ");
            tag_description = StringUtil.replace(tag_description, "\r", " ");
            tag_description = StringUtil.replace(tag_description, "\t", " ");
            tag_description = StringUtil.replace(tag_description, "<", "&#60;");
            tag_description = StringUtil.replace(tag_description, ">", "&#62;");
            tag_description = StringUtil.replace(tag_description, "'", "&#39;");
        } else {
            return OctopusErrorMessages.DESCRIPTION_TEXT_EMPTY;
        }
        if (tag_text != null && !tag_text.trim().equals("")) {
            tag_text = StringUtil.replace(tag_text, "\n", " ");
            tag_text = StringUtil.replace(tag_text, "\r", " ");
            tag_text = StringUtil.replace(tag_text, "\t", " ");
            tag_text = StringUtil.replace(tag_text, "<", "&#60;");
            tag_text = StringUtil.replace(tag_text, ">", "&#62;");
            tag_text = StringUtil.replace(tag_text, "'", "&#39;");
        } else {
            return OctopusErrorMessages.TRANSLATION_TEXT_EMPTY;
        }
        if (tag_author == null || tag_author.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            theConnection.setAutoCommit(false);
            String query = "INSERT INTO tr_tag (tr_tag_id,tr_tag_applicationid,tr_tag_info,tr_tag_creationdate) ";
            query += "VALUES (?,?,'" + tag_description + "',getdate())";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.setString(1, tag_id);
            state.setString(2, application_code);
            state.executeUpdate();
            String query2 = "INSERT INTO tr_translation (tr_translation_trtagid, tr_translation_language, tr_translation_text, tr_translation_version, tr_translation_lud, tr_translation_lun ) ";
            query2 += "VALUES(?,'" + OctopusApplication.MASTER_LANGUAGE + "','" + tag_text + "',0,getdate(),?)";
            PreparedStatement state2 = theConnection.prepareStatement(query2);
            state2.setString(1, tag_id);
            state2.setString(2, tag_author);
            state2.executeUpdate();
            theConnection.commit();
            so = OctopusErrorMessages.ACTION_DONE;
        } catch (SQLException e) {
            try {
                theConnection.rollback();
            } catch (SQLException ex) {
            }
            so = OctopusErrorMessages.ERROR_DATABASE;
        } finally {
            if (theConnection != null) {
                try {
                    theConnection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
                theConnection.release();
            }
        }
        return so;
    }

    public static String exist(String tag_id) {
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            String query = "SELECT * FROM tr_tag WHERE tr_tag_id=?";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.setString(1, tag_id);
            if (state == null) {
                return OctopusErrorMessages.UNKNOWN_ERROR;
            }
            ResultSet rs = state.executeQuery();
            if (rs.next()) {
                so = OctopusErrorMessages.ALREADY_EXIST;
            } else {
                so = OctopusErrorMessages.DOESNT_ALREADY_EXIST;
            }
        } catch (SQLException e) {
            so = OctopusErrorMessages.ERROR_DATABASE;
        } finally {
            if (theConnection != null) theConnection.release();
        }
        return so;
    }

    public static String deleteTag(String tag_id) {
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        if (tag_id == null || tag_id.trim().equals("")) {
            return OctopusErrorMessages.TAG_ID_CANT_BE_EMPTY;
        }
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            theConnection.setAutoCommit(false);
            String query = "DELETE FROM tr_translation WHERE tr_translation_trtagid=?";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.setString(1, tag_id);
            state.executeUpdate();
            String query2 = "DELETE FROM tr_tag WHERE tr_tag_id=? ";
            PreparedStatement state2 = theConnection.prepareStatement(query2);
            state2.setString(1, tag_id);
            state2.executeUpdate();
            theConnection.commit();
            so = OctopusErrorMessages.ACTION_DONE;
        } catch (SQLException e) {
            try {
                theConnection.rollback();
            } catch (SQLException ex) {
            }
            so = OctopusErrorMessages.ERROR_DATABASE;
        } finally {
            if (theConnection != null) {
                try {
                    theConnection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
                theConnection.release();
            }
        }
        return so;
    }
}
