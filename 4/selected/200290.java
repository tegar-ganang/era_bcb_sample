package org.fudaa.dodico.crue.io.neuf;

import gnu.trove.TObjectIntHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.dodico.crue.metier.emh.AbstractResPrtGeoSectionContainer;
import org.fudaa.dodico.crue.metier.emh.EnumTypeEMH;
import org.fudaa.dodico.crue.metier.emh.EnumTypeLoi;
import org.fudaa.dodico.crue.metier.emh.ResPrtGeoSection;
import org.fudaa.dodico.crue.metier.factory.CruePrefix;

/**
 * Classe permettant de stocker les données d'un profil lu dans STR
 * 
 * @author cde
 */
public class STRSequentialReader {

    private static final String TYPE_LIMITE_LIT_J = "LIMITEJ";

    private static final String TYPE_LIMITE_LIT_X = "LIMITEX";

    protected ByteOrder byteOrder;

    /**
   * Permet d'avoir la correspondance entre le nom du profil et sa position dans le STR. Rempli par StrReader. Le nom
   * est le nom dans le str et non le nom crue 10 avec les prefixes.
   */
    TObjectIntHashMap nomProfilPosition;

    List<String> nomProfils;

    /** Informations récupérées de STO */
    protected int nbLitMax;

    protected int nbHaut;

    protected int nbStr;

    protected int npo;

    protected int longueurEnregistrement;

    /**
   * @return le nombre de profil lu.
   */
    public int getNbProfil() {
        return nomProfils.size();
    }

    @Override
    public String toString() {
        return "Reader " + file.getName();
    }

    /**
   * @return la liste des noms de profils dans l'ordre du fichier et dans le format crue 9.
   */
    public List<String> getProfilNom() {
        return Collections.unmodifiableList(nomProfils);
    }

    /**
   * @param idx l'indice du profil
   * @return le nom du profil
   */
    public String getProfilName(final int idx) {
        return nomProfils.get(idx);
    }

    public class ResPrtGeoSectionContainer extends AbstractResPrtGeoSectionContainer {

        private final String profilNameCrue9;

        private final String profilNameCrue10;

        private SoftReference<ResPrtGeoSection> softRef;

        private final float[] loiZDact;

        /**
     * @param profilNameCrue9
     * @param loiZDact pour les branche de type 6, non null,
     */
        public ResPrtGeoSectionContainer(final String profilNameCrue9, final String profilNameCrue10, final float[] loiZDact) {
            this.profilNameCrue9 = profilNameCrue9;
            this.profilNameCrue10 = profilNameCrue10;
            this.loiZDact = loiZDact;
        }

        @Override
        public ResPrtGeoSection getResultat() {
            ResPrtGeoSection res = null;
            if (softRef == null || softRef.get() == null) {
                final DonneesSectionOuProfil resLu = read(profilNameCrue9);
                res = STRFactory.convertDonnees(resLu);
                if (loiZDact != null) {
                    try {
                        float[] discretisationEnZ = STRFactory.getDiscretisationEnZ(resLu);
                        res.setLoiZDact(STRFactory.createLoi(discretisationEnZ, loiZDact, profilNameCrue9, EnumTypeLoi.LoiZDact));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                res.setNom(profilNameCrue10);
                softRef = new SoftReference<ResPrtGeoSection>(res);
            } else {
                res = softRef.get();
            }
            return res;
        }

        /**
     * @return the profilName
     */
        @Override
        public String getId() {
            return profilNameCrue10;
        }
    }

    private final Map cache = new LRUMap(100);

    protected File file;

    /**
   * WARN: contient les loiZDact pour les section aval et amont des branches de type 6. L'id Crue 10 est utilisé et non
   * l'id lu dans le fichier STR comme pour le reste de cette classe.
   */
    private Map<String, float[]> loiZDactByIdBranch6;

    public AbstractResPrtGeoSectionContainer getResultatOnProfil(final String nomProfilCrue9) {
        AbstractResPrtGeoSectionContainer res = (AbstractResPrtGeoSectionContainer) cache.get(nomProfilCrue9);
        if (res == null) {
            String idCrue10 = CruePrefix.changePrefix(nomProfilCrue9, EnumTypeEMH.SECTION).toUpperCase();
            res = new ResPrtGeoSectionContainer(nomProfilCrue9, idCrue10, loiZDactByIdBranch6 == null ? null : loiZDactByIdBranch6.get(idCrue10));
            cache.put(nomProfilCrue9, res);
        }
        return res;
    }

    /**
   * Lit l'enregistrement correspondant au nom du profil passé en paramètre.
   * 
   * @param nomProfil le nom du profil
   * @return la structure lue dans le ficheir str.
   */
    public DonneesSectionOuProfil read(final String nomProfil) {
        try {
            if (nomProfilPosition.contains(nomProfil)) {
                return read(nomProfilPosition.get(nomProfil));
            }
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "read " + nomProfil, e);
        }
        return null;
    }

    private static final Logger LOGGER = Logger.getLogger(STRSequentialReader.class.getName());

    /**
   * Lit un enregistrement à partir du numéro d'enregistrement.
   * 
   * @param idx
   * @return DonneesSectionOuProfil
   * @throws IOException
   */
    public DonneesSectionOuProfil read(final int idx) throws IOException {
        final ByteBuffer bf = readData(idx);
        final DonneesSectionOuProfil donneesSection = new DonneesSectionOuProfil();
        String nom = getStringFromBuffer(bf, 16);
        donneesSection.setNom(nom.trim());
        donneesSection.setType(getStringFromBuffer(bf, 8).trim());
        final int nTypBra = bf.getInt();
        boolean isTypeBrancheOld = nTypBra == 0 || nTypBra == 2 || nTypBra == 6 || nTypBra == 9 || nTypBra == 15;
        donneesSection.setNTypBra(nTypBra);
        String prefRef = getStringFromBuffer(bf, 16).trim();
        donneesSection.setProfRef(prefRef);
        donneesSection.setDZref(bf.getFloat());
        String nomCasier = getStringFromBuffer(bf, 16).trim();
        donneesSection.setNomCasier(nomCasier);
        donneesSection.setDistAppliProfCasier(bf.getFloat());
        final int nbLit = bf.getInt();
        donneesSection.setNbLit(nbLit);
        final String[] typesLits = new String[nbLit];
        for (int j = 0; j < nbLit; j++) {
            typesLits[j] = getStringFromBuffer(bf, 8).trim();
        }
        donneesSection.setTypesLits(typesLits);
        skip(bf, 8 * (nbLitMax - nbLit));
        final String[] nomsLits = new String[nbLit];
        for (int j = 0; j < nbLit; j++) {
            nomsLits[j] = getStringFromBuffer(bf, 16).trim();
            if (StringUtils.isNotBlank(nomsLits[j])) {
                nomsLits[j] = CruePrefix.changePrefix(CruePrefix.P_LIT, nomsLits[j]);
            }
        }
        donneesSection.setNomsLits(nomsLits);
        skip(bf, 16 * (nbLitMax - nbLit));
        final float[] fondsLits = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            fondsLits[j] = bf.getFloat();
        }
        donneesSection.setZFondsLits(fondsLits);
        skip(bf, 4 * (nbLitMax - nbLit));
        final List<DonneesStricklerPourLitSection> stricklers = new ArrayList<DonneesStricklerPourLitSection>(nbLit);
        for (int j = 0; j < nbLit; j++) {
            final DonneesStricklerPourLitSection strickler = new DonneesStricklerPourLitSection();
            strickler.setNom(getStringFromBuffer(bf, 16));
            strickler.setType(getStringFromBuffer(bf, 8));
            strickler.setValeurConstante(bf.getFloat());
            final int nbPtLoiStrickler = bf.getInt();
            strickler.setNbPtLoi(nbPtLoiStrickler);
            final float[] zs = new float[nbPtLoiStrickler];
            for (int j2 = 0; j2 < nbPtLoiStrickler; j2++) {
                zs[j2] = bf.getFloat();
            }
            strickler.setZs(zs);
            skip(bf, 4 * (nbStr - nbPtLoiStrickler));
            final float[] ks = new float[nbPtLoiStrickler];
            for (int j2 = 0; j2 < nbPtLoiStrickler; j2++) {
                ks[j2] = bf.getFloat();
            }
            strickler.setKs(ks);
            skip(bf, 4 * (nbStr - nbPtLoiStrickler));
            stricklers.add(strickler);
        }
        donneesSection.setStricklers(stricklers);
        final int lgStrickler = (8 + 2 * nbStr) * 4;
        skip(bf, lgStrickler * (nbLitMax - nbLit));
        final float[] params = new float[5];
        for (int j = 0; j < 5; j++) {
            params[j] = bf.getFloat();
        }
        donneesSection.setParams(params);
        final int nbPtProfil = bf.getInt();
        donneesSection.setNbPtProfil(nbPtProfil);
        final float[] xsTravers = new float[nbPtProfil];
        for (int j = 0; j < nbPtProfil; j++) {
            xsTravers[j] = bf.getFloat();
        }
        donneesSection.setXsTravers(xsTravers);
        skip(bf, 4 * (npo - nbPtProfil));
        final float[] zsTravers = new float[nbPtProfil];
        for (int j = 0; j < nbPtProfil; j++) {
            zsTravers[j] = bf.getFloat();
        }
        donneesSection.setZsTravers(zsTravers);
        skip(bf, 4 * (npo - nbPtProfil));
        final float[] fentes = new float[2];
        for (int j = 0; j < 2; j++) {
            fentes[j] = bf.getFloat();
        }
        donneesSection.setFentes(fentes);
        donneesSection.setZDevers(bf.getFloat());
        donneesSection.setHDevers(bf.getFloat());
        donneesSection.setZDigueG(bf.getFloat());
        donneesSection.setZDigueD(bf.getFloat());
        donneesSection.setCDigueG(bf.getFloat());
        donneesSection.setCDigueD(bf.getFloat());
        donneesSection.setZf(bf.getFloat());
        donneesSection.setZHaut(bf.getFloat());
        donneesSection.setDZpro(bf.getFloat());
        final String typeLimLit = getStringFromBuffer(bf, 8);
        donneesSection.setTypeLimLit(typeLimLit);
        DonneesLimites limites = null;
        if (TYPE_LIMITE_LIT_J.equals(typeLimLit)) {
            limites = getDonneesLimiteJ(bf, nbLit);
        } else if (TYPE_LIMITE_LIT_X.equals(typeLimLit)) {
            limites = getDonneesLimiteX(bf, nbLit);
        } else {
            skip(bf, 2 * (4 * nbLitMax));
        }
        donneesSection.setDonneesLimites(limites);
        DonneesSectionPourBranche donneesBranche = null;
        if (isTypeBrancheOld) {
            donneesBranche = getBranche0Ou9(bf);
        } else {
            donneesBranche = getAutreBranche(bf, nbLit);
        }
        donneesSection.setDonneesPourBranche(donneesBranche);
        donneesSection.setNbHaut(nbHaut);
        return donneesSection;
    }

    private ByteBuffer readData(final int idx) {
        final ByteBuffer bf = ByteBuffer.allocate(longueurEnregistrement);
        bf.order(byteOrder);
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            final FileChannel channel = fileInputStream.getChannel();
            channel.read(bf, ((long) idx) * longueurEnregistrement);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "read", e);
        } finally {
            CtuluLibFile.close(fileInputStream);
        }
        bf.rewind();
        return bf;
    }

    /**
   * Récupère les données relatives aux limites droites et gauches des lits, en nombre de points
   * 
   * @param bf Buffer de lecture
   * @param nbLit nombre de points
   * @return DonneesLimitesJ
   */
    private DonneesLimitesJ getDonneesLimiteJ(final ByteBuffer bf, final int nbLit) {
        final DonneesLimitesJ limites = new DonneesLimitesJ();
        final int[] limsDeb = new int[nbLit];
        for (int j = 0; j < nbLit; j++) {
            limsDeb[j] = bf.getInt();
        }
        limites.setJLimsDeb(limsDeb);
        skip(bf, 4 * (nbLitMax - nbLit));
        final int[] limsFin = new int[nbLit];
        for (int j = 0; j < nbLit; j++) {
            limsFin[j] = bf.getInt();
        }
        limites.setJLimsFin(limsFin);
        skip(bf, 4 * (nbLitMax - nbLit));
        return limites;
    }

    /**
   * Récupère les données relatives aux limites droites et gauches des lits, en abcisses
   * 
   * @param bf Buffer de lecture
   * @param nbLit nombre de points
   * @return DonneesLimitesX
   */
    private DonneesLimitesX getDonneesLimiteX(final ByteBuffer bf, final int nbLit) {
        final DonneesLimitesX limites = new DonneesLimitesX();
        final float[] limsDeb = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            limsDeb[j] = bf.getFloat();
        }
        limites.setXLimsDeb(limsDeb);
        skip(bf, 4 * (nbLitMax - nbLit));
        final float[] limsFin = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            limsFin[j] = bf.getFloat();
        }
        limites.setXLimsFin(limsFin);
        skip(bf, 4 * (nbLitMax - nbLit));
        return limites;
    }

    /**
   * Retourne les informations d'une section pour une branche de type 0 ou 9
   * 
   * @param bf Buffer de lecture
   * @return SectionPourBranche0Ou9
   */
    private SectionPourBranche0Ou9 getBranche0Ou9(final ByteBuffer bf) {
        final SectionPourBranche0Ou9 donneesBranche = new SectionPourBranche0Ou9();
        donneesBranche.setUlm1(bf.getFloat());
        donneesBranche.setUlm2(bf.getFloat());
        donneesBranche.setUlm3(bf.getFloat());
        donneesBranche.setDZg11(bf.getFloat());
        donneesBranche.setDZd11(bf.getFloat());
        donneesBranche.setDZg12(bf.getFloat());
        donneesBranche.setDZd12(bf.getFloat());
        donneesBranche.setDZg13(bf.getFloat());
        donneesBranche.setDZd13(bf.getFloat());
        donneesBranche.setISec2(bf.getInt());
        donneesBranche.setIular4(bf.getInt());
        donneesBranche.setStric(bf.getFloat());
        final float[] listeSec1 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeSec1[j] = bf.getFloat();
        }
        donneesBranche.setListeSec1(listeSec1);
        final float[] listePer1 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listePer1[j] = bf.getFloat();
        }
        donneesBranche.setListePer1(listePer1);
        final float[] listeUlar1 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeUlar1[j] = bf.getFloat();
        }
        donneesBranche.setListeUlar1(listeUlar1);
        final float[] listeSec2 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeSec2[j] = bf.getFloat();
        }
        donneesBranche.setListeSec2(listeSec2);
        final float[] listePer2 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listePer2[j] = bf.getFloat();
        }
        donneesBranche.setListePer2(listePer2);
        final float[] listeUlar2 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeUlar2[j] = bf.getFloat();
        }
        donneesBranche.setListeUlar2(listeUlar2);
        final float[] listeUlar4 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeUlar4[j] = bf.getFloat();
        }
        donneesBranche.setListeUlar4(listeUlar4);
        final float[] listeCoefW1 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeCoefW1[j] = bf.getFloat();
        }
        donneesBranche.setListeCoefW1(listeCoefW1);
        final float[] listeCoefW2 = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeCoefW2[j] = bf.getFloat();
        }
        donneesBranche.setListeCoefW2(listeCoefW2);
        return donneesBranche;
    }

    /**
   * Retourne les informations d'une section pour un autre type de branche (que 0 ou 9)
   * 
   * @param bf Buffer de lecture
   * @param nbLit Nombre de points de lits
   * @return SectionPourAutreBranche
   */
    private SectionPourAutreBranche getAutreBranche(final ByteBuffer bf, final int nbLit) {
        final SectionPourAutreBranche donneesBranche = new SectionPourAutreBranche();
        final float[] listeSLitActif = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeSLitActif[j] = bf.getFloat();
        }
        donneesBranche.setListeSLitActif(listeSLitActif);
        final float[] listeLargLitActif = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeLargLitActif[j] = bf.getFloat();
        }
        donneesBranche.setListeLargLitActif(listeLargLitActif);
        final float[] listeDebLitActif = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeDebLitActif[j] = bf.getFloat();
        }
        donneesBranche.setListeDebLitActif(listeDebLitActif);
        final float[] listeLargCont = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeLargCont[j] = bf.getFloat();
        }
        donneesBranche.setListeLargCont(listeLargCont);
        final float[] listeBeta = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeBeta[j] = bf.getFloat();
        }
        donneesBranche.setListeBeta(listeBeta);
        final float[] listeLargSto = new float[nbHaut];
        for (int j = 0; j < nbHaut; j++) {
            listeLargSto[j] = bf.getFloat();
        }
        donneesBranche.setListeLargSto(listeLargSto);
        final float[] listeSurfHaut = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            listeSurfHaut[j] = bf.getFloat();
        }
        donneesBranche.setListeSurfHaut(listeSurfHaut);
        skip(bf, 4 * (nbLitMax - nbLit));
        final float[] listePerHaut = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            listePerHaut[j] = bf.getFloat();
        }
        donneesBranche.setListePerHaut(listePerHaut);
        skip(bf, 4 * (nbLitMax - nbLit));
        final float[] listeLargHaut = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            listeLargHaut[j] = bf.getFloat();
        }
        donneesBranche.setListeLargHaut(listeLargHaut);
        skip(bf, 4 * (nbLitMax - nbLit));
        final float[] listeStriHaut = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            listeStriHaut[j] = bf.getFloat();
        }
        donneesBranche.setListeStriHaut(listeStriHaut);
        skip(bf, 4 * (nbLitMax - nbLit));
        final float[] listeLargFond = new float[nbLit];
        for (int j = 0; j < nbLit; j++) {
            listeLargFond[j] = bf.getFloat();
        }
        donneesBranche.setListeLargFond(listeLargFond);
        skip(bf, 4 * (nbLitMax - nbLit));
        final int[] listeLitExtreme = new int[nbLit];
        for (int j = 0; j < nbLit; j++) {
            listeLitExtreme[j] = bf.getInt();
        }
        donneesBranche.setListeLitExtreme(listeLitExtreme);
        skip(bf, 4 * (nbLitMax - nbLit));
        donneesBranche.setRecAm(bf.getInt());
        donneesBranche.setRecAv(bf.getInt());
        donneesBranche.setDistAm(bf.getFloat());
        donneesBranche.setDistAv(bf.getFloat());
        return donneesBranche;
    }

    /**
   * Déplace le pointeur de lecture d'un certain nombre d'octets
   * 
   * @param bf le buffer pour lequel on veut modifier l'index
   * @param _nbToSkip le nombre d'octet a sauter
   */
    protected static void skip(final ByteBuffer bf, final int _nbToSkip) {
        bf.position(bf.position() + _nbToSkip);
    }

    /**
   * Récupère une chaîne de caractères dans le buffer
   * 
   * @param buf Buffer de lecture
   * @param _nbChar Nombre de caractères de la chaîne
   * @return String
   */
    protected static String getStringFromBuffer(final ByteBuffer buf, final int _nbChar) {
        final byte[] charByte = new byte[_nbChar];
        buf.get(charByte);
        return new String(charByte).trim();
    }

    /**
   * Interface permettant de représenter les données des limites de type J ou X
   * 
   * @author cde
   */
    public interface DonneesLimites {
    }

    /**
   * Classe représentant les données des limites en nombre de points
   * 
   * @author cde
   */
    public class DonneesLimitesJ implements DonneesLimites {

        /** Limite droite des lits, en numéro de point */
        private int[] jLimsDeb;

        /** Limite gauche des lits, en numéro de point */
        private int[] jLimsFin;

        /**
     * @return the jLimsDeb
     */
        public int[] getJLimsDeb() {
            return jLimsDeb;
        }

        /**
     * @param limsDeb the jLimsDeb to set
     */
        public void setJLimsDeb(final int[] limsDeb) {
            jLimsDeb = limsDeb;
        }

        /**
     * @return the jLimsFin
     */
        public int[] getJLimsFin() {
            return jLimsFin;
        }

        /**
     * @param limsFin the jLimsFin to set
     */
        public void setJLimsFin(final int[] limsFin) {
            jLimsFin = limsFin;
        }
    }

    /**
   * Classe représentant les données des limites en abscisses
   * 
   * @author cde
   */
    public class DonneesLimitesX implements DonneesLimites {

        /** Limite droite des lits, en abscisse */
        private float[] xLimsDeb;

        /** Limite gauche des lits, en abscisse */
        private float[] xLimsFin;

        /**
     * @return the xLimsDeb
     */
        public float[] getXLimsDeb() {
            return xLimsDeb;
        }

        /**
     * @param limsDeb the xLimsDeb to set
     */
        public void setXLimsDeb(final float[] limsDeb) {
            xLimsDeb = limsDeb;
        }

        /**
     * @return the xLimsFin
     */
        public float[] getXLimsFin() {
            return xLimsFin;
        }

        /**
     * @param limsFin the xLimsFin to set
     */
        public void setXLimsFin(final float[] limsFin) {
            xLimsFin = limsFin;
        }
    }

    /**
   * Données d'une section ou d'un profil
   * 
   * @author cde
   */
    public static class DonneesSectionOuProfil {

        /** Nom de la section */
        private String nom;

        /** Type de section: cstrPROFIL, cstrPROFRECT, cstrPROFTRAP, cstrPROFINT, cstrPROFAUTO */
        private String type;

        /** Nom éventuel du profil de référence associé */
        private String profRef;

        /** Décalage vertical éventuel appliqué au profil de référence pour recalculer toutes les cotes */
        private float dZref;

        /** Nom éventuel du casier associé */
        private String nomCasier;

        /** Distance d'application du profil de casier */
        private float distAppliProfCasier;

        /** Nombre de lits dans le profil */
        private int nbLit;

        private int typBra;

        /** Type du lit */
        private String[] typesLits;

        /** Nom du lit */
        private String[] nomsLits;

        /** Cote du fond du lit */
        private float[] zFondsLits;

        /** Structures Strickler pour chaque lit */
        private List<DonneesStricklerPourLitSection> stricklers;

        /**
     * Paramètres du profil (seuls 3 premiers utilisés): Param(1): numéro du lit de zfond ; Param(2): taux de surface de
     * la fente ; Param(3): taux de débitance de la fente
     */
        private float[] params;

        /** Nombre de points du profil */
        private int nbPtProfil;

        /** Premier élément du point du profil en travers */
        private float[] xsTravers;

        /** Second élément du point du profil en travers */
        private float[] zsTravers;

        /**
     * Paramètres de la fente: Fente(1): largeur ; Fente(2): profondeur
     */
        private float[] fentes;

        /** Cote à partir de laquelle toute la section de stockage est pleine en cas de connexion par-dessus une digue */
        private float zDevers;

        /** Hauteur séparant la crête de digue de Zdevers en cas de connexion de la zone de stockage par-dessus une digue */
        private float hDevers;

        /** Cote du déversoir de la digue évacuant du débit hors du profil en RG */
        private float zDigueG;

        /** Cote du déversoir de la digue évacuant du débit hors du profil en RD */
        private float zDigueD;

        /** Coefficient de débitance dénoyé du déversoir de la digue en RG */
        private float cDigueG;

        /** Coefficient de débitance dénoyé du déversoir de la digue en RD */
        private float cDigueD;

        /** Cote du fond de la section */
        private float zf;

        /** Cote du point le plus haut de la section */
        private float zHaut;

        /** Pas de hauteur pour le planimétrage de la section */
        private float dZpro;

        /** Type de limite de lit: cstrLIMITEJ, cstrLIMITEX */
        private String typeLimLit;

        private DonneesLimites donneesLimites;

        private DonneesSectionPourBranche donneesPourBranche;

        /** Donnée servant lors de l'alimentation métier des lois FF */
        private int nbHaut;

        /**
     * @return the cDigueD
     */
        public float getCDigueD() {
            return cDigueD;
        }

        /**
     * @return the cDigueG
     */
        public float getCDigueG() {
            return cDigueG;
        }

        /**
     * @return the distAppliProfCasier
     */
        public float getDistAppliProfCasier() {
            return distAppliProfCasier;
        }

        /**
     * @return DonneesLimites (DonneesLimitesJ ou DonneesLimitesX)
     */
        public DonneesLimites getDonneesLimites() {
            return donneesLimites;
        }

        /**
     * @return DonneesSectionPourBranche
     */
        public DonneesSectionPourBranche getDonneesPourBranche() {
            return donneesPourBranche;
        }

        /**
     * @return the dZpro
     */
        public float getDZpro() {
            return dZpro;
        }

        /**
     * @return the dZref
     */
        public float getDZref() {
            return dZref;
        }

        /**
     * @return the fentes
     */
        public float[] getFentes() {
            return fentes;
        }

        /**
     * @return the hDevers
     */
        public float getHDevers() {
            return hDevers;
        }

        /**
     * @return the nbLit
     */
        public int getNbLit() {
            return nbLit;
        }

        /**
     * @return the nbPtProfil
     */
        public int getNbPtProfil() {
            return nbPtProfil;
        }

        /**
     * @return the nom
     */
        public String getNom() {
            return nom;
        }

        /**
     * @return the nomCasier
     */
        public String getNomCasier() {
            return nomCasier;
        }

        /**
     * @return the nomsLits
     */
        public String[] getNomsLits() {
            return nomsLits;
        }

        /**
     * @return the nTypBra
     */
        public int getTypBra() {
            return typBra;
        }

        /**
     * @return the params
     */
        public float[] getParams() {
            return params;
        }

        /**
     * @return the profRef
     */
        public String getProfRef() {
            return profRef;
        }

        /**
     * @return the stricklers
     */
        public List<DonneesStricklerPourLitSection> getStricklers() {
            return stricklers;
        }

        /**
     * @return the type
     */
        public String getType() {
            return type;
        }

        /**
     * @return the typeLimLit
     */
        public String getTypeLimLit() {
            return typeLimLit;
        }

        /**
     * @return the typesLits
     */
        public String[] getTypesLits() {
            return typesLits;
        }

        /**
     * @return the xsTravers
     */
        public float[] getXsTravers() {
            return xsTravers;
        }

        /**
     * @return the zDevers
     */
        public float getZDevers() {
            return zDevers;
        }

        /**
     * @return the zDigueD
     */
        public float getZDigueD() {
            return zDigueD;
        }

        /**
     * @return the zDigueG
     */
        public float getZDigueG() {
            return zDigueG;
        }

        /**
     * @return the Zf
     */
        public float getZf() {
            return zf;
        }

        /**
     * @return the zFondsLits
     */
        public float[] getZFondsLits() {
            return zFondsLits;
        }

        /**
     * @return the zHaut
     */
        public float getZHaut() {
            return zHaut;
        }

        /**
     * @return the zsTravers
     */
        public float[] getZsTravers() {
            return zsTravers;
        }

        /**
     * @param digueD the cDigueD to set
     */
        public void setCDigueD(final float digueD) {
            cDigueD = digueD;
        }

        /**
     * @param digueG the cDigueG to set
     */
        public void setCDigueG(final float digueG) {
            cDigueG = digueG;
        }

        /**
     * @param distAppliProfCasier the distAppliProfCasier to set
     */
        public void setDistAppliProfCasier(final float distAppliProfCasier) {
            this.distAppliProfCasier = distAppliProfCasier;
        }

        /**
     * @param donneesLimites
     */
        public void setDonneesLimites(final DonneesLimites donneesLimites) {
            this.donneesLimites = donneesLimites;
        }

        /**
     * @param donneesPourBranche
     */
        public void setDonneesPourBranche(final DonneesSectionPourBranche donneesPourBranche) {
            this.donneesPourBranche = donneesPourBranche;
        }

        /**
     * @param zpro the dZpro to set
     */
        public void setDZpro(final float zpro) {
            dZpro = zpro;
        }

        /**
     * @param zref the dZref to set
     */
        public void setDZref(final float zref) {
            dZref = zref;
        }

        /**
     * @param fentes the fentes to set
     */
        public void setFentes(final float[] fentes) {
            this.fentes = fentes;
        }

        /**
     * @param devers the hDevers to set
     */
        public void setHDevers(final float devers) {
            hDevers = devers;
        }

        /**
     * @param nbLit the nbLit to set
     */
        public void setNbLit(final int nbLit) {
            this.nbLit = nbLit;
        }

        /**
     * @param nbPtProfil the nbPtProfil to set
     */
        public void setNbPtProfil(final int nbPtProfil) {
            this.nbPtProfil = nbPtProfil;
        }

        /**
     * @param nom the nom to set
     */
        public void setNom(final String nom) {
            this.nom = nom;
        }

        /**
     * @param nomCasier the nomCasier to set
     */
        public void setNomCasier(final String nomCasier) {
            this.nomCasier = nomCasier;
        }

        /**
     * @param nomsLits the nomsLits to set
     */
        public void setNomsLits(final String[] nomsLits) {
            this.nomsLits = nomsLits;
        }

        /**
     * @param typBra the nTypBra to set
     */
        public void setNTypBra(final int typBra) {
            this.typBra = typBra;
        }

        /**
     * @param params the params to set
     */
        public void setParams(final float[] params) {
            this.params = params;
        }

        /**
     * @param profRef the profRef to set
     */
        public void setProfRef(final String profRef) {
            this.profRef = profRef;
        }

        /**
     * @param stricklers the stricklers to set
     */
        public void setStricklers(final List<DonneesStricklerPourLitSection> stricklers) {
            this.stricklers = stricklers;
        }

        /**
     * @param type the type to set
     */
        public void setType(final String type) {
            this.type = type;
        }

        /**
     * @param typeLimLit the typeLimLit to set
     */
        public void setTypeLimLit(final String typeLimLit) {
            this.typeLimLit = typeLimLit;
        }

        /**
     * @param typesLits the typesLits to set
     */
        public void setTypesLits(final String[] typesLits) {
            this.typesLits = typesLits;
        }

        /**
     * @param xsTravers the xsTravers to set
     */
        public void setXsTravers(final float[] xsTravers) {
            this.xsTravers = xsTravers;
        }

        /**
     * @param devers the zDevers to set
     */
        public void setZDevers(final float devers) {
            zDevers = devers;
        }

        /**
     * @param digueD the zDigueD to set
     */
        public void setZDigueD(final float digueD) {
            zDigueD = digueD;
        }

        /**
     * @param digueG the zDigueG to set
     */
        public void setZDigueG(final float digueG) {
            zDigueG = digueG;
        }

        /**
     * @param zf the Zfto set
     */
        public void setZf(final float zf) {
            this.zf = zf;
        }

        /**
     * @param fondsLits the zFondsLits to set
     */
        public void setZFondsLits(final float[] fondsLits) {
            zFondsLits = fondsLits;
        }

        /**
     * @param haut the zHaut to set
     */
        public void setZHaut(final float haut) {
            zHaut = haut;
        }

        /**
     * @param zsTravers the zsTravers to set
     */
        public void setZsTravers(final float[] zsTravers) {
            this.zsTravers = zsTravers;
        }

        /**
     * @return the nbHaut
     */
        public int getNbHaut() {
            return nbHaut;
        }

        /**
     * @param nbHaut the nbHaut to set
     */
        public void setNbHaut(final int nbHaut) {
            this.nbHaut = nbHaut;
        }
    }

    /**
   * Données de définition d'une structure Strickler, pour chaque lit d'une section
   * 
   * @author cde
   */
    public static class DonneesStricklerPourLitSection {

        /** Nom du Strickler */
        private String nom;

        /** Type du Strickler: cstrSTRIREF, cstrSTRIREFZ, cstrSTRIREFH */
        private String type;

        /** Strickler constant */
        private float valeurConstante;

        /** Nombre de points définissant la loi de variation avec la cote ou la hauteur d'eau */
        private int nbPtLoi;

        /** Z si Type=cstrSTRIREFZ ou H si Type=cstrSTRIREFH */
        private float[] zs;

        /** Valeur du Strickler en fonction de la hauteur */
        private float[] ks;

        /**
     * @return the ks
     */
        public float[] getKs() {
            return ks;
        }

        /**
     * @return the nbPtLoi
     */
        public int getNbPtLoi() {
            return nbPtLoi;
        }

        /**
     * @return the nom
     */
        public String getNom() {
            return nom;
        }

        /**
     * @return the type
     */
        public String getType() {
            return type;
        }

        /**
     * @return the valeurConstante
     */
        public float getValeurConstante() {
            return valeurConstante;
        }

        /**
     * @return the zs
     */
        public float[] getZs() {
            return zs;
        }

        /**
     * @param ks the ks to set
     */
        public void setKs(final float[] ks) {
            this.ks = ks;
        }

        /**
     * @param nbPtLoi the nbPtLoi to set
     */
        public void setNbPtLoi(final int nbPtLoi) {
            this.nbPtLoi = nbPtLoi;
        }

        /**
     * @param nom the nom to set
     */
        public void setNom(final String nom) {
            this.nom = nom;
        }

        /**
     * @param type the type to set
     */
        public void setType(final String type) {
            this.type = type;
        }

        /**
     * @param valeurConstante the valeurConstante to set
     */
        public void setValeurConstante(final float valeurConstante) {
            this.valeurConstante = valeurConstante;
        }

        /**
     * @param zs the zs to set
     */
        public void setZs(final float[] zs) {
            this.zs = zs;
        }
    }

    /**
   * Classe contenant les informations d'une section quelle que soit le type de branche
   * 
   * @author cde
   */
    public interface DonneesSectionPourBranche {
    }

    /**
   * Données des sections dépendant du type de branche, pour une branche 2, 6, 15, 20 ou inconnue
   * 
   * @author cde
   */
    public static class SectionPourAutreBranche implements DonneesSectionPourBranche {

        /** Surfaces mouillées tabulées du lit actif */
        private float[] listeSLitActif;

        /** Largeurs tabulées du lit actif */
        private float[] listeLargLitActif;

        /** Débitances tabulées du lit actif */
        private float[] listeDebLitActif;

        /** Largeurs de continuité (largeurs totales) tabulées */
        private float[] listeLargCont;

        /** Coefficients Beta (de Boussinesq) */
        private float[] listeBeta;

        /** Largeurs de stockage tabulées */
        private float[] listeLargSto;

        /** Surfaces pour chaque lit à la cote supérieure */
        private float[] listeSurfHaut;

        /** Périmètres mouillés pour chaque lit à Zhaut */
        private float[] listePerHaut;

        /** Largeurs pour chaque lit à Zhaut */
        private float[] listeLargHaut;

        /** Strickler pour chaque lit à Zhaut */
        private float[] listeStriHaut;

        /** Largeurs pour chaque lit à Zfond */
        private float[] listeLargFond;

        /** Indice de lit pour le calcul du périmètre mouillé */
        private int[] listeLitExtreme;

        /** Numéro d'enregistrement du profil amont */
        private int recAm;

        /** Numéro d'enregistrement du profil aval */
        private int recAv;

        /** Distance au profil amont */
        private float distAm;

        /** Distance au profil aval */
        private float distAv;

        /**
     * @return the distAm
     */
        public float getDistAm() {
            return distAm;
        }

        /**
     * @return the distAv
     */
        public float getDistAv() {
            return distAv;
        }

        /**
     * @return the listeBeta
     */
        public float[] getListeBeta() {
            return listeBeta;
        }

        /**
     * @return the listeDebLitActif
     */
        public float[] getListeDebLitActif() {
            return listeDebLitActif;
        }

        /**
     * @return the listeLargCont
     */
        public float[] getListeLargCont() {
            return listeLargCont;
        }

        /**
     * @return the listeLargFond
     */
        public float[] getListeLargFond() {
            return listeLargFond;
        }

        /**
     * @return the listeLargHaut
     */
        public float[] getListeLargHaut() {
            return listeLargHaut;
        }

        /**
     * @return the listeLargLitActif
     */
        public float[] getListeLargLitActif() {
            return listeLargLitActif;
        }

        /**
     * @return the listeLargSto
     */
        public float[] getListeLargSto() {
            return listeLargSto;
        }

        /**
     * @return the listeLitExtreme
     */
        public int[] getListeLitExtreme() {
            return listeLitExtreme;
        }

        /**
     * @return the listePerHaut
     */
        public float[] getListePerHaut() {
            return listePerHaut;
        }

        /**
     * @return the listeSLitActif
     */
        public float[] getListeSLitActif() {
            return listeSLitActif;
        }

        /**
     * @return the listeStriHaut
     */
        public float[] getListeStriHaut() {
            return listeStriHaut;
        }

        /**
     * @return the listeSurfHaut
     */
        public float[] getListeSurfHaut() {
            return listeSurfHaut;
        }

        /**
     * @return the recAm
     */
        public int getRecAm() {
            return recAm;
        }

        /**
     * @return the recAv
     */
        public int getRecAv() {
            return recAv;
        }

        /**
     * @param distAm the distAm to set
     */
        public void setDistAm(final float distAm) {
            this.distAm = distAm;
        }

        /**
     * @param distAv the distAv to set
     */
        public void setDistAv(final float distAv) {
            this.distAv = distAv;
        }

        /**
     * @param listeBeta the listeBeta to set
     */
        public void setListeBeta(final float[] listeBeta) {
            this.listeBeta = listeBeta;
        }

        /**
     * @param listeDebLitActif the listeDebLitActif to set
     */
        public void setListeDebLitActif(final float[] listeDebLitActif) {
            this.listeDebLitActif = listeDebLitActif;
        }

        /**
     * @param listeLargCont the listeLargCont to set
     */
        public void setListeLargCont(final float[] listeLargCont) {
            this.listeLargCont = listeLargCont;
        }

        /**
     * @param listeLargFond the listeLargFond to set
     */
        public void setListeLargFond(final float[] listeLargFond) {
            this.listeLargFond = listeLargFond;
        }

        /**
     * @param listeLargHaut the listeLargHaut to set
     */
        public void setListeLargHaut(final float[] listeLargHaut) {
            this.listeLargHaut = listeLargHaut;
        }

        /**
     * @param listeLargLitActif the listeLargLitActif to set
     */
        public void setListeLargLitActif(final float[] listeLargLitActif) {
            this.listeLargLitActif = listeLargLitActif;
        }

        /**
     * @param listeLargSto the listeLargSto to set
     */
        public void setListeLargSto(final float[] listeLargSto) {
            this.listeLargSto = listeLargSto;
        }

        /**
     * @param listeLitExtreme the listeLitExtreme to set
     */
        public void setListeLitExtreme(final int[] listeLitExtreme) {
            this.listeLitExtreme = listeLitExtreme;
        }

        /**
     * @param listePerHaut the listePerHaut to set
     */
        public void setListePerHaut(final float[] listePerHaut) {
            this.listePerHaut = listePerHaut;
        }

        /**
     * @param listeSLitActif the listeSLitActif to set
     */
        public void setListeSLitActif(final float[] listeSLitActif) {
            this.listeSLitActif = listeSLitActif;
        }

        /**
     * @param listeStriHaut the listeStriHaut to set
     */
        public void setListeStriHaut(final float[] listeStriHaut) {
            this.listeStriHaut = listeStriHaut;
        }

        /**
     * @param listeSurfHaut the listeSurfHaut to set
     */
        public void setListeSurfHaut(final float[] listeSurfHaut) {
            this.listeSurfHaut = listeSurfHaut;
        }

        /**
     * @param recAm the recAm to set
     */
        public void setRecAm(final int recAm) {
            this.recAm = recAm;
        }

        /**
     * @param recAv the recAv to set
     */
        public void setRecAv(final int recAv) {
            this.recAv = recAv;
        }
    }

    /**
   * Données d'une section dépendant d'un type de branche 0 ou 9
   * 
   * @author cde
   */
    public static class SectionPourBranche0Ou9 implements DonneesSectionPourBranche {

        /** Largeur associée à la loi de déversement */
        private float ulm1;

        /** Largeur associée à la loi de déversement */
        private float ulm2;

        /** Largeur associée à la loi de déversement */
        private float ulm3;

        /** Cote de la limite de lit gauche de zone stockage */
        private float dZg11;

        /** Cote de la limite de lit gauche du lit majeur */
        private float dZg12;

        /** Cote de la limite de lit gauche du lit mineur */
        private float dZg13;

        /** Cote de la limite de lit droite de zone stockage */
        private float dZd11;

        /** Cote de la limite de lit droite du lit majeur */
        private float dZd12;

        /** Cote de la limite de lit droite du lit mineur */
        private float dZd13;

        /** Présence du lit majeur: 0 si absent, 1 sinon */
        private int iSec2;

        /** Présence de zone stockage: 0 si absent, 1 sinon */
        private int iular4;

        /** Strickler à la cote Zhaut */
        private float stric;

        /** Surfaces mouillées tabulées du lit mineur */
        private float[] listeSec1;

        /** Périmètres mouillés tabulés du lit mineur */
        private float[] listePer1;

        /** Largeurs tabulées du lit mineur */
        private float[] listeUlar1;

        /** Surfaces mouillées tabulées du lit majeur */
        private float[] listeSec2;

        /** Périmètres mouillés tabulés du lit majeur */
        private float[] listePer2;

        /** Largeurs tabulées du lit majeur */
        private float[] listeUlar2;

        /** Largeurs tabulées de zone stockage */
        private float[] listeUlar4;

        /** Coefficient Cw1 (lié aux tabulations de sections) */
        private float[] listeCoefW1;

        /** Coefficient Cw2 (lié aux tabulations de sections) */
        private float[] listeCoefW2;

        /**
     * @return the dZd11
     */
        public float getDZd11() {
            return dZd11;
        }

        /**
     * @return the dZd12
     */
        public float getDZd12() {
            return dZd12;
        }

        /**
     * @return the dZd13
     */
        public float getDZd13() {
            return dZd13;
        }

        /**
     * @return the dZg11
     */
        public float getDZg11() {
            return dZg11;
        }

        /**
     * @return the dZg12
     */
        public float getDZg12() {
            return dZg12;
        }

        /**
     * @return the dZg13
     */
        public float getDZg13() {
            return dZg13;
        }

        /**
     * @return the iSec2
     */
        public int getISec2() {
            return iSec2;
        }

        /**
     * @return the iular4
     */
        public int getIular4() {
            return iular4;
        }

        /**
     * @return the listeCoefW1
     */
        public float[] getListeCoefW1() {
            return listeCoefW1;
        }

        /**
     * @return the listeCoefW2
     */
        public float[] getListeCoefW2() {
            return listeCoefW2;
        }

        /**
     * @return the listePer1
     */
        public float[] getListePer1() {
            return listePer1;
        }

        /**
     * @return the listePer2
     */
        public float[] getListePer2() {
            return listePer2;
        }

        /**
     * @return the listeSec1
     */
        public float[] getListeSec1() {
            return listeSec1;
        }

        /**
     * @return the listeSec2
     */
        public float[] getListeSec2() {
            return listeSec2;
        }

        /**
     * @return the listeUlar1
     */
        public float[] getListeUlar1() {
            return listeUlar1;
        }

        /**
     * @return the listeUlar2
     */
        public float[] getListeUlar2() {
            return listeUlar2;
        }

        /**
     * @return the listeUlar4
     */
        public float[] getListeUlar4() {
            return listeUlar4;
        }

        /**
     * @return the stric
     */
        public float getStric() {
            return stric;
        }

        /**
     * @return the ulm1
     */
        public float getUlm1() {
            return ulm1;
        }

        /**
     * @return the ulm2
     */
        public float getUlm2() {
            return ulm2;
        }

        /**
     * @return the ulm3
     */
        public float getUlm3() {
            return ulm3;
        }

        /**
     * @param zd11 the dZd11 to set
     */
        public void setDZd11(final float zd11) {
            dZd11 = zd11;
        }

        /**
     * @param zd12 the dZd12 to set
     */
        public void setDZd12(final float zd12) {
            dZd12 = zd12;
        }

        /**
     * @param zd13 the dZd13 to set
     */
        public void setDZd13(final float zd13) {
            dZd13 = zd13;
        }

        /**
     * @param zg11 the dZg11 to set
     */
        public void setDZg11(final float zg11) {
            dZg11 = zg11;
        }

        /**
     * @param zg12 the dZg12 to set
     */
        public void setDZg12(final float zg12) {
            dZg12 = zg12;
        }

        /**
     * @param zg13 the dZg13 to set
     */
        public void setDZg13(final float zg13) {
            dZg13 = zg13;
        }

        /**
     * @param sec2 the iSec2 to set
     */
        public void setISec2(final int sec2) {
            iSec2 = sec2;
        }

        /**
     * @param iular4 the iular4 to set
     */
        public void setIular4(final int iular4) {
            this.iular4 = iular4;
        }

        /**
     * @param listeCoefW1 the listeCoefW1 to set
     */
        public void setListeCoefW1(final float[] listeCoefW1) {
            this.listeCoefW1 = listeCoefW1;
        }

        /**
     * @param listeCoefW2 the listeCoefW2 to set
     */
        public void setListeCoefW2(final float[] listeCoefW2) {
            this.listeCoefW2 = listeCoefW2;
        }

        /**
     * @param listePer1 the listePer1 to set
     */
        public void setListePer1(final float[] listePer1) {
            this.listePer1 = listePer1;
        }

        /**
     * @param listePer2 the listePer2 to set
     */
        public void setListePer2(final float[] listePer2) {
            this.listePer2 = listePer2;
        }

        /**
     * @param listeSec1 the listeSec1 to set
     */
        public void setListeSec1(final float[] listeSec1) {
            this.listeSec1 = listeSec1;
        }

        /**
     * @param listeSec2 the listeSec2 to set
     */
        public void setListeSec2(final float[] listeSec2) {
            this.listeSec2 = listeSec2;
        }

        /**
     * @param listeUlar1 the listeUlar1 to set
     */
        public void setListeUlar1(final float[] listeUlar1) {
            this.listeUlar1 = listeUlar1;
        }

        /**
     * @param listeUlar2 the listeUlar2 to set
     */
        public void setListeUlar2(final float[] listeUlar2) {
            this.listeUlar2 = listeUlar2;
        }

        /**
     * @param listeUlar4 the listeUlar4 to set
     */
        public void setListeUlar4(final float[] listeUlar4) {
            this.listeUlar4 = listeUlar4;
        }

        /**
     * @param stric the stric to set
     */
        public void setStric(final float stric) {
            this.stric = stric;
        }

        /**
     * @param ulm1 the ulm1 to set
     */
        public void setUlm1(final float ulm1) {
            this.ulm1 = ulm1;
        }

        /**
     * @param ulm2 the ulm2 to set
     */
        public void setUlm2(final float ulm2) {
            this.ulm2 = ulm2;
        }

        /**
     * @param ulm3 the ulm3 to set
     */
        public void setUlm3(final float ulm3) {
            this.ulm3 = ulm3;
        }
    }

    /**
   * WARN: contient les loiZDact pour les section aval et amont des branches de type 6. L'id Crue 10 est utilisé et non
   * l'id lu dans le fichier STR comme pour le reste de cette classe.
   * 
   * @param loiZDactByBranch6 the loiZDactByBranch6 to set
   */
    public void setLoiZDactByIdBranch6(Map<String, float[]> loiZDactByBranch6) {
        this.loiZDactByIdBranch6 = loiZDactByBranch6;
    }
}
