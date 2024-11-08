package ti.ftt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import ti.mcore.Environment;

public class FTTDeviceManager {

    private FTTApi fttApi;

    private boolean fttInitialized = false;

    private String errorStr;

    private void error(String str) {
        errorStr = str;
        fttInitialized = false;
    }

    /**
	 * Class Constructor.
	 */
    public FTTDeviceManager() {
        fttApi = FTTApi.getFTTApi();
        if (fttApi == null) {
            error("Failed to find FTT library for " + System.getProperty("os.name"));
        }
    }

    public byte[] getChannelStatus() {
        if (!fttInitialized) {
            return null;
        }
        return fttApi.readChannelStatus();
    }

    public InputStream getInputStream() {
        if (!fttInitialized) {
            if (errorStr != null) Environment.getEnvironment().warning(errorStr);
            return null;
        }
        return new FTTInputStream();
    }

    public OutputStream getOutputStream() {
        if (!fttInitialized) {
            if (errorStr != null) Environment.getEnvironment().warning(errorStr);
            return null;
        }
        return new FTTOutputStream();
    }

    public boolean openFTT() {
        try {
            if (!fttInitialized && (fttApi != null)) {
                if (fttApi.open() == 0) {
                    fttInitialized = true;
                    return true;
                }
            }
        } catch (IOException e) {
            Environment.getEnvironment().unhandledException(e);
        }
        return false;
    }

    public boolean closeFTT() {
        try {
            if (fttInitialized && (fttApi != null)) {
                fttApi.close();
                fttInitialized = false;
                return true;
            }
        } catch (IOException e) {
            Environment.getEnvironment().unhandledException(e);
        }
        return false;
    }

    public boolean isFTTInitialized() {
        return fttInitialized;
    }

    /**
	 *
	 * This class provides an input stream to the TTIFSUTConnection class. 
	 * All the read operations issued by the TTIFSUTConnection 
	 * are routed to the FTTApi.dll.
	 *
	 */
    class FTTInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            byte[] buf = new byte[1];
            int rc = read(buf, 0, 1);
            if (rc < 0) return -1;
            return buf[0] & 0xff;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            return fttApi.read(buf, off, len);
        }

        @Override
        public void close() throws IOException {
            super.close();
            fttApi.close();
        }
    }

    class FTTOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            byte[] buf = new byte[1];
            buf[0] = (byte) b;
            write(buf, 0, 1);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            if (fttApi.write(buf, off, len) < len) throw new IOException("incomplete write");
        }
    }
}
