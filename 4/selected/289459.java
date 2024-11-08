package de.guidoludwig.jtrade.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import de.guidoludwig.jtrade.ErrorMessage;
import de.guidoludwig.jtrade.domain.AllArchivesModel;
import de.guidoludwig.jtrade.domain.Archive;
import de.guidoludwig.jtrade.domain.Artist;
import de.guidoludwig.jtrade.domain.FuzzyDate;
import de.guidoludwig.jtrade.domain.Location;
import de.guidoludwig.jtrade.domain.Media;
import de.guidoludwig.jtrade.domain.Musician;
import de.guidoludwig.jtrade.domain.Show;
import de.guidoludwig.jtrade.domain.Song;
import de.guidoludwig.jtrade.domain.TimePoint;
import de.guidoludwig.jtrade.domain.Trader;
import de.guidoludwig.jtrade.enumtype.Grade;
import de.guidoludwig.jtrade.enumtype.MediaType;
import de.guidoludwig.jtrade.enumtype.Source;
import de.guidoludwig.jtrade.enumtype.TradeStatus;
import de.guidoludwig.jtrade.install.JTradeProperties;
import de.guidoludwig.jtrade.install.Version;
import de.guidoludwig.jtrade.ui.MinuteSecondFormatter;

/**
 * Read & write JTrade XML
 * 
 * @author <a href="mailto:jtrade@gigabss.de">Guido Ludwig </a>
 * @version $Revision: 1.28 $
 */
public class IO {

    private static final String VALUE_GENERAL = "general";

    private static final String ELEMENT_TRADELIST = "tradelist";

    private static final String ELEMENT_JTRADE = "jtrade";

    private static final String ELEMENT_ARCHIVE = "archive";

    private static final String ELEMENT_ARCHIVES = "archives";

    private static final String ELEMENT_INSTRUMENT = "instrument";

    private static final String ATTRIB_KBPS = "kbps";

    private static final String ATTRIB_MP3 = "mp3";

    private static final String ATTRIB_QUANTITY = "quantity";

    private static final String ATTRIB_TYPE = "type";

    private static final String ATTRIB_ARCHIVE = "archive";

    private static final String ELEMENT_COUNTRY = "country";

    private static final String ELEMENT_CITY = "city";

    private static final String ELEMENT_VENUE = "venue";

    private static final String VALUE_RECORDING = "recording";

    private static final String ATTRIB_QUALIFIER = "qualifier";

    private static final String ELEMENT_RECORDINGREMARKS = "recordingremarks";

    private static final String ELEMENT_SHOWREMARKS = "showremarks";

    private static final String ELEMENT_CDSEPARATOR = "cdseparator";

    private static final String ELEMENT_SONG = "song";

    private static final String ELEMENT_SETLIST = "setlist";

    private static final String ELEMENT_MUSICIAN = "musician";

    private static final String ELEMENT_LINEUP = "lineup";

    private static final String ATTRIB_LENGTH = "length";

    private static final String ELEMENT_LENGTH = "length";

    private static final String ELEMENT_TRADERNAME = "tradername";

    private static final String ELEMENT_RECEIVED = "received";

    private static final String ELEMENT_MEDIATYPE = "mediatype";

    private static final String ELEMENT_MEDIA = "media";

    private static final String ELEMENT_LOCATION = "location";

    private static final String ATTRIB_DATE = "date";

    private static final String ELEMENT_DATE = "date";

    private static final String ATTRIB_MASTER = "master";

    private static final String ATTRIB_GRADE = "grade";

    private static final String ATTRIB_SOURCE = "source";

    private static final String ATTRIB_STATUS = "status";

    private static final String ELEMENT_SHOW = "show";

    private static final String ELEMENT_FTX = "ftx";

    private static final String ELEMENT_REMARKS = "remarks";

    private static final String ELEMENT_URL = "url";

    private static final String ELEMENT_NAME = "name";

    private static final String ATTRIB_SORT = "sort";

    private static final String ENCODING = "ISO-8859-1";

    private static final String ELEMENT_ARTIST = "artist";

    private static final String ATTRIB_ID = "id";

    private static final String ATTRIB_STATE = "state";

    private static final String ELEMENT_VERSION = "version";

    private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static final List<Artist> readArtists = new ArrayList<Artist>();

    /**
	 * Avoid instantiation
	 */
    private IO() {
    }

    public static void read() {
        readArtists.clear();
        String filename = JTradeProperties.INSTANCE.getHome() + File.separator + JTradeProperties.INSTANCE.getProperty(JTradeProperties.DATA_DIRECTORY) + File.separator + "tradelist.xml";
        File file = new File(filename);
        if (!file.exists()) {
            int option = JOptionPane.showConfirmDialog(null, "No data file found \nStart with an empty Tradelist ?", "Tradelist Data not found", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.NO_OPTION) {
                System.exit(0);
            }
            return;
        }
        read(filename);
    }

    public static Collection<Trader> loadTraders() {
        ArrayList<Trader> traders = new ArrayList<Trader>();
        Trader t1 = new Trader();
        t1.setName("Name 1");
        t1.setEmail("a@b.c");
        traders.add(t1);
        Trader t2 = new Trader();
        t2.setName("Name 2");
        t2.setEmail("d@e.f");
        traders.add(t2);
        return traders;
    }

    @SuppressWarnings("unchecked")
    public static void read(String filename) {
        Artist artist = null;
        Archive archive = null;
        SAXReader reader = new SAXReader();
        Document document = null;
        try {
            document = reader.read(filename);
        } catch (DocumentException e) {
            ErrorMessage.handle(e);
            return;
        }
        Element root = document.getRootElement();
        Element tradelist = null;
        Element jtrade = null;
        Element version = null;
        Element archives = null;
        if (root.getName().equals(ELEMENT_TRADELIST)) {
            version = root.element(ELEMENT_VERSION);
            tradelist = root;
        } else {
            jtrade = root;
            version = jtrade.element(ELEMENT_VERSION);
            tradelist = jtrade.element(ELEMENT_TRADELIST);
            archives = jtrade.element(ELEMENT_ARCHIVES);
        }
        String dtdVersion = version.attributeValue(ATTRIB_ID);
        if (archives != null) {
            for (Iterator<Element> iter = archives.elementIterator(ELEMENT_ARCHIVE); iter.hasNext(); ) {
                Element element = iter.next();
                archive = createArchive(dtdVersion, element);
                AllArchivesModel.getInstance().add(archive);
            }
        }
        for (Iterator<Element> iter = tradelist.elementIterator(ELEMENT_ARTIST); iter.hasNext(); ) {
            Element element = iter.next();
            artist = createArtist(dtdVersion, element);
            readArtists.add(artist);
        }
    }

    public static Document createDocument(List<Artist> artists) {
        return createDocument(artists, true);
    }

    public static Document createDocument(List<Artist> artists, boolean includePrivate) {
        Document document = createRoot(ELEMENT_TRADELIST);
        Element root = document.getRootElement();
        for (Artist artist : artists) {
            List<Show> shows = includePrivate ? artist.getShows() : artist.getNonPrivateShows();
            if (shows.size() > 0) {
                Element artistElement = createArtistElement(root, artist);
                for (Show show : shows) {
                    createShowElement(artistElement, show);
                }
            }
        }
        return document;
    }

    public static void write(List<Archive> archives, List<Artist> artists) {
        String filename = JTradeProperties.INSTANCE.getProperty(JTradeProperties.DATA_DIRECTORY) + "/tradelist.xml";
        write(archives, artists, filename, true);
    }

    public static void write(List<Archive> archives, List<Artist> artists, String filename, boolean backup) {
        XMLWriter xmlWriter;
        try {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding(ENCODING);
            FileWriter fileWriter = new FileWriter(filename);
            xmlWriter = new XMLWriter(fileWriter, format);
            Document doc = createRoot();
            Element root = doc.getRootElement();
            Element archivesEl = root.addElement(ELEMENT_ARCHIVES);
            Element tradelist = root.addElement(ELEMENT_TRADELIST);
            for (Archive archive : archives) {
                createArchiveElement(archivesEl, archive);
            }
            for (Artist artist : artists) {
                Element artistElement = createArtistElement(tradelist, artist);
                for (Show show : artist.getShows()) {
                    createShowElement(artistElement, show);
                }
            }
            xmlWriter.write(doc);
            xmlWriter.close();
            if (backup) {
                DateFormat df = new SimpleDateFormat("yyyyMMddhhmmss");
                String timestamp = df.format(new Date());
                String backupFN = filename + "." + timestamp;
                FileUtils.copyFile(new File(filename), new File(backupFN));
            }
        } catch (IOException e) {
            ErrorMessage.handle(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Artist createArtist(String dtdVersion, Element element) {
        Artist artist = null;
        String ids = element.attributeValue(ATTRIB_ID);
        if (StringUtils.isBlank(ids)) {
            artist = new Artist();
        } else {
            artist = new Artist(Long.valueOf(ids).longValue());
        }
        artist.setVariantSort(element.attributeValue(ATTRIB_SORT));
        artist.setName(element.elementTextTrim(ELEMENT_NAME));
        artist.setUrl(element.elementTextTrim(ELEMENT_URL));
        if (StringUtils.isBlank(dtdVersion)) {
            artist.setRemarks(element.elementTextTrim(ELEMENT_REMARKS));
        } else {
            artist.setRemarks(element.elementTextTrim(ELEMENT_FTX));
        }
        for (Iterator<Element> iter = element.elementIterator(ELEMENT_SHOW); iter.hasNext(); ) {
            Element showElement = iter.next();
            artist.addShow(createShow(dtdVersion, artist, showElement));
        }
        return artist;
    }

    private static Archive createArchive(String dtdVersion, Element element) {
        Archive archive = new Archive();
        String ids = element.attributeValue(ATTRIB_ID);
        archive.setId(Integer.valueOf(ids).intValue());
        archive.setName(element.elementTextTrim(ELEMENT_NAME));
        return archive;
    }

    @SuppressWarnings("unchecked")
    public static Show createShow(String dtdVersion, Artist artist, Element showElement) {
        Show show = null;
        String ids = showElement.attributeValue(ATTRIB_ID);
        if (StringUtils.isBlank(ids)) {
            show = new Show();
        } else {
            show = new Show(Long.valueOf(ids).longValue());
        }
        show.setArtist(artist);
        try {
            String status = showElement.attributeValue(ATTRIB_STATUS);
            if (status != null) {
                show.setStatus(TradeStatus.valueOf(showElement.attributeValue(ATTRIB_STATUS).toUpperCase()));
            }
        } catch (IllegalArgumentException e) {
            ErrorMessage.handle(e);
        }
        show.setArchive(AllArchivesModel.getInstance().getArchiveFor(showElement.attributeValue(ATTRIB_ARCHIVE)));
        String sourceString = StringUtils.defaultString(showElement.attributeValue(ATTRIB_SOURCE)).toUpperCase();
        if (!StringUtils.isBlank(sourceString) && !"NULL".equals(sourceString)) {
            try {
                show.setSource(Source.valueOf(sourceString));
            } catch (IllegalArgumentException e) {
                ErrorMessage.handle(e);
                show.setSource(Source.UNKNOWN);
            }
        }
        try {
            show.setRecordingGrade(Grade.valueOf(showElement.attributeValue(ATTRIB_GRADE).toUpperCase()));
        } catch (IllegalArgumentException e) {
            try {
                show.setRecordingGrade(Grade.valueOfDisplay(showElement.attributeValue(ATTRIB_GRADE).toUpperCase()));
            } catch (IllegalArgumentException e1) {
                ErrorMessage.handle(e);
                show.setRecordingGrade(Grade.UNGRADED);
            }
        }
        show.setName(showElement.elementTextTrim(ELEMENT_NAME));
        if (!StringUtils.isBlank(dtdVersion)) {
            show.setMaster(Boolean.valueOf(showElement.attributeValue(ATTRIB_MASTER)).booleanValue());
        }
        show.setLocation(createLocation(showElement.element(ELEMENT_LOCATION)));
        if (StringUtils.isBlank(dtdVersion)) {
            show.setShowDate(new FuzzyDate(showElement.elementTextTrim(ELEMENT_DATE)));
        } else {
            show.setShowDate(new FuzzyDate(showElement.attributeValue(ATTRIB_DATE)));
        }
        Element medEl = showElement.element(ELEMENT_MEDIA);
        if (medEl != null) {
            for (Iterator<Element> iter = medEl.elementIterator(ELEMENT_MEDIATYPE); iter.hasNext(); ) {
                try {
                    show.addMedia(createMedia(dtdVersion, iter.next()));
                } catch (IllegalArgumentException ex) {
                    ErrorMessage.handle(show.getArtist() + "/" + show.getShowDate(), ex);
                }
            }
        }
        Element rec = showElement.element(ELEMENT_RECEIVED);
        if (rec != null) {
            String isoDateString = null;
            if (StringUtils.isBlank(dtdVersion)) {
                isoDateString = rec.element(ELEMENT_DATE).getTextTrim();
            } else {
                isoDateString = rec.attributeValue(ATTRIB_DATE);
            }
            if (!StringUtils.isBlank(isoDateString) && !isoDateString.trim().startsWith("????")) {
                try {
                    show.setReceivedDate(isoDateFormat.parse(isoDateString.trim()));
                } catch (ParseException e1) {
                    ErrorMessage.handle(e1, "Parsing error on " + isoDateString);
                }
            }
            if (StringUtils.isBlank(dtdVersion)) {
                show.setMaster(rec.element(ATTRIB_MASTER) != null);
            }
            show.setTrader(rec.elementTextTrim(ELEMENT_TRADERNAME));
        }
        TimePoint length = null;
        String lengthString = null;
        if (StringUtils.isBlank(dtdVersion)) {
            lengthString = showElement.elementTextTrim(ELEMENT_LENGTH);
        } else {
            lengthString = showElement.attributeValue(ATTRIB_LENGTH);
        }
        if (!StringUtils.isBlank(lengthString)) {
            try {
                length = MinuteSecondFormatter.INSTANCE_HOURS.stringToTimePoint(lengthString);
            } catch (ParseException e1) {
                ErrorMessage.handle(e1, "'" + lengthString + "'");
            }
            show.setLength(length);
        }
        Element lu = showElement.element(ELEMENT_LINEUP);
        if (lu != null) {
            for (Iterator<Element> iter = lu.elementIterator(ELEMENT_MUSICIAN); iter.hasNext(); ) {
                show.addMusician(createMusician(iter.next()));
            }
        }
        Element sl = showElement.element(ELEMENT_SETLIST);
        if (sl != null) {
            for (Iterator<Element> iter = sl.elementIterator(); iter.hasNext(); ) {
                Element element = iter.next();
                if (element.getName().equals(ELEMENT_SONG)) {
                    show.addSongToSetlist(element.elementTextTrim(ELEMENT_NAME));
                } else if (element.getName().equals(ELEMENT_CDSEPARATOR)) {
                    show.addSongToSetlist(Show.CDSEPARATOR);
                }
            }
        }
        if (StringUtils.isBlank(dtdVersion)) {
            show.setShowRemarks(showElement.elementTextTrim(ELEMENT_SHOWREMARKS));
            show.setRecordingRemarks(showElement.elementTextTrim(ELEMENT_RECORDINGREMARKS));
        } else {
            for (Iterator<Element> iter = showElement.elementIterator(ELEMENT_FTX); iter.hasNext(); ) {
                Element ftx = iter.next();
                String rem = null;
                if (ftx.nodeCount() > 0) {
                    rem = ftx.node(0).getText().trim();
                } else {
                    rem = ftx.getTextTrim();
                }
                if (ftx.attributeValue(ATTRIB_QUALIFIER).equals(ELEMENT_SHOW)) {
                    show.setShowRemarks(rem);
                } else if (ftx.attributeValue(ATTRIB_QUALIFIER).equals(VALUE_RECORDING)) {
                    show.setRecordingRemarks(rem);
                }
            }
        }
        List<Song> set = show.getSetlist();
        String s1 = set == null ? null : (set.size() > 0 ? set.get(0).toString() : null);
        if (StringUtils.equals(show.getName(), s1)) {
            show.setName(null);
        } else if (StringUtils.equals(show.getName(), artist.getName())) {
            show.setName(null);
        }
        return show;
    }

    private static Location createLocation(Element locationElement) {
        Location location = new Location();
        location.setVenue(locationElement.elementTextTrim(ELEMENT_VENUE));
        location.setCity(locationElement.elementTextTrim(ELEMENT_CITY));
        location.setState(locationElement.element(ELEMENT_CITY).attributeValue(ATTRIB_STATE));
        location.setCountry(locationElement.elementTextTrim(ELEMENT_COUNTRY));
        return location;
    }

    private static Media createMedia(String dtdVersion, Element mediaElement) {
        Media media = new Media();
        String type = mediaElement.attributeValue(ATTRIB_TYPE).toUpperCase();
        try {
            media.setMediaType(MediaType.valueOf(type));
        } catch (IllegalArgumentException ex) {
            throw ex;
        }
        media.setQuantity(1);
        String qty = mediaElement.attributeValue(ATTRIB_QUANTITY);
        if (qty != null && qty.length() > 0) {
            int i = (new Integer(qty)).intValue();
            media.setQuantity(i);
        }
        if (media.getMediaType().isCompressed()) {
            String m = null;
            if (StringUtils.isBlank(dtdVersion)) {
                m = mediaElement.attributeValue(ATTRIB_MP3);
            } else {
                m = mediaElement.attributeValue(ATTRIB_KBPS);
            }
            if (!StringUtils.isBlank(m)) {
                int i = (new Integer(m)).intValue();
                media.setKbps(i);
            }
        }
        return media;
    }

    private static Musician createMusician(Element musicianElement) {
        Musician musician = new Musician();
        musician.setName(musicianElement.elementTextTrim(ELEMENT_NAME));
        musician.setInstruments(musicianElement.elementTextTrim(ELEMENT_INSTRUMENT));
        return musician;
    }

    private static Document createRoot() {
        return createRoot(ELEMENT_JTRADE);
    }

    private static Document createRoot(String rootElement) {
        Document document = DocumentFactory.getInstance().createDocument(ENCODING);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).finer(document.getXMLEncoding());
        document.addDocType(rootElement, null, Version.DTD);
        Element root = document.addElement(rootElement);
        root.addElement(ELEMENT_VERSION).addAttribute(ATTRIB_ID, Version.DTD);
        return document;
    }

    private static Element createArchiveElement(Element parent, Archive archive) {
        Element archiveElement = parent.addElement(ELEMENT_ARCHIVE).addAttribute(ATTRIB_ID, String.valueOf(archive.getId()));
        archiveElement.addElement(ELEMENT_NAME).addText(StringUtils.defaultString((StringUtils.defaultString(archive.getName()))));
        return archiveElement;
    }

    private static Element createArtistElement(Element parent, Artist artist) {
        Element artistElement = parent.addElement(ELEMENT_ARTIST).addAttribute(ATTRIB_ID, String.valueOf(artist.getId())).addAttribute(ATTRIB_SORT, artist.getVariantSort());
        artistElement.addElement(ELEMENT_NAME).addText(StringUtils.defaultString((StringUtils.defaultString(artist.getName()))));
        artistElement.addElement(ELEMENT_FTX).addAttribute(ATTRIB_QUALIFIER, VALUE_GENERAL).addText(StringUtils.defaultString((artist.getRemarks())));
        return artistElement;
    }

    private static Element createShowElement(Element parent, Show show) {
        String status = show.getStatus() == null ? null : show.getStatus().name();
        String source = show.getSource() == null ? "" : show.getSource().name();
        String grade = show.getRecordingGrade() == null ? null : show.getRecordingGrade().name();
        String archive = show.getArchive() == null ? "" : String.valueOf(show.getArchive().getId());
        Element showElement = parent.addElement(ELEMENT_SHOW).addAttribute(ATTRIB_ID, String.valueOf(show.getId())).addAttribute(ATTRIB_STATUS, status).addAttribute(ATTRIB_SOURCE, source).addAttribute(ATTRIB_GRADE, grade).addAttribute(ATTRIB_ARCHIVE, archive).addAttribute(ELEMENT_DATE, StringUtils.trim(show.getShowDate().toString())).addAttribute(ATTRIB_MASTER, String.valueOf(show.isMaster()));
        showElement.addAttribute(ATTRIB_LENGTH, StringUtils.trim(MinuteSecondFormatter.INSTANCE_HOURS.valueToString(show.getLength())));
        showElement.addElement(ELEMENT_NAME).addText(StringUtils.defaultString((show.getName())));
        Element location = showElement.addElement(ELEMENT_LOCATION);
        location.addElement(ELEMENT_VENUE).addText(StringUtils.defaultString((show.getLocation().getVenue())));
        Element city = location.addElement(ELEMENT_CITY);
        city.addText(StringUtils.defaultString((show.getLocation().getCity())));
        if (!StringUtils.isBlank(show.getLocation().getState())) {
            city.addAttribute(ATTRIB_STATE, show.getLocation().getState());
        }
        location.addElement(ELEMENT_COUNTRY).addText(StringUtils.defaultString((show.getLocation().getCountry())));
        Element received = showElement.addElement(ELEMENT_RECEIVED);
        Date date = show.getReceivedDate();
        if (date != null) {
            received.addAttribute(ELEMENT_DATE, isoDateFormat.format(show.getReceivedDate()));
        }
        if (!show.isMaster()) {
            received.addElement(ELEMENT_TRADERNAME).addText(StringUtils.defaultString((show.getTrader())));
        }
        Element mediaElement = showElement.addElement(ELEMENT_MEDIA);
        for (Media media : show.getMedia()) {
            if (media != null && media.getMediaType() != null && media.getMediaType() != MediaType.EMPTY) {
                Element mediaType = mediaElement.addElement(ELEMENT_MEDIATYPE).addAttribute(ATTRIB_TYPE, media.getMediaType().name());
                if (media.getQuantity() != 0) {
                    mediaType.addAttribute(ATTRIB_QUANTITY, String.valueOf(media.getQuantity()));
                }
                if (media.getMediaType().isCompressed() && media.getKbps() != 0) {
                    mediaType.addAttribute(ATTRIB_KBPS, String.valueOf(media.getKbps()));
                }
            }
        }
        List<Musician> musicians = show.getMusicians();
        if (musicians.size() > 0) {
            Element lineup = showElement.addElement(ELEMENT_LINEUP);
            for (Musician musician : musicians) {
                Element musicianElement = lineup.addElement(ELEMENT_MUSICIAN);
                musicianElement.addElement(ELEMENT_NAME).addText(StringUtils.defaultString((musician.getName())));
                musicianElement.addElement(ELEMENT_INSTRUMENT).addText(StringUtils.defaultString((musician.getInstruments())));
            }
        }
        List<Song> setlist = show.getSetlist();
        if (setlist.size() > 0) {
            Element setlistElement = showElement.addElement(ELEMENT_SETLIST);
            for (Song song : setlist) {
                if (song.getName().equals(Show.CDSEPARATOR)) {
                    setlistElement.addElement(ELEMENT_CDSEPARATOR);
                } else {
                    setlistElement.addElement(ELEMENT_SONG).addElement(ELEMENT_NAME).addText(song.toString());
                }
            }
        }
        if (!StringUtils.isBlank(show.getShowRemarks())) {
            showElement.addElement(ELEMENT_FTX).addAttribute(ATTRIB_QUALIFIER, ELEMENT_SHOW).addCDATA(StringUtils.defaultString((show.getShowRemarks())));
        }
        if (!StringUtils.isBlank(show.getRecordingRemarks())) {
            showElement.addElement(ELEMENT_FTX).addAttribute(ATTRIB_QUALIFIER, VALUE_RECORDING).addCDATA(StringUtils.defaultString((show.getRecordingRemarks())));
        }
        return showElement;
    }
}
