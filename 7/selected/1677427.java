package shellkk.qiq.math.ml.timeseries;

import java.util.Random;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import shellkk.qiq.math.StopException;
import shellkk.qiq.math.StopHandle;
import shellkk.qiq.math.matrix.Matrix;
import shellkk.qiq.math.ml.ArraySampleSet;
import shellkk.qiq.math.ml.LearnMachine;
import shellkk.qiq.math.ml.Sample;
import shellkk.qiq.math.ml.SampleSet;
import shellkk.qiq.math.ml.TrainException;

/**
 * Autoregression moving average
 * 
 * x[n] = sum(a[i]*x[n-i])+a[0]+e[n]+sum(b[j]*e[n-j]) a is the autoregression
 * coefficients a[0] = (1-sum(a[i]))*E(x) b is the moving average coefficients
 * b[0] = 1
 * 
 * @author shellkk
 * 
 */
public class ARMA implements LearnMachine {

    private static final Log log = LogFactory.getLog(ARMA.class);

    protected Sample[] samples;

    protected int arLevel;

    protected int maLevel;

    protected double expectValue;

    protected double variance;

    protected double[] arCoef;

    protected double[] maCoef;

    protected int fakeArLevel;

    protected int maxArLevel;

    protected int maxMaLevel;

    public ARMA() {
        this(1, 0);
    }

    public ARMA(int arLevel, int maLevel) {
        this.arLevel = arLevel;
        this.maLevel = maLevel;
    }

    protected static double computeExpectValue(Sample[] samples) {
        double value = 0;
        double total = 0;
        for (int n = 0; n < samples.length; n++) {
            double[] x = (double[]) samples[n].x;
            double w = samples[n].weight;
            for (int j = 0; j < x.length; j++) {
                value += x[j] * w;
                total += w;
            }
        }
        value = total > 0 ? value / total : 0;
        return value;
    }

    protected static double[] computeARRel(int arLevel, double expectValue, int maLevel, Sample[] samples) {
        double[] rel = new double[arLevel + maLevel + 1];
        for (int shift = 0; shift < rel.length; shift++) {
            double total = 0;
            for (int n = 0; n < samples.length; n++) {
                double[] seq = (double[]) samples[n].x;
                double[] zeroseq = new double[seq.length];
                for (int i = 0; i < zeroseq.length; i++) {
                    zeroseq[i] = seq[i] - expectValue;
                }
                double w = samples[n].weight;
                for (int j = 0; j < zeroseq.length - shift; j++) {
                    rel[shift] += zeroseq[j] * zeroseq[j + shift] * w;
                    total += w;
                }
            }
            rel[shift] = total > 0 ? rel[shift] / total : 0;
        }
        return rel;
    }

    protected static double[] computeARCoef(int arLevel, double expectValue, int maLevel, double[] arrel, StopHandle handle) throws StopException {
        if (arLevel > 0) {
            double[][] m = new double[arLevel][arLevel];
            double[] r = new double[arLevel];
            for (int k = 1; k <= arLevel; k++) {
                r[k - 1] = arrel[maLevel + k];
                for (int j = 1; j <= arLevel; j++) {
                    int shift = maLevel + k - j;
                    if (shift >= 0) {
                        m[k - 1][j - 1] = arrel[shift];
                    } else {
                        m[k - 1][j - 1] = arrel[-shift];
                    }
                }
            }
            Matrix M = new Matrix(m);
            Matrix R = new Matrix(r, r.length);
            Matrix A = null;
            try {
                A = M.inverse().times(R);
            } catch (Exception e) {
                A = M.pseudoinverse().times(R);
            }
            double[] a = A.getRowPackedCopy();
            double[] coef = new double[a.length + 1];
            coef[0] = 1;
            for (int i = 1; i < coef.length; i++) {
                coef[i] = a[i - 1];
                coef[0] -= coef[i];
            }
            coef[0] *= expectValue;
            return coef;
        } else {
            return new double[] { expectValue };
        }
    }

    protected static double[] getMaseq(double[] arCoef, double[] seq) {
        int arLevel = arCoef.length - 1;
        if (seq.length - arLevel <= 0) {
            return new double[0];
        }
        double[] maseq = new double[seq.length - arLevel];
        for (int i = 0; i < maseq.length; i++) {
            maseq[i] = seq[i + arLevel] - arCoef[0];
            for (int j = 1; j < arCoef.length; j++) {
                maseq[i] -= arCoef[j] * seq[i + arLevel - j];
            }
        }
        return maseq;
    }

    protected static double[] computeMaCoef(double[] arCoef, int fakeArLevel, int maLevel, Sample[] samples, StopHandle handle) throws StopException, TrainException {
        if (maLevel > 0) {
            ARMA arma = new ARMA(fakeArLevel, 0);
            Sample[] maSamples = new Sample[samples.length];
            for (int i = 0; i < samples.length; i++) {
                double[] seq = (double[]) samples[i].x;
                double w = samples[i].weight;
                double[] x = getMaseq(arCoef, seq);
                maSamples[i] = new Sample(x, 0);
                maSamples[i].weight = w;
            }
            arma.train(new ArraySampleSet(maSamples), handle);
            double[][] m = new double[maLevel][maLevel];
            double[] xy = new double[maLevel];
            double total = 0;
            for (int i = 0; i < maSamples.length; i++) {
                double w = maSamples[i].weight;
                double[] maseq = (double[]) maSamples[i].x;
                double[] eseq = getMaseq(arma.arCoef, maseq);
                for (int j = maLevel + fakeArLevel; j < eseq.length; j++) {
                    double y = maseq[j];
                    double[] x = new double[maLevel];
                    System.arraycopy(eseq, j - fakeArLevel - maLevel, x, 0, maLevel);
                    total += w;
                    for (int p = 0; p < maLevel; p++) {
                        for (int q = 0; q < p; q++) {
                            double xpxq = x[p] * x[q];
                            m[p][q] += w * xpxq;
                        }
                        m[p][p] += w * x[p] * x[p];
                        xy[p] += w * x[p] * y;
                    }
                }
            }
            for (int i = 0; i < maLevel; i++) {
                for (int j = 0; j < i; j++) {
                    m[i][j] /= total;
                    m[j][i] = m[i][j];
                }
                m[i][i] /= total;
                xy[i] /= total;
            }
            Matrix M = new Matrix(m);
            Matrix XY = new Matrix(xy, xy.length);
            Matrix W = null;
            try {
                W = M.inverse().times(XY);
            } catch (Exception e) {
                W = M.pseudoinverse().times(XY);
            }
            double[] weight = W.getRowPackedCopy();
            double[] coef = new double[maLevel + 1];
            coef[0] = 1;
            for (int i = 1; i < coef.length; i++) {
                coef[i] = weight[weight.length - i];
            }
            return coef;
        } else {
            return new double[] { 1 };
        }
    }

    protected static double[] getTopResidual(double[] maCoef, double[] maseq) {
        int maLevel = maCoef.length - 1;
        if (maLevel > 0) {
            double[] q = new double[maLevel];
            for (int i = 0; i < maLevel; i++) {
                q[i] = maCoef[maLevel - i];
            }
            Matrix Q = new Matrix(q, 1);
            double[][] m = new double[maLevel][maLevel + 1];
            for (int i = 0; i < maLevel; i++) {
                m[i][i] = 1;
            }
            Matrix M = new Matrix(m);
            double[][] k = new double[maLevel][maLevel];
            for (int i = 0; i < k.length; i++) {
                k[i][i] = 1;
            }
            Matrix K = new Matrix(k);
            double[] b = new double[maLevel];
            Matrix B = new Matrix(b, b.length);
            for (int i = 0; i < maseq.length; i++) {
                double[] c = Q.times(-1).times(M).getRowPackedCopy();
                c[maLevel] += maseq[i];
                for (int j = 0; j < maLevel; j++) {
                    b[j] += c[j] * c[maLevel];
                    k[j][j] += c[j] * c[j];
                    for (int l = 0; l < maLevel; l++) {
                        if (l != j) {
                            k[j][l] += c[j] * c[l];
                        }
                    }
                }
                for (int j = 0; j < maLevel - 1; j++) {
                    m[j] = m[j + 1];
                }
                m[maLevel - 1] = c;
            }
            double[] x = null;
            try {
                x = K.inverse().times(B).getRowPackedCopy();
            } catch (Exception e) {
                log.warn(e);
                x = new double[maLevel];
            }
            double[] e = new double[maseq.length];
            for (int i = 0; i < e.length; i++) {
                e[i] = maseq[i];
                for (int j = 0; j < maLevel; j++) {
                    if (i - j - 1 >= 0) {
                        e[i] -= q[maLevel - j - 1] * e[i - j - 1];
                    } else {
                        e[i] -= q[maLevel - j - 1] * x[x.length + i - j - 1];
                    }
                }
            }
            return e;
        } else {
            return maseq;
        }
    }

    /**
	 * ma sequence for the input sequence
	 * 
	 * @param seq
	 * @return
	 */
    public double[] estimateMaSequence(double[] seq) {
        double[] maSeq = getMaseq(arCoef, seq);
        double[] output = new double[seq.length];
        System.arraycopy(maSeq, 0, output, arCoef.length - 1, maSeq.length);
        return output;
    }

    /**
	 * residual sequence for the input sequence
	 * 
	 * @param seq
	 * @return
	 */
    public double[] estimateResidualSequence(double[] seq) {
        double[] maSeq = getMaseq(arCoef, seq);
        double[] residualSeq = getTopResidual(maCoef, maSeq);
        double[] output = new double[seq.length];
        System.arraycopy(residualSeq, 0, output, arCoef.length - 1, residualSeq.length);
        return output;
    }

    /**
	 * predict value sequence given the input sequence E(x[n]|seq)
	 * 
	 * @param seq
	 * @param predictSize
	 * @return
	 */
    public double[] predictSequence(double[] seq, int predictSize) {
        double[] output = new double[predictSize];
        double[] residualSeq = estimateResidualSequence(seq);
        for (int i = 0; i < output.length; i++) {
            output[i] = arCoef[0];
            for (int j = 1; j < arCoef.length; j++) {
                if (i - j >= 0) {
                    output[i] += arCoef[j] * output[i - j];
                } else if (seq.length + i - j >= 0) {
                    output[i] += arCoef[j] * seq[seq.length + i - j];
                }
            }
            for (int j = 1; j < maCoef.length; j++) {
                if (i - j < 0 && residualSeq.length + i - j >= 0) {
                    output[i] += maCoef[j] * residualSeq[residualSeq.length + i - j];
                }
            }
        }
        return output;
    }

    /**
	 * coefficients on future residual for predict value. It may be useful when
	 * ARMA-GARCH
	 * 
	 * @param predictSize
	 * @return
	 */
    public double[][] predictCoefOnResidual(int predictSize) {
        double[][] coef = new double[predictSize][predictSize];
        for (int i = 0; i < coef.length; i++) {
            for (int j = 0; j <= i; j++) {
                if (i - j < maCoef.length) {
                    coef[i][j] += maCoef[i - j];
                }
                for (int k = 1; k < arCoef.length; k++) {
                    if (i - k >= 0) {
                        coef[i][j] += coef[i - k][j] * arCoef[k];
                    }
                }
            }
        }
        return coef;
    }

    /**
	 * maybe useful for computing confidence level E((x[n]-E(x[n]|seq))^2|seq)
	 * 
	 * @param seq
	 * @param predictSize
	 * @return
	 */
    public double[] predictVarianceSequence(double[] seq, int predictSize) {
        double[][] coef = predictCoefOnResidual(predictSize);
        double[] output = new double[predictSize];
        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j <= i; j++) {
                output[i] += coef[i][j] * coef[i][j];
            }
            output[i] = variance * output[i];
        }
        return output;
    }

    protected static double computeVariance(double[] arCoef, double[] maCoef, Sample[] samples, StopHandle handle) throws StopException {
        double total = 0;
        double var = 0;
        for (int i = 0; i < samples.length; i++) {
            if (handle != null && handle.isStoped()) {
                throw new StopException();
            }
            double[] seq = (double[]) samples[i].x;
            double w = samples[i].weight;
            double[] maseq = getMaseq(arCoef, seq);
            double[] residualSeq = getTopResidual(maCoef, maseq);
            for (int j = 0; j < residualSeq.length; j++) {
                total += w;
                var += residualSeq[j] * residualSeq[j];
            }
        }
        return total > 0 ? var / total : 0;
    }

    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("Expect value:");
        str.append(expectValue + "\n");
        str.append("Variance:");
        str.append(variance + "\n");
        str.append("Autoregression:\n");
        str.append(new Matrix(arCoef, 1));
        str.append("Moving average:\n");
        str.append(new Matrix(maCoef, 1));
        return str.toString();
    }

    public double getY(Object x) {
        return 0;
    }

    public void train(SampleSet sampleSet, StopHandle stopHandle) throws StopException, TrainException {
        ArraySampleSet data = ArraySampleSet.toArraySampleSet(sampleSet, stopHandle);
        samples = data.getSamples();
        if (maxArLevel <= 0 && maxMaLevel <= 0) {
            if (fakeArLevel <= maLevel) {
                fakeArLevel = maLevel * 10;
            }
            expectValue = computeExpectValue(samples);
            double[] arrel = computeARRel(arLevel, expectValue, maLevel, samples);
            arCoef = computeARCoef(arLevel, expectValue, maLevel, arrel, stopHandle);
            maCoef = computeMaCoef(arCoef, fakeArLevel, maLevel, samples, stopHandle);
            variance = computeVariance(arCoef, maCoef, samples, stopHandle);
        } else {
            double minCost = Double.MAX_VALUE;
            ARMA top = null;
            double l = 0;
            for (int i = 0; i < samples.length; i++) {
                double[] seq = (double[]) samples[i].x;
                l += seq.length;
            }
            double costFactor = Math.log(l) / l;
            int minAr = maxArLevel > 0 ? 0 : arLevel;
            int maxAr = maxArLevel > 0 ? maxArLevel : arLevel;
            int minMa = maxMaLevel > 0 ? 0 : maLevel;
            int maxMa = maxMaLevel > 0 ? maxMaLevel : maLevel;
            for (int p = minAr; p <= maxAr; p++) {
                for (int q = minMa; q <= maxMa; q++) {
                    ARMA arma = new ARMA(p, q);
                    arma.setFakeArLevel(fakeArLevel);
                    arma.setMaxArLevel(0);
                    arma.setMaxMaLevel(0);
                    arma.train(data, stopHandle);
                    double cost = Math.log(arma.variance) + (p + q) * costFactor;
                    if (cost < minCost) {
                        minCost = cost;
                        top = arma;
                    }
                }
            }
            if (top != null) {
                arLevel = top.arLevel;
                maLevel = top.maLevel;
                expectValue = top.expectValue;
                variance = top.variance;
                arCoef = top.arCoef;
                maCoef = top.maCoef;
            }
        }
    }

    public Sample[] getSamples() {
        return samples;
    }

    public void setSamples(Sample[] samples) {
        this.samples = samples;
    }

    public int getArLevel() {
        return arLevel;
    }

    public void setArLevel(int arLevel) {
        this.arLevel = arLevel;
    }

    public int getMaLevel() {
        return maLevel;
    }

    public void setMaLevel(int maLevel) {
        this.maLevel = maLevel;
    }

    public double getExpectValue() {
        return expectValue;
    }

    public void setExpectValue(double expectValue) {
        this.expectValue = expectValue;
    }

    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    public double[] getArCoef() {
        return arCoef;
    }

    public void setArCoef(double[] arCoef) {
        this.arCoef = arCoef;
    }

    public double[] getMaCoef() {
        return maCoef;
    }

    public void setMaCoef(double[] maCoef) {
        this.maCoef = maCoef;
    }

    public int getFakeArLevel() {
        return fakeArLevel;
    }

    public void setFakeArLevel(int fakeArLevel) {
        this.fakeArLevel = fakeArLevel;
    }

    public int getMaxArLevel() {
        return maxArLevel;
    }

    public void setMaxArLevel(int maxArLevel) {
        this.maxArLevel = maxArLevel;
    }

    public int getMaxMaLevel() {
        return maxMaLevel;
    }

    public void setMaxMaLevel(int maxMaLevel) {
        this.maxMaLevel = maxMaLevel;
    }

    public static void main(String[] args) {
        try {
            PropertyConfigurator.configure("log4j.properties");
            double[] arCoef = { 0.1, 0.3, 0.2, 0.3, 0.1 };
            double[] maCoef = { 0.3, 0.2 };
            Sample[] samples = new Sample[1];
            int size = 1000;
            int predictSize = 10;
            double[] seq = generateSeq(size, arCoef, maCoef);
            double[] seq1 = new double[size - predictSize];
            double[] seq2 = new double[predictSize];
            System.arraycopy(seq, 0, seq1, 0, size - predictSize);
            System.arraycopy(seq, size - predictSize, seq2, 0, predictSize);
            samples[0] = new Sample(seq1, 0);
            ARMA arma = new ARMA(4, 1);
            arma.setMaxArLevel(10);
            arma.setMaxMaLevel(10);
            arma.train(new ArraySampleSet(samples), null);
            System.out.println(arma.toString());
            double[] predict = arma.predictSequence(seq1, predictSize);
            double[] variance = arma.predictVarianceSequence(seq1, predictSize);
            System.out.println("actual:");
            System.out.println(new Matrix(seq2, 1));
            System.out.println("predict:");
            System.out.println(new Matrix(predict, 1));
            System.out.println("variance:");
            System.out.println(new Matrix(variance, 1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double[] generateSeq(int length, double[] arCoef, double[] maCoef) {
        double[] seq = new double[length];
        double[] r = new double[length];
        Random rd = new Random();
        for (int i = 0; i < seq.length; i++) {
            r[i] = rd.nextGaussian();
            seq[i] = arCoef[0];
            for (int j = 1; j < arCoef.length; j++) {
                if (i - j >= 0) {
                    seq[i] += arCoef[j] * seq[i - j];
                }
            }
            for (int j = 0; j < maCoef.length; j++) {
                if (i - j >= 0) {
                    seq[i] += maCoef[j] * r[i - j];
                }
            }
        }
        return seq;
    }
}
