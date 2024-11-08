package fr.megiste.interloc;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import fr.megiste.interloc.data.AccesseurFichierHOM;
import fr.megiste.interloc.data.LectureHOMException;
import fr.megiste.interloc.hist.ErreurRecupHistoriqueException;
import fr.megiste.interloc.ihm.InterlocIhm;

public class InterlocMain extends JFrame {

    public static String VERSION = "0.5";

    private static Logger logger = null;

    public static final FilenameFilter HOM_FILTER = new FilenameFilter() {

        public boolean accept(File arg0, String arg1) {
            if (arg1.endsWith(AccesseurFichierHOM.SUFFIX_HOM)) {
                return true;
            }
            return false;
        }
    };

    public static final String SUFFIX_LNK = "lien";

    public static final String NOM_FICHIER_JOURNAL = "interloc.log";

    public static Properties PROPS = new Properties();

    public static File FIC_PROPS = new File("./Interloc.properties");

    public static InterlocIhm fenetre = null;

    private File fichierInit;

    public InterlocMain(String[] args) {
        if (args.length > 0) {
            fichierInit = new File(args[0]);
            if (!fichierInit.exists()) fichierInit = null;
        }
        initInterloc();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        new InterlocMain(args);
    }

    public void initInterloc() {
        getLogger().info("Démarrage interloc");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            if (FIC_PROPS.exists()) {
                logger.info("chargement des properties from " + FIC_PROPS);
                PROPS.load(new FileInputStream(FIC_PROPS));
            }
            logger.info("init de la fen�tre");
            fenetre = new InterlocIhm();
            fenetre.init();
            int x = lirePropEntier("fenetrePrincipale.x", 0);
            int y = lirePropEntier("fenetrePrincipale.y", 0);
            int w = lirePropEntier("fenetrePrincipale.w", 1200);
            int h = lirePropEntier("fenetrePrincipale.h", 600);
            fenetre.setSize(w, h);
            fenetre.setLocation(x, y);
            File fichierACharger = fichierInit;
            if (fichierACharger == null) {
                String dernierFichierOuvert = PROPS.getProperty("dernierFichierOuvert");
                if (dernierFichierOuvert != null && new File(dernierFichierOuvert).exists()) {
                    fichierACharger = new File(dernierFichierOuvert);
                }
            }
            try {
                if (fichierACharger != null) fenetre.chargeFichier(fichierACharger);
            } catch (LectureHOMException e) {
                InterlocMain.erreur(e);
            } catch (ErreurRecupHistoriqueException e) {
                InterlocMain.erreur(e);
            }
            x = lirePropEntier("scroll.x", fenetre.getPosScrollPane().x);
            y = lirePropEntier("scroll.y", fenetre.getPosScrollPane().y);
            fenetre.setPosScrollPane(new Point(x, y));
            fenetre.setVisible(true);
        } catch (ClassNotFoundException e) {
            InterlocMain.quitter(e);
        } catch (InstantiationException e) {
            InterlocMain.quitter(e);
        } catch (IllegalAccessException e) {
            InterlocMain.quitter(e);
        } catch (UnsupportedLookAndFeelException e) {
            InterlocMain.quitter(e);
        } catch (FileNotFoundException e) {
            InterlocMain.quitter(e);
        } catch (IOException e) {
            InterlocMain.quitter(e);
        }
    }

    public static File chercherFichierLocal(String[] chemins, String nom, String suffixe) {
        final String nomFichier = nom.toUpperCase();
        final String suffixeFichier = suffixe.toUpperCase();
        FilenameFilter fnf = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                name = name.toUpperCase();
                return name.indexOf(nomFichier) != -1 && name.endsWith(suffixeFichier);
            }
        };
        for (int i = 0; i < chemins.length; i++) {
            String chemin = chemins[i];
            File loc = new File(chemin);
            File[] res = loc.listFiles(fnf);
            if (res != null && res.length > 0) return res[0];
        }
        String nomsChemins = "" + chemins[0];
        for (int i = 1; i < chemins.length; i++) {
            String chemin = chemins[i];
            nomsChemins = nomsChemins + "," + chemin;
        }
        logger.warning("Impossible de trouver un fichier " + nom + "*." + suffixe + " dans " + nomsChemins);
        return null;
    }

    public static String stackTraceToString(Throwable e) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        return result.toString();
    }

    public static void quitter(Throwable e) {
        erreur(e);
        quitter(1, e.getMessage());
    }

    public static void erreur(Throwable e) {
        logger.severe(e.getMessage());
        logger.severe(stackTraceToString(e));
    }

    public static void quitter(int niveau, String text) {
        try {
            if (PROPS != null && fenetre != null) {
                if (fenetre.getFichierCourant() != null) {
                    PROPS.setProperty("dernierFichierOuvert", fenetre.getFichierCourant().getPath());
                }
                ecrirePropEntier("fenetrePrincipale.x", fenetre.getLocation().x);
                ecrirePropEntier("fenetrePrincipale.y", fenetre.getLocation().y);
                ecrirePropEntier("fenetrePrincipale.w", fenetre.getSize().width);
                ecrirePropEntier("fenetrePrincipale.h", fenetre.getSize().height);
                ecrirePropEntier("scroll.x", fenetre.getPosScrollPane().x);
                ecrirePropEntier("scroll.y", fenetre.getPosScrollPane().y);
                logger.info("Enregistrement des properties");
                PROPS.store(new FileOutputStream(FIC_PROPS), "svg le :" + new Date());
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        if (niveau == 0) getLogger().info(text); else getLogger().severe(text);
        System.exit(niveau);
    }

    public static void quitter() {
        quitter(0, "Arrêt de Interloc");
    }

    public static void initLogger() {
        if (logger == null) {
            Formatter formatter = new Formatter() {

                boolean dernierMessageSpecial = false;

                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

                public String format(LogRecord record) {
                    if (LogRecordLocal.class.isInstance(record)) {
                        dernierMessageSpecial = true;
                        return record.getMessage();
                    } else {
                        StringBuffer sb = new StringBuffer();
                        if (dernierMessageSpecial) {
                            sb.append("\n");
                        }
                        dernierMessageSpecial = false;
                        sb.append("[").append(sdf.format(new Date(record.getMillis()))).append("]");
                        sb.append(record.getLevel().getName()).append(":");
                        sb.append(record.getMessage());
                        sb.append("\n");
                        return sb.toString();
                    }
                }
            };
            Formatter formatter2 = new Formatter() {

                public String format(LogRecord record) {
                    return "";
                }
            };
            logger = Logger.getLogger("");
            if (logger.getHandlers().length > 0) {
                logger.getHandlers()[0].setFormatter(formatter);
            }
            logger = Logger.getLogger("");
            logger.addHandler(new StreamHandler(System.out, formatter2));
            try {
                FileHandler fh = new FileHandler("./" + NOM_FICHIER_JOURNAL, false);
                fh.setFormatter(formatter);
                logger.addHandler(fh);
            } catch (SecurityException e) {
                InterlocMain.quitter(e);
            } catch (IOException e) {
                InterlocMain.quitter(e);
            }
        }
    }

    public static Logger getLogger() {
        if (logger == null) initLogger();
        return logger;
    }

    private static class LogRecordLocal extends LogRecord {

        public LogRecordLocal(Level arg0, String arg1) {
            super(arg0, arg1);
        }
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static int lirePropEntier(String clef, int defaut) {
        int sortie = -999;
        String val = PROPS.getProperty(clef);
        if (val == null) return defaut;
        sortie = Integer.parseInt(val);
        return sortie;
    }

    public static void ecrirePropEntier(String clef, int val) {
        PROPS.setProperty(clef, "" + val);
    }

    public static String readInterlocVersion() {
        Properties props = new Properties();
        try {
            InputStream is = InterlocMain.class.getClassLoader().getResourceAsStream("build.conf");
            props.load(is);
        } catch (IOException e) {
            return "Dev version";
        }
        return InterlocMain.VERSION + "." + props.getProperty("build.number");
    }
}
