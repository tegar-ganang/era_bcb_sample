package net.sf.xwav.soundrenderer;

/**
 * 
 * A simple buffer for holding multi-channel sound samples.
 * 
 */
public class SoundBuffer {

    protected int numberOfChannels;

    protected int dataLength;

    protected float[][] data;

    /**
	 * Constructor sets number of channels and buffer length.
	 * 
	 * @param numberOfChannels
	 * @param dataLength
	 */
    public SoundBuffer(int numberOfChannels, int dataLength) {
        this.numberOfChannels = numberOfChannels;
        this.dataLength = dataLength;
        data = new float[numberOfChannels][dataLength];
    }

    /**
	 * Get a reference to the data buffer. This is 2D float array containing the
	 * data for each channel. Access data as:
	 * <p>
	 * getData()[channel][sampleNo]
	 * 
	 * @return the data
	 */
    public float[][] getData() {
        return data;
    }

    /**
	 * Get a reference to the data buffer for a channel. This is float array
	 * containing the data for each channel. Access data as:
	 * <p>
	 * getChannelData(channel)[sampleNo]
	 * 
	 * @param channel
	 * @return the data
	 * @throws BadParameterException
	 */
    public float[] getChannelData(int channel) throws BadParameterException {
        if (channel >= numberOfChannels || channel < 0) throw new BadParameterException("Channel out of range");
        return data[channel];
    }

    /**
	 * @return the numberOfChannels
	 */
    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    /**
	 * @return the dataLength
	 */
    public int getDataLength() {
        return dataLength;
    }
}
