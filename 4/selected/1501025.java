package avisync.node.packer;

import avisync.AVIException;
import avisync.decoder.AC3Decoder;
import avisync.decoder.AC3Frame;
import avisync.model.AVIAC3AudioStreamFormat;
import avisync.model.AVIPacket;
import avisync.model.AVIStreamHeader;
import avisync.node.AVIInvalidStreamChannelsException;
import avisync.node.AVIStreamFormatException;

public class AVIAC3AudioPacker extends AVIAudioPacker {

    private AC3Decoder decoder = new AC3Decoder();

    private AVIPacketBuffer buffer = new AVIPacketBuffer();

    public AVIAC3AudioPacker() {
    }

    public void init() throws AVIException {
        checkStreamFormat();
        decoder.init();
        super.init();
        buffer.clearFrames();
    }

    public void destroy() throws AVIException {
        if (buffer.getNumBytes() != 0) {
            AVIPacket packet = new AVIPacket(getStreamPacketID(), buffer.getNumBytes());
            buffer.pollFrames(packet.getData());
            super.sendPacket(packet);
        }
        super.destroy();
        decoder.destroy();
    }

    public void sendPacket(AVIPacket packet) throws AVIException {
        decoder.writeBytes(packet.getData());
        byte[] frame;
        while ((frame = decoder.readFrame()) != null) {
            buffer.offerFrame(frame);
            if (buffer.getNumFrames() >= getFramesPerPacket()) {
                packet = new AVIPacket(getStreamPacketID(), buffer.getNumBytes());
                buffer.pollFrames(packet.getData());
                super.sendPacket(packet);
            }
        }
    }

    private void checkStreamFormat() throws AVIStreamFormatException {
        AVIStreamHeader header = getStreamHeader();
        AVIAC3AudioStreamFormat format = (AVIAC3AudioStreamFormat) getStreamFormat();
        if (format.getFormatTag() != AVIAC3AudioStreamFormat.AVI_AUDIO_STREAM_FORMAT_TAG_AC3) throw new AVIStreamFormatException();
        if (!AC3Frame.isSupportedChannels(format.getChannels())) {
            error("AC3 audio format doesn't support " + format.getChannels() + " channels");
            throw new AVIInvalidStreamChannelsException();
        }
        if (!AC3Frame.isSupportedSampleRate(format.getSamplesPerSecond())) {
            error("AC3 audio format doesn't support sample rate of " + format.getSamplesPerSecond() + " samples/second");
            throw new AVIStreamFormatException();
        }
        if (!AC3Frame.isSupportedBitRate(8 * format.getAvgBytesPerSec())) {
            error("AC3 audio format doesn't support bitrate of " + (8 * format.getAvgBytesPerSec() / 1000.0) + " Kbps");
            throw new AVIStreamFormatException();
        }
        double duration = header.getLength() * (double) header.getScale() / (double) header.getRate();
        format.setBlockAlign((short) 1);
        format.setBitsPerSample((short) 0);
        format.setExtraSize((short) 0);
        header.setHandler(0x0000);
        header.setFlags(0x0000);
        header.setPriority((short) 0);
        header.setInitialFrames(1);
        header.setStart(0);
        header.setScale(1);
        header.setRate(format.getAvgBytesPerSec());
        header.setQuality(-1);
        header.setSampleSize(1);
        header.setLength((int) Math.ceil(duration * (double) header.getRate() / (double) header.getScale()));
        int maxBytesPerFrame = AC3Frame.getMaxBytesPerFrame(8 * format.getAvgBytesPerSec(), format.getSamplesPerSecond());
        header.setSuggestedBufferSize(getFramesPerPacket() * maxBytesPerFrame);
    }
}
