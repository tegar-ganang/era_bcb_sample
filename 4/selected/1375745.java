package game;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.test.openal.BasicTest;
import org.lwjgl.util.WaveData;

/**
 *
 * This is a basic play test
 * Yes, over zealous use of getError ;)
 *
 * @author Brian Matzon <brian@matzon.dk>
 * @version $Revision$
 * $Id$
 */
public class PlayTestMemory extends BasicTest {

    private boolean usingVorbis;

    /**
     * Creates an instance of PlayTestMemory
     */
    public PlayTestMemory() {
        super();
    }

    /**
     * Runs the actual test, using supplied arguments
     */
    public void execute(String sound) {
        if (sound.length() < 2) {
            System.out.println("no argument supplied, default ding! used");
            sound = "sound/ding.wav";
        }
        if (sound.endsWith(".ogg")) {
            System.out.print("Attempting to load Ogg Vorbis file, checking for extension...");
            if (AL10.alIsExtensionPresent("AL_EXT_vorbis")) {
                System.out.println("found");
                usingVorbis = true;
            } else {
                System.out.println("not supported!");
                alExit();
            }
        }
        int lastError;
        IntBuffer buffers = BufferUtils.createIntBuffer(1);
        IntBuffer sources = BufferUtils.createIntBuffer(1);
        buffers.position(0).limit(1);
        AL10.alGenBuffers(buffers);
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        sources.position(0).limit(1);
        AL10.alGenSources(sources);
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        ByteBuffer filebuffer = getData(sound);
        if (filebuffer == null) {
            System.out.println("Error loading file: " + sound);
            System.exit(-1);
        }
        if (usingVorbis) {
            AL10.alBufferData(buffers.get(0), AL10.AL_FORMAT_VORBIS_EXT, filebuffer, -1);
            filebuffer.clear();
        } else {
            WaveData wavefile = WaveData.create(filebuffer.array());
            AL10.alBufferData(buffers.get(0), wavefile.format, wavefile.data, wavefile.samplerate);
            wavefile.dispose();
        }
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        AL10.alSourcei(sources.get(0), AL10.AL_BUFFER, buffers.get(0));
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        AL10.alSourcei(sources.get(0), AL10.AL_LOOPING, AL10.AL_TRUE);
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        AL10.alSourcePlay(sources.get(0));
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        try {
            Thread.sleep(360);
        } catch (InterruptedException inte) {
        }
        AL10.alSourceStop(sources.get(0));
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        sources.position(0).limit(1);
        AL10.alDeleteSources(sources);
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        buffers.position(0).limit(1);
        AL10.alDeleteBuffers(buffers);
        if ((lastError = AL10.alGetError()) != AL10.AL_NO_ERROR) {
            exit(lastError);
        }
        alExit();
    }

    /**
     * Reads the file into a ByteBuffer
     *
     * @param filename Name of file to load
     * @return ByteBuffer containing file data
     */
    protected ByteBuffer getData(String filename) {
        ByteBuffer buffer = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(WaveData.class.getClassLoader().getResourceAsStream(filename));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bufferLength = 4096;
            byte[] readBuffer = new byte[bufferLength];
            int read = -1;
            while ((read = bis.read(readBuffer, 0, bufferLength)) != -1) {
                baos.write(readBuffer, 0, read);
            }
            bis.close();
            if (usingVorbis) {
                buffer = ByteBuffer.allocateDirect(baos.size());
            } else {
                buffer = ByteBuffer.allocate(baos.size());
            }
            buffer.order(ByteOrder.nativeOrder());
            buffer.put(baos.toByteArray());
            buffer.rewind();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        return buffer;
    }
}
