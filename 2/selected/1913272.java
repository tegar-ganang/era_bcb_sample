package com.moviejukebox.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.TrailerFile;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.WebBrowser;
import com.moviejukebox.tools.WebStats;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.FileTools;

public class AppleTrailersPlugin {

    private static Logger logger = Logger.getLogger("moviejukebox");

    private String configResolution;

    private String configDownload;

    private String configTrailerTypes;

    private int configMax;

    private boolean configTypesInclude;

    protected WebBrowser webBrowser;

    private WebStats stats;

    public AppleTrailersPlugin() {
        webBrowser = new WebBrowser();
        configResolution = PropertiesUtil.getProperty("appletrailers.resolution", "");
        configDownload = PropertiesUtil.getProperty("appletrailers.download", "false");
        try {
            configMax = Integer.parseInt(PropertiesUtil.getProperty("appletrailers.max", "0"));
        } catch (Exception ignored) {
            configMax = 0;
        }
        configTypesInclude = Boolean.parseBoolean(PropertiesUtil.getProperty("appletrailers.typesinclude", "true"));
        configTrailerTypes = PropertiesUtil.getProperty("appletrailers.trailertypes", "tlr,clip,tsr,30sec,640w");
    }

    public void generate(Movie movie) {
        if (configResolution.equals("")) return;
        if (movie.isTrailerExchange()) return;
        if (movie.isTrailer()) return;
        if (movie.getMovieType().equals(Movie.TYPE_TVSHOW)) return;
        String movieName = movie.getOriginalTitle();
        String trailerPageUrl = GetTrailerPageUrl(movieName);
        if (trailerPageUrl == Movie.UNKNOWN) {
            logger.finer("AppleTrailers Plugin: Trailer not found for " + movie.getBaseName());
            movie.setTrailerExchange(true);
            return;
        }
        ArrayList<String> trailersUrl = new ArrayList<String>();
        ArrayList<String> bestTrailersUrl = new ArrayList<String>();
        getTrailerSubUrl(trailerPageUrl, trailersUrl);
        selectBestTrailer(trailersUrl, bestTrailersUrl);
        int trailerCnt = bestTrailersUrl.size();
        if (configMax > 0 && configMax < trailerCnt) {
            trailerCnt = configMax;
        }
        for (int i = 0; i < trailerCnt; i++) {
            String trailerRealUrl = bestTrailersUrl.get(i);
            MovieFile tmf = new MovieFile();
            tmf.setTitle(getTrailerTitle(trailerRealUrl));
            if (!isValidTrailer(getFilenameFromUrl(trailerRealUrl))) {
                if (configMax < trailerCnt) {
                    trailerCnt++;
                }
                logger.finer("AppleTrailers Plugin: Trailer skipped: " + getFilenameFromUrl(trailerRealUrl));
                continue;
            }
            logger.finer("AppleTrailers Plugin: Trailer found for " + movie.getBaseName() + " (" + getFilenameFromUrl(trailerRealUrl) + ")");
            if (!configDownload.equals("true")) {
                tmf.setFilename(trailerRealUrl);
                movie.addTrailerFile(new TrailerFile(tmf));
            } else {
                MovieFile mf = movie.getFirstFile();
                String parentPath = mf.getFile().getParent();
                String name = mf.getFile().getName();
                String basename;
                if (mf.getFilename().endsWith("/VIDEO_TS")) {
                    parentPath += File.separator + name;
                    basename = name;
                } else {
                    int index = name.lastIndexOf(".");
                    basename = index == -1 ? name : name.substring(0, index);
                }
                String trailerBasename = FileTools.makeSafeFilename(basename + ".[TRAILER]." + getFilenameFromUrl(trailerRealUrl));
                String trailerFileName = parentPath + File.separator + trailerBasename;
                int slash = mf.getFilename().lastIndexOf("/");
                String playPath = slash == -1 ? mf.getFilename() : mf.getFilename().substring(0, slash);
                String trailerPlayFileName = playPath + "/" + HTMLTools.encodeUrl(trailerBasename);
                logger.finest("Found trailer: " + trailerRealUrl);
                logger.finest("  D/L path: " + trailerFileName);
                logger.finest("  Play URL: " + trailerPlayFileName);
                File trailerFile = new File(trailerFileName);
                if (trailerFile.exists()) {
                    logger.finer("AppleTrailers Plugin: Trailer file (" + trailerPlayFileName + ") already exist for " + movie.getBaseName());
                    tmf.setFilename(trailerPlayFileName);
                    movie.addTrailerFile(new TrailerFile(tmf));
                } else if (trailerDownload(movie, trailerRealUrl, trailerFile)) {
                    tmf.setFilename(trailerPlayFileName);
                    movie.addTrailerFile(new TrailerFile(tmf));
                }
            }
        }
        movie.setTrailerExchange(true);
    }

    private String GetTrailerPageUrl(String movieName) {
        try {
            String searchURL = "http://www.apple.com/trailers/home/scripts/quickfind.php?callback=searchCallback&q=" + URLEncoder.encode(movieName, "UTF-8");
            String xml = webBrowser.request(searchURL);
            int index = 0;
            int endIndex = 0;
            while (true) {
                index = xml.indexOf("\"title\":\"", index);
                if (index == -1) break;
                index += 9;
                endIndex = xml.indexOf("\",", index);
                if (endIndex == -1) break;
                String trailerTitle = decodeEscapeICU(xml.substring(index, endIndex));
                index = endIndex + 2;
                index = xml.indexOf("\"location\":\"", index);
                if (index == -1) break;
                index += 12;
                endIndex = xml.indexOf("\",", index);
                if (endIndex == -1) break;
                String trailerLocation = decodeEscapeICU(xml.substring(index, endIndex));
                index = endIndex + 2;
                if (trailerTitle.equalsIgnoreCase(movieName)) {
                    String trailerUrl;
                    int itmsIndex = trailerLocation.indexOf("itms://");
                    if (itmsIndex == -1) {
                        trailerUrl = getAbsUrl("http://www.apple.com/trailers/", trailerLocation);
                    } else {
                        trailerUrl = "http" + trailerLocation.substring(itmsIndex + 4);
                    }
                    return trailerUrl;
                }
            }
        } catch (Exception e) {
            logger.severe("Failed retreiving trailer for movie : " + movieName);
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
        return Movie.UNKNOWN;
    }

    private void getTrailerSubUrl(String trailerPageUrl, ArrayList<String> trailersUrl) {
        try {
            String xml = webBrowser.request(trailerPageUrl);
            getTrailerMovieUrl(xml, trailersUrl);
            String trailerPageUrlHD = getAbsUrl(trailerPageUrl, "hd");
            String xmlHD = getSubPage(trailerPageUrlHD);
            getTrailerMovieUrl(xmlHD, trailersUrl);
            int index = 0;
            int endIndex = 0;
            while (true) {
                index = xml.indexOf("href=\"", index);
                if (index == -1) break;
                index += 6;
                endIndex = xml.indexOf("\"", index);
                if (endIndex == -1) break;
                String href = xml.substring(index, endIndex);
                index = endIndex + 1;
                String absHref = getAbsUrl(trailerPageUrl, href);
                if (absHref.startsWith(trailerPageUrl)) {
                    String subXml = getSubPage(absHref);
                    getTrailerMovieUrl(subXml, trailersUrl);
                }
            }
        } catch (Exception e) {
            logger.severe("Error : " + e.getMessage());
            return;
        }
    }

    private String getSubPage(String url) {
        String ret = "";
        try {
            ret = webBrowser.request(url);
            return ret;
        } catch (Exception e) {
            return ret;
        }
    }

    private void getTrailerMovieUrl(String xml, ArrayList<String> trailersUrl) {
        Matcher m = Pattern.compile("http://(movies|images).apple.com/movies/.+?\\.(mov|m4v)").matcher(xml);
        while (m.find()) {
            String movieUrl = m.group();
            boolean duplicate = false;
            for (int i = 0; i < trailersUrl.size(); i++) {
                if (trailersUrl.get(i).equals(movieUrl)) duplicate = true;
            }
            if (!duplicate) trailersUrl.add(movieUrl);
        }
    }

    private void selectBestTrailer(ArrayList<String> trailersUrl, ArrayList<String> bestTrailersUrl) {
        if (configResolution.equals("1080p")) {
            for (int i = 0; i < trailersUrl.size(); i++) {
                String curURL = trailersUrl.get(i);
                if (curURL.indexOf("1080p") != -1) addTailerRealUrl(bestTrailersUrl, curURL);
            }
            if (!bestTrailersUrl.isEmpty()) return;
        }
        if ((configResolution.equals("1080p")) || (configResolution.equals("720p"))) {
            for (int i = 0; i < trailersUrl.size(); i++) {
                String curURL = trailersUrl.get(i);
                if (curURL.indexOf("720p") != -1) addTailerRealUrl(bestTrailersUrl, curURL);
            }
            if (!bestTrailersUrl.isEmpty()) return;
        }
        if ((configResolution.equals("1080p")) || (configResolution.equals("720p")) || (configResolution.equals("480p"))) {
            for (int i = 0; i < trailersUrl.size(); i++) {
                String curURL = trailersUrl.get(i);
                if (curURL.indexOf("480p") != -1) addTailerRealUrl(bestTrailersUrl, curURL);
            }
            if (!bestTrailersUrl.isEmpty()) return;
        }
        for (int i = 0; i < trailersUrl.size(); i++) {
            String curURL = trailersUrl.get(i);
            if (curURL.indexOf("640") != -1) addTailerRealUrl(bestTrailersUrl, curURL);
        }
        if (!bestTrailersUrl.isEmpty()) return;
        for (int i = 0; i < trailersUrl.size(); i++) {
            String curURL = trailersUrl.get(i);
            if (curURL.indexOf("480") != -1) addTailerRealUrl(bestTrailersUrl, curURL);
        }
    }

    private void addTailerRealUrl(ArrayList<String> bestTrailersUrl, String trailerUrl) {
        String trailerRealUrl = getTrailerRealUrl(trailerUrl);
        for (int i = 0; i < bestTrailersUrl.size(); i++) {
            if (bestTrailersUrl.get(i).equals(trailerRealUrl)) return;
        }
        bestTrailersUrl.add(trailerRealUrl);
    }

    private String getTrailerRealUrl(String trailerUrl) {
        try {
            URL url = new URL(trailerUrl);
            HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
            InputStream inputStream = connection.getInputStream();
            byte buf[] = new byte[1024];
            int len;
            len = inputStream.read(buf);
            if (len == 1024) return trailerUrl;
            String mov = new String(buf);
            int pos = 44;
            String realUrl = "";
            while (mov.charAt(pos) != 0) {
                realUrl += mov.charAt(pos);
                pos++;
            }
            String absRealURL = getAbsUrl(trailerUrl, realUrl);
            return absRealURL;
        } catch (Exception e) {
            logger.severe("Error : " + e.getMessage());
            return Movie.UNKNOWN;
        }
    }

    private String getTrailerTitle(String url) {
        int start = url.lastIndexOf('/');
        int end = url.indexOf(".mov", start);
        if ((start == -1) || (end == -1)) return Movie.UNKNOWN;
        String title = "";
        for (int i = start + 1; i < end; i++) {
            if ((url.charAt(i) == '-') || (url.charAt(i) == '_')) title += ' '; else if (i == start + 1) title += Character.toUpperCase(url.charAt(i)); else title += url.charAt(i);
        }
        return title;
    }

    private String getAbsUrl(String BaseUrl, String RelativeUrl) {
        try {
            URL BaseURL = new URL(BaseUrl);
            URL AbsURL = new URL(BaseURL, RelativeUrl);
            String AbsUrl = AbsURL.toString();
            return AbsUrl;
        } catch (Exception e) {
            return Movie.UNKNOWN;
        }
    }

    private String decodeEscapeICU(String s) {
        String r = "";
        int i = 0;
        while (i < s.length()) {
            if ((s.charAt(i) == '%') && (i + 5 < s.length()) && (s.charAt(i + 1) == 'u')) {
                String value = s.substring(i + 2, i + 6);
                int intValue = Integer.parseInt(value, 16);
                if (intValue == 0x2019) intValue = 0x0027;
                char c = (char) intValue;
                r += c;
                i += 6;
            } else if (s.charAt(i) == '\\') {
                i++;
            } else {
                r += s.charAt(i);
                i++;
            }
        }
        return r;
    }

    private boolean trailerDownload(Movie movie, String trailerUrl, File trailerFile) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            public void run() {
                stats.print();
            }
        }, 1000, 1000);
        try {
            logger.fine("AppleTrailers Plugin: Download trailer for " + movie.getBaseName());
            URL url = new URL(trailerUrl);
            stats = WebStats.make(url);
            HttpURLConnection connection = (HttpURLConnection) (url.openConnection());
            InputStream inputStream = connection.getInputStream();
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                logger.severe("AppleTrailers Plugin: Download Failed");
                return false;
            }
            OutputStream out = new FileOutputStream(trailerFile);
            byte buf[] = new byte[1024 * 1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                out.write(buf, 0, len);
                stats.bytes(len);
            }
            out.close();
            return true;
        } catch (Exception e) {
            logger.severe("AppleTrailers Plugin: Download Exception");
            return false;
        } finally {
            stats.print();
            System.out.print("\n");
            timer.cancel();
        }
    }

    private String getFilenameFromUrl(String fullUrl) {
        int nameStart = fullUrl.lastIndexOf('/') + 1;
        return fullUrl.substring(nameStart);
    }

    private boolean isValidTrailer(String trailerFilename) {
        boolean validTrailer;
        if (configTypesInclude) validTrailer = false; else validTrailer = true;
        for (String ttype : configTrailerTypes.split(",")) {
            if (trailerFilename.lastIndexOf(ttype) > 0) {
                if (configTypesInclude) validTrailer = true; else validTrailer = false;
                break;
            }
        }
        return validTrailer;
    }
}
