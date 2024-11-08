package avisync.node.packer;

import avisync.AVIException;
import avisync.decoder.MP3Decoder;
import avisync.decoder.MP3Frame;
import avisync.model.AVIAudioStreamFormat;
import avisync.model.AVIMP3AudioStreamFormat;
import avisync.model.AVIPacket;
import avisync.model.AVIStreamHeader;

public class AVIMP3AudioPacker extends AVIAudioPacker {

    private MP3Decoder decoder = new MP3Decoder();

    private AVIPacketBuffer buffer = new AVIPacketBuffer();

    public AVIMP3AudioPacker() {
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

    private void checkStreamFormat() throws AVIException {
        AVIStreamHeader header = getStreamHeader();
        AVIAudioStreamFormat format = (AVIAudioStreamFormat) getStreamFormat();
        double duration = header.getLength() * (double) header.getScale() / (double) header.getRate();
        int samplesPerSecond = format.getSamplesPerSecond();
        int channels = format.getChannels();
        int bytesPerSecond = format.getAvgBytesPerSec();
        boolean variableBitRate;
        if (header.getSampleSize() == 0) {
            variableBitRate = true;
        } else {
            variableBitRate = false;
            if (header.getSampleSize() == 1) {
                int bitRate = (8 * bytesPerSecond + 500) / 1000;
                int bytesPerFrame = MP3Frame.getBytesPerFrame(bitRate, samplesPerSecond);
                int samplesPerFrame = MP3Frame.getSamplesPerFrame(samplesPerSecond);
                if (bytesPerSecond != (bytesPerFrame * samplesPerSecond) / samplesPerFrame) {
                    warning("Changing MP3 audio from constant (CBR) to variable bitrate (VBR) packing mode");
                    variableBitRate = true;
                }
            }
        }
        setupStreamFormat(samplesPerSecond, channels, bytesPerSecond, duration, variableBitRate, getFramesPerPacket());
    }

    private void setupStreamFormat(int samplesPerSecond, int channels, int bytesPerSecond, double duration, boolean variableBitRate, int framesPerBlock) throws AVIException {
        AVIStreamHeader header = getStreamHeader();
        AVIMP3AudioStreamFormat format = (AVIMP3AudioStreamFormat) getStreamFormat();
        header.setHandler(0x0000);
        header.setRate(samplesPerSecond);
        header.setScale(framesPerBlock * MP3Frame.getSamplesPerFrame(samplesPerSecond));
        header.setLength((int) Math.ceil(duration * (double) header.getRate() / (double) header.getScale()));
        if (variableBitRate) {
            header.setSampleSize(0);
            int maxBytesPerFrame = MP3Frame.getBytesPerFrame(MP3Frame.MPEG_MAX_BIT_RATE, samplesPerSecond);
            if (header.getSuggestedBufferSize() > framesPerBlock * maxBytesPerFrame) header.setSuggestedBufferSize(framesPerBlock * maxBytesPerFrame);
            format.setAvgBytesPerSec(bytesPerSecond);
            format.setSamplesPerSecond(samplesPerSecond);
            format.setChannels((short) channels);
            format.setBlockAlign((short) (framesPerBlock * MP3Frame.getSamplesPerFrame(samplesPerSecond)));
            format.setBlockSize((short) Math.ceil(framesPerBlock * format.getAvgBytesPerSec() * (double) header.getScale() / (double) header.getRate()));
            format.setFramesPerBlock((short) framesPerBlock);
        } else {
            int bitRate = (8 * bytesPerSecond + 500) / 1000;
            int bytesPerFrame = MP3Frame.getBytesPerFrame(bitRate, samplesPerSecond);
            header.setSampleSize(framesPerBlock * bytesPerFrame);
            header.setSuggestedBufferSize(framesPerBlock * bytesPerFrame);
            format.setAvgBytesPerSec(bytesPerSecond);
            format.setSamplesPerSecond(samplesPerSecond);
            format.setChannels((short) channels);
            format.setBlockAlign((short) (framesPerBlock * bytesPerFrame));
            format.setBlockSize((short) (framesPerBlock * bytesPerFrame));
            format.setFramesPerBlock((short) framesPerBlock);
        }
    }
}
