package org.rdv.rbnb;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rdv.DataViewer;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.ChannelTree;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

/**
 * A class to manage a connection to an RBNB server and to post channel data to
 * interested listeners.
 * 
 * @author Jason P. Hanley
 */
public class RBNBController implements Player, MetadataListener {

    static Log log = org.rdv.LogFactory.getLog(RBNBController.class.getName());

    /** the single instance of this class */
    protected static RBNBController instance;

    private String rbnbSinkName = "RDV";

    private Thread rbnbThread;

    private int state;

    private Sink sink;

    private String rbnbHostName;

    private int rbnbPortNumber;

    private static final String DEFAULT_RBNB_HOST_NAME = "localhost";

    private static final int DEFAULT_RBNB_PORT_NUMBER = 3333;

    private boolean requestIsMonitor;

    private ChannelMap requestedChannels;

    private ChannelTree metaDataChannelTree;

    private ChannelManager channelManager;

    private MetadataManager metadataManager;

    private MarkerManager markerManager;

    private List<TimeListener> timeListeners;

    private List<StateListener> stateListeners;

    private List<SubscriptionListener> subscriptionListeners;

    private List<PlaybackRateListener> playbackRateListeners;

    private List<TimeScaleListener> timeScaleChangeListeners;

    private List<MessageListener> messageListeners;

    private List<ConnectionListener> connectionListeners;

    private ChannelMap preFetchChannelMap;

    private Object preFetchLock = new Object();

    private boolean preFetchDone;

    private double location;

    private double playbackRate;

    private double timeScale;

    private double updateLocation = -1;

    private Object updateLocationLock = new Object();

    private double updateTimeScale = -1;

    private Object updateTimeScaleLock = new Object();

    private double updatePlaybackRate = -1;

    private Object updatePlaybackRateLock = new Object();

    private List<Integer> stateChangeRequests = new ArrayList<Integer>();

    private List<SubscriptionRequest> updateSubscriptionRequests = new ArrayList<SubscriptionRequest>();

    private boolean dropData;

    private final double PLAYBACK_REFRESH_RATE = 0.05;

    private final long LOADING_TIMEOUT = 30000;

    protected RBNBController() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            rbnbSinkName += "@" + hostname;
        } catch (UnknownHostException e) {
        }
        state = STATE_DISCONNECTED;
        rbnbHostName = DEFAULT_RBNB_HOST_NAME;
        rbnbPortNumber = DEFAULT_RBNB_PORT_NUMBER;
        requestIsMonitor = false;
        dropData = true;
        location = System.currentTimeMillis() / 1000d;
        playbackRate = 1;
        timeScale = 1;
        requestedChannels = new ChannelMap();
        metaDataChannelTree = ChannelTree.EMPTY_TREE;
        channelManager = new ChannelManager();
        metadataManager = new MetadataManager(this);
        markerManager = new MarkerManager(this);
        timeListeners = new ArrayList<TimeListener>();
        stateListeners = new ArrayList<StateListener>();
        subscriptionListeners = new ArrayList<SubscriptionListener>();
        playbackRateListeners = new ArrayList<PlaybackRateListener>();
        timeScaleChangeListeners = new ArrayList<TimeScaleListener>();
        messageListeners = new ArrayList<MessageListener>();
        connectionListeners = new ArrayList<ConnectionListener>();
        run();
    }

    /**
   * Get the single instance of this class.
   * 
   * @return  the instance of this class
   */
    public static RBNBController getInstance() {
        if (instance == null) {
            instance = new RBNBController();
        }
        return instance;
    }

    private void run() {
        rbnbThread = new Thread(new Runnable() {

            public void run() {
                runRBNB();
            }
        }, "RBNB");
        rbnbThread.start();
    }

    private void runRBNB() {
        log.info("RBNB data thread has started.");
        while (state != STATE_EXITING) {
            processSubscriptionRequests();
            processLocationUpdate();
            processTimeScaleUpdate();
            processPlaybackRateUpdate();
            processStateChangeRequests();
            switch(state) {
                case STATE_LOADING:
                    log.warn("You must always manually transition from the loading state.");
                    changeStateSafe(STATE_STOPPED);
                    break;
                case STATE_PLAYING:
                    updateDataPlaying();
                    break;
                case STATE_MONITORING:
                    updateDataMonitoring();
                    break;
                case STATE_STOPPED:
                case STATE_DISCONNECTED:
                    try {
                        Thread.sleep(50);
                    } catch (Exception e) {
                    }
                    break;
            }
        }
        closeRBNB();
        metadataManager.stopUpdating();
        log.info("RBNB data thread is exiting.");
    }

    private void processSubscriptionRequests() {
        while (!updateSubscriptionRequests.isEmpty()) {
            SubscriptionRequest subscriptionRequest;
            synchronized (updateSubscriptionRequests) {
                subscriptionRequest = (SubscriptionRequest) updateSubscriptionRequests.remove(0);
            }
            String channelName = subscriptionRequest.getChannelName();
            DataListener listener = subscriptionRequest.getListener();
            if (subscriptionRequest.isSubscribe()) {
                subscribeSafe(channelName, listener);
            } else {
                unsubscribeSafe(channelName, listener);
            }
        }
    }

    private void processLocationUpdate() {
        if (updateLocation != -1) {
            double oldLocation = location;
            synchronized (updateLocationLock) {
                location = updateLocation;
                updateLocation = -1;
            }
            if (oldLocation == location) {
                return;
            }
            log.info("Setting location to " + DataViewer.formatDate(location) + ".");
            if (requestedChannels.NumberOfChannels() > 0) {
                changeStateSafe(STATE_LOADING);
                double duration;
                if (oldLocation < location && oldLocation > location - timeScale) {
                    duration = location - oldLocation;
                } else {
                    duration = timeScale;
                }
                loadData(location, duration);
                changeStateSafe(STATE_STOPPED);
            } else {
                updateTimeListeners(location);
            }
        }
    }

    private void processTimeScaleUpdate() {
        if (updateTimeScale != -1) {
            double oldTimeScale = timeScale;
            synchronized (updateTimeScaleLock) {
                timeScale = updateTimeScale;
                updateTimeScale = -1;
            }
            if (timeScale == oldTimeScale) {
                return;
            }
            log.info("Setting time scale to " + timeScale + ".");
            fireTimeScaleChanged(timeScale);
            if (timeScale > oldTimeScale && requestedChannels.NumberOfChannels() > 0) {
                int originalState = state;
                changeStateSafe(STATE_LOADING);
                loadData();
                if (originalState == STATE_PLAYING) {
                    changeStateSafe(STATE_PLAYING);
                } else if (originalState == STATE_MONITORING) {
                    changeStateSafe(STATE_MONITORING);
                } else {
                    changeStateSafe(STATE_STOPPED);
                }
            }
        }
    }

    private void processPlaybackRateUpdate() {
        if (updatePlaybackRate != -1) {
            double oldPlaybackRate = playbackRate;
            synchronized (updatePlaybackRateLock) {
                playbackRate = updatePlaybackRate;
                updatePlaybackRate = -1;
            }
            if (playbackRate == oldPlaybackRate) {
                return;
            }
            log.info("Setting playback rate to " + playbackRate + " seconds.");
            if (state == STATE_PLAYING) {
                getPreFetchChannelMap();
                preFetchData(location, playbackRate);
            }
            firePlaybackRateChanged(playbackRate);
        }
    }

    private void processStateChangeRequests() {
        while (!stateChangeRequests.isEmpty()) {
            int updateState;
            synchronized (stateChangeRequests) {
                updateState = ((Integer) stateChangeRequests.remove(0)).intValue();
            }
            changeStateSafe(updateState);
        }
    }

    private boolean changeStateSafe(int newState) {
        if (state == newState) {
            log.info("Already in state " + getStateName(state) + ".");
            return true;
        } else if (state == STATE_PLAYING) {
            getPreFetchChannelMap();
        } else if (state == STATE_EXITING) {
            log.error("Can not transition out of exiting state to " + getStateName(state) + " state.");
            return false;
        } else if (state == STATE_DISCONNECTED && newState != STATE_EXITING) {
            fireConnecting();
            try {
                initRBNB();
            } catch (SAPIException e) {
                closeRBNB();
                String message = e.getMessage();
                if (message.contains("java.io.InterruptedIOException")) {
                    log.info("RBNB server connection canceled by user.");
                } else {
                    log.error("Failed to connect to the RBNB server.");
                    fireErrorMessage("Failed to connect to the RBNB server.");
                }
                fireConnectionFailed();
                return false;
            }
            metadataManager.startUpdating();
            fireConnected();
        }
        switch(newState) {
            case STATE_MONITORING:
                if (!monitorData()) {
                    fireErrorMessage("Stopping real time. Failed to load data from the server. Please try again later.");
                    return false;
                }
                break;
            case STATE_PLAYING:
                preFetchData(location, playbackRate);
                break;
            case STATE_LOADING:
            case STATE_STOPPED:
            case STATE_EXITING:
                break;
            case STATE_DISCONNECTED:
                closeRBNB();
                metadataManager.stopUpdating();
                break;
            default:
                log.error("Unknown state: " + state + ".");
                return false;
        }
        int oldState = state;
        state = newState;
        notifyStateListeners(state, oldState);
        log.info("Transitioned from state " + getStateName(oldState) + " to " + getStateName(state) + ".");
        return true;
    }

    private void initRBNB() throws SAPIException {
        if (sink == null) {
            sink = new Sink();
        } else {
            return;
        }
        sink.OpenRBNBConnection(rbnbHostName + ":" + rbnbPortNumber, rbnbSinkName);
        log.info("Connected to RBNB server.");
    }

    private void closeRBNB() {
        if (sink == null) return;
        sink.CloseRBNBConnection();
        sink = null;
        log.info("Connection to RBNB server closed.");
    }

    private void reInitRBNB() throws SAPIException {
        closeRBNB();
        initRBNB();
    }

    private boolean subscribeSafe(String channelName, DataListener panel) {
        if (state == STATE_DISCONNECTED) {
            return false;
        }
        try {
            requestedChannels.Add(channelName);
        } catch (SAPIException e) {
            log.error("Failed to add channel " + channelName + ".");
            e.printStackTrace();
            return false;
        }
        channelManager.subscribe(channelName, panel);
        int originalState = state;
        changeStateSafe(STATE_LOADING);
        loadData(channelName);
        if (originalState == STATE_MONITORING) {
            changeStateSafe(STATE_MONITORING);
        } else if (originalState == STATE_PLAYING) {
            changeStateSafe(STATE_PLAYING);
        } else {
            changeStateSafe(STATE_STOPPED);
        }
        fireSubscriptionNotification(channelName);
        return true;
    }

    private boolean unsubscribeSafe(String channelName, DataListener panel) {
        if (state == STATE_DISCONNECTED) {
            return false;
        }
        channelManager.unsubscribe(channelName, panel);
        if (!channelManager.isChannelSubscribed(channelName)) {
            ChannelMap newRequestedChannels = new ChannelMap();
            String[] channelList = requestedChannels.GetChannelList();
            for (int i = 0; i < channelList.length; i++) {
                if (!channelName.equals(channelList[i])) {
                    try {
                        newRequestedChannels.Add(channelList[i]);
                    } catch (SAPIException e) {
                        log.error("Failed to remove to channel " + channelName + ".");
                        e.printStackTrace();
                        return false;
                    }
                }
            }
            requestedChannels = newRequestedChannels;
        }
        log.info("Unsubscribed from " + channelName + " for listener " + panel + ".");
        if (state == STATE_MONITORING) {
            monitorData();
        }
        fireUnsubscriptionNotification(channelName);
        return true;
    }

    /**
   * Load data for all channels.
   */
    private void loadData() {
        loadData(location, timeScale);
    }

    /**
   * Load data for all channels.
   * 
   * @param location  the end time
   * @param duration  the duration
   */
    private void loadData(double location, double duration) {
        String[] subscribedChannels = requestedChannels.GetChannelList();
        loadData(Arrays.asList(subscribedChannels), location, duration);
    }

    /**
   * Load data for the specified channel.
   * 
   * @param channelName  the name of the channel
   */
    private void loadData(String channelName) {
        loadData(Collections.singletonList(channelName), location, timeScale);
    }

    /**
   * Load data for the specified channels.
   * 
   * @param channelNames  a list of channel names
   * @param location      the end time
   * @param duration      the amount of data to load
   */
    private void loadData(List<String> channelNames, double location, double duration) {
        ChannelMap realRequestedChannels = requestedChannels;
        ChannelMap imageChannels = new ChannelMap();
        ChannelMap tabularChannels = new ChannelMap();
        ChannelMap otherChannels = new ChannelMap();
        for (String channelName : channelNames) {
            try {
                if (isVideo(metaDataChannelTree, channelName)) {
                    imageChannels.Add(channelName);
                } else if (channelManager.isChannelTabularOnly(channelName)) {
                    tabularChannels.Add(channelName);
                } else {
                    otherChannels.Add(channelName);
                }
            } catch (SAPIException e) {
                log.error("Failed to add channel " + channelName + ".");
                e.printStackTrace();
            }
        }
        if (imageChannels.NumberOfChannels() > 0) {
            requestedChannels = imageChannels;
            if (!requestData(location, 0)) {
                fireErrorMessage("Failed to load data from the server. Please try again later.");
                requestedChannels = realRequestedChannels;
                changeStateSafe(STATE_STOPPED);
                return;
            }
            updateDataMonitoring();
            updateTimeListeners(location);
        }
        if (tabularChannels.NumberOfChannels() > 0) {
            requestedChannels = tabularChannels;
            if (!requestData(location - 1, 1)) {
                fireErrorMessage("Failed to load data from the server. Please try again later.");
                requestedChannels = realRequestedChannels;
                changeStateSafe(STATE_STOPPED);
                return;
            }
            updateDataMonitoring();
            updateTimeListeners(location);
        }
        if (otherChannels.NumberOfChannels() > 0) {
            requestedChannels = otherChannels;
            if (!requestData(location - duration, duration)) {
                fireErrorMessage("Failed to load data from the server. Please try again later.");
                requestedChannels = realRequestedChannels;
                changeStateSafe(STATE_STOPPED);
                return;
            }
            updateDataMonitoring();
            updateTimeListeners(location);
        }
        requestedChannels = realRequestedChannels;
        log.info("Loaded " + DataViewer.formatSeconds(timeScale) + " of data at " + DataViewer.formatDate(location) + ".");
    }

    private boolean requestData(double location, double duration) {
        return requestData(location, duration, true);
    }

    private boolean requestData(double location, double duration, boolean retry) {
        if (requestedChannels.NumberOfChannels() == 0) {
            return false;
        }
        if (requestIsMonitor) {
            try {
                reInitRBNB();
            } catch (SAPIException e) {
                requestIsMonitor = true;
                return false;
            }
            requestIsMonitor = false;
        }
        try {
            sink.Request(requestedChannels, location, duration, "absolute");
        } catch (SAPIException e) {
            log.error("Failed to request channels at " + DataViewer.formatDate(location) + " for " + DataViewer.formatSeconds(duration) + ".");
            e.printStackTrace();
            requestIsMonitor = true;
            if (retry) {
                return requestData(location, duration, false);
            } else {
                return false;
            }
        }
        return true;
    }

    private synchronized void updateDataPlaying() {
        if (requestedChannels.NumberOfChannels() == 0) {
            fireStatusMessage("Stopping playback. No channels are selected.");
            changeStateSafe(STATE_STOPPED);
            return;
        }
        ChannelMap getmap = null;
        getmap = getPreFetchChannelMap();
        if (getmap == null) {
            fireErrorMessage("Stopping playback. Failed to load data from the server. Please try again later.");
            changeStateSafe(STATE_STOPPED);
            requestIsMonitor = true;
            return;
        } else if (getmap.GetIfFetchTimedOut()) {
            fireErrorMessage("Stopping playback. Failed to load enough data from server. The playback rate may be too fast or the server is busy.");
            changeStateSafe(STATE_STOPPED);
            return;
        }
        if (getmap.NumberOfChannels() == 0 && !moreData(requestedChannels.GetChannelList(), metaDataChannelTree, location)) {
            log.warn("Received no data. Assuming end of channel.");
            changeStateSafe(STATE_STOPPED);
            return;
        }
        preFetchData(location + playbackRate, playbackRate);
        channelManager.postData(getmap);
        double playbackDuration = playbackRate;
        double playbackRefreshRate = PLAYBACK_REFRESH_RATE;
        double playbackStepTime = playbackRate * playbackRefreshRate;
        long playbackSteps = (long) (playbackDuration / playbackStepTime);
        double locationStartTime = location;
        long playbackStartTime = System.nanoTime();
        long i = 0;
        while (i < playbackSteps && stateChangeRequests.size() == 0 && updateLocation == -1 && updateTimeScale == -1 && updatePlaybackRate == -1) {
            double timeDifference = (playbackRefreshRate * (i + 1)) - ((System.nanoTime() - playbackStartTime) / 1000000000d);
            if (dropData && timeDifference < -playbackRefreshRate) {
                int stepsToSkip = (int) ((timeDifference * -1) / playbackRefreshRate);
                i += stepsToSkip;
            } else if (timeDifference > playbackRefreshRate) {
                try {
                    Thread.sleep((long) (timeDifference * 1000));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            i++;
            location = locationStartTime + (playbackStepTime) * i;
            updateTimeListeners(location);
        }
    }

    private void preFetchData(final double location, final double duration) {
        preFetchChannelMap = null;
        preFetchDone = false;
        new Thread(new Runnable() {

            public void run() {
                boolean requestStatus = false;
                if (state == STATE_PLAYING) {
                    requestStatus = requestData(location, duration);
                }
                if (requestStatus) {
                    try {
                        preFetchChannelMap = sink.Fetch(LOADING_TIMEOUT);
                    } catch (Exception e) {
                        log.error("Failed to fetch data.");
                        e.printStackTrace();
                    }
                } else {
                    preFetchChannelMap = null;
                }
                synchronized (preFetchLock) {
                    preFetchDone = true;
                    preFetchLock.notify();
                }
            }
        }, "prefetch").start();
    }

    private ChannelMap getPreFetchChannelMap() {
        synchronized (preFetchLock) {
            if (!preFetchDone) {
                log.debug("Waiting for pre-fetch channel map.");
                try {
                    preFetchLock.wait();
                } catch (Exception e) {
                    log.error("Failed to wait for channel map.");
                    e.printStackTrace();
                }
                log.debug("Done waiting for pre-fetch channel map.");
            }
        }
        ChannelMap fetchedMap = preFetchChannelMap;
        preFetchChannelMap = null;
        return fetchedMap;
    }

    private boolean monitorData() {
        return monitorData(true);
    }

    private boolean monitorData(boolean retry) {
        if (requestedChannels.NumberOfChannels() == 0) {
            return true;
        }
        if (requestIsMonitor) {
            try {
                reInitRBNB();
            } catch (SAPIException e) {
                e.printStackTrace();
                return false;
            }
        }
        log.debug("Monitoring data after location " + DataViewer.formatDate(location) + ".");
        requestIsMonitor = true;
        try {
            sink.Monitor(requestedChannels, 5);
            log.info("Monitoring selected data channels.");
        } catch (SAPIException e) {
            log.error("Failed to monitor channels.");
            e.printStackTrace();
            if (retry) {
                return monitorData(false);
            } else {
                return false;
            }
        }
        return true;
    }

    private void updateDataMonitoring() {
        if (requestedChannels.NumberOfChannels() == 0) {
            fireStatusMessage("Stopping real time. No channels are selected.");
            changeStateSafe(STATE_STOPPED);
            return;
        }
        ChannelMap getmap = null;
        long timeout;
        if (state == STATE_MONITORING) {
            timeout = 500;
        } else {
            timeout = LOADING_TIMEOUT;
        }
        try {
            getmap = sink.Fetch(timeout);
        } catch (Exception e) {
            fireErrorMessage("Failed to load data from the server. Please try again later.");
            e.printStackTrace();
            changeStateSafe(STATE_STOPPED);
            requestIsMonitor = true;
            return;
        }
        if (getmap.GetIfFetchTimedOut()) {
            if (state == STATE_MONITORING) {
                log.debug("Fetch timed out for monitor.");
                return;
            } else {
                log.error("Failed to fetch data.");
                fireErrorMessage("Failed to load data from the server. Please try again later.");
                changeStateSafe(STATE_STOPPED);
                return;
            }
        }
        if (getmap.NumberOfChannels() == 0) {
            return;
        }
        channelManager.postData(getmap);
        if (state == STATE_MONITORING) {
            double newLocation = getLastTime(getmap);
            if (newLocation > location) {
                location = newLocation;
            }
            updateTimeListeners(location);
        }
    }

    private void updateTimeListeners(double location) {
        for (int i = 0; i < timeListeners.size(); i++) {
            TimeListener timeListener = (TimeListener) timeListeners.get(i);
            try {
                timeListener.postTime(location);
            } catch (Exception e) {
                log.error("Failed to post time to " + timeListener + ".");
                e.printStackTrace();
            }
        }
    }

    private void notifyStateListeners(int state, int oldState) {
        for (int i = 0; i < stateListeners.size(); i++) {
            StateListener stateListener = (StateListener) stateListeners.get(i);
            stateListener.postState(state, oldState);
        }
    }

    private void fireSubscriptionNotification(String channelName) {
        SubscriptionListener subscriptionListener;
        for (int i = 0; i < subscriptionListeners.size(); i++) {
            subscriptionListener = (SubscriptionListener) subscriptionListeners.get(i);
            subscriptionListener.channelSubscribed(channelName);
        }
    }

    private void fireUnsubscriptionNotification(String channelName) {
        SubscriptionListener subscriptionListener;
        for (int i = 0; i < subscriptionListeners.size(); i++) {
            subscriptionListener = (SubscriptionListener) subscriptionListeners.get(i);
            subscriptionListener.channelUnsubscribed(channelName);
        }
    }

    private void firePlaybackRateChanged(double playbackRate) {
        PlaybackRateListener listener;
        for (int i = 0; i < playbackRateListeners.size(); i++) {
            listener = (PlaybackRateListener) playbackRateListeners.get(i);
            listener.playbackRateChanged(playbackRate);
        }
    }

    private void fireTimeScaleChanged(double timeScale) {
        TimeScaleListener listener;
        for (int i = 0; i < timeScaleChangeListeners.size(); i++) {
            listener = (TimeScaleListener) timeScaleChangeListeners.get(i);
            listener.timeScaleChanged(timeScale);
        }
    }

    private static boolean moreData(String[] channels, ChannelTree ctree, double time) {
        double endTime = -1;
        Iterator it = ctree.iterator();
        while (it.hasNext()) {
            ChannelTree.Node node = (ChannelTree.Node) it.next();
            double channelEndTime = node.getStart() + node.getDuration();
            if (channelEndTime != -1) {
                endTime = Math.max(endTime, channelEndTime);
            }
        }
        if (endTime == -1 || time >= endTime) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean isVideo(ChannelTree channelTree, String channelName) {
        if (channelName == null) {
            log.error("Channel name is null for. Can't determine if is video.");
            return false;
        } else if (channelTree == null) {
            log.warn("Haven't received metadata yet, can't determine channel type.");
            return false;
        }
        ChannelTree.Node node = channelTree.findNode(channelName);
        if (node == null) {
            log.error("Unable to find channel in metadata.");
            return false;
        } else {
            String mime = node.getMime();
            if (mime != null && mime.equals("image/jpeg")) {
                return true;
            } else if (channelName.endsWith(".jpg")) {
                return true;
            } else {
                return false;
            }
        }
    }

    private static double getLastTime(ChannelMap channelMap) {
        double lastTime = -1;
        String[] channels = channelMap.GetChannelList();
        for (int i = 0; i < channels.length; i++) {
            String channelName = channels[i];
            int channelIndex = channelMap.GetIndex(channelName);
            double[] times = channelMap.GetTimes(channelIndex);
            double endTime = times[times.length - 1];
            if (endTime > lastTime) {
                lastTime = endTime;
            }
        }
        return lastTime;
    }

    public int getState() {
        return state;
    }

    public void monitor() {
        if (state != STATE_MONITORING) {
            setLocation(System.currentTimeMillis() / 1000d);
        }
        setState(STATE_MONITORING);
    }

    public void play() {
        setState(STATE_PLAYING);
    }

    public void pause() {
        setState(STATE_STOPPED);
    }

    public void exit() {
        setState(STATE_EXITING);
        int count = 0;
        while (sink != null && count++ < 20) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
            }
        }
    }

    public void setState(int state) {
        synchronized (stateChangeRequests) {
            stateChangeRequests.add(new Integer(state));
        }
    }

    public double getLocation() {
        return location;
    }

    public void setLocation(final double location) {
        if (location < 0) {
            log.error("Location not set; location must be nonnegative.");
            return;
        }
        synchronized (updateLocationLock) {
            updateLocation = location;
        }
    }

    public double getPlaybackRate() {
        return playbackRate;
    }

    public void setPlaybackRate(final double playbackRate) {
        if (playbackRate <= 0) {
            log.error("Playback rate not set; playback rate must be positive.");
            return;
        }
        synchronized (updatePlaybackRateLock) {
            updatePlaybackRate = playbackRate;
        }
    }

    public double getTimeScale() {
        return timeScale;
    }

    public void setTimeScale(double timeScale) {
        if (timeScale <= 0) {
            log.error("Time scale not set; time scale must be positive.");
            return;
        }
        synchronized (updateTimeScaleLock) {
            updateTimeScale = timeScale;
        }
    }

    public boolean subscribe(String channelName, DataListener listener) {
        synchronized (updateSubscriptionRequests) {
            updateSubscriptionRequests.add(new SubscriptionRequest(channelName, listener, true));
        }
        return true;
    }

    public boolean unsubscribe(String channelName, DataListener listener) {
        synchronized (updateSubscriptionRequests) {
            updateSubscriptionRequests.add(new SubscriptionRequest(channelName, listener, false));
        }
        return true;
    }

    public boolean isSubscribed(String channelName) {
        return channelManager.isChannelSubscribed(channelName);
    }

    /**
   * Returns true if there is at least one listener subscribed to a channel.
   * 
   * @return  true if there are channel listener, false if there are none
   */
    public boolean hasSubscribedChannels() {
        return channelManager.hasSubscribedChannels();
    }

    public void addStateListener(StateListener stateListener) {
        stateListener.postState(state, state);
        stateListeners.add(stateListener);
    }

    public void removeStateListener(StateListener stateListener) {
        stateListeners.remove(stateListener);
    }

    public void addTimeListener(TimeListener timeListener) {
        timeListeners.add(timeListener);
        timeListener.postTime(location);
    }

    public void removeTimeListener(TimeListener timeListener) {
        timeListeners.remove(timeListener);
    }

    public void addPlaybackRateListener(PlaybackRateListener listener) {
        listener.playbackRateChanged(playbackRate);
        playbackRateListeners.add(listener);
    }

    public void removePlaybackRateListener(PlaybackRateListener listener) {
        playbackRateListeners.remove(listener);
    }

    public void addTimeScaleListener(TimeScaleListener listener) {
        listener.timeScaleChanged(timeScale);
        timeScaleChangeListeners.add(listener);
    }

    public void removeTimeScaleListener(TimeScaleListener listener) {
        timeScaleChangeListeners.remove(listener);
    }

    public void dropData(boolean dropData) {
        this.dropData = dropData;
    }

    public String getRBNBHostName() {
        return rbnbHostName;
    }

    public void setRBNBHostName(String rbnbHostName) {
        this.rbnbHostName = rbnbHostName;
    }

    public int getRBNBPortNumber() {
        return rbnbPortNumber;
    }

    public void setRBNBPortNumber(int rbnbPortNumber) {
        this.rbnbPortNumber = rbnbPortNumber;
    }

    public String getRBNBConnectionString() {
        return rbnbHostName + ":" + rbnbPortNumber;
    }

    /**
   * Gets the name of the server. If there is no active connection, null is
   * returned.
   * 
   * @return  the name of the server, or null if there is no connection
   */
    public String getServerName() {
        if (sink == null) {
            return null;
        }
        String serverName;
        try {
            serverName = sink.GetServerName();
            if (serverName.startsWith("/") && serverName.length() >= 2) {
                serverName = serverName.substring(1);
            }
        } catch (IllegalStateException e) {
            serverName = null;
        }
        return serverName;
    }

    public boolean isConnected() {
        return sink != null;
    }

    public void connect() {
        connect(false);
    }

    public boolean connect(boolean block) {
        if (isConnected()) {
            return true;
        }
        if (block) {
            final Thread object = Thread.currentThread();
            ConnectionListener listener = new ConnectionListener() {

                public void connecting() {
                }

                public void connected() {
                    synchronized (object) {
                        object.notify();
                    }
                }

                public void connectionFailed() {
                    object.interrupt();
                }
            };
            addConnectionListener(listener);
            synchronized (object) {
                setState(STATE_STOPPED);
                try {
                    object.wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
            removeConnectionListener(listener);
        } else {
            setState(STATE_STOPPED);
        }
        return true;
    }

    /**
   * Cancel a connection attempt.
   */
    public void cancelConnect() {
        if (rbnbThread != null) {
            rbnbThread.interrupt();
        }
    }

    /**
   * Disconnect from the RBNB server. This method will return immediately.
   */
    public void disconnect() {
        disconnect(false);
    }

    /**
   * Disconnect from the RBNB server. If block is set, this method will not
   * return until the server has disconnected.
   * 
   * @param block  if true, wait for the server to disconnect
   * @return       true if the server disconnected
   */
    public boolean disconnect(boolean block) {
        if (!isConnected()) {
            return true;
        }
        if (block) {
            final Thread object = Thread.currentThread();
            StateListener listener = new StateListener() {

                public void postState(int newState, int oldState) {
                    if (newState == STATE_DISCONNECTED) {
                        synchronized (object) {
                            object.notify();
                        }
                    }
                }
            };
            addStateListener(listener);
            synchronized (object) {
                setState(STATE_DISCONNECTED);
                try {
                    object.wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
            removeStateListener(listener);
        } else {
            setState(STATE_DISCONNECTED);
        }
        return true;
    }

    public void reconnect() {
        setState(STATE_DISCONNECTED);
        setState(STATE_STOPPED);
    }

    public void addSubscriptionListener(SubscriptionListener subscriptionListener) {
        subscriptionListeners.add(subscriptionListener);
    }

    public void removeSubscriptionListener(SubscriptionListener subscriptionListener) {
        subscriptionListeners.remove(subscriptionListener);
    }

    private void fireErrorMessage(String errorMessage) {
        for (int i = 0; i < messageListeners.size(); i++) {
            MessageListener messageListener = (MessageListener) messageListeners.get(i);
            messageListener.postError(errorMessage);
        }
    }

    private void fireStatusMessage(String statusMessage) {
        for (int i = 0; i < messageListeners.size(); i++) {
            MessageListener messageListener = (MessageListener) messageListeners.get(i);
            messageListener.postStatus(statusMessage);
        }
    }

    public void addMessageListener(MessageListener messageListener) {
        messageListeners.add(messageListener);
    }

    public void removeMessageListener(MessageListener messageListener) {
        messageListeners.remove(messageListener);
    }

    private void fireConnecting() {
        for (int i = 0; i < connectionListeners.size(); i++) {
            ConnectionListener connectionListener = (ConnectionListener) connectionListeners.get(i);
            connectionListener.connecting();
        }
    }

    private void fireConnected() {
        for (int i = 0; i < connectionListeners.size(); i++) {
            ConnectionListener connectionListener = (ConnectionListener) connectionListeners.get(i);
            connectionListener.connected();
        }
    }

    private void fireConnectionFailed() {
        for (int i = 0; i < connectionListeners.size(); i++) {
            ConnectionListener connectionListener = (ConnectionListener) connectionListeners.get(i);
            connectionListener.connectionFailed();
        }
    }

    public void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    public void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }

    public MetadataManager getMetadataManager() {
        return metadataManager;
    }

    public Channel getChannel(String channelName) {
        return metadataManager.getChannel(channelName);
    }

    public void updateMetadata() {
        metadataManager.updateMetadataBackground();
    }

    public void channelTreeUpdated(ChannelTree ctree) {
        metaDataChannelTree = ctree;
    }

    public MarkerManager getMarkerManager() {
        return markerManager;
    }

    public static String getStateName(int state) {
        String stateString;
        switch(state) {
            case STATE_LOADING:
                stateString = "loading";
                break;
            case STATE_PLAYING:
                stateString = "playing";
                break;
            case STATE_MONITORING:
                stateString = "real time";
                break;
            case STATE_STOPPED:
                stateString = "stopped";
                break;
            case STATE_EXITING:
                stateString = "exiting";
                break;
            case STATE_DISCONNECTED:
                stateString = "disconnected";
                break;
            default:
                stateString = "UNKNOWN";
        }
        return stateString;
    }

    /**
   * Returns the state code for a given state name
   * 
   * @param stateName  the state name
   * @return           the state code
   */
    public static int getState(String stateName) {
        int code;
        if (stateName.equals("loading")) {
            code = STATE_LOADING;
        } else if (stateName.equals("playing")) {
            code = STATE_PLAYING;
        } else if (stateName.equals("real time")) {
            code = STATE_MONITORING;
        } else if (stateName.equals("stopped")) {
            code = STATE_STOPPED;
        } else if (stateName.equals("exiting")) {
            code = STATE_EXITING;
        } else if (stateName.equals("disconnected")) {
            code = STATE_DISCONNECTED;
        } else {
            code = -1;
        }
        return code;
    }

    class SubscriptionRequest {

        private String channelName;

        private DataListener listener;

        private boolean isSubscribe;

        public SubscriptionRequest(String channelName, DataListener listener, boolean isSubscribe) {
            this.channelName = channelName;
            this.listener = listener;
            this.isSubscribe = isSubscribe;
        }

        public String getChannelName() {
            return channelName;
        }

        public DataListener getListener() {
            return listener;
        }

        public boolean isSubscribe() {
            return isSubscribe;
        }
    }
}
