package com.i3sp.sso;

import com.mortbay.Util.Code;
import com.mortbay.Util.UrlEncoded;
import com.i3sp.util.logging.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;

/** Access the auth key sent to authenticate the login to the client site.
 * <p> The Auth key consists of the user id and an auth token, separated by a
 * ':'. The auth token is generated by taking the user id and appending the
 * secretKey (at the moment, hard coded in this class - not very good!) and
 * MD5ing them.
 *
 * <p><h4>Notes</h4>
 * <p> Must later provide a better way of looking up the secret key, but
 * should really be using private key signing to authenticate the sso server.
 *
 * @version $Revision: 632 $ $Date: 2001-03-13 17:51:58 -0500 (Tue, 13 Mar 2001) $
 * @author  (mattw)
 */
public class AuthKey {

    public static class AuthKeyException extends Exception {

        private Exception ex;

        public AuthKeyException(String desc, Exception ex) {
            super(desc);
            this.ex = ex;
        }

        public Exception getContainedException() {
            return ex;
        }
    }

    private MessageDigest digest;

    private String authKey;

    private String id;

    private boolean authentic;

    private String oneTimeToken;

    private static final String secretKey = "dhsfkadfshdjshfkdshfkjsdhfsdrytjerluoifjlkdkgjfd m,3h598cn";

    private AuthKey(String authKey, String oneTimeToken, boolean unused) throws AuthKeyException {
        this.oneTimeToken = oneTimeToken;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Log.event(Logs.noMD5, ex);
            throw new AuthKeyException("Getting MD5 MessageDigest", ex);
        }
        int colon = authKey.indexOf(":");
        if (colon == -1) {
            authentic = false;
            return;
        }
        id = authKey.substring(0, colon);
        String key = authKey.substring(colon + 1);
        Code.debug("id:", id, "; token:", UrlEncoded.encodeString(oneTimeToken), "; authKey:", UrlEncoded.encodeString(authKey), "; generated:", UrlEncoded.encodeString(generateAuthToken(id)));
        authentic = (id != null && key.equals(generateAuthToken(id)));
        if (!authentic) id = null;
    }

    private AuthKey(String id, String oneTimeToken) throws AuthKeyException {
        this.oneTimeToken = oneTimeToken;
        this.id = id;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Log.event(Logs.noMD5, ex);
            throw new AuthKeyException("Getting MD5 MessageDigest", ex);
        }
        Code.debug("id:", id, "; token:", UrlEncoded.encodeString(oneTimeToken), "; generated:", UrlEncoded.encodeString(generateAuthToken(id)));
        authentic = true;
    }

    public static AuthKey decode(String authKey, String oneTimeToken) throws AuthKeyException {
        return new AuthKey(authKey, oneTimeToken, true);
    }

    public static AuthKey encode(String id, String oneTimeToken) throws AuthKeyException {
        return new AuthKey(id, oneTimeToken);
    }

    public String getId() {
        return id;
    }

    public boolean isAuthentic() {
        return authentic;
    }

    public String getValue(boolean URLencoded) {
        return id + ":" + (URLencoded ? UrlEncoded.encodeString(generateAuthToken(id)) : generateAuthToken(id));
    }

    private synchronized String generateAuthToken(String name) {
        String clearText = name + oneTimeToken + secretKey;
        digest.reset();
        byte authToken[] = digest.digest(clearText.getBytes());
        return new String(authToken);
    }
}
