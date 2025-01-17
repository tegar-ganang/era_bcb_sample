package net.sf.entDownloader.core;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Gestionnaire de mise à jour. Obtient les informations de mise à jour à partir
 * du site Internet du logiciel.
 * 
 * @since ENTDownloader 1.0.1
 */
public class Updater {

    private boolean available;

    private String version;

    private Document xmlUpdateInformation;

    private String location;

    private ArrayList<String> added;

    private ArrayList<String> changed;

    private ArrayList<String> fixed;

    private ArrayList<String> other;

    private Calendar datePub;

    /**
	 * Construit une nouvelle instance d'Updater et charge le fichier de version
	 * par défaut.
	 * 
	 * @throws Exception URL incorrecte, format de fichier invalide ...
	 */
    public Updater() throws Exception {
        this(CoreConfig.updaterURL);
    }

    /**
	 * Construit une nouvelle instance d'Updater et charge le fichier de version
	 * indiqué en argument.
	 * 
	 * @param updateURL L'URL du fichier XML contenant les informations de
	 *            version.
	 * @throws Exception URL incorrecte, format de fichier invalide ...
	 */
    public Updater(String updateURL) throws Exception {
        available = false;
        version = null;
        location = null;
        InputStream stream = null;
        try {
            DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
            DocumentBuilder constructeur = fabrique.newDocumentBuilder();
            constructeur.setErrorHandler(new ErrorHandler() {

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                }
            });
            URL url = new URL(updateURL.replaceAll("\\{version\\}", java.net.URLEncoder.encode(CoreConfig.getString("ProductInfo.version"), "UTF-8")));
            stream = url.openConnection(ENTDownloader.getInstance().getProxy()).getInputStream();
            xmlUpdateInformation = constructeur.parse(stream);
            Element racine = xmlUpdateInformation.getDocumentElement();
            NodeList liste = racine.getElementsByTagName("NeedToBeUpdated");
            if (liste.getLength() != 0) {
                Element e = (Element) liste.item(0);
                if (e.getTextContent().equals("yes")) {
                    available = true;
                }
            }
        } catch (Exception e) {
            try {
                stream.close();
            } catch (Exception e1) {
            }
            throw e;
        }
    }

    /**
	 * Détermine si le programme est à jour ou non.
	 * 
	 * @return True si le logiciel est à jour, false sinon.
	 */
    public boolean isUpToDate() {
        return !available;
    }

    /**
	 * Retourne le numéro de la nouvelle version, ou null si aucune mise à jour
	 * n'est disponible.
	 */
    public String version() {
        if (!available) return null;
        if (version == null) {
            version = getElementTextContentByTagName("Version");
            if (version == null) {
                version = "";
            }
        }
        return version;
    }

    /**
	 * Retourne l'adresse de téléchargement de la mise à jour, ou null si le
	 * programme est à jour.
	 */
    public String location() {
        if (!available) return null;
        if (location == null) {
            location = getElementTextContentByTagName("Location");
            if (location == null) {
                location = "";
            }
        }
        return location;
    }

    /**
	 * Retourne la date de publication de la nouvelle version, ou null si aucune
	 * mise à jour n'est disponible.
	 */
    public Calendar datePublication() {
        if (!available) return null;
        if (datePub == null) {
            long timestamp = Long.parseLong(getElementTextContentByTagName("Date"));
            datePub = Calendar.getInstance();
            datePub.setTimeInMillis(timestamp * 1000);
        }
        return datePub;
    }

    /**
	 * Retourne les nouveautés de la mise à jour, ou null si le programme est à
	 * jour.
	 */
    public ArrayList<String> changelog_added() {
        if (!available) return null;
        if (added == null) {
            added = getElementsTextContentByTagName("Add");
        }
        return added;
    }

    /**
	 * Retourne les fonctionnalités modifiées dans la nouvelle version, ou null
	 * si aucune mise à jour n'est disponible.
	 */
    public ArrayList<String> changelog_changed() {
        if (!available) return null;
        if (changed == null) {
            changed = getElementsTextContentByTagName("Change");
        }
        return changed;
    }

    /**
	 * Retourne les bogues corrigés par la mise à jour, ou null si le programme
	 * est à jour.
	 */
    public ArrayList<String> changelog_fixed() {
        if (!available) return null;
        if (fixed == null) {
            fixed = getElementsTextContentByTagName("Fix");
        }
        return fixed;
    }

    /**
	 * Retourne les autres changements de la nouvelle version, ou null si aucune
	 * mise à jour n'est disponible.
	 */
    public ArrayList<String> changelog_other() {
        if (!available) return null;
        if (other == null) {
            other = getElementsTextContentByTagName("Other");
        }
        return other;
    }

    /**
	 * Retourne le contenu textuel du premier noeud portant le nom indiqué en
	 * paramètre, ou null si aucun noeud correspondant n'est trouvé.
	 * 
	 * @param tagName Le nom du noeud désiré.
	 * @throws DOMException
	 */
    private String getElementTextContentByTagName(String tagName) throws DOMException {
        Element racine = xmlUpdateInformation.getDocumentElement();
        NodeList liste = racine.getElementsByTagName(tagName);
        if (liste.getLength() != 0) {
            Element e = (Element) liste.item(0);
            return e.getTextContent();
        } else return null;
    }

    /**
	 * Retourne les contenus textuels des noeuds portant le nom indiqué en
	 * paramètre, ou null si aucun noeud correspondant n'est trouvé.
	 * 
	 * @param tagName Le nom du noeud désiré.
	 * @throws DOMException
	 */
    private ArrayList<String> getElementsTextContentByTagName(String tagName) throws DOMException {
        Element racine = xmlUpdateInformation.getDocumentElement();
        NodeList liste = racine.getElementsByTagName(tagName);
        if (liste.getLength() != 0) {
            ArrayList<String> list = new ArrayList<String>();
            int nbElements = liste.getLength();
            for (int i = 0; i < nbElements; ++i) {
                Element e = (Element) liste.item(i);
                list.add(e.getTextContent());
            }
            return list;
        } else return null;
    }
}
