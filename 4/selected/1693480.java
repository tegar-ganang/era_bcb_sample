package titancommon.bluetooth;

import java.util.Date;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Generates a synchronized sampling of multiple datachannels
 * 
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class Synchronizer extends TimerTask implements Observer {

    private HashMap m_channels;

    private int m_samplePeriod;

    private int m_delayPeriod;

    private Timer m_timer;

    private class SyncObs extends Observable {

        public void reportValues(BTDataPacket dp) {
            setChanged();
            notifyObservers(dp);
        }
    }

    private SyncObs m_obs = new SyncObs();

    public Synchronizer() {
        m_channels = new HashMap();
    }

    public Synchronizer(int delayPeriod, int samplePeriod) {
        m_channels = new HashMap();
        m_samplePeriod = samplePeriod;
        m_delayPeriod = delayPeriod;
    }

    public void addObserver(Observer obs) {
        m_obs.addObserver(obs);
    }

    public void deleteObserver(Observer obs) {
        m_obs.deleteObserver(obs);
    }

    /**
    * Expects incoming packets coming e.g. from a StreamDecoder.
    * 
    * @param obs the observable object
    * @param arg the 
    */
    public void update(Observable obs, Object arg) {
        if (arg instanceof BTDataPacket) {
            BTDataPacket packet = (BTDataPacket) arg;
            PacketQueue pq = (PacketQueue) m_channels.get(packet.getSensorname());
            if (pq == null) {
                System.out.println("Synchronizer: adding new channel: " + packet.getSensorname());
                pq = new PacketQueue();
                m_channels.put(packet.getSensorname(), pq);
                if (m_channels.size() == 1) {
                    m_timer = new Timer();
                    m_timer.scheduleAtFixedRate(this, m_delayPeriod, m_samplePeriod);
                }
            }
            pq.offer(packet);
        } else {
            System.out.println("Synchronizer: got unknown update");
        }
    }

    /**
    * This function is called by the timer - do here the resampling.
    */
    public void run() {
        Date sampleTime = new Date((new Date()).getTime() - m_delayPeriod);
        Vector values = new Vector();
        String sensorname = "Synchronizer";
        String[] channels = new String[0];
        Object[] queues = m_channels.values().toArray();
        for (int i = 0; i < queues.length; i++) {
            PacketAligner.realign(((PacketQueue) queues[i]).getAllPackets());
            Object[] objpkts = ((PacketQueue) queues[i]).peekN(2);
            BTDataPacket[] pkts = new BTDataPacket[2];
            pkts[0] = (BTDataPacket) objpkts[0];
            pkts[1] = (BTDataPacket) objpkts[1];
            if (pkts[0] == null) {
                System.out.println("ERROR: Synchronizer: Empty queue found!");
                continue;
            }
            String[] newChannels = new String[channels.length + pkts[0].getChannels().length];
            for (int j = channels.length; j < newChannels.length; j++) {
                newChannels[j] = pkts[0].getSensorname() + "_" + pkts[0].getChannels()[j - channels.length];
            }
            channels = newChannels;
            while (true) {
                if (pkts[1] == null || sampleTime.getTime() <= pkts[0].getTimestamp()) {
                    values.addAll(pkts[0].getValues());
                    break;
                } else if (pkts[0].getTimestamp() < sampleTime.getTime() && sampleTime.getTime() < pkts[1].getTimestamp()) {
                    values.addAll(subsample(sampleTime.getTime(), pkts[0], pkts[1]));
                    break;
                }
                ((PacketQueue) queues[i]).poll();
                objpkts = ((PacketQueue) queues[i]).peekN(2);
                pkts = new BTDataPacket[2];
                pkts[0] = (BTDataPacket) objpkts[0];
                pkts[1] = (BTDataPacket) objpkts[1];
            }
        }
        BTDataPacket outPacket = new BTDataPacket(sampleTime, values, sensorname, channels);
        m_obs.reportValues(outPacket);
    }

    /**
    * Subsamples two consecutive values and weighs them according to the distance 
    * to the point in time requested.
    * @param timepoint
    * @param pktOlder
    * @param pktNewer
    * @return
    */
    public Vector subsample(long timepoint, BTDataPacket pktOlder, BTDataPacket pktNewer) {
        long diffold = timepoint - pktOlder.getTimestamp();
        long diffnew = pktNewer.getTimestamp() - timepoint;
        double oldweight = (double) diffnew / (double) (diffold + diffnew);
        double newweight = (double) diffold / (double) (diffold + diffnew);
        Vector olddata = pktOlder.getValues();
        Vector newdata = pktNewer.getValues();
        Vector subsampled = new Vector(olddata.size());
        for (int i = 0; i < olddata.size(); i++) {
            Object testvalue = olddata.get(i);
            if (testvalue instanceof Integer) {
                Integer oldvalue = (Integer) olddata.get(i);
                Integer newvalue = (Integer) newdata.get(i);
                subsampled.add(i, new Integer((int) ((double) oldvalue.intValue() * oldweight + (double) newvalue.intValue() * newweight)));
            } else if (testvalue instanceof Short) {
                Short oldvalue = (Short) olddata.get(i);
                Short newvalue = (Short) newdata.get(i);
                subsampled.add(i, new Short((short) ((double) oldvalue.shortValue() * oldweight + (double) newvalue.shortValue() * newweight)));
            } else if (testvalue instanceof Character) {
                Character oldvalue = (Character) olddata.get(i);
                Character newvalue = (Character) newdata.get(i);
                subsampled.add(i, new Character((char) ((double) oldvalue.charValue() * oldweight + (double) newvalue.charValue() * newweight)));
            }
        }
        return subsampled;
    }

    /**
    * test code
    * @param args
    */
    public static void main(String[] args) {
        System.out.println("Testing Synchronizer class");
        int delayPeriod = 400;
        int samplePeriod = 300;
        Synchronizer syn = new Synchronizer(delayPeriod, samplePeriod);
        syn.addObserver(new Observer() {

            public void update(Observable o, Object arg) {
                System.out.println(((BTDataPacket) arg));
            }
        });
        Vector a1 = new Vector();
        Vector a2 = new Vector();
        Vector a3 = new Vector();
        Vector b1 = new Vector();
        Vector b2 = new Vector();
        Vector b3 = new Vector();
        a1.add(new Integer(1));
        a2.add(new Integer(2));
        a3.add(new Integer(3));
        a1.add(new Short((short) 1));
        a2.add(new Short((short) 2));
        a3.add(new Short((short) 3));
        a1.add(new Character((char) 1));
        a2.add(new Character((char) 2));
        a3.add(new Character((char) 3));
        b1.add(new Integer(-30));
        b2.add(new Integer(100));
        b3.add(new Integer(25));
        Date now = new Date();
        String[] channels = { "1", "2", "3" };
        BTDataPacket dpA1 = new BTDataPacket(new Date(now.getTime() + 200), a1, "a", channels);
        BTDataPacket dpA2 = new BTDataPacket(new Date(now.getTime() + 500), a2, "a", channels);
        BTDataPacket dpA3 = new BTDataPacket(new Date(now.getTime() + 1100), a3, "a", channels);
        BTDataPacket dpB1 = new BTDataPacket(new Date(now.getTime() + 600), b1, "b", channels);
        BTDataPacket dpB2 = new BTDataPacket(new Date(now.getTime() + 900), b2, "b", channels);
        BTDataPacket dpB3 = new BTDataPacket(new Date(now.getTime() + 1400), b3, "b", channels);
        System.out.println("Sending packets");
        syn.update(null, dpA1);
        syn.update(null, dpA2);
        syn.update(null, dpA3);
        syn.update(null, dpB1);
        syn.update(null, dpB2);
        syn.update(null, dpB3);
        System.out.println("Packet A1 at: " + dpA1.getTimestamp());
        System.out.println("Packet A2 at: " + dpA2.getTimestamp());
        System.out.println("Packet A3 at: " + dpA3.getTimestamp());
        System.out.println("Packet B1 at: " + dpB1.getTimestamp());
        System.out.println("Packet B2 at: " + dpB2.getTimestamp());
        System.out.println("Packet B3 at: " + dpB3.getTimestamp());
    }
}
