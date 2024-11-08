package net.sf.signs.views.netlist;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Display;
import net.sf.signs.*;
import net.sf.signs.gates.PortGate;

/**
 * @author guenter
 * 
 * basically a big 2d grid to help place&route
 */
public class ChequerBoard {

    class PinConnection {

        Port port;

        int min, max;

        ArrayList<Integer> connections = new ArrayList<Integer>(2);

        public PinConnection(Port port_) {
            port = port_;
            min = -1;
            max = -1;
        }
    }

    private HashMap<Port, PinConnection> pinConnections;

    private HashMap<Gate, GBox> placedGates;

    private HashMap<Signal, Channel> placedSignals;

    private ArrayList<Channel> hChannels, vChannels;

    private NetList nl;

    private int width, height;

    private SymbolRegistry sr;

    public ChequerBoard(NetList nl_) {
        nl = nl_;
        sr = new SymbolRegistry();
        hChannels = new ArrayList<Channel>();
        vChannels = new ArrayList<Channel>();
        placedGates = new HashMap<Gate, GBox>();
        placedGates.put(nl_, null);
        placedSignals = new HashMap<Signal, Channel>();
        pinConnections = new HashMap<Port, PinConnection>();
        width = 0;
        height = 0;
    }

    public Object clone() {
        ChequerBoard board = new ChequerBoard(nl);
        for (Iterator i = placedGates.values().iterator(); i.hasNext(); ) {
            GBox box = (GBox) i.next();
            if (box == null) continue;
            board.place(box.getGate(), box.getCol(), box.getRow());
        }
        return board;
    }

    public GBox place(Gate gate_, int col_, int row_) {
        GBox box = new GBox(this, gate_, col_, row_);
        placedGates.put(gate_, box);
        if (width <= col_) width = col_ + 1;
        if (height <= row_) height = row_ + 1;
        Channel hc = getHC(row_);
        hc.put(col_, box);
        Channel vc = getVC(col_);
        vc.put(row_, box);
        return box;
    }

    public void place(Gate gate_, GBox box_) {
        placedGates.put(gate_, box_);
    }

    private int calcChannelPositions(ArrayList<Channel> channels) {
        int pos = 0;
        int n = channels.size();
        for (int i = 0; i < n; i++) {
            Channel channel = channels.get(i);
            channel.setPos(pos + 3);
            pos += channel.getSize() + 6;
        }
        return pos;
    }

    public int calcHChannelPositions() {
        return calcChannelPositions(hChannels);
    }

    public int calcVChannelPositions() {
        return calcChannelPositions(vChannels);
    }

    public int getRow(Gate g) {
        GBox box = (GBox) placedGates.get(g);
        if (box == null) {
            System.out.println("error: gate " + g + " not placed (getRow)");
            return 0;
        }
        return box.getRow();
    }

    public int getRow(PortBit p) {
        return getRow(p.getGate());
    }

    public int getCol(Gate g) {
        GBox box = placedGates.get(g);
        if (box == null) {
            System.out.println("error: gate " + g + " not placed (getRow)");
            return 0;
        }
        return box.getCol();
    }

    public void exchange(int col_, int row1_, int row2_) {
        Channel vc = getVC(col_);
        GBox box1 = vc.getGBox(row1_);
        GBox box2 = vc.getGBox(row2_);
        if (box1 != null) {
            Gate g1 = box1.getGate();
            Gate g2 = box2.getGate();
            box1.setGate(g2);
            box2.setGate(g1);
            placedGates.put(g1, box2);
            placedGates.put(g2, box1);
        } else {
            box2.setRow(row1_);
            vc.clearSlot(row2_);
            vc.put(row1_, box2);
            Channel hc = getHC(row2_);
            hc.clearSlot(col_);
            hc = getHC(row1_);
            hc.put(col_, box2);
        }
    }

    public int getCol(PortBit p) {
        return getCol(p.getGate());
    }

    public Channel getHC(int row) {
        return getChannel(hChannels, row, true);
    }

    public Channel getVC(int col) {
        return getChannel(vChannels, col, false);
    }

    private Channel getChannel(ArrayList<Channel> channels, int pos, boolean hor_) {
        Channel channel;
        while (pos >= channels.size()) {
            channel = new Channel(hor_, channels.size());
            channels.add(channel);
        }
        channel = (Channel) channels.get(pos);
        return channel;
    }

    public void addSignalV(Signal s_, int n_) {
        getVC(n_).addSignal(s_);
    }

    public void addSignalH(Signal s_, int n_) {
        if (!placedSignals.containsKey(s_)) {
            Channel c = getHC(n_);
            c.addSignal(s_);
            placedSignals.put(s_, c);
        }
    }

    public Channel getHChannel(Signal s_) {
        return (Channel) placedSignals.get(s_);
    }

    public int getVSignalIdx(Signal s_, int n_) {
        return getVC(n_).getSignalIdx(s_);
    }

    public Position getPortPosition(PortBit p) {
        GBox box = (GBox) placedGates.get(p.getGate());
        if (box == null) {
            System.out.println("error: gate " + p.getGate() + " not placed. (getPortPosition)");
            return new Position(0, 0);
        }
        return box.getPortPosition(p);
    }

    public int getVCPos(int n_) {
        return getVC(n_).getPos();
    }

    public GBox getGBox(Gate gate_) {
        return (GBox) placedGates.get(gate_);
    }

    /**
	 * the higher the score, the worse the placement
	 * 
	 * @param gate_
	 * @return
	 */
    private int getScore(Gate gate_, int row_) {
        int score = 0;
        ArrayList receivers = nl.getReceivers(gate_);
        int n = receivers.size();
        for (int i = 0; i < n; i++) {
            Gate receiver = (Gate) receivers.get(i);
            score += Math.abs(getRow(receiver) - row_);
        }
        ArrayList drivers = nl.getDrivers(gate_);
        n = drivers.size();
        for (int i = 0; i < n; i++) {
            Gate driver = (Gate) drivers.get(i);
            score += Math.abs(getRow(driver) - row_);
        }
        return score;
    }

    private int getScore(Gate gate_) {
        int row = getRow(gate_);
        return getScore(gate_, row);
    }

    public int getScore() {
        int score = 0;
        int n = nl.getNumSubs();
        for (int i = 0; i < n; i++) {
            Gate gate = nl.getSub(i);
            score += getScore(gate);
        }
        return score;
    }

    public void optimizePlacement() {
        int vc = (int) Math.round(vChannels.size() * Math.random());
        Channel c = getVC(vc);
        int highestGain = 0;
        int bestSlot1 = 0;
        int bestSlot2 = 0;
        for (int slot1 = 0; slot1 < height; slot1++) {
            for (int slot2 = 0; slot2 < height; slot2++) {
                int scoreBefore = 0;
                GBox box1 = c.getGBox(slot1);
                Gate gate1 = null;
                if (box1 != null) {
                    gate1 = box1.getGate();
                    scoreBefore = getScore(gate1);
                }
                GBox box2 = c.getGBox(slot2);
                Gate gate2 = null;
                if (box2 != null) {
                    gate2 = box2.getGate();
                    scoreBefore += getScore(gate2);
                }
                int scoreAfter = 0;
                if (gate1 != null) scoreAfter = getScore(gate1, slot2);
                if (gate2 != null) scoreAfter += getScore(gate2, slot1);
                int gain = scoreBefore - scoreAfter;
                if (gain != 0 && gain > highestGain) {
                    highestGain = gain;
                    bestSlot1 = slot1;
                    bestSlot2 = slot2;
                }
            }
        }
        if (bestSlot1 != bestSlot2) {
            swap(vc, bestSlot1, bestSlot2);
        }
    }

    private void swap(int col_, int slot1_, int slot2_) {
        Channel vc = getVC(col_);
        GBox box1 = vc.getGBox(slot1_);
        GBox box2 = vc.getGBox(slot2_);
        Channel hc1 = getHC(slot1_);
        hc1.put(col_, box2);
        Channel hc2 = getHC(slot2_);
        hc2.put(col_, box1);
        vc.put(slot1_, box2);
        vc.put(slot2_, box1);
        if (box1 != null) {
            box1.setRow(slot2_);
            box1.setHChannel(hc2);
        }
        if (box2 != null) {
            box2.setRow(slot1_);
            box2.setHChannel(hc1);
        }
    }

    public void setPortYPlacementHint(PortBit port_, int y_) {
        Gate gate = port_.getGate();
        if (!(gate instanceof PortGate)) return;
        PortGate p = (PortGate) gate;
        GBox box = getGBox(p);
        if (box.getYHint() == GBox.INVALID_POS) {
            Position pos = box.getPortOffset(port_);
            box.setYHint(y_ - pos.y);
            for (int i = 0; i < hChannels.size(); i++) {
                Channel hc = getHC(i);
                if (hc.getPos() <= y_) box.setRow(i);
            }
        }
    }

    public GBox getGBoxAt(int col_, int row_) {
        Channel hc = getHC(row_);
        return hc.getGBox(col_);
    }

    /**
	 * Usefull to resolve primary port collisions after hinting
	 * 
	 * @param col_
	 */
    public void resolveCollisions(int col_) {
        Channel c = getVC(col_);
        int n = c.getNumSlots();
        for (int i = 0; i < n; i++) {
            GBox box1 = c.getGBox(i);
            if (box1 == null) continue;
            int yhint = box1.getYPos();
            while (collides(col_, i)) {
                yhint += 5;
                box1.setYHint(yhint);
            }
        }
    }

    private boolean collides(int col_, int boxIdx_) {
        boolean collision = false;
        Channel c = getVC(col_);
        GBox box1 = c.getGBox(boxIdx_);
        if (box1 == null) return false;
        int y11 = box1.getYPos();
        int y12 = y11 + box1.getHeight();
        int n = c.getNumSlots();
        for (int j = 0; j < n; j++) {
            if (boxIdx_ == j) continue;
            GBox box2 = c.getGBox(j);
            if (box2 == null) continue;
            int y21 = box2.getYPos();
            int y22 = y21 + box2.getHeight();
            if (((y11 >= y21) && (y11 <= y22)) || ((y12 >= y21) && (y12 <= y22))) {
                box1.setYHint(y22 + 2);
                collision = true;
                break;
            }
        }
        return collision;
    }

    public int getWidth() {
        return width;
    }

    public Signal signalHit(NetListControl viewer_, int x_, int y_) {
        Signal res = null;
        int n = vChannels.size();
        for (int i = 0; i < n; i++) {
            Channel c = vChannels.get(i);
            res = c.signalHit(viewer_, x_, y_);
            if (res != null) return res;
        }
        n = hChannels.size();
        for (int i = 0; i < n; i++) {
            Channel c = hChannels.get(i);
            res = c.signalHit(viewer_, x_, y_);
            if (res != null) return res;
        }
        for (Iterator<Port> i = pinConnections.keySet().iterator(); i.hasNext(); ) {
            Port port = i.next();
            PinConnection pc = pinConnections.get(port);
            PortBit p;
            if (port instanceof PortAggregate) {
                PortAggregate pa = (PortAggregate) port;
                if (pa.getNumPorts() < 1) continue;
                p = pa.getPort(0);
            } else p = (PortBit) port;
            Position ppos = getPortPosition(p);
            int x1 = ppos.x;
            int x2 = pc.max;
            if (x1 > x2) {
                if (viewer_.isLineHit(x_, y_, x2, ppos.y, x1, ppos.y)) return p.getSignal();
            } else {
                if (viewer_.isLineHit(x_, y_, x1, ppos.y, x2, ppos.y)) return p.getSignal();
            }
        }
        return res;
    }

    public void paint(NetListControl viewer, Display display, GC gc) {
        int n = vChannels.size();
        for (int i = 0; i < n; i++) {
            Channel c = vChannels.get(i);
            c.paint(viewer, display, gc);
        }
        n = hChannels.size();
        for (int i = 0; i < n; i++) {
            Channel c = hChannels.get(i);
            c.paint(viewer, display, gc);
        }
        for (Iterator<Port> i = pinConnections.keySet().iterator(); i.hasNext(); ) {
            Port port = i.next();
            PinConnection pc = pinConnections.get(port);
            PortBit p;
            double w = 1.0;
            if (port instanceof PortAggregate) {
                w = 5.0;
                PortAggregate pa = (PortAggregate) port;
                if (pa.getNumPorts() < 1) continue;
                p = pa.getPort(0);
            } else p = (PortBit) port;
            boolean selected = viewer.isSignalHilight(p.getSignal());
            if (selected) {
                gc.setLineWidth((int) (w * 4 * viewer.getZoomFactor()));
                gc.setForeground(viewer.getColorScheme().getHilightColor());
            } else {
                gc.setLineWidth((int) (w * viewer.getZoomFactor()));
                gc.setForeground(viewer.getColorScheme().getSignalColor());
            }
            Position ppos = getPortPosition(p);
            gc.drawLine(viewer.tX(ppos.x), viewer.tY(ppos.y), viewer.tX(pc.max), viewer.tY(ppos.y));
            if (selected) {
                gc.setBackground(viewer.getColorScheme().getHilightColor());
            } else {
                gc.setBackground(viewer.getColorScheme().getSignalColor());
            }
            n = pc.connections.size();
            for (int j = 0; j < n; j++) {
                int pos = pc.connections.get(j);
                if (pos == pc.max) continue;
                if (pos == pc.min) continue;
                gc.fillOval(viewer.tX(pos - 1.5 * w), viewer.tY(ppos.y - 1.5 * w), (int) (3 * w * viewer.getZoomFactor()), (int) (3 * w * viewer.getZoomFactor()));
            }
        }
    }

    public void print(PlaceAndRoute par_, PrintWriter out_) {
        int n = vChannels.size();
        for (int i = 0; i < n; i++) {
            Channel c = vChannels.get(i);
            c.print(par_, out_);
        }
        n = hChannels.size();
        for (int i = 0; i < n; i++) {
            Channel c = hChannels.get(i);
            c.print(par_, out_);
        }
        for (Iterator<Port> i = pinConnections.keySet().iterator(); i.hasNext(); ) {
            Port port = i.next();
            PinConnection pc = pinConnections.get(port);
            PortBit p;
            double w = 1.0;
            if (port instanceof PortAggregate) {
                w = 5.0;
                PortAggregate pa = (PortAggregate) port;
                if (pa.getNumPorts() < 1) continue;
                p = pa.getPort(0);
            } else p = (PortBit) port;
            Position ppos = getPortPosition(p);
            int sx = ppos.x;
            int sy = ppos.y;
            int dx = pc.max;
            PSUtils.psDrawLine(par_, out_, ppos.x, ppos.y, pc.max, ppos.y, w);
            Signal s = port.getSignal();
            if (s != null) {
                String id = s.getId();
                if (s instanceof SignalAggregate) {
                    SignalAggregate bus = (SignalAggregate) s;
                    id = bus.getId() + "(...)";
                }
                int l = id.length() * 10;
                w = dx - sx;
                if (w > l) {
                    PSUtils.psDrawText(par_, out_, sx + w / 2 - l / 2, sy - 8, id, 6);
                }
            }
            n = pc.connections.size();
            for (int j = 0; j < n; j++) {
                int pos = pc.connections.get(j);
                if (pos == pc.max) continue;
                if (pos == pc.min) continue;
                PSUtils.psDrawArc(par_, out_, pos, ppos.y, 1.5, 0, 360, false);
            }
        }
    }

    private Port getPortToPlace(PortBit p_) {
        PortAggregate pa = p_.getAggregate();
        if (pa != null) return pa;
        return p_;
    }

    public void addPinConnection(PortBit port_, int pos_) {
        Port port = getPortToPlace(port_);
        PinConnection pc = pinConnections.get(port);
        if (pc == null) {
            pc = new PinConnection(port);
            pinConnections.put(port, pc);
        }
        if (pc.min == -1) {
            pc.min = pos_;
            pc.max = pos_;
        } else {
            if (pos_ < pc.min) pc.min = pos_;
            if (pos_ > pc.max) pc.max = pos_;
        }
        pc.connections.add(pos_);
    }

    public SymbolRegistry getSymbolRegistry() {
        return sr;
    }
}
