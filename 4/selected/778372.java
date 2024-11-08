package org.fudaa.dodico.rubar.io;

import gnu.trove.TDoubleArrayList;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.memoire.fu.FuLog;
import org.fudaa.ctulu.ProgressionInterface;
import org.fudaa.dodico.fichiers.DodicoLineNumberReader;
import org.fudaa.dodico.fortran.FortranReader;
import org.fudaa.dodico.h2d.resource.H2dResource;

/**
 * Permet de lire les fichiers TPS et TPC. Il faut pr�ciser le nombre d'elements et le nombre de variable. TPS=3
 * variables Transport TPC=3 aussi
 * 
 * @author Fred Deniger
 * @version $Id: RubarSolutionReader.java,v 1.16 2007-06-05 08:59:15 deniger Exp $
 */
public class RubarSolutionReader extends RubarSolutionReaderAbstract {

    boolean isTruncatedResultats_;

    boolean lineError_;

    DodicoLineNumberReader specReader_;

    double timeRemoved_;

    protected long getCurrentPosInReader() {
        return specReader_.getPositionInStream();
    }

    public ProgressionInterface getProg() {
        return super.progress_;
    }

    protected Object internalRead() {
        if (nbElt_ <= 0) {
            return null;
        }
        final TDoubleArrayList time = new TDoubleArrayList();
        final RubarSolutionSequentielResult r = new RubarSolutionSequentielResult();
        r.nbVar_ = nbValues_;
        r.nbElt_ = nbElt_;
        FileChannel ch = null;
        try {
            final String s = in_.readLine();
            time.add(Double.parseDouble(s));
            r.timeLength_ = getCurrentPosInReader();
            r.nbLigne_ = (int) Math.ceil(nbElt_ / nbValueByLigne_);
            in_.readLine();
            long pos = getCurrentPosInReader();
            r.resultLength_ = pos - r.timeLength_;
            if (r.nbLigne_ > 1) {
                for (int i = r.nbLigne_ - 2; i > 0; i--) {
                    in_.readLine();
                }
                pos = getCurrentPosInReader();
                in_.readLine();
                r.resultLastLineLength_ = getCurrentPosInReader() - pos;
            } else {
                r.resultLastLineLength_ = r.resultLength_;
                r.resultLength_ = 0;
            }
            pos = getCurrentPosInReader();
            in_.close();
            in_ = null;
            long lengthForVariable = r.resultLastLineLength_ + (r.nbLigne_ - 1) * r.resultLength_;
            ch = new FileInputStream(f_).getChannel();
            ch.position(pos + (nbValues_ - 1) * lengthForVariable);
            final ByteBuffer buf = ByteBuffer.allocate((int) r.timeLength_);
            lengthForVariable = lengthForVariable * nbValues_;
            while (ch.position() < (ch.size() - r.timeLength_)) {
                buf.rewind();
                if (ch.read(buf) != r.timeLength_) {
                    break;
                }
                buf.rewind();
                final String str = new String(buf.array());
                final char c = str.charAt(str.length() - 1);
                if (c != '\r' && c != '\n') {
                    lineError_ = true;
                }
                try {
                    time.add(Double.parseDouble(str));
                } catch (final NumberFormatException e) {
                    analyze_.addError(e.getMessage(), time.size());
                    isTruncatedResultats_ = true;
                    break;
                }
                ch.position(ch.position() + lengthForVariable);
            }
            if (ch.size() - ch.position() < 0) {
                analyze_.addError(H2dResource.getS("Fichier tronqu�"), -1);
                isTruncatedResultats_ = true;
                timeRemoved_ = time.remove(time.size() - 1);
            }
        } catch (final EOFException e) {
        } catch (final NumberFormatException e) {
            e.printStackTrace();
            analyze_.manageException(e);
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ch != null) {
                    ch.close();
                }
            } catch (final IOException _evt) {
                FuLog.error(_evt);
            }
        }
        r.timeStep_ = time.toNativeArray();
        return r;
    }

    /**
   * On utilise un reader sp�cial qui permet de conna�tre la position.
   */
    protected void setFile(final Reader _r) {
        specReader_ = new DodicoLineNumberReader(_r);
        in_ = new FortranReader(specReader_);
    }

    public final double getTimeRemoved() {
        return timeRemoved_;
    }

    public final boolean isLineError() {
        return lineError_;
    }

    public final boolean isTruncatedResultats() {
        return isTruncatedResultats_;
    }
}
