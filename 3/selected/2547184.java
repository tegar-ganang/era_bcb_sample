package org.opennebula.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

/**
 * This class represents the connection with the core and handles the
 * xml-rpc calls.
 *
 */
public class Client {

    /**
     * Creates a new xml-rpc client with default options:
     * the auth. file will be assumed to be at $ONE_AUTH, and
     * the endpoint will be set to $ONE_XMLRPC.
     * <br/>
     * It is the equivalent of Client(null, null).
     *
     * @throws Exception if one authorization file cannot be located.
     */
    public Client() throws Exception {
        setOneAuth(null);
        setOneEndPoint(null);
    }

    /**
     * Creates a new xml-rpc client with specified options.
     *
     * @param secret A string containing the ONE user:password tuple.
     * Can be null
     * @param endpoint Where the rpc server is listening, must be something
     * like "http://localhost:2633/RPC2". Can be null
     * @throws Exception if the authorization options are invalid
     */
    public Client(String secret, String endpoint) throws Exception {
        setOneAuth(secret);
        setOneEndPoint(endpoint);
    }

    /**
     * Performs an XML-RPC call.
     *
     * @param action ONE action
     * @param args ONE arguments
     * @return The server's xml-rpc response encapsulated
     */
    public OneResponse call(String action, Object... args) {
        boolean success = false;
        String msg = null;
        try {
            Object[] params = new Object[args.length + 1];
            params[0] = oneAuth;
            for (int i = 0; i < args.length; i++) params[i + 1] = args[i];
            Object[] result = (Object[]) client.execute("one." + action, params);
            success = (Boolean) result[0];
            if (result.length > 1) {
                try {
                    msg = (String) result[1];
                } catch (ClassCastException e) {
                    msg = ((Integer) result[1]).toString();
                }
            }
        } catch (XmlRpcException e) {
            msg = e.getMessage();
        }
        return new OneResponse(success, msg);
    }

    private String oneAuth;

    private String oneEndPoint;

    private XmlRpcClient client;

    private void setOneAuth(String secret) throws Exception {
        String oneSecret = secret;
        try {
            if (oneSecret == null) {
                String oneAuthEnv = System.getenv("ONE_AUTH");
                File authFile;
                if (oneAuthEnv != null && oneAuthEnv.length() != 0) {
                    authFile = new File(oneAuthEnv);
                } else {
                    authFile = new File(System.getenv("HOME") + "/.one/one_auth");
                }
                oneSecret = (new BufferedReader(new FileReader(authFile))).readLine();
            }
            String[] token = oneSecret.split(":");
            if (token.length != 2) {
                throw new Exception("Wrong format for authorization string: " + oneSecret + "\nFormat expected is user:password");
            }
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(token[1].getBytes());
            String hash = "";
            for (byte aux : digest) {
                int b = aux & 0xff;
                if (Integer.toHexString(b).length() == 1) {
                    hash += "0";
                }
                hash += Integer.toHexString(b);
            }
            oneAuth = token[0] + ":" + hash;
        } catch (FileNotFoundException e) {
            throw new Exception("ONE_AUTH file not present");
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Error initializing MessageDigest with SHA-1");
        }
    }

    private void setOneEndPoint(String endpoint) throws Exception {
        oneEndPoint = "http://localhost:2633/RPC2";
        if (endpoint != null) {
            oneEndPoint = endpoint;
        } else {
            String oneXmlRpcEnv = System.getenv("ONE_XMLRPC");
            if (oneXmlRpcEnv != null && oneXmlRpcEnv.length() != 0) {
                oneEndPoint = oneXmlRpcEnv;
            }
        }
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        try {
            config.setServerURL(new URL(oneEndPoint));
        } catch (MalformedURLException e) {
            throw new Exception("The URL " + oneEndPoint + " is malformed.");
        }
        client = new XmlRpcClient();
        client.setConfig(config);
    }
}
