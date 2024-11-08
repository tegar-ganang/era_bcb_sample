package jderead;

import java.io.FileReader;
import java.io.IOException;

/**
 * This class sets constants and read from ASCII file coefficients of Chebyshev
 * polynomials for JPL ephemeris DE406. Class is the successor of DEheader. All
 * necessary files can be found in :
 * "ftp://ssd.jpl.nasa.gov/pub/eph/planets/ascii/de406/"
 * 
 * @author Peter Hristozov E-mail: peterhri@hotmail.com
 * @version 1.3 2011 Jun 15.
 */
public class DE406 extends DEheader {

    /**
     * Array with starting dates for each file with chebychev coefficients.
     */
    private double[] startfiledates = { 625360.5, 661776.5, 698320.5, 734864.5, 771344.5, 807888.5, 844432.5, 880976.5, 917456.5, 954000.5, 990544.5, 1027024.5, 1063568.5, 1100112.5, 1136656.5, 1173136.5, 1209680.5, 1246224.5, 1282704.5, 1319248.5, 1355792.5, 1392272.5, 1428816.5, 1465360.5, 1501904.5, 1538384.5, 1574928.5, 1611472.5, 1647952.5, 1684496.5, 1721040.5, 1757520.5, 1794064.5, 1830608.5, 1867152.5, 1903632.5, 1940176.5, 1976720.5, 2013200.5, 2049744.5, 2086288.5, 2122832.5, 2159312.5, 2195856.5, 2232400.5, 2268880.5, 2305424.5, 2341968.5, 2378448.5, 2414992.5, 2451536.5, 2488016.5, 2524560.5, 2561104.5, 2597584.5, 2634128.5, 2670672.5, 2707152.5, 2743696.5, 2780240.5, 2816848.5 };

    /**
     * Array with the names of each file with chebychev coefficients.
     */
    private String[] filenames = { "ascm3000.406", "ascm2900.406", "ascm2800.406", "ascm2700.406", "ascm2600.406", "ascm2500.406", "ascm2400.406", "ascm2300.406", "ascm2200.406", "ascm2100.406", "ascm2000.406", "ascm1900.406", "ascm1800.406", "ascm1700.406", "ascm1600.406", "ascm1500.406", "ascm1400.406", "ascm1300.406", "ascm1200.406", "ascm1100.406", "ascm1000.406", "ascm0900.406", "ascm0800.406", "ascm0700.406", "ascm0600.406", "ascm0500.406", "ascm0400.406", "ascm0300.406", "ascm0200.406", "ascm0100.406", "ascp0000.406", "ascp0100.406", "ascp0200.406", "ascp0300.406", "ascp0400.406", "ascp0500.406", "ascp0600.406", "ascp0700.406", "ascp0800.406", "ascp0900.406", "ascp1000.406", "ascp1100.406", "ascp1200.406", "ascp1300.406", "ascp1400.406", "ascp1500.406", "ascp1600.406", "ascp1700.406", "ascp1800.406", "ascp1900.406", "ascp2000.406", "ascp2100.406", "ascp2200.406", "ascp2300.406", "ascp2400.406", "ascp2500.406", "ascp2600.406", "ascp2700.406", "ascp2800.406", "ascp2900.406" };

    /**
     * Constructor which sets the main parameters of the ephemeris.
     */
    public DE406() {
        numbersperinterval = 726;
        denomber = 406;
        startepoch = 625360.50;
        finalepoch = 2816848.50;
        intervalduration = 64;
        clight = 299792.458;
        au = 149597870.691;
        emrat = 81.30056;
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
        numberofcoefsets[1] = 4;
        numberofcoefsets[2] = 1;
        numberofcoefsets[3] = 2;
        numberofcoefsets[4] = 1;
        numberofcoefsets[5] = 1;
        numberofcoefsets[6] = 1;
        numberofcoefsets[7] = 1;
        numberofcoefsets[8] = 1;
        numberofcoefsets[9] = 1;
        numberofcoefsets[10] = 8;
        numberofcoefsets[11] = 1;
        numberofcoefsets[12] = 0;
        numberofcoefsets[13] = 0;
        numberofcoefs[1] = 14;
        numberofcoefs[2] = 12;
        numberofcoefs[3] = 9;
        numberofcoefs[4] = 10;
        numberofcoefs[5] = 6;
        numberofcoefs[6] = 6;
        numberofcoefs[7] = 6;
        numberofcoefs[8] = 6;
        numberofcoefs[9] = 6;
        numberofcoefs[10] = 13;
        numberofcoefs[11] = 12;
        numberofcoefs[12] = 0;
        numberofcoefs[13] = 0;
        ephemeriscoefficients = new double[415273];
    }

    /**
     * Method to read the DE406 ASCII ephemeris file corresponding to 'jultime'.
     * The start and final dates of the ephemeris file are set, as are the
     * Chebyshev polynomials coefficients for Mercury, Venus, Earth-Moon, Mars,
     * Jupiter, Saturn, Uranus, Neptune, Pluto, Geocentric Moon and Sun.
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
            char[] cline = new char[70];
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
                    for (i = 2; i <= 244; i++) {
                        file.read(cline, 0, 70);
                        cline[19] = 'e';
                        cline[42] = 'e';
                        cline[65] = 'e';
                        if (i > 2) {
                            ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) - 1)] = Double.parseDouble(String.valueOf(cline, 1, 22));
                        }
                        if (i > 2) {
                            ephemeriscoefficients[(j - 1) * numbersperinterval + 3 * (i - 2)] = Double.parseDouble(String.valueOf(cline, 24, 22));
                        }
                        if (i < 244) {
                            ephemeriscoefficients[(j - 1) * numbersperinterval + (3 * (i - 2) + 1)] = Double.parseDouble(String.valueOf(cline, 47, 22));
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
