package org.fudaa.dodico.fichiers;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * @author Fred Deniger
 * @version $Id: NativeNIOHelper.java,v 1.4 2006-09-19 14:42:28 deniger Exp $
 */
public class NativeNIOHelper {

    protected final FileChannel channel_;

    ByteBuffer buff_;

    ByteBuffer buff4_;

    ByteOrder order_;

    public final ByteOrder getOrder() {
        return order_;
    }

    public final FileChannel getChannel() {
        return channel_;
    }

    public NativeNIOHelper(final FileChannel _ch) {
        channel_ = _ch;
    }

    public long getCurrentPosition() throws IOException {
        return channel_.position();
    }

    public void inverseOrder() {
        if (buff_ == null) {
            order_ = ByteOrder.LITTLE_ENDIAN;
        } else {
            order_ = (buff_.order() == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
            buff_.order(order_);
        }
    }

    public ByteBuffer getBuffer() {
        return buff_;
    }

    /**
   * @param _nbOct le nombre total d'octets :y compris les 8 octets utilise pour le caractere sequentiel
   * @return le nombre d'octet non lu. -1 si fin de flux
   * @throws IOException
   */
    public int readAll(final int _nbOct) throws IOException {
        if ((buff_ == null) || buff_.capacity() != _nbOct) {
            buff_ = ByteBuffer.allocate(_nbOct);
            if (order_ != null) {
                buff_.order(order_);
            }
        } else {
            buff_.rewind();
        }
        final int r = channel_.read(buff_);
        buff_.rewind();
        return r;
    }

    public String getStingFromBuffer(final int _nbChar) {
        final byte[] charByte = new byte[_nbChar];
        buff_.get(charByte);
        return new String(charByte);
    }

    public ByteBuffer readData() throws IOException {
        final int i = readSequentialData();
        readAll(i + getLengthForInt());
        return buff_;
    }

    public long getAvailable() throws IOException {
        return channel_.size() - channel_.position();
    }

    public int readSequentialData() throws IOException {
        if (buff4_ == null) {
            buff4_ = ByteBuffer.allocate(getLengthForInt());
            if (order_ != null) {
                buff4_.order(order_);
            }
        }
        buff4_.rewind();
        final int ok = channel_.read(buff4_);
        if (ok != 4) {
            throw new EOFException("problem");
        }
        buff4_.rewind();
        return buff4_.getInt();
    }

    public void skipRecord() throws IOException {
        int toSkip = readSequentialData();
        channel_.position(channel_.position() + 4 + toSkip);
    }

    public int getLengthForInt() {
        return 4;
    }

    public int getLengthForDouble() {
        return 8;
    }

    public int getLengthRecordForDouble(final int _nbDouble) {
        return 8 + _nbDouble * 8;
    }

    public int getLengthRecordForInt(final int _nbInt) {
        return 8 + _nbInt * 4;
    }

    public int getLengthRecordForChar(final int _nbInt) {
        return 8 + _nbInt;
    }
}
