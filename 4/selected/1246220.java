package edu.sdsc.rtdsm.drivers.turbine;

import com.rbnb.sapi.*;
import java.util.*;
import java.lang.*;
import edu.sdsc.rtdsm.drivers.turbine.util.TurbineServer;
import edu.sdsc.rtdsm.framework.util.*;

/**
 * Class TurbineClient
 * 
 */
public abstract class TurbineClient {

    private TurbineServer server;

    private Client cli;

    private String cliName;

    protected ChannelMap map = null;

    Hashtable channelHash = new Hashtable<String, Integer>();

    public TurbineClient(TurbineServer server, String cliName) {
        if (null == server) {
            throw new IllegalArgumentException("TurbineClient should be called with a " + "valid server argument");
        }
        this.server = server;
        this.cliName = cliName;
    }

    public void setClient(Client cli) {
        this.cli = cli;
    }

    /**
   * Get the value of server
   * 
   * @return the value of server
   */
    private TurbineServer getServer() {
        return server;
    }

    /**
   * 
   * @return void  
   */
    public boolean connect() {
        try {
            Debugger.debug(Debugger.RECORD, "serverAddr=" + server.serverAddr);
            Debugger.debug(Debugger.RECORD, "cliName=" + cliName);
            Debugger.debug(Debugger.RECORD, "userName=" + server.userName);
            Debugger.debug(Debugger.RECORD, "password=" + server.password);
            cli.OpenRBNBConnection(server.serverAddr, cliName, server.userName, server.password);
            return true;
        } catch (SAPIException se) {
            se.printStackTrace();
        }
        return false;
    }

    /**
   * 
   * @param channelNames 
   * @return java.util.Vector  
   */
    public Vector<Integer> addChannels(Vector<String> channelNames) {
        int index;
        Vector<Integer> indicies = new Vector<Integer>();
        if (map != null) {
            throw new IllegalStateException("Channels are being added after the " + "Channel map has been initialized. Duplicate additions?");
        }
        map = new ChannelMap();
        try {
            map.PutTimeAuto("timeofday");
            for (int c = 0; c < channelNames.size(); c++) {
                index = map.Add(channelNames.elementAt(c));
                indicies.add(new Integer(index));
                channelHash.put(channelNames.elementAt(c), new Integer(c));
                Debugger.debug(Debugger.RECORD, "Added Channel: " + channelNames.elementAt(c));
            }
            return indicies;
        } catch (SAPIException se) {
            se.printStackTrace();
        }
        return null;
    }

    public String getName() {
        return cli.GetClientName();
    }

    public int getChannelIndex(String chName) {
        Integer chInt = (Integer) channelHash.get(chName);
        if (chInt == null) {
            throw new IllegalArgumentException("Channel by name \"" + chName + "\" does not exist in Client\"" + cliName + "\".");
        }
        return chInt.intValue();
    }

    public void disconnect() {
        cli.CloseRBNBConnection();
    }
}
