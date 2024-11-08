package com.umc.collector;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import tk.korbel.thetvdb.api.datastructures.FullSeries;
import tk.korbel.thetvdb.api.datastructures.MirrorType;
import com.umc.UMCStatistics;
import com.umc.beans.AudioTrack;
import com.umc.beans.BackdropImage;
import com.umc.beans.BannerImage;
import com.umc.beans.CoverImage;
import com.umc.beans.ImageFile;
import com.umc.beans.MovieFile;
import com.umc.beans.MovieFileComparator;
import com.umc.beans.PosterImage;
import com.umc.beans.Warning;
import com.umc.dao.DataAccessFactory;
import com.umc.dao.UMCDataAccessInterface;
import com.umc.filescanner.FilescannerController;
import com.umc.gui.scanner.MainFrame;
import com.umc.helper.UMCConstants;
import com.umc.helper.UMCImageUtils;
import com.umc.helper.UMCParams;
import com.umc.helper.UMCUtils;
import com.umc.online.AbstractFanartPlugin;
import com.umc.online.AbstractImagePlugin;
import com.umc.online.AbstractSeriesDBPlugin;
import com.umc.skins.SkinRenderer;
import de.umcProject.xmlbeans.MovieDocument.Movie;
import de.umcProject.xmlbeans.MovieDocument.Movie.Audio;
import de.umcProject.xmlbeans.MovieDocument.Movie.Cast;
import de.umcProject.xmlbeans.MovieDocument.Movie.Audio.Track;
import de.umcProject.xmlbeans.MovieDocument.Movie.Cast.Actor;

/**
 * Eine Instanz dieser/s Klasse/Threads dient als sogenannter BackgroundWorker
 * für den Filescanner.
 *
 * @author DonGyros
 *
 * @version 0.1 10.08.2008
 */
public class BackgroundWorker_Backup extends Thread {

    /**Logger*/
    private Logger log = null;

    public static Logger logTest = Logger.getLogger("com.umc.test");

    private UMCImageUtils imgUtils = null;

    private SkinRenderer skinRenderer = null;

    private FullSeries fullSeries = null;

    boolean debug = false;

    /**ID dieses BackgroundWorkers*/
    public int identifier = -1;

    public ArrayList<MovieFile> jobs = new ArrayList<MovieFile>();

    private AbstractSeriesDBPlugin pluginSeries = null;

    private AbstractFanartPlugin pluginFanart = null;

    private AbstractImagePlugin pluginImages = null;

    private UMCDataAccessInterface dao = null;

    private CoverImage movieCover = null;

    private BackdropImage movieBackdrop = null;

    /**
	 * initialisiert das logging
	 */
    private void startLogging() {
        try {
            if (log != null) return;
            ConsoleAppender consoleAppendr = new ConsoleAppender();
            consoleAppendr.setName("BACKGROUNDWORKER" + identifier);
            consoleAppendr.setLayout(new PatternLayout("%r %-5p - %C.%M - %m%n"));
            consoleAppendr.activateOptions();
            RollingFileAppender appndr = new RollingFileAppender();
            appndr.setName("BACKGROUNDWORKER" + identifier);
            appndr.setLayout(new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %r [%t] %-5p %c - %C.%M - %m%n"));
            appndr.setFile("logs/BackgroundWorker_" + identifier + ".log");
            appndr.activateOptions();
            log = Logger.getLogger("BW" + identifier);
            if (UMCParams.getInstance().getBackgroundWorkerLoglevel() != null) {
                if (UMCParams.getInstance().getBackgroundWorkerLoglevel().equals("FATAL")) log.setLevel(Level.FATAL); else if (UMCParams.getInstance().getBackgroundWorkerLoglevel().equals("ERROR")) log.setLevel(Level.ERROR); else if (UMCParams.getInstance().getBackgroundWorkerLoglevel().equals("WARN")) log.setLevel(Level.WARN); else if (UMCParams.getInstance().getBackgroundWorkerLoglevel().equals("INFO")) log.setLevel(Level.INFO); else if (UMCParams.getInstance().getBackgroundWorkerLoglevel().equals("DEBUG")) log.setLevel(Level.DEBUG); else if (UMCParams.getInstance().getBackgroundWorkerLoglevel().equals("TRACE")) log.setLevel(Level.TRACE);
            } else {
                log.setLevel(Level.INFO);
            }
            log.setAdditivity(false);
            if (Logger.getLogger("BW" + identifier).getAppender("BACKGROUNDWORKER" + identifier) == null) {
                log.addAppender(appndr);
            }
            debug = log.isDebugEnabled();
        } catch (Throwable ex) {
            System.out.println("BackgroundWorker " + BackgroundWorker_Backup.class.getName() + " konnte in kein Logfile schreiben : " + ex);
        }
    }

    /**
	 * Konstruktor
	 *
	 * @param identifier Eine Kennung unter der eine Instanz dieser Klasse zugeordnet wird
	 */
    public BackgroundWorker_Backup(int identifier) {
        this.identifier = identifier;
    }

    /**
	 * Diese Methode versucht den BackgroundWorker zu initialisieren.
	 * Sie wird vom {@link FilescannerController} getriggert.
	 *
	 * @return true/false
	 */
    public boolean init() {
        startLogging();
        log.trace("init()");
        if (debug) log.debug("BackgroundWorker " + identifier + " wurde neu erstellt");
        dao = DataAccessFactory.getUMCDataSourceAccessor(DataAccessFactory.DB_TYPE_SQLITE, UMCParams.getInstance().getDBDriverconnect() + UMCParams.getInstance().getDBName(), UMCParams.getInstance().getDBDriver(), UMCParams.getInstance().getDBUser(), UMCParams.getInstance().getDBPwd());
        if (dao == null) {
            log.fatal("DAO konnte nicht geladen werden");
            return false;
        }
        pluginImages = UMCUtils.loadImagePlugin(UMCParams.getInstance().getMovieCoverPlugin());
        pluginFanart = UMCUtils.loadFanartPlugin("com.umc.plugins.Fanart");
        pluginSeries = UMCUtils.loadSeriesDBPlugin("com.umc.plugins.TheTVDB");
        imgUtils = new UMCImageUtils(log);
        skinRenderer = new SkinRenderer(log);
        return true;
    }

    public void addJob(MovieFile mf) {
        log.trace("addJob()");
        this.jobs.add(mf);
    }

    /**
	 * Zentrale Abarbeitungs-Methode dieses Threads
	 */
    public void run() {
        log.trace("run()");
        log.info("###### Nächster Durchlauf " + GregorianCalendar.getInstance().getTime() + " ######");
        long start = 0;
        long end = 0;
        MovieFile mf = Publisher.getInstance().getBackgroundWorkerJob();
        if (mf != null) {
            try {
                start = System.currentTimeMillis();
                if (mf.isSeriesGroup()) log.info("BW" + identifier + ": Serien-Gruppen Job gefunden => " + mf.getGroupname()); else log.info("BW" + identifier + ": Movie Job gefunden => " + mf.getPath() + UMCConstants.fileSeparator + mf.getFilename() + mf.getFiletype());
                if (mf.isMovieGroup()) {
                    log.info("BW" + identifier + ": Movie-Gruppen Job gefunden => " + mf.getGroupname());
                    for (MovieFile f : mf.getMultiparts()) {
                        getInfosFromXML(f);
                        assignImages2(f);
                        if (mf.getCollectType() != 1) {
                            getMovieInfosOnline(f, f.getIdIMDB(), f.getID());
                        }
                        getMovieFanart(f);
                        getMediaInfos(f);
                    }
                    assignImages2(mf);
                } else if (mf.isSeriesGroup()) {
                    log.debug("getEpisodeInfosFromXML(mf)");
                    getEpisodeInfosFromXML(mf);
                    assignImages2(mf);
                    if (mf.getSeriesXMLfile() != null && (mf.getSeriesXMLfile().getSeries().getIdThetvdb() != null && !mf.getSeriesXMLfile().getSeries().getIdThetvdb().equals(""))) {
                        log.info("get SeriesInfo by ID : " + mf.getSeriesXMLfile().getSeries().getIdThetvdb());
                        fullSeries = pluginSeries.getSeries(Integer.valueOf(mf.getSeriesXMLfile().getSeries().getIdThetvdb()), "de");
                    } else {
                        log.info("get SeriesInfo by Title : " + mf.getGroupname());
                        fullSeries = pluginSeries.getSeries(mf.getGroupname(), "de");
                    }
                    getSeriesInfosOnline(mf);
                    getSeriesFanart(mf);
                } else {
                    getInfosFromXML(mf);
                    assignImages2(mf);
                    if (mf.getCollectType() != 1) getMovieInfosOnline(mf, mf.getIdIMDB(), mf.getID());
                    getMovieFanart(mf);
                    getMediaInfos(mf);
                }
                if (mf.getTitle() == null || mf.getTitle().equals("")) mf.setTitle(mf.getFilename());
                setUGGenres(mf);
                setUGSequence(mf);
                String adjustedTitle = "";
                if (mf.isMovieGroup()) {
                    log.info("BW" + identifier + ": Beginne mit Erzeugung der Frontend Bilder für die Universalgruppe " + mf.getGroupname());
                    skinRenderer.createMovieIndexGraphics(mf);
                    skinRenderer.createMovieDetailsGraphics(mf);
                    for (MovieFile f : mf.getMultiparts()) {
                        log.info("BW" + identifier + ": Beginne mit Erzeugung der Frontend Bilder für das Multipart " + f.getTitle());
                        skinRenderer.createMovieIndexGraphics(f);
                        skinRenderer.createMovieDetailsGraphics(f);
                    }
                } else if (mf.isMultipartGroup()) {
                    log.info("BW" + identifier + ": Beginne mit Erzeugung der Frontend Bilder für die Multipartgruppe " + mf.getTitle());
                    skinRenderer.createMovieIndexGraphics(mf);
                    skinRenderer.createMovieDetailsGraphics(mf);
                    log.info("BW" + identifier + ": Beginne mit Erzeugung der Playlist für die Multipartgruppe " + mf.getTitle());
                    createPlaylist(UMCParams.getInstance().getMediaCenterLocation() + UMCConstants.fileSeparator + "playlists" + UMCConstants.fileSeparator + "video" + UMCConstants.fileSeparator + adjustedTitle + "[MG].jsp", mf);
                } else if (mf.isSeriesGroup()) {
                    log.info("BW" + identifier + ": Beginne mit Erzeugung der Frontend Bilder für die Seriengruppe " + mf.getGroupname());
                    adjustedTitle = getAdjustedTitle(mf.getGroupname());
                    skinRenderer.createMainIndexGraphics(mf);
                    skinRenderer.createSeriesIndexGraphics(mf);
                    skinRenderer.createSeriesDetailsGraphics(mf);
                    createPlaylist(UMCParams.getInstance().getMediaCenterLocation() + UMCConstants.fileSeparator + "playlists" + UMCConstants.fileSeparator + "video" + UMCConstants.fileSeparator + adjustedTitle, mf);
                } else {
                    log.info("BW" + identifier + ": Beginne mit Erzeugung der Frontend Bilder für den Titel " + mf.getTitle());
                    adjustedTitle = getAdjustedTitle(mf.getTitle());
                    skinRenderer.createMainIndexGraphics(mf);
                    skinRenderer.createMovieIndexGraphics(mf);
                    skinRenderer.createMovieDetailsGraphics(mf);
                    if (UMCParams.getInstance().getTrailer() != null && !UMCParams.getInstance().getTrailer().equals("")) {
                        createPlaylist(UMCParams.getInstance().getMediaCenterLocation() + UMCConstants.fileSeparator + "playlists" + UMCConstants.fileSeparator + "video" + UMCConstants.fileSeparator + adjustedTitle + ".jsp", mf);
                    }
                }
                MainFrame.getInstance().updateCurrent();
            } catch (Throwable exc) {
                log.error("BW" + identifier + ": Fehler in BackgroundWorker", exc);
            } finally {
                end = System.currentTimeMillis();
                UMCStatistics.getInstance().addJobProcessingTime(end - start);
                run();
            }
        } else {
            if (debug) log.debug("BackgroundWorker " + identifier + ": schliesse DB-Connection ");
            dao.closeConnection();
            log.info("BW" + identifier + ": kein Job mehr gefunden");
        }
    }

    /**
	 *
	 * @param mf
	 * @param imdbID
	 * @param onlineID eine ID einer Online-Filmdatenbank (OFDB, TheMovieDB, ...)
	 */
    private void getMovieInfosOnline(MovieFile mf, String imdbID, String onlineID) {
        log.info("Ermittle Movie-Infos online für " + mf.getTitle());
        if (UMCParams.getInstance().getLanguage().equals("EN")) {
            mf = MovieInfosOnlineEnglish.getMovieInfosOnline(mf, log);
        } else if (UMCParams.getInstance().getLanguage().equals("DE")) {
            mf = MovieInfosOnlineGerman.getMovieInfosOnline(mf, log);
        } else if (UMCParams.getInstance().getLanguage().equals("FR")) {
            mf = MovieInfosOnlineFrench.getMovieInfosOnline(mf, log);
        } else if (UMCParams.getInstance().getLanguage().equals("ES")) {
            mf = MovieInfosOnlineSpanish.getMovieInfosOnline(mf, log);
        } else if (UMCParams.getInstance().getLanguage().equals("NL")) {
            mf = MovieInfosOnlineDutch.getMovieInfosOnline(mf, log);
        }
    }

    private void getSeriesInfosOnline(MovieFile mf) {
        SeriesInfosOnline.getSeriesInfosOnline(mf, fullSeries, log, debug);
    }

    /**
	 * Liest Werte aus einer xml-Datei und weist diese dem {@link MovieFile} zu.
	 *
	 * @param mf Ein {@link MovieFile} Objekt
	 */
    private void getInfosFromXML(MovieFile mf) {
        if (mf.getXMLfile() == null) return;
        if (debug) log.debug("Ermittle Infos aus lokaler Film XML-Datei (" + mf.getXMLfile().documentProperties().getSourceName() + "):");
        try {
            Movie movie = mf.getXMLfile().getMovie();
            if (movie.getId() != null && !movie.getId().equals("")) {
                if (movie.getId().startsWith("tt")) {
                    if (debug) log.debug("nfo: setze IMDB ID: " + movie.getId());
                    mf.setIdIMDB(movie.getId());
                } else {
                    if (debug) log.debug("nfo: setze Online-Filmdatenbank ID: " + movie.getId());
                    mf.setID(movie.getId());
                }
            }
            if (movie.getTitle() != null && !movie.getTitle().equals("")) {
                if (debug) log.debug("nfo: setze Title: " + movie.getTitle());
                mf.setTitle(movie.getTitle());
            }
            if (movie.getRating() != null && !movie.getRating().equals("")) {
                if (Pattern.matches("\\d", movie.getRating())) {
                    if (debug) log.debug("nfo: setze Rating: " + movie.getRating());
                    mf.setRatingOFDB(Float.parseFloat(movie.getRating().replaceAll(",", ".")));
                } else {
                    if (debug) log.warn("nfo: Rating " + movie.getRating() + " konnte nicht gesetzt werden. Es handelt sich nicht um eine Zahl!");
                }
            }
            if (movie.getGenre() != null && !movie.getGenre().equals("")) {
                StringTokenizer st = new StringTokenizer(movie.getGenre(), ",");
                String genre = "";
                while (st.hasMoreTokens()) {
                    genre = st.nextToken();
                    if (debug) log.debug("nfo: setze Genre: " + genre);
                    mf.addGenre(genre);
                }
            }
            if (movie.getYear() != null && !movie.getYear().equals("")) {
                if (debug) log.debug("nfo: setze Jahr: " + movie.getYear());
                if (Pattern.matches("\\d\\d\\d\\d", movie.getYear())) mf.setYear(Integer.parseInt(movie.getYear()));
            }
            Cast[] actors = movie.getCastArray();
            String fullName = "";
            for (int i = 0; i < actors.length; i++) {
                Actor[] a = actors[i].getActorArray();
                for (int k = 0; k < a.length; k++) {
                    fullName = "";
                    if (a[k].getForename() != null && !a[k].getForename().equals("")) fullName += a[k].getForename();
                    if (a[k].getSurname() != null && !a[k].getSurname().equals("")) fullName += " " + a[k].getSurname();
                    if (fullName != null && !fullName.equals("")) {
                        if (debug) log.debug("nfo: setze Schauspieler: " + fullName);
                        com.umc.beans.persons.IPerson act = new com.umc.beans.persons.Actor();
                        act.setName(fullName);
                        mf.addPerson(act);
                    }
                }
            }
            if (movie.getPlot() != null && !movie.getPlot().equals("")) {
                if (debug) log.debug("nfo: setze Story: " + movie.getPlot());
                mf.setStory(movie.getPlot());
            }
            if (movie.getFsk() != null && !movie.getFsk().equals("")) {
                if (debug) log.debug("nfo: setze FSK: " + movie.getFsk());
                if (Pattern.matches("\\d", movie.getFsk())) mf.setFSK(Byte.parseByte(movie.getFsk()));
            }
            if (movie.getResolution() != null && !movie.getResolution().equals("")) {
                if (debug) log.debug("nfo: setze Resolution: " + movie.getResolution());
                mf.setResolution(movie.getResolution());
            }
            if (movie.getBanner() != null && !movie.getBanner().equals("")) {
                if (debug) log.debug("nfo: setze Banner: " + movie.getBanner());
                BannerImage banner = new BannerImage();
                banner.setAbsolutePath(movie.getBanner());
                mf.setBanner(banner);
            }
            if (movie.getCover() != null && !movie.getCover().equals("")) {
                if (debug) log.debug("nfo: setze Cover: " + movie.getCover());
                CoverImage cover = new CoverImage();
                cover.setAbsolutePath(movie.getCover());
                mf.setCover(cover);
            }
            if (movie.getPoster() != null && !movie.getPoster().equals("")) {
                if (debug) log.debug("nfo: setze Poster: " + movie.getPoster());
                PosterImage poster = new PosterImage();
                poster.setAbsolutePath(movie.getPoster());
                mf.setPoster(poster);
            }
            if (movie.getBackdrop() != null && !movie.getBackdrop().equals("")) {
                if (debug) log.debug("nfo: setze Backdrop: " + movie.getBackdrop());
                BackdropImage backdrop = new BackdropImage();
                backdrop.setAbsolutePath(movie.getBackdrop());
                mf.setBackdrop(backdrop);
            }
            Audio audio = movie.getAudio();
            if (audio != null) {
                Track[] tracks = audio.getTrackArray();
                for (int i = 0; i < tracks.length; i++) {
                    AudioTrack at = new AudioTrack();
                    at.setLanguage(tracks[i].getLanguage());
                    at.setCodec(tracks[i].getCodec());
                    mf.addAudioTrack(at);
                }
            }
        } catch (Throwable exc) {
            log.error("Allgemeiner Fehler beim Auslesen der Film XML-Datei für " + mf.getFilename(), exc);
        }
    }

    /**
	 * Liest Werte aus einer xml-Datei und weist diese dem {@link MovieFile} zu.
	 *
	 * @param mf Ein {@link MovieFile} Objekt
	 */
    private void getEpisodeInfosFromXML(MovieFile mf) {
        if (mf.getSeriesXMLfile() == null) {
            log.debug("Seriengruppe ist keine XML Datei zugeordnet");
            return;
        }
        if (debug) log.debug("Ermittle Episoden Infos aus lokaler Serien XML-Datei (" + mf.getSeriesXMLfile().documentProperties().getSourceName() + "):");
        try {
            de.umcProject.xmlbeans.SeriesDocument.Series series = mf.getSeriesXMLfile().getSeries();
            if (series.getIdThetvdb() != null && !series.getIdThetvdb().equals("")) {
                if (debug) log.debug("nfo: setze Online-Filmdatenbank ID: " + series.getIdThetvdb());
                mf.setID(series.getIdThetvdb());
            }
            if (series.getTitle() != null && !series.getTitle().equals("")) {
                if (debug) log.debug("nfo: setze Serien Titel: " + series.getTitle());
                mf.setTitle(series.getTitle());
            }
            if (series.getPlot() != null && !series.getPlot().equals("")) {
                if (debug) log.debug("nfo: setze Serien Plot: " + series.getPlot());
                mf.setStory(series.getPlot());
            }
            if (series.getRating() != null && !series.getRating().equals("")) {
                if (Pattern.matches("\\d", series.getRating())) {
                    if (debug) log.debug("nfo: setze Serien Rating: " + series.getRating());
                    mf.setRatingTHETVDB(Float.parseFloat(series.getRating().replaceAll(",", ".")));
                } else {
                    if (debug) log.warn("nfo: Rating " + series.getRating() + " konnte nicht gesetzt werden. Es handelt sich nicht um eine Zahl!");
                }
            }
            de.umcProject.xmlbeans.SeriesDocument.Series.Audio audio = series.getAudio();
            if (audio != null) {
                de.umcProject.xmlbeans.SeriesDocument.Series.Audio.Track[] tracks = audio.getTrackArray();
                for (int i = 0; i < tracks.length; i++) {
                    AudioTrack at = new AudioTrack();
                    at.setLanguage(tracks[i].getLanguage());
                    at.setCodec(tracks[i].getCodec());
                    mf.addAudioTrack(at);
                }
            }
            if (series.getGenre() != null && !series.getGenre().equals("")) {
                StringTokenizer st = new StringTokenizer(series.getGenre(), ",");
                String genre = "";
                while (st.hasMoreTokens()) {
                    genre = st.nextToken();
                    if (debug) log.debug("nfo: setze Serien Genre: " + genre);
                    mf.addGenre(genre);
                }
            }
            if (series.getYear() != null && !series.getYear().equals("")) {
                if (debug) log.debug("nfo: setze Jahr: " + series.getYear());
                if (Pattern.matches("\\d\\d\\d\\d", series.getYear())) mf.setYear(Integer.parseInt(series.getYear()));
            }
            de.umcProject.xmlbeans.SeriesDocument.Series.Cast[] actors = series.getCastArray();
            String fullName = "";
            for (int i = 0; i < actors.length; i++) {
                de.umcProject.xmlbeans.SeriesDocument.Series.Cast.Actor[] a = actors[i].getActorArray();
                for (int k = 0; k < a.length; k++) {
                    fullName = "";
                    if (a[k].getForename() != null && !a[k].getForename().equals("")) fullName += a[k].getForename();
                    if (a[k].getSurname() != null && !a[k].getSurname().equals("")) fullName += " " + a[k].getSurname();
                    if (debug) log.debug("nfo: setze Serien Schauspieler: " + fullName);
                    com.umc.beans.persons.IPerson act = new com.umc.beans.persons.Actor();
                    act.setName(fullName);
                    mf.addPerson(act);
                }
            }
            if (series.getResolution() != null && !series.getResolution().equals("")) {
                if (debug) log.debug("nfo: setze Serien Resolution: " + series.getResolution());
                mf.setResolution(series.getResolution());
            }
            if (series.getBanner() != null && !series.getBanner().equals("")) {
                if (debug) log.debug("nfo: setze Serien Banner: " + series.getBanner());
                BannerImage banner = new BannerImage();
                banner.setAbsolutePath(series.getBanner());
                mf.setBanner(banner);
            }
            if (series.getCover() != null && !series.getCover().equals("")) {
                if (debug) log.debug("nfo: setze Serien Cover: " + series.getCover());
                CoverImage cover = new CoverImage();
                cover.setAbsolutePath(series.getCover());
                mf.setCover(cover);
            }
            if (series.getBackdrop() != null && !series.getBackdrop().equals("")) {
                if (debug) log.debug("nfo: setze Serien Backdrop: " + series.getBackdrop());
                BackdropImage backdrop = new BackdropImage();
                backdrop.setAbsolutePath(series.getBackdrop());
                mf.setBackdrop(backdrop);
            }
        } catch (Throwable exc) {
            log.error("Allgemeiner Fehler beim Auslesen der Serien XML-Datei für Serie" + mf.getGroupname(), exc);
        }
    }

    /**
	 * Diese Methode liest mit Hilfe von MPlayer Medieinfos wie Auflösung,Tonspur,Codec usw. aus
	 * und weist die Informationen dem übergebenen {@link MovieFile} zu.
	 *
	 * HINWEIS:
	 * Filme die HD Auflösung haben bekommen in dieser Methode automatisch das System-Genre HD zugewiesen.
	 *
	 * @param mf
	 */
    private void getMediaInfos(MovieFile mf) {
        log.info("Ermittle Medieninfos aus Datei...");
        if (new File(System.getProperty("user.dir") + "/tools/").exists()) {
            Process p = null;
            try {
                String os = System.getProperty("os.name");
                String mediaPath = mf.getScandir() + System.getProperty("file.separator") + mf.getPath() + System.getProperty("file.separator") + mf.getFilename() + mf.getFiletype();
                if (mf.isVideoTS()) mediaPath = mf.getScandir() + mf.getPathFromVIDEO_TS() + UMCConstants.fileSeparator + mf.getPath();
                log.info("Datei: " + mediaPath + " für " + mf.getTitle() + "/" + mf.getGroupname());
                if (mediaPath == null || mediaPath.equals("")) {
                    log.warn("Kein korrekter Pfad zu einer Filmdatei -> " + mediaPath + ". Das auslesen der Infos mit MediaInfo kann nicht ausgeführt werden und wird an dieser Stelle abgebrochen.");
                    return;
                }
                String[] playWin = { System.getProperty("user.dir") + "/tools/win/MediaInfo.exe", "--Inform=file://mediainfo_template", mediaPath };
                String[] playMac = { "./mediainfo", "--Inform=file://mediainfo_template", mediaPath };
                String[] playLinux = { UMCParams.getInstance().getMediainfo(), "--Inform=file://mediainfo_template", mediaPath };
                ProcessBuilder pc = null;
                if (os.toUpperCase().indexOf("WIN") != -1) {
                    pc = new ProcessBuilder(playWin);
                    pc.directory(new File(System.getProperty("user.dir") + "/tools/win/"));
                } else if (os.toUpperCase().indexOf("MAC OS") != -1) {
                    pc = new ProcessBuilder(playMac);
                    pc.directory(new File(System.getProperty("user.dir") + "/tools/mac/"));
                } else {
                    pc = new ProcessBuilder(playLinux);
                    pc.directory(new File(System.getProperty("user.dir") + "/tools/linux/"));
                }
                p = pc.start();
                InputStream is = p.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                Properties props = new Properties();
                props.load(isr);
                if (debug) {
                    log.debug(props.getProperty("fileSize", "").toString());
                    log.debug(props.getProperty("videoCodec", "").toString());
                    log.debug(props.getProperty("videoBitrate", "").toString());
                    log.debug(props.getProperty("videoWidth", "").toString());
                    log.debug(props.getProperty("videoHeight", "").toString());
                    log.debug(props.getProperty("videoFrameRate", "").toString());
                    log.debug(props.getProperty("durationHHmm", "").toString());
                    log.debug(props.getProperty("videoAR", "").toString());
                }
                if (mf.getFilesize() == null || mf.getFilesize().equals("")) {
                    mf.setFilesize(props.getProperty("fileSize", "").toString().replaceAll("GiB", "GB").replaceAll("MiB", "MB"));
                }
                if (mf.getCodec() == null || mf.getCodec().equals("")) mf.setCodec(props.getProperty("videoCodec", "").toString());
                if (mf.getBitrate() == null || mf.getBitrate().equals("")) mf.setBitrate(props.getProperty("videoBitrate", "").toString());
                if (mf.getWidth() == null || mf.getWidth().equals("")) {
                    mf.setWidth(props.getProperty("videoWidth", ""));
                }
                if (mf.getWidth() != null && !mf.getWidth().equals("") && Integer.parseInt(mf.getWidth().replaceAll("[a-zA-Z ]", "")) >= 1280) {
                    mf.addGenre("HD");
                    mf.setHD(true);
                } else {
                    mf.setSD(true);
                }
                if (mf.getHeight() == null || mf.getHeight().equals("")) {
                    mf.setHeight(props.getProperty("videoHeight", "").toString());
                }
                if (mf.getFramerate() == null || mf.getFramerate().equals("")) mf.setFramerate(props.getProperty("videoFrameRate", "").toString());
                if (mf.getLength() == null || mf.getLength().equals("")) mf.setLength(props.getProperty("durationHHmm", "").toString());
                if (mf.getAspectRatio() == null || mf.getAspectRatio().equals("")) mf.setAspectRatio(props.getProperty("videoAR", "").toString());
                Collection<AudioTrack> audioTracks = mf.getAudioTracks();
                int i = 0;
                for (AudioTrack audioTrack : audioTracks) {
                    if ((audioTrack.getCodec() == null || audioTrack.getCodec().equals("")) && props.getProperty("audioFormat" + i) != null) audioTrack.setCodec(props.getProperty("audioFormat" + i, "").toString());
                    if ((audioTrack.getLanguage() == null || audioTrack.getLanguage().equals("")) && props.getProperty("audioLng" + i) != null) audioTrack.setLanguage(props.getProperty("audioLng" + i, "").toString());
                    if ((audioTrack.getChannels() == null || audioTrack.getChannels().equals("")) && props.getProperty("audioChannels" + i) != null) audioTrack.setChannels(props.getProperty("audioChannels" + i, "").toString());
                    if ((audioTrack.getBitrate() == null || audioTrack.getBitrate().equals("")) && props.getProperty("audioBitrate" + i) != null) audioTrack.setBitrate(props.getProperty("audioBitrate" + i, "").toString());
                    log.debug("Audio: " + audioTrack.getCodec() + " | " + audioTrack.getLanguage() + " | " + audioTrack.getChannels() + " | " + audioTrack.getBitrate());
                    i++;
                }
                mf.getAudioTracks().clear();
                mf.getAudioTracks().addAll(audioTracks);
                p.getErrorStream().close();
                p.getInputStream().close();
                p.getOutputStream().close();
                int returnCode = p.waitFor();
                if (returnCode == 0) {
                } else {
                    log.error("Fehler mplayer Returncode = " + returnCode + " => " + pc.command());
                    mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getTitle() + ": Returncode -> " + returnCode));
                }
            } catch (NullPointerException exc) {
                log.error("NullPointer Fehler", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getTitle() + ": NullPointer Exception"));
            } catch (IndexOutOfBoundsException exc) {
                log.error("IndexOutOfBounds Fehler", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getTitle() + ": IndexOutOfBounds Exception"));
            } catch (SecurityException exc) {
                log.error("Security Fehler", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getTitle() + ": Security Exception"));
            } catch (IOException exc) {
                log.error("IO Fehler", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getTitle() + ": IO Exception"));
            } catch (Exception exc) {
                log.error("Allgemeiner Fehler", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getTitle() + ": Generic Exception"));
            }
        } else {
            log.error("MPlayer Verzeichniss " + System.getProperty("user.dir") + "/tools/" + " wurde nicht gefunden.");
            mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, "Verzeichniss " + System.getProperty("user.dir") + "/tools/" + " wurde nicht gefunden."));
        }
    }

    private void assignImages2(MovieFile mf) {
        String searchFor = null;
        String movieTitle = mf.getTitle();
        if (mf.isVideoTS()) {
            movieTitle = mf.getFilename();
            searchFor = mf.getFilename();
            if (debug) log.debug("suche nach VIDEO_TS Bild  " + movieTitle);
        } else if (mf.isMovieGroup()) {
            movieTitle = mf.getGroupname();
            searchFor = mf.getGroupname();
            log.debug("suche nach Cover für Universalgruppe " + movieTitle);
        } else if (mf.isSeriesGroup()) {
            movieTitle = mf.getGroupname();
            searchFor = mf.getGroupname();
            log.debug("suche nach Cover für Serie " + movieTitle);
        } else {
            movieTitle = mf.getTitle();
            searchFor = mf.getFilename();
            log.debug("suche nach Filmcover für Film " + movieTitle);
        }
        Map<String, ImageFile> images = Publisher.getInstance().getMovieImagefiles(searchFor);
        if (images != null) {
            if (mf.getCover() == null && images.containsKey(Publisher.KEY_COVER)) {
                log.info("Lokales Cover wurde gefunden und zugewiesen");
                mf.setCover((CoverImage) images.get(Publisher.KEY_COVER));
            }
            if (mf.getBanner() == null && images.containsKey(Publisher.KEY_BANNER)) {
                log.info("Lokales Banner wurde gefunden und zugewiesen");
                mf.setBanner((BannerImage) images.get(Publisher.KEY_BANNER));
            }
            if (mf.getBackdrop() == null && images.containsKey(Publisher.KEY_BACKDROP)) {
                log.info("Lokales Backdrops wurde gefunden und zugewiesen");
                mf.setBackdrop((BackdropImage) images.get(Publisher.KEY_BACKDROP));
            }
        } else {
            log.warn("Es wurden keine lokalen Frontend Bilder für " + movieTitle + " gefunden");
        }
    }

    private void getSeriesFanart(MovieFile mf) {
        if (mf.getSeriesXMLfile() != null) {
            de.umcProject.xmlbeans.SeriesDocument.Series seriesXML = mf.getSeriesXMLfile().getSeries();
            if (seriesXML.getBanner() != null && !seriesXML.getBanner().equals("")) {
                if (debug) log.debug("setze Banner: " + seriesXML.getBanner() + " aus XML Datei");
                BannerImage banner = new BannerImage();
                banner.setAbsolutePath(seriesXML.getBanner());
                mf.setBanner(banner);
            }
            if (seriesXML.getBackdrop() != null && !seriesXML.getBackdrop().equals("")) {
                if (debug) log.debug("setze Backdrop: " + seriesXML.getBackdrop() + " aus XML Datei");
                BackdropImage backdrop = new BackdropImage();
                backdrop.setAbsolutePath(seriesXML.getBackdrop());
                mf.setBackdrop(backdrop);
            }
        }
        String adjustedTitle = getAdjustedTitle(mf.getGroupname());
        if (fullSeries != null && (mf.getBanner() == null || mf.getBackdrop() == null)) {
            BufferedImage img = null;
            if (mf.getBanner() == null) {
                log.info("[Banner]");
                img = imgUtils.loadImageFromURL(fullSeries.getMirrors().getRandomMirror(MirrorType.BANNER).getMirrorpath() + "/banners/" + (fullSeries.getSerie().getBanner()));
                if (img != null) {
                    imgUtils.saveImage(img, UMCConstants.DEFAULT_SERIES_BANNER_DIR + UMCConstants.fileSeparator + adjustedTitle + UMCParams.getInstance().getBannerFilenameExtension() + ".jpg");
                    img = null;
                    BannerImage seriesBanner = new BannerImage();
                    seriesBanner.setAbsolutePath(UMCConstants.DEFAULT_SERIES_BANNER_DIR + UMCConstants.fileSeparator + adjustedTitle + UMCParams.getInstance().getBannerFilenameExtension() + ".jpg");
                    seriesBanner.setFinished(false);
                    mf.setBanner(seriesBanner);
                }
                img = null;
            }
            if (mf.getBackdrop() == null) {
                img = imgUtils.loadImageFromURL(fullSeries.getMirrors().getRandomMirror(MirrorType.BANNER).getMirrorpath() + "/banners/" + (fullSeries.getSerie().getFanart()));
                if (img != null) {
                    imgUtils.saveImage(img, UMCConstants.DEFAULT_SERIES_BACKDROPS_DIR + UMCConstants.fileSeparator + adjustedTitle + UMCParams.getInstance().getBackdropFilenameExtension() + ".jpg");
                    img = null;
                    BackdropImage seriesBackdrop = new BackdropImage();
                    seriesBackdrop.setAbsolutePath(UMCConstants.DEFAULT_SERIES_BACKDROPS_DIR + UMCConstants.fileSeparator + adjustedTitle + UMCParams.getInstance().getBackdropFilenameExtension() + ".jpg");
                    seriesBackdrop.setFinished(false);
                    mf.setBackdrop(seriesBackdrop);
                }
                img = null;
            }
        }
    }

    /**
	 * Ermittelt Cover,Poster,Backdrop und Schauspieler-Bilder für ein übergebenes MovieFile Objekt.
	 *
	 * @param mf
	 */
    private void getMovieFanart(MovieFile mf) {
        if (mf.getCover() == null || mf.getBackdrop() == null) {
            log.info("Ermittle Movie Fanart online mit Fanart-Plugin...");
            pluginFanart.request(mf.getTitle(), mf.getTitleAlternative(), mf.getIdIMDB());
        }
        if (mf.getCover() == null) {
            log.info("[Cover]");
            Collection<String> urls = pluginFanart.getCover();
            if (urls == null || urls.size() == 0) {
                log.info("Fanart-Plugin hat keine Urls geliefert, es wird über Yahoo gesucht...");
                urls = pluginImages.getPoster(mf.getTitle(), 150, 300);
            }
            if (urls != null && urls.size() > 0) {
                if (debug) log.debug("URLs für Moviecover-Abfrage gefunden");
                BufferedImage img = null;
                for (String url : urls) {
                    img = imgUtils.loadImageFromURL(url);
                    if (img != null) break;
                }
                if (img != null) {
                    imgUtils.saveImage(img, System.getProperty("user.dir") + "/resources/Cover/Movies/" + mf.getFilename() + UMCParams.getInstance().getCoverFilenameExtension() + ".jpg");
                    img = null;
                    movieCover = new CoverImage();
                    movieCover.setAbsolutePath(System.getProperty("user.dir") + "/resources/Cover/Movies" + UMCConstants.fileSeparator + mf.getFilename() + UMCParams.getInstance().getCoverFilenameExtension() + ".jpg");
                    movieCover.setFinished(false);
                    mf.setCover(movieCover);
                } else {
                    log.error("Cover konnte von keiner gefundenen URL geladen werden. Ziele vermutlich nicht erreichbar.");
                    mf.addWarning(new Warning(UMCConstants.STATUS_COVER_NOT_FOUND, "Cover für " + mf.getTitle() + " konnte von keiner einzigen gefundenen URL geladen werden"));
                }
            } else {
                if (debug) log.debug("keine URL für Moviecover-Abfrage gefunden");
                mf.addWarning(new Warning(UMCConstants.STATUS_COVER_NOT_FOUND, "Plugin hat keine URL für Cover geliefert [" + mf.getTitle() + "]"));
            }
        }
        if (mf.getCover() == null) {
            if (debug) log.debug("lade alternatives Cover von Online-Filmdatenbank: " + mf.getOnlineCover());
            BufferedImage bi = imgUtils.loadImageFromURL(mf.getOnlineCover());
            if (bi != null) {
                imgUtils.saveJPEGImage(bi, System.getProperty("user.dir") + "/resources/Cover/Movies" + UMCConstants.fileSeparator + mf.getFilename() + UMCParams.getInstance().getCoverFilenameExtension());
                if (debug) log.debug("alternatives Cover von Online-Filmdatenbank wurde runtergeladen");
                movieCover = new CoverImage();
                movieCover.setAbsolutePath(System.getProperty("user.dir") + "/resources/Cover/Movies" + UMCConstants.fileSeparator + mf.getFilename() + UMCParams.getInstance().getCoverFilenameExtension() + ".jpg");
                movieCover.setFinished(false);
                mf.setCover(movieCover);
            } else {
                if (debug) log.debug("Cover " + mf.getOnlineCover() + " konnte nicht von Online-Filmdatenbank geladen werden");
                UMCStatistics.getInstance().addMovieNoCoverFoundOnline();
            }
        }
        if (mf.getBackdrop() == null) {
            log.info("[Backdrop]");
            Collection<String> urls = pluginFanart.getBackdrop();
            if (urls == null || urls.size() == 0) urls = pluginImages.getImage(mf.getTitle(), 800, 1600);
            if (urls != null && urls.size() > 0) {
                if (debug) log.debug("URLs für Moviebackdrop-Abfrage gefunden");
                BufferedImage img = null;
                for (String url : urls) {
                    img = imgUtils.loadImageFromURL(url);
                    if (img != null) break;
                }
                if (img != null) {
                    imgUtils.saveImage(img, System.getProperty("user.dir") + "/resources/Backdrops/Movies/" + mf.getFilename() + UMCParams.getInstance().getBackdropFilenameExtension() + ".jpg");
                    img = null;
                    movieBackdrop = new BackdropImage();
                    movieBackdrop.setAbsolutePath(System.getProperty("user.dir") + "/resources/Backdrops/Movies" + UMCConstants.fileSeparator + mf.getFilename() + UMCParams.getInstance().getBackdropFilenameExtension() + ".jpg");
                    movieBackdrop.setFinished(false);
                    mf.setBackdrop(movieBackdrop);
                } else {
                    UMCStatistics.getInstance().addMovieNoBackdropFoundOnline();
                    log.error("Backdrop konnte von keiner gefundenen URL geladen werden. Ziele vermutlich nicht erreichbar");
                    mf.addWarning(new Warning(UMCConstants.STATUS_BACKDROP_NOT_FOUND, "Backdrop für " + mf.getTitle() + " konnte von keiner einzigen gefundenen URL geladen werden"));
                }
            } else {
                UMCStatistics.getInstance().addMovieNoBackdropFoundOnline();
                if (debug) log.debug("keine URL's für Moviebackdrop-Abfrage gefunden");
                mf.addWarning(new Warning(UMCConstants.STATUS_BACKDROP_NOT_FOUND, "Plugin hat keine URL's für Backdrop geliefert [" + mf.getTitle() + "]"));
            }
        }
    }

    /**
	 * Diese Methode entfernt aus einem ermittelten Filmnamen alle Sonderzeichen.
	 * Dies is nötig um z.B. ein Cover oder Poster mit Hilfe des Filmttitel auf der Festplatte
	 * speichern zu können.
	 *
	 * @param aTile Ein Titel der bereinigt werden soll
	 */
    private String getAdjustedTitle(String aTitle) {
        if (aTitle != null && !aTitle.equals("")) {
            for (int h = 0; h < UMCConstants.charsNotAllowedInFilename.length; h++) {
                aTitle = aTitle.replaceAll(UMCConstants.charsNotAllowedInFilename[h], "");
            }
        }
        return aTitle;
    }

    /**
	 * Erzeugt eine Playlist für Mutlipart-Gruppen (MG's) und für FM's (nur wenn ein Trailer in der umc-config.xml angegeben wurde) damit diese wie ein Film
	 * auf dem PCH behandelt werden.
	 *
	 * ACHTUNG:
	 *
	 * - Playlists werden für MG's nur erstellt wenn kein einziges Part der MG einen VIDEO_TS bzw. ISO Film darstellt
	 * - Playlists werden nicht für UG's erstellt.
	 *
	 * @param playlistPath Absoluter Pfad unter dem die Playlists gepseichert werden soll
	 * @param movieFile Ein MovieFile Objekt welches eine MG drastellt
	 */
    private void createPlaylist(String playlistPath, MovieFile movieFile) {
        try {
            File playlist = new File(playlistPath);
            if (playlist.exists()) {
                if (playlist.delete()) log.info("bestehende Playlist " + playlist.getAbsolutePath() + " wurde gelöscht"); else log.warn("bestehende Playlist " + playlist.getAbsolutePath() + " konnte nicht gelöscht werden");
            }
            if (movieFile.isSeriesGroup()) {
                File seasonPlaylist = null;
                FileWriter fw = null;
                int seasonIndex = 1;
                int episodeIndex = 1;
                for (MovieFile season : movieFile.getMultiparts()) {
                    seasonPlaylist = new File(playlistPath + "[" + seasonIndex + "][SG].jsp");
                    fw = new FileWriter(seasonPlaylist);
                    episodeIndex = 1;
                    for (MovieFile episode : season.getMultiparts()) {
                        fw.write("file" + episodeIndex + "|0|0|" + episode.getPCHPath(false) + "/" + episode.getPath() + "/" + episode.getFilename() + episode.getFiletype() + "|\n");
                        episodeIndex++;
                    }
                    fw.close();
                    seasonIndex++;
                }
            } else {
                if (playlist.createNewFile()) {
                    log.info("erzeuge Playlist " + playlist.getAbsolutePath());
                    FileWriter fw = new FileWriter(playlist);
                    int counter = 1;
                    boolean createPlaylist = true;
                    for (MovieFile part : movieFile.getMultiparts()) {
                        if (part.isVideoTS() || part.getFiletype().toUpperCase().equals(".ISO")) {
                            createPlaylist = false;
                            break;
                        }
                    }
                    if (createPlaylist) {
                        if (movieFile.isMultipartGroup()) {
                            Collection<MovieFile> list = new ArrayList<MovieFile>();
                            list.addAll(movieFile.getMultiparts());
                            while (list.size() > 0) {
                                MovieFile mp = Collections.min(list, new MovieFileComparator());
                                fw.write("file" + counter + "|0|0|" + mp.getPCHPath(false) + "/" + mp.getPath() + "/" + mp.getFilename() + mp.getFiletype() + "|\n");
                                counter++;
                                list.remove(mp);
                            }
                        } else {
                            fw.write("file1|0|0|" + UMCParams.getInstance().getTrailer() + "|\n");
                            fw.write("file2|0|0|" + movieFile.getPCHPath(false) + "/" + movieFile.getPath() + "/" + movieFile.getFilename() + movieFile.getFiletype() + "|\n");
                        }
                    } else {
                        UMCStatistics.getInstance().addPlaylistNotCreated();
                    }
                    fw.close();
                } else {
                    log.error("Playlist " + playlist.getAbsolutePath() + " konnte nicht angelegt werden!");
                }
            }
        } catch (IOException exc) {
            log.error("Playlist " + playlistPath + " konnte auf Grund eines IO Fehlers nicht erstellt werden", exc);
        }
    }

    /**
		 * Mit Hilfe dieser Methode kann ein Bild,unter Berücksichtigung des Verhültnisses, skaliert werden.
		 *
		 * @param path Der vollstündige Pfad zum Bild
		 * @param maxWidth die maximal gewünschte Breite des Bildes
		 * @param maxHeight die maximal gewünschte Hühe des Bildes
		 * @param hints Der Algorithmus der für das skalieren des Bildes benutzt werden soll
		 * @param fitType Gibt an ob die Breite(maxWidth)=2,die Höhe(maxHeight)=3 bzw. beides=1 berücksichtigt werden soll
		 * @return Das erzeugte Thumbnail
		 *
		 * @see RenderingHints
		 */
    public BufferedImage scaleToFit(String path, int maxWidth, int maxHeight, RenderingHints hints, int fitType) {
        BufferedImage img = imgUtils.loadImage(path);
        BufferedImage image = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        image.createGraphics().drawImage(img, 0, 0, null);
        BufferedImage thumbnail = null;
        if (image != null) {
            AffineTransform tx = new AffineTransform();
            double scale = 0;
            switch(fitType) {
                case 1:
                    scale = scaleToFitBoth(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
                case 2:
                    scale = scaleToFitHorizontal(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
                case 3:
                    scale = scaleToFitVertical(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
                default:
                    scale = scaleToFitBoth(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
            }
            tx.scale(scale, scale);
            double d1 = (double) image.getWidth() * scale;
            double d2 = (double) image.getHeight() * scale;
            thumbnail = new BufferedImage(((int) d1) < 1 ? 1 : (int) d1, ((int) d2) < 1 ? 1 : (int) d2, image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : image.getType());
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHints(hints);
            g2d.drawImage(image, tx, null);
            g2d.dispose();
        }
        return thumbnail;
    }

    /**
		 * Mit Hilfe dieser Methode kann ein Bild,unter Berücksichtigung des Verhültnisses, skaliert werden.
		 *
		 * @param path Der vollstündige Pfad zum Bild
		 * @param maxWidth die maximal gewünschte Breite des Bildes
		 * @param maxHeight die maximal gewünschte Hühe des Bildes
		 * @param hints Der Algorithmus der für das skalieren des Bildes benutzt werden soll
		 * @param fitType Gibt an ob die Breite(maxWidth)=2,die Höhe(maxHeight)=3 bzw. beides=1 berücksichtigt werden soll
		 * @param argb set true to use ARGB BuffereImage's
		 * @return Das erzeugte Thumbnail
		 *
		 * @see RenderingHints
		 */
    public BufferedImage scaleToFit(BufferedImage aImg, int maxWidth, int maxHeight, RenderingHints hints, int fitType, boolean argb) {
        BufferedImage img = aImg;
        BufferedImage image = null;
        if (argb) image = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB); else image = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_RGB);
        image.createGraphics().drawImage(img, 0, 0, null);
        BufferedImage thumbnail = null;
        if (image != null) {
            AffineTransform tx = new AffineTransform();
            double scale = 0;
            switch(fitType) {
                case 1:
                    scale = scaleToFitBoth(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
                case 2:
                    scale = scaleToFitHorizontal(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
                case 3:
                    scale = scaleToFitVertical(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
                default:
                    scale = scaleToFitBoth(image.getWidth(), image.getHeight(), maxWidth, maxHeight);
                    break;
            }
            tx.scale(scale, scale);
            double d1 = (double) image.getWidth() * scale;
            double d2 = (double) image.getHeight() * scale;
            thumbnail = new BufferedImage(((int) d1) < 1 ? 1 : (int) d1, ((int) d2) < 1 ? 1 : (int) d2, image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : image.getType());
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHints(hints);
            g2d.drawImage(image, tx, null);
            g2d.dispose();
        }
        return thumbnail;
    }

    /**
		 * Unter Berücksichtigung der Breite und der Hühe.
		 *
		 * @param w1 Breite des Ausgangs-Bildes
		 * @param h1 Hühe des Ausgangs-Bildes
		 * @param w2 Breite des gewünschten Thumbnails
		 * @param h2 Hühe des gewünschten Thumbnails
		 * @return
		 */
    private double scaleToFitBoth(double w1, double h1, double w2, double h2) {
        double scale = 1.0D;
        if (w1 > h1) {
            if (w1 > w2) scale = w2 / w1;
            h1 *= scale;
            if (h1 > h2) scale *= h2 / h1;
        } else {
            if (h1 > h2) scale = h2 / h1;
            w1 *= scale;
            if (w1 > w2) scale *= w2 / w1;
        }
        return scale;
    }

    /**
		 * Unter Berücksichtigung der Breite.
		 *
		 * @param w1 Breite des Ausgangs-Bildes
		 * @param h1 Hühe des Ausgangs-Bildes
		 * @param w2 Breite des gewünschten Thumbnails
		 * @param h2 Hühe des gewünschten Thumbnails
		 * @return
		 */
    private double scaleToFitHorizontal(double w1, double h1, double w2, double h2) {
        double scale = 1.0D;
        if (w1 > h1) {
            if (w1 > w2) scale = w2 / w1;
            h1 *= scale;
        } else {
            w1 *= scale;
            if (w1 > w2) scale *= w2 / w1;
        }
        return scale;
    }

    /**
		 * Unter Berücksichtigung der Hühe.
		 *
		 * @param w1 Breite des Ausgangs-Bildes
		 * @param h1 Hühe des Ausgangs-Bildes
		 * @param w2 Breite des gewünschten Thumbnails
		 * @param h2 Hühe des gewünschten Thumbnails
		 * @return
		 */
    private double scaleToFitVertical(double w1, double h1, double w2, double h2) {
        double scale = 1.0D;
        if (w1 > h1) {
            h1 *= scale;
            if (h1 > h2) scale *= h2 / h1;
        } else {
            if (h1 > h2) scale = h2 / h1;
            w1 *= scale;
        }
        return scale;
    }

    private void setUGSequence(MovieFile mf) {
        if (mf.isMovieGroup()) {
            if (debug) log.debug("Erzeuge Sequenz für UG " + mf.getGroupname());
            Collection<MovieFile> list = new ArrayList<MovieFile>();
            list.addAll(mf.getMultiparts());
            int sequenceStart = list.size();
            while (list.size() > 0) {
                MovieFile mp = Collections.max(list, new MovieFileComparator());
                mp.setSequencePosition(sequenceStart);
                sequenceStart--;
                if (debug) log.debug("Sequenz für " + mp.getFilename() + " " + mp.getSequencePosition());
                list.remove(mp);
            }
        }
    }

    private void setUGGenres(MovieFile mf) {
        if (mf.isMovieGroup()) {
            for (MovieFile part : mf.getMultiparts()) {
                for (String genre : part.getGenres()) {
                    mf.addGenre(genre);
                }
            }
        }
    }
}
