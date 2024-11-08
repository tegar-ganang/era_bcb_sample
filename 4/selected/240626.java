package org.fudaa.dodico.rubar.io;

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
import org.fudaa.dodico.h2d.rubar.H2dRubarSI;

/**
 * @author Fred Deniger
 * @version $Id: RubarSolutionSequentielReader.java,v 1.7 2007-03-23 17:21:53 deniger Exp $
 */
public class RubarSolutionSequentielReader {

    FileChannel ch_;

    ByteBuffer complexe_;

    int nbElt_;

    int nbLigneByValues_;

    int nbVar_;

    ByteBuffer simple_;

    long timeLength_;

    long valueLastLineLength_;

    long valueLineLength_;

    int[] fmt_;

    /**
   * @return le nombre d'element
   */
    public final int getNbElt() {
        return nbElt_;
    }

    /**
   * @return le nombre de variables geree par ce lecteur
   */
    public final int getNbVar() {
        return nbVar_;
    }

    /**
   * @param _f le fichier en question
   * @param _r les resultats
   * @throws IOException si on ne peut pas le fichier _f
   */
    public RubarSolutionSequentielReader(final File _f, final RubarSolutionSequentielResult _r) throws IOException {
        ch_ = new FileInputStream(_f).getChannel();
        nbVar_ = _r.nbVar_;
        timeLength_ = _r.timeLength_;
        valueLineLength_ = _r.resultLength_;
        nbLigneByValues_ = _r.nbLigne_;
        valueLastLineLength_ = _r.resultLastLineLength_;
        nbElt_ = _r.nbElt_;
        fmt_ = new int[8];
        Arrays.fill(fmt_, 10);
    }

    /**
   * @param _nb le nombre de caract�res utilis�es pour d�crire une valeur (un double. Par defaut vaut 10.
   */
    public void setDoubleLength(final int _nbValueByLigne, final int _nb) {
        fmt_ = new int[_nbValueByLigne];
        Arrays.fill(fmt_, _nb);
    }

    /**
   * Ferme le channel.
   * 
   * @throws IOException
   */
    public void close() throws IOException {
        ch_.close();
    }

    /**
   * @return la taille pour une variable
   */
    protected long getVarLength() {
        return (nbLigneByValues_ - 1) * valueLineLength_ + valueLastLineLength_;
    }

    /**
   * @param _time le pas de temps
   * @return la position de la premier ligne des valeurs du pas de temps demande
   */
    protected long getPosition(final int _time) {
        return _time * (timeLength_ + nbVar_ * getVarLength()) + timeLength_;
    }

    /**
   * @param _varIdx l'indice de la variable demandee
   * @param _timeStep l'indice du pas de temps
   * @param _idxPt l'indice du point
   * @return la valeur demande
   * @throws IOException si impossible de lire le fichier
   */
    public double read(final int _varIdx, final int _timeStep, final int _idxPt) throws IOException {
        FortranReader r = null;
        try {
            if (simple_ == null) {
                simple_ = ByteBuffer.allocateDirect((int) valueLineLength_);
            } else {
                simple_.rewind();
            }
            long pos = getPosition(_timeStep);
            pos += _varIdx * getVarLength();
            final int nbLigneToSkip = (int) Math.floor((double) _idxPt / (double) fmt_.length);
            pos += nbLigneToSkip * valueLineLength_;
            final int posOnLine = _idxPt - nbLigneToSkip * fmt_.length;
            ch_.read(simple_, pos);
            simple_.rewind();
            final ByteBufferInputStream in = new ByteBufferInputStream(simple_);
            r = new FortranReader(new LineNumberReader(new InputStreamReader(in)));
            r.readFields(fmt_);
            return r.doubleField(posOnLine);
        } catch (final IOException e) {
            throw e;
        } finally {
            if (r != null) {
                r.close();
            }
        }
    }

    /**
   * @param _timeStep le pas de temps demande
   * @param _d d[col.length][nbPoint]
   * @return null si pas d'erreur. Sinon renvoie une erreur
   * @throws IOException
   */
    public boolean read(final int _timeStep, final double[][] _d) throws IOException {
        FortranReader r = null;
        boolean result = true;
        try {
            if (complexe_ == null) {
                complexe_ = ByteBuffer.allocateDirect((int) (nbVar_ * getVarLength()));
            } else {
                complexe_.rewind();
            }
            ch_.read(complexe_, getPosition(_timeStep));
            complexe_.rewind();
            final ByteBufferInputStream in = new ByteBufferInputStream(complexe_);
            r = new FortranReader(new LineNumberReader(new InputStreamReader(in)));
            r.setBlankZero(true);
            final int fmtTaille = fmt_.length;
            for (int i = 0; i < nbVar_; i++) {
                r.readFields(fmt_);
                int tmpOnLine = 0;
                for (int ie = 0; ie < nbElt_; ie++) {
                    if (tmpOnLine == fmtTaille) {
                        r.readFields(fmt_);
                        tmpOnLine = 0;
                    }
                    final String str = r.stringField(tmpOnLine);
                    if (str != null && str.indexOf('*') >= 0) {
                        _d[i][ie] = H2dRubarSI.INDETERMINED_VALUE;
                        result = false;
                    } else {
                        _d[i][ie] = r.doubleField(tmpOnLine);
                    }
                    tmpOnLine++;
                }
            }
        } catch (final IOException e) {
            throw e;
        } finally {
            if (r != null) {
                r.close();
            }
        }
        return result;
    }
}
