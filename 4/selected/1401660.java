package org.fudaa.dodico.telemac.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * @author Fred Deniger
 * @version $Id: SerafinNewReaderInfo.java,v 1.10 2006-11-14 09:05:28 deniger Exp $
 */
public class SerafinNewReaderInfo {

    private final long firstTimeStepPos_;

    private boolean isColonne_;

    private ByteOrder order_;

    private long timeEnrLength_;

    ByteBuffer buffer_;

    FileChannel channel_;

    int nbPoint_;

    final File file_;

    int nbVar_;

    /**
   * @param _nbPoint
   * @param _channel
   */
    public SerafinNewReaderInfo(final int _nbPoint, final long _pos, final File _file) {
        super();
        nbPoint_ = _nbPoint;
        file_ = _file;
        firstTimeStepPos_ = _pos;
    }

    private void createChannel() throws IOException {
        if (channel_ == null) {
            channel_ = new FileInputStream(file_).getChannel();
        }
    }

    protected ByteBuffer createBuffer() {
        if (buffer_ == null) {
            buffer_ = ByteBuffer.allocateDirect(4 * nbPoint_);
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

    ByteBuffer oneDouble_;

    public synchronized double getDouble(final int _nbV, final int _timeStep, final int _idxPt) throws IOException {
        createChannel();
        createBufferOneDouble();
        long pos = firstTimeStepPos_ + _timeStep * timeEnrLength_;
        if (isColonne_) {
            pos += 12 + _nbV * (8 + nbPoint_ * 4) + 4;
        } else {
            pos += 4 + 4 + (_nbV * nbPoint_ * 4);
        }
        pos += _idxPt * 4;
        channel_.position(pos);
        oneDouble_.rewind();
        channel_.read(oneDouble_);
        oneDouble_.rewind();
        return oneDouble_.getFloat();
    }

    public int getTimeStepAvailable() throws IOException {
        final long size = channel_ == null ? file_.length() : channel_.size();
        return (int) ((size - firstTimeStepPos_) / timeEnrLength_);
    }

    /**
   * @param _nbV le nombre de variable
   * @param _timeStep le pas de temps
   * @return les valeurs
   */
    public synchronized double[] getDouble(final int _nbV, final int _timeStep) throws IOException {
        createBuffer();
        createChannel();
        buffer_.rewind();
        long pos = firstTimeStepPos_ + _timeStep * timeEnrLength_;
        if (isColonne_) {
            pos += 12L + _nbV * (8L + nbPoint_ * 4L) + 4L;
        } else {
            pos += 4L + 4L + (_nbV * nbPoint_ * 4L);
        }
        final double[] r = new double[nbPoint_];
        if (!channel_.isOpen()) {
            return null;
        }
        channel_.position(pos);
        channel_.read(buffer_);
        buffer_.rewind();
        for (int i = 0; i < nbPoint_; i++) {
            r[i] = buffer_.getFloat();
        }
        return r;
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
