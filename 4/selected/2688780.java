package tags;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import entagged.audioformats.AudioFile;
import entagged.audioformats.EncodingInfo;
import entagged.audioformats.Tag;

/**
 * EntaggedLibrary it is class that represents the entagged-audiofiles library in our program.
 * As a HachoirLibrary it`s implements interface MediaFile because for TagReader these classes are the same.Also it`s
 * inherits AudioFile because it is AudioFile too.
 *
 * There som problems with getting EncodingInfo from AudioFile thats why there one method in TagReader that are creates new instance of EncodingInfo from AudioFile
 * @author alexog
 */
public class EntaggedLibrary extends AudioFile implements MediaFile {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Cretes new Instance of EntaggedLibrary
     * @param f File
     * @param info entagged.audioformats.EncodingInfo
     */
    public EntaggedLibrary(File f, EncodingInfo info) {
        super(f, info);
    }

    /**
     * Cretes new Instance of EntaggedLibrary
     * @param f File for parsing
     * @param info EncodingInfo
     * @param tag Tag
     */
    public EntaggedLibrary(File f, EncodingInfo info, Tag tag) {
        super(f, info, tag);
    }

    /**
     * Cretes new Instance of EntaggedLibrary
     */
    public EntaggedLibrary(String path, EncodingInfo info) {
        super(path, info);
    }

    /**
     * Cretes new Instance of EntaggedLibrary
     */
    public EntaggedLibrary(String path, EncodingInfo info, Tag tag) {
        super(path, info, tag);
    }

    /**
     * There are no possibilities to get instance of EncodingInfo because it is private there.That`s why we create a new instance of EncodingInfo and fill it`s fields fom AudioFile.
     * @param f AudioFile
     * @return EncodingInfo
     */
    public static EncodingInfo createEncodingInfo(AudioFile f) {
        EncodingInfo info = new EncodingInfo();
        info.setBitrate(f.getBitrate());
        info.setChannelNumber(f.getChannelNumber());
        info.setEncodingType(f.getEncodingType());
        info.setExtraEncodingInfos(f.getExtraEncodingInfos());
        info.setLength(f.getLength());
        info.setPreciseLength(f.getPreciseLength());
        info.setSamplingRate(f.getSamplingRate());
        info.setVbr(f.isVbr());
        return info;
    }

    /**
     *
     * @param tag
     * @return
     */
    public List get(String tag) {
        return this.getTag().get(tag);
    }

    /**
     *
     * @return
     */
    public List getAlbum() {
        return this.getTag().getAlbum();
    }

    public List getArtist() {
        return this.getTag().getArtist();
    }

    public List getComment() {
        return this.getTag().getComment();
    }

    public Iterator getFields() {
        return this.getTag().getFields();
    }

    public String getFirstAlbum() {
        return this.getTag().getFirstAlbum();
    }

    public String getFirstArtist() {
        return this.getTag().getFirstArtist();
    }

    public String getFirstComment() {
        return this.getTag().getFirstComment();
    }

    public String getFirstGenre() {
        return this.getTag().getFirstGenre();
    }

    public String getFirstTitle() {
        return this.getTag().getFirstTitle();
    }

    public String getFirstTrack() {
        return this.getTag().getFirstTrack();
    }

    public String getFirstYear() {
        return this.getTag().getFirstYear();
    }

    public List getGenre() {
        return this.getTag().getGenre();
    }

    public List getTitle() {
        return this.getTag().getTitle();
    }

    public List getTrack() {
        return this.getTag().getTrack();
    }

    public List getYear() {
        return this.getTag().getYear();
    }

    public boolean hasCommonFields() {
        return this.getTag().hasCommonFields();
    }

    public boolean hasField(String s) {
        return this.getTag().hasField(s);
    }

    public boolean isEmpty() {
        return this.getTag().isEmpty();
    }
}
