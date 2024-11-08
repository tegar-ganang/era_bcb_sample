package com.salas.bb.core;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.util.*;
import java.util.logging.Logger;
import com.salas.bb.ai.*;
import com.salas.bb.ai.FeedStats;
import com.salas.bb.channelguide.*;

/**
 * GlobalModel - Root of the model of the whole application. 
 * 
 * The key state that we keep are the various channel guides, which may be displayed in the
 * main window. This is a singleton object.
 * 
 * N.B. This class is persisted as XML using XMLEncoder. XMLEncoder's default behavior is 
 * that it will write out all bean properties. Therefore the part of this object's state
 * which is available as a Bean Propertyu is exactly what will be written out. This is subtle
 * so be careful if you play with getXXX and setXXX methods which is how by default you can
 * tell what's a bean property and what is not.
 * 
 */
public class GlobalModel {

    private static final int PERSISTER_DELAY_MS = 5 * 1000;

    private static final int PERSISTER_REPEAT_MS = 60 * 1000;

    private static final int WEBSTAT_DELAY_MS = 1 * 60 * 1000;

    private static final int WEBSTAT_REPEAT_MS = 5 * 60 * 1000;

    private static final int DBG_PERSISTER_DELAY_MS = 100;

    private static final int DBG_PERSISTER_REPEAT_MS = 100;

    private static final int DBG_WEBSTAT_DELAY_MS = 100;

    private static final int DBG_WEBSTAT_REPEAT_MS = 100;

    public static GlobalModel SINGLETON;

    private Logger log = Logger.getLogger(this.getClass().getName());

    private boolean debugMode;

    private ChannelGuideSet channelGuideSet;

    private Timer daemon;

    private Map feedStatCollection;

    private Connection stateDb;

    /**
   * Construct the global state. After construction, the GlobalModel has an empty
   * ChannelGuideSet. Note that this constructor is called either directly (new, when 
   * the state is not coming from the persistent state) or via the XMLDecoder
   * call in GlobalController.UnPersistAsXML which instantiates a GlobalModel from
   * its persistent state. In either case the GlobalModel.SINGLETON static contains
   * a reference to the GlobalModel object.
   */
    public GlobalModel() {
        log.config("Constructing GlobalModel");
        feedStatCollection = new HashMap();
        setDebugMode(false);
    }

    /**
   * generateFakeState - Take a newly constructed GlobalModel and fill it with the 
   * initial built-in state. Normally this state is read in from the persistent state, so
   * this function is only called when the persistent state could not be found or restored.
   *  - 
   */
    public void generateFakeState() {
        ChannelGuideSet.setSINGLETON(new ChannelGuideSet());
        ChannelGuideSet.SINGLETON.generateFakeState();
        checkConsistency();
    }

    /**
   * initTransientState - After reading this in from persistent state, 
   * initialize parts of the state of GlobalModel which do not get
   * persisted.
   * 
   *  - 
   */
    public void initTransientState() {
        ChannelGuideSet.setSINGLETON(channelGuideSet);
        ChannelGuideSet.SINGLETON.initTransientState();
    }

    /**
   * setSelectedGuide - 
   * 
   * @param selected -  
   */
    public void setSelectedGuide(String selcode) {
        log.fine("Guide Selected");
        channelGuideSet.setSelectedGuide(selcode);
    }

    /**
   * getSelectedGuide - 
   * 
   * @return - 
   */
    public ChannelGuide getSelectedGuide() {
        return channelGuideSet.selectedGuide();
    }

    public Article getSelectedArticle() {
        ChannelGuideEntry aCGE = getSelectedCGE();
        return aCGE.getArticleAt(aCGE.selectedArticle());
    }

    /**
   * setSelectedChannel - 
   * 
   * @param theEntry - 
   */
    public void setSelectedChannel(ChannelGuideEntry theEntry) {
        channelGuideSet.setSelectedChannel(theEntry);
    }

    /**
   * setSelectedChannel - 
   * 
   * @param selected - 
   */
    public void setSelectedChannel(int selected) {
        ChannelGuideEntry aCGE = channelGuideSet.selectedGuide().getEntryAt(selected);
        setSelectedChannel(aCGE);
    }

    public void setSelectedArticle(int selected) {
        getSelectedCGE().setSelectedArticle(selected);
    }

    /**
   * getSelectedCGE - 
   * 
   * @return - Selected CGE. Null if there is no selected CGE.
   * @note - the only way the selected CGE is null is if there 
   * are no channels in the Selected ChannelGuide.
   */
    public ChannelGuideEntry getSelectedCGE() {
        return channelGuideSet.selectedGuide().selectedCGE();
    }

    public ChannelGuideSet getChannelGuideSet() {
        return channelGuideSet;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean b) {
        debugMode = b;
    }

    /**
   * startDaemons - Simple call to start running any timer tasks in the
   * background. Currently:
   
   * * ModelPersister - periodically stores the current state of the model
   * * into an XML file 
   * 
   * * WebStatServiceTask - Periodically collect information about Feeds from various
   * * Web based services using XML-RPC etc.
   * * Informa update Daemons, which are managed by the ChannelGuideSet
   */
    public void startDaemons() {
        int pers_delay, pers_rept, webs_delay, webs_rept;
        if (isDebugMode()) {
            pers_delay = DBG_PERSISTER_DELAY_MS;
            pers_rept = DBG_PERSISTER_REPEAT_MS;
            webs_delay = DBG_WEBSTAT_DELAY_MS;
            webs_rept = DBG_WEBSTAT_REPEAT_MS;
            log.severe("Starting daemons in Debug Mode (fast polling)");
        } else {
            pers_delay = PERSISTER_DELAY_MS;
            pers_rept = PERSISTER_REPEAT_MS;
            webs_delay = WEBSTAT_DELAY_MS;
            webs_rept = WEBSTAT_REPEAT_MS;
            log.config("Starting daemons in Regular Mode (regular polling)");
        }
        daemon = new Timer(true);
        daemon.schedule(new ModelPersister(this), pers_delay, pers_rept);
        daemon.schedule(new WebStatServiceTask(this), webs_delay, webs_rept);
        ChannelGuideIterator iterator = new ChannelGuideIterator();
        while (iterator.hasNext()) {
            ((ChannelGuide) iterator.next()).activateDaemons();
        }
    }

    /**
   * setChannelGuideSet - 
   * 
   * @param set - 
   */
    public void setChannelGuideSet(ChannelGuideSet set) {
        channelGuideSet = set;
    }

    /**
   * setSINGLETON - 
   * 
   * @param model - 
   */
    public static void setSINGLETON(GlobalModel model) {
        SINGLETON = model;
    }

    /**
   * genFeedKey - For a given HtmlURL string, generate a unique FeedKey
   * 
   * @param urlAsString
   * @return - Feedkey for that URL.
   */
    public FeedKey genFeedKey(String htmlUrlAsString) {
        log.fine("generating feedKey for: " + htmlUrlAsString);
        return new FeedKey(htmlUrlAsString);
    }

    /**
   * locateFeedStats - For a given FeedKey, return the FeedStats object. If none
   * exists then create a default one.
   * 
   * @param feed
   * @return - FeedStats object created or found, or null if the FeedKey is null
   */
    public FeedStats locateFeedStats(PropertyChangeListener listn, FeedKey feed) {
        if (feed == null) return null;
        FeedStats result;
        synchronized (feedStatCollection) {
            log.finer("Looking for feedStats for: " + feed + " ...");
            result = (FeedStats) feedStatCollection.get(feed);
            if (result == null) {
                result = new FeedStats();
                result.connectListener(listn);
                feedStatCollection.put(feed, result);
                log.finer("... added " + result + " to feedStatCollection");
            }
        }
        return result;
    }

    /**
   * updateFeedStats - 
   * 
   *  - 
   */
    void updateFeedStats(TimerTask thread) {
        WebStatService aService;
        for (int i = 0; i < WebStatService.services.length; i++) {
            aService = WebStatService.services[i];
            aService.prepareService();
            log.fine("updateFeedStats: " + aService);
            synchronized (feedStatCollection) {
                log.config("Entering feedStatCollection iteration");
                Collection feedStatCollectionValues = feedStatCollection.entrySet();
                Iterator iter = feedStatCollectionValues.iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    FeedStats aFeedStats = (FeedStats) entry.getValue();
                    FeedKey aFeedKey = (FeedKey) entry.getKey();
                    log.finer("Updating for: " + aFeedKey);
                    aService.beginUpdate(aFeedKey, aFeedStats);
                    aService.updateStats(aFeedKey, aFeedStats);
                    aService.endUpdate(aFeedKey, aFeedStats);
                    log.fine("FeedStats for: " + aFeedKey + " are now: " + aFeedStats);
                    try {
                        Thread.sleep(isDebugMode() ? 100 : 2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            aService.endService();
        }
        log.config("Completed feedStatCollection iteration");
        checkConsistency();
    }

    /**
   * get/setfeedStatCollection - Get and set the collection of 
   * FeedStats. These calls are there only for the purpose of the XMLEncoder 
   * and XMLDecoder functions.
   * 
   * This is tricky: by having a getter and setter on a collection it is recognized as
   * a property and saved correctly by the XMLEncode/Decode mechanism. It also correctly 
   * figures out that the IDs here correspond to the IDs of FeedStats pointed to by individual CGEs.
   */
    public void setFeedStatCollection(Map newFeedStatCol) {
        feedStatCollection = newFeedStatCol;
    }

    public Map getFeedStatCollection() {
        return feedStatCollection;
    }

    /**
   * checkObjects - Run through all the objects of the Global Model and 
   * check for consistency. 
   * 
   *  - 
   */
    private void checkConsistency() {
        log.fine("Checking GlobalModel consistency...");
        Collection feedStatCollectionValues = feedStatCollection.entrySet();
        Iterator iter = feedStatCollectionValues.iterator();
        log.fine("Iterating through feedStatCollection");
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            FeedStats aFeedStats = (FeedStats) entry.getValue();
            FeedKey aFeedKey = (FeedKey) entry.getKey();
            ChannelGuideEntry cge = lookupFeedStats(aFeedStats);
            if (cge == null) log.fine("**** FeedStats(Key=" + aFeedKey + ") object not found in any CGE: " + aFeedStats); else log.fine("Key: " + aFeedKey + " OK FeedStats: " + aFeedStats);
        }
        log.fine("Iterating through all CGEs");
        IterateAllCGEs cgeIter = new IterateAllCGEs();
        while (cgeIter.hasNext()) {
            ChannelGuideEntry cgeTemp;
            cgeTemp = cgeIter.next();
            if (cgeTemp.getStats() == null || feedStatCollection.containsValue(cgeTemp.getStats())) log.fine("CGE: " + cgeTemp + " OK"); else log.fine("**** CGE: " + cgeTemp + "contains missing Stats reference");
        }
    }

    /**
  * lookupFeedStats - Given a FeedStats object, locate the corresponding
  * ChannelGuideEntry. 
  * @TODO: this is really inefficient. Probably needs a hashtable instead.
  * 
  * @param fs
  * @return - 
  */
    private ChannelGuideEntry lookupFeedStats(FeedStats fs) {
        IterateAllCGEs iter = new IterateAllCGEs();
        while (iter.hasNext()) {
            ChannelGuideEntry cgeTemp;
            FeedStats tempStats;
            cgeTemp = iter.next();
            tempStats = cgeTemp.getStats();
            if (tempStats == fs) return cgeTemp;
        }
        return null;
    }

    /**
   * setStateDb - JDBC Connection to database that contains the Informa State.
   *  
   * @param object - Connection, or null if there isn't one. 
   */
    public void setStateDb(Connection db) {
        stateDb = db;
        if (db == null) return;
        InformaBackEnd backend = channelGuideSet.getInformaBackEnd();
        backend.connect(db);
    }
}

/**
 * ModelPersister - Timer Task which is called periodically to make sure that the
 * current overall state of BB up to date on disk.
 * @TODO: Deal with Synchronization issues
 * @TODO: Does this task belong here or at a more global level?
 * 
 */
class ModelPersister extends TimerTask {

    private Logger log = Logger.getLogger(this.getClass().getName());

    GlobalModel theModel;

    ModelPersister(GlobalModel amodel) {
        theModel = amodel;
    }

    /**
   * run - Produces a single file in the preferences area which will
   * contain the whole persisted state. The whole file is written out each time
   */
    public void run() {
        log.severe("Executing Model Persister.");
        Thread.currentThread().setName("BB Model Persister");
        synchronized (theModel.getFeedStatCollection()) {
            GlobalController.SINGLETON.persistAsXML();
        }
        log.config("Model Persister completed.");
    }
}

/**
 * WebStatServiceTask - Runs as a BlogBridge Timer Deamon and periodically updates
 * statistical information we have about the feeds.
 * 
 */
class WebStatServiceTask extends TimerTask {

    GlobalModel theModel;

    private Logger log = Logger.getLogger(this.getClass().getName());

    WebStatServiceTask(GlobalModel amodel) {
        theModel = amodel;
    }

    public void run() {
        log.severe("Executing WebStat Collection.");
        Thread.currentThread().setName("BB Webstat Collector");
        theModel.updateFeedStats(this);
        log.config("WebStat Collection completed.");
    }
}
