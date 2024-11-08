package elaborazione.operativo.colospace;

import elaborazione.operativo.tables.CHDEntry;
import it.tidalwave.imageio.io.RAWImageInputStream;
import java.io.IOException;
import javax.imageio.stream.ImageInputStream;

/**
 *
 * @author luca
 */
public class RGB implements LSColorSpace {

    /** Creates a new instance of RGB */
    public RGB() {
    }

    public int getChannelsNumber() {
        return 3;
    }

    public int getCodifica() {
        return LSColorSpace.RGB;
    }

    public void convertToRGB(int[] raster) {
    }

    public void convertToRGB(int[][] raster) {
    }

    public void convertFromRgb(int[] rasterRGB) {
    }

    public void convertFromRgb(int[][] raster) {
    }

    public int getVerticalSubSampleFactor(int channel) {
        return 1;
    }

    public int getHorizontalSubSampleFactor(int channel) {
        return 1;
    }

    public int[][] convertFromRgb(ImageInputStream iis, int w, int h) throws IOException {
        return null;
    }

    public int[][] getDCTable(int channel) {
        return null;
    }

    public int[][][] getACTable(int channel) {
        return null;
    }

    public int getMaxValue(int channel) {
        return 255;
    }

    public CHDEntry[] getDCCHDTable(int channel) {
        return null;
    }

    public CHDEntry[] getACCHDTable(int channel) {
        return null;
    }

    public int getDCMinCodeLen(int channel) {
        return -1;
    }

    public int getACMinCodeLen(int channel) {
        return -1;
    }

    public int[][] getQuantizationMatrix(int channel, int quality) {
        return null;
    }

    public int getBitsPerChannel() {
        return 8;
    }

    public int getVerticalInit(int channel) {
        return 0;
    }

    public int getChannelOffset(int channel) {
        return 8 * (3 - channel);
    }

    public String getCodificaAsString() {
        return LSColorSpace.RGB_NAME;
    }

    public void convertToRGB(ImageInputStream iis, int w, int h, int[][] data) throws IOException {
        RAWImageInputStream ris = new RAWImageInputStream(iis);
        ris.selectBitReader(-1, 262144);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; ) {
                data[i][j++] = (int) ris.readBits(24);
            }
        }
    }

    public String toString() {
        return "RGB";
    }
}
