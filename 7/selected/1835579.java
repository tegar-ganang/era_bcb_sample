package jderead;

import java.io.FileReader;
import java.io.IOException;

/**
 * This class sets constants and read from ASCII file coefficients of Chebyshev
 * polynomials for JPL ephemeris DE422. Class is the successor of DEheader. All
 * necessary files can be found in :
 * "ftp://ssd.jpl.nasa.gov/pub/eph/planets/ascii/de422/"
 * 
 * @author Peter Hristozov E-mail: peterhri@hotmail.com
 * @version 1.3 2011 Jun 15.
 */
public class DE422 extends DEheader {

    /**
     * Array with starting dates for each file with chebychev coefficients.
     */
    private double[] startfiledates = { 625648.5, 661808.5, 698352.5, 734864.5, 771376.5, 807920.5, 844432.5, 880976.5, 917488.5, 954032.5, 990544.5, 1027056.5, 1063600.5, 1100112.5, 1136656.5, 1173168.5, 1209680.5, 1246224.5, 1282736.5, 1319280.5, 1355792.5, 1392304.5, 1428848.5, 1465360.5, 1501904.5, 1538416.5, 1574928.5, 1611472.5, 1647984.5, 1684528.5, 1721040.5, 1757552.5, 1794096.5, 1830608.5, 1867152.5, 1903664.5, 1940176.5, 1976720.5, 2013232.5, 2049776.5, 2086288.5, 2122832.5, 2159344.5, 2195856.5, 2232400.5, 2268912.5, 2305424.5, 2341968.5, 2378480.5, 2414992.5, 2451536.5, 2488048.5, 2524592.5, 2561104.5, 2597616.5, 2634160.5, 2670672.5, 2707184.5, 2743728.5, 2780240.5, 2816816.5 };

    /**
     * Array with the names of each file with chebychev coefficients.
     */
    private String[] filenames = { "ascm3000.422", "ascm2900.422", "ascm2800.422", "ascm2700.422", "ascm2600.422", "ascm2500.422", "ascm2400.422", "ascm2300.422", "ascm2200.422", "ascm2100.422", "ascm2000.422", "ascm1900.422", "ascm1800.422", "ascm1700.422", "ascm1600.422", "ascm1500.422", "ascm1400.422", "ascm1300.422", "ascm1200.422", "ascm1100.422", "ascm1000.422", "ascm0900.422", "ascm0800.422", "ascm0700.422", "ascm0600.422", "ascm0500.422", "ascm0400.422", "ascm0300.422", "ascm0200.422", "ascm0100.422", "ascp0000.422", "ascp0100.422", "ascp0200.422", "ascp0300.422", "ascp0400.422", "ascp0500.422", "ascp0600.422", "ascp0700.422", "ascp0800.422", "ascp0900.422", "ascp1000.422", "ascp1100.422", "ascp1200.422", "ascp1300.422", "ascp1400.422", "ascp1500.422", "ascp1600.422", "ascp1700.422", "ascp1800.422", "ascp1900.422", "ascp2000.422", "ascp2100.422", "ascp2200.422", "ascp2300.422", "ascp2400.422", "ascp2500.422", "ascp2600.422", "ascp2700.422", "ascp2800.422", "ascp2900.422" };

    /**
     * Constructor which sets the main parameters of the ephemeris.
     */
    public DE422() {
        numbersperinterval = 1016;
        denomber = 422;
        startepoch = 625648.50;
        finalepoch = 2816816.50;
        intervalduration = 32;
        clight = 0.299792458000000000e+06;
        au = 0.149597870700126600e+09;
        emrat = 0.813005694044923300e+02;
        gm1 = 0.491254957186794000e-10;
        gm2 = 0.724345233269844100e-09;
        gmb = 0.899701140826804900e-09;
        gm4 = 0.954954869562239000e-10;
        gm5 = 0.282534584085505000e-06;
        gm6 = 0.845970607330847800e-07;
        gm7 = 0.129202482579265000e-07;
        gm8 = 0.152435910924974000e-07;
        gm9 = 0.217844105199052000e-11;
        gms = 0.295912208285591100e-03;
        ephemeriscoefficients = new double[1161289];
    }

    /**
     * Method to read the DE422 ASCII ephemeris file corresponding to 'jultime'.
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
            int i = 0;
            int records = 0;
            int j = 0;
            String filename = " ";
            char[] cline = new char[80];
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
                for (j = 1; j <= records; j++) {
                    file.read(cline, 0, 13);
                    for (i = 2; i <= 341; i++) {
                        file.read(cline, 0, 79);
                        cline[22] = 'e';
                        cline[48] = 'e';
                        cline[74] = 'e';
                        if (i > 2) {
                            ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) - 1)] = Double.parseDouble(String.valueOf(cline, 1, 25));
                        }
                        if ((i > 2) & (i < 341)) {
                            ephemeriscoefficients[(j - 1) * numbersperinterval + 3 * (i - 2)] = Double.parseDouble(String.valueOf(cline, 27, 25));
                        }
                        if (i < 341) {
                            ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) + 1)] = Double.parseDouble(String.valueOf(cline, 53, 25));
                        }
                    }
                }
                file.close();
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
