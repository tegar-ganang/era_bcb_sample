package eu.cherrytree.paj.sound;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;
import de.jarnbjo.ogg.EndOfOggStreamException;
import de.jarnbjo.ogg.FileStream;
import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.ogg.PhysicalOggStream;
import de.jarnbjo.vorbis.VorbisStream;
import eu.cherrytree.paj.base.AppDefinition;
import eu.cherrytree.paj.gui.Console;

public class OggAudioStream implements AudioStreamSource {

    private PhysicalOggStream os;

    private LogicalOggStream los;

    private VorbisStream vs;

    private int channels;

    private int sampleRate;

    public OggAudioStream(String file) {
        String path = AppDefinition.getMusicDirectoryPath() + "/" + file;
        os = null;
        try {
            os = new FileStream(new RandomAccessFile(path, "r"));
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't create sound stream!");
            return;
        }
        los = (LogicalOggStream) os.getLogicalStreams().iterator().next();
        if (los.getFormat() != LogicalOggStream.FORMAT_VORBIS) {
            Console.print("File is not in proper ogg vorbis format.");
            return;
        }
        vs = null;
        openStream();
        channels = vs.getIdentificationHeader().getChannels();
        sampleRate = vs.getIdentificationHeader().getSampleRate();
    }

    private void openStream() {
        try {
            vs = new VorbisStream(los);
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't create sound stream!");
            return;
        }
    }

    private void closeStream() {
        try {
            vs.close();
        } catch (IOException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't close sound stream!");
            return;
        }
    }

    public void close() {
        closeStream();
        try {
            los.close();
            os.close();
        } catch (IOException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't close sound stream!");
            return;
        }
    }

    public Vector<Byte> streamData() throws AudioStreamEndException, AudioStreamErrorException {
        Vector<Byte> s_buffer = new Vector<Byte>();
        byte[] pcm = new byte[65536];
        int size = 0;
        int result;
        try {
            while (size < 65536) {
                result = vs.readPcm(pcm, 0, pcm.length);
                for (int i = 0; i < result; i += 2) {
                    s_buffer.add(pcm[i + 1]);
                    s_buffer.add(pcm[i]);
                }
                if (result > 0) size += result;
            }
        } catch (EndOfOggStreamException e) {
            throw new AudioStreamEndException();
        } catch (IOException e) {
            throw new AudioStreamErrorException("IOException: " + e.getMessage());
        }
        if (size == 0) throw new AudioStreamErrorException("Audio stream read size = 0");
        return s_buffer;
    }

    public int getChannels() {
        return channels;
    }

    public int getSampleRate() {
        return sampleRate;
    }
}
