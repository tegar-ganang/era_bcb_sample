package cube;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public final class rubik extends Panel {

    static int zoom = 1;

    int i;

    int j;

    int k;

    int n;

    int o;

    int p;

    int q;

    int lastX;

    int lastY;

    int dx;

    int dy;

    int[] rectX;

    int[] rectY;

    Color[] colList;

    Color bgcolor;

    final double[] sideVec = { 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0 };

    final double[] corners = { -1.0, -1.0, -1.0, 1.0, -1.0, -1.0, 1.0, 1.0, -1.0, -1.0, 1.0, -1.0, -1.0, -1.0, 1.0, 1.0, -1.0, 1.0, 1.0, 1.0, 1.0, -1.0, 1.0, 1.0 };

    double[] topCorners;

    double[] botCorners;

    final int[] sides = { 4, 5, 6, 7, 3, 2, 1, 0, 0, 1, 5, 4, 1, 2, 6, 5, 2, 3, 7, 6, 0, 4, 7, 3 };

    final int[] nextSide = { 2, 3, 4, 5, 4, 3, 2, 5, 1, 3, 0, 5, 1, 4, 0, 2, 1, 5, 0, 3, 2, 0, 4, 1 };

    final int[] mainBlocks = { 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3 };

    final int[] twistDir = { -1, 1, -1, 1, -1, 1, -1, 1, 1, 1, 1, 1, 1, -1, 1, -1, 1, 1, 1, 1, -1, 1, -1, 1 };

    final int[] colDir = { -1, -1, 1, -1, 1, -1 };

    final int[] circleOrder = { 0, 1, 2, 5, 8, 7, 6, 3 };

    int[] topBlocks;

    int[] botBlocks;

    int[] sideCols;

    int sideW;

    int sideH;

    int dragReg;

    int twistSide = -1;

    int[] nearSide;

    int[] buffer;

    double[] dragCorn;

    double[] dragDir;

    double[] eye = { 0.3651, 0.1826, -0.9129 };

    double[] eX = { 0.9309, -0.0716, 0.3581 };

    double[] eY;

    double[] Teye;

    double[] TeX;

    double[] TeY;

    double[] light;

    double[] temp = { 0.0, 0.0, 0.0 };

    double[] temp2 = { 0.0, 0.0, 0.0 };

    double[] newCoord;

    double sx;

    double sy;

    double sdxh;

    double sdyh;

    double sdxv;

    double sdyv;

    double d;

    double t1;

    double t2;

    double t3;

    double t4;

    double t5;

    double t6;

    double phi;

    double phibase;

    double Cphi;

    double Sphi;

    double[] currDragDir;

    boolean naturalState = true;

    boolean twisting = false;

    boolean OKtoDrag = false;

    Math m;

    Graphics offGraphics;

    BufferedImage offImage;

    public static void main(String[] args) {
        JFrame fa = new JFrame();
        rubik cube = new rubik();
        cube.init();
        cube.setPreferredSize(new Dimension(120 * zoom, 120 * zoom));
        fa.add(cube);
        fa.pack();
        fa.setResizable(false);
        fa.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fa.setVisible(true);
        cube.shuffle();
    }

    public void init() {
        this.offImage = new BufferedImage(120, 120, BufferedImage.SCALE_SMOOTH);
        this.offGraphics = this.offImage.getGraphics();
        this.rectX = new int[4];
        this.rectY = new int[4];
        this.newCoord = new double[16];
        this.dragDir = new double[24];
        this.dragCorn = new double[96];
        this.topCorners = new double[24];
        this.botCorners = new double[24];
        this.topBlocks = new int[24];
        this.botBlocks = new int[24];
        this.buffer = new int[12];
        this.nearSide = new int[12];
        this.light = new double[3];
        this.Teye = new double[3];
        this.TeX = new double[3];
        this.TeY = new double[3];
        this.currDragDir = new double[2];
        this.eY = new double[3];
        vecProd(this.eye, 0, this.eX, 0, this.eY, 0);
        normalize(this.eY, 0);
        this.colList = new Color[120];
        for (this.i = 0; this.i < 20; this.i++) {
            this.colList[this.i] = new Color(103 + this.i * 8, 103 + this.i * 8, 103 + this.i * 8);
            this.colList[this.i + 20] = new Color(this.i * 6, this.i * 6, 84 + this.i * 9);
            this.colList[this.i + 40] = new Color(84 + this.i * 9, this.i * 5, this.i * 5);
            this.colList[this.i + 60] = new Color(this.i * 6, 84 + this.i * 9, this.i * 6);
            this.colList[this.i + 80] = new Color(84 + this.i * 9, 84 + this.i * 9, this.i * 6);
            this.colList[this.i + 100] = new Color(84 + this.i * 9, 55 + this.i * 8, this.i * 3);
        }
        this.sideCols = new int[54];
        for (this.i = 0; this.i < 54; this.i++) {
            this.sideCols[this.i] = this.i / 9;
        }
        this.bgcolor = findBGColor();
        this.repaint();
    }

    public Color findBGColor() {
        String string = "0123456789abcdef";
        int[] is = new int[6];
        String string_0_ = "808080";
        Color color;
        if (string_0_ != null && string_0_.length() == 6) {
            for (this.i = 0; this.i < 6; this.i++) {
                for (this.j = 0; this.j < 16; this.j++) {
                    if (Character.toLowerCase(string_0_.charAt(this.i)) == string.charAt(this.j)) {
                        is[this.i] = this.j;
                    }
                }
            }
            color = new Color(is[0] * 16 + is[1], is[2] * 16 + is[3], is[4] * 16 + is[5]);
        } else {
            color = Color.lightGray;
        }
        return color;
    }

    public double scalProd(double[] ds, int i, double[] ds_1_, int i_2_) {
        return (ds[i] * ds_1_[i_2_] + ds[i + 1] * ds_1_[i_2_ + 1] + ds[i + 2] * ds_1_[i_2_ + 2]);
    }

    public double vNorm(double[] ds, int i) {
        return Math.sqrt(ds[i] * ds[i] + ds[i + 1] * ds[i + 1] + ds[i + 2] * ds[i + 2]);
    }

    public double cosAng(double[] ds, int i, double[] ds_3_, int i_4_) {
        return (scalProd(ds, i, ds_3_, i_4_) / (vNorm(ds, i) * vNorm(ds_3_, i_4_)));
    }

    public void normalize(double[] ds, int i) {
        double d = vNorm(ds, i);
        ds[i] = ds[i] / d;
        ds[i + 1] = ds[i + 1] / d;
        ds[i + 2] = ds[i + 2] / d;
    }

    public void scalMult(double[] ds, int i, double d) {
        ds[i] = ds[i] * d;
        ds[i + 1] = ds[i + 1] * d;
        ds[i + 2] = ds[i + 2] * d;
    }

    public void addVec(double[] ds, int i, double[] ds_5_, int i_6_) {
        ds_5_[i_6_] += ds[i];
        ds_5_[i_6_ + 1] += ds[i + 1];
        ds_5_[i_6_ + 2] += ds[i + 2];
    }

    public void subVec(double[] ds, int i, double[] ds_7_, int i_8_) {
        ds_7_[i_8_] -= ds[i];
        ds_7_[i_8_ + 1] -= ds[i + 1];
        ds_7_[i_8_ + 2] -= ds[i + 2];
    }

    public void copyVec(double[] ds, int i, double[] ds_9_, int i_10_) {
        ds_9_[i_10_] = ds[i];
        ds_9_[i_10_ + 1] = ds[i + 1];
        ds_9_[i_10_ + 2] = ds[i + 2];
    }

    public void vecProd(double[] ds, int i, double[] ds_11_, int i_12_, double[] ds_13_, int i_14_) {
        ds_13_[i_14_] = ds[i + 1] * ds_11_[i_12_ + 2] - ds[i + 2] * ds_11_[i_12_ + 1];
        ds_13_[i_14_ + 1] = ds[i + 2] * ds_11_[i_12_] - ds[i] * ds_11_[i_12_ + 2];
        ds_13_[i_14_ + 2] = ds[i] * ds_11_[i_12_ + 1] - ds[i + 1] * ds_11_[i_12_];
    }

    public void cutUpCube() {
        for (this.i = 0; this.i < 24; this.i++) {
            this.topCorners[this.i] = this.corners[this.i];
            this.botCorners[this.i] = this.corners[this.i];
        }
        copyVec(this.sideVec, 3 * this.twistSide, this.temp, 0);
        copyVec(this.temp, 0, this.temp2, 0);
        scalMult(this.temp, 0, 1.3333);
        scalMult(this.temp2, 0, 0.6667);
        for (this.i = 0; this.i < 8; this.i++) {
            boolean bool = false;
            for (this.j = 0; this.j < 4; this.j++) {
                if (this.i == (this.sides[this.twistSide * 4 + this.j])) {
                    bool = true;
                }
            }
            if (bool) {
                subVec(this.temp2, 0, this.botCorners, this.i * 3);
            } else {
                addVec(this.temp, 0, this.topCorners, this.i * 3);
            }
        }
        for (this.i = 0; this.i < 24; this.i++) {
            this.topBlocks[this.i] = this.mainBlocks[this.i];
            this.botBlocks[this.i] = this.mainBlocks[this.i];
        }
        for (this.i = 0; this.i < 6; this.i++) {
            if (this.i == this.twistSide) {
                this.botBlocks[this.i * 4 + 1] = 0;
                this.botBlocks[this.i * 4 + 3] = 0;
            } else {
                this.k = -1;
                for (this.j = 0; this.j < 4; this.j++) {
                    if ((this.nextSide[this.i * 4 + this.j]) == this.twistSide) {
                        this.k = this.j;
                    }
                }
                switch(this.k) {
                    case 0:
                        this.topBlocks[this.i * 4 + 3] = 1;
                        this.botBlocks[this.i * 4 + 2] = 1;
                        break;
                    case 1:
                        this.topBlocks[this.i * 4] = 2;
                        this.botBlocks[this.i * 4 + 1] = 2;
                        break;
                    case 2:
                        this.topBlocks[this.i * 4 + 2] = 2;
                        this.botBlocks[this.i * 4 + 3] = 2;
                        break;
                    case 3:
                        this.topBlocks[this.i * 4 + 1] = 1;
                        this.botBlocks[this.i * 4] = 1;
                        break;
                    case -1:
                        this.topBlocks[this.i * 4 + 1] = 0;
                        this.topBlocks[this.i * 4 + 3] = 0;
                        break;
                }
            }
        }
    }

    public void shuffle() {
        this.twisting = false;
        this.naturalState = true;
        for (this.i = 0; this.i < 20; this.i++) {
            colorTwist((int) (Math.random() * 6.0), (int) (Math.random() * 3.0 + 1.0));
        }
        this.repaint();
    }

    public boolean keyDown(Event event, int i) {
        if (i == 114) {
            this.twisting = false;
            this.naturalState = true;
            for (this.i = 0; this.i < 54; this.i++) {
                this.sideCols[this.i] = this.i / 9;
            }
            this.repaint();
        } else if (i == 115) {
            this.twisting = false;
            this.naturalState = true;
            for (this.i = 0; this.i < 20; this.i++) {
                colorTwist((int) (Math.random() * 6.0), (int) (Math.random() * 3.0 + 1.0));
            }
            this.repaint();
        }
        return false;
    }

    public boolean mouseDrag(Event event, int i, int i_15_) {
        i /= zoom;
        i_15_ /= zoom;
        if (!this.twisting && this.OKtoDrag) {
            this.OKtoDrag = false;
            boolean bool = false;
            for (this.i = 0; this.i < this.dragReg; this.i++) {
                double d = (this.dragCorn[this.i * 8 + 1] - this.dragCorn[this.i * 8]);
                double d_16_ = (this.dragCorn[this.i * 8 + 5] - this.dragCorn[this.i * 8 + 4]);
                double d_17_ = (this.dragCorn[this.i * 8 + 3] - this.dragCorn[this.i * 8]);
                double d_18_ = (this.dragCorn[this.i * 8 + 7] - this.dragCorn[this.i * 8 + 4]);
                double d_19_ = ((d_18_ * ((double) this.lastX - (this.dragCorn[this.i * 8])) - d_17_ * ((double) this.lastY - (this.dragCorn[this.i * 8 + 4]))) / (d * d_18_ - d_17_ * d_16_));
                double d_20_ = ((-d_16_ * ((double) this.lastX - (this.dragCorn[this.i * 8])) + d * ((double) this.lastY - (this.dragCorn[this.i * 8 + 4]))) / (d * d_18_ - d_17_ * d_16_));
                if (d_19_ > 0.0 && d_19_ < 1.0 && d_20_ > 0.0 && d_20_ < 1.0) {
                    this.currDragDir[0] = this.dragDir[this.i * 2];
                    this.currDragDir[1] = this.dragDir[this.i * 2 + 1];
                    this.d = ((this.currDragDir[0] * (double) (i - this.lastX)) + (this.currDragDir[1] * (double) (i_15_ - this.lastY)));
                    this.d = (this.d * this.d / (((this.currDragDir[0] * this.currDragDir[0]) + (this.currDragDir[1] * this.currDragDir[1])) * (double) (((i - this.lastX) * (i - this.lastX)) + ((i_15_ - this.lastY) * (i_15_ - this.lastY)))));
                    if (this.d > 0.6) {
                        bool = true;
                        this.twistSide = this.nearSide[this.i];
                        this.i = 100;
                    }
                }
            }
            if (bool) {
                if (this.naturalState) {
                    cutUpCube();
                    this.naturalState = false;
                }
                this.twisting = true;
                this.phi = (0.02 * ((this.currDragDir[0] * (double) (i - this.lastX)) + (this.currDragDir[1] * (double) (i_15_ - this.lastY))) / Math.sqrt((this.currDragDir[0] * this.currDragDir[0]) + (this.currDragDir[1] * this.currDragDir[1])));
                this.repaint();
                return false;
            }
        }
        this.OKtoDrag = false;
        if (!this.twisting) {
            this.dx = this.lastX - i;
            copyVec(this.eX, 0, this.temp, 0);
            scalMult(this.temp, 0, (double) this.dx * 0.016);
            addVec(this.temp, 0, this.eye, 0);
            vecProd(this.eY, 0, this.eye, 0, this.eX, 0);
            normalize(this.eX, 0);
            normalize(this.eye, 0);
            this.dy = i_15_ - this.lastY;
            copyVec(this.eY, 0, this.temp, 0);
            scalMult(this.temp, 0, (double) this.dy * 0.016);
            addVec(this.temp, 0, this.eye, 0);
            vecProd(this.eye, 0, this.eX, 0, this.eY, 0);
            normalize(this.eY, 0);
            normalize(this.eye, 0);
            this.lastX = i;
            this.lastY = i_15_;
            this.repaint();
        } else {
            this.phi = (0.02 * ((this.currDragDir[0] * (double) (i - this.lastX)) + (this.currDragDir[1] * (double) (i_15_ - this.lastY))) / Math.sqrt((this.currDragDir[0] * this.currDragDir[0]) + (this.currDragDir[1] * this.currDragDir[1])));
            this.repaint();
        }
        return false;
    }

    public boolean mouseDown(Event event, int i, int i_21_) {
        i /= zoom;
        i_21_ /= zoom;
        this.lastX = i;
        this.lastY = i_21_;
        this.OKtoDrag = true;
        return false;
    }

    public boolean mouseUp(Event event, int i, int i_22_) {
        i /= zoom;
        i_22_ /= zoom;
        if (this.twisting) {
            this.twisting = false;
            this.phibase += this.phi;
            this.phi = 0.0;
            double d;
            for (d = this.phibase; d < 0.0; d += 125.662) {
            }
            int i_23_ = (int) (d * 3.183);
            if (i_23_ % 5 == 0 || i_23_ % 5 == 4) {
                i_23_ = (i_23_ + 1) / 5 % 4;
                if (this.colDir[this.twistSide] < 0) {
                    i_23_ = (4 - i_23_) % 4;
                }
                this.phibase = 0.0;
                this.naturalState = true;
                colorTwist(this.twistSide, i_23_);
            }
            this.repaint();
        }
        return false;
    }

    public void colorTwist(int i, int i_24_) {
        int i_25_ = 0;
        int i_26_ = i_24_ * 2;
        for (int i_27_ = 0; i_27_ < 8; i_27_++) {
            this.buffer[i_26_] = (this.sideCols[i * 9 + this.circleOrder[i_27_]]);
            i_26_ = (i_26_ + 1) % 8;
        }
        for (int i_28_ = 0; i_28_ < 8; i_28_++) {
            this.sideCols[i * 9 + this.circleOrder[i_28_]] = this.buffer[i_28_];
        }
        i_26_ = i_24_ * 3;
        for (int i_29_ = 0; i_29_ < 4; i_29_++) {
            for (int i_30_ = 0; i_30_ < 4; i_30_++) {
                if ((this.nextSide[this.nextSide[i * 4 + i_29_] * 4 + i_30_]) == i) {
                    i_25_ = i_30_;
                }
            }
            for (int i_31_ = 0; i_31_ < 3; i_31_++) {
                switch(i_25_) {
                    case 0:
                        this.buffer[i_26_] = (this.sideCols[(this.nextSide[i * 4 + i_29_] * 9 + i_31_)]);
                        break;
                    case 1:
                        this.buffer[i_26_] = (this.sideCols[(this.nextSide[i * 4 + i_29_] * 9 + 2 + 3 * i_31_)]);
                        break;
                    case 2:
                        this.buffer[i_26_] = (this.sideCols[(this.nextSide[i * 4 + i_29_] * 9 + 8 - i_31_)]);
                        break;
                    case 3:
                        this.buffer[i_26_] = (this.sideCols[(this.nextSide[i * 4 + i_29_] * 9 + 6 - 3 * i_31_)]);
                        break;
                }
                i_26_ = (i_26_ + 1) % 12;
            }
        }
        i_26_ = 0;
        for (int i_32_ = 0; i_32_ < 4; i_32_++) {
            for (int i_33_ = 0; i_33_ < 4; i_33_++) {
                if ((this.nextSide[this.nextSide[i * 4 + i_32_] * 4 + i_33_]) == i) {
                    i_25_ = i_33_;
                }
            }
            for (int i_34_ = 0; i_34_ < 3; i_34_++) {
                switch(i_25_) {
                    case 0:
                        this.sideCols[this.nextSide[i * 4 + i_32_] * 9 + i_34_] = this.buffer[i_26_];
                        break;
                    case 1:
                        this.sideCols[(this.nextSide[i * 4 + i_32_] * 9 + 2 + 3 * i_34_)] = this.buffer[i_26_];
                        break;
                    case 2:
                        this.sideCols[(this.nextSide[i * 4 + i_32_]) * 9 + 8 - i_34_] = this.buffer[i_26_];
                        break;
                    case 3:
                        this.sideCols[(this.nextSide[i * 4 + i_32_] * 9 + 6 - 3 * i_34_)] = this.buffer[i_26_];
                        break;
                }
                i_26_++;
            }
        }
    }

    public void paint(Graphics graphics) {
        this.dragReg = 0;
        this.offGraphics.setColor(this.bgcolor);
        this.offGraphics.fillRect(0, 0, 120, 120);
        this.offGraphics.setColor(Color.white);
        this.offGraphics.drawString("JavaCPC Cube", 14, 14);
        if (this.naturalState) {
            fixBlock(this.eye, this.eX, this.eY, this.corners, this.mainBlocks, 0);
        } else {
            copyVec(this.eye, 0, this.Teye, 0);
            copyVec(this.eX, 0, this.TeX, 0);
            this.Cphi = Math.cos(this.phi + this.phibase);
            this.Sphi = -Math.sin(this.phi + this.phibase);
            switch(this.twistSide) {
                case 0:
                    this.Teye[0] = (this.Cphi * this.eye[0] + this.Sphi * this.eye[1]);
                    this.TeX[0] = (this.Cphi * this.eX[0] + this.Sphi * this.eX[1]);
                    this.Teye[1] = (-this.Sphi * this.eye[0] + this.Cphi * this.eye[1]);
                    this.TeX[1] = (-this.Sphi * this.eX[0] + this.Cphi * this.eX[1]);
                    break;
                case 1:
                    this.Teye[0] = (this.Cphi * this.eye[0] - this.Sphi * this.eye[1]);
                    this.TeX[0] = (this.Cphi * this.eX[0] - this.Sphi * this.eX[1]);
                    this.Teye[1] = (this.Sphi * this.eye[0] + this.Cphi * this.eye[1]);
                    this.TeX[1] = (this.Sphi * this.eX[0] + this.Cphi * this.eX[1]);
                    break;
                case 2:
                    this.Teye[0] = (this.Cphi * this.eye[0] - this.Sphi * this.eye[2]);
                    this.TeX[0] = (this.Cphi * this.eX[0] - this.Sphi * this.eX[2]);
                    this.Teye[2] = (this.Sphi * this.eye[0] + this.Cphi * this.eye[2]);
                    this.TeX[2] = (this.Sphi * this.eX[0] + this.Cphi * this.eX[2]);
                    break;
                case 3:
                    this.Teye[1] = (this.Cphi * this.eye[1] + this.Sphi * this.eye[2]);
                    this.TeX[1] = (this.Cphi * this.eX[1] + this.Sphi * this.eX[2]);
                    this.Teye[2] = (-this.Sphi * this.eye[1] + this.Cphi * this.eye[2]);
                    this.TeX[2] = (-this.Sphi * this.eX[1] + this.Cphi * this.eX[2]);
                    break;
                case 4:
                    this.Teye[0] = (this.Cphi * this.eye[0] + this.Sphi * this.eye[2]);
                    this.TeX[0] = (this.Cphi * this.eX[0] + this.Sphi * this.eX[2]);
                    this.Teye[2] = (-this.Sphi * this.eye[0] + this.Cphi * this.eye[2]);
                    this.TeX[2] = (-this.Sphi * this.eX[0] + this.Cphi * this.eX[2]);
                    break;
                case 5:
                    this.Teye[1] = (this.Cphi * this.eye[1] - this.Sphi * this.eye[2]);
                    this.TeX[1] = (this.Cphi * this.eX[1] - this.Sphi * this.eX[2]);
                    this.Teye[2] = (this.Sphi * this.eye[1] + this.Cphi * this.eye[2]);
                    this.TeX[2] = (this.Sphi * this.eX[1] + this.Cphi * this.eX[2]);
                    break;
            }
            vecProd(this.Teye, 0, this.TeX, 0, this.TeY, 0);
            if (scalProd(this.eye, 0, this.sideVec, this.twistSide * 3) < 0.0) {
                fixBlock(this.Teye, this.TeX, this.TeY, this.topCorners, this.topBlocks, 2);
                fixBlock(this.eye, this.eX, this.eY, this.botCorners, this.botBlocks, 1);
            } else {
                fixBlock(this.eye, this.eX, this.eY, this.botCorners, this.botBlocks, 1);
                fixBlock(this.Teye, this.TeX, this.TeY, this.topCorners, this.topBlocks, 2);
            }
        }
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics = g2;
        graphics.drawImage(this.offImage, 0, 0, 120 * zoom, 120 * zoom, this);
    }

    public void update(Graphics graphics) {
        paint(graphics);
    }

    public void fixBlock(double[] ds, double[] ds_35_, double[] ds_36_, double[] ds_37_, int[] is, int i) {
        copyVec(ds, 0, this.light, 0);
        scalMult(this.light, 0, -3.0);
        addVec(ds_35_, 0, this.light, 0);
        subVec(ds_36_, 0, this.light, 0);
        for (this.i = 0; this.i < 8; this.i++) {
            this.newCoord[this.i * 2] = 60.0 + 35.1 * scalProd(ds_37_, this.i * 3, ds_35_, 0);
            this.newCoord[this.i * 2 + 1] = 60.0 - 35.1 * scalProd(ds_37_, this.i * 3, ds_36_, 0);
        }
        for (this.i = 0; this.i < 6; this.i++) {
            if (scalProd(ds, 0, this.sideVec, 3 * this.i) > 0.0010) {
                this.k = (int) (9.6 * (1.0 - cosAng(this.light, 0, this.sideVec, 3 * this.i)));
                this.offGraphics.setColor(Color.black);
                for (this.j = 0; this.j < 4; this.j++) {
                    this.rectX[this.j] = (int) (this.newCoord[2 * (this.sides[(this.i * 4 + this.j)])]);
                    this.rectY[this.j] = (int) (this.newCoord[2 * (this.sides[(this.i * 4 + this.j)]) + 1]);
                }
                this.offGraphics.fillPolygon(this.rectX, this.rectY, 4);
                this.sideW = is[this.i * 4 + 1] - is[this.i * 4];
                this.sideH = (is[this.i * 4 + 3] - is[this.i * 4 + 2]);
                if (this.sideW > 0) {
                    this.sx = (this.newCoord[2 * this.sides[this.i * 4]]);
                    this.sy = (this.newCoord[(2 * this.sides[this.i * 4] + 1)]);
                    this.sdxh = (((this.newCoord[2 * (this.sides[this.i * 4 + 1])]) - this.sx) / (double) this.sideW);
                    this.sdxv = (((this.newCoord[2 * (this.sides[this.i * 4 + 3])]) - this.sx) / (double) this.sideH);
                    this.sdyh = (((this.newCoord[2 * (this.sides[this.i * 4 + 1]) + 1]) - this.sy) / (double) this.sideW);
                    this.sdyv = (((this.newCoord[2 * (this.sides[this.i * 4 + 3]) + 1]) - this.sy) / (double) this.sideH);
                    this.p = is[this.i * 4 + 2];
                    for (this.n = 0; this.n < this.sideH; this.n++) {
                        this.q = is[this.i * 4];
                        for (this.o = 0; this.o < this.sideW; this.o++) {
                            this.rectX[0] = (int) (this.sx + (((double) this.o + 0.1) * this.sdxh) + (((double) this.n + 0.1) * this.sdxv));
                            this.rectX[1] = (int) (this.sx + (((double) this.o + 0.9) * this.sdxh) + (((double) this.n + 0.1) * this.sdxv));
                            this.rectX[2] = (int) (this.sx + (((double) this.o + 0.9) * this.sdxh) + (((double) this.n + 0.9) * this.sdxv));
                            this.rectX[3] = (int) (this.sx + (((double) this.o + 0.1) * this.sdxh) + (((double) this.n + 0.9) * this.sdxv));
                            this.rectY[0] = (int) (this.sy + (((double) this.o + 0.1) * this.sdyh) + (((double) this.n + 0.1) * this.sdyv));
                            this.rectY[1] = (int) (this.sy + (((double) this.o + 0.9) * this.sdyh) + (((double) this.n + 0.1) * this.sdyv));
                            this.rectY[2] = (int) (this.sy + (((double) this.o + 0.9) * this.sdyh) + (((double) this.n + 0.9) * this.sdyv));
                            this.rectY[3] = (int) (this.sy + (((double) this.o + 0.1) * this.sdyh) + (((double) this.n + 0.9) * this.sdyv));
                            this.offGraphics.setColor(this.colList[(20 * (this.sideCols[(this.i * 9 + this.p * 3 + this.q)]) + this.k)]);
                            this.offGraphics.fillPolygon(this.rectX, this.rectY, 4);
                            this.q++;
                        }
                        this.p++;
                    }
                }
                switch(i) {
                    case 0:
                        this.t1 = this.sx;
                        this.t2 = this.sy;
                        this.t3 = this.sdxh;
                        this.t4 = this.sdyh;
                        this.t5 = this.sdxv;
                        this.t6 = this.sdyv;
                        for (this.j = 0; this.j < 4; this.j++) {
                            this.dragCorn[8 * this.dragReg] = this.t1;
                            this.dragCorn[8 * this.dragReg + 4] = this.t2;
                            this.dragCorn[8 * this.dragReg + 3] = this.t1 + this.t5;
                            this.dragCorn[8 * this.dragReg + 7] = this.t2 + this.t6;
                            this.t1 = this.t1 + this.t3 * 3.0;
                            this.t2 = this.t2 + this.t4 * 3.0;
                            this.dragCorn[8 * this.dragReg + 1] = this.t1;
                            this.dragCorn[8 * this.dragReg + 5] = this.t2;
                            this.dragCorn[8 * this.dragReg + 2] = this.t1 + this.t5;
                            this.dragCorn[8 * this.dragReg + 6] = this.t2 + this.t6;
                            this.dragDir[this.dragReg * 2] = (this.t3 * (double) (this.twistDir[(this.i * 4 + this.j)]));
                            this.dragDir[this.dragReg * 2 + 1] = (this.t4 * (double) (this.twistDir[(this.i * 4 + this.j)]));
                            this.d = this.t3;
                            this.t3 = this.t5;
                            this.t5 = -this.d;
                            this.d = this.t4;
                            this.t4 = this.t6;
                            this.t6 = -this.d;
                            this.nearSide[this.dragReg] = (this.nextSide[this.i * 4 + this.j]);
                            this.dragReg++;
                        }
                        break;
                    case 2:
                        if (this.i != this.twistSide && this.sideW > 0) {
                            if (this.sideW == 3) {
                                if (is[this.i * 4 + 2] == 0) {
                                    this.dragDir[(this.dragReg * 2)] = (this.sdxh * (double) (this.twistDir[this.i * 4]));
                                    this.dragDir[this.dragReg * 2 + 1] = (this.sdyh * (double) (this.twistDir[this.i * 4]));
                                } else {
                                    this.dragDir[(this.dragReg * 2)] = (-this.sdxh * (double) (this.twistDir[(this.i * 4 + 2)]));
                                    this.dragDir[this.dragReg * 2 + 1] = (-this.sdyh * (double) (this.twistDir[(this.i * 4 + 2)]));
                                }
                            } else if (is[this.i * 4] == 0) {
                                this.dragDir[this.dragReg * 2] = (-this.sdxv * (double) (this.twistDir[this.i * 4 + 3]));
                                this.dragDir[(this.dragReg * 2 + 1)] = (-this.sdyv * (double) (this.twistDir[this.i * 4 + 3]));
                            } else {
                                this.dragDir[this.dragReg * 2] = (this.sdxv * (double) (this.twistDir[this.i * 4 + 1]));
                                this.dragDir[(this.dragReg * 2 + 1)] = (this.sdyv * (double) (this.twistDir[this.i * 4 + 1]));
                            }
                            for (this.j = 0; this.j < 4; this.j++) {
                                this.dragCorn[(this.dragReg * 8 + this.j)] = (this.newCoord[2 * (this.sides[(this.i * 4 + this.j)])]);
                                this.dragCorn[(this.dragReg * 8 + 4 + this.j)] = (this.newCoord[2 * (this.sides[(this.i * 4 + this.j)]) + 1]);
                            }
                            this.nearSide[this.dragReg] = this.twistSide;
                            this.dragReg++;
                        }
                        break;
                }
            }
        }
    }
}
