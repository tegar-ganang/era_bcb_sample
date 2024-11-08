package gov.sns.apps.diagnostics.blmview;

import gov.sns.apps.diagnostics.blmview.mvc.*;
import java.util.*;
import java.io.*;
import gov.sns.ca.*;

public class BLMsModel extends AbstractModel {

    private Vector blms = new Vector();

    private BLMdevice selectedDevice;

    private static int ONEHZRF = 6;

    private static final long TIMESWAPWAIT = 3000;

    private String timingPVName;

    private Axes fPlAx;

    private Axes icPlAx;

    private Axes ndPlAx;

    public void setPlotAxes(String name, String xm, String xM, String st) {
        double step = 0, xMin = 0, xMax = 0;
        try {
            xMin = Double.parseDouble(xm);
            xMax = Double.parseDouble(xM);
        } catch (Exception e) {
            return;
        }
        try {
            step = Double.parseDouble(st);
        } catch (Exception e) {
            step = xMax - xMin;
        }
        if (name.equals("FastPlot")) {
            fPlAx = new Axes(xMin, xMax, step);
        } else if (name.equals("ICIntegralPlot")) {
            icPlAx = new Axes(xMin, xMax, step);
        } else if (name.equals("NDIntegralPlot")) {
            ndPlAx = new Axes(xMin, xMax, step);
        }
    }

    public Axes getPlotAxes(String name) {
        if (name.equals("FastPlot")) {
            return fPlAx;
        } else if (name.equals("ICIntegralPlot")) {
            return icPlAx;
        } else if (name.equals("NDIntegralPlot")) {
            return ndPlAx;
        }
        return null;
    }

    public String getTimingPVname() {
        return timingPVName;
    }

    public void setTimingPVName(String n) {
        timingPVName = n;
    }

    public void performTimingSwap() {
        System.out.println("Timing swap");
        try {
            Channel ch;
            Iterator<Channel> it = slowTrig.iterator();
            int[] curSlow = new int[slowTrig.size()];
            int i = 0;
            while (it.hasNext()) {
                ch = it.next();
                curSlow[i] = ch.getValInt();
                System.out.println("Slow " + ch.channelName() + " " + curSlow[i]);
                if (ch.writeAccess()) ch.putVal(ONEHZRF); else System.out.println("TimeSwap() " + ch.channelName() + " has no permission to write.");
                i++;
            }
            it = fastTrig.iterator();
            int[] curFast = new int[fastTrig.size()];
            i = 0;
            while (it.hasNext()) {
                ch = it.next();
                curFast[i] = ch.getValInt();
                System.out.println("Fast " + ch.channelName() + " " + curFast[i]);
                if (ch.writeAccess()) ch.putVal(ONEHZRF); else System.out.println("TimeSwap() " + ch.channelName() + " has no permission to write.");
                i++;
            }
            System.out.println("Waiting a little while");
            Thread.currentThread().sleep(TIMESWAPWAIT);
            System.out.println("Restoring");
            it = slowTrig.iterator();
            i = 0;
            while (it.hasNext()) {
                ch = it.next();
                if (ch.writeAccess()) ch.putVal(curSlow[i]); else System.out.println("TimeSwap() " + ch.channelName() + " has no permission to write.");
                System.out.println("Slow " + ch.channelName() + " " + ch.getValInt());
                i++;
            }
            it = fastTrig.iterator();
            i = 0;
            while (it.hasNext()) {
                ch = it.next();
                if (ch.writeAccess()) ch.putVal(curFast[i]); else System.out.println("TimeSwap() " + ch.channelName() + " has no permission to write.");
                System.out.println("Fast " + ch.channelName() + " " + ch.getValInt());
                i++;
            }
        } catch (ConnectionException e) {
            System.out.println("Got connection exception " + e.getMessage());
        } catch (GetException e) {
            System.out.println("Got get exception " + e.getMessage());
        } catch (PutException e) {
            System.out.println("Got put exception " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Timing swap was interrupted, shit!");
        }
    }

    public void write(BufferedWriter out) throws IOException {
        Iterator<Channel> it = slowTrig.iterator();
        while (it.hasNext()) {
            out.write("   <Trigger name=\"" + it.next().channelName() + "\" type=\"slow\"/>\n");
        }
        it = fastTrig.iterator();
        while (it.hasNext()) {
            out.write("   <Trigger name=\"" + it.next().channelName() + "\" type=\"fast\"/>\n");
        }
        if (getTimingPVname() != null) out.write("  <TimingPV name=\"" + getTimingPVname() + "\"/>\n");
        for (int i = 0; i < blms.size(); i++) {
            ((BLMdevice) blms.get(i)).write(out);
        }
    }

    public void calculateAll(int firstVisibleRow, int lastVisibleRow) {
        if (firstVisibleRow < 0) return;
        int i = 0;
        long start = System.currentTimeMillis();
        try {
            Thread.currentThread().sleep(200);
        } catch (InterruptedException e) {
            System.out.println("I was interrupted, shit!");
        }
        for (i = firstVisibleRow; i < lastVisibleRow + 1; i++) {
            ((BLMdevice) blms.get(i)).calculateAll();
        }
        if (selectedDevice != null) selectedDevice.calculateAll();
        long time = (System.currentTimeMillis() - start);
        if (time > 600) System.out.println("Slowing down finish calc " + (lastVisibleRow - firstVisibleRow + 1) + " blms for " + time);
    }

    public void cleanUp() {
        Iterator it = blms.iterator();
        while (it.hasNext()) {
            ((BLMdevice) it.next()).cleanUP();
        }
    }

    /**
	 * Method calculateAll
	 *
	 */
    public void calculateAll() {
        long start = System.currentTimeMillis();
        Iterator it = blms.iterator();
        while (it.hasNext()) {
            ((BLMdevice) it.next()).calculateAll();
        }
        if (blms.size() != 1) System.out.println("Finish calc " + blms.size() + " blms for " + (System.currentTimeMillis() - start));
    }

    /**
	 * Method setSelectedDevice
	 *
	 * @param    selectedRow         an int
	 *
	 */
    public void setSelectedDevice(int selectedRow) {
        selectedDevice = (BLMdevice) blms.get(selectedRow);
        notifyChanged(new ModelEvent(this, 0, null, null));
    }

    public BLMdevice getSelectedDevice() {
        return selectedDevice;
    }

    public BLMdevice getBLMdevice(int rowIndex) {
        return ((BLMdevice) blms.get(rowIndex));
    }

    /**
	 * Method getNumberOfBLMs
	 *
	 */
    public int getNumberOfBLMs() {
        return blms.size();
    }

    /**
	 * Method addBLM
	 *
	 * @param    bLMdevice           a  BLMdevice
	 *
	 */
    public void addBLM(BLMdevice bLMdevice) {
        blms.add(bLMdevice);
    }

    ArrayList<Channel> slowTrig = new ArrayList<Channel>();

    ArrayList<Channel> fastTrig = new ArrayList<Channel>();

    public void addTrigger(String name, String type) {
        Channel ch = ChannelFactory.defaultFactory().getChannel(name);
        ch.connect();
        if (type.equals("slow")) {
            slowTrig.add(ch);
        } else if (type.equals("fast")) {
            fastTrig.add(ch);
        }
    }
}
