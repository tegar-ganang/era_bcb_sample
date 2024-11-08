package org.lnicholls.galleon.togo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hibernate.HibernateException;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.database.VideoManager;
import org.lnicholls.galleon.server.GoBackConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.server.ServerConfiguration;
import org.lnicholls.galleon.server.TiVo;
import org.lnicholls.galleon.util.ProgressListener;
import org.lnicholls.galleon.util.Tools;

public class ToGo {

    private static String QUERY_CONTAINER = "/TiVoConnect?Command=QueryContainer&Container=%2FNowPlaying";

    private static String RECURSE = "&Recurse=Yes";

    private static String ANCHOR_OFFSET = "&AnchorOffset=";

    private static String ITEM_COUNT = "&ItemCount=";

    private static int MAX = 20;

    private static Logger log = Logger.getLogger(ToGo.class.getName());

    private static SimpleDateFormat smTimeDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public ToGo() {
        mFileDateFormat = new SimpleDateFormat();
        mFileDateFormat.applyPattern("EEE MMM d yyyy hh mma");
        mCalendar = new GregorianCalendar();
    }

    public ArrayList<Video> getRecordings(List<TiVo> tivos, ProgressListener progressIndicator) {
        ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
        ArrayList<Video> videos = new ArrayList<Video>();
        log.debug("getRecordings: " + tivos.size());
        log.debug("mServerConfiguration.getMediaAccessKey()=" + serverConfiguration.getMediaAccessKey().length());
        PersistentValue persistentValue = PersistentValueManager.loadPersistentValue("VideoServer.lastUpdate");
        if (persistentValue != null) {
            try {
                String value = persistentValue.getValue();
                Date date = new Date(value);
                Date now = new Date();
                if (now.getTime() - date.getTime() < 60 * 1000) {
                    log.debug("backoff: tivo busy with goback download");
                    return videos;
                }
            } catch (Throwable ex) {
                log.error("Could not retrieve video server last update", ex);
            }
        }
        if (serverConfiguration.getMediaAccessKey().length() > 0) {
            GetMethod get = null;
            Iterator<TiVo> tivosIterator = tivos.iterator();
            while (tivosIterator.hasNext()) {
                TiVo tivo = tivosIterator.next();
                HashMap<Video, String> shows = new HashMap<Video, String>();
                try {
                    Protocol protocol = new Protocol("https", new TiVoSSLProtocolSocketFactory(), 443);
                    HttpClient client = new HttpClient();
                    client.getHostConfiguration().setHost(tivo.getAddress(), 443, protocol);
                    Credentials credentials = new UsernamePasswordCredentials("tivo", Tools.decrypt(serverConfiguration.getMediaAccessKey()));
                    client.getState().setCredentials("TiVo DVR", tivo.getAddress(), credentials);
                    int total = -1;
                    int counter = 0;
                    long startTime = System.currentTimeMillis();
                    long lastTime = 0;
                    do {
                        try {
                            String url = QUERY_CONTAINER + RECURSE + ITEM_COUNT + MAX + ANCHOR_OFFSET + counter;
                            log.debug(url);
                            get = new GetMethod(url);
                            client.executeMethod(get);
                            if ((System.currentTimeMillis() - lastTime) > 1000 * 30) {
                                PersistentValueManager.savePersistentValue("ToGo.lastQuery", new Date().toString());
                                lastTime = System.currentTimeMillis();
                            }
                            SAXReader saxReader = new SAXReader();
                            Document document = saxReader.read(get.getResponseBodyAsStream());
                            Element root = document.getRootElement();
                            Date lastChangedDate = new Date();
                            Element detailsElement = root.element("Details");
                            if (detailsElement != null) {
                                Element totalItemsElement = detailsElement.element("TotalItems");
                                if (totalItemsElement != null) {
                                    try {
                                        total = Integer.parseInt(totalItemsElement.getText());
                                    } catch (NumberFormatException ex) {
                                    }
                                }
                                Element lastChangeDateElement = detailsElement.element("LastChangeDate");
                                if (lastChangeDateElement != null) {
                                    try {
                                        lastChangedDate = Tools.hexDate(lastChangeDateElement.getText());
                                    } catch (NumberFormatException ex) {
                                    }
                                }
                            }
                            log.debug("lastChangedDate=" + lastChangedDate);
                            log.debug("tivo.getLastChangedDate()=" + tivo.getLastChangedDate());
                            log.debug("total=" + total);
                            log.debug("tivo.getNumShows()=" + tivo.getNumShows());
                            if (lastChangedDate.after(tivo.getLastChangedDate()) || total != tivo.getNumShows()) {
                                tivo.setLastChangedDate(lastChangedDate);
                                tivo.setNumShows(0);
                            } else {
                                synchronized (this) {
                                    List<Video> recordings = VideoManager.listAll();
                                    Iterator<Video> iterator = recordings.listIterator();
                                    while (iterator.hasNext()) {
                                        Video video = iterator.next();
                                        if (video.getUrl() != null && video.getUrl().indexOf(tivo.getAddress()) != -1) videos.add(video);
                                    }
                                }
                                break;
                            }
                            for (Iterator<Element> iterator = root.elementIterator(); iterator.hasNext(); ) {
                                Element child = iterator.next();
                                if (child.getName().equals("Item")) {
                                    Video video = new Video();
                                    video.setMimeType("mpeg");
                                    video.setDateModified(new Date());
                                    video.setParentalControls(false);
                                    counter = counter + 1;
                                    if (progressIndicator != null) {
                                        if (total > 0) {
                                            progressIndicator.progress(counter + " of " + total);
                                        } else progressIndicator.progress(counter + "");
                                    }
                                    Element details = child.element("Details");
                                    if (details != null) {
                                        String value = Tools.getAttribute(details, "CopyProtected");
                                        if (value != null) {
                                            if (value.equalsIgnoreCase("yes")) video.setStatus(Video.STATUS_PROTECTED);
                                        }
                                        value = Tools.getAttribute(details, "HighDefinition");
                                        if (value != null) video.setHighDefinition(value);
                                        value = Tools.getAttribute(details, "Title");
                                        if (value != null) video.setTitle(value);
                                        value = Tools.getAttribute(details, "ParentalControls");
                                        if (value != null) {
                                            if (value.equalsIgnoreCase("yes")) video.setParentalControls(true);
                                        }
                                        value = Tools.getAttribute(details, "SourceSize");
                                        if (value != null) video.setSize(Long.parseLong(value));
                                        value = Tools.getAttribute(details, "Duration");
                                        if (value != null) try {
                                            video.setDuration(Integer.parseInt(value));
                                        } catch (NumberFormatException ex) {
                                            log.error("Could not set duration", ex);
                                        }
                                        value = Tools.getAttribute(details, "CaptureDate");
                                        if (value != null) video.setDateRecorded(Tools.hexDate(value));
                                        value = Tools.getAttribute(details, "EpisodeTitle");
                                        if (value != null) video.setEpisodeTitle(value);
                                        value = Tools.getAttribute(details, "Description");
                                        if (value != null) video.setDescription(value);
                                        value = Tools.getAttribute(details, "SourceChannel");
                                        if (value != null) video.setChannel(value);
                                        value = Tools.getAttribute(details, "SourceStation");
                                        if (value != null) video.setStation(value);
                                        value = Tools.getAttribute(details, "InProgress");
                                        if (value != null) video.setStatus(Video.STATUS_RECORDING);
                                        video.setSource(tivo.getAddress());
                                        video.setTivo(tivo.getName());
                                    }
                                    String detailsUrl = null;
                                    Element links = child.element("Links");
                                    if (links != null) {
                                        Element element = links.element("Content");
                                        if (element != null) {
                                            String value = Tools.getAttribute(element, "Url");
                                            if (value != null) video.setUrl(value);
                                        }
                                        element = links.element("CustomIcon");
                                        if (element != null) {
                                            String value = Tools.getAttribute(element, "Url");
                                            if (value != null) {
                                                video.setIcon(value.substring(value.lastIndexOf(":") + 1));
                                            }
                                        }
                                        element = links.element("TiVoVideoDetails");
                                        if (element != null) {
                                            String value = Tools.getAttribute(element, "Url");
                                            if (value != null) {
                                                URL path = new URL(value);
                                                detailsUrl = path.getPath() + "?" + path.getQuery();
                                            }
                                        }
                                    }
                                    if (detailsUrl != null) {
                                        shows.put(video, detailsUrl);
                                        tivo.setNumShows(tivo.getNumShows() + 1);
                                    }
                                }
                            }
                            Thread.sleep(100);
                        } finally {
                            if (get != null) {
                                get.releaseConnection();
                                get = null;
                            }
                        }
                    } while (counter < total);
                    for (Iterator<Video> iterator = shows.keySet().iterator(); iterator.hasNext(); ) {
                        Video video = iterator.next();
                        String url = (String) shows.get(video);
                        getvideoDetails(client, video, url);
                        videos.add(video);
                        Thread.sleep(500);
                    }
                } catch (MalformedURLException ex) {
                    Tools.logException(ToGo.class, ex);
                } catch (Exception ex) {
                    Tools.logException(ToGo.class, ex);
                    try {
                        Thread.sleep(10000);
                    } catch (Exception ex2) {
                    }
                }
            }
        }
        return videos;
    }

    public static void getvideoDetails(HttpClient client, Video video, String url) {
        GetMethod get = null;
        try {
            get = new GetMethod(url);
            client.executeMethod(get);
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(get.getResponseBodyAsStream());
            parseVideoDetails(video, document);
        } catch (Exception ex) {
            Tools.logException(ToGo.class, ex);
        } finally {
            if (get != null) {
                get.releaseConnection();
                get = null;
            }
        }
    }

    public static void getvideoDetails(String filename, Video video) throws FileNotFoundException {
        SAXReader saxReader = new SAXReader();
        Document document;
        try {
            document = saxReader.read(new FileInputStream(filename));
            parseVideoDetails(video, document);
            File file = new File(filename);
            String value = Tools.trimSuffix(file.getName());
            Pattern pattern = Pattern.compile("^(.*) - (.*) \\(Recorded ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*)\\)$");
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                video.setCallsign(matcher.group(9));
            } else {
                pattern = Pattern.compile("^(.*) - ''(.*)'' \\(Recorded (.*), ([^,]*)\\)$");
                matcher = pattern.matcher(value);
                if (matcher.find()) {
                    video.setCallsign(matcher.group(4));
                } else {
                    pattern = Pattern.compile("^(.*) - (.*) \\(Recorded (.*), ([^,]*)\\)$");
                    matcher = pattern.matcher(value);
                    if (matcher.find()) {
                        video.setCallsign(matcher.group(4));
                    } else {
                        pattern = Pattern.compile("^(.*) \\(Recorded (.*), ([^,]*)\\)$");
                        matcher = pattern.matcher(value);
                        if (matcher.find()) {
                            video.setCallsign(matcher.group(3));
                        } else {
                            pattern = Pattern.compile("^(.*) \\(Recorded ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*) ([\\S]*)\\)$");
                            matcher = pattern.matcher(value);
                            if (matcher.find()) {
                                video.setCallsign(matcher.group(8));
                            } else {
                                pattern = Pattern.compile("^(.*) \\(Recorded (.*), ([^,]*)\\)$");
                                matcher = pattern.matcher(value);
                                if (matcher.find()) {
                                    video.setCallsign(matcher.group(3));
                                }
                            }
                        }
                    }
                }
            }
        } catch (DocumentException e) {
        }
    }

    public static void main(String args[]) {
        Video v = new Video();
        try {
            ToGo.getvideoDetails(args[0], v);
            System.out.println("video file" + args[0] + ": " + v.getDescription());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void parseVideoDetails(Video video, Document document) {
        Element root = document.getRootElement();
        Element showing = root.element("showing");
        if (showing != null) {
            Element element = showing;
            if (element != null) {
                Element node = element.element("partCount");
                if (node != null) try {
                    video.setPartCount(Integer.parseInt(node.getText()));
                } catch (Exception ex) {
                    log.error("Could not set part count", ex);
                }
                node = element.element("partIndex");
                if (node != null) try {
                    video.setPartIndex(Integer.parseInt(node.getText()));
                } catch (Exception ex) {
                    log.error("Could not set part index", ex);
                }
                Element program = element.element("program");
                if (program != null) {
                    node = program.element("vActor");
                    if (node != null) video.setActors(getElements(node));
                    node = program.element("vAdvisory");
                    if (node != null) video.setAdvisories(getElements(node));
                    node = program.element("vChoreographer");
                    if (node != null) video.setChoreographers(getElements(node));
                    node = program.element("vChoreographer");
                    if (node != null) video.setChoreographers(getElements(node));
                    node = program.element("colorCode");
                    if (node != null) {
                        video.setColor(node.getText());
                        try {
                            video.setColorCode(Integer.parseInt(node.attribute("value").getText()));
                        } catch (Exception ex) {
                            log.error("Could not set color code", ex);
                        }
                    }
                    node = program.element("description");
                    if (node != null) video.setDescription(node.getTextTrim());
                    node = program.element("vDirector");
                    if (node != null) video.setDirectors(getElements(node));
                    node = program.element("episodeNumber");
                    if (node != null) try {
                        video.setEpisodeNumber(Integer.parseInt(node.getTextTrim()));
                    } catch (Exception ex) {
                        log.error("Could not episode number", ex);
                    }
                    node = program.element("episodeTitle");
                    if (node != null) video.setEpisodeTitle(node.getTextTrim());
                    node = program.element("vExecProducer");
                    if (node != null) video.setExecProducers(getElements(node));
                    node = program.element("vProgramGenre");
                    if (node != null) video.setProgramGenre(getElements(node));
                    node = program.element("vGuestStar");
                    if (node != null) video.setGuestStars(getElements(node));
                    node = program.element("vHost");
                    if (node != null) video.setHosts(getElements(node));
                    node = program.element("isEpisode");
                    if (node != null) video.setEpisodic(Boolean.valueOf(node.getTextTrim()));
                    node = program.element("originalAirDate");
                    if (node != null) {
                        ParsePosition pos = new ParsePosition(0);
                        Date date = smTimeDateFormat.parse(node.getTextTrim(), pos);
                        if (date == null) date = new Date(0);
                        video.setOriginalAirDate(date);
                    }
                    node = program.element("vProducer");
                    if (node != null) video.setProducers(getElements(node));
                    Element series = program.element("series");
                    if (series != null) {
                        node = series.element("vSeriesGenre");
                        if (node != null) video.setSeriesGenre(getElements(node));
                        node = series.element("seriesTitle");
                        if (node != null) video.setSeriesTitle(node.getTextTrim());
                    }
                    node = program.element("showType");
                    if (node != null) {
                        video.setShowType(node.getTextTrim());
                        try {
                            video.setShowTypeValue(Integer.parseInt(node.attribute("value").getText()));
                        } catch (Exception ex) {
                            log.error("Could not set showtype value", ex);
                        }
                    }
                    node = program.element("title");
                    if (node != null) video.setTitle(node.getTextTrim());
                    node = program.element("vWriter");
                    if (node != null) video.setWriters(getElements(node));
                }
                Element channel = element.element("channel");
                if (channel != null) {
                    node = channel.element("displayMajorNumber");
                    if (node != null) try {
                        video.setChannelMajorNumber(Integer.parseInt(node.getTextTrim()));
                    } catch (Exception ex) {
                        log.error("Could not set channel major number", ex);
                    }
                    node = channel.element("displayMinorNumber");
                    if (node != null) try {
                        video.setChannelMinorNumber(Integer.parseInt(node.getTextTrim()));
                    } catch (Exception ex) {
                        log.error("Could not set channel minor number", ex);
                    }
                    node = channel.element("callsign");
                    if (node != null) video.setCallsign(node.getTextTrim());
                }
                Element rating = element.element("tvRating");
                if (rating != null) {
                    video.setRating(rating.getTextTrim());
                    try {
                        video.setRatingValue(Integer.parseInt(rating.attribute("value").getText()));
                    } catch (Exception ex) {
                        log.error("Could not set rating value", ex);
                    }
                }
            }
        }
        showing = root.element("vActualShowing");
        if (showing != null) {
            Iterator<Element> iterator = showing.elementIterator("element");
            while (iterator.hasNext()) {
                Element element = iterator.next();
                Element node = null;
                Element channel = element.element("channel");
                if (channel != null) {
                    node = channel.element("displayMajorNumber");
                    if (node != null) try {
                        int value = Integer.parseInt(node.getTextTrim());
                        if (value != video.getChannelMajorNumber()) break;
                        video.setChannelMajorNumber(value);
                    } catch (Exception ex) {
                        log.error("Could not set channel major number", ex);
                    }
                    node = channel.element("displayMinorNumber");
                    if (node != null) try {
                        int value = Integer.parseInt(node.getTextTrim());
                        if (value != video.getChannelMinorNumber()) break;
                        video.setChannelMinorNumber(value);
                    } catch (Exception ex) {
                        log.error("Could not set channel minor number", ex);
                    }
                    node = channel.element("callsign");
                    if (node != null && video.getCallsign() == null) video.setCallsign(node.getTextTrim());
                }
                node = element.element("partCount");
                if (node != null && video.getPartCount() == 0) try {
                    video.setPartCount(Integer.parseInt(node.getText()));
                } catch (Exception ex) {
                    log.error("Could not set part count", ex);
                }
                node = element.element("partIndex");
                if (node != null && video.getPartIndex() == 0) try {
                    video.setPartIndex(Integer.parseInt(node.getText()));
                } catch (Exception ex) {
                    log.error("Could not set part index", ex);
                }
                Element program = element.element("program");
                if (program != null) {
                    node = program.element("vActor");
                    if (node != null && video.getActors() == null) video.setActors(getElements(node));
                    node = program.element("vAdvisory");
                    if (node != null && video.getAdvisories() == null) video.setAdvisories(getElements(node));
                    node = program.element("vChoreographer");
                    if (node != null && video.getChoreographers() == null) video.setChoreographers(getElements(node));
                    node = program.element("vChoreographer");
                    if (node != null && video.getChoreographers() == null) video.setChoreographers(getElements(node));
                    node = program.element("colorCode");
                    if (node != null && video.getColor() == null) {
                        video.setColor(node.getText());
                        try {
                            video.setColorCode(Integer.parseInt(node.attribute("value").getText()));
                        } catch (Exception ex) {
                            log.error("Could not set color code", ex);
                        }
                    }
                    node = program.element("description");
                    if (node != null && video.getDescription() == null) video.setDescription(node.getTextTrim());
                    node = program.element("vDirector");
                    if (node != null && video.getDirectors() == null) video.setDirectors(getElements(node));
                    node = program.element("episodeNumber");
                    if (node != null && video.getEpisodeNumber() != 0) try {
                        video.setEpisodeNumber(Integer.parseInt(node.getTextTrim()));
                    } catch (Exception ex) {
                        log.error("Could not episode number", ex);
                    }
                    node = program.element("episodeTitle");
                    if (node != null && video.getEpisodeTitle() == null) video.setEpisodeTitle(node.getTextTrim());
                    node = program.element("vExecProducer");
                    if (node != null && video.getExecProducers() == null) video.setExecProducers(getElements(node));
                    node = program.element("vProgramGenre");
                    if (node != null && video.getProgramGenre() == null) video.setProgramGenre(getElements(node));
                    node = program.element("vGuestStar");
                    if (node != null && video.getGuestStars() == null) video.setGuestStars(getElements(node));
                    node = program.element("vHost");
                    if (node != null && video.getHosts() == null) video.setHosts(getElements(node));
                    node = program.element("isEpisode");
                    if (node != null && video.getEpisodic() == null) video.setEpisodic(Boolean.valueOf(node.getTextTrim()));
                    node = program.element("originalAirDate");
                    if (node != null && video.getOriginalAirDate() == null) {
                        ParsePosition pos = new ParsePosition(0);
                        Date date = smTimeDateFormat.parse(node.getTextTrim(), pos);
                        if (date == null) date = new Date(0);
                        video.setOriginalAirDate(date);
                    }
                    node = program.element("vProducer");
                    if (node != null && video.getProducers() == null) video.setProducers(getElements(node));
                    Element series = program.element("series");
                    if (series != null && video.getSeriesGenre() == null) {
                        node = series.element("vSeriesGenre");
                        if (node != null) video.setSeriesGenre(getElements(node));
                        node = series.element("seriesTitle");
                        if (node != null) video.setSeriesTitle(node.getTextTrim());
                    }
                    node = program.element("showType");
                    if (node != null && video.getShowType() == null) {
                        video.setShowType(node.getTextTrim());
                        try {
                            video.setShowTypeValue(Integer.parseInt(node.attribute("value").getText()));
                        } catch (Exception ex) {
                            log.error("Could not set showtype value", ex);
                        }
                    }
                    node = program.element("title");
                    if (node != null && video.getTitle() == null) video.setTitle(node.getTextTrim());
                    node = program.element("vWriter");
                    if (node != null && video.getWriters() == null) video.setWriters(getElements(node));
                }
                Element rating = element.element("tvRating");
                if (rating != null && video.getRating() == null) {
                    video.setRating(rating.getTextTrim());
                    try {
                        video.setRatingValue(Integer.parseInt(rating.attribute("value").getText()));
                    } catch (Exception ex) {
                        log.error("Could not set rating value", ex);
                    }
                }
            }
        }
        Element node = root.element("vBookmark");
        if (node != null) {
            StringBuffer buffer = new StringBuffer();
            int counter = 0;
            for (Iterator<Element> iterator = node.elementIterator("element"); iterator.hasNext(); ) {
                Element bookmarkElement = iterator.next();
                if (counter++ > 0) buffer.append(";");
                buffer.append(Tools.getAttribute(bookmarkElement, "time"));
            }
            video.setBookmarks(buffer.toString());
        }
        Element quality = root.element("recordingQuality");
        if (quality != null) {
            video.setRecordingQuality(quality.getTextTrim());
            try {
                video.setRecordingQualityValue(Integer.parseInt(quality.attribute("value").getText()));
            } catch (Exception ex) {
                log.error("Could not set quality value", ex);
            }
        }
        Element time = root.element("startTime");
        if (time != null) {
            ParsePosition pos = new ParsePosition(0);
            Date date = smTimeDateFormat.parse(time.getTextTrim(), pos);
            if (date == null) date = new Date(0);
            video.setStartTime(date);
        }
        time = root.element("stopTime");
        if (time != null) {
            ParsePosition pos = new ParsePosition(0);
            Date date = smTimeDateFormat.parse(time.getTextTrim(), pos);
            if (date == null) date = new Date(0);
            video.setStopTime(date);
        }
        time = root.element("expirationTime");
        if (time != null) {
            ParsePosition pos = new ParsePosition(0);
            Date date = smTimeDateFormat.parse(time.getTextTrim(), pos);
            if (date == null) date = new Date(0);
            video.setExpirationTime(date);
        }
    }

    private static String getElements(Element node) {
        StringBuffer buffer = new StringBuffer();
        int counter = 0;
        for (Iterator<Element> iterator = node.elementIterator("element"); iterator.hasNext(); ) {
            Element element = iterator.next();
            if (counter++ > 0) buffer.append(";");
            buffer.append(element.getTextTrim());
        }
        return buffer.toString();
    }

    public boolean Download(Video video, CancelDownload cancelDownload) {
        ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
        GoBackConfiguration goBackConfiguration = Server.getServer().getGoBackConfiguration();
        GetMethod get = null;
        try {
            URL url = new URL(video.getUrl());
            Protocol protocol = new Protocol("https", new TiVoSSLProtocolSocketFactory(), 443);
            HttpClient client = new HttpClient();
            client.getHostConfiguration().setHost(url.getHost(), 443, protocol);
            String password = Tools.decrypt(serverConfiguration.getMediaAccessKey());
            if (video.isParentalControls()) {
                if (serverConfiguration.getPassword() == null) throw new NullPointerException("Parental Controls Password is null");
                password = password + Tools.decrypt(serverConfiguration.getPassword());
            }
            Credentials credentials = new UsernamePasswordCredentials("tivo", password);
            client.getState().setCredentials(null, url.getHost(), credentials);
            get = new GetMethod(video.getUrl());
            client.executeMethod(get);
            if (get.getStatusCode() != 200) {
                log.debug("Status code: " + get.getStatusCode());
                return false;
            }
            InputStream input = get.getResponseBodyAsStream();
            String path = serverConfiguration.getRecordingsPath();
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String name = getFilename(video);
            File file = null;
            if (goBackConfiguration.isGroupByShow()) {
                if (video.getSeriesTitle() != null && video.getSeriesTitle().trim().length() > 0) {
                    path = path + File.separator + clean(video.getSeriesTitle());
                    File filePath = new File(path);
                    if (!filePath.exists()) filePath.mkdirs();
                    file = new File(path + File.separator + name);
                } else file = new File(path + File.separator + name);
            } else {
                file = new File(path + File.separator + name);
            }
            log.info("Downloading: " + name);
            WritableByteChannel channel = new FileOutputStream(file, false).getChannel();
            long total = 0;
            double diff = 0.0;
            ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 4);
            byte[] bytes = new byte[1024 * 4];
            int amount = 0;
            int index = 0;
            long target = video.getSize();
            long start = System.currentTimeMillis();
            long last = start;
            while (amount == 0 && total < target) {
                while (amount >= 0 && !cancelDownload.cancel()) {
                    if (index == amount) {
                        amount = input.read(bytes);
                        index = 0;
                        total = total + amount;
                    }
                    while (index < amount && buf.hasRemaining()) {
                        buf.put(bytes[index++]);
                    }
                    buf.flip();
                    channel.write(buf);
                    if (buf.hasRemaining()) {
                        buf.compact();
                    } else {
                        buf.clear();
                    }
                    if ((System.currentTimeMillis() - last > 10000) && (total > 0)) {
                        try {
                            video = VideoManager.retrieveVideo(video.getId());
                            if (video.getStatus() == Video.STATUS_DOWNLOADING) {
                                diff = (System.currentTimeMillis() - start) / 1000.0;
                                if (diff > 0) {
                                    video.setDownloadSize(total);
                                    video.setDownloadTime((int) diff);
                                    VideoManager.updateVideo(video);
                                }
                            }
                        } catch (HibernateException ex) {
                            log.error("Video update failed", ex);
                        }
                        last = System.currentTimeMillis();
                    }
                }
                if (cancelDownload.cancel()) {
                    channel.close();
                    return false;
                }
            }
            diff = (System.currentTimeMillis() - start) / 1000.0;
            channel.close();
            if (diff != 0) log.info("Download rate=" + (total / 1024) / diff + " KBps");
            try {
                video.setPath(file.getCanonicalPath());
                VideoManager.updateVideo(video);
            } catch (HibernateException ex) {
                log.error("Video update failed", ex);
            }
        } catch (MalformedURLException ex) {
            Tools.logException(ToGo.class, ex, video.getUrl());
            return false;
        } catch (Exception ex) {
            Tools.logException(ToGo.class, ex, video.getUrl());
            return false;
        } finally {
            if (get != null) get.releaseConnection();
        }
        return true;
    }

    private String getFilename(Video video) {
        String name = video.getTitle();
        if (video.getEpisodeTitle() != null && video.getEpisodeTitle().length() > 0) name = name + " - " + video.getEpisodeTitle();
        mCalendar.setTime(video.getDateRecorded());
        mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60 + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
        mCalendar.set(GregorianCalendar.SECOND, 0);
        name = name + " (Recorded " + mFileDateFormat.format(mCalendar.getTime());
        name = name + ", " + video.getStation() + ")";
        return clean(name) + ".TiVo";
    }

    private String clean(String value) {
        value = value.replaceAll(":", " ");
        value = value.replaceAll("\\\\", " ");
        value = value.replaceAll("/", " ");
        value = value.replaceAll("\"", " ");
        value = value.replaceAll("<", " ");
        value = value.replaceAll(">", " ");
        value = value.replaceAll("=", " ");
        value = value.replaceAll("\\*", " ");
        value = value.replaceAll("\\?", " ");
        value = value.replaceAll("'", "");
        value = value.replaceAll(",", "");
        return value;
    }

    public Video pickNextVideoForDownloading() {
        Video next = null;
        try {
            List<Video> recordings = VideoManager.listAll();
            Collections.sort(recordings, new Comparator<Object>() {

                public int compare(Object o1, Object o2) {
                    Video contact1 = (Video) o1;
                    Video contact2 = (Video) o2;
                    Date d1 = contact1.getDateRecorded();
                    Date d2 = contact2.getDateRecorded();
                    if (d1 == null && d2 == null) return 0; else if (d1 == null) return -1; else if (d2 == null) return 1; else return d1.compareTo(d2);
                }
            });
            Iterator<Video> downloadedIterator = recordings.iterator();
            while (downloadedIterator.hasNext()) {
                Video downloadedvideo = downloadedIterator.next();
                if (downloadedvideo.getStatus() == Video.STATUS_DOWNLOADING) {
                    next = downloadedvideo;
                    break;
                } else if (downloadedvideo.getStatus() == Video.STATUS_USER_SELECTED) {
                    next = downloadedvideo;
                    break;
                } else if (downloadedvideo.getStatus() == Video.STATUS_RULE_MATCHED) {
                    next = downloadedvideo;
                    break;
                }
            }
            recordings.clear();
        } catch (HibernateException ex) {
            log.error("Getting recordings failed", ex);
        }
        return next;
    }

    public void applyRules() {
        try {
            List<Video> recordings = VideoManager.listAll();
            List<Rule> rules = Server.getServer().getRules();
            List<TiVo> tivos = Server.getServer().getTiVos();
            Iterator<Video> iterator = recordings.iterator();
            while (iterator.hasNext()) {
                Video video = iterator.next();
                if (video.getStatus() != Video.STATUS_RECORDING && video.getStatus() != Video.STATUS_DOWNLOADED && video.getStatus() != Video.STATUS_USER_CANCELLED && video.getStatus() != Video.STATUS_INCOMPLETE && video.getStatus() != Video.STATUS_DELETED && video.getStatus() != Video.STATUS_PROTECTED) {
                    boolean prohibited = false;
                    boolean found = false;
                    if (video.getSource() != null) {
                        for (int j = 0; j < tivos.size(); j++) {
                            TiVo tivo = (TiVo) tivos.get(j);
                            if (tivo.getAddress().equals(video.getSource())) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        Iterator<Rule> rulesIterator = rules.iterator();
                        while (rulesIterator.hasNext()) {
                            Rule rule = rulesIterator.next();
                            if (rule.match(video) && !rule.getDownload()) {
                                prohibited = true;
                                break;
                            }
                        }
                        if (!prohibited) {
                            boolean matched = false;
                            rulesIterator = rules.iterator();
                            while (rulesIterator.hasNext()) {
                                Rule rule = (Rule) rulesIterator.next();
                                if (rule.match(video) && rule.getDownload()) {
                                    matched = true;
                                    break;
                                }
                            }
                            if (matched) {
                                video.setStatus(Video.STATUS_RULE_MATCHED);
                                try {
                                    VideoManager.updateVideo(video);
                                } catch (HibernateException ex) {
                                    log.error("Video update failed", ex);
                                }
                            }
                        }
                    }
                }
            }
            recordings.clear();
        } catch (HibernateException ex) {
            log.error("Getting recordings failed", ex);
        }
    }

    protected SimpleDateFormat mFileDateFormat;

    protected GregorianCalendar mCalendar;
}
