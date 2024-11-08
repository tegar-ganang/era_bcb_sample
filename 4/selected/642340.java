package serveur;

import org.apache.log4j.Logger;
import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.net.*;
import java.io.*;
import api_reseau.*;
import api_moteur.*;
import reseau.*;
import moteur.*;

public final class Client implements Serializable {

    static Logger logger = Logger.getLogger(Client.class);

    /** Socket associe au client */
    protected Socket socket;

    /** Le jeu associe */
    protected Jeu jeu;

    /** Le nom du client */
    protected String name;

    /** Les flux vers le client */
    protected ObjectInputStream inObj = null;

    protected ObjectOutputStream outObj = null;

    protected boolean hasRegistredTrader;

    /** Le constructeur classique
	 */
    public Client(Socket socket) {
        this.socket = socket;
        this.name = "foo";
        this.hasRegistredTrader = false;
    }

    /** Renvoie le flux entrant */
    public ObjectInputStream getInObj() {
        return inObj;
    }

    /** Renvoie le flux sortant */
    public ObjectOutputStream getOutObj() {
        return outObj;
    }

    /** Renvoie le nom du client */
    public String getName() {
        return name;
    }

    /** Renvoie le jeu actuel */
    public Jeu getJeu() {
        return jeu;
    }

    /** Renvoie l'adresse du client */
    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    /** Renvoie le port du client */
    public int getPort() {
        return socket.getPort();
    }

    /** Renvoie la socket du client */
    public Socket getSocket() {
        return socket;
    }

    /** Change le nom du client */
    public void setName(String name) {
        this.name = name;
    }

    /** Renvoie si le client a registre son trader ou non */
    public boolean hasRegistredTrader() {
        return hasRegistredTrader;
    }

    /** On veut detruire l'objet, par exemple, quand la partie est terminee
	 */
    public void destroy() {
        jeu = null;
        socket = null;
    }

    /** Envoie la configuration d'une partie
	 */
    public void sendPartie(PartieCrise p) {
        if (inObj == null || outObj == null) return;
        if (p == null) return;
        if (!jeu.hasPartieStarted()) {
            logger.info("Un joueur essaye de jouer alors que la partie n'est pas encore commencee.");
        }
        try {
            outObj.reset();
            outObj.writeObject(new NewTurnRequest(p));
            logger.info("La partie a ete envoye a un client.");
        } catch (IOException e) {
            logger.error("Probleme de connexion.");
            return;
        }
    }

    /** Joue un tour */
    public void turn(NewTurnRequest request) {
        if (jeu == null) return;
        if (inObj == null || outObj == null) return;
        if (!jeu.hasPartieStarted()) {
            logger.info("Un joueur essaye de jouer alors que la partie n'est pas encore commencee.");
        }
        try {
            if (request == null || request.getPartie() == null) {
                outObj.writeObject(new InvalidRequest(new InvalidException()));
            }
            jeu.sendStuffsToNextPlayer(request.getPartie());
        } catch (IOException e) {
            logger.error("Probleme de connexion.");
            return;
        }
    }

    /** Registre un trader */
    public void registerTrader(TraderUpdateRequestI request) {
        if (jeu == null) return;
        if (!jeu.hasGameStarted()) return;
        if (jeu.hasPartieStarted()) return;
        if (hasRegistredTrader) return;
        if (inObj == null || outObj == null) return;
        try {
            if (request == null || request.getTrader() == null) {
                outObj.writeObject(new InvalidRequest(new InvalidException()));
                logger.error("Un client a envoye une requete de synchronisation de Trader invalide.");
            }
            hasRegistredTrader = true;
            jeu.addTrader(request.getTrader(), this);
            logger.info("Trader " + request.getTrader());
            outObj.writeObject(new ValidRequest());
        } catch (IOException e) {
            logger.error("Probleme de connexion.");
            return;
        }
    }

    /** Gere l'ouverture de partie */
    public void open(OpenRequestI request, ObjectInputStream in, ObjectOutputStream out) {
        try {
            if (request == null) {
                logger.error("Probleme : requete invalide");
                out.writeObject(new InvalidRequest(new InvalidException()));
                return;
            } else {
                int taille = request.getPlateauTaille();
                if (taille < 7) {
                    logger.warn("Mauvaise taille de plateau de jeu demandee : " + taille);
                    taille = 7;
                } else logger.info("Taille du plateau : " + taille);
                int nbGaranties = request.getNbGaranties();
                if (nbGaranties < 0) {
                    logger.warn("Mauvais nombre de garantie demande : " + nbGaranties);
                    nbGaranties = 0;
                } else logger.info("Nombre de Garanties: " + nbGaranties);
                int nbJoueurs = request.getNbJoueurs();
                if (nbJoueurs < 2) {
                    logger.warn("Mauvais nombre de joueurs demande : " + nbGaranties);
                    nbJoueurs = 2;
                } else logger.info("Nombre de Joueurs: " + nbJoueurs);
                setName(request.getPlayerName());
                logger.info("Le joueur administrateur se nomme : " + request.getPlayerName());
                Jeu j = null;
                try {
                    j = Jeu.getNewJeu(request.getPartieName(), this, taille, nbGaranties, nbJoueurs);
                    if (j == null || j.getPartie() == null) {
                        logger.error("Creation de jeu impossible : ce nom de jeu existe deja");
                        out.writeObject(new InvalidRequest(new AlreadyExistsException()));
                        return;
                    } else {
                        if (this.jeu != null) this.jeu.close();
                        this.jeu = j;
                        try {
                            j.addClient(this);
                        } catch (TooManyPlayersException e) {
                            logger.error("Le jeu auquel voulait se connecte le client possede deja trop de joueurs.");
                            out.writeObject(new InvalidRequest(new TooManyPlayersException()));
                            return;
                        }
                        out.writeObject(new ValidRequest());
                        outObj = out;
                        inObj = in;
                        return;
                    }
                } catch (Exception e) {
                    logger.error("Creation de jeu impossible");
                    out.writeObject(new InvalidRequest(new Exception()));
                    return;
                }
            }
        } catch (IOException e) {
            logger.error("Probleme de connexion.");
            return;
        } catch (NumberFormatException e) {
            logger.error("Creation de jeu impossible");
            return;
        }
    }

    /** Gere le quittage de partie */
    public void quit() {
        if (this.jeu != null) {
            logger.info("Le Jeu " + this.jeu.getName() + " a ete termine par le client " + this.getInetAddress() + ".");
            this.jeu.close();
            this.jeu = null;
            this.socket = null;
            return;
        } else {
            logger.warn("Le client " + this.getInetAddress() + " a essayer de terminer son jeu, mais celui ci n'existait pas.");
            return;
        }
    }

    /** Gere le joignage de partie */
    public void join(JoinRequestI request, ObjectInputStream in, ObjectOutputStream out) {
        try {
            if (request == null) {
                logger.error("Probleme : requete invalide");
                out.writeObject(new InvalidRequest(new InvalidException()));
                return;
            } else {
                if (jeu != null && jeu.getName().equals(request.getPartieName())) {
                    logger.error("Le client fait deja partie du jeu " + request.getPartieName() + " sous le nom " + getName() + " .");
                    out.writeObject(new InvalidRequest(new YouAreAlreadyInItException()));
                    return;
                }
                Jeu j = Jeu.getJeu(request.getPartieName());
                if (j == null) {
                    logger.error("Le client voudrait se connecter a un jeu qui n'existe pas, ou plus: " + request.getPartieName());
                    out.writeObject(new InvalidRequest(new Inexistant_Game()));
                    return;
                }
                ArrayList<Client> aclient = j.getClients();
                if (aclient == null) {
                    logger.error("Le jeu auquel voudrait se connecter le client est invalide: " + request.getPartieName());
                    out.writeObject(new InvalidRequest(new InvalidException()));
                    return;
                }
                Iterator<Client> it = aclient.iterator();
                for (; it.hasNext(); ) {
                    Client c = it.next();
                    if (c.getName().equals(request.getPlayerName())) {
                        logger.error("Le jeu auquel voudrait se connecter le client possede deja un joueur du meme nom, ou le client est" + " deja connecte a cette partie. Le Jeu en question est : " + request.getPartieName());
                        out.writeObject(new InvalidRequest(new PlayerNameAlreadyUsed()));
                        return;
                    }
                }
                if (jeu != null) {
                    quit();
                }
                setName(request.getPlayerName());
                jeu = j;
                outObj = out;
                inObj = in;
                try {
                    j.addClient(this);
                } catch (TooManyPlayersException e) {
                    logger.error("Le jeu auquel voulait se connecte le client possede deja trop de joueurs.");
                    out.writeObject(new InvalidRequest(new TooManyPlayersException()));
                    return;
                }
                out.writeObject(new ValidRequest());
                logger.info("Le client " + getInetAddress() + " a l'adresse " + getPort() + " a rejoint la partie " + j.getName() + " sous le nom " + getName() + " .");
                j.start();
            }
        } catch (IOException e) {
            logger.error("Probleme de connexion.");
            return;
        } catch (NumberFormatException e) {
            logger.error("Creation de jeu impossible");
            return;
        }
    }
}
