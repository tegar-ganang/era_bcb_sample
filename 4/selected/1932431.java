package entagged.listing.statistics;

import entagged.audioformats.AudioFile;
import entagged.listing.Lister;

/**
 * This class will create a statistic from multiple audio files. <br>
 * 
 * @author Christian Laireiter (liree)
 */
public class StatisticsCollector implements Lister {

    /**
     * This field stores the statistics.
     */
    private Statistic statistic;

    /**
     * Creates an instance which is ready to recieve audio files.
     * 
     */
    public StatisticsCollector() {
        this.statistic = new Statistic();
    }

    /**
     * (overridden)
     * 
     * @see entagged.listing.Lister#addFile(entagged.audioformats.AudioFile,
     *           java.lang.String)
     */
    public void addFile(AudioFile audioFile, String relativePath) {
        statistic.increaseValidFileCount();
        statistic.addStatisticItem(Statistic.MAP_CODEC, audioFile.getEncodingType());
        statistic.addStatisticItem(Statistic.MAP_SAMPLING, "" + audioFile.getSamplingRate());
        statistic.addStatisticItem(Statistic.MAP_BITRATE, "" + audioFile.getBitrate());
        statistic.addStatisticItem(Statistic.MAP_CHANNELS, "" + audioFile.getChannelNumber());
        statistic.addDuration(audioFile.getLength() * 1000);
        statistic.addFileSize(audioFile.length());
    }

    /**
     * (overridden)
     * 
     * @see entagged.listing.Lister#addFile(java.lang.String)
     */
    public void addFile(String fileName) {
        statistic.increaseInvalidFileCount();
    }

    /**
     * (overridden)
     * 
     * @see entagged.listing.Lister#close()
     */
    public void close() {
    }

    /**
     * (overridden)
     * 
     * @see entagged.listing.Lister#getContent()
     */
    public String getContent() {
        return this.statistic.toString();
    }

    /**
     * Returns the statistic object of this collector. <br>
     * 
     * @return Statistics.
     */
    public Statistic getStatistic() {
        return this.statistic;
    }
}
