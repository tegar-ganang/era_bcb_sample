package jm.audio.io;

import java.io.*;
import java.util.Vector;
import javax.sound.sampled.*;

/**
* Audio file reading class for jMusic.
* The class utilises the JavaSound file reading classes.
* It works indepdently of the jMusic audio architecture used
* for rendering scores, and is intended for use by simple file
* manipulation utility programs operating asychronously (non real time).
* This class deals with some the current (at time of writing) incompletness
 * in the javaSound package, including converting bytes from a file into
 * floats for both big and little endian formats, and support for 24 bit files.
* @author Andrew Brown and Andrew Sorensen
*/
public class AudioFileIn {

    private String fileName;

    private File file;

    private AudioFileFormat fileFormat;

    private AudioFormat format;

    private boolean bigEndian;

    private int channels;

    private int sampleRate;

    private long duration;

    private int sampleSize;

    private AudioInputStream is;

    private float[] sampleData;

    private boolean audioFileSpecified = true;

    /**
    * This constructor opens a file and prepares it to be read.
     * @param fileName The name of the file to read, e.g., filename.aif
     */
    public AudioFileIn(String fileName) {
        this.fileName = fileName;
        try {
            this.file = new File(fileName);
            this.fileFormat = AudioSystem.getAudioFileFormat(this.file);
            this.format = this.fileFormat.getFormat();
            this.bigEndian = this.format.isBigEndian();
            channels = format.getChannels();
            sampleRate = (int) format.getSampleRate();
            this.duration = (long) this.fileFormat.getFrameLength() * this.channels;
            this.sampleSize = (format.getSampleSizeInBits()) / 8;
        } catch (UnsupportedAudioFileException uafe) {
            System.out.println("jMusic AudioFileIn warning: '" + fileName + "' may not be an audio file.");
            System.out.println("Reading it in as raw data...");
            this.audioFileSpecified = false;
            this.channels = 1;
            this.sampleSize = 1;
            this.sampleRate = 0;
        } catch (IOException ioe) {
            System.out.println("jMusic AudioFileIn error: Cannot read the specified file: " + fileName);
            System.out.println("Most likely the file does not exist at this location. Exiting...");
            System.exit(0);
        }
    }

    private void readFile() {
        if (audioFileSpecified) {
            try {
                this.is = AudioSystem.getAudioInputStream(this.file);
                byte[] tmp = new byte[(int) this.duration * this.sampleSize];
                this.is.read(tmp);
                this.is.close();
                ByteArrayInputStream bis = new ByteArrayInputStream(tmp);
                this.sampleData = new float[(int) this.duration];
                byte[] sampleWord = new byte[sampleSize];
                for (int i = 0; i < this.duration; i++) {
                    if (bis.read(sampleWord) == -1) {
                        System.out.println("Ran out of samples to read");
                    } else {
                        sampleData[i] = this.getFloat(sampleWord);
                    }
                }
                bis.close();
            } catch (UnsupportedAudioFileException uafe) {
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            Vector buffer = new Vector();
            try {
                FileInputStream fis = new FileInputStream(this.fileName);
                int val = fis.read();
                while (val != -1) {
                    buffer.addElement(new Float((float) val / 255f));
                    val = fis.read();
                }
                fis.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
            int max = buffer.size();
            this.sampleData = new float[max];
            for (int i = 0; i < max; i++) {
                sampleData[i] = (((Float) buffer.elementAt(i)).floatValue());
            }
        }
    }

    /**
    * Provide the bit size of the current audio file.
     * @return The bit depth, 8, 16, 24, or 32.
     */
    public int getBitResolution() {
        int depth = -1;
        switch(this.sampleSize) {
            case 1:
                depth = 8;
                break;
            case 2:
                depth = 16;
                break;
            case 3:
                depth = 24;
                break;
            case 4:
                depth = 32;
                break;
        }
        return depth;
    }

    /**
    * BigEndian conversion 
     */
    private float getFloat(byte[] b) {
        float sample = 0.0f;
        int ret = 0;
        int length = b.length;
        for (int i = 0; i < b.length; i++, length--) {
            ret |= ((int) (b[i] & 0xFF) << ((((bigEndian) ? length : (i + 1)) * 8) - 8));
        }
        switch(sampleSize) {
            case 1:
                if (ret > 0x7F) {
                    ret = ~ret + 1;
                    ret &= 0x7F;
                    ret = ~ret + 1;
                }
                sample = (float) ((float) ret / (float) Byte.MAX_VALUE);
                break;
            case 2:
                if (ret > 0x7FFF) {
                    ret = ~ret + 1;
                    ret &= 0x7FFF;
                    ret = ~ret + 1;
                }
                sample = (float) ((float) ret / (float) Short.MAX_VALUE);
                break;
            case 3:
                if (ret > 0x7FFFFF) {
                    ret = ~ret + 1;
                    ret &= 0x7FFFFF;
                    ret = ~ret + 1;
                }
                sample = (float) ((float) ret / 8388608f);
                break;
            case 4:
                sample = (float) ((float) ret / (float) Integer.MAX_VALUE);
                break;
            default:
                System.out.println("Format not accepted");
        }
        return sample;
    }

    /**
    * Provides a single array with the raw sample data in the format of the
     * source file. Check the number of channels and file type if required.
     * @return sampleData the audio data as an array of floating point values.
     */
    public float[] getSampleData() {
        readFile();
        return this.sampleData;
    }

    /**
    * Supplies the number of channels in the audio file/data.
     * @return channels The number of audio channels, 1 = mono, 2 = stereo etc.
     */
    public int getChannels() {
        return this.channels;
    }

    /**
    * Return the type of file, wav, aif, au etc.
     * @return fileType The type of file as a string.
     */
    public String getFileType() {
        if (audioFileSpecified) return fileFormat.toString(); else return new String("Non-audio");
    }

    /**
    * Access the sample rate.
     * @return sampleRate The number of samples per second.
     */
    public int getSampleRate() {
        return this.sampleRate;
    }

    /**
    * Access the sample size as a number of bits per sample.
     * @return bitDepth The number of bits per sample, 8, 16, 24, 32 etc.
     */
    public int getSampleBitDepth() {
        return this.sampleSize * 8;
    }

    /**
    * Indicates the type of encoding used by the file.
     * @return encodingInfo A string containing the encoding type.
     */
    public String getEncoding() {
        return this.format.getEncoding().toString();
    }

    /**
    * Access the number of samples in the file.
     * @return duration - The total number of samples.
     */
    public int getDuration() {
        return (int) this.duration;
    }
}
