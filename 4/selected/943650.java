package Thread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import lecture_ecriture.WriteFile;
import zip.OutilsZip;
import Utilitaires.Comptage;
import Utilitaires.ComptageAvantZip;
import Utilitaires.Copy;
import Utilitaires.FileUtility;
import Utilitaires.GestionRepertoire;
import Utilitaires.Historique;
import Utilitaires.VariableEnvironement;
import accesBDD.GestionDemandes;

public class Thread_Sauvegarde extends Thread {

    protected JLabel MESSAGE_UTILISATEUR;

    protected String EMPLACEMENT;

    protected JProgressBar PROGRESSION_EN_COURS, PROGRESSION_TOTALE;

    protected JList LISTE_SAUVEGARDE, LISTE_EXCLUSION;

    protected DefaultListModel MODEL_SAUVEGARDE, MODEL_EXCLUSION;

    int nbFichierACopier = 0;

    protected JButton PAUSE, GO, STOP, REFRESH, SAVE_OK, SAVE_NOK;

    private volatile boolean pause = false;

    private JCheckBox STOP_MACHINE;

    /**
	 * Affiche les differentes etapes du demarrage du logiciel,
	 * verifie certaines choses.
	 *
	 * @param actionListener 
	 * 
	 * @param Fenetre -JFrame pour l'affichage des resultats
	 * @param operation_jLabel -JLabel message pour l'utilisateur
	 * @param jTextField -JTextField message pour l'utilisateur
	 * @param jProgressBar -JProgressBar pour la progression
	 */
    public Thread_Sauvegarde(String destination, JProgressBar progressEnCours, JProgressBar progressTotal, JLabel operation, JList ListeSauvegarde, DefaultListModel ModelSauvegarde, JList ListeExclu, DefaultListModel ModelExclu, JButton Pause, JButton Go, JButton Stop, JButton Refresh, JButton Save_Ok, JButton Save_Nok, JCheckBox Stop_Machine) {
        MESSAGE_UTILISATEUR = operation;
        EMPLACEMENT = destination;
        PROGRESSION_EN_COURS = progressEnCours;
        PROGRESSION_TOTALE = progressTotal;
        LISTE_SAUVEGARDE = ListeSauvegarde;
        MODEL_SAUVEGARDE = ModelSauvegarde;
        LISTE_EXCLUSION = ListeExclu;
        MODEL_EXCLUSION = ModelExclu;
        PAUSE = Pause;
        GO = Go;
        STOP = Stop;
        REFRESH = Refresh;
        SAVE_OK = Save_Ok;
        SAVE_NOK = Save_Nok;
        STOP_MACHINE = Stop_Machine;
    }

    public void run() {
        File encours = new File(GestionRepertoire.RecupRepTravail() + "/enCours.txt");
        try {
            encours.createNewFile();
        } catch (IOException e3) {
            e3.printStackTrace();
        }
        PROGRESSION_EN_COURS.setValue(0);
        PROGRESSION_EN_COURS.setString(0 + " %");
        PROGRESSION_TOTALE.setValue(0);
        PROGRESSION_TOTALE.setString(0 + " %");
        MESSAGE_UTILISATEUR.setText("");
        long dateDuJour = System.currentTimeMillis();
        Date actuelle = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String Date = dateFormat.format(actuelle);
        String FileName = Date + "_AutoBackup.zip";
        EMPLACEMENT = EMPLACEMENT + "\\" + FileName;
        GestionDemandes.executeRequete("INSERT INTO SAUVEGARDE (DATE_SAUVEGARDE, EMPLACEMENT_SAUVEGARDE) VALUES (" + dateDuJour + ",'" + EMPLACEMENT + "')");
        int nbDeLigne = LISTE_SAUVEGARDE.getModel().getSize();
        String TempDirectory = VariableEnvironement.VarEnvSystem("TMP") + "\\" + Date;
        boolean succesCopie = false;
        File tempDirectory = null;
        MODEL_EXCLUSION.addElement(TempDirectory);
        tempDirectory = new File(TempDirectory);
        if (tempDirectory.exists() == false) {
            tempDirectory.mkdirs();
            try {
                Historique.ecrire("Cr�ation du repertoire temporaire : " + tempDirectory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (tempDirectory.canWrite() == false) {
            JOptionPane.showMessageDialog(null, "Impossible de r�aliser la sauvegarde\n\r" + "La creation du dossier temporaire � echou�", "Sauvegarde Impossible", JOptionPane.ERROR_MESSAGE);
            try {
                Historique.ecrire("Pb lors de la sauvegarde : La creation du dossier temporaire � echou�");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        for (int i = 0; i < nbDeLigne; i++) {
            LISTE_SAUVEGARDE.setSelectedIndex(i);
            final int index = i;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {

                    public void run() {
                        LISTE_SAUVEGARDE.ensureIndexIsVisible(index);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            String WorkingDirectory = MODEL_SAUVEGARDE.getElementAt(i).toString();
            String NomRepertoire = WorkingDirectory.substring(WorkingDirectory.lastIndexOf("\\"), WorkingDirectory.length());
            File Actu = new File(WorkingDirectory);
            if (Actu.isDirectory() == true && Actu.exists()) {
                Comptage count = new Comptage(WorkingDirectory, MESSAGE_UTILISATEUR, LISTE_EXCLUSION, MODEL_EXCLUSION);
                nbFichierACopier = count.getNbFichier() + nbFichierACopier;
                MESSAGE_UTILISATEUR.setText("Copie de " + 0 + " fichier(s)  / sur " + nbFichierACopier + " au total");
                try {
                    Historique.ecrire("Nombre de fichier � sauvegarder : " + nbFichierACopier);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                Copy save = null;
                try {
                    if (pause) {
                        waitThread();
                    }
                    save = new Copy(PAUSE, GO, STOP, WorkingDirectory, TempDirectory + "\\" + NomRepertoire, nbFichierACopier, PROGRESSION_EN_COURS, PROGRESSION_TOTALE, TempDirectory + "\\" + NomRepertoire, MESSAGE_UTILISATEUR, LISTE_EXCLUSION, MODEL_EXCLUSION);
                    try {
                        VerifDossierVideEtSupprSiVide(new File(TempDirectory + "\\" + NomRepertoire));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (SQLException e1) {
                    e1.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                nbDeLigne = LISTE_SAUVEGARDE.getModel().getSize();
                int nbderreur = save.getNbErreur();
                if (nbderreur != 0) {
                    succesCopie = false;
                    try {
                        Historique.ecrire("Il y a eu des pb lors de la copie des fichiers vers le repertoire temporaire ");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Historique.ecrire("Nombre d'erreur lors de la copie : " + nbderreur);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int ID_SAUVEGARDE = 0;
                    try {
                        ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    String RequetteDelete = "DELETE FROM SAUVEGARDE WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    RequetteDelete = "DELETE FROM FICHIER WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    return;
                } else {
                    succesCopie = true;
                }
            }
            if (Actu.isFile() == true && Actu.exists()) {
                nbFichierACopier = 1 + nbFichierACopier;
                long tailleSource = Actu.length();
                MESSAGE_UTILISATEUR.setText("Copie de " + nbFichierACopier + " fichier(s)  / sur " + nbFichierACopier + " au total");
                File tempo = new File(TempDirectory + "\\" + NomRepertoire);
                String cheminDuFichier = Actu.getAbsolutePath();
                long dateDuFichierOriginal = Actu.lastModified();
                long dateDuFIchierEnBase = 0;
                int nbEnregistrementPresent = 0;
                try {
                    String cheminDuFichierSansAccent = cheminDuFichier.replaceAll("'", "");
                    nbEnregistrementPresent = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT count(EMPLACEMENT_FICHIER) FROM FICHIER WHERE EMPLACEMENT_FICHIER = '" + cheminDuFichierSansAccent + "'"));
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                if (nbEnregistrementPresent != 0) {
                    try {
                        String cheminDuFichierSansAccent = cheminDuFichier.replaceAll("'", "");
                        dateDuFIchierEnBase = Long.parseLong(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT a.DATE_FICHIER FROM FICHIER a where  a.EMPLACEMENT_FICHIER= '" + cheminDuFichierSansAccent + "'"));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    if (dateDuFichierOriginal == dateDuFIchierEnBase) {
                        PROGRESSION_EN_COURS.setString(cheminDuFichier.toString() + " ignor� car non modifi� depuis la derni�re sauvegarde");
                        succesCopie = true;
                    } else {
                        int ID_SAUVEGARDE = 0;
                        try {
                            ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        String SRCSansAccent = cheminDuFichier.replaceAll("'", "").toString().trim();
                        GestionDemandes.executeRequete("INSERT INTO FICHIER (ID_SAUVEGARDE,DATE_FICHIER,EMPLACEMENT_FICHIER) VALUES (" + ID_SAUVEGARDE + "," + dateDuFichierOriginal + ",'" + SRCSansAccent + "')");
                        if (tailleSource > 1000000) {
                            succesCopie = copyAvecProgress(Actu, tempo, PROGRESSION_EN_COURS);
                        } else {
                            succesCopie = copyAvecProgressNIO(Actu, tempo, PROGRESSION_EN_COURS);
                        }
                    }
                } else {
                    int ID_SAUVEGARDE = 0;
                    try {
                        ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    String SRCSansAccent = cheminDuFichier.replaceAll("'", "").toString().trim();
                    GestionDemandes.executeRequete("INSERT INTO FICHIER (ID_SAUVEGARDE,DATE_FICHIER,EMPLACEMENT_FICHIER) VALUES (" + ID_SAUVEGARDE + "," + dateDuFichierOriginal + ",'" + SRCSansAccent + "')");
                    long tailleSource1 = Actu.length();
                    if (tailleSource1 > 15000000) {
                        succesCopie = copyAvecProgress(Actu, tempo, PROGRESSION_EN_COURS);
                    } else {
                        succesCopie = copyAvecProgressNIO(Actu, tempo, PROGRESSION_EN_COURS);
                    }
                    if (succesCopie == false) {
                        try {
                            Historique.ecrire("Erreur lors de la copie du fichier : " + Actu.getAbsolutePath() + " vers : " + tempo.getAbsolutePath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!succesCopie) {
                    int ID_SAUVEGARDE = 0;
                    try {
                        ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    String RequetteDelete = "DELETE FROM SAUVEGARDE WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    RequetteDelete = "DELETE FROM FICHIER WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    return;
                }
            }
            if ((!Actu.isDirectory() && !Actu.isFile()) || !Actu.exists()) {
                String RepActu = Actu.getAbsolutePath();
                JOptionPane.showMessageDialog(null, "Impossible de r�aliser la sauvegarde\n\r" + "Le repertoire/fichier: " + RepActu + " est introuvable.\n\r Veuillez verifier la liste des dossiers/fichiers a sauvegarder", "Sauvegarde Impossible", JOptionPane.ERROR_MESSAGE);
                PROGRESSION_EN_COURS.setValue(0);
                PROGRESSION_EN_COURS.setString(0 + " %");
                PROGRESSION_TOTALE.setValue(0);
                PROGRESSION_TOTALE.setString(0 + " %");
                MESSAGE_UTILISATEUR.setText("");
                REFRESH.setEnabled(true);
                STOP.setEnabled(false);
                GO.setEnabled(true);
                PAUSE.setEnabled(false);
                SAVE_NOK.setVisible(true);
                boolean succesDelete = encours.delete();
                if (!succesDelete) {
                    encours.deleteOnExit();
                }
                int ID_SAUVEGARDE = 0;
                try {
                    ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                String RequetteDelete = "DELETE FROM SAUVEGARDE WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                @SuppressWarnings("unused") boolean succes1 = GestionDemandes.executeRequete(RequetteDelete);
                RequetteDelete = "DELETE FROM FICHIER WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                @SuppressWarnings("unused") boolean succes2 = GestionDemandes.executeRequete(RequetteDelete);
                try {
                    Historique.ecrire("Pb lors de la sauvegarde : Le repertoire/fichier: " + RepActu + " est introuvable.\n\r Veuillez verifier la liste des dossiers/fichiers a sauvegarder");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                if (STOP_MACHINE.isSelected()) {
                    try {
                        WriteFile.WriteLineInNewFile("La derniere sauvegarde ne s'est pas correctement d�roul�e", GestionRepertoire.RecupRepTravail() + "/IniFile/Resultat.txt");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    LanceArret();
                    return;
                } else {
                    return;
                }
            }
        }
        if (pause) {
            waitThread();
        }
        boolean succesZip = false;
        if (succesCopie == true) {
            try {
                Historique.ecrire("Copie des diff�rents fichiers dans le repertoire temporaire reussie.");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            ComptageAvantZip count = new ComptageAvantZip(TempDirectory, MESSAGE_UTILISATEUR);
            int nbDeFIchierAZipper = count.getNbFichier();
            if (nbDeFIchierAZipper == 0) {
                GestionDemandes.executeRequete("DELETE FROM SAUVEGARDE WHERE DATE_SAUVEGARDE=" + dateDuJour + " and  EMPLACEMENT_SAUVEGARDE='" + EMPLACEMENT + "'");
                PROGRESSION_EN_COURS.setValue(0);
                PROGRESSION_EN_COURS.setString(0 + " %");
                PROGRESSION_TOTALE.setValue(0);
                PROGRESSION_TOTALE.setString(0 + " %");
                MESSAGE_UTILISATEUR.setText("");
                REFRESH.setEnabled(true);
                STOP.setEnabled(false);
                GO.setEnabled(true);
                PAUSE.setEnabled(false);
                SAVE_OK.setVisible(true);
                int ID_SAUVEGARDE = 0;
                try {
                    ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                String RequetteDelete = "DELETE FROM SAUVEGARDE WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                GestionDemandes.executeRequete(RequetteDelete);
                RequetteDelete = "DELETE FROM FICHIER WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                GestionDemandes.executeRequete(RequetteDelete);
                boolean succesDelete = encours.delete();
                if (!succesDelete) {
                    encours.deleteOnExit();
                }
                try {
                    Historique.ecrire("Sauvegarde �ffectu�e avec succ�s,pas de fichier modifi� � sauvegarder");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FileUtility.recursifDelete(tempDirectory);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e);
                }
                if (STOP_MACHINE.isSelected()) {
                    try {
                        WriteFile.WriteLineInNewFile("La derniere sauvegarde s'est correctement d�roul�e", GestionRepertoire.RecupRepTravail() + "/IniFile/Resultat.txt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LanceArret();
                    return;
                } else {
                    return;
                }
            }
            if (nbDeFIchierAZipper != 0) {
                try {
                    MESSAGE_UTILISATEUR.setText("Compression en cours");
                    Historique.ecrire("Archivage du dossier : " + TempDirectory + " vers le chemin : " + EMPLACEMENT);
                    succesZip = OutilsZip.zipDir(TempDirectory, EMPLACEMENT, nbDeFIchierAZipper, PROGRESSION_TOTALE, PROGRESSION_EN_COURS, MESSAGE_UTILISATEUR);
                } catch (FileNotFoundException e) {
                    System.out.println(e);
                    JOptionPane.showMessageDialog(null, "Pb lors de la sauvegarde : \n\r" + e, "Erreur", JOptionPane.ERROR_MESSAGE);
                    PROGRESSION_EN_COURS.setValue(0);
                    PROGRESSION_EN_COURS.setString(0 + " %");
                    PROGRESSION_TOTALE.setValue(0);
                    PROGRESSION_TOTALE.setString(0 + " %");
                    MESSAGE_UTILISATEUR.setText("");
                    REFRESH.setEnabled(true);
                    STOP.setEnabled(false);
                    GO.setEnabled(true);
                    PAUSE.setEnabled(false);
                    SAVE_NOK.setVisible(true);
                    int ID_SAUVEGARDE = 0;
                    try {
                        ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    String RequetteDelete = "DELETE FROM SAUVEGARDE WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    RequetteDelete = "DELETE FROM FICHIER WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    boolean succesDelete = encours.delete();
                    if (!succesDelete) {
                        encours.deleteOnExit();
                    }
                    try {
                        Historique.ecrire("Pb lors de la sauvegarde : " + e);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (STOP_MACHINE.isSelected()) {
                        try {
                            WriteFile.WriteLineInNewFile("La derniere sauvegarde ne s'est pas correctement d�roul�e", GestionRepertoire.RecupRepTravail() + "/IniFile/Resultat.txt");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        LanceArret();
                        return;
                    } else {
                        return;
                    }
                } catch (IOException e) {
                    System.out.println(e);
                    JOptionPane.showMessageDialog(null, "Pb lors de la sauvegarde : \n\r" + e, "Erreur", JOptionPane.ERROR_MESSAGE);
                    PROGRESSION_EN_COURS.setValue(0);
                    PROGRESSION_EN_COURS.setString(0 + " %");
                    PROGRESSION_TOTALE.setValue(0);
                    PROGRESSION_TOTALE.setString(0 + " %");
                    MESSAGE_UTILISATEUR.setText("");
                    REFRESH.setEnabled(true);
                    STOP.setEnabled(false);
                    GO.setEnabled(true);
                    PAUSE.setEnabled(false);
                    SAVE_NOK.setVisible(true);
                    int ID_SAUVEGARDE = 0;
                    try {
                        ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
                    } catch (NumberFormatException e1) {
                        e1.printStackTrace();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                    String RequetteDelete = "DELETE FROM SAUVEGARDE WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    RequetteDelete = "DELETE FROM FICHIER WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
                    GestionDemandes.executeRequete(RequetteDelete);
                    boolean succesDelete = encours.delete();
                    if (!succesDelete) {
                        encours.deleteOnExit();
                    }
                    try {
                        Historique.ecrire("Pb lors de la sauvegarde : " + e);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    if (STOP_MACHINE.isSelected()) {
                        try {
                            WriteFile.WriteLineInNewFile("La derniere sauvegarde ne s'est pas correctement d�roul�e", GestionRepertoire.RecupRepTravail() + "/IniFile/Resultat.txt");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        LanceArret();
                        return;
                    } else {
                        return;
                    }
                }
            }
            if (succesZip == true) {
                try {
                    Historique.ecrire("Sauvegarde �ffectu�e avec succ�s");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FileUtility.recursifDelete(tempDirectory);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println(e);
                }
                PROGRESSION_EN_COURS.setValue(0);
                PROGRESSION_EN_COURS.setString(0 + " %");
                PROGRESSION_TOTALE.setValue(0);
                PROGRESSION_TOTALE.setString(0 + " %");
                MESSAGE_UTILISATEUR.setText("");
                REFRESH.setEnabled(true);
                STOP.setEnabled(false);
                GO.setEnabled(true);
                PAUSE.setEnabled(false);
                SAVE_OK.setVisible(true);
                boolean succesDelete = encours.delete();
                if (!succesDelete) {
                    encours.deleteOnExit();
                }
                if (STOP_MACHINE.isSelected()) {
                    try {
                        WriteFile.WriteLineInNewFile("La derniere sauvegarde s'est correctement d�roul�e", GestionRepertoire.RecupRepTravail() + "/IniFile/Resultat.txt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    LanceArret();
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "La copie des fichiers vers le repertoire temporaire � echou�e.\n\r " + "Veuillez r�-essayer", "Erreur lors de la sauvegarde", JOptionPane.ERROR_MESSAGE);
            PROGRESSION_EN_COURS.setValue(0);
            PROGRESSION_EN_COURS.setString(0 + " %");
            PROGRESSION_TOTALE.setValue(0);
            PROGRESSION_TOTALE.setString(0 + " %");
            MESSAGE_UTILISATEUR.setText("");
            REFRESH.setEnabled(true);
            STOP.setEnabled(false);
            GO.setEnabled(true);
            PAUSE.setEnabled(false);
            SAVE_NOK.setVisible(true);
            int ID_SAUVEGARDE = 0;
            try {
                ID_SAUVEGARDE = Integer.parseInt(GestionDemandes.executeRequeteEtRetourne1Champ("SELECT MAX (ID_SAUVEGARDE) FROM SAUVEGARDE"));
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            String RequetteDelete = "DELETE FROM SAUVEGARDE WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
            GestionDemandes.executeRequete(RequetteDelete);
            RequetteDelete = "DELETE FROM FICHIER WHERE ID_SAUVEGARDE=" + ID_SAUVEGARDE;
            GestionDemandes.executeRequete(RequetteDelete);
            boolean succesDelete = encours.delete();
            if (!succesDelete) {
                encours.deleteOnExit();
            }
            if (STOP_MACHINE.isSelected()) {
                try {
                    WriteFile.WriteLineInNewFile("La derniere sauvegarde ne s'est pas correctement d�roul�e", GestionRepertoire.RecupRepTravail() + "/IniFile/Resultat.txt");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                LanceArret();
            }
            try {
                Historique.ecrire("La copie des fichiers vers le repertoire temporaire � echou�e.\n\r " + "Erreur lors de la sauvegarde");
            } catch (IOException e) {
            }
            return;
        }
    }

    private boolean copyAvecProgress(File sRC2, File dEST2, JProgressBar progressEnCours) {
        boolean resultat = false;
        long PourcentEnCours = 0;
        java.io.FileInputStream sourceFile = null;
        java.io.FileOutputStream destinationFile = null;
        try {
            dEST2.createNewFile();
            sourceFile = new java.io.FileInputStream(sRC2);
            destinationFile = new java.io.FileOutputStream(dEST2);
            long tailleTotale = sRC2.length();
            byte buffer[] = new byte[512 * 1024];
            int nbLecture;
            while ((nbLecture = sourceFile.read(buffer)) != -1) {
                destinationFile.write(buffer, 0, nbLecture);
                long tailleEnCours = dEST2.length();
                PourcentEnCours = ((100 * (tailleEnCours + 1)) / tailleTotale);
                int Pourcent = (int) PourcentEnCours;
                progressEnCours.setValue(Pourcent);
                progressEnCours.setString(sRC2 + " : " + Pourcent + " %");
            }
            resultat = true;
        } catch (java.io.FileNotFoundException f) {
        } catch (java.io.IOException e) {
        } finally {
            try {
                sourceFile.close();
            } catch (Exception e) {
            }
            try {
                destinationFile.close();
            } catch (Exception e) {
            }
        }
        return (resultat);
    }

    @SuppressWarnings("unused")
    private boolean copyAvecProgressNIO(File sRC2, File dEST2, JProgressBar progressEnCours) {
        boolean resultat = false;
        long PourcentEnCours = 0;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(sRC2);
        } catch (FileNotFoundException e) {
            try {
                Historique.ecrire("Erreur � la copie du fichier " + sRC2 + " pour la raison suivante : " + e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dEST2);
        } catch (FileNotFoundException e) {
            try {
                Historique.ecrire("Erreur � la creation du fichier " + dEST2 + " pour la raison suivante : " + e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        }
        java.nio.channels.FileChannel channelSrc = fis.getChannel();
        java.nio.channels.FileChannel channelDest = fos.getChannel();
        progressEnCours.setValue(0);
        progressEnCours.setString(sRC2 + " : 0 %");
        try {
            long tailleCopie = channelSrc.transferTo(0, channelSrc.size(), channelDest);
        } catch (IOException e) {
            try {
                Historique.ecrire("Erreur � la copie du fichier " + sRC2 + " vers la destination " + dEST2 + " pour la raison suivante : " + e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        }
        progressEnCours.setValue(100);
        progressEnCours.setString(sRC2 + " : 100 %");
        try {
            if (channelSrc.size() == channelDest.size()) {
                resultat = true;
            } else {
                resultat = false;
            }
        } catch (IOException e) {
            try {
                Historique.ecrire("Erreur � la copie du fichier " + sRC2 + " pour la raison suivante : " + e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        }
        try {
            fis.close();
        } catch (IOException e) {
            try {
                Historique.ecrire("Impossible de fermer le flux � la copie du fichier " + sRC2 + " pour la raison suivante : " + e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        }
        try {
            fos.close();
        } catch (IOException e) {
            try {
                Historique.ecrire("Impossible de fermer le flux � la copie du fichier " + dEST2 + " pour la raison suivante : " + e);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        }
        return (resultat);
    }

    private void LanceArret() {
        try {
            Historique.ecrire("Arret automatique de la machine");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        String cmdArretMachine = String.format("cmd /c shutdown -s -t 300 -f");
        Runtime r = Runtime.getRuntime();
        Process p = null;
        try {
            p = r.exec(cmdArretMachine);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waitThread() {
        synchronized (this) {
            try {
                this.wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    public void pause() {
        pause = true;
    }

    public void reprise() {
        pause = false;
        synchronized (this) {
            this.notifyAll();
        }
    }

    private void VerifDossierVideEtSupprSiVide(File path) throws IOException {
        if (!path.exists()) {
            throw new IOException("File not found '" + path.getAbsolutePath() + "'");
        }
        if (path.isDirectory()) {
            File[] children = path.listFiles();
            for (int i = 0; children != null && i < children.length; i++) {
                if (children[i].isDirectory() == true) {
                    VerifDossierVideEtSupprSiVide(children[i]);
                } else {
                    if (children.length == 0 && children[i].isFile()) {
                        path.delete();
                    }
                }
            }
            if (!path.delete()) {
                System.out.println("No delete path '" + path.getAbsolutePath() + "'");
            }
        } else if (!path.delete()) throw new IOException("No delete file '" + path.getAbsolutePath() + "'");
    }
}
