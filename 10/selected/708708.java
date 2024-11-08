package net.sourceforge.x360mediaserve.database.backends.hibernate;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.x360mediaserve.api.database.InvalidItemException;
import net.sourceforge.x360mediaserve.api.database.ItemNotFoundException;
import net.sourceforge.x360mediaserve.api.database.MediaDatabase;
import net.sourceforge.x360mediaserve.api.database.QueryResult;
import net.sourceforge.x360mediaserve.api.database.SortCriterion;
import net.sourceforge.x360mediaserve.api.database.items.Item;
import net.sourceforge.x360mediaserve.api.database.items.container.AlbumItem;
import net.sourceforge.x360mediaserve.api.database.items.container.ArtistItem;
import net.sourceforge.x360mediaserve.api.database.items.container.AudioGenreItem;
import net.sourceforge.x360mediaserve.api.database.items.container.PlaylistItem;
import net.sourceforge.x360mediaserve.api.database.items.container.TVSeasonItem;
import net.sourceforge.x360mediaserve.api.database.items.container.TVShowItem;
import net.sourceforge.x360mediaserve.api.database.items.media.AudioItem;
import net.sourceforge.x360mediaserve.api.database.items.media.ImageItem;
import net.sourceforge.x360mediaserve.api.database.items.media.MediaItem;
import net.sourceforge.x360mediaserve.api.database.items.media.VideoItem;
import net.sourceforge.x360mediaserve.api.database.searching.SearchExp;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.AlbumDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.ArtistDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.AudioGenreDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.AudioItemDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.AudioPlaylistDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.GenericDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.GenericMediaContainerDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.ImageItemDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.TVEpisodeDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.TVSeasonDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.dao.TVShowDAO;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.HibernateItem;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.containers.HibernateMediaContainer;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.containers.music.HibernateAlbum;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.containers.music.HibernateArtist;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.containers.music.HibernateAudioPlaylist;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.containers.music.HibernateGenre;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.containers.video.HibernateTVSeason;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.containers.video.HibernateTVShow;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.media.HibernateMediaItem;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.media.audio.HibernateAudioItem;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.media.image.HibernateImageItem;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.media.video.HibernateTVEpisodeItem;
import net.sourceforge.x360mediaserve.database.backends.hibernate.items.media.video.HibernateVideoItem;
import org.hibernate.Criteria;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MediaDatabase using hibernate as a backend
 * 
 * @author Tom
 * 
 */
public class HibernateDB implements MediaDatabase {

    protected static Logger logger = LoggerFactory.getLogger(HibernateDB.class);

    private String NO_ARTIST = "No Artist";

    private String NO_ALBUM = "No Album";

    private String NO_GENRE = "No Genre";

    private static final int BATCH_SIZE = 15;

    private final String DB_PATH = "hibernateDB";

    HibernateManager manager;

    public HibernateDAOFactory factory;

    public HibernateDB() {
        super();
        manager = new HibernateManager();
        factory = new HibernateDAOFactory(manager);
    }

    public HibernateDB(File basePath) {
        super();
        File dbPath = null;
        if (basePath != null) {
            dbPath = new File(basePath.getPath() + File.separatorChar + DB_PATH);
        }
        logger.info("Using db path:{}", dbPath);
        manager = new HibernateManager(dbPath);
        factory = new HibernateDAOFactory(manager);
    }

    public HibernateDB(HibernateDAOFactory factory) {
        this.factory = factory;
    }

    public HibernateDB(HibernateManager manager) {
        super();
        this.manager = manager;
        factory = new HibernateDAOFactory(manager);
    }

    public <T extends HibernateItem> T nonTransactionAddItem(T item, GenericDAO<T, ? extends Serializable> dao) {
        Transaction transaction = dao.beginTransaction();
        try {
            item = dao.save(item);
            dao.commitTransaction();
        } catch (Exception e) {
            transaction.rollback();
        }
        return item;
    }

    public HibernateAlbum addAlbum(String albumName) {
        HibernateAlbum album = new HibernateAlbum();
        album.setName(albumName);
        album = nonTransactionAddItem(album, factory.getAlbumDAO());
        return album;
    }

    public void addAlbumToArtist(AlbumItem albumRef, ArtistItem artistRef) {
        if (albumRef instanceof HibernateAlbum && artistRef instanceof HibernateArtist) {
            HibernateAlbum album = (HibernateAlbum) albumRef;
            HibernateArtist artist = (HibernateArtist) artistRef;
            AlbumDAO albumDAO = factory.getAlbumDAO();
            ArtistDAO artistDAO = factory.getArtistDAO();
            Transaction t = albumDAO.beginTransaction();
            try {
                album.setArtist(artist, true);
                albumDAO.update(album);
                albumDAO.commitTransaction();
            } catch (Exception e) {
                logger.error("Error adding album to artist", e);
                t.rollback();
            }
        }
    }

    public HibernateArtist addArtist(String artistString) {
        HibernateArtist artist = new HibernateArtist();
        artist.setName(artistString);
        artist = nonTransactionAddItem(artist, factory.getArtistDAO());
        return artist;
    }

    public AudioItem addAudio(AudioItem audio) {
        logger.debug("Adding audio");
        AudioItemDAO audioItemDAO = factory.getAudioItemDAO();
        Transaction transaction = audioItemDAO.beginTransaction();
        try {
            List<HibernateAudioItem> items = audioItemDAO.findByLocation(audio.getFirstResource().getLocation());
            AudioItem newItem;
            if (items != null && items.size() > 0) {
                newItem = items.get(0);
            } else {
                newItem = addExternalAudioItem(audio, audioItemDAO);
            }
            audioItemDAO.commitTransaction();
            return newItem;
        } catch (Exception e) {
            transaction.rollback();
            logger.error("Error:", e);
        }
        return null;
    }

    public HibernateGenre addAudioGenre(String genreString) {
        HibernateGenre genre = new HibernateGenre();
        genre.setName(genreString);
        genre = nonTransactionAddItem(genre, factory.getAudioGenreDAO());
        return genre;
    }

    private HibernateAudioItem addExternalAudioItem(AudioItem audio, AudioItemDAO audioItemDAO) {
        HibernateAudioItem audioItem = new HibernateAudioItem(audio);
        String artistName = audio.getArtistName();
        if (artistName == null || artistName.length() == 0) {
            artistName = NO_ARTIST;
        }
        HibernateArtist artist = findOrCreateArtistByString(artistName.trim());
        audioItem.setArtist(artist, true);
        String albumName = audio.getAlbumName();
        if (albumName == null || albumName.length() == 0) {
            albumName = NO_ALBUM;
        }
        HibernateAlbum album = findOrCreateAlbumByString(albumName.trim());
        album.setArtist(artist, true);
        audioItem.setAlbum(album, true);
        String genreName = audio.getGenreName();
        if (genreName == null || genreName.length() == 0) {
            genreName = NO_GENRE;
        }
        HibernateGenre genre = findOrCreateGenreByString(genreName.trim());
        audioItem.setGenre(genre, true);
        factory.getAlbumDAO().save(album);
        audioItemDAO.save(audioItem);
        return audioItem;
    }

    public ImageItem addImage(ImageItem image) {
        HibernateImageItem imageItem = new HibernateImageItem(image);
        imageItem = nonTransactionAddItem(imageItem, factory.getImageItemDAO());
        return imageItem;
    }

    public HibernateAudioPlaylist addPlaylist(String playlistName) {
        HibernateAudioPlaylist playlist = new HibernateAudioPlaylist();
        playlist.setName(playlistName);
        playlist = nonTransactionAddItem(playlist, factory.getAudioPlaylistDAO());
        return playlist;
    }

    public void addTrackToPlaylist(PlaylistItem playlistItem, AudioItem item) {
        logger.debug("Adding track to playlist");
        if (playlistItem instanceof HibernateAudioPlaylist && item instanceof HibernateAudioItem) {
            logger.debug("Adding track {} to playlist {}", item.getName(), playlistItem.getName());
            Long playlistId = (Long) playlistItem.getRef();
            Long audioId = (Long) item.getRef();
            AudioPlaylistDAO playlistDAO = factory.getAudioPlaylistDAO();
            AudioItemDAO audioDAO = factory.getAudioItemDAO();
            playlistDAO.beginTransaction();
            HibernateAudioPlaylist playlist = playlistDAO.load(playlistId);
            try {
                HibernateAudioItem audio = audioDAO.load(audioId);
                if (playlist != null && audio != null) {
                    playlist.addItem(audio);
                    playlistDAO.update(playlist);
                    logger.debug("Track added");
                } else {
                    logger.error("Problem loading audio and playlist");
                }
                playlistDAO.commitTransaction();
            } catch (Exception e) {
                logger.error("Error adding item {} to playlist {}", item.getId(), playlist.getDbId());
                playlistDAO.getSession().getTransaction().rollback();
            }
        } else {
            logger.error("Error adding item {} to playlist {}", item.getId(), playlistItem.getId());
        }
    }

    public HibernateTVShow addTVShow(TVShowItem show) {
        HibernateTVShow tvShow = new HibernateTVShow(show);
        tvShow = nonTransactionAddItem(tvShow, factory.getTVShowDAO());
        return tvShow;
    }

    public VideoItem addVideo(VideoItem video) {
        TVShowDAO showDAO = factory.getTVShowDAO();
        TVSeasonDAO seasonDAO = factory.getTVSeasonDAO();
        TVEpisodeDAO episodeDAO = factory.getTVEpisodeDAO();
        HibernateTVEpisodeItem item = new HibernateTVEpisodeItem(video);
        HibernateTVShow show = null;
        HibernateTVSeason season = null;
        episodeDAO.beginTransaction();
        if (video.getShowName() != null && video.getShowName().length() > 0) {
            show = findOrCreateTVShowItembyName(video.getShowName(), showDAO);
        }
        if (show != null && video.getSeasonNumber() != null) {
            season = findOrCreateTVSeason(show, video.getSeasonNumber(), seasonDAO);
            season = seasonDAO.load(season.getDbId());
        }
        if (show != null) item.setTVShow(show, true);
        if (season != null) item.setTVSeason(season, true);
        if (show != null && season != null) {
            logger.debug("Adding TV Episode with db ids: {} {}", item.getTvShow().getDbId(), item.getTvSeason().getDbId());
        }
        episodeDAO.save(item);
        episodeDAO.commitTransaction();
        return item;
    }

    private HibernateTVSeason findOrCreateTVSeason(HibernateTVShow show, int season, TVSeasonDAO dao) {
        List<HibernateTVSeason> seasons = dao.findSeasonForShow(show, season);
        if (seasons != null && seasons.size() > 0) {
            return seasons.get(0);
        } else {
            HibernateTVSeason result = new HibernateTVSeason();
            result.setSeasonNumber(season);
            result.setTVShow(show, true);
            dao.save(result);
            return result;
        }
    }

    private HibernateTVShow findOrCreateTVShowItembyName(String name, TVShowDAO dao) {
        List<HibernateTVShow> shows = dao.findByName(name);
        if (shows != null && shows.size() > 0) {
            return shows.get(0);
        } else {
            HibernateTVShow show = new HibernateTVShow();
            show.setName(name);
            dao.save(show);
            return show;
        }
    }

    public AlbumItem findAlbumByName(String albumName) {
        return nonTransactionFindItemByName(albumName, factory.getAlbumDAO());
    }

    public ArtistItem findArtistByName(String artistName) {
        return nonTransactionFindItemByName(artistName, factory.getArtistDAO());
    }

    public AudioGenreItem findAudioGenreByName(String genreName) {
        return nonTransactionFindItemByName(genreName, factory.getAudioGenreDAO());
    }

    private <T extends HibernateItem> T findOrCreateByName(T item, GenericDAO<T, Serializable> dao) {
        T result = null;
        dao.beginTransaction();
        List<T> items = dao.findByName(item.getName());
        if (items != null && items.size() > 0) {
            result = items.get(0);
        } else {
            result = dao.save(item);
        }
        return result;
    }

    private HibernateAlbum findOrCreateAlbumByString(String albumString) {
        HibernateAlbum result = null;
        AlbumDAO dao = factory.getAlbumDAO();
        List<HibernateAlbum> albums = dao.findByName(albumString);
        if (albums != null && albums.size() > 0) {
            result = albums.get(0);
        } else {
            result = new HibernateAlbum();
            result.setName(albumString);
            logger.debug("Creating album:{}", albumString);
            dao.save(result);
        }
        return result;
    }

    private HibernateArtist findOrCreateArtistByString(String artistString) {
        HibernateArtist result = null;
        ArtistDAO dao = factory.getArtistDAO();
        List<HibernateArtist> albums = dao.findByName(artistString);
        if (albums != null && albums.size() > 0) {
            result = albums.get(0);
        } else {
            result = new HibernateArtist();
            result.setName(artistString);
            logger.debug("Creating artist:{}", artistString);
            dao.save(result);
        }
        return result;
    }

    private HibernateGenre findOrCreateGenreByString(String genreString) {
        HibernateGenre result = null;
        AudioGenreDAO dao = factory.getAudioGenreDAO();
        List<HibernateGenre> genres = dao.findByName(genreString);
        if (genres != null && genres.size() > 0) {
            result = genres.get(0);
        } else {
            result = new HibernateGenre();
            result.setName(genreString);
            logger.info("Creating genre:{}", genreString);
            dao.save(result);
        }
        return result;
    }

    public PlaylistItem findPlaylistByName(String name) {
        return nonTransactionFindItemByName(name, factory.getAudioPlaylistDAO());
    }

    public TVShowItem findTVShowByName(String name) {
        return nonTransactionFindItemByName(name, factory.getTVShowDAO());
    }

    public HibernateAlbum getAlbumItemForID(String albumID) {
        return nonTransactionGetByID(albumID, factory.getAlbumDAO());
    }

    public List<HibernateAlbum> listAlbums(int offset, int limit, List<SortCriterion> sortCriteria) {
        logger.debug("Getting albums from {} limit {}", offset, limit);
        return getRange(offset, limit, sortCriteria, factory.getAlbumDAO());
    }

    public List<HibernateAlbum> listAlbumsForArtist(String artistID, List<SortCriterion> sortList) {
        AlbumDAO dao = factory.getAlbumDAO();
        dao.beginTransaction();
        Criteria criteria = dao.getSession().createCriteria(HibernateAlbum.class);
        criteria.add(Property.forName("artist.id").eq(getDBIdFromID(artistID)));
        if (sortList != null) HibernateUtils.addSortOrder(HibernateAlbum.class, criteria, sortList);
        List<HibernateAlbum> result = criteria.list();
        dao.commitTransaction();
        return result;
    }

    public List<HibernateAlbum> listAlbumsForArtistName(String artistString, List<SortCriterion> sortList) {
        AlbumDAO dao = factory.getAlbumDAO();
        dao.beginTransaction();
        Criteria criteria = dao.getSession().createCriteria(HibernateAlbum.class);
        criteria.createAlias("artist", "art");
        criteria.add(Restrictions.eq("art.name", artistString));
        if (sortList != null) HibernateUtils.addSortOrder(HibernateAlbum.class, criteria, sortList);
        List<HibernateAlbum> result = criteria.list();
        dao.commitTransaction();
        return result;
    }

    public HibernateArtist getArtistItemForID(String artistID) {
        logger.debug("Getting artist {}", artistID);
        return nonTransactionGetByID(artistID, factory.getArtistDAO());
    }

    public List<HibernateArtist> listArtists(int offset, int limit, List<SortCriterion> sortCriteria) {
        logger.debug("Getting artists from {} limit {}", offset, limit);
        return getRange(offset, limit, sortCriteria, factory.getArtistDAO());
    }

    public List<HibernateAudioItem> listAudio(int offset, int limit, List<SortCriterion> sortCriteria) {
        logger.debug("Getting audio from {} limit {}", offset, limit);
        return getRange(offset, limit, sortCriteria, factory.getAudioItemDAO());
    }

    private List<HibernateAudioItem> getAudioForAlbum(String albumId, int offset, int limit, List<SortCriterion> sortList) {
        return getSublist(getAudioForAlbum(albumId, sortList), offset, limit);
    }

    private List<HibernateAudioItem> getAudioForAlbum(String albumId, List<SortCriterion> sortList) {
        AudioItemDAO dao = factory.getAudioItemDAO();
        dao.beginTransaction();
        Criteria criteria = dao.getSession().createCriteria(HibernateAudioItem.class);
        criteria.add(Property.forName("album.id").eq(getDBIdFromID(albumId)));
        if (sortList != null) HibernateUtils.addSortOrder(HibernateAudioItem.class, criteria, sortList);
        List<HibernateAudioItem> result = criteria.list();
        dao.commitTransaction();
        return result;
    }

    private List<HibernateAudioItem> getAudioForPlaylist(String playlistId, List<SortCriterion> sortList) {
        AudioItemDAO dao = factory.getAudioItemDAO();
        dao.beginTransaction();
        Criteria criteria = dao.getSession().createCriteria(HibernateAudioItem.class);
        criteria.createAlias("playlist", "plist");
        criteria.add(Restrictions.eq("plist.id", getDBIdFromID(playlistId)));
        if (sortList != null) HibernateUtils.addSortOrder(HibernateAudioItem.class, criteria, sortList);
        List<HibernateAudioItem> result = criteria.list();
        dao.commitTransaction();
        return result;
    }

    private List<HibernateAudioItem> getAudioForArtist(String artistId, int offset, int limit, List<SortCriterion> sortList) {
        logger.debug("Getting audio for artist{}, offset {}, limit {}", new Object[] { artistId, offset, limit });
        return getSublist(getAudioForArtist(artistId, sortList), offset, limit);
    }

    private List<HibernateAudioItem> getAudioForPlaylist(String playlistId, int offset, int limit, List<SortCriterion> sortList) {
        logger.debug("Getting audio for artist{}, offset {}, limit {}", new Object[] { playlistId, offset, limit });
        return getSublist(getAudioForPlaylist(playlistId, sortList), offset, limit);
    }

    private List<HibernateAudioItem> getAudioForArtist(String artistID, List<SortCriterion> sortList) {
        logger.debug("Getting audio for artist {}", artistID);
        AudioItemDAO dao = factory.getAudioItemDAO();
        dao.beginTransaction();
        Criteria criteria = dao.getSession().createCriteria(HibernateAudioItem.class);
        criteria.add(Property.forName("artist.id").eq(getDBIdFromID(artistID)));
        if (sortList != null) HibernateUtils.addSortOrder(HibernateAlbum.class, criteria, sortList);
        List<HibernateAudioItem> result = criteria.list();
        dao.commitTransaction();
        return result;
    }

    public List<HibernateAudioItem> listAudioForContainer(String containerID, int offset, int limit, List<SortCriterion> sortCriteria) {
        logger.debug("Getting audio for container {}", containerID);
        HibernateItemPath containerType = getTypeFromId(containerID);
        switch(containerType) {
            case ALBUM:
                return getAudioForAlbum(containerID, offset, limit, sortCriteria);
            case ARTIST:
                return getAudioForArtist(containerID, offset, limit, sortCriteria);
            case AUDIOPLAYLIST:
                return nonTransactionGetContentForContainer(containerID, offset, limit, factory.getAudioPlaylistDAO());
            default:
                logger.error("Got request for audio for unsupported container:{}", containerID);
        }
        return new ArrayList<HibernateAudioItem>();
    }

    public HibernateAudioItem getAudioItemForID(String id) {
        logger.debug("Getting audioItem {}", id);
        return nonTransactionGetByID(id, factory.getAudioItemDAO());
    }

    public List<? extends HibernateMediaItem> listContentForContainer(String containerID, int offset, int limit, List<SortCriterion> sortCriteria) {
        logger.debug("Getting content for container {} from {} limit {}", new Object[] { containerID, offset, limit });
        HibernateItemPath containerType = getTypeFromId(containerID);
        switch(containerType) {
            case ALBUM:
                return getAudioForAlbum(containerID, offset, limit, sortCriteria);
            case ARTIST:
                return getAudioForArtist(containerID, offset, limit, sortCriteria);
            case AUDIOPLAYLIST:
                logger.debug("Returning contents of playlist");
                return getAudioForPlaylist(containerID, offset, limit, sortCriteria);
            case GENRE:
                logger.debug("Getting contents of genre");
                return nonTransactionGetByCriteria(offset, limit, factory.getAudioItemDAO(), sortCriteria, Restrictions.eq("genre.id", getDBIdFromID(containerID)));
            case TVSEASON:
                logger.debug("Getting contents of TV Season");
                return nonTransactionGetContentForContainer(containerID, offset, limit, factory.getTVSeasonDAO());
            default:
                logger.error("Got request for audio for unsupported container:{}", containerID);
        }
        return new ArrayList<HibernateMediaItem>();
    }

    @Deprecated
    public List<? extends HibernateMediaItem> listContentForContainer(String containerID, List<SortCriterion> sortCriteria) {
        logger.debug("Getting content for container {}", containerID);
        HibernateItemPath containerType = getTypeFromId(containerID);
        logger.debug("Got type:{}", containerType.name());
        switch(containerType) {
            case ALBUM:
                return getAudioForAlbum(containerID, sortCriteria);
            case ARTIST:
                return getAudioForArtist(containerID, sortCriteria);
            case AUDIOPLAYLIST:
                return nonTransactionGetContentForContainer(containerID, factory.getAudioPlaylistDAO());
            case GENRE:
                return nonTransactionGetByCriteria(factory.getAudioItemDAO(), sortCriteria, Restrictions.eq("genre.id", getDBIdFromID(containerID)));
            default:
                logger.error("Got request for audio for unsupported container:{}", containerID);
        }
        return new ArrayList<HibernateMediaItem>();
    }

    public Long getDBIdFromID(String Id) {
        return Long.parseLong(Id.split("/")[1]);
    }

    public HibernateGenre getGenreItemForID(String genreID) {
        return nonTransactionGetByID(genreID, factory.getAudioGenreDAO());
    }

    public List<HibernateGenre> listGenres(int offset, int limit, List<SortCriterion> sortCriteria) {
        logger.debug("Getting genres from {} limit {}", offset, limit);
        return getRange(offset, limit, sortCriteria, factory.getAudioGenreDAO());
    }

    public MediaItem getItemForFile(File file) {
        return null;
    }

    public HibernateItem getItemForID(String id) {
        HibernateItemPath type = getTypeFromId(id);
        if (type != null) switch(type) {
            case ALBUM:
                return getAlbumItemForID(id);
            case ARTIST:
                return getArtistItemForID(id);
            case AUDIOITEM:
                return getAudioItemForID(id);
            case AUDIOPLAYLIST:
                return getPlaylistItemForID(id);
            case GENRE:
                return getGenreItemForID(id);
            case IMAGEITEM:
                return getImageItemForID(id);
            case TVSEASON:
                return null;
            case TVSHOW:
                return null;
            case VIDEOITEM:
                return getVideoItemForID(id);
        }
        logger.error("Couldn't find item for ID:{}", id);
        return null;
    }

    @SuppressWarnings("unchecked")
    public Class getClassForID(String id) {
        HibernateItemPath path = getTypeFromId(id);
        return path.clazz;
    }

    public HibernateImageItem getImageItemForID(String id) {
        return nonTransactionGetByID(id, factory.getImageItemDAO());
    }

    public MediaItem getMediaForID(String id) {
        logger.debug("Getting media for id:{}", id);
        HibernateItemPath mediaType = getTypeFromId(id);
        switch(mediaType) {
            case AUDIOITEM:
                return getAudioItemForID(id);
            case VIDEOITEM:
                return getVideoItemForID(id);
            case IMAGEITEM:
                return getImageItemForID(id);
            default:
                logger.error("Got request for media Item of unsupported type:{}", id);
        }
        return null;
    }

    public int getNumberOfAlbums() {
        return nonTransactionGetTotal(factory.getAlbumDAO());
    }

    public int getNumberOfArtists() {
        return nonTransactionGetTotal(factory.getArtistDAO());
    }

    public int getNumberOfAudioItems() {
        return nonTransactionGetTotal(factory.getAudioItemDAO());
    }

    public int getNumberOfGenres() {
        return nonTransactionGetTotal(factory.getAudioGenreDAO());
    }

    public int getNumberOfItemsInContainer(String containerID) {
        HibernateItemPath containerType = getTypeFromId(containerID);
        switch(containerType) {
            case ALBUM:
                return getAudioForAlbum(containerID, null).size();
            case ARTIST:
                return getAudioForArtist(containerID, null).size();
            case AUDIOPLAYLIST:
                return getAudioForPlaylist(containerID, null).size();
            case TVSEASON:
                return listEpisodesForSeason(containerID, 0, 1000, null).size();
            default:
                logger.error("Got request for number of items for unsupported item:{}", containerID);
        }
        return -1;
    }

    public int getNumberOfPlaylists() {
        return nonTransactionGetTotal(factory.getAudioPlaylistDAO());
    }

    public int getNumberOfTVShows() {
        return nonTransactionGetTotal(factory.getTVShowDAO());
    }

    public int getNumberOfVideos() {
        return nonTransactionGetTotal(factory.getVideoItemDAO());
    }

    public HibernateAudioPlaylist getPlaylistItemForID(String playlistID) {
        logger.debug("Getting playlist {}", playlistID);
        return nonTransactionGetByID(playlistID, factory.getAudioPlaylistDAO());
    }

    public List<HibernateAudioPlaylist> listPlaylists(int offset, int limit, List<SortCriterion> sortCriteria) {
        return getRange(offset, limit, sortCriteria, factory.getAudioPlaylistDAO());
    }

    private <T extends HibernateItem> List<T> getRange(int offset, int limit, List<SortCriterion> sortCriteria, GenericDAO<T, Long> dao) {
        dao.beginTransaction();
        List<T> result = dao.getRange(offset, limit, sortCriteria);
        dao.commitTransaction();
        return result;
    }

    public List<HibernateTVSeason> listSeasonsForShow(String showID, int offset, int limit, List<SortCriterion> sortCriteria) {
        TVShowDAO dao = factory.getTVShowDAO();
        List<HibernateTVSeason> result;
        dao.beginTransaction();
        Long id = getDBIdFromID(showID);
        HibernateTVShow tvShow = dao.load(id);
        if (tvShow != null && tvShow.getTVSeasons() != null) {
            int size = tvShow.getTVSeasons().size();
            int max = offset + limit;
            result = tvShow.getTVSeasons().subList(offset < size ? offset : size, max < size ? max : size);
        } else {
            result = new ArrayList<HibernateTVSeason>(0);
        }
        dao.commitTransaction();
        return result;
    }

    private <T> List<T> getSublist(List<T> list, int offset, int limit) {
        int max = offset + limit;
        if (max > list.size()) {
            max = list.size();
        }
        return list.subList(offset, max);
    }

    public List<? extends TVShowItem> listTVShows(int offset, int limit, List<SortCriterion> sortCriteria) {
        return getRange(offset, limit, sortCriteria, factory.getTVShowDAO());
    }

    public HibernateItemPath getTypeFromId(String Id) {
        logger.debug("Getting type for id:{}", Id);
        String id = Id.split("/", 2)[0];
        logger.debug("Type path:{}", id);
        return HibernateItemPath.getType(id);
    }

    public List<? extends VideoItem> listVideo(int offset, int limit, List<SortCriterion> sortCriteria) {
        return getRange(offset, limit, sortCriteria, factory.getTVEpisodeDAO());
    }

    public HibernateTVEpisodeItem getVideoItemForID(String id) {
        HibernateTVEpisodeItem result = nonTransactionGetByID(id, factory.getTVEpisodeDAO());
        return result;
    }

    private <T extends HibernateItem> T nonTransactionFindItemByName(String name, GenericDAO<T, Long> dao) {
        T result = null;
        dao.beginTransaction();
        List<T> items = dao.findByName(name);
        if (items != null && items.size() > 0) {
            result = items.get(0);
        }
        dao.commitTransaction();
        return result;
    }

    private <T extends HibernateItem> List<T> nonTransactionGetByCriteria(GenericDAO<T, Long> dao, List<SortCriterion> sortCriteria, Criterion... criteria) {
        dao.beginTransaction();
        List<T> result = dao.findByCriteria(sortCriteria, criteria);
        dao.commitTransaction();
        return result;
    }

    private <T extends HibernateItem> List<T> nonTransactionGetByCriteria(int offset, int limit, GenericDAO<T, Long> dao, List<SortCriterion> sortCriteria, Criterion... criteria) {
        return getSublist(nonTransactionGetByCriteria(dao, sortCriteria, criteria), offset, limit);
    }

    private <T extends HibernateItem> T nonTransactionGetByID(String id, GenericDAO<T, Long> dao) {
        Long dbId = getDBIdFromID(id);
        dao.beginTransaction();
        T result = dao.get(dbId);
        dao.commitTransaction();
        return result;
    }

    private <T extends HibernateMediaItem, Q extends HibernateMediaContainer<T>> List<T> nonTransactionGetContentForContainer(String id, GenericMediaContainerDAO<Q, Long> dao) {
        Long dbId = getDBIdFromID(id);
        List<T> result;
        dao.beginTransaction();
        Q container = dao.load(dbId);
        if (container != null) {
            result = container.fullList();
            result.size();
        } else {
            result = new ArrayList<T>(0);
        }
        dao.commitTransaction();
        return result;
    }

    public List<? extends VideoItem> listEpisodesForSeason(String seasonID, int offset, int limit, List<SortCriterion> sortCriteria) {
        return nonTransactionGetContentForContainer(seasonID, offset, limit, factory.getTVSeasonDAO());
    }

    private <T extends HibernateMediaItem, ContainerClass extends HibernateMediaContainer<T>> List<T> nonTransactionGetContentForContainer(String id, int offset, int limit, GenericMediaContainerDAO<ContainerClass, Long> dao) {
        Long dbId = getDBIdFromID(id);
        List<T> result;
        dao.beginTransaction();
        ContainerClass container = dao.load(dbId);
        if (container != null) {
            logger.debug("Loaded container");
            result = container.subList(offset, limit);
        } else {
            logger.warn("Container id:{} not loaded");
            result = new ArrayList<T>(0);
        }
        dao.commitTransaction();
        logger.debug("Returning result");
        return result;
    }

    private int nonTransactionGetTotal(GenericDAO<?, Long> dao) {
        dao.beginTransaction();
        int result = dao.getTotal();
        dao.commitTransaction();
        return result;
    }

    private void clearCollections() {
        AudioPlaylistDAO dao = factory.getAudioPlaylistDAO();
        Transaction t = dao.beginTransaction();
        try {
            List<HibernateAudioPlaylist> playlists = dao.getAll();
            for (HibernateAudioPlaylist playlist : playlists) {
                playlist.getItems().clear();
                dao.getSession().update(playlist);
                dao.getSession().delete(playlist);
            }
            TVShowDAO tvShowDao = factory.getTVShowDAO();
            for (HibernateTVShow show : tvShowDao.getAll()) {
                tvShowDao.getSession().delete(show);
            }
            TVSeasonDAO tvSeasonDao = factory.getTVSeasonDAO();
            for (HibernateTVSeason season : tvSeasonDao.getAll()) {
                tvSeasonDao.getSession().delete(season);
            }
            t.commit();
        } catch (Exception e) {
            logger.error("Error:", e);
            t.rollback();
        }
    }

    public boolean reset() {
        if (logger.isDebugEnabled()) {
            logger.debug("DB contains");
            logger.debug("AudioItems:{}", getNumberOfAudioItems());
            logger.debug("Albums:{}", getNumberOfAlbums());
            logger.debug("Artists:{}", getNumberOfArtists());
        }
        clearCollections();
        AudioItemDAO dao = factory.getAudioItemDAO();
        Transaction t = dao.beginTransaction();
        try {
            logger.debug("Reseting db");
            dao.getSession().createQuery("Delete from " + HibernateAudioPlaylist.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateAudioItem.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateAlbum.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateArtist.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateGenre.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateTVSeason.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateTVShow.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateTVEpisodeItem.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateImageItem.class.getName()).executeUpdate();
            dao.getSession().createQuery("Delete from " + HibernateVideoItem.class.getName()).executeUpdate();
            dao.commitTransaction();
            logger.debug("Reset complete");
        } catch (Exception e) {
            t.rollback();
            logger.error("Error reseting db", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("DB now contains");
            logger.debug("AudioItems:{}", getNumberOfAudioItems());
            logger.debug("Albums:{}", getNumberOfAlbums());
            logger.debug("Artists:{}", getNumberOfArtists());
        }
        return false;
    }

    public QueryResult searchAlbums(String containerID, int offset, int limit, SearchExp searchExp, List<SortCriterion> sortList) {
        logger.info("Searching albums crit:{} sort:{}", searchExp, sortList);
        AlbumDAO dao = factory.getAlbumDAO();
        return getQueryForSearch(containerID, offset, limit, searchExp, sortList, dao);
    }

    @SuppressWarnings("unchecked")
    public QueryResult searchMusic(String containerID, int offset, int limit, SearchExp searchExp, List<SortCriterion> sortList) {
        AudioItemDAO dao = factory.getAudioItemDAO();
        return getQueryForSearch(containerID, offset, limit, searchExp, sortList, dao);
    }

    private <T extends HibernateItem> QueryResult getQueryForSearch(String containerID, int offset, int limit, SearchExp searchExp, List<SortCriterion> sortList, GenericDAO<T, Long> dao) {
        QueryResult result = null;
        Transaction t = dao.beginTransaction();
        try {
            List<? extends T> list = transactionGetContentForContainer(containerID, sortList, searchExp, dao);
            result = new QueryResult();
            result.setTotalNumber(list.size());
            result.setResult(getSublist(list, offset, limit));
            dao.commitTransaction();
        } catch (Exception e) {
            t.rollback();
            logger.error("Error in getting query for search:{}", e);
        }
        return result;
    }

    private void setCriteriaForDBID(String id, Criteria criteria) {
        if (id == null) return;
        HibernateItemPath type = getTypeFromId(id);
        Long dbId = getDBIdFromID(id);
        if (dbId == null) return;
        switch(type) {
            case ALBUM:
                criteria.add(Restrictions.eq("album.id", dbId));
                return;
            case ARTIST:
                criteria.add(Restrictions.eq("artist.id", dbId));
                return;
            case GENRE:
                criteria.add(Restrictions.eq("genre.id", dbId));
                return;
            case TVSEASON:
                criteria.add(Restrictions.eq("tvSeason.id", dbId));
                return;
            case TVSHOW:
                criteria.add(Restrictions.eq("tvShow.id", dbId));
                return;
            case AUDIOPLAYLIST:
                criteria.createAlias("playlist", "plist");
                criteria.add(Restrictions.eq("plist.id", dbId));
                return;
            default:
                logger.error("Unsupported type for setting criteria:{}", id);
        }
    }

    public boolean shutdown() {
        manager.close();
        return true;
    }

    private <T extends HibernateItem> T transactionGetByID(String id, GenericDAO<T, Long> dao) {
        Long dbId = getDBIdFromID(id);
        return dao.get(dbId);
    }

    private <T extends HibernateItem> List<T> transactionGetContentForContainer(String id, List<SortCriterion> sortList, SearchExp searchExp, GenericDAO<T, Long> dao) {
        logger.debug("Transaction get content for container:{}, sort:{}, searchExp:{}", new Object[] { id, sortList, searchExp });
        List<T> result;
        Criteria criteria = dao.createCriteria();
        if (id != null && id.length() > 0) {
            setCriteriaForDBID(id, criteria);
        }
        if (searchExp != null) {
            HibernateUtils.setCriteriaFromSearchExp(dao.getPersistentClass(), criteria, searchExp);
        }
        if (sortList != null) {
            HibernateUtils.addSortOrder(dao.getPersistentClass(), criteria, sortList);
        }
        result = criteria.list();
        return result;
    }

    private HibernateAudioPlaylist updatePlaylist(PlaylistItem updatedPlaylist) {
        AudioPlaylistDAO dao = factory.getAudioPlaylistDAO();
        Transaction t = dao.beginTransaction();
        try {
            HibernateAudioPlaylist dbPlaylist = dao.load(getDBIdFromID(updatedPlaylist.getId()));
            if (!updatedPlaylist.getName().equalsIgnoreCase(dbPlaylist.getName())) {
                dbPlaylist.setName(updatedPlaylist.getName());
            }
            dao.save(dbPlaylist);
            dao.commitTransaction();
            return dbPlaylist;
        } catch (Exception e) {
            t.rollback();
            logger.error("Error updating playlist:", e);
        }
        return null;
    }

    public Item updateItem(Item item) {
        HibernateItemPath type = getTypeFromId(item.getId());
        switch(type) {
            case AUDIOPLAYLIST:
                return updatePlaylist((PlaylistItem) item);
            default:
                logger.error("Trying to update unsupported item type:{}", type);
        }
        return null;
    }

    public QueryResult<HibernateAudioItem> batchAddAudioItems(List<? extends AudioItem> newAudioItems) throws InvalidItemException {
        AudioItemDAO audioItemDAO = factory.getAudioItemDAO();
        ArrayList<HibernateAudioItem> result = new ArrayList<HibernateAudioItem>(newAudioItems.size());
        QueryResult<HibernateAudioItem> queryResult = new QueryResult<HibernateAudioItem>();
        queryResult.setResult(result);
        Transaction transaction = audioItemDAO.beginTransaction();
        try {
            int i = 0;
            for (AudioItem audio : newAudioItems) {
                String location = audio.getFirstResource() != null ? audio.getFirstResource().getLocation() : null;
                List<HibernateAudioItem> locationItems = audioItemDAO.findByLocation(location);
                HibernateAudioItem newItem;
                if (locationItems != null && locationItems.size() > 0) {
                    newItem = locationItems.get(0);
                } else {
                    newItem = addExternalAudioItem(audio, audioItemDAO);
                }
                result.add(newItem);
                i++;
                if (i % BATCH_SIZE == 0) {
                    audioItemDAO.getSession().flush();
                    audioItemDAO.getSession().clear();
                    logger.debug("Added:{}", i);
                }
            }
            audioItemDAO.commitTransaction();
        } catch (Exception e) {
            transaction.rollback();
            throw new InvalidItemException("Couldn't add items", e);
        }
        return queryResult;
    }

    public void addAudioItemToPlaylist(String audioId, String playlistId) throws ItemNotFoundException {
        AudioPlaylistDAO playlistDAO = factory.getAudioPlaylistDAO();
        AudioItemDAO audioDAO = factory.getAudioItemDAO();
        Transaction t = playlistDAO.beginTransaction();
        try {
            HibernateAudioPlaylist playlist = transactionGetByID(playlistId, playlistDAO);
            HibernateAudioItem item = transactionGetByID(audioId, audioDAO);
            playlist.addItem(item);
            playlistDAO.update(playlist);
            playlistDAO.commitTransaction();
        } catch (Exception e) {
            t.rollback();
            logger.error("Error adding items to playlist:", e);
            throw new ItemNotFoundException("Couldnt find an id for playlist");
        }
    }

    public void removeAudioItemFromPlaylist(String audioId, String playlistId) throws ItemNotFoundException {
        AudioPlaylistDAO playlistDAO = factory.getAudioPlaylistDAO();
        AudioItemDAO audioDAO = factory.getAudioItemDAO();
        Transaction t = playlistDAO.beginTransaction();
        try {
            HibernateAudioPlaylist playlist = transactionGetByID(playlistId, playlistDAO);
            HibernateAudioItem item = transactionGetByID(audioId, audioDAO);
            playlist.removeItem(item);
            playlistDAO.update(playlist);
            playlistDAO.commitTransaction();
        } catch (Exception e) {
            t.rollback();
            logger.error("Error adding items to playlist:", e);
            throw new ItemNotFoundException("Couldnt find an id for playlist");
        }
    }

    public void batchAddAudioItemsToPlaylist(List<String> itemIds, String playlistId) throws ItemNotFoundException {
        AudioPlaylistDAO playlistDAO = factory.getAudioPlaylistDAO();
        AudioItemDAO audioDAO = factory.getAudioItemDAO();
        Transaction t = playlistDAO.beginTransaction();
        try {
            HibernateAudioPlaylist playlist = transactionGetByID(playlistId, playlistDAO);
            for (String audioId : itemIds) {
                HibernateAudioItem item = transactionGetByID(audioId, audioDAO);
                playlist.addItem(item);
            }
            playlistDAO.update(playlist);
            playlistDAO.commitTransaction();
        } catch (Exception e) {
            t.rollback();
            logger.error("Error adding items to playlist:", e);
            throw new ItemNotFoundException("Couldnt find an id for playlist");
        }
    }

    public QueryResult getVideos(int startIndex, int requestedCount, List<SortCriterion> sortCriteria) {
        logger.info("Getting videos");
        QueryResult result = new QueryResult();
        result.setTotalNumber(getNumberOfVideos());
        List<? extends VideoItem> items = listVideo((int) startIndex, (int) requestedCount, sortCriteria);
        logger.debug("Got {} videos", items.size());
        result.setResult(items);
        return result;
    }

    @SuppressWarnings("unchecked")
    public QueryResult getContentForContainer(String containerID, int offset, int limit, List<SortCriterion> sortCriteria) {
        QueryResult result = new QueryResult();
        result.setResult(listContentForContainer(containerID, (int) offset, (int) limit, sortCriteria));
        result.setParentPath(containerID);
        result.setTotalNumber(getNumberOfItemsInContainer(containerID));
        return result;
    }

    @SuppressWarnings("unchecked")
    public QueryResult getAlbumsForArtist(String containerID, int startIndex, int requestedCount, List<SortCriterion> sortOrder) {
        QueryResult result = new QueryResult();
        List<? extends AlbumItem> items = listAlbumsForArtist(containerID, sortOrder);
        result.setTotalNumber(items.size());
        result.setResult(items);
        return result;
    }

    @SuppressWarnings("unchecked")
    public QueryResult getSeasonsForShow(String containerID, int startIndex, int requestedCount, List<SortCriterion> sortOrder) {
        QueryResult result = new QueryResult();
        List<? extends TVSeasonItem> items = listSeasonsForShow(containerID, startIndex, requestedCount, sortOrder);
        result.setTotalNumber(items.size());
        result.setResult(items);
        return result;
    }

    @SuppressWarnings("unchecked")
    public QueryResult<? extends AlbumItem> getAlbums(int startIndex, int requestedCount, List<SortCriterion> sortCriteria) {
        QueryResult result = new QueryResult();
        result.setTotalNumber(getNumberOfAlbums());
        List<? extends AlbumItem> items = listAlbums((int) startIndex, (int) requestedCount, sortCriteria);
        result.setResult(items);
        return result;
    }

    public QueryResult<? extends ArtistItem> getArtists(int startIndex, int requestedCount, List<SortCriterion> sortCriteria) {
        QueryResult result = new QueryResult();
        result.setTotalNumber(getNumberOfArtists());
        List<? extends ArtistItem> list = listArtists((int) startIndex, (int) requestedCount, sortCriteria);
        result.setResult(list);
        return result;
    }

    public QueryResult<? extends AudioItem> getAudio(int startIndex, int requestedCount, List<SortCriterion> sortOrder) {
        QueryResult result = new QueryResult();
        result.setTotalNumber(getNumberOfAudioItems());
        List<? extends AudioItem> items = listAudio((int) startIndex, (int) requestedCount, sortOrder);
        result.setResult(items);
        return result;
    }

    public QueryResult<? extends AudioGenreItem> getGenres(int startIndex, int requestedCount, List<SortCriterion> sortCriteria) {
        QueryResult result = new QueryResult();
        result.setTotalNumber(getNumberOfGenres());
        List<? extends AudioGenreItem> list = listGenres((int) startIndex, (int) requestedCount, sortCriteria);
        result.setResult(list);
        return result;
    }

    public QueryResult<? extends PlaylistItem> getPlaylists(int startIndex, int requestedCount, List<SortCriterion> sortCriteria) {
        QueryResult result = new QueryResult();
        result.setTotalNumber(getNumberOfPlaylists());
        List<? extends PlaylistItem> list = listPlaylists((int) startIndex, (int) requestedCount, sortCriteria);
        result.setResult(list);
        return result;
    }

    public QueryResult<? extends TVShowItem> getTVShows(int startIndex, int requestedCount, List<SortCriterion> sortCriteria) {
        QueryResult result = new QueryResult();
        result.setTotalNumber(getNumberOfTVShows());
        List<? extends TVShowItem> list = listTVShows((int) startIndex, (int) requestedCount, sortCriteria);
        result.setResult(list);
        return result;
    }
}
