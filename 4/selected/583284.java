package ch.unibe.id.se.a3ublogin.view.jsphelpers;

import java.util.Enumeration;
import java.util.HashMap;
import ch.unibe.id.se.a3ublogin.beans.A3ubLoginBucketBean_v01;
import ch.unibe.id.se.a3ublogin.business.BusinessManager;
import ch.unibe.id.se.a3ublogin.exceptions.BusinessException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EnterEmailLogic {

    private Log log = null;

    /** instance of the singleton */
    private static EnterEmailLogic instance = null;

    /** returns the singleton */
    public static synchronized EnterEmailLogic getInstance() {
        if (instance == null) instance = new EnterEmailLogic();
        return instance;
    }

    /** privat constructor */
    private EnterEmailLogic() {
        this.log = LogFactory.getLog(getClass());
        if (log.isInfoEnabled()) {
            log.info("log for LoginLogic created: " + getClass());
        }
    }

    public String getEmail(A3ubLoginBucketBean_v01 bean) {
        if (bean == null) {
            return "";
        }
        String mail = ch.unibe.id.se.a3ublogin.persistence.readersandwriters.UniLdapReader.getInstance().getEmail(bean.getLoginbean().getCommonName());
        return mail;
    }
}
