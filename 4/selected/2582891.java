package org.at.lettres.clonage.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.at.lettres.clonage.exceptions.UtilisationFichierInterditeException;

/**
 * Classe utilitaire pour gérer facilement les fichiers.
 * 
 * @author compijam
 */
public final class FileUtils {

    /**
     * Copie un fichier dans un autre.
     * 
     * @param pFichierSource Fichier source.
     * @param pFichierDest Fichier destination.
     */
    public static void copier(final File pFichierSource, final File pFichierDest) {
        FileChannel vIn = null;
        FileChannel vOut = null;
        try {
            vIn = new FileInputStream(pFichierSource).getChannel();
            vOut = new FileOutputStream(pFichierDest).getChannel();
            vIn.transferTo(0, vIn.size(), vOut);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (vIn != null) {
                try {
                    vIn.close();
                } catch (IOException e) {
                }
            }
            if (vOut != null) {
                try {
                    vOut.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Efface le fichier donné en paramètre. En cas d'erreur, un message est affiché.
     * 
     * @param pFichierLettre Le fichier à supprimer.
     */
    public static void effaceFichier(File pFichierLettre) {
        if (!pFichierLettre.delete()) {
            MsgUtils.afficheMessageErreur(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.supprimposs") + pFichierLettre);
        }
    }

    /**
     * Dézippe le fichier donné en paramètre.
     * 
     * @param pFichierLettreType Le fichier à dézipper.
     */
    public static void dezippe(File pFichierLettreType) {
        try {
            final String lRepLettreTypeCopiee = pFichierLettreType.getParent() + File.separator;
            final ZipFile lZipFile = new ZipFile(pFichierLettreType);
            final Enumeration<? extends ZipEntry> lZipEnum = lZipFile.entries();
            while (lZipEnum.hasMoreElements()) {
                final ZipEntry lZipEntry = (ZipEntry) lZipEnum.nextElement();
                if (lZipEntry.isDirectory()) {
                    final File lNouveauRep = new File(lRepLettreTypeCopiee + lZipEntry.getName());
                    MsgUtils.afficheMessageSsRetLigne(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.creatrep") + lNouveauRep + "...");
                    lNouveauRep.mkdir();
                    MsgUtils.afficheMessage(java.util.ResourceBundle.getBundle("txt/txt").getString("com.termine"));
                } else {
                    final String lNomFichierAExtraire = lRepLettreTypeCopiee + lZipEntry.getName();
                    final File lRepertoireParent = new File(lNomFichierAExtraire).getParentFile();
                    if (!lRepertoireParent.exists()) {
                        lRepertoireParent.mkdirs();
                    }
                    MsgUtils.afficheMessageSsRetLigne(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.extraction") + lNomFichierAExtraire + "...");
                    final InputStream lInputStream = lZipFile.getInputStream(lZipEntry);
                    final FileOutputStream lFileOutputStream = new FileOutputStream(lNomFichierAExtraire);
                    int lCaractereLu;
                    while ((lCaractereLu = lInputStream.read()) != -1) {
                        lFileOutputStream.write(lCaractereLu);
                    }
                    lInputStream.close();
                    lFileOutputStream.close();
                    MsgUtils.afficheMessage(java.util.ResourceBundle.getBundle("txt/txt").getString("com.termine"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Zippe un répertoire.
     * 
     * @param pZipOutputStream Un flux d'écriture dans un fichier zip, ouvert sur le fichier à écrire.
     * @param pFichiersODT Le répertoire qui contient les fichiers du futur document .odt.
     * @param pNomRepertoire Le nom du répertoire à zipper.
     * @param pIsRacine 
     * @throws FileNotFoundException Si un fichier n'a pas été trouvé.
     * @throws IOException En cas d'erreur d'E/S.
     */
    public static void zipRepertoire(final ZipOutputStream pZipOutputStream, final File pFichiersODT, final String pNomRepertoire, final boolean pIsRacine) throws FileNotFoundException, IOException {
        final String lNomFichierDansArchive;
        if (pIsRacine) {
            lNomFichierDansArchive = "";
        } else {
            lNomFichierDansArchive = new StringBuilder(pNomRepertoire).append(pFichiersODT.getName()).append(pFichiersODT.isDirectory() ? '/' : "").toString();
        }
        if (pFichiersODT.isDirectory()) {
            for (final File lFichier : pFichiersODT.listFiles()) zipRepertoire(pZipOutputStream, lFichier, lNomFichierDansArchive, false);
            return;
        }
        BufferedInputStream lBufferedInputStream = null;
        final int lTailleBuffer = 1024;
        final byte[] lBuffer = new byte[lTailleBuffer];
        try {
            lBufferedInputStream = new BufferedInputStream(new FileInputStream(pFichiersODT), lTailleBuffer);
            final ZipEntry lZipEntry = new ZipEntry(lNomFichierDansArchive);
            pZipOutputStream.putNextEntry(lZipEntry);
            int lNbCaracsLus;
            while ((lNbCaracsLus = lBufferedInputStream.read(lBuffer, 0, lTailleBuffer)) != -1) {
                pZipOutputStream.write(lBuffer, 0, lNbCaracsLus);
            }
            pZipOutputStream.closeEntry();
        } finally {
            if (lBufferedInputStream != null) {
                lBufferedInputStream.close();
            }
        }
    }

    /**
     * Vérifie qu'il est possible d'écrire dans le fichier donné en paramètre. Si ce n'est pas possible,
     * une exception est levée.
     * 
     * @param pNomFichier Nom du fichier à tester.
     * @param pFichier Fichier à tester.
     * @param pIdFichier L'identifiant du fichier. Permet de savoir quel champ 
     * texte colorier en cas d'erreur.
     * @param pIsFichier Vaut true si le fichier doit être un fichier, false s'il
     * doit être un répertoire.
     * @throws UtilisationFichierInterditeException S'il n'est pas possible
     * d'écrire dans le fichier.
     */
    public static void verifAutorisationEcritureFichier(final String pNomFichier, final File pFichier, final int pIdFichier, final boolean pIsFichier) throws UtilisationFichierInterditeException {
        if ("".equals(pNomFichier)) {
            throw new UtilisationFichierInterditeException(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.ficvide"), pIdFichier);
        }
        if (!pFichier.exists()) {
            throw new UtilisationFichierInterditeException(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.fic") + pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.nexistepas"), pIdFichier);
        }
        if (!pFichier.canWrite()) {
            throw new UtilisationFichierInterditeException(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.fic") + pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.ecritimposs"), pIdFichier);
        }
        if (pFichier.isFile() && !pIsFichier) {
            throw new UtilisationFichierInterditeException(pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.repdemande"), pIdFichier);
        }
        if (pFichier.isDirectory() && pIsFichier) {
            throw new UtilisationFichierInterditeException(pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.ficdemande"), pIdFichier);
        }
    }

    /**
     * Vérifie qu'il est possible de lire un fichier. Lève une exception s'il n'est pas possible de le lire.
     * 
     * @param pNomFichier Le nom du fichier.
     * @param pFichier Le fichier à vérifier.
     * @param pIdFichier L'identifiant du fichier. Permet de savoir quel champ 
     * texte colorier en cas d'erreur.
     * @throws UtilisationFichierInterditeException S'il n'est pas possible de
     * lire le fichier.
     */
    public static void verifAutorisationLectureFichier(final String pNomFichier, final File pFichier, final int pIdFichier, final boolean pIsFichier) throws UtilisationFichierInterditeException {
        if ("".equals(pNomFichier)) {
            throw new UtilisationFichierInterditeException(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.ficvide"), pIdFichier);
        }
        if (!pFichier.exists()) {
            throw new UtilisationFichierInterditeException(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.fic") + pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.nexistepas"), pIdFichier);
        }
        if (!pFichier.canRead()) {
            throw new UtilisationFichierInterditeException(java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.fic") + pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.lectimposs"), pIdFichier);
        }
        if (pFichier.isFile() && !pIsFichier) {
            throw new UtilisationFichierInterditeException(pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.repdemande"), pIdFichier);
        }
        if (pFichier.isDirectory() && pIsFichier) {
            throw new UtilisationFichierInterditeException(pNomFichier + " " + java.util.ResourceBundle.getBundle("txt/txt").getString("FileUtils.msg.ficdemande"), pIdFichier);
        }
    }
}
