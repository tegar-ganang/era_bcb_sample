package jamp;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.format.AudioFormat;
import com.sun.media.BasicCodec;

public class PowerMeter extends BasicCodec {

    public float gain = 2.0F;

    int nPowersPerSec;

    long[] timeStamps;

    float[] powers;

    int startIndex = 0;

    int endIndex = 0;

    boolean enabled = true;

    static final int NUM_SECONDS = 5;

    public String getName() {
        return "PowerMeter";
    }

    public void setEnabled(boolean enable) {
        if (enable != enabled) {
            startIndex = 0;
            endIndex = 0;
        }
        enabled = enable;
    }

    public PowerMeter(int nPowersPerSec) {
        this.nPowersPerSec = nPowersPerSec;
        timeStamps = new long[nPowersPerSec * NUM_SECONDS];
        powers = new float[nPowersPerSec * NUM_SECONDS];
        inputFormats = new Format[] { new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray) };
        outputFormats = new Format[] { new AudioFormat(AudioFormat.LINEAR, Format.NOT_SPECIFIED, 16, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.NOT_SPECIFIED, Format.byteArray) };
    }

    public Format[] getSupportedOutputFormats(Format in) {
        if (in == null || !(in instanceof AudioFormat)) return outputFormats;
        return new Format[] { in };
    }

    public int process(Buffer inputBuffer, Buffer outputBuffer) {
        if (isEOM(inputBuffer)) {
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
        byte[] inBuffer = (byte[]) inputBuffer.getData();
        int inLength = inputBuffer.getLength();
        int inOffset = inputBuffer.getOffset();
        int samplesNumber = inLength;
        AudioFormat af = (AudioFormat) inputBuffer.getFormat();
        if (enabled) {
            int shiftZero = 0;
            int shiftOne = 8;
            if (af.getEndian() == AudioFormat.BIG_ENDIAN) {
                shiftZero = 8;
                shiftOne = 0;
            }
            int spa = ((int) af.getSampleRate() * af.getChannels()) / nPowersPerSec;
            long npa = 1000000000L / nPowersPerSec;
            long timeStamp = inputBuffer.getTimeStamp();
            float average = 0;
            long cspa = 0;
            for (int i = 0; i < inLength; i += 2) {
                short sample = (short) (((0xFF & inBuffer[inOffset + i]) << shiftZero) | ((0xFF & inBuffer[inOffset + i + 1]) << shiftOne));
                float normal = (float) sample;
                average = average + normal * normal;
                cspa++;
                if (cspa == spa) {
                    cspa = 0;
                    average = (float) Math.sqrt((average / spa)) / 32768;
                    push(timeStamp, average);
                    timeStamp += npa;
                    average = 0;
                }
            }
        }
        inputBuffer.setData(outputBuffer.getData());
        outputBuffer.setFormat(af);
        outputBuffer.setData(inBuffer);
        outputBuffer.setLength(inLength);
        outputBuffer.setOffset(inOffset);
        outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
        outputBuffer.setFlags(inputBuffer.getFlags());
        return BUFFER_PROCESSED_OK;
    }

    private synchronized void push(long timeStamp, float power) {
        int nextEnd = (endIndex + 1) % powers.length;
        powers[endIndex] = power;
        timeStamps[endIndex] = timeStamp;
        System.err.println("Pushing " + power + " at " + timeStamp);
        if (nextEnd == startIndex) {
            startIndex = (startIndex + 1) % powers.length;
        }
        endIndex = nextEnd;
    }

    public synchronized float pop(long timeStamp) {
        float returnVal;
        int nextStart;
        System.err.println("looking for " + timeStamp);
        while (true) {
            if (startIndex == endIndex) return 0;
            nextStart = (startIndex + 1) % powers.length;
            if (timeStamps[startIndex] <= timeStamp) {
                if (timeStamps[nextStart] > timeStamp) {
                    returnVal = powers[startIndex];
                    startIndex = nextStart;
                    return returnVal;
                }
            } else return 0f;
            startIndex = nextStart;
        }
    }
}
