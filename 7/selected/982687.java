package mipt.gui.graph.function;

import mipt.gui.graph.GraphRegion;
import mipt.gui.graph.plot.CurveStyle;
import java.awt.Graphics2D;
import java.awt.geom.QuadCurve2D;

/**
 * This class makes spline interpolation with Parabolic curves.
 * For spline coefficients to calculate you need to determine derivatives either on the left border or on the right
 * 'border' variable is a value of derivative on the border (0 by default)
 * 'bType' equals 'false' when derivative known on the left border, 'true' on the contrary
 * @author German Schelik
 */
public class ParabSplineRenderer extends AbstractSplineRenderer {

    private double border;

    private boolean bType;

    private double[][] koef;

    /**
	 * This method returns spline coefficients calculated before
	 */
    public final double[][] getCoef() {
        if (koef == null) {
            calculate(data);
        }
        return koef;
    }

    /**
	 * See the explanation above
	 * 'border' variable is 0 by default
	 * 'bType' equals 'false' when derivative known on the left border
	 */
    public ParabSplineRenderer() {
        this.border = 0;
        bType = false;
    }

    public ParabSplineRenderer(double[][] storage) {
        this();
        setData(storage);
    }

    public final double getBorder() {
        return border;
    }

    public final void setBorder(double border) {
        this.border = border;
    }

    public final boolean isBType() {
        return bType;
    }

    public final void setBType(boolean type) {
        bType = type;
    }

    /**
	 * Method returns the middle point of the Parabolic spline curve that is needed for QuadCurve2D.Double. 
	 * QuadCurve2D.Double class needs three points to draw the curve. Two are in the borders, the last one in the middle
	 * @param i - the number of drawing interval in data array
	 * @param data - the data array
	 * @return The value of Y for X = (data[i][0]+data[i+1][0])/2
	 */
    private final double getYMidPoint(int i, double[][] data) {
        double koef[][] = getCoef();
        return koef[i][0] + koef[i][1] * ((data[i][0] + data[i + 1][0]) / 2 - data[i][0]) + koef[i][2] / 2 * ((data[i][0] + data[i + 1][0]) / 2 - data[i][0]);
    }

    /**
	 * @see mipt.gui.graph.function.AbstractCurveRenderer#paintInterval(mipt.gui.graph.GraphRegion, mipt.gui.graph.plot.CurveStyle, java.lang.Object, int, int)
	 */
    protected void paintInterval(GraphRegion rgn, CurveStyle style, double[][] data, int i, int delta) {
        double x = getX(data[i][0]), y = getY(data[i][1]);
        double x1 = getX(data[i + 1][0]), y1 = getY(data[i + 1][1]);
        paintHorVerLines(rgn, style, x, y, i);
        QuadCurve2D c = new QuadCurve2D.Double();
        c.setCurve(rgn.getX(x), rgn.getY(y), rgn.getX((x + x1) / 2), rgn.getY(getYMidPoint(i, data)), rgn.getX(x1), rgn.getY(y1));
        ((Graphics2D) rgn.gr).draw(c);
    }

    /**
	 * Calculates massive of spline coefficients 
	 * Massive koef[][3] contains a[i],b[i],c[i] coefficients
	 * of the spline S(i) = a[i] + b[i]*(x - x[i]) + c[i]/2*(x - x[i])
	 */
    public void calculate(double[][] database) {
        int SP_NUM = getPointCount() - 1;
        if (SP_NUM < 0) {
            koef = null;
            return;
        }
        koef = new double[SP_NUM][3];
        double[] h = new double[SP_NUM];
        for (int i = 0; i < SP_NUM; i++) {
            if (database[i + 1] == null) {
                SP_NUM = i;
                break;
            }
            h[i] = database[i + 1][0] - database[i][0];
            koef[i][0] = database[i][1];
        }
        if (bType == false) {
            koef[0][1] = border;
            koef[0][2] = 2 * ((database[1][1] - database[0][1]) / h[0] / h[0] - koef[0][1] / h[0]);
            for (int i = 1; i < SP_NUM; i++) {
                koef[i][2] = 2 / h[i] * ((database[i + 1][1] - database[i][1]) / h[i] - (database[i][1] - database[i - 1][1]) / h[i - 1] - koef[i - 1][2] * h[i - 1] / 2);
                koef[i][1] = (database[i + 1][1] - database[i][1]) / h[i] - koef[i][2] / 2 * h[i];
            }
        } else {
            koef[SP_NUM - 1][2] = -2 * ((database[SP_NUM][1] - database[SP_NUM - 1][1]) / h[SP_NUM - 1] / h[SP_NUM - 1] - border / h[SP_NUM - 1]);
            koef[SP_NUM - 1][1] = border - koef[SP_NUM - 1][2] * h[SP_NUM - 1];
            for (int i = SP_NUM - 2; i >= 0; i--) {
                koef[i][2] = 2 / h[i] * ((database[i + 2][1] - database[i + 1][1]) / h[i + 1] - (database[i + 1][1] - database[i][1]) / h[i] - koef[i + 1][2] * h[i + 1] / 2);
                koef[i][1] = (database[i + 1][1] - database[i][1]) / h[i] - koef[i][2] / 2 * h[i];
            }
        }
    }
}
