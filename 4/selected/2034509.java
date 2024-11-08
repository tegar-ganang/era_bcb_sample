package com.googlecode.mp4parser.boxes.mp4.objectdescriptors;

import com.coremedia.iso.IsoTypeWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

@Descriptor(tags = 0x5, objectTypeIndication = 0x40)
public class AudioSpecificConfig extends BaseDescriptor {

    ByteBuffer configBytes;

    long endPos;

    public static Map<Integer, Integer> samplingFrequencyIndexMap = new HashMap<Integer, Integer>();

    public static Map<Integer, String> audioObjectTypeMap = new HashMap<Integer, String>();

    int audioObjectType;

    int samplingFrequencyIndex;

    int samplingFrequency;

    int channelConfiguration;

    int extensionAudioObjectType;

    int sbrPresentFlag;

    int psPresentFlag;

    int extensionSamplingFrequencyIndex;

    int extensionSamplingFrequency;

    int extensionChannelConfiguration;

    int sacPayloadEmbedding;

    int fillBits;

    int epConfig;

    int directMapping;

    int syncExtensionType;

    int frameLengthFlag;

    int dependsOnCoreCoder;

    int coreCoderDelay;

    int extensionFlag;

    int layerNr;

    int numOfSubFrame;

    int layer_length;

    int aacSectionDataResilienceFlag;

    int aacScalefactorDataResilienceFlag;

    int aacSpectralDataResilienceFlag;

    int extensionFlag3;

    boolean gaSpecificConfig;

    int isBaseLayer;

    int paraMode;

    int paraExtensionFlag;

    int hvxcVarMode;

    int hvxcRateMode;

    int erHvxcExtensionFlag;

    int var_ScalableFlag;

    int hilnQuantMode;

    int hilnMaxNumLine;

    int hilnSampleRateCode;

    int hilnFrameLength;

    int hilnContMode;

    int hilnEnhaLayer;

    int hilnEnhaQuantMode;

    boolean parametricSpecificConfig;

    @Override
    public void parseDetail(ByteBuffer bb) throws IOException {
        configBytes = bb.slice();
        configBytes.limit(sizeOfInstance);
        bb.position(bb.position() + sizeOfInstance);
        BitReaderBuffer bitReaderBuffer = new BitReaderBuffer(configBytes);
        audioObjectType = getAudioObjectType(bitReaderBuffer);
        samplingFrequencyIndex = bitReaderBuffer.readBits(4);
        if (samplingFrequencyIndex == 0xf) {
            samplingFrequency = bitReaderBuffer.readBits(24);
        }
        channelConfiguration = bitReaderBuffer.readBits(4);
        if (audioObjectType == 5 || audioObjectType == 29) {
            extensionAudioObjectType = 5;
            sbrPresentFlag = 1;
            if (audioObjectType == 29) {
                psPresentFlag = 1;
            }
            extensionSamplingFrequencyIndex = bitReaderBuffer.readBits(4);
            if (extensionSamplingFrequencyIndex == 0xf) extensionSamplingFrequency = bitReaderBuffer.readBits(24);
            audioObjectType = getAudioObjectType(bitReaderBuffer);
            if (audioObjectType == 22) extensionChannelConfiguration = bitReaderBuffer.readBits(4);
        } else {
            extensionAudioObjectType = 0;
        }
        switch(audioObjectType) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 6:
            case 7:
            case 17:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                parseGaSpecificConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, bitReaderBuffer);
                break;
            case 8:
                throw new UnsupportedOperationException("can't parse CelpSpecificConfig yet");
            case 9:
                throw new UnsupportedOperationException("can't parse HvxcSpecificConfig yet");
            case 12:
                throw new UnsupportedOperationException("can't parse TTSSpecificConfig yet");
            case 13:
            case 14:
            case 15:
            case 16:
                throw new UnsupportedOperationException("can't parse StructuredAudioSpecificConfig yet");
            case 24:
                throw new UnsupportedOperationException("can't parse ErrorResilientCelpSpecificConfig yet");
            case 25:
                throw new UnsupportedOperationException("can't parse ErrorResilientHvxcSpecificConfig yet");
            case 26:
            case 27:
                parseParametricSpecificConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, bitReaderBuffer);
                break;
            case 28:
                throw new UnsupportedOperationException("can't parse SSCSpecificConfig yet");
            case 30:
                sacPayloadEmbedding = bitReaderBuffer.readBits(1);
                throw new UnsupportedOperationException("can't parse SpatialSpecificConfig yet");
            case 32:
            case 33:
            case 34:
                throw new UnsupportedOperationException("can't parse MPEG_1_2_SpecificConfig yet");
            case 35:
                throw new UnsupportedOperationException("can't parse DSTSpecificConfig yet");
            case 36:
                fillBits = bitReaderBuffer.readBits(5);
                throw new UnsupportedOperationException("can't parse ALSSpecificConfig yet");
            case 37:
            case 38:
                throw new UnsupportedOperationException("can't parse SLSSpecificConfig yet");
            case 39:
                throw new UnsupportedOperationException("can't parse ELDSpecificConfig yet");
            case 40:
            case 41:
                throw new UnsupportedOperationException("can't parse SymbolicMusicSpecificConfig yet");
            default:
        }
        switch(audioObjectType) {
            case 17:
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case 39:
                epConfig = bitReaderBuffer.readBits(2);
                if (epConfig == 2 || epConfig == 3) {
                    throw new UnsupportedOperationException("can't parse ErrorProtectionSpecificConfig yet");
                }
                if (epConfig == 3) {
                    directMapping = bitReaderBuffer.readBits(1);
                    if (directMapping == 0) {
                        throw new RuntimeException("not implemented");
                    }
                }
        }
        if (extensionAudioObjectType != 5 && bitReaderBuffer.remainingBits() >= 16) {
            syncExtensionType = bitReaderBuffer.readBits(11);
            if (syncExtensionType == 0x2b7) {
                extensionAudioObjectType = getAudioObjectType(bitReaderBuffer);
                if (extensionAudioObjectType == 5) {
                    sbrPresentFlag = bitReaderBuffer.readBits(1);
                    if (sbrPresentFlag == 1) {
                        extensionSamplingFrequencyIndex = bitReaderBuffer.readBits(4);
                        if (extensionSamplingFrequencyIndex == 0xf) {
                            extensionSamplingFrequency = bitReaderBuffer.readBits(24);
                        }
                        if (bitReaderBuffer.remainingBits() >= 12) {
                            syncExtensionType = bitReaderBuffer.readBits(11);
                            if (syncExtensionType == 0x548) {
                                psPresentFlag = bitReaderBuffer.readBits(1);
                            }
                        }
                    }
                }
                if (extensionAudioObjectType == 22) {
                    sbrPresentFlag = bitReaderBuffer.readBits(1);
                    if (sbrPresentFlag == 1) {
                        extensionSamplingFrequencyIndex = bitReaderBuffer.readBits(4);
                        if (extensionSamplingFrequencyIndex == 0xf) {
                            extensionSamplingFrequency = bitReaderBuffer.readBits(24);
                        }
                    }
                    extensionChannelConfiguration = bitReaderBuffer.readBits(4);
                }
            }
        }
    }

    private int gaSpecificConfigSize() {
        return 0;
    }

    public int serializedSize() {
        int out = 4;
        if (audioObjectType == 2) {
            out += gaSpecificConfigSize();
        } else {
            throw new UnsupportedOperationException("can't serialize that yet");
        }
        return out;
    }

    public ByteBuffer serialize() {
        ByteBuffer out = ByteBuffer.allocate(serializedSize());
        IsoTypeWriter.writeUInt8(out, 5);
        IsoTypeWriter.writeUInt8(out, serializedSize() - 2);
        BitWriterBuffer bwb = new BitWriterBuffer(out);
        bwb.writeBits(audioObjectType, 5);
        bwb.writeBits(samplingFrequencyIndex, 4);
        if (samplingFrequencyIndex == 0xf) {
            throw new UnsupportedOperationException("can't serialize that yet");
        }
        bwb.writeBits(channelConfiguration, 4);
        return out;
    }

    private int getAudioObjectType(BitReaderBuffer in) throws IOException {
        int audioObjectType = in.readBits(5);
        if (audioObjectType == 31) {
            audioObjectType = 32 + in.readBits(6);
        }
        return audioObjectType;
    }

    private void parseGaSpecificConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        frameLengthFlag = in.readBits(1);
        dependsOnCoreCoder = in.readBits(1);
        if (dependsOnCoreCoder == 1) {
            coreCoderDelay = in.readBits(14);
        }
        extensionFlag = in.readBits(1);
        if (channelConfiguration == 0) {
            throw new UnsupportedOperationException("can't parse program_config_element yet");
        }
        if ((audioObjectType == 6) || (audioObjectType == 20)) {
            layerNr = in.readBits(3);
        }
        if (extensionFlag == 1) {
            if (audioObjectType == 22) {
                numOfSubFrame = in.readBits(5);
                layer_length = in.readBits(11);
            }
            if (audioObjectType == 17 || audioObjectType == 19 || audioObjectType == 20 || audioObjectType == 23) {
                aacSectionDataResilienceFlag = in.readBits(1);
                aacScalefactorDataResilienceFlag = in.readBits(1);
                aacSpectralDataResilienceFlag = in.readBits(1);
            }
            extensionFlag3 = in.readBits(1);
            if (extensionFlag3 == 1) {
            }
        }
        gaSpecificConfig = true;
    }

    private void parseParametricSpecificConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        isBaseLayer = in.readBits(1);
        if (isBaseLayer == 1) {
            parseParaConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        } else {
            parseHilnEnexConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        }
    }

    private void parseParaConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        paraMode = in.readBits(2);
        if (paraMode != 1) {
            parseErHvxcConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        }
        if (paraMode != 0) {
            parseHilnConfig(samplingFrequencyIndex, channelConfiguration, audioObjectType, in);
        }
        paraExtensionFlag = in.readBits(1);
        parametricSpecificConfig = true;
    }

    private void parseErHvxcConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        hvxcVarMode = in.readBits(1);
        hvxcRateMode = in.readBits(2);
        erHvxcExtensionFlag = in.readBits(1);
        if (erHvxcExtensionFlag == 1) {
            var_ScalableFlag = in.readBits(1);
        }
    }

    private void parseHilnConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        hilnQuantMode = in.readBits(1);
        hilnMaxNumLine = in.readBits(8);
        hilnSampleRateCode = in.readBits(4);
        hilnFrameLength = in.readBits(12);
        hilnContMode = in.readBits(2);
    }

    private void parseHilnEnexConfig(int samplingFrequencyIndex, int channelConfiguration, int audioObjectType, BitReaderBuffer in) throws IOException {
        hilnEnhaLayer = in.readBits(1);
        if (hilnEnhaLayer == 1) {
            hilnEnhaQuantMode = in.readBits(2);
        }
    }

    public ByteBuffer getConfigBytes() {
        return configBytes;
    }

    public int getAudioObjectType() {
        return audioObjectType;
    }

    public int getExtensionAudioObjectType() {
        return extensionAudioObjectType;
    }

    public int getSbrPresentFlag() {
        return sbrPresentFlag;
    }

    public int getPsPresentFlag() {
        return psPresentFlag;
    }

    public void setAudioObjectType(int audioObjectType) {
        this.audioObjectType = audioObjectType;
    }

    public void setSamplingFrequencyIndex(int samplingFrequencyIndex) {
        this.samplingFrequencyIndex = samplingFrequencyIndex;
    }

    public void setSamplingFrequency(int samplingFrequency) {
        this.samplingFrequency = samplingFrequency;
    }

    public void setChannelConfiguration(int channelConfiguration) {
        this.channelConfiguration = channelConfiguration;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AudioSpecificConfig");
        sb.append("{configBytes=").append(configBytes);
        sb.append(", audioObjectType=").append(audioObjectType).append(" (").append(audioObjectTypeMap.get(audioObjectType)).append(")");
        sb.append(", samplingFrequencyIndex=").append(samplingFrequencyIndex).append(" (").append(samplingFrequencyIndexMap.get(samplingFrequencyIndex)).append(")");
        sb.append(", samplingFrequency=").append(samplingFrequency);
        sb.append(", channelConfiguration=").append(channelConfiguration);
        if (extensionAudioObjectType > 0) {
            sb.append(", extensionAudioObjectType=").append(extensionAudioObjectType).append(" (").append(audioObjectTypeMap.get(extensionAudioObjectType)).append(")");
            sb.append(", sbrPresentFlag=").append(sbrPresentFlag);
            sb.append(", psPresentFlag=").append(psPresentFlag);
            sb.append(", extensionSamplingFrequencyIndex=").append(extensionSamplingFrequencyIndex).append(" (").append(samplingFrequencyIndexMap.get(extensionSamplingFrequencyIndex)).append(")");
            sb.append(", extensionSamplingFrequency=").append(extensionSamplingFrequency);
            sb.append(", extensionChannelConfiguration=").append(extensionChannelConfiguration);
        }
        sb.append(", syncExtensionType=").append(syncExtensionType);
        if (gaSpecificConfig) {
            sb.append(", frameLengthFlag=").append(frameLengthFlag);
            sb.append(", dependsOnCoreCoder=").append(dependsOnCoreCoder);
            sb.append(", coreCoderDelay=").append(coreCoderDelay);
            sb.append(", extensionFlag=").append(extensionFlag);
            sb.append(", layerNr=").append(layerNr);
            sb.append(", numOfSubFrame=").append(numOfSubFrame);
            sb.append(", layer_length=").append(layer_length);
            sb.append(", aacSectionDataResilienceFlag=").append(aacSectionDataResilienceFlag);
            sb.append(", aacScalefactorDataResilienceFlag=").append(aacScalefactorDataResilienceFlag);
            sb.append(", aacSpectralDataResilienceFlag=").append(aacSpectralDataResilienceFlag);
            sb.append(", extensionFlag3=").append(extensionFlag3);
        }
        if (parametricSpecificConfig) {
            sb.append(", isBaseLayer=").append(isBaseLayer);
            sb.append(", paraMode=").append(paraMode);
            sb.append(", paraExtensionFlag=").append(paraExtensionFlag);
            sb.append(", hvxcVarMode=").append(hvxcVarMode);
            sb.append(", hvxcRateMode=").append(hvxcRateMode);
            sb.append(", erHvxcExtensionFlag=").append(erHvxcExtensionFlag);
            sb.append(", var_ScalableFlag=").append(var_ScalableFlag);
            sb.append(", hilnQuantMode=").append(hilnQuantMode);
            sb.append(", hilnMaxNumLine=").append(hilnMaxNumLine);
            sb.append(", hilnSampleRateCode=").append(hilnSampleRateCode);
            sb.append(", hilnFrameLength=").append(hilnFrameLength);
            sb.append(", hilnContMode=").append(hilnContMode);
            sb.append(", hilnEnhaLayer=").append(hilnEnhaLayer);
            sb.append(", hilnEnhaQuantMode=").append(hilnEnhaQuantMode);
        }
        sb.append('}');
        return sb.toString();
    }

    static {
        samplingFrequencyIndexMap.put(0x0, 96000);
        samplingFrequencyIndexMap.put(0x1, 88200);
        samplingFrequencyIndexMap.put(0x2, 64000);
        samplingFrequencyIndexMap.put(0x3, 48000);
        samplingFrequencyIndexMap.put(0x4, 44100);
        samplingFrequencyIndexMap.put(0x5, 32000);
        samplingFrequencyIndexMap.put(0x6, 24000);
        samplingFrequencyIndexMap.put(0x7, 22050);
        samplingFrequencyIndexMap.put(0x8, 16000);
        samplingFrequencyIndexMap.put(0x9, 12000);
        samplingFrequencyIndexMap.put(0xa, 11025);
        samplingFrequencyIndexMap.put(0xb, 8000);
        audioObjectTypeMap.put(1, "AAC main");
        audioObjectTypeMap.put(2, "AAC LC");
        audioObjectTypeMap.put(3, "AAC SSR");
        audioObjectTypeMap.put(4, "AAC LTP");
        audioObjectTypeMap.put(5, "SBR");
        audioObjectTypeMap.put(6, "AAC Scalable");
        audioObjectTypeMap.put(7, "TwinVQ");
        audioObjectTypeMap.put(8, "CELP");
        audioObjectTypeMap.put(9, "HVXC");
        audioObjectTypeMap.put(10, "(reserved)");
        audioObjectTypeMap.put(11, "(reserved)");
        audioObjectTypeMap.put(12, "TTSI");
        audioObjectTypeMap.put(13, "Main synthetic");
        audioObjectTypeMap.put(14, "Wavetable synthesis");
        audioObjectTypeMap.put(15, "General MIDI");
        audioObjectTypeMap.put(16, "Algorithmic Synthesis and Audio FX");
        audioObjectTypeMap.put(17, "ER AAC LC");
        audioObjectTypeMap.put(18, "(reserved)");
        audioObjectTypeMap.put(19, "ER AAC LTP");
        audioObjectTypeMap.put(20, "ER AAC Scalable");
        audioObjectTypeMap.put(21, "ER TwinVQ");
        audioObjectTypeMap.put(22, "ER BSAC");
        audioObjectTypeMap.put(23, "ER AAC LD");
        audioObjectTypeMap.put(24, "ER CELP");
        audioObjectTypeMap.put(25, "ER HVXC");
        audioObjectTypeMap.put(26, "ER HILN");
        audioObjectTypeMap.put(27, "ER Parametric");
        audioObjectTypeMap.put(28, "SSC");
        audioObjectTypeMap.put(29, "PS");
        audioObjectTypeMap.put(30, "MPEG Surround");
        audioObjectTypeMap.put(31, "(escape)");
        audioObjectTypeMap.put(32, "Layer-1");
        audioObjectTypeMap.put(33, "Layer-2");
        audioObjectTypeMap.put(34, "Layer-3");
        audioObjectTypeMap.put(35, "DST");
        audioObjectTypeMap.put(36, "ALS");
        audioObjectTypeMap.put(37, "SLS");
        audioObjectTypeMap.put(38, "SLS non-core");
        audioObjectTypeMap.put(39, "ER AAC ELD");
        audioObjectTypeMap.put(40, "SMR Simple");
        audioObjectTypeMap.put(41, "SMR Main");
    }

    public int getSamplingFrequency() {
        return samplingFrequencyIndex == 0xf ? samplingFrequency : samplingFrequencyIndexMap.get(samplingFrequencyIndex);
    }

    public int getChannelConfiguration() {
        return channelConfiguration;
    }
}
