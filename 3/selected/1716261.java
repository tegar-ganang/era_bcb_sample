package com.googlecode.openmpis.action;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import com.googlecode.openmpis.dto.Log;
import com.googlecode.openmpis.dto.User;
import com.googlecode.openmpis.form.PasswordForm;
import com.googlecode.openmpis.persistence.ibatis.dao.impl.LogDAOImpl;
import com.googlecode.openmpis.persistence.ibatis.dao.impl.UserDAOImpl;
import com.googlecode.openmpis.persistence.ibatis.service.LogService;
import com.googlecode.openmpis.persistence.ibatis.service.UserService;
import com.googlecode.openmpis.persistence.ibatis.service.impl.LogServiceImpl;
import com.googlecode.openmpis.persistence.ibatis.service.impl.UserServiceImpl;
import com.googlecode.openmpis.util.Configuration;
import com.googlecode.openmpis.util.Constants;
import com.googlecode.openmpis.util.Mail;
import com.googlecode.openmpis.util.Validator;

/**
 * The PasswordAction class provides the method to reset a user's password.
 * 
 * @author  <a href="mailto:rvbabilonia@gmail.com">Rey Vincent Babilonia</a>
 * @version 1.0
 */
public class PasswordAction extends Action {

    /**
     * The file logger
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * This is the action called from the Struts framework.
     * 
     * @param mapping       the ActionMapping used to select this instance
     * @param form          the optional ActionForm bean for this request
     * @param request       the HTTP Request we are processing
     * @param response      the HTTP Response we are processing
     * @return              the forwarding instance
     * @throws java.lang.Exception
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserService userService = new UserServiceImpl(new UserDAOImpl());
        LogService logService = new LogServiceImpl(new LogDAOImpl());
        PasswordForm passwordForm = (PasswordForm) form;
        if (isValidAccount(request, form)) {
            User user = userService.getUserByUsername(passwordForm.getUsername());
            String forward = "";
            if (user == null) {
                ActionMessages errors = new ActionMessages();
                errors.add("user", new ActionMessage("error.login.invalid"));
                saveErrors(request, errors);
                return mapping.findForward(Constants.PASSWORD_REDO);
            } else {
                if ((passwordForm.getQuestion() == user.getQuestion()) && (passwordForm.getAnswer().equals(user.getAnswer()))) {
                    request.setAttribute("email", user.getEmail());
                    String password = "op3nmp!s";
                    user.setPassword(encryptPassword(password));
                    Log resetLog = new Log();
                    resetLog.setLog("User " + user.getUsername() + " reset his password.");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    resetLog.setDate(sdf.format(System.currentTimeMillis()));
                    userService.updatePassword(user);
                    logService.insertLog(resetLog);
                    logger.info(resetLog.toString());
                    Configuration config = new Configuration("mail.properties");
                    if (Boolean.parseBoolean(config.getProperty("mail.enable"))) {
                        Mail mail = new Mail();
                        mail.send("Administrator", "", config.getProperty("mail.administrator"), user.getEmail(), "Password Retrieval", "Dear " + user.getFirstName() + "," + "\n\nYour new password is " + password + ". You received this email because " + "you have forgotten your password or someone pretending to be you " + "is trying to log into the system." + "\n\nYours truly," + "\nAdministrator");
                    }
                    forward = Constants.PASSWORD_SUCCESS;
                } else {
                    ActionMessages errors = new ActionMessages();
                    errors.add("question", new ActionMessage("error.question.invalid"));
                    saveErrors(request, errors);
                    logger.info("Invalid password or answer for user " + passwordForm.getUsername() + " from " + request.getRemoteAddr() + ".");
                    forward = Constants.PASSWORD_REDO;
                }
                return mapping.findForward(forward);
            }
        } else {
            logger.info("Invalid password retrieval credentials from " + request.getRemoteAddr() + ".");
            return mapping.findForward(Constants.PASSWORD_REDO);
        }
    }

    /**
     * Validates the inputs from the user form.
     * 
     * @param request       the HTTP Request we are processing
     * @param form          the ActionForm bean for this request
     * @return              <code>true</code> if there are no errors in the form; <code>false</code> otherwise
     */
    private boolean isValidAccount(HttpServletRequest request, ActionForm form) {
        ActionMessages errors = new ActionMessages();
        Validator validator = new Validator();
        boolean isValid = true;
        PasswordForm passwordForm = (PasswordForm) form;
        String username = passwordForm.getUsername();
        String answer = passwordForm.getAnswer();
        if (username.length() < 1) {
            errors.add("username", new ActionMessage("error.username.required"));
        } else {
            if (!validator.isValidUsername(username)) {
                errors.add("username", new ActionMessage("error.username.invalid"));
            }
        }
        if (answer.length() < 1) {
            errors.add("answer", new ActionMessage("error.answer.required"));
        } else {
            if (!validator.isValidKeyword(answer)) {
                errors.add("answer", new ActionMessage("error.answer.invalid"));
            }
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            isValid = false;
        }
        return isValid;
    }

    /**
     * Creates an MD5-encrypted password.
     * Adapted from http://snipplr.com/view/4321/generate-md5-hash-from-string/.
     *
     * @param password      the password
     * @return              the 32 alphanumeric-equivalent of the password
     * @throws java.security.NoSuchAlgorithmException
     */
    private String encryptPassword(String password) throws NoSuchAlgorithmException {
        StringBuffer encryptedPassword = new StringBuffer();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(password.getBytes());
        byte digest[] = md5.digest();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                encryptedPassword.append('0');
            }
            encryptedPassword.append(hex);
        }
        return encryptedPassword.toString();
    }
}
