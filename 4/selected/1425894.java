package avrora.sim.platform;

import avrora.sim.Simulator;
import avrora.sim.SimulatorThread;
import avrora.sim.clock.StepSynchronizer;
import avrora.sim.clock.Synchronizer;
import avrora.sim.mcu.Microcontroller;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Very simple implementation of pin interconnect between microcontrollers
 *
 * @author Jacob Everist
 */
public class PinConnect {

    public static final PinConnect pinConnect;

    static {
        pinConnect = new PinConnect();
    }

    private final PinEvent pinEvent;

    public final Synchronizer synchronizer;

    protected LinkedList pinNodes;

    protected LinkedList pinConnections;

    protected int numNodes;

    public static final int NONE = -1;

    public static final int NORTH = 0;

    public static final int EAST = 1;

    public static final int SOUTH = 2;

    public static final int WEST = 3;

    public static final int LED1 = 0;

    public static final int LED2 = 1;

    public static final int LED3 = 2;

    public static final int LED4 = 3;

    public static final int LED5 = 4;

    public static final int LED6 = 5;

    public PinConnect() {
        pinNodes = new LinkedList();
        pinConnections = new LinkedList();
        pinEvent = new PinEvent();
        synchronizer = new StepSynchronizer(pinEvent);
        numNodes = 0;
    }

    public void addSeresNode(Microcontroller mcu, PinWire northTx, PinWire eastTx, PinWire southTx, PinWire westTx, PinWire northRx, PinWire eastRx, PinWire southRx, PinWire westRx, PinWire northInt, PinWire eastInt, PinWire southInt, PinWire westInt) {
        pinNodes.add(new PinNode(mcu, northTx, eastTx, southTx, westTx, northRx, eastRx, southRx, westRx, northInt, eastInt, southInt, westInt, numNodes));
        numNodes++;
    }

    public void addSuperbotNode(Microcontroller mcu, PinWire LED1Tx, PinWire LED2Tx, PinWire LED3Tx, PinWire LED4Tx, PinWire LED5Tx, PinWire LED6Tx, PinWire LED1Rx, PinWire LED2Rx, PinWire LED3Rx, PinWire LED4Rx, PinWire LED5Rx, PinWire LED6Rx, PinWire LED1Int, PinWire LED2Int, PinWire LED3Int, PinWire LED4Int, PinWire LED5Int, PinWire LED6Int) {
        pinNodes.add(new PinNode(mcu, LED1Tx, LED2Tx, LED3Tx, LED4Tx, LED5Tx, LED6Tx, LED1Rx, LED2Rx, LED3Rx, LED4Rx, LED5Rx, LED6Rx, LED1Int, LED2Int, LED3Int, LED4Int, LED5Int, LED6Int, numNodes));
        numNodes++;
    }

    public void addSimulatorThread(SimulatorThread simThread) {
        Simulator sim = simThread.getSimulator();
        Microcontroller currMCU = sim.getMicrocontroller();
        Iterator i = pinNodes.iterator();
        while (i.hasNext()) {
            PinNode p = (PinNode) i.next();
            if (currMCU == p.mcu) {
            }
        }
    }

    /**
     * Initialize the connections with a default topology of
     * a chain with connections on the north and south ports
     */
    public void initializeConnections() {
        Iterator i = pinNodes.iterator();
        if (!i.hasNext()) {
            return;
        }
        PinNode prevNode = (PinNode) i.next();
        while (i.hasNext()) {
            PinNode currNode = (PinNode) i.next();
            if ("SERES".equalsIgnoreCase(prevNode.platform)) {
                prevNode.connectNodes(currNode, SOUTH, SOUTH);
                prevNode.connectNodes(currNode, NORTH, NORTH);
                prevNode.connectNodes(currNode, EAST, EAST);
                prevNode.connectNodes(currNode, WEST, WEST);
            } else if ("Superbot".equalsIgnoreCase(prevNode.platform)) {
                prevNode.connectNodes(currNode, LED1, LED1);
                prevNode.connectNodes(currNode, LED2, LED2);
                prevNode.connectNodes(currNode, LED3, LED3);
                prevNode.connectNodes(currNode, LED4, LED4);
                prevNode.connectNodes(currNode, LED5, LED5);
                prevNode.connectNodes(currNode, LED6, LED6);
            } else {
                System.out.println("Unrecognized platform " + prevNode.platform);
            }
            prevNode = currNode;
        }
    }

    /**
     * This class stores all the information for a single controller node
     * and its PinWires.
     *
     * @author Jacob Everist
     */
    protected class PinNode {

        public Microcontroller mcu;

        protected PinWire[] TxPins;

        protected PinWire[] RxPins;

        protected PinWire[] IntPins;

        private int localNode;

        public PinNode[] neighborNodes;

        public int[] neighborSides;

        public final String platform;

        public PinNode(Microcontroller mcu, PinWire northTx, PinWire eastTx, PinWire southTx, PinWire westTx, PinWire northRx, PinWire eastRx, PinWire southRx, PinWire westRx, PinWire northInt, PinWire eastInt, PinWire southInt, PinWire westInt, int node) {
            this.mcu = mcu;
            TxPins = new PinWire[] { northTx, eastTx, southTx, westTx };
            RxPins = new PinWire[] { northRx, eastRx, southRx, westRx };
            IntPins = new PinWire[] { northInt, eastInt, southInt, westInt };
            neighborNodes = new PinNode[] { null, null, null, null };
            neighborSides = new int[] { NONE, NONE, NONE, NONE };
            localNode = node;
            platform = "SERES";
        }

        public PinNode(Microcontroller mcu, PinWire LED1Tx, PinWire LED2Tx, PinWire LED3Tx, PinWire LED4Tx, PinWire LED5Tx, PinWire LED6Tx, PinWire LED1Rx, PinWire LED2Rx, PinWire LED3Rx, PinWire LED4Rx, PinWire LED5Rx, PinWire LED6Rx, PinWire LED1Int, PinWire LED2Int, PinWire LED3Int, PinWire LED4Int, PinWire LED5Int, PinWire LED6Int, int node) {
            this.mcu = mcu;
            TxPins = new PinWire[] { LED1Tx, LED2Tx, LED3Tx, LED4Tx, LED5Tx, LED6Tx };
            RxPins = new PinWire[] { LED1Rx, LED2Rx, LED3Rx, LED4Rx, LED5Rx, LED6Rx };
            IntPins = new PinWire[] { LED1Int, LED2Int, LED3Int, LED4Int, LED5Int, LED6Int };
            neighborNodes = new PinNode[] { null, null, null, null, null, null };
            neighborSides = new int[] { NONE, NONE, NONE, NONE, NONE, NONE };
            localNode = node;
            platform = "Superbot";
        }

        public void connectNodes(PinNode neighbor, int localSide, int neighborSide) {
            if (neighborNodes[localSide] != null || neighbor.neighborNodes[neighborSide] != null) return;
            neighborNodes[localSide] = neighbor;
            neighborSides[localSide] = neighborSide;
            neighbor.neighborNodes[neighborSide] = this;
            neighbor.neighborSides[neighborSide] = localSide;
            PinLink localToNeighbor = new PinLink(TxPins[localSide]);
            localToNeighbor.outputNode = this;
            localToNeighbor.outputSide = localSide;
            localToNeighbor.inputNode = neighbor;
            localToNeighbor.inputSide = neighborSide;
            localToNeighbor.addInputPin(neighbor.RxPins[neighborSide]);
            localToNeighbor.addInputPin(neighbor.IntPins[neighborSide]);
            PinLink neighborToLocal = new PinLink(neighbor.TxPins[neighborSide]);
            neighborToLocal.outputNode = neighbor;
            neighborToLocal.outputSide = neighborSide;
            neighborToLocal.inputNode = this;
            neighborToLocal.inputSide = localSide;
            neighborToLocal.addInputPin(RxPins[localSide]);
            neighborToLocal.addInputPin(IntPins[localSide]);
            pinConnections.add(localToNeighbor);
            pinConnections.add(neighborToLocal);
        }

        public void disconnectNodes(PinNode neighbor, int localSide, int neighborSide) {
            neighborNodes[localSide] = null;
            neighborSides[localSide] = NONE;
            neighbor.neighborNodes[neighborSide] = null;
            neighbor.neighborSides[neighborSide] = NONE;
            Iterator i = pinConnections.iterator();
            while (i.hasNext()) {
                PinLink curr = (PinLink) i.next();
                if (curr.outputNode == this && curr.outputSide == localSide && curr.inputNode == neighbor && curr.inputSide == neighborSide) {
                    pinConnections.remove(curr);
                }
            }
            i = pinConnections.iterator();
            while (i.hasNext()) {
                PinLink curr = (PinLink) i.next();
                if (curr.outputNode == neighbor && curr.outputSide == neighborSide && curr.inputNode == this && curr.inputSide == localSide) {
                    pinConnections.remove(curr);
                }
            }
        }
    }

    /**
     * This class connects two PinNode devices together in two-way communication
     *
     * @author Jacob Everist
     */
    protected class PinLink {

        protected LinkedList pinWires;

        protected int currentDelay;

        public PinNode outputNode;

        public int outputSide;

        public PinNode inputNode;

        public int inputSide;

        public PinLink(PinWire outputPin) {
            pinWires = new LinkedList();
            outputPin.wireOutput.enableOutput();
            pinWires.add(outputPin);
        }

        public void addInputPin(PinWire inputPin) {
            inputPin.wireInput.enableInput();
            pinWires.add(inputPin);
        }

        public void propagateSignals() {
            Iterator i = pinWires.iterator();
            PinWire currOutput = null;
            while (i.hasNext()) {
                PinWire curr = (PinWire) i.next();
                if (curr.outputReady()) {
                    if (currOutput != null) {
                        String s = "ERROR: More than one output wire on this PinLink";
                        System.out.println(s);
                        return;
                    } else {
                        currOutput = curr;
                    }
                }
            }
            if (currOutput == null) {
            } else {
                i = pinWires.iterator();
                while (i.hasNext()) {
                    PinWire curr = (PinWire) i.next();
                    if (curr != currOutput) {
                        curr.wireOutput.write(currOutput.wireInput.read());
                    }
                }
            }
        }
    }

    protected class PinEvent implements Simulator.Event {

        public void fire() {
            Iterator i = pinConnections.iterator();
            PinLink currLink = (PinConnect.PinLink) i.next();
            currLink.propagateSignals();
            while (i.hasNext()) {
                currLink = (PinConnect.PinLink) i.next();
                currLink.propagateSignals();
            }
        }
    }

    /**
	 * @return Returns the pinNodes.
	 */
    public LinkedList getPinNodes() {
        return pinNodes;
    }
}
