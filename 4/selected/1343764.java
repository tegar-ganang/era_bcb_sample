package hdr_plugin.tonemapping.drago;

import hdr_plugin.helper.ArrayTools;
import ij.ImagePlus;

/**
 *
 * @author Alexander Heidrich <alexander.heidrich@hki-jena.de>
 */
public class DragoToneMapper {

    private ImagePlus imp;

    private double bias;

    public DragoToneMapper(ImagePlus imp) {
        this.imp = imp;
        this.bias = 0.85;
    }

    public double biasFunc(double b, double x) {
        return Math.pow(x, b);
    }

    private float[] calculateLuminance(int size, float[] Y) {
        float avLum = 0.0f;
        float maxLum = 0.0f;
        for (int i = 0; i < size; i++) {
            avLum += Math.log(Y[i] + 1e-4);
            maxLum = (Y[i] > maxLum) ? Y[i] : maxLum;
        }
        avLum = (float) Math.exp(avLum / size);
        return new float[] { avLum, maxLum };
    }

    public double[] fastmap() {
        int length = imp.getWidth() * imp.getHeight();
        float[] pixels = (float[]) imp.getChannelProcessor().getPixels();
        double[][] out = new double[imp.getHeight()][imp.getWidth()];
        float[] luminance = calculateLuminance(length, pixels);
        double maxLum = luminance[1];
        double avLum = luminance[0];
        maxLum /= avLum;
        float[][] pixels2D = ArrayTools.convert1D2D(pixels, imp.getWidth(), imp.getHeight());
        System.out.println(pixels2D.length);
        System.out.println(pixels2D[1].length);
        double LOG05 = -0.693147;
        float biasP = (float) (Math.log(bias) / LOG05);
        double divider = Math.log10(maxLum + 1.0);
        int i, j;
        for (int y = 0; y < imp.getHeight(); y += 3) {
            for (int x = 0; x < imp.getWidth(); x += 3) {
                double average = 0.0;
                for (i = 0; i < 3; i++) {
                    for (j = 0; j < 3; j++) {
                        System.out.println(x + " " + (x + i) + " " + (y + j));
                        average += pixels2D[x + i][y + j] / avLum;
                    }
                }
                average = average / 9.0 - pixels2D[x][y];
                if (average > -1.0f && average < 1.0) {
                    double interpol = Math.log(2.0 + biasFunc(biasP, pixels2D[x + 1][y + 1] / maxLum) * 8.0);
                    for (i = 0; i < 3; i++) {
                        for (j = 0; j < 3; j++) {
                            double Yw = pixels2D[x + i][y + j];
                            if (Yw < 1.0f) {
                                double L = Yw * (6.0 + Yw) / (6.0 + 4.0 * Yw);
                                Yw = (L / interpol) / divider;
                            } else if (Yw >= 1.0f && Yw < 2.0f) {
                                double L = Yw * (6.0 + 0.7662 * Yw) / (5.9897 + 3.7658 * Yw);
                                Yw = (L / interpol) / divider;
                            } else {
                                Yw = (Math.log(Yw + 1.0) / interpol) / divider;
                            }
                            out[x + i][y + j] = Yw;
                        }
                    }
                } else {
                    for (i = 0; i < 3; i++) {
                        for (j = 0; j < 3; j++) {
                            double Yw = pixels2D[x + i][y + j];
                            double interpol = Math.log(2.0f + biasFunc(biasP, Yw / maxLum) * 8.0);
                            out[x + i][y + j] = (Math.log(Yw + 1.0) / interpol) / divider;
                        }
                    }
                }
            }
        }
        for (int x = 0; x < imp.getWidth(); x++) {
            for (int y = 0; y < imp.getHeight(); y++) {
                double scale = out[x][y] / pixels2D[x][y];
                pixels2D[x][y] *= scale;
            }
        }
        return ArrayTools.convert2D1D(pixels2D, imp.getWidth(), imp.getHeight());
    }

    public double[] map() {
        double LOG05 = -0.693147;
        int length = imp.getWidth() * imp.getHeight();
        double[][] out = new double[imp.getHeight()][imp.getWidth()];
        float[] pixels = (float[]) imp.getChannelProcessor().getPixels();
        float[] luminance = calculateLuminance(length, pixels);
        double maxLum = luminance[1];
        double avLum = luminance[0];
        float[][] pixels2D = ArrayTools.convert1D2D(pixels, imp.getHeight(), imp.getWidth());
        maxLum /= avLum;
        double divider = Math.log10(maxLum + 1.0);
        double biasP = Math.log(bias) / LOG05;
        for (int y = 0; y < imp.getHeight(); y++) {
            for (int x = 0; x < imp.getWidth(); x++) {
                double Yw = pixels2D[y][x] / avLum;
                double interpol = Math.log(2.0 + biasFunc(biasP, Yw / maxLum) * 8.0);
                out[y][x] = (Math.log(Yw + 1.0) / interpol) / divider;
            }
        }
        for (int x = 0; x < imp.getWidth(); x++) {
            for (int y = 0; y < imp.getHeight(); y++) {
                double scale = out[y][x] / pixels2D[y][x];
                pixels2D[y][x] *= scale;
                if (pixels2D[y][x] == 1.0) {
                    System.out.println(scale);
                    System.out.println(pixels2D[y][x]);
                    System.out.println(x + " " + y);
                }
            }
        }
        return ArrayTools.convert2D1D(pixels2D, imp.getHeight(), imp.getWidth());
    }
}
