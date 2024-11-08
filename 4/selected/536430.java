package org.jdmp.sigmen.client.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.jdmp.sigmen.client.Fenetre;
import org.jdmp.sigmen.client.Main;

public class AddPackage extends Thread {

    private File file;

    private String location;

    public AddPackage(File file, String location) {
        super();
        this.file = file;
        this.location = location;
    }

    @Override
    public void run() {
        try {
            File dest = new File(location);
            if ((dest.getParent() != null && !dest.getParentFile().isDirectory() && !dest.getParentFile().mkdirs())) {
                throw new IOException("Impossible de créer un dossier (" + dest.getParent() + ").");
            } else if (dest.exists() && !dest.delete()) {
                throw new IOException("Impossible de supprimer un ancien fichier (" + dest + ").");
            } else if (!dest.createNewFile()) {
                throw new IOException("Impossible de créer un fichier (" + dest + ").");
            }
            FileChannel in = new FileInputStream(file).getChannel();
            FileChannel out = new FileOutputStream(dest).getChannel();
            try {
                in.transferTo(0, in.size(), out);
            } finally {
                in.close();
                out.close();
            }
        } catch (Exception e) {
            Main.fenetre().erreur(Fenetre.ERREUR_FATALE_UPDATE, e);
        } finally {
            file.delete();
        }
    }
}
