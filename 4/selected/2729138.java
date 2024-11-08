package eu.cherrytree.paj.sound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;
import java.util.zip.ZipException;
import de.jarnbjo.ogg.EndOfOggStreamException;
import de.jarnbjo.ogg.FileStream;
import de.jarnbjo.ogg.LogicalOggStream;
import de.jarnbjo.ogg.OggFormatException;
import de.jarnbjo.ogg.PhysicalOggStream;
import de.jarnbjo.vorbis.VorbisFormatException;
import de.jarnbjo.vorbis.VorbisStream;
import eu.cherrytree.paj.base.AppDefinition;
import eu.cherrytree.paj.base.AppState;
import eu.cherrytree.paj.file.FileReader;
import eu.cherrytree.paj.gui.Console;

public abstract class SoundCue {

    public enum SoundFormat {

        MONO_8, MONO_16, STEREO_8, STEREO_16
    }

    public class SoundData {

        public SoundFormat format;

        public int size;

        public int frequency;

        public byte[] data;
    }

    protected float[] probabilities;

    public SoundCue(float[] probabilities) {
        this.probabilities = probabilities.clone();
        float sum = 0.0f;
        for (int i = 0; i < this.probabilities.length; i++) sum += this.probabilities[i];
        if (sum != 1.0f) {
            for (int i = 0; i < this.probabilities.length; i++) this.probabilities[i] /= sum;
        }
    }

    private String getPath(String file) {
        String path = AppDefinition.getDefaultDataPackagePath();
        switch(SoundManager.getSoundQuality()) {
            case HIGH_QUALITY:
                path += "/sound/high/";
                break;
            case MEDIUM_QUALITY:
                path += "/sound/mid/";
                break;
            case LOW_QUALITY:
                path += "/sound/low/";
                break;
        }
        return path + file;
    }

    protected SoundData loadFromWav(String file) {
        FileReader in;
        String path = getPath(file);
        SoundData s_data = new SoundData();
        try {
            in = new FileReader(path);
        } catch (Exception e) {
            Console.print("Couldn't read in: " + file);
            e.printStackTrace();
            return null;
        }
        char[] id = new char[4];
        try {
            for (int i = 0; i < 4; i++) id[i] = (char) in.readByte();
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        String cmp = new String(id);
        if (!cmp.equals("RIFF")) {
            Console.print(file + " is not a proper wave file!");
            Console.print("Couldn't extract sound data!");
            return null;
        }
        @SuppressWarnings("unused") short format_tag, channels, block_align, bits_per_sample;
        @SuppressWarnings("unused") int filesize, format_length, sample_rate, avg_bytes_sec, data_size;
        try {
            filesize = in.readInt();
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        try {
            for (int i = 0; i < 4; i++) id[i] = (char) in.readByte();
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        cmp = new String(id);
        if (!cmp.equals("WAVE")) {
            Console.print(file + " is not a proper wave file!");
            Console.print("Couldn't extract sound data!");
            return null;
        }
        try {
            for (int i = 0; i < 4; i++) id[i] = (char) in.readByte();
            format_length = in.readInt();
            format_tag = in.readShort();
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        if (format_tag != 1) {
            Console.print("Not a raw wave file!");
            return null;
        }
        try {
            channels = in.readShort();
            sample_rate = in.readInt();
            avg_bytes_sec = in.readInt();
            block_align = in.readShort();
            bits_per_sample = in.readShort();
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        try {
            for (int i = 0; i < 4; i++) id[i] = (char) in.readByte();
            data_size = in.readInt();
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        try {
            s_data.data = new byte[data_size];
            in.readBytes(s_data.data);
        } catch (Exception e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        s_data.size = data_size;
        s_data.frequency = sample_rate;
        if (channels == 1 && bits_per_sample == 8) s_data.format = SoundFormat.MONO_8; else if (channels == 1 && bits_per_sample == 16) s_data.format = SoundFormat.MONO_16; else if (channels == 2 && bits_per_sample == 8) s_data.format = SoundFormat.STEREO_8; else if (channels == 2 && bits_per_sample == 16) s_data.format = SoundFormat.STEREO_16; else {
            Console.print("Couldn't extract sound data! Probably wrong sound format.");
            return null;
        }
        return s_data;
    }

    protected SoundData loadFromOgg(String file) {
        String path = getPath(file);
        String t_file = AppState.config.homeDir + "temp/" + file;
        SoundData s_data = new SoundData();
        try {
            FileReader.extractFile(path, t_file);
        } catch (ZipException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        } catch (IOException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        PhysicalOggStream os = null;
        try {
            os = new FileStream(new RandomAccessFile(t_file, "r"));
        } catch (OggFormatException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        } catch (FileNotFoundException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        } catch (IOException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        LogicalOggStream los = (LogicalOggStream) os.getLogicalStreams().iterator().next();
        if (los.getFormat() != LogicalOggStream.FORMAT_VORBIS) {
            Console.print("File is not in proper ogg vorbis format.");
            return null;
        }
        VorbisStream vs = null;
        try {
            vs = new VorbisStream(los);
        } catch (VorbisFormatException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        } catch (IOException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        int channels = vs.getIdentificationHeader().getChannels();
        int sample_rate = vs.getIdentificationHeader().getSampleRate();
        Vector<Byte> buffer = new Vector<Byte>();
        byte[] tempbuffer = new byte[65536];
        int len = 0;
        try {
            while (true) {
                int read = vs.readPcm(tempbuffer, 0, tempbuffer.length);
                len += read;
                for (int i = 0; i < read; i += 2) {
                    buffer.add(tempbuffer[i + 1]);
                    buffer.add(tempbuffer[i]);
                }
            }
        } catch (EndOfOggStreamException e) {
        } catch (IOException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        } catch (OutOfMemoryError e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data! File " + file + " is too large.");
            return null;
        }
        try {
            los.close();
            os.close();
            vs.close();
        } catch (IOException e) {
            Console.print("Exception: " + e.getMessage());
            Console.print("Couldn't extract sound data!");
            return null;
        }
        (new File(t_file)).delete();
        s_data.data = new byte[buffer.size()];
        for (int i = 0; i < s_data.data.length; i++) s_data.data[i] = buffer.get(i);
        s_data.size = len;
        s_data.frequency = sample_rate;
        if (channels == 1) s_data.format = SoundFormat.MONO_16; else if (channels == 2) s_data.format = SoundFormat.STEREO_16; else {
            Console.print("Couldn't extract sound data! Probably wrong sound format.");
            return null;
        }
        return s_data;
    }

    public abstract void destroy();
}
