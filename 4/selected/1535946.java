package jeplus.postproc;

import java.util.ArrayList;
import java.util.HashMap;
import jeplus.EPlusBatch;

/**
 *
 * @author zyyz
 */
public class ResultCollector {

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
        public int readReport(EPlusBatch manager, String dir, ArrayList<String> header, ArrayList<ArrayList<String>> table);

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
        public int readReport(String dir, String job_id, ArrayList<String> header, ArrayList<ArrayList<String>> table);
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
        public int readResult(EPlusBatch manager, String dir, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);

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
        public int readResult(String dir, String job_id, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);
    }

    public interface ResultReaderTR {

        /**
         * Read result from the named files within the given dir. New data will 
         * be added to Header and Data table. This function returns the number
         * of files read.
         * @param manager The simulation manager holds information on the jobs
         * @param dir Folder's path in which reports are stored. This is normally the working directory of the batch.
         * @param header Table header of the report data
         * @param table Table content of the report data
         * @param TRNSYSResultFile Name of the one of the result file(s)
         * @return Number of files read
         */
        public int readResultTR(EPlusBatch manager, String dir, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table, String TRNSYSResultFile);

        /**
         * This method read result from the named file in the given dir. New data will 
         * be added to Header and Data table with the assigned job_id. The header map is 
         * maintained to preserve consistency of the columns of the table. This function returns the number
         * of lines read.
         * @param dir Folder path in which reports are stored. This is normally the working directory of the batch.
         * @param job_id ID string of the job
         * @param header Table header of the report data
         * @param table Table content of the report data
         * @param TRNSYSResultFile Name of one of the result file(s)
         * @return Number of files read
         */
        public int readResultTR(String dir, String job_id, HashMap<String, Integer> header, ArrayList<ArrayList<String>> table, String TRNSYSResultFile);
    }

    public interface ResultWriter {

        public void writeResult(HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);
    }

    public interface ReportWriter {

        public void writeReport(ArrayList<String> header, ArrayList<ArrayList<String>> table);
    }

    public interface IndexWriter {

        public void writeIndex(String file, EPlusBatch manager);
    }

    public interface PostProcessor {

        public void postProcess(HashMap<String, Integer> header, ArrayList<ArrayList<String>> table);
    }

    /**
     * Create collector with assigned job owner
     * @param batch Job owner
     */
    public ResultCollector(EPlusBatch batch) {
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

    public void collectReports(ResultCollector.ReportReader reader, ResultCollector.ReportWriter writer, boolean onthefly) {
        if (onthefly) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            ReportHeader = new ArrayList<String>();
            ReportTable = new ArrayList<ArrayList<String>>();
            reader.readReport(JobOwner, OutputDir, ReportHeader, ReportTable);
            writer.writeReport(ReportHeader, ReportTable);
        }
    }

    public void collectResutls(ResultCollector.ResultReader reader, ResultCollector.PostProcessor processor, ResultCollector.ResultWriter writer, boolean onthefly) {
        if (onthefly) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            ResultHeader = new HashMap<String, Integer>();
            ResultTable = new ArrayList<ArrayList<String>>();
            reader.readResult(JobOwner, OutputDir, ResultHeader, ResultTable);
            if (processor != null) processor.postProcess(ResultHeader, ReportTable);
            writer.writeResult(ResultHeader, ResultTable);
        }
    }

    public void collectResutlsTR(ResultCollector.ResultReaderTR reader, ResultCollector.PostProcessor processor, ResultCollector.ResultWriter writer, String TRNSYSResultFile, boolean onthefly) {
        if (onthefly) {
            throw new UnsupportedOperationException("Not supported yet.");
        } else {
            ResultHeader = new HashMap<String, Integer>();
            ResultTable = new ArrayList<ArrayList<String>>();
            reader.readResultTR(JobOwner, OutputDir, ResultHeader, ResultTable, TRNSYSResultFile);
            if (processor != null) processor.postProcess(ResultHeader, ReportTable);
            writer.writeResult(ResultHeader, ResultTable);
        }
    }

    public void collectIndexes(jeplus.EPlusDefaultResultCollector.IndexWriter writer) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
