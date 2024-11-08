package org.jampa.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import javax.activation.MimetypesFileTypeMap;
import org.eclipse.jface.util.Util;
import org.jampa.controllers.Controller;
import org.jampa.engine.PlaybackEngine;
import org.jampa.logging.Log;

public class SystemUtils {

    public enum PlaylistFormat {

        XSPF, M3U
    }

    /**
	 * The current file separator character.
	 */
    public static final String fileSeparator = System.getProperty("file.separator");

    public static final String currentDir = System.getProperty("user.dir");

    public static String userHome = System.getProperty("user.home");

    public static String applicationDirectory = getApplicationDirectory();

    public static String playlistDirectory = applicationDirectory + "playlists" + fileSeparator;

    public static String databaseDirectory = applicationDirectory + "database" + fileSeparator;

    public static String presetFile = applicationDirectory + "presets.properties";

    public static String podcastDirectory = applicationDirectory + "podcasts" + fileSeparator;

    public static String podcastDowloadDirectory = podcastDirectory + "cache" + fileSeparator;

    public static String radioFile = applicationDirectory + "radiolist.xml";

    public static final String playlistM3UExtension = ".m3u";

    public static final String playlistXSPFExtension = ".xspf";

    public static final String podcastExtension = ".xml";

    public static final String lineTerminator = getSystemLineTerminator();

    public static final String mplayerWindowsName = "mplayer.exe";

    public static final String mplayerWindowsDirectory = "mplayer";

    public static final String mplayerLinuxPath = "/usr/bin/mplayer";

    public static void redefineUserHome(String newUserHome) {
        userHome = newUserHome;
        applicationDirectory = userHome + fileSeparator + Constants.APP_NAME + fileSeparator;
        playlistDirectory = applicationDirectory + "playlists" + fileSeparator;
        databaseDirectory = applicationDirectory + "database" + fileSeparator;
        presetFile = applicationDirectory + "presets.properties";
        podcastDirectory = applicationDirectory + "podcasts" + fileSeparator;
        podcastDowloadDirectory = podcastDirectory + "cache" + fileSeparator;
        radioFile = applicationDirectory + "radiolist.xml";
    }

    private static String getApplicationDirectory() {
        String result = null;
        String xdgConfigHomePath = System.getenv("XDG_CONFIG_HOME");
        if ((xdgConfigHomePath != null) && (!xdgConfigHomePath.isEmpty())) {
            if (!xdgConfigHomePath.endsWith(fileSeparator)) {
                xdgConfigHomePath += fileSeparator;
            }
            result = xdgConfigHomePath + Constants.APP_NAME + fileSeparator;
        } else {
            if (Util.isWindows()) {
                result = System.getProperty("user.home") + fileSeparator + "Application Data" + fileSeparator + Constants.APP_NAME + fileSeparator;
            } else {
                result = System.getProperty("user.home") + fileSeparator + ".config" + fileSeparator + Constants.APP_NAME + fileSeparator;
            }
        }
        return result;
    }

    /**
	 * Gets the system line terminator.
	 * 
	 * @return the system line terminator
	 */
    private static String getSystemLineTerminator() {
        if (Util.isWindows()) {
            return "\r\n";
        }
        return "\n";
    }

    /**
	 * Purge a directory on disk.
	 * @param directory The directory to purge.
	 */
    private static void purgeDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    purgeDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
            directory.delete();
        }
    }

    /**
	 * Purge current configuration folder.
	 */
    public static void purgeConfiguration() {
        Log.getInstance(SystemUtils.class).info("Purging configuration.");
        File appDir = new File(applicationDirectory);
        purgeDirectory(appDir);
    }

    /**
	 * Create default configuration content.
	 */
    public static void createConfiguration() {
        Log.getInstance(SystemUtils.class).info("Checking configuration.");
        File appDir = new File(applicationDirectory);
        if (!appDir.exists()) appDir.mkdir();
        File playlistDir = new File(playlistDirectory);
        if (!playlistDir.exists()) playlistDir.mkdir();
        File podcastsDir = new File(podcastDirectory);
        if (!podcastsDir.exists()) podcastsDir.mkdir();
        File podcastsDownloadDir = new File(podcastDowloadDirectory);
        if (!podcastsDownloadDir.exists()) podcastsDownloadDir.mkdir();
        File defaultPlaylist = new File(playlistDirectory + Constants.DEFAULT_PLAYLIST_ID + playlistXSPFExtension);
        if (!defaultPlaylist.exists()) {
            try {
                defaultPlaylist.createNewFile();
                FileWriter writer = new FileWriter(defaultPlaylist);
                writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><playlist xmlns=\"http://xspf.org/ns/0/\" version=\"1\"><trackList/></playlist>" + lineTerminator);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.getInstance(SystemUtils.class).error("Unable to create default playlist file.");
            }
        }
        File preset = new File(presetFile);
        if (!preset.exists()) {
            try {
                FileWriter writer = new FileWriter(preset);
                writer.append("Classic=-3.8:0.0:0.0:0.0:0.0:0.0:-1.6:-1.6:-1.6:-2.2" + lineTerminator);
                writer.append("Club=0.0:0.0:0.7:1.2:1.2:1.2:0.7:0.0:0.0:0.0" + lineTerminator);
                writer.append("Dance=1.9:1.42:0.3:0.0:0.0:-1.42:-1.7:-1.7:0.0:0.0" + lineTerminator);
                writer.append("Bass=2.0:2.0:2.0:1.25:0.375:-0.875:-1.75:-2.125:-2.25:-2.25" + lineTerminator);
                writer.append("Bass and treble=1.5:1.25:0.125:-1.5:-1.0:0.375:1.75:2.25:2.5:2.5" + lineTerminator);
                writer.append("Treble=-2.0:-2.0:-2.0:-0.875:0.625:2.25:3.25:3.25:3.25:3.5" + lineTerminator);
                writer.append("Headphones=1.0:2.25:1.0:-0.75:-0.5:0.375:1.0:2.0:2.625:3.0" + lineTerminator);
                writer.append("Hall=2.125:2.125:1.25:1.25:0.125:-1.0:-1.0:-1.0:0.125:0.125" + lineTerminator);
                writer.append("Live=-1.0:0.125:0.875:1.125:1.25:1.25:0.875:0.625:0.625:0.5" + lineTerminator);
                writer.append("Party=1.5:1.5:0.125:0.125:0.125:0.125:0.125:0.125:1.5:1.5" + lineTerminator);
                writer.append("Pop=-0.375:1.0:1.5:1.625:1.125:-0.25:-0.5:-0.5:-0.375:-0.375" + lineTerminator);
                writer.append("Rock=1.625:1.0:-1.125:-1.625:-0.75:0.875:1.875:2.25:2.25:2.25" + lineTerminator);
                writer.append("Soft=1.0:0.375:-0.25:-0.5:-0.25:0.875:1.75:2.0:2.25:2.5" + lineTerminator);
                writer.append("Techno=1.625:1.25:0.125:-1.125:-1.0:0.125:1.625:2.0:2.0:1.875" + lineTerminator);
                writer.flush();
                writer.close();
            } catch (IOException e) {
                Log.getInstance(SystemUtils.class).error("Unable to create preset file.");
            }
        }
        File radioList = new File(radioFile);
        if (!radioList.exists()) {
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/jampa/model/radio/io/radiolist.xml");
            if (stream != null) {
                try {
                    FileWriter writer = new FileWriter(radioList);
                    InputStreamReader streamReader = new InputStreamReader(stream);
                    BufferedReader buffer = new BufferedReader(streamReader);
                    String line = null;
                    while (null != (line = buffer.readLine())) {
                        writer.write(line + "\n");
                    }
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    Log.getInstance(SystemUtils.class).error("Unable to create radiolist file.");
                }
            }
        }
    }

    public static boolean isValidPlaylist(String fileName) {
        fileName = fileName.toLowerCase();
        return fileName.endsWith(playlistXSPFExtension) || fileName.endsWith(playlistM3UExtension);
    }

    public static boolean isFile(String fileName) {
        File testFile = new File(fileName);
        if ((testFile.exists()) && (testFile.isFile())) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidAudioFile(String fileName) {
        fileName = fileName.toLowerCase();
        if ((Controller.getInstance().getEngine().getEngineType() == PlaybackEngine.GSTREAMER) || (Controller.getInstance().getEngine().getEngineType() == PlaybackEngine.MPLAYER)) {
            return fileName.endsWith(".mp3") || fileName.endsWith(".ogg") || fileName.endsWith(".m4a") || fileName.endsWith(".aac") || fileName.endsWith(".wav") || fileName.endsWith(".wma") || fileName.endsWith(".flac") || fileName.endsWith(".ape") || fileName.endsWith(".mpc") || fileName.endsWith(".ra") || fileName.endsWith(".rm") || fileName.endsWith(".mp+") || fileName.endsWith(".mac");
        } else {
            return fileName.endsWith(".mp3") || fileName.endsWith(".ogg") || fileName.endsWith(".wav");
        }
    }

    public static String escapeAccent(String s) {
        String temp = Normalizer.normalize(s, Normalizer.Form.NFKD);
        return temp.replaceAll("[^\\p{ASCII}]", "");
    }

    public static void copyFile(File src, File dest, boolean notifyUserOnError) {
        if (src.exists()) {
            try {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
                byte[] read = new byte[128];
                int len = 128;
                while ((len = in.read(read)) > 0) out.write(read, 0, len);
                out.flush();
                out.close();
                in.close();
            } catch (IOException e) {
                String message = "Error while copying " + src.getAbsolutePath() + " to " + dest.getAbsolutePath() + " : " + e.getMessage();
                if (notifyUserOnError) {
                    Log.getInstance(SystemUtils.class).warnWithUserNotification(message);
                } else {
                    Log.getInstance(SystemUtils.class).warn(message);
                }
            }
        } else {
            String message = "Unable to copy file: source does not exists: " + src.getAbsolutePath();
            if (notifyUserOnError) {
                Log.getInstance(SystemUtils.class).warnWithUserNotification(message);
            } else {
                Log.getInstance(SystemUtils.class).warn(message);
            }
        }
    }

    public String getMIMEType(String fileName) {
        return new MimetypesFileTypeMap().getContentType(fileName);
    }

    public String getMIMEType(File file) {
        return new MimetypesFileTypeMap().getContentType(file);
    }
}
