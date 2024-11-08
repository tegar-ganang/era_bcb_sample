package ugliML.learning.quasinewton;

import java.util.Arrays;
import optim.util.MathUtils;

/**
 * Limited-Memory BFGS algorithm, due to Liu & Nocedal (1989).  
 * Estimates 2nd derivative using previous M gradients, acheiving O(M x N) memory usage on an N-dimensional problem.
 * Useful when full Hessian approximation update techniques are unfeasible due to O(N^2) mem requirement.
 * 
 * <p>Our implementation is a *direct* translation of Sergey Bochkanov's free C-Sharp version, which is itself a translation of J. Nocedal's FORTRAN code.  I've used what I believe are Nocedal's comments on his code in some comments in this document.  Thank you both!
 * @author gbd
 */
public class LBFGS implements QuasiNewtonAlgorithm {

    QuasiNewtonProblem problem;

    public static boolean DEBUG = false;

    static void padArray(double[] source, double[] target) {
        for (int i = 0; i < source.length; i++) {
            target[i + 1] = source[i];
        }
    }

    static void unpadArray(double[] source, double[] target) {
        for (int i = 0; i < target.length; i++) {
            target[i] = source[i + 1];
        }
    }

    class MDouble {

        public Double value = 0.0;

        Double getValue() {
            return value;
        }

        void setValue(Double value) {
            this.value = value;
        }

        public String toString() {
            return value.toString();
        }
    }

    class MInt {

        public Integer value = 0;

        Integer getValue() {
            return value;
        }

        void setValue(Integer value) {
            this.value = value;
        }

        public String toString() {
            return value.toString();
        }
    }

    class MBool {

        public Boolean value = false;

        Boolean getValue() {
            return value;
        }

        void setValue(Boolean value) {
            this.value = value;
        }

        public String toString() {
            return value.toString();
        }
    }

    /**
	 * Calculates the function value (f) and the gradient (g) at point x
	 * Prints out to the screen for debugging purposes.
	 * 
	 * @param x the point where f and g are to be evaluated at
	 * @param f stores the function value at x
	 * @param g stores the gradient value at x
	 */
    private void funcGrad(double[] x, MDouble f, double[] g) {
        double[] trueX = new double[x.length - 1];
        unpadArray(x, trueX);
        f.setValue(problem.getFunctionValAt(trueX));
        padArray(problem.getGradientAt(trueX), g);
        if (DEBUG) {
            System.out.println("x = " + Arrays.toString(trueX));
            System.out.println("f = " + f.value);
            System.out.println("g = " + Arrays.toString(g));
        }
    }

    /**
	 positive number which  defines  a  precision  of  search.  The
	 subroutine finishes its work if the condition ||G|| < EpsG  is
	 satisfied, where ||.|| means Euclidian norm, G - gradient, X -
	 current approximation.
	 */
    double epsg = .0001;

    /**
	 positive number which  defines  a  precision  of  search.  The
	 subroutine finishes its work if on iteration  number  k+1  the
	 condition |F(k+1)-F(k)| <= EpsF*max{|F(k)|, |F(k+1)|, 1}    is
	 satisfied.
	 */
    double epsf = .000001;

    /**
	 positive number which  defines  a  precision  of  search.  The
	 subroutine finishes its work if on iteration number k+1    the
	 condition |X(k+1)-X(k)| <= EpsX is fulfilled.
	 */
    double epsx = .000001;

    /**
	 M   -   number of corrections in the BFGS scheme of Hessian
	 approximation update. Recommended value:  3<=M<=7. The smaller
	 value causes worse convergence, the bigger will  not  cause  a
	 considerably better convergence, but will cause a fall in  the
	 performance. M<=N.
	 */
    int m = 4;

    /**
	 MaxIts- maximum number of iterations. If MaxIts=0, the number of
	 iterations is unlimited.
	 */
    int maxits = 0;

    public double getEpsf() {
        return epsf;
    }

    public double getEpsg() {
        return epsg;
    }

    public double getEpsx() {
        return epsx;
    }

    public int getM() {
        return m;
    }

    public int getMaxits() {
        return maxits;
    }

    public void setEpsf(double epsf) {
        this.epsf = epsf;
    }

    public void setEpsg(double epsg) {
        this.epsg = epsg;
    }

    public void setEpsx(double epsx) {
        this.epsx = epsx;
    }

    public void setM(int m) {
        this.m = m;
    }

    public void setMaxits(int maxits) {
        this.maxits = maxits;
    }

    /**
	 * Default constructor.
	 */
    public LBFGS() {
    }

    /**
	 * Initialize LBFGS algorithm with precision parameters.
	 * 
	 * @param epsg positive number which  defines  a  precision  of  search.  The
	 subroutine finishes its work if on iteration  number  k+1  the
	 condition |F(k+1)-F(k)| <= EpsF*max{|F(k)|, |F(k+1)|, 1}    is
	 satisfied.
	 * @param epsf positive number which  defines  a  precision  of  search.  The
	 subroutine finishes its work if on iteration  number  k+1  the
	 condition |F(k+1)-F(k)| <= EpsF*max{|F(k)|, |F(k+1)|, 1}    is
	 satisfied.
	 * @param epsx positive number which  defines  a  precision  of  search.  The
	 subroutine finishes its work if on iteration number k+1    the
	 condition |X(k+1)-X(k)| <= EpsX is fulfilled.
	 * @param m number of corrections in the BFGS scheme of Hessian
	 approximation update. Recommended value:  3<=M<=7. The smaller
	 value causes worse convergence, the bigger will  not  cause  a
	 considerably better convergence, but will cause a fall in  the
	 performance. M<=N.
	 * @param maxits maximum number of iterations. If MaxIts=0, the number of
	 iterations is unlimited.
	 */
    public LBFGS(double epsg, double epsf, double epsx, int m, int maxits) {
        super();
        this.epsg = epsg;
        this.epsf = epsf;
        this.epsx = epsx;
        this.m = m;
        this.maxits = maxits;
    }

    public double[] calcMinimum(QuasiNewtonProblem problem) {
        this.problem = problem;
        double[] x = new double[problem.getDimension() + 1];
        padArray(problem.initValues(), x);
        double[] ret = new double[problem.getDimension()];
        double[] result = lbfgsminimize(problem.getDimension(), m, x, epsg, epsf, epsx, maxits);
        unpadArray(result, ret);
        return ret;
    }

    /*************************************************************************
	 * Following are excerpts Jorge Nodecal's notes on his FORTRAN subroutine:

	 Input parameters:
	 N   -   problem dimension. N>0
	 X   -   initial solution approximation.
	 Array whose index ranges from 1 to N.
	 MaxIts- maximum number of iterations. If MaxIts=0, the number of

	 Output parameters:
	 X   -   solution approximation. Array whose index ranges from 1 to N.
	 Info-   a return code:
	 * -1 wrong parameters were specified,
	 * 0 interrupted by user,
	 * 1 relative function decreasing is less or equal to EpsF,
	 * 2 step is less or equal EpsX,
	 * 4 gradient norm is less or equal to EpsG,
	 * 5 number of iterations exceeds MaxIts.
	 </pre>
	 @author Jorge Nocedal
	 *************************************************************************/
    public double[] lbfgsminimize(int n, int m, double[] x, double epsg, double epsf, double epsx, int maxits) {
        MInt info = new MInt();
        double[] w = new double[0];
        MDouble f = new MDouble();
        double fold = 0;
        double tf = 0;
        double txnorm = 0;
        double v = 0;
        double[] xold = new double[0];
        double[] tx = new double[0];
        double[] g = new double[0];
        double[] diag = new double[0];
        double gnorm = 0;
        double stp1 = 0;
        double ftol = 0;
        MDouble stp = new MDouble();
        double ys = 0;
        double yy = 0;
        double sq = 0;
        double yr = 0;
        double beta = 0;
        double xnorm = 0;
        int iter = 0;
        int nfun = 0;
        int point = 0;
        int ispt = 0;
        int iypt = 0;
        int maxfev = 0;
        int bound = 0;
        int npt = 0;
        int cp = 0;
        int i = 0;
        MInt nfev = new MInt();
        int inmc = 0;
        int iycn = 0;
        int iscn = 0;
        double xtol = 0;
        double gtol = 0;
        double stpmin = 0;
        double stpmax = 0;
        int i_ = 0;
        w = new double[(n + 1) * (2 * m + 1)];
        g = new double[n + 1];
        xold = new double[n + 1];
        tx = new double[n + 1];
        diag = new double[n + 1];
        if (DEBUG) {
            System.out.println("Iteration");
        }
        funcGrad(x, f, g);
        fold = f.value;
        iter = 0;
        info.value = 0;
        if (m > n) m = n;
        if (n <= 0 | m <= 0 | m > n | epsg < 0 | epsf < 0 | epsx < 0 | maxits < 0) {
            info.value = -1;
            return null;
        }
        nfun = 1;
        point = 0;
        for (i = 1; i <= n; i++) {
            diag[i] = 1;
        }
        xtol = 100 * MathUtils.MACHINE_EPS;
        gtol = 0.9;
        stpmin = Math.pow(10, -20);
        stpmax = Math.pow(10, 20);
        ispt = n + 2 * m;
        iypt = ispt + n * m;
        for (i = 1; i <= n; i++) {
            w[ispt + i] = -(g[i] * diag[i]);
        }
        gnorm = Math.sqrt(lbfgsdotproduct(n, g, 1, g, 1));
        stp1 = 1 / gnorm;
        ftol = 0.0001;
        maxfev = 20;
        while (true) {
            for (i_ = 1; i_ <= n; i_++) {
                xold[i_] = x[i_];
            }
            iter += 1;
            info.value = 0;
            bound = iter - 1;
            if (iter != 1) {
                if (iter > m) {
                    bound = m;
                }
                ys = lbfgsdotproduct(n, w, iypt + npt + 1, w, ispt + npt + 1);
                yy = lbfgsdotproduct(n, w, iypt + npt + 1, w, iypt + npt + 1);
                for (i = 1; i <= n; i++) {
                    diag[i] = ys / yy;
                }
                cp = point;
                if (point == 0) {
                    cp = m;
                }
                w[n + cp] = 1 / ys;
                for (i = 1; i <= n; i++) {
                    w[i] = -g[i];
                }
                cp = point;
                for (i = 1; i <= bound; i++) {
                    cp = cp - 1;
                    if (cp == -1) {
                        cp = m - 1;
                    }
                    sq = lbfgsdotproduct(n, w, ispt + cp * n + 1, w, 1);
                    inmc = n + m + cp + 1;
                    iycn = iypt + cp * n;
                    w[inmc] = w[n + cp + 1] * sq;
                    lbfgslincomb(n, -w[inmc], w, iycn + 1, w, 1);
                }
                for (i = 1; i <= n; i++) {
                    w[i] = diag[i] * w[i];
                }
                for (i = 1; i <= bound; i++) {
                    yr = lbfgsdotproduct(n, w, iypt + cp * n + 1, w, 1);
                    beta = w[n + cp + 1] * yr;
                    inmc = n + m + cp + 1;
                    beta = w[inmc] - beta;
                    iscn = ispt + cp * n;
                    lbfgslincomb(n, beta, w, iscn + 1, w, 1);
                    cp = cp + 1;
                    if (cp == m) {
                        cp = 0;
                    }
                }
                for (i = 1; i <= n; i++) {
                    w[ispt + point * n + i] = w[i];
                }
            }
            nfev.value = 0;
            stp.value = 1.0;
            if (iter == 1) {
                stp.value = stp1;
            }
            for (i = 1; i <= n; i++) {
                w[i] = g[i];
            }
            lbfgsmcsrch(n, x, f, g, w, ispt + point * n + 1, stp, ftol, xtol, maxfev, info, nfev, diag, gtol, stpmin, stpmax);
            if (info.value != 1) {
                if (info.value == 0) {
                    info.value = -1;
                    return null;
                }
            }
            nfun = nfun + nfev.value;
            npt = point * n;
            for (i = 1; i <= n; i++) {
                w[ispt + npt + i] = stp.value * w[ispt + npt + i];
                w[iypt + npt + i] = g[i] - w[i];
            }
            point = point + 1;
            if (point == m) {
                point = 0;
            }
            if (iter > maxits & maxits > 0) {
                info.value = 5;
                return x;
            }
            gnorm = Math.sqrt(lbfgsdotproduct(n, g, 1, g, 1));
            if (gnorm <= epsg) {
                info.value = 4;
                return x;
            }
            tf = Math.max(Math.abs(fold), Math.max(Math.abs(f.value), 1.0));
            if (fold - f.value <= epsf * tf) {
                info.value = 1;
                return x;
            }
            for (i_ = 1; i_ <= n; i_++) {
                tx[i_] = xold[i_];
            }
            for (i_ = 1; i_ <= n; i_++) {
                tx[i_] = tx[i_] - x[i_];
            }
            xnorm = Math.sqrt(lbfgsdotproduct(n, x, 1, x, 1));
            txnorm = Math.max(xnorm, Math.sqrt(lbfgsdotproduct(n, xold, 1, xold, 1)));
            txnorm = Math.max(txnorm, 1.0);
            v = Math.sqrt(lbfgsdotproduct(n, tx, 1, tx, 1));
            if (v <= epsx) {
                info.value = 2;
                return x;
            }
            fold = f.value;
            for (i_ = 1; i_ <= n; i_++) {
                xold[i_] = x[i_];
            }
        }
    }

    /**
	 * Adds da*v1 to v2 where v1 is the vector in dx (length n, first element
	 * dx[sx]) and v2 is the vector in dy (length n, first element dx[sy])
	 * 
	 * @param n the length of the vectors
	 * @param da the multiplication factor
	 * @param dx the array containing v1
	 * @param sx the offset of v1 in dx
	 * @param dy the array containing v2
	 * @param sy the offset of v2 in dy
	 */
    private static void lbfgslincomb(int n, double da, double[] dx, int sx, double[] dy, int sy) {
        int fy = 0;
        int i_ = 0;
        int i1_ = 0;
        fy = sy + n - 1;
        i1_ = (sx) - (sy);
        for (i_ = sy; i_ <= fy; i_++) {
            dy[i_] = dy[i_] + da * dx[i_ + i1_];
        }
    }

    /**
	 * Calculates the dot product of two vectors of length n
	 * 
	 * @param n the length of the vectors
	 * @param dx the array which contains the first vector
	 * @param sx the offset of dx where the first vector's first element is
	 * @param dy the array which contains the second vector
	 * @param sy the offset of dy where the second vector's first element is
	 * @return
	 */
    private static double lbfgsdotproduct(int n, double[] dx, int sx, double[] dy, int sy) {
        double result = 0;
        double v = 0;
        int fx = 0;
        int i_ = 0;
        int i1_ = 0;
        fx = sx + n - 1;
        i1_ = (sy) - (sx);
        v = 0.0;
        for (i_ = sx; i_ <= fx; i_++) {
            v += dx[i_] * dy[i_ + i1_];
        }
        result = v;
        return result;
    }

    /**
	 * Line Search
	 * 
	 * @param n dimension of the problem
	 * @param x current value
	 * @param f function value of x
	 * @param g gradient at x
	 * @param s the last m gradients(negated) ??
	 * @param sstart
	 * @param stp starting step value
	 * @param ftol c1??
	 * @param xtol 100*MACHINE_EPS
	 * @param maxfev
	 * @param info
	 * @param nfev
	 * @param wa
	 * @param gtol c2??
	 * @param stpmin the minimum step length
	 * @param stpmax the maximum step length
	 */
    private void lbfgsmcsrch(int n, double[] x, MDouble f, double[] g, double[] s, int sstart, MDouble stp, double ftol, double xtol, int maxfev, MInt info, MInt nfev, double[] wa, double gtol, double stpmin, double stpmax) {
        MInt infoc = new MInt();
        int j = 0;
        MBool brackt = new MBool();
        Boolean stage1 = false;
        double dg = 0;
        double dgm = 0;
        double dginit = 0;
        double dgtest = 0;
        MDouble dgx = new MDouble();
        MDouble dgxm = new MDouble();
        MDouble dgy = new MDouble();
        MDouble dgym = new MDouble();
        double finit = 0;
        double ftest1 = 0;
        double fm = 0;
        MDouble fx = new MDouble();
        MDouble fxm = new MDouble();
        MDouble fy = new MDouble();
        MDouble fym = new MDouble();
        double p5 = 0;
        double p66 = 0;
        MDouble stx = new MDouble();
        MDouble sty = new MDouble();
        double stmin = 0;
        double stmax = 0;
        double width = 0;
        double width1 = 0;
        double xtrapf = 0;
        double zero = 0;
        double mytemp = 0;
        sstart = sstart - 1;
        p5 = 0.5;
        p66 = 0.66;
        xtrapf = 4.0;
        zero = 0;
        if (DEBUG) {
            System.out.println("Start Linesearch");
        }
        funcGrad(x, f, g);
        infoc.value = 1;
        info.value = 0;
        if (n <= 0 | stp.value <= 0 | ftol < 0 | gtol < zero | xtol < zero | stpmin < zero | stpmax < stpmin | maxfev <= 0) {
            return;
        }
        dginit = 0;
        for (j = 1; j <= n; j++) {
            dginit += g[j] * s[j + sstart];
        }
        if (dginit >= 0) {
            return;
        }
        brackt.value = false;
        stage1 = true;
        nfev.value = 0;
        finit = f.value;
        dgtest = ftol * dginit;
        width = stpmax - stpmin;
        width1 = width / p5;
        for (j = 1; j <= n; j++) {
            wa[j] = x[j];
        }
        stx.value = 0.0;
        fx.value = finit;
        dgx.value = dginit;
        sty.value = 0.0;
        fy.value = finit;
        dgy.value = dginit;
        while (true) {
            if (brackt.value) {
                if (stx.value < sty.value) {
                    stmin = stx.value;
                    stmax = sty.value;
                } else {
                    stmin = sty.value;
                    stmax = stx.value;
                }
            } else {
                stmin = stx.value;
                stmax = stp.value + xtrapf * (stp.value - stx.value);
            }
            if (stp.value > stpmax) {
                stp.value = stpmax;
            }
            if (stp.value < stpmin) {
                stp.value = stpmin;
            }
            if (brackt.value & (stp.value <= stmin | stp.value >= stmax) | nfev.value >= maxfev - 1 | infoc.value == 0 | brackt.value & stmax - stmin <= xtol * stmax) {
                stp.value = stx.value;
            }
            for (j = 1; j <= n; j++) {
                x[j] = wa[j] + stp.value * s[j + sstart];
            }
            if (DEBUG) {
                System.out.println("Testing Step: " + stp.value);
            }
            funcGrad(x, f, g);
            info.value = 0;
            nfev.value = nfev.value + 1;
            dg = 0;
            for (j = 1; j <= n; j++) {
                dg = dg + g[j] * s[j + sstart];
            }
            ftest1 = finit + stp.value * dgtest;
            if (brackt.value & (stp.value <= stmin | stp.value >= stmax) | infoc.value == 0) {
                info.value = 6;
            }
            if (stp.value == stpmax & f.value <= ftest1 & dg <= dgtest) {
                info.value = 5;
            }
            if (stp.value == stpmin & (f.value > ftest1 | dg >= dgtest)) {
                info.value = 4;
            }
            if (nfev.value >= maxfev) {
                info.value = 3;
            }
            if (brackt.value & stmax - stmin <= xtol * stmax) {
                info.value = 2;
            }
            if (f.value <= ftest1 & Math.abs(dg) <= -(gtol * dginit)) {
                info.value = 1;
            }
            if (info.value != 0) {
                return;
            }
            mytemp = ftol;
            if (gtol < ftol) {
                mytemp = gtol;
            }
            if (stage1 & f.value <= ftest1 & dg >= mytemp * dginit) {
                stage1 = false;
            }
            if (stage1 & f.value <= fx.value & f.value > ftest1) {
                fm = f.value - stp.value * dgtest;
                fxm.value = fx.value - stx.value * dgtest;
                fym.value = fy.value - sty.value * dgtest;
                dgm = dg - dgtest;
                dgxm.value = dgx.value - dgtest;
                dgym.value = dgy.value - dgtest;
                lbfgsmcstep(stx, fxm, dgxm, sty, fym, dgym, stp, fm, dgm, brackt, stmin, stmax, infoc);
                fx.value = fxm.value + stx.value * dgtest;
                fy.value = fym.value + sty.value * dgtest;
                dgx.value = dgxm.value + dgtest;
                dgy.value = dgym.value + dgtest;
            } else {
                lbfgsmcstep(stx, fx, dgx, sty, fy, dgy, stp, f.value, dg, brackt, stmin, stmax, infoc);
            }
            if (brackt.value) {
                if (Math.abs(sty.value - stx.value) >= p66 * width1) {
                    stp.value = stx.value + p5 * (sty.value - stx.value);
                }
                width1 = width;
                width = Math.abs(sty.value - stx.value);
            }
        }
    }

    private static void lbfgsmcstep(MDouble stx, MDouble fx, MDouble dx, MDouble sty, MDouble fy, MDouble dy, MDouble stp, double fp, double dp, MBool brackt, double stmin, double stmax, MInt info) {
        Boolean bound = false;
        double gamma = 0;
        double p = 0;
        double q = 0;
        double r = 0;
        double s = 0;
        double sgnd = 0;
        double stpc = 0;
        double stpf = 0;
        double stpq = 0;
        double theta = 0;
        info.value = 0;
        if (brackt.value & (stp.value <= Math.min(stx.value, sty.value) | stp.value >= Math.max(stx.value, sty.value)) | dx.value * (stp.value - stx.value) >= 0 | stmax < stmin) {
            return;
        }
        sgnd = dp * (dx.value / Math.abs(dx.value));
        if (fp > fx.value) {
            info.value = 1;
            bound = true;
            theta = 3 * (fx.value - fp) / (stp.value - stx.value) + dx.value + dp;
            s = Math.max(Math.abs(theta), Math.max(Math.abs(dx.value), Math.abs(dp)));
            gamma = s * Math.sqrt(Math.pow(theta / s, 2.0) - dx.value / s * (dp / s));
            if (stp.value < stx.value) {
                gamma = -gamma;
            }
            p = gamma - dx.value + theta;
            q = gamma - dx.value + gamma + dp;
            r = p / q;
            stpc = stx.value + r * (stp.value - stx.value);
            stpq = stx.value + dx.value / ((fx.value - fp) / (stp.value - stx.value) + dx.value) / 2 * (stp.value - stx.value);
            if (Math.abs(stpc - stx.value) < Math.abs(stpq - stx.value)) {
                stpf = stpc;
            } else {
                stpf = stpc + (stpq - stpc) / 2;
            }
            brackt.value = true;
        } else {
            if (sgnd < 0) {
                info.value = 2;
                bound = false;
                theta = 3 * (fx.value - fp) / (stp.value - stx.value) + dx.value + dp;
                s = Math.max(Math.abs(theta), Math.max(Math.abs(dx.value), Math.abs(dp)));
                gamma = s * Math.sqrt(Math.pow(theta / s, 2.0) - dx.value / s * (dp / s));
                if (stp.value > stx.value) {
                    gamma = -gamma;
                }
                p = gamma - dp + theta;
                q = gamma - dp + gamma + dx.value;
                r = p / q;
                stpc = stp.value + r * (stx.value - stp.value);
                stpq = stp.value + dp / (dp - dx.value) * (stx.value - stp.value);
                if (Math.abs(stpc - stp.value) > Math.abs(stpq - stp.value)) {
                    stpf = stpc;
                } else {
                    stpf = stpq;
                }
                brackt.value = true;
            } else {
                if (Math.abs(dp) < Math.abs(dx.value)) {
                    info.value = 3;
                    bound = true;
                    theta = 3 * (fx.value - fp) / (stp.value - stx.value) + dx.value + dp;
                    s = Math.max(Math.abs(theta), Math.max(Math.abs(dx.value), Math.abs(dp)));
                    gamma = s * Math.sqrt(Math.max(0, Math.pow(theta / s, 2.0) - dx.value / s * (dp / s)));
                    if (stp.value > stx.value) {
                        gamma = -gamma;
                    }
                    p = gamma - dp + theta;
                    q = gamma + (dx.value - dp) + gamma;
                    r = p / q;
                    if (r < 0 & gamma != 0) {
                        stpc = stp.value + r * (stx.value - stp.value);
                    } else {
                        if (stp.value > stx.value) {
                            stpc = stmax;
                        } else {
                            stpc = stmin;
                        }
                    }
                    stpq = stp.value + dp / (dp - dx.value) * (stx.value - stp.value);
                    if (brackt.value) {
                        if (Math.abs(stp.value - stpc) < Math.abs(stp.value - stpq)) {
                            stpf = stpc;
                        } else {
                            stpf = stpq;
                        }
                    } else {
                        if (Math.abs(stp.value - stpc) > Math.abs(stp.value - stpq)) {
                            stpf = stpc;
                        } else {
                            stpf = stpq;
                        }
                    }
                } else {
                    info.value = 4;
                    bound = false;
                    if (brackt.value) {
                        theta = 3 * (fp - fy.value) / (sty.value - stp.value) + dy.value + dp;
                        s = Math.max(Math.abs(theta), Math.max(Math.abs(dy.value), Math.abs(dp)));
                        gamma = s * Math.sqrt(Math.pow(theta / s, 2.0) - dy.value / s * (dp / s));
                        if (stp.value > sty.value) {
                            gamma = -gamma;
                        }
                        p = gamma - dp + theta;
                        q = gamma - dp + gamma + dy.value;
                        r = p / q;
                        stpc = stp.value + r * (sty.value - stp.value);
                        stpf = stpc;
                    } else {
                        if (stp.value > stx.value) {
                            stpf = stmax;
                        } else {
                            stpf = stmin;
                        }
                    }
                }
            }
        }
        if (fp > fx.value) {
            sty.value = stp.value;
            fy.value = fp;
            dy.value = dp;
        } else {
            if (sgnd < 0.0) {
                sty.value = stx.value;
                fy.value = fx.value;
                dy.value = dx.value;
            }
            stx.value = stp.value;
            fx.value = fp;
            dx.value = dp;
        }
        stpf = Math.min(stmax, stpf);
        stpf = Math.max(stmin, stpf);
        stp.value = stpf;
        if (brackt.value & bound) {
            if (sty.value > stx.value) {
                stp.value = Math.min(stx.value + 0.66 * (sty.value - stx.value), stp.value);
            } else {
                stp.value = Math.max(stx.value + 0.66 * (sty.value - stx.value), stp.value);
            }
        }
    }
}
