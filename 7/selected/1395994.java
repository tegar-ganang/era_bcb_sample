package kfschmidt.imageoverlay;

import java.awt.geom.*;
import java.util.Vector;

public class ContourAnalyzer {

    double[][] mData;

    int mLevels;

    double mMinVal;

    double mMaxVal;

    Area[] mContours;

    public static Area[] getContours(double[][] data, int numlevels, double min, double max) {
        double[] thresholds = getThresholds(numlevels, min, max);
        Area[] ret = new Area[thresholds.length];
        for (int a = 0; a < ret.length; a++) {
            ret[a] = getAreaForThreshold(data, thresholds[a]);
        }
        return ret;
    }

    private static double getAvgValueForPixel(double[][] data, int x, int y, int resolution) {
        return 0d;
    }

    public static Area getAreaForThreshold(double[][] data, double threshold) {
        Vector vec = new Vector(500000);
        System.out.println("getAreaForThreshold(" + threshold + ")");
        Area retarea = null;
        Area tmparea = null;
        Triangle[] triangles = null;
        Point2D.Float[] vert_xys = new Point2D.Float[4];
        double[] vert_values = new double[4];
        for (int a = 0; a < 4; a++) {
            vert_xys[a] = new Point2D.Float();
        }
        GeneralPath tmppath = new GeneralPath();
        for (int y = 0; y < data[0].length - 1; y++) {
            for (int x = 0; x < data.length - 1; x++) {
                vert_values[0] = data[x][y];
                vert_values[0] = data[x + 1][y];
                vert_values[0] = data[x + 1][y + 1];
                vert_values[0] = data[x][y + 1];
                vert_xys[0].x = (float) x;
                vert_xys[0].y = (float) y;
                vert_xys[1].x = (float) (x + 1);
                vert_xys[1].y = (float) y;
                vert_xys[2].x = (float) (x + 1);
                vert_xys[2].y = (float) (y + 1);
                vert_xys[3].x = (float) x;
                vert_xys[3].y = (float) (y + 1);
                triangles = SquareMarcher.marchSquare(vert_xys, vert_values, threshold);
                if (triangles != null) {
                    for (int a = 0; a < triangles.length; a++) vec.add(triangles[a]);
                }
            }
        }
        Triangle[] tris = new Triangle[vec.size()];
        vec.copyInto(tris);
        return getAreaForTriangles(tris);
    }

    public static Area getAreaForTriangles(Triangle[] triangles) {
        GeneralPath tmppath = new GeneralPath();
        Area retarea = new Area();
        System.out.println("getAreaForTriangles()");
        if (triangles != null) {
            System.out.println("Total triangles: " + triangles.length);
            tmppath.moveTo(triangles[0].v1.x, triangles[0].v1.y);
            for (int n = 0; n < triangles.length; n++) {
                tmppath.lineTo(triangles[n].v1.x, triangles[n].v1.y);
                tmppath.lineTo(triangles[n].v2.x, triangles[n].v2.y);
                tmppath.lineTo(triangles[n].v3.x, triangles[n].v3.y);
            }
            tmppath.closePath();
            return new Area(tmppath);
        }
        return null;
    }

    public static double[] getThresholds(int levels, double min, double max) {
        double[] ret = new double[levels];
        ret[0] = min;
        ret[ret.length - 1] = max;
        for (int a = 1; a < ret.length - 1; a++) {
            ret[a] = ret[0] + (double) a * ((max - min) / (double) ret.length - 2);
        }
        return ret;
    }
}

class SquareMarcher {

    public static Point2D.Float interpolate(Point2D.Float p1, Point2D.Float p2, double val1, double val2, double threshold) {
        double weight = 0;
        Point2D.Float interpolated_pt = new Point2D.Float();
        if (val1 <= threshold) {
            weight = (threshold - val1) / (val2 - val1);
            interpolated_pt.x = (float) (p1.x + (p2.x - p1.x) * weight);
            interpolated_pt.y = (float) (p1.y + (p2.y - p1.y) * weight);
        } else {
            weight = (threshold - val2) / (val1 - val2);
            interpolated_pt.x = (float) (p2.x + (p1.x - p2.x) * weight);
            interpolated_pt.y = (float) (p2.y + (p1.y - p2.y) * weight);
        }
        return interpolated_pt;
    }

    public static Triangle[] marchSquare(Point2D.Float[] vert_xys, double[] verts, double threshold) {
        Triangle[] ret = null;
        boolean a = verts[0] <= threshold ? false : true;
        boolean b = verts[1] <= threshold ? false : true;
        boolean c = verts[2] <= threshold ? false : true;
        boolean d = verts[3] <= threshold ? false : true;
        if (!a && !b && !c && !d) {
        } else if (!a && !b && !c && d) {
            ret = new Triangle[1];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[3];
            ret[0].v2 = interpolate(vert_xys[3], vert_xys[0], verts[3], verts[0], threshold);
            ret[0].v3 = interpolate(vert_xys[3], vert_xys[2], verts[3], verts[2], threshold);
        } else if (!a && !b && c && !d) {
            ret = new Triangle[1];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[2];
            ret[0].v2 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[0].v3 = interpolate(vert_xys[3], vert_xys[2], verts[3], verts[2], threshold);
        } else if (!a && !b && c && d) {
            ret = new Triangle[2];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[3];
            ret[0].v2 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
            ret[0].v3 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[2];
            ret[1].v2 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1].v3 = interpolate(vert_xys[2], vert_xys[3], verts[2], verts[3], threshold);
        } else if (!a && b && !c && !d) {
            ret = new Triangle[1];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[1];
            ret[0].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v3 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
        } else if (!a && b && !c && d) {
            ret = new Triangle[2];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[1];
            ret[0].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v3 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[3];
            ret[1].v2 = interpolate(vert_xys[3], vert_xys[2], verts[3], verts[2], threshold);
            ret[1].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
        } else if (!a && b && c && !d) {
            ret = new Triangle[2];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[1];
            ret[0].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v3 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[2];
            ret[1].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[1].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
        } else if (!a && b && c && d) {
            ret = new Triangle[3];
            ret[0] = new Triangle();
            ret[0].v1 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v2 = vert_xys[1];
            ret[0].v3 = vert_xys[2];
            ret[1] = new Triangle();
            ret[1].v1 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[1].v2 = vert_xys[2];
            ret[1].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
            ret[2] = new Triangle();
            ret[2].v1 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
            ret[2].v2 = vert_xys[3];
            ret[2].v3 = vert_xys[2];
        } else if (a && !b && !c && !d) {
            ret = new Triangle[1];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[0];
            ret[0].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
        } else if (a && !b && !c && d) {
            ret = new Triangle[2];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[0];
            ret[0].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v3 = vert_xys[3];
            ret[1] = new Triangle();
            ret[1].v1 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[1].v2 = interpolate(vert_xys[2], vert_xys[3], verts[2], verts[3], threshold);
            ret[1].v3 = vert_xys[3];
        } else if (a && !b && c && !d) {
            ret = new Triangle[2];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[0];
            ret[0].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[2];
            ret[1].v2 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1].v3 = interpolate(vert_xys[2], vert_xys[3], verts[2], verts[3], threshold);
        } else if (a && !b && c && d) {
            ret = new Triangle[3];
            ret[0] = new Triangle();
            ret[0].v1 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[0].v2 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[0].v3 = vert_xys[3];
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[0];
            ret[1].v2 = interpolate(vert_xys[0], vert_xys[1], verts[0], verts[1], threshold);
            ret[1].v3 = vert_xys[3];
            ret[2] = new Triangle();
            ret[2].v1 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[2].v2 = vert_xys[2];
            ret[2].v3 = vert_xys[3];
        } else if (a && b && !c && !d) {
            ret = new Triangle[2];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[0];
            ret[0].v2 = vert_xys[1];
            ret[0].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[2];
            ret[1].v2 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
        } else if (a && b && !c && d) {
            ret = new Triangle[3];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[0];
            ret[0].v2 = vert_xys[1];
            ret[0].v3 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[0];
            ret[1].v2 = interpolate(vert_xys[1], vert_xys[2], verts[1], verts[2], threshold);
            ret[1].v3 = interpolate(vert_xys[2], vert_xys[3], verts[2], verts[3], threshold);
            ret[2] = new Triangle();
            ret[2].v1 = vert_xys[0];
            ret[2].v2 = interpolate(vert_xys[2], vert_xys[3], verts[2], verts[3], threshold);
            ret[2].v3 = vert_xys[3];
        } else if (a && b && c && !d) {
            ret = new Triangle[3];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[0];
            ret[0].v2 = vert_xys[1];
            ret[0].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[1];
            ret[1].v2 = interpolate(vert_xys[2], vert_xys[3], verts[2], verts[3], threshold);
            ret[1].v3 = interpolate(vert_xys[0], vert_xys[3], verts[0], verts[3], threshold);
            ret[2] = new Triangle();
            ret[2].v1 = vert_xys[1];
            ret[2].v2 = vert_xys[2];
            ret[2].v3 = interpolate(vert_xys[2], vert_xys[3], verts[2], verts[3], threshold);
        } else if (a && b && c && d) {
            ret = new Triangle[2];
            ret[0] = new Triangle();
            ret[0].v1 = vert_xys[0];
            ret[0].v2 = vert_xys[1];
            ret[0].v3 = vert_xys[3];
            ret[1] = new Triangle();
            ret[1].v1 = vert_xys[1];
            ret[1].v2 = vert_xys[2];
            ret[1].v3 = vert_xys[3];
        }
        return ret;
    }
}

class Triangle {

    Point2D.Float v1;

    Point2D.Float v2;

    Point2D.Float v3;
}

class Grid2D {

    double[][] mData;

    float mPixelsPerSquare;

    public Grid2D(double[][] data, float pixels_per_square) {
        mData = data;
        mPixelsPerSquare = pixels_per_square;
    }

    public void getSquare(int x, int y, Point2D.Float[] verts, double[] vert_values) {
    }

    public double getValueAtGridPoint(int x, int y) {
        return 0d;
    }
}
