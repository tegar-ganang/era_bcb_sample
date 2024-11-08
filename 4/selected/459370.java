package org.jpedal.fonts.tt;

import org.jpedal.utils.LogWriter;

public class Hmtx extends Table {

    private int[] hMetrics;

    private short[] leftSideBearing;

    float scaling = 1f / 1000f;

    public Hmtx(FontFile2 currentFontFile, int glyphCount, int metricsCount, int maxAdvance) {
        scaling = (float) maxAdvance;
        LogWriter.writeMethod("{readHmtxTable}", 0);
        if (metricsCount < 0) metricsCount = -metricsCount;
        int startPointer = currentFontFile.selectTable(FontFile2.HMTX);
        int lsbCount = glyphCount - metricsCount;
        hMetrics = new int[glyphCount];
        leftSideBearing = new short[glyphCount];
        int currentMetric = 0;
        if (startPointer == 0) LogWriter.writeLog("No Htmx table found"); else if (lsbCount < 0) {
            LogWriter.writeLog("Invalid Htmx table found");
        } else {
            int i = 0;
            for (i = 0; i < metricsCount; i++) {
                currentMetric = currentFontFile.getNextUint16();
                hMetrics[i] = currentMetric;
                leftSideBearing[i] = currentFontFile.getNextInt16();
            }
            int tableLength = currentFontFile.getTableSize(FontFile2.HMTX);
            int lsbBytes = tableLength - (i * 4);
            lsbCount = (lsbBytes / 2);
            for (int j = i; j < lsbCount; j++) {
                hMetrics[j] = currentMetric;
                leftSideBearing[j] = currentFontFile.getFWord();
            }
        }
    }

    public short getRAWLSB(int i) {
        if (leftSideBearing == null || i >= leftSideBearing.length) return 0; else return leftSideBearing[i];
    }

    public short getLeftSideBearing(int i) {
        if (i < hMetrics.length) {
            return (short) (hMetrics[i] & 0xffff);
        } else if (leftSideBearing == null) {
            return 0;
        } else {
            try {
                return leftSideBearing[i - hMetrics.length];
            } catch (Exception e) {
                return 0;
            }
        }
    }

    public float getAdvanceWidth(int i) {
        return ((hMetrics[i] - getLeftSideBearing(i)) / scaling);
    }

    public float getWidth(int i) {
        float w = hMetrics[i];
        return ((w) / scaling);
    }

    public float getUnscaledWidth(int i) {
        return hMetrics[i];
    }
}
