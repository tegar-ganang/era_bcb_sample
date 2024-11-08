package Simulation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import Generic.PathSearch.Dijkstra;
import Generic.PathSearch.SimulatedAnnealing;
import Simulation.Network.City;
import Simulation.Network.Network;
import Simulation.Network.Route;
import Simulation.Network.Section;
import Simulation.Request.IRequest;
import Simulation.Request.RequestXMLHandler;
import Simulation.Response.IResponse;
import Simulation.Response.PathResponse;
import Simulation.Response.ResponseXMLHandler;

/**
 * Classe fa�ade impl�mentant l'interface ISimulationServer qui permet de
 * recevoir les requ�tes des clients et de leur r�pondre.
 */
public class Simulation implements ISimulationServer {

    /**
	 * R�seau sur lequel la simulation travail.
	 */
    private final Network network;

    /**
	 * Dernier itin�taire calcul�.
	 */
    private final Route route = new Route();

    /**
	 * Liste de requ�te en attente de pouvoir �tre ex�cut�es.
	 */
    private final Deque<IRequest> waitingRequest = new ArrayDeque<IRequest>();

    /**
	 * Client de qui envoie les requ�tes et qui re�oit les r�ponses.
	 */
    private ISimulationClient simulationClient;

    /**
	 * Permet d'enregister le client.
	 * 
	 * @param simulationClient
	 *            client.
	 */
    public void setSimulationClient(ISimulationClient simulationClient) {
        this.simulationClient = simulationClient;
    }

    /**
	 * @return dernier itin�raire calcul�.
	 */
    public Route getRoute() {
        return this.route;
    }

    /**
	 * @return r�seau.
	 */
    public Network getNetwork() {
        return this.network;
    }

    /**
	 * Lance une recherche d'itin�raire sur le r�seau.
	 */
    public void searchPath() {
        this.getNetwork().searchPath(this.getRoute());
        this.executeWaitingRequest();
    }

    /**
	 * Ajoute une requ�te � la liste des requ�tes en attente d'�tre ex�cut�es.
	 * 
	 * @param request
	 *            requ�te � ajouter
	 */
    public void addRequest(IRequest request) {
        assert (request != null);
        this.waitingRequest.offerLast(request);
    }

    /**
	 * Instance du singleton
	 */
    private static Simulation instance;

    /**
	 * @return instance du singleton.
	 */
    public static Simulation getInstance() {
        if (instance == null) {
            instance = new Simulation();
        }
        return instance;
    }

    /**
	 * Supprime l'instance du singleton.
	 */
    public static void freeInstance() {
        instance = null;
    }

    /**
	 * Constructeur priv�e pour �viter une instantiation directe.
	 */
    private Simulation() {
        this.network = new Network(new SimulatedAnnealing<City, Section>(), new Dijkstra<City, Section>());
    }

    @Override
    public void receiveRequest(String sFilepath) {
        try {
            this.receiveRequest(new FileInputStream(sFilepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void receiveRequest(InputStream inputStream) {
        IRequest request = RequestXMLHandler.readRequest(inputStream);
        this.receiveRequest(request);
    }

    @Override
    public void receiveRequest(Reader reader) {
        IRequest request = RequestXMLHandler.readRequest(reader);
        this.receiveRequest(request);
    }

    @Override
    public void receiveRequest(IRequest request) {
        assert (request != null);
        this.waitingRequest.offerFirst(request);
        this.executeWaitingRequest();
    }

    /**
	 * �valuation des requ�tes en attentes et ex�cution si valide.
	 */
    protected void executeWaitingRequest() {
        int requestRefusedCount = 0;
        while (this.waitingRequest.size() > requestRefusedCount) {
            IRequest currentRequest = this.waitingRequest.poll();
            if (!currentRequest.doRequest()) {
                this.waitingRequest.offerLast(currentRequest);
                requestRefusedCount++;
            } else {
                this.CheckIfRouteNeedUpdate(currentRequest);
            }
        }
    }

    /**
	 * Indique au client que l'itin�raire a �t� mis � jour si la requ�te a
	 * modifi�e le r�seau.
	 * 
	 * @param request
	 *            requ�te qui peut �ventuellement modifier l'itin�raire.
	 */
    protected void CheckIfRouteNeedUpdate(IRequest request) {
        if (!this.route.CheckIfRouteNeedUpdate(request)) {
            return;
        }
        this.searchPath();
        PathResponse pathResponse = new PathResponse(this.route);
        this.sendResponse(pathResponse);
    }

    @Override
    public void sendResponse(String sFilepath) {
        try {
            this.sendResponse(new FileInputStream(sFilepath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendResponse(InputStream inputStream) {
        IResponse response = ResponseXMLHandler.readResponse(inputStream);
        this.sendResponse(response);
    }

    @Override
    public void sendResponse(IResponse response) {
        assert (response != null);
        assert (this.simulationClient != null);
        StringWriter writer = new StringWriter();
        ResponseXMLHandler.writeResponse(response, writer);
        StringReader reader = new StringReader(writer.toString());
        this.simulationClient.receiveRequest(reader);
    }

    @Override
    public void sendResponse(Reader reader) {
        IResponse response = ResponseXMLHandler.readResponse(reader);
        this.sendResponse(response);
    }
}
