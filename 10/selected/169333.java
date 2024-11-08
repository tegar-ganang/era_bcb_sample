package acgvision;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

/**
 *
 * @author  ACGCenter - Rémi Debay
 * @year    2008
 * @ide     netbeans 6.1 RC2
 */
public class accidents {

    private Database db;

    private Connection con = null;

    /**
     * Fonction permettant de générer l'historique, copie un champ de la table incidents
     * dans la table historique. Retour true si succès.
     * @param CodeErreur : code de l'erreur à copier
     * @return true si succès.
     */
    private Boolean CreerHistorique(String CodeErreur) {
        String RequeteSQL;
        Boolean retour = false;
        Statement statement = null;
        RequeteSQL = "INSERT INTO tbl_incident_historic " + "(inchis_err_numer, inchis_err_etat, inchis_esc_etap, inchis_err_date," + "inchis_typ_user,inchis_cde_user, inchis_usr_msg, inchis_niv_crimd, " + "inchis_res_res1, inchis_res_res2) " + "SELECT * FROM  tbl_incident_encours WHERE incenc_err_numer ='" + CodeErreur + "';";
        try {
            statement = con.createStatement();
            acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
            statement.executeUpdate(RequeteSQL);
            retour = true;
        } catch (SQLException ex) {
            acgtools_core.AcgIO.SortieLog(ex.getMessage());
            acgtools_core.AcgIO.SortieLog(new Date() + "Probléme lors de l'éxécution de la requète SQL :");
            acgtools_core.AcgIO.SortieLog(RequeteSQL);
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                acgtools_core.AcgIO.SortieLog(new Date() + "Problème lors de la fermeture de la connection à la base de données");
            }
            return retour;
        }
    }

    private Boolean UpdateEnCours(String CodeErreur, String EtatJob, String crimd, String dateErreur, String message, String res1) {
        String RequeteSQL;
        Boolean retour = false;
        ResultSet resultat = null;
        Statement statement = null;
        RequeteSQL = "UPDATE tbl_incident_encours " + "SET incenc_err_etat='" + EtatJob + "'," + " incenc_niv_crimd = '" + crimd + "', " + "incenc_err_date = '" + dateErreur + "', " + "incenc_typ_user='n', incenc_cde_user=1, incenc_err_msg='" + message + "', " + "incenc_res_res1='" + res1 + "' " + "WHERE incenc_err_numer ='" + CodeErreur + "';";
        try {
            statement = con.createStatement();
            acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
            statement.executeUpdate(RequeteSQL);
            this.CreerHistorique(CodeErreur);
            retour = true;
        } catch (SQLException ex) {
            acgtools_core.AcgIO.SortieLog(ex.getMessage());
            acgtools_core.AcgIO.SortieLog(new Date() + "Probléme lors de l'éxécution de la requète SQL :");
            acgtools_core.AcgIO.SortieLog(RequeteSQL);
        } finally {
            try {
                if (resultat != null) {
                    resultat.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                acgtools_core.AcgIO.SortieLog(new Date() + " - Problème lors de la fermeture de la connection à la base de données");
            }
            return retour;
        }
    }

    private Boolean CreerMaj(Boolean type) {
        acgtools_core.AcgIO.SortieLog(new Date() + "Exécution de la procédure créer maj.");
        Boolean retour = false;
        ResultSet resultat = null;
        Statement statement = null;
        String RequeteSQL = "";
        if (type) {
            RequeteSQL = "SELECT incmaj_inc_new FROM tbl_incident_maj";
        } else {
            RequeteSQL = "SELECT incmaj_inc_maj FROM tbl_incident_maj";
        }
        try {
            statement = con.createStatement();
            acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
            resultat = statement.executeQuery(RequeteSQL);
            if (resultat.next()) {
                int valeur = resultat.getInt(1);
                valeur++;
                if (type) {
                    RequeteSQL = "UPDATE tbl_incident_maj SET incmaj_inc_new='" + valeur + "'; ";
                } else {
                    RequeteSQL = "UPDATE tbl_incident_maj SET incmaj_inc_maj='" + valeur + "'; ";
                }
            } else {
                RequeteSQL = "INSERT INTO tbl_incident_maj " + "(incmaj_inc_new, incmaj_inc_maj, incmaj_inc_esc ) values (1,1,1)";
            }
            acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
            statement.executeUpdate(RequeteSQL);
        } catch (SQLException ex) {
            acgtools_core.AcgIO.SortieLog(ex.getMessage());
            acgtools_core.AcgIO.SortieLog(new Date() + "Probléme lors de l'éxécution de la requète SQL :");
            acgtools_core.AcgIO.SortieLog(RequeteSQL);
        } finally {
            try {
                if (resultat != null) {
                    resultat.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                acgtools_core.AcgIO.SortieLog(new Date() + "Problème lors de la fermeture de la connection à la base de données");
            }
            return retour;
        }
    }

    /**
     * Procédure de création d'un bloc
     * @param numero - numéro temporaire du serveur
     * @return numéro du bloc nouvellement créé, 0 si problème
     * @throws java.sql.SQLException
     * @throws java.lang.ClassNotFoundException
     */
    private long CreationBloc(long NumeroServeur) throws SQLException, ClassNotFoundException {
        Statement statement;
        ResultSet resultat;
        String RequeteSQL;
        statement = con.createStatement();
        acgtools_core.AcgIO.SortieLog(new Date() + " - l'identifiant de bloc est 0, on en crée un nouveau");
        RequeteSQL = "INSERT INTO tbl_donnees_job " + "VALUES (" + NumeroServeur + ",'Referencement', 'ref', 0,'tbl_donnees_pilotage','donpil_cde_pilot','f','')";
        acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
        statement.executeUpdate(RequeteSQL);
        acgtools_core.AcgIO.SortieLog(new Date() + " - Création du bloc avec le job précédemment créé du bloc ");
        RequeteSQL = "INSERT INTO tbl_donnees_bloc " + "(donblo_typ_bloc,donblo_cde_job)  " + "SELECT 'rf', donjob_cde_job FROM tbl_donnees_job WHERE donjob_cde_srv = '" + NumeroServeur + "' " + "AND donjob_typ_trait ='ref';";
        acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
        statement.executeUpdate(RequeteSQL);
        RequeteSQL = "SELECT bloc.donblo_cde_bloc AS cdebloc " + "FROM tbl_donnees_bloc AS bloc, " + "tbl_donnees_job AS job " + "WHERE bloc.donblo_cde_job=job.donjob_cde_job " + "AND job.donjob_cde_srv='" + NumeroServeur + "'" + "AND job.donjob_typ_trait='ref';";
        acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
        resultat = statement.executeQuery(RequeteSQL);
        if (resultat.next()) {
            return resultat.getLong("cdebloc");
        } else {
            acgtools_core.AcgIO.SortieLog(new Date() + " - Un problème est survenu lors de l'éxécution, l'enregistrement de l'incident est abandonné");
            return 0;
        }
    }

    /**
     * Effectue tous les traitements nécessaires à la création d'un nouvel incident
     * @param idbloc
     * @param datebloc
     * @param Etatbloc
     * @param idServeur numéro du serveur + nombre d'incidents
     * @param niveau
     * @param message
     * @return true si aucun problemes
     */
    protected Boolean lancerincident(long idbloc, String Etatbloc, java.util.GregorianCalendar datebloc, long idServeur, String niveau, String message) {
        String codeerr;
        Boolean retour = false;
        Boolean SauvegardeEtatAutocommit;
        int etat;
        acgtools_core.AcgIO.SortieLog(new Date() + " - Appel de la fonction Lancer incident");
        Statement statement = null;
        ResultSet resultat = null;
        String RequeteSQL = "";
        acgtools_core.AcgIO.SortieLog(new Date() + " - nouvel incident pour le bloc : " + acgtools_core.AcgIO.RetourneDate(datebloc));
        try {
            this.con = db.OpenConnection();
            SauvegardeEtatAutocommit = this.con.getAutoCommit();
            this.con.setAutoCommit(false);
            if (idbloc == 0) {
                idbloc = this.CreationBloc(idServeur);
                if (idbloc == 0) {
                    retour = false;
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Problème lors de la création du bloc");
                    this.con.rollback();
                    this.con.close();
                    return false;
                }
            }
            acgtools_core.AcgIO.SortieLog(new Date() + " - bloc : " + idbloc);
            etat = this.ChargerEtatServeur(idbloc, datebloc);
            if (etat != 2) {
                statement = con.createStatement();
                acgtools_core.AcgIO.SortieLog(new Date() + " - Etat chargé");
                RequeteSQL = "SELECT incref_err_numer FROM tbl_incident_ref " + "WHERE incref_cde_job ='" + idbloc + "' " + "AND incref_err_numer NOT IN " + "(SELECT incref_err_numer FROM tbl_incident_ref " + "WHERE incref_err_etat='c') " + "AND incref_err_numer NOT IN " + "(SELECT incenc_err_numer FROM tbl_incident_encours " + "WHERE incenc_err_etat='c') ;";
                acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
                resultat = statement.executeQuery(RequeteSQL);
                if (!resultat.next()) {
                    resultat.close();
                    RequeteSQL = "INSERT INTO tbl_incident_ref " + "(incref_cde_job,incref_err_date,incref_err_etat,incref_niv_crimd,incref_err_msg,incref_err_srvnm)" + "VALUES ('" + idbloc + "','" + acgtools_core.AcgIO.RetourneDate(datebloc) + "','" + Etatbloc + "','" + niveau + "','" + message + "','" + idServeur + "');";
                    acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
                    statement.executeUpdate(RequeteSQL);
                    RequeteSQL = "SELECT incref_err_numer FROM tbl_incident_ref " + "WHERE incref_cde_job = '" + idbloc + "' " + "AND incref_err_srvnm = '" + idServeur + "' " + "AND incref_err_date = '" + acgtools_core.AcgIO.RetourneDate(datebloc) + "';";
                    acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
                    resultat = statement.executeQuery(RequeteSQL);
                    if (resultat.next()) {
                        codeerr = resultat.getString("incref_err_numer");
                        resultat.close();
                        RequeteSQL = "INSERT INTO tbl_incident_encours" + "(incenc_err_numer, incenc_err_etat, incenc_esc_etap, " + "incenc_err_date, incenc_typ_user,incenc_cde_user,incenc_err_msg,incenc_niv_crimd) " + "VALUES ('" + codeerr + "','" + Etatbloc + "',0, " + "'" + acgtools_core.AcgIO.RetourneDate(datebloc) + "','n',0,'" + message + "','" + niveau + "');";
                        acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
                        statement.executeUpdate(RequeteSQL);
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Incident inséré dans la base de données");
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Traitement de l'envois des emails si nécessaire");
                        this.usermail(codeerr, etat, acgtools_core.AcgIO.RetourneDate(datebloc), message);
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Création de l'historique");
                        this.CreerHistorique(codeerr);
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Créer maj");
                        this.CreerMaj(true);
                        retour = true;
                    } else {
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Problème d'insertion du nouvel incident dans la base");
                        retour = false;
                    }
                } else {
                    codeerr = resultat.getString("incref_err_numer");
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Numéro de l'erreur trouvé. Numéro =" + codeerr);
                    RequeteSQL = "SELECT incenc_err_etat FROM tbl_incident_encours " + "WHERE incenc_err_numer='" + codeerr + "';";
                    acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
                    resultat = statement.executeQuery(RequeteSQL);
                    if (!resultat.next()) {
                        resultat.close();
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Problème lors de la lecture de l'état de l'incident.");
                        String RequeteSQLInsert = "INSERT INTO tbl_incident_encours" + "(incenc_err_numer, incenc_err_etat, incenc_esc_etap, " + "incenc_err_date, incenc_typ_user,incenc_cde_user,incenc_err_msg,incenc_niv_crimd) " + "VALUES ('" + codeerr + "','" + Etatbloc + "',0, " + "'" + acgtools_core.AcgIO.RetourneDate(datebloc) + "','n',0,'" + "Incident non clotur&eacute; - " + message + "','" + niveau + "');";
                        acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQLInsert);
                        statement.execute(RequeteSQLInsert);
                        resultat = statement.executeQuery(RequeteSQL);
                    } else {
                        resultat = statement.executeQuery(RequeteSQL);
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Incident correctement positionné dans encours");
                    }
                    if (resultat.next()) {
                        switch(Etatbloc.charAt(0)) {
                            case 'c':
                                {
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - Cloture de l'incident.");
                                    RequeteSQL = "UPDATE tbl_incident_ref SET incref_err_etat='c'" + "WHERE incref_err_numer='" + codeerr + "';";
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
                                    statement.executeUpdate(RequeteSQL);
                                    this.UpdateEnCours(codeerr, "c", niveau, acgtools_core.AcgIO.RetourneDate(datebloc), message, "auto");
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - Traitement de l'envois des emails si nécessaire");
                                    this.usermail(codeerr, etat, message, acgtools_core.AcgIO.RetourneDate(datebloc));
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - Créer maj");
                                    this.CreerMaj(false);
                                    retour = true;
                                    break;
                                }
                            case 'm':
                                {
                                    this.UpdateEnCours(codeerr, "m", niveau, acgtools_core.AcgIO.RetourneDate(datebloc), message, "auto");
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - Traitement de l'envois des emails si nécessaire");
                                    this.usermail(codeerr, etat, message, acgtools_core.AcgIO.RetourneDate(datebloc));
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - Créer maj");
                                    this.CreerMaj(false);
                                    retour = true;
                                    break;
                                }
                            default:
                                {
                                    this.UpdateEnCours(codeerr, "m", niveau, acgtools_core.AcgIO.RetourneDate(datebloc), message, "");
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - Traitement de l'envois des emails si nécessaire");
                                    this.usermail(codeerr, etat, message, acgtools_core.AcgIO.RetourneDate(datebloc));
                                    acgtools_core.AcgIO.SortieLog(new Date() + " - Créer maj");
                                    this.CreerMaj(false);
                                    retour = true;
                                    break;
                                }
                        }
                    } else {
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Problème lors de la lecture de l'état de l'incident.");
                        retour = false;
                    }
                }
            } else {
                acgtools_core.AcgIO.SortieLog(new Date() + " - Systeme en maintenance, pas de remontée d'incidents.");
                retour = false;
            }
        } catch (ClassNotFoundException ex) {
            acgtools_core.AcgIO.SortieLog(new Date() + "Annulation des modifications.");
            con.rollback();
            acgtools_core.AcgIO.SortieLog(new Date() + "Probléme lors de l'éxécution de la connexion.");
            acgtools_core.AcgIO.SortieLog(ex.getMessage());
            retour = false;
        } catch (SQLException ex) {
            acgtools_core.AcgIO.SortieLog(new Date() + "Annulation des modifications.");
            con.rollback();
            acgtools_core.AcgIO.SortieLog(ex.getMessage());
            acgtools_core.AcgIO.SortieLog(new Date() + "Probléme lors de l'éxécution de la requète SQL :");
            acgtools_core.AcgIO.SortieLog(RequeteSQL);
            retour = false;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (retour) {
                    con.commit();
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Création de l'incident : succès");
                } else {
                    con.rollback();
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Création de l'incident : echec");
                }
                if (con != null) {
                    con.close();
                }
            } catch (Exception e) {
                acgtools_core.AcgIO.SortieLog(new Date() + "Problème lors de la fermeture de la connection à la base de données");
            }
            return retour;
        }
    }

    /**
     * 
     * @param idjob
     * @param dateetat   
     * @return 2 si le serveur n'est pas en astreinte
     */
    private int ChargerEtatServeur(long idjob, java.util.GregorianCalendar dateetat) {
        acgtools_core.AcgIO.SortieLog(new Date() + " - ChargeerEtatServeur ");
        acgtools_core.AcgIO.SortieLog(new Date() + " - Job : " + idjob);
        String RequeteSQL = "SELECT donser_eta_srv, donser_nom_srv FROM tbl_donnees_serveur " + "WHERE donser_cde_srv = (SELECT donjob_cde_srv FROM tbl_donnees_job, tbl_donnees_bloc " + "   WHERE tbl_donnees_job.donjob_cde_job = tbl_donnees_bloc.donblo_cde_job AND tbl_donnees_bloc.donblo_cde_bloc=" + idjob + ");";
        Statement statement = null;
        ResultSet resultat = null;
        int retour = -1;
        try {
            statement = con.createStatement();
            acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
            resultat = statement.executeQuery(RequeteSQL);
            if (resultat.next()) {
                if (resultat.getString("donser_eta_srv").equals("f")) {
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Le job " + idjob + " est sur un serveur en maintenance");
                    resultat.close();
                    statement.close();
                    return 2;
                }
            }
            resultat.close();
            acgtools_core.AcgIO.SortieLog(new Date() + " - Le serveur n'est pas répertorié en maintenance, vérifications de ses paramètres.");
            int numsemaine = new Integer(dateetat.get(java.util.GregorianCalendar.WEEK_OF_YEAR));
            int joursemaine = new Integer(dateetat.get(java.util.GregorianCalendar.DAY_OF_WEEK));
            joursemaine--;
            if (joursemaine == 0) joursemaine = 7;
            int nbheures = new Integer(dateetat.get(java.util.GregorianCalendar.HOUR_OF_DAY));
            int nbminute = new Integer(dateetat.get(java.util.GregorianCalendar.MINUTE));
            int nbannee = new Integer(dateetat.get(java.util.GregorianCalendar.YEAR));
            int nbquart = nbheures * 4 + (nbminute % 15);
            acgtools_core.AcgIO.SortieLog("heure : " + nbheures + " : " + nbminute);
            acgtools_core.AcgIO.SortieLog("numero semaine :" + numsemaine);
            acgtools_core.AcgIO.SortieLog("jour de la semaine :" + joursemaine);
            acgtools_core.AcgIO.SortieLog("numéro de quart :" + nbquart);
            acgtools_core.AcgIO.SortieLog("année :" + nbannee);
            RequeteSQL = "SELECT * FROM tbl_horaires_serveur WHERE " + "horser_cde_srv = (SELECT donjob_cde_srv FROM tbl_donnees_job, tbl_donnees_bloc " + "   WHERE tbl_donnees_job.donjob_cde_job = tbl_donnees_bloc.donblo_cde_job AND tbl_donnees_bloc.donblo_cde_bloc=" + idjob + ") " + "AND horser_num_sem = '" + numsemaine + "' AND horser_num_annee='" + nbannee + "';";
            acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
            resultat = statement.executeQuery(RequeteSQL);
            if (resultat.next()) {
                if (resultat.getString(("horser_tbl_ast" + joursemaine)).charAt(nbquart) == '1') {
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Systeme en astreinte");
                    retour = 1;
                } else {
                    if (resultat.getString(("horser_tbl_mai" + joursemaine)).charAt(nbquart) == '1') {
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Systeme en maintenance");
                        retour = 2;
                    } else {
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Systeme en fonctionnement normal");
                        retour = 0;
                    }
                }
                resultat.close();
                statement.close();
                return retour;
            } else {
                acgtools_core.AcgIO.SortieLog(new Date() + " - Utilisation du masque.");
                RequeteSQL = "SELECT * FROM tbl_horaires_masque ;";
                acgtools_core.AcgIO.SortieLog(new Date() + " - " + RequeteSQL);
                resultat = statement.executeQuery(RequeteSQL);
                if (resultat.next()) {
                    if (resultat.getString(("hormas_tbl_ast" + joursemaine)).charAt(nbquart) == '1') {
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Systeme en astreinte");
                        retour = 1;
                    } else {
                        if (resultat.getString(("hormas_tbl_mai" + joursemaine)).charAt(nbquart) == '1') {
                            acgtools_core.AcgIO.SortieLog(new Date() + " - Systeme en maintenance");
                            retour = 2;
                        } else {
                            acgtools_core.AcgIO.SortieLog(new Date() + " - Systeme en fonctionnement normal");
                            retour = 0;
                        }
                    }
                    resultat.close();
                    statement.close();
                    return retour;
                }
            }
            retour = 0;
        } catch (SQLException ex) {
            acgtools_core.AcgIO.SortieLog(ex.getMessage());
            acgtools_core.AcgIO.SortieLog(new Date() + "Probléme lors de l'éxécution de la requète SQL :");
            acgtools_core.AcgIO.SortieLog(RequeteSQL);
        } finally {
            try {
                if (resultat != null) {
                    resultat.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                acgtools_core.AcgIO.SortieLog(new Date() + "Problème lors de la fermeture de la connection à la base de données");
            }
            return retour;
        }
    }

    /**
     * Appel les scripts nécessaires à envoyer des mails. Donne en argument le nécessaire
     * à l'acheminement du mai.
     * @param mail1 : addresse email
     * @param mail2 : adresse email
     * @param texto : message du texto
     * @param tel1 : numéro pour le texto
     * @param tel2 : numéro pour le texto
     * @param sms : valeur = t si serveur en abstreinte
     * @param etatserveur
     * @param message
     * @param DateMessage
     * @param serveur
     * @param errnumer
     * @param niveau
     * @param etat
     */
    private void envoyermail(String mail1, String mail2, String texto, String tel1, String tel2, String sms, int etatserveur, String message, String DateMessage, String serveur, String errnumer, String niveau, String etat) {
        acgtools_core.AcgIO.SortieLog(new Date() + " - Appel de la procédure Envoyer Mail.");
        String Commande;
        String RepertoireScripts = db.getScripts() + java.io.File.separator;
        String SCRIPTTEXTO = RepertoireScripts + "acg_texto.sh";
        String SCRIPTMAIL = RepertoireScripts + "acg_mail.sh";
        String Os = acgtools_core.AcgIO.Informations_OS();
        String TitreSMS = "Srv : " + serveur;
        String ChaineSMS = "C:" + niveau + " - " + message;
        String ChaineEnvoi = "Noyau ACGVISION \n " + "Incident N " + errnumer + " \n " + "Serveur : " + serveur + " \n " + "Criticite : " + niveau + " \n " + "Etat de l'incident : " + etat + " \n " + "-----------------------------------------\n " + DateMessage + " : " + message;
        String[] args = new String[4];
        if (etatserveur == 0) {
            if (sms.equals("t")) {
                if (!texto.isEmpty()) {
                    args[0] = SCRIPTTEXTO;
                    args[1] = texto;
                    args[2] = TitreSMS;
                    args[3] = ChaineSMS;
                    Commande = args[0] + " " + args[1] + " '" + args[2] + "' '" + args[3] + "' ";
                    acgtools_core.AcgIO.SortieLog(new Date() + " - " + Commande);
                    acgtools_core.AcgExec.Executer(args);
                }
            }
            if (!mail1.isEmpty()) {
                args[0] = SCRIPTMAIL;
                args[1] = mail1;
                args[2] = "Message du Noyau ACGVision";
                args[3] = ChaineEnvoi;
                Commande = args[0] + " " + args[1] + " '" + args[2] + "' '" + args[3] + "' ";
                acgtools_core.AcgIO.SortieLog(new Date() + " - " + Commande);
                acgtools_core.AcgExec.Executer(args);
            }
        } else {
            if (!texto.isEmpty()) {
                args[0] = SCRIPTTEXTO;
                args[1] = texto;
                args[2] = TitreSMS;
                args[3] = ChaineSMS;
                Commande = args[0] + " " + args[1] + " '" + args[2] + "' '" + args[3] + "' ";
                acgtools_core.AcgIO.SortieLog(new Date() + " - " + Commande);
                acgtools_core.AcgExec.Executer(args);
            }
            if (!mail2.isEmpty()) {
                args[0] = SCRIPTMAIL;
                args[1] = mail2;
                args[2] = "Message du Noyau ACGVision";
                args[3] = ChaineEnvoi;
                Commande = args[0] + " " + args[1] + " '" + args[2] + "' '" + args[3] + "' ";
                acgtools_core.AcgIO.SortieLog(new Date() + " - " + Commande);
                acgtools_core.AcgExec.Executer(args);
            } else {
                if (!mail1.isEmpty()) {
                    args[0] = SCRIPTMAIL;
                    args[1] = mail1;
                    args[2] = "Message du Noyau ACGVision";
                    args[3] = ChaineEnvoi;
                    Commande = args[0] + " " + args[1] + " '" + args[2] + "' '" + args[3] + "' ";
                    acgtools_core.AcgIO.SortieLog(new Date() + " - " + Commande);
                    acgtools_core.AcgExec.Executer(args);
                }
            }
        }
        acgtools_core.AcgIO.SortieLog(new Date() + " - Fin du traitement des emails");
    }

    /**
     * Cette méthode vérifie la nécessité d'envoyer le mail par rapport aux paramètres
     * enregistrés dans la table mail_jenvoi
     * @param cdeerrnumer
     * @param etatserveur
     * @param message
     * @param datemessagee
     */
    private void usermail(String cdeerrnumer, int etatserveur, String message, String datemessage) {
        String Table, Champ, Code, Niveau;
        String Serveur, incetat;
        Boolean envoye = false;
        acgtools_core.AcgIO.SortieLog(new Date() + " - Appel de la méthode usermail.");
        String RequeteSQL = "SELECT donjob_tbl_trait, donjob_tbl_champ, donjob_cde_trait," + " incenc_niv_crimd, donser_nom_srv, incenc_err_etat " + " FROM vue_info_incident WHERE incref_err_numer= " + cdeerrnumer;
        ResultSet resultat = null;
        Statement statement = null;
        acgtools_core.AcgIO.SortieLog(new Date() + " - usermail-Etat du serveur : " + etatserveur);
        ResultSet resultatboucle = null;
        Statement statementboucle = null;
        try {
            statement = con.createStatement();
            acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
            resultat = statement.executeQuery(RequeteSQL);
            if (resultat.next()) {
                Table = resultat.getString("donjob_tbl_trait");
                Champ = resultat.getString("donjob_tbl_champ");
                Code = resultat.getString("donjob_cde_trait");
                Niveau = resultat.getString("incenc_niv_crimd");
                Serveur = resultat.getString("donser_nom_srv");
                incetat = resultat.getString("incenc_err_etat");
                resultat.close();
                if (etatserveur != 1) {
                    RequeteSQL = "SELECT * FROM tbl_mail_jenvoi WHERE maijen_typ_inc ='" + Niveau + "' " + "AND maijen_tbl_nom='" + Table + "' " + "AND maijen_tbl_champ ='" + Champ + "' " + "AND maijen_tbl_val='" + Code + "';";
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
                    resultat = statement.executeQuery(RequeteSQL);
                    String TypeUser;
                    statementboucle = con.createStatement();
                    while (resultat.next()) {
                        envoye = true;
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Traitement d'envois");
                        TypeUser = resultat.getString("maijen_typ_userx");
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Traitement d'envois de type :" + TypeUser);
                        if (TypeUser.equals("g") || TypeUser.equals("u")) {
                            RequeteSQL = "SELECT donuse_usr_mail1," + "donuse_usr_mail2,donuse_usr_texto, " + "donuse_usr_tel1, donuse_usr_tel2, " + "donuse_usr_texto, donuse_usr_nom " + "FROM tbl_donnees_user " + " WHERE donuse_cde_user in (" + "   SELECT grouse_cde_user FROM tbl_groupe_user " + "   WHERE grouse_cde_group=" + resultat.getString("maijen_cde_userx") + ") " + " OR donuse_cde_user = " + resultat.getString("maijen_cde_userx");
                            acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
                            resultatboucle = statementboucle.executeQuery(RequeteSQL);
                            while (resultatboucle.next()) {
                                acgtools_core.AcgIO.SortieLog(new Date() + " - Envois d'un email.");
                                envoyermail(resultatboucle.getString("donuse_usr_mail1"), resultatboucle.getString("donuse_usr_mail2"), resultatboucle.getString("donuse_usr_texto"), resultatboucle.getString("donuse_usr_tel1"), resultatboucle.getString("donuse_usr_tel2"), resultat.getString("maijen_env_vect"), etatserveur, message, datemessage, Serveur, cdeerrnumer, Niveau, incetat);
                            }
                        } else {
                            acgtools_core.AcgIO.SortieLog(new Date() + " - Envois d'un email.");
                            envoyermail(resultat.getString("maijen_adr_mail"), "", "", "", "", "", etatserveur, message, datemessage, Serveur, cdeerrnumer, Niveau, incetat);
                        }
                    }
                } else {
                    RequeteSQL = "SELECT donuse_usr_mail1," + "donuse_usr_mail2,donuse_usr_texto, " + "donuse_usr_tel1, donuse_usr_tel2, " + "donuse_usr_texto, donuse_usr_nom " + "FROM tbl_donnees_user " + " WHERE donuse_cde_user in (" + "   SELECT grouse_cde_user FROM tbl_groupe_user " + "   WHERE grouse_cde_group IN " + "       (SELECT dongro_cde_group " + "       FROM tbl_donnees_groupe " + "       WHERE dongro_grp_nom='Astreinte'));";
                    acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
                    statementboucle = con.createStatement();
                    resultatboucle = statementboucle.executeQuery(RequeteSQL);
                    while (resultatboucle.next()) {
                        acgtools_core.AcgIO.SortieLog(new Date() + " - Envois d'un email.");
                        envoyermail(resultatboucle.getString("donuse_usr_mail1"), resultatboucle.getString("donuse_usr_mail2"), resultatboucle.getString("donuse_usr_texto"), resultatboucle.getString("donuse_usr_tel1"), resultatboucle.getString("donuse_usr_tel2"), "", etatserveur, message, datemessage, Serveur, cdeerrnumer, Niveau, incetat);
                    }
                }
            }
        } catch (SQLException ex) {
            acgtools_core.AcgIO.SortieLog(ex.getMessage());
            acgtools_core.AcgIO.SortieLog(new Date() + "Probléme lors de l'éxécution de la requète SQL :");
            acgtools_core.AcgIO.SortieLog(RequeteSQL);
        } finally {
            try {
                if (resultat != null) {
                    resultat.close();
                }
                if (statement != null) {
                    statement.close();
                }
            } catch (Exception e) {
                acgtools_core.AcgIO.SortieLog(new Date() + " - Problème lors de la fermeture de la connection à la base de données");
            }
        }
    }

    /**
     * Renvoi le numéro de la semaine pour la date en argument
     * @param date dont on veu connaitre le numéro de semaine
     * @return numéro de la semaine, -1 si probleme
     * @throws java.lang.ClassNotFoundException
     * @throws java.sql.SQLException
     */
    public int Retourne_Numerosemaine(String date) throws SQLException {
        String RequeteSQL = "SELECT calcul_date('" + date + "'::timestamp)";
        acgtools_core.AcgIO.SortieLog(new Date() + " - Execution de la requete : " + RequeteSQL);
        Statement statement = con.createStatement();
        ResultSet rs = statement.executeQuery(RequeteSQL);
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            return -1;
        }
    }

    public void setDb(Database db) {
        this.db = db;
    }
}
