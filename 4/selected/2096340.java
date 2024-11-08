package de.cabanis.unific.library.playlist;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileFilter;
import org.apache.log4j.Logger;
import de.cabanis.unific.library.media.MediaFile;
import de.cabanis.unific.ui.utilities.ExtensionFileFilter;

/**
 * @author Nicolas Cabanis
 */
public class Playlist {

    private Logger logger = Logger.getLogger(getClass());

    public static final String TYPE_M3U = "Playlist-Type: m3u";

    public static final String TYPE_PLS = "Playlist-Type: pls";

    private static Set<String> playlistExtensions;

    private static FileFilter playlistFilter;

    private static Set<String> songExtensions;

    private String playlistType;

    private Set<PlaylistEntry> playlistEntries;

    /**
	 * ToDo: JavaDoc
	 * Creates new play-list
	 */
    public Playlist(String playlistType) {
        this.playlistType = playlistType;
        this.playlistEntries = new HashSet<PlaylistEntry>();
    }

    /**
	 * ToDo: JavaDoc
	 * Loads play-list
	 */
    public Playlist(MediaFile playlistFile) throws PlaylistParseException {
        if (songExtensions == null) {
            songExtensions = new HashSet<String>();
            songExtensions.add("wav");
            songExtensions.add("mp2");
            songExtensions.add("mp3");
            songExtensions.add("aac");
            songExtensions.add("ogg");
            songExtensions.add("wma");
        }
        String fileContent = null;
        try {
            FileReader fr = playlistFile.getFileReader();
            StringWriter sw = new StringWriter();
            char[] data = new char[2024];
            int read = 0;
            while ((read = fr.read(data)) > 0) {
                sw.write(data, 0, read);
            }
            fileContent = sw.getBuffer().toString();
        } catch (IOException e) {
            throw new PlaylistParseException("Exception while loading playlist file.", e);
        }
        String extensionRegEx = "";
        for (Iterator ext = songExtensions.iterator(); ext.hasNext(); ) {
            extensionRegEx += ext.next();
            if (ext.hasNext()) {
                extensionRegEx += " | ";
            }
        }
        extensionRegEx = "(" + extensionRegEx + ")";
        Set<PlaylistEntry> result = new HashSet<PlaylistEntry>();
        if (fileContent.startsWith("#EXTM3U")) {
            fileContent = fileContent.substring("#EXTM3U".length());
            playlistType = TYPE_M3U;
            String lineBreak = "(\\r\\n)";
            String patternLine1 = "EXTINF.*";
            String patternLine2 = ".*";
            String patternEntry = patternLine1 + lineBreak + patternLine2;
            Pattern regexEntry = Pattern.compile(lineBreak + "?(" + patternEntry + ")+");
            Matcher matchEntry = regexEntry.matcher(fileContent);
            String unparsedEntry = null;
            while (matchEntry.find()) {
                unparsedEntry = matchEntry.group();
                try {
                    int length = Integer.parseInt(unparsedEntry.substring("#EXTINF:".length() - 1, unparsedEntry.indexOf(',')));
                    String title = unparsedEntry.substring(unparsedEntry.indexOf(',') + 1, unparsedEntry.indexOf("\r\n")).replace('\r', ' ').replace('\n', ' ').trim();
                    File file = new File(unparsedEntry.substring(unparsedEntry.indexOf("\r\n") + 2));
                    result.add(new PlaylistEntry(title, file, length));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } else if (fileContent.startsWith("[playlist]")) {
            playlistType = TYPE_PLS;
        } else {
            throw new PlaylistParseException("Unknown playlist format.");
        }
        this.playlistEntries = result;
    }

    /**
	 * ToDo: JavaDoc
	 */
    public String getPlaylistType() {
        return playlistType;
    }

    /**
	 * ToDo: JavaDoc
	 */
    public void writePlaylist(MediaFile destination) {
        try {
            FileWriter writer = destination.getFileWriter();
            writer.write("#EXTM3U\r\n");
            String entry = null;
            PlaylistEntry playlistEntry = null;
            for (Iterator iterator = playlistEntries.iterator(); iterator.hasNext(); ) {
                playlistEntry = (PlaylistEntry) iterator.next();
                entry = "#EXTINF:" + playlistEntry.getLength() + "," + playlistEntry.getTitle() + "\r\n" + playlistEntry.getFile().getAbsolutePath() + "\r\n";
                writer.write(entry);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * ToDo: JavaDoc
	 */
    public Set<PlaylistEntry> existentPlaylistEntries() {
        Set<PlaylistEntry> existentEntries = new HashSet<PlaylistEntry>();
        for (PlaylistEntry entry : playlistEntries) {
            if (entry.getFile().exists()) {
                existentEntries.add(entry);
            }
        }
        return existentEntries;
    }

    /**
	 * ToDo: JavaDoc
	 */
    public Set<PlaylistEntry> missingPlaylistEntries() {
        Set<PlaylistEntry> missingEntries = new HashSet<PlaylistEntry>();
        for (PlaylistEntry entry : playlistEntries) {
            if (entry.getFile().exists()) {
                missingEntries.add(entry);
            }
        }
        return missingEntries;
    }

    /**
	 * ToDo: JavaDoc
	 */
    public void addPlaylistEntries(Set<PlaylistEntry> entries) {
        playlistEntries.addAll(entries);
    }

    /**
	 * ToDo: JavaDoc
	 */
    public void removeMediaSourceSet(Set<PlaylistEntry> entries) {
        playlistEntries.removeAll(entries);
    }

    /**
	 * ToDo: JavaDoc
	 */
    public void removeAllPlaylistEntriesBut(Set<PlaylistEntry> entries) {
        Set<PlaylistEntry> toRemove = new HashSet<PlaylistEntry>();
        for (PlaylistEntry playlistEntry : playlistEntries) {
            if (entries.contains(playlistEntry)) {
            } else {
                toRemove.add(playlistEntry);
            }
        }
        playlistEntries.removeAll(toRemove);
    }

    /**
	 * ToDo: JavaDoc
	 */
    public Set<PlaylistEntry> playlistEntries() {
        return playlistEntries;
    }

    /**
	 * ToDo: JavaDoc
	 */
    public static Set<String> playlistExtensions() {
        if (playlistExtensions == null) {
            playlistExtensions = new HashSet<String>();
            playlistExtensions.add("m3u");
            playlistExtensions.add("pls");
        }
        return playlistExtensions;
    }

    /**
	 * ToDo: JavaDoc
	 */
    public static FileFilter playlistFilter() {
        if (playlistFilter == null) {
            playlistFilter = new ExtensionFileFilter("Playlist", playlistExtensions());
        }
        return playlistFilter;
    }
}
