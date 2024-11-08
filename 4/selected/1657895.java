package drcl.inet.mac;

import drcl.data.*;
import drcl.comp.*;
import drcl.net.*;
import drcl.inet.*;
import drcl.inet.contract.*;
import java.util.*;
import drcl.comp.Port;
import drcl.comp.Contract;
import java.io.*;
import java.util.StringTokenizer;
import java.lang.Math;

/**
 * This class simulates many functions of a wireless physical card. It piggy-backs
 * various information (ie. location of the sending node, the transmission power
 * of data frame etc.) to the mac layer data frame and passes that fram to the channel.
 * While receiving a data frame from the channel component, it determines whether 
 * that frame can be decoded correctly by consulting <code>RadioPropagationModel</code>
 * and passes the decodable frame to the mac layer. It also contains a <code>EnergyModel</code>
 * component to track the energy consumption.
 * @author Ye Ge
 */
public class WirelessPhy extends drcl.net.Module implements ActiveComponent {

    long nid;

    private static String STATUS_SEND = "SEND";

    private static String STATUS_RECEIVE = "RECEIVE";

    private static String STATUS_IDLE = "IDLE";

    private static int numAODV = 0;

    private static int numACK = 0;

    private static int numRTS = 0;

    private static int numCTS = 0;

    private static int numUDP = 0;

    private static int numOthers = 0;

    ACATimer idleenergytimer_ = null;

    private String status;

    /** The energy model installed */
    EnergyModel em;

    /** Transmitting power  */
    double Pt;

    /** The last time the node sends somthing.  */
    double last_send_time;

    /** When the channel be idle again. */
    double channel_become_idle_time;

    /** The last time we update energy. */
    double last_energy_update_time;

    /** Frequency. */
    double freq;

    /** Wavelength  (m)     */
    double Lambda;

    /**  receive power threshold (W)   */
    double RXThresh;

    /**  carrier sense threshold (W)   */
    double CSThresh;

    /**  capture threshold (db)        */
    double CPThresh;

    public static final String CONFIG_PORT_ID = ".config";

    public static final String CHANNEL_PORT_ID = ".channel";

    public static final String PROPAGATION_PORT_ID = ".propagation";

    public static final String MOBILITY_PORT_ID = ".mobility";

    public static final String ENERGY_PORT_ID = ".energy";

    public static final String ANTENNA_PORT_ID = ".antenna";

    protected Port configPort = addPort(CONFIG_PORT_ID, false);

    /** the port receiving packets from the channel */
    protected Port channelPort = addPort(CHANNEL_PORT_ID, false);

    /** the port to query the path loss */
    protected Port propagationPort = addPort(PROPAGATION_PORT_ID, false);

    /** the port to query the current position of myself  */
    protected Port mobilityPort = addPort(MOBILITY_PORT_ID, false);

    protected Port energyPort = addPort(ENERGY_PORT_ID, false);

    /** antenna port  */
    protected Port antennaPort = addPort(ANTENNA_PORT_ID, false);

    Antenna antenna = new Antenna();

    ACATimer lockTimer;

    double Gt = 1.0;

    double Gr = 1.0;

    /** bandwidth   */
    double bandwidth;

    /** use this cache to speed up the simulation by avoiding unnecessary propagation loss calculation  */
    private Hashtable pathLossCache;

    private double Xc = 0.0;

    private double Yc = 0.0;

    private double Zc = 0.0;

    private double tc = -1.0;

    private static double tp = -1.0;

    private double xyz_tol = 10.0;

    private double long_lat_tol = 0.00009;

    /** A sample card  */
    static class SampleCard {

        static double freq = 900000000;

        static double bandwidth = 2000000.0;

        static double Pt = 0.2818;

        static double Pt_consume = 0.660;

        static double Pr_consume = 0.395;

        static double P_idle = 0.0;

        static double P_sleep = 0.130;

        static double P_off = 0.043;

        static double RXThresh = 0.2818 * (1 / 100.0) * (1 / 100.0) * (1 / 100.0) * (1 / 100.0);

        static double CSThresh = 0.2818 * (1 / 100.0) * (1 / 100.0) * (1 / 100.0) * (1 / 100.0) / 8.0;

        static double CPThresh = 10;

        SampleCard() {
        }

        ;
    }

    /** tank to soldier card for NMS demo  */
    static class Demo_TSCard {

        static double freq = 2400000000.0;

        static double bandwidth = 6000000.0;

        static double Pt = Math.pow(10.0, (21.94 + 0.15) / 10.0) * 0.001;

        static double Pt_consume = 0.660;

        static double Pr_consume = 0.395;

        static double P_idle = 0.0;

        static double P_sleep = 0.130;

        static double P_off = 0.043;

        static double RXThresh = Math.pow(10.0, -68.0 / 10.0) * 0.001;

        ;

        static double CSThresh = Math.pow(10.0, -78.0 / 10.0) * 0.001;

        ;

        static double CPThresh = 10;

        Demo_TSCard() {
        }

        ;
    }

    /** tank to tank card for NMS demo */
    static class Demo_TTCard {

        static double freq = 2400000000.0;

        static double bandwidth = 2000000.0;

        static double Pt = Math.pow(10.0, (40.0 + 0.15) / 10.0) * 0.001;

        static double Pt_consume = 0.660;

        static double Pr_consume = 0.395;

        static double P_idle = 0.0;

        static double P_sleep = 0.130;

        static double P_off = 0.043;

        static double RXThresh = Math.pow(10.0, -68.0 / 10.0) * 0.001;

        ;

        static double CSThresh = Math.pow(10.0, -78.0 / 10.0) * 0.001;

        ;

        static double CPThresh = 10;

        Demo_TTCard() {
        }

        ;
    }

    /** tank to uav card for NMS demo  */
    static class Demo_TUCard {

        static double freq = 2400000000.0;

        static double bandwidth = 3000000.0;

        static double Pt = Math.pow(10.0, 42.0 / 10.0) * 0.001;

        static double Pt_consume = 0.660;

        static double Pr_consume = 0.395;

        static double P_idle = 0.0;

        static double P_sleep = 0.130;

        static double P_off = 0.043;

        static double RXThresh = Math.pow(10.0, -68.0 / 10.0) * 0.001;

        ;

        static double CSThresh = Math.pow(10.0, -78.0 / 10.0) * 0.001;

        ;

        static double CPThresh = 10;

        Demo_TUCard() {
        }

        ;
    }

    public void _start() {
        idleenergytimer_ = setTimeout("IdleEnergyUpdateTimeout", 10.0);
    }

    /**
     * Constructor. Sets some parameters according to a simple card. 
     */
    public WirelessPhy() {
        super();
        pathLossCache = new Hashtable();
        freq = SampleCard.freq;
        Pt = SampleCard.Pt;
        bandwidth = SampleCard.bandwidth;
        Lambda = 300000000.0 / freq;
        RXThresh = SampleCard.RXThresh;
        CSThresh = SampleCard.CSThresh;
        CPThresh = SampleCard.CPThresh;
        last_send_time = 0.0;
        channel_become_idle_time = 0.0;
        last_energy_update_time = 0.0;
        em = new EnergyModel();
    }

    /**
     * Configures the card parameters.
     */
    public void configureCard(String card_) {
        if (card_.equals("Demo_TSCard")) {
            freq = Demo_TSCard.freq;
            Pt = Demo_TSCard.Pt;
            bandwidth = Demo_TSCard.bandwidth;
            RXThresh = Demo_TSCard.RXThresh;
            CSThresh = Demo_TSCard.CSThresh;
            CPThresh = Demo_TSCard.CPThresh;
            Lambda = 300000000.0 / freq;
        } else if (card_.equals("Demo_TTCard")) {
            freq = Demo_TTCard.freq;
            Pt = Demo_TTCard.Pt;
            bandwidth = Demo_TTCard.bandwidth;
            RXThresh = Demo_TTCard.RXThresh;
            CSThresh = Demo_TTCard.CSThresh;
            CPThresh = Demo_TTCard.CPThresh;
            Lambda = 300000000.0 / freq;
        } else if (card_.equals("Demo_TUCard")) {
            freq = Demo_TUCard.freq;
            Pt = Demo_TUCard.Pt;
            bandwidth = Demo_TUCard.bandwidth;
            RXThresh = Demo_TUCard.RXThresh;
            CSThresh = Demo_TUCard.CSThresh;
            CPThresh = Demo_TUCard.CPThresh;
            Lambda = 300000000.0 / freq;
        }
    }

    public EnergyModel getEnergyModel() {
        return em;
    }

    public String getName() {
        return "WirelessPhy";
    }

    public void duplicate(Object source_) {
        super.duplicate(source_);
        WirelessPhy that_ = (WirelessPhy) source_;
        Pt = that_.Pt;
        RXThresh = that_.RXThresh;
        CSThresh = that_.CSThresh;
        CPThresh = that_.CPThresh;
        freq = that_.freq;
        Lambda = that_.Lambda;
        bandwidth = that_.bandwidth;
    }

    public Port getChannelPort() {
        return channelPort;
    }

    /**
     * Sets the node id. 
     */
    public void setNid(long nid_) {
        nid = nid_;
    }

    /**
     * Gets the node id.
     */
    public long getNid(long nid_) {
        return nid;
    }

    /** Sets the transmission power */
    public void setPt(double Pt_) {
        Pt = Pt_;
    }

    /** Sets the power level.  */
    public void setPwl(int pwl_) {
        Pt = Pt / pwl_;
    }

    public void setRxThresh(double RXThresh_) {
        RXThresh = RXThresh_;
    }

    public void setCSThresh(double CSThresh_) {
        CSThresh = CSThresh_;
    }

    public void setCPThresh(double CPThresh_) {
        CPThresh = CPThresh_;
    }

    /** Sets the frequency */
    public void setFreq(double freq_) {
        freq = freq_;
        Lambda = 300000000.0 / freq;
    }

    public void setBandwidth(double bw_) {
        bandwidth = bw_;
    }

    /**
     * Processes data frame coming from MAC component.
     */
    protected synchronized void dataArriveAtUpPort(Object data_, drcl.comp.Port upPort_) {
        if (!em.getOn() || em.getSleep()) return;
        if (em.energy > 0) {
            double txtime = ((Packet) data_).size * 8.0 / bandwidth;
            double start_time = Math.max(channel_become_idle_time, getTime());
            double end_time = Math.max(channel_become_idle_time, getTime() + txtime);
            double actual_txtime = end_time - start_time;
            if (start_time > last_energy_update_time) {
                em.updateIdleEnergy(start_time - last_energy_update_time);
                last_energy_update_time = start_time;
            }
            double temp = Math.max(getTime(), last_send_time);
            double begin_adjust_time = Math.min(channel_become_idle_time, temp);
            double finish_adjust_time = Math.min(channel_become_idle_time, getTime() + txtime);
            double gap_adjust_time = finish_adjust_time - begin_adjust_time;
            if (gap_adjust_time < 0.0) {
                drcl.Debug.error("Negative gap time. Check WirelessPhy.java! finish=" + finish_adjust_time + ", begin=" + begin_adjust_time + "\n");
            }
            if ((gap_adjust_time > 0.0) && (status == STATUS_RECEIVE)) {
                em.updateTxEnergy(-gap_adjust_time);
                em.updateRxEnergy(gap_adjust_time);
            }
            em.updateTxEnergy(actual_txtime);
            if (end_time > channel_become_idle_time) {
                status = STATUS_SEND;
            }
            last_send_time = getTime() + txtime;
            channel_become_idle_time = end_time;
            last_energy_update_time = end_time;
            if (!em.getOn()) {
            }
        } else {
            return;
        }
        double t;
        t = this.getTime();
        if (Math.abs(t - tc) > 1.0) {
            PositionReportContract.Message msg = new PositionReportContract.Message();
            msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
            Xc = msg.getX();
            Yc = msg.getY();
            Zc = msg.getZ();
            tc = t;
        }
        downPort.doSending(new NodeChannelContract.Message(nid, Xc, Yc, Zc, Pt, Gt, data_));
        if (isDebugEnabled()) {
            if (t - tp > 1.0) {
                printPktStat();
                tp = t;
            }
            String pktType = ((Packet) data_).getPacketType();
            if (pktType.equals("AODV")) numAODV++; else if (pktType.equals("MAC-802.11_ACK_Frame")) numACK++; else if (pktType.equals("MAC-802.11_RTS_Frame")) numRTS++; else if (pktType.equals("MAC-802.11_CTS_Frame")) numCTS++; else if (pktType.equals("UDP")) numUDP++; else {
                numOthers++;
                System.out.println("type <" + pktType + ">");
            }
        }
    }

    void printPktStat() {
        StringBuffer sb_ = new StringBuffer(toString());
        sb_.append("AODV packet: " + numAODV);
        sb_.append("\tRTS packet: " + numRTS);
        sb_.append("\tCTS packet: " + numCTS);
        sb_.append("\tACK packet: " + numACK);
        sb_.append("\tUDP packet: " + numUDP);
        sb_.append("\tOther packet: " + numOthers);
        debug(sb_.toString());
    }

    void logEnergy() {
        ;
    }

    /**
     * Processes the received frame.
     */
    protected synchronized void dataArriveAtChannelPort(Object data_) {
        double Pr;
        double Loss;
        double Pt_received;
        double Gt_received;
        double Xs, Ys, Zs;
        boolean incorrect = false;
        Packet pkt;
        double t;
        t = this.getTime();
        if (Math.abs(t - tc) > 1.0) {
            PositionReportContract.Message msg = new PositionReportContract.Message();
            msg = (PositionReportContract.Message) mobilityPort.sendReceive(msg);
            Xc = msg.getX();
            Yc = msg.getY();
            Zc = msg.getZ();
            tc = t;
        }
        NodeChannelContract.Message msg2 = (NodeChannelContract.Message) data_;
        Xs = msg2.getX();
        Ys = msg2.getY();
        Zs = msg2.getZ();
        Gt_received = msg2.getGt();
        Pt_received = msg2.getPt();
        String type = antenna.QueryType();
        Antenna.Orientation incomingOrient = new Antenna.Orientation(0, 0);
        if (!type.equals("OMNIDIRECTIONAL ANTENNA")) {
            incomingOrient = CalcOrient(Xc, Yc, Zc, Xs, Ys, Zs);
            Gr = Math.exp(0.1 * antenna.getGain_dBi(incomingOrient));
        }
        Long sid = new Long(msg2.getNid());
        boolean cacheHit = false;
        Loss = 1.0;
        if (pathLossCache.containsKey(sid)) {
            CachedPathLoss c = (CachedPathLoss) (pathLossCache.get(sid));
            if (RadioPropagationModel.isCartesianCoordinates()) {
                if (Math.abs(c.xs - Xs) <= xyz_tol && Math.abs(c.ys - Ys) <= xyz_tol && Math.abs(c.zs - Zs) <= xyz_tol && Math.abs(c.xr - Xc) <= xyz_tol && Math.abs(c.yr - Yc) <= xyz_tol && Math.abs(c.zr - Zc) <= xyz_tol) {
                    cacheHit = true;
                    Loss = c.loss;
                }
            } else {
                if (Math.abs(c.xs - Xs) <= long_lat_tol && Math.abs(c.ys - Ys) <= long_lat_tol && Math.abs(c.zs - Zs) <= xyz_tol && Math.abs(c.xr - Xc) <= long_lat_tol && Math.abs(c.yr - Yc) <= long_lat_tol && Math.abs(c.zr - Zc) <= xyz_tol) {
                    cacheHit = true;
                    Loss = c.loss;
                }
            }
        }
        if (cacheHit == false) {
            RadioPropagationQueryContract.Message msg3 = (RadioPropagationQueryContract.Message) propagationPort.sendReceive(new RadioPropagationQueryContract.Message(Lambda, Xs, Ys, Zs, Xc, Yc, Zc));
            Loss = msg3.getLoss();
            CachedPathLoss c = new CachedPathLoss(Xc, Yc, Zc, Xs, Ys, Zs, Loss);
            pathLossCache.put(sid, c);
        }
        Pr = Pt_received * Gt_received * Gr * Loss;
        if (!em.getOn()) {
            return;
        }
        if (em.getSleep()) {
            return;
        }
        pkt = (Packet) msg2.getPkt();
        if (Pr < CSThresh / 1000) {
            return;
        }
        if (Pr < RXThresh) {
            incorrect = true;
        }
        double rcvtime = (8. * ((Packet) pkt).size) / bandwidth;
        double start_time = Math.max(channel_become_idle_time, getTime());
        double end_time = Math.max(channel_become_idle_time, getTime() + rcvtime);
        double actual_rcvtime = end_time - start_time;
        if (start_time > last_energy_update_time) {
            em.updateIdleEnergy(start_time - last_energy_update_time);
            last_energy_update_time = start_time;
        }
        em.updateRxEnergy(actual_rcvtime);
        if (end_time > channel_become_idle_time) {
            status = STATUS_RECEIVE;
        }
        channel_become_idle_time = end_time;
        last_energy_update_time = end_time;
        if (em.getOn()) {
        }
        if (!type.equals("OMNIDIRECTIONAL ANTENNA") && Pr >= RXThresh && !antenna.isLocked()) {
            antenna.lockAtSignal(incomingOrient);
            Gr = Math.exp(0.1 * antenna.getGain_dBi(incomingOrient));
            Pr = Pt_received * Gt_received * Gr * Loss;
            lockTimer = setTimeout("AntennaLockSignal_TimeOut", end_time - getTime());
        }
        MacPhyContract.Message msg4 = new MacPhyContract.Message(incorrect, Pr, CPThresh, CSThresh, RXThresh, pkt);
        upPort.doSending(msg4);
    }

    /**
     * Calculates the orientation of the sender (Xt, Yt, Zt) in regards to 
     * the receiver's position (Xr, Yr, Zr) 
     *
     */
    protected Antenna.Orientation CalcOrient(double Xr, double Yr, double Zr, double Xt, double Yt, double Zt) {
        double delta_x, delta_y, delta_z, delta_xy;
        double alfa = 0, beta = 0;
        Antenna.Orientation orient;
        delta_x = Xt - Xr;
        delta_y = Yt - Yr;
        delta_z = Zt - Zr;
        delta_xy = Math.sqrt(delta_x * delta_x + delta_y * delta_y);
        if (delta_x == 0) {
            if (delta_y == 0) alfa = 0; else if (delta_y > 0) alfa = 90; else alfa = 270;
        } else {
            alfa = Math.toDegrees(Math.abs(Math.atan(delta_y / delta_x)));
            if (delta_x > 0 && delta_y >= 0) ; else if (delta_x < 0 && delta_y >= 0) alfa = 180 - alfa; else if (delta_x < 0 && delta_y < 0) alfa = 180 + alfa; else if (delta_x > 0 && delta_y < 0) alfa = 360 - alfa;
        }
        if (delta_xy == 0) {
            if (delta_z == 0) beta = 0; else if (delta_z > 0) beta = 90; else beta = 270;
        } else {
            beta = Math.toDegrees(Math.abs(Math.atan(delta_z / delta_xy)));
            if (delta_xy > 0 && delta_z >= 0) ; else if (delta_xy < 0 && delta_z >= 0) beta = 180 - beta; else if (delta_xy < 0 && delta_z >= 0) beta = 180 + beta; else if (delta_xy > 0 && delta_z < 0) beta = 360 - beta;
        }
        return new Antenna.Orientation((int) alfa, (int) beta);
    }

    /**
     * Configures the node's antenna from assigned port.
     */
    protected void configAntenna(Object data_) {
        String args = ((String) data_).toLowerCase(), value;
        if (args.startsWith("create")) {
            String ant = args.substring(args.indexOf("create") + 6);
            ant = ant.trim();
            if (ant.equals("antenna")) {
                return;
            }
            if (ant.equals("switchedbeam antenna")) {
                antenna = new SwitchedBeamAntenna();
                return;
            }
            if (ant.equals("adaptive antenna")) {
                antenna = new AdaptiveAntenna();
                return;
            }
            System.out.println("FORMAT erorr! shall be <create antenna/switchedbeam antenna/adaptive antenna>");
            return;
        }
        if (args.startsWith("querytype")) {
            System.out.println(antenna.QueryType());
            return;
        }
        int index;
        if ((index = args.indexOf('=')) != -1) {
            value = (args.substring(index + 1)).trim();
            if (value.equals(null)) {
                System.out.println(this + ":: pls. use the format such as <height = 1.5>");
                return;
            }
            if (args.indexOf("height") != -1) {
                float height = Float.parseFloat(value);
                antenna.setHeight(height);
                System.out.println("set height = " + antenna.setHeight(height));
                return;
            }
            if (args.indexOf("omnigain_dbi") != -1) {
                float omniGain_dBi = Float.parseFloat(value);
                antenna.setOmniGain_dBi(omniGain_dBi);
                System.out.println("set omniGain_dBi = " + antenna.getGain_dBi());
                return;
            }
            if (args.indexOf("azimuthpatterns") != -1) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(value));
                    in.close();
                } catch (java.io.IOException e) {
                    System.out.println(this + ":: error in opening " + value);
                    return;
                }
                if (antenna.initAzimuthPatterns(value)) ; else System.out.println(this + "Failure in initializing the azimuth pattern file!");
                return;
            }
            if (args.indexOf("elevationpatterns") != -1) {
                try {
                    BufferedReader in = new BufferedReader(new FileReader(value));
                    in.close();
                } catch (java.io.IOException e) {
                    System.out.println(this + ":: error in opening " + value);
                    return;
                }
                if (antenna.initElevationPatterns(value)) System.out.println(this + "Successfully initialize the elevation pattern file!"); else System.out.println(this + "Failure in initializing the evlation pattern file!");
                return;
            }
            System.out.println(" Wrong format: no such initialization item!");
        }
        System.out.println("Wrong format to communicate with the Antenna component!");
    }

    protected synchronized void processOther(Object data_, Port inPort_) {
        String portid_ = inPort_.getID();
        if (portid_.equals(CHANNEL_PORT_ID)) {
            if (!(data_ instanceof NodeChannelContract.Message)) {
                error(data_, "processOther()", inPort_, "unknown object");
                return;
            }
            dataArriveAtChannelPort(data_);
            return;
        }
        if (portid_.equals(ENERGY_PORT_ID)) {
            if (data_ == null) energyPort.doSending(em); else if (data_ instanceof BooleanObj) {
                em.setSleep(((BooleanObj) data_).getValue());
                updateIdleEnergy();
            } else drcl.Debug.error("WirelessPhy.processOther()", "Unrecognized contract", true);
            return;
        }
        if (portid_.equals(ANTENNA_PORT_ID)) {
            configAntenna(data_);
            return;
        }
        super.processOther(data_, inPort_);
    }

    /**
     *  Preriodically timeout to update energy consumption even if it is in idle state.
     */
    public synchronized void timeout(Object data_) {
        if (data_ instanceof String) {
            if (((String) data_).equals("IdleEnergyUpdateTimeout")) {
                if (em.getEnergy() > 0) logEnergy();
                updateIdleEnergy();
                idleenergytimer_ = setTimeout("IdleEnergyUpdateTimeout", 10.0);
            } else if (((String) data_).equals("AntennaLockSignal_TimeOut")) {
                antenna.unlock();
            }
        }
    }

    /**
     * updates energy consumption during the idle state.
     */
    protected void updateIdleEnergy() {
        if (getTime() > last_energy_update_time) {
            if (em.getOn()) {
                if (em.getSleep()) em.updateSleepEnergy(getTime() - last_energy_update_time); else em.updateIdleEnergy(getTime() - last_energy_update_time);
            }
            last_energy_update_time = getTime();
        }
    }
}
