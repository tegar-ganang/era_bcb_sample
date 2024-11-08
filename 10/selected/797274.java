package house.neko.media.common;

import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.DatabaseMetaData;
import org.apache.commons.logging.Log;
import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 *
 * @author andy
 */
public class DatabaseDataStore implements DataStore {

    private HierarchicalConfiguration config = null;

    private Log log = null;

    private MediaLibrary library = null;

    private String usernm = "UNKNOWN";

    private int userid = 0;

    private Connection _conn = null;

    private PreparedStatement insertMediaTrackStatement = null;

    private PreparedStatement getArtistIDStatement = null;

    private PreparedStatement insertArtistAliasStatement = null;

    private PreparedStatement getArtistIdentityStatement = null;

    private PreparedStatement insertArtistStatement = null;

    private PreparedStatement getMediaTrackIdentityStatement = null;

    private PreparedStatement getMediaLocationUpdateableStatement = null;

    private PreparedStatement getMediaTrackByPersistantID = null;

    private TreeMap<String, MimeType> cacheFileExtenstionMimeType = new TreeMap<String, MimeType>();

    private TreeMap<Integer, MimeType> cacheMimeTypeID = new TreeMap<Integer, MimeType>();

    private TreeMap<String, Integer> cacheArtistID = new TreeMap<String, Integer>();

    private TreeMap<String, Integer> cacheArtistAliasID = new TreeMap<String, Integer>();

    /**
	 *
	 * @param library
	 * @param config
	 * @throws java.lang.Exception
	 */
    public void init(MediaLibrary library, HierarchicalConfiguration config) throws Exception {
        this.log = ConfigurationManager.getLog(getClass());
        if (log.isTraceEnabled()) {
            log.trace("Initializing database store");
        }
        this.config = config;
        try {
            Class c = Class.forName(config.getString("Driver"));
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        this.library = library;
        try {
            usernm = config.getString("User");
            _conn = getConnection();
            _conn.setAutoCommit(false);
            if (log.isInfoEnabled()) {
                DatabaseMetaData metadata = _conn.getMetaData();
                log.info("Using driver " + metadata.getDriverName() + " version " + metadata.getDriverVersion());
                log.info("Connected to " + metadata.getDatabaseProductName() + " version " + metadata.getDatabaseProductVersion());
            }
            PreparedStatement s = _conn.prepareStatement("select user_id from public.user where user_nm=?");
            s.setString(1, usernm);
            ResultSet rs = s.executeQuery();
            if (!rs.next()) {
                log.error("Unable to connect to datastore as user '" + usernm + "'!");
            } else {
                userid = rs.getInt(1);
            }
            rs.close();
            s.close();
            _conn.commit();
        } catch (SQLException e) {
            log.error(e.toString(), e);
        }
        DatabaseStore$LoadMedia loader = new DatabaseStore$LoadMedia();
        new Thread(loader).start();
        if (log.isTraceEnabled()) {
            log.trace("Initialion of database store complete");
        }
    }

    private Connection getConnection() throws java.sql.SQLException {
        if (_conn == null) {
            log.trace("Getting connection");
            String url = config.getString("URL");
            log.trace("URL = " + url);
            String user = config.getString("Username");
            log.trace("USER = " + user);
            String pass = config.getString("Password");
            log.trace("PASSWORD = " + pass);
            _conn = DriverManager.getConnection(url, user, pass);
            if (log.isTraceEnabled()) {
                log.trace("Created connection to " + url + " with user " + user);
            }
        }
        return _conn;
    }

    private void returnConnection(Connection c) throws SQLException {
        c.commit();
        return;
    }

    private Long getLong(ResultSet rs, int position) throws SQLException {
        Long l = rs.getLong(position);
        if (rs.wasNull()) {
            l = null;
        }
        return l;
    }

    private Media mapResultSetToMedia(ResultSet rs) throws java.sql.SQLException {
        Media m = new Media();
        m.setLocalID(rs.getLong(SQL_SELECT_TRACK_ID_POS));
        m.setID(rs.getString(SQL_SELECT_TRACK_PERSISTENT_ID_POS));
        m.setName(rs.getString(SQL_SELECT_TRACK_NAME_POS));
        m.setArtist(rs.getString(SQL_SELECT_TRACK_ARTIST_NAME_POS));
        m.setArtistAlias(rs.getString(SQL_SELECT_TRACK_ARTIST_ALIAS_NAME_POS));
        m.setAuthor(rs.getString(SQL_SELECT_TRACK_AUTHOR_NAME_POS));
        SETLOCATIONS: {
            String localURL = rs.getString(SQL_SELECT_TRACK_URL_LOCAL_POS);
            if (localURL != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Media " + m.getID() + " has local URl!");
                }
                MediaLocation localLocation = new MediaLocation();
                localLocation.setLocationURLString(localURL);
                localLocation.setMimeType(getMimeTypeByID(rs.getInt(SQL_SELECT_TRACK_MIME_TYPE_ID_LOCAL_POS), rs.getStatement().getConnection()));
                long size = rs.getLong(SQL_SELECT_TRACK_SIZE_LOCAL_POS);
                if (!rs.wasNull()) {
                    localLocation.setSize(size);
                }
                m.setLocalLocation(localLocation);
            }
            String remoteURL = rs.getString(SQL_SELECT_TRACK_URL_REMOTE_POS);
            if (remoteURL != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Media " + m.getID() + " has local URl!");
                }
                MediaLocation remoteLocation = new MediaLocation();
                remoteLocation.setLocationURLString(remoteURL);
                remoteLocation.setMimeType(getMimeTypeByID(rs.getInt(SQL_SELECT_TRACK_MIME_TYPE_ID_REMOTE_POS), rs.getStatement().getConnection()));
                long size = rs.getLong(SQL_SELECT_TRACK_SIZE_REMOTE_POS);
                if (!rs.wasNull()) {
                    remoteLocation.setSize(size);
                }
                m.setRemoteLocation(remoteLocation);
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("Mapped media -> " + m);
        }
        return m;
    }

    /**
	 *
	 * @param id
	 * @return
	 */
    public Media getMedia(String id) {
        if (_conn == null) {
            log.error("DatabaseDatastore not connected!");
            return null;
        }
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            s = _conn.prepareStatement(SQL_SELECTMEDIA + " WHERE track_persistent_id = ?");
            s.setString(1, id);
            rs = s.executeQuery();
            if (!rs.next()) {
                rs.close();
                s.close();
                return null;
            }
            Media m = mapResultSetToMedia(rs);
            rs.close();
            s.close();
            return m;
        } catch (Exception e) {
            log.error(e.toString(), e);
            return null;
        }
    }

    /**
	 *
	 * @return
	 */
    public Media[] getAllMedia() {
        if (_conn == null) {
            log.error("DatabaseDatastore not connected!");
            return new Media[0];
        }
        Media[] list = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            long startTimeMS = 0L;
            if (log.isDebugEnabled()) {
                log.debug("Getting all media from database store");
                startTimeMS = System.currentTimeMillis();
            }
            s = _conn.prepareStatement(SQL_SELECTMEDIA);
            System.out.println(SQL_SELECTMEDIA);
            rs = s.executeQuery();
            Vector<Media> mlist = new Vector<Media>();
            while (rs.next()) {
                Media m = mapResultSetToMedia(rs);
                m.resetDirty();
                mlist.add(m);
            }
            rs.close();
            s.close();
            if (log.isTraceEnabled()) {
                log.trace("Found " + mlist.size() + " tracks, loading");
            }
            list = mlist.toArray(new Media[mlist.size()]);
            for (int k = 0; k < list.length; k++) {
                library.addMedia(list[k]);
            }
            if (log.isDebugEnabled()) {
                log.debug("Loaded " + list.length + " tracks in " + (System.currentTimeMillis() - startTimeMS) + " millisecons");
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return list;
    }

    /**
	 *
	 * @param m
	 */
    public void putMedia(Media m) {
        if (m == null) {
            return;
        }
        if (_conn == null) {
            log.error("DatabaseDatastore not connected!");
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("Writing Media " + m.toString() + " to database");
        }
        try {
            try {
                long trackid = getLocalID(m, _conn);
                if (m.isBaseDirty()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Need to update base " + m.getID() + " to database");
                    }
                    Integer artist = getArtistID(m, _conn);
                    Integer author = getAuthorID(m, _conn);
                    Integer artistAlias = getArtistAliasID(m, _conn);
                    PreparedStatement s = _conn.prepareStatement("update media_track set track_name=?,track_artist_id=?,track_author_id=?,track_artist_alias_id=?,track_audit_timestamp=CURRENT_TIMESTAMP where track_id = ?");
                    s.setString(1, m.getName());
                    if (artist != null) {
                        s.setLong(2, artist);
                    } else {
                        s.setNull(2, Types.BIGINT);
                    }
                    if (author != null) {
                        s.setLong(3, author);
                    } else {
                        s.setNull(3, Types.BIGINT);
                    }
                    if (artistAlias != null) {
                        s.setLong(4, artistAlias);
                    } else {
                        s.setNull(4, Types.BIGINT);
                    }
                    s.setLong(5, trackid);
                    s.executeUpdate();
                    s.close();
                }
                if (m.isUserDirty()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Need to update user " + m.getID() + " to database");
                    }
                    PreparedStatement s = _conn.prepareStatement("update media_track_rating set rating=?, play_count=? where track_id=? and user_id=?");
                    s.setFloat(1, m.getRating());
                    s.setLong(2, m.getPlayCount());
                    s.setLong(3, trackid);
                    s.setLong(4, userid);
                    if (s.executeUpdate() != 1) {
                        s.close();
                    }
                    s.close();
                }
                if (m.isContentDirty()) {
                    updateLocation(m, _conn);
                }
                _conn.commit();
                m.resetDirty();
                if (log.isTraceEnabled()) {
                    log.trace("Committed " + m.getID() + " to database");
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
                _conn.rollback();
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    private void updateLocation(Media m, Connection c) throws SQLException {
        if (m.isContentDirty()) {
            PreparedStatement s = c.prepareStatement("select location_type,location_url,mime_id,track_id,location_size from media_track_location where track_id=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            s.setLong(1, getLocalID(m, c));
            ResultSet rs = s.executeQuery();
            boolean updateLocal = m.getLocalLocation() != null;
            boolean updateRemote = m.getRemoteLocation() != null;
            while (rs.next()) {
                String location_type = rs.getString(1);
                if (URL_LOCATION_TYPE_LOCAL.equals(location_type)) {
                    MimeType mt = m.getLocalLocation().getMimeType();
                    if (mt == null) {
                        mt = getMimeTypeByFileExtension(m.getLocalLocation().getLocationURLString());
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Updating local location to " + mt + " with URL " + m.getLocalLocation().getLocationURLString());
                    }
                    rs.updateString(2, m.getLocalLocation().getLocationURLString());
                    if (mt == null || mt.getLocalID() == null) {
                        rs.updateNull(3);
                    } else {
                        rs.updateInt(3, mt.getLocalID());
                    }
                    if (m.getLocalLocation().getSize() != null) {
                        rs.updateLong(4, m.getLocalLocation().getSize());
                    } else {
                        rs.updateNull(4);
                    }
                    updateLocal = false;
                } else if (URL_LOCATION_TYPE_REMOTE.equals(location_type)) {
                    MimeType mt = m.getLocalLocation().getMimeType();
                    if (mt == null) {
                        mt = getMimeTypeByFileExtension(m.getRemoteLocation().getLocationURLString());
                    }
                    rs.updateString(2, m.getRemoteLocation().getLocationURLString());
                    if (mt == null || mt.getLocalID() == null) {
                        rs.updateNull(3);
                    } else {
                        rs.updateInt(3, mt.getLocalID());
                    }
                    if (m.getRemoteLocation().getSize() != null) {
                        rs.updateLong(4, m.getRemoteLocation().getSize());
                    } else {
                        rs.updateNull(4);
                    }
                    updateLocal = false;
                }
            }
            rs.close();
            s.close();
            if (updateLocal || updateRemote) {
                s = c.prepareStatement("insert into media_track_location (track_id,location_type,mime_id,location_url,location_size) values (?,?,?,?,?)");
                if (updateLocal) {
                    s.setLong(1, m.getLocalID());
                    s.setString(2, URL_LOCATION_TYPE_LOCAL);
                    if (m.getLocalLocation() != null) {
                        if (m.getLocalLocation().getMimeType() != null) {
                            MimeType type = getMimeTypeByFileExtension(m.getLocalLocation().getMimeType().getFileExtension());
                            if (type == null) {
                                type = getMimeTypeByFileExtension(m.getLocalLocation().getLocationURLString());
                            }
                            if (type != null) {
                                setIntegerForStatement(s, 3, type.getLocalID());
                            } else {
                                s.setNull(3, Types.INTEGER);
                            }
                        } else {
                            s.setNull(3, Types.INTEGER);
                        }
                        s.setString(4, m.getLocalLocation().getLocationURLString());
                        if (m.getLocalLocation().getSize() != null) {
                            s.setLong(5, m.getLocalLocation().getSize());
                        } else {
                            s.setNull(5, Types.BIGINT);
                        }
                    } else {
                        s.setNull(3, Types.INTEGER);
                        s.setNull(4, Types.VARCHAR);
                        s.setNull(5, Types.BIGINT);
                    }
                    s.addBatch();
                }
                if (updateRemote) {
                    s.setLong(1, m.getLocalID());
                    s.setString(2, URL_LOCATION_TYPE_REMOTE);
                    if (m.getRemoteLocation() != null) {
                        if (m.getRemoteLocation().getMimeType() != null) {
                            setIntegerForStatement(s, 3, getMimeTypeByFileExtension(m.getLocalLocation().getMimeType().getFileExtension()).getLocalID());
                        } else {
                            s.setNull(3, Types.INTEGER);
                        }
                        s.setString(4, m.getRemoteLocation().getLocationURLString());
                        if (m.getRemoteLocation().getSize() != null) {
                            s.setLong(5, m.getRemoteLocation().getSize());
                        } else {
                            s.setNull(5, Types.BIGINT);
                        }
                    } else {
                        s.setNull(3, Types.INTEGER);
                        s.setNull(4, Types.VARCHAR);
                        s.setNull(5, Types.BIGINT);
                    }
                    s.addBatch();
                }
                s.executeBatch();
                s.close();
            }
        }
    }

    public MimeType getMimeTypeByURL(String url) {
        if (url == null) {
            return null;
        }
        Pattern p = Pattern.compile("^.*\\.([^.]+)$");
        Matcher m = p.matcher(url);
        if (m.matches()) {
            return getMimeTypeByFileExtension(m.group(1));
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Unable to interpret URL '" + url + "' to get extension");
            }
        }
        return null;
    }

    public MimeType getMimeTypeByFileExtension(String extension) {
        MimeType type = cacheFileExtenstionMimeType.get(extension);
        if (type == null) {
            if (log.isTraceEnabled()) {
                log.trace("Mime cache miss for '" + extension + "'");
            }
            try {
                Connection c = getConnection();
                try {
                    type = getMimeTypeByFileExtension(extension, c);
                } catch (SQLException sqle) {
                    log.error("Unable to get mime type for file extension '" + extension + "'", sqle);
                }
                returnConnection(c);
            } catch (SQLException sqle) {
                log.error("Unable to get connection to get mime type for file extension '" + extension + "'", sqle);
            }
        }
        return type;
    }

    private MimeType getMimeTypeByFileExtension(String extension, Connection c) throws SQLException {
        if (extension == null || extension.length() < 1) {
            return null;
        }
        extension = extension.toLowerCase();
        MimeType type = cacheFileExtenstionMimeType.get(extension);
        if (type == null) {
            PreparedStatement s = c.prepareStatement("select mime_id,mime_type, mime_sub_type, mime_file_extension, mime_file_type,mime_file_creator from mime_type where mime_file_extension = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            s.setString(1, extension);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                type = new MimeType();
                type.setLocalID(rs.getInt(1));
                type.setMimeType(rs.getString(2));
                type.setMimeSubType(rs.getString(3));
                type.setFileExtension(rs.getString(4));
                type.setFileCreatorID(getLong(rs, 5));
                type.setFileTypeID(getLong(rs, 6));
                cacheFileExtenstionMimeType.put(extension, type);
                if (log.isTraceEnabled()) {
                    log.trace("Got type for getMimeTypeByFileExtension(\"" + extension + "\") -> " + type + " with local ID " + rs.getInt(1));
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Type for getMimeTypeByFileExtension(\"" + extension + "\") NOT FOUND!");
                }
            }
            rs.close();
            s.close();
        }
        return type;
    }

    private MimeType getMimeTypeByID(Integer id, Connection c) throws SQLException {
        MimeType type = cacheMimeTypeID.get(id);
        if (type == null) {
            PreparedStatement s = c.prepareStatement("select mime_type, mime_sub_type, mime_file_extension, mime_file_type,mime_file_creator from mime_type where mime_id = ?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            s.setInt(1, id);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                type = new MimeType();
                type.setLocalID(id);
                type.setFileExtension(rs.getString(3));
                type.setMimeType(rs.getString(1));
                type.setMimeSubType(rs.getString(2));
                type.setFileCreatorID(getLong(rs, 5));
                type.setFileTypeID(getLong(rs, 4));
                cacheMimeTypeID.put(id, type);
                if (log.isTraceEnabled()) {
                    log.trace("Got type for getMimeTypeByID(\"" + id + "\") -> " + type);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Type for getMimeTypeByID(\"" + id + "\") NOT FOUND!");
                }
            }
            rs.close();
            s.close();
        }
        return type;
    }

    private Long getLocalID(Media m, Connection c) throws SQLException {
        Long localID = m.getLocalID();
        if (localID == null) {
            if (getMediaTrackByPersistantID == null) {
                getMediaTrackByPersistantID = _conn.prepareStatement("select track_id from media_track where track_persistent_id = ?");
            }
            getMediaTrackByPersistantID.setString(1, m.getID());
            ResultSet rs = getMediaTrackByPersistantID.executeQuery();
            if (rs != null && rs.next()) {
                localID = rs.getLong(1);
                m.setLocalID(localID);
                rs.close();
            } else {
                rs.close();
                if (log.isTraceEnabled()) {
                    log.trace("Need to insert " + m.getID() + " to database");
                }
                if (insertMediaTrackStatement == null) insertMediaTrackStatement = _conn.prepareStatement("insert into media_track(track_name,track_author_id,track_artist_id,track_artist_alias_id,track_audit_user,track_audit_timestamp,track_add_timestamp,track_length_ms,track_persistent_id) values (?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?)", Statement.RETURN_GENERATED_KEYS);
                insertMediaTrackStatement.setString(1, m.getName());
                Integer author = getAuthorID(m, _conn);
                if (author == null) {
                    insertMediaTrackStatement.setNull(2, Types.INTEGER);
                } else {
                    insertMediaTrackStatement.setInt(2, author);
                }
                int artist = getArtistID(m, _conn);
                insertMediaTrackStatement.setInt(3, artist);
                Integer artistAliasID = getArtistAliasID(m, _conn);
                if (artistAliasID == null) {
                    insertMediaTrackStatement.setNull(4, Types.INTEGER);
                } else {
                    insertMediaTrackStatement.setInt(4, artistAliasID);
                }
                insertMediaTrackStatement.setInt(5, userid);
                insertMediaTrackStatement.setLong(6, m.getLength());
                insertMediaTrackStatement.setString(7, m.getID());
                insertMediaTrackStatement.executeUpdate();
                if (log.isTraceEnabled()) {
                    log.trace("inserting track " + m.getName() + "' with artist '" + m.getArtist() + "' (" + artist + ")");
                }
                rs = insertMediaTrackStatement.getGeneratedKeys();
                if (rs.next()) {
                    localID = rs.getLong(1);
                    m.setLocalID(localID);
                    rs.close();
                } else {
                    rs.close();
                    throw new SQLException("Unable to get track_id key from insert for " + m);
                }
            }
        }
        return localID;
    }

    private Integer getAuthorID(Media m, Connection c) throws SQLException {
        return getArtistID(m, c, m.getAuthor());
    }

    private Integer getArtistID(Media m, Connection c) throws SQLException {
        return getArtistID(m, c, m.getArtist());
    }

    private Integer getArtistID(Media m, Connection c, String artistName) throws SQLException {
        if (artistName == null) {
            return null;
        }
        Integer artistID = cacheArtistID.get(artistName);
        if (artistID != null) {
            if (log.isTraceEnabled()) {
                log.trace("Found artist " + artistID + " in cache for '" + artistName + "'");
            }
            return artistID;
        }
        if (getArtistIDStatement == null) {
            getArtistIDStatement = c.prepareStatement("SELECT artist_id FROM artist WHERE artist_name=?");
        }
        getArtistIDStatement.setString(1, artistName);
        ResultSet rs = getArtistIDStatement.executeQuery();
        if (rs.next()) {
            artistID = rs.getInt(1);
            rs.close();
            if (log.isTraceEnabled()) {
                log.trace("Found artist " + artistID + " in DB for '" + artistName + "'");
            }
        } else {
            rs.close();
            if (log.isTraceEnabled()) {
                log.trace("Did not find artist in DB for '" + artistName + "', inserting . . .");
            }
            if (insertArtistStatement == null) {
                insertArtistStatement = c.prepareStatement("INSERT INTO artist(artist_name,artist_audit_user_id,artist_create_timestamp,artist_modify_timestamp) VALUES(?,?,current_timestamp,current_timestamp)", Statement.RETURN_GENERATED_KEYS);
            }
            insertArtistStatement.setString(1, artistName);
            insertArtistStatement.setInt(2, userid);
            insertArtistStatement.executeUpdate();
            rs = insertArtistStatement.getGeneratedKeys();
            if (rs.next()) {
                artistID = rs.getInt(1);
                rs.close();
                if (log.isTraceEnabled()) {
                    log.trace("Got artist id " + artistID + " for '" + artistName + "'");
                }
            } else {
                rs.close();
                throw new SQLException("Unable to get artist_id key from insert for " + m);
            }
            if (log.isDebugEnabled()) {
                log.debug("Adding artist '" + artistName + "' with local ID of " + artistID);
            }
        }
        cacheArtistID.put(artistName, artistID);
        return artistID;
    }

    private Integer getArtistAliasID(Media m, Connection c) throws SQLException {
        if (m.getArtistAlias() == null) {
            return null;
        }
        Integer artistAliasID = cacheArtistAliasID.get(m.getArtistAlias());
        if (artistAliasID != null) {
            return artistAliasID;
        }
        artistAliasID = getArtistID(m, c, m.getArtistAlias());
        Integer artistID = getArtistID(m, c);
        PreparedStatement s = c.prepareStatement("select artist_alias_id,artist_id from artist_alias where artist_id=? and artist_alias_id=?", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        s.setInt(1, artistID);
        s.setInt(2, artistAliasID);
        ResultSet rs = s.executeQuery();
        if (rs.next()) {
            artistAliasID = rs.getInt(1);
            artistID = rs.getInt(2);
            if (log.isTraceEnabled()) {
                log.trace("Found alias in artist_alias " + m.getArtistAlias() + " for " + m.toString() + " with " + artistID + " linked to " + artistAliasID);
            }
            rs.close();
        } else {
            rs.close();
            if (log.isTraceEnabled()) {
                log.trace("Did not find alias will add for " + m.toString());
            }
            if (insertArtistAliasStatement == null) {
                insertArtistAliasStatement = c.prepareStatement("INSERT INTO artist_alias(artist_id, artist_alias_id, artist_alias_audit_user_id, artist_alias_create_timestamp) VALUES(?, ?, ?, current_timestamp)");
            }
            insertArtistAliasStatement.setInt(1, artistID);
            insertArtistAliasStatement.setInt(2, artistAliasID);
            insertArtistAliasStatement.setInt(3, userid);
            try {
                insertArtistAliasStatement.executeUpdate();
            } catch (SQLException sqle) {
                log.error("Error trying to create relationship between " + artistID + " (base) and " + artistAliasID + " (alias) for '" + m.getArtistAlias() + "'", sqle);
            }
        }
        cacheArtistAliasID.put(m.getArtistAlias(), artistAliasID);
        return artistAliasID;
    }

    private void setIntegerForStatement(PreparedStatement s, int position, Integer i) throws SQLException {
        if (i != null) {
            s.setInt(position, i);
        } else {
            s.setNull(position, Types.INTEGER);
        }
    }

    private class DatabaseStore$LoadMedia implements Runnable {

        public void run() {
            getAllMedia();
        }
    }

    public DataStoreConfigurationHelper getConfigurationHelper() {
        String[] keys = { "User", "Type", "Driver", "URL", "Username", "Primary" };
        String[] defaultValues = { System.getProperty("user.name"), this.getClass().getName(), "org.postgresql.Driver", "jdbc:postgresql://localhost:5432/music?logUnclosedConnections=true?loginTimeout=10", "music", "music", "true" };
        String[] descriptions = { "Your name", "DataStore class, leave this alone", "JDBC Driver class", "JDBC Connection URL", "JDBC Connection username", "JDBC Connection password", "Flag to indicate primary datastore (where new media gets stored)" };
        return new DataStore.DataStoreConfigurationHelper(keys, defaultValues, descriptions);
    }

    /**
	 *
	 */
    @Override
    protected final void finalize() {
        try {
            if (_conn != null && !_conn.isClosed()) {
                _conn.close();
            }
        } catch (SQLException e) {
            log.error(e.toString(), e);
        }
    }

    private static final String URL_LOCATION_TYPE_LOCAL = "L";

    private static final String URL_LOCATION_TYPE_REMOTE = "R";

    private static final String SQL_SELECTMEDIA = "SELECT " + "t.track_id," + "t.track_name," + "t.track_artist_id," + "t.track_artist_alias_id," + "t.track_audit_user," + "t.track_audit_timestamp," + "" + "t.track_add_timestamp," + "t.track_length_ms," + "t.track_persistent_id, " + "art.artist_name," + "ll.location_url," + "ll.mime_id," + "rl.location_url," + "rl.mime_id," + "rl.location_size," + "ll.location_size," + "t.track_author_id, " + "auth.artist_name," + "aa.artist_name " + "FROM media_track t " + "inner join artist art on t.track_artist_id = art.artist_id " + "left join artist auth on t.track_author_id = auth.artist_id " + "left join artist_alias alias on t.track_artist_alias_id = alias.artist_alias_id " + "left join artist aa on alias.artist_alias_id = aa.artist_id " + "left join media_track_location ll on (t.track_id = ll.track_id and ll.location_type = '" + URL_LOCATION_TYPE_LOCAL + "') " + "left join media_track_location rl on (t.track_id = rl.track_id and rl.location_type = '" + URL_LOCATION_TYPE_REMOTE + "') ";

    private static final int SQL_SELECT_TRACK_ID_POS = 1;

    private static final int SQL_SELECT_TRACK_NAME_POS = 2;

    private static final int SQL_SELECT_TRACK_AUTHOR_ID_POS = 17;

    private static final int SQL_SELECT_TRACK_AUTHOR_NAME_POS = 18;

    private static final int SQL_SELECT_TRACK_ARTIST_ID_POS = 3;

    private static final int SQL_SELECT_TRACK_ARTIST_NAME_POS = 10;

    private static final int SQL_SELECT_TRACK_ARTIST_ALIAS_ID_POS = 4;

    private static final int SQL_SELECT_TRACK_ARTIST_ALIAS_NAME_POS = 19;

    private static final int SQL_SELECT_TRACK_LENGTH_MS_POS = 8;

    private static final int SQL_SELECT_TRACK_PERSISTENT_ID_POS = 9;

    private static final int SQL_SELECT_TRACK_URL_LOCAL_POS = 11;

    private static final int SQL_SELECT_TRACK_SIZE_LOCAL_POS = 16;

    private static final int SQL_SELECT_TRACK_SIZE_REMOTE_POS = 15;

    private static final int SQL_SELECT_TRACK_MIME_TYPE_ID_LOCAL_POS = 12;

    private static final int SQL_SELECT_TRACK_URL_REMOTE_POS = 13;

    private static final int SQL_SELECT_TRACK_MIME_TYPE_ID_REMOTE_POS = 14;
}
