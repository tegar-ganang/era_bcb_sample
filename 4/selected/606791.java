package poweria.guia.serveur;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import poweria.guia.serveur.message.MessageType;
import poweria.joueur.Equipe;
import poweria.ordre.ListOrdre;

/**
 *
 * @author Cody Stoutenburg
 */
public class JoueurPartieServeur extends Thread {

    private ServeurPartie _serveur;

    private Socket _joueur;

    private OutputStream _outputStream;

    private InputStream _inputStream;

    private int _tourJoueur;

    private Equipe _equipe;

    private int _id;

    public JoueurPartieServeur(Socket soc, ServeurPartie serv, int id, Equipe eq) {
        this._joueur = soc;
        this._serveur = serv;
        this._tourJoueur = -1;
        this._equipe = eq;
        this._id = id;
        try {
            this._outputStream = _joueur.getOutputStream();
            this._inputStream = _joueur.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Equipe getEquipe() {
        return _equipe;
    }

    public synchronized void waitForInit() {
        if (this._tourJoueur != 0) {
            try {
                wait();
            } catch (InterruptedException ex) {
                Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private synchronized void write(int i) {
        try {
            this._outputStream.write(i);
        } catch (IOException ex) {
            Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private synchronized void write(Object o) {
        try {
            ObjectOutputStream obj = new ObjectOutputStream(this._outputStream);
            obj.writeObject(o);
        } catch (IOException ex) {
            Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void init() {
        Logger logger = Logger.getLogger(JoueurPartieServeur.class.getName());
        try {
            boolean isInit = false;
            this.write(this._id);
            logger.log(Level.INFO, "INIT");
            while (!isInit) {
                int command = this._inputStream.read();
                ObjectInputStream in = null;
                switch(command) {
                    case MessageType.CHANGE_TEAM:
                        logger.log(Level.INFO, "CHANGE TEAM");
                        in = new ObjectInputStream(this._inputStream);
                        Equipe tmp = (Equipe) in.readObject();
                        logger.log(Level.INFO, "nouvelle equipe = {0}", tmp);
                        if (this._serveur.getAllEquipeDisponible().contains(tmp)) {
                            this._equipe = tmp;
                        }
                        break;
                    case MessageType.ASK_TEAM:
                        logger.log(Level.INFO, "ASK TEAM");
                        logger.log(Level.INFO, "equipe = {0}", this._equipe);
                        this.write(this._equipe);
                        break;
                    case MessageType.ASK_ALL_PLAYER:
                        logger.log(Level.INFO, "ASK ALL PLAYER");
                        int[] joueur = this._serveur.getAllJoueur();
                        for (int i = 0; i < joueur.length; i++) {
                            this.write(joueur[i]);
                        }
                        break;
                    case MessageType.ASK_TEAM_FOR_PLAYER:
                        logger.log(Level.INFO, "ASK TEAM FOR PLAYER");
                        int player = this._inputStream.read();
                        logger.log(Level.INFO, "PLAYER = {0}", player);
                        Equipe eq = this._serveur.getEquipe(player);
                        logger.log(Level.INFO, "EQUIPE = {0}", eq);
                        this.write(eq);
                        break;
                    case MessageType.ASK_NB_PLAYER:
                        logger.log(Level.INFO, "ASK NB PLAYER");
                        this.write(this._serveur.getNbJoueur());
                        break;
                    case MessageType.INIT_FINISH:
                        logger.log(Level.INFO, "init finish");
                    default:
                        isInit = true;
                        break;
                }
            }
            logger.log(Level.INFO, "END INIT");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
        }
        this._tourJoueur = 0;
        notifyAll();
    }

    @Override
    public void run() {
        Logger logger = Logger.getLogger(JoueurPartieServeur.class.getName());
        init();
        this._serveur.isReady();
        try {
            logger.log(Level.INFO, "Serveur is ready");
            this.write(0);
            while (!this._serveur.isFinish()) {
                int command = this._inputStream.read();
                ObjectInputStream in = null;
                switch(command) {
                    case MessageType.ASK_PLATEAU:
                        logger.log(Level.INFO, "ASK PLATEAU");
                        int cpt = 0;
                        while (_tourJoueur != this._serveur.getCurrentTour() || this._tourJoueur == 0) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            cpt++;
                            if (cpt == Integer.MAX_VALUE) System.exit(1000);
                        }
                        this.write(this._serveur.getPlateau());
                        break;
                    case MessageType.END_TURN:
                        logger.log(Level.INFO, "END TURN");
                        in = new ObjectInputStream(this._inputStream);
                        this._serveur.play((ListOrdre) in.readObject());
                        _tourJoueur++;
                        break;
                    case MessageType.HAVE_WIN:
                        logger.log(Level.INFO, "HAVE WIN");
                        in = new ObjectInputStream(this._inputStream);
                        this.write(this._serveur.haveWin((Equipe) in.readObject()));
                        break;
                    case MessageType.HAVE_LOSE:
                        logger.log(Level.INFO, "HAVE LOSE");
                        in = new ObjectInputStream(this._inputStream);
                        this.write(this._serveur.haveLose((Equipe) in.readObject()));
                        break;
                    case MessageType.TURN:
                        logger.log(Level.INFO, "TURN");
                        this.write(this._serveur.getCurrentTour());
                        break;
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JoueurPartieServeur.class.getName()).log(Level.SEVERE, "FATAL ERROR", ex);
            System.exit(10);
        }
    }
}
