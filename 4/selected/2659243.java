package org.elmarweber.sf.appletrailerfs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elmarweber.sf.appletrailerfs.FileSystemEntry.Type;
import org.elmarweber.sf.appletrailerfs.PathNames.PathNameCounter;

/**
 * Parent class for all movie database generators.
 * 
 * 
 * @author Elmar Weber (appletrailerfs@elmarweber.org)
 */
public abstract class CreateMovieDatabase {

    protected static final String MISC_IMAGES_SUFFIX = "/images/misc";

    private static Log log = LogFactory.getLog(CreateMovieDatabase.class);

    protected static List<FileSystemEntry> generateGenreFolderIcons(PathNameCounter counter) throws IOException {
        List<FileSystemEntry> entries = new ArrayList<FileSystemEntry>();
        for (String genre : counter.genreIndex.keySet()) {
            File genreImage = new File("./data/genres/" + genre.replace(' ', '_') + ".jpg");
            if (genreImage.exists()) {
                entries.add(createEntryFromFile(genreImage, PathNames.getGenrePath(genre), "folder.jpg", MISC_IMAGES_SUFFIX));
            } else {
                log.error("Could not find image for genre " + genre + " at expected location " + genreImage);
            }
        }
        return entries;
    }

    protected static List<FileSystemEntry> generateDefaultFolderIcons() throws IOException {
        List<FileSystemEntry> entries = new ArrayList<FileSystemEntry>();
        entries.add(createEntryFromFile(new File("./data/atfs_index.jpg"), PathNames.ROOT_PATH, "folder.jpg", MISC_IMAGES_SUFFIX));
        entries.add(createEntryFromFile(new File("./data/newest.jpg"), PathNames.NEWEST_PATH, "folder.jpg", MISC_IMAGES_SUFFIX));
        entries.add(createEntryFromFile(new File("./data/just_hd.jpg"), PathNames.JUST_HD_PATH, "folder.jpg", MISC_IMAGES_SUFFIX));
        entries.add(createEntryFromFile(new File("./data/by_genre.jpg"), PathNames.GENRES_PATH, "folder.jpg", MISC_IMAGES_SUFFIX));
        return entries;
    }

    protected static FileSystemEntry createEntryFromFile(File file, String path, String filename, String ftpSuffix) throws IOException {
        String md5Name = FtpHosting.hashFileForFtp(file);
        String url = FtpHosting.getUrl(ftpSuffix + "/" + md5Name);
        FileUtils.copyFile(file, new File(FtpHosting.getLocalPath(ftpSuffix + "/" + md5Name)));
        FileSystemEntry entry = new FileSystemEntry();
        entry.setType(Type.REMOTE);
        entry.setFilename(filename);
        if (path != null) {
            entry.getPaths().add(path);
        }
        entry.setContentLength(file.length());
        entry.setUrl(url);
        return entry;
    }

    protected static void addPaths(List<FileSystemEntry> entries, String... paths) {
        for (FileSystemEntry entry : entries) {
            if (entry != null) {
                for (String path : paths) {
                    entry.getPaths().add(path);
                }
            }
        }
    }

    protected static List<FileSystemEntry> generateTrailerEntries(JSONObject movie) {
        List<FileSystemEntry> entries = new ArrayList<FileSystemEntry>();
        for (int i = 0; i < movie.getJSONArray("trailers").size(); i++) {
            JSONObject trailer = movie.getJSONArray("trailers").getJSONObject(i);
            FileSystemEntry entry = new FileSystemEntry();
            entry.setType(Type.STREAM);
            entry.setFilename(PathNames.getTrailerFilename(movie, trailer));
            entry.setContentLength(trailer.getLong("contentlength"));
            entry.setUrl(trailer.getString("movurl"));
            entries.add(entry);
        }
        return entries;
    }

    protected static FileSystemEntry getMoviePosterEntry(JSONObject movie) {
        FileSystemEntry entry = new FileSystemEntry();
        entry.setType(Type.REMOTE);
        entry.setFilename("folder.jpg");
        entry.setContentLength(movie.getLong("postersize"));
        entry.setUrl(movie.getString("poster"));
        return entry;
    }

    protected static void saveDatabase(String name, List<FileSystemEntry> entries) throws IOException {
        FileUtils.writeStringToFile(new File("./movies-" + name + ".json"), JSONArray.fromObject(entries).toString());
        FileUtils.writeStringToFile(new File("./movies-" + name + "-formatted.json"), JSONArray.fromObject(entries).toString(4));
        log.info("Created " + name + " movie database");
    }
}
