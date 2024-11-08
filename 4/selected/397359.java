package org.jsresources.apps.jsinfo;

import javax.swing.table.AbstractTableModel;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

public abstract class AudioFormatTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = { "Encoding", "Sample Rate", "Sample Size (bits)", "Channels", "Frame Size (bytes)", "Frame Rate", "Endianess" };

    protected static final AudioFormat[] EMPTY_AUDIOFORMAT_ARRAY = new AudioFormat[0];

    protected abstract AudioFormat[] getAudioFormats();

    public int getRowCount() {
        return getAudioFormats().length;
    }

    private String[] getColumnArray() {
        return COLUMN_NAMES;
    }

    public int getColumnCount() {
        return getColumnArray().length;
    }

    public Class getColumnClass(int nColumn) {
        return String.class;
    }

    public String getColumnName(int nColumn) {
        return getColumnArray()[nColumn];
    }

    public Object getValueAt(int nRow, int nColumn) {
        AudioFormat format = getAudioFormats()[nRow];
        switch(nColumn) {
            case 0:
                return format.getEncoding().toString();
            case 1:
                float fSampleRate = format.getSampleRate();
                return (fSampleRate == AudioSystem.NOT_SPECIFIED) ? "any" : Float.toString(fSampleRate);
            case 2:
                int nSampleSize = format.getSampleSizeInBits();
                return (nSampleSize == AudioSystem.NOT_SPECIFIED) ? "any" : Integer.toString(nSampleSize);
            case 3:
                int nChannels = format.getChannels();
                return (nChannels == AudioSystem.NOT_SPECIFIED) ? "any" : Integer.toString(nChannels);
            case 4:
                int nFrameSize = format.getFrameSize();
                return (nFrameSize == AudioSystem.NOT_SPECIFIED) ? "any" : Integer.toString(nFrameSize);
            case 5:
                float fFrameRate = format.getFrameRate();
                return (fFrameRate == AudioSystem.NOT_SPECIFIED) ? "any" : Float.toString(fFrameRate);
            case 6:
                return format.isBigEndian() ? "big" : "little";
            default:
                return null;
        }
    }
}
