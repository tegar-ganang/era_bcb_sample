package bg.obs.internal.jnetplayer.player;

import java.io.Serializable;
import java.util.Map;

/**
 * Holds information about a single song from the playlist.
 * 
 * @author mnenov
 * 
 */
@SuppressWarnings("serial")
public final class PlaylistEntry implements Serializable {

    private final String path;

    private long duration;

    private String type;

    private int frames;

    private int frameSize;

    private float bitRate;

    private String album;

    private Integer channels;

    private Integer frequency;

    private String title;

    private String author;

    private long bytesLength;

    /**
     * @param path
     */
    public PlaylistEntry(String path) {
        this.path = path;
    }

    /**
     * @param path
     * @param properties
     */
    public PlaylistEntry(String path, Map<String, Object> properties) {
        this(path);
        setProperties(properties);
    }

    /**
     * @param properties
     */
    public void setProperties(Map<String, Object> properties) {
        if (properties != null) {
            try {
                this.type = (String) (properties.get("audio.type") != null ? properties.get("audio.type") : "Unknown");
                this.title = (String) (properties.get("title") != null ? properties.get("title") : "-");
                this.author = (String) (properties.get("author") != null ? properties.get("author") : "-");
                this.album = (String) (properties.get("album") != null ? properties.get("album") : "-");
                Object songDuration = properties.get("duration");
                if (songDuration != null) {
                    this.duration = songDuration instanceof Long ? (Long) songDuration : Long.parseLong(songDuration.toString());
                }
                Object audioChannels = properties.get("audio.channels");
                if (audioChannels != null) {
                    this.channels = audioChannels instanceof Integer ? (Integer) audioChannels : Integer.parseInt(audioChannels.toString());
                }
                Object audioLengthBytes = properties.get("audio.length.bytes");
                if (audioLengthBytes != null) {
                    this.bytesLength = audioLengthBytes instanceof Integer ? (Integer) audioLengthBytes : Integer.parseInt(audioLengthBytes.toString());
                }
                Object frequencyHz = properties.get("mp3.frequency.hz");
                if (frequencyHz != null) {
                    this.frequency = frequencyHz instanceof Integer ? (Integer) frequencyHz : Integer.parseInt(frequencyHz.toString());
                }
                Object bitrate = properties.get("bitrate");
                if (bitrate != null) {
                    this.bitRate = bitrate instanceof Integer ? (Integer) bitrate : Integer.parseInt(bitrate.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String[] getSongInfoAsStringArray() {
        String[] result = { "" + Math.round(getBitRate() / 1000), "" + getDuration(), "-", "-", "-", "" + getChannels() };
        if (getTitle() != null && getTitle().trim().length() > 0) result[2] = getTitle();
        if (getAlbum() != null && getAlbum().trim().length() > 0) result[3] = getAlbum();
        if (getAuthor() != null && getAuthor().trim().length() > 0) result[4] = getAuthor();
        return result;
    }

    /**
     * @return the bitrate
     */
    public String getType() {
        return type;
    }

    /**
     * @return the duration
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return getFileName(path);
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * @param path
     * @return
     */
    private static String getFileName(String path) {
        String[] songPath = path.split("\\\\");
        return songPath[songPath.length - 1];
    }

    public String toString() {
        return getPath();
    }

    /**
     * @return the album
     */
    public String getAlbum() {
        return album;
    }

    /**
     * @return the author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @return the bitRate
     */
    public float getBitRate() {
        return bitRate;
    }

    /**
     * @return the channels
     */
    public Integer getChannels() {
        return channels;
    }

    /**
     * @return the frames
     */
    public int getFrames() {
        return frames;
    }

    /**
     * @return the frameSize
     */
    public int getFrameSize() {
        return frameSize;
    }

    /**
     * @return the frequency
     */
    public Integer getFrequency() {
        return frequency;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    public int hashCode() {
        return path.hashCode();
    }

    public boolean equals(Object o) {
        return o != null && o.hashCode() == this.hashCode();
    }

    public long getBytesLength() {
        return bytesLength;
    }
}
