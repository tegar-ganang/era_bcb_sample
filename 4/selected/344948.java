package org.fudaa.dodico.fortran;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * @author Fred Deniger
 * @version $Id: NativeNIOHelper.java,v 1.1 2004-12-09 12:58:13 deniger Exp $
 */
public class NativeNIOHelper {

    protected FileChannel channel_;

    boolean inverse_;

    ByteBuffer buff_;

    ByteBuffer buff4_;

    ByteOrder order_;

    public final ByteOrder getOrder() {
        return order_;
    }

    public final FileChannel getChannel() {
        return channel_;
    }

    public NativeNIOHelper(FileChannel _ch) {
        channel_ = _ch;
    }

    public long getCurrentPosition() throws IOException {
        return channel_.position();
    }

    public void inverseOrder() {
        if (buff_ == null) order_ = ByteOrder.LITTLE_ENDIAN; else {
            order_ = (buff_.order() == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
            buff_.order(order_);
        }
    }

    public ByteBuffer getBuffer() {
        return buff_;
    }

    /**
   * @param _nbOct le nombre total d'octets :y compris les 8 octets utilise pour le caractere
   *          sequentiel
   * @return le nombre d'octet non lu. -1 si fin de flux
   * @throws IOException
   */
    public int readAll(int _nbOct) throws IOException {
        if ((buff_ == null) || buff_.capacity() != _nbOct) {
            buff_ = ByteBuffer.allocate(_nbOct);
            if (order_ != null) buff_.order(order_);
        } else buff_.rewind();
        int r = channel_.read(buff_);
        buff_.rewind();
        return r;
    }

    /**
   * Lit le premier bloc : bloc d'entier
   * @param _nbOct le nombre total d'octets :y compris les 8 octets utilise pour le caractere
   *          sequentiel
   * @return le nombre d'octet non lu. -1 si fin de flux
   * @throws IOException
   */
    private int readData(int _nbOct) throws IOException {
        int r = readAll(_nbOct);
        buff_.getInt();
        return r;
    }

    public String getStingFromBuffer(int nbChar) {
        byte[] charByte = new byte[nbChar];
        buff_.get(charByte);
        return new String(charByte);
    }

    public ByteBuffer readData() throws IOException {
        int i = readSequentialData();
        int lu = readAll(i + getLengthForInt());
        return buff_;
    }

    public long getAvailable() throws IOException {
        return channel_.size() - channel_.position();
    }

    public int readSequentialData() throws IOException {
        if (buff4_ == null) {
            buff4_ = ByteBuffer.allocate(getLengthForInt());
            if (order_ != null) buff4_.order(order_);
        }
        buff4_.rewind();
        int ok = channel_.read(buff4_);
        if (ok != 4) throw new EOFException("problem");
        buff4_.rewind();
        return buff4_.getInt();
    }

    public void skipRecord() throws IOException {
        channel_.position(channel_.position() + 4 + readSequentialData());
    }

    public int getLengthForInt() {
        return 4;
    }

    public int getLengthForDouble() {
        return 8;
    }

    public int getLengthRecordForDouble(int nbDouble) {
        return 8 + nbDouble * 8;
    }

    public int getLengthRecordForInt(int _nbInt) {
        return 8 + _nbInt * 4;
    }

    public int getLengthRecordForChar(int _nbInt) {
        return 8 + _nbInt;
    }
}
