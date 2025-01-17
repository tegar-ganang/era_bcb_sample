package org.spantus.core.io;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.spantus.core.extractor.IExtractorInputReader;
import org.spantus.core.extractor.SignalFormat;
import org.spantus.exception.ProcessingException;
import org.spantus.logger.Logger;

/**
 * 
 * 
 * @author Mindaugas Greibus
 * 
 * @since 0.0.1
 * 
 *        Created 2008.04.11
 * 
 */
public class DefaultAudioReader extends AbstractAudioReader {

    Logger log = Logger.getLogger(getClass());

    WraperExtractorReader wraperExtractorReader;

    public void readSignal(URL url, IExtractorInputReader extractorReader) {
        wraperExtractorReader = createWraperExtractorReader(extractorReader, 1);
        wraperExtractorReader.setFormat(getCurrentAudioFormat(url));
        List<URL> urls = new ArrayList<URL>(1);
        urls.add(url);
        readAudio(urls, wraperExtractorReader);
    }

    public void readSignalSmoothed(URL url, IExtractorInputReader extractorReader) {
        wraperExtractorReader = createWraperExtractorReader(extractorReader, 1);
        wraperExtractorReader.setFormat(getCurrentAudioFormat(url));
        wraperExtractorReader.setSmooth(false);
        List<URL> urls = new ArrayList<URL>(1);
        urls.add(url);
        readAudio(urls, wraperExtractorReader);
    }

    public void readSignal(List<URL> urls, IExtractorInputReader reader) throws ProcessingException {
        wraperExtractorReader = createWraperExtractorReader(reader, urls.size());
        wraperExtractorReader.setFormat(getCurrentAudioFormat(urls.get(0)));
        readAudio(urls, wraperExtractorReader);
    }

    public WraperExtractorReader createWraperExtractorReader(IExtractorInputReader bufferedReader, int size) {
        return new WraperExtractorReader(bufferedReader, size);
    }

    public void readAudio(URL url, WraperExtractorReader wraperExtractorReader) {
        List<URL> urls = new ArrayList<URL>(1);
        urls.add(url);
        readAudio(urls, wraperExtractorReader);
    }

    public void readAudio(List<URL> urls, WraperExtractorReader wraperExtractorReader) {
        this.wraperExtractorReader = wraperExtractorReader;
        try {
            readAudioInternal(urls);
        } catch (UnsupportedAudioFileException e) {
            throw new ProcessingException(e);
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    public void readAudioInternal(List<URL> urls) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = AudioSystem.getAudioFileFormat(urls.get(0));
        List<DataInputStream> diss = new ArrayList<DataInputStream>(urls.size());
        for (URL url : urls) {
            if (url != null) {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(AudioSystem.getAudioInputStream(url)));
                diss.add(dis);
            }
        }
        Long size = Long.valueOf(audioFileFormat.getFrameLength() * audioFileFormat.getFormat().getFrameSize());
        wraperExtractorReader.setSmoothingSize(audioFileFormat.getFrameLength());
        started(size);
        for (long index = 0; index < size; index++) {
            List<Byte> array = new ArrayList<Byte>(diss.size());
            for (DataInputStream dis : diss) {
                int readByte = dis.read();
                if (readByte == -1) break;
                array.add((byte) readByte);
            }
            wraperExtractorReader.put(array);
            processed(Long.valueOf(index), size);
        }
        wraperExtractorReader.pushValues();
        for (DataInputStream dis : diss) {
            dis.close();
        }
        ended();
    }

    public AudioFormat getCurrentAudioFormat(URL url) {
        return getAudioFormat(url).getFormat();
    }

    public AudioFileFormat getAudioFormat(URL url) {
        AudioFileFormat format = null;
        try {
            format = AudioSystem.getAudioFileFormat(url);
        } catch (UnsupportedAudioFileException e) {
        } catch (IOException e) {
            log.debug("[getAudioFormat]IO exception Exception " + url.getFile());
        }
        return format;
    }

    public Float getSampleRate(URL url) {
        try {
            return AudioSystem.getAudioFileFormat(url).getFormat().getSampleRate();
        } catch (UnsupportedAudioFileException e) {
            return 1F;
        } catch (IOException e) {
            return 1F;
        }
    }

    public WraperExtractorReader getWraperExtractorReader() {
        return wraperExtractorReader;
    }

    protected Map<String, Object> extractParameters(AudioFileFormat audioFileFormat, URL url) {
        Map<String, Object> parameters = new LinkedHashMap<String, Object>();
        parameters.put("file", url);
        parameters.put("type", audioFileFormat.getType().toString());
        parameters.put("sampleRateInHz", audioFileFormat.getFormat().getSampleRate());
        parameters.put("resolutionInBits", audioFileFormat.getFormat().getSampleSizeInBits());
        parameters.put("encoding", audioFileFormat.getFormat().getEncoding());
        parameters.put("encoding", audioFileFormat.getFormat().getEncoding());
        parameters.put("channels", audioFileFormat.getFormat().getChannels());
        parameters.put("bigEdian", audioFileFormat.getFormat().isBigEndian());
        parameters.put("bytes", audioFileFormat.getByteLength());
        Float totalTime = (audioFileFormat.getFrameLength() / audioFileFormat.getFormat().getFrameRate());
        parameters.put("lengthInTime", totalTime);
        return parameters;
    }

    public SignalFormat getFormat(URL url) {
        SignalFormat signalFormat = new SignalFormat();
        AudioFileFormat audioFileFormat = getAudioFormat(url);
        signalFormat.setLength((double) audioFileFormat.getFrameLength());
        signalFormat.setSampleRate((double) audioFileFormat.getFormat().getSampleRate());
        signalFormat.setParameters(extractParameters(audioFileFormat, url));
        return signalFormat;
    }

    public boolean isFormatSupported(URL url) {
        AudioFileFormat audioFileFormat = getAudioFormat(url);
        return audioFileFormat != null;
    }
}
