package spacewalklib;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;

/**
 *
 * Class RhnSwChannel represents a software channels in a spacewalk or satellite
 * server.
 *
 * @author Alfredo Moralejo
 *
 */
public class RhnSwChannel {

    /** The identificator of the channel */
    private Integer id_;

    /** The name of the channel */
    private String name_;

    /** The label of the channel */
    private String label_;

    /** The label of the parent channel for a child channel */
    private String parent_channel_label_;

    /** The spacewalk server where the channel exists */
    private String spacewalk_;

    /** The spacewalk server network name */
    private String connectionId_;

    /** The connection to the satellite server */
    private RhnConn connection_;

    /** The list of child channels for a base channel */
    private List<RhnSwChannel> child_channels_ = new ArrayList<RhnSwChannel>();

    /**
	 *
	 *
	 * Constructor class. Creates a RhnSwChannel object and populates main information.
	 *
	 * @param id the id of the software channel.
	 * @param name the name of the channel.
	 * @param label the label of the software channel.
	 * @param parent_channel the label of the parent software channel (for child channels).
	 * @param spacewalk the name of the spacewalk or satellite server
	 *
	 *
	 */
    public RhnSwChannel(Integer id, String name, String label, String parent_channel, String spacewalk) {
        this.id_ = id;
        this.name_ = name;
        this.label_ = label;
        this.parent_channel_label_ = parent_channel;
        this.spacewalk_ = spacewalk;
    }

    /**
	 *
	 *
	 * Constructor class. Creates a RhnSwChannel object and populates main information.
	 *
	 * @param spacewalk the network name of the spacewalk or satellite server
     * @param user the user name to establish the connection.
     * @param password the password for the given name
	 * @param id the id of the software channel.
	 *
	 * @throws RhnConnFault
	 * @throws RhnChannelNotFoundException
	 *
	 */
    public RhnSwChannel(String spacewalk, String user, String password, Integer id) throws RhnConnFault, RhnChannelNotFoundException {
        RhnConn connection = new RhnConn(spacewalk, user, password);
        this.connectionId_ = connection.getId();
        this.spacewalk_ = spacewalk;
        this.connection_ = connection;
        this.getInfo(id);
    }

    /**
	 *
	 *
	 * Constructor class. Creates a RhnSwChannel object and populates main information.
	 *
	 * @param spacewalk the network name of the spacewalk or satellite server
     * @param user the user name to establish the connection.
     * @param password the password for the given name
	 * @param label the label of the software channel.
	 *
	 * @throws RhnConnFault
	 * @throws RhnChannelNotFoundException
	 *
	 */
    public RhnSwChannel(String spacewalk, String user, String password, String label) throws RhnConnFault, RhnChannelNotFoundException {
        RhnConn connection = new RhnConn(spacewalk, user, password);
        this.connectionId_ = connection.getId();
        this.spacewalk_ = spacewalk;
        this.connection_ = connection;
        this.getInfo(label);
    }

    /**
	 *
	 *
	 * Constructor class. Creates a RhnSwChannel object and populates main information.
	 *
	 * @param connection the connection to the spacewalk or satellite server
	 * @param label the label of the software channel.
	 *
	 * @throws RhnConnFault
	 * @throws RhnChannelNotFoundException
	 *
	 */
    public RhnSwChannel(RhnConn connection, String label) throws RhnConnFault, RhnChannelNotFoundException {
        this.connectionId_ = connection.getId();
        this.spacewalk_ = connection.getServer();
        this.connection_ = connection;
        this.getInfo(label);
    }

    /**
	 *
	 * Get information about an specific software channel identified by id and
	 * populates object variables.
	 *
     * @param id, id for the channel to create.
     *
	 * @throws RhnConnFault
	 * @throws RhnChannelNotFoundException
	 *
	 */
    private void getInfo(Integer id) throws RhnConnFault, RhnChannelNotFoundException {
        try {
            XmlRpcClient client = new XmlRpcClient(this.spacewalk_, false);
            List args = new ArrayList();
            args.add(this.connectionId_);
            args.add(id);
            XmlRpcStruct channel = (XmlRpcStruct) client.invoke("channel.software.getDetails", args);
            if (channel.isEmpty()) {
                throw new RhnChannelNotFoundException("Software channel with id " + id + " not found");
            } else {
                this.id_ = (Integer) channel.get("id");
                this.label_ = channel.get("label").toString();
                this.name_ = channel.get("name").toString();
                this.parent_channel_label_ = channel.get("parent_channel_label").toString();
            }
        } catch (MalformedURLException ex) {
            throw new RhnConnFault("Error connecting to spacewalk. Problem found in server URL: " + ex.getMessage());
        } catch (XmlRpcFault ex) {
            if (ex.getMessage().contains("channel does not exist")) {
                throw new RhnChannelNotFoundException("Channel with id " + id + " not found: " + ex.getMessage());
            } else {
                throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
            }
        } catch (XmlRpcException ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        }
    }

    /**
	 *
	 * Get information about an specific software channel identified by label and
	 * populates object variables.
	 *
     * @param label, label for the channel to create.
     *
	 * @throws RhnConnFault
	 * @throws RhnChannelNotFoundException
	 *
	 */
    private void getInfo(String label) throws RhnConnFault, RhnChannelNotFoundException {
        try {
            XmlRpcClient client = new XmlRpcClient(this.spacewalk_, false);
            List args = new ArrayList();
            args.add(this.connectionId_);
            args.add(label);
            XmlRpcStruct channel = (XmlRpcStruct) client.invoke("channel.software.getDetails", args);
            if (channel.isEmpty()) {
                throw new RhnChannelNotFoundException("Software channel with label " + label + " not found");
            } else {
                this.id_ = (Integer) channel.get("id");
                this.label_ = channel.get("label").toString();
                this.name_ = channel.get("name").toString();
                this.parent_channel_label_ = channel.get("parent_channel_label").toString();
            }
        } catch (MalformedURLException ex) {
            throw new RhnConnFault("Error connecting to spacewalk. Problem found in server URL: " + ex.getMessage());
        } catch (XmlRpcFault ex) {
            if (ex.getMessage().contains("channel does not exist") || ex.getMessage().contains("No such channel")) {
                throw new RhnChannelNotFoundException("Channel " + label + " not found: " + ex.getMessage());
            } else {
                throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
            }
        } catch (XmlRpcException ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        }
    }

    /**
	 *
	 * Get information about child channels for a base channel and populates
	 * object child_channels_.
	 *
	 * @throws RhnConnFault
	 * @throws RhnChannelNotFoundException
	 *
	 */
    private void getChildChannels() throws RhnChannelNotFoundException, RhnConnFault {
        try {
            if (this.parent_channel_label_.isEmpty()) {
                this.child_channels_.clear();
                RhnSwChannels channels = new RhnSwChannels(this.connection_);
                for (RhnSwChannel channel : channels.getChannels()) {
                    if (channel.getParentchannel().equals(this.label_)) {
                        this.child_channels_.add(channel);
                    }
                }
            } else {
                throw new RhnChannelNotFoundException("Channel " + this.name_ + " is not a base channel");
            }
        } catch (XmlRpcException ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        }
    }

    /**
	 *
	 * @return the name of the software channel.
	 *
	 */
    public String getName() {
        return this.name_;
    }

    /**
	 *
	 * @return the label of the software channel.
	 *
	 */
    public String getLabel() {
        return this.label_;
    }

    /**
	 *
	 * @return the label of the parent software channel.
	 *
	 */
    public String getParentchannel() {
        return this.parent_channel_label_;
    }

    /**
	 *
	 * @return the id of the software channel.
	 *
	 */
    public Integer getId() {
        return this.id_;
    }

    /**
	 *
	 * @return the list of child channels for this channel.
	 *
	 * @throws RhnConnFault
	 * @throws RhnChannelNotFoundException
	 *
	 */
    public List<RhnSwChannel> getChildList() throws RhnChannelNotFoundException, RhnConnFault {
        if (this.child_channels_.size() == 0) {
            this.getChildChannels();
        }
        return this.child_channels_;
    }
}
