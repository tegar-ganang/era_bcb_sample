package ij.plugin;

import ij.*;
import ij.gui.*;
import ij.util.Tools;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

/** This plugin implements the Help/Update ImageJ command. */
public class ImageJ_Updater implements PlugIn {

    public void run(String arg) {
        if (arg.equals("menus")) {
            updateMenus();
            return;
        }
        if (IJ.getApplet() != null) return;
        URL url = getClass().getResource("/ij/IJ.class");
        String ij_jar = url == null ? null : url.toString().replaceAll("%20", " ");
        if (ij_jar == null || !ij_jar.startsWith("jar:file:")) {
            error("Could not determine location of ij.jar");
            return;
        }
        int exclamation = ij_jar.indexOf('!');
        ij_jar = ij_jar.substring(9, exclamation);
        if (IJ.debugMode) IJ.log("Updater: " + ij_jar);
        File file = new File(ij_jar);
        if (!file.exists()) {
            error("File not found: " + file.getPath());
            return;
        }
        if (!file.canWrite()) {
            String msg = "No write access: " + file.getPath();
            if (IJ.isVista()) msg += Prefs.vistaHint;
            error(msg);
            return;
        }
        String[] list = openUrlAsList(IJ.URL + "/download/jars/list.txt");
        int count = list.length + 2;
        String[] versions = new String[count];
        String[] urls = new String[count];
        String uv = getUpgradeVersion();
        if (uv == null) return;
        versions[0] = "v" + uv;
        urls[0] = IJ.URL + "/upgrade/ij.jar";
        if (versions[0] == null) return;
        for (int i = 1; i < count - 1; i++) {
            String version = list[i - 1];
            versions[i] = version.substring(0, version.length() - 1);
            urls[i] = IJ.URL + "/download/jars/ij" + version.substring(1, 2) + version.substring(3, 6) + ".jar";
        }
        versions[count - 1] = "daily build";
        urls[count - 1] = IJ.URL + "/ij.jar";
        int choice = showDialog(versions);
        if (choice == -1) return;
        if (!versions[choice].startsWith("daily") && versions[choice].compareTo("v1.39") < 0 && Menus.getCommands().get("ImageJ Updater") == null) {
            String msg = "This command is not available in versions of ImageJ prior\n" + "to 1.39 so you will need to install the plugin version at\n" + "<" + IJ.URL + "/plugins/imagej-updater.html>.";
            if (!IJ.showMessageWithCancel("Update ImageJ", msg)) return;
        }
        byte[] jar = getJar(urls[choice]);
        if (version().compareTo("1.37v") >= 0) Prefs.savePreferences();
        saveJar(file, jar);
        if (choice < count - 1) new File(IJ.getDirectory("macros") + "functions.html").delete();
        System.exit(0);
    }

    int showDialog(String[] versions) {
        GenericDialog gd = new GenericDialog("ImageJ Updater");
        gd.addChoice("Upgrade To:", versions, versions[0]);
        String msg = "You are currently running v" + version() + ".\n" + " \n" + "If you click \"OK\", ImageJ will quit\n" + "and you will be running the upgraded\n" + "version after you restart ImageJ.\n";
        gd.addMessage(msg);
        gd.showDialog();
        if (gd.wasCanceled()) return -1; else return gd.getNextChoiceIndex();
    }

    String getUpgradeVersion() {
        String url = IJ.URL + "/notes.html";
        String notes = openUrlAsString(url, 20);
        if (notes == null) {
            error("Unable to connect to " + IJ.URL + ". You\n" + "may need to use the Edit>Options>Proxy Settings\n" + "command to configure ImageJ to use a proxy server.");
            return null;
        }
        int index = notes.indexOf("Version ");
        if (index == -1) {
            error("Release notes are not in the expected format");
            return null;
        }
        String version = notes.substring(index + 8, index + 13);
        return version;
    }

    String openUrlAsString(String address, int maxLines) {
        StringBuffer sb;
        try {
            URL url = new URL(address);
            InputStream in = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            sb = new StringBuffer();
            int count = 0;
            String line;
            while ((line = br.readLine()) != null && count++ < maxLines) sb.append(line + "\n");
            in.close();
        } catch (IOException e) {
            sb = null;
        }
        return sb != null ? new String(sb) : null;
    }

    byte[] getJar(String address) {
        byte[] data;
        boolean gte133 = version().compareTo("1.33u") >= 0;
        try {
            URL url = new URL(address);
            IJ.showStatus("Connecting to " + IJ.URL);
            URLConnection uc = url.openConnection();
            int len = uc.getContentLength();
            String name = address.endsWith("ij/ij.jar") ? "daily build" : "ij.jar";
            IJ.showStatus("Downloading ij.jar (" + IJ.d2s((double) len / 1048576, 1) + "MB)");
            InputStream in = uc.getInputStream();
            data = new byte[len];
            int n = 0;
            while (n < len) {
                int count = in.read(data, n, len - n);
                if (count < 0) throw new EOFException();
                n += count;
                if (gte133) IJ.showProgress(n, len);
            }
            in.close();
        } catch (IOException e) {
            return null;
        }
        return data;
    }

    void saveJar(File f, byte[] data) {
        try {
            FileOutputStream out = new FileOutputStream(f);
            out.write(data, 0, data.length);
            out.close();
        } catch (IOException e) {
        }
    }

    String[] openUrlAsList(String address) {
        IJ.showStatus("Connecting to " + IJ.URL);
        Vector v = new Vector();
        try {
            URL url = new URL(address);
            InputStream in = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while (true) {
                line = br.readLine();
                if (line == null) break;
                if (!line.equals("")) v.addElement(line);
            }
            br.close();
        } catch (Exception e) {
        }
        String[] lines = new String[v.size()];
        v.copyInto((String[]) lines);
        IJ.showStatus("");
        return lines;
    }

    String version() {
        String version = "";
        try {
            Class ijClass = ImageJ.class;
            Field field = ijClass.getField("VERSION");
            version = (String) field.get(ijClass);
        } catch (Exception ex) {
        }
        return version;
    }

    boolean isMac() {
        String osname = System.getProperty("os.name");
        return osname.startsWith("Mac");
    }

    void error(String msg) {
        IJ.error("ImageJ Updater", msg);
    }

    void updateMenus() {
        if (IJ.debugMode) {
            long start = System.currentTimeMillis();
            Menus.updateImageJMenus();
            IJ.log("Refresh Menus: " + (System.currentTimeMillis() - start) + " ms");
        } else Menus.updateImageJMenus();
    }
}
