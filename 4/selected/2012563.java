package edu.emory.mathcs.restoretools.spectral;

import ij.ImagePlus;
import ij.io.Opener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import cern.colt.matrix.tdouble.DoubleMatrix3D;
import cern.colt.matrix.tdouble.impl.DenseDoubleMatrix3D;
import cern.colt.matrix.tfloat.FloatMatrix3D;
import cern.colt.matrix.tfloat.impl.DenseFloatMatrix3D;
import edu.emory.mathcs.restoretools.Enums.OutputType;
import edu.emory.mathcs.restoretools.spectral.SpectralEnums.ResizingType;
import edu.emory.mathcs.restoretools.spectral.gtik.DoublePeriodicGeneralizedTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.gtik.DoubleReflexiveGeneralizedTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.gtik.FloatPeriodicGeneralizedTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.gtik.FloatReflexiveGeneralizedTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.tik.DoublePeriodicTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.tik.DoubleReflexiveTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.tik.FloatPeriodicTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.tik.FloatReflexiveTikhonov3D;
import edu.emory.mathcs.restoretools.spectral.tsvd.DoublePeriodicTruncatedSVD3D;
import edu.emory.mathcs.restoretools.spectral.tsvd.DoubleReflexiveTruncatedSVD3D;
import edu.emory.mathcs.restoretools.spectral.tsvd.FloatPeriodicTruncatedSVD3D;
import edu.emory.mathcs.restoretools.spectral.tsvd.FloatReflexiveTruncatedSVD3D;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Benchmark for Parallel Spectral Deconvolution 3D
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class Benchmark3D {

    private static final String path = "/home/pwendyk/images/spectral/";

    private static final String blur_image = "head-blur.tif";

    private static final String psf_image = "head-psf.tif";

    private static final DoubleMatrix3D doubleStencil = new DenseDoubleMatrix3D(3, 3, 3).assign(new double[] { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, -6, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 });

    private static final FloatMatrix3D floatStencil = new DenseFloatMatrix3D(3, 3, 3).assign(new float[] { 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, -6, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0 });

    private static final ResizingType resizing = ResizingType.NONE;

    private static final OutputType output = OutputType.FLOAT;

    private static final int NITER = 20;

    private static final int threshold = 0;

    private static final double double_regParam_deblur = 0.01;

    private static final double double_regParam_update = 0.02;

    private static final float float_regParam_deblur = 0.01f;

    private static final float float_regParam_update = 0.02f;

    private static final String format = "%.1f";

    public static void benchmarkDoubleTSVD_Periodic_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking DoubleTSVD_Periodic_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            DoublePeriodicTruncatedSVD3D tsvd = new DoublePeriodicTruncatedSVD3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new DoublePeriodicTruncatedSVD3D(blurImage, psfImage, resizing, output, false, double_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(double_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleTSVD_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoubleTSVD_Reflexive_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking DoubleTSVD_Reflexive_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            DoubleReflexiveTruncatedSVD3D tsvd = new DoubleReflexiveTruncatedSVD3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new DoubleReflexiveTruncatedSVD3D(blurImage, psfImage, resizing, output, false, double_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(double_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleTSVD_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoubleTikhonov_Periodic_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking DoubleTikhonov_Periodic_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            DoublePeriodicTikhonov3D tsvd = new DoublePeriodicTikhonov3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new DoublePeriodicTikhonov3D(blurImage, psfImage, resizing, output, false, double_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(double_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoubleTikhonov_Reflexive_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking DoubleTikhonov_Reflexive_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            DoubleReflexiveTikhonov3D tsvd = new DoubleReflexiveTikhonov3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new DoubleReflexiveTikhonov3D(blurImage, psfImage, resizing, output, false, double_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(double_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoubleGTikhonov_Periodic_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking DoubleGTikhonov_Periodic_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            DoublePeriodicGeneralizedTikhonov3D tsvd = new DoublePeriodicGeneralizedTikhonov3D(blurImage, psfImage, doubleStencil, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new DoublePeriodicGeneralizedTikhonov3D(blurImage, psfImage, doubleStencil, resizing, output, false, double_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(double_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleGTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoubleGTikhonov_Reflexive_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking DoubleGTikhonov_Reflexive_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            DoubleReflexiveGeneralizedTikhonov3D tsvd = new DoubleReflexiveGeneralizedTikhonov3D(blurImage, psfImage, doubleStencil, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new DoubleReflexiveGeneralizedTikhonov3D(blurImage, psfImage, doubleStencil, resizing, output, false, double_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(double_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleGTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatTSVD_Periodic_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking FloatTSVD_Periodic_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            FloatPeriodicTruncatedSVD3D tsvd = new FloatPeriodicTruncatedSVD3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new FloatPeriodicTruncatedSVD3D(blurImage, psfImage, resizing, output, false, float_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(float_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatTSVD_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatTSVD_Reflexive_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking FloatTSVD_Reflexive_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            FloatReflexiveTruncatedSVD3D tsvd = new FloatReflexiveTruncatedSVD3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new FloatReflexiveTruncatedSVD3D(blurImage, psfImage, resizing, output, false, float_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(float_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatTSVD_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatTikhonov_Periodic_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking FloatTikhonov_Periodic_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            FloatPeriodicTikhonov3D tsvd = new FloatPeriodicTikhonov3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new FloatPeriodicTikhonov3D(blurImage, psfImage, resizing, output, false, float_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(float_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatTikhonov_Reflexive_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking FloatTikhonov_Reflexive_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            FloatReflexiveTikhonov3D tsvd = new FloatReflexiveTikhonov3D(blurImage, psfImage, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new FloatReflexiveTikhonov3D(blurImage, psfImage, resizing, output, false, float_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(float_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatGTikhonov_Periodic_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking FloatGTikhonov_Periodic_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            FloatPeriodicGeneralizedTikhonov3D tsvd = new FloatPeriodicGeneralizedTikhonov3D(blurImage, psfImage, floatStencil, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new FloatPeriodicGeneralizedTikhonov3D(blurImage, psfImage, floatStencil, resizing, output, false, float_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(float_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatGTikhonov_Periodic_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatGTikhonov_Reflexive_3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus psfImage = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        double av_time_deblur_regParam = 0;
        double av_time_update = 0;
        long elapsedTime_deblur = 0;
        long elapsedTime_deblur_regParam = 0;
        long elapsedTime_update = 0;
        System.out.println("Benchmarking FloatGTikhonov_Reflexive_3D using " + threads + " threads");
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            FloatReflexiveGeneralizedTikhonov3D tsvd = new FloatReflexiveGeneralizedTikhonov3D(blurImage, psfImage, floatStencil, resizing, output, false, -1, threshold);
            ImagePlus imX = tsvd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            elapsedTime_deblur_regParam = System.nanoTime();
            tsvd = new FloatReflexiveGeneralizedTikhonov3D(blurImage, psfImage, floatStencil, resizing, output, false, float_regParam_deblur, threshold);
            imX = tsvd.deconvolve();
            elapsedTime_deblur_regParam = System.nanoTime() - elapsedTime_deblur_regParam;
            av_time_deblur_regParam = av_time_deblur_regParam + elapsedTime_deblur_regParam;
            elapsedTime_update = System.nanoTime();
            tsvd.update(float_regParam_update, threshold, imX);
            elapsedTime_update = System.nanoTime() - elapsedTime_update;
            av_time_update = av_time_update + elapsedTime_update;
            imX = null;
            tsvd = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (deblur()): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (deblur(regParam)): " + String.format(format, av_time_deblur_regParam / 1000000000.0 / (double) NITER) + " sec");
        System.out.println("Average execution time (update()): " + String.format(format, av_time_update / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatGTikhonov_Reflexive_3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, (double) av_time_deblur_regParam / 1000000000.0 / (double) NITER, (double) av_time_update / 1000000000.0 / (double) NITER);
    }

    public static void writeResultsToFile(String filename, double time_deblur, double time_deblur_regParam, double time_update) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write(new Date().toString());
            out.newLine();
            out.write("Number of processors: " + ConcurrencyUtils.getNumberOfThreads());
            out.newLine();
            out.write("deblur() time=");
            out.write(String.format(format, time_deblur));
            out.write(" seconds");
            out.newLine();
            out.write("deblur(regParam) time=");
            out.write(String.format(format, time_deblur_regParam));
            out.write(" seconds");
            out.newLine();
            out.write("update() time=");
            out.write(String.format(format, time_update));
            out.write(" seconds");
            out.newLine();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        benchmarkFloatTSVD_Reflexive_3D(1);
        benchmarkFloatTSVD_Reflexive_3D(2);
        benchmarkFloatTSVD_Reflexive_3D(4);
        benchmarkFloatTSVD_Reflexive_3D(8);
        benchmarkFloatTikhonov_Reflexive_3D(1);
        benchmarkFloatTikhonov_Reflexive_3D(2);
        benchmarkFloatTikhonov_Reflexive_3D(4);
        benchmarkFloatTikhonov_Reflexive_3D(8);
        benchmarkFloatGTikhonov_Reflexive_3D(1);
        benchmarkFloatGTikhonov_Reflexive_3D(2);
        benchmarkFloatGTikhonov_Reflexive_3D(4);
        benchmarkFloatGTikhonov_Reflexive_3D(8);
        System.exit(0);
    }
}
