package org.fudaa.dodico.ef.io.serafin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import com.memoire.fu.FuVectordouble;
import org.fudaa.ctulu.CtuluAnalyze;
import org.fudaa.ctulu.CtuluLibMessage;
import org.fudaa.ctulu.CtuluLibString;
import org.fudaa.ctulu.ProgressionUpdater;
import org.fudaa.ctulu.fileformat.FileReadOperationAbstract;
import org.fudaa.ctulu.fileformat.FortranInterface;
import org.fudaa.ctulu.fileformat.FortranLib;
import org.fudaa.dodico.ef.EfElement;
import org.fudaa.dodico.ef.EfElementType;
import org.fudaa.dodico.ef.EfNodeMutable;
import org.fudaa.dodico.ef.impl.EfGridArray;
import org.fudaa.dodico.ef.io.EfIOResource;
import org.fudaa.dodico.fichiers.NativeNIOHelper;

/**
 * @author Fred Deniger
 * @version $Id: SerafinNewReader.java,v 1.17 2007-01-19 13:07:22 deniger Exp $
 */
public class SerafinNewReader extends FileReadOperationAbstract {

    /**
   * @return the isVolumique
   */
    public boolean isVolumique() {
        return isVolumique_;
    }

    /**
   * @param _isVolumique the isVolumique to set
   */
    public void setVolumique(boolean _isVolumique) {
        isVolumique_ = _isVolumique;
    }

    FileInputStream in_;

    NativeNIOHelper helper_;

    File file_;

    boolean isVolumique_;

    boolean onlyReadLast_;

    long readTimeStepFrom_ = -1;

    public SerafinNewReader() {
        this(false);
    }

    public SerafinNewReader(boolean isVolumique) {
        isVolumique_ = isVolumique;
    }

    /**
   * @return true si on doit lire que le dernier pas de temps
   */
    public final boolean isOnlyReadLast() {
        return onlyReadLast_;
    }

    /**
   * @param _onlyReadLast true si on doit lire que le dernier pas de temps
   */
    public final void setOnlyReadLast(final boolean _onlyReadLast) {
        onlyReadLast_ = _onlyReadLast;
    }

    protected FortranInterface getFortranInterface() {
        return FortranLib.getFortranInterface(in_);
    }

    protected Object internalRead() {
        helper_ = new NativeNIOHelper(in_.getChannel());
        final SerafinAdapter inter = new SerafinAdapter();
        SerafinNewReaderInfo info = null;
        try {
            helper_.readAll(88);
            ByteBuffer bf = helper_.getBuffer();
            int tempInt = bf.getInt();
            if (tempInt != 80) {
                helper_.inverseOrder();
            }
            tempInt = bf.getInt(0);
            if (tempInt != 80) {
                analyze_.addFatalError(EfIOResource.getS("La taille du premier bloc est incorrect"), (int) helper_.getCurrentPosition());
                return null;
            }
            inter.setTitre(helper_.getStingFromBuffer(80).trim());
            helper_.readData();
            bf = helper_.getBuffer();
            final int nbv1 = bf.getInt();
            if (nbv1 < 0) {
                analyze_.addFatalError(EfIOResource.getS("Le nombre de variables est nul ou n�gatif"));
                return null;
            }
            final int nbv2 = bf.getInt();
            if (nbv2 > 0) {
                analyze_.addWarn(EfIOResource.getS("Les variables de seconde discretisation seront ignor�es"), -1);
            }
            inter.setNbv1(nbv1);
            inter.setNbv2(nbv2);
            final String[] nomVariables = isReadOnlyTimeStep() ? null : new String[nbv1];
            final String[] uniteVariables = isReadOnlyTimeStep() ? null : new String[nbv1];
            for (int i = 0; i < nbv1; i++) {
                helper_.readData();
                if (!isReadOnlyTimeStep()) {
                    bf = helper_.getBuffer();
                    nomVariables[i] = helper_.getStingFromBuffer(16).trim();
                    uniteVariables[i] = helper_.getStingFromBuffer(16).trim();
                }
            }
            inter.setNomVariables(nomVariables);
            inter.setUniteVariables(uniteVariables);
            for (int i = 0; i < nbv2; i++) {
                helper_.skipRecord();
            }
            final int nbParam = SerafinFileFormat.IPARAM_NB;
            final int[] iparam = new int[nbParam];
            helper_.readData();
            bf = helper_.getBuffer();
            for (int i = 0; i < nbParam; i++) {
                iparam[i] = bf.getInt();
            }
            inter.setIparam(iparam);
            if (SerafinFileFormat.isIdateDefiniCommon(iparam)) {
                bf = helper_.readData();
                if (!isReadOnlyTimeStep()) {
                    final int y = bf.getInt();
                    final int m = bf.getInt();
                    final int j = bf.getInt();
                    final int h = bf.getInt();
                    final int min = bf.getInt();
                    final int s = bf.getInt();
                    final Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, y);
                    cal.set(Calendar.MONTH, m);
                    cal.set(Calendar.DAY_OF_MONTH, j);
                    cal.set(Calendar.HOUR_OF_DAY, h);
                    cal.set(Calendar.MINUTE, min);
                    cal.set(Calendar.SECOND, s);
                    cal.set(Calendar.MILLISECOND, 0);
                    inter.setIdateInMillis(cal.getTime().getTime());
                }
            }
            bf = helper_.readData();
            final int nelem1 = bf.getInt();
            final int npoin1 = bf.getInt();
            final int nppel1 = bf.getInt();
            inter.setIdisc1(bf.getInt());
            final EfNodeMutable[] points = isReadOnlyTimeStep() ? null : new EfNodeMutable[npoin1];
            final EfElement[] elements = isReadOnlyTimeStep() ? null : new EfElement[nelem1];
            if (nbv2 > 0) {
                helper_.skipRecord();
            }
            bf = helper_.readData();
            int[] indexElem;
            if (!isReadOnlyTimeStep()) {
                for (int i = 0; i < nelem1; i++) {
                    indexElem = new int[nppel1];
                    for (int j = 0; j < nppel1; j++) {
                        indexElem[j] = bf.getInt() - 1;
                    }
                    elements[i] = new EfElement(indexElem);
                }
            }
            if (nbv2 > 0) {
                helper_.skipRecord();
            }
            int[] ipobo1 = isReadOnlyTimeStep() ? null : new int[npoin1];
            int[] ipoboInit = isReadOnlyTimeStep() ? null : new int[npoin1];
            bf = helper_.readData();
            int temp;
            int index = 0;
            if (!isReadOnlyTimeStep()) {
                for (int i = 0; i < npoin1; i++) {
                    temp = bf.getInt();
                    ipoboInit[i] = temp;
                    if (temp > 0) {
                        index++;
                        if (temp < npoin1) {
                            ipobo1[temp - 1] = i;
                        }
                    }
                }
                final int[] tempipobo1 = new int[index];
                System.arraycopy(ipobo1, 0, tempipobo1, 0, index);
                inter.setIpoboInitial(ipoboInit);
                inter.setIpoboFr(tempipobo1);
                if (progress_ != null) {
                    if (CtuluLibMessage.DEBUG) {
                        CtuluLibMessage.debug("lecture frontiere ok");
                    }
                    progress_.setProgression(10);
                }
            }
            if (nbv2 > 0) {
                helper_.skipRecord();
            }
            bf = helper_.readData();
            if (!isReadOnlyTimeStep()) {
                for (int i = 0; i < npoin1; i++) {
                    points[i] = new EfNodeMutable();
                    float x = bf.getFloat();
                    points[i].setX(x);
                }
                if (progress_ != null) {
                    if (CtuluLibMessage.DEBUG) {
                        CtuluLibMessage.debug("lecture X");
                    }
                    progress_.setProgression(20);
                }
            }
            bf = helper_.readData();
            if (!isReadOnlyTimeStep()) {
                for (int i = 0; i < npoin1; i++) {
                    float y = bf.getFloat();
                    points[i].setY(y);
                }
                if (progress_ != null) {
                    if (CtuluLibMessage.DEBUG) {
                        CtuluLibMessage.debug("lecture Y");
                    }
                    progress_.setProgression(30);
                }
            }
            if (nbv2 > 0) {
                helper_.skipRecord();
                helper_.skipRecord();
            }
            int tempo;
            int nbPtOrElt = isVolumique_ ? nelem1 : npoin1;
            if (SerafinFileFormat.isFormatEnColonneCommon(iparam)) {
                tempo = 12 + 4 * nbv1 * nbPtOrElt + 8 * nbv1;
            } else {
                tempo = 12 + 4 * nbv1 * nbPtOrElt;
            }
            final int nbPasTempsEstime = (int) (helper_.getAvailable() / tempo);
            if (CtuluLibMessage.DEBUG) {
                CtuluLibMessage.debug("nombre pas de temps " + nbPasTempsEstime);
            }
            FuVectordouble vectorTemps = new FuVectordouble(isOnlyReadLast() ? 2 : nbPasTempsEstime + 1);
            if (nbv1 > 0) {
                final boolean isFormatColonne = SerafinFileFormat.isFormatEnColonneCommon(iparam);
                final ProgressionUpdater up = new ProgressionUpdater(progress_);
                up.setValue(7, nbPasTempsEstime, 30, 70);
                info = new SerafinNewReaderInfo(nbPtOrElt, helper_.getCurrentPosition(), file_, isVolumique_);
                info.setColonne(isFormatColonne);
                info.setTimeEnrLength(tempo);
                info.setOrder(helper_.getOrder());
                inter.setInfo(info);
                helper_.readSequentialData();
                long nextPos;
                final FileChannel ch = helper_.getChannel();
                if (onlyReadLast_) {
                    ch.position(ch.position() + tempo * (nbPasTempsEstime - 1));
                } else if (isReadOnlyTimeStep()) {
                    ch.position(ch.position() + (tempo * (this.readTimeStepFrom_)));
                }
                while (helper_.getAvailable() > 0) {
                    nextPos = ch.position() + tempo;
                    helper_.readAll(4);
                    bf = helper_.getBuffer();
                    vectorTemps.addElement(bf.getFloat());
                    ch.position(nextPos);
                    up.majAvancement();
                }
                inter.setPasDeTemps(vectorTemps.toArray());
                vectorTemps = null;
                if (!isOnlyReadLast() && !isReadOnlyTimeStep() && inter.getTimeStepNb() != nbPasTempsEstime) {
                    analyze_.addInfo(EfIOResource.getS("Nb pas de temps mal estim� (estim� {0}, lu {1})", CtuluLibString.getString(nbPasTempsEstime), CtuluLibString.getString(inter.getTimeStepNb())), 0);
                }
            }
            if (!isReadOnlyTimeStep()) {
                EfElementType type = EfElementType.getCommunType(elements[0].getPtNb());
                if (type == EfElementType.T6) type = EfElementType.T3_FOR_3D;
                inter.setMaillage(new EfGridArray(points, elements, type));
            }
        } catch (final IOException e) {
            analyze_.manageException(e);
        } finally {
            if (helper_ != null) {
                try {
                    helper_.getChannel().close();
                } catch (final IOException e1) {
                    analyze_.manageException(e1);
                }
            }
        }
        return inter;
    }

    public void setFile(final File _f) {
        file_ = _f;
        analyze_ = new CtuluAnalyze();
        analyze_.setDesc(super.getOperationDescription(_f));
        try {
            in_ = new FileInputStream(_f);
        } catch (final FileNotFoundException _e) {
            analyze_.manageException(_e);
        }
    }

    public long getReadTimeStepFrom() {
        return readTimeStepFrom_;
    }

    public boolean isReadOnlyTimeStep() {
        return readTimeStepFrom_ >= 0;
    }

    /**
   * @param _readTimeStepFrom si diff de -1, seul les pas de temps seront lu et cela � partir du pas de temps donn�e
   */
    public void setReadTimeStepFrom(final int _readTimeStepFrom) {
        readTimeStepFrom_ = _readTimeStepFrom;
    }
}
