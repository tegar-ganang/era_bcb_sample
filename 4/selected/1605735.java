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
 * Class RhnConfigChannels represents the list of config channels in a spacewalk
 * or satellite server.
 *
 * @author Alfredo Moralejo
 *
 */
public class RhnConfigChannels {

    /** The connection to xmlrpc client */
    private XmlRpcClient client_;

    /** The list of config channels in the system represented in RhnConfigChannel objects */
    private List<RhnConfigChannel> channels_ = new ArrayList<RhnConfigChannel>();

    /** The number of channels in the spacewalk system */
    private Integer counter_;

    /** The connection to the satellite server */
    private RhnConn connection_;

    /**
	 *
	 *
	 * Constructor class. Establish the connection with a spacewalk server and
	 * get basic config channels information.
	 *
	 * @param spacewalk the network name of the spacewalk or satellite server
	 * @param user the user name to establish the connection.
	 * @param password the password for the given name
	 *
	 *
	 */
    public RhnConfigChannels(String spacewalk, String user, String password) throws RhnConnFault {
        RhnConn connection = new RhnConn(spacewalk, user, password);
        this.connection_ = connection;
        try {
            client_ = new XmlRpcClient(spacewalk, false);
            this.loadInfo();
        } catch (MalformedURLException ex) {
            throw new RhnConnFault("Error connecting to spacewalk. Problem found in server URL: " + ex.getMessage());
        } catch (XmlRpcException ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        }
    }

    /**
	 *
	 * Constructor class. Using an already opened connection with a spacewalk server
	 * gets basic config channels information.
	 *
	 * @param connection opened connection to spacewalk server.
	 *
	 */
    public RhnConfigChannels(RhnConn connection) throws RhnConnFault {
        this.connection_ = connection;
        try {
            client_ = new XmlRpcClient(this.connection_.getServer(), false);
            this.loadInfo();
        } catch (MalformedURLException ex) {
            throw new RhnConnFault("Error connecting to spacewalk. Problem found in server URL: " + ex.getMessage());
        } catch (XmlRpcException ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        }
    }

    /**
	 *
	 * Read info about config channels from satellite server and populates information
	 * in the class.
	 *
	 * @throws RhnConnFault
	 *
	 */
    private void loadInfo() throws RhnConnFault {
        List args = new ArrayList();
        args.add(this.connection_.getId());
        try {
            this.channels_.clear();
            int counter = 0;
            List<XmlRpcStruct> channels = (List<XmlRpcStruct>) client_.invoke("configchannel.listGlobals", args);
            for (XmlRpcStruct channel : channels) {
                RhnConfigChannel config_channel = new RhnConfigChannel((Integer) channel.get("id"), channel.get("name").toString(), channel.get("label").toString(), this.connection_);
                this.channels_.add(config_channel);
                counter++;
            }
            this.counter_ = counter;
        } catch (XmlRpcFault ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        } catch (XmlRpcException ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        }
    }

    /**
	 *
	 * @return the number of config channels registered in the server.
	 *
	 */
    public Integer getCounter() {
        return this.counter_;
    }

    /**
	 *
	 * @return the list of config channels registered in the server.
	 *
	 * @throws RhnChannelNotFoundException
	 *
	 */
    public List<RhnConfigChannel> getChannels() throws RhnChannelNotFoundException {
        if (this.channels_.size() == 0) {
            throw new RhnChannelNotFoundException("No config channels found in server " + this.connection_.getServer());
        } else {
            return this.channels_;
        }
    }

    /**
	 *
	 * @return the list of names of config channels registered in the server.
	 *
	 * @throws RhnChannelNotFoundException
	 *
	 */
    public List<String> getNames() throws RhnChannelNotFoundException {
        if (this.channels_.size() == 0) {
            throw new RhnChannelNotFoundException("No config channels found in server " + this.connection_.getServer());
        } else {
            List<String> list = new ArrayList<String>();
            for (RhnConfigChannel channel : this.channels_) {
                list.add(channel.getName().toString());
            }
            return list;
        }
    }

    /**
	 *
	 * @return the list of labels of config channels registered in the server.
	 *
	 * @throws RhnChannelNotFoundException
	 *
	 */
    public List<String> getLabels() throws RhnChannelNotFoundException {
        if (this.channels_.size() == 0) {
            throw new RhnChannelNotFoundException("No config channels found in server " + this.connection_.getServer());
        } else {
            List<String> list = new ArrayList<String>();
            for (RhnConfigChannel channel : this.channels_) {
                list.add(channel.getLabel().toString());
            }
            return list;
        }
    }
}
