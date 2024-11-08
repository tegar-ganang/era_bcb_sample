package org.libreplan.web.users;

import org.libreplan.business.common.Registry;
import org.libreplan.business.users.entities.User;
import org.libreplan.web.users.bootstrap.MandatoryUser;
import org.zkoss.zk.ui.util.Clients;

/**
 * A class which is used to encapsulate some common behaviour of passwords.
 *
 * @author Cristina Alvarino Perez <cristina.alvarino@comtecsf.es>
 * @author Ignacio Diaz Teijido <ignacio.diaz@comtecsf.es>
 */
public class PasswordUtil {

    public static void checkIfChangeDefaultPasswd(User user, String clearPassword) {
        if (user.getLoginName().equalsIgnoreCase(MandatoryUser.ADMIN.getLoginName())) {
            checkIfChangeDefaultPasswd(MandatoryUser.ADMIN, clearPassword);
            return;
        }
        if (user.getLoginName().equalsIgnoreCase(MandatoryUser.USER.getLoginName())) {
            checkIfChangeDefaultPasswd(MandatoryUser.USER, clearPassword);
            return;
        }
        if (user.getLoginName().equalsIgnoreCase(MandatoryUser.WSREADER.getLoginName())) {
            checkIfChangeDefaultPasswd(MandatoryUser.WSREADER, clearPassword);
            return;
        }
        if (user.getLoginName().equalsIgnoreCase(MandatoryUser.WSWRITER.getLoginName())) {
            checkIfChangeDefaultPasswd(MandatoryUser.WSWRITER, clearPassword);
            return;
        }
    }

    private static void checkIfChangeDefaultPasswd(MandatoryUser user, String clearPassword) {
        boolean changedPasswd = true;
        if (clearPassword.isEmpty() || clearPassword.equals(user.getClearPassword())) {
            changedPasswd = false;
        }
        Registry.getConfigurationDAO().saveChangedDefaultPassword(user.getLoginName(), changedPasswd);
    }

    /**
     * It calls a JavaScript method called
     * <b>showOrHideDefaultPasswordWarnings</b> defined in
     * "/libreplan-webapp/js/defaultPasswordWarnings.js" to show or hide the
     * default password warnings if the user has changed the password or has
     * been disabled
     */
    public static void showOrHideDefaultPasswordWarnings() {
        boolean adminNotDefaultPassword = MandatoryUser.ADMIN.hasChangedDefaultPasswordOrDisabled();
        boolean userNotDefaultPassword = MandatoryUser.USER.hasChangedDefaultPasswordOrDisabled();
        boolean wsreaderNotDefaultPassword = MandatoryUser.WSREADER.hasChangedDefaultPasswordOrDisabled();
        boolean wswriterNotDefaultPassword = MandatoryUser.WSWRITER.hasChangedDefaultPasswordOrDisabled();
        Clients.evalJavaScript("showOrHideDefaultPasswordWarnings(" + adminNotDefaultPassword + ", " + userNotDefaultPassword + ", " + wsreaderNotDefaultPassword + ", " + wswriterNotDefaultPassword + ");");
    }
}
