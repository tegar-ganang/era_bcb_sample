package game.trainers;

import game.models.GradientTrainable;
import game.trainers.gradient.Newton.Uncmin_f77;
import game.trainers.gradient.Newton.Uncmin_methods;
import game.utils.GlobalRandom;
import configuration.models.game.trainers.QuasiNewtonConfig;
import game.utils.MyRandom;

/**
 * <p>Title: </p>
 * <p/>
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p/>
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class QuasiNewtonTrainer extends Trainer implements Uncmin_methods {

    private transient double[] na;

    private transient double[] besta;

    private transient double[] f;

    private transient double[] g;

    private transient double[][] aa;

    private transient double[] udiag;

    private transient int[] msg;

    private transient double[] typsiz;

    private transient double[] dlt;

    private transient double[] fscale;

    private transient double[] stepmx;

    private transient int[] ndigit;

    private transient int[] method;

    private transient int[] iexp;

    private transient int[] itnlim;

    private transient int[] iagflg;

    private transient int[] iahflg;

    private transient double[] gradtl;

    private transient double[] steptl;

    private transient int[] info;

    private transient double[] xn;

    private transient double[] gn;

    private transient double[][] hn;

    double lastError = -1;

    double firstError = -1;

    int rec;

    int draw;

    boolean forceHessian;

    public QuasiNewtonTrainer() {
        dlt = new double[2];
        fscale = new double[2];
        stepmx = new double[2];
        ndigit = new int[2];
        method = new int[2];
        iexp = new int[2];
        itnlim = new int[2];
        iagflg = new int[2];
        iahflg = new int[2];
        gradtl = new double[2];
        steptl = new double[2];
        msg = new int[2];
        double epsm = 1.12e-16;
        dlt[1] = -1.0;
        gradtl[1] = Math.pow(epsm, 1.0 / 3.0);
        steptl[1] = Math.sqrt(epsm);
        stepmx[1] = 0.0;
        method[1] = 2;
        fscale[1] = 1.0;
        msg[1] = 0;
        ndigit[1] = -1;
        itnlim[1] = 150;
    }

    public void init(GradientTrainable uni, Object cfg) {
        super.init(uni, cfg);
        QuasiNewtonConfig cf = (QuasiNewtonConfig) cfg;
        draw = cf.getDraw();
        rec = cf.getRec();
        forceHessian = cf.isForceAnalyticHessian();
    }

    public void setCoef(int coef) {
        super.setCoef(coef);
        xn = new double[coefficients];
        gn = new double[coefficients];
        hn = new double[coefficients][coefficients];
        na = new double[coef + 1];
        besta = new double[coef + 1];
        f = new double[coef + 1];
        g = new double[coef + 1];
        aa = new double[coef + 1][coef + 1];
        udiag = new double[coef + 1];
        info = new int[coef + 1];
        typsiz = new double[coef + 1];
        MyRandom rnd = GlobalRandom.getInstance();
        for (int i = 0; i < coef + 1; i++) na[i] = rnd.getSmallDouble();
        for (int i = 1; i <= coef; i++) {
            typsiz[i] = 1.0;
        }
        double[] x = new double[coef + 1];
        double[] g = new double[coef + 1];
        double[][] h = new double[coef + 1][coef + 1];
        if (!unit.gradient(x, g)) {
            iagflg[1] = 0;
        } else {
            iagflg[1] = 1;
        }
        if (forceHessian && unit.hessian(x, h)) {
            iexp[1] = 0;
            iahflg[1] = 1;
            System.out.println("analytic hessian enabled");
        } else {
            iexp[1] = 1;
            iahflg[1] = 0;
        }
    }

    /**
     * starts the teaching process
     */
    public void teach() {
        if (startingPoint != null) System.arraycopy(startingPoint, 0, na, 1, coefficients);
        Uncmin_f77.optif9_f77(coefficients, na, this, typsiz, fscale, method, iexp, msg, ndigit, itnlim, iagflg, iahflg, dlt, gradtl, stepmx, steptl, besta, f, g, info, aa, udiag);
    }

    /**
     * returns the name of the algorithm used for weights(coeffs.) estimation
     */
    public String getMethodName() {
        return "Quasi Newton Method";
    }

    public double f_to_minimize(double[] x) {
        System.arraycopy(x, 1, xn, 0, coefficients);
        return getAndRecordError(xn, rec, draw, true);
    }

    /**
     * no config class bbb
     */
    public Class getConfigClass() {
        return QuasiNewtonConfig.class;
    }

    public void gradient(double[] x, double[] g) {
        for (int i = 0; i < coefficients; i++) {
            xn[i] = x[i + 1];
            gn[i] = g[i + 1];
        }
        unit.gradient(xn, gn);
        for (int i = 0; i < coefficients; i++) {
            x[i + 1] = xn[i];
            g[i + 1] = gn[i];
        }
    }

    public void hessian(double[] x, double[][] h) {
        System.out.println("asking for hessian");
        for (int i = 0; i < coefficients; i++) {
            xn[i] = x[i + 1];
            System.arraycopy(h[i + 1], 1, hn[i], 0, coefficients);
        }
        unit.hessian(xn, hn);
        for (int i = 0; i < coefficients; i++) {
            x[i + 1] = xn[i];
            System.arraycopy(hn[i], 0, h[i + 1], 1, coefficients);
        }
    }

    public boolean allowedByDefault() {
        return true;
    }

    /**
     * added for multiprocessor support
     * by jakub spirk spirk.jakub@gmail.com
     * 05. 2008
     */
    public boolean isExecutableInParallelMode() {
        return true;
    }
}
