package org.pagger.data.picture.geo.nmea;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import org.pagger.util.ObjectUtils;
import org.pagger.util.Validator;

/**
 * GSV - Satellites in view
 * 
 * These sentences describe the sky position of a UPS satellite in view.
 * Typically they're shipped in a group of 2 or 3.
 * <pre>
 *        1 2 3 4 5 6 7     n
 *        | | | | | | |     |
 * $--GSV,x,x,x,x,x,x,x,...*hh
 * 
 *   Field Number: 
 *   1) total number of GSV messages to be transmitted in this group
 *   2) 1-origin number of this GSV message  within current group
 *   3) total number of satellites in view (leading zeros sent)
 *   4) satellite PRN number (leading zeros sent)
 *   5) elevation in degrees (00-90) (leading zeros sent)
 *   6) azimuth in degrees to true north (000-359) (leading zeros sent)
 *   7) SNR in dB (00-99) (leading zeros sent)
 *      more satellite info quadruples like 4-7
 *   n) checksum
 * </pre>
 * 
 *  Example:
 * <pre>
 *  $GPGSV,3,1,11,03,03,111,00,04,15,270,00,06,01,010,00,13,06,292,00*74
 *  $GPGSV,3,2,11,14,25,170,00,16,57,208,39,18,67,296,40,19,40,246,00*74
 *  $GPGSV,3,3,11,22,42,067,42,24,14,311,43,27,05,244,00,,,,*4D
 * </pre>
 * 
 * Some GPS receivers may emit more than 12 quadruples (more than three
 * GPGSV sentences), even though NMEA-0813 doesn't allow this.  (The
 * extras might be WAAS satellites, for example.) Receivers may also
 * report quads for satellites they aren't tracking, in which case the
 * SNR field will be null; we don't know whether this is formally allowed
 * or not.
 * 
 * @author Franz Wilhelmstötter
 */
public class Gsv implements Serializable {

    private static final long serialVersionUID = -4801891950282899008L;

    private final int _totalNumberOfMessages;

    private final int _satellitesInView;

    private final Info[] _satellites;

    public Gsv(final int totalNumberOfMessages, final int satellitesInView, final Info[] satellites) {
        _totalNumberOfMessages = totalNumberOfMessages;
        _satellitesInView = satellitesInView;
        _satellites = satellites;
    }

    public int getTotalNumberOfMessages() {
        return _totalNumberOfMessages;
    }

    public int getSatellitesInView() {
        return _satellitesInView;
    }

    public Info[] getSatellites() {
        return _satellites;
    }

    public static Gsv valueOf(final String... values) {
        Validator.notNull(values, "GSV sentences");
        if (values.length == 0) {
            throw new IllegalArgumentException("GSV sentences must not be empty.");
        }
        String[] parts = values[0].split(",");
        final int totalNumberOfMessages = Integer.parseInt(parts[1]);
        if (totalNumberOfMessages != values.length) {
            throw new NmeaFormatException(String.format("Not all messages given. Expected %s but got %s: '%s' ", totalNumberOfMessages, values.length, values[0]));
        }
        final int messageNumber = Integer.parseInt(parts[2]);
        if (messageNumber != 1) {
            throw new NmeaFormatException("Expecting message number 1, but was " + messageNumber + ".");
        }
        final int satellitesInView = Integer.parseInt(parts[3]);
        for (int i = 1; i < values.length; ++i) {
            Checksum.check(values[i]);
            parts = values[i].split(",");
            if (totalNumberOfMessages != Integer.parseInt(parts[1])) {
                throw new NmeaFormatException("Different number of messages.");
            }
            if (Integer.parseInt(parts[2]) != i + 1) {
                throw new NmeaFormatException("Expecting message number " + (i + 1) + ", but was " + messageNumber + ".");
            }
            if (satellitesInView != Integer.parseInt(parts[3])) {
                throw new NmeaFormatException("Different number of satellites in view.");
            }
        }
        final List<Info> info = new ArrayList<Info>();
        for (int i = 0; i < values.length; ++i) {
            parts = values[i].split(",");
            for (int j = 4; j < parts.length; j += 4) {
                if (info.size() < satellitesInView) {
                    String[] inf = new String[4];
                    inf[0] = parts[j + 0];
                    inf[1] = parts[j + 1];
                    inf[2] = parts[j + 2];
                    inf[3] = trim(parts[j + 3]);
                    info.add(parseInfo(inf));
                }
            }
        }
        if (info.size() != satellitesInView) {
            throw new NmeaFormatException("Different satellite infos.");
        }
        return new Gsv(totalNumberOfMessages, satellitesInView, info.toArray(new Info[0]));
    }

    private static Info parseInfo(final String[] parts) {
        try {
            final int satelliteNumber = Integer.parseInt(parts[0]);
            Measurable<Angle> elevation = null;
            if (!parts[1].isEmpty()) {
                elevation = Measure.valueOf(Double.parseDouble(parts[1]), NonSI.DEGREE_ANGLE);
            }
            Measurable<Angle> azimuth = null;
            if (!parts[2].isEmpty()) {
                azimuth = Measure.valueOf(Double.parseDouble(parts[2]), NonSI.DEGREE_ANGLE);
            }
            Measurable<Dimensionless> snr = null;
            if (!parts[3].isEmpty()) {
                snr = Measure.valueOf(Double.parseDouble(parts[3]), NonSI.DECIBEL);
            }
            return new Info(satelliteNumber, elevation, azimuth, snr);
        } catch (NumberFormatException e) {
            throw new NmeaFormatException(e);
        }
    }

    private static String trim(final String value) {
        String ret = value;
        int index = value.indexOf('*');
        if (index != -1) {
            ret = value.substring(0, index);
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash += 17 * _totalNumberOfMessages;
        hash += 17 * _satellitesInView;
        hash += 17 * Arrays.hashCode(_satellites);
        return hash;
    }

    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (object == null || object.getClass() != getClass()) {
            return false;
        }
        final Gsv gsv = (Gsv) object;
        return _totalNumberOfMessages == gsv._totalNumberOfMessages && _satellitesInView == gsv._satellitesInView && Arrays.equals(_satellites, gsv._satellites);
    }

    @Override
    public String toString() {
        return Arrays.toString(_satellites);
    }

    public static final class Info implements Serializable {

        private static final long serialVersionUID = 8191511949602159440L;

        private final int _satelliteNumber;

        private final Measurable<Angle> _elevation;

        private final Measurable<Angle> _azimuth;

        private final Measurable<Dimensionless> _snr;

        Info(final int satelliteNumber, final Measurable<Angle> elevation, final Measurable<Angle> azimuth, final Measurable<Dimensionless> snr) {
            _satelliteNumber = satelliteNumber;
            _elevation = elevation;
            _azimuth = azimuth;
            _snr = snr;
        }

        public int getSatelliteNumber() {
            return _satelliteNumber;
        }

        public Measurable<Angle> getElevation() {
            return _elevation;
        }

        public Measurable<Angle> getAzimuth() {
            return _azimuth;
        }

        public Measurable<Dimensionless> getSnr() {
            return _snr;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash += 17 * _satelliteNumber;
            hash += 17 * ObjectUtils.hashCode(_elevation, SI.RADIAN);
            hash += 17 * ObjectUtils.hashCode(_azimuth, SI.RADIAN);
            hash += 17 * ObjectUtils.hashCode(_snr, NonSI.DECIBEL);
            return hash;
        }

        @Override
        public boolean equals(final Object object) {
            if (object == this) {
                return true;
            }
            if (object == null || object.getClass() != getClass()) {
                return false;
            }
            final Info info = (Info) object;
            return _satelliteNumber == info._satelliteNumber && ObjectUtils.equalDoubleValues(_elevation, info._elevation, SI.RADIAN) && ObjectUtils.equalDoubleValues(_azimuth, info._azimuth, SI.RADIAN) && ObjectUtils.equalLongValues(_snr, info._snr, NonSI.DECIBEL);
        }

        @Override
        public String toString() {
            return _satelliteNumber + ": " + _elevation + " " + _azimuth + " " + _snr;
        }
    }
}
