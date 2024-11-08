package de.schwarzrot.rec.domain;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import de.schwarzrot.app.errors.ApplicationException;

/**
 * handles info files of vdr recordings. Info files are created by the vdr to store additional info
 * about the recording. Info file is a text-file with the first character of a line is a key
 * followed by a space, followed by the value. Sample:
 * 
 * <pre>
 * C S19.2E-1-1011-11120 arte HD
 * E 31207 1289762100 7500 4E 5
 * T Die Liebenden von Pont-Neuf
 * S Spielfilm F 1991 (Les amants du Pont-Neuf) - Thema: Pariser Impressionen
 * D Der Obdachlose Alex lebt auf der ältesten Brücke von Paris, der Pont-Neuf, und hält sich als Feuerschlucker über Wasser. Dort trifft er eines Tages auf Michèle, die wegen einer Augenerkrankung nicht mehr malen kann und sich völlig aufgibt...
 * G 10 F0
 * X 5 0B deu HD-Video
 * X 2 02 deu zweikanal
 * X 3 12 deu DVB-Untertitel
 * X 2 03 fra französisch
 * X 2 05 deu 
 * X 3 03 fra 
 * V 1289762100
 * F 50
 * P 50
 * L 14
 * </pre>
 * 
 * Supported keys by this class are:
 * <dl>
 * <dt>T</dt>
 * <dd>title of recording</dd>
 * <dt>S</dt>
 * <dd>subtitle of recording</dd>
 * <dt>D</dt>
 * <dd>description of recording</dd>
 * </dl>
 * 
 * @author <a href="mailto:rmantey@users.sourceforge.net">Reinhard Mantey</a>
 */
public class RecInfo {

    public class InfoEntry {

        public String id;

        public String value;

        public InfoEntry(String id, String value) {
            this.id = id;
            this.value = value;
        }
    }

    public static final String TITLE_KEY = "T";

    public static final String SUBTITLE_KEY = "S";

    public static final String DESCRIPTION_KEY = "D";

    private static final Pattern LINE_ENTRY = Pattern.compile("^(.)\\s+(.+)$");

    private Recording parent;

    private List<InfoEntry> entries;

    public RecInfo(Recording parent) {
        this.parent = parent;
    }

    public RecInfo(RecInfo other) {
        this.parent = other.parent;
        if (other.entries != null) {
            entries = new ArrayList<InfoEntry>();
            for (InfoEntry ie : other.entries) {
                entries.add(new InfoEntry(new String(ie.id), new String(ie.value)));
            }
        }
    }

    public void dump() {
        if (entries != null && entries.size() > 0) {
            for (InfoEntry ie : entries) System.out.println("entry <" + ie.id + "> has value: " + ie.value);
        } else {
            System.err.println("Oups, no entries. Did you try a read before dump?");
        }
    }

    /**
     * <pre>
     * C S19.2E-1-1011-11120 arte HD
     * E 31207 1289762100 7500 4E 5
     * T Die Liebenden von Pont-Neuf
     * S Spielfilm F 1991 (Les amants du Pont-Neuf) - Thema: Pariser Impressionen
     * D Der Obdachlose Alex lebt auf der ältesten Brücke von Paris, der Pont-Neuf, und hält sich als Feuerschlucker über Wasser. Dort trifft er eines Tages auf Michèle, die wegen einer Augenerkrankung nicht mehr malen kann und sich völlig aufgibt...
     * G 10 F0
     * X 5 0B deu HD-Video
     * X 2 02 deu zweikanal
     * X 3 12 deu DVB-Untertitel
     * X 2 03 fra französisch
     * X 2 05 deu 
     * X 3 03 fra 
     * V 1289762100
     * F 50
     * P 50
     * L 14
     * </pre>
     */
    protected void createDefault() {
        entries.add(new InfoEntry("C", "unknown"));
        entries.add(new InfoEntry("E", "fake"));
        entries.add(new InfoEntry("T", parent.getSection()));
        entries.add(new InfoEntry("S", parent.getTitle()));
        entries.add(new InfoEntry("D", parent.getDescription()));
        entries.add(new InfoEntry("G", "13 Blah"));
        StringBuilder sb;
        double fps = 0;
        for (Stream s : parent.getStreams()) {
            if (s instanceof VideoStream) {
                VideoStream vs = (VideoStream) s;
                sb = new StringBuilder("5 ");
                fps = vs.getFps();
                sb.append(s.getStreamId());
                sb.append(" ");
                sb.append("deu ");
                sb.append(s.getStreamName());
                entries.add(new InfoEntry("X", sb.toString()));
            }
        }
        for (Stream s : parent.getStreams()) {
            if (s instanceof AudioStream) {
                sb = new StringBuilder("2 ");
                sb.append(s.getStreamId());
                sb.append(" ");
                sb.append(((AudioStream) s).getLanguage());
                sb.append(" ");
                sb.append(s.getInfo());
                entries.add(new InfoEntry("X", sb.toString()));
            }
        }
        entries.add(new InfoEntry("V", new Long(parent.getDtScanned().getTime() / 1000).toString()));
        entries.add(new InfoEntry("F", new Double(fps).toString()));
        entries.add(new InfoEntry("P", "25"));
        entries.add(new InfoEntry("L", "99"));
    }

    public void read() {
        if (parent == null) throw new ApplicationException("Can't live without my parent!");
        File source = getInfoPath();
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader(source));
            entries = new ArrayList<InfoEntry>();
            String line;
            for (line = lnr.readLine(); line != null && !line.isEmpty(); line = lnr.readLine()) {
                Matcher m = LINE_ENTRY.matcher(line);
                if (m.matches()) {
                    entries.add(new InfoEntry(m.group(1), m.group(2)));
                }
            }
        } catch (Exception e) {
            throw new ApplicationException("could not read info file", e);
        } finally {
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (Exception e) {
                }
            }
        }
        if (entries.size() < 1) createDefault();
    }

    public void write() {
        if (parent == null) throw new ApplicationException("Can't live without my parent!");
        if (entries == null || entries.size() < 4) throw new ApplicationException("have no entries to write! Please do a read before write.");
        File dest = getInfoPath();
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(dest);
            for (InfoEntry ie : entries) {
                pw.println(ie.id + " " + ie.value);
            }
        } catch (Exception e) {
            throw new ApplicationException("could not write info file", e);
        } finally {
            if (pw != null) pw.close();
        }
    }

    public final Recording getParent() {
        return parent;
    }

    public final String getTitle() {
        String rv = null;
        InfoEntry ie = findEntry(TITLE_KEY);
        if (ie != null) rv = ie.value;
        return rv;
    }

    public final String setTitle(String title) {
        String rv = null;
        InfoEntry ie = findEntry(TITLE_KEY);
        if (ie != null) {
            rv = ie.value;
            ie.value = title;
        }
        return rv;
    }

    public final String getSubtitle() {
        String rv = null;
        InfoEntry ie = findEntry(SUBTITLE_KEY);
        if (ie != null) rv = ie.value;
        return rv;
    }

    public final String setSubtitle(String subtitle) {
        String rv = null;
        InfoEntry ie = findEntry(SUBTITLE_KEY);
        if (ie != null) {
            rv = ie.value;
            ie.value = subtitle;
        } else {
            ie = entries.get(2);
            if (ie.id.compareTo(TITLE_KEY) == 0) entries.add(3, new InfoEntry(SUBTITLE_KEY, subtitle));
        }
        return rv;
    }

    public final String getDescription() {
        String rv = null;
        InfoEntry ie = findEntry(DESCRIPTION_KEY);
        if (ie != null) rv = ie.value;
        return rv;
    }

    public final String setDescription(String desc) {
        String rv = null;
        InfoEntry ie = findEntry(DESCRIPTION_KEY);
        if (ie != null) {
            rv = ie.value;
            ie.value = desc;
        }
        return rv;
    }

    public final void setParent(Recording parent) {
        this.parent = parent;
    }

    protected InfoEntry findEntry(String key) {
        InfoEntry rv = null;
        if (entries != null) {
            for (InfoEntry ie : entries) {
                if (ie.id.compareTo(key) == 0) {
                    rv = ie;
                    break;
                }
            }
        }
        return rv;
    }

    protected File getInfoPath() {
        if (parent == null) throw new ApplicationException("Can't live without my parent!");
        return new File(parent.getPath(), parent.isPesRecording() ? "info.vdr" : "info");
    }
}
