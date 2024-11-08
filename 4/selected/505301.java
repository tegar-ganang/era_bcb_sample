package testRow.palms;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Used to generate PALMS test data
 * @author rzeszotj based off of shaferia
 */
public class GenData {

    private boolean warn_overwrite;

    private static String THISLOC = "testRow/PALMS/";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private File f;

    /**
	 * Use for one-time generation of data
	 * @param args	not used.
	 */
    public static void main(String[] args) {
        GenData d = new GenData();
        d.warn_overwrite = true;
        int items = 5;
        int mznum = 10;
        int mzscale = 1;
        long tstart = 3114199800l;
        long tdelta = 600;
        d.writeData("Test", items, mznum, mzscale, tstart, tdelta, new int[] { 2, 5, 6, 10 });
    }

    /**
	 * Generate sample PALMS data
	 * @param items	the number of PALMS items to write
	 * @param mzmin the minimum in the range of m/z valued to consider
	 * @param mzmax	the maximum in the range of m/z values to consider
	 * @param peaks	a list containing the m/z values at which PALMS items should have peaks (in range 1..mzlen)
	 * @param tstart	the time at which to start the particle timeseries
	 * @param tdelta	the change in time per particle
	 * @return relative pathnames of the files created: {datasetname, timeseriesname, mzname}
	 */
    public static String[] generate(String[] fnames, int items, int mznum, int mzscale, int[] peaks, long tstart, long tdelta) {
        GenData d = new GenData();
        d.warn_overwrite = false;
        d.writeData(fnames[0], items, mznum, mzscale, tstart, tdelta, peaks);
        for (int i = 0; i < fnames.length; ++i) fnames[i] = THISLOC + fnames[i] + ".txt";
        return fnames;
    }

    /**
	 * Get a PrintWriter that outputs to the specified file
	 * @param fname the name of the file
	 * @return a PrintWriter on file fname
	 */
    private PrintWriter getWriter(String fname) {
        f = new File(THISLOC + fname);
        if (warn_overwrite && f.exists()) System.out.printf("Warning: file %s already exists; overwriting.\n", fname);
        try {
            return new PrintWriter(new BufferedWriter(new FileWriter(f)));
        } catch (IOException ex) {
            System.out.printf("Error: Could not create file %s\n", fname);
            ex.printStackTrace();
            return null;
        }
    }

    /**
	 * Write the 2D matrix data file for the PALMS test data
	 * @param name	the dataset name (name and first line of file)
	 * @param items	the number of PALMS items to write to the file
	 * @param mzmin	the minimum of m/z values in the data
	 * @param mzmax the maximum of m/z values in the data
	 * @param tstart the starting time for particles
	 * @param tdelta the change in time between particles
	 * @param peaks	the m/z values at which to write non-negative values
	 * 
	 */
    public void writeData(String name, int items, int mznum, int mzscale, long tstart, long tdelta, int[] peaks) {
        PrintWriter file = getWriter(name + ".txt");
        file.println("999 9999");
        file.println("Doe, John");
        file.println("TEST Lab");
        if (mzscale == 1) file.println("PALMS Positive Ion Data"); else if (mzscale == -1) file.println("PALMS Negative Ion Data"); else file.println("PALMS GJIFJIGJ Ion Data");
        file.println("TEST Mission");
        file.println("1 1");
        file.println("1970 01 01 2008 07 09");
        file.println("0");
        file.println("TIME (UT SECONDS)");
        file.println(mznum + 4);
        for (int i = 0; i < mznum + 4; i++) file.println("1.0");
        for (int i = 0; i < mznum + 4; i++) file.println("9.9E29");
        file.println("TOTION total MCP signal (electron units)");
        file.println("HMASS high mass integral (fraction)");
        file.println("UNLIST (unlisted low mass peaks (fraction)");
        file.println("UFO unidentified peaks (fraction)");
        for (int i = 1; i <= mznum; i++) file.println("MS" + i + " (fraction)");
        int header2length = 13;
        file.println(header2length);
        for (int i = 0; i < header2length; i++) file.println("1.0");
        for (int i = 0; i < header2length; i++) file.println("9.9E29");
        file.println("AirCraftTime aircraft time (s)");
        file.println("INDEX index ()");
        file.println("SCAT scatter (V)");
        file.println("JMETER joule meter ()");
        file.println("ND neutral density (fraction)");
        file.println("SCALEA Mass scale intercept (us)");
        file.println("SCALEB mass scale slope (us)");
        file.println("NUMPKS number of peaks ()");
        file.println("CONF confidence (coded)");
        file.println("CAT preliminary category ()");
        file.println("AeroDiam aerodynamic diameter (um)");
        file.println("AeroDiam1p7 aero diam if density=1.7 (um)");
        file.println("TOTBACK total background subtracted (electron units)");
        file.println("0");
        file.println("0");
        String nothing = "0.000000";
        for (int i = 0; i < items; i++) {
            file.println(tstart + (tdelta * i));
            file.println(tstart + (tdelta * i) - 3);
            file.println(i + 1);
            for (int j = 0; j < 15; j++) file.println(Math.random());
            boolean peaked = false;
            for (int k = 1; k <= mznum; k++) {
                for (int j = 0; j < peaks.length && !peaked; j++) if (k == peaks[j]) {
                    double randData = (int) (1000000 * (j + 1));
                    file.println(randData / 1000000);
                    peaked = true;
                }
                if (!peaked) file.println(nothing);
                peaked = false;
            }
        }
        try {
            Scanner test = new Scanner(f);
            while (test.hasNext()) {
                System.out.println(test.nextLine());
            }
            System.out.println("test");
        } catch (Exception e) {
        }
        file.close();
    }
}
