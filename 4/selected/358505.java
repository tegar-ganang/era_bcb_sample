package ms.jasim.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import ms.jacrim.Constants;
import ms.jacrim.framework.Configuration;
import ms.jacrim.framework.MessageOutput;
import ms.jacrim.pddl.PddlProblem;
import ms.jacrim.pddl.PddlSolution;
import ms.spm.IAppContext;
import ms.utils.NamedList;

public class SolutionGenerator {

    public enum Status {

        READY, PLANNING
    }

    ;

    public final IAppContext Context;

    protected final Configuration Config;

    protected final MessageOutput StdOut;

    protected Status status;

    private String inputPddlFile;

    private Thread plannerThread;

    private String modelFile;

    private int numOfSolution;

    private NamedList<PddlSolution> solutions = new NamedList<PddlSolution>();

    private Runnable completeListener;

    private Process lpgProcess;

    public SolutionGenerator(IAppContext context) throws Exception {
        Context = context;
        Config = context.getService(Configuration.class);
        if (Config == null) throw new Exception("Unable to query Configuration service in the context.");
        StdOut = context.getService(MessageOutput.class);
        if (StdOut == null) throw new Exception("Unable to query MessageOutput service in the context.");
    }

    public Status Status() {
        return status;
    }

    public synchronized void Status(Status value) {
        status = value;
    }

    public void setCompleteListener(Runnable listener) {
        completeListener = listener;
    }

    public void startPlanner(PddlModel pddlModel, int numOfSolution, boolean workInBackground) throws Exception {
        this.numOfSolution = numOfSolution;
        modelFile = new File(pddlModel.getFilename()).getName();
        String tempFolder = Config.get("output_folder");
        int i = modelFile.lastIndexOf(".");
        inputPddlFile = tempFolder + "\\" + modelFile.substring(0, i) + "-auto.pddl";
        PddlProblem p = pddlModel.generatePddl();
        p.writeToFile(inputPddlFile);
        Status(Status.PLANNING);
        if (workInBackground) {
            plannerThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    runPlanner();
                }
            });
            plannerThread.start();
        } else runPlanner();
    }

    private void runPlanner() {
        try {
            Status(Status.PLANNING);
            String command = Config.get("planner");
            String arg = String.format("%s %s %s %d ", command, modelFile, inputPddlFile, numOfSolution);
            StdOut.write(null, arg + "\r\n");
            ProcessBuilder b = new ProcessBuilder(Constants.CMD_PATH, "/C", arg);
            b.redirectErrorStream(true);
            lpgProcess = b.start();
            InputStream is = lpgProcess.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) StdOut.write(null, line + "\r\n");
            StdOut.write(null, "Planner completed at %s.\r\n", new Date());
            Status(Status.READY);
            plannerThread = null;
            lpgProcess = null;
            stopPlanner();
        } catch (Exception e) {
            e.printStackTrace();
            Status(Status.READY);
        }
    }

    @SuppressWarnings("deprecation")
    public void stopPlanner() {
        if (plannerThread != null) plannerThread.stop();
        if (lpgProcess != null) lpgProcess.destroy();
        plannerThread = null;
        lpgProcess = null;
        collectionSolution();
        synchronized (this) {
            this.notifyAll();
        }
        if (completeListener != null) completeListener.run();
    }

    private PddlSolution getSolution(int index) {
        PddlSolution result = null;
        String filename = inputPddlFile + "_" + index + ".SOL";
        if (new File(filename).exists()) try {
            result = new PddlSolution(null, filename);
            result.setName(new File(filename).getName());
        } catch (FileNotFoundException e) {
        }
        return result;
    }

    protected void collectionSolution() {
        solutions.clear();
        for (int i = 1; ; i++) {
            PddlSolution sol = getSolution(i);
            if (sol != null) solutions.add(sol); else break;
        }
    }

    public NamedList<PddlSolution> getSolutions() {
        return solutions;
    }

    public int AvailableSolution() {
        int result = 0;
        for (int sol = 1; ; sol++) {
            String filename = inputPddlFile + "_" + sol + ".SOL";
            if (new File(filename).exists()) result++; else break;
        }
        return result;
    }

    public void waitUntilComplete() {
        if (plannerThread != null) synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException e) {
            }
        }
    }
}
