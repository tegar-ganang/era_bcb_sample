package org.mobicents.media.server.impl.rtp.sdp;

import org.mobicents.media.Format;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 *
 * @author kulikov
 * @author amit bhayani
 */
public class AudioFormatParser implements FormatParser {

    public Format getFormat(int payload) {
        return AVPROFILE.getAudioFormat(payload);
    }

    public boolean parse(String rtpmap, int[] payloads, Format[] formats, int count) {
        int pos = rtpmap.indexOf(':');
        int pos1 = rtpmap.indexOf(' ', pos);
        int payload = Integer.parseInt(rtpmap.substring(pos + 1, pos1).trim());
        int index = 0;
        while (index < count) {
            if (payloads[index] == payload) break;
            index++;
        }
        pos = rtpmap.indexOf('/', pos1);
        String encoding = rtpmap.substring(pos1 + 1, pos).trim();
        pos1 = rtpmap.indexOf('/', pos + 1);
        double clockRate = pos1 > 0 ? Double.parseDouble(rtpmap.substring(pos + 1, pos1).trim()) : Double.parseDouble(rtpmap.substring(pos + 1, rtpmap.length()).trim());
        int channels = pos1 > 0 ? Integer.parseInt(rtpmap.substring(pos1 + 1, rtpmap.length()).trim()) : 1;
        if (encoding.equalsIgnoreCase("pcmu")) {
            formats[index] = new AudioFormat(AudioFormat.ULAW, clockRate, 8, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("pcma")) {
            formats[index] = new AudioFormat(AudioFormat.ALAW, clockRate, 8, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("speex")) {
            formats[index] = new AudioFormat(AudioFormat.SPEEX, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("telephone-event")) {
            formats[index] = new AudioFormat("telephone-event", clockRate, AudioFormat.NOT_SPECIFIED, AudioFormat.NOT_SPECIFIED);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("g729")) {
            formats[index] = new AudioFormat(AudioFormat.G729, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("gsm")) {
            formats[index] = new AudioFormat(AudioFormat.GSM, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("l16")) {
            formats[index] = new AudioFormat(AudioFormat.LINEAR, clockRate, 16, channels, AudioFormat.LITTLE_ENDIAN, AudioFormat.SIGNED);
            payloads[index] = payload;
        } else if (encoding.equalsIgnoreCase("AMR")) {
            formats[index] = new AudioFormat(AudioFormat.AMR, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        } else {
            formats[index] = new AudioFormat(encoding, clockRate, AudioFormat.NOT_SPECIFIED, channels);
            payloads[index] = payload;
        }
        return index == count;
    }

    public void write(StringBuffer buff, int p, Format f) {
        AudioFormat fmt = (AudioFormat) f;
        String encName = f.getEncoding().toLowerCase();
        buff.append("a=rtpmap:");
        buff.append(p);
        buff.append(" ");
        if (encName.equals("alaw")) {
            buff.append("pcma");
        } else if (encName.equals("ulaw")) {
            buff.append("pcmu");
        } else if (encName.equals("linear")) {
            buff.append("l" + fmt.getSampleSizeInBits());
        } else {
            buff.append(encName);
        }
        double sr = fmt.getSampleRate();
        if (sr > 0) {
            buff.append("/");
            if ((sr - (int) sr) < 1E-6) {
                buff.append((int) sr);
            } else {
                buff.append(sr);
            }
        }
        if (fmt.getChannels() > 1) {
            buff.append("/" + fmt.getChannels());
        }
        if (f.equals(AVProfile.DTMF)) {
            buff.append("\na=fmtp:" + p + " 0-15");
        }
    }
}
