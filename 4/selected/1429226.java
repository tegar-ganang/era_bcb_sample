package logique.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import javax.swing.SwingWorker;
import logique.reseau.Reseau;
import logique.reseau.Route;
import logique.reseau.Troncon;
import logique.reseau.Ville;
import org.dom4j.io.XMLWriter;
import org.dom4j.tree.BaseElement;
import controleur.Controleur;

/**
 * 
 * @author camille
 *
 */
public class Parser {

    private static final int XML_MAX_SIZE = 35000000;

    public static final String FILE_EXT = ".xml";

    public static final String TEMP_FILE_EXT = ".temp";

    public static final String VILLES_DIR = "villes";

    public static final String ROUTES_DIR = "routes";

    public static final String DESC_NODE_NAME = "description";

    public static final String ROOT_NODE_NAME = "reseau";

    public static final String VILLE_NODE_NAME = "ville";

    public static final String ROUTE_NODE_NAME = "route";

    public static final String TRONCON_NODE_NAME = "troncon";

    public static final String NOM_NODE_NAME = "nom";

    public static final String TYPE_NODE_NAME = "type";

    public static final String TOURIST_NODE_NAME = "touristique";

    public static final String VILLE1_NODE_NAME = "ville1";

    public static final String VILLE2_NODE_NAME = "ville2";

    public static final String VITESSE_NODE_NAME = "vitesse";

    public static final String RADAR_NODE_NAME = "radar";

    public static final String PAYANT_NODE_NAME = "payant";

    public static final String LONGUEUR_NODE_NAME = "longueur";

    public static final String IND_POSITIVE = "oui";

    public static final String IND_NEGATIVE = "non";

    public static final String STATE_PROPERTY_NAME = "step";

    private Controleur controleur;

    private NetWorkReader netReader;

    private NetworkWriter netWriter;

    private URL urlReseau;

    private String parserWorkspace;

    private boolean fullParsing;

    private boolean segmentationDone;

    private LinkedList<Ville> villesToAddToFS;

    private LinkedList<Route> routesToAddToFS;

    private LinkedList<Troncon> tronconsToAddToFS;

    private LinkedList<Ville> villesToRemoveFromFS;

    private LinkedList<Route> routesToRemoveFromFS;

    private LinkedList<Troncon> tronconsToRemoveFromFS;

    public Parser(Controleur controleur) {
        this.controleur = controleur;
        netReader = new NetWorkReader(this);
        netWriter = new NetworkWriter(this);
        urlReseau = null;
        fullParsing = false;
        segmentationDone = false;
        villesToAddToFS = new LinkedList<Ville>();
        routesToAddToFS = new LinkedList<Route>();
        tronconsToAddToFS = new LinkedList<Troncon>();
        villesToRemoveFromFS = new LinkedList<Ville>();
        routesToRemoveFromFS = new LinkedList<Route>();
        tronconsToRemoveFromFS = new LinkedList<Troncon>();
    }

    public void setFichier(String cheminFichier) throws FileNotFoundException, MalformedURLException {
        File fichierXML = new File(cheminFichier);
        if (!fichierXML.exists()) {
            throw new FileNotFoundException(cheminFichier);
        }
        urlReseau = fichierXML.toURI().toURL();
        fullParsing = (fichierXML.length() < XML_MAX_SIZE);
    }

    public boolean chargementComplet() {
        return fullParsing;
    }

    public SwingWorker<Void, Void> segmenteReseau() throws ParserException {
        segmentationDone(false);
        createFileSystemFor(urlReseau);
        SwingWorker<Void, Void> wS = netReader.getWorkerSegmentation();
        wS.execute();
        return wS;
    }

    private void createFileSystemFor(URL reseau) throws ParserException {
        setWorkspace(getFileSystemRoot(reseau).getAbsolutePath());
        File dir = new File(parserWorkspace);
        if (!(dir.exists() && dir.isDirectory())) {
            dir.mkdir();
        }
        dir = new File(parserWorkspace + File.separator + VILLES_DIR);
        if (!(dir.exists() && dir.isDirectory())) {
            dir.mkdir();
        } else {
            for (File f : dir.listFiles()) {
                f.delete();
            }
        }
        dir = new File(parserWorkspace + File.separator + ROUTES_DIR);
        if (!(dir.exists() && dir.isDirectory())) {
            dir.mkdir();
        } else {
            for (File f : dir.listFiles()) {
                f.delete();
            }
        }
    }

    public SwingWorker<Reseau, Void> getWorkerOnePass() {
        return netReader.getWorkerOnePass();
    }

    public SwingWorker<Hashtable<String, Ville>, Void> getWorkerListeVilles() throws ParserException {
        if (urlReseau == null) {
            throw new ParserException("Pas de fichier reseau selectionne");
        }
        if (segmentationDone()) {
            return netReader.wLVSegmented();
        } else {
            return netReader.wLVUnsegmented();
        }
    }

    public SwingWorker<Hashtable<String, Route>, Void> getWorkerListeRoutes() throws ParserException {
        if (urlReseau == null) {
            throw new ParserException("Pas de fichier reseau selectionne");
        }
        return netReader.wLR();
    }

    public SwingWorker<LinkedList<Troncon>, Void> getWorkerTroncons(final Route route) throws ParserException {
        if (urlReseau == null) {
            throw new ParserException("Pas de fichier reseau selectionne");
        }
        if (segmentationDone()) {
            return netReader.wLTSegmented(route);
        } else {
            return netReader.wLTUnsegmented(route);
        }
    }

    public SwingWorker<LinkedList<Troncon>, Void> getWorkerTroncons(final Ville ville) throws Exception {
        if (urlReseau == null) {
            throw new Exception("Pas de fichier reseau selectionne");
        }
        if (segmentationDone()) {
            return netReader.wLTSegmented(ville);
        } else {
            return netReader.wLTUnsegmented(ville);
        }
    }

    public void addToFS(Ville ville) throws IOException {
        villesToRemoveFromFS.remove(ville);
        if (segmentationDone()) {
            netWriter.addToFS(ville);
        } else {
            villesToAddToFS.add(ville);
        }
    }

    public void addToFS(Route route) throws Exception {
        routesToRemoveFromFS.remove(route);
        if (segmentationDone()) {
            netWriter.addToFS(route);
        } else {
            routesToAddToFS.add(route);
        }
    }

    public void addToFS(Troncon troncon) throws Exception {
        tronconsToRemoveFromFS.remove(troncon);
        if (segmentationDone()) {
            netWriter.addToFS(troncon);
        } else {
            tronconsToAddToFS.add(troncon);
        }
    }

    public void removeFromFS(Ville ville) {
        villesToAddToFS.remove(ville);
        if (segmentationDone()) {
            netWriter.removeFromFS(ville);
        } else {
            villesToRemoveFromFS.add(ville);
        }
    }

    public void removeFromFS(Route route) {
        routesToAddToFS.remove(route);
        if (segmentationDone()) {
            netWriter.removeFromFS(route);
        } else {
            routesToRemoveFromFS.add(route);
        }
    }

    public void removeFromFS(Troncon troncon) {
        tronconsToAddToFS.remove(troncon);
        if (segmentationDone()) {
            netWriter.removeFromFS(troncon);
        } else {
            tronconsToRemoveFromFS.add(troncon);
        }
    }

    public void updateFS(String name, Ville newVille) {
        Ville ville = controleur.getVilles().get(name);
        villesToAddToFS.remove(ville);
        if (segmentationDone()) {
            netWriter.updateFS(ville, newVille);
        } else {
            villesToAddToFS.add(newVille);
        }
    }

    public void updateFS(String name, Route newRoute) {
        Route route = controleur.getRoutes().get(name);
        routesToAddToFS.remove(route);
        if (segmentationDone()) {
            netWriter.updateFS(route, newRoute);
        } else {
            routesToAddToFS.add(newRoute);
        }
    }

    public void updateFS(Troncon troncon, Troncon newTroncon) {
        tronconsToAddToFS.remove(troncon);
        if (segmentationDone()) {
            netWriter.updateFS(troncon, newTroncon);
        } else {
            tronconsToAddToFS.add(newTroncon);
        }
    }

    public synchronized void ecrireReseau(String savePath) throws Exception {
        XMLWriter writer = new XMLWriter(new FileWriter(savePath));
        BaseElement rootElement = new BaseElement(Parser.ROOT_NODE_NAME);
        writer.startDocument();
        writer.writeOpen(rootElement);
        for (Iterator<Entry<String, Ville>> iterator = controleur.getReseau().getVilles().entrySet().iterator(); iterator.hasNext(); ) {
            Entry<String, Ville> entry = iterator.next();
            netWriter.ecrireVille(writer, entry.getValue());
        }
        for (Iterator<Entry<String, Route>> iterator = controleur.getReseau().getRoutes().entrySet().iterator(); iterator.hasNext(); ) {
            Entry<String, Route> entry = iterator.next();
            netWriter.ecrireRoute(writer, entry.getValue());
        }
        writer.writeClose(rootElement);
        rootElement.clearContent();
        writer.endDocument();
        writer.close();
    }

    public static void copyFile(File src, File dest) throws IOException {
        FileInputStream fIn;
        FileOutputStream fOut;
        FileChannel fIChan, fOChan;
        long fSize;
        MappedByteBuffer mBuf;
        fIn = new FileInputStream(src);
        fOut = new FileOutputStream(dest);
        fIChan = fIn.getChannel();
        fOChan = fOut.getChannel();
        fSize = fIChan.size();
        mBuf = fIChan.map(FileChannel.MapMode.READ_ONLY, 0, fSize);
        fOChan.write(mBuf);
        fIChan.close();
        fIn.close();
        fOChan.close();
        fOut.close();
    }

    public boolean fullParsing() {
        return fullParsing;
    }

    public synchronized boolean segmentationDone() {
        return segmentationDone;
    }

    public synchronized void segmentationDone(boolean done) {
        segmentationDone = true;
    }

    public Controleur getControleur() {
        return controleur;
    }

    public String getWorkspace() {
        return parserWorkspace;
    }

    public File getVilleFile(String nomVille) {
        return new File(parserWorkspace + File.separator + Parser.VILLES_DIR + File.separator + nomVille + Parser.FILE_EXT);
    }

    public File getRouteFile(String nomRoute) {
        return new File(parserWorkspace + File.separator + Parser.ROUTES_DIR + File.separator + nomRoute + Parser.FILE_EXT);
    }

    public File getFileSystemRoot(URL networkPath) {
        return new File(controleur.getWorkspace() + File.separator + networkPath.getFile().substring(networkPath.getFile().lastIndexOf(File.separator) + 1, networkPath.getFile().lastIndexOf('.')));
    }

    public void setWorkspace(String workspace) {
        this.parserWorkspace = workspace;
    }

    public URL getURLreseau() {
        return urlReseau;
    }

    public NetWorkReader getNReader() {
        return netReader;
    }

    public NetworkWriter getNWriter() {
        return netWriter;
    }

    public LinkedList<Ville> getVillesToAddToFS() {
        return villesToAddToFS;
    }

    public LinkedList<Route> getRoutesToAddToFS() {
        return routesToAddToFS;
    }

    public LinkedList<Troncon> getTronconsToAddToFS() {
        return tronconsToAddToFS;
    }

    public LinkedList<Ville> getVillesToRemoveFromFS() {
        return villesToRemoveFromFS;
    }

    public LinkedList<Route> getRoutesToRemoveFromFS() {
        return routesToRemoveFromFS;
    }

    public LinkedList<Troncon> getTronconsToRemoveFromFS() {
        return tronconsToRemoveFromFS;
    }
}
