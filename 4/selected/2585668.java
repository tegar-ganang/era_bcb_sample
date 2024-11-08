package ms.jasim.pddl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ms.jasim.framework.Constants;
import ms.jasim.framework.IConfiguration;
import ms.jasim.framework.IMessageConsole;
import ms.jasim.framework.IServiceProvider;
import ms.jasim.model.JasimModel;
import ms.jasim.model.items.IntItem;
import ms.jasim.model.items.Item;
import ms.jasim.model.items.ItemList;
import ms.utils.EventListImpl;
import ms.utils.IEvent;
import ms.utils.IEventList;
import ms.utils.NamedList;
import ms.utils.PerformanceTimer;

public class PddlPlanner implements Runnable {

    public enum Status {

        READY, PLANNING
    }

    ;

    protected final IConfiguration config;

    protected final IMessageConsole stdOut;

    protected Status status;

    private String inputPddlFile;

    private String modelFile;

    private int numOfSolution;

    private NamedList<PddlSolution> solutions = new NamedList<PddlSolution>();

    public final IEventList<IEvent<Object>> onCompleted = new EventListImpl<IEvent<Object>>();

    private Process lpgProcess;

    private JasimModel model;

    private int maxCpuTime;

    private boolean cancelled;

    private PddlProblem problem;

    public PddlPlanner(IServiceProvider context) {
        config = context.getService(IConfiguration.class);
        stdOut = context.getService(IMessageConsole.class);
    }

    public void setPddlModel(String modeluri, JasimModel model) {
        this.model = model;
        File file = new File(modeluri);
        this.modelFile = file.getName();
    }

    public void setMaxSolution(int maxSolution) {
        this.numOfSolution = maxSolution;
    }

    public void setMaxCpuTime(int maxCpuTime) {
        this.maxCpuTime = maxCpuTime;
    }

    public int getMaxSolution() {
        if (numOfSolution > 0) return numOfSolution;
        Item po = model.getOptions().get("PlannerOptions");
        if (po != null && po instanceof ItemList) {
            Item maxSolution = ((ItemList) po).get("MaxSolution");
            if (maxSolution != null && maxSolution instanceof IntItem) return ((IntItem) maxSolution).getValue();
        }
        return 1;
    }

    public int getMaxCpuTime() {
        if (maxCpuTime > 0) return numOfSolution;
        Item po = model.getOptions().get("PlannerOptions");
        if (po != null && po instanceof ItemList) {
            Item item = ((ItemList) po).get("MaxCpuTime");
            if (item != null && item instanceof IntItem) return ((IntItem) item).getValue();
        }
        return 5;
    }

    public Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status value) {
        status = value;
    }

    @Override
    public void run() {
        PerformanceTimer.createEntry("Planner", "Solution planning");
        solutions.clear();
        while (solutions.size() < getMaxSolution()) {
            runPlanner();
            List<PddlSolution> list = collectionSolution();
            if (list.size() > 0) {
                if (solutions.size() + list.size() <= getMaxSolution()) solutions.addAll(list); else solutions.addAll(list.size() - getMaxSolution() + solutions.size(), list);
            } else break;
        }
        for (int i = 0; i < solutions.size(); i++) solutions.get(i).setName("Sol#" + i);
        PerformanceTimer.closeEntry();
        runCompleted();
    }

    protected void runPlanner() {
        try {
            String tempFolder = config.getString("temp_folder", "");
            int i = modelFile.lastIndexOf(".");
            inputPddlFile = tempFolder + "\\" + modelFile.substring(0, i) + "-auto.pddl";
            problem = model.generatePddlProblem();
            problem.writeToFile(inputPddlFile);
            setStatus(Status.PLANNING);
            String command = config.getString("planner", "");
            if (command.length() > 0) {
                String arg = String.format("%s %s %s %d %d", command, modelFile, inputPddlFile, getMaxSolution(), getMaxCpuTime());
                writeMessage(arg + "\r\n");
                ProcessBuilder b = new ProcessBuilder(Constants.CMD_PATH, "/C", arg);
                b.redirectErrorStream(true);
                lpgProcess = b.start();
                InputStream is = lpgProcess.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = br.readLine();
                cancelled = false;
                for (; line != null && !this.cancelled; ) while ((line = br.readLine()) != null) writeMessage(line + "\r\n");
                if (!this.cancelled) writeMessage("Planner completed at %s.\r\n", new Date()); else {
                    writeMessage("User cancelled as %s.\r\n", new Date());
                    lpgProcess.destroy();
                }
                setStatus(Status.READY);
                lpgProcess = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(Status.READY);
        }
    }

    public void cancel() {
        this.cancelled = true;
    }

    private void writeMessage(String message, Object... args) {
        if (stdOut != null) stdOut.write("Planner", String.format(message, args)); else System.out.print(message);
    }

    private void runCompleted() {
        lpgProcess = null;
        collectionSolution();
        ((EventListImpl<IEvent<Object>>) onCompleted).fire(this, null);
    }

    private PddlSolution getSolution(int index) {
        PddlSolution result = null;
        String filename = inputPddlFile + "_" + index + ".SOL";
        if (new File(filename).exists()) try {
            result = new PddlSolution(problem, filename);
            result.setName(new File(filename).getName());
        } catch (FileNotFoundException e) {
        }
        return result;
    }

    protected List<PddlSolution> collectionSolution() {
        ArrayList<PddlSolution> list = new ArrayList<PddlSolution>();
        for (int cnt = 1; ; cnt++) {
            PddlSolution sol = getSolution(cnt);
            if (sol != null) list.add(sol); else break;
        }
        return list;
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
}
