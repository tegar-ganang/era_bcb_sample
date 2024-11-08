package org.auramp.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.auramp.io.logging.AuraLogger;
import org.auramp.plugin.PluginImpl;
import org.auramp.util.OSToolkit;
import com.trolltech.qt.gui.QIcon;
import com.trolltech.qt.gui.QPixmap;

public class ResourceManager {

    private static HashMap<String, QPixmap> pixmaps = new HashMap<String, QPixmap>();

    private static HashMap<QPixmap, QIcon> icons = new HashMap<QPixmap, QIcon>();

    private static String storeLocation = null;

    private static boolean multiInstance = false;

    private static boolean preloaded = false;

    public static enum StandardIcons {

        OpenFile("classpath:org/auramp/resources/icons/document_open.png"), OpenURL("classpath:org/auramp/resources/icons/document_open_remote.png"), OpenDVD("classpath:org/auramp/resources/icons/cdrom_unmount.png"), Play("classpath:org/auramp/resources/icons/player_play.png"), Pause("classpath:org/auramp/resources/icons/player_pause.png"), Stop("classpath:org/auramp/resources/icons/player_stop.png"), Rewind("classpath:org/auramp/resources/icons/player_rew.png"), FastForward("classpath:org/auramp/resources/icons/player_fwd.png"), Next("classpath:org/auramp/resources/icons/player_end.png"), Previous("classpath:org/auramp/resources/icons/player_start.png"), RepeatSingle("classpath:org/auramp/resources/icons/repeat_single.png"), RepeatAll("classpath:org/auramp/resources/icons/repeat.png"), Random("classpath:org/auramp/resources/icons/roll.png"), FullScreen("classpath:org/auramp/resources/icons/window_fullscreen.png"), Lock("classpath:org/auramp/resources/icons/locked.png"), PlayList("classpath:org/auramp/resources/icons/media_playlist.png"), PlayListOpen("classpath:org/auramp/resources/icons/project_open.png"), PlayListClear("classpath:org/auramp/resources/icons/media_playlist_clear.png"), AddFile("classpath:org/auramp/resources/icons/fileadd.png"), AddFolder("classpath:org/auramp/resources/icons/folderadd.png"), Radio("classpath:org/auramp/resources/icons/radio.png"), PlayTime("classpath:org/auramp/resources/icons/player_time.png"), Remove("classpath:org/auramp/resources/icons/remove.png"), Console("classpath:org/auramp/resources/icons/console.png"), Info("classpath:org/auramp/resources/icons/info.png"), Settings("classpath:org/auramp/resources/icons/configure.png"), Volume("classpath:org/auramp/resources/icons/player_volume.png"), Mute("classpath:org/auramp/resources/icons/player_volume_muted.png"), Music("classpath:org/auramp/resources/icons/music.png"), FolderCyan("classpath:org/auramp/resources/icons/folder_cyan.png"), FolderGreen("classpath:org/auramp/resources/icons/folder_green.png"), Add("classpath:org/auramp/resources/icons/edit_add.svg"), Delete("classpath:org/auramp/resources/icons/edit_delete.svg"), Save("classpath:org/auramp/resources/icons/save.png"), Rename("classpath:org/auramp/resources/icons/edit_rename.svg"), Network("classpath:org/auramp/resources/icons/network.svg"), Exit("classpath:org/auramp/resources/icons/exit.png"), Title("classpath:org/auramp/resources/icons/title.png"), Track("classpath:org/auramp/resources/icons/track.png"), Album("classpath:org/auramp/resources/icons/album.svg"), Artist("classpath:org/auramp/resources/icons/artist.svg"), General("classpath:org/auramp/resources/icons/gear.png"), Display("classpath:org/auramp/resources/icons/display.png"), SubOSD("classpath:org/auramp/resources/icons/sub.png"), Shortcuts("classpath:org/auramp/resources/icons/key_enter.png"), Advanced("classpath:org/auramp/resources/icons/advanced.png"), Plugin("classpath:org/auramp/resources/icons/plugin.png"), Audio("classpath:org/auramp/resources/icons/music.png"), Video("classpath:org/auramp/resources/icons/video.png"), Genre("classpath:org/auramp/resources/icons/genre.svg"), TrackNumber("classpath:org/auramp/resources/icons/track_number.svg"), Year("classpath:org/auramp/resources/icons/year.svg"), Comment("classpath:org/auramp/resources/icons/comment.svg"), ClearAndAdd("classpath:org/auramp/resources/icons/clear_and_add.png"), PlaylistWide("classpath:org/auramp/resources/icons/network.png"), FavoriteAdd("classpath:org/auramp/resources/icons/bookmark_add.png"), Favorite("classpath:org/auramp/resources/icons/bookmark.png"), Filter("classpath:org/auramp/resources/icons/filter.png"), Interface("classpath:org/auramp/resources/icons/interface.png"), LeftArrow("classpath:org/auramp/resources/icons/back.png"), RightArrow("classpath:org/auramp/resources/icons/forward.png"), DownArrow("classpath:org/auramp/resources/icons/down.png"), UpArrow("classpath:org/auramp/resources/icons/up.png");

        private String path;

        private StandardIcons(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static enum StandardImages {

        NoCover("classpath:org/auramp/resources/graphics/nocover.png"), Logo("classpath:org/auramp/resources/graphics/logo.png");

        private String path;

        private StandardImages(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    public static void preloadResources() {
        if (preloaded) {
            return;
        }
        for (StandardIcons ico : StandardIcons.values()) {
            loadIcon(ico);
        }
        AuraLogger.logVerbose("Preloaded application resources.");
        preloaded = true;
    }

    public static QPixmap loadPixmap(String path) {
        if (pixmaps.get(path) != null) {
            return pixmaps.get(path);
        }
        QPixmap map = new QPixmap(path);
        pixmaps.put(path, map);
        return map;
    }

    public static QIcon loadIcon(StandardIcons icon) {
        QIcon ret = loadIcon(icon.getPath());
        if (ret.isNull()) {
            AuraLogger.logWarnng("Failed to load standard icon " + icon);
        }
        return ret;
    }

    public static QIcon loadIcon(String path) {
        QIcon ret = loadIcon(loadPixmap(path));
        if (ret.isNull()) {
            AuraLogger.logWarnng("Failed to load icon from path " + path);
        }
        return ret;
    }

    public static QIcon loadIcon(QPixmap map) {
        if (icons.get(map) != null) {
            return icons.get(map);
        }
        QIcon icon = new QIcon(map);
        icons.put(map, icon);
        return icon;
    }

    public static QPixmap loadPluginPixmap(PluginImpl plugin, String path) {
        try {
            InputStream in = plugin.getClass().getClassLoader().getResourceAsStream(path);
            int idx = path.lastIndexOf('.');
            String ext = "";
            if (idx >= 0 && idx < path.length() - 2) {
                ext = path.substring(idx + 1);
            }
            File f = File.createTempFile("aura_plugin_extract", ext);
            FileOutputStream out = new FileOutputStream(f);
            byte buffer[] = new byte[1024];
            int read = in.read(buffer);
            while (read > 0) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
            out.close();
            QPixmap pix = new QPixmap(f.getAbsolutePath());
            f.delete();
            return pix;
        } catch (IOException ex) {
            return null;
        }
    }

    public static String storeLocation() {
        return storeLocation;
    }

    public static void setStoreLocation(String storeLoca) {
        storeLocation = storeLoca;
    }

    public static void setMultiInstance(boolean inst) {
        multiInstance = inst;
    }

    public static boolean multiInstance() {
        return multiInstance;
    }

    public static File getExecDir() {
        try {
            String mpath = new ResourceManager().getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
            if (mpath.toLowerCase().endsWith(".jar")) {
                mpath = mpath.substring(0, mpath.lastIndexOf(File.separatorChar));
            }
            File fpath = new File(mpath);
            if (!new File(fpath.getAbsolutePath() + File.separatorChar + "lib").exists()) {
                fpath = fpath.getParentFile();
            }
            if (!new File(fpath.getAbsolutePath() + File.separatorChar + "lib").exists()) {
                return new File("");
            }
            return fpath;
        } catch (Exception ex) {
            return new File(".");
        }
    }

    public static File getPlaylistDir() {
        return new File(getDataDir().getAbsolutePath() + "/playlists");
    }

    public static File getDataDir() {
        return new File(OSToolkit.getUserHomeDir() + "/.aura");
    }

    private ResourceManager() {
    }
}
