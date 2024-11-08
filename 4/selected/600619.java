package org.kku.mp3;

import javax.sound.sampled.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javazoom.jlme.decoder.*;
import javazoom.spi.mpeg.sampled.file.*;

public class MusicPlayer2 implements Runnable, MusicPlayerIF {

    static MusicPlayer2 instance = new MusicPlayer2();

    private static boolean debug = true;

    private static int PLAY = 1;

    private static int PAUSE = 2;

    private static int STOP = 3;

    private static int SEEK = 4;

    private static int SEEKING = 5;

    private Object dataSource;

    private Decoder decoder;

    private DataLine.Info info;

    private SourceDataLine line;

    private AudioInputStream audioInputStream;

    private int state;

    private Object lock = new Object();

    private MusicPlayerListenerIF listener;

    private FloatControl gainControl;

    private BooleanControl muteControl;

    private double requestedGain;

    private double seek;

    private byte[] data;

    private boolean newSong;

    private MusicPlayer2() {
        Thread thread;
        thread = new Thread(this);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public static MusicPlayer2 getInstance() {
        return instance;
    }

    public void setListener(MusicPlayerListenerIF listener) {
        this.listener = listener;
    }

    public void play(String fileName) throws MusicPlayerException {
        dataSource = new File(fileName);
        play();
    }

    private void play() {
        state = PLAY;
        synchronized (lock) {
            debug("play");
            newSong = true;
            lock.notify();
        }
    }

    public void pause() {
        synchronized (lock) {
            debug("pause");
            state = PAUSE;
            lock.notify();
        }
    }

    public void resume() {
        synchronized (lock) {
            debug("resume");
            state = PLAY;
            lock.notify();
        }
    }

    public void stop() {
        state = STOP;
    }

    public void seek(double seek) {
        debug("seek = " + seek);
        this.seek = seek;
        state = SEEK;
    }

    public void run() {
        try {
            for (; ; ) {
                while (state == PLAY || state == SEEKING) {
                    _play();
                }
                synchronized (lock) {
                    while (state != PLAY) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void _play() throws Exception {
        int length;
        long startPosition;
        long currentPosition;
        long lastPosition;
        AudioFormat baseFormat;
        AudioFormat decodedFormat;
        AudioFileFormat baseFileFormat;
        AudioInputStream in;
        DataLine.Info newInfo;
        int totLength;
        int dataLength;
        boolean firstTime;
        int waitCounter;
        long skip;
        long skipped;
        int byteLength;
        int headerPos;
        boolean endOfSong;
        Map properties;
        InputStream inputStream;
        if (dataSource instanceof File) {
            try {
                properties = null;
                baseFileFormat = AudioSystem.getAudioFileFormat((File) dataSource);
                if (baseFileFormat != null) {
                    if (baseFileFormat instanceof MpegAudioFileFormat) {
                        properties = ((MpegAudioFileFormat) baseFileFormat).properties();
                        debug("properties = " + properties);
                    }
                }
                inputStream = new BufferedInputStream(new FileInputStream((File) dataSource));
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        } else {
            state = STOP;
            return;
        }
        newSong = false;
        debug("play : " + dataSource);
        in = AudioSystem.getAudioInputStream(inputStream);
        baseFormat = in.getFormat();
        totLength = 0;
        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
        audioInputStream = AudioSystem.getAudioInputStream(decodedFormat, in);
        newInfo = new DataLine.Info(SourceDataLine.class, decodedFormat);
        openLine(newInfo, decodedFormat);
        startPosition = line.getMicrosecondPosition();
        lastPosition = startPosition;
        dataLength = 1024 * decodedFormat.getFrameSize();
        if (data == null || data.length != dataLength) {
            data = new byte[dataLength];
        }
        line.start();
        endOfSong = false;
        try {
            firstTime = true;
            for (; ; ) {
                length = audioInputStream.read(data, 0, dataLength);
                totLength += length;
                if (length < 0) {
                    state = STOP;
                    if (listener != null) {
                        debug("total length = " + totLength);
                        listener.endOfSong();
                        endOfSong = true;
                    }
                    break;
                }
                if (line.getBufferSize() - line.available() < 10) {
                    debug("underrun !!");
                }
                waitCounter = 0;
                while (line.available() < data.length) {
                    if (state == PAUSE || state == SEEK) {
                        break;
                    }
                    Thread.sleep(1);
                    waitCounter++;
                    if (waitCounter > 20) {
                        debug("line.available = " + line.available() + ", " + data.length);
                    }
                }
                if (state == SEEK) {
                    state = SEEKING;
                    return;
                }
                if (state == SEEKING) {
                    byteLength = getByteLength(properties);
                    debug("byteLength = " + byteLength);
                    if (byteLength > 0) {
                        debug("performing skip");
                        skip = (int) (seek * byteLength);
                        while (skip > 0) {
                            skipped = audioInputStream.skip(skip);
                            if (skipped <= 0) {
                                break;
                            }
                            skip -= skipped;
                            System.out.println("skip = " + skip);
                        }
                        debug("skipped :" + skip);
                    }
                    state = PLAY;
                    continue;
                }
                if (state == PAUSE) {
                    debug("pause detected");
                    line.stop();
                    debug("  line stopped");
                    synchronized (lock) {
                        while (state == PAUSE) {
                            try {
                                lock.wait();
                            } catch (InterruptedException ex) {
                            }
                        }
                    }
                    debug("  line started again");
                    line.start();
                }
                if (!newSong) {
                    line.write(data, 0, length);
                    if (firstTime) {
                        firstTime = false;
                        dataLength = 1024 * decodedFormat.getFrameSize();
                    }
                    currentPosition = line.getMicrosecondPosition();
                    if (listener != null && (currentPosition > lastPosition + 1000000)) {
                        listener.updateCursor((int) ((currentPosition - startPosition) / 1000000));
                        lastPosition = currentPosition;
                    }
                } else {
                    debug("new song detected");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            debug("i'm stopping");
            in.close();
            if (endOfSong) {
                debug("drain");
                line.drain();
            } else {
                line.flush();
                line.stop();
            }
        }
    }

    private void openLine(DataLine.Info newInfo, AudioFormat format) throws Exception {
        if (newInfo.matches(info)) {
            return;
        }
        info = newInfo;
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format, 30720 * format.getFrameSize());
        gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        muteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        if (gainControl != null && requestedGain > 0.0) {
            setGain(requestedGain);
        }
    }

    private int getHeaderPos(Map properties) {
        Integer value;
        value = (Integer) properties.get("mp3.header.pos");
        if (value != null) {
            return value.intValue();
        }
        return -1;
    }

    private int getByteLength(Map properties) {
        Integer value;
        value = (Integer) properties.get("audio.value.bytes");
        if (value == null) {
            value = (Integer) properties.get("mp3.value.bytes");
        }
        if (value == null) {
            value = (Integer) properties.get("mp3.length.bytes");
        }
        if (value != null) {
            return value.intValue();
        }
        return -1;
    }

    public void setGain(double gain) {
        double minGain;
        double maxGain;
        double db;
        requestedGain = gain;
        if (gainControl == null) {
            return;
        }
        if (gain <= 0.0) {
            gainControl.setValue(gainControl.getMinimum());
        } else {
            gain = Math.min(gain, 100.0);
            minGain = Math.pow(10.0, (gainControl.getMinimum() / 20.0));
            maxGain = Math.pow(10.0, (gainControl.getMaximum() / 20.0));
            db = minGain + ((maxGain - minGain) / 100.0) * gain;
            db = ((Math.log(db) / Math.log(10.0)) * 20.0);
            gainControl.setValue((float) db);
        }
    }

    public double getGain() {
        double minGain;
        double maxGain;
        double gain;
        if (gainControl == null) {
            return 0.0;
        }
        minGain = Math.pow(10.0, (gainControl.getMinimum() / 20.0));
        maxGain = Math.pow(10.0, (gainControl.getMaximum() / 20.0));
        gain = Math.pow(10.0, (gainControl.getValue() / 20.0));
        gain = 0 + (gain / (maxGain - minGain)) * 100.0;
        return gain;
    }

    private void debug(String text) {
        if (!debug) {
            return;
        }
        System.out.println(text);
    }
}
