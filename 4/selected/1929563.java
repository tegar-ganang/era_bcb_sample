package jeme;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import jeme.io.SchemeOutputPort;
import jeme.lang.SchemeBoolean;
import jeme.lang.SchemeObject;
import jeme.lang.SchemePair;
import jeme.lang.SchemeString;
import jeme.lang.SchemeVector;
import jeme.math.SchemeComplex;
import jeme.math.SchemeInteger;
import jeme.math.SchemeNumber;
import jeme.math.SchemeReal;

/**
 * Represents a procedure in Scheme's standard library. See the
 * <a href="http://www.r6rs.org/final/html/r6rs-lib/r6rs-lib.html">R<sup>6</sup>RS
 * documentation</a> for more information.
 * 
 * @author Erik Silkensen (silkense@colorado.edu)
 * @version Aug 17, 2009
 */
public class StdlibProcedure extends Procedure {

    /**
     * Writes a representation of an object (the first argument) to a given 
     * output port (the second argument). If there is no specified output port,
     * then <code>(current-output-port)</code> is used.
     */
    public static final StdlibProcedure DISPLAY = new StdlibProcedure(1, 2, 0);

    /**
     * Returns the current output port.
     */
    public static final StdlibProcedure CURRENT_OUTPUT_PORT = new StdlibProcedure(0, 1);

    /**
     * Returns the sum of the arguments.
     */
    public static final StdlibProcedure ADD = new StdlibProcedure(0, Integer.MAX_VALUE, 2);

    /**
     * Returns the difference of the arguments.
     */
    public static final StdlibProcedure SUBTRACT = new StdlibProcedure(1, Integer.MAX_VALUE, 3);

    /**
     * Returns the product of the arguments.
     */
    public static final StdlibProcedure MULTIPLY = new StdlibProcedure(0, Integer.MAX_VALUE, 4);

    /**
     * Returns the quotient of the arguments.
     */
    public static final StdlibProcedure DIVIDE = new StdlibProcedure(1, Integer.MAX_VALUE, 5);

    /**
     * Returns the equality of the numeric arguments.
     */
    public static final StdlibProcedure NUM_EQ = new StdlibProcedure(0, Integer.MAX_VALUE, 6);

    /**
     * Constructs a pair using the two arguments.
     */
    public static final StdlibProcedure CONS = new StdlibProcedure(2, 2, 7);

    /**
     * Returns the car field of a pair.
     */
    public static final StdlibProcedure CAR = new StdlibProcedure(1, 1, 8);

    /**
     * Returns the cdr field of a pair.
     */
    public static final StdlibProcedure CDR = new StdlibProcedure(1, 1, 9);

    /**
     * Returns a list of the arguments.
     */
    public static final StdlibProcedure LIST = new StdlibProcedure(0, Integer.MAX_VALUE, 10);

    /**
     * Returns whether or not the argument is a pair.
     */
    public static final StdlibProcedure PAIRQ = new StdlibProcedure(1, 1, 11);

    /**
     * Returns whether or not the argument is a list.
     */
    public static final StdlibProcedure LISTQ = new StdlibProcedure(1, 1, 12);

    /**
     * Returns whether or not the arguments are equal.
     */
    public static final StdlibProcedure EQ = new StdlibProcedure(2, 2, 13);

    /**
     * Returns whether or not the argument is the empty list.
     */
    public static final StdlibProcedure NULLQ = new StdlibProcedure(1, 1, 14);

    /**
     * Displays the newline character.
     */
    public static final StdlibProcedure NEWLINE = new StdlibProcedure(0, 1, 15);

    /**
     * Returns the length of a list.
     */
    public static final StdlibProcedure LENGTH = new StdlibProcedure(1, 1, 16);

    /**
     * Returns the real part of a number.
     */
    public static final StdlibProcedure REAL_PART = new StdlibProcedure(1, 1, 17);

    /**
     * Returns the imaginary part of a number.
     */
    public static final StdlibProcedure IMAG_PART = new StdlibProcedure(1, 1, 18);

    /**
     * Returns the angle of a number.
     */
    public static final StdlibProcedure ANGLE = new StdlibProcedure(1, 1, 19);

    /**
     * Returns the magnitude of a number.
     */
    public static final StdlibProcedure MAGNITUDE = new StdlibProcedure(1, 1, 20);

    /**
     * Returns the natural logarithm of a number.
     */
    public static final StdlibProcedure LOG = new StdlibProcedure(1, 1, 21);

    /**
     * Returns the square root of a number.
     */
    public static final StdlibProcedure SQRT = new StdlibProcedure(1, 1, 22);

    /**
     * Returns the sine of a number.
     */
    public static final StdlibProcedure SIN = new StdlibProcedure(1, 1, 23);

    /**
     * Returns the cosine of a number.
     */
    public static final StdlibProcedure COS = new StdlibProcedure(1, 1, 24);

    /**
     * Returns the tangent of a number.
     */
    public static final StdlibProcedure TAN = new StdlibProcedure(1, 1, 25);

    /**
     * Returns the arcsine of a number.
     */
    public static final StdlibProcedure ASIN = new StdlibProcedure(1, 1, 26);

    /**
     * Returns the arccosine of a number.
     */
    public static final StdlibProcedure ACOS = new StdlibProcedure(1, 1, 27);

    /**
     * Returns the arctangent of a number.
     */
    public static final StdlibProcedure ATAN = new StdlibProcedure(1, 2, 28);

    /**
     * Returns whether each number is < the next.
     */
    public static final StdlibProcedure LT = new StdlibProcedure(0, Integer.MAX_VALUE, 29);

    /**
     * Returns whether each number is > the next.
     */
    public static final StdlibProcedure GT = new StdlibProcedure(0, Integer.MAX_VALUE, 30);

    /**
     * Returns whether each number is <= the next.
     */
    public static final StdlibProcedure LTE = new StdlibProcedure(0, Integer.MAX_VALUE, 31);

    /**
     * Returns whether each number is >= the next.
     */
    public static final StdlibProcedure GTE = new StdlibProcedure(0, Integer.MAX_VALUE, 32);

    /**
     * Returns whether the two arguments are equal to each other.
     */
    public static final StdlibProcedure EQUAL = new StdlibProcedure(2, 2, 33);

    /**
     * Returns the value of an element in a vector.
     */
    public static final StdlibProcedure VECREF = new StdlibProcedure(2, 2, 34);

    /**
     * TODO
     */
    public static final StdlibProcedure SYSTEM = new StdlibProcedure(1, 1, 35);

    /**
     * TODO
     */
    public static final StdlibProcedure VECLEN = new StdlibProcedure(1, 1, 36);

    /**
     * TODO
     */
    public static final StdlibProcedure STRING_APPEND = new StdlibProcedure(0, Integer.MAX_VALUE, 37);

    public SchemeObject apply(SchemeObject... args) {
        switch(getIndex()) {
            case 0:
                return c0(this, args);
            case 1:
                return c1(this, args);
            case 2:
                return c2(this, args);
            case 3:
                return c3(this, args);
            case 4:
                return c4(this, args);
            case 5:
                return c5(this, args);
            case 6:
                return c6(this, args);
            case 7:
                return c7(this, args);
            case 8:
                return c8(this, args);
            case 9:
                return c9(this, args);
            case 10:
                return c10(this, args);
            case 11:
                return c11(this, args);
            case 12:
                return c12(this, args);
            case 13:
                return c13(this, args);
            case 14:
                return c14(this, args);
            case 15:
                return c15(this, args);
            case 16:
                return c16(this, args);
            case 17:
                return c17(this, args);
            case 18:
                return c18(this, args);
            case 19:
                return c19(this, args);
            case 20:
                return c20(this, args);
            case 21:
                return c21(this, args);
            case 22:
                return c22(this, args);
            case 23:
                return c23(this, args);
            case 24:
                return c24(this, args);
            case 25:
                return c25(this, args);
            case 26:
                return c26(this, args);
            case 27:
                return c27(this, args);
            case 28:
                return c28(this, args);
            case 29:
                return c29(this, args);
            case 30:
                return c30(this, args);
            case 31:
                return c31(this, args);
            case 32:
                return c32(this, args);
            case 33:
                return c33(this, args);
            case 34:
                return c34(this, args);
            case 35:
                return c35(this, args);
            case 36:
                return c36(this, args);
            case 37:
                return c37(this, args);
            default:
                return cd(this, args);
        }
    }

    /**
     * Implements the display procedure. There may be one or two arguments. The
     * first argument is the object to be displayed, and the second argument is
     * optionally an output port to write the representation to. If no port is
     * specified, <code>(current-output-port)</code> is used.
     * 
     * @param p  the (display) procedure invoking this method
     * @param args  the arguments to the display procedure
     * @return  <code>SchemeObject.UNSPECIFIED</code>
     */
    private static SchemeObject c0(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (args.length > 1 && !(args[1] instanceof SchemeOutputPort)) {
            String msg = String.format(TYPE_ERROR_FMT, args[1], 2, p);
            throw new IllegalArgumentException(msg);
        }
        SchemeOutputPort port = (SchemeOutputPort) (args.length > 1 ? args[1] : c1(CURRENT_OUTPUT_PORT));
        port.print(args[0].toDisplayString());
        return SchemeObject.UNSPECIFIED;
    }

    /**
     * Implements the current-output-port procedure.
     * 
     * @param p  the (current-output-port) procedure invoking this method
     * @param args  the arguments to the current-output-port procedure
     * @return  the current output port
     */
    private static SchemeOutputPort c1(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        return SchemeOutputPort.out;
    }

    /**
     * Implements the + procedure.
     * 
     * @param p  the (+) procedure invoking this method
     * @param args  the arguments to the + procedure
     * @return  the sum of the arguments
     */
    private static SchemeNumber c2(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        SchemeNumber sum = SchemeInteger.ZERO;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SchemeNumber) {
                sum = sum.add((SchemeNumber) args[i]);
            } else {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            }
        }
        return sum;
    }

    /**
     * Implements the - procedure.
     * 
     * @param p  the (-) procedure invoking this method
     * @param args  the arguments to the - procedure
     * @return  the difference of the arguments
     */
    private static SchemeNumber c3(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (args[0] instanceof SchemeNumber) {
            if (args.length == 1) {
                return ((SchemeNumber) args[0]).negate();
            }
        } else {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        SchemeNumber difference = (SchemeNumber) args[0];
        for (int i = 1; i < args.length; i++) {
            if (args[i] instanceof SchemeNumber) {
                difference = difference.subtract((SchemeNumber) args[i]);
            } else {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            }
        }
        return difference;
    }

    /**
     * Implements the * procedure.
     * 
     * @param p  the (*) procedure invoking this method
     * @param args  the arguments to the * procedure
     * @return  the product of the arguments
     */
    private static SchemeNumber c4(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        SchemeNumber product = SchemeInteger.ONE;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SchemeNumber) {
                product = product.multiply((SchemeNumber) args[i]);
            } else {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            }
        }
        return product;
    }

    /**
     * Implements the / procedure.
     * 
     * @param p  the (/) procedure invoking this method
     * @param args  the arguments to the / procedure
     * @return  the quotient of the arguments
     */
    private static SchemeNumber c5(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (args.length == 1) {
            return SchemeInteger.ONE.divide((SchemeNumber) args[0]);
        }
        SchemeNumber quotient = (SchemeNumber) args[0];
        for (int i = 1; i < args.length; i++) {
            if (args[i] instanceof SchemeNumber) {
                quotient = quotient.divide((SchemeNumber) args[i]);
            } else {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            }
        }
        return quotient;
    }

    /**
     * Implements the = procedure.
     * 
     * @param p  the (=) procedure invoking this method
     * @param args  the arguments to the = procedure
     * @return  the equality of the numeric arguments
     */
    private static SchemeBoolean c6(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        boolean result = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof SchemeNumber) {
                if (i > 0) {
                    result = args[i].equals(args[i - 1]);
                    if (!result) {
                        break;
                    }
                }
            } else {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            }
        }
        return result ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the cons procedure.
     * 
     * @param p  the (cons) procedure invoking this method
     * @param args  the arguments to the cons procedure
     * @return  a pair of the two arguments
     */
    private static SchemePair c7(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        return new SchemePair(args[0], args[1]);
    }

    /**
     * Implements the car procedure.
     * 
     * @param p  the (car) procedure invoking this method
     * @param args  the pair to get the car of
     * @return  the car field of the pair argument
     */
    private static SchemeObject c8(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (args[0] instanceof SchemePair) {
            return ((SchemePair) args[0]).car();
        } else {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Implements the cdr procedure.
     * 
     * @param p  the (cdr) procedure invoking this method
     * @param args  the pair to get the cdr of
     * @return  the cdr field of the pair argument
     */
    private static SchemeObject c9(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (args[0] instanceof SchemePair) {
            return ((SchemePair) args[0]).cdr();
        } else {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Implements the list procedure.
     * 
     * @param p  the (list) procedure invoking this method
     * @param args  the elements of the list
     * @return  a list of the arguments
     */
    private static SchemePair c10(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        return (SchemePair) Module.toList(args);
    }

    /**
     * Implements the pair? procedure.
     * 
     * @param p  the (pair?) procedure invoking this method
     * @param args  the object to test
     * @return  whether or not the object is a pair
     */
    private static SchemeBoolean c11(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemePair)) {
            return SchemeBoolean.FALSE;
        }
        SchemePair pair = ((SchemePair) args[0]);
        return pair.isPair() ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the list? procedure.
     * 
     * @param p  the (list?) procedure invoking this method
     * @param args  the object to test
     * @return  whether or not the object is a list
     */
    private static SchemeBoolean c12(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemePair)) {
            return SchemeBoolean.FALSE;
        }
        SchemePair list = ((SchemePair) args[0]);
        return list.isList() ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the eq? procedure.
     * 
     * @param p  the (eq?) procedure invoking this method.
     * @param args  the objects to test
     * @return  whether or not the objects are equal
     */
    private static SchemeBoolean c13(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        return args[0] == args[1] ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the null? procedure.
     * 
     * @param p  the (null?) procedure invoking this method
     * @param args  the object to test
     * @return  whether or not the object is the empty list
     */
    private static SchemeBoolean c14(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemePair)) {
            return SchemeBoolean.FALSE;
        }
        SchemePair pair = ((SchemePair) args[0]);
        return pair.isNull() ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the newline procedure. An optional argument may specifiy the
     * output port to write the newline to.
     * 
     * @param p  the (newline) procedure invoking this method
     * @param args  an optional output port
     * @return  <code>SchemeObject.UNSPECIFIED</code>
     */
    private static SchemeObject c15(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (args.length > 0 && !(args[0] instanceof SchemeOutputPort)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        SchemeOutputPort port = (SchemeOutputPort) (args.length > 0 ? args[0] : c1(CURRENT_OUTPUT_PORT));
        port.print(System.getProperty("line.separator"));
        return SchemeObject.UNSPECIFIED;
    }

    /**
     * Implements the length procedure.
     * 
     * @param p  the (length) procedure invoking this method
     * @param args  the list to return the length of
     * @return  the length of the list
     */
    private static SchemeInteger c16(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (args[0] instanceof SchemePair) {
            SchemePair list = (SchemePair) args[0];
            if (list.isList()) {
                int length = 0;
                SchemePair it = list;
                while (it != SchemePair.EMPTY_LIST) {
                    length++;
                    it = (SchemePair) it.cdr();
                }
                return new SchemeInteger(Integer.toString(length), 10);
            }
        }
        String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
        throw new IllegalArgumentException(msg);
    }

    /**
     * Implements the real-part procedure.
     * 
     * @param p  the (real-part) procedure invoking this method
     * @param args  the number to return the real part of
     * @return  the real part of the number
     */
    private static SchemeReal c17(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeComplex)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return ((SchemeComplex) args[0]).getRealPart();
    }

    /**
     * Implements the imag-part procedure.
     * 
     * @param p  the (imag-part) procedure invoking this method
     * @param args  the number to return the imaginary part of
     * @return  the imaginary part of the number
     */
    private static SchemeReal c18(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeComplex)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return ((SchemeComplex) args[0]).getImagPart();
    }

    /**
     * Implements the angle procedure.
     * 
     * @param p  the (angle) procedure invoking this method
     * @param args  the number to return the angle of
     * @return  the angle of the number
     */
    private static SchemeNumber c19(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.angle((SchemeNumber) args[0]);
    }

    /**
     * Implements the magnitude procedure.
     * 
     * @param p  the (magnitude) procedure invoking this method
     * @param args  the number to return the magnitude of
     * @return  the magnitude of the number
     */
    private static SchemeNumber c20(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.magnitude((SchemeNumber) args[0]);
    }

    /**
     * Implements the log procedure.
     * 
     * @param p  the (log) procedure invoking this method
     * @param args  the number to return the natural logarithm of
     * @return  the natural logarithm of the number
     */
    private static SchemeNumber c21(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.log((SchemeNumber) args[0]);
    }

    /**
     * Implements the sqrt procedure.
     * 
     * @param p  the (sqrt) procedure invoking this method
     * @param args  the number to return the square root of
     * @return  the square root of the number
     */
    private static SchemeNumber c22(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.sqrt((SchemeNumber) args[0]);
    }

    /**
     * Implements the sin procedure.
     * 
     * @param p  the (sin) procedure invoking this method
     * @param args  the number to return the sine of
     * @return  the sine of the number
     */
    private static SchemeNumber c23(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.sin((SchemeNumber) args[0]);
    }

    /**
     * Implements the cos procedure.
     * 
     * @param p  the (cos) procedure invoking this method
     * @param args  the number to return the cosine of
     * @return  the cosine of the number
     */
    private static SchemeNumber c24(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.cos((SchemeNumber) args[0]);
    }

    /**
     * Implements the tan procedure.
     * 
     * @param p  the (tan) procedure invoking this method
     * @param args  the number to return the tangent of
     * @return  the tangent of the number
     */
    private static SchemeNumber c25(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.tan((SchemeNumber) args[0]);
    }

    /**
     * Implements the asin procedure.
     * 
     * @param p  the (asin) procedure invoking this method
     * @param args  the number to return the arcsine of
     * @return  the arcsine of the number
     */
    private static SchemeNumber c26(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.asin((SchemeNumber) args[0]);
    }

    /**
     * Implements the acos procedure.
     * 
     * @param p  the (acos) procedure invoking this method
     * @param args  the number to return the arccosine of
     * @return  the arccosine of the number
     */
    private static SchemeNumber c27(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return SchemeNumber.acos((SchemeNumber) args[0]);
    }

    /**
     * Implements the atan procedure.
     * 
     * @param p  the (arctan) procedure invoking this method
     * @param args  the number to return the arctangent of
     * @return  the arctangent of the number
     */
    private static SchemeNumber c28(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        if (args.length == 2 && !(args[1] instanceof SchemeNumber)) {
            String msg = String.format(TYPE_ERROR_FMT, args[1], 2, p);
            throw new IllegalArgumentException(msg);
        }
        return args.length == 2 ? SchemeNumber.atan2((SchemeNumber) args[0], (SchemeNumber) args[1]) : SchemeNumber.atan((SchemeNumber) args[0]);
    }

    /**
     * Implements the < procedure.
     * 
     * @param p  the (<) procedure invoking this method
     * @param args  the numbers to compare
     * @return  whether each number is < the next
     */
    private static SchemeBoolean c29(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        boolean ans = true;
        SchemeNumber prev = null;
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof SchemeNumber)) {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            } else if (ans) {
                if (prev != null) {
                    ans = prev.compareTo((SchemeNumber) args[i]) < 0;
                }
                prev = (SchemeNumber) args[i];
            }
        }
        return ans ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the > procedure.
     * 
     * @param p  the (>) procedure invoking this method
     * @param args  the numbers to compare
     * @return  whether each number is > the next
     */
    private static SchemeBoolean c30(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        boolean ans = true;
        SchemeNumber prev = null;
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof SchemeNumber)) {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            } else if (ans) {
                if (prev != null) {
                    ans = prev.compareTo((SchemeNumber) args[i]) > 0;
                }
                prev = (SchemeNumber) args[i];
            }
        }
        return ans ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the <= procedure.
     * 
     * @param p  the (<=) procedure invoking this method
     * @param args  the numbers to compare
     * @return  whether each number is <= the next
     */
    private static SchemeBoolean c31(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        boolean ans = true;
        SchemeNumber prev = null;
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof SchemeNumber)) {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            } else if (ans) {
                if (prev != null) {
                    ans = prev.compareTo((SchemeNumber) args[i]) <= 0;
                }
                prev = (SchemeNumber) args[i];
            }
        }
        return ans ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the >= procedure.
     * 
     * @param p  the (>=) procedure invoking this method
     * @param args  the numbers to compare
     * @return  whether each number is >= the next
     */
    private static SchemeBoolean c32(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        boolean ans = true;
        SchemeNumber prev = null;
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof SchemeNumber)) {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            } else if (ans) {
                if (prev != null) {
                    ans = prev.compareTo((SchemeNumber) args[i]) >= 0;
                }
                prev = (SchemeNumber) args[i];
            }
        }
        return ans ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the equal? procedure.
     * 
     * @param p  the (equal?) procedure invoking this method
     * @param args  the objects to compare
     * @return  whether each argument is equal to the other
     */
    private static SchemeBoolean c33(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        return args[0].equals(args[1]) ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
    }

    /**
     * Implements the vector-ref procedure.
     * 
     * @param p  the (vector-ref) procedure invoking this method
     * @param args  the vector and index
     * @return  the value at index in vector
     */
    private static SchemeObject c34(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!((args[0]) instanceof SchemeVector)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        if (!((args[1]) instanceof SchemeInteger)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        SchemeVector v = (SchemeVector) args[0];
        int k = ((SchemeInteger) args[1]).getValue().intValue();
        return v.get(k);
    }

    /**
     * TODO
     * 
     * @param p
     * @param args
     * @return
     */
    private static SchemeBoolean c35(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!((args[0]) instanceof SchemeString)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        try {
            Process q = Runtime.getRuntime().exec(args[0].toDisplayString());
            InputStream in = q.getInputStream();
            q.waitFor();
            Scanner sin = new Scanner(in);
            while (sin.hasNextLine()) {
                System.out.println(sin.nextLine());
            }
            return q.exitValue() == 0 ? SchemeBoolean.TRUE : SchemeBoolean.FALSE;
        } catch (IOException e) {
            e.printStackTrace();
            return SchemeBoolean.FALSE;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return SchemeBoolean.FALSE;
        }
    }

    /**
     * TODO
     * 
     * @param p
     * @param args
     * @return
     */
    private static SchemeInteger c36(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        if (!(args[0] instanceof SchemeVector)) {
            String msg = String.format(TYPE_ERROR_FMT, args[0], 1, p);
            throw new IllegalArgumentException(msg);
        }
        return ((SchemeVector) args[0]).length();
    }

    /**
     * TODO
     * 
     * @param p
     * @param args
     * @return
     */
    private static SchemeString c37(StdlibProcedure p, SchemeObject... args) {
        p.checkArity(args.length);
        for (int i = 0; i < args.length; i++) {
            if (!(args[i] instanceof SchemeString)) {
                String msg = String.format(TYPE_ERROR_FMT, args[i], i + 1, p);
                throw new IllegalArgumentException(msg);
            }
        }
        String str = "";
        for (int i = 0; i < args.length; i++) {
            str += args[i].toDisplayString();
        }
        return new SchemeString(str);
    }

    /**
     * Implements the error procedure.
     * 
     * @param p  the procedure invoking this method
     * @param args  the arguments to the error procedure
     * @return  unspecified
     */
    private static SchemeObject cd(StdlibProcedure p, SchemeObject... args) {
        return null;
    }

    /**
     * Creates a procedure with a specified arity and index value. The default
     * environment is used.
     * 
     * @param arity  the arity, or number of arguments, of this procedure
     * @param index  the index value of this procedure
     */
    public StdlibProcedure(int arity, int index) {
        this(arity, arity, index);
    }

    /**
     * Creates a procedure with a specified arity, index value, and environment.
     * 
     * @param arity  the arity, or number of arguments, of this procedure
     * @param index  the index value of this procedure
     * @param env  the environment to evaluate this procedure in
     */
    public StdlibProcedure(int arity, int index, Environment env) {
        this(arity, arity, index, env);
    }

    /**
     * Creates a procedure with a specified arity range and index value. The 
     * default environment is used. The arity arguments can be used to specify
     * a range of valid argument values (e.g. <code>new StdlibProcedure(1, 
     * 2)</code>).
     * 
     * @param arity1  an arity, or number of arguments, of this procedure
     * @param arity2  an arity, or number of arguments, of this procedure
     * @param index  the index value of this procedure
     */
    public StdlibProcedure(int arity1, int arity2, int index) {
        this(arity1, arity2, index, Environment.DEFAULT);
    }

    /**
     * Creates a procedure with a specified arity range, index value, and 
     * environment. The arity arguments can be used to specify a range of valid
     * argument values (e.g. <code>new StdlibProcedure(1, 2)</code>).
     * 
     * @param arity1  an arity, or number of arguments, of this procedure
     * @param arity2  an arity, or number of arguments, of this procedure
     * @param index  the index value of this procedure
     * @param env  the environment to evaluate this procedure in
     */
    public StdlibProcedure(int arity1, int arity2, int index, Environment env) {
        super(arity1, arity2, index, env);
    }

    public String toString() {
        return "#[compiled-procedure " + getIndex() + "]";
    }
}
