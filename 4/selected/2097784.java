package jeplus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

/**
 * EnergyPlus batch simulation result collector
 * @author yzhang
 */
public class EPlusDefaultResultCollector {

    /** Job's owner provide information of the jobs run */
    protected EPlusBatch JobOwner = null;

    /** The output folder of simulation results */
    protected String OutputDir = null;

    /** Report header list */
    protected ArrayList<String> ReportHeader = null;

    /** Table for report data */
    protected ArrayList<ArrayList<String>> ReportTable = null;

    /** Header map - column header is mapped to column number */
    protected HashMap<String, Integer> ResultHeader = null;

    /** Table for result data */
    protected ArrayList<ArrayList<String>> ResultTable = null;

    /** Process data on-the-fly instead of storing it memory */
    protected boolean OnTheFly = false;

    public interface ReportReader {

        /**
         * Read result from the named files within the given dir. New data will 
         * be added to Header and Data table. This function returns the number
         * of files read.
         * @param manager The simulation manager holds information on the jobs
         * @param dir Folder's path in which reports are stored. This is normally the working directory of the batch.
         * @param file Name of the report file(s), e.g. eplusout.end in the case of E+ simulations. This field may not contain the full file name.
         * @param header Table header of the report data
         * @param table Table content of the report data
         * @return Number of files read
         */
        public int readReport(EPlusBatch manager, String dir, String file, ArrayList<String> header, ArrayList<ArrayList<String>> table);

        /**
         * This method read result from the named file in the given dir. New data will 
         * be added to Header and Data table with the assigned job_id. This function returns the number
         * of lines read.
         * @param dir Folder path in which reports are stored. This is normally the working directory of the batch.
         * @param file Name of the report file(s), e.g. eplusout.end in the case of E+ simulations. This field may not contain the full file name.
         * @param job_id ID string of the job
         * @param header Table header of the report data
         * @param table Table content of the report data
         * @return Number of files read
         */
        public int readReport(String dir, String file, String job_id, ArrayList<String> header, ArrayList<ArrayList<String>> table);
    }

    public interface ResultReader {

        /**
         * Read result from the named files within the given dir. New data will 
         * be added to Header and Data table. This function returns the number
         * of files read.
         * @param manager The simulation manager holds information on the jobs
         * @param dir Folder's path in which reports are stored. This is normally the working directory of the batch.
         * @param file Name of the result file(s), e.g. eplusout.csv in the case of E+ simulations. This field may not contain the full file name.
         * @param header Table header of the report data
         * @param table Table content of the report data
         * @return Number of files read
         */
        public int readResult(EPlusBatch manager, String dir, String file, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);

        /**
         * This method read result from the named file in the given dir. New data will 
         * be added to Header and Data table with the assigned job_id. The header map is 
         * maintained to preserve consistency of the columns of the table. This function returns the number
         * of lines read.
         * @param dir Folder path in which reports are stored. This is normally the working directory of the batch.
         * @param file Name of the result file(s), e.g. eplusout.csv in the case of E+ simulations. This field may not contain the full file name.
         * @param job_id ID string of the job
         * @param header Table header of the report data
         * @param table Table content of the report data
         * @return Number of files read
         */
        public int readResult(String dir, String file, String job_id, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);
    }

    public interface ResultWriter {

        public void writeResult(String file, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);
    }

    public interface ReportWriter {

        public void writeResult(String file, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);
    }

    public interface IndexWriter {

        public void writeResult(String file, EPlusBatch manager);
    }

    public interface PostProcessor {

        public void postProcess(HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);
    }

    /**
     * Create collector with assigned job owner
     * @param batch Job owner
     */
    public EPlusDefaultResultCollector(EPlusBatch batch) {
        JobOwner = batch;
        OutputDir = JobOwner.getResolvedEnv().getParentDir();
    }

    public void setOutputDir(String dir) {
        OutputDir = dir;
    }

    public EPlusBatch getJobOwner() {
        return JobOwner;
    }

    public void setJobOwner(EPlusBatch JobOwner) {
        this.JobOwner = JobOwner;
    }

    public boolean isOnTheFly() {
        return OnTheFly;
    }

    public void setOnTheFly(boolean OnTheFly) {
        this.OnTheFly = OnTheFly;
    }

    public ArrayList<String> getReportHeader() {
        return ReportHeader;
    }

    public void setReportHeader(ArrayList<String> ReportHeader) {
        this.ReportHeader = ReportHeader;
    }

    public ArrayList<ArrayList<String>> getReportTable() {
        return ReportTable;
    }

    public void setReportTable(ArrayList<ArrayList<String>> ReportTable) {
        this.ReportTable = ReportTable;
    }

    public HashMap<String, Integer> getResultHeader() {
        return ResultHeader;
    }

    public void setResultHeader(HashMap<String, Integer> ResultHeader) {
        this.ResultHeader = ResultHeader;
    }

    public ArrayList<ArrayList<String>> getResultTable() {
        return ResultTable;
    }

    public void setResultTable(ArrayList<ArrayList<String>> ResultTable) {
        this.ResultTable = ResultTable;
    }

    public void collectReports(ReportReader reader, ReportWriter writer) {
    }

    public void collectResutls(ResultReader reader, PostProcessor processor, ResultWriter writer) {
    }

    public void collectIndexes(IndexWriter writer) {
    }

    /**
     * Collect the Run end information from "eplusout.end" file in each directory
     */
    public int collectReportsCSV(String filename, String dir) {
        return collectReportsCSV(filename, dir, false);
    }

    /**
     * Collect the Run end information from "eplusout.end" file in each directory
     */
    public int collectReportsCSV(String filename, String dir, boolean remove) {
        int nResCollected = 0;
        try {
            File fn = new File(OutputDir + filename);
            PrintWriter fw = new PrintWriter(new FileWriter(fn));
            String[] header = { "Id", "JobID", "Message", "Warnings", "Errors", "Hours", "Minutes", "Seconds" };
            StringBuffer buf = new StringBuffer(header[0]);
            for (int i = 1; i < header.length; i++) {
                buf.append(", ").append(header[i]);
            }
            fw.println(buf.toString());
            Vector<EPlusTask> JobQueue = JobOwner.getAgent().getFinishedJobs();
            for (int i = 0; i < JobQueue.size(); i++) {
                EPlusTask job = JobQueue.get(i);
                String[] vals = new String[header.length];
                vals[0] = Integer.toString(i);
                vals[1] = job.getJobID();
                String info = EPlusTask.getResultInfo(dir, job, remove);
                if (info != null && !info.startsWith("!")) {
                    int marker = info.indexOf("--");
                    vals[2] = info.substring(0, marker);
                    info = info.substring(marker + 2).trim();
                    String[] segment = info.split(";");
                    for (int j = 0; j < segment.length; j++) {
                        String thisseg = segment[j].trim();
                        if (thisseg.endsWith("Warning")) {
                            vals[3] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.endsWith("Severe Errors")) {
                            vals[4] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.startsWith("Elapsed Time")) {
                            vals[5] = thisseg.substring(thisseg.indexOf("=") + 1, thisseg.indexOf("hr "));
                            vals[6] = thisseg.substring(thisseg.indexOf("hr ") + 3, thisseg.indexOf("min"));
                            vals[7] = thisseg.substring(thisseg.indexOf("min ") + 4, thisseg.indexOf("sec"));
                        }
                    }
                }
                buf = new StringBuffer();
                buf.append(vals[0]);
                for (int j = 1; j < vals.length; j++) {
                    buf.append(", ").append(vals[j]);
                }
                fw.println(buf.toString());
                nResCollected++;
            }
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nResCollected;
    }

    /**
     * Collect the Run end information from "eplusout.end" file in each directory
     */
    public int collectReportsCSV(String filename, Vector<String> reports) {
        int nResCollected = 0;
        try {
            PrintWriter fw = new PrintWriter(new FileWriter(OutputDir + filename));
            String[] header = { "Id", "JobID", "Message", "Warnings", "Errors", "Hours", "Minutes", "Seconds" };
            StringBuffer buf = new StringBuffer(header[0]);
            for (int i = 1; i < header.length; i++) {
                buf.append(", ").append(header[i]);
            }
            fw.println(buf.toString());
            for (int i = 0; i < reports.size(); i++) {
                String info = reports.get(i);
                String[] vals = new String[header.length];
                vals[0] = Integer.toString(i);
                vals[1] = info.substring(info.indexOf("[") + 1, info.indexOf("]"));
                buf = new StringBuffer();
                buf.append(vals[0]).append(", ").append(vals[1]);
                info = info.substring(info.indexOf("] ") + 2);
                if (info.length() > 0 && !info.startsWith("!") && info.contains("--")) {
                    int marker = info.indexOf("--");
                    vals[2] = info.substring(0, marker);
                    info = info.substring(marker + 2).trim();
                    String[] segment = info.split(";");
                    for (int j = 0; j < segment.length; j++) {
                        String thisseg = segment[j].trim();
                        if (thisseg.endsWith("Warning")) {
                            vals[3] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.endsWith("Severe Errors")) {
                            vals[4] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.startsWith("Elapsed Time")) {
                            vals[5] = thisseg.substring(thisseg.indexOf("=") + 1, thisseg.indexOf("hr "));
                            vals[6] = thisseg.substring(thisseg.indexOf("hr ") + 3, thisseg.indexOf("min"));
                            vals[7] = thisseg.substring(thisseg.indexOf("min ") + 4, thisseg.indexOf("sec"));
                        }
                    }
                    for (int j = 2; j < vals.length; j++) {
                        buf.append(", ").append(vals[j]);
                    }
                }
                fw.println(buf.toString());
                nResCollected++;
            }
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nResCollected;
    }

    /**
     * Collect the Run end information from "eplusout.end" file in each directory
     */
    public int collectReportsSQL(String filename, String tablename, String dir) {
        return this.collectReportsSQL(filename, tablename, dir, false);
    }

    /**
     * Collect the Run end information from "eplusout.end" file in each directory
     */
    public int collectReportsSQL(String filename, String tablename, String dir, boolean remove) {
        int nResCollected = 0;
        try {
            PrintWriter fw = new PrintWriter(new FileWriter(OutputDir + filename));
            fw.println("CREATE TABLE `" + tablename + "` (");
            fw.println("`Id` int NOT NULL,");
            fw.println("`JobID` varchar(255) NOT NULL,");
            fw.println("`Message` varchar(255) default NULL,");
            fw.println("`Warnings` int default NULL,");
            fw.println("`Errors` int default NULL,");
            fw.println("`Hours` smallint default NULL,");
            fw.println("`Minutes` smallint default NULL,");
            fw.println("`Seconds` float default NULL,");
            fw.println("PRIMARY KEY  (`ID`),");
            fw.println("KEY `JobID` (`JobID`)");
            fw.println(") ENGINE=InnoDB DEFAULT CHARSET=latin1;");
            fw.println();
            fw.println("INSERT INTO `" + tablename + "` (`ID`, `JobID`, `Message`, `Warnings`, `Errors`, `Hours`, `Minutes`, `Seconds`) VALUES");
            Vector<EPlusTask> JobQueue = JobOwner.getAgent().getFinishedJobs();
            boolean emptyline = false;
            for (int i = 0; i < JobQueue.size(); i++) {
                if (i > 0 && !emptyline) fw.println(",");
                EPlusTask job = JobQueue.get(i);
                String info = EPlusTask.getResultInfo(dir, job, remove);
                if (info != null && !info.startsWith("!")) {
                    fw.print("(" + i + ", ");
                    fw.print("'" + job.getJobID() + "', ");
                    int marker = info.indexOf("--");
                    fw.print("'" + info.substring(0, marker) + "'");
                    info = info.substring(marker + 2).trim();
                    String[] segment = info.split(";");
                    String[] vals = new String[5];
                    for (int j = 0; j < segment.length; j++) {
                        String thisseg = segment[j].trim();
                        if (thisseg.endsWith("Warning")) {
                            vals[0] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.endsWith("Severe Errors")) {
                            vals[1] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.startsWith("Elapsed Time")) {
                            vals[2] = thisseg.substring(thisseg.indexOf("=") + 1, thisseg.indexOf("hr "));
                            vals[3] = thisseg.substring(thisseg.indexOf("hr ") + 3, thisseg.indexOf("min"));
                            vals[4] = thisseg.substring(thisseg.indexOf("min ") + 4, thisseg.indexOf("sec"));
                        }
                    }
                    for (int j = 0; j < vals.length; j++) fw.print(", " + vals[j]);
                    fw.print(")");
                    emptyline = false;
                } else {
                    emptyline = true;
                }
                nResCollected++;
            }
            fw.println(";");
            fw.println();
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nResCollected;
    }

    /**
     * Collect the Run end information from "eplusout.end" file in each directory
     */
    public int collectReportsSQL(String filename, String tablename, Vector<String> reports) {
        int nResCollected = 0;
        try {
            PrintWriter fw = new PrintWriter(new FileWriter(OutputDir + filename));
            fw.println("CREATE TABLE `" + tablename + "` (");
            fw.println("`Id` int NOT NULL,");
            fw.println("`JobID` varchar(255) NOT NULL,");
            fw.println("`Message` varchar(255) default NULL,");
            fw.println("`Warnings` int default NULL,");
            fw.println("`Errors` int default NULL,");
            fw.println("`Hours` smallint default NULL,");
            fw.println("`Minutes` smallint default NULL,");
            fw.println("`Seconds` float default NULL,");
            fw.println("PRIMARY KEY  (`ID`),");
            fw.println("KEY `JobID` (`JobID`)");
            fw.println(") ENGINE=InnoDB DEFAULT CHARSET=latin1;");
            fw.println();
            fw.println("INSERT INTO `" + tablename + "` (`ID`, `JobID`, `Message`, `Warnings`, `Errors`, `Hours`, `Minutes`, `Seconds`) VALUES");
            for (int i = 0; i < reports.size(); i++) {
                if (i > 0) fw.println(",");
                fw.print("(" + i + ", ");
                String info = reports.get(i);
                String job_id = info.substring(info.indexOf("[") + 1, info.indexOf("]"));
                fw.print("'" + job_id + "', ");
                info = info.substring(info.indexOf("] ") + 2);
                if (info.length() > 0 && !info.startsWith("!")) {
                    int marker = info.indexOf("--");
                    fw.print("'" + info.substring(0, marker) + "'");
                    info = info.substring(marker + 2).trim();
                    String[] segment = info.split(";");
                    String[] vals = new String[5];
                    for (int j = 0; j < segment.length; j++) {
                        String thisseg = segment[j].trim();
                        if (thisseg.endsWith("Warning")) {
                            vals[0] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.endsWith("Severe Errors")) {
                            vals[1] = thisseg.substring(0, thisseg.indexOf(" "));
                        } else if (thisseg.startsWith("Elapsed Time")) {
                            vals[2] = thisseg.substring(thisseg.indexOf("=") + 1, thisseg.indexOf("hr "));
                            vals[3] = thisseg.substring(thisseg.indexOf("hr ") + 3, thisseg.indexOf("min"));
                            vals[4] = thisseg.substring(thisseg.indexOf("min ") + 4, thisseg.indexOf("sec"));
                        }
                    }
                    for (int j = 0; j < vals.length; j++) fw.print(", " + vals[j]);
                    fw.print(")");
                } else {
                    fw.print("'" + info + "', , , , )");
                }
                nResCollected++;
            }
            fw.println(";");
            fw.println();
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nResCollected;
    }

    /**
     * Collect results in individual csv files and assemble a table of all results.
     * The name of individual job result files is expected to be [job_id].csv
     * @param filename The output file name
     * @param dir The directory where all results are located
     * @return Number of results collected
     */
    public int collectResultsCSV(String filename, String dir) {
        return collectResultsCSV(filename, dir, false);
    }

    /**
     * Collect results in individual csv files and assemble a table of all results.
     * The name of individual job result files is expected to be [job_id].csv
     * @param filename The output file name
     * @param dir The directory where all results are located
     * @param remove whether or not to remove individual csv files after collection
     * @return Number of results collected
     */
    public int collectResultsCSV(String filename, String dir, boolean remove) {
        int nResCollected = 0;
        try {
            Vector<Vector<String>> DataTable = new Vector<Vector<String>>();
            int NumRows = 0;
            HashMap<String, Integer> HeaderMap = new HashMap<String, Integer>();
            Vector<String> Headers = new Vector<String>();
            Vector<String> Job_Ids = new Vector<String>();
            Vector<EPlusTask> JobQueue = JobOwner.getAgent().getFinishedJobs();
            for (int i = 0; i < JobQueue.size(); i++) {
                EPlusTask job = JobQueue.get(i);
                String job_id = job.getJobID();
                try {
                    File csv = new File(dir + job_id + "/eplusout.csv");
                    if (csv.exists()) {
                        BufferedReader fr = new BufferedReader(new FileReader(csv));
                        String line = fr.readLine();
                        if (line != null) {
                            String[] headings = line.split(",");
                            int[] index = new int[headings.length];
                            for (int j = 0; j < headings.length; j++) {
                                if (!HeaderMap.containsKey(headings[j])) {
                                    index[j] = HeaderMap.size();
                                    HeaderMap.put(headings[j], index[j]);
                                    Headers.add(headings[j]);
                                    Vector<String> newcolumn = new Vector<String>();
                                    newcolumn.setSize(NumRows);
                                    Collections.fill(newcolumn, "-");
                                    DataTable.add(newcolumn);
                                } else {
                                    index[j] = HeaderMap.get(headings[j]).intValue();
                                }
                            }
                            line = fr.readLine();
                            while (line != null && line.trim().length() > 0) {
                                Job_Ids.add(job_id);
                                for (int j = 0; j < DataTable.size(); j++) {
                                    DataTable.get(j).add("-");
                                }
                                String[] data = line.split(",");
                                for (int j = 0; j < data.length; j++) {
                                    DataTable.get(index[j]).set(NumRows, data[j]);
                                }
                                NumRows++;
                                line = fr.readLine();
                            }
                        }
                        fr.close();
                        if (remove) csv.delete();
                        nResCollected++;
                        job.setResultAvailable(true);
                    } else {
                        job.setResultAvailable(false);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            PrintWriter fw = new PrintWriter(new FileWriter(OutputDir + filename));
            fw.print("Id,Job_Id");
            for (int j = 0; j < Headers.size(); j++) {
                fw.print(",");
                fw.print(Headers.get(j));
            }
            fw.println();
            for (int i = 0; i < NumRows; i++) {
                fw.print("" + i + "," + Job_Ids.get(i));
                for (int j = 0; j < DataTable.size(); j++) {
                    fw.print(",");
                    fw.print(DataTable.get(j).get(i));
                }
                fw.println();
            }
            fw.flush();
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nResCollected;
    }

    /**
     * Assemble a table of all results from tables collected by each task object.
     * The results from each task is collected and stored in a Vector of string Vectors.
     * @param filename The output file name
     * @param results A list of the contents of the result files
     * @return Number of results collected
     */
    public int collectResultsCSV(String filename, Vector<ArrayList<String>> results) {
        int nResCollected = 0;
        try {
            Vector<Vector<String>> DataTable = new Vector<Vector<String>>();
            int NumRows = 0;
            HashMap<String, Integer> HeaderMap = new HashMap<String, Integer>();
            Vector<String> Headers = new Vector<String>();
            Vector<String> Job_Ids = new Vector<String>();
            for (int i = 0; i < results.size(); i++) {
                ArrayList<String> job = results.get(i);
                if (job != null && job.size() > 0) {
                    String job_id = job.get(0);
                    if (job.size() > 2) {
                        String[] headings = job.get(1).split(",");
                        int[] index = new int[headings.length];
                        for (int j = 0; j < headings.length; j++) {
                            if (!HeaderMap.containsKey(headings[j])) {
                                index[j] = HeaderMap.size();
                                HeaderMap.put(headings[j], index[j]);
                                Headers.add(headings[j]);
                                Vector<String> newcolumn = new Vector<String>();
                                newcolumn.setSize(NumRows);
                                Collections.fill(newcolumn, "-");
                                DataTable.add(newcolumn);
                            } else {
                                index[j] = HeaderMap.get(headings[j]).intValue();
                            }
                        }
                        for (int k = 2; k < job.size(); k++) {
                            String line = job.get(k);
                            if (line != null && line.trim().length() > 0) {
                                Job_Ids.add(job_id);
                                for (int j = 0; j < DataTable.size(); j++) {
                                    DataTable.get(j).add("-");
                                }
                                String[] data = line.split(",");
                                for (int j = 0; j < data.length; j++) {
                                    DataTable.get(index[j]).set(NumRows, data[j]);
                                }
                                NumRows++;
                            }
                        }
                        nResCollected++;
                    }
                }
            }
            PrintWriter fw = new PrintWriter(new FileWriter(OutputDir + filename));
            fw.print("Id,Job_Id");
            for (int j = 0; j < Headers.size(); j++) {
                fw.print(",");
                fw.print(Headers.get(j));
            }
            fw.println();
            for (int i = 0; i < NumRows; i++) {
                fw.print("" + i + "," + Job_Ids.get(i));
                for (int j = 0; j < DataTable.size(); j++) {
                    fw.print(",");
                    fw.print(DataTable.get(j).get(i));
                }
                fw.println();
            }
            fw.flush();
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return nResCollected;
    }

    /**
     *
     * @return
     */
    public int collectResultsSQL(String sql_file, String tbl_name, String csv_file, String null_val_char) {
        try {
            PrintWriter fw = new PrintWriter(new FileWriter(OutputDir + sql_file, false));
            fw.println("CREATE TABLE `" + tbl_name + "` (");
            fw.println("`Id` int NOT NULL,");
            fw.println("`JobID` varchar(255) NOT NULL,");
            fw.println("`Period` varchar(255) default NULL,");
            for (int i = 0; i < 100; i++) fw.println("`Data" + i + "` double default NULL,");
            fw.println("PRIMARY KEY  (`ID`),");
            fw.println("KEY `JobID` (`JobID`)");
            fw.println(") ENGINE=InnoDB DEFAULT CHARSET=latin1;");
            fw.println();
            fw.println("LOAD DATA INFILE `" + csv_file + "` INTO TABLE " + tbl_name + " FIELDS TERMINATED BY `,` OPTIONALLY ENCLOSED BY `\"` LINES TERMINATED BY `\\n` IGNORE 1 LINES");
            fw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    /**
     * Collect results in the data table and save a sql file for importing
     * into MySQL. This function takes the first row of the data table to create a
     * database table, then insert all data rows into the table.
     * @param sql_file SQL file name to be generated
     * @param tbl_name The name of the table to be generated
     * @param results The data table
     * @param null_val_char The character (or string) used in the data table for missing values
     * @return Number of columns that the table contains
     */
    public int collectResultsSQL(String sql_file, String tbl_name, Vector<Vector<String>> results, String null_val_char) {
        return -1;
    }
}
