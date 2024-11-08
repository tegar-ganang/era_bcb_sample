package de.maramuse.soundcomp.files;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import de.maramuse.soundcomp.process.NamedSource;
import de.maramuse.soundcomp.process.ParameterMap;
import de.maramuse.soundcomp.process.ProcessElement;
import de.maramuse.soundcomp.process.SourceStore;
import de.maramuse.soundcomp.process.StandardParameters;
import de.maramuse.soundcomp.process.Stateful;
import de.maramuse.soundcomp.process.TypeMismatchException;
import de.maramuse.soundcomp.process.UnknownConnectionException;
import de.maramuse.soundcomp.process.ValueType;
import de.maramuse.soundcomp.process.StandardParameters.Parameter;
import de.maramuse.soundcomp.util.GlobalParameters;
import de.maramuse.soundcomp.util.NotImplementedException;
import de.maramuse.soundcomp.util.ReadOnlyMap;
import de.maramuse.soundcomp.util.ReadOnlyMapImpl;
import de.maramuse.soundcomp.util.NativeObjects;

public class OutputFile implements ProcessElement, Stateful {

    public long nativeSpace = -1;

    private String instanceName;

    private String abstractName;

    private String outputFileName;

    private static final double MIN32 = (-2147483648.0 - 0.4999) / 2147483648.0, MAX32 = (2147483648.0 - 0.5001) / (2147483648.0);

    private static final double MIN24 = (-(1 << 23) - 0.4999) / (1 << 23), MAX24 = ((1 << 23) - 0.5001) / (1 << 23);

    private static final double MIN16 = -32768.499 / 32768, MAX16 = 32767.499 / 32768;

    private static final double SC64 = 9223372036854775808.0;

    private static final double SC32 = 2147483648.0;

    private static final double SC24 = (1 << 23);

    private static final int SC24I = (1 << 23);

    private int format = FileFormats.FMT_WAVE_S16;

    private ReadOnlyMapImpl<Integer, ValueType> srcMap = new ReadOnlyMapImpl<Integer, ValueType>();

    private ReadOnlyMapImpl<Integer, SourceStore> sourceStoreMap = new ReadOnlyMapImpl<Integer, SourceStore>();

    private static ParameterMap inputsMap = new ParameterMap();

    private static ParameterMap outputsMap = new ParameterMap();

    static {
        inputsMap.put(StandardParameters.FACTOR);
        inputsMap.put(StandardParameters.IN);
        outputsMap.put(StandardParameters.OUT);
    }

    private SourceStore[] sources;

    private final int nChannels;

    private final DataList[] samples;

    private int wSampleRate;

    /**
   * Create a wave file output element for a n-channel file.
   * The exact format is specified when writing.
   * @param nChannels the number of channels to be written to the file
   */
    public OutputFile(int nChannels) {
        NativeObjects.registerNativeObject(this);
        this.nChannels = nChannels;
        sources = new SourceStore[nChannels];
        samples = new DataList[nChannels];
        for (int i = 0; i < nChannels; i++) samples[i] = new DataList();
        wSampleRate = (int) GlobalParameters.get().getSampleRate();
    }

    /**
   * This constructor would apply in the hypothetical case that a wave output
   * element is to be created from C++ code. This is not yet possible.
   * @param nChannels	the number of channels to be written to the file
   * @param s			ignored, used for distinguishing constructors
   */
    OutputFile(boolean s, int nChannels) {
        this.nChannels = nChannels;
        samples = new DataList[nChannels];
        for (int i = 0; i < nChannels; i++) samples[i] = new DataList();
        wSampleRate = (int) GlobalParameters.get().getSampleRate();
    }

    public int getNrSamples() {
        int nrSamples = 0;
        for (DataList l : samples) {
            if (l != null && l.size() > nrSamples) nrSamples = l.size();
        }
        return nrSamples;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public int getFormat() {
        return format;
    }

    public void write(OutputStream s) throws IOException {
        switch(format) {
            case FileFormats.FMT_WAVE_DOUBLE:
                writeDoubleWavStream(s);
                break;
            case FileFormats.FMT_WAVE_FLOAT:
                writeFloatWavStream(s);
                break;
            case FileFormats.FMT_WAVE_S16:
                writeWindowsStream(s);
                break;
            case FileFormats.FMT_WAVE_S24:
                write24WindowsStream(s);
                break;
            case FileFormats.FMT_WAVE_S32:
                write32WindowsStream(s);
                break;
            case FileFormats.FMT_MONKEY_8:
                writeApeStream(s, 8);
                break;
            case FileFormats.FMT_MONKEY_16:
                writeApeStream(s, 16);
                break;
            case FileFormats.FMT_MONKEY_24:
                writeApeStream(s, 24);
                break;
            case FileFormats.FMT_WAVE_U8:
                writeByteWavStream(s);
                break;
            case FileFormats.FMT_FLAC_RAW_16:
                writeFlacStream(s, false, 16);
                break;
            case FileFormats.FMT_FLAC_OGG_16:
                writeFlacStream(s, true, 16);
                break;
            case FileFormats.FMT_WAVE_U32:
            case FileFormats.FMT_VORBIS_OGG:
            case FileFormats.FMT_MPC7_16:
            case FileFormats.FMT_MPC8_16:
            case FileFormats.FMT_WAVE_S8:
            case FileFormats.FMT_WAVE_U16:
            case FileFormats.FMT_WAVE_U24:
                throw new NotImplementedException();
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
   * write a monkey's audio file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   * @throws IllegalArgumentException if the number of channels is not 1 or 2.
   */
    private void writeApeStream(OutputStream s, int bits) throws IOException {
        if (nChannels < 1 || nChannels > 2) throw new IllegalArgumentException("Monkey's Audio can only handle 1- or 2-channel audio data");
        double[] ar = samples[0].getArray();
        if (nChannels == 1) nWriteApe(nativeSpace, s, bits, samples[0].size(), ar, null); else nWriteApe(nativeSpace, s, bits, samples[0].size(), ar, samples[1].getArray());
    }

    /**
   * write a flac audio file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   * @throws IllegalArgumentException if the number of channels is not 1 or 2.
   */
    private void writeFlacStream(OutputStream s, boolean ogg, int bits) throws IOException {
        if (nChannels < 1 || nChannels > 2) throw new IllegalArgumentException("Monkey's Audio can only handle 1- or 2-channel audio data");
        double[] ar = samples[0].getArray();
        if (nChannels == 1) nWriteFlac(nativeSpace, s, ogg, bits, samples[0].size(), ar, null); else nWriteFlac(nativeSpace, s, ogg, bits, samples[0].size(), ar, samples[1].getArray());
    }

    /**
   * write a standard windows compliant 16-bit integer wav file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   */
    private void writeWindowsStream(OutputStream s) throws IOException {
        int bytesPerFrame = nChannels * 2;
        int dataLen = bytesPerFrame * samples[0].size();
        int headerLen = 36;
        writeString(s, "RIFF");
        writeInt(s, dataLen + headerLen);
        writeString(s, "WAVE");
        writeString(s, "fmt ");
        writeInt(s, 16);
        writeWord(s, 1);
        writeWord(s, nChannels);
        writeInt(s, this.wSampleRate);
        writeInt(s, bytesPerFrame * wSampleRate);
        writeWord(s, nChannels << 1);
        writeWord(s, 16);
        writeString(s, "data");
        writeInt(s, dataLen);
        for (int i = 0; i < samples[0].size(); i++) {
            for (DataList sample1 : samples) {
                double d = sample1.get(i);
                if (d < MIN16 || d > MAX16) throw new IOException("Attempt to write non-normalized PCM File, value " + d + " at index " + i);
            }
            for (DataList sample : samples) {
                writeWord(s, sample.get(i));
            }
        }
    }

    /**
   * write a windows compliant 24-bit integer wav file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   */
    private void write24WindowsStream(OutputStream s) throws IOException {
        int bytesPerFrame = nChannels * 3;
        boolean pad = false;
        int dataLen = bytesPerFrame * samples[0].size();
        if ((dataLen & 1) != 0) {
            pad = true;
            dataLen++;
        }
        int headerLen = 36;
        writeString(s, "RIFF");
        writeInt(s, dataLen + headerLen);
        writeString(s, "WAVE");
        writeString(s, "fmt ");
        writeInt(s, 16);
        writeWord(s, 1);
        writeWord(s, nChannels);
        writeInt(s, this.wSampleRate);
        writeInt(s, bytesPerFrame * wSampleRate);
        writeWord(s, 3 * nChannels);
        writeWord(s, 24);
        writeString(s, "data");
        writeInt(s, dataLen);
        for (int i = 0; i < samples[0].size(); i++) {
            for (DataList sample1 : samples) {
                double d = sample1.get(i);
                if (d < MIN24 || d > MAX24) throw new IOException("Attempt to write non-normalized PCM File");
            }
            for (DataList sample : samples) {
                writeInt24(s, sample.get(i));
            }
        }
        if (pad) writeByte(s, 0);
    }

    /**
   * write a windows compliant 32-bit integer wav file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   */
    private void write32WindowsStream(OutputStream s) throws IOException {
        int bytesPerFrame = nChannels * 4;
        int dataLen = bytesPerFrame * samples[0].size();
        int headerLen = 36;
        writeString(s, "RIFF");
        writeInt(s, dataLen + headerLen);
        writeString(s, "WAVE");
        writeString(s, "fmt ");
        writeInt(s, 16);
        writeWord(s, 1);
        writeWord(s, nChannels);
        writeInt(s, this.wSampleRate);
        writeInt(s, bytesPerFrame * wSampleRate);
        writeWord(s, bytesPerFrame);
        writeWord(s, 32);
        writeString(s, "data");
        writeInt(s, dataLen);
        for (int i = 0; i < samples[0].size(); i++) {
            for (DataList sample1 : samples) {
                double d = sample1.get(i);
                if (d < MIN32 || d > MAX32) throw new IOException("Attempt to write non-normalized PCM File. Got " + d + ", allowed " + MIN32 + ".." + MAX32);
            }
            for (DataList sample : samples) {
                writeInt(s, sample.get(i));
            }
        }
    }

    /**
   * write a 32-bit float wav file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   */
    private void writeFloatWavStream(OutputStream s) throws IOException {
        int bytesPerFrame = nChannels * 4;
        int dataLen = bytesPerFrame * samples[0].size();
        int headerLen = 46 + 8 + 8 + nChannels * (4 + 4);
        writeString(s, "RIFF");
        writeInt(s, dataLen + headerLen);
        writeString(s, "WAVE");
        writeString(s, "fmt ");
        writeInt(s, 18);
        writeWord(s, 3);
        writeWord(s, nChannels);
        writeInt(s, this.wSampleRate);
        writeInt(s, bytesPerFrame * wSampleRate);
        writeWord(s, nChannels << 2);
        writeWord(s, 32);
        writeWord(s, 0);
        writeString(s, "fact");
        writeInt(s, 4);
        writeInt(s, samples[0].size());
        writeString(s, "PEAK");
        writeInt(s, 8 + 8 * nChannels);
        writeInt(s, 1);
        writeInt(s, 0x4b0ab342);
        for (int i = 0; i < nChannels; i++) {
            int pos = getPeakPos(i);
            writeFloat(s, ((float) samples[i].get(pos)) / 32768f);
            writeInt(s, pos);
        }
        writeString(s, "data");
        writeInt(s, dataLen);
        for (int i = 0; i < samples[0].size(); i++) {
            for (DataList sample1 : samples) {
                double d = sample1.get(i);
                if (d < -Float.MAX_VALUE || d > Float.MAX_VALUE) throw new IOException("Attempt to write non-normalized PCM File");
            }
            for (DataList sample : samples) {
                writeFloat(s, sample.get(i));
            }
        }
    }

    /**
   * write a 64-bit double precision float wav file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   */
    private void writeDoubleWavStream(OutputStream s) throws IOException {
        int bytesPerFrame = nChannels * 8;
        int dataLen = bytesPerFrame * samples[0].size();
        int headerLen = 46 + 8 + 8 + nChannels * (4 + 4);
        writeString(s, "RIFF");
        writeInt(s, dataLen + headerLen);
        writeString(s, "WAVE");
        writeString(s, "fmt ");
        writeInt(s, 18);
        writeWord(s, 3);
        writeWord(s, nChannels);
        writeInt(s, this.wSampleRate);
        writeInt(s, bytesPerFrame * wSampleRate);
        writeWord(s, nChannels << 3);
        writeWord(s, 64);
        writeWord(s, 0);
        writeString(s, "fact");
        writeInt(s, 4);
        writeInt(s, samples[0].size());
        writeString(s, "PEAK");
        writeInt(s, 8 + 8 * nChannels);
        writeInt(s, 1);
        writeInt(s, 0x4b0ab342);
        for (int i = 0; i < nChannels; i++) {
            int pos = getPeakPos(i);
            writeFloat(s, ((float) samples[i].get(pos)) / 32768f);
            writeInt(s, pos);
        }
        writeString(s, "data");
        writeInt(s, dataLen);
        for (int i = 0; i < samples[0].size(); i++) {
            for (DataList sample : samples) {
                writeDouble(s, sample.get(i));
            }
        }
    }

    /**
   * write a 8-bit wav file.
   * @param s				the stream to write to.
   * @throws IOException	if anything is wrong with the stream
   */
    private void writeByteWavStream(OutputStream s) throws IOException {
        int bytesPerFrame = nChannels;
        int dataLen = bytesPerFrame * samples[0].size();
        int headerLen = 36;
        writeString(s, "RIFF");
        writeInt(s, dataLen + headerLen);
        writeString(s, "WAVE");
        writeString(s, "fmt ");
        writeInt(s, 16);
        writeWord(s, 1);
        writeWord(s, nChannels);
        writeInt(s, this.wSampleRate);
        writeInt(s, bytesPerFrame * wSampleRate);
        writeWord(s, nChannels);
        writeWord(s, 8);
        writeString(s, "data");
        writeInt(s, dataLen);
        for (int i = 0; i < samples[0].size(); i++) {
            for (DataList sample : samples) {
                writeByte(s, sample.get(i));
            }
        }
    }

    /**
   * Writes a String to the output stream, without adding a terminating zero byte 
   * @param s	the stream to write to
   * @param ss	the string value to write
   * @throws IOException
   */
    private static void writeString(OutputStream s, String ss) throws IOException {
        byte[] ls = new byte[ss.length()];
        int i = 0;
        for (char c : ss.toCharArray()) {
            ls[i++] = (byte) c;
        }
        s.write(ls);
    }

    /**
   * writes a double value as 32 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeInt(OutputStream s, double d) throws IOException {
        int i = (int) (d * SC32);
        if (d > SC32 - 1) i = 2147483647;
        if (d < -SC32) i = -2147483648;
        writeInt(s, i);
    }

    /**
   * writes an integer value as 32 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeInt(OutputStream s, int i) throws IOException {
        s.write((byte) (i & 255));
        s.write((byte) ((i >> 8) & 255));
        s.write((byte) ((i >> 16) & 255));
        s.write((byte) ((i >> 24) & 255));
    }

    /**
   * writes a double value as 24 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeInt24(OutputStream s, double d) throws IOException {
        int i = (int) (d * SC24);
        if (i > SC24 - 1) i = SC24I - 1;
        if (i < -SC24) i = -SC24I;
        writeInt24(s, i);
    }

    /**
   * writes an integer value as 24 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeInt24(OutputStream s, int i) throws IOException {
        s.write((byte) (i & 255));
        s.write((byte) ((i >> 8) & 255));
        s.write((byte) ((i >> 16) & 255));
    }

    /**
   * writes a double value as 64 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    @SuppressWarnings("unused")
    private static void writeLong(OutputStream s, double d) throws IOException {
        long l = (long) (d * SC64);
        if (d > SC64 - 1) l = 9223372036854775807L;
        if (d < -SC64) l = -9223372036854775808L;
        writeLong(s, l);
    }

    /**
   * writes a long value as 64 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeLong(OutputStream s, long l) throws IOException {
        s.write((byte) (l & 255));
        s.write((byte) ((l >> 8) & 255));
        s.write((byte) ((l >> 16) & 255));
        s.write((byte) ((l >> 24) & 255));
        s.write((byte) ((l >> 32) & 255));
        s.write((byte) ((l >> 40) & 255));
        s.write((byte) ((l >> 48) & 255));
        s.write((byte) ((l >> 56) & 255));
    }

    /**
   * writes a double value as 16 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeWord(OutputStream s, double _d) throws IOException {
        double d = _d * 32768;
        if (d < -32768) d = -32768; else if (d > 32767) d = 32767;
        writeWord(s, (int) d);
    }

    /**
   * writes an integer value as 16 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeWord(OutputStream s, int i) throws IOException {
        writeByte(s, (byte) (i & 255));
        writeByte(s, (byte) ((i >> 8) & 255));
    }

    /**
   * Writes a double value as 8 bit unsigned integer.
   * For old 8-bit file signal values.
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeByte(OutputStream s, double _d) throws IOException {
        double d = _d * 128;
        if (d < -128) d = -128; else if (d > 127) d = 127;
        writeByte(s, (int) d + 128);
    }

    /**
   * writes an integer value as 8 bit integer
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeByte(OutputStream s, int i) throws IOException {
        s.write((byte) (i & 255));
    }

    /**
   * writes a double value as 32 bit float
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeFloat(OutputStream s, double d) throws IOException {
        Float f;
        if (d < -Float.MAX_VALUE) f = -Float.MAX_VALUE; else if (d > Float.MAX_VALUE) f = Float.MAX_VALUE; else f = (float) d;
        writeInt(s, Float.floatToIntBits(f));
    }

    /**
   * writes a float value as 32 bit float
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeFloat(OutputStream s, float f) throws IOException {
        writeInt(s, Float.floatToIntBits(f));
    }

    /**
   * writes a double value as 64 bit double precision float
   * @param s	the stream to write to
   * @param d	the value to write
   * @throws IOException
   */
    private static void writeDouble(OutputStream s, double d) throws IOException {
        writeLong(s, Double.doubleToLongBits(d));
    }

    /**
   * converts a variable-size byte array to the corresponding int value
   * @param bytes
   * @return
   */
    private static int getInt(byte[] bytes) {
        if (bytes.length == 2) {
            return ((bytes[1] & 255) << 8) | (bytes[0] & 255);
        } else if (bytes.length == 4) {
            return ((((((bytes[3] & 255) << 8) | (bytes[2] & 255)) << 8) | (bytes[1] & 255)) << 8) | (bytes[0] & 255);
        } else return 0;
    }

    public static OutputFile readStream(InputStream s) {
        byte lc[] = new byte[4];
        byte wc[] = new byte[2];
        OutputFile w = null;
        try {
            s.read(lc);
            if (lc[0] != 'R' || lc[1] != 'I' || lc[2] != 'F' || lc[3] != 'F') return null;
            s.read(lc);
            int len = getInt(lc) - 28;
            if (len <= 0) return null;
            s.read(lc);
            if (lc[0] != 'W' || lc[1] != 'A' || lc[2] != 'V' || lc[3] != 'E') return null;
            s.read(lc);
            if (lc[0] != 'f' || lc[1] != 'm' || lc[2] != 't') return null;
            s.read(lc);
            int fmtsize = getInt(lc);
            if (fmtsize < 16) return null;
            s.read(wc);
            int wFormat = getInt(wc);
            if (wFormat != 1) return null;
            s.read(wc);
            int wChannels = getInt(wc);
            w = new OutputFile(wChannels);
            s.read(lc);
            w.wSampleRate = getInt(lc);
            s.read(lc);
            getInt(lc);
            s.read(wc);
            getInt(wc);
            s.read(wc);
            int bitsPerSample = getInt(wc);
            int rlen = 0;
            int l;
            while (rlen < len) {
                l = w.readChunk(s, bitsPerSample);
                if (l <= 0) break;
                rlen += l;
            }
        } catch (IOException ex) {
        }
        return w;
    }

    private int readChunk(InputStream s, int bitsPerSample) {
        byte lc[] = new byte[4];
        int len;
        try {
            s.read(lc);
            boolean isData = (lc[0] == 'd') && (lc[1] == lc[3]) && (lc[2] == 't') && (lc[1] == 'a');
            s.read(lc);
            len = getInt(lc);
            if ((len & 1) != 0) len++;
            if (isData) {
                int bytesPerSample = ((bitsPerSample + 7) >> 3);
                int nFrames = len / nChannels / bytesPerSample;
                int bytesPerFrame = bytesPerSample * nChannels;
                byte[] frame = new byte[bytesPerFrame];
                for (int i = 0; i < nFrames; i++) {
                    s.read(frame);
                    for (int c = 0; c < nChannels; c++) switch(bytesPerSample) {
                        case 1:
                            samples[c].add((double) (frame[c] & 255) - 128);
                            break;
                        case 2:
                            int v = ((frame[1 + c * 2] & 255) << 8) + (frame[c * 2] & 255);
                            if ((v & 32768) != 0) v -= 65536;
                            samples[c].add(v);
                            break;
                        case 3:
                            v = ((frame[2 + c * 3] & 255) << 16) + ((frame[1 + c * 3] & 255) << 8) + (frame[c * 3] & 255);
                            if ((v & 0x800000) != 0) v -= 0x1000000;
                            samples[c].add(v);
                            break;
                        case 4:
                            v = ((frame[3 + c * 4] & 255) << 24) + ((frame[2 + c * 4] & 255) << 16) + ((frame[1 + c * 4] & 255) << 8) + (frame[c * 4] & 255);
                            samples[c].add(Float.intBitsToFloat(v));
                            break;
                        case 8:
                            long ld = (((long) (frame[7 + c * 8] & 255)) << 56) + (((long) (frame[6 + c * 8] & 255)) << 48) + (((long) (frame[5 + c * 8] & 255)) << 40) + (((long) (frame[4 + c * 8] & 255)) << 32) + ((frame[3 + c * 8] & 255) << 24) + ((frame[2 + c * 8] & 255) << 16) + ((frame[1 + c * 8] & 255) << 8) + (frame[c * 8] & 255);
                            double d = Double.longBitsToDouble(ld);
                            samples[c].add(d);
                            break;
                    }
                }
            } else {
                s.skip(len);
            }
        } catch (IOException ex) {
            return -1;
        }
        return len + 8;
    }

    public int getWSampleRate() {
        return wSampleRate;
    }

    public void setWSampleRate(int wSampleRate) {
        this.wSampleRate = wSampleRate;
    }

    public int getNChannels() {
        return nChannels;
    }

    public int getNSamples() {
        return samples[0].size();
    }

    public DataList getChannel(int ch) {
        if (ch < 0 || ch >= nChannels) throw new IllegalArgumentException("Channel " + ch + " does not exist");
        return samples[ch];
    }

    public void setOutputFileName(String name) {
        this.outputFileName = name;
    }

    public String getOutputFileName() {
        return outputFileName;
    }

    private int getPeakPos(int channel) {
        if (channel < 0 || channel > nChannels) return 0;
        DataList dd = samples[channel];
        double d = -Double.MAX_VALUE;
        int ix = 0;
        for (int i = 0; i < dd.size(); i++) {
            double v = dd.get(i);
            if (d < v) {
                d = v;
                ix = i;
            }
            i++;
        }
        return ix;
    }

    @Override
    public ReadOnlyMap<Integer, ValueType> getDestinationTypes() {
        return srcMap;
    }

    @Override
    public void setSource(int connectionIndex, NamedSource source, int sourceIndex) throws UnknownConnectionException, TypeMismatchException {
        if (connectionIndex <= 0 && (-connectionIndex < nChannels)) {
            this.sources[-connectionIndex] = new SourceStore(source, sourceIndex);
        }
        sourceStoreMap.put(connectionIndex, new SourceStore(source, sourceIndex));
    }

    @Override
    public ReadOnlyMap<Integer, ValueType> getSourceTypes() {
        return srcMap;
    }

    @Override
    public double getValue(int index) {
        throw new IllegalStateException("");
    }

    @Override
    public void advanceOutput() {
    }

    @Override
    public void advanceState() {
        for (int i = 0; i < nChannels; i++) {
            if (sources[i] != null) getChannel(i).add(sources[i].getValue()); else getChannel(i).add(0d);
        }
    }

    @Override
    public String getAbstractName() {
        return abstractName;
    }

    @Override
    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void setAbstractName(String abstractName) {
        this.abstractName = abstractName;
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public long getNativeSpace() {
        return nativeSpace;
    }

    @Override
    public ReadOnlyMap<Integer, SourceStore> getSourceMap() {
        return sourceStoreMap;
    }

    /**
   * @see de.maramuse.soundcomp.process.ProcessElement#clone()
   * 
   *      But: this ProcessElement is usually not for use in single events 
   *      Should we throw an Exception on cloning attempt? 
   *      Maybe not, as we might have "voice templates" later on.
   */
    @Override
    public OutputFile clone() {
        OutputFile c = new OutputFile(nChannels);
        c.abstractName = abstractName;
        return c;
    }

    @Override
    public ReadOnlyMap<String, Parameter> outputsByName() {
        return outputsMap;
    }

    @Override
    public ReadOnlyMap<String, Parameter> inputsByName() {
        return inputsMap;
    }

    private native void nWriteApe(long _nativeSpace, OutputStream s, int bits, int _samples, double[] left, double[] right);

    private native void nWriteFlac(long _nativeSpace, OutputStream s, boolean ogg, int bits, int _samples, double[] left, double[] right);
}
