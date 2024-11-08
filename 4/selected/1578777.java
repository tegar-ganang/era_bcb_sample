package net.sf.jvdr.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.NoSuchElementException;
import net.sf.jvdr.data.comparator.ChannelComparator;
import net.sf.jvdr.data.comparator.VDRTimerComparator;
import net.sf.jvdr.data.ejb.VdrSmartSearch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.Connection;
import org.hampelratte.svdrp.Response;
import org.hampelratte.svdrp.commands.LSTT;
import org.hampelratte.svdrp.responses.highlevel.Channel;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;
import org.hampelratte.svdrp.responses.highlevel.Recording;
import org.hampelratte.svdrp.responses.highlevel.VDRTimer;
import org.hampelratte.svdrp.util.TimerParser;

public class VdrCache {

    static Log logger = LogFactory.getLog(VdrCache.class);

    private Hashtable<Integer, List<EPGEntry>> htEpg;

    private Hashtable<Integer, Channel> htChannel;

    private Hashtable<String, Integer> htChannelTransponder;

    private Hashtable<String, Integer> htChannelName;

    private List<VDRTimer> lTimer;

    private Hashtable<Integer, Recording> htRecordings;

    public VdrCache() {
        htEpg = new Hashtable<Integer, List<EPGEntry>>();
        htChannelTransponder = new Hashtable<String, Integer>();
        htChannelName = new Hashtable<String, Integer>();
        htRecordings = new Hashtable<Integer, Recording>();
    }

    public void updateEpgForChannel(int chNu, List<EPGEntry> lEpg) {
        if (lEpg.size() > 0) {
            htEpg.put(chNu, lEpg);
            EPGEntry epg = lEpg.get(0);
            htChannelTransponder.put(epg.getChannelID(), chNu);
            htChannelName.put(epg.getChannelName(), chNu);
            logger.debug("Update Channel EPG for channel " + chNu + " (" + epg.getChannelID() + "), " + lEpg.size() + " entries");
        } else {
            logger.warn("No EPG entries for channel " + chNu);
        }
    }

    public List<EPGEntry> getEpgForChNu(int chNu, Date fromTime) {
        List<EPGEntry> lEpg = htEpg.get(chNu);
        int fromIndex = 0;
        int toIndex = lEpg.size();
        for (int i = 0; i < lEpg.size(); i++) {
            EPGEntry epg = lEpg.get(i);
            if (isEpgTimeAt(epg, fromTime)) {
                fromIndex = i;
                break;
            }
        }
        return lEpg.subList(fromIndex, toIndex);
    }

    public String getChName(int chNu) {
        return htChannel.get(chNu).getName();
    }

    public int getChNum(String transponder) {
        return htChannelTransponder.get(transponder);
    }

    private boolean isEpgTimeAt(EPGEntry epg, Date atDate) {
        long cut = atDate.getTime();
        long leftT = epg.getStartTime().getTimeInMillis();
        long rightT = epg.getEndTime().getTimeInMillis();
        boolean left = cut >= leftT;
        boolean right = cut < rightT;
        return left && right;
    }

    public EPGEntry getEpgforTime(int chNu, Date atDate, boolean next) {
        List<EPGEntry> lEpg = htEpg.get(chNu);
        int index = 0;
        for (int i = 0; i < lEpg.size(); i++) {
            EPGEntry epg = lEpg.get(i);
            if (isEpgTimeAt(epg, atDate)) {
                index = i;
                break;
            }
        }
        if (next) {
            index = index + 1;
        }
        return lEpg.get(index);
    }

    public void updateChannels(List<Channel> lChannel) {
        htChannel = new Hashtable<Integer, Channel>();
        for (Channel c : lChannel) {
            htChannel.put(c.getChannelNumber(), c);
        }
        logger.debug("Update Channel List. Elements: " + htChannel.size());
    }

    public boolean isTimerConflict(VDRTimer vdrTimer) {
        List<VDRTimer> lConflictTimer = getTimerConflicts(vdrTimer);
        if (lConflictTimer.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public List<VDRTimer> getTimerConflicts(VDRTimer vdrTimer) {
        List<VDRTimer> lConflictTimer = new ArrayList<VDRTimer>();
        if (lTimer == null || lTimer.size() == 0) {
            return lConflictTimer;
        }
        for (VDRTimer savedT : lTimer) {
            if (savedT.getID() != vdrTimer.getID() && VDRTimerComparator.isTimerConflict(savedT, vdrTimer)) {
                lConflictTimer.add(savedT);
            }
        }
        return lConflictTimer;
    }

    private boolean isEpgInRange(EPGEntry epg, Date record, int rangeDown, int rangeUp) {
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(record);
        int minRecord = gc.get(GregorianCalendar.MINUTE) + (gc.get(GregorianCalendar.HOUR_OF_DAY) * 60);
        gc.setTime(epg.getStartTime().getTime());
        int minStart = gc.get(GregorianCalendar.MINUTE) + (gc.get(GregorianCalendar.HOUR_OF_DAY) * 60);
        long rDown = minRecord - (rangeDown);
        long rUp = minRecord + (rangeUp);
        boolean ok = rDown < minStart && minStart < rUp;
        logger.trace(epg.getTitle() + "  " + rDown + " < " + minStart + " < " + rUp + "    " + ok);
        return ok;
    }

    public List<EPGEntry> getSmartEpg(VdrSmartSearch vss) {
        ArrayList<EPGEntry> lEpgRepeats = new ArrayList<EPGEntry>();
        for (Integer chnu : htEpg.keySet()) {
            if (chnu == vss.getChannel() || !vss.isLimitToChannel()) {
                List<EPGEntry> lEpgCandidates = htEpg.get(chnu);
                for (EPGEntry epgCandidate : lEpgCandidates) {
                    boolean titel = epgCandidate.getTitle().startsWith(vss.getSuche());
                    boolean range = true;
                    if (titel && vss.isLimitRange()) {
                        range = isEpgInRange(epgCandidate, vss.getEpgStart(), vss.getRangeDown(), vss.getRangeUp());
                    }
                    if (titel && range) {
                        lEpgRepeats.add(epgCandidate);
                    }
                }
            }
        }
        return lEpgRepeats;
    }

    public List<EPGEntry> getEpgRepeats(EPGEntry epg, boolean allChannels) {
        ArrayList<EPGEntry> lEpgRepeats = new ArrayList<EPGEntry>();
        for (List<EPGEntry> lEpgCandidates : htEpg.values()) {
            for (EPGEntry epgCandidate : lEpgCandidates) {
                boolean titel = epgCandidate.getTitle().equals(epg.getTitle());
                boolean channel = epgCandidate.getChannelName().equals(epg.getChannelName());
                boolean later = epgCandidate.getStartTime().getTimeInMillis() > epg.getStartTime().getTimeInMillis();
                if ((channel || allChannels) && titel && later) {
                    lEpgRepeats.add(epgCandidate);
                }
            }
        }
        return lEpgRepeats;
    }

    public EPGEntry getEpg(String sChNu, String sStartTime) {
        int chNu = new Integer(sChNu);
        return getEpg(chNu, sStartTime);
    }

    public EPGEntry getEpg(int chNu, String sStartTime) {
        long startTime = new Long(sStartTime);
        logger.debug("Looking for chNu=" + chNu + " st=" + startTime);
        List<EPGEntry> lEpg = htEpg.get(chNu);
        for (EPGEntry epg : lEpg) {
            if (epg.getStartTime().getTimeInMillis() == startTime) {
                return epg;
            }
        }
        return lEpg.get(0);
    }

    public void updateRecordings(List<Recording> lRecordings) {
        htRecordings.clear();
        for (Recording r : lRecordings) {
            htRecordings.put(r.getNumber(), r);
        }
    }

    public Recording getRecording(int rid) {
        return htRecordings.get(rid);
    }

    public Collection<Recording> getRecordings() {
        return htRecordings.values();
    }

    public List<Channel> getChannelList() {
        List<Channel> lC = new ArrayList<Channel>();
        for (Integer i : htChannelTransponder.values()) {
            Channel c = htChannel.get(i);
            lC.add(c);
        }
        ChannelComparator jvdrComparator = new ChannelComparator();
        Comparator<Channel> comparator = jvdrComparator.getComparator(ChannelComparator.CompTyp.Number);
        Collections.sort(lC, comparator);
        return lC;
    }

    public VDRTimer getTimer(int tid) throws NoSuchElementException {
        VDRTimer result = null;
        for (VDRTimer timer : lTimer) {
            if (timer.getID() == tid) {
                result = timer;
                break;
            }
        }
        if (result == null) {
            throw new NoSuchElementException("Timer mit id=" + tid + " nicht in der Liste");
        }
        return result;
    }

    public synchronized void fetchTimer(VdrDataFetcherSvdrp vdrD) {
        lTimer = new ArrayList<VDRTimer>();
        try {
            boolean isSvdrConnected = vdrD.isConnected();
            if (!isSvdrConnected) {
                vdrD.connect();
            }
            Connection svdrp = vdrD.getSvdrp();
            Response r = svdrp.send(new LSTT());
            if (!isSvdrConnected) {
                vdrD.disconnect();
            }
            try {
                lTimer = TimerParser.parse(r.getMessage());
            } catch (NoSuchElementException e) {
                logger.warn("No Timer defined.");
            }
        } catch (IOException e) {
            logger.debug(e);
        }
    }

    public Channel getChannel(int chNu) {
        return htChannel.get(chNu);
    }

    public List<VDRTimer> getLTimer() {
        return lTimer;
    }

    public synchronized void setLTimer(List<VDRTimer> timer) {
        lTimer = timer;
    }
}
