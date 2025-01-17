package net.saim.game.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PrefixCCMap extends HashMap<String, String> {

    private static final String LOCAL_FILE = "prefixes.csv";

    private static PrefixCCMap instance;

    private PrefixCCMap() {
        fillMap();
    }

    private void fillMap() {
        try {
            BufferedReader bufRdr = new BufferedReader(new FileReader(new File(this.getClass().getClassLoader().getResource(LOCAL_FILE).getPath())));
            String line = null;
            String key = null;
            String value = null;
            while ((line = bufRdr.readLine()) != null) {
                String[] entry = line.split(",");
                if (entry.length == 2) {
                    key = entry[0].trim();
                    value = entry[1].trim();
                    put(key, value);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PrefixCCMap getInstance() {
        if (instance == null) {
            instance = new PrefixCCMap();
        }
        return instance;
    }

    /**
	 * This main methods updates the local prefix file by loading latest prefix list from prefix.cc.
	 * @param args
	 * @throws IOException 
	 */
    public static void main(String[] args) throws IOException {
        String target = "d://prefix.csv";
        URL google = new URL("http://prefix.cc/popular/all.file.csv");
        ReadableByteChannel rbc = Channels.newChannel(google.openStream());
        File file = new File(target);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.getChannel().transferFrom(rbc, 0, 1 << 24);
        fos.close();
        File tmpFile = new File(target + ".tmp");
        File inFile = new File(target);
        PrintWriter pw = new PrintWriter(new FileWriter(tmpFile));
        BufferedReader br = new BufferedReader(new FileReader(inFile));
        String line = null;
        Set<String> values = new HashSet<String>();
        while ((line = br.readLine()) != null) {
            String[] entry = line.split(",");
            if (entry.length == 2) {
                String key = entry[0];
                String value = entry[1];
                value = value.substring(1);
                value = value.substring(0, value.length() - 1);
                if (!value.trim().isEmpty() && !values.contains(value)) {
                    values.add(value);
                    pw.println(entry[0] + "," + value);
                    pw.flush();
                }
            }
        }
        pw.close();
        br.close();
        inFile.delete();
        tmpFile.renameTo(inFile);
    }
}
