import ij.IJ;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;

class Ball implements Curve {

    int[] all_x;

    int[] all_y;

    double[] all_radii;

    String name;

    String group;

    String color;

    String supergroupname;

    String supergroupcolor;

    int slice;

    Ball(String name, String group, String color, int slice, String supergroupname, String supergroupcolor) {
        all_x = new int[0];
        all_y = new int[0];
        all_radii = new double[0];
        this.name = name;
        this.group = group;
        this.color = color;
        this.supergroupname = supergroupname;
        this.supergroupcolor = supergroupcolor;
        this.slice = slice;
    }

    Ball(String name, String group, String color, int slice, String supergroupname, String supergroupcolor, int[] all_x, int[] all_y, double[] all_radii) {
        this.name = name;
        this.group = group;
        this.color = color;
        this.supergroupname = supergroupname;
        this.supergroupcolor = supergroupcolor;
        this.all_x = all_x;
        this.all_y = all_y;
        this.all_radii = all_radii;
        this.slice = slice;
    }

    void mousePressed(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        try {
            ic.dragged = findBallInBallField(ic.x_p, ic.y_p);
        } catch (Exception ebp) {
            IJ.write("error at mousePressed at Ball at findpointinballfield: " + ebp);
        }
        if (ic.dragged != -1 && e.isControlDown()) {
            removeBall(ic.dragged);
            ic.post_action = "deleted";
            ic.repaint();
            return;
        } else if (ic.dragged == -1 && !e.isControlDown()) {
            ic.dragged = addBall(ic.x_p, ic.y_p, Double.parseDouble(ic.a3de.csw.pipepointwidth.getText()));
            ic.post_action = "added";
            ic.repaint();
            return;
        }
    }

    void mouseDragged(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        if (ic.dragged != -1) {
            if (!e.isShiftDown()) {
                dragBall(ic.x_d, ic.y_d, ic.dragged);
                ic.post_action = "Dragged";
                ic.repaint();
                return;
            } else {
                all_radii[ic.dragged] = Math.sqrt((ic.x_d - ic.x_p) * (ic.x_d - ic.x_p) + (ic.y_d - ic.y_p) * (ic.y_d - ic.y_p));
                all_radii[ic.dragged] = Math.round(all_radii[ic.dragged] * 100) / 100.0;
                ic.a3de.csw.pipepointwidth.setText(all_radii[ic.dragged] + "");
                ic.post_action = "Resized";
                ic.repaint();
            }
        }
    }

    void mouseReleased(A_3D_editing.CustomCanvas ic, MouseEvent e) {
        ic.a3de.v_hist_ball.addElement(cloneBall());
        ic.a3de.v_hist_ball_namelist.addElement(ic.post_action);
        ic.a3de.csw.setHistory(ic.a3de.v_hist_ball, ic.a3de.v_hist_ball_namelist);
    }

    int addBall(int x, int y, double radius) {
        int npoints = all_x.length;
        int[] temp_all_x = new int[npoints + 1];
        int[] temp_all_y = new int[npoints + 1];
        double[] temp_all_radii = new double[npoints + 1];
        System.arraycopy(all_x, 0, temp_all_x, 0, npoints);
        System.arraycopy(all_y, 0, temp_all_y, 0, npoints);
        System.arraycopy(all_radii, 0, temp_all_radii, 0, npoints);
        temp_all_x[npoints] = x;
        temp_all_y[npoints] = y;
        temp_all_radii[npoints] = radius;
        all_x = temp_all_x;
        all_y = temp_all_y;
        all_radii = temp_all_radii;
        return npoints;
    }

    void removeBall(int index) {
        int npoints = all_x.length;
        int[] temp_all_x = new int[npoints - 1];
        int[] temp_all_y = new int[npoints - 1];
        double[] temp_all_radii = new double[npoints - 1];
        for (int i = 0; i < index; i++) {
            temp_all_x[i] = all_x[i];
            temp_all_y[i] = all_y[i];
            temp_all_radii[i] = all_radii[i];
        }
        for (int j = index; j < npoints - 1; j++) {
            temp_all_x[j] = all_x[j + 1];
            temp_all_y[j] = all_y[j + 1];
            temp_all_radii[j] = all_radii[j + 1];
        }
        all_x = temp_all_x;
        all_y = temp_all_y;
        all_radii = temp_all_radii;
    }

    int findBallInBallField(int clicked_x, int clicked_y) {
        int index = -1;
        for (int i = 0; i < all_x.length; i++) {
            if ((Math.abs(clicked_x - all_x[i]) + Math.abs(clicked_y - all_y[i])) <= 6) {
                index = i;
                break;
            }
        }
        return index;
    }

    void dragBall(int dragged_x, int dragged_y, int index) {
        all_x[index] = dragged_x;
        all_y[index] = dragged_y;
    }

    void paintBallField(Graphics g, boolean paint_ovals, Color givencolor, int srcRectX, int srcRectY, double magnification) {
        if (givencolor == null) {
            int col = Integer.parseInt(color);
            givencolor = new Color(col, col, col);
        }
        g.setColor(givencolor);
        int screen_radiusX = 0;
        int screen_radiusY = 0;
        int screen_X = 0;
        int screen_Y = 0;
        for (int i = 0; i < all_x.length; i++) {
            screen_radiusX = (int) ((all_radii[i]) * magnification);
            screen_radiusY = (int) ((all_radii[i]) * magnification);
            screen_X = (int) ((all_x[i] - srcRectX) * magnification);
            screen_Y = (int) ((all_y[i] - srcRectY) * magnification);
            g.drawOval(screen_X - screen_radiusX, screen_Y - screen_radiusY, screen_radiusX * 2, screen_radiusY * 2);
            if (paint_ovals) g.drawOval(screen_X - 3, screen_Y - 3, 6, 6);
        }
    }

    Ball cloneBall() {
        int[] new_all_x = new int[all_x.length];
        System.arraycopy(all_x, 0, new_all_x, 0, all_x.length);
        int[] new_all_y = new int[all_y.length];
        System.arraycopy(all_y, 0, new_all_y, 0, all_y.length);
        double[] new_all_radii = new double[all_radii.length];
        System.arraycopy(all_radii, 0, new_all_radii, 0, all_radii.length);
        return new Ball(name + "", group + "", color + "", slice + 0, supergroupname + "", supergroupcolor + "", new_all_x, new_all_y, new_all_radii);
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

    String ballToDataString() {
        String data = "";
        String l = "\n";
        data += "type=ball" + l;
        data += "name=" + name + l;
        data += "group=" + group + l;
        data += "color=" + color + l;
        data += "supergroup=" + supergroupname + l;
        data += "supercolor=" + supergroupcolor + l;
        data += "in slice=" + slice + l;
        for (int i = 0; i < all_x.length; i++) {
            data += "x" + all_x[i] + l;
            data += "y" + all_y[i] + l;
            data += "r" + all_radii[i] + l;
        }
        return data;
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

    double[][][][] makeDXFBall(double depth, double[][][] globe) {
        double[][][][] b = new double[all_x.length][globe.length][globe[0].length][3];
        for (int i = 0; i < all_x.length; i++) {
            for (int j = 0; j < globe.length; j++) {
                for (int k = 0; k < globe[0].length; k++) {
                    b[i][j][k][0] = globe[j][k][0] * all_radii[i] + all_x[i];
                    b[i][j][k][1] = globe[j][k][1] * all_radii[i] + all_y[i];
                    b[i][j][k][2] = globe[j][k][2] * all_radii[i] + depth;
                }
            }
        }
        return b;
    }

    public boolean hasMoreThanZeroPoints() {
        if (0 == all_x.length) {
            return false;
        }
        return true;
    }

    public int getSize() {
        return all_x.length;
    }

    public double[][] getPerimeterXY(double[] a) {
        return null;
    }
}
