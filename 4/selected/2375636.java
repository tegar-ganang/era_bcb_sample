package alesis.fusion.objects;

import alesis.fusion.ByteBufferInputStream;
import alesis.fusion.Constant;
import alesis.fusion.Utility;
import alesis.fusion.chunks.Chunk;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 *
 * @author jam
 */
public class Sample extends FusionObject {

    private SampleHdr header;

    /**
     *
     */
    public MainChunk config;

    /**
     *
     */
    public MappedByteBuffer data;

    private AudioFormat format;

    private class SampleHdr extends Header {

        SampleHdr() {
            this(0);
        }

        SampleHdr(int fileSize) {
            buffer = ByteBuffer.allocate(length);
            setSignature(Constant.SAMPLE_SIGNATURE);
            setSize(fileSize);
        }
    }

    /**
     *
     * @param pathfile
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static Sample createFromFile(String pathfile) throws FileNotFoundException, IOException {
        Sample sample = new Sample("", "", "");
        FileInputStream fis = new FileInputStream(pathfile);
        FileChannel fc = fis.getChannel();
        sample.header.readFromFileChannel(fc);
        sample.config.readFromFileChannel(fc);
        sample.data = fc.map(FileChannel.MapMode.READ_ONLY, 0x30, sample.config.getFramesNumber() * sample.config.getWordLength() * (sample.config.getStereo() == false ? 1 : 2));
        sample.setAudioFormat();
        return sample;
    }

    private void setAudioFormat() {
        format = new AudioFormat(config.getSampleRate(), config.getWordLength() * 8, config.getStereo() ? 2 : 1, true, true);
    }

    private Sample() {
        header = new SampleHdr();
        config = new MainChunk();
    }

    /**
     *
     * @param fileName
     * @param bankName
     * @param volumeName
     */
    public Sample(String fileName, String bankName, String volumeName) {
        this();
        setFileName(fileName);
        setBankName(bankName);
        setVolumeName(volumeName);
        config.setCompression("none");
        config.setSampleRate(Constant.DEFAULT_SAMPLE_RATE);
        config.setWordLength(16);
        config.setStereo(false);
        setAudioFormat();
    }

    /**
     *
     * @param fileName
     * @param bankName
     * @param volumeName
     * @param sampleRate
     * @param wordLength
     * @param stereo
     */
    public Sample(String fileName, String bankName, String volumeName, int sampleRate, int wordLength, boolean stereo) {
        this();
        setFileName(fileName);
        setBankName(bankName);
        setVolumeName(volumeName);
        config.setCompression("none");
        config.setSampleRate(sampleRate);
        config.setWordLength(wordLength);
        config.setStereo(false);
        setAudioFormat();
    }

    /**
     *
     */
    public static enum LoopTypeEnum {

        /**
         * 
         */
        NORMAL, /**
         *
         */
        FORWARD_REVERSE
    }

    /**
     *
     * @param filepath
     * @throws IOException
     */
    public void writeToWav(String filepath) throws IOException {
        AudioSystem.write(this.getAudioStream(), Type.WAVE, new File(filepath));
    }

    /**
     *
     * @return
     */
    public AudioInputStream getAudioStream() {
        return new AudioInputStream(new ByteBufferInputStream(data), format, config.getFramesNumber());
    }

    /**
     *
     */
    public class MainChunk extends Chunk {

        private int length;

        /**
         *
         */
        public MainChunk() {
            length = 0x28;
            buffer = ByteBuffer.allocate(length);
        }

        private void setWordLength(int wordLength) {
            buffer.put(0x4, (byte) (wordLength > 0 ? wordLength : 2));
        }

        /**
         *
         * @return
         */
        public int getWordLength() {
            return buffer.get(0x04);
        }

        private void setStereo(boolean enabled) {
            buffer.put(0x05, (byte) (enabled ? 1 : 0));
        }

        private void setStereo(int enabled) {
            buffer.put(0x05, (byte) (enabled == 0 ? 0 : 1));
        }

        /**
         *
         * @return
         */
        public boolean getStereo() {
            return buffer.get(0x05) == 0 ? false : true;
        }

        /**
         *
         * @param note
         */
        public void setRootNote(int note) {
            buffer.put(0x07, (byte) Utility.adjustRange(note, 0, 127));
        }

        /**
         *
         * @return
         */
        public int getRootNote() {
            return buffer.get(0x07);
        }

        private void setSampleRate(int sampleRate) {
            buffer.putInt(0x08, Utility.swapIntOrder(sampleRate));
        }

        /**
         *
         * @return
         */
        public int getSampleRate() {
            return Utility.swapIntOrder(buffer.getInt(0x08));
        }

        /**
         *
         * @param compressionType
         */
        public void setCompression(String compressionType) {
            System.arraycopy(Utility.swapSignatureString(compressionType).getBytes(), 0, buffer.array(), 0x0c, Constant.SIGNATURE_LENGTH);
        }

        /**
         *
         * @return
         */
        public String getCompression() {
            return Utility.swapSignatureString(new String(buffer.array(), 0x0c, Constant.SIGNATURE_LENGTH));
        }

        /**
         *
         * @param samples
         */
        public void setFramesNumber(int samples) {
            buffer.putInt(0x14, Utility.swapIntOrder(samples));
        }

        /**
         *
         * @return
         */
        public int getFramesNumber() {
            return Utility.swapIntOrder(buffer.getInt(0x14));
        }

        /**
         *
         * @param start
         */
        public void setLoopStart(int start) {
            buffer.putInt(0x18, Utility.swapIntOrder(start));
        }

        /**
         *
         * @return
         */
        public int getLoopStart() {
            return Utility.swapIntOrder(buffer.getInt(0x18));
        }

        /**
         *
         * @param end
         */
        public void setLoopEnd(int end) {
            buffer.putInt(0x1c, Utility.swapIntOrder(end));
        }

        /**
         *
         * @return
         */
        public int getLoopEnd() {
            return Utility.swapIntOrder(buffer.getInt(0x1c));
        }

        /**
         *
         * @param type
         */
        public void setLoopType(LoopTypeEnum type) {
            buffer.put(0x21, (byte) type.ordinal());
        }

        /**
         *
         * @param type
         */
        public void setLoopType(int type) {
            buffer.put(0x21, (byte) Utility.adjustRange(type, 0, LoopTypeEnum.values().length - 1));
        }

        /**
         *
         * @return
         */
        public LoopTypeEnum getLoopType() {
            return LoopTypeEnum.values()[buffer.get(0x21)];
        }
    }
}
