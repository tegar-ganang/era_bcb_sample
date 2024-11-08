package org.javasock.windows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.javasock.Layer;

public class RadioTapLayer extends Layer {

    /**
	  * Radio tap 'field is present' flags.  Note these are the bit numbers, 
	  *   not the mask value.  See ieee80211_radiotap.h.
	  */
    public static final int IEEE80211_RADIOTAP_TSFT = 0, IEEE80211_RADIOTAP_FLAGS = 1, IEEE80211_RADIOTAP_RATE = 2, IEEE80211_RADIOTAP_CHANNEL = 3, IEEE80211_RADIOTAP_FHSS = 4, IEEE80211_RADIOTAP_DBM_ANTSIGNAL = 5, IEEE80211_RADIOTAP_DBM_ANTNOISE = 6, IEEE80211_RADIOTAP_LOCK_QUALITY = 7, IEEE80211_RADIOTAP_TX_ATTENUATION = 8, IEEE80211_RADIOTAP_DB_TX_ATTENUATION = 9, IEEE80211_RADIOTAP_DBM_TX_POWER = 10, IEEE80211_RADIOTAP_ANTENNA = 11, IEEE80211_RADIOTAP_DB_ANTSIGNAL = 12, IEEE80211_RADIOTAP_DB_ANTNOISE = 13, IEEE80211_RADIOTAP_FCS = 14, IEEE80211_RADIOTAP_EXT = 31;

    /**
	  * Bit masks for flag values, if present.
	  */
    public static final int IEEE80211_RADIOTAP_F_CFP = 0x01, IEEE80211_RADIOTAP_F_SHORTPRE = 0x02, IEEE80211_RADIOTAP_F_WEP = 0x04, IEEE80211_RADIOTAP_F_FRAG = 0x08, IEEE80211_RADIOTAP_F_FCS = 0x10;

    /**
	  * Channel Flags, if present.
	  */
    public static final int IEEE80211_CHAN_TURBO = 0x0010, IEEE80211_CHAN_CCK = 0x0020, IEEE80211_CHAN_OFDM = 0x0040, IEEE80211_CHAN_2GHZ = 0x0080, IEEE80211_CHAN_5GHZ = 0x0100, IEEE80211_CHAN_PASSIVE = 0x0200, IEEE80211_CHAN_DYN = 0x0400, IEEE80211_CHAN_GFSK = 0x0800, IEEE80211_CHAN_STURBO = 0x2000;

    /**
	  * Offset into packet for various fields.
	  */
    public static final int OFFSET_LENGTH = 0x2;

    public RadioTapLayer() {
    }

    public RadioTapLayer(ByteBuffer bb) {
        ByteOrder oldOrder = bb.order();
        bb.order(ByteOrder.LITTLE_ENDIAN);
        try {
            int start = bb.position();
            version = bb.get() & 0xff;
            switch(version) {
                case 0:
                    bb.get();
                    length = bb.getShort() & 0xffff;
                    present = bb.getInt();
                    if (hasField(IEEE80211_RADIOTAP_TSFT)) tsft = bb.getLong();
                    if (hasField(IEEE80211_RADIOTAP_FLAGS)) flags = bb.get() & 0xff;
                    if (hasField(IEEE80211_RADIOTAP_RATE)) dataRate = bb.get() & 0xff;
                    if (hasField(IEEE80211_RADIOTAP_CHANNEL)) {
                        channel = bb.getShort() & 0xffff;
                        channelFlags = bb.getShort() & 0xffff;
                    }
                    if (hasField(IEEE80211_RADIOTAP_FHSS)) fhss = bb.getShort() & 0xffff;
                    if (hasField(IEEE80211_RADIOTAP_DBM_ANTSIGNAL)) signal_dBm = bb.get();
                    if (hasField(IEEE80211_RADIOTAP_DBM_ANTNOISE)) noise_dBm = bb.get();
                    if (hasField(IEEE80211_RADIOTAP_LOCK_QUALITY)) lockQuality = bb.getShort() & 0xffff;
                    if (hasField(IEEE80211_RADIOTAP_TX_ATTENUATION)) txAtten = bb.getShort() & 0xffff;
                    if (hasField(IEEE80211_RADIOTAP_DB_TX_ATTENUATION)) txAtten_dB = bb.getShort() & 0xffff;
                    if (hasField(IEEE80211_RADIOTAP_DBM_TX_POWER)) txPower_dBm = bb.get();
                    if (hasField(IEEE80211_RADIOTAP_ANTENNA)) antennaIndex = bb.get() & 0xff;
                    if (hasField(IEEE80211_RADIOTAP_DB_ANTSIGNAL)) signal_dB = bb.get() & 0xff;
                    if (hasField(IEEE80211_RADIOTAP_DB_ANTNOISE)) noise_dB = bb.get() & 0xff;
                    if (hasField(IEEE80211_RADIOTAP_FCS)) fcs = bb.getInt(start + length - 4);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported RadioTap version!");
            }
            bb.position(start + length);
        } finally {
            bb.order(oldOrder);
        }
    }

    public static RadioTapLayer createFromBytes(ByteBuffer bb) {
        return new RadioTapLayer(bb);
    }

    /**
	  * Returns true if the bit number in field is set in present.
	  *  Call this before using any of the other accessors, to make sure
	  *  the data there will be valid.
	  * @see #IEEE80211_RADIOTAP_TSFT
	  */
    public final boolean hasField(int field) {
        return ((1 << field) & present) != 0;
    }

    /**
	  * Returns the length in bytes of the header.
	  */
    public int getLength() {
        return length;
    }

    /**
	  * Gets the value of the 'fields present' field.
	  * @see #hasField(int)
	  */
    public int getPresent() {
        return present;
    }

    public long getTsft() {
        return tsft;
    }

    public int getFlags() {
        return flags;
    }

    /**
	  * Rate of the connection, in units of 500 kilobits per second.
	  */
    public int getDataRate() {
        return dataRate;
    }

    /**
	  * Center frequency of the transmission, in MHz.
	  */
    public int getChannel() {
        return channel;
    }

    public int getChannelFlags() {
        return channelFlags;
    }

    public int getFhss() {
        return fhss;
    }

    public int getSignal_dBm() {
        return signal_dBm;
    }

    public int getNoise_dBm() {
        return noise_dBm;
    }

    public int getLockQuality() {
        return lockQuality;
    }

    public int getTxAtten() {
        return txAtten;
    }

    public int getTxAtten_dB() {
        return txAtten_dB;
    }

    public int getTxPower_dBm() {
        return txPower_dBm;
    }

    public int getAntennaIndex() {
        return antennaIndex;
    }

    public int getSignal_dB() {
        return signal_dB;
    }

    public int getNoise_dB() {
        return noise_dB;
    }

    public int getFcs() {
        return fcs;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("RadioTapLayer (v=");
        sb.append(version);
        sb.append(",len=");
        sb.append(length);
        if (hasField(IEEE80211_RADIOTAP_TSFT)) sb.append(",tsft=" + tsft);
        if (hasField(IEEE80211_RADIOTAP_FLAGS)) sb.append(",flags=" + flags);
        if (hasField(IEEE80211_RADIOTAP_RATE)) sb.append(",dataRate=" + dataRate);
        if (hasField(IEEE80211_RADIOTAP_CHANNEL)) {
            sb.append(",channel=" + channel);
            sb.append(",channelFlags=" + channelFlags);
        }
        if (hasField(IEEE80211_RADIOTAP_FHSS)) sb.append(",fhss=" + fhss);
        if (hasField(IEEE80211_RADIOTAP_DBM_ANTSIGNAL)) sb.append(",signal_dBm=" + signal_dBm);
        if (hasField(IEEE80211_RADIOTAP_DBM_ANTNOISE)) sb.append(",noise_dBm=" + noise_dBm);
        if (hasField(IEEE80211_RADIOTAP_LOCK_QUALITY)) sb.append(",lockQuality=" + lockQuality);
        if (hasField(IEEE80211_RADIOTAP_TX_ATTENUATION)) sb.append(",txAtten=" + txAtten);
        if (hasField(IEEE80211_RADIOTAP_DB_TX_ATTENUATION)) sb.append(",txAtten_dB=" + txAtten_dB);
        if (hasField(IEEE80211_RADIOTAP_DBM_TX_POWER)) sb.append(",txPower_dBm=" + txPower_dBm);
        if (hasField(IEEE80211_RADIOTAP_ANTENNA)) sb.append(",antennaIndex=" + antennaIndex);
        if (hasField(IEEE80211_RADIOTAP_DB_ANTSIGNAL)) sb.append(",signal_dB=" + signal_dB);
        if (hasField(IEEE80211_RADIOTAP_DB_ANTNOISE)) sb.append(",noise_dB=" + noise_dB);
        return sb.toString();
    }

    protected void doWrite(ByteBuffer buff, Layer parent, Layer child) {
        throw new UnsupportedOperationException("The RadioTapLayer is read-only.");
    }

    private int length, version, present, flags, dataRate, channel, channelFlags, fhss, signal_dBm, noise_dBm, lockQuality, txAtten, txAtten_dB, txPower_dBm, antennaIndex, signal_dB, noise_dB, fcs;

    private long tsft;
}
