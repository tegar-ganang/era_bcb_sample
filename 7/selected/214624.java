package ch.schorn.numbertheory;

import java.applet.Applet;
import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.StringTokenizer;
import java.util.Vector;

public class FourSquares extends Applet {

    private static final String VERSION = "(V2.08 29-Nov-2005)";

    private static final BigInteger ZERO = BigInteger.valueOf(0L);

    private static final BigInteger ONE = BigInteger.valueOf(1L);

    private static final BigInteger TWO = BigInteger.valueOf(2L);

    private static final BigInteger MAXINT = BigInteger.valueOf(Integer.MAX_VALUE);

    private static final BigInteger ITERBETTER = ONE.shiftLeft(1024);

    private static final int primeCertainty = 10;

    private static final BigInteger[] specialCasesArray = new BigInteger[] { BigInteger.valueOf(9634L), BigInteger.valueOf(2986L), BigInteger.valueOf(1906L), BigInteger.valueOf(1414L), BigInteger.valueOf(730L), BigInteger.valueOf(706L), BigInteger.valueOf(526L), BigInteger.valueOf(370L), BigInteger.valueOf(226L), BigInteger.valueOf(214L), BigInteger.valueOf(130L), BigInteger.valueOf(85L), BigInteger.valueOf(58L), BigInteger.valueOf(34L), BigInteger.valueOf(10L), BigInteger.valueOf(3L), TWO };

    private static final int[][] specialCasesDecomposition = new int[][] { { 56, 57, 57 }, { 21, 32, 39 }, { 13, 21, 36 }, { 6, 17, 33 }, { 0, 1, 27 }, { 15, 15, 16 }, { 6, 7, 21 }, { 8, 9, 15 }, { 8, 9, 9 }, { 3, 6, 13 }, { 0, 3, 11 }, { 0, 6, 7 }, { 0, 3, 7 }, { 3, 3, 4 }, { 0, 1, 3 }, { 1, 1, 1 }, { 0, 1, 1 } };

    private static final java.util.Hashtable specialCases = new java.util.Hashtable(50);

    static {
        for (int i = 0; i < specialCasesArray.length; i++) specialCases.put(specialCasesArray[i], specialCasesDecomposition[i]);
    }

    private static final long magicN = 10080;

    private static final BigInteger bigMagicN = BigInteger.valueOf(magicN);

    private static final java.util.Hashtable squaresModMagicN = new java.util.Hashtable(500);

    static {
        for (long i = 0; i <= (magicN >> 1); i++) squaresModMagicN.put(BigInteger.valueOf((i * i) % magicN), ONE);
    }

    private static final boolean isProbableSquare(BigInteger n) {
        return squaresModMagicN.get(n.remainder(bigMagicN)) != null;
    }

    private static final void testisProbableSquare(BigInteger start, long number) {
        while (number-- > 0) {
            if (!isProbableSquare(start) && isSquare(start)) {
                System.out.println("isProbableSquare(" + start + ") failed.\n");
                return;
            }
            start.add(ONE);
        }
        System.out.println("All tests of isProbableSquare successful!");
        System.out.println(isProbableSquare(BigInteger.valueOf(385)) ? "yes\n" : "no\n");
    }

    private static final int jacobi(long b, BigInteger p) {
        int s = 1;
        long a = p.mod(BigInteger.valueOf(b)).longValue();
        while (a > 1) {
            if ((a & 3) == 0) a >>= 2; else if ((a & 1) == 0) {
                if (((b & 7) == 3) || ((b & 7) == 5)) s = -s;
                a >>= 1;
            } else {
                if (((a & 2) == 2) && ((b & 3) == 3)) s = -s;
                long t = b % a;
                b = a;
                a = t;
            }
        }
        return a == 0 ? -1 : s;
    }

    private static final long[] primes = { 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499 };

    private static final BigInteger lastPrecomputedPrime = BigInteger.valueOf(primes[primes.length - 1]);

    private static BigInteger cachePrime = lastPrecomputedPrime;

    private static int cacheN = primes.length;

    private static final BigInteger nthPrime(int n) {
        BigInteger result;
        if (n < 1) return TWO;
        if (n <= primes.length) return BigInteger.valueOf(primes[n - 1]);
        if (n < cacheN) {
            result = lastPrecomputedPrime;
            for (int i = primes.length; i < n; i++) result = nextProbablePrime(result.add(TWO), primeCertainty);
        } else {
            result = cachePrime;
            for (int i = cacheN; i < n; i++) result = nextProbablePrime(result.add(TWO), primeCertainty);
            cacheN = n;
            cachePrime = result;
        }
        return result;
    }

    private static final BigInteger primeProduct(int n) {
        BigInteger result = ONE;
        for (int i = 1; i <= n; i++) result = result.multiply(nthPrime(i));
        return result;
    }

    private static final BigInteger primeMod(BigInteger minimum, BigInteger modulus, BigInteger remainder) {
        if (modulus.compareTo(ONE) < 0) return ZERO;
        if (!modulus.gcd(remainder).equals(ONE)) return ONE;
        if (minimum.compareTo(TWO) < 0) minimum = TWO;
        BigInteger n = minimum.subtract(remainder).divide(modulus).multiply(modulus).add(remainder);
        if (n.compareTo(minimum) < 0) n = n.add(modulus);
        while (!isProbablePrime(n, primeCertainty)) n = n.add(modulus);
        return n;
    }

    private static final BigInteger factorial(int n) {
        BigInteger result = ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    private static final boolean isSquare(BigInteger n) {
        return isqrt(n)[1].signum() == 0;
    }

    private static final BigInteger[] iunit(BigInteger p) {
        BigInteger r = null;
        if (p.testBit(0) && !p.testBit(1) && p.testBit(2)) r = TWO; else {
            int k = 2;
            long q = 3;
            while (jacobi(q, p) == 1) {
                if (k < primes.length) {
                    q = primes[k++];
                    if ((q == 229) && isProbableSquare(p)) {
                        BigInteger[] sr = isqrt(p);
                        if (sr[1].signum() == 0) return new BigInteger[] { sr[0], ZERO };
                    }
                } else {
                    if (r == null) r = BigInteger.valueOf(q);
                    r = nextProbablePrime(r.add(TWO), 2);
                    q = r.longValue();
                }
            }
            if (r == null) r = BigInteger.valueOf(q);
        }
        return new BigInteger[] { r.modPow(p.shiftRight(2), p), ONE };
    }

    private static final BigInteger[] isqrtInternal(BigInteger n, int log2n) {
        if (n.compareTo(MAXINT) < 1) {
            int ln = n.intValue(), s = (int) java.lang.Math.sqrt(ln);
            return new BigInteger[] { BigInteger.valueOf(s), BigInteger.valueOf(ln - s * s) };
        }
        if (n.compareTo(ITERBETTER) < 1) {
            int d = 7 * (log2n / 14 - 1), q = 7;
            BigInteger s = BigInteger.valueOf((long) java.lang.Math.sqrt(n.shiftRight(d << 1).intValue()));
            while (d > 0) {
                if (q > d) q = d;
                s = s.shiftLeft(q);
                d -= q;
                q <<= 1;
                s = s.add(n.shiftRight(d << 1).divide(s)).shiftRight(1);
            }
            return new BigInteger[] { s, n.subtract(s.multiply(s)) };
        }
        int log2b = log2n >> 2;
        BigInteger mask = ONE.shiftLeft(log2b).subtract(ONE);
        BigInteger[] sr = isqrtInternal(n.shiftRight(log2b << 1), log2n - (log2b << 1));
        BigInteger s = sr[0];
        BigInteger[] qu = sr[1].shiftLeft(log2b).add(n.shiftRight(log2b).and(mask)).divideAndRemainder(s.shiftLeft(1));
        BigInteger q = qu[0];
        return new BigInteger[] { s.shiftLeft(log2b).add(q), qu[1].shiftLeft(log2b).add(n.and(mask)).subtract(q.multiply(q)) };
    }

    private static final BigInteger[] isqrt(BigInteger n) {
        if (n.compareTo(MAXINT) < 1) {
            long ln = n.longValue();
            long s = (long) java.lang.Math.sqrt(ln);
            return new BigInteger[] { BigInteger.valueOf(s), BigInteger.valueOf(ln - s * s) };
        }
        BigInteger[] sr = isqrtInternal(n, n.bitLength() - 1);
        if (sr[1].signum() < 0) {
            return new BigInteger[] { sr[0].subtract(ONE), sr[1].add(sr[0].shiftLeft(1)).subtract(ONE) };
        }
        return sr;
    }

    private static final void testisqrt(BigInteger n, long number) {
        BigInteger[] sr;
        BigInteger r, s, s1, s12, s2;
        System.out.println("Start test at " + n.toString() + " for " + new Long(number).toString() + " iterations.\n");
        for (long i = 0; i < number; i++) {
            sr = isqrt(n);
            s = sr[0];
            s2 = s.multiply(s);
            r = sr[1];
            s1 = s.add(ONE);
            s12 = s2.add(s.shiftLeft(1)).add(ONE);
            if ((s2.compareTo(n) > 0) || (s12.compareTo(n) < 1) || (s2.add(r).compareTo(n) != 0)) {
                System.out.println("Fail(" + n + ", " + s + ", " + n.subtract(s.multiply(s)) + ")");
            }
            n = n.add(ONE);
        }
        System.out.println("Test finished.\n");
    }

    private static final void testiunit(BigInteger start, long number) {
        BigInteger r, four = BigInteger.valueOf(4);
        start = start.compareTo(four) < 1 ? BigInteger.valueOf(5) : start.clearBit(1).setBit(0);
        System.out.println("Start with " + start);
        for (long z = 0; z < number; z++) {
            if (!isProbablePrime(start, primeCertainty)) {
                r = iunit(start)[0];
                if (r.multiply(r).add(ONE).mod(start).signum() == 0) System.out.println(start.toString() + " " + r.toString());
            }
            start = start.add(four);
        }
        System.out.println("Stop at " + start);
    }

    private static final BigInteger primeProduct97 = new BigInteger("1152783981972759212376551073665878035");

    private static final BigInteger b341 = BigInteger.valueOf(341L);

    private static final boolean isProbablePrime(BigInteger n, int certainty) {
        return ((n.compareTo(b341) < 0) || primeProduct97.gcd(n).equals(ONE)) && TWO.modPow(n.subtract(ONE), n).equals(ONE) && n.isProbablePrime(certainty);
    }

    private static final BigInteger nextProbablePrime(BigInteger n, int certainty) {
        while (!isProbablePrime(n, certainty)) n = n.add(TWO);
        return n;
    }

    private static final BigInteger[] decomposePrime(BigInteger p) {
        BigInteger a = p, b, t, x0 = ZERO, x1 = ONE;
        BigInteger[] sr = iunit(p);
        b = sr[0];
        if (ZERO.equals(sr[1])) return new BigInteger[] { ZERO, b, ONE };
        if (b.multiply(b).add(ONE).mod(p).signum() != 0) return new BigInteger[] { ZERO, ZERO, ZERO };
        while (b.multiply(b).compareTo(p) > 0) {
            t = a.remainder(b);
            a = b;
            b = t;
        }
        return new BigInteger[] { a.remainder(b), b, ONE };
    }

    private static final void testdecomposePrime(BigInteger n, long k) {
        BigInteger FOUR = BigInteger.valueOf(4);
        BigInteger[] result;
        n = n.shiftRight(2).shiftLeft(2).add(ONE);
        System.out.println("Start with " + n + " for " + new Long(k) + " iterations.");
        while (k-- > 0) {
            while (!isProbablePrime(n, 2)) {
                n = n.add(FOUR);
            }
            result = decomposePrime(n);
            if (ONE.equals(result[2]) && (result[0].multiply(result[0]).add(result[1].multiply(result[1])).compareTo(n) != 0)) {
                System.out.println("Failure for " + n + ".");
                return;
            }
            n = n.add(FOUR);
        }
        System.out.println("All tests successful. Last prime checked " + n.subtract(FOUR));
    }

    private static final BigInteger[] decompose(BigInteger n) {
        if (n.compareTo(ONE) < 1) return new BigInteger[] { ZERO, ZERO, ZERO, n, ONE };
        BigInteger sq, x, p, delta, v;
        BigInteger[] z, sqp;
        int k = n.getLowestSetBit() >> 1;
        if (k > 0) {
            v = ONE.shiftLeft(k);
            n = n.shiftRight(k << 1);
        } else v = ONE;
        sqp = isqrt(n);
        sq = sqp[0];
        if (sqp[1].signum() == 0) return new BigInteger[] { ZERO, ZERO, ZERO, v.multiply(sq), ONE };
        if (n.testBit(0) && !n.testBit(1) && isProbablePrime(n, primeCertainty)) {
            z = decomposePrime(n);
            if (ONE.equals(z[2])) return new BigInteger[] { ZERO, ZERO, v.multiply(z[0]), v.multiply(z[1]), ONE };
            delta = ZERO;
        } else if (n.testBit(0) && n.testBit(1) && n.testBit(2)) {
            if (sq.testBit(0) || sq.testBit(1)) {
                delta = v.multiply(sq);
                n = sqp[1];
            } else {
                delta = v.multiply(sq.subtract(ONE));
                n = sqp[1].add(sq.shiftLeft(1).subtract(ONE));
            }
            sqp = isqrt(n);
            sq = sqp[0];
        } else delta = ZERO;
        int[] special = (int[]) specialCases.get(n);
        if (special != null) return new BigInteger[] { delta, v.multiply(BigInteger.valueOf(special[0])), v.multiply(BigInteger.valueOf(special[1])), v.multiply(BigInteger.valueOf(special[2])), ONE };
        if (n.testBit(0) && n.testBit(1)) {
            if (sq.testBit(0)) {
                x = sq;
                p = sqp[1].shiftRight(1);
            } else {
                x = sq.subtract(ONE);
                p = sqp[1].add(sq.shiftLeft(1).subtract(ONE)).shiftRight(1);
            }
            while (true) {
                if (isProbablePrime(p, 2)) {
                    z = decomposePrime(p);
                    if (ONE.equals(z[2])) {
                        return new BigInteger[] { delta, v.multiply(x), v.multiply(z[0].add(z[1])), v.multiply(z[0].subtract(z[1])).abs(), ONE };
                    }
                }
                x = x.subtract(TWO);
                if (x.signum() < 0) return new BigInteger[] { ZERO, ZERO, ZERO, ZERO, ZERO };
                p = p.add(x.add(ONE).shiftLeft(1));
            }
        }
        if (n.subtract(sq).testBit(0)) {
            x = sq;
            p = sqp[1];
        } else {
            x = sq.subtract(ONE);
            p = sqp[1].add(sq.shiftLeft(1).subtract(ONE));
        }
        while (true) {
            if (isProbablePrime(p, 2)) {
                z = decomposePrime(p);
                if (ONE.equals(z[2])) {
                    return new BigInteger[] { delta, v.multiply(x), v.multiply(z[0]), v.multiply(z[1]), ONE };
                }
            }
            x = x.subtract(TWO);
            if (x.signum() < 0) return new BigInteger[] { ZERO, ZERO, ZERO, ZERO, ZERO };
            p = p.add(x.add(ONE).shiftLeft(2));
        }
    }

    private static final BigInteger[] sort(BigInteger[] result) {
        while (true) {
            int i = 0;
            while ((i < 3) && (result[i].compareTo(result[i + 1]) < 1)) i++;
            if (i == 3) return result;
            BigInteger t = result[i];
            result[i] = result[i + 1];
            result[i + 1] = t;
        }
    }

    private static final class IntegerExpression {

        private StringTokenizer getNextTokenizer;

        private String token;

        private String errorMessage;

        private String processedInput;

        public IntegerExpression(String inputString) {
            errorMessage = null;
            processedInput = "";
            getNextTokenizer = new StringTokenizer(inputString, " \t+-*/%^(,)!", true);
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getProcessedInput() {
            return processedInput;
        }

        private final void setErrorMessage(String text) {
            if (errorMessage == null) errorMessage = text;
        }

        private final void getNext() {
            while (true) if (getNextTokenizer.hasMoreTokens()) {
                token = getNextTokenizer.nextToken();
                if (!(" ".equals(token) || "\t".equals(token))) {
                    if (errorMessage == null) processedInput += token;
                    break;
                }
            } else {
                token = null;
                break;
            }
        }

        private final BigInteger expression() {
            BigInteger r;
            if (token == null) {
                setErrorMessage("Empty start of expression.");
                return ZERO;
            }
            if ("+".equals(token)) {
                getNext();
                r = mulTerm();
            } else if ("-".equals(token)) {
                getNext();
                r = mulTerm().negate();
            } else r = mulTerm();
            while (true) if ("+".equals(token)) {
                getNext();
                r = r.add(mulTerm());
            } else if ("-".equals(token)) {
                getNext();
                r = r.subtract(mulTerm());
            } else return r;
        }

        private final BigInteger mulTerm() {
            BigInteger r = expTerm();
            while (true) if ("*".equals(token)) {
                getNext();
                r = r.multiply(expTerm());
            } else if ("/".equals(token)) {
                getNext();
                BigInteger d = expTerm();
                if (d.signum() == 0) {
                    setErrorMessage("Division by zero.");
                    return ZERO;
                }
                r = r.divide(d);
            } else if ("%".equals(token)) {
                getNext();
                BigInteger d = expTerm();
                if (d.signum() <= 0) {
                    setErrorMessage("Modulus must be positive.");
                    return ZERO;
                }
                r = r.mod(d);
            } else return r;
        }

        private final BigInteger expTerm() {
            BigInteger r = factor();
            if ("^".equals(token)) {
                getNext();
                BigInteger exponent = factor();
                if ((exponent.signum() >= 0) && (exponent.compareTo(MAXINT) <= 0)) return r.pow(exponent.intValue());
                setErrorMessage("Exponenent must be non-negative and less than 2^31.");
                return ZERO;
            }
            return r;
        }

        private interface BigIntegerFunction {

            public abstract BigInteger function(BigInteger argument);
        }

        private final BigInteger[] getParameters() {
            Vector result = new Vector(10);
            getNext();
            if ("(".equals(token)) {
                do {
                    getNext();
                    result.addElement(expression());
                } while (",".equals(token));
                if (")".equals(token)) getNext(); else setErrorMessage("\")\" expected.");
            } else setErrorMessage("\"(\" expected.");
            if (errorMessage == null) {
                BigInteger[] r = new BigInteger[result.size()];
                for (int i = 0; i < r.length; i++) r[i] = (BigInteger) (result.elementAt(i));
                return r;
            }
            return new BigInteger[] {};
        }

        private final BigInteger evaluateFunction(BigIntegerFunction f) {
            BigInteger[] parameters = getParameters();
            if (parameters.length == 1) return f.function(parameters[0]);
            setErrorMessage("Only one parameter expected.");
            return ZERO;
        }

        private final BigInteger factor() {
            BigInteger r = ZERO;
            if (token == null) {
                setErrorMessage("Empty start of factor.");
            } else if ("(".equals(token)) {
                getNext();
                r = expression();
                if (")".equals(token)) getNext(); else setErrorMessage("\")\" expected.");
            } else if ("sqrt".equalsIgnoreCase(token)) r = evaluateFunction(new BigIntegerFunction() {

                public BigInteger function(BigInteger argument) {
                    if (argument.signum() < 0) {
                        setErrorMessage("Cannot take square root of negative number \"" + argument.toString() + "\".");
                        return ZERO;
                    }
                    return isqrt(argument)[0];
                }
            }); else if ("prime".equalsIgnoreCase(token)) r = evaluateFunction(new BigIntegerFunction() {

                public BigInteger function(BigInteger argument) {
                    return (argument.compareTo(TWO) < 1) ? TWO : nextProbablePrime(argument.testBit(0) ? argument : argument.add(ONE), primeCertainty);
                }
            }); else if ("ip".equalsIgnoreCase(token)) r = evaluateFunction(new BigIntegerFunction() {

                public BigInteger function(BigInteger argument) {
                    if ((argument.signum() > 0) && (argument.compareTo(MAXINT) <= 0)) return nthPrime(argument.intValue()); else {
                        setErrorMessage("Argument of ip must be positive and less than 2^31.");
                        return ZERO;
                    }
                }
            }); else if ("pp".equalsIgnoreCase(token)) r = evaluateFunction(new BigIntegerFunction() {

                public BigInteger function(BigInteger argument) {
                    if ((argument.signum() > 0) && (argument.compareTo(MAXINT) <= 0)) return primeProduct(argument.intValue());
                    setErrorMessage("Argument of pp must be positive and less than 2^31.");
                    return ZERO;
                }
            }); else if ("d".equalsIgnoreCase(token)) {
                BigInteger[] parameters = getParameters();
                if (parameters.length == 3) {
                    r = primeMod(parameters[0], parameters[1], parameters[2]);
                    if (r.signum() == 0) setErrorMessage("Modulus must be positive."); else if (r.equals(ONE)) setErrorMessage("Modulus must be prime to remainder.");
                } else setErrorMessage("Three parameters expected.");
            } else try {
                r = new BigInteger(token);
                getNext();
            } catch (NumberFormatException e) {
                setErrorMessage("\"" + token + "\" is not a legal decimal number.");
            }
            while ("!".equals(token)) {
                if ((r.signum() > 0) && (r.compareTo(MAXINT) <= 0)) {
                    getNext();
                    r = factorial(r.intValue());
                } else {
                    setErrorMessage("Argument to factorial must be positive and less than 2^31.");
                    return ZERO;
                }
            }
            return r;
        }

        public final BigInteger evaluate() {
            BigInteger r = ZERO;
            getNext();
            try {
                r = expression();
            } catch (Exception e) {
                setErrorMessage(e.toString());
            }
            if (token != null) setErrorMessage("Extra \"" + token + "\" detected.");
            return r;
        }
    }

    private final class ComputeThread extends Thread {

        private final String expression;

        private final TextArea resultArea;

        private final Label status;

        public ComputeThread(String expression, TextArea resultArea, Label status) {
            this.expression = expression;
            this.resultArea = resultArea;
            this.status = status;
        }

        public void run() {
            long startTime = System.currentTimeMillis();
            status.setText("Decomposing " + (expression.length() > 25 ? expression.substring(0, 25) + "..." : expression));
            final IntegerExpression exp = new IntegerExpression(expression);
            final BigInteger n = exp.evaluate();
            if (exp.getErrorMessage() != null) {
                resultArea.setText("Error in expression detected.\n" + exp.getProcessedInput() + "???\n" + exp.getErrorMessage() + "\n");
            } else if (n.signum() < 0) resultArea.setText("Cannot represent negative number " + n); else {
                final BigInteger[] result = sort(decompose(n));
                resultArea.setText(n.toString() + " =\n" + (result[0].signum() == 0 ? "" : "\t" + result[0].toString() + "^2 +\n") + (result[1].signum() == 0 ? "" : "\t" + result[1].toString() + "^2 +\n") + (result[2].signum() == 0 ? "" : "\t" + result[2].toString() + "^2 +\n") + "\t" + result[3].toString() + "^2" + (n.equals(result[0].multiply(result[0]).add(result[1].multiply(result[1])).add(result[2].multiply(result[2])).add(result[3].multiply(result[3]))) ? "" : " Fail(" + n + ")"));
            }
            status.setText("Time used = " + Long.toString(System.currentTimeMillis() - startTime) + " ms.");
        }
    }

    private final String spaces(int number) {
        StringBuffer temp = new StringBuffer(number);
        for (int i = 0; i < number; i++) temp.append(' ');
        return new String(temp);
    }

    private static final int textWidth = 85;

    private final TextField numberText = new TextField(textWidth);

    private final Label statusText = new Label(spaces(textWidth));

    private final TextArea resultText = new TextArea(5, textWidth);

    private final Button decomposeButton = new Button("Decompose");

    private final Button clearButton = new Button("Clear");

    private ComputeThread compute = null;

    private final void stopPreviousThread() {
        statusText.setText("");
        if (compute != null) {
            try {
                compute.stop();
            } catch (SecurityException e) {
                statusText.setText("Waiting for decomposition to finish...");
                try {
                    compute.join();
                } catch (InterruptedException ie) {
                }
            }
            compute = null;
        }
    }

    private final class ComputeAction implements ActionListener {

        public final void actionPerformed(java.awt.event.ActionEvent e) {
            stopPreviousThread();
            compute = new ComputeThread(numberText.getText(), resultText, statusText);
            compute.start();
        }
    }

    private static final Color backgroundColor = new Color(0xF7F7F7);

    public void init() {
        setFont(new Font("serif", Font.PLAIN, 12));
        setLayout(new FlowLayout(FlowLayout.LEFT));
        setBackground(backgroundColor);
        resultText.setBackground(Color.white);
        numberText.setBackground(Color.white);
        add(new Label("Enter an integer expression to be decomposed into a sum of at most four squares " + VERSION));
        add(numberText);
        add(decomposeButton);
        add(clearButton);
        add(resultText);
        add(statusText);
        clearButton.addActionListener(new ActionListener() {

            public final void actionPerformed(java.awt.event.ActionEvent e) {
                stopPreviousThread();
                resultText.setText("");
                numberText.setText("");
                numberText.setCaretPosition(0);
                numberText.requestFocus();
            }
        });
        ComputeAction ca = new ComputeAction();
        decomposeButton.addActionListener(ca);
        resultText.setEditable(false);
        numberText.addActionListener(ca);
        numberText.setEditable(true);
        numberText.setText("19081961");
        numberText.selectAll();
        numberText.setCaretPosition(0);
        numberText.requestFocus();
    }

    private static final class AppletFrame extends Frame {

        public AppletFrame(String name) {
            super(name);
            addWindowListener(new java.awt.event.WindowAdapter() {

                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.exit(0);
                }
            });
        }
    }

    private static final void testDecompose(BigInteger start, long number, boolean verbose) {
        final long startTime = System.currentTimeMillis(), perDot = 250, perLine = 10000;
        long errors = 0;
        final String range = " range [" + start + ", " + start.add(BigInteger.valueOf(number - 1)) + "] of size " + Long.toString(number);
        System.out.println("Start" + range + ".");
        BigInteger toBeTested = start;
        for (long i = 1; i <= number; i++) {
            if (i % perDot == 0) System.out.print(".");
            if (i % perLine == 0) System.out.println(" (i=" + Long.toString(i) + ", n=" + toBeTested + ")");
            BigInteger[] result = decompose(toBeTested);
            if (verbose) {
                sort(result);
                System.out.println(toBeTested.toString() + " " + result[0] + " " + result[1] + " " + result[2] + " " + result[3]);
            }
            if (result[4].longValue() == 0) {
                System.out.println("FAIL(" + toBeTested + ")");
                errors++;
            } else if (!result[0].multiply(result[0]).add(result[1].multiply(result[1])).add(result[2].multiply(result[2])).add(result[3].multiply(result[3])).equals(toBeTested)) {
                System.out.println("Error(" + toBeTested + ")");
                errors++;
            }
            toBeTested = toBeTested.add(ONE);
        }
        System.out.println((number % perLine == 0 ? "" : "\n") + "Finished" + range + ". Time used " + Long.toString(System.currentTimeMillis() - startTime) + " ms. " + (errors == 0 ? "No error detected." : (errors == 1 ? "One error detected." : Long.toString(errors) + " errors detected.")));
    }

    private static final void startApplet(FourSquares applet, int width, int height) {
        final AppletFrame f = new AppletFrame("Lagrange");
        f.add("Center", applet);
        f.pack();
        f.setSize(width, height);
        applet.setSize(width, height);
        applet.init();
        applet.start();
        f.show();
        f.repaint();
    }

    public static final void main(String[] args) {
        if ((0 < args.length) && (args.length < 4)) {
            try {
                IntegerExpression exp = new IntegerExpression(args[0]);
                BigInteger n = exp.evaluate();
                if (exp.getErrorMessage() == null) testDecompose(n, (args.length > 1 ? Long.parseLong(args[1]) : 1), (args.length == 1) || (args.length > 2) && (args[2].equalsIgnoreCase("verbose"))); else System.out.println(exp.getErrorMessage() + "\n" + exp.getProcessedInput() + "???");
            } catch (Exception e) {
                System.out.println("Exception(" + e + ")\nUsage: [startExpression [#tests] [\"verbose\"]]");
            }
        } else startApplet(new FourSquares(), 750, 270);
    }
}
