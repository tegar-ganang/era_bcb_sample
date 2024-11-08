package jderead;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class sets constants and read from ASCII file coefficients of Chebyshev
 * polynomials for JPL ephemeris DE405. Class is the successor of DEheader. All
 * necessary files can be found in :
 * "ftp://ssd.jpl.nasa.gov/pub/eph/planets/ascii/de405/"
 * 
 * @author Peter Hristozov E-mail: peterhri@hotmail.com
 * @version 1.3 2011 Jun 15.
 */
public class DE405 extends DEheader {

    /**
     * Array with starting dates for each file with chebychev coefficients.
     */
    private double[] startfiledates = { 2305424.5, 2312752.5, 2320048.5, 2327344.5, 2334640.5, 2341968.5, 2349264.5, 2356560.5, 2363856.5, 2371184.5, 2378480.5, 2385776.5, 2393104.5, 2400400.5, 2407696.5, 2414992.5, 2422320.5, 2429616.5, 2436912.5, 2444208.5, 2451536.5, 2458832.5, 2466128.5, 2473456.5, 2480752.5, 2488048.5, 2495344.5, 2502672.5, 2509968.5, 2517264.5, 2524624.5 };

    /**
     * Array with the names of each file with chebychev coefficients.
     */
    private String[] filenames = { "ascp1600.405", "ascp1620.405", "ascp1640.405", "ascp1660.405", "ascp1680.405", "ascp1700.405", "ascp1720.405", "ascp1740.405", "ascp1760.405", "ascp1780.405", "ascp1800.405", "ascp1820.405", "ascp1840.405", "ascp1860.405", "ascp1880.405", "ascp1900.405", "ascp1920.405", "ascp1940.405", "ascp1960.405", "ascp1980.405", "ascp2000.405", "ascp2020.405", "ascp2040.405", "ascp2060.405", "ascp2080.405", "ascp2100.405", "ascp2120.405", "ascp2140.405", "ascp2160.405", "ascp2180.405" };

    /**
     * Constructor which sets the main parameters of the ephemeris.
     */
    public DE405() {
        numbersperinterval = 1016;
        denomber = 405;
        startepoch = 2305424.50;
        finalepoch = 2524624.50;
        intervalduration = 32;
        clight = 0.299792457999999984e+06;
        au = 0.149597870691000015e+09;
        emrat = 0.813005600000000044e+02;
        gm1 = 0.491254745145081187e-10;
        gm2 = 0.724345248616270270e-09;
        gmb = 0.899701134671249882e-09;
        gm4 = 0.954953510577925806e-10;
        gm5 = 0.282534590952422643e-06;
        gm6 = 0.845971518568065874e-07;
        gm7 = 0.129202491678196939e-07;
        gm8 = 0.152435890078427628e-07;
        gm9 = 0.218869976542596968e-11;
        gms = 0.295912208285591095e-03;
        ephemeriscoefficients = new double[233681];
    }

    /**
     * Method to read the DE405 ASCII ephemeris file corresponding to 'jultime'.
     * The start and final dates of the ephemeris file are set, as are the
     * Chebyshev polynomials coefficients for Mercury, Venus, Earth-Moon, Mars,
     * Jupiter, Saturn, Uranus, Neptune, Pluto, Geocentric Moon, Sun, nutations
     * and lunar librations.
     * 
     * @param jultime
     *            Julian date for calculation.
     * @return true if all reading procedures is OK and 'jultime' is in properly
     *         interval.
     */
    @Override
    protected boolean readEphCoeff(double jultime) {
        boolean result = false;
        if ((jultime < this.startepoch) || (jultime >= this.finalepoch)) {
            return result;
        }
        if ((jultime < this.ephemerisdates[1]) || (jultime >= this.ephemerisdates[2])) {
            int mantissa1 = 0;
            int mantissa2 = 0;
            int exponent = 0;
            int i = 0;
            int records = 0;
            int j = 0;
            String filename = " ";
            String line = " ";
            try {
                for (i = 0; i < startfiledates.length - 1; i++) {
                    if ((jultime >= startfiledates[i]) && (jultime < startfiledates[i + 1])) {
                        ephemerisdates[1] = startfiledates[i];
                        ephemerisdates[2] = startfiledates[i + 1];
                        filename = filenames[i];
                        records = (int) (ephemerisdates[2] - ephemerisdates[1]) / intervalduration;
                    }
                }
                filename = this.patheph + filename;
                FileReader file = new FileReader(filename);
                BufferedReader buff = new BufferedReader(file);
                for (j = 1; j <= records; j++) {
                    line = buff.readLine();
                    for (i = 2; i <= 341; i++) {
                        line = buff.readLine();
                        if (i > 2) {
                            mantissa1 = Integer.parseInt(line.substring(4, 13));
                            mantissa2 = Integer.parseInt(line.substring(13, 22));
                            exponent = Integer.parseInt(line.substring(24, 26));
                            if (line.substring(23, 24).equals("+")) {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) - 1)] = mantissa1 * Math.pow(10, (exponent - 9)) + mantissa2 * Math.pow(10, (exponent - 18));
                            } else {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) - 1)] = mantissa1 * Math.pow(10, -(exponent + 9)) + mantissa2 * Math.pow(10, -(exponent + 18));
                            }
                            if (line.substring(1, 2).equals("-")) {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) - 1)] = -ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) - 1)];
                            }
                        }
                        if ((i > 2) & (i < 341)) {
                            mantissa1 = Integer.parseInt(line.substring(30, 39));
                            mantissa2 = Integer.parseInt(line.substring(39, 48));
                            exponent = Integer.parseInt(line.substring(50, 52));
                            if (line.substring(49, 50).equals("+")) {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + 3 * (i - 2)] = mantissa1 * Math.pow(10, (exponent - 9)) + mantissa2 * Math.pow(10, (exponent - 18));
                            } else {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + 3 * (i - 2)] = mantissa1 * Math.pow(10, -(exponent + 9)) + mantissa2 * Math.pow(10, -(exponent + 18));
                            }
                            if (line.substring(27, 28).equals("-")) {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + 3 * (i - 2)] = -ephemeriscoefficients[(j - 1) * numbersperinterval + 3 * (i - 2)];
                            }
                        }
                        if (i < 341) {
                            mantissa1 = Integer.parseInt(line.substring(56, 65));
                            mantissa2 = Integer.parseInt(line.substring(65, 74));
                            exponent = Integer.parseInt(line.substring(76, 78));
                            if (line.substring(75, 76).equals("+")) {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) + 1)] = mantissa1 * Math.pow(10, (exponent - 9)) + mantissa2 * Math.pow(10, (exponent - 18));
                            } else {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) + 1)] = mantissa1 * Math.pow(10, -(exponent + 9)) + mantissa2 * Math.pow(10, -(exponent + 18));
                            }
                            if (line.substring(53, 54).equals("-")) {
                                ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) + 1)] = -ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) + 1)];
                            }
                        }
                    }
                }
                buff.close();
                result = true;
            } catch (IOException e) {
                System.out.println("Error = " + e.toString());
            } catch (StringIndexOutOfBoundsException e) {
                System.out.println("Error = " + e.toString());
            }
        } else {
            result = true;
        }
        return result;
    }
}
