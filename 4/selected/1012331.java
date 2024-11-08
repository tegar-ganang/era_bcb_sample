package de.maramuse.soundcomp.files;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

public class InputFile implements ProcessElement, Stateful {

    public long nativeSpace = -1;

    private String instanceName;

    private String abstractName;

    private ReadOnlyMapImpl<Integer, ValueType> srcMap = new ReadOnlyMapImpl<Integer, ValueType>();

    private static ReadOnlyMapImpl<Integer, ValueType> destMap = new ReadOnlyMapImpl<Integer, ValueType>();

    private ReadOnlyMapImpl<Integer, SourceStore> sourceStoreMap = new ReadOnlyMapImpl<Integer, SourceStore>();

    private SourceStore loopstart, loopend, gate, factor;

    private static ParameterMap inputsMap = new ParameterMap();

    private ParameterMap outputsMap = new ParameterMap();

    static {
        destMap.put(StandardParameters.GATE.i, ValueType.STREAM);
        destMap.put(StandardParameters.FREQUENCY.i, ValueType.STREAM);
        inputsMap.put(StandardParameters.FACTOR);
        inputsMap.put(StandardParameters.IN);
        inputsMap.put(StandardParameters.IN_IMAG);
        inputsMap.put(StandardParameters.GATE);
    }

    private int nChannels;

    private final String name;

    private DataList[] samples;

    private int wSampleRate;

    private int wFileSampleRate;

    private int readIndex = -1;

    private boolean floatFormat;

    private double active = 0.0;

    /**
   * Create a wave file input element for a n-channel InputStream. The exact format should get detected on opening the
   * file.
   * 
   * @param name
   *          the name of the file data is read from
   */
    public InputFile(String name) {
        this.name = name;
        NativeObjects.registerNativeObject(this);
        InputStream stream;
        try {
            stream = new FileInputStream(name);
            detect(stream);
        } catch (FileNotFoundException e) {
            nChannels = 0;
        } catch (Exception e) {
            nChannels = 0;
        }
        wSampleRate = (int) GlobalParameters.get().getSampleRate();
        outputsMap.put(StandardParameters.FREQUENCY);
        srcMap.put(StandardParameters.FREQUENCY.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.GATE);
        srcMap.put(StandardParameters.GATE.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.OUT);
        srcMap.put(StandardParameters.OUT.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.OUT_IMAG);
        srcMap.put(StandardParameters.OUT_IMAG.i, ValueType.STREAM);
        for (int ix = 1; ix <= nChannels; ix++) {
            outputsMap.put(new StandardParameters.Parameter("channel" + ix, -ix));
            srcMap.put(-ix, ValueType.STREAM);
        }
    }

    /**
   * Create a wave file input element for a n-channel InputStream. The exact format should get detected on opening the
   * file.
   * 
   * @param stream
   *          the InputStream that the file data is read from
   */
    public InputFile(InputStream stream) {
        this.name = "";
        NativeObjects.registerNativeObject(this);
        try {
            detect(stream);
        } catch (Exception ex) {
            nChannels = 0;
        }
        samples = new DataList[nChannels];
        for (int i = 0; i < nChannels; i++) samples[i] = new DataList();
        wSampleRate = (int) GlobalParameters.get().getSampleRate();
        outputsMap.put(StandardParameters.FREQUENCY);
        srcMap.put(StandardParameters.FREQUENCY.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.GATE);
        srcMap.put(StandardParameters.GATE.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.OUT);
        srcMap.put(StandardParameters.OUT.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.OUT_IMAG);
        srcMap.put(StandardParameters.OUT_IMAG.i, ValueType.STREAM);
        for (int ix = 1; ix <= nChannels; ix++) {
            outputsMap.put(new StandardParameters.Parameter("channel" + ix, -ix));
            srcMap.put(-ix, ValueType.STREAM);
        }
    }

    /**
   * This constructor will apply in the case that a file input element is to be created from C++ code. This is a yet
   * unsupported scenario, but should be implemented sometime. Setting name is not supported in that case, and the input
   * stream is to be generated from native code then. The number of channels must be determined from native code in
   * advance.
   * 
   * @param nChannels
   *          the number of channels to be written to the file
   * @param s
   *          ignored, used for distinguishing constructors
   */
    InputFile(boolean s, int nChannels) {
        this.name = "";
        this.nChannels = nChannels;
        samples = new DataList[nChannels];
        for (int i = 0; i < nChannels; i++) samples[i] = new DataList();
        wSampleRate = (int) GlobalParameters.get().getSampleRate();
        outputsMap.put(StandardParameters.FREQUENCY);
        srcMap.put(StandardParameters.FREQUENCY.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.GATE);
        srcMap.put(StandardParameters.GATE.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.OUT);
        srcMap.put(StandardParameters.OUT.i, ValueType.STREAM);
        outputsMap.put(StandardParameters.OUT_IMAG);
        srcMap.put(StandardParameters.OUT_IMAG.i, ValueType.STREAM);
        for (int ix = 1; ix <= nChannels; ix++) {
            outputsMap.put(new StandardParameters.Parameter("channel" + ix, -ix));
            srcMap.put(-ix, ValueType.STREAM);
        }
    }

    public int getNrSamples() {
        int nrSamples = 0;
        for (DataList l : samples) {
            if (l != null && l.size() > nrSamples) nrSamples = l.size();
        }
        return nrSamples;
    }

    /**
   * converts a variable-size byte array to the corresponding int value
   * 
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

    /**
   * attempt to find out the format of the stream data, and initialize the InputFile accordingly so that future read
   * operations correctly interpret the sampled data
   * 
   * @param s
   *          the InputStream with header data
   * @return
   * @throws Exception
   */
    public void detect(InputStream s) throws Exception {
        byte lc[] = new byte[4];
        byte wc[] = new byte[2];
        String ename = (name != null) ? name : " an unnamed sample stream";
        try {
            s.read(lc);
            if (lc[0] != 'R' || lc[1] != 'I' || lc[2] != 'F' || lc[3] != 'F') {
                throw new Exception("cannot determine the format of" + ename + "; only RIFF wave files currently supported");
            }
            s.read(lc);
            int len = getInt(lc) - 28;
            if (len <= 0) throw new Exception("stream header of" + ename + " too short or file size info in header corrupt");
            s.read(lc);
            if (lc[0] != 'W' || lc[1] != 'A' || lc[2] != 'V' || lc[3] != 'E') throw new Exception("stream header of RIFF file" + ename + " not WAV compliant: format not 'WAVE', but '" + lc[0] + lc[1] + lc[2] + lc[3] + "'");
            s.read(lc);
            if (lc[0] != 'f' || lc[1] != 'm' || lc[2] != 't') throw new Exception("stream header of RIFF file" + ename + " not WAV compliant: format header not present or doesn't start with 'fmt'");
            s.read(lc);
            int fmtsize = getInt(lc);
            if (fmtsize < 16) throw new Exception("stream header of RIFF file" + ename + " not WAV compliant: size of format header < 16 (is " + fmtsize + ")");
            s.read(wc);
            int wFormat = getInt(wc);
            if (wFormat != 1 && wFormat != 3) throw new Exception("stream header of RIFF file" + ename + " neither 1=PCM nor 3=float, but instead " + wFormat);
            floatFormat = wFormat == 3;
            s.read(wc);
            nChannels = getInt(wc);
            samples = new DataList[nChannels];
            for (int i = 0; i < nChannels; i++) samples[i] = new DataList();
            s.read(lc);
            wFileSampleRate = getInt(lc);
            s.read(lc);
            s.read(wc);
            s.read(wc);
            int bitsPerSample = getInt(wc);
            int headerextensionsize = 0;
            if (fmtsize == 18) headerextensionsize = s.read(wc); else if (fmtsize != 16) throw new Exception("illegal fmt header size in RIFF header, only 16 or 18 supported");
            if (headerextensionsize != 0) {
                if (headerextensionsize != 22) throw new Exception("illegal fmt header extension size " + headerextensionsize + " in RIFF header, only 0 or 22 supported");
                for (int i = 0; i < 11; i++) s.read(wc);
            }
            int rlen = 0;
            int l;
            while (rlen < len) {
                l = readRIFFChunk(s, bitsPerSample);
                if (l <= 0) break;
                rlen += l;
            }
        } catch (IOException ex) {
        }
    }

    private int readRIFFChunk(InputStream s, int bitsPerSample) throws Exception {
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
                switch(bytesPerSample) {
                    case 1:
                        for (int i = 0; i < nFrames; i++) {
                            s.read(frame);
                            for (int c = 0; c < nChannels; c++) {
                                samples[c].add(((frame[c] & 255) - 128) / 128.0);
                            }
                        }
                        break;
                    case 2:
                        final double fact2 = 32768.0;
                        for (int i = 0; i < nFrames; i++) {
                            s.read(frame);
                            for (int c = 0; c < nChannels; c++) {
                                int v = ((frame[1 + c * 2] & 255) << 8) + (frame[c * 2] & 255);
                                if ((v & 32768) != 0) v -= 65536;
                                samples[c].add(v / fact2);
                            }
                        }
                        break;
                    case 3:
                        final double fact3 = 32768.0 * 256.0;
                        for (int i = 0; i < nFrames; i++) {
                            s.read(frame);
                            for (int c = 0; c < nChannels; c++) {
                                int v = ((frame[2 + c * 3] & 255) << 16) + ((frame[1 + c * 3] & 255) << 8) + (frame[c * 3] & 255);
                                if ((v & 0x800000) != 0) v -= 0x1000000;
                                samples[c].add(v / fact3);
                            }
                        }
                        break;
                    case 4:
                        if (floatFormat) {
                            for (int i = 0; i < nFrames; i++) {
                                s.read(frame);
                                for (int c = 0; c < nChannels; c++) {
                                    int v = ((frame[3 + c * 4] & 255) << 24) + ((frame[2 + c * 4] & 255) << 16) + ((frame[1 + c * 4] & 255) << 8) + (frame[c * 4] & 255);
                                    samples[c].add(Float.intBitsToFloat(v));
                                }
                            }
                        } else {
                            final double fact4 = 32768.0 * 65536.0;
                            for (int i = 0; i < nFrames; i++) {
                                s.read(frame);
                                for (int c = 0; c < nChannels; c++) {
                                    long v = ((frame[3 + c * 4] & 255) << 24) + ((frame[2 + c * 4] & 255) << 16) + ((frame[1 + c * 4] & 255) << 8) + (frame[c * 4] & 255);
                                    if ((v & 0x80000000) != 0) v -= 0x100000000L;
                                    samples[c].add(v / fact4);
                                }
                            }
                        }
                        break;
                    case 8:
                        if (floatFormat) {
                            for (int i = 0; i < nFrames; i++) {
                                s.read(frame);
                                for (int c = 0; c < nChannels; c++) {
                                    long ld = (((long) (frame[7 + c * 8] & 255)) << 56) + (((long) (frame[6 + c * 8] & 255)) << 48) + (((long) (frame[5 + c * 8] & 255)) << 40) + (((long) (frame[4 + c * 8] & 255)) << 32) + ((frame[3 + c * 8] & 255) << 24) + ((frame[2 + c * 8] & 255) << 16) + ((frame[1 + c * 8] & 255) << 8) + (frame[c * 8] & 255);
                                    double d = Double.longBitsToDouble(ld);
                                    samples[c].add(d);
                                }
                            }
                        } else {
                            throw new Exception("stream " + name + " contains 64-bit integer sample data, which is not supported");
                        }
                        break;
                    default:
                        throw new Exception("stream " + name + " contains " + bitsPerSample + " bit " + (floatFormat ? "float" : "integer") + " sample data, which is not supported (only 8i,16i,24i,32i,32f,64f allowed)");
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

    @Override
    public ReadOnlyMap<Integer, ValueType> getDestinationTypes() {
        return destMap;
    }

    @Override
    public void setSource(int connectionIndex, NamedSource source, int sourceIndex) throws UnknownConnectionException, TypeMismatchException {
        if (connectionIndex == StandardParameters.IN.i) {
            loopstart = new SourceStore(source, sourceIndex);
        } else if (connectionIndex == StandardParameters.IN_IMAG.i) {
            loopend = new SourceStore(source, sourceIndex);
        } else if (connectionIndex == StandardParameters.GATE.i) {
            gate = new SourceStore(source, sourceIndex);
        } else if (connectionIndex == StandardParameters.FACTOR.i) {
            factor = new SourceStore(source, sourceIndex);
        }
        sourceStoreMap.put(connectionIndex, new SourceStore(source, sourceIndex));
    }

    @Override
    public ReadOnlyMap<Integer, ValueType> getSourceTypes() {
        return srcMap;
    }

    @Override
    public double getValue(int index) {
        int ix = -index;
        if (ix == 0) return getFileSampleRate();
        if (-ix == StandardParameters.GATE.i) return active;
        if (-ix == StandardParameters.OUT.i) ix = 1; else if (-ix == StandardParameters.OUT_IMAG.i) ix = 2;
        if (ix == 2 && nChannels == 1) ix = 1;
        if (ix > nChannels) throw new IllegalArgumentException("cannot read channel " + ix + " of " + nChannels + " in file " + name);
        ix--;
        if (readIndex == -1) return 0;
        if (readIndex < -1 || readIndex >= samples[ix].size()) throw new IllegalArgumentException("attempt to read sample " + readIndex + " of file " + name + " with " + samples[ix].size() + " samples");
        return samples[ix].get(readIndex);
    }

    @Override
    public void advanceOutput() {
    }

    @Override
    public void advanceState() {
        if (readIndex < samples[0].size() - 1) {
            readIndex++;
            active = 1.0;
        } else active = 0.0;
        if (loopstart != null && loopend != null && gate != null) {
            if (gate.getValue() >= 0.5) {
                int le = (int) loopend.getValue();
                if (le >= 0 && readIndex >= le) {
                    int ri = (int) loopstart.getValue();
                    if (ri >= 0) {
                        if (ri < samples[0].size()) readIndex = ri; else readIndex = samples[0].size() - 1;
                    }
                }
            }
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

    public double getFileSampleRate() {
        return wFileSampleRate;
    }

    @Override
    public ReadOnlyMap<Integer, SourceStore> getSourceMap() {
        return sourceStoreMap;
    }

    /**
   * @see de.maramuse.soundcomp.process.ProcessElement#clone()
   * 
   *      But: this ProcessElement is usually not for use in single events Should we throw an Exception on cloning
   *      attempt? Maybe not, as we might have "voice templates" later on.
   */
    @Override
    public InputFile clone() {
        throw new NotImplementedException("InputFile cloning not yet supported");
    }

    @Override
    public ReadOnlyMap<String, Parameter> outputsByName() {
        return outputsMap;
    }

    @Override
    public ReadOnlyMap<String, Parameter> inputsByName() {
        return inputsMap;
    }
}
