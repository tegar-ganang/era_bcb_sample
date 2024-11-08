package org.sunspotworld.demo;

import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.sensorboard.peripheral.ITriColorLED;
import java.io.IOException;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import com.sun.spot.sensorboard.peripheral.LEDColor;
import com.sun.spot.util.IEEEAddress;
import java.util.*;

public class LeaderThread extends TimerTask {

    private EDemoBoard m_demo;

    private ITriColorLED[] m_leds;

    AudioThread m_audioThread;

    SensingThread m_sensingThread;

    private long m_playTime;

    private int m_lastRoleAssigned;

    private int m_motesRemaining;

    public LeaderThread() {
        m_demo = EDemoBoard.getInstance();
        m_leds = m_demo.getLEDs();
        m_lastRoleAssigned = SunSpotAudio.role_leader;
        m_motesRemaining = 0;
    }

    public LeaderThread(AudioThread audioThread, SensingThread sensingThread) {
        this();
        setAudioThread(audioThread);
        m_sensingThread = sensingThread;
    }

    public void setAudioThread(AudioThread audioThread) {
        m_audioThread = audioThread;
    }

    public void run() {
        SunSpotAudio.m_role = SunSpotAudio.role_leader;
        m_playTime = System.currentTimeMillis() + 3000;
        (new Timer()).schedule(m_audioThread, new Date(m_playTime));
        (new Timer()).schedule(m_sensingThread, new Date(m_playTime + 1000));
        advertiseLeadership();
    }

    /**
     * Receive local AV from some mote (through SunSpotAudio thread) and send
     * the global values out if all local values have been collected.
     * 
     * @param arousal
     * @param valence
     */
    public void addLocalAV(float arousal, float valence) {
        AV newLocal = new AV(arousal, valence);
        AVManager.add(newLocal);
        m_motesRemaining--;
        if (m_motesRemaining == 0) {
            AVManager.add(PlanningThread.localAV);
            PlanningThread.globalAV.setAV(AVManager.getAverageArousal(), AVManager.getAverageValence());
            broadcastGlobalValues();
            m_motesRemaining = m_lastRoleAssigned;
            AVManager.clear();
        }
    }

    /**
     * Broadcast a message declaring this mote as the new leader - doesn't
     * need any data besides the message type.
     */
    private void advertiseLeadership() {
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
            dg.writeShort(SunSpotAudio.message_newLeader);
            dgConnection.send(dg);
            dgConnection.close();
            System.out.println("New Leader broadcast is going through");
            signalBroadcast();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sending synching infomation to a new mote, including time synchronization,
     * when to start playing, and its newly assigned role. Immediately afterwards
     * also send it the global AV values so it's ready to play.
     * 
     * @param moteAddress
     */
    void synchNewMote(String moteAddress) {
        DatagramConnection dgConnection = null;
        Datagram dg = null;
        try {
            dgConnection = (DatagramConnection) Connector.open("radiogram://" + IEEEAddress.toDottedHex((new IEEEAddress(moteAddress)).asLong()) + ":37");
            dg = dgConnection.newDatagram(dgConnection.getMaximumLength());
        } catch (IOException e) {
            System.out.println("Could not open datagram broadcast connection");
            e.printStackTrace();
            return;
        }
        try {
            dg.reset();
            dg.writeShort(SunSpotAudio.message_orders);
            m_lastRoleAssigned++;
            dg.writeShort(m_lastRoleAssigned);
            dg.writeLong(m_playTime);
            dg.writeLong(System.currentTimeMillis());
            dgConnection.send(dg);
            dgConnection.close();
            System.out.println("Synch Mote message is going through");
            signalTargeted();
        } catch (IOException e) {
            e.printStackTrace();
        }
        broadcastGlobalValues();
    }

    /**
     * Broadcast global valence and arousal, as well as tempo.
     */
    void broadcastGlobalValues() {
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
            dg.writeShort(SunSpotAudio.message_globalValues);
            dg.writeFloat(PlanningThread.globalAV.getArousal());
            dg.writeFloat(PlanningThread.globalAV.getValence());
            dgConnection.send(dg);
            dgConnection.close();
            System.out.println("Local AV broadcast is going through");
            signalBroadcast();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void signalBroadcast() {
        m_leds[0].setColor(LEDColor.CYAN);
        m_leds[0].setOn();
        try {
            wait(500);
        } catch (Exception e) {
        }
        m_leds[0].setOff();
    }

    public void signalTargeted() {
        m_leds[0].setColor(LEDColor.WHITE);
        m_leds[0].setOn();
        try {
            wait(500);
        } catch (Exception e) {
        }
        m_leds[0].setOff();
    }

    /**
     * Sends out a Synch command at the time at which it sends the broadcast.
     * The broadcast is signalled with a cyan LED lighting.
     * 
     */
    void broadcastSynchTime() {
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
            dg.writeLong(new Date().getTime());
            dgConnection.send(dg);
            dgConnection.close();
            System.out.println("SynchTime broadcast is going through");
            m_leds[0].setColor(LEDColor.CYAN);
            m_leds[0].setOn();
            try {
                wait(500);
            } catch (Exception e) {
            }
            m_leds[0].setOff();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends out a Play command and the absolute time at which to start playing.
     * The broadcast is signalled with a blue LED lighting.
     * 
     * @param playTime
     */
    void broadcastPlayTime(long playTime) {
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
            dg.writeLong(playTime);
            dgConnection.send(dg);
            dgConnection.close();
            System.out.println("PlayTime broadcast is going through");
            m_leds[0].setColor(LEDColor.BLUE);
            m_leds[0].setOn();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
