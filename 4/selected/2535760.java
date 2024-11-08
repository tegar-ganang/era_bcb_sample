package org.fudaa.dodico.ef.io.serafin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;

/**
 * @author Fred Deniger
 * @version $Id: SerafinNewReaderInfo.java,v 1.10 2006-11-14 09:05:28 deniger Exp $
 */
public class SerafinNewReaderInfo {

    ByteBuffer buffer_;

    FileChannel channel_;

    final File file_;

    private final long firstTimeStepPos_;

    private boolean isColonne_;

    final int nbValues_;

    int nbVar_;

    ByteBuffer oneDouble_;

    private ByteOrder order_;

    private long timeEnrLength_;

    /** Cache pour getDouble(final int _nbV, final int _timeStep) */
    private HashMap<Integer, HashMap<Integer, SoftReference<double[]>>> cacheGetDouble_;

    final boolean volumique_;

    /**
   * @param _nbValue le nombre de valeur definies. Peut etre le nombre de noeud ou le nombre d'element si resultat
   *          volumique.
   * @param _channel
   */
    public SerafinNewReaderInfo(final int _nbValue, final long _pos, final File _file, boolean _volumique) {
        super();
        cacheGetDouble_ = new HashMap<Integer, HashMap<Integer, SoftReference<double[]>>>();
        nbValues_ = _nbValue;
        file_ = _file;
        firstTimeStepPos_ = _pos;
        volumique_ = _volumique;
    }

    public void close() {
        if (channel_ == null) {
            return;
        }
        try {
            channel_.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    protected ByteBuffer createBuffer() {
        if (buffer_ == null) {
            buffer_ = ByteBuffer.allocateDirect(4 * nbValues_);
            if (order_ != null) {
                buffer_.order(order_);
            }
        }
        return buffer_;
    }

    protected ByteBuffer createBufferOneDouble() {
        if (oneDouble_ == null) {
            oneDouble_ = ByteBuffer.allocateDirect(4);
            if (order_ != null) {
                oneDouble_.order(order_);
            }
        }
        return oneDouble_;
    }

    private void createChannel() throws IOException {
        if (channel_ == null) {
            channel_ = new FileInputStream(file_).getChannel();
        }
    }

    /**
   * @param _nbV le nombre de variable
   * @param _timeStep le pas de temps
   * @return les valeurs
   */
    public synchronized double[] getDouble(final int _nbV, final int _timeStep) throws IOException {
        if (!cacheGetDouble_.containsKey(_nbV)) {
            if (cacheGetDouble_.size() > 5) {
                cacheGetDouble_.clear();
            }
            cacheGetDouble_.put(_nbV, new HashMap<Integer, SoftReference<double[]>>());
        }
        HashMap<Integer, SoftReference<double[]>> hashMap = cacheGetDouble_.get(_nbV);
        SoftReference<double[]> softReference = hashMap.get(_timeStep);
        double[] res = null;
        if (softReference != null) {
            res = softReference.get();
        }
        if (res == null) {
            if (hashMap.size() > 5) {
                hashMap.clear();
            }
            createBuffer();
            createChannel();
            buffer_.rewind();
            long pos = firstTimeStepPos_ + _timeStep * timeEnrLength_;
            if (isColonne_) {
                pos += 12L + _nbV * (8L + nbValues_ * 4L) + 4L;
            } else {
                pos += 4L + 4L + (_nbV * nbValues_ * 4L);
            }
            res = new double[nbValues_];
            if (!channel_.isOpen()) {
                return null;
            }
            channel_.position(pos);
            channel_.read(buffer_);
            buffer_.rewind();
            for (int i = 0; i < nbValues_; i++) {
                res[i] = buffer_.getFloat();
            }
            hashMap.put(_timeStep, new SoftReference<double[]>(res));
        }
        return res;
    }

    public synchronized double getDouble(final int _nbV, final int _timeStep, final int _idxPt) throws IOException {
        createChannel();
        createBufferOneDouble();
        long pos = firstTimeStepPos_ + _timeStep * timeEnrLength_;
        if (isColonne_) {
            pos += 12 + _nbV * (8 + nbValues_ * 4) + 4;
        } else {
            pos += 4 + 4 + (_nbV * nbValues_ * 4);
        }
        pos += _idxPt * 4;
        channel_.position(pos);
        oneDouble_.rewind();
        channel_.read(oneDouble_);
        oneDouble_.rewind();
        return oneDouble_.getFloat();
    }

    public final long getFirstTimeStepPos() {
        return firstTimeStepPos_;
    }

    public final ByteOrder getOrder() {
        return order_;
    }

    public final long getTimeEnrLength() {
        return timeEnrLength_;
    }

    public int getTimeStepAvailable() throws IOException {
        final long size = channel_ == null ? file_.length() : channel_.size();
        return (int) ((size - firstTimeStepPos_) / timeEnrLength_);
    }

    /**
   * @return the volumique
   */
    public boolean isVolumique() {
        return volumique_;
    }

    public final void setColonne(final boolean _isColonne) {
        isColonne_ = _isColonne;
    }

    public final void setOrder(final ByteOrder _order) {
        order_ = _order;
    }

    public final void setTimeEnrLength(final int _timeEnrLength) {
        timeEnrLength_ = _timeEnrLength;
    }
}
