package testRow.spass;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Used to generate SPASS test data
 * @author rzeszotj based off of shaferia
 */
public class GenData {

    private boolean warn_overwrite;

    public static final int[] peakVals = { 10, 20, 30, 40, 50, 60, 70, 80, 99 };

    private static String THISLOC = "testRow/spass/";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private File f;

    /**
	 * Use for one-time generation of data
	 * @param args	not used.
	 */
    public static void main(String[] args) {
        GenData d = new GenData();
        d.warn_overwrite = true;
        int items = 20;
        int mzmin = -10;
        int mzmax = 10;
        long tstart = 3114199800l;
        long tdelta = 600;
        d.writeData("Test", items, mzmin, mzmax, tstart, tdelta, new int[] { 2, 5, 6 });
    }

    /**
	 * Generate sample SPASS data
	 * @param items	the number of SPASS items to write
	 * @param mzmin the minimum in the range of m/z valued to consider
	 * @param mzmax	the maximum in the range of m/z values to consider
	 * @param peaks	a list containing the m/z values at which SPASS items should have peaks (in range 1..mzlen)
	 * @param tstart	the time at which to start the particle timeseries
	 * @param tdelta	the change in time per particle
	 * @return relative pathnames of the files created: {datasetname, timeseriesname, mzname}
	 */
    public static String[] generate(String[] fnames, int items, int mzmin, int mzmax, int[] peaks, long tstart, long tdelta) {
        GenData d = new GenData();
        d.warn_overwrite = false;
        d.writeData(fnames[0], items, mzmin, mzmax, tstart, tdelta, peaks);
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
	 * Write the 2D matrix data file for the SPASS test data
	 * @param name	the dataset name (name and first line of file)
	 * @param items	the number of SPASS items to write to the file
	 * @param mzmin	the minimum of m/z values in the data
	 * @param mzmax the maximum of m/z values in the data
	 * @param tstart the starting time for particles
	 * @param tdelta the change in time between particles
	 * @param peaks	the m/z values at which to write non-negative values
	 * 
	 */
    public void writeData(String name, int items, int mzmin, int mzmax, long tstart, long tdelta, int[] peaks) {
        PrintWriter file = getWriter(name + ".txt");
        file.print("Filename\t");
        file.print("Date\t");
        file.print("Acquisition #\t");
        file.print("�m Diameter\t");
        for (int i = mzmin; i <= mzmax; i++) file.print(i + "\t");
        file.println();
        int nothing = 0;
        String fileLoc = "C:/abcd/" + name + ".txt\t";
        Date tempDate;
        for (int i = 0; i < items; i++) {
            tempDate = new Date(tstart);
            tstart += tdelta;
            file.print(fileLoc);
            file.print(dateFormat.format(tempDate) + "\t");
            file.print(i + 1 + "\t");
            double t = (double) (i) / 10;
            file.print(t + "\t");
            boolean peaked = false;
            for (int k = mzmin; k <= mzmax; k++) {
                for (int j = 0; j < peaks.length && !peaked; j++) {
                    if (k == peaks[j]) {
                        file.print(peakVals[j % peakVals.length] + "\t");
                        peaked = true;
                    }
                }
                if (!peaked) {
                    if (k == mzmax) file.print(nothing); else file.print(nothing + "\t");
                }
                peaked = false;
            }
            file.println();
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
