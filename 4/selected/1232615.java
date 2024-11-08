package org.rdv.rbnb;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rdv.DataViewer;
import org.rdv.data.DataChannel;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;
import com.rbnb.sapi.ChannelTree.NodeTypeEnum;

/**
 * A class to fetch metadata from the server and post it to listeners. Methods
 * are also included to access metadata for individual channels.
 * 
 * @author  Jason P. Hanley
 */
public class MetadataManager {

    static Log log = org.rdv.LogFactory.getLog(MetadataManager.class.getName());

    private RBNBController rbnbController;

    /**
   * The name of the RBNB sink used to get metadata
   */
    private String rbnbSinkName = "RDVMetadata";

    /**
   * Listeners for metadata updates.
   */
    private ArrayList<MetadataListener> metadataListeners;

    /**
   * Listeners for markers.
   */
    private ArrayList<DataListener> markerListeners;

    /**
   * Map of channel objects created from metadata.
   */
    private Map<String, Channel> channels;

    /**
   * Metadata channel tree.
   */
    private ChannelTree ctree;

    /**
   * Whether the metadata update thread is running.
   */
    private boolean update;

    /**
   * Whether the update thread is sleeping.
   */
    private Boolean sleeping;

    /**
   * The thread updating the metadata periodically.
   */
    private Thread updateThread;

    /**
   * The time to sleep between each metadata update.
   */
    private static final long updateRate = 10000;

    /**
   * The timeout used when fetching data.
   */
    private static final long FETCH_TIMEOUT = 15000;

    /**
   * The maximum depth to make recursive requests with (to detect circular
   * references).
   */
    private static final int MAX_REQUEST_DEPTH = 5;

    /**
   * Create the class using the RBNBController for connection information.
   *  
   * @param rbnbController the RBNBController to use
   */
    public MetadataManager(RBNBController rbnbController) {
        this.rbnbController = rbnbController;
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            rbnbSinkName += "@" + hostname;
        } catch (UnknownHostException e) {
        }
        metadataListeners = new ArrayList<MetadataListener>();
        addMetadataListener(rbnbController);
        channels = new HashMap<String, Channel>();
        ctree = null;
        update = false;
        sleeping = false;
        updateThread = null;
        markerListeners = new ArrayList<DataListener>();
    }

    /**
   * Triggers a metadata update. This will return immediately and the metadata
   * will be posted to the listeners when available.
   */
    public void updateMetadataBackground() {
        if (update) {
            synchronized (sleeping) {
                if (sleeping) {
                    updateThread.interrupt();
                }
            }
        }
    }

    /**
   * Start the thread that periodically updates the metadata.
   */
    public void startUpdating() {
        if (update == true) {
            return;
        }
        final Sink metadataSink = new Sink();
        try {
            metadataSink.OpenRBNBConnection(rbnbController.getRBNBConnectionString(), rbnbSinkName);
        } catch (SAPIException e) {
            log.error("Failed to connect to RBNB server: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        updateMetadata(metadataSink);
        updateThread = new Thread("MetadataManager") {

            public void run() {
                log.info("RBNB Metadata thread is starting.");
                updateMetadataThread(metadataSink);
                metadataSink.CloseRBNBConnection();
                channels.clear();
                ctree = null;
                log.info("RBNB Metadata thread is stopping.");
            }
        };
        update = true;
        updateThread.start();
    }

    /**
   * Stops the metadata update thread.
   */
    public void stopUpdating() {
        update = false;
        if (updateThread != null && sleeping) {
            updateThread.interrupt();
        }
    }

    /**
   * Updates the metadata and sleeps <code>updateRate</code>. The
   * <code>Sink</code> must be opened and will not be closed when this method
   * exits.
   * 
   * @param metadataSink The sink connection to the RBNB server
   * @see #updateMetadata(Sink)
   */
    private void updateMetadataThread(Sink metadataSink) {
        while (update) {
            synchronized (sleeping) {
                sleeping = true;
                try {
                    Thread.sleep(updateRate);
                } catch (InterruptedException e) {
                }
                sleeping = false;
            }
            if (update) {
                updateMetadata(metadataSink);
            }
        }
        fireMetadataUpdated(null);
        fireMarkersUpdated(null);
    }

    /**
   * Updates the metadata and posts it to listeners. It also notifies all
   * threads waiting on this object.
   * 
   * @param metadataSink the RBNB sink to use for the server connection
   */
    private synchronized void updateMetadata(Sink metadataSink) {
        Map<String, Channel> newChannels = new HashMap<String, Channel>();
        try {
            ctree = getChannelTree(metadataSink, newChannels);
        } catch (SAPIException e) {
            log.error("Failed to update metadata: " + e.getMessage() + ".");
            if (!metadataSink.VerifyConnection()) {
                log.error("Metadata RBNB connection is severed, try to reconnect to " + rbnbController.getRBNBConnectionString() + ".");
                metadataSink.CloseRBNBConnection();
                try {
                    metadataSink.OpenRBNBConnection(rbnbController.getRBNBConnectionString(), "RDVMetadata");
                } catch (SAPIException error) {
                    log.error("Failed to connect to RBNB server: " + error.getMessage());
                    error.printStackTrace();
                }
            }
            return;
        }
        channels = newChannels;
        fireMetadataUpdated(ctree);
        notifyAll();
    }

    /**
   * Get the metadata channel tree for the whole server. This will populate the
   * channel map with channel objects derived from the metadata.
   * 
   * @param sink the sink connection to the RBNB server
   * @param channels the map to populate with channel objects
   * @return the metadata channel tree
   * @throws SAPIException if a server error occurs
   */
    private ChannelTree getChannelTree(Sink sink, Map<String, Channel> channels) throws SAPIException {
        return getChannelTree(sink, null, channels, 0);
    }

    /**
   * Get the metadata channel tree for the given <code>path</code>. This will
   * populate the channel map with channel objects derived from the metadata.
   * This will recursively make requests for child servers and plugins up to the
   * maximum request depth of {@value #MAX_REQUEST_DEPTH}.
   * 
   * @param sink sink the sink connection to the RBNB server
   * @param path the path for the desired metadata
   * @param channels the map to populate with channel objects
   * @param depth the depth of the request
   * @return the metadata channel tree for the given path
   * @throws SAPIException if a server error occurs
   * @see #MAX_REQUEST_DEPTH
   */
    private ChannelTree getChannelTree(Sink sink, String path, Map<String, Channel> channels, int depth) throws SAPIException {
        depth++;
        ChannelTree ctree = ChannelTree.EMPTY_TREE;
        ChannelMap markerChannelMap = new ChannelMap();
        ChannelMap cmap = new ChannelMap();
        if (path == null) {
            path = "";
            cmap.Add("...");
        } else {
            cmap.Add(path + "/...");
        }
        sink.RequestRegistration(cmap);
        cmap = sink.Fetch(FETCH_TIMEOUT, cmap);
        if (cmap.GetIfFetchTimedOut()) {
            log.error("Failed to get metadata.  Fetch timed out.");
            return ctree;
        }
        ctree = ChannelTree.createFromChannelMap(cmap);
        String[] channelList = cmap.GetChannelList();
        for (int i = 0; i < channelList.length; i++) {
            int channelIndex = cmap.GetIndex(channelList[i]);
            if (channelIndex != -1) {
                ChannelTree.Node node = ctree.findNode(channelList[i]);
                String userMetadata = cmap.GetUserInfo(channelIndex);
                Channel channel = new Channel(node, userMetadata);
                channels.put(channelList[i], channel);
                String mimeType = channel.getMetadata("mime");
                if (mimeType != null && mimeType.compareToIgnoreCase(EventMarker.MIME_TYPE) == 0) {
                    markerChannelMap.Add(node.getFullName());
                }
            }
        }
        Iterator it = ctree.iterator();
        while (it.hasNext()) {
            ChannelTree.Node node = (ChannelTree.Node) it.next();
            NodeTypeEnum type = node.getType();
            if ((type == ChannelTree.SERVER || type == ChannelTree.PLUGIN) && !path.startsWith(node.getFullName()) && depth < MAX_REQUEST_DEPTH) {
                ChannelTree childChannelTree = getChannelTree(sink, node.getFullName(), channels, depth);
                ctree = childChannelTree.merge(ctree);
            }
        }
        if (markerChannelMap.NumberOfChannels() > 0) {
            double markersDuration = System.currentTimeMillis() / 1000d;
            sink.Request(markerChannelMap, 0, markersDuration, "absolute");
            markerChannelMap = sink.Fetch(FETCH_TIMEOUT, markerChannelMap);
            if (!markerChannelMap.GetIfFetchTimedOut()) {
                fireMarkersUpdated(markerChannelMap);
            } else {
                log.error("Failed to get event markers. Fetched timed out.");
            }
        }
        return ctree;
    }

    /**
   * Return the latest metadata channel tree.
   * 
   * @return the metadata channel tree.
   */
    public ChannelTree getMetadataChannelTree() {
        if (ctree == null) {
            synchronized (this) {
                do {
                    try {
                        wait(250);
                    } catch (InterruptedException e) {
                    }
                } while (ctree == null);
            }
        }
        return ctree;
    }

    /**
   * Return a channel object for the given <code>channelName</code>.
   * 
   * @param channelName the desired channel
   * @return the channel object for the channel name, or null if the channel is
   *         not found
   */
    public Channel getChannel(String channelName) {
        if (ctree == null) {
            synchronized (this) {
                do {
                    try {
                        wait(250);
                    } catch (InterruptedException e) {
                    }
                } while (ctree == null);
            }
        }
        return channels.get(channelName);
    }

    /**
   * Returns a list of channel objects.
   * 
   * @param channelNames  the list of channels names to get
   * @return              a list of channels
   */
    public List<DataChannel> getChannels(List<String> channelNames) {
        List<DataChannel> channelsRequest = new ArrayList<DataChannel>();
        for (String channelName : channelNames) {
            Channel channel = getChannel(channelName);
            if (channel != null) {
                channelsRequest.add(channel);
            }
        }
        return channelsRequest;
    }

    /**
   * Add a listener for metadata updates.
   * 
   * @param listener the metadata listener
   */
    public void addMetadataListener(MetadataListener listener) {
        metadataListeners.add(listener);
    }

    /**
   * Add a listener for marker data.
   * 
   * @param listener  the marker listener to add
   */
    public void addMarkerListener(DataListener listener) {
        markerListeners.add(listener);
    }

    /**
   * Remove a listener for metadata updates.
   * 
   * @param listener the metadata listener
   */
    public void removeMetadataListener(MetadataListener listener) {
        metadataListeners.remove(listener);
    }

    /**
   * Remove a listener for marker data.
   * 
   * @param listener  the marker listener to remove
   */
    public void removeMarkerListener(DataListener listener) {
        markerListeners.remove(listener);
    }

    /**
   * Post metadata updates to the subscribed listeners.
   * 
   * @param channelTree the new metadata channel tree
   */
    private void fireMetadataUpdated(ChannelTree channelTree) {
        for (MetadataListener listener : metadataListeners) {
            listener.channelTreeUpdated(channelTree);
        }
    }

    /**
   * Post marker data to subscribed listeners.
   * 
   * @param cmap  the channel map containing the marker data
   */
    protected void fireMarkersUpdated(ChannelMap cmap) {
        for (DataListener listener : markerListeners) {
            listener.postData(cmap);
        }
    }
}
