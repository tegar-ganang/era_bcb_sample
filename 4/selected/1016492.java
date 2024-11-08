package com.shieldsbetter.paramour.resources;

import com.shieldsbetter.paramour.data.AudioBuffer;
import com.shieldsbetter.paramour.soundgraph.CachingSound;
import com.shieldsbetter.paramour.time.Time;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author hamptos
 */
public class ResourceManager {

    private static final String RAW_FILE_EXTENSION = ".au";

    private static final String COMPRESSED_FILE_EXTENSION = ".rsrc";

    private static final int BUFFER_SIZE = 1048576;

    private final File myRawDirectory;

    private final File myCompressedDirectory;

    private final File myTransientDirectory;

    private final AudioFormat myRawFormat;

    private final AudioFormat myPreferredCompressedFormat;

    private CacheUpdateProcess myCacheUpdater = null;

    public ResourceManager(File cacheDirectory) {
        this(cacheDirectory, new AudioFormat(44100, 16, 2, true, false));
    }

    public ResourceManager(File cacheDirectory, AudioFormat rawFormat) {
        this(cacheDirectory, rawFormat, null);
    }

    public ResourceManager(File cacheDirectory, AudioFormat rawFormat, AudioFormat preferredCompressedFormat) {
        if (cacheDirectory.exists() && !cacheDirectory.isDirectory()) {
            throw new IllegalArgumentException("Must instantiate " + "ResourceManager with a directory.  Was given a file: " + cacheDirectory.getAbsolutePath() + ".");
        }
        myRawFormat = rawFormat;
        myPreferredCompressedFormat = preferredCompressedFormat;
        myRawDirectory = new File(cacheDirectory, "raw");
        myCompressedDirectory = new File(cacheDirectory, "compressed");
        myTransientDirectory = new File(cacheDirectory, "transient");
        if (!myRawDirectory.exists()) {
            myRawDirectory.mkdirs();
        }
        if (!myCompressedDirectory.exists()) {
            myCompressedDirectory.mkdirs();
        }
        if (!myTransientDirectory.exists()) {
            myTransientDirectory.mkdirs();
        }
    }

    public AudioFormat getRawAudioFormat() {
        return myRawFormat;
    }

    public void keepCachingSoundUpToDate(CachingSound s) {
        if (myCacheUpdater == null) {
            myCacheUpdater = new CacheUpdateProcess();
            myCacheUpdater.start();
        }
        myCacheUpdater.addCachingSound(s);
    }

    public void stopKeepingCachingSoundUpToDate(CachingSound s) {
        myCacheUpdater.removeCachingSound(s);
        if (myCacheUpdater.getCachingSoundCount() == 0) {
            myCacheUpdater.finish();
            myCacheUpdater = null;
        }
    }

    public void setCachingSoundUpdaterThreadPaused(boolean paused) {
        myCacheUpdater.setPaused(paused);
    }

    public boolean getCachingSoundUpdaterThreadPaused() {
        return myCacheUpdater.getPaused();
    }

    public AudioInputStream getAudioInputStreamFor(ResourceIdentifier r) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public SampleRecorder recordNewSample() {
        ResourceIdentifier newRID = new ResourceIdentifier();
        SampleRecorder retval = null;
        try {
            retval = new SampleRecorder(this, newRID, rawFile(newRID.toString()));
        } catch (FileNotFoundException e) {
        }
        return retval;
    }

    public Sample importAudioFile(File file) throws FileNotFoundException, UnsupportedAudioFileException, IOException, ResourceException {
        return importAudioFile(file, myPreferredCompressedFormat);
    }

    public Sample importAudioFile(File file, AudioFormat compressedFormat) throws FileNotFoundException, UnsupportedAudioFileException, IOException, ResourceException {
        Sample retval;
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        ResourceIdentifier newRID = new ResourceIdentifier();
        Time audioLength = createRawResourceFromFile(newRID, file);
        try {
            retval = new Sample(newRID, rawFile(newRID.toString()), myRawFormat);
        } catch (Exception e) {
            throw new ResourceException(newRID, e);
        }
        return retval;
    }

    public AudioBuffer getAudioBuffer() {
        return new AudioBuffer(transientFile(new ResourceIdentifier()), transientFile(new ResourceIdentifier()), (int) myRawFormat.getSampleRate());
    }

    public Time getLength(ResourceIdentifier r) {
        File f = rawFile(r.toString());
        long bytes = f.length();
        return new Time(bytes, myRawFormat);
    }

    private File rawFile(String name) {
        return new File(myRawDirectory, name + RAW_FILE_EXTENSION);
    }

    private File compressedFile(ResourceIdentifier rid) {
        return new File(myCompressedDirectory, rid.toString() + COMPRESSED_FILE_EXTENSION);
    }

    private File transientFile(ResourceIdentifier rid) {
        return new File(myTransientDirectory, rid.toString() + RAW_FILE_EXTENSION);
    }

    public Sample getSample(ResourceIdentifier r) throws ResourceException {
        Sample retval;
        String resourceName = r.toString();
        File rawResource = rawFile(resourceName);
        if (!rawResource.exists()) {
            restoreRawResource(r);
        }
        try {
            retval = new Sample(r, rawResource, myRawFormat);
        } catch (Exception e) {
            throw new ResourceException(r, e);
        }
        return retval;
    }

    private Time createRawResourceFromFile(ResourceIdentifier rid, File originalFile) throws UnsupportedAudioFileException, IOException {
        File outputFile = rawFile(rid.toString());
        AudioInputStream sourceAudioStream = AudioSystem.getAudioInputStream(originalFile);
        AudioInputStream rawData = AudioSystem.getAudioInputStream(myRawFormat, sourceAudioStream);
        OutputStream rawResource = new FileOutputStream(outputFile);
        long lengthInBytes = transfer(rawData, rawResource);
        Time length = new Time(lengthInBytes, myRawFormat);
        rawResource.close();
        return length;
    }

    private static String buildDescription(AudioFormat f, long numFrames, Time length) {
        String retval = "";
        retval += "(* This file contains machine-readable information ";
        retval += "necessary to open the payload\n";
        retval += "   of this resource. *) \n\n";
        retval += "{SampleResource: \n";
        retval += "    {Encoding:         " + f.getEncoding() + "}\n";
        retval += "    {SampleRate:       " + f.getSampleRate() + "}\n";
        retval += "    {SampleSizeInBits: " + f.getSampleSizeInBits() + "}\n";
        retval += "    {Channels:         " + f.getChannels() + "}\n";
        retval += "    {FrameSize:        " + f.getFrameSize() + "}\n";
        retval += "    {FrameRate:        " + f.getFrameRate() + "}\n";
        retval += "    {BigEndian:        " + f.isBigEndian() + "}\n";
        retval += "    {LengthInFrames:   " + numFrames + "}\n";
        retval += "    {LengthInSeconds:  " + length.asSeconds() + "}\n";
        retval += "}\n\n";
        return retval;
    }

    private static long transfer(InputStream source, OutputStream sink) throws IOException {
        long bytesSoFar = 0;
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = 0;
        do {
            sink.write(buffer, 0, length);
            length = source.read(buffer);
            bytesSoFar += length;
        } while (length >= 0);
        sink.flush();
        return bytesSoFar;
    }

    /**
     * <p>When the call to this method completes there is guaranteed to be a
     * raw version of the resource in the raw directory.</p>
     * 
     * @param resourceName
     */
    private void restoreRawResource(ResourceIdentifier rid) {
    }
}
