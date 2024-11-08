package com.jon.android.drupaldroid.services;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * XML-RPC client for the server offered by Drupal. Drupal has a weird
 * authentication scheme on the server side involving to sign every method call
 * using a HMAC signature generated with SHA256 hashing and the API key of
 * Drupal as key. This class implements this authentication scheme and some
 * method calls based on the apache XML-RPC implementation.
 * 
 * Usage:
 * 
 * <pre>
 * DrupalXmlRpcService service = new DrupalXmlRpcService(&quot;domain&quot;,
 * 		&quot;442c5629267cc4568ad43ceaa7f3dbe4&quot;,
 * 		&quot;http://www.example.com/drupal/?q=services/xmlrpc&quot;);
 * service.connect();
 * service.login(user, password);
 * service.nodeSave(mynode);
 * service.logout();
 * </pre>
 * 
 * see http://drupal.org/node/632844, with adaptations by Leo Sauermann
 * 
 * Changelog 16.2.2010 - made exceptions where exceptions are due, changed
 * logging to JUL
 * 
 * This class is written by Leo Sauermann on the basis of work published by
 * Aaron Moline.
 * 
 * It is currently part of the Aperture sourceforge project which is BSD
 * licensed, if you want to put it elsewhere, do so under this license.
 * 
 * @author Aaron Moline <Aaron.Moline@molinesoftware.com>
 * @author Leo Sauermann <leo.sauermann@dfki.de>
 */
public class DrupalXmlRpcService implements DrupalService {

    /**
	 * Method names
	 * 
	 * @author sauermann
	 */
    public static final String MethodNodeSave = "node.save";

    public static final String MethodNodeGet = "node.get";

    public static final String MethodSystemConnect = "system.connect";

    public static final String MethodUserLogout = "user.logout";

    public static final String MethodUserLogin = "user.login";

    public static final String MethodFileSave = "file.save";

    public static final String MethodTestCount = "test.count";

    Logger log = Logger.getLogger(DrupalXmlRpcService.class.getName());

    final String serviceURL;

    final String serviceDomain;

    final String apiKey;

    /**
	 * SessionID is set by "connect"
	 */
    String sessionID;

    XmlRpcClient xmlRpcClient;

    /**
	 * needed for signing
	 */
    final Charset asciiCs = Charset.forName("US-ASCII");

    /**
	 * Message authentification code algorithm already initialized with the
	 * ApiKey.
	 */
    Mac apikeyMac;

    /**
	 * Initialize the Drupal XML-RPC Service. The serviceURL must be a valid
	 * URL.
	 * 
	 * @param serviceDomain
	 *            domain
	 * @param apiKey
	 *            the API key with the domain
	 * @param serviceURL
	 *            the URL of the Drupal service. example:
	 *            http://www.example.com/drupal/?q=services/xmlrpc
	 * @throws MalformedURLException
	 *             if the serviceURL is not a URL
	 */
    public DrupalXmlRpcService(String serviceDomain, String apiKey, String serviceURL) throws MalformedURLException {
        this.serviceDomain = serviceDomain;
        this.apiKey = apiKey;
        this.serviceURL = serviceURL;
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(this.serviceURL));
        xmlRpcClient = new XmlRpcClient();
        xmlRpcClient.setConfig(config);
    }

    /**
	 * generate a random string for signing methods
	 * 
	 * @return
	 */
    private String generateNonce() {
        return "" + System.currentTimeMillis();
    }

    /**
	 * Generate the default parameters that need to precede every call
	 * 
	 * @param methodname
	 *            pass in the method name which should be signed.
	 * @return the default parameters. The Vector can be reused to pass other
	 *         objects via XML-RPC, hence the resulting vector is typed to
	 *         "Object" rather than String.
	 */
    private Vector<Object> generateDefaultParams(String methodname) throws Exception {
        String nonce = generateNonce();
        long timestamp = System.currentTimeMillis();
        String hashString = Long.toString(timestamp) + ";" + serviceDomain + ";" + nonce + ";" + methodname;
        String hash = generateHmacHash(hashString);
        Vector<Object> params = new Vector<Object>();
        params.add(hash);
        params.add(this.serviceDomain);
        params.add(Long.toString(timestamp));
        params.add(nonce);
        params.add(this.sessionID);
        return params;
    }

    /**
	 * Compute the HMAC-SHA256 hash of the passed message. As key, the API key
	 * is used.
	 * 
	 * @param message
	 *            the message to hash
	 * @return the hash as hex-encoded string.
	 * @throws Exception
	 *             if the encoding algorithm HmacSHA256 can't be found or other
	 *             problems arise.
	 */
    public String generateHmacHash(String message) throws Exception {
        byte[] hash = getApikeyMac().doFinal(asciiCs.encode(message).array());
        String result = "";
        for (int i = 0; i < hash.length; i++) {
            result += Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1);
        }
        log.finest("Created HMAC: " + result + " of " + message);
        return result;
    }

    /**
	 * Getter for HMAC, as this is used on every call, its good to buffer it. As
	 * the ApiKey is final, this can be buffered.
	 * 
	 * @return
	 */
    private Mac getApikeyMac() throws Exception {
        if (apikeyMac == null) {
            SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(asciiCs.encode(this.apiKey).array(), "HmacSHA256");
            apikeyMac = javax.crypto.Mac.getInstance("HmacSHA256");
            apikeyMac.init(keySpec);
        }
        return apikeyMac;
    }

    @Override
    public void connect() throws Exception {
        try {
            Map map = (Map) xmlRpcClient.execute(MethodSystemConnect, new Object[] {});
            this.sessionID = (String) map.get("sessid");
            log.fine("Connected to server using SessionID: " + this.sessionID);
        } catch (Exception x) {
            throw new Exception("cannot connect to " + serviceURL + ": " + x.getMessage(), x);
        }
    }

    @Override
    public void login(String username, String password) throws Exception {
        Vector<Object> params = generateDefaultParams(MethodUserLogin);
        params.add(username);
        params.add(password);
        Map o = (Map) xmlRpcClient.execute(MethodUserLogin, params);
        this.sessionID = (String) o.get("sessid");
        log.fine("Successfull Login");
    }

    @Override
    public void logout(String sessionID) throws Exception {
        Vector<Object> params = generateDefaultParams(MethodUserLogout);
        params.add(this.sessionID);
        xmlRpcClient.execute(MethodUserLogout, params);
        log.finer("Logout Sucessfull");
    }

    /**
	 * Call file.save. Pass in the file as byte-array. TODO: Leo says: This does
	 * not conform to the Drupal interface of file.save - its a leftover from
	 * Aaron's code.
	 */
    public void fileSave(byte[] file) throws Exception {
        Vector<Object> params = generateDefaultParams(MethodFileSave);
        params.add(file);
        Object o = xmlRpcClient.execute(MethodFileSave, params);
        if (log.isLoggable(Level.FINEST)) log.finest(MethodFileSave + " returned " + o.toString());
    }

    @Override
    public void nodeSave(DrupalNode node) throws Exception {
        Vector<Object> params = generateDefaultParams(MethodNodeSave);
        params.add(node);
        Object o = xmlRpcClient.execute(MethodNodeSave, params);
        if (log.isLoggable(Level.FINEST)) log.finest(MethodNodeSave + " returned " + o.toString());
    }

    @Override
    public DrupalNode nodeGet(DrupalNode node) throws Exception {
        DrupalNode retNode = new DrupalNode();
        Vector<Object> params = generateDefaultParams(MethodNodeGet);
        params.add(node);
        Object o = xmlRpcClient.execute(MethodNodeGet, params);
        retNode.setBody(o.toString());
        if (log.isLoggable(Level.FINEST)) log.finest(MethodNodeGet + " returned " + o.toString());
        return retNode;
    }

    public static void main(String[] args) throws Exception {
        DrupalService service = new DrupalXmlRpcService("localhost", "3cb91efcda4fd1d338aa07b611a7099a", "http://localhost/drupal/?q=services/xmlrpc");
        service.connect();
        service.login("root", "root");
        DrupalNode node = new DrupalNode();
        node.setType(DrupalNode.TYPE_STORY);
        node.setTitle("HEllo WORLD");
        node.setNid(1);
        DrupalNode retNode = service.nodeGet(node);
        System.out.println("retNode: " + retNode.toString());
        System.out.println("done");
    }

    @Override
    public int commentSave(DrupalNode node) throws Exception {
        return 0;
    }

    @Override
    public List<DrupalNode> commentLoadNodeComments(long nid, int count, int start) throws Exception {
        return null;
    }

    @Override
    public DrupalNode commentLoad(long cid) throws Exception {
        return null;
    }

    @Override
    public List<DrupalNode> viewsGet(String view_name, String display_id, String args, int offset, int limit) throws Exception {
        return null;
    }
}
