package grape.frontend.model;

import grape.GraPE;
import grape.ImplementationException;
import grape.backend.ProverException;
import grape.backend.Rewrite;
import grape.frontend.model.ast.Node;
import grape.frontend.model.ast.Selection;
import grape.frontend.model.syntax.NodeType;
import grape.frontend.model.syntax.operator.Operator;
import grape.frontend.model.syntax.parser.ParseError;
import grape.frontend.model.syntax.rule.InferenceRule;
import grape.frontend.model.syntax.rule.Rule;
import grape.frontend.model.syntax.rule.TransformationRule;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

/**
 * This class handles the macroscopic state of the proof editor.
 * It stores the current inference system, derivation, selection,
 * etc. In the spirit of MVC, it should not directly access
 * the view.
 * 
 * @author Max Schaefer
 *
 */
public class MainModel {

    private static final long serialVersionUID = 1L;

    /** The inference system used by the currently active
	 *  derivation; will be <code>null</code> if the derivation is. */
    private InferenceSystem system;

    /** The root of the abstract syntax tree representing the
	 *  current derivation.  */
    private Node derivation;

    /** The currently selected part of the derivation tree;
	 *  is only <code>null</null> if there is no active derivation;
	 *  if there is no explicit selection, the whole derivation
	 *  is assumed to be selected. */
    private Selection current_selection;

    /** As long as the StepChooser dialog is open we need to store the 
	 * original selection, because each application of a rewrite step
	 * overwrites the selection with a new one.  This behaviour should 
	 * be changed in the future.*/
    private Selection original_selection;

    /** The description file of <code>system</code>. */
    private File description_file;

    /** The undo stack contains simply the nodes that were
	 *  changed; to undo a change, this node is replaced by its
	 *  conclusion (or by <code>null</code> if it is not a rule node). */
    private Stack<Node> undo_stack;

    /** the controller */
    private GraPE grape;

    public MainModel(GraPE grape) {
        this.grape = grape;
    }

    /** Initializes the model to default values. */
    public void init_model() {
        system = null;
        derivation = null;
        current_selection = null;
        description_file = null;
        undo_stack = new Stack<Node>();
    }

    /** Resets the model to default values. */
    public void reset_model() {
        derivation = null;
        current_selection = null;
        system = null;
        description_file = null;
        undo_stack.clear();
    }

    public void init_system(File f) throws DescriptionFileException, ProverException {
        system = InferenceSystem.fromFile(f);
        description_file = f;
        Map<String, String> tmp = new HashMap<String, String>();
        tmp.put("name", f.getParent());
        system.getBackend().setParam("directory", tmp);
    }

    public InferenceSystem getSystem() {
        return system;
    }

    public Node getDerivation() {
        return derivation;
    }

    public Collection<Rewrite> findRewrites(Collection<Rule> rules) throws ProverException {
        Node n = current_selection.asNode();
        return system.getBackend().findRewrites(n, rules);
    }

    /**
	 * Given a rewrite step and the current selection, update the
	 * internal derivation tree by substituting the redex by its
	 * contractum. This is the only method to directly alter the
	 * derivation tree; hence it is declared <code>synchronized</code>
	 * to avoid race conditions.
	 * <p>
	 * Currently, the combination of deep inference and branching
	 * rules is not supported; i.e., you cannot apply a branching
	 * rule deep inside a formula.
	 * <p>
	 * Also, the code is considerably more convoluted than it ought
	 * to be; the last couple of bugs all originated here.
	 * 
	 *   @param r the rewrite step to apply
	 *   @param sel the current selection
	 *   @throws ImplementationException
	 *   @throws ProverException
	 */
    public synchronized void choose(Rewrite r, Selection sel) throws ImplementationException, ProverException {
        if (r.getRule() instanceof TransformationRule) {
            Node oldDeriv = derivation.clone();
            if (sel == null) derivation = r.getResults().firstElement(); else derivation = sel.replace(derivation, r.getResults().firstElement());
            Vector<Node> tmp = new Vector<Node>();
            tmp.add(oldDeriv);
            tmp.add(derivation);
            derivation = new Node(system, r.getRule(), tmp);
            undo_stack.push(derivation);
        } else {
            Node n = r.getRedex();
            if (!(sel.getSurroundingNode().getType() instanceof Operator)) sel = new Selection(n);
            Node p;
            while ((p = n.getParent()) != null && p.getType() instanceof Operator) n = p;
            if (sel.isCompleteNode() && n == sel.getSurroundingNode()) {
                Node parent = n.getParent();
                Vector<Node> tmp = new Vector<Node>(r.getResults());
                for (int i = 0; i < tmp.size(); i++) {
                    tmp.setElementAt(tmp.get(i).clone(), i);
                }
                tmp.add(0, n);
                Node newNode = new Node(system, r.getRule(), tmp);
                if (p == null) {
                    derivation = newNode;
                } else {
                    parent.replaceChild(n, newNode);
                }
                undo_stack.push(newNode);
            } else {
                Node oldN = n.clone();
                if (r.getResults().size() != 1) throw new ImplementationException("forking rules cannot " + "be combined with deep inference yet");
                n = sel.replace(n, r.getResults().firstElement().clone());
                System.out.println("oldN : " + oldN.prettyprint());
                System.out.println("n : " + n.prettyprint());
                if (n.getParent() == null) System.out.println("parent: null"); else System.out.println("parent: " + n.getParent().prettyprint());
                Node parent = n.getParent();
                Vector<Node> tmp = new Vector<Node>();
                tmp.add(oldN);
                tmp.add(n);
                Node newNode = new Node(system, r.getRule(), tmp);
                if (p == null) derivation = newNode; else parent.replaceChild(n, newNode);
                undo_stack.push(newNode);
            }
        }
        derivation = system.getBackend().normalize(derivation);
        current_selection = new Selection(derivation);
    }

    /**
	 * Perform a proofstep, i.e. get the available rewrites, have
	 * the user select one, apply it, display the new derivation,
	 * and update the selection. This method also catches any
	 * exceptions throws by its subsidiary methods, displays
	 * a dialog, dumps a stack trace and exits.
	 * 
	 * @param rules the usable rewrite rules 
	 * @throws ProverException 
	 */
    public void do_proofstep(Collection<Rule> rules) throws ImplementationException, ProverException {
        grape.status(FrontendStatus.READING_CHOICES);
        Collection<Rewrite> choices = grape.read_choices(rules);
        grape.status(FrontendStatus.CHOOSE_NEXT_STEP);
        if (choices != null) {
            Rewrite deflt = null;
            for (Rewrite r : choices) {
                deflt = r;
                original_selection = current_selection;
                break;
            }
            if (choices.size() == 1) {
                choose(deflt, original_selection);
            } else {
                grape.init_step_chooser(choices, deflt);
            }
        }
        current_selection = new Selection(derivation);
        grape.status(FrontendStatus.PROVING);
    }

    /**
	 * Undo a proofstep. Only applications of rules count as
	 * proof steps, not (for example) commutative reorderings.
	 * To undo a rule application, we simply replace the
	 * corresponding rule node by its conclusion.
	 */
    public synchronized void undo() {
        if (undo_stack.empty()) return;
        Node last_change = undo_stack.pop();
        NodeType elt = last_change.getType();
        if (elt instanceof Operator) {
            grape.status(FrontendStatus.NO_ACTIVE_PROOF);
        } else if (elt instanceof InferenceRule) {
            Node parent = last_change.getParent();
            if (parent == null) derivation = last_change.getChild(0); else parent.replaceChild(last_change, last_change.getChild(0));
            current_selection = new Selection(derivation);
        } else {
            derivation = last_change.getChild(0);
            current_selection = new Selection(derivation);
        }
        if (derivation != null) derivation.setParent(null);
    }

    public String texport() {
        StringBuffer code = new StringBuffer();
        String eol = System.getProperty("line.separator");
        code.append("\\begin{prooftree}" + eol);
        code.append(derivation.texify());
        code.append("\\end{prooftree}" + eol);
        return code.toString();
    }

    public void discardDerivation() {
        derivation = null;
        current_selection = null;
    }

    public String getDescriptionFile() {
        return description_file == null ? "" : description_file.getAbsolutePath();
    }

    public void startBackend() throws ProverException {
        grape.status(FrontendStatus.INITIALIZING_BACKEND);
        system.start();
    }

    public void newDerivation(String f) throws ParseError, ProverException {
        String ctxt = system.getEmbedding();
        if (ctxt != null) f = ctxt.replace("#1", f);
        derivation = system.getBackend().normalize(system.getParser().parse(f, system.getLexer()));
        current_selection = new Selection(derivation);
        undo_stack.push(derivation);
        grape.status(FrontendStatus.PROVING);
    }

    public boolean undo_stack_empty() {
        return undo_stack.empty();
    }

    public void stopBackend() throws ProverException {
        system.getBackend().abort();
    }

    public void eraseSelection() {
        current_selection = new Selection(derivation);
    }

    public void newSelection(Selection selection) {
        current_selection = selection;
    }

    public Selection getOriginalSelection() {
        return original_selection;
    }
}
