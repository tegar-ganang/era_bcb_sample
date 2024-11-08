package com.salas.bb.core;

import java.awt.event.ActionEvent;
import java.beans.*;
import java.io.*;
import java.net.URL;
import java.sql.*;
import java.sql.Connection;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import com.jgoodies.model.values.ValueHolder;
import com.jgoodies.swing.application.*;
import com.salas.bb.channelguide.*;
import com.salas.bb.dialogs.*;
import com.salas.bb.utils.LoggingPersistenceDelegate;
import com.salas.bb.views.MainFrame;
import de.nava.informa.impl.hibernate.Channel;

/**
 * GlobalController - Implements all the behaviors of commands in the app. 
 * A key design question is whether the selection is part of the model or of the controller. I concluded that
 * to have the stickyness of selections in channels, and items, it should be part of the model.
 * 
 */
public final class GlobalController {

    private Logger log = Logger.getLogger(this.getClass().getName());

    public static final String SELECT_ACTIVE = "selectActive";

    public static final String SELECT_BETS = "selectBestBets";

    public static final String SELECT_SUGGESTED = "selectSuggested";

    public static final String SELECT_GUIDE = "selectGuide";

    public static final String SELECT_FAVORITES = "selectFavorites";

    public static final String ADD_CHANNEL = "addChannel";

    public static final String APPEND_CHANNEL = "appendChannel";

    public static final String DEL_CHANNEL = "delChannel";

    public static final String CHANNEL_PROPERTIES = "showProps";

    public static final String MARK_READ = "markRead";

    public static final String MARK_UNREAD = "markUnread";

    public static final String SORT_CHANNELS = "sortChans";

    public static final String SHOW_NEXTUNREAD = "goNextNew";

    public static final String MARK_ALLREAD = "catchUp";

    public static final String MARK_FAVORITE = "markFavorite";

    public static final String SORT_ARTICLES = "sortArticles";

    public static final String ARTICLE_PROPERTIES = "showArtProps";

    public static final GlobalController SINGLETON = new GlobalController();

    /**
   * Constructor of the GlobalController. Note that this is called early in the 
   * launch of the application, before any of the UI has been built. Do not call
   * any UI related methods here because they will fail. 
   */
    private GlobalController() {
        log.fine("Constructing GlobalController");
        registerActions();
    }

    /**
   * selectGuide - Choose which of the Channel Guides is selected and therefore active in the UI
   * 
   * @param selected - For now, one of GlobalModel.ACTIVE, SUGGESTED, ... etc.
   */
    public void selectGuide(String selected) {
        log.fine("selectGuide: " + selected);
        GlobalModel.SINGLETON.setSelectedGuide(selected);
    }

    public void selectCGE(ChannelGuideEntry theChan) {
        log.fine("selectCGE: " + theChan);
        GlobalModel.SINGLETON.setSelectedChannel(theChan);
        getMainframe().showSelectedCGE();
    }

    /**
   * selectArticle - 
   * 
   * @param i - 
   */
    public void selectArticle(int i) {
        log.fine("selectArticle: " + i);
        GlobalModel.SINGLETON.setSelectedArticle(i);
        getMainframe().showArticleSelected(i);
    }

    public MainFrame getMainframe() {
        AbstractMainFrame theMF = Workbench.getMainFrame();
        assert theMF != null;
        return (MainFrame) (theMF);
    }

    /**
   * registerActions - Register actions in the Action Manager so 
   * they can be dispatched. 
   */
    public void registerActions() {
        ActionManager.register(SELECT_ACTIVE, selectGuideAction(ChannelGuideSet.ACTIVE));
        ActionManager.register(SELECT_BETS, selectGuideAction(ChannelGuideSet.BESTBETS));
        ActionManager.register(SELECT_FAVORITES, selectGuideAction(ChannelGuideSet.FAVORITE));
        ActionManager.register(SELECT_SUGGESTED, selectGuideAction(ChannelGuideSet.SUGGESTED));
        ActionManager.register(SELECT_GUIDE, selectGuideAction(ChannelGuideSet.GUIDE));
        ActionManager.register(MARK_READ, logAction("Command: Mark as Read"));
        ActionManager.register(MARK_UNREAD, logAction("Command: Mark as Unread"));
        ActionManager.register(SORT_CHANNELS, logAction("Command: Sort Channels"));
        ActionManager.register(SHOW_NEXTUNREAD, logAction("Command: Show next unread"));
        ActionManager.register(MARK_ALLREAD, logAction("Command: Mark all read"));
        ActionManager.register(MARK_FAVORITE, logAction("Command: Mark as Favorite"));
        ActionManager.register(SORT_ARTICLES, logAction("Command: Sort Articles"));
        ActionManager.register(DEL_CHANNEL, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                cmdSimpleDelChannel(GlobalModel.SINGLETON.getChannelGuideSet().selectedGuide(), GlobalModel.SINGLETON.getChannelGuideSet().selectedGuide().selectedCGE());
            }
        });
        ActionManager.register(ADD_CHANNEL, new AbstractAction() {

            ValueHolder name = new ValueHolder();

            ValueHolder URL = new ValueHolder();

            public void actionPerformed(ActionEvent e) {
                new AddChannelDialog(getMainframe(), name, URL).open();
                log.config("Command: Add Channel " + name.getValue() + ", at " + URL.getValue());
                if (name.getValue() != null && URL.getValue() != null) {
                    cmdAddPersistentChannel(GlobalModel.SINGLETON.getChannelGuideSet().selectedGuide(), GlobalModel.SINGLETON.getChannelGuideSet().selectedGuide().selectedCGE(), (String) URL.getValue(), (String) name.getValue());
                }
                ;
            }
        });
        ActionManager.register(APPEND_CHANNEL, new AbstractAction() {

            ValueHolder name = new ValueHolder();

            ValueHolder URL = new ValueHolder();

            public void actionPerformed(ActionEvent e) {
                new AddChannelDialog(getMainframe(), name, URL).open();
                log.config("Command: Add Channel " + name.getValue() + ", at " + URL.getValue());
                if (name.getValue() != null && URL.getValue() != null) {
                    cmdSimpleAddChannel(GlobalModel.SINGLETON.getChannelGuideSet().selectedGuide(), null, (String) URL.getValue(), (String) name.getValue());
                }
                ;
            }
        });
        ActionManager.register(CHANNEL_PROPERTIES, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                new ChannelPropertiesDialog(getMainframe(), GlobalModel.SINGLETON.getChannelGuideSet().selectedGuide().selectedCGE()).open();
            }
        });
        ActionManager.register(ARTICLE_PROPERTIES, new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                new ArticlePropertiesDialog(getMainframe(), GlobalModel.SINGLETON.getSelectedArticle()).open();
            }
        });
    }

    /**
   * logAction - Returns a null action which simply logs that the action
   * was called.
   * 
   * @param text - Text to log
   * @return - AbstractAction
   */
    private AbstractAction logAction(final String text) {
        return new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                log.config(text);
            }
        };
    }

    /**
   * selectGuideAction - Returns an action which calls GlobalModel.SINGLETON.setSelectedGuide for
   * a certain ChannelGuide.  
   * 
   * @param theGuide - String name of the guide
   * @return - Abstract action.
   */
    private AbstractAction selectGuideAction(final String theGuide) {
        return new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                GlobalModel.SINGLETON.setSelectedGuide(theGuide);
            }
        };
    }

    /**
   * persistAsXML - We create a single file representing the overall state of GlobalModel
   * and all its dependent objects. We use the XMLEncoder/Decoder mechanism. 
   */
    public synchronized void persistAsXML() {
        log.config("persistAsXML() called");
        String contextpath = getContextPath();
        BufferedOutputStream stateFile;
        try {
            String path = contextpath + "blogbridge.xml";
            stateFile = new BufferedOutputStream(new FileOutputStream(path));
            XMLEncoder xmlState = new XMLEncoder(stateFile);
            xmlState.setPersistenceDelegate(URL.class, new LoggingPersistenceDelegate() {

                protected Expression instantiate(Object oldInstance, Encoder out) {
                    return new Expression(oldInstance, oldInstance.getClass(), "new", new Object[] { oldInstance.toString() });
                }
            });
            xmlState.writeObject(GlobalModel.SINGLETON);
            xmlState.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * unPersistFromXML - Counterpart of persistAsXML. Given the state saved using the XMLEncoder
   * bean model, this method Decodes it back and returns a GlobalModelPersist object
   * representing the state of GlobalModel that was persisted.
   *    
   */
    public synchronized GlobalModel unPersistFromXML() {
        log.info("restoreFromXML() called");
        String contextpath = getContextPath();
        BufferedInputStream stateFile;
        GlobalModel persistentModel = null;
        try {
            String path = contextpath + "blogbridge.xml";
            log.config("Opening: " + path);
            stateFile = new BufferedInputStream(new FileInputStream(path));
            XMLDecoder xmlstate = new XMLDecoder(stateFile);
            persistentModel = (GlobalModel) xmlstate.readObject();
        } catch (Exception e) {
            log.severe("unPersistXML exception. Continuing by building dummy data!");
            persistentModel = null;
        }
        return persistentModel;
    }

    /**
   * Reads in the last saved persistent state and restores
   * the Model to that state. If there is no saved persistent state, then we generate
   * test data.
   */
    public void restorePersistentState() {
        GlobalModel savedState = unPersistFromXML();
        Connection stateDb = openStateDatabase();
        assert stateDb != null;
        if (savedState == null) {
            GlobalModel.setSINGLETON(new GlobalModel());
            GlobalModel.SINGLETON.generateFakeState();
        } else if (savedState != null) {
            GlobalModel.setSINGLETON(savedState);
            GlobalModel.SINGLETON.setStateDb(stateDb);
            GlobalModel.SINGLETON.initTransientState();
        }
    }

    /**
   * openStateDatabase - Open the database that will be used by Informa.
   *  - returns Connection, or Null if hard failure and database cannot be opened.
   */
    private Connection openStateDatabase() {
        Connection stateDb = null;
        String contextPath = getContextPath();
        try {
            String full_path = "jdbc:hsqldb:" + contextPath + "blogbridge";
            Class.forName("org.hsqldb.jdbcDriver");
            stateDb = DriverManager.getConnection(full_path, "sa", "");
        } catch (SQLException e) {
            log.severe("openStateDatabase failed with: " + e.getLocalizedMessage());
        } catch (ClassNotFoundException e) {
            log.severe("openStateDatabase failed with: " + e.getLocalizedMessage());
        }
        return stateDb;
    }

    /**
   * getContextPath - Compute where on the user's machine we will place various state files.
   * @return - 
   */
    public String getContextPath() {
        String userHomePath = System.getProperty("user.home");
        String nodePath = '.' + Workbench.getGlobals().getPreferencesNode();
        String contextPath = userHomePath + File.separatorChar + nodePath + File.separatorChar;
        return contextPath;
    }

    /**
   * getInformaHdlr - 
   * 
   *  - 
   */
    private InformaBackEnd getInformaHdlr() {
        return GlobalModel.SINGLETON.getChannelGuideSet().getInformaBackEnd();
    }

    /**
   * cmdSimpleAddChannel - 
   * 
   * @param guide - ChannelGuide to which we are adding
   * @param selected - CGE after which we add. null means we are adding at end
   * @param url - URL of actual RSS feed, if known
   * @param desc - Text Description of RSS feed, if known
   */
    public void cmdSimpleAddChannel(ChannelGuide guide, ChannelGuideEntry selected, String url, String desc) {
        PersistentInformaCGE cge = new PersistentInformaCGE();
        Object channelHandle;
        channelHandle = getInformaHdlr().addNewMemoryChannel(url, true);
        log.fine("Adding Channel into: " + guide + " at " + selected + "\n  (" + desc + "/" + url);
        if (channelHandle != null) {
            cge.setChannelHandle(channelHandle);
            cge.setMajorFields(url, desc);
            if (selected != null) {
                guide.insertEntryAfter(selected, cge);
                log.fine("Added Channel: " + cge);
            } else if (selected == null) {
                guide.appendNewEntry(cge);
                log.fine("Appended Channel: " + cge);
            }
        }
    }

    /**
   * cmdSimpleAddChannel - 
   * 
   * @param guide - ChannelGuide to which we are adding
   * @param selected - CGE after which we add. null means we are adding at end
   * @param url - URL of actual RSS feed, if known
   * @param desc - Text Description of RSS feed, if known
   */
    public void cmdAddPersistentChannel(ChannelGuide guide, ChannelGuideEntry selected, String url, String desc) {
        if (!(guide instanceof PersistentInformaChannelGuide)) throw new IllegalArgumentException("Can only add a PersistentInformaCGE to a PersistenInformaChannelGuide");
        PersistentInformaChannelGuide pguide = (PersistentInformaChannelGuide) guide;
        PersistentInformaCGE cge = new PersistentInformaCGE();
        Channel channelHandle;
        channelHandle = (Channel) getInformaHdlr().addNewPersistentChannel(pguide, url);
        log.fine("Adding PERSISTENT Channel into: " + guide + " at " + selected + "\n  (" + desc + "/" + url);
        if (channelHandle != null) {
            cge.setChannelHandle(channelHandle);
            cge.setMajorFields(url, desc);
            if (selected != null) {
                guide.insertEntryAfter(selected, cge);
                log.fine("Added Channel: " + cge);
            } else if (selected == null) {
                guide.appendNewEntry(cge);
                log.fine("Appended Channel: " + cge);
            }
            pguide.getPersistChanGrpMgr().notifyChannelsAndItems(channelHandle);
        }
    }

    /**
   * cmdSimpleDelChannel - Delete specified Channel in specified Guide
   * 
   * @param guide - Channel Guide that contains the CGE to be deleted
   * @param sel - CGE to be deleted.
   */
    public void cmdSimpleDelChannel(ChannelGuide guide, ChannelGuideEntry sel) {
        if (guide.deleteEntry(sel)) {
            log.config("Delete channel succeeded.");
        } else {
            log.config("Can't delete last channel.");
        }
    }
}
