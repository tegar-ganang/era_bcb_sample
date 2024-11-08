package elaborazione.operativo.colospace;

import elaborazione.operativo.tables.LSQuantizer;
import elaborazione.operativo.MathUtil;
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
public class YUV implements LSColorSpace {

    public int getChannelsNumber() {
        return 3;
    }

    public int getCodifica() {
        return LSColorSpace.YUV;
    }

    public void convertToRGB(int[] raster) {
        int len = raster.length;
        for (int i = 0; i < len; i++) {
            raster[i] = fromYUVtoRGB(raster[i]);
        }
    }

    public void convertToRGB(int[][] raster) {
        int len = raster.length;
        for (int i = 0; i < len; i++) {
            convertToRGB(raster[i]);
        }
    }

    public void convertFromRgb(int[] raster) {
        int len = raster.length;
        for (int i = 0; i < len; i++) {
            raster[i] = fromRGBtoYUV(raster[i]);
        }
    }

    public void convertFromRgb(int[][] rasterRGB) {
        int h = rasterRGB.length, w = rasterRGB[0].length;
        int[][] raster = new int[h][w];
        for (int i = 0; i < h; i++) {
            convertFromRgb(rasterRGB[i]);
        }
    }

    public int[][] convertFromRgb(ImageInputStream iis, int w, int h) throws IOException {
        int[][] raster = new int[h][w];
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; ) {
                raster[i][j++] = fromRGBtoYUV((int) iis.readBits(24));
            }
        }
        return raster;
    }

    public int getVerticalSubSampleFactor(int channel) {
        return 1;
    }

    public int getHorizontalSubSampleFactor(int channel) {
        return 1;
    }

    public int[][] getDCTable(int channel) {
        int[][] dcTable = null;
        switch(channel) {
            case 1:
                dcTable = LSTables.LUM_DC_TABLE;
                break;
            case 2:
            case 3:
                dcTable = LSTables.CHROM_DC_TABLE;
                break;
        }
        return dcTable;
    }

    public int[][][] getACTable(int channel) {
        return LSTables.LUM_AC_TABLE;
    }

    public CHDEntry[] getDCCHDTable(int channel) {
        CHDEntry[] table = null;
        switch(channel) {
            case 1:
                table = LSTables.LUM_DC_CHD;
                break;
            case 2:
            case 3:
                table = LSTables.CHROM_DC_CHD;
        }
        return table;
    }

    public CHDEntry[] getACCHDTable(int channel) {
        return LSTables.LUM_AC_CHD;
    }

    public int getMaxValue(int channel) {
        int max = 0;
        switch(channel) {
            case 1:
                max = 235;
                break;
            case 2:
            case 3:
                max = 240;
                break;
        }
        return max;
    }

    public static int fromYUVtoRGB(int pixel) {
        int C = ((pixel >>> 16) & 0xff) - 16;
        int D = ((pixel >>> 8) & 0xff) - 128;
        int E = (pixel & 0xff) - 128;
        int r = (298 * C + 409 * E + 128) >> 8;
        int g = (298 * C - 100 * D - 208 * E + 128) >> 8;
        int b = (298 * C + 516 * D + 128) >> 8;
        if (r < 0) {
            r = 0;
        } else if (r > 255) {
            r = 255;
        }
        if (g < 0) {
            g = 0;
        } else if (g > 255) {
            g = 255;
        }
        if (b < 0) {
            b = 0;
        } else if (b > 255) {
            b = 255;
        }
        return (r << 16) | (g << 8) | (b);
    }

    public static int fromRGBtoYUV(int rgb) {
        int r = (rgb >>> 16) & 0xff, g = (rgb >>> 8) & 0xff, b = rgb & 0xff;
        int yuv = (Math.min(MathUtil.abs(r * 2104 + g * 4130 + b * 802 + 135168) >> 13, 235)) << 16;
        yuv |= (Math.min(MathUtil.abs(r * -1214 + g * -2384 + b * 3598 + 1052672) >> 13, 240)) << 8;
        return yuv | Math.min(MathUtil.abs(r * 3598 + g * -3013 + b * -585 + 1052672) >> 13, 240);
    }

    public int getDCMinCodeLen(int channel) {
        int len = 0;
        switch(channel) {
            case 1:
                len = LSTables.LUM_DC_MIN_CODEWORD_LEN;
                break;
            case 2:
            case 3:
                len = LSTables.CHROM_DC_MIN_CODEWORD_LEN;
                break;
        }
        return len;
    }

    public int getACMinCodeLen(int channel) {
        return LSTables.LUM_AC_MIN_CODEWORD_LEN;
    }

    public int[][] getQuantizationMatrix(int channel, int quality) {
        int[][] qMatrix = null;
        switch(channel) {
            case 1:
                qMatrix = LSQuantizer.getLuminanceQM(quality);
                break;
            case 2:
            case 3:
                qMatrix = LSQuantizer.getChrominanceQM(quality);
                break;
        }
        return qMatrix;
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
        return LSColorSpace.YUV_NAME;
    }

    public void convertToRGB(ImageInputStream iis, int w, int h, int[][] data) throws IOException {
        RAWImageInputStream ris = new RAWImageInputStream(iis);
        ris.selectBitReader(-1, 262144);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; ) {
                data[i][j++] = fromYUVtoRGB((int) ris.readBits(24));
            }
        }
    }

    public String toString() {
        return "YUV";
    }
}
