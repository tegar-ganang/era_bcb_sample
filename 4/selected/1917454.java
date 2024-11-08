package com.salas.bb.channelguide;

import java.util.logging.Logger;
import com.salas.bb.core.GlobalModel;
import de.nava.informa.impl.hibernate.Channel;
import de.nava.informa.utils.PersistChanGrpMgr;

/**
 * BasicInformaCGE - CGE Based on Informa...
 * 
 */
public class PersistentInformaCGE extends InformaCGE {

    private Logger log = Logger.getLogger(this.getClass().getName());

    public PersistentInformaCGE() {
        super();
        log.finer("Constructed");
    }

    /**
   * rssBackEndHandleIdenticalTo - Override method so that we apply Hibernate's 
   * persistent object identity check.
   * 
   * @param id - RSSBackEndHandle being compared
   * @return - true if id is hibermate identical to the Channel that this CGE represents
   */
    public boolean rssBackEndHandleIdenticalTo(Object id) {
        if (!((rssBackEndhandle() instanceof Channel) && (id instanceof Channel))) return false;
        Channel thisChannel = (Channel) rssBackEndhandle();
        Channel thatChannel = (Channel) id;
        return thisChannel.getIntId() == thatChannel.getIntId();
    }

    /**
   * initTransientState - 
   *  - 
   */
    public void initTransientState() {
        log.fine("InitTransient state of: " + this);
        PersistChanGrpMgr mgr = getPersistChanGrpMgr();
        Channel informaChannel = mgr.addChannel(getXmlURL());
        if (informaChannel == null || !(informaChannel instanceof Channel)) throw new IllegalStateException();
        if (informaChannel != null) {
            setChannelHandle(informaChannel);
            reloadCachedValues();
        }
    }

    /**
   * getPersistChanGrpMgr - Locate the getPersistChanGrpMgr corresponding to this CGE. getPersistChanGrpMgr
   * is an Informa construct that manages the reading and writing of ChannelGroups in Informa. The 
   * PersistentInformaChannelGuide uses a PersistChanMgr to do its work. 
   * 
   * @return - Corresponding informa Persistent Channel Group Manager
   */
    private PersistChanGrpMgr getPersistChanGrpMgr() {
        PersistentInformaChannelGuide chanGuide = (PersistentInformaChannelGuide) GlobalModel.SINGLETON.getChannelGuideSet().mapCGE2ChannelGuide(this);
        return chanGuide.getPersistChanGrpMgr();
    }

    /**
   * toString - 
   * 
   * @return - 
   */
    public String toString() {
        return "Persistent CGE for " + contentHolder.getTitle() + "(Index=" + indexInChannelGuide + ", arts=" + getArticleCount() + ")";
    }
}
