package coda.sim;

import coda.CoDaStats;
import coda.Composition;
import coda.ext.jama.CholeskyDecomposition;
import coda.ext.jama.Matrix;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author mcomas
 */
public class CoDaRandom {

    private static Random generator = new Random();

    public CoDaRandom() {
    }

    public CoDaRandom(long seed) {
        generator = new Random(seed);
    }

    public double[] normalRandomVariable(double[][] cov) {
        double mean[] = new double[cov.length];
        Arrays.fill(mean, 0);
        return normalRandomVariable(mean, cov);
    }

    public static double[] normalRandomVariable(double[] mean, double[][] cov) {
        int size = mean.length;
        double result[] = new double[size];
        double cholesky[][] = new CholeskyDecomposition(new Matrix(cov)).getL().getArray();
        double z[] = new double[size];
        for (int i = 0; i < size; i++) {
            double a = generator.nextGaussian();
            z[i] = a;
        }
        for (int i = 0; i < size; i++) {
            result[i] = mean[i];
            for (int j = 0; j <= i; j++) result[i] += cholesky[i][j] * z[j];
        }
        return result;
    }

    public Composition nextGaussianBasis(double[] mean, double[][] cov) {
        double basis[] = normalRandomVariable(mean, cov);
        for (int i = 0; i < basis.length; i++) basis[i] = Math.exp(basis[i]);
        return new Composition(basis);
    }

    public Composition nextGaussianILRCoDa(double[] mean, double[][] var, double[][] basis) {
        double ilr[] = normalRandomVariable(mean, var);
        return new Composition(CoDaStats.transformILRRaw(ilr, basis));
    }

    public static double[] nextGaussianILR(double[] mean, double[][] var, double[][] basis) {
        double ilr[] = normalRandomVariable(mean, var);
        return CoDaStats.transformILRRaw(ilr, basis);
    }

    public Composition nextGaussianCLRCoDa(double[] mean, double[][] var) {
        double clr[] = normalRandomVariable(mean, var);
        return new Composition(CoDaStats.transformCLRRaw(clr));
    }

    public static double[] nextGaussianCLR(double[] mean, double[][] var) {
        double clr[] = normalRandomVariable(mean, var);
        return CoDaStats.transformCLRRaw(clr);
    }

    public Composition nextGaussianALRCoDa(double[] mean, double[][] var) {
        double alr[] = normalRandomVariable(mean, var);
        return new Composition(CoDaStats.transformALRRaw(alr));
    }

    public static double[] nextGaussianALR(double[] mean, double[][] var) {
        double alr[] = normalRandomVariable(mean, var);
        return CoDaStats.transformALRRaw(alr);
    }

    public double nextExp(double lambda) {
        return -Math.log(1.0 - generator.nextDouble()) / lambda;
    }

    public double epsilon(double delta) {
        double v[] = new double[4];
        double epsilon, nu;
        v[0] = Math.E / (Math.E + delta);
        do {
            v[1] = generator.nextDouble();
            v[2] = generator.nextDouble();
            v[3] = generator.nextDouble();
            if (v[1] <= v[0]) {
                epsilon = Math.pow(v[2], 1 / delta);
                nu = v[3] * Math.pow(epsilon, delta - 1);
            } else {
                epsilon = 1 - Math.log(v[2]);
                nu = v[3] * Math.exp(-epsilon);
            }
        } while (nu > Math.pow(epsilon, delta - 1) * Math.exp(-epsilon));
        return epsilon;
    }

    public double nextGamma(double k, double lambda) {
        double gamma = 0;
        int n = (int) k;
        for (int i = 0; i < n; i++) gamma += nextExp(lambda);
        gamma += epsilon(k - n);
        return gamma;
    }

    public Composition nextDirichlet(double alpha[]) {
        double[] dirichlet = new double[alpha.length];
        double total = 0;
        for (int i = 0; i < alpha.length; i++) {
            dirichlet[i] = nextGamma(alpha[i], 1);
            total += dirichlet[i];
        }
        for (int i = 0; i < alpha.length; i++) dirichlet[i] /= total;
        return new Composition(dirichlet);
    }

    public Composition nextSimplexUniform(double lower[], double upper[]) {
        int size = lower.length;
        double result[] = new double[size];
        for (int i = 0; i < size; i++) result[i] = Math.exp(lower[i] + generator.nextDouble() * (upper[i] - lower[i]));
        return new Composition(result);
    }

    public Composition nextSimplex3Uniform() {
        double result[] = nextTriangleUniform();
        for (int i = 0; i < 3; i++) result[i] = Math.exp(result[i]);
        return new Composition(result);
    }

    public double[] nextTriangleUniform(double a[], double b[], double c[]) {
        return null;
    }

    public double[] nextTriangleUniform() {
        double result[] = new double[3];
        double a = generator.nextDouble();
        double b = generator.nextDouble();
        double max = (a > b ? a : b);
        double min = (a > b ? b : a);
        result[0] = min;
        result[1] = max - min;
        result[2] = 1 - max;
        return result;
    }

    public Composition nextUniform(int size) {
        double vector[] = new double[size + 1];
        double result[] = new double[size];
        vector[0] = 0;
        for (int i = 1; i < size; i++) vector[i] = generator.nextDouble();
        vector[size] = 1;
        Arrays.sort(vector);
        for (int i = 0; i < size; i++) result[i] = vector[i + 1] - vector[i];
        vector = null;
        return new Composition(result);
    }
}
