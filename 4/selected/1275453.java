package org.jsresources.apps.ripper;

import java.net.*;
import java.util.*;
import java.io.*;

public class TrackList {

    String test = "<tracklist>\n" + "<track id=\"1\" start=\"0\" length=\"10095\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"2\" start=\"10095\" length=\"22955\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"3\" start=\"33050\" length=\"28770\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"4\" start=\"61820\" length=\"18495\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"5\" start=\"80315\" length=\"19630\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"6\" start=\"99945\" length=\"29130\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"7\" start=\"129075\" length=\"17120\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"8\" start=\"146195\" length=\"13300\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"9\" start=\"159495\" length=\"27875\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"10\" start=\"187370\" length=\"17990\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"11\" start=\"205360\" length=\"25350\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"12\" start=\"230710\" length=\"10675\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"13\" start=\"241385\" length=\"19060\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"14\" start=\"260445\" length=\"19940\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"15\" start=\"280385\" length=\"23010\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "<track id=\"16\" start=\"303395\" length=\"21896\" type=\"0\" copy=\"true\" pre=\"false\" channels=\"2\" />\n" + "</tracklist>\n";

    private Vector<Track> tracks;

    private Listener listener;

    public TrackList() {
        tracks = new Vector<Track>();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public synchronized int getCount() {
        return tracks.size();
    }

    public synchronized long getDurationInMs(int track) {
        return getTrack(track).getDurationInMs();
    }

    public synchronized Track getTrack(int track) {
        return tracks.elementAt(track);
    }

    public synchronized void setInputStream(InputStream is) throws Exception {
        tracks = new Vector<Track>();
        XMLParser parser = new XMLParser(is);
        parser.getStr("<", true);
        parser.getStr("tracklist", true);
        parser.getStr(">", true);
        while (true) {
            parser.mark();
            if (parser.getStr("<", false) && parser.getStr("/", false) && parser.getStr("tracklist", true) && parser.getStr(">", true)) {
                break;
            }
            parser.reset();
            tracks.add(new Track(parser));
        }
        if (listener != null) {
            listener.trackListChanged(this);
        }
    }

    public class Track {

        private int id;

        private long startSector;

        private long sectorLength;

        private int type;

        private boolean copy;

        private boolean preamp;

        private int channels;

        private final int SECTOR_SIZE = 2352;

        Track(XMLParser parser) throws Exception {
            channels = 2;
            parser.getStr("<", true);
            parser.getStr("track", true);
            boolean EOL = false;
            while (!EOL) {
                if (parser.getStr("id", false)) {
                    id = parser.getInt();
                }
                if (parser.getStr("start", false)) {
                    startSector = parser.getLong();
                }
                if (parser.getStr("length", false)) {
                    sectorLength = parser.getLong();
                }
                if (parser.getStr("type", false)) {
                    type = parser.getInt();
                }
                if (parser.getStr("copy", false)) {
                    copy = parser.getBoolean();
                }
                if (parser.getStr("pre", false)) {
                    preamp = parser.getBoolean();
                }
                if (parser.getStr("channels", false)) {
                    channels = parser.getInt();
                }
                if (parser.getStr("/", false)) {
                    parser.getStr(">", true);
                    EOL = true;
                }
            }
        }

        private long bytes2Ms(long bytes) {
            return (long) (bytes / 44100.0f * 500 / channels);
        }

        public long getDurationInMs() {
            return bytes2Ms(SECTOR_SIZE * sectorLength);
        }

        public int getID() {
            return id;
        }
    }

    class XMLParser {

        private String xml;

        private int currPos;

        private int markPos;

        public XMLParser(InputStream is) throws Exception {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read = 0;
            do {
                read = is.read(buffer);
                if (read > 0) {
                    baos.write(buffer, 0, read);
                } else if (read == 0) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ie) {
                    }
                }
            } while (read >= 0);
            xml = new String(baos.toByteArray());
        }

        public void mark() {
            markPos = currPos;
        }

        public void reset() {
            currPos = markPos;
        }

        private void skip() {
            while (currPos < xml.length() && xml.charAt(currPos) <= 32) {
                currPos++;
            }
        }

        public boolean getStr(String str, boolean mustExist) throws Exception {
            String s = xml.substring(currPos, currPos + str.length());
            if (s.equalsIgnoreCase(str)) {
                currPos += str.length();
                skip();
                return true;
            }
            if (mustExist) {
                throw new Exception("Column " + currPos + ": expected string: '" + str + "', got '" + s + "'");
            }
            return false;
        }

        public String getParam() throws Exception {
            getStr("=", true);
            boolean inQuotes = getStr("\"", false);
            int endPos = currPos;
            while ((inQuotes && (xml.charAt(endPos) != '"')) || (!inQuotes && (xml.charAt(endPos) > 32) && (xml.charAt(endPos) != '/'))) {
                endPos++;
            }
            String result;
            if (inQuotes) {
                result = xml.substring(currPos, endPos);
            } else {
                result = xml.substring(currPos, endPos + 1);
            }
            currPos = endPos + 1;
            skip();
            return result;
        }

        public long getLong() throws Exception {
            return Long.parseLong(getParam());
        }

        public int getInt() throws Exception {
            return Integer.parseInt(getParam());
        }

        public boolean getBoolean() throws Exception {
            return getParam().equalsIgnoreCase("true");
        }
    }

    public static interface Listener {

        public void trackListChanged(TrackList trackList);
    }
}
