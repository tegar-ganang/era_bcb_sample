package elaborazione.operativo.colospace;

import elaborazione.operativo.tables.LSQuantizer;
import elaborazione.operativo.tables.LSQuantizer;
import elaborazione.operativo.tables.CHDEntry;
import elaborazione.operativo.tables.LSTables;
import it.tidalwave.imageio.io.RAWImageInputStream;
import java.io.IOException;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author Luca
 */
public final class GrayScale implements LSColorSpace {

    public int getChannelsNumber() {
        return 1;
    }

    public int getCodifica() {
        return LSColorSpace.GRY;
    }

    public void convertFromRgb(int[] rasterRGB) {
    }

    public void convertFromRgb(int[][] rasterRGB) {
    }

    public void convertToRGB(int[][] raster) {
    }

    public void convertToRGB(int[] raster) {
    }

    public int getVerticalSubSampleFactor(int channel) {
        return 1;
    }

    public int getHorizontalSubSampleFactor(int channel) {
        return 1;
    }

    public int[][] convertFromRgb(ImageInputStream iis, int w, int h) throws IOException {
        int[][] raster = new int[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                raster[i][j] = 0xff & iis.readByte();
            }
        }
        return raster;
    }

    public int[][] getDCTable(int channel) {
        return LSTables.LUM_DC_TABLE;
    }

    public int[][][] getACTable(int channel) {
        return LSTables.LUM_AC_TABLE;
    }

    public int getMaxValue(int channel) {
        return 255;
    }

    public CHDEntry[] getDCCHDTable(int channel) {
        return LSTables.LUM_DC_CHD;
    }

    public CHDEntry[] getACCHDTable(int channel) {
        return LSTables.LUM_AC_CHD;
    }

    public int getDCMinCodeLen(int channel) {
        return LSTables.LUM_DC_MIN_CODEWORD_LEN;
    }

    public int getACMinCodeLen(int channel) {
        return LSTables.LUM_AC_MIN_CODEWORD_LEN;
    }

    public int[][] getQuantizationMatrix(int channel, int quality) {
        return LSQuantizer.getLuminanceQM(quality);
    }

    public int getBitsPerChannel() {
        return 8;
    }

    public int getVerticalInit(int channel) {
        return 0;
    }

    public int getChannelOffset(int channel) {
        return 0;
    }

    public String getCodificaAsString() {
        return LSColorSpace.GRY_NAME;
    }

    public void convertToRGB(ImageInputStream iis, int w, int h, int[][] data) throws IOException {
        RAWImageInputStream ris = new RAWImageInputStream(iis);
        ris.selectBitReader(-1, 262144);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; ) {
                data[i][j++] = (int) ris.readBits(8);
            }
        }
    }

    public String toString() {
        return "GRY";
    }
}
