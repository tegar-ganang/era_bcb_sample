package org.fudaa.dodico.reflux.io;

import gnu.trove.TDoubleArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import org.fudaa.dodico.fichiers.ByteBufferInputStream;
import org.fudaa.dodico.fortran.FortranReader;

/**
 * @author Fred Deniger
 * @version $Id: RefluxSolutionSequentielReader.java,v 1.10 2007-06-20 12:21:44 deniger Exp $
 */
public class RefluxSolutionSequentielReader {

    FileChannel ch_;

    final int nbVar_;

    final int nbPoint_;

    final int[] fmt_;

    /**
   * Taille d'une ligne de valeur.
   */
    final long lineLength_;

    /**
   * Taille du delimiteur -999.
   */
    final long delimLength_;

    /**
   * Taille d'un ligne d'entete t=.
   */
    final long enteteLength_;

    final long firstPos_;

    final File f_;

    /**
   * @param _r les resultats
   * @param _f le fichier
   * @throws IOException lancer si le fichier n'existe pas et si impossible de locker le fichier
   */
    public RefluxSolutionSequentielReader(final RefluxRefondeSolutionSequentielResult _r, final File _f) {
        super();
        nbVar_ = _r.nbVar_;
        nbPoint_ = _r.nbPoint_;
        f_ = _f;
        fmt_ = new int[nbVar_ + 1];
        final int temp = _r.ft_.getDataColLength();
        lineLength_ = _r.lineLength_;
        firstPos_ = _r.firstPost_;
        enteteLength_ = _r.enteteLength_;
        delimLength_ = _r.delimLength_;
        Arrays.fill(fmt_, 0, fmt_.length, temp);
        fmt_[0] = _r.sizeFirstCol_;
    }

    private void buildChannel() throws IOException {
        if (ch_ == null) {
            ch_ = new FileInputStream(f_).getChannel();
        }
    }

    protected long getPosition(final int _timeStep) {
        return firstPos_ + _timeStep * getOffset();
    }

    private long getOffset() {
        return delimLength_ + enteteLength_ + nbPoint_ * lineLength_;
    }

    /**
   * @return nombre de pas de temps devin� � partir de la taille du fichier
   * @throws IOException
   */
    public int getNbTimeStepGuessed() throws IOException {
        final long size = ch_ == null ? f_.length() : ch_.size();
        final long taille = size - firstPos_;
        final long tailleData = nbPoint_ * lineLength_;
        final long tailleBloc = tailleData + delimLength_;
        if (taille < tailleData) {
            return 0;
        }
        return (int) (2L + (taille - tailleBloc - tailleData - enteteLength_) / (tailleBloc + enteteLength_));
    }

    public double[] readTimeStep(final int _beginTimeStepIdx, final int _nb) throws IOException {
        final ByteBuffer buf = ByteBuffer.allocate((int) enteteLength_);
        final int[] fmtEtape = RefluxRefondeSolutionFileFormat.getInstance().getEtapeFormat();
        int debTimeStep = 0;
        for (int i = 0; i < fmtEtape.length - 1; i++) {
            debTimeStep += fmtEtape[i];
        }
        long pos = getPosition(_beginTimeStepIdx);
        pos -= enteteLength_;
        final TDoubleArrayList timeStep = new TDoubleArrayList(_nb);
        final long offset = getOffset();
        buildChannel();
        for (int i = 0; i < _nb; i++) {
            ch_.position(pos);
            buf.rewind();
            ch_.read(buf);
            buf.rewind();
            final String str = new String(buf.array()).substring(debTimeStep);
            timeStep.add(Double.parseDouble(str));
            pos += offset;
        }
        return timeStep.toNativeArray();
    }

    ByteBuffer simple_;

    ByteBuffer complexe_;

    /**
   * @param _varIdx l'indice de la variable demandee
   * @param _timeStep l'indice du pas de temps
   * @param _idxPt l'indice du point
   * @return la valeur demande
   * @throws IOException si impossible de lire le fichier
   */
    public double read(final int _varIdx, final int _timeStep, final int _idxPt) throws IOException {
        FortranReader r = null;
        buildChannel();
        try {
            if (simple_ == null) {
                simple_ = ByteBuffer.allocateDirect((int) lineLength_);
            } else {
                simple_.rewind();
            }
            ch_.read(simple_, getPosition(_timeStep) + _idxPt * lineLength_);
            simple_.rewind();
            final ByteBufferInputStream in = new ByteBufferInputStream(simple_);
            r = new FortranReader(new LineNumberReader(new InputStreamReader(in)));
            r.readFields(fmt_);
            return r.doubleField(_varIdx + 1);
        } catch (final IOException e) {
            throw e;
        } finally {
            if (r != null) {
                r.close();
            }
        }
    }

    /**
   * @param _varIdx le tableau des valeurs. Attention commence a 0 a partir de la colonne 1. La premiere colonne etant
   *          l'index du point
   * @param _timeStep le pas de temps demande
   * @param _d d[col.length][nbPoint]
   * @throws IOException
   */
    public void read(final int[] _varIdx, final int _timeStep, final double[][] _d) throws IOException {
        buildChannel();
        FortranReader r = null;
        try {
            if (complexe_ == null) {
                complexe_ = ByteBuffer.allocateDirect((int) (nbPoint_ * lineLength_));
            } else {
                complexe_.rewind();
            }
            ch_.read(complexe_, getPosition(_timeStep));
            complexe_.rewind();
            final ByteBufferInputStream in = new ByteBufferInputStream(complexe_);
            int idx = 0;
            r = new FortranReader(new LineNumberReader(new InputStreamReader(in)));
            while (idx < nbPoint_) {
                r.readFields(fmt_);
                for (int i = _varIdx.length - 1; i >= 0; i--) {
                    _d[i][idx] = r.doubleField(_varIdx[i] + 1);
                }
                idx++;
            }
        } catch (final NumberFormatException _e) {
            _e.printStackTrace();
        } catch (final IOException e) {
            throw e;
        } finally {
            if (r != null) {
                r.close();
            }
        }
    }

    /**
   * Ferme le channel.
   * 
   * @throws IOException
   */
    public void close() throws IOException {
        if (ch_ != null) {
            ch_.close();
        }
    }

    /**
   * @return the nbPoint
   */
    public int getNbPoint() {
        return nbPoint_;
    }
}
