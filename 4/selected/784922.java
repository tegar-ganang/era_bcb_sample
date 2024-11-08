package tags;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import entagged.audioformats.Tag;

public class HachoirLibrary implements MediaFile {

    private String name = "";

    public Hashtable hash = new Hashtable();

    int bitrate, length, samplingRate;

    ArrayList album, artist, genre, track, title;

    String firstComment, firstArtist, encodingType;

    boolean isVbr;

    private void testString(String source, String value) {
        if (source.toLowerCase().indexOf("album") == -1) {
            album.add(value);
        } else if (source.toLowerCase().indexOf("author") == -1) {
            artist.add(value);
            firstArtist = value;
        } else if (source.toLowerCase().indexOf("genre") == -1) {
            genre.add(value);
        } else if (source.toLowerCase().indexOf("track") == -1) {
            track.add(value);
        } else if (source.toLowerCase().indexOf("title") == -1) {
            title.add(value);
        } else if (source.toLowerCase().indexOf("comment") == -1) {
            firstComment = value;
        } else if (source.toLowerCase().indexOf("bit rate") == -1) {
            bitrate = extractInt(value);
        } else if (source.toLowerCase().indexOf("format version") == -1) {
            encodingType = value;
        } else if (source.toLowerCase().indexOf("duration") == -1) {
            length = extractLength(value);
        } else if (source.toLowerCase().indexOf("sample rate") == -1) {
            samplingRate = new Double(extractDouble(value)).intValue();
        }
        if (value.toLowerCase().indexOf("vbr") != -1) isVbr = true;
    }

    private String trimLeft(String source) {
        int j = 0;
        String toReturn = source;
        while (j < source.length() && !Character.isDigit(source.charAt(j))) {
            j++;
        }
        return source.substring(j);
    }

    private int extractLength(String source) {
        int dayPos = source.indexOf("day"), hourPos = source.indexOf("hour"), minPos = source.indexOf("min"), secPos = source.indexOf("sec"), value = 0;
        if (dayPos != -1) {
            value += extractInt(source.substring(0, dayPos - 1)) * 24 * 60 * 60;
            source = trimLeft(source.substring(dayPos));
        }
        if (hourPos != -1) {
            value += extractInt(source.substring(0, hourPos - 1)) * 60 * 60;
            source = trimLeft(source.substring(hourPos));
        }
        if (minPos != -1) {
            value += extractInt(source.substring(0, minPos - 1)) * 60;
            source = trimLeft(source.substring(minPos));
        }
        if (secPos != -1) {
            value += extractInt(source.substring(0, secPos - 1));
        }
        return value;
    }

    /**
     *
     * @return
     */
    public int getChannelNumber() {
        return -1;
    }

    /**
     *
     * @return
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     *
     * @return
     */
    public String getExtraEncodingInfos() {
        return "";
    }

    /**
     *
     * @return
     */
    public int getLength() {
        return length;
    }

    /**
     *
     * @return
     */
    public float getPreciseLength() {
        return new Float(length).floatValue();
    }

    /**
     *
     * @return
     */
    public int getSamplingRate() {
        return samplingRate;
    }

    /**
     *
     * @return
     */
    public Tag getTag() {
        return null;
    }

    /**
     *
     * @return
     */
    public boolean isVbr() {
        return isVbr;
    }

    private void putAll() {
        if (name == "") return;
        String s;
        try {
            s = returnAll();
        } catch (IOException ex) {
            return;
        }
        String[] strs;
        do {
            strs = getProperty(s);
            if (strs == null) {
                return;
            }
            testString(strs[0], strs[1]);
            s = strs[2];
        } while (true);
    }

    /**
     *
     * @param s
     * @return
     */
    private double extractDouble(String s) {
        String s2 = "";
        char ch;
        boolean trigger = false;
        for (int i = 0; i < s.length(); i++) {
            ch = s.charAt(i);
            if (Character.isDigit(ch)) {
                s2 += ch;
            } else if (ch == '.' || ch == ',') {
                if (trigger) {
                    break;
                } else {
                    s2 += ch;
                    trigger = true;
                }
            }
        }
        return new Double(s2).doubleValue();
    }

    /**
     *
     * @param s
     * @return
     */
    private int extractInt(String s) {
        return new Double(extractDouble(s)).intValue();
    }

    /**
     *
     * @return
     */
    public int getBitrate() {
        ArrayList arrL = (ArrayList) get("bit rate");
        if (arrL.size() > 0) return extractInt((String) arrL.get(0));
        return -1;
    }

    /**
     *
     * @return
     */
    public List getAlbum() {
        return album;
    }

    /**
     *
     * @return
     */
    public List getArtist() {
        Iterator it = getFields();
        return artist;
    }

    /**
     * Method for iterating Collection with tags and values
     * @return Iterator &lt;Entry&lt;Object,Object&gt;&gt;
     * @author alexog
     */
    public Iterator getFields() {
        return this.hash.entrySet().iterator();
    }

    /**
     *
     * @return
     */
    public String getFirstAlbum() {
        if (album.size() > 0) return (String) album.get(0);
        return "";
    }

    /**
     *
     * @return
     */
    public String getFirstArtist() {
        return firstArtist;
    }

    /**
     *
     * @return
     */
    public String getFirstComment() {
        return firstComment;
    }

    /**
     *
     * @return
     */
    public String getFirstGenre() {
        if (genre.size() > 0) return (String) genre.get(0); else return "";
    }

    /**
     *
     * @return
     */
    public String getFirstTitle() {
        if (title.size() > 0) return (String) title.get(0); else return "";
    }

    /**
     *
     * @return
     */
    public String getFirstTrack() {
        if (track.size() > 0) return (String) track.get(0); else return "";
    }

    /**
     *
     * @return
     */
    public String getFirstYear() {
        return "";
    }

    /**
     *
     * @return
     */
    public List getGenre() {
        return genre;
    }

    /**
     *
     * @return
     */
    public List getTitle() {
        return title;
    }

    /**
     *
     * @return
     */
    public List getTrack() {
        return null;
    }

    /**
     *
     * @return
     */
    public List getYear() {
        return null;
    }

    /**
     *
     * @param toFind
     * @return
     */
    public List get(String toFind) {
        ArrayList arrL = new ArrayList();
        if (name == "") return arrL;
        String s;
        try {
            s = returnAll();
        } catch (IOException ex) {
            return arrL;
        }
        String[] strs;
        do {
            strs = getProperty(s);
            if (strs == null) {
                return arrL;
            }
            if (strs[0].equalsIgnoreCase(toFind)) arrL.add(strs[1]);
            s = strs[2];
        } while (true);
    }

    /**
     *
     * @param source
     * @return
     */
    private String[] getProperty(String source) {
        if ((source == null) || (source == "")) return null;
        int pos2 = source.indexOf('-');
        if (pos2 == -1) return null;
        if (source.charAt(0) != '-') source = source.substring(pos2);
        int pos1 = source.indexOf('\n');
        if (pos1 == -1) pos1 = source.length() - 1;
        int pos0 = source.indexOf(':');
        if (pos0 == -1) {
            return null;
        }
        return new String[] { source.substring(1, pos0).trim(), source.substring(pos0 + 1, pos1).trim(), source.substring(pos1 + 1).trim() };
    }

    /**
     * Putting information into hashtable
     *
     * @throws ProcessingException
     */
    private void putH() throws IOException {
        if (name == "") return;
        String s = returnAll();
        String[] strs;
        do {
            strs = getProperty(s);
            if (strs == null) {
                return;
            }
            hash.put(strs[0], strs[1]);
            s = strs[2];
        } while (true);
    }

    public HachoirLibrary() {
    }

    /**
     * Hachoir constructor
     *
     * @param fileName
     *            media file name
     * @throws ProcessingException
     */
    public HachoirLibrary(String fileName) throws IOException {
        name = fileName;
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == ' ') {
                name = name.substring(0, i) + "\\ " + name.substring(i + 1);
                i++;
            }
        }
        putH();
        putAll();
    }

    /**
     * Get string representation of InputStream
     *
     * @param br
     * @return string representation of InputStream or "" if something wrong
     *         with this InputStream
     */
    private static String getFromBuffer(BufferedReader br) {
        String result = "";
        if (br == null) return "";
        try {
            String cs = br.readLine();
            while (cs != null) {
                result += cs + "\n";
                cs = br.readLine();
            }
        } catch (IOException io) {
            return null;
        }
        return result;
    }

    /**
     * Run specified command
     *
     * @param cmd
     *            command to execute
     * @return BufferedReader for InputStream if success; else throws
     *         ImageProcessingException contains message about error
     * @throws ProcessingException
     */
    private static BufferedReader open(String cmd) throws IOException {
        Runtime r = null;
        Process p = null;
        try {
            r = Runtime.getRuntime();
            p = r.exec(cmd);
            System.out.println(cmd);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
        String err = getFromBuffer(new BufferedReader(new InputStreamReader(p.getErrorStream())));
        System.out.println(err);
        return new BufferedReader(new InputStreamReader(p.getInputStream()));
    }

    /**
     * Get information from output channel of hachoir-matadata
     *
     * @return Output hachoir-metadata data
     * @throws ProcessingException
     */
    public String returnAll() throws IOException {
        return getFromBuffer(open("hachoir-metadata " + name));
    }

    /**
     *
     * @return
     */
    public List getComment() {
        return get("");
    }

    /**
     *
     * @return
     */
    public boolean hasCommonFields() {
        return false;
    }

    /**
     *
     * @param s
     * @return
     */
    public boolean hasField(String s) {
        return get(s).isEmpty();
    }

    /**
     *
     * @return
     */
    public boolean isEmpty() {
        return this.hash.isEmpty();
    }
}
