package net.sf.jradius.webservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.sf.jradius.exception.RadiusException;
import net.sf.jradius.realm.JRadiusRealm;
import net.sf.jradius.realm.JRadiusRealmManager;
import net.sf.jradius.server.ListenerRequest;
import net.sf.jradius.server.Processor;

/**
 * @author David Bird
 */
public class OTPProxyProcessor extends Processor {

    private static Map requests = Collections.synchronizedMap(new HashMap());

    protected void processRequest(ListenerRequest listenerRequest) throws IOException, RadiusException {
        Socket socket = listenerRequest.getSocket();
        socket.setSoTimeout(20000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String userName = reader.readLine();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        String realmName = realmFromUserName(userName);
        JRadiusRealm realm = JRadiusRealmManager.get(realmName);
        if (realm == null) throw new OTPProxyException("no such realm: " + realmName);
        OTPProxyRequest request = new OTPProxyRequest(userName, realm, socket, reader, writer);
        request.start();
        put(request);
    }

    protected String realmFromUserName(String username) throws OTPProxyException {
        int idx;
        if ((idx = username.indexOf("/")) > 0 || (idx = username.indexOf("\\")) > 0) {
            return username.substring(0, idx);
        }
        if ((idx = username.indexOf("@")) > 0) {
            return username.substring(idx + 1);
        }
        throw new OTPProxyException("no realm");
    }

    public static void remove(OTPProxyRequest request) {
        request.interrupt();
        requests.remove(request.getOtpName());
    }

    public static void put(OTPProxyRequest request) {
        requests.put(request.getOtpName(), request);
    }

    public static OTPProxyRequest get(String username) {
        return (OTPProxyRequest) requests.get(username);
    }
}
