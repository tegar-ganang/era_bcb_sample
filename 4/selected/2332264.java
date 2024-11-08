package org.lwjgl.test.fmod3;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.fmod3.FMOD;
import org.lwjgl.fmod3.FMODException;
import org.lwjgl.fmod3.FSound;
import org.lwjgl.fmod3.FSoundStream;

/**
 * 
 * @author Brian Matzon <brian@matzon.dk>
 * @version $Revision: 2383 $
 * $Id: StreamPlayerMemory.java 2383 2006-06-23 08:14:49Z matzon $ <br>
 */
public class StreamPlayerMemory {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage:\n StreamPlayerMemory <file>");
            args = new String[] { "res" + File.separator + "phero2.ogg" };
            System.out.println("Using default: " + args[0]);
        }
        try {
            FMOD.create();
        } catch (FMODException fmode) {
            fmode.printStackTrace();
            System.exit(0);
        }
        System.out.println("Initializing FMOD");
        if (!FSound.FSOUND_Init(44100, 32, 0)) {
            System.out.println("Failed to initialize FMOD");
            System.out.println("Error: " + FMOD.FMOD_ErrorString(FSound.FSOUND_GetError()));
            System.exit(0);
        }
        ByteBuffer data = getData(args[0]);
        FSoundStream stream = FSound.FSOUND_Stream_Open(data, FSound.FSOUND_LOADMEMORY);
        if (stream != null) {
            FSound.FSOUND_Stream_Play(0, stream);
            int length = FSound.FSOUND_Stream_GetLengthMs(stream);
            String time = ((length / 1000) / 60) + "m " + ((length / 1000) % 60) + "s";
            System.out.println("Waiting " + time + ", for song to finish");
            try {
                Thread.sleep(length);
            } catch (InterruptedException inte) {
            }
            FSound.FSOUND_Stream_Stop(stream);
            FSound.FSOUND_Stream_Close(stream);
        } else {
            System.out.println("Unable to play: " + args[0]);
            System.out.println("Error: " + FMOD.FMOD_ErrorString(FSound.FSOUND_GetError()));
        }
        FSound.FSOUND_Close();
        FMOD.destroy();
        System.exit(0);
    }

    /**
	 * Reads the file into a ByteBuffer
	 * 
	 * @param filename Name of file to load
	 * @return ByteBuffer containing file data
	 */
    protected static ByteBuffer getData(String filename) {
        ByteBuffer buffer = null;
        System.out.println("Attempting to load: " + filename);
        try {
            BufferedInputStream bis = new BufferedInputStream(StreamPlayerMemory.class.getClassLoader().getResourceAsStream(filename));
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
            System.out.println("loaded " + buffer.remaining() + " bytes");
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        return buffer;
    }
}
