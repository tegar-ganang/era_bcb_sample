package Networking.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class User {

    String userName = "Anonymous";

    Socket uSocket = null;

    OutputStream out = null;

    BufferedOutputStream bout = null;

    ObjectOutputStream objWriter = null;

    Channel channel = null;

    private boolean isValidated = false, isAdmin = false, canKick = false, canHack = false, canPM = false;

    /**
	 * @return the isAdmin
	 */
    public boolean isAdmin() {
        return isAdmin;
    }

    /**
	 * @param isAdmin the isAdmin to set
	 */
    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    /**
	 * @return the canKick
	 */
    public boolean canKick() {
        return canKick;
    }

    /**
	 * @param canKick the canKick to set
	 */
    public void setCanKick(boolean canKick) {
        this.canKick = canKick;
    }

    /**
	 * @return the canHack
	 */
    public boolean isCanHack() {
        return canHack;
    }

    /**
	 * @param canHack the canHack to set
	 */
    public void setCanHack(boolean canHack) {
        this.canHack = canHack;
    }

    /**
	 * @return the canPM
	 */
    public boolean canPM() {
        return canPM;
    }

    /**
	 * @param canPM the canPM to set
	 */
    public void setCanPM(boolean canPM) {
        this.canPM = canPM;
    }

    /**
	 * Creates a user with user name Anonymous
	 * @param uSocket the socket associated with this user
	 * @throws IOException
	 */
    public User(Socket uSocket) throws IOException {
        this(uSocket, "Anonymous");
    }

    /**
	 * @param uSocket the socket associated with this user
	 * @param uname the user name associated with this user
	 * @throws IOException
	 */
    public User(Socket uSocket, String uname) throws IOException {
        userName = uname;
        this.uSocket = uSocket;
        out = uSocket.getOutputStream();
        bout = new BufferedOutputStream(out);
        objWriter = new ObjectOutputStream(bout);
    }

    /**
	 * Authenticate this user against the servers registered user list.
	 * @param pw The users password
	 */
    public boolean authenticate(String pw) {
        return false;
    }

    /**
	 * @return the user name
	 */
    public String getUserName() {
        return userName;
    }

    /**
	 * @param userName the new user name
	 */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
	 * @return the socket associated with this user
	 */
    public Socket getUSocket() {
        return uSocket;
    }

    /**
	 * @param socket the new socket associated with this user
	 */
    public void setUSocket(Socket socket) {
        uSocket = socket;
    }

    /**
	 * @return the output stream
	 */
    public OutputStream getOut() {
        return out;
    }

    /**
	 * @param out the new output stream
	 */
    public void setOut(OutputStream out) {
        this.out = out;
    }

    /**
	 * @return the channel
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * @param channel the channel to set
	 */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
	 * @param isValidated the isValidated to set
	 */
    public void setValidated(boolean isValidated) {
        this.isValidated = isValidated;
    }

    /**
	 * @return the isValidated
	 */
    public boolean isValidated() {
        return isValidated;
    }

    public String toString() {
        return userName;
    }

    /**
	 * @return the objWriter
	 */
    public ObjectOutputStream getObjWriter() {
        return objWriter;
    }

    /**
	 * @param objWriter the objWriter to set
	 */
    public void setObjWriter(ObjectOutputStream objWriter) {
        this.objWriter = objWriter;
    }
}
