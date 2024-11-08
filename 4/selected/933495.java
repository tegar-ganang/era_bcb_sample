package de.pitkley.minejar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.jar.JarFile;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author pit
 */
public class Patcher {

    private ArrayList<Mod> mods = new ArrayList<Mod>();

    Patcher() {
    }

    public void addMod(Mod m) {
        mods.add(m);
    }

    public void clearMods() {
        mods.clear();
    }

    public void patch() throws IOException {
        if (mods.isEmpty()) {
            return;
        }
        IOUtils.copy(new FileInputStream(Paths.getMinecraftJarPath()), new FileOutputStream(new File(Paths.getMinecraftBackupPath())));
        JarFile mcjar = new JarFile(Paths.getMinecraftJarPath());
    }
}
