import com.cycling74.max.*;
import com.cycling74.msp.*;
import de.crysandt.audio.mpeg7audio.*;
import java.io.*;
import java.math.*;
import javax.sound.sampled.*;
import java.lang.String;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import org.w3c.dom.*;
import javax.xml.xpath.*;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Mpeg7Encoder extends MaxObject {

    private String fileName;

    private AudioInputStream ais;

    private Config config;

    private Config segmentConfig;

    private Mpeg7 mpeg7;

    private ArrayList<Integer> attacks;

    private ArrayList<Mpeg7> segments;

    private boolean bufferOperation;

    private MaxQelem fileQ;

    private MaxQelem bufferQ;

    private Atom[] bufferArgs;

    private float bufferSampleRate;

    private String segmentBaseName;

    private MaxQelem saveXMLQ;

    private MaxQelem sliceQ;

    private MaxQelem analyseSegmentsQ;

    private MaxQelem saveSegmentsXMLQ;

    private MaxQelem saveSegmentsSoundsQ;

    private MaxQelem setSliceThresholdQ;

    private int hopSize = 10;

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

    float sliceThreshold = 0.18f;

    float sliceBackgroundThreshold = 0.0004f;

    int sliceExclusionRadius = 8;

    String drawto = null;

    MaxBox lcd;

    float lcdWidth = 100f;

    float lcdHeight = 100f;

    MaxBox mp7db;

    MaxBox mpeg7Config;

    Mpeg7Encoder me = this;

    private static final String[] INLET_ASSIST = new String[] { "inlet 1 help" };

    private static final String[] OUTLET_ASSIST = new String[] { "outlet 1 help" };

    public Mpeg7Encoder(Atom[] args) {
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
        declareOutlets(new int[] { DataTypes.ALL, DataTypes.ALL });
        setInletAssist(new String[] { "file <filenpath>, buffer <buffername>, ...." });
        setOutletAssist(new String[] { "SQL data", "bang, done." });
        fileQ = new MaxQelem(new Callback(this, "_file"));
        bufferQ = new MaxQelem(new Callback(this, "_buffer"));
        saveXMLQ = new MaxQelem(new Callback(this, "_saveXML"));
        sliceQ = new MaxQelem(new Callback(this, "_slice"));
        analyseSegmentsQ = new MaxQelem(new Callback(this, "_analyseSegments"));
        saveSegmentsXMLQ = new MaxQelem(new Callback(this, "_saveSegmentsXML"));
        saveSegmentsSoundsQ = new MaxQelem(new Callback(this, "_saveSegmentsSounds"));
        setSliceThresholdQ = new MaxQelem(new Callback(this, "_setSliceThreshold"));
        attacks = new ArrayList<Integer>();
        segments = new ArrayList<Mpeg7>();
        mp7db = this.getParentPatcher().getNamedBox("mp7db");
        post("ver 0.31\n");
    }

    public void notifyDeleted() {
        fileQ.release();
        bufferQ.release();
        saveXMLQ.release();
        sliceQ.release();
        analyseSegmentsQ.release();
        saveSegmentsXMLQ.release();
        saveSegmentsSoundsQ.release();
        setSliceThresholdQ.release();
    }

    public void test() {
        int foo[] = { 3, 45, 1 };
        post(foo[-1] + "\n");
    }

    private void setAseResolution(float f) {
        if (f == 1.0f || f == 0.5f || f == 0.25f || f == 0.125f) {
            aseResolution = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope resolution are 1, 0.5, 0.25 and 0.125\n");
        }
    }

    private void setSegmentAseResolution(float f) {
        if (f == 1.0f || f == 0.5f || f == 0.25f || f == 0.125f) {
            segmentAseResolution = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope resolution are 1, 0.5, 0.25 and 0.125\n");
        }
    }

    private void setAseLoEdge(float f) {
        if (f == 31.25f || f == 62.5f || f == 125f || f == 250f || f == 500f) {
            aseLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    private void setSegmentAseLoEdge(float f) {
        if (f == 31.25f || f == 62.5f || f == 125f || f == 250f || f == 500f) {
            segmentAseLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    private void setAseHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            aseHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    private void setSegmentAseHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            segmentAseHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    private void setAseDbScale(boolean b) {
        if (b == true && getAudioSpectrumBasisProjection == true) {
            getAudioSpectrumBasisProjection = false;
        }
        aseDbScale = b;
    }

    private void setSegmentAseDbScale(boolean b) {
        if (b == true && getSegmentAudioSpectrumBasisProjection == true) {
            getSegmentAudioSpectrumBasisProjection = false;
        }
        segmentAseDbScale = b;
    }

    private void setAseNormalize(boolean b) {
        if (b) {
            aseNormalize = "on";
        } else {
            aseNormalize = "off";
        }
    }

    private void setSegmentAseNormalize(boolean b) {
        if (b) {
            segmentAseNormalize = "on";
        } else {
            segmentAseNormalize = "off";
        }
    }

    private boolean getAseNormalize() {
        if (aseNormalize == "on") {
            return true;
        } else {
            return false;
        }
    }

    private boolean getSegmentAseNormalize() {
        if (segmentAseNormalize == "on") {
            return true;
        } else {
            return false;
        }
    }

    private void setAsfLoEdge(float f) {
        if (f == 62.5f || f == 125f || f == 250f || f == 500f) {
            asfLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumFlatness low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    private void setSegmentAsfLoEdge(float f) {
        if (f == 62.5f || f == 125f || f == 250f || f == 500f) {
            segmentAsfLoEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumFlatness low edge are 31.25, 62.5, 125.0, 250 and 500\n");
        }
    }

    private void setAsfHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            asfHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    private void setSegmentAsfHiEdge(float f) {
        if (f == 2000f || f == 4000f || f == 8000f || f == 16000f) {
            asfHiEdge = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumEnvelope high edge are 2000, 4000, 8000 and 16000\n");
        }
    }

    private void setGetAudioSpectrumBasisProjection(boolean b) {
        if (b == true && aseDbScale == true) {
            aseDbScale = false;
        }
        getAudioSpectrumBasisProjection = b;
    }

    private void setSegmentGetAudioSpectrumBasisProjection(boolean b) {
        if (b == true && segmentAseDbScale == true) {
            segmentAseDbScale = false;
        }
        getSegmentAudioSpectrumBasisProjection = b;
    }

    private void setAsbpNumic(int i) {
        if (i >= 1) {
            asbpNumic = i;
        } else {
            post("MP7 Encoder Error: valid values for AudioSpectrumBasisProjection numic are integers greater than zero, try 10\n");
        }
    }

    private void setSegmentAsbpNumic(int i) {
        if (i >= 1) {
            segmentAsbpNumic = i;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSpectrumBasisProjection numic are integers greater than zero, try 10\n");
        }
    }

    private void setAsDecimation(int i) {
        if (i == 8 || i == 16 || i == 32 || i == 64) {
            asDecimation = i;
        } else {
            post("MP7 Encoder Error: valid values for AudioSignature decimation are 8, 16, 32, 64\n");
        }
    }

    private void setSegmentAsDecimation(int i) {
        if (i == 8 || i == 16 || i == 32 || i == 64) {
            segmentAsDecimation = i;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioSignature decimation are 8, 16, 32, 64\n");
        }
    }

    private void setAudioFFLoLimit(float f) {
        if (f == 5f || f == 10f || f == 20f || f == 25f || f == 50f) {
            audioFFLoLimit = f;
        } else {
            post("MP7 Encoder Error: valid values for AudioFundamentalFrequency low edge are 5.0, 10.0, 20.0, 25.0 and 50.0\n");
        }
    }

    private void setSegmentAudioFFLoLimit(float f) {
        if (f == 5f || f == 10f || f == 20f || f == 25f || f == 50f) {
            segmentAudioFFLoLimit = f;
        } else {
            post("MP7 Encoder Error: valid values for segment AudioFundamentalFrequency low edge are 5.0, 10.0, 20.0, 25.0 and 50.0\n");
        }
    }

    private void setAudioFFHiLimit(float f) {
        if (f > audioFFLoLimit) {
            audioFFHiLimit = f;
        } else {
            post("MP7 Encoder Error: AudioFundamentalFrequency high edge must be greater than AudioFundamentalFrequency LowEdge");
        }
    }

    private void setSegmentAudioFFHiLimit(float f) {
        if (f > segmentAudioFFLoLimit) {
            segmentAudioFFHiLimit = f;
        } else {
            post("MP7 Encoder Error: segment AudioFundamentalFrequency high edge must be greater than AudioFundamentalFrequency LowEdge");
        }
    }

    private void setHarmonicNonHarmonicity(float f) {
        if (f >= 0 && f <= 0.575) {
            harmonicNonHarmonicity = f;
        } else {
            post("MP7 Encoder Error: Harmonic nonHarmonicity must be between 0.0 and 0.575\n");
        }
    }

    private void setSegmentHarmonicNonHarmonicity(float f) {
        if (f >= 0 && f <= 0.575) {
            segmentHarmonicNonHarmonicity = f;
        } else {
            post("MP7 Encoder Error: segment Harmonic nonHarmonicity must be between 0.0 and 0.575\n");
        }
    }

    private void setHarmonicThreshold(float f) {
        if (f >= 0 && f < 1.0) {
            harmonicThreshold = f;
        } else {
            post("MP7 Encoder Error: Harmonic threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    private void setSegmentHarmonicThreshold(float f) {
        if (f >= 0 && f < 1.0) {
            harmonicThreshold = f;
        } else {
            post("MP7 Encoder Error: segment Harmonic threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    private void setGetHarmonicSpectralDeviation(boolean b) {
        if (b == true && getHarmonicSpectralCentroid == false) {
            getHarmonicSpectralCentroid = true;
        }
        getHarmonicSpectralDeviation = b;
    }

    private void setGetSegmentHarmonicSpectralDeviation(boolean b) {
        if (b == true && getSegmentHarmonicSpectralCentroid == false) {
            getSegmentHarmonicSpectralCentroid = true;
        }
        getSegmentHarmonicSpectralDeviation = b;
    }

    private void setGetHarmonicSpectralSpread(boolean b) {
        if (b == true && getHarmonicSpectralCentroid == false) {
            getHarmonicSpectralCentroid = true;
        }
        getHarmonicSpectralSpread = b;
    }

    private void setGetSegmentHarmonicSpectralSpread(boolean b) {
        if (b == true && getSegmentHarmonicSpectralCentroid == false) {
            getSegmentHarmonicSpectralCentroid = true;
        }
        getSegmentHarmonicSpectralSpread = b;
    }

    private void setGetHarmonicSpectralVariation(boolean b) {
        if (b == true && getHarmonicSpectralCentroid == false) {
            getHarmonicSpectralCentroid = true;
        }
        getHarmonicSpectralVariation = b;
    }

    private void setGetSegmentHarmonicSpectralVariation(boolean b) {
        if (b == true && getSegmentHarmonicSpectralCentroid == false) {
            getSegmentHarmonicSpectralCentroid = true;
        }
        getSegmentHarmonicSpectralVariation = b;
    }

    private void setLatThreshold(float f) {
        if (f >= 0f && f < 1.0f) {
            latThreshold = f;
        } else {
            post("MP7 Encoder Error: LogAttackTime threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    private void setSegmentLatThreshold(float f) {
        if (f >= 0f && f < 1.0f) {
            segmentLatThreshold = f;
        } else {
            post("MP7 Encoder Error: segment LogAttackTime threshold must be greater than or equal to 0.0 and less than 1.0\n");
        }
    }

    private float getSliceThreshold() {
        return (1f - sliceThreshold);
    }

    private void setSliceThreshold(float f) {
        if (f >= 0f && f <= 1f) {
            sliceThreshold = 1f - f;
            setSliceThresholdQ.set();
        } else {
            post("MP7 Encoder Error: slice threshold must be in range 0.0 to 1.0 inclusive\n");
        }
    }

    private void _setSliceThreshold() {
        Thread t = new Thread() {

            public void run() {
                if (sliceData != null) {
                    attacks = new ArrayList<Integer>();
                    if (lcd instanceof MaxBox) {
                        lcd.send("clear", null);
                    }
                    boolean[] attacksArray = new boolean[sliceData.length];
                    int checkSpace = 1;
                    if (sliceData.length < 2 * sliceExclusionRadius) {
                        post("MP7 Encoder Warn: sample length smaller than exclusion diameter");
                        return;
                    } else {
                        checkSpace = sliceExclusionRadius;
                    }
                    for (int j = 0; j < sliceData.length; j++) {
                        int i = sliceData[sliceData.length - 1 - j].getFrameNumber();
                        boolean alreadyHit = false;
                        if (i >= checkSpace && i <= sliceData.length - 1 - checkSpace) {
                            for (int k = checkSpace * -1; k <= checkSpace; k++) {
                                if (attacksArray[i + k]) {
                                    alreadyHit = true;
                                    break;
                                }
                            }
                            if (!alreadyHit && sliceData[sliceData.length - 1 - j].getDifference() >= sliceThreshold) {
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
                                if (!alreadyHit && sliceData[sliceData.length - 1 - j].getDifference() >= sliceThreshold) {
                                    attacksArray[i] = true;
                                }
                            } else {
                                for (int k = -checkSpace; k + i < attacksArray.length - 1; k++) {
                                    if (attacksArray[i + k]) {
                                        alreadyHit = true;
                                        break;
                                    }
                                }
                                if (!alreadyHit && sliceData[sliceData.length - 1 - j].getDifference() >= sliceThreshold) {
                                    attacksArray[i] = true;
                                }
                            }
                        }
                    }
                    for (int i = 0; i < attacksArray.length; i++) {
                        if (attacksArray[i]) {
                            Integer a = new Integer(i);
                            attacks.add(a);
                            if (lcd instanceof MaxBox) {
                                lcd.send("frgb", new Atom[] { Atom.newAtom(255), Atom.newAtom(0), Atom.newAtom(0) });
                                lcd.send("linesegment", new Atom[] { Atom.newAtom(((float) i / (float) mpeg7.audioPower.length) * lcdWidth), Atom.newAtom(0), Atom.newAtom(((float) i / (float) mpeg7.audioPower.length) * lcdWidth), Atom.newAtom(lcdHeight) });
                            }
                        }
                    }
                } else {
                    return;
                }
            }
        };
        t.start();
    }

    private void setSliceExclusionRadius(int i) {
        if (i >= 0) {
            sliceExclusionRadius = i;
            setSliceThreshold(sliceThreshold);
        } else {
            post("MP7 Encoder Error:  slice min delta must be greater than zero\n");
        }
    }

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
        fileQ.set();
    }

    private void _file() {
        Thread t = new Thread() {

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
                outletBang(1);
            }
        };
        t.start();
    }

    public void buffer(Atom[] args) {
        this.bufferArgs = args;
        bufferQ.set();
    }

    private void _buffer() {
        Thread t = new Thread() {

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
                            outletBang(getInfoIdx());
                            try {
                                ais.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
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
        };
        t.start();
    }

    public void saveXML() {
        if (mpeg7 == null) {
            post("MP7 Encoder Error: no data  to save, analyse a file or buffer first\n");
        } else {
            saveXMLQ.set();
        }
    }

    private void _saveXML() {
        Thread t = new Thread() {

            public void run() {
                String xmlOutputFileName = "";
                if (bufferOperation == true) {
                    xmlOutputFileName = me.getParentPatcher().getPath() + bufferArgs[0] + ".xml";
                } else {
                    try {
                        xmlOutputFileName = (new File(fileName)).getCanonicalPath().split("\\.")[0] + ".xml";
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mpeg7.write(xmlOutputFileName);
                post("MP7 Encoder: Wrote XML output to " + xmlOutputFileName + "\n");
                outletBang(getInfoIdx());
            }
        };
        t.start();
    }

    public void slice() {
        if ((mpeg7 == null) || (mpeg7.audioPower == null)) {
            post("MP7 Encoder Error: analyse a file or buffer with getAudioPower attribute enabled first\n");
        } else {
            sliceQ.set();
        }
    }

    private void _slice() {
        Thread t = new Thread() {

            public void run() {
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
                    if (lcd instanceof MaxBox) {
                        lcd.send("clear", null);
                        lcd.send("frgb", new Atom[] { Atom.newAtom(255), Atom.newAtom(0), Atom.newAtom(0) });
                        for (int i = 0; i < filtered.length; i++) {
                            lcd.send("linesegment", new Atom[] { Atom.newAtom(((float) i / (float) mpeg7.audioPower.length) * lcdWidth), Atom.newAtom(lcdHeight), Atom.newAtom(((float) i / (float) mpeg7.audioPower.length) * lcdWidth), Atom.newAtom(lcdHeight - lcdHeight * (filtered[i] / maxFiltered)) });
                        }
                    }
                    for (int i = 0; i < mpeg7.audioPower.length - 2; i++) {
                        float difference = filtered[i + 1] - filtered[i];
                        sliceData[i] = new SliceData();
                        sliceData[i].setFrameNumber(i);
                        sliceData[i].setDifference(difference);
                    }
                    Arrays.sort(sliceData);
                    setSliceThreshold(sliceThreshold);
                }
                outletBang(getInfoIdx());
            }
        };
        t.start();
    }

    public void analyseSegments() {
        if (attacks.size() == 0) {
            post("MP7 Encoder Error: No segments to analyse\n");
        } else {
            analyseSegmentsQ.set();
        }
    }

    private void _analyseSegments() {
        Thread t = new Thread() {

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
                outletBang(getInfoIdx());
            }
        };
        t.start();
    }

    public void saveSegmentsXML(String s) {
        if (segments.size() == 0) {
            post("analyse segments first\n");
        } else {
            if (bufferOperation) {
                segmentBaseName = this.getParentPatcher().getPath();
                if (s.length() == 0) {
                    segmentBaseName += bufferArgs[0].toString();
                } else {
                    segmentBaseName += s;
                }
            } else {
                try {
                    segmentBaseName = (new File(fileName)).getCanonicalPath().split("\\.")[0];
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            saveSegmentsXMLQ.set();
        }
    }

    private void _saveSegmentsXML() {
        Thread t = new Thread() {

            public void run() {
                for (int i = 0; i < segments.size(); i++) {
                    Mpeg7 segment = segments.get(i);
                    String xmlOutputFileName = segmentBaseName + Integer.toString(i) + ".xml";
                    segment.write(xmlOutputFileName);
                    post("Wrote XML output to " + xmlOutputFileName + "\n");
                }
                outletBang(getInfoIdx());
            }
        };
        t.start();
    }

    public void saveSegmentsSounds(String s) {
        if (attacks.size() == 0) {
            post("MP7 Encoder Error: No segments to save\n");
        } else {
            if (bufferOperation) {
                segmentBaseName = this.getParentPatcher().getPath();
                if (s.length() == 0) {
                    segmentBaseName += bufferArgs[0].toString();
                } else {
                    segmentBaseName += s;
                }
            } else {
                try {
                    segmentBaseName = (new File(fileName)).getCanonicalPath();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            saveSegmentsSoundsQ.set();
        }
    }

    private void _saveSegmentsSounds() {
        Thread t = new Thread() {

            public void run() {
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
                        AudioInputStream output = new AudioInputStream(bais, af, buffer.length);
                        String outputFileName = segmentBaseName + Integer.toString(i) + ".wav";
                        File outFile = new File(outputFileName);
                        try {
                            AudioSystem.write(output, AudioFileFormat.Type.WAVE, outFile);
                            ais.close();
                            post("MP7 Encoder: Wrote WAV output to " + outputFileName + "\n");
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
                outletBang(getInfoIdx());
            }
        };
        t.start();
    }

    public void storeSQL() {
        if (mp7db instanceof MaxBox) {
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
                if (bufferOperation) sql += ") VALUES ('" + bufferArgs[0].getString() + "', 'buffer'";
                if (!bufferOperation) sql += ") VALUES ('" + fileName + "', 'file'";
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
                mp7db.send("exec", new Atom[] { Atom.newAtom(sql) });
            } else {
                post("MP7 Encoder Error: No mpeg7 data to store in database, make sure some sound analysis attributes are enabled and analyse a file or buffer\n");
            }
        } else {
            post("MP7 Encoder Error: Cannot store mpeg7 data, cannot find js object named mp7db to interface with\n");
        }
    }

    public void storeSegmentsSQL() {
        if (mp7db instanceof MaxBox) {
            if (segments.size() > 0) {
                if (bufferOperation) mp7db.send("exec", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE parent = '" + bufferArgs[0].getString() + "'") });
                if (!bufferOperation) mp7db.send("exec", new Atom[] { Atom.newAtom("DELETE FROM segments WHERE parent = '" + fileName + "'") });
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
                    String sql = "INSERT INTO segments ('parent', 'start', 'end', 'length'";
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
                    if (bufferOperation) sql += ") VALUES ('" + bufferArgs[0].getString() + "', '" + start + "', '" + end + "', '" + length + "'";
                    if (!bufferOperation) sql += ") VALUES ('" + fileName + "', '" + start + "', '" + end + "', '" + length + "'";
                    if (segment.audioPower != null) {
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
                    mp7db.send("exec", new Atom[] { Atom.newAtom(sql) });
                }
            } else {
                post("MP7 Encoder Error: No mpeg7 segment data to store in database, make sure some there are some segments by using the slice and sliceThreshold messages, then analyse segments with some segment analysis attributes enabled\n");
            }
        } else {
            post("MP7 Encoder Error: Cannot store mpeg7 data, cannot find js object named mp7db to interface with\n");
        }
    }
}
