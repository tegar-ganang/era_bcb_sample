package net.sf.jradius.webservice;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import net.sf.jradius.client.RadiusClient;
import net.sf.jradius.client.auth.EAPAuthenticator;
import net.sf.jradius.dictionary.Attr_CHAPChallenge;
import net.sf.jradius.dictionary.Attr_CHAPPassword;
import net.sf.jradius.dictionary.Attr_EAPMessage;
import net.sf.jradius.dictionary.Attr_MessageAuthenticator;
import net.sf.jradius.dictionary.Attr_UserName;
import net.sf.jradius.dictionary.Attr_UserPassword;
import net.sf.jradius.dictionary.vsa_microsoft.Attr_MSCHAPChallenge;
import net.sf.jradius.exception.RadiusException;
import net.sf.jradius.log.RadiusLog;
import net.sf.jradius.packet.AccessRequest;
import net.sf.jradius.packet.RadiusPacket;
import net.sf.jradius.packet.attribute.AttributeDictionary;
import net.sf.jradius.packet.attribute.AttributeFactory;
import net.sf.jradius.packet.attribute.AttributeList;
import net.sf.jradius.packet.attribute.RadiusAttribute;
import net.sf.jradius.realm.JRadiusRealm;
import net.sf.jradius.util.Base64;
import net.sf.jradius.util.RadiusRandom;

/**
 * OTP Proxy Web Service Request. This thread give the client a one-time
 * username and password and does the EAP proxy in a RadiusClient for the request.
 *
 * @author David Bird
 */
public class OTPProxyRequest extends Thread {

    private String userName;

    private JRadiusRealm radiusRealm;

    private String otpName;

    private String otpPassword;

    private Socket socket;

    private BufferedReader reader;

    private BufferedWriter writer;

    private RadiusClient radiusClient;

    private RadiusPacket accessRequest;

    private RadiusPacket accessResponse;

    private long timeout = 20000;

    public OTPProxyRequest(String userName, JRadiusRealm realm, Socket socket, BufferedReader reader, BufferedWriter writer) throws OTPProxyException {
        this.userName = userName;
        this.otpName = RadiusRandom.getRandomString(16);
        this.otpPassword = RadiusRandom.getRandomString(16);
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
        this.radiusRealm = realm;
        try {
            radiusClient = new RadiusClient(InetAddress.getByName(this.radiusRealm.getServer()), this.radiusRealm.getSharedSecret());
        } catch (UnknownHostException e) {
            throw new OTPProxyException(e.getMessage());
        }
    }

    public void run() {
        try {
            writer.write(getOtpName());
            writer.write("\n");
            writer.write(getOtpPassword());
            writer.write("\n");
            writer.flush();
            synchronized (this) {
                if (accessRequest == null) {
                    wait(timeout);
                }
            }
            if (accessRequest == null) {
                RadiusLog.error("we never got the access request");
                abort();
                return;
            }
            AttributeList attrs = accessRequest.getAttributes();
            attrs.remove(Attr_UserName.TYPE);
            attrs.remove(Attr_UserPassword.TYPE);
            attrs.remove(Attr_CHAPChallenge.TYPE);
            attrs.remove(Attr_CHAPPassword.TYPE);
            attrs.remove(Attr_MSCHAPChallenge.TYPE);
            attrs.remove(Attr_EAPMessage.TYPE);
            attrs.remove(Attr_MessageAuthenticator.TYPE);
            attrs.add(new Attr_UserName(userName));
            AccessRequest realRequest = new AccessRequest(radiusClient, attrs);
            RadiusLog.debug("------------------------------------------------\n" + "OTP Proxy Request:\n" + realRequest.toString() + "------------------------------------------------\n");
            accessResponse = radiusClient.authenticate(realRequest, new EAPRelayAuthenticator(), 5);
            synchronized (this) {
                notify();
            }
        } catch (Exception e) {
            e.printStackTrace();
            abort();
        }
    }

    /**
     * @return Returns the otpName.
     */
    public String getOtpName() {
        return otpName;
    }

    /**
     * @return Returns the otpPassword.
     */
    public String getOtpPassword() {
        return otpPassword;
    }

    /**
     * @return Returns the userName.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return Returns the realm.
     */
    public JRadiusRealm getRadiusRealm() {
        return radiusRealm;
    }

    /**
     * @param accessRequest The accessRequest to set.
     */
    public void setAccessRequest(RadiusPacket accessRequest) {
        synchronized (this) {
            this.accessRequest = accessRequest;
            notify();
        }
    }

    public RadiusPacket getAccessResponse() {
        try {
            synchronized (this) {
                if (accessResponse == null) {
                    wait(timeout);
                }
            }
            if (accessResponse == null) {
                RadiusLog.error("we never got the access response");
                abort();
            }
            return accessResponse;
        } catch (InterruptedException e) {
            abort();
        }
        return null;
    }

    private byte[] readData() {
        try {
            String line = reader.readLine();
            if (line.startsWith("eap:")) {
                return Base64.decode(line.substring(4));
            }
        } catch (Exception e) {
            abort();
        }
        return null;
    }

    private byte[] relayEAP(byte[] eapIn) {
        try {
            writer.write("eap:");
            writer.write(Base64.encodeBytes(eapIn, Base64.DONT_BREAK_LINES));
            writer.write("\n");
            writer.flush();
            return readData();
        } catch (IOException e) {
            abort();
        }
        return null;
    }

    public void abort() {
        OTPProxyProcessor.remove(this);
        try {
            writer.close();
            reader.close();
            socket.close();
        } catch (IOException e) {
        }
    }

    private class EAPRelayAuthenticator extends EAPAuthenticator {

        public void processRequest(RadiusPacket p) throws RadiusException {
            RadiusAttribute a = AttributeFactory.newAttribute(AttributeDictionary.EAP_MESSAGE, readData());
            p.overwriteAttribute(a);
        }

        protected byte[] doEAP(byte[] eapReply) {
            return relayEAP(eapReply);
        }

        public byte[] doEAPType(byte id, byte[] data) {
            return null;
        }

        public String getAuthName() {
            return "OTPProxy-EAP-Callback";
        }
    }
}
