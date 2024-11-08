package cz.krtinec.telka.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import android.util.Log;
import cz.krtinec.telka.Constants;
import cz.krtinec.telka.dto.Channel;
import cz.krtinec.telka.dto.Programme;

class ProgrammeHandler extends DefaultHandler {

    private static final String PROGRAMME = "programme";

    private static final String CHANNEL = "channel";

    private Map<Channel, List<Programme>> channels;

    /** Programme we are currently reading */
    private Programme currentProgramme;

    /** Channel we are currently reading */
    private Channel currentChannel;

    /** Fake channel for help with Map */
    private Channel processingChannel = new Channel("null");

    private String element;

    private static final DateTimeFormatter FORMAT = DateTimeFormat.forPattern("yyyyMMddHHmmss Z");

    private static final boolean DLS = TimeZone.getDefault().inDaylightTime(new Date());

    public ProgrammeHandler() {
        super();
        Log.i("ProgrammeHandler", "Creating handler...");
        this.channels = new HashMap<Channel, List<Programme>>();
        Log.i("ProgrammeHandler", "Created...");
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        String s = new String(ch, start, length);
        if (s.trim().length() == 0) {
            return;
        }
        if ("title".equals(element)) {
            currentProgramme.title = s;
        } else if ("desc".equals(element)) {
            currentProgramme.description = s;
        } else if ("display-name".equals(element)) {
            currentChannel.displayName = s;
        }
    }

    public void endElement(String uri, String localName, String name) throws SAXException {
        if (localName.equals(PROGRAMME)) {
            channels.get(processingChannel).add(currentProgramme);
        } else if (localName.equals(CHANNEL)) {
            channels.put(currentChannel, new ArrayList<Programme>());
        }
    }

    public void startDocument() throws SAXException {
        Log.d("ProgrammeHandler", "start document");
    }

    public void startElement(String uri, String localName, String name, Attributes atts) throws SAXException {
        element = localName;
        if (localName.equals(PROGRAMME)) {
            currentProgramme = new Programme();
            currentProgramme.start = moveIfDaylightsaving(FORMAT.parseDateTime(atts.getValue("start")).toDate());
            currentProgramme.stop = moveIfDaylightsaving(FORMAT.parseDateTime(atts.getValue("stop")).toDate());
            processingChannel.id = atts.getValue("channel");
        } else if (localName.equals(CHANNEL)) {
            currentChannel = new Channel(atts.getValue("id"));
        } else if (localName.equals("icon")) {
            currentProgramme.iconURL = atts.getValue("src");
        }
    }

    /** Fixes bug in XMLTV */
    private Date moveIfDaylightsaving(Date d) {
        if (DLS) {
            d.setTime(d.getTime() - Constants.HOUR);
        }
        return d;
    }

    public Map<Channel, List<Programme>> getChannels() {
        return channels;
    }
}
