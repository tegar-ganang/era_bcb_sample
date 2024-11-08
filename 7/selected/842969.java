package jp.go.ipa.jgcl;

/**
 *      S                         N   X
 *
 * @version $Revision: 1.9 $, $Date: 2000/05/25 13:11:57 $
 * @author Information-technology Promotion Agency, Japan
 */
final class Interpolation {

    /**
     *          _             p     [ ^
     */
    final double[] params;

    /**
     *          _      
     */
    final int uip;

    /**
     *  p     [ ^   u
     *
     *  J        -1 I   W   A          -2 I   W             A
     *      A N Z X          (pInt()   \ b h   g  )
     */
    private double[] pInt = null;

    /**
     *  s  
     * @see	MatrixDouble
     */
    final MatrixDouble matrix;

    /**
     *  _                         t   O
     */
    final boolean isClosed;

    /**
     *  p     [ ^   ^     I u W F N g   \ z    
     * ( _     J                  )
     *
     * @param params    p     [ ^
     */
    Interpolation(double[] params) {
        if (params.length < 2) throw new ExceptionGeometryInvalidArgumentValue();
        this.params = params;
        this.isClosed = false;
        this.uip = params.length;
        this.pInt = getInterval();
        this.matrix = setupLinearSystem();
        computeLeftSideLinearSystem();
    }

    /**
     *  p     [ ^   _                               ^     I u W F N g   \ z    
     *
     * @param params    p     [ ^
     * @param isClosed  _                         t   O
     */
    Interpolation(double[] params, boolean isClosed) {
        if (params.length < 2) throw new ExceptionGeometryInvalidArgumentValue();
        this.params = params;
        this.isClosed = isClosed;
        if (this.isClosed) {
            this.uip = params.length - 1;
        } else {
            this.uip = params.length;
        }
        this.pInt = getInterval();
        if (!isClosed) {
            this.matrix = setupLinearSystem();
            computeLeftSideLinearSystem();
        } else {
            this.matrix = setupLinearSystemClosed();
        }
    }

    /**
     *  p     [ ^   u        
     * @param i	 C   f b N X
     * @return	 p     [ ^   u   l
     */
    double pInt(int i) {
        if (!isClosed) return pInt[i + 1]; else return pInt[i + 2];
    }

    /**
     *  p     [ ^   u   l          
     * @param i		 C   f b N X
     * @param value	         l
     */
    private void pInt(int i, double value) {
        if (!isClosed) pInt[i + 1] = value; else pInt[i + 2] = value;
    }

    /**
     *          _     p     [ ^   u        
     *
     * @return  p     [ ^     u
     */
    private double[] getInterval() {
        if (!isClosed) {
            pInt = new double[uip + 1];
            pInt(-1, 0.0);
            for (int i = 0; i < uip - 1; i++) {
                pInt(i, params[i + 1] - params[i]);
            }
            pInt(uip - 1, 0.0);
        } else {
            pInt = new double[uip + 3];
            int i;
            for (i = 0; i < uip; i++) {
                pInt(i, params[i + 1] - params[i]);
            }
            pInt(i, pInt(0));
            pInt(-2, pInt(uip - 2));
            pInt(-1, pInt(uip - 1));
        }
        return pInt;
    }

    /**
     *  s        ( J     _        )
     *
     * @return     s  
     * @see	MatrixDouble
     */
    private MatrixDouble setupLinearSystem() {
        MatrixDouble matrix = new MatrixDouble(uip, 3);
        double[] firstRow = { 0.0, 1.0, 0.0 };
        matrix.setElementsAt(0, firstRow);
        double denomJ = pInt(-1) + pInt(0) + pInt(1);
        for (int j = 1; j < uip - 1; j++) {
            double denomJ1 = pInt(j - 1) + pInt(j) + pInt(j + 1);
            double[] value = { (pInt(j) * pInt(j)) / denomJ, ((pInt(j) * (pInt(j - 2) + pInt(j - 1))) / denomJ) + ((pInt(j - 1) * (pInt(j) + pInt(j + 1))) / denomJ1), (pInt(j - 1) * pInt(j - 1)) / denomJ1 };
            matrix.setElementsAt(j, value);
            denomJ = denomJ1;
        }
        double[] lastRow = { 0.0, 1.0, 0.0 };
        matrix.setElementsAt(uip - 1, lastRow);
        return matrix;
    }

    /**
     *  s        (       _        )
     *
     * @return     s  
     * @see	MatrixDouble
     */
    private MatrixDouble setupLinearSystemClosed() {
        MatrixDouble matrix = new MatrixDouble(uip, uip);
        for (int j = 0; j < uip; j++) {
            for (int k = 0; k < uip; k++) {
                matrix.setElementAt(j, k, 0.0);
            }
        }
        double denomJ = pInt(-2) + pInt(-1) + pInt(0);
        for (int j = 0; j < uip; j++) {
            double denomJ1 = pInt(j - 1) + pInt(j) + pInt(j + 1);
            int alpha = (j == 0) ? (uip - 1) : j - 1;
            int beta = j;
            int gamma = (j == (uip - 1)) ? 0 : j + 1;
            matrix.setElementAt(j, alpha, (pInt(j) * pInt(j)) / denomJ);
            matrix.setElementAt(j, beta, ((pInt(j) * (pInt(j - 2) + pInt(j - 1))) / denomJ) + ((pInt(j - 1) * (pInt(j) + pInt(j + 1))) / denomJ1));
            matrix.setElementAt(j, gamma, (pInt(j - 1) * pInt(j - 1)) / denomJ1);
            denomJ = denomJ1;
        }
        matrix.makeLUDecomposition();
        return matrix;
    }

    /**
     *  s     v Z    
     */
    private void computeLeftSideLinearSystem() {
        for (int i = 1; i < uip; i++) {
            double val0 = matrix.getElementAt(i, 0);
            double val1 = matrix.getElementAt(i - 1, 1);
            double val2 = matrix.getElementAt(i - 1, 2);
            double value = matrix.getElementAt(i, 1);
            double value0 = val0 / val1;
            value -= value0 * val2;
            matrix.setElementAt(i, 0, value0);
            matrix.setElementAt(i, 1, value);
        }
        for (int i = uip - 2; i >= 0; i--) {
            double value0 = matrix.getElementAt(i, 2);
            double value1 = matrix.getElementAt(i + 1, 1);
            double value = value0 / value1;
            matrix.setElementAt(i, 2, value);
        }
    }

    /**
     *                m b g            ( J     _  )
     *
     * @return        m b g    
     * @see	BsplineKnotVector
     */
    private BsplineKnotVector knotDataOpened() {
        int uik = uip;
        double[] knots = new double[uik];
        int[] knotMultiplicities = new int[uik];
        knots[0] = params[0];
        knotMultiplicities[0] = 4;
        int i;
        for (i = 1; i < uik - 1; i++) {
            knots[i] = params[i];
            knotMultiplicities[i] = 1;
        }
        knots[i] = params[i];
        knotMultiplicities[i] = 4;
        return new BsplineKnotVector(3, KnotType.UNSPECIFIED, isClosed, uik, knotMultiplicities, knots, uip + 2);
    }

    /**
     *                m b g            (       _  )
     *
     * @return        m b g    
     * @see	BsplineKnotVector
     */
    private BsplineKnotVector knotDataClosed() {
        int degree = 3;
        int uik = (2 * degree) + uip + 1;
        double[] knots = new double[uik];
        int[] knotMultiplicities = new int[uik];
        knots[degree] = params[0];
        knotMultiplicities[degree] = 1;
        int i, j;
        for (i = degree - 1, j = uip - 1; i >= 0; i--, j--) {
            knots[i] = knots[i + 1] - pInt(j);
            knotMultiplicities[i] = 1;
        }
        for (i = degree + 1, j = 1; j < uip + 1; i++, j++) {
            knots[i] = params[j];
            knotMultiplicities[i] = 1;
        }
        for (j = 0; j < degree; i++, j++) {
            knots[i] = knots[i - 1] + pInt(j);
            knotMultiplicities[i] = 1;
        }
        return new BsplineKnotVector(3, KnotType.UNSPECIFIED, isClosed, uik, knotMultiplicities, knots, uip);
    }

    /**
     *                m b g            
     *
     * @return        m b g    
     * @see	BsplineKnotVector
     */
    BsplineKnotVector knotData() {
        if (!isClosed) {
            return knotDataOpened();
        } else {
            return knotDataClosed();
        }
    }

    /**
     *                    _          
     * @return	     _    
     */
    int nControlPoints() {
        if (isClosed) return uip; else return uip + 2;
    }
}
