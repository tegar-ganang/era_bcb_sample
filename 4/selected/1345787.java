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
 * Class RhnSwChannels represents the list of software channels in a spacewalk
 * or satellite server.
 *
 * @author Alfredo Moralejo
 *
 */
public class RhnSwChannels {

    /** The id for the connection to the spacewalk server */
    private String connectionId_;

    /** The connection to xmlrpc client */
    private XmlRpcClient client_;

    /** The list of RhnSwChannel objects for all channels in the system */
    private List<RhnSwChannel> channels_ = new ArrayList<RhnSwChannel>();

    /** The name of the spacewalk system */
    private String spacewalk_;

    /** The number of channels in the spacewalk system */
    private Integer counter_;

    /**
	 *
	 *
	 * Constructor class. Establish the connection with a spacewalk server and
	 * get basic software channels information.
	 *
	 * @param spacewalk the network name of the spacewalk or satellite server
	 * @param user the user name to establish the connection.
	 * @param password the password for the given name
	 *
	 *
	 */
    public RhnSwChannels(String spacewalk, String user, String password) throws RhnConnFault {
        RhnConn connection = new RhnConn(spacewalk, user, password);
        this.connectionId_ = connection.getId();
        this.spacewalk_ = spacewalk;
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
	 * Constructor class. Using an opened connection with a spacewalk server to
	 * get basic software channels information.
	 *
	 * @param connection opened connection to spacewalk server.
	 *
	 */
    public RhnSwChannels(RhnConn connection) throws RhnConnFault {
        this.connectionId_ = connection.getId();
        this.spacewalk_ = connection.getServer();
        try {
            client_ = new XmlRpcClient(this.spacewalk_, false);
            this.loadInfo();
        } catch (MalformedURLException ex) {
            throw new RhnConnFault("Error connecting to spacewalk. Problem found in server URL: " + ex.getMessage());
        } catch (XmlRpcException ex) {
            throw new RhnConnFault("Error connecting to spacewalk server. Problem found in connection: " + ex.getMessage());
        }
    }

    /**
	 *
	 * Read info about channels from satellite server and populates information
	 * in the class.
	 *
	 * @throws RhnConnFault
	 *
	 */
    private void loadInfo() throws RhnConnFault {
        List args = new ArrayList();
        args.add(this.connectionId_);
        try {
            int counter = 0;
            List<XmlRpcStruct> channels = (List<XmlRpcStruct>) client_.invoke("channel.listAllChannels", args);
            for (XmlRpcStruct channel : channels) {
                args.clear();
                args.add(this.connectionId_);
                args.add(channel.get("id"));
                XmlRpcStruct cdetails = (XmlRpcStruct) client_.invoke("channel.software.getDetails", args);
                RhnSwChannel swchannel = new RhnSwChannel((Integer) channel.get("id"), channel.get("name").toString(), channel.get("label").toString(), cdetails.get("parent_channel_label").toString(), this.spacewalk_);
                channels_.add(swchannel);
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
	 * @return the number of software channels registered in the server.
	 *
	 */
    public Integer getCounter() {
        return this.counter_;
    }

    /**
	 *
	 * @return the list of software channels registered in the server.
	 *
	 */
    public List<RhnSwChannel> getChannels() {
        return this.channels_;
    }

    /**
	 *
	 * @return the list of names of software channels registered in the server.
	 *
	 */
    public List<String> getNames() {
        List<String> list = new ArrayList<String>();
        for (RhnSwChannel channel : this.channels_) {
            list.add(channel.getName().toString());
        }
        return list;
    }

    /**
	 *
	 * @return the list of labels of software channels registered in the server.
	 *
	 */
    public List<String> getLabels() {
        List<String> list = new ArrayList<String>();
        for (RhnSwChannel channel : this.channels_) {
            list.add(channel.getLabel().toString());
        }
        return list;
    }
}
