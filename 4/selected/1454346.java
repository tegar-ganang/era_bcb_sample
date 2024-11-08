package cz.cuni.mff.ufal.volk.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import cz.cuni.mff.ufal.volk.Expression;
import cz.cuni.mff.ufal.volk.ProcessingException;
import cz.cuni.mff.ufal.volk.Speech;
import cz.cuni.mff.ufal.volk.Text;
import cz.cuni.mff.ufal.volk.UnsupportedLanguageException;
import cz.cuni.mff.ufal.volk.utils.MixedAsciiInputStream;
import cz.cuni.mff.ufal.volk.utils.Utils;

/**
 * Text-To-Speech service that uses Epos tts software for generating speech.
 *
 * @author Bartłomiej Etenkowski
 */
public class EposTextToSpeech implements TextToSpeech {

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(EposTextToSpeech.class);

    public EposTextToSpeech(String serverCommand, int port) throws IOException {
        serverCommand.getClass();
        this.serverPort = port;
        String[] cmdArray = new String[] { serverCommand, "--listen_port", Integer.toString(serverPort) };
        log.info("starting epos");
        eposServerProcess = Runtime.getRuntime().exec(cmdArray);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            log.error("an InterruptedException occurred when waiting for EPOS to start", e);
        }
        log.info("epos started successfully");
        Map<String, String> languageMapping = new HashMap<String, String>();
        languageMapping.put("cs", "czech");
        languageMapping.put("sk", "slovak");
        this.languageMapping = Collections.unmodifiableMap(languageMapping);
    }

    private final Map<String, String> languageMapping;

    private String mapLanguage(String iso) {
        if (languageMapping.containsKey(iso)) return languageMapping.get(iso);
        throw new ProcessingException("Unsupported language");
    }

    private final int serverPort;

    @Override
    public boolean languageSupported(String language) {
        return languageMapping.containsKey(language);
    }

    @Override
    public List<String> supportedLanguages() {
        return Collections.unmodifiableList(new ArrayList<String>(languageMapping.keySet()));
    }

    /**
   * <p>Generates speech from the given text.</p>
   *
   * @param  text the text that has to be converted into speech. If it has the language
   *         property set (see {@link Expression#getLanguage()}
   *         and {@link Expression#setLanguage(String)} methods) the language defined
   *         by the property is used. Otherwise, the service can (but does not have to)
   *         try to recognize the language of the text.
   * @param  voice the name of the voice to be used. It can be {@code null}, if so, the default
   *         voice for the given language will be used
   * @return the generated speech or {@code null} if the requested voice does not exist
   *         or does not support the given language
   *
   * @throws NullPointerException if the text parameter is null
   * @throws UnsupportedLanguageException if the specified language is not supported
   *         by this Text-To-Speech service
   * @throws LanguageNotRecognizedException if the language of the text cannot be recognized
   *         by the service
   * @throws ProcessingException if a processing exception occurred
   */
    @Override
    public Speech process(Text text, String voice) {
        log.info(String.format("begin[process(voice=%s)]", voice));
        try {
            if (voice == null || voice.equals("")) voice = DEFAULT_VOICE;
            voice = voice.replaceAll("[\\s]+", "");
            String language = mapLanguage(text.getLanguage());
            log.info("language mapping: " + language);
            Speech speech = process0(text, voice, language);
            log.info("end[process]");
            return speech;
        } catch (IOException e) {
            throw new ProcessingException("An I/O exception occurred, see cause for details", e);
        }
    }

    private static final String ENCODING = "cp1250";

    private static final String ENCODING_ISO = ENCODING;

    private Speech process0(Text text, String voice, String language) throws UnknownHostException, IOException {
        log.trace("opening control channel");
        Socket controlChannel = new Socket("localhost", serverPort);
        log.trace("control channel opened");
        try {
            log.trace("opening data channel");
            Socket dataChannel = new Socket("localhost", serverPort);
            log.trace("data channel opened");
            try {
                return generateSpeech(text, controlChannel, dataChannel, voice, language);
            } finally {
                log.trace("closing data channel");
                dataChannel.close();
                log.trace("data channel closed");
            }
        } finally {
            log.trace("closing control channel");
            controlChannel.close();
            log.trace("control channel closed");
        }
    }

    private final String DEFAULT_VOICE = "theimer";

    private Speech generateSpeech(Text text, Socket controlChannel, Socket dataChannel, String voice, String language) throws IOException, UnsupportedEncodingException {
        final String CONTROL = "control";
        final String DATA = "data";
        MixedAsciiInputStream controlIn = new MixedAsciiInputStream(new BufferedInputStream(controlChannel.getInputStream()));
        Writer controlOut = new OutputStreamWriter(controlChannel.getOutputStream(), ENCODING_ISO);
        MixedAsciiInputStream dataIn = new MixedAsciiInputStream(new BufferedInputStream(dataChannel.getInputStream()));
        Writer dataOut = new OutputStreamWriter(dataChannel.getOutputStream(), ENCODING_ISO);
        String controlHandle = getChannelHandle(controlIn, CONTROL);
        log.info("control channel handle=" + controlHandle);
        sendLineToChannel(controlIn, controlOut, "setl charset " + ENCODING, CONTROL);
        sendLineToChannel(controlIn, controlOut, "setl voice " + voice, CONTROL);
        sendLineToChannel(controlIn, controlOut, "setl language " + language, CONTROL);
        sendLineToChannel(controlIn, controlOut, "setl waveheader true", CONTROL);
        String dataHandle = getChannelHandle(dataIn, DATA);
        log.info("data channel handle=" + dataHandle);
        sendLineToChannel(dataIn, dataOut, "data " + controlHandle, DATA);
        sendLineToChannel(controlIn, controlOut, String.format("strm $%1$s:raw:rules:diphs:synth:$%1$s", dataHandle), CONTROL);
        sendLineToChannel(controlIn, controlOut, "appl " + text.getText().length(), CONTROL);
        dataOut.write(text.getText());
        dataOut.flush();
        getSize(controlIn, CONTROL);
        int dataSize = getSize(controlIn, CONTROL);
        log.info("data size: " + dataSize);
        getSize(controlIn, CONTROL);
        log.info(String.format("received(%s): %s", CONTROL, controlIn.nextLine()));
        byte[] bytes = Utils.readAllBytes(dataIn, 10000, dataSize);
        sendLineToChannel(controlIn, controlOut, "delh " + dataHandle, CONTROL);
        sendLineToChannel(controlIn, controlOut, "done", CONTROL);
        return new Speech(text.getLanguage(), "wav/signed", bytes);
    }

    private String getChannelHandle(MixedAsciiInputStream in, String streamId) throws IOException {
        final String HANDLE_PREFIX = "handle: ";
        String line;
        do {
            line = in.nextLine();
            log.info(String.format("line (%s) received: %s", streamId, line));
        } while (!line.startsWith(HANDLE_PREFIX));
        return line.substring(HANDLE_PREFIX.length());
    }

    private void sendLineToChannel(MixedAsciiInputStream in, Writer out, String line, String channelId) throws IOException {
        sendLineToChannel(in, out, line, channelId, true);
    }

    private void sendLineToChannel(MixedAsciiInputStream in, Writer out, String line, String channelId, boolean withNewLine) throws IOException {
        log.info(String.format("sending (%s): %s", channelId, line));
        out.write(line + (withNewLine ? "\r\n" : ""));
        out.flush();
        log.info(String.format("received (%s): %s", channelId, in.nextLine()));
    }

    private int getSize(MixedAsciiInputStream in, String streamId) throws IOException {
        String SIZE_REGEX = " [\\d]+";
        String line;
        do {
            line = in.nextLine();
            log.info(String.format("received (%s): %s", streamId, line));
        } while (!line.matches(SIZE_REGEX));
        return Integer.parseInt(line.substring(1));
    }

    @Override
    public List<String> availableVoices(String language) {
        List<String> voices = new ArrayList<String>(2);
        voices.add("machac");
        voices.add("violka");
        return voices;
    }

    private final Process eposServerProcess;

    public void close() throws IOException {
        log.info("stopping EPOS server");
        eposServerProcess.destroy();
        try {
            eposServerProcess.waitFor();
            log.info("EPOS server stopped");
        } catch (InterruptedException e) {
            log.error("failed to wait for EPOS termination");
        }
    }
}
