package com.jmex.sound.openAL.objects;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.jmex.sound.openAL.objects.util.StreamPlayer;
import com.jmex.sound.openAL.scene.Configuration;
import com.jmex.sound.openAL.scene.Playable;

public class MusicStream extends Playable {

    private ByteBuffer memoryData;

    private boolean opened = true;

    private boolean memory;

    private String streamFile;

    private Configuration configuration;

    public MusicStream(String file, boolean memoryLoad) {
        this.streamFile = file;
        if (memoryLoad) {
        } else {
            sourceNumber = StreamPlayer.getInstance().openStream(file);
        }
    }

    public MusicStream(URL file) {
        this.streamFile = file.getFile();
        sourceNumber = StreamPlayer.getInstance().openStream(file);
    }

    public MusicStream(URL file, boolean memoryLoad) {
        this.streamFile = file.getFile();
        if (memoryLoad) {
        } else {
            sourceNumber = StreamPlayer.getInstance().openStream(file);
        }
    }

    public void setConfiguration(Configuration conf) {
        configuration = conf;
    }

    public boolean play() {
        StreamPlayer.getInstance().play(sourceNumber);
        return true;
    }

    /**
     * Pause the stream
     * @return true if the stream is paused
     */
    public boolean pause() {
        return StreamPlayer.getInstance().pauseStream(sourceNumber);
    }

    /**
     * Stops the stream handled by this Music stream
     */
    public void stop() {
        StreamPlayer.getInstance().stopStream(sourceNumber);
    }

    public void close() {
    }

    /**
     * Get the playing status of this stream
     * @return true is the stream is playing
     */
    public boolean isPlaying() {
        return StreamPlayer.getInstance().isPlaying(sourceNumber);
    }

    public int length() {
        return (int) StreamPlayer.getInstance().length(sourceNumber);
    }

    /**
     * Reads the file into a ByteBuffer
     * @param filename Name of file to load
     * @return ByteBuffer containing file data
     */
    protected static ByteBuffer getData(String filename) {
        ByteBuffer buffer = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(MusicStream.class.getClassLoader().getResourceAsStream(filename));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bufferLength = 4096;
            byte[] readBuffer = new byte[bufferLength];
            int read = -1;
            while ((read = bis.read(readBuffer, 0, bufferLength)) != -1) {
                baos.write(readBuffer, 0, read);
            }
            bis.close();
            buffer = ByteBuffer.allocateDirect(baos.size());
            buffer.order(ByteOrder.nativeOrder());
            buffer.put(baos.toByteArray());
            buffer.flip();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return buffer;
    }

    public boolean isOpened() {
        return sourceNumber != -1;
    }

    /**
     * 
     */
    public void loop(boolean flag) {
        StreamPlayer.getInstance().loopStream(sourceNumber, flag);
    }

    public void setVolume(float volume) {
        StreamPlayer.getInstance().setVolume(sourceNumber, volume);
    }
}
