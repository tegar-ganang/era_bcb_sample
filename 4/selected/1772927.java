package net.sourceforge.pyrus.mcollection.mp3;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sourceforge.pyrus.mcollection.AlbumImpl;
import net.sourceforge.pyrus.mcollection.AuthorImpl;
import net.sourceforge.pyrus.mcollection.SongImpl;
import net.sourceforge.pyrus.mcollection.api.Album;
import net.sourceforge.pyrus.mcollection.api.Author;
import net.sourceforge.pyrus.mcollection.api.MusicCollection;
import net.sourceforge.pyrus.mcollection.api.MusicCollectionException;
import net.sourceforge.pyrus.mcollection.api.Song;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import entagged.audioformats.AudioFile;
import entagged.audioformats.AudioFileIO;

public class MP3Collection implements MusicCollection {

    private static final Log log = LogFactory.getLog(MP3Collection.class);

    private static final String PROP_DESCRIPTION = "description";

    private Connection dbcon;

    private List<Author> authors = new ArrayList<Author>();

    private String dbName;

    private File path;

    private String id;

    private String description;

    public MP3Collection(File path) {
        log.debug("ctor - path=" + path.getAbsolutePath());
        this.path = path;
        this.dbName = path.getAbsolutePath() + "/.pyrus/pyrus-music-collection.db";
        Properties props = new Properties();
        File propsFile = new File(path.getAbsolutePath() + "/.pyrus/pyrus-music-collection.properties");
        if (propsFile.exists()) {
            try {
                InputStream is = new FileInputStream(propsFile);
                props.load(is);
                is.close();
            } catch (IOException e) {
                log.error("ctor - cannot read collection properties", e);
            }
        } else {
            log.warn("ctor - collection in " + path + " has no properties");
        }
        description = props.getProperty(PROP_DESCRIPTION, path.toString());
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void start() {
    }

    public void stop() {
    }

    public File getPath() {
        return path;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    private Connection getConnection() throws SQLException {
        if (dbcon == null || dbcon.isClosed()) {
            dbcon = DriverManager.getConnection("jdbc:hsqldb:file:" + dbName, "sa", "");
        }
        return dbcon;
    }

    private static class ScanCollectionState {

        private int nsongs;

        private Listener ccb;

        private Map<String, AuthorImpl> authorsMap = new HashMap<String, AuthorImpl>();

        private Map<String, AlbumImpl> albumsMap = new HashMap<String, AlbumImpl>();

        private List<SongImpl> songs = new ArrayList<SongImpl>();

        private int nextAuthorId = 1;

        private int nextAlbumId = 1;

        private int nextSongId = 1;
    }

    private static final FileFilter COVERS_FILTER = new FileFilter() {

        public boolean accept(File pathname) {
            String path = pathname.getAbsolutePath();
            int index = path.lastIndexOf(".");
            String ext = pathname.getAbsolutePath().substring(index + 1);
            return ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("bmp") || ext.equalsIgnoreCase("gif");
        }
    };

    private static final int REPLACEMENT_CHAR = 65533;

    public void scan(Listener ccb) throws MusicCollectionException {
        ScanCollectionState scs = new ScanCollectionState();
        log.debug("scan - path=" + path.getAbsolutePath() + " id=" + id);
        ccb.processStarted("Buscando canciones");
        scs.nsongs = recursiveCountSongs(path);
        ccb.processFinished();
        scs.ccb = ccb;
        ccb.processStarted("Analizando " + scs.nsongs + " canciones");
        recursiveFillSongs(scs, path);
        ccb.processFinished();
        dump(scs);
        load(ccb);
    }

    public void load(Listener ccb) throws MusicCollectionException {
        clear();
        if (!new File(dbName + ".script").exists()) {
            try {
                createDB(ccb);
                scan(ccb);
            } catch (IOException e) {
                throw new MusicCollectionException("Cannot create collection database", e);
            }
        } else {
            Statement stmt = null;
            ResultSet rs = null;
            ccb.processStarted("Cargando la colección");
            try {
                Connection conn = getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT COUNT(*) FROM AUTHORS");
                rs.next();
                int ntotal = rs.getInt(1);
                rs.close();
                int ncur = 0;
                rs = stmt.executeQuery("SELECT * FROM AUTHORS ORDER BY name");
                while (rs.next()) {
                    Author author = new MP3Author(this, rs.getInt("id"), rs.getString("name"));
                    authors.add(author);
                    ncur++;
                    ccb.processRunning(100 * ncur / ntotal);
                }
                rs.close();
                rs = null;
            } catch (Exception e) {
                throw new MusicCollectionException("Cannot load collection database", e);
            } finally {
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (Exception e2) {
                    }
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (Exception e2) {
                    }
                }
            }
            ccb.processFinished();
        }
    }

    private void createDB(Listener ccb) throws IOException {
        OutputStream os;
        InputStream is;
        int c;
        ccb.processStarted("Creando la colección");
        new File(dbName).getParentFile().mkdirs();
        is = getClass().getResourceAsStream("pyrus-music-collection.db.properties");
        os = new FileOutputStream(dbName + ".properties");
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        is.close();
        os.close();
        ccb.processRunning(50);
        is = getClass().getResourceAsStream("pyrus-music-collection.db.script");
        os = new FileOutputStream(dbName + ".script");
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        is.close();
        os.close();
        ccb.processFinished();
    }

    void loadSongs(Album album, Collection<Song> songs) throws MusicCollectionException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM SONGS WHERE ALBUMID=" + album.getId() + " ORDER BY track, title");
            while (rs.next()) {
                songs.add(new SongImpl(rs.getInt("id"), album, album.getAuthor(), new File(rs.getString("file")), rs.getString("title"), rs.getInt("track"), rs.getLong("duration"), rs.getInt("channels"), rs.getInt("bitrate"), rs.getBoolean("bitrateVariable"), rs.getLong("size"), rs.getLong("lastModified")));
            }
            rs.close();
            rs = null;
        } catch (Exception e) {
            throw new MusicCollectionException("Cannot load album's songs: " + album.getName(), e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e2) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e2) {
                }
            }
        }
    }

    void loadAlbums(Author author, Collection<Album> albums) throws MusicCollectionException {
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Connection conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM ALBUMS WHERE AUTHORID=" + author.getId() + " ORDER BY name");
            while (rs.next()) {
                albums.add(new MP3Album(this, rs.getInt("id"), author, rs.getString("name"), new File(rs.getString("coverFile"))));
            }
            rs.close();
            rs = null;
        } catch (Exception e) {
            throw new MusicCollectionException("Cannot load artist's albums: " + author.getName(), e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e2) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e2) {
                }
            }
        }
    }

    public void refresh(Listener ccb) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED!");
    }

    private void clear() {
        authors.clear();
    }

    private void dump(ScanCollectionState scs) throws MusicCollectionException {
        Connection conn = null;
        Statement stmt = null;
        PreparedStatement pstmtAuthors = null;
        PreparedStatement pstmtAlbums = null;
        PreparedStatement pstmtSongs = null;
        int forEachCount;
        scs.ccb.processStarted("Guardando la colección");
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            pstmtAuthors = conn.prepareStatement("INSERT INTO AUTHORS(id,name) " + "VALUES(?,?)");
            pstmtAlbums = conn.prepareStatement("INSERT INTO ALBUMS(id,name,coverFile,authorId) " + "VALUES(?,?,?,?)");
            pstmtSongs = conn.prepareStatement("INSERT INTO SONGS(id,title,file,track,duration,bitrate,channels,bitrateVariable,albumId,authorId,size,lastModified) " + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)");
            int nauthors = scs.authorsMap.size();
            int nalbums = scs.albumsMap.size();
            int nsongs = scs.songs.size();
            int ntotal = nauthors + nalbums + nsongs;
            stmt.execute("DELETE FROM AUTHORS");
            forEachCount = 0;
            for (Author author : scs.authorsMap.values()) {
                pstmtAuthors.setInt(1, author.getId());
                pstmtAuthors.setString(2, author.getName());
                pstmtAuthors.executeUpdate();
                forEachCount++;
                scs.ccb.processRunning(100 * forEachCount / ntotal);
            }
            stmt.execute("DELETE FROM ALBUMS");
            for (Album album : scs.albumsMap.values()) {
                pstmtAlbums.setInt(1, album.getId());
                pstmtAlbums.setString(2, album.getName());
                if (album.getCover() != null) {
                    pstmtAlbums.setString(3, album.getCover().getAbsolutePath());
                } else {
                    pstmtAlbums.setString(3, "");
                }
                pstmtAlbums.setInt(4, album.getAuthor().getId());
                pstmtAlbums.executeUpdate();
                forEachCount++;
                scs.ccb.processRunning(100 * forEachCount / ntotal);
            }
            stmt.execute("DELETE FROM SONGS");
            for (Song song : scs.songs) {
                pstmtSongs.setInt(1, song.getId());
                pstmtSongs.setString(2, song.getTitle());
                pstmtSongs.setString(3, song.getFile().getAbsolutePath());
                pstmtSongs.setInt(4, song.getTrack());
                pstmtSongs.setLong(5, song.getDuration());
                pstmtSongs.setInt(6, song.getBitrate());
                pstmtSongs.setInt(7, song.getChannels());
                pstmtSongs.setBoolean(8, song.isBitrateVariable());
                pstmtSongs.setInt(9, song.getAlbum().getId());
                pstmtSongs.setInt(10, song.getAuthor().getId());
                pstmtSongs.setLong(11, song.getSize());
                pstmtSongs.setLong(12, song.getLastModified());
                pstmtSongs.executeUpdate();
                forEachCount++;
                scs.ccb.processRunning(100 * forEachCount / ntotal);
            }
            conn.commit();
            stmt.execute("SHUTDOWN");
        } catch (Exception e) {
            throw new MusicCollectionException("Cannot save collection database", e);
        } finally {
            if (pstmtAuthors != null) {
                try {
                    pstmtAuthors.close();
                } catch (Exception e2) {
                }
            }
            if (pstmtAlbums != null) {
                try {
                    pstmtAlbums.close();
                } catch (Exception e2) {
                }
            }
            if (pstmtSongs != null) {
                try {
                    pstmtSongs.close();
                } catch (Exception e2) {
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e2) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e2) {
                }
            }
        }
        scs.ccb.processRunning(100);
    }

    private void recursiveFillSongs(ScanCollectionState scs, File dir) {
        String ext;
        File file;
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                file = files[i];
                if (file.isDirectory()) {
                    recursiveFillSongs(scs, file);
                } else if (file.getName().length() > 4) {
                    ext = file.getName().substring(file.getName().length() - 3);
                    if (ext.compareToIgnoreCase("mp3") == 0) {
                        SongImpl song = analyzeSong(scs, file);
                        scs.songs.add(song);
                        scs.ccb.processRunning(100 * scs.songs.size() / scs.nsongs);
                    }
                }
            }
        }
    }

    private int recursiveCountSongs(File dir) {
        int nsongs = 0;
        log.trace("recursiveCountSongs - " + dir.getAbsolutePath());
        String ext;
        File file;
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                file = files[i];
                if (file.isDirectory()) {
                    nsongs += recursiveCountSongs(file);
                } else if (file.getName().length() > 4) {
                    ext = file.getName().substring(file.getName().length() - 3);
                    if (ext.compareToIgnoreCase("mp3") == 0) {
                        log.debug("recursiveCountSongs - count " + file.getAbsolutePath());
                        nsongs++;
                    }
                }
            }
        }
        return nsongs;
    }

    private AuthorImpl createOrGetAuthor(ScanCollectionState scs, String authorName) {
        if (authorName.equals("")) {
            authorName = "Desconocido";
        }
        AuthorImpl author = scs.authorsMap.get(authorName);
        if (author == null) {
            author = new AuthorImpl(scs.nextAuthorId++, authorName);
            scs.authorsMap.put(authorName, author);
        }
        return author;
    }

    private AlbumImpl createOrGetAlbum(ScanCollectionState scs, AuthorImpl author, File dir, String albumName) {
        if (albumName.equals("")) {
            albumName = "Desconocido";
        }
        String key = author.getName() + "#" + albumName;
        AlbumImpl album = scs.albumsMap.get(key);
        if (album == null) {
            album = new AlbumImpl(scs.nextAlbumId++, author, albumName, null);
            author.addAlbum(album);
            scs.albumsMap.put(key, album);
        }
        if (dir != null) {
            File[] covers = dir.listFiles(COVERS_FILTER);
            if (covers != null && covers.length > 0) {
                album.setCover(covers[0]);
            }
        }
        return album;
    }

    private SongImpl analyzeSong(ScanCollectionState scs, File file) {
        SongImpl song;
        try {
            AudioFile mp3 = AudioFileIO.read(file);
            AuthorImpl author = createOrGetAuthor(scs, process(mp3.getTag().getFirstArtist()));
            AlbumImpl album = createOrGetAlbum(scs, author, file.getParentFile(), process(mp3.getTag().getFirstAlbum()));
            int track = 0;
            try {
                track = Integer.valueOf(mp3.getTag().getFirstTrack());
            } catch (NumberFormatException e) {
            }
            song = new SongImpl(scs.nextSongId++, album, author, file, process(mp3.getTag().getFirstTitle()), track, (long) (mp3.getPreciseLength() * 1000), mp3.getChannelNumber(), mp3.getBitrate(), mp3.isVbr(), file.length(), file.lastModified());
            album.addSong(song);
        } catch (Exception e) {
            AuthorImpl author = createOrGetAuthor(scs, "");
            AlbumImpl album = createOrGetAlbum(scs, author, null, "");
            song = new SongImpl(scs.nextSongId++, album, author, file, "", 0, 0, 0, 0, false, file.length(), file.lastModified());
        }
        return song;
    }

    private String process(String value) {
        StringBuilder sb = new StringBuilder();
        for (char c : value.trim().toCharArray()) {
            if (((int) c) != REPLACEMENT_CHAR) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
