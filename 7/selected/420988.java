package geovista.cartogram;

import java.awt.geom.GeneralPath;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nick
 * 
 */
public class TransformsMain {

    static final Logger logger = Logger.getLogger(TransformsMain.class.getName());

    private String genFileName = "./cartogram.gen";

    private String dataFileName = "./census.dat";

    private String polygonFileName = "./map.gen";

    private int maxNSquareLog = 20;

    private double blurWidth = 0.1;

    private double blurWidthFactor = 1.2;

    private int arrayLength;

    public static final int DEFAULT_MAXNSQUARELOG = 10;

    public static final double DEFAULT_BLURWIDTH = 0.1;

    public static final double DEFAULT_BLURWIDTHFACTOR = 1.2;

    private static final String CART2PS = "";

    private static final String MAP2PS = "";

    private static final double CONVERGENCE = 1e-100;

    private static final double HINITIAL = 1e-4;

    private static final int IMAX = 50;

    private static final int MAXINTSTEPS = 3000;

    private static final double MINH = 1e-5;

    private static final int NSUBDIV = 1;

    private static final double PADDING = 1.5;

    private static final double PI = 3.141592653589793;

    private static final double TIMELIMIT = 1e8;

    private static final double TOLF = 1e-3;

    private static final double TOLINT = 1e-3;

    private static final double TOLX = 1e-3;

    public static final String DISPLFILE = null;

    GeneralPath[] inputShapes;

    GeneralPath[] outputShapes;

    ArrayFloat gridvx[], gridvy[];

    float maxx, maxy, minpop, minx, miny, polymaxx, polymaxy, polyminx, polyminy;

    ArrayFloat rho[], rho_0[], vx[], vy[], x[], y[], xappr[], yappr[];

    float xstepsize, ystepsize;

    int lx, ly, maxid, nblurs = 0, npoly;

    int ncorn[], polygonid[];

    ArrayPoint corn[];

    private static double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));
        double ans = 1 - t * Math.exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * (0.17087277))))))))));
        if (z >= 0) {
            return ans;
        }
        return -ans;
    }

    private void countpoly() {
        String line;
        BufferedReader inFile = FileTools.openFileRead(polygonFileName);
        if (inFile == null) {
            throw new RuntimeException("Could not open " + polygonFileName);
        }
        while ((line = FileTools.readLine(inFile)) != null) {
            if (line.charAt(0) == 'E') {
                npoly++;
            }
        }
        npoly--;
        FileTools.closeFile(inFile);
    }

    private void countcorn() {
        String line;
        BufferedReader inFile = FileTools.openFileRead(polygonFileName);
        float x, y;
        int polyctr = 0, ratiolog;
        countpoly();
        ncorn = new int[npoly];
        corn = new ArrayPoint[npoly];
        FileTools.readLine(inFile);
        line = FileTools.readLine(inFile);
        StringTokenizer st = new StringTokenizer(line);
        x = Float.parseFloat(st.nextToken());
        y = Float.parseFloat(st.nextToken());
        polyminx = x;
        polymaxx = x;
        polyminy = y;
        polymaxy = y;
        ncorn[0] = 1;
        while ((line = FileTools.readLine(inFile)) != null) {
            if (line.charAt(0) != 'E') {
                StringTokenizer st2 = new StringTokenizer(line);
                x = Float.parseFloat(st2.nextToken());
                y = Float.parseFloat(st2.nextToken());
                if (x < polyminx) {
                    polyminx = x;
                }
                if (x > polymaxx) {
                    polymaxx = x;
                }
                if (y < polyminy) {
                    polyminy = y;
                }
                if (y > polymaxy) {
                    polymaxy = y;
                }
                ncorn[polyctr]++;
            } else {
                line = FileTools.readLine(inFile);
                corn[polyctr] = new ArrayPoint(ncorn[polyctr]);
                polyctr++;
            }
        }
        if (Math.ceil(Math.log((polymaxx - polyminx) / (polymaxy - polyminy)) / Math.log(2)) + Math.floor(Math.log((polymaxx - polyminx) / (polymaxy - polyminy)) / Math.log(2)) > 2 * Math.log((polymaxx - polyminx) / (polymaxy - polyminy)) / Math.log(2)) {
            ratiolog = (int) Math.floor(Math.log((polymaxx - polyminx) / (polymaxy - polyminy)) / Math.log(2));
        } else {
            ratiolog = (int) Math.ceil(Math.log((polymaxx - polyminx) / (polymaxy - polyminy)) / Math.log(2));
        }
        lx = (int) Math.pow(2, (int) (0.5 * (ratiolog + maxNSquareLog)));
        ly = (int) Math.pow(2, (int) (0.5 * (maxNSquareLog - ratiolog)));
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("lx=" + lx + ", ly=" + ly + "\n");
        }
        if ((polymaxx - polyminx) / lx > (polymaxy - polyminy) / ly) {
            maxx = (float) (0.5 * ((1 + PADDING) * polymaxx + (1 - PADDING) * polyminx));
            minx = (float) (0.5 * ((1 - PADDING) * polymaxx + (1 + PADDING) * polyminx));
            maxy = (float) (0.5 * (polymaxy + polyminy + (maxx - minx) * ly / lx));
            miny = (float) (0.5 * (polymaxy + polyminy - (maxx - minx) * ly / lx));
        } else {
            maxy = (float) (0.5 * ((1 + PADDING) * polymaxy + (1 - PADDING) * polyminy));
            miny = (float) (0.5 * ((1 - PADDING) * polymaxy + (1 + PADDING) * polyminy));
            maxx = (float) (0.5 * (polymaxx + polyminx + (maxy - miny) * lx / ly));
            minx = (float) (0.5 * (polymaxx + polyminx - (maxy - miny) * lx / ly));
        }
        FileTools.closeFile(inFile);
    }

    private void readcorn() {
        String line;
        BufferedReader inFile = FileTools.openFileRead(polygonFileName);
        float xcoord, ycoord;
        int i, id, polyctr = 0;
        countcorn();
        polygonid = new int[npoly];
        xstepsize = (maxx - minx) / lx;
        ystepsize = (maxy - miny) / ly;
        if (Math.abs((xstepsize / ystepsize) - 1) > 1e-3) {
            System.err.println("WARNING: Area elements are not square: " + xstepsize + " : " + ystepsize + "\n");
        }
        line = FileTools.readLine(inFile);
        id = FileTools.readInt(line);
        polygonid[polyctr] = id;
        i = 0;
        while ((line = FileTools.readLine(inFile)) != null) {
            if (line.charAt(0) != 'E') {
                StringTokenizer st2 = new StringTokenizer(line);
                xcoord = Float.parseFloat(st2.nextToken());
                ycoord = Float.parseFloat(st2.nextToken());
                corn[polyctr].array[i].x = (xcoord - minx) / xstepsize;
                corn[polyctr].array[i++].y = (ycoord - miny) / ystepsize;
            } else {
                line = FileTools.readLine(inFile);
                i = 0;
                polyctr++;
                if (polyctr < npoly) {
                    polygonid[polyctr] = id = FileTools.readInt(line);
                }
            }
        }
        polyminx = (polyminx - minx) / xstepsize;
        polyminy = (polyminy - miny) / ystepsize;
        polymaxx = (polymaxx - minx) / xstepsize;
        polymaxy = (polymaxy - miny) / ystepsize;
        FileTools.closeFile(inFile);
    }

    private int crnmbr(float x, float y, int ncrns, Point polygon[]) {
        int i, j, c = 0;
        for (i = 0, j = ncrns - 1; i < ncrns; j = i++) {
            if ((((polygon[i].y <= y) && (y < polygon[j].y)) || ((polygon[j].y <= y) && (y < polygon[i].y))) && (x < (polygon[j].x - polygon[i].x) * (y - polygon[i].y) / (polygon[j].y - polygon[i].y) + polygon[i].x)) {
                c = (1 ^ c);
            }
        }
        return c;
    }

    private double polygonarea(int ncrns, Point polygon[]) {
        double area = 0;
        int i;
        for (i = 0; i < ncrns - 1; i++) {
            area += 0.5 * (polygon[i].x + polygon[i + 1].x) * (polygon[i + 1].y - polygon[i].y);
        }
        area += 0.5 * (polygon[ncrns - 1].x + polygon[0].x) * (polygon[0].y - polygon[ncrns - 1].y);
        return Math.abs(area);
    }

    private void digdens() {
        String line;
        double area[], totarea = 0.0, totpop = 0.0;
        BufferedReader inFile = FileTools.openFileRead(dataFileName);
        float avgdens, dens[];
        int cases[], i, id, ii, j, jj, ncases, polyctr;
        line = FileTools.readLine(inFile);
        id = FileTools.readInt(line);
        maxid = id;
        while ((line = FileTools.readLine(inFile)) != null) {
            id = FileTools.readInt(line);
            if (id > maxid) {
                maxid = id;
            }
        }
        FileTools.closeFile(inFile);
        cases = new int[maxid + 1];
        inFile = FileTools.openFileRead(dataFileName);
        while ((line = FileTools.readLine(inFile)) != null) {
            StringTokenizer st = new StringTokenizer(line);
            id = Integer.parseInt(st.nextToken());
            ncases = Integer.parseInt(st.nextToken());
            totpop += (cases[id] = ncases);
        }
        FileTools.closeFile(inFile);
        area = new double[npoly];
        for (polyctr = 0; polyctr < npoly; polyctr++) {
            totarea += polygonarea(ncorn[polyctr], corn[polyctr].array);
            area[polyctr] = 0;
            for (i = 0; i < npoly; i++) {
                if (polygonid[i] == polygonid[polyctr]) {
                    area[polyctr] += polygonarea(ncorn[i], corn[i].array);
                }
            }
        }
        dens = new float[npoly];
        for (polyctr = 0; polyctr < npoly; polyctr++) {
            dens[polyctr] = (float) (cases[polygonid[polyctr]] / area[polyctr]);
        }
        avgdens = (float) (totpop / totarea);
        for (i = 0; i <= lx; i++) {
            for (j = 0; j <= ly; j++) {
                rho_0[i].array[j] = 0;
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("digitizing density ...\n");
        }
        for (i = 0; i < lx; i++) {
            if (logger.isLoggable(Level.FINEST)) {
                if (i % 100 == 1 && i != 1) {
                    logger.finest("finished " + (i - 1) + " of " + lx + "\n");
                }
            }
            for (j = 0; j < ly; j++) {
                for (ii = 0; ii < NSUBDIV; ii++) {
                    for (jj = 0; jj < NSUBDIV; jj++) {
                        if (i - 0.5 + (float) (ii + 1) / NSUBDIV < polyminx || i - 0.5 + (float) ii / NSUBDIV > polymaxx || j - 0.5 + (float) (jj + 1) / NSUBDIV < polyminy || j - 0.5 + (float) jj / NSUBDIV > polymaxy) {
                            rho_0[i].array[j] += avgdens / (NSUBDIV * NSUBDIV);
                            continue;
                        }
                        for (polyctr = 0; polyctr < npoly; polyctr++) {
                            if (crnmbr((float) (i - 0.5 + (float) (2 * ii + 1) / (2 * NSUBDIV)), (float) (j - 0.5 + (float) (2 * ii + 1) / (2 * NSUBDIV)), ncorn[polyctr], corn[polyctr].array) != 0) {
                                rho_0[i].array[j] += dens[polyctr] / (NSUBDIV * NSUBDIV);
                                break;
                            }
                        }
                        if (polyctr == npoly) {
                            rho_0[i].array[j] += avgdens / (NSUBDIV * NSUBDIV);
                        }
                    }
                }
            }
        }
        rho_0[0].array[0] += rho_0[0].array[ly] + rho_0[lx].array[0] + rho_0[lx].array[ly];
        for (i = 1; i < lx; i++) {
            rho_0[i].array[0] += rho_0[i].array[ly];
        }
        for (j = 1; j < ly; j++) {
            rho_0[0].array[j] += rho_0[lx].array[j];
        }
        for (i = 0; i < lx; i++) {
            rho_0[i].array[ly] = rho_0[i].array[0];
        }
        for (j = 0; j <= ly; j++) {
            rho_0[lx].array[j] = rho_0[0].array[j];
        }
        coscosft(rho_0, 1, 1);
        area = null;
        cases = null;
        for (i = 0; i < npoly; i++) {
            corn[i].array = null;
            corn[i] = null;
        }
        corn = null;
        dens = null;
        ncorn = null;
        polygonid = null;
    }

    private void four1(float data[], long nn, int isign) {
        double theta, wi, wpi, wpr, wr, wtemp;
        float tempi, tempr;
        long i, istep, j, m, mmax, n;
        float tmpf;
        n = nn << 1;
        j = 1;
        for (i = 1; i < n; i += 2) {
            if (j > i) {
                tmpf = data[(int) j];
                data[(int) j] = data[(int) i];
                data[(int) i] = tmpf;
                tmpf = data[(int) j + 1];
                data[(int) j + 1] = data[(int) i + 1];
                data[(int) i + 1] = tmpf;
            }
            m = n >> 1;
            while (m >= 2 && j > m) {
                j -= m;
                m >>= 1;
            }
            j += m;
        }
        mmax = 2;
        while (n > mmax) {
            istep = mmax << 1;
            theta = isign * (6.28318530717959 / mmax);
            wtemp = Math.sin(0.5 * theta);
            wpr = -2.0 * wtemp * wtemp;
            wpi = Math.sin(theta);
            wr = 1.0;
            wi = 0.0;
            for (m = 1; m < mmax; m += 2) {
                for (i = m; i <= n; i += istep) {
                    j = i + mmax;
                    tempr = (float) (wr * data[(int) j] - wi * data[(int) j + 1]);
                    tempi = (float) (wr * data[(int) j + 1] + wi * data[(int) j]);
                    data[(int) j] = data[(int) i] - tempr;
                    data[(int) j + 1] = data[(int) i + 1] - tempi;
                    data[(int) i] += tempr;
                    data[(int) i + 1] += tempi;
                }
                wr = (wtemp = wr) * wpr - wi * wpi + wr;
                wi = wi * wpr + wtemp * wpi + wi;
            }
            mmax = istep;
        }
    }

    private void realft(float data[], long n, int isign) {
        double theta, wi, wpi, wpr, wr, wtemp;
        float c1 = (float) 0.5, c2, h1i, h1r, h2i, h2r;
        long i, i1, i2, i3, i4, np3;
        theta = 3.141592653589793 / (n >> 1);
        if (isign == 1) {
            c2 = (float) -0.5;
            four1(data, n >> 1, 1);
        } else {
            c2 = (float) 0.5;
            theta = -theta;
        }
        wtemp = Math.sin(0.5 * theta);
        wpr = -2.0 * wtemp * wtemp;
        wpi = Math.sin(theta);
        wr = 1.0 + wpr;
        wi = wpi;
        np3 = n + 3;
        for (i = 2; i <= (n >> 2); i++) {
            i4 = 1 + (i3 = np3 - (i2 = 1 + (i1 = i + i - 1)));
            h1r = c1 * (data[(int) i1] + data[(int) i3]);
            h1i = c1 * (data[(int) i2] - data[(int) i4]);
            h2r = -c2 * (data[(int) i2] + data[(int) i4]);
            h2i = c2 * (data[(int) i1] - data[(int) i3]);
            data[(int) i1] = (float) (h1r + wr * h2r - wi * h2i);
            data[(int) i2] = (float) (h1i + wr * h2i + wi * h2r);
            data[(int) i3] = (float) (h1r - wr * h2r + wi * h2i);
            data[(int) i4] = (float) (-h1i + wr * h2i + wi * h2r);
            wr = (wtemp = wr) * wpr - wi * wpi + wr;
            wi = wi * wpr + wtemp * wpi + wi;
        }
        if (isign == 1) {
            data[1] = (h1r = data[1]) + data[2];
            data[2] = h1r - data[2];
        } else {
            data[1] = c1 * ((h1r = data[1]) + data[2]);
            data[2] = c1 * (h1r - data[2]);
            four1(data, n >> 1, -1);
        }
    }

    private void cosft(float z[], int n, int isign) {
        double theta, wi = 0.0, wpi, wpr, wr = 1.0, wtemp;
        float a[], sum, y1, y2;
        int j, n2;
        a = new float[n + 2];
        for (j = 1; j <= n + 1; j++) {
            a[j] = z[j - 1];
        }
        theta = PI / n;
        wtemp = Math.sin(0.5 * theta);
        wpr = -2.0 * wtemp * wtemp;
        wpi = Math.sin(theta);
        sum = (float) (0.5 * (a[1] - a[n + 1]));
        a[1] = (float) (0.5 * (a[1] + a[n + 1]));
        n2 = n + 2;
        for (j = 2; j <= (n >> 1); j++) {
            wr = (wtemp = wr) * wpr - wi * wpi + wr;
            wi = wi * wpr + wtemp * wpi + wi;
            y1 = (float) (0.5 * (a[j] + a[n2 - j]));
            y2 = (a[j] - a[n2 - j]);
            a[j] = (float) (y1 - wi * y2);
            a[n2 - j] = (float) (y1 + wi * y2);
            sum += wr * y2;
        }
        realft(a, n, 1);
        a[n + 1] = a[2];
        a[2] = sum;
        for (j = 4; j <= n; j += 2) {
            sum += a[j];
            a[j] = sum;
        }
        if (isign == 1) {
            for (j = 1; j <= n + 1; j++) {
                z[j - 1] = a[j];
            }
        } else if (isign == -1) {
            for (j = 1; j <= n + 1; j++) {
                z[j - 1] = (float) (2.0 * a[j] / n);
            }
        }
        a = null;
    }

    private void sinft(float z[], int n, int isign) {
        double theta, wi = 0.0, wpi, wpr, wr = 1.0, wtemp;
        float a[], sum, y1, y2;
        int j;
        int n2 = n + 2;
        a = new float[n + 1];
        for (j = 1; j <= n; j++) {
            a[j] = z[j - 1];
        }
        theta = PI / n;
        wtemp = Math.sin(0.5 * theta);
        wpr = -2.0 * wtemp * wtemp;
        wpi = Math.sin(theta);
        a[1] = 0.0f;
        for (j = 2; j <= (n >> 1) + 1; j++) {
            wr = (wtemp = wr) * wpr - wi * wpi + wr;
            wi = wi * wpr + wtemp * wpi + wi;
            y1 = (float) (wi * (a[j] + a[n2 - j]));
            y2 = (float) (0.5 * (a[j] - a[n2 - j]));
            a[j] = y1 + y2;
            a[n2 - j] = y1 - y2;
        }
        realft(a, n, 1);
        a[1] *= 0.5;
        sum = a[2] = 0.0f;
        for (j = 1; j <= n - 1; j += 2) {
            sum += a[j];
            a[j] = a[j + 1];
            a[j + 1] = sum;
        }
        if (isign == 1) {
            for (j = 1; j <= n; j++) {
                z[j - 1] = a[j];
            }
        } else if (isign == -1) {
            for (j = 1; j <= n; j++) {
                z[j - 1] = (float) 2.0 * a[j] / n;
            }
        }
        z[n] = 0.0f;
        a = null;
    }

    private void coscosft(ArrayFloat y[], int isign1, int isign2) {
        float temp[] = new float[lx + 1];
        int i, j;
        for (i = 0; i <= lx; i++) {
            cosft(y[i].array, ly, isign2);
        }
        for (j = 0; j <= ly; j++) {
            for (i = 0; i <= lx; i++) {
                temp[i] = y[i].array[j];
            }
            cosft(temp, lx, isign1);
            for (i = 0; i <= lx; i++) {
                y[i].array[j] = temp[i];
            }
        }
    }

    private void cossinft(ArrayFloat y[], int isign1, int isign2) {
        float temp[] = new float[lx + 1];
        int i, j;
        for (i = 0; i <= lx; i++) {
            sinft(y[i].array, ly, isign2);
        }
        for (j = 0; j <= ly; j++) {
            for (i = 0; i <= lx; i++) {
                temp[i] = y[i].array[j];
            }
            cosft(temp, lx, isign1);
            for (i = 0; i <= lx; i++) {
                y[i].array[j] = temp[i];
            }
        }
    }

    private void sincosft(ArrayFloat y[], int isign1, int isign2) {
        float temp[] = new float[lx + 1];
        int i, j;
        for (i = 0; i <= lx; i++) {
            cosft(y[i].array, ly, isign2);
        }
        for (j = 0; j <= ly; j++) {
            for (i = 0; i <= lx; i++) {
                temp[i] = y[i].array[j];
            }
            sinft(temp, lx, isign1);
            for (i = 0; i <= lx; i++) {
                y[i].array[j] = temp[i];
            }
        }
    }

    private void fourn(D3Tensor data, int[] nn, int ndim, int isign) {
        int idim;
        long i1, i2, i3, i2rev, i3rev, ip1, ip2, ip3, ifp1, ifp2;
        long ibit, k1, k2, n, nprev, nrem, ntot;
        double tempi, tempr;
        float theta, wi, wpi, wpr, wr, wtemp;
        for (ntot = 1, idim = 1; idim <= ndim; idim++) {
            ntot *= nn[idim];
        }
        nprev = 1;
        for (idim = ndim; idim >= 1; idim--) {
            n = nn[idim];
            nrem = ntot / (n * nprev);
            ip1 = nprev << 1;
            ip2 = ip1 * n;
            ip3 = ip2 * nrem;
            i2rev = 1;
            for (i2 = 1; i2 <= ip2; i2 += ip1) {
                if (i2 < i2rev) {
                    for (i1 = i2; i1 <= i2 + ip1 - 2; i1 += 2) {
                        for (i3 = i1; i3 <= ip3; i3 += ip2) {
                            i3rev = i2rev + i3 - i2;
                            data.swapElements((int) i3, (int) i3rev);
                            data.swapElements((int) i3 + 1, (int) i3rev + 1);
                        }
                    }
                }
                ibit = ip2 >> 1;
                while (ibit >= ip1 && i2rev > ibit) {
                    i2rev -= ibit;
                    ibit >>= 1;
                }
                i2rev += ibit;
            }
            ifp1 = ip1;
            while (ifp1 < ip2) {
                ifp2 = ifp1 << 1;
                theta = (float) (2 * isign * PI / (ifp2 / ip1));
                wtemp = (float) Math.sin(0.5 * theta);
                wpr = (float) (-2.0 * wtemp * wtemp);
                wpi = (float) Math.sin(theta);
                wr = 1.0f;
                wi = 0.0f;
                for (i3 = 1; i3 <= ifp1; i3 += ip1) {
                    for (i1 = i3; i1 <= i3 + ip1 - 2; i1 += 2) {
                        for (i2 = i1; i2 <= ip3; i2 += ifp2) {
                            k1 = i2;
                            k2 = k1 + ifp1;
                            tempr = wr * data.getElement((int) k2) - wi * data.getElement((int) k2 + 1);
                            tempi = wr * data.getElement((int) k2 + 1) + wi * data.getElement((int) k2);
                            data.setElement((int) k2, (float) (data.getElement((int) k1) - tempr));
                            data.setElement((int) k2 + 1, (float) (data.getElement((int) k1 + 1) - tempi));
                            data.addToElement((int) k1, (float) tempr);
                            data.addToElement((int) k1 + 1, (float) tempi);
                        }
                    }
                    wr = (wtemp = wr) * wpr - wi * wpi + wr;
                    wi = wi * wpr + wtemp * wpi + wi;
                }
                ifp1 = ifp2;
            }
            nprev *= n;
        }
    }

    private void rlft3(D3Tensor data, DMatrix speq, int nn1, int nn2, int nn3, int isign) {
        double theta, wi, wpi, wpr, wr, wtemp;
        float c1, c2, h1r, h1i, h2r, h2i;
        int i1, i2, i3, j1, j2, j3, ii3;
        int[] nn = new int[4];
        if (data.getElementsCount() != nn1 * nn2 * nn3) {
            System.err.println("rlft3: problem with dimensions or contiguity of data array\n");
            System.exit(1);
        }
        c1 = 0.5f;
        c2 = -0.5f * isign;
        theta = 2 * isign * (PI / nn3);
        wtemp = (float) Math.sin(0.5 * theta);
        wpr = -2.0 * wtemp * wtemp;
        wpi = (float) Math.sin(theta);
        nn[1] = nn1;
        nn[2] = nn2;
        nn[3] = nn3 >> 1;
        if (isign == 1) {
            data.setOffset(0, 0, -1);
            fourn(data, nn, 3, isign);
            data.setOffset(0, 0, +1);
            for (i1 = 1; i1 <= nn1; i1++) {
                for (i2 = 1, j2 = 0; i2 <= nn2; i2++) {
                    speq.setElement(i1, ++j2, data.getElement(i1, i2, 1));
                    speq.setElement(i1, ++j2, data.getElement(i1, i2, 2));
                }
            }
        }
        for (i1 = 1; i1 <= nn1; i1++) {
            j1 = (i1 != 1 ? nn1 - i1 + 2 : 1);
            wr = 1.0;
            wi = 0.0;
            for (ii3 = 1, i3 = 1; i3 <= (nn3 >> 2) + 1; i3++, ii3 += 2) {
                for (i2 = 1; i2 <= nn2; i2++) {
                    if (i3 == 1) {
                        j2 = (i2 != 1 ? ((nn2 - i2) << 1) + 3 : 1);
                        h1r = c1 * (data.getElement(i1, i2, 1) + speq.getElement(j1, j2));
                        h1i = c1 * (data.getElement(i1, i2, 2) - speq.getElement(j1, j2 + 1));
                        h2i = c2 * (data.getElement(i1, i2, 1) - speq.getElement(j1, j2));
                        h2r = -c2 * (data.getElement(i1, i2, 2) + speq.getElement(j1, j2 + 1));
                        data.setElement(i1, i2, 1, h1r + h2r);
                        data.setElement(i1, i2, 2, h1i + h2i);
                        speq.setElement(j1, j2, h1r - h2r);
                        speq.setElement(j1, j2 + 1, h2i - h1i);
                    } else {
                        j2 = (i2 != 1 ? nn2 - i2 + 2 : 1);
                        j3 = nn3 + 3 - (i3 << 1);
                        h1r = c1 * (data.getElement(i1, i2, ii3) + data.getElement(j1, j2, j3));
                        h1i = c1 * (data.getElement(i1, i2, ii3 + 1) - data.getElement(j1, j2, j3 + 1));
                        h2i = c2 * (data.getElement(i1, i2, ii3) - data.getElement(j1, j2, j3));
                        h2r = -c2 * (data.getElement(i1, i2, ii3 + 1) + data.getElement(j1, j2, j3 + 1));
                        data.setElement(i1, i2, ii3, (float) (h1r + wr * h2r - wi * h2i));
                        data.setElement(i1, i2, ii3 + 1, (float) (h1i + wr * h2i + wi * h2r));
                        data.setElement(j1, j2, j3, (float) (h1r - wr * h2r + wi * h2i));
                        data.setElement(j1, j2, j3 + 1, (float) (-h1i + wr * h2i + wi * h2r));
                    }
                }
                wr = (wtemp = wr) * wpr - wi * wpi + wr;
                wi = wi * wpr + wtemp * wpi + wi;
            }
        }
        if (isign == -1) {
            data.setOffset(0, 0, -1);
            fourn(data, nn, 3, isign);
            data.setOffset(0, 0, +1);
        }
    }

    private void gaussianblur() {
        D3Tensor blur, conv, pop;
        DMatrix speqblur, speqconv, speqpop;
        int i, j, p, q;
        blur = new D3Tensor(1, 1, 1, lx, 1, ly);
        conv = new D3Tensor(1, 1, 1, lx, 1, ly);
        pop = new D3Tensor(1, 1, 1, lx, 1, ly);
        speqblur = new DMatrix(1, 1, 1, 2 * lx);
        speqconv = new DMatrix(1, 1, 1, 2 * lx);
        speqpop = new DMatrix(1, 1, 1, 2 * lx);
        for (i = 1; i <= lx; i++) {
            for (j = 1; j <= ly; j++) {
                if (i > lx / 2) {
                    p = i - 1 - lx;
                } else {
                    p = i - 1;
                }
                if (j > ly / 2) {
                    q = j - 1 - ly;
                } else {
                    q = j - 1;
                }
                pop.setElement(1, i, j, rho_0[i - 1].array[j - 1]);
                conv.setElement(1, i, j, (float) (0.5 * (erf((p + 0.5) / (Math.sqrt(2.0) * (blurWidth * Math.pow(blurWidthFactor, nblurs)))) - erf((p - 0.5) / (Math.sqrt(2.0) * (blurWidth * Math.pow(blurWidthFactor, nblurs))))) * (erf((q + 0.5) / (Math.sqrt(2.0) * (blurWidth * Math.pow(blurWidthFactor, nblurs)))) - erf((q - 0.5) / (Math.sqrt(2.0) * (blurWidth * Math.pow(blurWidthFactor, nblurs))))) / (lx * ly)));
            }
        }
        rlft3(pop, speqpop, 1, lx, ly, 1);
        rlft3(conv, speqconv, 1, lx, ly, 1);
        for (i = 1; i <= lx; i++) {
            for (j = 1; j <= ly / 2; j++) {
                blur.setElement(1, i, 2 * j - 1, pop.getElement(1, i, 2 * j - 1) * conv.getElement(1, i, 2 * j - 1) - pop.getElement(1, i, 2 * j) * conv.getElement(1, i, 2 * j));
                blur.setElement(1, i, 2 * j, pop.getElement(1, i, 2 * j) * conv.getElement(1, i, 2 * j - 1) + pop.getElement(1, i, 2 * j - 1) * conv.getElement(1, i, 2 * j));
            }
        }
        for (i = 1; i <= lx; i++) {
            speqblur.setElement(1, 2 * i - 1, speqpop.getElement(1, 2 * i - 1) * speqconv.getElement(1, 2 * i - 1) - speqpop.getElement(1, 2 * i) * speqconv.getElement(1, 2 * i));
            speqblur.setElement(1, 2 * i, speqpop.getElement(1, 2 * i) * speqconv.getElement(1, 2 * i - 1) + speqpop.getElement(1, 2 * i - 1) * speqconv.getElement(1, 2 * i));
        }
        rlft3(blur, speqblur, 1, lx, ly, -1);
        for (i = 1; i <= lx; i++) {
            for (j = 1; j <= ly; j++) {
                rho_0[i - 1].array[j - 1] = blur.getElement(1, i, j);
            }
        }
    }

    private void initcond() {
        float maxpop;
        int i, j;
        coscosft(rho_0, -1, -1);
        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly; j++) {
                if (rho_0[i].array[j] < -1e10) {
                    System.err.println("ERROR: Negative density in DENSITYFILE.");
                    System.exit(1);
                }
            }
        }
        logger.finest("Gaussian blur ...\n");
        gaussianblur();
        minpop = rho_0[0].array[0];
        maxpop = rho_0[0].array[0];
        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly; j++) {
                if (rho_0[i].array[j] < minpop) {
                    minpop = rho_0[i].array[j];
                }
            }
        }
        for (i = 0; i < lx; i++) {
            for (j = 0; j < ly; j++) {
                if (rho_0[i].array[j] > maxpop) {
                    maxpop = rho_0[i].array[j];
                }
            }
        }
        if (0 < minpop && minpop < 1e-8 * maxpop) {
            System.err.println("Minimimum population very small (" + minpop + "). Integrator");
            System.err.println("will probably converge very slowly. You can speed up the");
            System.err.println("process by increasing SIGMA to a value > " + blurWidth * Math.pow(blurWidthFactor, nblurs));
        }
        coscosft(rho_0, 1, 1);
    }

    private void calcv(float t) {
        int j, k;
        for (j = 0; j <= lx; j++) {
            for (k = 0; k <= ly; k++) {
                rho[j].array[k] = (float) Math.exp(-((PI * j / lx) * (PI * j / lx) + (PI * k / ly) * (PI * k / ly)) * t) * rho_0[j].array[k];
            }
        }
        for (j = 0; j <= lx; j++) {
            for (k = 0; k <= ly; k++) {
                gridvx[j].array[k] = (float) -(PI * j / lx) * rho[j].array[k];
                gridvy[j].array[k] = (float) -(PI * k / ly) * rho[j].array[k];
            }
        }
        coscosft(rho, -1, -1);
        sincosft(gridvx, -1, -1);
        cossinft(gridvy, -1, -1);
        for (j = 0; j <= lx; j++) {
            for (k = 0; k <= ly; k++) {
                gridvx[j].array[k] = -gridvx[j].array[k] / rho[j].array[k];
                gridvy[j].array[k] = -gridvy[j].array[k] / rho[j].array[k];
            }
        }
    }

    private float intpol(ArrayFloat[] arr, float x, float y) {
        int gaussx, gaussy;
        float deltax, deltay;
        gaussx = (int) x;
        gaussy = (int) y;
        deltax = x - gaussx;
        deltay = y - gaussy;
        if (gaussx == lx && gaussy == ly) {
            return arr[gaussx].array[gaussy];
        }
        if (gaussx == lx) {
            return (1 - deltay) * arr[gaussx].array[gaussy] + deltay * arr[gaussx].array[gaussy + 1];
        }
        if (gaussy == ly) {
            return (1 - deltax) * arr[gaussx].array[gaussy] + deltax * arr[gaussx + 1].array[gaussy];
        }
        return (1 - deltax) * (1 - deltay) * arr[gaussx].array[gaussy] + (1 - deltax) * deltay * arr[gaussx].array[gaussy + 1] + deltax * (1 - deltay) * arr[gaussx + 1].array[gaussy] + deltax * deltay * arr[gaussx + 1].array[gaussy + 1];
    }

    private boolean newt2(float h, float[] retxyAppr, float xguess, float yguess, int j, int k) {
        float deltax, deltay, dfxdx, dfxdy, dfydx, dfydy, fx, fy;
        int gaussx, gaussxplus, gaussy, gaussyplus, i;
        float xappr, yappr;
        xappr = xguess;
        yappr = yguess;
        for (i = 1; i <= IMAX; i++) {
            fx = (float) (xappr - 0.5 * h * intpol(gridvx, xappr, yappr) - x[j].array[k] - 0.5 * h * vx[j].array[k]);
            fy = (float) (yappr - 0.5 * h * intpol(gridvy, xappr, yappr) - y[j].array[k] - 0.5 * h * vy[j].array[k]);
            gaussx = (int) (xappr);
            gaussy = (int) (yappr);
            if (gaussx == lx) {
                gaussxplus = 0;
            } else {
                gaussxplus = gaussx + 1;
            }
            if (gaussy == ly) {
                gaussyplus = 0;
            } else {
                gaussyplus = gaussy + 1;
            }
            deltax = x[j].array[k] - gaussx;
            deltay = y[j].array[k] - gaussy;
            dfxdx = (float) (1 - 0.5 * h * ((1 - deltay) * (gridvx[gaussxplus].array[gaussy] - gridvx[gaussx].array[gaussy]) + deltay * (gridvx[gaussxplus].array[gaussyplus] - gridvx[gaussx].array[gaussyplus])));
            dfxdy = (float) (-0.5 * h * ((1 - deltax) * (gridvx[gaussx].array[gaussyplus] - gridvx[gaussx].array[gaussy]) + deltax * (gridvx[gaussxplus].array[gaussyplus] - gridvx[gaussxplus].array[gaussy])));
            dfydx = (float) (-0.5 * h * ((1 - deltay) * (gridvy[gaussxplus].array[gaussy] - gridvy[gaussx].array[gaussy]) + deltay * (gridvy[gaussxplus].array[gaussyplus] - gridvy[gaussx].array[gaussyplus])));
            dfydy = (float) (1 - 0.5 * h * ((1 - deltax) * (gridvy[gaussx].array[gaussyplus] - gridvy[gaussx].array[gaussy]) + deltax * (gridvy[gaussxplus].array[gaussyplus] - gridvy[gaussxplus].array[gaussy])));
            if ((fx * fx + fy * fy) < TOLF) {
                retxyAppr[0] = xappr;
                retxyAppr[1] = yappr;
                return true;
            }
            deltax = (fy * dfxdy - fx * dfydy) / (dfxdx * dfydy - dfxdy * dfydx);
            deltay = (fx * dfydx - fy * dfxdx) / (dfxdx * dfydy - dfxdy * dfydx);
            if ((deltax * deltax + deltay * deltay) < TOLX) {
                retxyAppr[0] = xappr;
                retxyAppr[1] = yappr;
                return true;
            }
            xappr += deltax;
            yappr += deltay;
        }
        System.err.println("newt2 failed, increasing sigma to " + blurWidth * Math.pow(blurWidthFactor, nblurs));
        retxyAppr[0] = xappr;
        retxyAppr[1] = yappr;
        return false;
    }

    private boolean nonlinvoltra() {
        boolean stepsize_ok;
        BufferedWriter displfile = null;
        if (DISPLFILE != null) {
            try {
                displfile = new BufferedWriter(new FileWriter(DISPLFILE));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        float h, maxchange = 0f, t, vxplus, vyplus, xguess, yguess;
        int i, j, k;
        do {
            initcond();
            nblurs++;
            if (minpop < 0.0) {
                logger.finest("Minimum population negative, will increase sigma to " + blurWidth * Math.pow(blurWidthFactor, nblurs));
            }
        } while (minpop < 0.0);
        h = (float) HINITIAL;
        t = 0;
        for (j = 0; j <= lx; j++) {
            for (k = 0; k <= ly; k++) {
                x[j].array[k] = j;
                y[j].array[k] = k;
            }
        }
        calcv(0.0f);
        for (j = 0; j <= lx; j++) {
            for (k = 0; k <= ly; k++) {
                vx[j].array[k] = gridvx[j].array[k];
                vy[j].array[k] = gridvy[j].array[k];
            }
        }
        i = 1;
        do {
            stepsize_ok = true;
            calcv(t + h);
            for (j = 0; j <= lx; j++) {
                for (k = 0; k <= ly; k++) {
                    vxplus = intpol(gridvx, x[j].array[k] + h * vx[j].array[k], y[j].array[k] + h * vy[j].array[k]);
                    vyplus = intpol(gridvy, x[j].array[k] + h * vx[j].array[k], y[j].array[k] + h * vy[j].array[k]);
                    xguess = (float) (x[j].array[k] + 0.5 * h * (vx[j].array[k] + vxplus));
                    yguess = (float) (y[j].array[k] + 0.5 * h * (vy[j].array[k] + vyplus));
                    float[] ret = new float[] { xappr[j].array[k], yappr[j].array[k] };
                    boolean result = newt2(h, ret, xguess, yguess, j, k);
                    xappr[j].array[k] = ret[0];
                    yappr[j].array[k] = ret[1];
                    if (!result) {
                        return false;
                    }
                    if ((xguess - xappr[j].array[k]) * (xguess - xappr[j].array[k]) + (yguess - yappr[j].array[k]) * (yguess - yappr[j].array[k]) > TOLINT) {
                        if (h < MINH) {
                            logger.finest("Time step below " + h + ", increasing SIGMA to " + blurWidth * Math.pow(blurWidthFactor, nblurs));
                            nblurs++;
                            return false;
                        }
                        h /= 10;
                        stepsize_ok = false;
                        break;
                    }
                }
            }
            if (!stepsize_ok) {
                continue;
            }
            t += h;
            maxchange = 0.0f;
            for (j = 0; j <= lx; j++) {
                for (k = 0; k <= ly; k++) {
                    if ((x[j].array[k] - xappr[j].array[k]) * (x[j].array[k] - xappr[j].array[k]) + (y[j].array[k] - yappr[j].array[k]) * (y[j].array[k] - yappr[j].array[k]) > maxchange) {
                        maxchange = (x[j].array[k] - xappr[j].array[k]) * (x[j].array[k] - xappr[j].array[k]) + (y[j].array[k] - yappr[j].array[k]) * (y[j].array[k] - yappr[j].array[k]);
                    }
                    x[j].array[k] = xappr[j].array[k];
                    y[j].array[k] = yappr[j].array[k];
                    vx[j].array[k] = intpol(gridvx, xappr[j].array[k], yappr[j].array[k]);
                    vy[j].array[k] = intpol(gridvy, xappr[j].array[k], yappr[j].array[k]);
                }
            }
            h *= 1.2;
            if (logger.isLoggable(Level.FINEST)) {
                if (i % 10 == 0) {
                    logger.finest("time " + t);
                }
            }
            i++;
        } while (i < MAXINTSTEPS && t < TIMELIMIT && maxchange > CONVERGENCE);
        if (maxchange > CONVERGENCE) {
            System.err.println("WARNING: Insufficient convergence within " + MAXINTSTEPS + " steps, time " + TIMELIMIT);
        }
        if (DISPLFILE != null) {
            try {
                displfile.write("time " + t + "\nminx " + minx + "\nmaxx " + maxx + "\nminy " + miny + "\nmaxy " + maxy + "\n");
                displfile.write("sigma " + blurWidth * Math.pow(blurWidthFactor, nblurs - 1) + "\n");
                displfile.write("background 0\nlx\nly\n\n");
                for (j = 0; j <= lx; j++) {
                    for (k = 0; k <= ly; k++) {
                        displfile.write("j " + j + ", k " + k + ", x " + x[j].array[k] + ", y " + y[j].array[k] + "\n");
                    }
                }
                displfile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    Point transf(final Point p) {
        float deltax, deltay, den, t, u;
        int gaussx, gaussy;
        Point a = new Point(), b = new Point(), c = new Point(), d = new Point(), ptr = new Point();
        p.x = (p.x - minx) * lx / (maxx - minx);
        p.y = (p.y - miny) * ly / (maxy - miny);
        gaussx = (int) p.x;
        gaussy = (int) p.y;
        if (gaussx < 0 || gaussx > lx || gaussy < 0 || gaussy > ly) {
            System.err.println("ERROR: Coordinate limits exceeded in transf.\n");
            System.exit(1);
        }
        deltax = p.x - gaussx;
        deltay = p.y - gaussy;
        a.x = (1 - deltax) * x[gaussx].array[gaussy] + deltax * x[gaussx + 1].array[gaussy];
        a.y = (1 - deltax) * y[gaussx].array[gaussy] + deltax * y[gaussx + 1].array[gaussy];
        b.x = (1 - deltax) * x[gaussx].array[gaussy + 1] + deltax * x[gaussx + 1].array[gaussy + 1];
        b.y = (1 - deltax) * y[gaussx].array[gaussy + 1] + deltax * y[gaussx + 1].array[gaussy + 1];
        c.x = (1 - deltay) * x[gaussx].array[gaussy] + deltay * x[gaussx].array[gaussy + 1];
        c.y = (1 - deltay) * y[gaussx].array[gaussy] + deltay * y[gaussx].array[gaussy + 1];
        d.x = (1 - deltay) * x[gaussx + 1].array[gaussy] + deltay * x[gaussx + 1].array[gaussy + 1];
        d.y = (1 - deltay) * y[gaussx + 1].array[gaussy] + deltay * y[gaussx + 1].array[gaussy + 1];
        if (Math.abs(den = (b.x - a.x) * (c.y - d.y) + (a.y - b.y) * (c.x - d.x)) < 1e-12) {
            System.err.println("ERROR: Transformed area element has parallel edges.\n");
        }
        t = ((c.x - a.x) * (c.y - d.y) + (a.y - c.y) * (c.x - d.x)) / den;
        u = ((b.x - a.x) * (c.y - a.y) + (a.y - b.y) * (c.x - a.x)) / den;
        if (t < -1e-3 || t > 1 + 1e-3 || u < -1e-3 || u > 1 + 1e-3) {
            System.err.println("WARNING: Transformed area element non-convex.\n");
        }
        ptr.x = (1 - (a.x + t * (b.x - a.x)) / lx) * minx + ((a.x + t * (b.x - a.x)) / lx) * maxx;
        ptr.y = (1 - (a.y + t * (b.y - a.y)) / ly) * miny + ((a.y + t * (b.y - a.y)) / ly) * maxy;
        return ptr;
    }

    private void cartogram() throws IOException {
        String id, line;
        BufferedReader infile;
        BufferedWriter outfile;
        float xcoord, ycoord;
        Point p = new Point();
        infile = FileTools.openFileRead(polygonFileName);
        outfile = FileTools.openFileWrite(genFileName);
        while ((line = FileTools.readLine(infile)) != null) {
            StringTokenizer s = new StringTokenizer(line);
            String a = s.nextToken();
            String b = null, c = null;
            if (s.hasMoreTokens()) {
                b = s.nextToken();
            }
            if (s.hasMoreTokens()) {
                c = s.nextToken();
            }
            boolean flag = false;
            if (b != null && c != null) {
                try {
                    id = a;
                    xcoord = Float.parseFloat(b);
                    ycoord = Float.parseFloat(c);
                    p.x = xcoord;
                    p.y = ycoord;
                    p = transf(p);
                    outfile.write(id + " " + p.x + " " + p.y + "\n");
                    flag = true;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            if (!flag && b != null && a != null) {
                try {
                    xcoord = Float.parseFloat(a);
                    ycoord = Float.parseFloat(b);
                    p.x = xcoord;
                    p.y = ycoord;
                    p = transf(p);
                    outfile.write(p.x + " " + p.y + "\n");
                    flag = true;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                id = a;
                outfile.write(id + "\n");
            }
        }
        infile.close();
        outfile.close();
    }

    private void pspicture(BufferedReader infile, BufferedWriter outfile) throws IOException {
        String line;
        float addx, addy, b, conv, g, r, xcoord, ycoord;
        int id;
        if (11 * lx > 8.5 * ly) {
            conv = (float) 8.5 * 72 / lx;
            addx = 0;
            addy = (float) (11 * 36 - 8.5 * 36 * ly / lx);
        } else {
            conv = (float) 11 * 72 / ly;
            addx = (float) (8.5 * 36 - 11 * 36 * lx / ly);
            addy = 0;
        }
        line = FileTools.readLine(infile);
        StringTokenizer s = new StringTokenizer(line);
        id = Integer.parseInt(s.nextToken());
        line = FileTools.readLine(infile);
        s = new StringTokenizer(line);
        xcoord = Float.parseFloat(s.nextToken());
        ycoord = Float.parseFloat(s.nextToken());
        outfile.write("newpath\n" + ((xcoord - minx) * conv / xstepsize + addx) + " " + ((ycoord - miny) * conv / ystepsize + addy) + " moveto\n");
        while ((line = FileTools.readLine(infile)) != null) {
            if (line.charAt(0) != 'E') {
                s = new StringTokenizer(line);
                xcoord = Float.parseFloat(s.nextToken());
                ycoord = Float.parseFloat(s.nextToken());
                outfile.write(((xcoord - minx) * conv / xstepsize + addx) + " " + ((ycoord - miny) * conv / ystepsize + addy) + " lineto\n");
            } else {
                if (id % 3 == 0) {
                    r = (float) id / maxid;
                    g = 1 - (float) id / maxid;
                    b = Math.abs(1 - 2 * (float) id / maxid);
                } else if (id % 3 == 1) {
                    b = (float) id / maxid;
                    r = 1 - (float) id / maxid;
                    g = Math.abs(1 - 2 * (float) id / maxid);
                } else {
                    g = (float) id / maxid;
                    b = 1 - (float) id / maxid;
                    r = Math.abs(1 - 2 * (float) id / maxid);
                }
                outfile.write("closepath\n" + r + " " + g + " " + b + " setrgbcolor\ngsave\nfill\ngrestore\n0 setgray stroke\n");
                line = FileTools.readLine(infile);
                if (line.charAt(0) == 'E') {
                    break;
                }
                s = new StringTokenizer(line);
                id = Integer.parseInt(s.nextToken());
                line = FileTools.readLine(infile);
                s = new StringTokenizer(line);
                xcoord = Float.parseFloat(s.nextToken());
                ycoord = Float.parseFloat(s.nextToken());
                outfile.write("newpath\n" + ((xcoord - minx) * conv / xstepsize + addx) + " " + ((ycoord - miny) * conv / ystepsize + addy) + " moveto\n");
            }
        }
        outfile.write("showpage\n");
    }

    public TransformsMain(boolean transformNow) {
        if (transformNow) {
            try {
                makeCartogram();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public TransformsMain() {
    }

    public TransformsMain(String genFile, String datFile, String polygonFile) throws IOException {
        polygonFileName = polygonFile;
        genFileName = genFile;
        dataFileName = datFile;
        makeCartogram();
    }

    public void makeCartogram() throws IOException {
        boolean n;
        BufferedReader infile;
        BufferedWriter outfile;
        int i;
        readcorn();
        gridvx = new ArrayFloat[lx + 1];
        gridvx = new ArrayFloat[lx + 1];
        gridvy = new ArrayFloat[lx + 1];
        rho = new ArrayFloat[lx + 1];
        rho_0 = new ArrayFloat[lx + 1];
        vx = new ArrayFloat[lx + 1];
        vy = new ArrayFloat[lx + 1];
        x = new ArrayFloat[lx + 1];
        xappr = new ArrayFloat[lx + 1];
        y = new ArrayFloat[lx + 1];
        yappr = new ArrayFloat[lx + 1];
        arrayLength = x.length;
        for (i = 0; i <= lx; i++) {
            gridvx[i] = new ArrayFloat(ly + 1);
            gridvy[i] = new ArrayFloat(ly + 1);
            rho[i] = new ArrayFloat(ly + 1);
            rho_0[i] = new ArrayFloat(ly + 1);
            vx[i] = new ArrayFloat(ly + 1);
            vy[i] = new ArrayFloat(ly + 1);
            x[i] = new ArrayFloat(ly + 1);
            xappr[i] = new ArrayFloat(ly + 1);
            y[i] = new ArrayFloat(ly + 1);
            yappr[i] = new ArrayFloat(ly + 1);
        }
        digdens();
        do {
            n = nonlinvoltra();
        } while (!n);
        if (!TransformsMain.MAP2PS.equals("")) {
            infile = FileTools.openFileRead(polygonFileName);
            outfile = FileTools.openFileWrite(MAP2PS);
            pspicture(infile, outfile);
            infile.close();
            outfile.close();
        }
        cartogram();
        if (!TransformsMain.CART2PS.equals("")) {
            infile = FileTools.openFileRead(genFileName);
            outfile = FileTools.openFileWrite(CART2PS);
            pspicture(infile, outfile);
            infile.close();
            outfile.close();
        }
    }

    public static void main(String[] args) {
        try {
            TransformsMain t = new TransformsMain(true);
            logger.finest("All done! " + t);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("I/O error!\n");
        }
    }

    public void setBlurWidth(double blurWidth) {
        this.blurWidth = blurWidth;
    }

    public void setBlurWidthFactor(double blurWidthFactor) {
        this.blurWidthFactor = blurWidthFactor;
    }

    public void setGenFileName(String genFileName) {
        this.genFileName = genFileName;
    }

    public void setPolygonFileName(String polygonFileName) {
        this.polygonFileName = polygonFileName;
    }

    public void setDataFileName(String dataFileName) {
        this.dataFileName = dataFileName;
    }

    public void setMaxNSquareLog(int maxNSquareLog) {
        this.maxNSquareLog = maxNSquareLog;
    }

    public double getBlurWidth() {
        return blurWidth;
    }

    public double getBlurWidthFactor() {
        return blurWidthFactor;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public String getGenFileName() {
        return genFileName;
    }

    public String getPolygonFileName() {
        return polygonFileName;
    }

    public int getMaxNSquareLog() {
        return maxNSquareLog;
    }

    public ArrayFloat[] retreiveX() {
        return x;
    }

    public ArrayFloat[] retreiveY() {
        return y;
    }

    public void putX(ArrayFloat[] x) {
        this.x = x;
    }

    public void putY(ArrayFloat[] y) {
        this.y = y;
    }

    public int getArrayLength() {
        return arrayLength;
    }

    public int getLx() {
        return lx;
    }

    public int getLy() {
        return ly;
    }

    public float getMaxx() {
        return maxx;
    }

    public float getMaxy() {
        return maxy;
    }

    public float getMinx() {
        return minx;
    }

    public float getMiny() {
        return miny;
    }

    public void setArrayLength(int arrayLength) {
        this.arrayLength = arrayLength;
    }

    public void setLx(int lx) {
        this.lx = lx;
    }

    public void setLy(int ly) {
        this.ly = ly;
    }

    public void setMaxx(float maxx) {
        this.maxx = maxx;
    }

    public void setMaxy(float maxy) {
        this.maxy = maxy;
    }

    public void setMinx(float minx) {
        this.minx = minx;
    }

    public void setMiny(float miny) {
        this.miny = miny;
    }
}
