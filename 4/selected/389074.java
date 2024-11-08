package org.chizar.mp3union.engine;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.spi.mpeg.sampled.file.MpegEncoding;
import javazoom.spi.mpeg.sampled.file.MpegFileFormatType;
import org.chizar.mp3union.util.Utils;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v24Tag;

public class AudioComposer {

    private InputDataComposer inputDataComposer;

    private AudioConverter audioConverter;

    private Map<String, String> cache = new HashMap<String, String>();

    public AudioComposer(InputDataComposer inputDataComposer) {
        this.inputDataComposer = inputDataComposer;
    }

    public InputDataComposer getInputDataComposer() {
        return inputDataComposer;
    }

    public void setInputDataComposer(InputDataComposer inputDataComposer) {
        this.inputDataComposer = inputDataComposer;
    }

    public void writeMp3Tag(File file) throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
        String comment = getInputDataComposer().getComment();
        String title = getInputDataComposer().getTitle();
        MP3File f = (MP3File) AudioFileIO.read(file);
        ID3v24Tag tag = new ID3v24Tag();
        if (comment != null) {
            tag.setComment(comment);
        }
        if (title != null) {
            tag.setTitle(title);
        }
        f.setTag(tag);
        f.commit();
    }

    public void process() throws UnsupportedAudioFileException, IOException, JavaLayerException, CannotReadException, TagException, ReadOnlyFileException, InvalidAudioFrameException, CannotWriteException {
        File joinedFile = joinFiles();
        writeMp3Tag(joinedFile);
    }

    public File joinFiles() throws UnsupportedAudioFileException, IOException, JavaLayerException {
        List<File> fileSequenceList = getInputDataComposer().getFileSequence();
        AudioFormat sourceFormat = getSourceFormat(fileSequenceList);
        getAudioConverter().setTargetFormat(sourceFormat);
        File pcmFile = Utils.createTemporaryFile();
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(pcmFile));
        try {
            for (int i = 0; i < fileSequenceList.size(); i++) {
                AudioInputStream temporaryAudioStream = getAudioStream(fileSequenceList.get(i));
                try {
                    byte[] buff = new byte[1024];
                    while (-1 != (temporaryAudioStream.read(buff))) {
                        output.write(buff);
                    }
                } finally {
                    if (temporaryAudioStream != null) {
                        temporaryAudioStream.close();
                    }
                }
            }
        } finally {
            if (output != null) {
                output.close();
            }
        }
        AudioFormat targetFormat = getTargetFormat(sourceFormat);
        AudioInputStream sourceStream = null;
        AudioInputStream finalStream = null;
        try {
            sourceStream = new AudioInputStream(new BufferedInputStream(new FileInputStream(pcmFile)), sourceFormat, AudioSystem.NOT_SPECIFIED);
            finalStream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            File outputFile = new File(getInputDataComposer().getOutputFilename());
            outputFile.delete();
            AudioSystem.write(finalStream, MpegFileFormatType.MP3, outputFile);
            return outputFile;
        } finally {
            if (sourceStream != null) {
                sourceStream.close();
            }
            if (finalStream != null) {
                finalStream.close();
            }
            Utils.clearTemporaryFiles();
        }
    }

    private AudioInputStream getAudioStream(File file) throws IOException, JavaLayerException, UnsupportedAudioFileException {
        if (cache.containsKey(file.getCanonicalPath())) {
            String cachedFileName = getFromCache(file.getCanonicalPath());
            File cachedFile = new File(cachedFileName);
            return AudioSystem.getAudioInputStream(cachedFile);
        }
        File temporaryFile = Utils.createTemporaryFile();
        AudioInputStream audioInputStream = getAudioConverter().convert(AudioSystem.getAudioInputStream(file), temporaryFile);
        if (temporaryFile.exists()) {
            putToCache(file.getCanonicalPath(), temporaryFile.getCanonicalPath());
        }
        return audioInputStream;
    }

    private AudioFormat getSourceFormat(List<File> fileSequenceList) throws UnsupportedAudioFileException, IOException, JavaLayerException {
        if (fileSequenceList.size() > 0) {
            AudioInputStream ais = getAudioConverter().convertToPcm(AudioSystem.getAudioInputStream(fileSequenceList.get(0)));
            return ais.getFormat();
        } else {
            throw new IllegalArgumentException("Empty file sequence");
        }
    }

    public AudioFormat getTargetFormat(AudioFormat sourceFormat) {
        return new AudioFormat(MpegEncoding.MPEG1L3, sourceFormat.getSampleRate(), AudioSystem.NOT_SPECIFIED, sourceFormat.getChannels(), AudioSystem.NOT_SPECIFIED, AudioSystem.NOT_SPECIFIED, false);
    }

    public AudioConverter getAudioConverter() {
        if (audioConverter == null) {
            audioConverter = new AudioConverter();
        }
        return audioConverter;
    }

    public void setAudioConverter(AudioConverter audioConverter) {
        this.audioConverter = audioConverter;
    }

    public void putToCache(String key, String value) {
        cache.put(key, value);
    }

    public String getFromCache(String key) {
        return cache.get(key);
    }
}
