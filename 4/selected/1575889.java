package edu.emory.mathcs.restoretools.iterative;

import ij.ImagePlus;
import ij.io.Opener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import cern.colt.matrix.tdouble.algo.solver.HyBRInnerSolver;
import cern.colt.matrix.tdouble.algo.solver.HyBRRegularizationMethod;
import edu.emory.mathcs.restoretools.Enums.OutputType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.BoundaryType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.PreconditionerType;
import edu.emory.mathcs.restoretools.iterative.IterativeEnums.ResizingType;
import edu.emory.mathcs.restoretools.iterative.cgls.CGLSDoubleIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.cgls.CGLSFloatIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.cgls.CGLSOptions;
import edu.emory.mathcs.restoretools.iterative.hybr.HyBRDoubleIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.hybr.HyBRFloatIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.hybr.HyBROptions;
import edu.emory.mathcs.restoretools.iterative.mrnsd.MRNSDDoubleIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.mrnsd.MRNSDFloatIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.mrnsd.MRNSDOptions;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLDoubleIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLFloatIterativeDeconvolver3D;
import edu.emory.mathcs.restoretools.iterative.wpl.WPLOptions;
import edu.emory.mathcs.utils.ConcurrencyUtils;

/**
 * Benchmark for Parallel Iterative Deconvolution 3D
 * 
 * @author Piotr Wendykier (piotr.wendykier@gmail.com)
 * 
 */
public class Benchmark3D {

    private static final String path = "/home/pwendyk/images/iterative/";

    private static final String blur_image = "head-blur.tif";

    private static final String psf_image = "head-psf.tif";

    private static final BoundaryType boundary = BoundaryType.REFLEXIVE;

    private static final ResizingType resizing = ResizingType.AUTO;

    private static final OutputType output = OutputType.FLOAT;

    private static final int NITER = 10;

    private static final int MAXITER = 5;

    private static final double PREC_TOL = 0.001;

    private static final String format = "%.2f";

    public static void benchmarkDoubleCGLS3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking DoubleCGLS3D using " + threads + " threads");
        CGLSOptions options = new CGLSOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            CGLSDoubleIterativeDeconvolver3D cgls = new CGLSDoubleIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.NONE, 0, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = cgls.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            cgls = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleCGLS3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoublePCGLS3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking DoublePCGLS3D using " + threads + " threads");
        CGLSOptions options = new CGLSOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            CGLSDoubleIterativeDeconvolver3D cgls = new CGLSDoubleIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.FFT, PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = cgls.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            cgls = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoublePCGLS3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void benchmarkDoubleMRNSD3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking DoubleMRNSD3D using " + threads + " threads");
        MRNSDOptions options = new MRNSDOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            MRNSDDoubleIterativeDeconvolver3D mrnsd = new MRNSDDoubleIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.NONE, PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = mrnsd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            mrnsd = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleMRNSD3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoublePMRNSD3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking DoublePMRNSD3D using " + threads + " threads");
        MRNSDOptions options = new MRNSDOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            MRNSDDoubleIterativeDeconvolver3D mrnsd = new MRNSDDoubleIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.FFT, PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = mrnsd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            mrnsd = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoublePMRNSD3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void benchmarkDoubleHyBR3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking DoubleHyBR3D using " + threads + " threads");
        HyBROptions options = new HyBROptions(HyBRInnerSolver.TIKHONOV, HyBRRegularizationMethod.ADAPTWGCV, 0, 0, false, 2, 0, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            HyBRDoubleIterativeDeconvolver3D hybr = new HyBRDoubleIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.NONE, PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = hybr.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            hybr = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleHyBR3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoublePHyBR3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking preconditioned DoubleHyBR3D using " + threads + " threads");
        HyBROptions options = new HyBROptions(HyBRInnerSolver.TIKHONOV, HyBRRegularizationMethod.ADAPTWGCV, 0, 0, false, 2, 0, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            HyBRDoubleIterativeDeconvolver3D hybr = new HyBRDoubleIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.FFT, PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = hybr.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            hybr = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoublePHyBR3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void benchmarkDoubleWPL3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking DoubleWPL3D using " + threads + " threads");
        WPLOptions options = new WPLOptions(0, 1.0, 1.0, true, false, false, 0, false, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            WPLDoubleIterativeDeconvolver3D wpl = new WPLDoubleIterativeDeconvolver3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = wpl.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            wpl = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoubleWPL3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkDoublePWPL3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking DoublePWPL3D using " + threads + " threads");
        WPLOptions options = new WPLOptions(0.01, 1.0, 1.0, true, false, false, 0, false, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            WPLDoubleIterativeDeconvolver3D wpl = new WPLDoubleIterativeDeconvolver3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = wpl.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            wpl = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("DoublePWPL3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void benchmarkFloatCGLS3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking FloatCGLS3D using " + threads + " threads");
        CGLSOptions options = new CGLSOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            CGLSFloatIterativeDeconvolver3D cgls = new CGLSFloatIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.NONE, 0, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = cgls.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            cgls = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatCGLS3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatPCGLS3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking FloatPCGLS3D using " + threads + " threads");
        CGLSOptions options = new CGLSOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            CGLSFloatIterativeDeconvolver3D cgls = new CGLSFloatIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.FFT, (float) PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = cgls.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            cgls = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatPCGLS3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void benchmarkFloatMRNSD3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking FloatMRNSD3D using " + threads + " threads");
        MRNSDOptions options = new MRNSDOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            MRNSDFloatIterativeDeconvolver3D mrnsd = new MRNSDFloatIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.NONE, (float) PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = mrnsd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            mrnsd = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatMRNSD3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatPMRNSD3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking FloatPMRNSD3D using " + threads + " threads");
        MRNSDOptions options = new MRNSDOptions(false, 0, false, 0, false);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            MRNSDFloatIterativeDeconvolver3D mrnsd = new MRNSDFloatIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.FFT, (float) PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = mrnsd.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            mrnsd = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatPMRNSD3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void benchmarkFloatHyBR3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking FloatHyBR3D using " + threads + " threads");
        HyBROptions options = new HyBROptions(HyBRInnerSolver.TIKHONOV, HyBRRegularizationMethod.ADAPTWGCV, 0, 0, false, 2, 0, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            HyBRFloatIterativeDeconvolver3D hybr = new HyBRFloatIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.NONE, (float) PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = hybr.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            hybr = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatHyBR3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatPHyBR3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking preconditioned FloatHyBR3D using " + threads + " threads");
        HyBROptions options = new HyBROptions(HyBRInnerSolver.TIKHONOV, HyBRRegularizationMethod.ADAPTWGCV, 0, 0, false, 2, 0, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            HyBRFloatIterativeDeconvolver3D hybr = new HyBRFloatIterativeDeconvolver3D(blurImage, psfImage, PreconditionerType.FFT, (float) PREC_TOL, boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = hybr.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            hybr = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatPHyBR3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void benchmarkFloatWPL3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking FloatWPL3D using " + threads + " threads");
        WPLOptions options = new WPLOptions(0, 1.0, 1.0, true, false, false, 0, false, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            WPLFloatIterativeDeconvolver3D wpl = new WPLFloatIterativeDeconvolver3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = wpl.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            wpl = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time: " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatWPL3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER);
    }

    public static void benchmarkFloatPWPL3D(int threads) {
        ConcurrencyUtils.setNumberOfThreads(threads);
        Opener o = new Opener();
        ImagePlus blurImage = o.openImage(path + blur_image);
        ImagePlus[][][] psfImage = new ImagePlus[1][1][1];
        psfImage[0][0][0] = o.openImage(path + psf_image);
        double av_time_deblur = 0;
        long elapsedTime_deblur = 0;
        System.out.println("Benchmarking FloatPWPL3D using " + threads + " threads");
        WPLOptions options = new WPLOptions(0.01, 1.0, 1.0, true, false, false, 0, false, false, false, 0);
        for (int i = 0; i < NITER; i++) {
            elapsedTime_deblur = System.nanoTime();
            WPLFloatIterativeDeconvolver3D wpl = new WPLFloatIterativeDeconvolver3D(blurImage, psfImage[0][0][0], boundary, resizing, output, MAXITER, false, options);
            ImagePlus imX = wpl.deconvolve();
            elapsedTime_deblur = System.nanoTime() - elapsedTime_deblur;
            av_time_deblur = av_time_deblur + elapsedTime_deblur;
            wpl = null;
            imX = null;
        }
        blurImage = null;
        psfImage = null;
        System.out.println("Average execution time (tol =  " + PREC_TOL + "): " + String.format(format, av_time_deblur / 1000000000.0 / (double) NITER) + " sec");
        writeResultsToFile("FloatPWPL3D_" + threads + "_threads.txt", (double) av_time_deblur / 1000000000.0 / (double) NITER, PREC_TOL);
    }

    public static void writeResultsToFile(String filename, double time_deblur) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write(new Date().toString());
            out.newLine();
            out.write("Number of processors: " + ConcurrencyUtils.getNumberOfThreads());
            out.newLine();
            out.write("deblur time: ");
            out.write(String.format(format, time_deblur));
            out.write(" seconds");
            out.newLine();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeResultsToFile(String filename, double time_deblur, double prec_tol) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));
            out.write(new Date().toString());
            out.newLine();
            out.write("Number of processors: " + ConcurrencyUtils.getNumberOfThreads());
            out.newLine();
            out.write("deblur time (tol = " + prec_tol + "): ");
            out.write(String.format(format, time_deblur));
            out.write(" seconds");
            out.newLine();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        benchmarkFloatPWPL3D(1);
        benchmarkFloatPWPL3D(2);
        benchmarkFloatPWPL3D(4);
        benchmarkFloatPWPL3D(8);
        benchmarkFloatPHyBR3D(1);
        benchmarkFloatPHyBR3D(2);
        benchmarkFloatPHyBR3D(4);
        benchmarkFloatPHyBR3D(8);
        benchmarkFloatPCGLS3D(1);
        benchmarkFloatPCGLS3D(2);
        benchmarkFloatPCGLS3D(4);
        benchmarkFloatPCGLS3D(8);
        benchmarkFloatPMRNSD3D(1);
        benchmarkFloatPMRNSD3D(2);
        benchmarkFloatPMRNSD3D(4);
        benchmarkFloatPMRNSD3D(8);
        System.exit(0);
    }
}
