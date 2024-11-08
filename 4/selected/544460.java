package gov.sns.ca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ConnectionChecker implements ConnectionListener {

    /** List of the PVs that had good connections */
    protected final List<String> goodPVs;

    /** List of the PVs that had bad connections */
    protected final List<String> badPVs;

    /** Map to stash connection results in. Results are stored as Boolean. */
    protected final Map<String, Boolean> checkMap = Collections.synchronizedMap(new HashMap<String, Boolean>());

    /** all the pvs */
    protected final List<String> thePVs;

    /** Contructor */
    public ConnectionChecker(Collection<String> pvs) {
        goodPVs = new ArrayList<String>();
        badPVs = new ArrayList<String>();
        thePVs = new ArrayList<String>(pvs);
    }

    /** get the HaspMap holding the results of the connection check */
    public Map<String, Boolean> getCheckMap() {
        return Collections.unmodifiableMap(checkMap);
    }

    /** return the list of PVs that had a connection */
    public List<String> getGoodPVs() {
        return Collections.unmodifiableList(goodPVs);
    }

    /** return the list of PVs that had a connection */
    public List<String> getBadPVs() {
        return Collections.unmodifiableList(badPVs);
    }

    /** do the connection checks */
    public void checkThem() {
        ChannelFactory.defaultFactory().init();
        goodPVs.clear();
        badPVs.clear();
        checkMap.clear();
        for (String name : thePVs) {
            Channel chan = ChannelFactory.defaultFactory.getChannel(name);
            if (chan.isConnected()) {
                checkMap.put(name, true);
            } else {
                chan.requestConnection();
                checkMap.put(name, false);
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        for (Entry<String, Boolean> me : checkMap.entrySet()) {
            if (me.getValue()) goodPVs.add(me.getKey()); else badPVs.add(me.getKey());
        }
    }

    public void connectionMade(Channel chan) {
        checkMap.put(chan.getId(), true);
    }

    public void connectionDropped(Channel chan) {
        checkMap.put(chan.getId(), false);
    }
}
