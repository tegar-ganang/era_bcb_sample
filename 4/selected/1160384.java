package net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3;

import java.awt.Dimension;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;
import javax.media.Buffer;
import net.sourceforge.jffmpeg.JMFCodec;
import net.sourceforge.jffmpeg.codecs.utils.BitStream;
import net.sourceforge.jffmpeg.codecs.utils.FFMpegException;
import net.sourceforge.jffmpeg.codecs.audio.mpeg.mp3.data.Table;
import java.io.*;

/**
 * Mp3 Codec
 */
public class MP3 implements Codec, JMFCodec {

    public static final boolean debug = false;

    public static final int MPA_STEREO = 0;

    public static final int MPA_JSTEREO = 1;

    public static final int MPA_DUAL = 2;

    public static final int MPA_MONO = 3;

    public static final int MODE_EXT_I_STEREO = 1;

    public static final int MODE_EXT_MS_STEREO = 2;

    protected BitStream in = new BitStream();

    protected BitStream granuleIn = new BitStream();

    /**
     * Expected header
     */
    protected int headerMask = 0;

    protected int streamHeader = 0;

    /**
     * MP3 Packet header
     */
    protected boolean gotHeader = false;

    protected boolean lsf;

    protected int layer;

    protected boolean mpeg25;

    protected boolean error_protection;

    protected int bitrate_index;

    protected int sample_rate_index;

    protected boolean padding;

    protected boolean extension;

    protected int mode;

    protected int mode_ext;

    protected boolean copyright;

    protected boolean original;

    protected int emphasis;

    protected int frame_size;

    protected int bitRate;

    /**
     * ID3 headers
     */
    public static int ID3 = 0x494433;

    public static int LAME = 0x4c414d45;

    private boolean lameFile;

    /**
     * Internal variables
     */
    protected int nb_channels;

    protected int[][][] mpa_bitrate_tab = Table.getBitrateTable();

    protected int[] mpa_freq_tab = new int[] { 44100, 48000, 32000 };

    protected Granule[][] granules;

    /**
     * IMDCT
     **/
    private int[][][] sb_samples = new int[2][2][18 * Granule.SBLIMIT];

    private int[][] mdct_buffer = new int[2][Granule.SBLIMIT * 18];

    private SoundOutput soundOutput = new SoundOutput();

    /**
     * Search for header
     */
    private final boolean isHeader(int header) {
        return ((header & 0xffe00000) == 0xffe00000) && ((header & (3 << 17)) != 0) && ((header & (15 << 12)) != 15) && ((header & (3 << 10)) != 3);
    }

    public MP3() {
        granules = new Granule[2][2];
        granules[0][0] = new Granule();
        granules[0][1] = new Granule();
        granules[1][0] = new Granule();
        granules[1][1] = new Granule();
    }

    private int decodeHeader() {
        int header = in.getBits(16) << 16 | in.getBits(16);
        int sync = (header >> 21) & 0x7ff;
        if ((header & (1 << 20)) != 0) {
            lsf = (header & (1 << 19)) == 0;
            mpeg25 = false;
        } else {
            lsf = true;
            mpeg25 = true;
        }
        layer = 4 - ((header >> 17) & 3);
        error_protection = ((header >> 16) & 1) == 0;
        bitrate_index = (header >> 12) & 0xf;
        sample_rate_index = (header >> 10) & 3;
        padding = ((header >> 9) & 1) != 0;
        extension = ((header >> 8) & 1) != 0;
        mode = ((header >> 6) & 3);
        mode_ext = ((header >> 4) & 3);
        copyright = ((header >> 3) & 1) != 0;
        original = ((header >> 2) & 1) != 0;
        emphasis = header & 3;
        nb_channels = (mode == MPA_MONO) ? 1 : 2;
        if ((((((0xffe00000 | (3 << 17) | (0xf << 12) | (3 << 10) | (3 << 19))) & currentHeader) != streamHeader) && streamHeader != 0) || (sync != 0x7ff) || (layer == 4) || (bitrate_index == 0xf) || (sample_rate_index == 3)) {
            in.seek((in.getPos() - 24) & ~0x07);
            while (in.availableBits() > 32) {
                if (in.showBits(11) == 0x7ff) {
                    break;
                }
                in.getBits(8);
            }
            return -1;
        }
        if (debug) {
            System.out.println("Decoded header...");
            System.out.println("layer:         " + layer);
            System.out.println("nb_channels:   " + nb_channels);
            System.out.println("lsf:           " + (lsf ? 1 : 0));
        }
        streamHeader = currentHeader & (0xffe00000 | (3 << 17) | (0xf << 12) | (3 << 10) | (3 << 19));
        if (bitrate_index != 0) {
            int sample_rate = mpa_freq_tab[sample_rate_index] >> ((lsf ? 1 : 0) + (mpeg25 ? 1 : 0));
            sample_rate_index += 3 * ((lsf ? 1 : 0) + (mpeg25 ? 1 : 0));
            frame_size = mpa_bitrate_tab[lsf ? 1 : 0][layer - 1][bitrate_index];
            bitRate = frame_size * 1000;
            switch(layer) {
                case 1:
                    {
                        frame_size = (frame_size * 12000) / sample_rate;
                        frame_size = (frame_size + (padding ? 1 : 0)) * 4;
                        break;
                    }
                case 2:
                    {
                        frame_size = (frame_size * 144000) / sample_rate;
                        frame_size += padding ? 1 : 0;
                        break;
                    }
                default:
                case 3:
                    {
                        frame_size = (frame_size * 144000) / (sample_rate << (lsf ? 1 : 0));
                        frame_size += padding ? 1 : 0;
                        break;
                    }
            }
        } else {
            throw new Error("Free format frame size");
        }
        if (debug) System.out.println("Frame size " + frame_size);
        return header;
    }

    private void decodeMP3(Buffer outputBuffer, int frameStart) throws FFMpegException {
        if (error_protection) {
            in.getBits(16);
        }
        int main_data_begin;
        int private_bits;
        int nb_granules;
        if (lsf) {
            main_data_begin = in.getBits(8);
            private_bits = in.getBits((nb_channels == 2) ? 2 : 1);
            nb_granules = 1;
        } else {
            main_data_begin = in.getBits(9);
            private_bits = in.getBits((nb_channels == 2) ? 3 : 5);
            nb_granules = 2;
            for (int channel = 0; channel < nb_channels; channel++) {
                granules[channel][0].setScfsi(0);
                granules[channel][1].setScfsi(in.getBits(4));
            }
        }
        if (debug) {
            System.out.println("main_data_begin " + main_data_begin);
            System.out.println("private_bits    " + private_bits);
        }
        for (int granuleNumber = 0; granuleNumber < nb_granules; granuleNumber++) {
            for (int channel = 0; channel < nb_channels; channel++) {
                granules[channel][granuleNumber].read(in, lsf, mode_ext);
                if (debug) System.out.println(granules[channel][granuleNumber]);
            }
        }
        int seekPos = granuleIn.getPos() + granuleIn.availableBits() - main_data_begin * 8;
        granuleIn.seek(seekPos);
        byte[] newGranuleData = in.getDataArray();
        int startNewData = (in.getPos() + 7) / 8;
        granuleIn.addData(newGranuleData, startNewData, frame_size - (startNewData - frameStart / 8));
        in.seek(frameStart + frame_size * 8);
        if (seekPos < 0) {
            return;
        }
        for (int granuleNumber = 0; granuleNumber < nb_granules; granuleNumber++) {
            for (int channel = 0; channel < nb_channels; channel++) {
                granules[channel][granuleNumber].readScaleFactors(granuleIn, lsf, granules[channel][0], channel, mode_ext);
                if (debug) granules[channel][granuleNumber].dumpScaleFactors();
                if (debug) System.out.println("exponents from scale factors");
                granules[channel][granuleNumber].exponents_from_scale_factors(sample_rate_index);
                if (debug) System.out.println("Decode Huffman");
                granules[channel][granuleNumber].huffman_decode(granuleIn, sample_rate_index);
            }
            if (nb_channels == 2) {
                if (debug) System.out.println("Compute Stereo");
                granules[1][granuleNumber].computeStereo(this, granules[0][granuleNumber]);
            }
            for (int channel = 0; channel < nb_channels; channel++) {
                if (debug) System.out.println("Reorder Block");
                granules[channel][granuleNumber].reorderBlock(this);
                if (debug) System.out.println("Antialias");
                granules[channel][granuleNumber].antialias(this);
                if (debug) granules[channel][granuleNumber].dumpHybrid();
                soundOutput.computeImdct(granules[channel][granuleNumber], sb_samples[channel][granuleNumber], mdct_buffer[channel]);
            }
        }
        byte[] out = (byte[]) outputBuffer.getData();
        int outputPointer = outputBuffer.getLength();
        if (out == null) {
            out = new byte[0];
            outputPointer = 0;
        }
        if (out.length - outputPointer < nb_channels * nb_granules * 18 * 32 * 2) {
            byte[] tmp = out;
            out = new byte[4 * (out.length + nb_channels * nb_granules * 18 * 32)];
            System.arraycopy(tmp, 0, out, 0, outputPointer);
            outputBuffer.setData(out);
        }
        for (int channel = 0; channel < nb_channels; channel++) {
            int samplePointer = outputPointer + channel * 2;
            for (int frameNumber = 0; frameNumber < nb_granules * 18; frameNumber++) {
                soundOutput.synth_filter(channel, nb_channels, out, samplePointer, sb_samples[channel][frameNumber / 18], (frameNumber % 18) * Granule.SBLIMIT);
                samplePointer += 32 * nb_channels * 2;
            }
        }
        outputBuffer.setLength(outputPointer + nb_channels * nb_granules * 18 * 32 * 2);
    }

    /**
     * Codec management
     */
    public Format[] getSupportedInputFormats() {
        if (debug) System.out.println("getSupportedInputFormats");
        return new Format[] { new AudioFormat("mpeglayer3") };
    }

    public Format[] getSupportedOutputFormats(Format format) {
        if (debug) System.out.println("getSupportedOutputFormats");
        return new Format[] { new AudioFormat("LINEAR") };
    }

    private AudioFormat inputFormat;

    public Format setInputFormat(Format format) {
        if (debug) System.out.println("Input Format: " + format);
        inputFormat = (AudioFormat) format;
        return format;
    }

    public Format setOutputFormat(Format format) {
        if (debug) System.out.println("Output Format: " + format);
        return new AudioFormat("LINEAR", inputFormat.getSampleRate(), inputFormat.getSampleSizeInBits() > 0 ? inputFormat.getSampleSizeInBits() : 16, inputFormat.getChannels(), 0, 1);
    }

    private int currentHeader = -1;

    public int process(Buffer input, Buffer output) {
        if ((input.getFlags() & Buffer.FLAG_FLUSH) != 0) reset();
        output.setFlags(input.getFlags());
        output.setTimeStamp(input.getTimeStamp());
        output.setDuration(input.getDuration());
        try {
            byte[] data = (byte[]) input.getData();
            int length = input.getLength();
            in.addData(data, 0, length);
            do {
                if (currentHeader == -1) {
                    if (in.availableBits() < 32) break;
                    currentHeader = decodeHeader();
                    if (currentHeader == -1) continue;
                }
                if (frame_size * 8 < in.availableBits() - 32) {
                    decodeMP3(output, in.getPos() - 32);
                    currentHeader = -1;
                }
            } while (currentHeader == -1 && in.availableBits() >= 128);
        } catch (Exception e) {
            reset();
            e.printStackTrace();
            return BUFFER_PROCESSED_FAILED;
        } catch (Error e) {
            reset();
            e.printStackTrace();
            currentHeader = -1;
        }
        return BUFFER_PROCESSED_OK;
    }

    public void open() {
    }

    public void close() {
    }

    public void reset() {
        sb_samples = new int[2][2][18 * Granule.SBLIMIT];
        mdct_buffer = new int[2][Granule.SBLIMIT * 18];
        soundOutput = new SoundOutput();
        in = new BitStream();
        granuleIn = new BitStream();
        granules[0][0] = new Granule();
        granules[0][1] = new Granule();
        granules[1][0] = new Granule();
        granules[1][1] = new Granule();
    }

    public String getName() {
        return "mpeglayer3";
    }

    public Object[] getControls() {
        return new Object[0];
    }

    public Object getControl(String type) {
        return null;
    }

    /**
     * Implement the Jffmpeg codec interface
     */
    public boolean isCodecAvailable() {
        return true;
    }

    /**
     * Outofbands video size
     */
    public void setVideoSize(Dimension size) {
    }

    public void setEncoding(String encoding) {
    }

    public void setIsRtp(boolean isRtp) {
    }

    public void setIsTruncated(boolean isTruncated) {
    }
}
