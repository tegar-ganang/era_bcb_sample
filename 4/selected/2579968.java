package portochat.common.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds data for the server's channel list.
 * 
 * @author Mike
 */
public class ChannelList extends DefaultData {

    private static final Logger logger = Logger.getLogger(ChannelList.class.getName());

    private List<String> channelList = null;

    /**
     * Public constructor
     */
    public ChannelList() {
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
            int numChannels = dis.readInt();
            channelList = new ArrayList<String>();
            for (int i = 0; i < numChannels; i++) {
                String channel = dis.readUTF();
                channelList.add(channel);
            }
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
            if (channelList == null || channelList.isEmpty()) {
                dos.writeInt(0);
            } else {
                dos.writeInt(channelList.size());
                for (String channel : channelList) {
                    dos.writeUTF(channel);
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Unable to write data", ex);
        }
        return dos.size();
    }

    /**
     * @return a List<String> containing all the channels in the list
     */
    public List<String> getChannelList() {
        return channelList;
    }

    /**
     * Sets the channel list
     * 
     * @param channelList
     */
    public void setChannelList(List<String> channelList) {
        this.channelList = channelList;
    }

    /**
     * Overridden toString method
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date(time));
        sb.append(" ChannelList - Num Channels: ");
        if (channelList != null) {
            sb.append(channelList.size());
            for (String channel : channelList) {
                sb.append(" ");
                sb.append(channel);
            }
        } else {
            sb.append("0");
        }
        return sb.toString();
    }

    /**
     * the object name
     */
    @Override
    public String getObjectName() {
        return "ChannelList";
    }
}
