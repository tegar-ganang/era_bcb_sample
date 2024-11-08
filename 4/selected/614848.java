package tags;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import tags.exceptions.NotSupportedFormatException;
import entagged.audioformats.AudioFile;
import entagged.audioformats.EncodingInfo;
import entagged.audioformats.Tag;
import entagged.audioformats.ape.MonkeyFileReader;
import entagged.audioformats.asf.AsfFileReader;
import entagged.audioformats.exceptions.CannotReadException;
import entagged.audioformats.flac.FlacFileReader;
import entagged.audioformats.generic.AudioFileReader;
import entagged.audioformats.mp3.Mp3FileReader;
import entagged.audioformats.mp4.Mp4FileReader;
import entagged.audioformats.mpc.MpcFileReader;
import entagged.audioformats.ogg.OggFileReader;
import entagged.audioformats.wav.WavFileReader;

/**
 *
 * The main class
 * @author alexog
 */
public class TagReader {

    /**
     * Instance of file for parsing
     */
    private MediaFile f;

    /**
     * Creates a new instance of TagReader
     * @param fu File for parsing
     * @throws java.io.IOException
     * @throws FileNotFoundException
     * @throws CannotReadException
     * @throws NotSupportedFormatException
     */
    public TagReader(File fu) throws FileNotFoundException, NotSupportedFormatException, IOException, CannotReadException {
        f = parseType(fu);
    }

    /**
     * Creates a new instance of TagReader
     *
     *
     * @param path String path to the file
     * @throws CannotReadException
     * @throws IOException
     * @throws NotSupportedFormatException
     * @throws FileNotFoundException
     */
    public TagReader(String path) throws CannotReadException, FileNotFoundException, NotSupportedFormatException, IOException {
        f = parseType(new File(path));
    }

    /**
     * getLenghtInMinutes convert Entagged.getLength() to good view
     *
     *
     * @return String
     * @param lenght int
     */
    private static String getLenghtInMinutes(int lenght) {
        int hours = (lenght / 360);
        int min = (lenght / 60);
        int sec = lenght - hours - 60 * min;
        return hours + ":" + (min < 10 ? "0" + min : "" + min) + ":" + (sec < 10 ? "0" + sec : "" + sec);
    }

    /**
     * getFileType "*. <type>"
     * @return String the type name
     * @param f File
     */
    private String getFileType(File f) {
        int point = f.getName().lastIndexOf(".");
        if (point == -1) {
            return "";
        } else return f.getName().substring(point + 1).toLowerCase();
    }

    /**
     * parseType returns specific AudioFile for type
     * @param f File
     * @return MediaFile
     * @throws java.io.IOException
     * @throws CannotReadException
     * @throws NotSupportedFormatException
     */
    private MediaFile parseType(File f) throws IOException, CannotReadException, NotSupportedFormatException {
        AudioFileReader fread;
        String type = getFileType(f);
        if (type.equals("mp3") || type.equals("aac")) {
            fread = new Mp3FileReader();
        } else if (type.equals("mp4")) {
            fread = new Mp4FileReader();
        } else if (type.equals("wav")) {
            fread = new WavFileReader();
        } else if (type.equals("ogg")) {
            fread = new OggFileReader();
        } else if (type.equals("mpc")) {
            fread = new MpcFileReader();
        } else if (type.equals("flac")) {
            fread = new FlacFileReader();
        } else if (type.equals("ape")) {
            fread = new MonkeyFileReader();
        } else if ((type.equals("asf")) || (type.equals("wma"))) {
            fread = new AsfFileReader();
        } else if ((type.equals("aif")) || (type.equals("aiff"))) {
            return new HachoirLibrary(f.getAbsolutePath());
        } else {
            throw new NotSupportedFormatException("Not suported format!!!");
        }
        AudioFile af = fread.read(f);
        return new EntaggedLibrary(af, EntaggedLibrary.createEncodingInfo(af), af.getTag());
    }

    /**
     *
     * @param s
     * @return
     */
    public List get(String s) {
        return f.get(s);
    }

    /**
     *
     * @return
     */
    public List getAlbum() {
        return f.getAlbum();
    }

    /**
     *
     * @return
     */
    public List getArtist() {
        return f.getArtist();
    }

    /**
     *
     * @return
     */
    public List getComment() {
        return f.getComment();
    }

    /**
     *
     * @return
     */
    public Iterator getFields() {
        return f.getFields();
    }

    /**
     *
     * @return
     */
    public String getFirstAlbum() {
        return f.getFirstAlbum();
    }

    /**
     *
     * @return
     */
    public String getFirstArtist() {
        return f.getFirstArtist();
    }

    /**
     *
     * @return
     */
    public String getFirstComment() {
        return f.getFirstComment();
    }

    /**
     *
     * @return
     */
    public String getFirstGenre() {
        return f.getFirstGenre();
    }

    /**
     *
     * @return
     */
    public String getFirstTitle() {
        return f.getFirstTitle();
    }

    /**
     *
     * @return
     */
    public String getFirstTrack() {
        return f.getFirstTrack();
    }

    /**
     *
     * @return
     */
    public String getFirstYear() {
        return f.getFirstYear();
    }

    /**
     *
     * @return
     */
    public List getGenre() {
        return f.getGenre();
    }

    /**
     *
     * @return
     */
    public List getTitle() {
        return f.getTitle();
    }

    /**
     *
     * @return
     */
    public List getTrack() {
        return f.getTrack();
    }

    /**
     *
     * @return
     */
    public List getYear() {
        return f.getYear();
    }

    /**
     *
     * @return
     */
    public boolean hasCommonFields() {
        return f.hasCommonFields();
    }

    /**
     *
     * @param s
     * @return
     */
    public boolean hasField(String s) {
        return f.hasField(s);
    }

    /**
     *
     * @return
     */
    public boolean isEmpty() {
        return f.isEmpty();
    }

    /**
     *
     * @return
     */
    public Tag getTag() {
        return f.getTag();
    }

    /**
     *
     * @return
     */
    public int getBitrate() {
        return f.getBitrate();
    }

    /**
     *
     * @return
     */
    public int getChannelNumber() {
        return f.getChannelNumber();
    }

    /**
     *
     * @return
     */
    public String getEncodingType() {
        return f.getEncodingType();
    }

    /**
     *
     * @return
     */
    public String getExtraEncodingInfos() {
        return f.getExtraEncodingInfos();
    }

    /**
     *
     * @return
     */
    public int getLength() {
        return f.getLength();
    }

    /**
     *
     * @return
     */
    public float getPreciseLength() {
        return f.getPreciseLength();
    }

    /**
     *
     * @return
     */
    public int getSamplingRate() {
        return f.getSamplingRate();
    }
}
