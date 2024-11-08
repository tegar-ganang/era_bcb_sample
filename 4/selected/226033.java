package fr.insa_rennes.pcreator.actions;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.FileEditorInput;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import fr.insa_rennes.pcreator.Application;
import fr.insa_rennes.pcreator.PCreatorConstantes;
import fr.insa_rennes.pcreator.editiongraphique.ChaineGraphicalEditor;
import fr.insa_rennes.pcreator.editiongraphique.model.Archi;
import fr.insa_rennes.pcreator.editiongraphique.model.Architecture;
import fr.insa_rennes.pcreator.editiongraphique.model.BriqueDeBase;
import fr.insa_rennes.pcreator.editiongraphique.model.Chaine;
import fr.insa_rennes.pcreator.editiongraphique.model.ElementComposite;
import fr.insa_rennes.pcreator.editiongraphique.model.Entree;
import fr.insa_rennes.pcreator.editiongraphique.model.EntreeChaine;
import fr.insa_rennes.pcreator.editiongraphique.model.Erreur;
import fr.insa_rennes.pcreator.editiongraphique.model.ErreurFichier;
import fr.insa_rennes.pcreator.editiongraphique.model.ErreurNumero;
import fr.insa_rennes.pcreator.editiongraphique.model.FileDAttente;
import fr.insa_rennes.pcreator.editiongraphique.model.InformationsService;
import fr.insa_rennes.pcreator.editiongraphique.model.Os;
import fr.insa_rennes.pcreator.editiongraphique.model.Pause;
import fr.insa_rennes.pcreator.editiongraphique.model.Service;
import fr.insa_rennes.pcreator.editiongraphique.model.Sortie;
import fr.insa_rennes.pcreator.editiongraphique.model.SousChaine;
import fr.insa_rennes.pcreator.editiongraphique.model.Tube;

/**
 * Cette classe représente l'action permettant d'exporter une chaîne au format XML
 * Cela est fait en plusieurs parties :
 * - d'abord on récupère la chaîne à exporter
 * - on obtient ensuite la description des services utilisés
 * - on exporte ensuite la structure de la chaîne
 * - on passe enfin à l'export à proprement parler
 * 		- on exporte d'abord dans un fichier
 * 		- on valide ensuite ce fichier par rapport à la DTD
 * 		- si c'est ok on envoie à PExecutor (création d'un autre thread indépendant)
 * @author romain
 *
 */
public class ExportChainToXMLAction implements IWorkbenchWindowActionDelegate {

    /**
	 * L'id de l'action
	 */
    public static final String ID = "fr.insa-rennes.pcreator.actions.ExportChainToXMLAction";

    /**
	 * Le workbench dans lequel l'action a été créé (et dans lequel elle sera appellée).
	 */
    private IWorkbench workbench;

    /**
	 * Le document dans lequel on crée notre structure XML,
	 * qui sera ensuite écrite dans un fichier
	 */
    private Document dom;

    /**
	 * On mémorise la chaine, ça peut servir
	 */
    private Chaine chaine;

    /**
	 * Sert à savoir si une erreur a été rencontrée durant l'export.
	 * Si c'est le cas, la chaîne ne sera pas envoyée à PExecutor.
	 */
    private boolean hasErrors;

    public void dispose() {
    }

    public void init(IWorkbenchWindow window) {
        workbench = window.getWorkbench();
        hasErrors = false;
    }

    public void run(IAction action) {
        hasErrors = false;
        IEditorPart activeEditor = workbench.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (activeEditor instanceof ChaineGraphicalEditor) {
            chaine = ((ChaineGraphicalEditor) activeEditor).getChaine();
            exporterFichierChaine(chaine);
        }
    }

    /**
	 * Exporte la chaîne passée en paramètre en XML
	 * @param chaine la chaîne à exporter
	 */
    private void exporterFichierChaine(Chaine chaine) {
        creerDocument();
        Element racineElt = dom.createElement("fichier_chaine");
        dom.appendChild(racineElt);
        Application.logger.debug("Export des services…");
        Node servicesElt = exporterServices(chaine);
        racineElt.appendChild(servicesElt);
        Application.logger.debug("Export du graphe…");
        Node chaineElt = exporterChaine(chaine);
        racineElt.appendChild(chaineElt);
        Application.logger.debug("Génération du fichier XML…");
        genererFichierXML();
    }

    /**
	 * Crée l'instance de Document qui contiendra nos données
	 * Provient de http://www.totheriver.com/learn/xml/xmltutorial.html
	 */
    private void creerDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            dom = db.newDocument();
        } catch (ParserConfigurationException pce) {
            Application.logger.error("C'est mort pour l'export, erreur en essayant d'instancier le DocumentBuilder " + pce);
        }
    }

    /**
	 * S'occupe d'exporter la description des services utilisés par la chaine
	 * @param chaine la chaîne à parcourir
	 * @return un élément contenant toutes les informations nécessaires à l'exploitation des services
	 */
    private Node exporterServices(Chaine chaine) {
        List<fr.insa_rennes.pcreator.editiongraphique.model.Element> listeChaine = chaine.getChildrenArray();
        HashMap<String, Element> services = new HashMap<String, Element>();
        recupererInfosServices(services, listeChaine);
        Element servicesElt = dom.createElement("services");
        Collection<Element> servicesEltCollection = services.values();
        for (Element serviceElt : servicesEltCollection) {
            servicesElt.appendChild(serviceElt);
        }
        return servicesElt;
    }

    /**
	 * Parcourt une liste de sous-chaînes, repére tous les services et extrait leurs informations
	 * pour export en XML
	 * @param services la hashmap dans laquelle seront stockés les Element (DOM) représentant les services
	 * @param listeChaine la liste de sous-chaîne dans laquelle on recherche les services.
	 */
    private void recupererInfosServices(HashMap<String, Element> services, List<fr.insa_rennes.pcreator.editiongraphique.model.Element> listeChaine) {
        for (fr.insa_rennes.pcreator.editiongraphique.model.Element sousChaine : listeChaine) {
            if (sousChaine instanceof Service) {
                Service service = (Service) sousChaine;
                String idService = service.getInfos().getId_service();
                if (!services.containsKey(idService)) {
                    Element serviceElt = creerServiceElt(service);
                    services.put(idService, serviceElt);
                }
            } else if (sousChaine instanceof ElementComposite) {
                List<fr.insa_rennes.pcreator.editiongraphique.model.Element> listeSousChaine = ((ElementComposite) sousChaine).getChildrenArray();
                recupererInfosServices(services, listeSousChaine);
            }
        }
    }

    /**
	 * Crée l'élément (pour le DOM) correspondant à un service donné
	 * @param service le service dont on veut obtenir les informations
	 * @return un élément contenant toutes les informations sur le service
	 */
    private Element creerServiceElt(Service service) {
        Element serviceElt = dom.createElement("service");
        InformationsService serviceInfos = service.getInfos();
        String idService = service.getInfos().getId_service();
        serviceElt.setAttribute("id_service", idService);
        serviceElt.setIdAttribute("id_service", true);
        serviceElt.setAttribute("nom", serviceInfos.getNom());
        serviceElt.setAttribute("desc", serviceInfos.getDescription());
        serviceElt.setAttribute("version", serviceInfos.getVersion());
        serviceElt.setAttribute("systeme", booleanToString(serviceInfos.getSysteme()));
        serviceElt.setAttribute("emplacement", serviceInfos.getEmplacement());
        Element entreesElt = creerEntreesElt(service);
        serviceElt.appendChild(entreesElt);
        Element sortiesElt = creerSortiesElt(service);
        serviceElt.appendChild(sortiesElt);
        Element erreursElt = creerErreursElt(service);
        serviceElt.appendChild(erreursElt);
        Element filesElt = creerFilesElt(service);
        serviceElt.appendChild(filesElt);
        Element archisElt = creerArchisElt(service);
        serviceElt.appendChild(archisElt);
        return serviceElt;
    }

    /**
	 * Renvoie l'élément décrivant toutes les entrées du service
	 * @param service le service à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerEntreesElt(Service service) {
        Element entreesElt = dom.createElement("entrees");
        List<fr.insa_rennes.pcreator.editiongraphique.model.Element> entrees = service.getBlocEntrees().getChildrenArray();
        for (fr.insa_rennes.pcreator.editiongraphique.model.Element entree : entrees) {
            if (entree instanceof Entree) {
                Element entreeElt = creerEntreeElt((Entree) entree);
                entreesElt.appendChild(entreeElt);
            }
        }
        return entreesElt;
    }

    /**
	 * Renvoie l'élément décrivant une entrée de service (pas de traitement)
	 * @param entree l'entrée à décrire
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerEntreeElt(Entree entree) {
        Element entreeElt = dom.createElement("entree");
        entreeElt.setAttribute("id_entree", entree.getIdEntree());
        entreeElt.setAttribute("type", entree.getType());
        entreeElt.setAttribute("desc", entree.getDescription());
        entreeElt.setAttribute("modifiee", booleanToString(entree.getModifie()));
        entreeElt.setAttribute("constante", booleanToString(entree.getConstante()));
        entreeElt.setAttribute("val_par_defaut", entree.getValeur_par_defaut());
        entreeElt.setAttribute("nom_option", entree.getNom_option());
        entreeElt.setAttribute("type_appel", PCreatorConstantes.TYPES_APPEL_STRINGS[entree.getType_appel()]);
        return entreeElt;
    }

    /**
	 * Renvoie l'élément décrivant toutes les sorties d'un service
	 * @param service le service à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerSortiesElt(Service service) {
        Element sortiesElt = dom.createElement("sorties");
        List<fr.insa_rennes.pcreator.editiongraphique.model.Element> sorties = service.getBlocSorties().getChildrenArray();
        for (fr.insa_rennes.pcreator.editiongraphique.model.Element sortie : sorties) {
            if (sortie instanceof Sortie) {
                Element sortieElt = creerSortieElt((Sortie) sortie);
                sortiesElt.appendChild(sortieElt);
            }
        }
        return sortiesElt;
    }

    /**
	 * Renvoie l'élément décrivant une sortie de service
	 * @param sortie la sortie à décrire
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerSortieElt(Sortie sortie) {
        Element sortieElt = dom.createElement("sortie");
        sortieElt.setAttribute("id_sortie", sortie.getId_sortie());
        sortieElt.setAttribute("type", sortie.getType());
        sortieElt.setAttribute("desc", sortie.getDescription());
        sortieElt.setAttribute("id_entree_associee", sortie.getId_entree_associee());
        sortieElt.setAttribute("type_prod", PCreatorConstantes.TYPES_SORTIE_STRING[sortie.getType_prod()]);
        sortieElt.setAttribute("nom_fichier", sortie.getNom_fichier());
        return sortieElt;
    }

    /**
	 * Renvoie l'élément décrivant toutes les erreurs que peut générer un service
	 * @param service le service à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerErreursElt(Service service) {
        Element erreursElt = dom.createElement("erreurs");
        List<Erreur> erreurs = service.getErreursList();
        for (Erreur erreur : erreurs) {
            Element erreurElt = creerErreurElt(erreur);
            erreursElt.appendChild(erreurElt);
        }
        return erreursElt;
    }

    /**
	 * Renvoie l'élément décrivant une erreur qui peut étre renvoyée par un service
	 * @param erreur l'erreur que le service pourrait générer
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerErreurElt(Erreur erreur) {
        if (erreur instanceof ErreurNumero) {
            ErreurNumero err = (ErreurNumero) erreur;
            Element erreurElt = dom.createElement("erreur_num");
            erreurElt.setAttribute("val", new Integer(err.getNumero()).toString());
            erreurElt.setAttribute("type", PCreatorConstantes.TYPES_ERREUR_STRING[err.getType()]);
            erreurElt.setAttribute("desc", err.getDescription());
            return erreurElt;
        } else if (erreur instanceof ErreurFichier) {
            ErreurFichier err = (ErreurFichier) erreur;
            Element erreurElt = dom.createElement("erreur_fichier");
            erreurElt.setAttribute("ext", err.getExtension());
            erreurElt.setAttribute("type", PCreatorConstantes.TYPES_ERREUR_STRING[err.getType()]);
            erreurElt.setAttribute("desc", err.getDescription());
            return erreurElt;
        }
        return null;
    }

    /**
	 * Renvoie l'élément décrivant toutes les architectures sur lesquels peut s'exécuter un service
	 * @param service le service à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerFilesElt(Service service) {
        Element filesElt = dom.createElement("files_attente");
        for (FileDAttente file : service.getFilesList()) {
            Element fileElt = dom.createElement("file_attente");
            fileElt.setAttribute("nom_cluster", file.getNom_cluster());
            fileElt.setAttribute("nom_file_attente", file.getNom_file());
            filesElt.appendChild(fileElt);
        }
        return filesElt;
    }

    /**
	 * Renvoie l'élément décrivant toutes les architectures sur lesquels peut s'exécuter un service
	 * @param service le service à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerArchisElt(Service service) {
        Element archisElt = dom.createElement("architectures");
        for (Architecture archi : service.getArchitectureList()) {
            Element archiElt = creerArchiElt(archi);
            archisElt.appendChild(archiElt);
        }
        return archisElt;
    }

    /**
	 * Renvoie l'élément décrivant une architecture sur laquelle peut tourner le service,
	 * soit une architecture (exemple : "64bits") soit un OS (le meilleur exemple : "Mac OS").
	 * @param architecture l'architecture à décrire
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerArchiElt(Architecture architecture) {
        if (architecture instanceof Archi) {
            Archi archi = (Archi) architecture;
            Element archiElt = dom.createElement("architecture");
            archiElt.setAttribute("type", archi.getType());
            return archiElt;
        } else if (architecture instanceof Os) {
            Os os = (Os) architecture;
            Element OsElt = dom.createElement("os");
            OsElt.setAttribute("nom", os.getNom());
            return OsElt;
        }
        return null;
    }

    /**
	 * Renvoie la chaîne de caractéres correspondant à un booleen
	 * @param b le booleen à renvoyer
	 * @return "vrai" si vrai, "faux" si faux
	 */
    private String booleanToString(boolean b) {
        if (b) {
            return new String("vrai");
        } else {
            return new String("faux");
        }
    }

    /**
	 * S'occupe de d'exporter la chaîne en elle-méme, sans se soucier des services
	 * @param chaine la chaîne à exporter
	 * @return la chaîne structurée pour le format XML
	 */
    private Node exporterChaine(Chaine chaine) {
        Element chaineElt = dom.createElement("chaine");
        chaineElt.setAttribute("id", chaine.getIdChaine());
        chaineElt.setAttribute("nom", chaine.getNom());
        chaineElt.setAttribute("desc", chaine.getDescription());
        chaineElt.setAttribute("version", chaine.getVersion());
        ArrayList<EntreeChaine> entreeChaines = chaine.getEntreesChaineList();
        for (EntreeChaine entreeChaine : entreeChaines) {
            Element entreeChaineElt = creerEntreeChaineElt(entreeChaine);
            chaineElt.appendChild(entreeChaineElt);
        }
        Element grapheElt = dom.createElement("graphe");
        for (fr.insa_rennes.pcreator.editiongraphique.model.Element sousChaine : chaine.getChildrenArray()) {
            Element elt = creerGrapheElement(sousChaine);
            if (elt != null) {
                grapheElt.appendChild(elt);
            }
        }
        for (Service service : chaine.getServices()) {
            for (Tube tube : service.getTubesSortants()) {
                for (BriqueDeBase brique : tube.getBriquesDeBaseList()) {
                    Element elt = creerGrapheElement(brique);
                    if (elt != null) {
                        grapheElt.appendChild(elt);
                    }
                }
            }
        }
        chaineElt.appendChild(grapheElt);
        return chaineElt;
    }

    /**
	 * Renvoie l'élément décrivant une entrée de chaine
	 * @param entreeChaine l'entrée de chaine à décrire
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerEntreeChaineElt(EntreeChaine entreeChaine) {
        Element entreeChaineElt = dom.createElement("entree_chaine");
        entreeChaineElt.setAttribute("id_traitement", "Traitement" + new Integer(entreeChaine.getIdTraitement()).toString());
        entreeChaineElt.setAttribute("id_entree", entreeChaine.getIdEntree());
        entreeChaineElt.setAttribute("desc", entreeChaine.getDescription());
        return entreeChaineElt;
    }

    /**
	 * Créée l'élément décrivant le graphe ou une partie du graphe (appelée à tous les niveau)
	 * @param element la partie de la chaîne à analyser
	 * @return toute la chaîne, préte à étre exportée en XML
	 */
    private Element creerGrapheElement(fr.insa_rennes.pcreator.editiongraphique.model.Element element) {
        Element elt = null;
        if (element instanceof Service) {
            elt = creerTraitementElement((Service) element);
        } else if (element instanceof BriqueDeBase) {
            elt = creerBriqueElement((BriqueDeBase) element);
        } else if (element instanceof SousChaine) {
            elt = creerSousChaineElement((SousChaine) element);
        }
        return elt;
    }

    /**
	 * Crée l'élément décrivant une sous-chaine
	 * @param sousChaine la sous-chaîne à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerSousChaineElement(SousChaine sousChaine) {
        if (sousChaine.getNiveau() < 0) {
            return null;
        }
        Element noeudElt = dom.createElement("sous_chaine");
        noeudElt.setAttribute("niveau", new Integer(sousChaine.getNiveau()).toString());
        for (fr.insa_rennes.pcreator.editiongraphique.model.Element element : sousChaine.getChildrenArray()) {
            Element elt = creerGrapheElement(element);
            if (elt != null) {
                noeudElt.appendChild(elt);
            }
        }
        return noeudElt;
    }

    /**
	 * Crée l'élément décrivant un traitement
	 * @param traitement le traitement à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerTraitementElement(Service traitement) {
        Element noeudElt = dom.createElement("traitement");
        noeudElt.setAttribute("id_traitement", "Traitement" + new Integer(traitement.getInfos().getId_traitement()).toString());
        noeudElt.setAttribute("ordre", new Integer(traitement.getInfos().getOrdre()).toString());
        noeudElt.setAttribute("id_service", traitement.getInfos().getId_service());
        for (fr.insa_rennes.pcreator.editiongraphique.model.Element element : traitement.getBlocEntrees().getChildrenArray()) {
            if (element instanceof Entree) {
                Entree entree = (Entree) element;
                if (entree.getTube() == null) {
                    String val = entree.getValeur();
                    Element entreeElt = dom.createElement("entree_specifiee");
                    entreeElt.setAttribute("id_entree", entree.getIdEntree());
                    entreeElt.setAttribute("val", val);
                    if (!entree.getEstEntreeChaine() && (val == null || val.length() < 1)) {
                        Application.logger.warn("L'entrée " + entree.getIdEntree() + " du service " + entree.getServiceParent().getNom() + " n'a pas de valeur spécifiée.\n" + "       Seules les valeurs des entrées de chaîne peuvent être laissées vides.");
                        hasErrors = true;
                    }
                    noeudElt.appendChild(entreeElt);
                } else {
                    Element entreeElt = dom.createElement("entree_arc");
                    entreeElt.setAttribute("id_entree", entree.getIdEntree());
                    Sortie sortieAvant = entree.getTube().getSortie();
                    entreeElt.setAttribute("id_sortie", sortieAvant.getId_sortie());
                    String idTraitement = new String();
                    fr.insa_rennes.pcreator.editiongraphique.model.Element servicePere = sortieAvant.getParent();
                    if (servicePere != null && servicePere instanceof Service) {
                        idTraitement = new Integer(((Service) servicePere).getInfos().getId_traitement()).toString();
                    }
                    entreeElt.setAttribute("id_traitement_producteur", "Traitement" + idTraitement);
                    noeudElt.appendChild(entreeElt);
                }
            }
        }
        return noeudElt;
    }

    /**
	 * Crée l'élément décrivant une brique de base
	 * @param brique la brique de base à analyser
	 * @return un Element que l'on peut ajouter à notre Document
	 */
    private Element creerBriqueElement(BriqueDeBase brique) {
        Element noeudElt = dom.createElement("brique");
        String type = "";
        if (brique instanceof Pause) {
            type = "pause";
        } else {
            Application.logger.error("Une brique de base après le niveau " + brique.getNiveauAvant() + " n'a pu être identifiée, il est fort probable que l'export échoue. Type rencontré : " + brique.getClass().getName());
        }
        noeudElt.setAttribute("type", type);
        noeudElt.setAttribute("niveau_avant", new Integer(brique.getNiveauAvant()).toString());
        return noeudElt;
    }

    /**
	 * Génére le fichier XML correspondant au modéle de document courant
	 */
    private void genererFichierXML() {
        try {
            OutputFormat format = new OutputFormat(dom);
            format.setIndenting(true);
            format.setDoctype(null, "pelias_chaine.dtd");
            XMLSerializer serializer;
            IEditorInput input = workbench.getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorInput();
            String path = PCreatorConstantes.CHAINE_FICHIER_DEFAUT;
            if (input instanceof FileEditorInput) {
                path = ((FileEditorInput) input).getFile().getLocation().toOSString();
                int extIndex = path.lastIndexOf(".pcc");
                path = path.substring(0, extIndex).concat(".xml");
            }
            File fichierXML = new File(path);
            serializer = new XMLSerializer(new FileOutputStream(fichierXML), format);
            serializer.serialize(dom);
            Application.refreshWorkspace(null);
            if (validerXMLChaine(fichierXML.getAbsolutePath()) && !hasErrors) {
                SendToPCreatorThread threadEnvoi = new SendToPCreatorThread(dom, format);
                threadEnvoi.start();
            } else {
                Application.logger.error("Le fichier exporté n'a pu être validé, il ne sera pas envoyé à PExecutor.\n       Le fichier a été enregistré à l'emplacement " + fichierXML.getAbsolutePath());
            }
        } catch (IOException ie) {
            Application.logger.error("Erreur lors de l'export en XML : " + ie.getLocalizedMessage());
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
    }

    /**
	 * Fonction qui vérifie que le fichier XML généré est bien conforme à la DTD
	 * @param path l'emplacement du fichier XML
	 * @return vrai si le fichier est bien formé et conforme, faux sinon 
	 */
    private boolean validerXMLChaine(String path) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(true);
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            VerboseDTDErrorHandler errorHandler = new VerboseDTDErrorHandler();
            db.setErrorHandler(errorHandler);
            try {
                db.parse(new FileInputStream(path), PCreatorConstantes.DTD_DOSSIER_PATH);
                if (errorHandler.isErreurRencontrees()) {
                    return false;
                } else {
                    return true;
                }
            } catch (SAXException e) {
                Application.logger.warn("Le fichier généré n'est pas valide et une erreur critique (XML mal formé ?) s'est produite.");
                Application.logger.info(e.getClass().getName() + " : " + e.getLocalizedMessage());
            } catch (IOException e) {
                Application.logger.error("Il y a eu un problème durant l'ouverture du fichier généré ou de son fichier DTD.");
                Application.logger.info(e.getClass().getName() + " : " + e.getLocalizedMessage());
            }
        } catch (ParserConfigurationException e) {
            Application.logger.error("Erreur de configuration de l'analyseur XML lors de la vérification de la conformité du fichier.");
            Application.logger.info(e.getClass().getName() + " : " + e.getLocalizedMessage());
        }
        return false;
    }

    /**
	 * Le thread qui servira a communiquer avec PExecutor, pour éviter que l'action d'export soit bloquante
	 * En effet, les communications entre les deux peuvent étre relativement lentes
	 * (connection au serveur, envoi de la chaine, puis des services...)
	 */
    private class SendToPCreatorThread extends Thread {

        /**
		 * Le document dans lequel on crée notre structure XML,
		 * qui sera ensuite envoyée à PExecutor
		 */
        private Document dom;

        /**
		 * Le format dans lequel on écrit les données
		 */
        private OutputFormat format;

        public SendToPCreatorThread(Document dom, OutputFormat format) {
            this.dom = dom;
            this.format = format;
        }

        @Override
        public void run() {
            try {
                boolean transactionTerminee = false;
                int TAILLE_BUFFER = 128;
                Socket PESocket = new Socket(PCreatorConstantes.PEXECUTOR_ADRESSE, PCreatorConstantes.PEXECUTOR_PORT_DIALOGUE);
                Application.logger.info("Connexion à PExecutor effectuée");
                PrintStream PEOutputStream = new PrintStream(PESocket.getOutputStream());
                BufferedReader PEAnswer = new BufferedReader(new InputStreamReader(PESocket.getInputStream()));
                Application.logger.info("Envoi des données");
                PEOutputStream.println("Envoi chaine : " + chaine.getNom());
                String reponse = PEAnswer.readLine();
                if (reponse.equals(PCreatorConstantes.PE_OK)) {
                    Socket PEFSocket = new Socket(PCreatorConstantes.PEXECUTOR_ADRESSE, PCreatorConstantes.PEXECUTOR_PORT_FICHIERS);
                    PrintStream PEFOutputStream = new PrintStream(PEFSocket.getOutputStream());
                    XMLSerializer serializer = new XMLSerializer(PEFOutputStream, format);
                    serializer.serialize(dom);
                    Application.logger.info("Chaine envoyée");
                    PEFSocket.close();
                    reponse = PEAnswer.readLine();
                    while (reponse != null && reponse.matches(PCreatorConstantes.PE_ENVOI_SERVICE + ".*")) {
                        Application.logger.debug(reponse);
                        Pattern pattern = Pattern.compile(PCreatorConstantes.PE_ENVOI_SERVICE + "(.*)");
                        Matcher matcher = pattern.matcher(reponse);
                        if (matcher.find()) {
                            String idService = matcher.group(1);
                            String emplacement = null;
                            try {
                                emplacement = dom.getElementById(idService).getAttribute("emplacement");
                                byte[] bbuf = new byte[TAILLE_BUFFER];
                                BufferedInputStream bim = new BufferedInputStream(new FileInputStream(emplacement));
                                PEOutputStream.println(PCreatorConstantes.PE_OK);
                                reponse = PEAnswer.readLine();
                                if (reponse != null && reponse.equals(PCreatorConstantes.PE_OK)) {
                                    PEFSocket = new Socket(PCreatorConstantes.PEXECUTOR_ADRESSE, PCreatorConstantes.PEXECUTOR_PORT_FICHIERS);
                                    PEFOutputStream = new PrintStream(PEFSocket.getOutputStream());
                                    int read = bim.read(bbuf, 0, TAILLE_BUFFER);
                                    while (read >= 0) {
                                        PEFOutputStream.write(bbuf, 0, read);
                                        read = bim.read(bbuf, 0, TAILLE_BUFFER);
                                    }
                                    PEFSocket.close();
                                    Application.logger.debug("Service " + idService + " envoyé avec succés");
                                    reponse = PEAnswer.readLine();
                                }
                            } catch (NullPointerException e) {
                                Application.logger.error("Service " + idService + " non trouvé.\n	Transaction annulée.");
                                PEOutputStream.println("Erreur : Service " + idService + "non trouvé");
                                break;
                            } catch (FileNotFoundException e) {
                                Application.logger.error("Impossible d'envoyer le service " + idService + ", emplacement invalide : " + emplacement + "\n	Transaction annulée.");
                                PEOutputStream.println("Erreur : Impossible d'envoyer le service " + idService + ", emplacement invalide : " + emplacement);
                                break;
                            }
                        }
                    }
                    if (reponse.equals(PCreatorConstantes.PE_FIN_TRANSMISSION)) {
                        Application.logger.info("Données envoyées avec succés, fermeture de la connexion.");
                        transactionTerminee = true;
                    }
                }
                if (!transactionTerminee) {
                    Application.logger.error("Une erreur s'est produite lors de l'envoi de la chaîne.\n	Dernier message du serveur : " + reponse);
                }
                PEOutputStream.close();
                PESocket.close();
            } catch (ConnectException ce) {
                Application.logger.error("Connexion à PExecutor échouée : " + ce.getLocalizedMessage());
            } catch (UnknownHostException e) {
                Application.logger.error("Connexion à PExecutor échouée (héte inconnu) : " + e.getLocalizedMessage());
            } catch (IOException e) {
                Application.logger.error("Erreur lors de l'envoi de la chaîne à PExecutor: " + e.getLocalizedMessage());
            }
        }
    }
}
