package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.User;

/**
 * This class contains information about a channel join/part event.  It includes
 * information about the channel, user and whether it is a join.
 * @author Mike
 */
public class ChannelJoinPart extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelJoinPart.class.getName());

    private User user = null;

    private String channel = null;

    private boolean joined = false;

    /**
     * Public constructor
     */
    public ChannelJoinPart() {
    }

    /**
     * Parses the data input stream
     * 
     * @param dis the data input stream
     */
    @Override
    public void parse(DataInputStream dis) {
        super.parse(dis);
        try {
            user = new User(dis);
            channel = dis.readUTF();
            joined = dis.readBoolean();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    /**
     * Writes the data to the data output stream
     * 
     * @param dos The data output stream
     */
    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            User.writeDos(user, dos);
            dos.writeUTF(channel);
            dos.writeBoolean(joined);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        return dos.size();
    }

    /**
     * @return returns the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user
     * @param user 
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * @return The channel that the user is joining/parting
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Sets the channel
     * 
     * @param channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return true if user is joining channel, false if leaving
     */
    public boolean hasJoined() {
        return joined;
    }

    /**
     * Sets if the user is joining the channel
     * 
     * @param joined true if the user is joining, false if he is parting
     */
    public void setJoined(boolean joined) {
        this.joined = joined;
    }

    /**
     * Overridden toString method
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" ChannelJoinPart - User: ");
        sb.append(user);
        sb.append((joined ? " has joined " : " has parted "));
        sb.append(channel);
        return sb.toString();
    }

    /**
     * the object name
     */
    @Override
    public String getObjectName() {
        return "ChannelJoinPart";
    }
}
