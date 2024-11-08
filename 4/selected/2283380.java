package uk.midearth.dvb.confParser;

import uk.midearth.dvb.*;
import java.io.*;
import java.util.*;

public class ConfParser {

    private File configFile;

    private Vector channelList = new Vector();

    private int currentChannel = 0;

    public ConfParser(String newConfigFilename) {
        configFile = new File(newConfigFilename);
        if (configFile.isFile() && configFile.canRead()) {
            try {
                BufferedReader in = new BufferedReader(new FileReader(configFile));
                String str;
                String[] configParts;
                while ((str = in.readLine()) != null) {
                    if (str.length() > 0) {
                        if (str.trim().charAt(0) != '#') {
                            configParts = str.split(":");
                            channelList.addElement(new Channel(configParts[0], Integer.parseInt(configParts[configParts.length - 2]), Integer.parseInt(configParts[configParts.length - 1])));
                        }
                    }
                }
                in.close();
            } catch (Exception ioe) {
                System.out.println("ConfParser>> IOException:: " + ioe);
                ioe.printStackTrace();
            }
        } else {
            System.out.println("ConfParser>> File " + configFile.getAbsolutePath() + " does not exist or can not be read.");
            System.exit(1);
        }
    }

    private void listChannels() {
        Channel tempChannel;
        for (int i = 0; i < channelList.size(); i++) {
            tempChannel = (Channel) channelList.elementAt(i);
            System.out.println("ConfParser>> Name: " + tempChannel.channelName() + "\tVideo: " + tempChannel.videoPID() + "\tAudio: " + tempChannel.audioPID());
        }
    }

    public Channel getChannel(int channelNo) {
        return new Channel((Channel) channelList.elementAt(channelNo));
    }

    public String[] channelList() {
        String[] result = new String[channelList.size()];
        for (int i = 0; i < channelList.size(); i++) {
            result[i] = new String(((Channel) channelList.elementAt(i)).channelName());
        }
        return result;
    }

    public String configFilename() {
        return configFile.getAbsolutePath();
    }
}
