package edu.cmu.sphinx.tools.corpus;

/**
 * An AudioDatabaseOld manages the large binary data that contains the audio recordings referenced by a Corpus.  The data
 * includes PCM audio, pitch and energy.  It will also contain any other data (e.g. video and lip position etc.) that
 * may be used to train and test recognizer models.  The data is assumed to indexed by time, and this class provides
 * efficient random access to contiguous blocks of the data.  This is a base class, it does not deal with creating this data,
 * or storing it.  Sub-classes will implement the behavior when data is stored as files or in databases.
 */
public abstract class AudioDatabaseOld {

    String pcmFileName;

    String pitchFileName;

    String energyFileName;

    int bitsPerSample;

    int samplesPerSecond;

    int channelCount;

    int bytesPerMillisecond;

    public AudioDatabaseOld() {
    }

    void init() {
        bytesPerMillisecond = (bitsPerSample / 8) * channelCount * samplesPerSecond / 1000;
    }

    private int time2PcmOffet(int time) {
        return time * bytesPerMillisecond;
    }

    private int time2AsciiDoubleLine(int time) {
        return time;
    }

    public String getPcmFileName() {
        return pcmFileName;
    }

    public String getPitchFileName() {
        return pitchFileName;
    }

    public String getEnergyFileName() {
        return energyFileName;
    }

    public int getSamplesPerSecond() {
        return samplesPerSecond;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getBytesPerMillisecond() {
        return bytesPerMillisecond;
    }

    public abstract byte[] readPcmAsBytes(int beginTime, int endTime);

    public abstract short[] readPcmAsShorts(int beginTime, int endTime);

    public abstract double[] readPitch(int beginTime, int endTime);

    public abstract double[] readEnergy(int beginTime, int endTime);

    public void setPcmFileName(String pcmFileName) {
        this.pcmFileName = pcmFileName;
    }

    public void setPitchFileName(String pitchFileName) {
        this.pitchFileName = pitchFileName;
    }

    public void setEnergyFileName(String energyFileName) {
        this.energyFileName = energyFileName;
    }

    public void setBitsPerSample(int bitsPerSample) {
        this.bitsPerSample = bitsPerSample;
    }

    public void setSamplesPerSecond(int samplesPerSecond) {
        this.samplesPerSecond = samplesPerSecond;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }
}
