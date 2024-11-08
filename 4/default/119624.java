import java.io.*;
import java.net.*;
import iwork.eheap2.*;

public class tilt implements BlueSentryListener {

    BlueSentry bs;

    EventHeap eheap;

    private int lastSliderRange;

    private String ID;

    private int count = 0;

    private int last_side = -1;

    public tilt(String eheapName, String comPort) {
        ID = comPort;
        eheap = new EventHeap(eheapName);
        bs = new BlueSentry(comPort);
        bs.register(this);
    }

    private static final int X_center = 32400;

    private static final int Y_center = 31930;

    private static final int X_max = 36189 - X_center;

    private static final int Y_max = 36100 - Y_center;

    private static final int X_min = 28769 - X_center;

    private static final int Y_min = 28351 - Y_center;

    public void callback() {
        try {
            double chan0 = (bs.getChannel(0) - X_center) / 4000.0;
            double chan1 = (bs.getChannel(1) - Y_center) / 4000.0;
            System.out.println("Chan 0 = " + chan0 + " : Chan 1 = " + chan1 + " : ");
            Event newEvent = new Event("iStuffEvent");
            newEvent.addField("Name", "Tilt");
            newEvent.addField("X.Axis.Min", new Double(-1));
            newEvent.addField("X.Axis.Max", new Double(1));
            newEvent.addField("X.Axis.Value", new Double(chan0));
            newEvent.addField("X.Axis.IsChanged", new Integer(1));
            newEvent.addField("Y.Axis.Min", new Double(-1));
            newEvent.addField("Y.Axis.Max", new Double(1));
            newEvent.addField("Y.Axis.Value", new Double(chan1));
            newEvent.addField("Y.Axis.IsChanged", new Integer(1));
            newEvent.addField("EHC_Timestamp", new Long(System.currentTimeMillis()));
            eheap.putEvent(newEvent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendEvent(int side) {
        try {
            if (side != -1 && side != last_side) {
                Event newEvent = new Event("CubeEvent");
                newEvent.addField("Side", new Integer(side));
                eheap.putEvent(newEvent);
                last_side = side;
                System.out.println("Sending side " + side);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public OutputStream getOutputStream() {
        return bs.getOutputStream();
    }

    public static void main(String[] args) {
        tilt t;
        if (args.length == 0) t = new tilt("iw-room2", "COM3"); else if (args.length == 1) t = new tilt(args[0], "COM3"); else t = new tilt(args[0], args[1]);
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                t.getOutputStream().write(in.readLine().getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
