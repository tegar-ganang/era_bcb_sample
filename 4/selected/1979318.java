package PRISM.RobotCtrl.drivers;

import PRISM.RobotCtrl.*;
import PRISM.RobotCtrl.tracking.ObjectTracker;
import PRISM.VRW.VRWBall;
import PRISM.RobotCtrl.RangeDataSet;
import PRISM.RobotCtrl.tracking.*;
import PRISM.RobotCtrl.Activities.RangeFinderDriver;
import java.util.*;
import java.io.*;

/**
 * 
 * @author Mauro Dragone
 */
public class Camera extends RangeFinderDriver implements Runnable {

    int dX = 0, dY = 0;

    int sector = 0;

    int distance = 0;

    Thread m_thread = null;

    int[] objects = new int[100];

    int[] ntvcmd = new int[3];

    int[] rangeData = null;

    int[] sharedRangeData = null;

    int[] extraData = new int[100];

    int[] obst = new int[32];

    RateController m_rateController = null;

    int dataVersion = 0;

    int dataVersionRead = 0;

    int readFromLog = 0;

    int writeLog = 0;

    public native void init(int readAVI, int writeAVI);

    public native void init2(int readAVI, int writeAVI);

    public native void release();

    public native void getData(int data[], int range[], int extra[]);

    public native void pushJavaImage(int pixels[], int w, int h);

    public native void getFrame(int dimensions[], int pixels[]);

    protected static Camera ref = null;

    int m_frameDelay = 100;

    boolean runThread = true;

    public Camera() {
        super("Camera", "Camera", new RangeDataSet("Camera.Range", "Camera.Range", "cm", 41, 10, 200, true));
        System.out.println("Camera ctor...");
        rangeData = getDataSet().getData();
        m_rateController = new RateController("Camera frame rate", 5, 1);
        m_thread = new Thread(this);
        sharedRangeData = new int[41];
        ref = this;
    }

    public static Camera getCamera() {
        System.out.println("Camera is " + ref);
        return ref;
    }

    public void enableThread(boolean enable) {
        runThread = enable;
    }

    public void readFromLog(boolean enable) {
        if (enable) readFromLog = 1; else readFromLog = 0;
    }

    public void writeToLog(boolean enable) {
        if (enable) writeLog = 1; else writeLog = 0;
    }

    public void exit() {
        release();
    }

    public final void run() {
        dX = 0;
        dY = 0;
        System.out.println("Camera Thread Started...");
        while (true) {
            processOutput();
            try {
                m_thread.sleep(m_frameDelay);
            } catch (InterruptedException e) {
            }
        }
    }

    public void processOutput() {
        getData(objects, rangeData, extraData);
        synchronized (sharedRangeData) {
            for (int i = 0; i < 40; i++) sharedRangeData[i] = rangeData[i];
            dataVersion++;
        }
        m_rateController.check(false);
        getImpl().m_sensorData.setUpdateTime(System.currentTimeMillis());
        int nobjs = objects[0];
        boolean senseBall = false;
        boolean senseGoal = false;
        boolean senseRobot = false;
        for (int i = 0; i < nobjs; i++) {
            int base = i * 10;
            int typeObject = objects[base + 1];
            float confidence = 0;
            String objectName = "unknown";
            if (typeObject == 1) {
                senseBall = true;
                objectName = "ball";
                confidence = objects[base + 5];
                getObjectTracker().observation(objectName, (double) objects[base + 2] * (double) 10, (double) objects[base + 4] * (double) 10, (double) objects[base + 3] * Math.PI / (double) 180, 0, 0, confidence);
            }
            if (typeObject == 2) {
                senseGoal = true;
                objectName = "goal";
                confidence = objects[base + 5];
                getObjectTracker().observation(objectName, (double) objects[base + 2] * (double) 10, 1000, (double) objects[base + 3] * Math.PI / (double) 180, (double) objects[base + 4] * Math.PI / (double) 1800, objects[base + 6], confidence, -1);
            }
            if (typeObject == 3) {
                senseRobot = true;
                objectName = "robot";
                confidence = objects[base + 5];
                getObjectTracker().observation(objectName, (double) objects[base + 2] * (double) 10, 1000, (double) objects[base + 3] * Math.PI / (double) 180, 0, 0, confidence);
            }
        }
        if (!senseBall) getObjectTracker().lost("ball");
        if (!senseGoal) getObjectTracker().lost("goal");
        if (!senseRobot) getObjectTracker().lost("robot");
    }

    public Vector process() {
        if (dataVersion <= 0) return null;
        int deg;
        int min;
        return super.process();
    }

    public void setActive(boolean val) {
        if (val) {
            System.out.println("Activating Camera driver...");
            if (runThread) {
                init(readFromLog, writeLog);
                m_thread.start();
            } else {
                System.out.println("Initializating jni camera driver...");
                init2(readFromLog, writeLog);
                m_thread.start();
            }
        }
        super.setActive(val);
    }

    static {
        try {
            System.out.println(System.getProperty("java.library.path"));
            System.loadLibrary("RobotCamera");
        } catch (Throwable t) {
            System.out.println("Exception loading Camera library " + t);
        }
    }
}
