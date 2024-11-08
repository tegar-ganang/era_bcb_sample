package Modele;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Mathieu PASSENAUD
 * @version 1.0
 */
public class Parametres {

    public static String url = "";

    public static String nom = "";

    public static String motdepasse = "";

    public static int type = 0;

    public static String emplacement = "";

    public static int tailleBlocs = 1024;

    public static int nbThreads = 2;

    public static int tailleCache = 1000;

    /**
     * initialise tous les paramètres depuis le fichier de configuration. Si le fichier n'existe pas, il est créé.
     * @return true si le fichier est nouveau
     */
    public static boolean initialiserParametres() {
        File f = new File("param.de");
        if (!f.exists()) {
            try {
                try {
                    f.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
                }
                FileOutputStream fs = null;
                try {
                    fs = new FileOutputStream(f);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
                }
                ObjectOutputStream os = null;
                try {
                    os = new ObjectOutputStream(fs);
                } catch (IOException ex) {
                    Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
                }
                os.writeObject("");
                os.writeObject("");
                os.writeObject("");
                os.writeObject("");
                os.writeInt(1);
                os.writeInt(1);
                os.writeInt(1);
                os.writeInt(1);
                os.close();
                fs.close();
                return true;
            } catch (IOException ex) {
                Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                FileInputStream fi = null;
                try {
                    fi = new FileInputStream(f);
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
                }
                ObjectInputStream oi = null;
                try {
                    oi = new ObjectInputStream(fi);
                } catch (IOException ex) {
                    Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
                }
                url = (String) oi.readObject();
                nom = (String) oi.readObject();
                motdepasse = (String) oi.readObject();
                emplacement = (String) oi.readObject();
                type = oi.readInt();
                tailleBlocs = oi.readInt();
                nbThreads = oi.readInt();
                tailleCache = oi.readInt();
            } catch (IOException ex) {
                Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return false;
    }

    /**
     * Sauvegarde tous les paramètres sur disque
     * @param url de connection
     * @param nom
     * @param motdepasse
     * @param type de base de données
     * @param emplacement de restauration
     * @param tailleBlocs
     * @param nbThreads
     * @param tailleCache
     */
    public static void setParametres(String url, String nom, String motdepasse, int type, String emplacement, int tailleBlocs, int nbThreads, int tailleCache) {
        try {
            File f = new File("param.de");
            if (!f.exists()) {
                try {
                    f.createNewFile();
                } catch (IOException ex) {
                    Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            FileOutputStream fs = null;
            try {
                fs = new FileOutputStream(f);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
            }
            ObjectOutputStream os = null;
            try {
                os = new ObjectOutputStream(fs);
            } catch (IOException ex) {
                Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
            }
            os.writeObject(url);
            os.writeObject(nom);
            os.writeObject(motdepasse);
            os.writeObject(emplacement);
            os.writeInt(type);
            os.writeInt(tailleBlocs);
            os.writeInt(nbThreads);
            os.writeInt(tailleCache);
            os.close();
            fs.close();
            Parametres.url = url;
            Parametres.nom = nom;
            Parametres.motdepasse = motdepasse;
            Parametres.emplacement = emplacement;
            Parametres.type = type;
            Parametres.tailleBlocs = tailleBlocs;
            Parametres.nbThreads = nbThreads;
            Parametres.tailleCache = tailleCache;
        } catch (IOException ex) {
            Logger.getLogger(Parametres.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
