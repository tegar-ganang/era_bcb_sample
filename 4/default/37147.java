import com.cycling74.max.*;
import com.cycling74.msp.*;
import de.crysandt.audio.mpeg7audio.*;
import java.io.*;
import javax.sound.sampled.*;
import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;

public class Mpeg7Encoder extends MaxObject {

    private float version = 0.344f;

    private String fileName;

    private AudioInputStream ais;

    private Config config;

    private Config segmentConfig;

    private Mpeg7 mpeg7;

    private ArrayList<Integer> attacks;

    private ArrayList<Mpeg7> segments;

    private boolean bufferOperation;

    private WorkQueue stack;

    private Atom[] bufferArgs;

    private float bufferSampleRate;

    private String segmentBaseName;

    private int hopSize = 10;

    String path = null;

    boolean getAudioWaveform = false;

    boolean getSegmentAudioWaveform = false;

    boolean getAudioPower = false;

    boolean getSegmentAudioPower = false;

    boolean getAudioSpectrumEnvelope = false;

    boolean getSegmentAudioSpectrumEnvelope = false;

    float aseResolution = 0.25f;

    float segmentAseResolution = 0.25f;

    float aseLoEdge = 62.5f;

    float segmentAseLoEdge = 62.5f;

    float aseHiEdge = 16000f;

    float segmentAseHiEdge = 16000f;

    boolean aseDbScale = false;

    boolean segmentAseDbScale = false;

    String aseNormalize = "off";

    String segmentAseNormalize = "off";

    boolean getAudioSpectrumCentroidSpread = false;

    boolean getSegmentAudioSpectrumCentroidSpread = false;

    boolean getAudioSpectrumFlatness = false;

    boolean getSegmentAudioSpectrumFlatness = false;

    float asfLoEdge = 250f;

    float segmentAsfLoEdge = 250f;

    float asfHiEdge = 16000f;

    float segmentAsfHiEdge = 16000f;

    boolean getAudioSpectrumBasisProjection = false;

    boolean getSegmentAudioSpectrumBasisProjection = false;

    int asbpNumic = 8;

    int segmentAsbpNumic = 8;

    boolean getAudioSignature = false;

    boolean getSegmentAudioSignature = false;

    int asDecimation = 8;

    int segmentAsDecimation = 8;

    boolean getAudioFundamentalFrequency = false;

    boolean getSegmentAudioFundamentalFrequency = false;

    float audioFFLoLimit = 50f;

    float segmentAudioFFLoLimit = 50f;

    float audioFFHiLimit = 12000f;

    float segmentAudioFFHiLimit = 12000f;

    boolean getAudioHarmonicity = false;

    boolean getSegmentAudioHarmonicity = false;

    boolean getHarmonicSpectralCentroid = false;

    boolean getSegmentHarmonicSpectralCentroid = false;

    float harmonicNonHarmonicity = 0.15f;

    float segmentHarmonicNonHarmonicity = 0.15f;

    float harmonicThreshold = 0.1f;

    float segmentHarmonicThreshold = 0.1f;

    boolean getHarmonicSpectralDeviation = false;

    boolean getSegmentHarmonicSpectralDeviation = false;

    boolean getHarmonicSpectralSpread = false;

    boolean getSegmentHarmonicSpectralSpread = false;

    boolean getHarmonicSpectralVariation = false;

    boolean getSegmentHarmonicSpectralVariation = false;

    boolean getTemporalCentroid = false;

    boolean getSegmentTemporalCentroid = false;

    boolean getSpectralCentroid = false;

    boolean getSegmentSpectralCentroid = false;

    boolean getLogAttackTime = false;

    boolean getSegmentLogAttackTime = false;

    float latThreshold = 0.02f;

    float segmentLatThreshold = 0.02f;

    SliceData[] sliceData = null;

    float alphaUp = 0.8f;

    float alphaDown = 0.3f;

    float sliceThreshold = 0f;

    private float _sliceThreshold = 0f;

    float sliceBackgroundThreshold = 0.0004f;

    int sliceExclusionRadius = 8;

    String drawto = null;

    MaxBox lcd;

    float lcdWidth = 100f;

    float lcdHeight = 100f;

    MaxBox mp7db;

    MaxBox mpeg7Config;

    Mpeg7Encoder me = this;

    public Mpeg7Encoder(Atom[] args) {
        declareAttribute("path", null, "setPath");
        declareAttribute("getAudioWaveform");
        declareAttribute("getSegmentAudioWaveform");
        declareAttribute("getAudioPower");
        declareAttribute("getSegmentAudioPower");
        declareAttribute("getAudioSpectrumEnvelope");
        declareAttribute("getSegmentAudioSpectrumEnvelope");
        declareAttribute("aseResolution", null, "setAseResolution");
        declareAttribute("segmentAseResolution", null, "setSegmentAseResolution");
        declareAttribute("aseLoEdge", null, "setAseLoEdge");
        declareAttribute("segmentAseLoEdge", null, "setSegmentAseLoEdge");
        declareAttribute("aseHiEdge", null, "setAseHiEdge");
        declareAttribute("segmentAseHiEdge", null, "setSegmentAseHiEdge");
        declareAttribute("aseDbScale", null, "setAseDbScale");
        declareAttribute("segmentAseDbScale", null, "setSegmentAseDbScale");
        declareAttribute("aseNormalize", "getAseNormalize", "setAseNormalize");
        declareAttribute("segmentAseNormalize", "getSegmentAseNormalize", "setSegmentAseNormalize");
        declareAttribute("getAudioSpectrumCentroidSpread");
        declareAttribute("getSegmentAudioSpectrumCentroidSpread");
        declareAttribute("getAudioSpectrumFlatness");
        declareAttribute("getSegmentAudioSpectrumFlatness");
        declareAttribute("asfLoEdge", null, "setAsfLoEdge");
        declareAttribute("segmentAsfLoEdge", null, "setSegmentAsfLoEdge");
        declareAttribute("asfHiEdge", null, "setAsfHiEdge");
        declareAttribute("segmentAsfHiEdge", null, "setSegmentAsfHiEdge");
        declareAttribute("getAudioSpectrumBasisProjection", null, "setGetAudioSpectrumBasisProjection");
        declareAttribute("getSegmentAudioSpectrumBasisProjection", null, "setGetSegmentAudioSpectrumBasisProjection");
        declareAttribute("asbpNumic", null, "setAsbpNumic");
        declareAttribute("segmentAsbpNumic", null, "setSegmentAsbpNumic");
        declareAttribute("getAudioSignature");
        declareAttribute("getSegmentAudioSignature");
        declareAttribute("asDecimation", null, "setAsDecimation");
        declareAttribute("segmentAsDecimation", null, "setSegmentAsDecimation");
        declareAttribute("getAudioFundamentalFrequency");
        declareAttribute("getSegmentAudioFundamentalFrequency");
        declareAttribute("audioFFLoLimit", null, "setAudioFFLoLimit");
        declareAttribute("segmentAudioFFLoLimit", null, "setSegmentAudioFFLoLimit");
        declareAttribute("audioFFHiLimit", null, "setAudioFFHiLimit");
        declareAttribute("segmentAudioFFHiLimit", null, "setSegmentAudioFFHiLimit");
        declareAttribute("getAudioHarmonicity");
        declareAttribute("getSegmentAudioHarmonicity");
        declareAttribute("getHarmonicSpectralCentroid");
        declareAttribute("getSegmentHarmonicSpectralCentroid");
        declareAttribute("harmonicNonHarmonicity", null, "setHarmonicNonHarmonicity");
        declareAttribute("segmentHarmonicNonHarmonicity", null, "setSegmentHarmonicNonHarmonicity");
        declareAttribute("harmonicThreshold", null, "setHarmonicThreshold");
        declareAttribute("segmentHarmonicThreshold", null, "setSegmentHarmonicThreshold");
        declareAttribute("getHarmonicSpectralDeviation", null, "setGetHarmonicSpectralDeviation");
        declareAttribute("getSegmentHarmonicSpectralDeviation", null, "setGetSegmentHarmonicSpectralDeviation");
        declareAttribute("getHarmonicSpectralSpread", null, "setGetHarmonicSpectralSpread");
        declareAttribute("getSegmentHarmonicSpectralSpread", null, "setGetSegmentHarmonicSpectralSpread");
        declareAttribute("getHarmonicSpectralVariation", null, "setGetHarmonicSpectralVariation");
        declareAttribute("getSegmentHarmonicSpectralVariation", null, "setGetSegmentHarmonicSpectralVariation");
        declareAttribute("getTemporalCentroid");
        declareAttribute("getSegmentTemporalCentroid");
        declareAttribute("getSpectralCentroid");
        declareAttribute("getSegmentSpectralCentroid");
        declareAttribute("getLogAttackTime");
        declareAttribute("getSegmentLogAttackTime");
        declareAttribute("latThreshold", null, "setLatThreshold");
        declareAttribute("segmentLatThreshold", null, "setSegmentLatThreshold");
        declareAttribute("sliceThreshold", "getSliceThreshold", "setSliceThreshold");
        declareAttribute("sliceBackgroundThreshold");
        declareAttribute("sliceExclusionRadius", null, "setSliceExclusionRadius");
        declareAttribute("alphaUp");
        declareAttribute("alphaDown");
        declareAttribute("drawto", null, "setDrawto");
        declareInlets(new int[] { DataTypes.ALL });
        declareOutlets(new int[] { DataTypes.ALL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL, DataTypes.ALL });
        setInletAssist(new String[] { "file <filenpath>, buffer <buffername>, ...." });
        setOutletAssist(new String[] { "<list>SQL data", "<list> paths of saved wavs", "<int> number of segments found", "<list> offset and length of segments in ms", "<bang> Process done." });
        stack = new WorkQueue(1);
        attacks = new ArrayList<Integer>();
        segments = new ArrayList<Mpeg7>();
        mp7db = this.getParentPatcher().getNamedBox("mp7db");
        post("--------------------------------------------");
        post("mxj Mpeg7Encoder, version " + version + "\n");
        post("  LGPL 2009 Robin Price\n");
        post("--------------------------------------------");
    }

    public void notifyDeleted() {
    }

    @SuppressWarnings("unused")
    private void setPath(String p) {
        File pathFolder = new File(p);
        if (pathFolder.exists()) {
            post("MP7 Encoder: path set to " + p + "\n");
            this.path = p;
        } else if (new File("C:" + p).exists()) {
            post("MP7 Encoder: path set to C:" + p + "\n");
            this.path = p;
        } else {
            post("MP7 Encoder Error: invalid path " + p + "\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAseResolution(float f) {
        if (f == 1.0f || f == 0.5f || f == 0.25f || f == 0.125f) {
            aseResolution = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope resolution are 1, 0.5, 0.25 and 0.125\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAseResolution(float f) {
        if (f == 1.0f || f == 0.5f || f == 0.25f || f == 0.125f) {
            segmentAseResolution = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope resolution are 1, 0.5, 0.25 and 0.125\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAseLoEdge(float f) {
        if (f == 31.25f || f == 62.5f || f == 125f || f == 250f || f == 500f) {
            aseLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAseLoEdge(float f) {
        if (f == 31.25f || f == 62.5f || f == 125f || f == 250f || f == 500f) {
            segmentAseLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAseHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            aseHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAseHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            segmentAseHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAseDbScale(boolean b) {
        if (b == true && getAudioSpectrumBasisProjection == true) {
            getAudioSpectrumBasisProjection = false;
        }
        aseDbScale = b;
    }

    @SuppressWarnings("unused")
    private void setSegmentAseDbScale(boolean b) {
        if (b == true && getSegmentAudioSpectrumBasisProjection == true) {
            getSegmentAudioSpectrumBasisProjection = false;
        }
        segmentAseDbScale = b;
    }

    @SuppressWarnings("unused")
    private void setAseNormalize(boolean b) {
        if (b) {
            aseNormalize = "on";
        } else {
            aseNormalize = "off";
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAseNormalize(boolean b) {
        if (b) {
            segmentAseNormalize = "on";
        } else {
            segmentAseNormalize = "off";
        }
    }

    @SuppressWarnings("unused")
    private boolean getAseNormalize() {
        if (aseNormalize == "on") {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private boolean getSegmentAseNormalize() {
        if (segmentAseNormalize == "on") {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private void setAsfLoEdge(float f) {
        if (f == 62.5f || f == 125f || f == 250f || f == 500f) {
            asfLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumFlatness low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAsfLoEdge(float f) {
        if (f == 62.5f || f == 125f || f == 250f || f == 500f) {
            segmentAsfLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumFlatness low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAsfHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            asfHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAsfHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            asfHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    @SuppressWarnings("unused")
    private void setGetAudioSpectrumBasisProjection(boolean b) {
        if (b == true && aseDbScale == true) {
            aseDbScale = false;
        }
        getAudioSpectrumBasisProjection = b;
    }

    @SuppressWarnings("unused")
    private void setSegmentGetAudioSpectrumBasisProjection(boolean b) {
        if (b == true && segmentAseDbScale == true) {
            segmentAseDbScale = false;
        }
        getSegmentAudioSpectrumBasisProjection = b;
    }

    @SuppressWarnings("unused")
    private void setAsbpNumic(int i) {
        if (i >= 1) {
            asbpNumic = i;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumBasisProjection numic are integers greater than zero, try 10\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAsbpNumic(int i) {
        if (i >= 1) {
            segmentAsbpNumic = i;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumBasisProjection numic are integers greater than zero, try 10\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAsDecimation(int i) {
        if (i == 8 || i == 16 || i == 32 || i == 64) {
            asDecimation = i;
        } else {
            post("MP7 Encoder Error: valid values for AudioSignature decimation are 8, 16, 32, 64\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAsDecimation(int i) {
        if (i == 8 || i == 16 || i == 32 || i == 64) {
            segmentAsDecimation = i;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSignature decimation are 8, 16, 32, 64\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAudioFFLoLimit(float f) {
        if (f == 5f || f == 10f || f == 20f || f == 25f || f == 50f) {
            audioFFLoLimit = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioFundamentalFrequency low edge are 5.0, 10.0, 20.0, 25.0 and 50.0\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAudioFFLoLimit(float f) {
        if (f == 5f || f == 10f || f == 20f || f == 25f || f == 50f) {
            segmentAudioFFLoLimit = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioFundamentalFrequency low edge are 5.0, 10.0, 20.0, 25.0 and 50.0\n");
        }
    }

    @SuppressWarnings("unused")
    private void setAudioFFHiLimit(float f) {
        if (f > audioFFLoLimit) {
            audioFFHiLimit = f;
        } else {
            post("MP7 Encoder Error: AudioFundamentalFrequency high edge must be greater than AudioFundamentalFrequency LowEdge");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentAudioFFHiLimit(float f) {
        if (f > segmentAudioFFLoLimit) {
            segmentAudioFFHiLimit = f;
        } else {
            post("MP7 Encoder Error: segment AudioFundamentalFrequency high edge must be greater than AudioFundamentalFrequency LowEdge");
        }
    }

    @SuppressWarnings("unused")
    private void setHarmonicNonHarmonicity(float f) {
        if (f >= 0 && f <= 0.575) {
            harmonicNonHarmonicity = f;
        } else {
            post("MP7 Encoder Error: Harmonic nonHarmonicity must be between 0.0 and 0.575\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentHarmonicNonHarmonicity(float f) {
        if (f >= 0 && f <= 0.575) {
            segmentHarmonicNonHarmonicity = f;
        } else {
            post("MP7 Encoder Error: segment Harmonic nonHarmonicity must be between 0.0 and 0.575\n");
        }
    }

    @SuppressWarnings("unused")
    private void setHarmonicThreshold(float f) {
        if (f >= 0 && f < 1.0) {
            harmonicThreshold = f;
        } else {
            post("MP7 Encoder Error: Harmonic threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentHarmonicThreshold(float f) {
        if (f >= 0 && f < 1.0) {
            harmonicThreshold = f;
        } else {
            post("MP7 Encoder Error: segment Harmonic threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    @SuppressWarnings("unused")
    private void setGetHarmonicSpectralDeviation(boolean b) {
        if (b == true && getHarmonicSpectralCentroid == false) {
            getHarmonicSpectralCentroid = true;
        }
        getHarmonicSpectralDeviation = b;
    }

    @SuppressWarnings("unused")
    private void setGetSegmentHarmonicSpectralDeviation(boolean b) {
        if (b == true && getSegmentHarmonicSpectralCentroid == false) {
            getSegmentHarmonicSpectralCentroid = true;
        }
        getSegmentHarmonicSpectralDeviation = b;
    }

    @SuppressWarnings("unused")
    private void setGetHarmonicSpectralSpread(boolean b) {
        if (b == true && getHarmonicSpectralCentroid == false) {
            getHarmonicSpectralCentroid = true;
        }
        getHarmonicSpectralSpread = b;
    }

    @SuppressWarnings("unused")
    private void setGetSegmentHarmonicSpectralSpread(boolean b) {
        if (b == true && getSegmentHarmonicSpectralCentroid == false) {
            getSegmentHarmonicSpectralCentroid = true;
        }
        getSegmentHarmonicSpectralSpread = b;
    }

    @SuppressWarnings("unused")
    private void setGetHarmonicSpectralVariation(boolean b) {
        if (b == true && getHarmonicSpectralCentroid == false) {
            getHarmonicSpectralCentroid = true;
        }
        getHarmonicSpectralVariation = b;
    }

    @SuppressWarnings("unused")
    private void setGetSegmentHarmonicSpectralVariation(boolean b) {
        if (b == true && getSegmentHarmonicSpectralCentroid == false) {
            getSegmentHarmonicSpectralCentroid = true;
        }
        getSegmentHarmonicSpectralVariation = b;
    }

    @SuppressWarnings("unused")
    private void setLatThreshold(float f) {
        if (f >= 0f && f < 1.0f) {
            latThreshold = f;
        } else {
            post("MP7 Encoder Error: LogAttackTime threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    @SuppressWarnings("unused")
    private void setSegmentLatThreshold(float f) {
        if (f >= 0f && f < 1.0f) {
            segmentLatThreshold = f;
        } else {
            post("MP7 Encoder Error: segment LogAttackTime threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    @SuppressWarnings("unused")
    private float getSliceThreshold() {
        return (sliceThreshold);
    }

    private void setSliceThreshold(float f) {
        if (f >= 0f && f <= 1f) {
            _sliceThreshold = f;
            stack.execute(new SliceThresholdSetter());
        } else {
            post("MP7 Encoder Error: slice threshold must be in range 0.0 to 1.0 inclusive\n");
        }
    }

    private class SliceThresholdSetter implements Runnable {

        public void run() {
            if (sliceData != null) {
                int sliceThresholdInt = Math.round((float) sliceData.length * _sliceThreshold);
                if (sliceData != null) {
                    attacks = new ArrayList<Integer>();
                    boolean[] attacksArray = new boolean[sliceData.length];
                    int checkSpace = 1;
                    if (2 * sliceExclusionRadius > sliceData.length) {
                        post("MP7 Encoder Error: slice exclusion diameter greater than sample length\n");
                        return;
                    } else {
                        checkSpace = sliceExclusionRadius;
                    }
                    for (int j = 0; j < sliceThresholdInt; j++) {
                        int i = sliceData[j].getFrameNumber();
                        boolean alreadyHit = false;
                        if (i >= checkSpace && i <= sliceData.length - 1 - checkSpace) {
                            for (int k = checkSpace * -1; k <= checkSpace; k++) {
                                if (attacksArray[i + k]) {
                                    alreadyHit = true;
                                    break;
                                }
                            }
                            if (!alreadyHit) {
                                attacksArray[i] = true;
                            }
                        } else {
                            if (i - checkSpace < 0) {
                                for (int k = 0; k <= i + checkSpace; k++) {
                                    if (attacksArray[i + k]) {
                                        alreadyHit = true;
                                        break;
                                    }
                                }
                                if (!alreadyHit) {
                                    attacksArray[i] = true;
                                }
                            } else {
                                for (int k = -checkSpace; k + i < attacksArray.length - 1; k++) {
                                    if (attacksArray[i + k]) {
                                        alreadyHit = true;
                                        break;
                                    }
                                }
                                if (!alreadyHit) {
                                    attacksArray[i] = true;
                                }
                            }
                        }
                    }
                    for (int i = 0; i < attacksArray.length; i++) {
                        if (attacksArray[i]) {
                            Integer a = new Integer(i);
                            attacks.add(a);
                        }
                    }
                    sliceThreshold = _sliceThreshold;
                    outlet(3, "clear");
                    if (attacks.size() > 0) {
                        float audioLength = 0f;
                        if (bufferOperation) {
                            if (bufferArgs.length > 0) {
                                String bufferName = bufferArgs[0].getString();
                                if (MSPBuffer.getChannels(bufferName) != 0) {
                                    if (MSPBuffer.getLength(bufferName) <= Float.MAX_VALUE) {
                                        audioLength = (float) MSPBuffer.getLength(bufferName);
                                    }
                                }
                            }
                        } else {
                            try {
                                ais = AudioSystem.getAudioInputStream(new File(fileName));
                                if (ais.getFrameLength() <= Integer.MAX_VALUE) {
                                    audioLength = 1000f * (float) ais.getFrameLength() / ais.getFormat().getFrameRate();
                                }
                            } catch (UnsupportedAudioFileException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (attacks.size() == 1) {
                            outlet(3, new Atom[] { Atom.newAtom(0), Atom.newAtom(hopSize * attacks.get(0)), Atom.newAtom(audioLength - hopSize * attacks.get(0)) });
                        } else {
                            for (int i = 0; i < attacks.size() - 1; i++) {
                                outlet(3, new Atom[] { Atom.newAtom(i), Atom.newAtom(hopSize * attacks.get(i)), Atom.newAtom(hopSize * attacks.get(i + 1) - hopSize * attacks.get(i)) });
                            }
                            outlet(3, new Atom[] { Atom.newAtom((attacks.size() - 1)), Atom.newAtom(hopSize * attacks.get(attacks.size() - 1)), Atom.newAtom(audioLength - hopSize * attacks.get(attacks.size() - 1)) });
                        }
                        outlet(2, attacks.size());
                        if (lcd instanceof MaxBox) {
                            lcd.send("clear", null);
                            for (int i = 0; i < attacks.size(); i++) {
                                int attack = attacks.get(i);
                                lcd.send("frgb", new Atom[] { Atom.newAtom(255), Atom.newAtom(0), Atom.newAtom(0) });
                                lcd.send("linesegment", new Atom[] { Atom.newAtom(((float) attack / (float) mpeg7.audioPower.length) * lcdWidth), Atom.newAtom(0), Atom.newAtom(((float) attack / (float) mpeg7.audioPower.length) * lcdWidth), Atom.newAtom(lcdHeight) });
                            }
                        }
                    } else {
                        outlet(2, 0);
                        if (lcd instanceof MaxBox) {
                            lcd.send("clear", null);
                        }
                    }
                } else {
                    return;
                }
            } else {
            }
        }
    }

    @SuppressWarnings("unused")
    private void setSliceExclusionRadius(int i) {
        if (i >= 0) {
            sliceExclusionRadius = i;
            stack.execute(new SliceThresholdSetter());
        } else {
            post("MP7 Encoder Error:  slice min delta must be greater than zero\n");
        }
    }

    @SuppressWarnings("unused")
    private void setDrawto(String s) {
        drawto = s;
        lcd = this.getParentPatcher().getNamedBox(drawto);
        if (lcd instanceof MaxBox) {
            lcdWidth = (float) (lcd.getRect()[2] - lcd.getRect()[0]);
            lcdHeight = (float) (lcd.getRect()[3] - lcd.getRect()[1]);
        }
    }

    private void configure() {
        config = new ConfigDefault();
        config.enableAll(false);
        config.setValue("Resizer", "HopSize", hopSize);
        config.setValue("AudioPower", "enable", getAudioPower);
        config.setValue("AudioSpectrumEnvelope", "enable", getAudioSpectrumEnvelope);
        config.setValue("AudioSpectrumEnvelope", "resolution", aseResolution);
        config.setValue("AudioSpectrumEnvelope", "loEdge", aseLoEdge);
        config.setValue("AudioSpectrumEnvelope", "hiEdge", aseHiEdge);
        config.setValue("AudioSpectrumEnvelope", "dbScale", aseDbScale);
        config.setValue("AudioSpectrumEnvelope", "normalize", aseNormalize);
        config.setValue("AudioSpectrumCentroidSpread", "enable", getAudioSpectrumCentroidSpread);
        config.setValue("AudioSpectrumFlatness", "enable", getAudioSpectrumFlatness);
        config.setValue("AudioSpectrumFlatness", "loEdge", asfLoEdge);
        config.setValue("AudioSpectrumFlatness", "hiEdge", asfHiEdge);
        config.setValue("AudioSpectrumBasisProjection", "enable", getAudioSpectrumBasisProjection);
        config.setValue("AudioSpectrumBasisProjection", "numic", asbpNumic);
        config.setValue("AudioSpectrumBasisProjection", "frames", 0);
        config.setValue("AudioSignature", "enable", getAudioSignature);
        config.setValue("AudioSignature", "decimation", asDecimation);
        config.setValue("AudioFundamentalFrequency", "enable", getAudioFundamentalFrequency);
        config.setValue("AudioFundamentalFrequency", "lolimit", audioFFLoLimit);
        config.setValue("AudioFundamentalFrequency", "hilimit", audioFFHiLimit);
        config.setValue("AudioHarmonicity", "enable", getAudioHarmonicity);
        config.setValue("HarmonicSpectralCentroid", "enable", getHarmonicSpectralCentroid);
        config.setValue("HarmonicSpectralCentroid", "nonHarmonicity", harmonicNonHarmonicity);
        config.setValue("HarmonicSpectralCentroid", "threshold", harmonicThreshold);
        config.setValue("HarmonicSpectralDeviation", "enable", getHarmonicSpectralDeviation);
        config.setValue("HarmonicSpectralSpread", "enable", getHarmonicSpectralSpread);
        config.setValue("HarmonicSpectralVariation", "enable", getHarmonicSpectralVariation);
        config.setValue("TemporalCentroid", "enable", getTemporalCentroid);
        config.setValue("SpectralCentroid", "enable", getSpectralCentroid);
        config.setValue("LogAttackTime", "enable", getLogAttackTime);
        config.setValue("LogAttackTime", "threshold", latThreshold);
    }

    private void segmentConfigure() {
        segmentConfig = new ConfigDefault();
        segmentConfig.enableAll(false);
        segmentConfig.setValue("Resizer", "HopSize", hopSize);
        segmentConfig.setValue("AudioPower", "enable", getSegmentAudioPower);
        segmentConfig.setValue("AudioSpectrumEnvelope", "enable", getSegmentAudioSpectrumEnvelope);
        segmentConfig.setValue("AudioSpectrumEnvelope", "resolution", segmentAseResolution);
        segmentConfig.setValue("AudioSpectrumEnvelope", "loEdge", segmentAseLoEdge);
        segmentConfig.setValue("AudioSpectrumEnvelope", "hiEdge", segmentAseHiEdge);
        segmentConfig.setValue("AudioSpectrumEnvelope", "dbScale", segmentAseDbScale);
        segmentConfig.setValue("AudioSpectrumEnvelope", "normalize", segmentAseNormalize);
        segmentConfig.setValue("AudioSpectrumCentroidSpread", "enable", getSegmentAudioSpectrumCentroidSpread);
        segmentConfig.setValue("AudioSpectrumFlatness", "enable", getSegmentAudioSpectrumFlatness);
        segmentConfig.setValue("AudioSpectrumFlatness", "loEdge", segmentAsfLoEdge);
        segmentConfig.setValue("AudioSpectrumFlatness", "hiEdge", segmentAsfHiEdge);
        segmentConfig.setValue("AudioSpectrumBasisProjection", "enable", getSegmentAudioSpectrumBasisProjection);
        segmentConfig.setValue("AudioSpectrumBasisProjection", "numic", segmentAsbpNumic);
        segmentConfig.setValue("AudioSpectrumBasisProjection", "frames", 0);
        segmentConfig.setValue("AudioSignature", "enable", getSegmentAudioSignature);
        segmentConfig.setValue("AudioSignature", "decimation", segmentAsDecimation);
        segmentConfig.setValue("AudioFundamentalFrequency", "enable", getSegmentAudioFundamentalFrequency);
        segmentConfig.setValue("AudioFundamentalFrequency", "lolimit", segmentAudioFFLoLimit);
        segmentConfig.setValue("AudioFundamentalFrequency", "hilimit", segmentAudioFFHiLimit);
        segmentConfig.setValue("AudioHarmonicity", "enable", getSegmentAudioHarmonicity);
        segmentConfig.setValue("HarmonicSpectralCentroid", "enable", getSegmentHarmonicSpectralCentroid);
        segmentConfig.setValue("HarmonicSpectralCentroid", "nonHarmonicity", segmentHarmonicNonHarmonicity);
        segmentConfig.setValue("HarmonicSpectralCentroid", "threshold", segmentHarmonicThreshold);
        segmentConfig.setValue("HarmonicSpectralDeviation", "enable", getSegmentHarmonicSpectralDeviation);
        segmentConfig.setValue("HarmonicSpectralSpread", "enable", getSegmentHarmonicSpectralSpread);
        segmentConfig.setValue("HarmonicSpectralVariation", "enable", getSegmentHarmonicSpectralVariation);
        segmentConfig.setValue("TemporalCentroid", "enable", getSegmentTemporalCentroid);
        segmentConfig.setValue("SpectralCentroid", "enable", getSegmentSpectralCentroid);
        segmentConfig.setValue("LogAttackTime", "enable", getSegmentLogAttackTime);
        segmentConfig.setValue("LogAttackTime", "threshold", segmentLatThreshold);
    }

    private float[] getMeanAndStdDev(float[] data) {
        float total = 0f;
        for (int i = 0; i < data.length; i++) {
            total += data[i];
        }
        float mean = total / (float) data.length;
        total = 0f;
        for (int i = 0; i < data.length; i++) {
            total += Math.pow(data[i] - mean, 2.);
        }
        float stdDev = (float) Math.sqrt(total / (float) (data.length));
        float[] result = new float[2];
        result[0] = mean;
        result[1] = stdDev;
        return result;
    }

    public void file(String fileName) {
        this.fileName = fileName;
        stack.execute(new FileReader());
    }

    private class FileReader implements Runnable {

        public void run() {
            if (lcd instanceof MaxBox) {
                lcd.send("clear", null);
            }
            configure();
            try {
                bufferOperation = false;
                ais = AudioSystem.getAudioInputStream(new File(fileName));
                mpeg7 = new Mpeg7(config, ais);
                ais.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            }
            outletBang(4);
        }
    }

    public void buffer(Atom[] args) {
        this.bufferArgs = args;
        stack.execute(new BufferReader());
    }

    private class BufferReader implements Runnable {

        public void run() {
            if (bufferArgs.length > 0) {
                String bufferName = bufferArgs[0].getString();
                if (MSPBuffer.getChannels(bufferName) != 0) {
                    if (MSPBuffer.getFrames(bufferName) <= Integer.MAX_VALUE) {
                        int channel = 0;
                        if (bufferArgs.length > 1) {
                            try {
                                channel = bufferArgs[1].getInt();
                                if (MSPBuffer.getChannels(bufferName) <= channel) {
                                    post("MP7 Encoder Error: illegal channel number, defaulting to channel zero");
                                }
                            } catch (NumberFormatException e) {
                                post("MP7 Encoder Error: malformed channel number, defaulting to channel zero");
                            }
                        }
                        bufferOperation = true;
                        if (lcd instanceof MaxBox) {
                            lcd.send("clear", null);
                        }
                        float[] buffer = new float[(int) MSPBuffer.getFrames(bufferName)];
                        buffer = MSPBuffer.peek(bufferName, 0);
                        byte[] bytes = new byte[(int) MSPBuffer.getFrames(bufferName) * 2];
                        for (int i = 0; i < buffer.length; i++) {
                            int data = 0;
                            if (buffer[i] < 0f) {
                                if (buffer[i] > -1f) {
                                    data = (int) ((buffer[i]) * -1f * ((float) Integer.MIN_VALUE));
                                } else {
                                    data = Integer.MIN_VALUE;
                                }
                            } else {
                                if (buffer[i] < 1f) {
                                    data = (int) ((buffer[i]) * ((float) Integer.MAX_VALUE));
                                } else {
                                    data = Integer.MAX_VALUE;
                                }
                            }
                            bytes[i * 2] = (byte) (data >>> 16);
                            bytes[i * 2 + 1] = (byte) (data >>> 24);
                        }
                        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                        bufferSampleRate = (float) MSPBuffer.getFrames(bufferName) * 1000f / (float) MSPBuffer.getLength(bufferName);
                        AudioFormat af = new AudioFormat(bufferSampleRate, 16, 1, true, false);
                        ais = new AudioInputStream(bais, af, buffer.length);
                        configure();
                        mpeg7 = new Mpeg7(config, ais);
                        try {
                            ais.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        outletBang(4);
                    } else {
                        post("MP7 Encoder Error: Buffer " + bufferName + " too big to process\n");
                    }
                } else {
                    post("MP7 Encoder Error: Buffer " + bufferName + " not found\n");
                }
            } else {
                post("MP7 Encoder Error: bufffer requires at least one argument\n");
                post("Example 'buffer buffername bufferchannel filename'\n");
                post("the second and third optional arguments are the channel number of the buffer you wish to analyze\n");
                post("and the base file name you wish to assign to any samples you save from this buffer");
            }
        }
    }

    public void saveXML() {
        if (path != null) {
            if (mpeg7 == null) {
                post("MP7 Encoder Error: no data  to save, analyse a file or buffer first\n");
            } else {
                stack.execute(new XmlFileWriter());
            }
        } else {
            post("MP7 Encoder Error: cannot save xml without valid path\n");
        }
    }

    private class XmlFileWriter implements Runnable {

        public void run() {
            String xmlOutputFileName = "";
            if (bufferOperation == true) {
                if (bufferArgs.length > 2) {
                    xmlOutputFileName = me.path + bufferArgs[2].toString() + ".xml";
                } else {
                    xmlOutputFileName = me.path + bufferArgs[0].toString() + ".xml";
                }
            } else {
                xmlOutputFileName = me.path + (new File(fileName)).getName().split("\\.")[0] + ".xml";
            }
            mpeg7.write(xmlOutputFileName);
            post("MP7 Encoder: Wrote XML output to " + xmlOutputFileName + "\n");
            outletBang(4);
        }
    }

    public void slice() {
        stack.execute(new Slicer());
    }

    private class Slicer implements Runnable {

        public void run() {
            if ((mpeg7 == null) || (mpeg7.audioPower == null)) {
                post("MP7 Encoder Error: analyse a file or buffer with getAudioPower attribute enabled first\n");
            } else {
                sliceData = new SliceData[mpeg7.audioPower.length - 2];
                float[] filtered = new float[mpeg7.audioPower.length - 1];
                float maxFiltered = 0f;
                if (mpeg7.audioPower[0] >= sliceBackgroundThreshold) {
                    filtered[0] = mpeg7.audioPower[0];
                    if (filtered[0] > maxFiltered) {
                        maxFiltered = filtered[0];
                    } else {
                        filtered[0] = 0;
                    }
                    for (int i = 1; i < mpeg7.audioPower.length - 1; i++) {
                        float input = mpeg7.audioPower[i];
                        if (input < sliceBackgroundThreshold) {
                            input = 0f;
                        }
                        if (input - filtered[i - 1] <= 0) {
                            filtered[i] = filtered[i - 1] + alphaDown * (input - filtered[i - 1]);
                        } else {
                            filtered[i] = filtered[i - 1] + alphaUp * (input - filtered[i - 1]);
                        }
                        if (filtered[i] > maxFiltered) {
                            maxFiltered = filtered[i];
                        }
                    }
                    for (int i = 0; i < mpeg7.audioPower.length - 2; i++) {
                        float difference = filtered[i + 1] - filtered[i];
                        sliceData[i] = new SliceData();
                        sliceData[i].setFrameNumber(i);
                        sliceData[i].setDifference(difference);
                    }
                    Arrays.sort(sliceData);
                    setSliceThreshold(_sliceThreshold);
                }
                outletBang(4);
            }
        }
    }

    public void analyseSegments() {
        if (attacks.size() == 0) {
            post("MP7 Encoder Error: No segments to analyse, try using slice message first\n");
        } else {
            stack.execute(new segmentAnalyser());
        }
    }

    private class segmentAnalyser implements Runnable {

        public void run() {
            segmentConfigure();
            segments = new ArrayList<Mpeg7>();
            for (int i = 0; i < attacks.size(); i++) {
                Integer a = attacks.get(i);
                Integer d = new Integer(0);
                if (i == attacks.size() - 1) {
                    d = (Integer) mpeg7.audioPower.length;
                } else {
                    d = attacks.get(i + 1);
                }
                if (bufferOperation) {
                    int frameOffset = Math.round(0.01f * (float) a * bufferSampleRate);
                    int frameLength = Math.round(0.01f * (float) (d - a) * bufferSampleRate);
                    float[] buffer = new float[frameLength];
                    int channel = 0;
                    if (bufferArgs.length > 1) {
                        channel = bufferArgs[1].getInt();
                    }
                    buffer = MSPBuffer.peek(bufferArgs[0].getString(), channel, frameOffset, frameLength);
                    byte[] bytes = new byte[frameLength * 2];
                    for (int j = 0; j < buffer.length; j++) {
                        int data = 0;
                        if (buffer[j] < 0f) {
                            if (buffer[j] > -1f) {
                                data = (int) ((buffer[j]) * -1f * ((float) Integer.MIN_VALUE));
                            } else {
                                data = Integer.MIN_VALUE;
                            }
                        } else {
                            if (buffer[j] < 1f) {
                                data = (int) ((buffer[j]) * ((float) Integer.MAX_VALUE));
                            } else {
                                data = Integer.MAX_VALUE;
                            }
                        }
                        bytes[j * 2] = (byte) (data >>> 16);
                        bytes[j * 2 + 1] = (byte) (data >>> 24);
                    }
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    AudioFormat af = new AudioFormat(bufferSampleRate, 16, 1, true, false);
                    AudioInputStream sample = new AudioInputStream(bais, af, buffer.length);
                    Mpeg7 segment = new Mpeg7(segmentConfig, sample);
                    segments.add(segment);
                    try {
                        ais.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    int audioFrameSize = ais.getFormat().getFrameSize();
                    float audioFrameRate = ais.getFormat().getFrameRate();
                    AudioFormat format = ais.getFormat();
                    int byteOffset = Math.round(0.01f * (float) a * audioFrameRate * (float) audioFrameSize);
                    int byteLength = Math.round(0.01f * (float) (d - a) * audioFrameRate * (float) audioFrameSize);
                    byte[] sampleData = new byte[byteLength];
                    try {
                        ais = AudioSystem.getAudioInputStream(new File(fileName));
                        ais.skip(byteOffset);
                        ais.read(sampleData, 0, byteLength);
                        ByteArrayInputStream bais = new ByteArrayInputStream(sampleData);
                        AudioInputStream sample = new AudioInputStream(bais, format, byteLength / format.getFrameSize());
                        Mpeg7 segment = new Mpeg7(segmentConfig, sample);
                        segments.add(segment);
                        ais.close();
                    } catch (UnsupportedAudioFileException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            outletBang(4);
        }
    }

    public void saveSegmentsXML() {
        if (path != null) {
            if (segments.size() == 0) {
                post("MP7 Encoder Error: No mp7 segments to save, slice and analyse mp7 first\n");
            } else {
                if (bufferOperation) {
                    segmentBaseName = this.path;
                    if (bufferArgs.length > 2) {
                        segmentBaseName += bufferArgs[2].toString();
                    } else {
                        segmentBaseName += bufferArgs[0].toString();
                    }
                } else {
                    segmentBaseName = this.path + (new File(fileName)).getName().split("\\.")[0];
                }
                stack.execute(new SegmentsXmlFileWriter());
            }
        } else {
            post("MP7 Encoder Error: cannot save segments' xml without valid path\n");
        }
    }

    private class SegmentsXmlFileWriter implements Runnable {

        public void run() {
            for (int i = 0; i < segments.size(); i++) {
                Mpeg7 segment = segments.get(i);
                String xmlOutputFileName = segmentBaseName + Integer.toString(i) + ".xml";
                segment.write(xmlOutputFileName);
                post("Wrote XML output to " + xmlOutputFileName + "\n");
            }
            outletBang(4);
        }
    }

    public void saveSegmentsSounds() {
        if (path != null) {
            if (attacks.size() == 0) {
                post("MP7 Encoder Error: No segments to save\n");
            } else {
                if (bufferOperation) {
                    segmentBaseName = this.path;
                    if (bufferArgs.length > 2) {
                        segmentBaseName += bufferArgs[2].toString();
                    } else {
                        segmentBaseName += bufferArgs[0].toString();
                    }
                } else {
                    segmentBaseName = this.path + (new File(fileName)).getName().split("\\.")[0];
                }
                stack.execute(new SegmentsFileWriter());
            }
        } else {
            post("MP7 Encoder Error: cannot save segments as WAVs without valid path\n");
        }
    }

    public class SegmentsFileWriter implements Runnable {

        public void run() {
            outlet(1, "clear");
            int[] offsets = new int[attacks.size()];
            int[] lengths = new int[attacks.size()];
            for (int i = 0; i < attacks.size(); i++) {
                Integer a = attacks.get(i);
                Integer d = new Integer(0);
                if (i == attacks.size() - 1) {
                    d = (Integer) mpeg7.audioPower.length;
                } else {
                    d = attacks.get(i + 1);
                }
                if (bufferOperation) {
                    int frameOffset = Math.round(0.01f * (float) a * bufferSampleRate);
                    int frameLength = Math.round(0.01f * (float) (d - a) * bufferSampleRate);
                    offsets[i] = frameOffset;
                    lengths[i] = frameLength;
                    float[] buffer = new float[frameLength];
                    int channel = 0;
                    if (bufferArgs.length > 1) {
                        channel = bufferArgs[1].getInt();
                    }
                    buffer = MSPBuffer.peek(bufferArgs[0].getString(), channel, frameOffset, frameLength);
                    byte[] bytes = new byte[frameLength * 2];
                    for (int j = 0; j < buffer.length; j++) {
                        int data = 0;
                        if (buffer[j] < 0f) {
                            if (buffer[j] > -1f) {
                                data = (int) ((buffer[j]) * -1f * ((float) Integer.MIN_VALUE));
                            } else {
                                data = Integer.MIN_VALUE;
                            }
                        } else {
                            if (buffer[j] < 1f) {
                                data = (int) ((buffer[j]) * ((float) Integer.MAX_VALUE));
                            } else {
                                data = Integer.MAX_VALUE;
                            }
                        }
                        bytes[j * 2] = (byte) (data >>> 16);
                        bytes[j * 2 + 1] = (byte) (data >>> 24);
                    }
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                    AudioFormat af = new AudioFormat(bufferSampleRate, 16, 1, true, false);
                    AudioInputStream output = new AudioInputStream(bais, af, buffer.length);
                    String outputFileName = segmentBaseName + Integer.toString(i) + ".wav";
                    File outFile = new File(outputFileName);
                    try {
                        AudioSystem.write(output, AudioFileFormat.Type.WAVE, outFile);
                        ais.close();
                        post("MP7 Encoder: Wrote WAV output to " + outFile.getAbsolutePath() + "\n");
                        outlet(1, new Atom[] { Atom.newAtom("append"), Atom.newAtom(outputFileName) });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    int audioFrameSize = ais.getFormat().getFrameSize();
                    float audioFrameRate = ais.getFormat().getFrameRate();
                    AudioFormat format = ais.getFormat();
                    int byteOffset = Math.round(0.01f * (float) a * audioFrameRate * (float) audioFrameSize);
                    int byteLength = Math.round(0.01f * (float) (d - a) * audioFrameRate * (float) audioFrameSize);
                    byte[] sampleData = new byte[byteLength];
                    try {
                        ais = AudioSystem.getAudioInputStream(new File(fileName));
                        ais.skip(byteOffset);
                        ais.read(sampleData, 0, byteLength);
                        ByteArrayInputStream bais = new ByteArrayInputStream(sampleData);
                        AudioInputStream output = new AudioInputStream(bais, format, byteLength / format.getFrameSize());
                        String suffix = Integer.toString(i) + ".wav";
                        String outputFileName = segmentBaseName.replace(".wav", suffix);
                        File outFile = new File(outputFileName);
                        AudioSystem.write(output, AudioFileFormat.Type.WAVE, outFile);
                        ais.close();
                        post("MP7 Encoder: Wrote WAV output to " + outputFileName + "\n");
                    } catch (UnsupportedAudioFileException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            outletBang(4);
        }
    }

    public void storeSQL() {
        if (mpeg7 != null) {
            String sql = "INSERT OR REPLACE INTO sounds ('name', 'type'";
            if (mpeg7.audioPower != null) sql += ", 'audioPowerMean', 'audioPowerStdDev'";
            if (mpeg7.temporalCentroid != null) sql += ", 'temporalCentroid'";
            if (mpeg7.spectralCentroid != null) sql += ", 'spectralCentroid'";
            if (mpeg7.logAttackTime != null) sql += ", 'logAttackTime'";
            if (mpeg7.audioFundamentalFrequency != null) sql += ", 'audioFundamentalFrequencyMean', 'audioFundamentalFrequencyStdDev'";
            if (mpeg7.harmonicRatio != null) sql += ", 'audioHarmonicityMean', 'audioHarmonicityStdDev'";
            if (mpeg7.harmonicSpectralCentroid != null) sql += ", 'harmonicSpectralCentroid'";
            if (mpeg7.harmonicSpectralDeviation != null) sql += ", 'harmonicSpectralDeviation'";
            if (mpeg7.harmonicSpectralSpread != null) sql += ", 'harmonicSpectralSpread'";
            if (mpeg7.harmonicSpectralVariation != null) sql += ", 'harmonicSpectralVariation'";
            if (bufferOperation) {
                if (bufferArgs.length > 2) {
                    sql += ") VALUES ('" + bufferArgs[2].getString() + "', 'buffer'";
                } else {
                    sql += ") VALUES ('" + bufferArgs[0].getString() + "', 'buffer'";
                }
            } else {
                sql += ") VALUES (\"" + fileName + "\", 'file'";
            }
            if (mpeg7.audioPower != null) {
                float[] audioPower = getMeanAndStdDev(mpeg7.audioPower);
                sql += ", '" + audioPower[0] + "', '" + audioPower[1] + "'";
            }
            if (mpeg7.temporalCentroid != null) sql += ", '" + mpeg7.temporalCentroid + "'";
            if (mpeg7.spectralCentroid != null) sql += ", '" + mpeg7.spectralCentroid + "'";
            if (mpeg7.logAttackTime != null) sql += ", '" + mpeg7.logAttackTime + "'";
            if (mpeg7.audioFundamentalFrequency != null) {
                float[] audioFundamentalFrequency = getMeanAndStdDev(mpeg7.audioFundamentalFrequency);
                sql += ", '" + audioFundamentalFrequency[0] + "', '" + audioFundamentalFrequency[1] + "'";
            }
            if (mpeg7.harmonicRatio != null) {
                float[] audioHarmonicity = getMeanAndStdDev(mpeg7.harmonicRatio);
                sql += ", '" + audioHarmonicity[0] + "', '" + audioHarmonicity[1] + "'";
            }
            if (mpeg7.harmonicSpectralCentroid != null) sql += ", '" + mpeg7.harmonicSpectralCentroid + "'";
            if (mpeg7.harmonicSpectralDeviation != null) sql += ", '" + mpeg7.harmonicSpectralDeviation + "'";
            if (mpeg7.harmonicSpectralSpread != null) sql += ", '" + mpeg7.harmonicSpectralSpread + "'";
            if (mpeg7.harmonicSpectralVariation != null) sql += ", '" + mpeg7.harmonicSpectralVariation + "'";
            sql += ")";
            if (mp7db instanceof MaxBox) {
                mp7db.send("insertData", new Atom[] { Atom.newAtom(sql) });
                outletBang(4);
            } else {
                outlet(0, "insertData", new Atom[] { Atom.newAtom(sql) });
                outletBang(4);
            }
        } else {
            post("MP7 Encoder Error: No mpeg7 data to store in database, make sure some sound analysis attributes are enabled and analyse a file or buffer\n");
        }
    }

    public void testSQL() {
        outlet(0, "insertData", new Atom[] { Atom.newAtom("INSERT INTO test ('foo') VALUES('43')") });
    }

    public void storeSegmentsSQL() {
        if (segments.size() > 0) {
            if (mp7db instanceof MaxBox) {
                if (bufferOperation) {
                    if (bufferArgs.length > 2) {
                        mp7db.send("insertData", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE name = '" + bufferArgs[2].getString() + "'") });
                    } else {
                        mp7db.send("insertData", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE name = '" + bufferArgs[0].getString() + "'") });
                    }
                } else {
                    mp7db.send("insertData", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE name = '" + fileName + "'") });
                }
            } else {
                if (bufferOperation) {
                    if (bufferArgs.length > 2) {
                        outlet(0, "insertData", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE name = '" + bufferArgs[2].getString() + "'") });
                    } else {
                        outlet(0, "insertData", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE name = '" + bufferArgs[0].getString() + "'") });
                    }
                } else {
                    outlet(0, "insertData", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE name = '" + fileName + "'") });
                }
            }
            for (int i = 0; i < segments.size(); i++) {
                Integer a = attacks.get(i);
                Integer d = new Integer(0);
                if (i == attacks.size() - 1) {
                    d = (Integer) mpeg7.audioPower.length;
                } else {
                    d = attacks.get(i + 1);
                }
                int start = a * 10;
                int end = d * 10;
                int length = (d - a) * 10;
                Mpeg7 segment = segments.get(i);
                String sql = "INSERT INTO segments ('name', 'start', 'end', 'length'";
                if (segment.audioPower != null) sql += ", 'audioPowerMean', 'audioPowerStdDev'";
                if (segment.temporalCentroid != null) sql += ", 'temporalCentroid'";
                if (segment.spectralCentroid != null) sql += ", 'spectralCentroid'";
                if (segment.logAttackTime != null) sql += ", 'logAttackTime'";
                if (segment.audioFundamentalFrequency != null) sql += ", 'audioFundamentalFrequencyMean', 'audioFundamentalFrequencyStdDev'";
                if (segment.harmonicRatio != null) sql += ", 'audioHarmonicityMean', 'audioHarmonicityStdDev'";
                if (segment.harmonicSpectralCentroid != null) sql += ", 'harmonicSpectralCentroid'";
                if (segment.harmonicSpectralDeviation != null) sql += ", 'harmonicSpectralDeviation'";
                if (segment.harmonicSpectralSpread != null) sql += ", 'harmonicSpectralSpread'";
                if (segment.harmonicSpectralVariation != null) sql += ", 'harmonicSpectralVariation'";
                if (bufferOperation) {
                    if (bufferArgs.length > 2) {
                        sql += ") VALUES ('" + bufferArgs[2].getString() + "', '" + start + "', '" + end + "', '" + length + "'";
                    } else {
                        sql += ") VALUES ('" + bufferArgs[0].getString() + "', '" + start + "', '" + end + "', '" + length + "'";
                    }
                } else {
                    sql += ") VALUES ('" + fileName + "', '" + start + "', '" + end + "', '" + length + "'";
                }
                if (bufferOperation) if (segment.audioPower != null) {
                    float[] audioPower = getMeanAndStdDev(segment.audioPower);
                    sql += ", '" + audioPower[0] + "', '" + audioPower[1] + "'";
                }
                if (segment.temporalCentroid != null) sql += ", '" + segment.temporalCentroid + "'";
                if (segment.spectralCentroid != null) sql += ", '" + segment.spectralCentroid + "'";
                if (segment.logAttackTime != null) sql += ", '" + segment.logAttackTime + "'";
                if (segment.audioFundamentalFrequency != null) {
                    float[] audioFundamentalFrequency = getMeanAndStdDev(segment.audioFundamentalFrequency);
                    sql += ", '" + audioFundamentalFrequency[0] + "', '" + audioFundamentalFrequency[1] + "'";
                }
                if (segment.harmonicRatio != null) {
                    float[] audioHarmonicity = getMeanAndStdDev(segment.harmonicRatio);
                    sql += ", '" + audioHarmonicity[0] + "', '" + audioHarmonicity[1] + "'";
                }
                if (segment.harmonicSpectralCentroid != null) sql += ", '" + segment.harmonicSpectralCentroid + "'";
                if (segment.harmonicSpectralDeviation != null) sql += ", '" + segment.harmonicSpectralDeviation + "'";
                if (segment.harmonicSpectralSpread != null) sql += ", '" + segment.harmonicSpectralSpread + "'";
                if (segment.harmonicSpectralVariation != null) sql += ", '" + segment.harmonicSpectralVariation + "'";
                sql += ")";
                if (mp7db instanceof MaxBox) {
                    mp7db.send("insertData", new Atom[] { Atom.newAtom(sql) });
                    outletBang(4);
                } else {
                    outlet(0, "insertData", new Atom[] { Atom.newAtom(sql) });
                    outletBang(4);
                }
            }
        } else {
            post("MP7 Encoder Error: No mpeg7 segment data to store in database,\n");
            post("make sure some there are some segments by using the slice and sliceThreshold messages,\n");
            post("then analyseSegments with some segment analysis attributes enabled\n");
        }
    }
}
