package octopus.requests;

import java.sql.PreparedStatement;
import java.sql.*;
import hambo.util.StringUtil;
import hambo.svc.database.*;
import hambo.util.StringUtil;
import octopus.tools.Messages.OctopusErrorMessages;

/**
* Class used to handles Languages Requests
* 
*/
public class LanguageRequestFactory {

    /**
     * Method used to Add a New Language
     * 
     * @param _code         Code of the new Language
     * @param _label        Label of the new Language
     * @param _author       User name who create the New Language
     * @return              An OctopusErrorMessages Message 
     */
    public static String addLanguage(String _code, String _label, String _author) {
        if (_code == null || _code.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        } else {
            _code = _code.toUpperCase();
        }
        if (!StringUtil.isAlphaNumerical(_code)) {
            return OctopusErrorMessages.CODE_MUST_BE_ALPHANUMERIC;
        }
        if (_label == null || _label.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        if (_author == null || _author.trim().equals("")) {
            return OctopusErrorMessages.MAIN_PARAMETER_EMPTY;
        }
        String test_code = LanguageExist("code", _code);
        if (!test_code.equals(OctopusErrorMessages.DOESNT_ALREADY_EXIST)) {
            return OctopusErrorMessages.LANGUAGE_CODE_TAKEN;
        }
        String test_label = LanguageExist("label", _label);
        if (!test_label.equals(OctopusErrorMessages.DOESNT_ALREADY_EXIST)) {
            return OctopusErrorMessages.LANGUAGE_LABEL_TAKEN;
        }
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            String query = "INSERT INTO tr_language (tr_language_label,tr_language_code) VALUES (?,?)";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.setString(1, _label);
            state.setString(2, _code);
            state.executeUpdate();
            so = OctopusErrorMessages.ACTION_DONE;
        } catch (SQLException e) {
            so = OctopusErrorMessages.ERROR_DATABASE;
        } finally {
            if (theConnection != null) theConnection.release();
        }
        return so;
    }

    /**
     * Method used to check if an Language already exists with a specific label or a specific code
     * 
     * @param type       label or code according to what you want to test
     * @param appl       Parameter of the new Application to check
     * @return           An OctopusErrorMessages Message 
     */
    public static String LanguageExist(String type, String appl) {
        String so = OctopusErrorMessages.UNKNOWN_ERROR;
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            String query = "";
            PreparedStatement state = null;
            if (type.equals("label")) {
                query += "SELECT * FROM tr_language WHERE tr_language_label=?";
                state = theConnection.prepareStatement(query);
                state.setString(1, appl);
            } else if (type.equals("code")) {
                query += "SELECT * FROM tr_language WHERE tr_language_code=?";
                state = theConnection.prepareStatement(query);
                state.setString(1, appl);
            } else {
                return OctopusErrorMessages.UNKNOWN_ERROR;
            }
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

    /**
     * Method used to Delete an Language<BR>
     * Remove all the Translation linked to the specific Language Code and the Language    
     * 
     * @param applicationExtension      Code of the Application to delete
     * @return                          An OctopusErrorMessages Message 
     */
    public static void deleteLanguage(String LanguageLabel) {
        DBConnection theConnection = null;
        try {
            theConnection = DBServiceManager.allocateConnection();
            theConnection.setAutoCommit(false);
            String query = "DELETE FROM tr_translation WHERE tr_translation_language =?";
            PreparedStatement state = theConnection.prepareStatement(query);
            state.setString(1, LanguageLabel);
            state.executeUpdate();
            String query2 = "DELETE FROM tr_language WHERE tr_language_label=?";
            PreparedStatement state2 = theConnection.prepareStatement(query2);
            state2.setString(1, LanguageLabel);
            state2.executeUpdate();
            theConnection.commit();
        } catch (SQLException e) {
            try {
                theConnection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            if (theConnection != null) {
                try {
                    theConnection.setAutoCommit(true);
                } catch (SQLException ex) {
                }
                theConnection.release();
            }
        }
    }
}
