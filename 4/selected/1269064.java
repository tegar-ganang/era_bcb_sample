package adbexplorer.util;

/**
 * @author Ralf Bensmann
 * http://blog.bensmann.com/?tag=java&page=2
 */
public class Piper implements java.lang.Runnable {

    private java.io.InputStream input;

    private java.io.OutputStream output;

    adbexplorer.util.Log4jInit log4jInit = new adbexplorer.util.Log4jInit();

    private org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Piper.class);

    public Piper(java.io.InputStream input, java.io.OutputStream output) {
        this.input = input;
        this.output = output;
    }

    public void run() {
        try {
            byte[] b = new byte[512];
            int read = 1;
            while (read > -1) {
                read = input.read(b, 0, b.length);
                if (read > -1) {
                    output.write(b, 0, read);
                }
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
            try {
                input.close();
            } catch (Exception e) {
                log.error(e);
            }
            try {
                output.close();
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}
