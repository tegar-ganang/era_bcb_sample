import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.gui.Wand;
import ij.IJ;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Point;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.util.Vector;
import java.awt.event.MouseEvent;

class Brush implements Curve {

    int[] x;

    int[] y;

    int[] temp_x = new int[0];

    int[] temp_y = new int[0];

    int[] temp_r = new int[0];

    String name;

    String group;

    String color;

    int slice;

    String supergroupname;

    String supergroupcolor;

    boolean dragging = false;

    Brush(String name, String group, String color, int slice, String supergroupname, String supergroupcolor) {
        this.name = name;
        this.group = group;
        this.slice = slice;
        this.color = color;
        this.supergroupname = supergroupname;
        this.supergroupcolor = supergroupcolor;
        x = new int[0];
        y = new int[0];
    }

    Brush(String name, String group, String color, int slice, String supergroupname, String supergroupcolor, int[] x, int[] y) {
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
        if (ic.a3de.onRealPoints) {
            ic.dragged = findPointInPerimeter(ic.x_p, ic.y_p);
            if (ic.dragged != -1) {
                if (e.isControlDown()) {
                    removePoint(ic.dragged);
                    ic.post_action = "point removed";
                }
            } else if (!e.isAltDown()) {
                ic.dragged = insertPoint(ic.x_p, ic.y_p);
                ic.post_action = "point added";
            }
        } else {
            newAddedArea(ic.x_p, ic.y_p, (int) Double.parseDouble(ic.a3de.csw.pipepointwidth.getText()));
            setDragging(false);
            ic.post_action = "painted";
        }
        ic.repaint();
        return;
    }

    void mouseDragged(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        if (ic.a3de.onRealPoints) {
            if (ic.dragged != -1) {
                dragBrushPoint(ic.x_d, ic.y_d, ic.dragged);
                ic.repaint();
                return;
            } else if (e.isAltDown()) {
                if (!ic.startMoving) {
                    ic.x_d_old = ic.x_d;
                    ic.y_d_old = ic.y_d;
                    ic.startMoving = true;
                }
                ic.post_action = "Moved";
                moveBrushCurve(ic.x_d_old, ic.y_d_old, ic.x_d, ic.y_d);
                ic.x_d_old = ic.x_d;
                ic.y_d_old = ic.y_d;
                ic.repaint();
                return;
            }
        } else {
            if (ic.x_d_old != ic.x_d && ic.y_d_old != ic.y_d) {
                newAddedArea(ic.x_d, ic.y_d, (int) Double.parseDouble(ic.a3de.csw.pipepointwidth.getText()));
                setDragging(true);
                ic.x_d_old = ic.x_d;
                ic.y_d_old = ic.y_d;
                ic.post_action = "painted";
                ic.x_moving = e.getX();
                ic.y_moving = e.getY();
                ic.radius_moving = ic.screenX((int) Double.parseDouble(ic.a3de.csw.pipepointwidth.getText()));
                ic.repaint();
                return;
            }
        }
    }

    void mouseReleased(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        setDragging(false);
        if (ic.post_action == "Moved") resetSideImage(ic.a3de.g_side, ic.a3de.img.getWidth(), ic.a3de.img.getHeight());
        ic.repaint();
        ic.a3de.v_hist_brush.addElement(cloneBrush());
        ic.a3de.v_hist_brush_namelist.addElement(ic.post_action);
        ic.a3de.csw.setHistory(ic.a3de.v_hist_brush, ic.a3de.v_hist_brush_namelist);
    }

    void newAddedArea(int added_x, int added_y, int chosen_radius) {
        int[] new_temp_x = new int[temp_x.length + 1];
        int[] new_temp_y = new int[temp_x.length + 1];
        int[] new_temp_r = new int[temp_x.length + 1];
        System.arraycopy(temp_x, 0, new_temp_x, 0, temp_x.length);
        System.arraycopy(temp_y, 0, new_temp_y, 0, temp_x.length);
        System.arraycopy(temp_r, 0, new_temp_r, 0, temp_x.length);
        new_temp_x[temp_x.length] = added_x;
        new_temp_y[temp_x.length] = added_y;
        new_temp_r[temp_x.length] = chosen_radius;
        temp_x = new_temp_x;
        temp_y = new_temp_y;
        temp_r = new_temp_r;
    }

    void paintBrush(Graphics g, boolean paint_ovals, Color givencolor, Image side_image, Graphics g_side, boolean onRealPoints, int srcRectX, int srcRectY, double magnification) {
        try {
            if (givencolor == null) {
                int col = Integer.parseInt(color);
                givencolor = new Color(col, col, col);
            }
            g.setColor(givencolor);
            if (dragging) {
                fillPerimeterWithMagnification(g, srcRectX, srcRectY, magnification);
                for (int i = 0; i < temp_x.length; i++) {
                    g.fillOval((int) ((temp_x[i] - temp_r[i] / 2 - srcRectX) * magnification), (int) ((temp_y[i] - temp_r[i] / 2 - srcRectY) * magnification), (int) ((temp_r[i] - srcRectX) * magnification), (int) ((temp_r[i] - srcRectY) * magnification));
                    g_side.fillOval(temp_x[i] - temp_r[i] / 2, temp_y[i] - temp_r[i] / 2, temp_r[i], temp_r[i]);
                }
            } else {
                for (int i = 0; i < temp_x.length; i++) {
                    g_side.fillOval(temp_x[i] - temp_r[i] / 2, temp_y[i] - temp_r[i] / 2, temp_r[i], temp_r[i]);
                }
                if (onRealPoints) fillPerimeter(g_side);
                collectPerimeterPoints(side_image);
                if (x.length > 0) drawPerimeter(g, paint_ovals, srcRectX, srcRectY, magnification);
                temp_x = new int[0];
                temp_y = new int[0];
                temp_r = new int[0];
            }
        } catch (Exception e) {
            IJ.write("Error ar paintBrush: " + e);
        }
    }

    void setDragging(boolean b) {
        dragging = b;
    }

    void drawPerimeter(Graphics g, boolean paint_ovals, int srcRectX, int srcRectY, double magnification) {
        for (int i = 0; i < x.length - 1; i++) {
            if (paint_ovals) g.fillOval((int) ((x[i] - srcRectX) * magnification) - 2, (int) ((y[i] - srcRectY) * magnification) - 2, 4, 4);
            g.drawLine((int) ((x[i] - srcRectX) * magnification), (int) ((y[i] - srcRectY) * magnification), (int) ((x[i + 1] - srcRectX) * magnification), (int) ((y[i + 1] - srcRectY) * magnification));
        }
        g.drawLine((int) ((x[0] - srcRectX) * magnification), (int) ((y[0] - srcRectY) * magnification), (int) ((x[x.length - 1] - srcRectX) * magnification), (int) ((y[y.length - 1] - srcRectY) * magnification));
    }

    void fillPerimeterWithMagnification(Graphics g, int srcRectX, int srcRectY, double magnification) {
        int[] screen_x = new int[x.length];
        int[] screen_y = new int[y.length];
        for (int i = 0; i < x.length; i++) {
            screen_x[i] = (int) ((x[i] - srcRectX) * magnification);
            screen_y[i] = (int) ((y[i] - srcRectY) * magnification);
        }
        g.fillPolygon(screen_x, screen_y, x.length);
    }

    void fillPerimeter(Graphics g) {
        g.fillPolygon(x, y, x.length);
    }

    void collectPerimeterPoints(Image side_image) {
        try {
            if (temp_x.length == 0) return;
            ImagePlus side_plus = new ImagePlus("", side_image);
            ImageProcessor ipr = side_plus.getProcessor();
            Wand w = new Wand(ipr);
            double t1 = ipr.getMinThreshold();
            if (t1 == ipr.NO_THRESHOLD) w.autoOutline(temp_x[0], temp_y[0]); else w.autoOutline(temp_x[0], temp_y[0], (int) t1, (int) ipr.getMaxThreshold());
            int[] new_x = new int[w.npoints];
            int[] new_y = new int[w.npoints];
            System.arraycopy(w.xpoints, 0, new_x, 0, w.npoints);
            System.arraycopy(w.ypoints, 0, new_y, 0, w.npoints);
            x = new_x;
            y = new_y;
        } catch (Exception e) {
            IJ.write("Error ar collectPerimeterPoints: " + e + "\n");
        }
    }

    void resetSideImage(Graphics g_side, int width, int height) {
        g_side.setColor(Color.black);
        g_side.fillRect(0, 0, width, height);
        g_side.setColor(Color.white);
        fillPerimeter(g_side);
    }

    Brush cloneBrush() {
        int[] new_x = new int[x.length];
        int[] new_y = new int[y.length];
        System.arraycopy(x, 0, new_x, 0, x.length);
        System.arraycopy(y, 0, new_y, 0, y.length);
        return new Brush(name + "", group + "", color + "", slice + 0, supergroupname + "", supergroupcolor + "", new_x, new_y);
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

    int findPointInPerimeter(int clicked_x, int clicked_y) {
        int index = -1;
        for (int i = 0; i < x.length; i++) {
            if ((Math.abs(clicked_x - x[i]) + Math.abs(clicked_y - y[i])) <= 2) {
                index = i;
                break;
            }
        }
        return index;
    }

    void dragBrushPoint(int dragged_x, int dragged_y, int index) {
        x[index] = dragged_x;
        y[index] = dragged_y;
    }

    void moveBrushCurve(int x_dragged_old, int y_dragged_old, int x_dragged, int y_dragged) {
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
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
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

    public boolean hasMoreThanZeroPoints() {
        boolean check = true;
        if (x.length == 0) check = false;
        return check;
    }

    public int getSize() {
        return x.length;
    }

    Point[] getPerimeterPoints() {
        smoothPerimeter_UseAveragePoints(0);
        Point[] points = new Point[x.length];
        for (int i = 0; i < x.length; i++) {
            points[i] = new Point(x[i], y[i]);
        }
        return points;
    }

    void smoothPerimeter_EliminateMiddlePoints() {
        int[] sx = new int[x.length];
        int[] sy = new int[x.length];
        int max = (x.length % 2 == 0) ? x.length - 1 : x.length - 2;
        int f_x, f_y, count = 0;
        for (int i = 1; i < max; i += 2) {
            f_x = x[i - 1] - x[i] + x[i + 1] - x[i];
            f_y = y[i - 1] - y[i] + y[i + 1] - y[i];
            f_x = (f_x < 0) ? -f_x : f_x;
            f_y = (f_y < 0) ? -f_y : f_y;
            sx[count] = x[i - 1];
            sy[count] = y[i - 1];
            count++;
            if ((f_x < 2 || f_y < 2) && (f_x != 0 || f_y != 0) && (f_x != 2 || f_y != 2)) {
            } else {
                sx[count] = x[i];
                sy[count] = y[i];
                count++;
            }
        }
        x = new int[count];
        y = new int[count];
        System.arraycopy(sx, 0, x, 0, count);
        System.arraycopy(sy, 0, y, 0, count);
    }

    void smoothPerimeter_UseAveragePoints(int preserve) {
        int[] sx = new int[x.length];
        int[] sy = new int[x.length];
        int max = (x.length % 2 == 0) ? x.length - 1 : x.length - 2;
        int middle_x = 0, middle_y = 0, count = 0;
        int dif = 1;
        if (preserve == 1) {
            for (int i = 0; i < max; i += 2) {
                sx[i] = x[i];
                sy[i] = y[i];
            }
            dif = 2;
            count = 1;
        } else if (preserve == 2) {
            for (int i = 1; i < max - 1; i += 2) {
                sx[i] = x[i];
                sy[i] = y[i];
            }
            dif = 2;
            count = 0;
        }
        for (int i = 0; i < max; i++) {
            sx[count] = (int) ((x[i] + x[i + 1]) / 2);
            sy[count] = (int) ((y[i] + y[i + 1]) / 2);
            count += dif;
        }
        if (preserve == 2) count++;
        System.arraycopy(sx, 0, x, 0, count);
        System.arraycopy(sy, 0, y, 0, count);
        for (int k = 0; k < count; k++) {
            IJ.write("saved x,y: " + sx[k] + " , " + sy[k]);
        }
    }

    Point2D.Double[] getPerimeterPoints_AveragePlusAdvance(int n_average, int n_advance) {
        if (n_advance == 0) n_advance = 1;
        if (n_average == 0) n_average = 1;
        double[] sx = new double[x.length];
        double[] sy = new double[x.length];
        int max = (int) (x.length / n_advance) * n_advance;
        int last_set = x.length - max;
        if (last_set < n_average) max -= n_average; else max -= last_set;
        int count = 0;
        for (int i = 0; i < max; i += n_advance) {
            sx[count] = x[i];
            sy[count] = y[i];
            for (int j = 1; j < n_average; j++) {
                sx[count] += x[i + j];
                sy[count] += y[i + j];
            }
            sx[count] = sx[count] / n_average;
            sy[count] = sy[count] / n_average;
            count++;
        }
        Point2D.Double[] points = new Point2D.Double[count];
        for (int k = 0; k < count; k++) {
            points[k] = new Point2D.Double(sx[k], sy[k]);
        }
        return points;
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

    String brushToDataString() {
        StringBuffer data = new StringBuffer();
        String l = "\n";
        data.append("type=brush").append(l).append("name=").append(name).append(l).append("group=").append(group).append(l).append("color=").append(color).append(l).append("supergroup=").append(supergroupname).append(l).append("supercolor=").append(supergroupcolor).append(l).append("in slice=" + slice).append(l);
        for (int i = 0; i < x.length; i++) {
            data.append("x=" + x[i]).append(l).append("y=" + y[i]).append(l);
        }
        return data.toString();
    }
}
