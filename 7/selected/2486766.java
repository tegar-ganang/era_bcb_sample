package com.interworldtransport.clados;

/** 
 * Multiplication table for a Monad and associated methods.
 * <p>
 * ProductTables encapsulate Clifford Algebra multiplication tables.  ProductTables
 * are kept in a separate class so that flat Monads may share them in the future.
 * Only Monads should need to construct and own a ProductTable.  A Monad should 
 * be able to rely upon a ProductTable to handle the minutia of multiplication.
 * <p>
 * Operations in the Product Table should return logicals for tests the table
 * can answer.  They should alter inbound objects for complex requests.  At no
 * point should a Product Table have to make a copy of itself or inbound objects
 * except for private use.
 * <p>
 * A Product table will actually assume it is OK to perform the requested
 * operations and throw an exception if it discovers later that it isn't.  This
 * discovery isn't likely to be very good, so it should not be expected to work
 * well.  Only checks against primitive elements can be done.  Physical sense
 * checks need to be performed at the Monad level.
 * <p>
 * At present, each Monad will maintain a copy of its own product table.  In the
 * not too distant future, the product table should be made static or shareable
 * by all Monads using the same signature, reference frame and foot.
 * <p>
 * @version 0.80, $Date: 2005/09/29 08:36:20 $
 * @author Dr Alfred W Differ
 */
public class ProductTable extends CladosObject {

    /** 
 * This string holds the parent Monad for this product table.
 */
    private CladosObject Parent;

    /** This string holds the signature information describing the squares of all
 *  geometry generators present on the multiplication table.
 */
    private String Signature;

    private int[] intSignature;

    /**
 * This basis holds a representation of all the elements that can be built from
 * the generators that space the algebra's vector space. 
 */
    private Basis ABasis;

    /** This array holds the geometric multiplication table for the Monad.
 *  The array contains numbers that represent the row of the Eddington
 *  Basis one would produce with a product of elements represented by
 *  the row and column of ProductResult.
 */
    private int[][] ProductResult;

    /** This integer is the number of linearly independent basis elements in the algebra.
 *  It is a count of the number of Eddington Basis elements and is used often
 *  enough to be worth keeping around.
 */
    private int linearDim;

    /** This integer is the number of independent blades in the algebra.
 *  It is equal to FrPosSig+FrNegSig+1 and is used often enough to be worth keeping.
 */
    private int GradeCount;

    /** This array is used for keeping track of where grades start and stop in the
 *  EddingtonBasis
 *  GradeRange[j][0] is the first postion for a coefficient for grade j.
 *  GradeRange[j][1] is the last postion for a coefficient for grade j.
 */
    private int[][] GradeRange;

    /** 
 * Main constructor of ProductTable with signature information passed in.
 * It figures out the rest of what it needs.
 * @param pParent			Monad
 * @param pSig				String
 */
    public ProductTable(CladosObject pParent, String pSig) throws BadSignatureException {
        boolean check = this.validateSignature(pSig);
        if (check) {
            this.Parent = pParent;
            this.setLinearDimension();
            this.setGradeCount();
            this.setGradeRange();
            this.ABasis = new Basis(Parent.getName(), Parent.getAlgebraName(), Parent.getFootName(), getGradeCount() - 1);
            this.fillProductResult();
        } else {
            throw new BadSignatureException(pParent, "Valid signature was expected.");
        }
    }

    /** 
 * Return the signature of the generator geometry. This lists the squares of
 * the generators in their numeric order.
 * @return String
 */
    public String getSignature() {
        return this.Signature;
    }

    /** 
 * Get the linear dimension variable.
 * @return int
 */
    public int getLinearDimension() {
        return this.linearDim;
    }

    /** 
 * Get the grade count variable.
 * @return int
 */
    public int getGradeCount() {
        return this.GradeCount;
    }

    /** 
 * Return an element of the array holding the geometric multiplication rules.
 * @param	pj				int
 * @param	pk				int
 * @return int
 */
    public int getProductResult(int pj, int pk) {
        return this.ProductResult[pj][pk];
    }

    /** 
 * Get start index from the GradeRange array
 * GradeRange[j][0] is the first postion for a coefficient for grade j.
 * GradeRange[j][1] is the last postion for a coefficient for grade j.
 * @param	pGrade			int
 * @return int
 */
    public int getGradeRangeF(int pGrade) {
        return GradeRange[pGrade][0];
    }

    /** 
 * Get Final index from the GradeRange array
 * GradeRange[j][0] is the first postion for a coefficient for grade j.
 * GradeRange[j][1] is the last postion for a coefficient for grade j.
 * @param	pGrade			int
 * @return int
 */
    public int getGradeRangeB(int pGrade) {
        return GradeRange[pGrade][1];
    }

    /** 
 * Set the linear dimension variable using the length of a valid Signature
 * string.
 */
    private void setLinearDimension() {
        this.linearDim = (int) Math.pow(2, Signature.length());
    }

    /** 
 * Set the grade count variable using the length of a valid Signature 
 * string.
 */
    private void setGradeCount() {
        this.GradeCount = Signature.length() + 1;
    }

    /** 
 * Set the array used for keeping track of where grades start and stop in the
 * Coefficient array for this Monad.
 * GradeRange[j][0] is the first postion for a coefficient for grade j.
 * GradeRange[j][1] is the last postion for a coefficient for grade j.
 */
    private void setGradeRange() {
        this.GradeRange = new int[this.GradeCount][2];
        this.GradeRange[0][0] = 1;
        this.GradeRange[0][1] = 1;
        for (int j = 1; j < this.GradeCount; j++) {
            this.GradeRange[j][0] = this.GradeRange[j - 1][1] + 1;
            this.GradeRange[j][1] = this.GradeRange[j][0] + (factorial(this.GradeCount - 1) / (factorial(j) * factorial(this.GradeCount - 1 - j))) - 1;
        }
    }

    /** 
 * Set the array used for representing the geometric multiplication rules for
 * this Monad.  This method takes pairs of Eddington Basis elements, multiplies
 * them and figures out which other basis element is the result.  Standard
 * index commutation is performed while using the generator signatures to
 * eliminate pairs of indecies.  This method should only be called once when
 * the Monad is initialized.
 */
    private void fillProductResult() {
        this.ProductResult = new int[this.linearDim + 1][this.linearDim + 1];
        for (int j = 1; j < this.linearDim + 1; j++) {
            this.ProductResult[1][j] = j;
            this.ProductResult[j][1] = j;
        }
        int[] doubleSort = new int[2 * this.GradeCount - 1];
        int permuteCounter = 0;
        int doubleKey = 0;
        int j = 2;
        int k = 2;
        int m = 1;
        int n = 1;
        int tempSort = 0;
        for (j = 2; j < this.linearDim + 1; j++) {
            for (k = 2; k < this.linearDim + 1; k++) {
                permuteCounter = 0;
                doubleKey = 0;
                for (m = 1; m < this.GradeCount; m++) {
                    doubleSort[m] = this.ABasis.getBasis(j, m);
                    doubleSort[m + this.GradeCount - 1] = this.ABasis.getBasis(k, m);
                }
                m = 1;
                for (m = 1; m < 2 * this.GradeCount - 1; m++) {
                    for (n = 1; n < 2 * this.GradeCount - 2; n++) {
                        if (doubleSort[n] > doubleSort[n + 1]) {
                            tempSort = doubleSort[n];
                            doubleSort[n] = doubleSort[n + 1];
                            doubleSort[n + 1] = tempSort;
                            if (!(doubleSort[n] == 0 || doubleSort[n + 1] == 0)) {
                                permuteCounter += 1;
                            }
                        }
                    }
                    n = 1;
                }
                m = 1;
                permuteCounter = permuteCounter % 2;
                for (m = 1; m < 2 * this.GradeCount - 2; m++) {
                    if (doubleSort[m] == 0) continue;
                    if (doubleSort[m] == doubleSort[m + 1]) {
                        tempSort = doubleSort[m];
                        doubleSort[m] = 0;
                        doubleSort[m + 1] = 0;
                        m += 1;
                        permuteCounter += intSignature[tempSort - 1];
                    }
                }
                m = 1;
                permuteCounter = permuteCounter % 2;
                for (m = 1; m < 2 * this.GradeCount - 1; m++) {
                    for (n = 1; n < 2 * this.GradeCount - 2; n++) {
                        if (doubleSort[n] > doubleSort[n + 1]) {
                            tempSort = doubleSort[n];
                            doubleSort[n] = doubleSort[n + 1];
                            doubleSort[n + 1] = tempSort;
                            if (!(doubleSort[n] == 0 || doubleSort[n + 1] == 0)) {
                                permuteCounter += 1;
                            }
                        }
                    }
                    n = 1;
                }
                m = 1;
                permuteCounter = permuteCounter % 2;
                for (m = 1; m < 2 * this.GradeCount - 1; m++) {
                    doubleKey += (int) doubleSort[m] * Math.pow(this.GradeCount, 2 * this.GradeCount - 2 - m);
                }
                m = 1;
                this.ProductResult[j][k] = 0;
                for (m = 1; m < this.linearDim + 1; m++) {
                    if (doubleKey == this.ABasis.getBasisKey(m)) {
                        this.ProductResult[j][k] = m * (int) Math.pow(-1.0, permuteCounter);
                        break;
                    }
                }
                m = 1;
            }
            k = 2;
        }
    }

    /** 
 * Return a measure of the validitity of the Signature string.  A string with
 * +'s and -'s will pass.  No other one should.
 * @param pSg				String
 */
    private boolean validateSignature(String pSg) {
        intSignature = new int[pSg.length()];
        for (int j = 0; j < pSg.length(); j++) {
            if (pSg.substring(j, j + 1).equals("+")) {
                intSignature[j] = 0;
            } else {
                if (pSg.substring(j, j + 1).equals("-")) {
                    intSignature[j] = 1;
                } else {
                    return false;
                }
            }
        }
        this.Signature = pSg;
        return true;
    }

    /**
 * Private factorial function.  Couldn't find one in the JDK when this package
 * first needed it.
 * @param p					int
 */
    public static final int factorial(int p) {
        if (p <= 0) return 1;
        int temp = 1;
        for (int k = 1; k < p + 1; k++) {
            temp *= k;
        }
        return temp;
    }
}
