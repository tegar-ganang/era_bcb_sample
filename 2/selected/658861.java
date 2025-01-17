package edu.wisc.ssec.mcidas.adde;

import java.*;
import java.lang.*;
import java.util.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import visad.jmet.*;
import visad.*;
import visad.data.mcidas.*;
import visad.data.units.*;
import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

/** 
 * AddeGridReader interface for McIDAS ADDE grid data sets.   Simulates a
 * McIDAS GRDLIST request using an ADDE URL.
 *
 * <pre>
 * URLs must all have the following format:
 *
 *   for directory listing:
 *
 *   adde://host/griddirectory?keyword_1=value_1&amp;keyword_2=value_2
 *
 *   or  (for data)
 *
 *   adde://host/griddata?keyword_1=value_1&amp;keyword_2=value_2
 *
 * there can be any valid combination of the following supported keywords:
 *
 *   group=&lt;groupname&gt;         ADDE group name
 *   descr=&lt;descriptor&gt;        ADDE descriptor name
 *   param=&lt;param list&gt;        parameter code list
 *   time=&lt;model run time&gt;     time in hhmmss format
 *   day=&lt;model run day&gt;       day in ccyyddd format
 *   lev=&lt;level list&gt;          list of requested levels (value or SFC, MSL 
 *                               or TRO)
 *   ftime=&lt;forecast time&gt;     valid time (hhmmss format) (use with fday)
 *   fday=&lt;forecast day&gt;       forecast day (ccyyddd)
 *   fhour=&lt;forecast hours&gt;    forecast hours (offset from model run time)
 *                                (hhmmss format)
 *   lat=&lt;min lat&gt; &lt;max lat&gt;   latitude bounding box (needs lon specified)
 *   lon=&lt;min lon&gt; &lt;max lon&gt;   longitude bounding box (needs lat specified)
 *   row=&lt;min row&gt; &lt;max row&gt;   row bounding box (needs col specified)
 *   col=&lt;min col&gt; &lt;max col&gt;   column bounding box (needs row specified)
 *   skip=&lt;row&gt; &lt;col&gt;          skip factors for rows and columns (def = 1 1)
 *   gpro=&lt;pro&gt;                grid projection (e.g. TANC)
 *   src=&lt;s1&gt; ... &lt;s2&gt;         list of grid sources (ETA, AVN, etc)
 *   drange=&lt;btime&gt; &lt;etime&gt; &lt;inc&gt;  range of primary days 
 *                                 that the grid represents (cannot use with 
 *                                 day=)
 *   frange=&lt;btime&gt; &lt;etime&gt; &lt;inc&gt;  range of forecast times 
 *                                 that the grid represents (cannot use with
 *                                 fhour=, fday= or ftime=)
 *   trange=&lt;btime&gt; &lt;etime&gt; &lt;inc&gt;  range of primary times 
 *                                 that the grid represents (cannot use with time=)
 *   num=&lt;max&gt;                 maximum number of grids (nn) to return (def=1)
 *
 * the following keywords are required:
 *
 *   group
 *   descr
 *
 * an example URL might look like:
 *   adde://viper/griddirectory?group=rtmodel&amp;descr=eta
 * </pre>
 *
 * @author Tom Whittaker
 * 
 */
public class AddeGridReader {

    static {
        try {
            String handlers = System.getProperty("java.protocol.handler.pkgs");
            String newProperty = null;
            if (handlers == null) newProperty = "edu.wisc.ssec.mcidas"; else if (handlers.indexOf("edu.wisc.ssec.mcidas") < 0) newProperty = "edu.wisc.ssec.mcidas | " + handlers;
            if (newProperty != null) System.setProperty("java.protocol.handler.pkgs", newProperty);
        } catch (Exception e) {
            System.out.println("Unable to set System Property: java.protocol.handler.pkgs");
        }
    }

    private DataInputStream dis;

    private int status = 0;

    private URLConnection urlc;

    private char[] data;

    private final int HEARTBEAT = 11223344;

    ArrayList fileHeaders, gridHeaders, gridData;

    /**
      * allows reading of McIDAS grid headers and data
      *
      */
    public AddeGridReader() {
    }

    /**
     * creates an ArrayList of McIDASGridDirectories
     *
     * @param request ADDE URL to read from.  See class javadoc.
     *
     * <pre>
     * an example URL might look like:
     *   adde://viper/griddirectory?group=gvar&amp;type=image
     * </pre>
     *
     * @exception AddeURLException if there are no datasets of the particular
     *            type or there is an error reading data
     *
     */
    public ArrayList getGridDirectory(String request) throws AddeURLException {
        URL url;
        gridHeaders = new ArrayList();
        fileHeaders = new ArrayList();
        try {
            url = new URL(request);
            urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            dis = new DataInputStream(new BufferedInputStream(is));
        } catch (AddeURLException ae) {
            throw new AddeURLException("Dataset not found: " + ae);
        } catch (Exception e) {
            throw new AddeURLException("Error opening connection: " + e);
        }
        int numBytes = ((AddeURLConnection) urlc).getInitialRecordSize();
        if (numBytes == 0) {
            status = -1;
            throw new AddeURLException("No datasets found");
        }
        try {
            int check;
            byte[] header = new byte[256];
            while (numBytes == 4) {
                check = dis.readInt();
                if (check != HEARTBEAT) {
                    System.out.println("problem...not heartbeat = " + check);
                }
                numBytes = dis.readInt();
            }
            while ((check = dis.readInt()) == 0) {
                dis.readFully(header, 0, 256);
                String head = new String(header, 0, 32);
                fileHeaders.add(head);
                int check2;
                while ((check2 = dis.readInt()) == 0) {
                    dis.readFully(header, 0, 256);
                    String name = new String(header, 24, 4);
                    McIDASGridDirectory mg = new McIDASGridDirectory(header);
                    gridHeaders.add(mg);
                }
            }
        } catch (Exception re) {
            System.out.println(re);
        }
        return gridHeaders;
    }

    public ArrayList getGridHeaders() {
        return gridHeaders;
    }

    public ArrayList getFileHeaders() {
        return fileHeaders;
    }

    /**
     * creates an ArrayList of arrays of data, plus an ArrayList
     * of grid headers (McIDASGridDirectories) which are then
     * available using the getGridHeaders() method.
     *
     * @param request ADDE URL to read from.  See class javadoc.
     *
     * <pre>
     * an example URL might look like:
     *   adde://viper/griddata?group=abom&amp;descr=grid&amp;parm=T&amp;lev=500
     * </pre>
     *
     * @exception AddeURLException if there are no datasets of the particular
     *            type or there is an error reading data
     *
     */
    public ArrayList getGridData(String request) throws AddeURLException {
        URL url;
        gridHeaders = new ArrayList();
        gridData = new ArrayList();
        try {
            url = new URL(request);
            urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            dis = new DataInputStream(new BufferedInputStream(is));
        } catch (AddeURLException ae) {
            throw new AddeURLException("Dataset not found: " + ae);
        } catch (Exception e) {
            throw new AddeURLException("Error opening connection: " + e);
        }
        int numBytes = ((AddeURLConnection) urlc).getInitialRecordSize();
        if (numBytes == 0) {
            status = -1;
            throw new AddeURLException("No datasets found");
        }
        try {
            int check;
            byte[] header = new byte[256];
            while (numBytes == 4) {
                check = dis.readInt();
                if (check != HEARTBEAT) {
                    System.out.println("problem...not heartbeat = " + check);
                }
                numBytes = dis.readInt();
            }
            int checkBytes = dis.readInt();
            if (checkBytes != numBytes) {
                throw new AddeURLException("Invalid number of bytes returned for grid.");
            }
            int numGrids = dis.readInt();
            for (int i = 0; i < numGrids; i++) {
                dis.readFully(header, 0, 256);
                String name = new String(header, 24, 4);
                McIDASGridDirectory mg = new McIDASGridDirectory(header);
                System.out.println(mg.toString());
                CoordinateSystem c = mg.getCoordinateSystem();
                gridHeaders.add(mg);
                int rows = mg.getRows();
                int cols = mg.getColumns();
                double scale = mg.getParamScale();
                double[] ddata = new double[rows * cols];
                int n = 0;
                for (int nc = 0; nc < cols; nc++) {
                    for (int nr = 0; nr < rows; nr++) {
                        int temp = dis.readInt();
                        ddata[(rows - nr - 1) * cols + nc] = (temp == McIDASUtil.MCMISSING) ? Double.NaN : ((double) temp) / scale;
                    }
                }
                gridData.add(ddata);
                check = dis.readInt();
                if (check != 0) break;
            }
        } catch (Exception re) {
            System.out.println(re);
        }
        return gridData;
    }

    /** test by running 'java edu.wisc.ssec.mcidas.adde.AddeGridReader' */
    public static void main(String[] args) throws Exception {
        String request = (args.length == 0) ? "adde://sweetpea.ssec.wisc.edu/grid?group=ABOM&descr=GRIDS&pos=302&num=40&lev=500&proj=6999&user=tomw&" : args[0];
        AddeGridReader d = new AddeGridReader();
        ArrayList v = d.getGridData(request);
        ArrayList vd = d.getGridHeaders();
    }
}
