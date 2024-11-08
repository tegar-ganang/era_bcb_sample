package net.sf.mzmine.distributionframework;

import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.distributionframework.*;
import net.sf.mzmine.miscellaneous.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import java.rmi.Naming;
import java.util.Vector;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.channels.FileChannel;
import java.net.InetAddress;

/**
 *
 */
public class NodeServer {

    private boolean runningAsSingle;

    private String settingsFilename = "mzminenode.ini";

    private String myPort;

    private String myDataRoot;

    private InetAddress myIP;

    private String myHostname;

    private File myDataRootFile;

    private WorkerThread workerThread;

    private Hashtable<Integer, RawDataAtNode> rawDataTable;

    private ControllerForNodes myController;

    private Node servicesForController;

    private Job completionRateUpdatingJob;

    private int completionRateUpdatingCurrentStatus;

    private double completionRateUpdatingCurrentRate;

    private double completionRateUpdatingNextWaypoint;

    private double completionRateUpdatingWaypointDistance;

    /**
	 * Constructor when running on a cluster
	 */
    public NodeServer() {
        runningAsSingle = false;
        Logger.put("NODE: Starting up...");
        try {
            myIP = InetAddress.getLocalHost();
            myHostname = myIP.getHostName();
        } catch (Exception e) {
            Logger.putFatal("NODE: FATAL ERROR - Failed to determine IP address and hostname of the computer.");
            Logger.putFatal(e.toString());
            Logger.putFatal("NODE: Unable to continue.");
            System.exit(0);
        }
        if (loadSettings(settingsFilename) == false) {
            Logger.putFatal("NODE: FATAL ERROR - Failed to read necessary settings from " + settingsFilename);
            Logger.putFatal("NODE: Unable to continue.");
            System.exit(0);
        }
        myDataRootFile = new File(myDataRoot);
        workerThread = new WorkerThread(this);
        workerThread.start();
        rawDataTable = new Hashtable<Integer, RawDataAtNode>();
        Logger.put("NODE: Starting service... ");
        try {
            servicesForController = new NodeImpl(this);
            Naming.rebind("rmi://" + myHostname + ":" + myPort + "/NodeService", servicesForController);
            Logger.put("NODE: Service started ok.");
        } catch (Exception e) {
            Logger.putFatal("NODE: FATAL ERROR: Service failed to start.");
            Logger.putFatal(e.toString());
            Logger.putFatal("NODE: Unable to continue.");
            System.exit(0);
        }
        Logger.put("NODE: Started ok.");
    }

    /**
	 * Constructor when running on a single computer
	 */
    public NodeServer(boolean flagForSingleMode) {
        runningAsSingle = true;
        Logger.put("NODE: Starting up...");
        if (loadSettings(settingsFilename) == false) {
            Logger.putFatal("NODE: FATAL ERROR - Failed to read necessary settings from " + settingsFilename);
            Logger.putFatal("NODE: Unable to continue.");
            System.exit(0);
        }
        myDataRootFile = new File(myDataRoot);
        try {
            servicesForController = new NodeImpl(this);
        } catch (Exception e) {
        }
        workerThread = new WorkerThread(this);
        workerThread.start();
        rawDataTable = new Hashtable<Integer, RawDataAtNode>();
        Logger.put("NODE: Started ok.");
    }

    /**
	 * When running without Java RMI, this method is used to get servicesForController-object
	 */
    public Node getServicesForController() {
        return servicesForController;
    }

    public static void main(String argz[]) {
        if (argz.length > 0) {
            if ((argz[0].compareToIgnoreCase(new String("quiet"))) == 0) {
                Logger.disableOutput();
            }
        } else {
            Logger.setOutputOnScreen();
        }
        new NodeServer();
    }

    public synchronized void setController(ControllerForNodes cm) {
        if (cm != null) {
            Logger.put("NODE: Got my controller");
        } else {
            Logger.put("NODE: Controller was set to null");
        }
        myController = cm;
    }

    /**
	 * Add job: open raw data file
	 */
    public synchronized void addJob(int taskID, int jobID, File originalRawDataFile) {
        Job j = new Job(taskID, jobID, Job.TYPE_OPENRAWDATA);
        j.addJobParameter(Job.PARAM_ORIGINALFILE, originalRawDataFile);
        updateJobStatusAndRestartWorker(j);
    }

    /**
	 * Add job: close raw data file
	 */
    public synchronized void addJob(int taskID, int jobID, boolean saveChanges) {
        Job j = new Job(taskID, jobID, Job.TYPE_CLOSERAWDATA);
        j.addJobParameter(Job.PARAM_SAVECHANGES, new Boolean(saveChanges));
        updateJobStatusAndRestartWorker(j);
    }

    /**
	 * Add job: refresh raw data visualizers
	 */
    public synchronized void addJob(int taskID, int jobID, RawDataVisualizerRefreshRequest refreshRequest) {
        Job j = new Job(taskID, jobID, Job.TYPE_REFRESHVISUALIZERS);
        j.addJobParameter(Job.PARAM_REFRESHREQUEST, refreshRequest);
        updateJobStatusAndRestartWorker(j);
    }

    /**
	 * Add job: filter raw data file
	 */
    public synchronized void addJob(int taskID, int jobID, FilterParameters filterParameters) {
        Job j = new Job(taskID, jobID, Job.TYPE_FILTERRAWDATA);
        j.addJobParameter(Job.PARAM_FILTERPARAMETERS, filterParameters);
        updateJobStatusAndRestartWorker(j);
    }

    /**
	 * Add job: find peaks in a raw data file
	 */
    public synchronized void addJob(int taskID, int jobID, PeakPickerParameters parameters) {
        Job j = new Job(taskID, jobID, Job.TYPE_FINDPEAKS);
        j.addJobParameter(Job.PARAM_PEAKPICKERPARAMETERS, parameters);
        updateJobStatusAndRestartWorker(j);
    }

    public synchronized void addJob(int taskID, int jobID, Hashtable<Integer, PeakList> peakLists, PeakListAlignerParameters parameters) {
        Job j = new Job(taskID, jobID, Job.TYPE_ALIGNMENT);
        j.addJobParameter(Job.PARAM_ALIGNERPARAMETERS, parameters);
        j.addJobParameter(Job.PARAM_PEAKLISTS, peakLists);
        updateJobStatusAndRestartWorker(j);
    }

    public synchronized void addJob(int taskID, int jobID, Hashtable<Integer, double[]> gapsToFill, GapFillerParameters parameters) {
        Job j = new Job(taskID, jobID, Job.TYPE_GAPFILLING);
        j.addJobParameter(Job.PARAM_GAPFILLERPARAMETERS, parameters);
        j.addJobParameter(Job.PARAM_GAPLIST, gapsToFill);
        updateJobStatusAndRestartWorker(j);
    }

    public synchronized void addJob(int taskID, int jobID) {
        Job j = new Job(taskID, jobID, Job.TYPE_CALCTOTALRAWSIGNAL);
        updateJobStatusAndRestartWorker(j);
    }

    public synchronized void addJob(int taskID, int jobID, PeakList peakList, PeakListProcessorParameters parameters) {
        Job j = new Job(taskID, jobID, Job.TYPE_PROCESSPEAKLIST);
        j.addJobParameter(Job.PARAM_PEAKLIST, peakList);
        j.addJobParameter(Job.PARAM_PEAKLISTPROCESSORPARAMETERS, parameters);
        updateJobStatusAndRestartWorker(j);
    }

    /**
	 * This method
	 * - updates job's status on controller
	 * - adds job to queue
	 * - restarts worker thread if it is not already active
	 * These are implemented as a separate method, because every addJob method needs to do same things after creating a job.
	 */
    private void updateJobStatusAndRestartWorker(Job j) {
        int taskID = j.getTaskID();
        int jobID = j.getJobID();
        setJobCompletionRate(j, Task.JOBSTATUS_INQUEUEATNODE, 0.0F);
        workerThread.addJob(j);
    }

    /**
	 * Loads settings from .ini file
	 */
    private boolean loadSettings(String filename) {
        int varsRead = -2;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String nextLine = br.readLine();
            String varStr = null;
            String valStr = null;
            while (nextLine != null) {
                StringTokenizer st = new StringTokenizer(nextLine, " = ");
                if (st.hasMoreTokens()) {
                    varStr = st.nextToken();
                    if (st.hasMoreTokens()) {
                        valStr = st.nextToken();
                    } else {
                        valStr = new String("");
                    }
                    if (varStr.equals("NodePort")) {
                        myPort = valStr;
                        varsRead++;
                    }
                    if (varStr.equals("NodeDataRoot")) {
                        myDataRoot = valStr;
                        varsRead++;
                    }
                }
                nextLine = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            Logger.put("NODE: ERROR -  Could not open/read settings from file " + filename);
            Logger.put(e.toString());
            return false;
        }
        if (varsRead < 0) {
            Logger.put("NODE: ERROR - Could not find all necessary variables in file " + filename);
            return false;
        } else {
            return true;
        }
    }

    private void setJobCompletionRate(Job j, int status, double rate) {
        try {
            myController.updateJobCompletionRate(j.getTaskID(), j.getJobID(), status, rate);
        } catch (Exception e) {
            Logger.put("NODE: ERROR - Failed to update job completion rate on controller.");
            Logger.put(e.toString());
        }
    }

    private void initializeJobCompletionRate(Job j, int status, double rate, double waypointRate) {
        completionRateUpdatingJob = j;
        completionRateUpdatingCurrentStatus = status;
        completionRateUpdatingCurrentRate = rate;
        completionRateUpdatingNextWaypoint = rate + waypointRate;
        completionRateUpdatingWaypointDistance = waypointRate;
        try {
            myController.updateJobCompletionRate(completionRateUpdatingJob.getTaskID(), completionRateUpdatingJob.getJobID(), completionRateUpdatingCurrentStatus, completionRateUpdatingCurrentRate);
        } catch (Exception e) {
            Logger.put("NODE: ERROR - Failed to update job completion rate on controller.");
            Logger.put(e.toString());
        }
    }

    public void updateJobCompletionRate(double rate) {
        completionRateUpdatingCurrentRate = rate;
        if (completionRateUpdatingCurrentRate >= completionRateUpdatingNextWaypoint) {
            try {
                myController.updateJobCompletionRate(completionRateUpdatingJob.getTaskID(), completionRateUpdatingJob.getJobID(), completionRateUpdatingCurrentStatus, completionRateUpdatingCurrentRate);
            } catch (Exception e) {
                Logger.put("NODE: ERROR - Failed to update job completion rate on controller.");
                Logger.put(e.toString());
            }
            completionRateUpdatingNextWaypoint += completionRateUpdatingWaypointDistance;
        }
    }

    /**
	 * This class represents a worker thread that does all real processing and communicates results back to controller.
	 * Thread is started by the Node service when it receives a new task and adds it to queue.
	 * When taskQueue is empty thread dies.
	 */
    private class WorkerThread extends Thread {

        private Vector<Job> jobQueue;

        private NodeServer nodeServer;

        public WorkerThread(NodeServer _nodeServer) {
            nodeServer = _nodeServer;
            jobQueue = new Vector<Job>();
        }

        /**
		 * Adds a new job to queue
		 */
        public void addJob(Job j) {
            synchronized (this) {
                jobQueue.add(j);
                try {
                    notifyAll();
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: Worker failed to notifyAll()");
                    Logger.putFatal(e.toString());
                }
            }
        }

        /**
		 * Run method of the thread
		 */
        public void run() {
            Logger.put("NODE WORKER THREAD: Worker thread started!");
            while (1 == 1) {
                synchronized (this) {
                    if (jobQueue.size() == 0) {
                        Logger.put("NODE WORKER THREAD: Nothing to do, going to wait().");
                        try {
                            wait();
                        } catch (Exception e) {
                        }
                    }
                }
                Job j = jobQueue.remove(0);
                Logger.put("NODE WORKER THREAD: Picked up a job from queue.");
                if (j.getJobType() == Job.TYPE_OPENRAWDATA) {
                    openRawDataFile(j);
                }
                if (j.getJobType() == Job.TYPE_CLOSERAWDATA) {
                    closeRawDataFile(j);
                }
                if (j.getJobType() == Job.TYPE_REFRESHVISUALIZERS) {
                    refreshVisualizers(j);
                }
                if (j.getJobType() == Job.TYPE_FILTERRAWDATA) {
                    filterRawDataFile(j);
                }
                if (j.getJobType() == Job.TYPE_FINDPEAKS) {
                    findPeaks(j);
                }
                if (j.getJobType() == Job.TYPE_ALIGNMENT) {
                    doAlignment(j);
                }
                if (j.getJobType() == Job.TYPE_GAPFILLING) {
                    fillGaps(j);
                }
                if (j.getJobType() == Job.TYPE_CALCTOTALRAWSIGNAL) {
                    calcTotalRawSignal(j);
                }
                if (j.getJobType() == Job.TYPE_PROCESSPEAKLIST) {
                    processPeakList(j);
                }
            }
        }

        /**
		 * Processor for open raw data file job
		 */
        private void openRawDataFile(Job j) {
            Logger.put("NODE WORKER THREAD: Processing open raw data file job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.5F);
            File originalRawDataFile = (File) j.getJobParameter(Job.PARAM_ORIGINALFILE);
            Logger.put("NODE WORKER THREAD: Opening raw data file " + originalRawDataFile.getName());
            File fullPathToOriginalRawDataFile;
            if (myDataRootFile.equals(new File(new String("")))) {
                fullPathToOriginalRawDataFile = new File(originalRawDataFile.getPath());
            } else {
                fullPathToOriginalRawDataFile = new File(myDataRootFile, originalRawDataFile.getPath());
            }
            Logger.put("NODE WORKER THREAD: Full path to raw data file is " + fullPathToOriginalRawDataFile.getAbsolutePath());
            RawDataAtNode rawData = new RawDataAtNode(j.getJobID(), fullPathToOriginalRawDataFile);
            File workingCopy = createWorkingCopy(fullPathToOriginalRawDataFile);
            if (workingCopy == null) {
                Logger.put("NODE WORKER THREAD: ERROR - Failed to create working copy.");
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), originalRawDataFile.getName());
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: ERROR - Failed to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
                return;
            }
            rawData.setWorkingCopy(workingCopy);
            updateJobCompletionRate(0.5F);
            int retval = rawData.preLoad();
            if (retval == -1) {
                Logger.put("NODE WORKER THREAD: ERROR - Failed to preload data file.");
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), new String("Unable to preload data."));
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: ERROR - Failed to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
                return;
            }
            rawDataTable.put(new Integer(rawData.getRawDataID()), rawData);
            RawDataOnTransit rawDataOT = new RawDataOnTransit(rawData);
            try {
                myController.setJobResult(j.getTaskID(), j.getJobID(), rawDataOT);
            } catch (Exception e) {
                Logger.putFatal("NODE WORKER THREAD: ERROR - Failed to send results to controller.");
                Logger.putFatal(e.toString());
            }
        }

        /**
		 * Processor for close raw data file job
		 */
        private void closeRawDataFile(Job j) {
            Logger.put("NODE WORKER THREAD: Processing close raw data files job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.5F);
            Integer rawDataIDInteger = new Integer(j.getJobID());
            RawDataAtNode rawData = rawDataTable.get(rawDataIDInteger);
            boolean saveChanges = ((Boolean) (j.getJobParameter(Job.PARAM_SAVECHANGES))).booleanValue();
            if (rawData.isModified() && saveChanges) {
            }
            rawData.getWorkingCopy().delete();
            rawDataTable.remove(rawDataIDInteger);
            try {
                myController.setJobResult(j.getTaskID(), j.getJobID(), rawDataIDInteger);
            } catch (Exception e) {
                Logger.putFatal("NODE WORKER THREAD: ERROR - Failed to send results to controller.");
                Logger.putFatal(e.toString());
            }
        }

        /**
		 * Processor for visualizer refresh job
		 */
        private void refreshVisualizers(Job j) {
            Logger.put("NODE WORKER THREAD: Processing refresh visualizers job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.1F);
            Integer rawDataIDInteger = new Integer(j.getJobID());
            RawDataAtNode rawData = rawDataTable.get(rawDataIDInteger);
            RawDataVisualizerRefreshRequest refreshRequest = (RawDataVisualizerRefreshRequest) (j.getJobParameter(Job.PARAM_REFRESHREQUEST));
            int firstScan = Integer.MAX_VALUE;
            int lastScan = Integer.MIN_VALUE;
            if (refreshRequest.ticNeedsRawData) {
                if (refreshRequest.ticStartScan <= firstScan) {
                    firstScan = refreshRequest.ticStartScan;
                }
                if (refreshRequest.ticStopScan >= lastScan) {
                    lastScan = refreshRequest.ticStopScan;
                }
            }
            if (refreshRequest.spectrumNeedsRawData) {
                if (refreshRequest.spectrumStartScan <= firstScan) {
                    firstScan = refreshRequest.spectrumStartScan;
                }
                if (refreshRequest.spectrumStopScan >= lastScan) {
                    lastScan = refreshRequest.spectrumStopScan;
                }
            }
            if (refreshRequest.twodNeedsRawData) {
                if (refreshRequest.twodStartScan <= firstScan) {
                    firstScan = refreshRequest.twodStartScan;
                }
                if (refreshRequest.twodStopScan >= lastScan) {
                    lastScan = refreshRequest.twodStopScan;
                }
            }
            rawData.initializeScanBrowser(firstScan, lastScan);
            RawDataVisualizerTICCalc ticCalculator = new RawDataVisualizerTICCalc();
            ticCalculator.refreshInitialize(refreshRequest);
            RawDataVisualizerSpectrumCalc spectrumCalculator = new RawDataVisualizerSpectrumCalc();
            spectrumCalculator.refreshInitialize(refreshRequest);
            RawDataVisualizerTwoDCalc twodCalculator = new RawDataVisualizerTwoDCalc();
            twodCalculator.refreshInitialize(refreshRequest);
            if (firstScan != -1) {
                for (int scanInd = firstScan; scanInd <= lastScan; scanInd++) {
                    Scan s = rawData.getNextScan();
                    if ((refreshRequest.ticNeedsRawData) && (refreshRequest.ticStartScan <= scanInd) && (refreshRequest.ticStopScan >= scanInd)) {
                        ticCalculator.refreshHaveOneScan(s);
                    }
                    if ((refreshRequest.spectrumNeedsRawData) && (refreshRequest.spectrumStartScan <= scanInd) && (refreshRequest.spectrumStopScan >= scanInd)) {
                        spectrumCalculator.refreshHaveOneScan(s);
                    }
                    if ((refreshRequest.twodNeedsRawData) && (refreshRequest.twodStartScan <= scanInd) && (refreshRequest.twodStopScan >= scanInd)) {
                        twodCalculator.refreshHaveOneScan(s);
                    }
                    updateJobCompletionRate((double) (scanInd - firstScan + 1) / (double) (lastScan - firstScan + 1));
                }
            }
            ticCalculator.refreshFinalize();
            spectrumCalculator.refreshFinalize();
            twodCalculator.refreshFinalize();
            RawDataVisualizerRefreshResult refreshResult = new RawDataVisualizerRefreshResult();
            refreshResult.rawDataID = j.getJobID();
            refreshResult.changeType = refreshRequest.changeType;
            refreshResult.spectrumCombinationStartScan = refreshRequest.spectrumStartScan;
            refreshResult.spectrumCombinationStopScan = refreshRequest.spectrumStopScan;
            refreshResult.twodMatrixWidth = refreshRequest.twodXResolution;
            refreshResult.twodMatrixHeight = refreshRequest.twodYResolution;
            refreshResult.ticIntensities = ticCalculator.getIntensities();
            refreshResult.ticMaxIntensity = ticCalculator.getMaxIntensity();
            refreshResult.ticScanNumbers = ticCalculator.getScanNumbers();
            refreshResult.spectrumIntensities = spectrumCalculator.getIntensities();
            refreshResult.spectrumMaxIntensity = spectrumCalculator.getMaxIntensity();
            refreshResult.spectrumMZValues = spectrumCalculator.getMZValues();
            refreshResult.spectrumMinMZValue = spectrumCalculator.getMinMZValue();
            refreshResult.spectrumMaxMZValue = spectrumCalculator.getMaxMZValue();
            refreshResult.twodMatrix = twodCalculator.getMatrix();
            refreshResult.twodMaxIntensity = twodCalculator.getMaxIntensity();
            refreshResult.twodMinIntensity = twodCalculator.getMinIntensity();
            refreshResult.twodDataMaxIntensity = rawData.getDataMaxIntensity();
            try {
                myController.setJobResult(j.getTaskID(), j.getJobID(), refreshResult);
            } catch (Exception e) {
                Logger.putFatal("NODE WORKER THREAD: ERROR - Failed to send resuls to controller.");
                Logger.putFatal(e.toString());
            }
        }

        /**
		 * Processor for filter raw data file job
		 */
        private void filterRawDataFile(Job j) {
            Logger.put("NODE WORKER THREAD: Processing filter job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.1F);
            Integer rawDataIDInteger = new Integer(j.getJobID());
            RawDataAtNode rawData = rawDataTable.get(rawDataIDInteger);
            FilterParameters filterParameters = (FilterParameters) (j.getJobParameter(Job.PARAM_FILTERPARAMETERS));
            Class filterClass = filterParameters.getFilterClass();
            int retValue = 0;
            if (filterClass == MeanFilter.class) {
                MeanFilter mf = new MeanFilter();
                retValue = mf.doFiltering(nodeServer, rawData, filterParameters);
            }
            if (filterClass == SavitzkyGolayFilter.class) {
                SavitzkyGolayFilter sf = new SavitzkyGolayFilter();
                retValue = sf.doFiltering(nodeServer, rawData, filterParameters);
            }
            if (filterClass == ChromatographicMedianFilter.class) {
                ChromatographicMedianFilter cmf = new ChromatographicMedianFilter();
                retValue = cmf.doFiltering(nodeServer, rawData, filterParameters);
            }
            if (filterClass == CropFilter.class) {
                CropFilter cf = new CropFilter();
                retValue = cf.doFiltering(nodeServer, rawData, filterParameters);
            }
            if (filterClass == ZoomScanFilter.class) {
                ZoomScanFilter zsf = new ZoomScanFilter();
                retValue = zsf.doFiltering(nodeServer, rawData, filterParameters);
            }
            if (retValue == 1) {
                RawDataOnTransit rawDataOT = new RawDataOnTransit(rawData);
                try {
                    myController.setJobResult(j.getTaskID(), j.getJobID(), rawDataOT);
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send results to controller.");
                    Logger.putFatal(e.toString());
                }
            } else {
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), new String("Unable to filter data."));
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
            }
        }

        /**
		 * Processor for find peaks job
		 */
        private void findPeaks(Job j) {
            Logger.put("NODE WORKER THREAD: Processing find peaks job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.1F);
            Integer rawDataIDInteger = new Integer(j.getJobID());
            RawDataAtNode rawData = rawDataTable.get(rawDataIDInteger);
            PeakPickerParameters parameters = (PeakPickerParameters) (j.getJobParameter(Job.PARAM_PEAKPICKERPARAMETERS));
            Class peakPickerClass = parameters.getPeakPickerClass();
            PeakList peakList = null;
            if (peakPickerClass == LocalPicker.class) {
                LocalPicker lp = new LocalPicker();
                peakList = lp.findPeaks(nodeServer, rawData, parameters);
            }
            if (peakPickerClass == RecursiveThresholdPicker.class) {
                Logger.put("Calling findPeaks");
                RecursiveThresholdPicker rp = new RecursiveThresholdPicker();
                peakList = rp.findPeaks(nodeServer, rawData, parameters);
            }
            if (peakPickerClass == CentroidPicker.class) {
                CentroidPicker cp = new CentroidPicker();
                peakList = cp.findPeaks(nodeServer, rawData, parameters);
            }
            if (peakList != null) {
                try {
                    myController.setJobResult(j.getTaskID(), j.getJobID(), peakList);
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send results to controller.");
                    Logger.putFatal(e.toString());
                }
            } else {
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), new String("Unable do peak picking."));
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: ERROR - Failed to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
            }
        }

        private void doAlignment(Job j) {
            Logger.put("NODE WORKER THREAD: Processing alignment job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.1F);
            PeakListAlignerParameters parameters = (PeakListAlignerParameters) (j.getJobParameter(Job.PARAM_ALIGNERPARAMETERS));
            Hashtable<Integer, PeakList> peakLists = (Hashtable<Integer, PeakList>) (j.getJobParameter(Job.PARAM_PEAKLISTS));
            Class peakListAlignerClass = parameters.getPeakListAlignerClass();
            AlignmentResult alignmentResult = null;
            if (peakListAlignerClass == JoinAligner.class) {
                JoinAligner joinAligner = new JoinAligner();
                alignmentResult = joinAligner.doAlignment(nodeServer, peakLists, parameters);
            }
            if (peakListAlignerClass == FastAligner.class) {
                FastAligner fastAligner = new FastAligner();
                alignmentResult = fastAligner.doAlignment(nodeServer, peakLists, parameters);
            }
            if (alignmentResult != null) {
                try {
                    myController.setJobResult(j.getTaskID(), j.getJobID(), alignmentResult);
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send results to controller.");
                    Logger.putFatal(e.toString());
                }
            } else {
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), new String("Unable align peak lists."));
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
            }
        }

        private void fillGaps(Job j) {
            Logger.put("NODE WORKER THREAD: Processing fill-in gaps job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.1F);
            int rawDataID = j.getJobID();
            RawDataAtNode rawData = rawDataTable.get(new Integer(rawDataID));
            GapFillerParameters parameters = (GapFillerParameters) (j.getJobParameter(Job.PARAM_GAPFILLERPARAMETERS));
            Hashtable<Integer, double[]> gapsToFill = (Hashtable<Integer, double[]>) (j.getJobParameter(Job.PARAM_GAPLIST));
            Class gapFillerClass = parameters.getGapFillerClass();
            Hashtable<Integer, double[]> fillIns = null;
            if (gapFillerClass == SimpleGapFiller.class) {
                SimpleGapFiller simpleGapFiller = new SimpleGapFiller();
                fillIns = simpleGapFiller.fillGaps(nodeServer, gapsToFill, rawData, parameters);
            }
            if (fillIns != null) {
                try {
                    myController.setJobResult(j.getTaskID(), j.getJobID(), fillIns);
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send results to controller.");
                    Logger.putFatal(e.toString());
                }
            } else {
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), new String("Unable to fill-in gaps in alignment result."));
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
            }
        }

        /**
		 * Processor for filter raw data file job
		 */
        private void calcTotalRawSignal(Job j) {
            Logger.put("NODE WORKER THREAD: Processing calc total raw signal job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.1F);
            Integer rawDataIDInteger = new Integer(j.getJobID());
            RawDataAtNode rawData = rawDataTable.get(rawDataIDInteger);
            Double totalRawSignal = rawData.calculateTotalRawSignal();
            if (totalRawSignal > 0) {
                try {
                    myController.setJobResult(j.getTaskID(), j.getJobID(), totalRawSignal);
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send results to controller.");
                    Logger.putFatal(e.toString());
                }
            } else {
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), new String("Unable to calculate total raw signal."));
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
            }
        }

        /**
		 * Processor for process peak list job
		 */
        private void processPeakList(Job j) {
            Logger.put("NODE WORKER THREAD: Processing peak list processing job.");
            initializeJobCompletionRate(j, Task.JOBSTATUS_UNDERPROCESSING, 0.0F, 0.1F);
            Integer rawDataIDInteger = new Integer(j.getJobID());
            RawDataAtNode rawData = rawDataTable.get(rawDataIDInteger);
            PeakList peakList = (PeakList) (j.getJobParameter(Job.PARAM_PEAKLIST));
            PeakListProcessorParameters parameters = (PeakListProcessorParameters) (j.getJobParameter(Job.PARAM_PEAKLISTPROCESSORPARAMETERS));
            Class peakListProcessorClass = parameters.getPeakListProcessorClass();
            PeakList newPeakList = null;
            if (peakListProcessorClass == SimpleDeisotoper.class) {
                SimpleDeisotoper sd = new SimpleDeisotoper();
                newPeakList = sd.processPeakList(nodeServer, rawData, peakList, parameters);
            }
            if (peakListProcessorClass == IncompleteIsotopePatternFilter.class) {
                IncompleteIsotopePatternFilter iif = new IncompleteIsotopePatternFilter();
                newPeakList = iif.processPeakList(nodeServer, rawData, peakList, parameters);
            }
            if (peakListProcessorClass == CombinatorialDeisotoper.class) {
                CombinatorialDeisotoper cd = new CombinatorialDeisotoper();
                newPeakList = cd.processPeakList(nodeServer, rawData, peakList, parameters);
            }
            if (newPeakList != null) {
                try {
                    myController.setJobResult(j.getTaskID(), j.getJobID(), newPeakList);
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: FAILED to send results to controller.");
                    Logger.putFatal(e.toString());
                }
            } else {
                try {
                    myController.setJobErrorMessage(j.getTaskID(), j.getJobID(), new String("Unable do peak list processing."));
                } catch (Exception e) {
                    Logger.putFatal("NODE WORKER THREAD: ERROR - Failed to send error message to controller.");
                    Logger.putFatal(e.toString());
                }
            }
        }

        /**
		 * Creates a working copy of given data file
		 * Returns the name of newly created copy
		 */
        private File createWorkingCopy(File _originalRawDataFile) {
            File fOriginalRawDataFile = _originalRawDataFile;
            File fWorkingCopy;
            System.gc();
            try {
                fWorkingCopy = File.createTempFile("MZmine", null);
                FileChannel sourceChannel = new FileInputStream(fOriginalRawDataFile).getChannel();
                FileChannel destinationChannel = new FileOutputStream(fWorkingCopy).getChannel();
                long sourceChannelPos = 0;
                long sourceChannelSize = sourceChannel.size();
                long maxReadSize = 5 * 1024 * 1024;
                long targetChannelPos = 0;
                while (sourceChannelPos < sourceChannelSize) {
                    long transferAmount = maxReadSize;
                    if (transferAmount > (sourceChannelSize - sourceChannelPos)) {
                        transferAmount = sourceChannelSize - sourceChannelPos;
                    }
                    sourceChannel.transferTo(sourceChannelPos, transferAmount, destinationChannel);
                    sourceChannelPos += transferAmount;
                }
                sourceChannel.close();
                destinationChannel.close();
            } catch (Exception ekse) {
                Logger.put("NODE WORKER THREAD: ERROR - Failed to create working copy!");
                Logger.put(ekse.toString());
                return null;
            }
            return fWorkingCopy;
        }
    }
}
