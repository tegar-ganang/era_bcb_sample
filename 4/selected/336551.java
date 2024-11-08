package org.spnt.applet.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import org.spantus.logger.Logger;
import org.spnt.applet.SpntAppletEventListener;
import org.spnt.applet.ctx.SpantusAudioCtx;

public class BuferedAudioWriterRunnable implements Runnable {

    private static Logger LOG = Logger.getLogger(RecordHandler.class);

    /**
		 * 
		 */
    private AudioInputStream in = null;

    private SpantusAudioCtx ctx;

    private SpntAppletEventListener listener;

    public BuferedAudioWriterRunnable(SpantusAudioCtx ctx, AudioInputStream in, SpntAppletEventListener listener) {
        this.in = in;
        this.ctx = ctx;
        this.listener = listener;
    }

    public void run() {
        try {
            LOG.debug("[recordAudio]Posting audio to {0}", this.ctx.getRecordUrl());
            LOG.debug("[recordAudio]Format: {0}", this.ctx.getRecordFormat());
            HttpURLConnection conn = (HttpURLConnection) this.ctx.getRecordUrl().openConnection();
            conn.setRequestProperty("Content-Type", getContentType(this.ctx.getRecordFormat()));
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            List<Byte> bytes = new ArrayList<Byte>(10240);
            byte[] buffer = new byte[10240];
            while (true) {
                int numRead = in.read(buffer);
                if (numRead < 0) {
                    break;
                }
                for (byte b : buffer) {
                    bytes.add(b);
                }
            }
            conn.setFixedLengthStreamingMode(bytes.size());
            OutputStream out = conn.getOutputStream();
            for (byte b : bytes) {
                out.write(b);
            }
            out.flush();
            conn.connect();
            out.close();
            in.close();
            if (this.ctx.getPlayRecordTone()) {
                this.listener.play("end_tone.wav");
            }
            LOG.debug("[run]Posted total of  {0}  audio bytes", bytes.size());
            LOG.debug("[run]Http response line: {0} - {1}", conn.getResponseCode(), conn.getResponseMessage());
        } catch (IOException e) {
            LOG.error(e);
            this.listener.setConnectionStatus(false);
        }
    }

    /**
	 * 
	 * @param format
	 * @return
	 */
    private String getContentType(AudioFormat format) {
        String encoding = null;
        if (format.getEncoding() == AudioFormat.Encoding.ULAW) {
            encoding = "MULAW";
        } else if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
            encoding = "L16";
        }
        return "AUDIO/" + encoding + "; CHANNELS=" + format.getChannels() + "; RATE=" + (int) format.getSampleRate() + "; BIG=" + format.isBigEndian();
    }
}
