package edu.vt.middleware.password;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>PasswordSourceRule</code> contains methods for determining if a
 * password matches a password from a different source. Useful for when separate
 * systems cannot have matching passwords.
 *
 * @author  Middleware Services
 * @version  $Revision: 1253 $ $Date: 2010-04-20 10:54:19 -0400 (Tue, 20 Apr 2010) $
 */
public class PasswordSourceRule extends AbstractDigestRule {

    /** password sources. */
    private Map<String, String> sources = new HashMap<String, String>();

    /**
   * This will add the supplied password as a password source.
   *
   * @param  source  <code>String</code> label
   * @param  password  <code>String</code> to add
   */
    public void addSource(final String source, final String password) {
        if (source != null && password != null) {
            this.sources.put(source, password);
        }
    }

    /** {@inheritDoc} */
    public boolean verifyPassword(final Password password) {
        boolean success = false;
        if (this.sources.size() == 0) {
            success = true;
        } else {
            for (Map.Entry<String, String> entry : this.sources.entrySet()) {
                final String p = entry.getValue();
                if (this.digest != null) {
                    final String hash = this.digest.digest(password.getText().getBytes(), this.converter);
                    if (p.equals(hash)) {
                        success = false;
                        this.setMessage(String.format("Password can not be the same as your %s password", entry.getKey()));
                        break;
                    } else {
                        success = true;
                    }
                } else {
                    if (p.equals(password.getText())) {
                        success = false;
                        this.setMessage(String.format("Password can not be the same as your %s password", entry.getKey()));
                        break;
                    } else {
                        success = true;
                    }
                }
            }
        }
        return success;
    }
}
