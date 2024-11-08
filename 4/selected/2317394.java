package silence.format.xm.data;

import java.io.*;
import java.net.URL;

/**
 * Handles a modules data
 *
 * @author Fredrik Ehnbom
 */
public class Module {

    private String title = "";

    private int patternOrder[];

    private int restartPosition;

    private boolean amigaFreqTable = false;

    private int tempo = 0;

    private int bpm = 0;

    private Pattern[] pattern;

    private Instrument[] instrument;

    private int channelCount;

    public void setAmigaFreqTable(boolean b) {
        amigaFreqTable = b;
    }

    public boolean getAmigaFreqTable() {
        return amigaFreqTable;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setChannelCount(int c) {
        channelCount = c;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setPatternOrder(int[] order) {
        patternOrder = order;
    }

    public int[] getPatternOrder() {
        return patternOrder;
    }

    public void setRestartPosition(int r) {
        restartPosition = r;
    }

    public int getRestartPosition() {
        return restartPosition;
    }

    public void setPatterns(Pattern[] pat) {
        pattern = pat;
    }

    public Pattern[] getPatterns() {
        return pattern;
    }

    public void setInstruments(Instrument[] i) {
        instrument = i;
    }

    public Instrument[] getInstruments() {
        return instrument;
    }

    public Instrument getInstrument(int idx) {
        if (idx < instrument.length) return instrument[idx];
        return null;
    }

    public void setBpm(int b) {
        bpm = b;
    }

    public int getBpm() {
        return bpm;
    }

    public void setTempo(int t) {
        tempo = t;
    }

    public int getTempo() {
        return tempo;
    }
}
