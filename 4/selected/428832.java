package outils;

import gestionnaires.GestionnaireErreur;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.channels.FileChannel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import nat.ConfigNat;
import nat.Nat;
import outils.CharsetToolkit;

/**
 * Classe gérant différentes opérations de base sur les fichiers, comme la copie ou l'encodage
 * @author Fred et Bruno
 *
 */
public class FileToolKit {

    /** Enregistre une chaîne de caractères dans un
	 * fichier avec l'encodage donné. Si l'encodage est absent,
	 * choisit l'encodage de sortie de la configuration
	 * courant de NAT 
	 * @param stringToSave chaine à sauver
	 * @param fileName non du fichier de sauvegarde
	 * @param encodageFichier  encodage de filename
	 * @return true si succès
	 */
    public static boolean saveStrToFile(String stringToSave, String fileName, String encodageFichier) {
        String encodage;
        boolean retour = false;
        if (encodageFichier != null) {
            encodage = encodageFichier;
        } else {
            encodage = ConfigNat.getCurrentConfig().getBrailleEncoding();
        }
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), encodage));
            bw.write(stringToSave);
            bw.close();
            retour = true;
        } catch (IOException e) {
            System.err.println("erreur dans: " + e);
        }
        return retour;
    }

    /** Enregistre une chaîne de caractères dans un
	 * fichier avec l'encodage donné. Si l'encodage est absent,
	 * choisit l'encodage de sortie de la configuration
	 * courant de NAT 
	 * @param stringToSave chaine à sauvegarder
	 * @param fileName non du fichier
	 * @return true si succès 
	 */
    public static boolean saveStrToFile(String stringToSave, String fileName) {
        return saveStrToFile(stringToSave, fileName, ConfigNat.getCurrentConfig().getBrailleEncoding());
    }

    /** Lit le fichier en paramètre avec l'encoding donné
	 * et renvoie une String contenant ce fichier. Si l'encoding
	 * est omis, prend le charset par défaut du système 
	 * @param fileName adresse du fichier à lire
	 * @param encodageFichier encodage du fichier
	 * @return chaine contenant le contenu de fileName*/
    public static String loadFileToStr(String fileName, String encodageFichier) {
        String encodage;
        StringBuffer retour = new StringBuffer();
        if (encodageFichier != null) {
            encodage = encodageFichier;
        } else {
            encodage = CharsetToolkit.getDefaultSystemCharset().name();
        }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), encodage));
            String ligne = null;
            while ((ligne = br.readLine()) != null) {
                retour.append(ligne + "\n");
            }
            br.close();
        } catch (IOException e) {
            System.err.println("erreur dans: " + fileName + encodage + retour.toString() + "   " + e);
        }
        return retour.toString();
    }

    /** Lit le fichier en paramètre avec l'encoding par défaut
	 * et renvoie une String contenant ce fichier. 
	 * @param fileName adresse du fichier
	 * @return chaine contenant le contenu de fileName*/
    public static String loadFileToStr(String fileName) {
        return loadFileToStr(fileName, CharsetToolkit.getDefaultSystemCharset().name());
    }

    /** Copie un fichier vers un autre en changeant d'encodage 
	 * @param fileIn adresse du fichier source
	 * @param fileOut adresse du fichier sortie
	 * @param encodingIn encodage de fileIn
	 * @param encodingOut encodage de fileOut
	 * @return true si succès
	 */
    public static boolean copyFile(String fileIn, String fileOut, String encodingIn, String encodingOut) {
        boolean retour = false;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileIn), encodingIn));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), encodingOut));
            String ligne;
            while ((ligne = br.readLine()) != null) {
                bw.write(ligne + "\n");
            }
            br.close();
            bw.close();
            retour = true;
        } catch (FileNotFoundException fnfe) {
            System.err.println("Erreur copyFile fnfe");
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            System.err.println("Erreur copyFile ioe");
            ioe.printStackTrace();
        }
        return retour;
    }

    /** Copie un fichier vers un autre à l'identique (sans changer d'encodage) 
	 * @param fileIn adresse du fichier source
	 * @param fileOut adresse du fichier cible
	 * @return true si succès
	 */
    public static boolean copyFile(String fileIn, String fileOut) {
        FileChannel in = null;
        FileChannel out = null;
        boolean retour = false;
        try {
            in = new FileInputStream(fileIn).getChannel();
            out = new FileOutputStream(fileOut).getChannel();
            in.transferTo(0, in.size(), out);
            in.close();
            out.close();
            retour = true;
        } catch (IOException e) {
            System.err.println("File : " + fileIn);
            e.printStackTrace();
        }
        return retour;
    }

    /** change la table braille d'un fichier texte. Pour ce faire,
	 * ajoute les entetes XML au fichier source, copie les deux
	 * tables brailles entree et sortie dans ./xsl/tablesUsed,
	 * fait la transfo avec convert.xsl et recopie la sortie
	 * UTF-8 vers l'encoding de sortie voulu.
	 * @param fileIn fichier texte d'entrée
	 * @param fileOut fichier texte de sortie (brf)
	 * @param table1 table du fichier d'entrée
	 * @param table2 table pour le fichier de sortie
	 * @param encoding1 encoding du fichier d'entrée
	 * @param encoding2 encoding du fichier de sortie
	 * @param g gestionnaire d'erreur (peut être null)
	 * @return true si tout s'est bien passé
	 */
    public static boolean convertBrailleFile(String fileIn, String fileOut, String table1, String table2, String encoding1, String encoding2, GestionnaireErreur g) {
        Boolean retour = false;
        GestionnaireErreur gest = g;
        gest.afficheMessage("ok\n** Conversion de fichier", Nat.LOG_VERBEUX);
        if ((table1.equals(table2))) {
            retour = copyFile(fileIn, fileOut, encoding1, encoding2);
        } else {
            String fichConvTmpXml = ConfigNat.getUserTempFolder() + "tmpConv.xml";
            String fichConvTmpTxt = ConfigNat.getUserTempFolder() + "tmpConv.txt";
            String filtre = ConfigNat.getUserTempFolder() + "conv.xsl";
            String convTable1 = ConfigNat.getUserTempFolder() + "ConvtabIn.ent";
            String convTable2 = ConfigNat.getUserTempFolder() + "ConvtabOut.ent";
            retour = (ajouteEntete(loadFileToStr(fileIn, encoding1), fichConvTmpXml) && copyFile(table1, convTable1) && copyFile(table2, convTable2));
            if (retour) {
                String instruct;
                String importFunctx = "";
                String variables = "<xsl:variable name=\"ptBraille\" as=\"xs:string\">\n" + "<xsl:text>&pt;&pt1;&pt12;&pt123;&pt1234;&pt12345;&pt123456;&pt12346;&pt1235;&pt12356;&pt1236;&pt124;&pt1245;&pt12456;&pt1246;&pt125;&pt1256;&pt126;&pt13;&pt134;&pt1345;&pt13456;&pt1346;&pt135;&pt1356;&pt136;&pt14;&pt145;&pt1456;&pt146;&pt15;&pt156;&pt16;&pt2;&pt23;&pt234;&pt2345;&pt23456;&pt2346;&pt235;&pt2356;&pt236;&pt24;&pt245;&pt2456;&pt246;&pt25;&pt256;&pt26;&pt3;&pt34;&pt345;&pt3456;&pt346;&pt35;&pt356;&pt36;&pt4;&pt45;&pt456;&pt46;&pt5;&pt56;&pt6;</xsl:text>\n" + "</xsl:variable>\n" + "<xsl:variable name=\"ptEmbos\" as=\"xs:string\">\n" + "<xsl:text>&pte;&pte1;&pte12;&pte123;&pte1234;&pte12345;&pte123456;&pte12346;&pte1235;&pte12356;&pte1236;&pte124;&pte1245;&pte12456;&pte1246;&pte125;&pte1256;&pte126;&pte13;&pte134;&pte1345;&pte13456;&pte1346;&pte135;&pte1356;&pte136;&pte14;&pte145;&pte1456;&pte146;&pte15;&pte156;&pte16;&pte2;&pte23;&pte234;&pte2345;&pte23456;&pte2346;&pte235;&pte2356;&pte236;&pte24;&pte245;&pte2456;&pte246;&pte25;&pte256;&pte26;&pte3;&pte34;&pte345;&pte3456;&pte346;&pte35;&pte356;&pte36;&pte4;&pte45;&pte456;&pte46;&pte5;&pte56;&pte6;</xsl:text>\n" + "</xsl:variable>\n";
                if ((!ConfigNat.getCurrentConfig().getMep()) && ConfigNat.getCurrentConfig().getChaineIn().length() > 0) {
                    ConfigNat.getCurrentConfig();
                    importFunctx = "<xsl:import href=\"" + ConfigNat.getInstallFolder() + "xsl/functions/functx-1.1alpha.xsl\" />\n";
                    variables += "<xsl:variable name=\"chaineEntree1\" select=\"translate('" + ConfigNat.getCurrentConfig().getChaineIn() + "',$ptEmbos,$ptBraille)\"/>\n" + "<xsl:variable name=\"chaineSortie\" select=\"translate('" + ConfigNat.getCurrentConfig().getChaineOut() + "',$ptEmbos,$ptBraille)\"/>\n" + "<!-- il faut virer les chars qui sont pas dans la table braille sinon le replace remplace aussi les chars des rajouts (qui sont pas brailleutf8) -->\n" + "<xsl:variable name=\"chaineEntree\" select=\"translate($chaineEntree1,'" + ConfigNat.getCurrentConfig().getChaineIn() + "','')\"/>\n";
                    instruct = "<xsl:choose>\n  <xsl:when test=\"$chaineEntree\">\n" + "   <xsl:value-of select=\"translate(replace(string(.), functx:escape-for-regex($chaineEntree), functx:escape-for-regex2($chaineSortie)),$ptBraille,$ptEmbos)\"/>\n" + "  </xsl:when>\n  <xsl:otherwise>\n    <xsl:value-of select=\"translate(string(.),$ptBraille,$ptEmbos)\"/>\n  </xsl:otherwise>\n</xsl:choose>\n";
                } else if (!ConfigNat.getCurrentConfig().getSaxonAsXsltProcessor()) {
                    System.out.println("Je suis dans le wrapper");
                    instruct = "<xsl:value-of select=\"javaNat:translate(string(.),$ptBraille,$ptEmbos)\"/>\n";
                } else {
                    instruct = "<xsl:value-of select=\"translate(string(.),$ptBraille,$ptEmbos)\"/>\n";
                }
                saveStrToFile("<?xml version='1.1' encoding=\"UTF-8\" ?>\n" + "<!DOCTYPE xsl:stylesheet SYSTEM \"" + ConfigNat.getCurrentConfig().getDTD() + "\"\n[\n<!ENTITY % table_braille PUBLIC \"table braille entree\" \"file://" + convTable1 + "\">\n%table_braille;\n" + "<!ENTITY % table_conversion PUBLIC \"table braille sortie\" \"file://" + convTable2 + "\">\n%table_conversion;\n]>\n" + "<xsl:stylesheet version=\"2.0\"\n" + "xmlns:javaNat=\"java:nat.saxFuncts.SaxFuncts\"\n" + "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'\n" + "xmlns:xs='http://www.w3.org/2001/XMLSchema'\n" + "xmlns:functx='http://www.functx.com'>\n" + importFunctx + "<xsl:output method=\"text\" encoding=\"UTF-8\" indent=\"no\"/>\n" + variables + "<xsl:template match=\"/\">\n" + instruct + "</xsl:template>\n" + "</xsl:stylesheet>", filtre, "UTF-8");
                gest.afficheMessage("ok\n*** Création de la fabrique (DocumentBuilderFactory) ...", Nat.LOG_VERBEUX);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setValidating(ConfigNat.getCurrentConfig().getNiveauLog() == Nat.LOG_DEBUG);
                factory.setIgnoringComments(true);
                factory.setIgnoringElementContentWhitespace(false);
                try {
                    factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    builder.setErrorHandler(gest);
                    gest.afficheMessage("ok\n*** Parsage du document texte d'entrée avec SAX ...", Nat.LOG_VERBEUX);
                    Document doc = builder.parse(new File(fichConvTmpXml));
                    doc.setStrictErrorChecking(true);
                    gest.afficheMessage("ok\n*** Initialisation et lecture de la feuille de style de conversion...", Nat.LOG_VERBEUX);
                    TransformerFactory transformFactory = TransformerFactory.newInstance();
                    StreamSource styleSource = new StreamSource(new File(filtre));
                    Transformer transform = transformFactory.newTransformer(styleSource);
                    DOMSource in = new DOMSource(doc);
                    gest.afficheMessage("ok\n*** Création du fichier convertit ...", Nat.LOG_VERBEUX);
                    File file = new File(fichConvTmpTxt);
                    StreamResult out = new StreamResult(file);
                    gest.afficheMessage("ok\n*** Conversion du fichier d'entrée...", Nat.LOG_VERBEUX);
                    transform.transform(in, out);
                    retour = copyFile(fichConvTmpTxt, fileOut, "UTF-8", encoding2);
                } catch (Exception e) {
                    gest.setException(e);
                    gest.gestionErreur();
                    e.printStackTrace();
                    retour = false;
                }
            }
            if (!retour) {
                gest.afficheMessage("Erreur ConvertBrailleFile", Nat.LOG_SILENCIEUX);
            }
        }
        return retour;
    }

    /** appelle l'autre convertBrailleFile avec l'encoding de sortie
	 * de NAT pour les deux encoding entrée et sortie 
	 * @param fileIn fichier texte d'entrée
	 * @param fileOut fichier texte de sortie (brf)
	 * @param table1 table du fichier d'entrée
	 * @param table2 table pour le fichier de sortie
	 * @param g gestionnaire d'erreur (peut être null)
	 * @return true si tout s'est bien passé
	 */
    public static boolean convertBrailleFile(String fileIn, String fileOut, String table1, String table2, GestionnaireErreur g) {
        String encoding = ConfigNat.getCurrentConfig().getBrailleEncoding();
        return convertBrailleFile(fileIn, fileOut, table1, table2, encoding, encoding, g);
    }

    /** méthode privée ajoute les entetes XML a une string donnée et l'enregistre ds un fichier
	 * @param stringToSave string du contenu texte
	 * @param fileOut fichier xml de sortie
	 * @return true si ça s'est bien passé
	 */
    private static boolean ajouteEntete(String stringToSave, String fileOut) {
        boolean retour = false;
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileOut), "UTF-8"));
            bw.write("<?xml version=\"1.1\" encoding=\"UTF-8\"?><textconv>");
            bw.write(stringToSave.replaceAll(Character.toString((char) 12), "&#xC;").replaceAll("<", "&lt;"));
            bw.write("</textconv>");
            bw.close();
            retour = true;
        } catch (IOException e) {
            System.err.println("erreur d'ajout d'entete dans: " + e);
        }
        return retour;
    }

    /**
	 * Renvoie un nom complet de fichier ou de répertoire selon le système :
	 * .toURI().getPath() pour tous les Windows sauf Vista
	 * .toURI().getRawPath() pour les autres systèmes d'exploitation
	 * @param path string du nom du fichier ou du répertoire à convertir
	 * @return le chemin complet converti
	 */
    public static String getSysDepPath(String path) {
        String retour;
        URI ucf = new File(path).toURI();
        if (SystemUtils.IS_OS_WINDOWS) {
            retour = ucf.getPath();
        } else {
            retour = ucf.getRawPath();
        }
        return retour;
    }

    /**
	 * Renvoie le nom de sortie automatique de document :
	 * extension d'origine avec - et _nat.ext à la fin
	 * exemple : test.odt, txt -> test-odt_nat.txt
	 * @param nomEntree nom de fichier à convertir
	 * @param ext extension à ajouter
	 * @return nom de sortie automatique
	 */
    public static String nomSortieAuto(String nomEntree, String ext) {
        String nom = nomEntree.replaceAll("[.][^.]+$", "");
        String extension = nomEntree.substring(nom.length()).replace(".", "-");
        return (nom + extension + "_nat." + ext);
    }
}
