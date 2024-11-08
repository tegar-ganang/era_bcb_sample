package blockworld;

import apapl.Environment;
import apapl.ExternalActionFailedException;
import apapl.data.*;
import java.awt.Point;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.LinkedList;
import javax.swing.SwingUtilities;
import eis.exceptions.EntityException;
import eis.exceptions.ManagementException;
import eis.exceptions.NoEnvironmentException;
import eis.exceptions.RelationException;
import eis.iilang.EnvironmentCommand;
import eis.iilang.Function;
import eis.iilang.Numeral;
import eis.iilang.Parameter;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import blockworld.lib.ObsVectListener;
import blockworld.lib.Signal;
import blockworld.lib.ObsVect;

public class Env extends Environment implements ObsVectListener {

    protected final Window m_window;

    protected int _senserange = 5;

    protected Dimension m_size = new Dimension(16, 16);

    private HashMap<String, Agent> agentmap = new HashMap<String, Agent>();

    protected ObsVect _agents = new ObsVect(this);

    protected ObsVect _traps = new ObsVect();

    protected ObsVect _stones = new ObsVect();

    protected ObsVect _bombs = new ObsVect();

    protected String _objType = "default";

    public transient Signal signalBombTrapped = new Signal("env bomb trapped");

    public transient Signal signalSenseRangeChanged = new Signal("env sense range changed");

    public transient Signal signalSizeChanged = new Signal("env size changed");

    public transient Signal signalTrapChanged = new Signal("env trap position changed");

    public Env() {
        super();
        m_window = new Window(this);
    }

    public Term enter(String sAgent, APLNum xt, APLNum yt, APLIdent colort) throws ExternalActionFailedException {
        int x = xt.toInt();
        int y = yt.toInt();
        String color = colort.toString();
        Agent agent = getAgent(sAgent);
        agent.signalMove.emit();
        writeToLog("Agent entered: " + agent.getName());
        Point position = new Point(x, y);
        String pos = "(" + x + "," + y + ")";
        if (agent.isEntered()) {
            writeToLog("agent already entered");
            throw new ExternalActionFailedException("Agent \"" + agent.getName() + "\" has already entered.");
        }
        if (isOutOfBounds(position)) {
            throw new ExternalActionFailedException("Position " + pos + " out of bounds.");
        }
        if (!isFree(position)) {
            throw new ExternalActionFailedException("Position " + pos + " is not free.");
        }
        agent._position = position;
        int nColorID = getColorID(color);
        agent._colorID = nColorID;
        validatewindow();
        m_window.repaint();
        agent.signalMoveSucces.emit();
        return wrapBoolean(true);
    }

    public Term north(String sAgent) throws ExternalActionFailedException {
        Agent agent = getAgent(sAgent);
        Point p = (Point) agent.getPosition().clone();
        p.y = p.y - 1;
        boolean r = setAgentPosition(agent, p);
        if (!r) throw new ExternalActionFailedException("Moving north failed.");
        validatewindow();
        m_window.repaint();
        return wrapBoolean(r);
    }

    public Term east(String sAgent) throws ExternalActionFailedException {
        Agent agent = getAgent(sAgent);
        Point p = (Point) agent.getPosition().clone();
        p.x = p.x + 1;
        boolean r = setAgentPosition(agent, p);
        if (!r) throw new ExternalActionFailedException("Moving north failed.");
        validatewindow();
        m_window.repaint();
        return wrapBoolean(r);
    }

    public Term south(String sAgent) throws ExternalActionFailedException {
        Agent agent = getAgent(sAgent);
        Point p = (Point) agent.getPosition().clone();
        p.y = p.y + 1;
        boolean r = setAgentPosition(agent, p);
        if (!r) throw new ExternalActionFailedException("Moving north failed.");
        validatewindow();
        m_window.repaint();
        return wrapBoolean(r);
    }

    public Term west(String sAgent) throws ExternalActionFailedException {
        Agent agent = getAgent(sAgent);
        Point p = (Point) agent.getPosition().clone();
        p.x = p.x - 1;
        boolean r = setAgentPosition(agent, p);
        if (!r) throw new ExternalActionFailedException("Moving north failed.");
        validatewindow();
        m_window.repaint();
        return wrapBoolean(r);
    }

    public Term pickup(String sAgent) throws ExternalActionFailedException {
        Agent agent = getAgent(sAgent);
        agent.signalPickupBomb.emit();
        if (agent.atCapacity()) {
            writeToLog("Pickup bomb failed");
            throw new ExternalActionFailedException("Pickup bomb failed");
        }
        TypeObject bomb = removeBomb(agent.getPosition());
        if (bomb == null) {
            writeToLog("Pickup bomb failed");
            throw new ExternalActionFailedException("Pickup bomb failed");
        }
        agent.signalPickupBombSucces.emit();
        agent.pickBomb(bomb);
        validatewindow();
        m_window.repaint();
        return wrapBoolean(true);
    }

    public Term drop(String sAgent) throws ExternalActionFailedException {
        Agent agent = getAgent(sAgent);
        agent.signalDropBomb.emit();
        TypeObject bomb = agent.senseBomb();
        if (bomb == null) {
            writeToLog("Drop bomb failed");
            throw new ExternalActionFailedException("Drop bomb failed");
        }
        Point pos = agent.getPosition();
        if (!addBomb(pos) && (isTrap(pos) == null || agent.senseBomb(isTrap(pos).getType()) == null)) {
            writeToLog("Drop bomb failed");
            throw new ExternalActionFailedException("Drop bomb failed");
        }
        if (isTrap(pos) != null && agent.senseBomb(isTrap(pos).getType()) != null) {
            signalBombTrapped.emit();
        }
        agent.dropBomb();
        agent.signalDropBombSucces.emit();
        validatewindow();
        m_window.repaint();
        return wrapBoolean(true);
    }

    public Term getSenseRange(String agent) {
        int r = getSenseRange();
        return new APLList(new APLNum(r));
    }

    public synchronized Term senseAllAgent(String sAgent) throws ExternalActionFailedException {
        Point position = getAgent(sAgent).getPosition();
        Vector all = new Vector();
        Iterator j = _agents.iterator();
        while (j.hasNext()) {
            Agent agent = (Agent) j.next();
            Point p = agent.getPosition();
            if (p == null) continue;
            if (p.equals(position)) continue;
            all.add(agent);
        }
        LinkedList<Term> listpar = new LinkedList<Term>();
        for (Object i : all) {
            final Agent a = (Agent) i;
            APLListVar tmp = new APLList(new APLIdent(a.getName()), new APLNum(a.getPosition().x), new APLNum(a.getPosition().y));
            listpar.add(tmp);
        }
        return new APLList(listpar);
    }

    public synchronized Term sensePosition(String sAgent) throws ExternalActionFailedException {
        Point p = getAgent(sAgent).getPosition();
        return new APLList(new APLNum(p.x), new APLNum(p.y));
    }

    public synchronized Term senseTraps(String agent) throws ExternalActionFailedException {
        Point position = getAgent(agent).getPosition();
        Vector visible = new Vector();
        Iterator i = _traps.iterator();
        while (i.hasNext()) {
            TypeObject t = (TypeObject) i.next();
            if (position.distance(t.getPosition()) <= _senserange) visible.add(t);
        }
        return convertCollectionToTerm(visible);
    }

    public synchronized Term senseAllTraps(String agent) {
        Vector all = new Vector();
        Iterator i = _traps.iterator();
        while (i.hasNext()) {
            all.add((TypeObject) i.next());
        }
        return convertCollectionToTerm(all);
    }

    public synchronized Term senseBombs(String agent) throws ExternalActionFailedException {
        Point position = getAgent(agent).getPosition();
        Vector visible = new Vector();
        Iterator i = _bombs.iterator();
        while (i.hasNext()) {
            TypeObject b = (TypeObject) i.next();
            if (position.distance(b.getPosition()) <= _senserange) visible.add(b);
        }
        return convertCollectionToTerm(visible);
    }

    public synchronized Term senseAllBombs(String agent) {
        Vector all = new Vector();
        Iterator i = _bombs.iterator();
        while (i.hasNext()) {
            all.add((TypeObject) i.next());
        }
        return convertCollectionToTerm(all);
    }

    public synchronized Term senseStones(String agent) throws ExternalActionFailedException {
        Point position = getAgent(agent).getPosition();
        Vector visible = new Vector();
        Iterator i = _stones.iterator();
        while (i.hasNext()) {
            TypeObject t = (TypeObject) i.next();
            if (position.distance(t.getPosition()) <= _senserange) visible.add(t);
        }
        return convertCollectionToTerm(visible);
    }

    public synchronized Term senseAllStones(String agent) {
        Vector all = new Vector();
        Iterator i = _stones.iterator();
        while (i.hasNext()) {
            all.add((TypeObject) i.next());
        }
        return convertCollectionToTerm(all);
    }

    public synchronized Term senseAgent(String sAgent) throws ExternalActionFailedException {
        Point position = getAgent(sAgent).getPosition();
        Vector visible = new Vector();
        Iterator j = _agents.iterator();
        while (j.hasNext()) {
            Agent agent = (Agent) j.next();
            Point p = agent.getPosition();
            if (p == null) continue;
            if (p.equals(position)) continue;
            if (position.distance(p) <= _senserange) visible.add(agent);
        }
        LinkedList<Term> listpar = new LinkedList<Term>();
        for (Object i : visible) {
            final Agent a = (Agent) i;
            APLListVar tmp = new APLList(new APLIdent(a.getName()), new APLNum(a.getPosition().x), new APLNum(a.getPosition().y));
            listpar.add(tmp);
        }
        return new APLList(listpar);
    }

    private void notifyAgents(APLFunction event, String... receivers) {
        throwEvent(event, receivers);
    }

    private void notifyEvent(String parm1, Point ptPosition) {
        APLNum nX = new APLNum((double) (ptPosition.getX()));
        APLNum nY = new APLNum((double) (ptPosition.getY()));
        ArrayList<String> targetAgents = new ArrayList<String>();
        for (Agent a : agentmap.values()) {
            if ((a.getPosition() != null) && (ptPosition.distance(a.getPosition()) <= getSenseRange())) targetAgents.add(a.getName());
        }
        writeToLog("EVENT: " + parm1 + "(" + nX + "," + nY + ")" + " to " + targetAgents);
        if (!targetAgents.isEmpty()) {
            notifyAgents(new APLFunction(parm1, nX, nY), targetAgents.toArray(new String[0]));
        }
    }

    public synchronized void addAgent(String sAgent) {
        String sAgentMain = getMainModule(sAgent);
        if (agentmap.keySet().contains(sAgentMain)) {
            agentmap.put(sAgent, agentmap.get(sAgentMain));
            writeToLog("linking " + sAgent + "");
        } else {
            final Agent agent = new Agent(sAgentMain);
            _agents.add(agent);
            agentmap.put(sAgent, agent);
            writeToLog("agent " + agent + " added");
        }
    }

    public synchronized void removeAgent(String sAgent) {
        try {
            Agent a = getAgent(sAgent);
            agentmap.remove(sAgent);
            if (!agentmap.containsValue(a)) {
                _agents.remove(a);
                a.reset();
            }
            writeToLog("Agent removed: " + sAgent);
            synchronized (this) {
                notifyAll();
            }
        } catch (ExternalActionFailedException e) {
        }
    }

    public synchronized Term getWorldSize(String agent) {
        int w = getWidth();
        int h = getHeight();
        return new APLList(new APLNum(w), new APLNum(h));
    }

    private synchronized Agent getAgent(String name) throws ExternalActionFailedException {
        Agent a = null;
        a = agentmap.get(name);
        if (a == null) throw new ExternalActionFailedException("No such agent: " + name); else return a;
    }

    private static String getMainModule(String sAgent) {
        int dotPos;
        if ((dotPos = sAgent.indexOf('.')) == -1) return sAgent; else return sAgent.substring(0, dotPos);
    }

    public synchronized int getWidth() {
        return m_size.width;
    }

    public synchronized int getHeight() {
        return m_size.height;
    }

    public synchronized Collection getBlockWorldAgents() {
        return new Vector(_agents);
    }

    private static Term convertCollectionToTerm(Collection c) {
        LinkedList<Term> listpar = new LinkedList<Term>();
        for (Object i : c) {
            final TypeObject o = (TypeObject) i;
            APLListVar tmp = new APLList(new APLIdent(o.getType()), new APLNum(o.getPosition().x), new APLNum(o.getPosition().y));
            listpar.add(tmp);
        }
        return new APLList(listpar);
    }

    public int getSenseRange() {
        return _senserange;
    }

    private void validatewindow() {
        Runnable repaint = new Runnable() {

            public void run() {
                m_window.doLayout();
            }
        };
        SwingUtilities.invokeLater(repaint);
    }

    private synchronized boolean setAgentPosition(Agent agent, Point position) {
        agent.signalMove.emit();
        if (isOutOfBounds(position)) return false;
        if (!isFree(position)) return false;
        agent.signalMoveSucces.emit();
        synchronized (this) {
            notifyAll();
        }
        agent._position = position;
        return true;
    }

    protected boolean isOutOfBounds(Point p) {
        if ((p.x >= m_size.getWidth()) || (p.x < 0) || (p.y >= m_size.getHeight()) || (p.y < 0)) {
            return true;
        }
        return false;
    }

    public synchronized boolean isFree(final Point position) {
        return (isStone(position)) == null && (isAgent(position) == null);
    }

    public synchronized Agent isAgent(final Point p) {
        synchronized (_agents) {
            Iterator i = _agents.iterator();
            while (i.hasNext()) {
                final Agent agent = (Agent) i.next();
                if (p.equals(agent.getPosition())) return agent;
            }
            return null;
        }
    }

    public synchronized TypeObject isStone(Point p) {
        synchronized (_agents) {
            Iterator i = _stones.iterator();
            while (i.hasNext()) {
                TypeObject stones = (TypeObject) i.next();
                if (p.equals(stones.getPosition())) return stones;
            }
            return null;
        }
    }

    public synchronized TypeObject isTrap(Point p) {
        synchronized (_traps) {
            Iterator i = _traps.iterator();
            while (i.hasNext()) {
                TypeObject trap = (TypeObject) i.next();
                if (p.equals(trap.getPosition())) return trap;
            }
            return null;
        }
    }

    public synchronized TypeObject isBomb(Point p) {
        synchronized (_bombs) {
            Iterator i = _bombs.iterator();
            while (i.hasNext()) {
                TypeObject bomb = (TypeObject) i.next();
                if (p.equals(bomb.getPosition())) return bomb;
            }
            return null;
        }
    }

    public synchronized TypeObject removeBomb(Point position) {
        synchronized (this) {
            Iterator i = _bombs.iterator();
            while (i.hasNext()) {
                TypeObject bomb = (TypeObject) i.next();
                if (position.equals(bomb.getPosition())) {
                    i.remove();
                    return bomb;
                }
            }
        }
        notifyEvent("bombRemovedAt", position);
        return null;
    }

    public synchronized boolean removeStone(Point position) {
        synchronized (_stones) {
            Iterator i = _stones.iterator();
            while (i.hasNext()) if (position.equals(((TypeObject) i.next()).getPosition())) {
                i.remove();
                synchronized (this) {
                    notifyAll();
                }
                return true;
            }
        }
        notifyEvent("wallRemovedAt", position);
        return false;
    }

    public synchronized boolean removeTrap(Point position) {
        synchronized (_traps) {
            Iterator i = _traps.iterator();
            while (i.hasNext()) {
                if (position.equals(((TypeObject) i.next()).getPosition())) {
                    i.remove();
                    return true;
                }
            }
        }
        notifyEvent("trapRemovedAt", position);
        return false;
    }

    public synchronized boolean addStone(Point position) throws IndexOutOfBoundsException {
        if (isOutOfBounds(position)) throw new IndexOutOfBoundsException("setStone out of range: " + position + ", " + m_size);
        if (isBomb(position) != null || isStone(position) != null || isTrap(position) != null) return false;
        synchronized (_stones) {
            _stones.add(new TypeObject(_objType, position));
        }
        notifyEvent("wallAt", position);
        return true;
    }

    public synchronized boolean addBomb(Point position) throws IndexOutOfBoundsException {
        if (isOutOfBounds(position)) throw new IndexOutOfBoundsException("addBomb outOfBounds: " + position + ", " + m_size);
        if (isBomb(position) != null || isStone(position) != null || isTrap(position) != null) return false;
        synchronized (_bombs) {
            _bombs.add(new TypeObject(_objType, position));
        }
        notifyEvent("bombAt", position);
        return true;
    }

    public synchronized boolean addTrap(Point position) throws IndexOutOfBoundsException {
        if (isOutOfBounds(position)) throw new IndexOutOfBoundsException("setTrap out of range: " + position + ", " + m_size);
        if (isBomb(position) != null || isStone(position) != null || isTrap(position) != null) return false;
        synchronized (_traps) {
            _traps.add(new TypeObject(_objType, position));
        }
        notifyEvent("trapAt", position);
        return true;
    }

    public static void writeToLog(String message) {
    }

    public static APLListVar wrapBoolean(boolean b) {
        return new APLList(new APLIdent(b ? "true" : "false"));
    }

    private int getColorID(String sColor) {
        if (sColor.equals("army")) {
            return 0;
        } else if (sColor.equals("blue")) {
            return 1;
        } else if (sColor.equals("gray")) {
            return 2;
        } else if (sColor.equals("green")) {
            return 3;
        } else if (sColor.equals("orange")) {
            return 4;
        } else if (sColor.equals("pink")) {
            return 5;
        } else if (sColor.equals("purple")) {
            return 6;
        } else if (sColor.equals("red")) {
            return 7;
        } else if (sColor.equals("teal")) {
            return 8;
        } else if (sColor.equals("yellow")) {
            return 9;
        }
        return 7;
    }

    public void setSenseRange(int senserange) {
        _senserange = senserange;
        signalSenseRangeChanged.emit();
    }

    public void setSize(int width, int height) {
        setSize(new Dimension(width, height));
    }

    public void setSize(Dimension size) {
        m_size = size;
        signalSizeChanged.emit();
        Iterator i = _bombs.iterator();
        while (i.hasNext()) {
            if (isOutOfBounds(((TypeObject) i.next()).getPosition())) i.remove();
        }
        i = _stones.iterator();
        while (i.hasNext()) {
            if (isOutOfBounds((Point) i.next())) i.remove();
        }
        i = _traps.iterator();
        while (i.hasNext()) {
            if (isOutOfBounds(((TypeObject) i.next()).getPosition())) i.remove();
        }
    }

    public String getObjType() {
        return _objType;
    }

    public void setObjType(String objType) {
        _objType = objType;
    }

    public void clear() {
        _stones.removeAllElements();
        _bombs.removeAllElements();
        _traps.removeAllElements();
    }

    public void save(OutputStream destination) throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(destination);
        stream.writeObject(m_size);
        stream.writeInt(_senserange);
        stream.writeObject((Vector) _stones);
        stream.writeObject((Vector) _bombs);
        stream.writeObject((Vector) _traps);
        stream.flush();
    }

    public void load(InputStream source) throws IOException, ClassNotFoundException {
        ObjectInputStream stream = new ObjectInputStream(source);
        Dimension size = (Dimension) stream.readObject();
        int senserange = stream.readInt();
        Vector stones = (Vector) stream.readObject();
        Vector bombs = (Vector) stream.readObject();
        Vector traps = (Vector) stream.readObject();
        m_size = size;
        _senserange = senserange;
        signalSizeChanged.emit();
        signalTrapChanged.emit();
        signalSenseRangeChanged.emit();
        clear();
        _stones.addAll(stones);
        _bombs.addAll(bombs);
        _traps.addAll(traps);
    }

    public void addAgentListener(ObsVectListener o) {
        _agents.addListener(o);
    }

    public void addStonesListener(ObsVectListener o) {
        _stones.addListener(o);
    }

    public void addBombsListener(ObsVectListener o) {
        _bombs.addListener(o);
    }

    public void addTrapsListener(ObsVectListener o) {
        _traps.addListener(o);
    }

    public void onAdd(int index, Object o) {
    }

    public void onRemove(int index, Object o) {
        ((Agent) o).deleteObservers();
    }

    /** EIS Environment Management  **/
    @Override
    public void manageEnvironment(EnvironmentCommand cmd) throws ManagementException, NoEnvironmentException {
        if (cmd.getType() == EnvironmentCommand.INIT) {
            int width = 10;
            int height = 10;
            int robots = 0;
            for (Parameter param : cmd.getParameters()) {
                if (param instanceof Function) {
                    if (((Function) param).getName().equals("gridWidth")) {
                        width = ((Numeral) ((Function) param).getParameters().getFirst()).getValue().intValue();
                    }
                    if (((Function) param).getName().equals("gridHeight")) {
                        height = ((Numeral) ((Function) param).getParameters().getFirst()).getValue().intValue();
                    }
                    if (((Function) param).getName().equals("entities")) {
                        robots = ((Numeral) ((Function) param).getParameters().getFirst()).getValue().intValue();
                    }
                }
            }
            setSize(width, height);
            if (robots > 0) {
                for (int i = 0; i <= robots; i++) {
                    addAgent("robot" + i);
                    try {
                        addEntity("robot" + i);
                    } catch (EntityException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void addAgentEntity(String agent) {
        if (agentmap.containsKey("robot0")) return;
        super.addAgentEntity(agent);
    }
}
