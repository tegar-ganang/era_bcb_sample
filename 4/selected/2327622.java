package jpcsp.connector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashSet;
import jpcsp.Memory;
import jpcsp.State;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceAtrac3plus;
import jpcsp.media.ExternalDecoder;
import jpcsp.media.FileProtocolHandler;
import jpcsp.media.MediaEngine;
import jpcsp.media.PacketChannel;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.settings.AbstractBoolSettingsListener;
import jpcsp.settings.Settings;
import jpcsp.util.Hash;
import jpcsp.util.Utilities;
import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public class AtracCodec {

    private class EnableMediaEngineSettingsListerner extends AbstractBoolSettingsListener {

        @Override
        protected void settingsValueChanged(boolean value) {
            setEnableMediaEngine(value);
        }
    }

    protected String id;

    protected static final String atracSuffix = ".at3";

    protected static final String decodedSuffix = ".decoded";

    protected static final String decodedAtracSuffix = atracSuffix + decodedSuffix;

    protected RandomAccessFile decodedStream;

    protected OutputStream atracStream;

    protected int atracEnd;

    protected int atracEndSample;

    protected int atracMaxSamples;

    protected int atracFileSize;

    protected int atracBufferAddress;

    protected int bytesPerFrame;

    protected byte[] atracDecodeBuffer;

    protected static boolean instructionsDisplayed = false;

    protected static boolean commandFileDirty = true;

    public static int waveFactChunkHeader = 0x74636166;

    public static int waveDataChunkHeader = 0x61746164;

    protected MediaEngine me;

    protected PacketChannel atracChannel;

    protected int currentLoopCount;

    protected boolean useMediaEngine = false;

    protected byte[] samplesBuffer;

    protected ExternalDecoder externalDecoder;

    protected boolean requireAllAtracData;

    private static final String name = "AtracCodec";

    public AtracCodec() {
        Settings.getInstance().registerSettingsListener(name, "emu.useMediaEngine", new EnableMediaEngineSettingsListerner());
        if (useMediaEngine()) {
            me = new MediaEngine();
            atracChannel = new PacketChannel();
            currentLoopCount = 0;
        }
        externalDecoder = new ExternalDecoder();
        generateCommandFile();
    }

    protected boolean checkMediaEngineState() {
        return useMediaEngine && me != null;
    }

    protected boolean useMediaEngine() {
        return useMediaEngine;
    }

    private void setEnableMediaEngine(boolean state) {
        useMediaEngine = state;
    }

    public void setAtracMaxSamples(int atracMaxSamples) {
        this.atracMaxSamples = atracMaxSamples;
        if (useMediaEngine()) {
            me.setAudioSamplesSize(atracMaxSamples);
        }
        atracDecodeBuffer = new byte[atracMaxSamples * 4];
        samplesBuffer = new byte[atracMaxSamples * 4];
    }

    protected String generateID(int address, int length, int fileSize) {
        int hashCode = Hash.getHashCodeFloatingMemory(0, address, length);
        return String.format("Atrac-%08X-%08X", fileSize, hashCode);
    }

    public static String getBaseDirectory() {
        return String.format("%s%s%cAtrac%c", Connector.baseDirectory, State.discId, File.separatorChar, File.separatorChar);
    }

    protected String getCompleteFileName(String suffix) {
        String completeFileName = String.format("%s%s%s", getBaseDirectory(), id, suffix);
        return completeFileName;
    }

    protected void generateCommandFile() {
        if (!commandFileDirty) {
            return;
        }
        String baseDirectory = getBaseDirectory();
        File directory = new File(baseDirectory);
        String[] files = directory.list();
        HashSet<String> atracFiles = new HashSet<String>();
        HashSet<String> decodedFiles = new HashSet<String>();
        if (files != null) {
            for (String fileName : files) {
                if (fileName.endsWith(atracSuffix)) {
                    atracFiles.add(fileName);
                } else if (fileName.endsWith(decodedAtracSuffix)) {
                    decodedFiles.add(fileName);
                }
            }
        }
        PrintWriter command = null;
        try {
            command = new PrintWriter(String.format("%s%s", baseDirectory, Connector.commandFileName));
            for (String atracFileName : atracFiles) {
                if (!decodedFiles.contains(atracFileName + decodedSuffix)) {
                    command.println("DecodeAtrac3");
                    command.println(Connector.basePSPDirectory + atracFileName);
                }
            }
            command.println("Exit");
            commandFileDirty = false;
        } catch (FileNotFoundException e) {
        } finally {
            Utilities.close(command);
        }
    }

    protected void closeStreams() {
        Utilities.close(decodedStream, atracStream);
        decodedStream = null;
        atracStream = null;
        requireAllAtracData = false;
    }

    public void setRequireAllAtracData() {
        requireAllAtracData = true;
    }

    public void atracSetData(int atracID, int codecType, int address, int length, int atracFileSize) {
        this.atracFileSize = atracFileSize;
        this.atracBufferAddress = address;
        id = generateID(address, length, atracFileSize);
        closeStreams();
        atracEndSample = -1;
        requireAllAtracData = false;
        int memoryCodecType = sceAtrac3plus.getCodecType(address);
        if (memoryCodecType != codecType && memoryCodecType != 0) {
            Modules.log.info(String.format("Different CodecType received %d != %d, assuming %d", codecType, memoryCodecType, memoryCodecType));
            codecType = memoryCodecType;
        }
        if (codecType == 0x00001001) {
            Modules.log.info("Decodable AT3 data detected.");
            if (checkMediaEngineState()) {
                me.finish();
                atracChannel = new PacketChannel();
                atracChannel.setTotalStreamSize(atracFileSize);
                atracChannel.setFarRewindAllowed(true);
                atracChannel.write(address, length);
                atracEndSample = 0;
                return;
            }
        } else if (codecType == 0x00001000) {
            if (checkMediaEngineState() && ExternalDecoder.isEnabled()) {
                String decodedFile = externalDecoder.decodeAtrac(address, length, atracFileSize, this);
                if (decodedFile != null) {
                    Modules.log.info("AT3+ data decoded by the external decoder.");
                    me.finish();
                    atracChannel = null;
                    me.init(new FileProtocolHandler(decodedFile), false, true);
                    atracEndSample = -1;
                    return;
                } else if (requireAllAtracData) {
                    me.finish();
                    atracChannel = new PacketChannel();
                    atracChannel.setTotalStreamSize(atracFileSize);
                    atracChannel.write(address, length);
                    return;
                }
                Modules.log.info("AT3+ data could not be decoded by the external decoder.");
            } else {
                Modules.log.info("Undecodable AT3+ data detected.");
            }
        }
        me = null;
        File decodedFile = new File(getCompleteFileName(decodedAtracSuffix));
        if (!decodedFile.canRead()) {
            int numberOfSamples = 0;
            int data = 0;
            Memory mem = Memory.getInstance();
            int scanAddress = address + 12;
            int endScanAddress = address + length;
            while (scanAddress < endScanAddress) {
                int chunkHeader = mem.read32(scanAddress);
                int chunkSize = mem.read32(scanAddress + 4);
                if (chunkHeader == waveFactChunkHeader) {
                    numberOfSamples = mem.read32(scanAddress + 8);
                } else if (chunkHeader == waveDataChunkHeader) {
                    data = mem.read32(scanAddress + 8);
                    break;
                }
                scanAddress += chunkSize + 8;
            }
            File alternateDecodedFile = new File(String.format("%sAtrac-%08X-%08X-%08X%s", getBaseDirectory(), atracFileSize, numberOfSamples, data, decodedAtracSuffix));
            if (alternateDecodedFile.canRead()) {
                decodedFile = alternateDecodedFile;
            }
        }
        File atracFile = new File(getCompleteFileName(atracSuffix));
        if (decodedFile.canRead()) {
            try {
                decodedStream = new RandomAccessFile(decodedFile, "r");
                atracEndSample = (int) (decodedFile.length() / 4);
            } catch (FileNotFoundException e) {
                Modules.log.warn(e);
            }
        } else if (atracFile.canRead() && atracFile.length() == atracFileSize) {
        } else if (sceAtrac3plus.isEnableConnector()) {
            commandFileDirty = true;
            displayInstructions();
            new File(getBaseDirectory()).mkdirs();
            try {
                atracStream = new FileOutputStream(getCompleteFileName(atracSuffix));
                byte[] buffer = new byte[length];
                IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
                for (int i = 0; i < length; i++) {
                    buffer[i] = (byte) memoryReader.readNext();
                }
                atracStream.write(buffer);
            } catch (IOException e) {
                Modules.log.warn(e);
            }
            generateCommandFile();
        }
    }

    public void atracAddStreamData(int address, int length) {
        if (checkMediaEngineState()) {
            if (atracChannel != null) {
                atracChannel.write(address, length);
            }
            return;
        }
        if (atracStream != null) {
            try {
                byte[] buffer = new byte[length];
                IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
                for (int i = 0; i < length; i++) {
                    buffer[i] = (byte) memoryReader.readNext();
                }
                atracStream.write(buffer);
            } catch (IOException e) {
                Modules.log.error(e);
            }
        }
    }

    public int atracDecodeData(int atracID, int address) {
        int samples = 0;
        boolean isEnded = false;
        if (checkMediaEngineState()) {
            if (me.getContainer() == null && atracChannel != null) {
                if (requireAllAtracData) {
                    if (atracChannel.length() >= atracFileSize) {
                        requireAllAtracData = false;
                        if (checkMediaEngineState() && ExternalDecoder.isEnabled()) {
                            String decodedFile = externalDecoder.decodeAtrac(atracChannel, atracBufferAddress, atracFileSize);
                            if (decodedFile != null) {
                                Modules.log.info("AT3+ data decoded by the external decoder (all AT3+ data retrieved).");
                                me.finish();
                                atracChannel = null;
                                me.init(new FileProtocolHandler(decodedFile), false, true);
                                atracEndSample = -1;
                            } else {
                                Modules.log.info("AT3+ data could not be decoded by the external decoder, even after retrieving all AT3+ data.");
                                me = null;
                            }
                        } else {
                            Modules.log.info("AT3+ data could not be decoded by the external decoder, even after retrieving all AT3+ data.");
                            me = null;
                        }
                        if (me == null) {
                            return atracDecodeData(atracID, address);
                        }
                    } else {
                        samples = 1;
                        Memory.getInstance().memset(address, (byte) 0, samples * 4);
                    }
                } else if (atracChannel.length() >= 0x8000 * 3 || atracChannel.length() >= atracFileSize) {
                    me.init(atracChannel, false, true);
                } else {
                    samples = 1;
                    Memory.getInstance().memset(address, (byte) 0, samples * 4);
                }
            }
            if (me.stepAudio(atracMaxSamples * 4)) {
                samples = copySamplesToMem(address);
            }
            if (samples == 0) {
                isEnded = true;
            }
        } else if (decodedStream != null) {
            try {
                int length = decodedStream.read(atracDecodeBuffer);
                if (length > 0) {
                    samples = length / 4;
                    Memory.getInstance().copyToMemory(address, ByteBuffer.wrap(atracDecodeBuffer, 0, length), length);
                    long restLength = decodedStream.length() - decodedStream.getFilePointer();
                    if (restLength <= 0) {
                        isEnded = true;
                    }
                } else {
                    isEnded = true;
                }
            } catch (IOException e) {
                Modules.log.warn(e);
            }
        } else {
            samples = -1;
            isEnded = true;
        }
        if (isEnded) {
            atracEnd = 1;
        } else {
            atracEnd = 0;
        }
        return samples;
    }

    public void atracResetPlayPosition(int sample) {
        if (checkMediaEngineState()) {
            me.audioResetPlayPosition(sample);
        }
        if (decodedStream != null) {
            try {
                decodedStream.seek(sample * 4L);
            } catch (IOException e) {
                Modules.log.error(e);
            }
        }
    }

    public int getChannelLength() {
        if (atracChannel == null) {
            return atracFileSize;
        }
        return atracChannel.length();
    }

    public int getChannelPosition() {
        if (atracChannel == null) {
            return -1;
        }
        return (int) atracChannel.getPosition();
    }

    public void resetChannel() {
        if (atracChannel == null) {
            return;
        }
        atracChannel.clear();
    }

    public int getAtracEnd() {
        return atracEnd;
    }

    public int getAtracEndSample() {
        return atracEndSample;
    }

    public void setAtracLoopCount(int count) {
        currentLoopCount = count;
    }

    protected int copySamplesToMem(int address) {
        Memory mem = Memory.getInstance();
        int bytes = me.getCurrentAudioSamples(samplesBuffer);
        if (bytes > 0) {
            atracEndSample += bytes;
            mem.copyToMemory(address, ByteBuffer.wrap(samplesBuffer, 0, bytes), bytes);
        }
        return bytes / 4;
    }

    public void finish() {
        closeStreams();
        Settings.getInstance().removeSettingsListener(name);
    }

    public boolean isExternalAudio() {
        return atracChannel == null;
    }

    protected void displayInstructions() {
        if (instructionsDisplayed) {
            return;
        }
        Logger log = Modules.log;
        log.info("The ATRAC3 audio is currently being saved under");
        log.info("    " + getBaseDirectory());
        log.info("To decode the audio, copy the following file");
        log.info("    *" + atracSuffix);
        log.info("    " + Connector.commandFileName);
        log.info("to your PSP under");
        log.info("    " + Connector.basePSPDirectory);
        log.info("and run the '" + Connector.jpcspConnectorName + "' on your PSP.");
        log.info("After decoding on the PSP, move the following files");
        log.info("    " + Connector.basePSPDirectory + decodedAtracSuffix);
        log.info("back to Jpcsp under");
        log.info("    " + getBaseDirectory());
        log.info("Afterwards, you can delete the files on the PSP.");
        instructionsDisplayed = true;
    }
}
