package nat;

import gestionnaires.GestionnaireErreur;
import java.awt.AWTKeyStroke;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Properties;
import nat.OptNames;
import nat.transcodeur.AmbiguityResolverUI;
import java.lang.Boolean;
import java.lang.Integer;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import outils.FileToolKit;
import outils.HyphenationToolkit;
import outils.regles.RulesToolKit;
import ui.AmbiguityResolverGUI;

/**
 * Cette classe contient l'ensemble des paramètres de configuration de NAT et
 * gère la sauvegarde et la récupération des différentes configurations
 * @author Bruno Mascret
 */
public class ConfigNat implements Serializable {

    /** Filter Properties */
    private Properties fiConf;

    /** UI Properties */
    private Properties uiConf;

    /** UI configuration filename */
    private static final String uiConfFilename = "nat-gui.conf";

    /** filters (conf files) directory */
    private static final String dirFilters = "filters/";

    /** name of the file containing scenarii data (steps, names...)*/
    private static final String scenFileName = "scenarii.cfg";

    /** tmp directory */
    private static final String dirTmp = "tmp/";

    /** text directory */
    private static final String dirText = "/text/";

    /** tables braille user */
    private static final String dirTablesBraille = "tablesBraille/";

    /** tables embosseuses user */
    private static final String dirTablesEmboss = "tablesEmbosseuse/";

    /** vrai si nat est en mode gui */
    private static boolean gui = false;

    /** serial version UID */
    private static final long serialVersionUID = 1L;

    /** constante pour représenter le mode garder toutes les lignes vides */
    public static final int AllEmptyLinesMode = -1;

    /** constante pour représenter le mode supprimer toutes les lignes vides */
    public static final int NoEmptyLinesMode = 0;

    /** constante pour représenter le mode de gestion paramétrée des lignes vides */
    public static final int ParametricEmptyLinesMode = 1;

    /** constante contenant l'adresse du dico de coupure par défaut */
    private static final String dicoCoupDefautName = ConfigNat.getInstallFolder() + "xsl/dicts/hyph_fr_nat.dic";

    /** Le fichier contenant la configuration */
    private String fichierConf;

    /** Numéro de version */
    private static final String version = "2.0-rc7-scénario";

    /** Nom de version long */
    private static final String versionLong = "2.0-rc-7-scénario (svn r2021)";

    /** Nom de la version svn correspondante */
    private static final int svnVersion = 2021;

    /** adresse du dernier fichier source utilisé */
    private String fichNoir = "";

    /** adresse du dernier fichier cible (sortie) utilisé */
    private String fichBraille = "";

    /** encoding du fichier source */
    private String sourceEncoding = "automatique";

    /** encoding du fichier de sortie */
    private String sortieEncoding = "UTF-8";

    /** l'instance singleton de CbbonfigNat*/
    private static ConfigNat cn = null;

    /**
     * Constructor
     * Creates user-specific configuration folders
     */
    private ConfigNat() {
    }

    /**
     * @return the path of the system-wide configuration filters folder
     */
    public static String getSystemConfigFilterFolder() {
        String scf = getInstallFolder() + "configurations/";
        return scf;
    }

    /**
     * @return the path of the user-specific configuration folder 
     */
    public static String getUserConfigFolder() {
        return FileToolKit.getSysDepPath(ConfigNat.getWorkingDir());
    }

    /**
     * @return the path of the file containing scenarii steps data
     */
    public static String getScenariiFile() {
        return getUserConfigFolder() + scenFileName;
    }

    /**
     * @return the path of the user-specific configuration filters folder 
     */
    public static String getUserConfigFilterFolder() {
        return getUserConfigFolder() + dirFilters;
    }

    /**
     * @return the path of the user-specific temporary folder 
     */
    public static String getUserTempFolder() {
        return getUserConfigFolder() + dirTmp;
    }

    /**
     * @return the path of the user-specific braille table folder 
     */
    public static String getUserBrailleTableFolder() {
        return getUserConfigFolder() + dirTablesBraille;
    }

    /**
     * @return the path of the user-specific embossing table folder 
     */
    public static String getUserEmbossTableFolder() {
        return getUserConfigFolder() + dirTablesEmboss;
    }

    /**
     * @return the path of install folder 
     */
    public static String getInstallFolder() {
        return FileToolKit.getSysDepPath("");
    }

    /**
     * @return the path of the temp config directory
     */
    public static String getConfTempFolder() {
        return ConfigNat.getUserTempFolder() + "confIO/";
    }

    /**
     * @return the path of the imported config directory
     */
    public static String getConfImportFolder() {
        return ConfigNat.getUserTempFolder() + "import/";
    }

    /**
     * @return the path of the text config directory
     */
    public static String getConfTextFolder() {
        return ConfigNat.getUserConfigFolder() + dirText;
    }

    /** nom par défaut du fichier temporaire tan */
    public static final String fichTmpTan = getUserTempFolder() + "nouveau.tan";

    /** @return {@link #versionLong} */
    public static String getVersionLong() {
        return versionLong;
    }

    /** @return {@link #version}*/
    public static String getVersion() {
        return version;
    }

    /** @return {@link #svnVersion}*/
    public static int getSvnVersion() {
        return svnVersion;
    }

    /** @return <p>le nom de la configuration suivante du scénario; si pas absolu, c'est une conf système;
     *  si "", c'est la dernière étape</p>*/
    public String getNextStep() {
        return fiConf.getProperty(OptNames.sc_next_step, "");
    }

    /** @param s l'adresse de l'étape suivante du scénario; si pas absolue, c'est une conf système */
    public void setNextStep(String s) {
        fiConf.setProperty(OptNames.sc_next_step, s);
    }

    /** @return le nom du scénario */
    public String getScenarioName() {
        return fiConf.getProperty(OptNames.sc_name, "");
    }

    /** @param sn le nom du scénario */
    public void setScenarioName(String sn) {
        fiConf.setProperty(OptNames.sc_name, sn);
    }

    /** @return true si vérification en ligne l'existence d'une nouvelle version */
    public boolean getUpdateCheck() {
        return new Boolean(fiConf.getProperty(OptNames.ge_check_update, "true")).booleanValue();
    }

    /** @param uc true si vérification en ligne l'existence d'une nouvelle version */
    public void setUpdateCheck(boolean uc) {
        fiConf.setProperty(OptNames.ge_check_update, Boolean.toString(uc));
    }

    /** @return true if translation mode is activated */
    public boolean getTranslationMode() {
        return new Boolean(fiConf.getProperty(OptNames.ge_translation_mode, "true")).booleanValue();
    }

    /** @param uc true to activate translation */
    public void setTranslationMode(boolean uc) {
        fiConf.setProperty(OptNames.ge_translation_mode, Boolean.toString(uc));
    }

    /** @return true : changes system files instead of custom */
    public boolean getChangeSysFiles() {
        return new Boolean(fiConf.getProperty(OptNames.ge_change_sys_files, "false")).booleanValue();
    }

    /** @param uc true to change system files instead of custom */
    public void setChangeSysFiles(boolean uc) {
        fiConf.setProperty(OptNames.ge_change_sys_files, Boolean.toString(uc));
    }

    /** @return {@link #fichNoir}*/
    public String getFichNoir() {
        return fichNoir;
    }

    /** @param fNoir valeur pour {@link #fichNoir}*/
    public void setFNoir(String fNoir) {
        fichNoir = fNoir;
    }

    /** @return {@link #fichBraille}*/
    public String getFBraille() {
        return fichBraille;
    }

    /** @param fc valeur pour {@link #fichBraille}*/
    public void setFBraille(String fc) {
        fichBraille = fc;
    }

    /** @param f valeur pour {@link #fichierConf}*/
    public void setFichierConf(String f) {
        fichierConf = f;
    }

    /** @return {@link #fichierConf}*/
    public String getFichierConf() {
        return fichierConf;
    }

    /** @return le nom court de la configuration courante, par exemple default */
    public String getShortFichierConf() {
        String name = (new File(fichierConf).getName());
        return name;
    }

    /** @return version de configuration  */
    public String getConfVersion() {
        return fiConf.getProperty(OptNames.conf_version, "0");
    }

    /** @param v version de configuration  */
    public void setConfVersion(String v) {
        fiConf.setProperty(OptNames.conf_version, v);
    }

    /** @return encodage du fichier noir*/
    public String getNoirEncoding() {
        return fiConf.getProperty(OptNames.en_in, "UTF-8");
    }

    /** @param se valeur pour l'encodage du fichier noir*/
    public void setNoirEncoding(String se) {
        fiConf.setProperty(OptNames.en_in, se);
    }

    /** @return encodage du fichier braille*/
    public String getBrailleEncoding() {
        return fiConf.getProperty(OptNames.en_out, "UTF-8");
    }

    /** @param se valeur pour l'encodage du fichier braille*/
    public void setBrailleEncoding(String se) {
        fiConf.setProperty(OptNames.en_out, se);
    }

    /** @param lg valeur pour le niveau de log*/
    public void setNiveauLog(int lg) {
        fiConf.setProperty(OptNames.ge_log_verbosity, (new Integer(lg)).toString());
    }

    /**
     * Change le niveau de log et répercute la modification au GestionnaireErreur ge
     * @param lg le nouveau niveau de log
     * @param ge le gestionnaire à mettre à jour
     */
    public void setNiveauLog(int lg, GestionnaireErreur ge) {
        fiConf.setProperty(OptNames.ge_log_verbosity, (new Integer(lg)).toString());
        if (ge != null) {
            ge.setNiveauLog(lg);
        }
    }

    /** @return le niveau de log*/
    public int getNiveauLog() {
        return ((new Integer(fiConf.getProperty(OptNames.ge_log_verbosity, "1"))).intValue());
    }

    /** Changes the choice of the screen reader 
     * If you pick blind-mode, all windows will be open full screen
     * @param sr : the new screen reader choice
     */
    public void setScrReader(int sr) {
        if (sr != 1) {
            setMaximizedPrincipal(true);
            setMaximizedOptions(true);
            setMaximizedEditeur(true);
        }
        fiConf.setProperty(OptNames.ge_sr_choice, (new Integer(sr)).toString());
    }

    /** @return the screen reader choice. The complete list can be found in outils\ScrReaders.txt
     * 1 : default (unknown or sighted person),
     * 2 : JAWS,
     * 3 : NVDA,
     * 4 : Window-Eyes.
     */
    public int getScrReader() {
        return ((new Integer(fiConf.getProperty(OptNames.ge_sr_choice, "1"))).intValue());
    }

    /** Changes the choice of the language
     * @param l : the new language
     */
    public void setLanguage(int l) {
        fiConf.setProperty(OptNames.ge_lang_choice, (new Integer(l)).toString());
    }

    /** @return the language choice*/
    public int getLanguage() {
        int retour = 0;
        try {
            retour = (new Integer(fiConf.getProperty(OptNames.ge_lang_choice, "0"))).intValue();
        } catch (NumberFormatException nbe) {
        }
        return retour;
    }

    /** @return adresse de la dtd */
    public String getDTD() {
        return (fiConf.getProperty(OptNames.fi_dtd_filename, getInstallFolder() + "xsl/mmlents/windob.dtd"));
    }

    /** @param dtd valeur pour adresse de la dtd*/
    public void setDTD(String dtd) {
        fiConf.setProperty(OptNames.fi_dtd_filename, dtd);
    }

    /** @return adresse de la feuille xsl principale de transcription*/
    public String getXSL() {
        return (fiConf.getProperty(OptNames.fi_filter_filename, getUserTempFolder() + "xsl.xsl"));
    }

    /** @param xslfn valeur pour l'adresse de la feuille xsl principale de transcription*/
    public void setXSL(String xslfn) {
        fiConf.setProperty(OptNames.fi_filter_filename, xslfn);
    }

    /** @return le nom de la table Braille utilisée*/
    public String getTableBraille() {
        return (fiConf.getProperty(OptNames.fi_braille_table, "brailleUTF8.ent"));
    }

    /**
     * Met à jour le nom de la table braille et copie la nouvelle table dans les
     * fichiers Brltab.ent et tmp/Table_pour_chaines.ent (comme une table embosseuse)
     * méthode d'accès
     * @param tableBraille le nom de la table braille à utiliser
     * @param sys true si table système
     */
    public void setTableBraille(String tableBraille, boolean sys) {
        fiConf.setProperty(OptNames.fi_braille_table, tableBraille);
        fiConf.setProperty(OptNames.fi_is_sys_braille_table, Boolean.toString(sys));
        FileChannel in = null;
        FileChannel out = null;
        try {
            String fichTable;
            if (!(tableBraille.endsWith(".ent"))) {
                tableBraille = tableBraille + ".ent";
            }
            if (sys) {
                fichTable = ConfigNat.getInstallFolder() + "xsl/tablesBraille/" + tableBraille;
            } else {
                fichTable = ConfigNat.getUserBrailleTableFolder() + tableBraille;
            }
            in = new FileInputStream(fichTable).getChannel();
            out = new FileOutputStream(getUserBrailleTableFolder() + "Brltab.ent").getChannel();
            in.transferTo(0, in.size(), out);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String fichTable;
            if (sys) {
                fichTable = ConfigNat.getInstallFolder() + "/xsl/tablesEmbosseuse/" + tableBraille;
            } else {
                fichTable = ConfigNat.getUserEmbossTableFolder() + "/" + tableBraille;
            }
            in = new FileInputStream(fichTable).getChannel();
            out = new FileOutputStream(ConfigNat.getUserTempFolder() + "Table_pour_chaines.ent").getChannel();
            in.transferTo(0, in.size(), out);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** @return true si Table Braille système */
    public boolean getIsSysTable() {
        return new Boolean(fiConf.getProperty(OptNames.fi_is_sys_braille_table, "true")).booleanValue();
    }

    /** @param a vrai si la table braille est une table système*/
    public void setIsSysTable(boolean a) {
        fiConf.setProperty(OptNames.fi_is_sys_braille_table, Boolean.toString(a));
    }

    /** @return true si Table Braille système */
    public boolean getIsSysEmbossTable() {
        return new Boolean(fiConf.getProperty(OptNames.fi_is_sys_emboss_table, "true")).booleanValue();
    }

    /** @param a vrai si la table braille est une table système*/
    public void setIsSysEmbossTable(boolean a) {
        fiConf.setProperty(OptNames.fi_is_sys_emboss_table, Boolean.toString(a));
    }

    /** @return nom de la configuration*/
    public String getName() {
        return (fiConf.getProperty(OptNames.fi_name, "* base *"));
    }

    /** @param name valeur pour le nom de la configuration*/
    public void setName(String name) {
        fiConf.setProperty(OptNames.fi_name, name);
    }

    /** @return description de la configuration*/
    public String getInfos() {
        return (fiConf.getProperty(OptNames.fi_infos, "* configuration de base  * "));
    }

    /** @param infos valeur pour la description de la configuration*/
    public void setInfos(String infos) {
        fiConf.setProperty(OptNames.fi_infos, infos);
    }

    /** @return vrai si la configuration est une configuration système*/
    public boolean getIsSysConfig() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_is_sys_config, "false"))).booleanValue());
    }

    /** @param a vrai si la configuration est une configuration système*/
    public void setIsSysConfig(boolean a) {
        fiConf.setProperty(OptNames.fi_is_sys_config, Boolean.toString(a));
    }

    /** @return true if optimization enabled */
    public boolean getOptimize() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_optimize, "false"))).booleanValue());
    }

    /** @param o vrai si activation des optimisations*/
    public void setOptimize(boolean o) {
        fiConf.setProperty(OptNames.fi_optimize, Boolean.toString(o));
    }

    /** @return le temps d'attente pour le serveur open office*/
    public int getTempsAttenteOO() {
        return Integer.parseInt((fiConf.getProperty(OptNames.ad_oo_wait, "5")));
    }

    /** @param sec le temps d'attente en secondes pour le serveur open office (dernière tentative)*/
    public void setTempsAttenteOO(int sec) {
        fiConf.setProperty(OptNames.ad_oo_wait, "" + sec);
    }

    /** @return renvoit vrai si détranscription, faux si transcription */
    public boolean isReverseTrans() {
        return ((new Boolean(uiConf.getProperty(OptNames.ui_reverse_trans, "false"))).booleanValue());
    }

    /** @param r vrai si transcription inverse*/
    public void setReverseTrans(boolean r) {
        uiConf.setProperty(OptNames.ui_reverse_trans, Boolean.toString(r));
    }

    /** @return vrai si abreger*/
    public boolean getAbreger() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_litt_abbreg, "false"))).booleanValue());
    }

    /** @param a valeur pour abreger*/
    public void setAbreger(boolean a) {
        fiConf.setProperty(OptNames.fi_litt_abbreg, Boolean.toString(a));
    }

    /** @return vrai si abreger*/
    public boolean getIvbMajSeule() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_abr_ivb_cap, "true"))).booleanValue());
    }

    /** @param ims valeur pour abreger*/
    public void setIvbMajSeule(boolean ims) {
        fiConf.setProperty(OptNames.fi_abr_ivb_cap, Boolean.toString(ims));
    }

    /** @return vrai si demander la liste des exceptions (boms propres) avant la transcription */
    public boolean getDemandeException() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_abr_ask_exceptions, "true"))).booleanValue());
    }

    /** @param de valeur pour demander la liste des exceptions*/
    public void setDemandeException(boolean de) {
        fiConf.setProperty(OptNames.fi_abr_ask_exceptions, Boolean.toString(de));
    }

    /** @return la liste des mots à conserver en intégral, séparés par des virgules */
    public String getListeIntegral() {
        return (fiConf.getProperty(OptNames.fi_abr_integral, ""));
    }

    /** @param li liste des mots à conserver en intégral, séparés par des virgules*/
    public void setListeIntegral(String li) {
        fiConf.setProperty(OptNames.fi_abr_integral, li);
    }

    /** @return la liste des mots à conserver parfois en intégral, séparés par des virgules */
    public String getListeIntegralAmb() {
        return (fiConf.getProperty(OptNames.fi_abr_integral_amb, ""));
    }

    /** @param lia liste des mots à conserver parfois en intégral, séparés par des virgules*/
    public void setListeIntegralAmb(String lia) {
        fiConf.setProperty(OptNames.fi_abr_integral_amb, lia);
    }

    /** @return vrai si traiter maths */
    public boolean getTraiterMaths() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_math_transcribe, "true"))).booleanValue());
    }

    /** @param m traiter maths*/
    public void setTraiterMaths(boolean m) {
        fiConf.setProperty(OptNames.fi_math_transcribe, Boolean.toString(m));
    }

    /** @return vrai si traiter littéraire */
    public boolean getTraiterLiteraire() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_litt_transcribe, "true"))).booleanValue());
    }

    /** @param l traiter littéraire */
    public void setTraiterLiteraire(boolean l) {
        fiConf.setProperty(OptNames.fi_litt_transcribe, Boolean.toString(l));
    }

    /** @return traiter musique */
    public boolean getTraiterMusique() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_music_transcribe, "true"))).booleanValue());
    }

    /** @param m traiter musique*/
    public void setTraiterMusique(boolean m) {
        fiConf.setProperty(OptNames.fi_music_transcribe, Boolean.toString(m));
    }

    /**
	 * TODO: options
     * @return true
     */
    public boolean getTraiterChimie() {
        return true;
    }

    /** @return utilisation de la notation spécifique trigo */
    public boolean getMathTrigoSpec() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_math_use_trigo_spec, "true"))).booleanValue());
    }

    /** @param m utilisation de la notation spécifique trigo */
    public void setMathTrigoSpec(boolean m) {
        fiConf.setProperty(OptNames.fi_math_use_trigo_spec, Boolean.toString(m));
    }

    /** @return true si préfixage systématique des maths */
    public boolean getMathPrefixAlways() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_math_force_prefix, "false"))).booleanValue());
    }

    /** @param mp true si préfixage systématique des maths */
    public void setMathPrefixAlways(boolean mp) {
        fiConf.setProperty(OptNames.fi_math_force_prefix, Boolean.toString(mp));
    }

    /** @param lg longueur de la ligne */
    public void setLongueurLigne(int lg) {
        fiConf.setProperty(OptNames.fi_line_lenght, (new Integer(lg)).toString());
    }

    /** @return longueur de la ligne */
    public int getLongueurLigne() {
        return ((new Integer(fiConf.getProperty(OptNames.fi_line_lenght, "40"))).intValue());
    }

    /** @param ln nombre de lignes par page*/
    public void setNbLigne(int ln) {
        fiConf.setProperty(OptNames.fi_line_number, (new Integer(ln)).toString());
    }

    /** @return nombre de lignes par page*/
    public int getNbLigne() {
        return ((new Integer(fiConf.getProperty(OptNames.fi_line_number, "40"))).intValue());
    }

    /** @param m activer la mise en page*/
    public void setMep(boolean m) {
        fiConf.setProperty(OptNames.pf_do_layout, Boolean.toString(m));
    }

    /** @return activer la mise en page*/
    public boolean getMep() {
        return ((new Boolean(fiConf.getProperty(OptNames.pf_do_layout, "true"))).booleanValue());
    }

    /** @param c coupure active*/
    public void setCoupure(boolean c) {
        fiConf.setProperty(OptNames.fi_hyphenation, Boolean.toString(c));
    }

    /** @return coupure active */
    public boolean getCoupure() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_hyphenation, "true"))).booleanValue());
    }

    /** @return use of the description automatically in the context */
    public boolean getReadDescr() {
        return ((new Boolean(fiConf.getProperty(OptNames.ui_readdescr, "false"))).booleanValue());
    }

    /** @param o true to use the description automatically in the context */
    public void setReadDescr(boolean o) {
        fiConf.setProperty(OptNames.ui_readdescr, Boolean.toString(o));
    }

    /** @return use of the tool tip text as the contextual description */
    public boolean getUseTTT() {
        return ((new Boolean(fiConf.getProperty(OptNames.ui_usettt, "false"))).booleanValue());
    }

    /** @param o true to use of the tool tip text as the contextual description */
    public void setUseTTT(boolean o) {
        fiConf.setProperty(OptNames.ui_usettt, Boolean.toString(o));
    }

    /** @return use of the tool tip text as the context */
    public boolean getReadTTT() {
        return ((new Boolean(fiConf.getProperty(OptNames.ui_readttt, "false"))).booleanValue());
    }

    /** @param o true to use of the tool tip text as the context */
    public void setReadTTT(boolean o) {
        fiConf.setProperty(OptNames.ui_readttt, Boolean.toString(o));
    }

    /** @return never use the description automatically in the context */
    public boolean getNeverReadDescr() {
        return ((new Boolean(fiConf.getProperty(OptNames.ui_neverreaddescr, "false"))).booleanValue());
    }

    /** @param o true to remove the description automatically in the context */
    public void setNeverReadDescr(boolean o) {
        fiConf.setProperty(OptNames.ui_readdescr, Boolean.toString(o));
    }

    /** @return open the help with browser */
    public boolean getOpenWithBrowser() {
        return ((new Boolean(fiConf.getProperty(OptNames.ui_openwithbrowser, "false"))).booleanValue());
    }

    /** @param o true to open the help with a browser */
    public void setOpenWithBrowser(boolean o) {
        fiConf.setProperty(OptNames.ui_openwithbrowser, Boolean.toString(o));
    }

    /** @return help key */
    public AWTKeyStroke getHelpKey() {
        return AWTKeyStroke.getAWTKeyStroke(fiConf.getProperty(OptNames.ui_helpkey, "released F1"));
    }

    /** @param o new help key*/
    public void setHelpKey(AWTKeyStroke o) {
        fiConf.setProperty(OptNames.ui_helpkey, o.toString());
    }

    /** @return help key */
    public AWTKeyStroke getTradKey() {
        return AWTKeyStroke.getAWTKeyStroke(fiConf.getProperty(OptNames.ui_tradkey, "released F2"));
    }

    /** @param o new help key*/
    public void setTradKey(AWTKeyStroke o) {
        fiConf.setProperty(OptNames.ui_tradkey, o.toString());
    }

    /** @return open the help with browser */
    public boolean getUseInternet() {
        return ((new Boolean(fiConf.getProperty(OptNames.ui_useinternet, "false"))).booleanValue());
    }

    /** @param o true to open the help with a browser */
    public void setUseInternet(boolean o) {
        fiConf.setProperty(OptNames.ui_useinternet, Boolean.toString(o));
    }

    /**@param c coupure littéraire*/
    public void setCoupureLit(boolean c) {
        fiConf.setProperty(OptNames.fi_hyphenation_lit, Boolean.toString(c));
    }

    /**@return coupure littéraire*/
    public boolean getCoupureLit() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_hyphenation_lit, "false"))).booleanValue());
    }

    /**@param m mode sagouin*/
    public void setModeCoupureSagouin(boolean m) {
        fiConf.setProperty(OptNames.fi_hyphenation_dirty, Boolean.toString(m));
    }

    /**@return mode sagouin*/
    public boolean getModeCoupureSagouin() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_hyphenation_dirty, "false"))).booleanValue());
    }

    /** @return adresse dico de coupure */
    public String getDicoCoup() {
        return (fiConf.getProperty(OptNames.fi_hyphenation_rulefile_name, getInstallFolder() + "xsl/dicts/hyph_fr_nat.dic"));
    }

    /** @param dc adresse dico de coupure*/
    public void setDicoCoup(String dc) {
        fiConf.setProperty(OptNames.fi_hyphenation_rulefile_name, dc);
    }

    /** @return feuille pour g1*/
    public String getXSL_g1() {
        return (fiConf.getProperty(OptNames.fi_litt_fr_int_filter_filename, getInstallFolder() + "xsl/fr-g1.xsl"));
    }

    /** @param filename feuilel pour g1*/
    public void setXSL_g1(String filename) {
        fiConf.setProperty(OptNames.fi_litt_fr_int_filter_filename, filename);
    }

    /** @return feuille pour g2*/
    public String getXSL_g2() {
        return (fiConf.getProperty(OptNames.fi_litt_fr_abbreg_filter_filename, getInstallFolder() + "xsl/fr-g2.xsl"));
    }

    /** @param filename feuilel pour g2*/
    public void setXSL_g2(String filename) {
        fiConf.setProperty(OptNames.fi_litt_fr_abbreg_filter_filename, filename);
    }

    /**
     * Renvoie l'adresse du fichier de règle d'abrégé de l'utilisateur;
     * Si ce fichier n'existe pas, le fabrique à partir du fichier de référence
     * @return l'adresse du fichier de règles d'abrégé 
     * TODO: permettre le paramétrage plus fin et l'utilisation de fichiers différents
     */
    public String getXSL_g2_Rules() {
        String adresse = fiConf.getProperty(OptNames.fi_litt_fr_abbreg_rules_filter_filename, getUserTempFolder() + "fr-g2-rules.xsl");
        if (!new File(adresse).exists()) {
            System.err.println("Pas de fichier xsl de règles pour l'abrégé; création à partir du fichier utilisateur");
            RulesToolKit.writeRules(RulesToolKit.getRules(new File(getRulesFrG2Perso()).toURI().toString()));
        }
        return adresse;
    }

    /** @return renvoie l'adresse du fichier XML de règles de référence */
    public String getRulesFrG2() {
        return fiConf.getProperty(OptNames.fi_litt_fr_abbreg_rules_filename, getInstallFolder() + "xsl/dicts/fr-g2.xml");
    }

    /** @return renvoie l'adresse du fichier XML de règles de l'utilisateur ou le fichier de référence si ce dernier n'existe pas */
    public String getRulesFrG2Perso() {
        return fiConf.getProperty(OptNames.fi_litt_fr_abbreg_rules_filename_perso, getInstallFolder() + "xsl/dicts/fr-g2.xml");
    }

    /** @param rulesFrG2Perso adresse du fichier XML de règles de l'utilisateur, situé dans le répertoire temporaire de l'utilisateur*/
    public void setRulesFrG2Perso(String rulesFrG2Perso) {
        fiConf.setProperty(OptNames.fi_litt_fr_abbreg_rules_filename_perso, new File(rulesFrG2Perso).getAbsolutePath());
    }

    /** @return feuille pour maths*/
    public String getXSL_maths() {
        return (fiConf.getProperty(OptNames.fi_math_filter_filename, getInstallFolder() + "xsl/fr-maths.xsl"));
    }

    /** @param filename feuille pour maths*/
    public void setXSL_maths(String filename) {
        fiConf.setProperty(OptNames.fi_math_filter_filename, filename);
    }

    /** @return feuille pour musique*/
    public String getXSL_musique() {
        return (fiConf.getProperty(OptNames.fi_music_filter_filename, getInstallFolder() + "xsl/musique.xsl"));
    }

    /** @param filename feuille pour musique*/
    public void setXSL_musique(String filename) {
        fiConf.setProperty(OptNames.fi_music_filter_filename, filename);
    }

    /** @return feuille pour la chimie */
    public String getXSL_chimie() {
        return (fiConf.getProperty(OptNames.fi_chemistry_filter_filename, getInstallFolder() + "xsl/fr-chimie.xsl"));
    }

    /** @param filename feuille pour la chimie*/
    public void setXSL_chimie(String filename) {
        fiConf.setProperty(OptNames.fi_chemistry_filter_filename, filename);
    }

    /** @return LitMajDouble*/
    public boolean getLitMajDouble() {
        return ((new Boolean(fiConf.getProperty(OptNames.tr_litt_double_upper, "true"))).booleanValue());
    }

    /** @param lmd LitMajDouble*/
    public void setLitMajDouble(boolean lmd) {
        fiConf.setProperty(OptNames.tr_litt_double_upper, Boolean.toString(lmd));
    }

    /** @return LitMajPassage*/
    public boolean getLitMajPassage() {
        return ((new Boolean(fiConf.getProperty(OptNames.tr_litt_part_upper, "true"))).booleanValue());
    }

    /** @param lmp LitMajPassage*/
    public void setLitMajPassage(boolean lmp) {
        fiConf.setProperty(OptNames.tr_litt_part_upper, Boolean.toString(lmp));
    }

    /** @return LitMajMelange*/
    public boolean getLitMajMelange() {
        return ((new Boolean(fiConf.getProperty(OptNames.tr_litt_mixed_upper, "true"))).booleanValue());
    }

    /** @param lmp LitMajMelange*/
    public void setLitMajMelange(boolean lmp) {
        fiConf.setProperty(OptNames.tr_litt_mixed_upper, Boolean.toString(lmp));
    }

    /** @return LitEvidenceMot*/
    public boolean getLitEvidenceMot() {
        return ((new Boolean(fiConf.getProperty(OptNames.tr_litt_word_emph, "true"))).booleanValue());
    }

    /** @param lmp LitEvidenceMot*/
    public void setLitEvidenceMot(boolean lmp) {
        fiConf.setProperty(OptNames.tr_litt_word_emph, Boolean.toString(lmp));
    }

    /** @return LitEvidencePassage*/
    public boolean getLitEvidencePassage() {
        return ((new Boolean(fiConf.getProperty(OptNames.tr_litt_part_emph, "true"))).booleanValue());
    }

    /** @param lmp LitEvidencePassage*/
    public void setLitEvidencePassage(boolean lmp) {
        fiConf.setProperty(OptNames.tr_litt_part_emph, Boolean.toString(lmp));
    }

    /** @return LitEvidenceDansMot*/
    public boolean getLitEvidenceDansMot() {
        return ((new Boolean(fiConf.getProperty(OptNames.tr_litt_in_word_emph, "true"))).booleanValue());
    }

    /** @param lmp LitEvidenceDansMot*/
    public void setLitEvidenceDansMot(boolean lmp) {
        fiConf.setProperty(OptNames.tr_litt_in_word_emph, Boolean.toString(lmp));
    }

    /** @return true if images are transcripted into braille*/
    public boolean getTranscrireImages() {
        return ((new Boolean(fiConf.getProperty(OptNames.tr_image_processing, "false"))).booleanValue());
    }

    /** @param ti true if images must be transcripted into braille*/
    public void setTranscrireImages(boolean ti) {
        fiConf.setProperty(OptNames.tr_image_processing, Boolean.toString(ti));
    }

    /** @param imd adresse du répertoire d'installation d'image magick*/
    public void setImageMagickDir(String imd) {
        fiConf.setProperty(OptNames.tr_image_magick_dir, imd);
    }

    /** @return l'adresse du répertoire d'installation de Image Magick*/
    public String getImageMagickDir() {
        return fiConf.getProperty(OptNames.tr_image_magick_dir, "");
    }

    /** @param nta Niveau de titre à partir duquel on abrège */
    public void setNiveauTitreAbrege(int nta) {
        fiConf.setProperty(OptNames.tr_min_title_contracted, "" + nta);
    }

    /** @return le niveau de titre à partir duquel on abrège */
    public int getNiveauTitreAbrege() {
        return Integer.parseInt(fiConf.getProperty(OptNames.tr_min_title_contracted, "1"));
    }

    /** @return MepModelignes*/
    public int getMepModelignes() {
        return ((new Integer(fiConf.getProperty(OptNames.pf_empty_line_mode, "3"))).intValue());
    }

    /** @param mml MepModelignes*/
    public void setMepModelignes(int mml) {
        fiConf.setProperty(OptNames.pf_empty_line_mode, (new Integer(mml)).toString());
    }

    /** @return MepMinLigne1*/
    public int getMepMinLigne1() {
        return ((new Integer(fiConf.getProperty(OptNames.pf_min_empty_line_1, "2"))).intValue());
    }

    /** @param mml1 MepMinLigne1*/
    public void setMepMinLigne1(int mml1) {
        fiConf.setProperty(OptNames.pf_min_empty_line_1, (new Integer(mml1)).toString());
    }

    /** @return MepMinLigne2*/
    public int getMepMinLigne2() {
        return ((new Integer(fiConf.getProperty(OptNames.pf_min_empty_line_2, "3"))).intValue());
    }

    /** @param mml2 MepMinLigne2*/
    public void setMepMinLigne2(int mml2) {
        fiConf.setProperty(OptNames.pf_min_empty_line_2, (new Integer(mml2)).toString());
    }

    /** @return MepMinLigne3*/
    public int getMepMinLigne3() {
        return ((new Integer(fiConf.getProperty(OptNames.pf_min_empty_line_3, "4"))).intValue());
    }

    /** @param mml3 MepMinLigne3*/
    public void setMepMinLigne3(int mml3) {
        fiConf.setProperty(OptNames.pf_min_empty_line_3, (new Integer(mml3)).toString());
    }

    /** @return MepMinLignePB*/
    public int getMepMinLignePB() {
        return ((new Integer(fiConf.getProperty(OptNames.pf_min_page_break, "5"))).intValue());
    }

    /** @param mmlpb MepMinLignePB*/
    public void setMepMinLignePB(int mmlpb) {
        fiConf.setProperty(OptNames.pf_min_page_break, (new Integer(mmlpb)).toString());
    }

    /** @return GeneratePB*/
    public boolean getGeneratePB() {
        return ((new Boolean(fiConf.getProperty(OptNames.pf_generate_page_break, "false"))).booleanValue());
    }

    /** @param sgpb GeneratePB*/
    public void setGeneratePB(boolean sgpb) {
        fiConf.setProperty(OptNames.pf_generate_page_break, Boolean.toString(sgpb));
    }

    /** @return KeepPageBreak*/
    public boolean getKeepPageBreak() {
        return ((new Boolean(fiConf.getProperty(OptNames.pf_keep_page_break, "false"))).booleanValue());
    }

    /** @param skpb KeepPageBreak*/
    public void setKeepPageBreak(boolean skpb) {
        fiConf.setProperty(OptNames.pf_keep_page_break, Boolean.toString(skpb));
    }

    /** @return SautPageFin*/
    public boolean getSautPageFin() {
        return ((new Boolean(fiConf.getProperty(OptNames.pf_add_form_feed, "true"))).booleanValue());
    }

    /** @param spf SautPageFin*/
    public void setSautPageFin(boolean spf) {
        fiConf.setProperty(OptNames.pf_add_form_feed, Boolean.toString(spf));
    }

    /** @param n Numerotation*/
    public void setNumerotation(String n) {
        fiConf.setProperty(OptNames.pf_numbering_style, n);
    }

    /** @return Numerotation*/
    public String getNumerotation() {
        return fiConf.getProperty(OptNames.pf_numbering_style, "'nn'");
    }

    /** @param snf NumeroteFirst*/
    public void setNumeroteFirst(boolean snf) {
        fiConf.setProperty(OptNames.pf_number_first_page, Boolean.toString(snf));
    }

    /** @return NumeroteFirst*/
    public boolean getNumeroteFirst() {
        return ((new Boolean(fiConf.getProperty(OptNames.pf_number_first_page, "false"))).booleanValue());
    }

    /** @return TitresStricts*/
    public boolean getTitresStricts() {
        return ((new Boolean(fiConf.getProperty(OptNames.pf_strict_titles, "true"))).booleanValue());
    }

    /** @param ts TitresStricts*/
    public void setTitresStricts(boolean ts) {
        fiConf.setProperty(OptNames.pf_strict_titles, Boolean.toString(ts));
    }

    /** @return NiveauxTitres*/
    public String getNiveauxTitres() {
        return fiConf.getProperty(OptNames.pf_titles_levels, "1,2,3,4,5,5,5,5,5");
    }

    /** @param levels NiveauxTitres
     * @throws NumberFormatException problème de format du niveau de titre*/
    public void setNiveauxTitres(String levels) throws NumberFormatException {
        int i = 0;
        String[] decoup = levels.split(",");
        try {
            for (i = 0; i < decoup.length; i++) {
                Integer.parseInt(decoup[i]);
            }
        } catch (NumberFormatException nfe) {
            throw nfe;
        }
        fiConf.setProperty(OptNames.pf_titles_levels, levels);
    }

    /** @return LineariseTable*/
    public boolean getLineariseTable() {
        return ((new Boolean(fiConf.getProperty(OptNames.pf_linearise_table, "false"))).booleanValue());
    }

    /** @param lt LineariseTable*/
    public void setLineariseTable(boolean lt) {
        fiConf.setProperty(OptNames.pf_linearise_table, Boolean.toString(lt));
    }

    /** @param mcl MinCellLin*/
    public void setMinCellLin(int mcl) {
        fiConf.setProperty(OptNames.pf_min_cell_linearise, Integer.toString(mcl));
    }

    /** @return MinCellLin*/
    public int getMinCellLin() {
        return ((new Integer(fiConf.getProperty(OptNames.pf_min_cell_linearise, "4"))).intValue());
    }

    /** @return the Poetry style name */
    public String getStylePoesie() {
        return fiConf.getProperty(OptNames.pf_poetry_style_name, "poesie");
    }

    /** @param psn the name for poetry style */
    public void setStylePoesie(String psn) {
        fiConf.setProperty(OptNames.pf_poetry_style_name, psn);
    }

    /** @return The chemistry style name */
    public String getStyleChimie() {
        return fiConf.getProperty(OptNames.pf_chemistry_style_name, "chimie");
    }

    /** @param csn the name for chemistry style */
    public void setStyleChimie(String csn) {
        fiConf.setProperty(OptNames.pf_chemistry_style_name, csn);
    }

    /**
     * Splits a string using a separator which is regarded as a single caracter if doubled
     * @return a String[] of splited values
     * @param  s   : the string
     * @param  sep : the separator
     */
    public static String[] intelliSplit(String s, String sep) {
        ArrayList<String> z = new ArrayList<String>();
        String curString = "";
        String[] ca = s.split("");
        int i = 0;
        while (i < (ca.length - 1)) {
            String c = ca[i];
            String n = ca[i + 1];
            if (c.equals(sep)) {
                if (n.equals(sep)) {
                    curString += c;
                    i++;
                } else {
                    z.add(curString);
                    curString = new String();
                }
            } else {
                curString += c;
            }
            i++;
        }
        if (i <= ca.length) {
            curString += ca[i];
        }
        z.add(curString);
        return z.toArray(new String[0]);
    }

    /** @return Rajout*/
    public String getRajout() {
        return fiConf.getProperty(OptNames.pf_strings_addons, "'','','','','','','','','','','',''");
    }

    /** @param r Rajout*/
    public void setRajout(String r) {
        fiConf.setProperty(OptNames.pf_strings_addons, r);
    }

    /** @return RajoutCompte*/
    public String getRajoutCompte() {
        return fiConf.getProperty(OptNames.pf_strings_addons_count, "false,false,false,false,false,false,false,false,false,false,false,false");
    }

    /** @param rc RajoutCompte*/
    public void setRajoutCompte(String rc) {
        fiConf.setProperty(OptNames.pf_strings_addons_count, rc);
    }

    /** @return Chaine_in*/
    public String getChaineIn() {
        return fiConf.getProperty(OptNames.pf_string_replace_in, "");
    }

    /** @param ci Chaine_in*/
    public void setChaineIn(String ci) {
        fiConf.setProperty(OptNames.pf_string_replace_in, ci);
    }

    /** @return Chaine_out*/
    public String getChaineOut() {
        return fiConf.getProperty(OptNames.pf_string_replace_out, "");
    }

    /** @param co Chaine_out*/
    public void setChaineOut(String co) {
        fiConf.setProperty(OptNames.pf_string_replace_out, co);
    }

    /** @param pe PoliceEditeur*/
    public void setPoliceEditeur(String pe) {
        fiConf.setProperty(OptNames.ui_editor_font, pe);
    }

    /** @return PoliceEditeur*/
    public String getPoliceEditeur() {
        return (fiConf.getProperty(OptNames.ui_editor_font, "Braille Antoine"));
    }

    /** @param pe2 PoliceEditeur2*/
    public void setPolice2Editeur(String pe2) {
        fiConf.setProperty(OptNames.ui_editor_font2, pe2);
    }

    /** @return PoliceEditeur2*/
    public String getPolice2Editeur() {
        return (fiConf.getProperty(OptNames.ui_editor_font2, "Courrier"));
    }

    /** @param t TaillePolice*/
    public void setTaillePolice(int t) {
        fiConf.setProperty(OptNames.ui_editor_font_size, (new Integer(t)).toString());
    }

    /** @return TaillePolice*/
    public int getTaillePolice() {
        return ((new Integer(fiConf.getProperty(OptNames.ui_editor_font_size, "24"))).intValue());
    }

    /** @param tp2 TaillePolice2*/
    public void setTaillePolice2(int tp2) {
        fiConf.setProperty(OptNames.ui_editor_font2_size, (new Integer(tp2)).toString());
    }

    /** @return TaillePolice2*/
    public int getTaillePolice2() {
        return ((new Integer(fiConf.getProperty(OptNames.ui_editor_font2_size, "24"))).intValue());
    }

    /** @param saxp SaxonAsXsltProcessor*/
    public void setSaxonAsXsltProcessor(boolean saxp) {
        fiConf.setProperty(OptNames.tr_use_saxon_processor, Boolean.toString(saxp));
    }

    /** @return SaxonAsXsltProcessor*/
    public boolean getSaxonAsXsltProcessor() {
        return new Boolean(fiConf.getProperty(OptNames.tr_use_saxon_processor, "true")).booleanValue();
    }

    /** @param nlf NbLogFiles*/
    public void setNbLogFiles(int nlf) {
        fiConf.setProperty(OptNames.ad_nb_log_files, Integer.toString(nlf));
    }

    /** @return NbLogFiles*/
    public int getNbLogFiles() {
        return ((new Integer(fiConf.getProperty(OptNames.ad_nb_log_files, "3"))).intValue());
    }

    /** @param lfs LogFileSize*/
    public void setLogFileSize(int lfs) {
        fiConf.setProperty(OptNames.ad_log_file_size, Integer.toString(lfs));
    }

    /** @return LogFileSize*/
    public int getLogFileSize() {
        return ((new Integer(fiConf.getProperty(OptNames.ad_log_file_size, "10"))).intValue());
    }

    /** @param o OuvreEditeurApresTranscription*/
    public void setOuvreEditeurApresTranscription(boolean o) {
        uiConf.setProperty(OptNames.ui_editor_auto_open, Boolean.toString(o));
    }

    /** @return OuvreEditeurApresTranscription*/
    public boolean getOuvrirEditeur() {
        return ((new Boolean(uiConf.getProperty(OptNames.ui_editor_auto_open, "true"))).booleanValue());
    }

    /** @return AfficheLigneSecondaire*/
    public boolean getAfficheLigneSecondaire() {
        return ((new Boolean(uiConf.getProperty(OptNames.ui_editor_zone2_display, "true"))).booleanValue());
    }

    /** @param als AfficheLigneSecondaire*/
    public void setAfficheLigneSecondaire(boolean als) {
        uiConf.setProperty(OptNames.ui_editor_zone2_display, Boolean.toString(als));
    }

    /** @return Editeur*/
    public String getEditeur() {
        return fiConf.getProperty(OptNames.ui_editor_external, "");
    }

    /** @param e Editeur*/
    public void setEditeur(String e) {
        fiConf.setProperty(OptNames.ui_editor_external, e);
    }

    /** @return UseNatEditor*/
    public boolean getUseNatEditor() {
        return new Boolean(fiConf.getProperty(OptNames.ui_editor_nat, "true")).booleanValue();
    }

    /** @param une UseNatEditor*/
    public void setUseNatEditor(boolean une) {
        fiConf.setProperty(OptNames.ui_editor_nat, "" + une);
    }

    /** @return UseDefaultEditor*/
    public boolean getUseDefaultEditor() {
        return new Boolean(fiConf.getProperty(OptNames.ui_editor_default, "false")).booleanValue();
    }

    /** @param ude UseDefaultEditor*/
    public void setUseDefaultEditor(boolean ude) {
        fiConf.setProperty(OptNames.ui_editor_default, "" + ude);
    }

    /** @return LastSource*/
    public String getLastSource() {
        return (uiConf.getProperty(OptNames.last_source_filename, ""));
    }

    /** @param filename LastSource*/
    public void setLastSource(String filename) {
        uiConf.setProperty(OptNames.last_source_filename, filename);
    }

    /** @return LastSourceEncoding*/
    public String getLastSourceEncoding() {
        return (uiConf.getProperty(OptNames.last_source_encoding, "automatique"));
    }

    /** @param enc LastSourceEncoding*/
    public void setLastSourceEncoding(String enc) {
        uiConf.setProperty(OptNames.last_source_encoding, enc);
    }

    /** @return LastDest*/
    public String getLastDest() {
        return (uiConf.getProperty(OptNames.last_dest_filename, ""));
    }

    /** @param filename LastDest*/
    public void setLastDest(String filename) {
        uiConf.setProperty(OptNames.last_dest_filename, filename);
    }

    /** @return LastDestEncoding*/
    public String getLastDestEncoding() {
        return (uiConf.getProperty(OptNames.last_dest_encoding, "automatique"));
    }

    /** @param enc LastDestEncoding*/
    public void setLastDestEncoding(String enc) {
        uiConf.setProperty(OptNames.last_dest_encoding, enc);
    }

    /** @param conffn LastFilterConfigurationFilename*/
    public void setLastFilterConfigurationFilename(String conffn) {
        uiConf.setProperty(OptNames.last_filter_configuration_filename, conffn);
    }

    /** @return LastFilterConfigurationFilename*/
    public String getLastFilterConfigurationFilename() {
        return (uiConf.getProperty(OptNames.last_filter_configuration_filename));
    }

    /** @param te table embossage
     * @param sys true si table système*/
    public void setTableEmbossage(String te, boolean sys) {
        fiConf.setProperty(OptNames.pr_emboss_table, te);
        fiConf.setProperty(OptNames.fi_is_sys_emboss_table, Boolean.toString(sys));
        FileChannel in = null;
        FileChannel out = null;
        try {
            String fichTable;
            if (!(te.endsWith(".ent"))) {
                te = te + ".ent";
            }
            if (sys) {
                fichTable = ConfigNat.getInstallFolder() + "/xsl/tablesEmbosseuse/" + te;
            } else {
                fichTable = ConfigNat.getUserEmbossTableFolder() + "/" + te;
            }
            in = new FileInputStream(fichTable).getChannel();
            out = new FileOutputStream(ConfigNat.getUserEmbossTableFolder() + "/Embtab.ent").getChannel();
            in.transferTo(0, in.size(), out);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** @return table Embossage*/
    public String getTableEmbossage() {
        return (fiConf.getProperty(OptNames.pr_emboss_table, "brailleUTF8"));
    }

    /** @param o OS*/
    public void setOs(String o) {
        fiConf.setProperty(OptNames.pr_os, o);
    }

    /** @return OS*/
    public String getOs() {
        return (fiConf.getProperty(OptNames.pr_os, "Linux"));
    }

    /** @param c Commande*/
    public void setCommande(String c) {
        fiConf.setProperty(OptNames.pr_emboss_command, c);
    }

    /** @return Commande */
    public String getCommande() {
        return (fiConf.getProperty(OptNames.pr_emboss_command, ""));
    }

    /** @return UtiliserCommandeEmbossage*/
    public boolean getUtiliserCommandeEmbossage() {
        return ((new Boolean(fiConf.getProperty(OptNames.pr_use_emboss_command, "false"))).booleanValue());
    }

    /** @param uce UtiliserCommandeEmbossage*/
    public void setUtiliserCommandeEmbossage(boolean uce) {
        fiConf.setProperty(OptNames.pr_use_emboss_command, Boolean.toString(uce));
    }

    /** @return UtiliserEmbosseuse*/
    public boolean getUtiliserEmbosseuse() {
        return ((new Boolean(fiConf.getProperty(OptNames.pr_emboss_auto, "false"))).booleanValue());
    }

    /** @param uce UtiliserEmbosseuse*/
    public void setUtiliserEmbosseuse(boolean uce) {
        fiConf.setProperty(OptNames.pr_emboss_auto, Boolean.toString(uce));
    }

    /** @param c print service*/
    public void setPrintService(String c) {
        if (c != null) {
            fiConf.setProperty(OptNames.pr_emboss_print_service, c);
        } else {
            fiConf.setProperty(OptNames.pr_emboss_print_service, "");
        }
    }

    /** @return nom du print service*/
    public String getPrintservice() {
        return (fiConf.getProperty(OptNames.pr_emboss_print_service, ""));
    }

    /** @return true si embosser avec un interligne double, false sinon */
    public boolean getDoubleSpace() {
        return new Boolean(fiConf.getProperty(OptNames.pr_double_space_on, "false")).booleanValue();
    }

    /** @param db utiliser un interligne double lors de l'embossage*/
    public void setDoubleSpace(boolean db) {
        fiConf.setProperty(OptNames.pr_double_space_on, Boolean.toString(db));
    }

    /** @return MemoriserFenetre*/
    public boolean getMemoriserFenetre() {
        return new Boolean(uiConf.getProperty(OptNames.ui_remember_windows_size, "true")).booleanValue();
    }

    /** @param rms MemoriserFenetre*/
    public void setMemoriserFenetre(boolean rms) {
        uiConf.setProperty(OptNames.ui_remember_windows_size, Boolean.toString(rms));
    }

    /** @return CentrerFenetre*/
    public boolean getCentrerFenetre() {
        return new Boolean(uiConf.getProperty(OptNames.ui_center_windows, "true")).booleanValue();
    }

    /** @param cf CentrerFenetre*/
    public void setCentrerFenetre(boolean cf) {
        uiConf.setProperty(OptNames.ui_center_windows, Boolean.toString(cf));
    }

    /** @return HeightEditeur*/
    public int getHeightEditeur() {
        return new Integer(uiConf.getProperty(OptNames.ui_y_editor, "0").toString());
    }

    /** @param he HeightEditeur*/
    public void setHeightEditeur(int he) {
        uiConf.setProperty(OptNames.ui_y_editor, "" + he);
    }

    /** @return WidthEditeur*/
    public int getWidthEditeur() {
        return new Integer(uiConf.getProperty(OptNames.ui_x_editor, "0").toString());
    }

    /** @param we WidthEditeur*/
    public void setWidthEditeur(int we) {
        uiConf.setProperty(OptNames.ui_x_editor, "" + we);
    }

    /** @return MaximizedEditeur*/
    public boolean getMaximizedEditeur() {
        return new Boolean(uiConf.getProperty(OptNames.ui_max_editor, "false")).booleanValue();
    }

    /** @param me MaximizedEditeur */
    public void setMaximizedEditeur(boolean me) {
        uiConf.setProperty(OptNames.ui_max_editor, "" + me);
    }

    /**@return true si affichage du texte et des icones sur les boutons */
    public boolean getShowIconText() {
        return new Boolean(uiConf.getProperty(OptNames.ui_text_icons_editor, "false")).booleanValue();
    }

    /** @param sit true if text and icons, false if icons only in editor */
    public void setShowIconText(boolean sit) {
        uiConf.setProperty(OptNames.ui_text_icons_editor, "" + sit);
    }

    /** @return HeightPrincipal*/
    public int getHeightPrincipal() {
        return new Integer(uiConf.getProperty(OptNames.ui_y_princ, "0").toString());
    }

    /** @param hp HeightPrincipal*/
    public void setHeightPrincipal(int hp) {
        uiConf.setProperty(OptNames.ui_y_princ, "" + hp);
    }

    /** @return WidthPrincipal*/
    public int getWidthPrincipal() {
        return new Integer(uiConf.getProperty(OptNames.ui_x_princ, "0").toString());
    }

    /** @param wp WidthPrincipal*/
    public void setWidthPrincipal(int wp) {
        uiConf.setProperty(OptNames.ui_x_princ, "" + wp);
    }

    /** @return MaximizedPrincipal*/
    public boolean getMaximizedPrincipal() {
        return new Boolean(uiConf.getProperty(OptNames.ui_max_princ, "false")).booleanValue();
    }

    /** @param mp MaximizedPrincipal */
    public void setMaximizedPrincipal(boolean mp) {
        uiConf.setProperty(OptNames.ui_max_princ, "" + mp);
    }

    /** @return HeightOptions*/
    public int getHeightOptions() {
        return new Integer(uiConf.getProperty(OptNames.ui_y_options, "0").toString());
    }

    /** @param ho HeightOptions*/
    public void setHeightOptions(int ho) {
        uiConf.setProperty(OptNames.ui_y_options, "" + ho);
    }

    /** @return WidthOptions*/
    public int getWidthOptions() {
        return new Integer(uiConf.getProperty(OptNames.ui_x_options, "0").toString());
    }

    /** @param wo WidthOptions*/
    public void setWidthOptions(int wo) {
        uiConf.setProperty(OptNames.ui_x_options, "" + wo);
    }

    /** @return MaximizedOptions*/
    public boolean getMaximizedOptions() {
        return new Boolean(uiConf.getProperty(OptNames.ui_max_options, "false")).booleanValue();
    }

    /** @param mo MaximizedOptions */
    public void setMaximizedOptions(boolean mo) {
        uiConf.setProperty(OptNames.ui_max_options, "" + mo);
    }

    /** @param dividerLocation position de la barre de division du splitpane de l'éditeur */
    public void setSplitPositionEditor(int dividerLocation) {
        uiConf.setProperty(OptNames.ui_divider_pos_editor, "" + dividerLocation);
    }

    /** @return divider location in pixel*/
    public int getSplitPositionEditor() {
        return new Integer(uiConf.getProperty(OptNames.ui_divider_pos_editor, "100")).intValue();
    }

    /** @return SonPendantTranscription*/
    public boolean getSonPendantTranscription() {
        return new Boolean(uiConf.getProperty(OptNames.ui_sound_during_work, "false")).booleanValue();
    }

    /** @param spt SonPendantTranscription*/
    public void setSonPendantTranscription(boolean spt) {
        uiConf.setProperty(OptNames.ui_sound_during_work, "" + spt);
    }

    /** @return SonFinTranscription*/
    public boolean getSonFinTranscription() {
        return new Boolean(uiConf.getProperty(OptNames.ui_sound_at_end, "false")).booleanValue();
    }

    /** @param sft SonFinTranscription*/
    public void setSonFinTranscription(boolean sft) {
        uiConf.setProperty(OptNames.ui_sound_at_end, "" + sft);
    }

    /** @return Nommer automatiquement le fichier de sortie*/
    public boolean getSortieAuto() {
        return new Boolean(uiConf.getProperty(OptNames.ui_output_file_auto, "false")).booleanValue();
    }

    /** @param sa true si nommer automatiquement le fichier de sortie*/
    public void setSortieAuto(boolean sa) {
        uiConf.setProperty(OptNames.ui_output_file_auto, "" + sa);
    }

    /** @return working directory*/
    public static String getWorkingDir() {
        String wd = "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(getInstallFolder() + "workingDir.txt"), "UTF-8"));
            wd = new File(br.readLine()).getAbsolutePath();
            br.close();
        } catch (FileNotFoundException e) {
            wd = new File(System.getProperty("user.home") + "/.nat-braille/").getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wd;
    }

    /** @param dirname dirname*/
    public void setWorkingDir(String dirname) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getInstallFolder() + "workingDir.txt")));
            bw.write(dirname);
            bw.close();
            JOptionPane.showMessageDialog(null, "Répertoire de travail changé.\n" + "Vous devez redémarrer NAT pour le prendre en considération.", "Information", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Impossible de changer le répertoire de travail.\n" + "Vous devez avoir les droits d'administrateur pour le faire", "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Enregistre la configuration par serialisation dans le fichier {@link ConfigNat#fichierConf}
     */
    public void sauvegarder() {
        saveUiConf();
        saveFilterConf(fichierConf);
    }

    /**
     * Donne l'adresse du dictionnaire de coupure par défaut
     * @return l'adresse du dictionnaire de coupure par défaut
     */
    public static String getDicoCoupDefaut() {
        return dicoCoupDefautName;
    }

    /**
     * Crée ou remplace une configuration à partir du fichier sérialisé 
     * fconf passé en parametre ou trouvé dans le fichier 
     * de conf de l'interface graphique. 
     * @param fconf ; si null, pris dans le fichier de conf d'interface.
     * @return Un objet {@link ConfigNat} créé à partir de fconf
     * <p>IOException Erreur E/S lors de la lecture du fichier fconf<p>
     * <p>ClassNotFoundException Erreur lors du cast de la sortie de <code>ObjectInputStream.readObject()</code></p>
     */
    public static ConfigNat charger(String fconf) {
        if (cn == null) {
            cn = new ConfigNat();
        }
        if (fconf == null) {
            checkWorkingDir();
            cn.loadUiConf();
            fconf = cn.getLastFilterConfigurationFilename();
        }
        cn.loadFilterConf(fconf);
        cn.setFichierConf(fconf);
        cn.setTableBraille(cn.getTableBraille(), cn.getIsSysTable());
        cn.setTableEmbossage(cn.getTableEmbossage(), cn.getIsSysEmbossTable());
        HyphenationToolkit.fabriqueDicoNat(cn.getDicoCoup(), Transcription.xslHyphen, "UTF-8");
        return cn;
    }

    /**
	 * Vérifie la présence des répertoires nécessaires dans répertoire de travail de l'utilisateur
	 * Si non présents, essaie de créer ces répertoires
	 */
    private static void checkWorkingDir() {
        String cpn = "";
        try {
            cpn = (new File(ConfigNat.getUserConfigFolder()).getCanonicalPath());
            boolean success = (new File(ConfigNat.getUserConfigFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = (new File(ConfigNat.getUserConfigFilterFolder()).getCanonicalPath());
            success = (new File(ConfigNat.getUserConfigFilterFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = (new File(ConfigNat.getUserTempFolder()).getCanonicalPath());
            success = (new File(ConfigNat.getUserTempFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = (new File(ConfigNat.getUserBrailleTableFolder()).getCanonicalPath());
            success = (new File(ConfigNat.getUserBrailleTableFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = (new File(ConfigNat.getUserEmbossTableFolder()).getCanonicalPath());
            success = (new File(ConfigNat.getUserEmbossTableFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = new File(ConfigNat.getUserTempFolder() + "regles").getCanonicalPath();
            success = (new File(ConfigNat.getUserTempFolder() + "regles")).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = new File(ConfigNat.getConfTempFolder()).getCanonicalPath();
            success = (new File(ConfigNat.getConfTempFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = new File(ConfigNat.getConfImportFolder()).getCanonicalPath();
            success = (new File(ConfigNat.getConfImportFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
            cpn = new File(ConfigNat.getConfTextFolder()).getCanonicalPath();
            success = (new File(ConfigNat.getConfTextFolder())).mkdir();
            if (success) {
                System.out.println("Directory: " + cpn + " created");
            }
        } catch (IOException ioe) {
            System.err.println("Erreur lors de la création du répertoire " + cpn);
        }
    }

    /** Renvoie la configuration en cours 
     * @return {@link #cn}*/
    public static ConfigNat getCurrentConfig() {
        return cn;
    }

    /** load and save ui and filter configuration 
     * @param configfile adresse du fichier de conf**/
    public void loadFilterConf(String configfile) {
        fiConf = new Properties();
        if (configfile != null) {
            try {
                fiConf.load(new FileInputStream(configfile));
                RulesToolKit.writeRules(RulesToolKit.getRules(new File(ConfigNat.getCurrentConfig().getRulesFrG2Perso()).toURI().toString()));
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.err.println("1-Exception while reading filter configuration file");
            }
        }
    }

    /** save configuration in file named configFile 
     * @param configFile adresse du fichier de conf*/
    public void saveFilterConf(String configFile) {
        try {
            fiConf.store(new FileOutputStream(configFile), null);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("Exception while writing filter configuration file");
        } catch (NullPointerException npe) {
            System.err.println("Sauvegarde de la configuration impossible: pas de configuration choisie");
        }
    }

    /** load user interface configuration file **/
    public void loadUiConf() {
        uiConf = new Properties();
        boolean fail = false;
        try {
            uiConf.load(new FileInputStream(getUserConfigFolder() + uiConfFilename));
        } catch (IOException ioe) {
            System.err.println("Exception while reading UI configuration file " + getUserConfigFolder() + uiConfFilename + "; using default values");
            setLastSource("license.txt");
            fail = true;
        }
        if (fail) {
            try {
                uiConf.load(new FileInputStream("nat-gui.conf-initial"));
            } catch (IOException ioe) {
                System.err.println("chargement de nat-gui.conf et .conf-initial échoué");
            }
        }
        setFNoir(getLastSource());
        setFBraille(getLastDest());
    }

    /** save user interface configuration file **/
    public void saveUiConf() {
        setLastSource(fichNoir);
        setLastSourceEncoding(sourceEncoding);
        setLastDest(fichBraille);
        setLastDestEncoding(sortieEncoding);
        try {
            setLastFilterConfigurationFilename(fichierConf);
        } catch (NullPointerException npe) {
            System.err.println("Sauvegarde du nom du fichier de configuration de l'interface graphique impossible: pas de configuration choisie");
        }
        try {
            uiConf.store(new FileOutputStream(getUserConfigFolder() + uiConfFilename), null);
        } catch (IOException ioe) {
            System.err.println("Exception while writing UI configuration file");
        }
    }

    /**
	 * Renvoie une instance de nat.transcodeur.AmbiguityResolverUI correspondant au mode d'excécution de NAT
     * @return une instance de AmbiguityResolver
     */
    public static AmbiguityResolverUI getAmbiguityResolverUI() {
        AmbiguityResolverUI retour = new AmbiguityResolverTextGUI();
        if (gui) {
            retour = new AmbiguityResolverGUI();
        }
        return retour;
    }

    /**
	 * valeur pour {@link #gui}
     * @param g va leur pour {@value #gui}
     */
    public static void setGUI(boolean g) {
        gui = g;
    }
}
