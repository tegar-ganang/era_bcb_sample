package com.flagstone.transform.util.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.DataFormatException;
import com.flagstone.transform.MovieTag;
import com.flagstone.transform.sound.DefineSound;

/**
 * SoundFactory is used to generate the objects representing an event or
 * streaming sound from an sound stored in a file, references by a URL or read
 * from an stream. An plug-in architecture allows decoders to be registered to
 * handle different formats. The SoundFactory provides a standard interface
 * for using the different decoders for each supported sound format.
 */
public final class SoundFactory {

    /** The object used to decode the sound. */
    private transient SoundDecoder decoder;

    /**
     * Decode a sound located in the specified file.
     *
     * @param file
     *            a file containing the abstract path to the sound.
     *
     * @throws IOException
     *             if there is an error reading the file.
     *
     * @throws DataFormatException
     *             if there is a problem decoding the sound, either it is in an
     *             unsupported format or an error occurred while decoding the
     *             sound data.
     */
    public void read(final File file) throws IOException, DataFormatException {
        String mimeType;
        if (file.getName().endsWith("wav")) {
            mimeType = "audio/x-wav";
        } else if (file.getName().endsWith("mp3")) {
            mimeType = "audio/mpeg";
        } else {
            throw new DataFormatException("Unsupported format");
        }
        decoder = SoundRegistry.getSoundProvider(mimeType);
        decoder.read(new FileInputStream(file));
    }

    /**
     * Decode a sound referenced by a URL.
     *
     * @param url
     *            the Uniform Resource Locator referencing the file.
     *
     * @throws IOException
     *             if there is an error reading the file.
     *
     * @throws DataFormatException
     *             if there is a problem decoding the sound, either it is in an
     *             unsupported format or an error occurred while decoding the
     *             sound data.
     */
    public void read(final URL url) throws IOException, DataFormatException {
        final URLConnection connection = url.openConnection();
        final int fileSize = connection.getContentLength();
        if (fileSize < 0) {
            throw new FileNotFoundException(url.getFile());
        }
        final String mimeType = connection.getContentType();
        decoder = SoundRegistry.getSoundProvider(mimeType);
        if (decoder == null) {
            throw new DataFormatException("Unsupported format");
        }
        decoder.read(url.openStream());
    }

    /**
     * Decode a sound from a stream.
     *
     * @param stream
     *            the InputStream containing the sound data.
     *
     * @throws IOException
     *             if there is an error reading the stream.
     *
     * @throws DataFormatException
     *             if there is a problem decoding the sound, either it is in an
     *             unsupported format or an error occurred while decoding the
     *             sound data.
     */
    public void read(final InputStream stream) throws IOException, DataFormatException {
        decoder.read(stream);
    }

    /**
     * Create a definition for an event sound that can be added to a Flash
     * movie.
     *
     * @param identifier the unique identifier for the sound.
     *
     * @return a DefineSound object containing the image definition.
     *
     * @throws IOException
     *             if there is an error reading the file.
     *
     * @throws DataFormatException
     *             if there is a problem decoding the sound, either it is in an
     *             unsupported format or an error occurred while decoding the
     *             sound data.
     */
    public DefineSound defineSound(final int identifier) throws IOException, DataFormatException {
        return decoder.defineSound(identifier);
    }

    /**
     * Create a definition for an event sound that can be added to a Flash
     * movie.
     *
     * @param identifier the unique identifier for the sound.
     *
     * @param duration the number of seconds to play the sound for.
     *
     * @return a DefineSound object containing the image definition.
     *
     * @throws IOException
     *             if there is an error reading the file.
     *
     * @throws DataFormatException
     *             if there is a problem decoding the sound, either it is in an
     *             unsupported format or an error occurred while decoding the
     *             sound data.
     */
    public DefineSound defineSound(final int identifier, final float duration) throws IOException, DataFormatException {
        return decoder.defineSound(identifier, duration);
    }

    /**
     * Generate the header for a streaming sound.
     *
     * @param frameRate the frame rate for the movie so the sound can be divided
     * into sets of samples that can be played with each frame.
     *
     * @return a SoundStreamHead2 object that defines the streaming sound.
     */
    public MovieTag streamHeader(final float frameRate) {
        return decoder.streamHeader(frameRate);
    }

    /**
     * Generate a SoundStreamBlock with next set of sound samples.
     *
     * @return a SoundStreamBlock containing the sound samples or null if there
     * are no more samples to available.
     * @throws IOException if there is an error reading the sound data.
     * @throws DataFormatException if the file contains an unsupported format.
     */
    public MovieTag streamSound() throws IOException, DataFormatException {
        return decoder.streamSound();
    }
}
