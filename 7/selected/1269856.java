package mipt.gui.graph.function;

import java.awt.Graphics2D;
import java.awt.geom.CubicCurve2D;
import mipt.gui.graph.GraphRegion;
import mipt.gui.graph.plot.CurveStyle;

/**
 * 
 * @author German Shchelik
 */
public class CubicSplineRenderer extends AbstractSplineRenderer {

    private double BorderLeft;

    private double BorderRight;

    private double[][] koef;

    public CubicSplineRenderer() {
        BorderLeft = 0;
        BorderRight = 0;
    }

    public CubicSplineRenderer(double[][] storage) {
        this();
        setData(storage);
    }

    public void setBorderLeft(double borderLeft) {
        BorderLeft = borderLeft;
    }

    public double getBorderLeft() {
        return BorderLeft;
    }

    public void setBorderRight(double borderRight) {
        BorderRight = borderRight;
    }

    public double getBorderRight() {
        return BorderRight;
    }

    private final double getYPoint(double x, int i, double[][] data) {
        double koef[][] = getCoef();
        return koef[i][0] + koef[i][1] * (x - data[i][0]) + koef[i][2] * (x - data[i][0]) * (x - data[i][0]) + koef[i][3] * (x - data[i][0]) * (x - data[i][0]) * (x - data[i][0]);
    }

    /**
	 * @see mipt.gui.graph.function.AbstractCurveRenderer#paintInterval(mipt.gui.graph.GraphRegion, mipt.gui.graph.plot.CurveStyle, java.lang.Object, int, int)
	 */
    protected void paintInterval(GraphRegion rgn, CurveStyle style, double[][] data, int i, int delta) {
        double x = getX(data[i][0]), y = getY(data[i][1]);
        double x1 = getX(data[i + 1][0]), y1 = getY(data[i + 1][1]);
        double ctrlx = 2 * x / 3 + x1 / 3;
        paintHorVerLines(rgn, style, x, y, i);
        CubicCurve2D c = new CubicCurve2D.Double();
        double cx = rgn.getX(ctrlx), cy = getYPoint(ctrlx, i, data);
        c.setCurve(rgn.getX(x), rgn.getY(y), cx, cy, cx, cy, rgn.getX(x1), rgn.getY(y1));
        ((Graphics2D) rgn.gr).draw(c);
    }

    /**
	 * @see mipt.gui.graph.function.AbstractSplineRenderer#getCoef(mipt.gui.graph.plot.CurveStyle)
	 */
    protected void calculate(double[][] database) {
        int SP_NUM = getPointCount() - 1;
        if (SP_NUM < 0) {
            koef = null;
            return;
        }
        koef = new double[SP_NUM][4];
        double[] h = new double[SP_NUM];
        for (int i = 0; i < SP_NUM; i++) {
            h[i] = database[i + 1][0] - database[i][0];
            koef[i][0] = database[i][1];
        }
        double[] sub = new double[SP_NUM];
        double[] diag = new double[SP_NUM];
        double[] sup = new double[SP_NUM];
        double[] f = new double[SP_NUM];
        for (int i = 1; i < SP_NUM - 1; i++) {
            sub[i] = h[i] / 3;
            sup[i] = h[i - 1] / 3;
            diag[i] = 2 * h[i] / 3 + 2 * h[i - 1] / 3;
            f[i] = (database[i + 1][1] + database[i][1]) / h[i] - (database[i][1] + database[i - 1][1]) / h[i - 1];
        }
        sub[0] = h[0] / 3;
        sup[SP_NUM - 1] = h[SP_NUM - 1] / 3;
        diag[0] = 2 * h[0] / 3;
        diag[SP_NUM - 1] = 2 * h[SP_NUM - 1] / 3;
        f[0] = (database[1][1] + database[0][1]) / h[0] - getBorderLeft();
        f[SP_NUM - 1] = -(database[SP_NUM][1] + database[SP_NUM - 1][1]) / h[SP_NUM - 1] + getBorderRight();
        solveTridiag(sub, diag, sup, f, SP_NUM);
        koef[0][1] = getBorderLeft();
        koef[SP_NUM - 1][1] = getBorderRight();
        for (int i = 1; i < SP_NUM - 1; i++) {
            koef[i][2] = f[i];
            koef[i][3] = (f[i + 1] - f[i]) / 3 / h[i];
            koef[i][1] = (f[i] + f[i - 1]) * h[i - 1] + koef[i - 1][1];
        }
        koef[0][2] = f[0];
        koef[SP_NUM - 1][2] = f[SP_NUM - 1];
        koef[0][3] = (f[1] - f[0]) / 3 / h[0];
        koef[SP_NUM - 1][3] = (database[SP_NUM][1] - database[SP_NUM][1]) / h[SP_NUM - 1] / h[SP_NUM - 1] / h[SP_NUM - 1] - f[SP_NUM - 1] / h[SP_NUM - 1] - koef[SP_NUM - 1][1] / h[SP_NUM - 1] / h[SP_NUM - 1];
    }

    /**
	 * 
	 * @see mipt.gui.graph.function.AbstractSplineRenderer#getCoef()
	 */
    protected double[][] getCoef() {
        if (koef == null) {
            calculate(data);
        }
        return koef;
    }

    private void solveTridiag(double[] sub, double[] diag, double[] sup, double[] f, int n) {
        int i;
        double a[] = new double[n];
        double b[] = new double[n];
        a[1] = -sub[0] / diag[0];
        b[1] = -f[0] / diag[0];
        for (i = 2; i < n; i++) {
            a[i] = -sub[i - 1] / (sup[i - 1] * a[i - 1] + diag[i - 1]);
            b[i] = (f[i - 1] - sup[i - 1] * b[i - 1]) / (sup[i - 1] * a[i - 1] + diag[i - 1]);
        }
        f[n - 1] = (f[n - 1] - sup[n - 1] * b[n - 1]) / (diag[n - 1] + sup[n - 1] * a[n - 1]);
        for (i = n - 2; i >= 0; i--) {
            f[i] = b[i + 1] - a[i + 1] * f[i + 1];
        }
    }
}
