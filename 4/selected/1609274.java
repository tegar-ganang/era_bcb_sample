package org.zamia.vg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import org.zamia.ExceptionLogger;
import org.zamia.ZamiaLogger;
import org.zamia.util.HashMapArray;
import org.zamia.util.HashSetArray;
import org.zamia.util.Position;
import org.zamia.util.ZStack;
import org.zamia.vg.VGGC.VGColor;

/**
 * 
 * @author Guenter Bartsch
 * 
 */
public class VGLayout<NodeType, PortType, SignalType> {

    public static final ZamiaLogger logger = ZamiaLogger.getInstance();

    public static final ExceptionLogger el = ExceptionLogger.getInstance();

    private ArrayList<VGChannel<NodeType, PortType, SignalType>> fChannels;

    private VGContentProvider<NodeType, PortType, SignalType> fContentProvider;

    private VGLabelProvider<NodeType, PortType, SignalType> fLabelProvider;

    private static final double PLACEMENT_OPT_TIMEOUT = 3.0;

    private static final int PLACEMENT_OPT_NUM_ITERATIONS = 5;

    private static final int TOP_MARGIN = 10;

    private static final int LEFT_MARGIN = 10;

    private int fMaxDepth;

    private HashMapArray<PortType, VGBox<NodeType, PortType, SignalType>> fPrimaryPorts;

    private HashMapArray<NodeType, VGBox<NodeType, PortType, SignalType>> fNodeBoxes;

    private ArrayList<VGBox<NodeType, PortType, SignalType>> fBoxes;

    private HashMap<VGBox<NodeType, PortType, SignalType>, Integer> fNodeDepth;

    private HashMap<Integer, ArrayList<VGBox<NodeType, PortType, SignalType>>> fDepthNode;

    private Position fTotalSize;

    private HashMapArray<SignalType, VGSignal<NodeType, PortType, SignalType>> fSignals;

    private final VGGC fGC;

    private HashSetArray<VGPort<NodeType, PortType, SignalType>> fExpandablePorts;

    private HashMapArray<PortType, VGPort<NodeType, PortType, SignalType>> fPortMap;

    public VGLayout(VGContentProvider<NodeType, PortType, SignalType> aContentProvider, VGLabelProvider<NodeType, PortType, SignalType> aLabelProvider, VGGC aGC) {
        fContentProvider = aContentProvider;
        fLabelProvider = aLabelProvider;
        fGC = aGC;
        placeAndRoute();
    }

    VGSignal<NodeType, PortType, SignalType> getOrCreateSignal(SignalType aSignal) {
        VGSignal<NodeType, PortType, SignalType> s = fSignals.get(aSignal);
        if (s == null) {
            s = new VGSignal<NodeType, PortType, SignalType>(aSignal, fLabelProvider.getSignalWidth(aSignal), this);
            fSignals.put(aSignal, s);
        }
        return s;
    }

    /**
	 * Create VG* wrappers around underlying graph model
	 * 
	 */
    private void generateWrappers() {
        fBoxes = new ArrayList<VGBox<NodeType, PortType, SignalType>>();
        fPrimaryPorts = new HashMapArray<PortType, VGBox<NodeType, PortType, SignalType>>();
        fNodeBoxes = new HashMapArray<NodeType, VGBox<NodeType, PortType, SignalType>>();
        fPortMap = new HashMapArray<PortType, VGPort<NodeType, PortType, SignalType>>();
        fSignals = new HashMapArray<SignalType, VGSignal<NodeType, PortType, SignalType>>();
        fExpandablePorts = new HashSetArray<VGPort<NodeType, PortType, SignalType>>();
        NodeType root = fContentProvider.getRoot();
        int n = fContentProvider.getNumPorts(root);
        for (int i = 0; i < n; i++) {
            PortType port = fContentProvider.getPort(root, i);
            VGBox<NodeType, PortType, SignalType> box = new VGBox<NodeType, PortType, SignalType>(null, port, this, fExpandablePorts);
            fPrimaryPorts.put(port, box);
            fBoxes.add(box);
        }
        n = fContentProvider.getNumChildren(root);
        for (int i = 0; i < n; i++) {
            NodeType node = fContentProvider.getChild(root, i);
            if (!fContentProvider.isNodeVisible(node)) {
                continue;
            }
            VGBox<NodeType, PortType, SignalType> box = new VGBox<NodeType, PortType, SignalType>(node, null, this, fExpandablePorts);
            fNodeBoxes.put(node, box);
            fBoxes.add(box);
            int m = box.getNumPorts();
            for (int j = 0; j < m; j++) {
                VGPort<NodeType, PortType, SignalType> port = box.getPort(j);
                fPortMap.put(port.getPort(), port);
            }
        }
        n = fBoxes.size();
        for (int i = 0; i < n; i++) {
            VGBox<NodeType, PortType, SignalType> box = fBoxes.get(i);
            box.compute(fContentProvider);
        }
    }

    /**
	 * calculates the depth of each node and stores it into a HashMap. Also
	 * generates the reverse mapping of levels to lists of nodes
	 */
    private void levelize() {
        fNodeDepth = new HashMap<VGBox<NodeType, PortType, SignalType>, Integer>();
        HashMap<VGBox<NodeType, PortType, SignalType>, Integer> reached = new HashMap<VGBox<NodeType, PortType, SignalType>, Integer>();
        LinkedList<VGBox<NodeType, PortType, SignalType>> queue = new LinkedList<VGBox<NodeType, PortType, SignalType>>();
        HashSet<VGBox<NodeType, PortType, SignalType>> todo = new HashSet<VGBox<NodeType, PortType, SignalType>>();
        int n = fBoxes.size();
        for (int i = 0; i < n; i++) {
            VGBox<NodeType, PortType, SignalType> box = fBoxes.get(i);
            todo.add(box);
            reached.put(box, 0);
            if (box.getNumDrivers() == 0) {
                queue.add(box);
                fNodeDepth.put(box, 0);
            }
        }
        fMaxDepth = 0;
        int numTodo = todo.size();
        int count = 0;
        int pold = 0;
        while (true) {
            int pc = 0;
            if (numTodo > 0) pc = count * 100 / numTodo;
            if (pc > pold) {
                pold = pc;
            }
            count++;
            while (!queue.isEmpty()) {
                VGBox<NodeType, PortType, SignalType> box = queue.poll();
                if (!todo.contains(box)) {
                    continue;
                }
                int depth = fNodeDepth.get(box);
                todo.remove(box);
                n = box.getNumReceivers();
                for (int i = 0; i < n; i++) {
                    VGBox<NodeType, PortType, SignalType> receiver = box.getReceiver(i);
                    if (todo.contains(receiver)) {
                        Integer rdepth = fNodeDepth.get(receiver);
                        if (rdepth == null) {
                            fNodeDepth.put(receiver, depth + 1);
                            if (depth >= fMaxDepth) {
                                fMaxDepth = depth + 1;
                            }
                        } else {
                            int r_depth = rdepth.intValue();
                            if (depth >= r_depth) {
                                fNodeDepth.put(receiver, depth + 1);
                                if (depth >= fMaxDepth) {
                                    fMaxDepth = depth + 1;
                                }
                            }
                        }
                        int numReached = reached.get(receiver);
                        numReached++;
                        reached.put(receiver, numReached);
                        int numDrivers = receiver.getNumDrivers();
                        if (numDrivers == numReached) queue.add(receiver);
                    }
                }
            }
            if (!todo.isEmpty()) {
                int mostOftenReached = 0;
                for (Iterator<VGBox<NodeType, PortType, SignalType>> i = todo.iterator(); i.hasNext(); ) {
                    VGBox<NodeType, PortType, SignalType> box = i.next();
                    int numReached = reached.get(box);
                    if (numReached > mostOftenReached) mostOftenReached = numReached;
                }
                for (Iterator<VGBox<NodeType, PortType, SignalType>> i = todo.iterator(); i.hasNext(); ) {
                    VGBox<NodeType, PortType, SignalType> box = i.next();
                    int numReached = reached.get(box);
                    if (numReached == mostOftenReached) {
                        queue.add(box);
                        fNodeDepth.put(box, fMaxDepth);
                    }
                }
            } else break;
        }
        fDepthNode = new HashMap<Integer, ArrayList<VGBox<NodeType, PortType, SignalType>>>();
        n = fBoxes.size();
        for (int i = 0; i < n; i++) {
            VGBox<NodeType, PortType, SignalType> box = fBoxes.get(i);
            Integer depth = fNodeDepth.get(box);
            if (depth == null) continue;
            ArrayList<VGBox<NodeType, PortType, SignalType>> list = fDepthNode.get(depth);
            if (list == null) {
                list = new ArrayList<VGBox<NodeType, PortType, SignalType>>();
            }
            list.add(box);
            fDepthNode.put(depth, list);
        }
    }

    /**
	 * place VBoxes on channels according to their logic depth
	 * 
	 */
    private void prePlace() {
        levelize();
        fChannels = new ArrayList<VGChannel<NodeType, PortType, SignalType>>(fMaxDepth + 1);
        for (int i = 0; i < fMaxDepth + 1; i++) {
            fChannels.add(new VGChannel<NodeType, PortType, SignalType>(this, i));
        }
        int n = fBoxes.size();
        ZStack<VGBox<NodeType, PortType, SignalType>> stack = new ZStack<VGBox<NodeType, PortType, SignalType>>();
        HashSet<VGBox<NodeType, PortType, SignalType>> todo = new HashSet<VGBox<NodeType, PortType, SignalType>>(n);
        for (int i = 0; i < n; i++) {
            VGBox<NodeType, PortType, SignalType> box = fBoxes.get(i);
            todo.add(box);
            Integer depth = fNodeDepth.get(box);
            if (depth != null && depth.intValue() == 0) {
                stack.push(box);
            }
        }
        while (!todo.isEmpty()) {
            while (!stack.isEmpty()) {
                VGBox<NodeType, PortType, SignalType> box = stack.pop();
                if (!todo.contains(box)) continue;
                todo.remove(box);
                int col = fNodeDepth.get(box);
                box.setCol(col);
                fChannels.get(col).add(box);
                logger.debug("VGLayout: Box %s is in column %d", box, col);
                int m = box.getNumReceivers();
                for (int j = 0; j < m; j++) {
                    VGBox<NodeType, PortType, SignalType> receiver = box.getReceiver(j);
                    if (todo.contains(receiver)) stack.push(receiver);
                }
                m = box.getNumDrivers();
                for (int j = 0; j < m; j++) {
                    VGBox<NodeType, PortType, SignalType> driver = box.getDriver(j);
                    if (todo.contains(driver)) stack.push(driver);
                }
            }
            if (!todo.isEmpty()) stack.push(todo.iterator().next());
        }
        n = fChannels.size();
        for (int i = 0; i < n; i++) {
            logger.debug("VGLayout: Channel %d contains:", i);
            VGChannel<NodeType, PortType, SignalType> channel = fChannels.get(i);
            int m = channel.getNumBoxes();
            for (int j = 0; j < m; j++) {
                VGBox<NodeType, PortType, SignalType> box = channel.getBox(j);
                logger.debug("VGLayout:    %s", box);
            }
        }
    }

    Position getPortPosition(VGPort<NodeType, PortType, SignalType> aPort) {
        return aPort.getBox().getPortPosition(aPort);
    }

    private int optimizeRouting() {
        int score = Integer.MAX_VALUE;
        int n = fChannels.size();
        for (int i = 0; i < n; i++) {
            VGChannel<NodeType, PortType, SignalType> channel = fChannels.get(i);
            score -= channel.route();
        }
        return score;
    }

    private int calcBoxPlacementScore(VGBox<NodeType, PortType, SignalType> aBox) {
        int col = aBox.getCol();
        VGChannel<NodeType, PortType, SignalType> c = fChannels.get(col);
        int ymin1 = aBox.getYPos();
        int ymax1 = ymin1 + aBox.getHeight();
        int n = c.getNumBoxes();
        for (int i = 0; i < n; i++) {
            VGBox<NodeType, PortType, SignalType> b2 = c.getBox(i);
            if (b2 == aBox) continue;
            int ymin2 = b2.getYPos();
            int ymax2 = ymin2 + b2.getHeight();
            if (ymin2 <= ymin1 && ymax2 >= ymin1) return Integer.MIN_VALUE;
            if (ymin2 >= ymin1 && ymax2 <= ymax1) return Integer.MIN_VALUE;
            if (ymin2 <= ymax1 && ymax2 >= ymax1) return Integer.MIN_VALUE;
        }
        return optimizeRouting();
    }

    private void optimizeYPlacementPorts(VGBox<NodeType, PortType, SignalType> aBox) {
        int bestScore = calcBoxPlacementScore(aBox);
        int bestYPos = aBox.getYPos();
        int l = aBox.getNumPorts();
        for (int k = 0; k < l; k++) {
            VGPort<NodeType, PortType, SignalType> p = aBox.getPort(k);
            VGSignal<NodeType, PortType, SignalType> s = p.getSignal();
            if (s == null) continue;
            Position offset = aBox.getPortOffset(p);
            int nConns = s.getNumConnections();
            for (int iConn = 0; iConn < nConns; iConn++) {
                VGPort<NodeType, PortType, SignalType> p2 = s.getConnection(iConn);
                if (p2 == p) continue;
                int ypos = getPortPosition(p2).getY() - offset.getY();
                if (ypos < 0) continue;
                aBox.setYPos(ypos);
                int score = calcBoxPlacementScore(aBox);
                if (score > bestScore) {
                    bestScore = score;
                    bestYPos = ypos;
                }
            }
        }
        logger.debug("VGLayout: ******* %s best Y pos score: %d at YPos: %d", aBox, bestScore, bestYPos);
        aBox.setYPos(bestYPos);
    }

    private void optimizeYPlacementIncr(VGBox<NodeType, PortType, SignalType> aBox) {
        int bestScore = calcBoxPlacementScore(aBox);
        int bestYPos = aBox.getYPos();
        int step = aBox.getHeight();
        while (step > 0) {
            int yp1 = bestYPos + step;
            aBox.setYPos(yp1);
            int score1 = calcBoxPlacementScore(aBox);
            int yp2 = bestYPos - step;
            aBox.setYPos(yp2);
            int score2 = calcBoxPlacementScore(aBox);
            if (score1 > score2) {
                if (score1 > bestScore) {
                    bestScore = score1;
                    bestYPos = yp1;
                } else {
                    step /= 2;
                }
            } else {
                if (score2 > bestScore) {
                    bestScore = score2;
                    bestYPos = yp2;
                } else {
                    step /= 2;
                }
            }
        }
        aBox.setYPos(bestYPos);
    }

    private void optimizeYPlacementBySwapping(VGBox<NodeType, PortType, SignalType> aBox, VGBox<NodeType, PortType, SignalType> aBox2) {
        int score1 = calcBoxPlacementScore(aBox);
        int score2 = calcBoxPlacementScore(aBox2);
        int score = score1 + score2;
        int y1 = aBox.getYPos();
        int y2 = aBox2.getYPos();
        aBox.setYPos(y2);
        aBox2.setYPos(y1);
        int nscore1 = calcBoxPlacementScore(aBox);
        int nscore2 = calcBoxPlacementScore(aBox2);
        int nscore = nscore1 + nscore2;
        if (nscore <= score) {
            aBox.setYPos(y1);
            aBox2.setYPos(y2);
        }
    }

    private void initialYPlacement() {
        int n = fChannels.size();
        ZStack<VGBox<NodeType, PortType, SignalType>> todo = new ZStack<VGBox<NodeType, PortType, SignalType>>();
        HashSet<VGBox<NodeType, PortType, SignalType>> done = new HashSet<VGBox<NodeType, PortType, SignalType>>();
        int maxY[] = new int[n];
        for (int i = 0; i < n; i++) {
            maxY[i] = 0;
        }
        VGChannel<NodeType, PortType, SignalType> c = fChannels.get(n - 1);
        int m = c.getNumBoxes();
        for (int j = 0; j < m; j++) {
            VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
            if (box == null) continue;
            todo.push(box);
        }
        while (!todo.isEmpty()) {
            VGBox<NodeType, PortType, SignalType> box = todo.pop();
            if (done.contains(box)) {
                continue;
            }
            done.add(box);
            int ypos = Integer.MIN_VALUE;
            int l = box.getNumPorts();
            for (int k = 0; k < l; k++) {
                VGPort<NodeType, PortType, SignalType> p = box.getPort(k);
                if (!p.isOutput()) {
                    continue;
                }
                VGSignal<NodeType, PortType, SignalType> s = p.getSignal();
                if (s == null) continue;
                Position offset = box.getPortOffset(p);
                int nConns = s.getNumConnections();
                for (int iConn = 0; iConn < nConns; iConn++) {
                    VGPort<NodeType, PortType, SignalType> p2 = s.getConnection(iConn);
                    if (p2 == p) continue;
                    ypos = getPortPosition(p2).getY() - offset.getY();
                    break;
                }
                if (ypos > Integer.MIN_VALUE) {
                    break;
                }
            }
            int ci = box.getCol();
            if (ypos == Integer.MIN_VALUE) {
                ypos = maxY[ci];
            }
            int ypos2 = ypos + box.getHeight() + 1;
            if (ypos2 > maxY[ci]) {
                maxY[ci] = ypos2;
            }
            box.setYPos(ypos);
            m = box.getNumDrivers();
            for (int i = 0; i < m; i++) {
                VGBox<NodeType, PortType, SignalType> driver = box.getDriver(i);
                todo.push(driver);
            }
        }
    }

    private void computeYPlacement() {
        initialYPlacement();
        int n = fChannels.size();
        long startTime = System.currentTimeMillis();
        int bestScore = Integer.MIN_VALUE;
        for (int r = 0; r < PLACEMENT_OPT_NUM_ITERATIONS; r++) {
            logger.debug("VGLayout: ===================== optimize rtl module placement iter=%d ===========", r);
            for (int i = 0; i < n; i++) {
                logger.debug("VGLayout: VGChannel: %d", i);
                VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
                int m = c.getNumBoxes();
                for (int j = 0; j < m; j++) {
                    VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
                    if (box == null) continue;
                    for (int k = j + 1; k < m; k++) {
                        VGBox<NodeType, PortType, SignalType> box2 = c.getBox(k);
                        if (box2 == null) continue;
                        optimizeYPlacementBySwapping(box, box2);
                    }
                }
            }
            for (int i = 1; i < n; i++) {
                logger.debug("VGLayout: PORTS VGChannel: %d", i);
                VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
                int m = c.getNumBoxes();
                for (int j = 0; j < m; j++) {
                    VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
                    if (box == null) continue;
                    optimizeYPlacementPorts(box);
                }
            }
            for (int i = n - 1; i >= 0; i--) {
                logger.debug("VGLayout: PORTS VGChannel: %d", i);
                VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
                int m = c.getNumBoxes();
                for (int j = 0; j < m; j++) {
                    VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
                    if (box == null) continue;
                    optimizeYPlacementPorts(box);
                }
            }
            for (int i = 0; i < n; i++) {
                logger.debug("VGLayout: GAPS VGChannel: %d", i);
                VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
                int m = c.getNumBoxes();
                for (int j = 0; j < m; j++) {
                    VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
                    if (box == null) continue;
                    for (int k = 0; k < m; k++) {
                        VGBox<NodeType, PortType, SignalType> box2 = c.getBox(k);
                        if (box2 == null) continue;
                        if (box2 == box) continue;
                        int score1 = calcBoxPlacementScore(box);
                        int y1 = box.getYPos();
                        box.setYPos(box2.getYPos() + box2.getHeight() + 1);
                        int score2 = calcBoxPlacementScore(box);
                        if (score2 < score1) {
                            box.setYPos(y1);
                        }
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                logger.debug("VGLayout: INCR VGChannel: %d", i);
                VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
                int m = c.getNumBoxes();
                for (int j = 0; j < m; j++) {
                    VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
                    if (box == null) continue;
                    optimizeYPlacementIncr(box);
                }
            }
            int score = optimizeRouting();
            logger.debug("VGLayout: Overall score: %d, best score so far: %d", score, bestScore);
            if (score <= bestScore) {
                break;
            }
            bestScore = score;
            double time = (System.currentTimeMillis() - startTime) / 1000.0;
            if (time > PLACEMENT_OPT_TIMEOUT) {
                break;
            }
        }
        int minY = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
            int m = c.getNumBoxes();
            for (int j = 0; j < m; j++) {
                VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
                if (box == null) continue;
                int y = box.getYPos();
                if (y < minY) {
                    minY = y;
                }
            }
        }
        int offset = TOP_MARGIN - minY;
        for (int i = 0; i < n; i++) {
            VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
            int m = c.getNumBoxes();
            for (int j = 0; j < m; j++) {
                VGBox<NodeType, PortType, SignalType> box = c.getBox(j);
                if (box == null) continue;
                int y = box.getYPos();
                box.setYPos(y + offset);
            }
        }
        optimizeRouting();
    }

    private void routeSignal(VGSignal<NodeType, PortType, SignalType> s, VGPort<NodeType, PortType, SignalType> source, VGPort<NodeType, PortType, SignalType> dest) {
        int sc = getCol(source);
        int dc = getCol(dest);
        if (source.isOutput()) sc++;
        if (dest.isOutput()) dc++;
        if (sc > dc) {
            int tmp;
            tmp = sc;
            sc = dc;
            dc = tmp;
            VGPort<NodeType, PortType, SignalType> tmpPrt = source;
            source = dest;
            dest = tmpPrt;
        }
        VGChannel<NodeType, PortType, SignalType> c = getChannel(sc);
        c.addPortConnection(source, s);
        while (sc < dc) {
            sc++;
            VGChannel<NodeType, PortType, SignalType> c2 = getChannel(sc);
            c.addChannelConnRight(s, c2);
            c2.addChannelConnLeft(s, c);
            c = c2;
        }
        c.addPortConnection(dest, s);
    }

    private void routeSignal(VGSignal<NodeType, PortType, SignalType> signal) {
        int pos = Integer.MAX_VALUE;
        VGPort<NodeType, PortType, SignalType> sp = null;
        int m = signal.getNumConnections();
        for (int j = 0; j < m; j++) {
            VGPort<NodeType, PortType, SignalType> port = signal.getConnection(j);
            VGBox<NodeType, PortType, SignalType> box = port.getBox();
            int d = box.getCol();
            if (d < pos) {
                pos = d;
                sp = port;
            }
        }
        if (sp == null) return;
        int num = signal.getNumConnections();
        for (int j = 0; j < num; j++) {
            VGPort<NodeType, PortType, SignalType> dp = signal.getConnection(j);
            if (sp == dp) continue;
            routeSignal(signal, sp, dp);
        }
    }

    private void route() {
        HashSet<VGSignal<NodeType, PortType, SignalType>> routedSignals = new HashSet<VGSignal<NodeType, PortType, SignalType>>();
        int n = fBoxes.size();
        for (int i = 0; i < n; i++) {
            VGBox<NodeType, PortType, SignalType> box = fBoxes.get(i);
            int m = box.getNumPorts();
            for (int j = 0; j < m; j++) {
                VGPort<NodeType, PortType, SignalType> port = box.getPort(j);
                VGSignal<NodeType, PortType, SignalType> signal = port.getSignal();
                if (signal == null || routedSignals.contains(signal)) {
                    continue;
                }
                routeSignal(signal);
                routedSignals.add(signal);
            }
        }
    }

    private void computeChannelXPositions() {
        int w = LEFT_MARGIN;
        int h = 0;
        int n = fChannels.size();
        for (int i = 0; i < n; i++) {
            VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
            c.setXPos(w);
            w += c.getWidth();
            int hh = c.getHeight();
            if (hh > h) h = hh;
        }
        fTotalSize = new Position(w + 100, h + 100);
    }

    private void placeAndRoute() {
        fTotalSize = new Position(50, 50);
        logger.info("VGLayout: layout starts on %s", fContentProvider);
        logger.info("VGLayout: ======================================================");
        long startTime = System.currentTimeMillis();
        logger.debug("VGLayout: 1/5: generateWrappers()");
        generateWrappers();
        logger.debug("VGLayout: 2/5: prePlace()");
        prePlace();
        double time = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.debug("VGLayout: time elapsed so far: " + time + "s.");
        logger.debug("VGLayout: 3/5: route()");
        route();
        time = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.debug("VGLayout: time elapsed so far: " + time + "s.");
        logger.debug("VGLayout: 4/5: computeYPlacement()");
        computeYPlacement();
        time = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.debug("VGLayout: time elapsed so far: " + time + "s.");
        logger.debug("VGLayout: 5/5: computeChannelXPositions()");
        computeChannelXPositions();
        time = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.debug("VGLayout: done. total time elapsed: " + time + "s.\n");
    }

    public void paint(VGSelectionProvider<NodeType, SignalType> aSelectionProvider) {
        fGC.start(fTotalSize.getX(), fTotalSize.getY());
        int n = fChannels.size();
        for (int i = 0; i < n; i++) {
            VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
            c.paint(aSelectionProvider);
        }
        if (fContentProvider.isDynamicMode()) {
            fGC.setBackground(VGColor.BACKGROUND);
            fGC.setForeground(VGColor.MODULE);
            fGC.setLineWidth(1);
            n = fExpandablePorts.size();
            for (int i = 0; i < n; i++) {
                VGPort<NodeType, PortType, SignalType> p = fExpandablePorts.get(i);
                VGBox<NodeType, PortType, SignalType> box = p.getBox();
                if (aSelectionProvider.isNodeSelected(box.getNode())) {
                    fGC.setForeground(VGColor.HIGHLIGHT);
                } else {
                    fGC.setForeground(VGColor.MODULE);
                }
                Position pos = getPortPosition(p);
                logger.debug("Got position %s for expandable port %s", pos, p);
                fGC.fillRectangle(pos.getX() - 5, pos.getY() - 5, 10, 10);
                fGC.drawLine(pos.getX() - 3, pos.getY(), pos.getX() + 3, pos.getY());
                fGC.drawLine(pos.getX(), pos.getY() - 3, pos.getX(), pos.getY() + 3);
            }
        }
        fGC.finish();
    }

    VGChannel<NodeType, PortType, SignalType> getChannel(int aIdx) {
        return fChannels.get(aIdx);
    }

    VGContentProvider<NodeType, PortType, SignalType> getContentProvider() {
        return fContentProvider;
    }

    VGLabelProvider<NodeType, PortType, SignalType> getLabelProvider() {
        return fLabelProvider;
    }

    public VGBox<NodeType, PortType, SignalType> getNodeBox(NodeType aNode) {
        return fNodeBoxes.get(aNode);
    }

    VGBox<NodeType, PortType, SignalType> getPortBox(PortType aPort) {
        NodeType node = fContentProvider.getNode(aPort);
        VGBox<NodeType, PortType, SignalType> box = fNodeBoxes.get(node);
        if (box != null) {
            return box;
        }
        return fPrimaryPorts.get(aPort);
    }

    private int getCol(VGBox<NodeType, PortType, SignalType> aBox) {
        return aBox.getCol();
    }

    private int getCol(VGPort<NodeType, PortType, SignalType> aPort) {
        return getCol(aPort.getBox());
    }

    public VGGC getGC() {
        return fGC;
    }

    public Position getTotalSize() {
        return fTotalSize;
    }

    public int getNumExpandablePorts() {
        return fExpandablePorts.size();
    }

    public PortType getExpandablePort(int aIdx) {
        return fExpandablePorts.get(aIdx).getPort();
    }

    public PortType checkHitExpandablePort(int aX, int aY, int aPointerSize) {
        int n = fExpandablePorts.size();
        for (int i = 0; i < n; i++) {
            VGPort<NodeType, PortType, SignalType> port = fExpandablePorts.get(i);
            Position pos = getPortPosition(port);
            if (pos.getX() - 5 <= aX + aPointerSize && pos.getY() - 5 <= aY + aPointerSize && pos.getX() + 5 >= aX - aPointerSize && pos.getY() <= aY - aPointerSize) {
                return port.getPort();
            }
        }
        return null;
    }

    public SignalType checkHitSignal(int aX, int aY, int aPointerSize) {
        int n = fChannels.size();
        for (int i = 0; i < n; i++) {
            VGChannel<NodeType, PortType, SignalType> c = fChannels.get(i);
            SignalType s = c.checkSignalHit(aX, aY, aPointerSize);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public NodeType checkHitNode(int aX, int aY, int aPointerSize) {
        int n = fBoxes.size();
        for (int i = 0; i < n; i++) {
            VGBox<NodeType, PortType, SignalType> box = fBoxes.get(i);
            if (box.isHit(aX, aY, aPointerSize)) {
                return box.getNode();
            }
        }
        return null;
    }

    public Position getPortPosition(PortType aPort) {
        VGPort<NodeType, PortType, SignalType> p = fPortMap.get(aPort);
        if (p == null) {
            return null;
        }
        return getPortPosition(p);
    }
}
