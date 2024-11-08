package org.anuta.xmltv;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import org.anuta.imdb.IMDBAccess;
import org.anuta.imdb.beans.MovieRating;
import org.anuta.xmltv.beans.Channel;
import org.anuta.xmltv.beans.Program;
import org.anuta.xmltv.beans.ProgramComparator;
import org.anuta.xmltv.beans.Rating;
import org.anuta.xmltv.cache.CacheManager;
import org.anuta.xmltv.exceptions.TransportException;
import org.anuta.xmltv.xmlbeans.Credits;
import org.anuta.xmltv.xmlbeans.Image;
import org.anuta.xmltv.xmlbeans.Lstring;
import org.anuta.xmltv.xmlbeans.Programme;
import org.anuta.xmltv.xmlbeans.Subtitles;
import org.anuta.xmltv.xmlbeans.TvDocument;
import org.anuta.xmltv.xmlbeans.TvDocument.Tv;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.Oustermiller.util.StringHelper;

public class XMLTVGrabberTask implements Runnable {

    private static final String COMA = ",";

    private static final Log log = LogFactory.getLog(XMLTVGrabberTask.class);

    private int day = 0;

    private long maxOverlap = 10;

    private int overlapFixMode = 0;

    private Set starRatingGanres = new HashSet();

    private CacheManager cache = null;

    private IMDBAccess imdbAccess = null;

    private Channel channel = null;

    private TvDocument result = null;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public CacheManager getCache() {
        return cache;
    }

    public void setCache(CacheManager cache) {
        this.cache = cache;
    }

    public Set getStarRatingGanres() {
        return starRatingGanres;
    }

    public void setStarRatingGanres(Set starRatingGanres) {
        this.starRatingGanres = starRatingGanres;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public List<String> getDelimitedValues(String value, String delimiter, boolean processAnd) {
        if (value == null) {
            return null;
        }
        String values = value.trim();
        if (values.length() == 0) {
            return null;
        }
        if (processAnd) {
            values = values.replaceAll(" en ", delimiter);
            if (values.endsWith("e.a")) {
                values = values.substring(0, values.length() - 3);
            }
        }
        StringTokenizer st = new StringTokenizer(values, delimiter);
        List<String> ret = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token != null) {
                token = token.trim();
                if (token.length() > 0) {
                    ret.add(token);
                }
            }
        }
        if (ret.size() == 0) {
            return null;
        } else {
            return ret;
        }
    }

    private void adjustTiming(List programs) {
        if ((programs == null) || (programs.size() < 2)) {
            return;
        }
        for (int i = 0; i < (programs.size() - 1); i++) {
            Program p1 = (Program) programs.get(i);
            Program p2 = (Program) programs.get(i + 1);
            if (p1.getEndDate().after(p2.getStartDate())) {
                if (log.isDebugEnabled()) {
                    log.debug("Overlap detected " + p1 + " " + p2);
                }
                long overlap = p1.getEndDate().getTime() - p2.getStartDate().getTime();
                long overlapMinutes = Math.round((float) overlap / 60000L);
                if (log.isDebugEnabled()) {
                    log.debug("Overlapped on " + overlapMinutes + " minutes");
                }
                if (overlapMinutes <= getMaxOverlap()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Fix overlap with mode " + getOverlapFixMode());
                    }
                    if (0 == getOverlapFixMode()) {
                        overlap = overlap >> 1;
                        p1.getEndDate().setTime(p1.getEndDate().getTime() - overlap);
                        p2.setStartDate(p1.getEndDate());
                    } else if (2 == getOverlapFixMode()) {
                        p1.setEndDate(p2.getStartDate());
                    } else if (1 == getOverlapFixMode()) {
                        p2.setStartDate(p1.getEndDate());
                    } else {
                        throw new IllegalArgumentException("Overlap mode can be 0,1 or 2");
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Overlap is too long, lets mark it as special");
                    }
                    p1.setClumpIdx("0/2");
                    p2.setClumpIdx("1/2");
                }
            }
        }
    }

    public long getMaxOverlap() {
        return maxOverlap;
    }

    public void setMaxOverlap(long maxOverlap) {
        this.maxOverlap = maxOverlap;
    }

    public int getOverlapFixMode() {
        return overlapFixMode;
    }

    public void setOverlapFixMode(int overlapFixMode) {
        this.overlapFixMode = overlapFixMode;
    }

    public IMDBAccess getImdbAccess() {
        return imdbAccess;
    }

    public void setImdbAccess(IMDBAccess imdbAccess) {
        this.imdbAccess = imdbAccess;
    }

    public void run() {
        SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyyMMddHHmmss ZZZZ");
        result = TvDocument.Factory.newInstance();
        Tv tv = result.addNewTv();
        tv.setGeneratorInfoName("anuta xmltv generator");
        tv.setGeneratorInfoUrl("http://www.anuta.org/xmltv");
        tv.setDate(sdfDateTime.format(new Date()));
        tv.setSourceDataUrl("http://www.tvgids.nl");
        if (log.isInfoEnabled()) {
            log.info("Processing channel " + channel);
        }
        List<Program> programs;
        try {
            programs = channel.getGrabber().getPrograms(channel, new Date(), day);
        } catch (TransportException e) {
            log.error("Eror getting programs for channel " + channel + " Day: " + day, e);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Sorting programs");
        }
        Collections.sort(programs, new ProgramComparator());
        adjustTiming(programs);
        for (Program shortProgram : programs) {
            Programme prog = null;
            if (getCache() != null) {
                try {
                    prog = getCache().getFromCache(shortProgram.getStartDate(), shortProgram.getId());
                } catch (Exception e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Cache error occured, reloading");
                    }
                }
            }
            if (prog == null) {
                Program fullProgram;
                try {
                    fullProgram = channel.getGrabber().getProgram(shortProgram);
                } catch (TransportException e) {
                    log.error("Error getting full data for " + prog);
                    fullProgram = shortProgram;
                }
                if (log.isInfoEnabled()) {
                    log.info(fullProgram);
                }
                prog = tv.addNewProgramme();
                if ((fullProgram.getDescription() != null) && (fullProgram.getDescription().length() > 0)) {
                    Lstring desc = prog.addNewDesc();
                    desc.setLang(channel.getLanguage());
                    desc.setStringValue(StringHelper.unescapeHTML(fullProgram.getDescription()));
                }
                prog.setChannel(channel.getXmltvChannelId());
                if (fullProgram.getPremiere() != null) {
                    prog.setDate(fullProgram.getPremiere());
                }
                prog.setStart(sdfDateTime.format(fullProgram.getStartDate()));
                prog.setStop(sdfDateTime.format(fullProgram.getEndDate()));
                Lstring title = prog.addNewTitle();
                title.setLang(channel.getLanguage());
                title.setStringValue(StringHelper.unescapeHTML(fullProgram.getLongTitle()));
                if (fullProgram.getGanre() != null) {
                    String ganre = channel.getGrabber().getMappedGanre(fullProgram.getGanre());
                    Lstring cat = prog.addNewCategory();
                    cat.setLang(channel.getLanguage());
                    cat.setStringValue(StringHelper.unescapeHTML(ganre));
                    if (getImdbAccess() != null) {
                        if (getStarRatingGanres().contains(ganre.toLowerCase())) {
                            int year = 0;
                            if (fullProgram.getPremiere() != null) {
                                try {
                                    year = Integer.parseInt(fullProgram.getPremiere());
                                } catch (NumberFormatException e) {
                                    if (log.isTraceEnabled()) {
                                        log.trace("Unable to parce premiere year");
                                    }
                                }
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("Checking imdb for rating of " + fullProgram.getTitle() + " " + fullProgram.getSubTitle() + " " + year);
                            }
                            MovieRating starRating = getImdbAccess().getRating(fullProgram.getTitle(), fullProgram.getSubTitle(), year);
                            if (starRating != null) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Found rating " + starRating);
                                }
                                prog.addNewStarRating().setValue(starRating.getFormattedRating());
                            }
                        }
                    }
                }
                if ((fullProgram.getActors() != null) || (fullProgram.getDirectors() != null) || (fullProgram.getPresentors() != null)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Add credits");
                    }
                    Credits credits = prog.addNewCredits();
                    List<String> list = getDelimitedValues(fullProgram.getDirectors(), COMA, true);
                    if (list != null) {
                        for (String string : list) {
                            String tmp = StringHelper.unescapeHTML(string);
                            credits.addDirector(tmp);
                        }
                    }
                    list = getDelimitedValues(fullProgram.getActors(), COMA, true);
                    if (list != null) {
                        for (String string : list) {
                            String tmp = StringHelper.unescapeHTML(string);
                            credits.addActor(tmp);
                        }
                    }
                    list = getDelimitedValues(fullProgram.getPresentors(), COMA, true);
                    if (list != null) {
                        for (String string : list) {
                            String tmp = StringHelper.unescapeHTML(string);
                            credits.addPresenter(tmp);
                        }
                    }
                }
                if (fullProgram.getSpecials() != null) {
                    List<String> list = getDelimitedValues(fullProgram.getSpecials(), COMA, false);
                    if (list != null) {
                        for (String tmp : list) {
                            if (log.isDebugEnabled()) {
                                log.debug("Process special: " + tmp);
                            }
                            if ("breedbeeld uitzending".equalsIgnoreCase(tmp)) {
                                if (prog.getVideo() == null) {
                                    prog.addNewVideo();
                                }
                                prog.getVideo().setAspect("16:9");
                            } else if ("teletekst ondertiteld".equalsIgnoreCase(tmp)) {
                                Subtitles st = prog.addNewSubtitles();
                                st.setType(Subtitles.Type.TELETEXT);
                                Lstring lang = st.addNewLanguage();
                                lang.setLang(channel.getLanguage());
                                lang.setStringValue("Nederlands");
                            } else if ("stereo".equalsIgnoreCase(tmp)) {
                                if (prog.getAudio() == null) {
                                    prog.addNewAudio();
                                }
                                prog.getAudio().setStereo("stereo");
                            } else if ("dolby surround".equalsIgnoreCase(tmp)) {
                                if (prog.getAudio() == null) {
                                    prog.addNewAudio();
                                }
                                prog.getAudio().setStereo("surround");
                            }
                        }
                    }
                }
                if (fullProgram.getRating().size() > 0) {
                    for (Rating rat : fullProgram.getRating()) {
                        org.anuta.xmltv.xmlbeans.Rating rating = prog.addNewRating();
                        rating.setSystem(rat.getSystem());
                        rating.setValue(StringHelper.unescapeHTML(rat.getValue()));
                        Image icon = rating.addNewIcon();
                        icon.setSrc(rat.getIcon());
                    }
                }
                if (fullProgram.getSubTitle() != null) {
                    Lstring subtitle = prog.addNewSubTitle();
                    subtitle.setLang(channel.getLanguage());
                    subtitle.setStringValue(StringHelper.unescapeHTML(fullProgram.getSubTitle()));
                }
                if (fullProgram.getClumpIdx() != null) {
                    prog.setClumpidx(fullProgram.getClumpIdx());
                }
                try {
                    if (getCache() != null) {
                        getCache().saveInCache(fullProgram.getStartDate(), fullProgram.getId(), prog);
                    }
                } catch (Exception e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to save document to cache " + e);
                    }
                }
            } else {
                tv.addNewProgramme().set(prog);
            }
        }
    }

    public TvDocument getResult() {
        return result;
    }

    public void setResult(TvDocument result) {
        this.result = result;
    }
}
