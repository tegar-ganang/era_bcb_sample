import ij.IJ;
import ij.gui.Wand;
import ij.process.ImageProcessor;
import ij.gui.Roi;
import ij.gui.PolygonRoi;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.Rectangle;

class WandCurve implements Curve {

    int[] x;

    int[] y;

    String name;

    String group;

    String color;

    int slice;

    String supergroupname;

    String supergroupcolor;

    WandCurve(String name, String group, String color, int slice, String supergroupname, String supergroupcolor) {
        this.name = name;
        this.group = group;
        this.slice = slice;
        this.color = color;
        this.supergroupname = supergroupname;
        this.supergroupcolor = supergroupcolor;
        x = new int[0];
        y = new int[0];
    }

    WandCurve(String name, String group, String color, int slice, String supergroupname, String supergroupcolor, int[] x, int[] y) {
        this.name = name;
        this.group = group;
        this.slice = slice;
        this.color = color;
        this.supergroupname = supergroupname;
        this.supergroupcolor = supergroupcolor;
        this.x = x;
        this.y = y;
    }

    void mousePressed(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        if (isDone()) {
            ic.imp.killRoi();
            ic.dragged = findPointInWandCurve(ic.x_p, ic.y_p);
            if (ic.dragged != -1) {
                if (e.isControlDown()) {
                    removePoint(ic.dragged);
                    ic.post_action = "Deleted";
                    return;
                } else {
                    ic.post_action = "Dragged";
                }
            } else {
                if (!e.isAltDown()) {
                    ic.dragged = insertPoint(ic.x_p, ic.y_p);
                    ic.post_action = "New Point";
                }
            }
        } else if (!ic.a3de.switch_wand) {
            ImageProcessor ipr = ic.imp.getStack().getProcessor(ic.imp.getCurrentSlice());
            Wand w = new Wand(ipr);
            double t1 = ipr.getMinThreshold();
            if (t1 == ipr.NO_THRESHOLD) w.autoOutline(ic.x_p, ic.y_p); else w.autoOutline(ic.x_p, ic.y_p, (int) t1, (int) ipr.getMaxThreshold());
            if (w.npoints > 0) {
                ic.a3de.current_wandcurve = new WandCurve("", "", "", ic.imp.getCurrentSlice() - 1, "", "", w.xpoints, w.ypoints);
                ic.a3de.current_wandcurve.cleanTrailingZeros(w.npoints);
                ic.post_action = "New Wand";
            }
        }
    }

    void mouseDragged(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        if (ic.dragged != -1) {
            dragWandCurvePoint(ic.x_d, ic.y_d, ic.dragged);
            ic.repaint();
            return;
        }
        if (ic.dragged == -1 && e.isAltDown()) {
            if (!ic.startMoving) {
                ic.x_d_old = ic.x_d;
                ic.y_d_old = ic.y_d;
                ic.startMoving = true;
            }
            ic.post_action = "Moved";
            moveWandCurve(ic.x_d_old, ic.y_d_old, ic.x_d, ic.y_d);
            ic.x_d_old = ic.x_d;
            ic.y_d_old = ic.y_d;
            ic.repaint();
            return;
        }
    }

    void mouseReleased(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        if (ic.a3de.switch_wand) {
            if (ic.a3de.SA == null) {
                ic.a3de.SA = new Segmenting_Assistant_();
                ic.a3de.SA.setup(new String(), ic.imp);
                ic.a3de.SA.run(ic.imp.getStack().getProcessor(ic.imp.getCurrentSlice()));
            } else ic.a3de.SA.visible(true);
        }
        ic.post_action = ic.a3de.current_group + "-" + ic.post_action + "(" + getSize() + ")";
        ic.a3de.v_hist_wandcurve.addElement(cloneWandCurve());
        ic.a3de.v_hist_wandcurve_namelist.addElement(ic.post_action);
        ic.a3de.csw.setHistory(ic.a3de.v_hist_wandcurve, ic.a3de.v_hist_wandcurve_namelist);
        ic.repaint();
    }

    void makeWandCurveFromROI(Roi roi) {
        if (roi.getType() == Roi.POLYGON) {
            PolygonRoi p = (PolygonRoi) roi;
            int[] px = p.getXCoordinates();
            int[] py = p.getYCoordinates();
            Rectangle r = p.getBoundingRect();
            x = new int[p.getNCoordinates()];
            y = new int[p.getNCoordinates()];
            for (int i = p.getNCoordinates() - 1; i > -1; i--) {
                x[i] = r.x + px[i];
                y[i] = r.y + py[i];
            }
        } else {
            IJ.write("Warning! WandCurve can't be made from ROIs other than POLYGON_ROI");
        }
    }

    void cleanTrailingZeros(int npoints) {
        int[] new_x = new int[npoints];
        int[] new_y = new int[npoints];
        System.arraycopy(x, 0, new_x, 0, npoints);
        System.arraycopy(y, 0, new_y, 0, npoints);
        x = new_x;
        y = new_y;
    }

    int insertPoint(int clicked_x, int clicked_y) {
        int[] index_and_dist = new int[2];
        index_and_dist = findClosestPoint(clicked_x, clicked_y);
        int[] new_x = new int[x.length + 1];
        int[] new_y = new int[y.length + 1];
        if (index_and_dist[0] == 0) {
            new_x[0] = clicked_x;
            new_y[0] = clicked_y;
            System.arraycopy(x, 0, new_x, 1, x.length);
            System.arraycopy(y, 0, new_y, 1, y.length);
        } else {
            System.arraycopy(x, 0, new_x, 0, index_and_dist[0] + 1);
            System.arraycopy(y, 0, new_y, 0, index_and_dist[0] + 1);
            new_x[index_and_dist[0] + 1] = clicked_x;
            new_y[index_and_dist[0] + 1] = clicked_y;
            System.arraycopy(x, index_and_dist[0] + 1, new_x, index_and_dist[0] + 2, new_x.length - index_and_dist[0] - 2);
            System.arraycopy(y, index_and_dist[0] + 1, new_y, index_and_dist[0] + 2, new_y.length - index_and_dist[0] - 2);
        }
        x = new_x;
        y = new_y;
        return index_and_dist[0] + 1;
    }

    void removePoint(int index) {
        int npoints = x.length;
        int[] temp_x = new int[npoints - 1];
        int[] temp_y = new int[npoints - 1];
        for (int i = 0; i < index; i++) {
            temp_x[i] = x[i];
            temp_y[i] = y[i];
        }
        for (int j = index; j < npoints - 1; j++) {
            temp_x[j] = x[j + 1];
            temp_y[j] = y[j + 1];
        }
        x = temp_x;
        y = temp_y;
    }

    int findPointInWandCurve(int clicked_x, int clicked_y) {
        int index = -1;
        for (int i = 0; i < x.length; i++) {
            if ((Math.abs(clicked_x - x[i]) + Math.abs(clicked_y - y[i])) <= 2) {
                index = i;
                break;
            }
        }
        return index;
    }

    void dragWandCurvePoint(int dragged_x, int dragged_y, int index) {
        x[index] = dragged_x;
        y[index] = dragged_y;
    }

    void moveWandCurve(int x_dragged_old, int y_dragged_old, int x_dragged, int y_dragged) {
        for (int i = 0; i < x.length; i++) {
            x[i] = x[i] + (x_dragged - x_dragged_old);
            y[i] = y[i] + (y_dragged - y_dragged_old);
        }
    }

    int[] findClosestPoint(int clicked_x, int clicked_y) {
        int[] index_and_dist = new int[2];
        if (x.length != 0) {
            double distance = calcDist(x[0], y[0], clicked_x, clicked_y);
            double dist2 = 0;
            int position = 0;
            for (int i = 0; i < x.length; i++) {
                dist2 = calcDist(x[i], y[i], clicked_x, clicked_y);
                if (dist2 < distance) {
                    distance = dist2;
                    position = i;
                }
            }
            int previous_point = position - 1;
            index_and_dist[0] = previous_point;
            index_and_dist[1] = (int) distance;
        } else {
            index_and_dist[0] = -1;
            index_and_dist[1] = 10000;
        }
        return index_and_dist;
    }

    double calcDist(int x1, int y1, int x2, int y2) {
        return (Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2)));
    }

    void driftWandCurve(int driftX, int driftY) {
        for (int i = 0; i < x.length; i++) {
            x[i] += driftX;
            y[i] += driftY;
        }
    }

    void paintWandCurve(Graphics g, boolean paint_ovals, Color givencolor, int srcRectX, int srcRectY, double magnification) {
        if (x.length == 0) return;
        if (givencolor == null) {
            int col = Integer.parseInt(color);
            givencolor = new Color(col, col, col);
        }
        g.setColor(givencolor);
        if (x.length == 1 && paint_ovals) {
            g.drawOval((int) ((x[0] - srcRectX) * magnification) - 2, (int) ((y[0] - srcRectY) * magnification) - 2, 4, 4);
            return;
        }
        for (int i = 0; i < x.length - 1; i++) {
            if (paint_ovals) g.drawOval((int) ((x[i] - srcRectX) * magnification) - 2, (int) ((y[i] - srcRectX) * magnification) - 2, 4, 4);
            g.drawLine((int) ((x[i] - srcRectX) * magnification), (int) ((y[i] - srcRectY) * magnification), (int) ((x[i + 1] - srcRectX) * magnification), (int) ((y[i + 1] - srcRectY) * magnification));
        }
        if (paint_ovals) g.drawOval((int) ((x[x.length - 1] - srcRectX) * magnification), (int) ((y[x.length - 1] - srcRectY) * magnification), 4, 4);
        g.drawLine((int) ((x[x.length - 1] - srcRectX) * magnification), (int) ((y[x.length - 1] - srcRectY) * magnification), (int) ((x[0] - srcRectX) * magnification), (int) ((y[0] - srcRectY) * magnification));
    }

    WandCurve cloneWandCurve() {
        int[] new_x = new int[x.length];
        int[] new_y = new int[y.length];
        System.arraycopy(x, 0, new_x, 0, x.length);
        System.arraycopy(y, 0, new_y, 0, y.length);
        return new WandCurve(name + "", group + "", color + "", slice + 0, supergroupname + "", supergroupcolor + "", new_x, new_y);
    }

    boolean isDone() {
        if (x.length > 0) return true;
        return false;
    }

    void setXArray(int[] XArray) {
        x = XArray;
    }

    void setYArray(int[] YArray) {
        y = YArray;
    }

    int[] getXCopy() {
        int[] new_x = new int[x.length];
        System.arraycopy(x, 0, new_x, 0, x.length);
        return new_x;
    }

    int[] getYCopy() {
        int[] new_y = new int[y.length];
        System.arraycopy(y, 0, new_y, 0, y.length);
        return new_y;
    }

    public void setSlice(int sl) {
        slice = sl;
    }

    public void setName(String new_name) {
        name = new_name;
    }

    public void setGroup(String new_group) {
        group = new_group;
    }

    public void setColor(String new_color) {
        color = new_color;
    }

    public void setSuperGroup(String new_supergroup) {
        supergroupname = new_supergroup;
    }

    public void setSuperColor(String new_supercolor) {
        supergroupcolor = new_supercolor;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getColor() {
        return color;
    }

    public int getSlice() {
        return slice;
    }

    public String getSuperGroup() {
        return supergroupname;
    }

    public String getSuperColor() {
        return supergroupcolor;
    }

    public int getSize() {
        return x.length;
    }

    public boolean hasMoreThanZeroPoints() {
        boolean check = true;
        if (x.length == 0) check = false;
        return check;
    }

    public double[][] getPerimeterXY(double[] a) {
        if (a[0] == 1.0) {
            return SmoothIt.smoothTeeth(x, y, (int) a[1]);
        } else {
            double[][] points = new double[2][x.length];
            for (int i = 0; i < points[0].length; i++) {
                points[0][i] = x[i];
                points[1][i] = y[i];
            }
            return points;
        }
    }

    Point[] getWandCurvePoints() {
        Point[] points = new Point[x.length];
        for (int i = 0; i < x.length; i++) {
            points[i] = new Point(x[i], y[i]);
        }
        return points;
    }

    String wandCurveToDataString() {
        StringBuffer data = new StringBuffer();
        String l = "\n";
        data.append("type=wandcurve").append(l).append("name=").append(name).append(l).append("group=").append(group).append(l).append("color=").append(color).append(l).append("supergroup=").append(supergroupname).append(l).append("supercolor=").append(supergroupcolor).append(l).append("in slice=" + slice).append(l);
        for (int i = 0; i < x.length; i++) {
            data.append("x=" + x[i]).append(l).append("y=" + y[i]).append(l);
        }
        return data.toString();
    }
}
