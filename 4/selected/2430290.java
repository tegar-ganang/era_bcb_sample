package org.tzi.ugt.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.SortedMap;
import org.tzi.ugt.model.CollaborationDiagram;
import org.tzi.ugt.model.HostSystemState;
import org.tzi.ugt.model.Model;
import org.tzi.ugt.model.ModelFactory;
import org.tzi.ugt.model.StatechartDiagram;
import org.tzi.ugt.model.UseCaseDiagram;
import org.tzi.ugt.model.predefined.Predefined;
import org.tzi.ugt.parser.UGTCompiler;
import org.tzi.ugt.ss2.Rule;
import org.tzi.ugt.ss2.TU;
import org.tzi.use.uml.ocl.value.VarBindings;
import systemStates.Grammar;
import systemStates.exceptions.RuleNotApplicableException;
import systemStates.exceptions.TUNotApplicableException;
import systemStates.exceptions.UnknownRuleException;

/**
 * Main class of UGT
 * 
 * @author lschaps
 */
public class Main extends Observable {

    private static final String filename = "example.ugt";

    private File m_File;

    private Grammar m_Grammar;

    private Model m_Model;

    private SortedMap m_Rules;

    /**
	 * Creates the main class of the application.
	 */
    public Main() {
        m_Model = null;
        m_Grammar = null;
        m_File = null;
    }

    /**
	 * The main method (for testing purposes)
	 * 
	 * @param args
	 *            Arguments are not evalueted at this moment.
	 */
    public static void main(String[] args) {
        Main app = new Main();
        app.open(new File(filename));
    }

    /**
	 * Applies a rule on the model
	 * 
	 * @param in_Rule
	 *            The rule to be applied.
	 * 
	 * @return True, when the rule was successfully applied.
	 * 
	 * @throws Exception
	 */
    public boolean applyRule(Rule in_Rule) throws Exception {
        try {
            System.out.println("Try to apply rule " + in_Rule.getName() + ".");
            m_Grammar.apply(in_Rule.getRule());
            System.out.println("Rule applied.");
            this.setChanged();
            this.notifyObservers();
        } catch (RuleNotApplicableException e) {
            System.out.println("Rule not applicable");
            throw new Exception();
        } catch (java.lang.Exception e) {
            System.out.println("Rule not applicable");
            throw new Exception();
        }
        return true;
    }

    /**
	 * Applies a rule on the model
	 * 
	 * @param in_Rule
	 *            The rule to be applied.
	 * @param in_VB
	 *            The varbindings for the application.
	 * 
	 * @return True, when the rule was successfully applied.
	 * @throws Exception
	 */
    public boolean applyRule(Rule in_Rule, VarBindings in_VB) throws Exception {
        try {
            System.out.println("Try to apply rule " + in_Rule.getName() + ".");
            m_Grammar.apply(in_Rule.getRule(), in_VB);
            System.out.println("Rule applied.");
            this.setChanged();
            this.notifyObservers();
            return true;
        } catch (RuleNotApplicableException e) {
            System.out.println("Rule not applicable");
            throw new Exception();
        }
    }

    /**
	 * Applies a rule on the model
	 * 
	 * @param in_TU
	 *            The rule to be applied.
	 * 
	 * @return True, when the rule was successfully applied.
	 * @throws Exception
	 */
    public boolean applyTU(TU in_TU) throws Exception {
        try {
            System.out.println("Try to apply TU " + in_TU.getName() + ".");
            m_Grammar.apply(in_TU.getTU());
            System.out.println("TU applied.");
            this.setChanged();
            this.notifyObservers();
        } catch (TUNotApplicableException e) {
            System.out.println("TU not applicable");
            throw new Exception();
        }
        return true;
    }

    /**
	 * Closes the actual model.
	 */
    public void close() {
        if (isActive()) {
            m_Grammar = null;
            m_File = null;
            m_Model = null;
            this.setChanged();
            this.notifyObservers();
        }
    }

    /**
	 * Returns the collaborations.
	 * 
	 * @return The collaborations.
	 */
    public Collection getCollaborations() {
        return m_Model.getCollaborations();
    }

    /**
	 * Returns the statecharts.
	 * 
	 * @return The statecharts.
	 */
    public Collection getStatecharts() {
        return m_Model.getStatecharts();
    }

    /**
	 * Returns the invariants.
	 * 
	 * @return The invariants.
	 */
    public Collection getInvariants() {
        return m_Model.getClassInvariants();
    }

    /**
	 * Returns the model.
	 * 
	 * @return Returns the model.
	 */
    public Model getModel() {
        return m_Model;
    }

    /**
	 * Returns the predefined rules.
	 * 
	 * @return The predefiend rules.
	 */
    public Predefined getPredefined() {
        return m_Model.getPredefined();
    }

    /**
	 * Returns the host system state of the application.
	 * 
	 * @return The agg host systemstate.
	 */
    public systemStates.HostSystemState getSystemState() {
        return m_Grammar.getSystemState();
    }

    /**
	 * Returns the use case diagramm.
	 * 
	 * @return The use case diagram.
	 */
    public UseCaseDiagram getUseCaseDiagram() {
        return m_Model.getUseCase();
    }

    /**
	 * Returns true if there is a actual model.
	 * 
	 * @return True, if there is a actual model.
	 */
    public boolean isActive() {
        if (null == m_Grammar) {
            return false;
        } else {
            return true;
        }
    }

    /**
	 * Opens the file and processes it.
	 * 
	 * @param in_File
	 *            The file.
	 */
    public void open(File in_File) {
        try {
            FileReader fileReader = new FileReader(in_File);
            startApp(fileReader, in_File.getName());
            m_File = in_File;
            this.getSystemState().useSystem.state().checkStructure(new PrintWriter(System.out));
            this.setChanged();
            this.notifyObservers();
        } catch (FileNotFoundException e) {
            System.out.println("File exception");
            m_File = null;
        }
    }

    /**
	 * Saves the actual model as agg file.
	 * 
	 * @param in_FileName
	 *            The name of the file.
	 */
    public void save(String in_FileName) {
        m_Grammar.save(in_FileName);
    }

    /**
	 * Starts the application with the default file (for testing).
	 */
    public void start() {
        this.open(new File(filename));
    }

    /**
	 * Tries to applya rule.
	 * 
	 * @return True, when a rule was applied.
	 */
    public boolean step() {
        boolean ruleApplicated = false;
        Iterator it;
        it = this.getPredefined().getRules().values().iterator();
        while (it.hasNext() && !ruleApplicated) {
            Rule rule = (Rule) it.next();
            try {
                m_Grammar.apply(rule.getRule());
                ruleApplicated = true;
            } catch (TUNotApplicableException e) {
                ruleApplicated = false;
            } catch (UnknownRuleException e) {
                ruleApplicated = false;
            } catch (java.lang.Exception e) {
                ruleApplicated = false;
            }
        }
        it = this.getPredefined().getTUs().values().iterator();
        while (it.hasNext() && !ruleApplicated) {
            TU tu = (TU) it.next();
            try {
                m_Grammar.apply(tu.getTU());
                ruleApplicated = true;
            } catch (TUNotApplicableException e) {
                ruleApplicated = false;
            } catch (UnknownRuleException e) {
                ruleApplicated = false;
            } catch (java.lang.Exception e) {
                ruleApplicated = false;
            }
        }
        it = this.getStatecharts().iterator();
        while (it.hasNext() && !ruleApplicated) {
            StatechartDiagram sc = (StatechartDiagram) it.next();
            Iterator it2 = sc.getRules().iterator();
            while (it2.hasNext() && !ruleApplicated) {
                Rule rule = (Rule) it2.next();
                try {
                    m_Grammar.apply(rule.getRule());
                    ruleApplicated = true;
                } catch (RuleNotApplicableException e) {
                    ruleApplicated = false;
                } catch (java.lang.Exception e) {
                    ruleApplicated = false;
                }
            }
        }
        it = this.getCollaborations().iterator();
        while (it.hasNext() && !ruleApplicated) {
            CollaborationDiagram cd = (CollaborationDiagram) it.next();
            if (!cd.definesUseCase()) {
                Iterator it2 = cd.getRules().iterator();
                while (it2.hasNext() && !ruleApplicated) {
                    Rule rule = (Rule) it2.next();
                    try {
                        m_Grammar.apply(rule.getRule());
                        ruleApplicated = true;
                    } catch (RuleNotApplicableException e) {
                        ruleApplicated = false;
                    } catch (java.lang.Exception e) {
                        ruleApplicated = false;
                    }
                }
            }
        }
        if (true == ruleApplicated) {
            this.setChanged();
            this.notifyObservers();
        }
        return ruleApplicated;
    }

    /**
	 * Generate the SystemStates for AGG
	 * 
	 * @param in_Model
	 *            Model generated by the parser from the input file.
	 * 
	 * @throws Exception
	 *             Exception is thrown in case of incomplete metamodel in_Model.
	 */
    private void generateSS(Model in_Model) throws Exception {
        m_Grammar = new Grammar(in_Model.name(), in_Model.getMModel());
        HostSystemState.gen(in_Model, m_Grammar);
        m_Rules = in_Model.generateRules(m_Grammar);
    }

    /**
	 * Starts the application.
	 * 
	 * @param in_FileReader
	 *            The filereader.
	 * @param in_Filename
	 *            TODO
	 */
    private void startApp(FileReader in_FileReader, String in_Filename) {
        UGTCompiler comp = new UGTCompiler();
        PrintWriter pwriter = new PrintWriter(System.out, true);
        FileReader filereader = null;
        if (null == in_FileReader) {
            try {
                filereader = new FileReader(filename);
                in_Filename = filename;
            } catch (FileNotFoundException e) {
                System.err.println("File exception");
                System.exit(0);
            }
        } else {
            filereader = in_FileReader;
        }
        m_Model = comp.compileSpecification(filereader, in_Filename, pwriter, new ModelFactory());
        try {
            generateSS(m_Model);
        } catch (Exception e1) {
            System.err.println(e1.getMessage());
            e1.printStackTrace();
        }
    }

    /**
	 * Returns the rules.
	 * 
	 * @return Returns the rules sorted by name.
	 */
    public SortedMap getRules() {
        return m_Rules;
    }

    /**
	 * Returns the grammar.
	 * 
	 * @return Returns the grammar.
	 */
    public Grammar getGrammar() {
        return m_Grammar;
    }

    /**
	 * Applies a rule by name.
	 * 
	 * @param in_rulename
	 *            The name of the rule.
	 */
    public void applyByName(String in_rulename) {
        boolean ruleApplied = false;
        SortedMap map = m_Rules.tailMap(in_rulename).headMap(in_rulename + Character.MAX_VALUE);
        Iterator it = map.values().iterator();
        while (it.hasNext() && !ruleApplied) {
            try {
                Object o = it.next();
                if (o instanceof Rule) {
                    Rule rule = (Rule) o;
                    if (!rule.needsParameter()) {
                        applyRule((Rule) o);
                    } else {
                        System.out.println("Rules needs parameters");
                    }
                } else if (o instanceof TU) {
                    applyTU((TU) o);
                }
                ruleApplied = true;
            } catch (Exception e1) {
            }
        }
    }
}
