package mylibrary;

import db.Database;
import db.TexturesDB;
import java.sql.*;
import java.util.*;
import java.net.*;
import utilities.*;
import java.io.*;
import java.text.SimpleDateFormat;
import org.apache.commons.io.FileUtils;

public class ThumbCleaner extends Config implements Constants {

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                Config.log(INFO, "Attempting to auto-determine base directory. If this fails, specify the base directory of this program as a command line parameter.");
                BASE_PROGRAM_DIR = getBaseDirectory();
            } else BASE_PROGRAM_DIR = args[0];
            Config.log(NOTICE, "Base program dir = " + BASE_PROGRAM_DIR);
            ThumbCleaner c = new ThumbCleaner();
        } catch (Exception x) {
            log(ERROR, "General error: " + x, x);
        } finally {
            end();
        }
    }

    boolean TESTING = false;

    boolean READ_FROM_CACHE = false;

    final String[] THUMB_EXTS = new String[] { "jpg", "png", "tbn", "dds" };

    final String[] folderImages = new String[] { "folder.jpg", "cover.jpg", "thumb.jpg" };

    static final String CONCAT = "CONCAT";

    static final SimpleDateFormat texturesSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    String THUMB_DIR;

    static Set<Character> validCRCChars = new HashSet<Character>();

    Map<String, String> validHashs = new HashMap<String, String>();

    Database dbVideo, dbMusic, dbTextures;

    private long textureDeleteSizeKB = 0, thumbDeleteSizeKB = 0;

    private int thumbsDeletedCount = 0, texturesDeletedCount = 0;

    public ThumbCleaner() {
        super(THUMB_CLEANER);
        try {
            final char[] crcCharArray = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
            for (char c : crcCharArray) validCRCChars.add(c);
            texturesSDF.setTimeZone(TimeZone.getTimeZone("UTC"));
            if (!loadConfig()) throw new Exception("Failed to initialize...");
            THUMB_DIR = XBMC_THUMBNAILS_FOLDERS.get(0);
            File thumbDir = new File(THUMB_DIR);
            if (thumbDir.isDirectory()) {
                cleanLibraryThumbsAndFanart();
                if (CLEAN_TEXTURES) cleanTextures();
                setShortLogDesc("CleanSummary");
                log(NOTICE, "Total files " + (SIMULATION ? "SIMLUATED" : "") + " deleted = " + (thumbsDeletedCount + texturesDeletedCount) + " images totaling " + ((thumbDeleteSizeKB + textureDeleteSizeKB) / 1024) + " MB");
                logFileExpiration();
                setShortLogDesc("Exit");
                Config.log(INFO, "Exiting...");
                setShortLogDesc(null);
            } else throw new Exception("Thumbnail directory does not exist: " + thumbDir);
        } catch (Exception x) {
            Config.log(ERROR, "General exception: " + x, x);
            end();
            System.exit(1);
        }
    }

    private void cleanTextures() {
        setShortLogDesc("CleanTextures");
        java.util.Date cutoffDate = new java.util.Date((long) (System.currentTimeMillis() - (ONE_DAY * TEXTURE_LAST_USED_DAYS_THRESHOLD)));
        log(NOTICE, "Will clean textures that haven't been used in the past " + TEXTURE_LAST_USED_DAYS_THRESHOLD + " days (not used since " + log_sdf.format(cutoffDate) + ") " + TEXTURE_AND_OR_FOR_THRESHOLD + " have been used less than " + TEXTURE_USE_COUNT_THRESHOLD + " times.");
        if (TEXTURE_LAST_USED_DAYS_THRESHOLD == 0.0 && TEXTURE_USE_COUNT_THRESHOLD == 0) {
            Config.log(WARNING, "No textures will be cleaned because last used days threshold is set to zero and use count threshold is set to zero.");
            return;
        }
        if (TEXTURE_USE_COUNT_THRESHOLD == 0 && TEXTURE_AND_OR_FOR_THRESHOLD.equals("and")) {
            Config.log(WARNING, "No textures will be cleaned because use count threshold is set to zero and \"and\" was specified.");
            return;
        }
        if (TEXTURE_LAST_USED_DAYS_THRESHOLD == 0.0 && TEXTURE_AND_OR_FOR_THRESHOLD.equals("and")) {
            Config.log(WARNING, "No textures will be cleaned because last used days threshold is set to zero and \"and\" was specified.");
            return;
        }
        dbTextures = new TexturesDB(sqlLiteTexturesDBPath);
        if (dbTextures.isConnected()) Config.log(Config.INFO, "Connection to Textures database \"" + sqlLiteTexturesDBPath + "\" was successful"); else {
            Config.log(Config.ERROR, "Connection to XBMC " + DATABASE_TYPE + " database " + sqlLiteTexturesDBPath + " failed.");
            Config.log(WARNING, "Will not clean textures.");
            return;
        }
        Config.log(NOTICE, "Determining which textures hash's are valid based on configuration parameters.");
        String sql = "SELECT id, cachedurl, usecount, lastusetime FROM texture " + "ORDER BY lastusetime desc";
        Set<String> validTextureHashs = new HashSet<String>();
        try {
            ResultSet rs = dbTextures.executeQuery(sql, null);
            int deleteCount = 0, totalTextureCount = 0, validCount = 0, failCount = 0;
            final long NOW = System.currentTimeMillis();
            while (rs.next()) {
                totalTextureCount++;
                int id = rs.getInt("id");
                String location = rs.getString("cachedurl");
                java.util.Date lastUsed = getTextureLastUsedTimeLocal(rs.getString("lastusetime"));
                double daysAgo = (NOW - lastUsed.getTime()) / ((double) ONE_DAY);
                int useCount = rs.getInt("usecount");
                if (!tools.valid(location)) {
                    Config.log(WARNING, "Found a texture (id:" + id + ") with no cachedurl/location specified, skipping");
                    failCount++;
                    continue;
                }
                boolean delete = false;
                if (TEXTURE_AND_OR_FOR_THRESHOLD.equals("or")) {
                    if ((TEXTURE_LAST_USED_DAYS_THRESHOLD == 0.0 || daysAgo > TEXTURE_LAST_USED_DAYS_THRESHOLD) || (TEXTURE_USE_COUNT_THRESHOLD == 0 || useCount < TEXTURE_USE_COUNT_THRESHOLD)) delete = true;
                } else {
                    if ((TEXTURE_LAST_USED_DAYS_THRESHOLD == 0.0 || daysAgo > TEXTURE_LAST_USED_DAYS_THRESHOLD) && (TEXTURE_USE_COUNT_THRESHOLD == 0 || useCount < TEXTURE_USE_COUNT_THRESHOLD)) delete = true;
                }
                if (delete) {
                    Config.log(DEBUG, "Will " + (SIMULATION ? "simulate" : "") + " delete: " + tools.tfl(location, 15) + " last used: " + log_sdf.format(lastUsed) + " (" + tools.toTwoDecimals(daysAgo) + " days ago)" + ", use count: " + useCount);
                    deleteCount++;
                } else {
                    String hash = location.toLowerCase();
                    int slashIndx = hash.indexOf("/");
                    if (slashIndx != -1) hash = hash.substring(slashIndx + 1, hash.length());
                    int dotIndx = hash.indexOf(".");
                    if (dotIndx != -1) hash = hash.substring(0, dotIndx);
                    if (!isValidHash(hash)) {
                        Config.log(WARNING, "Cannot parse valid CRC hash from \"" + location + "\". Found \"" + hash + "\". This may result in extra textures being deleted.");
                        failCount++;
                        continue;
                    }
                    validTextureHashs.add(hash);
                    Config.log(DEBUG, "Found valid hash, saving for later (" + hash + "). " + " last used: " + log_sdf.format(lastUsed) + " (" + tools.toTwoDecimals(daysAgo) + " days ago)" + ", use count: " + useCount);
                    validCount++;
                }
            }
            double percentDelete = (((double) deleteCount) / ((double) totalTextureCount)) * 100.0;
            Config.log(INFO, tools.toTwoDecimals(percentDelete) + "% of the textures in Textures.db were determined to be old. " + "Of " + totalTextureCount + " total textures in Textures.db, " + deleteCount + " textures are old and can be cleaned. " + validCount + " textures are valid based on configuration thresholds. Failed to handle " + failCount + " textures.");
        } catch (Exception x) {
            log(ERROR, "Failed which determining which textures to clean: " + x, x);
        } finally {
            dbTextures.closeStatement();
        }
        final String[] textureExts = new String[] { "png", "jpg", "dds", "tbn" };
        Config.log(NOTICE, "Determining which texture files are old based on valid hashs found from Textures.db.");
        List<File> texturesToDelete = new ArrayList<File>();
        List<File> allTextureFiles = new ArrayList<File>();
        log(INFO, "Reading all textures from disk. This may take a while.");
        for (Character c : validCRCChars) {
            File subfolder = new File(THUMB_DIR + SEP + c);
            if (!subfolder.isDirectory()) {
                Config.log(WARNING, "No texture subdirectory found at: " + subfolder + ". Cannot clean texture out from this subdirectory.");
                continue;
            }
            Collection<File> filesInSubDir = FileUtils.listFiles(subfolder, textureExts, false);
            log(DEBUG, "Found " + filesInSubDir.size() + " textures in " + subfolder);
            allTextureFiles.addAll(filesInSubDir);
        }
        log(INFO, "Found " + allTextureFiles.size() + " total texture files in the filesystem, will determine which ones can be deleted.");
        log(NOTICE, "Finding all texture files that don't have a vald CRC hash.");
        for (File f : allTextureFiles) {
            String hash = getHashFromFile(f);
            if (!isValidHash(hash)) {
                log(WARNING, "Cannot determine hash from file at: " + f + ". It will not be cleaned.");
                continue;
            }
            boolean isValidTexture = validTextureHashs.contains(hash);
            if (isValidTexture) {
                log(DEBUG, "This texture is valid and will not be cleaned: " + f);
            } else {
                texturesToDelete.add(f);
            }
        }
        double percentDelete = (((double) texturesToDelete.size()) / ((double) allTextureFiles.size())) * 100.0;
        log(NOTICE, tools.toTwoDecimals(percentDelete) + "% of the textures on the filesystem can be cleaned. " + "There are " + allTextureFiles.size() + " total texture files, and " + texturesToDelete.size() + " can be cleaned.");
        setShortLogDesc("DeleteTextures");
        if (SIMULATION) {
            log(NOTICE, "Since this is a simulation, no files will be deleted and the Textures.db will not be modified!");
            spotCheck("Textures", texturesToDelete, SPOT_CHECK_MAX_IMAGES);
        }
        int successDeleteCount = 0;
        int failCount = 0;
        long deleteSizeKB = 0;
        Set<String> hashsDeletedFromDB = new HashSet<String>();
        for (File textureFile : texturesToDelete) {
            String hash = getHashFromFile(textureFile);
            if (!isValidHash(hash)) {
                log(WARNING, "The hash \"" + hash + "\" found from: " + textureFile + " is not valid. This files will not be cleaned.");
                continue;
            }
            boolean alreadyDeletedFromDatabase = hashsDeletedFromDB.contains(hash);
            long fileSizeKB = textureFile.length() / 1024;
            boolean deletedFromFileSystem = (SIMULATION ? true : textureFile.delete());
            if (!deletedFromFileSystem) {
                Config.log(WARNING, "Failed to delete texture from filesystem, skipping cleaning for this file: " + textureFile);
                failCount++;
                continue;
            }
            boolean deletedFromDB;
            if (SIMULATION) deletedFromDB = true; else if (alreadyDeletedFromDatabase) deletedFromDB = true; else {
                int deleteCount = dbTextures.executeMultipleUpdate("DELETE FROM texture WHERE cachedurl LIKE ?", tools.params("%" + hash + "%"));
                if (deleteCount > 0) log(DEBUG, "Successfully deleted " + deleteCount + " row(s) from database for hash " + hash); else if (deleteCount == 0) log(DEBUG, "The hash " + hash + " was not found in textures.db. It does not need to be deleted from the database.");
                deletedFromDB = deleteCount >= 0;
            }
            if (!deletedFromDB) {
                Config.log(ERROR, "After deleting texture from filesystem, failed to delete it from the Textures.db. Please manually delete all entries for hash: " + hash);
                failCount++;
                continue;
            } else {
                hashsDeletedFromDB.add(hash);
            }
            Config.log(DEBUG, (SIMULATION ? "SIMULATED deleting" : "Successfully deleted") + " texture with hash of " + hash + ", size " + fileSizeKB + " KB, located at: " + textureFile);
            successDeleteCount++;
            deleteSizeKB += fileSizeKB;
        }
        Config.log(NOTICE, (SIMULATION ? "SIMULATED deleting " : "Successfully deleted ") + successDeleteCount + " old textures totalling " + (deleteSizeKB / 1024) + " MB. " + failCount + " textures failed to be cleaned up.");
        textureDeleteSizeKB = deleteSizeKB;
        texturesDeletedCount = successDeleteCount;
    }

    private void cleanLibraryThumbsAndFanart() {
        if (TESTING) {
            String computername = "";
            try {
                computername = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            }
            if (computername.equalsIgnoreCase("bradvido")) {
                THUMB_DIR = "T:";
                XBMC_MYSQL_VIDEO_SCHEMA = "xbmc_video";
                XBMC_MYSQL_MUSIC_SCHEMA = "xbmc_music";
            }
        }
        boolean isSqlLite = DATABASE_TYPE.equals(SQL_LITE);
        String schemaOrDBPath = (isSqlLite ? sqlLiteVideoDBPath : XBMC_MYSQL_VIDEO_SCHEMA);
        dbVideo = new Database(DATABASE_TYPE, schemaOrDBPath, XBMC_MYSQL_SERVER, XBMC_MYSQL_UN, XBMC_MYSQL_PW, XBMC_MYSQL_PORT);
        if (dbVideo.isConnected()) Config.log(Config.INFO, "Connection to video database " + schemaOrDBPath + (!isSqlLite ? ":" + XBMC_MYSQL_VIDEO_SCHEMA : "") + " was successful"); else Config.log(Config.ERROR, "Connection to XBMC " + DATABASE_TYPE + " database " + schemaOrDBPath + (!isSqlLite ? ":" + XBMC_MYSQL_VIDEO_SCHEMA : "") + " failed.");
        schemaOrDBPath = (isSqlLite ? sqlLiteMusicDBPath : XBMC_MYSQL_MUSIC_SCHEMA);
        dbMusic = new Database(DATABASE_TYPE, schemaOrDBPath, XBMC_MYSQL_SERVER, XBMC_MYSQL_UN, XBMC_MYSQL_PW, XBMC_MYSQL_PORT);
        if (dbMusic.isConnected()) Config.log(Config.INFO, "Connection to music database " + schemaOrDBPath + (!isSqlLite ? " on " + XBMC_MYSQL_SERVER : "") + " was successful"); else Config.log(Config.ERROR, "Connection to XBMC " + DATABASE_TYPE + " database " + schemaOrDBPath + (!isSqlLite ? " on " + XBMC_MYSQL_SERVER : "") + " failed.");
        XbmcJsonRpc jsonRPC = new XbmcJsonRpc(JSON_RPC_SERVER);
        log(NOTICE, "Starting cleanup process for Library videos (Thumbs & Fanart under the /Video and /Music directories)");
        setShortLogDesc(DATABASE_TYPE + ":VideoLib");
        Config.log(INFO, "Generating CRC hashs from Movies");
        for (String fullFilePath : getMovies()) {
            fullFilePath = cleanStack(fullFilePath);
            boolean fileExists = (CONFIRM_PATHS_EXIST ? checkFilesExists(XBMCInterface.getWindowsPath(fullFilePath)) : true);
            if (fileExists) createAndSaveHash(fullFilePath);
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from Movie paths/folders");
        for (String folder : getMoviePaths()) {
            createAndSaveHash(folder);
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from TV Episodes");
        for (String fullFilePath : getEpisodes()) {
            fullFilePath = cleanStack(fullFilePath);
            boolean fileExists = (CONFIRM_PATHS_EXIST ? checkFilesExists(XBMCInterface.getWindowsPath(fullFilePath)) : true);
            if (fileExists) createAndSaveHash(fullFilePath);
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from TV Shows and Seasons");
        for (String folder : getTVShowPaths()) {
            createAndSaveHash(folder);
            createAndSaveHash("season" + folder + "* All Seasons");
            createAndSaveHash("season" + folder + " All Seasons");
            createAndSaveHash("season" + folder + "All Seasons");
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from Actors");
        for (String actorName : getActors()) {
            createAndSaveHash("actor" + actorName);
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from Music Videos");
        for (String fullFilePath : getMusicVideos()) {
            fullFilePath = cleanStack(fullFilePath);
            boolean fileExists = (CONFIRM_PATHS_EXIST ? checkFilesExists(XBMCInterface.getWindowsPath(fullFilePath)) : true);
            if (fileExists) createAndSaveHash(fullFilePath);
        }
        logHashCount();
        dbVideo.close();
        setShortLogDesc(DATABASE_TYPE + ":MusicLib");
        Config.log(INFO, "Generating CRC hashs from Artists");
        for (String artistName : getArtists()) {
            createAndSaveHash(artistName);
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from Albums");
        for (String albumArtist : getAlbumArtist()) {
            createAndSaveHash(albumArtist);
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from Song Folders");
        for (String folder : getSongFolderPaths()) {
            createAndSaveHash(folder);
        }
        logHashCount();
        Config.log(INFO, "Generating CRC hashs from Song Files");
        for (String fullFilePath : getSongFilePaths()) {
            fullFilePath = cleanStack(fullFilePath);
            boolean fileExists = (CONFIRM_PATHS_EXIST ? checkFilesExists(XBMCInterface.getWindowsPath(fullFilePath)) : true);
            if (fileExists) createAndSaveHash(fullFilePath);
        }
        logHashCount();
        Config.log(INFO, "Querying database for Album CRC Hashs");
        addAlbumHashsFromDB();
        logHashCount();
        Config.log(NOTICE, "Done with " + DATABASE_TYPE + " Queries");
        dbMusic.close();
        setShortLogDesc("JSON-RPC:Video");
        Config.log(NOTICE, "Adding JSON-RPC videos/audio to catch any hashs that weren't generated from SQL DB data. This may take a while...");
        Config.log(INFO, "Adding hashs from JSON-RPC video library items");
        boolean includeExtras = true;
        Map<String, XBMCFile> allLibraryVideos = jsonRPC.getLibraryVideos(includeExtras);
        for (Map.Entry<String, XBMCFile> entry : allLibraryVideos.entrySet()) {
            String fileLocation = entry.getKey();
            XBMCFile video = entry.getValue();
            if (video != null) {
                String thumbFullPath = video.getThumbnail();
                addJSONRPCHash(thumbFullPath, fileLocation);
                String fanartFullPath = video.getFanart();
                addJSONRPCHash(fanartFullPath, fileLocation);
            }
        }
        logHashCount();
        setShortLogDesc("JSON-RPC:Audio");
        Config.log(INFO, "Adding hashs from JSON-RPC audio library items");
        includeExtras = true;
        Map<String, XBMCFile> allLibraryAudio = jsonRPC.getLibraryMusic(includeExtras);
        for (Map.Entry<String, XBMCFile> entry : allLibraryAudio.entrySet()) {
            String fileLocation = entry.getKey();
            XBMCFile audio = entry.getValue();
            if (audio != null) {
                String thumbFullPath = audio.getThumbnail();
                addJSONRPCHash(thumbFullPath, fileLocation);
                String fanartFullPath = audio.getFanart();
                addJSONRPCHash(fanartFullPath, fileLocation);
            }
        }
        logHashCount();
        Config.log(NOTICE, "Found " + validHashs.size() + " unique CRC hash's. Continuing with cleanup");
        setShortLogDesc("ScanThumbs");
        List<File> allThumbs = new ArrayList<File>();
        Collection<File> musicThumbs = new ArrayList<File>();
        Collection<File> videoThumbs = new ArrayList<File>();
        if (!READ_FROM_CACHE) {
            File videoThumbDir = new File(THUMB_DIR + SEP + "Video");
            Config.log(INFO, "Reading thumbs/fanart " + Arrays.toString(THUMB_EXTS) + " from disk at: " + videoThumbDir + ". This may take some time.");
            videoThumbs = FileUtils.listFiles(videoThumbDir, THUMB_EXTS, true);
            Config.log(INFO, "Found " + videoThumbs.size() + " video thumbs");
            allThumbs.addAll(videoThumbs);
            File musicThumbDir = new File(THUMB_DIR + SEP + "Music");
            Config.log(INFO, "Reading thumbs/fanart " + Arrays.toString(THUMB_EXTS) + " from disk at: " + musicThumbDir + ". This may take some time.");
            musicThumbs = FileUtils.listFiles(musicThumbDir, THUMB_EXTS, true);
            Config.log(INFO, "Found " + musicThumbs.size() + " music thumbs");
            allThumbs.addAll(musicThumbs);
            if (TESTING) {
                Config.log(INFO, "Caching " + (allThumbs.size()) + " thumb paths to file");
                tools.writeToFile(new File("C:\\cachedThumbPaths.txt"), allThumbs, true);
            }
        } else {
            Config.log(INFO, "Reading from cache...");
            Collection<String> cachedThumbs = tools.readFile(new File("C:\\cachedThumbPaths.txt"));
            for (String path : cachedThumbs) {
                if (path.toLowerCase().contains("\\music\\")) musicThumbs.add(new File(path)); else if (path.toLowerCase().contains("\\video\\")) videoThumbs.add(new File(path)); else Config.log(WARNING, "Unknown cached path: " + path);
            }
            Config.log(INFO, "Done reading from cache...");
        }
        setShortLogDesc("FindOldThumbs");
        Config.log(INFO, "Checking " + (musicThumbs.size()) + " music thumbs and " + videoThumbs.size() + " video thumbs to see if they are still valid");
        List<File> invalidThumbs = new ArrayList<File>();
        for (File f : musicThumbs) {
            if (f.getPath().toLowerCase().contains("lastfm")) continue;
            String name = f.getName();
            String hash = name.toLowerCase();
            if (name.contains(".")) hash = name.substring(0, name.lastIndexOf("."));
            if (!isValidHash(hash)) {
                Config.log(INFO, "Skipping invalid hash \"" + hash + "\" found from file: " + f);
                continue;
            }
            if (validHashs.get(hash) == null) invalidThumbs.add(f);
        }
        for (File f : videoThumbs) {
            String name = f.getName();
            if (name.startsWith("auto-")) {
                invalidThumbs.add(f);
                continue;
            }
            if (f.getPath().toLowerCase().contains("bookmarks")) continue;
            String hash = name.toLowerCase();
            if (name.contains(".")) hash = name.substring(0, name.lastIndexOf("."));
            if (!isValidHash(hash)) {
                Config.log(INFO, "Skipping invalid hash \"" + hash + "\" found from file: " + f);
                continue;
            }
            if (validHashs.get(hash) == null) invalidThumbs.add(f);
        }
        setShortLogDesc("Summary");
        Config.log(INFO, "Of " + (videoThumbs.size() + musicThumbs.size()) + " video/music thumbs, " + "found " + invalidThumbs.size() + " unused thumbs that can be deleted...");
        if (SIMULATION) {
            setShortLogDesc("Simulation");
            Config.log(NOTICE, "Since this is a simulation, no thumbs/fanart are being deleted...");
            spotCheck("VideoAudio Thumbs and Fanart", invalidThumbs, SPOT_CHECK_MAX_IMAGES);
            setShortLogDesc("Simulation");
        } else setShortLogDesc("DeleteOldThumbs");
        Config.log(INFO, (SIMULATION ? "SIMULATING " : "") + "Deleting unused thumbs/fanart. This may take some time.");
        long deletedBytesKB = 0;
        int deletedCount = 0;
        int failedCount = 0;
        for (File f : invalidThumbs) {
            if (f.exists()) {
                long length = f.length();
                boolean deleted = (TESTING || SIMULATION) ? true : f.delete();
                if (!deleted) {
                    failedCount++;
                    Config.log(INFO, "Failed to delete: " + invalidThumbs);
                } else {
                    Config.log(DEBUG, "Successfully " + (SIMULATION ? "simulated deleting" : "deleted") + ": " + f);
                    deletedCount++;
                    deletedBytesKB += (length / 1024);
                }
            } else Config.log(WARNING, "Cannot delete invalid thumb because it does not exist: " + f);
        }
        Config.log(NOTICE, "Done with Thumbs & Fanart cleanup under the /Video and /Music directories. " + "Successfully " + (SIMULATION ? "SIMULATED deleting" : "deleted") + " " + deletedCount + " images totalling " + (deletedBytesKB / 1024) + " MB. " + failedCount + " failed to delete.");
        thumbDeleteSizeKB = deletedBytesKB;
        thumbsDeletedCount = deletedCount;
    }

    public void addJSONRPCHash(String hashFullPath, String xbmcFileLocation) {
        if (tools.valid(hashFullPath)) {
            boolean isFanart = hashFullPath.toLowerCase().contains("fanart");
            String hash = getHashFromFullPath(hashFullPath);
            if (hash != null) {
                boolean fileExists = (CONFIRM_PATHS_EXIST ? checkFilesExists(XBMCInterface.getWindowsPath(xbmcFileLocation)) : true);
                if (fileExists) {
                    if (validHashs.get(hash) == null) {
                        validHashs.put(hash, xbmcFileLocation);
                        Config.log(DEBUG, "JSON-RPC " + (isFanart ? "fanart" : "thumbnail") + " hash (" + hash + ") " + "added from " + xbmcFileLocation);
                    } else Config.log(DEBUG, "SKIPPING duplicate JSON-RPC " + (isFanart ? "fanart" : "thumbnail") + " hash " + "(" + hash + ") added from " + xbmcFileLocation);
                }
            } else Config.log(DEBUG, "Cannot parse CRC hash from full hash of: \"" + hashFullPath + "\", retrieved from " + "\"" + xbmcFileLocation + "\"");
        } else Config.log(DEBUG, "Cannot get CRC hash from " + xbmcFileLocation + " because the JSON-RPC interface did not " + "return a hash for this location.");
    }

    public static String getHashFromFullPath(String fullPath) {
        if (!tools.valid(fullPath)) return null;
        int lastDot = fullPath.lastIndexOf(".");
        int lastSlash = fullPath.lastIndexOf("/");
        if (lastDot == -1 || lastSlash == -1 || lastDot <= lastSlash) return null;
        String hash = fullPath.substring(lastSlash + 1, lastDot);
        if (hash.startsWith("auto-")) return null;
        if (isValidHash(hash)) return hash.toLowerCase().trim(); else {
            Config.log(WARNING, "Found invalid hash from JSON-RPC interface (unexpected): \"" + hash + "\"");
            return null;
        }
    }

    private void spotCheck(String subfName, List<File> invalidThumbs, int numberToCheck) {
        setShortLogDesc("SpotCheck");
        if (!tools.valid(SPOT_CHECK_DIR)) {
            Config.log(INFO, "Skipping spot check because no directory was specified....");
            return;
        }
        File spotCheckDir = new File(SPOT_CHECK_DIR);
        if (!spotCheckDir.exists()) {
            log(INFO, "Creating directory at: " + spotCheckDir);
            boolean created = spotCheckDir.mkdir();
            if (!created) log(ERROR, "Failed to create spot check directory, skipping spot check.");
            return;
        }
        File subdir = new File(spotCheckDir.getPath() + SEP + subfName);
        if (!subdir.exists()) subdir.mkdir(); else {
            Config.log(INFO, "Deleting spot check directory contents in preperation for new images at: " + subdir);
            try {
                for (Iterator<File> i = FileUtils.iterateFiles(subdir, THUMB_EXTS, false); i.hasNext(); ) {
                    File f = i.next();
                    if (f.exists()) f.delete();
                }
            } catch (Exception x) {
                Config.log(WARNING, "Failed to delete spot check directory contents: " + x, x);
            }
        }
        Random r = new Random();
        int numberCopied = 0;
        int attempts = 0;
        int maxAttempts = Math.abs((int) Math.round(numberToCheck * 3.5));
        if (invalidThumbs.isEmpty()) {
            Config.log(INFO, "No thumbs/fanart found to copy to spot-check directory. Skipping.");
        } else while (true) {
            attempts++;
            if (attempts > maxAttempts) break;
            File f = invalidThumbs.get(r.nextInt(invalidThumbs.size()));
            if (!f.getName().endsWith(".dds")) {
                int dotIndx = f.getName().lastIndexOf(".");
                String newName = f.getName().substring(0, dotIndx == -1 ? f.getName().length() : dotIndx);
                if (f.getPath().toLowerCase().contains("fanart")) newName += "-fanart";
                String ext;
                if (dotIndx == -1) ext = ".jpg"; else ext = f.getName().substring(dotIndx, f.getName().length()).toLowerCase();
                if (ext.equals(".tbn")) ext = ".jpg";
                newName += ext;
                File dest = new File(subdir + "\\" + newName);
                if (!dest.exists()) {
                    try {
                        FileUtils.copyFile(f, dest);
                        numberCopied++;
                    } catch (Exception x) {
                        Config.log(WARNING, "Failed to copy: " + x);
                    }
                }
            }
            if (numberCopied >= numberToCheck) break;
        }
        Config.log(INFO, "Copied " + numberCopied + " images to spot check directory at: " + subdir);
    }

    private void createAndSaveHash(String stringToHash) {
        String hash = Hash.generateCRC(stringToHash);
        putHash(hash, stringToHash);
    }

    private void putHash(String hash, String hashSource) {
        if (!isValidHash(hash)) {
            Config.log(WARNING, "Skipping invalid hash: \"" + hash + "\" from " + hashSource);
            return;
        }
        String currentPath = validHashs.get(hash);
        boolean alreadyExists = currentPath != null;
        if (alreadyExists) {
            if (!currentPath.equalsIgnoreCase(hashSource)) Config.log(WARNING, "Duplicate hash from different source! Skipping hash because it already exists: \"" + hash + "\". " + "It was generated from \"" + currentPath + "\". " + "Found the same hash \"" + hash + "\" generated from \"" + hashSource + "\"");
        } else validHashs.put(hash, hashSource);
    }

    public List<String> getMovies() {
        String sql = "SELECT " + CONCAT + "(strPath, strFileName) FROM movieview";
        return getList(sql, dbVideo, null);
    }

    public List<String> getMoviePaths() {
        String sql = "SELECT distinct(strPath) FROM movieview";
        List<String> paths = getList(sql, dbVideo, null);
        List<String> pathsWithFolderImages = new ArrayList<String>();
        for (String path : paths) {
            for (String folderImage : folderImages) pathsWithFolderImages.add(path + folderImage);
        }
        paths.addAll(pathsWithFolderImages);
        return paths;
    }

    public List<String> getEpisodes() {
        String sql = "SELECT " + CONCAT + "(strPath, strFileName) FROM episodeview";
        return getList(sql, dbVideo, null);
    }

    private Collection<String> getTVShowPaths() {
        String sql = "SELECT strPath FROM path WHERE idPath in (SELECT idPath FROM tvshowlinkpath)";
        return getList(sql, dbVideo, null);
    }

    private List<String> getMusicVideos() {
        String sql = "SELECT " + CONCAT + "(strPath, strFileName) FROM musicvideoview";
        return getList(sql, dbVideo, null);
    }

    public List<String> getActors() {
        String sql = "SELECT strActor FROM actors";
        return getList(sql, dbVideo, null);
    }

    public List<String> getArtists() {
        String sql = "SELECT strArtist FROM artist";
        List<String> artistsNames = getList(sql, dbMusic, null);
        List<String> artists = new ArrayList<String>();
        for (String name : artistsNames) artists.add("artist" + name);
        artists.addAll(artistsNames);
        return artists;
    }

    public List<String> getAlbumArtist() {
        String sql = "SELECT " + CONCAT + "(strAlbum, strArtist) FROM albumview";
        return getList(sql, dbMusic, null);
    }

    public List<String> getSongFolderPaths() {
        String sql = "SELECT strPath FROM path";
        List<String> paths = getList(sql, dbMusic, null);
        List<String> pathsWithFolderImages = new ArrayList<String>();
        for (String path : paths) {
            for (String folderImage : folderImages) pathsWithFolderImages.add(path + folderImage);
        }
        paths.addAll(pathsWithFolderImages);
        return paths;
    }

    public List<String> getSongFilePaths() {
        String sql = "SELECT " + CONCAT + "(strPath, strFilename) FROM songview";
        return getList(sql, dbMusic, null);
    }

    public void addAlbumHashsFromDB() {
        String sql = "SELECT " + CONCAT + "(strAlbum, strArtist) as albumArtist, strThumb FROM albumview";
        Map<String, String> hashMap = getMap(sql, dbMusic, null);
        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            String albumArtist = entry.getKey();
            String path = entry.getValue();
            if (tools.valid(path) && !path.equalsIgnoreCase("NONE")) {
                int lastSlashIndx = path.lastIndexOf("/");
                int lastDotIndx = path.lastIndexOf(".");
                if (lastDotIndx != -1 && lastSlashIndx != -1 && lastDotIndx > lastSlashIndx) {
                    String hash = path.substring(lastSlashIndx + 1, lastDotIndx).toLowerCase().trim();
                    if (hash.length() == 8) {
                        if (validHashs.get(hash) == null) validHashs.put(hash, "MySQL saved hash from albumview.strThumb");
                    }
                } else Config.log(WARNING, "unknown album hash path: " + path + " for album/artist: " + albumArtist);
            }
        }
    }

    public static String convertToSQLLite(String sql) {
        if (sql.contains(CONCAT)) {
            int concatIndx = sql.indexOf(CONCAT);
            concatIndx += CONCAT.length();
            int commaIndx = sql.indexOf(",", concatIndx);
            sql = sql.substring(0, commaIndx) + " || " + sql.substring(commaIndx + 1, sql.length());
            sql = sql.replace(CONCAT, "");
            sql = sql.replace("  ", " ");
        }
        return sql;
    }

    public List<String> getList(String sql, Database db, List<Param> params) {
        if (db.isSQLite()) sql = convertToSQLLite(sql);
        try {
            List<String> list = new ArrayList<String>();
            Config.log(DEBUG, "Executing SQL: " + sql);
            ResultSet rs = db.executeQuery(sql, params);
            while (rs.next()) {
                String s = rs.getString(1);
                if (db.isMySQL()) {
                    s = charSetString(s);
                }
                list.add(s);
            }
            rs.close();
            return list;
        } catch (Exception x) {
            Config.log(ERROR, "Failed to while executing SQL: " + sql, x);
            Config.log(INFO, "Since a query failed, will exit now to ensure valid thumbs aren't deleted.");
            end();
            System.exit(1);
            return null;
        } finally {
            db.closeStatement();
        }
    }

    public Map<String, String> getMap(String sql, Database db, List<Param> params) {
        if (db.isSQLite()) sql = convertToSQLLite(sql);
        try {
            Map<String, String> list = new LinkedHashMap<String, String>();
            Config.log(DEBUG, "Executing SQL: " + sql);
            ResultSet rs = db.executeQuery(sql, params);
            while (rs.next()) {
                String s1 = rs.getString(1);
                String s2 = rs.getString(2);
                if (db.isMySQL()) {
                    s1 = charSetString(s1);
                    s2 = charSetString(s2);
                }
                list.put(s1, s2);
            }
            rs.close();
            return list;
        } catch (Exception x) {
            Config.log(INFO, "Failed while executing SQL: " + sql, x);
            Config.log(INFO, "Since a query failed, will exit now to ensure valid thumbs aren't deleted.");
            end();
            System.exit(1);
            return null;
        } finally {
            db.closeStatement();
        }
    }

    public static String charSetString(String s) {
        try {
            byte[] bytes = s.getBytes(MYSQL_CHARACTER_SET);
            String charSetString = new String(bytes);
            return charSetString;
        } catch (Exception x) {
            Config.log(ERROR, "Error converting to character set \"" + MYSQL_CHARACTER_SET + "\": " + x);
            return s;
        }
    }

    public void logHashCount() {
        Config.log(INFO, validHashs.size() + " valid CRC hashs have now been found");
    }

    public static boolean isValidHash(String hash) {
        if (!tools.valid(hash)) return false;
        char[] chars = hash.toCharArray();
        if (chars.length != 8) return false;
        for (char c : chars) {
            if (!validCRCChars.contains(c)) return false;
        }
        return true;
    }

    public static java.util.Date getTextureLastUsedTimeLocal(String strUTCDate) {
        try {
            java.util.Date dtLocal = texturesSDF.parse(strUTCDate);
            return dtLocal;
        } catch (Exception x) {
            Config.log(ERROR, "Failed to parse texture last used date \"" + strUTCDate + "\" using pattern: " + texturesSDF.toPattern(), x);
            return null;
        }
    }

    public static String getHashFromFile(File f) {
        String hash = f.getName().toLowerCase();
        int dotIndx = hash.indexOf(".");
        if (dotIndx != -1) hash = hash.substring(0, dotIndx);
        return hash.trim();
    }

    public static String cleanStack(String xbmcPath) {
        if (tools.valid(xbmcPath)) {
            int stackIndex = xbmcPath.toLowerCase().indexOf("stack://");
            if (stackIndex != -1) return xbmcPath.substring(stackIndex, xbmcPath.length());
        }
        return xbmcPath;
    }

    static Set<File> filesThatExist = new HashSet<File>();

    public static boolean checkFilesExists(List<File> filesToCheck) {
        boolean allFilesExist = true;
        for (File f : filesToCheck) {
            if (f == null) continue;
            if (filesThatExist.contains(f)) continue;
            if (!f.isFile()) {
                Config.log(DEBUG, "This file does not exist, will delete any old thumbs/fanart for it: " + f);
                allFilesExist = false;
            } else {
                filesThatExist.add(f);
            }
        }
        return allFilesExist;
    }
}
