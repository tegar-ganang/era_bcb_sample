package com.salas.bb.channelguide;

import java.net.*;
import java.net.URL;
import java.sql.Connection;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import net.sf.hibernate.HibernateException;
import com.salas.bb.core.*;
import de.nava.informa.core.*;
import de.nava.informa.impl.hibernate.*;
import de.nava.informa.impl.hibernate.SessionHandler;
import de.nava.informa.utils.*;
import de.nava.informa.utils.ChannelRegistry;

/**
 * informaHandler - Common handling of Informa RSS Back End. Note where the 
 * support for BasicInformaBackent(=non persistent) and PersistentInformaBackEnd
 * are the same, that code can be found in this class. Otherwise look in the 
 * derived classes.
 * 
 */
public class InformaBackEnd extends RSSBackEnd implements ChannelObserverIF {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private SessionHandler handler;

    private Connection jdbcConnection;

    private static final int DEFAULT_MEMCHAN_UPDATE_MS = 60000;

    private static final int DBG_MEMCHAN_UPDATE_MS = 4000;

    private ChannelBuilderIF transientChanBuilder;

    private ChannelRegistry channelRegistry;

    int defaultPollingInterval = 180;

    public InformaBackEnd() {
        super();
        jdbcConnection = null;
        transientChanBuilder = new de.nava.informa.impl.basic.ChannelBuilder();
        channelRegistry = new ChannelRegistry(transientChanBuilder);
    }

    /**
   * mapChannel2CGE - Convert an Informa ChannelIF to the corresponding ChannelGuideEntry
   * 
   * @param theChan
   * @return - 
   */
    protected ChannelGuideEntry mapChannel2CGE(Object theChan) {
        return ChannelGuideSet.SINGLETON.mapRSSChan2CGE((ChannelIF) theChan);
    }

    /**
   * itemAdded - Called by Informa whenever the parsing of a feed produces a new 
   * ItemIF. 
   * 
   * @param informaItem - ItemIF of new item that Informa reported has been added.
   */
    public void itemAdded(final ItemIF informaItem) {
        log.fine("itemAdded called" + informaItem);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                InformaCGE theCGE;
                try {
                    theCGE = (InformaCGE) mapChannel2CGE(informaItem.getChannel());
                } catch (IllegalArgumentException e) {
                    log.fine("Item Retrieved via Hibernate for which there is no CGE." + informaItem);
                    return;
                }
                theCGE.addItem(informaItem);
                ArticleListModel aListModl = ArticleListModel.SINGLETON;
                ChannelGuideEntry selCGE = GlobalModel.SINGLETON.getSelectedCGE();
                log.finer("itemAdded: cge = " + theCGE + ", sel CGE = " + selCGE);
                if (selCGE == theCGE && theCGE.getArticleCount().intValue() == 1) GlobalController.SINGLETON.selectArticle(0);
                theCGE.reSortItems();
                theCGE.reloadCachedValues();
            }
        });
    }

    /**
   * channelRetrieved - Called by Informa whenever the parsing of a feed produces a 
   * new ChannelIF.
   * 
   * @param Informa ChannelIF which was retrieved and therefore updated.
   */
    public void channelRetrieved(final ChannelIF theChan) {
        InformaCGE theCGE;
        try {
            theCGE = (InformaCGE) mapChannel2CGE(theChan);
        } catch (IllegalArgumentException e) {
            log.severe("Channel Retrieved via Hibernate for which there is no CGE:" + theChan);
            return;
        }
        URL siteURL = theChan.getSite();
        if (siteURL != null) theCGE.updateHtmlURL(siteURL.toString());
        URL xmlURL = theChan.getLocation();
        if (xmlURL != null) theCGE.setXmlURL(xmlURL.toString());
        theCGE.getCGEContentHolder().setDescription(theChan.getDescription());
        log.config("channelRetrieved called" + theChan);
        SwingUtilities.invokeLater(new UpdateCgeUiThread(theChan, theCGE));
    }

    private final class UpdateCgeUiThread extends Thread {

        private InformaCGE theCGE;

        private ChannelIF theChan;

        public UpdateCgeUiThread(ChannelIF aChan, InformaCGE aCGE) {
            super();
            theCGE = aCGE;
            theChan = aChan;
        }

        public void run() {
            theCGE.setTitle(theChan.getTitle());
            theCGE.incRetrievals();
            theCGE.reloadCachedValues();
        }
    }

    /**
   * addNewPersistentChannel - 
   * 
   * @param xmlURL
   * @return - 
   */
    public Object addNewPersistentChannel(PersistentInformaChannelGuide cg, String xmlURL) {
        Channel achannel;
        PersistChanGrpMgr chanGrpMgr = cg.getPersistChanGrpMgr();
        chanGrpMgr.deActivate();
        achannel = chanGrpMgr.addChannel(xmlURL);
        chanGrpMgr.activate();
        return achannel;
    }

    /**
   * activatePersistentChannels4CG - 
   * 
   * @param cg - 
   */
    public void activatePersistentChannels4CG(PersistentInformaChannelGuide cg) {
        cg.chanGrpMgr.activate();
    }

    public void deActivatePersistenChannels4CG(PersistentInformaChannelGuide cg) {
        cg.chanGrpMgr.deActivate();
    }

    /**
   * connect - 
   * 
   * @param connection - 
   */
    public void connect(Connection connection) {
        jdbcConnection = connection;
        try {
            handler = SessionHandler.getInstance();
            handler.setConnection(jdbcConnection);
        } catch (HibernateException e) {
            handler = null;
            e.printStackTrace();
        }
    }

    /**
   * addNewMemoryChannel - Add new non-persistent Informa Channel to Informa, and return a generic handle to it.
   * 
   * @param XmlUrl - String representation of the URL
   * @param activate - boolean indicating whether we want to Activate newly created ChannelIF
   * @return - opaque channel handle (= ChannelIf for informa)
   */
    public Object addNewMemoryChannel(String xmlURL, boolean activate) {
        URL theXML = null;
        ChannelIF aChannel;
        try {
            theXML = new URL(xmlURL);
            aChannel = (ChannelIF) channelRegistry.addChannel(theXML, defaultPollingInterval, activate);
            aChannel.addObserver(this);
            return aChannel;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
   * activateMemoryChannels4CG - 
   * 
   *  - 
   */
    public void activateMemoryChannels4CG(ChannelGuide cg) {
        Iterator it = cg.getChannels().iterator();
        while (it.hasNext()) {
            ChannelGuideEntry entry = (ChannelGuideEntry) it.next();
            ChannelIF channel = (ChannelIF) entry.rssBackEndhandle();
            int chan_upd_ms = GlobalModel.SINGLETON.isDebugMode() ? DBG_MEMCHAN_UPDATE_MS : DEFAULT_MEMCHAN_UPDATE_MS;
            if (channel instanceof de.nava.informa.impl.basic.Channel) channelRegistry.activateChannel(channel, chan_upd_ms);
        }
    }

    public SessionHandler getSessionHandler() {
        return handler;
    }

    public Connection getJDBCConnection() {
        return jdbcConnection;
    }
}
