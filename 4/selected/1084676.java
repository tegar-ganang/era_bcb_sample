package de.mnit.basis.sys.thread;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import de.mnit.basis.fehler.Fehler;

/**
 * @author Michael Nitsche
 *
 * TODO finally!!!
 */
public class StreamPumpe {

    public static final int BLOCKGROESSE = 1024;

    public static Schranke verbinde(InputStream quelle, OutputStream ziel, boolean parallel) {
        return verbinde(quelle, ziel, parallel, true);
    }

    public static Schranke verbinde(InputStream quelle, OutputStream ziel, boolean parallel, boolean ziel_schliessen) {
        return verbinde(quelle, ziel, parallel, true, BLOCKGROESSE);
    }

    public static Schranke verbinde(final InputStream quelle, final OutputStream ziel, final boolean parallel, final boolean ziel_schliessen, final int block) {
        final Schranke schranke = parallel ? new Schranke(false) : null;
        Runnable run = new Runnable() {

            public void run() {
                try {
                    BufferedInputStream bis = new BufferedInputStream(quelle);
                    BufferedOutputStream bos = new BufferedOutputStream(ziel);
                    byte[] ba = new byte[block];
                    for (int len = 0; (len = bis.read()) > -1; ) {
                        bos.write(len);
                        while ((len = bis.read(ba)) == block) bos.write(ba);
                        if (len > 0) bos.write(ba, 0, len);
                    }
                    bis.close();
                    if (ziel_schliessen) bos.close(); else bos.flush();
                    if (parallel) schranke.oeffnen();
                } catch (Throwable t) {
                    Fehler.zeig(t);
                }
            }
        };
        if (parallel) new Thread(run).start(); else run.run();
        return schranke;
    }
}
