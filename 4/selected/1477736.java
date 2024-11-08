package uchicago.src.sim.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Created by IntelliJ IDEA.
 * User: thowe
 * Date: Jan 2, 2003
 * Time: 4:02:08 PM
 * To change this template use Options | File Templates.
 */
public class DataCallback {

    protected SocketChannel channel;

    protected BufferedWriter out;

    protected static int channelNum = 0;

    public DataCallback(SocketChannel channel, String fqModelName) {
        channelNum++;
        try {
            String homeDir = System.getProperty("user.home");
            String modelDir = homeDir + File.separator + ".repast" + File.separator + fqModelName.replace('.', File.separatorChar);
            File fModelDir = new File(modelDir);
            if (!fModelDir.exists()) {
                fModelDir.mkdirs();
            }
            String fileName = modelDir + File.separator + ".tmp." + channelNum + ".txt";
            out = new BufferedWriter(new FileWriter(fileName, true));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.channel = channel;
    }

    public void execute() throws IOException {
        out.close();
    }

    public SocketChannel getChannel() {
        return this.channel;
    }

    public void record(String values) {
        if (values.indexOf("\007") >= 0) {
            values = values.substring(0, values.indexOf("\007"));
        }
        try {
            out.write(values);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
