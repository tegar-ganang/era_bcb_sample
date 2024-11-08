package joptima.tests;

import joptima.fortran.*;

/**
*
*This class tests the Minpack_f77.lmdif1_f77 method, a Java
*translation of the MINPACK lmdif1 subroutine.
*This class is based
*on FORTRAN test code for lmdif that is available at Netlib
*(<a href="http://www.netlib.org/minpack/ex/">
*http://www.netlib.org/minpack/ex/</a>).  
*The Netlib test code was provided by the authors of MINPACK,
*Burton S. Garbow, Kenneth E. Hillstrom, and Jorge J. More.
*
*@author (translator) Steve Verrill
*@version .5 --- December 2, 2000
*
*/
public class LmdifTest_f77 extends Object implements Lmdif_fcn {

    static final double epsmch = 2.22044604926e-16;

    static final double zero = 0.0;

    static final double half = .5;

    static final double one = 1.0;

    static final double two = 2.0;

    static final double three = 3.0;

    static final double four = 4.0;

    static final double five = 5.0;

    static final double seven = 7.0;

    static final double eight = 8.0;

    static final double ten = 10.0;

    static final double twenty = 20.0;

    static final double twntf = 25.0;

    int nfev = 0;

    int njev = 0;

    int nprob = 0;

    public static void main(String args[]) {
        int iii, i, ic, k, m, n, nread, ntries, nwrite;
        int info[] = new int[2];
        int nprobfile[] = new int[29];
        int nfile[] = new int[29];
        int mfile[] = new int[29];
        int ntryfile[] = new int[29];
        int ma[] = new int[61];
        int na[] = new int[61];
        int nf[] = new int[61];
        int nj[] = new int[61];
        int np[] = new int[61];
        int nx[] = new int[61];
        double factor, fnorm1, fnorm2, tol;
        double fnm[] = new double[61];
        double fvec[] = new double[66];
        double x[] = new double[41];
        int ipvt[] = new int[41];
        int num5, ilow, numleft;
        nprobfile[1] = 1;
        nprobfile[2] = 1;
        nprobfile[3] = 2;
        nprobfile[4] = 2;
        nprobfile[5] = 3;
        nprobfile[6] = 3;
        nprobfile[7] = 4;
        nprobfile[8] = 5;
        nprobfile[9] = 6;
        nprobfile[10] = 7;
        nprobfile[11] = 8;
        nprobfile[12] = 9;
        nprobfile[13] = 10;
        nprobfile[14] = 11;
        nprobfile[15] = 11;
        nprobfile[16] = 11;
        nprobfile[17] = 12;
        nprobfile[18] = 13;
        nprobfile[19] = 14;
        nprobfile[20] = 15;
        nprobfile[21] = 15;
        nprobfile[22] = 15;
        nprobfile[23] = 15;
        nprobfile[24] = 16;
        nprobfile[25] = 16;
        nprobfile[26] = 16;
        nprobfile[27] = 17;
        nprobfile[28] = 18;
        nfile[1] = 5;
        nfile[2] = 5;
        nfile[3] = 5;
        nfile[4] = 5;
        nfile[5] = 5;
        nfile[6] = 5;
        nfile[7] = 2;
        nfile[8] = 3;
        nfile[9] = 4;
        nfile[10] = 2;
        nfile[11] = 3;
        nfile[12] = 4;
        nfile[13] = 3;
        nfile[14] = 6;
        nfile[15] = 9;
        nfile[16] = 12;
        nfile[17] = 3;
        nfile[18] = 2;
        nfile[19] = 4;
        nfile[20] = 1;
        nfile[21] = 8;
        nfile[22] = 9;
        nfile[23] = 10;
        nfile[24] = 10;
        nfile[25] = 30;
        nfile[26] = 40;
        nfile[27] = 5;
        nfile[28] = 11;
        mfile[1] = 10;
        mfile[2] = 50;
        mfile[3] = 10;
        mfile[4] = 50;
        mfile[5] = 10;
        mfile[6] = 50;
        mfile[7] = 2;
        mfile[8] = 3;
        mfile[9] = 4;
        mfile[10] = 2;
        mfile[11] = 15;
        mfile[12] = 11;
        mfile[13] = 16;
        mfile[14] = 31;
        mfile[15] = 31;
        mfile[16] = 31;
        mfile[17] = 10;
        mfile[18] = 10;
        mfile[19] = 20;
        mfile[20] = 8;
        mfile[21] = 8;
        mfile[22] = 9;
        mfile[23] = 10;
        mfile[24] = 10;
        mfile[25] = 30;
        mfile[26] = 40;
        mfile[27] = 33;
        mfile[28] = 65;
        ntryfile[1] = 1;
        ntryfile[2] = 1;
        ntryfile[3] = 1;
        ntryfile[4] = 1;
        ntryfile[5] = 1;
        ntryfile[6] = 1;
        ntryfile[7] = 3;
        ntryfile[8] = 3;
        ntryfile[9] = 3;
        ntryfile[10] = 3;
        ntryfile[11] = 3;
        ntryfile[12] = 3;
        ntryfile[13] = 2;
        ntryfile[14] = 3;
        ntryfile[15] = 3;
        ntryfile[16] = 3;
        ntryfile[17] = 1;
        ntryfile[18] = 1;
        ntryfile[19] = 3;
        ntryfile[20] = 3;
        ntryfile[21] = 1;
        ntryfile[22] = 1;
        ntryfile[23] = 1;
        ntryfile[24] = 3;
        ntryfile[25] = 1;
        ntryfile[26] = 1;
        ntryfile[27] = 1;
        ntryfile[28] = 1;
        tol = Math.sqrt(epsmch);
        ic = 0;
        for (iii = 1; iii <= 28; iii++) {
            n = nfile[iii];
            m = mfile[iii];
            ntries = ntryfile[iii];
            LmdifTest_f77 lmdiftest = new LmdifTest_f77();
            lmdiftest.nprob = nprobfile[iii];
            factor = one;
            for (k = 1; k <= ntries; k++) {
                ic++;
                LmdifTest_f77.initpt_f77(n, x, lmdiftest.nprob, factor);
                LmdifTest_f77.ssqfcn_f77(m, n, x, fvec, lmdiftest.nprob);
                fnorm1 = Minpack_f77.enorm_f77(m, fvec);
                System.out.print("\n\n\n\n\n problem " + lmdiftest.nprob + ", dimensions:  " + n + "  " + m + "\n");
                lmdiftest.nfev = 0;
                lmdiftest.njev = 0;
                Minpack_f77.lmdif1_f77(lmdiftest, m, n, x, fvec, tol, info);
                LmdifTest_f77.ssqfcn_f77(m, n, x, fvec, lmdiftest.nprob);
                fnorm2 = Minpack_f77.enorm_f77(m, fvec);
                np[ic] = lmdiftest.nprob;
                na[ic] = n;
                ma[ic] = m;
                nf[ic] = lmdiftest.nfev;
                nj[ic] = lmdiftest.njev / n;
                nx[ic] = info[1];
                fnm[ic] = fnorm2;
                System.out.print("\n Initial L2 norm of the residuals: " + fnorm1 + "\n Final L2 norm of the residuals: " + fnorm2 + "\n Number of function evaluations: " + nf[ic] + "\n Number of Jacobian evaluations: " + nj[ic] + "\n Info value: " + info[1] + "\n Final approximate solution: \n\n");
                num5 = n / 5;
                for (i = 1; i <= num5; i++) {
                    ilow = (i - 1) * 5;
                    System.out.print(x[ilow + 1] + "  " + x[ilow + 2] + "  " + x[ilow + 3] + "  " + x[ilow + 4] + "  " + x[ilow + 5] + "\n");
                }
                numleft = n % 5;
                ilow = n - numleft;
                switch(numleft) {
                    case 1:
                        System.out.print(x[ilow + 1] + "\n\n");
                        break;
                    case 2:
                        System.out.print(x[ilow + 1] + "  " + x[ilow + 2] + "\n\n");
                        break;
                    case 3:
                        System.out.print(x[ilow + 1] + "  " + x[ilow + 2] + "  " + x[ilow + 3] + "\n\n");
                        break;
                    case 4:
                        System.out.print(x[ilow + 1] + "  " + x[ilow + 2] + "  " + x[ilow + 3] + "  " + x[ilow + 4] + "\n\n");
                        break;
                }
                factor *= ten;
            }
        }
        System.out.print("\n\n\n Summary of " + ic + " calls to lmdif1: \n\n");
        System.out.print("\n\n nprob   n    m   nfev  njev  info  final L2 norm \n\n");
        for (i = 1; i <= ic; i++) {
            System.out.print(np[i] + "  " + na[i] + "  " + ma[i] + "  " + nf[i] + "  " + nj[i] + "  " + nx[i] + "  " + fnm[i] + "\n");
        }
    }

    public void fcn(int m, int n, double x[], double fvec[], int iflag[]) {
        LmdifTest_f77.ssqfcn_f77(m, n, x, fvec, this.nprob);
        if (iflag[1] == 1) this.nfev++;
        if (iflag[1] == 2) this.njev++;
        return;
    }

    public static void ssqfcn_f77(int m, int n, double x[], double fvec[], int nprob) {
        int i, iev, j, nm1;
        double c13, c14, c29, c45, div, dx, prod, sum, s1, s2, temp, ti, tmp1, tmp2, tmp3, tmp4, tpi, zp5;
        zp5 = .5;
        c13 = 13.0;
        c14 = 14.0;
        c29 = 29.0;
        c45 = 45.0;
        double v[] = { -9999.0, 4.0e0, 2.0e0, 1.0e0, 5.0e-1, 2.5e-1, 1.67e-1, 1.25e-1, 1.0e-1, 8.33e-2, 7.14e-2, 6.25e-2 };
        double y1[] = { -9999.0, 1.4e-1, 1.8e-1, 2.2e-1, 2.5e-1, 2.9e-1, 3.2e-1, 3.5e-1, 3.9e-1, 3.7e-1, 5.8e-1, 7.3e-1, 9.6e-1, 1.34e0, 2.1e0, 4.39e0 };
        double y2[] = { -9999.0, 1.957e-1, 1.947e-1, 1.735e-1, 1.6e-1, 8.44e-2, 6.27e-2, 4.56e-2, 3.42e-2, 3.23e-2, 2.35e-2, 2.46e-2 };
        double y3[] = { -9999.0, 3.478e4, 2.861e4, 2.365e4, 1.963e4, 1.637e4, 1.372e4, 1.154e4, 9.744e3, 8.261e3, 7.03e3, 6.005e3, 5.147e3, 4.427e3, 3.82e3, 3.307e3, 2.872e3 };
        double y4[] = { -9999.0, 8.44e-1, 9.08e-1, 9.32e-1, 9.36e-1, 9.25e-1, 9.08e-1, 8.81e-1, 8.5e-1, 8.18e-1, 7.84e-1, 7.51e-1, 7.18e-1, 6.85e-1, 6.58e-1, 6.28e-1, 6.03e-1, 5.8e-1, 5.58e-1, 5.38e-1, 5.22e-1, 5.06e-1, 4.9e-1, 4.78e-1, 4.67e-1, 4.57e-1, 4.48e-1, 4.38e-1, 4.31e-1, 4.24e-1, 4.2e-1, 4.14e-1, 4.11e-1, 4.06e-1 };
        double y5[] = { -9999.0, 1.366e0, 1.191e0, 1.112e0, 1.013e0, 9.91e-1, 8.85e-1, 8.31e-1, 8.47e-1, 7.86e-1, 7.25e-1, 7.46e-1, 6.79e-1, 6.08e-1, 6.55e-1, 6.16e-1, 6.06e-1, 6.02e-1, 6.26e-1, 6.51e-1, 7.24e-1, 6.49e-1, 6.49e-1, 6.94e-1, 6.44e-1, 6.24e-1, 6.61e-1, 6.12e-1, 5.58e-1, 5.33e-1, 4.95e-1, 5.0e-1, 4.23e-1, 3.95e-1, 3.75e-1, 3.72e-1, 3.91e-1, 3.96e-1, 4.05e-1, 4.28e-1, 4.29e-1, 5.23e-1, 5.62e-1, 6.07e-1, 6.53e-1, 6.72e-1, 7.08e-1, 6.33e-1, 6.68e-1, 6.45e-1, 6.32e-1, 5.91e-1, 5.59e-1, 5.97e-1, 6.25e-1, 7.39e-1, 7.1e-1, 7.29e-1, 7.2e-1, 6.36e-1, 5.81e-1, 4.28e-1, 2.92e-1, 1.62e-1, 9.8e-2, 5.4e-2 };
        switch(nprob) {
            case 1:
                sum = zero;
                for (j = 1; j <= n; j++) {
                    sum += x[j];
                }
                temp = two * sum / m + one;
                for (i = 1; i <= m; i++) {
                    fvec[i] = -temp;
                    if (i <= n) fvec[i] += x[i];
                }
                return;
            case 2:
                sum = zero;
                for (j = 1; j <= n; j++) {
                    sum += j * x[j];
                }
                for (i = 1; i <= m; i++) {
                    fvec[i] = i * sum - one;
                }
                return;
            case 3:
                sum = zero;
                nm1 = n - 1;
                for (j = 2; j <= nm1; j++) {
                    sum += j * x[j];
                }
                for (i = 1; i <= m; i++) {
                    fvec[i] = (i - 1) * sum - one;
                }
                fvec[m] = -one;
                return;
            case 4:
                fvec[1] = ten * (x[2] - x[1] * x[1]);
                fvec[2] = one - x[1];
                return;
            case 5:
                tpi = eight * Math.atan(one);
                if (x[2] < 0.0) {
                    tmp1 = -.25;
                } else {
                    tmp1 = .25;
                }
                if (x[1] > zero) tmp1 = Math.atan(x[2] / x[1]) / tpi;
                if (x[1] < zero) tmp1 = Math.atan(x[2] / x[1]) / tpi + zp5;
                tmp2 = Math.sqrt(x[1] * x[1] + x[2] * x[2]);
                fvec[1] = ten * (x[3] - ten * tmp1);
                fvec[2] = ten * (tmp2 - one);
                fvec[3] = x[3];
                return;
            case 6:
                fvec[1] = x[1] + ten * x[2];
                fvec[2] = Math.sqrt(five) * (x[3] - x[4]);
                fvec[3] = Math.pow(x[2] - two * x[3], 2);
                fvec[4] = Math.sqrt(ten) * Math.pow(x[1] - x[4], 2);
                return;
            case 7:
                fvec[1] = -c13 + x[1] + ((five - x[2]) * x[2] - two) * x[2];
                fvec[2] = -c29 + x[1] + ((one + x[2]) * x[2] - c14) * x[2];
                return;
            case 8:
                for (i = 1; i <= 15; i++) {
                    tmp1 = i;
                    tmp2 = 16 - i;
                    tmp3 = tmp1;
                    if (i > 8) tmp3 = tmp2;
                    fvec[i] = y1[i] - (x[1] + tmp1 / (x[2] * tmp2 + x[3] * tmp3));
                }
                return;
            case 9:
                for (i = 1; i <= 11; i++) {
                    tmp1 = v[i] * (v[i] + x[2]);
                    tmp2 = v[i] * (v[i] + x[3]) + x[4];
                    fvec[i] = y2[i] - x[1] * tmp1 / tmp2;
                }
                return;
            case 10:
                for (i = 1; i <= 16; i++) {
                    temp = five * i + c45 + x[3];
                    tmp1 = x[2] / temp;
                    tmp2 = Math.exp(tmp1);
                    fvec[i] = x[1] * tmp2 - y3[i];
                }
                return;
            case 11:
                for (i = 1; i <= 29; i++) {
                    div = i / c29;
                    s1 = zero;
                    dx = one;
                    for (j = 2; j <= n; j++) {
                        s1 += (j - 1) * dx * x[j];
                        dx *= div;
                    }
                    s2 = zero;
                    dx = one;
                    for (j = 1; j <= n; j++) {
                        s2 += dx * x[j];
                        dx *= div;
                    }
                    fvec[i] = s1 - s2 * s2 - one;
                }
                fvec[30] = x[1];
                fvec[31] = x[2] - x[1] * x[1] - one;
                return;
            case 12:
                for (i = 1; i <= m; i++) {
                    temp = i;
                    tmp1 = temp / ten;
                    fvec[i] = Math.exp(-tmp1 * x[1]) - Math.exp(-tmp1 * x[2]) + (Math.exp(-temp) - Math.exp(-tmp1)) * x[3];
                }
                return;
            case 13:
                for (i = 1; i <= m; i++) {
                    temp = i;
                    fvec[i] = two + two * temp - Math.exp(temp * x[1]) - Math.exp(temp * x[2]);
                }
                return;
            case 14:
                for (i = 1; i <= m; i++) {
                    temp = i / five;
                    tmp1 = x[1] + temp * x[2] - Math.exp(temp);
                    tmp2 = x[3] + Math.sin(temp) * x[4] - Math.cos(temp);
                    fvec[i] = tmp1 * tmp1 + tmp2 * tmp2;
                }
                return;
            case 15:
                for (i = 1; i <= m; i++) {
                    fvec[i] = zero;
                }
                for (j = 1; j <= n; j++) {
                    tmp1 = one;
                    tmp2 = two * x[j] - one;
                    temp = two * tmp2;
                    for (i = 1; i <= m; i++) {
                        fvec[i] += tmp2;
                        ti = temp * tmp2 - tmp1;
                        tmp1 = tmp2;
                        tmp2 = ti;
                    }
                }
                dx = one / n;
                iev = -1;
                for (i = 1; i <= m; i++) {
                    fvec[i] *= dx;
                    if (iev > 0) fvec[i] += one / (i * i - one);
                    iev = -iev;
                }
                return;
            case 16:
                sum = -(n + 1);
                prod = one;
                for (j = 1; j <= n; j++) {
                    sum += x[j];
                    prod *= x[j];
                }
                for (i = 1; i <= n; i++) {
                    fvec[i] = x[i] + sum;
                }
                fvec[n] = prod - one;
                return;
            case 17:
                for (i = 1; i <= 33; i++) {
                    temp = ten * (i - 1);
                    tmp1 = Math.exp(-x[4] * temp);
                    tmp2 = Math.exp(-x[5] * temp);
                    fvec[i] = y4[i] - (x[1] + x[2] * tmp1 + x[3] * tmp2);
                }
                return;
            case 18:
                for (i = 1; i <= 65; i++) {
                    temp = (i - 1) / ten;
                    tmp1 = Math.exp(-x[5] * temp);
                    tmp2 = Math.exp(-x[6] * (temp - x[9]) * (temp - x[9]));
                    tmp3 = Math.exp(-x[7] * (temp - x[10]) * (temp - x[10]));
                    tmp4 = Math.exp(-x[8] * (temp - x[11]) * (temp - x[11]));
                    fvec[i] = y5[i] - (x[1] * tmp1 + x[2] * tmp2 + x[3] * tmp3 + x[4] * tmp4);
                }
                return;
        }
    }

    public static void initpt_f77(int n, double x[], int nprob, double factor) {
        int j;
        double c1, c2, c3, c4, c5, c6, c7, c8, c9, c10, c11, c12, c13, c14, c15, c16, c17, h;
        c1 = 1.2;
        c2 = .25;
        c3 = .39;
        c4 = .415;
        c5 = .02;
        c6 = 4000.0;
        c7 = 250.0;
        c8 = .3;
        c9 = .4;
        c10 = 1.5;
        c11 = .01;
        c12 = 1.3;
        c13 = .65;
        c14 = .7;
        c15 = .6;
        c16 = 4.5;
        c17 = 5.5;
        switch(nprob) {
            case 1:
                for (j = 1; j <= n; j++) {
                    x[j] = one;
                }
                break;
            case 2:
                for (j = 1; j <= n; j++) {
                    x[j] = one;
                }
                break;
            case 3:
                for (j = 1; j <= n; j++) {
                    x[j] = one;
                }
                break;
            case 4:
                x[1] = -c1;
                x[2] = one;
                break;
            case 5:
                x[1] = -one;
                x[2] = zero;
                x[3] = zero;
                break;
            case 6:
                x[1] = three;
                x[2] = -one;
                x[3] = zero;
                x[4] = one;
                break;
            case 7:
                x[1] = half;
                x[2] = -two;
                break;
            case 8:
                x[1] = one;
                x[2] = one;
                x[3] = one;
                break;
            case 9:
                x[1] = c2;
                x[2] = c3;
                x[3] = c4;
                x[4] = c3;
                break;
            case 10:
                x[1] = c5;
                x[2] = c6;
                x[3] = c7;
                break;
            case 11:
                for (j = 1; j <= n; j++) {
                    x[j] = zero;
                }
                break;
            case 12:
                x[1] = zero;
                x[2] = ten;
                x[3] = twenty;
                break;
            case 13:
                x[1] = c8;
                x[2] = c9;
                break;
            case 14:
                x[1] = twntf;
                x[2] = five;
                x[3] = -five;
                x[4] = -one;
                break;
            case 15:
                h = one / (n + 1);
                for (j = 1; j <= n; j++) {
                    x[j] = j * h;
                }
                break;
            case 16:
                for (j = 1; j <= n; j++) {
                    x[j] = half;
                }
                break;
            case 17:
                x[1] = half;
                x[2] = c10;
                x[3] = -one;
                x[4] = c11;
                x[5] = c5;
                break;
            case 18:
                x[1] = c12;
                x[2] = c13;
                x[3] = c13;
                x[4] = c14;
                x[5] = c15;
                x[6] = three;
                x[7] = five;
                x[8] = seven;
                x[9] = two;
                x[10] = c16;
                x[11] = c17;
        }
        if (factor == one) return;
        if (nprob != 11) {
            for (j = 1; j <= n; j++) {
                x[j] *= factor;
            }
        } else {
            for (j = 1; j <= n; j++) {
                x[j] = factor;
            }
        }
        return;
    }
}
