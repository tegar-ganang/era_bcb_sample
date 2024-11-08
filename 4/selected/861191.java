package grape;

import grape.backend.ProverException;
import grape.backend.Rewrite;
import grape.frontend.model.DescriptionFileException;
import grape.frontend.model.FrontendStatus;
import grape.frontend.model.MainModel;
import grape.frontend.model.ast.ActiveCharacter;
import grape.frontend.model.ast.Node;
import grape.frontend.model.ast.Selection;
import grape.frontend.model.syntax.parser.ParseError;
import grape.frontend.model.syntax.rule.Rule;
import grape.frontend.view.MainView;
import grape.frontend.view.dialogs.ExceptionDialog;
import grape.frontend.view.dialogs.FormulaEditor;
import grape.frontend.view.dialogs.RuleChooser;
import grape.frontend.view.dialogs.SystemChooser;
import grape.frontend.view.dialogs.TeXportDialog;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Frontend of the proof editor. This class acts as the
 * main controller, managing the main model (see
 * {@link MainModel}) and the main view (see {@link MainView}).
 * 
 * @author Max Schaefer
 *
 */
public class GraPE implements PropertyChangeListener {

    /** current GraPE version */
    public static final String GRAPE_VERSION = "0.9.5";

    /** The model, storing the current state of the proof editor. */
    private MainModel theModel;

    /** The main view component. */
    private MainView theView;

    /** The status of the GUI/of GraPE; we cannot store the
	 * status inside theModel, since status changes entail
	 * changes to the view as well. Thus it seems more natural
	 * to handle them inside the controller. */
    private FrontendStatus status;

    /** Initializes the model */
    private void init_model() {
        theModel = new MainModel(this);
        theModel.init_model();
    }

    /** Initializes the view */
    private void init_view() {
        theView = new MainView();
        theView.init_view();
        theView.addPropertyChangeListener(this);
    }

    /**
	 * Given a system description file name, this method
	 * initializes the <code>system</code> and <code>description_file</code>
	 * fields, and sets up a {@link RuleChooser} dialog.
	 *  
	 * @param sysfn name of the description file
	 */
    private void initialize_system(File f) throws DescriptionFileException, ProverException {
        theModel.init_system(f);
        theView.init_rule_chooser(theModel.getSystem());
    }

    private void choose(Rewrite r, Selection sel) throws ImplementationException, ProverException {
        theModel.choose(r, sel);
        canUndo(true);
    }

    /**
	 * Asks the backend for possible rewrites, given the current
	 * selection and the enabled rules; see {@link grape.backend.Prover#findRewrites(Node, Collection)}.
	 * 
	 * @param rules the rules that can be used  
	 * @return the possible rewrites
	 * @throws ProverException
	 */
    public Collection<Rewrite> read_choices(Collection<Rule> rules) throws ProverException {
        Collection<Rewrite> choices;
        JFrame frame = theView.getFrame();
        try {
            choices = theModel.findRewrites(rules);
        } catch (ProverException pe) {
            ExceptionDialog.showProverError(frame, "Prover Backend Error", pe.getMessage());
            return null;
        }
        status(FrontendStatus.PROVING);
        if (choices.size() == 0) {
            JOptionPane.showMessageDialog(frame, "No inference step or transformation possible.", "Oops...", JOptionPane.ERROR_MESSAGE);
            return null;
        } else {
            return choices;
        }
    }

    private void undo() {
        theModel.undo();
        theView.display_proof(theModel.getDerivation());
    }

    /**
	 * Exports the current derivation as TeX code. It first
	 * generates the code, and then opens the {@link TeXportDialog},
	 * where the user can choose to save the code to a file
	 * or paste it into another application.
	 */
    private void texport() {
        String code = theModel.texport();
        new TeXportDialog(theView.getFrame(), code);
    }

    /**
	 * Starts a new proof.
	 * <ol>
	 *   <li> Erases the old derivation and hide the rule chooser. </li>
	 *   <li> Prompts the user for a system description file. </li>
	 *   <li> Initializes the inference system. </li>
	 *   <li> Lets the user enter a formula to prove. </li>
	 *   <li> Starts up the backend. </li>
	 *   <li> Normalizes the formula entered by the user. </li>
	 *   <li> Constructs the derivation object. </li>
	 *   <li> Initializes the current selection to contain the entire
	 *        derivation. </li>
	 *   <li> Initializes the undo stack. </li>
	 * </ol>
	 */
    private void new_proof() {
        theModel.discardDerivation();
        theView.display_proof(null);
        theView.dispose_rule_chooser();
        JFrame frame = theView.getFrame();
        String fn = theModel.getDescriptionFile();
        SystemChooser syschoose = new SystemChooser(frame, fn);
        syschoose.setVisible(true);
        File sysf = syschoose.getSystemFile();
        if (syschoose.userCanceled) {
            status(FrontendStatus.NO_ACTIVE_PROOF);
            return;
        }
        try {
            initialize_system(sysf);
        } catch (grape.frontend.model.DescriptionFileException dfe) {
            Throwable cause = dfe.getCause();
            String msg2;
            if (cause != null) {
                msg2 = cause.getMessage();
            } else {
                msg2 = "";
            }
            ExceptionDialog.showDescriptionFileError(frame, "Description File Error -- " + sysf.getName(), "The system description file could not be processed correctly.\n" + "A common cause is a syntax error in the description file. The\n" + "message below might help:\n\n" + dfe.getMessage() + "\n" + msg2);
            dfe.printStackTrace();
            return;
        } catch (ProverException pe) {
            ExceptionDialog.showProverError(frame, "Prover Backend Error", pe.getMessage());
            return;
        }
        try {
            theModel.startBackend();
        } catch (ProverException pe) {
            ExceptionDialog.showProverError(frame, "Prover Backend Error", pe.getMessage());
            status(FrontendStatus.NO_ACTIVE_PROOF);
            return;
        }
        FormulaEditor sc = new FormulaEditor(frame);
        boolean done = false;
        while (!done) {
            sc.show();
            String f = sc.getFormula();
            if (f == null) {
                status(FrontendStatus.NO_ACTIVE_PROOF);
                break;
            }
            try {
                theModel.newDerivation(f);
                done = true;
            } catch (ParseError e) {
                ExceptionDialog.showWarningDialog(frame, e);
            } catch (ProverException pe) {
                ExceptionDialog.showProverError(frame, "Prover Backend Error", pe.getMessage());
                status(FrontendStatus.NO_ACTIVE_PROOF);
            }
        }
    }

    /**
	 * Sets the frontend's current status and displays it in
	 * the status bar.
	 * @param newStatus the new status to set
	 */
    public void status(FrontendStatus newStatus) {
        status = newStatus;
        switch(newStatus) {
            case NO_ACTIVE_PROOF:
                theModel.reset_model();
                canNew(true);
                canStep(false);
                canUndo(false);
                canStop(false);
                canTexport(false);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        theView.setStatusText("No active proof");
                        theView.setNormalCursor();
                        theView.dispose_rule_chooser();
                        theView.display_proof(theModel.getDerivation());
                    }
                });
                break;
            case PROVING:
                canNew(true);
                canStep(true);
                canUndo(true);
                canStop(false);
                canTexport(true);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        theView.setStatusText("Proving");
                        theView.setNormalCursor();
                        theView.activate_rule_chooser();
                        theView.display_proof(theModel.getDerivation());
                    }
                });
                break;
            case INITIALIZING_BACKEND:
                canNew(false);
                canStep(false);
                canUndo(false);
                canStop(false);
                canTexport(false);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        theView.setStatusText("Initializing Backend");
                        theView.setBusyCursor();
                    }
                });
                break;
            case READING_CHOICES:
                canNew(false);
                canStep(false);
                canUndo(false);
                canStop(true);
                canTexport(false);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        theView.setStatusText("Reading Choices");
                        theView.deactivate_rule_chooser();
                        theView.setBusyCursor();
                    }
                });
                break;
            case STOPPING_BACKEND:
                canNew(false);
                canStep(false);
                canUndo(false);
                canTexport(false);
                canStop(false);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        theView.setStatusText("Stopping Backend");
                        theView.setBusyCursor();
                    }
                });
                break;
            case CHOOSE_NEXT_STEP:
                canNew(false);
                canStep(false);
                canUndo(false);
                canStop(false);
                canTexport(false);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        theView.setStatusText("Choose the next step.");
                        theView.deactivate_rule_chooser();
                        theView.setBusyCursor();
                    }
                });
                break;
            case CONFIRM_NEXT_STEP:
                canNew(false);
                canStep(false);
                canUndo(false);
                canStop(false);
                canTexport(false);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        theView.setStatusText("Choose the next step.");
                        theView.setBusyCursor();
                    }
                });
                break;
        }
    }

    /** Called to indicate whether a new derivation can be started. */
    private void canNew(boolean b) {
        theView.canNew(b);
    }

    /** Called to indicate whether a proofstep can be taken. */
    private void canStep(boolean b) {
        theView.canStep(b);
    }

    /** Called to indicate whether undo is possible. */
    private void canUndo(boolean b) {
        if (theModel.undo_stack_empty()) b = false;
        theView.canUndo(b);
    }

    /** Called to indicate whether the backend can be interrupted. */
    private void canStop(boolean b) {
        theView.canStop(b);
    }

    /** Called to indicate whether a TeX export is possible. */
    private void canTexport(boolean b) {
        theView.canTexport(b);
    }

    public void init_step_chooser(Collection<Rewrite> choices, Rewrite deflt) {
        theView.init_step_chooser(choices, deflt);
    }

    /**
	 * Callback method to handle property changes. Selection
	 * events and clicks on active characters are intercepted
	 * by the widget responsible for displaying an individual
	 * formula, and then propagated to the frontend as a property
	 * change event.
	 * 
	 * 	@param evt the property change event to handle
	 */
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        try {
            if (name.equals("SelectionErased")) {
                theModel.eraseSelection();
            } else if (name.equals("NewSelectionEvent")) {
                theModel.newSelection((Selection) evt.getNewValue());
            } else if (name.equals("RollbackChoice")) {
                if (status == FrontendStatus.CONFIRM_NEXT_STEP) undo();
            } else if (name.equals("NewProof")) {
                new_proof();
            } else if (name.equals("ActiveCharacter")) {
                ActiveCharacter ac = (ActiveCharacter) evt.getNewValue();
                ac.trigger();
                theView.display_proof(theModel.getDerivation());
            } else if (name.equals("PreviewRewrite")) {
                Rewrite r = (Rewrite) evt.getNewValue();
                if (status == FrontendStatus.CONFIRM_NEXT_STEP) {
                    System.out.println("undoing");
                    undo();
                }
                if (status == FrontendStatus.CHOOSE_NEXT_STEP) status(FrontendStatus.CONFIRM_NEXT_STEP);
                choose(r, theModel.getOriginalSelection());
                theView.display_proof(theModel.getDerivation());
            } else if (name.equals("DoProofStep")) {
                theModel.do_proofstep(theView.getEnabledRules());
            } else if (name.equals("ApplyRule")) {
                Rule r = (Rule) evt.getNewValue();
                Vector<Rule> tmp = new Vector<Rule>(1);
                tmp.add(r);
                theModel.do_proofstep(tmp);
            } else if (name.equals("Undo")) {
                undo();
            } else if (name.equals("TeXport")) {
                texport();
            } else if (name.equals("Stop")) {
                status(FrontendStatus.STOPPING_BACKEND);
                theModel.stopBackend();
                status(FrontendStatus.PROVING);
            } else if (name.equals("Exit")) {
                System.exit(0);
            } else {
            }
        } catch (Exception e) {
            ExceptionDialog.showExceptionDialog(theView.getFrame(), e);
        }
    }

    /**
	 * The main method initializes the model, displays
	 * the GUI, and prompts the user to start a new proof.
	 * 
	 * @param args command line parameters; not currently used
	 */
    public static void main(String[] args) {
        final GraPE app = new GraPE();
        app.init_model();
        app.init_view();
        app.new_proof();
    }
}
