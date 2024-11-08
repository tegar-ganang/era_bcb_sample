package de.gstpl.algo.itc;

import de.gstpl.algo.AutomaticTT;
import de.gstpl.algo.GeneralAlgoProperties;
import de.gstpl.algo.IAlgorithm;
import de.gstpl.algo.TimetableObjectCollection;
import de.gstpl.algo.genetic.GeneticAlgoProperties;
import de.gstpl.algo.genetic.GeneticAlgorithmA;
import de.gstpl.algo.localsearch.NoCollisionPrincipleA;
import de.gstpl.data.ApplicationProperties;
import de.gstpl.data.DBException;
import de.gstpl.data.DBFactory;
import de.gstpl.data.GDB;
import de.gstpl.data.IDBProperties;
import de.gstpl.data.IFeature;
import de.gstpl.data.IPerson;
import de.gstpl.data.IRoom;
import de.gstpl.data.ISubject;
import de.gstpl.data.ITimeInterval;
import de.gstpl.data.TimeFormat;
import de.gstpl.data.set.ERasterType;
import de.gstpl.data.set.IWeekRaster;
import de.peathal.util.GLog;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class supports import + export of Track 2 of the International
 * Timetabling Competition.
 *
 * Track Number 2 has several names:
 * "The problem model has been given various names in the literature including
 * the Class Timetabling Problem, the Event Timetabling Problem, the Class
 * assignment Problem, and the University Course Timetabling Problem.
 * Taken from http://www.cs.qub.ac.uk/eventmap/postenrolcourse/report/Post%20Enrolment%20based%20CourseTimetabling.pdf
 * @see http://www.cs.qub.ac.uk/eventmap/postenrolcourse/course_post_index_files/Inputformat.htm
 * @author Peter Karich, peat_hal 'at' users 'dot' sourceforge 'dot' net
 */
public class Track2 {

    /** 
     * There are some command line options; you can use either
     * -f realtiveOrAbsoluteFolder/file.tim
     * -f realtiveOrAbsoluteFolder
     * -seed long
     * -time timeInSeconds
     * 
     * Periode.compress():
     * -finalDepth how deep the algorithm should go for small hard constraint 
     *             violations in Periode.compress
     * -depthMultiplier multiplied with depth this results in the number of 
     *             maximal allowed colliding partners in Periode.compress
     * -maxNodes number of maximal nodes to be processed in Periode.compress
     * 
     * Try to get the files from 
     * http://www.cs.qub.ac.uk/eventmap/Login/SecretPage.php
     */
    public static void main(String arg[]) {
        boolean saveToDatabase = false;
        ApplicationProperties props = ApplicationProperties.get();
        if (saveToDatabase) {
            props.setDatabase("itc-connected");
        } else {
            props.setDatabase("itc-disconnected");
        }
        int seconds = 20;
        long seed = 181282L;
        File folderOrFile = toAbsFile(new File("track2"));
        props.setMaxNodes(15000);
        props.setFinalDepth(4);
        props.setDepthMultiplier(1.5);
        for (int a = 0; a < arg.length; a++) {
            if (arg[a].equalsIgnoreCase("-f")) {
                folderOrFile = toAbsFile(new File(arg[a + 1]));
            }
            if (arg[a].equalsIgnoreCase("-time")) {
                seconds = Integer.valueOf(arg[a + 1]);
            }
            if (arg[a].equalsIgnoreCase("-seed")) {
                seed = Long.valueOf(arg[a + 1]);
            }
            if (arg[a].equalsIgnoreCase("-maxNodes")) {
                props.setMaxNodes(Integer.valueOf(arg[a + 1]));
            }
            if (arg[a].equalsIgnoreCase("-finalDepth")) {
                props.setFinalDepth(Integer.valueOf(arg[a + 1]));
            }
            if (arg[a].equalsIgnoreCase("-depthMultiplier")) {
                props.setDepthMultiplier(Double.valueOf(arg[a + 1]).longValue());
            }
        }
        if (folderOrFile == null) {
            GLog.error("Can't find .tim files! You specified directory or file was: " + folderOrFile);
            GLog.error("Please specify correct working directory via: -Duser.dir=<userdir>");
            return;
        }
        File[] files;
        String folder;
        if (folderOrFile.isDirectory()) {
            files = folderOrFile.listFiles();
            folder = folderOrFile.getAbsolutePath();
        } else {
            files = new File[] { folderOrFile };
            folder = folderOrFile.getParent();
        }
        Arrays.sort(files);
        int counter = 0;
        for (File f : files) {
            if (!f.getName().endsWith(".tim")) {
                GLog.log("Skipped file (not a .tim file):" + f);
                continue;
            }
            if (!f.exists()) {
                GLog.log("Skipped file (does not exist):" + f);
                continue;
            }
            counter++;
            long start = System.currentTimeMillis();
            System.gc();
            String file = folder + File.separatorChar + f.getName().split(".tim")[0];
            GLog.log("Process file:" + file);
            Track2 track2Obj = new Track2(seed);
            try {
                GLog.setLevel(GLog.DEBUG);
                GLog.setOutput(GLog.OutputType.CONSOLE);
                track2Obj.parse(file);
                if (saveToDatabase) {
                    track2Obj.save();
                    break;
                }
                List<? extends ITimeInterval> optimizedTIs = track2Obj.optimize(seconds);
                if (optimizedTIs != null) {
                    track2Obj.writeToFile(optimizedTIs, file);
                } else {
                    GLog.warn("Couldn't write file. No subjects available.");
                }
            } catch (InterruptedException ex) {
                GLog.warn("Waiting for optimization was interrupted.", ex);
            } catch (FileNotFoundException ex) {
                GLog.warn("Couldn't find file:" + file, ex);
            } catch (IOException ex) {
                GLog.warn("IOException while parsing.", ex);
            } catch (DBException ex) {
                GLog.warn("DBException while parsing.", ex);
            }
            GLog.debug2("Total Time (with read and write): " + (System.currentTimeMillis() - start) / 1000f + " seconds necessary \tfor: " + file);
        }
    }

    /**
     * This method transforms a relative file/folder into a file/folder with 
     * absolute path.
     */
    private static File toAbsFile(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException ex) {
            return f.getAbsoluteFile();
        }
    }

    private IAlgorithm startingAlgorithm;

    private List<IRoom> allRooms;

    private List<ISubject> allSubjects;

    private List<ITimeInterval> allTimeIntervals;

    private List<IPerson> allPersons;

    private List<IFeature> allFeatures;

    private int newRoomCounter;

    private int newSubjectCounter;

    private int newPersonCounter;

    private int newFeatureCounter;

    private int noOfTimeSlots;

    private int noOfSubjects;

    private int noOfRooms;

    private int noOfFeatures;

    private int noOfPersons;

    private int dayNo;

    private int dayDuration;

    private GDB db;

    public Track2(long seed) {
        newRoomCounter = 0;
        newSubjectCounter = 0;
        newPersonCounter = 0;
        newFeatureCounter = 0;
        IDBProperties prop = DBFactory.getDefaultDB().getDBProperties();
        dayNo = prop.getDayNo();
        dayDuration = prop.getDayDuration();
        noOfTimeSlots = dayNo * dayDuration;
        ApplicationProperties.get().setRandomSeed(seed);
        startingAlgorithm = getStartingAlgorithm();
        db = startingAlgorithm.getObjects().getDatabase();
    }

    public void printStatistics() {
        int urooms = 0;
        int uti = 0;
        int noOfOrderingProblems = 0;
        int noOfPersonCollisions = 0;
        int noOfRoomCollisions = 0;
        int noOfUnplacedTIs = 0;
        int distanceToFeasibility = 0;
        boolean moreDetails = false;
        if (moreDetails) {
            GLog.log("----------------------------");
            GLog.log("ITimeInterval with raster collisions:");
            for (ISubject s : allSubjects) {
                int no = ((ITCSubject) s).getRasterViolations(TimeFormat.DEFAULT_WEEK);
                if (no > 0) {
                    GLog.log("Rasterviolations:" + s + ":" + no);
                }
            }
            GLog.log("----------------------------");
            GLog.log("Unsatisfied features of following ISubject's:");
            for (ISubject s : allSubjects) {
                int no = ((ITCSubject) s).getFeatureViolations(TimeFormat.DEFAULT_WEEK);
                if (no > 0) {
                    GLog.log("feature violation:" + s + ":" + no);
                }
            }
            GLog.log("----------------------------");
            GLog.log("Unsatisfied Ordering of following ITimeInterval's:");
        }
        for (ISubject s : allSubjects) {
            ITimeInterval ti = ((ITCSubject) s).getSingleTI();
            if (ti.isUnplaced()) {
                noOfUnplacedTIs++;
                distanceToFeasibility += ti.getSubject().getPersons().size();
                assert ti.getRoom() == null : ti.getRoom();
            } else {
                int no = ((ITCSubject) s).getOrderViolations();
                if (no > 0) {
                    noOfOrderingProblems++;
                }
            }
        }
        if (moreDetails) {
            GLog.log("----------------------------");
            GLog.log("Collisions of following IPerson's:");
        }
        for (IPerson p : allPersons) {
            int no = p.getWeekTISet(TimeFormat.DEFAULT_WEEK).getCollisions();
            if (no > 0) {
                if (moreDetails) {
                    GLog.log(p + ":" + no);
                }
                noOfPersonCollisions++;
            }
        }
        if (moreDetails) {
            GLog.log("----------------------------");
            GLog.log("Collisions of following IRoom's:");
        }
        for (IRoom r : allRooms) {
            int no = r.getWeekTISet(TimeFormat.DEFAULT_WEEK).getCollisions();
            if (no > 0) {
                if (moreDetails) {
                    GLog.log(r + ":" + no);
                }
                noOfRoomCollisions++;
            }
        }
        if (moreDetails) {
            GLog.log("----------------------------");
            GLog.log("Following IPerson's have >3 ITimeInterval's in one day:");
        }
        int moreThan2 = 0;
        for (IPerson p : allPersons) {
            int counter = ((ITCPerson) p).getSuccessiveViolation();
            if (counter > 0) {
                if (moreDetails) {
                    GLog.log(p + " has successive violation: " + counter);
                }
                moreThan2 += counter;
            }
        }
        if (moreDetails) {
            GLog.log("----------------------------");
            GLog.log("Following IPerson's have a single ITimeInterval in one day:");
        }
        int onlyOne = 0;
        for (IPerson p : allPersons) {
            int counter = ((ITCPerson) p).getSingleViolation();
            if (counter > 0) {
                if (moreDetails) {
                    GLog.log(p + " has only a single event: " + counter);
                }
                onlyOne += counter;
            }
        }
        if (moreDetails) {
            GLog.log("----------------------------");
            GLog.log("Following IPerson's have ITimeInterval at the end of a day:");
        }
        int atTheEnd = 0;
        for (IPerson p : allPersons) {
            int counter = ((ITCPerson) p).getEndViolation();
            if (counter > 0) {
                if (moreDetails) {
                    GLog.log(p + " has an event at the end: " + counter);
                }
                atTheEnd += counter;
            }
        }
        GLog.log("No of unsuitable IRoom's:" + urooms);
        GLog.log("No of unsuitable ITimeInterval's:" + uti);
        GLog.log("No of ordering problems:" + noOfOrderingProblems);
        GLog.log("No of Person collisions:" + noOfPersonCollisions);
        GLog.log("No of Room collisions:" + noOfRoomCollisions);
        GLog.log("### No of unplaced events:" + noOfUnplacedTIs + " out of:" + allSubjects.size() + " => " + (float) 100 * noOfUnplacedTIs / allSubjects.size() + " % are unplaced ###");
        GLog.log("Distance to a feasibility solution:" + distanceToFeasibility);
        GLog.log("Penalty for students having three or more events in a row:" + moreThan2);
        GLog.log("Penalty for students having single events on a day:" + onlyOne);
        GLog.log("Penalty for students having end of day events:" + atTheEnd);
        GLog.log("Total soft constraint penalty:" + (onlyOne + atTheEnd + moreThan2));
    }

    /**
     * This method parses an input file with a special timetabling format.
     * @see http://www.cs.qub.ac.uk/eventmap/postenrolcourse/course_post_index_files/Inputformat.htm
     */
    public void parse(String fileName) throws FileNotFoundException, IOException, DBException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName + ".tim"));
        try {
            String numberOf[] = reader.readLine().split(" ");
            noOfSubjects = Integer.parseInt(numberOf[0]);
            noOfRooms = Integer.parseInt(numberOf[1]);
            noOfFeatures = Integer.parseInt(numberOf[2]);
            noOfPersons = Integer.parseInt(numberOf[3]);
            allRooms = new ArrayList<IRoom>(noOfRooms);
            allSubjects = new ArrayList<ISubject>(noOfSubjects);
            allTimeIntervals = new ArrayList<ITimeInterval>(noOfSubjects);
            allPersons = new ArrayList<IPerson>(noOfPersons);
            allFeatures = new ArrayList<IFeature>(noOfFeatures);
            for (int i = 0; i < noOfRooms; i++) {
                allRooms.add(newRoom(Integer.parseInt(reader.readLine())));
            }
            for (int i = 0; i < noOfSubjects; i++) {
                allSubjects.add(newSubject());
            }
            for (int i = 0; i < noOfFeatures; i++) {
                allFeatures.add(newFeature());
            }
            ITCPerson currentPerson;
            for (int p = 0; p < noOfPersons; p++) {
                allPersons.add(currentPerson = newPerson());
                for (int s = 0; s < noOfSubjects; s++) {
                    if ((Integer.parseInt(reader.readLine()) > 0)) {
                        currentPerson.addSubject(allSubjects.get(s), true);
                    }
                }
            }
            for (IRoom currentRoom : allRooms) {
                for (int f = 0; f < noOfFeatures; f++) {
                    if ((Integer.parseInt(reader.readLine()) > 0)) {
                        ((ITCRoom) currentRoom).addFeature(allFeatures.get(f));
                    }
                }
            }
            for (ISubject currentSubject : allSubjects) {
                for (int f = 0; f < noOfFeatures; f++) {
                    if ((Integer.parseInt(reader.readLine()) != 0)) {
                        ((ITCSubject) currentSubject).addFeature(allFeatures.get(f));
                    }
                }
            }
            for (ISubject currentSubject : allSubjects) {
                IWeekRaster raster = currentSubject.getWeekRaster(TimeFormat.DEFAULT_WEEK);
                for (int ts = 0; ts < noOfTimeSlots; ts++) {
                    if (Integer.parseInt(reader.readLine()) != 0) {
                        raster.set(ts, ERasterType.ALLOWED);
                    }
                }
            }
            for (ISubject firstSubject : allSubjects) {
                for (ISubject secSubject : allSubjects) {
                    int i = Integer.parseInt(reader.readLine());
                    if (i == 1) {
                        ((ITCSubject) firstSubject).addFollow((ITCSubject) secSubject);
                        ((ITCSubject) secSubject).addBefore((ITCSubject) firstSubject);
                    } else if (i == -1) {
                    }
                }
            }
            String line = reader.readLine();
            assert line == null : "Line:<" + line + ">";
        } finally {
            reader.close();
        }
    }

    /**
     * This method writes the solution to a file.
     */
    public void writeToFile(List<? extends ITimeInterval> optTIs, String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName + ".sln"));
        GLog.debug2("Write to file: " + fileName + ".sln");
        try {
            for (ITimeInterval ti : optTIs) {
                assert ti.getStartTime() == -1 ? ti.getRoom() == null : true;
                writer.write(ti.getStartTime() + " " + allRooms.indexOf(ti.getRoom()) + "\n");
            }
        } finally {
            writer.close();
        }
    }

    public ITCRoom newRoom(int capacity) throws DBException {
        IRoom r = db.create(false, IRoom.class, "Room " + (newRoomCounter++));
        r.setCapacity(capacity);
        return (ITCRoom) r;
    }

    private int roomCounterForSubject = 0;

    public ITCSubject newSubject() throws DBException {
        ITCSubject s = (ITCSubject) db.create(false, ISubject.class, "Subject " + (newSubjectCounter++));
        ITimeInterval ti = db.createTI(false, ApplicationProperties.get().getRandom().nextInt(noOfTimeSlots), 1);
        allTimeIntervals.add(ti);
        ti.setRoom(allRooms.get(roomCounterForSubject++), true);
        if (roomCounterForSubject >= allRooms.size()) {
            roomCounterForSubject = 0;
        }
        s.addTimeInterval(ti, true);
        return s;
    }

    public ITCPerson newPerson() throws DBException {
        return (ITCPerson) db.create(false, IPerson.class, "Person " + (newPersonCounter++));
    }

    public IFeature newFeature() throws DBException {
        return db.createFeature("Feature " + (newFeatureCounter++));
    }

    private void save() throws DBException {
        ITCConnectedDB connectedDB = (ITCConnectedDB) db;
        GLog.log("Start saving.");
        for (ISubject s : allSubjects) {
            connectedDB.update(s);
        }
        for (IRoom r : allRooms) {
            connectedDB.update(r);
        }
        for (IPerson p : allPersons) {
            connectedDB.update(p);
        }
        for (IFeature feat : allFeatures) {
            connectedDB.update(feat);
        }
        try {
            connectedDB.saveChanges();
        } catch (DBException ex) {
            GLog.warn("Can't save changes.", ex);
        }
        GLog.log("Finished saving.");
        connectedDB.closeConnection();
    }

    public List<? extends ITimeInterval> optimize(int seconds) throws InterruptedException {
        GLog.debug2("Try to optimize within:" + seconds + " seconds.");
        float percentageForNCP = 100f / 100;
        float ncp = seconds * percentageForNCP / 60;
        TimetableObjectCollection ttObjects = startingAlgorithm.getObjects();
        ttObjects.setSubjects(allSubjects);
        ttObjects.setTimeIntervals(allTimeIntervals);
        ttObjects.setRooms(allRooms);
        ttObjects.setPersons(allPersons);
        if (ncp > 0f) {
            initProps(ttObjects.getProperties(), ncp);
            startingAlgorithm.doWork();
            startingAlgorithm.join(0);
        }
        new AutomaticTT(db).removeHardConstraintViolations(ttObjects);
        List<ITimeInterval> result = new ArrayList<ITimeInterval>(ttObjects.getSubjects().size());
        for (ISubject s : ttObjects.getSubjects()) {
            result.add(s.getTimeIntervals().iterator().next());
        }
        ttObjects.setTimeIntervals(result);
        return ttObjects.getTimeIntervals();
    }

    private void initProps(GeneralAlgoProperties prop, float minutes) {
        prop.setEnableSortingOnStart(false);
        prop.setGuiAvailable(false);
        prop.setDelayInMinutes(minutes);
        prop.setSaveResults(true);
    }

    private IAlgorithm getStartingAlgorithm() {
        return new NoCollisionPrincipleA();
    }

    private IAlgorithm getGA(TimetableObjectCollection ttObjects) {
        GeneticAlgorithmA gen = new GeneticAlgorithmA(ttObjects);
        ((GeneticAlgoProperties) gen.getProperties()).setBestIndivCorrectedIfMutationFails(false);
        return gen;
    }
}
