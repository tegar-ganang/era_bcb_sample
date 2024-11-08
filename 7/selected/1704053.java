package shellkk.qiq.math.ml.hmm;

import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import shellkk.qiq.math.StopException;
import shellkk.qiq.math.StopHandle;
import shellkk.qiq.math.matrix.Maths;
import shellkk.qiq.math.matrix.Matrix;
import shellkk.qiq.math.ml.ArraySampleSet;
import shellkk.qiq.math.ml.LearnMachine;
import shellkk.qiq.math.ml.Sample;
import shellkk.qiq.math.ml.SampleSet;
import shellkk.qiq.math.ml.TrainException;

/**
 * Continuous hidden markov model compute with log function deal with long long
 * sequence
 * 
 * @author shellkk
 * 
 */
public class LogCHMM implements LearnMachine {

    /**
	 * log
	 */
    private static Log log = LogFactory.getLog(LogCHMM.class);

    private static final double factor = -0.5d * Math.log(2.0d * Math.PI);

    protected boolean logStep = true;

    protected double eps = 1.0E-5;

    protected double[][] transProb;

    protected double[] stProb;

    protected Matrix[] obInvSigma;

    protected double[] obDetInvSigma;

    protected Matrix[] obMean;

    protected int dim;

    protected double sigmaScale = 1;

    protected double scale = 1;

    protected double shift = 0;

    protected Sample[] samples;

    public LogCHMM(int states, int dim) {
        transProb = new double[states][states];
        stProb = new double[states];
        obInvSigma = new Matrix[states];
        obDetInvSigma = new double[states];
        obMean = new Matrix[states];
        this.dim = dim;
        randomInit();
    }

    public void init(double[] d, double v) {
        for (int i = 0; i < d.length; i++) {
            d[i] = v;
        }
    }

    public void init(double[][] dd, double v) {
        for (double[] d : dd) {
            init(d, v);
        }
    }

    public void init(double[][][] dd, double v) {
        for (double[][] d : dd) {
            init(d, v);
        }
    }

    protected void randomProb(double[] p) {
        double w = 0;
        for (int i = 0; i < p.length; i++) {
            p[i] = Math.random();
            w += p[i];
        }
        for (int i = 0; i < p.length; i++) {
            p[i] = p[i] > 0 ? Math.log(p[i] / w) : Double.NaN;
        }
    }

    public void randomInit() {
        for (int i = 0; i < transProb.length; i++) {
            randomProb(transProb[i]);
        }
        randomProb(stProb);
        for (int i = 0; i < obMean.length; i++) {
            obMean[i] = new Matrix(new double[dim], dim);
            obInvSigma[i] = new Matrix(new double[dim][dim]);
            for (int j = 0; j < dim; j++) {
                obMean[i].set(j, 0, Math.random() * scale + shift);
                obInvSigma[i].set(j, j, sigmaScale);
                obDetInvSigma[i] += Math.log(sigmaScale);
            }
        }
    }

    /**
	 * y = log(sum[exp(a[i])])
	 * 
	 * @param a
	 * @param start
	 * @param end
	 * @return
	 */
    protected double logSumExp(double[] a, int start, int end) {
        double max = -Double.MAX_VALUE;
        for (int i = start; i < end; i++) {
            if (a[i] >= max) {
                max = a[i];
            }
        }
        double e = 0;
        for (int i = start; i < end; i++) {
            if (!Double.isNaN(a[i])) {
                e += Math.exp(a[i] - max);
            }
        }
        return e > 0 ? max + Math.log(e) : Double.NaN;
    }

    /**
	 * density of x by state i, log value
	 * 
	 * @param i
	 * @param x
	 * @return
	 */
    protected double getLogDensity(int i, double[] x) {
        double[] v = new double[dim];
        for (int k = 0; k < dim; k++) {
            v[k] = x[k] - obMean[i].get(k, 0);
        }
        Matrix V = new Matrix(v, dim);
        Matrix Vt = V.transpose();
        double e = Vt.times(obInvSigma[i]).times(V).get(0, 0);
        double d = factor * dim + 0.5 * (obDetInvSigma[i] - e);
        return d;
    }

    /**
	 * y[i][j] = log(P[ob[0-i],state[i]=j])
	 * 
	 * @param ob
	 * @return
	 */
    protected double[][] getAlpha(double[][] ob) {
        int l = ob.length;
        int stateCount = stProb.length;
        double[] tmp = new double[stateCount];
        double[][] alpha = new double[l][stateCount];
        for (int i = 0; i < stateCount; i++) {
            alpha[0][i] = stProb[i] + getLogDensity(i, ob[0]);
        }
        for (int t = 1; t < l; t++) {
            for (int i = 0; i < stateCount; i++) {
                for (int j = 0; j < stateCount; j++) {
                    tmp[j] = alpha[t - 1][j] + transProb[j][i];
                }
                alpha[t][i] = logSumExp(tmp, 0, tmp.length) + getLogDensity(i, ob[t]);
            }
        }
        return alpha;
    }

    /**
	 * y[i][j] = log(P[ob[i+1-],state[i]=j])
	 * 
	 * @param ob
	 * @return
	 */
    protected double[][] getBeta(double[][] ob) {
        int l = ob.length;
        int stateCount = stProb.length;
        double[] tmp = new double[stateCount];
        double[][] beta = new double[l][stateCount];
        for (int i = 0; i < stateCount; i++) {
            beta[l - 1][i] = 0;
        }
        for (int t = l - 2; t >= 0; t--) {
            for (int i = 0; i < stateCount; i++) {
                for (int j = 0; j < stateCount; j++) {
                    tmp[j] = transProb[i][j] + getLogDensity(j, ob[t + 1]) + beta[t + 1][j];
                }
                beta[t][i] = logSumExp(tmp, 0, tmp.length);
            }
        }
        return beta;
    }

    /**
	 * top state sequence for the observer
	 * 
	 * @param ob
	 * @return
	 */
    public int[] getStateSequence(double[][] ob) {
        int l = ob.length;
        int stateCount = stProb.length;
        int[][] top = new int[l][stateCount];
        double[][] d = new double[l][stateCount];
        for (int i = 0; i < stateCount; i++) {
            d[0][i] = stProb[i] + getLogDensity(i, ob[0]);
        }
        for (int t = 1; t < l; t++) {
            for (int i = 0; i < stateCount; i++) {
                double max = -Double.MAX_VALUE;
                int top_state = -1;
                for (int j = 0; j < stateCount; j++) {
                    double v = d[t - 1][j] + transProb[j][i];
                    if (v > max) {
                        max = v;
                        top_state = j;
                    }
                }
                d[t][i] = max + getLogDensity(i, ob[t]);
                top[t][i] = top_state;
            }
        }
        int[] seq = new int[l];
        double max = -Double.MAX_VALUE;
        int maxi = -1;
        for (int i = 0; i < stateCount; i++) {
            double v = d[l - 1][i];
            if (v > max) {
                max = v;
                maxi = i;
            }
        }
        seq[l - 1] = maxi;
        for (int t = l - 2; t >= 0; t--) {
            seq[t] = top[t + 1][seq[t + 1]];
        }
        return seq;
    }

    public double getLogDensity(double[][] ob) {
        int l = ob.length;
        double[][] alpha = getAlpha(ob);
        return logSumExp(alpha[l - 1], 0, alpha[l - 1].length);
    }

    public double getDensity(double[][] ob) {
        double e = getLogDensity(ob);
        return Double.isNaN(e) ? 0 : Math.exp(e);
    }

    public double getY(Object x) {
        return getDensity((double[][]) x);
    }

    public void train(SampleSet sampleSet, StopHandle stopHandle) throws StopException, TrainException {
        samples = ArraySampleSet.toArraySampleSet(sampleSet, stopHandle).getSamples();
        int states = transProb.length;
        double[][] tmpTransProb = new double[states][states];
        double[] tmpStProb = new double[states];
        double[][] tmpMean = new double[states][dim];
        double[][][] tmpSigma = new double[states][dim][dim];
        double[] w = new double[samples.length];
        double total = 0;
        for (int k = 0; k < samples.length; k++) {
            w[k] = samples[k].weight;
            total += w[k];
        }
        for (int k = 0; k < samples.length; k++) {
            w[k] = w[k] / total;
        }
        double[] prob = new double[samples.length];
        for (int k = 0; k < samples.length; k++) {
            prob[k] = getLogDensity((double[][]) samples[k].x);
        }
        double lastTarget = Double.NaN;
        double thisTarget = 0;
        for (int k = 0; k < samples.length; k++) {
            thisTarget += prob[k] * samples[k].weight;
        }
        while (Double.isNaN(lastTarget) || thisTarget - lastTarget >= eps) {
            if (stopHandle != null && stopHandle.isStoped()) {
                throw new StopException();
            }
            if (logStep) {
                logStep(thisTarget);
            }
            init(tmpTransProb, 0);
            init(tmpMean, 0);
            init(tmpSigma, 0);
            init(tmpStProb, 0);
            for (int k = 0; k < samples.length; k++) {
                double[][] ob = (double[][]) samples[k].x;
                int l = ob.length;
                double[][] alpha = getAlpha(ob);
                double[][] beta = getBeta(ob);
                for (int i = 0; i < states; i++) {
                    double v = alpha[0][i] + beta[0][i] - prob[k];
                    tmpStProb[i] += Double.isNaN(v) ? 0 : Math.exp(v) * w[k];
                    double[] tmpgamma = new double[l];
                    for (int t = 0; t < l; t++) {
                        tmpgamma[t] = alpha[t][i] + beta[t][i];
                    }
                    double gama1 = logSumExp(tmpgamma, 0, l);
                    double gama2 = logSumExp(tmpgamma, 0, l - 1);
                    for (int j = 0; j < states; j++) {
                        double[] kc = new double[l - 1];
                        for (int t = 0; t < l - 1; t++) {
                            kc[t] = alpha[t][i] + transProb[i][j] + getLogDensity(j, ob[t + 1]) + beta[t + 1][j];
                        }
                        v = logSumExp(kc, 0, kc.length) - gama2;
                        tmpTransProb[i][j] += Double.isNaN(v) ? 0 : Math.exp(v) * w[k];
                    }
                    double[] q = new double[l];
                    for (int t = 0; t < l; t++) {
                        q[t] = Math.exp(tmpgamma[t] - gama1);
                    }
                    for (int b = 0; b < dim; b++) {
                        v = 0;
                        for (int t = 0; t < l; t++) {
                            v += q[t] * ob[t][b];
                        }
                        tmpMean[i][b] += v * w[k];
                        for (int c = 0; c <= b; c++) {
                            v = 0;
                            for (int t = 0; t < l; t++) {
                                v += q[t] * (ob[t][b] - obMean[i].get(b, 0)) * (ob[t][c] - obMean[i].get(c, 0));
                            }
                            tmpSigma[i][b][c] += v * w[k];
                            tmpSigma[i][c][b] = tmpSigma[i][b][c];
                        }
                    }
                }
            }
            for (int i = 0; i < states; i++) {
                for (int j = 0; j < states; j++) {
                    transProb[i][j] = Math.log(tmpTransProb[i][j]);
                }
                stProb[i] = Math.log(tmpStProb[i]);
                for (int b = 0; b < dim; b++) {
                    obMean[i].set(b, 0, tmpMean[i][b]);
                }
                Matrix sigma = new Matrix(tmpSigma[i]);
                obInvSigma[i] = sigma.inverse();
                obDetInvSigma[i] = Math.log(obInvSigma[i].det());
            }
            for (int k = 0; k < samples.length; k++) {
                prob[k] = getLogDensity((double[][]) samples[k].x);
            }
            lastTarget = thisTarget;
            thisTarget = 0;
            for (int k = 0; k < samples.length; k++) {
                thisTarget += prob[k] * samples[k].weight;
            }
        }
    }

    protected void logStep(double p) {
        log.info("[p=" + p + "]");
    }

    public double[] getStProb() {
        return stProb;
    }

    public void setStProb(double[] stProb) {
        this.stProb = stProb;
    }

    public double[][] getTransProb() {
        return transProb;
    }

    public void setTransProb(double[][] transProb) {
        this.transProb = transProb;
    }

    public Matrix[] getObInvSigma() {
        return obInvSigma;
    }

    public void setObInvSigma(Matrix[] obInvSigma) {
        this.obInvSigma = obInvSigma;
    }

    public double[] getObDetInvSigma() {
        return obDetInvSigma;
    }

    public void setObDetInvSigma(double[] obDetInvSigma) {
        this.obDetInvSigma = obDetInvSigma;
    }

    public Matrix[] getObMean() {
        return obMean;
    }

    public void setObMean(Matrix[] obMean) {
        this.obMean = obMean;
    }

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }

    public double getSigmaScale() {
        return sigmaScale;
    }

    public void setSigmaScale(double sigmaScale) {
        this.sigmaScale = sigmaScale;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public double getShift() {
        return shift;
    }

    public void setShift(double shift) {
        this.shift = shift;
    }

    public double getEps() {
        return eps;
    }

    public void setEps(double eps) {
        this.eps = eps;
    }

    public boolean isLogStep() {
        return logStep;
    }

    public void setLogStep(boolean logStep) {
        this.logStep = logStep;
    }

    public Sample[] getSamples() {
        return samples;
    }

    public void setSamples(Sample[] samples) {
        this.samples = samples;
    }

    public static void main(String[] args) {
        try {
            PropertyConfigurator.configure("log4j.properties");
            double[][] a = { { 0.1, 0.8, 0.1 }, { 0.1, 0.1, 0.8 }, { 0.8, 0.1, 0.1 } };
            double[] c = { 0.8, 0.1, 0.1 };
            double[] m1 = { 1, 1 };
            double[] m2 = { 3.5, 3.5 };
            double[] m3 = { 6, 1 };
            double[][] d1 = { { 0.5, 0 }, { 0, 0.5 } };
            double[][] d2 = { { 0.5, 0 }, { 0, 0.5 } };
            double[][] d3 = { { 1, 0 }, { 0, 1 } };
            Matrix[] M = { new Matrix(m1, m1.length), new Matrix(m2, m2.length), new Matrix(m3, m3.length) };
            Matrix[] D = { new Matrix(d1), new Matrix(d2), new Matrix(d3) };
            Random[][] R = { { new Random(), new Random() }, { new Random(), new Random() }, { new Random(), new Random() } };
            Sample[] samples = new Sample[1000];
            for (int i = 0; i < samples.length; i++) {
                double[][] seq = generate(50, a, c, M, D, R);
                samples[i] = new Sample(seq, 0);
            }
            LogCHMM hmm = new LogCHMM(3, 2);
            long begin = System.currentTimeMillis();
            hmm.train(new ArraySampleSet(samples), null);
            long end = System.currentTimeMillis();
            System.out.println("seconds used by training:" + ((end - begin) / 1000));
            hmm.print();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void print() {
        double[] bb = getStProb();
        double[][] aa = getTransProb();
        printExp(bb);
        printExp(aa);
        for (int i = 0; i < obMean.length; i++) {
            print(obMean[i].getRowPackedCopy());
            System.out.println(obInvSigma[i].toString());
        }
    }

    private void printExp(double[][] dd) {
        for (int i = 0; i < dd.length; i++) {
            printExp(dd[i]);
        }
        System.out.println();
    }

    private void print(double[] d) {
        for (int i = 0; i < d.length; i++) {
            if (i > 0) {
                System.out.print(',');
            }
            System.out.print(d[i]);
        }
        System.out.println();
    }

    private void printExp(double[] d) {
        for (int i = 0; i < d.length; i++) {
            if (i > 0) {
                System.out.print(',');
            }
            System.out.print(Math.exp(d[i]));
        }
        System.out.println();
    }

    private static double[][] generate(int length, double[][] a, double[] c, Matrix[] m, Matrix[] sigma, Random[][] random) {
        double[][] seq = new double[length][m[0].getRowDimension()];
        int state = randomIndex(c);
        for (int i = 0; i < length; i++) {
            seq[i] = randomArray(m[state], sigma[state], random[state]);
            state = randomIndex(a[state]);
        }
        return seq;
    }

    private static double[] randomArray(Matrix m, Matrix sigma, Random[] random) {
        return Maths.rnorm(1, m, sigma, random)[0].getRowPackedCopy();
    }

    private static int randomIndex(double[] p) {
        double v = Math.random();
        double q = 0;
        for (int i = 0; i < p.length; i++) {
            q += p[i];
            if (v <= q) {
                return i;
            }
        }
        return p.length - 1;
    }
}
