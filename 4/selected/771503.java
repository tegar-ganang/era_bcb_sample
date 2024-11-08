package musicsequencer.designer;

import javax.sound.sampled.*;
import java.io.*;

/**
 * This class stores floating point audio data along with an AudioFormat object.
 * This way the sound data is centralized and all sounds are in a common form.
 *
 * @author Music Sequencer Group
 */
public class AudioData implements Serializable {

    private static final long serialVersionUID = AudioData.class.getName().hashCode();

    private static final float TWO_TO_THE_15TH = 32768.0f;

    private static final float TWO_TO_THE_NEGATIVE_7TH = 1 / 128.0f;

    private static final float TWO_TO_THE_NEGATIVE_15TH = 1 / 32768.0f;

    private static final float SAMPLE_RATE_22KHZ = 22050.0f;

    private static final float SAMPLE_RATE_44KHZ = 44100.0f;

    /** The name of the file this data came from **/
    private String name;

    /** Audio data stored in specified format */
    private float[] data;

    /** Data format for all AudioData objects - CD quality audio */
    public static final transient AudioFormat format = new AudioFormat(SAMPLE_RATE_44KHZ, 16, 2, true, false);

    /**
     * Extracts audio data from a file and stores it in high quality floating point format.
     *
     * @param file A .wav file to extract audio from. If null, will create blank data.
     */
    public AudioData(File file) {
        if (file == null) {
            data = new float[0];
            name = "Choose Wav";
        } else {
            String fileName = file.getName();
            int extensionIndex = fileName.lastIndexOf(".");
            if (extensionIndex == -1) {
                name = fileName;
            } else {
                name = fileName.substring(0, extensionIndex);
            }
            AudioInputStream inputStream = getAudioInputStream(file);
            AudioFormat inputFormat = inputStream.getFormat();
            byte[] rawData = convertAudioInputStreamToByteArray(inputStream, inputFormat);
            data = convertByteToFloat(rawData, inputFormat);
            if (inputFormat.getChannels() == 1) {
                data = convertMonoToStereo(data);
            }
            if (inputFormat.getSampleRate() == SAMPLE_RATE_22KHZ) {
                data = convert22to44(data);
            } else if (inputFormat.getSampleRate() != SAMPLE_RATE_44KHZ) {
                throw new IllegalArgumentException("Unsupported sample rate: " + inputFormat.getSampleRate());
            }
        }
    }

    private AudioInputStream getAudioInputStream(File file) {
        AudioInputStream inputStream = null;
        try {
            inputStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException ex) {
            System.err.println("Unsupported Audio File");
            ex.printStackTrace(System.err);
        } catch (IOException ex) {
            System.err.println("IOException: " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        return inputStream;
    }

    private byte[] convertAudioInputStreamToByteArray(AudioInputStream inputStream, AudioFormat inputFormat) {
        byte[] rawData = new byte[(int) inputStream.getFrameLength() * inputFormat.getFrameSize()];
        try {
            for (int numBytesRead = 0; numBytesRead < rawData.length; ) {
                numBytesRead += inputStream.read(rawData, numBytesRead, rawData.length - numBytesRead);
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return rawData;
    }

    /**
     * @param length the size of the array desired
     * @return an array of all 0.0f's
     */
    public static float[] getBlankData(int length) {
        float[] output = new float[length];
        for (int i = 0; i < output.length; i++) {
            output[i] = 0.0f;
        }
        return output;
    }

    /**
     * Returns the number of samples stored in data
     * @return Length of the data array
     */
    public int getNumberOfSamples() {
        return data.length;
    }

    /**
     * Takes raw audio byte array and returns float array with one float per sample
     * @param input raw audio bytes
     * @param inputFormat the format of the input raw audio bytes
     * @return Floating point equivalent of the raw data.
     */
    private float[] convertByteToFloat(byte[] input, AudioFormat inputFormat) {
        int sampleCount = input.length / (inputFormat.getSampleSizeInBits() / 8);
        int inputIncrement;
        int bytesPerSample = inputFormat.getSampleSizeInBits() / 8;
        inputIncrement = bytesPerSample;
        if (bytesPerSample > 2) throw new IllegalArgumentException("Unsupported sample size: " + inputFormat.getSampleSizeInBits());
        boolean signed = inputFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
        float[] output = new float[sampleCount];
        for (int inputIndex = 0, outputIndex = 0; outputIndex < sampleCount; outputIndex++) {
            if (bytesPerSample == 1 && signed) {
                output[outputIndex] = ((float) input[inputIndex]) * TWO_TO_THE_NEGATIVE_7TH;
            } else if (bytesPerSample == 1 && !signed) {
                output[outputIndex] = ((float) ((input[inputIndex] & 0xFF) - 128)) * TWO_TO_THE_NEGATIVE_7TH;
            } else if (bytesPerSample == 2 && signed) {
                output[outputIndex] = ((float) ((input[inputIndex + 1] << 8) | (input[inputIndex] & 0xFF))) * TWO_TO_THE_NEGATIVE_15TH;
            } else if (bytesPerSample == 2 && !signed) {
                output[outputIndex] = ((float) ((input[inputIndex] << 8) | (input[inputIndex + 1] & 0xFF))) * TWO_TO_THE_NEGATIVE_15TH;
            } else {
                throw new IllegalArgumentException("You should never get here!");
            }
            inputIndex += inputIncrement;
        }
        return output;
    }

    /**
     * Converts mono data to stereo data by duplicating the mono channel.
     *
     * @param input Mono floating point samples
     * @return Stereo floating point samples
     */
    private float[] convertMonoToStereo(float[] input) {
        float[] output = new float[input.length * 2];
        for (int i = 0; i < input.length; i++) {
            output[2 * i] = input[i];
            output[2 * i + 1] = input[i];
        }
        return output;
    }

    /**
     * Resamples 22kHz data to 44 kHz thorugh interpolation.
     *
     * @param input Stereo 22khZ floating point samples
     * @return Stereo 44khZ floating point samples
     */
    private float[] convert22to44(float[] input) {
        float[] output = new float[input.length * 2];
        int i;
        for (i = 0; i < input.length - 2; i += 2) {
            output[2 * i] = input[i];
            output[2 * i + 1] = input[i + 1];
            output[2 * i + 2] = (input[i] + input[i + 2]) / 2.0f;
            output[2 * i + 3] = (input[i + 1] + input[i + 3]) / 2.0f;
        }
        output[2 * i] = input[i];
        output[2 * i + 1] = input[i + 1];
        output[2 * i + 2] = input[i];
        output[2 * i + 3] = input[i + 1];
        return output;
    }

    /**
     * Converts a floating point sample to a 16-bit integer representation.
     *
     * @param sample Floating point sample intended to be between +- 2^15.
     * @return 16-bit integer equivalent, properly clipped and rounded.
     */
    private static int quantize16(float sample) {
        if (sample >= 32767.0f) {
            return 32767;
        } else if (sample <= -32768.0f) {
            return -32768;
        } else {
            return (int) (sample < 0 ? (sample - 0.5f) : (sample + 0.5f));
        }
    }

    /**
     * Converts from one float per sample to one short per sample in little
     * endian byte order.
     *
     * @param input Floating point data that needs to be converted to raw bytes.
     * @return Raw byte equivalent of input data.
     */
    public static byte[] convertFloatToByte(float[] input) {
        int outputIndex = 0;
        int sampleCount = input.length;
        int intSample;
        byte[] output = new byte[2 * sampleCount];
        for (int inputIndex = 0; inputIndex < sampleCount; inputIndex++) {
            intSample = quantize16(input[inputIndex] * TWO_TO_THE_15TH);
            output[outputIndex + 1] = (byte) (intSample >> 8);
            output[outputIndex] = (byte) (intSample & 0xFF);
            outputIndex += 2;
        }
        return output;
    }

    public String getName() {
        return name;
    }

    /**
     * Adds audio data to the output array from the specified start point.
     *
     * This will continue up the end of the output array or the end of the audio data, whichever comes first.
     *
     * @param start what position to start adding data from
     * @param volume scales the audio data being added
     * @param output the array to add the audio data to
     */
    public void addData(int start, float volume, float[] output) {
        int end = start + output.length - 1;
        for (int i = 0; ((i + start) <= end) && ((i + start) < data.length); i++) {
            output[i] += volume * data[i + start];
        }
    }

    /**
     * Adds audio data to the output array from the specified start point, but reverses the data first.
     *
     * This will continue up the end of the output array or the end of the audio data, whichever comes first.
     *
     * @param start what position to start adding data from - <I> a negative number, referenced from the end of the data array rather than its beginning </I>
     * @param volume scales the audio data being added
     * @param output the array to add the audio data to
     */
    public void addReverseCuedData(int start, float volume, float[] output) {
        int end = start + output.length - 1;
        for (int i = (-data.length >= start) ? (1 - data.length) : start; (i <= 0) && (i <= end); i++) {
            output[i - start] += volume * data[-i];
        }
    }
}
