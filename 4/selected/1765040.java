package net.javasource.net.tacacs;

import java.net.Socket;
import java.net.SocketException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.security.NoSuchAlgorithmException;

/**
 * Make sure that Sun's jce (1.2) jar is in the classpath.  The jar can be<BR>
 * obtained from Sun's website at:  http://java.sun.com/products/jce/index.html<BR>
 *<BR>
 * usage is very easy:<BR>
 *<BR>
 * Tacacs mytac = new Tacacs();<BR>
 * mytac.setHostname = "tacacsServer.mydomain.com";<BR>
 * mytac.setKey = "mysupersecretkey";<BR>
 * boolean authentic = mytac.isAuthenticated("UsersName","UsersPassword");<BR>
 *<BR>
 * that's it!  Enjoy, and if you have any questions - feel free to email me:<BR>
 * jay@javasource.net<BR>
 * <BR>
 * @author Jay Colson, Copyright 2000
 * @version v0.10, 06/13/2000
 */
public class Tacacs {

    private static final byte AUTHEN_PASS = (byte) 0x01;

    private static final byte AUTHEN_FAIL = (byte) 0x02;

    private static final byte AUTHEN_GETDATA = (byte) 0x03;

    private static final byte AUTHEN_GETUSER = (byte) 0x04;

    private static final byte AUTHEN_GETPASS = (byte) 0x05;

    private static final byte AUTHEN_RESTART = (byte) 0x06;

    private static final byte AUTHEN_ERROR = (byte) 0x07;

    private static final byte AUTHEN_FOLLOW = (byte) 0x21;

    private static final byte ZEROBYTE = (byte) 0x00;

    static final byte HEADERFLAG_UNENCRYPT = (byte) 0x01;

    private static final byte HEADERFLAG_SINGLECON = (byte) 0x04;

    public static final byte VERSION_13_0 = (byte) 0xc0;

    public static final byte VERSION_13_1 = (byte) 0xc1;

    public static final int PORT_STANDARD = 49;

    private byte headerFlags;

    private byte[] sessionID;

    private Integer tacacsSequence;

    private Byte version;

    private Integer port;

    private String hostname;

    private byte[] secretkey;

    private Socket theSocket = null;

    /**
   * Constructs a new Tacacs object.  No parameters. Sets up inital values for
   * variables in scope of class.
   */
    public Tacacs() {
        headerFlags = ZEROBYTE;
        version = new Byte(VERSION_13_0);
        port = new Integer(PORT_STANDARD);
        hostname = "";
        secretkey = "".getBytes();
    }

    /**
   * Sets the Hostname of your TacacsPlus server.  Remember, MOST (if not all) Tacacs+ servers
   * require you to designate fixed NAS ips that the server expects requests from, so if you
   * use this class on a machine, remember to set up the Tacacs+ server with the correct IP address.
   * (Pretend the machine that the class is running on is a NAS)
   * @param Hostname the hostname of your Tacacs+ server, should be a string, also
   * should be the FQDN of the server (ex:  myTacacsServer.mydomain.com)
   */
    public void setHostname(String Hostname) {
        hostname = Hostname;
    }

    /**
   * Sets the Version of Tacacs+ to use.  This is not treated any differently as
   * far as the class is concerned, however the packets sent and received need
   * to have a value, by default it is set to VERSION_13_00
   * @param Version this is the version, either Tacacs.VERSION_13_0 or Tacacs.VERSION_13_1
   */
    public void setVersion(byte Version) {
        version = new Byte(Version);
    }

    /**
   * Sets the secret key that the server and the client must know in order to decrypt packets.
   * @param SecretKey this is the secret key in String form that you have setup on your Tacacs+ Server
   */
    public void setKey(String SecretKey) {
        secretkey = SecretKey.getBytes();
    }

    /**
   * Sets the port number to talk to the Tacacs+ server over.  By default this is set to
   * Tacacs.PORT_STANDARD (49)
   * @param PortNumber a new port number to talk to the Tacacs server with, make sure you set this BEFORE
   * initiating any communications with the server (ie, RIGHT after instantiating the Tacacs() object).
   */
    public void setPortNumber(int PortNumber) {
        port = new Integer(PortNumber);
    }

    private void Connect() throws IOException {
        tacacsSequence = new Integer(1);
        sessionID = Header.generateSessionID();
        if (theSocket == null) {
            theSocket = new Socket(hostname, port.intValue());
        }
    }

    private void CloseConnection() throws IOException {
        if (theSocket != null) {
            theSocket.close();
            theSocket = null;
        }
        sessionID = null;
    }

    /**
   * This is the workhorse method.  It calls many private methods, connects to the Tacacs+ server and
   * determines whether or not the Username and Password are legit by returning a boolean.
   * @param Username the username to be authenticated
   * @param Password the password matching the username to be authenticated
   * @exception IOException will be thrown is there is a problem with writing to ByteArrayOutputStream or
   * the network socket.
   * @exception NoSuchAlgorithmException is thrown if the vm can not locate the MD5 algorithm (jce 1.2)
   * @return a true for successful authentication or false for unsuccessful
   */
    public synchronized boolean isAuthenticated(String Username, String Password) throws IOException, NoSuchAlgorithmException {
        if (Username.equals("")) {
            return false;
        }
        Connect();
        AuthSTART AS = new AuthSTART();
        AS.send(Username, Password);
        AuthREPLY AR = new AuthREPLY();
        AR.get();
        boolean exitLoop = false;
        while (AR.getStatus() != AUTHEN_PASS && AR.getStatus() != AUTHEN_FAIL && AR.getStatus() != AUTHEN_ERROR && AR.getStatus() != AUTHEN_FOLLOW && exitLoop != true) {
            synchronized (tacacsSequence) {
                int tmpSeqNum = tacacsSequence.intValue();
                tmpSeqNum++;
                tmpSeqNum++;
                tacacsSequence = new Integer(tmpSeqNum);
            }
            if (AR.REPLY_status == AUTHEN_GETDATA | AR.REPLY_status == AUTHEN_GETUSER) {
                AuthCONT AC = new AuthCONT();
                AC.send(Username);
                AR.get();
            } else if (AR.REPLY_status == AUTHEN_GETPASS) {
                AuthCONT AC = new AuthCONT();
                AC.send(Password);
                AR.get();
            } else if (AR.REPLY_status == AUTHEN_RESTART) {
                synchronized (tacacsSequence) {
                    tacacsSequence = new Integer(1);
                }
                AS.send(Username, Password);
                AR.get();
            } else if (tacacsSequence.intValue() > 5) {
                exitLoop = true;
            } else {
                exitLoop = true;
            }
        }
        this.CloseConnection();
        if (AR.REPLY_status == AUTHEN_PASS) {
            return true;
        } else {
            return false;
        }
    }

    private class AuthSTART {

        private byte ACTION_LOGIN = (byte) 0x01;

        private byte ACTION_CHPASS = (byte) 0x02;

        private byte ACTION_SENDAUTH = (byte) 0x04;

        private byte AUTHTYPE_ASCII = (byte) 0x01;

        private byte AUTHTYPE_PAP = (byte) 0x02;

        private byte AUTHTYPE_CHAP = (byte) 0x03;

        private byte AUTHTYPE_ARAP = (byte) 0x04;

        private byte AUTHTYPE_MSCHAP = (byte) 0x05;

        private byte PRIVLVL_MAX = (byte) 0x0f;

        private byte PRIVLVL_MIN = (byte) 0x00;

        private byte SERVICE_NONE = (byte) 0x00;

        private byte SERVICE_LOGIN = (byte) 0x01;

        private byte SERVICE_ENABLE = (byte) 0x02;

        private byte SERVICE_PPP = (byte) 0x03;

        private byte action = ACTION_LOGIN;

        private byte authtype = AUTHTYPE_ASCII;

        private byte privlvl = PRIVLVL_MIN;

        private byte service = SERVICE_NONE;

        private AuthSTART() {
        }

        private void send(String User, String Pass) throws IOException, NoSuchAlgorithmException {
            byte[] Username = User.getBytes();
            byte[] Data = Pass.getBytes();
            byte[] Port = "JAVA".getBytes();
            byte[] RemoteAdd = "Somewhere".getBytes();
            byte User_Len = (byte) Username.length;
            byte Port_Len = (byte) Port.length;
            byte Data_Len = (byte) Data.length;
            byte RemoteAdd_Len = (byte) RemoteAdd.length;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(action);
            baos.write(privlvl);
            baos.write(authtype);
            baos.write(service);
            baos.write(User_Len);
            baos.write(Port_Len);
            baos.write(RemoteAdd_Len);
            baos.write(Data_Len);
            baos.write(Username);
            baos.write(Port);
            baos.write(RemoteAdd);
            baos.write(Data);
            byte[] body = Header.crypt(version.byteValue(), tacacsSequence.byteValue(), baos.toByteArray(), headerFlags, sessionID, secretkey);
            baos.reset();
            byte[] header = Header.makeHeader(body, version, Header.TYPE_AUTHENTIC, tacacsSequence, headerFlags, sessionID);
            baos.write(header);
            baos.write(body);
            baos.writeTo(theSocket.getOutputStream());
        }
    }

    private class AuthCONT {

        private byte FLAG_ABORT = 0x01;

        private AuthCONT() {
        }

        private void send(String UserMsgData) throws IOException, NoSuchAlgorithmException {
            byte[] UserMsg = UserMsgData.getBytes();
            byte[] CONT_data = "NONE".getBytes();
            byte CONT_Flags = ZEROBYTE;
            byte[] UserMsg_Len = Bytes.ShorttoBytes((short) UserMsg.length);
            byte[] CONT_data_Len = Bytes.ShorttoBytes((short) CONT_data.length);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(UserMsg_Len);
            baos.write(CONT_data_Len);
            baos.write(CONT_Flags);
            baos.write(UserMsg);
            baos.write(CONT_data);
            byte[] body = Header.crypt(version.byteValue(), tacacsSequence.byteValue(), baos.toByteArray(), headerFlags, sessionID, secretkey);
            byte[] header = Header.makeHeader(body, version, Header.TYPE_AUTHENTIC, tacacsSequence, headerFlags, sessionID);
            baos.reset();
            baos.write(header);
            baos.write(body);
            baos.writeTo(theSocket.getOutputStream());
        }
    }

    private class AuthREPLY {

        private byte FLAG_NOECHO = 0x01;

        private byte REPLY_status;

        private byte REPLY_flags;

        private byte[] servermsgLen = new byte[2];

        private byte[] dataLen = new byte[2];

        private AuthREPLY() {
        }

        private void get() throws IOException, SocketException, NoSuchAlgorithmException {
            DataInputStream dis = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] body = null;
            byte[] header = null;
            dis = new DataInputStream(theSocket.getInputStream());
            for (int i = 0; i < 12; i++) {
                baos.write(dis.readByte());
            }
            header = baos.toByteArray();
            baos.reset();
            int Body_Len = Header.extractBodyLen(header);
            for (int i = 0; i < Body_Len; i++) {
                baos.write(dis.readByte());
            }
            byte[] tempBody = baos.toByteArray();
            byte headerVersionNumber = Header.extractVersionNumber(header);
            byte headerFlags = Header.extractFlags(header);
            byte headerSequenceNumber = Header.extractSeqNum(header);
            body = Header.crypt(headerVersionNumber, headerSequenceNumber, tempBody, headerFlags, sessionID, secretkey);
            REPLY_status = body[0];
            REPLY_flags = body[1];
        }

        private byte getStatus() {
            return REPLY_status;
        }

        private byte getFlags() {
            return REPLY_flags;
        }
    }
}
