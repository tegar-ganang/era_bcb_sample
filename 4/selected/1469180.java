package arch;

import static jason.asSyntax.ASSyntax.createAtom;
import static jason.asSyntax.ASSyntax.createLiteral;
import static jason.asSyntax.ASSyntax.createNumber;
import jason.JasonException;
import jason.ReceiverNotFoundException;
import jason.RevisionFailedException;
import jason.asSemantics.Intention;
import jason.asSemantics.Message;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.Literal;
import jason.asSyntax.LiteralImpl;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.grid.Location;
import jason.mas2j.ClassParameters;
import jason.runtime.Settings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jia.cluster;
import jmoise.OrgAgent;
import agent.SelectEvent;
import env.WorldModel;
import env.WorldView;
import env.WorldModel.Move;
import env.ClusterModelFactory;
import env.CowModelFactory;
import env.IClusterModel;
import env.ICowModel;
import env.Cow;

/** 
 *  Common arch for both local and contest architectures.
 *  
 *   @author Jomi
 */
public class CowboyArch extends OrgAgent {

    LocalWorldModel model = null;

    WorldView view = null;

    ICowModel cModel = null;

    IClusterModel clModel = null;

    String simId = null;

    int myId = -1;

    boolean gui = false;

    boolean playing = false;

    String massimBackDir = null;

    String teamId = null;

    Map<Integer, Integer> lastSeen = new HashMap<Integer, Integer>();

    int simStep = 0;

    WriteStatusThread writeStatusThread = null;

    protected Logger logger = Logger.getLogger(CowboyArch.class.getName());

    public static Atom aOBSTACLE = new Atom("obstacle");

    public static Atom aENEMY = new Atom("enemy");

    public static Atom aENEMYCORRAL = new Atom("enemycorral");

    public static Atom aALLY = new Atom("ally");

    public static Atom aEMPTY = new Atom("empty");

    public static Atom aSWITCH = new Atom("switch");

    public static Atom aFENCE = new Atom("fence");

    public static Atom aOPEN = new Atom("open");

    public static Atom aCLOSED = new Atom("closed");

    @Override
    public void initAg(String agClass, ClassParameters bbPars, String asSrc, Settings stts) throws JasonException {
        super.initAg(agClass, bbPars, asSrc, stts);
        gui = "yes".equals(stts.getUserParameter("gui"));
        if (getMyId() == 0) gui = true;
        boolean writeStatus = "yes".equals(stts.getUserParameter("write_status"));
        boolean dumpAgsMind = "yes".equals(stts.getUserParameter("dump_ags_mind"));
        if (writeStatus || dumpAgsMind) writeStatusThread = WriteStatusThread.create(this, writeStatus, dumpAgsMind);
        teamId = stts.getUserParameter("teamid");
        if (teamId == null) logger.info("*** No 'teamid' parameter!!!!"); else if (teamId.startsWith("")) teamId = teamId.substring(1, teamId.length() - 1);
        WriteStatusThread.registerAgent(getAgName(), this);
        massimBackDir = stts.getUserParameter("ac_sim_back_dir");
        if (massimBackDir != null && massimBackDir.startsWith("\"")) massimBackDir = massimBackDir.substring(1, massimBackDir.length() - 1);
        logger = Logger.getLogger(CowboyArch.class.getName() + ".CA-" + getAgName());
        setCheckCommunicationLink(false);
        initialBeliefs();
    }

    void initialBeliefs() throws RevisionFailedException {
        getTS().getAg().addBel(createLiteral("team_size", createNumber(WorldModel.agsByTeam)));
        for (int i = 1; i <= WorldModel.agsByTeam; i++) getTS().getAg().addBel(createLiteral("agent_id", createAtom(teamId + i), createNumber(i)));
        getTS().getAg().addBel(createLiteral("cows_by_boy", createNumber(cluster.COWS_BY_BOY)));
    }

    @Override
    public void stopAg() {
        if (view != null) view.dispose();
        if (writeStatusThread != null) writeStatusThread.interrupt();
        super.stopAg();
    }

    void setSimId(String id) {
        simId = id;
    }

    public String getSimId() {
        return simId;
    }

    public boolean hasGUI() {
        return gui;
    }

    public int getMyId() {
        if (myId < 0) myId = getAgId(getAgName());
        return myId;
    }

    public LocalWorldModel getModel() {
        return model;
    }

    /** The perception of the grid size is removed from the percepts list 
        and "directly" added as a belief */
    void gsizePerceived(int w, int h, String opponent) throws RevisionFailedException {
        model = new LocalWorldModel(w, h, WorldModel.agsByTeam, getTS().getAg().getBB());
        model.setOpponent(opponent);
        getTS().getAg().addBel(Literal.parseLiteral("gsize(" + w + "," + h + ")"));
        playing = true;
        cModel = CowModelFactory.getModel(teamId + myId);
        cModel.setSize(w, h);
        clModel = ClusterModelFactory.getModel(teamId + myId);
        clModel.setStepcl(-1);
        if (view != null) view.dispose();
        if (gui) {
            try {
                view = new WorldView("Herding (view of cowboy " + (getMyId() + 1) + ") -- against " + opponent, model);
            } catch (Exception e) {
                logger.info("error starting GUI");
                e.printStackTrace();
            }
        }
        if (writeStatusThread != null) writeStatusThread.reset();
    }

    /** The perception of the corral location is removed from the percepts list 
        and "directly" added as a belief */
    void corralPerceived(Location upperLeft, Location downRight) throws RevisionFailedException {
        model.setCorral(upperLeft, downRight);
        getTS().getAg().addBel(createLiteral("corral", createNumber(upperLeft.x), createNumber(upperLeft.y), createNumber(downRight.x), createNumber(downRight.y)));
    }

    /** The number of steps of the simulation is removed from the percepts list 
        and "directly" added as a belief */
    void stepsPerceived(int s) throws RevisionFailedException {
        getTS().getAg().addBel(createLiteral("steps", createNumber(s)));
        model.setMaxSteps(s);
        model.restoreObstaclesFromFile(this);
        model.createStoreObsThread(this);
    }

    /** update set set of perceived cows */
    public void cowPerceived(int cowId, int x, int y) {
        Location c = new Location(x, y);
        if (model.getCorral().contains(c)) {
        } else {
            lastSeen.put(cowId, getSimStep());
            cModel.insertCow(cowId, x, y, getSimStep());
        }
    }

    public Cow[] getPerceivedCows() {
        return cModel.getCows();
    }

    public int getLastSeenCow(int cow) {
        return lastSeen.get(cow);
    }

    /** update the model with obstacle and share them with the team mates */
    public void obstaclePerceived(int x, int y) {
        if (!model.hasObject(WorldModel.OBSTACLE, x, y)) {
            model.add(WorldModel.OBSTACLE, x, y);
            Message m = new Message("tell", null, null, createCellPerception(x, y, aOBSTACLE));
            try {
                broadcast(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Location l = new Location(x, y);
            if (ephemeralObstacle.remove(l)) logger.info("uuuuu ephemeral location " + l + " perceived! so, it is no more ephemeral.");
        }
        if (clModel == null) clModel = ClusterModelFactory.getModel(teamId + myId);
        clModel.insertTree(x, y);
    }

    public void enemyCorralPerceived(int x, int y) {
        if (!model.hasObject(WorldModel.ENEMYCORRAL, x, y)) {
            model.add(WorldModel.ENEMYCORRAL, x, y);
            Message m = new Message("tell", null, null, createCellPerception(x, y, aENEMYCORRAL));
            try {
                broadcast(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void fencePerceived(int x, int y, boolean open) {
        int obj;
        Atom s;
        if (open) {
            obj = WorldModel.OPEN_FENCE;
            s = aOPEN;
        } else {
            obj = WorldModel.CLOSED_FENCE;
            s = aCLOSED;
        }
        if (!model.hasObject(obj, x, y)) {
            model.add(obj, x, y);
            Message m = new Message("tell", null, null, createCellPerception(x, y, ASSyntax.createStructure(aFENCE.toString(), s)));
            try {
                broadcast(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void switchPerceived(int x, int y) throws RevisionFailedException {
        if (!model.hasObject(WorldModel.SWITCH, x, y)) {
            getTS().getAg().addBel(createLiteral("switch", createNumber(x), createNumber(y)));
            Message m = new Message("tell", null, null, createCellPerception(x, y, aSWITCH));
            try {
                broadcast(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        model.add(WorldModel.SWITCH, x, y);
    }

    public void lineOfSightPerceived(int line) throws RevisionFailedException {
        model.setAgPerceptionRatio(line);
        getTS().getAg().addBel(createLiteral("ag_perception_ratio", createNumber(line)));
        getTS().getAg().addBel(createLiteral("cow_perception_ratio", createNumber(WorldModel.cowPerceptionRatio)));
    }

    private static final int lastRecordCapacity = 7;

    Queue<Location> lastLocations = new LinkedBlockingQueue<Location>(lastRecordCapacity);

    Queue<Move> lastActions = new LinkedBlockingQueue<Move>(lastRecordCapacity);

    Set<Location> ephemeralObstacle = new HashSet<Location>();

    ScheduledExecutorService schedule = new ScheduledThreadPoolExecutor(30);

    private static <T> void myAddQueue(Queue<T> q, T e) {
        if (!q.offer(e)) {
            q.poll();
            q.offer(e);
        }
    }

    private static <T> boolean allEquals(Queue<T> q) {
        if (q.size() + 1 < lastRecordCapacity) return false;
        Iterator<T> il = q.iterator();
        if (il.hasNext()) {
            T first = il.next();
            while (il.hasNext()) {
                T other = il.next();
                if (!first.equals(other)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isEphemeralObstacle(Location l) {
        return ephemeralObstacle.contains(l);
    }

    Location oldLoc;

    /** the location in previous cycle */
    public Location getLastLocation() {
        return oldLoc;
    }

    public Queue<Location> getLastLocations() {
        return lastLocations;
    }

    protected String lastAct;

    protected void setLastAct(String act) {
        lastAct = act;
        if (act != null) myAddQueue(lastActions, Move.valueOf(act));
    }

    public String getLastAction() {
        return lastAct;
    }

    public int getSimStep() {
        return simStep;
    }

    /** update the model with the agent location and share this information with team mates */
    void locationPerceived(final int x, final int y) {
        if (model == null) {
            logger.info("***** No model created yet! I cannot set my location");
            return;
        }
        oldLoc = model.getAgPos(getMyId());
        if (oldLoc != null) {
            model.clearAgView(oldLoc);
        }
        if (oldLoc == null || !oldLoc.equals(new Location(x, y))) {
            try {
                model.setAgPos(getMyId(), x, y);
                model.incVisited(x, y);
                Message m = new Message("tell", null, null, "my_status(" + x + "," + y + ")");
                broadcast(m);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        myAddQueue(lastLocations, new Location(x, y));
        checkRestart();
    }

    protected void checkRestart() {
        if (allEquals(lastLocations)) {
            try {
                logger.info("** Arch adding restart for " + getAgName() + ", last locations are " + lastLocations);
                getTS().getC().addAchvGoal(new LiteralImpl("restart"), Intention.EmptyInt);
                lastLocations.clear();
                lastSeen.clear();
            } catch (Exception e) {
                logger.info("Error in restart!" + e);
            }
            if (allEquals(lastActions) && lastActions.iterator().next() != Move.skip) {
                final Location newLoc = WorldModel.getNewLocationForAction(getLastLocation(), lastActions.peek());
                if (newLoc != null && !model.hasObject(WorldModel.OBSTACLE, newLoc)) {
                    if (model.hasObject(WorldModel.FENCE, newLoc)) {
                        logger.info("uuu adding restart for " + getAgName() + ", case of fence, last actions " + lastActions);
                        Literal l = new LiteralImpl("restart_fence_case");
                        l.addTerm(ASSyntax.createNumber(newLoc.x));
                        l.addTerm(ASSyntax.createNumber(newLoc.y));
                        getTS().getC().addAchvGoal(l, Intention.EmptyInt);
                    } else {
                        logger.info("uuu last actions and locations do not change!!!! setting " + newLoc + " as ephemeral obstacle");
                        model.add(WorldModel.OBSTACLE, newLoc);
                        ephemeralObstacle.add(newLoc);
                        schedule.schedule(new Runnable() {

                            public void run() {
                                if (ephemeralObstacle.contains(newLoc)) {
                                    logger.info("uuuuuuu removing ephemeral location " + newLoc);
                                    model.remove(WorldModel.OBSTACLE, newLoc);
                                }
                            }
                        }, 10, TimeUnit.SECONDS);
                    }
                }
            }
        }
    }

    public static Literal createCellPerception(int x, int y, Term obj) {
        return createLiteral("cell", createNumber(x), createNumber(y), obj);
    }

    void initKnownCows() {
        model.clearKnownCows();
    }

    void enemyPerceived(int x, int y) {
        model.add(WorldModel.ENEMY, x, y);
    }

    void simulationEndPerceived(String result) throws RevisionFailedException {
        getTS().getAg().addBel(Literal.parseLiteral("end_of_simulation(" + result + ")"));
        model = null;
        playing = false;
        lastSeen.clear();
        if (view != null) view.dispose();
        cModel.reset();
    }

    void setSimStep(int s) {
        ((SelectEvent) getTS().getAg()).cleanCows();
        simStep = s;
        try {
            super.setCycleNumber(simStep);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (view != null) view.setCycle(simStep);
        if (writeStatusThread != null) writeStatusThread.go();
    }

    void setScore(int s) {
        model.setCowsBlue(s);
    }

    /** change broadcast to send messages to only my team mates */
    @Override
    public void broadcast(Message m) throws Exception {
        if (model == null) return;
        for (int i = 1; i <= model.getAgsByTeam(); i++) {
            String oname = teamId + i;
            if (!getAgName().equals(oname)) {
                Message msg = new Message(m);
                msg.setReceiver(oname);
                for (int t = 0; t < 6; t++) {
                    try {
                        sendMsg(msg);
                        break;
                    } catch (ReceiverNotFoundException e) {
                        logger.info("Receiver " + oname + " not found: wait and try later, " + t);
                        Thread.sleep(500);
                    }
                }
            }
        }
    }

    @Override
    public void checkMail() {
        super.checkMail();
        Iterator<Message> im = getTS().getC().getMailBox().iterator();
        while (im.hasNext()) {
            Message m = im.next();
            try {
                if (m.getIlForce().equals("tell-cows")) {
                    im.remove();
                } else {
                    String ms = m.getPropCont().toString();
                    if (ms.startsWith("cell") && model != null) {
                        im.remove();
                        Literal p = (Literal) m.getPropCont();
                        int x = (int) ((NumberTerm) p.getTerm(0)).solve();
                        int y = (int) ((NumberTerm) p.getTerm(1)).solve();
                        if (model.inGrid(x, y)) {
                            if (p.getTerm(2).equals(aOBSTACLE)) {
                                model.add(WorldModel.OBSTACLE, x, y);
                            } else if (p.getTerm(2).equals(aENEMYCORRAL)) {
                                model.add(WorldModel.ENEMYCORRAL, x, y);
                            } else if (p.getTerm(2).equals(aSWITCH)) {
                                model.add(WorldModel.SWITCH, x, y);
                                Literal l = createLiteral("switch", createNumber(x), createNumber(y));
                                l.addSource(createAtom(m.getSender()));
                                getTS().getAg().addBel(l);
                            } else if (p.getTerm(2).toString().startsWith(aFENCE.toString())) {
                                Structure s = (Structure) p.getTerm(2);
                                if (s.getTerm(0).equals(aOPEN)) {
                                    model.add(WorldModel.OPEN_FENCE, x, y);
                                } else if (s.getTerm(0).equals(aCLOSED)) {
                                    model.add(WorldModel.CLOSED_FENCE, x, y);
                                }
                            }
                        }
                    } else if (ms.startsWith("my_status") && model != null) {
                        im.remove();
                        Literal p = Literal.parseLiteral(m.getPropCont().toString());
                        int x = (int) ((NumberTerm) p.getTerm(0)).solve();
                        int y = (int) ((NumberTerm) p.getTerm(1)).solve();
                        if (model.inGrid(x, y)) {
                            try {
                                int agid = getAgId(m.getSender());
                                model.setAgPos(agid, x, y);
                                model.incVisited(x, y);
                                Literal tAlly = createLiteral("ally_pos", new Atom(m.getSender()), createNumber(x), createNumber(y));
                                getTS().getAg().addBel(tAlly);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error checking email!", e);
            }
        }
    }

    /** returns the number of the agent based on its name (the number in the end of the name, e.g. dummie9 ==> id = 8) */
    public static int getAgId(String agName) {
        boolean nbWith2Digits = Character.isDigit(agName.charAt(agName.length() - 2));
        if (nbWith2Digits) return (Integer.parseInt(agName.substring(agName.length() - 2))) - 1; else return (Integer.parseInt(agName.substring(agName.length() - 1))) - 1;
    }

    public ICowModel getCowModel() {
        return cModel;
    }
}
