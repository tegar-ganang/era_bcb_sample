package Utilitaires;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class CopyAuto {

    protected String src, dest;

    File DEST, SRC;

    int nbTotal;

    int nbDerreur = 0;

    /**
	 * Copie le contenu d'un repertoire vers un autre et affiche le status de la copie dans une barre de progression.
	 * @param src -String Le r�pertoire source
	 * @param dest -String le repertoire de destination
	 * @param nbTotal -int le nb total de fichier a copier qui permet de calculer la progression.
	 * @param progress -JProgressBar la barre de progression.
	 * @param sortieModel -DefaultModelList model de liste
	 * @param sortieList -JList le composant JList.
	 * @throws SQLException 
	 * @throws IOException 
	 */
    public CopyAuto(String src, String dest, final int nbTotal, final JProgressBar progressEnCours, final JProgressBar progressTotal, final String RepRacineLocal, final JLabel label) throws SQLException, IOException {
        this.nbTotal = nbTotal;
        this.src = src;
        this.dest = dest;
        this.SRC = new File(src);
        this.DEST = new File(dest);
        if (!DEST.exists()) {
            if (SRC.isDirectory()) {
                try {
                    Historique.ecrire("Cr�ation du r�p�rtoire : " + DEST);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                DEST.mkdir();
            }
        }
        if (SRC.isFile()) {
            long tailleSource = SRC.length();
            boolean succes = false;
            if (tailleSource > 100000) {
                succes = copyAvecProgress(SRC, DEST, progressEnCours);
            } else {
                succes = copyAvecProgressNIO(SRC, DEST, progressEnCours);
            }
            if (succes == false) {
                nbDerreur++;
                try {
                    Historique.ecrire("Erreur lors de la copie du fichier : " + SRC + " vers : " + DEST);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (SRC.isDirectory()) {
            for (File f : SRC.listFiles()) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        /**
						 * {@inheritDoc}
						 */
                        @Override
                        public void run() {
                            ComptageAuto count = new ComptageAuto(RepRacineLocal, label);
                            int nbEncours = count.getNbFichier();
                            int PourcentProgression = (100 * (nbEncours + 1)) / nbTotal;
                            label.setText("Copie de " + nbEncours + " fichier(s)  / sur " + nbTotal + " au total");
                            progressTotal.setValue(PourcentProgression / 2);
                            progressTotal.setString("Total : " + PourcentProgression / 2 + " %");
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                new CopyAuto(f.getAbsolutePath(), DEST.getAbsoluteFile() + "/" + f.getName(), nbTotal, progressEnCours, progressTotal, RepRacineLocal, label);
            }
        }
    }

    private boolean copyAvecProgressNIO(File sRC2, File dEST2, JProgressBar progressEnCours) throws IOException {
        boolean resultat = false;
        FileInputStream fis = new FileInputStream(sRC2);
        FileOutputStream fos = new FileOutputStream(dEST2);
        java.nio.channels.FileChannel channelSrc = fis.getChannel();
        java.nio.channels.FileChannel channelDest = fos.getChannel();
        progressEnCours.setValue(0);
        progressEnCours.setString(sRC2 + " : 0 %");
        channelSrc.transferTo(0, channelSrc.size(), channelDest);
        progressEnCours.setValue(100);
        progressEnCours.setString(sRC2 + " : 100 %");
        if (channelSrc.size() == channelDest.size()) {
            resultat = true;
        } else {
            resultat = false;
        }
        fis.close();
        fos.close();
        return (resultat);
    }

    /**
	 * Permet de fixer la date systeme en fonction de la date de cr�ation d'un fichier
	 * @param cheminDuFichier -String le fichier dont on se sert pour fixer la date Systeme
	 * 
	 */
    public static void FixeDateSystemeALaDateDeCreationDuFichier(String cheminDuFichier) {
        Runtime r = Runtime.getRuntime();
        String cmdRecupDate = String.format("cmd.exe /c dir /TC %s | find \"/\"  > tmp.txt", cheminDuFichier);
        try {
            Process p = r.exec(cmdRecupDate);
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String cmdSetDate = String.format("cmd.exe /c FOR /F \"tokens=1-4 delims= \" %%i in (tmp.txt) do DATE %%i");
        try {
            Process p = r.exec(cmdSetDate);
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String cmdEffaceTmpText = String.format("cmd.exe /c del tmp.txt");
        try {
            Process p = r.exec(cmdEffaceTmpText);
            try {
                p.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
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
                progressEnCours.setString(sRC2.getName() + " : " + Pourcent + " %");
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

    public int getNbErreur() {
        return nbDerreur;
    }
}
