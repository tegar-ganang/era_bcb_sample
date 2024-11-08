package com.umc.collector;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.ID3Tag;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.MediaFile;
import org.blinkenlights.jid3.v1.ID3V1_0Tag;
import org.blinkenlights.jid3.v2.ID3V2_3_0Tag;
import com.umc.ConfigController;
import com.umc.UMCStatistics;
import com.umc.beans.AudioTrack;
import com.umc.beans.BackdropImage;
import com.umc.beans.BannerImage;
import com.umc.beans.CoverImage;
import com.umc.beans.ImageFile;
import com.umc.beans.PosterImage;
import com.umc.beans.Warning;
import com.umc.beans.media.Episode;
import com.umc.beans.media.Movie;
import com.umc.beans.media.MovieGroup;
import com.umc.beans.media.Music;
import com.umc.beans.media.MusicAlbum;
import com.umc.beans.media.Photo;
import com.umc.beans.media.PhotoAlbum;
import com.umc.beans.media.Season;
import com.umc.beans.media.SeriesGroup;
import com.umc.beans.media.Trailer;
import com.umc.dao.DataAccessFactory;
import com.umc.dao.UMCDataAccessInterface;
import com.umc.filescanner.FilescannerController;
import com.umc.gui.GuiController;
import com.umc.helper.UMCConstants;
import com.umc.helper.UMCImageUtils;
import com.umc.helper.UMCUtils;
import com.umc.plugins.PluginController;
import com.umc.plugins.moviedb.AbstractFanartPlugin;
import com.umc.plugins.moviedb.AbstractImagePlugin;
import com.umc.plugins.moviedb.AbstractSeriesDBPlugin;
import com.umc.skins.SkinRenderer;
import de.umcProject.xmlbeans.MovieDocument;
import de.umcProject.xmlbeans.SeriesDocument;
import de.umcProject.xmlbeans.MovieDocument.Movie.Audio;
import de.umcProject.xmlbeans.MovieDocument.Movie.Cast;
import de.umcProject.xmlbeans.MovieDocument.Movie.Language;
import de.umcProject.xmlbeans.MovieDocument.Movie.Audio.Track;
import de.umcProject.xmlbeans.MovieDocument.Movie.Cast.Actor;
import de.umcProject.xmlbeans.MovieDocument.Movie.Language.Plugin;
import de.umcProject.xmlbeans.yamj.MovieDocument.Movie.Id;

/**
 * Eine Instanz dieser/s Klasse/Threads dient als sogenannter BackgroundWorker
 * für den Filescanner.
 *
 * @author DonGyros
 *
 * @version 0.1 10.08.2008
 */
public class BackgroundWorker extends Thread {

    /**Logger*/
    private Logger log = null;

    public static Logger logTest = Logger.getLogger("com.umc.test");

    private UMCImageUtils imgUtils = null;

    private SkinRenderer skinRenderer = null;

    /**ID dieses BackgroundWorkers*/
    public int identifier = -1;

    private AbstractSeriesDBPlugin pluginSeries = null;

    private AbstractFanartPlugin pluginFanart = null;

    private AbstractImagePlugin pluginImages = null;

    private UMCDataAccessInterface dao = null;

    private CoverImage movieCover = null;

    private BannerImage movieBanner = null;

    private BackdropImage movieBackdrop = null;

    /**
	 * initialisiert das logging
	 */
    private void startLogging() {
        if (log == null) {
            try {
                ConsoleAppender consoleAppendr = new ConsoleAppender();
                consoleAppendr.setName("BACKGROUNDWORKER" + identifier);
                consoleAppendr.setLayout(new PatternLayout("%r %-5p - %C.%M - %m%n"));
                consoleAppendr.activateOptions();
                RollingFileAppender appndr = new RollingFileAppender();
                appndr.setName("BACKGROUNDWORKER" + identifier);
                appndr.setLayout(new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %r [%t] %-5p %c - %C.%M - %m%n"));
                appndr.setFile("logs/BackgroundWorker_" + identifier + ".log");
                appndr.setAppend(false);
                appndr.activateOptions();
                log = Logger.getLogger("BW" + identifier);
                if (Publisher.getInstance().getParamBackgroundWorkerLoglevel() != null) {
                    log.setLevel(Level.DEBUG);
                } else {
                    log.setLevel(Level.INFO);
                }
                log.setAdditivity(false);
                if (Logger.getLogger("BW" + identifier).getAppender("BACKGROUNDWORKER" + identifier) == null) {
                    log.addAppender(appndr);
                }
            } catch (Throwable ex) {
                System.out.println("BackgroundWorker " + BackgroundWorker.class.getName() + " could not write to any logfile : " + ex);
            }
        }
    }

    /**
	 * Konstruktor
	 *
	 * @param identifier Eine Kennung unter der eine Instanz dieser Klasse zugeordnet wird
	 */
    public BackgroundWorker(int identifier) {
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
        if (UMCConstants.debug) log.debug("BackgroundWorker " + identifier + " has been created");
        dao = DataAccessFactory.getUMCDataSourceAccessor(DataAccessFactory.DB_TYPE_SQLITE, Publisher.getInstance().getParamDBDriverconnect() + Publisher.getInstance().getParamDBName(), Publisher.getInstance().getParamDBDriver(), Publisher.getInstance().getParamDBUser(), Publisher.getInstance().getParamDBPwd());
        if (dao == null) {
            log.fatal("DAO could not be loaded");
            return false;
        }
        pluginImages = UMCUtils.loadImagePlugin(Publisher.getInstance().getParamMovieCoverPlugin());
        pluginFanart = UMCUtils.loadFanartPlugin("com.umc.plugins.Fanart");
        pluginSeries = UMCUtils.loadSeriesDBPlugin("com.umc.plugins.TheTVDB");
        imgUtils = new UMCImageUtils(log);
        skinRenderer = new SkinRenderer(log);
        return true;
    }

    /**
	 * Zentrale Abarbeitungs-Methode dieses Threads
	 */
    public void run() {
        while (!isInterrupted()) {
            log.info("\n\n\n################################################  Next run " + GregorianCalendar.getInstance().getTime() + " ################################################");
            long start = 0;
            long end = 0;
            Object job = Publisher.getInstance().getBackgroundWorkerMovieJob();
            if (job == null) {
                job = Publisher.getInstance().getBackgroundWorkerMusicJob();
            }
            if (job == null) {
                job = Publisher.getInstance().getBackgroundWorkerPhotoJob();
            }
            if (job != null) {
                try {
                    start = System.currentTimeMillis();
                    if (job instanceof SeriesGroup) {
                        SeriesGroup sg = (SeriesGroup) job;
                        log.info("BW" + identifier + ": series group job found => " + sg.getComputedTitel());
                        getEpisodeInfosFromNfo(sg);
                        assignImages(job);
                        if (UMCConstants.onlineinfo || sg.isOptionMovieinfo()) {
                            if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving online series information for " + sg.getComputedTitel());
                            PluginController.getSeriesInfosOnline(sg, log);
                        }
                        if (UMCConstants.fanart || sg.isOptionFanart()) {
                            getSeriesFanart(sg);
                            getEpisodeImages(sg);
                        }
                        log.info("BW" + identifier + ": Starting with creation of the images for the frontend for the series " + sg.getMetaInfoSearchValue());
                        skinRenderer.createSeriesGraphics(sg);
                        for (Season season : sg.getSeasons().values()) {
                            for (Episode episode : season.getEpisodes().values()) {
                                skinRenderer.createEpisodeGraphics(episode);
                            }
                        }
                    } else if (job instanceof Episode) {
                        Episode e = (Episode) job;
                        log.info("BW" + identifier + ": episode job found => " + e.getMetaInfoSearchValue());
                        if (UMCConstants.mediainfo || e.isOptionMediainfo()) getEpisodeMediaInfos(e);
                    } else if (job instanceof MovieGroup) {
                        MovieGroup mg = (MovieGroup) job;
                        log.info("BW" + identifier + ": movie group job found => " + mg.getComputedTitel());
                        assignImages(job);
                        log.info("BW" + identifier + ": Starting with creation of the images for the frontend for the movie group " + mg.getComputedTitel());
                        skinRenderer.createMovieGroupGraphics(mg);
                    } else if (job instanceof Movie) {
                        Movie m = (Movie) job;
                        log.info("BW" + identifier + ": movie job found => " + m.getComputedTitel() + " (isMultipartGroup: " + m.isMultipartgroup() + ")");
                        getInfosFromNfo(m);
                        assignImages(job);
                        assignTrailer(m);
                        if (m.getCollectType() != 1 && (UMCConstants.onlineinfo || m.isOptionMovieinfo())) {
                            if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving online movie information for " + m.getMetaInfoSearchValue());
                            PluginController.getMovieInfosOnline(m, log);
                        }
                        if (m.isOptionPersoninfo()) {
                            if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving online persons information for " + m.getMetaInfoSearchValue());
                            PluginController.getPersonInfosOnline(m.getPersons(), log);
                        }
                        if (UMCConstants.fanart || m.isOptionFanart()) getMovieFanart(m);
                        if (UMCConstants.mediainfo || m.isOptionMediainfo()) getMovieMediaInfos(m);
                        log.info("BW" + identifier + ": Starting with creation of the images for the frontend for the movie " + m.getMetaInfoSearchValue());
                        skinRenderer.createMovieGraphics(m);
                    } else if (job instanceof MusicAlbum) {
                        log.info("============================> ToDo: BackgroundWorker-Logik für MusicAlbum-Bean");
                    } else if (job instanceof Music) {
                        Music m = (Music) job;
                        log.info("BW" + identifier + ": music job found => " + m.getPCPath());
                        if (m.isOptionID3()) {
                            if (m.getFiletype().toLowerCase().endsWith("mp3")) readID3FromFile(m, log);
                        }
                        assignImages(job);
                        if (UMCConstants.mediainfo || m.isOptionMediainfo()) getMusicMediaInfos(m);
                    } else if (job instanceof PhotoAlbum) {
                        log.info("============================> ToDo: BackgroundWorker-Logik für PhotoAlbum-Bean");
                        log.info("============================> ToDo: photoalbum processing still not implemented");
                    } else if (job instanceof Photo) {
                        Photo p = (Photo) job;
                        log.info("BW" + identifier + ": photo job found => " + p.getPCPath());
                        ImageInfo image_info = Sanselan.getImageInfo(new File(p.getPCPath()));
                        p.setWidth(image_info.getWidth());
                        p.setHeight(image_info.getHeight());
                        if (UMCConstants.exifinfo || p.isOptionExif()) readExifFromFile(p);
                    }
                } catch (Throwable exc) {
                    log.error("BW" + identifier + ": error in backgroundworker", exc);
                    if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("BW" + identifier + " : job could not be finsihed -> " + exc.getMessage());
                } finally {
                    Publisher.getInstance().addProcessedJob(job);
                    end = System.currentTimeMillis();
                    UMCStatistics.getInstance().addJobProcessingTime(end - start);
                    System.gc();
                }
            } else {
                if (UMCConstants.debug) log.debug("BackgroundWorker " + identifier + ": closing database connection ");
                dao.closeConnection();
                log.info("BW" + identifier + ": no job found");
                interrupt();
            }
        }
    }

    /**
	 * Liest Werte aus einer xml-Datei und weist diese dem {@link MovieFile} zu.
	 *
	 * @param mf Ein {@link MovieFile} Objekt
	 */
    private void getInfosFromNfo(Movie m) {
        if (m.getMovieNfo() == null) {
            if (UMCConstants.debug) log.debug("BW" + identifier + ": movie nfo for job not found");
            return;
        }
        if (UMCConstants.debug) log.debug("BW" + identifier + ": retreiving information from local movie nfo file (" + m.getMovieNfo().getPath() + "):");
        switch(m.getMovieNfo().getNfoType()) {
            case UMCConstants.NFO_TYPE_UMC_MOVIE:
                assignInfosFromUmcNfo(m);
                break;
            case UMCConstants.NFO_TYPE_YAMJ_MOVIE:
                assignInfosFromYamjNfo(m);
                break;
        }
    }

    private void assignInfosFromUmcNfo(Movie m) {
        try {
            MovieDocument movieDoc = MovieDocument.Factory.parse(new File(m.getMovieNfo().getPath()));
            MovieDocument.Movie movie = movieDoc.getMovie();
            ArrayList validationErrors = new ArrayList();
            XmlOptions validationOptions = new XmlOptions();
            validationOptions.setErrorListener(validationErrors);
            Iterator iter = null;
            boolean isValid = movieDoc.validate(validationOptions);
            boolean process = false;
            if (!isValid) {
                log.error("The nfo is not valid");
                if (UMCConstants.debug) {
                    iter = validationErrors.iterator();
                    while (iter.hasNext()) {
                        log.error(">> " + iter.next() + "\n");
                    }
                }
                if (UMCConstants.debug) log.debug("migrating movie nfo to V2.0 structure...");
                MovieDocument movieDocNew = MovieDocument.Factory.newInstance();
                MovieDocument.Movie movieNew = movieDocNew.addNewMovie();
                Language l = movieDocNew.getMovie().addNewLanguage();
                l.setId(ConfigController.getInstance().getSelectedLanguage().getLanguage());
                if (StringUtils.isNotEmpty(movie.getId())) {
                    Plugin p = l.addNewPlugin();
                    if (movie.getId().startsWith("tt")) {
                        p.setType("imdb");
                    } else {
                        p.setType("ofdb");
                    }
                    p.setId(movie.getId());
                }
                if (StringUtils.isNotEmpty(movie.getTitle())) l.setTitle(movie.getTitle());
                if (StringUtils.isNotEmpty(movie.getPlot())) l.setPlot(movie.getPlot());
                if (StringUtils.isNotEmpty(movie.getRating())) l.setRating(movie.getRating());
                if (StringUtils.isNotEmpty(movie.getGenre())) l.setGenre(movie.getGenre());
                if (StringUtils.isNotEmpty(movie.getYear())) movieNew.setYear(movie.getYear());
                if (StringUtils.isNotEmpty(movie.getBanner())) movieNew.setBanner(movie.getBanner());
                if (StringUtils.isNotEmpty(movie.getCover())) movieNew.setCover(movie.getCover());
                if (StringUtils.isNotEmpty(movie.getPoster())) movieNew.setPoster(movie.getPoster());
                if (StringUtils.isNotEmpty(movie.getBackdrop())) movieNew.setBackdrop(movie.getBackdrop());
                if (StringUtils.isNotEmpty(movie.getFsk())) movieNew.setFsk(movie.getFsk());
                if (StringUtils.isNotEmpty(movie.getResolution())) movieNew.setResolution(movie.getResolution());
                if (movie.isSetCast()) {
                    Cast cast = movieNew.addNewCast();
                    Actor[] actorsOld = movie.getCast().getActorArray();
                    for (Actor actorOld : actorsOld) {
                        Actor actor = cast.addNewActor();
                        actor.setForename(actorOld.getForename());
                        actor.setSurname(actorOld.getSurname());
                    }
                }
                if (movie.isSetAudio()) {
                    Audio audio = movieNew.addNewAudio();
                    Track[] tracksOld = movie.getAudio().getTrackArray();
                    for (Track trackOld : tracksOld) {
                        Track track = audio.addNewTrack();
                        track.setLanguage(trackOld.getLanguage());
                        track.setCodec(trackOld.getCodec());
                        track.setBitrate(trackOld.getBitrate());
                    }
                }
                File source = new File(m.getMovieNfo().getPath());
                File dest = new File(m.getMovieNfo().getPath() + ".bak");
                if (source.exists() && source.isFile()) {
                    if (source.renameTo(dest)) {
                        XmlOptions options = new XmlOptions();
                        options.setSaveAggressiveNamespaces();
                        options.setSavePrettyPrint();
                        movieDocNew.save(new File(m.getMovieNfo().getPath()), options);
                        movieDoc = MovieDocument.Factory.parse(new File(m.getMovieNfo().getPath()));
                        movie = movieDoc.getMovie();
                        validationErrors = new ArrayList();
                        validationOptions = new XmlOptions();
                        validationOptions.setErrorListener(validationErrors);
                        isValid = movieDoc.validate(validationOptions);
                        if (!isValid) {
                            log.error("################# converted movie nfo is not valid! #################");
                            iter = validationErrors.iterator();
                            while (iter.hasNext()) {
                                log.error(">> " + iter.next() + "\n");
                            }
                            return;
                        }
                    } else {
                        if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(m.getMetaInfoSearchValue() + " -> could not create a backup of " + m.getMovieNfo().getPath() + " -> nfo could not be migrated and will be disgarded");
                        log.error("Could not create a backup of " + m.getMovieNfo().getPath() + " -> nfo could not be migrated and will be disgarded");
                    }
                }
            }
            String language = null;
            for (Language l : movie.getLanguageArray()) {
                language = l.getId().toUpperCase();
                if (UMCConstants.debug) log.debug("nfo: LanguageID: " + language);
                Plugin[] plugins = l.getPluginArray();
                if (plugins != null) {
                    for (Plugin plugin : plugins) {
                        if (StringUtils.isNotEmpty(plugin.getId())) {
                            if (UMCConstants.debug) log.debug("nfo: setting movie id: " + plugin.getId() + " for plugin " + plugin.getType());
                            m.getLanguageData().addID(plugin.getType(), plugin.getId());
                        }
                    }
                }
                if (l.getTitle() != null && !l.getTitle().equals("")) {
                    if (UMCConstants.debug) log.debug("nfo: setting title: " + l.getTitle() + " for language " + language);
                    m.getLanguageData().addTitle(l.getTitle(), language);
                }
                if (l.getPlot() != null && !l.getPlot().equals("")) {
                    if (UMCConstants.debug) log.debug("nfo: setting story: " + l.getPlot() + " for language " + language);
                    m.getLanguageData().addPlot(l.getPlot(), language);
                }
                if (l.getRating() != null && !l.getRating().equals("")) {
                    if (UMCConstants.debug) log.debug("nfo: setting rating: " + l.getRating() + " for language " + language);
                    try {
                        float f = Float.parseFloat(l.getRating().replaceAll(",", "."));
                        m.getLanguageData().addRating(f, language);
                    } catch (NumberFormatException exc) {
                        if (UMCConstants.debug) log.warn("nfo: Rating " + l.getRating() + " could not be set. Value is not a number!", exc);
                        if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(m.getMetaInfoSearchValue() + " -> nfo: Rating " + l.getRating() + " could not be set. Value is not a number!");
                    }
                }
                if (StringUtils.isNotEmpty(l.getGenre())) {
                    String genre = "";
                    Scanner sc = new Scanner(l.getGenre()).useDelimiter(",");
                    while (sc.hasNext()) {
                        genre = sc.next().trim();
                        if (UMCConstants.debug) log.debug("nfo: setting genre: " + genre + " for language " + language);
                        m.addGenre(genre);
                    }
                }
            }
            if (movie.getYear() != null && !movie.getYear().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting year: " + movie.getYear());
                if (Pattern.matches("\\d\\d\\d\\d", movie.getYear())) m.setYear(Integer.parseInt(movie.getYear()));
            }
            if (movie.getBanner() != null && !movie.getBanner().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting banner: " + movie.getBanner());
                BannerImage banner = new BannerImage();
                banner.setAbsolutePath(movie.getBanner());
                m.setBanner(banner);
            }
            if (movie.getCover() != null && !movie.getCover().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting cover: " + movie.getCover());
                CoverImage cover = new CoverImage();
                cover.setAbsolutePath(movie.getCover());
                m.setCover(cover);
            }
            if (movie.getPoster() != null && !movie.getPoster().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting poster: " + movie.getPoster());
                PosterImage poster = new PosterImage();
                poster.setAbsolutePath(movie.getPoster());
                m.setPoster(poster);
            }
            if (movie.getBackdrop() != null && !movie.getBackdrop().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting backdrop: " + movie.getBackdrop());
                BackdropImage backdrop = new BackdropImage();
                backdrop.setAbsolutePath(movie.getBackdrop());
                m.setBackdrop(backdrop);
            }
            if (movie.getFsk() != null && !movie.getFsk().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting fsk: " + movie.getFsk());
                if (Pattern.matches("\\d", movie.getFsk())) m.setFSK(Byte.parseByte(movie.getFsk()));
            }
            if (movie.getResolution() != null && !movie.getResolution().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting resolution: " + movie.getResolution());
                m.setResolution(movie.getResolution());
            }
            Cast actors = movie.getCast();
            if (actors == null) {
            } else {
                String fullName = "";
                Actor[] a = actors.getActorArray();
                for (int k = 0; k < a.length; k++) {
                    fullName = "";
                    if (StringUtils.isNotEmpty(a[k].getForename())) fullName += a[k].getForename();
                    if (StringUtils.isNotEmpty(a[k].getSurname())) fullName += " " + a[k].getSurname();
                    if (StringUtils.isNotEmpty(fullName)) {
                        if (UMCConstants.debug) log.debug("nfo: setting actor: " + fullName);
                        com.umc.beans.persons.IPerson act = new com.umc.beans.persons.Actor();
                        act.setName(fullName);
                        m.addPerson(act);
                    }
                }
            }
            Audio audio = movie.getAudio();
            if (audio != null) {
                Track[] tracks = audio.getTrackArray();
                for (int i = 0; i < tracks.length; i++) {
                    AudioTrack at = new AudioTrack();
                    at.setLanguage(tracks[i].getLanguage());
                    at.setCodec(tracks[i].getCodec());
                    if (UMCConstants.debug) log.debug("nfo: setting audiotrack: [" + at.getLanguage() + " | " + at.getCodec() + "]");
                    m.addAudioTrack(at);
                }
            }
        } catch (IOException exc) {
            log.error("Error reading movie nfo file: " + exc.getMessage());
            if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(m.getMetaInfoSearchValue() + " -> Error reading movie nfo file");
        } catch (XmlException exc) {
            log.error("Error loading movie nfo file: " + exc.getMessage());
            if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(m.getMetaInfoSearchValue() + " -> Error loading movie nfo file");
        } catch (Exception exc) {
            log.error("General error during reading of movie nfo file for " + m.getFilename(), exc);
            if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(m.getMetaInfoSearchValue() + " -> General error during reading of movie nfo file");
        }
    }

    private void assignInfosFromYamjNfo(Movie m) {
        try {
            de.umcProject.xmlbeans.yamj.MovieDocument movieDoc = de.umcProject.xmlbeans.yamj.MovieDocument.Factory.parse(new File(m.getMovieNfo().getPath()));
            de.umcProject.xmlbeans.yamj.MovieDocument.Movie movie = movieDoc.getMovie();
            if (movie.getIdArray() != null && movie.getIdArray().length > 0) {
                for (int a = 0; a < movie.getIdArray().length; a++) {
                    Id id = movie.getIdArray()[a];
                    if (!id.isSetMoviedb()) {
                        if (UMCConstants.debug) log.debug("nfo: setting IMDB ID: " + id.toString());
                        m.getLanguageData().addID("imdb", id.toString());
                    } else {
                        if (id.getMoviedb().equals("allocine")) {
                        } else if (id.getMoviedb().equals("filmweb")) {
                        }
                    }
                }
            }
            String language = "EN";
            if (StringUtils.isNotEmpty(movie.getTitle())) {
                if (UMCConstants.debug) log.debug("nfo: setting title: " + movie.getTitle() + " for language " + language);
                m.getLanguageData().addTitle(movie.getTitle(), language);
            }
            if (StringUtils.isNotEmpty(movie.getPlot())) {
                if (UMCConstants.debug) log.debug("nfo: setting story: " + movie.getPlot() + " for language " + language);
                m.getLanguageData().addPlot(movie.getPlot(), language);
            }
            if (StringUtils.isNotEmpty(movie.getRating())) {
                if (UMCConstants.debug) log.debug("nfo: setting rating: " + movie.getRating() + " for language " + language);
                try {
                    float f = Float.parseFloat(movie.getRating().replaceAll(",", "."));
                    m.getLanguageData().addRating(f, language);
                } catch (NumberFormatException exc) {
                    if (UMCConstants.debug) log.warn("nfo: Rating " + movie.getRating() + " could not be set. Value is not a number!", exc);
                }
            }
            if (StringUtils.isNotEmpty(movie.getGenre())) {
                StringTokenizer st = new StringTokenizer(movie.getGenre(), ",");
                while (st.hasMoreTokens()) {
                    String genre = st.nextToken().trim();
                    if (UMCConstants.debug) log.debug("nfo: setting genre: " + genre + " for language " + language);
                    m.addGenre(genre);
                }
            }
            if (StringUtils.isNotEmpty(movie.getYear())) {
                if (UMCConstants.debug) log.debug("nfo: setting year: " + movie.getYear());
                if (Pattern.matches("\\d\\d\\d\\d", movie.getYear())) m.setYear(Integer.parseInt(movie.getYear()));
            }
            de.umcProject.xmlbeans.yamj.MovieDocument.Movie.Actor[] actors = movie.getActorArray();
            for (int i = 0; i < actors.length; i++) {
                if (StringUtils.isNotEmpty(actors[i].getName())) {
                    if (UMCConstants.debug) log.debug("nfo: setting actor: " + actors[i].getName());
                    com.umc.beans.persons.IPerson act = new com.umc.beans.persons.Actor();
                    act.setName(actors[i].getName());
                    m.addPerson(act);
                }
            }
            if (StringUtils.isNotEmpty(movie.getMpaa())) {
                if (UMCConstants.debug) log.debug("nfo: setting mpaa: " + movie.getMpaa());
                if (Pattern.matches("\\d", movie.getMpaa())) m.setFSK(Byte.parseByte(movie.getMpaa()));
            }
            if (movie.getFileinfoArray() != null && movie.getFileinfoArray().length > 0) {
                if (movie.getFileinfoArray()[0].getStreamdetailsArray() != null && movie.getFileinfoArray()[0].getStreamdetailsArray().length > 0) {
                    if (movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getVideoArray() != null && movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getVideoArray().length > 0) {
                        String width = movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getVideoArray()[0].getWidth();
                        if (UMCConstants.debug) log.debug("nfo: setting width: " + width);
                        m.setWidth(width);
                        String height = movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getVideoArray()[0].getHeight();
                        if (UMCConstants.debug) log.debug("nfo: setting height: " + height);
                        m.setWidth(height);
                    }
                    if (movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getAudioArray() != null && movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getAudioArray().length > 0) {
                        for (int i = 0; i < movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getAudioArray().length; i++) {
                            AudioTrack at = new AudioTrack();
                            at.setLanguage(movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getAudioArray()[i].getLanguage());
                            at.setCodec(movie.getFileinfoArray()[0].getStreamdetailsArray()[0].getAudioArray()[i].getCodec());
                            if (UMCConstants.debug) log.debug("nfo: setting audiotrack: [" + at.getLanguage() + " | " + at.getCodec() + "]");
                            m.addAudioTrack(at);
                        }
                    }
                }
            }
        } catch (IOException exc) {
            log.error("Error reading movie nfo file: " + exc.getMessage());
        } catch (XmlException exc) {
            log.error("Error loading movie nfo file: " + exc.getMessage());
        } catch (Exception exc) {
            log.error("General error during reading of movie nfo file for " + m.getFilename(), exc);
        }
    }

    /**
	 * Liest Werte aus einer xml-Datei und weist diese dem {@link SeriesGroup} zu.
	 *
	 * @param mf Ein {@link MovieFile} Objekt
	 */
    private void getEpisodeInfosFromNfo(SeriesGroup sg) {
        if (sg.getSeriesNfo() == null) {
            log.debug("No xml file assigned to series group");
            return;
        }
        if (UMCConstants.debug) log.debug("Retreiving epsiode information from local series xml file (" + sg.getSeriesNfo().getPath() + "):");
        switch(sg.getSeriesNfo().getNfoType()) {
            case UMCConstants.NFO_TYPE_UMC_SERIES:
                assignEpisodeInfosFromUmcNfo(sg);
                break;
        }
    }

    private void assignEpisodeInfosFromUmcNfo(SeriesGroup sg) {
        try {
            SeriesDocument seriesDoc = SeriesDocument.Factory.parse(new File(sg.getSeriesNfo().getPath()));
            SeriesDocument.Series series = seriesDoc.getSeries();
            if (series.getIdThetvdb() != null && !series.getIdThetvdb().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting online moviedatabase ID: " + series.getIdThetvdb());
                sg.setIdTHETVDB(series.getIdThetvdb());
            }
            if (series.getTitle() != null && !series.getTitle().equals("")) {
                for (String lng : sg.getLanguages()) {
                    if (UMCConstants.debug) log.debug("nfo: setting series title (" + lng + "): " + series.getTitle());
                    sg.setTitle(lng, series.getTitle());
                }
            }
            if (series.getPlot() != null && !series.getPlot().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting series plot: " + series.getPlot());
                sg.getLanguageData().addPlot(series.getPlot(), "EN");
            }
            if (series.getRating() != null && !series.getRating().equals("")) {
                if (Pattern.matches("\\d", series.getRating())) {
                    if (UMCConstants.debug) log.debug("nfo: setting series rating: " + series.getRating());
                    sg.setRatingTHETVDB(Float.parseFloat(series.getRating().replaceAll(",", ".")));
                } else {
                    if (UMCConstants.debug) log.warn("nfo: Rating " + series.getRating() + " could not be set. Value is not a number!");
                }
            }
            de.umcProject.xmlbeans.SeriesDocument.Series.Audio audio = series.getAudio();
            if (audio != null) {
                de.umcProject.xmlbeans.SeriesDocument.Series.Audio.Track[] tracks = audio.getTrackArray();
                for (int i = 0; i < tracks.length; i++) {
                    AudioTrack at = new AudioTrack();
                    at.setLanguage(tracks[i].getLanguage());
                    at.setCodec(tracks[i].getCodec());
                    sg.addAudioTrack(at);
                }
            }
            if (series.getGenre() != null && !series.getGenre().equals("")) {
                StringTokenizer st = new StringTokenizer(series.getGenre(), ",");
                String genre = "";
                while (st.hasMoreTokens()) {
                    genre = st.nextToken();
                    if (UMCConstants.debug) log.debug("nfo: setting series genre: " + genre);
                    sg.getLanguageData().addGenre(genre, "EN");
                }
            }
            if (series.getYear() != null && !series.getYear().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting year: " + series.getYear());
                if (Pattern.matches("\\d\\d\\d\\d", series.getYear())) sg.setYear(Integer.parseInt(series.getYear()));
            }
            de.umcProject.xmlbeans.SeriesDocument.Series.Cast[] actors = series.getCastArray();
            String fullName = "";
            for (int i = 0; i < actors.length; i++) {
                de.umcProject.xmlbeans.SeriesDocument.Series.Cast.Actor[] a = actors[i].getActorArray();
                for (int k = 0; k < a.length; k++) {
                    fullName = "";
                    if (a[k].getForename() != null && !a[k].getForename().equals("")) fullName += a[k].getForename();
                    if (a[k].getSurname() != null && !a[k].getSurname().equals("")) fullName += " " + a[k].getSurname();
                    if (UMCConstants.debug) log.debug("nfo: setting series actor: " + fullName);
                    com.umc.beans.persons.IPerson act = new com.umc.beans.persons.Actor();
                    act.setName(fullName);
                    sg.addPerson(act);
                }
            }
            if (series.getResolution() != null && !series.getResolution().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting series resolution: " + series.getResolution());
                sg.setResolution(series.getResolution());
            }
            if (series.getBanner() != null && !series.getBanner().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting series banner: " + series.getBanner());
                BannerImage banner = new BannerImage();
                banner.setAbsolutePath(series.getBanner());
                sg.setBanner(banner);
            }
            if (series.getCover() != null && !series.getCover().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting series cover: " + series.getCover());
                CoverImage cover = new CoverImage();
                cover.setAbsolutePath(series.getCover());
                sg.setCover(cover);
            }
            if (series.getBackdrop() != null && !series.getBackdrop().equals("")) {
                if (UMCConstants.debug) log.debug("nfo: setting series backdrop: " + series.getBackdrop());
                BackdropImage backdrop = new BackdropImage();
                backdrop.setAbsolutePath(series.getBackdrop());
                sg.setBackdrop(backdrop);
            }
        } catch (IOException exc) {
            log.error("Error reading series nfo file: " + exc.getMessage());
        } catch (XmlException exc) {
            log.error("Error loading series nfo file: " + exc.getMessage());
        } catch (Exception exc) {
            log.error("General error during reading of the series nfo file for the series " + sg.getMetaInfoSearchValue(), exc);
        }
    }

    /**
	 * Diese Methode liest mit Hilfe von MediaInfo Medieinfos wie Auflösung,Tonspur,Codec usw. aus
	 * und weist die Informationen dem übergebenen {@link MovieFile} zu.
	 *
	 * HINWEIS:
	 * Filme die HD Auflösung haben bekommen in dieser Methode automatisch das System-Genre HD zugewiesen.
	 *
	 * @param mf
	 */
    private void getMovieMediaInfos(Movie mf) {
        if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving Media infos from movie file...");
        if (new File(System.getProperty("user.dir") + "/tools/").exists()) {
            Process p = null;
            try {
                String mediaPath = mf.getPCPath();
                log.info("=======>" + mediaPath);
                if (StringUtils.isNotEmpty(mediaPath) && !mediaPath.startsWith("\\\\")) mediaPath = mediaPath.replaceAll("//", "/").replaceAll("\\\\\\\\", "\\");
                log.info("File: " + mediaPath + " for " + mf.getMetaInfoSearchValue());
                if (mediaPath == null || mediaPath.equals("")) {
                    log.warn("Path to movie file not valid -> " + mediaPath + ". MediaInfo could not retreive the media information and will be aborted.");
                    return;
                }
                String[] playWin = { System.getProperty("user.dir") + "/tools/win/MediaInfo.exe", "--Inform=file://mediainfo_template", mediaPath };
                String[] playMac = { "./mediainfo", "--Inform=file://mediainfo_template", mediaPath };
                String[] playLinux = { Publisher.getInstance().getParamMediainfo(), "--Inform=file://mediainfo_template", mediaPath };
                ProcessBuilder pc = null;
                if (UMCConstants.os.toUpperCase().indexOf("WIN") != -1) {
                    pc = new ProcessBuilder(playWin);
                    pc.directory(new File(System.getProperty("user.dir") + "/tools/win/"));
                } else if (UMCConstants.os.toUpperCase().indexOf("MAC OS") != -1) {
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
                if (UMCConstants.debug) {
                    log.debug("fileSize -> " + props.getProperty("fileSize", "").toString());
                    log.debug("videoCodec -> " + props.getProperty("videoCodec", "").toString());
                    log.debug("videoBitrate -> " + props.getProperty("videoBitrate", "").toString());
                    log.debug("videoWidth -> " + props.getProperty("videoWidth", "").toString());
                    log.debug("videoHeight -> " + props.getProperty("videoHeight", "").toString());
                    log.debug("videoFrameRate -> " + props.getProperty("videoFrameRate", "").toString());
                    log.debug("durationHHmm -> " + props.getProperty("durationHHmm", "").toString());
                    log.debug("videoAR -> " + props.getProperty("videoAR", "").toString());
                    log.debug("audioCount -> " + props.getProperty("audioCount", "").toString());
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
                if (mf.isMultipartgroup() || mf.isVideoTS() || mf.isBDMV()) {
                    long milliseconds = 0;
                    ProcessBuilder pcMP = null;
                    Process pMP = null;
                    InputStream isMP = null;
                    InputStreamReader isrMP = null;
                    Properties propsMP = null;
                    for (int x = 0; x < mf.getChilds().size(); x++) {
                        pcMP = new ProcessBuilder(pc.command());
                        pcMP.directory(pc.directory());
                        pMP = pc.start();
                        isMP = pMP.getInputStream();
                        isrMP = new InputStreamReader(isMP);
                        propsMP = new Properties();
                        propsMP.load(isrMP);
                        try {
                            milliseconds += Long.parseLong(propsMP.getProperty("durationMs", "").toString());
                        } catch (NumberFormatException e) {
                            log.info("asdfasdfasd");
                        }
                        pMP.getErrorStream().close();
                        pMP.getInputStream().close();
                        pMP.getOutputStream().close();
                        int returnCode = pMP.waitFor();
                        if (returnCode != 0) {
                            log.error("Error mediainfo return code = " + returnCode + " => " + pcMP.command());
                            if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("Error mediainfo return code = " + returnCode + " => " + pcMP.command());
                        }
                    }
                    if (mf.getDuration() == 0) mf.setDuration(milliseconds);
                    if (StringUtils.isEmpty(mf.getDurationFormatted())) {
                        int milli = (int) (milliseconds % 1000);
                        milliseconds /= 1000;
                        byte sec = (byte) (milliseconds % 60);
                        milliseconds /= 60;
                        byte min = (byte) (milliseconds % 60);
                        milliseconds /= 60;
                        byte h = (byte) (milliseconds % 24);
                        milliseconds /= 24;
                        int d = (int) milliseconds;
                        mf.setDurationFormatted(h + "h" + " " + min + "mn");
                    }
                } else {
                    if (mf.getDuration() == 0 && StringUtils.isNotEmpty(props.getProperty("durationMs", ""))) mf.setDuration(Long.parseLong(props.getProperty("durationMs", "")));
                    if (StringUtils.isEmpty(mf.getDurationFormatted())) mf.setDurationFormatted(props.getProperty("durationHHmm", "").toString());
                }
                if (mf.getAspectRatio() == null || mf.getAspectRatio().equals("")) mf.setAspectRatio(props.getProperty("videoAR", "").toString());
                if (StringUtils.isNotEmpty(mf.getWidth()) && StringUtils.isNotEmpty(mf.getHeight())) {
                    int width = Integer.parseInt(mf.getWidth());
                    int height = Integer.parseInt(mf.getHeight());
                    if (width == 1920) {
                        if (height <= 1080) mf.addIcon(UMCConstants.ICON_1080p);
                    } else if (width == 1280) {
                        if (height <= 720) mf.addIcon(UMCConstants.ICON_720p);
                    } else {
                        if (height > 540 && height <= 576) {
                            mf.addIcon(UMCConstants.ICON_576p);
                        } else if (height > 480 && height <= 540) {
                            mf.addIcon(UMCConstants.ICON_540p);
                        } else if (height <= 480) {
                            mf.addIcon(UMCConstants.ICON_480p);
                        }
                    }
                }
                if ((mf.getAudioTracks() != null || mf.getAudioTracks().size() == 0) && StringUtils.isNotEmpty(props.getProperty("audioCount"))) {
                    int audioCount = Integer.parseInt(props.getProperty("audioCount"));
                    for (int a = 0; a < audioCount; a++) {
                        AudioTrack audioTrack = new AudioTrack();
                        if (StringUtils.isNotEmpty(props.getProperty("audioChannels" + a))) {
                            audioTrack.setChannels(props.getProperty("audioChannels" + a, "").toString().replaceAll("channels", "").trim());
                        }
                        if (StringUtils.isNotEmpty(props.getProperty("audioFormat" + a))) {
                            String formatProfile = props.getProperty("formatProfile" + a, "").toString();
                            if (StringUtils.isNotEmpty(formatProfile) && (formatProfile.indexOf("TrueHD") != -1 || formatProfile.equalsIgnoreCase("HRA") || formatProfile.equalsIgnoreCase("MA"))) {
                                if (formatProfile.indexOf("TrueHD") != -1) {
                                    audioTrack.setCodec("TrueHD");
                                    mf.addIcon(UMCConstants.ICON_DOLBYTRUEHD);
                                }
                                if (formatProfile.equalsIgnoreCase("HRA") || formatProfile.equalsIgnoreCase("MA")) {
                                    audioTrack.setCodec("DTS-HD");
                                    mf.addIcon(UMCConstants.ICON_DTSMA);
                                }
                            } else {
                                audioTrack.setCodec(props.getProperty("audioFormat" + a, "").toString());
                                if (audioTrack.getCodec().equalsIgnoreCase("AC-3")) {
                                    mf.addIcon(UMCConstants.ICON_AC3);
                                } else if (audioTrack.getCodec().equalsIgnoreCase("DTS")) {
                                    if (audioTrack.getChannels() == "6") mf.addIcon(UMCConstants.ICON_DTS51); else if (audioTrack.getChannels() == "8") mf.addIcon(UMCConstants.ICON_DTS71); else mf.addIcon(UMCConstants.ICON_DTS);
                                } else if (audioTrack.getCodec().equalsIgnoreCase("MPEG Audio")) {
                                    mf.addIcon(UMCConstants.ICON_MP2);
                                } else if (audioTrack.getCodec().equalsIgnoreCase("AAC")) {
                                    mf.addIcon(UMCConstants.ICON_AAC);
                                }
                            }
                        }
                        if (StringUtils.isNotEmpty(props.getProperty("audioLng" + a))) audioTrack.setLanguage(props.getProperty("audioLng" + a, "").toString().toUpperCase());
                        if (StringUtils.isNotEmpty(props.getProperty("audioBitrate" + a))) audioTrack.setBitrate(props.getProperty("audioBitrate" + a, "").toString());
                        if (StringUtils.isNotEmpty(audioTrack.getCodec()) || StringUtils.isNotEmpty(audioTrack.getLanguage()) || StringUtils.isNotEmpty(audioTrack.getChannels()) || StringUtils.isNotEmpty(audioTrack.getBitrate())) {
                            mf.addAudioTrack(audioTrack);
                            if (UMCConstants.debug) log.debug("Audio Track " + (a + 1) + " added: " + audioTrack.getCodec() + " | " + audioTrack.getLanguage() + " | " + audioTrack.getChannels() + " | " + audioTrack.getBitrate());
                        } else {
                            if (UMCConstants.debug) log.debug("Audio Track " + (a + 1) + " not added: " + audioTrack.getCodec() + " | " + audioTrack.getLanguage() + " | " + audioTrack.getChannels() + " | " + audioTrack.getBitrate());
                        }
                    }
                }
                p.getErrorStream().close();
                p.getInputStream().close();
                p.getOutputStream().close();
                int returnCode = p.waitFor();
                if (returnCode == 0) {
                } else {
                    log.error("Error mediainfo return code = " + returnCode + " => " + pc.command());
                    mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getMetaInfoSearchValue() + ": Returncode -> " + returnCode));
                    if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("Error mediainfo return code = " + returnCode + " => " + pc.command());
                }
            } catch (NullPointerException exc) {
                log.error("NullPointer error", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getMetaInfoSearchValue() + ": NullPointer Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + mf.getMetaInfoSearchValue() + " (" + mf.getFilename() + mf.getFiletype() + "): NullPointer Exception");
            } catch (IndexOutOfBoundsException exc) {
                log.error("IndexOutOfBounds error", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getMetaInfoSearchValue() + ": IndexOutOfBounds Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + mf.getMetaInfoSearchValue() + " (" + mf.getFilename() + mf.getFiletype() + "): IndexOutOfBounds Exception");
            } catch (SecurityException exc) {
                log.error("Security error", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getMetaInfoSearchValue() + ": Security Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + mf.getMetaInfoSearchValue() + " (" + mf.getFilename() + mf.getFiletype() + "): Security Exception");
            } catch (IOException exc) {
                log.error("IO error", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getMetaInfoSearchValue() + ": IO Exception, check MediaInfo permissions"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + mf.getFilename() + mf.getFiletype() + " - IO Exception, check MediaInfo permissions");
            } catch (Exception exc) {
                log.error("Allgemeiner Fehler", exc);
                mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, mf.getMetaInfoSearchValue() + ": Generic Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + mf.getMetaInfoSearchValue() + ": Generic Exception");
            }
        } else {
            log.error("MediaInfo directory " + System.getProperty("user.dir") + "/tools/" + " not found.");
            mf.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, "Verzeichniss " + System.getProperty("user.dir") + "/tools/" + " wurde nicht gefunden."));
            if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: Tools directory " + System.getProperty("user.dir") + "/tools/" + " not found.");
        }
    }

    /**
	 * Diese Methode liest mit Hilfe von MediaInfo Medieinfos wie Auflösung,Tonspur,Codec usw. aus
	 * und weist die Informationen der übergebenen {@link Episode} zu.
	 *
	 * HINWEIS:
	 * Filme die HD Auflösung haben bekommen in dieser Methode automatisch das System-Genre HD zugewiesen.
	 *
	 * @param sg
	 */
    private void getEpisodeMediaInfos(Episode e) {
        if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving Media infos from episode file '" + e.getPCPath() + "'...");
        if (new File(System.getProperty("user.dir") + "/tools/").exists()) {
            Process p = null;
            try {
                String os = System.getProperty("os.name");
                String mediaPath = e.getPCPath();
                if (StringUtils.isNotEmpty(mediaPath) && !mediaPath.startsWith("\\\\")) mediaPath = mediaPath.replaceAll("//", "/").replaceAll("\\\\\\\\", "\\");
                log.info("File: " + mediaPath + " for " + e.getMetaInfoSearchValue());
                if (mediaPath == null || mediaPath.equals("")) {
                    log.warn("Path to episode file not valid -> " + mediaPath + ". MediaInfo could not retreive the media information and will be aborted.");
                    return;
                }
                String[] playWin = { System.getProperty("user.dir") + "/tools/win/MediaInfo.exe", "--Inform=file://mediainfo_template", mediaPath };
                String[] playMac = { "./mediainfo", "--Inform=file://mediainfo_template", mediaPath };
                String[] playLinux = { Publisher.getInstance().getParamMediainfo(), "--Inform=file://mediainfo_template", mediaPath };
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
                if (UMCConstants.debug) {
                    log.debug(props.getProperty("fileSize", "").toString());
                    log.debug(props.getProperty("videoCodec", "").toString());
                    log.debug(props.getProperty("videoBitrate", "").toString());
                    log.debug(props.getProperty("videoWidth", "").toString());
                    log.debug(props.getProperty("videoHeight", "").toString());
                    log.debug(props.getProperty("videoFrameRate", "").toString());
                    log.debug(props.getProperty("durationHHmm", "").toString());
                    log.debug(props.getProperty("videoAR", "").toString());
                }
                if (e.getFilesize() == null || e.getFilesize().equals("")) {
                    e.setFilesize(props.getProperty("fileSize", "").toString().replaceAll("GiB", "GB").replaceAll("MiB", "MB"));
                }
                if (e.getCodec() == null || e.getCodec().equals("")) e.setCodec(props.getProperty("videoCodec", "").toString());
                if (e.getBitrate() == null || e.getBitrate().equals("")) e.setBitrate(props.getProperty("videoBitrate", "").toString());
                if (e.getWidth() == null || e.getWidth().equals("")) {
                    e.setWidth(props.getProperty("videoWidth", ""));
                }
                if (e.getHeight() == null || e.getHeight().equals("")) {
                    e.setHeight(props.getProperty("videoHeight", "").toString());
                }
                if (e.getFramerate() == null || e.getFramerate().equals("")) e.setFramerate(props.getProperty("videoFrameRate", "").toString());
                if (e.getDuration() == 0 && StringUtils.isNotEmpty(props.getProperty("durationMs", ""))) e.setDuration(Long.parseLong(props.getProperty("durationMs", "")));
                if (StringUtils.isEmpty(e.getDurationFormatted())) e.setDurationFormatted(props.getProperty("durationHHmm", "").toString());
                if (e.getAspectRatio() == null || e.getAspectRatio().equals("")) e.setAspectRatio(props.getProperty("videoAR", "").toString());
                p.getErrorStream().close();
                p.getInputStream().close();
                p.getOutputStream().close();
                int returnCode = p.waitFor();
                if (returnCode == 0) {
                } else {
                    log.error("Error mediainfo return code = " + returnCode + " => " + pc.command());
                    e.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, e.getMetaInfoSearchValue() + ": Returncode -> " + returnCode));
                    if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("Error mediainfo return code = " + returnCode + " => " + pc.command());
                }
            } catch (NullPointerException exc) {
                log.error("NullPointer error", exc);
                e.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, e.getMetaInfoSearchValue() + ": NullPointer Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + e.getMetaInfoSearchValue() + " (" + e.getFilename() + e.getFiletype() + "): NullPointer Exception");
            } catch (IndexOutOfBoundsException exc) {
                log.error("IndexOutOfBounds error", exc);
                e.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, e.getMetaInfoSearchValue() + ": IndexOutOfBounds Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + e.getMetaInfoSearchValue() + " (" + e.getFilename() + e.getFiletype() + "): IndexOutOfBounds Exception");
            } catch (SecurityException exc) {
                log.error("Security error", exc);
                e.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, e.getMetaInfoSearchValue() + ": Security Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + e.getMetaInfoSearchValue() + " (" + e.getFilename() + e.getFiletype() + "): Security Exception");
            } catch (IOException exc) {
                log.error("IO error", exc);
                e.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, e.getMetaInfoSearchValue() + ": IO Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + e.getMetaInfoSearchValue() + " (" + e.getFilename() + e.getFiletype() + "): IO Exception");
            } catch (Exception exc) {
                log.error("Allgemeiner Fehler", exc);
                e.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, e.getMetaInfoSearchValue() + ": Generic Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + e.getMetaInfoSearchValue() + ": Generic Exception");
            }
        } else {
            log.error("MPlayer directory " + System.getProperty("user.dir") + "/tools/" + " not found.");
            e.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, "Verzeichniss " + System.getProperty("user.dir") + "/tools/" + " wurde nicht gefunden."));
            if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: Tools directory " + System.getProperty("user.dir") + "/tools/" + " not found.");
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
    private void getMusicMediaInfos(Music m) {
        if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving Media infos from music file...");
        if (new File(System.getProperty("user.dir") + "/tools/").exists()) {
            Process p = null;
            try {
                String os = System.getProperty("os.name");
                String mediaPath = m.getScanDir() + UMCConstants.fileSeparator + m.getPath();
                if (StringUtils.isNotEmpty(mediaPath)) mediaPath = mediaPath.replaceAll("//", "/").replaceAll("\\\\\\\\", "\\");
                log.info("File: " + mediaPath + " for " + m.getMetaInfoSearchValue());
                if (mediaPath == null || mediaPath.equals("")) {
                    log.warn("Path to music file not valid -> " + mediaPath + ". MediaInfo could not retreive the media information and will be aborted.");
                    return;
                }
                String[] playWin = { System.getProperty("user.dir") + "/tools/win/MediaInfo.exe", "--Inform=file://mediainfo_audio", mediaPath };
                String[] playMac = { "./mediainfo", "--Inform=file://mediainfo_audio", mediaPath };
                String[] playLinux = { Publisher.getInstance().getParamMediainfo(), "--Inform=file://mediainfo_audio", mediaPath };
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
                if (UMCConstants.debug) {
                    log.debug(props.getProperty("artist", "").toString());
                    log.debug(props.getProperty("album", "").toString());
                    log.debug(props.getProperty("title", "").toString());
                    log.debug(props.getProperty("genre", "").toString());
                    log.debug(props.getProperty("track", "").toString());
                    log.debug(props.getProperty("durationMs", "").toString());
                    log.debug(props.getProperty("durationHHmm", "").toString());
                    log.debug(props.getProperty("mode", "").toString());
                    log.debug(props.getProperty("bitrate", "").toString());
                    log.debug(props.getProperty("cover", "").toString());
                    log.debug(props.getProperty("coverdata", "").toString());
                }
                if (StringUtils.isEmpty(m.getArtist())) m.setArtist(props.getProperty("artist", "").toString());
                if (StringUtils.isEmpty(m.getAlbum())) m.setAlbum(props.getProperty("album", "").toString());
                if (StringUtils.isEmpty(m.getTitle())) m.setTitle(props.getProperty("title", "").toString());
                if (StringUtils.isEmpty(m.getGenre())) m.setGenre(props.getProperty("genre", "").toString());
                if (m.getTrack() == -1) m.setTrack(Integer.parseInt(props.getProperty("genre", "").toString()));
                if (m.getDuration() == 0 && StringUtils.isNotEmpty(props.getProperty("durationMs", ""))) m.setDuration(Long.parseLong(props.getProperty("durationMs", "")));
                if (StringUtils.isEmpty(m.getDurationFormatted())) m.setDurationFormatted(props.getProperty("durationHHmm", "").toString());
                if (m.getCodec() == null || m.getCodec().equals("")) m.setCodec(props.getProperty("mode", "").toString());
                if (m.getBitrate() == null || m.getBitrate().equals("")) m.setBitrate(props.getProperty("bitrate", "").toString());
                if (m.getCover() == null && props.getProperty("cover", "").toString().equals("yes")) {
                    String base64EncodetString = props.getProperty("coverdata", "").toString();
                    byte[] decoded = Base64.decodeBase64(base64EncodetString.getBytes());
                    try {
                        String file = System.getProperty("user.dir") + "/resources/Cover/Movies/" + m.getFilename() + Publisher.getInstance().getParamCoverFilenameExtension() + ".jpg";
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(decoded);
                        fos.close();
                        CoverImage cover = new CoverImage();
                        cover.setAbsolutePath(file);
                        cover.setFinished(false);
                        m.setCover(cover);
                    } catch (FileNotFoundException ex) {
                        log.error("FileNotFoundException : " + ex);
                    } catch (IOException ioe) {
                        log.error("IOException : " + ioe);
                    }
                }
                p.getErrorStream().close();
                p.getInputStream().close();
                p.getOutputStream().close();
                int returnCode = p.waitFor();
                if (returnCode == 0) {
                } else {
                    log.error("Error mediainfo return code = " + returnCode + " => " + pc.command());
                    m.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, m.getMetaInfoSearchValue() + ": Returncode -> " + returnCode));
                    if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("Error mediainfo return code = " + returnCode + " => " + pc.command());
                }
            } catch (NullPointerException exc) {
                log.error("NullPointer error", exc);
                m.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, m.getMetaInfoSearchValue() + ": NullPointer Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + m.getMetaInfoSearchValue() + " (" + m.getFilename() + m.getFiletype() + "): NullPointer Exception");
            } catch (IndexOutOfBoundsException exc) {
                log.error("IndexOutOfBounds error", exc);
                m.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, m.getMetaInfoSearchValue() + ": IndexOutOfBounds Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + m.getMetaInfoSearchValue() + " (" + m.getFilename() + m.getFiletype() + "): IndexOutOfBounds Exception");
            } catch (SecurityException exc) {
                log.error("Security error", exc);
                m.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, m.getMetaInfoSearchValue() + ": Security Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + m.getMetaInfoSearchValue() + " (" + m.getFilename() + m.getFiletype() + "): Security Exception");
            } catch (IOException exc) {
                log.error("IO error", exc);
                m.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, m.getMetaInfoSearchValue() + ": IO Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + m.getMetaInfoSearchValue() + " (" + m.getFilename() + m.getFiletype() + "): IO Exception");
            } catch (Exception exc) {
                log.error("Allgemeiner Fehler", exc);
                m.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, m.getMetaInfoSearchValue() + ": Generic Exception"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: " + m.getMetaInfoSearchValue() + ": Generic Exception");
            }
        } else {
            log.error("MPlayer directory " + System.getProperty("user.dir") + "/tools/" + " not found.");
            m.addWarning(new Warning(UMCConstants.STATUS_MPLAYER_ERROR, "Verzeichniss " + System.getProperty("user.dir") + "/tools/" + " wurde nicht gefunden."));
            if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning("MediaInfo: Tools directory " + System.getProperty("user.dir") + "/tools/" + " not found.");
        }
    }

    private void assignImages(Object job) {
        String searchFor = "";
        if (job instanceof Movie) {
            searchFor = ((Movie) job).getFilename();
        } else if (job instanceof MovieGroup) {
            searchFor = ((MovieGroup) job).getComputedTitel();
        } else if (job instanceof SeriesGroup) {
            searchFor = ((SeriesGroup) job).getMetaInfoSearchValue();
        } else if (job instanceof Music) {
            searchFor = ((Music) job).getMetaInfoSearchValue();
        }
        if (UMCConstants.debug) log.debug("BW" + identifier + ": searching images in the publisher with key '" + searchFor + "'");
        Map<String, ImageFile> images = Publisher.getInstance().getMovieImagefiles(searchFor);
        if (images != null) {
            if (job instanceof Movie) {
                Movie m = (Movie) job;
                if (m.getCover() == null && images.containsKey(Publisher.KEY_COVER)) {
                    if (UMCConstants.debug) log.debug("Local cover has been found and assigned");
                    m.setCover((CoverImage) images.get(Publisher.KEY_COVER));
                }
                if (m.getBackdrop() == null && images.containsKey(Publisher.KEY_BACKDROP)) {
                    if (UMCConstants.debug) log.debug("Local backdrop has been found and assigned");
                    m.setBackdrop((BackdropImage) images.get(Publisher.KEY_BACKDROP));
                }
            } else if (job instanceof MovieGroup) {
                MovieGroup mg = (MovieGroup) job;
                if (mg.getCover() == null && images.containsKey(Publisher.KEY_COVER)) {
                    if (UMCConstants.debug) log.debug("Local cover has been found and assigned");
                    mg.setCover((CoverImage) images.get(Publisher.KEY_COVER));
                }
                if (mg.getBackdrop() == null && images.containsKey(Publisher.KEY_BACKDROP)) {
                    if (UMCConstants.debug) log.debug("Local backdrop has been found and assigned");
                    mg.setBackdrop((BackdropImage) images.get(Publisher.KEY_BACKDROP));
                }
            } else if (job instanceof SeriesGroup) {
                SeriesGroup sg = (SeriesGroup) job;
                if (sg.getBanner() == null && images.containsKey(Publisher.KEY_BANNER)) {
                    if (UMCConstants.debug) log.debug("Local banner has been found and assigned");
                    sg.setBanner((BannerImage) images.get(Publisher.KEY_BANNER));
                }
                if (sg.getCover() == null && images.containsKey(Publisher.KEY_COVER)) {
                    if (UMCConstants.debug) log.debug("Local cover has been found and assigned");
                    sg.setCover((CoverImage) images.get(Publisher.KEY_COVER));
                }
                if (sg.getBackdrop() == null && images.containsKey(Publisher.KEY_BACKDROP)) {
                    if (UMCConstants.debug) log.debug("Local backdrop has been found and assigned");
                    sg.setBackdrop((BackdropImage) images.get(Publisher.KEY_BACKDROP));
                }
            } else if (job instanceof Music) {
                Music m = (Music) job;
                if (m.getCover() == null && images.containsKey(Publisher.KEY_COVER)) {
                    if (UMCConstants.debug) log.debug("Local cover has been found and assigned");
                    m.setCover((CoverImage) images.get(Publisher.KEY_COVER));
                }
                if (m.getBackdrop() == null && images.containsKey(Publisher.KEY_BACKDROP)) {
                    if (UMCConstants.debug) log.debug("Local backdrop has been found and assigned");
                    m.setBackdrop((BackdropImage) images.get(Publisher.KEY_BACKDROP));
                }
            }
        } else {
            if (job instanceof MovieGroup) {
                if (UMCConstants.debug) log.debug("no local cover for the moviegroup has been found, checking child covers");
                MovieGroup mg = (MovieGroup) job;
                for (Movie child : mg.getChilds()) {
                    if (UMCConstants.debug) log.debug("checking child: " + child.getFilename());
                    images = Publisher.getInstance().getMovieImagefiles(child.getFilename());
                    if (images != null && images.containsKey(Publisher.KEY_COVER)) {
                        if (UMCConstants.debug) log.debug("Local child cover has been found and assigned to the group");
                        mg.setCover((CoverImage) images.get(Publisher.KEY_COVER));
                    }
                    if (images != null && images.containsKey(Publisher.KEY_BACKDROP)) {
                        if (UMCConstants.debug) log.debug("Local child backdrop has been found and assigned to the group");
                        mg.setBackdrop((BackdropImage) images.get(Publisher.KEY_BACKDROP));
                    }
                }
            } else {
                if (UMCConstants.debug) log.debug("No local images found");
            }
        }
    }

    /**
	 * Will search trailer files for the given {@link Movie}.
	 * A trailer file must be named extactly as the movie file itself
	 * and additional with the extension [Trailer] or [Trailer1-n].
	 * Trailer files will be only searched in origin directory of the given {@link Movie}
	 * 
	 * @param m
	 */
    private void assignTrailer(Movie movie) {
        if (UMCConstants.debug) log.debug("searching trailer files in the publisher with key '" + movie.getFilename() + "'");
        if (StringUtils.isNotEmpty(movie.getFilename()) && Publisher.getInstance().getAllTrailer().containsKey(movie.getFilename())) {
            for (String trailerPath : Publisher.getInstance().getAllTrailer().get(movie.getFilename())) {
                Trailer trailer = new Trailer();
                trailer.setIdDir(Publisher.getInstance().getID_DIR(trailerPath, UMCConstants.SCAN_TYPE_MOVIES));
                if (UMCConstants.debug) log.debug("[ID DIR]\t\t\t\t" + trailer.getIdDir());
                trailer.setScanType(Publisher.getInstance().getScanType(trailer.getIdDir()));
                if (UMCConstants.debug) log.debug("[ScanType]\t\t\t\t" + trailer.getScanType());
                File f = new File(trailerPath);
                trailer.setFilename(f.getName().substring(0, f.getName().lastIndexOf(".")));
                trailer.setFiletype(f.getName().substring(f.getName().lastIndexOf("."), f.getName().length()));
                if (UMCConstants.debug) log.debug("[Filename]\t\t\t\t" + trailer.getFilename());
                if (UMCConstants.debug) log.debug("[Filetype]\t\t\t\t" + trailer.getFiletype());
                String scanDir = Publisher.getInstance().getDirForIdDir(trailer.getIdDir(), UMCConstants.SCAN_TYPE_MOVIES, true);
                trailer.setScanDir(scanDir);
                if (UMCConstants.debug) log.debug("[ScanDir]\t\t\t\t" + trailer.getScanDir());
                String path = trailerPath.substring(trailer.getScanDir().length(), trailerPath.lastIndexOf(UMCConstants.fileSeparator));
                trailer.setPath(path);
                if (UMCConstants.debug) log.debug("[Path]\t\t\t\t\t" + trailer.getPath());
                String nmtPathRoot = Publisher.getInstance().getDirForIdDir(trailer.getIdDir(), trailer.getScanType(), false);
                String nmtPathFull = nmtPathRoot + trailer.getPath();
                nmtPathFull += "/" + trailer.getFilename() + trailer.getFiletype();
                trailer.setNMTRootPath(nmtPathRoot);
                trailer.setNMTFullPath(nmtPathFull);
                if (UMCConstants.debug) {
                    log.debug("[NMT Root Path]\t\t\t\t" + trailer.getNMTRootPath());
                    log.debug("[NMT Full Path]\t\t\t\t" + trailer.getNMTFullPath());
                }
                movie.addTrailer(trailer);
                if (UMCConstants.debug) log.debug("Local trailer has been found and assigned");
            }
        }
    }

    /**
	 * Ermittelt Cover,Poster,Backdrop und Schauspieler-Bilder für ein übergebenes MovieFile Objekt.
	 *
	 * @param mf
	 */
    private void getMovieFanart(com.umc.beans.media.Movie mf) {
        if (mf.getCover() == null || mf.getBackdrop() == null) {
            if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving online fanart for " + mf.getMetaInfoSearchValue());
        } else {
            if (UMCConstants.debug) log.debug("BW" + identifier + ": Local images has been assigned -> retreiving online fanart not needed for " + mf.getMetaInfoSearchValue());
            return;
        }
        try {
            PluginController.getMovieFanartOnline(mf, log);
            String filenamePrefix = mf.getFilename();
            if (mf.isVideoTS() || mf.isBDMV()) {
                filenamePrefix = mf.getMetaInfoSearchValue();
            }
            if (mf.getCover() == null) {
                log.info("[Cover]");
                BufferedImage img = null;
                for (String coverURL : mf.getCoverURLs()) {
                    img = imgUtils.loadImageFromURL(coverURL);
                    if (img != null) break;
                }
                if (img != null) {
                    imgUtils.saveImage(img, System.getProperty("user.dir") + "/resources/Cover/Movies" + UMCConstants.fileSeparator + filenamePrefix + Publisher.getInstance().getParamCoverFilenameExtension() + ".jpg");
                    img = null;
                    movieCover = new CoverImage();
                    movieCover.setAbsolutePath(System.getProperty("user.dir") + "/resources/Cover/Movies" + UMCConstants.fileSeparator + filenamePrefix + Publisher.getInstance().getParamCoverFilenameExtension() + ".jpg");
                    movieCover.setFinished(false);
                    mf.setCover(movieCover);
                } else {
                    UMCStatistics.getInstance().addMovieNoCoverFoundOnline();
                    log.error("Cover could not be loaded from any found url. Maybe targets are not available.");
                    mf.addWarning(new Warning(UMCConstants.STATUS_COVER_NOT_FOUND, "Cover for " + mf.getMetaInfoSearchValue() + " could not be loaded from any url -> plugin returned no url's"));
                    if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(mf.getMetaInfoSearchValue() + ": cover could not be loaded from any found url -> plugin returned no url's");
                }
            }
            if (mf.getBackdrop() == null) {
                log.info("[Backdrop]");
                BufferedImage img = null;
                for (String coverURL : mf.getBackdropURLs()) {
                    img = imgUtils.loadImageFromURL(coverURL, UMCConstants.RESOLUTION_HD);
                    if (img != null) break;
                }
                if (img != null) {
                    imgUtils.saveJPEGImage(img, System.getProperty("user.dir") + "/resources/Backdrops/Movies" + UMCConstants.fileSeparator + filenamePrefix + Publisher.getInstance().getParamBackdropFilenameExtension());
                    img = null;
                    movieBackdrop = new BackdropImage();
                    movieBackdrop.setAbsolutePath(System.getProperty("user.dir") + "/resources/Backdrops/Movies" + UMCConstants.fileSeparator + filenamePrefix + Publisher.getInstance().getParamBackdropFilenameExtension() + ".jpg");
                    movieBackdrop.setFinished(false);
                    mf.setBackdrop(movieBackdrop);
                } else {
                    UMCStatistics.getInstance().addMovieNoBackdropFoundOnline();
                    log.error("Backdrop could not be loaded from any found url. Maybe targets are not available.");
                    mf.addWarning(new Warning(UMCConstants.STATUS_BACKDROP_NOT_FOUND, "Backdrop for " + mf.getMetaInfoSearchValue() + " could not be loaded from any url -> plugin returned no url's"));
                    if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(mf.getMetaInfoSearchValue() + ": backdrop could not be loaded from any found url -> plugin returned no url's");
                }
            }
        } catch (Exception exc) {
            log.error("BW" + identifier + ": Could not retreive online fanart for " + mf.getMetaInfoSearchValue(), exc);
        }
    }

    /**
	 * Ermittelt Cover,Poster,Backdrop und Schauspieler-Bilder für ein übergebenes Serien Objekt.
	 *
	 * @param mf
	 */
    private void getSeriesFanart(SeriesGroup sg) {
        if (sg.getBanner() == null || sg.getCover() == null || sg.getBackdrop() == null) {
            if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving online fanart for " + sg.getMetaInfoSearchValue());
        } else {
            if (UMCConstants.debug) log.debug("BW" + identifier + ": Local images has been assigned -> retreiving online fanart not needed for " + sg.getMetaInfoSearchValue());
            return;
        }
        if (sg.getBanner() == null) {
            log.info("[Banner]");
            BufferedImage img = null;
            for (String bannerURL : sg.getBannerURLs()) {
                img = imgUtils.loadImageFromURL(bannerURL);
                if (img != null) break;
            }
            if (img != null) {
                imgUtils.saveImage(img, System.getProperty("user.dir") + "/resources/Banner/Series" + UMCConstants.fileSeparator + sg.getMetaInfoSearchValue() + Publisher.getInstance().getParamBannerFilenameExtension() + ".jpg");
                img = null;
                movieBanner = new BannerImage();
                movieBanner.setAbsolutePath(System.getProperty("user.dir") + "/resources/Banner/Series" + UMCConstants.fileSeparator + sg.getMetaInfoSearchValue() + Publisher.getInstance().getParamBannerFilenameExtension() + ".jpg");
                movieBanner.setFinished(false);
                sg.setBanner(movieBanner);
            } else {
                log.error("Banner could not be loaded from any found url. Maybe targets are not available.");
                sg.addWarning(new Warning(UMCConstants.STATUS_BANNER_NOT_FOUND, "Banner for " + sg.getMetaInfoSearchValue() + " could not be loaded from any url -> plugin returned no url's"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(sg.getMetaInfoSearchValue() + ": banner could not be loaded from any found url -> plugin returned no url's");
            }
        }
        if (sg.getCover() == null) {
            log.info("[Cover]");
            BufferedImage img = null;
            for (String coverURL : sg.getPosterURLs()) {
                img = imgUtils.loadImageFromURL(coverURL);
                if (img != null) break;
            }
            if (img != null) {
                imgUtils.saveJPEGImage(img, System.getProperty("user.dir") + "/resources/Cover/Series" + UMCConstants.fileSeparator + sg.getMetaInfoSearchValue() + Publisher.getInstance().getParamCoverFilenameExtension());
                img = null;
                movieCover = new CoverImage();
                movieCover.setAbsolutePath(System.getProperty("user.dir") + "/resources/Cover/Series" + UMCConstants.fileSeparator + sg.getMetaInfoSearchValue() + Publisher.getInstance().getParamCoverFilenameExtension() + ".jpg");
                movieCover.setFinished(false);
                sg.setCover(movieCover);
            } else {
                log.error("Cover could not be loaded from any found url. Maybe targets are not available.");
                sg.addWarning(new Warning(UMCConstants.STATUS_COVER_NOT_FOUND, "Cover for " + sg.getMetaInfoSearchValue() + " could not be loaded from any url -> plugin returned no url's"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(sg.getMetaInfoSearchValue() + ": cover could not be loaded from any found url -> plugin returned no url's");
            }
        }
        if (sg.getBackdrop() == null) {
            log.info("[Backdrop]");
            BufferedImage img = null;
            for (String coverURL : sg.getBackdropURLs()) {
                img = imgUtils.loadImageFromURL(coverURL, UMCConstants.RESOLUTION_HD);
                if (img != null) break;
            }
            if (img != null) {
                imgUtils.saveJPEGImage(img, System.getProperty("user.dir") + "/resources/Backdrops/Series" + UMCConstants.fileSeparator + sg.getMetaInfoSearchValue() + Publisher.getInstance().getParamBackdropFilenameExtension());
                img = null;
                movieBackdrop = new BackdropImage();
                movieBackdrop.setAbsolutePath(System.getProperty("user.dir") + "/resources/Backdrops/Series" + UMCConstants.fileSeparator + sg.getMetaInfoSearchValue() + Publisher.getInstance().getParamBackdropFilenameExtension() + ".jpg");
                movieBackdrop.setFinished(false);
                sg.setBackdrop(movieBackdrop);
            } else {
                log.error("Baackdrop could not be loaded from any found url. Maybe targets are not available.");
                sg.addWarning(new Warning(UMCConstants.STATUS_BACKDROP_NOT_FOUND, "Backdrop for " + sg.getMetaInfoSearchValue() + " could not be loaded from any url -> plugin returned no url's"));
                if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(sg.getMetaInfoSearchValue() + ": backdrop could not be loaded from any found url -> plugin returned no url's");
            }
        }
    }

    /**
	 * Ermittelt Episoden Bilder für ein übergebenes Episoden Objekt.
	 *
	 * @param mf
	 */
    private void getEpisodeImages(SeriesGroup sg) {
        if (UMCConstants.debug) log.debug("BW" + identifier + ": Retreiving online episode images for tv show " + sg.getMetaInfoSearchValue());
        log.info("[Episode Images]");
        BufferedImage img = null;
        for (Season season : sg.getSeasons().values()) {
            for (Episode episode : season.getEpisodes().values()) {
                Map<String, ImageFile> images = Publisher.getInstance().getMovieImagefiles(episode.getMetaInfoSearchValue());
                if (StringUtils.isNotEmpty(episode.getScreenshotURL()) && images == null) {
                    img = imgUtils.loadImageFromURL(episode.getScreenshotURL());
                    if (img != null) {
                        imgUtils.saveImage(img, System.getProperty("user.dir") + "/resources/Cover/Episodes" + UMCConstants.fileSeparator + episode.getMetaInfoSearchValue() + Publisher.getInstance().getParamCoverFilenameExtension() + ".jpg");
                        img = null;
                        movieCover = new CoverImage();
                        movieCover.setAbsolutePath(System.getProperty("user.dir") + "/resources/Cover/Episodes" + UMCConstants.fileSeparator + episode.getMetaInfoSearchValue() + Publisher.getInstance().getParamCoverFilenameExtension() + ".jpg");
                        movieCover.setFinished(false);
                        episode.setCover(movieCover);
                    } else {
                        log.error("Episode image could not be loaded from any found url. Maybe target is not available.");
                        episode.addWarning(new Warning(UMCConstants.STATUS_BANNER_NOT_FOUND, "Episode image for " + episode.getMetaInfoSearchValue() + " could not be loaded from any url -> plugin returned no url"));
                        if (UMCConstants.guiMode) GuiController.getInstance().getScanStatusPanel().getPanelBottom().addWarning(episode.getMetaInfoSearchValue() + ": episode image could not be loaded from any found url -> plugin returned no url");
                    }
                } else {
                    episode.setCover((CoverImage) images.get(Publisher.KEY_COVER));
                }
            }
        }
    }

    /**
	 * Reads the ID3 information from a given MP3.
	 * 
	 * @param m {@link Music} bean representing an MP3 file
	 * @throws Exception
	 */
    private void readID3FromFile(Music m, Logger log) throws Exception {
        MediaFile oMediaFile = new MP3File(new File(m.getPCPath()));
        ID3Tag[] aoID3Tag = oMediaFile.getTags();
        for (int i = 0; i < aoID3Tag.length; i++) {
            if (aoID3Tag[i] instanceof ID3V1_0Tag) {
                readID3V1_0(m, aoID3Tag[i], log);
            } else if (aoID3Tag[i] instanceof ID3V2_3_0Tag) {
                readID3V2_3(m, aoID3Tag[i], log);
            }
        }
    }

    /**
	 * Reads ID3 from a v1.0 ID3 tag.
	 * 
	 * @param m {@link Music} bean representing an MP3 file
	 * @param tag
	 */
    private void readID3V1_0(Music m, ID3Tag tag, Logger log) {
        ID3V1_0Tag oID3V1_0Tag = (ID3V1_0Tag) tag;
        log.debug("readID3V1_0 = > Title = " + oID3V1_0Tag.getTitle());
        if (StringUtils.isNotEmpty(oID3V1_0Tag.getTitle())) m.setTitle(oID3V1_0Tag.getTitle());
        if (StringUtils.isNotEmpty(oID3V1_0Tag.getTitle())) m.setAlbum(oID3V1_0Tag.getAlbum());
        if (StringUtils.isNotEmpty(oID3V1_0Tag.getArtist())) m.setAlbum(oID3V1_0Tag.getArtist());
        if (StringUtils.isNotEmpty(oID3V1_0Tag.getTitle())) m.setYear(oID3V1_0Tag.getYear());
        if (StringUtils.isNotEmpty(oID3V1_0Tag.getTitle())) m.setGenre(oID3V1_0Tag.getGenre().toString());
    }

    /**
	 * Reads ID3 from a v2.3 ID3 tag.
	 * 
	 * @param m {@link Music} bean representing an MP3 file
	 * @param tag
	 */
    private void readID3V2_3(Music m, ID3Tag tag, Logger log) {
        ID3V2_3_0Tag oID3V2_3_0Tag = (ID3V2_3_0Tag) tag;
        log.debug("readID3V2_3 = > Title = " + oID3V2_3_0Tag.getTitle());
        log.debug("readID3V2_3 = > Album = " + oID3V2_3_0Tag.getAlbum());
        log.debug("readID3V2_3 = > Genre = " + oID3V2_3_0Tag.getGenre());
        m.setTitle(oID3V2_3_0Tag.getTitle());
        m.setAlbum(oID3V2_3_0Tag.getAlbum());
        m.setArtist(oID3V2_3_0Tag.getArtist());
        try {
            log.debug("readID3V2_3 = > Year = " + oID3V2_3_0Tag.getYear());
            m.setYear(oID3V2_3_0Tag.getYear() + "");
        } catch (ID3Exception e) {
            log.error("readID3V2_3 = > Year = not set");
        }
    }

    /**
	 * Ermittelt die Meta-Daten die im Exif Format gespeichert sind(JPEG oder TIFF).
	 */
    private void readExifFromFile(Photo p) {
        try {
            if (UMCConstants.debug) log.debug("Reading exif data from " + p.getPCPath());
            IImageMetadata metadata = Sanselan.getMetadata(new File(p.getPCPath()));
            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                if (UMCConstants.debug) {
                    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_XRESOLUTION);
                    printTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_CREATE_DATE);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_ISO);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_APERTURE_VALUE);
                    printTagValue(jpegMetadata, TiffConstants.EXIF_TAG_BRIGHTNESS_VALUE);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE_REF);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE_REF);
                    printTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE);
                    System.out.println();
                }
                StringBuilder sb = new StringBuilder();
                sb.append(getTagValue(jpegMetadata, TiffConstants.TIFF_TAG_XRESOLUTION) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.TIFF_TAG_DATE_TIME) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.EXIF_TAG_CREATE_DATE) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.EXIF_TAG_ISO) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.EXIF_TAG_SHUTTER_SPEED_VALUE) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.EXIF_TAG_APERTURE_VALUE) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.EXIF_TAG_BRIGHTNESS_VALUE) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE_REF) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LATITUDE) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE_REF) + "\n");
                sb.append(getTagValue(jpegMetadata, TiffConstants.GPS_TAG_GPS_LONGITUDE) + "\n");
                p.setExifInfo(sb.toString());
            }
        } catch (IOException exc) {
            log.error("IO error while trying to read photo exif data", exc);
        } catch (ImageReadException exc) {
            log.error("Image read error while trying to read photo exif data", exc);
        }
    }

    private void printTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) throws ImageReadException, IOException {
        TiffField field = jpegMetadata.findEXIFValue(tagInfo);
        if (field == null) log.debug(tagInfo.name + ": " + "Not Found."); else log.debug(tagInfo.name + ": " + field.getValueDescription());
    }

    private String getTagValue(JpegImageMetadata jpegMetadata, TagInfo tagInfo) throws ImageReadException, IOException {
        String result = "";
        TiffField field = jpegMetadata.findEXIFValue(tagInfo);
        if (field == null) result += tagInfo.name + ": " + "Not Found."; else result += tagInfo.name + ": " + field.getValueDescription();
        return result;
    }
}
