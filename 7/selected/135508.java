package edu.idp.client.model;

import java.awt.*;

/**
 *
 * @author Kristopher T Babic
 */
public abstract class Model3D {

    private float vert[];

    private int tvert[];

    private int nvert, maxvert;

    private int con[];

    private int ncon, maxcon;

    private boolean transformed;

    private Matrix4D mat;

    private float xmin, xmax, ymin, ymax, zmin, zmax;

    public Model3D() {
        mat = new Matrix4D();
        initModel();
    }

    public float getXMin() {
        return xmin;
    }

    public float getXMax() {
        return xmax;
    }

    public float getYMax() {
        return ymax;
    }

    public float getYMin() {
        return ymin;
    }

    public float getZMin() {
        return zmin;
    }

    public float getZMax() {
        return zmax;
    }

    public void setTransform() {
        transformed = false;
    }

    public void setIdentity() {
        mat.setIdentity();
    }

    public void setRotX(float angle) {
        mat.setXRot(angle);
    }

    public void setRotY(float angle) {
        mat.setYRot(angle);
    }

    public void setRotZ(float angle) {
        mat.setZRot(angle);
    }

    public void setScale(float x, float y, float z) {
        mat.setScale(x, y, z);
    }

    public void setScale(float s) {
        mat.setScale(s);
    }

    public void setTranslate(float x, float y, float z) {
        mat.setTranslate(x, y, z);
    }

    public void setTranlate(float t) {
        mat.setTranslate(t);
    }

    protected abstract void initModel();

    /**
     * Add a vertex to this model
     */
    protected int addVert(float x, float y, float z) {
        int i = nvert;
        if (i >= maxvert) if (vert == null) {
            maxvert = 100;
            vert = new float[maxvert * 3];
        } else {
            maxvert *= 2;
            float nv[] = new float[maxvert * 3];
            System.arraycopy(vert, 0, nv, 0, vert.length);
            vert = nv;
        }
        i *= 3;
        vert[i] = x;
        vert[i + 1] = y;
        vert[i + 2] = z;
        return nvert++;
    }

    /**
     * Add a line from vertex p1 to vertex p2
     */
    protected void addEdge(int p1, int p2) {
        int i = ncon;
        if (p1 >= nvert || p2 >= nvert) return;
        if (i >= maxcon) if (con == null) {
            maxcon = 100;
            con = new int[maxcon];
        } else {
            maxcon *= 2;
            int nv[] = new int[maxcon];
            System.arraycopy(con, 0, nv, 0, con.length);
            con = nv;
        }
        if (p1 > p2) {
            int t = p1;
            p1 = p2;
            p2 = t;
        }
        con[i] = (p1 << 16) | p2;
        ncon = i + 1;
    }

    /**
     * Transform all the points in this model
     */
    protected void transform() {
        if (transformed || nvert <= 0) return;
        if (tvert == null || tvert.length < nvert * 3) tvert = new int[nvert * 3];
        float temp[] = new float[4];
        for (int i = nvert * 3; (i -= 3) >= 0; ) {
            temp[0] = vert[i];
            temp[1] = vert[i + 1];
            temp[2] = vert[i + 2];
            temp[3] = 1.0f;
            float[] newVert = mat.mult(temp);
            tvert[i] = (int) newVert[0];
            tvert[i + 1] = (int) newVert[1];
            tvert[i + 2] = (int) newVert[2];
            transformed = true;
        }
    }

    private void quickSort(int a[], int left, int right) {
        int leftIndex = left;
        int rightIndex = right;
        int partionElement;
        if (right > left) {
            partionElement = a[(left + right) / 2];
            while (leftIndex <= rightIndex) {
                while ((leftIndex < right) && (a[leftIndex] < partionElement)) ++leftIndex;
                while ((rightIndex > left) && (a[rightIndex] > partionElement)) --rightIndex;
                if (leftIndex <= rightIndex) {
                    swap(a, leftIndex, rightIndex);
                    ++leftIndex;
                    --rightIndex;
                }
            }
            if (left < rightIndex) quickSort(a, left, rightIndex);
            if (leftIndex < right) quickSort(a, leftIndex, right);
        }
    }

    private void swap(int a[], int i, int j) {
        int T;
        T = a[i];
        a[i] = a[j];
        a[j] = T;
    }

    /**
     * eliminate duplicate lines
     */
    protected void compress() {
        int limit = ncon;
        int c[] = con;
        quickSort(con, 0, ncon - 1);
        int d = 0;
        int pp1 = -1;
        for (int i = 0; i < limit; i++) {
            int p1 = c[i];
            if (pp1 != p1) {
                c[d] = p1;
                d++;
            }
            pp1 = p1;
        }
        ncon = d;
    }

    static Color gr[];

    /**
     * Paint this model to a graphics context.  It uses the matrix associated
     * with this model to map from model space to screen space.
     * The next version of the browser should have double buffering,
     * which will make this *much* nicer
     */
    public void paint(Graphics g) {
        if (vert == null || nvert <= 0) return;
        transform();
        if (gr == null) {
            gr = new Color[16];
            double slope = (20. - 200.) / (15. - 0.);
            for (int i = 0; i < 16; i++) {
                int grey = (int) ((slope * i) + 200.);
                if (grey <= 0) grey = 1;
                gr[i] = new Color(grey, grey, grey);
            }
        }
        int lg = 0;
        int lim = ncon;
        int c[] = con;
        int v[] = tvert;
        if (lim <= 0 || nvert <= 0) return;
        for (int i = 0; i < lim; i++) {
            int T = c[i];
            int p1 = ((T >> 16) & 0xFFFF) * 3;
            int p2 = (T & 0xFFFF) * 3;
            int grey = v[p1 + 2] + v[p2 + 2];
            if (grey < 0) grey = 0;
            if (grey > 15) grey = 15;
            lg = grey;
            g.setColor(gr[grey]);
            g.drawLine(v[p1], v[p1 + 1], v[p2], v[p2 + 1]);
        }
    }

    /**
     * Find the bounding box of this model
     */
    protected void findBB() {
        if (nvert <= 0) return;
        float v[] = vert;
        float xmin = v[0], xmax = xmin;
        float ymin = v[1], ymax = ymin;
        float zmin = v[2], zmax = zmin;
        for (int i = nvert * 3; (i -= 3) > 0; ) {
            float x = v[i];
            if (x < xmin) xmin = x;
            if (x > xmax) xmax = x;
            float y = v[i + 1];
            if (y < ymin) ymin = y;
            if (y > ymax) ymax = y;
            float z = v[i + 2];
            if (z < zmin) zmin = z;
            if (z > zmax) zmax = z;
        }
        this.xmax = xmax;
        this.xmin = xmin;
        this.ymax = ymax;
        this.ymin = ymin;
        this.zmax = zmax;
        this.zmin = zmin;
    }
}
