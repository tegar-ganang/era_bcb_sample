package uk.ac.city.soi.everest.monitor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.apache.log4j.Logger;
import uk.ac.city.soi.everest.core.Constants;
import uk.ac.city.soi.everest.core.TimeExpression;

/**
 * This class holds the constraint matrix resulted from all the ranges of predicates in a formula. This matrix is
 * further used in Simplex method.
 * 
 * @author Khaled Mahbub
 * 
 */
public class ConstraintMatrix {

    private static Logger logger = Logger.getLogger(ConstraintMatrix.class);

    private String formulaId = "";

    private ArrayList<String> timeVarNames = new ArrayList<String>();

    private LinkedHashMap<String, String> timeVars = new LinkedHashMap<String, String>();

    private long A[][];

    private long B[];

    private int rowCount = 0;

    private int colCount = 0;

    /**
     * Constructor for given template object.
     * 
     * @param template
     */
    public ConstraintMatrix(Template template) {
        this.formulaId = new String(template.getFormulaId());
        initializeMatrix(template);
    }

    private void initializeMatrix(Template template) {
        for (int i = 0; i < template.totalPredicates(); i++) {
            Predicate pred = template.getPredicate(i);
            if (!pred.getEcName().equals(Constants.PRED_RELATION)) {
                if (!timeVars.containsKey(pred.getTimeVarName())) {
                    timeVars.put(pred.getTimeVarName(), pred.getPartnerId());
                    timeVarNames.add(pred.getTimeVarName());
                }
            }
        }
        colCount = timeVars.size();
        rowCount = calculateRowNumbers(template);
        A = new long[rowCount][colCount];
        B = new long[rowCount];
        for (int i = 0, row = 0; i < template.totalPredicates(); i++) {
            Predicate pred = template.getPredicate(i);
            if ((pred.getEcName().equals(Constants.PRED_HAPPENS) || pred.getEcName().equals(Constants.PRED_INITIATES)) && !pred.isUnconstrained()) {
                String timeVariableName = pred.getTimeVarName();
                int timeVariableColumnIndex = getColumnIndex(timeVariableName);
                TimeExpression lowerBound = pred.getLowBound();
                if (!timeVariableName.equals(lowerBound.getTimeVarName())) {
                    int lowerBoundTimeVariableColumnIndex = getColumnIndex(lowerBound.getTimeVarName());
                    for (int col = 0; col < getColumnCount(); col++) {
                        if (col == timeVariableColumnIndex) {
                            A[row][col] = -1;
                        } else if (col == lowerBoundTimeVariableColumnIndex) {
                            A[row][col] = 1;
                        } else {
                            A[row][col] = 0;
                        }
                    }
                    B[row] = ((-1) * lowerBound.getNumber());
                    row++;
                }
                TimeExpression upperBound = pred.getUpBound();
                if (!timeVariableName.equals(upperBound.getTimeVarName())) {
                    int upperBoundTimeVariableColumnIndez = getColumnIndex(upperBound.getTimeVarName());
                    for (int col = 0; col < getColumnCount(); col++) {
                        if (col == timeVariableColumnIndex) {
                            A[row][col] = 1;
                        } else if (col == upperBoundTimeVariableColumnIndez) {
                            A[row][col] = -1;
                        } else A[row][col] = 0;
                    }
                    B[row] = upperBound.getNumber();
                    row++;
                }
                if (pred.getSource().equals(Constants.SOURCE_ABD) && pred.getTime2() != Constants.TIME_UD) {
                    for (int col = 0; col < getColumnCount(); col++) {
                        if (col == timeVariableColumnIndex) {
                            A[row][col] = -1;
                            A[((int) row + 1)][col] = 1;
                        } else {
                            A[row][col] = 0;
                            A[((int) row + 1)][col] = 0;
                        }
                    }
                    B[row] = ((-1) * pred.getTime1());
                    B[((int) row + 1)] = pred.getTime2();
                    row += 2;
                }
            }
        }
    }

    private int calculateRowNumbers(Template t) {
        int r = 0;
        logger.debug("calculating rows for the template: " + t.getTemplateId());
        for (int i = 0; i < t.totalPredicates(); i++) {
            Predicate pred = t.getPredicate(i);
            if ((pred.getEcName().equals(Constants.PRED_HAPPENS) || pred.getEcName().equals(Constants.PRED_INITIATES)) && !pred.isUnconstrained()) {
                logger.debug("\n" + pred.toString(true) + "\n");
                if (pred.getSource().equals(Constants.SOURCE_ABD) && pred.getTime2() != Constants.TIME_UD) {
                    logger.debug("abduced found: " + pred.toString(true) + "\n");
                    logger.debug("ConstraintTest");
                    r++;
                    r++;
                }
                String timeVariableName = pred.getTimeVarName();
                TimeExpression lowerBound = pred.getLowBound();
                TimeExpression upperBound = pred.getUpBound();
                if (!timeVariableName.equals(lowerBound.getTimeVarName())) {
                    r++;
                }
                if (!timeVariableName.equals(upperBound.getTimeVarName())) {
                    r++;
                }
            }
        }
        return r;
    }

    /**
     * Retrieves the channel name, which is actually the name of the time variable.
     * 
     * @param timeVarName
     * @return
     */
    public String getChannelName(String timeVarName) {
        return timeVars.containsKey(timeVarName) ? timeVars.get(timeVarName) : "";
    }

    /**
     * Retrieves the ColumnIndex, which is actually the name of the time variable.
     * 
     * @param timeVarName
     * @return
     */
    public int getColumnIndex(String timeVarName) {
        return timeVarNames.contains(timeVarName) ? timeVarNames.indexOf(timeVarName) : -1;
    }

    /**
     * Returns the number of columns taken into account.
     * 
     * @return
     */
    public int getColumnCount() {
        return colCount;
    }

    /**
     * Returns the number of rows taken into account.
     * 
     * @return
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * Returns the A matrix i.e., the matrix of Coefficients.
     * 
     * @return
     */
    public long[][] getA() {
        return A;
    }

    /**
     * Sets the A matrix i.e., the matrix of Coefficients.
     * 
     * @param a
     */
    public void setA(long[][] a) {
        A = a;
    }

    /**
     * Returns the B matrix i.e., the values matrix.
     * 
     * @return
     */
    public long[] getB() {
        return B;
    }

    /**
     * Sets the B matrix i.e., the values matrix.
     * 
     * @param b
     */
    public void setB(long[] b) {
        B = b;
    }

    /**
     * Returns the formula id.
     * 
     * @return
     */
    public String getFormulaId() {
        return formulaId;
    }

    /**
     * Sets the formula id.
     * 
     * @param formulaId
     */
    public void setFormulaId(String formulaId) {
        this.formulaId = formulaId;
    }

    /**
     * Returns the list of time variable names.
     * 
     * @return
     */
    public ArrayList getTimeVarNames() {
        return timeVarNames;
    }

    /**
     * Sets the list of time variable names.
     * 
     * @param timeVarNames
     */
    public void setTimeVarNames(ArrayList timeVarNames) {
        this.timeVarNames = timeVarNames;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String result = new String();
        for (String timeVariableName : timeVarNames) {
            result += timeVariableName + "\t";
        }
        result += "\n";
        for (int r = 0; r < A.length; r++) {
            for (int c = 0; c < A[r].length; c++) {
                result += A[r][c] + "\t";
            }
            result += "<=\t " + B[r] + "\n";
        }
        return result;
    }
}
