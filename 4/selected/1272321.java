package co.edu.unal.ungrid.transformation.dft;

import co.edu.unal.ungrid.util.ConcurrencyHelper;

/**
 * Benchmark of single precision FFT's
 */
public class BenchmarkFloatFft {

    private BenchmarkFloatFft() {
    }

    public static void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.out.println("args[" + i + "]:" + args[i]);
        }
        if ((args == null) || (args.length != 10)) {
            System.out.println("Parameters: <number of threads> <THREADS_BEGIN_N_2D> <THREADS_BEGIN_N_3D> <number of iterations> <perform warm-up> <perform scaling> <number of sizes> <initial exponent for 1D transforms> <initial exponent for 2D transforms> <initial exponent for 3D transforms>");
            System.exit(-1);
        }
        nthread = Integer.parseInt(args[0]);
        ConcurrencyHelper.setThreadsMinSize2D(Integer.parseInt(args[1]));
        ConcurrencyHelper.setThreadsMinSize3D(Integer.parseInt(args[2]));
        niter = Integer.parseInt(args[3]);
        doWarmup = Boolean.parseBoolean(args[4]);
        doScaling = Boolean.parseBoolean(args[5]);
        nsize = Integer.parseInt(args[6]);
        initialExponent1D = Integer.parseInt(args[7]);
        initialExponent2D = Integer.parseInt(args[8]);
        initialExponent3D = Integer.parseInt(args[9]);
        ConcurrencyHelper.setNumberOfProcessors(nthread);
    }

    public static void benchmarkComplexForward_1D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Complex forward FFT 1D of size 2^" + exponent);
            FloatFft1D fft = new FloatFft1D(N);
            x = new float[2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_1D(2 * N, x);
                fft.complexForward(x);
                IoUtils.fillMatrix_1D(2 * N, x);
                fft.complexForward(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_1D(2 * N, x);
                elapsedTime = System.nanoTime();
                fft.complexForward(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatComplexForwardFFT_1D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkRealForward_1D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Real forward FFT 1D of size 2^" + exponent);
            FloatFft1D fft = new FloatFft1D(N);
            x = new float[2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_1D(N, x);
                fft.realForwardFull(x);
                IoUtils.fillMatrix_1D(N, x);
                fft.realForwardFull(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_1D(N, x);
                elapsedTime = System.nanoTime();
                fft.realForwardFull(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatRealForwardFFT_1D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkComplexForward_2D_input_1D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Complex forward FFT 2D (input 1D) of size 2^" + exponent + " x 2^" + exponent);
            FloatFft2D fft2 = new FloatFft2D(N, N);
            x = new float[N * 2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_2D(N, 2 * N, x);
                fft2.complexForward(x);
                IoUtils.fillMatrix_2D(N, 2 * N, x);
                fft2.complexForward(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_2D(N, 2 * N, x);
                elapsedTime = System.nanoTime();
                fft2.complexForward(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft2 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatComplexForwardFFT_2D_input_1D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkComplexForward_2D_input_2D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[][] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Complex forward FFT 2D (input 2D) of size 2^" + exponent + " x 2^" + exponent);
            FloatFft2D fft2 = new FloatFft2D(N, N);
            x = new float[N][2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_2D(N, 2 * N, x);
                fft2.complexForward(x);
                IoUtils.fillMatrix_2D(N, 2 * N, x);
                fft2.complexForward(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_2D(N, 2 * N, x);
                elapsedTime = System.nanoTime();
                fft2.complexForward(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft2 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatComplexForwardFFT_2D_input_2D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkRealForward_2D_input_1D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Real forward FFT 2D (input 1D) of size 2^" + exponent + " x 2^" + exponent);
            FloatFft2D fft2 = new FloatFft2D(N, N);
            x = new float[N * 2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_2D(N, N, x);
                fft2.realForwardFull(x);
                IoUtils.fillMatrix_2D(N, N, x);
                fft2.realForwardFull(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_2D(N, N, x);
                elapsedTime = System.nanoTime();
                fft2.realForwardFull(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft2 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatRealForwardFFT_2D_input_1D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkRealForward_2D_input_2D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[][] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Real forward FFT 2D (input 2D) of size 2^" + exponent + " x 2^" + exponent);
            FloatFft2D fft2 = new FloatFft2D(N, N);
            x = new float[N][2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_2D(N, N, x);
                fft2.realForwardFull(x);
                IoUtils.fillMatrix_2D(N, N, x);
                fft2.realForwardFull(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_2D(N, N, x);
                elapsedTime = System.nanoTime();
                fft2.realForwardFull(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft2 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatRealForwardFFT_2D_input_2D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkComplexForward_3D_input_1D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Complex forward FFT 3D (input 1D) of size 2^" + exponent + " x 2^" + exponent + " x 2^" + exponent);
            FloatFft3D fft3 = new FloatFft3D(N, N, N);
            x = new float[N * N * 2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_3D(N, N, 2 * N, x);
                fft3.complexForward(x);
                IoUtils.fillMatrix_3D(N, N, 2 * N, x);
                fft3.complexForward(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_3D(N, N, 2 * N, x);
                elapsedTime = System.nanoTime();
                fft3.complexForward(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft3 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatComplexForwardFFT_3D_input_1D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkComplexForward_3D_input_3D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[][][] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Complex forward FFT 3D (input 3D) of size 2^" + exponent + " x 2^" + exponent + " x 2^" + exponent);
            FloatFft3D fft3 = new FloatFft3D(N, N, N);
            x = new float[N][N][2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_3D(N, N, 2 * N, x);
                fft3.complexForward(x);
                IoUtils.fillMatrix_3D(N, N, 2 * N, x);
                fft3.complexForward(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_3D(N, N, 2 * N, x);
                elapsedTime = System.nanoTime();
                fft3.complexForward(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft3 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatComplexForwardFFT_3D_input_3D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkRealForward_3D_input_1D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Real forward FFT 3D (input 1D) of size 2^" + exponent + " x 2^" + exponent + " x 2^" + exponent);
            FloatFft3D fft3 = new FloatFft3D(N, N, N);
            x = new float[N * N * 2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_3D(N, N, N, x);
                fft3.realForwardFull(x);
                IoUtils.fillMatrix_3D(N, N, N, x);
                fft3.realForwardFull(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_3D(N, N, N, x);
                elapsedTime = System.nanoTime();
                fft3.realForwardFull(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft3 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatRealForwardFFT_3D_input_1D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void benchmarkRealForward_3D_input_3D(int init_exp) {
        int[] sizes = new int[nsize];
        double[] times = new double[nsize];
        float[][][] x;
        for (int i = 0; i < nsize; i++) {
            int exponent = init_exp + i;
            int N = (int) Math.pow(2, exponent);
            sizes[i] = N;
            System.out.println("Real forward FFT 3D (input 3D) of size 2^" + exponent + " x 2^" + exponent + " x 2^" + exponent);
            FloatFft3D fft3 = new FloatFft3D(N, N, N);
            x = new float[N][N][2 * N];
            if (doWarmup) {
                IoUtils.fillMatrix_3D(N, N, N, x);
                fft3.realForwardFull(x);
                IoUtils.fillMatrix_3D(N, N, N, x);
                fft3.realForwardFull(x);
            }
            float av_time = 0;
            long elapsedTime = 0;
            for (int j = 0; j < niter; j++) {
                IoUtils.fillMatrix_3D(N, N, N, x);
                elapsedTime = System.nanoTime();
                fft3.realForwardFull(x);
                elapsedTime = System.nanoTime() - elapsedTime;
                av_time = av_time + elapsedTime;
            }
            times[i] = (double) av_time / 1000000.0 / (double) niter;
            System.out.println("\tAverage execution time: " + String.format("%.2f", av_time / 1000000.0 / (float) niter) + " msec");
            x = null;
            fft3 = null;
            System.gc();
        }
        IoUtils.writeFFTBenchmarkResultsToFile("benchmarkFloatRealForwardFFT_3D_input_3D.txt", nthread, niter, doWarmup, doScaling, sizes, times);
    }

    public static void main(String[] args) {
        parseArguments(args);
        benchmarkComplexForward_1D(initialExponent1D);
        benchmarkRealForward_1D(initialExponent1D);
        benchmarkComplexForward_2D_input_1D(initialExponent2D);
        benchmarkComplexForward_2D_input_2D(initialExponent2D);
        benchmarkRealForward_2D_input_1D(initialExponent2D);
        benchmarkRealForward_2D_input_2D(initialExponent2D);
        benchmarkComplexForward_3D_input_1D(initialExponent3D);
        benchmarkComplexForward_3D_input_3D(initialExponent3D);
        benchmarkRealForward_3D_input_1D(initialExponent3D);
        benchmarkRealForward_3D_input_3D(initialExponent3D);
    }

    private static int nthread = 2;

    private static int nsize = 6;

    private static int niter = 200;

    private static int initialExponent1D = 17;

    private static int initialExponent2D = 7;

    private static int initialExponent3D = 2;

    private static boolean doWarmup = true;

    private static boolean doScaling = false;
}
