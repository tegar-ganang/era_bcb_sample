package machine;

import java.awt.Point;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import main.Constants;
import main.Constants.Zoom;
import main.Constants.ZoomMode;
import modules.machineTree.UndoRedoInt;
import exceptions.IllegalParameterException;
import exceptions.InvalidNameException;
import exceptions.ObjectAlreadyInsertedException;
import exceptions.ParsingException;
import exceptions.UnknownOvalException;
import exceptions.UnknownTapeException;
import exceptions.UnknownTransitionException;

/**
 * An instance of this class stores the model and the visual representation of a
 * turingmachine.
 * 
 * @author Sven Schneider
 * @since 0.1.1
 */
public class Turingmachine {

    /**
	 * The user decides to zoom.
	 * 
	 * @param z
	 *            The zoom-mode.
	 */
    public void zoom(Zoom z) {
        if (z == Zoom.ZOOM_IN) {
            switch(zoom) {
                case ZOOM_100:
                    zoom = ZoomMode.ZOOM_200;
                    break;
                case ZOOM_200:
                    zoom = ZoomMode.ZOOM_400;
                    break;
                case ZOOM_400:
                    zoom = ZoomMode.ZOOM_800;
                    break;
                case ZOOM_800:
                    break;
            }
            System.out.println("in");
        } else {
            switch(zoom) {
                case ZOOM_100:
                    break;
                case ZOOM_200:
                    zoom = ZoomMode.ZOOM_100;
                    break;
                case ZOOM_400:
                    zoom = ZoomMode.ZOOM_200;
                    break;
                case ZOOM_800:
                    zoom = ZoomMode.ZOOM_400;
                    break;
            }
            System.out.println("out");
        }
    }

    /** Every object tape/transition/oval etc has an int-id. */
    private static int nextID = 0;

    /** The current zoom-factor. */
    private ZoomMode zoom = ZoomMode.ZOOM_100;

    /**
	 * A turingmachine has a description, i.e. of what the user think it is
	 * doing.
	 */
    private String description = "<empty description>";

    /** The path where the machine is stored. */
    private String path = "default.new.xml";

    /** The vector stores all states that the machine containes. */
    private Vector<State> states = new Vector<State>();

    /** The vector stores all submachines that the machine contains. */
    private Vector<Submachine> submachines = new Vector<Submachine>();

    /** The vector stores all symbols that the machine contains. */
    private Vector<Symbol> symbols = new Vector<Symbol>();

    /** The vector stores all tapes that the machine contains. */
    private Vector<Tape> tapes = new Vector<Tape>();

    /** The vector stores all transitions that the machine contains. */
    private Vector<Transition> transitions = new Vector<Transition>();

    /**
	 * if <code>true</code> any operation performed is sent to the
	 * <code>UndoRedoInt</code>.
	 * <p>
	 * if <code>false</code> no operation performed is sent to the
	 * <code>UndoRedoInt</code>.
	 */
    private boolean undoEnabled = true;

    /** this interface allows it to undo/redo the operation that were performed. */
    private UndoRedoInt undoRedo = null;

    /**
	 * Constructor setting the undoredo-interface the path of the machine and
	 * eventually creating a tape.
	 * 
	 * @param pUndoRedo
	 *            the interface that the machine will use to perform undo/redo
	 *            operations
	 * @param pPath
	 *            the path where the machine will be stored at/loaded from
	 * @param createTape
	 *            <code>true</code> a standardtape will be added<br>
	 *            <code>false</code> no tape is created, i.e. if the machine
	 *            is supposed to be loaded from a file
	 * @see Tape#Tape(Turingmachine, int, int, String)
	 * @throws InvalidTapeDescriptionException
	 *             see Tape#Tape
	 * @throws IllegalParameterException
	 *             see Tape#Tape
	 */
    public Turingmachine(UndoRedoInt pUndoRedo, String pPath, boolean createTape) throws InvalidNameException, IllegalParameterException {
        path = pPath;
        undoRedo = pUndoRedo;
        diableUndo();
        if (createTape) addTape("unnamed");
        enableUndo();
    }

    /**
	 * An oval is added to the machine.
	 * 
	 * @param st
	 *            the oval to be inserted.
	 * @see #addState(State)
	 * @see #addSubmachine(Submachine)
	 * @throws ObjectAlreadyInsertedException
	 *             see #addState/#addSubmachine
	 */
    protected void addOval(SelectableOval st) throws ObjectAlreadyInsertedException {
        if (st.getClass() == State.class) addState((State) st); else if (st.getClass() == Submachine.class) addSubmachine((Submachine) st);
    }

    /**
	 * A state is added to the machine.
	 * 
	 * @param st
	 *            the state to be inserted.
	 * @throws ObjectAlreadyInsertedException
	 *             a state with the same id is already inserted
	 */
    protected void addState(State st) throws ObjectAlreadyInsertedException {
        if (isState(st)) throw new ObjectAlreadyInsertedException(Turingmachine.class.getCanonicalName() + " / the passed state is already part of this turingmachine  / not inserting state");
        states.add(st);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_ADD, new Object[] { st }));
    }

    /**
	 * A state is added to the machine.
	 * 
	 * @param sn
	 *            the name of the new state.
	 * @param st
	 *            the type of the new state.
	 * @param pos
	 *            the position of the new state.
	 * @return the id of the inserted state
	 * @see State#State(StateType, String, int, Point)
	 * @throws InvalidNameException
	 *             see State#State
	 */
    public int addState(String sn, StateType st, Point pos) throws InvalidNameException {
        State s = new State(st, sn, nextID++, pos);
        states.add(s);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_ADD, new Object[] { s }));
        return nextID - 1;
    }

    /**
	 * A submachine is added to the machine.
	 * 
	 * @param pName
	 *            the name of the new submachine.
	 * @param pTM
	 *            the turingmachine the submachine links to.
	 * @param pPos
	 *            the position of the new submachine.
	 * @return the id of the inserted submachine
	 * @see Submachine#Submachine(String, int, Point, Turingmachine)
	 * @throws InvalidNameException
	 *             see Submachine#Submachine
	 */
    public int addSubmachine(String pName, Point pPos, Turingmachine pTM) throws InvalidNameException {
        Submachine sm = new Submachine(pName, nextID++, pPos, pTM);
        submachines.add(sm);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_ADD, new Object[] { sm }));
        return nextID - 1;
    }

    /**
	 * A submachine is added to the machine.
	 * 
	 * @param sm
	 *            the submachine to be inserted.
	 * @throws ObjectAlreadyInsertedException
	 *             a submachine with the same id is already inserted
	 */
    protected void addSubmachine(Submachine sm) throws ObjectAlreadyInsertedException {
        if (isSubmachine(sm)) throw new ObjectAlreadyInsertedException(Turingmachine.class.getCanonicalName() + " / the passed submachine is already part of this turingmachine  / not inserting ssubmachine");
        submachines.add(sm);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_ADD, new Object[] { sm }));
    }

    /**
	 * A symbol is added to the machine.
	 * 
	 * @param symb
	 *            the symbol to be inserted.
	 */
    protected void addSymbol(Symbol symb) {
        symbols.add(symb);
    }

    /**
	 * Multiple symbols are added to the machine.
	 * <p>
	 * BNF of expression:<br>
	 * &lt;expr&gt; ::= &lt;allowedChars&gt; | &lt;allowedChars&gt;&lt;expr&gt; |
	 * "["&lt;inside&gt;"]" | "["&lt;inside&gt;"]"&lt;expr&gt;<br>
	 * &lt;inside&gt; ::= &lt;allowedChars&gt; |
	 * &lt;allowedChars&gt;&lt;inside&gt;<br>
	 * &lt;allowedChars&gt; ::= ALLOWED_SYMBOL_CHARS
	 * 
	 * @see main.Constants#ALLOWED_SYMBOL_CHARS
	 * @param sn
	 *            the expression representing the new symbols.
	 * @see Symbol#Symbol(String)
	 * @throws InvalidNameException
	 *             see Symbol#Symbol
	 * @see #parseBracketExpression(String)
	 * @throws ParsingException
	 *             see #parseBracketExpression
	 */
    public void addSymbols(String sn) throws InvalidNameException, ParsingException {
        Vector<String> newSymbs = parseBracketExpression(sn);
        for (String it : newSymbs) if (getMatchingSymbols(it, false, false) == null) {
            Symbol symb = new Symbol(it);
            symbols.add(symb);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.SYMBOL_ADD, new Object[] { symb }));
        }
    }

    /**
	 * A tape is added to the machine.
	 * 
	 * @param descr
	 *            the description of the new tape.
	 * @return the inserted tape.
	 * @see Tape#Tape(Turingmachine, int, int, String)
	 * @throws InvalidTapeDescriptionException
	 *             see Tape#Tape
	 * @throws IllegalParameterException
	 *             see Tape#Tape
	 */
    public Tape addTape(String descr) throws InvalidNameException, IllegalParameterException {
        Tape np = new Tape(this, nextID++, tapes.size(), descr);
        tapes.add(np);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_ADD, new Object[] { np }));
        return np;
    }

    /**
	 * A tape is added to the machine.
	 * 
	 * @param ta
	 *            the tape to be inserted.
	 * @throws ObjectAlreadyInsertedException
	 *             A tape with the same id is already in the machine.
	 */
    protected void addTape(Tape ta) throws ObjectAlreadyInsertedException {
        if (isTape(ta)) throw new ObjectAlreadyInsertedException(Turingmachine.class.getCanonicalName() + " / the passed tape is already part of this turingmachine / not inserting tape");
        tapes.add(ta);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_ADD, new Object[] { ta }));
    }

    /**
	 * A transition is added to the machine.
	 * 
	 * @param pStart
	 *            the oval the transition starts in.
	 * @param pStartName
	 *            the name of the state the transition starts in, in case the
	 *            pStart is a submachine.
	 * @param pEnd
	 *            the oval the transition ends in.
	 * @param pEndName
	 *            the name of the state the transition ends in, in case the pEnd
	 *            is a submachine.
	 * @return the inserted transition.
	 * @see Transition#Transition(SelectableOval, String, SelectableOval,
	 *      String, Vector, Vector, Point, Turingmachine)
	 * @throws IllegalParameterException
	 *             see Transition#Transition
	 * @throws UnknownTapeException
	 *             see Transition#Transition
	 * @throws UnknownTransitionException
	 *             see Transition#Transition
	 * @throws UnknownOvalException
	 *             thrown if on of the selected ovals is not part of the machine<br>
	 *             or if the strings do not match ovals in their appropriate
	 *             submachines.
	 */
    public Transition addTransition(SelectableOval pStart, String pStartName, SelectableOval pEnd, String pEndName) throws IllegalParameterException, UnknownTapeException, UnknownTransitionException, UnknownOvalException {
        if (!isOval(pStart) || !isOval(pEnd)) throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / at least one of the passed ovals is not part of this machine / not adding transition");
        Transition trans = new Transition(pStart, pStartName, pEnd, pEndName, null, new Vector<TransitionOpns>(), null, this);
        transitions.add(trans);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_ADD, new Object[] { trans }));
        return trans;
    }

    /**
	 * A transition is added to the machine.
	 * 
	 * @param tr
	 *            the transition to be inserted.
	 * @throws ObjectAlreadyInsertedException
	 *             a transition with the same id is already inserted
	 */
    protected void addTransition(Transition tr) throws ObjectAlreadyInsertedException {
        if (isTransition(tr)) throw new ObjectAlreadyInsertedException(Turingmachine.class.getCanonicalName() + " / the passed transition is already part of this turingmachine / not inserting transition");
        transitions.add(tr);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_ADD, new Object[] { tr }));
    }

    /**
	 * An operation is added to a transition of the machine.
	 * 
	 * @param tran
	 *            the transition the operation will belong to
	 * @param tape
	 *            the tape the operation is using
	 * @param hm
	 *            the HeadMove performed by the operation
	 * @param read
	 *            the symbol read by the operation
	 * @param write
	 *            the symbol written by the operation
	 * @see HeadMove#HeadMove(direction)
	 * @see Transition#addOpns(TransitionOpns)
	 * @throws IllegalParameterException
	 *             see HeadMove#HeadMove(int)<br>
	 *             see Transition#addOpns(TransitionOpns)
	 * @throws UnknownTapeException
	 *             see Transition#addOpns(TransitionOpns)
	 * @throws UnknownTransitionException
	 *             see Transition#addOpns(TransitionOpns)
	 */
    public void addTransitionOpns(Transition tran, Tape tape, HeadMove hm, Symbol read, Symbol write) throws IllegalParameterException, UnknownTapeException, UnknownTransitionException {
        if (tran == null || tape == null || hm == null) return;
        TransitionOpns to = new TransitionOpns(new HeadMove(hm.getMove()), read, write, tape);
        tran.addOpns(to);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_ADD_OPNS, new Object[] { to, tran }));
    }

    /**
	 * All symbols stored on the tape are removed.
	 * <p>
	 * Afterwards all positions on the tape are BLANK.
	 * 
	 * @param t
	 *            the tape to remove all symbols from
	 * @throws UnknownTapeException
	 *             the tape is not part of this machine
	 */
    public void clearTape(Tape t) throws UnknownTapeException {
        if (isTape(t)) {
            HashMap old = t.getContent();
            t.clearTape();
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_CLEAR, new Object[] { old, t }));
        } else throw new UnknownTapeException(Turingmachine.class.getCanonicalName() + " / the passed tape is not part of this turingmachine / not clearing tape");
    }

    /**
	 * Checks if a Transition contains a specified TransitionDot.
	 * 
	 * @param trans
	 *            the transition to check
	 * @param dot
	 *            the TransitionDot that might be in the Transition
	 * @return <code>true</code>/<code>false</code> if dot is/is not in
	 *         trans
	 */
    private static boolean containsDot(Transition trans, TransitionDot dot) {
        Collection<TransitionDot> dots = trans.getDots();
        if (trans.getTextAnchor() == dot) return true;
        for (TransitionDot it : dots) if (it == dot) return true;
        return false;
    }

    /**
	 * No operations applied to the machine will be send to the
	 * undoredointerface.
	 */
    protected void diableUndo() {
        undoEnabled = false;
    }

    /**
	 * All operations applied to the machine will be send to the
	 * undoredointerface.
	 */
    protected void enableUndo() {
        undoEnabled = true;
    }

    /**
	 * Getter.
	 * 
	 * @return the description of the turingmachine.
	 */
    public String getDescription() {
        return description;
    }

    /**
	 * Getter.
	 * 
	 * @return all states that are accepting states.
	 */
    public Vector<State> getEndStates() {
        Vector<State> res = new Vector<State>();
        for (State it : states) if (it.getType().isEndState()) res.add(it);
        return res;
    }

    /**
	 * Returns all symbols from the current alphabt that match the expression.
	 * 
	 * @param text
	 *            the expression
	 * @return the symbols matching the expression
	 * @see #parseBracketExpression(String)
	 * @throws ParsingException
	 *             see #parseBracketExpression
	 */
    public Vector<String> getMatchingSymols(String text) throws ParsingException {
        if (text.length() == 0) return new Vector<String>();
        boolean front = false;
        boolean back = false;
        if (text.charAt(0) == '*') {
            front = true;
            text = text.substring(1);
        }
        if (text.length() > 0 && text.charAt(text.length() - 1) == '*') {
            back = true;
            text = text.substring(0, text.length() - 1);
        }
        Vector<String> resTmp = parseBracketExpression(text);
        Vector<String> res = new Vector<String>();
        for (String it : resTmp) {
            Vector<String> symb = getMatchingSymbols(it, front, back);
            if (symb != null) res.addAll(symb);
        }
        return res;
    }

    /**
	 * Returns the oval with the specified id.
	 * 
	 * @param id
	 *            the id of the object that is requested.
	 * @return the oval that has the passed id or null if there is no such oval.
	 */
    public SelectableOval getOval(int id) {
        for (SelectableOval it : states) if (it.getID() == id) return it;
        for (SelectableOval it : submachines) if (it.getID() == id) return it;
        return null;
    }

    /**
	 * Returns an oval that is close to the point p.
	 * <p>
	 * that means that p is inside the ellipse of the oval.
	 * 
	 * @param p
	 *            the point that is/is not inside the ellipse of an oval
	 * @return an oval that is selected / <code>null</code> if no oval was
	 *         selected the oval
	 */
    public SelectableOval getOval(Point p) {
        for (SelectableOval it : states) if (isOvalSelected(it.getPosition(), p)) return it;
        for (SelectableOval it : submachines) if (isOvalSelected(it.getPosition(), p)) return it;
        return null;
    }

    /**
	 * Gettter.
	 * 
	 * @return a vector of all the ovals (states & submachines) that are part of
	 *         the mchine
	 */
    public Vector<SelectableOval> getOvals() {
        Vector<SelectableOval> vec = new Vector<SelectableOval>();
        vec.addAll(states);
        vec.addAll(submachines);
        return vec;
    }

    /**
	 * Getter.
	 * 
	 * @return the path where the machine is stored.
	 */
    public String getPath() {
        return path;
    }

    /**
	 * The machine is analized and all transitions that could be used from the
	 * passed State s are returned.
	 * 
	 * @param s
	 *            the state the machine is currently in
	 * @return a vector of transitions the machine can now use (is a single one
	 *         if the machine would be deterministic)
	 */
    public Vector<Transition> getPossibleTransitions(State s) {
        s.equals(s);
        return null;
    }

    /**
	 * Getter.
	 * 
	 * @return all states that are start states.
	 */
    public Vector<State> getStartStates() {
        Vector<State> res = new Vector<State>();
        for (State it : states) if (it.getType().isStartState()) res.add(it);
        return res;
    }

    /**
	 * Protected getter.
	 * 
	 * @return an unmodifiable collection of the states the machine contains
	 */
    public Collection<State> getStates() {
        return Collections.unmodifiableCollection(states);
    }

    /**
	 * Protected getter.
	 * 
	 * @return an unmodifiable collection of the submachine the machine contains
	 */
    public Collection<Submachine> getSubmachines() {
        return Collections.unmodifiableCollection(submachines);
    }

    /**
	 * Getter.
	 * 
	 * @param symb
	 *            the string symbol
	 * @return the symbol that has the string symbol <code>symb</code>.<br>
	 *         in case symb is the BLANK/EPSILON string the appropriate symbol
	 *         is returned.
	 * @see Symbol#Symbol(String)
	 * @see main.Constants#BLANK
	 * @see main.Constants#EPSILON
	 * @throws IllegalParameterException
	 *             see Symbol#Symbol
	 * 
	 */
    public Symbol getSymbol(String symb) throws IllegalParameterException {
        if (symb.equals(Constants.BLANK)) return new Symbol(Symbol.symbolType.BLANK);
        if (symb.equals(Constants.EPSILON)) return new Symbol(Symbol.symbolType.EPSILON);
        for (Symbol it : symbols) if (it.getSymbol().equals(symb)) return it;
        return null;
    }

    /**
	 * Getter.
	 * 
	 * @return the list of symbols in their string-representation.
	 */
    public Vector<String> getSymbols() {
        Vector<String> res = new Vector<String>();
        for (Symbol it : symbols) res.add(it.getSymbol());
        return res;
    }

    /**
	 * Getter.
	 * 
	 * @param str
	 *            the string representation of the tape to find
	 * @return the tape that string-representation matches str
	 */
    public Tape getTape(String str) {
        for (Tape it : tapes) if (it.toString().equals(str)) return it;
        return null;
    }

    /**
	 * Getter.
	 * 
	 * @param i
	 *            the id of the tape to find
	 * @return the tape that id matches i
	 */
    public Tape getTapeByID(int i) {
        for (Tape it : tapes) if (it.getID() == i) return it;
        return null;
    }

    /**
	 * Getter.
	 * 
	 * @param i
	 *            the tapenumber of the tape to find
	 * @return the tape that tapenumber matches i
	 */
    public Tape getTapeByTapeNumber(int i) {
        for (Tape it : tapes) if (it.getTapeNumber() == i) return it;
        return null;
    }

    /**
	 * Getter.
	 * 
	 * @return an unmodifiable collection of the tapes the machine contains
	 */
    public Collection<Tape> getTapes() {
        return Collections.unmodifiableCollection(tapes);
    }

    /**
	 * Finds transitions that are bound to a certain oval.
	 * 
	 * @param s
	 *            the oval the transition starts/ends in.
	 * @param in
	 *            the transitions returned are starting in s
	 * @param out
	 *            the transitions returned are ending in s
	 * @return the vector containing all transitions that are starting in s (if
	 *         in is true) and alle transitions that are ending in s (if out is
	 *         true).
	 * @throws UnknownOvalException
	 *             the passed oval s is not part of the machine
	 */
    public Vector<Transition> getTrans(SelectableOval s, boolean in, boolean out) throws UnknownOvalException {
        Vector<Transition> res = new Vector<Transition>();
        if (s.getClass().equals(State.class) && isState((State) s) || s.getClass().equals(Submachine.class) && isSubmachine((Submachine) s)) {
            for (Transition it : transitions) if (out && it.getStart() == s) res.add(it); else if (in && it.getEnd() == s) res.add(it);
        } else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed oval is not part of this turingmachine  / not removing any transition");
        return res;
    }

    /**
	 * Returns the transition that is selected via one of its dots.
	 * 
	 * @param p
	 *            the point that has to be close enough to one of the
	 *            transitions dots.
	 * @return the selected transition or null if no transition has been
	 *         selected.
	 */
    public Transition getTransition(Point p) {
        for (Transition it : transitions) {
            Collection<TransitionDot> dots = it.getDots();
            for (TransitionDot it2 : dots) if (isTransitionDotSelected(it2.getPosition(), p)) return it;
        }
        return null;
    }

    /**
	 * Returns the transition the passed dot belongs to.
	 * 
	 * @param dot
	 *            one transitionDot that is part of the transition returned.
	 * @return the transisition that contains the transitionDot dot.
	 */
    public Transition getTransition(TransitionDot dot) {
        Collection<TransitionDot> dots = null;
        for (Transition it : transitions) {
            dots = it.getDots();
            if (dot == it.getTextAnchor()) return it;
            for (TransitionDot it2 : dots) if (it2 == dot) return it;
        }
        return null;
    }

    /**
	 * Getter.
	 * 
	 * @param p
	 *            a point the user selected
	 * @return the transitionDot of any transition that is close enough to the
	 *         point p.
	 */
    public TransitionDot getTransitionDot(Point p) {
        Collection<TransitionDot> dots = null;
        for (Transition it : transitions) {
            dots = it.getDots();
            if (isTransitionDotSelected(it.getTextAnchor().getPosition(), p)) return it.getTextAnchor();
            for (TransitionDot it2 : dots) if (isTransitionDotSelected(it2.getPosition(), p)) return it2;
        }
        return null;
    }

    /**
	 * Getter.
	 * 
	 * @return an unmodifiable collection of the transitions that are part of
	 *         the machine.
	 */
    public Collection<Transition> getTransitions() {
        return Collections.unmodifiableCollection(transitions);
    }

    /**
	 * Check id there is at least one start-state.
	 * 
	 * @return <code>true</code> if there is at least one start-state<br>
	 *         <code>false</code> if there is no start-state.
	 */
    public boolean hasStartState() {
        return !getStartStates().isEmpty();
    }

    /**
	 * Inserts a TransitionDot to a transition.
	 * 
	 * @param t
	 *            the transition that will get an additional dot.
	 * @param dot
	 *            the TransitionDot of the transition t where the new dot is
	 *            inserted.
	 * @see Transition#insertDotAt(TransitionDot)
	 * @throws UnknownTransitionException
	 *             thrown if t does not belong to the machine
	 * @throws IllegalParameterException
	 *             see Transition#insertDotAt
	 */
    public void insertDot(Transition t, TransitionDot dot) throws UnknownTransitionException, IllegalParameterException {
        if (isTransition(t)) {
            TransitionDot newDot = t.insertDotAt(dot);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_INSERT_DOT, new Object[] { newDot, (t.getDots().indexOf(newDot)), t }));
        } else throw new UnknownTransitionException(Turingmachine.class.getCanonicalName() + " / the passed transition is not part of this turingmachine  / not inserting a dot");
    }

    /**
	 * Check if the passed oval is part of the machine.
	 * 
	 * @param st
	 *            the oval to check
	 * @return <code>true</code> the oval is part of the machine<br>
	 *         <code>false</code> the oval is not part of the machine.
	 */
    private boolean isOval(SelectableOval st) {
        if (st.getClass().equals(State.class)) return isState((State) st);
        if (st.getClass().equals(Submachine.class)) return isSubmachine((Submachine) st);
        return false;
    }

    /**
	 * Checks if the point some is close enough to the point cent, which is the
	 * center of an oval, to select it.
	 * 
	 * @param cent
	 *            the center of an oval.
	 * @param some
	 *            the point that is/is not inside the oval.
	 * @return <code>true/false</code> if some is/is not inside the oval.
	 */
    private boolean isOvalSelected(Point cent, Point some) {
        int distX = Math.abs(cent.x - some.x);
        int distY = Math.abs(cent.y - some.y);
        int radikant = (int) (Math.pow(Constants.OVAL_WIDTH / 2, 2) - Math.pow(distX, 2));
        if (radikant < 0) return false;
        int res = (int) Math.round((Constants.OVAL_HEIGHT / 2) * Math.sqrt(radikant) / (Constants.OVAL_WIDTH / 2));
        return res - distY >= 0;
    }

    /**
	 * Checks if the passed state is part of the machine.
	 * 
	 * @param s
	 *            the state to check
	 * @return true/false if s is/is not part of the machine
	 */
    public boolean isState(State s) {
        for (State it : states) if (it == s) return true;
        return false;
    }

    /**
	 * Checks if the passed submachine is part of the machine.
	 * 
	 * @param s
	 *            the submachine to check
	 * @return true/false if s is/is not part of the machine
	 */
    public boolean isSubmachine(Submachine s) {
        for (Submachine it : submachines) if (it == s) return true;
        return false;
    }

    /**
	 * Returns all symbols in string-representation that match the passed sn.
	 * <p>
	 * if front and back are true, a symbol has to contain sn to be retured.<br>
	 * if front is true and back is false, a symbol has to end with sn to be
	 * retured.<br>
	 * if front is false back is false, a symbol has to be sn to be retured.<br>
	 * if front is false and back is true, a symbol has to start with sn to be
	 * retured.
	 * 
	 * @param sn
	 *            the string the symbol has to contain/start with/end with/be.
	 * @param front
	 *            works like REGEXPR ".*" at the beginning of the string
	 * @param back
	 *            works like REGEXPR ".*" at the end of the string
	 * @return the vector of strings that match the parsed expression passed.
	 */
    public Vector<String> getMatchingSymbols(String sn, boolean front, boolean back) {
        Vector<String> match = new Vector<String>();
        for (Symbol it : symbols) {
            String tmp = it.getSymbol();
            if (front && back && tmp.contains(sn)) match.add(tmp);
            if (front && !back && tmp.endsWith(sn)) match.add(tmp);
            if (!front && back && tmp.startsWith(sn)) match.add(tmp);
            if (!front && !back && tmp.equals(sn)) match.add(tmp);
        }
        return (match.isEmpty()) ? null : match;
    }

    /**
	 * Checks if the symbol is part of the machine.
	 * 
	 * @param sn
	 *            the symbol to check.
	 * @return <code>true/false</code> if the symbol is/is not part of the
	 *         machine.
	 */
    public boolean isSymbol(Symbol sn) {
        for (Symbol it : symbols) if (it == sn) return true;
        return false;
    }

    /**
	 * Checks if the tape is part of the machine.
	 * 
	 * @param tape
	 *            the tape to check.
	 * @return <code>true/false</code> if the tape is/is not part of the
	 *         machine.
	 */
    public boolean isTape(Tape tape) {
        for (Tape it : tapes) if (it == tape) return true;
        return false;
    }

    /**
	 * Checks if the transition is part of the machine.
	 * 
	 * @param t
	 *            the transition to check.
	 * @return <code>true/false</code> if the transition is/is not part of the
	 *         machine.
	 */
    public boolean isTransition(Transition t) {
        for (Transition it : transitions) if (it == t) return true;
        return false;
    }

    /**
	 * Checks if the point some is close enough to the point cent, which is the
	 * center of a transitionDot, to select it.
	 * 
	 * @param cent
	 *            the center of a transitionDot.
	 * @param some
	 *            the point that is/is not inside the circle.
	 * @return <code>true/false</code> if some is/is not inside the cirlce.
	 */
    private boolean isTransitionDotSelected(Point cent, Point some) {
        return cent.distance(some) <= Constants.DOT_RADIUS;
    }

    /**
	 * Move the head of the passed tape to a relative index.
	 * 
	 * @param t
	 *            the tape which head is moved.
	 * @param idx
	 *            the relative index by that the head is moved.
	 * @throws UnknownTapeException
	 *             the tape t does not belong to the machine
	 */
    public void moveTapeHead(Tape t, int idx) throws UnknownTapeException {
        if (isTape(t)) {
            t.moveHead(idx);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_SET_HEAD_POS, new Object[] { idx, t }));
        } else throw new UnknownTapeException(Turingmachine.class.getCanonicalName() + " / the passed tape is already part of this turingmachine / not changing tape head position");
    }

    /**
	 * All transitionDots are removed and default look of the transition is
	 * created.
	 * 
	 * @param st
	 *            the transition to normalize
	 * @see Transition#normalize()
	 * @throws UnknownTransitionException
	 *             st is not part of the machine
	 * @throws IllegalParameterException
	 *             see Transition#normalize
	 */
    public void normalizeTransition(Transition st) throws UnknownTransitionException, IllegalParameterException {
        if (!isTransition(st)) throw new UnknownTransitionException(Turingmachine.class.getCanonicalName() + " / the passed transitiondot is not part of this turingmachine / not changing dots position");
        st.normalize();
    }

    /**
	 * An expression is transformed into several strings.
	 * <p>
	 * the single feature is the bracket : "[]"<br>
	 * nesting is not allowed<br>
	 * the part in front of the "[" is taken and for every char inside the "[]"
	 * the char is appended to the first part.<br>
	 * afterwards everything after the closing "]" is appened to all the
	 * strings.
	 * 
	 * @param str
	 *            the expression
	 * @return the list of expanded strings
	 * @throws ParsingException
	 *             invalid char/nesting/not closed a "["
	 */
    private Vector<String> parseBracketExpression(String str) throws ParsingException {
        Vector<String> newSymbs = new Vector<String>();
        newSymbs.add("");
        Vector<String> newSymbsTmp = new Vector<String>();
        char[] tmp = str.toCharArray();
        for (int i = 0; i < tmp.length; i++) {
            if (Constants.ALLOWED_SYMBOL_CHARS.indexOf(tmp[i]) < 0 && tmp[i] != '[' && tmp[i] != ']') throw new ParsingException(Turingmachine.class.getCanonicalName() + " / invalid symbol " + tmp[i] + " / not adding any symbols to list"); else if (tmp[i] == '[') {
                i++;
                newSymbsTmp.clear();
                while (i < tmp.length && tmp[i] != ']') {
                    if (tmp[i] == '[') throw new ParsingException(Turingmachine.class.getCanonicalName() + " / second depth [] in expression / not adding any symbols to list");
                    for (String it : newSymbs) newSymbsTmp.add(it + tmp[i]);
                    i++;
                }
                newSymbs.clear();
                newSymbs.addAll(newSymbsTmp);
                if (i == tmp.length) throw new ParsingException(Turingmachine.class.getCanonicalName() + " / missing ] in expression / not adding any symbols to list");
            } else {
                newSymbsTmp.clear();
                for (String it : newSymbs) newSymbsTmp.add(it + tmp[i]);
                newSymbs.clear();
                newSymbs.addAll(newSymbsTmp);
            }
        }
        return newSymbs;
    }

    /**
	 * Removes a transitionDot from a transition.
	 * 
	 * @param t
	 *            the transition to remove a dot from
	 * @param dot
	 *            the dot to be removed.
	 * @throws UnknownTransitionException
	 *             the transition t is not part of the machine.
	 */
    public void removeDot(Transition t, TransitionDot dot) throws UnknownTransitionException {
        if (isTransition(t)) {
            int pos = t.getDots().indexOf(dot);
            t.removeDot(dot);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_REMOVE_DOT, new Object[] { dot, pos, t }));
        } else throw new UnknownTransitionException(Turingmachine.class.getCanonicalName() + " / the passed transition is not part of this turingmachine  / not removing the dot");
    }

    /**
	 * Removes an oval from the machine.
	 * 
	 * @param st
	 *            the oval to be removed.
	 * @see #removeState(State)
	 * @see #removeSubmachine(Submachine)
	 * @throws UnknownOvalException
	 *             see #removeState<br>
	 *             see #removeSubmachine
	 * @throws UnknownTransitionException
	 *             see #removeState<br>
	 *             see #removeSubmachine
	 */
    public void removeOval(SelectableOval st) throws UnknownOvalException, UnknownTransitionException {
        if (st.getClass() == State.class) removeState((State) st); else if (st.getClass() == Submachine.class) removeSubmachine((Submachine) st);
    }

    /**
	 * Removes a state from the machine.
	 * 
	 * @param s
	 *            the state to be removed.
	 * @see #removeTrans(SelectableOval, boolean, boolean)
	 * @throws UnknownOvalException
	 *             s is not part of the machine
	 * @throws UnknownTransitionException
	 *             see #removeTrans
	 */
    private void removeState(State s) throws UnknownOvalException, UnknownTransitionException {
        if (isState(s)) {
            removeTrans(s, true, true);
            s.setSelected(false);
            states.remove(s);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_REMOVE, new Object[] { s }));
        } else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed state is not part of this turingmachine  / not removing state");
    }

    /**
	 * Removes a submachine from the machine.
	 * 
	 * @param sm
	 *            the submachine to be removed.
	 * @see #removeTrans(SelectableOval, boolean, boolean)
	 * @throws UnknownOvalException
	 *             sm is not part of the machine
	 * @throws UnknownTransitionException
	 *             see #removeTrans
	 */
    private void removeSubmachine(Submachine sm) throws UnknownOvalException, UnknownTransitionException {
        if (isSubmachine(sm)) {
            removeTrans(sm, true, true);
            sm.setSelected(false);
            submachines.remove(sm);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_REMOVE, new Object[] { sm }));
        } else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed submachine is not part of this turingmachine  / not removing submachine");
    }

    /**
	 * Removes all symbols that match the passed expression.
	 * <p>
	 * the expression can start/end with "*".<br>
	 * the expression can contain "[]".
	 * 
	 * @see #parseBracketExpression(String)
	 * @see #getMatchingSymbols(String, boolean, boolean)
	 * @param sn
	 *            the expression
	 * @throws ParsingException
	 *             see #getMatchingSymbols
	 * @throws InvalidNameException
	 *             a symbol is created that has an invalid name
	 */
    public void removeSymbols(String sn) throws ParsingException, InvalidNameException {
        Vector<String> vec = getMatchingSymols(sn);
        for (String it : vec) {
            Symbol symb = new Symbol(it);
            if (!symbols.contains(symb)) continue;
            symbols.remove(symb);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.SYMBOL_REMOVE, new Object[] { symb }));
        }
    }

    /**
	 * Removes a tape from the machine.
	 * 
	 * @param t
	 *            the tape to be removed
	 * @throws IllegalParameterException
	 *             the tape is not part of the machine
	 */
    public void removeTape(Tape t) throws IllegalParameterException {
        if (!isTape(t)) throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the passed tape is not part of this turingmachine / not removing tape");
        tapes.remove(t);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_REMOVE, new Object[] { t }));
    }

    /**
	 * Removes a set of transitions.
	 * 
	 * @param s
	 *            the transitions are connected to s.
	 * @param in
	 *            all incoming transitions are removed.
	 * @param out
	 *            all outgoing transitions are removed.
	 * @throws UnknownTransitionException
	 *             should be an internal error, getTrans should not return any
	 *             transitions that are not part of the machine
	 * @throws UnknownOvalException
	 *             s is not part of the machine
	 */
    public void removeTrans(SelectableOval s, boolean in, boolean out) throws UnknownTransitionException, UnknownOvalException {
        for (Transition it : getTrans(s, in, out)) removeTransition(it);
    }

    /**
	 * A transition is removed from the machine.
	 * 
	 * @param trans
	 *            the transition to be removed.
	 * @throws UnknownTransitionException
	 *             the transition trans is not part of the machine.
	 */
    public void removeTransition(Transition trans) throws UnknownTransitionException {
        if (isTransition(trans)) {
            transitions.remove(trans);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_REMOVE, new Object[] { trans }));
        } else throw new UnknownTransitionException(Turingmachine.class.getCanonicalName() + " / the passed transition is not part of this turingmachine / not removing the transitions");
    }

    /**
	 * A transitionOperation is removed from a transition.
	 * 
	 * @param tran
	 *            the transition from which an operation is removed.
	 * @param t
	 *            the tape the operation used.
	 * @param hm
	 *            the headmove the operation performed.
	 * @param read
	 *            the symbol that was read.
	 * @param write
	 *            the symbols that was written.
	 * @throws UnknownTapeException
	 *             the tape t is not part of the machine
	 * @throws UnknownTransitionException
	 *             the transition tran is not part of the machine
	 * @throws IllegalParameterException
	 *             an illegal symbol/headmove was passed
	 */
    public void removeTransitionOpn(Transition tran, Tape t, HeadMove hm, Symbol read, Symbol write) throws UnknownTapeException, UnknownTransitionException, IllegalParameterException {
        if (!isTransition(tran)) throw new UnknownTransitionException(Turingmachine.class.getCanonicalName() + " / the passed transition is not part of this turingmachine / not removing the transitions opn");
        if (!isTape(t)) throw new UnknownTapeException(Turingmachine.class.getCanonicalName() + " / the passed tape is not part of this turingmachine / not removing the transitions opn");
        if (hm == null) throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the headmove has to be specified / not removing the transitions opn");
        if ((read == null || !isSymbol(read) && !read.isBlank() && !read.isEpsilon()) && (write == null || !isSymbol(write) && !write.isBlank() && !write.isEpsilon())) throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the passed symbol is not part of this turingmachine / not removing the transitions opn");
        TransitionOpns op = tran.removeOpn(t, hm, read, write);
        if (op != null) if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_REMOVE_OPNS, new Object[] { op, tran }));
    }

    /**
	 * A transition that is normalized is renormalized when a connected oval has
	 * been moved.
	 * 
	 * @param s
	 *            the oval that has been moved.
	 * @see Transition#normalize()
	 * @throws UnknownOvalException
	 *             s is not part of the machine
	 * @throws UnknownTransitionException
	 *             should not occur since getTrans should not return invalid
	 *             transitions
	 * @throws IllegalParameterException
	 *             see Transition#normalize
	 */
    private void reNormalizeTrans(SelectableOval s) throws UnknownOvalException, UnknownTransitionException, IllegalParameterException {
        for (Transition it : getTrans(s, true, true)) if (it.isNormalized()) normalizeTransition(it);
    }

    /**
	 * The machine is stored in XML-format to os.
	 * 
	 * @param os
	 *            the outputstream where the machine is stored.
	 * @see XML_converter#save(Turingmachine, OutputStream)
	 * @throws TransformerException
	 *             see XML_converter.save
	 * @throws ParserConfigurationException
	 *             see XML_converter.save
	 */
    public void save(OutputStream os) throws TransformerException, ParserConfigurationException {
        XML_converter.save(this, os);
    }

    /**
	 * Selects/Deselects an oval.
	 * 
	 * @param s
	 *            the oval to be de/selected
	 * @param mode
	 *            toggles the selection
	 * @throws UnknownOvalException
	 *             s is not part of the machine
	 */
    public void selectOval(SelectableOval s, boolean mode) throws UnknownOvalException {
        if (s.getClass() == State.class && isState((State) s) || s.getClass() == Submachine.class && isSubmachine((Submachine) s)) s.setSelected(mode); else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed state is not part of this turingmachine  / not changing selection mode");
    }

    /**
	 * The expression used to display the operations of a transition is changed.
	 * 
	 * @param tran
	 *            the transition which anchor-expression is changed
	 * @param expr
	 *            the new expression
	 * @throws UnknownTransitionException
	 *             tran is not part of the machine
	 */
    public void setAnchorExpr(Transition tran, String expr) throws UnknownTransitionException {
        if (!isTransition(tran)) throw new UnknownTransitionException(Turingmachine.class.getCanonicalName() + " / the passed transition is not part of this turingmachine / not changing the transitions anchor-exp");
        String oldV = tran.getAnchorExpr();
        if (oldV != null && oldV.equals(expr) || oldV == expr) return;
        tran.setAnchorExp(expr);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_SET_ANCHOR_EXPR, new Object[] { oldV, expr, tran }));
    }

    /**
	 * The description of the turingmachine is changed.
	 * 
	 * @param pDescription
	 *            the new description.
	 */
    public void setDescription(String pDescription) {
        if (description.equals(pDescription)) return;
        String prev = description;
        description = pDescription;
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.MACHINE_SET_DESCRIPTION, new Object[] { prev, description }));
    }

    /**
	 * The position of a transitiondot is changed.
	 * 
	 * @param dot
	 *            the transitiondot to move.
	 * @param newPos
	 *            the new position of the transitiondot
	 * @throws IllegalParameterException
	 *             the passed dot is not part of the machine
	 */
    public void setDotPosition(TransitionDot dot, Point newPos) throws IllegalParameterException {
        Transition trans = null;
        for (Transition it : transitions) {
            trans = it;
            if (containsDot(trans, dot)) break;
            trans = null;
        }
        if (trans == null) throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the passed transitiondot is not part of this turingmachine / not changing dots position");
        if (dot != trans.getTextAnchor() && dot.getPosition().distance(trans.getTextAnchor().getPosition()) < Constants.TAKE_ANCHOR && undoEnabled) setDotPosition(trans.getTextAnchor(), new Point(trans.getTextAnchor().getPosition().x - dot.getPosition().x + newPos.x, trans.getTextAnchor().getPosition().y - dot.getPosition().y + newPos.y));
        Point old = new Point(dot.getPosition());
        dot.setPosition(newPos, true);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TRANSITION_MOVE_DOT, new Object[] { old, new Point(newPos), dot }));
    }

    /**
	 * Changes the ovals name.
	 * 
	 * @param s
	 *            the oval which name is changed.
	 * @param desc
	 *            the new name of the oval.
	 * @throws UnknownOvalException
	 *             s is not part of the machine.
	 * @throws InvalidNameException
	 *             the passed name is ivalid.
	 */
    public void setOvalName(SelectableOval s, String desc) throws UnknownOvalException, InvalidNameException {
        if (isOval(s)) {
            String old = s.getName();
            if (old.equals(desc)) return;
            s.setName(desc);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_SET_NAME, new Object[] { desc, old, s.getID() }));
        } else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed state is not part of this turingmachine  / not changing name");
    }

    /**
	 * Changes an ovals position.
	 * 
	 * @param st
	 *            the oval to move
	 * @param pos
	 *            the new position
	 * @see #reNormalizeTrans(SelectableOval)
	 * @throws UnknownOvalException
	 *             st is not part of the machine
	 * @throws IllegalParameterException
	 *             see #reNormalizeTrans
	 */
    public void setOvalPosition(SelectableOval st, Point pos) throws UnknownOvalException, IllegalParameterException {
        if (isOval(st)) {
            Point old = st.getPosition();
            if (old.equals(pos)) return;
            st.setPosition(pos);
            try {
                reNormalizeTrans(st);
            } catch (UnknownOvalException e) {
                return;
            } catch (UnknownTransitionException e) {
                return;
            }
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.OVAL_SET_POS, new Object[] { st.getPosition(), old, st.getID() }));
        } else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed state is not part of this turingmachine  / not changing selection mode");
    }

    /**
	 * Changes a states type.
	 * 
	 * @param s
	 *            the state which type is changed.
	 * @param st
	 *            the new type.
	 * @throws UnknownOvalException
	 *             s is not part of the machine.
	 */
    public void setStateType(State s, StateType st) throws UnknownOvalException {
        if (isState(s)) {
            StateType old = s.getType();
            s.setType(new StateType(st.isStartState(), st.isEndState()));
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.STATE_SET_TYPE, new Object[] { s.getType(), old, s.getID() }));
        } else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed state is not part of this turingmachine  / not changing name");
    }

    /**
	 * Changes a states type.
	 * 
	 * @param s
	 *            thestate which type is changed.
	 * @param desc
	 *            the new type.
	 * @throws UnknownOvalException
	 *             s is not part of the machine.
	 * @throws IllegalParameterException
	 *             desc is not a valid type-description
	 */
    public void setStateType(State s, String desc) throws UnknownOvalException, IllegalParameterException {
        StateType st;
        if (desc.equals(Constants.STATE_A_STRING)) st = new StateType(false, true); else if (desc.equals(Constants.STATE_SA_STRING)) st = new StateType(true, true); else if (desc.equals(Constants.STATE_S_STRING)) st = new StateType(true, false); else if (desc.equals(Constants.STATE_N_STRING)) st = new StateType(false, false); else throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the passed type-string is not valid / not changing state type");
        if (isState(s)) {
            StateType old = s.getType();
            s.setType(st);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.STATE_SET_TYPE, new Object[] { st, old, s.getID() }));
        } else throw new UnknownOvalException(Turingmachine.class.getCanonicalName() + " / the passed state is not part of this turingmachine  / not changing type");
    }

    /**
	 * The link a submachine to its referenced machine is changed.
	 * 
	 * @param selected
	 *            the submachine which link is changed.
	 * @param tm
	 *            the new turingmachine this submachine references.
	 * @throws IllegalParameterException
	 *             selected is not part of the machine.
	 */
    public void setSubmachineMachine(Submachine selected, Turingmachine tm) throws IllegalParameterException {
        if (isSubmachine(selected)) {
            Turingmachine prev = selected.getMachine();
            selected.setMachine(tm);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.SUBMACHINE_SET_LINK, new Object[] { prev, tm, selected }));
        } else throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the passed submachine is not part of this turingmachine / not changing link");
    }

    /**
	 * A symbol is assigned to a spot on the tape.
	 * 
	 * @param tape
	 *            the tape on which a symbol is written
	 * @param str
	 *            the symbol in string-representation
	 * @param i
	 *            the relative position to the head where the symbol is written.
	 * @throws IllegalParameterException
	 *             the passed tape/symbol are not part of the machine.
	 */
    public void setSymbolAt(Tape tape, String str, int i) throws IllegalParameterException {
        if (!isTape(tape)) throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the passed tape is not part of this turingmachine / not changing symbol on tape");
        Symbol tmp = null;
        for (Symbol it : symbols) {
            tmp = it;
            if (tmp.getSymbol().equals(str)) break;
            tmp = null;
        }
        if (tmp == null && str != null) throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the passed symbol is not part of this turingmachine / not changing symbol on tape");
        String old = tape.getSymbolAt(i);
        tape.setSymbolAt(tmp, i);
        if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_SET_SYMBOL_AT, new Object[] { old, str, i, tape }));
    }

    /**
	 * The description of a tape is changed.
	 * 
	 * @param t
	 *            the tape which description is changed.
	 * @param desc
	 *            the new description
	 * @throws UnknownTapeException
	 *             t is not part of the machine
	 * @throws InvalidNameException
	 *             desc is an invalid tape-description
	 */
    public void setTapeDescription(Tape t, String desc) throws UnknownTapeException, InvalidNameException {
        if (isTape(t)) {
            String prev = t.getDescription();
            if (prev.equals(desc)) return;
            t.setDescription(desc);
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_SET_DESCRIPTION, new Object[] { prev, t.getDescription(), t }));
        } else throw new UnknownTapeException(Turingmachine.class.getCanonicalName() + " / the passed tape is not part of this turingmachine / not changing tapes description");
    }

    /**
	 * A tape gets a new tapenumber.
	 * <p>
	 * The two tapes just switch their numbers.
	 * 
	 * @param t
	 *            the tape which number is changed.
	 * @param number
	 *            the number the tape t gets.
	 * @throws IllegalParameterException
	 *             the number number is out of range. (there is no tape with
	 *             that number)
	 */
    public void setTapeNumber(Tape t, int number) throws IllegalParameterException {
        if (t.getTapeNumber() == number) return;
        for (Tape it : tapes) if (it.getTapeNumber() == number) {
            int prev = it.getTapeNumber();
            it.setTapeNumber(t.getTapeNumber());
            if (undoRedo != null && undoEnabled) undoRedo.performedOperation(this, new Operation(Operation.opType.TAPE_SET_NUMBER, new Object[] { prev, it.getTapeNumber(), it }));
            t.setTapeNumber(number);
            return;
        }
        throw new IllegalParameterException(Turingmachine.class.getCanonicalName() + " / the given tape number is too large / not changing any tape number");
    }

    /**
	 * All objects that have a location are moved by the coordinates of cent.
	 * 
	 * @param cent
	 *            the new relative location.
	 */
    public void reCenter(Point cent) {
        for (SelectableOval s : getOvals()) s.setPosition(new Point(s.getPosition().x + cent.x, s.getPosition().y + cent.y));
        for (Transition t : getTransitions()) {
            for (TransitionDot td : t.getDots()) td.setPosition(new Point(td.getPosition().x + cent.x, td.getPosition().y + cent.y), false);
            TransitionDot td = t.getTextAnchor();
            Point pos = new Point(td.getPosition().x + cent.x, td.getPosition().y + cent.y);
            td.setPosition(pos, false);
            try {
                t.setTextAnchor(td);
            } catch (IllegalParameterException e) {
            }
        }
    }

    /**
	 * Applys the appropriate zoom-factor.
	 * 
	 * @param in
	 *            The input-value that need to be adjusted.
	 * @return The adjusted value.
	 */
    public int applyZoom(int in) {
        switch(zoom) {
            case ZOOM_100:
                return in;
            case ZOOM_200:
                return in / 2;
            case ZOOM_400:
                return in / 4;
            case ZOOM_800:
                return in / 8;
        }
        return in;
    }

    /**
	 * Returns the current zoom-factor.
	 * 
	 * @return The current zoom-factor.
	 */
    public ZoomMode getZoom() {
        return zoom;
    }
}
