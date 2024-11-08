import java.io.*;
import java.net.*;
import iwork.eheap2.*;

public class cube implements BlueSentryListener {

    BlueSentry bs;

    EventHeap eheap;

    private String eheapName;

    private int lastSliderRange;

    private String ID;

    private int count = 0;

    private static int SLEEP_COUNT = 100;

    private static int DEBOUNCE_COUNT = 2;

    private int last_side = 0;

    public int X_tare[] = { 32096, 36095, 31938, 28711, 33010, 32790 };

    public int Y_tare[] = { 31965, 31587, 28443, 32918, 36163, 32655 };

    public int chan0;

    public int chan1;

    private static String SIDE_DATA = "sides.txt";

    public cube(String eheapName, String comPort) {
        ID = comPort;
        this.eheapName = eheapName;
        eheap = new EventHeap(eheapName);
        bs = new BlueSentry(comPort);
        readCalibrationData();
        bs.register(this);
        calibrate();
    }

    private int decode(float X, float Y) {
        double min_dist = 0x0150 * 0x0150;
        for (int i = 0; i < X_tare.length; i++) {
            double dist = (X - X_tare[i]) * (X - X_tare[i]) + (Y - Y_tare[i]) * (Y - Y_tare[i]);
            if (dist < min_dist) return i;
        }
        return -1;
    }

    public void callback() {
        try {
            chan0 = bs.getChannel(0);
            chan1 = bs.getChannel(1);
            System.out.println("Chan 0 = " + chan0 + " : Chan 1 = " + chan1 + " : ");
            int side = decode(chan0, chan1);
            if (side < 0) System.out.println("not a side"); else sendEvent(side);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendEvent(int side) {
        try {
            if (side != -1) {
                if (side == last_side) {
                    count++;
                    if (count == DEBOUNCE_COUNT) {
                        if (eheap.isConnected()) {
                            Event newEvent = new Event("CubeEvent");
                            newEvent.addField("Side", new Integer(side));
                            eheap.putEvent(newEvent);
                            System.out.println("Sending side " + side);
                        } else {
                            System.out.println("WARNING: event heap is not connected to " + eheapName);
                        }
                    }
                    if (count == SLEEP_COUNT) {
                        bs.sleep();
                    }
                } else {
                    last_side = side;
                    count = 0;
                }
            } else {
                count = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public OutputStream getOutputStream() {
        return bs.getOutputStream();
    }

    public void calibrate() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                int chr = in.read();
                switch(chr) {
                    case '0':
                        X_tare[0] = chan0;
                        Y_tare[0] = chan1;
                        System.out.println("Tare for side 0: x=" + X_tare[0] + " y=" + Y_tare[0]);
                        writeCalibrationData();
                        break;
                    case '1':
                        X_tare[1] = chan0;
                        Y_tare[1] = chan1;
                        System.out.println("Tare for side 1: x=" + X_tare[1] + " y=" + Y_tare[1]);
                        writeCalibrationData();
                        break;
                    case '2':
                        X_tare[2] = chan0;
                        Y_tare[2] = chan1;
                        System.out.println("Tare for side 2: x=" + X_tare[2] + " y=" + Y_tare[2]);
                        writeCalibrationData();
                        break;
                    case '3':
                        X_tare[3] = chan0;
                        Y_tare[3] = chan1;
                        System.out.println("Tare for side 3: x=" + X_tare[3] + " y=" + Y_tare[3]);
                        writeCalibrationData();
                        break;
                    case '4':
                        X_tare[4] = chan0;
                        Y_tare[4] = chan1;
                        System.out.println("Tare for side 4: x=" + X_tare[4] + " y=" + Y_tare[4]);
                        writeCalibrationData();
                        break;
                    case '5':
                        X_tare[5] = chan0;
                        Y_tare[5] = chan1;
                        System.out.println("Tare for side 5: x=" + X_tare[5] + " y=" + Y_tare[5]);
                        writeCalibrationData();
                        break;
                    default:
                        getOutputStream().write(chr);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void readCalibrationData() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(SIDE_DATA));
            for (int i = 0; i < 6; i++) {
                X_tare[i] = new Integer(br.readLine()).intValue();
                Y_tare[i] = new Integer(br.readLine()).intValue();
            }
        } catch (FileNotFoundException ex) {
            File f = new File(SIDE_DATA);
        } catch (Exception ex) {
            System.out.println("Invalid Side Data format... delete " + SIDE_DATA + " and try again.");
            ex.printStackTrace();
        }
    }

    public void writeCalibrationData() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(SIDE_DATA));
            for (int i = 0; i < X_tare.length; i++) {
                bw.write("" + X_tare[i]);
                bw.newLine();
                bw.write("" + Y_tare[i]);
                bw.newLine();
            }
            bw.close();
        } catch (FileNotFoundException ex) {
            File f = new File(SIDE_DATA);
        } catch (Exception ex) {
            System.out.println("Invalid Side Data format... delete " + SIDE_DATA + " and try again.");
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        cube c;
        if (args.length != 2) {
            System.out.println("Usage: cube <event heap server> <comm port>");
            System.exit(-1);
        }
        c = new cube(args[0], args[1]);
    }
}
