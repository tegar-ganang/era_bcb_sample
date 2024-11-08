package com.salas.bb.channelguide;

import java.util.Iterator;
import java.util.logging.Logger;
import com.salas.bb.core.GlobalModel;
import de.nava.informa.utils.PersistChanGrpMgr;

/**
 * InformaChannelGuide - description...
 * 
 */
public class PersistentInformaChannelGuide extends ChannelGuide {

    private Logger log = Logger.getLogger(this.getClass().getName());

    PersistChanGrpMgr chanGrpMgr;

    InformaBackEnd informaBackEnd;

    /**
   * initTransientState - 
   *  - 
   */
    public void initTransientState(ChannelGuideSet theChannelGuideSet) {
        log.fine("InitTransient state of: " + this);
        informaBackEnd = (theChannelGuideSet.getInformaBackEnd());
        chanGrpMgr = new PersistChanGrpMgr(informaBackEnd.getSessionHandler(), GlobalModel.SINGLETON.isDebugMode());
        chanGrpMgr.createGroup(getTextName());
        chanGrpMgr.setGlobalObserver(ChannelGuideSet.SINGLETON.getInformaBackEnd());
        log.fine("Created Informa Channel Group for: " + getTextName() + " -> " + chanGrpMgr);
        setSelectedCGE(getEntryAt(0));
        selectedCGE.setSelectedArticle(0);
        Iterator cgeIterator = iterator();
        while (cgeIterator.hasNext()) {
            ((ChannelGuideEntry) cgeIterator.next()).initTransientState();
        }
        chanGrpMgr.notifyChannelsAndItems();
    }

    public PersistChanGrpMgr getPersistChanGrpMgr() {
        return chanGrpMgr;
    }

    public void activateDaemons() {
        log.config("Activating Demons for: " + this);
        ChannelGuideSet theSet = GlobalModel.SINGLETON.getChannelGuideSet();
        theSet.getInformaBackEnd().activateMemoryChannels4CG(this);
        theSet.getInformaBackEnd().activatePersistentChannels4CG(this);
    }
}
