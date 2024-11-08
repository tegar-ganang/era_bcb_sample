package com.oreditions.csvconverter.controller;

import com.oreditions.csvconverter.operation.*;
import com.oreditions.csvconverter.exception.AggregationException;
import com.oreditions.csvconverter.util.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author HO.OREY
 */
public class Controller implements Process {

    /**
     * Constants
     */
    protected static final String SEP1 = "|";

    protected static final String SEP2 = "=";

    protected static final String SEP3 = ":";

    protected static final String SEP4 = ",";

    protected static final String COM = "#";

    /**
     * General options
     */
    protected boolean verbose = false;

    /**
     * Control file management
     */
    protected String controlfilename = null;

    protected BufferedReader reader = null;

    /**
     * source file management
     */
    protected String sourcefile = null;

    protected CSVLine sourcefileheader = null;

    protected BufferedReader inputfilereader = null;

    protected int inputnbcol = 0;

    /**
     * Target file management
     */
    protected CSVLine targetfileheader = null;

    protected String targetfile = null;

    protected BufferedWriter outputfilewriter = null;

    protected int outputnbcol = 0;

    /**
     * error file management
     */
    protected String errorfile = null;

    protected BufferedWriter errorfilewriter = null;

    /**
     * rejected lines file management
     */
    protected String rejectedlines = null;

    protected BufferedWriter rejectedlineswriter = null;

    /**
     * aggregation file management
     * Note that is a separate process
     */
    protected String aggsource = null;

    protected String aggtarget = null;

    protected String aggkey = null;

    protected String aggamount = null;

    protected AggregationProcessor aggcontroller = null;

    protected boolean aggregationcommand = false;

    protected boolean aggheader = false;

    /**
     * Operations
     */
    protected Vector<Operation> ops = new Vector<Operation>();

    /**
     * Construct
     * @param reader
     * @param verbose
     */
    public Controller(String controlfilename, boolean verbose) {
        this.controlfilename = controlfilename;
        try {
            reader = new BufferedReader(new FileReader(controlfilename));
        } catch (FileNotFoundException e) {
            System.err.println("Control file : " + controlfilename + " not" + "found");
            throw new RuntimeException();
        }
        this.verbose = verbose;
    }

    /**
     * Parse command file. Main entry point of the controller.
     * @return
     */
    public int parseCommandFile() {
        String line = null;
        try {
            int k = 0;
            while ((line = reader.readLine()) != null) {
                k = parseLine(line);
                if (k != 0) throw new RuntimeException("Error in parsing line");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        int i = prepareDataFiles();
        if (i != 0) throw new RuntimeException();
        return 0;
    }

    /**
     * Run mapping
     */
    public int process() {
        OperationProcessor proc = new OperationProcessor(inputfilereader, outputfilewriter, errorfilewriter, rejectedlineswriter, ops, inputnbcol, outputnbcol, true);
        proc.process();
        try {
            AggregationProcessor proc2 = new AggregationProcessor(aggsource, aggtarget, aggkey, aggamount, aggheader, verbose);
            proc2.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /************************************************************
     * parseLine method
     * This method is protected, invoked by the ParseCommandFile
     * method and is reading all the options in the command line
     * of the command file.
     * @param line
     * @return int ; 0 is success, 1 is error, please raise an
     * exception when 1 is returned
     ************************************************************/
    protected int parseLine(String line) {
        String tline = line.trim();
        if (tline.equals("")) {
            if (verbose) System.out.println("Controller : Void line parsed");
            return 0;
        }
        if (tline.startsWith(COM)) {
            if (verbose) System.out.println("Controller : Comment line");
            return 0;
        }
        if (tline.startsWith("sourcefile=")) {
            sourcefile = line.substring(11);
            if (verbose) System.out.println("Controller : Sourcefile line parsed");
            return 0;
        }
        if (tline.startsWith("targetfile=")) {
            String ss = line.substring(11);
            StringTokenizer st = new StringTokenizer(ss, SEP1);
            targetfile = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(st.nextToken(), SEP2);
            String temp = st2.nextToken();
            if (!(temp.equals("header"))) {
                System.err.println("Unrecognized syntax on command file, line:");
                System.err.println("-->" + line);
                System.err.println("Expecting 'header' keyword instead of '" + temp + "'");
                return 1;
            }
            temp = st2.nextToken();
            targetfileheader = new CSVLine(temp);
            if (verbose) System.out.println("Controller : Target file line parsed");
            return 0;
        }
        if (tline.startsWith("errorfile=")) {
            errorfile = line.substring(10);
            if (verbose) System.out.println("Controller : Error file line parsed");
            return 0;
        }
        if (tline.startsWith("rejectedlines=")) {
            rejectedlines = line.substring(14);
            if (verbose) System.out.println("Controller : Rejected lines file" + " line parsed");
            return 0;
        }
        if (tline.startsWith("C")) {
            StringTokenizer st = new StringTokenizer(tline, SEP1);
            String cols = st.nextToken();
            StringTokenizer st2 = new StringTokenizer(cols, SEP3);
            String sourcecols = st2.nextToken();
            Vector<Integer> scols = ColumnListDecoder.decode(sourcecols, SEP4);
            String destcols = st2.nextToken();
            Vector<Integer> dcols = ColumnListDecoder.decode(destcols, SEP4);
            String mf = st.nextToken();
            String mapfilename = null;
            if (mf.startsWith("map=")) {
                mapfilename = mf.substring("map=".length());
                HashMap<String, String> map = loadMapFile(mapfilename, scols.size(), dcols.size());
                String temp3 = null;
                try {
                    temp3 = st.nextToken();
                } catch (NoSuchElementException e) {
                    ops.add(new MapOperation(scols, dcols, map, null));
                    return 0;
                }
                if (temp3 == null) {
                    ops.add(new MapOperation(scols, dcols, map, null));
                    return 0;
                }
                if (!(temp3.startsWith("default="))) {
                    System.err.println("Incorrect syntax for default" + " value of mapping file, line:");
                    System.err.println("-->" + line);
                    System.err.println("Expecting : '|default=DEF1;DEF2'");
                    System.err.println("Ignoring default command and try " + "to work without it");
                    ops.add(new MapOperation(scols, dcols, map, null));
                    return 0;
                }
                String defstring = temp3.substring("default=".length());
                CSVLine check = new CSVLine(defstring);
                if (check.getColumnNumber() != dcols.size()) {
                    System.err.println("Incorrect syntax for default" + " value of mapping file, line:");
                    System.err.println("-->" + line);
                    System.err.println("Expecting : " + dcols.size() + " number of parameters and finding : " + check.getColumnNumber());
                    System.err.println("Ignoring default command and try " + "to work without it");
                    ops.add(new MapOperation(scols, dcols, map, null));
                    return 0;
                }
                ops.add(new MapOperation(scols, dcols, map, defstring));
                return 0;
            }
            if (mf.startsWith("copy")) {
                if ((scols.size() != 1) || (dcols.size() != 1)) {
                    System.err.println("Incorrect number of parameters in " + "'copy', line:");
                    System.err.println("-->" + line);
                    System.err.println("Expecting only one source column" + " and one target column with 'copy' keyword");
                    return 1;
                }
                ops.add(new CopyOperation(scols.firstElement().intValue(), dcols.firstElement().intValue()));
            } else {
                System.err.println("Unrecognized syntax on command file, line:");
                System.err.println("-->" + line);
                System.err.println("Expecting 'map=' keyword instead of '" + mf + "'");
                return 1;
            }
            if (verbose) System.out.println("Controller : Command line parsed");
            return 0;
        }
        if (tline.startsWith("aggregationsourcefile=")) {
            aggsource = line.substring("aggregationsourcefile=".length());
            aggregationcommand = true;
            if (verbose) System.out.println("Controller : Aggregation source file" + " line parsed");
            return 0;
        }
        if (tline.startsWith("aggregationtargetfile=")) {
            aggtarget = line.substring("aggregationtargetfile=".length());
            if (verbose) System.out.println("Controller : Aggregation target file" + " line parsed");
            return 0;
        }
        if (tline.startsWith("aggregationkey=")) {
            aggkey = line.substring("aggregationkey=".length());
            if (verbose) System.out.println("Controller : Aggregation key" + " line parsed");
            return 0;
        }
        if (tline.startsWith("aggregationamountfield=")) {
            aggamount = line.substring("aggregationamountfield=".length());
            if (verbose) System.out.println("Controller : Aggregation amount " + "field line parsed");
            return 0;
        }
        if (tline.startsWith("aggregationheader=")) {
            String temp = line.substring("aggregationheader=".length());
            if (temp.equals("yes")) aggheader = true;
            if (verbose) System.out.println("Controller : Aggregation header " + "field line parsed, header is set to " + aggheader);
            return 0;
        }
        return 0;
    }

    /**
     * Objective of this method is to map the file with the context of the 
     * operation so to check first the adequation of the operation and the map 
     * file
     * @param filename
     * @param ssize
     * @param dsize
     */
    protected HashMap<String, String> loadMapFile(String filename, int ssize, int dsize) {
        HashMap<String, String> map = new HashMap<String, String>();
        try {
            BufferedReader filereader = new BufferedReader(new FileReader(filename));
            String line = filereader.readLine();
            if (line == null) {
                System.err.println("Map file : '" + filename + "' empty");
                throw new RuntimeException();
            }
            CSVLine temp = new CSVLine(line);
            int n = temp.getColumnNumber();
            if (n != (ssize + dsize)) {
                System.err.println("Map file has not the required number of " + "columns. The system found " + n + " columns, whereas " + "the command contains " + ssize + " columns to map into " + dsize + " columns");
                throw new RuntimeException();
            }
            while ((line = filereader.readLine()) != null) {
                temp = new CSVLine(line);
                Vector<CSVLine> toto = temp.splitLineAtIndex(ssize);
                map.put(toto.get(0).serializeCSVLine(), toto.get(1).serializeCSVLine());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Map file issue on " + filename);
            System.exit(1);
        }
        return map;
    }

    /**
     * This method prepare all cursors in files
     * The objective if to let the file open for big volumes treatments
     * @return int 0 is OK, 1 if KO
     */
    protected int prepareDataFiles() {
        if (sourcefile == null) {
            System.err.println("No source file found. Check the syntax " + "in the command file");
            return 1;
        }
        if (targetfile == null) {
            System.err.println("Warning: no target file found in the command file. " + "Taking a default name : output.csv");
            targetfile = "output.csv";
        }
        if (errorfile == null) {
            System.err.println("Warning: no error file found in the command file. " + "Taking a default name : errors.txt");
            errorfile = "errors.txt";
        }
        if (rejectedlines == null) {
            System.err.println("Warning: no rejected lines file found" + " in the command file. " + "Taking a default name : rejectedlines.csv");
            errorfile = "rejectedlines.csv";
        }
        try {
            inputfilereader = new BufferedReader(new FileReader(sourcefile));
            sourcefileheader = new CSVLine(inputfilereader.readLine());
            inputnbcol = sourcefileheader.getColumnNumber();
        } catch (Exception e) {
            System.err.println("Input data file : " + sourcefile + " not " + "found");
            return 1;
        }
        try {
            outputfilewriter = new BufferedWriter(new FileWriter(targetfile));
            outputfilewriter.write(targetfileheader.serializeCSVLine());
            outputfilewriter.newLine();
            outputfilewriter.flush();
            outputnbcol = targetfileheader.getColumnNumber();
        } catch (IOException e) {
            System.err.println("Problem in writing in output data file : " + targetfile);
            e.printStackTrace();
            return 1;
        }
        try {
            errorfilewriter = new BufferedWriter(new FileWriter(errorfile));
            errorfilewriter.write("===================================");
            errorfilewriter.newLine();
            errorfilewriter.write("ERROR FILE");
            errorfilewriter.newLine();
            errorfilewriter.flush();
        } catch (IOException e) {
            System.err.println("Problem in writing in error file : " + targetfile);
            e.printStackTrace();
            return 1;
        }
        try {
            rejectedlineswriter = new BufferedWriter(new FileWriter(rejectedlines));
            rejectedlineswriter.write(sourcefileheader.serializeCSVLine());
            rejectedlineswriter.newLine();
            rejectedlineswriter.flush();
        } catch (IOException e) {
            System.err.println("Problem in writing in rejected lines file : " + targetfile);
            e.printStackTrace();
            return 1;
        }
        if (aggregationcommand) {
            try {
                aggcontroller = new AggregationProcessor(aggsource, aggtarget, aggkey, aggamount, aggheader, verbose);
            } catch (IOException e) {
                aggregationcommand = false;
            } catch (AggregationException e) {
                System.err.println(e.getMessage());
                switch(e.getProblemId()) {
                    case AggregationException.KEY_PROBLEM:
                        System.err.println("Expecting : 'key=C2,C4' " + "like statement");
                        break;
                    case AggregationException.AMOUNT_PROBLEM:
                        System.err.println("Expecting : 'aggregationamountfield=C5' " + "like statement");
                        break;
                    default:
                        break;
                }
                System.err.println("Turning aggregation mode off");
                aggregationcommand = false;
            }
        }
        return 0;
    }

    /**
     * Let's terminate on a clean note !
     */
    public void closeFiles() {
        try {
            inputfilereader.close();
            outputfilewriter.close();
            errorfilewriter.close();
            rejectedlineswriter.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error in some file closing");
        }
    }
}
