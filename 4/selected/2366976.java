package org.jpedal.fonts.tt;

import org.jpedal.utils.LogWriter;

public class Hhea extends Table {

    private int version;

    private short ascender;

    private short descender;

    private short lineGap;

    private short advanceWidthMax;

    private short minLeftSideBearing;

    private short minRightSideBearing;

    private short xMaxExtent;

    private short caretSlopeRise;

    private short caretSlopeRun;

    private short caretOffset;

    private short metricDataFormat;

    private int numberOfHMetrics;

    public Hhea(FontFile2 currentFontFile) {
        LogWriter.writeMethod("{readHheaTable}", 0);
        int startPointer = currentFontFile.selectTable(FontFile2.HHEA);
        if (startPointer != 0) {
            version = currentFontFile.getNextUint32();
            ascender = currentFontFile.getFWord();
            descender = currentFontFile.getFWord();
            lineGap = currentFontFile.getFWord();
            advanceWidthMax = currentFontFile.readUFWord();
            minLeftSideBearing = currentFontFile.getFWord();
            minRightSideBearing = currentFontFile.getFWord();
            xMaxExtent = currentFontFile.getFWord();
            caretSlopeRise = currentFontFile.getNextInt16();
            caretSlopeRun = currentFontFile.getNextInt16();
            caretOffset = currentFontFile.getFWord();
            for (int i = 0; i < 4; i++) currentFontFile.getNextUint16();
            metricDataFormat = currentFontFile.getNextInt16();
            numberOfHMetrics = currentFontFile.getNextUint16();
        }
    }

    public int getNumberOfHMetrics() {
        return numberOfHMetrics;
    }
}
