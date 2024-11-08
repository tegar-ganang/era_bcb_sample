package org.opensourcephysics.display3d.simple3d;

import org.opensourcephysics.controls.*;

/**
 * <p>Title: ElementEllipsoid</p>
 * <p>Description: Painter's algorithm implementation of an Ellipsoid</p>
 * @author Francisco Esquembre
 * @version March 2005
 */
public class ElementEllipsoid extends AbstractTile implements org.opensourcephysics.display3d.core.ElementEllipsoid {

    private boolean closedBottom = true, closedTop = true;

    private boolean closedLeft = true, closedRight = true;

    private int minAngleU = 0, maxAngleU = 360;

    private int minAngleV = -90, maxAngleV = 90;

    private boolean changeNTiles = true;

    private int nr = -1, nu = -1, nv = -1;

    private double[][][] standardSphere = null;

    {
        getStyle().setResolution(new Resolution(3, 12, 12));
    }

    public void setClosedBottom(boolean close) {
        this.closedBottom = close;
        setElementChanged(true);
        changeNTiles = true;
    }

    public boolean isClosedBottom() {
        return this.closedBottom;
    }

    public void setClosedTop(boolean close) {
        this.closedTop = close;
        setElementChanged(true);
        changeNTiles = true;
    }

    public boolean isClosedTop() {
        return this.closedTop;
    }

    public void setClosedLeft(boolean close) {
        this.closedLeft = close;
        setElementChanged(true);
        changeNTiles = true;
    }

    public boolean isClosedLeft() {
        return this.closedLeft;
    }

    public void setClosedRight(boolean close) {
        this.closedRight = close;
        setElementChanged(true);
        changeNTiles = true;
    }

    public boolean isClosedRight() {
        return this.closedRight;
    }

    public void setMinimumAngleU(int angle) {
        this.minAngleU = angle;
        setElementChanged(true);
        changeNTiles = true;
    }

    public int getMinimumAngleU() {
        return this.minAngleU;
    }

    public void setMaximumAngleU(int angle) {
        this.maxAngleU = angle;
        setElementChanged(true);
        changeNTiles = true;
    }

    public int getMaximumAngleU() {
        return this.maxAngleU;
    }

    public void setMinimumAngleV(int angle) {
        this.minAngleV = angle;
        setElementChanged(true);
        changeNTiles = true;
    }

    public int getMinimumAngleV() {
        return this.minAngleV;
    }

    public void setMaximumAngleV(int angle) {
        this.maxAngleV = angle;
        setElementChanged(true);
        changeNTiles = true;
    }

    public int getMaximumAngleV() {
        return this.maxAngleV;
    }

    protected synchronized void computeCorners() {
        int theNr = 1, theNu = 1, theNv = 1;
        double angleu1 = minAngleU, angleu2 = maxAngleU;
        if (Math.abs(angleu2 - angleu1) > 360) {
            angleu2 = angleu1 + 360;
        }
        double anglev1 = minAngleV, anglev2 = maxAngleV;
        if (Math.abs(anglev2 - anglev1) > 180) {
            anglev2 = anglev1 + 180;
        }
        org.opensourcephysics.display3d.core.Resolution res = getRealStyle().getResolution();
        if (res != null) {
            switch(res.getType()) {
                case Resolution.DIVISIONS:
                    theNr = Math.max(res.getN1(), 1);
                    theNu = Math.max(res.getN2(), 1);
                    theNv = Math.max(res.getN3(), 1);
                    break;
                case Resolution.MAX_LENGTH:
                    double maxRadius = Math.max(Math.max(Math.abs(getSizeX()), Math.abs(getSizeY())), Math.abs(getSizeZ())) / 2;
                    theNr = Math.max((int) Math.round(0.49 + maxRadius / res.getMaxLength()), 1);
                    theNu = Math.max((int) Math.round(0.49 + Math.abs(angleu2 - angleu1) * TO_RADIANS * maxRadius / res.getMaxLength()), 1);
                    theNv = Math.max((int) Math.round(0.49 + Math.abs(anglev2 - anglev1) * TO_RADIANS * maxRadius / res.getMaxLength()), 1);
                    break;
            }
        }
        if (nr != theNr || nu != theNu || nv != theNv || changeNTiles) {
            nr = theNr;
            nu = theNu;
            nv = theNv;
            standardSphere = createStandardEllipsoid(nr, nu, nv, angleu1, angleu2, anglev1, anglev2, closedTop, closedBottom, closedLeft, closedRight);
            setCorners(new double[standardSphere.length][4][3]);
            changeNTiles = false;
        }
        for (int i = 0; i < numberOfTiles; i++) {
            for (int j = 0, sides = corners[i].length; j < sides; j++) {
                System.arraycopy(standardSphere[i][j], 0, corners[i][j], 0, 3);
                sizeAndToSpaceFrame(corners[i][j]);
            }
        }
        setElementChanged(false);
    }

    protected static final double TO_RADIANS = Math.PI / 180.0;

    protected static final double[] vectorx = { 1.0, 0.0, 0.0 }, vectory = { 0.0, 1.0, 0.0 }, vectorz = { 0.0, 0.0, 1.0 };

    private static double[][][] createStandardEllipsoid(int nr, int nu, int nv, double angleu1, double angleu2, double anglev1, double anglev2, boolean top, boolean bottom, boolean left, boolean right) {
        int totalN = nu * nv;
        if (Math.abs(anglev2 - anglev1) < 180) {
            if (bottom) {
                totalN += nr * nu;
            }
            if (top) {
                totalN += nr * nu;
            }
        }
        if (Math.abs(angleu2 - angleu1) < 360) {
            if (left) {
                totalN += nr * nv;
            }
            if (right) {
                totalN += nr * nv;
            }
        }
        double[][][] data = new double[totalN][4][3];
        double[] cosu = new double[nu + 1], sinu = new double[nu + 1];
        double[] cosv = new double[nv + 1], sinv = new double[nv + 1];
        for (int u = 0; u <= nu; u++) {
            double angle = ((nu - u) * angleu1 + u * angleu2) * TO_RADIANS / nu;
            cosu[u] = Math.cos(angle);
            sinu[u] = Math.sin(angle);
        }
        for (int v = 0; v <= nv; v++) {
            double angle = ((nv - v) * anglev1 + v * anglev2) * TO_RADIANS / nv;
            cosv[v] = Math.cos(angle) / 2;
            sinv[v] = Math.sin(angle) / 2;
        }
        int tile = 0;
        double[] center = new double[] { 0, 0, 0 };
        {
            for (int v = 0; v < nv; v++) {
                for (int u = 0; u < nu; u++, tile++) {
                    for (int k = 0; k < 3; k++) {
                        data[tile][0][k] = (cosu[u] * vectorx[k] + sinu[u] * vectory[k]) * cosv[v] + sinv[v] * vectorz[k];
                        data[tile][1][k] = (cosu[u + 1] * vectorx[k] + sinu[u + 1] * vectory[k]) * cosv[v] + sinv[v] * vectorz[k];
                        data[tile][2][k] = (cosu[u + 1] * vectorx[k] + sinu[u + 1] * vectory[k]) * cosv[v + 1] + sinv[v + 1] * vectorz[k];
                        data[tile][3][k] = (cosu[u] * vectorx[k] + sinu[u] * vectory[k]) * cosv[v + 1] + sinv[v + 1] * vectorz[k];
                    }
                }
            }
        }
        if (Math.abs(anglev2 - anglev1) < 180) {
            if (bottom) {
                center[2] = sinv[0];
                for (int u = 0; u < nu; u++) {
                    for (int i = 0; i < nr; i++, tile++) {
                        for (int k = 0; k < 3; k++) {
                            data[tile][0][k] = ((nr - i) * center[k] + i * data[u][0][k]) / nr;
                            data[tile][1][k] = ((nr - i - 1) * center[k] + (i + 1) * data[u][0][k]) / nr;
                            data[tile][2][k] = ((nr - i - 1) * center[k] + (i + 1) * data[u][1][k]) / nr;
                            data[tile][3][k] = ((nr - i) * center[k] + i * data[u][1][k]) / nr;
                        }
                    }
                }
            }
            if (top) {
                center[2] = sinv[nv];
                int ref = nu * (nv - 1);
                for (int u = 0; u < nu; u++) {
                    for (int i = 0; i < nr; i++, tile++) {
                        for (int k = 0; k < 3; k++) {
                            data[tile][0][k] = ((nr - i) * center[k] + i * data[ref + u][3][k]) / nr;
                            data[tile][1][k] = ((nr - i - 1) * center[k] + (i + 1) * data[ref + u][3][k]) / nr;
                            data[tile][2][k] = ((nr - i - 1) * center[k] + (i + 1) * data[ref + u][2][k]) / nr;
                            data[tile][3][k] = ((nr - i) * center[k] + i * data[ref + u][2][k]) / nr;
                        }
                    }
                }
            }
        }
        if (Math.abs(angleu2 - angleu1) < 360) {
            double[] nextCenter = new double[] { 0, 0, 0 };
            if (right) {
                int ref = 0;
                for (int j = 0; j < nv; j++, ref += nu) {
                    center[2] = sinv[j];
                    nextCenter[2] = sinv[j + 1];
                    for (int i = 0; i < nr; i++, tile++) {
                        for (int k = 0; k < 3; k++) {
                            data[tile][0][k] = ((nr - i) * center[k] + i * data[ref][0][k]) / nr;
                            data[tile][1][k] = ((nr - i - 1) * center[k] + (i + 1) * data[ref][0][k]) / nr;
                            data[tile][2][k] = ((nr - i - 1) * nextCenter[k] + (i + 1) * data[ref][3][k]) / nr;
                            data[tile][3][k] = ((nr - i) * nextCenter[k] + i * data[ref][3][k]) / nr;
                        }
                    }
                }
            }
            if (left) {
                int ref = nu - 1;
                for (int j = 0; j < nv; j++, ref += nu) {
                    center[2] = sinv[j];
                    nextCenter[2] = sinv[j + 1];
                    for (int i = 0; i < nr; i++, tile++) {
                        for (int k = 0; k < 3; k++) {
                            data[tile][0][k] = ((nr - i) * center[k] + i * data[ref][1][k]) / nr;
                            data[tile][1][k] = ((nr - i - 1) * center[k] + (i + 1) * data[ref][1][k]) / nr;
                            data[tile][2][k] = ((nr - i - 1) * nextCenter[k] + (i + 1) * data[ref][2][k]) / nr;
                            data[tile][3][k] = ((nr - i) * nextCenter[k] + i * data[ref][2][k]) / nr;
                        }
                    }
                }
            }
        }
        return data;
    }

    /**
   * Returns an XML.ObjectLoader to save and load object data.
   * @return the XML.ObjectLoader
   */
    public static XML.ObjectLoader getLoader() {
        return new Loader();
    }

    private static class Loader extends org.opensourcephysics.display3d.core.ElementEllipsoid.Loader {

        public Object createObject(XMLControl control) {
            return new ElementEllipsoid();
        }
    }
}
