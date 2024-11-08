package org.grailrtls.solver;

import java.net.*;
import java.io.*;
import java.util.*;

@SuppressWarnings("serial")
public class LocalizationSolver {

    private static final String WORK_DIR = "./non-java/solver/work-area/";

    public static final int INFO_REGION = 0;

    public static final int INFO_MIN_X = 1;

    public static final int INFO_MAX_X = 2;

    public static final int INFO_MIN_Y = 3;

    public static final int INFO_MAX_Y = 4;

    public static final int INFO_RESULT = 5;

    public static final int INFO_LANDMARKS = 6;

    public static final int INFO_TRAINING = 7;

    public static final int INFO_TESTING = 8;

    public static final int INFO_ALGORITHM = 9;

    public static final int INFO_BURNIN = 10;

    public static final int INFO_ITERATION = 11;

    public static final int INFO_THRESHOLD = 12;

    public static final int INFO_X_TILE = 13;

    public static final int INFO_Y_TILE = 14;

    public static final int INFO_NUM_NEIGHBORS = 15;

    public static final int INFO_NUM_NEIGHBOR_LANDMARKS = 16;

    private static boolean debug = true;

    public static final HashMap<String, Integer> INFO_KEY = new HashMap<String, Integer>(16) {

        {
            put("region", new Integer(INFO_REGION));
            put("minx", new Integer(INFO_MIN_X));
            put("maxx", new Integer(INFO_MAX_X));
            put("miny", new Integer(INFO_MIN_Y));
            put("maxy", new Integer(INFO_MAX_Y));
            put("result", new Integer(INFO_RESULT));
            put("landmarks", new Integer(INFO_LANDMARKS));
            put("training", new Integer(INFO_TRAINING));
            put("testing", new Integer(INFO_TESTING));
            put("algorithm", new Integer(INFO_ALGORITHM));
            put("burnin", new Integer(INFO_BURNIN));
            put("iterations", new Integer(INFO_ITERATION));
            put("threshold", new Integer(INFO_THRESHOLD));
            put("xTiles", new Integer(INFO_X_TILE));
            put("yTiles", new Integer(INFO_Y_TILE));
            put("neighbors", new Integer(INFO_NUM_NEIGHBORS));
            put("neighborLandmarks", new Integer(INFO_NUM_NEIGHBOR_LANDMARKS));
        }
    };

    public static final HashMap<String, String> ALGO_MAP = new HashMap<String, String>(6) {

        {
            put("fs_m1", "org.grailrtls.solver.FastSolver");
            put("fs_m2", "org.grailrtls.solver.FastSolver");
            put("fs_m3", "org.grailrtls.solver.FastSolver");
            put("spm", "org.grailrtls.solver.spm.SPMSolver");
            put("knn", "org.grailrtls.solver.knn.KNNSolver");
            put("knl", "org.grailrtls.solver.knl.KNLSolver");
        }
    };

    private PrintWriter toServer = null;

    private Scanner fromServer = null;

    private HashMap<String, Object> info;

    private String workDir;

    private int msgId = 0;

    public LocalizationSolver(String name, String serverIP, int portNum, String workDir) {
        this.info = new HashMap<String, Object>();
        this.workDir = workDir;
        try {
            Socket solverSocket = new Socket(serverIP, portNum);
            this.fromServer = new Scanner(solverSocket.getInputStream());
            this.toServer = new PrintWriter(solverSocket.getOutputStream(), true);
            this.toServer.println("login client abc");
            this.toServer.println("solver " + name);
            System.out.println(this.fromServer.nextLine());
        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Localization Solver started with name: " + name);
    }

    private void parseInfo() {
        Scanner tmp;
        String next, key;
        next = this.fromServer.nextLine();
        this.info.clear();
        while (!next.equals("end")) {
            tmp = new Scanner(next);
            tmp.useDelimiter("=");
            key = tmp.next();
            if (INFO_KEY.containsKey(key)) {
                switch(INFO_KEY.get(key).intValue()) {
                    case INFO_MIN_X:
                    case INFO_MAX_X:
                    case INFO_MIN_Y:
                    case INFO_MAX_Y:
                    case INFO_THRESHOLD:
                        this.info.put(key, new Double(tmp.nextDouble()));
                        break;
                    case INFO_BURNIN:
                    case INFO_ITERATION:
                    case INFO_X_TILE:
                    case INFO_Y_TILE:
                    case INFO_NUM_NEIGHBORS:
                    case INFO_NUM_NEIGHBOR_LANDMARKS:
                        this.info.put(key, new Integer(tmp.nextInt()));
                        break;
                    case INFO_REGION:
                    case INFO_RESULT:
                    case INFO_ALGORITHM:
                        this.info.put(key, tmp.next());
                        break;
                    case INFO_LANDMARKS:
                        this.info.put(key, parseLandmarks());
                        break;
                    case INFO_TRAINING:
                    case INFO_TESTING:
                        this.info.put(key, parseFingerPrint(INFO_KEY.get(key).intValue()));
                        break;
                    default:
                        System.err.println("IGNORE info line:" + next);
                }
            }
            next = this.fromServer.nextLine();
        }
    }

    @SuppressWarnings("unchecked")
    private void printInfo() {
        String key;
        ArrayList<Object> list;
        Iterator<String> itr = this.info.keySet().iterator();
        System.out.println("\n\n======Infomration parsed======:\n");
        while (itr.hasNext()) {
            key = itr.next();
            switch(INFO_KEY.get(key).intValue()) {
                case INFO_LANDMARKS:
                case INFO_TRAINING:
                case INFO_TESTING:
                    System.out.println(key + ":");
                    list = (ArrayList<Object>) this.info.get(key);
                    for (Object obj : list) System.out.println(obj);
                    break;
                default:
                    System.out.println(key + "=" + this.info.get(key));
            }
            System.out.println("-------------------------------");
        }
    }

    private ArrayList<FingerPrint> parseFingerPrint(int type) {
        ArrayList<FingerPrint> fps = new ArrayList<FingerPrint>();
        this.fromServer.useDelimiter("[ |\n]");
        String tmp = this.fromServer.next();
        while (!tmp.equals("eotraining") && !tmp.equals("eotesting")) {
            FingerPrint fp = null;
            if (type == INFO_TRAINING) {
                fp = new FingerPrint(Double.parseDouble(tmp), this.fromServer.nextDouble());
            } else {
                fp = new FingerPrint(FingerPrint.NA_VALUE, FingerPrint.NA_VALUE);
                fp.setNetID(tmp);
                this.fromServer.next();
                this.fromServer.next();
            }
            Scanner sc = new Scanner(this.fromServer.nextLine());
            sc.useDelimiter(" ");
            while (sc.hasNext()) fp.addSS(sc.nextDouble());
            fps.add(fp);
            tmp = this.fromServer.next();
        }
        this.fromServer.nextLine();
        return fps;
    }

    private ArrayList<Landmark> parseLandmarks() {
        String mac;
        ArrayList<Landmark> lms = new ArrayList<Landmark>();
        this.fromServer.useDelimiter("[ |\n]");
        mac = this.fromServer.next();
        while (!mac.equals("eolandmarks")) {
            lms.add(new Landmark(mac, this.fromServer.nextDouble(), this.fromServer.nextDouble(), this.fromServer.nextDouble()));
            mac = this.fromServer.next();
        }
        this.fromServer.nextLine();
        return lms;
    }

    private void solve() throws Exception {
        String algo = (String) this.info.get("algorithm");
        if (!ALGO_MAP.containsKey(algo)) throw new Exception("Unknown algorithm :" + algo);
        ClassLoader cl = this.getClass().getClassLoader();
        System.out.println("----------------------");
        System.out.println(ALGO_MAP.get(algo) + " triggered for this request");
        System.out.println("----------------------");
        SolverIfc sol = (SolverIfc) (cl.loadClass(ALGO_MAP.get(algo)).newInstance());
        sol.setInfo(this.info, this.workDir);
        sol.solve();
        this.toServer.println("messageID " + this.msgId++);
        sol.printResult(this.toServer);
        this.toServer.println("end");
        this.toServer.flush();
        if (!debug) sol.cleanUp();
    }

    public void start() {
        try {
            while (true) {
                parseInfo();
                printInfo();
                solve();
            }
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void writeProcessIDFile(String dir, String name) {
        String fileName;
        PrintWriter out;
        int pid;
        try {
            fileName = dir.concat("/").concat(name).concat(".pid");
            out = new PrintWriter(new FileOutputStream(fileName));
            pid = getMyPID();
            if (pid == -1) {
                System.out.println("Error writing process ID file");
                return;
            }
            out.println(pid);
            out.flush();
            out.close();
        } catch (Exception e) {
            System.out.println("Error writing process ID file" + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static int getMyPID() {
        Runtime run;
        Process proc;
        BufferedReader in;
        String programAll[];
        String line;
        int myPID;
        myPID = -1;
        programAll = new String[3];
        programAll[0] = "perl";
        programAll[1] = "-e";
        programAll[2] = "print  getppid() ";
        programAll[2] = programAll[2].concat(".\"\\n\" ");
        try {
            run = Runtime.getRuntime();
            proc = run.exec(programAll);
            in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            line = in.readLine();
            myPID = Integer.parseInt(line);
        } catch (Exception e) {
            System.out.println("Error getting process ID " + e);
            e.printStackTrace();
        }
        return myPID;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("usage: java LocalizationSolver <solver engine name> <server IP> <port number> [work dir] ");
            System.exit(0);
        }
        int portNum = 0;
        try {
            portNum = Integer.parseInt(args[2]);
            LocalizationSolver solver = null;
            if (args.length == 4) solver = new LocalizationSolver(args[0], args[1], portNum, args[3]); else solver = new LocalizationSolver(args[0], args[1], portNum, WORK_DIR);
            solver.start();
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(0);
        }
    }
}
