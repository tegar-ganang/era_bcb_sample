package net.sourceforge.socketrocket.model.network.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.socketrocket.model.network.ITcpSocketListener;
import net.sourceforge.socketrocket.model.network.TcpServerConnectionThread;

public class TcpInputHandler extends Thread implements IOHandlerInterface {

    Logger logger = Logger.getLogger(getClass().getName());

    private TcpServerConnectionThread tcpConnection;

    private InputStream inputStream;

    private boolean isConnectionRunning;

    private ITcpSocketListener socketListener;

    public TcpInputHandler(TcpServerConnectionThread tcpConnection) {
        isConnectionRunning = true;
        this.tcpConnection = tcpConnection;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void run() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (isConnectionRunning) {
            try {
                while (inputStream.available() > 0) {
                    baos.write(inputStream.read());
                }
                if (null != socketListener) {
                    byte[] received = baos.toByteArray();
                    if (null != received) {
                        socketListener.messageReceived(received);
                        received = null;
                        baos.reset();
                    }
                }
                Thread.sleep(100);
            } catch (IOException ioe) {
                if (isConnectionRunning) {
                    logger.log(Level.SEVERE, "IOException reading from inputStream.");
                    tcpConnection.errorOccuredCallback();
                }
            } catch (NullPointerException npe) {
                if (isConnectionRunning) {
                    logger.log(Level.SEVERE, "NPException from inputStream.");
                    tcpConnection.errorOccuredCallback();
                }
            } catch (InterruptedException ie) {
            }
        }
        try {
            baos.close();
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "IOException closing baos.");
        } finally {
            baos = null;
        }
    }

    @Override
    public void stopConnection() {
        isConnectionRunning = false;
        try {
            inputStream.close();
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, "Could not close inputStream.");
        } catch (NullPointerException npe) {
            logger.log(Level.SEVERE, "InputStream was null.");
        } finally {
            inputStream = null;
        }
    }

    public void setSocketListener(ITcpSocketListener socketListener) {
        this.socketListener = socketListener;
    }
}
