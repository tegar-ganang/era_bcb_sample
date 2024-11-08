package client;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.scene.Geometry;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 *
 * @author Ben
 */
public class FileHandler implements Savable {

    static LundWidget lund = new LundWidget();

    public static void save(String file) {
        TarArchiveOutputStream out = null;
        try {
            FileWriter sf = new FileWriter(file + ".lundata");
            BufferedWriter o = new BufferedWriter(sf);
            out = new TarArchiveOutputStream(new BufferedOutputStream(new FileOutputStream(file + ".lundmap")));
            File sfv = new File(file + ".lundata");
            if (sfv.exists()) {
                lund.printConsoleWarning("Previous mapdata cache found; Overwriting.");
                boolean stard = sfv.delete();
                if (stard) {
                    lund.printConsoleWarning("Deleted previous mapdata cache succesfully.");
                } else {
                    lund.printConsoleWarning("Problem deleting previous mapdata cache.");
                }
            }
            for (int i = 0; i < Main.rendernode.getChildren().size(); i++) {
                lund.printConsoleInfo("Saving: " + Main.rendernode.getChild(i).toString());
                o.write(Main.rendernode.getChild(i).toString());
                if (Main.rendernode.getChild(i) instanceof Geometry) {
                    o.write(Main.rendernode.getChild(i).getLocalTransform().toString());
                    o.write(" ");
                } else {
                    o.write(" ");
                    o.write("nd");
                }
                o.newLine();
            }
            lund.printConsoleInfo("Saved!");
            o.flush();
            sf.flush();
            out.flush();
            o.close();
            o = null;
            sf.close();
            sf = null;
            addFileToTarGz(out, file + ".lundata", "");
            out.close();
            out = null;
            System.gc();
            if (sfv.exists()) {
                boolean tryd = sfv.delete();
                if (tryd) {
                    lund.printConsoleInfo("Data packed safely!");
                } else {
                    lund.printConsoleWarning("Temp file left behind!");
                }
            }
        } catch (Throwable e) {
            lund.printConsoleError(e.getMessage());
        }
    }

    private static void addFileToTarGz(TarArchiveOutputStream taro, String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        FileInputStream goIn = new FileInputStream(f);
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        taro.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        taro.putArchiveEntry(tarEntry);
        if (f.isFile()) {
            IOUtils.copy(goIn, taro);
            taro.closeArchiveEntry();
        } else {
            taro.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToTarGz(taro, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
        taro.close();
        goIn.close();
    }

    public static void load(String file) {
        try {
            FileInputStream lf = new FileInputStream(file + ".txt");
            DataInputStream lfd = new DataInputStream(lf);
            BufferedReader lfb = new BufferedReader(new InputStreamReader(lfd));
            String parse;
            while ((parse = lfb.readLine()) != null) {
                lund.printConsoleInfo(parse.substring(parse.lastIndexOf("(") + 1, parse.lastIndexOf(")")));
            }
            lfd.close();
        } catch (Throwable e) {
            lund.printConsoleError(e.getMessage());
        }
    }

    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
