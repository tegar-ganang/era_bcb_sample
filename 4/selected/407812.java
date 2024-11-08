package org.tritonus.sampled.mixer.alsa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import org.tritonus.share.TDebug;
import org.tritonus.share.TSettings;
import org.tritonus.share.sampled.mixer.TMixer;
import org.tritonus.share.sampled.mixer.TMixerInfo;
import org.tritonus.share.sampled.mixer.TSoftClip;
import org.tritonus.share.GlobalInfo;
import org.tritonus.lowlevel.alsa.Alsa;
import org.tritonus.lowlevel.alsa.AlsaPcm;
import org.tritonus.lowlevel.alsa.AlsaPcmHWParams;
import org.tritonus.lowlevel.alsa.AlsaPcmHWParamsFormatMask;

public class AlsaDataLineMixer extends TMixer {

    private static final AudioFormat[] EMPTY_AUDIOFORMAT_ARRAY = new AudioFormat[0];

    private static final int CHANNELS_LIMIT = 32;

    private static final int DEFAULT_BUFFER_SIZE = 32768;

    private String m_strPcmName;

    public static String getDeviceNamePrefix() {
        if (TSettings.AlsaUsePlughw) {
            return "plughw";
        } else {
            return "hw";
        }
    }

    public static String getPcmName(int nCard) {
        String strPcmName = getDeviceNamePrefix() + ":" + nCard;
        if (TSettings.AlsaUsePlughw) {
        }
        return strPcmName;
    }

    public AlsaDataLineMixer() {
        this(0);
    }

    public AlsaDataLineMixer(int nCard) {
        this(getPcmName(nCard));
    }

    public AlsaDataLineMixer(String strPcmName) {
        super(new TMixerInfo("Alsa DataLine Mixer (" + strPcmName + ")", GlobalInfo.getVendor(), "Mixer for the Advanced Linux Sound Architecture (card " + strPcmName + ")", GlobalInfo.getVersion()), new Line.Info(Mixer.class));
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.<init>(String): begin.");
        }
        m_strPcmName = strPcmName;
        List<AudioFormat> sourceFormats = getSupportedFormats(AlsaPcm.SND_PCM_STREAM_PLAYBACK);
        List<AudioFormat> targetFormats = getSupportedFormats(AlsaPcm.SND_PCM_STREAM_CAPTURE);
        List<Line.Info> sourceLineInfos = new ArrayList<Line.Info>();
        Line.Info sourceLineInfo = new DataLine.Info(SourceDataLine.class, sourceFormats.toArray(EMPTY_AUDIOFORMAT_ARRAY), AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
        sourceLineInfos.add(sourceLineInfo);
        List<Line.Info> targetLineInfos = new ArrayList<Line.Info>();
        Line.Info targetLineInfo = new DataLine.Info(TargetDataLine.class, targetFormats.toArray(EMPTY_AUDIOFORMAT_ARRAY), AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED);
        targetLineInfos.add(targetLineInfo);
        setSupportInformation(sourceFormats, targetFormats, sourceLineInfos, targetLineInfos);
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.<init>(String): end.");
        }
    }

    public String getPcmName() {
        return m_strPcmName;
    }

    public void open() {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.open(): begin");
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.open(): end");
        }
    }

    public void close() {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.close(): begin");
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.close(): end");
        }
    }

    public int getMaxLines(Line.Info info) {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getMaxLines(): begin");
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getMaxLines(): end");
        }
        return 0;
    }

    protected SourceDataLine getSourceDataLine(AudioFormat format, int nBufferSize) throws LineUnavailableException {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSourceDataLine(): begin");
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSourceDataLine(): format: " + format);
            TDebug.out("AlsaDataLineMixer.getSourceDataLine(): buffer size: " + nBufferSize);
        }
        if (nBufferSize < 1) {
            nBufferSize = DEFAULT_BUFFER_SIZE;
        }
        AlsaSourceDataLine sourceDataLine = new AlsaSourceDataLine(this, format, nBufferSize);
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSourceDataLine(): returning: " + sourceDataLine);
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSourceDataLine(): end");
        }
        return sourceDataLine;
    }

    protected TargetDataLine getTargetDataLine(AudioFormat format, int nBufferSize) throws LineUnavailableException {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getTargetDataLine(): begin");
        }
        int nBufferSizeInBytes = nBufferSize * format.getFrameSize();
        AlsaTargetDataLine targetDataLine = new AlsaTargetDataLine(this, format, nBufferSizeInBytes);
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getTargetDataLine(): returning: " + targetDataLine);
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getTargetDataLine(): end");
        }
        return targetDataLine;
    }

    protected Clip getClip(AudioFormat format) throws LineUnavailableException {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getClip(): begin");
        }
        Clip clip = new TSoftClip(this, format);
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getClip(): end");
        }
        return clip;
    }

    private List<AudioFormat> getSupportedFormats(int nDirection) {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSupportedFormats(): begin");
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSupportedFormats(): direction: " + nDirection);
        }
        List<AudioFormat> supportedFormats = new ArrayList<AudioFormat>();
        AlsaPcm alsaPcm = null;
        try {
            alsaPcm = new AlsaPcm(getPcmName(), nDirection, 0);
        } catch (Exception e) {
            if (TDebug.TraceAllExceptions) {
                TDebug.out(e);
            }
            throw new RuntimeException("cannot open pcm");
        }
        int nReturn;
        AlsaPcmHWParams hwParams = new AlsaPcmHWParams();
        nReturn = alsaPcm.getAnyHWParams(hwParams);
        if (nReturn != 0) {
            TDebug.out("AlsaDataLineMixer.getSupportedFormats(): getAnyHWParams(): " + Alsa.getStringError(nReturn));
            throw new RuntimeException(Alsa.getStringError(nReturn));
        }
        AlsaPcmHWParamsFormatMask formatMask = new AlsaPcmHWParamsFormatMask();
        int nMinChannels = hwParams.getChannelsMin();
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSupportedFormats(): min channels: " + nMinChannels);
        }
        int nMaxChannels = hwParams.getChannelsMax();
        nMaxChannels = Math.min(nMaxChannels, CHANNELS_LIMIT);
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSupportedFormats(): max channels: " + nMaxChannels);
        }
        hwParams.getFormatMask(formatMask);
        for (int i = 0; i < 32; i++) {
            if (TDebug.TraceMixer) {
                TDebug.out("AlsaDataLineMixer.getSupportedFormats(): checking ALSA format index: " + i);
            }
            if (formatMask.test(i)) {
                if (TDebug.TraceMixer) {
                    TDebug.out("AlsaDataLineMixer.getSupportedFormats(): ...supported");
                }
                AudioFormat audioFormat = AlsaUtils.getAlsaFormat(i);
                if (TDebug.TraceMixer) {
                    TDebug.out("AlsaDataLineMixer.getSupportedFormats(): adding AudioFormat: " + audioFormat);
                }
                addChanneledAudioFormats(supportedFormats, audioFormat, nMinChannels, nMaxChannels);
            } else {
                if (TDebug.TraceMixer) {
                    TDebug.out("AlsaDataLineMixer.getSupportedFormats(): ...not supported");
                }
            }
        }
        alsaPcm.close();
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.getSupportedFormats(): end");
        }
        return supportedFormats;
    }

    private static void addChanneledAudioFormats(Collection<AudioFormat> collection, AudioFormat protoAudioFormat, int nMinChannels, int nMaxChannels) {
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.addChanneledAudioFormats(): begin");
        }
        for (int nChannels = nMinChannels; nChannels <= nMaxChannels; nChannels++) {
            AudioFormat channeledAudioFormat = getChanneledAudioFormat(protoAudioFormat, nChannels);
            if (TDebug.TraceMixer) {
                TDebug.out("AlsaDataLineMixer.addChanneledAudioFormats(): adding AudioFormat: " + channeledAudioFormat);
            }
            collection.add(channeledAudioFormat);
        }
        if (TDebug.TraceMixer) {
            TDebug.out("AlsaDataLineMixer.addChanneledAudioFormats(): end");
        }
    }

    private static AudioFormat getChanneledAudioFormat(AudioFormat audioFormat, int nChannels) {
        AudioFormat channeledAudioFormat = new AudioFormat(audioFormat.getEncoding(), audioFormat.getSampleRate(), audioFormat.getSampleSizeInBits(), nChannels, (audioFormat.getSampleSizeInBits() / 8) * nChannels, audioFormat.getFrameRate(), audioFormat.isBigEndian());
        return channeledAudioFormat;
    }
}
