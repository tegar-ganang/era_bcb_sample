package jreader;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Zawiera metody potrzebne do importowania i eksportowania kanałów do plików
 * zapisanych w standardzie OPML.
 */
public class ImportExport extends DefaultHandler {

    /** Lista kanałów, którą zwraca metoda getChannelsFromFile. */
    private static List<Channel> channels;

    private static ChannelComparator channelComparator = new ChannelComparator();

    public ImportExport() {
        super();
    }

    /**
	 * @return Lista kanałów wyeksportowanych z podanego pliku OPML.
	 * @throws FileNotFoundException jeśli podany plik nie istnieje.
	 * @throws IOException jeśli nie można odczytać pliku.
	 * @throws SAXParseException jeśli parsowanie podanego pliku OPML
	 *         nie powiodło się.
	 * @throws SAXException jeśli wystąpił błąd parsera XML.
	 */
    public static List<Channel> getChannelsFromFile(String fileName) throws IOException, SAXException {
        FileReader fr = new FileReader(fileName);
        channels = new ArrayList<Channel>();
        XMLReader xr = XMLReaderFactory.createXMLReader();
        ImportExport handler = new ImportExport();
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);
        xr.parse(new InputSource(fr));
        return channels;
    }

    /**
	 * Zapisuje podane kanały do podanego pliku.
	 *
	 * @throws IOException jeśli zapisanie pliku jest niemożliwe.
	 */
    public static void writeChannelsToFile(List<Channel> channels, String fileName) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
        out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        out.write("<opml version=\"1.0\">\n");
        out.write("<head>\n");
        out.write("<title>Subskrypcje kanałów</title>\n");
        out.write("</head>\n");
        out.write("<body>\n");
        Collections.sort(channels, channelComparator);
        for (Channel channel : channels) {
            if (channel.getTags().size() == 0) {
                out.write("<outline text=\"");
                out.write(channel.getTitle());
                out.write("\" title=\"");
                out.write(channel.getTitle());
                out.write("\" type=\"rss\"\n");
                out.write("    xmlUrl=\"");
                out.write(channel.getChannelURL().replace("&", "&amp;"));
                out.write("\" htmlUrl=\"");
                out.write(channel.getLink());
                out.write("\"/>\n");
            }
        }
        for (String tag : JReader.getTags()) {
            out.write("<outline title=\"" + tag + "\" text=\"" + tag + "\">\n");
            for (Channel channel : channels) {
                if (channel.containsTag(tag)) {
                    out.write("    <outline text=\"");
                    out.write(channel.getTitle());
                    out.write("\" title=\"");
                    out.write(channel.getTitle());
                    out.write("\" type=\"rss\"\n");
                    out.write("        xmlUrl=\"");
                    out.write(channel.getChannelURL().replace("&", "&amp;"));
                    out.write("\" htmlUrl=\"");
                    out.write(channel.getLink());
                    out.write("\"/>\n");
                }
            }
            out.write("</outline>\n");
        }
        out.write("</body>\n");
        out.write("</opml>\n");
        out.close();
    }

    /** Nazwa aktualnie parsowanego znacznika. */
    private String currentTag = "";

    private Channel channel;

    /** Nazwa aktualnego taga do oznaczenia kanału. */
    private String channelTag;

    public void startDocument() {
    }

    public void endDocument() {
    }

    /**
	 * Wywoływana kiedy parser natrafia na początek znacznika.
	 */
    public void startElement(String uri, String name, String qName, Attributes atts) {
        if ("".equals(uri)) {
            currentTag = qName;
        } else {
            currentTag = name;
        }
        if (currentTag.equals("outline")) {
            if (atts.getValue("type") != null) {
                if ("rss".equals(atts.getValue("type"))) {
                    if (atts.getValue("xmlUrl") != null) {
                        if (!"".equals(atts.getValue("xmlUrl"))) {
                            channel = new Channel(atts.getValue("xmlUrl"));
                            channel.setTitle(atts.getValue("title"));
                            if (channel.getTitle() == null) {
                                channel.setTitle(atts.getValue("text"));
                            }
                            channel.setLink(atts.getValue("htmlUrl"));
                            int index = indexOf(channel, channels);
                            if (index == -1) {
                                if (channelTag != null) {
                                    channel.setTags(JReader.parseTags(channelTag));
                                }
                                if (channel.getTitle() != null) {
                                    channels.add(channel);
                                }
                            } else {
                                channels.get(index).addTags(JReader.parseTags(channelTag));
                            }
                        }
                    }
                }
            } else if (atts.getValue("xmlUrl") == null) {
                channelTag = atts.getValue("title");
                if (channelTag == null) {
                    channelTag = atts.getValue("text");
                }
            }
        }
    }

    /**
	 * Metoda pomocnicza dla startElement(). Chodzi o to, żeby ignorować tagi
	 * przy porównywaniu kanałów.
	 */
    private int indexOf(Channel channel, List<Channel> channels) {
        int index = -1;
        for (int i = 0; i < channels.size(); i++) {
            if (channel.getChannelURL().equals(channels.get(i).getChannelURL())) {
                return i;
            }
        }
        return index;
    }

    /**
	 * Wywoływana kiedy parser natrafia na koniec znacznika.
	 */
    public void endElement(String uri, String name, String qName) {
    }

    /**
	 * Analiza treści znacznika.
	 */
    public void characters(char ch[], int start, int length) {
    }
}
