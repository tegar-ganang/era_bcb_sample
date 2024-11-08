package pl.vdl.azbest.mremote.com;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import pl.vdl.azbest.log.LoggingTheGathering;
import pl.vdl.azbest.mremote.Conf;

class CSConnecting implements CState {

    private CConnect conn;

    private Logger logger = Logger.getLogger(getClass().getName());

    {
        LoggingTheGathering.addPath(getClass().getName());
    }

    public CSConnecting(CConnect conn) {
        this.conn = conn;
    }

    public void connect() {
        conn.setState(CommState.CONNECTION_FAILURE, "Another connection attempt is performed now.");
    }

    public void disconnect() {
    }

    public void error() {
    }

    public void closeCommunication() {
    }

    public void openCommunication() {
    }

    public void write(String line) {
    }

    public void perform() {
        CommPortIdentifier port = Conf.getInstance().getDevicePort();
        int rate = Conf.getInstance().getDeviceBaudRate();
        boolean inUse = port.isCurrentlyOwned();
        logger.info("Performing connection - CONNECTION - starting thread DEVICE NAME =" + Conf.getInstance().getDevicePort().getName() + "_");
        Thread t = new Thread(new Connector(port));
        t.start();
        logger.info("Performing connection - thread started");
    }

    public void timeout() {
    }

    class Connector implements Runnable {

        CommPortIdentifier port;

        Connector(CommPortIdentifier port) {
            this.port = port;
        }

        public void run() {
            try {
                logger.info("Performing connection THREAD =" + Thread.currentThread().getName() + " in runnable :: " + "Comm Connection " + Conf.getInstance().getCommPortName() + " bauds = " + Conf.getInstance().getDeviceBaudRate() + " PORT NAME =" + Conf.getInstance().getCommPortName() + "_ " + " DEVICE BAUD RATE =" + Conf.getInstance().getDeviceBaudRate() + "_" + "COMM PORT NAME= " + Conf.getInstance().getCommPortName() + "_");
                CommPort commPort = port.open("Comm Connection ", Conf.getInstance().getDeviceBaudRate());
                logger.info("openning port");
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(57600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                SerialPort sPort = (SerialPort) commPort;
                logger.info("Performing connection THREAD =" + Thread.currentThread().getName() + " obtained serial port ");
                conn.setSPort(sPort);
                InputStream is = sPort.getInputStream();
                OutputStream os = sPort.getOutputStream();
                logger.info("Performing connection THREAD =" + Thread.currentThread().getName() + " obtained stream to port " + sPort.getName());
                conn.setPortInputStream(is);
                conn.setPortOutputstream(os);
                logger.info("Performing connection THREAD =" + Thread.currentThread().getName() + " IO streams loaded ");
                logger.info("Serial port reader and writer S E T ! ");
                conn.setState(CommState.CONNECTED, "You are connected to " + sPort.getName() + " with baud rate " + sPort.getBaudRate(), conn.sConnected);
                conn.openCommunication();
            } catch (PortInUseException e) {
                conn.setState(CommState.CONNECTION_FAILURE, "Port is in use.");
                e.printStackTrace();
            } catch (IOException e) {
                conn.setState(CommState.CONNECTION_FAILURE, "IO Exception.");
                e.printStackTrace();
            } catch (UnsupportedCommOperationException e) {
                conn.setState(CommState.CONNECTION_FAILURE, "Unsupported operation excption.");
                e.printStackTrace();
            }
            if (conn.state instanceof CSConnected) {
                logger.info("Performing connection THREAD =" + Thread.currentThread().getName() + " YOU ARE CONECTED");
            } else if (conn.state instanceof CSDisconnected) {
                logger.info("Performing connection THREAD =" + Thread.currentThread().getName() + " Time out ");
                conn.setState(CommState.CONNECTION_FAILURE, "Time out");
            }
        }
    }
}
