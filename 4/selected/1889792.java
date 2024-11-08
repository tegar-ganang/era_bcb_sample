package net.kodeninja.jem.server.content.transcoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FFmpegStreamBridge extends Thread {

    private InputStream in;

    private OutputStream out;

    private Process pid;

    private boolean doCleanup;

    public FFmpegStreamBridge(Process pid, InputStream in, OutputStream out, boolean doCleanup) {
        super("FFmpeg Transcoder Thread");
        this.in = in;
        this.out = out;
        this.pid = pid;
        this.doCleanup = doCleanup;
    }

    @Override
    public void run() {
        byte buffer[] = new byte[2048];
        int length;
        try {
            while ((length = in.read(buffer)) > -1) if (out != null) out.write(buffer, 0, length);
            if (out != null) out.flush();
        } catch (IOException e) {
            pid.destroy();
        } finally {
            try {
                if ((out != null) && (doCleanup)) out.close();
            } catch (IOException e) {
                pid.destroy();
            }
        }
    }
}
