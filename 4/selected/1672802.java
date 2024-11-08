package com.volantis.map.sti.model;

/**
 * Video audio element model class.
 */
public class VideoVideoAudio {

    /**
     * Codec attribute.
     */
    protected String codec;

    /**
     * Codec parameters.
     */
    protected Properties codecParams;

    /**
     * Size limit of audio part.
     */
    protected long sizeLimit;

    /**
     * Bit rate.
     */
    protected int bitRate;

    /**
     * Sampling rate.
     */
    protected int samplingRate;

    /**
     * Sampling resolution.
     */
    protected int samplingResolution;

    /**
     * Channels attribute.
     */
    protected String channels;

    /**
     * Transformations for this part.
     */
    protected Transformations transformations;

    /**
     * Getter for codec attribute.
     * 
     * @return codec
     */
    public String getCodec() {
        return this.codec;
    }

    /**
     * Setter for codec attribute.
     * 
     * @param codec codec
     */
    public void setCodec(String codec) {
        this.codec = codec;
    }

    /**
     * Getter for codec parameters.
     * 
     * @return codec parameters.
     */
    public Properties getCodecParams() {
        return this.codecParams;
    }

    /**
     * Setter for codec parameters.
     * 
     * @param codecParams codec parameters.
     */
    public void setCodecParams(Properties codecParams) {
        this.codecParams = codecParams;
    }

    /**
     * Getter for size limit for this part.
     * 
     * @return size limit.
     */
    public long getSizeLimit() {
        return this.sizeLimit;
    }

    /**
     * Setter for size limit for this part.
     * 
     * @param sizeLimit size limit.
     */
    public void setSizeLimit(long sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    /**
     * Getter for bit rate attribute.
     * 
     * @return bit rate.
     */
    public int getBitRate() {
        return this.bitRate;
    }

    /**
     * Setter for bit rate attribute.
     * 
     * @param bitRate bit rate.
     */
    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * Getter for sampling rate attribute.
     * 
     * @return sampling rate.
     */
    public int getSamplingRate() {
        return this.samplingRate;
    }

    /**
     * Setter for sampling rate attribute.
     * 
     * @param samplingRate sampling rate.
     */
    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }

    /**
     * Getter for sampling resolution attribute.
     * 
     * @return sampling resolution.
     */
    public int getSamplingResolution() {
        return this.samplingResolution;
    }

    /**
     * Setter for sampling resolution attribute.
     * 
     * @param samplingResolution sampling resolution.
     */
    public void setSamplingResolution(int samplingResolution) {
        this.samplingResolution = samplingResolution;
    }

    /**
     * Getter for channels attribute.
     * 
     * @return channels.
     */
    public String getChannels() {
        return this.channels;
    }

    /**
     * Setter for channels attribute.
     * 
     * @param channels channels.
     */
    public void setChannels(String channels) {
        this.channels = channels;
    }

    /**
     * Getter for transformations on this part.
     * 
     * @return transformations.
     */
    public Transformations getTransformations() {
        return this.transformations;
    }

    /**
     * Setter for transformations on this part.
     * 
     * @param transformations transformations.
     */
    public void setTransformations(Transformations transformations) {
        this.transformations = transformations;
    }
}
