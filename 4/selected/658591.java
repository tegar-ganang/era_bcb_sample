package bunwarpj;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Stack;
import java.util.StringTokenizer;

/**
 * Different tools for the bUnwarpJ interface.
 */
public class MiscTools {

    /**
	 * Apply a given B-spline transformation to the source (gray-scale) image.
	 * The source image is modified. The target image is used to know
	 * the output size.
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param source source image model
	 * @param intervals intervals in the deformation
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
	 * 
	 * @deprecated
	 */
    public static void applyTransformationToSource(ImagePlus sourceImp, ImagePlus targetImp, BSplineModel source, int intervals, double[][] cx, double[][] cy) {
        int targetHeight = targetImp.getProcessor().getHeight();
        int targetWidth = targetImp.getProcessor().getWidth();
        int sourceHeight = sourceImp.getProcessor().getHeight();
        int sourceWidth = sourceImp.getProcessor().getWidth();
        double[][] transformation_x = new double[targetHeight][targetWidth];
        double[][] transformation_y = new double[targetHeight][targetWidth];
        BSplineModel swx = new BSplineModel(cx);
        BSplineModel swy = new BSplineModel(cy);
        boolean ORIGINAL = false;
        for (int v = 0; v < targetHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetHeight - 1) + 1.0F;
            for (int u = 0; u < targetWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetWidth - 1) + 1.0F;
                swx.prepareForInterpolation(tu, tv, ORIGINAL);
                transformation_x[v][u] = swx.interpolateI();
                swy.prepareForInterpolation(tu, tv, ORIGINAL);
                transformation_y[v][u] = swy.interpolateI();
            }
        }
        if (!(sourceImp.getProcessor() instanceof ColorProcessor)) {
            source.startPyramids();
            try {
                source.getThread().join();
            } catch (InterruptedException e) {
                IJ.error("Unexpected interruption exception " + e);
            }
            FloatProcessor fp = new FloatProcessor(targetWidth, targetHeight);
            for (int v = 0; v < targetHeight; v++) for (int u = 0; u < targetWidth; u++) {
                final double x = transformation_x[v][u];
                final double y = transformation_y[v][u];
                if (x >= 0 && x < sourceWidth && y >= 0 && y < sourceHeight) {
                    source.prepareForInterpolation(x, y, ORIGINAL);
                    fp.putPixelValue(u, v, source.interpolateI());
                } else fp.putPixelValue(u, v, 0);
            }
            fp.resetMinAndMax();
            sourceImp.setProcessor(sourceImp.getTitle(), fp);
            sourceImp.updateImage();
        } else {
            BSplineModel sourceR = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(0, null), false, 1);
            sourceR.setPyramidDepth(0);
            sourceR.startPyramids();
            BSplineModel sourceG = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(1, null), false, 1);
            sourceG.setPyramidDepth(0);
            sourceG.startPyramids();
            BSplineModel sourceB = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(2, null), false, 1);
            sourceB.setPyramidDepth(0);
            sourceB.startPyramids();
            try {
                sourceR.getThread().join();
                sourceG.getThread().join();
                sourceB.getThread().join();
            } catch (InterruptedException e) {
                IJ.error("Unexpected interruption exception " + e);
            }
            ColorProcessor cp = new ColorProcessor(targetWidth, targetHeight);
            FloatProcessor fpR = new FloatProcessor(targetWidth, targetHeight);
            FloatProcessor fpG = new FloatProcessor(targetWidth, targetHeight);
            FloatProcessor fpB = new FloatProcessor(targetWidth, targetHeight);
            for (int v = 0; v < targetHeight; v++) for (int u = 0; u < targetWidth; u++) {
                final double x = transformation_x[v][u];
                final double y = transformation_y[v][u];
                if (x >= 0 && x < sourceWidth && y >= 0 && y < sourceHeight) {
                    sourceR.prepareForInterpolation(x, y, ORIGINAL);
                    fpR.putPixelValue(u, v, sourceR.interpolateI());
                    sourceG.prepareForInterpolation(x, y, ORIGINAL);
                    fpG.putPixelValue(u, v, sourceG.interpolateI());
                    sourceB.prepareForInterpolation(x, y, ORIGINAL);
                    fpB.putPixelValue(u, v, sourceB.interpolateI());
                } else {
                    fpR.putPixelValue(u, v, 0);
                    fpG.putPixelValue(u, v, 0);
                    fpB.putPixelValue(u, v, 0);
                }
            }
            cp.setPixels(0, fpR);
            cp.setPixels(1, fpG);
            cp.setPixels(2, fpB);
            cp.resetMinAndMax();
            sourceImp.setProcessor(sourceImp.getTitle(), cp);
            sourceImp.updateImage();
        }
    }

    /**
	 * Apply a given splines transformation to the source (RGB color) image.
	 * The source image is modified. The target image is used to know
	 * the output size.
	 * 
	 * @deprecated
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param sourceR image model of the source red channel 
	 * @param sourceG image model of the source green channel
	 * @param sourceB image model of the source blue channel
	 * @param intervals intervals in the deformation
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
	 */
    public static void applyTransformationToSource(ImagePlus sourceImp, ImagePlus targetImp, BSplineModel sourceR, BSplineModel sourceG, BSplineModel sourceB, int intervals, double[][] cx, double[][] cy) {
        int targetHeight = targetImp.getProcessor().getHeight();
        int targetWidth = targetImp.getProcessor().getWidth();
        int sourceHeight = sourceImp.getProcessor().getHeight();
        int sourceWidth = sourceImp.getProcessor().getWidth();
        double[][] transformation_x = new double[targetHeight][targetWidth];
        double[][] transformation_y = new double[targetHeight][targetWidth];
        BSplineModel swx = new BSplineModel(cx);
        BSplineModel swy = new BSplineModel(cy);
        boolean ORIGINAL = false;
        for (int v = 0; v < targetHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetHeight - 1) + 1.0F;
            for (int u = 0; u < targetWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetWidth - 1) + 1.0F;
                swx.prepareForInterpolation(tu, tv, ORIGINAL);
                transformation_x[v][u] = swx.interpolateI();
                swy.prepareForInterpolation(tu, tv, ORIGINAL);
                transformation_y[v][u] = swy.interpolateI();
            }
        }
        ColorProcessor cp = new ColorProcessor(targetWidth, targetHeight);
        FloatProcessor fpR = new FloatProcessor(targetWidth, targetHeight);
        FloatProcessor fpG = new FloatProcessor(targetWidth, targetHeight);
        FloatProcessor fpB = new FloatProcessor(targetWidth, targetHeight);
        for (int v = 0; v < targetHeight; v++) for (int u = 0; u < targetWidth; u++) {
            final double x = transformation_x[v][u];
            final double y = transformation_y[v][u];
            if (x >= 0 && x < sourceWidth && y >= 0 && y < sourceHeight) {
                sourceR.prepareForInterpolation(x, y, ORIGINAL);
                fpR.putPixelValue(u, v, sourceR.interpolateI());
                sourceG.prepareForInterpolation(x, y, ORIGINAL);
                fpG.putPixelValue(u, v, sourceG.interpolateI());
                sourceB.prepareForInterpolation(x, y, ORIGINAL);
                fpB.putPixelValue(u, v, sourceB.interpolateI());
            } else {
                fpR.putPixelValue(u, v, 0);
                fpG.putPixelValue(u, v, 0);
                fpB.putPixelValue(u, v, 0);
            }
        }
        cp.setPixels(0, fpR);
        cp.setPixels(1, fpG);
        cp.setPixels(2, fpB);
        cp.resetMinAndMax();
        sourceImp.setProcessor(sourceImp.getTitle(), cp);
        sourceImp.updateImage();
    }

    /**
	 * Apply a given raw transformation to the source image.
	 * The source image is modified. The target image is used to know
	 * the output size.
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param source source image
	 * @param transformation_x x- mapping coordinates
	 * @param transformation_y y- mapping coordinates
	 */
    public static void applyRawTransformationToSource(ImagePlus sourceImp, ImagePlus targetImp, BSplineModel source, double[][] transformation_x, double[][] transformation_y) {
        int targetHeight = targetImp.getProcessor().getHeight();
        int targetWidth = targetImp.getProcessor().getWidth();
        int sourceHeight = sourceImp.getProcessor().getHeight();
        int sourceWidth = sourceImp.getProcessor().getWidth();
        boolean ORIGINAL = false;
        if (!(sourceImp.getProcessor() instanceof ColorProcessor)) {
            source.startPyramids();
            try {
                source.getThread().join();
            } catch (InterruptedException e) {
                IJ.error("Unexpected interruption exception " + e);
            }
            FloatProcessor fp = new FloatProcessor(targetWidth, targetHeight);
            for (int v = 0; v < targetHeight; v++) for (int u = 0; u < targetWidth; u++) {
                final double x = transformation_x[v][u];
                final double y = transformation_y[v][u];
                if (x >= 0 && x < sourceWidth && y >= 0 && y < sourceHeight) {
                    source.prepareForInterpolation(x, y, ORIGINAL);
                    fp.putPixelValue(u, v, source.interpolateI());
                } else fp.putPixelValue(u, v, 0);
            }
            fp.resetMinAndMax();
            sourceImp.setProcessor(sourceImp.getTitle(), fp);
            sourceImp.updateImage();
        } else {
            BSplineModel sourceR = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(0, null), false, 1);
            sourceR.setPyramidDepth(0);
            sourceR.startPyramids();
            BSplineModel sourceG = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(1, null), false, 1);
            sourceG.setPyramidDepth(0);
            sourceG.startPyramids();
            BSplineModel sourceB = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(2, null), false, 1);
            sourceB.setPyramidDepth(0);
            sourceB.startPyramids();
            try {
                sourceR.getThread().join();
                sourceG.getThread().join();
                sourceB.getThread().join();
            } catch (InterruptedException e) {
                IJ.error("Unexpected interruption exception " + e);
            }
            ColorProcessor cp = new ColorProcessor(targetWidth, targetHeight);
            FloatProcessor fpR = new FloatProcessor(targetWidth, targetHeight);
            FloatProcessor fpG = new FloatProcessor(targetWidth, targetHeight);
            FloatProcessor fpB = new FloatProcessor(targetWidth, targetHeight);
            for (int v = 0; v < targetHeight; v++) for (int u = 0; u < targetWidth; u++) {
                final double x = transformation_x[v][u];
                final double y = transformation_y[v][u];
                if (x >= 0 && x < sourceWidth && y >= 0 && y < sourceHeight) {
                    sourceR.prepareForInterpolation(x, y, ORIGINAL);
                    fpR.putPixelValue(u, v, sourceR.interpolateI());
                    sourceG.prepareForInterpolation(x, y, ORIGINAL);
                    fpG.putPixelValue(u, v, sourceG.interpolateI());
                    sourceB.prepareForInterpolation(x, y, ORIGINAL);
                    fpB.putPixelValue(u, v, sourceB.interpolateI());
                } else {
                    fpR.putPixelValue(u, v, 0);
                    fpG.putPixelValue(u, v, 0);
                    fpB.putPixelValue(u, v, 0);
                }
            }
            cp.setPixels(0, fpR);
            cp.setPixels(1, fpG);
            cp.setPixels(2, fpB);
            cp.resetMinAndMax();
            sourceImp.setProcessor(sourceImp.getTitle(), cp);
            sourceImp.updateImage();
        }
    }

    /**
	 * Calculate the warping index between two opposite elastic deformations.
	 * Note: the only difference between the warping index and the consistency 
	 * term formulae is a squared root: warping index = sqrt(consistency error).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx_direct direct transformation x- B-spline coefficients
	 * @param cy_direct direct transformation y- B-spline coefficients
	 * @param cx_inverse inverse transformation x- B-spline coefficients
	 * @param cy_inverse inverse transformation y- B-spline coefficients
	 * 
	 * @return geometric error (warping index) between both deformations.
	 */
    public static double warpingIndex(ImagePlus sourceImp, ImagePlus targetImp, int intervals, double[][] cx_direct, double[][] cy_direct, double[][] cx_inverse, double[][] cy_inverse) {
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        int sourceCurrentHeight = sourceImp.getProcessor().getHeight();
        int sourceCurrentWidth = sourceImp.getProcessor().getWidth();
        double[][] transformation_x_direct = new double[targetCurrentHeight][targetCurrentWidth];
        double[][] transformation_y_direct = new double[targetCurrentHeight][targetCurrentWidth];
        double[][] transformation_x_inverse = new double[sourceCurrentHeight][sourceCurrentWidth];
        double[][] transformation_y_inverse = new double[sourceCurrentHeight][sourceCurrentWidth];
        int cYdim = intervals + 3;
        int cXdim = cYdim;
        int Nk = cYdim * cXdim;
        int twiceNk = 2 * Nk;
        double c_direct[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c_direct[n] = cx_direct[i][j];
            c_direct[n + Nk] = cy_direct[i][j];
        }
        BSplineModel swx_direct = new BSplineModel(c_direct, cYdim, cXdim, 0);
        BSplineModel swy_direct = new BSplineModel(c_direct, cYdim, cXdim, Nk);
        double c_inverse[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c_inverse[n] = cx_inverse[i][j];
            c_inverse[n + Nk] = cy_inverse[i][j];
        }
        BSplineModel swx_inverse = new BSplineModel(c_inverse, cYdim, cXdim, 0);
        BSplineModel swy_inverse = new BSplineModel(c_inverse, cYdim, cXdim, Nk);
        for (int v = 0; v < targetCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < targetCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx_direct.prepareForInterpolation(tu, tv, false);
                transformation_x_direct[v][u] = swx_direct.interpolateI();
                swy_direct.prepareForInterpolation(tu, tv, false);
                transformation_y_direct[v][u] = swy_direct.interpolateI();
            }
        }
        for (int v = 0; v < sourceCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (sourceCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < sourceCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (sourceCurrentWidth - 1) + 1.0F;
                swx_inverse.prepareForInterpolation(tu, tv, false);
                transformation_x_inverse[v][u] = swx_inverse.interpolateI();
                swy_inverse.prepareForInterpolation(tu, tv, false);
                transformation_y_inverse[v][u] = swy_inverse.interpolateI();
            }
        }
        double warpingIndex = 0;
        int n = 0;
        for (int v = 0; v < targetCurrentHeight; v++) for (int u = 0; u < targetCurrentWidth; u++) {
            final int x = (int) Math.round(transformation_x_direct[v][u]);
            final int y = (int) Math.round(transformation_y_direct[v][u]);
            if (x >= 0 && x < sourceCurrentWidth && y >= 0 && y < sourceCurrentHeight) {
                final double x2 = transformation_x_inverse[y][x];
                final double y2 = transformation_y_inverse[y][x];
                double aux1 = u - x2;
                double aux2 = v - y2;
                warpingIndex += aux1 * aux1 + aux2 * aux2;
                n++;
            }
        }
        if (n != 0) {
            warpingIndex /= (double) n;
            warpingIndex = Math.sqrt(warpingIndex);
        } else warpingIndex = -1;
        return warpingIndex;
    }

    /**
	 * Calculate the raw transformation mapping from B-spline
	 * coefficients.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx transformation x- B-spline coefficients
	 * @param cy transformation y- B-spline coefficients
	 * @param transformation_x raw transformation in x- axis (output)
	 * @param transformation_y raw transformation in y- axis (output)
	 */
    public static void convertElasticTransformationToRaw(ImagePlus targetImp, int intervals, double[][] cx, double[][] cy, double[][] transformation_x, double[][] transformation_y) {
        if (cx == null || cy == null || transformation_x == null || transformation_y == null) {
            IJ.error("Error in transformations parameters!");
            return;
        }
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        int cYdim = intervals + 3;
        int cXdim = cYdim;
        int Nk = cYdim * cXdim;
        int twiceNk = 2 * Nk;
        double c[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c[n] = cx[i][j];
            c[n + Nk] = cy[i][j];
        }
        BSplineModel swx = new BSplineModel(c, cYdim, cXdim, 0);
        BSplineModel swy = new BSplineModel(c, cYdim, cXdim, Nk);
        swx.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swy.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        for (int v = 0; v < targetCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < targetCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx.prepareForInterpolation(tu, tv, false);
                transformation_x[v][u] = swx.interpolateI();
                swy.prepareForInterpolation(tu, tv, false);
                transformation_y[v][u] = swy.interpolateI();
            }
        }
    }

    public static void convertRawTransformationToBSpline(ImagePlus targetImp, int intervals, double[][] transformation_x, double[][] transformation_y, double[][] cx, double[][] cy) {
        if (cx == null || cy == null || transformation_x == null || transformation_y == null) {
            IJ.error("Error in transformations parameters!");
            return;
        }
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        for (int i = 0; i < intervals + 3; i++) {
            final double v = (double) ((i - 1) * (targetCurrentHeight - 1)) / (double) intervals;
            for (int j = 0; j < intervals + 3; j++) {
                final double u = (double) ((j - 1) * (targetCurrentWidth - 1)) / (double) intervals;
                if (v >= 0 && v < targetCurrentHeight && u < targetCurrentWidth && u >= 0) {
                    final int tv = (int) v;
                    final int tu = (int) u;
                    cx[i][j] = transformation_x[tv][tu];
                    cy[i][j] = transformation_y[tv][tu];
                }
            }
        }
        for (int i = 0; i < intervals + 3; i++) for (int j = 0; j < intervals + 3; j++) {
            int iFrom = i;
            int jFrom = j;
            int iPivot = -1;
            int jPivot = -1;
            if (iFrom < 1) {
                iFrom = 2 * 1 - i;
                iPivot = 1;
                jPivot = j;
            } else if (iFrom > (intervals + 1)) {
                iFrom = 2 * (intervals + 1) - i;
                iPivot = intervals + 1;
                jPivot = j;
            }
            if (jFrom < 1) {
                jFrom = 2 * 1 - j;
                jPivot = 1;
                iPivot = (iPivot != -1) ? iPivot : i;
            } else if (jFrom > (intervals + 1)) {
                jFrom = 2 * (intervals + 1) - j;
                jPivot = intervals + 1;
                iPivot = (iPivot != -1) ? iPivot : i;
            }
            if (iPivot != -1 && jPivot != -1) {
                cx[i][j] = 2 * cx[iPivot][jPivot] - cx[iFrom][jFrom];
                cy[i][j] = 2 * cy[iPivot][jPivot] - cy[iFrom][jFrom];
            }
        }
    }

    public static void invertRawTransformation(ImagePlus targetImp, double[][] transformation_x, double[][] transformation_y, double[][] inv_x, double[][] inv_y) {
        if (inv_x == null || inv_y == null || transformation_x == null || transformation_y == null) {
            IJ.error("Error in transformations parameters!");
            return;
        }
        final int targetCurrentHeight = targetImp.getProcessor().getHeight();
        final int targetCurrentWidth = targetImp.getProcessor().getWidth();
        for (int i = 0; i < targetCurrentHeight; i++) for (int j = 0; j < targetCurrentWidth; j++) {
            final int originX = (int) Math.round(transformation_x[i][j]);
            final int originY = (int) Math.round(transformation_y[i][j]);
            if (originX >= 0 && originX < targetCurrentWidth && originY >= 0 && originY < targetCurrentHeight) {
                inv_x[originY][originX] = j;
                inv_y[originY][originX] = i;
            }
        }
        for (int i = 0; i < targetCurrentHeight; i++) for (int j = 0; j < targetCurrentWidth; j++) {
            if (inv_x[i][j] == 0 && inv_y[i][j] == 0) {
                double val_x = 0;
                double val_y = 0;
                int n = 0;
                if (i > 0) {
                    if (inv_x[i - 1][j] != 0 && inv_y[i - 1][j] != 0) {
                        val_x += inv_x[i - 1][j];
                        val_y += inv_y[i - 1][j];
                        n++;
                    }
                    if (j > 0 && inv_x[i - 1][j - 1] != 0 && inv_y[i - 1][j - 1] != 0) {
                        val_x += inv_x[i - 1][j - 1];
                        val_y += inv_y[i - 1][j - 1];
                        n++;
                    }
                    if (j < targetCurrentWidth - 1 && inv_x[i - 1][j + 1] != 0 && inv_y[i - 1][j + 1] != 0) {
                        val_x += inv_x[i - 1][j + 1];
                        val_y += inv_y[i - 1][j + 1];
                        n++;
                    }
                }
                if (i < targetCurrentHeight - 1) {
                    if (inv_x[i + 1][j] != 0 && inv_y[i + 1][j] != 0) {
                        val_x += inv_x[i + 1][j];
                        val_y += inv_y[i + 1][j];
                        n++;
                    }
                    if (j > 0 && inv_x[i + 1][j - 1] != 0 && inv_y[i + 1][j - 1] != 0) {
                        val_x += inv_x[i + 1][j - 1];
                        val_y += inv_y[i + 1][j - 1];
                        n++;
                    }
                    if (j < targetCurrentWidth - 1 && inv_x[i + 1][j + 1] != 0 && inv_y[i + 1][j + 1] != 0) {
                        val_x += inv_x[i + 1][j + 1];
                        val_y += inv_y[i + 1][j + 1];
                        n++;
                    }
                }
                if (j > 0 && inv_x[i][j - 1] != 0 && inv_y[i][j - 1] != 0) {
                    val_x += inv_x[i][j - 1];
                    val_y += inv_y[i][j - 1];
                    n++;
                }
                if (j < targetCurrentWidth - 1 && inv_x[i][j + 1] != 0 && inv_y[i][j + 1] != 0) {
                    val_x += inv_x[i][j + 1];
                    val_y += inv_y[i][j + 1];
                    n++;
                }
                if (n != 0) {
                    inv_x[i][j] += val_x / n;
                    inv_y[i][j] += val_y / n;
                }
            }
        }
    }

    /**
	 * Warping index for comparing elastic deformations with any kind
	 * of deformation (both transformations having same direction).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx_direct direct transformation x- B-spline coefficients
	 * @param cy_direct direct transformation y- B-spline coefficients
	 * @param transformation_x raw direct transformation in x- axis
	 * @param transformation_y raw direct transformation in y- axis
	 */
    public static double rawWarpingIndex(ImagePlus sourceImp, ImagePlus targetImp, int intervals, double[][] cx_direct, double[][] cy_direct, double[][] transformation_x, double[][] transformation_y) {
        if (cx_direct == null || cy_direct == null || transformation_x == null || transformation_y == null) {
            IJ.error("Error in the raw warping index parameters!");
            return -1;
        }
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        int sourceCurrentHeight = sourceImp.getProcessor().getHeight();
        int sourceCurrentWidth = sourceImp.getProcessor().getWidth();
        double[][] transformation_x_direct = new double[targetCurrentHeight][targetCurrentWidth];
        double[][] transformation_y_direct = new double[targetCurrentHeight][targetCurrentWidth];
        int cYdim = intervals + 3;
        int cXdim = cYdim;
        int Nk = cYdim * cXdim;
        int twiceNk = 2 * Nk;
        double c_direct[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c_direct[n] = cx_direct[i][j];
            c_direct[n + Nk] = cy_direct[i][j];
        }
        BSplineModel swx_direct = new BSplineModel(c_direct, cYdim, cXdim, 0);
        BSplineModel swy_direct = new BSplineModel(c_direct, cYdim, cXdim, Nk);
        swx_direct.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swy_direct.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        for (int v = 0; v < targetCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < targetCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx_direct.prepareForInterpolation(tu, tv, false);
                transformation_x_direct[v][u] = swx_direct.interpolateI();
                swy_direct.prepareForInterpolation(tu, tv, false);
                transformation_y_direct[v][u] = swy_direct.interpolateI();
            }
        }
        double warpingIndex = 0;
        int n = 0;
        for (int v = 0; v < targetCurrentHeight; v++) for (int u = 0; u < targetCurrentWidth; u++) {
            final double x_elastic = transformation_x_direct[v][u];
            final double y_elastic = transformation_y_direct[v][u];
            if (x_elastic >= 0 && x_elastic < sourceCurrentWidth && y_elastic >= 0 && y_elastic < sourceCurrentHeight) {
                double x_random = transformation_x[v][u];
                double y_random = transformation_y[v][u];
                double aux1 = x_elastic - x_random;
                double aux2 = y_elastic - y_random;
                warpingIndex += aux1 * aux1 + aux2 * aux2;
                n++;
            }
        }
        if (n != 0) {
            warpingIndex /= (double) n;
            warpingIndex = Math.sqrt(warpingIndex);
        } else warpingIndex = -1;
        return warpingIndex;
    }

    /**
	 * Warping index for comparing two raw deformations (both 
	 * transformations having same direction).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param transformation_x_1 raw first transformation in x- axis
	 * @param transformation_y_1 raw first transformation in y- axis
	 * @param transformation_x_2 raw second transformation in x- axis
	 * @param transformation_y_2 raw second transformation in y- axis
	 */
    public static double rawWarpingIndex(ImagePlus sourceImp, ImagePlus targetImp, double[][] transformation_x_1, double[][] transformation_y_1, double[][] transformation_x_2, double[][] transformation_y_2) {
        if (transformation_x_1 == null || transformation_y_1 == null || transformation_x_2 == null || transformation_y_2 == null) {
            IJ.error("Error in the raw warping index parameters!");
            return -1;
        }
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        int sourceCurrentHeight = sourceImp.getProcessor().getHeight();
        int sourceCurrentWidth = sourceImp.getProcessor().getWidth();
        double warpingIndex = 0;
        int n = 0;
        for (int v = 0; v < targetCurrentHeight; v++) for (int u = 0; u < targetCurrentWidth; u++) {
            final double x1 = transformation_x_1[v][u];
            final double y1 = transformation_y_1[v][u];
            if (x1 >= 0 && x1 < sourceCurrentWidth && y1 >= 0 && y1 < sourceCurrentHeight) {
                double x2 = transformation_x_2[v][u];
                double y2 = transformation_y_2[v][u];
                double aux1 = x1 - x2;
                double aux2 = y1 - y2;
                warpingIndex += aux1 * aux1 + aux2 * aux2;
                n++;
            }
        }
        if (n != 0) {
            warpingIndex /= (double) n;
            warpingIndex = Math.sqrt(warpingIndex);
        } else warpingIndex = -1;
        return warpingIndex;
    }

    /**
	 * Draw an arrow between two points.
	 * The arrow head is in (x2,y2)
	 *
	 * @param canvas canvas where we are painting
	 * @param x1 x- coordinate for the arrow origin
	 * @param y1 y- coordinate for the arrow origin
	 * @param x2 x- coordinate for the arrow head
	 * @param y2 y- coordinate for the arrow head
	 * @param color arrow color
	 * @param arrow_size arrow size
	 */
    public static void drawArrow(double[][] canvas, int x1, int y1, int x2, int y2, double color, int arrow_size) {
        drawLine(canvas, x1, y1, x2, y2, color);
        int arrow_size2 = 2 * arrow_size;
        if ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) < arrow_size * arrow_size) return;
        if (x2 == x1) {
            if (y2 > y1) {
                drawLine(canvas, x2, y2, x2 - arrow_size, y2 - arrow_size2, color);
                drawLine(canvas, x2, y2, x2 + arrow_size, y2 - arrow_size2, color);
            } else {
                drawLine(canvas, x2, y2, x2 - arrow_size, y2 + arrow_size2, color);
                drawLine(canvas, x2, y2, x2 + arrow_size, y2 + arrow_size2, color);
            }
        } else if (y2 == y1) {
            if (x2 > x1) {
                drawLine(canvas, x2, y2, x2 - arrow_size2, y2 - arrow_size, color);
                drawLine(canvas, x2, y2, x2 - arrow_size2, y2 + arrow_size, color);
            } else {
                drawLine(canvas, x2, y2, x2 + arrow_size2, y2 - arrow_size, color);
                drawLine(canvas, x2, y2, x2 + arrow_size2, y2 + arrow_size, color);
            }
        } else {
            double t1 = Math.abs(new Integer(y2 - y1).doubleValue());
            double t2 = Math.abs(new Integer(x2 - x1).doubleValue());
            double theta = Math.atan(t1 / t2);
            if (x2 < x1) {
                if (y2 < y1) theta = Math.PI + theta; else theta = -(Math.PI + theta);
            } else if (x2 > x1 && y2 < y1) theta = 2 * Math.PI - theta;
            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            Point p2 = new Point(-arrow_size2, -arrow_size);
            Point p3 = new Point(-arrow_size2, +arrow_size);
            int x = new Long(Math.round((cosTheta * p2.x) - (sinTheta * p2.y))).intValue();
            p2.y = new Long(Math.round((sinTheta * p2.x) + (cosTheta * p2.y))).intValue();
            p2.x = x;
            x = new Long(Math.round((cosTheta * p3.x) - (sinTheta * p3.y))).intValue();
            p3.y = new Long(Math.round((sinTheta * p3.x) + (cosTheta * p3.y))).intValue();
            p3.x = x;
            p2.translate(x2, y2);
            p3.translate(x2, y2);
            drawLine(canvas, x2, y2, p2.x, p2.y, color);
            drawLine(canvas, x2, y2, p3.x, p3.y, color);
        }
    }

    /**
	 * Draw a line between two points.
	 * Bresenham's algorithm.
	 *
	 * @param canvas canvas where we are painting
	 * @param x1 x- coordinate for first point
	 * @param y1 y- coordinate for first point
	 * @param x2 x- coordinate for second point
	 * @param y2 y- coordinate for second point
	 * @param color line color
	 */
    public static void drawLine(double[][] canvas, int x1, int y1, int x2, int y2, double color) {
        int temp;
        int dy_neg = 1;
        int dx_neg = 1;
        int switch_x_y = 0;
        int neg_slope = 0;
        int tempx, tempy;
        int dx = x2 - x1;
        if (dx == 0) if (y1 > y2) {
            for (int n = y2; n <= y1; n++) Point(canvas, n, x1, color);
            return;
        } else {
            for (int n = y1; n <= y2; n++) Point(canvas, n, x1, color);
            return;
        }
        int dy = y2 - y1;
        if (dy == 0) if (x1 > x2) {
            for (int n = x2; n <= x1; n++) Point(canvas, y1, n, color);
            return;
        } else {
            for (int n = x1; n <= x2; n++) Point(canvas, y1, n, color);
            return;
        }
        float m = (float) dy / dx;
        if (m > 1 || m < -1) {
            temp = x1;
            x1 = y1;
            y1 = temp;
            temp = x2;
            x2 = y2;
            y2 = temp;
            dx = x2 - x1;
            dy = y2 - y1;
            m = (float) dy / dx;
            switch_x_y = 1;
        }
        if (x1 > x2) {
            temp = x1;
            x1 = x2;
            x2 = temp;
            temp = y1;
            y1 = y2;
            y2 = temp;
            dx = x2 - x1;
            dy = y2 - y1;
            m = (float) dy / dx;
        }
        if (m < 0) {
            if (dy < 0) {
                dy_neg = -1;
                dx_neg = 1;
            } else {
                dy_neg = 1;
                dx_neg = -1;
            }
            neg_slope = 1;
        }
        int d = 2 * (dy * dy_neg) - (dx * dx_neg);
        int incrH = 2 * dy * dy_neg;
        int incrHV = 2 * ((dy * dy_neg) - (dx * dx_neg));
        int x = x1;
        int y = y1;
        tempx = x;
        tempy = y;
        if (switch_x_y == 1) {
            temp = x;
            x = y;
            y = temp;
        }
        Point(canvas, y, x, color);
        x = tempx;
        y = tempy;
        while (x < x2) {
            if (d <= 0) {
                x++;
                d += incrH;
            } else {
                d += incrHV;
                x++;
                if (neg_slope == 0) y++; else y--;
            }
            tempx = x;
            tempy = y;
            if (switch_x_y == 1) {
                temp = x;
                x = y;
                y = temp;
            }
            Point(canvas, y, x, color);
            x = tempx;
            y = tempy;
        }
    }

    /**
	 * Put the image from an ImageProcessor into a double array.
	 *
	 * @param ip input, origin of the image
	 * @param image output, the image in a double array
	 */
    public static void extractImage(final ImageProcessor ip, double image[]) {
        int k = 0;
        int height = ip.getHeight();
        int width = ip.getWidth();
        if (ip instanceof ByteProcessor) {
            final byte[] pixels = (byte[]) ip.getPixels();
            for (int y = 0; (y < height); y++) for (int x = 0; (x < width); x++, k++) image[k] = (double) (pixels[k] & 0xFF);
        } else if (ip instanceof ShortProcessor) {
            final short[] pixels = (short[]) ip.getPixels();
            for (int y = 0; (y < height); y++) for (int x = 0; (x < width); x++, k++) if (pixels[k] < (short) 0) image[k] = (double) pixels[k] + 65536.0F; else image[k] = (double) pixels[k];
        } else if (ip instanceof FloatProcessor) {
            final float[] pixels = (float[]) ip.getPixels();
            for (int p = 0; p < height * width; p++) image[p] = pixels[p];
        } else if (ip instanceof ColorProcessor) {
            ImageProcessor fp = ip.convertToFloat();
            final float[] pixels = (float[]) fp.getPixels();
            for (int p = 0; p < height * width; p++) image[p] = pixels[p];
        }
    }

    /**
	 * Put the image from an ImageProcessor into a double[][].
	 *
	 * @param ip input, origin of the image
	 * @param image output, the image in a double[][]
	 */
    public static void extractImage(final ImageProcessor ip, double image[][]) {
        int k = 0;
        int height = ip.getHeight();
        int width = ip.getWidth();
        if (ip instanceof ByteProcessor) {
            final byte[] pixels = (byte[]) ip.getPixels();
            for (int y = 0; (y < height); y++) for (int x = 0; (x < width); x++, k++) image[y][x] = (double) (pixels[k] & 0xFF);
        } else if (ip instanceof ShortProcessor) {
            final short[] pixels = (short[]) ip.getPixels();
            for (int y = 0; (y < height); y++) for (int x = 0; (x < width); x++, k++) if (pixels[k] < (short) 0) image[y][x] = (double) pixels[k] + 65536.0F; else image[y][x] = (double) pixels[k];
        } else if (ip instanceof FloatProcessor) {
            final float[] pixels = (float[]) ip.getPixels();
            for (int y = 0; (y < height); y++) for (int x = 0; (x < width); x++, k++) image[y][x] = pixels[k];
        }
    }

    /**
	 * Load landmarks from file.
	 *
	 * @param filename landmarks file name
	 * @param sourceStack stack of source related points (output)
	 * @param targetStack stack of target related points (output)
	 */
    public static void loadPoints(String filename, Stack<Point> sourceStack, Stack<Point> targetStack) {
        Point sourcePoint;
        Point targetPoint;
        try {
            final FileReader fr = new FileReader(filename);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            String index;
            String xSource;
            String ySource;
            String xTarget;
            String yTarget;
            int separatorIndex;
            int k = 1;
            if (!(line = br.readLine()).equals("Index\txSource\tySource\txTarget\tyTarget")) {
                fr.close();
                IJ.write("Line " + k + ": 'Index\txSource\tySource\txTarget\tyTarget'");
                return;
            }
            ++k;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                separatorIndex = line.indexOf('\t');
                if (separatorIndex == -1) {
                    fr.close();
                    IJ.write("Line " + k + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                    return;
                }
                index = line.substring(0, separatorIndex);
                index = index.trim();
                line = line.substring(separatorIndex);
                line = line.trim();
                separatorIndex = line.indexOf('\t');
                if (separatorIndex == -1) {
                    fr.close();
                    IJ.write("Line " + k + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                    return;
                }
                xSource = line.substring(0, separatorIndex);
                xSource = xSource.trim();
                line = line.substring(separatorIndex);
                line = line.trim();
                separatorIndex = line.indexOf('\t');
                if (separatorIndex == -1) {
                    fr.close();
                    IJ.write("Line " + k + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                    return;
                }
                ySource = line.substring(0, separatorIndex);
                ySource = ySource.trim();
                line = line.substring(separatorIndex);
                line = line.trim();
                separatorIndex = line.indexOf('\t');
                if (separatorIndex == -1) {
                    fr.close();
                    IJ.write("Line " + k + ": #Index# <tab> #xSource# <tab> #ySource# <tab> #xTarget# <tab> #yTarget#");
                    return;
                }
                xTarget = line.substring(0, separatorIndex);
                xTarget = xTarget.trim();
                yTarget = line.substring(separatorIndex);
                yTarget = yTarget.trim();
                sourcePoint = new Point(Integer.valueOf(xSource).intValue(), Integer.valueOf(ySource).intValue());
                sourceStack.push(sourcePoint);
                targetPoint = new Point(Integer.valueOf(xTarget).intValue(), Integer.valueOf(yTarget).intValue());
                targetStack.push(targetPoint);
            }
            fr.close();
        } catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return;
        }
    }

    /**
	 * Load point rois in the source and target images as landmarks.
	 * 
	 * @param sourceImp source image plus
	 * @param targetImp target image plus 
	 * @param sourceStack stack of source related points (output)
	 * @param targetStack stack of target related points (output)
	 */
    public static void loadPointRoiAsLandmarks(ImagePlus sourceImp, ImagePlus targetImp, Stack<Point> sourceStack, Stack<Point> targetStack) {
        Roi roiSource = sourceImp.getRoi();
        Roi roiTarget = targetImp.getRoi();
        if (roiSource instanceof PointRoi && roiTarget instanceof PointRoi) {
            PointRoi prSource = (PointRoi) roiSource;
            int[] xSource = prSource.getXCoordinates();
            PointRoi prTarget = (PointRoi) roiTarget;
            int[] xTarget = prTarget.getXCoordinates();
            int numOfPoints = xSource.length;
            if (numOfPoints != xTarget.length) return;
            int[] ySource = prSource.getYCoordinates();
            int[] yTarget = prTarget.getYCoordinates();
            Rectangle recSource = prSource.getBounds();
            int originXSource = recSource.x;
            int originYSource = recSource.y;
            Rectangle recTarget = prTarget.getBounds();
            int originXTarget = recTarget.x;
            int originYTarget = recTarget.y;
            for (int i = 0; i < numOfPoints; i++) {
                sourceStack.push(new Point(xSource[i] + originXSource, ySource[i] + originYSource));
                targetStack.push(new Point(xTarget[i] + originXTarget, yTarget[i] + originYTarget));
            }
        }
    }

    /**
	 * Load a transformation from a file.
	 *
	 * @param filename transformation file name
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
	 */
    public static void loadTransformation(String filename, final double[][] cx, final double[][] cy) {
        try {
            final FileReader fr = new FileReader(filename);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            line = br.readLine();
            int lineN = 1;
            StringTokenizer st = new StringTokenizer(line, "=");
            if (st.countTokens() != 2) {
                fr.close();
                IJ.write("Line " + lineN + "+: Cannot read number of intervals");
                return;
            }
            st.nextToken();
            int intervals = Integer.valueOf(st.nextToken()).intValue();
            line = br.readLine();
            line = br.readLine();
            lineN += 2;
            for (int i = 0; i < intervals + 3; i++) {
                line = br.readLine();
                lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != intervals + 3) {
                    fr.close();
                    IJ.write("Line " + lineN + ": Cannot read enough coefficients");
                    return;
                }
                for (int j = 0; j < intervals + 3; j++) cx[i][j] = Double.valueOf(st.nextToken()).doubleValue();
            }
            line = br.readLine();
            line = br.readLine();
            lineN += 2;
            for (int i = 0; i < intervals + 3; i++) {
                line = br.readLine();
                lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != intervals + 3) {
                    fr.close();
                    IJ.write("Line " + lineN + ": Cannot read enough coefficients");
                    return;
                }
                for (int j = 0; j < intervals + 3; j++) cy[i][j] = Double.valueOf(st.nextToken()).doubleValue();
            }
            fr.close();
        } catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return;
        }
    }

    /**
	 * Load a raw transformation from a file.
	 *
	 * @param filename transformation file name
	 * @param transformation_x output x- transformation coordinates
	 * @param transformation_y output y- transformation coordinates
	 */
    public static void loadRawTransformation(String filename, double[][] transformation_x, double[][] transformation_y) {
        try {
            final FileReader fr = new FileReader(filename);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            line = br.readLine();
            int lineN = 1;
            StringTokenizer st = new StringTokenizer(line, "=");
            if (st.countTokens() != 2) {
                fr.close();
                IJ.write("Line " + lineN + "+: Cannot read transformation width");
                return;
            }
            st.nextToken();
            int width = Integer.valueOf(st.nextToken()).intValue();
            line = br.readLine();
            lineN++;
            st = new StringTokenizer(line, "=");
            if (st.countTokens() != 2) {
                fr.close();
                IJ.write("Line " + lineN + "+: Cannot read transformation height");
                return;
            }
            st.nextToken();
            int height = Integer.valueOf(st.nextToken()).intValue();
            line = br.readLine();
            line = br.readLine();
            lineN += 2;
            for (int i = 0; i < height; i++) {
                line = br.readLine();
                lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != width) {
                    fr.close();
                    IJ.write("Line " + lineN + ": Cannot read enough coordinates");
                    return;
                }
                for (int j = 0; j < width; j++) transformation_x[i][j] = Double.valueOf(st.nextToken()).doubleValue();
            }
            line = br.readLine();
            line = br.readLine();
            lineN += 2;
            for (int i = 0; i < height; i++) {
                line = br.readLine();
                lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != width) {
                    fr.close();
                    IJ.write("Line " + lineN + ": Cannot read enough coordinates");
                    return;
                }
                for (int j = 0; j < width; j++) transformation_y[i][j] = Double.valueOf(st.nextToken()).doubleValue();
            }
            fr.close();
        } catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return;
        }
    }

    /**
	 * Load a raw transformation from a file.
	 *
	 * @param filename transformation file name
	 * @param transformation_x output x- transformation coordinates
	 * @param transformation_y output y- transformation coordinates
	 */
    public static void loadRawTransformation(String filename, double[] transformation_x, double[] transformation_y) {
        try {
            final FileReader fr = new FileReader(filename);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            line = br.readLine();
            int lineN = 1;
            StringTokenizer st = new StringTokenizer(line, "=");
            if (st.countTokens() != 2) {
                fr.close();
                IJ.write("Line " + lineN + "+: Cannot read transformation width");
                return;
            }
            st.nextToken();
            int width = Integer.valueOf(st.nextToken()).intValue();
            line = br.readLine();
            lineN++;
            st = new StringTokenizer(line, "=");
            if (st.countTokens() != 2) {
                fr.close();
                IJ.write("Line " + lineN + "+: Cannot read transformation height");
                return;
            }
            st.nextToken();
            int height = Integer.valueOf(st.nextToken()).intValue();
            line = br.readLine();
            line = br.readLine();
            lineN += 2;
            for (int i = 0; i < height; i++) {
                line = br.readLine();
                lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != width) {
                    fr.close();
                    IJ.write("Line " + lineN + ": Cannot read enough coordinates");
                    return;
                }
                for (int j = 0; j < width; j++) transformation_x[j + i * width] = Double.valueOf(st.nextToken()).doubleValue();
            }
            line = br.readLine();
            line = br.readLine();
            lineN += 2;
            for (int i = 0; i < height; i++) {
                line = br.readLine();
                lineN++;
                st = new StringTokenizer(line);
                if (st.countTokens() != width) {
                    fr.close();
                    IJ.write("Line " + lineN + ": Cannot read enough coordinates");
                    return;
                }
                for (int j = 0; j < width; j++) transformation_y[j + i * width] = Double.valueOf(st.nextToken()).doubleValue();
            }
            fr.close();
        } catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return;
        }
    }

    /**
	 * Load an affine matrix from a file.
	 *
	 * @param filename matrix file name
	 * @param affineMatrix output affine matrix
	 */
    public static void loadAffineMatrix(String filename, double[][] affineMatrix) {
        try {
            final FileReader fr = new FileReader(filename);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            line = br.readLine();
            StringTokenizer st = new StringTokenizer(line, " ");
            if (st.countTokens() != 6) {
                fr.close();
                IJ.write("Cannot read affine transformation matrix");
                return;
            }
            affineMatrix[0][0] = Double.valueOf(st.nextToken()).doubleValue();
            affineMatrix[0][1] = Double.valueOf(st.nextToken()).doubleValue();
            affineMatrix[1][0] = Double.valueOf(st.nextToken()).doubleValue();
            affineMatrix[1][1] = Double.valueOf(st.nextToken()).doubleValue();
            affineMatrix[0][2] = Double.valueOf(st.nextToken()).doubleValue();
            affineMatrix[1][2] = Double.valueOf(st.nextToken()).doubleValue();
            fr.close();
        } catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return;
        }
    }

    /**
	 * Compose two elastic deformations into a raw deformation.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx1 first transformation x- B-spline coefficients
	 * @param cy1 first transformation y- B-spline coefficients
	 * @param cx2 second transformation x- B-spline coefficients
	 * @param cy2 second transformation y- B-spline coefficients
	 * @param outputTransformation_x output transformation coordinates in x-axis
	 * @param outputTransformation_y output transformation coordinates in y-axis
	 */
    public static void composeElasticTransformations(ImagePlus targetImp, int intervals, double[][] cx1, double[][] cy1, double[][] cx2, double[][] cy2, double[][] outputTransformation_x, double[][] outputTransformation_y) {
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        int cYdim = intervals + 3;
        int cXdim = cYdim;
        int Nk = cYdim * cXdim;
        int twiceNk = 2 * Nk;
        double c1[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c1[n] = cx1[i][j];
            c1[n + Nk] = cy1[i][j];
        }
        BSplineModel swx1 = new BSplineModel(c1, cYdim, cXdim, 0);
        BSplineModel swy1 = new BSplineModel(c1, cYdim, cXdim, Nk);
        double c2[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c2[n] = cx2[i][j];
            c2[n + Nk] = cy2[i][j];
        }
        BSplineModel swx2 = new BSplineModel(c2, cYdim, cXdim, 0);
        BSplineModel swy2 = new BSplineModel(c2, cYdim, cXdim, Nk);
        swx1.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swy1.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swx2.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swy2.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        for (int v = 0; v < targetCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < targetCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx2.prepareForInterpolation(tu, tv, false);
                final double x2 = swx2.interpolateI();
                swy2.prepareForInterpolation(tu, tv, false);
                final double y2 = swy2.interpolateI();
                final double tv2 = (double) (y2 * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
                final double tu2 = (double) (x2 * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx1.prepareForInterpolation(tu2, tv2, false);
                outputTransformation_x[v][u] = swx1.interpolateI();
                swy1.prepareForInterpolation(tu2, tv2, false);
                outputTransformation_y[v][u] = swy1.interpolateI();
            }
        }
    }

    /**
	 * Compose a raw deformation and an elastic deformation into a raw deformation.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param transformation_x_1 first transformation coordinates in x-axis
	 * @param transformation_y_1 first transformation coordinates in y-axis
	 * @param cx2 second transformation x- B-spline coefficients
	 * @param cy2 second transformation y- B-spline coefficients
	 * @param outputTransformation_x output transformation coordinates in x-axis
	 * @param outputTransformation_y output transformation coordinates in y-axis
	 */
    public static void composeRawElasticTransformations(ImagePlus targetImp, int intervals, double[][] transformation_x_1, double[][] transformation_y_1, double[][] cx2, double[][] cy2, double[][] outputTransformation_x, double[][] outputTransformation_y) {
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        int cYdim = intervals + 3;
        int cXdim = cYdim;
        int Nk = cYdim * cXdim;
        int twiceNk = 2 * Nk;
        double c2[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c2[n] = cx2[i][j];
            c2[n + Nk] = cy2[i][j];
        }
        BSplineModel swx2 = new BSplineModel(c2, cYdim, cXdim, 0);
        BSplineModel swy2 = new BSplineModel(c2, cYdim, cXdim, Nk);
        swx2.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swy2.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        for (int v = 0; v < targetCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < targetCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx2.prepareForInterpolation(tu, tv, false);
                final double x2 = swx2.interpolateI();
                swy2.prepareForInterpolation(tu, tv, false);
                final double y2 = swy2.interpolateI();
                int xbase = (int) x2;
                int ybase = (int) y2;
                double xFraction = x2 - xbase;
                double yFraction = y2 - ybase;
                if (x2 >= 0 && x2 < targetCurrentWidth && y2 >= 0 && y2 < targetCurrentHeight) {
                    final double lowerLeftX = transformation_x_1[ybase][xbase];
                    final double lowerLeftY = transformation_y_1[ybase][xbase];
                    final int xp1 = (xbase < (targetCurrentWidth - 1)) ? xbase + 1 : xbase;
                    final int yp1 = (ybase < (targetCurrentHeight - 1)) ? ybase + 1 : ybase;
                    final double lowerRightX = transformation_x_1[ybase][xp1];
                    final double lowerRightY = transformation_y_1[ybase][xp1];
                    final double upperRightX = transformation_x_1[yp1][xp1];
                    final double upperRightY = transformation_y_1[yp1][xp1];
                    final double upperLeftX = transformation_x_1[yp1][xbase];
                    final double upperLeftY = transformation_y_1[yp1][xbase];
                    final double upperAverageX = upperLeftX + xFraction * (upperRightX - upperLeftX);
                    final double upperAverageY = upperLeftY + xFraction * (upperRightY - upperLeftY);
                    final double lowerAverageX = lowerLeftX + xFraction * (lowerRightX - lowerLeftX);
                    final double lowerAverageY = lowerLeftY + xFraction * (lowerRightY - lowerLeftY);
                    outputTransformation_x[v][u] = lowerAverageX + yFraction * (upperAverageX - lowerAverageX);
                    outputTransformation_y[v][u] = lowerAverageY + yFraction * (upperAverageY - lowerAverageY);
                } else {
                    outputTransformation_x[v][u] = x2;
                    outputTransformation_y[v][u] = y2;
                }
            }
        }
    }

    /**
	 * Compose two elastic deformations into a raw deformation at pixel level.
	 *
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx1 first transformation x- B-spline coefficients
	 * @param cy1 first transformation y- B-spline coefficients
	 * @param cx2 second transformation x- B-spline coefficients
	 * @param cy2 second transformation y- B-spline coefficients
	 * @param outputTransformation_x output transformation coordinates in y-axis
	 * @param outputTransformation_y output transformation coordinates in y-axis
	 */
    public static void composeElasticTransformationsAtPixelLevel(ImagePlus targetImp, int intervals, double[][] cx1, double[][] cy1, double[][] cx2, double[][] cy2, double[][] outputTransformation_x, double[][] outputTransformation_y) {
        int targetCurrentHeight = targetImp.getProcessor().getHeight();
        int targetCurrentWidth = targetImp.getProcessor().getWidth();
        double[][] transformation_x_1 = new double[targetCurrentHeight][targetCurrentWidth];
        double[][] transformation_y_1 = new double[targetCurrentHeight][targetCurrentWidth];
        double[][] transformation_x_2 = new double[targetCurrentHeight][targetCurrentWidth];
        double[][] transformation_y_2 = new double[targetCurrentHeight][targetCurrentWidth];
        int cYdim = intervals + 3;
        int cXdim = cYdim;
        int Nk = cYdim * cXdim;
        int twiceNk = 2 * Nk;
        double c1[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c1[n] = cx1[i][j];
            c1[n + Nk] = cy1[i][j];
        }
        BSplineModel swx1 = new BSplineModel(c1, cYdim, cXdim, 0);
        BSplineModel swy1 = new BSplineModel(c1, cYdim, cXdim, Nk);
        double c2[] = new double[twiceNk];
        for (int n = 0, i = 0; i < cYdim; i++) for (int j = 0; j < cYdim; j++, n++) {
            c2[n] = cx2[i][j];
            c2[n + Nk] = cy2[i][j];
        }
        BSplineModel swx2 = new BSplineModel(c2, cYdim, cXdim, 0);
        BSplineModel swy2 = new BSplineModel(c2, cYdim, cXdim, Nk);
        swx1.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swy1.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swx2.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        swy2.precomputed_prepareForInterpolation(targetCurrentHeight, targetCurrentWidth, intervals);
        for (int v = 0; v < targetCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < targetCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx1.prepareForInterpolation(tu, tv, false);
                transformation_x_1[v][u] = swx1.interpolateI();
                swy1.prepareForInterpolation(tu, tv, false);
                transformation_y_1[v][u] = swy1.interpolateI();
            }
        }
        for (int v = 0; v < targetCurrentHeight; v++) {
            final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
            for (int u = 0; u < targetCurrentWidth; u++) {
                final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                swx2.prepareForInterpolation(tu, tv, false);
                transformation_x_2[v][u] = swx2.interpolateI();
                swy2.prepareForInterpolation(tu, tv, false);
                transformation_y_2[v][u] = swy2.interpolateI();
            }
        }
        MiscTools.composeRawTransformations(targetCurrentWidth, targetCurrentHeight, transformation_x_1, transformation_y_1, transformation_x_2, transformation_y_2, outputTransformation_x, outputTransformation_y);
    }

    /**
	 * Compose two raw transformations (Bilinear interpolation)
	 *
	 * @param width image width
	 * @param height image height
	 * @param transformation_x_1 first transformation coordinates in x-axis
	 * @param transformation_y_1 first transformation coordinates in y-axis
	 * @param transformation_x_2 second transformation coordinates in x-axis
	 * @param transformation_y_2 second transformation coordinates in y-axis
	 * @param outputTransformation_x output transformation coordinates in y-axis
	 * @param outputTransformation_y output transformation coordinates in y-axis
	 */
    public static void composeRawTransformations(int width, int height, double[][] transformation_x_1, double[][] transformation_y_1, double[][] transformation_x_2, double[][] transformation_y_2, double[][] outputTransformation_x, double[][] outputTransformation_y) {
        for (int i = 0; i < height; i++) for (int j = 0; j < width; j++) {
            double dX = transformation_x_2[i][j];
            double dY = transformation_y_2[i][j];
            int xbase = (int) dX;
            int ybase = (int) dY;
            double xFraction = dX - xbase;
            double yFraction = dY - ybase;
            if (dX >= 0 && dX < width && dY >= 0 && dY < height) {
                double lowerLeftX = transformation_x_1[ybase][xbase];
                double lowerLeftY = transformation_y_1[ybase][xbase];
                int xp1 = (xbase < (width - 1)) ? xbase + 1 : xbase;
                int yp1 = (ybase < (height - 1)) ? ybase + 1 : ybase;
                double lowerRightX = transformation_x_1[ybase][xp1];
                double lowerRightY = transformation_y_1[ybase][xp1];
                double upperRightX = transformation_x_1[yp1][xp1];
                double upperRightY = transformation_y_1[yp1][xp1];
                double upperLeftX = transformation_x_1[yp1][xbase];
                double upperLeftY = transformation_y_1[yp1][xbase];
                double upperAverageX = upperLeftX + xFraction * (upperRightX - upperLeftX);
                double upperAverageY = upperLeftY + xFraction * (upperRightY - upperLeftY);
                double lowerAverageX = lowerLeftX + xFraction * (lowerRightX - lowerLeftX);
                double lowerAverageY = lowerLeftY + xFraction * (lowerRightY - lowerLeftY);
                outputTransformation_x[i][j] = lowerAverageX + yFraction * (upperAverageX - lowerAverageX);
                outputTransformation_y[i][j] = lowerAverageY + yFraction * (upperAverageY - lowerAverageY);
            } else {
                outputTransformation_x[i][j] = dX;
                outputTransformation_y[i][j] = dY;
            }
        }
    }

    /**
	 * Save the elastic transformation.
	 *
	 * @param intervals number of intervals in the deformation
	 * @param cx x- deformation coefficients
	 * @param cy y- deformation coefficients
	 * @param filename transformation file name
	 */
    public static void saveElasticTransformation(int intervals, double[][] cx, double[][] cy, String filename) {
        try {
            final FileWriter fw = new FileWriter(filename);
            String aux;
            fw.write("Intervals=" + intervals + "\n\n");
            fw.write("X Coeffs -----------------------------------\n");
            for (int i = 0; i < intervals + 3; i++) {
                for (int j = 0; j < intervals + 3; j++) {
                    aux = "" + cx[i][j];
                    while (aux.length() < 21) aux = " " + aux;
                    fw.write(aux + " ");
                }
                fw.write("\n");
            }
            fw.write("\n");
            fw.write("Y Coeffs -----------------------------------\n");
            for (int i = 0; i < intervals + 3; i++) {
                for (int j = 0; j < intervals + 3; j++) {
                    aux = "" + cy[i][j];
                    while (aux.length() < 21) aux = " " + aux;
                    fw.write(aux + " ");
                }
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
        } catch (SecurityException e) {
            IJ.error("Security exception" + e);
        }
    }

    /**
	 * Save a raw transformation
	 *
	 * @param filename raw transformation file name
	 * @param width image width
	 * @param height image height
	 * @param transformation_x transformation coordinates in x-axis
	 * @param transformation_y transformation coordinates in y-axis
	 */
    public static void saveRawTransformation(String filename, int width, int height, double[][] transformation_x, double[][] transformation_y) {
        if (filename == null || filename.equals("")) {
            String path = "";
            final OpenDialog od = new OpenDialog("Save Transformation", "");
            path = od.getDirectory();
            filename = od.getFileName();
            if ((path == null) || (filename == null)) return;
            filename = path + filename;
        }
        try {
            final FileWriter fw = new FileWriter(filename);
            String aux;
            fw.write("Width=" + width + "\n");
            fw.write("Height=" + height + "\n\n");
            fw.write("X Trans -----------------------------------\n");
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    aux = "" + transformation_x[i][j];
                    while (aux.length() < 21) aux = " " + aux;
                    fw.write(aux + " ");
                }
                fw.write("\n");
            }
            fw.write("\n");
            fw.write("Y Trans -----------------------------------\n");
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    aux = "" + transformation_y[i][j];
                    while (aux.length() < 21) aux = " " + aux;
                    fw.write(aux + " ");
                }
                fw.write("\n");
            }
            fw.close();
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
        } catch (SecurityException e) {
            IJ.error("Security exception" + e);
        }
    }

    /**
	 * Read the number of intervals of a transformation from a file.
	 *
	 * @param filename transformation file name
	 * @return number of intervals
	 */
    public static int numberOfIntervalsOfTransformation(String filename) {
        try {
            final FileReader fr = new FileReader(filename);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            line = br.readLine();
            int lineN = 1;
            StringTokenizer st = new StringTokenizer(line, "=");
            if (st.countTokens() != 2) {
                fr.close();
                IJ.write("Line " + lineN + "+: Cannot read number of intervals");
                return -1;
            }
            st.nextToken();
            int intervals = Integer.valueOf(st.nextToken()).intValue();
            fr.close();
            return intervals;
        } catch (FileNotFoundException e) {
            IJ.error("File not found exception" + e);
            return -1;
        } catch (IOException e) {
            IJ.error("IOException exception" + e);
            return -1;
        } catch (NumberFormatException e) {
            IJ.error("Number format exception" + e);
            return -1;
        }
    }

    /**
	 * Plot a point in a canvas.
	 *
	 * @param canvas canvas where we are painting
	 * @param x x- coordinate for the point
	 * @param y y- coordinate for the point
	 * @param color point color
	 */
    public static void Point(double[][] canvas, int y, int x, double color) {
        if (y < 0 || y >= canvas.length) return;
        if (x < 0 || x >= canvas[0].length) return;
        canvas[y][x] = color;
    }

    /**
	 * Print a matrix in the command line.
	 *
	 * @param title matrix title
	 * @param array matrix to be printed
	 */
    public static void printMatrix(final String title, final double[][] array) {
        int Ydim = array.length;
        int Xdim = array[0].length;
        System.out.println(title);
        for (int i = 0; i < Ydim; i++) {
            for (int j = 0; j < Xdim; j++) System.out.print(array[i][j] + " ");
            System.out.println();
        }
    }

    /**
	 * Show an image in a new bUnwarpJ window.
	 *
	 * @param title image title
	 * @param array image in a double array
	 * @param Ydim image height
	 * @param Xdim image width
	 */
    public static void showImage(final String title, final double[] array, final int Ydim, final int Xdim) {
        final FloatProcessor fp = new FloatProcessor(Xdim, Ydim);
        int ij = 0;
        for (int i = 0; i < Ydim; i++) for (int j = 0; j < Xdim; j++, ij++) fp.putPixelValue(j, i, array[ij]);
        fp.resetMinAndMax();
        final ImagePlus ip = new ImagePlus(title, fp);
        ip.updateImage();
        ip.show();
    }

    /**
	 * Show an image in a new bUnwarpJ window.
	 *
	 * @param title image title
	 * @param array image in a double array
	 */
    public static void showImage(final String title, final double[][] array) {
        int Ydim = array.length;
        int Xdim = array[0].length;
        final FloatProcessor fp = new FloatProcessor(Xdim, Ydim);
        for (int i = 0; i < Ydim; i++) for (int j = 0; j < Xdim; j++) fp.putPixelValue(j, i, array[i][j]);
        fp.resetMinAndMax();
        final ImagePlus ip = new ImagePlus(title, fp);
        ip.updateImage();
        ip.show();
    }

    /**
	 * Adapt B-spline coefficients to a scale factor
	 * @param xScale
	 * @param yScale
	 * @param intervals
	 * @param cx
	 * @param cy
	 */
    public static void adaptCoefficients(double xScale, double yScale, int intervals, double[][] cx, double[][] cy) {
        for (int i = 0; i < (intervals + 3); i++) for (int j = 0; j < (intervals + 3); j++) {
            cx[i][j] *= xScale;
            cy[i][j] *= yScale;
        }
    }

    /**
	 * Apply a given B-spline transformation to the source (gray-scale) image.
	 * The source image is modified. The target image is used to know
	 * the output size (Multi-thread version).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param intervals intervals in the deformation
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
	 */
    public static void applyTransformationToSourceMT(ImagePlus sourceImp, ImagePlus targetImp, int intervals, double[][] cx, double[][] cy) {
        BSplineModel source = new BSplineModel(sourceImp.getProcessor(), false, 1);
        ImageProcessor result_imp = applyTransformationMT(sourceImp, targetImp, source, intervals, cx, cy);
        sourceImp.setProcessor(sourceImp.getTitle(), result_imp);
        sourceImp.updateImage();
    }

    /**
	 * Apply a given B-spline transformation to the source (gray-scale) image.
	 * The source image is modified. The target image is used to know
	 * the output size (Multi-thread version).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param source source image model
	 * @param intervals intervals in the deformation
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
	 */
    public static void applyTransformationToSourceMT(ImagePlus sourceImp, ImagePlus targetImp, BSplineModel source, int intervals, double[][] cx, double[][] cy) {
        ImageProcessor result_imp = applyTransformationMT(sourceImp, targetImp, source, intervals, cx, cy);
        sourceImp.setProcessor(sourceImp.getTitle(), result_imp);
        sourceImp.updateImage();
    }

    /**
	 * Apply a given B-spline transformation to the source (gray-scale) image.
	 * The result image is return. The target image is used to know
	 * the output size (Multi-thread version).
	 *
	 * @param sourceImp source image representation
	 * @param targetImp target image representation
	 * @param source source image model
	 * @param intervals intervals in the deformation
	 * @param cx x- B-spline coefficients
	 * @param cy y- B-spline coefficients
	 * 
	 * @return result transformed image
	 */
    public static ImageProcessor applyTransformationMT(ImagePlus sourceImp, ImagePlus targetImp, BSplineModel source, int intervals, double[][] cx, double[][] cy) {
        final int targetHeight = targetImp.getProcessor().getHeight();
        final int targetWidth = targetImp.getProcessor().getWidth();
        BSplineModel swx = new BSplineModel(cx);
        BSplineModel swy = new BSplineModel(cy);
        if (!(sourceImp.getProcessor() instanceof ColorProcessor)) {
            source.startPyramids();
            try {
                source.getThread().join();
            } catch (InterruptedException e) {
                IJ.error("Unexpected interruption exception " + e);
            }
            FloatProcessor fp = new FloatProcessor(targetWidth, targetHeight);
            int nproc = Runtime.getRuntime().availableProcessors();
            int block_height = targetHeight / nproc;
            if (targetHeight % 2 != 0) block_height++;
            int nThreads = nproc;
            Thread[] threads = new Thread[nThreads];
            Rectangle[] rects = new Rectangle[nThreads];
            FloatProcessor[] fp_tile = new FloatProcessor[nThreads];
            for (int i = 0; i < nThreads; i++) {
                int y_start = i * block_height;
                if (nThreads - 1 == i) block_height = targetHeight - i * block_height;
                rects[i] = new Rectangle(0, y_start, targetWidth, block_height);
                fp_tile[i] = new FloatProcessor(rects[i].width, rects[i].height);
                threads[i] = new Thread(new GrayscaleApplyTransformTile(swx, swy, source, targetWidth, targetHeight, intervals, rects[i], fp_tile[i]));
                threads[i].start();
            }
            for (int i = 0; i < nThreads; i++) {
                try {
                    threads[i].join();
                    threads[i] = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < nThreads; i++) {
                fp.insert(fp_tile[i], rects[i].x, rects[i].y);
                fp_tile[i] = null;
                rects[i] = null;
            }
            fp.resetMinAndMax();
            return fp;
        } else {
            BSplineModel sourceR = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(0, null), false, 1);
            sourceR.setPyramidDepth(0);
            sourceR.startPyramids();
            BSplineModel sourceG = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(1, null), false, 1);
            sourceG.setPyramidDepth(0);
            sourceG.startPyramids();
            BSplineModel sourceB = new BSplineModel(((ColorProcessor) (sourceImp.getProcessor())).toFloat(2, null), false, 1);
            sourceB.setPyramidDepth(0);
            sourceB.startPyramids();
            try {
                sourceR.getThread().join();
                sourceG.getThread().join();
                sourceB.getThread().join();
            } catch (InterruptedException e) {
                IJ.error("Unexpected interruption exception " + e);
            }
            ColorProcessor cp = new ColorProcessor(targetWidth, targetHeight);
            FloatProcessor fpR = new FloatProcessor(targetWidth, targetHeight);
            FloatProcessor fpG = new FloatProcessor(targetWidth, targetHeight);
            FloatProcessor fpB = new FloatProcessor(targetWidth, targetHeight);
            int nproc = Runtime.getRuntime().availableProcessors();
            int block_height = targetHeight / nproc;
            if (targetHeight % 2 != 0) block_height++;
            int nThreads = nproc;
            Thread[] threads = new Thread[nThreads];
            Rectangle[] rects = new Rectangle[nThreads];
            FloatProcessor[] fpR_tile = new FloatProcessor[nThreads];
            FloatProcessor[] fpG_tile = new FloatProcessor[nThreads];
            FloatProcessor[] fpB_tile = new FloatProcessor[nThreads];
            for (int i = 0; i < nThreads; i++) {
                int y_start = i * block_height;
                if (nThreads - 1 == i) block_height = targetHeight - i * block_height;
                rects[i] = new Rectangle(0, y_start, targetWidth, block_height);
                fpR_tile[i] = new FloatProcessor(rects[i].width, rects[i].height);
                fpG_tile[i] = new FloatProcessor(rects[i].width, rects[i].height);
                fpB_tile[i] = new FloatProcessor(rects[i].width, rects[i].height);
                threads[i] = new Thread(new ColorApplyTransformTile(swx, swy, sourceR, sourceG, sourceB, targetWidth, targetHeight, intervals, rects[i], fpR_tile[i], fpG_tile[i], fpB_tile[i]));
                threads[i].start();
            }
            for (int i = 0; i < nThreads; i++) {
                try {
                    threads[i].join();
                    threads[i] = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < nThreads; i++) {
                fpR.insert(fpR_tile[i], rects[i].x, rects[i].y);
                fpG.insert(fpG_tile[i], rects[i].x, rects[i].y);
                fpB.insert(fpB_tile[i], rects[i].x, rects[i].y);
                fpR_tile[i] = null;
                fpG_tile[i] = null;
                fpB_tile[i] = null;
                rects[i] = null;
            }
            cp.setPixels(0, fpR);
            cp.setPixels(1, fpG);
            cp.setPixels(2, fpB);
            cp.resetMinAndMax();
            return cp;
        }
    }

    /**
	 *  Class to apply transformation to grayscale images in a concurrent way
	 * 	 
	 */
    private static class GrayscaleApplyTransformTile implements Runnable {

        /** B-spline deformation in x */
        final BSplineModel swx;

        /** B-spline deformation in y */
        final BSplineModel swy;

        /** current source image */
        final BSplineModel source;

        /** target current width */
        final int targetCurrentWidth;

        /** target current height */
        final int targetCurrentHeight;

        /** number of intervals between B-spline coefficients */
        final int intervals;

        /** area of the image to be transformed in this thread */
        final Rectangle rect;

        /** resulting float processor containing the transformed area */
        private final FloatProcessor fp;

        /**
		 * Constructor for grayscale image transform 
		 * @param swx B-spline deformation in x
		 * @param swy B-spline deformation in y
		 * @param source current source image
		 * @param targetCurrentWidth target current width 
		 * @param targetCurrentHeight target current height
		 * @param intervals number of intervals between B-spline coefficients
		 * @param rect rectangle containing the area of the image to be transformed
		 * @param fp resulting float processor (output)
		 */
        GrayscaleApplyTransformTile(BSplineModel swx, BSplineModel swy, BSplineModel source, int targetCurrentWidth, int targetCurrentHeight, int intervals, Rectangle rect, FloatProcessor fp) {
            this.swx = swx;
            this.swy = swy;
            this.source = source;
            this.targetCurrentWidth = targetCurrentWidth;
            this.targetCurrentHeight = targetCurrentHeight;
            this.intervals = intervals;
            this.rect = rect;
            this.fp = fp;
        }

        /**
		 * Run method to update the intermediate window. Only the part defined by
		 * the rectangle will be updated (in this thread).
		 */
        public void run() {
            int uv = rect.y * rect.width + rect.x;
            int auxTargetHeight = rect.y + rect.height;
            int auxTargetWidth = rect.x + rect.width;
            float[] fp_array = (float[]) fp.getPixels();
            final int sourceWidth = source.getWidth();
            final int sourceHeight = source.getHeight();
            for (int v_rect = 0, v = rect.y; v < auxTargetHeight; v++, v_rect++) {
                final int v_offset = v_rect * rect.width;
                final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
                for (int u_rect = 0, u = rect.x; u < auxTargetWidth; u++, uv++, u_rect++) {
                    final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                    final double transformation_x_v_u = swx.prepareForInterpolationAndInterpolateI(tu, tv, false, false);
                    final double transformation_y_v_u = swy.prepareForInterpolationAndInterpolateI(tu, tv, false, false);
                    final double x = transformation_x_v_u;
                    final double y = transformation_y_v_u;
                    if (x >= 0 && x < sourceWidth && y >= 0 && y < sourceHeight) {
                        double sval = source.prepareForInterpolationAndInterpolateI(x, y, false, false);
                        fp_array[u_rect + v_offset] = (float) sval;
                    } else {
                        fp_array[u_rect + v_offset] = 0;
                    }
                }
            }
        }
    }

    /**
	 *  Class to apply transformation to color images in a concurrent way
	 * 	 
	 */
    private static class ColorApplyTransformTile implements Runnable {

        /** B-spline deformation in x */
        final BSplineModel swx;

        /** B-spline deformation in y */
        final BSplineModel swy;

        /** red channel of the source image */
        final BSplineModel sourceR;

        /** green channel of the source image */
        final BSplineModel sourceG;

        /** blue channel of the source image */
        final BSplineModel sourceB;

        /** target current width */
        final int targetCurrentWidth;

        /** target current height */
        final int targetCurrentHeight;

        /** number of intervals between B-spline coefficients */
        final int intervals;

        /** area of the image to be transformed */
        final Rectangle rect;

        /** resulting float processor for the red channel */
        private final FloatProcessor fpR;

        /** resulting float processor for the green channel */
        private final FloatProcessor fpG;

        /** resulting float processor for the blue channel */
        private final FloatProcessor fpB;

        /**
		 * Constructor for color image transform 
		 * 
		 * @param swx B-spline deformation in x
		 * @param swy B-spline deformation in y
		 * @param sourceR red source image
		 * @param sourceG green source image
		 * @param sourceB blue source image
		 * @param targetCurrentWidth target current width
		 * @param targetCurrentHeight target current height
		 * @param intervals number of intervals between B-spline coefficients
		 * @param rect area of the image to be transformed
		 * @param fpR red channel processor to be updated
		 * @param fpG green channel processor to be updated
		 * @param fpB blue channel processor to be updated
		 */
        ColorApplyTransformTile(BSplineModel swx, BSplineModel swy, BSplineModel sourceR, BSplineModel sourceG, BSplineModel sourceB, int targetCurrentWidth, int targetCurrentHeight, int intervals, Rectangle rect, FloatProcessor fpR, FloatProcessor fpG, FloatProcessor fpB) {
            this.swx = swx;
            this.swy = swy;
            this.sourceR = sourceR;
            this.sourceG = sourceG;
            this.sourceB = sourceB;
            this.targetCurrentWidth = targetCurrentWidth;
            this.targetCurrentHeight = targetCurrentHeight;
            this.intervals = intervals;
            this.rect = rect;
            this.fpR = fpR;
            this.fpG = fpG;
            this.fpB = fpB;
        }

        /**
		 * Run method to update the intermediate window. Only the part defined by
		 * the rectangle will be updated (in this thread).
		 */
        public void run() {
            int uv = rect.y * rect.width + rect.x;
            int auxTargetHeight = rect.y + rect.height;
            int auxTargetWidth = rect.x + rect.width;
            float[] fpR_array = (float[]) fpR.getPixels();
            float[] fpG_array = (float[]) fpG.getPixels();
            float[] fpB_array = (float[]) fpB.getPixels();
            final int sourceWidth = sourceR.getWidth();
            final int sourceHeight = sourceR.getHeight();
            for (int v_rect = 0, v = rect.y; v < auxTargetHeight; v++, v_rect++) {
                final int v_offset = v_rect * rect.width;
                final double tv = (double) (v * intervals) / (double) (targetCurrentHeight - 1) + 1.0F;
                for (int u_rect = 0, u = rect.x; u < auxTargetWidth; u++, uv++, u_rect++) {
                    final double tu = (double) (u * intervals) / (double) (targetCurrentWidth - 1) + 1.0F;
                    final double x = swx.prepareForInterpolationAndInterpolateI(tu, tv, false, false);
                    final double y = swy.prepareForInterpolationAndInterpolateI(tu, tv, false, false);
                    if (x >= 0 && x < sourceWidth && y >= 0 && y < sourceHeight) {
                        fpR_array[u_rect + v_offset] = (float) (sourceR.prepareForInterpolationAndInterpolateI(x, y, false, false));
                        fpG_array[u_rect + v_offset] = (float) (sourceG.prepareForInterpolationAndInterpolateI(x, y, false, false));
                        fpB_array[u_rect + v_offset] = (float) (sourceB.prepareForInterpolationAndInterpolateI(x, y, false, false));
                    } else {
                        fpR_array[u_rect + v_offset] = 0;
                        fpG_array[u_rect + v_offset] = 0;
                        fpB_array[u_rect + v_offset] = 0;
                    }
                }
            }
        }
    }
}
