package org.fudaa.dodico.crue.io.neuf;

import gnu.trove.TDoubleArrayList;
import gnu.trove.TLongArrayList;
import gnu.trove.TObjectIntHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.dodico.crue.metier.emh.AbstractInfosEMH;
import org.fudaa.dodico.crue.metier.emh.EMH;
import org.fudaa.dodico.crue.metier.emh.EnumInfosEMH;
import org.fudaa.dodico.crue.metier.emh.InfosEMH;
import org.fudaa.dodico.crue.metier.emh.ResCalcul;
import org.fudaa.dodico.crue.metier.transformer.ToStringHelper;

/**
 * Structure de données fcb construite à la volée avec les infos fcb qui vont bien. garde en memoire les indices des pas
 * de temps, les noeuds utilises, les branches, l'indice de regulation, le nb de pas de temps crue et regulation.
 * 
 * @author Adrien Hadoux
 */
public class FCBSequentialReader {

    private final class DataContainerEmpty extends EnteteContainer {

        public DataContainerEmpty() {
            super(Collections.emptyList(), null);
        }
    }

    /**
   * @author denf01a
   * @creation 27 mai 2009
   * @version
   * @param <T> l'entete
   * @param <R> le resultat
   */
    public class EnteteContainer<T extends FCBValueObject.AbstractEntete, R extends FCBValueObject.AbstractRes> {

        private final List<T> listData;

        private final ResBuilder<R> resBuilder;

        private final TObjectIntHashMap namePos;

        private final int nb;

        protected EnteteContainer(final List<T> struc, final ResBuilder<R> resBuilder) {
            super();
            this.listData = Collections.unmodifiableList(struc);
            this.resBuilder = resBuilder;
            namePos = new TObjectIntHashMap();
            int idx = 0;
            for (final T t : struc) {
                String nom = t.getNom();
                if (StringUtils.isNotBlank(nom)) {
                    namePos.put(nom.toUpperCase(), idx);
                }
                idx++;
            }
            nb = listData.size();
        }

        /**
     * @param emh l'emh en question
     * @return le resultat contenu dans une infoEMH
     */
        public ResultatCalcul createInfoEMH(EMH emh) {
            return new ResultatCalcul<T, R>(emh, getPosition(emh.getId()), this);
        }

        public ResultatCalcul createInfoEMH(EMH emh, int position) {
            return new ResultatCalcul<T, R>(emh, position, this);
        }

        /**
     * @param nom le nom
     * @return la position de lecture. -1 si non present
     */
        public T getData(final String nom) {
            final int i = getPosition(nom);
            return i < 0 ? null : getListData().get(i);
        }

        /**
     * @param nom le nom
     * @return la position de lecture. -1 si non present
     */
        public T getData(final int i) {
            return (i < 0 || i >= getListData().size()) ? null : getListData().get(i);
        }

        /**
     * @param idx l'indice
     * @return le nom de la donnes
     */
        public String getDataName(final int idx) {
            return listData.get(idx).getNom();
        }

        /**
     * @return liste non modifiable des donnees.
     */
        public List<T> getListData() {
            return listData;
        }

        /**
     * @return le nombre de données
     */
        public int getNbData() {
            return nb;
        }

        public ResultatCalculPasDeTemps getPasDeTemps() {
            return FCBSequentialReader.this.getPdt();
        }

        /**
     * @param nom le nom
     * @return la position de lecture. -1 si non present
     */
        public int getPosition(final String nom) {
            if (namePos.contains(nom)) {
                return namePos.get(nom);
            }
            return -1;
        }

        protected ResBuilder<R> getResBuilder() {
            return resBuilder;
        }

        /**
     * @param idxPt
     * @param nomEMH
     * @param inout
     * @return le resultat lu
     * @throws IOException
     */
        public R read(final int idxPt, final String nomEMH, final R inout) throws IOException {
            return getResultat(idxPt, nomEMH, inout, this);
        }

        /**
     * @param idxPdt
     * @param nomEMH
     * @param inout
     * @return le resultat lu
     * @throws IOException
     */
        public R read(final int idxPdt, final int nomEMH, final R inout) throws IOException {
            return getResultat(idxPdt, nomEMH, inout, this);
        }
    }

    /**
   * indice des positions de chaque pdt de crue, longueur variable
   */
    public static class ResultatCalculPasDeTemps extends AbstractInfosEMH {

        public EnumInfosEMH getCatType() {
            return EnumInfosEMH.RESULTAT_PAS_DE_TEMPS;
        }

        protected final long[] positions;

        protected final double[] pdts;

        protected final boolean[] isRegu;

        final double[] ruinou;

        /**
     * @param idx le pas de temps demande.
     * @return coefficient de ruissellement au pas de temps idx
     */
        public double getRuinou(final int idx) {
            return ruinou[idx];
        }

        protected ResultatCalculPasDeTemps() {
            positions = ArrayUtils.EMPTY_LONG_ARRAY;
            pdts = ArrayUtils.EMPTY_DOUBLE_ARRAY;
            isRegu = ArrayUtils.EMPTY_BOOLEAN_ARRAY;
            ruinou = ArrayUtils.EMPTY_DOUBLE_ARRAY;
        }

        protected ResultatCalculPasDeTemps(final TLongArrayList tableauIndicesPdtCrue, final TDoubleArrayList pdt, final List<Boolean> isRegu, TDoubleArrayList ruinou) {
            positions = tableauIndicesPdtCrue.toNativeArray();
            this.ruinou = ruinou.toNativeArray();
            pdts = pdt.toNativeArray();
            this.isRegu = ArrayUtils.toPrimitive(isRegu.toArray(new Boolean[isRegu.size()]), false);
        }

        public int getNbPdt() {
            return pdts.length;
        }

        public boolean isRegulation(final int idxPdt) {
            return isRegu[idxPdt];
        }

        public boolean containsRui(final int idxPdt) {
            return !isRegu[idxPdt];
        }

        public double getPdt(final int idxPdt) {
            return pdts[idxPdt];
        }

        public long getPosition(final int idxPdt) {
            return positions[idxPdt];
        }
    }

    /**
   * Interface definissant un contrat permettant de construire un resultat
   * 
   * @author denf01a
   * @creation 27 mai 2009
   * @version
   * @param <T> le resultat voulu
   */
    private interface ResBuilder<T extends FCBValueObject.AbstractRes> {

        /**
     * @return le resultat instancie
     */
        T createRes();

        /**
     * @param idxEnr l'indice de l'enregistrement a lire
     * @return le nombre de byte a sauter pour lire la bonne donnee
     */
        int getOffset(int idxEnr);

        /**
     * @return la longueur des donnees pour un resultat ( pour un profil,noeud ou branche).
     */
        int getTailleEnr();
    }

    private class ResBuilderBranche implements ResBuilder<FCBValueObject.ResBranche> {

        public FCBValueObject.ResBranche createRes() {
            return new FCBValueObject.ResBranche();
        }

        public int getOffset(final int idxEnr) {
            return getTailleBlocProfil() + 4 + idxEnr * TAILLE_ENR_BRANCHE;
        }

        public int getTailleEnr() {
            return TAILLE_ENR_BRANCHE;
        }
    }

    private class ResBuilderNoeud implements ResBuilder<FCBValueObject.ResCasier> {

        public FCBValueObject.ResCasier createRes() {
            return new FCBValueObject.ResCasier();
        }

        public int getOffset(final int idxEnr) {
            return getTailleBlocProfil() + getTailleBlocBranche() + 4 + idxEnr * TAILLE_ENR_NOEUD;
        }

        public int getTailleEnr() {
            return TAILLE_ENR_NOEUD;
        }
    }

    private class ResBuilderProfil implements ResBuilder<FCBValueObject.ResProfil> {

        public FCBValueObject.ResProfil createRes() {
            return new FCBValueObject.ResProfil();
        }

        public int getOffset(final int idxEnr) {
            return 4 + idxEnr * TAILLE_ENR_PROFIL;
        }

        public int getTailleEnr() {
            return TAILLE_ENR_PROFIL;
        }
    }

    public static class ResultatCalcul<T extends FCBValueObject.AbstractEntete, R extends FCBValueObject.AbstractRes> implements InfosEMH {

        final EMH emh;

        final int position;

        final EnteteContainer<T, R> entete;

        /**
     * @param emh
     * @param entete
     */
        public ResultatCalcul(EMH emh, int position, EnteteContainer<T, R> entete) {
            super();
            this.emh = emh;
            this.position = position;
            this.entete = entete;
        }

        public R getResContainer() {
            return entete.getResBuilder().createRes();
        }

        public T getEntete() {
            return entete.getData(position);
        }

        public boolean getActivated() {
            return true;
        }

        public EnumInfosEMH getCatType() {
            return EnumInfosEMH.RESULTAT;
        }

        public EMH getEmh() {
            return emh;
        }

        /**
     * @return les pas de temps du résultats.
     */
        public ResultatCalculPasDeTemps getPasDeTemps() {
            return entete.getPasDeTemps();
        }

        public String getType() {
            return getClass().getSimpleName();
        }

        @Override
        public String getTypei18n() {
            return ToStringHelper.typeToi18n(getType());
        }

        /**
     * @param idxPdt
     * @param inout
     * @return le resultat lu
     * @throws IOException
     */
        public ResCalcul read(final int idxPdt, final R inout) throws IOException {
            R read = entete.read(idxPdt, position, inout);
            return read == null ? null : read.createRes(emh);
        }

        /**
     * @param idxPdt
     * @return
     * @throws IOException
     */
        public ResCalcul read(final int idxPdt) throws IOException {
            R read = entete.read(idxPdt, position, null);
            return read == null ? null : read.createRes(emh);
        }

        public void setEmh(EMH emh) {
        }

        /**
     * @return the position
     */
        public int getPosition() {
            return position;
        }
    }

    private static final int TAILLE_ENR_PROFIL = 20;

    private static final int TAILLE_ENR_BRANCHE = 16;

    private static final int TAILLE_ENR_NOEUD = 12;

    private static final ResultatCalculPasDeTemps EMPTY_PDT = new ResultatCalculPasDeTemps();

    private final EnteteContainer EMPTY = new DataContainerEmpty();

    protected List<String> commentDc;

    protected List<String> commentDh;

    /**
   * le fichier associé.
   */
    protected final File file;

    /**
   * true si le fichier contient une régulation
   */
    protected boolean regulation;

    protected boolean sousFente;

    private ResultatCalculPasDeTemps pdt = EMPTY_PDT;

    private EnteteContainer<FCBValueObject.EnteteProfil, FCBValueObject.ResProfil> containerProfils = EMPTY;

    private EnteteContainer<FCBValueObject.EnteteBranche, FCBValueObject.ResBranche> containerBranches = EMPTY;

    private EnteteContainer<FCBValueObject.EnteteCasier, FCBValueObject.ResCasier> containerCasiers = EMPTY;

    private ByteOrder order;

    private static final Logger LOGGER = Logger.getLogger(FCBSequentialReader.class.getName());

    /**
   * Constructeur de la structure d'accès rapide fcb.
   * 
   * @param file
   */
    public FCBSequentialReader(final File file) {
        this.file = file;
    }

    private ByteBuffer createByteBuffer(final int taille) {
        final ByteBuffer allocate = ByteBuffer.allocate(taille);
        if (order != null) {
            allocate.order(order);
        }
        return allocate;
    }

    /**
   * @return the commentDc
   */
    public List<String> getCommentDc() {
        return commentDc;
    }

    /**
   * @return the commentDh
   */
    public List<String> getCommentDh() {
        return commentDh;
    }

    /**
   * @return le container des branches
   */
    public EnteteContainer<FCBValueObject.EnteteBranche, FCBValueObject.ResBranche> getContainerBranches() {
        return containerBranches;
    }

    /**
   * @return le container des noeuds.
   */
    public EnteteContainer<FCBValueObject.EnteteCasier, FCBValueObject.ResCasier> getContainerCasiers() {
        return containerCasiers;
    }

    /**
   * @return le container des profils
   */
    public EnteteContainer<FCBValueObject.EnteteProfil, FCBValueObject.ResProfil> getContainerProfil() {
        return containerProfils;
    }

    /**
   * @return the nbBranches
   */
    public int getNbBranches() {
        return containerBranches.getNbData();
    }

    /**
   * @return le nombre de noeuds
   */
    public Object getNbNoeuds() {
        return containerCasiers.getNbData();
    }

    /**
   * @return the nbProfils
   */
    public int getNbProfils() {
        return containerProfils.getNbData();
    }

    protected ByteOrder getOrder() {
        return order;
    }

    /**
   * @return les pas de temps normaux
   */
    public ResultatCalculPasDeTemps getPdt() {
        return pdt;
    }

    private <T extends FCBValueObject.AbstractEntete, R extends FCBValueObject.AbstractRes> R getResultat(final int idxPdt, final String nomEMH, final R inout, final EnteteContainer<T, R> dataContainer) throws IOException {
        final int idxBranche = dataContainer.getPosition(nomEMH);
        if (idxBranche < 0) {
            throw new IOException("io.fcb.emh.notFound.error");
        }
        return getResultat(idxPdt, idxBranche, inout, dataContainer);
    }

    private <T extends FCBValueObject.AbstractEntete, R extends FCBValueObject.AbstractRes> R getResultat(final int idxPdt, final int idxBranche, final R inout, final EnteteContainer<T, R> dataContainer) throws IOException {
        final long positionTimeStep = pdt.getPosition(idxPdt);
        if (positionTimeStep < 0) {
            throw new IOException("io.fcb.timeStep.notFound.error");
        }
        final ResBuilder<R> resBuilder = dataContainer.getResBuilder();
        if (resBuilder == null) {
            throw new IOException("io.fcb.emh.noBuilder.error");
        }
        final long deb = positionTimeStep + resBuilder.getOffset(idxBranche);
        final ByteBuffer bf = readByte(resBuilder, deb);
        R res = inout;
        if (res == null) {
            res = resBuilder.createRes();
        }
        res.read(bf);
        return res;
    }

    /**
   * @param idxPdt l'indice du pas de temps normal
   * @param nomBranche le nom de la branche
   * @param inout le resultat a modifier. si null, une nouvelle instance est créée
   * @return le resultat. inout si non null
   * @throws IOException si erreur de lecture
   */
    public FCBValueObject.ResBranche getResultatBranche(final int idxPdt, final String nomBranche, final FCBValueObject.ResBranche inout) throws IOException {
        return getResultat(idxPdt, nomBranche, inout, containerBranches);
    }

    /**
   * @param idxPdt l'indice du pas de temps normal
   * @param nomNoeud le nom de la branche
   * @param inout le resultat a modifier. si null, une nouvelle instance est créée
   * @return le resultat. inout si non null
   * @throws IOException si erreur de lecture
   */
    public FCBValueObject.ResCasier getResultatNoeud(final int idxPdt, final String nomNoeud, final FCBValueObject.ResCasier inout) throws IOException {
        return getResultat(idxPdt, nomNoeud, inout, containerCasiers);
    }

    /**
   * @param idxPdt l'indice du pas de temps normal
   * @param nomProfil le nom du profil
   * @param inout le resultat a modifier. si null, une nouvelle instance est créée
   * @return le resultat. inout si non null
   * @throws IOException si erreur de lecture
   */
    public FCBValueObject.ResProfil getResultatProfil(final int idxPdt, final String nomProfil, final FCBValueObject.ResProfil inout) throws IOException {
        return getResultat(idxPdt, nomProfil, inout, containerProfils);
    }

    /**
   * @param idxPdt l'indice du pas de temps normal
   * @param nomBranche le nom de la branche
   * @param inout le resultat a modifier. si null, une nouvelle instance est créée
   * @return le resultat. inout si non null
   * @throws IOException si erreur de lecture
   */
    public FCBValueObject.ResBranche getResultatBranche(final int idxPdt, final int nomBranche, final FCBValueObject.ResBranche inout) throws IOException {
        return getResultat(idxPdt, nomBranche, inout, containerBranches);
    }

    /**
   * @param idxPdt l'indice du pas de temps normal
   * @param nomNoeud le nom de la branche
   * @param inout le resultat a modifier. si null, une nouvelle instance est créée
   * @return le resultat. inout si non null
   * @throws IOException si erreur de lecture
   */
    public FCBValueObject.ResCasier getResultatNoeud(final int idxPdt, final int nomNoeud, final FCBValueObject.ResCasier inout) throws IOException {
        return getResultat(idxPdt, nomNoeud, inout, containerCasiers);
    }

    /**
   * @param idxPdt l'indice du pas de temps normal
   * @param nomProfil le nom du profil
   * @param inout le resultat a modifier. si null, une nouvelle instance est créée
   * @return le resultat. inout si non null
   * @throws IOException si erreur de lecture
   */
    public FCBValueObject.ResProfil getResultatProfil(final int idxPdt, final int nomProfil, final FCBValueObject.ResProfil inout) throws IOException {
        return getResultat(idxPdt, nomProfil, inout, containerProfils);
    }

    private int getTailleBlocBranche() {
        return 8 + getNbBranches() * TAILLE_ENR_BRANCHE;
    }

    private int getTailleBlocProfil() {
        return 8 + getNbProfils() * TAILLE_ENR_PROFIL;
    }

    /**
   * @return the isRegulation
   */
    public boolean isRegulation() {
        return regulation;
    }

    /**
   * @return the sousFente
   */
    public boolean isSousFente() {
        return sousFente;
    }

    private <R extends FCBValueObject.AbstractRes> ByteBuffer readByte(final ResBuilder<R> resBuilder, final long deb) {
        final ByteBuffer bf = createByteBuffer(resBuilder.getTailleEnr());
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            final FileChannel channel = fileInputStream.getChannel();
            channel.read(bf, deb);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "readByte", e);
        } finally {
            CtuluLibFile.close(fileInputStream);
        }
        bf.rewind();
        return bf;
    }

    /**
   * @param branches the branches to set
   */
    protected void setBranches(final List<FCBValueObject.EnteteBranche> branches) {
        this.containerBranches = new EnteteContainer<FCBValueObject.EnteteBranche, FCBValueObject.ResBranche>(branches, new ResBuilderBranche());
    }

    protected void setNoeuds(final List<FCBValueObject.EnteteCasier> noeuds) {
        this.containerCasiers = new EnteteContainer<FCBValueObject.EnteteCasier, FCBValueObject.ResCasier>(noeuds, new ResBuilderNoeud());
    }

    protected void setOrder(final ByteOrder order) {
        this.order = order;
    }

    protected void setPdt(final ResultatCalculPasDeTemps pdt) {
        this.pdt = pdt.getNbPdt() == 0 ? EMPTY_PDT : pdt;
    }

    /**
   * @param profils the profils to set
   */
    protected void setProfils(final List<FCBValueObject.EnteteProfil> profils) {
        this.containerProfils = new EnteteContainer<FCBValueObject.EnteteProfil, FCBValueObject.ResProfil>(profils, new ResBuilderProfil());
    }
}
