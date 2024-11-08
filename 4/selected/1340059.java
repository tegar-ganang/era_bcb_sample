package avisync.node.packer;

import avisync.AVIException;
import avisync.model.AVIAudioStreamFormat;
import avisync.model.AVIPacket;
import avisync.model.AVIStreamHeader;
import avisync.node.AVIInvalidPacketLengthException;
import avisync.node.AVIInvalidPacketTypeException;
import avisync.node.AVIInvalidStreamChannelsException;
import avisync.node.AVIInvalidStreamSampleSizeException;
import avisync.node.AVIInvalidStreamTypeException;
import avisync.util.AVICharCode;

public class AVIAudioPacker extends AVIPacker {

    public static final String AUDIO_FRAMES_PER_PACKET_PROPERTY_NAME = "avisync.audio.framesPerPacket";

    public static final int AUDIO_FRAMES_PER_PACKET_DEFAULT_VALUE = 1;

    private int framesPerPacket;

    public AVIAudioPacker() {
        setFramesPerPacket(AUDIO_FRAMES_PER_PACKET_DEFAULT_VALUE);
    }

    public int getFramesPerPacket() {
        return framesPerPacket;
    }

    public void setFramesPerPacket(int framesPerPacket) {
        this.framesPerPacket = framesPerPacket;
    }

    public AVIStreamHeader getStreamHeader() {
        return getStream().getHeader();
    }

    public AVIAudioStreamFormat getStreamFormat() {
        return (AVIAudioStreamFormat) getStream().getFormat();
    }

    public int getStreamPacketID() {
        return AVICharCode.packetCC(getStreamID(), AVIPacket.AVI_PACKET_AUDIO_TYPE_ID);
    }

    public void setProperty(String name, int value) {
        if (name.equals(AUDIO_FRAMES_PER_PACKET_PROPERTY_NAME)) setFramesPerPacket(value);
    }

    public void init() throws AVIException {
        checkStreamFormat();
        super.init();
    }

    public void destroy() throws AVIException {
        super.destroy();
    }

    public void sendPacket(AVIPacket packet) throws AVIException {
        checkPacket(packet);
        super.sendPacket(packet);
    }

    private void checkPacket(AVIPacket packet) throws AVIException {
        if (!packet.isAudioType()) throw new AVIInvalidPacketTypeException();
        AVIStreamHeader header = getStreamHeader();
        if (packet.getLength() > header.getSuggestedBufferSize()) warning("Audio buffer overflow processing packet of " + packet.getLength() + " bytes");
        AVIAudioStreamFormat format = getStreamFormat();
        if (header.getSampleSize() == 0) {
            if (packet.getLength() > format.getBlockAlign()) {
                error("Audio packet of " + packet.getLength() + " bytes is larger than " + format.getBlockAlign() + " bytes");
                throw new AVIInvalidPacketLengthException();
            }
        } else {
            if ((packet.getLength() % format.getBlockAlign()) != 0) {
                warning("Audio packet of " + packet.getLength() + " bytes is not multiple of " + format.getBlockAlign() + " bytes");
            }
        }
    }

    private void checkStreamFormat() throws AVIException {
        AVIStreamHeader header = getStreamHeader();
        if (!header.isAudioType()) throw new AVIInvalidStreamTypeException();
        AVIAudioStreamFormat format = getStreamFormat();
        int sampleRate = format.getSamplesPerSecond();
        if (sampleRate != 48000 && sampleRate != 44100 && sampleRate != 32000 && sampleRate != 24000 && sampleRate != 22050 && sampleRate != 16000) warning("Audio sample rate " + sampleRate + " is not standard (48000, 44100, 32000, 24000, 22050 or 16000 Hz)");
        short channels = format.getChannels();
        if (channels != 1 && channels != 2 && channels != 4 && channels != 5 && channels != 6) {
            error("Audio streams don't support " + channels + " channels");
            throw new AVIInvalidStreamChannelsException();
        }
        if (header.getSampleSize() != 0) {
            if (header.getSampleSize() != format.getBlockAlign()) throw new AVIInvalidStreamSampleSizeException();
        }
    }
}
