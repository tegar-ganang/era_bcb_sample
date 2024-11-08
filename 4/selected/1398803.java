package org.at.lettres.clonage.moteur;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;
import org.at.lettres.clonage.donnees.DonneesDestinataire;
import org.at.lettres.clonage.utils.FileUtils;
import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import java.security.InvalidParameterException;
import org.at.lettres.clonage.exceptions.UtilisationFichierInterditeException;
import org.at.lettres.clonage.utils.MsgUtils;

/**
 * Classe gérant le clonage du document-type et l'ajout des personnalisations de l'utilisateur.
 * 
 * Commande à exécuter : soffice -headless -accept="socket,port=8100;urp;"
 * 
 * @author compijam
 */
public final class MoteurLettre {

    /**
     * Le fichier contenant la lettre-type à cloner.
     */
    private File aFichierLettreType;

    /**
     * Le répertoire de sortie, où seront stockés tous les fichiers clonés.
     */
    private File aRepertoireSortie;

    /**
     * L'objet qui contient toutes les données du programme.
     */
    private DonneesProgramme aDonneesProgramme;

    /**
     * Le texte du fichier à cloner.
     */
    private List<String> aListeContenu;

    /**
     * Constructeur du moteur.
     */
    public MoteurLettre() {
        super();
    }

    /**
     * Lance l'exécution du clonage.
     * 
     * @param pArguments Les arguments du programme tels qu'ils seraient si cela avait été la 
     * méthode main(String []). L'ordre et le contenu sont les suivants :
     * - args[0] = chemin du fichier de propriétés
     * - args[1] = chemin du document-type
     * - args[2] = chemin du répertoire de sortie
     * @throws InvalidParameterException Si le nombre d'arguments est inexact.
     * @throws UtilisationFichierInterditeException S'il est impossible d'utiliser les fichiers donnés en paramètres.
     * @throws IOException En cas d'erreur d'E/S.
     */
    public void execute(final String[] pArguments) throws InvalidParameterException, UtilisationFichierInterditeException, IOException {
        if (pArguments.length != 3) {
            throw new InvalidParameterException();
        }
        final File lFichierDonneesDestinataires = new File(pArguments[0]);
        aFichierLettreType = new File(pArguments[1]);
        aRepertoireSortie = new File(pArguments[2]);
        FileUtils.verifAutorisationLectureFichier(pArguments[0], lFichierDonneesDestinataires, 0, true);
        FileUtils.verifAutorisationLectureFichier(pArguments[1], aFichierLettreType, 1, true);
        FileUtils.verifAutorisationEcritureFichier(pArguments[2], aRepertoireSortie, 2, false);
        aDonneesProgramme = new DonneesProgramme();
        aDonneesProgramme.initialisation(lFichierDonneesDestinataires);
        executeClonage();
    }

    /**
     * Exécute le clonage du document-type.
     * 
     * @throws IOException En cas d'erreur d'E/S.
     */
    private void executeClonage() throws IOException {
        final String lNomLettreRef = aDonneesProgramme.getRepTravail().getAbsolutePath() + File.separator + aFichierLettreType.getName();
        final File lFichierLettreRef = new File(lNomLettreRef);
        FileUtils.copier(aFichierLettreType, lFichierLettreRef);
        FileUtils.dezippe(lFichierLettreRef);
        FileUtils.effaceFichier(lFichierLettreRef);
        final File lContentXmlPDG = new File(aDonneesProgramme.getRepTravail().getAbsolutePath() + File.separator + "content.xml");
        initLecture(lContentXmlPDG);
        traitementLettre(lContentXmlPDG);
    }

    /**
     * Remplace les balises par les textes personnalisés tels que défini dans le 
     * fichier de propriétés. Une fois le remplacement fait, sauvegarde le 
     * tout sous format OpenOffice (.odt).
     * 
     * @param pContentXmlFile Le fichier dans lequel on écrira (peu importe son
     * contenu, puisqu'on l'a déjà).
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    private void traitementLettre(File pContentXmlFile) throws IOException {
        OpenOfficeConnection vOpenOfficeConnection = new SocketOpenOfficeConnection(8100);
        try {
            vOpenOfficeConnection.connect();
        } catch (ConnectException ex) {
            throw new IOException(java.util.ResourceBundle.getBundle("txt/txt").getString("com.msg.conneximposs"), ex);
        }
        for (DonneesDestinataire lDonneesDestinataire : aDonneesProgramme.getEnsembleDonnees().getEnsembleDonnees()) {
            MsgUtils.afficheMessageSsRetLigne(java.util.ResourceBundle.getBundle("txt/txt").getString("com.msg.generationclone") + lDonneesDestinataire.getIdDestinataire() + "...");
            final String vContenuPersonnalise = getContenuPersonnalise(lDonneesDestinataire);
            final BufferedWriter out = new BufferedWriter(new FileWriter(pContentXmlFile));
            out.write(vContenuPersonnalise);
            out.close();
            final String lCheminFichierSsExtRet = creationFichierODT(aRepertoireSortie, pContentXmlFile.getParentFile(), lDonneesDestinataire);
            conversionODTEnPDF(vOpenOfficeConnection, aRepertoireSortie, lDonneesDestinataire, lCheminFichierSsExtRet);
            MsgUtils.afficheMessage(java.util.ResourceBundle.getBundle("txt/txt").getString("com.termine"));
        }
        vOpenOfficeConnection.disconnect();
    }

    /**
     * Convertit un fichier OpenOffice .odt en fichier .pdf.
     * 
     * @param pOpenOfficeConnection La connexion au serveur de conversion d'OpenOffice.
     * @param pRepertoireSortie Le répertoire où seront stockés les fichiers clonés.
     * @param pDonneesDestinataire Les données concernant le destinataire.
     * @param pCheminFichierSsExtRet Le nom complet du fichier à convertir, sans 
     * l'extension (sans ".odt").
     * @throws ConnectException En cas d'erreur de connexion au serveur de conversion
     * d'OpenOffice.
     */
    private void conversionODTEnPDF(final OpenOfficeConnection pOpenOfficeConnection, final File pRepertoireSortie, final DonneesDestinataire pDonneesDestinataire, final String pCheminFichierSsExtRet) throws ConnectException {
        final String lCheminFichierODT = pCheminFichierSsExtRet + ".odt";
        final String lCheminFichierPDF = pCheminFichierSsExtRet + ".pdf";
        final File inputFile = new File(lCheminFichierODT);
        final File outputFile = new File(lCheminFichierPDF);
        final DocumentConverter lDocumentConverter = new OpenOfficeDocumentConverter(pOpenOfficeConnection);
        lDocumentConverter.convert(inputFile, outputFile);
    }

    /**
     * Crée un fichier OpenOffice .odt.
     * 
     * @param pRepertoireSortie Le répertoire où seront stockés les fichiers clonés.
     * @param pRepLettreClone Le répertoire qui contient tous les fichiers du futur document .odt.
     * @param pDonneesDestinataire Les données concernant le destinataire.
     * @return Le nom complet du fichier, sans l'extension.
     * @throws IOException En cas d'erreur d'entrée/sortie.
     */
    private String creationFichierODT(final File pRepertoireSortie, final File pRepLettreClone, final DonneesDestinataire pDonneesDestinataire) throws IOException {
        final String lCheminFichierSsExtRet = pRepertoireSortie.getAbsolutePath() + File.separator + aFichierLettreType.getName().substring(0, aFichierLettreType.getName().lastIndexOf(".odt")) + "_" + pDonneesDestinataire.getIdDestinataire();
        final String vCheminFichierODT = lCheminFichierSsExtRet + ".odt";
        final BufferedOutputStream vBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(vCheminFichierODT));
        final ZipOutputStream vZipOutputStream = new ZipOutputStream(vBufferedOutputStream);
        FileUtils.zipRepertoire(vZipOutputStream, pRepLettreClone, "", true);
        vZipOutputStream.close();
        return lCheminFichierSsExtRet;
    }

    /**
     * Retourne le contenu personnalisé d'un fichier cloné.
     * 
     * @param pDonneesDestinataire Les données concernant le destinataire.
     * @return Le contenu personnalisé du fichier cloné.
     */
    private String getContenuPersonnalise(final DonneesDestinataire pDonneesDestinataire) {
        final StringBuffer lStringBuffer = new StringBuffer();
        for (int i = 0; i < aListeContenu.size(); i++) {
            final String lContenu = aListeContenu.get(i);
            final List<String> lContenuMap = pDonneesDestinataire.getDonneesMap().get(lContenu);
            if (lContenuMap != null) {
                lStringBuffer.append(getContenuBalise(lContenuMap, i));
            } else {
                lStringBuffer.append(lContenu);
            }
        }
        return lStringBuffer.toString();
    }

    /**
     * Retourne le texte qui remplace la balise. Si pContenuMap n'a qu'un élément, on ne prend que cet
     * élément. Si elle en a plusieurs, on prend le premier élément tel quel. Les suivants seront entourés
     * des balises de formattage OpenOffice. En effet, si le formattage préconisait par exemple un retour
     * à la ligne, celui doit être fait pour chacun des éléments.
     * 
     * @param pContenuMap La liste qui contient tous les textes qui remplaceront la balise.
     * @param pIndiceDansListeContenu L'indice de positionnement dans la liste des contenus.
     * @return Le texte qui remplacera la balise.
     */
    private String getContenuBalise(final List<String> pContenuMap, final int pIndiceDansListeContenu) {
        final StringBuffer lStrBuf = new StringBuffer();
        if (pContenuMap.size() == 0) {
            lStrBuf.append("");
        } else {
            lStrBuf.append(pContenuMap.get(0));
            if (pContenuMap.size() > 1) {
                int lIndiceChevron = aListeContenu.get(pIndiceDansListeContenu - 1).lastIndexOf('<');
                final String lBaliseAvantContenuPerso = aListeContenu.get(pIndiceDansListeContenu - 1).substring(lIndiceChevron);
                lIndiceChevron = aListeContenu.get(pIndiceDansListeContenu - 1).indexOf('>');
                final String lBaliseApresContenuPerso = aListeContenu.get(pIndiceDansListeContenu + 1).substring(0, lIndiceChevron + 1);
                for (int i = 1; i < pContenuMap.size(); i++) {
                    lStrBuf.append(lBaliseApresContenuPerso);
                    lStrBuf.append(lBaliseAvantContenuPerso);
                    lStrBuf.append(pContenuMap.get(i));
                }
            }
        }
        return lStrBuf.toString();
    }

    /**
     * Lit le fichier de référence pour différencier le contenu fixe du contenu personnalisable.
     * 
     * @param pFile Le fichier de référence.
     * @throws FileNotFoundException Si le fichier n'a pas été trouvé.
     * @throws IOException En cas d'erreur de lecture.
     */
    private void initLecture(File pFile) throws FileNotFoundException, IOException {
        final FileChannel lFileChannel = new FileInputStream(pFile).getChannel();
        final int lTailleFileChannel = (int) lFileChannel.size();
        final MappedByteBuffer lMappedByteBuffer = lFileChannel.map(FileChannel.MapMode.READ_ONLY, 0, lTailleFileChannel);
        final Charset lCharset = Charset.forName(java.util.ResourceBundle.getBundle("txt/txt").getString("MoteurLettre.xml.encodage"));
        final CharsetDecoder lCharsetDecoder = lCharset.newDecoder();
        final CharBuffer lCharBuffer = lCharsetDecoder.decode(lMappedByteBuffer);
        aListeContenu = new ArrayList<String>();
        final char[] lCharTab = new char[2048];
        String lBufStr;
        int i;
        int lIndiceDebutBalise = -1;
        char lCarLu;
        for (i = 0; lCharBuffer.hasRemaining(); i++) {
            lCarLu = lCharBuffer.get();
            if (i >= lCharTab.length) {
                lBufStr = String.valueOf(lCharTab, 0, ((lIndiceDebutBalise == -1) ? i : lIndiceDebutBalise));
                aListeContenu.add(lBufStr);
                if (lIndiceDebutBalise == -1) {
                    i = 0;
                } else {
                    for (int j = lIndiceDebutBalise, k = 0; j < lCharTab.length; j++, k++) {
                        lCharTab[k] = lCharTab[j];
                    }
                    i = i - lIndiceDebutBalise;
                    lIndiceDebutBalise = 0;
                }
            }
            lCharTab[i] = lCarLu;
            if (i > 2 && lCharTab[i - 3] == '&' && lCharTab[i - 2] == 'g' && lCharTab[i - 1] == 't' && lCharTab[i] == ';') {
                final String lBaliseInconnue = new String(lCharTab, lIndiceDebutBalise, i + 1 - lIndiceDebutBalise);
                if (aDonneesProgramme.getEnsembleDonnees().getEnsembleDonnees().get(0).getDonneesMap().get(lBaliseInconnue) != null) {
                    final String lContenuAvantBalise = new String(lCharTab, 0, lIndiceDebutBalise);
                    aListeContenu.add(lContenuAvantBalise);
                    aListeContenu.add(lBaliseInconnue);
                    lIndiceDebutBalise = -1;
                    i = -1;
                }
            } else if (i > 2 && lCharTab[i - 3] == '&' && lCharTab[i - 2] == 'l' && lCharTab[i - 1] == 't' && lCharTab[i] == ';') {
                lIndiceDebutBalise = i - 3;
            }
        }
        lBufStr = String.valueOf(lCharTab, 0, i);
        aListeContenu.add(lBufStr);
        lFileChannel.close();
    }
}
