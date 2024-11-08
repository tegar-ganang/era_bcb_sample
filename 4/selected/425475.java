package org.fudaa.dodico.simboat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;
import org.fudaa.ctulu.CtuluLibString;
import org.fudaa.dodico.corba.navmer.*;
import org.fudaa.dodico.corba.objet.IConnexion;
import org.fudaa.dodico.corba.objet.IPersonne;
import org.fudaa.dodico.corba.simboat.ISimulateurSimboat;
import org.fudaa.dodico.corba.simboat.ISimulateurSimboatOperations;
import org.fudaa.dodico.corba.simulation.SMobile;
import org.fudaa.dodico.corba.simulation.SOrdreMobile;
import org.fudaa.dodico.corba.simulation.typeAffichage;
import org.fudaa.dodico.navmer.DParametresNavmer;
import org.fudaa.dodico.objet.CDodico;
import org.fudaa.dodico.simulation.DSimulateur;

/**
 * Simulateur pour navmer.
 * 
 * @version $Revision: 1.9 $ $Date: 2006-09-19 14:45:58 $ by $Author: deniger $
 * @author Nicolas Maillot
 */
public class DSimulateurSimboat extends DSimulateur implements ISimulateurSimboat, ISimulateurSimboatOperations {

    IPersonne personne_;

    SMobile[] mobiles_;

    Vector[] ordresMobiles_;

    SCoefficientsNavire[] coeffNav_;

    SParametresINI[] paramINI_;

    int nbMobiles_;

    /**
   * periode de calcul.
   */
    public static final int periode_ = 4;

    public DSimulateurSimboat() {
        super();
        mobiles_ = chargeScenario("dodico_java_ecrit/org/fudaa/dodico/simboat/scenarios/scenario1.sc");
        paramINI_ = new SParametresINI[nbMobiles_];
        ordresMobiles_ = new Vector[nbMobiles_];
        for (int i = 0; i < nbMobiles_; i++) {
            ordresMobiles_[i] = new Vector();
        }
    }

    /**
   * Utilisateur (serveur) du service qui sert � cr�er les connexions. avec les calculs Navmer
   */
    public void setUtilisateur(final IPersonne p) {
        personne_ = p;
    }

    /**
   * Modifie la valeur de l attribut CoeffNav pour DSimulateurSimboat object.
   * 
   * @param chemins La nouvelle valeur de CoeffNav
   */
    public void setCoeffNav(final String[] chemins) {
        coeffNav_ = new SCoefficientsNavire[nbMobiles_];
        for (int i = 0; i < chemins.length; i++) {
            System.out.println(chemins[i]);
            coeffNav_[i] = DParametresNavmer.lectureFichierNav(chemins[i]);
        }
    }

    public final Object clone() throws CloneNotSupportedException {
        return new DSimulateurSimboat();
    }

    public String toString() {
        return "DSimulateur()";
    }

    public String description() {
        return "Simulateur" + super.description();
    }

    public SMobile[] mobiles() {
        return mobiles_;
    }

    public void mobiles(final SMobile[] _mobiles) {
        System.arraycopy(_mobiles, 0, mobiles_, 0, nbMobiles_);
    }

    /**
   * cette fonction recupere les ordres envoyes par les clients. et les place dans le vecteur d'ordres associe au bon
   * client
   */
    public void ordre(final SOrdreMobile[] ordres) {
        for (int i = 0; i < ordres.length; i++) {
            System.err.println("Passage d'un ordre au destinataire n�" + ordres[i].destinataire);
            (ordresMobiles_[ordres[i].destinataire]).addElement(ordres[i]);
        }
    }

    /**
   * Renvoie une image, ou un objet structur�(vrml..) selon le type d'affichage demand�. le second parametre correspon �
   * l'etat du mobile(endommag�,...)Description de la methode.
   * 
   * @param type
   * @param representation
   */
    public byte[] representation(final typeAffichage type, final int representation) {
        byte[] tab = null;
        if (type == typeAffichage.IMAGE) {
            try {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                final String path = "dodico_java_ecrit/org/fudaa/dodico/simboat/ressources/" + representation + "/" + type.value() + ".gif";
                final FileInputStream is = new FileInputStream(path);
                while (is.available() != 0) {
                    os.write(is.read());
                }
                tab = os.toByteArray();
                os.close();
                is.close();
            } catch (final IOException _e) {
            }
        }
        return tab;
    }

    public void simule() {
        final SResultatsDAT[] res = new SResultatsDAT[nbMobiles_];
        final String[] interfaces = CDodico.findServerNames("::navmer::ICalculNavmer", 8000);
        final ICalculNavmer[] nv = new ICalculNavmer[interfaces.length];
        final IConnexion[] cx = new IConnexion[interfaces.length];
        int k;
        for (k = 0; k < interfaces.length; k++) {
            nv[k] = ICalculNavmerHelper.narrow(CDodico.findServerByName(interfaces[k], 4000));
            if (nv[k] == null) {
                System.err.println("connexion au serveur echouee...");
            } else {
                cx[k] = nv[k].connexion(personne_);
                System.err.println("connexion " + cx[k].enChaine() + " etablie...");
            }
        }
        final IParametresNavmer[] paramsNv = new IParametresNavmer[nv.length];
        for (k = 0; k < interfaces.length; k++) {
            paramsNv[k] = IParametresNavmerHelper.narrow(nv[k].parametres(cx[k]));
        }
        if (paramsNv == null) {
            System.out.println(" paramsN null...");
        }
        for (int i = 0; i < nbMobiles_; i++) {
            paramINI_[i] = new SParametresINI();
            paramINI_[i].entete = new SEnteteINI();
            paramINI_[i].entete.tempsDebut = 0;
            paramINI_[i].entete.navire = "";
            paramINI_[i].entete.port = "";
            paramINI_[i].entete.parametresProgramme = new SParametresProgramme();
            paramINI_[i].entete.parametresProgramme.periodeSortieResultats = 1;
            paramINI_[i].entete.parametresProgramme.periodePriseEnCompteEtat = 1;
            paramINI_[i].entete.etatInitial = genereEtatInitial(0, mobiles_[i].x, mobiles_[i].y, mobiles_[i].az, mobiles_[i].vx, mobiles_[i].vy, mobiles_[i].vrz);
        }
        final double t1 = nbMobiles_;
        double t2 = nv.length;
        if (t2 > t1) {
            t2 = t1;
        }
        final Thread[] threads = new Thread[(int) t2];
        System.out.println("Nombre de thread " + threads.length);
        int nb = (int) Math.round(t1 / t2);
        while (true) {
            final int tempsavantcalcul = (int) System.currentTimeMillis();
            int i;
            int numserv = 0;
            final int nbtmp = nb;
            for (i = 0, numserv = 0; i < nbMobiles_; i += nb, numserv++) {
                if (nbMobiles_ - i <= nb) {
                    nb = nbMobiles_ - i;
                }
                threads[numserv] = new simboatThread(i, nb, numserv, paramsNv, nv, cx, res) {

                    /**
           * La methode dexecution pour {1} {2}
           */
                    public void run() {
                        Vector ordre = null;
                        for (int j = i_; j < i_ + nb_; j++) {
                            ordre = ordresMobiles_[j];
                            paramINI_[j].ordres = new SPassageOrdre[ordre.size()];
                            paramINI_[j].entete.tempsFin = periode_ * 10;
                            for (int u = 0; u < ordre.size(); u++) {
                                paramINI_[j].ordres[u] = new SPassageOrdre(u, ((SOrdreMobile) ordre.elementAt(u)).ordre, ((SOrdreMobile) ordre.elementAt(u)).valeur);
                                paramINI_[j].ordres[u].instant = periode_ * 10 / ordre.size() - 1;
                                System.err.println("*****" + periode_ + " " + ordre.size() + "****");
                                System.err.println("temps Debut: " + paramINI_[j].entete.tempsDebut);
                                System.err.println("Instant: " + paramINI_[j].ordres[u].instant);
                                System.err.println("temps fin: " + paramINI_[j].entete.tempsFin);
                                System.err.println("mobile n�" + j + " ORDRE: " + "Type: " + paramINI_[j].ordres[u].type + " Valeur:" + paramINI_[j].ordres[u].valeur);
                            }
                            ordresMobiles_[j].removeAllElements();
                            paramNv_[numserv_].parametresINI(paramINI_[j]);
                            paramNv_[numserv_].parametresNAV(coeffNav_[j]);
                            nv_[numserv_].calcul(cx_[numserv_]);
                            final IResultatsNavmer resultsNv = IResultatsNavmerHelper.narrow(nv_[numserv_].resultats(cx_[numserv_]));
                            if (resultsNv == null) {
                                System.out.println(" resultsNv null...");
                            }
                            res_[j] = resultsNv.resultatsDAT();
                            mobiles_[j].id = j;
                            mobiles_[j].x = (res_[j].etats[res_[j].etats.length - 1]).cinematique.x;
                            mobiles_[j].y = (res_[j].etats[res_[j].etats.length - 1]).cinematique.y;
                            mobiles_[j].z = 0.;
                            mobiles_[j].ax = 0.;
                            mobiles_[j].ay = 0.;
                            mobiles_[j].az = (res_[j].etats[res_[j].etats.length - 1]).cinematique.cap;
                            mobiles_[j].vx = (res_[j].etats[res_[j].etats.length - 1]).cinematique.vitesseLongitudinale;
                            mobiles_[j].vy = (res_[j].etats[res_[j].etats.length - 1]).cinematique.vitesseLaterale;
                            mobiles_[j].vz = (res_[j].etats[res_[j].etats.length - 1]).cinematique.vitesseRotationnelle;
                            mobiles_[j].vrx = 0.;
                            mobiles_[j].vry = 0.;
                            mobiles_[j].vrz = 0.;
                            paramINI_[j].entete.etatInitial = (res_[j].etats[res_[j].etats.length - 1]);
                        }
                        System.err.println("\n********");
                    }
                };
                threads[numserv].start();
            }
            nb = nbtmp;
            try {
                for (i = 0; i < threads.length; i++) {
                    threads[i].join();
                }
            } catch (final InterruptedException _e) {
                _e.printStackTrace();
            }
            final int delay = (int) System.currentTimeMillis() - tempsavantcalcul;
            if (delay < periode_ * 1000) {
                try {
                    Thread.sleep(periode_ * 1000 - delay);
                } catch (final InterruptedException _e) {
                }
            }
        }
    }

    /**
   * Cette fonction charge un scenario et renvoie un tableau de mobiles gener� � partir du fichier Format du fichier n
   * x1,y1,z1,cap1,vx1,vy1,vz1,vrx1,vry1,vrz1 .... xn,yn,zn,capn,vxn,vyn,vzn,vrxn,vryn,vrzn
   * 
   * @param _nomfic
   */
    SMobile[] chargeScenario(final String _nomfic) {
        SMobile[] m = null;
        try {
            String chaine;
            final BufferedReader fluxlu = new BufferedReader(new FileReader(_nomfic));
            chaine = fluxlu.readLine();
            nbMobiles_ = new Double(chaine).intValue();
            m = new SMobile[nbMobiles_];
            final String[] chemins = new String[nbMobiles_];
            for (int i = 0; i < nbMobiles_; i++) {
                chemins[i] = fluxlu.readLine();
                chaine = fluxlu.readLine();
                final StringTokenizer st = new StringTokenizer(chaine, CtuluLibString.VIR);
                while (st.hasMoreTokens()) {
                    final int id = i;
                    final int rep = (new Double(st.nextToken())).intValue();
                    final double x = (new Double(st.nextToken())).doubleValue();
                    final double y = (new Double(st.nextToken())).doubleValue();
                    final double z = (new Double(st.nextToken())).doubleValue();
                    final double ax = (new Double(st.nextToken())).doubleValue();
                    final double ay = (new Double(st.nextToken())).doubleValue();
                    final double az = (new Double(st.nextToken())).doubleValue();
                    final double vx = (new Double(st.nextToken())).doubleValue();
                    final double vy = (new Double(st.nextToken())).doubleValue();
                    final double vz = (new Double(st.nextToken())).doubleValue();
                    final double vrx = (new Double(st.nextToken())).doubleValue();
                    final double vry = (new Double(st.nextToken())).doubleValue();
                    final double vrz = (new Double(st.nextToken())).doubleValue();
                    m[i] = new SMobile(id, rep, x, y, z, ax, ay, az, vx, vy, vz, vrx, vry, vrz);
                }
            }
            setCoeffNav(chemins);
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        return m;
    }

    /**
   * Genere un SEtatNavire.
   */
    private static SEtatNavire genereEtatInitial(final int _instant, final double _x, final double _y, final double _cap, final double _vx, final double _vy, final double _vr) {
        final SEtatNavire etatini = new SEtatNavire();
        etatini.cinematique = new SCinematiqueNavire(_instant, _x, _y, _cap, _vx, _vy, _vr);
        etatini.ordre = new SOrdreNavire(0., .1, .1, 1., 1., 0., 0., 0., 0.);
        etatini.commande = new SCommandeNavire(0., .1, .1, 1., 1., 0., 0., 0., 0.);
        etatini.environnement = new SEnvironnement(0., 0., 10000., 0., 0., 0., 1000., 1000., false);
        etatini.ordreRemorqueurs = new SOrdreRemorqueur[nombreRemorqueurs.value];
        etatini.commandeRemorqueurs = new SCommandeRemorqueur[nombreRemorqueurs.value];
        for (int i = 0; i < nombreRemorqueurs.value; i++) {
            etatini.ordreRemorqueurs[i] = new SOrdreRemorqueur();
            etatini.commandeRemorqueurs[i] = new SCommandeRemorqueur();
            etatini.ordreRemorqueurs[i].position = 0;
            etatini.ordreRemorqueurs[i].angle = 0;
            etatini.ordreRemorqueurs[i].force = 0;
            etatini.commandeRemorqueurs[i].position = 0;
            etatini.commandeRemorqueurs[i].angle = 0;
            etatini.commandeRemorqueurs[i].force = 0;
        }
        return etatini;
    }

    /**
   * On d�rive de Thread afin de pouvoir acceder � certaines variables pass�es au constructeur � l'interieur du thread.
   * 
   * @author deniger
   * @version $Revision: 1.9 $
   */
    private class simboatThread extends Thread {

        public int i_;

        public int nb_;

        public int numserv_;

        public IParametresNavmer[] paramNv_;

        public ICalculNavmer[] nv_;

        public IConnexion[] cx_;

        public SResultatsDAT[] res_;

        public simboatThread(final int _i, final int _nb, final int _numserv, final IParametresNavmer[] _paramsNv, final ICalculNavmer[] _nv, final IConnexion[] _cx, final SResultatsDAT[] _res) {
            i_ = _i;
            nb_ = _nb;
            numserv_ = _numserv;
            paramNv_ = _paramsNv;
            nv_ = _nv;
            cx_ = _cx;
            res_ = _res;
        }
    }

    /**
   * @see org.fudaa.dodico.corba.simboat.ISimulateurSimboatOperations#utilisateur()
   */
    public IPersonne utilisateur() {
        return personne_;
    }

    /**
   * @see org.fudaa.dodico.corba.simboat.ISimulateurSimboatOperations#utilisateur(org.fudaa.dodico.corba.objet.IPersonne)
   */
    public void utilisateur(final IPersonne _newUtilisateur) {
        setUtilisateur(_newUtilisateur);
    }
}
