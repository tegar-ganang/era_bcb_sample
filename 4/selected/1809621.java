package NatPackage.server;

import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.lang.Boolean;
import java.lang.Integer;
import java.io.File;
import java.util.ArrayList;
import outils.FileToolKit;
import outils.regles.RulesToolKit;

/**
 * Cette classe contient l'ensemble des paramètres de configuration de NAT et
 * gère la sauvegarde et la récupération des différentes configurations
 * @author Bruno Mascret
 */
public class ConfigNat implements Serializable {

    private static String myCustomFolder = "";

    private static String installFolder = "/opt/jboss-3.2.6/server/default/deploy/jbossweb-tomcat50.sar/ROOT.war/Nat_JSP_Client/ressources/";

    /** Filter Properties */
    private Properties fiConf;

    /** UI Properties */
    private Properties uiConf;

    /** UI configuration filename */
    private static final String uiConfFilename = "nat-gui.conf";

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
    private final String version = "2.0rc";

    /** Nom de version long */
    private final String versionLong = "2.0rc";

    /** Nom de la version svn correspondante */
    private final String svnVersion = "r793";

    /** adresse du dernier fichier source utilisé */
    private String fsource = "";

    /** adresse du dernier fichier cible (sortie) utilisé */
    private String fcible = "";

    /** encoding du fichier source */
    private String sourceEncoding = "automatique";

    /** encoding du fichier de sortie */
    private String sortieEncoding = "UTF-8";

    /** l'instance singleton de ConfigNat*/
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
        String scf = "configurations/";
        return scf;
    }

    /**
     * @return the path of the user-specific configuration folder 
     */
    public static String getUserConfigFolder() {
        if ("".equals(myCustomFolder)) {
            return FileToolKit.getSysDepPath(System.getProperty("user.home") + "/.nat-braille/");
        } else {
            return myCustomFolder + "/.nat-braille/";
        }
    }

    /**
     * @return the path of the user-specific configuration filters folder 
     */
    public static String getUserConfigFilterFolder() {
        return getUserConfigFolder() + "filters/";
    }

    /**
     * @return the path of the user-specific temporary folder 
     */
    public static String getUserTempFolder() {
        return getUserConfigFolder() + "tmp/";
    }

    /**
     * @return the path of the user-specific braille table folder 
     */
    public static String getUserBrailleTableFolder() {
        return getUserConfigFolder() + "tablesBraille/";
    }

    /**
     * @return the path of the user-specific embossing table folder 
     */
    public static String getUserEmbossTableFolder() {
        return getUserConfigFolder() + "tablesEmbosseuse/";
    }

    /**
     * @return the path of install folder 
     */
    public static String getInstallFolder() {
        return installFolder;
    }

    /** @return {@link #versionLong} */
    public String getVersionLong() {
        return versionLong;
    }

    /** @return {@link #version}*/
    public String getVersion() {
        return version;
    }

    /** @return {@link #svnVersion}*/
    public String getSvnVersion() {
        return svnVersion;
    }

    /** @return {@link #fsource}*/
    public String getFsource() {
        return fsource;
    }

    /** @param fs valeur pour {@link #fsource}*/
    public void setFsource(String fs) {
        fsource = fs;
    }

    /** @return {@link #fcible}*/
    public String getFcible() {
        return fcible;
    }

    /** @param fc valeur pour {@link #fcible}*/
    public void setFcible(String fc) {
        fcible = fc;
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

    /** @return encodage du fichier source*/
    public String getSourceEncoding() {
        return fiConf.getProperty(OptNames.en_in, "UTF-8");
    }

    /** @param se valeur pour l'encodage du fichier source*/
    public void setSourceEncoding(String se) {
        fiConf.setProperty(OptNames.en_in, se);
    }

    /** @return encodage du fichier de sortie*/
    public String getSortieEncoding() {
        return fiConf.getProperty(OptNames.en_out, "UTF-8");
    }

    /** @param se valeur pour l'encodage du fichier de sortie*/
    public void setSortieEncoding(String se) {
        fiConf.setProperty(OptNames.en_out, se);
    }

    /** @param lg valeur pour le niveau de log*/
    public void setNiveauLog(int lg) {
        fiConf.setProperty(OptNames.ge_log_verbosity, (new Integer(lg)).toString());
    }

    /** @return le niveau de log*/
    public int getNiveauLog() {
        return ((new Integer(fiConf.getProperty(OptNames.ge_log_verbosity, "1"))).intValue());
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
     * Met à jour le nom de la table braille et copie la nouvelle table dans le
     * fichier Brltab.ent
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

    /** @return vrai si abreger*/
    public boolean getAbreger() {
        return ((new Boolean(fiConf.getProperty(OptNames.fi_litt_abbreg, "false"))).booleanValue());
    }

    /** @param a valeur pour abreger*/
    public void setAbreger(boolean a) {
        fiConf.setProperty(OptNames.fi_litt_abbreg, Boolean.toString(a));
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

    /** @param filename feuilel pour maths*/
    public void setXSL_maths(String filename) {
        fiConf.setProperty(OptNames.fi_math_filter_filename, filename);
    }

    /** @return feuille pour musique*/
    public String getXSL_musique() {
        return (fiConf.getProperty(OptNames.fi_music_filter_filename, getInstallFolder() + "xsl/musique.xsl"));
    }

    /** @param filename feuilel pour musique*/
    public void setXSL_musique(String filename) {
        fiConf.setProperty(OptNames.fi_music_filter_filename, filename);
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

    public static ConfigNat charger(String fconf) {
        System.out.println("*Methode ConfigNat* ConfigNat Charger");
        if (cn == null) {
            cn = new ConfigNat();
        }
        cn.loadFilterConf(fconf);
        cn.setFichierConf(fconf);
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

    /** Renvoie la configuration en cours 
     * @return {@link #cn}*/
    public static ConfigNat getCurrentConfig() {
        return cn;
    }

    /** Nos fonctions ALT*/
    public static void setWorkingDirectory(String wd) {
        myCustomFolder = wd;
        System.out.println("Working Directory : " + myCustomFolder + "\n");
    }

    public static void createWorkingDirectory() {
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
        } catch (IOException ioe) {
            System.err.println("Erreur lors de la création du répertoire " + cpn);
        }
    }
}
