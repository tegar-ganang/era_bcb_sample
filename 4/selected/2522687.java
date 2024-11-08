package org.jdmp.sigmen.client.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.jdmp.sigmen.client.Fenetre;
import org.jdmp.sigmen.client.Main;
import org.jdmp.sigmen.client.Waiter;
import org.jdmp.sigmen.messages.Constantes.Envoi;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class XMLVersion {

    private boolean restart;

    private boolean offline;

    private Map<Integer, PackageVersion> addPackages;

    private Map<Integer, PackageVersion> removePackages;

    private Element root;

    private File xml;

    private boolean updatable = true;

    public XMLVersion() {
        File xml = new File("versions.xml");
        if (xml.exists()) {
            try {
                root = new SAXBuilder().build(xml).getRootElement();
            } catch (Exception e) {
                Main.out("Fichier XML de versions manquant ou corrompu. Téléchargera tous les fichiers.");
            }
        } else {
            Main.out("Fichier XML de versions manquant ou corrompu. Téléchargera tous les fichiers.");
        }
    }

    public String getVersion() {
        try {
            return root.getAttributeValue("version");
        } catch (Exception e) {
            return "inconnu";
        }
    }

    public void setUpdateSource(byte[] donnees) {
        updatable = true;
        try {
            xml = File.createTempFile("jdmpmmo-versions", ".xml");
            FileOutputStream os = new FileOutputStream(xml);
            os.write(donnees);
            os.flush();
            os.close();
        } catch (IOException e) {
            Main.fenetre().erreur(Fenetre.ERREUR_UPDATE, e);
        }
    }

    public void setManualUpdate() {
        updatable = false;
    }

    public void update() {
        if (!updatable) {
            Main.fenetre().erreur(Fenetre.OLD_VERSION);
            return;
        }
        try {
            Main.fenetre().update();
            Element remoteRoot = new SAXBuilder().build(xml).getRootElement();
            addPackages = new HashMap<Integer, PackageVersion>();
            Iterator<?> iterElem = remoteRoot.getChildren().iterator();
            while (iterElem.hasNext()) {
                PackageVersion pack = new PackageVersion((Element) iterElem.next());
                addPackages.put(pack.id(), pack);
            }
            removePackages = new HashMap<Integer, PackageVersion>();
            iterElem = root.getChildren("package").iterator();
            while (iterElem.hasNext()) {
                PackageVersion pack = new PackageVersion((Element) iterElem.next());
                int id = pack.id();
                if (!addPackages.containsKey(id)) {
                    removePackages.put(id, pack);
                } else if (addPackages.get(id).version().equals(pack.version())) {
                    addPackages.remove(id);
                } else {
                    addPackages.get(id).ecrase();
                }
            }
            Iterator<PackageVersion> iterPack = addPackages.values().iterator();
            while (iterPack.hasNext()) {
                install(iterPack.next());
            }
            iterPack = removePackages.values().iterator();
            while (iterPack.hasNext()) {
                remove(iterPack.next());
            }
            if (offline) {
                Runtime.getRuntime().addShutdownHook(new AddPackage(xml, "versions.xml"));
                Main.fenetre().erreur(Fenetre.UPDATE_TERMINE_RESTART);
            } else {
                File oldXML = new File("versions.xml");
                oldXML.delete();
                oldXML.createNewFile();
                FileChannel out = new FileOutputStream(oldXML).getChannel();
                FileChannel in = new FileInputStream(xml).getChannel();
                in.transferTo(0, in.size(), out);
                in.close();
                out.close();
                xml.delete();
                if (restart) {
                    Main.fenetre().erreur(Fenetre.UPDATE_TERMINE_RESTART);
                } else {
                    Main.updateVersion();
                }
            }
        } catch (Exception e) {
            Main.fenetre().erreur(Fenetre.ERREUR_UPDATE, e);
        }
    }

    public void remove(PackageVersion pack) {
        Main.out("Fichier " + pack.location() + " marqué pour être supprimé.");
        if (pack.offlineRemove()) {
            restart = true;
            new File(pack.location()).deleteOnExit();
        } else {
            new File(pack.location()).delete();
        }
    }

    public void install(int id, byte[] data) {
        try {
            PackageVersion pack = addPackages.get(id);
            if (pack.offline()) {
                offline = true;
                restart = true;
                File file = File.createTempFile("jdmpmmp-" + id, ".tmp");
                FileOutputStream os = new FileOutputStream(file);
                os.write(data);
                os.flush();
                os.close();
                Runtime.getRuntime().addShutdownHook(new AddPackage(file, pack.location()));
            } else {
                if (pack.restart()) {
                    restart = true;
                }
                File file = new File(pack.location());
                if ((file.getParent() != null && !file.getParentFile().isDirectory() && !file.getParentFile().mkdirs())) {
                    throw new IOException("Impossible de créer un dossier (" + file.getParent() + ").");
                } else if (file.exists() && !file.delete()) {
                    throw new IOException("Impossible de supprimer un ancien fichier (" + file + ").");
                } else if (!file.createNewFile()) {
                    throw new IOException("Impossible de créer un fichier (" + file + ").");
                }
                file.createNewFile();
                FileOutputStream os = new FileOutputStream(file);
                os.write(data);
                os.flush();
                os.close();
            }
        } catch (IOException e) {
            Main.fenetre().erreur(Fenetre.ERREUR_UPDATE, e);
        }
    }

    public void install(PackageVersion pack) {
        Main.out("Fichier " + pack.location() + " marqué pour être installé.");
        Waiter.addLock(Waiter.ASK_PACKAGE);
        Main.sender().send(Envoi.ASK_PACKAGE, pack.id());
        Waiter.lockAlways(Waiter.ASK_PACKAGE);
    }
}
