package com.dukesoftware.viewlon3.gui.realworldview;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import com.dukesoftware.utils.math.Point3d;
import com.dukesoftware.utils.math.SimpleFace;
import com.dukesoftware.utils.math.SimplePoint3d;
import com.dukesoftware.utils.math.SimpleVector3d;
import com.dukesoftware.viewlon3.data.common.Const;
import com.dukesoftware.viewlon3.data.common.DataControl;
import com.dukesoftware.viewlon3.data.common.DataManagerCore;
import com.dukesoftware.viewlon3.data.internal.RealObject;
import com.dukesoftware.viewlon3.data.relation.interfacetool.RelationManager;
import com.dukesoftware.viewlon3.gui.common.CommonCanvas;
import com.dukesoftware.viewlon3.gui.infopanel.InfoPanel;
import com.dukesoftware.viewlon3.utils.viewlon.Fonts;

/**
 * 実世界ビューです。
 * 
 * 
 *
 *
 */
public class RealWorldCanvas extends CommonCanvas {

    private static final long serialVersionUID = 1L;

    private static final double SENSE = 0.01;

    private Point3d vt[];

    private SimpleFace fc[];

    private double m_Scale;

    private double phi, theta;

    public RealWorldCanvas(RelationManager r_con, DataControl d_con, int mapx, int mapy, InfoPanel info_panel) {
        super(r_con, d_con, mapx, mapy, info_panel);
        centerp.x = mapx / 2.0;
        centerp.y = mapy / 2.0;
        initVertexAndFace(100, 100);
        addMouseMotionListener(this);
        addMouseListener(this);
    }

    public void executeInLoop() throws InterruptedException {
        repaint();
        Thread.sleep(INTERVAL);
    }

    private void paintRealObj(Graphics g) {
        DataManagerCore oarray = d_con.getPointerData();
        int select_id = oarray.getSelectRealObjNum();
        calcDist();
        for (Iterator<RealObject> it = oarray.objectIterator(); it.hasNext(); ) {
            final RealObject robj = it.next();
            String id = robj.getID();
            int i = oarray.searchRealObjectInternalID(id);
            double z = oarray.getRealPhyZ(i);
            double x = oarray.getRealPhyX(i);
            double y = oarray.getRealPhyY(i);
            SimplePoint3d size = oarray.getRealSize(id);
            if (size != null) {
                if (robj.getFrontPositionID() != null && robj.getBackPositionID() != null) {
                    final SimpleVector3d direction = robj.getDirection();
                    double theta_obj = getTheta(direction.x, direction.y);
                    setModelDataDirectionObject(x, y, -z, size.x, size.y, size.z, theta_obj);
                    if (select_id == i) DrawModel(g, i, true, (float) (0.2 + 0.01 * i), (float) 0.5, 10, 16); else DrawModel(g, i, true, (float) (0.7 + 0.01 * i), (float) 0.5, 10, 16);
                } else {
                    setModelDataBox(x, y, -z, size.x, size.y, size.z, 0);
                    if (select_id == i) DrawModel(g, i, true, (float) (0.2 + 0.01 * i), (float) 0.5, 8, 12); else DrawModel(g, i, true, (float) (0.7 + 0.01 * i), (float) 0.5, 8, 12);
                }
            }
        }
    }

    public void setCenterX(int x) {
        centerp.x = x;
    }

    public void setCenterY(int y) {
        centerp.y = y;
    }

    public int getCenterX() {
        return ((int) centerp.x);
    }

    public int getCenterY() {
        return ((int) centerp.y);
    }

    protected void setFocus() {
        DataManagerCore oarray = d_con.getPointerData();
        int select_num = oarray.getSelectRealObjNum();
        if (select_num != Const.NOID) {
            centerp.x = -oarray.getRealPhyX(select_num) * m_Scale + panelx / 2.0;
            centerp.y = oarray.getRealPhyY(select_num) * m_Scale + panely / 2.0;
        } else {
            centerp.x = panelx / 2d;
            centerp.y = panely / 2d;
        }
    }

    public void paintBody(Graphics g) {
        paintRealObj(g);
        paintDate(g);
    }

    private void initVertexAndFace(int vt_num, int fc_num) {
        vt = new Point3d[vt_num];
        fc = new SimpleFace[fc_num];
        m_Scale = 1;
        phi = theta = 0;
    }

    private void setModelDataBox(double x, double y, double z, double sizex, double sizey, double sizez, double theta_obj) {
        double sizex2 = sizex / 2;
        double sizey2 = sizey / 2;
        double sizez2 = sizez / 2;
        vt[0] = new Point3d(x + sizex2, y - sizey2, z + sizez2);
        vt[1] = new Point3d(x - sizex2, y - sizey2, z + sizez2);
        vt[2] = new Point3d(x - sizex2, y + sizey2, z + sizez2);
        vt[3] = new Point3d(x + sizex2, y + sizey2, z + sizez2);
        vt[4] = new Point3d(x + sizex2, y + sizey2, z - sizez2);
        vt[5] = new Point3d(x + sizex2, y - sizey2, z - sizez2);
        vt[6] = new Point3d(x - sizex2, y - sizey2, z - sizez2);
        vt[7] = new Point3d(x - sizex2, y + sizey2, z - sizez2);
        if (theta_obj != 0) rotateXY(theta_obj, x, y, 8, vt);
        fc[0] = new SimpleFace(vt[0], vt[2], vt[1]);
        fc[1] = new SimpleFace(vt[6], vt[7], vt[5]);
        fc[2] = new SimpleFace(vt[0], vt[1], vt[5]);
        fc[3] = new SimpleFace(vt[2], vt[3], vt[4]);
        fc[4] = new SimpleFace(vt[0], vt[5], vt[3]);
        fc[5] = new SimpleFace(vt[1], vt[2], vt[6]);
        fc[6] = new SimpleFace(vt[0], vt[3], vt[2]);
        fc[7] = new SimpleFace(vt[5], vt[7], vt[4]);
        fc[8] = new SimpleFace(vt[1], vt[6], vt[5]);
        fc[9] = new SimpleFace(vt[2], vt[4], vt[7]);
        fc[10] = new SimpleFace(vt[3], vt[5], vt[4]);
        fc[11] = new SimpleFace(vt[2], vt[7], vt[6]);
    }

    private void rotateXY(double theta_obj, double x, double y, int vt_num, Point3d[] vt) {
        double tmpSin = Math.sin(-theta_obj);
        double tmpCos = Math.cos(-theta_obj);
        for (int i = 0; i < vt_num; i++) {
            double tx = (vt[i].x - x) * tmpCos + (vt[i].y - y) * tmpSin;
            double ty = -(vt[i].x - x) * tmpSin + (vt[i].y - y) * tmpCos;
            vt[i].x = tx + x;
            vt[i].y = ty + y;
        }
    }

    private static double getTheta(double x, double y) {
        double r = Point2D.distance(x, y, 0, 0);
        if (y >= 0 && x >= 0) return Math.asin(y / r); else if (y > 0 && x < 0) return Math.acos(x / r); else return -Math.acos(x / r);
    }

    private void setModelDataDirectionObject(double x, double y, double z, double sizex, double sizey, double sizez, double theta_obj) {
        double sizex2 = sizex / 2;
        double sizey2 = sizey / 2;
        double sizez2 = sizez / 2;
        vt[0] = new Point3d(x + sizex2, y - sizey2, z + sizez2);
        vt[1] = new Point3d(x - sizex2, y - sizey2, z + sizez2);
        vt[2] = new Point3d(x - sizex2, y + sizey2, z + sizez2);
        vt[3] = new Point3d(x + sizex2, y + sizey2, z + sizez2);
        vt[4] = new Point3d(x + sizex2, y + sizey2, z - sizez2);
        vt[5] = new Point3d(x + sizex2, y - sizey2, z - sizez2);
        vt[6] = new Point3d(x - sizex2, y - sizey2, z - sizez2);
        vt[7] = new Point3d(x - sizex2, y + sizey2, z - sizez2);
        vt[8] = new Point3d(x + sizex, y, z + sizez2);
        vt[9] = new Point3d(x + sizex, y, z - sizez2);
        if (theta_obj != 0) rotateXY(theta_obj, x, y, 10, vt);
        fc[0] = new SimpleFace(vt[0], vt[2], vt[1]);
        fc[1] = new SimpleFace(vt[6], vt[7], vt[5]);
        fc[2] = new SimpleFace(vt[0], vt[1], vt[5]);
        fc[3] = new SimpleFace(vt[2], vt[3], vt[4]);
        fc[4] = new SimpleFace(vt[1], vt[2], vt[6]);
        fc[5] = new SimpleFace(vt[0], vt[3], vt[2]);
        fc[6] = new SimpleFace(vt[5], vt[7], vt[4]);
        fc[7] = new SimpleFace(vt[1], vt[6], vt[5]);
        fc[8] = new SimpleFace(vt[2], vt[4], vt[7]);
        fc[9] = new SimpleFace(vt[2], vt[7], vt[6]);
        fc[10] = new SimpleFace(vt[0], vt[8], vt[3]);
        fc[11] = new SimpleFace(vt[5], vt[4], vt[9]);
        fc[12] = new SimpleFace(vt[0], vt[5], vt[9]);
        fc[13] = new SimpleFace(vt[0], vt[9], vt[8]);
        fc[14] = new SimpleFace(vt[8], vt[9], vt[3]);
        fc[15] = new SimpleFace(vt[9], vt[4], vt[3]);
    }

    public void setM_Scale(double m_Scale) {
        this.m_Scale = m_Scale;
    }

    public double getM_Scale() {
        return m_Scale;
    }

    private void DrawModel(Graphics offg, int obj_num, boolean object, float h, float s, int vt_num, int fc_num) {
        int px[] = new int[3];
        int py[] = new int[3];
        int count = 0;
        int tmp[] = new int[fc_num];
        double tmp_depth[] = new double[fc_num];
        rotate(vt_num);
        offg.setColor(Color.black);
        for (int i = 0; i < fc_num; i++) {
            double a1 = fc[i].vt1.x - fc[i].vt0.x;
            double a2 = fc[i].vt1.y - fc[i].vt0.y;
            double a3 = fc[i].vt1.z - fc[i].vt0.z;
            double b1 = fc[i].vt2.x - fc[i].vt1.x;
            double b2 = fc[i].vt2.y - fc[i].vt1.y;
            double b3 = fc[i].vt2.z - fc[i].vt1.z;
            fc[i].nx = a2 * b3 - a3 * b2;
            fc[i].ny = a3 * b1 - a1 * b3;
            fc[i].nz = a1 * b2 - a2 * b1;
            if (fc[i].nz < 0) {
                fc[i].nx = a2 * b3 - a3 * b2;
                fc[i].ny = a3 * b1 - a1 * b3;
                tmp[count] = i;
                tmp_depth[count] = fc[i].getDepth();
                count++;
            }
        }
        int lim = count - 1;
        do {
            int m = 0;
            for (int n = 0; n <= lim - 1; n++) {
                if (tmp_depth[n] < tmp_depth[n + 1]) {
                    double t = tmp_depth[n];
                    tmp_depth[n] = tmp_depth[n + 1];
                    tmp_depth[n + 1] = t;
                    int ti = tmp[n];
                    tmp[n] = tmp[n + 1];
                    tmp[n + 1] = ti;
                    m = n;
                }
            }
            lim = m;
        } while (lim != 0);
        for (int m = 0; m < count; m++) {
            int i = tmp[m];
            double l = Math.sqrt(fc[i].nx * fc[i].nx + fc[i].ny * fc[i].ny + fc[i].nz * fc[i].nz);
            test(offg, i, l, h, s);
            px[0] = (int) (fc[i].vt0.x * m_Scale + centerp.x);
            py[0] = (int) (-fc[i].vt0.y * m_Scale + centerp.y);
            px[1] = (int) (fc[i].vt1.x * m_Scale + centerp.x);
            py[1] = (int) (-fc[i].vt1.y * m_Scale + centerp.y);
            px[2] = (int) (fc[i].vt2.x * m_Scale + centerp.x);
            py[2] = (int) (-fc[i].vt2.y * m_Scale + centerp.y);
            offg.fillPolygon(px, py, 3);
        }
        if (labelFlag && object) {
            offg.setFont(Fonts.FONT_REAL);
            offg.drawString(d_con.getPointerData().getRealObjName(obj_num), (int) ((fc[0].vt0.x + 10) * m_Scale + centerp.x), (int) (-(fc[0].vt0.y + 10) * m_Scale + centerp.y));
        }
    }

    private void test(Graphics offg, int i, double l, float h, float s) {
        int B = (int) (75 - 180 * fc[i].nz / l);
        if (B < 0) B = 0;
        if (B > 255) B = 255;
        offg.setColor(Color.getHSBColor((float) h, (float) s, (float) B / 255));
    }

    public void rotate(int vt_num) {
        double tmpSin = Math.sin(theta);
        double tmpCos = Math.cos(theta);
        for (int i = 0; i < vt_num; i++) {
            double ty = vt[i].y * tmpCos + vt[i].z * tmpSin;
            double tz = -vt[i].y * tmpSin + vt[i].z * tmpCos;
            vt[i].y = ty;
            vt[i].z = tz;
        }
        tmpSin = Math.sin(phi);
        tmpCos = Math.cos(phi);
        for (int i = 0; i < vt_num; i++) {
            double tx = vt[i].x * tmpCos - vt[i].z * tmpSin;
            double tz = vt[i].x * tmpSin + vt[i].z * tmpCos;
            vt[i].x = tx;
            vt[i].z = tz;
        }
    }

    private void calcDist() {
        DataManagerCore oarray = d_con.getPointerData();
        for (Iterator<RealObject> it = oarray.objectIterator(); it.hasNext(); ) {
            String id = it.next().getID();
            int i = oarray.searchRealObjectInternalID(id);
            if (oarray.checkPosFlag(i)) {
                double x = oarray.getRealPhyX(i);
                double y = oarray.getRealPhyY(i);
                double z = oarray.getRealPhyZ(i);
                double tmpSin = Math.sin(-theta);
                double tmpCos = Math.cos(-theta);
                Point3d[] vt = new Point3d[3];
                SimplePoint3d size = oarray.getRealSize(id);
                if (size != null) {
                    vt[0] = new Point3d(x + size.x, y - size.y, z + size.z);
                    vt[1] = new Point3d(x - size.x, y - size.y, z + size.z);
                    vt[2] = new Point3d(x - size.x, y + size.y, z + size.z);
                }
                for (int j = 0; j < 3; j++) {
                    double ty = vt[j].y * tmpCos + vt[j].z * tmpSin;
                    double tz = -vt[j].y * tmpSin + vt[j].z * tmpCos;
                    vt[j].y = ty;
                    vt[j].z = tz;
                }
                tmpSin = Math.sin(-phi);
                tmpCos = Math.cos(-phi);
                for (int j = 0; j < 3; j++) {
                    double tx = vt[j].x * tmpCos - vt[j].z * tmpSin;
                    double tz = vt[j].x * tmpSin + vt[j].z * tmpCos;
                    vt[j].x = tx;
                    vt[j].z = tz;
                }
            }
        }
    }

    public void mouseDragged(MouseEvent e) {
        Point p2 = e.getPoint();
        phi += (p2.x - p.x) * SENSE;
        theta += (p2.y - p.y) * SENSE;
        p.x = p2.x;
        p.y = p2.y;
    }
}
