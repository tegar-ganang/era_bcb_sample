package au.vermilion.samplebank;

import static au.vermilion.Vermilion.logger;
import au.vermilion.fileio.VermilionDocTypes;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.logging.Level;

/**
 * A sample object that stores PCM sample data.
 */
public class SampleAudio extends SampleObject {

    /**
     * Indicates whether the sampled audio is mono, stereo, or whatever.
     */
    public int numChannels = 0;

    /**
     * Stores the length of the sampled audio, in samples per channel.
     */
    public int length = 0;

    /**
     * The base sample rate for the sampled audio, in Hz.
     */
    public int sampleRate = 0;

    /**
     * The floating point sample data, which should be of size numChannels x length.
     */
    public float[][] sampleData;

    /**
     * The relative volume of the sample, with 100 being 'normal'.
     */
    public int volume = 100;

    /**
     * The relative pitch of the sample, with 100 being 'normal'.
     */
    public int pitch = 100;

    /**
     * The panning of the sample, with from -100 to 100.
     */
    public int panning = 0;

    /**
     * The serialisation type for this object is 2003.
     */
    @Override
    public short getType() {
        return VermilionDocTypes.DOCUMENT_TYPE_SAM_AUD;
    }

    /**
     * Returns the current version ID for this codec.
     */
    @Override
    public short getVersion() {
        return 1;
    }

    /**
     * Attempt to interpret the given file as sampled audio, returning true
     * if data is loaded.
     */
    @Override
    public boolean tryLoadFile(String fileName) {
        try {
            File fileIn = new File(fileName);
            try {
                IStreamHandler ash = null;
                if (fileName.toLowerCase().endsWith(".wav")) ash = new WavStreamHandler(fileIn); else if (fileName.toLowerCase().endsWith(".mp3")) ash = new Mp3StreamHandler(fileIn); else if (fileName.toLowerCase().endsWith(".flac")) ash = new FlacStreamHandler(fileIn);
                if (ash == null) return false;
                length = (int) ash.getFrameLength();
                numChannels = ash.getChannels();
                sampleRate = ash.getSampleRate();
                sampleData = new float[numChannels][length];
                ash.loadSample(sampleData);
                return true;
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, "Exception reading sample", ex);
                return false;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception reading sample", ex);
            return false;
        }
    }

    /**
     * Here we create a new instance of this type of SampleObject. This is
     * used for loading, we are not creating a 'blank' sample but an empty one.
     */
    @Override
    public SampleObject newInstance() {
        return new SampleAudio();
    }

    @Override
    public SampleObject blankInstance() {
        SampleAudio ssa = (SampleAudio) newInstance();
        ssa.length = 48000;
        ssa.sampleRate = 48000;
        ssa.numChannels = 1;
        ssa.sampleData = new float[1][48000];
        return ssa;
    }

    /**
     * Allows the sampled audio file to be shown as a String so it can be listed
     * in the sample bank.
     */
    @Override
    public String toString() {
        if (length == 0 || sampleData == null) return "Sampled Audio";
        return "Sampled Audio: " + length * 1000L / sampleRate + " ms, " + numChannels + " channels";
    }

    @Override
    public boolean canStore(Object obj) {
        return (obj instanceof SampleAudio);
    }

    @Override
    public Object readObject(DataInputStream dis, short dataVersion) {
        try {
            SampleAudio sObj = new SampleAudio();
            sObj.numChannels = dis.readInt();
            sObj.length = dis.readInt();
            sObj.panning = dis.readInt();
            sObj.pitch = dis.readInt();
            sObj.sampleRate = dis.readInt();
            sObj.volume = dis.readInt();
            sObj.sampleData = new float[sObj.numChannels][sObj.length];
            for (int x = 0; x < sObj.numChannels; x++) {
                for (int y = 0; y < sObj.length; y++) {
                    sObj.sampleData[x][y] = dis.readFloat();
                }
            }
            return sObj;
        } catch (Exception ex) {
        }
        return null;
    }

    @Override
    public void writeObject(Object obj, DataOutputStream dos) {
        if (!(obj instanceof SampleAudio)) return;
        SampleAudio sObj = (SampleAudio) obj;
        try {
            dos.writeInt(sObj.numChannels);
            dos.writeInt(sObj.length);
            dos.writeInt(sObj.panning);
            dos.writeInt(sObj.pitch);
            dos.writeInt(sObj.sampleRate);
            dos.writeInt(sObj.volume);
            for (int x = 0; x < sObj.numChannels; x++) {
                for (int y = 0; y < sObj.length; y++) {
                    dos.writeFloat(sObj.sampleData[x][y]);
                }
            }
        } catch (Exception ex) {
        }
    }
}
