package com.sounnecessary.flixfinder;

import com.tms.webservices.applications.xtvd.Lineup;
import com.tms.webservices.applications.xtvd.Parser;
import com.tms.webservices.applications.xtvd.ParserFactory;
import com.tms.webservices.applications.xtvd.Program;
import com.tms.webservices.applications.xtvd.SOAPRequest;
import com.tms.webservices.applications.xtvd.Schedule;
import com.tms.webservices.applications.xtvd.Station;
import com.tms.webservices.applications.xtvd.Xtvd;
import org.gnu.stealthp.rsslib.RSSHandler;
import org.gnu.stealthp.rsslib.RSSItem;
import org.gnu.stealthp.rsslib.RSSParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class FlixFinder {

    public Xtvd _xtvd = new Xtvd();

    public Map _channel = new HashMap();

    public Map _nflink = new HashMap();

    public Set _nfqueue = new HashSet();

    public HashSet _matches = new HashSet();

    public HashSet _movies = new HashSet();

    private static final String PROPERTIES_FILE_NAME = "config.xml";

    private static final String DATA_FILE_NAME = "__ddcache";

    private String _nfURI = "http://rss.netflix.com/Top100RSS";

    private int _daysOfData = 7;

    private String _password = null;

    private String _userName = null;

    private String _webserviceURI = null;

    public FlixFinder() {
        try {
            getConfig();
        } catch (Exception e) {
            System.err.println("Warning: Couldn't load some or all of config from " + PROPERTIES_FILE_NAME + ". Using defaults.");
        }
    }

    private void getConfig() throws Exception {
        String fileName = getPropertiesFile();
        ConfigProperties config = new ConfigProperties(fileName);
        _userName = config.getUserName();
        _password = config.getPassword();
        int daysOfData = config.getDaysOfData();
        if (daysOfData >= 1 && daysOfData <= 14) {
            _daysOfData = daysOfData;
        }
        _webserviceURI = config.getWebserviceURI();
        String nfURI = config.getNfURI();
        if (nfURI != null && !nfURI.equals("")) {
            _nfURI = nfURI;
        }
    }

    private void cacheNFrss() throws Exception {
        RSSHandler hand = new RSSHandler();
        RSSParser.parseXmlFile(new URL(_nfURI), hand, false);
        LinkedList queue = hand.getRSSChannel().getItems();
        _nfqueue = new HashSet();
        _nflink = new HashMap();
        Iterator queueiter = queue.listIterator();
        while (queueiter.hasNext()) {
            RSSItem movie = (RSSItem) queueiter.next();
            String netflix_title = movie.getTitle().substring(movie.getTitle().indexOf(' ')).trim().toLowerCase();
            _nfqueue.add(netflix_title);
            _nflink.put(netflix_title, movie.getLink());
        }
    }

    private void fetchGuide(String fileName) throws Exception {
        SOAPRequest soapRequest;
        if (_webserviceURI == null || _webserviceURI.equals("")) {
            soapRequest = new SOAPRequest(_userName, _password);
        } else {
            soapRequest = new SOAPRequest(_userName, _password, _webserviceURI);
        }
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTime(new Date());
        end.setTime(start.getTime());
        end.add(Calendar.DAY_OF_YEAR, _daysOfData);
        soapRequest.getDataFile(start, end, fileName);
    }

    private void cacheXTVD(boolean fresh) throws Exception {
        String dataDirectory = System.getProperty("dataDirectory", ".");
        String fileName = dataDirectory + File.separator + DATA_FILE_NAME;
        if (fresh) {
            fetchGuide(fileName);
        }
        _xtvd = new Xtvd();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
        Parser parser = ParserFactory.getXtvdParser(reader, _xtvd);
        parser.parseXTVD();
        reader.close();
        Map lineups = _xtvd.getLineups();
        if (lineups.size() != 1) {
            throw new Exception("FATAL: more than one cable lineup on your acct");
        }
        Lineup lineup = (Lineup) _xtvd.getLineups().values().iterator().next();
        _channel = new HashMap();
        Iterator mapiter = lineup.getMap().iterator();
        while (mapiter.hasNext()) {
            com.tms.webservices.applications.xtvd.Map m = (com.tms.webservices.applications.xtvd.Map) mapiter.next();
            Integer stationid = new Integer(m.getStation());
            if (_channel.containsKey(stationid)) {
                System.err.println("Dup channel for " + ((Station) _xtvd.getStations().get(stationid)).getCallSign() + ". Keeping " + _channel.get(stationid) + ", discarding " + m.getChannel());
            } else {
                _channel.put(stationid, m.getChannel());
            }
        }
    }

    public String getChannel(Integer stationid) {
        return (String) _channel.get(stationid);
    }

    private void cacheMatches() {
        _matches = new HashSet();
        Map programs = _xtvd.getPrograms();
        if (programs != null) {
            Iterator progiter = _xtvd.getPrograms().values().iterator();
            while (progiter.hasNext()) {
                Program p = (Program) progiter.next();
                if (p.getId().startsWith("MV")) {
                    String guide_title = p.getTitle().trim().toLowerCase();
                    if (_nfqueue.contains(guide_title)) {
                        _matches.add(p.getId());
                    }
                }
            }
        }
    }

    private void cacheMovies() {
        _movies = new HashSet();
        Collection schedules = _xtvd.getSchedules();
        if (schedules != null) {
            Iterator schediter = schedules.iterator();
            while (schediter.hasNext()) {
                Schedule s = (Schedule) schediter.next();
                if (_matches.contains(s.getProgram())) {
                    Program p = (Program) _xtvd.getPrograms().get(s.getProgram());
                    Integer stationid = new Integer(s.getStation());
                    Station st = (Station) _xtvd.getStations().get(stationid);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    _movies.add(new Movie(new String[] { sdf.format(s.getTime().getLocalDate()), getChannel(stationid) + "-" + st.getCallSign(), p.getTitle() + " (" + p.getYear() + ")", p.getStarRating().toString(), p.getMpaaRating().toString(), s.getDuration().getHours() + "h " + s.getDuration().getMinutes() + "m", p.getDescription() }));
                }
            }
        }
    }

    public void match(boolean fresh) throws Exception {
        long start = System.currentTimeMillis();
        cacheNFrss();
        System.err.println("cacheNFrss() complete. Elapsed seconds: " + ((System.currentTimeMillis() - start) / 1000.0));
        cacheXTVD(fresh);
        System.err.println("cacheXTVD() complete. Elapsed seconds: " + ((System.currentTimeMillis() - start) / 1000.0));
        cacheMatches();
        System.err.println("cacheMatches() complete. Elapsed seconds: " + ((System.currentTimeMillis() - start) / 1000.0));
        cacheMovies();
        System.err.println("cacheMovies() complete. Elapsed seconds: " + ((System.currentTimeMillis() - start) / 1000.0));
    }

    public HashSet getMovies() {
        return _movies;
    }

    public static String getDataFile() {
        String dataDirectory = System.getProperty("dataDirectory", ".");
        return (dataDirectory + File.separator + DATA_FILE_NAME);
    }

    public static String getPropertiesFile() {
        String propertiesDirectory = System.getProperty("propertiesDirectory", ".");
        return (propertiesDirectory + File.separator + PROPERTIES_FILE_NAME);
    }
}
