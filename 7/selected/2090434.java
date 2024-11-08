package org.designerator.media.image.filter.util;

public class OffsetKernel {

    public OffsetKernel() {
        super();
    }

    public static int[] getSquareKernel(int kernellength, int kernelwidth, int imagewidth) {
        int[] offset = new int[kernellength];
        for (int i = 0; i < kernelwidth; i++) {
            for (int j = 0; j < kernelwidth; j++) {
                offset[j + kernelwidth * i] = (imagewidth * i + j) * 4;
            }
        }
        return offset;
    }

    public static int[] getSquareCenteredKernel(int kernellength, int kernelwidth, int imagewidth, int bpp, int pad) {
        int[] offset = new int[kernellength];
        int factor = -(kernelwidth / 2);
        int factor2 = factor;
        int x = 0;
        for (int i = 0; i < kernelwidth; i++) {
            for (int j = 0, f = factor2; j < kernelwidth; j++, f++) {
                int k = imagewidth * factor + f;
                offset[x] = k * bpp + (factor * pad);
                x++;
            }
            factor++;
        }
        return offset;
    }

    public static int[] getRoundCenteredKernel5(int kernellength, int kernelwidth, int imagewidth, int bpp, int pad) {
        if (kernellength < 9) {
            return null;
        }
        int[] offset2 = getSquareCenteredKernel(kernellength, kernelwidth, imagewidth, bpp, pad);
        int[] offset = new int[offset2.length - 4];
        for (int i = 1, j = 0; i < offset2.length - 1; i++) {
            if (i != 4 && i != 20) {
                offset[j++] = offset2[i];
            }
        }
        return offset;
    }

    /**
	 * @param kernelWidth minimum==5
	 * @param imagewidth imagewidth
	 * @param bpp bits per pixel (1,2,3,4)
	 * @return returns the offsets for a round kernel
	 */
    public static int[] getRoundKernel(int kernelWidth, int imagewidth, int bpp) {
        if (kernelWidth < 5 || kernelWidth % 2 == 0) return null;
        int[] rows = new int[kernelWidth];
        int r = 0;
        int kernelSize = 0;
        for (int i = 3; i < kernelWidth; i += 2) {
            rows[r++] = i;
            kernelSize += i;
        }
        for (int i = 0; i < 3; i++) {
            rows[r++] = kernelWidth;
            kernelSize += kernelWidth;
        }
        for (int i = kernelWidth - 2; i > 1; i -= 2) {
            rows[r++] = i;
            kernelSize += i;
        }
        int[] kernel = new int[kernelSize];
        int roundPart = (kernelWidth - 3) / 2;
        int rowcount = 0;
        int n = 0;
        int imagewidth2 = imagewidth * bpp;
        int distance = -(kernelWidth / 2);
        for (int i = 0; i < roundPart; i++) {
            for (int j = -rows[rowcount] / 2; j <= rows[rowcount] / 2; j++) {
                kernel[n++] = j + imagewidth2 * distance;
            }
            rowcount++;
            distance++;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = -rows[rowcount] / 2; j <= rows[rowcount] / 2; j++) {
                kernel[n++] = j + imagewidth2 * distance;
            }
            rowcount++;
            distance++;
        }
        for (int i = 0; i < roundPart; i++) {
            for (int j = -rows[rowcount] / 2; j <= rows[rowcount] / 2; j++) {
                kernel[n++] = j + imagewidth2 * distance;
            }
            rowcount++;
            distance++;
        }
        return kernel;
    }

    public static int[] getRoundKernel7x7(int imagewidth) {
        int[] offset = new int[37];
        int[] offset2 = getSquareKernel(49, 7, imagewidth);
        int j = 2;
        for (int i = 0; i < 3; i++, j++) {
            offset[i] = offset2[j];
        }
        j = 8;
        for (int i = 3; i < 8; i++, j++) {
            offset[i] = offset2[j];
        }
        j = 14;
        for (int i = 8; i < 29; i++, j++) {
            offset[i] = offset2[j];
        }
        j = 36;
        for (int i = 29; i < 34; i++, j++) {
            offset[i] = offset2[j];
        }
        j = 44;
        for (int i = 34; i < 37; i++, j++) {
            offset[i] = offset2[j];
        }
        return offset;
    }

    public static int[] getSquareKernelnoCenter(int kernellength, int kernelwidth, int imagewidth) {
        int[] offset = new int[kernellength];
        int center = kernellength / 2 + 1;
        for (int i = 0; i < kernelwidth; i++) {
            for (int j = 0; j < kernelwidth; j++) {
                if (center == i && center == j) {
                    continue;
                }
                offset[j + kernelwidth * i] = (imagewidth * i + j) * 4;
            }
        }
        int[] offset2 = new int[kernellength - 1];
        for (int i = 0; i < offset2.length / 2; i++) {
            offset2[i] = offset[i];
        }
        for (int i = offset2.length / 2; i < offset2.length; i++) {
            offset2[i] = offset[i + 1];
        }
        return offset2;
    }

    public static int[] getSingleKernel(int imagewidth, int length) {
        int[] offset = new int[length];
        for (int i = 0; i < offset.length; i++) {
            int j = offset[i];
        }
        return offset;
    }

    public static int[] getCrossKernel3x3(int imagewidth) {
        int[] offset = { -imagewidth * 4, -4, 0, 4, imagewidth * 4 };
        return offset;
    }

    public static int[] getCrossKernel3x3noCenter(int imagewidth) {
        int[] offset = { -imagewidth * 4, -4, 4, imagewidth * 4 };
        return offset;
    }

    public static int[] getCrossKernel5x5noCenter(int imagewidth) {
        int[] offset = { -imagewidth * 4 * 2, -imagewidth * 4, -8, -4, 4, 8, imagewidth * 4, -imagewidth * 4 * 2 };
        return offset;
    }

    public static void main(String[] args) {
        int[] offset = getRoundCenteredKernel5(25, 5, 333, 3, 3);
        for (int i = 0; i < offset.length; i++) {
            System.out.println(offset[i]);
        }
        System.out.println();
        for (int i = 4; i < 15; i += 5) {
            for (int j = 0; j < 3; j++) {
                System.out.println(offset[i + j]);
            }
        }
    }
}
