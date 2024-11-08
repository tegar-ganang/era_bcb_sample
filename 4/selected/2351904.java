package edu.sdsc.rtdsm.stubs;

import java.io.IOException;
import java.util.*;
import java.text.DateFormat;
import edu.sdsc.rtdsm.dig.sites.*;
import edu.sdsc.rtdsm.framework.src.*;
import edu.sdsc.rtdsm.framework.feedback.*;

public class SiteSrcStub extends SrcFeedbackListener {

    int cacheSize = 0;

    String sourceName = null;

    String tmpStr;

    boolean connected = false;

    SiteSource source;

    Vector<Integer> chnlIndex;

    Vector<String> chnlNames;

    String configFile;

    SrcConfig srcConfig;

    SrcFeedbackListener fbListener;

    public SiteSrcStub(String configFile, String sourceName) {
        this(configFile, sourceName, null);
    }

    public SiteSrcStub(String configFile, String sourceName, SrcFeedbackListener feedbackListener) {
        this.configFile = configFile;
        this.sourceName = sourceName;
        this.fbListener = feedbackListener;
        SrcConfigParser parser = new SrcConfigParser();
        parser.fileName = configFile;
        parser.parse();
        srcConfig = parser.getSourceConfig(sourceName);
    }

    public SiteSrcStub(SrcConfig srcConfig, String sourceName) {
        this(srcConfig, sourceName, null);
    }

    public SiteSrcStub(SrcConfig srcConfig, String sourceName, SrcFeedbackListener feedbackListener) {
        this.srcConfig = srcConfig;
        this.sourceName = sourceName;
        this.fbListener = feedbackListener;
    }

    public void connect() {
        source = new SiteSource(srcConfig, fbListener);
        connected = source.connect();
        chnlIndex = source.getChannelIndicies();
        chnlNames = source.getChannelNames();
    }

    public void startPumping(long sleepTime) {
        int count = 0;
        try {
            while (connected) {
                for (int channel = 0; channel < chnlIndex.size(); channel++) {
                    double data[] = new double[2];
                    data[0] = (double) (count * 100 + (double) channel + 1) / 100.00;
                    data[1] = (double) (count * 100 + 10 + (double) channel + 1) / 100.00;
                    source.insertData(((Integer) chnlIndex.elementAt(channel)).intValue(), (Object) data);
                    String time = DateFormat.getDateTimeInstance().format(new Date());
                    for (int i = 0; i < data.length; i++) {
                        System.out.println("Sent|" + source.getName() + "/" + chnlNames.elementAt(channel) + "|" + time + System.currentTimeMillis() + "|" + i + "|" + data[i]);
                    }
                }
                String time = DateFormat.getDateTimeInstance().format(new Date());
                time = DateFormat.getDateTimeInstance().format(new Date());
                int numChannelsFlushed = source.flush();
                System.out.println("Flushed|" + source.getName() + "|" + time + "|" + System.currentTimeMillis());
                System.out.println("Stub:Flushed:" + time + ":" + numChannelsFlushed);
                System.out.println("-----------------------------------");
                Thread.sleep(sleepTime);
                count++;
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void startPumpingRandom(long sleepTime) {
        int count = 0;
        int channel = (int) (Math.random() * 1000) % chnlIndex.size();
        int flushAfter = (int) (Math.random() * 1000) % 5 + 1;
        try {
            System.out.println("Next Flush after:" + flushAfter);
            while (connected) {
                double data[] = new double[2];
                data[0] = (double) (count * 100 + (double) channel + 1) / 100.00;
                data[1] = (double) (count * 100 + 10 + (double) channel + 1) / 100.00;
                source.insertData(((Integer) chnlIndex.elementAt(channel)).intValue(), (Object) data);
                String time = DateFormat.getDateTimeInstance().format(new Date());
                for (int i = 0; i < data.length; i++) {
                    System.out.println("Sent|" + source.getName() + "/" + chnlNames.elementAt(channel) + "|" + time + "|" + i + "|" + data[i]);
                }
                flushAfter--;
                if (flushAfter == 0) {
                    flushAfter = (int) (Math.random() * 1000) % 5 + 1;
                    time = DateFormat.getDateTimeInstance().format(new Date());
                    int numChannelsFlushed = source.flush();
                    System.out.println("Stub:Flushed:" + time + ":" + numChannelsFlushed + " Next Flush after " + flushAfter);
                    System.out.println("Flushed|" + source.getName() + "|" + time);
                    System.out.print("-----------------------------------");
                    System.out.println("-----------------------------------");
                }
                Thread.sleep(sleepTime);
                count++;
                channel = (int) (Math.random() * 1000) % chnlIndex.size();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String configFile = null;
        String sourceName = null;
        boolean feedbackReqd = false;
        switch(args.length) {
            case 3:
                if ("feedback".equals(args[2])) {
                    feedbackReqd = true;
                }
            case 2:
                configFile = args[0];
                sourceName = args[1];
                break;
            default:
                System.err.println("Usage: java stubs.SiteSrcStub " + "<srcConfig xml file> <source name> [feedback]");
                return;
        }
        SiteSrcStub src = null;
        if (feedbackReqd) {
            FeedbackListenerStub fbListen = new FeedbackListenerStub();
            src = new SiteSrcStub(configFile, sourceName, fbListen);
        } else {
            src = new SiteSrcStub(configFile, sourceName, null);
        }
        src.connect();
        src.startPumpingRandom(10000);
    }

    public void receiveFeedback(String feedbackMsg, Date time) {
        String timeStr = DateFormat.getDateTimeInstance().format(time);
        System.out.println("Received some feedback at the end " + "source. Time: " + timeStr + " Msg: " + feedbackMsg);
    }
}
