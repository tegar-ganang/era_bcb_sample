package uk.co.caprica.vlcj.radio.service.bbcstreams;

import org.simpleframework.xml.Element;
import uk.co.caprica.vlcj.radio.model.DirectoryEntry;

/**
 * Implementation of a directory entry.
 */
public class BbcStreamsDirectoryEntry implements DirectoryEntry {

    /**
   * Name of the station.
   */
    @Element
    private String name;

    /**
   * Streaming URL for the audio.
   */
    @Element
    private String url;

    /**
   * Default constructor (required for XML binding).
   */
    public BbcStreamsDirectoryEntry() {
    }

    /**
   * Create a directory entry.
   * 
   * @param name station name
   * @param url listen address
   */
    public BbcStreamsDirectoryEntry(String name, String url) {
        this.name = name;
        this.url = url;
    }

    @Override
    public String getDirectory() {
        return "BBC Streams";
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
        return null;
    }

    @Override
    public String getBitRate() {
        return null;
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
        return null;
    }

    @Override
    public String getNowPlaying() {
        return null;
    }

    @Override
    public int compareTo(DirectoryEntry o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(150);
        sb.append(getClass().getSimpleName()).append('[');
        sb.append("name=").append(name).append(',');
        sb.append("url=").append(url).append(']');
        return sb.toString();
    }
}
