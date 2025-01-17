package uk.co.caprica.vlcj.radio.service.indymedia;

import org.simpleframework.xml.Element;
import uk.co.caprica.vlcj.radio.model.DirectoryEntry;

/**
 * Implementation of a directory entry.
 */
public class IndymediaDirectoryEntry implements DirectoryEntry {

    /**
   * Name of the station.
   */
    @Element(required = false)
    private String name;

    /**
   * Streaming URL for the audio.
   */
    @Element
    private String url;

    /**
   * The (media) type of the server.
   */
    @Element(required = false)
    private String type;

    /**
   * Bit-rate of the stream.
   */
    @Element(required = false)
    private String bitRate;

    /**
   * Genre.
   */
    @Element(required = false)
    private String genre;

    /**
   * Default constructor (required for XML binding).
   */
    public IndymediaDirectoryEntry() {
    }

    /**
   * Create a directory entry.
   * 
   * @param name station name
   * @param url listen address
   * @param type type of media
   * @param bitRate bit-rate of the stream
   * @param genre genre
   */
    public IndymediaDirectoryEntry(String name, String url, String type, String bitRate, String genre) {
        this.name = name;
        this.url = url;
        this.type = type;
        this.bitRate = bitRate;
        this.genre = genre;
    }

    @Override
    public String getDirectory() {
        return "Indymedia";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getBitRate() {
        return bitRate;
    }

    @Override
    public int getChannels() {
        return -1;
    }

    @Override
    public int getSampleRate() {
        return -1;
    }

    @Override
    public String getGenre() {
        return genre;
    }

    @Override
    public String getNowPlaying() {
        return null;
    }

    @Override
    public int compareTo(DirectoryEntry o) {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(150);
        sb.append(getClass().getSimpleName()).append('[');
        sb.append("name=").append(name).append(',');
        sb.append("url=").append(url).append(',');
        sb.append("type=").append(type).append(',');
        sb.append("bitRate=").append(bitRate).append(',');
        sb.append("genre=").append(genre).append(']');
        return sb.toString();
    }
}
