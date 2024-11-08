package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import portochat.common.User;

/**
 * This class holds data for user lists in a channel, or server wide.
 * @author Mike
 */
public class UserList extends DefaultData {

    private static final Logger logger = Logger.getLogger(UserList.class.getName());

    private List<User> userList = null;

    private String channel = null;

    public UserList() {
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
            boolean isChannel = dis.readBoolean();
            if (isChannel) {
                channel = dis.readUTF();
            }
            int numUsers = dis.readInt();
            if (numUsers > 0) {
                userList = new ArrayList<User>();
            }
            for (int i = 0; i < numUsers; i++) {
                userList.add(new User(dis));
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to parse data!", ex);
        }
    }

    @Override
    public int writeBody(DataOutputStream dos) {
        try {
            if (userList == null) {
                if (channel != null) {
                    dos.writeBoolean(true);
                    dos.writeUTF(channel);
                } else {
                    dos.writeBoolean(false);
                }
            } else if (channel != null) {
                dos.writeBoolean(true);
                dos.writeUTF(channel);
            } else {
                dos.writeBoolean(false);
            }
            if (userList != null) {
                dos.writeInt(userList.size());
                for (User user : userList) {
                    User.writeDos(user, dos);
                }
            } else {
                dos.writeInt(0);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        return dos.size();
    }

    /**
     * @return A List<User> of the users
     */
    public List<User> getUserList() {
        return userList;
    }

    public void setUserList(List<User> userList) {
        this.userList = userList;
    }

    /**
     * @return Returns the channel
     */
    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * Overridden toString method
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" UserList -");
        if (channel != null) {
            sb.append(" ");
            sb.append(channel);
        }
        sb.append(" Num Users: ");
        if (userList != null) {
            sb.append(userList.size());
            for (User user : userList) {
                sb.append(" ");
                sb.append(user);
            }
        } else {
            sb.append("0");
        }
        return sb.toString();
    }

    @Override
    public String getObjectName() {
        return "UserList";
    }
}
