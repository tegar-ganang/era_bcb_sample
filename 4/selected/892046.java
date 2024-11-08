package pgbennett.speech;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import pgbennett.jampal.AudioPlayer;

public class ESpeakSpeaker implements SpeechInterface {

    int rateWPM = 165;

    int volume = 100;

    String voice;

    Process eSpeakProcess;

    Process mbrolaProcess;

    ReaderThread errThread;

    ReaderThread errThread2;

    ReaderThread outThread;

    ReaderThread pipeThread;

    PlayThread playThread;

    String espeakProg = "espeak";

    String mbrolaProg = "mbrola";

    String mbrolaBase = "/opt/mbrola";

    String espeakDataPath = "/usr/share/espeak-data";

    String mixerName;

    String mixerGain;

    String xmlVoice = null;

    /** Creates a new instance of CepstralSpeaker */
    public ESpeakSpeaker() {
    }

    public boolean close() {
        outWriter.flush();
        outWriter.close();
        return true;
    }

    PrintWriter outWriter;

    public void setPaths(String espeakProg, String espeakDataPath, String mbrolaProg, String mbrolaBase, String mixerName, String mixerGain) {
        this.espeakProg = espeakProg;
        this.mbrolaProg = mbrolaProg;
        this.mbrolaBase = mbrolaBase;
        this.espeakDataPath = espeakDataPath;
        this.mixerName = mixerName;
        this.mixerGain = mixerGain;
    }

    public boolean init() {
        String command = espeakProg;
        Vector cmdVec = new Vector();
        cmdVec.add(command);
        if (voice != null && voice.length() > 0) {
            if (voice.startsWith("mb-")) {
                String esVoice = voice;
                String[] split = voice.split(" ");
                esVoice = split[0];
                cmdVec.add("-v");
                cmdVec.add(esVoice);
            } else xmlVoice = voice;
        } else xmlVoice = null;
        cmdVec.add("-m");
        cmdVec.add("-s" + Integer.toString(rateWPM));
        cmdVec.add("-a" + volume);
        String[] cmdArray = new String[cmdVec.size()];
        cmdArray = (String[]) cmdVec.toArray(cmdArray);
        if (eSpeakProcess != null) close();
        try {
            eSpeakProcess = Runtime.getRuntime().exec(cmdArray);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        outWriter = new PrintWriter(eSpeakProcess.getOutputStream(), true);
        errThread = new ReaderThread(eSpeakProcess.getErrorStream(), System.err, "espeak-err", false);
        errThread.start();
        if (voice != null && voice.startsWith("mb-")) {
            try {
                String mbrolaLang = voice.substring(3, 6);
                ProcessBuilder pb = new ProcessBuilder(mbrolaProg, "-e", "-t", "0.8", mbrolaBase + "/" + mbrolaLang + "/" + mbrolaLang, "-", "-.au");
                mbrolaProcess = pb.start();
            } catch (IOException ex) {
                Logger.getLogger(ESpeakSpeaker.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            pipeThread = new ReaderThread(eSpeakProcess.getInputStream(), mbrolaProcess.getOutputStream(), "espeak-mbrola", true);
            pipeThread.start();
            errThread2 = new ReaderThread(mbrolaProcess.getErrorStream(), System.err, "mbrola-err", false);
            errThread2.start();
            playThread = new PlayThread(mbrolaProcess.getInputStream());
            playThread.start();
        } else {
            outThread = new ReaderThread(eSpeakProcess.getInputStream(), System.out, "espeak-out", false);
            outThread.start();
        }
        return true;
    }

    class ReaderThread extends Thread {

        InputStream inStream;

        OutputStream outStream;

        boolean eof = false;

        byte[] buffer;

        int count;

        boolean close;

        ReaderThread(InputStream in, OutputStream out, String name, boolean close) {
            super("espeak-reader-" + name);
            this.close = close;
            inStream = in;
            outStream = out;
            buffer = new byte[1024];
        }

        public void run() {
            while (!eof) {
                try {
                    count = inStream.read(buffer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    count = -1;
                }
                if (count == -1) {
                    eof = true;
                    break;
                }
                try {
                    outStream.write(buffer, 0, count);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (close) {
                try {
                    outStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    class PlayThread extends Thread {

        InputStream inStream;

        PlayThread(InputStream in) {
            super("espeak-play");
            inStream = in;
        }

        @Override
        public void run() {
            AudioFormat audioFormat = null;
            long bytesPerSecond = 44100 * 4;
            int nSampleSizeInBits = 16;
            String strMixerName = null;
            int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
            int bufferSecs = 1;
            SourceDataLine line = null;
            try {
                AudioInputStream audioInputStream;
                audioInputStream = AudioSystem.getAudioInputStream(inStream);
                audioFormat = audioInputStream.getFormat();
                bytesPerSecond = (long) audioFormat.getSampleRate() * audioFormat.getChannels() * (nSampleSizeInBits / 8);
                nInternalBufferSize = (int) bytesPerSecond * bufferSecs;
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nInternalBufferSize);
                boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
                if (!bIsSupportedDirectly) {
                    AudioFormat sourceFormat = audioFormat;
                    AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), nSampleSizeInBits, sourceFormat.getChannels(), sourceFormat.getChannels() * (nSampleSizeInBits / 8), sourceFormat.getSampleRate(), false);
                    audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                    audioFormat = audioInputStream.getFormat();
                }
                line = null;
                line = AudioPlayer.getSourceDataLine(mixerName, audioFormat, nInternalBufferSize);
                line.start();
                FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                String gainstr = mixerGain;
                float gain = 0;
                if (gainstr != null) {
                    try {
                        gain = Float.parseFloat(gainstr);
                        gain = (float) 10.0 * (float) Math.log10((double) gain * 0.01);
                        float speechGain = (float) 10.0 * (float) Math.log10((double) volume * 0.01);
                        gain += speechGain;
                    } catch (NumberFormatException ex) {
                        gain = 0;
                        System.err.println("Invalid gain value: " + gain + ", set to 0");
                    }
                    if (gain > volumeControl.getMaximum()) {
                        System.err.println("Gain value: " + gain + " too high, set to Maximum value: " + volumeControl.getMaximum());
                        gain = volumeControl.getMaximum();
                    }
                    if (gain < volumeControl.getMinimum()) {
                        System.err.println("Gain value: " + gain + " too low, set to Minimum value: " + volumeControl.getMinimum());
                        gain = volumeControl.getMinimum();
                    }
                }
                volumeControl.setValue(gain);
                int nBytesRead = 0;
                byte[] abData;
                abData = new byte[4608];
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                while (nBytesRead != -1) {
                    if (nBytesRead >= 0) line.write(abData, 0, nBytesRead);
                    nBytesRead = audioInputStream.read(abData, 0, abData.length);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (line != null) {
                    line.drain();
                    line.close();
                }
            }
        }
    }

    public boolean setRate(int rate) {
        rateWPM = (rate + 11) * 15;
        return true;
    }

    public void setVoice(String voiceName) {
        voice = voiceName;
    }

    public boolean setVolume(int volume) {
        this.volume = volume;
        return true;
    }

    public boolean speak(String strInput) {
        if (eSpeakProcess == null || outWriter == null) init();
        if (eSpeakProcess == null || outWriter == null) return false;
        if (xmlVoice != null) outWriter.println("<voice name=\"" + voice + "\">");
        outWriter.println(strInput);
        outWriter.println("\n");
        if (xmlVoice != null) outWriter.println("</voice>");
        close();
        int ret = 999;
        try {
            ret = eSpeakProcess.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (ret != 0) {
            Exception ex = new Exception("ESpeak process failed, ret=" + ret);
            ex.printStackTrace();
        }
        eSpeakProcess = null;
        if (mbrolaProcess != null) {
            ret = 999;
            try {
                ret = mbrolaProcess.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (ret != 0) {
                Exception ex = new Exception("mbrola process failed, ret=" + ret);
                ex.printStackTrace();
            }
            mbrolaProcess = null;
        }
        if (playThread != null) {
            try {
                playThread.join(60000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return true;
    }

    public String[] getVoiceList() {
        if (espeakDataPath != null && espeakDataPath.length() > 0) {
            File voicesDir = new File(espeakDataPath, "voices");
            Vector voicesVec = new Vector();
            processDirectory(voicesDir, voicesVec);
            if (voicesVec.size() == 0) return null;
            String[] voicesArray = new String[voicesVec.size()];
            voicesArray = (String[]) voicesVec.toArray(voicesArray);
            return voicesArray;
        } else return null;
    }

    void processDirectory(File dir, Vector voicesVec) {
        String dirName = dir.getName();
        File[] dirArray = dir.listFiles();
        if (dirArray == null) return;
        int ix;
        for (ix = 0; ix < dirArray.length; ix++) {
            if (dirArray[ix].isDirectory()) {
                String subDirName = dirArray[ix].getName();
                if (!"!v".equals(subDirName)) {
                    processDirectory(dirArray[ix], voicesVec);
                }
            } else {
                String name = null;
                String mbVoice = null;
                if ("mb".equals(dirName)) {
                    mbVoice = dirArray[ix].getName();
                    String mbrolaLang = mbVoice.substring(3, 6);
                    File mbVoiceFile = new File(mbrolaBase + "/" + mbrolaLang + "/" + mbrolaLang);
                    if (!mbVoiceFile.exists()) continue;
                }
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(dirArray[ix]));
                    while (name == null) {
                        String inLine = reader.readLine();
                        if (inLine == null) break;
                        inLine = inLine.trim();
                        String[] fields = inLine.split(" ", 2);
                        if ("name".equals(fields[0])) {
                            if (mbVoice != null) name = mbVoice + " " + fields[1]; else name = fields[1];
                        }
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(ESpeakSpeaker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (name != null) voicesVec.add(name);
            }
        }
    }
}
