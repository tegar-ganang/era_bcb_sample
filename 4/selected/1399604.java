package edu.cmu.sphinx.tools.riddler.persist.audio;

import javax.persistence.*;

/**
 * Describes an Audio record and holds its data.
 * @see edu.cmu.sphinx.tools.riddler.persist.Corpus
 * @author Garrett Weinberg
 */
@Entity
public abstract class AudioDescriptor {

    protected int samplesPerSecond;

    protected int channelCount;

    protected String filename;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    protected String id;

    protected AudioDescriptor() {
    }

    public AudioDescriptor(int samplesPerSecond, int channelCount, String filename) {
        this.samplesPerSecond = samplesPerSecond;
        this.channelCount = channelCount;
        this.filename = filename;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSamplesPerSecond() {
        return samplesPerSecond;
    }

    public void setSamplesPerSecond(int samplesPerSecond) {
        this.samplesPerSecond = samplesPerSecond;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
