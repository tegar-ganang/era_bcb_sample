package com.jmex.sound.fmod.objects;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import org.lwjgl.fmod3.FSound;
import org.lwjgl.fmod3.FSoundStream;
import org.lwjgl.fmod3.callbacks.FSoundStreamCallback;
import com.jme.util.LoggingSystem;
import com.jmex.sound.fmod.scene.Configuration;
import com.jmex.sound.fmod.scene.Playable;

public class MusicStream extends Playable {

    private ByteBuffer memoryData;

    private FSoundStream stream;

    private boolean opened;

    private boolean memory;

    private URL streamLoc;

    public MusicStream(String file, boolean memoryLoad) {
        this.streamLoc = MusicStream.class.getClassLoader().getResource(file);
        if (memoryLoad) {
            memoryData = getData(streamLoc);
            stream = FSound.FSOUND_Stream_Open(memoryData, FSound.FSOUND_LOADMEMORY);
            memory = memoryLoad;
        } else {
            stream = FSound.FSOUND_Stream_Open(file, FSound.FSOUND_NORMAL | FSound.FSOUND_MPEGACCURATE, 0, 0);
            if (stream == null) {
                URL fileU = MusicStream.class.getClassLoader().getResource(file);
                file = fileU.getFile();
                if (file.startsWith("/")) file = file.substring(1);
                stream = FSound.FSOUND_Stream_Open(file, FSound.FSOUND_NORMAL | FSound.FSOUND_MPEGACCURATE, 0, 0);
            }
        }
        opened = (stream != null);
    }

    public MusicStream(URL file, boolean memoryLoad) {
        this.streamLoc = file;
        if (memoryLoad) {
            memoryData = getData(streamLoc);
            stream = FSound.FSOUND_Stream_Open(memoryData, FSound.FSOUND_LOADMEMORY);
            memory = memoryLoad;
        } else {
            stream = FSound.FSOUND_Stream_Open(streamLoc.toString(), FSound.FSOUND_NORMAL | FSound.FSOUND_MPEGACCURATE, 0, 0);
            if (stream == null) {
                String fileName = streamLoc.getFile();
                if (fileName.startsWith("/")) fileName = fileName.substring(1);
                stream = FSound.FSOUND_Stream_Open(fileName, FSound.FSOUND_NORMAL | FSound.FSOUND_MPEGACCURATE, 0, 0);
            }
        }
        if (stream == null) {
            LoggingSystem.getLogger().log(Level.SEVERE, "Unable to open stream: " + file);
        }
        opened = (stream != null);
    }

    public void setConfiguration(Configuration conf) {
        configuration = conf;
    }

    public boolean play() {
        if (!isOpened()) {
            LoggingSystem.getLogger().log(Level.SEVERE, "Stream is not open.  Can not play.");
            return false;
        }
        if (!isPlaying()) {
            if (stream == null) LoggingSystem.getLogger().log(Level.SEVERE, "STREAM NULL");
            playingChannel = FSound.FSOUND_Stream_Play(FSound.FSOUND_FREE, stream);
        }
        if (playingChannel == -1) {
            if (memory) {
                memoryData = getData(streamLoc);
                stream = FSound.FSOUND_Stream_Open(memoryData, FSound.FSOUND_LOADMEMORY);
            } else {
                stream = FSound.FSOUND_Stream_Open(streamLoc.toString(), FSound.FSOUND_NORMAL | FSound.FSOUND_MPEGACCURATE, 0, 0);
            }
            opened = (stream != null);
        }
        FSound.FSOUND_Stream_SetEndCallback(stream, new EndCallback());
        return (playingChannel != -2 || playingChannel != -1);
    }

    /**
     * Pause the stream
     * @return true if the stream is paused
     */
    public boolean pause() {
        FSound.FSOUND_SetPaused(playingChannel, !FSound.FSOUND_GetPaused(playingChannel));
        return FSound.FSOUND_GetPaused(playingChannel);
    }

    public void stop() {
        FSound.FSOUND_Stream_Stop(stream);
    }

    public void close() {
        FSound.FSOUND_Stream_Close(stream);
    }

    public boolean isPlaying() {
        return FSound.FSOUND_IsPlaying(playingChannel);
    }

    public int length() {
        return FSound.FSOUND_Stream_GetLengthMs(stream);
    }

    /**
     * Range Between 0 and 255
     * @param volume
     */
    public void setVolume(int volume) {
        FSound.FSOUND_SetVolume(playingChannel, volume);
    }

    /**
     * 
     * @param doLoop
     */
    public void loop(boolean doLoop) {
        FSound.FSOUND_Stream_SetMode(stream, doLoop ? FSound.FSOUND_LOOP_NORMAL : FSound.FSOUND_LOOP_OFF);
    }

    /**
     * Reads the file into a ByteBuffer
     * @param filename Name of file to load
     * @return ByteBuffer containing file data
     */
    protected static ByteBuffer getData(URL file) {
        ByteBuffer buffer = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(file.openStream());
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

    private class EndCallback implements FSoundStreamCallback {

        public void FSOUND_STREAMCALLBACK(FSoundStream arg0, ByteBuffer arg1, int arg2) {
            close();
        }
    }

    public boolean isOpened() {
        return opened;
    }
}
