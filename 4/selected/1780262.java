package org.mobicents.media.server;

import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.spi.format.AudioFormat;

/**
 *
 * @author kulikov
 */
public class SdpTemplate {

    private String template;

    private boolean isAudioSupported;

    private boolean isVideoSupported;

    public SdpTemplate(RTPFormats audio, RTPFormats video) {
        StringBuilder builder = new StringBuilder();
        writeHeader(builder);
        if (audio != null && !audio.isEmpty()) {
            this.isAudioSupported = true;
            this.writeAudioDescriptor(builder, audio);
        }
        if (video != null && !video.isEmpty()) {
            this.isVideoSupported = true;
            this.writeVideoDescriptor(builder, video);
        }
        template = builder.toString();
    }

    private void writeHeader(StringBuilder builder) {
        builder.append("v=0\n");
        builder.append("o=- %s 1 IN IP4 %s\n");
        builder.append("s=Mobicents Media Server \n");
        builder.append("c=%s %s %s\n");
        builder.append("t=0 0\n");
    }

    private void writeAudioDescriptor(StringBuilder builder, RTPFormats formats) {
        builder.append("m=audio %s RTP/AVP ");
        builder.append(payloads(formats));
        builder.append("\n");
        formats.rewind();
        while (formats.hasMore()) {
            RTPFormat f = formats.next();
            String rtpmap = null;
            AudioFormat fmt = (AudioFormat) f.getFormat();
            if (fmt.getChannels() == 1) {
                rtpmap = String.format("a=rtpmap:%d %s/%d\n", f.getID(), fmt.getName(), f.getClockRate());
            } else {
                rtpmap = String.format("a=rtpmap:%d %s/%d/%d\n", f.getID(), fmt.getName(), f.getClockRate(), fmt.getChannels());
            }
            builder.append(rtpmap);
            if (f.getFormat().getOptions() != null) {
                builder.append(String.format("a=fmtp:%d %s\n", f.getID(), f.getFormat().getOptions()));
            }
        }
    }

    private void writeVideoDescriptor(StringBuilder builder, RTPFormats formats) {
        builder.append("m=video %s RTP/AVP ");
        builder.append(payloads(formats));
        builder.append("\n");
        formats.rewind();
        while (formats.hasMore()) {
            RTPFormat f = formats.next();
            builder.append(String.format("a=rtpmap:%d %s/%d\n", f.getID(), f.getFormat().getName(), f.getClockRate()));
            if (f.getFormat().getOptions() != null) {
                builder.append(String.format("a=fmtp: %d %s\n", f.getID(), f.getFormat().getOptions().toString()));
            }
        }
    }

    /**
     * List of payloads.
     *
     * @param formats the RTP format objects.
     * @return the string which with payload numbers
     */
    private String payloads(RTPFormats formats) {
        StringBuilder builder = new StringBuilder();
        formats.rewind();
        while (formats.hasMore()) {
            RTPFormat f = formats.next();
            builder.append(f.getID());
            builder.append(" ");
        }
        return builder.toString().trim();
    }

    public String getSDP(String bindAddress, String netwType, String addressType, String address, int audioPort, int videoPort) {
        if (this.isAudioSupported && !this.isVideoSupported) {
            return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address, audioPort);
        } else if (!this.isAudioSupported && this.isVideoSupported) {
            return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address, videoPort);
        } else if (this.isAudioSupported && this.isVideoSupported) {
            return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address, audioPort, videoPort);
        }
        return String.format(template, System.currentTimeMillis(), bindAddress, netwType, addressType, address);
    }
}
