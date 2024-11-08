package org.jsresources.apps.jam.audio;

import javax.sound.sampled.AudioFormat;
import java.net.URL;
import org.jsresources.apps.jmvp.manager.RM;
import org.jsresources.apps.jam.Debug;
import org.tritonus.share.sampled.FloatSampleBuffer;

public class Metronome {

    private static final float VOLUME_OFFBEAT = 0.1f;

    private static final float VOLUME_DOWNBEAT = 0.2f;

    private static final float VOLUME_FIRSTBEAT = 0.5f;

    private String m_strCannotGetSoundText = RM.getResourceString("metronome.cannotGetSoundText");

    private AudioFormat m_audioFormat;

    private int m_nBeatsPerMinute;

    private int m_nBeatsPerMeasure;

    private int m_nMeasures;

    private static FloatSampleBuffer m_tock = null;

    private FloatSampleBuffer m_loop = null;

    public Metronome() throws Exception {
        m_audioFormat = AudioQualityModel.getInstance().getPlaybackFormat();
        if (m_tock == null) {
            String strTockUrl = RM.getResourceString("metronome.tock", false);
            URL tockURL = null;
            if (strTockUrl != null) {
                tockURL = Thread.currentThread().getContextClassLoader().getResource(strTockUrl);
            }
            if (tockURL != null) {
                byte[] data = AudioUtils.getByteArrayFromURL(tockURL, m_audioFormat);
                m_tock = new FloatSampleBuffer(data, 0, data.length, m_audioFormat);
            } else {
                throw new Exception(m_strCannotGetSoundText);
            }
        }
        m_nBeatsPerMinute = 0;
        m_nBeatsPerMeasure = 0;
        m_nMeasures = 0;
    }

    public void setProperties(int nBeatsPerMinute, int nBeatsPerMeasure, int nMeasures) {
        if (m_nBeatsPerMinute != nBeatsPerMinute || m_nBeatsPerMeasure != nBeatsPerMeasure || m_nMeasures != nMeasures) {
            if (Debug.getTracePlay()) Debug.out("Metronome.setProperties(" + nBeatsPerMinute + "bpm, " + nBeatsPerMeasure + "beats/measure, " + nMeasures + " measures)");
            m_nBeatsPerMinute = nBeatsPerMinute;
            m_nBeatsPerMeasure = nBeatsPerMeasure;
            m_nMeasures = nMeasures;
            if (m_loop != null) {
                recalculate();
            }
        }
    }

    public int getBeatsPerMinute() {
        return m_nBeatsPerMinute;
    }

    public int getBeatsPerMeasure() {
        return m_nBeatsPerMeasure;
    }

    public int getMeasures() {
        return m_nMeasures;
    }

    private void recalculate() {
        double sampleRate = m_audioFormat.getSampleRate();
        double bpm = getBeatsPerMinute();
        int beatsPerMeasure = getBeatsPerMeasure();
        int totalBeats = beatsPerMeasure * getMeasures();
        if (m_loop == null) {
            m_loop = new FloatSampleBuffer();
        }
        if (beatsPerMeasure <= 0 || getMeasures() <= 0 || bpm <= 0) {
            m_loop.reset(1, 0, (float) sampleRate);
            return;
        }
        double durationInSeconds = 60.0 / bpm * totalBeats;
        int sampleCount = (int) (sampleRate * durationInSeconds);
        if (Debug.getTracePlay()) Debug.out("Metronome.recalculate: sampleCount = " + sampleCount);
        m_loop.reset(1, sampleCount, (float) sampleRate);
        m_loop.makeSilence();
        float[] data = m_loop.getChannel(0);
        int tockDataLen = m_tock.getSampleCount();
        float[] tockData = m_tock.getChannel(0);
        for (int beat = 0; beat < totalBeats; beat++) {
            int samplePos = beat * sampleCount / totalBeats;
            float vol = VOLUME_OFFBEAT;
            if ((beat % beatsPerMeasure) == 0) {
                vol = VOLUME_DOWNBEAT;
                if (beat == 0) {
                    vol = VOLUME_FIRSTBEAT;
                }
            }
            mix(tockData, tockDataLen, data, samplePos, sampleCount, vol);
        }
    }

    private void mix(float[] src, int srcLen, float[] dest, int destOffset, int destLen, float vol) {
        if (destOffset + srcLen > destLen) {
            srcLen = destLen - destOffset;
        }
        for (int i = 0; i < srcLen; i++) {
            dest[destOffset + i] += vol * src[i];
        }
    }

    public FloatSampleBuffer getTockBuffer() {
        if (m_loop == null) {
            recalculate();
        }
        return m_loop;
    }
}
