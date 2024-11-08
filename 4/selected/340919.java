package com.eip.yost.relais;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import org.apache.log4j.xml.DOMConfigurator;
import com.eip.yost.utils.Constantes;
import com.eip.yost.utils.FactoryBundle;

public class ConnectionsManager {

    private Properties config = new Properties();

    private ServerSocket srvSock;

    private ArrayList<Pilote> pilotes;

    private ArrayList<Client> clients;

    public ConnectionsManager() throws IOException {
        initializeServer();
        stopServer();
    }

    private void startAndRunServer() {
        while (!srvSock.isClosed()) {
            acceptClient();
        }
    }

    private void stopServer() throws IOException {
        Debug.message("Coupure des connexions...");
        closeConnections();
        Debug.message("Arret du serveur");
        srvSock.close();
    }

    private void initializeServer() throws IOException {
        StringBuilder vConfigFile = new StringBuilder(Constantes.EXTERNAL_CONFIG_PATH).append("log4j.xml");
        DOMConfigurator.configureAndWatch(vConfigFile.toString(), 30000);
        Debug.message("Initialisation du serveur");
        Debug.message("Chargement de la configuration");
        loadConfig();
        FactoryBundle.getInstance().loadFile(new StringBuilder(Constantes.EXTERNAL_CONFIG_PATH).append(Constantes.EXTERNAL_PROPERTIES).toString());
        if (config != null) {
            int port = FactoryBundle.getInstance().getInt("relais.port");
            int timeout = FactoryBundle.getInstance().getInt("relais.timeout");
            if (port > 0 && timeout > 0) {
                Debug.message("Ouverture de la socket serveur sur le port " + port);
                srvSock = new ServerSocket(port);
                srvSock.setReuseAddress(true);
                srvSock.setSoTimeout(timeout);
                clients = new ArrayList<Client>();
                pilotes = new ArrayList<Pilote>();
                startAndRunServer();
            }
        }
    }

    private void loadConfig() {
        try {
            config.load(new FileInputStream("yost.conf"));
        } catch (IOException e) {
            Debug.error(e.getLocalizedMessage());
        }
    }

    private void acceptClient() {
        try {
            removeUnusedAgents();
            Socket cliSock;
            try {
                cliSock = srvSock.accept();
            } catch (IOException e) {
                throw new ExceptionAccept();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(cliSock.getInputStream()));
            String agentIdentifier = br.readLine();
            if (!identifierValid(agentIdentifier) || identifierAlreadyExists(agentIdentifier)) {
                cliSock.getOutputStream().write("KO => Login already exists!".getBytes());
                cliSock.close();
            } else {
                if (agentIdentifier.startsWith("pilote")) {
                    Debug.message("Connexion du pilote \"" + agentIdentifier + "\"");
                    Pilote tmpPilote = new Pilote(this, cliSock, agentIdentifier);
                    tmpPilote.start();
                    pilotes.add(tmpPilote);
                } else {
                    Debug.message("Connexion du client \"" + agentIdentifier + "\"");
                    Client cli = new Client(this, cliSock, agentIdentifier);
                    cli.start();
                    clients.add(cli);
                }
            }
        } catch (ExceptionAccept e1) {
        } catch (IOException e2) {
            Debug.error(e2.getLocalizedMessage());
        }
    }

    private boolean identifierAlreadyExists(String id) {
        for (Client c : clients) {
            if (c.getIdentifier().equals(id)) return true;
        }
        for (Pilote p : pilotes) {
            if (p.getIdentifier().equals(id)) return true;
        }
        return false;
    }

    private boolean identifierValid(String id) {
        return id.matches("^[a-zA-Z0-9_\\-]+$");
    }

    private void removeUnusedAgents() {
        Iterator<Client> i = clients.iterator();
        while (i.hasNext()) {
            Client c = i.next();
            if (!c.isAlive()) {
                Debug.message("Deconnexion du client \"" + c.getIdentifier() + "\"");
                c.closeConnection();
                i.remove();
            }
        }
        Iterator<Pilote> it = pilotes.iterator();
        while (it.hasNext()) {
            Pilote p = it.next();
            if (!p.isAlive()) {
                Debug.message("Deconnexion du pilote \"" + p.getIdentifier() + "\"");
                p.closeConnection();
                it.remove();
            }
        }
    }

    private void closeConnections() {
        for (Pilote p : pilotes) {
            p.closeConnection();
        }
        for (Client c : clients) {
            c.closeConnection();
        }
        pilotes.clear();
        clients.clear();
    }

    public ArrayList<Client> getClients() {
        return clients;
    }

    public ArrayList<Pilote> getPilotes() {
        return pilotes;
    }
}
