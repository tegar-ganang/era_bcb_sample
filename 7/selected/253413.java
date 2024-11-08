package rossi.dfp;

/**
 * <pre>
 *    Decimal floating point library for Java
 *
 *    Another floating point class.  This one is built using radix 10000
 *    which is 10&circ;4, so its almost decimal.
 *
 *    The design goals here are -
 *       1.) decimal math, or close to it.
 *       2.) Compile-time settable precision
 *       3.) Portability.  Code should be keep as portable as possible.
 *       4.) Performance
 *       5.) Accuracy  - Results should always be +/- 1 ULP for basic
 *           algebraic operation
 *       6.) Comply with IEEE 854-1987 as much as possible.
 *           (See IEEE 854-1987 notes below)
 *
 *    The trade offs -
 *       1.) Memory foot print.  I'm using more memory than necessary to
 *           represent numbers to get better performance.
 *       2.) Digits are bigger, so rounding is a greater loss.  So, if you
 *           really need 12 decimal digits, better use 4 base 10000 digits
 *           there can be one partially filled.
 *
 *    Numbers are represented  in the following form:
 *
 *    n  =  sign * mant * (radix) &circ; exp;
 *
 *    where sign is +/- 1, mantissa represents a fractional number between
 *    zero and one.  mant[0] is the least significant digit.
 *    exp is in the range of -32767 to 32768
 *
 *    dfp objects are immuatable via their public interface.
 *
 *    IEEE 854-1987  Notes and differences
 *
 *    IEEE 854 requires the radix to be either 2 or 10.  The radix here is
 *    10000, so that requirement is not met, but  it is possible that a
 *    subclassed can be made to make it behave as a radix 10
 *    number.  It is my opinion that if it looks and behaves as a radix
 *    10 number then it is one and that requirement would be met.
 *
 *    The radix of 10000 was chosen because it should be faster to operate
 *    on 4 decimal digits at once intead of one at a time.  Radix 10 behaviour
 *    can be realized by add an additional rounding step to ensure that
 *    the number of decimal digits represented is constant.
 *
 *    The IEEE standard specifically leaves out internal data encoding,
 *    so it is reasonable to conclude that such a subclass of this radix
 *    10000 system is merely an encoding of a radix 10 system.
 *
 *    IEEE 854 also specifies the existance of &quot;sub-normal&quot; numbers.  This
 *    class does not contain any such entities.  The most significant radix
 *    10000 digit is always non-zero.  Instead, we support &quot;gradual underflow&quot;
 *    by raising the underflow flag for numbers less with exponent less than
 *    expMin, but dont flush to zero until the exponent reaches expMin-DIGITS.
 *    Thus the smallest number we can represent would be:
 *    1E(-(minExp-DIGITS-1)*4),  eg, for DIGITS=5, minExp=-32767, that would
 *    be 1e-131092.
 *
 *    IEEE 854 defines that the implied radix point lies just to the right
 *    of the most significant digit and to the left of the remaining digits.
 *    This implementation puts the implied radix point to the left of all
 *    digits including the most significant one.  The most significant digit
 *    here is the one just to the right of the radix point.  This is a fine
 *    detail and is really only a matter of definition.  Any side effects of
 *    this can be rendered invisible by a subclass.
 * </pre>
 */
public class dfp {

    /** The mantissa */
    protected int[] mant;

    /** the sign bit. 1 for positive, -1 for negative */
    protected byte sign;

    /** the exponent. */
    protected int exp;

    /** Indicates non-finite / non-number values */
    protected byte nans;

    /** Current rounding mode */
    protected static int rMode = 4;

    /** IEEE 854-1987 signals */
    protected static int ieeeFlags = 0;

    /**
     * The number of digits. note these are radix 10000 digits, so each one is
     * equivilent to 4 decimal digits
     */
    public static final int DIGITS = 5;

    /** Each digit yeilds 4 decimal digits */
    private static final int LOG_10K = 4;

    /** The radix, or base of this system. Set to 10000 */
    public static final int radix = 10000;

    /**
     * The minium exponent before underflow is signaled. Flush to zero occurs at
     * minExp-DIGITS
     */
    public static final int minExp = -32767;

    /**
     * The maximum exponent before overflow is signaled and results flushed to
     * infinity
     */
    public static final int maxExp = 32768;

    /** The amount under/overflows are scaled by before going to trap handler */
    public static final int errScale = 32760;

    /** Rounds toward zero. I.E. truncation */
    public static final int ROUND_DOWN = 0;

    /** Rounds away from zero if discarded digit is non-zero */
    public static final int ROUND_UP = 1;

    /**
     * Rounds towards nearest unless both are equidistant in which case it
     * rounds away from zero
     */
    public static final int ROUND_HALF_UP = 2;

    /**
     * Rounds towards nearest unless both are equidistant in which case it
     * rounds toward zero
     */
    public static final int ROUND_HALF_DOWN = 3;

    /**
     * Rounds towards nearest unless both are equidistant in which case it
     * rounds toward the even neighbor. This is the default as specified by IEEE
     * 854-1987
     */
    public static final int ROUND_HALF_EVEN = 4;

    /**
     * Rounds towards nearest unless both are equidistant in which case it
     * rounds toward the odd neighbor.
     */
    public static final int ROUND_HALF_ODD = 5;

    /** Rounds towards positive infinity */
    public static final int ROUND_CEIL = 6;

    /** Rounds towards negative infinity */
    public static final int ROUND_FLOOR = 7;

    /** Normal finite numbers */
    public static final byte FINITE = 0;

    /** Infinity */
    public static final byte INFINITE = 1;

    /** Signaling NaN */
    public static final byte SNAN = 2;

    /** Quiet NaN */
    public static final byte QNAN = 3;

    /** Invalid operation */
    public static final int FLAG_INVALID = 1;

    /** Division by zero */
    public static final int FLAG_DIV_ZERO = 2;

    /** Overflow */
    public static final int FLAG_OVERFLOW = 4;

    /** Underflow */
    public static final int FLAG_UNDERFLOW = 8;

    /** Inexact */
    public static final int FLAG_INEXACT = 16;

    /** dfp equivalent to numeric 0 */
    public static final dfp zero = new dfp();

    /** dfp equivalent to numeric 1 */
    public static final dfp one = new dfp("1");

    /** dfp equivalent to numeric 2 */
    public static final dfp two = new dfp("2");

    /** Default constructor. Makes a dfp with a value of zero */
    public dfp() {
        mant = new int[DIGITS];
        for (int i = DIGITS - 1; i >= 0; i--) mant[i] = 0;
        sign = 1;
        exp = 0;
        nans = FINITE;
    }

    /** Copy constructor. Creates a copy of the supplied dfp */
    public dfp(dfp d) {
        mant = new int[DIGITS];
        for (int i = DIGITS - 1; i >= 0; i--) {
            mant[i] = d.mant[i];
        }
        sign = d.sign;
        exp = d.exp;
        nans = d.nans;
    }

    /** Create a dfp given a String representation */
    public dfp(String s) {
        dfp r = string2dfp(s);
        this.mant = r.mant;
        this.exp = r.exp;
        this.sign = r.sign;
        this.nans = r.nans;
    }

    /**
     * Create a dfp. Use this internally in preferenct to constructors to
     * facilitate subclasses.
     */
    public dfp newInstance(dfp d) {
        return new dfp(d);
    }

    /**
     * Create a dfp. Use this internally in preferenct to constructors to
     * facilitate subclasses.
     */
    public dfp newInstance(String s) {
        return new dfp(s);
    }

    /** Shift the mantissa left, and adjust the exponent to compensate */
    protected void shiftLeft() {
        for (int i = DIGITS - 1; i > 0; i--) mant[i] = mant[i - 1];
        mant[0] = 0;
        exp--;
    }

    /** Shift the mantissa right, and adjust the exponent to compensate */
    protected void shiftRight() {
        for (int i = 0; i < DIGITS - 1; i++) mant[i] = mant[i + 1];
        mant[DIGITS - 1] = 0;
        exp++;
    }

    /**
     * Make our exp equal to the supplied one. This may cause rounding. Also
     * causes de-normalized numbers. These numbers are generally dangerous
     * because most routines assume normalized numbers. Align doesn't round, so
     * it will return the last digit destroyed by shifting right.
     */
    protected int align(int e) {
        int diff;
        int adiff;
        int lostdigit = 0;
        boolean inexact = false;
        diff = exp - e;
        adiff = diff;
        if (adiff < 0) adiff = -adiff;
        if (diff == 0) return 0;
        if (adiff > (DIGITS + 1)) {
            for (int i = DIGITS - 1; i >= 0; i--) mant[i] = 0;
            exp = e;
            ieeeFlags |= FLAG_INEXACT;
            dotrap(FLAG_INEXACT, "align", this, this);
            return 0;
        }
        for (int i = 0; i < adiff; i++) {
            if (diff < 0) {
                if (lostdigit != 0) inexact = true;
                lostdigit = mant[0];
                shiftRight();
            } else shiftLeft();
        }
        if (inexact) {
            ieeeFlags |= FLAG_INEXACT;
            dotrap(FLAG_INEXACT, "align", this, this);
        }
        return lostdigit;
    }

    /**
     * returns true if this is less than x. returns false if this or x is NaN
     */
    public boolean lessThan(dfp x) {
        if (nans == SNAN || nans == QNAN || x.nans == SNAN || x.nans == QNAN) {
            ieeeFlags |= FLAG_INVALID;
            dotrap(FLAG_INVALID, "lessThan", x, newInstance(zero));
            return false;
        }
        return (compare(this, x) < 0);
    }

    /**
     * returns true if this is greater than x. returns false if this or x is NaN
     */
    public boolean greaterThan(dfp x) {
        if (nans == SNAN || nans == QNAN || x.nans == SNAN || x.nans == QNAN) {
            ieeeFlags |= FLAG_INVALID;
            dotrap(FLAG_INVALID, "lessThan", x, newInstance(zero));
            return false;
        }
        return (compare(this, x) > 0);
    }

    /**
     * returns true if this is equal to x. returns false if this or x is NaN
     */
    public boolean equal(dfp x) {
        if (nans == SNAN || nans == QNAN || x.nans == SNAN || x.nans == QNAN) return false;
        return (compare(this, x) == 0);
    }

    /**
     * returns true if this is not equal to x. different from !equal(x) in the
     * way NaNs are handled.
     */
    public boolean unequal(dfp x) {
        if (nans == SNAN || nans == QNAN || x.nans == SNAN || x.nans == QNAN) return false;
        return (greaterThan(x) || lessThan(x));
    }

    /**
     * compare a and b. return -1 if a<b, 1 if a>b and 0 if a==b Note this
     * method does not properly handle NaNs.
     */
    protected static int compare(dfp a, dfp b) {
        if (a.mant[DIGITS - 1] == 0 && b.mant[DIGITS - 1] == 0 && a.nans == FINITE && b.nans == FINITE) return 0;
        if (a.sign != b.sign) {
            if (a.sign == -1) return -1; else return 1;
        }
        if (a.nans == INFINITE && b.nans == FINITE) return a.sign;
        if (a.nans == FINITE && b.nans == INFINITE) return -b.sign;
        if (a.nans == INFINITE && b.nans == INFINITE) return 0;
        if (b.mant[DIGITS - 1] != 0 && a.mant[DIGITS - 1] != 0) {
            if (a.exp < b.exp) return -a.sign;
            if (a.exp > b.exp) return a.sign;
        }
        for (int i = DIGITS - 1; i >= 0; i--) {
            if (a.mant[i] > b.mant[i]) return a.sign;
            if (a.mant[i] < b.mant[i]) return -a.sign;
        }
        return 0;
    }

    /**
     * Round to nearest integer using the round-half-even method. That is round
     * to nearest integer unless both are equidistant. In which case round to
     * the even one.
     */
    public dfp rint() {
        return trunc(ROUND_HALF_EVEN);
    }

    /**
     * Round to an integer using the round floor mode. That is, round toward
     * -Infinity
     */
    public dfp floor() {
        return trunc(ROUND_FLOOR);
    }

    /**
     * Round to an integer using the ceil floor mode. That is, round toward
     * +Infinity
     */
    public dfp ceil() {
        return trunc(ROUND_CEIL);
    }

    /**
     * Returns the IEEE remainder. That is the result of this less n times d,
     * where n is the integer closest to this/d.
     */
    public dfp remainder(dfp d) {
        dfp result;
        result = this.subtract(this.divide(d).rint().multiply(d));
        if (result.mant[DIGITS - 1] == 0) result.sign = sign;
        return result;
    }

    /** Does the integer conversions with the spec rounding. */
    protected dfp trunc(int rmode) {
        dfp result, a, half;
        boolean changed = false;
        if (nans == SNAN || nans == QNAN || nans == INFINITE) return newInstance(this);
        if (mant[DIGITS - 1] == 0) return newInstance(this);
        if (exp < 0) {
            ieeeFlags |= FLAG_INEXACT;
            result = newInstance(zero);
            result = dotrap(FLAG_INEXACT, "trunc", this, result);
            return result;
        }
        if (exp >= DIGITS) return newInstance(this);
        result = newInstance(this);
        for (int i = 0; i < (DIGITS - result.exp); i++) {
            changed |= (result.mant[i] != 0);
            result.mant[i] = 0;
        }
        if (changed) {
            switch(rmode) {
                case ROUND_FLOOR:
                    if (result.sign == -1) result = result.add(newInstance("-1"));
                    break;
                case ROUND_CEIL:
                    if (result.sign == 1) result = result.add(one);
                    break;
                case ROUND_HALF_EVEN:
                default:
                    half = newInstance("0.5");
                    a = subtract(result);
                    a.sign = 1;
                    if (a.greaterThan(half)) {
                        a = newInstance(one);
                        a.sign = sign;
                        result = result.add(a);
                    }
                    if (a.equal(half) && result.exp > 0 && (result.mant[DIGITS - result.exp] & 1) != 0) {
                        a = newInstance(one);
                        a.sign = sign;
                        result = result.add(a);
                    }
                    break;
            }
            ieeeFlags |= FLAG_INEXACT;
            result = dotrap(FLAG_INEXACT, "trunc", this, result);
            return result;
        }
        return result;
    }

    /**
     * Convert this to an integer. If greater than 2147483647, it returns
     * 2147483647. If less than -2147483648 it returns -2147483648.
     */
    public int intValue() {
        dfp rounded;
        int result = 0;
        rounded = rint();
        if (rounded.greaterThan(newInstance("2147483647"))) return 2147483647;
        if (rounded.lessThan(newInstance("-2147483648"))) return -2147483648;
        for (int i = DIGITS - 1; i >= DIGITS - rounded.exp; i--) result = result * radix + rounded.mant[i];
        if (rounded.sign == -1) result = -result;
        return result;
    }

    /**
     * Returns the exponent of the greatest power of 10000 that is less than or
     * equal to the absolute value of this. I.E. if this is 10e6 then log10K
     * would return 1.
     */
    public int log10K() {
        return exp - 1;
    }

    /** Return the specified power of 10000 */
    public dfp power10K(int e) {
        dfp d = newInstance(one);
        d.exp = e + 1;
        return d;
    }

    /**
     * Return the exponent of the greatest power of 10 that is less than or
     * equal to than abs(this).
     */
    public int log10() {
        int mantMSB = mant[DIGITS - 1];
        if (mantMSB >= 1000) return exp * LOG_10K - 1;
        if (mantMSB >= 100) return exp * LOG_10K - 2;
        if (mantMSB >= 10) return exp * LOG_10K - 3;
        return (exp - 1) * LOG_10K;
    }

    /** Return the specified power of 10 */
    public dfp power10(int e) {
        dfp d = newInstance(one);
        if (e >= 0) d.exp = e / LOG_10K + 1; else d.exp = (e + 1) / LOG_10K;
        switch((e % LOG_10K + LOG_10K) % LOG_10K) {
            case 0:
                break;
            case 1:
                d = d.multiply(10);
                break;
            case 2:
                d = d.multiply(100);
                break;
            case 3:
                d = d.multiply(1000);
                break;
        }
        return d;
    }

    /**
     * Negate the mantissa of this by computing the complement. Leaves the sign
     * bit unchanged, used internally by add. Denormalized numbers are handled
     * properly here.
     */
    protected int complement(int extra) {
        int r, rl, rh;
        extra = radix - extra;
        for (int i = 0; i < DIGITS; i++) mant[i] = radix - mant[i] - 1;
        rh = extra / radix;
        extra = extra % radix;
        for (int i = 0; i < DIGITS; i++) {
            r = mant[i] + rh;
            rl = r % radix;
            rh = r / radix;
            mant[i] = rl;
        }
        return extra;
    }

    /** Add x to this and return the result */
    public dfp add(dfp x) {
        int r, rh, rl;
        dfp a, b, result;
        byte asign, bsign, rsign;
        int aextradigit = 0, bextradigit = 0;
        if (nans != FINITE || x.nans != FINITE) {
            if (nans == QNAN || nans == SNAN) return this;
            if (x.nans == QNAN || x.nans == SNAN) return x;
            if (nans == INFINITE && x.nans == FINITE) return this;
            if (x.nans == INFINITE && nans == FINITE) return x;
            if (x.nans == INFINITE && nans == INFINITE && sign == x.sign) return x;
            if (x.nans == INFINITE && nans == INFINITE && sign != x.sign) {
                ieeeFlags |= FLAG_INVALID;
                result = newInstance(zero);
                result.nans = QNAN;
                result = dotrap(FLAG_INVALID, "add", x, result);
                return result;
            }
        }
        a = newInstance(this);
        b = newInstance(x);
        result = newInstance(zero);
        asign = a.sign;
        bsign = b.sign;
        a.sign = 1;
        b.sign = 1;
        rsign = bsign;
        if (compare(a, b) > 0) rsign = asign;
        if (b.mant[DIGITS - 1] == 0) b.exp = a.exp;
        if (a.mant[DIGITS - 1] == 0) a.exp = b.exp;
        if (a.exp < b.exp) aextradigit = a.align(b.exp); else bextradigit = b.align(a.exp);
        if (asign != bsign) {
            if (asign == rsign) bextradigit = b.complement(bextradigit); else aextradigit = a.complement(aextradigit);
        }
        rh = 0;
        for (int i = 0; i < DIGITS; i++) {
            r = a.mant[i] + b.mant[i] + rh;
            rl = r % radix;
            rh = r / radix;
            result.mant[i] = rl;
        }
        result.exp = a.exp;
        result.sign = rsign;
        if (rh != 0 && (asign == bsign)) {
            int lostdigit = result.mant[0];
            result.shiftRight();
            result.mant[DIGITS - 1] = rh;
            int excp = result.round(lostdigit);
            if (excp != 0) result = dotrap(excp, "add", x, result);
        }
        for (int i = 0; i < DIGITS; i++) {
            if (result.mant[DIGITS - 1] != 0) break;
            result.shiftLeft();
            if (i == 0) {
                result.mant[0] = aextradigit + bextradigit;
                aextradigit = 0;
                bextradigit = 0;
            }
        }
        if (result.mant[DIGITS - 1] == 0) {
            result.exp = 0;
            if (asign != bsign) result.sign = 1;
        }
        int excp = result.round((aextradigit + bextradigit));
        if (excp != 0) result = dotrap(excp, "add", x, result);
        return result;
    }

    /**
     * Returns a number that is this number with the sign bit reversed
     */
    public dfp negate() {
        dfp result = newInstance(this);
        result.sign = (byte) -result.sign;
        return result;
    }

    /** Subtract a from this */
    public dfp subtract(dfp a) {
        return add(a.negate());
    }

    /**
     * round this given the next digit n using the current rounding mode returns
     * a flag if an exception occured
     */
    protected int round(int n) {
        int r, rh, rl;
        boolean inc = false;
        switch(rMode) {
            case ROUND_DOWN:
                inc = false;
                break;
            case ROUND_UP:
                inc = (n != 0);
                break;
            case ROUND_HALF_UP:
                inc = (n >= 5000);
                break;
            case ROUND_HALF_DOWN:
                inc = (n > 5000);
                break;
            case ROUND_HALF_EVEN:
                inc = (n > 5000 || (n == 5000 && (mant[0] & 1) == 1));
                break;
            case ROUND_HALF_ODD:
                inc = (n > 5000 || (n == 5000 && (mant[0] & 1) == 0));
                break;
            case ROUND_CEIL:
                inc = (sign == 1 && n != 0);
                break;
            case ROUND_FLOOR:
                inc = (sign == -1 && n != 0);
                break;
        }
        if (inc) {
            rh = 1;
            for (int i = 0; i < DIGITS; i++) {
                r = mant[i] + rh;
                rh = r / radix;
                rl = r % radix;
                mant[i] = rl;
            }
            if (rh != 0) {
                shiftRight();
                mant[DIGITS - 1] = rh;
            }
        }
        if (exp < minExp) {
            ieeeFlags |= FLAG_UNDERFLOW;
            return FLAG_UNDERFLOW;
        }
        if (exp > maxExp) {
            ieeeFlags |= FLAG_OVERFLOW;
            return FLAG_OVERFLOW;
        }
        if (n != 0) {
            ieeeFlags |= FLAG_INEXACT;
            return FLAG_INEXACT;
        }
        return 0;
    }

    /** Multiply this by x */
    public dfp multiply(dfp x) {
        int product[];
        int r, rh, rl;
        int md = 0;
        int excp;
        dfp result = newInstance(zero);
        if (nans != FINITE || x.nans != FINITE) {
            if (nans == QNAN || nans == SNAN) return this;
            if (x.nans == QNAN || x.nans == SNAN) return x;
            if (nans == INFINITE && x.nans == FINITE && x.mant[DIGITS - 1] != 0) {
                result = newInstance(this);
                result.sign = (byte) (sign * x.sign);
                return result;
            }
            if (x.nans == INFINITE && nans == FINITE && mant[DIGITS - 1] != 0) {
                result = newInstance(x);
                result.sign = (byte) (sign * x.sign);
                return result;
            }
            if (x.nans == INFINITE && nans == INFINITE) {
                result = newInstance(this);
                result.sign = (byte) (sign * x.sign);
                return result;
            }
            if ((x.nans == INFINITE && nans == FINITE && mant[DIGITS - 1] == 0) || (nans == INFINITE && x.nans == FINITE && x.mant[DIGITS - 1] == 0)) {
                ieeeFlags |= FLAG_INVALID;
                result = newInstance(zero);
                result.nans = QNAN;
                result = dotrap(FLAG_INVALID, "multiply", x, result);
                return result;
            }
        }
        product = new int[DIGITS * 2];
        for (int i = 0; i < DIGITS * 2; i++) product[i] = 0;
        for (int i = 0; i < DIGITS; i++) {
            rh = 0;
            for (int j = 0; j < DIGITS; j++) {
                r = mant[i] * x.mant[j];
                r = r + product[i + j] + rh;
                rl = r % radix;
                rh = r / radix;
                product[i + j] = rl;
            }
            product[i + DIGITS] = rh;
        }
        md = DIGITS * 2 - 1;
        for (int i = DIGITS * 2 - 1; i >= 0; i--) {
            if (product[i] != 0) {
                md = i;
                break;
            }
        }
        for (int i = 0; i < DIGITS; i++) result.mant[DIGITS - i - 1] = product[md - i];
        result.exp = (exp + x.exp + md - 2 * DIGITS + 1);
        result.sign = (byte) ((sign == x.sign) ? 1 : -1);
        if (result.mant[DIGITS - 1] == 0) result.exp = 0;
        if (md > (DIGITS - 1)) excp = result.round(product[md - DIGITS]); else excp = result.round(0);
        if (excp != 0) result = dotrap(excp, "multiply", x, result);
        return result;
    }

    /**
     * Multiply this by a single digit 0&lt;=x&lt;radix. There are speed
     * advantages in this special case
     */
    public dfp multiply(int x) {
        int r, rh, rl;
        int excp;
        int lostdigit;
        dfp result = newInstance(this);
        if (nans != FINITE) {
            if (nans == QNAN || nans == SNAN) return this;
            if (nans == INFINITE && x != 0) {
                result = newInstance(this);
                return result;
            }
            if (nans == INFINITE && x == 0) {
                ieeeFlags |= FLAG_INVALID;
                result = newInstance(zero);
                result.nans = QNAN;
                result = dotrap(FLAG_INVALID, "multiply", newInstance(zero), result);
                return result;
            }
        }
        if (x < 0 || x >= radix) {
            ieeeFlags |= FLAG_INVALID;
            result = newInstance(zero);
            result.nans = QNAN;
            result = dotrap(FLAG_INVALID, "multiply", result, result);
            return result;
        }
        rh = 0;
        for (int i = 0; i < DIGITS; i++) {
            r = mant[i] * x + rh;
            rl = r % radix;
            rh = r / radix;
            result.mant[i] = rl;
        }
        lostdigit = 0;
        if (rh != 0) {
            lostdigit = result.mant[0];
            result.shiftRight();
            result.mant[DIGITS - 1] = rh;
        }
        if (result.mant[DIGITS - 1] == 0) result.exp = 0;
        excp = result.round(lostdigit);
        if (excp != 0) result = dotrap(excp, "multiply", result, result);
        return result;
    }

    /** Divide this by divisor */
    public dfp divide(dfp divisor) {
        int dividend[];
        int quotient[];
        int remainder[];
        int qd;
        int nsqd;
        int trial = 0;
        int min, max;
        int minadj;
        boolean trialgood;
        int r, rh, rl;
        int md = 0;
        int excp;
        dfp result = newInstance(zero);
        if (nans != FINITE || divisor.nans != FINITE) {
            if (nans == QNAN || nans == SNAN) return this;
            if (divisor.nans == QNAN || divisor.nans == SNAN) return divisor;
            if (nans == INFINITE && divisor.nans == FINITE) {
                result = newInstance(this);
                result.sign = (byte) (sign * divisor.sign);
                return result;
            }
            if (divisor.nans == INFINITE && nans == FINITE) {
                result = newInstance(zero);
                result.sign = (byte) (sign * divisor.sign);
                return result;
            }
            if (divisor.nans == INFINITE && nans == INFINITE) {
                ieeeFlags |= FLAG_INVALID;
                result = newInstance(zero);
                result.nans = QNAN;
                result = dotrap(FLAG_INVALID, "divide", divisor, result);
                return result;
            }
        }
        if (divisor.mant[DIGITS - 1] == 0) {
            ieeeFlags |= FLAG_DIV_ZERO;
            result = newInstance(zero);
            result.sign = (byte) (sign * divisor.sign);
            result.nans = INFINITE;
            result = dotrap(FLAG_DIV_ZERO, "divide", divisor, result);
            return result;
        }
        dividend = new int[DIGITS + 1];
        quotient = new int[DIGITS + 2];
        remainder = new int[DIGITS + 1];
        dividend[DIGITS] = 0;
        quotient[DIGITS] = 0;
        quotient[DIGITS + 1] = 0;
        remainder[DIGITS] = 0;
        for (int i = 0; i < DIGITS; i++) {
            dividend[i] = mant[i];
            quotient[i] = 0;
            remainder[i] = 0;
        }
        nsqd = 0;
        for (qd = DIGITS + 1; qd >= 0; qd--) {
            r = dividend[DIGITS] * radix + dividend[DIGITS - 1];
            min = r / (divisor.mant[DIGITS - 1] + 1);
            max = (r + 1) / divisor.mant[DIGITS - 1];
            trialgood = false;
            while (!trialgood) {
                trial = (min + max) / 2;
                rh = 0;
                for (int i = 0; i < (DIGITS + 1); i++) {
                    int dm = (i < DIGITS) ? divisor.mant[i] : 0;
                    r = (dm * trial) + rh;
                    rh = r / radix;
                    rl = r % radix;
                    remainder[i] = rl;
                }
                rh = 1;
                for (int i = 0; i < (DIGITS + 1); i++) {
                    r = ((radix - 1) - remainder[i]) + dividend[i] + rh;
                    rh = r / radix;
                    rl = r % radix;
                    remainder[i] = rl;
                }
                if (rh == 0) {
                    max = trial - 1;
                    continue;
                }
                minadj = (remainder[DIGITS] * radix) + remainder[DIGITS - 1];
                minadj = minadj / (divisor.mant[DIGITS - 1] + 1);
                if (minadj >= 2) {
                    min = trial + minadj;
                    continue;
                }
                trialgood = false;
                for (int i = (DIGITS - 1); i >= 0; i--) {
                    if (divisor.mant[i] > remainder[i]) trialgood = true;
                    if (divisor.mant[i] < remainder[i]) break;
                }
                if (remainder[DIGITS] != 0) trialgood = false;
                if (trialgood == false) min = trial + 1;
            }
            quotient[qd] = trial;
            if (trial != 0 || nsqd != 0) nsqd++;
            if (rMode == ROUND_DOWN && nsqd == DIGITS) break;
            if (nsqd > DIGITS) break;
            dividend[0] = 0;
            for (int i = 0; i < DIGITS; i++) dividend[i + 1] = remainder[i];
        }
        md = DIGITS;
        for (int i = DIGITS + 1; i >= 0; i--) {
            if (quotient[i] != 0) {
                md = i;
                break;
            }
        }
        for (int i = 0; i < DIGITS; i++) result.mant[DIGITS - i - 1] = quotient[md - i];
        result.exp = (exp - divisor.exp + md - DIGITS + 1 - 1);
        result.sign = (byte) ((sign == divisor.sign) ? 1 : -1);
        if (result.mant[DIGITS - 1] == 0) result.exp = 0;
        if (md > (DIGITS - 1)) excp = result.round(quotient[md - DIGITS]); else excp = result.round(0);
        if (excp != 0) result = dotrap(excp, "divide", divisor, result);
        return result;
    }

    /**
     * Divide by a single digit less than radix. Special case, so there are
     * speed advantages. 0 &lt;= divisor &lt; radix
     */
    public dfp divide(int divisor) {
        dfp result;
        int r, rh, rl;
        int excp;
        if (nans != FINITE) {
            if (nans == QNAN || nans == SNAN) return this;
            if (nans == INFINITE) {
                result = newInstance(this);
                return result;
            }
        }
        if (divisor == 0) {
            ieeeFlags |= FLAG_DIV_ZERO;
            result = newInstance(zero);
            result.sign = sign;
            result.nans = INFINITE;
            result = dotrap(FLAG_DIV_ZERO, "divide", zero, result);
            return result;
        }
        if (divisor < 0 || divisor >= radix) {
            ieeeFlags |= FLAG_INVALID;
            result = newInstance(zero);
            result.nans = QNAN;
            result = dotrap(FLAG_INVALID, "divide", result, result);
            return result;
        }
        result = newInstance(this);
        rl = 0;
        for (int i = DIGITS - 1; i >= 0; i--) {
            r = rl * radix + result.mant[i];
            rh = r / divisor;
            rl = r % divisor;
            result.mant[i] = rh;
        }
        if (result.mant[DIGITS - 1] == 0) {
            result.shiftLeft();
            r = rl * radix;
            rh = r / divisor;
            rl = r % divisor;
            result.mant[0] = rh;
        }
        excp = result.round(rl * radix / divisor);
        if (excp != 0) result = dotrap(excp, "divide", result, result);
        return result;
    }

    public dfp sqrt() {
        dfp x, dx, px;
        if (nans == FINITE && mant[DIGITS - 1] == 0) return newInstance(this);
        if (nans != FINITE) {
            if (nans == INFINITE && sign == 1) return newInstance(this);
            if (nans == QNAN) return newInstance(this);
            if (nans == SNAN) {
                dfp result;
                ieeeFlags |= FLAG_INVALID;
                result = newInstance(this);
                result = dotrap(FLAG_INVALID, "sqrt", null, result);
                return result;
            }
        }
        if (sign == -1) {
            dfp result;
            ieeeFlags |= FLAG_INVALID;
            result = newInstance(this);
            result.nans = QNAN;
            result = dotrap(FLAG_INVALID, "sqrt", null, result);
            return result;
        }
        x = newInstance(this);
        if (x.exp < -1 || x.exp > 1) x.exp = (this.exp / 2);
        switch(x.mant[DIGITS - 1] / 2000) {
            case 0:
                x.mant[DIGITS - 1] = x.mant[DIGITS - 1] / 2 + 1;
                break;
            case 2:
                x.mant[DIGITS - 1] = 1500;
                break;
            case 3:
                x.mant[DIGITS - 1] = 2200;
                break;
            case 4:
                x.mant[DIGITS - 1] = 3000;
                break;
        }
        dx = newInstance(x);
        px = zero;
        while (x.unequal(px)) {
            dx = newInstance(x);
            dx.sign = -1;
            dx = dx.add(this.divide(x));
            dx = dx.divide(2);
            px = x;
            x = x.add(dx);
            if (dx.mant[DIGITS - 1] == 0) break;
        }
        return x;
    }

    public String toString() {
        if (nans != FINITE) {
            switch(sign * nans) {
                case INFINITE:
                    return "Infinity";
                case -INFINITE:
                    return "-Infinity";
                case QNAN:
                    return "NaN";
                case -QNAN:
                    return "NaN";
                case SNAN:
                    return "NaN";
                case -SNAN:
                    return "NaN";
            }
        }
        if (exp > DIGITS || exp < -1) {
            return dfp2sci(this);
        }
        return dfp2string(this);
    }

    protected String dfp2sci(dfp a) {
        char rawdigits[] = new char[DIGITS * LOG_10K];
        char outputbuffer[] = new char[DIGITS * LOG_10K + 20];
        int p;
        int q;
        int e;
        int ae;
        int shf;
        p = 0;
        for (int i = DIGITS - 1; i >= 0; i--) {
            rawdigits[p++] = (char) ((a.mant[i] / 1000) + '0');
            rawdigits[p++] = (char) (((a.mant[i] / 100) % 10) + '0');
            rawdigits[p++] = (char) (((a.mant[i] / 10) % 10) + '0');
            rawdigits[p++] = (char) (((a.mant[i]) % 10) + '0');
        }
        for (p = 0; p < rawdigits.length; p++) if (rawdigits[p] != '0') break;
        shf = p;
        q = 0;
        if (a.sign == -1) outputbuffer[q++] = '-';
        if (p != rawdigits.length) {
            outputbuffer[q++] = rawdigits[p++];
            outputbuffer[q++] = '.';
            while (p < rawdigits.length) outputbuffer[q++] = rawdigits[p++];
        } else {
            outputbuffer[q++] = '0';
            outputbuffer[q++] = '.';
            outputbuffer[q++] = '0';
            outputbuffer[q++] = 'e';
            outputbuffer[q++] = '0';
            return new String(outputbuffer, 0, 5);
        }
        outputbuffer[q++] = 'e';
        e = a.exp * LOG_10K - shf - 1;
        ae = e;
        if (e < 0) ae = -e;
        for (p = 1000000000; p > ae; p /= 10) ;
        if (e < 0) outputbuffer[q++] = '-';
        while (p > 0) {
            outputbuffer[q++] = (char) (ae / p + '0');
            ae = ae % p;
            p = p / 10;
        }
        return new String(outputbuffer, 0, q);
    }

    protected String dfp2string(dfp a) {
        char buffer[] = new char[DIGITS * LOG_10K + 20];
        int p = 1;
        int q;
        int e = a.exp;
        boolean pointInserted = false;
        buffer[0] = ' ';
        if (e <= 0) {
            buffer[p++] = '0';
            buffer[p++] = '.';
            pointInserted = true;
        }
        while (e < 0) {
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            e++;
        }
        for (int i = DIGITS - 1; i >= 0; i--) {
            buffer[p++] = (char) ((a.mant[i] / 1000) + '0');
            buffer[p++] = (char) (((a.mant[i] / 100) % 10) + '0');
            buffer[p++] = (char) (((a.mant[i] / 10) % 10) + '0');
            buffer[p++] = (char) (((a.mant[i]) % 10) + '0');
            if (--e == 0) {
                buffer[p++] = '.';
                pointInserted = true;
            }
        }
        while (e > 0) {
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            buffer[p++] = '0';
            e--;
        }
        if (!pointInserted) buffer[p++] = '.';
        q = 1;
        while (buffer[q] == '0') q++;
        if (buffer[q] == '.') q--;
        while (buffer[p - 1] == '0') p--;
        if (a.sign < 0) buffer[--q] = '-';
        String output = new String(buffer, q, p - q);
        return (output != null && output.length() > 0) ? output : "0";
    }

    protected dfp string2dfp(String fpin) {
        String fpdecimal;
        int trailing_zeros;
        int significant_digits;
        int p, q;
        char Striped[];
        dfp result;
        boolean decimalFound = false;
        int decimalPos = 0;
        final int rsize = LOG_10K;
        final int offset = LOG_10K;
        int sciexp = 0;
        int i;
        Striped = new char[DIGITS * rsize + offset * 2];
        if (fpin.equals("Infinite")) return create((byte) 1, (byte) INFINITE);
        if (fpin.equals("-Infinite")) return create((byte) -1, (byte) INFINITE);
        if (fpin.equals("NaN")) return create((byte) 1, (byte) QNAN);
        result = newInstance(zero);
        p = fpin.indexOf("e");
        if (p == -1) p = fpin.indexOf("E");
        if (p != -1) {
            fpdecimal = fpin.substring(0, p);
            String fpexp = fpin.substring(p + 1);
            boolean negative = false;
            sciexp = 0;
            for (i = 0; i < fpexp.length(); i++) {
                if (fpexp.charAt(i) == '-') {
                    negative = true;
                    continue;
                }
                if (fpexp.charAt(i) >= '0' && fpexp.charAt(i) <= '9') sciexp = sciexp * 10 + fpexp.charAt(i) - '0';
            }
            if (negative) sciexp = -sciexp;
        } else {
            fpdecimal = fpin;
        }
        if (fpdecimal.indexOf("-") != -1) result.sign = -1;
        p = 0;
        for (; ; ) {
            if (fpdecimal.charAt(p) >= '1' && fpdecimal.charAt(p) <= '9') break;
            if (decimalFound && fpdecimal.charAt(p) == '0') decimalPos--;
            if (fpdecimal.charAt(p) == '.') decimalFound = true;
            p++;
            if (p == fpdecimal.length()) break;
        }
        q = offset;
        Striped[0] = '0';
        Striped[1] = '0';
        Striped[2] = '0';
        Striped[3] = '0';
        significant_digits = 0;
        for (; ; ) {
            if (p == (fpdecimal.length())) break;
            if (q == DIGITS * rsize + offset + 1) break;
            if (fpdecimal.charAt(p) == '.') {
                decimalFound = true;
                decimalPos = significant_digits;
                p++;
                continue;
            }
            if (fpdecimal.charAt(p) < '0' || fpdecimal.charAt(p) > '9') {
                p++;
                continue;
            }
            Striped[q] = fpdecimal.charAt(p);
            q++;
            p++;
            significant_digits++;
        }
        if (decimalFound && q != offset) {
            for (; ; ) {
                q--;
                if (q == offset) break;
                if (Striped[q] == '0') {
                    significant_digits--;
                } else {
                    break;
                }
            }
        }
        if (decimalFound && significant_digits == 0) decimalPos = 0;
        if (!decimalFound) decimalPos = q - offset;
        q = offset;
        p = significant_digits - 1 + offset;
        trailing_zeros = 0;
        while (p > q) {
            if (Striped[p] != '0') break;
            trailing_zeros++;
            p--;
        }
        i = (((rsize * 100) - decimalPos - sciexp % rsize) % rsize);
        q -= i;
        decimalPos += i;
        while ((p - q) < (DIGITS * rsize)) {
            for (i = 0; i < rsize; i++) Striped[++p] = '0';
        }
        for (i = (DIGITS - 1); i >= 0; i--) {
            result.mant[i] = (Striped[q] - '0') * 1000 + (Striped[q + 1] - '0') * 100 + (Striped[q + 2] - '0') * 10 + (Striped[q + 3] - '0');
            q += LOG_10K;
        }
        result.exp = ((decimalPos + sciexp) / rsize);
        if (q < Striped.length) result.round((Striped[q] - '0') * 1000);
        return result;
    }

    /**
     * Set the rounding mode to be one of the following values: ROUND_UP,
     * ROUND_DOWN, ROUND_HALF_UP, ROUND_HALF_DOWN, ROUND_HALF_EVEN,
     * ROUND_HALF_ODD, ROUND_CEIL, ROUND_FLOOR.
     *
     * Default is ROUND_HALF_EVEN
     *
     * Note that the rounding mode is common to all instances in the system and
     * will effect all future calculations.
     */
    public static void setRoundingMode(int mode) {
        rMode = mode;
    }

    /** Returns the current rounding mode */
    public static int getRoundingMode() {
        return rMode;
    }

    /**
     * Raises a trap. This does not set the corresponding flag however.
     *
     * @param type
     *            the trap type
     * @param what -
     *            name of routine trap occured in
     * @param oper -
     *            input operator to function
     * @param result -
     *            the result computed prior to the trap
     * @return The suggested return value from the trap handler
     */
    public dfp dotrap(int type, String what, dfp oper, dfp result) {
        dfp def = result;
        switch(type) {
            case FLAG_INVALID:
                def = newInstance(zero);
                def.sign = result.sign;
                def.nans = QNAN;
                break;
            case FLAG_DIV_ZERO:
                if (nans == FINITE && mant[DIGITS - 1] != 0) {
                    def = newInstance(zero);
                    def.sign = (byte) (sign * oper.sign);
                    def.nans = INFINITE;
                }
                if (nans == FINITE && mant[DIGITS - 1] == 0) {
                    def = newInstance(zero);
                    def.nans = QNAN;
                }
                if (nans == INFINITE || nans == QNAN) {
                    def = newInstance(zero);
                    def.nans = QNAN;
                }
                if (nans == INFINITE || nans == SNAN) {
                    def = newInstance(zero);
                    def.nans = QNAN;
                }
                break;
            case FLAG_UNDERFLOW:
                if ((result.exp + DIGITS) < minExp) {
                    def = newInstance(zero);
                    def.sign = result.sign;
                } else {
                    def = newInstance(result);
                }
                result.exp = result.exp + errScale;
                break;
            case FLAG_OVERFLOW:
                result.exp = result.exp - errScale;
                def = newInstance(zero);
                def.sign = result.sign;
                def.nans = INFINITE;
                break;
            default:
                def = result;
                break;
        }
        return trap(type, what, oper, def, result);
    }

    /**
     * Trap handler. Subclasses may override this to provide trap functionality
     * per IEEE 854-1987.
     *
     * @param type
     *            The exception type - e.g. FLAG_OVERFLOW
     * @param what
     *            The name of the routine we were in e.g. divide()
     * @param oper
     *            An operand to this function if any
     * @param def
     *            The default return value if trap not enabled
     * @param result
     *            The result that is spcefied to be delivered per IEEE 854, if
     *            any
     */
    protected dfp trap(int type, String what, dfp oper, dfp def, dfp result) {
        return def;
    }

    /** Returns the IEEE 854 status flags */
    public static int getIEEEFlags() {
        return ieeeFlags;
    }

    /** Clears the IEEE 854 status flags */
    public static void clearIEEEFlags() {
        ieeeFlags = 0;
    }

    /** Sets the IEEE 854 status flags */
    public static void setIEEEFlags(int flags) {
        ieeeFlags = flags;
    }

    /** Returns the type - one of FINITE, INFINITE, SNAN, QNAN */
    public int classify() {
        return nans;
    }

    /** Creates a dfp with a non-finite value */
    public static dfp create(byte sign, byte nans) {
        dfp result = new dfp();
        result.sign = sign;
        result.nans = nans;
        return result;
    }

    /**
     * Creates a dfp that is the same as x except that it has the sign of y.
     * abs(x) = dfp.copysign(x, dfp.one)
     */
    public static dfp copysign(dfp x, dfp y) {
        dfp result = x.newInstance(x);
        result.sign = y.sign;
        return result;
    }

    /**
     * Returns the next number greater than this one in the direction of x. If
     * this==x then simply returns this.
     */
    public dfp nextAfter(dfp x) {
        boolean up = false;
        dfp result, inc;
        if (this.lessThan(x)) up = true;
        if (compare(this, x) == 0) return newInstance(x);
        if (lessThan(zero)) up = !up;
        if (up) {
            inc = newInstance(one);
            inc.exp = this.exp - DIGITS + 1;
            inc.sign = this.sign;
            if (this.equal(zero)) inc.exp = minExp - DIGITS;
            result = add(inc);
        } else {
            inc = newInstance(one);
            inc.exp = this.exp;
            inc.sign = this.sign;
            if (this.equal(inc)) inc.exp = this.exp - DIGITS; else inc.exp = this.exp - DIGITS + 1;
            if (this.equal(zero)) inc.exp = minExp - DIGITS;
            result = this.subtract(inc);
        }
        if (result.classify() == INFINITE && this.classify() != INFINITE) {
            ieeeFlags |= FLAG_INEXACT;
            result = dotrap(FLAG_INEXACT, "nextAfter", x, result);
        }
        if (result.equal(zero) && this.equal(zero) == false) {
            ieeeFlags |= FLAG_INEXACT;
            result = dotrap(FLAG_INEXACT, "nextAfter", x, result);
        }
        return result;
    }
}
