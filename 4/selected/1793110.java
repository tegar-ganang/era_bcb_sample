package com.sts.webmeet.server.util.audio;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import com.sts.webmeet.common.RecordedWebmeetMessage;
import com.sts.webmeet.content.common.ConfigMessage;
import com.sts.webmeet.content.common.audio.AudioDataMessage;
import com.sts.webmeet.content.common.audio.Mp3Decoder;

public class Mp3AudioAndArchiveMerger implements AudioAndArchiveMerger {

    private InputStream isWebHuddle;

    private Mp3StreamPacketizer packetizer;

    private File fileMp3;

    private OutputStream osOut;

    private double dAudioCursor;

    private int iFramesPerMessage;

    private float fMillisecondsPerMessage;

    private static final int MIN_MESSAGE_INTERVAL_MILLIS = 500;

    public static final String DECODER_PROPERTY = "webhuddle.property.audio.decoder";

    public static final String ENCODER_PROPERTY = "webhuddle.property.audio.encoder";

    private static final String MP3_DECODER_CLASS_NAME = "com.sts.webmeet.content.common.audio.Mp3Decoder";

    public Mp3AudioAndArchiveMerger(InputStream isWebHuddle, File fileMp3, OutputStream osOut) throws Exception {
        this(isWebHuddle, fileMp3, osOut, 0);
    }

    public Mp3AudioAndArchiveMerger(InputStream isWebHuddle, File fileMp3, OutputStream osOut, long lOffset) throws Exception {
        this.isWebHuddle = isWebHuddle;
        this.fileMp3 = fileMp3;
        this.osOut = osOut;
        this.dAudioCursor = Math.max(0, lOffset);
        init();
        System.out.println("audio cursor set at " + this.dAudioCursor + " millis");
    }

    public void merge() throws Exception, ClassNotFoundException {
        ObjectOutputStream oos = new ObjectOutputStream(this.osOut);
        ObjectInputStream ois = new ObjectInputStream(this.isWebHuddle);
        RecordedWebmeetMessage mess = null;
        try {
            while (true) {
                mess = (RecordedWebmeetMessage) ois.readObject();
                writeAudioAndNextMessage(mess, oos);
            }
        } catch (EOFException eof) {
            System.out.println("stream ended");
        }
        oos.close();
        ois.close();
    }

    private void init() throws Exception {
        this.packetizer = new Mp3StreamPacketizer(this.fileMp3);
        float fMillisPerFrame = this.packetizer.getMillisecondsPerPacket();
        System.out.println("millis per frame: " + fMillisPerFrame);
        System.out.println("sample rate: " + this.packetizer.getSampleFequency());
        this.iFramesPerMessage = 1;
        while ((iFramesPerMessage * fMillisPerFrame) < this.MIN_MESSAGE_INTERVAL_MILLIS) {
            this.iFramesPerMessage++;
        }
        System.out.println("frames per message: " + this.iFramesPerMessage);
        this.fMillisecondsPerMessage = this.iFramesPerMessage * fMillisPerFrame;
        System.out.println("millis per message: " + this.fMillisecondsPerMessage);
    }

    public static boolean isWholeNumber(double dNumber) {
        return dNumber - ((int) dNumber) == 0;
    }

    private void writeAudioAndNextMessage(RecordedWebmeetMessage recMess, ObjectOutputStream oos) throws Exception {
        long lTime = recMess.getTimestamp();
        while (this.dAudioCursor < lTime) {
            writeAudioMessage(oos);
            this.dAudioCursor += this.fMillisecondsPerMessage;
        }
        if (recMess.getMessage() instanceof AudioDataMessage) {
        } else {
            if (recMess.getMessage() instanceof ConfigMessage) {
                ConfigMessage configMess = (ConfigMessage) recMess.getMessage();
                if (configMess.getContentType().equals("audio")) {
                    configMess = injectMp3Info(configMess);
                    recMess.setMessage(configMess);
                }
            }
            oos.writeObject(recMess);
        }
    }

    private ConfigMessage injectMp3Info(ConfigMessage config) {
        config.getProperties().put(this.DECODER_PROPERTY, getDecoderClassName());
        config.getProperties().put(Mp3Decoder.MP3_SAMPLE_RATE_PROPERTY, this.packetizer.getSampleFequency() + "");
        config.getProperties().put(Mp3Decoder.MP3_CHANNEL_COUNT_PROPERTY, this.packetizer.getChannelCount() + "");
        return config;
    }

    private String getDecoderClassName() {
        return this.MP3_DECODER_CLASS_NAME;
    }

    private void writeAudioMessage(ObjectOutputStream oos) throws Exception {
        AudioDataMessage audioMess = new AudioDataMessage();
        boolean bData = false;
        for (int i = 0; i < this.iFramesPerMessage; i++) {
            int iNextPacketLength = this.packetizer.getNextPacketLengthInBytes();
            if (-1 == iNextPacketLength) {
                System.out.println("audio stream ended: skipping");
                break;
            }
            byte[] ba = new byte[iNextPacketLength];
            this.packetizer.getPacket(ba, 0, ba.length);
            audioMess.addFrame(ba);
            bData = true;
        }
        if (bData) {
            RecordedWebmeetMessage rwm = new RecordedWebmeetMessage();
            rwm.setTimestamp((long) this.dAudioCursor);
            rwm.setMessage(audioMess);
            oos.writeObject(rwm);
        }
    }

    public static void main(String[] args) throws Exception {
        Mp3AudioAndArchiveMerger merger = new Mp3AudioAndArchiveMerger(new FileInputStream(args[0]), new File(args[1]), new FileOutputStream(args[0] + ".merged"));
        merger.merge();
    }
}
