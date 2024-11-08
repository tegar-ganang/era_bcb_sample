package com.ogprover.polynomials;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Vector;
import com.ogprover.main.OGPConstants;
import com.ogprover.main.OpenGeoProver;
import com.ogprover.utilities.io.FileLogger;
import com.ogprover.utilities.io.OGPOutput;
import com.ogprover.utilities.io.SpecialFileFormatting;

/**
 * <dl>
 * <dt><b>Class description:</b></dt>
 * <dd>Class for system of x-polynomials</dd>
 * </dl>
 * 
 * @version 1.00
 * @author Ivan Petrovic
 */
public class XPolySystem {

    /**
	 * <i><b>
	 * Version number of class in form xx.yy where
	 * xx is major version/release number and yy is minor
	 * release number.
	 * </b></i>
	 */
    public static final String VERSION_NUM = "1.00";

    /**
	 * Collection of polynomials that make the system
	 */
    private Vector<XPolynomial> polynomials;

    /**
	 * List of variable indices as they were introduced in triangular system 
	 */
    private Vector<Integer> variableList;

    /**
	 * Method that retrieves collection of polynomials
	 * 
	 * @return The polynomials
	 */
    public Vector<XPolynomial> getPolynomials() {
        return polynomials;
    }

    /**
	 * Method that sets collection of polynomials
	 * 
	 * @param polynomials The polynomials to set
	 */
    public void setPolynomials(Vector<XPolynomial> polynomials) {
        this.polynomials = polynomials;
    }

    /**
	 * Method that retrieves variable list
	 * 
	 * @return The variableList
	 */
    public Vector<Integer> getVariableList() {
        return variableList;
    }

    /**
	 * Method that sets variable list
	 * 
	 * @param variableList The variableList to set
	 */
    public void setVariableList(Vector<Integer> variableList) {
        this.variableList = variableList;
    }

    /**
	 * Constructor method
	 */
    public XPolySystem() {
        this.polynomials = new Vector<XPolynomial>();
        this.variableList = null;
    }

    /**
	 * Gives XPolynomial object with specified index
	 * 
	 * @param index		Index of element
	 * @return			XPolynomial with specified index from collection
	 */
    public XPolynomial getXPoly(int index) {
        return this.polynomials.get(index);
    }

    /**
	 * Updates XPolynomial object in collection at specified index
	 * with passed in XPolynomial
	 * 
	 * @param index		Index of polynomial in collection
	 * @param xPoly		New value of polynomial to be set in collection
	 */
    public void setXPoly(int index, XPolynomial xPoly) {
        this.polynomials.set(index, xPoly);
    }

    /**
	 * Adds new polynomial to collection at specified index and
	 * shifts all polynomials in collection from and after that 
	 * position for one place to right
	 * 
	 * @param index		Index of polynomial in collection
	 * @param xPoly		Polynomial to be added to collection
	 */
    public void addXPoly(int index, XPolynomial xPoly) {
        this.polynomials.add(index, xPoly);
    }

    /**
	 * Adds new polynomial to the end of collection
	 * 
	 * @param xPoly		Polynomial to be added to collection
	 */
    public void addXPoly(XPolynomial xPoly) {
        this.polynomials.add(xPoly);
    }

    /**
	 * Removes polynomial with specified index from collection
	 * and shifts all polynomials after that position to the
	 * left for one place
	 *  
	 * @param index		Index of polynomial in collection
	 */
    public void removePoly(int index) {
        this.polynomials.remove(index);
    }

    /**
	 * Gets number of polynomials that make system
	 * 
	 * @return	Number of polynomials in system
	 */
    public int numOfPols() {
        return this.polynomials.size();
    }

    /**
	 * Check whether system is valid.
	 * It is valid if polynomials have no other
	 * x variables but those with indices from 1 to n
	 * where n is number of polynomials in system.
	 * Also, all these variables must appear in system.
	 * 
	 * @return	True if system is valid, false otherwise
	 */
    public boolean isValid() {
        int n = this.polynomials.size();
        int[] usedIndices = new int[n];
        for (int ii = 0; ii < n; ii++) usedIndices[ii] = 0;
        for (XPolynomial xp : this.polynomials) {
            for (Term xt : xp.getTermsAsDescList()) {
                for (Power pow : xt.getPowers()) {
                    int currIndex = (int) pow.getIndex();
                    if (currIndex <= 0 || currIndex > n) return false;
                    usedIndices[currIndex - 1] = 1;
                }
            }
        }
        for (int ii = 0; ii < n; ii++) {
            if (usedIndices[ii] == 0) return false;
        }
        return true;
    }

    /**
	 * Method that checks whether polynomial system is in triangular form 
	 * and re-orders it so that there is polynomial with only
	 * one variable at first place in system, then polynomial with
	 * only two variables (one from previous polynomial) at second place 
	 * in system, and so on.
	 * System of polynomials is triangular iff each next polynomial
	 * introduces exactly one new x variable.
	 *  
	 * @return	True if system is in triangular form, false otherwise
	 */
    public boolean checkAndReOrderTriangularSystem() {
        if (this.getPolynomials().size() == 0) return true;
        int n = this.polynomials.size();
        Vector<Integer> polysByNumOfVars = new Vector<Integer>(n + 1);
        Vector<BitSet> varsInPolys = new Vector<BitSet>(n);
        for (int ii = 0; ii < n; ii++) {
            polysByNumOfVars.add(new Integer(-1));
            BitSet bs = new BitSet(n);
            bs.clear();
            varsInPolys.add(bs);
        }
        polysByNumOfVars.add(new Integer(-1));
        this.variableList = new Vector<Integer>(n);
        for (int ii = 0; ii < n; ii++) {
            int counter = 0;
            ArrayList<Term> terms = this.polynomials.get(ii).getTermsAsDescList();
            for (int jj = 0, size = terms.size(); jj < size; jj++) {
                Vector<Power> powers = terms.get(jj).getPowers();
                for (int kk = 0, psize = powers.size(); kk < psize; kk++) {
                    int varIndex = (int) powers.get(kk).getIndex() - 1;
                    BitSet bs = varsInPolys.get(ii);
                    if (!bs.get(varIndex)) {
                        counter++;
                        bs.set(varIndex);
                    }
                }
            }
            if (counter <= 0 || counter > n || polysByNumOfVars.get(counter).intValue() >= 0) return false;
            polysByNumOfVars.set(counter, new Integer(ii));
        }
        this.variableList.add(0, new Integer(varsInPolys.get(polysByNumOfVars.get(1)).nextSetBit(0) + 1));
        for (int ii = 1, jj = 2; jj <= n; ii++, jj++) {
            BitSet bsi = varsInPolys.get(polysByNumOfVars.get(ii));
            BitSet bsiCopy = new BitSet(n);
            bsiCopy.clear();
            bsiCopy.or(bsi);
            BitSet bsj = varsInPolys.get(polysByNumOfVars.get(jj));
            bsiCopy.and(bsj);
            if (!bsi.equals(bsiCopy)) return false;
            BitSet bsiNewCopy = new BitSet(n);
            bsiNewCopy.clear();
            bsiNewCopy.or(bsi);
            bsiNewCopy.xor(bsj);
            this.variableList.add(jj - 1, new Integer(bsiNewCopy.nextSetBit(0) + 1));
        }
        Vector<XPolynomial> triangularSystem = new Vector<XPolynomial>(n);
        for (int ii = 0; ii < n; ii++) triangularSystem.add(ii, this.polynomials.get(polysByNumOfVars.get(ii + 1)));
        this.polynomials = triangularSystem;
        return true;
    }

    /**
	 * Method that performs triangulation over this system, i.e.
	 * transforms this system in triangular form
	 * 
	 * @return	Return code is zero when operation is successfully completed
	 * 			and negative with specific error code, if error happens
	 */
    public int triangulate() {
        StringBuilder sb;
        OGPOutput output = OpenGeoProver.settings.getOutput();
        FileLogger logger = OpenGeoProver.settings.getLogger();
        if (this.checkAndReOrderTriangularSystem() == true) {
            try {
                output.writePlainText("The system is already triangular.\n\n");
                output.writePolySystem(this);
            } catch (IOException e) {
                logger.error("Failed to write to output file(s).");
                output.close();
                return OGPConstants.ERR_CODE_GENERAL;
            }
            return OGPConstants.RET_CODE_SUCCESS;
        }
        this.variableList = new Vector<Integer>();
        Vector<XPolynomial> triangularSystem = new Vector<XPolynomial>();
        Vector<XPolynomial> freeSystem = null;
        Vector<XPolynomial> notFreeSystem = null;
        Vector<XPolynomial> auxSystem = this.polynomials;
        Vector<XPolynomial> tempSystemForOutput = null;
        XPolySystem tempPolySystem = null;
        Vector<Integer> originalIndexes = null;
        boolean tempSystemChanged = true;
        try {
            output.writePlainText("The input system is:\n\n");
            output.writePolySystem(this);
        } catch (IOException e) {
            logger.error("Failed to write to output file(s).");
            output.close();
            return OGPConstants.ERR_CODE_GENERAL;
        }
        for (int ii = this.polynomials.size(), istep = 1, isize = this.polynomials.size(); ii > 0; ii--, istep++) {
            try {
                output.openSubSection("Triangulation, step " + istep, true);
                output.openEnum(SpecialFileFormatting.ENUM_COMMAND_DESCRIPTION);
                output.openItemWithDesc("Choosing variable:");
                sb = new StringBuilder();
                sb.append("Trying the variable with index ");
                sb.append(ii);
                sb.append(".\n\n");
                output.closeItemWithDesc(sb.toString());
            } catch (IOException e) {
                logger.error("Failed to write to output file(s).");
                output.close();
                return OGPConstants.ERR_CODE_GENERAL;
            }
            freeSystem = new Vector<XPolynomial>();
            notFreeSystem = new Vector<XPolynomial>();
            originalIndexes = new Vector<Integer>();
            tempSystemChanged = true;
            for (int jj = 0, kk = auxSystem.size(); jj < kk; jj++) {
                XPolynomial currXPoly = auxSystem.get(jj);
                int varExp = 0;
                ArrayList<Term> termList = currXPoly.getTermsAsDescList();
                boolean allProcessed = false;
                int numOfTerms = termList.size();
                int counter = 0;
                boolean found = false;
                while (counter < numOfTerms && !allProcessed && !found) {
                    XTerm currTerm = (XTerm) termList.get(counter);
                    if (currTerm == null) {
                        logger.error("Found null term");
                        return OGPConstants.ERR_CODE_NULL;
                    }
                    varExp = currTerm.getVariableExponent(ii);
                    if (varExp > 0) found = true; else if (currTerm.getPowers().size() == 0 || currTerm.getPowers().get(0).getIndex() < ii) allProcessed = true;
                    counter++;
                }
                if (found) {
                    notFreeSystem.add((XPolynomial) currXPoly.clone());
                    originalIndexes.add(new Integer(jj));
                } else freeSystem.add((XPolynomial) currXPoly.clone());
            }
            if (notFreeSystem.size() == 0) {
                logger.error("Variable with index " + ii + " not found in polynomial system.");
                return OGPConstants.ERR_CODE_GENERAL;
            }
            try {
                sb = new StringBuilder();
                sb.append("Variable <ind_text><label>x</label><ind>");
                sb.append(ii);
                sb.append("</ind></ind_text> selected:");
                output.openItemWithDesc(sb.toString());
                sb = new StringBuilder();
                sb.append("The number of polynomials with this variable, with indexes from 1 to ");
                sb.append(isize - istep + 1);
                sb.append(", is ");
                sb.append(notFreeSystem.size());
                sb.append(".\n\n");
                output.closeItemWithDesc(sb.toString());
            } catch (IOException e) {
                logger.error("Failed to write to output file(s).");
                output.close();
                return OGPConstants.ERR_CODE_GENERAL;
            }
            if (notFreeSystem.size() == 1) {
                triangularSystem.add(0, notFreeSystem.get(0));
                this.variableList.add(0, new Integer(ii));
                auxSystem = freeSystem;
                tempSystemChanged = false;
                try {
                    output.openItemWithDesc("Single polynomial with chosen variable:");
                    sb = new StringBuilder();
                    sb.append("Chosen polynomial is <ind_text><label>p</label><ind>");
                    sb.append(originalIndexes.get(0).intValue() + 1);
                    sb.append("</ind></ind_text>. No reduction needed.\n\n");
                    output.closeItemWithDesc(sb.toString());
                    output.writeEnumItem("The triangular system has not been changed.\n\n");
                } catch (IOException e) {
                    logger.error("Failed to write to output file(s).");
                    output.close();
                    return OGPConstants.ERR_CODE_GENERAL;
                }
            } else {
                boolean end = false;
                do {
                    int first = 0, second = 1;
                    int exp1 = notFreeSystem.get(first).getLeadingExp(ii), exp2 = notFreeSystem.get(second).getLeadingExp(ii);
                    int min1, min2, count1 = 1, count2 = 1;
                    if (exp1 == 0 || exp2 == 0) {
                        logger.error("Variable not found when expected to be found.");
                        return OGPConstants.ERR_CODE_GENERAL;
                    }
                    if (exp1 <= exp2) {
                        min1 = exp1;
                        min2 = exp2;
                    } else {
                        first = 1;
                        second = 0;
                        min1 = exp2;
                        min2 = exp1;
                    }
                    for (int ll = 2, mm = notFreeSystem.size(); ll < mm; ll++) {
                        int currExp = notFreeSystem.get(ll).getLeadingExp(ii);
                        if (currExp == 0) {
                            logger.error("Variable not found when expected to be found.");
                            return OGPConstants.ERR_CODE_GENERAL;
                        }
                        if (currExp < min1) {
                            first = ll;
                            min1 = currExp;
                            count1 = 1;
                        } else if (currExp == min1) {
                            count1++;
                        } else if (currExp < min2) {
                            second = ll;
                            min2 = currExp;
                            count2 = 1;
                        } else if (currExp == min2) {
                            count2++;
                        }
                    }
                    try {
                        output.openItemWithDesc("Minimal degrees:");
                        sb = new StringBuilder();
                        if (min1 < min2) {
                            sb.append(count1);
                            sb.append(" polynomial(s) with degree ");
                            sb.append(min1);
                            sb.append(" and ");
                            sb.append(count2);
                            sb.append(" polynomial(s) with degree ");
                            sb.append(min2);
                        } else if (min1 == min2) {
                            sb.append(count1 + count2);
                            sb.append(" polynomial(s) with degree ");
                            sb.append(min1);
                        }
                        sb.append(".\n\n");
                        output.closeItemWithDesc(sb.toString());
                    } catch (IOException e) {
                        logger.error("Failed to write to output file(s).");
                        output.close();
                        return OGPConstants.ERR_CODE_GENERAL;
                    }
                    if (min1 == 1) {
                        try {
                            output.openItemWithDesc("Polynomial with linear degree:");
                            sb = new StringBuilder();
                            sb.append("Removing variable <ind_text><label>x</label><ind>");
                            sb.append(ii);
                            sb.append("</ind></ind_text> from all other polynomials by reducing them with polynomial <ind_text><label>p</label><ind>");
                            sb.append(originalIndexes.get(first).intValue() + 1);
                            sb.append("</ind></ind_text> from previous step.\n\n");
                            output.closeItemWithDesc(sb.toString());
                        } catch (IOException e) {
                            logger.error("Failed to write to output file(s).");
                            output.close();
                            return OGPConstants.ERR_CODE_GENERAL;
                        }
                        XPolynomial currPoly = notFreeSystem.get(first);
                        triangularSystem.add(0, currPoly);
                        this.variableList.add(0, new Integer(ii));
                        notFreeSystem.remove(first);
                        for (int ll = 0, mm = notFreeSystem.size(); ll < mm; ll++) {
                            XPolynomial tempXP = notFreeSystem.get(ll).pseudoReminder(currPoly, ii);
                            if (tempXP == null) return OpenGeoProver.settings.getRetCodeOfPseudoDivision();
                            int numOfTerms = tempXP.getTerms().size();
                            if (numOfTerms > OpenGeoProver.settings.getParameters().getSpaceLimit()) {
                                logger.error("Polynomial exceeds maximal allowed number of terms.");
                                return OGPConstants.ERR_CODE_SPACE;
                            }
                            if (numOfTerms > OpenGeoProver.settings.getMaxNumOfTerms()) {
                                OpenGeoProver.settings.setMaxNumOfTerms(numOfTerms);
                            }
                            if (OpenGeoProver.settings.getTimer().isTimeIsUp()) {
                                logger.error("Prover execution time has been expired.");
                                return OGPConstants.ERR_CODE_TIME;
                            }
                            freeSystem.add(tempXP);
                        }
                        auxSystem = freeSystem;
                        end = true;
                    } else {
                        XPolynomial r2 = notFreeSystem.get(second);
                        XPolynomial r1 = notFreeSystem.get(first);
                        int leadExp = 0;
                        try {
                            output.openItemWithDesc("No linear degree polynomials:");
                            sb = new StringBuilder();
                            sb.append("Reducing polynomial <ind_text><label>p</label><ind>");
                            sb.append(second + 1);
                            sb.append("</ind></ind_text> (of degree ");
                            sb.append(count2);
                            sb.append(") with <ind_text><label>p</label><ind>");
                            sb.append(first + 1);
                            sb.append("</ind></ind_text> (of degree ");
                            sb.append(count1);
                            sb.append(").\n\n");
                            output.closeItemWithDesc(sb.toString());
                        } catch (IOException e) {
                            logger.error("Failed to write to output file(s).");
                            output.close();
                            return OGPConstants.ERR_CODE_GENERAL;
                        }
                        do {
                            XPolynomial temp = r2.pseudoReminder(r1, ii);
                            if (temp == null) return OpenGeoProver.settings.getRetCodeOfPseudoDivision();
                            int numOfTerms = temp.getTerms().size();
                            if (numOfTerms > OpenGeoProver.settings.getParameters().getSpaceLimit()) {
                                logger.error("Polynomial exceeds maximal allowed number of terms.");
                                return OGPConstants.ERR_CODE_SPACE;
                            }
                            if (numOfTerms > OpenGeoProver.settings.getMaxNumOfTerms()) {
                                OpenGeoProver.settings.setMaxNumOfTerms(numOfTerms);
                            }
                            if (OpenGeoProver.settings.getTimer().isTimeIsUp()) {
                                logger.error("Prover execution time has been expired.");
                                return OGPConstants.ERR_CODE_TIME;
                            }
                            r2 = r1;
                            r1 = temp;
                            if (r1.isZero()) {
                                logger.error("Two polynomials have common factor.");
                                return OGPConstants.ERR_CODE_GENERAL;
                            }
                            leadExp = r1.getLeadingExp(ii);
                        } while (leadExp > 1);
                        notFreeSystem.set(first, r1);
                        notFreeSystem.set(second, r2);
                        if (leadExp == 0) {
                            freeSystem.add(r1);
                            notFreeSystem.remove(first);
                            if (notFreeSystem.size() == 1) {
                                triangularSystem.add(0, r2);
                                this.variableList.add(0, new Integer(ii));
                                auxSystem = freeSystem;
                                end = true;
                            }
                        } else {
                            triangularSystem.add(0, r1);
                            this.variableList.add(0, new Integer(ii));
                            notFreeSystem.remove(first);
                            for (int ll = 0, mm = notFreeSystem.size(); ll < mm; ll++) {
                                XPolynomial tempXP = notFreeSystem.get(ll).pseudoReminder(r1, ii);
                                if (tempXP == null) return OpenGeoProver.settings.getRetCodeOfPseudoDivision();
                                int numOfTerms = tempXP.getTerms().size();
                                if (numOfTerms > OpenGeoProver.settings.getParameters().getSpaceLimit()) {
                                    logger.error("Polynomial exceeds maximal allowed number of terms.");
                                    return OGPConstants.ERR_CODE_SPACE;
                                }
                                if (numOfTerms > OpenGeoProver.settings.getMaxNumOfTerms()) {
                                    OpenGeoProver.settings.setMaxNumOfTerms(numOfTerms);
                                }
                                if (OpenGeoProver.settings.getTimer().isTimeIsUp()) {
                                    logger.error("Prover execution time has been expired.");
                                    return OGPConstants.ERR_CODE_TIME;
                                }
                                freeSystem.add(tempXP);
                            }
                            auxSystem = freeSystem;
                            end = true;
                        }
                    }
                } while (!end);
            }
            tempSystemForOutput = new Vector<XPolynomial>();
            for (XPolynomial xp : auxSystem) tempSystemForOutput.add(xp);
            for (XPolynomial xp : triangularSystem) tempSystemForOutput.add(xp);
            tempPolySystem = new XPolySystem();
            tempPolySystem.setPolynomials(tempSystemForOutput);
            try {
                output.closeEnum(SpecialFileFormatting.ENUM_COMMAND_DESCRIPTION);
                if (tempSystemChanged) {
                    output.writePlainText("Finished a triangulation step, the current system is:\n\n");
                    output.writePolySystem(tempPolySystem);
                }
                output.closeSubSection();
            } catch (IOException e) {
                logger.error("Failed to write to output file(s).");
                output.close();
                return OGPConstants.ERR_CODE_GENERAL;
            }
        }
        this.polynomials = triangularSystem;
        try {
            output.writePlainText("\n\nThe triangular system is:\n\n");
            output.writePolySystem(this);
        } catch (IOException e) {
            logger.error("Failed to write to output file(s).");
            output.close();
            return OGPConstants.ERR_CODE_GENERAL;
        }
        return OGPConstants.RET_CODE_SUCCESS;
    }

    /**
	 * Method which examines whether this polynomial system is linear;
	 * i.e. if power of each dependent variable in each term of each
	 * polynomial of system is not greater than 1.
	 * 
	 * @return		True if system is linear, false otherwise
	 */
    public boolean isSystemLinear() {
        for (XPolynomial xp : this.polynomials) {
            for (Term xt : xp.getTermsAsDescList()) {
                for (Power pow : xt.getPowers()) {
                    if (pow.getExponent() > 1) return false;
                }
            }
        }
        return true;
    }
}
