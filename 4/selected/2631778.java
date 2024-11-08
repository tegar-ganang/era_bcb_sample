package mapc2011;

import java.io.File;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import net.sf.beenuts.ap.AgentArchitecture;
import net.sf.beenuts.ap.AgentProcess;
import net.sf.beenuts.apc.util.LocalSettings;
import net.sf.beenuts.apr.MassimPerceptionChunk;
import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import com.google.common.collect.*;
import net.sf.tweety.logicprogramming.asplibrary.syntax.*;
import net.sf.tweety.logicprogramming.asplibrary.util.*;
import net.sf.tweety.logicprogramming.asplibrary.solver.*;

/**
 * a beenuts agent implementation used for devastating
 * opponents and breaking competitors dreams taking part
 * at the mapc2011.
 * 
 * @author Thomas Vengels
 *
 */
public class MAPC2011Agent extends AgentArchitecture {

    /** simstate holds information about the simulation an agent participates in */
    protected SimStartPerception simstate = null;

    /** beliefs are all facts an agent beliefs in */
    protected BeliefSet beliefs = new BeliefSet();

    /** graph is a properitary partition of all beliefs */
    protected Graph graph = null;

    /** logical programs defining an agents behavior */
    protected Map<String, Program> programs = new HashMap<String, Program>();

    protected Map<String, Set<String>> includes = new HashMap<String, Set<String>>();

    /** for now, dlv complex is fine as a solver */
    DLVComplex dlv = null;

    Clingo clingo = null;

    /** cycle counter (TODO: add this to the agent architecture */
    protected int cycleCounter = 0;

    /** debug logging (ELPs are stored on disc) */
    String logPath = null;

    /** role used by an agent */
    protected String role = null;

    /** logical programs for agent implementation */
    protected String[][] elp2load = { { "init", "./agents/init.lp.txt" }, { "step", "./agents/step.lp.txt" }, { "graph", "./agents/graph.lp.txt" }, { "explorer", "./agents/explorer.lp.txt" }, { "saboteur", "./agents/saboteur.lp.txt" }, { "generic", "./agents/generic.lp.txt" }, { "sentinel", "./agents/sentinel.lp.txt" }, { "exagent", "./agents/ex_agent.lp.txt" } };

    @Override
    public boolean init(AgentProcess agArch) {
        super.init(agArch);
        System.out.println("initializing agent");
        for (String[] l : elp2load) {
            Program p = Program.loadFrom(l[1]);
            if (p != null) {
                Program pOld = programs.put(l[0], p);
                if (pOld != null) System.err.println("warning: already loaded program: " + l[0]); else System.err.println("info: loaded program: " + l[0] + ", rules:" + p.size());
            } else {
                System.err.println("warning: could not load: " + l[0] + " from " + l[1]);
            }
        }
        for (String pname : programs.keySet()) {
            Program p = programs.get(pname);
            Set<String> includes = getIncludes(p);
            for (String inc : includes) if (!programs.keySet().contains(inc)) System.err.println("warning: cannot find include program: " + inc);
            this.includes.put(pname, includes);
        }
        LocalSettings ls = LocalSettings.getSettings();
        String dlvPath = ls.getValue("dlv");
        dlv = (dlvPath != null) ? new DLVComplex(dlvPath) : null;
        String clingoPath = ls.getValue("clingo");
        clingo = (clingo != null) ? new Clingo(clingoPath) : null;
        try {
            File f = new File("./tmp/");
            if (!f.exists()) f.mkdir();
        } catch (Exception e) {
        }
        logPath = "./tmp/" + this.getName();
        return true;
    }

    @Override
    public boolean cycle(Object perception) {
        ++cycleCounter;
        if (perception != null) {
            System.out.println("got perception\ntype: " + perception.getClass());
            System.out.println("data: " + perception.toString());
            if (perception instanceof MassimPerceptionChunk) {
                MassimPerceptionChunk per = (MassimPerceptionChunk) perception;
                Multimap<String, Percept> mmp = ArrayListMultimap.<String, Percept>create();
                for (Percept p : per.perceptions) {
                    mmp.put(p.getName(), p);
                }
                if (mmp.containsKey("simStart")) {
                    onSimulationStart(new SimStartPerception(mmp));
                } else if (mmp.containsKey("requestAction")) {
                    onSimulationStep(new SimStepPerception(mmp));
                }
            } else if (perception instanceof mapc2011.Message) {
                if (this.simstate != null) {
                    mapc2011.Message msg = (mapc2011.Message) perception;
                    if (msg.simID == simstate.simID) {
                        onCommunication(msg);
                    } else {
                        ;
                    }
                } else {
                    ;
                }
            }
        }
        return true;
    }

    @Override
    public void shutdown() {
    }

    /**
	 * dedicated java handler for simulation start events
	 * 
	 * whenever an agent receives a simulation start perception,
	 * it checks if it already participates in that simulation.
	 * then nothing happens.
	 * if it not participates, an agents beliefs are deleted and
	 * replaced by initial beliefs for a simulation.
	 * 
	 */
    public void onSimulationStart(SimStartPerception ssp) {
        System.out.println(this.getName() + " entering simulation!");
        graph = new Graph(ssp.nVertices, ssp.nEdges);
        Rule r = new Rule();
        r.addHead(new Atom("name", new StdTerm(this.getName())));
        Program p = new Program();
        p.add(r);
        p.add(this.programs.get("init"));
        p.add(ssp.toProgram());
        p.saveTo(this.getLogPath() + "_init.lp.txt");
        List<AnswerSet> asl = this.getDLV().computeModels(p, 1);
        if (asl.size() == 1) {
            System.out.println(this.getName() + " initial # beliefs: " + asl.get(0).size());
            this.beliefs = asl.get(0);
            Literal role = this.beliefs.getLiteralsBySymbol("arch_setrole").iterator().next();
            this.role = role.getAtom().getTermStr(0);
            System.out.println(this.getName() + " playing as a " + this.role);
        } else {
            System.err.println(this.getName() + ": initialization error");
        }
    }

    /**
	 * dedicated java handler for simulation end events
	 */
    public void onSimulationEnd() {
    }

    /**
	 * dedicated java handler for simulation step events
	 */
    public void onSimulationStep(SimStepPerception ssp) {
        System.out.println(this.getName() + " simulation step!");
        Graph p = ssp.getGraph(this.graph.nVertices, this.graph.nEdges);
        graph.update(p, true);
        graph.toProgram().saveTo(this.getLogPath() + this.getName() + "_graph.lp.txt");
        Program pers = ssp.toProgram();
        pers.add(assembleProgram(this.role));
        pers.add(this.beliefs.toProgram());
        pers.add(this.graph.toProgram());
        pers.saveTo(logPath + "step_rc" + cycleCounter + ".lp.txt");
        System.out.println("computing next action");
        List<AnswerSet> asl = dlv.computeModels(pers, 1);
        System.out.println("done, answer sets: " + asl.size());
        int i = 1;
        Collection<Literal> actions = null;
        AnswerSet goodAS = null;
        SolveTime st = this.getDLV().getTimings();
        if (st != null) {
            System.out.println("time (w|c|r): " + st.write / 1000000 + " | " + st.calculate / 1000000 + " | " + st.read / 1000000);
        }
        for (AnswerSet a : asl) {
            if (i == 1) goodAS = a;
            System.out.println("solution " + i + ": " + a.size() + " facts");
            a.toProgram().saveTo(logPath + "step_bels_rc" + cycleCounter + "_as" + i + ".lp.txt");
            ++i;
        }
        if (goodAS != null) {
            System.out.print("actions: ");
            for (String pred : goodAS.literals.keySet()) {
                if (pred.startsWith("do_")) {
                    System.out.print(goodAS.literals.get(pred) + ", ");
                    actions = goodAS.literals.get(pred);
                }
            }
            System.out.println("");
        }
        if (actions != null) {
            Literal action = actions.iterator().next();
            Action l = Literal2Action(action);
            this.act(l);
        } else {
            System.err.println(this.getName() + " warning: i have no action to commit!");
        }
        if (goodAS != null) {
            this.updateBeliefs(goodAS);
        }
    }

    private Action Literal2Action(Literal action) {
        String asym = action.getAtom().getSymbol().substring(3);
        if (action.getAtom().getArity() == 0) {
            return new Action(asym);
        } else {
            LinkedList<Parameter> params = new LinkedList<Parameter>();
            if (asym.equalsIgnoreCase("goto")) {
                params.add(new Identifier("vertex" + action.getAtom().getTermStr(0)));
            }
            return new Action(asym, params);
        }
    }

    /**
	 * dedicated java handler for communication requests
	 */
    public void onCommunication(mapc2011.Message msg) {
    }

    /**
	 * this method extracts the unique index from a massim vertex id
	 * 
	 * @param vertex string like "vertex1" or "vertex345"
	 * @return number suffix of vertex id
	 */
    public int vertex2int(String vertex) {
        return Integer.parseInt(vertex.substring(6));
    }

    public void updateBeliefs(AnswerSet updateSet) {
        Collection<Literal> bs_replace = updateSet.getLiteralsBySymbol("bs_replace");
        for (Literal l : bs_replace) {
            String key = l.getAtom().getTermStr(0);
            Set<Literal> values = updateSet.getLiteralsBySymbol(key);
            System.err.println("replace " + key + ":" + values.size());
            this.beliefs.replace(key, values);
        }
        Collection<Literal> bs_pos_replace = updateSet.getLiteralsBySymbol("bs_pos_replace");
        for (Literal l : bs_pos_replace) {
            String key = l.getAtom().getTermStr(0);
            Set<Literal> values = updateSet.getLiteralsBySymbol(key);
            System.err.println("pos replace " + key + ":" + values.size());
            this.beliefs.pos_replace(key, values);
        }
    }

    public String getLogPath() {
        return this.logPath;
    }

    public DLVComplex getDLV() {
        return this.dlv;
    }

    public Clingo getClingo() {
        return null;
    }

    public Set<String> getIncludes(Program p) {
        Set<String> ret = new HashSet<String>();
        Iterator<Rule> rIter = p.iterator();
        while (rIter.hasNext()) {
            Rule r = rIter.next();
            if (r.getHead() != null) if (r.getHead().size() == 1) {
                Atom a = r.getHead().get(0).getAtom();
                if (a.getSymbol().equalsIgnoreCase("include")) {
                    System.out.println(a);
                    ret.add(a.getTermStr(0));
                    rIter.remove();
                }
            }
        }
        return ret;
    }

    public Program assembleProgram(String pName) {
        Program ret = new Program();
        ret.add(this.programs.get(pName));
        Set<String> in = new HashSet<String>();
        in.add(pName);
        Set<String> missing = new HashSet<String>();
        Set<String> dep = this.includes.get(pName);
        if (dep != null) missing.addAll(dep);
        while (missing.size() > 0) {
            Iterator<String> iter = missing.iterator();
            Set<String> missingNext = new HashSet<String>();
            while (iter.hasNext()) {
                String pNext = iter.next();
                if (!in.contains(pNext)) {
                    Program p2 = this.programs.get(pNext);
                    ret.add(p2);
                    in.add(pNext);
                    missingNext.addAll(this.includes.get(pNext));
                }
            }
            missing = missingNext;
        }
        return ret;
    }
}
