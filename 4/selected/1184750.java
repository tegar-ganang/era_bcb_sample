package com.volantis.map.sti.model;

/**
 * Audio element model class.
 */
public class Audio extends Media {

    /**
     * Codec attribute. 
     */
    protected String codec;

    /**
     * Parameters of codec.
     */
    protected Properties codecParams;

    /**
     * Bit rate attribute.
     */
    protected int bitRate;

    /**
     * Sampling rate attribute.
     */
    protected int samplingRate;

    /**
     * Sampling resolution attribute.
     */
    protected int samplingResolution;

    /**
     * Channels attribute.
     */
    protected String channels;

    /**
     * Audio syntheric attribute.
     */
    protected AudioSynthetic synthetic;

    /**
     * Getter for codec attribute.
     * 
     * @return codec attribute.
     */
    public String getCodec() {
        return this.codec;
    }

    /**
     * Setter for codec attribute.
     * 
     * @param codec codec attribute.
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
     * Getter for bit rate.
     * 
     * @return bit rate.
     */
    public int getBitRate() {
        return this.bitRate;
    }

    /**
     * Setter for bit rate.
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
     * Getter for sampling resolution.
     * 
     * @return sampling resolution.
     */
    public int getSamplingResolution() {
        return this.samplingResolution;
    }

    /**
     * Setter for sampling resolution.
     * 
     * @param samplingResolution sampling resolution.
     */
    public void setSamplingResolution(int samplingResolution) {
        this.samplingResolution = samplingResolution;
    }

    /**
     * Getter for channels.
     * 
     * @return channels attribute.
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
     * Getter for audio synthetic attribute.
     * 
     * @return audio synthetic attribute.
     */
    public AudioSynthetic getSynthetic() {
        return this.synthetic;
    }

    /**
     * Setter for audio synthetic attribute.
     * 
     * @param synthetic audio synthetic.
     */
    public void setSynthetic(AudioSynthetic synthetic) {
        this.synthetic = synthetic;
    }
}
