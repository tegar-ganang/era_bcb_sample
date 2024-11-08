package org.sunspotworld.demo;

import java.io.IOException;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import java.util.*;
import com.sun.spot.io.j2me.radiogram.*;
import javax.microedition.io.*;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import com.sun.spot.sensorboard.peripheral.LEDColor;

public class SunSpotAudio extends MIDlet {

    private final int MIN_TEMPO = 230;

    private final int MAX_TEMPO = 3000;

    private AudioThread m_audioThread;

    private LeaderThread m_leaderThread;

    ITriColorLED[] m_leds = EDemoBoard.getInstance().getLEDs();

    private boolean m_useLog = true;

    private boolean m_useLightShow = true;

    public static final short message_newMote = 0;

    public static final short message_localAV = 1;

    public static final short message_newLeader = 2;

    public static final short message_globalValues = 3;

    public static final short message_orders = 4;

    public static final short role_none = 0;

    public static final short role_melody = 1;

    public static final short role_bass = 2;

    public static final short role_harmony = 3;

    public static final short role_percussion = 4;

    public static final short role_leader = role_melody;

    public static short m_role = role_none;

    public static long m_synchOffset = 0;

    private void initAndRun() throws Exception {
        m_audioThread = new AudioThread(50, 1000, 90);
        m_audioThread.IsLogEnabled(m_useLog);
        m_audioThread.IsLightShowEnabled(m_useLightShow);
        Timer audioTimer = new Timer();
        SensingThread m_sensingThread = new SensingThread();
        Timer sensingTimer = new Timer();
        m_leaderThread = new LeaderThread(m_audioThread, m_sensingThread);
        Timer electionTimer = new Timer();
        electionTimer.schedule(m_leaderThread, 3000);
        RadiogramConnection rCon;
        Datagram dg;
        try {
            rCon = (RadiogramConnection) Connector.open("radiogram://:" + 37);
            dg = rCon.newDatagram(rCon.getMaximumLength());
        } catch (Exception e) {
            System.err.println("setUp caught " + e.getMessage());
            throw e;
        }
        advertiseNewMote();
        while (true) {
            try {
                rCon.receive(dg);
                short messageType = dg.readShort();
                if (messageType == message_newLeader) {
                    signalReceived();
                    electionTimer.cancel();
                    int randomBackoff = Math.abs((new Random()).nextInt()) % 4;
                    for (int i = 0; i < randomBackoff * 50000; i++) {
                    }
                    advertiseNewMote();
                } else if (messageType == message_orders) {
                    signalReceived();
                    long localTime = System.currentTimeMillis();
                    electionTimer.cancel();
                    m_role = dg.readShort();
                    long playTime = dg.readLong();
                    m_synchOffset = localTime - dg.readLong() - 50;
                    audioTimer.schedule(m_audioThread, new Date(playTime + m_synchOffset));
                    sensingTimer.schedule(m_sensingThread, new Date(playTime + m_synchOffset + 1000));
                } else if (messageType == message_globalValues) {
                    signalReceived();
                    PlanningThread.globalAV.setArousal(dg.readFloat());
                    PlanningThread.globalAV.setValence(dg.readFloat());
                } else if (messageType == message_newMote && m_role == role_leader) {
                    signalReceived();
                    m_leaderThread.synchNewMote(dg.getAddress());
                } else if (messageType == message_localAV && m_role == role_leader) {
                    signalReceived();
                    m_leaderThread.addLocalAV(dg.readFloat(), dg.readFloat());
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }

    public void signalReceived() {
        if (m_useLightShow) {
            m_leds[0].setColor(LEDColor.YELLOW);
            m_leds[0].setOn();
            try {
                wait(500);
            } catch (Exception e) {
            }
            m_leds[0].setOff();
        }
    }

    public void signalBroadcast() {
        if (m_useLightShow) {
            m_leds[0].setColor(LEDColor.GREEN);
            m_leds[0].setOn();
            try {
                wait(500);
            } catch (Exception e) {
            }
            m_leds[0].setOff();
        }
    }

    /**
     * Signal the mote's new arrival to the network.
     */
    public void advertiseNewMote() {
        m_role = role_none;
        DatagramConnection dgConnection = null;
        Datagram dg = null;
        try {
            dgConnection = (DatagramConnection) Connector.open("radiogram://broadcast:37");
            dg = dgConnection.newDatagram(dgConnection.getMaximumLength());
        } catch (IOException e) {
            System.out.println("Could not open datagram broadcast connection");
            e.printStackTrace();
            return;
        }
        try {
            dg.reset();
            dg.writeShort(SunSpotAudio.message_newMote);
            dgConnection.send(dg);
            dgConnection.close();
            System.out.println("New Mote broadcast is going through");
            signalBroadcast();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Transmit local AV values to the leader mote.
     */
    public static void broadcastLocalAV() {
        DatagramConnection dgConnection = null;
        Datagram dg = null;
        try {
            dgConnection = (DatagramConnection) Connector.open("radiogram://broadcast:37");
            dg = dgConnection.newDatagram(dgConnection.getMaximumLength());
        } catch (IOException e) {
            System.out.println("Could not open datagram broadcast connection");
            e.printStackTrace();
            return;
        }
        try {
            dg.reset();
            dg.writeShort(SunSpotAudio.message_localAV);
            dg.writeFloat(PlanningThread.localAV.getArousal());
            dg.writeFloat(PlanningThread.localAV.getValence());
            dgConnection.send(dg);
            dgConnection.close();
            System.out.println("Local AV broadcast is going through");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void update(AudioThread t) throws IOException {
    }

    protected void startApp() throws MIDletStateChangeException {
        try {
            initAndRun();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void pauseApp() {
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}
